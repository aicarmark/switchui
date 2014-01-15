package com.motorola.mmsp.motohomex.apps;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;

import com.motorola.mmsp.motohomex.ApplicationInfo;
import com.motorola.mmsp.motohomex.AppsCustomizePagedView;
import com.motorola.mmsp.motohomex.Launcher;
import com.motorola.mmsp.motohomex.LauncherApplication;
import com.motorola.mmsp.motohomex.LauncherSettings;
import com.motorola.mmsp.motohomex.R;
import com.motorola.mmsp.motohomex.apps.AppsSchema.Groups;
/* 2012-7-15 add by Hu ShuAn for switchui-2209  start*/
import com.motorola.mmsp.motohomex.TransitionSettingActivity;
/* 2012-7-15 add by Hu ShuAn for switchui-2209  end*/

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.Spinner;
import android.widget.Toast;

public class AllAppsPage extends FrameLayout implements
    OnLongClickListener, OnItemSelectedListener, OnDismissListener,
    DialogInterface.OnKeyListener, OnClickListenerSpinner {

    static final String TAG = "AllAppsPage";

    /** SAVE_STATE_APPS_DIALOG value indicating that no dialog needs to be restored. */
    static final int DIALOG_NONE = 0;
    /** SAVE_STATE_APPS_DIALOG value indicating the SelectAppsDialog should be restored. */
    static final int DIALOG_SELECT_APPS = 1;
    /** SAVE_STATE_APPS_DIALOG value indicating the ConfirmRemoveDialog should be restored. */
    static final int DIALOG_CONFIRM_REMOVE = 2;
    /** SAVE_STATE_APPS_DIALOG value indicating the DuplicateNameDialog should be restored. */
    static final int DIALOG_DUPLICATE_NAME = 3;
    /** SAVE_STATE_APPS_DIALOG value indicating the SortOptionsDialog should be restored. */
    static final int DIALOG_SORT_OPTIONS = 4;
    /** SAVE_STATE_APPS_DIALOG value indicating the SetIconDialog should be restored. */
    static final int DIALOG_SET_ICON = 5;
    /** SAVE_STATE_APPS_DIALOG value indicating the SetIconDialog should be restored. */
    static final int DIALOG_CONFIRM_DISABLE_APP = 6;
    /** SAVE_STATE_GROUP_DIALOG value indicating the EditGroupDialog should be restored. */
    static final int DIALOG_EDIT_GROUP = 7;

    /** Indicates that user is not editing a group. */
    static final int EDIT_STATE_NONE = 0;
    /** Indicates that user is editing an existing group. */
    static final int EDIT_STATE_EDIT = 1;
    /** Indicates that user is creating a new group. */
    static final int EDIT_STATE_NEW = 2;
    /** Indicates that user is adding an app to a new group. */
    static final int EDIT_STATE_ADD_TO_NEW = 3;

    static final int GROUP_LIST_STATE_HIDE = 0;
    static final int GROUP_LIST_STATE_FILTER = 1;
    static final int GROUP_LIST_STATE_DROP = 2;

    /** SharedPreferences file name for AppsView state. */
    private static final String PREFS_FILE_STATE = "AppsView";
    /** SharedPreferences key for User Selected Group Id. */
    private static final String KEY_USER_SELECTED_GROUP_ID = "KEY_USER_SELECTED_GROUP_ID";

    /** Type: long.  Key for saving the current-selected group ID. */
    private static final String SAVE_STATE_CURRENT_GROUP = "current_group";
    /** Type: int.  Key for saving current edit state (EDIT_STATE_* constants). */
    private static final String SAVE_STATE_EDIT_STATE = "edit_state";
    /** Type: String.  Key for saving edited group name. */
    private static final String SAVE_STATE_EDIT_TEXT = "edit_text";
    /** Type: String.  Key for saving edit group name selection start. */
    private static final String SAVE_STATE_EDIT_TEXT_SEL_START = "edit_text_sel_start";
    /** Type: String.  Key for saving edit group name selection start. */
    private static final String SAVE_STATE_EDIT_TEXT_SEL_END = "edit_text_sel_end";
    /** Type: int.  Key for saving edited icon set. */
    private static final String SAVE_STATE_EDIT_ICON = "edit_icon";
    /** Type: long.  Key for saving the a. */
    private static final String SAVE_STATE_CURRENT_APP = "current_app";
    /** Type: int.  Key for saving currently running dialog (DIALOG_* constants). */
    private static final String SAVE_STATE_APPS_DIALOG = "apps_dialog";
    /** Type: String.  Key for saving DuplicateNameDialog's other name. */
    private static final String SAVE_STATE_DND_NEW_NAME = "dnd_new_name";
    /** Type: int.  Key for saving SetIconDialog's current icon. */
    static final String SAVE_STATE_SET_ICON_CURRENT = "set_icon_current";

    /** Intent action for adding shortcut to Home workspace. */
    public static final String ACTION_INSTALL_SHORTCUT =
        "com.android.launcher.action.INSTALL_SHORTCUT";
    /** Intent action for updating shortcut at Home workspace. */
    public static final String ACTION_UPDATE_SHORTCUT =
        "com.android.launcher.action.UPDATE_SHORTCUT";
    /** Intent action for adding folder to Home workspace. */
    public static final String ACTION_INSTALL_FOLDER =
        "com.android.launcher.action.INSTALL_FOLDER";
    /** Intent action for removing shortcut to Home workspace. */
    private static final String ACTION_UNINSTALL_SHORTCUT =
            "com.android.launcher.action.UNINSTALL_SHORTCUT";

    private static final String ACTION_CUSTOMIZE_ICON =
        "com.motorola.mmsp.motohomex.action.CUSTOMIZE_APP_ICON";
    private static final String ACTION_RESTORE_ICON =
        "com.motorola.mmsp.motohomex.action.RESTORE_APP_ICON";

    private static final String APPICONSERVICE = "com.motorola.appicon.manager";

    /** Indicates the currently-selected group. */
    long mCurrentGroupId; // SAVED IN STATE
    /** Indicates the currently-selected group. */
    int mEditState; // SAVED IN STATE
    /** Currently-open dialog. */
    AppsDialog mAppsDialog; // SAVED IN STATE
    /** Application behind context menu. */
    ApplicationInfo mCurrentApp; // SAVED IN STATE
    /** State waiting to be restored when apps finish loading. */
    Bundle mRestoreState;

    /** Used to launch another dialog when the current dialog is dismissed. */
    private int mNextDialogId;
    /** Non-null when editing a group. */
    GroupItem mEditGroupItem;
    /** Model manages apps groups and other data used by applications on device. */
    AppsModel mAppsModel;

    /** Currently-running dialog. */
    private Dialog mDialog;
    /** Manages a dialog to select apps in a group. */
    private SelectAppsDialog mSelectAppsDialog;
    /** Confirms the deletion of an apps group. */
    private ConfirmRemoveDialog mConfirmRemoveDialog;
    /** Confirms that applications was disabled. */
    private ConfirmDisableAppDialog mConfirmDisableAppDialog;
    /** Confirms the deletion of an apps group. */
    private DuplicateNameDialog mDuplicateNameDialog;
    /** Manages a dialog for selecting sort options. */
    private SortOptionsDialog mSortOptionsDialog;
    /** Manages a dialog for setting a group icon. */
    private SetGroupIconDialog mSetIconDialog;
    /** Manages a dialog for edit group. */
    private EditGroupDialog mEditGroupDialog;

    private GroupAdapter mGroupAdapter;
    private MotoAppsCustomizePagedView mAppsCustomizePagedView;
    private ListPopupWindow mPopupWindow;
    private BaseAdapter mGroupOptinosAdapter;
    LayoutInflater mInflater;
    private boolean mDragging;
    private float mMotionDownX;
    private float mMotionDownY;
    private DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    private View mDraggedView;
    private boolean mStartMenuOnUp;
    //private AllAppsDragController mAllAppsDragController;
    private AllAppsDropDownMenu mMoreOverFlowButton;
    private AllAppsDropDownGroup mGroupButton;
    //private ProductConfigManager mConfig;
    private boolean mAddingGroup;
    private boolean mSetGroupAdapter = true;

    private final ArrayList<Integer> mMenuDropDownOptions = new ArrayList<Integer>();
    //modified by amt_wangpeipei 2012/07/11 for switchui-2121 begin
    private static final int MENU_SEARCH_APP_WIDGET = 0;
    /* 2012-7-15 add by Hu ShuAn for switchui-2209  start*/
    private static final int MENU_TRANSITION_EFFECT = 1;
    private static final int MENU_MANAGE_APPS = 2;
    public static final int MENU_SYSTEM_SETTINGS = 3;
    private static final int MENU_ADD_TO_HOME = 4;
    private static final int MENU_ADD_RM_APPS = 5;
    private static final int MENU_EDIT_GROUP = 6;
    private static final int MENU_DELETE_GROUP = 7;
    /* 2012-7-15 add by Hu ShuAn for switchui-2209  end*/
    //modified by amt_wangpeipei 2012/07/11 for switchui-2121 end
    //added by amt_wangpeipei 2012/07/21 for switchui-2397 begin
    private Menu mMenu;
    //added by amt_wangpeipei 2012/07/21 for switchui-2397 end.

    public AllAppsPage(Context context) {
        super(context);
    }

    public AllAppsPage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AllAppsPage(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init() {
        final LauncherApplication app = (LauncherApplication) getContext().getApplicationContext();
        //mConfig = app.getConfigManager();
        mInflater = LayoutInflater.from(getContext());

        // Get the model and the adapter
        mAppsModel = app.getAppsModel();
        //CR IKCNDEVICS-2163
      	/*Added by ncqp34 at Jul-17-2012 for group switch*/
        int numbBaseOptions = MENU_SEARCH_APP_WIDGET  + 1;
	/*ended by ncqp34*/
        if(app.hasNavigationBar()||app.isScreenLarge()){
        	//modified by amt_wangpeipei 2012/07/11 for switchui-2121 begin
        	mMenuDropDownOptions.add(R.string.menu_search_app_and_widget);
        	//modified by amt_wangpeipei 2012/07/11 for switchui-2121 end.
        	/* 2012-7-15 add by Hu ShuAn for switchui-2209  start*/
        	mMenuDropDownOptions.add(R.string.transition);
        	/* 2012-7-15 add by Hu ShuAn for switchui-2209  end*/
            mMenuDropDownOptions.add(R.string.menu_manage_apps);
            mMenuDropDownOptions.add(R.string.menu_settings);
      	    /*Added by ncqp34 at Jul-17-2012 for group switch*/
            numbBaseOptions = MENU_SEARCH_APP_WIDGET + 4;
	    /*ended by ncqp34*/
        }

        /*Added by ncqp34 at Jul-17-2012 for group switch*/
        if(LauncherApplication.mGroupEnable){   
            // Drop Down Menu
            // Add the options
            mMenuDropDownOptions.add(R.string.add_to_home);
            mMenuDropDownOptions.add(R.string.add_rm_apps);
            //2012-7-15 add by Hu ShuAn for switchui-2203
            mMenuDropDownOptions.add(R.string.edit_group_name);
            mMenuDropDownOptions.add(R.string.delete_group);
      	    /*Added by ncqp34 at Jul-17-2012 for group switch*/
            numbBaseOptions = MENU_SEARCH_APP_WIDGET + 5;
	    /*ended by ncqp34*/
	}
	/*ended by ncqp34*/
        // Create the adapter
        mGroupAdapter = new GroupAdapter(getContext(), mAppsModel, GroupAdapter.ICON_LEFT);
        
        mGroupOptinosAdapter = new GroupOptionsAdapter(this, mMenuDropDownOptions, numbBaseOptions);
    }

    @Override
    protected void onAttachedToWindow() {
        ((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(mDisplayMetrics);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // Get the views
        mAppsCustomizePagedView = (MotoAppsCustomizePagedView) findViewById(R.id.apps_customize_pane_content);
        init();
    }

    public boolean onLongClick(View v) {
        /// Turn off floating menu
        //startDragMenu(v);
        //return true;

        // Just update the mCurrentApp because it is
        // used by appMenuDialog and return false
        // to use the android default behavior
        mCurrentApp = (ApplicationInfo) v.getTag();
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch(keyCode){
            case KeyEvent.KEYCODE_BACK:
                return onBackPressed();
        }
        return false;
    }

    public boolean onBackPressed(){
        /*2012-7-17, add by bvq783 for switchui-2270*/
        resetAllAppsState();
        /*2012-7-17, add end*/
        return false;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG,"onItemSelected position="+position);
        // We should not check it, but sometimes when the orientation is changed
        // it is possible that spinner call the method even when we are on
        // widget tab, so we block this kind of calling
        if (mAppsCustomizePagedView.isApplicationTab()) {
            if ((null !=parent) && (parent.getId() == R.id.tab_widget_all_apps_button)) {
                //BEGIN motorola rnj486 03/23/2012:IKCNDEVICS-3478
                if (mSetGroupAdapter){
                    mSetGroupAdapter = false;
                    if (/*All apps*/1!=getUserSelectedGroupId())
                        return;
                }
                //END motorola rnj486 03/23/2012:IKCNDEVICS-3478
                // Click on an item in the group picker
                position = mGroupAdapter.getUnfilteredPosition(position);
                if (mGroupAdapter.isAddOption(position)) {
                    // Because of the data changed, we can
                    // receive two add in a row. This "if"
                    // is to avoid adding in sequence
                    if (mAddingGroup) {
                        setGroupButton(mCurrentGroupId);
                        return;
                    }
                    // Add the group
                    startNewGroup();
                    // New group, disable the spinner
                    mAddingGroup = true;
                    setGrouButtonClickable(false);
                } else {
                    // It is not a new group, enable the spinner
                    mAddingGroup = false;
                    setGrouButtonClickable(true);
                    // Set the group
                    setCurrentGroupIndex(position);
                    mAppsCustomizePagedView.refreshPage();
                }
            }  
            //BEGIN motorola rnj486 03/13/2012:IKCNDEVICS-2736
            else if ((null !=view) && (view.getId() == R.id.all_apps_drop_down_menu_item)){
                LauncherApplication app = (LauncherApplication) getContext().getApplicationContext();
                if (!app.hasNavigationBar() &&!app.isScreenLarge()){
                    return;
                }
                	switch(position){
                	//added by amt_wangpeipei 2012/07/11 for switchui-2121 begin
					case MENU_SEARCH_APP_WIDGET:
						enterSearchAppAndWidget(app);
						break;
					//added by amt_wangpeipei 2012/07/11 for switchui-2121 end
			        /* 2012-7-15 add by Hu ShuAn for switchui-2209  start*/
					case MENU_TRANSITION_EFFECT:
						setTransition();
						break;
			        /* 2012-7-15 add by Hu ShuAn for switchui-2209  end*/
                    case MENU_MANAGE_APPS:
                        startManageApp();
                        break;
                    case MENU_SYSTEM_SETTINGS:
                         startSystemSettings();
                        break;
                    case MENU_ADD_TO_HOME:
                        addGroupToWorkspace();
                        break;
                    case MENU_ADD_RM_APPS:
                        getSelectAppsDialog().show();
                        break;
                    case MENU_EDIT_GROUP:
                        setEditState(EDIT_STATE_EDIT);
                        break;
                    case MENU_DELETE_GROUP:
                        getConfirmRemoveDialog().show();
                        break;
                }
            }
            //END motorola rnj486 03/13/2012:IKCNDEVICS-2736
        } else {
            if ((null !=view) && (view.getId() == R.id.all_apps_drop_down_menu_item)) {
                switch(position){
                	//added by amt_wangpeipei 2012/07/11 for switchui-2121 begin
                	case MENU_SEARCH_APP_WIDGET:
                		LauncherApplication app = (LauncherApplication) getContext().getApplicationContext();
                		enterSearchAppAndWidget(app);
                		break;
                	//added by amt_wangpeipei 2012/07/11 for switchui-2121 end
                    /* 2012-7-15 add by Hu ShuAn for switchui-2209  start*/
                	case MENU_TRANSITION_EFFECT:
						setTransition();
						break;
						/* 2012-7-15 add by Hu ShuAn for switchui-2209  end*/
                    case MENU_MANAGE_APPS:
                        startManageApp();
                        break;
                    case MENU_SYSTEM_SETTINGS:
                         startSystemSettings();
                        break;
                    case MENU_ADD_TO_HOME:
                        addGroupToWorkspace();
                        break;
                    case MENU_ADD_RM_APPS:
                        getSelectAppsDialog().show();
                        break;
                    case MENU_EDIT_GROUP:
                        setEditState(EDIT_STATE_EDIT);
                        break;
                    case MENU_DELETE_GROUP:
                        getConfirmRemoveDialog().show();
                        break;
                }
            }
        }
    }

    /**
     * added by amt_wangpeipei 2012/07/11 for switchui-2121
     */
    private void enterSearchAppAndWidget(LauncherApplication app) {
		Launcher launcher = app.getLauncher();
		launcher.enterSearchAppAndWidget();
	}
    
    /** 
     * 2012-7-15 add by Hu ShuAn for switchui-2209
     */
    private void setTransition(){
    	LauncherApplication app = (LauncherApplication) getContext().getApplicationContext();
    	Launcher launcher = app.getLauncher();
    	Intent intent = new Intent(launcher,TransitionSettingActivity.class);
        launcher.startActivityForResult(intent,Launcher.REQUEST_TRANSITION_EFFECT);
    }

	@Override
    public void onNothingSelected(AdapterView<?> arg0) {}

    public void setTab(Spinner spinner){
        // Set the spinner adapter and listeners
        mGroupButton = (AllAppsDropDownGroup)spinner;
	/*Added by ncqp34 at Jul-17-2012 for group switch*/
	if(LauncherApplication.mGroupEnable){
            mGroupButton.setAdapter(mGroupAdapter);
            mGroupButton.setOnItemSelectedListener(this);
            mGroupButton.setOnClickListenerSpinner(this);
	}
	/*ended by ncqp34*/
    }

    private void setGrouButtonClickable(boolean set) {
	/*Added by ncqp34 at Jul-17-2012 for group switch*/
    	Log.d("Test", "setGrouButtonClickable  Set=" + set);
	if(LauncherApplication.mGroupEnable){
            mGroupButton.setClickable(set);
            mGroupButton.setFocusable(set);
	}else{
            mGroupButton.setClickable(false);
            mGroupButton.setFocusable(false);
	}
	/*ended by ncqp34*/

    }

    public void setAppsPageItems(final boolean set) {
        //IKCNDEVICS-3718
        /*if (null != mMoreOverFlowButton) {
            mMoreOverFlowButton.setClickable(set);
            mMoreOverFlowButton.setFocusable(set);
            mMoreOverFlowButton.setVisibility(set == true ? VISIBLE : GONE);
        }*/
    	Log.d("Test", "setAppsPageItems  Set=" + set);
        setGrouButtonClickable(set);
        mGroupButton.setEnabled(set);
    }

    //IKCNDEVICS-3718
    public boolean isApplicationTab(){
        return mAppsCustomizePagedView.isApplicationTab();
    }

    public void setApps(ArrayList<ApplicationInfo> list) {
        long groupId = 1;
        if (mRestoreState != null){
            // Restore the group before calling setApps because
            // they need to know the group to bind the apps
            groupId = mRestoreState.getLong(SAVE_STATE_CURRENT_GROUP, 1);
            setCurrentGroupId(groupId);
            mAppsCustomizePagedView.setApps(list);
            // Call the restore after calling setApps because
            // it may restore a dialog and the dialogs need the
            // list of applications to display them.
            restoreState();
        } else {
            groupId = getUserSelectedGroupId();
            // Restore the group before calling setApps because
            // they need to know the group to bind the apps
            setCurrentGroupId(groupId);
            mAppsCustomizePagedView.setApps(list);
        }

        setGroupButton(groupId);

        // Already restored
        mRestoreState = null;
    }

    public void setGroupButton(long groupId){
        mGroupButton.setSelection(mGroupAdapter.indexOf(groupId), false);
    }

    static boolean isNew(int editState) {
        return (editState == EDIT_STATE_NEW) || (editState == EDIT_STATE_ADD_TO_NEW);
    }

    public GroupItem getCurrentGroupItem() {
        long id = mCurrentGroupId;
        return mGroupAdapter.getGroupItemFromId(id);
    }

    public void onTabChanged(AppsCustomizePagedView.ContentType type){
	Log.d("dxx","AllAppsPage---onTabChange");
	/*Added by ncqp34 at Jul-12-2012 for switchui-2107*/
	if(type != AppsCustomizePagedView.ContentType.Applications){
        try{
            if (mPopupWindow == null){
                Field field = Spinner.class.getDeclaredField("mPopup");
                field.setAccessible(true);
                mPopupWindow = (ListPopupWindow) field.get(mGroupButton);
            }
            mPopupWindow.dismiss();
        } catch(Exception e){
            e.printStackTrace();
        }
	}
	/*ended by ncqp34*/
	Log.d("Test", "onTabChanged  type=" + type);
        setAppsPageItems(type == AppsCustomizePagedView.ContentType.Applications);
        onBackPressed();
    }

    int getAppsListSize(){
        return mAppsCustomizePagedView.getAppsListSize();
    }

    ApplicationInfo getAppsListItem(int index){
        return mAppsCustomizePagedView.getAppsListItem(index);
    }

    boolean groupAppsListContains(ApplicationInfo info){
        return mAppsCustomizePagedView.groupAppsListContains(info);
    }

    public void notifyGroupChanged(){
        mAppsCustomizePagedView.refreshPage();
    }

    /** Add the group being edited to the workspace as a folder. */
    void addGroupToWorkspace() {
        //Modified by e13775 July25 for SWITCHUI-2404 start
        //sleep 500 ms to avoid db chaos caused by fast operation 
        try {
            Thread.sleep(500);
        } catch (InterruptedException exc) { }
        //Modified by e13775 July25 for SWITCHUI-2404 end

        // Remember which group is being edited
        final GroupItem groupItem = getCurrentGroupItem();

        // Launch home app dragging icon for edited group
        final Intent intent = groupItem.createIntent(getContext())
            .setAction(ACTION_INSTALL_SHORTCUT);
        getContext().sendBroadcast(intent);

        // Close apps tray after adding to home
        onBackPressed();

        // log the event
        //HomeCheckin.logMeanEventGroupToWorkspace(groupItem.getName(getContext()));
    }

    /** Called to add an application to a group. */
    void addAppToGroup(int position) {
        position = mGroupAdapter.getUnfilteredPosition(position);
        final GroupItem groupItem = mGroupAdapter.getGroupItemFromIndex(position);
        mAppsModel.addToGroup(mCurrentApp, groupItem.getId());

        // Show success toast
        final String formatString = getContext().getResources().getString(R.string.app_added_to_group);
        final String message = MessageFormat.format(formatString,
                mCurrentApp.title.toString(), groupItem.getName(getContext()));
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

        // log event
        //HomeCheckin.logMeanEventAppAddedToGroup(groupItem.getName(getContext()), mCurrentApp);
    }

    /**Change this appicon. */
    void changeAppIcon() {
        Intent intent = new Intent(ACTION_CUSTOMIZE_ICON);
        intent.putExtra("packageName", mCurrentApp.componentName.getPackageName());
        intent.putExtra("iconId", mCurrentApp.icon);
        intent.setDataAndType(null, "image/jpg");
        Log.i(TAG, "changeAppIcon, intent is:"+intent.toString());
        getContext().startService(intent);
    }

    void restoreAppIcon() {
        Intent intent = new Intent(ACTION_RESTORE_ICON);
        intent.putExtra("packageName", mCurrentApp.componentName.getPackageName());
        intent.putExtra("iconId", mCurrentApp.icon);
        Log.i(TAG, "restoreAppIcon, intent is:"+intent.toString());
        getContext().startService(intent);
    }

    boolean isAppIconServiceInstalled() {
       boolean installed = false;
       try {
               android.content.pm.ApplicationInfo appinfo = null;
               PackageInfo pkgInfo = this.getContext().getPackageManager().getPackageInfo(APPICONSERVICE,0);
               if (pkgInfo != null) {
                   appinfo = pkgInfo.applicationInfo;
                   if ((null != appinfo) && ((appinfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM)!=0)) {
                       installed = true;
                   }
               }
       } catch(PackageManager.NameNotFoundException e) {
               Log.e(TAG, "Exception " + e);
               e.printStackTrace();
       }
       return installed;
    }

    /** Remove the app that was long-pressed from the current group. */
    void removeAppFromGroup() {
        final GroupItem groupItem = getCurrentGroupItem();
        final long groupId = groupItem.getId();
        mAppsModel.removeFromGroup(mCurrentApp, groupId);
        notifyGroupChanged();

        // Show success toast
        final String formatString = getContext().getResources().getString(R.string.app_removed_from_group);
        final String message = MessageFormat.format(formatString, mCurrentApp.title.toString());
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    void startApplicationDetailsActivity() {
        String packageName = mCurrentApp.componentName.getPackageName();
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        getContext().startActivity(intent);
    }

    /** Tell Android to uninstall the application from the device. */
    void uninstallApp() {
        final Uri packageURI = Uri.parse("package:" + mCurrentApp.componentName.getPackageName());
        final Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
        getContext().startActivity(uninstallIntent);

        // log the event
        //HomeCheckin.logMeanEventUninstall(mCurrentApp.componentName.getPackageName());
    }

    /** Tell Android to disable the application from the device. */
    void disableApp() {
        final String pkg = mCurrentApp.componentName.getPackageName();
        final PackageManager pm = getContext().getPackageManager();
        new Thread(new Runnable(){
            public void run(){
                pm.setApplicationEnabledSetting(pkg,
                                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
                                                0);
            }
        }).start();
    }


    /** Remove the group once the user has confirmed it. */
    void removeGroup() {
        //Modified by e13775 July25 for SWITCHUI-2404 start
        //To avoid mis-delete preload groups  
        if (mCurrentGroupId == 0 || mCurrentGroupId == 1 || mCurrentGroupId ==2)
            return;
        //Modified by e13775 July25 for SWITCHUI-2404 end
        // Remove the group from our groups adapter
        final GroupAdapter appsGroupAdapter = mGroupAdapter;
        final GroupItem groupItem = getCurrentGroupItem();
        appsGroupAdapter.remove(groupItem);
        // Set new group
        setCurrentGroupIndex(0);
        // Set the current position in the spinner
        setGroupButton(mCurrentGroupId);

        // If you change here, you need to chage the FocusOnlyTabWidget.onMeasure
        // Force an adjustment in the measure
        //Modified by e13775 at July11 2012 for organize apps' group
        //((View)mGroupButton.getParent()).getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
        //mGroupButton.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
        //Modified by e13775 at July11 2012 for organize apps' group
        // Refresh the page
        notifyGroupChanged();

        // Remove the group for all apps
        mAppsModel.removeGroup(groupItem.getId());

        // Show success toast
        final String formatString = getContext().getResources().getString(R.string.remove_group_toast);
        final String message = MessageFormat.format(formatString, groupItem.getName(getContext()));
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

        // Tell home screen to delete any shortcuts to this group
        final Intent intent = groupItem.createIntent(getContext())
                .setAction(ACTION_UNINSTALL_SHORTCUT);

        getContext().sendBroadcast(intent);

        setEditState(EDIT_STATE_NONE);
    }

    /** Start a drag on the workspace with the current app. */
    void addAppToWorkspace() {
        Intent intent = null;
        // Launch home app with current app
       /* if (mCurrentApp.itemType == LauncherSettings.Favorites.ITEM_TYPE_SYSTEM_FOLDER){
            // Adds SystemFolder as UserFolder in the home screen
            SystemFolderInfo folderInfo = (SystemFolderInfo)mCurrentApp;
            ArrayList<Intent> appIntents = new ArrayList<Intent>();
            // Gets the applications into the SystemFolder
            for (ApplicationInfo appInfo:folderInfo.getContents()){
                appIntents.add(appInfo.intent);
            }
            intent = new Intent(ACTION_INSTALL_FOLDER)
                .putExtra(Intent.EXTRA_SHORTCUT_NAME, mCurrentApp.title)
                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, appIntents);
        } else {*/
            // Adds an application
            final LauncherApplication app = (LauncherApplication) getContext().getApplicationContext();
            final Intent appIntent = mCurrentApp.intent;
            intent = new Intent(ACTION_INSTALL_SHORTCUT)
                .putExtra(Intent.EXTRA_SHORTCUT_NAME, mCurrentApp.title)
                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, appIntent)
                .putExtra(Intent.EXTRA_SHORTCUT_ICON, app.getIconCache().getIcon(appIntent));

       // }
        getContext().sendBroadcast(intent);
        onBackPressed();
    }

    /**
     * Called to set the group for filtering the apps tray.
     * @param position Index of the group to select.
     * @return true for success.
     */
    boolean setCurrentGroupIndex(int position) {
        final GroupItem groupItem = mGroupAdapter.getGroupItemFromIndex(position);
        if (groupItem == null) {
            return false;
        }
        final long groupId = groupItem.getId();
        if (groupId <= 0) {
            return false;
        }

        return setCurrentGroupId(groupId);
    }

    /**
     * Called to set the group for filtering the apps tray.
     * @param groupId ID of the group to select.
     * @return true for success.
     */
    public boolean setCurrentGroupId(long groupId) {
        final GroupItem groupItem = (groupId <= 0 ? null :
                mGroupAdapter.getGroupItemFromId(groupId));
        if (groupItem == null) {
            Log.w(TAG, "Illegal groupId "+ groupId);
            return false;
        }
        mCurrentGroupId = groupId;

        // Notify AllAppsPagedView
        mAppsCustomizePagedView.setGroup(groupItem);

        // Tell the group picker that groups have changed
        mGroupAdapter.notifyDataSetChanged();
        setUserSelectedGroupId(groupId);

        return true;
    }

    /** Sets the sorting option for the current group. */
    public void setGroupSort(int sort) {
        // Update the main grid
        final GroupItem groupItem = getCurrentGroupItem();
        groupItem.setSort(sort);
        notifyGroupChanged();

        // Save the sort index in the model
        mAppsModel.setGroupSort(groupItem.getId(), sort);
    }

    //---------------
    // EDIT MODE

    /**
     * Called to change edit mode.
     * @param editState The new edit state.
     */
    void setEditState(int editState) {
        if (editState == mEditState) {
            return;
        }

        boolean editing = (editState != EDIT_STATE_NONE);
        boolean newGroup = isNew(editState);

        if (editing) {
            if (newGroup) {
                mEditGroupItem = new GroupItem("");
            } else if (mEditGroupItem == null) {
                GroupItem groupItem = getCurrentGroupItem();
                mEditGroupItem =  new GroupItem(groupItem.getId(),
                                                groupItem.getName(getContext()),
                                                groupItem.getType(),
                                                groupItem.getSort(),
                                                groupItem.getIconSet());
            }

            if (mRestoreState != null) {
                // we are restoring the state
                mEditGroupItem.setName(mRestoreState.getCharSequence(SAVE_STATE_EDIT_TEXT, mEditGroupItem.getName(getContext())));
                mEditGroupItem.setIconSet(mRestoreState.getInt(SAVE_STATE_EDIT_ICON, mEditGroupItem.getIconSet()));
            } else {
                // Happy path
                getEditGroupDialog().show();
            }
        } else {
            mEditGroupItem = null;
        }

        mEditState = editState;
    }

    /** End the group edit session. */
    void endEditGroup() {
        // Reset the views to normal mode
        setEditState(EDIT_STATE_NONE);
        setGroupButton(mCurrentGroupId);
    }

    void startNewGroup() {
        setEditState(EDIT_STATE_NEW);
    }

    /** Verify that it's OK to save the group. */
    boolean saveGroup(boolean checkDupName) {
        if(mEditGroupItem == null) {
            //setEditState can make this variable null
            return false;
        }

        // Check for duplicate group names
        if (checkDupName) {
            final CharSequence name = mEditGroupItem.getName(getContext());
            final CharSequence otherName = mAppsModel.groupHasName(name, mEditGroupItem);
            if (otherName != null) {
                DuplicateNameDialog dialog = getDuplicateNameDialog();
                dialog.setNewName(otherName);
                setNextDialog(DIALOG_DUPLICATE_NAME);
                return false;
            }
        }

        // All systems go!
        doSaveGroup();
        return true;
    }

    /** Save the group that the user has been editing or creating. */
    void doSaveGroup() {
        final boolean isNewGroup = isNew(mEditState);
        if (isNewGroup) {
            // If it was a new group, add to adapter
            mGroupAdapter.add(mEditGroupItem);
        } else {
            // Otherwise, set the group in the adapter
            GroupItem groupItem = getCurrentGroupItem();
            groupItem.setName(mEditGroupItem.getName(getContext()));
            groupItem.setIconSet(mEditGroupItem.getIconSet());
            mEditGroupItem = groupItem;
            // and send an event to home to update the shortcut
            // if it has this one
            // Launch home app dragging icon for edited group
            final Intent intent = mEditGroupItem.createIntent(getContext())
                .setAction(ACTION_UPDATE_SHORTCUT);
            getContext().sendBroadcast(intent);
        }

        // Save the item (or create the item)
        long groupId = mAppsModel.saveGroup(mEditGroupItem);

        // Save the selected icon
        mAppsModel.setGroupIconSet(mEditGroupItem.getId(), mEditGroupItem.getIconSet());

        if (mEditState == EDIT_STATE_NEW) {
            // log event
            //HomeCheckin.logMeanEventAddGroup(mEditGroupItem.getName(getContext()));
        }

        // Sort the group list and filter by the new group
        mGroupAdapter.sort();
        setCurrentGroupId(groupId);
        setGroupButton(groupId);
        setEditState(EDIT_STATE_NONE);
        notifyGroupChanged();

        if (isNewGroup) {
            getSelectAppsDialog().show();
        }
    }

    //---------------
    // DIALOGS

    /**
     * Interface for dialogs launched by the apps tray.  This is mainly useful
     * for saving and restoring state before and after an orientation change.
     */
    interface AppsDialog {
        /** @return the integer constant that represents this dialog. */
        int getId();
        /** Display this dialog. */
        void show();
        /** Save dialog state in preparation for an orientation switch. */
        void saveState(Bundle state);
        /** Restore dialog state following an orientation switch. */
        void restoreState(Bundle state);
        /** Called to dismiss the dialog. */
        void dismiss();
    }

    /**
     * Helper function to show an AlertDialog.  Saves the dialog in a field
     * so that it can be dismissed if the apps tray is closed.
     * @param builder
     */
    void showDialog(Dialog dialog) {
        if (mDialog != null) {
            // If another dialog running, dismiss now
            mDialog.dismiss();
        }
        if (dialog != null) {
            // Show the dialog
            mDialog = dialog;
            mDialog.show();
            // Listen for dismiss so we can clear the field.
            mDialog.setOnDismissListener(this);
            // Listen for search key
            mDialog.setOnKeyListener(this);
        }
    }

    @Override
    protected void onDetachedFromWindow (){
        showDialog(null);
        super.onDetachedFromWindow();
    }

    /**
     * Called from one dialog to launch another dialog after it is dismissed.
     * This avoids a threading issue where the next dialog sets the mAppsDialog
     * field, only to have onDismiss for the previous dialog clear it.
     * @param nextDialogId The AppsDialog ID of the next dialog to show.
     */
    void setNextDialog(int nextDialogId) {
        mNextDialogId = nextDialogId;
    }

    /*Added by ncqp34 at Jul-12-2012 for switchui-2128*/
    public void dismissSpinnerPopup(){
        try{
            if (mPopupWindow == null){
                Field field = Spinner.class.getDeclaredField("mPopup");
                field.setAccessible(true);
                mPopupWindow = (ListPopupWindow) field.get(mGroupButton);
            }
            mPopupWindow.dismiss();
        } catch(Exception e){
            e.printStackTrace();
        }
    }
    /*ended by ncqp34*/

    public void resetAllAppsState() {
        mNextDialogId = DIALOG_NONE;

        // Close the group drop down
        // A hack to close the spinner when
        // the home key is pressed
        try{
            if (mPopupWindow == null){
                Field field = Spinner.class.getDeclaredField("mPopup");
                field.setAccessible(true);
                mPopupWindow = (ListPopupWindow) field.get(mGroupButton);
            }
            mPopupWindow.dismiss();
        } catch(Exception e){
            e.printStackTrace();
        }

        // Close the menu drop down
        if (null !=mMoreOverFlowButton)
            mMoreOverFlowButton.dismiss();

        // Close other dialog, if any
        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (mAppsDialog != null) {
            mAppsDialog.dismiss();
            mAppsDialog = null;
        }

        // Reset the views to normal mode
        endEditGroup();
		/* 2012-11-27, Added by amt_chenjing for SWITCHUITWO-135 */
		if ((mAppsCustomizePagedView.pt != null && mAppsCustomizePagedView
				.isPageMoving())
				|| (mAppsCustomizePagedView.pt.ani != null && mAppsCustomizePagedView.pt.ani.play))
			mAppsCustomizePagedView.pt.resetPagedView(mAppsCustomizePagedView);
		/* 2012-11-27, Add end */
    }

    public void onDismiss(DialogInterface dialog) {
        if (mDialog == dialog) {
            mDialog = null;
        }

        if (mAppsDialog != null) {
            mAppsDialog.dismiss();
            mAppsDialog = null;
        }

        // If the previous dialog wants to launch another, then launch it now
        if (mNextDialogId != DIALOG_NONE) {
            getDialog(mNextDialogId).show();
            mNextDialogId = DIALOG_NONE;
        } else {
            // We use dialogs to edit/create a group.
            // The best place to check if an editions is over
            // is here. If we are editing and there is no more
            // dialog means the edition is over.
            if (mEditState != EDIT_STATE_NONE) {
                endEditGroup();
            }
        }
    }

    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * Fetch a new instance of the dialog object corresponding to the
     * given dialog ID.  This is used when restoring state after an
     * orientation switch.
     * @param which the dialog ID.
     * @return a new AppsDialog object.
     */
    AppsDialog getDialog(int which) {
        switch (which) {
            case DIALOG_SELECT_APPS:
                return getSelectAppsDialog();
            case DIALOG_CONFIRM_REMOVE:
                return getConfirmRemoveDialog();
            case DIALOG_CONFIRM_DISABLE_APP:
                return getConfirmDisableAppDialog();
            case DIALOG_DUPLICATE_NAME:
                return getDuplicateNameDialog();
            case DIALOG_SORT_OPTIONS:
                return getSortOptionsDialog();
            case DIALOG_SET_ICON:
                return getSetIconDialog();
            case DIALOG_EDIT_GROUP:
                return getEditGroupDialog();
        }
        return null;
    }


    private SelectAppsDialog getSelectAppsDialog() {
        if (mSelectAppsDialog == null) {
            mSelectAppsDialog = new SelectAppsDialog(this);
        }
        return mSelectAppsDialog;
    }

    private ConfirmRemoveDialog getConfirmRemoveDialog() {
        if (mConfirmRemoveDialog == null) {
            mConfirmRemoveDialog = new ConfirmRemoveDialog();
        }
        return mConfirmRemoveDialog;
    }

    private ConfirmDisableAppDialog getConfirmDisableAppDialog() {
        if (mConfirmDisableAppDialog == null) {
            mConfirmDisableAppDialog = new ConfirmDisableAppDialog();
        }
        return mConfirmDisableAppDialog;
    }

    private DuplicateNameDialog getDuplicateNameDialog() {
        if (mDuplicateNameDialog == null) {
            mDuplicateNameDialog = new DuplicateNameDialog();
        }
        return mDuplicateNameDialog;
    }

    private SortOptionsDialog getSortOptionsDialog() {
        if (mSortOptionsDialog == null) {
            mSortOptionsDialog = new SortOptionsDialog();
        }
        return mSortOptionsDialog;
    }

    public SetGroupIconDialog getSetIconDialog() {
        if (mSetIconDialog == null) {
            mSetIconDialog = new SetGroupIconDialog(this);
        }
        return mSetIconDialog;
    }

    public EditGroupDialog getEditGroupDialog() {
        if (mEditGroupDialog == null) {
            mEditGroupDialog = new EditGroupDialog(this);
        }
        return mEditGroupDialog;
    }

    private void startManageApp(){
        Intent manageApps = new Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS);
        manageApps.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        /*2012-7-11, modify by bvq783 for switchui-2081*/
        getContext().startActivity(manageApps);
        /*2012-7-11, modify end*/
    }

    private void startSystemSettings() {
        Intent settings = new Intent(android.provider.Settings.ACTION_SETTINGS);
        settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        /*2012-7-11, modify by bvq783 for switchui-2081*/
        getContext().startActivity(settings);
        /*2012-7-11, modify end*/
    }
    /**
     * The user has chosen an icon set to represent a group.  If editing, we
     * update the icon in the action bar.  If we came from the group menu,
     * we set the icon into the group now.
     * @param iconSet The index of the icon set to use for the group.
     */
    void setGroupIconSet(int iconSet) {
        if (mEditState != EDIT_STATE_NONE) {
            mEditGroupItem.setIconSet(iconSet);
        }
    }

    /**
     * Called to set the icon set for a group.
     * @param groupItem The group to modify.
     * @param iconSet The new icon chosen by the user.
     */
    private void doSetGroupIconSet(GroupItem groupItem, int iconSet) {
        groupItem.setIconSet(iconSet);
        mAppsModel.setGroupIconSet(groupItem.getId(), iconSet);
    }

    GroupItem getEditGroupItem() {
        return mEditGroupItem;
    }

    /* there function need modification later*/
    Drawable getGroupIcon(int iconSet) {
        /*TypedArray array = mConfig.obtainTypedArray(ProductConfigs.EDIT_ICON_IDS);
        Drawable d = array.getDrawable(iconSet);
        array.recycle();
        return d;*/
        return null;

    }

    //----------------------------------------------------
    // SortOptionsDialog

    /**
     * Manages a dialog to let user select a group sorting options.
     */
    class SortOptionsDialog implements AppsDialog, DialogInterface.OnClickListener {

        String[] mSortOptions;
        ArrayList<Integer> mSortOptionsMap;

        public SortOptionsDialog(){
            Resources res = getContext().getResources();
            ArrayList<String> sortOptions = new ArrayList<String>();
            mSortOptionsMap = new ArrayList<Integer>();

            if(mAppsModel.hasCarrierList()){
                mSortOptionsMap.add(Groups.SORT_CARRIER);
                sortOptions.add(res.getString(R.string.apps_sort_carrier));
            }

            mSortOptionsMap.add(Groups.SORT_ALPHA);
            sortOptions.add(res.getString(R.string.apps_sort_alphabetically));

            mSortOptionsMap.add(Groups.SORT_FREQUENTS);
            sortOptions.add(res.getString(R.string.apps_sort_frequently));

            mSortOptions = sortOptions.toArray(new String[sortOptions.size()]);
        }

        /** Show a menu of sorting options for the current group. */
        @Override
        public void show() {
            mAppsDialog = this;
            int currentSelection = mSortOptionsMap.indexOf(getCurrentGroupItem().getSort());
            //Sanity check
            currentSelection = (currentSelection != -1)?currentSelection:0;

            showDialog(new AlertDialog.Builder(getContext())
                .setTitle(R.string.sort_group)
                .setSingleChoiceItems(mSortOptions,currentSelection, this)
                .setNegativeButton(android.R.string.cancel, null)
                .create());
        }

        /** Respond to a sorting selection. */
        @Override
        public void onClick(DialogInterface dialog, int which) {
            setGroupSort(mSortOptionsMap.get(which));
            dialog.dismiss();
        }

        @Override
        public int getId() {
            return DIALOG_SORT_OPTIONS;
        }

        @Override
        public void restoreState(Bundle state){}

        @Override
        public void saveState(Bundle state){}

        @Override
        public void dismiss() {}

    }

    //-----------------------------------------------------
    // ConfirmRemoveDialog

    /**
     * Manages a dialog to confirm the removal of an apps group.
     */
    class ConfirmRemoveDialog implements AppsDialog, DialogInterface.OnClickListener {
        /** Ask if the group should be removed. */
        @Override
        public void show() {
            mAppsDialog = this;
            final GroupItem groupItem = getCurrentGroupItem();
            final Resources res = getContext().getResources();
            showDialog(new AlertDialog.Builder(getContext())
                .setTitle(R.string.remove)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(MessageFormat.format(
                    res.getString(R.string.remove_group_confirm),
                    groupItem.getName(getContext())))
                .setPositiveButton(R.string.remove, this)
                .setNegativeButton(android.R.string.cancel, null)
                .create());
        }

        /** Remove the group on a positive button press. */
        @Override
        public void onClick(DialogInterface dialog, int which) {
            removeGroup();
            dialog.dismiss();
        }

        @Override
        public int getId() {
            return DIALOG_CONFIRM_REMOVE;
        }

        @Override
        public void restoreState(Bundle state){}

        @Override
        public void saveState(Bundle state){}

        @Override
        public void dismiss() {}

    }

    //-----------------------------------------------------
    // DuplicateNameDialog

    /**
     * Manages a dialog to warn the user of duplicate group names.
     */
    class DuplicateNameDialog implements AppsDialog, DialogInterface.OnClickListener {
        private CharSequence mNewName;

        public void setNewName(CharSequence newName) {
            mNewName = newName;
        }

        /** Ask if the group should be removed. */
        @Override
        public void show() {
            mAppsDialog = this;
            final Resources res = getContext().getResources();
            showDialog(new AlertDialog.Builder(getContext())
                .setTitle(R.string.duplicate_name_title)
                .setIcon(R.drawable.ic_dialog_alert)
                .setMessage(MessageFormat.format(
                    res.getString(R.string.duplicate_name_message), mNewName))
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, null)
                .create());
        }

        /** Remove the group on a positive button press. */
        @Override
        public void onClick(DialogInterface dialog, int which) {
            doSaveGroup();
            dialog.dismiss();
        }

        @Override
        public int getId() {
            return DIALOG_DUPLICATE_NAME;
        }

        @Override
        public void restoreState(Bundle state) {
            mNewName = state.getString(SAVE_STATE_DND_NEW_NAME, "");
        }

        @Override
        public void saveState(Bundle state) {
            state.putCharSequence(SAVE_STATE_DND_NEW_NAME, mNewName);
        }

        @Override
        public void dismiss() {
            endEditGroup();
        }

    }

    //-----------------------------------------------------
    // ConfirmDisableAppDialog

    /**
     * Manages a dialog to confirm the removal of an apps group.
     */
    class ConfirmDisableAppDialog implements AppsDialog, DialogInterface.OnClickListener {
        /** Ask if the group should be removed. */
        @Override
        public void show() {
            mAppsDialog = this;
            final GroupItem groupItem = getCurrentGroupItem();
            final Resources res = getContext().getResources();
            showDialog(new AlertDialog.Builder(getContext())
                .setTitle(R.string.disable_dlg_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(MessageFormat.format(
                    res.getString(R.string.disable_dlg_text),
                    groupItem.getName(getContext())))
                .setPositiveButton(R.string.disable, this)
                .setNegativeButton(android.R.string.cancel, null)
                .create());
        }

        /** Remove the group on a positive button press. */
        @Override
        public void onClick(DialogInterface dialog, int which) {
            disableApp();
            dialog.dismiss();
        }

        @Override
        public int getId() {
            return DIALOG_CONFIRM_DISABLE_APP;
        }

        @Override
        public void restoreState(Bundle state){}

        @Override
        public void saveState(Bundle state){}

        @Override
        public void dismiss() {}
    }

    /**
     * Save the current state of the view, so that it can be restored next time.
     * @return Return all state information in a Bundle.
     */
    public Bundle getSaveInstanceState() {
        // If happens a lot of orientation changes, there is a
        // chance that the getSaveInstanceState is called before
        // the organize apps tray restore its state by restoreState.
        // In this case you send back the mRestoreState;
        if (mRestoreState != null){
            return mRestoreState;
        }

        Bundle outState = new Bundle();
        try {
            // Added by amt_chenjing for SWITCHUI-2116 20120715 begin
            if(mCurrentGroupId == 0) {
                mCurrentGroupId = getUserSelectedGroupId();
            }
            // Added by amt_chenjing for SWITCHUI-2116 20120715 end
            outState.putLong(SAVE_STATE_CURRENT_GROUP, mCurrentGroupId);
            outState.putInt(SAVE_STATE_EDIT_STATE, mEditState);

            if (mEditState != EDIT_STATE_NONE) {
                outState.putCharSequence(SAVE_STATE_EDIT_TEXT, mEditGroupItem.getName(getContext()));
                outState.putInt(SAVE_STATE_EDIT_ICON, mEditGroupItem.getIconSet());
            }

            outState.putString(SAVE_STATE_CURRENT_APP, mCurrentApp == null ? null :
                mCurrentApp.intent.getComponent().flattenToString());

            if (mAppsDialog == null) {
                outState.putInt(SAVE_STATE_APPS_DIALOG, DIALOG_NONE);
            } else {
                outState.putInt(SAVE_STATE_APPS_DIALOG, mAppsDialog.getId());
                mAppsDialog.saveState(outState);
            }

        } catch (Exception e) {
            Log.w(TAG, "Unable to save AppsView state," + e);
        }

        return outState;
    }

    public void restoreInstanceState(Bundle inState){
        mRestoreState = inState;
    }

    /**
     * Restores the state of the view to what it was before we were so
     * rudely interrupted.
     * @param inState Retrieve all state information from here.
     */
    public void restoreState(){
        if (mRestoreState == null) {
            return;
        }

        setEditState(mRestoreState.getInt(SAVE_STATE_EDIT_STATE, EDIT_STATE_NONE));

        final String componentString = mRestoreState.getString(SAVE_STATE_CURRENT_APP, null);
        if (componentString != null) {
            mCurrentApp = mAppsCustomizePagedView.getAppInfo(ComponentName.unflattenFromString(componentString));
        }

        final int appsDialogId = mRestoreState.getInt(SAVE_STATE_APPS_DIALOG, DIALOG_NONE);
        if (appsDialogId != DIALOG_NONE) {
            final AppsDialog dialog = getDialog(appsDialogId);
            if (dialog != null) {
                dialog.show();
                dialog.restoreState(mRestoreState);
            }
        }
    }

    private void setUserSelectedGroupId(final long groupId) {
        final SharedPreferences sharedPrefs = getContext().getSharedPreferences(
                PREFS_FILE_STATE, getContext().MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putLong(KEY_USER_SELECTED_GROUP_ID, groupId);
        editor.apply();
    }

    private long getUserSelectedGroupId() {
        final SharedPreferences sharedPrefs = getContext().getSharedPreferences(
                PREFS_FILE_STATE, getContext().MODE_PRIVATE);
        long groupId = sharedPrefs.getLong(KEY_USER_SELECTED_GROUP_ID, /*All apps*/1);
        return groupId;
    }

    public ImageView getAppsGroupIcon() {
        return (ImageView) mGroupButton.findViewById(R.id.all_apps_drop_down_group_item_button_icon);
    }

    void startDragMenu(View v){
        mDragging = true;
        mCurrentApp = (ApplicationInfo)v.getTag();
        mDraggedView = v;
    }

    void endDragMenu(boolean animate){
        mDragging = false;
        mDraggedView = null;
    }

    private static int clamp(int val, int min, int max) {
        if (val < min) {
            return min;
        } else if (val >= max) {
            return max - 1;
        } else {
            return val;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        final int screenX = clamp((int)ev.getRawX(), 0, mDisplayMetrics.widthPixels);
        final int screenY = clamp((int)ev.getRawY(), 0, mDisplayMetrics.heightPixels);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // Remember location of down touch
                mMotionDownX = screenX;
                mMotionDownY = screenY;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mDragging) {
                    if (mStartMenuOnUp) {
                        mCurrentApp = (ApplicationInfo)mDraggedView.getTag();
                        endDragMenu(false);
                    } else {
                        endDragMenu(true);
                    }
                }
                break;
        }

        return mDragging;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mDragging) {
            final int action = ev.getAction();
            final int screenX = clamp((int)ev.getRawX(), 0, mDisplayMetrics.widthPixels);
            final int screenY = clamp((int)ev.getRawY(), 0, mDisplayMetrics.heightPixels);

            switch (action) {
            case MotionEvent.ACTION_DOWN:
                // Remember where the motion event started
                mMotionDownX = screenX;
                mMotionDownY = screenY;
            case MotionEvent.ACTION_MOVE:
                mMotionDownX = screenX;
                mMotionDownY = screenY;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mDragging) {
                    if (mStartMenuOnUp) {
                        mCurrentApp = (ApplicationInfo)mDraggedView.getTag();
                        endDragMenu(false);
                    } else {
                        endDragMenu(true);
                    }
                }
                break;
            }
            return true;
        }
        return super.onTouchEvent(ev);
    }


    public void startActivity(Intent intent) {
        //mAppsCustomizePagedView.startActivitySafely(intent);
    }

    public void setOverFlowMenu(View overFlowMenu) {
        if (overFlowMenu != null && overFlowMenu instanceof AllAppsDropDownMenu) {
            mMoreOverFlowButton = (AllAppsDropDownMenu) overFlowMenu;
            mMoreOverFlowButton.setOnItemSelectedListener(this);
            mMoreOverFlowButton.setAdapter(mGroupOptinosAdapter);
            mMoreOverFlowButton.setOnClickListenerSpinner(this);
	    /*Added by ncqp34 at Jul-12-2012 for switchui-2128*/
	    mMoreOverFlowButton.setAllAppPage(this);
	    /*ended by ncqp34*/
        }
    }

@Override
    public void onClickSpinner() {
    }

    /*2012-7-16, add by bvq783 for switchui-2237*/
    public boolean checkShowSpinner() {
        LauncherApplication app = (LauncherApplication) getContext().getApplicationContext();
    	Launcher launcher = app.getLauncher();
        boolean bApps = launcher.isAllAppsCustomizeOpen();
        Log.d("July", "checkShowSpinner, launcher is in apps:"+bApps);
        return bApps;
    }
    /*2012-7-16, add by bvq783 end*/

    public void onCreateOptionsMenu(Menu menu) {
        /*Added by ncqp34 at Jul-12-2012 for switchui-2177*/ 	
	menu.add(Launcher.MENU_GROUP_SEARCH_APP_WIDGET,MENU_SEARCH_APP_WIDGET,0, R.string.menu_search_app_and_widget)
        .setAlphabeticShortcut('S');
	/*ended by ncqp34*/
        menu.add(Launcher.MENU_GROUP_ADD_GROUP_TO_HOME, MENU_ADD_TO_HOME, 0, R.string.add_to_home)
        .setIcon(R.drawable.ic_thb_add_to_home)
        .setAlphabeticShortcut('H');
        menu.add(Launcher.MENU_GROUP_EDIT_APPS_GROUP, MENU_ADD_RM_APPS, 1, R.string.add_rm_apps)
        .setIcon(R.drawable.ic_thb_add_to_group)
        .setAlphabeticShortcut('R');
        //2012-7-15 add by Hu ShuAn for switchui-2203
        menu.add(Launcher.MENU_GROUP_EDIT_APPS_GROUP, MENU_EDIT_GROUP, 2, R.string.edit_group_name)
        .setIcon(R.drawable.ic_thb_edit)
        .setAlphabeticShortcut('E');
        menu.add(Launcher.MENU_GROUP_EDIT_APPS_GROUP, MENU_DELETE_GROUP, 3, R.string.delete_group)
        .setIcon(R.drawable.ic_launcher_clear_normal_holo)
        .setAlphabeticShortcut('D');
        //added by amt_wangpeipei 2012/07/21 for switchui-2397 begin
        mMenu = menu;
        //added by amt_wangpeipei 2012/07/21 for switchui-2397 end.
    }
    
    //added by amt_wangpeipei 2012/07/21 for switchui-2397
    public Menu getMenu(){
    	return mMenu;
    }

    public void onPrepareOptionsMenu(Menu menu) {
    /*Added by ncqp34 at Jul-17-2012 for group switch*/
    if(LauncherApplication.mGroupEnable){   
        if (mGroupButton != null && mGroupButton.isEnabled()) {
            GroupItem currentGroupItem = getCurrentGroupItem();
            if (currentGroupItem != null) {
                menu.setGroupVisible(Launcher.MENU_GROUP_EDIT_APPS_GROUP, currentGroupItem.isEditable());
		/*Added by ncqp34 at Jul-12-2012 for switchui-2163*/
        	menu.setGroupVisible(Launcher.MENU_GROUP_ADD_GROUP_TO_HOME, true);
		/*ended by ncqp34*/
            } else {
                menu.setGroupVisible(Launcher.MENU_GROUP_EDIT_APPS_GROUP, false);
		/*Added by ncqp34 at Jul-12-2012 for switchui-2163*/
        	menu.setGroupVisible(Launcher.MENU_GROUP_ADD_GROUP_TO_HOME, false);
		/*ended by ncqp34*/
            }
        } else {
            menu.setGroupVisible(Launcher.MENU_GROUP_EDIT_APPS_GROUP, false);
	    /*Added by ncqp34 at Jul-12-2012 for switchui-2163*/
            menu.setGroupVisible(Launcher.MENU_GROUP_ADD_GROUP_TO_HOME, false);
	    /*ended by ncqp34*/
        }
    }
  /*ended by ncqp34*/

        /*Added by ncqp34 at Jul-12-2012 for switchui-2177*/ 	
	menu.setGroupVisible(Launcher.MENU_GROUP_SEARCH_APP_WIDGET, true);
	/*ended by ncqp34*/
        // Modified by e13775 at July17 2012 for SWITCHUI-2279 start
        LauncherApplication app = (LauncherApplication) getContext().getApplicationContext();
        Launcher launcher = app.getLauncher();
        if (launcher.isAppsCustomizeSearchStatus()) 
            menu.setGroupVisible(Launcher.MENU_GROUP_ADD_GROUP_TO_HOME, false);
        // Modified by e13775 at July17 2012 for SWITCHUI-2279 end

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	/*Added by ncqp34 at Jul-12-2012 for switchui-2177*/ 
	case MENU_SEARCH_APP_WIDGET:
            LauncherApplication app = (LauncherApplication) getContext().getApplicationContext();
            enterSearchAppAndWidget(app);
	    return true;
	/*ended by ncqp34*/
        case MENU_ADD_TO_HOME:
            addGroupToWorkspace();
            return true;
        case MENU_ADD_RM_APPS:
            getSelectAppsDialog().show();
            return true;
        case MENU_EDIT_GROUP:
            setEditState(EDIT_STATE_EDIT);
            return true;
        case MENU_DELETE_GROUP:
            getConfirmRemoveDialog().show();
            return true;
        }
        return false;
    }
    public void markAsUsed(Intent intent) {
        mAppsModel.markAsUsed(intent);
        mAppsCustomizePagedView.refreshFrequentGroup();
    }


}

