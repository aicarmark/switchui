/*
 * @(#)Action.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2011/05/09 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.business;

import java.net.URISyntaxException;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ActionTuple;
import com.motorola.contextual.smartrules.db.table.ModalTable;
import com.motorola.contextual.smartrules.uipublisher.ActionPublisher;

/** This implements the business rules around a single SmartRule Action. 
*
*<code><pre>
* CLASS:
* 	Extends ActionTuple which implements the basic methods of storing and retrieving a action 
*              (without the business logic) 
*
* RESPONSIBILITIES:
* 	implement business-layer of the Action.
*
* COLABORATORS:
* 	Rule - this class is a child class to Rule, which implements the business layer of Rules.
*
* USAGE:
* 	See each method.
*
*</pre></code>
*/
public class Action extends ActionTuple implements Parcelable{

    private static final String TAG = Action.class.getSimpleName();   
    public static final String EXTRA_ACTION_RULE_KEY = "com.motorola.intent.extra.RULE_KEY";
    private Rule mChildRule = null;

	/** basic constructor
	 */
	public Action() {
		super();
	}
	
	 /** constructor for parcel re-inflate.
    *
    * @param parcel to reconstruct the instance
    */
	public Action(Parcel parcel) {	
       super(parcel);
       mChildRule = new Rule();
       mChildRule = (Rule) parcel.readParcelable(Rule.class.getClassLoader());
	}
	
	/** Constructor based on a ActionPublisher. The caller needs to set the URI to fire the
	 *  action once the Action object is returned.
	 * 
	 * @param actionPublisher - Condition Publisher
	 */
	public Action(ActionPublisher actionPublisher) {
		super();
		this.setActivityIntent(actionPublisher.getActivityPkgUri());
		this.setPublisherKey(actionPublisher.getPublisherKey());
		this.setDescription(actionPublisher.getBlockDescription());
		this.setStateMachineName(actionPublisher.getBlockName());
		this.setModality(ModalTable.convertPkgMgrModalityType(actionPublisher.getStateType()));			
	}


	/** Tuple Constructor
	 * 
	 * @param t - Action Tuple
	 */
	public Action(ActionTuple t) {
		super(t);
	}
	
    /** creator for parceling */
    public static final Parcelable.Creator<Action> CREATOR =
    new Parcelable.Creator<Action>() {

        public Action createFromParcel(Parcel in) {
            return new Action(in);
        }

        public Action [] newArray(int size) {
            return new Action[size];
        }
    };

	@Override
	public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    	dest.writeParcelable(mChildRule, flags);
    }
	
    /** resets the persistent fields for the action.
     */
    public void resetPersistentFields(String parentRuleKey) {
        this.set_id(-1);
        this.clearLastFiredDateTime();
        this.setParentFk(-1);
        this.setActFailMsg(null);
        // This function is called when a rule is copied, if the original rule had action blocks
        // with Conflict loser, the copied rule also shows incorrect Conflict status in the action
        // blocks. Hence clear the Conflict winner field.
        this.setConfWinner(ActionTable.ConflictWinner.WINNER);
        if(this.mChildRule != null) {
            String newChildRuleKey = RulePersistence.createClonedRuleKeyForSample(this.mChildRule.getKey());
            this.setChildRuleKey(newChildRuleKey);
            this.mChildRule.resetChildRulePersistenceFields(newChildRuleKey, parentRuleKey);
            String config = this.getConfig();
            if(config != null) {
                    Intent intent;
                    try {
                            intent = Intent.parseUri(config, 0);
                            intent.putExtra(EXTRA_ACTION_RULE_KEY, newChildRuleKey);
                            this.setConfig(intent.toUri(0));
                    } catch (URISyntaxException e) {
                            e.printStackTrace();
                    }
            }
        }
    }

	
	/** getter - child rule associated with this action
	 * 
	 * @return - null or a valid Rule element
	 */
	public Rule getChildRule() {
		return mChildRule;
	}

	/** setter - child rule associated with this action
	 *
	 * @param mChildRule - child rule
	 */
	public void setChildRule(Rule mChildRule) {
		this.mChildRule = mChildRule;
	}

    /** Sends the broadcast intent to Quick actions for the action on hand
    *
    * @param context - context
    * @param actionPublisher - action publisher key to which intent has to be broadcasted
    * @param ruleKey - ruleKey of the rule that is currently processed
    * @param modeName - modeName of the rule that is currently processed
    * @param saveDefault - true to indicate save default
    * @param actionName - name of the action being fired (GPS, Wi-Fi etc..)
    * @param command - either fire or revert
    * @param config - target value of the action to set (On/Off etc...)
    */
    public static void sendBroadcastIntentForAction(final Context context, final String actionPublisher,
            final String ruleKey, final String modeName,
            final boolean saveDefault, final String actionName,
            final String command, final String config, final long actionId) {

            Intent broadcastActionIntent = new Intent(actionPublisher);
            broadcastActionIntent.putExtra(EXTRA_SAVE_DEFAULT, saveDefault);
            broadcastActionIntent.putExtra(EXTRA_CONFIG, config);
            broadcastActionIntent.putExtra(EXTRA_COMMAND, command);
            // Send both ruleKey and actionId as ruleKey is needed by Action Publisher to log into
            // Analytics Provider. actionId is sent in request_id, since this is necessary to
            // map the fire/revert response to the correct action Id in case where there are
            // more than one action with same publisher key for a particular rule.
            broadcastActionIntent.putExtra(EXTRA_REQUEST_ID, ruleKey + COLON + actionId);
            broadcastActionIntent.putExtra(EXTRA_VERSION, ACTION_PUBLISHER_VERSION);
            broadcastActionIntent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            if(LOG_INFO) Log.i(TAG, "broadcastActionIntent = "+broadcastActionIntent.toUri(0));
            context.sendBroadcast(broadcastActionIntent, PERM_ACTION_PUBLISHER);

            String Direction = DebugTable.Direction.OUT;
            if (ruleKey!= null && ruleKey.equals(DEFAULT_RULE_KEY))
                Direction = DebugTable.Direction.INTERNAL;

            DebugTable.writeToDebugViewer(context, Direction, null, modeName, ruleKey,
                                    TO_QUICKACTIONS_OUT_DBG_MSG, actionName, command,
                                            Constants.PACKAGE, Constants.PACKAGE);

    }
}
