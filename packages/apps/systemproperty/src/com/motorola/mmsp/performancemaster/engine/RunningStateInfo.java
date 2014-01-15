/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.motorola.mmsp.performancemaster.engine;

import android.app.ActivityManager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;

import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import android.content.res.Resources;
import android.os.Debug;

import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.motorola.mmsp.performancemaster.engine.Job.JobDoneListener;
import com.motorola.mmsp.performancemaster.R;

/**
 * Singleton for retrieving and monitoring the state about all running
 * applications/processes/services.
 */
public class RunningStateInfo extends InfoBase implements JobDoneListener {
    private final static String TAG = "RunningStateInfo";

    protected static final int MSG_UPDATE_CONTENTS = 1;
    protected static final int MSG_REFRESH_UI = 2;
    protected static final int MSG_UPDATE_TIME = 3;
    protected static final int MAX_SERVICES = 100;

    private final ProcessComparator mProcessComparator = new ProcessComparator();

    private final Context mContext;
    private final ActivityManager mActivityManager;
    private final PackageManager mPackageManager;

    // Processes that are hosting a service we are interested in, organized
    // by uid and name. Note that this mapping does not change even across
    // service restarts, and during a restart there will still be a process
    // entry.
    private final SparseArray<HashMap<String, ProcessItem>> mServiceProcessesByName = new SparseArray<HashMap<String, ProcessItem>>();

    // Processes that are hosting a service we are interested in, organized
    // by their pid. These disappear and re-appear as services are restarted.
    private final SparseArray<ProcessItem> mServiceProcessesByPid = new SparseArray<ProcessItem>();

    // Used to sort the interesting processes.
    private final ServiceProcessComparator mServiceProcessComparator = new ServiceProcessComparator();

    // All currently running processes, for finding dependencies etc.
    private final SparseArray<ProcessItem> mRunningProcesses = new SparseArray<ProcessItem>();

    // The processes associated with services, in sorted order.
    private final ArrayList<ProcessItem> mProcessItems = new ArrayList<ProcessItem>();

    // All processes, used for retrieving memory information.
    // private final ArrayList<ProcessItem> mAllProcessItems = new
    // ArrayList<ProcessItem>();

    private int mSequence = 0;
    private boolean mEndProcessing = false; // it is for display it is ending
                                            // the process

    // ----- following protected by mLock -----

    // Lock for protecting the state that will be shared between the
    // background update thread and the UI thread.
    final public Object mLock = new Object();
    final public Object mEndProcessLock = new Object();

    private boolean mHaveData = false;

    private ArrayList<MergedItem> mMergedItems = new ArrayList<MergedItem>();
    private ArrayList<MergedItem> mBackgroundItems = new ArrayList<MergedItem>();
    private ArrayList<String> mEndedProcessName = new ArrayList<String>();

    private Job mUpdateJob = null;

    private Timer mUpdateTimer = null;

    private TimerTask mTimeTask = null;
    private final int UPDATE_INTERVAL = 6000;
    private final int UPDATE_DELAY = 15000;
    private boolean mIsUpdate = false;
    private boolean mEndAllProcess = true;

    public RunningStateInfo(Context context) {
        mContext = context;
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mPackageManager = mContext.getPackageManager();

        Log.e(TAG, "RunningStateInfo RunningStateInfo is in");

        mUpdateJob = new Job(RunningStateInfo.this) {
            @Override
            public void doJob() {
    
                synchronized (mLock) {
                    if (!mIsUpdate) {
                        mIsUpdate = true;
                    } else {
                        return;
                    }

                    mEndProcessing = false;
                }

                update(mContext, mActivityManager);

                synchronized (mLock) {
                    mIsUpdate = false;
                }
            }

        };

    }

    @Override
    public void onJobDone() {
        Log.e(TAG, "RunningStateInfo onJobDone");

        onInfoUpdate();

    }

    @Override
    protected void startInfoUpdate() {
        Log.e(TAG, "RunningStateInfo startInfoUpdate");

        updateAgain();
    }

    @Override
    protected void stopInfoUpdate() {
        disableUpdate();
    }

    public boolean hasData() {

        synchronized (mLock) {
            return mHaveData;
        }

    }

    public void startUpdate() {

        synchronized (mLock) {
            if (mIsUpdate)
                return;
        }

        Worker.getInstance().addJob(mUpdateJob);
    }

    /*
     * one click the all process
     */

    public void oneClickClear() {

        mEndAllProcess = true;
        endAllProcess(mEndAllProcess);

    }

    /*
     * end the process if it is selected
     */

    public void runningEndProcess() {

        mEndAllProcess = false;
        endAllProcess(mEndAllProcess);

    }

    public void disableUpdate() {

        if (mUpdateTimer != null) {
            mUpdateTimer.cancel();
            mTimeTask = null;
        }
    }

    public void updateAgain() {

        mUpdateTimer = new Timer();
        mTimeTask = new TimerTask() {
            public void run() {

                synchronized (mLock) {
                    Log.e(TAG, "RunningStateInfo updateAgain mTimeTask  run mIsUpdate ="
                            + mIsUpdate);
                    if (mIsUpdate)
                        return;
                }
                Worker.getInstance().addJob(mUpdateJob);
            }
        };
        // if if is updating, so delay the UPDATE_DELAY to update

        mUpdateTimer.schedule(this.mTimeTask, 0, UPDATE_INTERVAL);

    }

    public void endAllProcess(boolean isOneClick) {

        String mself = mContext.getPackageName();
        Log.e(TAG, "endAllProcess is  in");
        synchronized (mLock) {
            mEndProcessing = true;
            if (mEndedProcessName.size() > 0) {
                mEndedProcessName.clear();
            }
            Log.e(TAG, "it is in set endAllProcess");
            for (int i = 0, size = mBackgroundItems.size(); i < size; i++) {

                MergedItem mMergedItem = mBackgroundItems.get(i);

                if ((!isOneClick) && (!mMergedItem.isChecked)) // if it is
                    // not
                    // selected , it
                    // will
                    // be not close
                    continue;

                String procName = mMergedItem.mProcess.mRunningProcessInfo.processName;

                if (!mself.equals(procName)) {
                    Log.e(TAG, "endProcess before procName = " + procName + " size = " + size);
                    mEndedProcessName.add(procName);
                    endProcess(mActivityManager, mMergedItem.mProcess.mRunningProcessInfo.pkgList);
                    mBackgroundItems.remove(i);
                    size--;
                    i--;
                }

            }

        }
        Log.e(TAG, "RunningStateInfo endAllProcess");

    }

    private void endProcess(ActivityManager am, String[] pkgs) {
        if (pkgs != null) {
            for (String pkg : pkgs) {
                if (pkg != null) {

                    am.killBackgroundProcesses(pkg);
                    Log.e(TAG, "endProcess pkg = " + pkg);
                }
            }
        }
    }

    private boolean update(Context context, ActivityManager am) {
        final PackageManager pm = context.getPackageManager();

        mSequence++;

        boolean changed = false;

        List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(MAX_SERVICES);
        final int NS = services != null ? services.size() : 0;
        for (int i = 0; i < NS; i++) {
            ActivityManager.RunningServiceInfo si = services.get(i);
            // We are not interested in services that have not been started
            // and don't have a known client, because
            // there is nothing the user can do about them.
            si.toString();

            if (!si.started && si.clientLabel == 0) {
                continue;
            }
            // We likewise don't care about services running in a
            // persistent process like the system or phone.
            if ((si.flags & ActivityManager.RunningServiceInfo.FLAG_PERSISTENT_PROCESS) != 0) {
                continue;
            }

            HashMap<String, ProcessItem> procs = mServiceProcessesByName.get(si.uid);
            if (procs == null) {
                procs = new HashMap<String, ProcessItem>();

                mServiceProcessesByName.put(si.uid, procs);
            }

            ProcessItem proc = procs.get(si.process);
            if (proc == null) {

                changed = true;
                proc = new ProcessItem(context, si.uid, si.process);
                procs.put(si.process, proc);
            }

            if (proc.mCurSeq != mSequence) {
                int pid = si.restarting == 0 ? si.pid : 0;

                if (pid != proc.mPid) {
                    changed = true;
                    if (proc.mPid != pid) {
                        if (proc.mPid != 0) {
                            mServiceProcessesByPid.remove(proc.mPid);
                        }
                        if (pid != 0) {
                            mServiceProcessesByPid.put(pid, proc);
                        }
                        proc.mPid = pid;
                    }
                }
                proc.mDependentProcesses.clear();
                proc.mCurSeq = mSequence;
            }
            changed |= proc.updateService(context, si);
        }

        // Now update the map of other processes that are running (but
        // don't have services actively running inside them).
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        final int NP = processes != null ? processes.size() : 0;

        for (int i = 0; i < NP; i++) {
            ActivityManager.RunningAppProcessInfo pi = processes.get(i);

            ProcessItem proc = mServiceProcessesByPid.get(pi.pid);
            if (proc == null) {
                // This process is not one that is a direct container
                // of a service, so look for it in the secondary
                // running list.
                proc = mRunningProcesses.get(pi.pid);
                if (proc == null) {
                    changed = true;
                    proc = new ProcessItem(context, pi.uid, pi.processName);
                    proc.mPid = pi.pid;
                    mRunningProcesses.put(pi.pid, proc);
                }
                proc.mDependentProcesses.clear();
            }

            proc.mRunningSeq = mSequence;
            proc.mRunningProcessInfo = pi;
        }

        // Build the chains from client processes to the process they are
        // dependent on; also remove any old running processes.
        int NRP = mRunningProcesses.size();
        for (int i = 0; i < NRP;) {
            ProcessItem proc = mRunningProcesses.valueAt(i);
            if (proc.mRunningSeq == mSequence) {
                int clientPid = proc.mRunningProcessInfo.importanceReasonPid;

                if (clientPid != 0) {
                    ProcessItem client = mServiceProcessesByPid.get(clientPid);
                    if (client == null) {
                        client = mRunningProcesses.get(clientPid);
                    }
                    if (client != null) {
                        client.mDependentProcesses.put(proc.mPid, proc);
                    }
                } else {
                    // In this pass the process doesn't have a client.
                    // Clear to make sure that, if it later gets the same one,
                    // we will detect the change.
                    proc.mClient = null;
                }
                i++;
            } else {
                changed = true;

                mRunningProcesses.remove(mRunningProcesses.keyAt(i));
                NRP--;
            }
        }

        // Follow the tree from all primary service processes to all
        // processes they are dependent on, marking these processes as
        // still being active and determining if anything has changed.
        final int NAP = mServiceProcessesByPid.size();
        // Log.e("RunningState",
        // " RunningAppProcessInfo mServiceProcessesByPid NAP() = " + NAP);
        for (int i = 0; i < NAP; i++) {
            ProcessItem proc = mServiceProcessesByPid.valueAt(i);
            if (proc.mCurSeq == mSequence) {
                changed |= proc.buildDependencyChain(context, pm, mSequence);
            }
        }

        // Look for services and their primary processes that no longer exist...
        for (int i = 0; i < mServiceProcessesByName.size(); i++) {
            HashMap<String, ProcessItem> procs = mServiceProcessesByName.valueAt(i);
            Iterator<ProcessItem> pit = procs.values().iterator();
            while (pit.hasNext()) {
                ProcessItem pi = pit.next();
                if (pi.mCurSeq == mSequence) {
                    pi.ensureLabel(pm);
                    if (pi.mPid == 0) {
                        // Sanity: a non-process can't be dependent on
                        // anything.
                        pi.mDependentProcesses.clear();
                    }
                } else {
                    changed = true;
                    pit.remove();
                    if (procs.size() == 0) {
                        mServiceProcessesByName.remove(mServiceProcessesByName.keyAt(i));
                    }
                    if (pi.mPid != 0) {
                        mServiceProcessesByPid.remove(pi.mPid);
                    }
                    continue;
                }
                Iterator<ServiceItem> sit = pi.mServices.values().iterator();
                while (sit.hasNext()) {
                    ServiceItem si = sit.next();
                    if (si.mCurSeq != mSequence) {
                        changed = true;
                        sit.remove();
                    }
                }
            }
        }

        ArrayList<MergedItem> newMergedItems = new ArrayList<MergedItem>();

        if (changed) {
            // First determine an order for the services.
            ArrayList<ProcessItem> sortedProcesses = new ArrayList<ProcessItem>();
            for (int i = 0; i < mServiceProcessesByName.size(); i++) {
                for (ProcessItem pi : mServiceProcessesByName.valueAt(i).values()) {
                    pi.mIsSystem = false;
                    pi.mIsStarted = true;
                    pi.mActiveSince = Long.MAX_VALUE;
                    for (ServiceItem si : pi.mServices.values()) {
                        if (si.mServiceInfo != null
                                && (si.mServiceInfo.applicationInfo.flags
                                        & ApplicationInfo.FLAG_SYSTEM) != 0) {
                            pi.mIsSystem = true;
                        }
                        if (si.mRunningService != null
                                && si.mRunningService.clientLabel != 0) {
                            pi.mIsStarted = false;
                            if (pi.mActiveSince > si.mRunningService.activeSince) {
                                pi.mActiveSince = si.mRunningService.activeSince;
                            }
                        }
                    }
                    sortedProcesses.add(pi);
                }
            }

            mProcessItems.clear();
            for (int i = 0; i < sortedProcesses.size(); i++) {
                ProcessItem pi = sortedProcesses.get(i);

                int firstProc = mProcessItems.size();
                // First add processes we are dependent on.
                pi.addDependentProcesses(mProcessItems);
                // And add the process itself.

                if (pi.mPid > 0) {
                    mProcessItems.add(pi);

                }

                // Now add the services running in it.
                MergedItem mergedItem = null;

                for (ServiceItem si : pi.mServices.values()) {

                    if (si.mMergedItem != null) {
                        if (mergedItem != null && mergedItem != si.mMergedItem) {

                        }
                        mergedItem = si.mMergedItem;
                    }
                }

                if (mergedItem == null
                        || mergedItem.mServices.size() != pi.mServices.size()) {
                    // Whoops, we need to build a new MergedItem!
                    mergedItem = new MergedItem();
                    for (ServiceItem si : pi.mServices.values()) {
                        mergedItem.mServices.add(si);
                        si.mMergedItem = mergedItem;
                    }
                    mergedItem.mProcess = pi;
                    mergedItem.mOtherProcesses.clear();
                    for (int mpi = firstProc; mpi < (mProcessItems.size() - 1); mpi++) {
                        mergedItem.mOtherProcesses.add(mProcessItems.get(mpi));
                    }
                }

                mergedItem.update(context, false);
                mergedItem.isSysProcess();
                newMergedItems.add(mergedItem);
            }
        }

        // Count number of interesting other (non-active) processes, and
        // build a list of all processes we will retrieve memory for.
        ArrayList<ProcessItem> mAllProcessItems = new ArrayList<ProcessItem>();
        mAllProcessItems.addAll(mProcessItems);
        int numBackgroundProcesses = 0;
        int numForegroundProcesses = 0;
        int numServiceProcesses = 0;
        NRP = mRunningProcesses.size();
        for (int i = 0; i < NRP; i++) {
            ProcessItem proc = mRunningProcesses.valueAt(i);
            if (proc.mCurSeq != mSequence) {
                // We didn't hit this process as a dependency on one
                // of our active ones, so add it up if needed.
                if (proc.mRunningProcessInfo.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                    numBackgroundProcesses++;
                    mAllProcessItems.add(proc);

                } else if (proc.mRunningProcessInfo.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    numForegroundProcesses++;
                    mAllProcessItems.add(proc);

                } else {
                    Log.i(TAG, "Unknown non-service process: "
                            + proc.mProcessName + " #" + proc.mPid);
                }
            } else {
                numServiceProcesses++;
            }
        }

        ArrayList<MergedItem> newBackgroundItems = new ArrayList<MergedItem>(numBackgroundProcesses);

        final int numProc = mAllProcessItems.size();
        int[] pids = new int[numProc];
        for (int i = 0; i < numProc; i++) {
            pids[i] = mAllProcessItems.get(i).mPid;
        }

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> apps = mPackageManager.queryIntentActivities(mainIntent, 0);

        Debug.MemoryInfo[] mem = am.getProcessMemoryInfo(pids);

        for (int i = 0; i < pids.length; i++) {
            ProcessItem proc = mAllProcessItems.get(i);

            proc.updateSize(context, mem[i], mSequence);
            if (proc.mCurSeq == mSequence) {

            } else if (proc.mRunningProcessInfo.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {

                MergedItem mergedItem;
                /* home which we need not be killed */

                if (apps != null) {

                    int mLaucheersize = 0;
                    /* check if package is in list */
                    for (mLaucheersize = 0; mLaucheersize < apps.size(); mLaucheersize++) {
                        if (apps.get(mLaucheersize).activityInfo.packageName
                                    .equals(proc.mProcessName)) {
                            Log.e(TAG, "home task " + proc.mProcessName);
                            break;
                        }
                    }
                    if (mLaucheersize < apps.size()) {
                        continue;
                    }
                }

                mergedItem = proc.mMergedItem = new MergedItem();
                proc.mMergedItem.mProcess = proc;

                if (proc.mSize != 0) {
                    proc.ensureLabel(pm);
                    newBackgroundItems.add(mergedItem);

                }
                mergedItem.update(context, true);
                mergedItem.updateSize(context);

            }

        }

        for (int ii = 0; ii < newMergedItems.size(); ii++) {
            newMergedItems.get(ii).updateSize(context);
        }

        Collections.sort(newMergedItems, mServiceProcessComparator);

        if (changed) {
            synchronized (mLock) {
                mMergedItems = newMergedItems;

            }
        }

        mAllProcessItems.clear();

        synchronized (mLock) {
            int mNewBackItemsize = newBackgroundItems.size();
            int msize = 0;
            Log.e(TAG, "it is in set mBackgroundItems  mEndProcessing" + mEndProcessing);

            if (mEndProcessing) {
                for (msize = 0; msize < mNewBackItemsize; msize++) {
                    MergedItem mergedItem = newBackgroundItems.get(msize);
                    for (int size = 0; size < mEndedProcessName.size(); size++) {
                        if (mergedItem.mProcess.mProcessName
                                .equals(mEndedProcessName.get(size))) {
                            newBackgroundItems.remove(msize);
                            mNewBackItemsize--;
                            msize--;
                        }
                    }

                }
            }

            for (int i1 = 0; i1 < mBackgroundItems.size(); i1++) {
                if (mBackgroundItems.get(i1).isChecked) {
                    Log.e(TAG, "it is ******* mBackgroundItems.size()" + mBackgroundItems.size());

                    continue;
                }

                for (msize = 0; msize < mNewBackItemsize; msize++) {
                    MergedItem mergedItem = newBackgroundItems.get(msize);
                    if (mergedItem.mProcess.mProcessName
                            .equals(mBackgroundItems.get(i1).mProcess.mProcessName)) {
                        mergedItem.isChecked = false;
                    }
                }
            }

        }
        Collections.sort(newBackgroundItems, mProcessComparator);

        synchronized (mLock) {
            if (newBackgroundItems != null) {
                mBackgroundItems = newBackgroundItems;
            }

            if (!mHaveData) {
                mHaveData = true;
            }
        }

        return changed;
    }

    public class BaseItem {
        final public boolean mIsProcess;

        public PackageItemInfo mPackageInfo;
        public CharSequence mDisplayLabel;
        String mLabel;


        int mCurSeq;

        public long mActiveSince;
        public long mSize;
        public String mSizeStr;
        public String mCurSizeStr;

        public boolean mBackground;

        public BaseItem(boolean isProcess) {
            mIsProcess = isProcess;
        }
    }

    public class ServiceItem extends BaseItem {
        ActivityManager.RunningServiceInfo mRunningService;
        ServiceInfo mServiceInfo;
        boolean mShownAsStarted;

        MergedItem mMergedItem;

        public ServiceItem() {
            super(false);
        }
    }

    public class ProcessItem extends BaseItem {
        final HashMap<ComponentName, ServiceItem> mServices = new HashMap<ComponentName, ServiceItem>();
        final SparseArray<ProcessItem> mDependentProcesses = new SparseArray<ProcessItem>();

        final int mUid;
        public String mProcessName;
        int mPid;

        ProcessItem mClient;
        int mLastNumDependentProcesses;

        int mRunningSeq;
        public ActivityManager.RunningAppProcessInfo mRunningProcessInfo;

        MergedItem mMergedItem;

        // Purely for sorting.
        boolean mIsSystem;
        boolean mIsStarted;
        long mActiveSince;

        public ProcessItem(Context context, int uid, String processName) {
            super(true);
            mUid = uid;
            mProcessName = processName;


        }

        public void ensureLabel(PackageManager pm) {
            if (mLabel != null) {
                return;
            }

            try {
                ApplicationInfo ai = pm.getApplicationInfo(mProcessName, 0);
                if (ai.uid == mUid) {
                    mDisplayLabel = ai.loadLabel(pm);
                    mLabel = mDisplayLabel.toString();
                    mPackageInfo = ai;
                    return;
                }
            } catch (PackageManager.NameNotFoundException e) {
            }

            // If we couldn't get information about the overall
            // process, try to find something about the uid.
            String[] pkgs = pm.getPackagesForUid(mUid);

            // If there is one package with this uid, that is what we want.
            if (pkgs.length == 1) {
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(pkgs[0], 0);
                    mDisplayLabel = ai.loadLabel(pm);
                    mLabel = mDisplayLabel.toString();
                    mPackageInfo = ai;
                    return;
                } catch (PackageManager.NameNotFoundException e) {
                }
            }

            // If there are multiple, see if one gives us the official name
            // for this uid.
            for (String name : pkgs) {
                try {
                    PackageInfo pi = pm.getPackageInfo(name, 0);
                    if (pi.sharedUserLabel != 0) {
                        CharSequence nm = pm.getText(name,
                                pi.sharedUserLabel, pi.applicationInfo);
                        if (nm != null) {
                            mDisplayLabel = nm;
                            mLabel = nm.toString();
                            mPackageInfo = pi.applicationInfo;
                            return;
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }

            // If still don't have anything to display, just use the
            // service info.
            if (mServices.size() > 0) {
                mPackageInfo = mServices.values().iterator().next()
                        .mServiceInfo.applicationInfo;
                mDisplayLabel = mPackageInfo.loadLabel(pm);
                mLabel = mDisplayLabel.toString();
                return;
            }

            // Finally... whatever, just pick the first package's name.
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkgs[0], 0);
                mDisplayLabel = ai.loadLabel(pm);
                mLabel = mDisplayLabel.toString();
                mPackageInfo = ai;
                return;
            } catch (PackageManager.NameNotFoundException e) {
            }
        }

        boolean updateService(Context context,
                ActivityManager.RunningServiceInfo service) {
            final PackageManager pm = context.getPackageManager();

            boolean changed = false;
            ServiceItem si = mServices.get(service.service);
            if (si == null) {
                changed = true;
                si = new ServiceItem();
                si.mRunningService = service;
                try {
                    si.mServiceInfo = pm.getServiceInfo(service.service, 0);
                } catch (PackageManager.NameNotFoundException e) {
                }
                si.mDisplayLabel = makeLabel(pm,
                        si.mRunningService.service.getClassName(), si.mServiceInfo);
                mLabel = mDisplayLabel != null ? mDisplayLabel.toString() : null;
                Log.e("RunningState", " updateService mLabel = " + mLabel);

                if (si.mServiceInfo == null) {
                    mServices.remove(service.service);
                } else {
                    si.mPackageInfo = si.mServiceInfo.applicationInfo;
                    mServices.put(service.service, si);
                }

            }

            si.mCurSeq = mCurSeq;
            si.mRunningService = service;
            long activeSince = service.restarting == 0 ? service.activeSince : -1;

            if (si.mActiveSince != activeSince) {
                si.mActiveSince = activeSince;
                changed = true;
            }
            if (service.clientPackage != null && service.clientLabel != 0) {
                if (si.mShownAsStarted) {
                    si.mShownAsStarted = false;
                    changed = true;
                }

      
            } else {
                if (!si.mShownAsStarted) {
                    si.mShownAsStarted = true;
                    changed = true;
                }

            }

            return changed;
        }

        boolean updateSize(Context context, Debug.MemoryInfo mem, int curSeq) {
            mSize = ((long) mem.getTotalPss()) * 1024;
            if (mCurSeq == curSeq) {
                String sizeStr = Formatter.formatShortFileSize(
                        context, mSize);
                if (!sizeStr.equals(mSizeStr)) {
                    mSizeStr = sizeStr;
                    // We update this on the second tick where we update just
                    // the text in the current items, so no need to say we
                    // changed here.
                    return false;
                }
            }
            return false;
        }

        boolean buildDependencyChain(Context context, PackageManager pm, int curSeq) {
            final int NP = mDependentProcesses.size();
            boolean changed = false;
            for (int i = 0; i < NP; i++) {
                ProcessItem proc = mDependentProcesses.valueAt(i);
                if (proc.mClient != this) {
                    changed = true;
                    proc.mClient = this;
                }
                proc.mCurSeq = curSeq;
                proc.ensureLabel(pm);
                changed |= proc.buildDependencyChain(context, pm, curSeq);
            }

            if (mLastNumDependentProcesses != mDependentProcesses.size()) {
                changed = true;
                mLastNumDependentProcesses = mDependentProcesses.size();
            }

            return changed;
        }

        void addDependentProcesses(ArrayList<ProcessItem> destProc) {
            final int NP = mDependentProcesses.size();
            for (int i = 0; i < NP; i++) {
                ProcessItem proc = mDependentProcesses.valueAt(i);
                proc.addDependentProcesses(destProc);
                if (proc.mPid > 0) {
                    destProc.add(proc);
                }
            }
        }
    }

    public class MergedItem extends BaseItem {
        public ProcessItem mProcess;
        public boolean isChecked = true;
        public boolean issystem;
        final ArrayList<ProcessItem> mOtherProcesses = new ArrayList<ProcessItem>();
        final public ArrayList<ServiceItem> mServices = new ArrayList<ServiceItem>();

        private int mLastNumProcesses = -1, mLastNumServices = -1;

        MergedItem() {
            super(false);
        }

        boolean isSysProcess() {
            boolean misSysProcess = false;
            issystem = misSysProcess;
            
            Log.e(TAG, "isSysProcess mProcess.mProcessName = " + mProcess.mProcessName);

            try {
                ApplicationInfo ai = mPackageManager.getApplicationInfo(mProcess.mProcessName, 0);

                if (ai != null) {
                    misSysProcess = ai.sourceDir.startsWith("/system");
                }
            } catch (NameNotFoundException e) {

                try {
                    if (mProcess.mRunningProcessInfo == null) {
                        
                        if (mPackageInfo.packageName != null) {
                            ApplicationInfo mAppInfo = mPackageManager.getApplicationInfo(
                                    mPackageInfo.packageName, 0);

                            if (mAppInfo != null) {
                                misSysProcess = mAppInfo.sourceDir.startsWith("/system");
                            }
                        }
                        
                        
                        Log.e(TAG, "isSysProcess item.mProcess.mRunningProcessInfo == null misSysProcess = " + misSysProcess);
                        return misSysProcess;
                    } else if ((mProcess.mRunningProcessInfo != null)
                            && (mProcess.mRunningProcessInfo.pkgList == null)) {
                        Log.e(TAG, "isSysProcess item.mProcess.mRunningProcessInfo != null ");
                        if (mPackageInfo.packageName != null) {
                            ApplicationInfo mAppInfo = mPackageManager.getApplicationInfo(
                                    mPackageInfo.packageName, 0);

                            if (mAppInfo != null) {
                                misSysProcess = mAppInfo.sourceDir.startsWith("/system");
                            }
                        }
                        
                        return misSysProcess;
                    }

                    for (String mpkg : mProcess.mRunningProcessInfo.pkgList) {
                        Log.e(TAG, "isSysProcess mpkg=  " + mpkg);
                        if (mpkg != null) {
                            ApplicationInfo mAppInfo = mPackageManager.getApplicationInfo(
                                    mpkg, 0);

                            if (mAppInfo != null) {
                                misSysProcess = mAppInfo.sourceDir.startsWith("/system");
                            }
                        }
                    }
                } catch (NameNotFoundException err) {

                }

            }
            issystem = misSysProcess;
            return misSysProcess;
        }

        boolean update(Context context, boolean background) {
            mPackageInfo = mProcess.mPackageInfo;
            mDisplayLabel = mProcess.mDisplayLabel;
            mLabel = mProcess.mLabel;
            mBackground = background;

            if (!mBackground) {
                int numProcesses = (mProcess.mPid > 0 ? 1 : 0) + mOtherProcesses.size();
                int numServices = mServices.size();
                if (mLastNumProcesses != numProcesses || mLastNumServices != numServices) {
                    mLastNumProcesses = numProcesses;
                    mLastNumServices = numServices;
    
                }
            }

            mActiveSince = -1;
            for (int i = 0; i < mServices.size(); i++) {
                ServiceItem si = mServices.get(i);
                if (si.mActiveSince >= 0 && mActiveSince < si.mActiveSince) {
                    mActiveSince = si.mActiveSince;
                }
            }

            return false;
        }

        boolean updateSize(Context context) {
            mSize = mProcess.mSize;
            for (int i = 0; i < mOtherProcesses.size(); i++) {
                mSize += mOtherProcesses.get(i).mSize;
            }

            String sizeStr = Formatter.formatShortFileSize(
                    context, mSize);
            if (!sizeStr.equals(mSizeStr)) {
                mSizeStr = sizeStr;
                // We update this on the second tick where we update just
                // the text in the current items, so no need to say we
                // changed here.
                return false;
            }
            return false;
        }
    }

     class ServiceProcessComparator implements Comparator<MergedItem> {
        public int compare(MergedItem object1, MergedItem object2) {

            return ((object1.mSize == object2.mSize) ? 0 : ((object1.mSize > object2.mSize) ? -1
                    : 1));

        }
    }

     CharSequence makeLabel(PackageManager pm,
            String className, PackageItemInfo item) {
        if (item != null && (item.labelRes != 0
                || item.nonLocalizedLabel != null)) {
            CharSequence label = item.loadLabel(pm);
            if (label != null) {
                return label;
            }
        }

        String label = className;
        int tail = label.lastIndexOf('.');
        if (tail >= 0) {
            label = label.substring(tail + 1, label.length());
        }
        return label;
    }

     class ProcessComparator implements Comparator<MergedItem> {
        public int compare(MergedItem object1, MergedItem object2) {

            return ((object1.mSize == object2.mSize) ? 0 : ((object1.mSize > object2.mSize) ? -1
                    : 1));

        }
    }

    public ArrayList<MergedItem> getCurrentMergedItems() {
        synchronized (mLock) {
            return mMergedItems;
        }
    }

    public ArrayList<MergedItem> getCurrentBackgroundItems() {
        synchronized (mLock) {
            return mBackgroundItems;
        }
    }
}
