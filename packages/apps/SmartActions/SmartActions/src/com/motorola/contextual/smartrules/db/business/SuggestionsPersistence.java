/*
 * @(#)SuggestionDetailsActivity.java
 *
 * (c) COPYRIGHT 2010 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2011/05/04 NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.db.business;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.Schema.RuleTableColumns;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.RuleTable.Columns;
import com.motorola.contextual.smartrules.db.table.RuleTable.SuggState;

/**
 * This class holds handlers that make database related operations which pertain to Suggestions
 * framework
 *
 * <code><pre>
 * CLASS:
 *  None
 *
 *  implements
 *      Constants - Contain common constants to be used
 *      DbSyntax - DB related constants
 *
 * RESPONSIBILITIES:
 *  See each method.
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 *  See each method.
 * </pre></code>
 */

public class SuggestionsPersistence implements Constants, DbSyntax {

    private static final String TAG = SuggestionsPersistence.class.getSimpleName();

    // Clause to show ALL suggestions
    public static final String WHERE_CLAUSE_ALL = RuleTable.Columns.SUGGESTED_STATE + NOT_EQUAL + Q
            + RuleTable.SuggState.ACCEPTED + Q;

    // Clause to show UNREAD suggestions
    public static final String WHERE_CLAUSE_UNREAD = RuleTable.Columns.SUGGESTED_STATE + EQUALS + Q
            + RuleTable.SuggState.UNREAD + Q;

    // Clause to show READ suggestions
    public static final String WHERE_CLAUSE_READ = RuleTable.Columns.SUGGESTED_STATE + EQUALS + Q
            + RuleTable.SuggState.READ + Q;


    /**
     * Update Rule table flags to reflect the suggestion as accepted as
     * a Rule
     *
     * @param ct
     * @param ruleId
     */
    public static void acceptSuggestion(Context ct, long ruleId){
    	acceptSuggestion(ct, ruleId, 0);
    }
 
    /** Update Rule table flags to reflect the suggestion as accepted as
     *  a Rule and also update the adopt count value.
     *
     * @param ct - context
     * @param ruleId - rule ID to update
     * @param adoptCount - the value in the ADOPT_COUNT column
     */
    public static void acceptSuggestion(Context ct, long ruleId, int adoptCount) {
    	// Only enable the rule if the rule still has valid enabled conditions.
    	// The user could have customized the rule and disconnected/deleted all the rules.
    	int enabled = ConditionPersistence.getEnabledConditionsCount(ct, ruleId) == 0 ?
    					RuleTable.Enabled.DISABLED : RuleTable.Enabled.ENABLED;
        ContentValues cv = new ContentValues();

        cv.put(RuleTable.Columns.ENABLED, enabled);
        cv.put(RuleTable.Columns.FLAGS, "");
        cv.put(RuleTable.Columns.SUGGESTED_STATE, RuleTable.SuggState.ACCEPTED);
        cv.put(RuleTable.Columns.ADOPT_COUNT, adoptCount);
        
        // update the Tuple of rule table
        ct.getContentResolver().update(Schema.RULE_TABLE_CONTENT_URI, cv,
                RuleTable.Columns._ID + EQUALS + ruleId, null);
        
  }

    /**
     * Enable the actions that have SuggState as Unread or read
     *
     * @param ct - context
     * @param ruleId - rule ID
     */
    public static void enableNewSuggestedActions(Context ct, long ruleId) {

        String whereClause = ActionTable.Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q + AND +
                             ActionTable.Columns.SUGGESTED_STATE + NOT_EQUAL + RuleTable.SuggState.ACCEPTED;

        ContentValues cv = new ContentValues();

        cv.put(ActionTable.Columns.ENABLED, ActionTable.Enabled.ENABLED);
        cv.put(ActionTable.Columns.SUGGESTED_STATE, RuleTable.SuggState.ACCEPTED);

        // This would update one or more rows in Action Table
        ct.getContentResolver().update(Schema.ACTION_TABLE_CONTENT_URI, cv, whereClause, null);
    }

    /**
     * Enable the actions associated with a rule
     *
     * @param ct - context
     * @param ruleId - rule ID
     */
    public static void enableAllSuggestedActions(Context ct, long ruleId) {

        String whereClause = ActionTable.Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q;
        ContentValues cv = new ContentValues();

        cv.put(ActionTable.Columns.ENABLED, ActionTable.Enabled.ENABLED);
        cv.put(ActionTable.Columns.SUGGESTED_STATE, RuleTable.SuggState.ACCEPTED);

        // update the Tuple of rule table
        ct.getContentResolver().update(Schema.ACTION_TABLE_CONTENT_URI, cv, whereClause, null);
    }

    /**
     * Read rule table and return the number of suggestion present there
     *
     * @param ct - Context
     * @return count
     */
    public static int getCount(Context ct) {
        int sCount = 0;

        Cursor ruleCursor = ct.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, null,
                WHERE_CLAUSE_ALL, null, null);
        if (ruleCursor == null)
            Log.e(TAG, "NULL Cursot in getAllSuggestionsCount");
        else {
            try {
                sCount = ruleCursor.getCount();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ruleCursor.close();
            }
        }

        return sCount;
    }

    /**
     * Read rule table and return the number of UNREAD suggestion present
     *
     * @param ct - Context
     * @return count
     */
    public static int getUnreadCount(Context ct) {
        int sCount = 0;

        Cursor ruleCursor = ct.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, null,
                WHERE_CLAUSE_UNREAD, null, null);
        if (ruleCursor == null)
            Log.e(TAG, "NULL Cursor in getUnreadSuggestionsCount");
        else {
            try {
                sCount = ruleCursor.getCount();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ruleCursor.close();
            }
        }

        return sCount;
    }


    /**
     * Set the Suggestion State as Read, Unread or accepted, based on
     * the supplied ruleId
     *
     * @param context - context
     * @param ruleId - Rule ID of the suggestion that needs update
     * @param state - what state? Read, Unread or accepted.
     */
    public static void setSuggestionState(Context context, long ruleId, int state) {

        final String whereClause = RuleTable.Columns._ID + EQUALS + Q + ruleId + Q;

        ContentValues cv = new ContentValues();
        cv.put(RuleTable.Columns.SUGGESTED_STATE, state);

        // update the row
        context.getContentResolver().update(Schema.RULE_TABLE_CONTENT_URI, cv, whereClause, null);
    }

    /**
     * Set the Suggestion State as Read, Unread or accepted, based on
     * the supplied ruleKey
     *
     * @param context - context
     * @param ruleKey - Rule key of the suggestion that needs update
     * @param state - what state? Read, Unread or accepted.
     */
    public static void setSuggestionState(Context context, String ruleKey, int state) {

        final String whereClause = RuleTable.Columns.KEY + EQUALS + Q + ruleKey + Q;

        ContentValues cv = new ContentValues();
        cv.put(RuleTable.Columns.SUGGESTED_STATE, state);

        // update the row
        context.getContentResolver().update(Schema.RULE_TABLE_CONTENT_URI, cv, whereClause, null);

    }

    /**
     * Get the Suggestion State as Read, Unread or accepted, based on
     * the supplied ruleKey
     *
     * @param context - context
     * @param ruleKey - Rule key of the suggestion that needs update
     * @return state - what state? Read, Unread or accepted.
     */
    public static int getSuggestionState(Context context, String ruleKey) {

        int result = SuggState.ACCEPTED;

        Cursor ruleCursor = getSuggestedRuleCursor(context,
                RulePersistence.getRuleIdForRuleKey(context, ruleKey));

        if(ruleCursor == null) return result;

        try{
            int sourceCol = ruleCursor.getColumnIndexOrThrow(Columns.SUGGESTED_STATE);
            result = ruleCursor.getInt(sourceCol);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        } finally {
            ruleCursor.close();
        }

        return result;
    }

    /**
     * Read Rule table
     *
     * @return rule table cursor;
     */
    public static Cursor getSuggestedRuleCursor(Context ct, long ruleId) {

        // create WhereClause
        String ruleTableSelection = RuleTable.Columns._ID + EQUALS + Q + ruleId + Q;

        Cursor ruleTableCursor = ct.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, null,
                ruleTableSelection, null, null);

        return ruleTableCursor;
    }

    /**
     * Read condition table
     *
     * @return condition table cursor
     */
    public static Cursor getSuggestedConditionCursor(Context ct, long ruleId) {

        // create WhereClause
        String conditionTableSelection = ConditionTable.Columns.PARENT_FKEY + EQUALS + Q + ruleId
                + Q;
        Cursor conditionTableCursor = ct.getContentResolver().query(
                Schema.CONDITION_TABLE_CONTENT_URI, null, conditionTableSelection, null, null);

        return conditionTableCursor;
    }

    /**
     * Read action table
     *
     * @return action table cursor
     */
    public static Cursor getSuggestedActionCursor(Context ct, long ruleId) {

        // create WhereClause
        String actionTableSelection = ActionTable.Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q;
        Cursor actionTableCursor = ct.getContentResolver().query(Schema.ACTION_TABLE_CONTENT_URI,
                null, actionTableSelection, null, null);

        return actionTableCursor;
    }

    /** Deletes all the action pertaining to a RuleID that have not been Accepted yet
     *
     * @param context - context
     * @param _id - _id for the rule to be deleted.
     */
    public static void deleteUnAcceptedActions(Context context, long _id) {

	 String whereClause =
	        ActionTable.Columns.PARENT_FKEY + EQUALS + _id + AND +
	        ActionTable.Columns.SUGGESTED_STATE + NOT_EQUAL + RuleTable.SuggState.ACCEPTED;
	    context.getContentResolver().delete(Schema.ACTION_TABLE_CONTENT_URI, whereClause, null);

	    /* Commented out the code below and replaced it with a provider call. This is being done
	     *  to avoid a SQLLiteException for database is locked when we call this handler and another
	     *  handler queries the DB inside an Aysnc Task - as seen in bug IKMAIN-20864.
	     *
	     */
	    /*
        SQLiteManager db = SQLiteManager.openForWrite(context, TAG+".1");
        // remove all or none of the rule using transaction boundary, let this crash if db is null
        db.beginTransaction();
        try {
            // remove all actions
            String whereClause =
                ActionTable.Columns.PARENT_FKEY + EQUALS + _id + AND +
                ActionTable.Columns.SUGGESTED_STATE + NOT_EQUAL + RuleTable.SuggState.ACCEPTED;
            new ActionTable().deleteWhere(db, whereClause);

            // entire delete successful without a crash
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            db.endTransaction();
            db.close(TAG+".1");
        } */
    }

    /**
     * Queries RuleTable and checks if the rule is of suggested type
     *
     * @param ct - context
     * @param _id - rule Id
     * @return - true or false
     */
    public static boolean isSuggestedType(Context ct, final long _id){
        boolean type = false;

        Cursor ruleCursor = RulePersistence.getRuleCursor(ct, RulePersistence.getRuleKeyForRuleId(ct, _id),
                new String[] {RuleTableColumns.SUGGESTED_STATE});

        if(ruleCursor == null) return type;

        try{
            int column = ruleCursor.getColumnIndexOrThrow(Columns.SUGGESTED_STATE);
            type = (ruleCursor.getInt(column) != SuggState.ACCEPTED);
        } catch(IllegalArgumentException iae){
            iae.printStackTrace();
        } finally {
            ruleCursor.close();
        }

        return type;
    }

    /**
     * Queries RuleTable and checks if the suggestion state
     *
     * @param ct - context
     * @param _id - rule Id
     * @return - suggestion state
     */
    public static int getSuggestionState(Context ct, final long _id){

        int state = RuleTable.SuggState.ACCEPTED;

        Cursor ruleCursor = RulePersistence.getRuleCursor(ct, RulePersistence.getRuleKeyForRuleId(ct, _id),
                new String[] {RuleTableColumns.SUGGESTED_STATE});

        if(ruleCursor == null) return state;

        try{
            int column = ruleCursor.getColumnIndexOrThrow(Columns.SUGGESTED_STATE);
            state = ruleCursor.getInt(column);
        } catch(IllegalArgumentException iae){
            iae.printStackTrace();
        } finally {
            ruleCursor.close();
        }

        return state;
    }

    /**
     * Returns if a notification should be shown for this Rule.
     *
     * @param context - context
     * @param ruleKey - Key of the rule
     * @return true - if notification is not to be shown
     */
    public static boolean isSilentSuggestion(Context context, final String ruleKey){

        boolean result = false;
        String[] columns = new String[] {
                RuleTable.Columns.LIFECYCLE,
                RuleTable.Columns.SUGGESTED_STATE,
                RuleTable.Columns.SOURCE};
        String whereClause = RuleTable.Columns.KEY + EQUALS + Q + ruleKey + Q;
        Cursor cursor = RulePersistence.getDisplayRulesCursor(context, whereClause, columns);

        if(cursor != null){
            try{
                if(cursor.moveToFirst()){
                    int lifecycle = cursor.getInt(cursor.getColumnIndex(RuleTable.Columns.LIFECYCLE));
                    int childCount = cursor.getInt(cursor.getColumnIndex(RulePersistence.SAMPLE_RULE_ADOPTED_COUNT));
                    int source = cursor.getInt(cursor.getColumnIndex(RuleTable.Columns.SOURCE));

                    /*
                     * for stable7: don't show notification if a *sample* with *unadopted*
                     * rules is being *updated*
                     */
                    result = (lifecycle == RuleTable.Lifecycle.UPDATE_RULE) &&
                             (source == RuleTable.Source.FACTORY)           &&
                             (childCount == 0);
                }
            }catch (IllegalArgumentException e){
                e.printStackTrace();
            } finally{
                cursor.close();
            }
        }

        return result;
    }

    /**
     * Checks if the rule has any new UNACCEPTED actions
     *
     * @param ct - context
     * @param ruleId - Rule id
     * @return - true if new action exists
     */
    public static boolean hasNewSuggestedAction(Context ct, final long ruleId){
        boolean result = false;

        Cursor actionCursor = SuggestionsPersistence.getSuggestedActionCursor(ct, ruleId);
        if(actionCursor == null) return result;

        try{
            if(actionCursor.moveToFirst()){
                do{
                    int SuggStateCol = actionCursor.getColumnIndexOrThrow(ActionTable.Columns.SUGGESTED_STATE);

                    if(actionCursor.getInt(SuggStateCol) != SuggState.ACCEPTED){
                        result = true;
                        break;
                    }
                } while (actionCursor.moveToNext());
            }
        } catch (IllegalArgumentException e){
            e.printStackTrace();
        } finally {
            actionCursor.close();
        }

        return result;
    }

    /**
     * Checks if the rule has any new UNACCEPTED actions
     *
     * @param ct - context
     * @param ruleKey - Rule key
     * @return - true if new action exists
     */
    public static boolean hasNewSuggestedAction(Context ct, final String ruleKey){
        return hasNewSuggestedAction(ct, RulePersistence.getRuleIdForRuleKey(ct, ruleKey));
    }
}