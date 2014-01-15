package com.motorola.contextual.smartrules.uiabstraction;

import com.motorola.contextual.smartrules.uipublisher.Publisher;

import android.content.Intent;
import android.view.View;


/**
 * This interface defines the main interaction to be supported
 * at the Rule level for any UI interaction.
 * Loading, Saving a Rule
 * Configuring/Re-configuring, Adding, Removing publishers to Rule
 *
 * Typically this has to be implemented in a Model-View-Controller pattern
 * and the Controller should be the one implementing this interface, and
 * delegating the Model to the use-case specific implementation driving the
 * view/UI
 */

public interface IRuleUserInteraction {

	/**
	 * Add an ActionPublisher as an Action in the Rule
	 * This would take care of transforming from ActionPublisher
	 * to Action entity which can be added to a Rule
	 * @param actPub
	 * @return
	 */
	public void  addActionPublisherInRule(Intent actPub);

	/**
	 * Add an ConditionPublisher as an Condition in the Rule
	 * This would take care of transforming from ConditionPublisher
	 * to Condition entity which can be added to a Rule
	 * @param condPub
	 * @return
	 */
	public void addConditionPublisherInRule(Intent condPub);

	/**
	 * Remove the Action from the existing Rule
	 * @param actionInst
	 */
	public void removeExistingActionFromRule(View actPub);

	/**
	 * Remove the Condition from the existing Rule
	 * @param actionInst
	 */
	public void removeExistingConditionFromRule(View condPub);

	/**
	 * Save all the edits done in the edit session
	 * Any additions / deletions to Actions/Conditions
	 * and any re-configuration, enabling/disabling would all
	 * be persisted with the Rule instance into the DB
	 *
	 * @param isManual
	 */
	public void saveRule(boolean isManual);

	/**
	 * Configures/Re-configures an existing Action
	 * @param actionInst
	 * @return
	 */
	public void configureActionInRule(Intent actionInst, ActionInteractionModel mLastAction);

	/**
	 * Configures/Re-configures an existing condition
	 * @param conditionInst
	 * @return
	 */
	public void configureConditionInRule(Intent conditionInst, ConditionInteractionModel mLastCond);

	/**
	 * Load a Rule into the UI for any edits
	 * @param ruleInst
	 */
	public void loadRule();

	/**
	 * Change Icon representing the rule
	 * @param ruleInst
	 * @param newName
	 */
	public void changeRuleName(String newName);

	/**
	 * Change the name of the Rule
	 * @param ruleInst
	 * @param resId  This could change, needs discussion
	 */
	public void changeRuleIcon( String resId);

	/**
	 * Enables or considers the publisher to be part of the Rule
	 * @param ruleInst
	 * @param pub
	 */
	public void enablePublisher(Publisher pub);

	/**
	 * Disables or ignore the publisher to be part of the Rule
	 * Even though the Publisher is part of the Rule entity
	 * in disabled state it should not contribute to any logic
	 * for the Rule
	 *
	 * @param ruleInst
	 * @param pub
	 */
	public void disablePublisher(Publisher pub);

}
