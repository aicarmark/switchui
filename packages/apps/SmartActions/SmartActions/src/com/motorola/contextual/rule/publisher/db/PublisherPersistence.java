/*
 * @(#)PublisherPersistence.java
 *
 * (c) COPYRIGHT 2010-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2012/02/14 NA                Initial version
 *
 */

package com.motorola.contextual.rule.publisher.db;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.rule.Constants;
import com.motorola.contextual.rule.Constants.RuleState;
import com.motorola.contextual.rule.CoreConstants;
import com.motorola.contextual.rule.CoreConstants.RuleType;
import com.motorola.contextual.rule.Util;
import com.motorola.contextual.smartrules.db.DbSyntax;

/** This class contains the utilities used to read the DB
*
*
*<code><pre>
* CLASS:
* Implements DbSyntax to reuse constants.
*
*
* RESPONSIBILITIES:
*  see each method for details
*
* COLABORATORS:
*  None.
*
* USAGE:
*  see methods for usage instructions
*
*</pre></code>
*/
public class PublisherPersistence implements DbSyntax {

    private static final String TAG = Constants.RP_TAG + PublisherPersistence.class.getSimpleName();


    /**
     * Returns the no# of Rules
     *
     * @param ct - context
     * @return - rule count
     */
    public static int getRulesCount(Context ct) {
        int count = 0;

        // what columns to query
        String[] columns = new String[] { RulePublisherTable.Columns._ID };

        // query the db to fetch the rule tuple
        Cursor c = DbHelper.getInstance(ct).query(
                RulePublisherTable.CONTENT_URI, columns, null, null, null);

        if(c != null ){
            count = c.getCount();
            c.close();
        }

        return count;
    }


    /**
     * Returns the complete Rule XML from DB for the Rule Key
     *
     * @param ct - context
     * @param ruleKey - rule key
     * @return - rule xml
     */
    public static String getRuleXmls(Context ct, String whereClause) {

        String xml = null;

        // what columns to query
        String[] columns = new String[] { RulePublisherTable.Columns.RULE_XML,
                RulePublisherTable.Columns.RULE_KEY};

        // query the db to fetch the rule tuple
        Cursor c = DbHelper.getInstance(ct).query(
                RulePublisherTable.CONTENT_URI, columns, whereClause, null,
                null);

        if (c != null) {
            try {
                if (c.moveToFirst()) {

                    StringBuilder bldr = new StringBuilder();
                    do{

                        // read the rule xml from the column
                        String ruleXml = c.getString(c.getColumnIndexOrThrow(columns[0]));
                        ruleXml = Util.updateLocalizableFields(ct, ruleXml);

                        bldr.append(ruleXml);
                        bldr.append(Constants.NEW_LINE);

                        if(Constants.LOG_DEBUG) Log.w(TAG, "Key=" + ruleXml.split(Constants.IDENTIFIER)[1]);
                    }while(c.moveToNext());

                    xml =  bldr.toString();

                }
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Fectching of Samples rule xmls failed");
                e.printStackTrace();
            } finally {
                c.close();
            }
        }

        return xml;
    }

    /**
     * Returns the complete Rule XML from DB for the Rule Key
     *
     * @param ct - context
     * @param ruleKey - rule key
     * @return - rule xml
     */
    public static String getRuleXml(Context ct, String ruleKey) {

        // populate the where clause
        String whereClause = RulePublisherTable.Columns.RULE_KEY + EQUALS + Q
                + ruleKey + Q;

        return getRuleXmls(ct, whereClause);
    }

    /**
     * Returns the complete Rule XML from DB for the Rule Key
     *
     * @param ct - context
     * @param ruleKey - rule key
     * @return - rule xml
     */
    public static String getAllSampleRuleXmls(Context ct) {

        // populate the where clause
        String whereClause = 
            RulePublisherTable.Columns.RULE_TYPE + EQUALS + Q + RuleType.FACTORY + Q
            + OR +
            RulePublisherTable.Columns.RULE_TYPE + EQUALS + Q + RuleType.CHILD + Q;

        return getRuleXmls(ct, whereClause);
    }



    /**
     * Reads the Suggestion Text Column
     *
     * @param ct - context
     * @param ruleKey - rule key
     * @return - Suggestion Text
     */
    public static String getRuleSuggestionText(Context ct, String ruleKey) {
        String xml = null;

        // what columns to query
        String[] columns = new String[] { RulePublisherTable.Columns.SUGGESTION_TEXT };
        // populate the where clause
        String whereClause = RulePublisherTable.Columns.RULE_KEY + EQUALS + Q
                + ruleKey + Q;

        // query the db to fetch the rule tuple
        Cursor c = DbHelper.getInstance(ct).query(
                RulePublisherTable.CONTENT_URI, columns, whereClause, null,
                null);

        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    // read the rule xml from the column
                    xml = c.getString(c.getColumnIndexOrThrow(columns[0]));
                }
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Fetching rule xml failed for = " + ruleKey);
                e.printStackTrace();
            } finally {
                c.close();
            }
        }

        return xml;
    }

    /**
     * Reads the string value of the column supplied.
     *
     * @param ct - context
     * @param ruleKey - rule key
     * @param column - columns to read
     * @return - column value
     */
    public static String getRuleColumnValue(Context ct, String ruleKey, String column) {

        // populate the where clause
        String whereClause = RulePublisherTable.Columns.RULE_KEY + EQUALS + Q
                + ruleKey + Q;

        return getRuleColumnValue(whereClause, column, ct);
    }

    /**
     * Reads the string value of the column supplied.
     *
     * @param ct - context
     * @param ruleId - rule _ID
     * @param column - columns to read
     * @return - column value
     */
    public static String getRuleColumnValue(Context ct, long ruleId, String column) {

        // populate the where clause
        String whereClause = RulePublisherTable.Columns._ID + EQUALS + Q
                + ruleId + Q;

        return getRuleColumnValue(whereClause, column, ct);
    }

    /**
     * Reads the string value of the column supplied.
     *
     * @param whereClause - where clause
     * @param ct - context
     * @param column - columns to read
     * @return - column value
     */
    public static String getRuleColumnValue(String whereClause, String column, Context ct) {
        String value = null;

        // what columns to query
        String[] columns = new String[] { column };

        // query the db to fetch the rule tuple
        Cursor c = DbHelper.getInstance(ct).query(
                RulePublisherTable.CONTENT_URI, columns, whereClause, null,
                null);

        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    // read the rule xml from the column
                    value = c.getString(c.getColumnIndexOrThrow(column));
                }
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Fetching rule xml failed for = " + whereClause);
                e.printStackTrace();
            } finally {
                c.close();
            }
        }

        return value;
    }

    /**
     * Insert a new Rule
     *
     * @param ct - context
     * @param values - content values
     * @return the URL of the newly created row.
     */
    public static Uri insertNewRule(Context ct, ContentValues values) {

        return DbHelper.getInstance(ct).insert(RulePublisherTable.CONTENT_URI, values);
    }

    /**
     * Update an existing Rule
     *
     * @param ct - context
     * @param ruleKey - rule key
     * @param values - content values
     * @return - # of rows updated
     */
    public static int updateRule(Context ct, String ruleKey, ContentValues values) {

        // populate where clause
        String whereClause = RulePublisherTable.Columns.RULE_KEY + EQUALS + Q + ruleKey
                + Q;

        // update the DB row for this rule key
        return DbHelper.getInstance(ct).update(RulePublisherTable.CONTENT_URI, values,
                whereClause, null);
    }

    /**
     * Sets the inferred state for the rule
     *
     * @param ct - context
     * @param ruleKey - rule key
     * @param state - 0 if incomplete
     */
    public static void setInferredState(Context ct, String ruleKey, String state) {

        // populate where clause
        String whereClause = RulePublisherTable.Columns.RULE_KEY + EQUALS + Q + ruleKey
                + Q;

        ContentValues cv = new ContentValues();
        cv.put(RulePublisherTable.Columns.INFERRED_STATE, state);

        // it is inferred but not published yet!
        cv.put(RulePublisherTable.Columns.PUBLISH_STATE, RuleState.UNPUBLISHED);

        // set the inferred time-stamp
        cv.put(RulePublisherTable.Columns.INFERRED_TIME, new Date().getTime());

        // update the DB row for this rule key
        DbHelper.getInstance(ct).update(RulePublisherTable.CONTENT_URI, cv,
                whereClause, null);

    }

    /**
     * Sets the inferred state for the rule
     *
     * @param ct - context
     * @param ruleKey - rule key
     * @param col - column to set
     * @param val - value to set
     */
    public static void setColumnValue(Context ct, String ruleKey, String col, String val) {

        // populate where clause
        String whereClause = RulePublisherTable.Columns.RULE_KEY + EQUALS + Q + ruleKey
                + Q;

        ContentValues cv = new ContentValues();
        cv.put(col, val);

        // update the DB row for this rule key
        DbHelper.getInstance(ct).update(RulePublisherTable.CONTENT_URI, cv,
                whereClause, null);

    }

    /**
     * reads the inferred state from the db
     *
     * @param ct - context
     * @param ruleKey - rule key
     * @return - inferred state
     */
    public static String getInferredState(Context ct, String ruleKey) {
        String state  = RuleState.UNPUBLISHED;

        // what columns to query
        String[] columns = new String[] { RulePublisherTable.Columns.INFERRED_STATE };

        // create the where clause
        String whereClause = RulePublisherTable.Columns.RULE_KEY + EQUALS + Q
                + ruleKey + Q;

        // fetch the row from the db
        Cursor c = DbHelper.getInstance(ct).query(
                RulePublisherTable.CONTENT_URI, columns, whereClause, null,
                null);

        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    // read the column value
                    state = c.getString(c.getColumnIndexOrThrow(columns[0]));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                c.close();
            }
        }

        // Ensure that we do not return null or invalid value
        if(Util.isNull(state)) state  = RuleState.UNPUBLISHED;

        return state;
    }

    /**
     * fetches the rule inferred time
     *
     * @param ct - context
     * @param ruleKey - rule key
     * @return - column value
     */
    public static long getRuleInferredTime(Context ct, String ruleKey) {
        long time = 0;

        // what columns to query
        String[] columns = new String[] { RulePublisherTable.Columns.INFERRED_TIME};

        // create the where clause
        String whereClause = RulePublisherTable.Columns.RULE_KEY + EQUALS + Q
                + ruleKey + Q;

        // query the db to fetch the rule tuple
        Cursor c = DbHelper.getInstance(ct).query(
                RulePublisherTable.CONTENT_URI, columns, whereClause, null,
                null);

        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    // read the rule xml from the column
                    time = c.getLong(c.getColumnIndexOrThrow(columns[0]));
                }
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Fetching rule xml failed for = " + whereClause);
                e.printStackTrace();
            } finally {
                c.close();
            }
        }

        return time;
    }


    /**
     * Sets the inferred state for the rule
     *
     * @param ct - context
     * @param ruleKey - rule key
     * @param response - publish response received from Core
     */
    public static void setPublishResponse(Context ct, String ruleKey, String response) {

        // populate where clause
        String whereClause = RulePublisherTable.Columns.RULE_KEY + EQUALS + Q + ruleKey + Q;

        ContentValues cv = new ContentValues();

        // set the timestamp
        cv.put(RulePublisherTable.Columns.PUBLISH_TIME, new Date().getTime());

        String state = response.contains(CoreConstants.XML_INSERT_SUCCESS)?
                RuleState.PUBLISHED:response;

        cv.put(RulePublisherTable.Columns.PUBLISH_STATE, state);

        // update the DB row for this rule key
        DbHelper.getInstance(ct).update(RulePublisherTable.CONTENT_URI, cv,
                whereClause, null);

        if(!response.contains(CoreConstants.XML_INSERT_SUCCESS)){
            DebugTable.writeToDebugViewer(ct, DebugTable.Direction.IN, TAG, null, ruleKey, 
                    Constants.DBG_CORE_TO_RP, Constants.DBG_MSG_RULE_PUB_FAILED, response,
                    Constants.PUBLISHER_KEY, Constants.PUBLISHER_KEY);
        }

        if(Constants.LOG_INFO && !state.equals(RuleState.PUBLISHED))
            Log.i(TAG, "Rule publish failed for=" + ruleKey + "; Response="+ response);
    }


    /**
     * Retrieves the Set of rule keys that are present in RP DB
     *
     * @param ct - context
     * @return - rule key set
     */
    public static Set<String> getExistingRuleKeys(Context ct){

        String[] columns = new String[] {RulePublisherTable.Columns.RULE_KEY};
        Set<String> keySet = new HashSet<String>();

        Cursor c = DbHelper.getInstance(ct).query(RulePublisherTable.CONTENT_URI, columns,
                        null, null, null);

        if(c != null){
            try{
                if(c.moveToFirst()){
                    do{
                        keySet.add(c.getString(c.getColumnIndexOrThrow(RulePublisherTable.Columns.RULE_KEY)));
                    }while(c.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                c.close();
            }
        }

        return keySet;
    }
}
