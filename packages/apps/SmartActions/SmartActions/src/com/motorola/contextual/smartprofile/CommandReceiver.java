/*
 * @(#)CommandReceiver.java
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
package  com.motorola.contextual.smartprofile;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.content.ComponentName;
import com.motorola.contextual.smartprofile.Constants;
/**
 * This is a Broadcast receiver which receives command broadcasts
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Implements Constants
 *      Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 * This class launches CommandInvokerIntentService to handle commands
 *
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class CommandReceiver extends BroadcastReceiver implements Constants {

    private final static String LOG_TAG = CommandReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent == null) {
            Log.w(LOG_TAG, " Null intent received ");
        } else {
            if(LOG_DEBUG) Log.d(LOG_TAG, "Received command Broadcast" + intent.toUri(0));
            intent.setClass(context, CommandInvokerIntentService.class);
            ComponentName compName = context.startService(intent);

            if(compName == null) {
                Log.e(LOG_TAG, " Start service failed for : " + CommandInvokerIntentService.class.getSimpleName());
            }
        }
    }
}
