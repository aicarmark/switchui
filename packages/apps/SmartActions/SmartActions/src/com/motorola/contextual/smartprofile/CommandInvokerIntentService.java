/*
 * @(#)CommandInvokerIntentService.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/16  NA                Initial version
 *
 */

package  com.motorola.contextual.smartprofile;

import java.util.ArrayList;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.psf.table.LocalPublisherTable;



/**
 * This class receives the commands sent to the condition publishers and
 * routes to the appropriate handler/s
 *
 * <code><pre>
 *
 * CLASS:
 *     Extends IntentService
 *     Implements Constants
 *
 * RESPONSIBILITIES:
 * This class also parses basic command parameters and sends out error responses
 * if there are errors
 *
 * COLLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method
 *
 * </pre></code>
 */

public class CommandInvokerIntentService extends IntentService implements Constants {
    private static final String LOG_TAG =   CommandInvokerIntentService.class.getSimpleName();
    private static final String SMART_PROFILE_PUB_KEY_PREFIX =   "com.motorola.contextual.smartprofile";


    public CommandInvokerIntentService () {
        super(LOG_TAG);
    }

    public CommandInvokerIntentService(String name) {
        super(name);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if( LOG_DEBUG) Log.d(LOG_TAG, " onDestroy");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if( LOG_DEBUG) Log.d(LOG_TAG, " onHAndleIntent ") ;

        Context context = getApplicationContext();
        if(intent == null) {
            Log.w(LOG_TAG, " Null intent received ");
        } else {
            String action = intent.getAction();
            String coreDataCleared = POSSIBLE_VALUE_FALSE;

            if(action != null) {

            	if (action.equals(SA_CORE_INIT_COMPLETE)) {
                    coreDataCleared = intent.getStringExtra(EXTRA_DATA_CLEARED);
                }
                String status = FAILURE;
                // Get the factory which produces right handler to handle this command
                CommandHandlerFactory cmdFactory = createCommandHandlerFactory(intent);
                if(cmdFactory != null) {
                    if ((coreDataCleared != null) && (coreDataCleared.equals(POSSIBLE_VALUE_TRUE))) {
                        handleCoreDataCleared(context, cmdFactory, intent);
                    } else {
                        generateAndExecuteCommandHandlers(context, cmdFactory, intent);
                    }

                } else {
                    Intent responseIntent = CommandHandler.constructFailureResponse(intent, status);
                    context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
                }


            } else
                Log.w(LOG_TAG, " Null intent action received ");
        }
    }

    /**
     * This method handles the intent from the core which notifies 
     * core data clear
     * @param context
     * @param cmdFactory
     * @param intent
     */
    private void handleCoreDataCleared(Context context, CommandHandlerFactory cmdFactory, Intent intent) {
    	StringBuilder sb = new StringBuilder();
    	
    	sb.append(LocalPublisherTable.Columns.TYPE).append(DbSyntax.EQUALS)
    	  .append(DbSyntax.Q).append(LocalPublisherTable.Type.CONDITION).append(DbSyntax.Q)
    	  .append(DbSyntax.AND)
    	  .append(LocalPublisherTable.Columns.BLACKLIST).append(DbSyntax.EQUALS).append(LocalPublisherTable.BlackList.FALSE)
    	  .append(DbSyntax.AND)
    	  .append(LocalPublisherTable.Columns.PACKAGE).append(DbSyntax.EQUALS)
    	  .append(DbSyntax.Q).append(com.motorola.contextual.smartrules.Constants.PACKAGE).append(DbSyntax.Q);
    	  
    	  
        String whereClause = sb.toString();

        String columns[] = new String[] {LocalPublisherTable.Columns._ID, LocalPublisherTable.Columns.PUBLISHER_KEY};

        ContentResolver cResolver = context.getContentResolver();
        Cursor cursor = null;

        /**
         * When there is a SA_CORE_INIT_COMPLETE with data_cleared=true
         * then, we have to ask all publishers to cancel their subscriptions.
         * In this code, we take care of publishers present inside this package.
         * We basically change the command to cancel with action=pubkey,
         * config=*(indicating all configs), event_type = cancel_request
         */
        try {
            cursor = cResolver.query(LocalPublisherTable.CONTENT_URI, columns, whereClause, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    String pubKey = cursor.getString(cursor.getColumnIndexOrThrow(LocalPublisherTable.Columns.PUBLISHER_KEY));
                    intent.setAction(pubKey);
                    intent.putExtra(EXTRA_COMMAND, CANCEL_REQUEST);
                    intent.putExtra(EXTRA_CONFIG, ALL_CONFIGS);
                    generateAndExecuteCommandHandlers(context, cmdFactory, intent);
                    cursor.moveToNext();
                }
            } else {
                Log.e(LOG_TAG, "Cursor null in handleCoreDataCleared " + whereClause);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception in handleCoreDataCleared " + whereClause + " " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

    }

    /**
     * This method creates and executes command handlers
     * @param context
     * @param cmdFactory
     * @param intent
     */
    private void generateAndExecuteCommandHandlers(Context context, CommandHandlerFactory cmdFactory, Intent intent) {

        String status = FAILURE;

        ArrayList<CommandHandler> commnadHandlers = cmdFactory.createCommandHandlers(context, intent);

        if ((commnadHandlers == null) || (commnadHandlers.size() == 0)) {
            Intent responseIntent = CommandHandler.constructFailureResponse(intent, status);
            context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
            return;
        }


        for(CommandHandler cmdHdr : commnadHandlers) {
            if(cmdHdr != null) {
                status = cmdHdr.executeCommand(context, intent);
            }
            if(!status.equals(SUCCESS) && (!SA_CORE_INIT_COMPLETE.equals(intent.getAction()))) {
                Log.w(LOG_TAG, " Command handling failure " + intent.getAction());
                Intent responseIntent = CommandHandler.constructFailureResponse(intent, status);
                context.sendBroadcast(responseIntent, PERM_CONDITION_PUBLISHER_ADMIN);
            }
        }
    }

    /**
     * This method creates command handler factory from the incoming intent
     * @param intent
     * @return CommandHandlerFactory
     */
    private final CommandHandlerFactory createCommandHandlerFactory(Intent intent) {
        String action = intent.getAction();
        String coreDataCleared = POSSIBLE_VALUE_TRUE;

        if(action == null) return null;
        
        if ((action.equals(SA_CORE_INIT_COMPLETE))) {
            coreDataCleared = intent.getStringExtra(EXTRA_DATA_CLEARED);
            coreDataCleared = (coreDataCleared != null) ? coreDataCleared : POSSIBLE_VALUE_FALSE;
            if (coreDataCleared.equals(POSSIBLE_VALUE_TRUE)) {
                intent.putExtra(EXTRA_COMMAND, CANCEL_REQUEST);
            }
        }

        if( LOG_DEBUG) Log.d(LOG_TAG, " createCommandHandlerFactory " + action + " , " + intent.toUri(0)) ;
        if(coreDataCleared.equals(POSSIBLE_VALUE_FALSE)) {
            if( LOG_INFO) Log.i(LOG_TAG, " disable MD" ) ;
            return new InitCommandFactory();
        } else if (action.contains(SMART_PROFILE_PUB_KEY_PREFIX) ||
                   action.equals("com.motorola.contextual.Motion") /* Action with package name */ ||
                   coreDataCleared.equals(POSSIBLE_VALUE_TRUE)) {
            String command = intent.getStringExtra(EXTRA_COMMAND);
            if( LOG_DEBUG) Log.d(LOG_TAG, " command " + command) ;
            if(command != null) {
                return new CommandHandlerFactory();
            }
        }
        return null;

    }
}

