/*
 * @(#)BaseContainer.java
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
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.motorola.contextual.smartrules.Constants;

import android.util.Log;

/**
 * This class contains the generic util methods to parse the xml and
 * populate the nodes in a hash map
 *
 * @author a21693
 *
 */
public abstract class BaseContainer extends HashMap<String, String> implements Constants{

    private static final long serialVersionUID = 632658906455754194L;
	private static final String TAG = BaseContainer.class.getSimpleName();
    protected Node mNode;

    /**
     * constructor
     *
     * @param node
     */
    BaseContainer(Node node) {
        mNode = node;

        // parse the Xml
        parse();
    }

    /**
     *
     * @return
     */
    Node getNode() {
        return mNode;
    }

    /** Loads the xml values from a node into HashMap
     *
     * @return
     */
    List<Node> loadValues() {
        List<Node> children = new ArrayList<Node>();
        if (getNode() != null) {
            NodeList nl = getNode().getChildNodes();

            for (int i = 0; i < nl.getLength(); i++) {
                Node n = null;
                if ((n = nl.item(i).getFirstChild()) != null) {
                    Node node =  nl.item(i);
                    if (!hasCDataChildNodes(node)) {
                        String name = node.getNodeName();
                        node.normalize();
                        this.put(name, n.getNodeValue());
                        if (LOG_DEBUG)
                            Log.d(TAG, TAG + "Added " + name + ", " + this.get(name) + ":"
                                    + n.getNodeType());
                    } else {
                        children.add(node);
                    }
                }
            }
        }
        return children;
    }

    /** Parses the tree starting from the base node
     *
     */
    protected void parse() {
        List<Node> children =  loadValues();
        // For nodes which have children
        for (Node child : children) {
            String name = child.getNodeName();
            if (LOG_DEBUG) Log.d(TAG, "Node name is " + name);
            put(name, getCdataValues(child).get(0));
        }
    }

    /** Checks whether the given node has child CDATA nodes.
     *
     * @param node
     * @return
     */
    protected final boolean hasCDataChildNodes(Node node) {
        boolean found = false;
        if (node.hasChildNodes()) {
            NodeList list = node.getChildNodes();
            for (int j=0; j < list.getLength(); j++) {
                Node subnode = list.item(j);
                if (subnode.getNodeType() == Node.CDATA_SECTION_NODE)
                {
                    if (LOG_DEBUG) Log.d(TAG, "subnode is " + subnode.getNodeName());
                    found = true;
                    break;
                }
            }
        }
        return found;

    }

    /** Returns the content of all the child CDATA nodes as a list.
     *
     * @param node
     * @return
     */
    protected final List<String> getCdataValues(final Node node) {

        List<String> contentList = new ArrayList<String>();
        NodeList list = node.getChildNodes();

        for (int j=0; j < list.getLength(); j++) {
            Node subnode = list.item(j);
            if (subnode.getNodeType() == Node.CDATA_SECTION_NODE)
            {
                if (LOG_DEBUG) Log.d(TAG, "subnode is " + subnode.getNodeName());
                String str = subnode.getNodeValue();
                contentList.add(str);
            }
        }
        return contentList;
    }

    /** Returns the integer value corresponding to a particular key.
     *
     * @param key
     * @param defaultValue
     * @return
     */
    int getIntValue(String key, int defaultValue) {
        int value = defaultValue;
        String strValue = get(key);
        if (strValue != null) {
            try {
                value = Integer.parseInt(strValue);
            } catch(NumberFormatException e) {
                if(LOG_DEBUG) Log.w(TAG, "Exception when parsing " + key);
            }
        }

        return value;
    }  

    /** Returns the long value corresponding to a particular key.
    *
    * @param key
    * @param defaultValue
    * @return
    */
    long getLongValue(String key, long defaultValue) {
        long value = defaultValue;
        String strValue = get(key);
        if (strValue != null) {
            try {
                value = Long.parseLong(strValue);
            } catch(NumberFormatException e) {
                if(LOG_DEBUG) Log.w(TAG, "Exception when parsing " + key);
            }
        }

        return value;
    }

    /** Returns the float value corresponding to a particular key.
    *
    * @param key
    * @param defaultValue
    * @return
    */
    float getFloatValue(String key, float defaultValue) {
        float value = defaultValue;
        String strValue = get(key);
        if (strValue != null) {
            try {
                value = Float.parseFloat(strValue);
            } catch(NumberFormatException e) {
                if(LOG_DEBUG) Log.w(TAG, "Exception when parsing " + key);
            }
        }

        return value;
    }
}
