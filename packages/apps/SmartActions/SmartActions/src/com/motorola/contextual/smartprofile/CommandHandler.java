/*
 * @(#)CommandHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/15  NA                Initial version of CommandHandler
 *
 */

package com.motorola.contextual.smartprofile;

import java.util.HashMap;
import java.util.List;

import com.motorola.contextual.smartrules.monitorservice.CommonMonitorService;

import android.content.Context;
import android.content.Intent;
import android.util.Log;



/**
 * This class handles common processing of commands from Smart Actions Core or
 * any other client to the publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Implements Constants
 *
 * RESPONSIBILITIES:
 * Common functions such as registering/unregistering/saving/removing
 * configs
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public abstract class  CommandHandler implements Constants  {

    private static final String LOG_TAG = CommandHandler.class.getSimpleName();
    private static final String DETAIL_COMPOSER = "DetailComposer";
    private static final String RESPONSE = "response";
    private static final String UNDERSCORE = "_";
    /**
     * Abstract method to execute command
     * To be implemented by individual command handlers
     * @param context
     * @param intent
     */
    abstract protected String executeCommand(Context context, Intent intent);

    /**
     * Method to register state monitor with the common publisher service
     * @param context
     * @param stateMon - Individual publisher StateMonitor class
     */
    protected void registerReceiver(Context context, String stateMon) {
        Intent serviceIntent = new Intent(context, CommonMonitorService.class);
        serviceIntent.putExtra(EXTRA_STATE_MON_CLASS, stateMon);
        serviceIntent.putExtra(EXTRA_REGISTER, true);
        context.startService(serviceIntent);
    }
    /**
     * Method to unregister state monitor with the common publisher service
     * @param context
     * @param stateMon - Individual publisher StateMonitor class
     */
    protected void unregisterReceiver(Context context, String stateMon) {
        Intent serviceIntent = new Intent(context, CommonMonitorService.class);
        serviceIntent.putExtra(EXTRA_STATE_MON_CLASS, stateMon);
        serviceIntent.putExtra(EXTRA_REGISTER, false);
        context.startService(serviceIntent);
    }
    /**
     * Method to save monitored config in Persistence
     * @param context
     * @param intent
     * @param configName
     * @return registerReceiver - Whether this is the first config monitored, in which case receiver/observer
     *                            need to be registered.
     */
    protected final boolean saveMonitoredConfig(Context context, Intent intent, String configName) {
        String config = intent.getStringExtra(EXTRA_CONFIG);
        boolean registerReceiver = false;

        if(LOG_DEBUG) Log.d(LOG_TAG, "saveMonitoredConfig : " + config + " : " + configName);

        List<String> valueList = Persistence.retrieveValuesAsList(context, configName);

        // Register receiver only once for all the configs of this publisher
        if(valueList.isEmpty()) {
            registerReceiver = true;
        }

        if(!valueList.contains(config)) {
            valueList.add(config);
        }

        Persistence.commitValuesAsList(context, configName, valueList);

        return registerReceiver;
    }

    /**
     * Method to remove monitored config in Persistence
     * @param context
     * @param intent
     * @param configName
     * @return deregisterReceiver - Whether this is the last config monitored, in which case receiver/observer
     *                            need to be deregistered.
     */
    protected final boolean removeMonitoredConfig(Context context, Intent intent, String configName) {
        String config = intent.getStringExtra(EXTRA_CONFIG);
        boolean deregisterReceiver = false;


        if(LOG_DEBUG) Log.d(LOG_TAG, "removeMonitoredConfig : " + config + " : " + configName);

        List<String> valueList = Persistence.retrieveValuesAsList(context, configName);

        if(valueList.contains(config)) {
            valueList.remove(config);
        }

        Persistence.commitValuesAsList(context, configName, valueList);

        // Unregister receiver only after no config is dependent on this
        if(valueList.isEmpty()) {
            deregisterReceiver = true;
        }
        return deregisterReceiver;
    }

    /**
     * Construct common response for most of the commands
     * @param intent
     * @return responseIntent
     */
    protected final Intent constructCommonResponse(Intent intent) {
        Intent responseIntent = new Intent(ACTION_CONDITION_PUBLISHER_EVENT);
        //TODO : Interface version processing
        responseIntent.putExtra(EXTRA_STATUS, SUCCESS);

        responseIntent.putExtra(EXTRA_CONFIG, intent.getStringExtra(EXTRA_CONFIG));
        responseIntent.putExtra(EXTRA_RESPONSE_ID,intent.getStringExtra(EXTRA_REQUEST_ID));
        responseIntent.putExtra(EXTRA_PUB_KEY, intent.getAction());

        return responseIntent;

    }
    /**
     * Construct notification for different publishers
     * @param intent
     */
    public final static Intent constructNotification(HashMap<String, String> configStateMap, String pubKey) {
        Intent newIntent = new Intent(ACTION_CONDITION_PUBLISHER_EVENT);
        newIntent.putExtra(EXTRA_CONFIG_STATE_MAP, configStateMap);
        newIntent.putExtra(EXTRA_PUB_KEY, pubKey);
        newIntent.putExtra(EXTRA_EVENT_TYPE, NOTIFY);

        return newIntent;

    }
    /**
     * Construct failure response for publishers
     * @param intent
     * @param pubkey
     * @return responseIntent
     */
    protected static final Intent constructFailureResponse(Intent intent, String failureDescription) {
        Intent responseIntent = new Intent(intent.getStringExtra(ACTION_CONDITION_PUBLISHER_EVENT));

        //TODO : Interface version processing

        responseIntent.putExtra(EXTRA_STATUS, FAILURE);
        responseIntent.putExtra(EXTRA_CONFIG, intent.getStringExtra(EXTRA_CONFIG));
        responseIntent.putExtra(EXTRA_RESPONSE_ID,intent.getStringExtra(EXTRA_REQUEST_ID));
        responseIntent.putExtra(EXTRA_PUB_KEY, intent.getAction());
        responseIntent.putExtra(EXTRA_FAILURE_DESCRIPTION, failureDescription);
        String request = intent.getStringExtra(EXTRA_EVENT_TYPE);
        if(request != null) {
        	request = request.substring(0, (request.indexOf(UNDERSCORE)+1));
        	request += RESPONSE;
        }
        responseIntent.putExtra(EXTRA_EVENT_TYPE, request);
        return responseIntent;

    }

    /**
     * Instantiates detail composer for individual publishers dynamically
     * @param actName - name of the activity, along with condition key from manifest
     * @return detailComposer corresponding to the publisher
     */
    protected static final AbstractDetailComposer instantiateDetailComposer(String actName) {
        Class<?> cls = null;
        AbstractDetailComposer detailComposer = null;


        try {
            String conditionKey = actName.substring((actName.lastIndexOf(":")) + 1);
            actName = actName.substring(0, (actName.lastIndexOf(":")));
            if(LOG_DEBUG) Log.d(LOG_TAG, "Act name - " + actName + " : " + conditionKey);

            cls = Class.forName(actName.substring(0, (actName.lastIndexOf(".")+1)) + conditionKey + DETAIL_COMPOSER);

        } catch(ClassNotFoundException e) {
            if(LOG_DEBUG) Log.d(LOG_TAG, "Class not found " + actName.substring(0, (actName.lastIndexOf(".")+1)) + DETAIL_COMPOSER);
            return null;
        } catch (Exception e) {
        	Log.e(LOG_TAG, "Exception ");
        	return null;
        }
        try {
            detailComposer = (AbstractDetailComposer)cls.newInstance();
        } catch(IllegalAccessException e) {
            Log.e(LOG_TAG, "class instantiation failed: default ctor not visible ");
        } catch (InstantiationException e) {
            Log.e(LOG_TAG, "class instantiation failed: instance cannot be created ");
        }
        return detailComposer;
    }
}
