
/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.motorola.mmsp.motohomex;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Advanceable;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.common.Search;
import com.motorola.mmsp.motohomex.R;
import com.motorola.mmsp.motohomex.DropTarget.DragObject;
// Added by e13775 at 19 June 2012 for organize apps' group start
import com.motorola.mmsp.motohomex.apps.AllAppsPage;
import com.motorola.mmsp.motohomex.apps.GroupItem;
// Added by e13775 at 19 June 2012 for organize apps' group end
//added by amt_wangpeipei 2012/07/11 for swtichui-2050 begin
import com.motorola.mmsp.motohomex.apps.MotoAppsCustomizePagedView;
//added by amt_wangpeipei 2012/07/11 for swtichui-2050 end.
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*2012-8-3, add by bvq783 for plugin*/
import com.motorola.mmsp.plugin.widget.*;
import com.motorola.mmsp.plugin.base.*;
import java.util.Iterator;
import java.util.Map;
/*2012-8-3, add end*/


/*2011-12-31, DJHV83 Added for Data Switch*/
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import java.util.List;
/*DJHV83 end*/
/*transition*/
import android.os.ServiceManager;
import android.view.IWindowManager;
import android.os.RemoteException;
/*2012-11-21, add by 003033 for switchuitwo-86*/
import java.lang.IllegalArgumentException;
/*2012-11-21, add end*/

/**
 * Default launcher application.
 */
public final class Launcher extends Activity
        implements View.OnClickListener, OnLongClickListener, LauncherModel.Callbacks,
                   AllAppsView.Watcher, View.OnTouchListener {
    static final String TAG = "Launcher";
    static final boolean LOGD = false;

    static final boolean PROFILE_STARTUP = false;
    static final boolean DEBUG_WIDGETS = false;
    static final boolean DEBUG_STRICT_MODE = false;

    private static final int MENU_GROUP_WALLPAPER = 1;
    // Added by e13775 at 19 June 2012 for organize apps' group start
    public static final int MENU_GROUP_EDIT_APPS_GROUP = MENU_GROUP_WALLPAPER + 1;
    public static final int MENU_GROUP_ADD_GROUP_TO_HOME = MENU_GROUP_EDIT_APPS_GROUP + 1;
    // Added by e13775 at 19 June 2012 for organize apps' group end
    /*Added by ncqp34 at Jul-12-2012 for switchui-2177*/ 	
    public static final int MENU_GROUP_SEARCH_APP_WIDGET = MENU_GROUP_ADD_GROUP_TO_HOME + 1;
    /*ended by ncqp34*/
    private static final int MENU_WALLPAPER_SETTINGS = Menu.FIRST + 1;
    private static final int MENU_MANAGE_APPS = MENU_WALLPAPER_SETTINGS + 1;
    private static final int MENU_SYSTEM_SETTINGS = MENU_MANAGE_APPS + 1;
    private static final int MENU_HELP = MENU_SYSTEM_SETTINGS + 1;
    //hnd734 Home Transition start
    private static final int MENU_TRANSITION_EFFECT = MENU_HELP+1;
    //hnd734 Home Transition end

    private static final int REQUEST_CREATE_SHORTCUT = 1;
    private static final int REQUEST_CREATE_APPWIDGET = 5;
    private static final int REQUEST_PICK_APPLICATION = 6;
    private static final int REQUEST_PICK_SHORTCUT = 7;
    private static final int REQUEST_PICK_APPWIDGET = 9;
    private static final int REQUEST_PICK_WALLPAPER = 10;

    private static final int REQUEST_BIND_APPWIDGET = 11;
	
    //hnd734 Home Transition start
    /* 2012-7-15 add by Hu ShuAn for switchui-2209 modify private to public*/
    public static final int REQUEST_TRANSITION_EFFECT = 12;
    //hnd734 Home Transition end

    static final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";

	// modify for pinch panel
    static int SCREEN_COUNT = 5;
    static int DEFAULT_SCREEN = 2;
	// modify end

    private static final String PREFERENCES = "launcher.preferences";
    static final String FORCE_ENABLE_ROTATION_PROPERTY = "launcher.force_enable_rotation";

    // The Intent extra that defines whether to ignore the launch animation
    static final String INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION =
            "com.motorola.mmsp.motohomex.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION";

    // Type: int
    private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    // Type: int
    private static final String RUNTIME_STATE = "launcher.state";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_CONTAINER = "launcher.add_container";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_SCREEN = "launcher.add_screen";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_CELL_X = "launcher.add_cell_x";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_CELL_Y = "launcher.add_cell_y";
    // Type: boolean
    private static final String RUNTIME_STATE_PENDING_FOLDER_RENAME = "launcher.rename_folder";
    // Type: long
    private static final String RUNTIME_STATE_PENDING_FOLDER_RENAME_ID = "launcher.rename_folder_id";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_SPAN_X = "launcher.add_span_x";
    // Type: int
    private static final String RUNTIME_STATE_PENDING_ADD_SPAN_Y = "launcher.add_span_y";
    // Type: parcelable
    private static final String RUNTIME_STATE_PENDING_ADD_WIDGET_INFO = "launcher.add_widget_info";

    private static final String TOOLBAR_ICON_METADATA_NAME = "com.android.launcher.toolbar_icon";
    private static final String TOOLBAR_SEARCH_ICON_METADATA_NAME =
            "com.android.launcher.toolbar_search_icon";
    private static final String TOOLBAR_VOICE_SEARCH_ICON_METADATA_NAME =
            "com.android.launcher.toolbar_voice_search_icon";

    /** The different states that Launcher can be in. */
    //RJG678 Pinch Panel
    private enum State { NONE, WORKSPACE, APPS_CUSTOMIZE, APPS_CUSTOMIZE_SPRING_LOADED, PANELVIEW };
    //RJG678 Pinch Panel END
    private State mState = State.WORKSPACE;
    private AnimatorSet mStateAnimation;
    private AnimatorSet mDividerAnimator;

    static final int APPWIDGET_HOST_ID = 1024;
    private static final int EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT = 300;
    private static final int EXIT_SPRINGLOADED_MODE_LONG_TIMEOUT = 600;
    private static final int SHOW_CLING_DURATION = 550;
    private static final int DISMISS_CLING_DURATION = 250;

    private static final Object sLock = new Object();
    private static int sScreen = DEFAULT_SCREEN;

    // How long to wait before the new-shortcut animation automatically pans the workspace
    private static int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 10;

    private final BroadcastReceiver mCloseSystemDialogsReceiver
            = new CloseSystemDialogsIntentReceiver();
    private final ContentObserver mWidgetObserver = new AppWidgetResetObserver();

    private LayoutInflater mInflater;

    private Workspace mWorkspace;
    private View mQsbDivider;
    private View mDockDivider;
    private DragLayer mDragLayer;
    private DragController mDragController;

    private AppWidgetManager mAppWidgetManager;
    private LauncherAppWidgetHost mAppWidgetHost;

    private ItemInfo mPendingAddInfo = new ItemInfo();
    private AppWidgetProviderInfo mPendingAddWidgetInfo;

    private int[] mTmpAddItemCellCoordinates = new int[2];

    private FolderInfo mFolderInfo;

    private Hotseat mHotseat;
    private View mAllAppsButton;

    //RJG678 Pinch Panel
    private DragPanel mDragPanel;
    //RJG678 Pinch Panel END
    private SearchDropTargetBar mSearchDropTargetBar;
    private AppsCustomizeTabHost mAppsCustomizeTabHost;
    // Added by e13775 at 19 June 2012 for organize apps' group
    private AllAppsPage mAllAppsPage;
    private AppsCustomizePagedView mAppsCustomizeContent;
    //added by amt_wangpeipei 2012/07/21 for switchui-2397 begin
    private View moreoverflow;
    //added by amt_wangpeipei 2012/07/21 for switchui-2397 end
    private boolean mAutoAdvanceRunning = false;

    private Bundle mSavedState;
    // We set the state in both onCreate and then onNewIntent in some cases, which causes both
    // scroll issues (because the workspace may not have been measured yet) and extra work.
    // Instead, just save the state that we need to restore Launcher to, and commit it in onResume.
    private State mOnResumeState = State.NONE;

    private SpannableStringBuilder mDefaultKeySsb = null;

    private boolean mWorkspaceLoading = true;

    private boolean mPaused = true;
    private boolean mRestoring;
    private boolean mWaitingForResult;
    private boolean mOnResumeNeedsLoad;

    // Keep track of whether the user has left launcher
    private static boolean sPausedFromUserAction = false;

    private Bundle mSavedInstanceState;

    private LauncherModel mModel;
    private IconCache mIconCache;
    private boolean mUserPresent = true;
    private boolean mVisible = false;
    private boolean mAttached = false;

    private static LocaleConfiguration sLocaleConfiguration = null;

    private static HashMap<Long, FolderInfo> sFolders = new HashMap<Long, FolderInfo>();

    private Intent mAppMarketIntent = null;

    // Related to the auto-advancing of widgets
    private final int ADVANCE_MSG = 1;
    private final int mAdvanceInterval = 20000;
    private final int mAdvanceStagger = 250;
    private long mAutoAdvanceSentTime;
    private long mAutoAdvanceTimeLeft = -1;
    private HashMap<View, AppWidgetProviderInfo> mWidgetsToAdvance =
        new HashMap<View, AppWidgetProviderInfo>();

    // Determines how long to wait after a rotation before restoring the screen orientation to
    // match the sensor state.
    private final int mRestoreScreenOrientationDelay = 500;

    // External icons saved in case of resource changes, orientation, etc.
    private static Drawable.ConstantState[] sGlobalSearchIcon = new Drawable.ConstantState[2];
    private static Drawable.ConstantState[] sVoiceSearchIcon = new Drawable.ConstantState[2];
    private static Drawable.ConstantState[] sAppMarketIcon = new Drawable.ConstantState[2];

    private final ArrayList<Integer> mSynchronouslyBoundPages = new ArrayList<Integer>();

    static final ArrayList<String> sDumpLogs = new ArrayList<String>();
    /*2012-07-26, Added by amt_chenjing for SWITCHUI-2469*/
    private final int SHOW_ALL_APPS = 3;
    /*2012-07-26, end*/
    // We only want to get the SharedPreferences once since it does an FS stat each time we get
    // it from the context.
    private SharedPreferences mSharedPrefs;

    // Holds the page that we need to animate to, and the icon views that we need to animate up
    // when we scroll to that page on resume.
    private int mNewShortcutAnimatePage = -1;
    private ArrayList<View> mNewShortcutAnimateViews = new ArrayList<View>();
    private ImageView mFolderIconImageView;
    private Bitmap mFolderIconBitmap;
    private Canvas mFolderIconCanvas;
    private Rect mRectForFolderAnimation = new Rect();
	//RJG678 Pinch Panel
    private ArrayList<Bitmap> mPanelBitmaps = new ArrayList<Bitmap>();
    private Bitmap mOnePreivewBitmap;
    private static final int DEFAULT_NUMBER = 7;/*old 5;, 2012-04-24, Chen Yidong for SWITCHUI-780*/
    /*2012-05-15, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/
    private final int SNAP_TO_PAGE_MSG = 2;
    /*2012-05-15, end*/
    //RJG678 Pinch Panel END    
    //2012-02-20 added by RJG678 for
    final static float mScale = 0.4f;
    //end by rjg678
    /*2012-09-11, added by amt_chenjing for SWITCHUI-2759*/
    private ComponentName marketActivityName;
    /*2012-09-11, addend*/
    private BubbleTextView mWaitingForResume;

    /*2012-8-3, add by bvq783 for plugin*/
    private PluginWidgetHost mPluginWidgetHost = null;
    private HashMap<String, PluginWidget> mPluginWidgets = null;
    /*2011-12-31, DJHV83 Added for Data Switch*/
    public View mCurrentView = null;//37726
    public Intent mCurrentIntent = null;
    private DataSwitchReceiver mDataSwitchReceiver = null;
    private static final String DATAALERTAPK = "com.motorola.dataalert.mmcp";
    /*DJHV83 end*/
    /*2012-7-15, add by bvq783 for switchui-2229*/
    private ArrayList<LauncherPluginWidgetInfo> mPluginWidgetInfos = new ArrayList<LauncherPluginWidgetInfo>();
    /*2012-8-13, add end*/
    //added by amt_wangpeipei 2012/05/09 a flag indicate whether current status is 
    //AppsCustomizeSearch, used when back key is down begin.
    private boolean isAppsCustomizeSearchStatus = false;
    //added by amt_wangpeipei 2012/05/09 end.

    private Runnable mBuildLayersRunnable = new Runnable() {
        public void run() {
            if (mWorkspace != null) {
                mWorkspace.buildPageHardwareLayers();
            }
        }
    };

    private static ArrayList<PendingAddArguments> sPendingAddList
            = new ArrayList<PendingAddArguments>();

    private static class PendingAddArguments {
        int requestCode;
        Intent intent;
        long container;
        int screen;
        int cellX;
        int cellY;
    }

    //hnd734 Home Transition start
    private int mWorkspaceTransitionEffect = 0;
    private int mAppsTransitionEffect = 0;
    private TransitionFactory mWorkspaceTransitionFactory;;
    private TransitionFactory mAppsTransitionFactory;
    //hnd734 Home Transition end
    
    //2012-05-11, added by Chen Yidong for SWITCHUI-1016
    private AlertDialog mPreferenceSettingDialog;
    //2012-05-11, added end
    /*add by bvq783 for entering transition*/
    private TransitionFactory mEntryTransitionFactory;;
    private EntryTransition et;
    private int mEntryTransitionEffect = 0;
    private ImageView hotseatbg;
    private final static String PASSWORD_TYPE="lockscreen.password_type";
    private final static String DISABLE_LOCKSCREEN = "lockscreen.disabled";
    /*add by bvq783 end*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEBUG_STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }

        super.onCreate(savedInstanceState);
        LauncherApplication app = ((LauncherApplication)getApplication());
        mSharedPrefs = getSharedPreferences(LauncherApplication.getSharedPreferencesKey(),
                Context.MODE_PRIVATE);
        mModel = app.setLauncher(this);
        mIconCache = app.getIconCache();
        mDragController = new DragController(this);
        mInflater = getLayoutInflater();

        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mAppWidgetHost = new LauncherAppWidgetHost(this, APPWIDGET_HOST_ID);
        mAppWidgetHost.startListening();

	/*2012-3-12, add by bvq783 for plugin*/
        mPluginWidgetHost = mModel.getPluginHost();
        /*2012-3-12, add end*/
        // If we are getting an onCreate, we can actually preempt onResume and unset mPaused here,
        // this also ensures that any synchronous binding below doesn't re-trigger another
        // LauncherModel load.
        mPaused = false;

        if (PROFILE_STARTUP) {
            android.os.Debug.startMethodTracing(
                    Environment.getExternalStorageDirectory() + "/launcher");
        }

        checkForLocaleChange();
        setContentView(R.layout.launcher);
        setupViews();
        showFirstRunWorkspaceCling();

        registerContentObservers();

        lockAllApps();

        mSavedState = savedInstanceState;
        restoreState(mSavedState);

        // Update customization drawer _after_ restoring the states
        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.onPackagesUpdated(true);
        }

        if (PROFILE_STARTUP) {
            android.os.Debug.stopMethodTracing();
        }

        if (!mRestoring) {
            if (sPausedFromUserAction) {
                // If the user leaves launcher, then we should just load items asynchronously when
                // they return.
                mModel.startLoader(true, -1);
            } else {
                // We only load the page synchronously if the user rotates (or triggers a
                // configuration change) while launcher is in the foreground
                mModel.startLoader(true, mWorkspace.getCurrentPage());
            }
        }

        if (!mModel.isAllAppsLoaded()) {
            ViewGroup appsCustomizeContentParent = (ViewGroup) mAppsCustomizeContent.getParent();
            mInflater.inflate(R.layout.apps_customize_progressbar, appsCustomizeContentParent);
        }

        // For handling default keys
        mDefaultKeySsb = new SpannableStringBuilder();
        Selection.setSelection(mDefaultKeySsb, 0);

        IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mCloseSystemDialogsReceiver, filter);

        updateGlobalIcons();
      //hnd734 Home Transition start
        mWorkspaceTransitionFactory =
                new WorkspacePagedTransitionFactory(this,"workspace_transition_effect");
        mWorkspaceTransitionEffect = mWorkspaceTransitionFactory.getEffect();
        mWorkspace.pt = mWorkspaceTransitionFactory
                .creatPagedTransition(mWorkspaceTransitionEffect);

        mAppsTransitionFactory =
                new AppsCustomizePagedTransitionFactory(this,"apps_transition_effect");
        mAppsTransitionEffect = mAppsTransitionFactory.getEffect();
        mAppsCustomizeContent.pt = mAppsTransitionFactory
                .creatPagedTransition(mAppsTransitionEffect);
        //hnd734 Home Transition end

        /*add by bvq783 for entering transition and home service*/
        mEntryTransitionFactory =
                new EntryTransitionFactory(this,"entering_transition_effect");
        mEntryTransitionEffect = mEntryTransitionFactory.getEffect();
        et = mEntryTransitionFactory.creatEntryTransition(mEntryTransitionEffect); 
        Log.d("Transition", "effect:"+mWorkspaceTransitionEffect);
        /*add by bvq783 end*/
        // On large interfaces, we want the screen to auto-rotate based on the current orientation
        unlockScreenOrientation(true);
        /*2012-01-06, DJHV83 added for Data Switch*/
        checkDataSwitchValid();
        /*End added*/
    }

    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        sPausedFromUserAction = true;
    }

    private void updateGlobalIcons() {
        boolean searchVisible = false;
        boolean voiceVisible = false;
        // If we have a saved version of these external icons, we load them up immediately
        int coi = getCurrentOrientationIndexForGlobalIcons();
        if (sGlobalSearchIcon[coi] == null || sVoiceSearchIcon[coi] == null ||
                sAppMarketIcon[coi] == null) {
            updateAppMarketIcon();
            searchVisible = updateGlobalSearchIcon();
            voiceVisible = updateVoiceSearchIcon(searchVisible);
        }
        if (sGlobalSearchIcon[coi] != null) {
             updateGlobalSearchIcon(sGlobalSearchIcon[coi]);
             searchVisible = true;
        }
        if (sVoiceSearchIcon[coi] != null) {
            updateVoiceSearchIcon(sVoiceSearchIcon[coi]);
            voiceVisible = true;
        }
        if (sAppMarketIcon[coi] != null) {
            updateAppMarketIcon(sAppMarketIcon[coi]);
        }
        mSearchDropTargetBar.onSearchPackagesChanged(searchVisible, voiceVisible);
    }

    private void checkForLocaleChange() {
        if (sLocaleConfiguration == null) {
            new AsyncTask<Void, Void, LocaleConfiguration>() {
                @Override
                protected LocaleConfiguration doInBackground(Void... unused) {
                    LocaleConfiguration localeConfiguration = new LocaleConfiguration();
                    readConfiguration(Launcher.this, localeConfiguration);
                    return localeConfiguration;
                }

                @Override
                protected void onPostExecute(LocaleConfiguration result) {
                    sLocaleConfiguration = result;
                    checkForLocaleChange();  // recursive, but now with a locale configuration
                }
            }.execute();
            return;
        }

        final Configuration configuration = getResources().getConfiguration();

        final String previousLocale = sLocaleConfiguration.locale;
        final String locale = configuration.locale.toString();

        final int previousMcc = sLocaleConfiguration.mcc;
        final int mcc = configuration.mcc;

        final int previousMnc = sLocaleConfiguration.mnc;
        final int mnc = configuration.mnc;

        boolean localeChanged = !locale.equals(previousLocale) || mcc != previousMcc || mnc != previousMnc;

        if (localeChanged) {
            sLocaleConfiguration.locale = locale;
            sLocaleConfiguration.mcc = mcc;
            sLocaleConfiguration.mnc = mnc;

            mIconCache.flush();

            final LocaleConfiguration localeConfiguration = sLocaleConfiguration;
            new Thread("WriteLocaleConfiguration") {
                @Override
                public void run() {
                    writeConfiguration(Launcher.this, localeConfiguration);
                }
            }.start();
        }
    }

    private static class LocaleConfiguration {
        public String locale;
        public int mcc = -1;
        public int mnc = -1;
    }

    private static void readConfiguration(Context context, LocaleConfiguration configuration) {
        DataInputStream in = null;
        try {
            in = new DataInputStream(context.openFileInput(PREFERENCES));
            configuration.locale = in.readUTF();
            configuration.mcc = in.readInt();
            configuration.mnc = in.readInt();
        } catch (FileNotFoundException e) {
            // Ignore
        } catch (IOException e) {
            // Ignore
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    private static void writeConfiguration(Context context, LocaleConfiguration configuration) {
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(context.openFileOutput(PREFERENCES, MODE_PRIVATE));
            out.writeUTF(configuration.locale);
            out.writeInt(configuration.mcc);
            out.writeInt(configuration.mnc);
            out.flush();
        } catch (FileNotFoundException e) {
            // Ignore
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            context.getFileStreamPath(PREFERENCES).delete();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    public DragLayer getDragLayer() {
        return mDragLayer;
    }

    boolean isDraggingEnabled() {
        // We prevent dragging when we are loading the workspace as it is possible to pick up a view
        // that is subsequently removed from the workspace in startBinding().
        return !mModel.isLoadingWorkspace();
    }

    static int getScreen() {
        synchronized (sLock) {
            return sScreen;
        }
    }

    static void setScreen(int screen) {
        synchronized (sLock) {
            sScreen = screen;
        }
    }

    /**
     * Returns whether we should delay spring loaded mode -- for shortcuts and widgets that have
     * a configuration step, this allows the proper animations to run after other transitions.
     */
    private boolean completeAdd(PendingAddArguments args) {
        boolean result = false;
        switch (args.requestCode) {
            case REQUEST_PICK_APPLICATION:
                completeAddApplication(args.intent, args.container, args.screen, args.cellX,
                        args.cellY);
                break;
            case REQUEST_PICK_SHORTCUT:
                processShortcut(args.intent);
                break;
            case REQUEST_CREATE_SHORTCUT:
                completeAddShortcut(args.intent, args.container, args.screen, args.cellX,
                        args.cellY);
                result = true;
                break;
            case REQUEST_CREATE_APPWIDGET:
                int appWidgetId = args.intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                completeAddAppWidget(appWidgetId, args.container, args.screen, null, null);
                result = true;
                break;
            case REQUEST_PICK_WALLPAPER:
                // We just wanted the activity result here so we can clear mWaitingForResult
                break;
        }
        // Before adding this resetAddInfo(), after a shortcut was added to a workspace screen,
        // if you turned the screen off and then back while in All Apps, Launcher would not
        // return to the workspace. Clearing mAddInfo.container here fixes this issue
        resetAddInfo();
        return result;
    }

    @Override
    protected void onActivityResult(
            final int requestCode, final int resultCode, final Intent data) {
		//hnd734 Home Transition start
        if (requestCode == REQUEST_TRANSITION_EFFECT) {
            /*2012-07-24, added by Bai Jian SWITCHUI-2449*/
            if ( resultCode == Activity.RESULT_OK ) {
                /**2012-07-26, ChenYidong change for SWITCHUI-2441, widgets, shortcuts disappear*/
                boolean transitionChanged = false;
                if (mWorkspaceTransitionEffect !=
                        mWorkspaceTransitionFactory.getEffect()) {
                    transitionChanged = true;
                }
                if (mAppsTransitionEffect !=
                        mAppsTransitionFactory.getEffect()) {
                    transitionChanged = true;
                }
                if(transitionChanged){
                    try{
                        //set transition and restart home
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(0);
                        return;
                    }catch(Exception e){
                        Log.i(TAG, "catch a exception when change transition and restart motohome");
                    }
                } else {
					// RJG678 Pinch Panel
                    if ( mState == State.PANELVIEW ) showWorkspace(false);
					// RJG678 Pinch Panel
                }
                /**2012-07-26, end*/
            }
            /*2012-07-24, end*/
        }
        //hnd734 Home Transition end
			
        if (requestCode == REQUEST_BIND_APPWIDGET) {
            int appWidgetId = data != null ?
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (resultCode == RESULT_CANCELED) {
                completeTwoStageWidgetDrop(RESULT_CANCELED, appWidgetId);
            } else if (resultCode == RESULT_OK) {
                addAppWidgetImpl(appWidgetId, mPendingAddInfo, null, mPendingAddWidgetInfo);
            }
            return;
        }
        boolean delayExitSpringLoadedMode = false;
        boolean isWidgetDrop = (requestCode == REQUEST_PICK_APPWIDGET ||
                requestCode == REQUEST_CREATE_APPWIDGET);
        mWaitingForResult = false;

        // We have special handling for widgets
        if (isWidgetDrop) {
            int appWidgetId = data != null ?
                    data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) : -1;
            if (appWidgetId < 0) {
                Log.e(TAG, "Error: appWidgetId (EXTRA_APPWIDGET_ID) was not returned from the \\" +
                        "widget configuration activity.");
                completeTwoStageWidgetDrop(RESULT_CANCELED, appWidgetId);
            } else {
                completeTwoStageWidgetDrop(resultCode, appWidgetId);
            }
            return;
        }

        // The pattern used here is that a user PICKs a specific application,
        // which, depending on the target, might need to CREATE the actual target.

        // For example, the user would PICK_SHORTCUT for "Music playlist", and we
        // launch over to the Music app to actually CREATE_SHORTCUT.
        if (resultCode == RESULT_OK && mPendingAddInfo.container != ItemInfo.NO_ID) {
            final PendingAddArguments args = new PendingAddArguments();
            args.requestCode = requestCode;
            args.intent = data;
            args.container = mPendingAddInfo.container;
            args.screen = mPendingAddInfo.screen;
            args.cellX = mPendingAddInfo.cellX;
            args.cellY = mPendingAddInfo.cellY;
            if (isWorkspaceLocked()) {
                sPendingAddList.add(args);
            } else {
                delayExitSpringLoadedMode = completeAdd(args);
            }
        }
        mDragLayer.clearAnimatedView();
        // Exit spring loaded mode if necessary after cancelling the configuration of a widget
        exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED), delayExitSpringLoadedMode,
                null);
    }

    private void completeTwoStageWidgetDrop(final int resultCode, final int appWidgetId) {
        CellLayout cellLayout =
                (CellLayout) mWorkspace.getChildAt(mPendingAddInfo.screen);
        Runnable onCompleteRunnable = null;
        int animationType = 0;

        AppWidgetHostView boundWidget = null;
        if (resultCode == RESULT_OK) {
            animationType = Workspace.COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION;
            final AppWidgetHostView layout = mAppWidgetHost.createView(this, appWidgetId,
                    mPendingAddWidgetInfo);
            boundWidget = layout;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    completeAddAppWidget(appWidgetId, mPendingAddInfo.container,
                            mPendingAddInfo.screen, layout, null);
                    exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED), false,
                            null);
                }
            };
        } else if (resultCode == RESULT_CANCELED) {
            animationType = Workspace.CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    exitSpringLoadedDragModeDelayed((resultCode != RESULT_CANCELED), false,
                            null);
                }
            };
        }
        if (mDragLayer.getAnimatedView() != null) {
            mWorkspace.animateWidgetDrop(mPendingAddInfo, cellLayout,
                    (DragView) mDragLayer.getAnimatedView(), onCompleteRunnable,
                    animationType, boundWidget, true);
        } else {
            // The animated view may be null in the case of a rotation during widget configuration
            onCompleteRunnable.run();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPaused = false;
        /*2012-6-18, add by bvq783 for switchui-1547*/
        if (!LauncherApplication.isEnterHomeSent()) {
            Intent intent = new Intent("com.motorola.mmsp.motohome.ENTERED_HOME");
            sendBroadcast(intent);
            if(LOGD) Log.d(TAG, "send intent to mota");
            LauncherApplication.setEnterHomeSent(true);
        }
        /*2012-6-18, add by bvq783 for switchui-1547*/

        // Restore the previous launcher state
        if (mOnResumeState == State.WORKSPACE) {
            showWorkspace(false);
        } else if (mOnResumeState == State.APPS_CUSTOMIZE) {
            showAllApps(false);
        }
        mOnResumeState = State.NONE;

        // Process any items that were added while Launcher was away
        InstallShortcutReceiver.flushInstallQueue(this);

        mPaused = false;
        sPausedFromUserAction = false;
        if (mRestoring || mOnResumeNeedsLoad) {
            mWorkspaceLoading = true;
            mModel.startLoader(true, -1);
            mRestoring = false;
            mOnResumeNeedsLoad = false;
        }
        /*2012-8-3, add by bvq783 for plugin*/
        if (mPluginWidgetHost != null && mState == State.WORKSPACE)
            mPluginWidgetHost.statusChange(mPluginWidgetHost.ONRESUME);
        /*2012-8-3, add end*/

        // Reset the pressed state of icons that were locked in the press state while activities
        // were launching
        if (mWaitingForResume != null) {
            // Resets the previous workspace icon press state
            mWaitingForResume.setStayPressed(false);
        }
        mAppsCustomizeTabHost.onResume();
        if (mAppsCustomizeContent != null) {
            // Resets the previous all apps icon press state
            mAppsCustomizeContent.resetDrawableState();
        }
        // It is possible that widgets can receive updates while launcher is not in the foreground.
        // Consequently, the widgets will be inflated in the orientation of the foreground activity
        // (framework issue). On resuming, we ensure that any widgets are inflated for the current
        // orientation.
        getWorkspace().reinflateWidgetsIfNecessary();

        // Again, as with the above scenario, it's possible that one or more of the global icons
        // were updated in the wrong orientation.
        updateGlobalIcons();
        /*2012-04-26, Chen Yidong, for SWITCHUI-792*/
        if(mState == State.WORKSPACE && mWorkspace.getChildCount() > 0){
            CellLayout cell = (CellLayout) mWorkspace.getChildAt(0);
            if(cell.isShowForeground()){
                cell.setOverscrollTransformsDirty(true);
                cell.resetOverscrollTransforms();
            }
            
            cell = (CellLayout) mWorkspace.getChildAt(mWorkspace.getChildCount() - 1);
            if(cell.isShowForeground()){
                cell.setOverscrollTransformsDirty(true);
                cell.resetOverscrollTransforms();
            }
        }
        /*2012-04-26, end*/
        /*2012-06-05, add by Chen Yidong for SWITCHUI-1355*/
        if(mState == State.APPS_CUSTOMIZE && mAppsCustomizeContent != null && mAppsCustomizeContent.getChildCount() > 0){
            int cp = mAppsCustomizeContent.getCurrentPage();
            if(cp == 0 || cp == mAppsCustomizeContent.getChildCount() - 1){
                View v = mAppsCustomizeContent.getPageAt(cp);
                if(v != null && Math.abs(v.getRotationY()) > 0.1f){
                    if(cp == 0){
                        mAppsCustomizeContent.mOverScrollX = 0;
                    } else {
                        mAppsCustomizeContent.mOverScrollX = mAppsCustomizeContent.mMaxScrollX ;
                    }
                    mAppsCustomizeContent.resetPageWhenResume();
                    v.setRotationY(0f);
                    v.requestLayout();
                }
            }
        }
        /*2012-06-05, end*/
        /*2012-07-22, ChenYidong added for SWITCHUI-2217*/
        checkHomeState();
        /*2012-07-22, end*/
    }

    @Override
    protected void onPause() {
        // NOTE: We want all transitions from launcher to act as if the wallpaper were enabled
        // to be consistent.  So re-enable the flag here, and we will re-disable it as necessary
        // when Launcher resumes and we are still in AllApps.
        updateWallpaperVisibility(true);

        super.onPause();
        mPaused = true;
        mDragController.cancelDrag();
        mDragController.resetLastGestureUpTime();
        /*2012-8-3, add by bvq783*/
        if (mPluginWidgetHost != null)
            mPluginWidgetHost.statusChange(mPluginWidgetHost.ONPAUSE);
        /*2012-8-3, add end*/
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        // Flag the loader to stop early before switching
        mModel.stopLoader();
        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.surrender();
        }
        return Boolean.TRUE;
    }

    // We can't hide the IME if it was forced open.  So don't bother
    /*
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            final InputMethodManager inputManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            inputManager.hideSoftInputFromWindow(lp.token, 0, new android.os.ResultReceiver(new
                        android.os.Handler()) {
                        protected void onReceiveResult(int resultCode, Bundle resultData) {
                            Log.d(TAG, "ResultReceiver got resultCode=" + resultCode);
                        }
                    });
            Log.d(TAG, "called hideSoftInputFromWindow from onWindowFocusChanged");
        }
    }
    */

    private boolean acceptFilter() {
        final InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        return !inputManager.isFullscreenMode();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        final int uniChar = event.getUnicodeChar();
        final boolean handled = super.onKeyDown(keyCode, event);
        final boolean isKeyNotWhitespace = uniChar > 0 && !Character.isWhitespace(uniChar);
        /*2012-8-16, add by bvq783*/
        if(mDragController.isDragging()) {
            mDragController.cancelDrag();
            exitSpringLoadedDragMode();
        }
        /*2012-8-16, add end*/

        if (!handled && acceptFilter() && isKeyNotWhitespace) {
            boolean gotKey = TextKeyListener.getInstance().onKeyDown(mWorkspace, mDefaultKeySsb,
                    keyCode, event);
            if (gotKey && mDefaultKeySsb != null && mDefaultKeySsb.length() > 0) {
                // something usable has been typed - start a search
                // the typed text will be retrieved and cleared by
                // showSearchDialog()
                // If there are multiple keystrokes before the search dialog takes focus,
                // onSearchRequested() will be called for every keystroke,
                // but it is idempotent, so it's fine.
                return onSearchRequested();
            }
        }

        // Eat the long press event so the keyboard doesn't come up.
        if (keyCode == KeyEvent.KEYCODE_MENU && event.isLongPress()) {
            return true;
        }

        return handled;
    }

    private String getTypedText() {
        return mDefaultKeySsb.toString();
    }

    private void clearTypedText() {
        mDefaultKeySsb.clear();
        mDefaultKeySsb.clearSpans();
        Selection.setSelection(mDefaultKeySsb, 0);
    }

    /**
     * Given the integer (ordinal) value of a State enum instance, convert it to a variable of type
     * State
     */
    private static State intToState(int stateOrdinal) {
        State state = State.WORKSPACE;
        final State[] stateValues = State.values();
        for (int i = 0; i < stateValues.length; i++) {
            if (stateValues[i].ordinal() == stateOrdinal) {
                state = stateValues[i];
                break;
            }
        }
        return state;
    }

    /**
     * Restores the previous state, if it exists.
     *
     * @param savedState The previous state.
     */
    private void restoreState(Bundle savedState) {
        if (savedState == null) {
            return;
        }

        State state = intToState(savedState.getInt(RUNTIME_STATE, State.WORKSPACE.ordinal()));
        if (state == State.APPS_CUSTOMIZE) {
            mOnResumeState = State.APPS_CUSTOMIZE;
        }

        int currentScreen = savedState.getInt(RUNTIME_STATE_CURRENT_SCREEN, -1);
        if (currentScreen > -1) {
            mWorkspace.setCurrentPage(currentScreen);
        }

        final long pendingAddContainer = savedState.getLong(RUNTIME_STATE_PENDING_ADD_CONTAINER, -1);
        final int pendingAddScreen = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SCREEN, -1);

        if (pendingAddContainer != ItemInfo.NO_ID && pendingAddScreen > -1) {
            mPendingAddInfo.container = pendingAddContainer;
            mPendingAddInfo.screen = pendingAddScreen;
            mPendingAddInfo.cellX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_X);
            mPendingAddInfo.cellY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_Y);
            mPendingAddInfo.spanX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_X);
            mPendingAddInfo.spanY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y);
            mPendingAddWidgetInfo = savedState.getParcelable(RUNTIME_STATE_PENDING_ADD_WIDGET_INFO);
            mWaitingForResult = true;
            mRestoring = true;
        }


        boolean renameFolder = savedState.getBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, false);
        if (renameFolder) {
            long id = savedState.getLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID);
            mFolderInfo = mModel.getFolderById(this, sFolders, id);
            mRestoring = true;
        }


        // Restore the AppsCustomize tab
        if (mAppsCustomizeTabHost != null) {
            String curTab = savedState.getString("apps_customize_currentTab");
            if (curTab != null) {
                mAppsCustomizeTabHost.setContentTypeImmediate(
                        mAppsCustomizeTabHost.getContentTypeForTabTag(curTab));
                mAppsCustomizeContent.loadAssociatedPages(
                        mAppsCustomizeContent.getCurrentPage());
            }

            int currentIndex = savedState.getInt("apps_customize_currentIndex");
            /*2012-3-21, add by bvq783 for IKSWITCHUI-1567*/
            if (mAppsCustomizeContent.getCurrentPage() <= 0) {
                currentIndex = -1;
            }
            /*2012-3-21, add end*/
            mAppsCustomizeContent.restorePageForIndex(currentIndex);
        }

        // Added by e13775 at 19 June 2012 for organize apps' group start
        if (mAllAppsPage != null){
            mAllAppsPage.restoreInstanceState(savedState.getBundle("allappspage_bundle"));
        }

    }
    //modified by amt_wangpeipei 2012/07/21 for switchui-2397 begin
    // Added by e13775 at 19 June 2012 for organize apps' group start
    public AllAppsPage getAllAppsPage(){
        return mAllAppsPage;
    }
    //modified by amt_wangpeipei 2012/07/21 for switchui-2397 end

    /**
     * Finds all the views we need and configure them properly.
     */
    private void setupViews() {
        final DragController dragController = mDragController;

        mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
        mWorkspace = (Workspace) mDragLayer.findViewById(R.id.workspace);
        mQsbDivider = (ImageView) findViewById(R.id.qsb_divider);
        mDockDivider = (ImageView) findViewById(R.id.dock_divider);

        // Setup the drag layer
        mDragLayer.setup(this, dragController);

        // Setup the hotseat
        mHotseat = (Hotseat) findViewById(R.id.hotseat);
        if (mHotseat != null) {
            mHotseat.setup(this);
        }

        // Setup the workspace
        mWorkspace.setHapticFeedbackEnabled(false);
        mWorkspace.setOnLongClickListener(this);
        mWorkspace.setup(dragController);
        dragController.addDragListener(mWorkspace);

        //RJG678 Pinch Panel
        
		// initialize Workspace
		int count = getPanelNumber();
		mWorkspace.setScreenCount(count);
        /*2012-3-6, add by bvq783 for IKSWITCHUI-94 as panel is not specified*/
        SCREEN_COUNT = count;
        /*2012-3-6, add end*/
		
        // Setup the Pinch Panel
        mDragPanel = (DragPanel) findViewById(R.id.drap_panel);
        mDragPanel.setVisibility(View.INVISIBLE);
        mDragPanel.setFocusable(false);
        mDragPanel.setWillNotDraw(false);
        /*add by bvq783 for hotseat background*/
        hotseatbg = (ImageView)findViewById(R.id.hotseatback);
        /*add by bvq783 end*/
        //RJG678 Pinch Panel END
        // Get the search/delete bar
        mSearchDropTargetBar = (SearchDropTargetBar) mDragLayer.findViewById(R.id.qsb_bar);

        // Setup AppsCustomize
        mAppsCustomizeTabHost = (AppsCustomizeTabHost)
                findViewById(R.id.apps_customize_pane);
        mAppsCustomizeContent = (AppsCustomizePagedView)
                mAppsCustomizeTabHost.findViewById(R.id.apps_customize_pane_content);
        mAppsCustomizeTabHost.setup(this);
        mAppsCustomizeContent.setup(this, dragController);

        // Added by e13775 at 19 June 2012 for organize apps' group
        mAllAppsPage = (AllAppsPage) findViewById(android.R.id.tabcontent);
        LauncherApplication app = ((LauncherApplication)getApplication());
        if (mAllAppsPage != null) {
            if (app.hasNavigationBar() ||app.isScreenLarge()){
                //modified by amt_wangpeipei 2012/07/21 for switchui-2397 begin
                moreoverflow = findViewById(R.id.moreoverflow_button);
                //modified by amt_wangpeipei 2012/07/21 for switchui-2397 end
                moreoverflow.setVisibility(View.VISIBLE);
                mAllAppsPage.setOverFlowMenu(moreoverflow);
            }
        }

        // Get the all apps button
        mAllAppsButton = findViewById(R.id.all_apps_button);
        if (mAllAppsButton != null) {
            mAllAppsButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
                        onTouchDownAllAppsButton(v);
                    }
                    return false;
                }
            });
        }
        // Setup the drag controller (drop targets have to be added in reverse order in priority)
        dragController.setDragScoller(mWorkspace);
        dragController.setScrollView(mDragLayer);
        dragController.setMoveTarget(mWorkspace);
        dragController.addDropTarget(mWorkspace);
        if (mSearchDropTargetBar != null) {
            mSearchDropTargetBar.setup(this, dragController);
        }
    }

    
    //added by amt_wangpeipei 2012/07/21 for switchui-2397
    public View getOverFlowMenu(){
    	return moreoverflow;
    }
	
	    //RJG678 Pinch Panel
    /*
     * Get the number of Pinch Panel
     */
    public int getPanelNumber() {

        SharedPreferences sharedPrefs = getSharedPreferences("PanelNumbers", Activity.MODE_PRIVATE);

        if (sharedPrefs != null) {
            return sharedPrefs.getInt("Panel", DEFAULT_NUMBER);
        }
        return DEFAULT_NUMBER;
    }

    /*
     *  Save the Panel Number
     */
    private void savePanelNumber(final int panel_num) {
        /*2012-3-6, add by bvq783 for IKSWITCHUI-94 as panel is not specified*/
        SCREEN_COUNT = panel_num;
        /*2012-3-6, add end*/
        SharedPreferences sharedPrefs = getSharedPreferences("PanelNumbers", Activity.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPrefs.edit();
        if (sharedPrefs != null) {
            editor.putInt("Panel", panel_num);
            editor.commit();
        }
    }
    //2012-03-05 added by rjg678 for IKSWITCHUI-88
    private void removeRelatedItemInfo( ItemInfo item ){

        if(item instanceof ShortcutInfo){
            LauncherModel.deleteItemFromDatabase(this, item);
        }else if (item instanceof LauncherAppWidgetInfo){
            // Remove the widget from the workspace
            removeAppWidget((LauncherAppWidgetInfo) item);
            LauncherModel.deleteItemFromDatabase(this, item);

            final LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) item;
            final LauncherAppWidgetHost appWidgetHost = getAppWidgetHost();
            if (appWidgetHost != null) {
                // Deleting an app widget ID is a void call but writes to disk before returning
                // to the caller...
                new Thread("deleteAppWidgetId") {
                    public void run() {
                        appWidgetHost.deleteAppWidgetId(launcherAppWidgetInfo.appWidgetId);
                    }
                }.start();
            }
        }else if (item instanceof FolderInfo){
            // Remove the folder from the workspace and delete the contents from launcher model
            FolderInfo folderInfo = (FolderInfo) item;
            removeFolder(folderInfo);
            LauncherModel.deleteFolderContentsFromDatabase(this, folderInfo);
        /*2012-6-7, add by bvq783 for switchui-1936*/
        }else if (item instanceof LauncherPluginWidgetInfo) {
            //Remove pluginwidget from workspace and delete the db
            LauncherModel.deleteItemFromDatabase(this, item);
        /*2012-6-7, add by bvq783 end*/		
        }
		
    }

    private void removeItems(final int id){

        CellLayout child = (CellLayout) mWorkspace.getChildAt(id);
        for (int i = 0; i < child.getChildCount(); i++){
            if( child.getChildAt(i) instanceof ShortcutAndWidgetContainer ){
                ViewGroup cChild = (ViewGroup) child.getChildAt(i);
                for (int t = 0; t < cChild.getChildCount(); t++) {
                    View ccChild =  cChild.getChildAt(t);
                    ItemInfo item = (ItemInfo) ccChild.getTag();
                    removeRelatedItemInfo(item);
                }
            }
        }
    }
    //end by rjg678
    /*
     *   Update the items info which in CellLayout
     */
    private void updateCellLayoutTag(final int fromId, final int toId) {
        for (int k = fromId; k < toId; k++) {
        
            CellLayout child = (CellLayout) mWorkspace.getChildAt(k);
            
            //CellLayout just has one child which is CellLayoutChildren.
            for (int i = 0; i < child.getChildCount(); i++) {

                //CellLayoutChildren
                // 2011-01-31 Updated by RJG678 for Pinch Panel
                // CellLayout has more than one Children, so it should be
                // checked if the Child is CellLayoutChildren
                if( child.getChildAt(i) instanceof ShortcutAndWidgetContainer ){
                	
                    ViewGroup cChild = (ViewGroup) child.getChildAt(i);
                    
                    for (int t = 0; t < cChild.getChildCount(); t++) {
                        View ccChild = cChild.getChildAt(t);
                        if (ccChild.getTag() instanceof ItemInfo) {
                            ((ItemInfo) ccChild.getTag()).screen = k;
                        }
                    }
                }
            }
            child.updateCellInfo();
        }
    }

    //2012-02-29 added by RJG678 for IKSWITCHUI-73

    /*
     * Phone UI is in error sometimes, so check the status of PhoneUI
     */
    private boolean isPhoneUINormal(View anchor){

        int width = anchor.getWidth();
        int height = getWindowManager().getDefaultDisplay().getHeight();

        if( width <= 0 || height <=0 ) {
            Log.e(TAG, "Home cell size is zero, phone UI is in error.");
            return false;
        }
        return true;
    }
    //end by RJG678

    public void showDragPanelView(View anchor){
        /*2012-06-06 added by Bai Jian for SWITCHUI-1352*/
        /*2012-06-20 added by Bai Jian for SWITCHUI-1517*/
    	/*2012-11-21 added by songshun.zhang for SWITCHUITWO-79 */
	 if (mWorkspace.isPageMoving() ||
	     mDragPanel.getVisibility() == View.VISIBLE ||
	     mWorkspace.isSwitchingState()) return;
	 /*2012-11-21 end*/
	 /*2012-06-20 end*/
        /*2012-06-06 end*/

        //2012-02-29 added by RJG678 for IKSWITCHUI-73
        //make sure the Phone UI is normal before to show the Panel
        if ( !isPhoneUINormal( anchor ) ){
            return;
        }
        //end by RJG678
        /*2012-05-03, add by Chen Yidong for SWITCHUI-909*/
        /*if (mDragController != null && mDragController.mBtnTimer) {
            mDragController.stopBtnTimer();
        }*/
        /*2012-05-03, add end*/
        /*2012-7-2, add by bvq783 for SWITCHUI-1787*/
        stopUnlockAnimation();
        /*2012-7-2, add by bvq783 end*/
        showDragPanelView(anchor,0,mWorkspace.getChildCount());

        /*2012-06-06, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/
        //showAppsCustomizeHelper(State.PANELVIEW, false, false);
        /*2012-06-06, end*/

        // Hide the search bar and hotseat
        mSearchDropTargetBar.hideSearchBar(false);
        // 2011-02-07 Changed by RJG678 for Pinch Panel
        //2012-03-06 added by RJG678 for IKSWITCHUI-95
        if (!LauncherApplication.isScreenLarge()) {
            mHotseat.setVisibility(View.GONE);
            hideHotseat(true);
        }
        /*2012-05-31, Modified by e13775 for SWITCHUI-1309*/
        mWorkspace.setVisibility(View.INVISIBLE);
        /*2012-05-31, Modified by e13775 for SWITCHUI-1309 end*/
        
        // Changed by RJG678 END
        mState = State.PANELVIEW;
        /*2012-6-18, add by bvq783 for switchui-1539*/
        mWorkspace.hideScrollingIndicator(true);
	/*2012-6-18, add end*/
        hideDockDivider();
        
        /* 2012-04-07 Modified by e13775 for MMCPPROJECT-28*/
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        /* 2012-04-07End by e13775 for MMCPPROJECT-28*/
        
        mDragPanel.setFocusable(true);
        mDragPanel.requestFocus();
    	
        // Pause the auto-advance of widgets
        mUserPresent = false;
        updateRunning();
        
        // send an accessibility event to announce the context change
        getWindow().getDecorView().sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        
    }
    
    private void showDragPanelView(final View anchor, int start, int end){
        final Resources resources = getResources();
        final Workspace workspace = mWorkspace;
        CellLayout cell = ((CellLayout) workspace.getChildAt(start));

        float max = workspace.getChildCount();

        int count = end - start;

        float scale = mScale;
        int sWidth = (int)(cell.getWidth()*scale);
        int sHeight = (int)(cell.getHeight()*scale);

        DragPanelViewClickHandler handler = new DragPanelViewClickHandler();
       
        //set Callback for Panel View
        mDragPanel.setDragPanelListener(handler);
        mDragPanel.setHighlight(mWorkspace.getCurrentPage());
        // 2011-01-31 Deleted by RJG678 for Pinch Panel
        // mDragPanel.setTitle("Theme: ");
        /*2012-6-11, add by bvq783 for pinchpanel crash*/
        if (mPanelBitmaps.size() > 0) {
            for (Bitmap bitmap : mPanelBitmaps) {
                if (!bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
            mPanelBitmaps.clear();
        }
        /*2012-6-11, add end*/
        /*2012-08-09, added by Bai Jian SWITCHUI-2528*/
        if (mOnePreivewBitmap != null) {
            if (!mOnePreivewBitmap.isRecycled()) {
                mOnePreivewBitmap.recycle();
                mOnePreivewBitmap = null;
            }
        }
        /*2012-08-09, add end*/
        for (int i = start; i < end; i++) {
            cell = (CellLayout) workspace.getChildAt(i);
            Bitmap bitmap = null;
            // Sometimes this throws an exception, in that case,
            // use a smaller scale.
            try{
                /*2012-06-06, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/
                if ( i == mDragPanel.getHighlight() ) {
                    bitmap = Bitmap.createBitmap(cell.getWidth(),cell.getHeight(),
                            Bitmap.Config.ARGB_8888);
                } else {
                    bitmap = Bitmap.createBitmap((int) sWidth, (int) sHeight,
                            Bitmap.Config.ARGB_8888);
                }
                /*2012-06-06, end*/
            }catch (OutOfMemoryError e) {

                final Rect r = new Rect();
                // Sometimes this throws an exception, in that case, 
                // use an empty rect anyway
                try{
                    resources.getDrawable(R.drawable.preview_background).getPadding(r);
                }catch (OutOfMemoryError ex){}
                

                int extraW = (int) ((r.left + r.right) * max);
                int extraH = r.top + r.bottom;

                int aW = cell.getWidth() - extraW;
                float w = aW / count;

                int width = cell.getWidth();
                int height = cell.getHeight();

                scale = w / width;

                sWidth = (int) (width * scale);
                sHeight = (int) (height * scale);
                try  {
                    /*2012-06-06, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/
                    if ( i == mDragPanel.getHighlight() ) {
                        bitmap = Bitmap.createBitmap(cell.getWidth(),cell.getHeight(),
                                Bitmap.Config.ARGB_8888);
                    } else {
                        bitmap = Bitmap.createBitmap((int) sWidth, (int) sHeight,
                                Bitmap.Config.ARGB_8888);
                    }
                    /*2012-06-06, end*/
                } catch (OutOfMemoryError error) {
                    // If after all effort to get bitmap, it is still
                    // not possible to create one, do not create this specific preview
                    bitmap = null;
                }
            }

            final Canvas c = new Canvas(bitmap);
            /*2012-06-06, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/
            if (i != mDragPanel.getHighlight()) c.scale(scale, scale);
            /*2012-06-06, end*/
//            c.translate(-cell.getLeftPadding(), -cell.getTopPadding());
            /*2012-07-20, added by Bai Jian SWITCHUI-2327*/
            c.translate(cell.getPaddingLeft(), 0);
            /*2012-07-20, end*/
            cell.drawChildren(c);


            mPanelBitmaps.add(bitmap);
            
        }
        mDragPanel.updateAll(mPanelBitmaps);
        /*2012-06-06, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/
        mDragPanel.show();
        /*2012-06-06, end*/
    }

    /*2012-08-09, added by Bai Jian SWITCHUI-2528*/
    private void updateOnePinchPanel(int index){
        CellLayout cell = (CellLayout)mWorkspace.getChildAt(index);
        int width = cell.getWidth();
        int height = cell.getHeight();
        // Sometimes this throws an exception, in that case,
        // use a null.
        try{
            mOnePreivewBitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);
        }catch (OutOfMemoryError e) {
            mOnePreivewBitmap = null;
        }
        if ( mOnePreivewBitmap != null ) {
            final Canvas c = new Canvas(mOnePreivewBitmap);
            c.translate(cell.getPaddingLeft(), 0);
            cell.drawChildren(c);
            mDragPanel.updateOne(index, mOnePreivewBitmap);
        }
    }
    /*2012-08-09, add end*/

    class DragPanelViewClickHandler implements DragPanel.DragPanelListener{

        @Override
        public void onAddItem() {
            // TODO Auto-generated method stub
            CellLayout child = (CellLayout) mInflater.inflate(R.layout.workspace_screen, null);
            child.setOnLongClickListener(Launcher.this);
            mWorkspace.addView(child, mWorkspace.getChildCount());
            savePanelNumber(mWorkspace.getChildCount());
            //2012-02-17 added by rjg678 for IKSWITCHUI-21
            mWorkspace.onUpdateDefaultPage();
            // end by rjg678
        }

        /*2012-06-06, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/
        @Override
        public void onHideAnimationStart(int index) {
            // TODO Auto-generated method stub
            mWorkspace.setVisibility(View.VISIBLE);
            mWorkspace.setAlpha(0f);
            //modified by amt_wangpeipei 2013/01/14 for switchuitwo-468 begin
            //mHandler.sendEmptyMessage(SNAP_TO_PAGE_MSG);
            mWorkspace.mWithEffect = false;
            mWorkspace.snapToPage(mDragPanel.getHighlight(), 300);
            mSearchDropTargetBar.showSearchBar(true);
            if (!LauncherApplication.isScreenLarge()) {
                showHotseat(true);
                mHotseat.setVisibility(View.VISIBLE);
            }
            //modified by amt_wangpeipei 2013/01/14 for switchuitwo-468 end
        }

        @Override
        public void onHideEnd(boolean animated, final int index) {
            // TODO Auto-generated method stub
            if (animated) {
                mWorkspace.setAlpha(1f);
            } else {
                mWorkspace.setVisibility(View.VISIBLE);
                mWorkspace.setAlpha(1f);
                mSearchDropTargetBar.showSearchBar(true);
                if (!LauncherApplication.isScreenLarge()) {
                    showHotseat(true);
                    mHotseat.setVisibility(View.VISIBLE);
                }
            }
            if (mAllAppsButton != null) {
                mAllAppsButton.requestFocus();
            }
            for (Bitmap bitmap : mPanelBitmaps) {
                bitmap.recycle();
            }
            mPanelBitmaps.clear();
            /*2012-08-09, added by Bai Jian SWITCHUI-2528*/
            if (mOnePreivewBitmap != null) {
                if (!mOnePreivewBitmap.isRecycled()) {
                    mOnePreivewBitmap.recycle();
                    mOnePreivewBitmap = null;
                }
            }
            /*2012-08-09, add end*/
        }
        /*2012-06-06, end*/

        @Override
        public void onClickItem(int index) {
            // TODO Auto-generated method stub
            //added by RJG678 for IKDOMINO-6133 2012-02-14
            /*2012-05-11, added by Bai Jian for SWITCHUI-1049*/
            /*2012-06-06, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/
            // mWorkspace.setCurrentPage(index);
            /*2012-06-06, end*/
            /*2012-05-11, added end*/
            
            Resources res = getResources();
            int stagger = res.getInteger(R.integer.config_appsCustomizeWorkspaceAnimationStagger);
            mWorkspace.getChangeStateAnimation(Workspace.State.NORMAL, true, stagger);
            
            /*2012-06-06, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/
            hidePanelView(true, index);
            /*2012-06-06, end*/
            /*2012-05-11, added by Bai Jian for SWITCHUI-1049*/
            //mWorkspace.setCurrentPage(index);
            /*2012-05-11, added end*/
            showDockDivider(true);
            mWorkspace.flashScrollingIndicator(true);
            
            // Change the state after we've called all the transition code
            mState = State.WORKSPACE;

            // Resume the auto-advance of widgets
            mUserPresent = true;
            updateRunning();

            // send an accessibility event to announce the context change
            getWindow().getDecorView().sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
            
            
            //added by RJG678 end
        }

        @Override
        public void onRemoveItem(int index) {
            // TODO Auto-generated method stub
            Context context = (Context) (Launcher.this);
            //2012-03-05 added by rjg678 for IKSWITCHUI-88
            removeItems(index);
            //end by rjg678

            //forbid to remove the last screen
            /*2012-2-27, add by bvq783 */
            removeItemsbyScreen(index);
            /*2012-2-27, end */
            mWorkspace.removeViewAt(index);
            savePanelNumber(mWorkspace.getChildCount());

            updateCellLayoutTag(index, mWorkspace.getChildCount());

            //Update the CurrentPage
            if (index > 0 && index < mWorkspace.getChildCount()) {
                mWorkspace.updateCurrentPage(index);
            } else {
                mWorkspace.updateCurrentPage(mWorkspace.getChildCount() - 1);
            }
            //2012-02-17 added by rjg678 for IKSWITCHUI-21
            mWorkspace.onUpdateDefaultPage();
            // end by rjg678
            // End loop
            //2012-03-05 deleted by rjg678 for IKSWITCHUI-88
            // LauncherModel.deleteItemsByScreenID(context, index);
            LauncherModel.updateScreenID(context, index, mWorkspace.getChildCount());
        }

		@Override
		public void onReorderItems(int fromIndex, int toIndex) {
			// TODO Auto-generated method stub
			if (fromIndex == toIndex)
				return;

			Context context = (Context) (Launcher.this);
			CellLayout child = (CellLayout) mWorkspace.getChildAt(fromIndex);
	        //the child sometimes is null
			child.setOnLongClickListener(Launcher.this);
			mWorkspace.detachView(fromIndex);
			mWorkspace.addView(child, toIndex);

			if (fromIndex > toIndex) {
				updateCellLayoutTag(toIndex, fromIndex + 1);
			} else {
				updateCellLayoutTag(fromIndex, toIndex + 1);
			}
			LauncherModel.updateScreenID(context, fromIndex, toIndex);
		}
    	
       /*2012-6-25, add by bvq783 for switchui-1581*/
       @Override
       public void onLaunchTransition() {
    	   //modified by amt_wangpeipei 2013/01/14 for switchuitwo-469 begin
    	   if(mState == State.PANELVIEW){
    		   Intent intent = new Intent(Launcher.this,TransitionSettingActivity.class);
    		   startActivityForResult(intent,REQUEST_TRANSITION_EFFECT);
    	   }
    	   //modified by amt_wangpeipei 2013/01/14 for switchuitwo-469 end
       }
       /*2012-6-25, end*/
    }
    
    /*2012-06-06, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/
    void hidePanelView(boolean animated, int index) {
        if (mState == State.PANELVIEW) {
            /*2012-08-09, added by Bai Jian SWITCHUI-2528*/
            if (animated) updateOnePinchPanel(index);
            /*2012-08-09, add end*/
            mDragPanel.hide(animated,index);
            /*
            hideAppsCustomizeHelper(State.PANELVIEW, false,false);
            
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            // 2011-02-07 Updated by RJG678 for Pinch Panel
            mWorkspace.setVisibility(View.VISIBLE);
            
            // Show the search bar and hotseat
            mSearchDropTargetBar.showSearchBar(animated);
            // 2012-02-23 added by rjg678 for IKSWITCHUI-49
            //2012-03-06 added by RJG678 for IKSWITCHUI-95
            if (!LauncherApplication.isScreenLarge()) {
                showHotseat(true);
                mHotseat.setVisibility(View.VISIBLE);
            }
            // 2012-01-31 Updated by RJG678 END

            // Set focus to the AppsCustomize button
            if (mAllAppsButton != null) {
                mAllAppsButton.requestFocus();
            }
            for (Bitmap bitmap : mPanelBitmaps) {
            	bitmap.recycle();
            }
            mPanelBitmaps.clear();
            */
        }
    }
    /*2012-06-06, end*/
    
    //RJG678 Pinch Panel END
    
    /**
     * Creates a view representing a shortcut.
     *
     * @param info The data structure describing the shortcut.
     *
     * @return A View inflated from R.layout.application.
     */
    View createShortcut(ShortcutInfo info) {
        return createShortcut(R.layout.application,
                (ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentPage()), info);
    }

    /**
     * Creates a view representing a shortcut inflated from the specified resource.
     *
     * @param layoutResId The id of the XML layout used to create the shortcut.
     * @param parent The group the shortcut belongs to.
     * @param info The data structure describing the shortcut.
     *
     * @return A View inflated from layoutResId.
     */
    View createShortcut(int layoutResId, ViewGroup parent, ShortcutInfo info) {
        BubbleTextView favorite = (BubbleTextView) mInflater.inflate(layoutResId, parent, false);
        favorite.applyFromShortcutInfo(info, mIconCache);
        favorite.setOnClickListener(this);
        return favorite;
    }

    /**
     * Add an application shortcut to the workspace.
     *
     * @param data The intent describing the application.
     * @param cellInfo The position on screen where to create the shortcut.
     */
    void completeAddApplication(Intent data, long container, int screen, int cellX, int cellY) {
        final int[] cellXY = mTmpAddItemCellCoordinates;
        final CellLayout layout = getCellLayout(container, screen);

        // First we check if we already know the exact location where we want to add this item.
        if (cellX >= 0 && cellY >= 0) {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
        } else if (!layout.findCellForSpan(cellXY, 1, 1)) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        final ShortcutInfo info = mModel.getShortcutInfo(getPackageManager(), data, this);

        if (info != null) {
            info.setActivity(data.getComponent(), Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            info.container = ItemInfo.NO_ID;
            mWorkspace.addApplicationShortcut(info, layout, container, screen, cellXY[0], cellXY[1],
                    isWorkspaceLocked(), cellX, cellY);
        } else {
            Log.e(TAG, "Couldn't find ActivityInfo for selected application: " + data);
        }
    }

    /**
     * Add a shortcut to the workspace.
     *
     * @param data The intent describing the shortcut.
     * @param cellInfo The position on screen where to create the shortcut.
     */
    private void completeAddShortcut(Intent data, long container, int screen, int cellX,
            int cellY) {
        int[] cellXY = mTmpAddItemCellCoordinates;
        int[] touchXY = mPendingAddInfo.dropPos;
        CellLayout layout = getCellLayout(container, screen);

        boolean foundCellSpan = false;

        ShortcutInfo info = mModel.infoFromShortcutIntent(this, data, null);
        if (info == null) {
            return;
        }
        final View view = createShortcut(info);

        // First we check if we already know the exact location where we want to add this item.
        if (cellX >= 0 && cellY >= 0) {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
            foundCellSpan = true;

            // If appropriate, either create a folder or add to an existing folder
            if (mWorkspace.createUserFolderIfNecessary(view, container, layout, cellXY, 0,
                    true, null,null)) {
                return;
            }
            DragObject dragObject = new DragObject();
            dragObject.dragInfo = info;
            if (mWorkspace.addToExistingFolderIfNecessary(view, layout, cellXY, 0, dragObject,
                    true)) {
                return;
            }
        } else if (touchXY != null) {
            // when dragging and dropping, just find the closest free spot
            int[] result = layout.findNearestVacantArea(touchXY[0], touchXY[1], 1, 1, cellXY);
            foundCellSpan = (result != null);
        } else {
            foundCellSpan = layout.findCellForSpan(cellXY, 1, 1);
        }

        if (!foundCellSpan) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        LauncherModel.addItemToDatabase(this, info, container, screen, cellXY[0], cellXY[1], false);

        if (!mRestoring) {
            mWorkspace.addInScreen(view, container, screen, cellXY[0], cellXY[1], 1, 1,
                    isWorkspaceLocked());
        }
    }

    static int[] getSpanForWidget(Context context, ComponentName component, int minWidth,
            int minHeight) {
        Rect padding = AppWidgetHostView.getDefaultPaddingForWidget(context, component, null);
        // We want to account for the extra amount of padding that we are adding to the widget
        // to ensure that it gets the full amount of space that it has requested
        int requiredWidth = minWidth + padding.left + padding.right;
        int requiredHeight = minHeight + padding.top + padding.bottom;
        return CellLayout.rectToCell(context.getResources(), requiredWidth, requiredHeight, null);
    }

    static int[] getSpanForWidget(Context context, AppWidgetProviderInfo info) {
        return getSpanForWidget(context, info.provider, info.minWidth, info.minHeight);
    }

    static int[] getMinSpanForWidget(Context context, AppWidgetProviderInfo info) {
        return getSpanForWidget(context, info.provider, info.minResizeWidth, info.minResizeHeight);
    }

    static int[] getSpanForWidget(Context context, PendingAddWidgetInfo info) {
        return getSpanForWidget(context, info.componentName, info.minWidth, info.minHeight);
    }

    static int[] getMinSpanForWidget(Context context, PendingAddWidgetInfo info) {
        return getSpanForWidget(context, info.componentName, info.minResizeWidth,
                info.minResizeHeight);
    }

    /**
     * Add a widget to the workspace.
     *
     * @param appWidgetId The app widget id
     * @param cellInfo The position on screen where to create the widget.
     */
    private void completeAddAppWidget(final int appWidgetId, long container, int screen,
            AppWidgetHostView hostView, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo == null) {
            appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        }

        // Calculate the grid spans needed to fit this widget
        CellLayout layout = getCellLayout(container, screen);

        int[] minSpanXY = getMinSpanForWidget(this, appWidgetInfo);
        int[] spanXY = getSpanForWidget(this, appWidgetInfo);

        // Try finding open space on Launcher screen
        // We have saved the position to which the widget was dragged-- this really only matters
        // if we are placing widgets on a "spring-loaded" screen
        int[] cellXY = mTmpAddItemCellCoordinates;
        int[] touchXY = mPendingAddInfo.dropPos;
        int[] finalSpan = new int[2];
        boolean foundCellSpan = false;
        if (mPendingAddInfo.cellX >= 0 && mPendingAddInfo.cellY >= 0) {
            cellXY[0] = mPendingAddInfo.cellX;
            cellXY[1] = mPendingAddInfo.cellY;
            spanXY[0] = mPendingAddInfo.spanX;
            spanXY[1] = mPendingAddInfo.spanY;
            foundCellSpan = true;
        } else if (touchXY != null) {
            // when dragging and dropping, just find the closest free spot
            int[] result = layout.findNearestVacantArea(
                    touchXY[0], touchXY[1], minSpanXY[0], minSpanXY[1], spanXY[0],
                    spanXY[1], cellXY, finalSpan);
            spanXY[0] = finalSpan[0];
            spanXY[1] = finalSpan[1];
            foundCellSpan = (result != null);
        } else {
            foundCellSpan = layout.findCellForSpan(cellXY, minSpanXY[0], minSpanXY[1]);
        }

        if (!foundCellSpan) {
            if (appWidgetId != -1) {
                // Deleting an app widget ID is a void call but writes to disk before returning
                // to the caller...
                new Thread("deleteAppWidgetId") {
                    public void run() {
                        mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                    }
                }.start();
            }
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        // Build Launcher-specific widget info and save to database
        LauncherAppWidgetInfo launcherInfo = new LauncherAppWidgetInfo(appWidgetId,
                appWidgetInfo.provider);
        launcherInfo.spanX = spanXY[0];
        launcherInfo.spanY = spanXY[1];
        launcherInfo.minSpanX = mPendingAddInfo.minSpanX;
        launcherInfo.minSpanY = mPendingAddInfo.minSpanY;

        LauncherModel.addItemToDatabase(this, launcherInfo,
                container, screen, cellXY[0], cellXY[1], false);

        if (!mRestoring) {
            if (hostView == null) {
                // Perform actual inflation because we're live
                launcherInfo.hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
                launcherInfo.hostView.setAppWidget(appWidgetId, appWidgetInfo);
            } else {
                // The AppWidgetHostView has already been inflated and instantiated
                launcherInfo.hostView = hostView;
            }

            launcherInfo.hostView.setTag(launcherInfo);
            launcherInfo.hostView.setVisibility(View.VISIBLE);
            launcherInfo.notifyWidgetSizeChanged(this);
            mWorkspace.addInScreen(launcherInfo.hostView, container, screen, cellXY[0], cellXY[1],
                    launcherInfo.spanX, launcherInfo.spanY, isWorkspaceLocked());

            addWidgetToAutoAdvanceIfNeeded(launcherInfo.hostView, appWidgetInfo);
        }
        resetAddInfo();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /*2012-07-19, ChenYidong added for SWITCHUI-1926*/
            boolean needAnimation = false;
            try {
                IWindowManager wm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
                //get window animation scale
                float scale = wm.getAnimationScale(0);
                needAnimation = scale > 0.1f;
            } catch (RemoteException e) {
            }
            /*2012-07-19, end*/
            final String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                mUserPresent = false;
                mDragLayer.clearAllResizeFrames();
                updateRunning();

                // Reset AllApps to its initial state only if we are not in the middle of
                // processing a multi-step drop
                if (mAppsCustomizeTabHost != null && mPendingAddInfo.container == ItemInfo.NO_ID) {
                    mAppsCustomizeTabHost.reset();
                    // Added by e13775 at 19 June 2012 for organize apps' group start
                    if (mAllAppsPage != null){
                        mAllAppsPage.resetAllAppsState();
                    }
		    // Added by e13775 at 19 June 2012 for organize apps' group end
                    showWorkspace(false);
                }
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                mUserPresent = true;
                updateRunning();
                /*2012-07-19, ChenYidong Changed for SWITCHUI-1926*/
                if(needAnimation){
                    /*add by bvq783 for entering transition*/
                  if (mPaused || mWorkspaceLoading) {
                  } else {
                     /*2012-6-17, add by bvq783 for switchui-1432*/
                     if (et != null) {
                    	View cell = mWorkspace.getChildAt(getCurrentWorkspaceScreen());
                    	et.initUnlockPage(cell, mSearchDropTargetBar, mHotseat, hotseatbg);
                     }
                     /*2012-6-17, add end*/
                     showUnlockAnimation();
                  }
                }
                /*2012-07-19, end*/
          }  else if (Intent.ACTION_SCREEN_ON.equals(action) && needAnimation/*2012-07-19, for SWITCHUI-1926*/) {
              ContentResolver resolver  =  getContentResolver();
              int index = (int)(android.provider.Settings.Secure.getLong(resolver, PASSWORD_TYPE,
            		  /*DevicePolicyManager.PASSWORD_QUALITY_SOMETHING*/65536));
                 // Log.d("July", "get unlock type:"+index);
              int auto = android.provider.Settings.Secure.getInt(resolver,
              		    Settings.Secure.LOCK_PATTERN_ENABLED, 0);
              int dis = android.provider.Settings.Secure.getInt(resolver,
                 	    DISABLE_LOCKSCREEN, 0);
              if (index == 65536 && auto == 0 && dis == 1) {
                	  //it's none unlock type
                   if (mPaused || mWorkspaceLoading) {
                   } else {
                       /*2012-6-17, add by bvq783 for switchui-1432*/
                       if (et != null) {
                	      View cell = mWorkspace.getChildAt(getCurrentWorkspaceScreen());
                	      et.initUnlockPage(cell, mSearchDropTargetBar, mHotseat, hotseatbg);
                       }
                       /*2012-6-17, add end*/
                       showUnlockAnimation();
                   }
              }
              /*add by bvq783 end*/
            }
        }
    };

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Listen for broadcasts related to user-presence
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mReceiver, filter);

        mAttached = true;
        mVisible = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mVisible = false;

        if (mAttached) {
            unregisterReceiver(mReceiver);
            mAttached = false;
        }
        updateRunning();
    }

    public void onWindowVisibilityChanged(int visibility) {
        mVisible = visibility == View.VISIBLE;
        updateRunning();
        // The following code used to be in onResume, but it turns out onResume is called when
        // you're in All Apps and click home to go to the workspace. onWindowVisibilityChanged
        // is a more appropriate event to handle
        if (mVisible) {
            mAppsCustomizeTabHost.onWindowVisible();
            if (!mWorkspaceLoading) {
                final ViewTreeObserver observer = mWorkspace.getViewTreeObserver();
                // We want to let Launcher draw itself at least once before we force it to build
                // layers on all the workspace pages, so that transitioning to Launcher from other
                // apps is nice and speedy. Usually the first call to preDraw doesn't correspond to
                // a true draw so we wait until the second preDraw call to be safe
                observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    public boolean onPreDraw() {
                        // We delay the layer building a bit in order to give
                        // other message processing a time to run.  In particular
                        // this avoids a delay in hiding the IME if it was
                        // currently shown, because doing that may involve
                        // some communication back with the app.
                        mWorkspace.postDelayed(mBuildLayersRunnable, 500);

                        observer.removeOnPreDrawListener(this);
                        return true;
                    }
                });
            }
            // When Launcher comes back to foreground, a different Activity might be responsible for
            // the app market intent, so refresh the icon
            updateAppMarketIcon();
            clearTypedText();
        }
    }

    private void sendAdvanceMessage(long delay) {
        mHandler.removeMessages(ADVANCE_MSG);
        Message msg = mHandler.obtainMessage(ADVANCE_MSG);
        mHandler.sendMessageDelayed(msg, delay);
        mAutoAdvanceSentTime = System.currentTimeMillis();
    }

    private void updateRunning() {
        boolean autoAdvanceRunning = mVisible && mUserPresent && !mWidgetsToAdvance.isEmpty();
        if (autoAdvanceRunning != mAutoAdvanceRunning) {
            mAutoAdvanceRunning = autoAdvanceRunning;
            if (autoAdvanceRunning) {
                long delay = mAutoAdvanceTimeLeft == -1 ? mAdvanceInterval : mAutoAdvanceTimeLeft;
                sendAdvanceMessage(delay);
            } else {
                if (!mWidgetsToAdvance.isEmpty()) {
                    mAutoAdvanceTimeLeft = Math.max(0, mAdvanceInterval -
                            (System.currentTimeMillis() - mAutoAdvanceSentTime));
                }
                mHandler.removeMessages(ADVANCE_MSG);
                mHandler.removeMessages(0); // Remove messages sent using postDelayed()
            }
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ADVANCE_MSG) {
                int i = 0;
                for (View key: mWidgetsToAdvance.keySet()) {
                    final View v = key.findViewById(mWidgetsToAdvance.get(key).autoAdvanceViewId);
                    final int delay = mAdvanceStagger * i;
                    if (v instanceof Advanceable) {
                       postDelayed(new Runnable() {
                           public void run() {
                               ((Advanceable) v).advance();
                           }
                       }, delay);
                    }
                    i++;
                }
                sendAdvanceMessage(mAdvanceInterval);
            }
            /*2012-06-06, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/
            else if (msg.what == SNAP_TO_PAGE_MSG) {
                mWorkspace.mWithEffect = false;
                mWorkspace.snapToPage(mDragPanel.getHighlight(), 300);
                mSearchDropTargetBar.showSearchBar(true);
                if (!LauncherApplication.isScreenLarge()) {
                    showHotseat(true);
                    mHotseat.setVisibility(View.VISIBLE);
                }
            }
            /*2012-07-26, Added by amt_chenjing for SWITCHUI-2469*/
            else if (msg.what == SHOW_ALL_APPS) {
                if(mPaused) {
                    return;
                }
                showAllApps(true);
            }
            /*2012-07-26, end*/
        }
    };

    void addWidgetToAutoAdvanceIfNeeded(View hostView, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo == null || appWidgetInfo.autoAdvanceViewId == -1) return;
        View v = hostView.findViewById(appWidgetInfo.autoAdvanceViewId);
        if (v instanceof Advanceable) {
            mWidgetsToAdvance.put(hostView, appWidgetInfo);
            ((Advanceable) v).fyiWillBeAdvancedByHostKThx();
            updateRunning();
        }
    }

    void removeWidgetToAutoAdvance(View hostView) {
        if (mWidgetsToAdvance.containsKey(hostView)) {
            mWidgetsToAdvance.remove(hostView);
            updateRunning();
        }
    }

    public void removeAppWidget(LauncherAppWidgetInfo launcherInfo) {
        removeWidgetToAutoAdvance(launcherInfo.hostView);
        launcherInfo.hostView = null;
    }

    void showOutOfSpaceMessage(boolean isHotseatLayout) {
        int strId = (isHotseatLayout ? R.string.hotseat_out_of_space : R.string.out_of_space);
        Toast.makeText(this, getString(strId), Toast.LENGTH_SHORT).show();
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    void closeSystemDialogs() {
        getWindow().closeAllPanels();

        // Whatever we were doing is hereby canceled.
        mWaitingForResult = false;
        /*212-05-11, added by Chen Yidong, for SWITCHUI-1016*/
        if(mPreferenceSettingDialog != null && mPreferenceSettingDialog.isShowing()){
            mPreferenceSettingDialog.cancel();
        }
        /*2012-05-11, added end*/
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Close the menu
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            // also will cancel mWaitingForResult.
            closeSystemDialogs();

            boolean alreadyOnHome = ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                        != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

            Folder openFolder = mWorkspace.getOpenFolder();
            // In all these cases, only animate if we're already on home
            mWorkspace.exitWidgetResizeMode();
            //2012-02-17 updated by rjg678 for IKSWITCHUI-23
            boolean needToMoveScreen = !mWorkspace.isDefaultScreenShowing();
            
            if (alreadyOnHome && mState == State.WORKSPACE && !mWorkspace.isTouchActive() &&
                    openFolder == null) {
            	/*2012-11-19, Modifyed by amt_chenjing for switchuitwo-37*/
            	/*2012-07-22, Chen Yidong for SWITCHUI-2388*/
                if(mWorkspace.isPageMoving() && !needToMoveScreen 
                        && mWorkspace.mNextPage != mWorkspace.INVALID_PAGE
                        && mWorkspace.mNextPage != mWorkspace.mCurrentPage){
                    mWorkspace.mCurrentPage = mWorkspace.mNextPage;
                    needToMoveScreen = true;
                }
                if(needToMoveScreen) {
                	/*2012-06-20, add by bvq783 for SWITCHUI-1545*/
        	        if (!mWorkspace.isSmall() && !mWorkspace.isSwitchingState() && mWorkspace.pt != null
        	                && (mWorkspace.isPageMoving() || (mWorkspace.pt.ani != null && mWorkspace.pt.ani.play))) {
        	             mWorkspace.pt.resetPagedView(mWorkspace);
        	        }
        	        /*2012-06-20, end*/
                    mWorkspace.moveToDefaultScreen(true);
                } else if (hasWindowFocus()) {
                	showDragPanelView(findViewById(R.id.drag_layer));
                }
            /*2012-11-21, add by 003033 for returning to worksapce when press home key*/    
            } else if( mState != State.WORKSPACE 
                   || mOnResumeState == State.APPS_CUSTOMIZE){
                /*2012-11-26, add by 003033 for switchuitwo-121*/
                if (mState == State.PANELVIEW && mDragPanel.isDragging()) {
                   MotionEvent cancel = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0,0,0,0,0,0,0,0,0);
                   mDragPanel.dispatchTouchEvent(cancel);
                }
                /*2012-11-26, add end*/
                /*2012-11-30, Modifyed by amt_chenjing for switchuitwo-155*/
                //showWorkspace(alreadyOnHome);
                mOnResumeState = State.WORKSPACE;
            } else if (mState == State.WORKSPACE) {
            	showWorkspace(alreadyOnHome);
            }
            /*2012-11-30, Modify end*/
            closeFolder();
            exitSpringLoadedDragMode();

            // If we are already on home, then just animate back to the workspace, otherwise, just
            // wait until onResume to set the state back to Workspace
//            if (alreadyOnHome) {
//                showWorkspace(true);
//            } else {
//                mOnResumeState = State.WORKSPACE;
//            }
            /*2012-11-19, Modify end*/

            final View v = getWindow().peekDecorView();
            if (v != null && v.getWindowToken() != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(
                        INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }

            // Reset AllApps to its initial state
            if (!alreadyOnHome && mAppsCustomizeTabHost != null) {
                mAppsCustomizeTabHost.reset();
            }

            // Added by e13775 at 19 June 2012 for organize apps' group start
            if (mAllAppsPage != null){
                mAllAppsPage.resetAllAppsState();
            }
            /*2012-11-27, Added by amt_chenjing for SWITCHUITWO-135*/
			if ((mAppsCustomizeContent.pt != null && mAppsCustomizeContent
					.isPageMoving())
					|| (mAppsCustomizeContent.pt.ani != null && mAppsCustomizeContent.pt.ani.play)) {
				mAppsCustomizeContent.pt.resetPagedView(mAppsCustomizeContent);
			}
            /*2012-11-27, Add end*/
	    // Added by e13775 at 19 June 2012 for organize apps' group end
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        for (int page: mSynchronouslyBoundPages) {
            mWorkspace.restoreInstanceStateForChild(page);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(RUNTIME_STATE_CURRENT_SCREEN, mWorkspace.getNextPage());
        super.onSaveInstanceState(outState);

        outState.putInt(RUNTIME_STATE, mState.ordinal());
        // We close any open folder since it will not be re-opened, and we need to make sure
        // this state is reflected.
        closeFolder();

        if (mPendingAddInfo.container != ItemInfo.NO_ID && mPendingAddInfo.screen > -1 &&
                mWaitingForResult) {
            outState.putLong(RUNTIME_STATE_PENDING_ADD_CONTAINER, mPendingAddInfo.container);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SCREEN, mPendingAddInfo.screen);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_X, mPendingAddInfo.cellX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_Y, mPendingAddInfo.cellY);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_X, mPendingAddInfo.spanX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y, mPendingAddInfo.spanY);
            outState.putParcelable(RUNTIME_STATE_PENDING_ADD_WIDGET_INFO, mPendingAddWidgetInfo);
        }

        if (mFolderInfo != null && mWaitingForResult) {
            outState.putBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, true);
            outState.putLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID, mFolderInfo.id);
        }

        // Save the current AppsCustomize tab
        if (mAppsCustomizeTabHost != null) {
            String currentTabTag = mAppsCustomizeTabHost.getCurrentTabTag();
            if (currentTabTag != null) {
                outState.putString("apps_customize_currentTab", currentTabTag);
            }
            int currentIndex = mAppsCustomizeContent.getSaveInstanceStateIndex();
            outState.putInt("apps_customize_currentIndex", currentIndex);
        }

       // Added by e13775 at 19 June 2012 for organize apps' group start
        if (mAllAppsPage != null){
            outState.putBundle("allappspage_bundle", mAllAppsPage.getSaveInstanceState());
        }
        // Added by e13775 at 19 June 2012 for organize apps' group end

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Remove all pending runnables
        mHandler.removeMessages(ADVANCE_MSG);
        mHandler.removeMessages(0);
        mWorkspace.removeCallbacks(mBuildLayersRunnable);

        // Stop callbacks from LauncherModel
        LauncherApplication app = ((LauncherApplication) getApplication());
        mModel.stopLoader();
        app.setLauncher(null);
        /*2012-8-3, add by bvq783 for plugin*/
        if (mPluginWidgetHost != null) {
            mPluginWidgetHost.statusChange(mPluginWidgetHost.ONDESTROY);
            mPluginWidgetHost.onDestroy();
        }
        /*2012-8-3, add end*/

        try {
            mAppWidgetHost.stopListening();
        } catch (NullPointerException ex) {
            Log.w(TAG, "problem while stopping AppWidgetHost during Launcher destruction", ex);
        }
        mAppWidgetHost = null;

        mWidgetsToAdvance.clear();

        TextKeyListener.getInstance().release();


        unbindWorkspaceAndHotseatItems();
        mWorkspace.clearDropTargets(); //2012-08-16, add by bvq783

        getContentResolver().unregisterContentObserver(mWidgetObserver);
        unregisterReceiver(mCloseSystemDialogsReceiver);

        mDragLayer.clearAllResizeFrames();
        ((ViewGroup) mWorkspace.getParent()).removeAllViews();
        mWorkspace.removeAllViews();
        mWorkspace = null;
        mDragController = null;
        //RJG678 Pinch Panel
        if (mPanelBitmaps.size() > 0){
            for (Bitmap bitmap : mPanelBitmaps) {
                bitmap.recycle();
            }
            mPanelBitmaps.clear();
        }
        //RJG678 Pinch Panel END
        
        ValueAnimator.clearAllAnimations();
    }

    public DragController getDragController() {
        return mDragController;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (requestCode >= 0) mWaitingForResult = true;
        super.startActivityForResult(intent, requestCode);
    }

    /**
     * Indicates that we want global search for this activity by setting the globalSearch
     * argument for {@link #startSearch} to true.
     */
    @Override
    public void startSearch(String initialQuery, boolean selectInitialQuery,
            Bundle appSearchData, boolean globalSearch) {

        showWorkspace(true);

        if (initialQuery == null) {
            // Use any text typed in the launcher as the initial query
            initialQuery = getTypedText();
        }
        if (appSearchData == null) {
            appSearchData = new Bundle();
            appSearchData.putString(Search.SOURCE, "launcher-search");
        }
        Rect sourceBounds = mSearchDropTargetBar.getSearchBarBounds();

        final SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchManager.startSearch(initialQuery, selectInitialQuery, getComponentName(),
            appSearchData, globalSearch, sourceBounds);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isWorkspaceLocked()) {
            return false;
        }

        super.onCreateOptionsMenu(menu);

        Intent manageApps = new Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS);
        manageApps.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        Intent settings = new Intent(android.provider.Settings.ACTION_SETTINGS);
        settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        String helpUrl = getString(R.string.help_url);
        Intent help = new Intent(Intent.ACTION_VIEW, Uri.parse(helpUrl));
        help.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        menu.add(MENU_GROUP_WALLPAPER, MENU_WALLPAPER_SETTINGS, 0, R.string.menu_wallpaper)
            .setIcon(android.R.drawable.ic_menu_gallery)
            .setAlphabeticShortcut('W');
        //hnd734 Home Transition start
        menu.add(0, MENU_TRANSITION_EFFECT, 0, R.string.transition);
        //hnd734 Home Transition end
        menu.add(0, MENU_MANAGE_APPS, 0, R.string.menu_manage_apps)
            .setIcon(android.R.drawable.ic_menu_manage)
            .setIntent(manageApps)
            .setAlphabeticShortcut('M');
        menu.add(0, MENU_SYSTEM_SETTINGS, 0, R.string.menu_settings)
            .setIcon(android.R.drawable.ic_menu_preferences)
            .setIntent(settings)
            .setAlphabeticShortcut('P');
        if (!helpUrl.isEmpty()) {
            menu.add(0, MENU_HELP, 0, R.string.menu_help)
                .setIcon(android.R.drawable.ic_menu_help)
                .setIntent(help)
                .setAlphabeticShortcut('H');
        }

        // Added by e13775 at 19 June 2012 for organize apps' group start
        mAllAppsPage.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mAppsCustomizeTabHost.isTransitioning()) {
            return false;
        }
        boolean allAppsVisible = (mAppsCustomizeTabHost.getVisibility() == View.VISIBLE);
        menu.setGroupVisible(MENU_GROUP_WALLPAPER, !allAppsVisible);
        // Added by e13775 at 19 June 2012 for organize apps' group start
        menu.setGroupVisible(MENU_GROUP_EDIT_APPS_GROUP, false);
        menu.setGroupVisible(MENU_GROUP_ADD_GROUP_TO_HOME, false);
        /*Added by ncqp34 at Jul-12-2012 for switchui-2177*/ 
	menu.setGroupVisible(Launcher.MENU_GROUP_SEARCH_APP_WIDGET, false);
	/*ended by ncqp34*/
	
		/*added by Hu ShuAn at Jul-18-2012 for switchui-2286*/ 
        if (allAppsVisible && !isAppsCustomizeSearchStatus()) {
        /*ended by Hu ShuAn*/
            mAllAppsPage.onPrepareOptionsMenu(menu);
        }
        // Added by e13775 at 19 June 2012 for organize apps' group end
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Added by e13775 at 19 June 2012 for organize apps' group start
        /*Added by ncqp34 at Jul-12-2012 for switchui-2177*/ 
        if (mAppsCustomizeTabHost.getVisibility() == View.VISIBLE &&
            (item.getGroupId() == MENU_GROUP_EDIT_APPS_GROUP ||
             item.getGroupId() == MENU_GROUP_ADD_GROUP_TO_HOME || 
	     item.getGroupId() == MENU_GROUP_SEARCH_APP_WIDGET) &&
            mAllAppsPage.onOptionsItemSelected(item)) {
	/*ended by ncqp34*/
            return true;
        }
        // Added by e13775 at 19 June 2012 for organize apps' group start
        switch (item.getItemId()) {
        case MENU_WALLPAPER_SETTINGS:
            startWallpaper();
            return true;
        //hnd734 Home Transition start
        case MENU_TRANSITION_EFFECT:
            Intent intent = new Intent(this,TransitionSettingActivity.class);
            startActivityForResult(intent,REQUEST_TRANSITION_EFFECT);
            return true;
        //hnd734 Home Transition end
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null, true);
        // Use a custom animation for launching search
        overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast);
        return true;
    }

    public boolean isWorkspaceLocked() {
        return mWorkspaceLoading || mWaitingForResult;
    }

    private void resetAddInfo() {
        mPendingAddInfo.container = ItemInfo.NO_ID;
        mPendingAddInfo.screen = -1;
        mPendingAddInfo.cellX = mPendingAddInfo.cellY = -1;
        mPendingAddInfo.spanX = mPendingAddInfo.spanY = -1;
        mPendingAddInfo.minSpanX = mPendingAddInfo.minSpanY = -1;
        mPendingAddInfo.dropPos = null;
    }

    void addAppWidgetImpl(final int appWidgetId, ItemInfo info, AppWidgetHostView boundWidget,
            AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo.configure != null) {
            mPendingAddWidgetInfo = appWidgetInfo;

            // Launch over to configure widget, if needed
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResultSafely(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            // Otherwise just add it
            completeAddAppWidget(appWidgetId, info.container, info.screen, boundWidget,
                    appWidgetInfo);
            // Exit spring loaded mode if necessary after adding the widget
            exitSpringLoadedDragModeDelayed(true, false, null);
        }
    }

    /**
     * Process a shortcut drop.
     *
     * @param componentName The name of the component
     * @param screen The screen where it should be added
     * @param cell The cell it should be added to, optional
     * @param position The location on the screen where it was dropped, optional
     */
    void processShortcutFromDrop(ComponentName componentName, long container, int screen,
            int[] cell, int[] loc) {
        resetAddInfo();
        mPendingAddInfo.container = container;
        mPendingAddInfo.screen = screen;
        mPendingAddInfo.dropPos = loc;

        if (cell != null) {
            mPendingAddInfo.cellX = cell[0];
            mPendingAddInfo.cellY = cell[1];
        }

        Intent createShortcutIntent = new Intent(Intent.ACTION_CREATE_SHORTCUT);
        createShortcutIntent.setComponent(componentName);
        processShortcut(createShortcutIntent);
    }

    /**
     * Process a widget drop.
     *
     * @param info The PendingAppWidgetInfo of the widget being added.
     * @param screen The screen where it should be added
     * @param cell The cell it should be added to, optional
     * @param position The location on the screen where it was dropped, optional
     */
    void addAppWidgetFromDrop(PendingAddWidgetInfo info, long container, int screen,
            int[] cell, int[] span, int[] loc) {
        resetAddInfo();
        mPendingAddInfo.container = info.container = container;
        mPendingAddInfo.screen = info.screen = screen;
        mPendingAddInfo.dropPos = loc;
        mPendingAddInfo.minSpanX = info.minSpanX;
        mPendingAddInfo.minSpanY = info.minSpanY;

        if (cell != null) {
            mPendingAddInfo.cellX = cell[0];
            mPendingAddInfo.cellY = cell[1];
        }
        if (span != null) {
            mPendingAddInfo.spanX = span[0];
            mPendingAddInfo.spanY = span[1];
        }

        AppWidgetHostView hostView = info.boundWidget;
        int appWidgetId;
        if (hostView != null) {
            appWidgetId = hostView.getAppWidgetId();
            addAppWidgetImpl(appWidgetId, info, hostView, info.info);
        } else {
            // In this case, we either need to start an activity to get permission to bind
            // the widget, or we need to start an activity to configure the widget, or both.
            appWidgetId = getAppWidgetHost().allocateAppWidgetId();
            if (mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.componentName)) {
                addAppWidgetImpl(appWidgetId, info, null, info.info);
            } else {
                mPendingAddWidgetInfo = info.info;
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.componentName);
                startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
            }
        }
    }

    void processShortcut(Intent intent) {
        // Handle case where user selected "Applications"
        String applicationName = getResources().getString(R.string.group_applications);
        String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

        if (applicationName != null && applicationName.equals(shortcutName)) {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
            pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
            pickIntent.putExtra(Intent.EXTRA_TITLE, getText(R.string.title_select_application));
            startActivityForResultSafely(pickIntent, REQUEST_PICK_APPLICATION);
        } else {
            startActivityForResultSafely(intent, REQUEST_CREATE_SHORTCUT);
        }
    }

    void processWallpaper(Intent intent) {
        startActivityForResult(intent, REQUEST_PICK_WALLPAPER);
    }

    FolderIcon addFolder(CellLayout layout, long container, final int screen, int cellX,
            int cellY) {
        final FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = getText(R.string.folder_name);

        // Update the model
        LauncherModel.addItemToDatabase(Launcher.this, folderInfo, container, screen, cellX, cellY,
                false);
        sFolders.put(folderInfo.id, folderInfo);

        // Create the view
        FolderIcon newFolder =
            FolderIcon.fromXml(R.layout.folder_icon, this, layout, folderInfo, mIconCache);
        mWorkspace.addInScreen(newFolder, container, screen, cellX, cellY, 1, 1,
                isWorkspaceLocked());
        return newFolder;
    }

    void removeFolder(FolderInfo folder) {
        sFolders.remove(folder.id);
    }

    private void startWallpaper() {
        showWorkspace(true);
        final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        Intent chooser = Intent.createChooser(pickWallpaper,
                getText(R.string.chooser_wallpaper));
        // NOTE: Adds a configure option to the chooser if the wallpaper supports it
        //       Removed in Eclair MR1
//        WallpaperManager wm = (WallpaperManager)
//                getSystemService(Context.WALLPAPER_SERVICE);
//        WallpaperInfo wi = wm.getWallpaperInfo();
//        if (wi != null && wi.getSettingsActivity() != null) {
//            LabeledIntent li = new LabeledIntent(getPackageName(),
//                    R.string.configure_wallpaper, 0);
//            li.setClassName(wi.getPackageName(), wi.getSettingsActivity());
//            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { li });
//        }
        startActivityForResult(chooser, REQUEST_PICK_WALLPAPER);
    }

    /**
     * Registers various content observers. The current implementation registers
     * only a favorites observer to keep track of the favorites applications.
     */
    private void registerContentObservers() {
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(LauncherProvider.CONTENT_APPWIDGET_RESET_URI,
                true, mWidgetObserver);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (SystemProperties.getInt("debug.launcher2.dumpstate", 0) != 0) {
                        dumpState();
                        return true;
                    }
                    break;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_HOME:
                    return true;
                //added by amt_wangpeipei 2012/05/09 exit from search status when click back key 
                case KeyEvent.KEYCODE_BACK:    
                    if(isAppsCustomizeSearchStatus){
                    	onClickAppsCustomizeBackButton(null);
                    	return true;
                    }
                    //added by amt_wangpeipei 2012/05/09 end.
                    // Added by e13775 at July11 2012 for organize apps' group start
                    if (mAllAppsPage != null &&
                            mAllAppsPage.onKeyUp(event.getKeyCode(), event) == true){
                        return true;
                            }
                    // Added by e13775 at July11 2012 for organize apps' group end
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
	 	//RJG678 Pinch Panel
        if (isAllAppsVisible() || mState == State.PANELVIEW) {
		//RJG678 Pinch Panel END
        	if (mDragPanel.isDragging()) {
                MotionEvent cancel = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0,0,0,0,0,0,0,0,0);
                mDragPanel.dispatchTouchEvent(cancel);
             }
            showWorkspace(true);
        } else if (mWorkspace.getOpenFolder() != null) {
            Folder openFolder = mWorkspace.getOpenFolder();
            if (openFolder.isEditingName()) {
                openFolder.dismissEditingName();
            } else {
                closeFolder();
            }
        } else {
            mWorkspace.exitWidgetResizeMode();

            // Back button is a no-op here, but give at least some feedback for the button press
            mWorkspace.showOutlinesTemporarily();
        }
        /*2012-07-22, ChenYidong added for SWITCHUI-2217*/
        checkHomeState();
        /*2012-07-22, end*/
    }

    /**
     * Re-listen when widgets are reset.
     */
    private void onAppWidgetReset() {
        if (mAppWidgetHost != null) {
            mAppWidgetHost.startListening();
        }
    }

    /**
     * Go through the and disconnect any of the callbacks in the drawables and the views or we
     * leak the previous Home screen on orientation change.
     */
    private void unbindWorkspaceAndHotseatItems() {
        if (mModel != null) {
            mModel.unbindWorkspaceItems();
        }
    }

    /**
     * Launches the intent referred by the clicked shortcut.
     *
     * @param v The view representing the clicked shortcut.
     */
    public void onClick(View v) {
        // Make sure that rogue clicks don't get through while allapps is launching, or after the
        // view has detached (it's possible for this to happen if the view is removed mid touch).
        if (v.getWindowToken() == null) {
            return;
        }

        if (!mWorkspace.isFinishedSwitchingState()) {
            return;
        }

        Object tag = v.getTag();
       /*2011-12-31, DJHV83 Added for Data Switch*/
        mCurrentView = v;
        if (tag instanceof ShortcutInfo) {
          //added by amt_wangpeipei 2012/09/11 for switchui-2771 begin
        	Intent temp = ((ShortcutInfo) tag).intent;
        	String data = temp.getDataString();
        	if(data != null && data.startsWith("file:///") && data.endsWith(".html")){
        		startLocalFile(data);
        		return;
        	}
        	//added by amt_wangpeipei 2012/09/11 for switchui-2771 end.
            // Open shortcut
            LauncherApplication app = ((LauncherApplication)getApplication());
            Log.d(TAG, "DataSwitchEnable is "+app.getDataSwitchEnable());
            if(!app.getDataSwitchEnable()){
            final Intent intent = ((ShortcutInfo) tag).intent;
         // Added by e13775 at July11 2012 for organize apps' group start
            Log.d("Test", "onclick intent.getAction()=" + intent.getAction());
			if (GroupItem.ACTION_START_GROUP.equals(intent.getAction())) {
				showAllAppsForGroup(intent);
			} else {
				// Added by e13775 at July11 2012 for organize apps' group end
				int[] pos = new int[2];
				v.getLocationOnScreen(pos);
				intent.setSourceBounds(new Rect(pos[0], pos[1], pos[0]
						+ v.getWidth(), pos[1] + v.getHeight()));

				boolean success = startActivitySafely(v, intent, tag);

				if (success && v instanceof BubbleTextView) {
					mWaitingForResume = (BubbleTextView) v;
					mWaitingForResume.setStayPressed(true);
				}
			}
            }else{
                mCurrentIntent = ((ShortcutInfo) tag).intent;
                /*Added by ncqp34 at Jan-11-2012 for IKDOMINO-5728*/
                if(mCurrentIntent == null || mCurrentIntent.getComponent() == null){
                    launchActivity();
                    return;
                }
                /*ended by ncqp34*/
                String packageName = mCurrentIntent.getComponent().getPackageName(); 
                if(DataSwitchUtil.isDialogNeedToShow(packageName, getApplicationContext())){
                    Log.d(TAG, " Send an intent to the data switch Show Dialog");
                    mDataSwitchReceiver = new DataSwitchReceiver();
                    IntentFilter intentFilter = new IntentFilter("com.motorola.dataalert.LAUNCH_APP");
                    registerReceiver(mDataSwitchReceiver, intentFilter);

                    Intent intent = new Intent(DataSwitchUtil.ACTION_DATA_CONNECTION_DIALOG);
                    intent.putExtra("AppLaunchPoint", "Home");
                    /*2012-01-13, DJHV83 added for removing whitlist, IKDOMINO-5775*/
                    intent.putExtra("Package", packageName);
                    /*DJHV83 end*/
                    getApplicationContext().sendBroadcast(intent);
                    //Send An Intent to Show a dialog
                } else {
                    launchActivity(); 
                }
            }
	    /*DJHV83 end*/
        } else if (tag instanceof FolderInfo) {
        	//2012-7-20 add by Hu ShuAn for switchui-2360
            if (mWorkspace.isSmall() || mWorkspace.isPageMoving() || mState != State.WORKSPACE) {//add for SWITCHUI-604, Chen yidong, 2012-04-14
                return;
            }
            /*2012-3-21, add end*/
            if (v instanceof FolderIcon) {
                FolderIcon fi = (FolderIcon) v;
                handleFolderClick(fi);
            }
        } else if (v == mAllAppsButton) {
            if (isAllAppsVisible()) {
                showWorkspace(true);
            } else {
                onClickAllAppsButton(v);
            }
        }
    }

    //added by amt_wangpeipei 2012/09/11 for switchui-2771 begin
	private void startLocalFile(String data) {
		// TODO Auto-generated method stub
		Uri uri = Uri.parse(data);
		Intent it = new Intent(Intent.ACTION_VIEW);
		it.setDataAndType(uri, "text/html");
		startActivity(it);
	}
	//added by amt_wangpeipei 2012/09/11 for switchui-2771 end

	// Added by e13775 at July11 2012 for organize apps' group start
    void showAllAppsForGroup(Intent intent) {
        final Uri uri = intent.getData();
        if (uri != null) {
            final String idString = uri.getLastPathSegment();
            final long groupId = Long.parseLong(idString);
            if (groupId > 0) {
                // Tell the apps tray show the group
                mAllAppsPage.setGroupButton(groupId);
                mAllAppsPage.setCurrentGroupId(groupId);
                mAllAppsPage.notifyGroupChanged();
                /*2012-07-26, Added by amt_chenjing for SWITCHUI-2469*/
                mHandler.removeMessages(SHOW_ALL_APPS);
                Message msg = new Message();
                msg.what = SHOW_ALL_APPS;
                mHandler.sendMessageDelayed(msg, 200);
                //showAllApps(true);
                /*2012-07-26, end*/
            } else {
                Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
        }
    }
	// Added by e13775 at July11 2012 for organize apps' group end 

    public boolean onTouch(View v, MotionEvent event) {
        // this is an intercepted event being forwarded from mWorkspace;
        // clicking anywhere on the workspace causes the customization drawer to slide down
        showWorkspace(true);
        return false;
    }

    /**
     * Event handler for the search button
     *
     * @param v The view that was clicked.
     */
    public void onClickSearchButton(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        onSearchRequested();
    }

    /**
     * Event handler for the voice button
     *
     * @param v The view that was clicked.
     */
    public void onClickVoiceButton(View v) {
        /*2012-11-20, add by 003033 for switchuitwo-74*/
        if (v instanceof DrawableStateProxyView
            && Flex.getSearchBarHideSetting((Context)(Launcher.this))) {
            return;
        }
        /*2012-11-20, add end*/
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        try {
            final SearchManager searchManager =
                    (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            ComponentName activityName = searchManager.getGlobalSearchActivity();
            Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (activityName != null) {
                intent.setPackage(activityName.getPackageName());
            }
            startActivity(null, intent, "onClickVoiceButton");
            overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast);
        } catch (ActivityNotFoundException e) {
            Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivitySafely(null, intent, "onClickVoiceButton");
        }
    }

    /**
     * Event handler for the "grid" button that appears on the home screen, which
     * enters all apps mode.
     *
     * @param v The view that was clicked.
     */
    public void onClickAllAppsButton(View v) {
        //2012-07-25, ChenYidong for SWITCHUI-2442, isAllAppsClicked is false
        resetPagedView(mWorkspace, true);
        /*2012-06-01 end*/
        showAllApps(true);
    }

    public void onTouchDownAllAppsButton(View v) {
        // Provide the same haptic feedback that the system offers for virtual keys.
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }

    public void onClickAppMarketButton(View v) {
	/*Added by ncqp34 at Jan-06-2012 for market flex update*/
	if(mAppMarketIntent == null){	   
	    mAppMarketIntent = Flex.getMarketIntent(this);
	}
	/*ended by ncqp34*/
        if (mAppMarketIntent != null) {
            startActivitySafely(v, mAppMarketIntent, "app market");
        } else {
            Log.e(TAG, "Invalid app market intent.");
        }
    }

    /**
     * Added by amt_wangpeipei 2012/05/09
     * Process search button click event in AppsCustomizeTabHost
     * modified by amt_wangpeipei 2012/07/11 for switchui-2121 
     */
    public void enterSearchAppAndWidget(){
	// Added by e13775 at 19 June 2012 for organize apps' group 
    	mAppsCustomizeContent.setOriginalApp();

    	AppsCustomizeSearchBar searchBar = mAppsCustomizeTabHost.getAppsCustomizeSearchBar();
    	
    	//used when exit from search status.
    	mAppsCustomizeContent.saveOriginalPageAndTab();
    	searchBar.clearSearchText();
    	searchBar.showInputMethod();
    	searchBar.addTextChangedListener();
    	mAppsCustomizeTabHost.setAppsCustomizeSearchBarVisibility(View.VISIBLE);
    	mAppsCustomizeTabHost.setTabsContainerVisibility(View.GONE);
    	isAppsCustomizeSearchStatus = true;
    }
    
    /**
     * Added by amt_wangpeipei 2012/07/11 for switchui-2050
     * @return
     */
    public boolean isAppsCustomizeSearchStatus(){
    	return isAppsCustomizeSearchStatus;
    }
    
    /**
     * Added by amt_wangpeipei 2012/05/09
     * Process back button click event in AppsCustomizeTabHost
     */
    public void onClickAppsCustomizeBackButton(View v){
    	AppsCustomizeSearchBar searchBar = mAppsCustomizeTabHost.getAppsCustomizeSearchBar();
    	searchBar.clearSearchText();
    	searchBar.removeTextChangedListener();
    	searchBar.hideInputMethod();
    	mAppsCustomizeTabHost.setTabsContainerVisibility(View.VISIBLE);
    	mAppsCustomizeTabHost.setAppsCustomizeSearchBarVisibility(View.GONE);
    	isAppsCustomizeSearchStatus = false;
    	//modified by amt_wangpeipei 2012/07/11 for switchui-2050 begin
    	if(mAppsCustomizeContent instanceof MotoAppsCustomizePagedView){
    		((MotoAppsCustomizePagedView)mAppsCustomizeContent).showOriginalAppsAndWidgets();
    	}
		else{
			mAppsCustomizeContent.showOriginalAppsAndWidgets();
		}
    	//modified by amt_wangpeipei 2012/07/11 for switchui-2050 end.
    }

    void startApplicationDetailsActivity(ComponentName componentName) {
        String packageName = componentName.getPackageName();
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivitySafely(null, intent, "startApplicationDetailsActivity");
    }

    void startApplicationUninstallActivity(ApplicationInfo appInfo) {
        if ((appInfo.flags & ApplicationInfo.DOWNLOADED_FLAG) == 0) {
            // System applications cannot be installed. For now, show a toast explaining that.
            // We may give them the option of disabling apps this way.
            int messageId = R.string.uninstall_system_app_text;
            Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
        } else {
            String packageName = appInfo.componentName.getPackageName();
            String className = appInfo.componentName.getClassName();
            Intent intent = new Intent(
                    Intent.ACTION_DELETE, Uri.fromParts("package", packageName, className));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);
        }
    }

    // Added by e13775 at 19 June 2012 for organize apps' group start
    void markAsLaunchedAssync(final Intent intent) {
    	//modified by amt_wangpeipei 2012/08/24 for switchui-2569 begin
        LauncherModel.attachTaskDelayed(new Runnable() {
            @Override
            public void run() {
                mAllAppsPage.markAsUsed(intent);
            }
        }, 2000);
      //modified by amt_wangpeipei 2012/08/24 for switchui-2569 end.
   }

   // Added by e13775 at 19 June 2012 for organize apps' group end
    // Modified by e13775 at 19 June 2012 for organize apps' group start
    public  boolean startActivitySafely(final Intent intent, Object tag) {
    // Modified by e13775 at 19 June 2012 for organize apps' group end
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
	    // Added by e13775 at 19 June 2012 for organize apps' group start
             markAsLaunchedAssync(intent);
             return true;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to launch. tag=" + tag + " intent=" + intent, e);
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity. "
                    + "tag="+ tag + " intent=" + intent, e);
        }
        return false;
    }

    boolean startActivity(View v, Intent intent, Object tag) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            // Only launch using the new animation if the shortcut has not opted out (this is a
            // private contract between launcher and may be ignored in the future).
            boolean useLaunchAnimation = (v != null) &&
                    !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);
            if (useLaunchAnimation) {
                ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0,
                        v.getMeasuredWidth(), v.getMeasuredHeight());

                startActivity(intent, opts.toBundle());
            } else {
                startActivity(intent);
            }
            return true;
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity. "
                    + "tag="+ tag + " intent=" + intent, e);
        }
        return false;
    }

    boolean startActivitySafely(View v, Intent intent, Object tag) {
        boolean success = false;
        try {
            success = startActivity(v, intent, tag);
            /*2012-11-14,Added by amt_chenjing for organize apps' group frequent*/            
            markAsLaunchedAssync(intent);
            /*2012-11-14,Added end*/
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Unable to launch. tag=" + tag + " intent=" + intent, e);
        }
        return success;
    }

    void startActivityForResultSafely(Intent intent, int requestCode) {
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Launcher does not have the permission to launch " + intent +
                    ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                    "or use the exported attribute for this activity.", e);
        }
    }

    private void handleFolderClick(FolderIcon folderIcon) {
        final FolderInfo info = folderIcon.mInfo;
        Folder openFolder = mWorkspace.getFolderForTag(info);

        // If the folder info reports that the associated folder is open, then verify that
        // it is actually opened. There have been a few instances where this gets out of sync.
        if (info.opened && openFolder == null) {
            Log.d(TAG, "Folder info marked as open, but associated folder is not open. Screen: "
                    + info.screen + " (" + info.cellX + ", " + info.cellY + ")");
            info.opened = false;
        }

        if (!info.opened) {
            // Close any open folder
            closeFolder();
            // Open the requested folder
            openFolder(folderIcon);
        } else {
            // Find the open folder...
            int folderScreen;
            if (openFolder != null) {
                folderScreen = mWorkspace.getPageForView(openFolder);
                // .. and close it
                closeFolder(openFolder);
                if (folderScreen != mWorkspace.getCurrentPage()) {
                    // Close any folder open on the current screen
                    closeFolder();
                    // Pull the folder onto this screen
                    openFolder(folderIcon);
                }
            }
        }
    }

    /**
     * This method draws the FolderIcon to an ImageView and then adds and positions that ImageView
     * in the DragLayer in the exact absolute location of the original FolderIcon.
     */
    private void copyFolderIconToImage(FolderIcon fi) {
        final int width = fi.getMeasuredWidth();
        final int height = fi.getMeasuredHeight();

        // Lazy load ImageView, Bitmap and Canvas
        if (mFolderIconImageView == null) {
            mFolderIconImageView = new ImageView(this);
        }
        if (mFolderIconBitmap == null || mFolderIconBitmap.getWidth() != width ||
                mFolderIconBitmap.getHeight() != height) {
            mFolderIconBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mFolderIconCanvas = new Canvas(mFolderIconBitmap);
        }

        DragLayer.LayoutParams lp;
        if (mFolderIconImageView.getLayoutParams() instanceof DragLayer.LayoutParams) {
            lp = (DragLayer.LayoutParams) mFolderIconImageView.getLayoutParams();
        } else {
            lp = new DragLayer.LayoutParams(width, height);
        }

        mDragLayer.getViewRectRelativeToSelf(fi, mRectForFolderAnimation);
        lp.customPosition = true;
        lp.x = mRectForFolderAnimation.left;
        lp.y = mRectForFolderAnimation.top;
        lp.width = width;
        lp.height = height;

        mFolderIconCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        fi.draw(mFolderIconCanvas);
        mFolderIconImageView.setImageBitmap(mFolderIconBitmap);
        if (fi.mFolder != null) {
            mFolderIconImageView.setPivotX(fi.mFolder.getPivotXForIconAnimation());
            mFolderIconImageView.setPivotY(fi.mFolder.getPivotYForIconAnimation());
        }
        // Just in case this image view is still in the drag layer from a previous animation,
        // we remove it and re-add it.
        if (mDragLayer.indexOfChild(mFolderIconImageView) != -1) {
            mDragLayer.removeView(mFolderIconImageView);
        }
        mDragLayer.addView(mFolderIconImageView, lp);
        if (fi.mFolder != null) {
            fi.mFolder.bringToFront();
        }
    }

    private void growAndFadeOutFolderIcon(FolderIcon fi) {
        if (fi == null) return;
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.5f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.5f);

        FolderInfo info = (FolderInfo) fi.getTag();
        if (info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            CellLayout cl = (CellLayout) fi.getParent().getParent();
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) fi.getLayoutParams();
            cl.setFolderLeaveBehindCell(lp.cellX, lp.cellY);
        }

        // Push an ImageView copy of the FolderIcon into the DragLayer and hide the original
        copyFolderIconToImage(fi);
        fi.setVisibility(View.INVISIBLE);

        ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(mFolderIconImageView, alpha,
                scaleX, scaleY);
        oa.setDuration(getResources().getInteger(R.integer.config_folderAnimDuration));
        oa.start();
    }

    private void shrinkAndFadeInFolderIcon(final FolderIcon fi) {
        if (fi == null) return;
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1.0f);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f);

        final CellLayout cl = (CellLayout) fi.getParent().getParent();

        // We remove and re-draw the FolderIcon in-case it has changed
        mDragLayer.removeView(mFolderIconImageView);
        copyFolderIconToImage(fi);
        ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(mFolderIconImageView, alpha,
                scaleX, scaleY);
        oa.setDuration(getResources().getInteger(R.integer.config_folderAnimDuration));
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (cl != null) {
                    cl.clearFolderLeaveBehind();
                    // Remove the ImageView copy of the FolderIcon and make the original visible.
                    mDragLayer.removeView(mFolderIconImageView);
                    fi.setVisibility(View.VISIBLE);
                }
            }
        });
        oa.start();
    }

    /**
     * Opens the user folder described by the specified tag. The opening of the folder
     * is animated relative to the specified View. If the View is null, no animation
     * is played.
     *
     * @param folderInfo The FolderInfo describing the folder to open.
     */
    public void openFolder(FolderIcon folderIcon) {
        Folder folder = folderIcon.mFolder;
        FolderInfo info = folder.mInfo;

        info.opened = true;

        // Just verify that the folder hasn't already been added to the DragLayer.
        // There was a one-off crash where the folder had a parent already.
        if (folder.getParent() == null) {
            mDragLayer.addView(folder);
            mDragController.addDropTarget((DropTarget) folder);
        } else {
            Log.w(TAG, "Opening folder (" + folder + ") which already has a parent (" +
                    folder.getParent() + ").");
        }
        folder.animateOpen();
        growAndFadeOutFolderIcon(folderIcon);
    }

    public void closeFolder() {
        //add for a force close issue by null pointer. Chen Yidong, 2012-05-24, SWITCHUI-1246
        if(mWorkspace != null){
        Folder folder = mWorkspace.getOpenFolder();
        if (folder != null) {
            if (folder.isEditingName()) {
                folder.dismissEditingName();
            }
            closeFolder(folder);

            // Dismiss the folder cling
            dismissFolderCling(null);
            }
        }
    }

    void closeFolder(Folder folder) {
        folder.getInfo().opened = false;

        ViewGroup parent = (ViewGroup) folder.getParent().getParent();
        if (parent != null) {
            FolderIcon fi = (FolderIcon) mWorkspace.getViewForTag(folder.mInfo);
            shrinkAndFadeInFolderIcon(fi);
        }
        folder.animateClosed();
    }

    public boolean onLongClick(View v) {
        if (!isDraggingEnabled()) return false;
        if (isWorkspaceLocked()) return false;
        if (mState != State.WORKSPACE) return false;

        if (!(v instanceof CellLayout)) {
            v = (View) v.getParent().getParent();
        }

        resetAddInfo();
        CellLayout.CellInfo longClickCellInfo = (CellLayout.CellInfo) v.getTag();
        // This happens when long clicking an item with the dpad/trackball
        if (longClickCellInfo == null) {
            return true;
        }

        // The hotseat touch handling does not go through Workspace, and we always allow long press
        // on hotseat items.
        final View itemUnderLongClick = longClickCellInfo.cell;
        boolean allowLongPress = isHotseatLayout(v) || mWorkspace.allowLongPress();
        if (allowLongPress && !mDragController.isDragging()) {
            if (itemUnderLongClick == null) {
                // User long pressed on empty space
                mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
                        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                startWallpaper();
            } else {
                if (!(itemUnderLongClick instanceof Folder)) {
                    /* modified by amt_wangpeipei 2012/04/06 SWITCHUI-273 begin*/
                    if(getSearchBar().getIsQSBarHide()){
                	enterSpringLoadedDragModeOnWorkspace();
                    }
                    /* modified end*/

                    // User long pressed on an item
                    mWorkspace.startDrag(longClickCellInfo);
                }
            }
        }
        return true;
    }

    boolean isHotseatLayout(View layout) {
        return mHotseat != null && layout != null &&
                (layout instanceof CellLayout) && (layout == mHotseat.getLayout());
    }
    Hotseat getHotseat() {
        return mHotseat;
    }
    SearchDropTargetBar getSearchBar() {
        return mSearchDropTargetBar;
    }

    /**
     * Returns the CellLayout of the specified container at the specified screen.
     */
    CellLayout getCellLayout(long container, int screen) {
        if (container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            if (mHotseat != null) {
                return mHotseat.getLayout();
            } else {
                return null;
            }
        } else {
            return (CellLayout) mWorkspace.getChildAt(screen);
        }
    }

   // Modified by e13775 at 19 June 2012 for organize apps' group
    public Workspace getWorkspace() {
        return mWorkspace;
    }

    // Now a part of LauncherModel.Callbacks. Used to reorder loading steps.
    public boolean isAllAppsVisible() {
        return (mState == State.APPS_CUSTOMIZE) || (mOnResumeState == State.APPS_CUSTOMIZE);
    }

    public boolean isAllAppsButtonRank(int rank) {
        return mHotseat.isAllAppsButtonRank(rank);
    }

    // AllAppsView.Watcher
    public void zoomed(float zoom) {
        if (zoom == 1.0f) {
            mWorkspace.setVisibility(View.GONE);
        }
    }

    /**
     * Helper method for the cameraZoomIn/cameraZoomOut animations
     * @param view The view being animated
     * @param state The state that we are moving in or out of (eg. APPS_CUSTOMIZE)
     * @param scaleFactor The scale factor used for the zoom
     */
    private void setPivotsForZoom(View view, float scaleFactor) {
        if (view == null) {
            return;
        }
        view.setPivotX(view.getWidth() / 2.0f);
        view.setPivotY(view.getHeight() / 2.0f);
    }

    void disableWallpaperIfInAllApps() {
        // Only disable it if we are in all apps
        if (isAllAppsVisible()) {
            if (mAppsCustomizeTabHost != null &&
                    !mAppsCustomizeTabHost.isTransitioning()) {
                updateWallpaperVisibility(false);
            }
        }
    }

    void updateWallpaperVisibility(boolean visible) {
        int wpflags = visible ? WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER : 0;
        int curflags = getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
        if (wpflags != curflags) {
            getWindow().setFlags(wpflags, WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        }
    }

    private void dispatchOnLauncherTransitionPrepare(View v, boolean animated, boolean toWorkspace) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionPrepare(this, animated, toWorkspace);
        }
    }

    private void dispatchOnLauncherTransitionStart(View v, boolean animated, boolean toWorkspace) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionStart(this, animated, toWorkspace);
        }

        // Update the workspace transition step as well
        dispatchOnLauncherTransitionStep(v, 0f);
    }

    private void dispatchOnLauncherTransitionStep(View v, float t) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionStep(this, t);
        }
    }

    private void dispatchOnLauncherTransitionEnd(View v, boolean animated, boolean toWorkspace) {
        if (v instanceof LauncherTransitionable) {
            ((LauncherTransitionable) v).onLauncherTransitionEnd(this, animated, toWorkspace);
        }

        // Update the workspace transition step as well
        dispatchOnLauncherTransitionStep(v, 1f);
    }

    /**
     * Things to test when changing the following seven functions.
     *   - Home from workspace
     *          - from center screen
     *          - from other screens
     *   - Home from all apps
     *          - from center screen
     *          - from other screens
     *   - Back from all apps
     *          - from center screen
     *          - from other screens
     *   - Launch app from workspace and quit
     *          - with back
     *          - with home
     *   - Launch app from all apps and quit
     *          - with back
     *          - with home
     *   - Go to a screen that's not the default, then all
     *     apps, and launch and app, and go back
     *          - with back
     *          -with home
     *   - On workspace, long press power and go back
     *          - with back
     *          - with home
     *   - On all apps, long press power and go back
     *          - with back
     *          - with home
     *   - On workspace, power off
     *   - On all apps, power off
     *   - Launch an app and turn off the screen while in that app
     *          - Go back with home key
     *          - Go back with back key  TODO: make this not go to workspace
     *          - From all apps
     *          - From workspace
     *   - Enter and exit car mode (becuase it causes an extra configuration changed)
     *          - From all apps
     *          - From the center workspace
     *          - From another workspace
     */

    /**
     * Zoom the camera out from the workspace to reveal 'toView'.
     * Assumes that the view to show is anchored at either the very top or very bottom
     * of the screen.
     */
    private void showAppsCustomizeHelper(final boolean animated, final boolean springLoaded) {
        if (mStateAnimation != null) {
            mStateAnimation.cancel();
            mStateAnimation = null;
        }
        final Resources res = getResources();

        final int duration = res.getInteger(R.integer.config_appsCustomizeZoomInTime);
        final int fadeDuration = res.getInteger(R.integer.config_appsCustomizeFadeInTime);
        final float scale = (float) res.getInteger(R.integer.config_appsCustomizeZoomScaleFactor);
        final View fromView = mWorkspace;
        final AppsCustomizeTabHost toView = mAppsCustomizeTabHost;
        final int startDelay =
                res.getInteger(R.integer.config_workspaceAppsCustomizeAnimationStagger);

        setPivotsForZoom(toView, scale);

        // Shrink workspaces away if going to AppsCustomize from workspace
        Animator workspaceAnim =
                mWorkspace.getChangeStateAnimation(Workspace.State.SMALL, animated);

        if (animated) {
            toView.setScaleX(scale);
            toView.setScaleY(scale);
            final LauncherViewPropertyAnimator scaleAnim = new LauncherViewPropertyAnimator(toView);
            scaleAnim.
                scaleX(1f).scaleY(1f).
                setDuration(duration).
                setInterpolator(new Workspace.ZoomOutInterpolator());

            toView.setVisibility(View.VISIBLE);
            toView.setAlpha(0f);
            final ObjectAnimator alphaAnim = ObjectAnimator
                .ofFloat(toView, "alpha", 0f, 1f)
                .setDuration(fadeDuration);
            alphaAnim.setInterpolator(new DecelerateInterpolator(1.5f));
            alphaAnim.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float t = (Float) animation.getAnimatedValue();
                    dispatchOnLauncherTransitionStep(fromView, t);
                    dispatchOnLauncherTransitionStep(toView, t);
                }
            });

            // toView should appear right at the end of the workspace shrink
            // animation
            mStateAnimation = new AnimatorSet();
            mStateAnimation.play(scaleAnim).after(startDelay);
            mStateAnimation.play(alphaAnim).after(startDelay);

            mStateAnimation.addListener(new AnimatorListenerAdapter() {
                boolean animationCancelled = false;

                @Override
                public void onAnimationStart(Animator animation) {
                    updateWallpaperVisibility(true);
                    // Prepare the position
                    toView.setTranslationX(0.0f);
                    toView.setTranslationY(0.0f);
                    toView.setVisibility(View.VISIBLE);
                    toView.bringToFront();
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    /*2012-8-16, add by bvq783*/
                    toView.setScaleX(1.0f);
                    toView.setScaleY(1.0f);
                    /*2012-8-16, add end*/
                    dispatchOnLauncherTransitionEnd(fromView, animated, false);
                    dispatchOnLauncherTransitionEnd(toView, animated, false);

                    // wag044 03/19/2012: IKHSS7-14625 workspace might have been destroyed
                    // before animation ends. Just ignore this block in that case, so add workspace isn't null
                    if (!springLoaded && !LauncherApplication.isScreenLarge() && mWorkspace != null) {
                        // Hide the workspace scrollbar
                        mWorkspace.hideScrollingIndicator(true);
                        hideDockDivider();
                    }
                    if (!animationCancelled) {
                        updateWallpaperVisibility(false);
                    }

                    // Hide the search bar
                    mSearchDropTargetBar.hideSearchBar(false);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    animationCancelled = true;
                    /*2012-07-22, ChenYidong added for SWITCHUI-2217*/
                    checkHomeState();
                    /*2012-07-22, end*/
                }
            });

            if (workspaceAnim != null) {
                mStateAnimation.play(workspaceAnim);
            }

            boolean delayAnim = false;
            final ViewTreeObserver observer;

            dispatchOnLauncherTransitionPrepare(fromView, animated, false);
            dispatchOnLauncherTransitionPrepare(toView, animated, false);

            // If any of the objects being animated haven't been measured/laid out
            // yet, delay the animation until we get a layout pass
            if ((((LauncherTransitionable) toView).getContent().getMeasuredWidth() == 0) ||
                    (mWorkspace.getMeasuredWidth() == 0) ||
                    (toView.getMeasuredWidth() == 0)) {
                observer = mWorkspace.getViewTreeObserver();
                delayAnim = true;
            } else {
                observer = null;
            }

            final AnimatorSet stateAnimation = mStateAnimation;
            final Runnable startAnimRunnable = new Runnable() {
                public void run() {
                    // Check that mStateAnimation hasn't changed while
                    // we waited for a layout/draw pass
                    if (mStateAnimation != stateAnimation)
                        return;
                    setPivotsForZoom(toView, scale);
                    dispatchOnLauncherTransitionStart(fromView, animated, false);
                    dispatchOnLauncherTransitionStart(toView, animated, false);
                    toView.post(new Runnable() {
                        public void run() {
                            // Check that mStateAnimation hasn't changed while
                            // we waited for a layout/draw pass
                            if (mStateAnimation != stateAnimation)
                                return;
                            mStateAnimation.start();
                        }
                    });
                }
            };
            if (delayAnim) {
                final OnGlobalLayoutListener delayedStart = new OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        toView.post(startAnimRunnable);
                        observer.removeOnGlobalLayoutListener(this);
                    }
                };
                observer.addOnGlobalLayoutListener(delayedStart);
            } else {
                startAnimRunnable.run();
            }
        } else {
            toView.setTranslationX(0.0f);
            toView.setTranslationY(0.0f);
            toView.setScaleX(1.0f);
            toView.setScaleY(1.0f);
            toView.setVisibility(View.VISIBLE);
            toView.bringToFront();

            if (!springLoaded && !LauncherApplication.isScreenLarge()) {
                // Hide the workspace scrollbar
                mWorkspace.hideScrollingIndicator(true);
                hideDockDivider();

                // Hide the search bar
                mSearchDropTargetBar.hideSearchBar(false);
            }
            dispatchOnLauncherTransitionPrepare(fromView, animated, false);
            dispatchOnLauncherTransitionStart(fromView, animated, false);
            dispatchOnLauncherTransitionEnd(fromView, animated, false);
            dispatchOnLauncherTransitionPrepare(toView, animated, false);
            dispatchOnLauncherTransitionStart(toView, animated, false);
            dispatchOnLauncherTransitionEnd(toView, animated, false);
            updateWallpaperVisibility(false);
        }
        /*2012-8-3, add by bvq783 for plugin*/
        if (mPluginWidgetHost != null)
            mPluginWidgetHost.statusChange(mPluginWidgetHost.ONPAUSE);
        /*2012-8-3, add end*/
    }

    /**
     * Zoom the camera back into the workspace, hiding 'fromView'.
     * This is the opposite of showAppsCustomizeHelper.
     * @param animated If true, the transition will be animated.
     */
    private void hideAppsCustomizeHelper(State toState, final boolean animated,
            final boolean springLoaded, final Runnable onCompleteRunnable) {

        if (mStateAnimation != null) {
            mStateAnimation.cancel();
            mStateAnimation = null;
        }
        Resources res = getResources();

        final int duration = res.getInteger(R.integer.config_appsCustomizeZoomOutTime);
        final int fadeOutDuration =
                res.getInteger(R.integer.config_appsCustomizeFadeOutTime);
        final float scaleFactor = (float)
                res.getInteger(R.integer.config_appsCustomizeZoomScaleFactor);
        final View fromView = mAppsCustomizeTabHost;
        final View toView = mWorkspace;
        Animator workspaceAnim = null;

        if (toState == State.WORKSPACE) {
            int stagger = res.getInteger(R.integer.config_appsCustomizeWorkspaceAnimationStagger);
            workspaceAnim = mWorkspace.getChangeStateAnimation(
                    Workspace.State.NORMAL, animated, stagger);
        } else if (toState == State.APPS_CUSTOMIZE_SPRING_LOADED) {
            workspaceAnim = mWorkspace.getChangeStateAnimation(
                    Workspace.State.SPRING_LOADED, animated);
        }

        setPivotsForZoom(fromView, scaleFactor);
        updateWallpaperVisibility(true);
        showHotseat(animated);
        if (animated) {
            final LauncherViewPropertyAnimator scaleAnim =
                    new LauncherViewPropertyAnimator(fromView);
            scaleAnim.
                scaleX(scaleFactor).scaleY(scaleFactor).
                setDuration(duration).
                setInterpolator(new Workspace.ZoomInInterpolator());

            final ObjectAnimator alphaAnim = ObjectAnimator
                .ofFloat(fromView, "alpha", 1f, 0f)
                .setDuration(fadeOutDuration);
            alphaAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            alphaAnim.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float t = 1f - (Float) animation.getAnimatedValue();
                    dispatchOnLauncherTransitionStep(fromView, t);
                    dispatchOnLauncherTransitionStep(toView, t);
                }
            });

            mStateAnimation = new AnimatorSet();

            dispatchOnLauncherTransitionPrepare(fromView, animated, true);
            dispatchOnLauncherTransitionPrepare(toView, animated, true);

            mStateAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    updateWallpaperVisibility(true);
                    fromView.setVisibility(View.GONE);
                    dispatchOnLauncherTransitionEnd(fromView, animated, true);
                    dispatchOnLauncherTransitionEnd(toView, animated, true);
                    if (mWorkspace != null) {
                        mWorkspace.hideScrollingIndicator(false);
                    }
                    if (onCompleteRunnable != null) {
                        onCompleteRunnable.run();
                    }
                }
            });

            mStateAnimation.playTogether(scaleAnim, alphaAnim);
            if (workspaceAnim != null) {
                mStateAnimation.play(workspaceAnim);
            }
            dispatchOnLauncherTransitionStart(fromView, animated, true);
            dispatchOnLauncherTransitionStart(toView, animated, true);
            final Animator stateAnimation = mStateAnimation;
            mWorkspace.post(new Runnable() {
                public void run() {
                    if (stateAnimation != mStateAnimation)
                        return;
                    mStateAnimation.start();
                }
            });
        } else {
            fromView.setVisibility(View.GONE);
            dispatchOnLauncherTransitionPrepare(fromView, animated, true);
            dispatchOnLauncherTransitionStart(fromView, animated, true);
            dispatchOnLauncherTransitionEnd(fromView, animated, true);
            dispatchOnLauncherTransitionPrepare(toView, animated, true);
            dispatchOnLauncherTransitionStart(toView, animated, true);
            dispatchOnLauncherTransitionEnd(toView, animated, true);
            mWorkspace.hideScrollingIndicator(false);
        }
    }

    /*2012-07-22, ChenYidong added for SWITCHUI-2217*/
    public void checkHomeState(){
        //2012-07-27, change for this may cause sametimes the sateanimation not run.
        if(mAppsCustomizeTabHost != null && mState != State.APPS_CUSTOMIZE
                && mAppsCustomizeTabHost.getVisibility() == View.VISIBLE
                && (mStateAnimation == null || !mStateAnimation.isStarted())){
            updateWallpaperVisibility(true);
            mAppsCustomizeTabHost.setVisibility(View.GONE);
            /*2012-11-27, remove by songshun.zhang for SWITCHUITWO-137 */
            //mWorkspace.hideScrollingIndicator(false);
            /*2012-11-27, remove end*/
        }
    }
    /*2012-07-22, end*/

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            mAppsCustomizeTabHost.onTrimMemory();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (!hasFocus) {
            // When another window occludes launcher (like the notification shade, or recents),
            // ensure that we enable the wallpaper flag so that transitions are done correctly.
            updateWallpaperVisibility(true);
        } else {
            // When launcher has focus again, disable the wallpaper if we are in AllApps
            mWorkspace.postDelayed(new Runnable() {
                @Override
                public void run() {
                    disableWallpaperIfInAllApps();
                }
            }, 500);
        }
    }

    void showWorkspace(boolean animated) {
        showWorkspace(animated, null);
    }

    void showWorkspace(boolean animated, Runnable onCompleteRunnable) {
        if (mState != State.WORKSPACE) {
			//2012-01-17 RJG678 Pinch Panel
            if (mState == State.PANELVIEW){
                /*2012-06-06, added by Bai Jian for PinchPanel Transition SWITCHUI-1361*/              
            	hidePanelView(animated, mDragPanel.getHighlight());
                /*2012-06-06, end*/
                mWorkspace.setCurrentPage(mDragPanel.getHighlight());
                showDockDivider(true);
            //RJG678 Pinch Panel END
            } else {            	
            	
            	       		
            	boolean wasInSpringLoadedMode = (mState == State.APPS_CUSTOMIZE_SPRING_LOADED);
	            mWorkspace.setVisibility(View.VISIBLE);
	            hideAppsCustomizeHelper(State.WORKSPACE, animated, false, onCompleteRunnable);	            
	           
	            // Show the search bar (only animate if we were showing the drop target bar in spring
	            // loaded mode)
	            mSearchDropTargetBar.showSearchBar(wasInSpringLoadedMode);

            // We only need to animate in the dock divider if we're going from spring loaded mode
            showDockDivider(animated && wasInSpringLoadedMode);

	            // Set focus to the AppsCustomize button
	            if (mAllAppsButton != null) {
	                mAllAppsButton.requestFocus();
	            }
			}
        }

        mWorkspace.flashScrollingIndicator(animated);

        // Change the state *after* we've called all the transition code
        mState = State.WORKSPACE;

        // Resume the auto-advance of widgets
        mUserPresent = true;
        updateRunning();

        /*2012-8-3, add by bvq783 for plugin*/
        if (mPluginWidgetHost != null)
            mPluginWidgetHost.statusChange(mPluginWidgetHost.ONRESUME);
        /*2012-8-3, add end*/

        // send an accessibility event to announce the context change
        getWindow().getDecorView().sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
    }

    void showAllApps(boolean animated) {
        if (mState != State.WORKSPACE) return;

        showAppsCustomizeHelper(animated, false);
        mAppsCustomizeTabHost.requestFocus();

        // Hide the search bar and hotseat
        mSearchDropTargetBar.hideSearchBar(animated);

        // Change the state *after* we've called all the transition code
        mState = State.APPS_CUSTOMIZE;

        // Pause the auto-advance of widgets until we are out of AllApps
        mUserPresent = false;
        updateRunning();
        closeFolder();

        // Send an accessibility event to announce the context change
        getWindow().getDecorView().sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
    }

    void enterSpringLoadedDragMode() {
        if (isAllAppsVisible()) {
            hideAppsCustomizeHelper(State.APPS_CUSTOMIZE_SPRING_LOADED, true, true, null);
            hideDockDivider();
            mState = State.APPS_CUSTOMIZE_SPRING_LOADED;
        }
    }
    
    /* added by amt_wangpeipei 2012/04/16 SWITCHUI-273 begin*/
    void enterSpringLoadedDragModeOnWorkspace(){
    	if (mState == State.WORKSPACE && getSearchBar().getIsQSBarHide()) {
            hideAppsCustomizeHelper(State.APPS_CUSTOMIZE_SPRING_LOADED, true, true, null);
            hideDockDivider();
            mState = State.APPS_CUSTOMIZE_SPRING_LOADED;
        }
    }
    
    void exitSpringLoadedDragModeOnWorkspace(){
   	 if (mState == State.APPS_CUSTOMIZE_SPRING_LOADED) {
            showWorkspace(true);
            mState = State.WORKSPACE;
        }
   }
   /* 2012/04/16 added end*/

    void exitSpringLoadedDragModeDelayed(final boolean successfulDrop, boolean extendedDelay,
            final Runnable onCompleteRunnable) {
        if (mState != State.APPS_CUSTOMIZE_SPRING_LOADED) return;

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (successfulDrop) {
                    // Before we show workspace, hide all apps again because
                    // exitSpringLoadedDragMode made it visible. This is a bit hacky; we should
                    // clean up our state transition functions
                    mAppsCustomizeTabHost.setVisibility(View.GONE);
                    showWorkspace(true, onCompleteRunnable);
                } else {
                    exitSpringLoadedDragMode();
                }
            }
        }, (extendedDelay ?
                EXIT_SPRINGLOADED_MODE_LONG_TIMEOUT :
                EXIT_SPRINGLOADED_MODE_SHORT_TIMEOUT));
    }

    void exitSpringLoadedDragMode() {
        if (mState == State.APPS_CUSTOMIZE_SPRING_LOADED) {
            final boolean animated = true;
            final boolean springLoaded = true;
            showAppsCustomizeHelper(animated, springLoaded);
            mState = State.APPS_CUSTOMIZE;
        }
        // Otherwise, we are not in spring loaded mode, so don't do anything.
    }

    void hideDockDivider() {
        if (mQsbDivider != null && mDockDivider != null) {
            mQsbDivider.setVisibility(View.INVISIBLE);
            mDockDivider.setVisibility(View.INVISIBLE);
        /*2012-8-16, add by bvq783*/
        } else if (mDockDivider != null) {
            mDockDivider.setVisibility(View.INVISIBLE);
        /*2012-8-16, add end*/
        }
    }

    void showDockDivider(boolean animated) {
        if (mQsbDivider != null && mDockDivider != null) {
            mQsbDivider.setVisibility(View.VISIBLE);
            mDockDivider.setVisibility(View.VISIBLE);
            if (mDividerAnimator != null) {
                mDividerAnimator.cancel();
                mQsbDivider.setAlpha(1f);
                mDockDivider.setAlpha(1f);
                mDividerAnimator = null;
            }
            if (animated) {
                mDividerAnimator = new AnimatorSet();
                mDividerAnimator.playTogether(ObjectAnimator.ofFloat(mQsbDivider, "alpha", 1f),
                        ObjectAnimator.ofFloat(mDockDivider, "alpha", 1f));
                mDividerAnimator.setDuration(mSearchDropTargetBar.getTransitionInDuration());
                mDividerAnimator.start();
            }
        /*2012-8-16, add by bvq783*/
        } else if (mDockDivider != null) {
            mDockDivider.setVisibility(View.VISIBLE);
            if (mDividerAnimator != null) {
                mDividerAnimator.cancel();
                mDockDivider.setAlpha(1f);
                mDividerAnimator = null;
            }
            if (animated) {
                mDividerAnimator = new AnimatorSet();
                mDividerAnimator.play(ObjectAnimator.ofFloat(mDockDivider, "alpha", 1f));
                mDividerAnimator.setDuration(mSearchDropTargetBar.getTransitionInDuration());
                mDividerAnimator.start();
            }
        /*2012-8-16, add by bvq783 end*/
        }             
    }

    void lockAllApps() {
        // TODO
    }

    void unlockAllApps() {
        // TODO
    }

    public boolean isAllAppsCustomizeOpen() {
        return mState == State.APPS_CUSTOMIZE;
    }

    //2012-02-27 added by rjg678 for IKSWITCHUI-65
    public boolean isWorkspaceOpen() {
        /*2012-3-8, add by bvq783 for IKSWITCHUI-101*/
        if (mWorkspace == null) {
            return false;
        } else {
        /*2012-3-8, add end*/
            return mState == State.WORKSPACE && !mWorkspace.isTouchActive()
                && mWorkspace.getOpenFolder() == null;
        }
    }
    //end by rjg678
    /**
     * Shows the hotseat area.
     */
    void showHotseat(boolean animated) {
        if (!LauncherApplication.isScreenLarge()) {
            if (animated) {
            	/*2012-11-26, Modifyed by amt_chenjing for switchuitwo-110*/
                if (mHotseat.getAlpha() != 1f || mHotseat.getVisibility() != View.VISIBLE) {
                	/*2012-11-26, Modify end*/
                    int duration = mSearchDropTargetBar.getTransitionInDuration();
                    mHotseat.animate().alpha(1f).setDuration(duration);
                }
            } else {
                mHotseat.setAlpha(1f);
            }
        }
    }

    /**
     * Hides the hotseat area.
     */
    void hideHotseat(boolean animated) {
        if (!LauncherApplication.isScreenLarge()) {
            if (animated) {
                if (mHotseat.getAlpha() != 0f) {
                    int duration = mSearchDropTargetBar.getTransitionOutDuration();
                    mHotseat.animate().alpha(0f).setDuration(duration);
                }
            } else {
                mHotseat.setAlpha(0f);
            }
        }
    }

    /**
     * Add an item from all apps or customize onto the given workspace screen.
     * If layout is null, add to the current screen.
     */
    void addExternalItemToScreen(ItemInfo itemInfo, final CellLayout layout) {
        if (!mWorkspace.addExternalItemToScreen(itemInfo, layout)) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
        }
    }

    /** Maps the current orientation to an index for referencing orientation correct global icons */
    private int getCurrentOrientationIndexForGlobalIcons() {
        // default - 0, landscape - 1
        switch (getResources().getConfiguration().orientation) {
        case Configuration.ORIENTATION_LANDSCAPE:
            return 1;
        default:
            return 0;
        }
    }

    private Drawable getExternalPackageToolbarIcon(ComponentName activityName, String resourceName) {
        if (activityName == null) {
            return null;
        }
        try {
            PackageManager packageManager = getPackageManager();
            // Look for the toolbar icon specified in the activity meta-data
            Bundle metaData = packageManager.getActivityInfo(
                    activityName, PackageManager.GET_META_DATA).metaData;
            if (metaData != null) {
                int iconResId = metaData.getInt(resourceName);
                if (iconResId != 0) {
                    Resources res = packageManager.getResourcesForActivity(activityName);
                    return res.getDrawable(iconResId);
                }
            }
        } catch (NameNotFoundException e) {
            // This can happen if the activity defines an invalid drawable
            Log.w(TAG, "Failed to load toolbar icon; " + activityName.flattenToShortString() +
                    " not found", e);
        } catch (Resources.NotFoundException nfe) {
            // This can happen if the activity defines an invalid drawable
            Log.w(TAG, "Failed to load toolbar icon from " + activityName.flattenToShortString(),
                    nfe);
        }
        return null;
    }

    // if successful in getting icon, return it; otherwise, set button to use default drawable
    private Drawable.ConstantState updateTextButtonWithIconFromExternalActivity(
            int buttonId, ComponentName activityName, int fallbackDrawableId,
            String toolbarResourceName) {
        Drawable toolbarIcon = getExternalPackageToolbarIcon(activityName, toolbarResourceName);
        Resources r = getResources();
        int w = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_width);
        int h = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_height);

        TextView button = (TextView) findViewById(buttonId);
        // If we were unable to find the icon via the meta-data, use a generic one
        if (toolbarIcon == null) {
            toolbarIcon = r.getDrawable(fallbackDrawableId);
            toolbarIcon.setBounds(0, 0, w, h);
            if (button != null) {
                button.setCompoundDrawables(toolbarIcon, null, null, null);
            }
            return null;
        } else {
            toolbarIcon.setBounds(0, 0, w, h);
            if (button != null) {
                button.setCompoundDrawables(toolbarIcon, null, null, null);
            }
            return toolbarIcon.getConstantState();
        }
    }

    // if successful in getting icon, return it; otherwise, set button to use default drawable
    private Drawable.ConstantState updateButtonWithIconFromExternalActivity(
            int buttonId, ComponentName activityName, int fallbackDrawableId,
            String toolbarResourceName) {
        ImageView button = (ImageView) findViewById(buttonId);
        Drawable toolbarIcon = getExternalPackageToolbarIcon(activityName, toolbarResourceName);

        if (button != null) {
            // If we were unable to find the icon via the meta-data, use a
            // generic one
            if (toolbarIcon == null) {
                button.setImageResource(fallbackDrawableId);
            } else {
                button.setImageDrawable(toolbarIcon);
            }
        }

        return toolbarIcon != null ? toolbarIcon.getConstantState() : null;

    }

    private void updateTextButtonWithDrawable(int buttonId, Drawable d) {
        TextView button = (TextView) findViewById(buttonId);
        button.setCompoundDrawables(d, null, null, null);
    }

    private void updateButtonWithDrawable(int buttonId, Drawable.ConstantState d) {
        ImageView button = (ImageView) findViewById(buttonId);
        button.setImageDrawable(d.newDrawable(getResources()));
    }

    private void invalidatePressedFocusedStates(View container, View button) {
        if (container instanceof HolographicLinearLayout) {
            HolographicLinearLayout layout = (HolographicLinearLayout) container;
            layout.invalidatePressedFocusedStates();
        } else if (button instanceof HolographicImageView) {
            HolographicImageView view = (HolographicImageView) button;
            view.invalidatePressedFocusedStates();
        }
    }

    private boolean updateGlobalSearchIcon() {
        final View searchButtonContainer = findViewById(R.id.search_button_container);
        final ImageView searchButton = (ImageView) findViewById(R.id.search_button);
        final View searchDivider = findViewById(R.id.search_divider);
        final View voiceButtonContainer = findViewById(R.id.voice_button_container);
        final View voiceButton = findViewById(R.id.voice_button);
        final View voiceButtonProxy = findViewById(R.id.voice_button_proxy);

        final SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        ComponentName activityName = searchManager.getGlobalSearchActivity();
        if (activityName != null) {
            int coi = getCurrentOrientationIndexForGlobalIcons();
            sGlobalSearchIcon[coi] = updateButtonWithIconFromExternalActivity(
                    R.id.search_button, activityName, R.drawable.ic_home_search_normal_holo,
                    TOOLBAR_SEARCH_ICON_METADATA_NAME);
            if (sGlobalSearchIcon[coi] == null) {
                sGlobalSearchIcon[coi] = updateButtonWithIconFromExternalActivity(
                        R.id.search_button, activityName, R.drawable.ic_home_search_normal_holo,
                        TOOLBAR_ICON_METADATA_NAME);
            }

            if (searchDivider != null) searchDivider.setVisibility(View.VISIBLE);
            if (searchButtonContainer != null) searchButtonContainer.setVisibility(View.VISIBLE);
            searchButton.setVisibility(View.VISIBLE);
            invalidatePressedFocusedStates(searchButtonContainer, searchButton);
            return true;
        } else {
            // We disable both search and voice search when there is no global search provider
            if (searchDivider != null) searchDivider.setVisibility(View.GONE);
            if (searchButtonContainer != null) searchButtonContainer.setVisibility(View.GONE);
            if (voiceButtonContainer != null) voiceButtonContainer.setVisibility(View.GONE);
            searchButton.setVisibility(View.GONE);
            voiceButton.setVisibility(View.GONE);
            if (voiceButtonProxy != null) {
                voiceButtonProxy.setVisibility(View.GONE);
            }
            return false;
        }
    }

    private void updateGlobalSearchIcon(Drawable.ConstantState d) {
        final View searchButtonContainer = findViewById(R.id.search_button_container);
        final View searchButton = (ImageView) findViewById(R.id.search_button);
        updateButtonWithDrawable(R.id.search_button, d);
        invalidatePressedFocusedStates(searchButtonContainer, searchButton);
    }

    private boolean updateVoiceSearchIcon(boolean searchVisible) {
        final View searchDivider = findViewById(R.id.search_divider);
        final View voiceButtonContainer = findViewById(R.id.voice_button_container);
        final View voiceButton = findViewById(R.id.voice_button);
        final View voiceButtonProxy = findViewById(R.id.voice_button_proxy);

        // We only show/update the voice search icon if the search icon is enabled as well
        final SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        ComponentName globalSearchActivity = searchManager.getGlobalSearchActivity();

        ComponentName activityName = null;
        if (globalSearchActivity != null) {
            // Check if the global search activity handles voice search
            Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            intent.setPackage(globalSearchActivity.getPackageName());
            activityName = intent.resolveActivity(getPackageManager());
        }

        if (activityName == null) {
            // Fallback: check if an activity other than the global search activity
            // resolves this
            Intent intent = new Intent(RecognizerIntent.ACTION_WEB_SEARCH);
            activityName = intent.resolveActivity(getPackageManager());
        }
        if (searchVisible && activityName != null) {
            int coi = getCurrentOrientationIndexForGlobalIcons();
            sVoiceSearchIcon[coi] = updateButtonWithIconFromExternalActivity(
                    R.id.voice_button, activityName, R.drawable.ic_home_voice_search_holo,
                    TOOLBAR_VOICE_SEARCH_ICON_METADATA_NAME);
            if (sVoiceSearchIcon[coi] == null) {
                sVoiceSearchIcon[coi] = updateButtonWithIconFromExternalActivity(
                        R.id.voice_button, activityName, R.drawable.ic_home_voice_search_holo,
                        TOOLBAR_ICON_METADATA_NAME);
            }
            if (searchDivider != null) searchDivider.setVisibility(View.VISIBLE);
            if (voiceButtonContainer != null) voiceButtonContainer.setVisibility(View.VISIBLE);
            if (voiceButton != null) voiceButton.setVisibility(View.VISIBLE);
            if (voiceButtonProxy != null) {
                voiceButtonProxy.setVisibility(View.VISIBLE);
            }
            if (voiceButtonContainer != null && voiceButton != null)
                invalidatePressedFocusedStates(voiceButtonContainer, voiceButton);
            return true;
        } else {
            if (searchDivider != null) searchDivider.setVisibility(View.GONE);
            if (voiceButtonContainer != null) voiceButtonContainer.setVisibility(View.GONE);
            if (voiceButton != null) voiceButton.setVisibility(View.GONE);
            if (voiceButtonProxy != null) {
                voiceButtonProxy.setVisibility(View.GONE);
            }
            return false;
        }
    }

    private void updateVoiceSearchIcon(Drawable.ConstantState d) {
        final View voiceButtonContainer = findViewById(R.id.voice_button_container);
        final View voiceButton = findViewById(R.id.voice_button);
        updateButtonWithDrawable(R.id.voice_button, d);
        if (voiceButtonContainer != null && voiceButton != null)
            invalidatePressedFocusedStates(voiceButtonContainer, voiceButton);
    }

    /**
     * Sets the app market icon
     */
    private void updateAppMarketIcon() {
        final View marketButton = findViewById(R.id.market_button);
        if (marketButton == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MARKET);
        // Find the app market activity by resolving an intent.
        // (If multiple app markets are installed, it will return the ResolverActivity.)
        ComponentName activityName = intent.resolveActivity(getPackageManager());
        if (activityName != null) {
            int coi = getCurrentOrientationIndexForGlobalIcons();
            mAppMarketIntent = intent;
            sAppMarketIcon[coi] = updateTextButtonWithIconFromExternalActivity(
                    R.id.market_button, activityName, R.drawable.ic_launcher_market_holo,
                    TOOLBAR_ICON_METADATA_NAME);
            marketButton.setVisibility(View.VISIBLE);
            /*2012-05-09, added by Chen Yidong for SWITCHUI-1055*/
            marketButton.setEnabled(true);
            /*2012-05-09, added end*/
        } else {
            // We should hide and disable the view so that we don't try and restore the visibility
            // of it when we swap between drag & normal states from IconDropTarget subclasses.
            marketButton.setVisibility(View.GONE);
            marketButton.setEnabled(false);
        }
    }

    private void updateAppMarketIcon(Drawable.ConstantState d) {
        // Ensure that the new drawable we are creating has the approprate toolbar icon bounds
        Resources r = getResources();
        Drawable marketIconDrawable = d.newDrawable(r);
        int w = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_width);
        int h = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_height);
        marketIconDrawable.setBounds(0, 0, w, h);

        updateTextButtonWithDrawable(R.id.market_button, marketIconDrawable);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        boolean result = super.dispatchPopulateAccessibilityEvent(event);
        final List<CharSequence> text = event.getText();
        text.clear();
        text.add(getString(R.string.home));
        return result;
    }

    /**
     * Receives notifications when system dialogs are to be closed.
     */
    private class CloseSystemDialogsIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            closeSystemDialogs();
        }
    }

    /**
     * Receives notifications whenever the appwidgets are reset.
     */
    private class AppWidgetResetObserver extends ContentObserver {
        public AppWidgetResetObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            onAppWidgetReset();
        }
    }

    /**
     * If the activity is currently paused, signal that we need to re-run the loader
     * in onResume.
     *
     * This needs to be called from incoming places where resources might have been loaded
     * while we are paused.  That is becaues the Configuration might be wrong
     * when we're not running, and if it comes back to what it was when we
     * were paused, we are not restarted.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     *
     * @return true if we are currently paused.  The caller might be able to
     * skip some work in that case since we will come back again.
     */
    public boolean setLoadOnResume() {
        if (mPaused) {
            Log.i(TAG, "setLoadOnResume");
            mOnResumeNeedsLoad = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public int getCurrentWorkspaceScreen() {
        if (mWorkspace != null) {
            return mWorkspace.getCurrentPage();
        } else {
            return SCREEN_COUNT / 2;
        }
    }

    /**
     * Refreshes the shortcuts shown on the workspace.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void startBinding() {
        final Workspace workspace = mWorkspace;

        mNewShortcutAnimatePage = -1;
        mNewShortcutAnimateViews.clear();
        mWorkspace.clearDropTargets();
        int count = workspace.getChildCount();
        for (int i = 0; i < count; i++) {
            // Use removeAllViewsInLayout() to avoid an extra requestLayout() and invalidate().
            final CellLayout layoutParent = (CellLayout) workspace.getChildAt(i);
            layoutParent.removeAllViewsInLayout();
        }
        //removed by amt_wangpeipei 2012/12/20 for SWITCHUITWO-365 begin
        /*2012-10-30, add by 003033 for switchui-2990*/
//        int num = mPluginWidgetInfos.size();
//        for (int i = 0; i < num; i++) {
//            LauncherPluginWidgetInfo info = mPluginWidgetInfos.get(i);
//            /*2012-10-31, modify by 003033 for switchui-3011*/
//            if (mPluginWidgets != null) {
//               PluginWidget widget = mPluginWidgets.get(info.key);
//               if (widget != null)
//                   widget.removeView(info.pluginWidgetId);
//            }
//            /*2012-10-31, modify end*/
//        }
        //removed by amt_wangpeipei 2012/12/20 for SWITCHUITWO-365 end.
        mPluginWidgetInfos.clear();
        /*2012-10-30, add end*/
        mWidgetsToAdvance.clear();
        if (mHotseat != null) {
            mHotseat.resetLayout();
        }
    }

    /**
     * Bind the items start-end from the list.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindItems(ArrayList<ItemInfo> shortcuts, int start, int end) {
        setLoadOnResume();

        // Get the list of added shortcuts and intersect them with the set of shortcuts here
        Set<String> newApps = new HashSet<String>();
        newApps = mSharedPrefs.getStringSet(InstallShortcutReceiver.NEW_APPS_LIST_KEY, newApps);

        Workspace workspace = mWorkspace;
        for (int i = start; i < end; i++) {
            final ItemInfo item = shortcuts.get(i);

            // Short circuit if we are loading dock items for a configuration which has no dock
            if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT &&
                    mHotseat == null) {
                continue;
            }

            switch (item.itemType) {
                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    ShortcutInfo info = (ShortcutInfo) item;
                    String uri = info.intent.toUri(0).toString();
                    View shortcut = createShortcut(info);
                    workspace.addInScreen(shortcut, item.container, item.screen, item.cellX,
                            item.cellY, 1, 1, false);
                    boolean animateIconUp = false;
                    synchronized (newApps) {
                        if (newApps.contains(uri)) {
                            animateIconUp = newApps.remove(uri);
                        }
                    }
                    if (animateIconUp) {
                        // Prepare the view to be animated up
                        shortcut.setAlpha(0f);
                        shortcut.setScaleX(0f);
                        shortcut.setScaleY(0f);
                        mNewShortcutAnimatePage = item.screen;
                        if (!mNewShortcutAnimateViews.contains(shortcut)) {
                            mNewShortcutAnimateViews.add(shortcut);
                        }
                    }
                    break;
                case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                    FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon, this,
                            (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()),
                            (FolderInfo) item, mIconCache);
                    workspace.addInScreen(newFolder, item.container, item.screen, item.cellX,
                            item.cellY, 1, 1, false);
                    break;
            }
        }

        workspace.requestLayout();
    }

    /**
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindFolders(HashMap<Long, FolderInfo> folders) {
        setLoadOnResume();
        sFolders.clear();
        sFolders.putAll(folders);
    }

    /**
     * Add the views for a widget to the workspace.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppWidget(LauncherAppWidgetInfo item) {
        setLoadOnResume();

        final long start = DEBUG_WIDGETS ? SystemClock.uptimeMillis() : 0;
        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bindAppWidget: " + item);
        }
        final Workspace workspace = mWorkspace;

        final int appWidgetId = item.appWidgetId;
        final AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        // 04/13/2012 wag044: IKHSS7-23561 avoid FC by disabled widget provider
        // If it continue disabled, it will be removed on next WS load.
        if (appWidgetInfo == null) {
            return;
        }

        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bindAppWidget: id=" + item.appWidgetId + " belongs to component " + appWidgetInfo.provider);
        }

        item.hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);

        item.hostView.setTag(item);
        item.onBindAppWidget(this);

        workspace.addInScreen(item.hostView, item.container, item.screen, item.cellX,
                item.cellY, item.spanX, item.spanY, false);
        addWidgetToAutoAdvanceIfNeeded(item.hostView, appWidgetInfo);

        workspace.requestLayout();

        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bound widget id="+item.appWidgetId+" in "
                    + (SystemClock.uptimeMillis()-start) + "ms");
        }
    }

    public void onPageBoundSynchronously(int page) {
        mSynchronouslyBoundPages.add(page);
    }

    /**
     * Callback saying that there aren't any more items to bind.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void finishBindingItems() {
        setLoadOnResume();

        if (mSavedState != null) {
            if (!mWorkspace.hasFocus()) {
                mWorkspace.getChildAt(mWorkspace.getCurrentPage()).requestFocus();
            }
            mSavedState = null;
        }
        /*2012-11-21, add catch exception by 003033 for switchuitwo-86*/
        try {
            mWorkspace.restoreInstanceStateForRemainingPages();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "finish binding items meet error:"+e);
        }
        /*2012-11-21, add end*/
        // If we received the result of any pending adds while the loader was running (e.g. the
        // widget configuration forced an orientation change), process them now.
        for (int i = 0; i < sPendingAddList.size(); i++) {
            completeAdd(sPendingAddList.get(i));
        }
        sPendingAddList.clear();

        // Update the market app icon as necessary (the other icons will be managed in response to
        // package changes in bindSearchablesChanged()
        updateAppMarketIcon();

        // Animate up any icons as necessary
        if (mVisible || mWorkspaceLoading) {
            Runnable newAppsRunnable = new Runnable() {
                @Override
                public void run() {
                    runNewAppsAnimation(false);
                }
            };

            boolean willSnapPage = mNewShortcutAnimatePage > -1 &&
                    mNewShortcutAnimatePage != mWorkspace.getCurrentPage();
            if (canRunNewAppsAnimation()) {
                // If the user has not interacted recently, then either snap to the new page to show
                // the new-apps animation or just run them if they are to appear on the current page
                if (willSnapPage) {
                    mWorkspace.snapToPage(mNewShortcutAnimatePage, newAppsRunnable);
                } else {
                    runNewAppsAnimation(false);
                }
            } else {
                // If the user has interacted recently, then just add the items in place if they
                // are on another page (or just normally if they are added to the current page)
                runNewAppsAnimation(willSnapPage);
            }
        }

        mWorkspaceLoading = false;
    }

    private boolean canRunNewAppsAnimation() {
        long diff = System.currentTimeMillis() - mDragController.getLastGestureUpTime();
        return diff > (NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS * 1000);
    }

    /**
     * Runs a new animation that scales up icons that were added while Launcher was in the
     * background.
     *
     * @param immediate whether to run the animation or show the results immediately
     */
    private void runNewAppsAnimation(boolean immediate) {
        AnimatorSet anim = new AnimatorSet();
        Collection<Animator> bounceAnims = new ArrayList<Animator>();

        // Order these new views spatially so that they animate in order
        Collections.sort(mNewShortcutAnimateViews, new Comparator<View>() {
            @Override
            public int compare(View a, View b) {
                CellLayout.LayoutParams alp = (CellLayout.LayoutParams) a.getLayoutParams();
                CellLayout.LayoutParams blp = (CellLayout.LayoutParams) b.getLayoutParams();
                int cellCountX = LauncherModel.getCellCountX();
                return (alp.cellY * cellCountX + alp.cellX) - (blp.cellY * cellCountX + blp.cellX);
            }
        });

        // Animate each of the views in place (or show them immediately if requested)
        if (immediate) {
            for (View v : mNewShortcutAnimateViews) {
                v.setAlpha(1f);
                v.setScaleX(1f);
                v.setScaleY(1f);
            }
        } else {
            for (int i = 0; i < mNewShortcutAnimateViews.size(); ++i) {
                View v = mNewShortcutAnimateViews.get(i);
                ValueAnimator bounceAnim = ObjectAnimator.ofPropertyValuesHolder(v,
                        PropertyValuesHolder.ofFloat("alpha", 1f),
                        PropertyValuesHolder.ofFloat("scaleX", 1f),
                        PropertyValuesHolder.ofFloat("scaleY", 1f));
                bounceAnim.setDuration(InstallShortcutReceiver.NEW_SHORTCUT_BOUNCE_DURATION);
                bounceAnim.setStartDelay(i * InstallShortcutReceiver.NEW_SHORTCUT_STAGGER_DELAY);
                bounceAnim.setInterpolator(new SmoothPagedView.OvershootInterpolator());
                bounceAnims.add(bounceAnim);
            }
            anim.playTogether(bounceAnims);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mWorkspace.postDelayed(mBuildLayersRunnable, 500);
                }
            });
            anim.start();
        }

        // Clean up
        mNewShortcutAnimatePage = -1;
        mNewShortcutAnimateViews.clear();
        new Thread("clearNewAppsThread") {
            public void run() {
                mSharedPrefs.edit()
                            .putInt(InstallShortcutReceiver.NEW_APPS_PAGE_KEY, -1)
                            .putStringSet(InstallShortcutReceiver.NEW_APPS_LIST_KEY, null)
                            .commit();
            }
        }.start();
    }

    @Override
    public void bindSearchablesChanged() {
        boolean searchVisible = updateGlobalSearchIcon();
        boolean voiceVisible = updateVoiceSearchIcon(searchVisible);
        mSearchDropTargetBar.onSearchPackagesChanged(searchVisible, voiceVisible);
    }

    /**
     * Add the icons for all apps.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAllApplications(final ArrayList<ApplicationInfo> apps) {
        Runnable setAllAppsRunnable = new Runnable() {
            public void run() {
            	/*2012-11-19, Modifyed by amt_chenjing for switchuitwo-44*/
                //if (mAppsCustomizeContent != null) {
                    //mAppsCustomizeContent.setApps(apps);
                //}
                if (mAllAppsPage != null) {
                    mAllAppsPage.setApps(apps);
                }
                /*2012-11-19, Modify end*/
            }
        };

        // Remove the progress bar entirely; we could also make it GONE
        // but better to remove it since we know it's not going to be used
        View progressBar = mAppsCustomizeTabHost.
            findViewById(R.id.apps_customize_progress_bar);
        if (progressBar != null) {
            ((ViewGroup)progressBar.getParent()).removeView(progressBar);

            // We just post the call to setApps so the user sees the progress bar
            // disappear-- otherwise, it just looks like the progress bar froze
            // which doesn't look great
            mAppsCustomizeTabHost.post(setAllAppsRunnable);
        } else {
            // If we did not initialize the spinner in onCreate, then we can directly set the
            // list of applications without waiting for any progress bars views to be hidden.
            setAllAppsRunnable.run();
        }
    }

    /**
     * A package was installed.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsAdded(ArrayList<ApplicationInfo> apps) {
        setLoadOnResume();
	/*Added by ncqp34 at Mar-23-2012 for fake app*/
	//step1:remove all shortcut on workspace
	Log.d(TAG,"bindAppsAdded--apps=" + apps);
	mWorkspace.removeFakeItems(apps);
	/*ended by ncqp34*/
	/*2012-5-17 Added by e13775 for SWITCHUI-1170*/
        if (mWorkspace != null) {
            mWorkspace.updateShortcuts(apps);
        }
	/*2012-5-17 Added by e13775 for SWITCHUI-1170*/

        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.addApps(apps);
        }
    }

    /**
     * A package was updated.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsUpdated(ArrayList<ApplicationInfo> apps) {
        setLoadOnResume();
        if (mWorkspace != null) {
            mWorkspace.updateShortcuts(apps);
        }

        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.updateApps(apps);
        }
    }

    /**
     * A package was uninstalled.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindAppsRemoved(ArrayList<ApplicationInfo> apps, boolean permanent) {
        if (permanent) {
            mWorkspace.removeItems(apps);
        }

        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.removeApps(apps);
        }

        // Notify the drag controller
        mDragController.onAppsRemoved(apps, this);
    }

    /**
     * A number of packages were updated.
     */
    public void bindPackagesUpdated() {
        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.onPackagesUpdated(false);
        }
    }

    private int mapConfigurationOriActivityInfoOri(int configOri) {
        final Display d = getWindowManager().getDefaultDisplay();
        int naturalOri = Configuration.ORIENTATION_LANDSCAPE;
        switch (d.getRotation()) {
        case Surface.ROTATION_0:
        case Surface.ROTATION_180:
            // We are currently in the same basic orientation as the natural orientation
            naturalOri = configOri;
            break;
        case Surface.ROTATION_90:
        case Surface.ROTATION_270:
            // We are currently in the other basic orientation to the natural orientation
            naturalOri = (configOri == Configuration.ORIENTATION_LANDSCAPE) ?
                    Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE;
            break;
        }

        int[] oriMap = {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        };
        // Since the map starts at portrait, we need to offset if this device's natural orientation
        // is landscape.
        int indexOffset = 0;
        if (naturalOri == Configuration.ORIENTATION_LANDSCAPE) {
            indexOffset = 1;
        }
        return oriMap[(d.getRotation() + indexOffset) % 4];
    }

    public boolean isRotationEnabled() {
        boolean forceEnableRotation = "true".equalsIgnoreCase(SystemProperties.get(
                FORCE_ENABLE_ROTATION_PROPERTY, "false"));
        boolean enableRotation = forceEnableRotation ||
                getResources().getBoolean(R.bool.allow_rotation);
        return enableRotation;
    }
    public void lockScreenOrientation() {
        if (isRotationEnabled()) {
            setRequestedOrientation(mapConfigurationOriActivityInfoOri(getResources()
                    .getConfiguration().orientation));
        }
    }
    public void unlockScreenOrientation(boolean immediate) {
        if (isRotationEnabled()) {
            if (immediate) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            } else {
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }
                }, mRestoreScreenOrientationDelay);
            }
        }
    }

    /* Cling related */
    private boolean isClingsEnabled() {
        // disable clings when running in a test harness
        if(ActivityManager.isRunningInTestHarness()) return false;

        return true;
    }
    private Cling initCling(int clingId, int[] positionData, boolean animate, int delay) {
        Cling cling = (Cling) findViewById(clingId);
        if (cling != null) {
            cling.init(this, positionData);
            cling.setVisibility(View.VISIBLE);
            cling.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            cling.requestAccessibilityFocus();
            if (animate) {
                cling.buildLayer();
                cling.setAlpha(0f);
                cling.animate()
                    .alpha(1f)
                    .setInterpolator(new AccelerateInterpolator())
                    .setDuration(SHOW_CLING_DURATION)
                    .setStartDelay(delay)
                    .start();
            } else {
                cling.setAlpha(1f);
            }
        }
        return cling;
    }
    private void dismissCling(final Cling cling, final String flag, int duration) {
        if (cling != null) {
            ObjectAnimator anim = ObjectAnimator.ofFloat(cling, "alpha", 0f);
            anim.setDuration(duration);
            anim.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    cling.setVisibility(View.GONE);
                    cling.cleanup();
                    // We should update the shared preferences on a background thread
                    new Thread("dismissClingThread") {
                        public void run() {
                            SharedPreferences.Editor editor = mSharedPrefs.edit();
                            editor.putBoolean(flag, true);
                            editor.commit();
                        }
                    }.start();
                };
            });
            anim.start();
        }
    }
    private void removeCling(int id) {
        final View cling = findViewById(id);
        if (cling != null) {
            final ViewGroup parent = (ViewGroup) cling.getParent();
            parent.post(new Runnable() {
                @Override
                public void run() {
                    parent.removeView(cling);
                }
            });
        }
    }

    private boolean skipCustomClingIfNoAccounts() {
        Cling cling = (Cling) findViewById(R.id.workspace_cling);
        boolean customCling = cling.getDrawIdentifier().equals("workspace_custom");
        if (customCling) {
            AccountManager am = AccountManager.get(this);
            Account[] accounts = am.getAccountsByType("com.google");
            return accounts.length == 0;
        }
        return false;
    }

    public void showFirstRunWorkspaceCling() {
        // Enable the clings only if they have not been dismissed before
        if (isClingsEnabled() &&
                !mSharedPrefs.getBoolean(Cling.WORKSPACE_CLING_DISMISSED_KEY, false) &&
                !skipCustomClingIfNoAccounts() ) {
            initCling(R.id.workspace_cling, null, false, 0);
        } else {
            removeCling(R.id.workspace_cling);
        }
    }
    public void showFirstRunAllAppsCling(int[] position) {
        // Enable the clings only if they have not been dismissed before
        if (isClingsEnabled() &&
                !mSharedPrefs.getBoolean(Cling.ALLAPPS_CLING_DISMISSED_KEY, false)) {
            initCling(R.id.all_apps_cling, position, true, 0);
        } else {
            removeCling(R.id.all_apps_cling);
        }
    }
    public Cling showFirstRunFoldersCling() {
        // Enable the clings only if they have not been dismissed before
        if (isClingsEnabled() &&
                !mSharedPrefs.getBoolean(Cling.FOLDER_CLING_DISMISSED_KEY, false)) {
            return initCling(R.id.folder_cling, null, true, 0);
        } else {
            removeCling(R.id.folder_cling);
            return null;
        }
    }
    public boolean isFolderClingVisible() {
        Cling cling = (Cling) findViewById(R.id.folder_cling);
        if (cling != null) {
            return cling.getVisibility() == View.VISIBLE;
        }
        return false;
    }
    public void dismissWorkspaceCling(View v) {
        Cling cling = (Cling) findViewById(R.id.workspace_cling);
        dismissCling(cling, Cling.WORKSPACE_CLING_DISMISSED_KEY, DISMISS_CLING_DURATION);
    }
    public void dismissAllAppsCling(View v) {
        Cling cling = (Cling) findViewById(R.id.all_apps_cling);
        dismissCling(cling, Cling.ALLAPPS_CLING_DISMISSED_KEY, DISMISS_CLING_DURATION);
    }
    public void dismissFolderCling(View v) {
        Cling cling = (Cling) findViewById(R.id.folder_cling);
        dismissCling(cling, Cling.FOLDER_CLING_DISMISSED_KEY, DISMISS_CLING_DURATION);
    }

    /**
     * Prints out out state for debugging.
     */
    public void dumpState() {
        Log.d(TAG, "BEGIN launcher2 dump state for launcher " + this);
        Log.d(TAG, "mSavedState=" + mSavedState);
        Log.d(TAG, "mWorkspaceLoading=" + mWorkspaceLoading);
        Log.d(TAG, "mRestoring=" + mRestoring);
        Log.d(TAG, "mWaitingForResult=" + mWaitingForResult);
        Log.d(TAG, "mSavedInstanceState=" + mSavedInstanceState);
        Log.d(TAG, "sFolders.size=" + sFolders.size());
        mModel.dumpState();

        if (mAppsCustomizeContent != null) {
            mAppsCustomizeContent.dumpState();
        }
        Log.d(TAG, "END launcher2 dump state");
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.println(" ");
        writer.println("Debug logs: ");
        for (int i = 0; i < sDumpLogs.size(); i++) {
            writer.println("  " + sDumpLogs.get(i));
        }
    }

    /*2012-8-3, add by bvq783 for plugin */
    /**
     * Callback saying that bind plugin widget.
     *
     * Implementation of the method from LauncherModel.Callbacks.
     */
    public void bindPluginWidget(LauncherPluginWidgetInfo info) {
        setLoadOnResume();
        final long start = DEBUG_WIDGETS ? SystemClock.uptimeMillis() : 0;
        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bindPluginWidget: " + info);
        }
        if (mPluginWidgetHost == null)
            return;
        final Workspace workspace = mWorkspace;

        final int widgetId = info.pluginWidgetId;
        String key = mPluginWidgetHost.getPluginWidgetKey(widgetId);
        /*Add for flex plugin widget*/
        if(key == null) {
        	PluginWidgetHostId pluginHost = new PluginWidgetHostId(this);
        	key = pluginHost.getPluginWidgetKey(widgetId);
                mPluginWidgetHost.setPluginWidgetHostId(pluginHost);
        }
        /*Add end*/
        if (mPluginWidgets == null)
            mPluginWidgets = mPluginWidgetHost.getPluginWidgets();
        PluginWidget widget = mPluginWidgets.get(key);
        /*2012-6-7, add by bvq783 for switchui-1936*/
        if (widget == null) {
            LauncherModel.deleteItemFromDatabase(this, info);
            return;
        }
        /*2012-6-7, add by bvq783 end*/
        PluginWidgetProviderInfo provider = (PluginWidgetProviderInfo)widget.mProvider;
        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bindAppWidget: id=" + info.pluginWidgetId + " belongs to component " + provider);
        }
        info.key = provider.provider.getClassName();
        info.hostView = (PluginWidgetHostView)mPluginWidgetHost.creatHostView(info.key, widgetId);
        /*2012-6-7, add by bvq783 for switchui-1936*/
        if (info.hostView == null) {
            LauncherModel.deleteItemFromDatabase(this, info);
            return;
        }
        /*2012-6-7, add by bvq783 end*/
        //2012-09-05, ChenYidong for SWITCHUI-2675
        int[] minSpanXY = getMinSpanForPluginWidget(provider.provider, provider.minResizeWidth, provider.minResizeHeight, null);
        info.minSpanX = minSpanXY[0];
        info.minSpanY = minSpanXY[1];
        //2012-09-05, end
        info.hostView.setTag(info);

        workspace.addInScreen(info.hostView, info.container, info.screen, info.cellX,
                info.cellY, info.spanX, info.spanY, false);

        workspace.requestLayout();
        /*2012-6-7, add by bvq783 for switchui-1936*/
        PluginWidgetScrollListener listener = info.hostView.getScrollListener();
        if (listener != null) {
            listener.createViewCompleted(mModel, widgetId);
        }
        /*2012-6-7, add by bvq783 end*/
        /*2012-7-15, add by bvq783 for switchui-2229*/
        mPluginWidgetInfos.add(info);
        /*2012-7-15, add by bvq783 end*/
        if (DEBUG_WIDGETS) {
            Log.d(TAG, "bound widget id="+info.pluginWidgetId+" in "
                    + (SystemClock.uptimeMillis()-start) + "ms");
        }
    }

    /**
     * Process a widget drop.
     *
     * @param info The PendingAppWidgetInfo of the widget being added.
     * @param screen The screen where it should be added
     * @param cell The cell it should be added to, optional
     * @param position The location on the screen where it was dropped, optional
     */
    public void addPluginWidgetFromDrop(ComponentName provider, long container, int screen,
            int[] cell, int[] span, int[] loc) {
        resetAddInfo();
        mPendingAddInfo.container = container;
        mPendingAddInfo.screen = screen;
        mPendingAddInfo.dropPos = loc;

        if (cell != null) {
            mPendingAddInfo.cellX = cell[0];
            mPendingAddInfo.cellY = cell[1];
        }
        if (span != null) {
            mPendingAddInfo.spanX = span[0];
            mPendingAddInfo.spanY = span[1];
        }
        completeAddPluginWidget(provider.getClassName());
        // Exit spring loaded mode if necessary after adding the widget
        exitSpringLoadedDragModeDelayed(true, false, null);
    }

    private void completeAddPluginWidget(String key) {
        if (key == null || mPluginWidgetHost == null)
            return;
        int pluginId = mPluginWidgetHost.allocatePluginWidgetId(key);
        // Calculate the grid spans needed to fit this widget
        CellLayout layout = getCellLayout(mPendingAddInfo.container, mPendingAddInfo.screen);

        if (mPluginWidgetHost != null && mPluginWidgets == null)
            mPluginWidgets = mPluginWidgetHost.getPluginWidgets();

        PluginWidget widget = mPluginWidgets.get(key);
        if (widget == null)
            return;
        PluginWidgetProviderInfo info = (PluginWidgetProviderInfo)widget.mProvider;
        int[] spanXY = getSpanForPluginWidget(info.provider, info.minWidth, info.minHeight, null);
        int[] minSpanXY = getMinSpanForPluginWidget(info.provider, info.minResizeWidth, info.minResizeHeight, null);

        // Try finding open space on Launcher screen
        // We have saved the position to which the widget was dragged-- this really only matters
        // if we are placing widgets on a "spring-loaded" screen
        int[] cellXY = mTmpAddItemCellCoordinates;
        int[] touchXY = mPendingAddInfo.dropPos;
        boolean foundCellSpan = false;
        if (mPendingAddInfo.cellX >= 0 && mPendingAddInfo.cellY >= 0) {
            cellXY[0] = mPendingAddInfo.cellX;
            cellXY[1] = mPendingAddInfo.cellY;
            spanXY[0] = mPendingAddInfo.spanX;
            spanXY[1] = mPendingAddInfo.spanY;
            foundCellSpan = true;
        } else if (touchXY != null) {
            // when dragging and dropping, just find the closest free spot
            int[] result = layout.findNearestVacantArea(
                    touchXY[0], touchXY[1], spanXY[0], spanXY[1], cellXY);
            foundCellSpan = (result != null);
        } else {
            foundCellSpan = layout.findCellForSpan(cellXY, spanXY[0], spanXY[1]);
        }

        if (!foundCellSpan) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }

        // Build Launcher-specific widget info and save to database
        LauncherPluginWidgetInfo launcherInfo = new LauncherPluginWidgetInfo(pluginId);
        launcherInfo.spanX = spanXY[0];
        launcherInfo.spanY = spanXY[1];
        //2012-09-05, ChenYidong for SWITCHUI-2675
        launcherInfo.minSpanX = minSpanXY[0];
        launcherInfo.minSpanY = minSpanXY[1];
        //2012-09-05, end
        launcherInfo.pluginWidgetId = pluginId;
        launcherInfo.appWidgetId = pluginId;
        launcherInfo.key = info.provider.getClassName();
        launcherInfo.container = mPendingAddInfo.container;
        launcherInfo.screen = mPendingAddInfo.screen;

        LauncherModel.addItemToDatabase(this, launcherInfo,
                launcherInfo.container, launcherInfo.screen, cellXY[0], cellXY[1], false);

        if (!mRestoring) {
            // Perform actual inflation because we're live
            launcherInfo.hostView = (PluginWidgetHostView)mPluginWidgetHost.creatHostView(key, pluginId);
            /*2012-6-7, add by bvq783 for switchui-1936*/
            if (launcherInfo.hostView == null) {
                LauncherModel.deleteItemFromDatabase(this, launcherInfo);
                return;
            }
            /*2012-6-7, add by bvq783 end*/
            launcherInfo.hostView.setTag(launcherInfo);
            launcherInfo.hostView.setVisibility(View.VISIBLE);
            mWorkspace.addInScreen(launcherInfo.hostView, launcherInfo.container, launcherInfo.screen, 
                    cellXY[0], cellXY[1], launcherInfo.spanX, launcherInfo.spanY, isWorkspaceLocked());
            /*2012-6-7, add by bvq783 for switchui-1936*/
            PluginWidgetScrollListener listener = launcherInfo.hostView.getScrollListener();
            if (listener != null) {
                listener.createViewCompleted(mModel, pluginId);
            } 
            /*2012-6-7, add by bvq783 end*/
            /*2012-7-15, add by bvq783 for switchui-2229*/
            mPluginWidgetInfos.add(launcherInfo);
            /*2012-7-15, add by bvq783 end*/
        }
        resetAddInfo();        
    }

    int[] getSpanForPluginWidget(ComponentName component, int minWidth, int minHeight, int[] spanXY) {
        if (spanXY == null) {
            spanXY = new int[2];
        }

        return CellLayout.rectToCell(getResources(), minWidth, minHeight, null);
    }

    int[] getMinSpanForPluginWidget(ComponentName component, int minResizeWidth, int minResizeHeight, int[] spanXY) {
        return getSpanForPluginWidget(component, minResizeWidth, minResizeHeight, spanXY);
    }

    public void removePluginWidget(LauncherPluginWidgetInfo info) {
        if (mPluginWidgetHost != null) {
            mPluginWidgetHost.removeHostView(info.key, info.pluginWidgetId);
        }
        /*2012-7-15, add by bvq783 for switchui-2229*/
        mPluginWidgetInfos.remove(info);
        /*2012-7-15, add by bvq783 end*/
        info = null;
    }

    public ArrayList<PluginWidgetProviderInfo> getPluginWidgetList() {
        if (mPluginWidgetHost == null)
            return null;
        mPluginWidgets = mPluginWidgetHost.getPluginWidgets();
        ArrayList<PluginWidgetProviderInfo> list = new ArrayList<PluginWidgetProviderInfo>();
        Set set = mPluginWidgets.entrySet();
        Iterator itr = set.iterator();
        while(itr.hasNext()) {
            Map.Entry map = (Map.Entry)itr.next();
            String pkg = map.getKey().toString();
            PluginWidget widget = (PluginWidget)map.getValue();
            PluginWidgetProviderInfo info = widget.mProvider;
            list.add(info);
        }
        return list;
    }

    public AppWidgetProviderInfo getAppWidgetProviderInfo(PluginWidgetProviderInfo info) {
        if (info == null)
            return null;
        AppWidgetProviderInfo item = new AppWidgetProviderInfo();
        item.provider = info.provider;
        item.minWidth = info.minWidth;
        item.minHeight = info.minHeight;
        item.minResizeWidth = info.minResizeHeight;
        item.minResizeHeight = info.minResizeWidth;
        item.updatePeriodMillis = info.updatePeriodMillis;
        item.initialLayout = info.initialLayout;
        item.label = info.label;
        item.icon = info.icon;
        item.autoAdvanceViewId = info.autoAdvanceViewId;
        item.previewImage = info.previewImage;
        item.resizeMode = info.resizeMode;

        return item;
    }

    public void bindPluginAdded(String[] widgets) {
    	
    }

    public void bindPluginRemoved(final String[] widgets, boolean permanent) {
    	if (permanent)  {
            mWorkspace.removePluginItems(widgets);
            mHandler.post(new Runnable() {
                public void run() {
                for (int i=0; i<widgets.length; i++)
                    mPluginWidgetHost.removePluginWidget(widgets[i]);
                }
            });
    	}
    }

    public void bindPluginUpdated(String[] widgets) {
    	
    }

    /*2011-12-31, DJHV83 Added for Data Switch*/
    private class DataSwitchReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "DataSwitchReceiver: onReceive, action=" + action);
            Bundle extras = intent.getExtras();
            String appLaunchPoint = null;
            if(extras !=null){
                appLaunchPoint = extras.getString("AppLaunchPoint");
                Log.d(TAG,"AppLaunchPoint"+appLaunchPoint);
            }
            if("com.motorola.dataalert.LAUNCH_APP".equals(action) && appLaunchPoint!=null && 
                appLaunchPoint.equals("Home")) {
                launchActivity();
            }
        }
    }

    public void launchActivity(){
        Log.d(TAG, "Launch activity");
        if(mCurrentIntent != null){
            int[] pos = new int[2];
            mCurrentView.getLocationOnScreen(pos);
            mCurrentIntent.setSourceBounds(new Rect(pos[0], pos[1],
                            pos[0] + mCurrentView.getWidth(), pos[1] + mCurrentView.getHeight()));
            startActivitySafely(null, mCurrentIntent, mCurrentView.getTag());
            mCurrentIntent = null;
            mCurrentView = null;
        }
    }

    private boolean checkDataSwitchValid(){
        final PackageManager packageManager = this.getPackageManager();
        Intent intent = new Intent(DataSwitchUtil.ACTION_DATA_CONNECTION_DIALOG);
        
        final List<ResolveInfo> app = packageManager.queryIntentActivities(intent, 0);
        if(app != null && app.size() != 0){
             for (int i = 0; i < app.size(); ++i) {
                if(app.get(i).activityInfo.applicationInfo.packageName.equals(DATAALERTAPK)){
                    LauncherApplication ap = ((LauncherApplication)getApplication());
                    ap.setDataSwitchEnable(true);
                    return true;
                }
            }
        }
        return false;
    }
    /*DJHV83 end*/

    /*2012-6-7, add by bvq783 for switchui-1936*/
    public boolean checkCurrentPage(int widgetId) {
        CellLayout v = (CellLayout)mWorkspace.getChildAt(mWorkspace.getCurrentPage());
        if (v != null) {
            ViewGroup vv = (ViewGroup)v.getChildAt(0);
            for (int j=0; j<vv.getChildCount(); j++) {
                View vvv = vv.getChildAt(j);
                if (vvv instanceof PluginWidgetHostView) {
                    LauncherPluginWidgetInfo info = (LauncherPluginWidgetInfo)vvv.getTag();
                    if (info.pluginWidgetId == widgetId) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    /*2012-6-7, add by bvq783 end*/

    /*2012-7-15, add by bvq783 for switchui-2229*/
    public ArrayList<LauncherPluginWidgetInfo> getPluginWidgets() {
        return mPluginWidgetInfos;
    }
    /*2012-7-15, add by bvq783 end*/
 
    //2012-07-25, ChenYidong for SWITCHUI-2442, add param isAllAppsClicked
    public void resetPagedView(PagedView pagedView, boolean isAllAppsClicked) {
        if(pagedView.mFirstLayout)return;
        //2012-07-20, ChenYidong for SWITCHUI-2371 
        //2012-07-25, ChenYidong for SWITCHUI-2442, add isAllAppsClicked
        if(!(pagedView instanceof Workspace) || isAllAppsClicked){
            pagedView.setLayoutScale(1.0f);
        }
        //2012-07-20, end
        float finalScaleFactor = 1.0f;
        float finalBackgroundAlpha = 0f;
        float finalAlphaMultiplierValue = 1f;
        float finalAlpha = 1.0f;
        float translationX = 0;
        float translationY = 0;
        float rotation = 0f;
        for (int i = 0; i < pagedView.getPageCount(); i++) {
            final View v = pagedView.getPageAt(i);
            v.setTranslationX(translationX);
            v.setTranslationY(translationY);
            v.setRotation(rotation);
            v.setRotationX(rotation);
            v.setRotationY(rotation);
            v.setPivotX(v.getMeasuredWidth() / 2);
            v.setPivotY(v.getMeasuredHeight() / 2);
            v.setScaleX(finalScaleFactor);
            v.setScaleY(finalScaleFactor);
            v.setVisibility(View.VISIBLE);
            v.setAlpha(finalAlpha);
            /*2012-11-20, Added for SWITCHUITWO-52*/
            if ( v instanceof CellLayout ) {
                ((CellLayout)v).setOverScrollAmount(0, i == 0);
            }
            /*2012-11-20, Add end*/
        }
    }
    //hnd734 Home Transition end
    /*2012-2-27, add by bvq783 for IKSWITCHUI-62*/
    private void removeItemsbyScreen(int index) {
        CellLayout cell = (CellLayout)mWorkspace.getChildAt(index);
        ShortcutAndWidgetContainer child = (ShortcutAndWidgetContainer)cell.getChildAt(0);
        int num = child.getChildCount();
        for (int i=0; i<num; i++) {
            View v = child.getChildAt(i);
            if (v instanceof LauncherAppWidgetHostView) {
               removeWidgetToAutoAdvance(v);
            /*2012-3-12, add by bvq783 for plugin*/
            }	
			else if (v instanceof PluginWidgetHostView) {
               removePluginWidget((LauncherPluginWidgetInfo)(v.getTag()));
            /*2012-3-12, add end*/
            }			
        }
    }
    /*2012-2-27, add end*/
    
    /*add by bvq783 for enter transition*/
    public int getWorkspaceEffect() {
        return mWorkspaceTransitionEffect;
    }

    public void showUnlockAnimation() {
        if (et != null) {
     	   View cell = mWorkspace.getChildAt(getCurrentWorkspaceScreen());
     	   et.beginEntryTransition(cell, mSearchDropTargetBar, mHotseat, hotseatbg);
        }
    }

    public void stopUnlockAnimation() {
       if (et != null) {
    	   View cell = mWorkspace.getChildAt(getCurrentWorkspaceScreen());
    	   et.endEntryTransition(cell, mSearchDropTargetBar, mHotseat, hotseatbg);
       }
    	   
    }

    private void resetView(View v) {
       v.clearAnimation();
       v.setAlpha(1f);
       v.setScaleX(1f);
       v.setScaleY(1f);
    }

    public void resetCurrentPage() {
        /*2012-6-13, add by bvq783 for switchui-1447*/
        if (mState == State.WORKSPACE) {
        /*2012-6-13, add end*/
            mHotseat.setVisibility(View.VISIBLE);
            mSearchDropTargetBar.setVisibility(View.VISIBLE);
            mWorkspace.setVisibility(View.VISIBLE);
            hotseatbg.setVisibility(View.VISIBLE);
        }
    }
    /*add by bvq783 end*/
}

interface LauncherTransitionable {
    View getContent();
    void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace);
    void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace);
    void onLauncherTransitionStep(Launcher l, float t);
    void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace);
}
