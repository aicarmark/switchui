/**
 * Copyright (C) 2011, Motorola Mobility Inc,
 * All Rights Reserved
 * Class name: StatsTrigger.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Jan 20, 2011        bluremployee      Created file
 **********************************************************
 */
package com.motorola.devicestatistics;

import android.os.BatteryManager;
import android.util.Log;

import com.motorola.devicestatistics.StatsCollector.DataTypes;

/**
 * @author bluremployee
 *
 */
public class StatsTrigger {

    public interface ITriggerCb {
        void onFire(int type);
    }
    
    private final static boolean DUMP = true;
    private final static String TAG = "StatsTrigger";

    private final static int BATT_TRIGGER = 80;

    /*
     * @param level - current battery level
     * @param status - current plugged status
     * @param oldLevel - last battery level
     * @param oldStatus - last plugged status
     * @param lastStatus - last battery status (charging, discharging, full, etc)
     * @param chargeStartLevel - level at start of charge cycle if applicable
     */
    public void noteLevel(int level, int status, int oldLevel, int oldStatus,
            int lastStatus, int chargeStartLevel) {
        if(status != 0) {
            boolean call = false;
            call = (status != oldStatus && level >= BATT_TRIGGER) ||
                    (status == oldStatus && level != oldLevel && oldLevel < BATT_TRIGGER && level >= BATT_TRIGGER);
            if(call) {
                if(DUMP) Log.v(TAG, "Time to collect battery stats at " + level + "," + status);
                mCb.onFire(DataTypes.BATTERY_STATS);
            }
        }else if(status != oldStatus) {
            // The below are special conditions mirrored from how
            // battery stats resets itself - be careful if changing this
            if(lastStatus == BatteryManager.BATTERY_STATUS_FULL
                    || level >= 90) {
                    // Battery Stats doesn't do this right - so we shouldn't reset
                    //|| (chargeStartLevel != -1 && chargeStartLevel < 20 && level >= 80)) {
                if(DUMP) Log.v(TAG, "Time to note a reset hint " + lastStatus + ","
                        + chargeStartLevel + "," + level);
                mCb.onFire(DataTypes.BATTERY_STATS_RESET);
            }
        }
    }

    private ITriggerCb mCb;
    
    public StatsTrigger(ITriggerCb tcb) {
        mCb = tcb;
    }
    
}

