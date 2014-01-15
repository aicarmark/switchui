/*
 * @(#)SuggestionsBroadcastReceiver.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2012/04/27 NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.suggestions;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;

/**
 * This receives broadcasts from Rules Importer and passes it to IntentService
 * for further processing
 *
 * <code><pre>
 *
 * CLASS:
 *  extends BroadcastReceiver
 *
 *  implements
 *      Constants - Contains common constants to be used
 *      DbSyntax - DB related constants
 *
 * RESPONSIBILITIES:
 *      - Listen to broadcast
 *      - Start IntentService when broadcast is received
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 *  See each method.
 * </pre></code>
 */
public class SuggestionsBroadcastReceiver extends BroadcastReceiver implements DbSyntax, Constants {

    // debug TAG
    private static final String TAG = SuggestionsBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent != null) {
            String action = intent.getAction();

            if (action == null) return;

            if(action.equals(INTENT_RULES_VALIDATED) || action.equals(ACTION_SA_CORE_INIT_COMPLETE)) {
                if(LOG_INFO) Log.i(TAG, "Broadcast Received, action = "+ action);

                Intent serviceIntent = new Intent(context, SuggestionsNotificationService.class);
                serviceIntent.putExtra(INTENT_ACTION, action);

                if(action.equals(INTENT_RULES_VALIDATED)) {
                    Bundle bundle = intent.getExtras();
                    if(bundle == null) return;

                    serviceIntent.putExtras(bundle);
                }

                if(LOG_DEBUG) Log.i(TAG, "Starting Notification Service");
                ComponentName component = context.startService(serviceIntent);

                if (component == null) {
                    Log.e(TAG, "context.startService() failed for: " +
                          SuggestionsBroadcastReceiver.class.getSimpleName());
                }
            }
        }
    }
}
