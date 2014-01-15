/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *                             Modification     Tracking
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * gkp864                      07/06/2012                   Initial release
 */

package com.motorola.mmsp.performancemaster.ui;

import android.app.ActivityManager;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;

import android.os.Debug;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StatFs;
import android.util.Log;
import android.util.SparseArray;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.motorola.mmsp.performancemaster.engine.SysPropEngine;
import com.motorola.mmsp.performancemaster.R;

public class MemoryCleanerService extends Service {

    public static final String EXTRA_WIDGET_IDS = "widget_ids";

    public static final String TAG = "MemoryCleanerService";

    public static final String START_SERVICE_CLEAN = "START_SERVICE_CLEAN";

    public static final String RESTAART_SERVICE = "com.motorola.memorywidget.service.RESTART";

    private static final int MAX_SERVICES = 100;

    private Toast mToast = null;

    private ActivityManager mAm = null;

    private PackageManager mPm = null;

    private long mRamTotal = 0;

    private int mRomFree = 0;

    private int mRomTotal = 0;

    private int mRamPercent = 0;

    private int mRomPercent = 0;

    private int ReleaseRamFree = 0;

    private boolean mTimeRunning = false;

    private RemoteViews mRemoteViews;

    final SparseArray<ProcessItem> mServiceProcessesByPid = new SparseArray<ProcessItem>();

    private MemoryReceiver mMemoryReceiver = new MemoryReceiver();

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private class MemoryReceiver extends BroadcastReceiver {
        private ComponentName TopActivity = null;

        private List<String> LancherName = null;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {

                Log.i(TAG, "ACTION_TIME_TICK");

                TopActivity = getTopActivity();
                LancherName = getLauncherList();
                for (int i = 0; i < LancherName.size(); i++) {
                    if (TopActivity.getPackageName().equals(LancherName.get(i))) {

                        updateView();
                        Log.i(TAG, "TopActivity=" + TopActivity.getPackageName());

                    }
                }

            }
        }
    }

    @Override
    public void onCreate() {
        SysPropEngine.setContext(getApplicationContext());
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        this.registerReceiver(mMemoryReceiver, filter);
        Notification notification = new Notification();
        startForeground(1, notification);

        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {

        // sometimes, intent is null
        // eg. when this service is killed and re-start again
        if (intent != null) {
            String action = intent.getAction();
            Log.i(TAG, "!!!!!!onStart!!!!!!!" + action);
            if (action.equals(MemoryCleanWidget.ACTION_UPDATE_WIDGET)) {
                Log.i(TAG, "ACTION_UPDATE_WIDGET");
                mTimeRunning = true;
                updateView();
                mTimeRunning = false;
            } else if (action.equals(START_SERVICE_CLEAN)) {

                Log.i(TAG, "START_SERVICE_CLEAN");

                if (mTimeRunning == false) {
                    ClearMemoryAndCache();
                    pushWidgetManager();
                    /****
                     * mUpdateMemoryTimer = new Timer(); mTimeMemoryTask = new
                     * TimerTask() { public void run() {
                     * mUpdateMemoryTimer.cancel(); mTimeMemoryTask = null; } };
                     * if (mTimeMemoryTask != null) {
                     * mUpdateMemoryTimer.schedule(this.mTimeMemoryTask, 0,
                     * 100); }
                     ****/

                    showMemoryStaus();
                } else {
                    updateView();
                    showMemoryStaus();
                }
            } else if (action.equals(RESTAART_SERVICE)) {
                updateView();
                Log.i(TAG, "RESTAART_SERVICE");

            }
        }

        super.onStart(intent, startId);
    }

    public RemoteViews getRemoteViews(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.memory_appwidget);
        /****
         * Intent mintent = new Intent(this, MemoryCleanerService.class);
         * mintent.setAction(START_SERVICE_CLEAN); PendingIntent pendingIntent =
         * PendingIntent.getService(this, 0, mintent, 0);
         * views.setOnClickPendingIntent(R.id.first_circle, pendingIntent);
         *****/

        Intent memAnim = new Intent(context, MemoryAnimateActivity.class);
        PendingIntent memAnimPending = PendingIntent.getActivity(context, 0, memAnim, 0);
        views.setOnClickPendingIntent(R.id.first_circle, memAnimPending);

        ComponentName myComponentName = new ComponentName(context, MemoryCleanWidget.class);
        Log.i(TAG, "myComponentName" + myComponentName);

        AppWidgetManager myAppWidgetManager = AppWidgetManager.getInstance(this);
        myAppWidgetManager.updateAppWidget(myComponentName, views);

        return views;
    }

    public void pushWidgetManager() {

        ComponentName myComponentName = new ComponentName(this, MemoryCleanWidget.class);
        Log.i(TAG, "myComponentName" + myComponentName);

        AppWidgetManager myAppWidgetManager = AppWidgetManager.getInstance(this);
        myAppWidgetManager.updateAppWidget(myComponentName, mRemoteViews);

    }

    private int getFreeMemory() {

        final Context mApp;
        ActivityManager.MemoryInfo moutInfo = new ActivityManager.MemoryInfo();
        mApp = this.getApplicationContext();
        ActivityManager TempAm = (ActivityManager) mApp.getSystemService(Context.ACTIVITY_SERVICE);

        TempAm.getMemoryInfo(moutInfo);

        Long TempMemory = moutInfo.availMem;

        Log.i(TAG, "Function getFreeMemory mTemp=" + (TempMemory >> 20) + "mb");
        int TempFree = (int) (TempMemory >> 20);
        return TempFree;
    }

    private void updateView() {
        mRemoteViews = getRemoteViews(this);
        MemoryCleanWidget.mRamFree = (int) getFreeMemory();

        Log.i(TAG, "MemoryTotal=" + (SysPropEngine.getInstance().getMemInfo().getRamTotal() >> 10)
                + "mb");

        ReleaseRamFree = 0;
        CalcMemoryAndCache();
        pushWidgetManager();

    }

    private long[] getInternalStorageInfo() {
        return getStorageInfo(Environment.getDataDirectory());
    }

    private long[] getStorageInfo(File path) {
        long[] values = new long[] {
                0, 0
        };
        if (path != null) {
            try {
                StatFs stat = new StatFs(path.getAbsolutePath());
                long blockSize = stat.getBlockSize();

                values[0] = stat.getBlockCount() * blockSize / 1024;
                values[1] = stat.getAvailableBlocks() * blockSize / 1024;
            } catch (Exception e) {
                Log.e(TAG, "Cannot access path: " + path.getAbsolutePath());
            }
        }

        return values;
    }

    private ComponentName getTopActivity() {

        ActivityManager am = (ActivityManager) this.getApplicationContext().getSystemService(
                Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;

        Log.i(TAG, "!!!!!!!!!!!!!!getTopActivity+TopTask=" + cn.getPackageName());

        return cn;
    }

    private List<String> getLauncherList() {
        List<String> LauncherName = new ArrayList<String>();
        PackageManager packageManager = this.getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo ri : resolveInfo) {
            LauncherName.add(ri.activityInfo.packageName);

            Log.i(TAG, "getLauncherList" + ri.activityInfo.packageName);
        }

        return LauncherName;
    }

    class ProcessItem {

        final int mUid;

        final String mProcessName;

        int mPid;

        public ProcessItem(int uid, String processName, int pid) {
            mUid = uid;
            mProcessName = processName;
            mPid = pid;
        }
    }

    private void ClearMemoryAndCache() {

        final Context mApplicationContext;
        List<String> TempLaunch = null;

        Debug.MemoryInfo[] mem = null;

        mApplicationContext = this.getApplicationContext();
        mAm = (ActivityManager) mApplicationContext.getSystemService(Context.ACTIVITY_SERVICE);
        mPm = mApplicationContext.getPackageManager();
        List<ActivityManager.RunningServiceInfo> services = mAm.getRunningServices(MAX_SERVICES);

        ReleaseRamFree = 0;
        MemoryCleanWidget.mRamFree = this.getFreeMemory();
        int mNumService = services != null ? services.size() : 0;
        Log.i(TAG, "mNumService=" + mNumService);
        for (int i = 0; i < mNumService; i++) {
            ActivityManager.RunningServiceInfo si = services.get(i);
            ProcessItem proc = new ProcessItem(si.uid, si.process, si.pid);

	    Log.i("GKP864_SERVICE", "pi_processname=" + proc.mProcessName
					+ "si.pid=" + si.pid + "!!!si_service=" + si.service);
            if (!si.started && si.clientLabel == 0) {
				continue;
	    }
			// We likewise don't care about services running in a
			// persistent process like the system or phone.
	   if ((si.flags & ActivityManager.RunningServiceInfo.FLAG_PERSISTENT_PROCESS) != 0) {
		continue;
           }

            if (si.pid != 0) {
                mServiceProcessesByPid.put(proc.mPid, proc);
            }
        }

        List<ActivityManager.RunningAppProcessInfo> processes = mAm.getRunningAppProcesses();
        int mNumProcess = processes != null ? processes.size() : 0;
        TempLaunch = getLauncherList();
        for (int i = 0; i < mNumProcess; i++) {
            ActivityManager.RunningAppProcessInfo pi = processes.get(i);
            ProcessItem proc = mServiceProcessesByPid.get(pi.pid);
           /**** for (String pkg1 : pi.pkgList) {
				Log.i("GKP864_PROCESS", "pi.pkg" + " " + "" + i + "=" + pkg1
						+ "pi.pid=" + pi.pid);
			}****/
            if (proc == null) {
                if(true) {
                    if ((pi.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND)) {
                        int[] TempPid = new int[1];
                        TempPid[0] = pi.pid;
                        mem = mAm.getProcessMemoryInfo(TempPid);
                        ReleaseRamFree += mem[0].getTotalPss() * 1024;
                        Log.i(TAG, "packagename " + i + "=" + pi.processName
                                + "    proc.mRunningProcessInfo.importance=" + pi.importance);
                        if (!pi.processName.equals("com.motorola.mmsp.performancemaster")) {
                            for (String pkg : pi.pkgList) {
                                if (pkg != null) {
                                    for (int j = 0; j < TempLaunch.size(); j++) {

                                        if (!pkg.equals(TempLaunch.get(j))) {

                                            Log.i(TAG, "@@@@@@@@@@@@@@@@KillProcess pkg = " + pkg);
                                            mAm.killBackgroundProcesses(pkg);
                                        }
                                    }

                                }
                            }
                        }
                    }

                }


            }
        }
        ReleaseRamFree = ReleaseRamFree >> 20;
        clearAllCaches();

        CalcMemoryAndCache();

    }

    private void CalcMemoryAndCache() {

        this.mRamTotal = (SysPropEngine.getInstance().getMemInfo().getRamTotal() >> 10);
        Log.i(TAG, "MemoryTotal=" + (SysPropEngine.getInstance().getMemInfo().getRamTotal() >> 10));
        if (mRamTotal <= 0) {
            return;
        }

        int TempCount = MemoryCleanWidget.mRamFree + ReleaseRamFree;
        Log.i(TAG, "mRamFree=====" + MemoryCleanWidget.mRamFree + "MB");

        MemoryCleanWidget.mRamFree = TempCount;
        this.mRamPercent = (int) (100 - (TempCount * 100) / this.mRamTotal);

        long[] mTempRom = getInternalStorageInfo();
        this.mRomTotal = (int) (mTempRom[0] >> 10);
        this.mRomFree = (int) (mTempRom[1] >> 10);

        if (mRomTotal <= 0) {
            return;
        }

        this.mRomPercent = (int) (100 - mTempRom[1] * 100 / mTempRom[0]);

        if (mRemoteViews == null) {
            mRemoteViews = this.getRemoteViews(this);
        }
        mRemoteViews.setTextViewText(R.id.percnet, (new Integer(mRamPercent)).toString());

        Log.i(TAG, "TempCount=" + TempCount + "MB");
        Log.i(TAG, "mRamPercent=" + mRamPercent + "%");
        Log.i(TAG, "ReleaseRamFree=" + ReleaseRamFree + "MB");
        Log.i(TAG, "mRomPercent=" + mRomPercent + "mRomTotal=" + (mRomTotal) + "MB" + "mRomFree="
                + (mRomFree) + "MB");
    }

    private long getEnvironmentSize() {
        File localFile = Environment.getDataDirectory();
        long TempSize;
        if (localFile == null)
            TempSize = 0L;
        while (true) {

            String str = localFile.getPath();
            StatFs localStatFs = new StatFs(str);
            long BlockSize = localStatFs.getBlockSize();
            TempSize = localStatFs.getBlockCount() * BlockSize;
            return TempSize;
        }
    }

    /**
     * clear all application Cache
     **/
    private void clearAllCaches() {
        try {
            Method localMethod = mPm.getClass().getMethod("freeStorageAndNotify", Long.TYPE,
                    IPackageDataObserver.class);
            Long localLong = Long.valueOf(getEnvironmentSize() - 1L);
            Object[] arrayOfObject = new Object[2];
            arrayOfObject[0] = localLong;
            Log.i(TAG, " localLong = " + localLong);
            localMethod.invoke(mPm, localLong, new IPackageDataObserver.Stub() {
                @Override
                public void onRemoveCompleted(String packageName, boolean succeeded)
                        throws RemoteException {
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, " SecurityException");
            e.printStackTrace();
        } catch (NoSuchMethodException e) {

            Log.e(TAG, " NoSuchMethodException");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, " IllegalArgumentException");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(TAG, " IllegalAccessException");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.e(TAG, " InvocationTargetException");
            e.printStackTrace();
        }
    }

    public void showMemoryStaus() {
        CharSequence relaseMemoryStatus = null;
        final Resources res = this.getResources();

        if (this.ReleaseRamFree == 0) {

            relaseMemoryStatus = res.getText(R.string.MemoryCleaned);

        } else if (this.ReleaseRamFree > 0) {
            relaseMemoryStatus = res.getText(R.string.ReleaseMemory) + " "
                    + (new Integer(ReleaseRamFree)).toString() + " M";
        }

        if (mToast == null) {
            mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        }
        ReleaseRamFree = 0;
        mToast.setText(relaseMemoryStatus);
        mToast.show();
    }

    @Override
    public void onDestroy() {
        Intent operator = new Intent(RESTAART_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, operator,
                PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.RTC, System.currentTimeMillis() + 60 * 1000, pendingIntent);
        Log.i(TAG, "Destory MemoryCleanerService");
        if (mMemoryReceiver != null) {
            this.unregisterReceiver(mMemoryReceiver);
        }

    }

}
