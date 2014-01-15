/*
 * @(#)MissedCallListHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/06  NA                Initial version of MissedCallListHandler
 *
 */

package  com.motorola.contextual.smartprofile.sensors.missedcallsensor;

import android.content.Context;
import android.content.Intent;

import com.motorola.contextual.smartprofile.CommandHandler;


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

public final class MissedCallListHandler extends  CommandHandler  {

    @SuppressWarnings("unused")
	private final static String LOG_TAG = MissedCallListHandler.class.getSimpleName();

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
        responseIntent.putExtra(EXTRA_CONFIG_ITEMS, MissedCallDetailComposer.getConfigItems(context));
        responseIntent.putExtra(EXTRA_NEW_STATE_TITLE, MissedCallDetailComposer.getName(context));
        context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
    }
}