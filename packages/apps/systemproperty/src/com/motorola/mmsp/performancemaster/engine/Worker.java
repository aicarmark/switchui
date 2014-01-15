
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

/**
 * worker which handle all asynchronous jobs
 * 
 * @author dfb746
 */
public class Worker {
    private final static String TAG = "Worker";

    private static Worker mInstance = null;

    private int JOB_WAIT_TIME_MAX = 20; // 10 seconds

    private HashSet<Long> mObseleteThreadSet = null;

    /**
     * Job with TimeStamp when the Job been added
     */
    private class JobWithTS {
        public Job mJob;
        public Date mJoinDate;

        JobWithTS(Job job, Date date) {
            mJob = job;
            mJoinDate = date;
        }
    }

    /**
     * job list for all jobs need to be handled
     */
    private ArrayList<JobWithTS> mJobList = null;

    /**
     * thread handle all jobs, sleep when there's no job in mJobList
     */
    private Thread mWorkThread = null;

    /**
     * thread sequence num
     */
    private int mThreadNum = 0;

    private Worker() {
        mJobList = new ArrayList<JobWithTS>();
        mObseleteThreadSet = new HashSet<Long>();
        startNewThread();
    }

    private void startNewThread() {
        mWorkThread = new Thread(mWorkRunnable, "WorkerThread" + mThreadNum++);
        mWorkThread.start();
    }

    public static Worker getInstance() {
        if (mInstance == null) {
            mInstance = new Worker();
        }

        return mInstance;
    }

    private Runnable mWorkRunnable = new Runnable() {
        public void run() {
            while (true) {
                Job job = null;
                synchronized (mJobList) {
                    long tID = Thread.currentThread().getId();
                    if (mObseleteThreadSet.contains(tID)) {
                        mObseleteThreadSet.remove(tID);
                        Log.e(TAG, "Error WorkerThread EXIT " + Thread.currentThread().getName());
                        break;
                    }
                    while (mJobList.size() == 0) {
                        try {
                            Log.i(TAG, "no job, Worker thread go sleeping....");
                            mJobList.wait();
                            Log.i(TAG, "new job, Worker thread wakeup....");
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            Log.e(TAG, "InterruptedException when call wait()");
                        }
                    }

                    job = mJobList.get(0).mJob;
                    mJobList.remove(0);
                }

                if (job != null) {
                    job.doJob();
                    job.jobDone();
                }
            }
        }
    };

    public void addJob(Job job) {
        synchronized (mJobList) {
            checkThreadStatus();

            // added the new Job
            mJobList.add(new JobWithTS(job, new Date()));
            mJobList.notify();
        }
    }

    /**
     * if the next job wait too long, start a new thread to process Joblist set
     * a flag for the old thread which used to exit the first thread
     */
    private void checkThreadStatus() {
        if (mJobList.isEmpty()) {
            return;
        }

        // check if the next Job wait too long
        Date date = new Date();
        if (date.getTime() - mJobList.get(0).mJoinDate.getTime() > JOB_WAIT_TIME_MAX * 1000) {
            Log.e(TAG, "Error, job wait too long, start new Thread...");
            mObseleteThreadSet.add(mWorkThread.getId());
            startNewThread();
        }
    }
}
