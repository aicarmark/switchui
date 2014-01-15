/*
 * @(#)FileUtil.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18172        2011/01/18 NA				  Initial version
 * A18172        2011/02/21 NA				  Updated with review comments
 * 											  from Craig
 *
 */

package com.motorola.contextual.smartrules.rulesimporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.SQLiteManager;
import com.motorola.contextual.smartrules.db.table.ModalTable;
import com.motorola.contextual.smartrules.psf.table.LocalPublisherTable;
import com.motorola.contextual.smartrules.util.Util;


/** This class contains the utilities used by RulesImporter to read the xml
 *
 *
 *<code><pre>
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
 *</pre></code>
 */
public class FileUtil implements  Constants, XmlConstants, DbSyntax {

    public static final String TAG = FileUtil.class.getSimpleName();
    private static final String ETC = File.separator + "etc" + File.separator;
    private static final String KEY_NODE_EXPRESSION = "/RULES/SMARTRULE[1]/RULEINFO/PrereqSmartActionsDbVersion";

    
    private static final int BUFSIZE = 8192;


    /**
     * This method is a wrapper function to read from the xml
     * Asset folder
     *
     * It also filters out the NEXT_CONTENT and provides only
     * SMARTRULE
     *
     * @param Context - application Context
     * @param  fromSdcard - true if the xml is read from /sdcard/rules.d
     *                      false if the xml is read from Assets directory
     * @return String - string formed from the xml which only relates to SMARTRULE
     */
    public static String readRulesImporterFile (Context mContext,
            final boolean fromSdcard) {

        if (LOG_DEBUG) Log.d(TAG,"Fn: readRulesImporterFile");

        // Read Rules files from SDCARD or Asset Folder
        String xml = readXmlFromFile(mContext, fromSdcard);

        return processXmlAndSendIntents(mContext, xml);

    }

    /** This method provides the XML string from the respective
     * folder whether it is sdcard, /system/etc/rules.d or asset directory
     *
     * @param mContext
     * @param fromSdcard
     * @return String - XML String which contains both NEXT_CONTENT &
     * 					SMARTRULE
     */
    public static String readXmlFromFile (Context mContext,
                                          final boolean fromSdcard) {
        if (LOG_DEBUG) Log.d(TAG,"Fn: getTheXmlString");
        if(fromSdcard) {
            // Read the XML file from SDCARD
            return readFile(openRulesImporterFileFromSdCard(false));
        } else {
        	//Read XML file from /system/etc/rules.d if it exists
        	String fileName = Environment.getRootDirectory().getPath()+ETC+RULES_FOLDER_NAME+File.separator+RULES_IMPORTER_FILE_NAME;
        	if (LOG_DEBUG) Log.d(TAG,"Fn: getTheXmlString from " + fileName);
        	File ruleFile = new File(fileName);
             if (ruleFile.exists()) 
            	 return readFile(ruleFile);
            // Read the XML file from the assets directory of the application
             if (LOG_DEBUG) Log.d(TAG,"Fn: getTheXmlString from assets");
            return readFilefromAssets(mContext);
        }
    }

    /**
     * Reads the xml from the specified file path
     * and converts it into a string
     *
     * @param fileInput - File Path
     * @return String - string read from the XML
     */
    @SuppressWarnings("unused")
	public static String readFile(final File fileInput) {

        if (LOG_DEBUG) Log.d(TAG,"Fn: readFile");

        StringBuilder result = new StringBuilder();

        // Read the file
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileInput);
            if (fis != null) {
                byte[] buffer = new byte[BUFSIZE];

                int readLen = 0;
                while ((readLen = fis.read(buffer, 0, buffer.length)) != INVALID_KEY) {
                    result.append(new String(buffer, 0, readLen));
                }
            } else {
                Log.e(TAG,"New File Input Stream Failed");
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG,"Fn: FileNotFoundException");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG,"Fn: IOException");
            e.printStackTrace();
        }
        finally {

            try {
                if (fis != null) {
                    fis.close();
                }
            }
            catch (IOException e) {
                Log.e(TAG,"Fn: IOException");
                e.printStackTrace();
            }
        }
        if (LOG_DEBUG) Log.d(TAG, TAG+" .readRuleFile - returning - "
                                +result.toString());

        return result.toString();
    }



    /**
     * This method is used to create file path for a specific
     * location /sdcard/rules.d/rulesimporter.rules
     * @param forWrite - whether the file is opened
     * 			         for writing
     * @return File  - File path
     */
    public static File openRulesImporterFileFromSdCard(final boolean forWrite) {

        if (LOG_DEBUG) Log.d(TAG,"Fn: openRulesImporterFileFromSdCard");

        File result = null;
        // Open file for reading
        try {

            File vol = (Process.myUid() == Process.SYSTEM_UID) ? Environment.getDataDirectory() 
 						: Environment.getExternalStorageDirectory();
            String dirName = vol.getAbsolutePath()+File.separator+RULES_FOLDER_NAME;

            File dir = new File(dirName);
            boolean status = dir.mkdirs();

            // putting this under LOG_DEBUG creates FindBug issue for status DEAD_STORE
            // TODO: File a false positive for this
            if (LOG_INFO) Log.i(TAG, dirName + " mkdirs status is " + status);

            File file = new File(dirName, RULES_IMPORTER_FILE_NAME);
            if (forWrite) {
                // try to erase any existing file
                if (file.delete()) {
                    if (!file.createNewFile()) {
                        if (LOG_DEBUG) Log.d(TAG,"File already exist");
                    }
                } else {
                    Log.e(TAG,"File deletion failed");
                }
            }
            result = file;

        } catch (FileNotFoundException e) {
            Log.e(TAG,"FileNotFoundException");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG,"IOException");
            e.printStackTrace();
        } catch (SecurityException e) {
            Log.e(TAG,"SecurityException");
            e.printStackTrace();
        }

        return result;
    }
   
    /**  Reads the xml located in the asset directory
     *  and then convert it into  a string
     *  @param  Context - application context
     *  @return String - String
     *
     */
    public  static String readFilefromAssets(Context mContext) {

        if (LOG_DEBUG) Log.d(TAG,"Fn: readFilefromAssets");

        StringBuilder result = new StringBuilder();

        // Read the file
        InputStream in = null;
        AssetManager assetManager = mContext.getAssets();

        try {
            in = assetManager.open(RULES_IMPORTER_FILE_NAME);

            if (in != null) {
                byte[] buffer = new byte[BUFSIZE];
                int read = 0;
                while((read = in.read(buffer)) != INVALID_KEY) {
                    result.append(new String(buffer, 0, read));
                }
                if (in!=null) in.close();
            } else {
                Log.e(TAG,"Opening Asset Manager Failed");
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG,"FileNotFoundException");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG,"IOException");
            e.printStackTrace();
        }

        if (LOG_DEBUG) Log.d(TAG, TAG+" .readRuleFile - returning - "+result.toString());

        return result.toString();
    }

    /**
     * Parses the XML from Server which can have SMARTRULES as well as NEXT_CONTENT
     *
     * SMARTRULES can accommodate any RULES as defined in the SOURCE
     * NEXT_CONTENT will only have the template for the Suggested Rules which can
     * be changed dynamically during run time based on the Rule to Infer
     *
     * Sample XML format which can be placed in SDCARD or Asset Folder
     *
     * <RULES>
     * .........
     * <SMARTRULE>
     * </SMARTRULE>
     * ..........
     * <NEXT_CONTENT>
     * </NEXT_CONTENT>
     * ...........
     * <TIMEFRAMES>
     * </TIMEFRAMES>
     * </RULES>
     *
     * If the Rules has got NEXT_CONTENT, such rules will be collated
     * and send to Inference Manager to process.
     *
     * If the Rules from the Server has SMARTRULE, such rules will be collated and
     * returned to the caller.
     *
     *
     * @param context - Context to work with
     * @param xmlString - XML String which contains both NEXT_CONTENT & SMARTRULE
     *  @return String - XML String related to SMARTRULE
     */
    public static String processXmlAndSendIntents(Context context,final String xmlString) {

        if (LOG_DEBUG) Log.d(TAG,"Fn: processXmlAndSendIntents");

        if(Util.isNull(xmlString)){
            Log.e(TAG, "Input Xml string is null");
            return xmlString;
        }

        // To track if there is atleast one dot Next Rules present in the XML
        // from the Server
        boolean hasDotNextRules = false;
        // <ROOT> Node is added to the XML send to the Inference Manager
        boolean addedTheRootNode = false;
        // To track if there is atleast one Smart Rules is present in the XML
        // from the Server
        boolean hasSmartRules = false;

        // To Collate dot Next Rules for Inference Manager
        StringBuilder dotNextXml = new StringBuilder();

        // To Collate Smart Rules for Rules Importer
        StringBuilder smartRuleString = new StringBuilder();

        // To Collate TimeFrames for Time Frames
        String tfDataString = null;

        // To Collate Locations Data
        String locDataString = null;

        // To Collate Calendar events Data
        String calendarEventsDataString = null;

        String smartRuleXml = null;

        String deleteSectionString = null;

        // Get all child nodes from the parent Node <RULE>
        NodeList nodes = getAllRelevantNodesFromXml(xmlString);

        if(nodes != null) {
            for ( int i = 0; i < nodes.getLength(); i++ ) {
                Node curNode = nodes.item(i);
                String curNodeName = curNode.getNodeName();

                if (curNodeName.equalsIgnoreCase(SMARTRULE_NODE)) {
                    smartRuleString.append(getXmlTreeIn(curNode));
                    hasSmartRules = true;
                } else if(curNodeName.equalsIgnoreCase(NEXT_CONTENT)) {
                    if(!addedTheRootNode) {
                        dotNextXml.append(ROOT_START_TAG);
                        addedTheRootNode = true;
                    }
                    dotNextXml.append(getXmlTreeIn(curNode));
                    hasDotNextRules = true;
                } else if(curNodeName.equalsIgnoreCase(DELETE_RULES_NODE)) {

                    deleteSectionString = getXmlTreeIn(curNode);
                } else if(curNodeName.equalsIgnoreCase(TIMEFRAMES_NODE)) {
                    Node node = curNode.getFirstChild();
                    if (node != null) {
                        tfDataString = node.getNodeValue();
                    }
                } else if(curNodeName.equalsIgnoreCase(LOCATION_NODE)) {
                    /*
                     * We need to normalize this as the presence of &amp; was splitting the node
                     * into multiple siblings. Normalize merges all the sibling nodes.
                     */
                    curNode.normalize();
                    Node node = curNode.getFirstChild();
                    if (node != null) {
                        locDataString = node.getNodeValue();
                    }
                } else if (curNodeName.equalsIgnoreCase(CALENDAR_EVENTS_NODE)) {
                    Node node = curNode.getFirstChild();
                    if (node != null) {
                        calendarEventsDataString = node.getNodeValue();
                    }
                }
            }

            //Send the dot Next Rule XML to Inference Manager
            if (hasDotNextRules) {
                dotNextXml.append(ROOT_END_TAG);
                if (LOG_DEBUG) Log.d(TAG,"dotNextXmls :"+dotNextXml.toString());
                Intent intent1 = new Intent(LAUNCH_INFERENCE_MANAGER_FOR_DOTNEXT);
                intent1.putExtra(EXTRA_DOT_NEXT_CONTENT,dotNextXml.toString());
                context.sendBroadcast(intent1);
            }

            //Send the Smart Rule XML to RulesImporter
            if(hasSmartRules) {
                if (LOG_DEBUG) Log.d(TAG,"smartRulesString :"+smartRuleString.toString());

                smartRuleXml  = smartRuleString.toString();
            }

            if((tfDataString == null) || (tfDataString.length() <= 0)){
                if(LOG_DEBUG) Log.d(TAG, "No timeframa data in the XML");
            } else {
                // Send this to TimeFrame module
                if (LOG_DEBUG) Log.d(TAG," Sending Intent to TimeFrame Module with extra : " + tfDataString);
                Intent tfIntent = new Intent(TIMEFRAME_IMPORT_ACTION);
                tfIntent.putExtra(EXTRA_TIMEFRAME_DATA, tfDataString);
                context.sendBroadcast(tfIntent);
            }

            if(locDataString == null) {
                if(LOG_DEBUG) Log.d(TAG, "No location data in the XML");
            } else {
                // Send this to location module
                if (LOG_DEBUG) Log.d(TAG," Sending Intent to location Module with extra : " + locDataString);

                Intent locIntent = new Intent(LOCATION_IMPORT_ACTION);
                locIntent.putExtra(EXTRA_LOCATION_DATA, locDataString);
                context.sendBroadcast(locIntent);
            }

            if (calendarEventsDataString == null) {
                if (LOG_DEBUG) {
                    Log.d(TAG, "No calendar events data in the XML");
                }
            } else {
                // Send this to calendar events module
                if (LOG_DEBUG) {
                    Log.d(TAG,
                            "Sending Intent to calendar events Module with extra : "
                                    + calendarEventsDataString);
                }
                Intent calendarEventsIntent = new Intent(
                        CALENDAR_EVENTS_IMPORT_ACTION);
                calendarEventsIntent.putExtra(EXTRA_CALENDAR_EVENTS_DATA,
                        calendarEventsDataString);
                context.sendBroadcast(calendarEventsIntent);
            }

            if(deleteSectionString == null) {
                if(LOG_DEBUG) Log.d(TAG, "No clean up needed!");
            } else {
                // invoke rules cleanup module
                if (LOG_DEBUG) Log.d(TAG,"Starting rules clean-up module");
                new RulesDeleter(context).startRulesCleaner(deleteSectionString);

                // Write the parsed delete xml into the shared preference.
                SharedPreferences prefsnew = context.getSharedPreferences(DELETE_RULES_SHARED_PREFERENCE,
                        Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefsnew.edit();
                editor.clear();
                editor.putString(DELETE_RULES_XML, deleteSectionString);
                editor.commit();
            }
        } else {
            Log.e(TAG, "Nodes is null");
        }

        return smartRuleXml;
    }

    /**
     * Parses the VERSION node from the XML which contains SMARTRULES as well as
     * NEXT_CONTENT
     *
     *
     *
     * <RULES>
     * <VERSION>1.0</VERSION>
     * .........
     * <SMARTRULE>
     * </SMARTRULE>
     * ..........
     * <NEXT_CONTENT>
     * </NEXT_CONTENT>
     * ...........
     * </RULES>
     *
     * @param xmlString - XML String which contains VERSION, NEXT_CONTENT & SMARTRULE
     * @return float - Provides the version number in the XML
     */
    public static float getTheXmlVersion(final String xmlString) {

        if (LOG_DEBUG) Log.d(TAG,"Fn: getTheXmlVersion");

        float xmlVersion = DEFAULT_XML_VERSION;

        // Get all child nodes from the parent Node <RULE>
        NodeList nodes = getAllRelevantNodesFromXml(xmlString);

        if(nodes != null) {
            for ( int i = 0; i < nodes.getLength(); i++ ) {
                Node curNode = nodes.item(i);
                String curNodeName = curNode.getNodeName();

                if(LOG_DEBUG) Log.d(TAG,"Node Name: "+curNodeName);

                String curNodeValue = null;

                if (curNodeName.equalsIgnoreCase(XML_VERSION_NODE) &&
                        curNode.getFirstChild() != null &&
                        ((curNodeValue = curNode.getFirstChild().getNodeValue()) !=null)) {

                    if (LOG_DEBUG) Log.d(TAG,"Node Value "+curNodeValue);

                    try {
                        xmlVersion = Float.valueOf(curNodeValue);
                    } catch(NumberFormatException e) {
                        Log.w(TAG, "Exception when converting Xml Version into float");
                    }

                    if (LOG_DEBUG) Log.d(TAG,"Xml Version : "+xmlVersion);
                    break;
                }
            }
        } else {
            Log.e(TAG, "Nodes is null");
        }

        return xmlVersion;
    }

    /** Get the XML Tree inside a node
     *
     * @param node - Node containing the tree
     * @return - Entire tree under node as a String
     */
    public static String getXmlTreeIn(final Node node) {

        if (LOG_DEBUG) Log.d(TAG,"Fn: getXmlTreeIn");
        String result = EMPTY_STRING;

        if(node == null) return result;

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
            Log.e(TAG, "Unable to get XML tree inside " +
                  node.getNodeName() + " " + e.getMessage());
        }
        return result;
    }

    /**
     * Get all the relevant child nodes from the XML
     * @param ruleXml - contents in XML
     * @return - all nodes
     */
    public static NodeList getAllRelevantNodesFromXml(final String ruleXml) {
        if (LOG_DEBUG) Log.d(TAG, "Entered getAllRelevantNodesFromXml for :" + ruleXml);
        Document doc = getParsedDoc(ruleXml);

        if(doc!= null) {
            NodeList nodes = getAllNodeNamesFromXml(doc);
            return nodes;
        } else {
            Log.e(TAG,"doc is null");
            return null;
        }
    }

    /** Get the XML doc for an XML
     *
     * @param ruleXml - contents in XML
     * @return - xml Document
     */
    public static Document getParsedDoc(final String ruleXml) {

        if(Util.isNull(ruleXml)) return null;

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
            doc = db.parse(new InputSource( new StringReader( ruleXml )));
        } catch (SAXException e) {
            Log.e(TAG, "XML Syntax error");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "IO Exception");
            e.printStackTrace();
        }
        return doc;
    }

    /**Get all the Child Nodes within <RULES> node
     *
     * @param doc - XML document
     * @return - get all nodes under tag RULES
     */
    public static NodeList getAllNodeNamesFromXml(Document doc) {
        NodeList nodes = doc.getElementsByTagName(RULES).item(0).getChildNodes();

        if (LOG_DEBUG){
            Element intElement = (Element) doc.getElementsByTagName(RULES).item(0);
            String value = intElement.getChildNodes().item(0).getNodeValue();

            if (LOG_DEBUG) Log.d(TAG,"getAllNodeNamesFromXml : " + value);
        }

        return nodes;
    }

    /**
     * Translates only Suggestion freeflow text
     * @param context   - Context to work with
     * @param originalText - English original text 
     * @return - translated text
     */
    public static String translateSuggestionText(Context context, String originalText) {
        StringBuffer translatedSuggestedText = new StringBuffer(S_SUGGESTION_CONTENT);
        Document doc = null;
        String string = "string";

        if (originalText != null) {
            doc = getParsedDoc(originalText);
        }
        if (originalText == null || doc == null) {
            if (LOG_DEBUG) Log.d(TAG,"No XML Content in SUGGESTION_FREEFLOW tag");
            translatedSuggestedText = null;
        } else {
            if (LOG_DEBUG) Log.d(TAG,"SUGGESTION_FREEFLOW=" + originalText);

            NodeList nodes = doc.getElementsByTagName(SUGGESTION_CONTENT).item(0).getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {

                Node curNode = nodes.item(i);
                if(curNode == null) continue;

                String curNodeName = curNode.getNodeName();
                if(curNodeName == null) continue;

                if (curNodeName.equals(SUGGESTION_ICON) && curNode.getFirstChild() != null) {
                    String icon = curNode.getFirstChild().getNodeValue();
                    translatedSuggestedText.append(S_SUGGESTION_ICON + icon + E_SUGGESTION_ICON);
                    if(LOG_DEBUG) Log.d(TAG,"SUGGESTION_ICON DATA: "+ icon);

                } else if (curNodeName.equals(SUGGESTION_DESC) && curNode.getFirstChild() != null) {
                    String desc = curNode.getFirstChild().getNodeValue();
                    if (desc.startsWith(context.getString(R.string.rulesxml_prefix)))
                    	desc = context.getString(context.getResources().getIdentifier(desc, string, context.getPackageName()));
                    translatedSuggestedText.append(S_SUGGESTION_DESC + desc + E_SUGGESTION_DESC);
                    if(LOG_DEBUG) Log.d(TAG,"SUGGESTION_DESC DATA: "+ desc);

                } else if (curNodeName.equals(PROLOGUE) && curNode.getFirstChild() != null) {
                    String prologue = curNode.getFirstChild().getNodeValue();
                    if (prologue.startsWith(context.getString(R.string.rulesxml_prefix)))
                        prologue = context.getString(context.getResources().getIdentifier(prologue, string, context.getPackageName()));
                    translatedSuggestedText.append(S_PROLOGUE + prologue + E_PROLOGUE);
                    if(LOG_DEBUG) Log.d(TAG,"PROLOGUE DATA: "+ prologue);

                } else if (curNodeName.equals(EPILOGUE) && curNode.getFirstChild() != null) {
                    String epilogue = curNode.getFirstChild().getNodeValue();
                    if (epilogue.startsWith(context.getString(R.string.rulesxml_prefix)))
                        epilogue = context.getString(context.getResources().getIdentifier(epilogue, string, context.getPackageName()));
                    translatedSuggestedText.append(S_EPILOGUE + epilogue + E_EPILOGUE);
                    if(LOG_DEBUG) Log.d(TAG,"EPILOGUE DATA: "+ epilogue);
                } else if (curNodeName.equals(BODY)){
                    String xml = FileUtil.getXmlTreeIn(curNode);
                    if(LOG_DEBUG) Log.d(TAG,"Print the BODY XML" + xml);
                    if(xml.equals(EMPTY_STRING)) continue;

                    Document bodyDoc = getParsedDoc(xml);
                    if (bodyDoc == null){ 
                        if (LOG_DEBUG) Log.d(TAG,"No XML Content in BODY tag");
                        continue;
                    }
                    NodeList bodyChildNodes = bodyDoc.getElementsByTagName(BODY).item(0).getChildNodes();
                    translatedSuggestedText.append(S_BODY);
                    for (int j = 0; j < bodyChildNodes.getLength(); j++) {
                        Node bodyCurNode = bodyChildNodes.item(j);
                        if(bodyCurNode == null) continue;

                        String bodyCurNodeName = bodyCurNode.getNodeName();
                        if(bodyCurNodeName.equals(ITEM) && bodyCurNode.getFirstChild() != null){
                            String body = bodyCurNode.getFirstChild().getNodeValue();
                            if (body.startsWith(context.getString(R.string.rulesxml_prefix)))
                                body = context.getString(context.getResources().getIdentifier(body, string, context.getPackageName()));
                            translatedSuggestedText.append(S_ITEM + body + E_ITEM);
                            if(LOG_DEBUG) Log.d(TAG,"Item " + j + ": " + body);
                        } else if(bodyCurNodeName.equals(BULLET_ITEM)) {
                        	String itemXml = FileUtil.getXmlTreeIn(bodyCurNode);
                        	if (LOG_DEBUG) Log.d(TAG,"Print the BULLET_ITEM XML" + itemXml);
                            if(itemXml.equals(EMPTY_STRING)) continue;

                            Document itemDoc = FileUtil.getParsedDoc(itemXml);
                            if (itemDoc == null) {
                                Log.e(TAG,"NULL return from getParsedDoc");
                                continue;
                        }

                            NodeList itemList = itemDoc.getElementsByTagName(BULLET_ITEM);
                            if(itemList == null){
                                Log.e(TAG, "nodelist is null");
                                continue;
                    }

                            if (LOG_DEBUG) Log.d(TAG,"BULLET_ITEM XML : itemList is " + itemList.toString());
                            // item 0 should not be null
                            NodeList itemChildNodes = itemList.item(0).getChildNodes();
                            String bulletItemImage = null;
                            String bulletItemText = null;
                            for (int k = 0; k < itemChildNodes.getLength(); k++) {
                            	Node itemCurNode = itemChildNodes.item(k);
                            	if(itemCurNode == null) continue;

                                String itemCurNodeName = itemCurNode.getNodeName();
                                if (itemCurNodeName.equals(ICON) && itemCurNode.getFirstChild() != null)
                                    bulletItemImage = itemCurNode.getFirstChild().getNodeValue();
                                else if(itemCurNodeName.equals(DESCRIPTION) && itemCurNode.getFirstChild() != null) {
                                	bulletItemText = itemCurNode.getFirstChild().getNodeValue();
                                	if (bulletItemText.startsWith(context.getString(R.string.rulesxml_prefix)))
                                		bulletItemText = context.getString(context.getResources().getIdentifier(bulletItemText, string, context.getPackageName()));
                                }
                            }
                            
                            if (LOG_DEBUG) Log.d(TAG,"BULLET_ITEM XML : bulletItemImage is " + bulletItemImage);
                            if (LOG_DEBUG) Log.d(TAG,"BULLET_ITEM XML : bulletItemText is " + bulletItemText);
                            if (bulletItemImage == null || bulletItemText == null) {
                            	Log.e(TAG, "nodelist is null");
                                continue;
                            }
                            translatedSuggestedText.append(S_BULLET_ITEM);
                            translatedSuggestedText.append(S_ICON);
                            translatedSuggestedText.append(bulletItemImage);
                            translatedSuggestedText.append(E_ICON);
                            translatedSuggestedText.append(S_DESC);
                            translatedSuggestedText.append(bulletItemText);
                            translatedSuggestedText.append(E_DESC);
                            translatedSuggestedText.append(E_BULLET_ITEM);   
                        }
                    }
                    translatedSuggestedText.append(E_BODY);
                }
            }
            translatedSuggestedText.append(E_SUGGESTION_CONTENT);
        }
        
        return (translatedSuggestedText != null ? translatedSuggestedText.toString() : null);
    }
    
    /**
     * This method helps to write the data to a file in the internal storage
     * 
     * @param context - Application Context
     * @param data - data which needs to written into the file
     * @param fileName - data will be stored in this file name
     */
    
    public static void writeToInternalStorage(Context context,
    										  final String data,
    										  final String fileName) {
    	
    	if (LOG_DEBUG) Log.d(TAG, "Entered writeToInternalStorage");
    	
    	FileOutputStream fos = null;
    	   
		try {
			fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
			if (fos != null){
		        fos.write(data.getBytes());
			}else{
				 Log.e(TAG,"New File Output Stream Failed");
			}
		 } catch (FileNotFoundException e) {
	            Log.e(TAG,"Fn: FileNotFoundException");
	            e.printStackTrace();
	     } catch (IOException e) {
	            Log.e(TAG,"Fn: IOException");
	            e.printStackTrace();
	     } finally {
	            try {
	                if (fos != null) {
	                    fos.close();
	                }
	            }
	            catch (IOException e) {
	                Log.e(TAG,"Fn: IOException");
	                e.printStackTrace();
	            }
	     }
    }
    
    /**
     * This methods helps to read the data from a specified file in 
     * the internal storage.
     * 
     * @param context - Application context
     * @param fileName - read from this file
     * @return data in the file
     */
    public static String readFromInternalStorage(Context context, 
    										     final String fileName) {

    	if (LOG_DEBUG) Log.d(TAG, "readFromInternalStorage");
    	
        StringBuilder result = new StringBuilder();
        
    	FileInputStream fis = null;
    		
        try {
        	fis = context.openFileInput(fileName);
            if (fis != null) {
                byte[] buffer = new byte[BUFSIZE];

                int readLen = 0;
                while ((readLen = fis.read(buffer, 0, buffer.length)) != INVALID_KEY) {
                    result.append(new String(buffer, 0, readLen));
                }
            } else {
                Log.e(TAG,"New File Input Stream Failed");
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG,"Fn: FileNotFoundException");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG,"Fn: IOException");
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            }
            catch (IOException e) {
                Log.e(TAG,"Fn: IOException");
                e.printStackTrace();
            }
        }
        
        if (LOG_DEBUG) Log.d(TAG, TAG+" .readFromInternalStorage - returning - "
                                +result.toString());

        return result.toString();
    }
    
    /** This method helps to read the Modality data from psf database
     *  
     * @param context
     * @param pubKey
     * @return int - Modality of a publisher
     */
	public static int getModalForPublisher(Context context, String pubKey){
		
		if (LOG_DEBUG) Log.d(TAG,"getModalForPublisher");
		
		Cursor cursor = null;
		int modality = ModalTable.Modality.UNKNOWN;
        ContentResolver cr = context.getContentResolver();
        String whereClause = LocalPublisherTable.Columns.PUBLISHER_KEY + EQUALS + Q + pubKey + Q;
        String[] projStr = new String[] {LocalPublisherTable.Columns.STATE_TYPE};
        try {
        	cursor = cr.query(LocalPublisherTable.CONTENT_URI, projStr, 
        												whereClause, null, null);
            if(cursor != null && cursor.moveToFirst()) {
            	modality = cursor.getInt(
            			cursor.getColumnIndexOrThrow(LocalPublisherTable.Columns.STATE_TYPE));
            }
        } catch(Exception e) {
            Log.e(TAG, "Error in Query " + e.getMessage());
            e.printStackTrace();
        } finally {
            if(cursor != null) cursor.close();
        }
		
	    if(LOG_DEBUG) Log.d(TAG, "modality for pub Key : "+pubKey+" Modality "+modality);
	    
	    return modality;
	}
	
	/** This method helps to get the node value for a node
	 * 
	 * @param xml - xml to be parsed
	 * @param keyNodeExpression - Key Node Expression whose value needs to be retrieved
	 * @return String - Node value for the expression
	 */
	 public static String getNodeValueFromXml(String xml, String keyNodeExpression){
		    String nodeValue = null;
		    
	    	if (Util.isNull(xml)) return nodeValue;
	    	
	    	XPath xpath = XPathFactory.newInstance().newXPath();
	    	
	    	Document document = getParsedDoc(xml);
	    	if (document != null){
	    		 try {
		    		 Node keyNode = (Node)xpath.evaluate(keyNodeExpression, document,
													XPathConstants.NODE);
		    		 if (keyNode != null && keyNode.getNodeType() == Node.ELEMENT_NODE
		    				 			&& (keyNode.getFirstChild() != null)) {
		    			 nodeValue = keyNode.getFirstChild().getNodeValue();
		    			 if (LOG_DEBUG)  Log.i(TAG, "Key Node Expresssion :"+keyNodeExpression 
		    					 +" node Value = " +nodeValue);
		    		 } else {
	                    Log.e(TAG, "keyNode is null for expression : "+keyNodeExpression);
		    		 }
	            } catch (XPathExpressionException e) {
	            	   e.printStackTrace();
	            } catch (IllegalArgumentException e) {
	                    e.printStackTrace();
	            }
	    	 }else{
	    		 Log.e(TAG,"doc is null");
	    	 }
	    	
	    	return nodeValue;
	    }
	 
	    /** This method helps to clarify whether the XML DB Version is 
	     *  compatible with the SQLite DB Version
	     * 
	     * @param xml - xml to be parsed for the DB version
	     * @return boolean XML DB version
	     */
	    public static boolean isXmlDBVersionCompatible(String xml){
	    	String xmlDbVersion = null;
        	boolean isCompatible = false;
        	// the XML DB version can't be null	for restore case
        	if (!Util.isNull(xmlDbVersion = FileUtil.getNodeValueFromXml(xml,KEY_NODE_EXPRESSION))){
            	if (LOG_INFO) Log.i(TAG,"xmlDbVersion "+xmlDbVersion + 
            			" SQLite DB Version" + SQLiteManager.DATABASE_VERSION); 
              
            	// Check if the rule is compatible with the current DB
                try {
                    if(Integer.parseInt(xmlDbVersion) <= SQLiteManager.DATABASE_VERSION){
                    	isCompatible = true;
                    	if (LOG_INFO) Log.i(TAG,"XML DB Version Compatible");
                    }else{
                        Log.w(TAG,"XML DB Version is not compatible");	
                    }
                } catch(NumberFormatException nfe) {
                       Log.e(TAG,"Could not parse :" + nfe);
                } 
        	}else{
        	  Log.e(TAG,"DB Version in the XML is null");
        	} 
        	
        	return isCompatible;
	    }

	    /** This method retrieves content for a given tag from Description XML
	     *  XML content should be enclosed within <DESCRIPTION> and </DESCRIPTION>
	     *  tags
	     * 
	     * @param xml - xml to be parsed
	     * @param tag - Tag to be retrieved
	     * @return String - Node value for the given tag; If there is no valid xml content in 
	     * incoming string, the same string will be returned. If a valid xml is given, but the
	     * requested tag is not present, NULL will be returned.
	     */
	    
        public static String getDescTag(String xml, String tag) {
        Document doc = null;
        String desc = null;
        
        if (xml != null) {
            doc = getParsedDoc(xml);
        }
        if (xml == null || doc == null) {
            if (LOG_DEBUG) Log.d(TAG,"No XML Content in " + xml);
            desc = xml;
        } else {
            if (LOG_DEBUG) Log.d(TAG,"desc xml is =" + xml);

            NodeList nodes = doc.getElementsByTagName(DESCRIPTION_NEW).item(0).getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {

                Node curNode = nodes.item(i);
                if(curNode == null) continue;

                String curNodeName = curNode.getNodeName();
                if(curNodeName == null) continue;

                if (curNodeName.equals(tag) && curNode.getFirstChild() != null) {
                    desc = curNode.getFirstChild().getNodeValue();
                    if(LOG_DEBUG) Log.d(TAG, tag + " : "+ desc);
                    break;
                }
            }
        }
        return desc;
    }
}
