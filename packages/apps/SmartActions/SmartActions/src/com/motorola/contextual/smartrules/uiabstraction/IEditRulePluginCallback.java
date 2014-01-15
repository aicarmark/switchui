package com.motorola.contextual.smartrules.uiabstraction;

import com.motorola.contextual.smartrules.uipublisher.ActionPublisherList;
import com.motorola.contextual.smartrules.uipublisher.ConditionPublisherList;
import com.motorola.contextual.smartrules.uipublisher.Publisher;

import android.content.Intent;
import android.view.View;


/**
 * This class should be implemented by EditRuleActivity to facilitate callback between
 * classes representing functionality through RuleController
 * Using this will help keep the Activity based implementation in the Activity class
 * and the Controller will have the callback instance which it can share to
 * be used from member objects.
 * This will have the callbacks to implement Plugin UI functionality in the main activity.
 * Currently this supports Blocks based UI
 */

public interface IEditRulePluginCallback extends IEditRuleGenericCallback{


	/**
	 * Loads the Plugin Layouts/views and related params
	 * If using a separate plugin UI design this part needs to be modified
	 */
	public void loadPluginRuleViews(RuleInteractionModel model,
										ActionPublisherList acList, ConditionPublisherList cdList);

	/**
	 * Takes care on Plug-in UI side corresponding to adding from Condition publisher list
	 */
	public View processPluginSelectedConditionListItem(Publisher condPubInfo, Intent data);

	/**
	 * Takes care on Plug-in UI side corresponding to adding from Action publisher list
	 */
	public View processPluginSelectedActionListItem(Publisher condPubInfo, Intent data);

	/**
	 * Handles all the View related functionality that is specific to
	 * inner UI Action components , Condition/Actions which are specific to
	 * UI implementation and can be replaceable as a Plugin-UI.
	 */
	public void postConfigurePluginActionBlock();

	/**
	 * Handles all the View related functionality that is specific to
	 * inner UI Condition components , Condition/Actions which are specific to
	 * UI implementation and can be replaceable as a Plugin-UI.
	 */
	public void postConfigurePluginConditionBlock();
}
