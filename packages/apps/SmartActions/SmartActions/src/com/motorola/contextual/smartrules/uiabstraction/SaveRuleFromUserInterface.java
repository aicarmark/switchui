package com.motorola.contextual.smartrules.uiabstraction;

import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.app.LandingPageActivity.LandingPageIntentExtras;
import com.motorola.contextual.smartrules.db.business.ConditionPersistence;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.publishermanager.RulesValidatorInterface;
import com.motorola.contextual.smartrules.rulesbuilder.RulesBuilderConstants;
import com.motorola.contextual.smartrules.rulesbuilder.RulesBuilderUtils;
import com.motorola.contextual.smartrules.rulesimporter.XmlConstants.ImportType;
import com.motorola.contextual.smartrules.uiabstraction.ConditionInteractionModel.ConditionInteractionModelList;
import com.motorola.contextual.smartrules.util.Util;

/**
 * Helper class to be used by RuleController to facilitate Saving
 * of a Rule that has been edited/re-configured
 *
 * <code><pre>
 * CLASS:
 *  None.
 *
 * RESPONSIBILITIES:
 * Transforms Rule and Publisher Models back into persisted forms
 * that can be committed back to the DB. Also helps with the
 * Rule state transitions.
 *
 * COLABORATORS:
 * 	Context
 *  RuleInteractionModel
 *  Rule
 *  IEditRulePluginCallback
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class SaveRuleFromUserInterface implements Constants, RulesBuilderConstants{
	private static final String TAG = SaveRuleFromUserInterface.class.getSimpleName();
	private Context						mContext = null;
	private RuleInteractionModel       	mRuleModel = null;
	private IEditRuleGenericCallback	mEditActivityCallback;

	SaveRuleFromUserInterface(Context context, RuleInteractionModel ruleModel){
		mContext = context;
		mRuleModel = ruleModel;

	}

	/**
	 * Saves rules, has routines which take care of Models and
	 * how the RuleModel including Action/Condition models gets
	 * persisted to the DB
	 *
	 * @param cb
	 * @param isManual
	 */
	public void initSave(IEditRuleGenericCallback cb, boolean isManual){
		mEditActivityCallback = cb;

	    if (mRuleModel.getRuleInstance().get_id() == DEFAULT_RULE_ID)
			addNewRuleToDB();
	    else{
		 //If the rule is edited while Active then the rule will need to be disabled first.
		     if (mRuleModel.getEditWhileActiveFlag()){
			 if(isManual){
		    		 if(LOG_DEBUG) Log.d(TAG, "Manual Rule is active and was edited");
				 RulePersistence.toggleRuleStatus(mContext, mRuleModel.getRuleInstance().get_id());
				 mEditActivityCallback.onSaveEditWhileActive(true);
		    	 }else{
					if(LOG_DEBUG) Log.d(TAG, "Automatic rule is in active and in edit state");
					mEditActivityCallback.onSaveEditWhileActive(false);
					Intent callbackIntent= new Intent(SAVE_INTENT_CALLBACK);
				    RulePersistence.toggleRuleStatus(mContext, mRuleModel.getRuleInstance().get_id(), callbackIntent);
					    // As part of toggling Rule status to disabled and starting ConditionBuilder service
					    // we register to listen for the explicit Intent send to the ConditionBuilder
					    // We move forward with our routine of sending the EDIT_ACTIVE_RULE to self,
					    // only after listening to that intent back from ConditionBuilder

					    //Leaving this code commented, if in future we need to work on logic for skipping the
					    //ConditionBuilder if no condition were changed.
					//in this case (No conditions has been touched) we dont disable enable or call the ConditionBuilder
					//if(LOG_DEBUG) Log.d(TAG, "Automatic rule NO Conditions seemed to have changed");
					//mHandler.sendEmptyMessageDelayed(HandlerMessage.SKIP_COND_BUILDER, 2000);
			 }

		 }else{
						startThreadToeditExistingRuleFromDB(mRuleModel.getRuleInstance().get_id());
		  }
		}

	}


    /** This saves a new Rule (created from scratch) to the DB
    *
    */
    private void addNewRuleToDB()
    {
        Thread thread = new Thread() {
			public void run() {
				try {
					insertNewRuleToDB();
				} catch (Exception e) {
					Log.e(TAG, "Exception while inserting new rule");
					e.printStackTrace();
				}
			}
		};
		thread.setPriority(Thread.NORM_PRIORITY-1);
		thread.start();
    }

    /**
     * This Edits an existing Rule in the DB
     * @param ruleid
     */
     public void startThreadToeditExistingRuleFromDB(final long ruleid)
     {
         Thread thread = new Thread() {
			public void run() {
				try {
					editExistingRuleFromDB(ruleid);
				} catch (Exception e) {
					Log.e(TAG, "Exception while editing rule " + ruleid);
					e.printStackTrace();
				}
			}
		};
		thread.setPriority(Thread.NORM_PRIORITY-1);
		thread.start();
     }

    /** Inserts new Rule to DB. For this, some fields of the Rule Tuple needs to be updated with the latest from
     * the Rules Builder screen.
     */
    private void insertNewRuleToDB(){
       String createdDateAndTime = mRuleModel.getRuleInstance().getKey();

        if (LOG_DEBUG) Log.d(TAG,"Insert New Rule to DB");

	// Generate a new rule key only if the rule key is null or has not been passed in
        // which is only for blank rule use case.
        if(Util.isNull(mRuleModel.getRuleInstance().getKey()))
        mRuleModel.setUpdatedRuleKey();

        //Get the RuleVSensor string to be inserted into the Rule Table.
        ConditionInteractionModelList conditions = mRuleModel.getConditionList();

        mRuleModel.getRuleInstance().setEnabled(conditions.isConnectedConditionPresent()? RuleTable.Enabled.ENABLED: RuleTable.Enabled.DISABLED);
        mRuleModel.getRuleInstance().setRuleType(conditions.isConnectedConditionPresent()? RuleTable.RuleType.AUTOMATIC: RuleTable.RuleType.MANUAL);
        mRuleModel.getRuleInstance().setTags(mRuleModel.getRuleInstance().getKey());
        mRuleModel.getRuleInstance().setCreatedDateTime(Util.getLongValue(createdDateAndTime, new Date().getTime()));

        mRuleModel.insertNewRule(mContext);

        String ruleKey = mRuleModel.getRuleInstance().getKey();
        if (TableBase.Validity.INVALID.equals(mRuleModel.getRuleInstance().getValidity()))
            mRuleModel.getRuleInstance().setValidity(RulesValidatorInterface.updateRuleValidity(mContext, ruleKey));
            
        if (TableBase.Validity.VALID.equals(mRuleModel.getRuleInstance().getValidity()))
                RulesValidatorInterface.launchModeAd(mContext, ruleKey, 
                    ImportType.IGNORE, 
                    RuleTable.Validity.VALID, 
                    RulePersistence.fetchRuleOnly(mContext, ruleKey),
                    RulePersistence.isRulePsuedoManualOrManual(mContext, ruleKey));
    
        
 /*       // Launch RulesValidator
        Intent rvIntent =  new Intent(ACTION_RULES_VALIDATE_REQUEST);
        ArrayList<String> ruleList = new ArrayList<String>();
        ruleList.add(mRuleModel.getRuleInstance().getKey());
        rvIntent.putExtra(EXTRA_RULE_LIST, ruleList);
        rvIntent.putExtra(EXTRA_REQUEST_ID, String.valueOf(mRuleModel.getRuleInstance().get_id()));
        mContext.sendBroadcast(rvIntent);
*/
        Intent resultIntent = new Intent();
        resultIntent.putExtra(LandingPageIntentExtras.IS_RULE_MANUAL_RULE, mRuleModel.isManualRule());
        ((Activity) mContext).setResult(Activity.RESULT_OK, resultIntent);

        //Logging into debug viewer
        if (mRuleModel.getRuleInstance().getSource() == RuleTable.Source.USER )
        	RulesBuilderUtils.writeInfoToDebugTable(mContext, DebugTable.Direction.OUT,
        			SMARTRULES_INTERNAL_DBG_MSG, mRuleModel.getRuleInstance().getName(), mRuleModel.getRuleInstance().getKey(),
        			null, null, NEW_RULE_CREATED );

        // Adding a new rule to the DB. Call notifyConditionPublishers
        // to notify trigger publishers to start listening to sensor
        // state changes if required.
        if(mRuleModel.getRuleInstance().getRuleType() == RuleTable.RuleType.AUTOMATIC) {
            if(LOG_DEBUG) Log.d(TAG, "Rule "+mRuleModel.getRuleInstance().get_id()+" is automatic - notify publishers if required");
            ConditionPersistence.notifyConditionPublishers(mContext, mRuleModel.getRuleInstance().get_id(), true);
        }

        mEditActivityCallback.onSaveFinish();
    }

    /**This is called when an existing rule has been modified in the UI, and that needs to be updated in the DB.
    *
    * @param existingRuleId
    */
   private void editExistingRuleFromDB(long existingRuleId)
   {
       if(LOG_DEBUG) Log.d(TAG, "editExistingRuleFromDB rule_id: "+ existingRuleId);
	   mRuleModel.updateExistingRule(mContext);
       String ruleKey = mRuleModel.getRuleInstance().getKey();

	   if(!mRuleModel.getEditWhileActiveFlag()){

	         // Launch the Mode AD to cancel to old config+publisher key pair
	         Intent editIntent = new Intent(ACTION_LAUNCH_MODE_ACT_DEACTIVATOR);
	         editIntent.putExtra(EXTRA_RULE_KEY, ruleKey);
	         editIntent.putExtra(EXTRA_LAUNCH_REASON, RULE_DELETED);
	         editIntent.putExtra(EXTRA_PREV_CONFIG_PUBKEY_LIST, mRuleModel.getOldConfigPubKeyList());
	         mContext.sendBroadcast(editIntent);
	   }

	   //If this the Edit while Active use case, then the rule needs to be enabled first (because it was disabled earlier).
       // The toggleRuleStatus handler will take care of launching the Mode AD.
       // If this is a manual rule, we leave it to the user to enable it. So we do not call the toggleRuleStatus handler.
	   if(!mRuleModel.getQuickSaveOfActiveRuleFlag()){
	       if (mRuleModel.isManualRule() || !mRuleModel.getEditWhileActiveFlag()){
	           if (TableBase.Validity.INVALID.equals(mRuleModel.getRuleInstance().getValidity()))
	               mRuleModel.getRuleInstance().setValidity(RulesValidatorInterface.updateRuleValidity(mContext, ruleKey));
	               
	           if (TableBase.Validity.VALID.equals(mRuleModel.getRuleInstance().getValidity()))
	               RulesValidatorInterface.launchModeAd(mContext, ruleKey, 
    	                   ImportType.IGNORE, 
    	                   RuleTable.Validity.VALID, 
    	                   RulePersistence.fetchRuleOnly(mContext, ruleKey),
    	                   RulePersistence.isRulePsuedoManualOrManual(mContext, ruleKey));
	           
		           // Launch RulesValidator
		    /*       Intent rvIntent =  new Intent(ACTION_RULES_VALIDATE_REQUEST);
		           ArrayList<String> ruleList = new ArrayList<String>();
		           ruleList.add(ruleKey);
		           rvIntent.putExtra(EXTRA_RULE_LIST, ruleList);
		           rvIntent.putExtra(EXTRA_REQUEST_ID, String.valueOf(mRuleModel.getRuleInstance().get_id()));
		           mContext.sendBroadcast(rvIntent);*/
	       }
	       else {
			   // Calling the toggle function to enable the rule and launch Mode Ad
			   RulePersistence.toggleRuleStatus(mContext, mRuleModel.getRuleInstance().get_id());
	       }
	   }
       sendWidgetBroadcastMessage();

       Intent resultIntent = new Intent();
       resultIntent.putExtra(LandingPageIntentExtras.RULE_ID_INSERTED, mRuleModel.getRuleInstance().get_id()); //This ruleId is used by the Suggestions Module.
       resultIntent.putExtra(LandingPageIntentExtras.RULE_ICON_CHANGED, mRuleModel.getHasRuleIconChanged());
       resultIntent.putExtra(LandingPageIntentExtras.IS_RULE_MANUAL_RULE, mRuleModel.isManualRule());
       ((Activity) mContext).setResult(Activity.RESULT_OK, resultIntent);

       mEditActivityCallback.onSaveFinish();
		//Seeing issues when putting this in a Thread. Will need to discuss about this before making changes. Some of the issues are:
		//- In Edit while Active scenario, there is a lag in refreshing the Landing Page with the edited rule. It shows up as Disabled first
		//and then after some goes to the Ready state. Need to investigate further. Risky to put this change in now. So leaving it the
        //way it has been tested.
   }


   /** sends a broadcast intent to the widget to let it know that the rule has been modified.
    */
   private void sendWidgetBroadcastMessage() {
       int ruleType = RuleTable.RuleType.AUTOMATIC;
       int enabled = RuleTable.Enabled.ENABLED;
       if(mRuleModel.isManualRule()) {
	       ruleType = RuleTable.RuleType.MANUAL;
	       enabled = RuleTable.Enabled.DISABLED;
       } else if (! mRuleModel.getRuleInstance().isEnabled()) {
	       enabled = RuleTable.Enabled.DISABLED;
       }

		if(LOG_DEBUG) Log.d(TAG, "Sending intent to widget with details for "+mRuleModel.getRuleInstance().getKey());
		if(LOG_DEBUG) Log.d(TAG, "ruleType = "+ruleType+" enabled = "+enabled);

       Intent widgetUpdateIntent = new Intent(RULE_MODIFIED_MESSAGE);
       widgetUpdateIntent.putExtra(RuleTable.Columns.KEY, mRuleModel.getRuleInstance().getKey());
       widgetUpdateIntent.putExtra(RuleTable.Columns._ID, mRuleModel.getRuleInstance().get_id());
       widgetUpdateIntent.putExtra(RuleTable.Columns.ACTIVE, RuleTable.Active.INACTIVE);
       widgetUpdateIntent.putExtra(RuleTable.Columns.ENABLED, enabled);
       widgetUpdateIntent.putExtra(RuleTable.Columns.RULE_TYPE, ruleType);
       widgetUpdateIntent.putExtra(RuleTable.Columns.ICON, mRuleModel.getRuleInstance().getIcon());
       widgetUpdateIntent.putExtra(RuleTable.Columns.NAME, mRuleModel.getRuleInstance().getName());
       mContext.sendBroadcast(widgetUpdateIntent, SMART_RULES_PERMISSION);
   }


}
