/*
 * @(#)DisplayDetailComposer.java
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
package  com.motorola.contextual.pickers.conditions.display;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import com.motorola.contextual.smartprofile.AbstractDetailComposer;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.SmartProfileConfig;
import com.motorola.contextual.smartprofile.util.Util;
import com.motorola.contextual.smartrules.R;

/**
 * This class helps to compose details of display publisher
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


public final class DisplayDetailComposer extends AbstractDetailComposer implements  Constants, DisplayConstants
{
    private final static String LOG_TAG = DisplayDetailComposer.class.getSimpleName();

    @Override
    public final String getDescription(Context context, String config) {
        String supportedDisplayLevelDesc[] = new String[] {
            context.getString(R.string.on),
            context.getString(R.string.off)
        };

        String supportedDisplayLevels[] = new String[] {ON, OFF};

        for(int index = 0; index < supportedDisplayLevels.length; index++) {
            if(supportedDisplayLevels[index].equals(config)) {
                return supportedDisplayLevelDesc[index];
            }
        }
        return null;

    }

    @Override
    public final String getConfigItems(Context context) {

        String supportedDisplayLevels[] = new String[] {ON, OFF};

        String supportedDisplayLevelDesc[] = new String[] {
            context.getString(R.string.on),
            context.getString(R.string.off)
        };

        return Util.generateXMLStringForList(context, supportedDisplayLevels, supportedDisplayLevelDesc);

    }

    @Override
    public final String getName(Context context) {
        return(context.getString(R.string.display));
    }

    @Override
    public final String getCurrentState(Context context, String config) {

        //Check Power Manager for current screen state
        PowerManager mPm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        
        String status = FALSE;
        if (config.equals(ON))
            status = mPm.isScreenOn() ? TRUE : FALSE ;
        else
            status = mPm.isScreenOn() ? FALSE : TRUE ;

        if(LOG_INFO) Log.i(LOG_TAG, "getCurrentState - state : " +   " : " + config + " : " +  status);
        return status;
    }

    @Override
    public final String getUpdatedConfig(Context context, String config) {
        String supportedDisplayLevels[] = new String[] {ON, OFF};
        if(LOG_INFO) Log.i(LOG_TAG, "getUpdatedConfig : " +   " : " + config);

        if(config.contains(OLD_CONFIG_PREFIX)) {
            String oldConfig = config.substring(config.indexOf(OLD_CONFIG_PREFIX)+OLD_CONFIG_PREFIX.length(), config.indexOf(END_STRING));
            config = NEW_DISPLAY_CONFIG_PREFIX + oldConfig.toUpperCase();

            SmartProfileConfig profileConfig = new SmartProfileConfig(config);
            profileConfig.addNameValuePair(CONFIG_VERSION, DISPLAY_CONFIG_VERSION);
            config = profileConfig.getConfigString();
        } else {
            boolean configFound = false;
            for(int index = 0; index < supportedDisplayLevels.length; index++) {
                if(supportedDisplayLevels[index].equals(config)) {
                    configFound = true;
                    break;
                }
            }
            if(configFound == false) {
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
        String value = profileConfig.getValue(NEW_DISPLAY_CONFIG_PREFIX_KEY);
        if(LOG_INFO) Log.i(LOG_TAG, "validateConfig : " +  version +  " : " +  value);
        return ((version != null) && (DISPLAY_CONFIG_VERSION.equals(version)) && (value != null)) ? true : false;
    }
}


