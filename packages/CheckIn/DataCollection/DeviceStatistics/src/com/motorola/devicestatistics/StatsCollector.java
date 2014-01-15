/**
 * Copyright (C) 2011, Motorola Mobility Inc,
 * All Rights Reserved
 * Class name: StatsCollector.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Jan 20, 2011        bluremployee      Created file
 **********************************************************
 */
package com.motorola.devicestatistics;

import java.util.ArrayList;

import com.motorola.devicestatistics.util.Scheduler;
import com.motorola.devicestatistics.util.Scheduler.SchedulerException;

import android.content.Context;
import android.util.AndroidException;
import android.util.Log;

/**
 * @author bluremployee
 *
 */
public class StatsCollector {
    
    public final static class DataTypes {
        // Bit mask positions
        public final static int WIFI_DATA = 0;
        public final static int MOBILE_DATA = 1;
        public final static int BATTERY_STATS = 2;
        public final static int BATTERY_STATS_RESET = 3;
        // TODO: Add everything we collect through here
    }
    
    public final static class DataPolicy {
        // How data should be accumulated
        // We use two bits to encode this and hence we are allowed four values
        public final static int CUMULATIVE = 0;
        public final static int NON_CUMULATIVE = 1;
        public final static int PASS_THROUGH = 2;
    }
    
    static class StatsException extends AndroidException {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public StatsException(String cause) {
            super(cause);
        }
    }

    private final static boolean DUMP = true;
    private final static String TAG = "StatsCollector";
    
    DataRetriever mRetriever;
    DataAccumulator mAccumulator;
    DataBridge mBridge;
    
    interface IJobComplete {
        void onJobDone(ArrayList<DataBundle> result);
    }
    
    private static class DataBridge {
        private static class Job {
            long mType; // bit-mask of things to do
            Runnable mCb; // cb to call after this job is done
            
            public Job(long type, Runnable cb) {
                mType = type;
                mCb = cb;
            }
        }
        
        private DataRetriever mMaster;
        private DataAccumulator mSlave;
        private ArrayList<Job> mJobs;
        private Scheduler mScheduler;
        private Runnable mJobCompleteCb;
        private IJobComplete mMasterCb = new IJobComplete() {

            // TODO: Should we track status of each job - this is where we could
            public void onJobDone(ArrayList<DataBundle> result) {
                if(DUMP) Log.v(TAG, "Master reports done, pass result to slave");
                mSlave.doWork(result, mSlaveCb);
            }
            
        };
        private IJobComplete mSlaveCb = new IJobComplete() {

            public void onJobDone(ArrayList<DataBundle> result) {
                if(DUMP) Log.v(TAG, "Slave reports done.");
                synchronized(mJobs) {
                    jobDone();
                }
            }
            
        };
        
        public DataBridge(DataRetriever master, DataAccumulator client) {
            mMaster = master;
            mSlave = client;
            mJobs = new ArrayList<Job>();
            mScheduler = new Scheduler(Scheduler.Options.ALLOW_THREAD_SYNC_EXEC);
        }
        
        public boolean schedule(long type, boolean force, Runnable cb) {
            /* Logic:
             * A logical job is from schedule on us to cb from slave - so at
             * any point in time, either the master or the slave could be
             * processing it's part
             * If there is already a 'type' job on-going and we are not forced
             *            we will just return false
             * else we will tell the master what needs to be done
             * If there is a cb expected, we remember it and when the slave
             * informs us of completion, call it
             * TODO: We do jobs serially now, add provision for parallel
             * processing of independent jobs
             */
            // TODO: This lock should typically be inside isTypePending
            //       but we do this here to make the async job cb safe
            synchronized(mJobs) {
                if(isTypePending(type)) {
                    if(DUMP) Log.v(TAG, "Already pending job of type " + type
                            + " force, cb is " + force + "," + cb);
                    if(force && cb != null) {
                        mJobCompleteCb = cb;
                    }
                    return false;
                }
            }
            
            boolean start = false;
            Job j = new Job(type, cb);
            
            synchronized(mJobs) {
                start = mJobs.size() == 0;
                mJobs.add(j);
                if(start) {
                    startJob();
                }
            }
            return true;
        }
       
        // SHOULD be called under lock 
        private boolean isTypePending(long type) {
            int len = mJobs.size();
            for(int i = 0; i < len; ++i) {
                if((mJobs.get(i).mType & type) != 0) {
                    return true;
                }
            }
            return false;
        }
       
        // SHOULD be called under lock 
        private void startJob() {
            Job j = mJobs.get(0);
            final long type = j.mType;
            if(DUMP) Log.v(TAG, "Starting job of type " + type);
            try {
                mScheduler.schedule(new Runnable() {
    
                        public void run() {
                            try {
                            mMaster.doWork(type, mMasterCb);
                            }catch(Exception ex) {
                                Log.v(TAG, "Failed to do work for type: " + type, ex);
                            }
                        }
                        
                    }, Scheduler.Options.THREAD_SYNC_EXEC);
            }catch(SchedulerException schEx) {
                Log.v(TAG, "Scheduler exception trying to schedule " + type
                        + schEx.getMessage());
                jobDone(); 
            }
        }

        // SHOULD be called under lock
        private void jobDone() {
            Job j = mJobs.get(0);
            if(j.mCb != null) {
                j.mCb.run();
            }
            if(mJobCompleteCb != null) {
                mJobCompleteCb.run();
                mJobCompleteCb = null;
            }
            mJobs.remove(0);
            if(mJobs.size() > 0) {
                startJob();
            }
        }
    }
    
    public static class DataBundle {
        private int mDataType; // what type of data is this
        private Object mResult; // the data
        
        public DataBundle(int type, Object data) {
            mDataType = type;
            mResult = data;
        }
        
        public int getDataType() {
            return mDataType;
        }
        
        public Object getData() {
            return mResult;
        }
                
        public static DataBundle create(int dataType, Object data) {
            return new DataBundle(dataType, data);
        }
    }
    
    public StatsCollector(Context context) {
        mRetriever = new DataRetriever(context);
        mAccumulator = new DataAccumulator(context);
        mBridge = new DataBridge(mRetriever, mAccumulator);
    }
    
    public boolean schedule(int type, boolean force, Runnable cb) {
        if(DUMP) Log.v(TAG, "Schedule job " + type + " and force: " + force);
        return mBridge.schedule((long)(1 << type), force, cb);
    }
}

