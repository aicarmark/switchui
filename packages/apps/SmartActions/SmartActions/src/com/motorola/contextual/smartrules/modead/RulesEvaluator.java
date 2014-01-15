/*
 * @(#)RulesEvaluator.java
 *
 * (c) COPYRIGHT 2010-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21345        2012/02/28 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.modead;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.db.table.view.RuleConditionView;

/** This is the class that Evaluates the state of the rule and sends
 * State change intent with the rule key.
 *
 *<code><pre>
 * CLASS:
 *
 *  implements
 *   Constants - for the constants used
 *   DbSyntax - for constructing the query
 *
 * RESPONSIBILITIES:
 * 	 Evaluates the rule and send state change intent
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 *</pre></code>
 */
public class RulesEvaluator implements Constants, DbSyntax {

    private static final String TAG = RulesEvaluator.class.getSimpleName();
    private Context mContext;

    public RulesEvaluator(Context context) {
        mContext = context;
    }


    /**
     * Queries the RuleConditionView to get the current state of all the
     * Rules that uses the given config and publisher key and then invokes evaluateRule
     * @param config - Configuration of the Condition publisher
     * @param pubKey - Publisher key of the the condition publisher
     */
    void evaluateAndSend(String config, String pubKey) {

        ContentResolver cr = mContext.getContentResolver();
        if(LOG_DEBUG)Log.d(TAG, " Evaluate : " + config + " :" + pubKey);
        String[] projection = { RuleConditionView.Columns.KEY, RuleConditionView.Columns.ACTIVE };

        String whereClause = RuleConditionView.Columns.CONDITION_CONFIG+EQUALS+Q+config+Q+
                             AND+RuleConditionView.Columns.CONDITION_PUBLISHER_KEY+EQUALS+Q+pubKey+Q+
                             AND+RuleConditionView.Columns.ACTIVE+NOT_EQUAL+Q+"2"+Q +
                             AND+RuleConditionView.Columns.CONDITION_VALIDITY+EQUALS+Q+TableBase.Validity.VALID+Q+
                             AND+RuleConditionView.Columns.VALIDITY+EQUALS+Q+TableBase.Validity.VALID+Q;
        Cursor cursor = null;
        try {
            cursor = cr.query(RuleConditionView.CONTENT_URI, projection, whereClause, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                do {
                    String ruleKey = cursor.getString(cursor.getColumnIndex(RuleConditionView.Columns.KEY));
                    int currentRuleState = cursor.getInt(cursor.getColumnIndex(RuleConditionView.Columns.ACTIVE));
                    int ruleState = evaluateRule(ruleKey, currentRuleState);
                    sendRuleStateChangeIntent(ruleKey, ruleState);
                } while (cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.e(TAG,"Exception during query");
        } finally {
            if(cursor != null) cursor.close();
        }
    }

    /**
     * Sends State change event to manual rule
     * @param ruleKey
     */
    void sendRuleStateChangeIntentForManualRule(String ruleKey) {
        sendRuleStateChangeIntent(ruleKey, 1);
    }

    /**
     * Sends the state change intent with the give rulekey and rule state
     * @param ruleKey - Rule key for which the state change intent is sent
     * @param ruleState - state information of the rule
     */
    private void sendRuleStateChangeIntent(String ruleKey, int ruleState) {
        if(LOG_DEBUG) Log.d(TAG, "sendRuleStateChangeIntent " + ruleKey + ":" + ruleState);
        Intent intent = new Intent(ACTION_RULESTATE_CHANGED);
        intent.putExtra(EXTRA_RULE_KEY, ruleKey);
        intent.putExtra(EXTRA_STATE, (ruleState > 0 ? TRUE:FALSE));
        mContext.sendBroadcast(intent);
        DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.INTERNAL,
                                      (ruleState > 0 ? TRUE:FALSE), "sendRuleStateChangeIntent",
                                      ruleKey, MODEAD_DBG_MSG, null, null,
                                      Constants.PACKAGE, Constants.PACKAGE);
    }

    /**
     * Queries the state of all the config and publisher key of the condition publishers
     * used in the rule, then evaluates the state of the Rule.
     * @param ruleKey - Rule key for which the state needs to be evaluated
     * @param currentRuleState - Current State of the Rule
     * @return - Newly evaluated state of the rule
     */
    private int evaluateRule(String ruleKey, int currentRuleState) {
        if(LOG_DEBUG) Log.d(TAG, "evaluateRule " + ruleKey + ":" + currentRuleState);
        int ruleState =  currentRuleState;
        ContentResolver cr = mContext.getContentResolver();
        String[] projection = { RuleConditionView.Columns.KEY, RuleConditionView.Columns.CONDITION_CONFIG,
                                RuleConditionView.Columns.PUBLISHER_KEY, RuleConditionView.Columns.CONDITION_MET,
                                ConditionTable.Columns.ENABLED
                              };
        String whereClause = RuleConditionView.Columns.KEY+EQUALS+Q+ruleKey+Q+
                             AND+RuleConditionView.Columns.CONDITION_PUBLISHER_KEY+IS_NOT_NULL+
                             AND+RuleConditionView.Columns.CONDITION_VALIDITY+EQUALS+Q+TableBase.Validity.VALID+Q+
                             AND+ConditionTable.Columns.ENABLED+EQUALS+Q+ConditionTable.Enabled.ENABLED+Q;
        Cursor cursor = null;
        try {
            cursor = cr.query(RuleConditionView.CONTENT_URI, projection, whereClause, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                int allConditionState = 1;
                do {
                    int conditionState = cursor.getInt(cursor.getColumnIndex(RuleConditionView.Columns.CONDITION_MET));
                    allConditionState &= conditionState;
                } while(cursor.moveToNext());
                ruleState = allConditionState;
            }
        } catch(Exception e) {
            Log.e(TAG,"Exception during query");
        } finally {
            if(cursor != null) cursor.close();
        }
        return ruleState;
    }

}
