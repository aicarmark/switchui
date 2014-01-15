/*
 * @(#)SmartRulesService.java
 *
 * (c) COPYRIGHT 2010 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/04/15 NA				  Initial version
 * w30219        2012/01/31 IKMAIN-35110      Changes to addQueue timing to avoid ANR
 *
 */
package com.motorola.contextual.smartrules.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.business.Action;
import com.motorola.contextual.smartrules.db.business.ActionPersistence;
import com.motorola.contextual.smartrules.db.business.Conflicts;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ActionTuple;
import com.motorola.contextual.smartrules.db.table.ModalTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.util.Util;

/** This is a Intent Service class that handles the VSM Intent by
 * 	- Checking if the intent can be processed
 *  - If the intent can be processed perform conflict resolution on stateful actions
 *  - Sends a broadcast intent to Actions to handle the action
 *  - Sets the status of the rule and actions accordingly in the Database
 *
 *  Note: This is no longer an intent service as intent service stops itself as soon as it returns and not sticky.
 *  We need a sticky service to receive the responses from QA. Once all of the responses are received
 *  and there are no more intents to be serviced, this service will stop itself.
 *  
 *<code><pre>
 * CLASS:
 * 	 extends IntentService
 *
 *  implements
 *   Constants - for the constants used
 *   DbSyntax - for the DB related constants
 *
 * RESPONSIBILITIES:
 * 	 Processes the pending intents fired by VSM
 *   	- checks if the intent can be processed
 *   	- activates/de-activates rules and actions
 *   	- fires the actions after conflict resolution
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 *</pre></code>
 */
public class SmartRulesService extends Service implements Constants, DbSyntax {

	private static final String TAG = SmartRulesService.class.getSimpleName();

	// There should be a required set of these parameters which  SmartRulesService requires 
	//   as a valid set of inputs on the inbound intent.
	private static final String QA_RULE_KEY = "com.motorola.contextual.smartrules.rulekey";

	private static final int RESPONSE_WAIT_TIME_BASE 		= 1000;
	private static final int RESPONSE_WAIT_TIME_PER_ACTION 	= 2000;
	
	private static Context 						mContext = null;
	private BroadcastReceiver 				mQuickActionsReceiver;
	private static RuleStateChangeThread 	mThread;
    private static IntentQueue 				mIntentQueue = null;
    
	/** Thread which handles all processing once the intent to change a rule state is received. */
	private class RuleStateChangeThread extends Thread {		
		
		private boolean running = false;
		private static final int SLEEP_INTERVAL_MS = 200;
		/** max time we wait for Quick actions to respond to a request to fire an action */
		
		/** only constructor that should be used */
		public RuleStateChangeThread() {
			super("RuleStateChangeThread");
			running = true;
			this.setPriority(Thread.NORM_PRIORITY-1);
		}

		/* (non-Javadoc)
		 * @see java.lang.Thread#destroy()
		 */
		@Override
		public void destroy() {
			super.destroy();
			running = false;
		}

		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			super.run();
			// NOTE: This thread cannot handle an interruption event until the persistence of the queue is implemented
			while (mIntentQueue.size() > 0) {								
				// peek at the top item in queue, process it as needed, then pop it.
				IntentQueueItem queueItemHead = mIntentQueue.peekHead();
				if (processIntent(queueItemHead)) {
					
					boolean timeout = false;
					long accumTime = 0;
					long MAX_WAIT_TIME_MS = queueItemHead.responseWaitList.getWaitTimeMs();
					boolean allResponsesReceived = queueItemHead.responseWaitList.isEmpty();
					while (! allResponsesReceived && ! timeout ) {
						try {
							sleep(SLEEP_INTERVAL_MS);
							accumTime += SLEEP_INTERVAL_MS;
							timeout = (accumTime > MAX_WAIT_TIME_MS);
						} catch (InterruptedException e) {
							Log.e(TAG, TAG+" This code is not designed to handle interrupts!");
							e.printStackTrace();
						}
						allResponsesReceived = queueItemHead.responseWaitList.isEmpty();
					}
					if (timeout) 
						Log.e(TAG, "Timeout occurred waiting on Action item:"+ queueItemHead.toString());
					
					if (LOG_DEBUG) Log.d(TAG, TAG+"Waiting on responses = "+ queueItemHead.responseWaitList.size());
				}
				
				//Broadcast RULE_STATE_CHANGED 
				sendRuleProcessedBroadcast(queueItemHead);
				
				// always pop the top of the queue;
				// Changed for IKMAIN-35110 : We want to pop the item from the queue ONLY when all processing is complete
				// This should be the last call in this while loop (except for the debug log)
				mIntentQueue.popHead();
				
				if (LOG_DEBUG) Log.d(TAG, TAG+"After pop head, queue size = "+mIntentQueue.size());
			}
			running = false;
			// once the thread stops, we must shut down, perhaps this could be  
			//        performed via handler message to the service after a quiet period, not sure
			SmartRulesService.this.stopSelf();			
		}
		
		private void sendRuleProcessedBroadcast(IntentQueueItem queueItem) {
			if (queueItem.ruleKey != null) {
				Intent stateChangeIntent = new Intent(RULE_STATE_CHANGED);
				stateChangeIntent.putExtra(MM_RULE_KEY, queueItem.ruleKey);
				stateChangeIntent.putExtra(MM_RULE_STATUS, queueItem.ruleStatus);
				stateChangeIntent.putExtra(MM_DISABLE_RULE, queueItem.disableRule);
				stateChangeIntent.putExtra(MM_DELETE_RULE, queueItem.deleteRule);
				mContext.sendBroadcast(stateChangeIntent);
			}
		}
	}
    
	@Override
	public synchronized int onStartCommand(Intent intent, int flags, int startId) {

		if(intent == null) {
			Log.e(TAG, "service started with null intent");
			if(mIntentQueue == null) {
				Log.e(TAG, "queue is also empty so OK to stop service");
				stopSelf();
			}
		} else {
			if (LOG_INFO) Log.i(TAG, "in onStartCommand " + intent.toUri(0));

			boolean saCoreInitCompleteBroadcast = intent.getBooleanExtra(EXTRA_SA_CORE_INIT_COMPLETE_SENT, false);
			if(saCoreInitCompleteBroadcast) Util.sendMessageToNotificationManager(this, 0);
			else {

				if (mIntentQueue == null)
					mIntentQueue = new IntentQueue();

				// Add the item to the Queue first;
				// We will check the status of the thread after addQueueItem returns;
				// Since addQueueItem is a synchronous function, it might take a while to return
				// and the thread might have finished running by then
				addQueueItem(intent);

				if (mThread == null) {
					if (LOG_DEBUG) Log.d(TAG, "in mThread is null; Start a new thread ");
					mThread = new RuleStateChangeThread();
					mThread.start();
				} else if (mThread.running) {
					//No need to do anything here; the running thread will pick-up the added intent
					if (LOG_DEBUG) Log.d(TAG, "in mThread is running added into Q ");
				} else {
					// once the thread stops (ends run() method), a new thread must be created, per Java documentation
					if (LOG_DEBUG) Log.d(TAG, "in mThread is not running; Start a new thread ");
					mThread = new RuleStateChangeThread();
					mThread.start();
				}
			}
		}
		return START_STICKY;
	}
		

	/** This method simply adds the queue event. The events are processed when:
	 * 
	 * 1.) The first item is added to the queue
	 * 2.) A timeout event occurs waiting for QA to respond to action intents sent from here. 
	 * 3.) All the QA responses are received to the request at the top of the queue
	 *  
	 * @param intent - Intent that invokes this Service (this is the entry point)
	 */
	private synchronized void addQueueItem(final Intent intent) {	
			// simply add it to the queue 
			IntentQueueItem queueItem = new IntentQueueItem(intent);
			mIntentQueue.add(queueItem);
	}
	
	
		
	public boolean processIntent(IntentQueueItem queueItem) {	

		boolean result = false;
		Intent ruleIntent = queueItem.intent;
		if (LOG_DEBUG) Log.d(TAG, "processIntent intent is " + ruleIntent.toUri(0));


		boolean disableAll = ruleIntent.getBooleanExtra(MM_DISABLE_ALL, false);
		if (disableAll) {
			ActionPersistence.fireDefaultRecordActions(mContext);
            ActionPersistence.fireAllStatelessActionsDisabled(mContext);
            RulePersistence.unsubscribeFromConditionPublishers(mContext);
            Cursor disabledRulesCursor = RulePersistence.markRulesInactiveInDb(mContext);
            ActionPersistence.markActionsInactiveInDb(mContext);
            sendBroadcastResponse(disabledRulesCursor);
            Util.clearOnGoingNotifications(mContext);
		} else {
		String ruleKey = ruleIntent.getStringExtra(MM_RULE_KEY);
	    String ruleStatus = ruleIntent.getStringExtra(MM_RULE_STATUS);
	    if (ruleStatus == null) ruleStatus = FALSE;   //Default is false
	    boolean enableRule = ruleIntent.getBooleanExtra(MM_ENABLE_RULE, false);
	    boolean disableRule = ruleIntent.getBooleanExtra(MM_DISABLE_RULE, false);
	    boolean deleteRule = ruleIntent.getBooleanExtra(MM_DELETE_RULE, false);
	    
	    Cursor ruleCursor = RulePersistence.getRuleCursor(mContext, ruleKey);
		if (ruleCursor == null || ruleCursor.getCount() == 0) {
			Log.e(TAG, "ruleCursor is null or empty");
				if (ruleCursor != null && !ruleCursor.isClosed())
					ruleCursor.close();
		} else {
			try {
				queueItem.populateRuleInfo(ruleCursor, ruleKey, ruleStatus, enableRule, disableRule, deleteRule);
				if(LOG_DEBUG) Log.d(TAG, "Rule can be processed further to check and handle rule state");
				result = processRuleStateChange(mContext, queueItem);
				if ( result ) {
					int source = ruleCursor.getInt(ruleCursor.getColumnIndex(RuleTable.Columns.SOURCE));
					if ( source == RuleTable.Source.CHILD)
						DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.IN, ruleStatus,
			            		null, ruleKey, FROM_MODEAD_DBG_MSG, CHILD_RULE, Constants.VISIBLE_RULE,
			            		Constants.PACKAGE, Constants.PACKAGE);
					else
						DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.IN, ruleStatus,
								null, ruleKey, FROM_MODEAD_DBG_MSG, null, Constants.VISIBLE_RULE,
								Constants.PACKAGE, Constants.PACKAGE);
				}
			} catch (SQLiteException e) {
				e.printStackTrace();
			} finally {
				ruleCursor.close();					
			}
		  }
		}
        
		return result;
	}
		


	/** default constructor
	 */
	public SmartRulesService() {
		super();
	}

	/** onCreate
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		if (LOG_DEBUG) Log.d(TAG, "SmartRulesService Oncreate called; Registering receiver");
		
		mContext = this;
		mQuickActionsReceiver = registerReceiver(mContext);
	}

	/** onDestroy()
	 */
	@Override
	public void onDestroy() {
		if (LOG_DEBUG) Log.d(TAG, "onDestroy called");
		super.onDestroy();
		if(mQuickActionsReceiver != null)
			mContext.unregisterReceiver(mQuickActionsReceiver);
	}

	

   /** Checks if the rule can be processed further
    *
    * @param context - context
    * @param queueItem - currently processed item in queue
    * @return - true if the rule can be processed further
    * 			 else false (if rule is disabled for automatic rule)
    */
   private static boolean isEnabled(final Context context, IntentQueueItem queueItem) {

       boolean result = true;
       String state = null;
       
       // check if the rule is automatic before checking if it is enabled.
       // if it is a manual rule, we would still proceed further now that the
       // manual rule firing also is through this service.
       if(queueItem.automatic) {
	       if(!queueItem.enabled) {
	           if(LOG_DEBUG) Log.d(TAG, "Rule is disabled");
	           state = context.getString(R.string.disabled) + BLANK_SPC + context.getString(R.string.rules_disabled);
	           result = false;
	       } 
       }

       if(!result)
           DebugTable.writeToDebugViewer(context, DebugTable.Direction.INTERNAL, state, 
        		   queueItem.modeName, queueItem.ruleKey, SMARTRULES_INTERNAL_DBG_MSG, null, null,
        		   Constants.PACKAGE, Constants.PACKAGE);

       if (LOG_DEBUG) Log.d(TAG, "Returning from isEnabled = "+result);
       return result;
   }

   
   /** Handles the rule entering and leaving case based on if it is active or inactive
   *
   * @param context - context
   * @param queueItem - item being processed
   * @return true if we want to wait for response from QA, else false
   */
  private static boolean processRuleStateChange(final Context context, 
		   							IntentQueueItem queueItem) { 

	   boolean result = true;
      String state = null;
      boolean processedMessage = false;
      boolean disableRule = false;
      
      if(queueItem.active == RuleTable.Active.INACTIVE) {
          if (isEnabled(mContext, queueItem) && queueItem.isArriving) {
              if(LOG_DEBUG) Log.d(TAG, "Entering "+queueItem.modeName+
            		  						" disableRule = "+disableRule);
              state = context.getString(R.string.active) + COLON + BLANK_SPC 
           		+ context.getString(R.string.arrived) + BLANK_SPC + queueItem.modeName;
              RulePersistence.updateDatabaseTables(context, queueItem.ruleId, 
           		   		queueItem.isArriving, false);
              handleRuleActions(context, queueItem); 
              sendBroadcastResponse(context, queueItem, 
           		   RuleTable.Enabled.ENABLED, RuleTable.Active.ACTIVE);
              processedMessage = true;
          } else if (queueItem.automatic) { 
        	  result = handleAutoRuleEnableDisable(context, queueItem);
          } else {
              if(LOG_DEBUG) Log.d(TAG, "Inactive: "+queueItem.modeName+" ignoring request");
              state = context.getString(R.string.inactive) + COLON + BLANK_SPC 
           		   + context.getString(R.string.ignoring) + BLANK_SPC +
                      context.getString(R.string.leaving) + BLANK_SPC + queueItem.modeName;
              result = false;
          }
      } else if (queueItem.active == RuleTable.Active.ACTIVE) {
          if(queueItem.isArriving || !isEnabled(mContext, queueItem)) {
              if(LOG_DEBUG) Log.d(TAG, "Already in "+queueItem.modeName+" ignoring request");
              state = context.getString(R.string.active) + COLON + BLANK_SPC
                      + context.getString(R.string.ignoring) + BLANK_SPC 
                    + context.getString(R.string.arrived) + BLANK_SPC + queueItem.modeName;
              result = false;
              
          } else {
              if(LOG_DEBUG) Log.d(TAG, "Leaving "+queueItem.modeName);
              state = context.getString(R.string.inactive) + COLON + BLANK_SPC 
           		+ context.getString(R.string.leaving) + BLANK_SPC + queueItem.modeName;
              handleRuleActions(context, queueItem);
              // This is the path to deactivate the rule. So if the intent had 
              // the disable rule flag set to true (for Auto rules going from
              // Active to Disabled state as user selected the icon on the
              // Landing Page) or a Manual Rule (going from Active to Disabled)
              // then we need to disable the rule in the database.
              if(queueItem.disableRule || !queueItem.automatic)
           	   disableRule = true;

              // Update the DB table.
              RulePersistence.updateDatabaseTables(context, queueItem.ruleId, 
           		   queueItem.isArriving, disableRule);
              sendBroadcastResponse(context, queueItem, 
           		   disableRule? RuleTable.Enabled.DISABLED : RuleTable.Enabled.ENABLED, 
           				   RuleTable.Active.INACTIVE);               
              processedMessage = true;
          }
      }

      // Deletion takes precedence over just disabling a rule - so check for it before
      // the disableRule flag.
      if (queueItem.deleteRule) {
   	   // Deleting the rule which marks the rule to disable and will start
   	   // the condition builder so need not call specifically from here.
   	   RulePersistence.deleteRule(mContext, queueItem.ruleId,
   			   queueItem.modeName, queueItem.ruleKey, true);
      } else if (disableRule) {
   	   // Rule is going from active to disabled state so call to disable
   	   // the rule, which will start the condition builder and also notify
   	   // the trigger publishers in this rule to stop.
   	   RulePersistence.markRuleAsDisabled(mContext, queueItem.ruleId, true);
      }
      
      if(processedMessage && queueItem.silent == RuleTable.Silent.TELL_USER
   		   && ! queueItem.flags.equals(RuleTable.Flags.INVISIBLE)) {
          Util.sendMessageToNotificationManager(context, 0);   
      }
      
      DebugTable.writeToDebugViewer(context, DebugTable.Direction.INTERNAL, state, 
   		   queueItem.modeName, queueItem.ruleKey, SMARTRULES_INTERNAL_DBG_MSG, null, null,
   		   Constants.PACKAGE, Constants.PACKAGE);
      
      return result;
  }

   private static boolean handleAutoRuleEnableDisable(Context context, IntentQueueItem queueItem) {
	   boolean result = false;
	   if (queueItem.enabled && queueItem.disableRule ){
    	   if(LOG_DEBUG) Log.d(TAG, "handleAutoRuleEnableDisable: "+queueItem.modeName+ " disabling rule");
    	   RulePersistence.markRuleAsDisabled(mContext, queueItem.ruleId, false);
    	   sendBroadcastResponse(context, queueItem, RuleTable.Enabled.DISABLED, RuleTable.Active.INACTIVE);
    	   result = true;
       } else if (!queueItem.enabled && queueItem.enableRule){
    	   if(LOG_DEBUG) Log.d(TAG, "handleAutoRuleEnableDisable: "+queueItem.modeName+ " enabling rule");
    	   RulePersistence.markRuleAsEnabled(mContext, queueItem.ruleId);
    	   sendBroadcastResponse(context, queueItem, RuleTable.Enabled.ENABLED, RuleTable.Active.INACTIVE);
    	   result = true;
       } else {
    	   if(LOG_DEBUG) Log.d(TAG, "handleAutoRuleEnableDisable: "+queueItem.modeName+ " Ignoring request");
       }
	   return result;
   }

   /** sends a broadcast response for each rule in the cursor
    *  for the rule
    *
    * @param context - context
    * @param queueItem - item being processed
    */
   private static void sendBroadcastResponse(Cursor ruleCursor) {
	   // Send Response to Landing Page that all rules are disabled
	   Intent rulesDiabledIntent = new Intent(INTENT_RULES_DISABLED);
	   mContext.sendBroadcast(rulesDiabledIntent, SMART_RULES_PERMISSION);

	   if(ruleCursor != null) {
		   try{
               if(ruleCursor.moveToFirst()){
                   int keyCol = ruleCursor.getColumnIndexOrThrow(RuleTable.Columns.KEY);
                   int idCol = ruleCursor.getColumnIndexOrThrow(RuleTable.Columns._ID);
                   int typeCol = ruleCursor.getColumnIndexOrThrow(RuleTable.Columns.RULE_TYPE);
                   do{
                       String ruleKey = ruleCursor.getString(keyCol);
                       Long ruleId = ruleCursor.getLong(idCol);
                       int ruleType = ruleCursor.getInt(typeCol);
                       if(LOG_DEBUG) Log.d(TAG, "Sending the broadcast response for diabled rule key "+ ruleKey);
                       Intent responseIntent = new Intent(RULE_PROCESSED_RESPONSE);
                	   responseIntent.putExtra(RuleTable.Columns.KEY, ruleKey);
                	   responseIntent.putExtra(RuleTable.Columns._ID, ruleId);
                	   responseIntent.putExtra(RuleTable.Columns.ACTIVE, RuleTable.Active.INACTIVE);
                	   responseIntent.putExtra(RuleTable.Columns.ENABLED, RuleTable.Enabled.DISABLED);
                	   responseIntent.putExtra(RuleTable.Columns.RULE_TYPE,ruleType);
                	   mContext.sendBroadcast(responseIntent, SMART_RULES_PERMISSION);	
                         
                   } while(ruleCursor.moveToNext());
               }
           } catch (Exception e){
               e.printStackTrace();
           } finally {
               if (!ruleCursor.isClosed())
            	   ruleCursor.close();
           }
       } else {
           Log.e(TAG, "sendBroadcastResponse(): rule cursor is null");
       }
   }
   

/** sends a broadcast response indicating the rule has processed and the current states
    *  for the rule
    * 
    * @param context - context
    * @param queueItem - item being processed 
    */
   private static void sendBroadcastResponse(final Context context, IntentQueueItem queueItem, int enabled, int active) {
	   if(LOG_DEBUG) Log.d(TAG, "Sending the broadcast response for the rule key "+queueItem.ruleKey);
	   if(LOG_DEBUG) Log.d(TAG, "active = "+active+" enabled = "+enabled);
	   
	   Intent responseIntent = new Intent(RULE_PROCESSED_RESPONSE);
	   responseIntent.putExtra(RuleTable.Columns.KEY, queueItem.ruleKey);
	   responseIntent.putExtra(RuleTable.Columns._ID, queueItem.ruleId);
	   responseIntent.putExtra(RuleTable.Columns.ACTIVE, active);
	   responseIntent.putExtra(RuleTable.Columns.ENABLED, enabled);
	   responseIntent.putExtra(RuleTable.Columns.RULE_TYPE, 
			   (queueItem.automatic ? RuleTable.RuleType.AUTOMATIC : RuleTable.RuleType.MANUAL));
	   context.sendBroadcast(responseIntent, SMART_RULES_PERMISSION);	   
   }
   
   /** Fetches the action cursor for the rule and queries for conflicts.
    *
    * @param context - context
    * @param queueItem - item being processed 
    */
   private static void handleRuleActions(final Context context, IntentQueueItem queueItem) {

       if(LOG_DEBUG) Log.d(TAG, "in handleFiringActions for "+queueItem.modeName);
       Cursor actionCursor = null;
       try {
           String whereClause = ActionTable.Columns.PARENT_FKEY + EQUALS + Q + queueItem.ruleId + Q +
			   AND+ActionTable.Columns.ACTION_VALIDITY+EQUALS+Q+TableBase.Validity.VALID+Q;;
           actionCursor = context.getContentResolver().query(Schema.ACTION_TABLE_CONTENT_URI, null, whereClause, null, null);

           if(actionCursor != null) {
               if(actionCursor.moveToFirst() && actionCursor.getCount() > 0) {
                   if(LOG_DEBUG) Log.d(TAG, "Calling to check conflicts");
                   checkAndFireActions(context, actionCursor, queueItem);
               } else {
                   Log.e(TAG, "actionCursor.moveToFirst() failed or count is "+actionCursor.getCount()+" for "+queueItem.modeName);
               }
           } else {
               Log.e(TAG, "action cursor is null");
           }
       } catch (Exception e) {
           Log.e(TAG, PROVIDER_CRASH);
           e.printStackTrace();

       } finally {
           if(actionCursor != null && ! actionCursor.isClosed())
               actionCursor.close();
       }
   }

   /** Checks for conflict for each action in the action cursor for the rule that is being processed
    * 	and fires the actions if needed.
    *
    * @param context - context
    * @param actionCursor - action table cursor that contains the list of actions for the rule that is currently processed
    * @param queueItem - item currently processed
    */
   private static void checkAndFireActions(final Context context, final Cursor actionCursor, IntentQueueItem queueItem) {

       if(LOG_DEBUG) Log.d(TAG, "in checkConflictsAndFireAction for "+queueItem.modeName+" isArriving "+queueItem.isArriving);
       for(int i = 0; i < actionCursor.getCount(); i++) {

    	   ActionTable table = new ActionTable();
    	   ActionTuple t = table.toTuple(actionCursor);
           if(t.getEnabled() == ActionTable.Enabled.ENABLED && t.getActive() == ActionTable.Active.ACTIVE) {
               if(LOG_DEBUG) Log.d(TAG, "for actionID "+ t.get_id() +"; actionPublisherKey is "+ t.getPublisherKey());

               if(t.getModality() == ModalTable.Modality.STATELESS) {
            	   int onModeExit =  t.getOnExitModeFlag();
                   if(LOG_DEBUG) Log.d(TAG, "Action is Stateless & onModeExit = "+onModeExit+"; isarriving is "+queueItem.isArriving);
                   String config = t.getConfig();

                   if(LOG_DEBUG) Log.d(TAG, "for actionId "+ t.get_id() +"; action is "+ t.getPublisherKey());

                   if(onModeExit == ActionPersistence.OnModeExit.ON_ENTER && queueItem.isArriving == true ||
                		   onModeExit == ActionPersistence.OnModeExit.ON_EXIT && queueItem.isArriving == false){
			   Action.sendBroadcastIntentForAction(context, t.getPublisherKey(),
					   queueItem.ruleKey, queueItem.modeName, false,
					   t.getPublisherKey(), COMMAND_FIRE, config, t.get_id());
                   } else {
			   Action.sendBroadcastIntentForAction(context, t.getPublisherKey(),
					   queueItem.ruleKey, queueItem.modeName, false,
					   t.getPublisherKey(), COMMAND_REVERT, config, t.get_id());
                   }
               } else if (t.getModality() == ModalTable.Modality.STATEFUL) {
                   if(LOG_DEBUG) Log.d(TAG, "Action is Stateful");
                   checkConflictsAndFireStatefulActions(context, t, queueItem); 
               } else {
                   Log.e(TAG, "Action ID "+ t.get_id() +" does not have a modal value set. It is currently "+ t.getModality());
               }
           } else {
               Log.e(TAG, "Action "+ t.get_id() +" is not currently active ignoring the firing");
           }
           actionCursor.moveToNext();
       }
   }

   /** Checks for conflicts for stateful actions and handles it accordingly.
    *
    * @param context - context
    * @param actionCursor - action table cursor that contains the list of actions for the rule that is currently processed
    * @param queueItem - queue item currently processed
    */
   private static void checkConflictsAndFireStatefulActions(final Context context, final ActionTuple t, 
		   														final IntentQueueItem queueItem) {

       Cursor conflictingActionsCursor = null;
       try {
           if(LOG_DEBUG) Log.d(TAG, "fetching the conflicts for the publisher key "+ t.getPublisherKey());
           conflictingActionsCursor = Conflicts.getConflictingActionsCursor(context, 
        		                         t.getPublisherKey(), Conflicts.Type.ACTIVE_ONLY);
           if(conflictingActionsCursor != null) {
               if(conflictingActionsCursor.moveToFirst()) {
                   if(t.get_id() == conflictingActionsCursor.getLong(conflictingActionsCursor.getColumnIndex(Conflicts.ACTION_ID))) {
                       if(LOG_DEBUG) Log.d(TAG, "Current rule's action is at the top of the conflictingactions stack");
                       if(queueItem.isArriving) {
                           if (LOG_DEBUG) Log.d(TAG, "for actionId "+ t.get_id() +"; action is "+ t.getPublisherKey());
                           boolean saveDefault = ActionPersistence.isDefaultValueNeeded(context, 
                        		         t.getPublisherKey(), t.getDescription(), t.getStateMachineName(), t.getActivityIntent());
                           if (LOG_DEBUG) Log.d(TAG, "saveDefault = "+saveDefault);                        
                           if (LOG_DEBUG) Log.d(TAG, "Adding to Q "+ t.getPublisherKey());
                           queueItem.responseWaitList.add(t.get_id(), t.getPublisherKey(), queueItem.ruleKey);
                        
                           Action.sendBroadcastIntentForAction(context, t.getPublisherKey(),
							queueItem.ruleKey, queueItem.modeName, saveDefault,
							t.getPublisherKey(), COMMAND_FIRE, t.getConfig(), t.get_id());
                           // Update the ActionTable.Columns.CONFLICT_WINNER_FLAG 
                           ActionPersistence.updateConflictWinnerFlag(context, t.get_id(), t.getPublisherKey(), 
                        		   ActionTable.ConflictWinner.WINNER);
                           
                       } else {
                           if(conflictingActionsCursor.moveToNext()) {
                               long nextRuleId = conflictingActionsCursor.getLong(conflictingActionsCursor.getColumnIndex(RuleTable.Columns._ID));
                               String nextRuleName = conflictingActionsCursor.getString(conflictingActionsCursor.getColumnIndex(RuleTable.Columns.NAME)); 
                               String config = conflictingActionsCursor.getString(conflictingActionsCursor.getColumnIndexOrThrow(ActionTable.Columns.CONFIG));
                               if(LOG_DEBUG) Log.d(TAG, "Rule ID for the next rule is "+nextRuleId);
                               String nextRuleKey = conflictingActionsCursor.getString(conflictingActionsCursor.getColumnIndex(RuleTable.Columns.KEY));
                               boolean restoreDefault = RulePersistence.isNextRuleDefaultRule(context, nextRuleKey);
                               
                               if (LOG_DEBUG) Log.d(TAG, "Adding to Q "+ t.getPublisherKey());
                               queueItem.responseWaitList.add(t.get_id(), t.getPublisherKey(), queueItem.ruleKey);
         
                               //If restoreDefault is true, then this rule is the default record, hence revert the action,
                               // else "fire" the action for this record.
                               if(restoreDefault){
                                   Action.sendBroadcastIntentForAction(context, t.getPublisherKey(), nextRuleKey, nextRuleName, false,
						    t.getStateMachineName(), COMMAND_REVERT, config, t.get_id());
                            	   ActionPersistence.setDefaultRecordForActionInactive(context, t.getPublisherKey());
                               } else {
                                   Action.sendBroadcastIntentForAction(context, t.getPublisherKey(), nextRuleKey, nextRuleName, false,
						     t.getStateMachineName(), COMMAND_FIRE, config, t.get_id());
                               }
                               
                               // Update the ActionTable.Columns.CONFLICT_WINNER_FLAG 
                               ActionPersistence.updateConflictWinnerFlag(context, 
                            		          conflictingActionsCursor.getLong(conflictingActionsCursor.getColumnIndex(Conflicts.ACTION_ID)), 
                               				  t.getPublisherKey(), ActionTable.ConflictWinner.WINNER);
                           } else {
                               if(LOG_DEBUG) Log.d(TAG, "cannot move to the next record in the conflicting actions cursor - restore default");
                               ActionPersistence.fetchAndFireActionForDefaultRule(context, t.getPublisherKey(), true);
                               // Update the ActionTable.Columns.CONFLICT_WINNER_FLAG 
                               ActionPersistence.updateConflictWinnerFlag(context, t.get_id(), t.getPublisherKey(), 
                            		   ActionTable.ConflictWinner.LOSER);
                           }
                       }
                   } else {
                       Log.e(TAG, "Conflict: id's do not match actionId = "+ t.get_id() +"; conflict cusror id = "+conflictingActionsCursor.getLong(conflictingActionsCursor.getColumnIndex(ActionTable.Columns._ID)));                        
                       DebugTable.writeToDebugViewer(context, DebugTable.Direction.OUT, CONFLICT_ACTION_NOT_FIRED, 
                    		   						 queueItem.modeName, queueItem.ruleKey, SMARTRULES_INTERNAL_DBG_MSG, 
										 t.getStateMachineName(), null, Constants.PACKAGE, Constants.PACKAGE);
                       // Update the ActionTable.Columns.CONFLICT_WINNER_FLAG 
                       ActionPersistence.updateConflictWinnerFlag(context, t.get_id(), t.getPublisherKey(), 
                    		   ActionTable.ConflictWinner.LOSER);
                   }
               } else {
                   Log.e(TAG, "conflictingActionsCursor.moveToFirst() failed");
               }
           }
       } catch (Exception e) {
           Log.e(TAG, "Crash while fetching the conflicting actions");
           e.printStackTrace();
       } finally {
           if(conflictingActionsCursor != null && ! conflictingActionsCursor.isClosed())
               conflictingActionsCursor.close();
       }
   }
   
   
	/** Registers a receiver for receiving the QA intent for every 
	 *  
	 * @param context - context
	 */    
	private BroadcastReceiver registerReceiver(Context context){
		
		// register the receiver
		BroadcastReceiver result = null;
		context.registerReceiver(result = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				
				if(LOG_INFO) Log.i(TAG, "In onReceive to handle "+intent.toUri(0));

				Bundle bundle = intent.getExtras();
				
				if(bundle == null) {
					Log.e(TAG, "Error - Intent bundle is null");
				} else {						
					String actPubKey = intent.getStringExtra(EXTRA_PUBLISHER_KEY);
					String ruleKey = intent.getStringExtra(QA_RULE_KEY); 
					if (LOG_DEBUG) Log.d(TAG, "actPubKey is " + actPubKey +" ruleKey is " + ruleKey );
	
					if (actPubKey == null) {
						// should ALWAYS have an action publisher key
						Log.e (TAG, "received null actPubKey in response from QA:"+intent.toUri(0));
						
					} 
					else {
						if (mIntentQueue != null) {
							WaitListItem item = mIntentQueue.removeMatchingIntentResponse(actPubKey);					
							if (item == null) {
								// this could be a Stateless response, so it may not be in the responseWaitList 
								Log.e(TAG, "Response not found, null return actPubKey="+actPubKey+" rule="+ruleKey);
							}
						}else
							Log.e(TAG, " Error - Intent queue is null");
					}
				}
			}
			
		}, new IntentFilter(QA_EXEC_STATUS_PROCESSED)); 
		if (LOG_DEBUG) Log.d(TAG, "registered receiver");
		return result;
	}
	
	
	// -------------------------  queue handling code ------------------------------
	
    
    /** This class contains a wait list item coming from Quick Actions. */
    private static class WaitListItem implements java.io.Serializable{
    	
		private static final long serialVersionUID = -4000L;

    	long actionId = -1;
    	String ruleKey;
    	String actionPubKey;
    	
		public WaitListItem(long actionId, String ruleKey, String actionPubKey) {
			super();
			this.actionId = actionId;
			this.ruleKey = ruleKey;
			this.actionPubKey = actionPubKey;
		}  


		@Override
		public String toString() {
    		StringBuilder builder = new StringBuilder();
    		builder.append(" key="+ruleKey)
    			.append(", actionId="+actionId)
    			.append(", actPubKey="+this.actionPubKey);
    		return builder.toString();
		}
    }

    /** holds all the stateful actions -- awaiting a response from quick actions,
     * String key is the Action Publisher Key. There should never be more than 1 action publisher key
     * of the same type being waited on at one time because it is a stateful action, therefore,
     * there should never be more than one stateful action allowed in a single rule. */
    private static class ResponseWaitList extends HashMap<String, WaitListItem> implements java.io.Serializable {
    	
		private static final long serialVersionUID = -3000L;

		public synchronized void add(long actionId, String actionPubKey, String ruleKey) {
			   
			WaitListItem actHolder = new WaitListItem(actionId, ruleKey, actionPubKey);		   
			this.put(actionPubKey, actHolder);
			if (LOG_DEBUG) Log.d(TAG, "ResponseWaitList Adding " + actionPubKey);
		}
	   
		public long getWaitTimeMs() {
		   return RESPONSE_WAIT_TIME_BASE + this.size() * RESPONSE_WAIT_TIME_PER_ACTION;
		}
		
		public String toString() {
			StringBuilder builder = new StringBuilder();
			
			Set<String> keys = this.keySet();
			Iterator<String> keyIter = keys.iterator();
			while (keyIter.hasNext()) {
				builder.append(", "+this.get(keyIter.next()).toString());
			}
			
			return builder.toString();
		}
    }

    private static class IntentQueueItem implements java.io.Serializable {
    	
		private static final long serialVersionUID = -2000L;    	
    	
    	private long   	timestamp;
    	private Intent intent; 
    	private long	ruleId;
    	private long 	silent;
    	private int		active;
    	private boolean isArriving;
    	private boolean enableRule;
    	private boolean disableRule;
    	private boolean deleteRule;
    	private String 	ruleKey;
    	private String 	ruleStatus;
    	private String 	modeName;
    	private String	flags;
    	private boolean enabled;
    	private boolean automatic;
    	private ResponseWaitList responseWaitList = new ResponseWaitList();
    	
    	public IntentQueueItem(final Intent ruleIntent) {
		       
    		this.timestamp = new Date().getTime();
    		this.intent = ruleIntent;
    	}
    	
    	public void populateRuleInfo(final Cursor ruleCursor, final String ruleKey, 
    									final String ruleStatus, final boolean enableRule,
    									final boolean disableRule, final boolean deleteRule) {
    		this.isArriving = ruleStatus.equals(TRUE);
    		this.ruleKey = ruleKey;
    		this.ruleStatus = ruleStatus;
    		this.enableRule = enableRule;
    		this.disableRule = disableRule;
    		this.deleteRule = deleteRule;
    		this.modeName = ruleCursor.getString(ruleCursor.getColumnIndex(RuleTable.Columns.NAME));
    		this.active = ruleCursor.getInt(ruleCursor.getColumnIndex(RuleTable.Columns.ACTIVE));
    		this.ruleId = ruleCursor.getInt(ruleCursor.getColumnIndex(RuleTable.Columns._ID));
    		this.silent = ruleCursor.getLong(ruleCursor.getColumnIndex(RuleTable.Columns.SILENT));
    	    this.flags = ruleCursor.getString(ruleCursor.getColumnIndex(RuleTable.Columns.FLAGS));
    	    this.enabled = ruleCursor.getInt(ruleCursor.getColumnIndex(RuleTable.Columns.ENABLED)) 
    	    					== RuleTable.Enabled.ENABLED;
    	    this.automatic = ruleCursor.getInt(ruleCursor.getColumnIndex(RuleTable.Columns.RULE_TYPE)) == 
    	    					RuleTable.RuleType.AUTOMATIC;
    	    
    	    if(LOG_DEBUG) Log.d(TAG, "Exiting populateRuleInfo with the queueItem as "+this.toString());
    	    					
    	}
    	
    	@Override
    	public String toString() {
    		StringBuilder builder = new StringBuilder();
    		builder.append(timestamp)
    			.append("intent="+intent.toUri(0))
    			.append(";isArriving="+isArriving)
    			.append(";ruleKey="+ruleKey)
    			.append(";ruleStatus="+ruleStatus)
    			.append(";enableRule="+enableRule)
    			.append(";disableRule="+disableRule)
    			.append(";modeName="+modeName)
    			.append(";isActive="+active)
    			.append(";ruleId="+ruleId)
    			.append(";silent="+silent)
    			.append(";flags="+flags)
    			.append(";enabled="+enabled)
    			.append(";automatic="+automatic)
    			
    			;
    		return builder.toString();
    	}

    }; 


    /** manages the queue of Intent requests coming from VSM */
    private static class IntentQueue extends Vector<IntentQueueItem> implements java.io.Serializable {

		private static final long serialVersionUID = -1000L;
		
		public IntentQueue() {
			super();
		}


		public synchronized boolean add(IntentQueueItem queueItem) {
			
			return super.add(queueItem);
		}
		
		
		/** removes the waiting intent response */
		public synchronized WaitListItem removeMatchingIntentResponse(String actPublKey) {
			
			if (LOG_DEBUG) Log.d(TAG, " Removing from waitlist " + actPublKey);
			WaitListItem result = null;
			if (actPublKey == null) {
				Log.e(TAG, "actPublKey key cannot be null");
			} else {
				if (this.size() > 0) {
					result = peekHead().responseWaitList.remove(actPublKey);	
				}
			}
			return result;
		}
		
		public synchronized IntentQueueItem peekHead() {
			if (this.isEmpty()) return null;
    		return this.firstElement();
    	}
    	
    	public synchronized IntentQueueItem popHead() {
    		IntentQueueItem result = null;
    		
    		if (!this.isEmpty()) {
    			if (LOG_DEBUG) Log.d(TAG, " Removing from IntentQueueItem ");
    			result = this.remove(0); 
    			
    		}
    		return result;
    	}

    }
    
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
}
