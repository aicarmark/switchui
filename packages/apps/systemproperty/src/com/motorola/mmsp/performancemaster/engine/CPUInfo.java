/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *                             Modification     Tracking
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * dfb746                      16/04/2012                   Initial release
 */

package com.motorola.mmsp.performancemaster.engine;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;

import com.motorola.mmsp.performancemaster.engine.Job.JobDoneListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CPUInfo extends InfoBase implements JobDoneListener {
    private final static String TAG = "CPUInfo";

    /**
     * linux /proc filesystem path for getting cpu info
     */
    private final static String CPU_FILE_PRE = "/sys/devices/system/cpu/cpu";

    private final static String CPU_FREQ_FILE_SUR = "/cpufreq/scaling_cur_freq";

    private final static String CPU_MIN_FREQ_FILE_SUR = "/cpufreq/cpuinfo_min_freq";

    private final static String CPU_MAX_FREQ_FILE_SUR = "/cpufreq/cpuinfo_max_freq";

    private final static String CPUINFO_FILE = "/proc/cpuinfo";

    private final static String CPU_STAT_FILE = "/proc/stat";

    private static final int ORDER_ORIENTATION_DEC = -10;

    private static int NORMAL_UPDATE_INTERVAL = 3000;
    
    //define model label
    private static final String QUALCOMM_PLATFORM_MODEL_LABEL = "Processor";
    
    private static final String INTEL_PLATFORM_MODEL_LABEL = "model name";

    /**
     * cpu current frequence, element in this ArrayList from index 0...n
     * corresponding to cpu 0...n
     */
    private ArrayList<Long> mCpuFreq = null;

    /**
     * cpu frequence range (min ~ max), element in this ArrayList from index
     * 0...n corresponding to cpu 0...n, each element in the list is a two
     * elements array, first is min freq, the second is max freq
     */
    private ArrayList<long[]> mCpuFreqRange = null;

    /**
     * cpu usage in last UPDATE_INTERVAL, element in this ArrayList from index
     * 0...n corresponding to first cpu total then cpu 0...n
     */
    private ArrayList<Float> mCpuUsage = null;

    /**
     * cpu workloads for helping calculate the cpu usage, element in this
     * ArrayList from index 0...n corresponding to first cpu total then cpu
     * 0...n
     */
    private ArrayList<long[]> mCpuPrevWorkloads = null;

    private Context mContext = null;

    private String mModel = "";

    private int mCpuCoreNum = 0;

    private Timer mUpdateTimer = null;

    private TimerTask mTimeTask = null;

    private Job mUpdateJob = null;

    /**
     * map contains all process info, key is PID
     */
    private HashMap<Integer, ProcessInfo> mAllProcessMap = null;

    /**
     * list contains all process info, sorted by process's cpu usage descending
     */
    private ArrayList<ProcessInfo> mAllProcessList = null;

    private List<RunningAppProcessInfo> mRunningAppInfolist = null;

    private DecimalFormat mFormatPercent = new DecimalFormat("##0.0");

    private static boolean mShouldReadAppProcInfo = false;

    private int mProcInfoReadIntervalCount = 0;

    // should update process information list at once
    private boolean mNeedQuicklyUpdateProcInfo = false;

    // the process information list has updated or not
    private boolean mProcInfoListeUpdated = false;
    
    //first start engine should read running process 
    private boolean mReadRunningAppFirstTime = true;

    private class ProcessTimeUsage {
        private long lUStime; // User + System

        private long lTotalTime; // User + Nice + System + Idle + IOW + IRQ +
                                 // SIRQ

        ProcessTimeUsage() {
            lUStime = 0;
            lTotalTime = 0;
        }
    }

    public class ProcessInfo {
        public int nProcessId; // process id

        public String strProcessName;// process name

        public String strPackageName;// package name

        public String strApplicationName; // application name

        public Drawable icon; // application icon

        public Boolean bSystemProcess;

        public ProcessTimeUsage ptuOld;

        public ProcessTimeUsage ptuNew;

        /*
         * ptuNew.lUStime - ptuOld.lUStime) *100f / (ptuNew.lTotalTime -
         * ptuOld.lTotalTime)
         */
        public float fProcessUsage; // (

        ProcessInfo() {
            this.nProcessId = -1;
            this.strProcessName = "";
            this.strPackageName = "";
            this.strApplicationName = "";
            this.icon = null;
            this.ptuOld = new ProcessTimeUsage();
            this.ptuNew = new ProcessTimeUsage();
            this.fProcessUsage = 0.0f;
            this.bSystemProcess = false;
        }
    }

    public CPUInfo(Context context) {
        super();

        this.mContext = context;
        this.mModel = getCpuModelFromFile();
        this.mCpuCoreNum = getCpuCoreNumFromFile();

        this.mCpuFreq = getAllCpuFreqFromFile();
        this.mCpuFreqRange = getAllCpuFreqRangeFromFile();
        this.mCpuUsage = calculateAllCpuUsage();

        this.mAllProcessMap = new HashMap<Integer, ProcessInfo>();

        mUpdateJob = new Job(CPUInfo.this) {
            @Override
            public void doJob() {
                // get cpu freq
                ArrayList<Long> cpuFreq = getAllCpuFreqFromFile();

                // get cpu usage
                ArrayList<Float> cpuUsage = calculateAllCpuUsage();
               
                synchronized (CPUInfo.this) {
                    // get all running process's cpu usage
                    autoReadProcess();
                    
                    mCpuFreq = cpuFreq;
                    mCpuUsage = cpuUsage;
                }
            }
        };
    }

    @Override
    public void onJobDone() {
        synchronized (this) {
            if (mCpuUsage != null) {
                onInfoUpdate();
            }
        }
    }

    @Override
    protected void startInfoUpdate() {
        mCpuUsage = null;
        mCpuPrevWorkloads = null;
        mUpdateTimer = new Timer();
        mTimeTask = new TimerTask() {
            public void run() {
                Worker.getInstance().addJob(mUpdateJob);
            }
        };

        mUpdateTimer.schedule(this.mTimeTask, 0, NORMAL_UPDATE_INTERVAL);
    }

    @Override
    protected void stopInfoUpdate() {
        if (mUpdateTimer != null) {
            mUpdateTimer.cancel();
            mTimeTask = null;
        }
    }

    public ArrayList<Float> getCpuUsageSingleShot() {
        ArrayList<Float> cpuUsage = null;
        while (null == cpuUsage) {
            cpuUsage = calculateAllCpuUsage();
        }

        return cpuUsage;
    }

    private void autoReadProcess() {
        if (mShouldReadAppProcInfo) {
            Log.e(TAG, " engine: autoReadProcess(), intervalCOunt=" + mProcInfoReadIntervalCount
                    + ", mNeedQuicklyUpdateProcInfo=" + mNeedQuicklyUpdateProcInfo);

            /*
             * There is three situation for update all running process 1. first
             * entry activity which should list all running process 2. user
             * finish the activity which list the process's details 3. normally
             * update info during 3*NORMAL_UPDATE_INTERVAL
             * mNeedQuicklyUpdateProcInfo=true in situation 1 and 2
             */
            if (mNeedQuicklyUpdateProcInfo || (++mProcInfoReadIntervalCount == 3))// normal
            {
                mNeedQuicklyUpdateProcInfo = false;
                mProcInfoReadIntervalCount = 0;

                Log.e(TAG, " Engine:update process info list");

                loadProcess();
            }
        }
        
        if (mReadRunningAppFirstTime)
        {
            Log.e(TAG, " first entry CPU engine should read all running process info");
            mReadRunningAppFirstTime = false;
            updateAllRunningAppProcess();
        }
    }

    private ArrayList<Long> getAllCpuFreqFromFile() {
        ArrayList<Long> allCPUFreq = new ArrayList<Long>();

        for (int i = 0; i < mCpuCoreNum; ++i) {
            allCPUFreq.add(getFreqFromFile(CPU_FILE_PRE + i + CPU_FREQ_FILE_SUR));
        }

        return allCPUFreq;
    }

    private ArrayList<long[]> getAllCpuFreqRangeFromFile() {
        ArrayList<long[]> allCPUFreqRange = new ArrayList<long[]>();

        for (int i = 0; i < mCpuCoreNum; ++i) {
            String maxFreqFile = CPU_FILE_PRE + i + CPU_MAX_FREQ_FILE_SUR;
            String minFreqFile = CPU_FILE_PRE + i + CPU_MIN_FREQ_FILE_SUR;
            long[] freqRange = new long[2];
            freqRange[0] = getFreqFromFile(minFreqFile);
            freqRange[1] = getFreqFromFile(maxFreqFile);
            allCPUFreqRange.add(freqRange);
        }

        return allCPUFreqRange;
    }

    /**
     * calculate all cpu usage in the last UPDATE_INTERVAL
     * 
     * @return all cpu usage in an ArrayList, null if there's exception data:
     *         cpu + usr + system + idle + cpu0 + cpu1 + .. + cpun index: 0 1 2
     *         3 4 5 n+4
     */
    private ArrayList<Float> calculateAllCpuUsage() {
        ArrayList<long[]> CPUCurWorkloads = new ArrayList<long[]>();
        // record the sample to mCPUPrevWorkloads
        try {
            // there should be mCpuCoreNum+1 lines of cpu statistics, first line
            // is the sum
            BufferedReader inStream;
            inStream = new BufferedReader(new FileReader(CPU_STAT_FILE));

            for (int nCPUIndex = 0; nCPUIndex < mCpuCoreNum + 1; ++nCPUIndex) {
                String cpuStatistics = inStream.readLine();
                if ((cpuStatistics != null) && (cpuStatistics.startsWith("cpu"))) {
                    String[] values = null;

                    values = cpuStatistics.split("[ ]+", 9);

                    long work = Long.parseLong(values[1]) + Long.parseLong(values[2])
                            + Long.parseLong(values[3]);
                    long total = work + Long.parseLong(values[4]) + Long.parseLong(values[5])
                            + Long.parseLong(values[6]) + Long.parseLong(values[7]);
                    long[] curWorkloads = new long[2];
                    curWorkloads[0] = work;
                    curWorkloads[1] = total;
                    CPUCurWorkloads.add(curWorkloads);

                    // calc cpu usr/system/idle
                    if (0 == nCPUIndex) {
                        long[] lTemp1 = new long[2];
                        lTemp1[0] = Long.parseLong(values[1]);
                        lTemp1[1] = total;
                        CPUCurWorkloads.add(lTemp1);

                        long[] lTemp2 = new long[2];
                        lTemp2[0] = Long.parseLong(values[3]);
                        lTemp2[1] = total;
                        CPUCurWorkloads.add(lTemp2);

                        long[] lTemp3 = new long[2];
                        lTemp3[0] = Long.parseLong(values[4]);
                        lTemp3[1] = total;
                        CPUCurWorkloads.add(lTemp3);
                    }
                } else if((2==nCPUIndex) && (!cpuStatistics.startsWith("cpu"))){
                    
                    long[] lcalcSecondCoreWorkloads = new long[2];
                    if (5 == CPUCurWorkloads.size()) // cpu0's info must be available
                    {
                        lcalcSecondCoreWorkloads[0] = CPUCurWorkloads.get(0)[0] - CPUCurWorkloads.get(4)[0];
                        lcalcSecondCoreWorkloads[1] = CPUCurWorkloads.get(0)[1] - CPUCurWorkloads.get(4)[1];
                        CPUCurWorkloads.add(lcalcSecondCoreWorkloads);
                    }
                    else {
                        Log.e(TAG, "error, can't obtain cpu0 info, set cpu2's info equal to total.");
                        
                        lcalcSecondCoreWorkloads[0] = CPUCurWorkloads.get(0)[0];
                        lcalcSecondCoreWorkloads[1] = CPUCurWorkloads.get(0)[1];
                        CPUCurWorkloads.add(lcalcSecondCoreWorkloads);
                    }
                    
                    cpuStatistics = "";
                } else {
                    Log.e(TAG, "unexcepted " + CPU_STAT_FILE + " format");
                    CPUCurWorkloads = null;
                }
            }
            
            inStream.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, " FileNotFoundException: " + e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e(TAG, " IOException: " + e.getMessage());
            return null;
        }

        // cpu usage can only be calculated since the second sample
        synchronized (this) {
            if ((CPUCurWorkloads == null) || (mCpuPrevWorkloads == null)
                    || (CPUCurWorkloads.size() != mCpuPrevWorkloads.size())) {
                mCpuPrevWorkloads = CPUCurWorkloads;
                return null;
            }
        }

        // calculate the cpu usage
        ArrayList<Float> allCPUUsage = new ArrayList<Float>();
        for (int i = 0; i < CPUCurWorkloads.size(); ++i) {
            long workAdded = CPUCurWorkloads.get(i)[0] - mCpuPrevWorkloads.get(i)[0];
            long totalAdded = CPUCurWorkloads.get(i)[1] - mCpuPrevWorkloads.get(i)[1];
            float cpuUsage = 0.0f;

            try {
                // this condition will remove NaN error.
                if ((workAdded != 0) ||
                        (totalAdded != 0)) {
                    cpuUsage = (float) workAdded * 100 / (float) totalAdded;
                }
            } catch (Exception e) {
                Log.e(TAG, "calculateAllCpuUsage(), e=" + e.getMessage());
            }

            allCPUUsage.add(cpuUsage);
        }

        synchronized (this) {
            mCpuPrevWorkloads = CPUCurWorkloads;
        }

        return allCPUUsage;
    }
       
    private String getCpuModelFromFile() {
        try {
            String strTemp = null, strModel = null;

            BufferedReader inStream = new BufferedReader(new FileReader(CPUINFO_FILE));

            while (((strTemp = inStream.readLine()) != null) && (strTemp.length() > 0)) {
                
                if (strTemp.startsWith(QUALCOMM_PLATFORM_MODEL_LABEL) || 
                    strTemp.startsWith(INTEL_PLATFORM_MODEL_LABEL)) {

                    strModel = strTemp.substring(strTemp.indexOf(":") + 1);
                    int nEndPos = strModel.indexOf('@');
                    nEndPos = (nEndPos == -1) ? strModel.length() : nEndPos;

                    strModel = strModel.substring(0, nEndPos);

                    strModel = strModel.trim();

                    Log.e(TAG, "  Model=" + strModel);
                    break;
                }
            }
            inStream.close();

            return strModel;

        } catch (IOException e) {
            Log.e(TAG, "getCpuModelFromFile(), error msg: " + e.getMessage());
        }

        return null;
    }

    private long getFreqFromFile(String filePath) {
        String strFreq = getStringFromFile(filePath);
        if (strFreq != null) {
            return Long.parseLong(strFreq.trim());
        }

        return 0;
    }

    private String getStringFromFile(String filePath) {
        File f = new File(filePath);
        try {
            BufferedReader in = new BufferedReader(new FileReader(f));
            String strReadOut = in.readLine();
            in.close();
            return strReadOut;
        } catch (FileNotFoundException e) {
            Log.e(TAG, " FileNotFoundException: " + e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e(TAG, " IOException: " + e.getMessage());
            return null;
        }
    }

    /**
     * get cpu core number by checking all cpu[0~9]+ under
     * /sys/devices/system/cpu/, eg. if there exists cpu0 cpu1 cpu2 cpu3, the
     * cpu core number should be (3+1) = 4
     */
    private int getCpuCoreNumFromFile() {
        int coreNum = 0;

        File dir = new File("/sys/devices/system/cpu/");
        String regEx = "cpu[0-9]+";
        Pattern p = Pattern.compile(regEx);

        if (!dir.exists() || (!dir.isDirectory())) {
            Log.e(TAG, "not exit or is not dir");
            return coreNum + 1;
        }

        File[] files = dir.listFiles();

        for (File f : files) {
            String filename = f.getName();
            Matcher m = p.matcher(filename);
            if (m.find()) {
                int num = Integer.parseInt(filename.substring("cpu".length()));
                if (num > coreNum) {
                    coreNum = num;
                }
            }
        }

        return coreNum + 1;
    }

    private void loadProcess() {
        // update all individual process's cpu usage info
        updateAllRunningAppProcess();

        // reorder
        reOrderAllRunningAppProcess(ORDER_ORIENTATION_DEC);

        // Process info list has updated, UI can read it.
        mProcInfoListeUpdated = true;

        Log.e(TAG, " Running APP Process list size is " +
                mAllProcessList.size());
    }

    /**
     * 1. find all need process id 2. update special process's info 3. reorder
     */
    private void updateAllRunningAppProcess() {
        // find all running application process
        ActivityManager mActivityManager = (ActivityManager) (mContext
                .getSystemService(Context.ACTIVITY_SERVICE));
        mRunningAppInfolist = mActivityManager.getRunningAppProcesses();

        long totalTime = readTotalTime();

        if (mAllProcessMap.isEmpty()) {// read running application process first
                                       // time
            Log.e(TAG, "  first time entry Running App Process");
            for (RunningAppProcessInfo rapi : mRunningAppInfolist) {
                saveNewProcessInfo(rapi, totalTime);
            }
        } else {// the mALProcessInfo is't empty, we only save individual
                // process in map

            Log.e(TAG, "  entry Running App Process again, mapSize=" + mAllProcessMap.size());
            // copy process info to a temp array
            HashMap<Integer, ProcessInfo> oldProcessMap = mAllProcessMap;
            mAllProcessMap = new HashMap<Integer, ProcessInfo>();

            // we only save newest running app process id, discard other
            for (RunningAppProcessInfo rapi : mRunningAppInfolist) {
                // find out this process in old array
                ProcessInfo pi = oldProcessMap.get(rapi.pid);

                if (pi != null) {// process info in old array found

                    // 1. exchange old/new value
                    pi.ptuOld.lTotalTime = pi.ptuNew.lTotalTime;
                    pi.ptuOld.lUStime = pi.ptuNew.lUStime;

                    // 2. save new value
                    pi.ptuNew.lUStime = readSpecialProcessTime(pi.nProcessId);
                    pi.ptuNew.lTotalTime = totalTime;

                    // 3. update array element
                    addProcessInfoToMap(pi, rapi);
                } else {// this is a new process
                    saveNewProcessInfo(rapi, totalTime);
                }
            }
            
            //clear old map, the oldProcessMap will be initialized at next time            
            oldProcessMap.clear();
            oldProcessMap = null;
        }
    }

    @SuppressWarnings("unused")
    private void saveNewProcessInfo(RunningAppProcessInfo rapi, long totalTime) {
        ProcessInfo pi = new ProcessInfo();

        if (null == pi) {
            Log.e(TAG, " pi == null");
            return;
        }

        pi.strProcessName = rapi.processName;
        pi.nProcessId = rapi.pid;

        // update new value, and old value is 0
        pi.ptuNew.lUStime = readSpecialProcessTime(pi.nProcessId);
        pi.ptuNew.lTotalTime = totalTime;
        
        // save this process information to hash map
        addProcessInfoToMap(pi, rapi);
    }

    private void addProcessInfoToMap(ProcessInfo pi, RunningAppProcessInfo rapi) {
        // 1. init application information
        updateApplicationInfo(pi, rapi);

        // 2. calc process cpu usage
        updateProcessCpuUsage(pi);

        // 3. update array element
        mAllProcessMap.put(pi.nProcessId, pi);
    }

    private void updateApplicationInfo(ProcessInfo pi, RunningAppProcessInfo rapi) {
        if ((pi == null) || (rapi == null)) {
            Log.e(TAG, "(pi == null) || (rapi == null)");
        }

        PackageManager pm = mContext.getPackageManager();

        try // package name
        {
            String[] strPkgList = rapi.pkgList;

            for (String strPkgName : strPkgList) {
                if (strPkgName != null) {
                    ApplicationInfo ai = pm.getApplicationInfo(strPkgName, 0);

                    pi.strPackageName = ai.packageName;

                    pi.bSystemProcess = ai.sourceDir.startsWith("/system");

                    break;
                }
            }

        } catch (NameNotFoundException err) {
            Log.e(TAG, "it's NameNotFoundException : " + err);
        }

        try// icon + label
        {
            ApplicationInfo ai = pm.getApplicationInfo(pi.strProcessName, 0);

            if (ai != null) {
                try {
                    pi.icon = pm.getApplicationIcon(ai);

                    CharSequence label = pm.getApplicationLabel(ai);

                    if (label != null) {
                        pi.strApplicationName = label.toString();
                    }

                } catch (OutOfMemoryError oomerr) {
                    Log.e(TAG, " error for loading icon: "  + oomerr.getMessage());
                }
            }
        } catch (NameNotFoundException nnfe) {
            // can't find this package, we set strApplicationName =
            // strProcessName
            pi.icon = mContext.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
            pi.strApplicationName = pi.strProcessName;
        }
    }

    private void updateProcessCpuUsage(ProcessInfo pi) {
        if (pi == null) {
            return;
        }

        float fUsage = 0.0f;
        try {
            fUsage = (float)((pi.ptuNew.lUStime - pi.ptuOld.lUStime) * 100f)
                    / (pi.ptuNew.lTotalTime - pi.ptuOld.lTotalTime);
        } catch (Exception e) {
            fUsage = 0.0f;
            Log.e(TAG, "error: " + e.getMessage());
        }

        pi.fProcessUsage = Float.parseFloat(mFormatPercent.format(fUsage));
    }

    private void reOrderAllRunningAppProcess(final int nOrderAsc) {
        mAllProcessList = new ArrayList<ProcessInfo>(mAllProcessMap.values());

        Collections.sort(mAllProcessList, new Comparator<ProcessInfo>() {
            public int compare(ProcessInfo obj1, ProcessInfo obj2) {
                int nRes = Float.compare(obj1.fProcessUsage, obj2.fProcessUsage);

                return (nRes * nOrderAsc);
            }
        });
    }

    private long readTotalTime() {
        BufferedReader inStream;
        String strReadFilePath = "/proc/stat", strReadOutData = null;
        String[] resArray;
        long lTotalTime = 0;

        try {
            inStream = new BufferedReader(new FileReader(strReadFilePath));
            strReadOutData = inStream.readLine();

            if (null != strReadOutData) {
                resArray = strReadOutData.split("[ ]+");

                lTotalTime = Long.parseLong(resArray[1]) + Long.parseLong(resArray[2])
                        + Long.parseLong(resArray[3]) + Long.parseLong(resArray[4])
                        + Long.parseLong(resArray[5]) + Long.parseLong(resArray[6])
                        + Long.parseLong(resArray[7]);
            }

            inStream.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, " FileNotFoundException: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, " IOException: " + e.getMessage());
        }

        return lTotalTime;
    }

    private long readSpecialProcessTime(int nPid) {
        BufferedReader inStream;
        String strReadFilePath = "/proc/" + nPid + "/stat", strReadOutData = null;
        String[] resArray;
        long lUSTime = 0;

        try {
            inStream = new BufferedReader(new FileReader(strReadFilePath));
            strReadOutData = inStream.readLine();

            if (strReadOutData != null) {
                resArray = strReadOutData.split("[ ]+");
                lUSTime = Integer.parseInt(resArray[13]) + Integer.parseInt(resArray[14]);
            }
            
            inStream.close();

        } catch (FileNotFoundException e) {
            Log.e(TAG, " FileNotFoundException: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, " IOException: " + e.getMessage());
        }

        return lUSTime;
    }

    public void setReadAppProcess(boolean bShouldRead) {
        synchronized (CPUInfo.this) {
            if (bShouldRead != mShouldReadAppProcInfo) {
                mShouldReadAppProcInfo = bShouldRead;
            }

            mNeedQuicklyUpdateProcInfo = bShouldRead;
        }
    }

    public boolean getReadAppProcess() {
        synchronized (CPUInfo.this) {
            return mShouldReadAppProcInfo;
        }
    }

    public String getModel() {
        return mModel;
    }

    public ArrayList<Long> getCpuFreq() {
        return mCpuFreq;
    }

    public ArrayList<long[]> getCpuFreqRange() {
        return mCpuFreqRange;
    }

    public ArrayList<Float> getCpuUsage() {
        return mCpuUsage;
    }

    public int getCpuCoreNum() {
        return mCpuCoreNum;
    }

    public ArrayList<ProcessInfo> getmAllProcessInfo() {
        // user has taked all process info,so we must change flag
        mProcInfoListeUpdated = false;
        return mAllProcessList;
    }

    public boolean isProcessInfoUpdated() {
        return mProcInfoListeUpdated;
    }

    @Override
    public String toString() {
        return "CPUInfo: " + this.mCpuFreq + "/" + this.mCpuFreqRange + "/" + this.mCpuUsage + "/"
                + this.mModel + "/" + this.mCpuCoreNum;
    }
}
