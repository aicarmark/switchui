/*
 * @(#)DisplayReceiver.java
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
package  com.motorola.contextual.pickers.conditions.display;

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
 * This is a Broadcast receiver which receives the broadcasts for display publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Implements Constants
 *      Implements DisplayConstants
 *      Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 * Broadcats "notify" intents when it receives display mode change indications
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class DisplayReceiver extends BroadcastReceiver implements Constants, DisplayConstants {

    private final static String LOG_TAG = DisplayReceiver.class.getSimpleName();


    @Override
    public void onReceive(Context context, Intent intent) {

        String action = null;

        if(intent == null || ((action = intent.getAction()) == null)) {
            Log.w(LOG_TAG, " Null intent received ");
        } else if(action.equals(Intent.ACTION_SCREEN_ON)) {
            if(LOG_INFO) Log.i(LOG_TAG, "Received Broadcast - state : " +  "ON");
            postNotify(context, true);
        } else if(action.equals(Intent.ACTION_SCREEN_OFF)) {
            if(LOG_INFO) Log.i(LOG_TAG, "Received Broadcast - state : " +  "OFF");
            postNotify(context, false);
        }
    }

    /**
     * The method posts notify message for related configs when a state changes
     * for display
     * @param context
     * @param state
     */
    protected static final void postNotify(Context context, boolean state) {
        HashMap<String, String> configStateMap = new HashMap<String, String>();

        List<String> valueList = Persistence.retrieveValuesAsList(context, DISPLAY_PERSISTENCE);

        if(valueList.contains(ON)) {
            configStateMap.put(ON, (state) ? TRUE : FALSE);
        }
        if(valueList.contains(OFF)) {
            configStateMap.put(OFF, (state) ? FALSE : TRUE);
        }

        if(!configStateMap.isEmpty()) {
            Intent newIntent = CommandHandler.constructNotification(configStateMap, DISPLAY_PUB_KEY);
            context.sendBroadcast(newIntent, PERM_CONDITION_PUBLISHER_ADMIN);
            if(LOG_INFO) Log.i(LOG_TAG, "Post notify : " +  configStateMap.toString());
        }

    }

}
