package com.motorola.mmsp.taskmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import android.app.ActivityManager;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;


public class ApplicationActivity extends ListActivity implements View.OnClickListener,
        DialogInterface.OnCancelListener {
    private static final String TAG = "ApplicationActivity";
    private static final String FMRADIO_PACKAGENAME = "com.motorola.fmradio";
    private static final String MOTO_HOME_CLASS = "com.motorola.mmsp.motohome.HomeActivity";
    // added by amt_sunli 2012-12-24 SWITCHUITWO-329 begin
    final static String REMOTESETTING = "com.android.settings.multisettab.InternationalRoaming";
    final static String SETTING = "com.android.settings";
    // added by amt_sunli 2012-12-24 SWITCHUITWO-329 end

    private static final int MENU_ITEM_SWITCH_TO = 1;
    private static final int MENU_ITEM_END_TASK = 2;
    private static final int MENU_ITEM_CANCEL = 3;

    // temporary dialog displayed while the application info loads
    private static final int DLG_LOADING = 0;

    // Custom Adapter used for managing items in the list
    private ProcessAndTaskAdapter mAppInfoAdapter;
    // list of task info
    private ListView mTaskInfoList;
    // button of force stop
    private Button mForceStopButton;
    // button of refresh
    private Button mRefreshButton;

    public PackageManager mPm = null;
    public ActivityManager mactivityManager = null;

    private packageIntentReceiver mReceiver;

    // Thread to load resources
    ResourceLoaderThread mResourceThread;

    //remember the item been checked
    private final static HashSet<String> mCheckedItemSet = new HashSet<String>();

    // messages posted to the handler
    private static final int INIT_THREAD = 0;
    private static final int REFRESH_TOAST = 1;
    private static final int REFRESH_LIST = 2;
    private static final int DISMISS_DIALOG = 3;
    private static final int KILL_SERVICE_DELAY = 4;

    // stop fmradio service,then delay 100ms to restart package
    private static final int KILL_SERVICE_TIMEOUT = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Common.Log(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.application);

        /* task info */
        mPm = getPackageManager();
        mactivityManager = (ActivityManager) this
                .getSystemService(ACTIVITY_SERVICE);

        if (Common.log_flag)
            printRunningAppProcesses();

        List<ProcessAndTaskInfo> runningTasks = new ArrayList<ProcessAndTaskInfo>();

        /* list info */
        mAppInfoAdapter = new ProcessAndTaskAdapter(this, runningTasks);
        mTaskInfoList = getListView();
        setListAdapter(mAppInfoAdapter);
        // mTaskInfoList.setOnCreateContextMenuListener(this);
        mTaskInfoList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        /* button info */
        mForceStopButton = ((Button) findViewById(R.id.Button_End_App));
        mForceStopButton.setOnClickListener(this);
        mForceStopButton.setEnabled(false); // IKDOMINO-5119 "End" in Task Manager is not gray out when there is not any application selected.
        mRefreshButton = ((Button) findViewById(R.id.Button_Refresh_App));
        mRefreshButton.setOnClickListener(this);

        /* add listener of restart task */
        mReceiver = new packageIntentReceiver();
    }

    @Override
    public void onResume() {
        Common.Log(TAG, "onResume");
        super.onResume();
        // register receiver
        mReceiver.registerReceiver();
        /* init refresh thread */
        mHandler.sendEmptyMessage(INIT_THREAD);
    }

    @Override
    public void onPause() {
        Common.Log(TAG, "onPause");
        super.onPause();
        /* kill runing fresh thread */
        if (mResourceThread != null) {
            mResourceThread.setAbort();
        }
        // clear all messages related to application list
        clearMessagesInHandler();

        // register receiver here
        unregisterReceiver(mReceiver);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DLG_LOADING) {
            Common.Log(TAG, "onCreateDialog");
            ProgressDialog dlg = new ProgressDialog(this);
            dlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dlg.setMessage(getText(R.string.loading));
            dlg.setIndeterminate(true);
            dlg.setOnCancelListener(this);
            return dlg;
        }
        return null;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        Common.Log(TAG, "onCancel");
        //finish(); - IKCBSMMCPPRC-377 DUT should not exit from task manager when tap the loading screen twice. 
    }

    /* main thread handler message from refresh thread and main thread */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case INIT_THREAD:
                showDialog(DLG_LOADING);
                initRefreshThread();
                break;
            case DISMISS_DIALOG:
                try {
                    dismissDialog(DLG_LOADING);
                } catch (java.lang.IllegalArgumentException e) {
                    Common.Log(TAG, "exception IllegalArgumentException.");
                    //to fix bug DMQ.B-868, just ignore...
                }
                break;
            case REFRESH_LIST:
                List<ProcessAndTaskInfo> appList = (List<ProcessAndTaskInfo>) (msg.obj);
                if (null != appList)
                    mAppInfoAdapter.updateList(appList);
                if (mCheckedItemSet.isEmpty()){
                    mForceStopButton.setEnabled(false);
                }
                else {
                    mForceStopButton.setEnabled(true);
                }
                break;
            case REFRESH_TOAST:
                if (null != msg.obj)
                    Toast.makeText(ApplicationActivity.this,
                            (String) (msg.obj), Toast.LENGTH_SHORT).show();
                break;
            case KILL_SERVICE_DELAY:
                if (null != msg.obj)
                    Common.forceStopTaskByname(mactivityManager,
                            (String) msg.obj);

            default:
                break;
            }
        };
    };

    /* refresh thread */
    class ResourceLoaderThread extends Thread {
        volatile boolean abort = false;/* which used to abort thread */

        public void setAbort() {
            abort = true;
        }

        public void run() {
            if (abort) {
                return;
            }
            updateRunningTaskList();
            mHandler.sendEmptyMessage(DISMISS_DIALOG);
        }
    };

    /* run refresh thread */
    private void initRefreshThread() {
        if ((mResourceThread != null) && mResourceThread.isAlive()) {
            mResourceThread.setAbort();
        }
        mResourceThread = new ResourceLoaderThread();
        mResourceThread.start();
    }

    private void clearMessagesInHandler() {
        mHandler.removeMessages(INIT_THREAD);
        mHandler.removeMessages(REFRESH_LIST);
        mHandler.removeMessages(REFRESH_TOAST);
        mHandler.removeMessages(DISMISS_DIALOG);
        mHandler.removeMessages(KILL_SERVICE_DELAY);
    }

    /*
     * Method implementing functionality of buttons clicked
     * 
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    public void onClick(View v) {
        if (v == mForceStopButton) {
            for (int i = 0; i < mTaskInfoList.getAdapter().getCount(); i++) {
                /* get task info */
                ProcessAndTaskInfo tempTaskInfo = (ProcessAndTaskInfo) mTaskInfoList
                        .getAdapter().getItem(i);

                /* equal app name */
                if (!tempTaskInfo.checked) {
                    continue;
                }

                String packageName = tempTaskInfo.baseActivity.getPackageName();

                /* pick up special case of "fm radio" */
                if (packageName.equals(FMRADIO_PACKAGENAME)) {
                    handleEndFmradio();
                    continue;
                }

                Common.forceStopTaskByname(mactivityManager,
                        tempTaskInfo.baseActivity.getPackageName());

                if (!tempTaskInfo.baseActivity.getPackageName().equals(
                        tempTaskInfo.topActivity.getPackageName())
                        && !tempTaskInfo.isPersistent_TopActivity) {
                    Common.forceStopTaskByname(mactivityManager,
                            tempTaskInfo.topActivity.getPackageName());
                }
                /** special package */
                String gm_pachagename = "com.android.setupwizard";
                if (tempTaskInfo.topActivity.getPackageName().equals(
                        gm_pachagename)) {
                    String googleapps_packagename = "com.google.android.googleapps";
                    Common.forceStopTaskByname(mactivityManager,
                            googleapps_packagename);
                }
            }

            //clear all the checked item
            mCheckedItemSet.clear();
            mForceStopButton.setEnabled(false); //IKDOMINO-5119 "End" in Task Manager is not gray out when there is not any application selected.            
        } else if (v == mRefreshButton) {
            /* task manager wake up form activity stack */
            mHandler.sendEmptyMessage(INIT_THREAD);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
            ContextMenuInfo menuInfo) {
        Common.Log(TAG, "onCreateContextMenu");

        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Common.Log(TAG, "bad menuInfo" + e);
            return;
        }

        ProcessAndTaskInfo selectedTask = (ProcessAndTaskInfo) getListAdapter()
                .getItem(info.position);

        menu.setHeaderTitle(selectedTask.appname);

        menu.add(0, MENU_ITEM_SWITCH_TO, 0, getString(R.string.menuSwitch));
        if (!selectedTask.isPersistent) {
            menu.add(0, MENU_ITEM_END_TASK, 0, getString(R.string.menuEndTask));
        }
        menu.add(0, MENU_ITEM_CANCEL, 0, getString(R.string.menuCancel));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Common.Log(TAG, "bad menuInfo" + e);
            return false;
        }

        ProcessAndTaskInfo selectedTask = (ProcessAndTaskInfo) getListAdapter()
                .getItem(info.position);

        switch (item.getItemId()) {
        case MENU_ITEM_SWITCH_TO: {
            try {
                startActivity(selectedTask.currentIntent);
            } catch (Exception e) {
                //reuse REFRESH_TOAST to show exception message
                String errStr = e.toString();
                Message msg = mHandler.obtainMessage(REFRESH_TOAST);
                msg.obj = errStr;
                mHandler.sendMessage(msg);
            }
            return true;
        }
        case MENU_ITEM_END_TASK: {
            /* pick up special case of "fm radio" */
            if (selectedTask.baseActivity.getPackageName().equals(
                    FMRADIO_PACKAGENAME)) {
                handleEndFmradio();
                return true;
            }
            Common.forceStopTaskByname(mactivityManager,
                    selectedTask.baseActivity.getPackageName());
            if (!selectedTask.baseActivity.getPackageName().equals(
                    selectedTask.topActivity.getPackageName())
                    && !selectedTask.isPersistent_TopActivity) {
                Common.forceStopTaskByname(mactivityManager,
                        selectedTask.topActivity.getPackageName());
            }
            /** special package */
            String gm_pachagename = "com.android.setupwizard";
            if (selectedTask.topActivity.getPackageName()
                    .equals(gm_pachagename)) {
                String googleapps_packagename = "com.google.android.googleapps";
                Common.forceStopTaskByname(mactivityManager,
                        googleapps_packagename);
            }

            return true;
        }
        case MENU_ITEM_CANCEL: {
            break;
        }
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Receives notifications when applications are restarted.
     */
    private class packageIntentReceiver extends BroadcastReceiver {
        void registerReceiver() {
            IntentFilter filter = new IntentFilter(
                    Intent.ACTION_PACKAGE_RESTARTED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addDataScheme("package");
            ApplicationActivity.this.registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Common.Log(TAG, "Received intent broadcaster:" + intent.toString());
            /* init refresh thread */
            mHandler.sendEmptyMessage(INIT_THREAD);
        }
    }

    /* update task into */
    private void updateRunningTaskList() {
        Common.Log(TAG, "updateRunningTaskList");

        /* task info */
        List<ProcessAndTaskInfo> runningTasks = new ArrayList<ProcessAndTaskInfo>();
        createTaskInfoList(runningTasks);

        /* show memory info and task NO */
        int taskNO = runningTasks.size();
        displayMemoryAndTaskInfo(taskNO);

        /* show running task list */
        Message msg = mHandler.obtainMessage(REFRESH_LIST);
        msg.obj = runningTasks;
        mHandler.sendMessage(msg);

        /* print running service list */
        if (Common.log_flag)
            printRunningServiceList();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Common.Log(TAG, "OnListItemClick");
        ProcessAndTaskInfo tempTaskInfo = (ProcessAndTaskInfo) mTaskInfoList
                .getAdapter().getItem(position);
        /* click the Persistent item of list ,that does not work */
        if (tempTaskInfo.isPersistent) {
            mAppInfoAdapter.updateList();
            return;
        }
        /* change check of list item */
        tempTaskInfo.checked = !tempTaskInfo.checked;

        /*maintain the mCheckedItemSet*/
        if (tempTaskInfo.checked) {
            mCheckedItemSet.add(tempTaskInfo.packagename + tempTaskInfo.appname);
            mForceStopButton.setEnabled(true); //IKDOMINO-5119 "End" in Task Manager is not gray out when there is not any application selected.
        } else {
            mCheckedItemSet.remove(tempTaskInfo.packagename  + tempTaskInfo.appname);
            /* IKDOMINO-5119 "End" in Task Manager is not gray out when there is not any application selected. */
            if (mCheckedItemSet.isEmpty()){
               mForceStopButton.setEnabled(false);
            }
        }

        mAppInfoAdapter.updateList();

        return;
    }

    /* select all of checkbox */
    private void createTaskInfoList(List<ProcessAndTaskInfo> TaskInfoList) {
        /* dfb746, fix IKDOMINO-367, waiting 1000 msec for app quit cleanly*/
        try {
            Thread.sleep(1000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        HashMap<String, String> mAppName_PersistMap = new HashMap<String, String>();

        /* clear the list first */
        TaskInfoList.clear();
        /* get running task */
        List<ActivityManager.RunningTaskInfo> runningTasks = mactivityManager
                .getRunningTasks(100);
        List<ActivityManager.RecentTaskInfo> recentTasks = mactivityManager
                .getRecentTasks(100, ActivityManager.RECENT_WITH_EXCLUDED);
        ActivityManager.RunningTaskInfo curTask = null;

        mAppName_PersistMap = Common.createMapTableofApp(mPm, mactivityManager);

        HashSet<String> allRunningApps = new HashSet<String>();

        /* search process list */
        for (int i = 0; i < runningTasks.size(); i++) {
            /* current task */
            curTask = runningTasks.get(i);
            Intent currentIntent = null;
            String AppName = null;
            Drawable icon = null;
            ApplicationInfo info = null;

            //check if the Task is really running.
            if (curTask.numRunning == 0) {
                continue;
            }

            // system required special permission for switch to InCallScreen, ignore it
            if (curTask.baseActivity.getClassName().equals("com.android.phone.InCallScreen")) {
                continue;
            }

            // IKDOMINO-1379, ignore com.android.contacts.ui.QuickContactActivity
            if (curTask.baseActivity.getClassName().equals("com.android.contacts.ui.QuickContactActivity")) {
                continue;
            }
            // IKDOMINO-1379 end

            // IKDOMINO-1393/1394, system required special permission for switch to UsbStorageActivity, ignore it
            if (curTask.baseActivity.getClassName().contains("UsbStorageActivity")) {
                continue;
            }
            // IKDOMINO-1393/1394 end

            /* get current intent from recent task info */
            if ( (recentTasks.size() != 0) &&
                 /*	IKDOMINO-1079, don't get Intent from recent task for MotoHome Activity,
                    which may include extras */
                 (!curTask.baseActivity.getClassName().equals(MOTO_HOME_CLASS)) ) {
                currentIntent = getRecentIntentOfRunningTask(curTask);
            }

            /* get current intent from its activity */
            if (null == currentIntent) {
                currentIntent = getSelfIntentOfRunningTask(curTask);
            }

            /* check if get intent success */
            if (null == currentIntent)
                continue;

            /* get application name and icon */
            ResolveInfo resolveInfo = mPm.resolveActivity(currentIntent, 0);
            if (resolveInfo != null) {
                ActivityInfo activityInfo = resolveInfo.activityInfo;
                AppName = activityInfo.loadLabel(mPm).toString();
                icon = activityInfo.loadIcon(mPm);
            }

            /* get current application info */
            try {
                info = mPm.getApplicationInfo(curTask.baseActivity
                        .getPackageName(), 0);
            } catch (NameNotFoundException e) {
                AppName = curTask.baseActivity.getPackageName();
            }
            
            /* fix for klocwork of titanium */
            if (null == info)
                continue;
            
            /**modify for SWITCHUITWOV-157 by bphx43 2012-09-11*/
            if (null == resolveInfo || AppName == null || "".equals(AppName)) {
            /** end by bphx43*/
                AppName = info.loadLabel(mPm).toString();
                icon = info.loadIcon(mPm);
            }

            /* set value of list item */
            ProcessAndTaskInfo temp_TakInfo = new ProcessAndTaskInfo();
            temp_TakInfo.currentIntent = currentIntent;
            temp_TakInfo.appname = AppName;
            temp_TakInfo.appicon = icon;
            temp_TakInfo.baseActivity = curTask.baseActivity;
            temp_TakInfo.topActivity = curTask.topActivity;
            temp_TakInfo.tid = curTask.id;
            temp_TakInfo.packagename = curTask.baseActivity.getPackageName();
            temp_TakInfo.checked = mCheckedItemSet.contains(temp_TakInfo.packagename + temp_TakInfo.appname);
            temp_TakInfo.isPersistent = Common.isPersistentRuningTask(mPm,
                    info, mAppName_PersistMap);
            // added by amt_sunli 2012-12-24 SWITCHUITWO-329 begin
            // only for CT remote setting
            if (Common.getMOLCTModel()) {
                if (temp_TakInfo.packagename != null && temp_TakInfo.packagename.equals(SETTING)) {
                    ComponentName mComponentName = new ComponentName(SETTING, REMOTESETTING);
                    if (temp_TakInfo.topActivity != null && temp_TakInfo.topActivity.equals(mComponentName)) {
                        temp_TakInfo.isPersistent = true;
                    }
                }
            }
            // added by amt_sunli 2012-12-24 SWITCHUITWO-329 end
            Common.Log(TAG, "temp_TakInfo = " + temp_TakInfo);

            /* top activity persist */
            if (!temp_TakInfo.baseActivity.getPackageName().equals(
                    temp_TakInfo.topActivity.getPackageName())) {
                try {
                    info = mPm.getApplicationInfo(temp_TakInfo.topActivity
                            .getPackageName(), 0);
                    AppName = info.loadLabel(mPm).toString();
                    temp_TakInfo.isPersistent_TopActivity = Common
                            .isPersistentRuningTask(mPm, info,
                                    mAppName_PersistMap);
                } catch (NameNotFoundException e) {
                    temp_TakInfo.isPersistent_TopActivity = true;
                }
            }
            temp_TakInfo.checkvisibility = true;
                        
            // tbrn43 4.10.2012 IKCBSMMCPPRC-762 begin
            if(!allRunningApps.contains(temp_TakInfo.packagename + temp_TakInfo.appname)){
            	TaskInfoList.add(temp_TakInfo);
            	allRunningApps.add(temp_TakInfo.packagename + temp_TakInfo.appname);
            }
            // tbrn43 4.10.2012 IKCBSMMCPPRC-762 end
        }
        /* sort display name */
        Comparator<ProcessAndTaskInfo> comp = new DisplayNameCompare();
        Collections.sort(TaskInfoList, comp);

        /* remove those checked item which not running now */
        Vector<String> obseleteItemVec = new Vector<String>();
        Iterator<String> ir = mCheckedItemSet.iterator();
        while(ir.hasNext()) {
            // String val = new String(ir.next());
            String val = ir.next();
            if (!allRunningApps.contains(val)) {
                obseleteItemVec.add(val);
            }
        }

        for (int i=0; i<obseleteItemVec.size(); ++i) {
            mCheckedItemSet.remove(obseleteItemVec.elementAt(i));
        }
    }

    private void displayMemoryAndTaskInfo(int taskNO) {
        /* memory info and task NO */
        long availMem = Common.getFreeMemory(mactivityManager).availMem >> 20;
        String memory_string = getString(R.string.textAvailableMemory) + " "
                + Long.toString(availMem)
                + getString(R.string.textAvailableMemoryUnit) + "\n"
                + getString(R.string.textNumOfApplications) + " " + taskNO;

        Message msg = mHandler.obtainMessage(REFRESH_TOAST);
        msg.obj = memory_string;
        mHandler.sendMessage(msg);

    }

    /* print Process Info List */
    private void printRunningServiceList() {
        List<ActivityManager.RunningServiceInfo> runningServiceList = mactivityManager
                .getRunningServices(100);
        if (null == runningServiceList)
            return;

        for (int i = 0; i < runningServiceList.size(); i++) {
            Common.Log(TAG, "Service packagename " + Integer.toString(i)
                    + " = "
                    + runningServiceList.get(i).service.getPackageName());
            Common.Log(TAG, "Service preocess = "
                    + runningServiceList.get(i).process);
            Common.Log(TAG, "Service pid = "
                    + Integer.toString(runningServiceList.get(i).pid));
        }
    }

    private Intent getRecentIntentOfRunningTask(
            ActivityManager.RunningTaskInfo runningTask) {
        List<ActivityManager.RecentTaskInfo> recentTasks = mactivityManager
                .getRecentTasks(100, ActivityManager.RECENT_WITH_EXCLUDED);
        Intent taskintent = null;
        int numTasks = recentTasks.size();
        int i = 0;

        /* get the corresponding intent from recent task info */
        ActivityManager.RecentTaskInfo taskFound = null;
        for (i = 0; i < numTasks; i++) {
            ActivityManager.RecentTaskInfo info = recentTasks.get(i);

            if (runningTask.id == info.id) {
                taskFound = info;
                break;
            }
        }

        /* get the intent from recent task */
        if (taskFound != null) {
            taskintent = new Intent(taskFound.baseIntent);
            if (taskFound.origActivity != null) {
                taskintent.setComponent(taskFound.origActivity);
            }

            taskintent.setFlags((taskintent.getFlags() & ~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                                | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);

            return taskintent;
        }

        if (!runningTask.baseActivity.getClassName().equals("com.android.phone.InCallScreen"))
            return null;

        /* special example for calling ,will search recent task info list again */
        for (i = 0; i < numTasks; i++) {
            ActivityManager.RecentTaskInfo info = recentTasks.get(i);

            if (info.baseIntent.getComponent().getPackageName().equals(
                    "com.android.contacts")
                    && info.baseIntent.getComponent().getClassName().equals(
                            "com.android.contacts.DialtactsActivity")) {
                taskintent = new Intent(info.baseIntent);

                if (info.origActivity != null) {
                    taskintent.setComponent(info.origActivity);
                }
                taskintent.setFlags((taskintent.getFlags() & ~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                                    | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
                break;
            }
        }
        /* get successful */
        if (i < numTasks)
            return taskintent;
        return null;
    }

    /* launch task if launch task not from recent task list */
    private Intent getSelfIntentOfRunningTask(
            ActivityManager.RunningTaskInfo runningTask) {
        Intent mainIntent = new Intent();

        /* define intent */
        mainIntent.setComponent(runningTask.baseActivity);
        List<ResolveInfo> apps = mPm.queryIntentActivities(mainIntent, 0);
        /* check if package is in list */
        if (0 == apps.size())
            return null;

        /* create component name except for "com.android.phone.InCallScreen" */
        if (!runningTask.baseActivity.getPackageName().equals(
                "com.android.phone")) {
            mainIntent.setAction(Intent.ACTION_MAIN);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                                Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
            return mainIntent;
        }
        return null;
    }

    private void printRunningAppProcesses() {
        // processes
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = mactivityManager
                .getRunningAppProcesses();
        int length = runningProcesses.size();
        Common.Log(TAG, "=========totoal processes NO:" + length);
        for (int i = 0; i < length; i++) {
            RunningAppProcessInfo task = runningProcesses.get(i);
            Common.Log(TAG, "\nRunningAppProcessInfo: pid: " + task.pid
                    + "\n  processName:" + task.processName + "\n  importance:"
                    + task.importance + "\n  lru:" + task.lru);
            ApplicationInfo info = null;
            try {
                info = mPm.getApplicationInfo(task.pkgList[0],
                        PackageManager.GET_ACTIVITIES);
                Common.Log(TAG, "  className:" + info.className + "\n  name:"
                        + info.loadLabel(mPm));
                Common.Log(TAG, "  uid:" + Integer.toString(info.uid));

            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            for (int j = 0; j < task.pkgList.length; j++)
                Common.Log(TAG, "  pkgList:" + task.pkgList[j]);
        }
    }

    /** special case of Fm radio */
    private void handleEndFmradio() {
        Common.Log(TAG, "handleEndFmradio");
        int i = 0;
        List<ActivityManager.RunningServiceInfo> runningServiceList = mactivityManager
                .getRunningServices(100);
        if (null == runningServiceList) {
            return;
        }
        /* check if fmradio service is running */
        for (i = 0; i < runningServiceList.size(); i++) {
            if (runningServiceList.get(i).service.getPackageName().equals(
                    FMRADIO_PACKAGENAME)) {
                break;
            }
        }

        /*
         * if fmradio service is running,kill the service first ,and restart
         * package after 100ms
         */
        if (runningServiceList.size() != 0 && i < runningServiceList.size()) {
            Intent serviceIntent = new Intent();
            serviceIntent.setComponent(runningServiceList.get(i).service);
            boolean stopService = stopService(serviceIntent);

            Message msg = mHandler.obtainMessage(KILL_SERVICE_DELAY);
            msg.obj = FMRADIO_PACKAGENAME;
            mHandler.sendMessageDelayed(msg, KILL_SERVICE_TIMEOUT);
            return;
        }

        /* if fmradio service is not running,restart package right now */
        Common.forceStopTaskByname(mactivityManager, FMRADIO_PACKAGENAME);

        /* return */
        return;
    }
}
