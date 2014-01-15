/*
 * @(#)UiAbstractionLayer.java
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
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.app.AboutActivity;
import com.motorola.contextual.smartrules.app.AddRuleListActivity;
import com.motorola.contextual.smartrules.app.CheckStatusActivity;
import com.motorola.contextual.smartrules.app.CopyExisitingRuleActivity;
import com.motorola.contextual.smartrules.app.ProfileActivity;
import com.motorola.contextual.smartrules.app.SettingsActivity;
import com.motorola.contextual.smartrules.app.WebViewActivity;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.business.Conflicts;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.util.Util;

/** This class abstracts the SmartActionsListInterface and implements the methods that can be 
 * 	called from the UI layer.
 * 
 *<code><pre>
 * CLASS:
 *  implements
 *  	Constants - for the constants used
 *  	DbSyntax - for the DB related constant strings
 *  	SmartActionsListInterface - interface for the rule list
 *
 * RESPONSIBILITIES:
 * 	implement the methods that can be called by the UI layer.
 *
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class UiAbstractionLayer implements SmartActionsListInterface, Constants, DbSyntax {

	private static final String TAG = UiAbstractionLayer.class.getSimpleName();
		
	// TODO: Will be implemented in phase 2
	/* @Override
	public List<Rule> fetchRulesList(Context context, int flags) {
		Log.d(TAG, "Called with flags "+flags);
		
		Cursor cursor = RulePersistence.getRulesCursor(context, flags);
		if(cursor != null && cursor.moveToFirst())
			DatabaseUtils.dumpCursor(cursor);
		return null;
	} */

	public List<Rule> fetchLandingPageRulesList(Context context) {				
		return Rule.convertToRuleList(context, 
					RulePersistence.getVisibleRulesCursor(context));
	}

	public List<Rule> fetchSuggestionsList(Context context, int suggFlags) {	
		return Rule.convertToRuleList(context, 
				RulePersistence.getSuggestions(context, suggFlags)); 
	}

	public List<Rule> fetchSamplesList(Context context) {
		return Rule.convertToRuleList(context, 
				RulePersistence.getSampleRules(context)); 		
	}

	public Rule fetchRule(Context context, long _id) {
		return RulePersistence.fetchFullRule(context, _id);	
	}	

	public boolean disableAllRules(Context context) {
		if(LOG_DEBUG) Log.d(TAG, "Called to disable all rules");
		RulePersistence.disableAllRules(context);
		return true;
	}
	
	public Intent fetchMenuIntent(Context context, int itemType) {

		if(LOG_DEBUG) Log.d(TAG, "fetchMenuIntent called for menu item "+itemType);
		
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		switch (itemType) {
			case MenuType.ABOUT:
				intent.setClass(context, AboutActivity.class);
				break;
				
			case MenuType.CHECK_STATUS:	
				intent.setClass(context, CheckStatusActivity.class);
				break;
				
			case MenuType.HELP:
				intent.setClass(context, WebViewActivity.class);
				String helpUri = Util.getHelpUri(context);
		     	intent.putExtra(WebViewActivity.REQUIRED_PARMS.CATEGORY, 
		     							WebViewActivity.WebViewCategory.HELP);
		     	intent.putExtra(WebViewActivity.REQUIRED_PARMS.LAUNCH_URI, helpUri);
		     	break;
		     	
			case MenuType.MY_PROFILE:
				intent.setClass(context, ProfileActivity.class);
				break;
				
			case MenuType.SETTINGS:
				intent.setClass(context, SettingsActivity.class);
				break;
				
			case MenuType.ADD_BUTTON:
				if(RulePersistence.getVisibleEnaAutoRulesCount(context)
						< MAX_VISIBLE_ENABLED_AUTOMATIC_RULES) {
					intent.setClass(context, AddRuleListActivity.class);
				} else
					intent = null;
	        	break;
	        	
			case MenuType.COPY_RULE:
				intent.setClass(context, CopyExisitingRuleActivity.class);
				break;
				
			default:
				intent = null;
				break;
		}
		return intent;
	}

	public List<ConflictItem> fetchConflictWinnersList(Context context) {		
		Cursor cursor = context.getContentResolver().query(Schema.ACTIVE_SETTINGS_VIEW_CONTENT_URI, 
										null, null, null, null);
		return ConflictItem.convertToConflictItemList(context, cursor);
	}

	public List<ConflictItem> fetchPublisherConflictStack(Context context, String actionPubKey) {
		Cursor cursor = Conflicts.getConflictingActionsCursor(context, actionPubKey, 
										Conflicts.Type.ACTIVE_ONLY);
		return ConflictItem.convertToConflictItemList(context, cursor);
	}
}
