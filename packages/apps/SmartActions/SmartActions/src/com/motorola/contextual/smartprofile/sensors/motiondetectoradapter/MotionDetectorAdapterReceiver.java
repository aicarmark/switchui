/*
 * @(#)MotionDetectorAdapterReceiver.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * a18491        2012/03/16 NA                Initial version of MotionDetectorAdapterReceiver
 *
 */
package  com.motorola.contextual.smartprofile.sensors.motiondetectoradapter;

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
 * This is a Broadcast receiver which receives the broadcasts for motion detection adapter publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Implements Constants
 *      Implements MotionConstants
 *      Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 * Broadcats "notify" intents when it receives motion detection adapter mode change indications
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class MotionDetectorAdapterReceiver extends BroadcastReceiver implements Constants, MotionConstants {

    private final static String LOG_TAG = MotionDetectorAdapterReceiver.class.getSimpleName();


    @Override
    public void onReceive(Context context, Intent intent) {

        String action = null;
        
        if(intent == null || ((action = intent.getAction()) == null) ) {
            Log.w(LOG_TAG, " Null intent received ");
        } else if(action.equals("com.motorola.intent.action.STILL")) {
            if(LOG_INFO) Log.i(LOG_TAG, "Received Broadcast - state : Still");
            postNotify(context, true);
        } else if(action.equals("com.motorola.intent.action.MOTION")) {
            if(LOG_INFO) Log.i(LOG_TAG, "Received Broadcast - state : Motion");
            postNotify(context, false);
        }
    }

    /**
     * The method posts notify message for related configs when a state changes
     * for motion detection adapter
     * @param context
     * @param state
     */
    protected static final void postNotify(Context context, boolean state) {
        HashMap<String, String> configStateMap = new HashMap<String, String>();

        List<String> valueList = Persistence.retrieveValuesAsList(context, MOTION_PERSISTENCE);

        if(valueList.contains(STILL)) {
            configStateMap.put(STILL, (state) ? TRUE : FALSE);
        }

        if(!configStateMap.isEmpty()) {
            Intent newIntent = CommandHandler.constructNotification(configStateMap, MOTION_PUB_KEY);
            context.sendBroadcast(newIntent, PERM_CONDITION_PUBLISHER_ADMIN);
            if(LOG_INFO) Log.i(LOG_TAG, "postNotify : " +   " : " + configStateMap);
        }

    }
}
