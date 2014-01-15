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
import android.content.SharedPreferences;
import android.util.Log;

import com.motorola.devicestatistics.DataRetriever.IDataCollector;
import com.motorola.devicestatistics.StatsCollector.DataBundle;
import com.motorola.devicestatistics.StatsCollector.DataTypes;

/**
 * @author bluremployee
 *
 */
/*package*/ class BatteryStatsResetHint implements IDataCollector {
        
    private final static boolean DUMP = true;
    private final static String TAG = "BStatsResetHint";

    /* (non-Javadoc)
     * @see com.motorola.devicestatistics.StatsCollector.DataRetriever.IDataCollector#canHandleType(int)
     */
    public boolean canHandleType(int type) {
        return type == DataTypes.BATTERY_STATS_RESET;
    }

    /* (non-Javadoc)
     * @see com.motorola.devicestatistics.StatsCollector.DataRetriever.IDataCollector#doCollect(int, android.content.Context, com.motorola.devicestatistics.StatsCollector.DataBundle)
     */
    public boolean doCollect(int type, Context context, ArrayList<DataBundle> result) {
        setResetHint(context);
        return true;
    }

    private void setResetHint(Context context) {
        if(DUMP) Log.v(TAG, "setting reset hint");
        SharedPreferences sp = context.getSharedPreferences(DevStatPrefs.PREFS_FILE,
                    Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(DevStatPrefs.DEVSTATS_BATTRST_HINT, true);
        Utils.saveSharedPreferences(ed);
        return;
    }

    public static boolean readAndResetHint(Context context) {
        if(DUMP) Log.v(TAG, "reset hint cleared");
        SharedPreferences sp = context.getSharedPreferences(DevStatPrefs.PREFS_FILE,
                    Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        boolean reset = sp.getBoolean(DevStatPrefs.DEVSTATS_BATTRST_HINT, false);
        ed.putBoolean(DevStatPrefs.DEVSTATS_BATTRST_HINT, false);
        Utils.saveSharedPreferences(ed);
        return reset;
    } 
}

