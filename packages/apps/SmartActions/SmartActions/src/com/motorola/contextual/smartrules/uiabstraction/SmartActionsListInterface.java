/*
 * @(#)SmartActionsListInterface.java
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

import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.table.RuleTable;

import android.content.Context;
import android.content.Intent;

/** This is an interface class that would be implemented by the UI Abstraction layer.
 * 
 *<code><pre>
 * CLASS:
 *  None.
 *
 * RESPONSIBILITIES:
 *  None.
 *  
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	None.
 *</pre></code>
 */
public abstract interface SmartActionsListInterface {
		
	/** interface that can be used to fetch different types of list items.
	 */
	/* public interface ListFilter {
		int LANDING_PAGE = 0; // Will return all Auto, Manual, Active, Ready and Disabled.
		int SAMPLES = 1 << 1;
		int SUGGESTIONS = 1 << 2;
		int INVISIBLE = 1 << 3;
		int AUTO = 1 << 4;
		int MANUAL = 1 << 5;
		int ACTIVE = 1 << 6;
		int READY = 1 << 7;
		int DISABLED = 1 << 8;
	}
	
	public interface ListFilterShift {
		int SHIFT_AUTO = 0;
		int SHIFT_MANUAL = 1;
		int SHIFT_ACTIVE = 2;
		int SHIFT_READY = 3;
		int SHIFT_DISABLED = 4;
		int SHIFT_SAMPLES = 5;
		int SHIFT_SUGGESTIONS = 6;
		int SHIFT_MAX = 7;
	} */
	
	/** interface to provide the kind of suggestions that needs to be returned.
	 */
	public interface SuggestionFlag extends RuleTable.SuggState {}
	
	/** interface of options that can be shown via the hard key menu.
	 */
	public interface MenuType {
		int INVALID = -1;
		/** Corresponds to the 'Check status' menu item */
		int CHECK_STATUS = 1;
		/** Corresponds to the 'Disable all' menu item */
		int DISABLE_ALL = CHECK_STATUS + 1;
		/** Corresponds to the 'My profile' menu item */
		int MY_PROFILE = DISABLE_ALL + 1;
		/** Corresponds to the 'About' menu item */
		int ABOUT = MY_PROFILE + 1;
		/** Corresponds to the 'Help' menu item */
		int HELP = ABOUT + 1;
		/** Corresponds to the 'Settings' menu item */
		int SETTINGS = HELP + 1;
		/** Corresponds to the 'Add (+)' button in action bar */
		int ADD_BUTTON = SETTINGS + 1;
		/** Corresponds to the 'Copy an existing rule' menu item */
		int COPY_RULE = ADD_BUTTON + 1;
	}
	
	/** interface of possible rule states.
	 */
	public interface RuleState {
		/** implies the rule is enabled and active */
		int ENABLED = 0; 
		/** implies the rule is disabled and inactive */
		int DISABLED = 1;
		/** implies the rule is in ready state (only for Automatic rules) */
		int READY = 2;
	}

	/** fetches the rules based on the list filter and returns a list of Rule elements.
	 *  TODO: Implement in second phase
	 *  
	 * @param context - context
	 * @param flags - @see SmartActionsListInterface.ListFilter
	 * @return a list of Rule elements
	 */
	//public abstract List<Rule> fetchRulesList(Context context, int flags);

	/** fetches all the rules visible to the user in the Landing Page
	 * 
	 * @param context - context
	 * @return - a list of Rule instances
	 */
	public abstract List<Rule> fetchLandingPageRulesList(Context context);
	
	/** fetching the suggestions based on the flags (Could be all, only new, only read etc)
	 * 
	 * @param context - context
	 * @return - list of requested Suggestions as Rule instances 
	 */
	public abstract List<Rule> fetchSuggestionsList(Context context, int suggFlags);
	
	/** fetches the list of Sample rules
	 * 
	 * @param context - context
	 * @return - list of Samples as Rule instances
	 */
	public abstract List<Rule> fetchSamplesList(Context context);

	/** fetches a rule
	 * 
	 * @param context - context
	 * @param _id - rule ID
	 * @return - returns a Rule instance for the rule ID passed
	 */
	public abstract Rule fetchRule(Context context, long _id);
	
	/** disables all the rules that are shown on the landing page
	 * 
	 * @param context - context
	 * @return true or false
	 */
	public abstract boolean disableAllRules(Context context);
	
	
	/** constructs an intent for the menu selection and returns it.
	 * 
	 * @param context - context
	 * @param itemType - @see SmartActionsListInterface.MenuType
	 * @return an intent
	 */
	public abstract Intent fetchMenuIntent(Context context, int itemType);
	
	/** fetches the actions conflict stack and returns it to the caller
	 * 
	 * @param context - context
	 * @return list of ConflitItem elements
	 */
	public abstract List<ConflictItem> fetchConflictWinnersList(Context context);
	
	/** fetches the conflict stack for the action publisher key passed in. The winner is at
	 * the top of the list and the bottom is the "default" value.
	 * 
	 * @param context - context
	 * @param actionPubKey - action publisher key
	 * @return list of ConflictItem elements for the action publisher key
	 */
	public abstract List<ConflictItem> fetchPublisherConflictStack(Context context, String actionPubKey);
}