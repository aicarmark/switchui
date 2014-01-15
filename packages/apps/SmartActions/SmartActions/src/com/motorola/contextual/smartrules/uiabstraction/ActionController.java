/*
 * @(#)ActionController.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * E51185        2011/05/06    NA              Initial version
 *
 */
package com.motorola.contextual.smartrules.uiabstraction;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ModalTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.rulesbuilder.Blocks;
import com.motorola.contextual.smartrules.rulesbuilder.EditRuleActivity.DialogId;
import com.motorola.contextual.smartrules.rulesbuilder.LocationConsent;
import com.motorola.contextual.smartrules.rulesbuilder.RulesBuilderConstants;
import com.motorola.contextual.smartrules.rulesbuilder.Blocks.EditType;
import com.motorola.contextual.smartrules.uipublisher.ActionPublisherList;
import com.motorola.contextual.smartrules.uipublisher.Publisher;
import com.motorola.contextual.smartrules.uipublisher.PublisherController;
import com.motorola.contextual.smartrules.util.Util;


/** 
* ActionController  bridges the Activity/Context and UI to ActionInteractionModel 
* UI will make calls to the controller which delegated the persistent level
* call to the ActionInteractionModel
* Each action of the Rule will have a corresponding ActionInteractionModel which will
* be controlled through the ActionController to communicate to the View 
* 
* 
*<code><pre>
* CLASS:
* 	 
*
* RESPONSIBILITIES:
* 	bridges the UI interactions to the business-layer of an Action of a Rule.
*   Support Enable, Disable, Add, Remove action use cases
*
* COLABORATORS:
* 	ActionInteractionInfo - implements the persistence part of the Action
* 	Action - implements the business layer of the individual Actions.
* 	Condition - implements the business layer of the individual Conditions.
*
* USAGE:
* 	See each method.
*
*</pre></code>
*/
public class ActionController extends PublisherController implements Constants, RulesBuilderConstants{
	
	ActionPublisherList 				mActPubList;
	
	private static final String TAG = ActionController.class.getSimpleName();
	private static final String EXTRA_IMPORT_RULE = "com.motorola.intent.action.IMPORT_RULE";
	
	public ActionController(Context context, RuleController rc,
									IEditRulePluginCallback cb){
		super(context, rc, cb);
	}
	
	/** This function is called when a action has been selected from the condition list
    *
    * @param data
    */
   protected void processSelectedActionListItem(Intent data) {
	   if (data == null) {
           if (LOG_DEBUG) Log.d(TAG, NULL_INTENT);
       }
       else {
       	   String actionPub = data.getStringExtra(ACTION_PUB_SEL);
           Publisher actionPubInfo = mRuleController.fetchActionPublisherList().getMatchingPublisher(actionPub);
           if(actionPubInfo != null){
	           actionPubInfo.setWhenRuleEnds(data.getBooleanExtra(EXTRA_RULE_ENDS, false));
	           String desc_extra = data.getStringExtra(EXTRA_DESCRIPTION);
	           if (desc_extra == null){
	        	   actionPubInfo.setBlockDescription(mContext.getString(R.string.description));
	           }else{
	        	   actionPubInfo.setBlockDescription(desc_extra);
	           }
	           actionPubInfo.setValidity(TableBase.Validity.VALID);
	           actionPubInfo.setConfig(data.getStringExtra(EXTRA_CONFIG));
	           if (LOG_DEBUG) Log.d(TAG, "Extras from Action Activity " + actionPubInfo.toString());
	           if (LOG_DEBUG) Log.d(TAG, "Extras block name: " + actionPubInfo.getBlockName());
	           
	           	//First we need to add the view representing this action to the  Rule
	           View tmpView = mEditRuleCallback.processPluginSelectedActionListItem(actionPubInfo, data);
	           Publisher pubInfo = (Publisher)tmpView.getTag();
	           addNewAction( pubInfo,  data);
	           //this helps in toggling the Save button among other things after a Rule has been added
	           //at which point it becomes a valid rule to be saved
	           mEditRuleCallback.processGenericSelectedActionListItem(); 
	           
	           //check if Dialog for WiFi action correlation to Loc needs to be shown
	           //this is just an informative dialog with OK, no Cancel option
	           if(pubInfo.getBlockName().equalsIgnoreCase(mContext.getString(R.string.wifi_action_block_name))){
	        	   if(LocationConsent.checkForWifiActionConsent(mContext)){
	        		   	((Activity) mContext).showDialog(DialogId.DIALOG_WIFI_AIRPLANE_LOC_CORRELATION_ID);
	        		}
	
	           }
           }
       }
   }
   
   /**
    * Add the action block information to the Action List
    * @param pubInfo
    * @param data
    */
   private void addNewAction(Publisher pubInfo, Intent data) {
     int modality = ModalTable.Modality.UNKNOWN;
     String targetState = null;

     if (pubInfo.getStateType().equals(STATEFUL)) modality = ModalTable.Modality.STATEFUL;
     else modality = ModalTable.Modality.STATELESS;
  
     //Extras returned by QuickActions
     targetState = data.getStringExtra(EXTRA_DESCRIPTION);
     if (targetState == null) targetState = NO_TARGET_STATE;
     
     String ruleXml = null;
     String configUri  = data.getStringExtra(EXTRA_CONFIG);
     Intent configIntent = Util.getIntent(configUri);
     if (configUri != null) {
         ruleXml = configIntent.getStringExtra(EXTRA_IMPORT_RULE);
     }
     
     mRuleModel.getActionList().add(new ActionInteractionModel(-1,
   		  									mRuleModel.getRuleInstance().get_id(),
   		  									(pubInfo.getBlockConnectedStatus()?ActionTable.Enabled.ENABLED:ActionTable.Enabled.DISABLED),
   		  									0, //Active
   		  									ActionTable.ConflictWinner.LOSER,
   		  									(pubInfo.getWhenRuleEnds()?ActionTable.OnModeExit.ON_EXIT:ActionTable.OnModeExit.ON_ENTER),
   		  									RuleTable.SuggState.ACCEPTED,
   		  									null,
   		  									pubInfo.getBlockDescription(),
   		  									pubInfo.getPublisherKey(),
   		  									modality,
   		  									pubInfo.getBlockName(),
   		  									targetState,
											pubInfo.getConfig(), //TODO whys this 2 times
   		  									pubInfo.getIntentUriString(),
											pubInfo.getConfig(), //ActionSyntax was null in original code
   		  									0,//Last fired date and time
   		  									null,//action fail msg
   		  									null,//icon
   		  									Integer.toString(pubInfo.getBlockId()),
   		                                    pubInfo.getBlockInstanceId(),
   		                                    EditType.ADDED,
   		                                    ruleXml,
   		                                    pubInfo.getConfig(),
   		                                    TableBase.Validity.VALID,
   		                                    pubInfo.getMarketUrl(), 
   		                                    null)); // Child Rule Key ; Will be populated when the rule is saved
   }
   
   /**
    * Helper api to remove Action from Rule
    * @param conPub
    */
   public void removeAction(View actPub){
	   Blocks.updateActionBlockDeleteStatus(mRuleModel, actPub);
	   //callback to do the needful on View side
	   mEditRuleCallback.removeGenericPublisher(actPub);
	   mEditRuleCallback.removePluginPublisher(actPub);

   }
   
   /**
    * Helper function to configure action in Rule
    * @param data
    * @param mLastAction
    */
   public void configureAction(Intent data, ActionInteractionModel mLastAction){
		String actionDescription = null;
	   	@SuppressWarnings("unused")
		String targetState = null;
	   	
	   	    if (mLastAction == null) {
	   	    	Log.e(TAG, "mLastAction is null");
	   	    }
	   	    else{
	   	    	mLastAction.getAction().setOnExitModeFlag(data.getBooleanExtra(EXTRA_RULE_ENDS, false)?
		      			ActionTable.OnModeExit.ON_EXIT:ActionTable.OnModeExit.ON_ENTER);
		
	   	    	String configUri  = data.getStringExtra(EXTRA_CONFIG);
	   	    	if (LOG_DEBUG) Log.d(TAG, "Config Uri is " + configUri);
	   	    	Intent configIntent = Util.getIntent(configUri);
	   	    	if (configIntent != null) {
	   	    	    String ruleXml = configIntent.getStringExtra(EXTRA_IMPORT_RULE);
	   	    	    if (ruleXml != null) mLastAction.setChildRuleXml(ruleXml);
	   	    	    configIntent.removeExtra(EXTRA_IMPORT_RULE);
	   	    	    data.putExtra(EXTRA_CONFIG, configIntent.toUri(0));
	   	    	    if (LOG_DEBUG) Log.d(TAG, "Config Uri is " + configIntent.toUri(0));
	            }
                
	   	    	if ((actionDescription = targetState = data.getStringExtra(EXTRA_DESCRIPTION)) == null) 
		  			actionDescription = targetState = mContext.getString(R.string.description);
		  		mLastAction.getAction().setDescription(actionDescription);
                String config = data.getStringExtra(EXTRA_CONFIG);
				mLastAction.getAction().setConfig(config);
				mLastAction.getAction().setValidity(TableBase.Validity.VALID);
		  		
		  		mEditRuleCallback.postConfigureGenericActionBlock();
		  		mEditRuleCallback.postConfigurePluginActionBlock();
	   	    }
	   
   }

}
