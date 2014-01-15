/*
 * @(#)CommandHandlerFactory.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/15  NA                Initial version of CommandHandlerFactory
 *
 */

package com.motorola.contextual.smartprofile;


import java.util.ArrayList;

import com.motorola.contextual.smartprofile.util.Util;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class is a factory which creates appropriate publisher handlers to handle
 * specific commands
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Implements Constants
 *
 * RESPONSIBILITIES:
 * Instantiates command handlers dynamically using package manager
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public  class  CommandHandlerFactory implements Constants  {

    private static final String LOG_TAG =   CommandHandlerFactory.class.getSimpleName();
    private static final String COMMAND_HANDLER =   "Handler";
    private static final String UNDERSCORE =   "_";

    /** Instantiates the interface dynamically based on the class name
     * @param  commandType
     * @param  actName
     * @return cmdHandler
     */
    protected static final CommandHandler instantiateCommandHandler(String commandType, String actName) {
        Class<?> cls = null;
        CommandHandler cmdHandler = null;
        String packageName = null, conditionKey = null, className = null;

        try {

            conditionKey = actName.substring((actName.lastIndexOf(":")) + 1);
            actName = actName.substring(0, (actName.lastIndexOf(":")));
            packageName = actName.substring(0, (actName.lastIndexOf(".")+1));

            className = packageName + conditionKey + commandType + COMMAND_HANDLER;
            if(LOG_DEBUG) Log.d(LOG_TAG, "Class to be created " + packageName + " : " + commandType + " : " + className
            							+ " : " +  actName + " : " + conditionKey);
            cls = Class.forName(className);

        } catch(Exception e) {
            if(LOG_DEBUG) Log.d(LOG_TAG, "Class not found " + packageName);
            try {
                cls = Class.forName("com.motorola.contextual.smartprofile." + commandType + COMMAND_HANDLER);
            } catch(ClassNotFoundException exception) {
                return null;
            }
        }
        try {
            cmdHandler = (CommandHandler)cls.newInstance();
        } catch(IllegalAccessException e) {
            if(LOG_DEBUG) Log.d(LOG_TAG, "class instantiation failed: default ctor not visible " + packageName);
        } catch (InstantiationException e) {
            if(LOG_DEBUG) Log.d(LOG_TAG, "class instantiation failed: instance cannot be created " + packageName);
        }
        return cmdHandler;
    }

    /** Creates command handlers from the given intent
     * @param  context
     * @param  intent
     * @return cmdHandlers
     */
    protected  ArrayList<CommandHandler> createCommandHandlers(Context context, Intent intent) {
        ArrayList<CommandHandler> cmdHandlers = new ArrayList<CommandHandler>();
        String pubKey = intent.getAction();
        String actName = Util.getPublisherNameFromPublisherKey(context, pubKey);

        String command = intent.getStringExtra(EXTRA_COMMAND);

        if((command != null) && (actName != null)){
        	
        	// command currently is "subscribe_request".
        	// Strip off "_request". Convert "subscribe" to "Subscribe" to generate 
        	// corresponding handler
        	char[] charArray = command.toCharArray();

        	charArray[0] = command.toUpperCase().charAt(0);
        	
            command = String.copyValueOf(charArray);

            command = command.substring(0, command.indexOf(UNDERSCORE));

            if(LOG_DEBUG) Log.d(LOG_TAG, " derived command : " +  command);
            cmdHandlers.add(instantiateCommandHandler(command, actName));
            
        }

        return cmdHandlers;

    }

}
