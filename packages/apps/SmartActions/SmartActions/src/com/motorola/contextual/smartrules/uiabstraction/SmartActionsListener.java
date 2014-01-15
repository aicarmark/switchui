/*
 * @(#)SmartActionsListener.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2012/19/03 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.uiabstraction;

import java.util.List;

import android.content.Context;

/** This class is a listener interface that can be used to call into the UI layer from
 *  the UI Abstraction layer.
 * 
 *<code><pre>
 * CLASS:
 *  None.
 *
 * RESPONSIBILITIES:
 * 	provide the call backs for the UI layer to implement
 *
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public interface SmartActionsListener {
	
	/** class to hold the rule state change event element
	 */
	public class RuleStateChangeEvent {
		String key;
		// @see SmartActionsListInterface.RuleState
		int oldState;
		// @see SmartActionsListInterface.RuleState
		int newState;
	}
	
	/** class to hold the conflict stack change event element
	 */
	public class ConflictStackChangeEvent {
		String publisherKey;
		List<ConflictItem> item;
	}
	
	/** this is fired when a rule state changes.
	 * 
	 * NOTE: THIS IS YET TO BE IMPLEMENTED AND NOT AVAILBLE IN THE CURRENT RELEASE.
	 * 
	 * @param context - context
	 * @param event - RuleStateChangeEvent element
	 */
	public void onRuleStateChanged(Context context, RuleStateChangeEvent event);
	
	/** this is fired when a conflict stack state is updated.
	 * 
	 * NOTE: THIS IS YET TO BE IMPLEMENTED AND NOT AVAILBLE IN THE CURRENT RELEASE.
	 *
	 * @param context - context 
	 * @param event - ConflictStackChangeEvent element
	 */
	public void onConflictStackUpdate(Context context, ConflictStackChangeEvent event);
}