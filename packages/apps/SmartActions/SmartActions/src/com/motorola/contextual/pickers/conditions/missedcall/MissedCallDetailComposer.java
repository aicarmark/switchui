/*
 * @(#)MissedCallDetailComposer.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491        2012/03/16 NA                Initial version
 *
 */
package  com.motorola.contextual.pickers.conditions.missedcall;

import java.util.ArrayList;
import java.util.StringTokenizer;

import android.content.Context;
import android.util.Log;

import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.SmartProfileConfig;
import com.motorola.contextual.smartprofile.util.Util;
import com.motorola.contextual.smartrules.R;


/**
 * This class helps to compose details of missed call publisher
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
public final class MissedCallDetailComposer implements  Constants, MissedCallConstants
{
    private static final String TAG = MissedCallDetailComposer.class.getSimpleName();

    /**
     * This method gets number list from config
     *
     * @param config
     * @return list of configs
     */
    public static final ArrayList<String> getNumberListFromConfig(String numberConfig) {
        StringTokenizer st = new StringTokenizer(numberConfig, OR_STRING);
        ArrayList<String> numberList = new ArrayList<String>();

        if (LOG_DEBUG)
            Log.d(TAG, "updateDBDataFromConfig : " + numberConfig);

        while (st.hasMoreTokens()) {
            numberList.add(st.nextToken());
        }

        return numberList;
    }


    /**
     * This method gets name of the CP
     * @param  Application context
     * @return Name of CP
     */
    public static final String getName(Context context) {
        return(context.getString(R.string.MissedCall));
    }

    /**
     * This method gets config items for MissedCall
     * @param  Application context
     * @return config list for MissedCall CP
     */
    public static final String getConfigItems(Context context) {

        String supportedMissedCallLevels[] = new String[0];
        String supportedMissedCallLevelDesc[] = new String[0];

        return Util.generateXMLStringForList(context, supportedMissedCallLevels, supportedMissedCallLevelDesc);

    }

    /**
     * This method gets state from config
     * @param context
     * @param config
     * @return state
     */
    public static final String getStateForConfig(Context context, String config) {

        if(!config.contains(MISSED_CALLS_CONFIG_STRING)) return FALSE;

        SmartProfileConfig profileConfig = new SmartProfileConfig(config);
        String value = profileConfig.getValue(MISSED_CALLS_NAME);
        if(value == null) return FALSE;

        config = value.replace(MISSED_CALLS_CONFIG_STRING, "");

        ArrayList<String> numberList = getNumberListFromConfig(config.substring((OPEN_B).length(), config.indexOf(CLOSE_B)));
        String frequency = config.substring(config.indexOf(CLOSE_B+OPEN_B)+(CLOSE_B+OPEN_B).length());

        frequency = frequency.substring(0, frequency.indexOf(CLOSE_B));

        if (LOG_INFO)
            Log.i(TAG, " getStateForConfig : " + frequency);

        int maxCount = Integer.valueOf(frequency);
        MissedCallDBAdapter dbAdapter = new MissedCallDBAdapter(context);

        for(String number :  numberList)  {
            int currentCount = dbAdapter.getCurrentCount(number);
            dbAdapter.close();

            if(currentCount >= maxCount) {
                return TRUE;
            }
        }

        return FALSE;
    }


    /**
     * Gets description for given config
     *
     * @param context
     * @param config
     * @return description
     */
    public static String getDescriptionForConfig(Context context, String config) {

        if(!config.contains(MISSED_CALLS_CONFIG_STRING)) return "";

        SmartProfileConfig profileConfig = new SmartProfileConfig(config);
        String value = profileConfig.getValue(MISSED_CALLS_NAME);
        if(value == null) return null;
        config = value.replace(MISSED_CALLS_CONFIG_STRING, "");

        ArrayList<String> numberList = getNumberListFromConfig(config.substring((OPEN_B).length(), config.indexOf(CLOSE_B)));
        StringBuilder descBuffer = new StringBuilder();

        String frequency = config.substring(config.indexOf(CLOSE_B+OPEN_B)+(CLOSE_B+OPEN_B).length());
        frequency = frequency.substring(0, frequency.indexOf(CLOSE_B));

        if (LOG_INFO)
            Log.i(TAG, " getStateForConfig : " + frequency);

        descBuffer.append(frequency).append(BLANK_SPC);
        if (Integer.valueOf(frequency) == 1)
            descBuffer.append(context.getString(R.string.call));
        else
            descBuffer.append(context.getString(R.string.calls));
        descBuffer.append(BLANK_SPC)
        .append(context.getString(R.string.missed_call_from)).append(BLANK_SPC);

        ArrayList<String> nameList = MissedCallActivity.getNamesFromNumbers(context, numberList);

        descBuffer = MissedCallActivity.getDescBuffer(context, descBuffer, nameList, numberList);

        if (LOG_INFO)
            Log.i(TAG, "getDescriptionForConfig : " + descBuffer.toString());
        return descBuffer.toString();
    }

    /**
     * Gets updated config for a given config
     *
     * @param context
     * @param config
     * @return updated config
     */
    public static String getUpdatedConfig(Context context, String config) {

        if(LOG_INFO) Log.i(TAG, "getUpdatedConfig - old : " +  config);
        String frequency = null;
        if(config.contains(OLD_CONFIG_PREFIX)) {
            String oldNameAndNumber = config.substring(config.indexOf(OLD_CONFIG_PREFIX)+OLD_CONFIG_PREFIX.length(), config.indexOf(END_STRING));

            if(oldNameAndNumber.contains(MISSED_CALL_ANY_NUMBER) && oldNameAndNumber.contains(MISSED_CALLS_OLD_COUNT_SEPARATOR)) {
                frequency = oldNameAndNumber.substring(oldNameAndNumber.indexOf(MISSED_CALLS_OLD_COUNT_SEPARATOR)+1);
                config = MISSED_CALLS_CONFIG_STRING+OPEN_B+MISSED_CALL_ANY_NUMBER+CLOSE_B+OPEN_B+frequency+CLOSE_B;
            } else if((oldNameAndNumber.contains(MISSED_CALLS_OLD_COUNT_SEPARATOR)) && (oldNameAndNumber.contains(MISSED_CALLS_OLD_NAME_NUMBER_SEPARATOR))) {
                frequency = oldNameAndNumber.substring(oldNameAndNumber.indexOf(MISSED_CALLS_OLD_COUNT_SEPARATOR)+1, oldNameAndNumber.indexOf(MISSED_CALLS_OLD_NAME_NUMBER_SEPARATOR));
                oldNameAndNumber = oldNameAndNumber.substring(oldNameAndNumber.indexOf(MISSED_CALLS_OLD_NAME_NUMBER_SEPARATOR)+1);
                oldNameAndNumber = oldNameAndNumber.replaceAll(MISSED_CALLS_OLD_NUMBER_SEPARATOR, OR_STRING);

                if(LOG_DEBUG) Log.d(TAG, "getUpdatedConfig - final : "  + oldNameAndNumber);
                config = MISSED_CALLS_CONFIG_STRING + OPEN_B + oldNameAndNumber + CLOSE_B + OPEN_B+frequency+CLOSE_B;
            } else {
                config = null;
            }

            if (config != null) {
                SmartProfileConfig profileConfig = new SmartProfileConfig(config);
                profileConfig.addNameValuePair(MISSED_CALLS_CONFIG_VERSION, MISSED_CALLS_VERSION);
                config = profileConfig.getConfigString();
            }
        } else if (!config.contains(MISSED_CALLS_CONFIG_STRING)) {
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
        String version = profileConfig.getValue(MISSED_CALLS_CONFIG_VERSION);
        String value = profileConfig.getValue(MISSED_CALLS_NAME);
        if(LOG_INFO) Log.i(TAG, "validateConfig : " +  version +  " : " +  value);
        return ((version != null) && (MISSED_CALLS_VERSION.equals(version)) && (value != null)) ? true : false;
    }
}







