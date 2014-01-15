/*
 * @(#)RulesDeleter.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2011/11/15 IKINTNETAPP-444   Initial version
 *
 */

package com.motorola.contextual.smartrules.rulesimporter;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.util.Util;

/**
 * This class contains methods to handle deletion of Rules from
 * smartrules db and inference manager db
 *
 * @author a21693
 *
 */
public class RulesDeleter implements Constants, XmlConstants {

    private static final String TAG = RulesDeleter.class.getSimpleName();
    private static Context mContext = null;

    // Constructor
    RulesDeleter(Context ct){
        mContext = ct;
    }

    /**
     * Parses the Xml and kicks in the appropriate deleter
     *
     * @param delXml - input delete section xml
     */
    public void startRulesCleaner(String delXml){

        if(delXml == null){
            //Retrieve the value from shared preference
            SharedPreferences prefs = mContext.getSharedPreferences(DELETE_RULES_SHARED_PREFERENCE,
                                        Context.MODE_PRIVATE);
            delXml = prefs.getString(DELETE_RULES_XML, EMPTY_STRING);

            // Reset the value
            SharedPreferences prefsnew = mContext.getSharedPreferences(DELETE_RULES_SHARED_PREFERENCE,
                                        Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefsnew.edit();
            editor.clear();
            editor.putString(DELETE_RULES_XML,EMPTY_STRING);
            editor.commit();
        }

        if(Util.isNull(delXml)) {
            if(LOG_DEBUG) Log.d(TAG, "No clean up needed!");
        } else {
            // invoke rules cleanup module
            //TODO: simplify the log after testing is over
            if (LOG_DEBUG) Log.d(TAG,"Starting rules clean-up module after My Rules restore\n" + delXml);

            Document doc = FileUtil.getParsedDoc(delXml);
            if (doc == null) {
                Log.e(TAG,"NULL return from getParsedDoc");
                return;
            }

            // parse and invoke respective deleters
            parseAndInvokeDeleters(doc.getFirstChild());
        }
    }


   /**
    * Parses the delete rules section and then invoke
    * the respective deleter
    *
    * @param Node - parsed Xml doc
    */
    private void parseAndInvokeDeleters(Node nodes) {

        ArrayList<String> inferenceList = new ArrayList<String>();
        ArrayList<String> nxtContentList = new ArrayList<String>();

        NodeList nl = nodes.getChildNodes();

        for (int index = 0; index < nl.getLength(); index++) {

            Node node = nl.item(index);

            // validate it first
            if (isValidTopLevelNode(node)) {

                String name = node.getNodeName();

                if (name.equals(SMARTRULE_NODE)) {

                    SmartRuleDeleter delSmartRule = new SmartRuleDeleter(node);

                    if(delSmartRule.getIntValue(SOURCE, RuleTable.RuleType.DEFAULT) == RuleTable.Source.INFERRED){
                        inferenceList.add(delSmartRule.get(KEY));
                    } else {
                        delSmartRule.delete();
                    }

                } else if ( name.equals(NEXT_CONTENT)) {

                    NextContentDeleter delNextContent = new NextContentDeleter(node);
                    nxtContentList.add(delNextContent.get(NAME));
                }
            }
        }

        if(inferenceList.size() > 0 || nxtContentList.size() > 0)
            NextContentDeleter.sendBroadcastToInferenceManager(inferenceList, nxtContentList);
   }

   /** Returns true if the node is a Top level node i.e. one of
    *  SMARTRULE or NEXT_CONTENT
    *
    * @param node
    * @return - true if valid
    */
   private boolean isValidTopLevelNode(Node node) {
       String name = node.getNodeName();
       if (name.equals(SMARTRULE_NODE) || name.equals(NEXT_CONTENT)) {
           return true;
       } else {
           return false;
       }
   }


   /**
    * This class contains methods to parse and delete a Smart Rule from ModeManager db.
    *
    * @author a21693
    *
    */
    private static class SmartRuleDeleter extends BaseContainer implements DbSyntax, XmlConstants{

        private static final long serialVersionUID = 4001710925549283313L;

        /**
         * Constructor
         *
         * @param node
         */
        SmartRuleDeleter(Node node) {
            super(node);
        }

        /**
         * Dynamically create whereclause depending on the TAGS in the delete section XML
         * and queries smartrules DB to delete all the entries that match the whereclause
         *
         */
        public void delete(){

            String whereClause = createWhereClause();
            String columns[] = {RuleTable.Columns._ID, RuleTable.Columns.NAME};

            Cursor ruleCursor = RulePersistence.getRuleCursor(mContext, columns, whereClause);

            if(ruleCursor == null){
                Log.e(TAG, "Null cursor in deleteSmartRule");
            } else{
                try{
                    if(ruleCursor.moveToFirst()){
                        do{
                            String name = ruleCursor.getString(ruleCursor.getColumnIndexOrThrow(RuleTable.Columns.NAME));
                            long id = ruleCursor.getLong(ruleCursor.getColumnIndexOrThrow(RuleTable.Columns._ID));

                            if(LOG_DEBUG) Log.d(TAG, "deleteSmartRule deleting= " + name);

                            int rows = RulePersistence.deleteRule(mContext, id, name, get(KEY), true);
                            if(LOG_DEBUG) Log.d(TAG, "Deleted rows = " + rows);
                        }while(ruleCursor.moveToNext());
                    }
                } catch (IllegalArgumentException e){
                    Log.e(TAG, "Exception in deleteSmartRule");
                } finally{
                    ruleCursor.close();
                }
            }
        }

        /**
         * Create where clause dynamically depending on what source/state/enabled states have
         * been provided
         *
         * @return - where clause
         */
        private String createWhereClause(){

             int source = getIntValue(SOURCE, RuleTable.RuleType.DEFAULT);
             int state = getIntValue(SUGGESTED_STATE, RuleTable.SuggState.INVALID);
             int enabled = getIntValue(ENABLED, -1);

             StringBuilder sb = new StringBuilder();
             sb.append(RuleTable.Columns.KEY + LIKE + Q + get(KEY) + LIKE_WILD + Q);

             if(source != RuleTable.RuleType.DEFAULT){
                 sb.append(AND + RuleTable.Columns.SOURCE + EQUALS + Q + source + Q);
             }

             if(state != RuleTable.SuggState.INVALID){
                 sb.append(AND + RuleTable.Columns.SUGGESTED_STATE + EQUALS + Q + state + Q);
             }

             if(enabled != -1){
                 sb.append(AND + RuleTable.Columns.ENABLED + EQUALS + Q + enabled + Q);
             }

             if(LOG_DEBUG) Log.d(TAG, "deleteSmartRule where clause= " + sb.toString());

             return sb.toString();
        }
    }

    /**
     * This class contains methods to parse and delete Next content
     * from inference manager db.
     *
     * @author a21693
     *
     */
    private static class NextContentDeleter extends BaseContainer
                                           implements DbSyntax, XmlConstants{

        private static final long serialVersionUID = 4001710955549283313L;

        /**
         * Constructor
         *
         * @param node
         */
        NextContentDeleter(Node node) {
            super(node);
        }

        /**
         * Invoke IM to cleanup inference rules and next content
         *
         * @param infList - List of all the inference rules to be deleted
         * @param nxtList - List of all the next content to be deleted
         */
        public static void sendBroadcastToInferenceManager(ArrayList<String>  infList, ArrayList<String>  nxtList){

            Intent inferenceManagerIntent = new Intent(LAUNCH_INFERENCE_MANAGER_TO_DELETE_RULE);

            if(infList.size() > 0){
                if (LOG_DEBUG) Log.d(TAG, "Inference Rule Delete Request for: " + infList.toString());
                inferenceManagerIntent.putExtra(EXTRA_DELETE_RULE_INFERENCE, infList);
            }

            if(nxtList.size() > 0){
                if(LOG_DEBUG) Log.d(TAG, "Next Content Delete Request for: " + nxtList.toString());
                inferenceManagerIntent.putExtra(EXTRA_DELETE_RULE_NEXT_CONTENT, nxtList);
            }

            // Send a broadcast to IM to do a proper clean up
            mContext.sendBroadcast(inferenceManagerIntent);
        }
    }
}
