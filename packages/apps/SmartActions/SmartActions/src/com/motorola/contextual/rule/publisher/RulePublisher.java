/*
 * @(#)RulePublisher.java
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

package com.motorola.contextual.rule.publisher;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.motorola.contextual.rule.Constants;
import com.motorola.contextual.rule.CoreConstants;
import com.motorola.contextual.rule.Util;
import com.motorola.contextual.rule.publisher.db.PublisherPersistence;
import com.motorola.contextual.rule.publisher.db.RulePublisherTable;
import com.motorola.contextual.smartrules.db.DbSyntax;

/**
 * This class contains methods required to send xml or refreshes to SA Core
 *
 * <code><pre>
 * CLASS:
 *  Extends none
 *
 * RESPONSIBILITIES:
 *  - Publishes rule xml
 *
 * COLABORATORS:
 *  None
 *
 * USAGE:
 *  See each method.
 * </pre></code>
 *
 * @author a21693
 *
 */
public final class RulePublisher implements Constants, CoreConstants{

    private static final String TAG = Constants.RP_TAG
            + RulePublisher.class.getSimpleName();
    Context mContext = null;

    // constructor
    public RulePublisher(Context ct) {
        mContext = ct;
    }

    /**
     * broadcasts the rule delete xml
     *
     * @param ct - context
     * @param delXml - delete xml
     */
    public static void publishDeleteRules(Context ct, String delXml) {

        if(Util.isNull(delXml)){
            return;
        }

        // send broadcast to SA core
        broadcastToCore(ct, delXml, RuleType.DELETE);
    }

    /**
     * pulls rule xml from the RP DB and broadcasts it out
     *
     * @param ruleKey
     *            - rule key
     */
    public static void publishSuggestedRule(Context ct, String ruleKey) {

        // fetch the rule xml from RP db
        String ruleXml = PublisherPersistence.getRuleXml(ct, ruleKey);

        if(Util.isNull(ruleXml)){
            Log.w(TAG, "publishSuggestedRule: Null Xml for key=" + ruleKey);
            return;
        }

        // translate Rule name and desc
        ruleXml = XmlUtils.updateRuleTypeField(ct, ruleXml, RuleType.SUGGESTED);

        // send broadcast to SA core
        broadcastToCore(ct, ruleXml, ruleKey);

        // Set the inferred state, this internally sets the inferred time as well
        PublisherPersistence.setInferredState(ct, ruleKey, RuleState.INFERRED);

        if(LOG_INFO) Log.i(TAG, "\nInferred rule published=" + ruleKey);

        if(LOG_VERBOSE){
            Toast.makeText(ct, Util.getTranslatedString(ct, ruleKey) + " Published", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * pulls rule xml from the RP DB and broadcasts it out
     *
     * @param ruleKey
     *            - rule key
     */
    public static void publishSampleRules(Context ct, float reqId) {

        // fetch the rule xml from RP db
        String ruleXml = PublisherPersistence.getAllSampleRuleXmls(ct);

        if(Util.isNull(ruleXml)){
            Log.e(TAG, "publishSampleRules: Null Xml");
            return;
        }

        // send broadcast to SA core
        broadcastToCore(ct, ruleXml, XML_UPGRADE + RuleType.FACTORY + "=" + reqId);
    }

    /**
     * pulls rule xml from the RP DB and broadcasts it out
     *
     * @param ruleKey - rule key
     */
    public static void publishSelectRule(Context ct, String ruleKey) {

        // fetch the rule xml from RP db
        String ruleXml = PublisherPersistence.getRuleXml(ct, ruleKey);

        if(Util.isNull(ruleXml)){
            Log.w(TAG, "publishSelectRule: Null Xml for key=" + ruleKey);
            return;
        }

        // send broadcast to SA core
        broadcastToCore(ct, ruleXml, ruleKey);
    }

    /**
     * pulls rule xml from the RP DB and broadcasts it out
     *
     * @param ruleKey
     *            - rule key
     */
    public static void publishInferredRules(Context ct) {

        String whereClause = 
            RulePublisherTable.Columns.INFERRED_STATE + DbSyntax.EQUALS
            + DbSyntax.Q + RuleState.INFERRED + DbSyntax.Q + DbSyntax.AND
            + RulePublisherTable.Columns.PUBLISH_STATE + DbSyntax.NOT_EQUAL
            + DbSyntax.Q + RuleState.PUBLISHED + DbSyntax.Q;

        // fetch the rule xml from RP db
        String ruleXml = PublisherPersistence.getRuleXmls(ct, whereClause);

        if(Util.isNull(ruleXml)){
            if(LOG_DEBUG) Log.d(TAG, "publishInferredRules: No unpublished inferred rules");
            return;
        } else {
            if(LOG_INFO) Log.i(TAG, "publishInferredRules: Publishing inferred rules!");
        }

        // send broadcast to SA core
        broadcastToCore(ct, ruleXml, RuleType.SUGGESTED);
    }

    /**
     * pulls rule xml from the RP DB and broadcasts it out
     *
     * @param ruleKey
     *            - rule key
     */
    public static void publishSelectRules(Context ct, ArrayList<String> list) {

        String whereClause = getWhereClause(RulePublisherTable.Columns.RULE_KEY, list);

        // fetch the rule xml from RP db
        String ruleXml = PublisherPersistence.getRuleXmls(ct, whereClause);

        if(Util.isNull(ruleXml)){
            return;
        }

        // send broadcast to SA core
        broadcastToCore(ct, ruleXml, RuleType.FACTORY);
    }



    /**
     * sends broadcast to SA Core along with: - Import type as INFERRED - Rule
     * XML in intent extra
     *
     * @param ct - context
     * @param ruleXml - rule xml
     * @param reqId - req id
     */
    private static void broadcastToCore(Context ct, String ruleXml, String reqId) {

        final String RULES_START = "<RULES>\n";
        final String RULES_END = "\n</RULES>";

        if(ruleXml == null || ruleXml.equals(EMPTY_STRING))
            return;

        ruleXml = RULES_START + ruleXml + RULES_END;

        Intent ruleIntent = new Intent(CoreConstants.PUBLISH_TO_SA_CORE);
        ruleIntent.putExtra(CoreConstants.XML_CONTENT, ruleXml);
        ruleIntent.putExtra(CoreConstants.EXTRA_VERSION, CoreConstants.VERSION);
        ruleIntent.putExtra(CoreConstants.REQUEST_ID, reqId);
        ruleIntent.putExtra(CoreConstants.PUB_KEY, PUBLISHER_KEY);
        ct.sendBroadcast(ruleIntent, PERM_RULE_PUBLISHER_ADMIN);

        if (Constants.LOG_INFO) Log.d(TAG, "RP Publishes with reqId=" + reqId);

    }


    /**
     * Create Key based where clause
     *
     * @param list - list of keys
     * @return - where clause
     */
    private static String getWhereClause(String column, ArrayList<String> list){

        if(list == null) return null;
        Iterator<String> iter = list.iterator();

        // create the whereClause
        String col = column + DbSyntax.LIKE + DbSyntax.Q;
        String or = DbSyntax.Q + DbSyntax.OR;
        StringBuilder where = new StringBuilder();

        int indx = 0;
        while(iter.hasNext()){
            if(indx++ > 0) where.append(or);
            where.append(col);
            where.append(iter.next());
        }
        where.append(DbSyntax.Q);

        return where.toString();
    }
}