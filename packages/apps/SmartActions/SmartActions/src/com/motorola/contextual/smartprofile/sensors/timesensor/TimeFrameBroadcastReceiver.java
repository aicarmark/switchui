/*
 * @(#)TimeFrameBroadcastReceiver.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a15776       2010/12/01   NA               Initial Version
 *
 */
package com.motorola.contextual.smartprofile.sensors.timesensor;

import java.net.URISyntaxException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;



/** This Receiver does the initializations that are required for the time frames.
 *  The operation could be time consuming as it has to access the database. So
 *  the operations are done inside the service TimeFrameService
 *
 *<code><pre>
 *
 * CLASS:
 *  Extends BroadcastReceiver which provides basic receiver functionality
 *
 * RESPONSIBILITIES:
 *  1. Starts the service TimeFrameService
 *
 * COLABORATORS:
 *  None
 *
 * USAGE:
 *  See each method.
 *
 *</pre></code>
 */
public class TimeFrameBroadcastReceiver extends BroadcastReceiver implements TimeFrameConstants {
    private static final String TAG = TimeFrameBroadcastReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (LOG_INFO) Log.i(TAG,"Received broadcast event: " + intent.toUri(0));
        if (action == null) {
            return;
        }

        if (action.equals(TIMEFRAME_INTENT_ACTION)) {
            // handle the intent from the alarm manager for time frame states
            onAlarmBroadcastReceive(context, intent);
        } else if (action.equals(SETALARM_INTENT_ACTION)) {
            // handle the intent from the alarm manager for time frame states
            onSetAlarmBroadcastReceive(context, intent);
        } else if (action.equals(TF_MODIFY_INTENT_ACTION)) {
            // handle the intent from the alarm manager for time frame states
            onModifyTimeFrameBroadcastReceive(context, intent);
        } else if (action.equals(TIMEFRAME_IMPORT_ACTION)) {
            // handle the intent from the Rules Exporter for time frame data
        	onImportTimeFrameBroadcastReceive(context, intent);
        } else if (action.equals(UPDATE_SMARTRULES_INTENT_ACTION)) {
            // handle the intent from the Rules Exporter for time frame data
        	onImportUpdateSmartRulesDBBroadcastReceive(context, intent);
        } else {
            // handle the intents from the system,for time frames registration
            onSystemBroadcastReceive(context, intent);
        }
    }





    /**
     * Handle the broadcasts from the system, that time-frames is interested in.
     * @param context - Application Context
     * @param intent  - Incoming Intent
     */
    private void onSystemBroadcastReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // android.intent.action.TEST - is just for testing purpose. to be removed later
        if ( (action != null) &&
                ((action.equals(SA_CORE_INIT_COMPLETE)) ||
                 (action.equals(TIME_ZONE_CHANGED)) ||
                 (action.equals(DATE_CHANGED)))
           )
        {
            //start the service that reads and registers all the time frames
            Intent serviceStartIntent = new Intent(context,TimeFrameService.class);
            ComponentName compName = context.startService(serviceStartIntent);
            if (compName == null) {
                Log.e(TAG,
                      "context.startService() failed for: " + TimeFrameService.class.getSimpleName());
            }
            else {
                if (LOG_INFO) Log.i(TAG, "Started Service - " + compName.flattenToString());
            }
        }
    }

    /**
     * Handle the broadcasts from the Alarm Manager
     * @param context - Application Context
     * @param intent  - Incoming Intent
     */
    private void onAlarmBroadcastReceive(Context context, Intent intent) {
        // check against the action is not required here, since it is already done in the
        // onReceive of the Broadcast Receiver. This method is called only if the
        // action matches with what we expect TIMEFRAME_INTENT_ACTION. A check here will
        // be redundant
        Intent serviceStartIntent = new Intent(TIMEFRAME_INTENT_ACTION);
        serviceStartIntent.putExtras(intent);
        serviceStartIntent.setClass(context,TimeFrameService.class);
        ComponentName compName = context.startService(serviceStartIntent);
        if (compName == null) {
            Log.e(TAG,
                  "context.startService() failed for: " + TimeFrameService.class.getSimpleName());
        }
        else {
            if (LOG_DEBUG) Log.d(TAG, "Started Service - " + compName.flattenToString());
        }
    }
    /**
     * Handle the requests for setting new Alarm with Alarm Manager
     * @param context - Application Context
     * @param intent  - Incoming Intent
     */
    private void onSetAlarmBroadcastReceive(Context context, Intent intent) {
        // check against the action is not required here, since it is already done in the
        // onReceive of the Broadcast Receiver. This method is called only if the
        // action matches with what we expect SETALARM_INTENT_ACTION. A check here will
        // be redundant
        // EXTRA_TEXT of this intent will contain URI of the intent that should be registered
        // with the Alam Manager.
        // EXTRA_SET_TIME of this intent will contain the delay time in minutes when the alarm should be set
        // EXTRA_TYPE of this intent will contain the Alarm type
        // extract the time frame name, time frame state and the time frame's internal name
        String uri = intent.getStringExtra(Intent.EXTRA_TEXT);
        int delay = intent.getIntExtra(EXTRA_SET_TIME, 0);
        int type = intent.getIntExtra(EXTRA_TYPE, 1);
        if (uri == null) {
            Log.e(TAG,"Error : Uri is NULL");
            return;
        }
        Intent alarmIntent = null;
        try {
            alarmIntent = Intent.parseUri(uri, 0);
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(TAG,"Error Parsing uri; uri = " + uri);
            return;
        }

        PendingIntent sender = PendingIntent.getBroadcast(
                                   context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager)context.getSystemService(
                              android.content.Context.ALARM_SERVICE);

        // clear previous pending intents and set new
        am.cancel(sender);

        long product = (long)delay * 60 * 1000;
        am.set(type, System.currentTimeMillis() + product, sender);
    }
    /**
     * Handle the requests for Modifying an existing Time Frame
     * or Add a new Time Frame
     * @param context - Application Context
     * @param intent  - Incoming Intent
     */
    private void onModifyTimeFrameBroadcastReceive(Context context, Intent intent) {
        // check against the action is not required here, since it is already done in the
        // onReceive of the Broadcast Receiver. This method is called only if the
        // action matches with what we expect TF_MODIFY_INTENT_ACTION. A check here will
        // be redundant
        // EXTRA_TEXT of this intent will contain URI of the intent that should be registered
        // with the Alam Manager.
        // EXTRA_SET_TIME of this intent will contain the delay time in minutes when the alarm should be set
        // EXTRA_TYPE of this intent will contain the Alarm type

        Intent serviceStartIntent = new Intent(TF_MODIFY_INTENT_ACTION);
        serviceStartIntent.putExtras(intent);
        serviceStartIntent.setClass(context,TimeFrameService.class);
        ComponentName compName = context.startService(serviceStartIntent);
        if (compName == null) {
            Log.e(TAG,
                  "context.startService() failed for: " + TimeFrameService.class.getSimpleName());
        }
        else {
            if (LOG_INFO) Log.i(TAG, "Started Service - " + compName.flattenToString());
        }
    }
    
    /**
     * Handle the requests for importing Time Frame
     * data from Rules Importer
     * @param context - Application Context
     * @param intent  - Incoming Intent
     */
    private void onImportTimeFrameBroadcastReceive(Context context, Intent intent) {
        // check against the action is not required here, since it is already done in the
        // onReceive of the Broadcast Receiver. This method is called only if the action
        // matches with what we expect TIMEFRAME_IMPORT_ACTION. A check here will be redundant.
        // EXTRA_TIMEFRAME_DATA of this intent will contain TimeFrame data in JSON format 
        Intent serviceStartIntent = new Intent(TIMEFRAME_IMPORT_ACTION);
        serviceStartIntent.putExtras(intent);
        serviceStartIntent.setClass(context,TimeFrameService.class);
        ComponentName compName = context.startService(serviceStartIntent);
        
        if (compName == null) {
            Log.e(TAG,
                  "context.startService() failed for: " + TimeFrameService.class.getSimpleName());
        }
        else {
            if (LOG_DEBUG) Log.d(TAG, "Started Service - " + compName.flattenToString());
        }
    }
    
    /**
     * Handle the requests for updating Condition Table in 
     * Smart Rules DB for a Suggested rule if the friendly name 
     * is not same as the one in timeframe DB
     * @param context - Application Context
     * @param intent  - Incoming Intent
     */
    private void onImportUpdateSmartRulesDBBroadcastReceive(Context context, Intent intent) {
    
        if (intent != null){
	        if (LOG_INFO) Log.i(TAG,"Received UPDATE_SMARTRULES_INTENT_ACTION " + intent.toUri(0));
	        Bundle bundle = intent.getExtras();
	        if(bundle != null){
		        Intent serviceStartIntent = new Intent(TIMEFRAME_UPDATE_SMARTRULES_ACTION);
		        serviceStartIntent.putExtras(bundle);
		        serviceStartIntent.setClass(context,TimeFrameService.class);
		        ComponentName compName = context.startService(serviceStartIntent);
		        
		        if (compName == null) {
		            Log.e(TAG,
		                  "context.startService() failed for: " + TimeFrameService.class.getSimpleName());
		        }
		        else {
		            if (LOG_INFO) Log.i(TAG, "Started Service - " + compName.flattenToString());
		        }
	        }else{
	        	Log.e(TAG,"No Extras in the Intent");
	        }
        }
    }
}
