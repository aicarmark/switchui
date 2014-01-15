/*
 * @(#)ConditionController.java
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

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.ModalTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.rulesbuilder.Blocks;
import com.motorola.contextual.smartrules.rulesbuilder.Blocks.EditType;
import com.motorola.contextual.smartrules.rulesbuilder.RulesBuilderConstants;
import com.motorola.contextual.smartrules.uipublisher.ConditionPublisherList;
import com.motorola.contextual.smartrules.uipublisher.Publisher;
import com.motorola.contextual.smartrules.uipublisher.PublisherController;

/**
* ConditionController  bridges the Activity/Context and UI to ConditionInteractionModel
* UI will make calls to the controller which delegated the persistent level
* call to the ConditionInteractionInfo
* Each condition of the Rule will have a corresponding ConditionController and
* ConditionInteractionModel
*
*<code><pre>
* CLASS:
*
*
* RESPONSIBILITIES:
* 	Bridges the UI interactions to the business-layer of an Condition of a Rule.
*   Support Enable, Disable, Add, Remove condition use cases
*
* COLABORATORS:
* 	ConditionInteractionInfo - implements the persistence part of the Action
* 	Action - implements the business layer of the individual Actions.
* 	Condition - implements the business layer of the individual Conditions.
*
* USAGE:
* 	See each method.
*
*</pre></code>
*/
public class ConditionController extends PublisherController implements Constants, RulesBuilderConstants{

	ConditionPublisherList 				mCondPubList;

	private static final String TAG = ConditionController.class.getSimpleName();

	public ConditionController(Context context, RuleController rc,
									IEditRulePluginCallback cb){
		super(context, rc, cb);
	}

	/** This function is called when a condition has been selected from the condition list
    *
    * @param data
    */
   protected void processSelectedConditionListItem(Intent data) {
       if (data == null) {
           if (LOG_DEBUG) Log.d(TAG, NULL_INTENT);
       }
       else {
	       String condPub = data.getStringExtra(CONDITION_PUB_SEL);
           Publisher condPubInfo = mRuleController.fetchConditionPublisherList().get(condPub);
           if(condPubInfo != null){
	           if (data.getStringExtra(EXTRA_DESCRIPTION) != null){
				condPubInfo.setBlockDescription(data.getStringExtra(EXTRA_DESCRIPTION));
	           }else{
				condPubInfo.setBlockDescription(mContext.getString(R.string.description));
	           }
	           String config = data.getStringExtra(EXTRA_CONFIG);

	           condPubInfo.setConfig(config);
	           condPubInfo.setValidity(TableBase.Validity.VALID);
	           if (LOG_DEBUG) Log.d(TAG, "Extras from Condition Activity " + condPubInfo.toString());

	           mEditRuleCallback.processGenericSelectedConditionListItem();
	           View tmpView = mEditRuleCallback.processPluginSelectedConditionListItem(condPubInfo, data);
	           Publisher pubInfo = (Publisher)tmpView.getTag();
	           addNewCondition( pubInfo,  data);
           }
       }
   }

   /**Add the condition block information to the condition lists
   *
   * @param createdBlock
   */
    private void addNewCondition(Publisher pubInfo, Intent data) {
        //Extras returned by the trigger activities
		String config = data.getStringExtra(EXTRA_CONFIG);
		String description = data.getStringExtra(EXTRA_DESCRIPTION);
		if(LOG_DEBUG) Log.d(TAG, " Config info : " + config + " description : " + description);

		mRuleModel.getConditionList().add(new ConditionInteractionModel(-1,
		                                           mRuleModel.getRuleInstance().get_id(),
		                                           (pubInfo.getBlockConnectedStatus()?ConditionTable.Enabled.ENABLED: ConditionTable.Enabled.DISABLED ),
		                                           RuleTable.SuggState.ACCEPTED, //Sugg State
		                                           null, //Sugg Reason
		                                           ConditionTable.CondMet.COND_NOT_MET, //cond Met
		                                           pubInfo.getPublisherKey(),
		                                           ModalTable.Modality.UNKNOWN,//modality
		                                           pubInfo.getBlockName(), //sensor name
		                                           pubInfo.getIntentUriString(), //Activity Intent
		                                           description,
		                                           pubInfo.getBlockDescription(),
		                                           config,//State syntax
		                                           0,//Created data time
		                                           0, //Last Fail Date time
		                                           null, //cond Fail
		                                           null, //icon
		                                           Integer.toString(pubInfo.getBlockId()),
		                                           pubInfo.getBlockInstanceId(),
		                                           EditType.ADDED,
		                                           pubInfo.getBlockName(),
   		                                           pubInfo.getBlockName(),
		                                           config,
		                                           TableBase.Validity.VALID,
		                                           pubInfo.getMarketUrl()));


    }
    

    public void removeCondition(View conPub){
		Blocks.updateConditionBlockDeleteStatus(mRuleModel, conPub);
		//callback to do the needful on View side
		mEditRuleCallback.removeGenericPublisher(conPub);
		mEditRuleCallback.removePluginPublisher(conPub);
    }

    public void configureCondition(Intent data, ConditionInteractionModel mLastCondition){
	 String newEventDescription = null;

	     if(mLastCondition == null) {
		  Log.e(TAG, "mLastCondition is null for eventName "+newEventDescription);
	     }else {

	         if ((newEventDescription = data.getStringExtra(EXTRA_DESCRIPTION)) == null)
		  newEventDescription = mContext.getString(R.string.description);
	          String config = data.getStringExtra(EXTRA_CONFIG);
		  String oldConfig = mLastCondition.getCondition().getConfig();
		      if(oldConfig != null && !oldConfig.equals(config)){
		       if(LOG_DEBUG) Log.d(TAG, " Old Config " + oldConfig + "new config " + config);
		        mRuleController.getRuleModel().addToOldConfigPubKeyList(oldConfig, mLastCondition.getCondition().getPublisherKey());
	     }

		  mLastCondition.getCondition().setDescription(newEventDescription);
		  mLastCondition.getCondition().setConfig(config);
		  mLastCondition.getCondition().setValidity(TableBase.Validity.VALID);

		      mEditRuleCallback.postConfigureGenericConditionBlock();
		      mEditRuleCallback.postConfigurePluginConditionBlock();
         }

    }
}
