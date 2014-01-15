/*
 * @(#)DockReceiver.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * a18491        2012/03/16 NA                Initial version
 *
 */
package  com.motorola.contextual.pickers.conditions.dock;

import java.util.HashMap;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.Persistence;


/**
 * This is a Broadcast receiver which receives the broadcasts for dock publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Implements Constants
 *      Implements DockConstants
 *      Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 * Broadcats "notify" intents when it receives dock mode change indications
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class DockReceiver extends BroadcastReceiver implements Constants, DockConstants {

    private final static String LOG_TAG = DockReceiver.class.getSimpleName();


    @Override
    public void onReceive(Context context, Intent intent) {

        String action = null;

        if(intent == null || ((action = intent.getAction()) == null)) {
            Log.w(LOG_TAG, " Null intent received ");
        } else if(action.equals(Intent.ACTION_DOCK_EVENT)) {
            if(LOG_INFO) Log.i(LOG_TAG, "Received Broadcast - state : ACTION_DOCK_EVENT");
            postNotifyForAndroidDocks(context, intent.getIntExtra(Intent.EXTRA_DOCK_STATE, 0));
        } else if(action.equals(ACTION_MOT_DOCK_EVENT)) {
            if(LOG_INFO) Log.i(LOG_TAG, "Received Broadcast - state : ACTION_MOT_DOCK_EVENT");
            postNotifyForMotoDocks(context, intent.getIntExtra(EXTRA_MOT_DOCK_STATE, 0));
        }
    }

    /**
     * The method posts notify message for related configs when state changes
     * for Android docks
     * @param context
     * @param state
     */
    protected static final void postNotifyForAndroidDocks(Context context, int state) {
        HashMap<String, String> configStateMap = new HashMap<String, String>();

        List<String> valueList = Persistence.retrieveValuesAsList(context, DOCK_PERSISTENCE);

        if(valueList.isEmpty()) {
            return;
        }
        if(valueList.contains(DOCK_ANY)) {
            configStateMap.put(DOCK_ANY, (state != Intent.EXTRA_DOCK_STATE_UNDOCKED) ? TRUE : FALSE);
        }
        if(valueList.contains(DOCK_CAR)) {
            configStateMap.put(DOCK_CAR, (state == Intent.EXTRA_DOCK_STATE_CAR) ? TRUE : FALSE);
        }
        if(valueList.contains(DOCK_DESK)) {
            configStateMap.put(DOCK_DESK, (state == Intent.EXTRA_DOCK_STATE_DESK) ? TRUE : FALSE);
        }
        if(!configStateMap.isEmpty()) {
            Intent newIntent = CommandHandler.constructNotification(configStateMap, DOCK_PUB_KEY);
            context.sendBroadcast(newIntent, PERM_CONDITION_PUBLISHER_ADMIN);
            if(LOG_INFO) Log.i(LOG_TAG, "postNotify : " +   " : " + configStateMap.toString());
        }

    }

    /**
     * The method posts notify message for related configs when state changes
     * for Moto docks
     * @param context
     * @param state
     */
    protected static final void postNotifyForMotoDocks(Context context, int state) {
        HashMap<String, String> configStateMap = new HashMap<String, String>();

        List<String> valueList = Persistence.retrieveValuesAsList(context, DOCK_PERSISTENCE);

        if(valueList.isEmpty()) {
            return;
        }
        if(valueList.contains(DOCK_ANY)) {
            configStateMap.put(DOCK_ANY, (state != Intent.EXTRA_DOCK_STATE_UNDOCKED) ? TRUE : FALSE);
        }
        if(valueList.contains(DOCK_HD)) {
            configStateMap.put(DOCK_HD, (state == DOCK_EXTRA_HD) ? TRUE : FALSE);
        }
        if(valueList.contains(DOCK_MOBILE)) {
            configStateMap.put(DOCK_MOBILE, (state == DOCK_EXTRA_MOBILE) ? TRUE : FALSE);
        }
        if(!configStateMap.isEmpty()) {
            Intent newIntent = CommandHandler.constructNotification(configStateMap, DOCK_PUB_KEY);
            context.sendBroadcast(newIntent, PERM_CONDITION_PUBLISHER_ADMIN);
            if(LOG_INFO) Log.i(LOG_TAG, "postNotify : " +   " : " + configStateMap.toString());
        }
    }

}
