/*
 * @(#)BatteryLevelActivity.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21693        2010/10/20 NA		          Initial version
 * a18491        2011/2/17  NA                Incorporated first set of review
 *                                            comments
 */

package com.motorola.contextual.smartprofile.sensors.batterysensor;


import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.smartprofile.DialogActivity;
import com.motorola.contextual.smartrules.R;



/**
 * Constants for Battery levels
 *
 */
interface BatteryLevelConstants {

    String BATTERY_LEVEL_50 = "BatteryLevel=50;Version=1.0";
    String BATTERY_LEVEL_35 = "BatteryLevel=35;Version=1.0";
    String BATTERY_LEVEL_25 = "BatteryLevel=25;Version=1.0";
    String BATTERY_LEVEL_10 = "BatteryLevel=10;Version=1.0";

    String BATTERY_LEVEL_PERSISTENCE = "com.motorola.contextual.smartprofile.battery.persistence";

    String BATTERY_PUB_KEY = "com.motorola.contextual.smartprofile.battery";

    String BATTERY_LEVEL_STATE_MONITOR = "com.motorola.contextual.smartprofile.sensors.batterysensor.BatteryLevelStateMonitor";

    String NEW_BATTERY_LEVEL_CONFIG_PREFIX = "BatteryLevel=";

    String NEW_BATTERY_LEVEL_CONFIG_PREFIX_KEY = "BatteryLevel";
    
    String OLD_BATTERY_LEVEL_CONFIG_PREFIX = "S.BATTERY_LEVEL=";

    String BATTERY_LEVEL_CONFIG_VERSION = "1.0";
}
/**
 * This class displays options for BatteryLevel precondition and allows the user to chose one
 *
 *
 *<CODE><PRE>
 *
 * CLASS:
 *     Extends DialogActivity which is a super class for the dialog based PreConditions
 *     Implements BatteryLevels which holds Battery Level constants
 *
 * RESPONSIBILITIES:
 *     This class displays options for BatteryLevel precondition and allows the user to chose one.
 *     This also provides config information chosen by the user to Smart Actions Core.
 *
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *    None
 * </PRE></CODE>
 **/
public final class BatteryLevelActivity extends DialogActivity implements BatteryLevelConstants {

    private static final String TAG = BatteryLevelActivity.class.getSimpleName();

    protected static final String sSupportedBatteryLevels[] = new String[] {
        BATTERY_LEVEL_50,
        BATTERY_LEVEL_35,
        BATTERY_LEVEL_25,
        BATTERY_LEVEL_10
    };


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String supportedUIBatteryLevels[] = new String[] {
            LESS_THAN + BLANK_SPC + getString(R.string.battery_at_50),
            LESS_THAN + BLANK_SPC + getString(R.string.battery_at_35),
            LESS_THAN + BLANK_SPC + getString(R.string.battery_at_25),
            LESS_THAN + BLANK_SPC + getString(R.string.battery_at_10)
        };


        mItems = supportedUIBatteryLevels;
        mModeDescption = sSupportedBatteryLevels;
        mDescription = supportedUIBatteryLevels;

        mTitle = getString(R.string.battery_level);
        mIcon = R.drawable.ic_dialog_battery_level;

        if(LOG_DEBUG) Log.d(TAG, "Launched Battery Level Activity");

        // Do a show Dialog, only when the activity is first created, don't do it for
        // orientation changes
        if (savedInstanceState  == null) super.showDialog();

    }
}
