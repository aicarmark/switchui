/*
 * @(#)TimeFramesDetailComposer.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a15776       2012/12/01   NA               Initial Version of TimeFramesDetailComposer
 *
 */

package com.motorola.contextual.smartprofile.sensors.timesensor;

import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.Persistence;
import com.motorola.contextual.smartprofile.SmartProfileConfig;
import com.motorola.contextual.smartprofile.util.Util;
import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This class is  helps to compose details of TimeFrames
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
public class TimeFramesDetailComposer implements Constants, TimeFrameConstants, TimeFrameXmlSyntax {

    private static final String TAG = TimeFrames.class.getSimpleName();
    private static final String TF_TRUE = "TRUE";

    /**
     * This method gets config list from timeframe internal name
     * @param context
     * @param intName
     * @return list of configs
     */
    public static ArrayList<String> getConfigListByInternalName(Context context, String intName) {
        List<String> valueList = Persistence.retrieveValuesAsList(context, TIMEFRAME_CONFIG_PERSISTENCE);
        ArrayList<String> returnConfigList = new ArrayList<String>();
        for(String config : valueList) {
            if(config.contains(intName)) {
                returnConfigList.add(config);
            }
        }
        return returnConfigList;
    }


    /**
     * This method gets name of the CP
	 * @param  Application context
     * @return Name of CP
     */    
    public static final String getName(Context context) {
        return(context.getString(R.string.timeframes));
    }
    
    /**
     * This method gets config items for TimeFrames
	 * @param  Application context
     * @return config list for TimeFrames CP
     */    
    public static final String getConfigItems(Context context) {

        String supportedTimeFramesLevels[] = new String[0];
        String supportedTimeFramesLevelDesc[] = new String[0];

        return Util.generateXMLStringForList(context, supportedTimeFramesLevels, supportedTimeFramesLevelDesc);

    }
    
    /**
     * This method gets internal name from config
     * @param context
     * @param config
     * @return list of timeframe internal names
     */
    public static ArrayList<String>  getIntNameFromConfig(Context context, String config) {
        ArrayList<String> intNameList = new ArrayList<String>();

        if(LOG_INFO) Log.i(TAG,"config : "+ config);

        if(config.contains(TIMEFRAME_CONFIG_STRING)) {
			SmartProfileConfig profileConfig = new SmartProfileConfig(config);
			
			String value = profileConfig.getValue(TIMEFRAME_NAME);
			if(value == null) return intNameList;
			
			config = TimeUtil.trimBraces(value);
            StringTokenizer st = new StringTokenizer(config, OR_STRING);
            while (st.hasMoreTokens()) {
                intNameList.add(st.nextToken());
            }
        }

        return intNameList;
    }

    /**
     * This method gets state for config
     * @param context
     * @param status
     * @param config
     * @return state
     */
    static String getStateForConfig(Context context, String status, String config) {
        String state = (status.equals(TF_TRUE) ? TRUE : getStateForConfig(context, config));
        return state;
    }

    /**
     * This method gets state for config
     * @param context
     * @param config
     * @return state
     */
    static public String getStateForConfig(Context context, String config) {
        String state = FALSE;
        ArrayList<String> strList = TimeFramesDetailComposer.getIntNameFromConfig(context, config);;

        int size = strList.size();
        for(int i = 0; i<size; i++) {
            if(TimeUtil.isTimeFrameActive(context, strList.get(i)) == true) {
                state = TRUE;
                break;
            }
        }

        if(LOG_INFO) Log.i(TAG,"status : "+ state);
        return state;
    }

    /**
     * This method gets description for config
     * @param context
     * @param config
     * @return description
     */
    public static String getDescriptionForConfig(Context context, String config) {
        ArrayList<String> strList = TimeFramesDetailComposer.getIntNameFromConfig(context, config);
        StringBuilder descriptionBuilder = new StringBuilder();

        TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(
            context);
        String orSplitString = BLANK_SPC + context.getString(R.string.or) + BLANK_SPC;
        int size = strList.size();
        if (size <= 2) {
            for (int index = 0; index < size; index++) {
                String name = dbAdapter.getFriendlyNameForInternalName(strList.get(index));
                String friendlyName = (name != null) ? TimeUtil.getTranslatedTextForId(context, name) : "";
                if (index == 0) {
                    descriptionBuilder.append(friendlyName);
                } else {
                    descriptionBuilder.append(orSplitString).append(friendlyName);
                }
            }
        } else {
            String name = dbAdapter.getFriendlyNameForInternalName(strList.get(0));
            descriptionBuilder.append((name != null) ? TimeUtil.getTranslatedTextForId(context, name) : "")
            .append(orSplitString)
            .append(size - 1).append(BLANK_SPC)
            .append(context.getString(R.string.more));
        }

        dbAdapter.close();
        if(LOG_INFO) Log.i(TAG, "status : "+ descriptionBuilder);
        return descriptionBuilder.toString();
    }

    /**
     * This method gets updated config for a given config
     * @param context
     * @param config
     * @return description
     */
    public static String getUpdatedConfig(Context context, String config) {

        if(LOG_INFO) Log.i(TAG, "getUpdatedConfig - old : " +  config);
        if(config.contains(TIMEFRAME_OLD_CONFIG_PREFIX)) {
            String oldConfig = config.substring(config.indexOf(TIMEFRAME_OLD_CONFIG_PREFIX)+TIMEFRAME_OLD_CONFIG_PREFIX.length(), config.indexOf(END_STRING));
            config = TIMEFRAME_CONFIG_STRING+OPEN_B + oldConfig + CLOSE_B;
            SmartProfileConfig profileConfig = new SmartProfileConfig(config);
            profileConfig.addNameValuePair(CONFIG_VERSION, TIMEFRAME_CONFIG_VERSION);
            config = profileConfig.getConfigString();
        } else if (!config.contains(TIMEFRAME_CONFIG_STRING)) {
            config = null;
        }
        if(LOG_INFO) Log.i(TAG, "getUpdatedConfig  - new : " + config);
        return config;
    }

    /**
     * Validates config
     *
     * @param context
     * @param config
     * @return true/false - valid / invalid
     */
    public static boolean validateConfig(Context context, String config) {
    	
    	SmartProfileConfig profileConfig = new SmartProfileConfig(config);
        String version = profileConfig.getValue(CONFIG_VERSION);
        String value = profileConfig.getValue(TIMEFRAME_NAME);
        if(LOG_INFO) Log.i(TAG, "validateConfig : " +  version +  " : " +  value);
        return ((version != null) && (TIMEFRAME_CONFIG_VERSION.equals(version)) && (value != null)) ? true : false;
    }
}
