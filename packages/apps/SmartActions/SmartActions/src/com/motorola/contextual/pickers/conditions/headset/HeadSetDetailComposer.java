/*
 * @(#)HeadSetDetailComposer.java
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
package  com.motorola.contextual.pickers.conditions.headset;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.motorola.contextual.smartprofile.AbstractDetailComposer;
import com.motorola.contextual.smartprofile.SmartProfileConfig;
import com.motorola.contextual.smartprofile.util.Util;
import com.motorola.contextual.smartrules.R;

/**
 * This class helps to compose details of headset publisher
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


public final class HeadSetDetailComposer extends AbstractDetailComposer implements  HeadSetConstants
{
    private final static String LOG_TAG = HeadSetDetailComposer.class.getSimpleName();

    @Override
    public final String getDescription(final Context context, final String config) {
        final String supportedHeadsetLevelDesc[] = new String[] {
            context.getString(R.string.Connected),
            context.getString(R.string.NotConnected)
        };

        final String supportedHeadsetLevels[] = new String[] {CONNECTED, NOT_CONNECTED};

        for(int index = 0; index < supportedHeadsetLevels.length; index++) {
            if(supportedHeadsetLevels[index].equals(config)) {
                return supportedHeadsetLevelDesc[index];
            }
        }
        return null;

    }

    @Override
    public final String getConfigItems(final Context context) {

        final String supportedHeadsetLevelDesc[] = new String[] {
            context.getString(R.string.Connected),
            context.getString(R.string.NotConnected)
        };

        final String supportedHeadsetLevels[] = new String[] {CONNECTED, NOT_CONNECTED};

        return Util.generateXMLStringForList(context, supportedHeadsetLevels, supportedHeadsetLevelDesc);

    }

    @Override
    public final String getName(final Context context) {
        return(context.getString(R.string.headset));
    }

    @Override
    public final String getCurrentState(final Context context, final String config) {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        final Intent intent = context.registerReceiver(null, filter);


        final int state = (intent != null) ? intent.getIntExtra("state", 0) : 0;

        String status = FALSE;

        if(state == 1) {
            status = (config.equals(CONNECTED)) ? TRUE : FALSE ;
        } else if(state == 0) {
            status = (config.equals(NOT_CONNECTED)) ? TRUE : FALSE ;
        }

        if(LOG_INFO) Log.i(LOG_TAG, "getCurrentState - state : " +   " : " + config + " : " + state + " : " + status);
        return status;
    }

    @Override
    public final String getUpdatedConfig(final Context context, String config) {
        final String supportedHeadsetLevels[] = new String[] {CONNECTED, NOT_CONNECTED};

        if(config.contains(OLD_CONFIG_PREFIX)) {
            final String oldConfig = config.substring(config.indexOf(OLD_CONFIG_PREFIX)+OLD_CONFIG_PREFIX.length(), config.indexOf(END_STRING));
            config = NEW_HEADSET_CONFIG_PREFIX + oldConfig;
            final SmartProfileConfig profileConfig = new SmartProfileConfig(config);
            profileConfig.addNameValuePair(CONFIG_VERSION, HEADSET_CONFIG_VERSION);
            config = profileConfig.getConfigString();

        } else {
        	int index;
            for(index = 0; index < supportedHeadsetLevels.length; index++) {
                if(supportedHeadsetLevels[index].equals(config)) {
                    break;
                }
            }
            if(index == supportedHeadsetLevels.length) {
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
        final String value = profileConfig.getValue(NEW_HEADSET_CONFIG_PREFIX_KEY);
        if(LOG_INFO) Log.i(LOG_TAG, "validateConfig : " +  version +  " : " +  value);
        return ((version != null) && (HEADSET_CONFIG_VERSION.equals(version)) && (value != null)) ? true : false;
    }

}


