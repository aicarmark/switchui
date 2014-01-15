package com.motorola.mmsp.taskmanager;

import java.util.HashMap;
import java.util.List;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

class Common {
    private static final String TAG = "Common";
    final static String TaskName = "com.motorola.mmsp.taskmanager";
    final static String FoTa = "com.motorola.fota";
    final static String Android_keyboard = "com.android.inputmethod.latin";
    final static String PHONE = "com.android.contacts";
    final static String MESSAGE = "com.android.mms";
    final static boolean log_flag = true;

    public static void Log(String TAG, String printinfo) {
	if (log_flag)
	    Log.v(TAG, printinfo);
    }

    public static boolean isPersistentRuningTask(PackageManager manager,
	    ApplicationInfo info, HashMap<String, String> mAppName_PersistMap) {
	int i = 0;
	String appName = null;
	ApplicationInfo appInfoOfProcess = null;

	/* check if the application is persist */
	if ((info.flags & ApplicationInfo.FLAG_PERSISTENT) == ApplicationInfo.FLAG_PERSISTENT) {
            Log(TAG, "Task Manager can't kill PackageName = " + info.packageName);
	    return true;
	}

	/* check if the process of application is persist */
	appName = info.loadLabel(manager).toString();
	Log(TAG, "Task Manager App name = " + appName + " Package name="
		+ info.packageName + " process name = " + info.processName);
	if (Boolean.parseBoolean(mAppName_PersistMap.get(appName))) {
            Log(TAG, "Task Manager can't kill PackageName = " + info.packageName);
	    return true;
	}

	/*
	 * if package name is not process name,we need to check if process is
	 * persist
	 */
	if (!info.packageName.equals(info.processName)) {
	    try {
		appInfoOfProcess = manager.getApplicationInfo(info.processName,
			0);
		appName = appInfoOfProcess.loadLabel(manager).toString();
		if (Boolean.parseBoolean(mAppName_PersistMap.get(appName))) {
                    Log(TAG, "Task Manager can't kill PackageName = "
			    + info.packageName);
		    return true;
		}
	    } catch (NameNotFoundException e) {
	    }
	}

	/* Android keyboard can't be killed */
	if (Android_keyboard.equals(info.packageName)) {
            Log(TAG, "Task Manager can't kill PackageName = " + info.packageName);
	    return true;
	}

	/* fota can't be killed */
	if (FoTa.equals(info.packageName)) {
            Log(TAG, "Task Manager can't kill PackageName = " + info.packageName);
	    return true;
	}

	/* can't kill self */
	if (TaskName.equals(info.packageName)) {
            Log(TAG, "Task Manager can't kill PackageName = " + info.packageName);
	    return true;
	}
	
	/* phone can't be killed */
        if (PHONE.equals(info.packageName)) {
            Log(TAG, "Task Manager can't kill PackageName = " + info.packageName);
            return true;
        }
    
        /* message can't be killed */
        if (MESSAGE.equals(info.packageName)) {
            Log(TAG, "Task Manager can't kill PackageName = " + info.packageName);
            return true;
        }

	/* home which we need not be killed */
	Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
	mainIntent.addCategory(Intent.CATEGORY_HOME);
	List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
	if (null == apps) {
	    return false;
	}
	/* check if package is in list */
	for (i = 0; i < apps.size(); i++) {
	    if (apps.get(i).activityInfo.packageName.equals(info.packageName)) {
		Log(TAG, "home task " + info.packageName);
		return true;
	    }
	}	
    
        return false;
    }

    /* Force stop according package name */
    public static void forceStopTaskByname(ActivityManager activityManager,
	    String packagename) {
	Log(TAG, "Force stop  activity: " + packagename);
	activityManager.forceStopPackage(packagename);
    }

    /* get free memory */
    public static ActivityManager.MemoryInfo getFreeMemory(
	    ActivityManager activityManager) {
	ActivityManager.MemoryInfo outInfo = new ActivityManager.MemoryInfo();
	/* get memoryInfo */
	activityManager.getMemoryInfo(outInfo);
	return outInfo;
    }

    /* Kill Process */
    public static void killProcess(ActivityManager activityManager,
	    ProcessAndTaskInfo PackageInfo) {
	/* check the input parameter */
	if (null == PackageInfo)
	    return;

	Log(TAG, "kill Process name " + PackageInfo.packagename);
	Log(TAG, "kill Process importance "
		+ Integer.toString(PackageInfo.importance));

	activityManager.forceStopPackage(PackageInfo.packagename);
    }

    /* check if application is in list */
    public static boolean isAppInList(
	    List<ProcessAndTaskInfo> PackPid_Map_List, String appname) {
	int i = 0;

	/* check the input parameter */
	if (null == PackPid_Map_List || null == appname)
	    return false;

	for (i = 0; i < PackPid_Map_List.size(); i++) {
	    if (PackPid_Map_List.get(i).appname.equals(appname))
		break;
	}

	if (i >= PackPid_Map_List.size())
	    return false;
	else {
	    Common.Log(TAG, "Applicatioan is in the list " + appname);
	    return true;
	}
    }

    /* create the map of application and persist flag */
    public static HashMap<String, String> createMapTableofApp(
	    PackageManager mPm, ActivityManager mactivityManager) {

	List<ActivityManager.RunningAppProcessInfo> ProcessList = mactivityManager
		.getRunningAppProcesses();
	HashMap<String, String> mAppName_PersistMap = new HashMap<String, String>();
	String appName = null;
	ApplicationInfo info = null;
	boolean pid_persist_flag = false;

	/*
	 * search process list and modify mPid_PersistMap mAppName_PersistMap
	 * according to flag of application
	 */
	for (int i = 0; i < ProcessList.size(); i++) {
	    pid_persist_flag = false;
	    /* find out the persist process */
	    try {
		info = mPm
			.getApplicationInfo(ProcessList.get(i).processName, 0);
	    } catch (NameNotFoundException e) {
		continue;
	    }

            if (info== null) {
                    Log(TAG, "error: application info not found for process: " + ProcessList.get(i).processName);
                    continue;
            }
	    /* check process */
	    if ((info.flags & ApplicationInfo.FLAG_PERSISTENT) == ApplicationInfo.FLAG_PERSISTENT) {
		pid_persist_flag = true;
	    } else {
		/* search package list every process */
		for (int j = 0; j < ProcessList.get(i).pkgList.length; j++) {
		    try {
			info = mPm.getApplicationInfo(
				ProcessList.get(i).pkgList[j], 0);
		    } catch (NameNotFoundException e) {
			continue;
		    }

                    if (info== null) {
                        Log(TAG, "error: application not found for package: " + ProcessList.get(i).pkgList[j]);
                            continue;
                    }
		    /* the application is persist */
		    if ((info.flags & ApplicationInfo.FLAG_PERSISTENT) == ApplicationInfo.FLAG_PERSISTENT) {
			pid_persist_flag = true;
			break;
		    } else {
			/* get app name */
			appName = info.loadLabel(mPm).toString();
			mAppName_PersistMap.put(appName, Boolean
				.toString(false));
		    }
		}
	    }

	    /*
	     * if persist process,the package list of this process are all
	     * persist
	     */
	    if (pid_persist_flag) {
		for (int k = 0; k < ProcessList.get(i).pkgList.length; k++) {
		    try {
			info = mPm.getApplicationInfo(
				ProcessList.get(i).pkgList[k], 0);
		    } catch (NameNotFoundException e) {
			continue;
		    }
		    /* push the map of app name and persist flag */
		    appName = info.loadLabel(mPm).toString();
		    mAppName_PersistMap.put(appName, Boolean.toString(true));
		}
	    }
            
	}
	return mAppName_PersistMap;
    }
    // added by amt_sunli 2012-12-24 SWITCHUITWO-329 begin
    private static boolean isMOLCTModel = false;

    public static void setMOLCTModel() {
        Common.isMOLCTModel = isMOLCTModel();
    }

    public static boolean getMOLCTModel() {
        return Common.isMOLCTModel;
    }

    private static boolean isMOLCTModel() {
        boolean result = false;
        String name = Build.MODEL;
        if (!TextUtils.isEmpty(name)) {
            Log.d(TAG, "MODEL = " + name);
            result = name.trim().contains("XT788");//CT model name
        }
        Log.d(TAG, "isMOLCTModel:" + result);
        return result;
    }
    // added by amt_sunli 2012-12-24 SWITCHUITWO-329 end
}
