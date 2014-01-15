/*
 * @(#)BatteryLevel.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
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

package com.motorola.contextual.pickers.conditions.battery;


import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.pickers.conditions.DialogActivity;
import com.motorola.contextual.smartprofile.SmartProfileConfig;
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

    String BATTERY_LEVEL_STATE_MONITOR = "com.motorola.contextual.pickers.conditions.battery.BatteryLevelStateMonitor";

    String NEW_BATTERY_LEVEL_CONFIG_PREFIX = "BatteryLevel=";

    String NEW_BATTERY_LEVEL_CONFIG_PREFIX_KEY = "BatteryLevel";

    String OLD_BATTERY_LEVEL_CONFIG_PREFIX = "S.BATTERY_LEVEL=";

    String BATTERY_LEVEL_CONFIG_VERSION = "1.0";
}

/**
 * This class displays options for BatteryLevel precondition and allows the user to chose one
 * This also constructs the rule for BatteryLevel precondition and passes to the Condition Builder
 *
 *
 *<CODE><PRE>
 *
 * CLASS:
 *     Extends DialogActivity which is a super class for the dialog based PreConditions
 *     Implements ModeConstantsForRuleConstructor which contains constants for rule construction
 *     Implements BatteryLevels which holds Battery Level constants
 *
 * RESPONSIBILITIES:
 *     This class displays options for BatteryLevel precondition and allows the user to chose one
 *     This also constructs the rule for BatteryLevel precondition and passes to the Condition Builder
 *
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *    None
 * </PRE></CODE>
 **/
public final class BatteryLevelActivity extends DialogActivity implements BatteryLevelConstants {

    protected static final String sSupportedBatteryLevels[] = new String[] {
        BATTERY_LEVEL_50,
        BATTERY_LEVEL_35,
        BATTERY_LEVEL_25,
        BATTERY_LEVEL_10
    };

    private static final String TAG = BatteryLevelActivity.class.getSimpleName();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {

        // TODO Refactor based on cjd comment below
        // cjd - seems like all this battery level stuff should be encapsulated into a class. The presentation is being
        //        mixed with the battery level constants and such. It breaks specialization and encapsulation.
        //        That is, changing a level from let's say 50 to 70 should not require changes to the presentation layer code.
        // all that should be required here perhaps is to instantiate a class called something like BatteryLevel.
        final String supportedUIBatteryLevels[] = new String[] {
            getString(R.string.battery_at_50),
            getString(R.string.battery_at_35),
            getString(R.string.battery_at_25),
            getString(R.string.battery_at_10)
        };

        final String PERCENT = getString(R.string.percent);
        final String supportedBatteryLevelDescription[] = new String[] {
            LESS_THAN + BLANK_SPC + new SmartProfileConfig(BATTERY_LEVEL_50).getValue(NEW_BATTERY_LEVEL_CONFIG_PREFIX_KEY) + PERCENT,
            LESS_THAN + BLANK_SPC + new SmartProfileConfig(BATTERY_LEVEL_35).getValue(NEW_BATTERY_LEVEL_CONFIG_PREFIX_KEY) + PERCENT,
            LESS_THAN + BLANK_SPC + new SmartProfileConfig(BATTERY_LEVEL_25).getValue(NEW_BATTERY_LEVEL_CONFIG_PREFIX_KEY) + PERCENT,
            LESS_THAN + BLANK_SPC + new SmartProfileConfig(BATTERY_LEVEL_10).getValue(NEW_BATTERY_LEVEL_CONFIG_PREFIX_KEY) + PERCENT
        };

        final String supportedBatteryLevels[] = new String[] {
            BATTERY_LEVEL_50,
            BATTERY_LEVEL_35,
            BATTERY_LEVEL_25,
            BATTERY_LEVEL_10
        };

        // Update super class members
        mItems = supportedUIBatteryLevels;
        mModeDescption = supportedBatteryLevels;
        mDescription = new String[supportedBatteryLevelDescription.length];

        for(int index = 0; index < supportedBatteryLevelDescription.length; index++) {
            mDescription[index] = supportedBatteryLevelDescription[index];
        }

        mActionBarTitle = getString(R.string.battery_level_title);
        mTitle = this.getResources().getString(R.string.battery_level_prompt);

        if(LOG_INFO) Log.i(TAG, "Launched Battery Level Activity");

        super.onCreate(savedInstanceState);
    }
}
