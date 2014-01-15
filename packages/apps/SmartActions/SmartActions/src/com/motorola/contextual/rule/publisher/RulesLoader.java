/*
 * @(#)RulesLoader.java
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.rule.Constants;
import com.motorola.contextual.rule.CoreConstants.RuleType;
import com.motorola.contextual.rule.publisher.db.PublisherPersistence;
import com.motorola.contextual.rule.publisher.db.RulePublisherTable;

/**
 * This class initializes RP db, reads XML file and loads the default rules.
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
public class RulesLoader implements Constants {

    private static final String TAG = RP_TAG
            + RulesLoader.class.getSimpleName();

    private static final String SUGGESTIONS_FILE_NAME = "rulespublisher.xml";
    private static final int EOS = -1;
    private static final int BUFFER_SIZE = 8192;

    private Context mContext = null;

    public RulesLoader(Context ct) {
        mContext = ct;
    }

    /**
     * - Reads the XML file for Asset folder - add a new record in RP DB for the
     * each rule read
     * 
     * @param coreDataCleared - true id data cleared
     * @return - true if rules were updated
     */
    public void loadRules(boolean coreDataCleared) {

        // read the XML file
        String ruleXml = readFilefromAssetDir(mContext);

        // Get all child nodes from the parent Node <RULE>
        NodeList topNodes = XmlUtils.getAllRelevantNodesFromXml(ruleXml);

        // read version from the xml
        float xmlVer = XmlUtils.getVersionFromXml(topNodes);

        // check if we need to load rules to DB
        if (!isUpdateRequired(mContext, xmlVer, coreDataCleared))
            return;

        if (LOG_DEBUG)
            Log.d(TAG, "Starting to load Rules from Asset file");

        // insert default rules to DB
        processXmlNodes(mContext, topNodes);

        // publish sample rules
        RulePublisher.publishSampleRules(mContext, xmlVer);
    }

    /**
     * Check if we need to import rules
     *
     * @param ct
     *            = context
     * @param xmlVer
     *            = version read from the xml
     * @return - true or false
     */
    private static boolean isUpdateRequired(Context ct, float xmlVer, boolean coreDataCleared) {

        boolean result = true;

        if (!coreDataCleared) {
	        // read previously stored xml version from shared prefs
	        float storedXmlVer = XmlUtils.getXmlVersionFromSharedPref(ct);

	        // Does the stored ver exist?
	        if (storedXmlVer != INVALID_KEY) {
	            // is the xml ver greater than stored ver?
	            if (storedXmlVer >= xmlVer) {
	                result = false;
	                if (LOG_INFO) Log.i(TAG, "No need to update rules");
	            } else if (LOG_INFO)
	                Log.i(TAG, "Version from XML=" + xmlVer + " is greater than the old="
	                        + storedXmlVer + ", Re-importing rules");
	        } else if (LOG_INFO) {
	            Log.i(TAG, "Load rules for the first time");
	        }
        } else {
		if (LOG_DEBUG) Log.d(TAG, "Core data cleared.  update required");
        }

        return result;
    }

    /**
     * Reads the xml located in the asset directory
     *
     * @param Context
     *            - application context
     * @return String - String xml
     *
     */
    private String readFilefromAssetDir(Context ct) {

        if (LOG_DEBUG)
            Log.d(TAG, "Fn: readFilefromAssetDir");

        // Read the file
        InputStream in = null;
        AssetManager assetManager = ct.getAssets();
        StringBuilder result = new StringBuilder();

        try {
            in = assetManager.open(SUGGESTIONS_FILE_NAME);

            if (in != null) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read = 0;
                while ((read = in.read(buffer)) != EOS) {
                    result.append(new String(buffer, 0, read));
                }
            } else {
                Log.e(TAG, "readFilefromAssetDir: Xml file open failed");
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException in readFilefromAssetDir");
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException in readFilefromAssetDir");
                    e.printStackTrace();
                }
            }
        }

        return result.toString();
    }

    /**
     * Parses the XML and inserts/updates a row in RP DB for each rule
     *
     * @param context
     *            - context
     * @param topNodes
     *            - Xml nodes
     */
    private void processXmlNodes(Context context, NodeList topNodes) {

        if (topNodes == null) {
            Log.e(TAG, "Input Xml string is null");
            return;
        }

        int inserted = 0; int updated = 0;
        Set<String> keySet = PublisherPersistence.getExistingRuleKeys(context);

        for (int i = 0; i < topNodes.getLength(); i++) {
            Node curNode = topNodes.item(i);
            String curNodeName = curNode.getNodeName();

            if (LOG_VERBOSE) Log.d(TAG, "Node Name: " + curNodeName);

            if (curNodeName.equalsIgnoreCase(SMARTRULE)) {

                // returns values in key-pair format
                ContentValues cv = getRuleInfoValues(context,
                        curNode.getChildNodes());

                // insert to db only if rule type is a suggestion or sample
                if (cv != null && isSupportedRuleType(cv
                        .getAsString(RulePublisherTable.Columns.RULE_TYPE))) {

                    // rebuild xml string from the parsed docs
                    cv.put(RulePublisherTable.Columns.RULE_XML,
                            rebuildUpdatedRuleXml(context, curNode));

                    String key = cv.getAsString(RulePublisherTable.Columns.RULE_KEY);
                    if(! keySet.contains(key)){
                        // insert the rule with unique keys
                        Uri uri = PublisherPersistence.insertNewRule(context, cv);
                        if (uri != null) {
                            if(LOG_VERBOSE) Log.i(TAG, "Inserted:" + key);
                            inserted++;
                        }
                    } else {
                        int row = PublisherPersistence.updateRule(context, key, cv);
                        if(row > 0 && LOG_DEBUG) Log.d(TAG, "Updated:" + key);
                        updated++;
                    }
                }
            } else if (curNodeName.equalsIgnoreCase(DELETE_RULES)) {
                String xml = XmlUtils.getXmlTreeIn(curNode);
                if(LOG_VERBOSE) Log.i(TAG,"DEL:\n" + xml);
                RulePublisher.publishDeleteRules(context, xml);
            }
        }

        if (LOG_INFO) Log.i(TAG, "Inserted:" + inserted + ", Updated:" + updated);
        return;
    }


    /**
     * Returns true if rule type is suggested or sample
     *
     * @param type
     *            - rule type (Source)
     * @return - true or false
     */
    private boolean isSupportedRuleType(String type) {

        boolean result = false;

        if (type.equals(RuleType.SUGGESTED)
                || type.equals(RuleType.FACTORY)
                || type.equals(RuleType.CHILD)) {
            result = true;
        }

        return result;
    }

    /**
     * Populates the Content values required for RP DB from the input XML
     *
     * @param ct
     *            - context
     * @param nodes
     *            - xml nodes
     * @return - content values
     */
    private static ContentValues getRuleInfoValues(Context ct, NodeList nodes) {

        ContentValues cv = new ContentValues();

        for (int j = 0; j < nodes.getLength(); j++) {
            Node curNode = nodes.item(j);
            String curNodeName = curNode.getNodeName();

            if (LOG_VERBOSE)
                Log.d(TAG, "Node Name: " + curNodeName);

            if (curNodeName.equalsIgnoreCase(RULEINFO)) {

                // parse <RULEINFO>
                RuleInfoContainer ruleInfo = new RuleInfoContainer(curNode);
                cv.putAll(ruleInfo.getContentValues(ct));

                break;
            }
        }

        return cv;
    }

    /**
     * We need to Remove the Suggestion text
     *
     * @param ct
     * @param nodes
     * @return
     */
    private String rebuildUpdatedRuleXml(Context ct, Node topNode) {

        NodeList nodes = topNode.getChildNodes();
        for (int j = 0; j < nodes.getLength(); j++) {
            Node curNode = nodes.item(j);
            String curNodeName = curNode.getNodeName();

            if (LOG_VERBOSE)
                Log.d(TAG, "Node Name: " + curNodeName);
            if (curNodeName.equalsIgnoreCase(RULEINFO)) {

                /*
                 * We need to remove the Suggestion text
                 */
                NodeList children = curNode.getChildNodes();
                for (int k = 0; k < children.getLength(); k++) {
                    Node node = children.item(k);
                    if (node.getNodeName().equalsIgnoreCase(
                            SUGGESTION_FREEFLOW)
                            && node.getFirstChild().getNodeValue() != null) {
                        curNode.removeChild(node);
                        break;
                    }
                }
            }
        }

        return XmlUtils.getXmlTreeIn(topNode);
    }

    /**
     * This class parses the RuleInfo node via its super class
     *
     * @author a21693
     *
     */
    private static class RuleInfoContainer extends BaseContainer {

        private static final long serialVersionUID = -3481668012398156643L;

        // the constructor parses the ruleInfo Node
        RuleInfoContainer(Node ruleInfoNode) {
            super(ruleInfoNode);
        }

        /**
         * Converts the RuleInfo data into ContentValues format.
         *
         * @return content values
         */
        ContentValues getContentValues(Context context) {

            ContentValues cv = new ContentValues();
            cv.put(RulePublisherTable.Columns.RULE_NAME, get(NAME));
            cv.put(RulePublisherTable.Columns.SUGGESTION_TEXT,
                    get(SUGGESTION_FREEFLOW));
            cv.put(RulePublisherTable.Columns.RULE_KEY, get(IDENTIFIER));
            cv.put(RulePublisherTable.Columns.RULE_TYPE, get(TYPE));

            return cv;
        }
    }
}
