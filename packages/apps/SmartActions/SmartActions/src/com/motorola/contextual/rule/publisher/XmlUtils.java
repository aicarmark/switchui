/*
 * @(#)XmlUtil.java
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
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.motorola.contextual.rule.Constants;


/**
 * This class contains the utilities used to read the xml
 *
 *
 * <code><pre>
 * CLASS:
 * Implements Constants to reuse constants.
 *
 *
 * RESPONSIBILITIES:
 * 	Reading the string from XML
 *  Creating a file with a specific name
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 * 	see methods for usage instructions
 *
 * </pre></code>
 */
public class XmlUtils implements Constants {

    public static final String TAG = RP_TAG
            + XmlUtils.class.getSimpleName();

    /**
     * Fetch the Stored XML version from the shared preference
     *
     * @param context- Application context
     * @return - XML version
     */
    public static float getXmlVersionFromSharedPref(Context context) {

        SharedPreferences sp = context.getSharedPreferences(Constants.PUBLISHER_KEY, Context.MODE_PRIVATE);
        float xmlVer = sp.getFloat(RP_XML_VERSION, INVALID_KEY);
        if (LOG_DEBUG) Log.d(TAG, " XML version = " + xmlVer);

        return xmlVer;
    }

    /**
     * Stores the XML version in a shared preference
     *
     * @param context
     *            - Application context
     * @param xmlVersion
     *            - new XML version
     */
    public static void storeXmlVersionToSharedPref(Context context, float xmlVersion) {

        SharedPreferences sp = context.getSharedPreferences(Constants.PUBLISHER_KEY, Context.MODE_PRIVATE);
        sp.edit().putFloat(RP_XML_VERSION, xmlVersion).apply();
        if (LOG_DEBUG) Log.d(TAG, "XML version stored= " + xmlVersion);
    }
    
    /**
     * Resets the XML version in the shared preference
     *
     * @param context
     *            - Application context
     * 
     */
    public static void resetXmlVersionSharedPref(Context context) {

        SharedPreferences sp = context.getSharedPreferences(Constants.PUBLISHER_KEY, Context.MODE_PRIVATE);
        sp.edit().putFloat(RP_XML_VERSION, INVALID_KEY).apply();
        if (LOG_DEBUG) Log.d(TAG, "XML version reset");
    }

    /**
     * Parses the VERSION node from the XML which contains SMARTRULES
     *
     *
     * @param topNodes
     *            - XML top nodes
     * @return float - Provides the version number in the XML
     */
    public static float getVersionFromXml(NodeList topNodes) {

        if (LOG_VERBOSE)
            Log.d(TAG, "Fn: getTheXmlVersion");

        float xmlVersion = DEFAULT_XML_VERSION;

        if (topNodes == null) {
            Log.e(TAG, "getVersionFromXml: Nodes == null");
            return xmlVersion;
        }

        for (int i = 0; i < topNodes.getLength(); i++) {
            Node curNode = topNodes.item(i);
            String curNodeName = curNode.getNodeName();

            if (LOG_VERBOSE)
                Log.d(TAG, "Node Name: " + curNodeName);

            String curNodeValue = null;
            if (curNodeName.equalsIgnoreCase(XML_VERSION_NODE)
                    && curNode.getFirstChild() != null
                    && ((curNodeValue = curNode.getFirstChild().getNodeValue()) != null)) {

                if (LOG_VERBOSE)
                    Log.d(TAG, "Node Value " + curNodeValue);

                try {
                    xmlVersion = Float.valueOf(curNodeValue);
                } catch (NumberFormatException e) {
                    Log.w(TAG,
                            "Exception when converting Xml Version into float");
                }

                if (LOG_DEBUG)
                    Log.d(TAG, "Xml Version : " + xmlVersion);
                break;
            }
        }
        return xmlVersion;
    }

    /**
     * Get the XML Tree inside a node
     *
     * @param node
     *            - Node containing the tree
     * @return - Entire tree under node as a String
     */
    public static String getXmlTreeIn(final Node node) {

        if (LOG_VERBOSE)
            Log.d(TAG, "Fn: getXmlTreeIn");
        String result = EMPTY_STRING;

        if (node == null)
            return result;

        try {
            TransformerFactory tranFact = TransformerFactory.newInstance();
            Transformer transfor = tranFact.newTransformer();
            transfor.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            Source src = new DOMSource(node);
            StringWriter buffer = new StringWriter();
            Result dest = new StreamResult(buffer);
            transfor.transform(src, dest);
            result = buffer.toString();
        } catch (Exception e) {
            Log.e(TAG, "Unable to get XML tree inside " + node.getNodeName()
                    + " " + e.getMessage());
        }
        return result;
    }

    /**
     * Get all the relevant child nodes from the XML
     *
     * @param ruleXml
     *            - contents in XML
     * @return - all nodes
     */
    public static NodeList getAllRelevantNodesFromXml(final String ruleXml) {
        if (LOG_VERBOSE)
            Log.d(TAG, "Entered getAllRelevantNodesFromXml");
        Document doc = getParsedDoc(ruleXml);

        if (doc != null) {
            NodeList nodes = getAllNodesFromXml(doc);
            return nodes;
        } else {
            Log.e(TAG, "doc is null");
            return null;
        }
    }

    /**
     * Get the XML doc for an XML
     *
     * @param ruleXml
     *            - contents in XML
     * @return - xml Document
     */
    public static Document getParsedDoc(final String ruleXml) {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        }
        Document doc = null;
        try {
            doc = db.parse(new InputSource(new StringReader(ruleXml)));
        } catch (SAXException e) {
            Log.e(TAG, "XML Syntax error");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "IO Exception");
            e.printStackTrace();
        }
        return doc;
    }

    /**
     * Get all the Child Nodes within <RULES> node
     *
     * @param doc
     *            - XML document
     * @return - get all nodes under tag RULES
     */
    public static NodeList getAllNodesFromXml(Document doc) {
        NodeList nodes = doc.getElementsByTagName(RULES).item(0)
                .getChildNodes();

        if (LOG_VERBOSE) {
            Element intElement = (Element) doc.getElementsByTagName(RULES)
                    .item(0);
            String value = intElement.getChildNodes().item(0).getNodeValue();

            Log.i(TAG, "getAllNodesFromXml : " + value);
        }

        return nodes;
    }

    /**
     * We need to update the existing xml rule type/source
     *
     * @param ct - context
     * @param String - rule xml
     * @param - source
     * @return - update rule xml
     */
    public static String updateRuleTypeField(Context ct, String ruleXml, String source) {

        if(source == null) throw new IllegalArgumentException();

        Document doc = XmlUtils.getParsedDoc(ruleXml);
        if (doc == null) {
            Log.e(TAG,"updateRuleTypeField: NULL return from getParsedDoc");
            return ruleXml;
        }

        Node smartRuleNode = doc.getFirstChild();
        if (smartRuleNode != null) {
            NodeList topNodes = smartRuleNode.getChildNodes();
            for (int j = 0; j < topNodes.getLength(); j++) {
                Node curNode = topNodes.item(j);
                String curNodeName = curNode.getNodeName();

                if (Constants.LOG_VERBOSE)
                    Log.d(TAG, "Node Name: " + curNodeName);
                if (curNodeName.equalsIgnoreCase(RULEINFO)) {
                    NodeList children = curNode.getChildNodes();
                    for (int k = 0; k < children.getLength(); k++) {
                        Node node = children.item(k);
                        if (node.getNodeName().equalsIgnoreCase(TYPE)
                                && node.getFirstChild().getNodeValue() != null) {
                            node.getFirstChild().setNodeValue(source);
                        }
                    }
                    ruleXml = XmlUtils.getXmlTreeIn(smartRuleNode);
                    break;
                }
            }
        }

        return ruleXml;
    }
}
