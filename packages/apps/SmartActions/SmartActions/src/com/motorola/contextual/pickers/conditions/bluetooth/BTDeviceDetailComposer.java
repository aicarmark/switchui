/*
 * @(#)BTDeviceDetailComposer.java
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
package  com.motorola.contextual.pickers.conditions.bluetooth;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.Persistence;
import com.motorola.contextual.smartprofile.SmartProfileConfig;
import com.motorola.contextual.smartprofile.util.Util;
import com.motorola.contextual.smartrules.R;


/**
 * This class helps to compose details of bt devices publisher
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
public final class BTDeviceDetailComposer implements  Constants, BTConstants
{
    private static final String TAG = BTDeviceDetailComposer.class.getSimpleName();
    /**
     * This method gets device list from config
     *
     * @param config
     * @return list of configs
     */
    static public ArrayList<String> getDeviceListFromConfig(String config) {
        ArrayList<String> strList = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(config, OR_STRING);

        while (st.hasMoreTokens()) {
            strList.add(st.nextToken());
        }
        return strList;
    }

    /**
     * This method gets name of the CP
     * @param  Application context
     * @return Name of CP
     */
    public static final String getName(Context context) {
        return(context.getString(R.string.btconnectionwithaddress));
    }

    /**
     * This method gets config items for BT
     * @param  Application context
     * @return config list for BT CP
     */
    public static final String getConfigItems(Context context) {

        String supportedBTDeviceLevels[] = new String[0];
        String supportedBTDeviceLevelDesc[] = new String[0];

        return Util.generateXMLStringForList(context, supportedBTDeviceLevels, supportedBTDeviceLevelDesc);

    }

    /**
     * This method gets state from config
     * @param context
     * @param config
     * @return state
     */
    static public String getStateForConfig(Context context, String config) {

        String state = FALSE;

        SmartProfileConfig profileConfig = new SmartProfileConfig(config);
        String value = profileConfig.getValue(BT_NAME);
        if(value == null) return state;
        config = BTDeviceUtil.trimBraces(value);

        ArrayList<String> strList = getDeviceListFromConfig(config);

        List<String> valueList = Persistence.retrieveValuesAsList(context, BT_CONNECTED_LIST_PERSISTENCE);

        int size = strList.size();
        for(int i = 0; i<size; i++) {
            if(valueList.contains(strList.get(i))) {
                state = TRUE;
                break;
            }
        }

        if(LOG_INFO) Log.i(TAG, "status : "+ state);
        return state;
    }

    /**
     * Gets description for given config
     *
     * @param context
     * @param config
     * @return description
     */
    public static String getDescriptionForConfig(Context context, String config) {

        SmartProfileConfig profileConfig = new SmartProfileConfig(config);

        String value = profileConfig.getValue(BT_NAME);
        if(value == null) return null;

        config = BTDeviceUtil.trimBraces(value);

        ArrayList<String> intNameList = getDeviceListFromConfig(config);
        StringBuilder descBuffer = new StringBuilder();

        String orSplitString = BLANK_SPC + context.getString(R.string.or) + BLANK_SPC;

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (intNameList.size() <= NUM_DEVICE_NAME_SHOWN) {
            for (int index = 0; index < intNameList.size() ; index++) {
                descBuffer.append(btAdapter.getRemoteDevice(intNameList.get(index)).getName()).append(
                    index != (intNameList.size() - 1) ? orSplitString : "");
            }
        } else {
            descBuffer.append(btAdapter.getRemoteDevice(intNameList.get(0)).getName()).append(orSplitString)
            .append(intNameList.size() - 1).append(BLANK_SPC)
            .append(context.getString(R.string.more));
        }

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
        if(config.contains(OLD_CONFIG_PREFIX)) {
            String oldNameAndAddress = config.substring(config.indexOf(OLD_CONFIG_PREFIX)+OLD_CONFIG_PREFIX.length(), config.indexOf(END_STRING));
            if(oldNameAndAddress.contains(OLD_BT_DEVICES_NAME_ADDRESS_SEPARATOR))	{
                oldNameAndAddress = oldNameAndAddress.substring(oldNameAndAddress.indexOf(OLD_BT_DEVICES_NAME_ADDRESS_SEPARATOR)+1);
                if(oldNameAndAddress.contains(OLD_BT_DEVICES_ADDRESS_STRING_SEPARATOR)) oldNameAndAddress = oldNameAndAddress.replaceAll(OLD_BT_DEVICES_ADDRESS_SEPARATOR, OR_STRING);
                config = BT_CONFIG_STRING + LEFT_PAREN + oldNameAndAddress + RIGHT_PAREN;

                SmartProfileConfig profileConfig = new SmartProfileConfig(config);
                profileConfig.addNameValuePair(BT_CONFIG_VERSION, BT_VERSION);
                config = profileConfig.getConfigString();

            } else {
                config = null;
            }
        } else if (!config.contains(BT_CONFIG_STRING)) {
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
        String version = profileConfig.getValue(BT_CONFIG_VERSION);
        String value = profileConfig.getValue(BT_NAME);
        if(LOG_DEBUG) Log.d(TAG, "validateConfig : " +  version +  " : " +  value);
        return ((version != null) && (BT_VERSION.equals(version)) && (value != null)) ? true : false;
    }
}







