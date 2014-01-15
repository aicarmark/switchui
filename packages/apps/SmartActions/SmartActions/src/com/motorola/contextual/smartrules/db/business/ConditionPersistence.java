/*
 * @(#)ConditionPersistence.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/04/20 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.SQLiteManager;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.Schema.ConditionTableColumns;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.ConditionTuple;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.view.TriggerStateCountView;
import com.motorola.contextual.smartrules.util.Util;

/** This class holds the handlers that deal with query, insert, update and delete on
 * 	Condition Table Columns.
 *
 *<code><pre>
 * CLASS:
 * 	 extends ConditionTable
 *
 *  implements
 *   Constants - for the constants used
 *   DbSyntax - for the DB related constants
 *
 * RESPONSIBILITIES:
 *   None.
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 *</pre></code>
 */
public class ConditionPersistence extends ConditionTable implements Constants, DbSyntax {

    private static final String TAG = ConditionPersistence.class.getSimpleName();

    /** returns the count of enabled conditions for the rule Id
     * 
     * @param context - context
     * @param ruleId - parent rule Id
     * @return - count of enabled conditions in the rule
     */
    public static int getEnabledConditionsCount(final Context context, long ruleId) {
    	int count = 0;
    	String whereClause = Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q 
    				+ AND + Columns.ENABLED + EQUALS + Q + Enabled.ENABLED + Q;
    	
    	Cursor cursor = context.getContentResolver()
    			.query(Schema.CONDITION_TABLE_CONTENT_URI, null, whereClause, null, null);
    	
    	if(cursor == null) {
    		Log.e(TAG, "cursor returned is null for "+whereClause);
    	} else {
    		try {
    			if(cursor.moveToFirst())
    				count = cursor.getCount();
    		} catch (Exception e) {
    			Log.e(TAG, "Exception processing cursor");
    			e.printStackTrace();
    		} finally {
    			if(! cursor.isClosed())
    				cursor.close();
    		}
    	}
    	if(LOG_DEBUG) Log.d(TAG, "returning the # of enabled " +
    							"conditions "+count+" for rule "+ruleId);
    	return count;
    }
    
    /** Fetches a list of Conditions and Condition Sensors associated with the rule. 
     * Returns a list of type Condition.
     *  
     * @param context - context
     * @param ruleId - rule ID in the rule table
     * @return list of conditions (Condition and Condition Sensors) associated with the rule
     */
	@SuppressWarnings("unchecked")
	public <E extends Condition> ConditionList<Condition> fetch(Context context, long ruleId) {
    
    	String whereClause = Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q;
    	ArrayList<ConditionTuple> conditionTupleList = this.fetchList(context, whereClause);
    	
    	ConditionList<Condition> conditionList = null;
    	if(conditionTupleList != null) {
    		conditionList = new ConditionList<Condition>();
    		for (ConditionTuple t: conditionTupleList) {
    			conditionList.add((E) new Condition(t));
    		}
    	}    	
    	return conditionList;
    }
 
	/** inserts conditions to the database.
	 * 
     * @param db - database instance
     * @param list - list of conditions (Condition and Condition Sensors) to be inserted
	 */
	public void insertList(SQLiteManager db, ConditionList<Condition> list) {  	
    	for(Condition condition: list) {
		super.insert(db, condition);
    	}
	}

    /** updates the list of actions in the DB.
     * 
     * @param db - database instance
     * @param list - list of actions to be updated
     */
    public void updateList(SQLiteManager db, ConditionList<Condition> list) {  	
    	for(ConditionTuple condition: list) {
    		if(condition.isNew()) {
    			super.insert(db, condition);
    		}
    		else if(condition.isLogicalDelete()) {
    			String whereClause = 
    					WHERE + Columns._ID + EQUALS + Q + condition.get_id()  + Q;
    			super.deleteWhere(db, whereClause);
    		} else if (condition.isDirtyFlag()) {
    			super.update(db, condition);
    		} 
    	}
    }
    
    /** Inserts a Condition Tuple into the Condition Table
     * 
     * @param context
     * @param conditionTuple
     * @return condition key
     */
    public static Uri insertCondition(Context context, ConditionTuple conditionTuple){
    	Uri conditionKey = null;
    	
    	try{
    		conditionKey = 
    			context.getContentResolver().insert(Schema.CONDITION_TABLE_CONTENT_URI, 
    												new ConditionTable().toContentValues(conditionTuple));
    	} catch (Exception e) {
    		Log.e(TAG, "Insert to Condition Table failed");
    		e.printStackTrace();
    	}
    	return conditionKey;
    }
    
    /** Deletes a Condition Tuple from the Condition Table.
     * 
     * @param context
     * @param whereClause
     */
    public static void deleteCondition(Context context, String whereClause){
    	try {
    		context.getContentResolver().delete(Schema.CONDITION_TABLE_CONTENT_URI, whereClause, null);
    	} catch (Exception e) {
    		Log.e(TAG, "Delete from Condition Table failed");
    		e.printStackTrace();
    	} 
    }


    /** Returns the Condition Table Cursor
     *  Note: The caller is expected to close the Cursor
     *
     *  @param context
     *  @param ruleId - the rule ID for Conditions
     *
     *  @return - conditionCursor - Condition Table Cursor
     */
    public static Cursor getConditionCursor(Context context, final long ruleId) {

        Cursor conditionCursor = null;

        if (context != null)
        {
            try {
                String tableSelection = Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q;
                conditionCursor = context.getContentResolver().query(
                                      Schema.CONDITION_TABLE_CONTENT_URI,
                                      null, tableSelection, null,  null);
            } catch(Exception e) {
                Log.e(TAG, "Query failed for Condition Table");
            }
        } else {
            Log.e(TAG,"context is null");
        }

        return conditionCursor;
    }

    /**
     * Use this method to update the Validity column of Condition publisher
     * @param context - Context
     * @param ruleKey Rule Key
     * @param config - configuration of the condition
     * @param conditionPublisherKey - condition publisher key for the condition
     * @param conditionValidity - the new validity value for the condition
     */
    public static void updateConditionValidity(final Context context, String ruleKey,
            final String conditionPublisherKey, final String config, final String conditionValidity) {
	    if(LOG_DEBUG) Log.d(TAG, "updateConditionValidity: " + conditionPublisherKey +
			"\n config :" + config + "ruleKey: " + ruleKey + " actionValidity: " + conditionValidity);
        ContentResolver cr = context.getContentResolver();
        ContentValues cv = new ContentValues();
        cv.put(ConditionTableColumns.CONDITION_VALIDITY, conditionValidity);
        String where = null;
        String[] whereArgs = null;
        //If ruleKey is null, then update Action Validity for all enties in action table 
        // with this publisher key, else form the query based on the available config
        // ruleKey and update accordingly.
        try {
            if(ruleKey == null){
                where = ConditionTableColumns.CONDITION_PUBLISHER_KEY + EQUALS + Q + conditionPublisherKey + Q;
                cr.update(ConditionTable.CONTENT_URI, cv, where, null);
            }else {
                long ruleId = RulePersistence.getRuleIdForRuleKey(context, ruleKey);
                // If rulekey is not null, check if config is provided. If yes, add config also
                // to the query arguments
                if(config != null) {
			// If ruleId is default rule id, do not add rule id to query arguments
			// Else add rule id to query arguments
                    if(ruleId == DEFAULT_RULE_ID) {
                        where = ConditionTableColumns.CONDITION_PUBLISHER_KEY + EQUALS + ANY +
	                            AND + ConditionTableColumns.CONDITION_CONFIG + EQUALS + ANY;
                        whereArgs = new String[]{conditionPublisherKey, config};
                    } else {
                        where = ConditionTableColumns.CONDITION_PUBLISHER_KEY + EQUALS + ANY +
	                           AND + ConditionTableColumns.PARENT_FKEY + EQUALS + ANY +
	                                   AND + ConditionTableColumns.CONDITION_CONFIG + EQUALS + ANY;
                        whereArgs = new String[]{conditionPublisherKey, Long.toString(ruleId), config};
                    }
                } else {
			// If config is null, check if ruleId is default rule id, If so
			// do not add rule key to query arguments, else add rule id to query arguments
                    if(ruleId == DEFAULT_RULE_ID) {
                        where = ConditionTableColumns.CONDITION_PUBLISHER_KEY + EQUALS + ANY;
                        whereArgs = new String[]{conditionPublisherKey};
                    } else {
                        where = ConditionTableColumns.CONDITION_PUBLISHER_KEY + EQUALS + ANY +
	                        AND + ConditionTableColumns.PARENT_FKEY + EQUALS + ANY ;
                        whereArgs = new String[]{conditionPublisherKey, Long.toString(ruleId)};
                    }
                }
                cr.update(ConditionTable.CONTENT_URI, cv, where, whereArgs);
	        }
        }catch(Exception e){
		   e.printStackTrace();
        }


    }
    
    /** updates the Condition table entry
     *
     * @param context - context
     * @param updateValues - content values used to update the row
     * @param whereClause - where clause for the update
     * @return number of rows updated
     */
    public static int updateCondition(Context context, ContentValues updateValues, String whereClause){
        return context.getContentResolver().update(Schema.CONDITION_TABLE_CONTENT_URI, updateValues, whereClause, null);
    }

    private static final String PUBLISH_EXTRA = "com.motorola.contextual"+".PUBLISH";
    private static final String TIMESTAMP_EXTRA = "com.motorola.contextual"+".TIMESTAMP";
    public static final String START = "start";
    public static final String STOP = "stop";

    /** fetches the list of publisher keys that needs to be notified about either starting or 
     * 	stopping listening to state changes and sends the notification.
     * 
     * @param context - context
     * @param _id - rule ID of the rule either being disabled or enabled
     * @param isEnabling - true if rule is being enabled else false
     */
    public static void notifyConditionPublishers(final Context context, final long _id, boolean isEnabling) {
		
    	ArrayList<String> pubKeyArrayList = getAllEnabledPubKeyArrayList(context, _id);
    	
    	pubKeyArrayList = getPublisherKeyArrayListToNotify(context, pubKeyArrayList, isEnabling);
    	
    	if(pubKeyArrayList.size() > 0) {
    		
    		String publish = START;    		
    		long timeStamp = new Date().getTime();
    		ListIterator<String> iter = pubKeyArrayList.listIterator();

    		if(iter == null)
    			Log.e(TAG, "Nothing to send");
    		else {
    			while (iter.hasNext()) {
    				String pubKey = iter.next();
    				if(pubKey.equals(MOTION_SENSOR_PUB_KEY) && 
    						! pubKeyArrayList.contains(LOCATION_TRIGGER_PUB_KEY)) {
    					if(LOG_DEBUG) Log.d(TAG, "List contains motion but does not contain " +
    										"location sensor so do not send stop but just send start to motion sensor trigger");
    					publish = START;
    				} else
    					publish = isEnabling ? START : STOP;
    				
    				if(LOG_DEBUG) Log.d(TAG, "Sending "+publish+" for "+pubKey);
    				sendBroadcastToPublisher(context, pubKey, publish, timeStamp);    				
    			}
    		}
    			
    	} else
    		if(LOG_DEBUG) Log.d(TAG, "No broadcasts needs to be sent");
    }
    
    /** sends a broadcast intent to the publisher keys
     * 
     * @param context - context
     * @param pubKey -  publisher keys
     * @param publish - start or stop listening to the state changes
     * @param timeStamp - current timestamp
     */
    public static void sendBroadcastToPublisher(final Context context, final String pubKey, 
    						final String publish, final long timeStamp) {
		Intent intent = new Intent();
		intent.setAction(pubKey);
		intent.putExtra(PUBLISH_EXTRA, publish);
		intent.putExtra(TIMESTAMP_EXTRA, timeStamp);
		context.sendBroadcast(intent);
    }
    
    /** fetches the condition table cursor of enabled triggers for the rule ID.
     * 
     * @param context - context
     * @param _id - rule ID for which we need the list of enabled triggers
     * @return - condition table cursor of enabled conditions
     */
    public static Cursor getEnabledConditionsCursor(Context context, final long _id) {
    	String whereClause = ConditionTable.Columns.PARENT_FKEY + EQUALS + Q + _id + Q 
					+ AND + ConditionTable.Columns.ENABLED + EQUALS + Q + ConditionTable.Enabled.ENABLED + Q;

	String[] projection = {ConditionTable.Columns.CONDITION_PUBLISHER_KEY};

    	return new ConditionTable().fetchWhere(context, projection, whereClause, null, null, 0);
    }

    /** returns the list of publisher keys that needs to be notified about stop or start 
     * 	listening for state changes for that trigger.
     * 
     * @param context - context 
     * @param pubKeyArrayList - complete array list of publisher keys from which to select
     * @param isEnabling - true if rule is being enabled else false
     * @return - an array list of publisher keys that needs to be notifed
     */
    private static ArrayList<String> getPublisherKeyArrayListToNotify(final Context context,
    												ArrayList<String> pubKeyArrayList, final boolean isEnabling) {
    	
    	String pubKeyList = Util.toCommaDelimitedQuotedString(pubKeyArrayList.toArray(new String[0]));
    	
    	Cursor triggerStateCntCursor = getTriggerStateViewCursor(context, pubKeyList);
		if(triggerStateCntCursor == null) {
			Log.e(TAG, "getTriggerStateViewCursor returned null");
		} else {
			try {
				if (triggerStateCntCursor.moveToFirst()) {
					do {
						String pubKey = triggerStateCntCursor.getString(triggerStateCntCursor.getColumnIndex(ConditionTable.Columns.CONDITION_PUBLISHER_KEY));
						int count = triggerStateCntCursor.getInt(triggerStateCntCursor.getColumnIndex(TriggerStateCountView.Columns.TRIGGER_COUNT_BY_STATUS));

						if(LOG_DEBUG) Log.d(TAG, "count is "+count+" for pub key "+pubKey);
						if((isEnabling && count > 1) ||
								(!isEnabling && count > 0))
							pubKeyArrayList.remove(pubKey);
					} while (triggerStateCntCursor.moveToNext());
				}
			} catch (Exception e) {
				Log.e(TAG, "Exception processing the triggerStateCntCursor");
				e.printStackTrace();
			} finally {
				if (!triggerStateCntCursor.isClosed())
					triggerStateCntCursor.close();
			}
		}		
		
		if(LOG_DEBUG) Log.d(TAG, "returning from getPublisherKeyArrayListToNotify "+pubKeyArrayList.toString());
		return pubKeyArrayList;
    }
 
    /** Returns the list of condition publisher keys corresponding to a Rule Key.
    *
    * @param context
    * @param ruleFk - Rule table foreign key
    */
    public static List<String> getPublisherKeys(Context context, long ruleFk) {

        List<String> condPubKeys = new ArrayList<String>();

        if (context != null) {

            String whereClause = ConditionTable.Columns.PARENT_FKEY + DbSyntax.EQUALS + DbSyntax.Q +
                                 ruleFk + DbSyntax.Q;

            Cursor cursor = context.getContentResolver().query(Schema.CONDITION_TABLE_CONTENT_URI,
                         new String[] { ConditionTable.Columns.CONDITION_PUBLISHER_KEY},
                         whereClause, null, null);

            if (cursor == null) {
                Log.e(TAG, "Null cursor for " + whereClause);
                return condPubKeys;
            }

            try {
                if (cursor.moveToFirst()) {
                    do {
                        condPubKeys.add(cursor.getString(cursor.getColumnIndexOrThrow(ConditionTable.Columns.CONDITION_PUBLISHER_KEY)));
                    } while(cursor.moveToNext());
                }
            } catch(IllegalArgumentException e) {
                Log.e(TAG, "Query failed for " + whereClause);
            }
            finally {
                cursor.close();
            }
        }

        return condPubKeys;
    }

    /** fetches the cursor of count for each conditions/trigger that is enabled using the
     * 	Trigger State View.
     * 
     * @param context - context
     * @param pubKeyList - list of publisher keys
     * @return - cursor of publisher keys that have a count of >= 1
     */
    public static Cursor getTriggerStateViewCursor(final Context context, final String pubKeyList) {
    	
	String whereClause = ConditionTable.Columns.CONDITION_PUBLISHER_KEY + IN + LP + pubKeyList + RP
				+ AND + RuleTable.Columns.ENABLED + EQUALS + Q + RuleTable.Enabled.ENABLED + Q;
    	
		return context.getContentResolver().query(Schema.TRIGGER_STATE_CNT_VIEW_CONTENT_URI, 
					null, whereClause, null, null);
    }
    
    /** retrieves the publishers keys of enabled triggers for the rule ID passed. 
     * 
     * @param context - context
     * @param _id - rule ID for which we need the list of publisher keys
     * @return - an array list of publisher keys
     */
    private static ArrayList<String> getAllEnabledPubKeyArrayList(final Context context, final long _id) {
    	
    	Cursor conditionCursor = getEnabledConditionsCursor(context, _id);

    	ArrayList<String> pubKeyArrayList = new ArrayList<String>();

		if(conditionCursor == null) {
			Log.e(TAG, "No triggers for the rule "+_id);
		} else {
			if(conditionCursor.moveToFirst()) {
				do {
					pubKeyArrayList.add(conditionCursor.getString(0));					
				} while (conditionCursor.moveToNext()); 
			}
			conditionCursor.close();
		}
		
		if(LOG_DEBUG) Log.d(TAG, "Returning from getAllEnabledPubKeyArrayList "+pubKeyArrayList.toString());
		return pubKeyArrayList;
    }    


   /**
     * Returns the Condition  Table Cursor
     * Note: The caller is expected to close the Cursor
     *
     * @param context
     * @param tableSelection - where clause
     * @param columns - columns to fetch
     *  @return - condCursor - Condition Table Cursor
     */
   public static Cursor getConditionCursor(Context context, final String tableSelection,
           String[] columns) {

       Cursor condCursor = null;

       if(Util.isNull(tableSelection)) return condCursor;

       if (context != null)
       {
           try {
               condCursor = context.getContentResolver().query(
                                      Schema.CONDITION_TABLE_CONTENT_URI,
                                      columns, tableSelection, null, null);
           } catch(Exception e) {
               Log.e(TAG, "Query failed for Condition Table");
           }
       } else {
           Log.e(TAG,"context is null");
       }

       return condCursor;
   }
    

     /** Returns the list of rule keys corresponding to a condition publisher Key.
     *
     * @param context
     * @param condPubKey - condition publisher key of the condition
     */
     public static List<String> getRuleKeys(Context context, String condPubKey) {
         List<String> ruleKeys = new ArrayList<String>();

         if (context != null && condPubKey != null) {

             Cursor cursor = null;
             String whereClause = ConditionTable.Columns.CONDITION_PUBLISHER_KEY + EQUALS + Q +
			 condPubKey + Q;
             try {

                 cursor = context.getContentResolver().query(Schema.CONDITION_TABLE_CONTENT_URI,
                          new String[] { ConditionTable.Columns.PARENT_FKEY},
                          whereClause, null, null);
                 if (cursor != null && cursor.moveToFirst()) {
                     do {
                            long ruleId = cursor.getLong(cursor.getColumnIndex(ConditionTable.Columns.PARENT_FKEY));
                            ruleKeys.add(RulePersistence.getRuleKeyForRuleId(context, ruleId));
                     } while(cursor.moveToNext());

                 }
             } catch(Exception e) {
                 Log.e(TAG, "Query failed for " + whereClause);
             }
             finally {
                 if (cursor != null) {
                     cursor.close();
                 }
             }
         }else {
         	Log.e(TAG, " Input parameters are null + context : " + context + " condPubKey : " + condPubKey);
         }

         return ruleKeys;
     }

     /** fetches and returns the  Enabled value from the condition table for the publisher key passed in.
     *
     * @param context - context
     * @param publisherKey - publisher key in the rule table
     * @param ruleId - Rule Id
     * @return - the Enabled field value of the condition that matches the publisher key in the table
     */
     public static int getEnabledForPublisherKey(final Context context, final String publisherKey, long ruleId) {
         int enabled = 0;
         String whereClause = ConditionTable.Columns.CONDITION_PUBLISHER_KEY + EQUALS + Q +
                 publisherKey + Q  + AND + ConditionTable.Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q;
         Cursor cursor = context.getContentResolver().query(Schema.CONDITION_TABLE_CONTENT_URI, new String[] {Columns.ENABLED},
                             whereClause, null, null);
         if(cursor != null) {
             try {
                 if(cursor.moveToFirst()) {
                     enabled = cursor.getInt(cursor.getColumnIndex(Columns.ENABLED));
                 } else
                     Log.e(TAG, "cursor.moveToFirst() failed for "+whereClause);
             } catch (Exception e) {
                 Log.e(TAG, PROVIDER_CRASH);
                 e.printStackTrace();
             } finally {
                 if(!cursor.isClosed())
                     cursor.close();
             }
         } else
             Log.e(TAG, " cursor fetched is null for whereClause "+whereClause);

         if(LOG_DEBUG) Log.d(TAG, "returning Enabled as "+enabled+" for publisherKey "+publisherKey);
         return enabled;
     }
     
     /**
      * This method helps to find out if there any blocks which are enabled and 
      * un configured.
      * @param context
      * @param ruleId
      * @return boolean - true - un configured enabled blocks exist
      */
     public static boolean anyUnconfiguredEnabledConditions(final Context context, long ruleId) {
    	 
    	 if (LOG_DEBUG) Log.d(TAG,"Condition: anyConnectedUnConfiguredBlocks");
    	 
    	 // No un configured connected blocks
    	 boolean status = false;
    	 
    	 String columns[] = {ConditionTable.Columns.CONDITION_CONFIG};
    	 
         String whereClause = ConditionTable.Columns.PARENT_FKEY  + EQUALS + Q + ruleId + Q +AND +
         					  ConditionTable.Columns.ENABLED + EQUALS + Q + Enabled.ENABLED  +Q;
         
         Cursor condCursor = getConditionCursor(context, whereClause, columns);

         if(condCursor == null){
             Log.e(TAG, "Null Cursor in anyConnectedUnConfiguredBlocks");
             return status;
         }

         try{
             if(condCursor.moveToFirst()){
                 do{
                     String config = condCursor.getString(condCursor.getColumnIndexOrThrow(
                     						ConditionTable.Columns.CONDITION_CONFIG));
                     if (status = Util.isNull(config))  break;  
                 } while(condCursor.moveToNext());
             } 
         } catch (IllegalArgumentException e) {
             Log.e(TAG, "IllegalArgumentException in anyConnectedUnConfiguredBlocks");
             e.printStackTrace();
         } finally {
        	 if(condCursor != null) condCursor.close();
         } 
         
         return status;
     }
}
