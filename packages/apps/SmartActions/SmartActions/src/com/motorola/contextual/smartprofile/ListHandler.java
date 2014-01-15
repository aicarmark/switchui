/*
 * @(#)ListHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of ListHandler
 *
 */

package com.motorola.contextual.smartprofile;



import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.util.Util;


/**
 * This class handles "list" command from Smart Actions Core for
 * Condition Publishers
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

public final class ListHandler extends  CommandHandler  implements Constants {

    private final static String LOG_TAG = ListHandler.class.getSimpleName();

    @Override
    protected final String executeCommand(Context context, Intent intent) {
        constructAndPostResponse(context, intent);
        return SUCCESS;
    }

    /**
     * This method constructs response for "list" command and sends
     * it to Smart Actions Core
     * @param context
     * @param intent - incoming intent
     */
    private void constructAndPostResponse(Context context, Intent intent) {

        Intent responseIntent = constructCommonResponse(intent);
        responseIntent.putExtra(EXTRA_EVENT_TYPE, EXTRA_LIST_RESPONSE);
        
        String actName = Util.getPublisherNameFromPublisherKey(context, intent.getAction());
        if(LOG_DEBUG) Log.d(LOG_TAG, "constructAndPostResponse : " + actName);
        AbstractDetailComposer ruleConstr = null;
        if(actName != null) {
            ruleConstr = instantiateDetailComposer(actName);            
            responseIntent.putExtra(EXTRA_CONFIG_ITEMS, ((ruleConstr != null) ? ruleConstr.getConfigItems(context) : ""));
            responseIntent.putExtra(EXTRA_NEW_STATE_TITLE, ((ruleConstr != null) ? ruleConstr.getName(context) : ""));
            
        }
        
        context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
    }
}
