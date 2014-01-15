/**
 * Copyright (C) 2011, Motorola Mobility Inc,
 * All Rights Reserved
 * Class name: BatteryStatsCollector.java
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

import android.content.Context;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BatteryStatsImpl;
import com.motorola.devicestatistics.DataRetriever.IDataCollector;
import com.motorola.devicestatistics.StatsCollector.DataBundle;
import com.motorola.devicestatistics.StatsCollector.DataTypes;
import com.motorola.devicestatistics.StatsCollector.StatsException;

/**
 * @author bluremployee
 *
 */
/*package*/ class BatteryStatsCollector implements IDataCollector {
        
    private final static String TAG = "BStatsCollector";

    /* (non-Javadoc)
     * @see com.motorola.devicestatistics.StatsCollector.DataRetriever.IDataCollector#canHandleType(int)
     */
    public boolean canHandleType(int type) {
        return type == DataTypes.BATTERY_STATS;
    }

    /* (non-Javadoc)
     * @see com.motorola.devicestatistics.StatsCollector.DataRetriever.IDataCollector#doCollect(int, android.content.Context, com.motorola.devicestatistics.StatsCollector.DataBundle)
     */
    public boolean doCollect(int type, Context context, ArrayList<DataBundle> result) {
        try {
            IBatteryStats iface = getStatsInterface();
                        
            BatteryStatsState state = collectStats(iface, context);
            if(state != null) {
                DataBundle myResult = DataBundle.create(DataTypes.BATTERY_STATS, state);
                result.add(myResult);
            }
        }catch(StatsException cEx) {
            Log.i(TAG, "doCollect for battey stats, got exception " + cEx.getMessage());
            return false;
        }
        
        return true;
    }
    
    private BatteryStatsState collectStats(IBatteryStats service, Context context)
            throws StatsException {
        byte[] data;
        try {
            data = service.getStatistics();
        } catch (RemoteException e) {
            throw new StatsException("Could not read statisitcs from service");
        }
        Parcel dataBundle = Parcel.obtain();
        dataBundle.unmarshall(data, 0, data.length);
        dataBundle.setDataPosition(0);
        
        BatteryStatsImpl stats = BatteryStatsImpl.CREATOR.createFromParcel(dataBundle);
        BatteryStatsState state = BatteryStatsState.createFromSource(stats, context);
        return state;
    }
    
    private IBatteryStats getStatsInterface() throws StatsException {
        IBatteryStats iface = IBatteryStats.Stub.asInterface(
                ServiceManager.getService("batteryinfo"));
        if(iface == null) {
            throw new StatsException("Could not obtain battery stats" +
                    " interface");
        }
        return iface;
    }

}

