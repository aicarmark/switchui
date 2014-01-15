/*
 * @(#)Suggestions.java
 *
 * (c) COPYRIGHT 2010 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2011/04/09 NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.suggestions;


import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.app.LandingPageActivity.LandingPageIntentExtras;
import com.motorola.contextual.smartrules.app.WebViewActivity;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema.ConditionTableColumns;
import com.motorola.contextual.smartrules.db.Schema.RuleTableColumns;
import com.motorola.contextual.smartrules.db.business.Action;
import com.motorola.contextual.smartrules.db.business.ConditionPersistence;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.business.SuggestionsPersistence;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.ConditionTable.Columns;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.RuleTable.RuleType;
import com.motorola.contextual.smartrules.publishermanager.RulesValidatorInterface;
import com.motorola.contextual.smartrules.rulesimporter.XmlConstants.ImportType;
import com.motorola.contextual.smartrules.util.Util;

/**
 * This class utility functions related to Suggestions framework
 * 
 * <code><pre>
 * CLASS:
 *  extends Activity
 *
 *  implements
 *      Constants - Contain common constants to be used
 *      DbSyntax - DB related constants
 *
 * RESPONSIBILITIES:
 *  See each method
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 *  See each method.
 * </pre></code>
 */

public class Suggestions implements Constants, DbSyntax {

    private static final String TAG = Suggestions.class.getSimpleName();

    public static final String DISPLAY_TITLE    = PACKAGE + ".displaytitle";
    public static final String WHERE_CLAUSE     = PACKAGE + ".whereclause";
    public static final String EXTERNAL_LAUNCH  = PACKAGE + ".external";
    public static final String FIRST_REJECT  = PACKAGE + ".rejected";

    /**
     * Launches  suggestion inbox
     *
     * @param context
     */
    public static void showSuggestionsInbox(Context context) {

        // Always show Suggestions Inbox
        Intent intent = new Intent(context, SuggestionsInboxActivity.class);
        intent.putExtra(DISPLAY_TITLE,
                context.getString(R.string.suggestions));
        intent.putExtra(WHERE_CLAUSE, SuggestionsPersistence.WHERE_CLAUSE_ALL);

        ((Activity)context).startActivityForResult(intent, RULE_SUGGESTED);
    }

    /**
     * Open Suggestions Detail Activity
     *
     * @param context - context
     * @param ruleId - Rule ID of the Suggestion
     */
    public static void showSuggestionDialog(Context context, long ruleId){

        Intent sIntent = new Intent(context, SuggestionDialogActivity.class);
        sIntent.putExtra(PUZZLE_BUILDER_RULE_ID, ruleId);

        ((Activity)context).startActivityForResult(sIntent, RULE_SUGGESTED);
    }

    /**
     * Enables/adds the suggestion as a new Rule in MM DB set ENABLED == true set FLAG == none (not
     * as suggested or invisible)
     *
     * @param ruleId: ID of the suggestion to be converted to a Rule
     */
    public static void addSuggestionAsRule(Context ct, long ruleId) {

       // Is this a sample? then clone it to create new rule
       long newRuleId;
       int source = RulePersistence.getColumnIntValue(ct, ruleId, RuleTableColumns.SOURCE);

       if( source == RuleTable.Source.FACTORY)
            newRuleId = createSampleChild(ct, ruleId);
       else
            newRuleId = ruleId;

        // remove sample/user rule from inbox
        SuggestionsPersistence.setSuggestionState(ct, ruleId, RuleTable.SuggState.ACCEPTED);

        // Accept the newly inserted rule
        SuggestionsPersistence.acceptSuggestion(ct, newRuleId);

        String ruleKey = RulePersistence.getRuleKeyForRuleId(ct, newRuleId);
        RulesValidatorInterface.launchModeAd(ct, ruleKey, 
                ImportType.IGNORE, 
                RuleTable.Validity.VALID, 
                RulePersistence.fetchRuleOnly(ct, ruleKey),
                RulePersistence.isRulePsuedoManualOrManual(ct, ruleKey));
        
        // Launch RulesValidator
   /*     Intent rvIntent =  new Intent(ACTION_RULES_VALIDATE_REQUEST);
        ArrayList<String> ruleList = new ArrayList<String>();
        ruleList.add(RulePersistence.getRuleKeyForRuleId(ct, newRuleId));
        rvIntent.putExtra(EXTRA_RULE_LIST, ruleList);
        rvIntent.putExtra(EXTRA_REQUEST_ID, String.valueOf(newRuleId));
        ct.sendBroadcast(rvIntent);
*/
        // This is needed by LandingPage activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra(LandingPageIntentExtras.RULE_ID_INSERTED, ruleId);
        ((Activity)ct).setResult(Activity.RESULT_OK, resultIntent);
    }

    /**
     * Returns if this is the first suggestion rejection
     *
     * @param ct - context
     * @return - true or false
     */
    public static boolean getFirstRejectState(Context ct) {
        return Util.getSharedPrefStateValue(ct, SUGGESTIONS_FIRST_NO_PREF, TAG);
    }

    /**
     * Sets the rejection preference state
     *
     * @param ct - context
     * @param state - true or false
     */
    public static void setFirstRejectState(Context ct, boolean state) {
        Util.setSharedPrefStateValue(ct, SUGGESTIONS_FIRST_NO_PREF, TAG, state);
    }

    /**
     * Returns if this is the first launch or not
     *
     * @param ct - context
     * @return - true or false
     */
    public static boolean isInitState(Context ct) {
        return Util.getSharedPrefStateValue(ct, SUGGESTIONS_LAUNCH_PREF, TAG);
    }

    /**
     * Sets if this is the first launch or not
     *
     * @param ct - context
     * @param state - true or false
     */
    public static void setInitState(Context ct, boolean state) {
        Util.setSharedPrefStateValue(ct, SUGGESTIONS_LAUNCH_PREF, TAG, state);
    }

    /**
     * Set the state of notification - being shown or not
     *
     * @param ct - context
     * @param shown - true = notification shown
     */
    public static void setNotificationShow(Context ct, boolean shown) {
        Util.setSharedPrefStateValue(ct, SUGGESTIONS_NOTIFICATION_PREF, TAG, shown);
    }

    /**
     * Returns if notification is being shown
     *
     * @param ct - context
     * @return - true = notification shown
     */
    public static boolean isNotificationbeingShown(Context ct){
        return Util.getSharedPrefStateValue(ct, SUGGESTIONS_NOTIFICATION_PREF, TAG);
    }

    /**
     * Updates the unread suggestion count in the notification bar
     *
     * @param ct - context
     */
    public static void updateNotification(Context ct) {
        int unreadCount = SuggestionsPersistence.getUnreadCount(ct);
        if (LOG_INFO)
            Log.i(TAG, "Unread count = " + unreadCount);

        SuggestionsNotificationService.showSuggestionNotification(ct, unreadCount, -1);
    }

    /**
     * Removes Suggestion notification 
     *
     * @param context - context
     */
    public static void removeNotification(Context context){

        if(isNotificationbeingShown(context)){
            // Remove notification bar
            SuggestionsNotificationService.showSuggestionNotification(context, 0, RuleTable.RuleType.DEFAULT);

            if(LOG_INFO) Log.i(TAG,"Suggestion Notification Removed!");
        }
    }

    /**
     * Action are fired instantaneously without being added as rules
     *
     * @param context - context
     * @param ruleId - Suggestion ID 
     */
    public static void fireInstantActions(Context context, long ruleId) {

        String modeName = null;
        String ruleKey = null;

        // read Rule table
        Cursor ruleCursor = SuggestionsPersistence.getSuggestedRuleCursor(context, ruleId);

        if (ruleCursor == null) {
            Log.e(TAG, "NULL ruleCursor in fireInstantActions");
            return;
        } 

        try {
            if (ruleCursor.moveToFirst()) {
                // read the values
                modeName = ruleCursor.getString(ruleCursor.getColumnIndexOrThrow(RuleTable.Columns.NAME));
                ruleKey = ruleCursor.getString(ruleCursor.getColumnIndexOrThrow(RuleTable.Columns.KEY));
            } else {
                Log.e(TAG, "Empty ruleCursor in fireInstantActions");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ruleCursor.close();
        }

        // read the actions
        Cursor actionCursor = SuggestionsPersistence.getSuggestedActionCursor(context, ruleId);
        if (actionCursor == null) {
            Log.e(TAG, "NULL actionCursor in fireInstantActions");
        } else {
            try {
                if (actionCursor.moveToFirst() && actionCursor.getCount() > 0) {
                    do {
                        int accCol = actionCursor.getColumnIndexOrThrow(ActionTable.Columns.SUGGESTED_STATE);
                        if (Integer.parseInt(actionCursor.getString(accCol)) == RuleTable.SuggState.ACCEPTED) {
                            // get the URI TO FIRE ACTION
                            String actionPublisherKey = actionCursor.getString(actionCursor.getColumnIndexOrThrow(ActionTable.Columns.ACTION_PUBLISHER_KEY));
                            String config = actionCursor.getString(actionCursor.getColumnIndexOrThrow(ActionTable.Columns.CONFIG));
                            long id = actionCursor.getLong(actionCursor.getColumnIndex(Columns._ID));
                            // This should fire the action
                            Action.sendBroadcastIntentForAction(context, // context
					actionPublisherKey, //publisher key
                                    ruleKey, // ruleKey of the rule that is currently processed
                                    modeName, // modeName of the rule that is currently processed
                                    false, // saveDefault
                                    actionPublisherKey, // actionPublisherKey
                                    COMMAND_FIRE, // Fire command
                                    config, // action state value to be set
                                    id); // id
                        }
                    } while (actionCursor.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                actionCursor.close();

                // delete this suggestion
                RulePersistence.deleteRule(context, ruleId, null, null, false);
            }
        }
    }

    /**
     * Accept the Rule if it is a Suggestion
     *
     * @param ct
     * @param ruleId
     */
    public static void verifyAndAcceptSuggestion(Context ct, final long ruleId){

        if(ruleId == RuleType.DEFAULT) return;

        // Is the rule a suggestion?
        if(SuggestionsPersistence.isSuggestedType(ct, ruleId)){

            // Update fields to accept it as a rule
            SuggestionsPersistence.acceptSuggestion(ct, ruleId);

            // remove notification, if any
            removeNotification(ct);
        }
    }

    /**
     * starts help/about screen activity
     * 
     * @param ct - context
     */
    public static  void startHelpAboutActivity(Context ct){
        String helpUri = Util.getHelpUri(ct);
        Intent intent = new Intent(ct, WebViewActivity.class);
        intent.putExtra(WebViewActivity.REQUIRED_PARMS.CATEGORY, WebViewActivity.WebViewCategory.HELP);
        intent.putExtra(WebViewActivity.REQUIRED_PARMS.LAUNCH_URI, helpUri);
        ct.startActivity(intent);
    }

    /**
     * This creates a Clone of the Sample, it is used to create a child
     * rule from sample
     *
     *
     * @param context - Context
     * @param sampleRuleId - Rule ID of the sample rule
     * @return - new rule ID
     */
    public static long createSampleChild(Context context, final long sampleRuleId){
    	
    	if (LOG_INFO) Log.i(TAG,"createSampleChild");

        long newId = RuleTable.RuleType.DEFAULT;
        String sampleRuleKey = RulePersistence.getRuleKeyForRuleId(context, sampleRuleId);
        String[] columns = new String[] {RuleTableColumns.NAME, RuleTableColumns.ADOPT_COUNT};
        Cursor ruleCursor = RulePersistence.getRuleCursor(context, sampleRuleKey, columns);

        if(ruleCursor == null) return newId;

        try{
            if(ruleCursor.moveToFirst()){
                String newRuleName  = ruleCursor.getString(ruleCursor.getColumnIndexOrThrow(RuleTableColumns.NAME));
                int childCount = ruleCursor.getInt(ruleCursor.getColumnIndexOrThrow(RuleTableColumns.ADOPT_COUNT));

                newRuleName = RulePersistence.createClonedRuleName(newRuleName, childCount);
                String newRuleKey = RulePersistence.createClonedRuleKeyForSample(sampleRuleKey);

                newId = RulePersistence.cloneRule(context, sampleRuleId, newRuleKey, newRuleName, false);  

                if(newId == DEFAULT_RULE_ID)
                    Log.e(TAG, "Rule clone failed for ruleKey: " + newRuleKey);
                else{
                    RulePersistence.setAdoptCount(context, sampleRuleId, childCount + 1);
                    DebugTable.writeToDebugViewer(context, DebugTable.Direction.OUT, null, null,
                									newRuleKey, SMARTRULES_INTERNAL_DBG_MSG,
                									sampleRuleKey, Constants.RULEKEY_UPDATED,
                									Constants.PACKAGE, Constants.PACKAGE);
                }
            }
        }catch(IllegalArgumentException e){
            Log.e(TAG, "addSuggestionAsRule: Null cursor");
            return newId;
        } finally {
            ruleCursor.close();
        }

        return newId;
    }

    /**
     * Enable the new and delete the old condition
     */
    public static void swapAcceptCondition(Context context, final long ruleId){

        Cursor condCursor = ConditionPersistence.getConditionCursor(context, ruleId);

        if(condCursor == null || !condCursor.moveToFirst()){
            if(condCursor != null) condCursor.close();
            Log.e(TAG, "swapCondition: null cursor");
            return;
        }

        try{
            do{
                int col = condCursor.getColumnIndexOrThrow(ConditionTable.Columns.SUGGESTED_STATE);
                int state = condCursor.getInt(col);

                if(state != RuleTable.SuggState.ACCEPTED){
                    col = condCursor.getColumnIndexOrThrow(ConditionTable.Columns.FAILURE_MESSAGE);
                    String swapPubKey = condCursor.getString(col);

                    // delete the old condition
                    String whereClause = ConditionTable.Columns.PARENT_FKEY + EQUALS + ruleId + AND
                            + ConditionTable.Columns.CONDITION_PUBLISHER_KEY + EQUALS + Q + swapPubKey + Q;

                    ConditionPersistence.deleteCondition(context, whereClause);

                    ContentValues cv = new ContentValues();
                    cv.put(ConditionTableColumns.ENABLED, RuleTable.Enabled.ENABLED);
                    cv.put(ConditionTableColumns.SUGGESTED_STATE, RuleTable.SuggState.ACCEPTED);

                    whereClause = Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q;
                    ConditionPersistence.updateCondition(context, cv, whereClause);

                    SuggestionsPersistence.setSuggestionState(context, ruleId, RuleTable.SuggState.ACCEPTED);

                }
            }while(condCursor.moveToNext());
        }catch (IllegalArgumentException e) {
            Log.e(TAG, "swapCondition: IllegalArgumentException");
        }
    }

    /**
     * Delete the new suggested condition
     * 
     * @param context - context
     * @param ruleId - Rule ID
     */
    public static void swapRejectCondition(Context context, final long ruleId){

        /*
         * First fetch the condition _ID
         */
        String whereClause = ConditionTable.Columns.PARENT_FKEY + EQUALS + ruleId + AND
                           + ConditionTable.Columns.SUGGESTED_STATE + NOT_EQUAL + Q
                           + RuleTable.SuggState.ACCEPTED + Q;

        // Delete the condition
        ConditionPersistence.deleteCondition(context, whereClause);

        SuggestionsPersistence.setSuggestionState(context, ruleId, RuleTable.SuggState.ACCEPTED);
    }

}
