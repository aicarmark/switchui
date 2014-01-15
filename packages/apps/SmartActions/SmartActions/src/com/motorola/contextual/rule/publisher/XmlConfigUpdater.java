/*
 * @(#)XmlConfigUpdater.java
 *
 * (c) COPYRIGHT 2010-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2012/06/14 NA                Initial version
 *
 */

package com.motorola.contextual.rule.publisher;

import java.util.Set;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.util.Log;

import com.motorola.contextual.rule.Constants;
import com.motorola.contextual.rule.Util;
import com.motorola.contextual.rule.inference.Inference;
import com.motorola.contextual.rule.publisher.db.PublisherPersistence;
import com.motorola.contextual.rule.publisher.db.RulePublisherTable;

/**
 * This class has util methods to parse XML till we reach config tag
 * within Action or Condition block. The updated xml with new/updated
 * config is inserted in the DB
 *
 * <code><pre>
 * CLASS:
 *  Extends None
 *
 * RESPONSIBILITIES:
 *  - load default rules to db
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
public class XmlConfigUpdater {

    private static final String TAG = Constants.RP_TAG + XmlConfigUpdater.class.getSimpleName();

    /**
     * We need to update the existing xml with translated
     * name and desc
     *
     * @param ct - context
     * @param String - rule key
     * @return - update rule xml
     */
    public static void updateOrAddConfig(Context ct, String ruleKey, Set<String> pubKeySet) {

        if(pubKeySet == null || pubKeySet.size() == 0)
            return;

        String ruleXml = PublisherPersistence.getRuleColumnValue(ct, ruleKey,
                RulePublisherTable.Columns.RULE_XML);
        if(Util.isNull(ruleXml)) return;

        Document doc = XmlUtils.getParsedDoc(ruleXml);
        if (doc == null) {
            Log.e(TAG,"updateLocalizableFields: NULL return from getParsedDoc");
            return;
        }

        Node smartRuleNode = doc.getFirstChild();
        if (smartRuleNode != null) {
            NodeList topNodes = smartRuleNode.getChildNodes();
            for (int j = 0; j < topNodes.getLength(); j++) {
                Node curNode = topNodes.item(j);
                String curNodeName = curNode.getNodeName();

                if (Constants.LOG_VERBOSE) Log.d(TAG, "Node Name: " + curNodeName);
                if (! curNodeName.equalsIgnoreCase(Constants.RULEINFO)) {

                    if(pubKeySet != null && pubKeySet.size() > 0){
                        checkAndUpdateBlock(ct, ruleKey, pubKeySet, curNode);
                    }
                }
            }
        }

        // write the updated XML back to DB
        PublisherPersistence.setColumnValue(ct, ruleKey, RulePublisherTable.Columns.RULE_XML,
                XmlUtils.getXmlTreeIn(smartRuleNode));

    }

    /**
     * traverses the action block and translates any string if present in a config
     *
     * @param ct - context
     * @param pubConfigs = publisher/config combo
     * @param curNode - rule info top node
     */
    private static void checkAndUpdateBlock(Context ct, String ruleKey, Set<String> pubKeySet, Node nodes){

        if (Constants.LOG_VERBOSE) Log.d(TAG, "UpdateBlock");

        NodeList topNodes = nodes.getChildNodes();
        if (Constants.LOG_VERBOSE) Log.d(TAG, "child="+topNodes.getLength());
        for (int i = 0; i < topNodes.getLength(); i++) {
            Node curNode = topNodes.item(i);
            String curNodeName = curNode.getNodeName();

            if (curNodeName.equalsIgnoreCase(Constants.ACTION) ||
                    curNodeName.equalsIgnoreCase(Constants.CONDITION)) {

                if (Constants.LOG_VERBOSE) Log.d(TAG, "block=" + curNodeName);
                String pubKey = null;
                NodeList actCondNodes = curNode.getChildNodes();
                if (Constants.LOG_VERBOSE) Log.d(TAG, "child count="+actCondNodes.getLength());
                for (int j = 0; j < actCondNodes.getLength(); j++) {
                    Node node = actCondNodes.item(j);
                    String nodeName = node.getNodeName();

                    if (Constants.LOG_VERBOSE) Log.d(TAG, "Node Name: " + nodeName);
                    if (nodeName.equalsIgnoreCase(Constants.PUB_KEY)
                            && (pubKey = node.getFirstChild().getNodeValue()) != null) {
                        if (Constants.LOG_VERBOSE) Log.d(TAG, "pubkey " + nodeName);

                        if(pubKeySet.contains(pubKey)){
                            updateConfigInXml(ct, actCondNodes, ruleKey, pubKey);
                        }
                    }
                }
            }
        }
    }

    /**
     * Add/replace config from publish_config column
     *
     * @param ct - context
     * @param actCondChildNodes - Xml nodes for action/condition block
     * @param pubConfig - pub/config combo from publish_config column
     */
    private static void updateConfigInXml(Context ct, NodeList actCondChildNodes, String ruleKey, String pubKey){

        if (Constants.LOG_VERBOSE) Log.d(TAG, "updateConfigInXml");
        boolean updated = false;

        for (int j = 0; j < actCondChildNodes.getLength(); j++) {
            Node node = actCondChildNodes.item(j);
            String nodeName = node.getNodeName();

            if (nodeName.equalsIgnoreCase(Constants.CONFIG)) {

                String config = node.getFirstChild().getNodeValue();

                if (Constants.LOG_DEBUG) Log.d(TAG, "Orignial config" + config);
                Inference obj = Inference.getInstance(ruleKey);
                config = obj.getUpdatedConfig(ct, pubKey, config);

                if (Constants.LOG_DEBUG) Log.d(TAG, "Updated config" + config);
                node.getFirstChild().setNodeValue(config);

                updated = true;
                break;
            }
        }

        // this means the config tag itself was missing
        if(! updated){

            // fetch updated config
            Inference inf = Inference.getInstance(ruleKey);
            String config = inf.getUpdatedConfig(ct, pubKey, null);

            if (Constants.LOG_DEBUG) Log.d(TAG, "Updated config=" + config);

            // now create a new node
            try {
                Document doc= actCondChildNodes.item(0).getOwnerDocument();
                Element element = doc.createElement(Constants.CONFIG);
                element.appendChild(doc.createTextNode(config));

                if (Constants.LOG_DEBUG) Log.d(TAG, "New node " + element.getFirstChild().getNodeName());

                // Insert the new node to the parent - action/condition node
                actCondChildNodes.item(0).getParentNode().appendChild(element);

            } catch (DOMException e) {
                Log.e(TAG, "Err=" + e.code);
                e.printStackTrace();
            }
        }
    }
}