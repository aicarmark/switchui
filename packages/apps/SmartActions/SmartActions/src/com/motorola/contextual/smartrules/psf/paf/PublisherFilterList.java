/*
 * @(#)ListFromXml.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21345        2012/03/26                   Initial version
 *
 */
package com.motorola.contextual.smartrules.psf.paf;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.util.Log;

/** Provides Black listed publishers and List of packages in white list
 *<code><pre>
 * CLASS:
 *     ListFromXml Extends ArrayList<String>
 *
 * RESPONSIBILITIES:
 *     Parse the xml in the give path and provide the list of Black listed providers or
 *     White listed packages
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class PublisherFilterList extends ArrayList<String> {

    private static final long serialVersionUID = -8544654801258005698L;


    interface Tags {
        String BLACKLIST = "blacklist";
        String WHITELIST = "whitelist";
        String PUBLISHER = "publisher";

        public interface Attrib {
            String NAME = "name";
        }
    }

    /**
     * Default constructor
     */
    public PublisherFilterList() {};

    /**
     * Constructor to parse the given xml and populate the list
     * @param xmlPath Path of xml file to parse
     */
    public PublisherFilterList(String xmlPath) {
        File f = new File(xmlPath);
        if (f != null && f.exists()) {
            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(f));
                load(new InputSource(is), this);
            } catch (FileNotFoundException e) {
                if (xmlPath.equals(PafConstants.BLACKLIST_DEFAULT_XML_FILE_PATH)) {
                    Log.e(PackageManagerPublisherList.TAG, PackageManagerPublisherList.TAG+".load - no default publisher blacklist in expected location:"+
                          PafConstants.BLACKLIST_DEFAULT_XML_FILE_PATH+" no packages blacklisted");
                }
                else {
                    e.printStackTrace();
                }
            } finally {
                try {
                    if(is != null) is.close();
                }  catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Log.e(PackageManagerPublisherList.TAG, PackageManagerPublisherList.TAG+".load: f="+xmlPath+" exists="+(f==null?"null":f.exists()) );
        }
    }

    /**
     * Loads list from the xml
     * @param is Inputstream of xml
     * @param list - List instance (instance of either BlackList or WhiteList )
     */
    public void load(InputSource is, PublisherFilterList list) {
        loadXml(is, list);
    }

    /** load XML from an input source, either URI or filepath, see the methods which drive this method
     *
     * @param is - input source
     * @param list - List instance (instance of either BlackLisy or WhiteList )
     */
    private synchronized void loadXml(InputSource is, PublisherFilterList list) {

        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser parser;
        try {
            parser = spf.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.setContentHandler(new ListContentHandler(list));
            reader.parse(is);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /** handle parsing out the XML file
     */
    private class ListContentHandler implements ContentHandler {
        private PublisherFilterList mInstList;
        private boolean mInsidelist;

        public ListContentHandler(PublisherFilterList list) {
            mInstList = list;
        }

        public void startElement(String uri, String localName,
                                 final String qName, Attributes attrs) throws SAXException {

            if (qName.toLowerCase().equals(Tags.BLACKLIST) ||
                    qName.toLowerCase().equals(Tags.WHITELIST))
                mInsidelist = true;
            else {
                if (qName.toLowerCase().equals(Tags.PUBLISHER)) {
                    String name = null, value=null;
                    for (int i=0; i<attrs.getLength(); i++) {
                        name = attrs.getLocalName(i);
                        if (name.toLowerCase().equals(Tags.Attrib.NAME) && mInsidelist) {
                            value = attrs.getValue(i);
                            if (value != null) {
                                mInstList.add(value);
                            }
                        }
                        if (PackageManagerPublisherList.LOG_DEBUG) Log.v(PackageManagerPublisherList.TAG, PackageManagerPublisherList.TAG+"("+i+"):"+name+"="+attrs.getValue(name) );
                    }
                }
            }
            if (PackageManagerPublisherList.LOG_DEBUG) Log.v(PackageManagerPublisherList.TAG, PackageManagerPublisherList.TAG+".startElement: insideFilterlist:"+mInsidelist+" locName="+localName+
                        " qName="+qName+" attrs:"+attrs.toString());
        }

        public void characters(char[] ch, int start, int length)
        throws SAXException {

        }

        public void endDocument() throws SAXException {

        }

        public void endElement(String uri, String localName, String qName)
        throws SAXException {

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