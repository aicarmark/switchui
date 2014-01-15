package com.motorola.contextual.actions;

import com.motorola.contextual.actions.StatefulAction.Status;
import com.motorola.contextual.debug.DebugTable;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** This class is responsible for performing actions received by Settings onReceive
 * <code><pre>
 *
 * CLASS:
 *  Extends IntentService that handles asynchronous requests using a worker thread
 *
 * RESPONSIBILITIES:
 *  Interacts with the package manager and various actions.
 *
 * COLABORATORS:
 *  extends IntentService
 *  implements Constants
 *
 * USAGE:
 *  See each method.
 *
 * </pre></code>
 */

public class SettingsUtilityService extends IntentService implements Constants {

	private static final String TAG =  TAG_PREFIX + SettingsUtilityService.class.getSimpleName();
    
    /**
     * Default constructor
     */
    public SettingsUtilityService() {
        super("SettingsUtilityService");
    }

    /**
     * Constructor with name of the worker thread as an argument
     * @param name Name of the worker thread
     */
    public SettingsUtilityService(String name) {
        super(name);
    }
    
    @Override
    public void onDestroy() {      
        if(LOG_INFO) Log.i(TAG, "IntentService onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {   	
        Context context = getApplicationContext(); 
        
        if (intent == null ) {
            Log.w(TAG, "Null intent or action");
            return;
        }
        
        String action = intent.getStringExtra(EXTRA_INTENT_ACTION);

        if(LOG_INFO) Log.i(TAG, "onHandleIntent called. Action is "+action) ;

        String ruleKey = USER;
        String debugReqResp = Utils.generateReqRespKey();

        StatefulAction sai = StatefulActionHelper.convertBroadcastActionToActionInterface(context, action);

        if (sai == null) {
            Log.w(TAG, "Unrecognized setting change");
            return;
        }

        String actionKey = sai.getActionKey();
        if(actionKey.equals(setwallpaper.WALLPAPER_ACTION_KEY) ||
                (actionKey.equals(Wifi.WIFI_ACTION_KEY) &&
                (Persistence.retrieveValue(context, Wifi.WIFI_STATE_AFTER_TRANSITION) != null)) ||
                Persistence.retrieveValue(context, actionKey + MONITOR_SUFFIX) != null) {
            Status status = sai.handleSettingChange(context, intent);
            if (status.equals(Status.SUCCESS) || status.equals(Status.FAILURE)) {
                Persistence.removeValue(context, actionKey + MONITOR_SUFFIX);
                String settingString = sai.getSettingString(context);

                StatefulActionHelper.sendSettingsChange(context, actionKey, settingString);
                if (LOG_INFO) Log.i(TAG, "sendSettingsChange : " + actionKey);

                if (sai.getSettingToObserve() != null) {
                    //This action uses both broadcast receiver and service to monitor publisher state changes
                    //No need to monitor the changes now since user has changed the setting
                    StatefulActionHelper.deregisterFromSettingChanges(context, actionKey);
                }

                Utils.writeToDebugViewer(context, ruleKey, DebugTable.Direction.INTERNAL, QA_TO_MM, sai.getState(context),
                        debugReqResp, status.equals(Status.SUCCESS) ? SUCCESS : FAILURE, actionKey);
            }
        } else {
            if (LOG_INFO) Log.i(TAG, "Transient state or action status already sent");
        }  
    }
    
}
