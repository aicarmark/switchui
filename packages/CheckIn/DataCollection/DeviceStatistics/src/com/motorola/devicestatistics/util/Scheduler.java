/**
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: Scheduler.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Dec 31, 2010        bluremployee      Created file
 **********************************************************
 */
package com.motorola.devicestatistics.util;

import android.os.Process;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author bluremployee
 *
 */
public class Scheduler {
    private final static String LOG_TAG = "Scheduler";
    
    public static final class Options {
        // Options to be passed per runnable exec
        public static final int INLINE_EXEC = 0;
        public static final int THREAD_EXEC = 1;
        public static final int THREAD_SYNC_EXEC = 2;
        
        // Bit-mask to identify what kinds of scheduling are allowed
        // Cannot be changed once set on a Scheduler
        public static final int ALLOW_INLINE_EXEC = 1 << INLINE_EXEC;
        public static final int ALLOW_THREAD_EXEC = 1 << THREAD_EXEC;
        public static final int ALLOW_THREAD_SYNC_EXEC = 1 << THREAD_SYNC_EXEC;
        
        static final int VALID_MASK = ALLOW_INLINE_EXEC | 
                ALLOW_THREAD_EXEC | 
                ALLOW_THREAD_SYNC_EXEC;
        static final int MAX_CORE_THREADS = 2;
    }
    
    public static final class SchedulerException extends Exception {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public SchedulerException(String desc) {
            super(desc);
        }
    }
    
    private final int mOptions;
    private ScheduledExecutorService mSyncWorker;
    private ScheduledExecutorService mThreadedWorker;
    
    public Scheduler(int options) {
        mOptions = options & Options.VALID_MASK;
        init(mOptions);
    }

    /**
     * @param mOptions2
     */
    private void init(int options) {
        if((options & Options.ALLOW_THREAD_SYNC_EXEC) != 0) {
            mSyncWorker = Executors.newSingleThreadScheduledExecutor();
        }
        if((options & Options.ALLOW_THREAD_EXEC) != 0) {
            mThreadedWorker = Executors.newScheduledThreadPool(Options.MAX_CORE_THREADS);
        }
    }

    public void stop() {
        if((mOptions & Options.ALLOW_THREAD_SYNC_EXEC) != 0)
            mSyncWorker.shutdown();
        if((mOptions & Options.ALLOW_THREAD_EXEC) != 0)
            mThreadedWorker.shutdown();
    }
    
    public void schedule(Runnable work, int type) throws SchedulerException {
        if(work == null)
            throw new SchedulerException("Work is null");
        work = getLowPriorityRunnable(work);
        
        int option = 1 << type;
        if((option & mOptions) != 0) {
            switch(option) {
                case Options.ALLOW_INLINE_EXEC:
                    work.run();
                    break;
                case Options.ALLOW_THREAD_EXEC:
                    try {
                        mThreadedWorker.submit(work);
                    }catch(RejectedExecutionException reEx) {
                        throw new SchedulerException("Multi-thread exec submit failed");
                    }
                    break;
                case Options.ALLOW_THREAD_SYNC_EXEC:
                    try {
                        mSyncWorker.submit(work);
                    }catch(RejectedExecutionException reEx) {
                        throw new SchedulerException("Sync exec submit failed");
                    }
                    break;
                default:
                    break;
            }
        }else {
            throw new SchedulerException("Can't execute type:" + type
                    + " when i'm:" + mOptions);
        }
    }
    
    public void schedule(Runnable work, long delay, int type) throws SchedulerException {
        if(delay < 0)
            throw new SchedulerException("Negative delay:" + delay);
        if(work == null)
            throw new SchedulerException("Work is null");

        work = getLowPriorityRunnable(work);

        int option = 1 << type;
        if((option & mOptions) != 0) {
            switch(option) {
                case Options.ALLOW_INLINE_EXEC:
                    throw new SchedulerException("I don't support inline delayed exec");
                case Options.ALLOW_THREAD_EXEC:
                    try {
                        mThreadedWorker.schedule(work, delay, TimeUnit.MILLISECONDS);
                    }catch(RejectedExecutionException reEx) {
                        throw new SchedulerException("Multi-thread delayed exec submit failed");
                    }
                    break;
                case Options.ALLOW_THREAD_SYNC_EXEC:
                    try {
                        mSyncWorker.schedule(work, delay, TimeUnit.MILLISECONDS);
                    }catch(RejectedExecutionException reEx) {
                        throw new SchedulerException("Sync delayed exec submit failed");
                    }
                    break;
                default:
                    break;
            }
        }else {
            throw new SchedulerException("Can't execute type:" + type
                    + " when i'm:" + mOptions);
        }
    }

    private Runnable getLowPriorityRunnable(final Runnable work) {
        return new Runnable() {
            public void run() {
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT +
                            Process.THREAD_PRIORITY_LESS_FAVORABLE);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error setting priority", e);
                }
                work.run();
            }
        };
    }
}
