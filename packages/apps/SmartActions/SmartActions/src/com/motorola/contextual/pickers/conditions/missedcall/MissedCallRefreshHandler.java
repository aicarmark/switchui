/*
 * @(#)MissedCallRefreshHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of MissedCallRefreshHandler
 *
 */

package com.motorola.contextual.pickers.conditions.missedcall;



import android.content.Context;
import android.content.Intent;
import com.motorola.contextual.smartprofile.CommandHandler;


/**
 * This class handles "refresh" command from Smart Actions Core for Missed Call
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *     Implements MissedCallConstants
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

public final class MissedCallRefreshHandler extends  CommandHandler  implements MissedCallConstants {

    @Override
    protected final String executeCommand(Context context, Intent intent) {
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
    private final void constructAndPostResponse(Context context, Intent intent) {
        String config = intent.getStringExtra(EXTRA_CONFIG);

        Intent responseIntent = constructCommonResponse(intent);
        String state = null;
        responseIntent.putExtra(EXTRA_EVENT_TYPE, REFRESH_RESPONSE);

        String oldConfig = config;
        config = MissedCallDetailComposer.getUpdatedConfig(context, config);

        if((config != null) && oldConfig.equals(config)) {
            state = MissedCallDetailComposer.getStateForConfig(context, config);
        }

        if(config != null) {
            responseIntent.putExtra(EXTRA_CONFIG, config);
            responseIntent.putExtra(EXTRA_DESCRIPTION, MissedCallDetailComposer.getDescriptionForConfig(context, config));
            responseIntent.putExtra(EXTRA_STATE, state);
        } else {
            responseIntent.putExtra(EXTRA_STATUS, FAILURE);
        }
        context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
    }
}
