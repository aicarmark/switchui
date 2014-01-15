/*
 * @(#)MediatorReceiver.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/05/21  NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.psf.mediator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Broadcast receiver to handle various commands/command responses. Starts the Mediator Service to process the command.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends BroadcastReceiver
 *
 * RESPONSIBILITIES:
 * Handle command/command response intents
 *
 * COLABORATORS:
 *     MediatorService - Starts IntentServiceto handle the broadcasts
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public class MediatorReceiver extends BroadcastReceiver implements MediatorConstants {

    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setClass(context, MediatorService.class);
        context.startService(intent);
    }
}
