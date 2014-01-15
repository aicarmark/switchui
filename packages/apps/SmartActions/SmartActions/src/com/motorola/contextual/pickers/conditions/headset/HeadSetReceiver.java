/*
 * @(#)HeadSetReceiver.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * a18491        2012/03/16 NA                Initial version of HeadSetReceiver
 *
 */
package  com.motorola.contextual.pickers.conditions.headset;

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
 * This is a Broadcast receiver which receives the broadcasts for headset publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Implements Constants
 *      Implements HeadSetConstants
 *      Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 * Broadcats "notify" intents when it receives headset mode change indications
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class HeadSetReceiver extends BroadcastReceiver implements Constants, HeadSetConstants {

    private final static String LOG_TAG = HeadSetReceiver.class.getSimpleName();


    @Override
    public void onReceive(final Context context, final Intent intent) {

        String action = null;

        if(intent == null || ((action = intent.getAction()) == null)) {
            Log.w(LOG_TAG, " Null intent received ");
        } else if(action.equals(Intent.ACTION_HEADSET_PLUG)) {
            final int state =  intent.getIntExtra("state", 0);
            if(LOG_INFO) Log.i(LOG_TAG, "Received Broadcast - state : " +   " : " + state);
            postNotify(context, state);
        }
    }

    /**
     * The method posts notify message for related configs when a state changes
     * for headset
     * @param context
     * @param state
     */
    protected static final void postNotify(final Context context, final int state) {
        final HashMap<String, String> configStateMap = new HashMap<String, String>();

        final List<String> valueList = Persistence.retrieveValuesAsList(context, HEADSET_PERSISTENCE);

        if(valueList.contains(CONNECTED)) {
            configStateMap.put(CONNECTED, (state == 1) ? TRUE : FALSE);
        }
        if(valueList.contains(NOT_CONNECTED)) {
            configStateMap.put(NOT_CONNECTED, (state == 0) ? TRUE : FALSE);
        }

        if(!configStateMap.isEmpty()) {
            final Intent newIntent = CommandHandler.constructNotification(configStateMap, HEADSET_PUB_KEY);
            context.sendBroadcast(newIntent, PERM_CONDITION_PUBLISHER_ADMIN);
            if(LOG_INFO) Log.i(LOG_TAG, "postNotify : " +   " : " + configStateMap.toString());
        }
    }

}
