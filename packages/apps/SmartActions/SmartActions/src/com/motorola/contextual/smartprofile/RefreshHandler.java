/*
 * @(#)RefreshHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of RefreshHandler
 *
 */

package com.motorola.contextual.smartprofile;


import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.util.Util;


/**
 * This class handles "refresh" command from Smart Actions Core for Charging
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *
 * RESPONSIBILITIES:
 * This class updates the config parameters and sends reponse to Smart
 * Actions Core
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public final class RefreshHandler extends  CommandHandler  implements Constants {

    private final static String LOG_TAG = RefreshHandler.class.getSimpleName();

    @Override
    protected final String executeCommand(Context context, Intent intent) {
        //TODO Validate config
        if(LOG_DEBUG) Log.d(LOG_TAG, "executeCommand " + intent.toUri(0));

        String status = FAILURE;

        if(intent.getStringExtra(EXTRA_CONFIG) != null) {
            constructAndPostResponse(context, intent);
            status = SUCCESS;
        }

        return status;
    }

    /**
     * This method constructs response for "refresh" command and sends
     * it to Smart Actions Core
     * @param context
     * @param intent - incoming intent
     */
    private void constructAndPostResponse(Context context, Intent intent) {
        String config = intent.getStringExtra(EXTRA_CONFIG);

        Intent responseIntent = constructCommonResponse(intent);

        String actName = Util.getPublisherNameFromPublisherKey(context, intent.getAction());
        if(LOG_DEBUG) Log.d(LOG_TAG, "constructAndPostResponse : " + actName);
        AbstractDetailComposer ruleConstr = null;
        if(actName != null) {
            ruleConstr = instantiateDetailComposer(actName);
        }
        config = (ruleConstr != null) ? ruleConstr.getUpdatedConfig(context, config) : config;
        responseIntent.putExtra(EXTRA_CONFIG, config);
        if(config != null) {
            responseIntent.putExtra(EXTRA_DESCRIPTION, (ruleConstr != null) ? ruleConstr.getDescription(context, config) : null);
            responseIntent.putExtra(EXTRA_STATE, (ruleConstr != null) ? ruleConstr.getCurrentState(context, config) : FALSE);
        }

        responseIntent.putExtra(EXTRA_EVENT_TYPE, REFRESH_RESPONSE);
        context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
    }
}
