/*
 * @(#)RuleFilterlist.java
 *
 * (c) COPYRIGHT 2012-2013 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number     Brief Description
 * ------------- ---------- ------------- ------------------------
 * A21693        2012/04/02 NA            Initial version
 *
 */

package com.motorola.contextual.smartrules.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;

/**
 * This class parses the rule filter xml and provides methods to check if a rule
 * is present in a black list
 *
 * <code><pre>
 * CLASS:
 *  extends none
 *
 *  implements
 *      Constants - to access generic constants
 *
 * RESPONSIBILITIES:
 *  - Parse Filter XML
 *  - Provide util methods to validate rule keys
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 *  See each method.
 * </pre></code>
 */
public class RuleFilterList implements Constants {

    private interface Tags {
        String BLACKLIST = "blacklist";
        String RULE_KEY = "rulekey";

        public interface Attrib {
            String NAME = "name";
        }
    }

    private static final String RULE_BLACKLIST_DEFAULT_XML_FILE_PATH = "/system/etc/smartactions/com.motorola.smartactions_ruleblacklist.xml";

    private static RuleFilterList mInstRuleFilterlist;
    private static final String TAG = RuleFilterList.class.getSimpleName();
    private FilterList mBlacklist = new FilterList();

    private boolean mLoadedBlackList = false;
    private boolean mIsBlackListFilePresent = false;

    /**
     * private constructor so that no one can instantiate
     * this class
     */
    private RuleFilterList() {}

    /**
     * Returns the instance of this class
     *
     * @return - Returns the instance of this class
     */
    public static synchronized RuleFilterList getInstance() {
        if (mInstRuleFilterlist == null) {
            mInstRuleFilterlist = new RuleFilterList();
        }
        return mInstRuleFilterlist;
    }

    /**
     * This class parses the Filter list XML and populates the rules keys in a
     * HashSet
     *
     * @author a21693
     *
     */
    private static class FilterList extends HashSet<String> {

        private static final long serialVersionUID = -3785700027188974847L;

        /**
         *
         * @param is
         *            - input source
         * @param fl
         *            - FilterList instance (instance of either BlackList )
         */
        public void load(InputSource is, FilterList fl) {
            loadXml(is, fl);
        }

        /**
         * load XML from an input source, either URI or filepath, see the
         * methods which drive this method
         *
         * @param is
         *            - input source
         * @param fl
         *            - FilterList instance (instance of either BlackList )
         */
        private synchronized void loadXml(InputSource is, FilterList fl) {

            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser parser;
            try {
                parser = spf.newSAXParser();
                XMLReader reader = parser.getXMLReader();
                reader.setContentHandler(new FilterContentHandler(fl));
                reader.parse(is);

            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * handle parsing out the XML file
         *
         * @author a21693
         */
        private static class FilterContentHandler implements ContentHandler {
            private FilterList mInstFilterList;
            private boolean mInsideFilterlist;

            public FilterContentHandler(FilterList fl) {
                mInstFilterList = fl;
            }

            /**
             * This method populates the rule keys in a Hash Set
             *
             * (non-Javadoc)
             *
             * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
             *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
             */
            public void startElement(String uri, String localName,
                    final String qName, Attributes attrs) throws SAXException {

                if (qName.toLowerCase().equals(Tags.BLACKLIST))
                    mInsideFilterlist = true;
                else {
                    if (qName.toLowerCase().equals(Tags.RULE_KEY)) {
                        String name = null, value = null;
                        for (int i = 0; i < attrs.getLength(); i++) {
                            name = attrs.getLocalName(i);
                            if (name.toLowerCase().equals(Tags.Attrib.NAME)
                                    && mInsideFilterlist) {
                                value = attrs.getValue(i);
                                if (value != null) {
                                    mInstFilterList.add(value);
                                }
                            }
                            if (LOG_DEBUG)
                                Log.v(TAG, TAG + "(" + i + "):" + name + "="
                                        + attrs.getValue(name));
                        }
                    }
                }
                if (LOG_DEBUG)
                    Log.v(TAG, TAG + ".startElement: insideFilterlist:"
                            + mInsideFilterlist + " locName=" + localName
                            + " qName=" + qName + " attrs:" + attrs.toString());
            }

            public void characters(char[] ch, int start, int length)
                    throws SAXException {
            }

            public void endDocument() throws SAXException {
            }

            public void endElement(String uri, String localName, String qName)
                    throws SAXException {
                if (qName.toLowerCase().equals(Tags.BLACKLIST))
                    mInsideFilterlist = false;
                if (LOG_DEBUG)
                    Log.v(TAG, TAG + ".endElement:" + qName
                            + " insideFilterlist=" + mInsideFilterlist);
            }

            public void endPrefixMapping(String prefix) throws SAXException {
            }

            public void ignorableWhitespace(char[] ch, int start, int length)
                    throws SAXException {
            }

            public void processingInstruction(String target, String data)
                    throws SAXException {
            }

            public void setDocumentLocator(Locator locator) {
            }

            public void skippedEntity(String name) throws SAXException {
            }

            public void startDocument() throws SAXException {
            }

            public void startPrefixMapping(String prefix, String uri)
                    throws SAXException {
            }

        }
    }

    /**
     * check to see if a rule key is blacklisted.
     *
     * @param context
     *            - context
     * @param ruleKey
     *            - the rule key to check for the blacklist
     * @return true if on the blacklist
     */
    public synchronized boolean isBlacklisted(Context context, final String ruleKey) {
        boolean ret = false;
        if (!mLoadedBlackList) {
            if (LOG_DEBUG)
                Log.d(TAG, "Black list file not yet loaded, attempting to load");
            load(context, RULE_BLACKLIST_DEFAULT_XML_FILE_PATH, mBlacklist);
            mLoadedBlackList = true;
        }
        if (mIsBlackListFilePresent) {
            ret = mBlacklist.contains(ruleKey);
            if (LOG_DEBUG)
                Log.d(TAG, "Black list :" + ruleKey + " " + ret);
        }
        return ret;
    }


    /**
     * load the XML into the blacklist
     *
     * @param Context
     * @param filepath
     *            - the file path to load the XML from. See File class for more
     *            information.
     * @param toLoad
     *            the blacklist FilterList - IT MAY BE SET TO
     *            AN EMPTY STRING TO LOAD THE DEFAULT LOCATION blacklist file.
     */
    private void load(final Context context, String filepath,
            FilterList toLoad) {

        File f = new File(filepath);
        if (f != null && f.exists()) {
            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(f));
                toLoad.load(new InputSource(is), toLoad);

                mIsBlackListFilePresent = true;

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if(is != null) is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Log.e(TAG, TAG + ".load: f=" + filepath + " exists="
                    + (f == null ? "null" : f.exists()));
        }
    }
}