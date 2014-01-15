package com.motorola.mmsp.activitygraph.activityWidget2d;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Observer;
import java.util.Observable;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

import com.motorola.mmsp.activitygraph.R;
import com.motorola.mmsp.activitygraph.activityWidget2d.GraphWidgetProvider.Item;

public class ActivityModel {

    private static String UPDATE_MANUAL_LIST = "com.motorola.mmsp.activitygraph.UPDATE_MANUAL_LIST";
    private static String UPDATE_MUSIC = "MOTOWIDGET_UPDATE_ALBUMNID";
    private static String UPDATE_SETTING_MODE = "com.motorola.mmsp.activitygraph.CHANGE_MODE";
    private static String UPDATE_SETTING_SKIN = "com.motorola.mmsp.activitygraph.CHANGE_SKIN";
    private static String ADD_APP_PACKAGE = "com.motorola.mmsp.activitygraph.ADD_APP_PACKAGE";
    private static String REMOVE_APP_PACKAGE = "com.motorola.mmsp.activitygraph.REMOVE_APP_PACKAGE";
    private static String UPDATE_APP_PACKAGE = "com.motorola.mmsp.activitygraph.UPDATE_APP_PACKAGE";
    public static final String BROADCAST_UPDATE_HIDDEN = "com.motorola.mmsp.activitygraph.UPDATE_HIDDEN";
    public static final String BROADCAST_RESET_WIDGET = "com.motorola.mmsp.activitygraph.RESET";
    public static final String BROADCAST_OUT_OF_BOX = "com.motorola.mmsp.activitygraph.OUT_OF_BOX";
    private static String ACTION_WIDGET_RELEASE = "com.motorola.mmsp.home.ACTION_WIDGET_RELEASE";
    private static String ACTION_WIDGET_TIME_OUT = "com.motorola.mmsp.motohome.ACTION_WIDGET_TIME_OUT";
    private static final String MEDIA_SCANNER_FINISHED = "android.intent.action.MEDIA_SCANNER_FINISHED";
    private static final String UPDATE_EXTERNAL_APPS = "com.motorola.mmsp.activitygraph.UPDATE_EXTERNAL_APPS";
    private static final String UPDATE_APPS = "com.motorola.mmsp.activitygraph.UPDATE_APPS";
    private static final String SDCARD_EJECTED = "android.intent.action.MEDIA_EJECT";
    private static final String PREFERENCE_NAME = "widgetPreferences";
    private static final int NO_VISUAL_REPRESENTATION = 0xffff;
    /*2012-08-21 added by amt_chenjing for Scaled app's icon*/
    public static final int DEFAULT_ICON_SIZE = 72;
    /*2012-08-21 add end*/
    private static String[] mComponents = {
            "com.android.browser/.BrowserActivity",
            "com.android.email/.activity.Welcome",
            "com.android.mms/.ui.traditional.MessageLaunchActivity",
            "com.android.mms/.ui.ConversationList",
            "com.android.contacts/.DialtactsActivity",
            "com.android.contacts/.DialtactsContactsEntryActivity",
            "com.android.deskclock/.DeskClock",
            // "com.motorola.vissage/.VissageActivity",
            // "com.motorola.sdcardbackuprestore/.MainUI",
            "com.android.settings/.Settings", "com.android.camera/.Camera",
            "com.android.calendar/.LaunchActivity",
            "com.android.quicksearchbox/.SearchActivity",
            "com.fihtdc.filemanager/.FileBrowser",
            "com.motorola.mmsp.taskmanager/.TaskManagerActivity",
            "com.android.providers.downloads.ui/.DownloadList",
            "com.android.calculator2/.Calculator",
            "com.android.voicedialer/.VoiceDialerActivity",
            "com.fihtdc.fmradio/.FMRadio",
            "com.broadcom.bt.app.fm/.rx.FmRadio",
            "com.android.music/.VideoBrowserActivity" };

    private static String[] mVisualRepesIds = {
            "com.motorola.mmsp.activitygraph:drawable/browser",
            "com.motorola.mmsp.activitygraph:drawable/email",
            "com.motorola.mmsp.activitygraph:drawable/message",
            "com.motorola.mmsp.activitygraph:drawable/message",
            "com.motorola.mmsp.activitygraph:drawable/phone",
            "com.motorola.mmsp.activitygraph:drawable/contacts",
            "com.motorola.mmsp.activitygraph:drawable/clock",
            // "com.motorola.mmsp.activitygraph:drawable/vissage",
            // "com.motorola.mmsp.activitygraph:drawable/sdcardbackuprestore",
            "com.motorola.mmsp.activitygraph:drawable/settings",
            "com.motorola.mmsp.activitygraph:drawable/camera",
            "com.motorola.mmsp.activitygraph:drawable/calendar",
            "com.motorola.mmsp.activitygraph:drawable/search",
            "com.motorola.mmsp.activitygraph:drawable/filemanager",
            "com.motorola.mmsp.activitygraph:drawable/taskmanager",
            "com.motorola.mmsp.activitygraph:drawable/downloads",
            "com.motorola.mmsp.activitygraph:drawable/calculator",
            "com.motorola.mmsp.activitygraph:drawable/voicedialer",
            "com.motorola.mmsp.activitygraph:drawable/fmradio",
            "com.motorola.mmsp.activitygraph:drawable/fmradio",
            "com.motorola.mmsp.activitygraph:drawable/videoplayer" };

    private static Uri artUri = Uri
            .parse("content://media/external/audio/albumart");
    private static Uri alarmUri = Uri
            .parse("content://com.android.deskclock/alarm");
    private static Uri appUri = Uri.parse("content://com.motorola.appgraphy");
    private static Uri rankUri = Uri.withAppendedPath(appUri, "ranks");
    private static String ALARMAPP = "com.android.deskclock/.DeskClock";
    private static String ALARMAPP_S = "com.android.deskclock/.AlarmClock";
    private static String MUSICAPP = "com.android.music/.MusicBrowserActivity";
    /*2012-12-07,Modifyed for android 4.1.1*/
    /*2012-11-19, Modifyed for SWITCHUITWO-30*/
    private static String GALLERYAPP = "com.android.gallery3d/.app.Gallery";
    private static String GALLERYAPPALIAS = "com.google.android.gallery3d/com.android.gallery3d.app.Gallery";
    /*2012-11-19, Modify end*/
    /*2012-12-07,Modify end*/
    private static Uri updateUri = Uri.withAppendedPath(appUri, "set_all_rank");
    private static Uri manualUri = Uri
            .withAppendedPath(appUri, "manual_config");

    private static String ContactApps = "com.android.contacts";
    private static String ContactApp = "com.android.contacts/.DialtactsActivity";
    private static String DialApp = "com.android.contacts/.DialtactsContactsEntryActivity";
    private static String CallLogApp_SilverSmart = "com.android.contacts/.DialtactsCallLogEntryActivity";
    // private static int[] AutoRankIndex = { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
    private static boolean sdcardPicDetected = false;
    private static String sdcardPicPath = null;

    private int mMode;
    private int mSkin;
    private Context mGraphContext = null;
    private static ArrayList<String> RankListTemp = new ArrayList<String>();
    private static HashMap<String, String> nameMap = new HashMap<String, String>();
    private static HashMap<String, Drawable> iconMap = new HashMap<String, Drawable>();

    private static AlarmObserver alarmob;
    private static Cursor alarmCursor;
    private static ContentQueryMap mContentQueryMap;
    private boolean bGallery = false;
    private boolean bMusic = false;
    private boolean bAlarm = false;
    private String mAlarmTime = "";
    private Bitmap mPicture = null;
    private Bitmap mAlbum = null;
    private static long mAlbumId = -1;
    private static long mSongId = -1;

    private Context mContext;
    private static final String TAG = "ActivityModel";
    public static int MAXCARD = 9;
    public static int MAXCARDWIDTH = 80;
    public static int CARDSIZE = 179;
    public int mOutOfBoxed = 0;
    private ContentResolver mResolver = null;
    private ActivityProvider myProvider;

    // private ArrayList<String> mOldlist = null;

    public static final int HANDLE_UPDATE_SPECIAL_APP = 0;
    public static final int HANDLE_UPDATE_MANUAL_LIST = 1;
    public static final int HANDLE_UPDATE_OUT_OF_BOX = 2;
    /*2012-11-30, Added by amt_chenjing for switchuitwo-173*/
    public static final int HANDLE_CHANGE_SKIN =3;
    /*2012-11-30, Added end*/
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            Log.i(TAG, "---handler-----update widget---- what = " + msg.what);

            switch (msg.what) {
            case HANDLE_UPDATE_SPECIAL_APP:
            case HANDLE_UPDATE_MANUAL_LIST:
            	/*2012-11-30, Modifyed by amt_chenjing for switchuitwo-173*/
            case HANDLE_CHANGE_SKIN:
            	/*2012-11-30, Modify end*/
                getProvider().updateWidget();
                break;
            case HANDLE_UPDATE_OUT_OF_BOX:
                // mOutOfBoxed = Settings.System.getInt(mResolver, "box", 0);
                // mMode = getMode();
                if (mOutOfBoxed == 0) {
                    getProvider().isShowPlayButton(true);
                } else {
                    getProvider().isShowPlayButton(false);
                }
                getProvider().updateWidget();
                break;
            }
        }
    };

    private static String[] mDefaultIconIds = {
            "com.motorola.mmsp.activitygraph:drawable/default1",
            "com.motorola.mmsp.activitygraph:drawable/default2",
            "com.motorola.mmsp.activitygraph:drawable/default3",
            "com.motorola.mmsp.activitygraph:drawable/default4",
            "com.motorola.mmsp.activitygraph:drawable/default5",
            "com.motorola.mmsp.activitygraph:drawable/default6",
            "com.motorola.mmsp.activitygraph:drawable/default7",
            "com.motorola.mmsp.activitygraph:drawable/default8",
            "com.motorola.mmsp.activitygraph:drawable/default9" };

    private final ContentObserver rankobserver = new ContentObserver(
            new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "rank database change");
            if (mMode == 0) {
                checkRankList();

            }
        }
    };
    // private final ContentObserver manualobserver = new ContentObserver(
    // new Handler()) {
    // @Override
    // public void onChange(boolean selfChange) {
    // Log.d(TAG, "manual database change");
    // if (mMode == 1) {
    // // checkRankList();
    // getProvider().updateWidget();
    // }
    // }
    // };

    private final ContentObserver imgobserver = new ContentObserver(
            new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "imgobserver onChange()bGallery=" + bGallery);
            Intent intent = new Intent(
                    "com.motorola.mmsp.activitygraph.IMAGE_DATA_CHANGED");
            mContext.sendBroadcast(intent);
            if (bGallery) {
                Message mMsg = new Message();
                mMsg.what = HANDLE_UPDATE_SPECIAL_APP;
                mHandler.removeMessages(HANDLE_UPDATE_SPECIAL_APP);
                mHandler.sendMessageDelayed(mMsg, 3000);
            }
        }
    };

    private final ContentObserver mediaobserver = new ContentObserver(
            new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "mediaobserver onChange() bMusic=" + bMusic);
            if (!bMusic) {
                return;
            } else {
                // getProvider().updateWidget();
                Message mMsg = new Message();
                mMsg.what = HANDLE_UPDATE_SPECIAL_APP;
                mHandler.removeMessages(HANDLE_UPDATE_SPECIAL_APP);
                mHandler.sendMessageDelayed(mMsg, 3000);

            }
        }
    };

    private final class AlarmObserver implements Observer {
        public void update(Observable o, Object arg) {
            Log.d(TAG, "next alarm changed");
            if (!bAlarm) {
                return;
            } else {
                // getProvider().updateWidget();
                Message mMsg = new Message();
                mMsg.what = HANDLE_UPDATE_SPECIAL_APP;
                mHandler.removeMessages(HANDLE_UPDATE_SPECIAL_APP);
                mHandler.sendMessageDelayed(mMsg, 3000);

            }
        }

    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            Log.d(TAG, "receive intent:" + intent);

            String action = intent.getAction();
            if (UPDATE_MANUAL_LIST.equals(action)) {
                if (mMode == 1) {
                    mHandler.sendEmptyMessage(HANDLE_UPDATE_MANUAL_LIST);
                }
            } else if (UPDATE_MUSIC.equals(action)) {
                if (!bMusic) {
                    return;
                } else {
                    mAlbumId = intent.getLongExtra("albumnId", 0);
                    mSongId = intent.getLongExtra("audioId", 0);
                    Log.d(TAG, "get music extra, album:" + mAlbumId + ", song:"
                            + mSongId);
                    getProvider().updateWidget();
                }
            } else if (UPDATE_SETTING_MODE.equals(action)) {
                int mode = intent.getIntExtra("widgetmode", 0);
                mMode = mode;
                // updateWidgetMode(mode);
                // updateWidgetMode();
                getProvider().updateWidget();
            } else if (UPDATE_SETTING_SKIN.equals(action)) {
                int skin = intent.getIntExtra("widgetskin", 0);
                // updateWidgetSkin(skin);
                /*2012-11-30, Modifyed by amt_chenjing for switchuitwo-173*/
                //getProvider().updateWidget();
                mHandler.removeMessages(HANDLE_CHANGE_SKIN);
                mHandler.sendEmptyMessageDelayed(HANDLE_CHANGE_SKIN, 1100);
                /*2012-11-30, Modify end*/
            } else if (UPDATE_EXTERNAL_APPS.equals(action)
                    || (UPDATE_APPS.equals(action))) {
                if (mMode == 0) {
                    checkRankList();
                    // RankListTemp = getRankList();
                }
                if (mMode == 1) {
                    checkRankList();
                }
            } else if (ACTION_WIDGET_RELEASE.equals(action)) {
                // Log.d(TAG, "long press widget");
                // int widgetId = intent.getIntExtra("EXTRA_WIDGET_ID", 0);
                // int bShow = intent.getIntExtra("EXTRA_BUTTON_SHOW", 0);
                // if(mOutOfBoxed ==1){
                // if (bShow == 1) {
                // if(isOurWidget(context, widgetId))
                // {
                // Log.d(TAG, "+__+++show button");
                // getProvider().showSettingButton(context,widgetId,true);
                // }
                // } else {
                // Log.d(TAG, "+__+++hide button");
                // getProvider().showSettingButton(context, new
                // ComponentName(context,ActivityProvider.class),false);
                // }
                // }
                // getProvider().updateWidget();
            } else if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
                Log.d(TAG, "++LOCAL lan change");
                setAppResolveInfo();
                getProvider().updateWidget();
            } else if (ACTION_WIDGET_TIME_OUT.equals(action)) {
                // hide setting button
                Log.d(TAG, "hide setting button");
            } else if (ADD_APP_PACKAGE.equals(action)) {
                String packageName = intent.getStringExtra("package name");
                Log.d(TAG, "add app package:" + packageName);
                addAppLabelAndIconInfo(packageName);
                getProvider().updateWidget();
            } else if (REMOVE_APP_PACKAGE.equals(action)) {
                String compName = intent.getStringExtra("component name");
                Log.d(TAG, "remove app package:" + compName);
                removeAppLabelAndIconInfo(compName);
                getProvider().updateWidget();
            } else if (UPDATE_APP_PACKAGE.equals(action)) {
                String packageName = intent.getStringExtra("package name");
                Log.d(TAG, "update app package:" + packageName);
                updateAppLabelAndIconInfo(packageName);
                getProvider().updateWidget();
            } else if (BROADCAST_UPDATE_HIDDEN.equals(action)) {
                Log.d(TAG, "hidden app intent");
                if (mMode == 0) {
                    checkRankList();
                    // RankListTemp = getRankList();
                }
            } else if (BROADCAST_OUT_OF_BOX.equals(action)) {
                Log.d(TAG, "out of box intent");
                mOutOfBoxed = Settings.System.getInt(mResolver, "box", 0);
                mMode = getMode();
                mHandler.sendEmptyMessageDelayed(HANDLE_UPDATE_OUT_OF_BOX, 1500);
                // mOutOfBoxed = Settings.System.getInt(mResolver, "box", 0);
                // // Added by amt_chenjing for SWITCHUI-2076 20120712 begin
                // mMode = getMode();
                // // Added by amt_chenjing for SWITCHUI-2076 20120712 end
                // if (mOutOfBoxed == 0) {
                // getProvider().isShowPlayButton(true);
                // } else {
                // getProvider().isShowPlayButton(false);
                // }
                // getProvider().updateWidget();
            } else if (BROADCAST_RESET_WIDGET.equals(action)) {
                Log.d(TAG, "reset widget intent");
                resetState();
                getProvider().updateWidget();
            } else if (MEDIA_SCANNER_FINISHED.equals(action)) {
                Uri uri = intent.getData();
                String path = uri.getPath();
                Log.d(TAG, "sd scan path:" + path);
                String externalStoragePath = Environment
                        .getExternalStorageDirectory().getPath();
                if (path.equals(externalStoragePath)) {
                    if (bMusic) {
                        Log.d(TAG, "bMusic = TRUE");
                        getProvider().updateWidget();
                    }
                }
            }
        }

    };

    public ActivityModel(Context context, ActivityProvider provider) {
        myProvider = provider;
        Log.i(TAG, "+Model create");
        mContext = context.getApplicationContext();
        mResolver = mContext.getContentResolver();
        mOutOfBoxed = Settings.System.getInt(mResolver, "box", 0);

        mMode = getMode();
        mSkin = getSkin();
        setAppResolveInfo();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_MANUAL_LIST);
        filter.addAction(UPDATE_MUSIC);
        filter.addAction(ADD_APP_PACKAGE);
        filter.addAction(UPDATE_APP_PACKAGE);
        filter.addAction(REMOVE_APP_PACKAGE);
        filter.addAction(UPDATE_SETTING_MODE);
        filter.addAction(UPDATE_SETTING_SKIN);
        filter.addAction(ACTION_WIDGET_RELEASE);
        filter.addAction(BROADCAST_UPDATE_HIDDEN);
        filter.addAction(BROADCAST_RESET_WIDGET);
        filter.addAction(BROADCAST_OUT_OF_BOX);
        filter.addAction(UPDATE_EXTERNAL_APPS);
        filter.addAction(UPDATE_APPS);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        mContext.registerReceiver(mReceiver, filter);

        IntentFilter filterSd = new IntentFilter();
        filterSd.addAction(MEDIA_SCANNER_FINISHED);
        filterSd.addDataScheme("file");
        mContext.registerReceiver(mReceiver, filterSd);

        ContentResolver resolver = context.getContentResolver();
        resolver.registerContentObserver(updateUri, true, rankobserver);

        // resolver = context.getContentResolver();
        // resolver.registerContentObserver(manualUri, true, manualobserver);

        resolver = context.getContentResolver();
        resolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, imgobserver);
        resolver = context.getContentResolver();
        resolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true,
                mediaobserver);

        alarmCursor = context.getContentResolver().query(
                Settings.System.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?)",
                new String[] { Settings.System.NEXT_ALARM_FORMATTED }, null);
        mContentQueryMap = new ContentQueryMap(alarmCursor,
                Settings.System.NAME, true, null);
        alarmob = new AlarmObserver();
        mContentQueryMap.addObserver(alarmob);
        Log.d(TAG, "RankListTemp init");
        RankListTemp = getRankList();

    }

    public void setAppResolveInfo() {
        Log.d(TAG, "set app resolveInfo");
        nameMap.clear();
        iconMap.clear();

        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);

        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> apps = null;
        apps = pm.queryIntentActivities(main, 0);
        if (apps == null || apps.size() == 0) {
            Log.d(TAG, "apps is null or size is 0");
            return;
        }
        int num = apps.size();
        for (int i = 0; i < num; i++) {
            ResolveInfo item = apps.get(i);
            ComponentName name = new ComponentName(
                    item.activityInfo.applicationInfo.packageName,
                    item.activityInfo.name);
            String appname = name.flattenToShortString();
            String label = item.loadLabel(pm).toString();
            Drawable icon = item.loadIcon(pm);

            nameMap.put(appname, label);
            iconMap.put(appname, icon);
        }
    }

    public ArrayList<String> getRankList() {
        Log.d(TAG, "get rank list");
        ArrayList<String> list = new ArrayList<String>();
        Cursor c = null;

        try {
            c = mResolver.query(rankUri, null, "day" + "=?" + " and "
                    + "current_rank" + "!=?", new String[] { "-1", "-1" },
                    "current_rank DESC");
            Log.d(TAG, "query rank data");
            if (c != null && c.getCount() > 0) {
                Log.d(TAG, "count > 0");
                c.moveToFirst();
                while (!c.isAfterLast()) {
                    String name = c.getString(c.getColumnIndex("appName"));
                    boolean bHidden = c.getInt(c.getColumnIndex("hidden")) == 0 ? false
                            : true;
                    Log.d(TAG, "app name:" + name + ", hidden:" + bHidden);

                    if (name != null && !checkHiddenApp(name) && !bHidden
                            && getNameFromApp(name) != null) {
                        list.add(name);
                    }
                    if (list.size() >= MAXCARD) {
                        break;
                    }
                    c.moveToNext();
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "get rank error");
            e.printStackTrace();
        } finally {
            Log.d(TAG, "finally");
            if (c != null) {
                c.close();
                c = null;
            }
        }
        return list;
    }

    private boolean checkHiddenApp(String name) {
        // Log.d(TAG, "check hidden app");
        //
        // int ind = name.indexOf('/');
        // if (ind > 1) {
        // String newname = name.substring(0, ind);
        // if (newname.equals(ContactApps)) {
        // return true;
        // }
        // }
        return false;
    }

    public String getNameFromApp(String name) {
        if (name == null) {
            return null;
        }
        Log.d(TAG, "get name:" + name + " from app");

        String appName = nameMap.get(name);
        return appName;
    }

    private Bitmap getBitmapFromApp(String name) {
        Log.d(TAG, "get Bitmap from app, name:" + name);
        Drawable icon = null;
        if (name == null) {
            Log.d(TAG, "name or bmp is null");
            return null;
        }
        icon = iconMap.get(name);
        if (icon == null) {
            Log.d(TAG, "icon map can't get drawable by name");
            return null;
        }
        Bitmap first = ((BitmapDrawable) icon).getBitmap();
        // Bitmap iconBitmap =
        // Bitmap.createScaledBitmap(first,MAXCARDWIDTH,MAXCARDWIDTH, true);
        Log.d("ActivityG", "first.getWidth():" + first.getWidth() + "first.getHeight()" + first.getHeight());
		/*2012-08-21 Modifyed by amt_chenjing for  app's icon size*/        
        Bitmap iconBitmap = null;
        /*2012-11-14, modifyed by amt_chenjing for SWITCHUI-3062 for upgrating to JB*/
		if (first.getHeight() > DEFAULT_ICON_SIZE || first.getWidth() > DEFAULT_ICON_SIZE) {
			iconBitmap = Bitmap.createScaledBitmap(first.copy(Config.ARGB_8888, true), DEFAULT_ICON_SIZE,
					DEFAULT_ICON_SIZE, true);
		} else {
			iconBitmap = Bitmap.createScaledBitmap(first.copy(Config.ARGB_8888, true), first.getHeight(),
					first.getWidth(), true);
		}
		/*2012-11-14, modify end*/
		/*2012-08-21 Modifyed end*/
        return iconBitmap;
    }

    public int getOutBox() {
        return mOutOfBoxed;
    }

    private int getSkin() {
        int skin = 0;
        Context context = getGraphContext();
        if (context == null) {
            return skin;
        }
        SharedPreferences shared = context.getSharedPreferences(
                "graphyPreferences", Context.MODE_WORLD_READABLE
                        + Context.MODE_WORLD_WRITEABLE
                        + Context.MODE_MULTI_PROCESS);
        skin = shared.getInt("Skin", 0);
        Log.d(TAG, "get skin mode:" + skin);
        return skin;
    }

    private int getMode() {
        int mode = 0;
        Context context = getGraphContext();
        if (context == null) {
            return mode;
        }
        SharedPreferences shared = context.getSharedPreferences(
                "graphyPreferences", Context.MODE_WORLD_READABLE
                        + Context.MODE_WORLD_WRITEABLE
                        + Context.MODE_MULTI_PROCESS);
        mode = shared.getInt("Mode", 0);
        Log.d(TAG, "get origin mode:" + mode);
        return mode;
    }

    public Context getGraphContext() {
        if (mGraphContext == null) {
            try {
                mGraphContext = mContext.createPackageContext(
                        "com.motorola.mmsp.activitygraph", 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mGraphContext;
    }

    public Bitmap getSkinBitmap(int index) {
        int bmId = getSkinBmp(2 * index + 1);
        if (bmId != 0) {
            return loadSkinBgFromGraph(bmId);
        } else {
            //return BitmapFactory.decodeResource(mContext.getResources(),
            //        R.drawable.bg_image_big);
            return BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.class_bg_image_big);
        }
    }

    public Bitmap getTextBarBitmap(int index) {
        int bmId = getSkinBmp(2 * index);
        if (bmId != 0) {
            return loadSkinBgFromGraph(bmId);
        } else {
            //return BitmapFactory.decodeResource(mContext.getResources(),
            //        R.drawable.bar_image_big);
            return BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.class_bar_image_big);
        }
    }

    public ArrayList<Item> getList() {
        resetState();
        if (mMode == 0) {
            return getAutoList();
        } else {
            return getManualList();
        }
    }

    public ArrayList<Item> getAutoList() {
        long start = System.currentTimeMillis();
        ArrayList<Item> tempList = new ArrayList<Item>();
        RankListTemp = getRankList();
        int size = RankListTemp.size();
        int sum = Math.min(size, MAXCARD);
        Log.i(TAG, "++sum=" + sum + "+size=" + size);
        for (int i = 0; i < sum; i++) {
            Item item = new Item();
            if (checkIsSpecialApp(RankListTemp.get(i))) {
                // item = getSpecialItem(RankListTemp.get(i), AutoRankIndex[i]);
                item = getSpecialItem(RankListTemp.get(i), i);
            } else {
                // item.position = AutoRankIndex[i];
                item.position = i;
                item.skin = getSkinBitmap(item.position);
                item.cover = getIconFromGraph(RankListTemp.get(i));
                item.AppName = RankListTemp.get(i);
                item.text = getNameFromApp(RankListTemp.get(i));
                item.textbar = getTextBarBitmap(item.position);
                item.intentStr = RankListTemp.get(i);
                Log.i(TAG, "+name=" + item.intentStr);
                Log.i(TAG,
                        "comname++="
                                + ComponentName
                                        .unflattenFromString(item.intentStr));

            }
            tempList.add(item);
        }
        if (sum < MAXCARD) {
            Context context = getGraphContext();
            Resources r = context.getResources();
            for (int i = sum; i < MAXCARD; i++) {
                Item item = new Item();
                item.AppName = "";
                // item.position = AutoRankIndex[i];
                item.position = i;
                item.skin = getSkinBitmap(item.position);
                int id = r.getIdentifier(mDefaultIconIds[item.position], null,
                        null);
                item.cover = BitmapFactory.decodeResource(r, id);
                item.intentStr = "";
                tempList.add(item);
            }
        }
        long end = System.currentTimeMillis();
        Log.i(TAG, "getAutoList-----cost time : " + (end - start));
        return tempList;
    }

    public ArrayList<Item> getManualList() {
        long start = System.currentTimeMillis();
        ArrayList<Item> tempList = new ArrayList<Item>();
        Cursor c = null;
        try {
            c = mResolver
                    .query(manualUri, null, null, null, "manual_index ASC");
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                while (!c.isAfterLast()) {
                    String name = c.getString(c.getColumnIndex("apps_name"));
                    int index = c.getInt(c.getColumnIndex("manual_index"));
                    Log.d(TAG, "app name:" + name + ", index:" + index);

                    if ((name != null) && (getNameFromApp(name) != null)) {
                        Item item = new Item();
                        if (checkIsSpecialApp(name)) {
                            item = getSpecialItem(name, index);
                        } else {
                            item.position = index;
                            item.skin = getSkinBitmap(item.position);
                            item.cover = getIconFromGraph(name);
                            item.AppName = name;
                            item.text = getNameFromApp(name);
                            item.textbar = getTextBarBitmap(item.position);
                            item.intentStr = name;
                        }

                        tempList.add(item);
                    }
                    c.moveToNext();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }

        Log.i(TAG, "++temp size=" + tempList.size());
        if (tempList.size() != MAXCARD) {
            Context context = getGraphContext();
            Resources r = context.getResources();
            for (int i = 0; i < MAXCARD; i++) {
                boolean isHave = false;
                for (int j = 0; j < tempList.size(); j++) {
                    Log.i(TAG, "++++j=" + j);
                    if (tempList.get(j).position == i) {
                        isHave = true;
                        break;
                    }
                }
                if (!isHave) {
                    int id = r.getIdentifier(mDefaultIconIds[i], null, null);
                    Item item = new Item();
                    item.position = i;
                    item.skin = getSkinBitmap(item.position);
                    item.AppName = "";
                    item.cover = BitmapFactory.decodeResource(r, id);
                    item.intentStr = "com.motorola.mmsp.activitygraph/.ManageShortcuts";
                    tempList.add(item);

                }
            }
        }

        long end = System.currentTimeMillis();
        Log.i(TAG, "getManualList-----cost time : " + (end - start));
        return tempList;
    }

    public ActivityProvider getProvider() {
        // return ActivityApplication.getApplicationInstance().getProvider();
        return myProvider;
    }

    public void updateWidgetMode() {
        mMode = getMode();
        getProvider().updateWidget();
    }

    public void checkRankList() {
        Log.i(TAG, "checkrankList");
        ArrayList<String> mOldlist = RankListTemp;
        ArrayList<String> newlist = getRankList();
        if (mOldlist == null || mOldlist.size() == 0) {
            Log.i(TAG, "checkrankList---01");
            mOldlist = newlist;
         // Modifyed by amt_chenjing 20120711 for SWITCHUI-2033 begin
            //updateCards(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 });
            Runnable runable = new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					updateCards(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 });
				}
			};
			runable.run();
         // Modifyed by amt_chenjing 20120711 for SWITCHUI-2033 end
            return;
        }

        int newSize = newlist.size();
        int oldSize = mOldlist.size();

        Log.i(TAG, "checkrankList---old size = " + oldSize);
        Log.i(TAG, "checkrankList---new size = " + newSize);

        if (newSize < oldSize) {
            Log.i(TAG, "checkrankList---02");
            mOldlist = newlist;
            updateCards(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 });
            return;
        } else if (newSize >= oldSize) {
            ArrayList<Integer> indexes = new ArrayList<Integer>();

            for (int i = 0; i < oldSize; i++) {
                if (!(newlist.get(i)).equals(mOldlist.get(i))) {
                    indexes.add(i);
                }
            }

            for (int j = oldSize; j < newSize; j++) {
                indexes.add(j);
            }

            int[] positions = new int[indexes.size()];
            for (int i = 0; i < indexes.size(); i++) {
                positions[i] = indexes.get(i);
            }

            Log.i(TAG, "checkrankList ---03---- " + positions.length);
            mOldlist = newlist;
            updateCards(positions);
            return;
        }
        // getProvider().updateWidget();
    }

    private void updateCards(int[] positions) {
        if (positions == null || positions.length == 0) {
            return;
        }
        getProvider().updateSpecificItems(positions);
    }

    private void addAppLabelAndIconInfo(String packageName) {

        final PackageManager pm = mContext.getPackageManager();
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.setPackage(packageName);
        final List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

        if (apps != null) {
            if (apps.size() > 0) {
                for (int i = 0; i < apps.size(); i++) {
                    ResolveInfo item = apps.get(i);
                    ComponentName name = new ComponentName(
                            item.activityInfo.applicationInfo.packageName,
                            item.activityInfo.name);
                    String comp = name.flattenToShortString();
                    String label = item.loadLabel(pm).toString();
                    Drawable icon = item.loadIcon(pm);
                    Log.d(TAG, "add App Label And Icon Info:" + comp
                            + ", label:" + label);
                    Log.d(TAG, "icon:" + icon);
                    nameMap.put(comp, label);
                    iconMap.put(comp, icon);
                }
            }
        }
    }

    private void removeAppLabelAndIconInfo(String componentName) {

        if (componentName != null) {
            Log.d(TAG, "remove App label and icon Info:" + componentName);
            nameMap.remove(componentName);
            iconMap.remove(componentName);
        }
        checkRankList();

        // if (mMode == 0) {
        // if (getCardPos(componentName)!= NOT_FOUND){
        // checkRankList();
        // RankListTemp = getRankList();
        // }
        // }else {
        // for (int i=0; i< CardNamesManual.length; i++){
        // if (CardNamesManual[i].equals(componentName)) {
        // getManualNames();
        // setCardAction(CardOperation.UPDATEALL, 0, 0, null);
        // break;
        // }
        // }
        //
        // }
    }

    private void updateAppLabelAndIconInfo(String packageName) {
        final PackageManager pm = mContext.getPackageManager();
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.setPackage(packageName);
        final List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

        if ((apps != null) && (apps.size() > 0)) {
            for (int i = 0; i < apps.size(); i++) {
                ResolveInfo item = apps.get(i);
                ComponentName name = new ComponentName(
                        item.activityInfo.applicationInfo.packageName,
                        item.activityInfo.name);
                String comp = name.flattenToShortString();
                String label = item.loadLabel(pm).toString();
                Drawable icon = item.loadIcon(pm);
                Log.d(TAG, "update App Label And Icon Info:" + comp
                        + ", label:" + label);
                nameMap.put(comp, label);
                iconMap.put(comp, icon);
            }
        } else {
            ArrayList<String> removed = new ArrayList<String>();
            for (String comp : nameMap.keySet()) {
                int ind = comp.indexOf('/');
                if (ind > 1) {
                    String newname = comp.substring(0, ind);
                    if (newname.equals(packageName)) {
                        removed.add(comp);
                    }
                }
            }
            for (int i = 0; i < removed.size(); i++) {
                nameMap.remove(removed.get(i));
                iconMap.remove(removed.get(i));
            }

        }
    }

    private int getSkinBmp(int index) {
        int id = 0;
        Context context = getGraphContext();
        if (context == null) {
            return id;
        }
        SharedPreferences shared = context.getSharedPreferences(
                "graphyPreferences", Context.MODE_WORLD_READABLE
                        + Context.MODE_WORLD_WRITEABLE
                        + Context.MODE_MULTI_PROCESS);
        id = shared.getInt("bm" + String.valueOf(index), 0);
        return id;
    }

    public Bitmap loadSkinBgFromGraph(int index) {
        Context context = getGraphContext();
        Resources r = null;
        if (context != null) {
            r = context.getResources();
            return loadIconByIndex(index, r);
        }

        return null;
    }

    private Bitmap loadIconByIndex(int id, Resources r) {
        return BitmapFactory.decodeResource(r, id);
    }

    public boolean checkIsSpecialApp(String appname) {
    	/*2012-12-07,Modifyed for android 4.1.1*/
    	Log.d(TAG, "checkIsSpecialApp appname=" + appname);
        return appname.equals(ALARMAPP) || appname.equals(GALLERYAPP) || appname.equals(GALLERYAPPALIAS)
                || appname.equals(MUSICAPP) || appname.equals(ALARMAPP_S);
        /*2012-12-07,Modify end*/
    }

    public Item getSpecialItem(String appname, int position) {
        Item item = new Item();
        if (appname.equals(ALARMAPP) || appname.equals(ALARMAPP_S)) {
            Log.i(TAG, "+++++alarm");
            bAlarm = true;
            getAlarmTime();
            item.position = position;
            item.skin = getSkinBitmap(item.position);
            item.cover = getIconFromGraph(appname);
            item.AppName = appname;
            if (!mAlarmTime.equals("")) {
                item.text = mAlarmTime;
            } else {
                item.text = getNameFromApp(appname);
            }
            item.textbar = getTextBarBitmap(item.position);
            item.intentStr = appname;
        } else if (appname.equals(GALLERYAPP)) {
            Log.i(TAG, "+++++galley");
            bGallery = true;
            if (mPicture == null) {
                loadPhoto();
            }
            item.position = position;
            item.skin = getSkinBitmap(item.position);
            if ((mPicture != null) && (!mPicture.isRecycled())) {
            	//2012-09-05 Modifyed by amt_chenjing begin
                float w = mPicture.getHeight();
                float h = mPicture.getWidth();
            	float ratio = w / h;
            	int width =  (int)(CARDSIZE * ratio);
            	Log.d("LogTag", "ratio : " + ratio + " CARDSIZE * ratio" + width);
            	if(width <= 0){
            		width = CARDSIZE;
            	}
            	item.cover = getScaledBmp(mPicture, CARDSIZE, width);
            	//2012-09-05 Modify end
            } else {
                // Context context = getGraphContext();
                // Resources r = null;
                // int photoId = 0;
                // if (context != null) {
                // r = context.getResources();
                // }
                // if (r != null) {
                // photoId =
                // r.getIdentifier("com.motorola.mmsp.activitygraph:drawable/gallery",
                // null, null);
                // item.cover = loadIconByIndex(photoId, r);
                // }
                item.cover = getIconFromGraph(appname);
            }
            item.AppName = appname;
            item.text = getNameFromApp(appname);
            item.textbar = getTextBarBitmap(item.position);
            item.intentStr = appname;
        } else {
            Log.i(TAG, "+++++music");
            bMusic = true;
            item.position = position;
            item.skin = getSkinBitmap(item.position);
            if (checkSDCard()) {
                getAlbumIdFromGraph();
                mAlbum = getMusicAlbum(mContext, mAlbumId, mSongId);
                if (mAlbum == null) {
                    mAlbum = getMusicRandom(mContext);
                }
                if (mAlbum != null && !mAlbum.isRecycled()) {
                    item.cover = mAlbum;
                } else {
                    item.cover = getIconFromGraph(appname);
                }
            } else {
                item.cover = getIconFromGraph(appname);
            }
            item.AppName = appname;
            item.text = getNameFromApp(appname);
            item.textbar = getTextBarBitmap(item.position);
            item.intentStr = appname;
        }
        return item;
    }

    private void getAlarmTime() {
        Log.d(TAG, "get alarmTime");
        String nextAlarm = Settings.System.getString(mResolver,
                Settings.System.NEXT_ALARM_FORMATTED);
        Log.d(TAG, "next alarm time:" + nextAlarm);
        if (nextAlarm == null || nextAlarm.length() == 0) {
            mAlarmTime = "";
        } else {
            int id = nextAlarm.indexOf(0x20);
            if (id < nextAlarm.length() && id > 0) {
                mAlarmTime = nextAlarm.substring(id + 1);
                Log.d(TAG, "get alarm time:" + mAlarmTime);
            }
        }
    }

    private void loadPhoto() {
        Log.d(TAG, "loadPhoto");
        if (mPicture != null && !mPicture.isRecycled()) {
            mPicture.recycle();
        }
        mPicture = null;
        if (checkSDCard()) {
            try {
                String path = getPathFromDB();
                if (path != null) {
                    mPicture = loadData(path);
                } else {
                    mPicture = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkSDCard() {
        String state = android.os.Environment.getExternalStorageState();
        Log.d(TAG, "checkSDCard:" + state);
        if (state != null && state.equals(android.os.Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    private static Bitmap getScaledBmp(Bitmap bmp, int w, int h) {
        Log.d(TAG, "get scaled bmp, w:" + w + ", h:" + h);
        Bitmap newBmp = Bitmap.createScaledBitmap(bmp, w, h, true);
        return newBmp;
    }

    private Bitmap getMusicRandom(Context context) {
        Bitmap bm = null;
        Cursor c = null;
        ParcelFileDescriptor pfd = null;
        Log.d(TAG, "get music by random");
        long album = 0;
        try {
            c = mResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    new String[] { "_id" }, MediaStore.Audio.Albums.ALBUM_ART
                            + "!=?", new String[] { "null" }, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                while (!c.isAfterLast()) {
                    album = c.getLong(0);
                    Log.d(TAG, "album = " + album);
                    Uri uri = ContentUris.withAppendedId(artUri, album);
                    Log.d(TAG, "uri = " + uri);
                    pfd = context.getContentResolver().openFileDescriptor(uri,
                            "r");
                    if (pfd != null) {
                        FileDescriptor fd = pfd.getFileDescriptor();
                        bm = BitmapFactory.decodeFileDescriptor(fd);
                        mAlbumId = album;
                        mSongId = -1;
                        if (bm != null)
                            return bm;
                    }
                    c.moveToNext();
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "error");
            e.printStackTrace();
        } finally {
            Log.d(TAG, "finally");
            if (c != null) {
                c.close();
                c = null;
                Log.d(TAG, "close cursor");
            }
            if (pfd != null) {
                try {
                    pfd.close();
                    pfd = null;
                    Log.d(TAG, "close pfd");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return bm;
    }

    private void getAlbumIdFromGraph() {
        Context context = getGraphContext();
        if (context == null) {
            return;
        }
        SharedPreferences shared = context.getSharedPreferences(
                "graphyPreferences", Context.MODE_WORLD_READABLE
                        + Context.MODE_WORLD_WRITEABLE
                        + Context.MODE_MULTI_PROCESS);
        mSongId = shared.getLong("audio", 0);
        mAlbumId = shared.getLong("album", 0);
        Log.d(TAG, "get origin audio:" + mSongId + ", album:" + mAlbumId);
        return;
    }

    private static Bitmap getMusicAlbum(Context context, long albumId,
            long songId) {
        Log.d(TAG, "get music album, album:" + albumId + ", songId:" + songId);
        Bitmap bm = null;
        ParcelFileDescriptor pfd = null;

        if (albumId < 0 && songId < 0) {
            throw new IllegalArgumentException(
                    "Must specify an album or a song id");
        }

        try {
            if ((albumId <= 0) && (songId > 0)) {
                Log.d(TAG, "albumID <= 0");
                Uri uri = Uri.parse("content://media/external/audio/media/"
                        + songId + "/albumart");
                pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    Log.d(TAG, "get album art");
                    FileDescriptor fd = pfd.getFileDescriptor();
                    bm = BitmapFactory.decodeFileDescriptor(fd);
                }
            } else if (albumId > 0) {
                Log.d(TAG, "albumid > 0");
                Uri uri = ContentUris.withAppendedId(artUri, albumId);
                pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    Log.d(TAG, "get album art");
                    FileDescriptor fd = pfd.getFileDescriptor();
                    bm = BitmapFactory.decodeFileDescriptor(fd);
                }
            }
          /*2012-08-22 Modifyed by amt_chenjing for Exception*/
        } /*catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (pfd != null) {
                try {
                    pfd.close();
                    Log.d(TAG, "close pfd");
                    pfd = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        /*2012-08-22 Modify end*/
        return bm;
    }

    public void resetState() {
     // Modifyed by amt_chenjing for SWITCHUI-2076 20120712 begin
        mMode = getMode();
     // Modifyed by amt_chenjing for SWITCHUI-2076 20120712 end
        bAlarm = false;
        bGallery = false;
        bMusic = false;
        mAlarmTime = "";
        if (mPicture != null && !mPicture.isRecycled()) {
            mPicture.recycle();
        }
        mPicture = null;
        if (mAlbum != null && !mAlbum.isRecycled()) {
            mAlbum.recycle();
        }
        mAlbum = null;

    }

    private Bitmap getIconFromGraph(String appname) {
        // int mChecked = checkVisualRepresentation(appname);
        // if (mChecked!=NO_VISUAL_REPRESENTATION){
        // Context context = getGraphContext();
        // Resources r = null;
        // if (context != null) {
        // r = context.getResources();
        // }
        // if (r != null) {
        // int photoId = r.getIdentifier(mVisualRepesIds[mChecked], null, null);
        // return loadIconByIndex(photoId, r);
        // }else
        // {
        // return getBitmapFromApp(appname);
        // }
        // } else{
        return getBitmapFromApp(appname);
        // }
    }

    private int checkVisualRepresentation(String componentName) {

        for (int i = 0; i < mComponents.length; i++) {
            if (mComponents[i].equals(componentName))
                return i;
        }
        return NO_VISUAL_REPRESENTATION;
    }

    private boolean isOurWidget(Context context, int widgetId) {
        int[] appWidgetIds = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(getWidgetComponent(context));
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            for (int item : appWidgetIds) {
                if (item == widgetId) {
                    return true;
                }
            }
        }
        return false;
    }

    private ComponentName getWidgetComponent(Context context) {
        return new ComponentName(context, ActivityProvider.class);
    }

    private String getPathFromDB() {
        Log.d(TAG, "getPathFromDB()");
        Cursor c = null;
        String path = null;
        try {
            c = mResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[] { "_data" }, "bucket_display_name=?",
                    new String[] { "Camera" }, "datetaken DESC");
            if (c != null && c.getCount() > 0) {
                Log.d(TAG, "cursor isn't null or count is >0");
                c.moveToFirst();
                if (!c.isAfterLast()) {
                    path = c.getString(0);
                    Log.d(TAG, "get path from db is:" + path);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Log.d(TAG, "finally");
            if (c != null) {
                c.close();
                c = null;
            }
        }
        // if (path != null) {
        // return path;
        // } else {
        // Log.d(TAG, "get from sdcard picture");
        // File sdcardfile = Environment.getExternalStorageDirectory();
        // readSdFileName(sdcardfile);
        // if (sdcardPicDetected) {
        // path = sdcardPicPath;
        // }
        // Log.d(TAG, "path = " + path);
        // return path;
        // }
        return path;
    }

    private void readSdFileName(File sdcardfile) {
        Log.d(TAG, "readSdFileName()");
        File[] fileList = sdcardfile.listFiles();
        if ((!sdcardPicDetected) && (fileList != null)) {
            for (int i = 0; i < fileList.length; i++) {
                if ((!sdcardPicDetected)) {
                    if (fileList[i].isDirectory()) {
                        readSdFileName(fileList[i]);
                    } else {
                        String filepath = fileList[i].getAbsolutePath();
                        if (filepath.endsWith("jpg")) { // ||filepath.endsWith("gif")||filepath.endsWith("bmp")||filepath.endsWith("png")){
                            Log.d(TAG, "sdcardPicDetected  filepath:"
                                    + filepath);
                            sdcardPicDetected = true;
                            sdcardPicPath = filepath;
                        }
                    }
                }
            }

        }
    }

    public Bitmap loadData(String filePath) {
        Log.d(TAG, "load data from filePath:" + filePath);
        if (filePath == null) {
            return null;
        }
        Bitmap thumb = null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        thumb = BitmapFactory.decodeFile(filePath, options);

        Log.d(TAG, "options.outWidth = " + options.outWidth);
        Log.d(TAG, "options.outHeight = " + options.outHeight);
      //2012-09-13 Modifyed by amt_chenjing begin
      //improve bitmap's definition
        int comp = options.outWidth / 800;
      //2012-09-13 Modify end
        if (comp <= 0) {
            comp = 1;
        }
        Log.d(TAG, "comp = " + comp);
        if ((options.outWidth > 0) && (options.outHeight > 0)) {
            int height = options.outHeight * MAXCARDWIDTH / options.outWidth;
            Log.d(TAG, "height = " + height);
            Log.d(TAG, "MAXCARDWIDTH = " + MAXCARDWIDTH);
            options.outWidth = MAXCARDWIDTH;
            options.outHeight = height;
            options.inJustDecodeBounds = false;
            options.inSampleSize = comp;
            thumb = BitmapFactory.decodeFile(filePath, options);
        }

        return thumb;
    }
}
