/*
 * @(#)BTDeviceInitHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2010/03/06  NA                Initial version of BTDeviceInitHandler
 *
 */

package com.motorola.contextual.pickers.conditions.bluetooth;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Persistence;
import com.motorola.contextual.smartprofile.SmartProfileConfig;



/**
 * This class handles "init" command from Smart Actions Core for BT Device
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *     Implements BTConstants
 *
 * RESPONSIBILITIES:
 * This class initializes BT Device Condition Publisher and sends current status
 * of configs associated with rules
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public final class BTDeviceInitHandler extends  CommandHandler implements BTConstants  {

    private final static String LOG_TAG = BTDeviceInitHandler.class.getSimpleName();

    @Override
    protected String executeCommand(Context context, Intent intent) {

        notifyAllConfigs(context);
        return SUCCESS;
    }

    /**
     * Method to notify current state for all configs
     *
     * @param context
     * @return none
     */
    private void notifyAllConfigs(Context context) {
        HashMap<String, String> configStateMap = new HashMap<String, String>();
        List<String> connectedList = Persistence.retrieveValuesAsList(context, BT_CONNECTED_LIST_PERSISTENCE);
        List<String> configList = Persistence.retrieveValuesAsList(context, BT_CONFIG_PERSISTENCE);

        int size = configList.size();
        for(int i=0; i<size; i++) {

            SmartProfileConfig profileConfig = new SmartProfileConfig(configList.get(i));
            String value = profileConfig.getValue(BT_NAME);
            if(value == null) return;
            String config = BTDeviceUtil.trimBraces(value);

            ArrayList<String> deviceList = BTDeviceDetailComposer.getDeviceListFromConfig(config);
            String state = FALSE;

            if(connectedList.isEmpty()) {
                state = FALSE;
            } else {
                boolean deviceConnected = false;
                for(String dev : deviceList) {
                    if(connectedList.contains(dev)) {
                        deviceConnected = true;
                        break;
                    }
                }

                if(deviceConnected == true) {
                    state = TRUE;
                }
            }

            configStateMap.put(configList.get(i), state);
        }
        if(!configList.isEmpty()) {
            Intent newIntent = CommandHandler.constructNotification(configStateMap, BT_PUB_KEY);
            context.sendBroadcast(newIntent, PERM_CONDITION_PUBLISHER_ADMIN);
            if(LOG_DEBUG) Log.d(LOG_TAG, "notifyAllConfigs : " +  " : " + configStateMap.toString());
        }
    }
}

