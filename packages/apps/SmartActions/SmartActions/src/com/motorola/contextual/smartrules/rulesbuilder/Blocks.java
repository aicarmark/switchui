/*
 * @(#)Blocks.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18385/E51185 2011/01/27 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.rulesbuilder;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.text.Html;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ActionTable.Active;
import com.motorola.contextual.smartrules.db.table.ActionTable.ConflictWinner;
import com.motorola.contextual.smartrules.db.table.ActionTuple;
import com.motorola.contextual.smartrules.db.table.ConditionTuple;
import com.motorola.contextual.smartrules.db.table.ModalTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.RuleTable.SuggState;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.rulesbuilder.BlockAbsoluteLayout.LayoutParams;
import com.motorola.contextual.smartrules.uiabstraction.ActionInteractionModel;
import com.motorola.contextual.smartrules.uiabstraction.ActionInteractionModel.ActionInteractionModelList;
import com.motorola.contextual.smartrules.uiabstraction.ConditionInteractionModel;
import com.motorola.contextual.smartrules.uiabstraction.ConditionInteractionModel.ConditionInteractionModelList;
import com.motorola.contextual.smartrules.uiabstraction.RuleInteractionModel;
import com.motorola.contextual.smartrules.uipublisher.ActionPublisherList;
import com.motorola.contextual.smartrules.uipublisher.ConditionPublisherList;
import com.motorola.contextual.smartrules.uipublisher.Publisher;
import com.motorola.contextual.smartrules.util.Util;


/** This class has all the block related logic for a given rule
 *
 *<code><pre>
 * CLASS:
 *    Implements Constants
 * RESPONSIBILITIES:
 *     -Creates and Displays blocks for a rule
 *     - Updates Status text and lights of blocks
 *
 * USAGE:
 *     See each method.
 *
 *</pre></code>
 */
public class Blocks implements Constants,
							   RulesBuilderConstants{

	private static final String TAG = Blocks.class.getSimpleName();

	public static void displayActionBlocks(Context context, RuleInteractionModel rule, ActionPublisherList actionPubList,
    		BlockController pcBlockController, BlockController actionBlockController,
			 BlockLayout pcBlockLayer, BlockLayout actionBlockLayer, View.OnClickListener thisOnClickListener){

    	for (int i = 0; i < rule.getActionList().size(); i++){
			String pubKey = rule.getActionList().get(i).getAction().getPublisherKey();
			if(pubKey!=null && actionPubList.containsKey(pubKey)){
				//Identify which action in the Action List corresponds to this action entry. This is done by comparing the publisher key.
				//We compare action publisher from Model/DB and from PublisherList/PackageMgr
				Publisher blockInfo = actionPubList.getMatchingPublisher(pubKey);
				addActionBlockToCanvasFromDB(context, rule, blockInfo,
												rule.getActionList().get(i), actionBlockLayer, thisOnClickListener);
			} else if(pubKey!=null && !actionPubList.containsKey(pubKey)){
				ActionTuple t = rule.getActionList().get(i).getAction();
				if(LOG_DEBUG) Log.d(TAG, "Action not found in master list  " +t.getPublisherKey());
				if(t.getMarketUrl() != null && !t.getValidity().equals(TableBase.Validity.BLACKLISTED)) {
					Publisher blockInfo = new Publisher();
					addActionBlockToCanvasFromDB(context, rule, blockInfo,
								rule.getActionList().get(i), actionBlockLayer, thisOnClickListener);
				}
			}
    	}

   	}

    /**
     *
     */
    public static void displayConditionBlocks(Context context, RuleInteractionModel rule, ConditionPublisherList conditionPubList,
    		BlockController pcBlockController, BlockController actionBlockController,
			 BlockLayout pcBlockLayer, BlockLayout actionBlockLayer, View.OnClickListener thisOnClickListener){

       	for (int i = 0; i < rule.getConditionList().size(); i++){
		String pubKey = rule.getConditionList().get(i).getCondition().getPublisherKey();
		if(pubKey!=null && conditionPubList.containsKey(pubKey)){
			//Identify which condition in the Condition List corresponds to this condition entry. This is done by comparing the publisher key.
			//We compare action publisher from Model/DB and from PublisherList/PackageMgr
			Publisher blockInfo = (Publisher)conditionPubList.get(pubKey);
			addConditionBlockToCanvasFromDB(context, rule, blockInfo, rule.getConditionList().get(i),
												pcBlockLayer, thisOnClickListener );
		}else if(pubKey!=null && !conditionPubList.containsKey(pubKey)){
			ConditionTuple t = rule.getConditionList().get(i).getCondition();
			if(LOG_DEBUG) Log.d(TAG, "Condition not found in master list  " +
					 rule.getConditionList().get(i).getCondition().getPublisherKey());
			if(t.getMarketUrl() != null  && !t.getValidity().equals(TableBase.Validity.BLACKLISTED)){
				Publisher blockInfo = new Publisher();
				addConditionBlockToCanvasFromDB(context, rule, blockInfo, rule.getConditionList().get(i),
								pcBlockLayer, thisOnClickListener );
			}

	}
    	}
    }


    /**
     *
     * @param position
     * @param action
     */
    private static void addActionBlockToCanvasFromDB(Context context, RuleInteractionModel rule, Publisher blockInfo, ActionInteractionModel action,
    										BlockLayout actionBlockLayer, View.OnClickListener thisOnClickListener){

	    updateActionBlockInfoFromTuple(context, blockInfo, action);
		createCorrespondingActionBlocks(context,actionBlockLayer, thisOnClickListener, blockInfo, rule);
		int numBlocks = rule.getNumberOfActionEntriesForBlockId(Integer.toString(blockInfo.getBlockId()));
		blockInfo.setBlockInstanceId(blockInfo.getPublisherKey() + Integer.toString(numBlocks + 1)) ;
		action.setBlockId(Integer.toString(blockInfo.getBlockId()));
    	action.setBlockInstanceId(blockInfo.getBlockInstanceId());
    }


    /**
     *
     * @param position
     * @param condition
     */
    public static void addConditionBlockToCanvasFromDB(Context context, RuleInteractionModel rule, Publisher blockInfo, ConditionInteractionModel condition,
    												   BlockLayout pcBlockLayer, View.OnClickListener thisOnClickListener) {

		updateConditionBlockInfoFromTuple(context, blockInfo, condition);
		createCorrespondingConditionBlocks(context,pcBlockLayer, thisOnClickListener, blockInfo, rule);
	    int numBlocks = rule.getNumberOfConditionEntriesForBlockId(Integer.toString(blockInfo.getBlockId()));
		blockInfo.setBlockInstanceId(blockInfo.getPublisherKey() + Integer.toString(numBlocks + 1));
		condition.setBlockId(Integer.toString(blockInfo.getBlockId()));
		condition.setBlockInstanceId(blockInfo.getBlockInstanceId());
    }

    /** Populates the following from the Action Table
     *  Connected status, Description, Name, Config (used for determining if incomplete configuration),
     *  whenRuleEnds, intentUriString, suggested, conflict, error
     * @param blockInfo -  The block to be updated
     * @param actionTuple -  The action tuple
     */
    private static void updateActionBlockInfoFromTuple( Context context, Publisher blockInfo, ActionInteractionModel action ){
	    blockInfo.setBlockConnectedStatus((action.getAction().getEnabled()==1)) ;
	    
		blockInfo.setBlockDescription(action.getAction().getDescription()) ;
		if (!Util.isNull(action.getAction().getStateMachineName())){
			blockInfo.setBlockName(action.getAction().getStateMachineName());
		}

		String config = action.getAction().getConfig();
		if (!Util.isNull(config)){
			blockInfo.setConfig(config);
		}

		if(action.getAction().isOnExitModeAction()) blockInfo.setWhenRuleEnds(true);
		if((action.getAction().getSuggState() != SuggState.ACCEPTED) && (action.getAction().getEnabled() == 0)){
			blockInfo.setSuggested(true);
		}
		String actionFailMsg = action.getAction().getActFailMsg();
		if ((actionFailMsg != null ) && (!actionFailMsg.equals(NULL_STRING)) && 
				(!actionFailMsg.equals(SUCCESS)) && (!actionFailMsg.equals(QA_SUCCESS)) ||
				action.getAction().getValidity().equals(ActionTable.Validity.INVALID) ||
				action.getAction().getValidity().equals(ActionTable.Validity.UNAVAILABLE)) {
			blockInfo.setError(true);
			StringBuffer buf = new StringBuffer();
			buf.append(context.getResources().getString(R.string.error));
			if (actionFailMsg != null &&!actionFailMsg.equals(FAILURE) && !actionFailMsg.equals(QA_FAILURE)) {
				buf.append (COLON + SPACE + actionFailMsg.replaceFirst(FAILURE+COLON, ""));
			}
			blockInfo.setErrorText(buf.toString());
		} else {
		    //clear any previously set error
		    blockInfo.setError(false);
		    blockInfo.setErrorText(null);
		}
		if((action.getAction().getActive() == Active.ACTIVE) && 
				(action.getAction().getConfWinner() == ConflictWinner.LOSER) && (action.getAction().getModality() == ModalTable.Modality.STATEFUL)){
			blockInfo.setConflict(true);
		}
		if(action.getAction().getActive() == Active.ACTIVE){
			blockInfo.setActive(true);
		}

		blockInfo.setPublisherKey(action.getAction().getPublisherKey());
		blockInfo.setValidity(action.getAction().getValidity());
		blockInfo.setMarketUrl(action.getAction().getMarketUrl());
		if(LOG_DEBUG) Log.d(TAG, "Action block : " + blockInfo + "\n Action : " +action.getAction());
    }


    /** Populates the following from the Condition Table
     * Connected status, Description, Config (used for determining if incomplete configuration),
     * intentUriString
     * @param context TODO
     * @param blockInfo - The block to be updated
     * @param conditionTuple - the condition tuple
     */
    private static void updateConditionBlockInfoFromTuple(Context context, Publisher blockInfo, ConditionInteractionModel condition ){
    	//Populate the following from the Condition Table
		//Connected status, Description, Config (used for determining if incomplete configuration),
		//intentUriString
    	if (!Util.isNull(condition.getCondition().getSensorName())){
            blockInfo.setBlockName(condition.getCondition().getSensorName());
        }
		String config = condition.getCondition().getConfig();
		if (!Util.isNull(config)){
			blockInfo.setConfig(config);
		}
		blockInfo.setAction(false);
		blockInfo.setBlockConnectedStatus((condition.getCondition().getEnabled()==1));
		blockInfo.setBlockDescription(condition.getCondition().getDescription());
		blockInfo.setPublisherKey(condition.getCondition().getPublisherKey());
		blockInfo.setValidity(condition.getCondition().getValidity());
		blockInfo.setMarketUrl(condition.getCondition().getMarketUrl());

		String conditionFailMsg = condition.getCondition().getCondFailMsg();
		if ((conditionFailMsg != null ) && (!conditionFailMsg.equals(NULL_STRING)) && 
				(!conditionFailMsg.equals(SUCCESS)) && (!conditionFailMsg.equals(QA_SUCCESS)) ||
				condition.getCondition().getValidity().equals(ActionTable.Validity.INVALID) ||
				condition.getCondition().getValidity().equals(ActionTable.Validity.UNAVAILABLE)) {
			blockInfo.setError(true);
			StringBuffer buf = new StringBuffer();
			buf.append(context.getResources().getString(R.string.error));
			if (conditionFailMsg != null && !conditionFailMsg.equals(FAILURE) && !conditionFailMsg.equals(QA_FAILURE)) {
				buf.append (COLON + SPACE + conditionFailMsg.replaceFirst(FAILURE+COLON, ""));
			}
			blockInfo.setErrorText(buf.toString());
		} else {
            //clear any previously set error
            blockInfo.setError(false);
            blockInfo.setErrorText(null);
        }
		
		if((condition.getCondition().getSuggState() != SuggState.ACCEPTED) && (condition.getCondition().getEnabled() == 0)){
			blockInfo.setSuggested(true);
		}
		if(LOG_DEBUG) Log.d(TAG, "Condition block : " + blockInfo + "\n Condition tuple : " + condition.getCondition());
    }




	 /** This function creates an action block that needs to be displayed on the canvas.
     *
     *
     * @param position
     * @return
     */
    public static View createCorrespondingActionBlocks(Context context, BlockLayout actionBlockLayer,
													View.OnClickListener thisOnClickListener, Publisher blockInfo, RuleInteractionModel rule) {
		View innerBlockContainer;
        int indexOfAddedChild;
        ViewGroup justAddedView;
        int left = 0;
        int top = actionBlockLayer.getHeight();

        View tmpView = LayoutInflater.from(context).inflate(R.layout.action_block_layout_2, null, false);
        tmpView.setTag(blockInfo);

        BlockLayout.LayoutParams lp = new BlockLayout.LayoutParams (LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, left, top);
        actionBlockLayer.addView(tmpView, lp);
        tmpView.setOnClickListener(thisOnClickListener);

        blockInfo = setActionBlockStatus(context, tmpView, rule.getRuleInstance().getActive());
        tmpView.setTag(blockInfo);

        indexOfAddedChild = actionBlockLayer.getChildCount();
        if(LOG_DEBUG) Log.d(TAG, "Index of added child" + indexOfAddedChild);
        justAddedView = (ViewGroup)actionBlockLayer.getChildAt(indexOfAddedChild-1);
        innerBlockContainer = justAddedView.findViewById(R.id.BlockContainer);

        if(!blockInfo.getBlockConnectedStatus()) {
            RelativeLayout.LayoutParams in_lp = new RelativeLayout.LayoutParams
            (LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            in_lp.setMargins(getPixelsOfDpi(context,DISCONNECT_POS), 0, 0, 0);
            justAddedView.updateViewLayout(innerBlockContainer, in_lp);
        }
        return tmpView;
    }

    /** This function creates a condition block that needs to be displayed on the canvas.
    *
    *
    * @param blockInfo - the condition block
    * @return
    */
   public static View createCorrespondingConditionBlocks(Context context,
														BlockLayout pcBlockLayer,
													    View.OnClickListener thisOnClickListener, Publisher blockInfo, RuleInteractionModel rule) {

       View innerBlockContainer;
       int indexOfAddedChild;
       ViewGroup justAddedView;

       View tmpView = LayoutInflater.from(context).inflate(R.layout.trigger_block_layout_2, null, false);
       tmpView.setTag(blockInfo);

       int left = 0;
       int top = pcBlockLayer.getHeight();
       BlockLayout.LayoutParams lp = new BlockLayout.LayoutParams (LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, left, top);
       pcBlockLayer.addView(tmpView, lp);
       tmpView.setOnClickListener(thisOnClickListener);

       blockInfo = setConditionBlockStatus(context, tmpView, rule.getRuleInstance().getActive());
       tmpView.setTag(blockInfo);
       //tmpView reference seems to be stale, need to find reference to just added view
       // and use that reference in findViewById
       indexOfAddedChild = pcBlockLayer.getChildCount();
       if (LOG_DEBUG) Log.d(TAG, "Index of added child" + indexOfAddedChild);
       justAddedView = (ViewGroup)pcBlockLayer.getChildAt(indexOfAddedChild-1);
       innerBlockContainer = justAddedView.findViewById(R.id.BlockContainer);
       if(!blockInfo.getBlockConnectedStatus()) {
           RelativeLayout.LayoutParams in_lp = new RelativeLayout.LayoutParams
           (LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
           in_lp.setMargins(getPixelsOfDpi(context,DISCONNECT_POS), 0, 0, 0);
           justAddedView.updateViewLayout(innerBlockContainer, in_lp);
       }
       return tmpView;
   }

    /**This function displays the appropriate status line and status light on the action blocks
     *
     * @param tmpView - The view corresponding to the action block
     * @return Action block
     */
    public static Publisher setActionBlockStatus(Context context, View tmpView, int ruleActiveFlag){
    	TextView aTitle = (TextView)tmpView.findViewById(R.id.actiontitle);
    	ImageView aImage = (ImageView)tmpView.findViewById(R.id.actionblockImg);
    	ImageView statInd = (ImageView)tmpView.findViewById(R.id.blockStatusInd);
    	TextView aDescription = (TextView)tmpView.findViewById(R.id.actiondescription);
    	TextView aStatus = (TextView)tmpView.findViewById(R.id.actionStatus);
    	Publisher savedBlockInfo = (Publisher)tmpView.getTag();
        aTitle.setText(Html.fromHtml(savedBlockInfo.getBlockName()));
        aImage.setImageDrawable(savedBlockInfo.getImage_drawable());
        aImage.setColorFilter(Color.WHITE);

        // Set actionStatus TextView's visibility to GONE now, on need basis it will be enabled in the below method calls
        aStatus.setVisibility(View.GONE);
        if(null==savedBlockInfo.getBlockDescription()){
        	aDescription.setVisibility(View.GONE);
        }else{
        	aDescription.setVisibility(View.VISIBLE);
        	aDescription.setText(Html.fromHtml(savedBlockInfo.getBlockDescription()));
        }
        if (savedBlockInfo.getWhenRuleEnds()) displayEndOfRuleStatus(context, tmpView);
        if ((ruleActiveFlag == RuleTable.Active.ACTIVE) && (savedBlockInfo.getBlockConnectedStatus()))
        	statInd.setImageResource(R.drawable.status_indicator_active);
		else
			statInd.setImageResource(R.drawable.status_indicator_disabled);

        if (savedBlockInfo.getConflict()) displayConflictStatus(context, savedBlockInfo, tmpView);
        if (savedBlockInfo.getConfig() == null) displayConfigurationRequiredStatus(context, tmpView);
        if (savedBlockInfo.getSuggested()) displaySuggestionStatus(context, savedBlockInfo, tmpView);
        if (savedBlockInfo.getError()) displayErrorStatus(context, savedBlockInfo, tmpView);
        if(savedBlockInfo.getValidity() != null &&
		   savedBlockInfo.getValidity().equals(TableBase.Validity.UNAVAILABLE)){
	        if(savedBlockInfo.getMarketUrl() != null) displayInstallationRequiredStatus(context, tmpView);
		    else displayErrorStatus(context, savedBlockInfo, tmpView);
        }
        return savedBlockInfo;
    }


    /**This function displays the appropriate status line and status light on the condition blocks
    *
    * @param tmpView - The view corresponding to the condition block
    * @return Action block
    */
   public static Publisher setConditionBlockStatus(Context context, View tmpView, int ruleActiveFlag){
	   Publisher savedBlockInfo = (Publisher)tmpView.getTag();
       TextView cTitle = (TextView)tmpView.findViewById(R.id.actiontitle);
       cTitle.setText(Html.fromHtml(savedBlockInfo.getBlockName()));
       ImageView cImage = (ImageView)tmpView.findViewById(R.id.actionblockImg);
       cImage.setImageDrawable(savedBlockInfo.getImage_drawable());
       TextView cDescription = (TextView)tmpView.findViewById(R.id.actiondescription);
       TextView cStatus = (TextView)tmpView.findViewById(R.id.actionStatus);
	   ImageView statInd = (ImageView)tmpView.findViewById(R.id.blockStatusInd);

       // Set actionStatus TextView's visibility to GONE now, on need basis it will be enabled in the below method calls
       cStatus.setVisibility(View.GONE);

       if(null==savedBlockInfo.getBlockDescription()){
	   cDescription.setVisibility(View.GONE);
       }else{
	   cDescription.setVisibility(View.VISIBLE);
	   cDescription.setText(Html.fromHtml(savedBlockInfo.getBlockDescription()));
       }

       if (savedBlockInfo.getPublisherKey().equals(LOCATION_TRIGGER)){
	  if (!LocationConsent.isGoogleLocationProviderAvailable(context) ||
							(LocationConsent.isAirplaneModeOn(context))	 ||
							!LocationConsent.isWifiEnabled(context) ||
							!LocationConsent.isWifiSleepSetToNever(context)  ||
							!Util.isMotLocConsentAvailable(context) ){
		  if (LOG_DEBUG) Log.d(TAG, "Need to show error in Condition");
		  savedBlockInfo.setErrorText(context.getResources().getString(R.string.error)) ;
		  savedBlockInfo.setError(true);
		  //we need this attribute to differentiate which activity will
		  // show the dialog upon touching the UI Block, as UI module
		  // needs to do the 'dirty' work of handling/consuming extra touch events,
		  // while the dialog comes from another activity
		  // In this specific case dialog needs to be popped from same activity from which blocks view is bound
		  savedBlockInfo.setNeedToPopDialogFromThisActivity(true);
	  }else {
			  if (LOG_DEBUG) Log.d(TAG, "Location consent available, remove error");
			  savedBlockInfo.setError(false);
			  savedBlockInfo.setNeedToPopDialogFromThisActivity(false);
			  cDescription.setTextColor(context.getResources().getColor(R.color.white));
			  removeStatus(tmpView);
		    }
       }


       if ((ruleActiveFlag == RuleTable.Active.ACTIVE) && (savedBlockInfo.getBlockConnectedStatus())){
		statInd.setImageResource(R.drawable.status_indicator_active);
		savedBlockInfo.setActive(true);
       } else
			statInd.setImageResource(R.drawable.status_indicator_disabled);

       if (savedBlockInfo.getConfig() == null) displayConfigurationRequiredStatus(context, tmpView);
       if (savedBlockInfo.getSuggested()) displaySuggestionStatus(context, savedBlockInfo, tmpView);
       if (savedBlockInfo.getError()) displayErrorStatus(context, savedBlockInfo, tmpView);
       if(savedBlockInfo.getValidity() != null &&
		   savedBlockInfo.getValidity().equals(TableBase.Validity.UNAVAILABLE)){
		   if(savedBlockInfo.getMarketUrl() != null) displayInstallationRequiredStatus(context, tmpView);
		   else displayErrorStatus(context, savedBlockInfo, tmpView);
	   }
       return savedBlockInfo;
   }



    /** Displays the error status line and light on the block
     *
     * @param blockInfo - condition or action block
     * @param tmpView
     */
    public static void displayErrorStatus(Context context, Publisher blockInfo, View tmpView){
    	TextView blockStatus = (TextView)tmpView.findViewById(R.id.actionStatus);
    	ImageView statusInd = (ImageView)tmpView.findViewById(R.id.blockStatusInd);
    	blockStatus.setVisibility(View.VISIBLE);
    	blockStatus.setText(blockInfo.getErrorText());
    	blockInfo.setActive(false);
    	blockStatus.setTextColor(context.getResources().getColor(R.color.error_orange));
    	statusInd.setImageResource(R.drawable.status_indicator_error);
    }

    /** Displays the suggestion status line and light on the block
     *
     * @param blockInfo -  condition or action block
     * @param tmpView
     */
    public static void displaySuggestionStatus(Context context, Publisher blockInfo, View tmpView){
    	TextView blockStatus = (TextView)tmpView.findViewById(R.id.actionStatus);
    	ImageView statusInd = (ImageView)tmpView.findViewById(R.id.blockStatusInd);
    	blockStatus.setVisibility(View.VISIBLE);
	blockStatus.setText(R.string.suggested);
    	blockStatus.setTextColor(context.getResources().getColor(R.color.suggestion_green));
    	statusInd.setImageResource(R.drawable.status_indicator_suggestion);
    }

    /** Removes the "Suggestion" text from the block
     *
     * @param tmpView
     */
    public static void removeStatus(View tmpView){
    	ImageView statInd = (ImageView)tmpView.findViewById(R.id.blockStatusInd);
    	statInd.setImageResource(R.drawable.status_indicator_disabled);
    	TextView blockStatus = (TextView)tmpView.findViewById(R.id.actionStatus);
    	blockStatus.setVisibility(View.GONE);
    }

    /** Displays the End of Rule status line and light on the block
     *
     * @param blockInfo
     * @param tmpView
     */
    public static void displayEndOfRuleStatus(Context context, View tmpView){
    	TextView blockStatus = (TextView)tmpView.findViewById(R.id.actionStatus);
    	blockStatus.setVisibility(View.VISIBLE);
    	blockStatus.setText(R.string.end_of_rule);
    	blockStatus.setTextColor(context.getResources().getColor(R.color.block_status_gray));
    }

    /** Displays the Conflict status line and light on the block
     *
     * @param blockInfo
     * @param tmpView
     */
    public static void displayConflictStatus(Context context, Publisher blockInfo, View tmpView){
	if(LOG_DEBUG) Log.d(TAG, "displayConflictStatus ");
    	TextView blockStatus = (TextView)tmpView.findViewById(R.id.actionStatus);
        blockStatus.setVisibility(View.VISIBLE);
    	blockStatus.setText(R.string.conflict);
    	blockStatus.setTextColor(context.getResources().getColor(R.color.block_status_gray));
    	displayNoLights(tmpView);
    }

    /**Displays the "Configuration required" status line and light on the block
     *
     * @param tmpView
     */
    public static void displayConfigurationRequiredStatus(Context context, View tmpView){
    	TextView blockStatus = (TextView)tmpView.findViewById(R.id.actiondescription);
    	blockStatus.setVisibility(View.VISIBLE);
    	blockStatus.setText(R.string.status);

    	Publisher savedBlockInfo = (Publisher)tmpView.getTag();
    	if (savedBlockInfo.getBlockConnectedStatus())
			enableConfigurationRequiredStatus(context, tmpView);
		else
    		disableConfigurationRequiredStatus(context, tmpView);
	}

    /**Displays the "Touch here to install" status line and light on the block
    *
    * @param tmpView
    */
   public static void displayInstallationRequiredStatus(Context context, View tmpView){
	   if(LOG_DEBUG) Log.d( TAG, " displayInstallationRequiredStatus ");
	   TextView blockStatus = (TextView)tmpView.findViewById(R.id.actionStatus);
	   blockStatus.setVisibility(View.VISIBLE);
	   blockStatus.setText(R.string.install);
	   blockStatus.setTextColor(context.getResources().getColor(R.color.error_orange));
	   ImageView statusInd = (ImageView)tmpView.findViewById(R.id.blockStatusInd);
	   statusInd.setImageResource(R.drawable.status_indicator_error);

	}

    /**The status line should not be displayed if everything is in order
     *
     * @param tmpView
     */
    public static void disableConfigurationRequiredStatus(Context context, View tmpView){

    	TextView blockStatus = (TextView)tmpView.findViewById(R.id.actiondescription);
    	blockStatus.setTextColor(context.getResources().getColor(R.color.block_status_gray));
    	displayNoLights(tmpView);
    }

    /**The status line should not be displayed if everything is in order
    *
    * @param tmpView
    */
   public static void removeConfigurationRequiredStatus(Context context, View tmpView){

	TextView blockStatus = (TextView)tmpView.findViewById(R.id.actiondescription);
	blockStatus.setTextColor(context.getResources().getColor(R.color.white));
	displayNoLights(tmpView);
   }

    public static void enableConfigurationRequiredStatus(Context context, View tmpView){

    	TextView blockStatus = (TextView)tmpView.findViewById(R.id.actiondescription);
	    blockStatus.setTextColor(context.getResources().getColor(R.color.error_orange));
    	displayNoLights(tmpView);
    }

    /** The status lights should not be on if the block is configured right.
     *
     * @param tmpView
     */
    public static void displayNoLights(View tmpView){
	if(LOG_DEBUG) Log.d(TAG, "displayNoLights ");
     	ImageView statInd = (ImageView)tmpView.findViewById(R.id.blockStatusInd);
    	statInd.setImageResource(R.drawable.status_indicator_disabled);
    }
    
    
    /** The status lights should active when its part of a Connected block of an active rule
     *  which is not in error state
    *
    * @param tmpView
    */
    public static void displayActiveLights(View tmpView){
	if(LOG_DEBUG) Log.d(TAG, "displayActiveLights ");
    	ImageView statInd = (ImageView)tmpView.findViewById(R.id.blockStatusInd);
    	statInd.setImageResource(R.drawable.status_indicator_active);
    }

    /** Refresh the location block with the correct status
    *
    * @param context - Context
    * @param blockView - The view corresponding to the location block.
    * @param event - Event data structure corresponding to the location block
    */
   public static void refreshLocationBlock(Context context, View blockView, ConditionInteractionModel event){
	    if ( (LocationConsent.isGoogleLocationProviderAvailable(context)) &&
	    						!LocationConsent.isAirplaneModeOn(context) &&
	    						LocationConsent.isWifiEnabled(context) && 
	    						LocationConsent.isWifiSleepSetToNever(context)){ 
		    Publisher savedBlockInfo = (Publisher)blockView.getTag();
		    if(LOG_DEBUG) Log.d(TAG, "refreshLocationBlock: " + savedBlockInfo);
		    savedBlockInfo.setError(false) ;
		    savedBlockInfo.setNeedToPopDialogFromThisActivity(false);
			String eventStateSyntax = event.getCondition().getConfig();
	   		if (Util.isNull(eventStateSyntax))
	   			Blocks.displayConfigurationRequiredStatus(context, blockView);
	    }
   }

    /**This function sets the block connected status in the container
    *
    * @param block
    * @param connectStatus
    */
   public static void updateActionBlockConnectStatus(RuleInteractionModel rule, View blockView) {
       Publisher savedBlockInfo = (Publisher)blockView.getTag();

       ActionInteractionModelList actions = rule.getActionList();
       for(int j=0; j < actions.size(); j++) {
           if (actions.get(j).getBlockInstanceId().equals(savedBlockInfo.getBlockInstanceId())) {
        	   actions.get(j).getAction().setEnabled(
        			   				savedBlockInfo.getBlockConnectedStatus()?ActionTable.Enabled.ENABLED:ActionTable.Enabled.DISABLED);
        	   actions.get(j).setState(EditType.EDITED);
               break;
           }
       }
   }

   /**This function sets the block connected status in the container
   *
   * @param block
   * @param connectStatus
   */
   public static void updateConditionBlockConnectStatus(RuleInteractionModel rule, View blockView) {
	   Publisher savedBlockInfo = (Publisher)blockView.getTag();

	  ConditionInteractionModelList conditions = rule.getConditionList();
	  for(int j=0; j < conditions.size(); j++) {
	      if (conditions.get(j).getBlockInstanceId().equals(savedBlockInfo.getBlockInstanceId())) {
	    	  conditions.get(j).getCondition().setEnabled(
	    			  						savedBlockInfo.getBlockConnectedStatus()?ActionTable.Enabled.ENABLED:ActionTable.Enabled.DISABLED);
	    	  conditions.get(j).setState(EditType.EDITED);
	          break;
	      }
	  }
  }

   /**This function sets the block connected status in the container
   *
   * @param block
   * @param connectStatus
   */
  public static void updateActionBlockSuggestionAcceptedStatus(RuleInteractionModel rule, View blockView) {
      Publisher savedBlockInfo = (Publisher)blockView.getTag();

      ActionInteractionModelList actions = rule.getActionList();
      for(int j=0; j < actions.size(); j++) {
          if (actions.get(j).getBlockInstanceId().equals(savedBlockInfo.getBlockInstanceId())) {
       	   actions.get(j).getAction().setSuggState(RuleTable.SuggState.ACCEPTED);
       	   actions.get(j).setState(EditType.EDITED);
              break;
          }
      }
  }

	  /**This function sets the block connected status in the container
	  *
	  * @param block
	  * @param connectStatus
	  */
	 public static void updateConditionBlockSuggestionAcceptedStatus(RuleInteractionModel rule, View blockView) {
	     Publisher savedBlockInfo = (Publisher)blockView.getTag();

	     ConditionInteractionModelList conditions = rule.getConditionList();
	     for(int j=0; j < conditions.size(); j++) {
	         if (conditions.get(j).getBlockInstanceId().equals(savedBlockInfo.getBlockInstanceId())) {
			 conditions.get(j).getCondition().setSuggState(RuleTable.SuggState.ACCEPTED);
	        	 conditions.get(j).setState(EditType.EDITED);
	             break;
	         }
	     }
	 }



   /**This function sets the block connected status in the container
   *
   * @param block
   * @param connectStatus
   */
  public static void updateActionBlockDeleteStatus(RuleInteractionModel rule, View blockView) {
      Publisher savedBlockInfo = (Publisher)blockView.getTag();
      
      ActionInteractionModelList actions = rule.getActionList();
      for(int j=0; j < actions.size(); j++) {
          if (actions.get(j).getBlockInstanceId().equals(savedBlockInfo.getBlockInstanceId())) {
        	  //just mark it to be deleted, actual deletion from persistence class
        	  //happens from RuleInteractionModel:updateActionTable
        	  actions.get(j).setState(EditType.DELETED);
              break;
          }
      }
  }


	  /**This function sets the block connected status in the container
	  *
	  * @param block
	  * @param connectStatus
	  */
	 public static void updateConditionBlockDeleteStatus(RuleInteractionModel rule, View blockView) {
	     Publisher savedBlockInfo = (Publisher)blockView.getTag();
	     ConditionInteractionModelList events = rule.getConditionList();
         for(int j=0; j < events.size(); j++) {
             if (events.get(j).getBlockInstanceId().equals(savedBlockInfo.getBlockInstanceId())) {
            	//just mark it to be deleted, actual deletion from persistence class
           	  	//happens from RuleInteractionModel:updateConditionTable
            	events.get(j).setState(EditType.DELETED);
                break;
             }
         }
	 }

	/**
     *
     * @param ruleActiveFlag
     */
    public static void displayStatusForAllConditionBlocks(Context context, BlockLayout pcBlockLayer, int ruleActiveFlag){
    	View blockView = null;
    	ArrayList<View> triggerList = Blocks.getListofTriggers(pcBlockLayer);
		int numOfTriggers = pcBlockLayer.getChildCount();
		for(int i=0; i< numOfTriggers; i++){


    	    blockView = triggerList.get(i);
    	    Publisher blockInfo = (Publisher)blockView.getTag();
	    setConditionBlockStatus(context, blockView, ruleActiveFlag);

	    if(LOG_DEBUG) Log.d(TAG, "displayStatusForAllConditionBlocks: " + blockInfo);
    	}
    }

    /**
     *
     * @param ruleActiveFlag
     */
    public static void displayStatusForAllActionBlocks(Context context, BlockLayout actionBlockLayer, int ruleActiveFlag ){
    	View blockView = null;
    	ArrayList<View> actionList = Blocks.getListofActions(actionBlockLayer);
	    int numOfActions = actionBlockLayer.getChildCount();
	    for(int i=0; i< numOfActions; i++){
    		blockView = actionList.get(i);
    		Publisher blockInfo = (Publisher)blockView.getTag();
		setActionBlockStatus(context, blockView, ruleActiveFlag);

	        if(LOG_DEBUG) Log.d(TAG, "displayStatusForAllActionBlocks: " + blockInfo);
    	}

    }

    /** This function returns a list of visible trigger blocks
	 *
	 * @return list of visible trigger blocks.
	 */
	public static ArrayList<View> getListofTriggers(BlockLayout pcBlockLayer ){
		ArrayList<View> listView = null;
		if(pcBlockLayer != null) {
			listView = new ArrayList<View>();
			int indexTotal = pcBlockLayer.getChildCount();
			for(int i=0; i<indexTotal; i++)
				listView.add((View)pcBlockLayer.getChildAt(i)) ;
		}
		return listView;
	}

	/**This function returns a list of visible action blocks
	 *
	 * @return list of visible action blocks.
	 */
	public static ArrayList<View> getListofActions(BlockLayout actionBlockLayer){
		ArrayList<View> listView = null;
		if(actionBlockLayer != null) {
			listView = new ArrayList<View>();
			int indexTotal = actionBlockLayer.getChildCount();
			for(int i=0; i<indexTotal; i++)
				listView.add((View)actionBlockLayer.getChildAt(i)) ;
		}
		return listView;
	}



    /**
     *
     * @param blockView
     */
   public static void generateConditionBlockInstanceId(RuleInteractionModel rule, View blockView){
       Publisher savedBlockInfo = (Publisher)blockView.getTag();
       int numBlocks = rule.getNumberOfConditionEntriesForBlockId(Integer.toString(savedBlockInfo.getBlockId()));
       savedBlockInfo.setBlockInstanceId(savedBlockInfo.getPublisherKey() + Integer.toString(numBlocks + 1));
       blockView.setTag(savedBlockInfo);
   }

   /**
    *
    * @param blockView
    */
   public static void generateActionBlockInstanceId(RuleInteractionModel rule, View blockView){
       Publisher savedBlockInfo = (Publisher)blockView.getTag();
       int numBlocks = rule.getNumberOfActionEntriesForBlockId(Integer.toString(savedBlockInfo.getBlockId()));
       savedBlockInfo.setBlockInstanceId(savedBlockInfo.getPublisherKey() + Integer.toString(numBlocks + 1));
       blockView.setTag(savedBlockInfo);
   }

   /**
   * This method returns pixel for the passed dpi values
   *
   * @param context - context
   * @param dpi - values to be converted
   * @return int - values in pixel
   */
   private static int getPixelsOfDpi(Context context,float dpi){
       float scale = context.getResources().getDisplayMetrics().density;
       return (int) (dpi * scale + 0.5f);
   }


    public interface EditType {
        final String ADDED 	= "added";
        final String EDITED = "edited";
        final String DELETED = "deleted";
        final String UNKNOWN = "unknown";
        final String PROCESSED = "processed";
    }

}
