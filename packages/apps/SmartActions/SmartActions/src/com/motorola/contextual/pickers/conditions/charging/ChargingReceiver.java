/*
 * @(#)ChargingReceiver.java
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
package  com.motorola.contextual.pickers.conditions.charging;

import java.util.HashMap;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.Persistence;


/**
 * This is a Broadcast receiver which receives the broadcasts for charging publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Implements Constants
 *      Implements ChargingConstants
 *      Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 * Broadcats "notify" intents when it receives charging mode change indications
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class ChargingReceiver extends BroadcastReceiver implements Constants, ChargingConstants {

    private final static String LOG_TAG = ChargingReceiver.class.getSimpleName();


    @Override
    public void onReceive(final Context context, final Intent intent) {

        if(LOG_INFO) Log.i(LOG_TAG, "Received Broadcast");
        String action = null;

        if(intent == null || ((action = intent.getAction()) == null)) {
            Log.w(LOG_TAG, " Null intent received ");
        } else if(action.equals(Intent.ACTION_BATTERY_CHANGED)) {

            final int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            if(LOG_DEBUG) Log.d(LOG_TAG, "Received Broadcast - state : " +   " : " + plugged);

            postNotify(context, plugged);

        }
    }

    /**
     * The method posts notify message for related configs when a state changes
     * for charging
     * @param context
     * @param plugged
     */
    protected static final void postNotify(final Context context, final int plugged) {
        final HashMap<String, String> configStateMap = new HashMap<String, String>();

        final List<String> valueList = Persistence.retrieveValuesAsList(context, CHARGING_PERSISTENCE);

        if(valueList.contains(NOT_CHARGING)) {
            configStateMap.put(NOT_CHARGING, ((plugged == 0) ? TRUE : FALSE));
        }
        if(valueList.contains(AC_CHARGING)) {
            configStateMap.put(AC_CHARGING, ((plugged == 1) ? TRUE : FALSE));
        }
        if(valueList.contains(USB_CHARGING)) {
            configStateMap.put(USB_CHARGING, ((plugged == 2) ? TRUE : FALSE));
        }
        if(valueList.contains(USB_AC_CHARGING)) {
            configStateMap.put(USB_AC_CHARGING, (plugged != 0) ? TRUE : FALSE);
        }

        if(!configStateMap.isEmpty()) {
            if(LOG_INFO) Log.i(LOG_TAG, "postNotify : " +   " : " + configStateMap.toString());
            final Intent newIntent = CommandHandler.constructNotification(configStateMap, CHARGING_PUB_KEY);
            context.sendBroadcast(newIntent, PERM_CONDITION_PUBLISHER_ADMIN);
        }

    }

}
