/*
 * @(#)DockDetailComposer.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491        2012/03/16 NA		          Initial version
 *
 */
package  com.motorola.contextual.pickers.conditions.dock;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.motorola.contextual.smartprofile.AbstractDetailComposer;
import com.motorola.contextual.smartprofile.SmartProfileConfig;
import com.motorola.contextual.smartprofile.util.Util;
import com.motorola.contextual.smartrules.R;

/**
 * This class is  helps to compose details of dock publisher
 *
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


public final class DockDetailComposer extends AbstractDetailComposer implements  DockConstants
{
    private final static String LOG_TAG = DockDetailComposer.class.getSimpleName();

    @Override
    public final String getDescription(Context context, String config) {
        String supportedDockLevelDesc[] = new String[] {
            context.getString(R.string.any), context.getString(R.string.desk),
            context.getString(R.string.hd), context.getString(R.string.car),
            context.getString(R.string.mobile)
        };


        for(int index = 0; index < DockActivity.sSupportedDockLevels.length; index++) {
            if(DockActivity.sSupportedDockLevels[index].equals(config)) {
                return supportedDockLevelDesc[index];
            }
        }
        return null;

    }

    @Override
    public  final String getConfigItems(Context context) {

        String supportedDockLevelDesc[] = new String[] {
            context.getString(R.string.any), context.getString(R.string.desk),
            context.getString(R.string.hd), context.getString(R.string.car),
            context.getString(R.string.mobile)
        };

        return Util.generateXMLStringForList(context, DockActivity.sSupportedDockLevels,
                                              supportedDockLevelDesc);

    }

    @Override
    public final String getName(Context context) {
        return(context.getString(R.string.dock));
    }

    @Override
    public final String getCurrentState(Context context, String config) {

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DOCK_EVENT);
        Intent androidIntent = context.registerReceiver(null, filter);
        int androidDockState = (androidIntent != null) ? androidIntent.getIntExtra(Intent.EXTRA_DOCK_STATE, 0) : 0;

        filter = new IntentFilter();
        filter.addAction(EXTRA_MOT_DOCK_STATE);
        Intent motIntent = context.registerReceiver(null, filter);
        int motDockState = (motIntent != null) ? motIntent.getIntExtra(EXTRA_MOT_DOCK_STATE, 0) : 0;

        String status = FALSE;

        if(config.equals(DOCK_ANY)) {
            status = ((androidDockState != Intent.EXTRA_DOCK_STATE_UNDOCKED) || (motDockState != Intent.EXTRA_DOCK_STATE_UNDOCKED)) ? TRUE : FALSE;
        } else if (config.equals(DOCK_DESK)) {
            status = (androidDockState == Intent.EXTRA_DOCK_STATE_DESK) ? TRUE : FALSE;
        } else if (config.equals(DOCK_CAR)) {
            status = (androidDockState == Intent.EXTRA_DOCK_STATE_CAR) ? TRUE : FALSE;
        } else if (config.equals(DOCK_HD)) {
            status = (motDockState == DOCK_EXTRA_HD) ? TRUE : FALSE;
        } else if (config.equals(DOCK_MOBILE)) {
            status = (motDockState == DOCK_EXTRA_MOBILE) ? TRUE : FALSE;
        }
        if(LOG_INFO) Log.i(LOG_TAG, "getCurrentState - state : " +   " : " + config + " : " +  status);
        return status;
    }

    @Override
    public final String getUpdatedConfig(Context context, String config) {

        if(LOG_INFO) Log.i(LOG_TAG, "getUpdatedConfig : old - " +    config);
        if(config.contains(OLD_CONFIG_PREFIX)) {
            String oldConfig = config.substring(config.indexOf(OLD_CONFIG_PREFIX)+OLD_CONFIG_PREFIX.length(), config.indexOf(END_STRING));
            config = NEW_DOCK_CONFIG_PREFIX + oldConfig;

            SmartProfileConfig profileConfig = new SmartProfileConfig(config);
            profileConfig.addNameValuePair(CONFIG_VERSION, DOCK_CONFIG_VERSION);
            config = profileConfig.getConfigString();
        } else {
            int index;
            for(index = 0; index < DockActivity.sSupportedDockLevels.length; index++) {
                if(DockActivity.sSupportedDockLevels[index].equals(config)) {
                    break;
                }
            }
            if(index == DockActivity.sSupportedDockLevels.length) {
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
        String value = profileConfig.getValue(NEW_DOCK_CONFIG_PREFIX_KEY);
        if(LOG_INFO) Log.i(LOG_TAG, "validateConfig : " +  version +  " : " +  value);
        return ((version != null) && (DOCK_CONFIG_VERSION.equals(version)) && (value != null)) ? true : false;
    }
}


