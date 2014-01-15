/*
 * @(#)BTDeviceReceiver.java
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
package  com.motorola.contextual.pickers.conditions.bluetooth;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


import com.motorola.contextual.smartprofile.CommandInvokerIntentService;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.Persistence;



/**
 * This is a Broadcast receiver which receives the broadcasts for BT Device condition publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Implements Constants
 *      Implements BTConstants
 *      Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 * Clears the list during shut down. Posts "notify" event to the intent service
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class BTDeviceReceiver extends BroadcastReceiver implements Constants, BTConstants {

    private final static String LOG_TAG = BTDeviceReceiver.class.getSimpleName();


    @Override
    public void onReceive(Context context, Intent intent) {

        if(LOG_INFO) Log.i(LOG_TAG, "Received Broadcast :" + intent.toUri(0));
        String action = null;

        if((intent == null) || ((action = intent.getAction()) == null)) {
            Log.w(LOG_TAG, " Null intent received ");
        } else if (action.equals(Intent.ACTION_SHUTDOWN)) {
            List<String> valueList = Persistence.retrieveValuesAsList(context, BT_CONNECTED_LIST_PERSISTENCE);
            valueList.clear();
            Persistence.commitValuesAsList(context, BT_CONNECTED_LIST_PERSISTENCE, valueList);
        } else {
            intent.setClass(context, CommandInvokerIntentService.class);
            intent.putExtra(EXTRA_COMMAND, NOTIFY_REQUEST);
            intent.putExtra(EXTRA_BT_ACTION, intent.getAction());
            intent.setAction(BT_PUB_KEY);
            ComponentName compName = context.startService(intent);

            if(compName == null) {
                Log.e(LOG_TAG, " Start service failed for : " + CommandInvokerIntentService.class.getSimpleName());
            }
        }
    }

}
