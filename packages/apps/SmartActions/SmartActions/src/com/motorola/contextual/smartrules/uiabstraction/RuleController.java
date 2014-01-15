package com.motorola.contextual.smartrules.uiabstraction;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.uipublisher.ActionPublisherFilterList;
import com.motorola.contextual.smartrules.uipublisher.ActionPublisherList;
import com.motorola.contextual.smartrules.uipublisher.ConditionPublisherFilterList;
import com.motorola.contextual.smartrules.uipublisher.ConditionPublisherList;
import com.motorola.contextual.smartrules.uipublisher.IRulesBuilderPublisher;
import com.motorola.contextual.smartrules.uipublisher.Publisher;
import com.motorola.contextual.smartrules.uipublisher.PublisherList;

/**
* RuleController  bridges the Activity/Context and UI to RuleInteractionModel
* UI will make calls to the controller which delegates the persistent level
* call to the RuleInteractionModel
*
* Each Rule will have a
* 			RuleInteractionModel, composition class with Rule instance
* 			View/UI represented by the Activity loading the Rule
* 			RuleController
*
*<code><pre>
* CLASS:
*
*
* RESPONSIBILITIES:
* 	bridges the UI interactions to the business-layer of a Rule.
*   Create new rule, open existing rule
*   Switch Rule into Edit mode listening from Action/Condition controllers
*
* COLABORATORS:
* 	RuleInteractionModel - implements the persistence part of the Action, extends RuleTuple
* 	ActionController - implements the business layer of the individual Actions.
* 	ConditionController - implements the business layer of the individual Conditions.
* 	Action - implements the business layer of the individual Actions.
* 	Condition - implements the business layer of the individual Conditions.
*
* USAGE:
* 	See each method.
*
*</pre></code>
*/
public class RuleController implements IRulesBuilderPublisher, IRuleUserInteraction,
										Constants{
	private Context						mContext = null;
	private RuleInteractionModel       	mRuleModel = null;
	private IEditRulePluginCallback		mEditRuleCallback;
	private ConditionPublisherList 				mCondPubList;
	private ActionPublisherList 				mActPubList;
	private ConditionController 				mCondController;
	private ActionController					mActController;
	private SaveRuleFromUserInterface 	mSaveRule;

	public RuleController(Context context, Rule rule, boolean isCopy){
		mContext = context;
		setRuleModel(rule, isCopy);
		initRuleController();

	}

	/**
	 * Loads the publisher related info from list of available publishers
	 */
	private void initRuleController(){
		mCondPubList = new ConditionPublisherList(mContext);
		mActPubList = new ActionPublisherList(mContext);

	}

	/**
	 * Initializes the RuleInteractionModel which is the entity holding all the info
	 * related to Rule components(Triggers/Actions) when it is loaded or is being created
	 * @param rule
	 * @param isCopy
	 */
	private void setRuleModel(Rule rule, boolean isCopy){
		mRuleModel = new RuleInteractionModel(rule);
		mRuleModel.setIsCopyFlag(isCopy);
	}

	/**
	 * Returns the RuleModel which holds the ActionInteraction and
	 * ConditionInteraction models
	 * @return
	 */
    public RuleInteractionModel getRuleModel() {
		return mRuleModel;
	}

    /**
     * Returns the callback set earlier during loading of Rule
     * @return
     */
	public IEditRuleGenericCallback getBlockLayerCallback() {
        return mEditRuleCallback;
    }

	/**
	 * Registers callback to View from Model
	 * Also inits the Action and Condition controllers
	 *
	 * @param blockLayerCallback
	 */
    public void setBlockLayerCallback(IEditRulePluginCallback blockLayerCallback) {
        this.mEditRuleCallback = blockLayerCallback;
        mCondController = new ConditionController(mContext, this ,mEditRuleCallback);
        mActController = new ActionController(mContext, this ,mEditRuleCallback);
    }

	public void loadRule() {
		LoadRuleInUserInterface loadRule = new LoadRuleInUserInterface(mContext, mRuleModel);
		loadRule.initLoad(mEditRuleCallback, mActPubList, mCondPubList);
	}

	public void saveRule(boolean isManual) {
		mSaveRule = new SaveRuleFromUserInterface(mContext, mRuleModel);
		mSaveRule.initSave(mEditRuleCallback, isManual);
	}

	public void editActiveRule(){
		mSaveRule.startThreadToeditExistingRuleFromDB(mRuleModel.getRuleInstance().get_id());
	}

	public ActionPublisherList fetchActionPublisherList() {
		return mActPubList;
	}

	public ConditionPublisherList fetchConditionPublisherList() {
		return mCondPubList;
	}

	public PublisherList fetchActionListToGrey(ActionPublisherList actionPubList) {
		ActionPublisherFilterList availActPubList = new ActionPublisherFilterList(mContext, mRuleModel, actionPubList);
		return availActPubList.getGreyListOfActions();
	}

	public PublisherList fetchConditionListToGrey(ConditionPublisherList conditionPubList) {
		ConditionPublisherFilterList availCondPubList = new ConditionPublisherFilterList(mContext, mRuleModel, conditionPubList);
		return availCondPubList.getGreyListOfConditions();
	}

	public void addActionPublisherInRule(Intent actPubData) {
		mActController.processSelectedActionListItem(actPubData);

	}

	public void addConditionPublisherInRule(Intent condPubData) {
		mCondController.processSelectedConditionListItem(condPubData);

	}

	public void removeExistingActionFromRule(View actPub) {
		mActController.removeAction(actPub);

	}

	public void removeExistingConditionFromRule(View condPub) {
		mCondController.removeCondition(condPub);

	}

	public void configureActionInRule(Intent actInt, ActionInteractionModel mLastAction) {
		mActController.configureAction(actInt, mLastAction);
	}

	public void configureConditionInRule(Intent condInt, ConditionInteractionModel mLastCondition) {
		mCondController.configureCondition(condInt, mLastCondition);
	}

	public void changeRuleName(String newName) {
		// TODO Auto-generated method stub

	}

	public void changeRuleIcon(String resId) {
		// TODO Auto-generated method stub

	}

	public void enablePublisher(Publisher pub) {
		// TODO Auto-generated method stub

	}

	public void disablePublisher(Publisher pub) {
		// TODO Auto-generated method stub

	}


}
