/*
 * @(#)IRulesBuilderPublisher.java
 *
 * (c) COPYRIGHT 2009 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * E51185        2012/04/03  NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.uipublisher;

/**
 * Interface to expose the RulesBuilder utility for UI related to any specific rule
 * and which exposes utility functions to provide basic functionality for any rule
 *
 */
public interface IRulesBuilderPublisher {

	/**
	 * This would return all the possible Action publishers supported by SmartActions
	 * @param rule_id
	 * @return PublisherList list of Action publishers
	 */
	public ActionPublisherList fetchActionPublisherList();

	/**
	 * This would return all the possible Conditions publishers supported by SmartActions
	 * @param rule_id
	 * @return PublisherList list of Action publishers
	 */
	public ConditionPublisherList fetchConditionPublisherList();

	/**
	 * Fetches the actions list that need to be grayed/stubbed out
	 * Helps in showing Actions that can be added to an existing rule/blank rule
	 * @param rule_id rule_id of rule or null in case of blank rule
	 * @return PublisherList List of ActionPublisher to be grayed
	 */
	public PublisherList fetchActionListToGrey(ActionPublisherList actionPubList);

	/**
	 * Fetches the conditions list that need to be grayed/stubbed out
	 * Helps in showing Conditions that can be added to an existing rule/blank rule
	 * @param rule_id rule_id of rule or null in case of blank rule
	 * @return PublisherList List of ConditionPublisher to be grayed
	 */
	public PublisherList fetchConditionListToGrey(ConditionPublisherList conditionPubList);


}
