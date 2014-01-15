package com.motorola.contextual.smartrules.uiabstraction;

import android.view.View;

/**
 * This class should be implemented by Activity loading a Rule to facilitate callback between
 * classes representing functionality through RuleController
 * Using this will help keep the Activity based implementation in the Activity class
 * and the Controller will have the callback instance which it can share to
 * be used from member objects.
 * This specific Interface will have the callback to implement Generic functionality only in the main activity.
 * Any functionality related to Plugin-UI should be done in EditRulePluginCallbackInterface
 * which will be an extension of this interface
 *
 * Call Direction: From Controller to View/Activity
 */
public interface IEditRuleGenericCallback {

	/**
	 * Loads the generic Layouts/views and related params
	 * This would be Views such as Rule name/icon/status
	 * This will implement UI parts which are NOT specific
	 * to the plugin UI framework
	 */
	public void loadGenericRuleViews();

	/**
	 * This function gets called whenever there is user activity on the screen, like renaming rule name, selection different rule icon,
	 * adding/deleting/reconfiguring/connecting-disconnecting blocks, so that the 'Save' option is enabled.
	 * @param userEdited
	 */
	public void enterEditMode(boolean userEdited);

	/**
	 * Takes care on Plug-in UI side corresponding to adding from Condition publisher list
	*/
	public void processGenericSelectedConditionListItem();

	/**
	 * Takes care on Plug-in UI side corresponding to adding from Action publisher list
	*/
	public void processGenericSelectedActionListItem();

	/**
	 * Takes care of doing generic stuff, like marking to persist deletion upon
	 *  removal of a Publisher from the Rule
	 */
	public void removeGenericPublisher(View pub);

	/**
	 * Takes care of removing Visual elements related to 
	 * removal of a Publisher from the Rule
	 */
	public void removePluginPublisher(View pub);
	
	/**
	 * Configure / Re-configures the Action Publisher part of the Rule
	 * @param pubView
	 */
	public void invokeActionActivityToConfigure(View pubView);

	/**
	 * Configure / Re-configures the Condition Publisher part of the Rule
	 * @param pubView
	 */
	public void invokeConditionActivityToConfigure(View pubView);

	/**
	 * Handles all the View related functionality that is generic or specific to
	 * outer UI Action components , such as Save, Rule status, etc
	 * Any UI related to Rule components Condition/Actions which are specific to
	 * UI implementation must be handled in postConfigurePluginActionBlock
	 */
	public void postConfigureGenericActionBlock();

	/**
	 * Handles all the View related functionality that is generic or specific to
	 * outer UI Condition components , such as Save, Rule status, etc
	 * Any UI related to Rule components Condition/Actions which are specific to
	 * UI implementation must be handled in postConfigurePluginActionBlock
	 */
	public void postConfigureGenericConditionBlock();

	/**
	 * Handles the use case where Rule controller delegates saving rule to
	 * SaveRuleFromUserInterface, which in turn needs to finish some UI interaction
	 * on the UI side
	 */
	public void onSaveEditWhileActive(boolean isManual);

	/**
	 * Handles taking care of all before UI is removed (Activity destroyed/finished)
	 * after saving of the edit session of the Rule
	 */
	public void onSaveFinish();
}
