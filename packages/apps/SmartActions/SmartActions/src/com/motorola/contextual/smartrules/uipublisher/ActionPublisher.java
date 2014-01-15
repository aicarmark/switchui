/*
 * @(#)ActionPublisher.java
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
 * Generalizes Publisher and provides holder class for Actions
 * that can be added to a Rule
 *
 *<code><pre>
 * CLASS:
 *  This class represents a publisher representing an Action
 *
 * RESPONSIBILITIES:
* 	Uses generalization on Publisher class to extend/implement
*   any functionality specific to Action publishers
*
* COLABORATORS:
*
* USAGE:
* 	See each method.
*</pre></code>
 */

public class ActionPublisher extends Publisher {

	/**
	 * creates and initializes an ActionPublisher
	 * @param callbackList
	 */
	ActionPublisher(PublisherPresenceListener callbackList) {
		super(callbackList);
	}

	//Action publisher specific methods Not yet implemented
}
