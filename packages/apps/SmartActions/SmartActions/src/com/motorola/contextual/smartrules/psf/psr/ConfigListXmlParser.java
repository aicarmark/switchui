/*
 * @(#)ConfigListXmlParser.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA MOBILITY INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21034        2012/03/27                   Initial version
 *
 */

package com.motorola.contextual.smartrules.psf.psr;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.motorola.contextual.smartrules.psf.PsfConstants;
import com.motorola.contextual.smartrules.psf.psr.ConfigList.Xml;

/**This class is for parsing the XML for command=list from publishers,
 * PSR=Publisher States Receiver
 * 
 *<code><pre>
 * CLASS:
 *     Extends DefaultHandler
 *
 * RESPONSIBILITIES:
 *     Parses the XML response for command=list
 *     Uses SAX Parser
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class ConfigListXmlParser extends DefaultHandler {
    private static final String TAG = PsfConstants.PSF_PREFIX + ConfigListXmlParser.class.getSimpleName();
    private List<ConfigItem> mConfigList = null;
    private ConfigItem mItem = null;
    private String mValue;

    ConfigListXmlParser(String xml) {
        mConfigList = new ArrayList<ConfigItem>();
        parseXml(xml);
    }

    /**
     * Returns a list of configItem after parsing the input XML
     * @return - List of configItem
     */
    public List<ConfigItem> getConfigList() {

        List<ConfigItem> returnList = new ArrayList<ConfigItem> ();

        for (ConfigItem configItem : mConfigList) {
            ConfigItem returnItem = new ConfigItem(configItem);
            returnList.add(returnItem);
        }

        return returnList;
    }

    @Override
    public void startElement(String uri, String localName,String qName,
                             Attributes attributes) throws SAXException {
        if (qName.equals(Xml.CONFIG_ITEMS)) {

        } else if (qName.equals(Xml.ITEM)) {
            mItem = new ConfigItem();
        } else if (qName.equals(Xml.CONFIG)) {
            mValue = "";
        } else if (qName.equals(Xml.DESCRIPTION)) {
            mValue = "";
        }
    }

    @Override
    public void endElement(String uri, String localName,
                           String qName) throws SAXException {

        if (qName.equals(Xml.ITEM)) {
            mConfigList.add(mItem);
        } else if (qName.equals(Xml.CONFIG)) {
            mItem.setConfig(mValue);
        } else if (qName.equals(Xml.DESCRIPTION)) {
            mItem.setDescription(mValue);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String current = new String(ch,start,length);
        mValue += current;
    }

    /**
     * Uses SAX Parser to parse the XML
     * @param xml - Xml to be parsed.  This can parse only the response to command=list
     */
    private void parseXml(String xml) {
        SAXParserFactory spf = SAXParserFactory.newInstance();

        try {
            SAXParser sp = spf.newSAXParser();
            sp.parse(new InputSource(new StringReader(xml)), this);
        } catch (Exception e) {
            Log.e(TAG, "Exception in parseXml " + xml + e.getMessage());
            e.printStackTrace();
        }

    }
}
