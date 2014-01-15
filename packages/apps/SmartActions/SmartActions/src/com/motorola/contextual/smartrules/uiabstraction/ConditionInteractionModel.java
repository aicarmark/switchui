/*
 * @(#)ConditionInteractionModel.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA MOBILITY, INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2011/05/09 NA				  Initial version
 * A18385        2011/06/20 NA                Added Condition business logic to this file
 */
package com.motorola.contextual.smartrules.uiabstraction;

import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.business.Condition;
import com.motorola.contextual.smartrules.db.business.ConditionPersistence;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.db.table.ConditionTable.Enabled;
import com.motorola.contextual.smartrules.db.table.ConditionTuple;
import com.motorola.contextual.smartrules.db.table.view.TriggerStateCountView;
import com.motorola.contextual.smartrules.rulesbuilder.Blocks.EditType;
import com.motorola.contextual.smartrules.rulesbuilder.RulesBuilderConstants;
import com.motorola.contextual.smartrules.util.Util;


/** This implements the business rules around a single SmartRule (Pre)Condition. 
*
*<code><pre>
* CLASS:
* 	Implements the User interaction related business logic
*   Bridges between UI and ConditionTuple
*
* RESPONSIBILITIES:
* 	implement business-layer of the Condition.
*
* COLABORATORS:
* 	ConditionTuple - providing utilities to persist the Condition entity to DB
*
* USAGE:
* 	See each method.
*
*</pre></code>
*/
public class ConditionInteractionModel implements Constants, RulesBuilderConstants, DbSyntax {
	
	private static final String TAG = ConditionInteractionModel.class.getSimpleName();
	private Condition mCondition;
	String cSensorName;
	String mBlockId;
	String mBlockInstanceId;
	String mState;
	String mConditionName;
    String pubKey;
	/**Basic constructor
	 * 
	 */
	public ConditionInteractionModel() {
		super();
    }

	/**Constructor - Constructs Condition class from a tuple
	 * 
	 * @param tuple
	 */
	public ConditionInteractionModel(Condition condition) {
		setCondition(condition);
		mBlockId = EMPTY_STRING;
		mBlockInstanceId = EMPTY_STRING;
		mState = EditType.UNKNOWN;
		pubKey = condition.getPublisherKey();
	}

	/** Constructs Condition class from parameters*/
	public ConditionInteractionModel(long id, 
				     long ruleFkey,
				     int enabled,
				     int suggState,
			         String suggReason,
			         int condMet,
			         String publisherKey,
			         int modality,
			         String sensorName,
			         String activityIntent,
			         String targetState,
			         String description,
			         String stateSyntax,
			         long createdDateTime,
			         long lastFailDateTime,
			         String condFailMsg, 
			         String icon,
			         String blockId,
			         String blockInstanceId,
			         String state,
			         String conditionName,
			         String cSensorName,
			         String config,
			         String validity,
			         String marketUrl) {
			//instantiate the ConditionTuple
			ConditionTuple condTuple = new ConditionTuple(	  id, 
													  ruleFkey, 
													  enabled, 
													  suggState,
													  suggReason, 
													  condMet,
													  publisherKey,
													  modality,
													  sensorName, 
													  activityIntent,
													  targetState,
													  description,
												      stateSyntax, 
												      createdDateTime,
												      lastFailDateTime, 
												      condFailMsg, 
												      icon,
												      config,
												      validity,
												      marketUrl)	;
			 setCondition(	new Condition(condTuple));
			 
			 setBlockId(blockId);
			 setBlockInstanceId(blockInstanceId);
			 setState(state);
			 setConditionName(conditionName);
			 setCSensorName(cSensorName);
			 setPubKey(publisherKey);
	}

	/**getter - CSensor Name. This is needed for logging in the Debug table
	 * 
	 * @return cSensor Name
	 */
	public String getCSensorName(){
		return cSensorName;
	}
	
	/** setter - CSensor Name.
	 * 
	 * @param cSensorName
	 */
	public void setCSensorName(String cSensorName){
		this.cSensorName = cSensorName;
	}


    /** getter - Block ID
     * 
     * @return block Id
     */
    public String getBlockId() {
        return mBlockId;
    }
    
    /** setter - Block ID
     * 
     * @param blockId
     */
    public void setBlockId(String blockId) {
        this.mBlockId = blockId;
    }
    
    /** getter - Block Instance ID
     * 
     * @return
     */
    public String getBlockInstanceId() {
        return mBlockInstanceId;
    }
    
    /** setter - Block Instance ID
     * 
     * @param blockInstanceId
     */
    public void setBlockInstanceId(String blockInstanceId) {
        this.mBlockInstanceId = blockInstanceId;
    }
    
    /** getter - State - Added/Edited/Deleted
     * 
     * @return
     */
    public String getState() {
        return mState;
    }
	    
    /** setter - State
     * 
     * @param state
     */
    public void setState(String state) {
        this.mState = state;
    }
    
    /** getter - conditionName
     * 
     * @return
     */
    public String getConditionName() {
        return mConditionName;
    }
	    
    /** setter - condition Name
     * 
     * @param conditionName
     */
    public void setConditionName(String conditionName) {
        this.mConditionName = conditionName;
    }
    
	public String getPubKey() {
		return pubKey;
	}

	public void setPubKey(String pubKey) {
		this.pubKey = pubKey;
	}
    
	/**
	 * This class gives a ArrayList collection of all the Conditions represented
	 * in a Model format.
	 *
	 */
    public static class ConditionInteractionModelList extends ArrayList<ConditionInteractionModel>{
    	private static final long serialVersionUID = 1L;

  	  /**If there is at least one connected condition present, then return true.
	    *
	    * @param conditions
	    * @return true if there is at least one connected condition present.
	    */
		public boolean isConnectedConditionPresent(){
    		boolean isPresent = false;
			if (this.size() != 0){
			    for ( int i = 0; i < this.size(); i++ ) {
			        if ((this.get(i).getCondition().getEnabled() == ConditionTable.Enabled.ENABLED) 
			        	&& (!this.get(i).getState().equals(EditType.DELETED))
			        	&& (!this.get(i).getCondition().getValidity().equals(TableBase.Validity.BLACKLISTED))
		            	&& ((!this.get(i).getCondition().getValidity().equals(TableBase.Validity.UNAVAILABLE)) 
		            	     || (!Util.isNull(this.get(i).getCondition().getMarketUrl())))){
			            isPresent = true;
			            break;
			        }
			    }
			}
    	    return isPresent;
    	}
		
	  	  /**If there is at least one visible condition present, then return true.
		    *
		    * @param conditions
		    * @return true if there is at least one visible condition present.
		    */
			public boolean isVisibleConditionPresent(){
	    		boolean isPresent = false;
				if (this.size() != 0){
				    for ( int i = 0; i < this.size(); i++ ) {
				        if ((!this.get(i).getState().equals(EditType.DELETED))
				        	&& (!this.get(i).getCondition().getValidity().equals(TableBase.Validity.BLACKLISTED))
			            	&& ((!this.get(i).getCondition().getValidity().equals(TableBase.Validity.UNAVAILABLE)) 
			            	     || (!Util.isNull(this.get(i).getCondition().getMarketUrl())))){
				            isPresent = true;
				            break;
				        }
				    }
				}
	    	    return isPresent;
	    	}
		
		/** Gets the first condition Name in the list. This will be used for the rule name in case the user 
		 * does not have a rule name
		 * 
		 * @return
		 */
		 public String getFirstConditionName(Context context){
			String firstConditionName = null;
	    	if (this.size() != 0){
	    		for (int i = 0; i < this.size(); i++){
	    			if (!this.get(i).getState().equals(EditType.DELETED)){
	    				firstConditionName = this.get(i).getConditionName();
	    				break;
	    			}
	    		}
	    	}
	    	if (firstConditionName == null) firstConditionName = context.getString(R.string.manual_rule);
	    	return firstConditionName;
	    }
	 
		/** Checks if there is any unconfigured trigger present. The trigger needs to be a connected trigger
		 * for it to be unconfigured. If it is not connected, it means it is an optional trigger and optional
		 * triggers can be unconfigured.
		 * 
		 * @return True if unconfigured condition present
		 */
		 public boolean isUnconfiguredConditionPresent() {
				boolean isPresent = false;
				if (this.size() != 0){
					for ( int i = 0; i < this.size(); i++ ) {
						if (!this.get(i).getState().equals(EditType.DELETED) && 
							(this.get(i).getCondition().getEnabled() == ConditionTable.Enabled.ENABLED) &&
							Util.isNull(this.get(i).getCondition().getConfig())
							&& (!this.get(i).getCondition().getValidity().equals(TableBase.Validity.BLACKLISTED))
					        && ((!this.get(i).getCondition().getValidity().equals(TableBase.Validity.UNAVAILABLE)) 
					            || (!Util.isNull(this.get(i).getCondition().getMarketUrl())))){
							isPresent = true;
							break;
						}
					}
				}
				return isPresent;
		   }
		
	    public boolean isParticularConditionPresent(String publisherKey){
		    	
	    	boolean isPresent = false;
			if (this.size() != 0){
				for ( int i = 0; i < this.size(); i++ ) {
					if (!this.get(i).getState().equals(EditType.DELETED) &&
					    this.get(i).getCondition().getPublisherKey().equals(publisherKey)
						&& (!this.get(i).getCondition().getValidity().equals(TableBase.Validity.BLACKLISTED))
						&& ((!this.get(i).getCondition().getValidity().equals(TableBase.Validity.UNAVAILABLE)) 
						    || (!Util.isNull(this.get(i).getCondition().getMarketUrl())))){
							isPresent = true;
							break;
					}
				}
			}
			return isPresent;
		    	
		 }
      }

	    /** will notify the condition publisher either to start or stop listening to the
	     *  trigger sensor state changes.
	     * 
	     * @param context - context
	     * @param condition - Condition to process
	     * @param ruleFlags - rule flags from the rule table
	     */
	    public static void notifyConditionPublisher(final Context context, final ConditionInteractionModel condition,
						final String ruleFlags) {

		if(LOG_DEBUG) Log.d(TAG, "Handling for condition "+condition.toString()+" ruleFlags = "+ruleFlags);

		String editType = condition.getState();
		boolean isEnabled = condition.getCondition().getEnabled() == Enabled.ENABLED ? true : false;

		if(!isEnabled && editType.equals(EditType.UNKNOWN)) {
			// DO NOTHING. This is the case where the trigger is not enabled
			// and the trigger is in unknown state (for suggestions and sample
			// rule).
		}
		else {
			// valid case so process for the trigger.
			String pubKey = condition.getCondition().getPublisherKey();
			String publish = null;

			Cursor cursor = ConditionPersistence.getTriggerStateViewCursor(context, Q + pubKey + Q);

			if(cursor != null) {
				try {
					if(cursor.moveToFirst()) {
						int count = cursor.getInt(cursor.getColumnIndex(TriggerStateCountView.Columns.TRIGGER_COUNT_BY_STATUS));
						if(count == 1) {
							// means there is exactly one count of this trigger in the database.
							if(isEnabled) {
								// The condition is still connected and enabled in the rule
								// or marked for deletion.
								if(editType.equals(EditType.UNKNOWN) && ! Util.isNull(ruleFlags)) {
									// the edit type is set to unknown for triggers that are not edited
									// or for triggers that come for sample and suggested rules.
									// So need to check if this is a sample rule or suggested rule (i.e.
									// check if the ruleFlags is also not null as this field
									// for sample and suggested rule is is set to 's'
									publish = ConditionPersistence.START;
								}
								else if(editType.equals(EditType.DELETED)) {
									// means the trigger is deleted and hence send a stop to
									// the trigger publisher
									publish = ConditionPersistence.STOP;
								}
							} else {
								// This means the trigger was disconnected in the edit of the rule. So
								// a count of 1 means this is the only rule controlling the trigger
								// so send a stop to the trigger publisher.
								if(LOG_DEBUG) Log.d(TAG, "condition is disabled and count is 1 so send STOP");
									publish = ConditionPersistence.STOP;
							}
						}
					} else {
						// Move to first failed, it means there are no other rules that control this
						// trigger. If the trigger was either edited (disconnected to connected state)
						// or newly added to the rule or unknown (for sample/suggested rules).
						// Send a start if the trigger is enabled else it means the trigger was
						// disconnected so send a stop to the trigger publisher accordingly.
						if(LOG_DEBUG) Log.d(TAG, "cursor.movetofirst failed for "+pubKey);
						if(! editType.equals(EditType.DELETED)) {
							if(isEnabled)
								publish = ConditionPersistence.START;
							else
								publish = ConditionPersistence.STOP;
			    			}
					}
				} catch (Exception e) {
					Log.e(TAG, "Exception in cursor for "+pubKey);
					e.printStackTrace();
				} finally {
					if(!cursor.isClosed())
						cursor.close();
		    		}
			}

			if(publish != null) {
				if(LOG_DEBUG) Log.d(TAG, "sending "+publish+" to "+pubKey);
				ConditionPersistence.sendBroadcastToPublisher(context, pubKey, publish, new Date().getTime());
			}
	    	}
	    }

	    
	public ConditionTuple getCondition() {
		return mCondition;
	}

	private void setCondition(Condition cond) {
		mCondition = cond;
	}
	

 }
