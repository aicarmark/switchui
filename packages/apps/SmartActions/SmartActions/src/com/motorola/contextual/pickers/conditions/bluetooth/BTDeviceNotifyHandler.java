/*
 * @(#)BTDeviceNotifyHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2010/03/06  NA                Initial version of BTDeviceNotifyHandler
 *
 */

package com.motorola.contextual.pickers.conditions.bluetooth;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Persistence;



/**
 * This class handles "notify" command for BT Connection With Address
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *     Implements BTConstants
 *
 * RESPONSIBILITIES:
 * This class handles "notify" command for BT Connection With Address
 * Condition Publisher
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public final class BTDeviceNotifyHandler extends  CommandHandler implements BTConstants  {

    private final static String LOG_TAG = BTDeviceNotifyHandler.class.getSimpleName();

    @Override
    protected String executeCommand(Context context, Intent intent) {

        // Returning FAILURE here doesn't make sense, since this command handler is internally
        // generated and no response is expected from this command handler
        if(intent == null) {
            Log.w(LOG_TAG, " Null intent received ");
        } else {
            String btAction = intent.getStringExtra(EXTRA_BT_ACTION);

            if(btAction == null) {
                return SUCCESS;
            }
            if(btAction.equals(BluetoothDevice.ACTION_ACL_CONNECTED) ||
                    btAction.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) {
                    Log.e(LOG_TAG, "device null");
                    return NO_RESPONSE;
                }
                updateConnectedListPersistence(context, btAction, device.getAddress());
                postNotify(context, btAction, device.getAddress());
            } else if(btAction.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED) ||
                      btAction.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int nstate = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                                BluetoothAdapter.ERROR);
                if (nstate == BluetoothAdapter.STATE_OFF) {
                    updateConnectedListPersistence(context,btAction, null);
                    postNotify(context, btAction, null);
                }
            } else {
                Log.e(LOG_TAG, "Action not known -" + btAction);
            }
        }
        return SUCCESS;
    }

    /**
     * Updates persistence for BT connection
     * @param context
     * @param btAction
     * @param macAddress
     */
    protected static final void updateConnectedListPersistence(Context context, String btAction, String macAddress) {

        if(LOG_DEBUG) Log.d(LOG_TAG, "updateConnectedListPersistence -  " + btAction + " : " + macAddress);
        List<String> valueList = Persistence.retrieveValuesAsList(context, BT_CONNECTED_LIST_PERSISTENCE);
        if(btAction.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
            if(!valueList.contains(macAddress)) {
                valueList.add(macAddress);
            }
        } else if(btAction.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
            if(valueList.contains(macAddress)) {
                valueList.remove(macAddress);
            }
        } else {
            valueList.clear();
        }

        Persistence.commitValuesAsList(context, BT_CONNECTED_LIST_PERSISTENCE, valueList);
    }

    /**
     * The method posts notify message for related configs when state changes
     * for BT Device Condition Publisher
     * @param context
     * @param btAction
     * @param macAddress
     *
     */
    protected static final void postNotify(Context context, String btAction, String macAddress) {
        ArrayList<String> configs = getConfigListByDeviceAddress(context, macAddress, btAction);
        HashMap<String, String> configStateMap = new HashMap<String, String>();

        int size = configs.size();
        for(int index = 0; index < size; index++) {
            configStateMap.put(configs.get(index), getStateForConfig(context, btAction, configs.get(index)));
        }

        if(!configs.isEmpty()) {
            Intent newIntent = CommandHandler.constructNotification(configStateMap, BT_PUB_KEY);
            context.sendBroadcast(newIntent, PERM_CONDITION_PUBLISHER_ADMIN);
            if(LOG_INFO) Log.i(LOG_TAG, "postNotify : " +   " : " + configs + " : " + configStateMap.toString());
        }

    }

    /**
     * The method gets config list by device address for BT Device Condition Publisher
     * @param context
     * @param macAddress
     * @param btAction
     * @return config list
     *
     */
    static ArrayList<String> getConfigListByDeviceAddress(Context context, String macAddress, String btAction) {
        List<String> valueList = Persistence.retrieveValuesAsList(context, BT_CONFIG_PERSISTENCE);

        if(btAction.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED) ||
           btAction.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            return (ArrayList<String>)valueList;
        }
        ArrayList<String> returnConfigList = new ArrayList<String>();
        for(String config : valueList) {
            if(config.contains(macAddress)) {
                returnConfigList.add(config);
            }
        }
        return returnConfigList;
    }

    /**
     * The method gets state for given config
     * @param context
     * @param btAction
     * @param config
     * @return current state
     *
     */
    static String getStateForConfig(Context context, String btAction, String config) {
        String state = null;

        if(btAction.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
            state = TRUE;
        } else if(btAction.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED) ||
                  btAction.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            state = FALSE;
        } else {
            state = BTDeviceDetailComposer.getStateForConfig(context, config);
        }
        return state;
    }
}

