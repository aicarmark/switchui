/*
 * @(#)InitCommandFactory.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2011/03/16  NA                Initial version
 */

package com.motorola.contextual.smartprofile;


import java.util.ArrayList;
import java.util.List;

import com.motorola.contextual.smartprofile.util.Util;

import android.content.Context;
import android.content.Intent;


/**
 * This class is a factory for init command handlers
 * <CODE><PRE>
 *
 * CLASS:
 *
 * RESPONSIBILITIES:
 * TODO
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public class InitCommandFactory extends CommandHandlerFactory  {

	private static final String INIT = "Init";
	
    @Override
    protected ArrayList<CommandHandler> createCommandHandlers(Context context, Intent intent) {
        ArrayList<CommandHandler> commandHandlers = new ArrayList<CommandHandler>();
        List<String> list = Util.getActivityNameListFromPackageManager(context);

        for (String activityName: list) {
            commandHandlers.add(instantiateCommandHandler(INIT, activityName));
        }
        return commandHandlers;
    }



}
