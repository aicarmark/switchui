/*
 * @(#)BatteryLevelReceiver.java
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
package  com.motorola.contextual.pickers.conditions.battery;

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
 * This is a Broadcast receiver which receives the broadcasts for battery level publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Implements Constants
 *      Implements BatteryLevelConstants
 *      Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 * Broadcats "notify" intents when it receives battery level change indications
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class BatteryLevelReceiver extends BroadcastReceiver implements Constants, BatteryLevelConstants {

    private final static String LOG_TAG = BatteryLevelReceiver.class.getSimpleName();


    @Override
    public void onReceive(Context context, Intent intent) {

        if(LOG_INFO) Log.i(LOG_TAG, "Received Broadcast");
        String action = null;

        if((intent == null) || ((action = intent.getAction()) == null)) {
            Log.w(LOG_TAG, " Null intent received ");
        } else if(action.equals(Intent.ACTION_BATTERY_CHANGED)) {

            Integer level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            if(LOG_INFO) Log.i(LOG_TAG, "Received Broadcast - state : " +   " : " + level);

            postNotify(context, level);

        }

    }
    /**
     * The posts states for all the configured configs
     * for charging
     * @param context
     * @param plugged
     */
    protected static final void postNotify(Context context, int level) {
        List<String> valueList = Persistence.retrieveValuesAsList(context, BATTERY_LEVEL_PERSISTENCE);
        HashMap<String, String> configStateMap = new HashMap<String, String>();

        if(valueList.contains(BATTERY_LEVEL_50)) {
            configStateMap.put(BATTERY_LEVEL_50, ((level < 50) ? TRUE : FALSE));
        }
        if(valueList.contains(BATTERY_LEVEL_35)) {
            configStateMap.put(BATTERY_LEVEL_35, ((level < 35) ? TRUE : FALSE));
        }
        if(valueList.contains(BATTERY_LEVEL_25)) {
            configStateMap.put(BATTERY_LEVEL_25, ((level < 25) ? TRUE : FALSE));
        }
        if(valueList.contains(BATTERY_LEVEL_10)) {
            configStateMap.put(BATTERY_LEVEL_10, ((level < 10) ? TRUE : FALSE));
        }

        if(!configStateMap.isEmpty()) {
            if(LOG_INFO) Log.i(LOG_TAG, "Received Broadcast - state : " +   " : " + configStateMap.toString());
            Intent newIntent = CommandHandler.constructNotification(configStateMap, BATTERY_PUB_KEY);
            context.sendBroadcast(newIntent, PERM_CONDITION_PUBLISHER_ADMIN);
        }
    }
}
