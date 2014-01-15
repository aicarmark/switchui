/*
 * @(#)Rule.java
 *
 * (c) COPYRIGHT 2009 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2011/05/09 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.business;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.table.RuleTable.Active;
import com.motorola.contextual.smartrules.db.table.RuleTable.Columns;
import com.motorola.contextual.smartrules.db.table.RuleTable.Flags;
import com.motorola.contextual.smartrules.db.table.RuleTable.Source;
import com.motorola.contextual.smartrules.db.table.RuleTable.SuggState;
import com.motorola.contextual.smartrules.db.table.RuleTuple;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.rulesbuilder.EditRuleActivity;
import com.motorola.contextual.smartrules.service.SmartRulesService;


/** This implements the business rules around the SmartRules Rule as 
* well as the corresponding Actions and Conditions for 1 rule. This class holds the list of
* related conditions and actions. 
*
*<code><pre>
* CLASS:
* 	Extends RuleTuple which implements the basic methods of storing and retrieving a rule (no business logic) 
*
* RESPONSIBILITIES:
* 	implement business-layer of the Rule.
*
* COLABORATORS:
* 	RulePersistence - knows how to persist an instance of this class
* 	Action - implements the business layer of the individual Actions.
* 	Condition - implements the business layer of the individual Conditions.
*
* USAGE:
* 	See each method.
*
*</pre></code>
*/
public class Rule extends RuleTuple implements Parcelable, Constants {

	private static final String TAG = Rule.class.getSimpleName();
	
	private boolean cachedErrorFlag;
	private boolean cachedSuggestedActionFlag;
	private boolean cachedLocationBlockFlag;
	private int cachedAdoptedCount;
	
	private ActionList<Action> mActionList;
	private ConditionList<Condition> mConditionList;
	private byte[] mRuleIconBlob = null;
	private long mRuleIconId ;

	
	/** basic constructor */
	public Rule() {
		super();
		this.setValidity(TableBase.Validity.VALID);
	}
	
	/** parcel constructor */
	public Rule(Parcel parcel) {
		super(parcel);
		mActionList = new ActionList<Action>();
		mConditionList = new ConditionList<Condition>();
		parcel.readTypedList(mActionList, Action.CREATOR);
		parcel.readTypedList(mConditionList, Condition.CREATOR);
	}
	
	/** constructor from a rule tuple */
	public Rule(RuleTuple tuple) {
		super(tuple);
		mRuleIconId = this._id;
	}

	/** constructor from a rule tuple, action list and condition list 
	 * 
	 * @param tuple - RuleTuple, can be obtained via new RuleTupe(field list); 
	 * @param actionList - list of actions for this rule
	 * @param conditionList - list of conditions for this rule
	 */
	public Rule(RuleTuple tuple, ActionList<Action> actionList, ConditionList<Condition> conditionList) {
		super(tuple);
		mRuleIconId = this._id;
		if(actionList == null)
			this.mActionList = new ActionList<Action>();
		else
			this.mActionList = actionList;
		if(conditionList == null)
			this.mConditionList = new ConditionList<Condition>();
		else
			this.mConditionList = conditionList;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {

        super.writeToParcel(dest, flags);
        dest.writeTypedList(mActionList);
        dest.writeTypedList(mConditionList);
    }
	
	/** creator for parceling */
    public static final Parcelable.Creator<Rule> CREATOR =
    new Parcelable.Creator<Rule>() {

        public Rule createFromParcel(Parcel in) {
            return new Rule(in);
        }

        public Rule [] newArray(int size) {
            return new Rule[size];
        }
    };

	/** @see toString()
	 */
	@Override
	public String toString() {
		String ruleString = null;
		if( (this.getConditionList() !=null) && (this.getActionList() !=null) ){
			ruleString = super.toString().concat(this.getConditionList().toString()).concat(this.getActionList().toString());
		}
		return ruleString;
	} 
	
	/** getter - action list
	 * 
	 * @return the actionList
	 */
	public ActionList<Action> getActionList() {
		return mActionList;
	}

	/** setter - action list
	 * 
	 * @param actionList the actionList to set
	 */
	public void setActionList(ActionList<Action> actionList) {
		mActionList = actionList;
	}

	/** getter - condition list
	 * 
	 * @return the conditionList
	 */
	public ConditionList<Condition> getConditionList() {
		return mConditionList;
	}
	 
	/** setter - condition list
	 * 
	 * @param conditionList the conditionList to set
	 */
	public void setConditionList(ConditionList<Condition> conditionList) {
		mConditionList = conditionList;
	}
	
	/** getter - error in the rule due to error in one of the action or conditions.
	 * 
	 * @return the cachedErrorFlag
	 */
	public boolean isError() {
		return cachedErrorFlag;
	}

	/** setter - cachedErrorFlag
	 * 
	 * @param cachedErrorFlag the cachedErrorFlag to set
	 */
	protected void setErrorFlag(boolean cachedErrorFlag) {
		this.cachedErrorFlag = cachedErrorFlag;
	}

	/** getter - derived flag to indicate when a rule has a suggested action
	 * 
	 * @return the cachedSuggestedActionFlag
	 */
	public boolean isSuggestedActionOrCondition() {
		return cachedSuggestedActionFlag;
	}

	/** setter - cachedSuggestedActionFlag
	 * 
	 * @param cachedSuggestedActionFlag the cachedSuggestedActionFlag to set
	 */
	protected void setHasSuggestionFlag(boolean cachedSuggestedActionFlag) {
		this.cachedSuggestedActionFlag = cachedSuggestedActionFlag;
	}

	/** getter - derived flag to indicate when a rule has location block as a trigger.
	 * 
	 * @return the cachedLocationBlockFlag
	 */
	public boolean isLocationBlockPresent() {
		return cachedLocationBlockFlag;
	}

	/** setter - cachedLocationBlockFlag
	 * 
	 * @param cachedLocationBlockFlag the cachedLocationBlockFlag to set
	 */
	protected void setHasLocationBlockFlag(boolean cachedLocationBlockFlag) {
		this.cachedLocationBlockFlag = cachedLocationBlockFlag;
	}

	/** getter - this is the number of times this rule has been adopted by the user
	 * 	from a sample rule (> 0 means adopted)
	 * 
	 * @return the cachedAdoptedCount value
	 */
	public int getCachedAdoptedCount() {
		return cachedAdoptedCount;
	}

	/** setter - this is the number of times this rule has been adopted by the user
	 * 	from a sample rule (> 0 means adopted)
	 * 
	 * @param cachedAdoptedCount the cachedAdoptedCount to set
	 */
	protected void setCachedAdoptedCount(int cachedAddedCount) {
		this.cachedAdoptedCount = cachedAddedCount;
	}

	/** deletes the rule from the DB - if the rule is active then the rule is 
	 * 	deactivated before deleting from the DB.
	 * 
	 * @param context - context
	 * @return - true if we deleted only one rule successfully else false
	 */
	public boolean delete(Context context) {
		int rowsDeleted = 0;
		// Check of the rule is active before proceeding - use case as
		// in IKINTNETAPP-400
		boolean isActive = RulePersistence.isRuleActive(context, this.get_id());
		if(isActive) {
        	if(LOG_DEBUG) Log.d(TAG, "Process the rule state change " +
        			"of active rule before deleting");
			this.toggleRuleState(context, true);
			rowsDeleted = 1;
		} else {
        	if(LOG_DEBUG) Log.d(TAG, "Deleting non-active rule "+this.get_id());
			rowsDeleted = RulePersistence.deleteRule(context, this.get_id(), 
				this.getName(), this.getKey(), true);
		}
		return (rowsDeleted == 1);
	}

	/** adds/inserts the rule as a new rule into the DB.
	 * 
	 * @param context - context
	 * @return - inserted rule ID or -1 if insert fails
	 */
	public long insert(Context context) {
		long id = DEFAULT_RULE_ID;
		if(this.get_id() == DEFAULT_RULE_ID) {
			id = new RulePersistence().insert(context, this);		
		} else {
			Log.w(TAG, "The rule "+this.get_id()+" is already in the DB use " +
					"rule._id == DEFAULT_RULE_ID to call insert to DB");
		}	
		return id;
	}
	
	/** persists the rule into the DB. Handles add/deletion/update of the rule, actions
	 *  and conditions.
	 * 
	 * @param context - context
	 * @return - true if the update was successful else false.
	 */
	public boolean update(Context context) {
		return new RulePersistence().updateRule(context, this);
	}

	/** returns the clone of this rule instance (does not persist to DB).
	 * 
	 * @param context - context
	 * @return - clone of the rule as a Rule instance. Need to call rule.insert()
	 * 				to persist the cloned rule to DB.
	 */
	public Rule clone(Context context) {
		return RulePersistence.fetchFullRule(context, this._id);		
	}

	/** handles the rule state change request by calling the appropriate service.
	 * 
	 * @param context - context
	 */
	public void toggleRuleState(Context context) {
		this.toggleRuleState(context, false);
	}
	
	/** handles the rule state change request by calling the appropriate service.
	 * 
	 * @param context - context
	 * @param deleteRule - delete the rule from DB if set to true
	 */
	public void toggleRuleState(Context context, boolean deleteRule) {
		boolean isActive = this.getActive() == Active.ACTIVE;
		Intent serviceIntent = new Intent();
		if(deleteRule) {
			// Case when an active rule was being marked for deletion.
			// So handle the rule inactivation before deleting it.
			serviceIntent.setClass(context, SmartRulesService.class);
			serviceIntent.putExtra(MM_RULE_KEY, this.getKey());
			serviceIntent.putExtra(MM_RULE_STATUS, FALSE);
			serviceIntent.putExtra(MM_DELETE_RULE, true);
		}
		else if((this.isEnabled() && isActive)
				|| (!this.isAutomatic()))  {
			// Case when the rule was activated (manual) or deactivated (auto or manual)
			if(LOG_DEBUG) Log.d(TAG, "Start SmartRulesService to process the rule state change");
			serviceIntent.setClass(context, SmartRulesService.class);
			serviceIntent.putExtra(MM_RULE_KEY, this.getKey());
			serviceIntent.putExtra(MM_RULE_STATUS, isActive ? FALSE : TRUE);
			serviceIntent.putExtra(MM_DISABLE_RULE, this.isAutomatic() ? true : false);
		} else if(this.isAutomatic()) {
			// Case when an automatic rule was moving from Ready to Disabled
			// or Disabled to Ready states.
			if(LOG_DEBUG) Log.d(TAG, "Start SmartRulesService to process the non-active rule state change");
			Intent ruleStateIntent = new Intent(context, SmartRulesService.class);
			ruleStateIntent.putExtra(Constants.MM_RULE_KEY, this.getKey());
			ruleStateIntent.putExtra(Constants.MM_RULE_STATUS, Constants.FALSE);
			if (this.isEnabled()) {
				if(LOG_DEBUG) Log.d(TAG, "Disabling ready rule");
				ruleStateIntent.putExtra(Constants.MM_DISABLE_RULE, true);
			} else {
				if(LOG_DEBUG) Log.d(TAG, "Enabling automatic rule");
				ruleStateIntent.putExtra(Constants.MM_ENABLE_RULE, true);
			}
		}
		Log.d(TAG, "key is "+this.getKey());
		serviceIntent.putExtra(Columns._ID, this.get_id());
		context.startService(serviceIntent);		
	}

	/** constructs the intent to launch the rules builder
	 * 
	 * @param context - context
	 * @return intent to launch the rules builder
	 */
	public Intent fetchRulesBuilderIntent(Context context, boolean isCopy) {
		Intent intent = new Intent(context, EditRuleActivity.class);
		Bundle intentBundle = new Bundle();
		intentBundle.putLong(PUZZLE_BUILDER_RULE_ID, this.get_id());
		intentBundle.putBoolean(PUZZLE_BUILDER_RULE_COPY, isCopy);		
        intentBundle.putParcelable(PUZZLE_BUILDER_RULE_INSTANCE, this);
		intent.putExtras(intentBundle);		
		return intent;
	}

	/** resets the fields in the child rule and for corresponding actions and conditions.
	 *  Does not persist to the DB.
	 * 
	 * @param ruleKey - the rule key that needs to be set
	 * @param parentRuleKey - parent rule key
	 */
	public void resetChildRulePersistenceFields(String ruleKey, String parentRuleKey) {
		this.setKey(ruleKey);
		this.set_id(-1);
		this.setTags(ruleKey);
		this.setActive(Active.INACTIVE);
		this.setParentRuleKey(parentRuleKey);

		this.setCreatedDateTime(0);
		this.setLastEditedDateTime(0);
		this.setLastActiveDateTime(0);
		this.setLastInactiveDateTime(0);
		this.setFlags(Flags.INVISIBLE);
		
		if(this.getActionList() != null)
			this.getActionList().resetPersistentFields(null);
		
		if(this.getConditionList() != null)
			this.getConditionList().resetPresistentFields();
		
	}
	
	/** resets the fields in the Rule, and corresponding Actions and Conditions (does not
	 * 	persist to the DB - mainly used for cloning records)
	 * 
	 * @param ruleKey - the rule key that needs to be set
	 * @param ruleName - the rule name that needs to be set
	 * @param sampleFKey - rule Id that needs to be set for the SampleFkOrCount column
	 * @param isRuleCopy - was this clone for a "Copy from existing rule" case
	 */
	public void resetPersistentFields(String ruleKey, String ruleName, 
										long sampleFKey, boolean isRuleCopy) {
		
		String parentRuleKey = this.getKey();
		this.mRuleIconId = this.get_id();
		this.set_id(-1);
		this.setName(ruleName);
		this.setKey(ruleKey);
		this.setTags(ruleKey);
		this.setActive(Active.INACTIVE);
		this.setAdoptCount(0);
		this.setSampleFkOrCount(-1);
		
		if(this.getSource() == Source.FACTORY && !isRuleCopy) {
			this.setParentRuleKey(parentRuleKey);
		}
		else {
			this.setSource(Source.USER);
			this.setParentRuleKey(null);
		}
		
		this.setSuggState(SuggState.ACCEPTED);
		this.setFlags("");

		this.setCreatedDateTime(0);
		this.setLastEditedDateTime(0);
		this.setLastActiveDateTime(0);
		this.setLastInactiveDateTime(0);
		
		if(this.getActionList() != null)
			this.getActionList().resetPersistentFields(ruleKey);
		
		if(this.getConditionList() != null)
			this.getConditionList().resetPresistentFields();
	}	
	
	/** returns a Rule instance for the current row of the Cursor.
	 * 
	 * @param context - context
	 * @param cursor - list of rules cursor
	 * @return - Rule instance
	 */
	protected static Rule parseIntoRule(Context context, Cursor cursor) {
		Rule rule = new Rule();
		long _id = cursor.getLong(cursor.getColumnIndex(Columns._ID));
		rule.mRuleIconId = _id;
		rule.set_id(_id);
		rule.setKey(cursor.getString(cursor.getColumnIndex(Columns.KEY)));
		rule.setEnabled(cursor.getInt(cursor.getColumnIndex(Columns.ENABLED)));
		rule.setActive(cursor.getInt(cursor.getColumnIndex(Columns.ACTIVE)));
		rule.setRuleType(cursor.getInt(cursor.getColumnIndex(Columns.RULE_TYPE)));
		rule.setSource(cursor.getInt(cursor.getColumnIndex(Columns.SOURCE)));
		rule.setName(cursor.getString(cursor.getColumnIndex(Columns.NAME)));
		rule.setDesc(cursor.getString(cursor.getColumnIndex(Columns.DESC)));
		rule.setSuggState(cursor.getInt(cursor.getColumnIndex(Columns.SUGGESTED_STATE)));
		rule.setIcon(cursor.getString(cursor.getColumnIndex(Columns.ICON)));
		rule.setAdoptCount(cursor.getLong(cursor.getColumnIndex(Columns.ADOPT_COUNT)));
		rule.setValidity(cursor.getString(cursor.getColumnIndex(Columns.VALIDITY)));
		rule.setPublisherKey(cursor.getString(cursor.getColumnIndex(Columns.PUBLISHER_KEY)));
		
		rule.setIconBlob(IconPersistence.getIconBlob(context, rule.getRuleIconId(),
				rule.getPublisherKey(), rule.getIcon()));
		try {
			int failCount = 
					cursor.getInt(cursor.getColumnIndexOrThrow(RulePersistence.FAIL_COUNT));
			rule.setErrorFlag(failCount > 0 ? true : false);
		} catch (Exception e) {
			if(LOG_DEBUG) Log.d(TAG, "FAIL_COUNT column is not present in the cursor");
			rule.setErrorFlag(false);
		}
		
		try {
			int suggestionCount = 
					cursor.getInt(cursor.getColumnIndexOrThrow(RulePersistence.SUGGESTION_COUNT));
			rule.setHasSuggestionFlag(suggestionCount > 0 ? true : false);
		} catch (Exception e) {
			if(LOG_DEBUG) Log.d(TAG, "SUGGESTION_COUNT column is not present in the cursor");
			rule.setHasSuggestionFlag(false);
		}
		
		try {
			int locationBlockCount = 
					cursor.getInt(cursor.getColumnIndexOrThrow(RulePersistence.LOCATION_BLOCK_COUNT));
			rule.setHasLocationBlockFlag(locationBlockCount > 0 ? true : false);
		} catch(Exception e) {
			if(LOG_DEBUG) Log.d(TAG, "LOCATION_BLOCK_COUNT column is not present in the cursor");
			rule.setHasLocationBlockFlag(false);
		}

		try {
			int addedCount = 
					cursor.getInt(cursor.getColumnIndexOrThrow(RulePersistence.SAMPLE_RULE_ADOPTED_COUNT));
			rule.setCachedAdoptedCount(addedCount);
		} catch (Exception e) {
			if(LOG_DEBUG) Log.d(TAG, "LOCATION_BLOCK_COUNT column is not present in the cursor");
			rule.setCachedAdoptedCount(0);
		}
		
		rule.setActionList(new ActionPersistence().fetch(context, _id));
		rule.setConditionList(new ConditionPersistence().fetch(context, _id));
		
		return rule;
	}	
	
	/** takes a cursor and constructs a List of Rule instances.
	 * 
	 * @param context - context
	 * @param cursor - cursor
	 * @return list of Rule instances
	 */
	public static List<Rule> convertToRuleList(Context context, Cursor cursor) {
		ArrayList<Rule> list = null;
		if(cursor == null) {
			Log.e(TAG, "Cursor passed is null");
		} else {
			try {
				if(cursor.moveToFirst()) {
					list = new ArrayList<Rule>();
					for(int i = 0; i < cursor.getCount(); i++) {
						Rule rule = Rule.parseIntoRule(context, cursor);
						cursor.moveToNext();
						list.add(rule);
					}			
				} else {
					Log.e(TAG, "cursor.moveToFirst failed");
				}
			} catch (Exception e){
				Log.e(TAG, "Exception while iterating through the cursor");
				e.printStackTrace();
			} finally {
				if(! cursor.isClosed())
					cursor.close();
			}	
		}		
		return list;
	}

	/**
	 * Sets the Icon drawable to rule
	 * @param iconBlob
	 */
	public void setIconBlob(byte[] iconBlob) {
		mRuleIconBlob = iconBlob;
	}

	/**
	 * Gets the Icon Drawable for the Rule
	 * @param context TODO
	 * @return Drawable containg the Icon
	 */
	public Drawable getIconDrawable(Context context) {
		return IconPersistence.getIconDrawableFromBlob(context, mRuleIconBlob);
	}

	/**
	 * Gets the RuleIconId for the rule
	 * @return RuleIconId
	 */
	public long getRuleIconId() {
		return mRuleIconId;
	}
}