/**
 * Copyright (C) 2011, Motorola Mobility Inc,
 * All Rights Reserved
 * Class name: DataRetriever.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Feb 2, 2011        bluremployee      Created file
 **********************************************************
 */
package com.motorola.devicestatistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.util.Log;

import com.motorola.devicestatistics.StatsCollector.DataBundle;
import com.motorola.devicestatistics.StatsCollector.DataTypes;
import com.motorola.devicestatistics.StatsCollector.IJobComplete;

/**
 * @author bluremployee
 *
 */
/*package*/ class DataRetriever {
    interface IDataCollector {
        boolean canHandleType(int type);
        boolean doCollect(int type, Context context, ArrayList<DataBundle> result);
    }

    private final static boolean DUMP = true;
    private final static String TAG = "DataRetriever";
    
    private HashMap<Integer, IDataCollector> mCollectors;
    private Context mContext;
    
    public DataRetriever(Context context) {
        mContext = context;
        mCollectors = new HashMap<Integer, IDataCollector>();
    }
    
    public void doWork(long type, IJobComplete cb) {
        ArrayList<DataBundle> result = new ArrayList<DataBundle>();
        long todos = type;

        if(DUMP) Log.v(TAG, "To process jobs: " + todos);
        long l = Long.lowestOneBit(todos);
        while(l != 0) {
            int jobType = Long.numberOfTrailingZeros(l);

            if(DUMP) Log.v(TAG, "Job type to handle now:" + jobType);
            IDataCollector worker = getWorker(jobType);
            if(worker != null) {
                worker.doCollect(jobType, mContext, result);
            }
            todos = todos & (~l);
            l = Long.lowestOneBit(todos);
        }
        resetWorkers();
        if(cb != null) {
            cb.onJobDone(result);
        }
    }
    
    IDataCollector getWorker(int type) {
        IDataCollector worker = null;
        Iterator<IDataCollector> workers = mCollectors.values().iterator();
        boolean found = false;
        while(workers.hasNext()) {
            worker = workers.next();
            if(worker.canHandleType(type)) {
                found = true;
                break;
            }
        }
        if(!found) {
            worker = createWorker(type);
        }
        if(worker != null) {
            mCollectors.put(type, worker);
        }
        return worker;
    }
    
    IDataCollector createWorker(int type) {
        IDataCollector worker = null;
        switch(type) {
        case DataTypes.BATTERY_STATS:
            worker = new BatteryStatsCollector();
            break;
        case DataTypes.BATTERY_STATS_RESET:
            worker = new BatteryStatsResetHint();
            break;
        case DataTypes.MOBILE_DATA:
        case DataTypes.WIFI_DATA:
            break;
        default:
                break;
        }
        return worker;
    }
    
    void resetWorkers() {
        mCollectors.clear();
    }   
}

