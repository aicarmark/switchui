/*
 * @(#)BatteryLevelDetailComposer.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491        2011/03/16 NA		          Initial version
 *
 */
package  com.motorola.contextual.pickers.conditions.battery;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;


import com.motorola.contextual.smartprofile.AbstractDetailComposer;
import com.motorola.contextual.smartprofile.SmartProfileConfig;
import com.motorola.contextual.smartprofile.util.Util;
import com.motorola.contextual.smartrules.R;

/**
 * This class helps to compose details of battery level publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *
 * RESPONSIBILITIES:
 * This class composes details of the publisher.
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public final class BatteryLevelDetailComposer extends AbstractDetailComposer  implements  BatteryLevelConstants
{

    private final static String LOG_TAG = BatteryLevelDetailComposer.class.getSimpleName();
    private final static String XML_LESS_THAN = "&lt;";

    @Override
    public final String getDescription(Context context, String config) {
        String supportedUIBatteryLevels[] = new String[] {
            LESS_THAN + BLANK_SPC + context.getString(R.string.battery_at_50),
            LESS_THAN + BLANK_SPC + context.getString(R.string.battery_at_35),
            LESS_THAN + BLANK_SPC + context.getString(R.string.battery_at_25),
            LESS_THAN + BLANK_SPC + context.getString(R.string.battery_at_10)
        };


        for(int index = 0; index < BatteryLevelActivity.sSupportedBatteryLevels.length; index++) {
            if(BatteryLevelActivity.sSupportedBatteryLevels[index].equals(config)) {
                return supportedUIBatteryLevels[index];
            }
        }
        return null;

    }

    @Override
    public final String getConfigItems(Context context) {

        String supportedUIBatteryLevels[] = new String[] {
            XML_LESS_THAN + BLANK_SPC + context.getString(R.string.battery_at_50),
            XML_LESS_THAN + BLANK_SPC + context.getString(R.string.battery_at_35),
            XML_LESS_THAN + BLANK_SPC + context.getString(R.string.battery_at_25),
            XML_LESS_THAN + BLANK_SPC + context.getString(R.string.battery_at_10)
        };

        return Util.generateXMLStringForList(context,
                                             BatteryLevelActivity.sSupportedBatteryLevels,
                                             supportedUIBatteryLevels);

    }

    @Override
    public  final String getName(Context context) {
        return(context.getString(R.string.battery_level));
    }

    @Override
    public final String getCurrentState(Context context, String config) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = context.registerReceiver(null, filter);

        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

        String status = FALSE;
        if(config.equals(BATTERY_LEVEL_50)) {
            status = (level < 50) ? TRUE : FALSE;
        } else if (config.equals(BATTERY_LEVEL_35)) {
            status = (level < 35) ? TRUE : FALSE;
        } else if (config.equals(BATTERY_LEVEL_25)) {
            status = (level < 25) ? TRUE : FALSE;
        } else if (config.equals(BATTERY_LEVEL_10)) {
            status = (level < 10) ? TRUE : FALSE;
        }

        return status;
    }

    @Override
    public final String getUpdatedConfig(Context context, String config) {

        if(LOG_INFO) Log.i(LOG_TAG, "getUpdatedConfig : " +   " : " + config);
        if(config.contains(OLD_BATTERY_LEVEL_CONFIG_PREFIX)) {
            String oldConfig = config.substring(config.indexOf(OLD_BATTERY_LEVEL_CONFIG_PREFIX)+OLD_BATTERY_LEVEL_CONFIG_PREFIX.length(), config.indexOf(END_STRING));
            config = NEW_BATTERY_LEVEL_CONFIG_PREFIX + oldConfig;

            SmartProfileConfig profileConfig = new SmartProfileConfig(config);
            profileConfig.addNameValuePair(CONFIG_VERSION, BATTERY_LEVEL_CONFIG_VERSION);
            config = profileConfig.getConfigString();
        } else {
            int index;
            for(index = 0; index < BatteryLevelActivity.sSupportedBatteryLevels.length; index++) {
                if(BatteryLevelActivity.sSupportedBatteryLevels[index].equals(config)) {
                    break;
                }
            }
            if(index == BatteryLevelActivity.sSupportedBatteryLevels.length) {
                config = null;
            }
        }
        if(LOG_INFO) Log.i(LOG_TAG, "getUpdatedConfig : new - "  + config);
        return config;
    }

    @Override
    public  boolean validateConfig(Context context, String config) {

        SmartProfileConfig profileConfig = new SmartProfileConfig(config);
        String version = profileConfig.getValue(CONFIG_VERSION);
        String value = profileConfig.getValue(NEW_BATTERY_LEVEL_CONFIG_PREFIX_KEY);
        if(LOG_INFO) Log.i(LOG_TAG, "validateConfig : " +  version +  " : " +  value);
        return ((version != null) && (BATTERY_LEVEL_CONFIG_VERSION.equals(version)) && (value != null)) ? true : false;
    }
}







