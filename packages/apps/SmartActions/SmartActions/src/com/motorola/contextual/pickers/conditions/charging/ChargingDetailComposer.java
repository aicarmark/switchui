/*
 * @(#)ChargingDetailComposer.java
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
package  com.motorola.contextual.pickers.conditions.charging;

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
 * This class helps to compose details of charging publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *
 * RESPONSIBILITIES:
 * This class helps to compose details of charging publisher
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */


public final class ChargingDetailComposer extends AbstractDetailComposer implements  ChargingConstants
{
    private final static String LOG_TAG = ChargingDetailComposer.class.getSimpleName();

    @Override
    public final String getDescription(final Context context, final String config) {
        final String supportedChargingLevelDesc[] = new String[] {
            context.getString(R.string.anysourcecharging),
            context.getString(R.string.accharging),
            context.getString(R.string.usbcharging),
            context.getString(R.string.notcharging)
        };


        for(int index = 0; index < ChargingActivity.sSupportedChargingLevels.length; index++) {
            if(ChargingActivity.sSupportedChargingLevels[index].equals(config)) {
                return supportedChargingLevelDesc[index];
            }
        }
        return null;

    }

    @Override
    public  final String getConfigItems(final Context context) {

        final String supportedChargingLevelDesc[] = new String[] {
            context.getString(R.string.anysourcecharging),
            context.getString(R.string.accharging),
            context.getString(R.string.usbcharging),
            context.getString(R.string.notcharging)
        };

        return Util.generateXMLStringForList(context,
        						             ChargingActivity.sSupportedChargingLevels,
        						             supportedChargingLevelDesc);


    }

    @Override
    public final String getName(final Context context) {
        return(context.getString(R.string.charging));
    }

    @Override
    public final String getCurrentState(final Context context, final String config) {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        final Intent intent = context.registerReceiver(null, filter);


        final int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);

        String status = FALSE;

        if(plugged == 0) {
            status = (config.equals(NOT_CHARGING)) ? TRUE : FALSE ;
        } else if(plugged == BatteryManager.BATTERY_PLUGGED_AC) {
            status = (config.equals(AC_CHARGING) || config.equals(USB_AC_CHARGING)) ? TRUE : FALSE ;
        } else if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {
            status = (config.equals(USB_CHARGING) || config.equals(USB_AC_CHARGING)) ? TRUE : FALSE ;
        }

        if(LOG_INFO) Log.i(LOG_TAG, "getCurrentState - state : " +   " : " + config + " : " + plugged + " : " + status);
        return status;
    }

    @Override
    public final String getUpdatedConfig(final Context context, String config) {

        if(LOG_INFO) Log.i(LOG_TAG, "getUpdatedConfig : " +   " : " + config);
        if(config.contains(OLD_CONFIG_PREFIX)) {
            final String oldConfig = config.substring(config.indexOf(OLD_CONFIG_PREFIX)+OLD_CONFIG_PREFIX.length(), config.indexOf(END_STRING));
            config = NEW_CHARGING_CONFIG_PREFIX + oldConfig;

            final SmartProfileConfig profileConfig = new SmartProfileConfig(config);
            profileConfig.addNameValuePair(CONFIG_VERSION, CHARGING_CONFIG_VERSION);
            config = profileConfig.getConfigString();
        } else {
            int index;
            for(index = 0; index < ChargingActivity.sSupportedChargingLevels.length; index++) {
                if(ChargingActivity.sSupportedChargingLevels[index].equals(config)) {
                    break;
                }
            }
            if(index == ChargingActivity.sSupportedChargingLevels.length) {
                config = null;
            }
        }
        if(LOG_INFO) Log.i(LOG_TAG, "getUpdatedConfig : new - "  + config);
        return config;
    }

    @Override
    public  boolean validateConfig(final Context context, final String config) {

    	final SmartProfileConfig profileConfig = new SmartProfileConfig(config);
        final String version = profileConfig.getValue(CONFIG_VERSION);
        final String value = profileConfig.getValue(NEW_CHARGING_CONFIG_PREFIX_KEY);
        if(LOG_INFO) Log.i(LOG_TAG, "validateConfig : " +  version +  " : " +  value);
        return ((version != null) && (CHARGING_CONFIG_VERSION.equals(version)) && (value != null)) ? true : false;
    }
}


