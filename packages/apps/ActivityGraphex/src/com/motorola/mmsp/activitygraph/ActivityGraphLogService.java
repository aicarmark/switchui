package com.motorola.mmsp.activitygraph;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.motorola.mmsp.activitygraph.ActivityGraphModel.Callbacks;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;

public class ActivityGraphLogService extends Service implements Runnable {
    private final static String LOG_ACTION = "com.motorola.mmsp.activitygraph.COUNT_APP";
    private final static String TAG = "ActivityGraphLogService";
    private boolean isObserverLog = false;
    private StringBuffer logContent = null;
    private Bundle mBundle = null;
    private Intent mIntent = null;
    private static ArrayList<String> nameList = new ArrayList<String>();
    // Modifyed by amt_chenjing for switchui-2505 20120731 begin
    //private static final String PROCESS_NAME = "android.process.activitygraph";
    private static final String PROCESS_NAME = "com.motorola.mmsp.activitygraph";
    // Ended by amt_chenjing
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        setAppResolveInfo();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        filter.addDataScheme("package");
        this.registerReceiver(mReceiver, filter);

        mIntent = new Intent();
        mBundle = new Bundle();
        logContent = new StringBuffer();
        startLogObserver();
        super.onCreate();
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "onReceive : " + intent.getAction());
            final String action = intent.getAction();
            if (action == null) {
                return;
            }
            if (Intent.ACTION_PACKAGE_ADDED.equals(action)
                    || Intent.ACTION_PACKAGE_CHANGED.equals(action)
                    || Intent.ACTION_PACKAGE_REMOVED.equals(action)
                    || Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE
                            .equals(action)
                    || Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE
                            .equals(action)) {

                setAppResolveInfo();

            }

        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        // mIntent = new Intent();
        // mBundle = new Bundle();
        // logContent = new StringBuffer();
        // setAppResolveInfo();
        // startLogObserver();
        return super.onStartCommand(intent, flags, startId);
    }

    private void startLogObserver() {
        Intent intent = new Intent();
        intent.setAction("com.motorola.mmsp.activitywidget2d.action.databaseChanged");
        sendBroadcast(intent);
        
        Log.d(TAG, "startLogObserver");
        isObserverLog = true;
        Thread mTherad = new Thread(this);
        mTherad.start();
    }

    private void stopLogObserver() {
        isObserverLog = false;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        stopLogObserver();
        this.unregisterReceiver(mReceiver);
    }

    private void sendLogContent(String logContent) {
        Log.e(TAG, "sendLogContent: " + logContent);
        mBundle.putString("appname", logContent);
        mIntent.putExtras(mBundle);
        mIntent.setAction(ActivityGraphLogService.LOG_ACTION);
        sendBroadcast(mIntent);
    }

    private void setAppResolveInfo() {
        Log.d(TAG, "set app resolveInfo");
        nameList.clear();

        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);

        PackageManager pm = this.getPackageManager();
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
            nameList.add(appname);
        }
        Log.d(TAG, "set app resolveInfo------end");
    }

    class ProcessInfo {
        public String user;
        public String pid;
        public String ppid;
        public String name;

        @Override
        public String toString() {
            String str = "user=" + user + " pid=" + pid + " ppid=" + ppid
                    + " name=" + name;
            return str;
        }
    }

    // kill logcat processes
    private void killLogcatProc(List<ProcessInfo> allProcList) {
        Log.d(TAG, "--------killLogcatProc-------allProclist.size--"
                + allProcList.size());
        String myUser = getAppUser(PROCESS_NAME, allProcList);
        Log.d(TAG, "-------- user ---------" + myUser);
        for (ProcessInfo processInfo : allProcList) {
            if (processInfo.name.toLowerCase().equals("logcat")
                    && processInfo.user.equals(myUser)) {
                Log.d(TAG, "--------kill--------logcat---------");
                android.os.Process.killProcess(Integer
                        .parseInt(processInfo.pid));
            }
        }
    }

    private String getAppUser(String procesName, List<ProcessInfo> allProcList) {
        for (ProcessInfo processInfo : allProcList) {
            if (processInfo.name.equals(procesName)) {
                return processInfo.user;
            }
        }
        return null;
    }

    // get all processes by use 'ps' command
    private List<String> getAllProcess() {
        List<String> orgProcList = new ArrayList<String>();
        Process proc = null;
        try {
            proc = Runtime.getRuntime().exec("ps");

            try {
                InputStreamReader isr = new InputStreamReader(
                        proc.getInputStream());
                BufferedReader br = new BufferedReader(isr);

                String line = null;
                while ((line = br.readLine()) != null) {
                    if (orgProcList != null) {
                        orgProcList.add(line);
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            if (proc.waitFor() != 0) {
                Log.e(TAG, "getAllProcess proc.waitFor() != 0");
                // recordLogServiceLog("getAllProcess proc.waitFor() != 0");
            }
        } catch (Exception e) {
            Log.e(TAG, "getAllProcess failed 01", e);
            // recordLogServiceLog("getAllProcess failed");
        } finally {
            try {
                proc.destroy();
            } catch (Exception e) {
                Log.e(TAG, "getAllProcess failed 02", e);
                // recordLogServiceLog("getAllProcess failed");
            }
        }
        Log.d(TAG, "-----------getAllProcess---------orgProcList.size---"
                + orgProcList.size());
        return orgProcList;
    }

    private List<ProcessInfo> getProcessInfoList(List<String> orgProcessList) {
        List<ProcessInfo> procInfoList = new ArrayList<ProcessInfo>();
        for (int i = 0; i < orgProcessList.size(); i++) {
            String processInfo = orgProcessList.get(i);
            String[] proStr = processInfo.split(" ");
            // USER PID PPID VSIZE RSS WCHAN PC NAME
            // root 1 0 416 300 c00d4b28 0000cd5c S /init
            List<String> orgInfo = new ArrayList<String>();
            for (String str : proStr) {
                if (!"".equals(str)) {
                    orgInfo.add(str);
                }
            }
            if (orgInfo.size() == 9) {
                ProcessInfo pInfo = new ProcessInfo();
                pInfo.user = orgInfo.get(0);
                pInfo.pid = orgInfo.get(1);
                pInfo.ppid = orgInfo.get(2);
                pInfo.name = orgInfo.get(8);
                procInfoList.add(pInfo);
            }
        }
        return procInfoList;
    }

    @Override
    public void run() {
        Log.d(TAG, "count------started");
        Process pro = null;
        try {

            // kill all logcat processes started by ActivityGraph before create
            // a new one
            List<String> orgProcessList = getAllProcess();
            List<ProcessInfo> processInfoList = getProcessInfoList(orgProcessList);
            killLogcatProc(processInfoList);

            // logcat -s ActivityManager
            Runtime.getRuntime().exec("logcat -c").waitFor();

            pro = Runtime.getRuntime().exec("logcat -s ActivityManager");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        DataInputStream dis = new DataInputStream(pro.getInputStream());
        String line = null;
        while (isObserverLog) {
            try {
                while ((line = dis.readLine()) != null) {

                    Log.e(TAG, "ActivityManager Print log : " + line);
                    String cmp = null;
                    // boolean isMain = line
                    // .contains("act=android.intent.action.MAIN")
                    // &&
                    // line.contains("cat=[android.intent.category.LAUNCHER]");
                    boolean isMain = line.contains("START")
                            && line.contains("cmp=");// modified by gxl
                    if (isMain) {
                        int a = line.indexOf("cmp=");
                        int b = line.indexOf("}");
                        cmp = line.substring(a + 4, b);

                        // add by gxl----start
                        if (cmp.contains(" ")) {
                            cmp = cmp.substring(0, cmp.indexOf(" "));
                        }
                        // add by gxl----end

                        // String str[] = cmp.split("/");
                        // String packageName = str[0];
                        // String activityName = str[0] + str[1];
                        // Log.i(TAG, "componetName = " + cmp);
                        // Log.i(TAG, "packageName = " + packageName);
                        // Log.i(TAG, "activityName = " + activityName);

                        // Intent main = new Intent(Intent.ACTION_MAIN, null);
                        // main.addCategory(Intent.CATEGORY_LAUNCHER);
                        // main.setComponent(new ComponentName(packageName,
                        // activityName));
                        // PackageManager pm = ActivityGraphLogService.this
                        // .getPackageManager();
                        // List<ResolveInfo> list =
                        // pm.queryIntentActivities(main, 0);
                        // Log.i(TAG, "list.size = " + list.size());

                        // if (list.size() == 1) {
                        // sendLogContent(cmp);
                        // }
                        Log.e(TAG, "cmpName : " + cmp);
                        if (nameList.contains(cmp)) {
                            sendLogContent(cmp);
                        }
                    }
                    // Thread.yield();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}