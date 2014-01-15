/**
 * Copyright (C) 2011, Motorola Mobility Inc,
 * All Rights Reserved
 * Class name: DataAccumulator.java
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
import com.motorola.devicestatistics.StatsCollector.DataPolicy;
import com.motorola.devicestatistics.StatsCollector.DataTypes;
import com.motorola.devicestatistics.StatsCollector.IJobComplete;

/**
 * @author bluremployee
 *
 */
/*package*/ class DataAccumulator {
    
    public static class AccumulatorPolicy {
        // For cumulative data class, change of class
        // may be needed in case of resets/partial resets
        public interface IConflictResolver {
            int getPolicy(long oldVal, long newVal, long refOld, long refNew);
        }
        
        public static IConflictResolver sSimpleConflictResolver =
                new IConflictResolver() {

                    public int getPolicy(long oldVal, long newVal,
                            long refOld, long refNew) {
                        int policy = DataPolicy.CUMULATIVE;
                        if(refNew <= refOld) {
                            // This should ideally never happen
                            // but since we can't decide we do a pass through
                            return DataPolicy.PASS_THROUGH;
                        }
                        long refDiff = refNew - refOld;
                        long valDiff = newVal - oldVal;
                        
                        /*
                         * Logic:
                         * If val diff is > 0, simple cumulative
                         * else if new val > refDiff, pass through // partial reset
                         * else non-cumulative // full reset
                         */
                        if(valDiff < 0) {
                            if(newVal > refDiff) {
                                policy = DataPolicy.PASS_THROUGH;
                            }else {
                                policy = DataPolicy.NON_CUMULATIVE;
                            }
                        }else if(valDiff > refDiff) {
                            policy = DataPolicy.PASS_THROUGH;
                        }
                        return policy;
                    }
            
        };
    }
    
    interface IAccumulator {
        boolean canHandleType(int type);
        boolean doAccumulate(int type, Context context, DataBundle result);
    }

    private final static boolean DUMP = true;
    private final static String TAG = "DataAccumulator";
    
    private HashMap<Integer, IAccumulator> mAccumulators;
    private Context mContext;
    
    public DataAccumulator(Context context) {
        mContext = context;
        mAccumulators = new HashMap<Integer, IAccumulator>();
    }
    
    public void doWork(ArrayList<DataBundle> result, IJobComplete cb) {
        if(result != null && result.size() > 0) {
            final int N = result.size();
            for(int i = 0; i < N; ++i) {
                DataBundle bundle = result.get(i);
                int type = bundle.getDataType();

                if(DUMP) Log.v(TAG, "Handling job " + i + " of type " + type);
                IAccumulator acc = getAccumulator(type);
                if(acc != null) {
                    acc.doAccumulate(type, mContext, bundle);
                }
            }
            resetAccumulators();
        }
        if(cb != null) {
            cb.onJobDone(null);
        }
    }
    
    IAccumulator getAccumulator(int type) {
        IAccumulator worker = null;
        Iterator<IAccumulator> workers = mAccumulators.values().iterator();
        boolean found = false;
        while(workers.hasNext()) {
            worker = workers.next();
            if(worker.canHandleType(type)) {
                found = true;
                break;
            }
        }
        if(!found) {
            worker = createAccumulator(type);
        }
        if(worker != null) {
            mAccumulators.put(type, worker);
        }
        return worker;
    }
    
    IAccumulator createAccumulator(int type) {
        IAccumulator worker = null;
        switch(type) {
        case DataTypes.BATTERY_STATS:
            worker = new BatteryStatsAccumulator();
            break;
        case DataTypes.BATTERY_STATS_RESET:
            break;
        case DataTypes.MOBILE_DATA:
        case DataTypes.WIFI_DATA:
            break;
        default:
                break;
        }
        return worker;
    }
    
    void resetAccumulators() {
        mAccumulators.clear();
    }
}

