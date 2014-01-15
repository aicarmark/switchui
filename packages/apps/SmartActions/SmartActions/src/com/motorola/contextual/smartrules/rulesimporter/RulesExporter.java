/*
 * @(#)RulesExporter.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18172        2011/01/18 NA				  Initial version
 * A18172        2011/02/23 NA				  Updated with review comments
 * 											  from Craig
 * A18172        2011/04/05 IKINTNETAPP-156   Updated with XML Serilaizer
 *
 */
package com.motorola.contextual.smartrules.rulesimporter;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.business.ActionPersistence;
import com.motorola.contextual.smartrules.db.business.ConditionPersistence;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.ActionTuple;
import com.motorola.contextual.smartrules.db.table.ConditionTuple;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.RuleTuple;


/** This class handles the following:
 *  (a) Entire Smart Rules Database is read
 *  (b) Converts the data read into a single XML format
 *  (c) Stores the XML format file "rulesimporter.rues" in the location
 *      /sdcard/rules.d
 *<code><pre>
 * CLASS:
 * Implements DbSyntax to perform SQL operations.
 * Implements Constants to reuse constants.
 *
 * RESPONSIBILITIES:
 *  (a) Entire Smart Rules Database is read
 *  (b) Converts the data read into a single XML format
 *  (c) Stores the XML format file "rulesimporter.rues" in the location.
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 * 	see methods for usage instructions
 *
 *</pre></code>
 */
public class RulesExporter  implements Constants, XmlConstants, DbSyntax {


    private static final String TAG = RulesExporter.class.getSimpleName();
    private Context mContext;
    private XmlSerializer serializer = null;
    private String mTfData = null;
    private String mLocData = null;
    private String mCalendarEventsData = null;

    /** basic constructor */
    public RulesExporter() {
        super(); // 100 is removed
        if (LOG_DEBUG) Log.d(TAG,TAG+".constr - 1");
    }

    /** This method provides the XML string for a particular list of SOURCE
     *
     * @param sourceList - List of SOURCE Ex: Factory, User Created, Suggested etc
     * @return String - XML String for that particular list of SOURCE
     */
    private String smartRulesToXmlforSpecificSource(ArrayList<String> sourceList) {
        if (LOG_DEBUG) Log.d(TAG,"Fn: smartRulesToXmlforSpecificSource");

        // Final Xml String
        String xmlString = null;

        Cursor ruleCursor = RulePersistence.getRuleCursorForSourceList(mContext, sourceList);

        // For every rule
        if(ruleCursor == null) {
            Log.e(TAG, "Rule Table cursor is null");
        } else {
            try {
                xmlString = getTheXmlStringForAllData(ruleCursor);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (!ruleCursor.isClosed())
                    ruleCursor.close();
            }
        }

        return xmlString;
    }

    /** Reads the entire Smart Rules database and
      * converts it into an XML.
      *
      * @param  context - Application Context
      * @return String - XML content in the form of string
      */
    private String smartRulestoXml(Context context) {

        if (LOG_DEBUG) Log.d(TAG,"Fn: smartRulestoXml");

        // Final Xml String
        String xmlString = null;


        Cursor ruleCursor = RulePersistence.getRuleCursor(context);

        // For every rule
        if(ruleCursor == null) {
            Log.e(TAG, "Rule Table cursor is null");
        } else {
            try {
                xmlString = getTheXmlStringForAllData(ruleCursor);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (!ruleCursor.isClosed())
                    ruleCursor.close();
            }
        }

        return xmlString;

    }


    /** This method provides the XML String for all the rules in
      * the Smart Rules Database
      *
      * @param ruleCursor - cursor with respect to the Rule Table
      */
    private void processRuleCursor(final Cursor ruleCursor) throws Exception {

        if (LOG_DEBUG)  Log.d(TAG,"Fn: processRuleCursor");

        if(ruleCursor.moveToFirst()) {
            if (LOG_DEBUG) Log.d(TAG, "Rule table size"+ruleCursor.getCount());

            do {
                long ruleId = ruleCursor.getLong(ruleCursor.
                                                 getColumnIndexOrThrow(RuleTable.Columns._ID));

                if (ruleId > 0) {
                    //Start tag for <SMARTRULE>
                    serializer.startTag(null, XmlConstants.SMARTRULE);
                    serializer.text("\n\n");

                    // Create XML for <RULEINFO>
                    RuleTuple.createXmlStringForRuleInfo(serializer, ruleCursor);

                    if (LOG_DEBUG) Log.d(TAG, "Processing Condition !!!");

                    //Now get the PreConditions corresponding to the rule from the Condition
                    // table matching the ruleID
                    Cursor conditionCursor = ConditionPersistence.getConditionCursor(
                                                 mContext, ruleId);

                    if(conditionCursor == null) {
                        Log.e(TAG, "Condition Table cursor is null");
                    } else {
                        try {
                            processConditionCursor(conditionCursor);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (!conditionCursor.isClosed())
                                conditionCursor.close();
                        }
                    }


                    if (LOG_DEBUG) Log.d(TAG, "Processing Action !!!");

                    //Now get the Actions corresponding to the rule from the Action
                    // table matching the ruleID
                    Cursor actionCursor = ActionPersistence.getActionCursor(mContext, ruleId);

                    if(actionCursor == null) {
                        Log.e(TAG, "Action Table cursor is null");
                    } else {
                        try {
                            processActionCursor(actionCursor);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (!actionCursor.isClosed())
                                actionCursor.close();
                        }
                    }

                    //End tag for <SMARTRULE>
                    serializer.endTag(null, XmlConstants.SMARTRULE);
                    serializer.text("\n\n");

                } else {
                    if (LOG_DEBUG) Log.d(TAG,"Rule ID is invalid !!!");
                }
            } while (ruleCursor.moveToNext());
        }
    }


    /** This method provides the Condition / STATE level XML String for
      * each rule in the Smart Rules Database
      *
      * @param conditionCursor - cursor with respect to the Condition Table
      */
    private void processConditionCursor(final Cursor conditionCursor) throws Exception {

        if (LOG_DEBUG) Log.d(TAG,"Fn: processConditionCursor");


        if(conditionCursor.moveToFirst()) {

            if (LOG_DEBUG) Log.d(TAG, "Condition table size"+
                                     conditionCursor.getCount());

            //Start tag for <CONDITIONS>
            serializer.startTag(null, XmlConstants.CONDITIONS);
            serializer.text("\n\n");

            do {
                //Create the XML String for <CONDITION>
                createXmlStringForCondition(conditionCursor);
            } while (conditionCursor.moveToNext());

            //End tag for <CONDITIONS>
            serializer.endTag(null, XmlConstants.CONDITIONS);
            serializer.text("\n\n");
        }
    }

    /** This method provides the Action  level XML String for
      * each rule in the Smart Rules Database
      *
      * @param actionCursor - cursor with respect to the Action Table
      */
    private void processActionCursor(final Cursor actionCursor) throws Exception {

        if (LOG_DEBUG) Log.d(TAG,"Fn: processActionCursor");

        if(actionCursor.moveToFirst()) {

            if (LOG_DEBUG) Log.d(TAG, "Action table size"+ actionCursor.getCount());

            //Start tag for <ACTIONS>
            serializer.startTag(null, XmlConstants.ACTIONS);
            serializer.text("\n\n");

            do {
                //Create XML String for Action
                ActionTuple.createXmlStringForAction(serializer, actionCursor);
            } while (actionCursor.moveToNext());

            //End tag for <ACTIONS>
            serializer.endTag(null, XmlConstants.ACTIONS);
            serializer.text("\n\n");
        }
    }

    /** This method provides the XML String for <CONDITION> node
     *  with respect to a rule
     *
     * @param conditionCursor - cursor with respect to the Condition Table
     */
    private void createXmlStringForCondition(final Cursor conditionCursor) throws Exception {

        if (LOG_DEBUG) Log.d(TAG,"Fn: createXmlStringForCondition");

        //Start tag for <CONDITION>
        serializer.startTag(null, XmlConstants.CONDITION);
        serializer.text("\n");

        // Creating the content of <CONDITION> tag
        ConditionTuple.createXmlStringForCondition(serializer, conditionCursor);


        // <CONDITION> End tag
        serializer.endTag(null, XmlConstants.CONDITION);
        serializer.text("\n\n");
    }




    /** This method writes the final XML String which contains all the Rule
      * related information to /sdcard/rules.d directory and the single xml
      * file name is "rulesimporter.rules"
      *
      * @param rules - Final Xml String which encapsulates all the rule related
      *                information
      * @return boolean - rule successfully exported or not
      */
    private boolean writeAllRulesToXML(final String rules) {
        if (LOG_DEBUG) Log.d(TAG,"Fn: writeAllRulesToXML > Rules = " + rules);

        boolean status = false;

        File mFile = FileUtil.openRulesImporterFileFromSdCard(true);

        if (mFile != null) {
            // Open the file for appending
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mFile, false);
                if (fos != null) {
                    fos.write(rules.getBytes());
                    status = true;
                }
            } catch (IOException e) {
                status = false;
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } finally {
                // Close the file
                try {
                    if (fos != null) fos.close();
                } catch (IOException e) {
                    status = false;
                    e.printStackTrace();
                }
            }
        }
        return status;
    }


    /** This method writes the final XML String which contains all the Rule
      * related information to /sdcard/rules.d directory and the single xml
      * file name is "rulesimporter.rules" and also shows the toast based on
      * whether the rules exported successfully or not
      *
      * @param context - Application context
      * @return RulesExporterXml - instance of RulesImporterXml (hierarchy objects
      * 							mapping to the XML structure)
      */

    public void startRulesExporter(Context context) {

        if (LOG_DEBUG) Log.d(TAG,"Fn: Start RulesExporter !!!");

        mContext = context;
        String smartRulesXml = smartRulestoXml(context);

        if(smartRulesXml != null) {
            if(writeAllRulesToXML(smartRulesXml)) {
                Toast.makeText(mContext, R.string.rules_exported_pass,
                               Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, R.string.rules_exported_fail,
                               Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "SmartRulesXml is null");
        }
        return;
    }

    /** This method provides the final XML String for a list of SOURCE
     * and it also uploads the rule to the Server
     *
     * @param context - Application context
     * @param sourceList - List of SOURCE Ex: Factory, Suggested etc.
     * @return String - Smart Rules XML 
     *                             
     */
    public String startRulesExporter(Context context,ArrayList<String> sourceList) {

        if (LOG_DEBUG) Log.d(TAG,"Fn: Start RulesExporter  for ArrayList !!!");

        mContext = context;

        String xmlStringToServer = null;
        xmlStringToServer = smartRulesToXmlforSpecificSource(sourceList);
        
        if (LOG_DEBUG) Log.d(TAG,"xmlStringToServer is : " + xmlStringToServer);

        if (xmlStringToServer != null && xmlStringToServer.contains(SMARTRULE_NODE)) {
            return xmlStringToServer;
        } 
            
        Log.w(TAG,"No rules to export!");
      
        return null;
    }


    /** This method provides the final XML String for a list of SOURCE
     * and it also uploads the rule to the Server
     *
     * @param context - Application context
     * @param sourceList - List of SOURCE Ex: Factory, Suggested etc.
     * @param timeFrameData - TimeFrame data canned in JSON format
     * @param locaData = Location data canned in JSON format
     *
     * @return String - Smart Rules XML 
     */
    public String startRulesExporter(Context context,ArrayList<String> sourceList,
            String timeFrameData, String locData, String calendarEventsData) {

        if (LOG_DEBUG)
            Log.d(TAG,"Fn: Start RulesExporter for SourceList, TimeFrames and Location Data!!!");
        
        if (timeFrameData == null) {
        	if(LOG_DEBUG) Log.d(TAG, "Timeframe Data is null");
        } else {
        	mTfData = timeFrameData;

	        //Updating the Shared Preference for TimeFrames
	        if(LOG_INFO) Log.i(TAG, "Updating Shared Preference for TimeFrames");

	        SharedPreferences tfPref = context.getSharedPreferences(
	                                         TIMEFRAME_SHARED_PREFERENCE, Context.MODE_PRIVATE);
	        SharedPreferences.Editor editor = tfPref.edit();
	        editor.clear();
	        editor.putString(TIMEFRAME_XML_CONTENT, timeFrameData);
	        editor.commit();
        }

        if (locData == null) {
            if(LOG_DEBUG) Log.d(TAG, "Location Data is null");
        } else {
            mLocData = locData;

            //Updating the Shared Preference for Location
            if(LOG_INFO)  Log.i(TAG, "Updating Shared Preference for Location");

            SharedPreferences locPref = context.getSharedPreferences(
                    LOCATION_SHARED_PREFERENCE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = locPref.edit();
            editor.clear();
            editor.putString(LOCATION_XML_CONTENT, locData);
            editor.commit();
        }

        if (calendarEventsData == null) {
            if (LOG_DEBUG) {
                Log.d(TAG, "Calendar events data is null");
            }
        } else {
            mCalendarEventsData = calendarEventsData;
            if (LOG_INFO) {
                Log.i(TAG, "Updating Shared Preference for Calendar events");
            }
            SharedPreferences calendarEventsPreference = context
                    .getSharedPreferences(CALENDAR_EVENTS_SHARED_PREFERENCE,
                            Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = calendarEventsPreference.edit();
            editor.clear();
            editor.putString(CALENDAR_EVENTS_XML_CONTENT, calendarEventsData);
            editor.commit();
        }

        //By default get the XML for User, Suggestions which are not accepted.
        if (sourceList == null){
        	 sourceList = new ArrayList<String>();
             sourceList.add(Integer.toString(RuleTable.Source.USER));
             sourceList.add(Integer.toString(RuleTable.Source.SUGGESTED));
             sourceList.add(Integer.toString(RuleTable.Source.FACTORY));
             sourceList.add(Integer.toString(RuleTable.Source.CHILD));
        }

        return startRulesExporter(context,sourceList);
    }


    /** This method provides the XML String for all rules
     *  in the Smart Rules Database, the TimeFrame data and the location data
     *
     * @param ruleCursor - cursor with respect to the Rule Table
     */

    private String getTheXmlStringForAllData(Cursor ruleCursor) {

        if (LOG_DEBUG) Log.d(TAG,"Fn: getTheXmlStringForRulesAndTimeFrames !!!");

        // Final Xml String
        String xmlString = null;

        StringWriter writer = null;

        try {
            // XML Serializer
            writer = new StringWriter();
            serializer = Xml.newSerializer();
            serializer.setOutput(writer);
            serializer.startDocument("UTF-8", null);
            serializer.setFeature("" +
                                  "http://xmlpull.org/v1/doc/features.html#indent-output", true);


            //Start tag for <RULES>
            serializer.startTag(null, Constants.RULES);

            // Location
            appendLocationData();

            //Rules
            processRuleCursor(ruleCursor);

            //TimeFrames
            appendTimeFrameData();

            // Calendar events
            appendCalendarEventsData();

            //End tag for <RULES>
            serializer.endTag(null, Constants.RULES);

            serializer.endDocument();

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter buffer = new StringWriter();
            transformer.transform(new DOMSource(FileUtil.getParsedDoc(writer.toString())), new StreamResult(buffer));

            xmlString = buffer.toString();


        } catch (Exception e) {
            e.printStackTrace();
        }

        return xmlString;
    }


    /** This method appends TimeFrame Data to
     *  the XML containing all the Rules
     */

    public void appendTimeFrameData() throws Exception{

    	if (LOG_DEBUG) Log.d(TAG,"Fn: appendTimeFrameData !!!");

        if (mTfData == null) {
        	//Retrieve the timeframe XML data from the shared preference
            SharedPreferences prefs = mContext.getSharedPreferences(
            								TIMEFRAME_SHARED_PREFERENCE, Context.MODE_PRIVATE);

            mTfData = prefs.getString(TIMEFRAME_XML_CONTENT, EMPTY_STRING);
        }

        if ((mTfData == null) || (mTfData.length() <= 0)) {
        	Log.w(TAG, " No Timeframe data to write ");
        } else {
	        //Start tag for <TIMEFRAMES>
	        serializer.startTag(null, TIMEFRAMES_NODE);
	        serializer.text("\n");

	        if(LOG_DEBUG) Log.d(TAG," Rules Exporter : timeframe data =  " + mTfData);

	        serializer.text(mTfData);
	        serializer.text("\n");

	        //End tag for <TIMEFRAMES>
	        serializer.endTag(null, TIMEFRAMES_NODE);
	        serializer.text("\n\n");
	    }
    }

    /** This method appends Location Data to
     *  the XML containing all the Rules
     */
    public void appendLocationData() throws Exception{

        if (LOG_DEBUG) Log.d(TAG,"Fn: appendLocationData !!!");

        if (mLocData == null) {
            //Retrieve the Location XML data from the shared preference
            SharedPreferences prefs = mContext.getSharedPreferences(
                    LOCATION_SHARED_PREFERENCE, Context.MODE_PRIVATE);

            mLocData = prefs.getString(LOCATION_XML_CONTENT, EMPTY_STRING);
        }

        if (mLocData != null && mLocData.length() > 0) {
            //Start tag for <LOCATION>
            serializer.startTag(null, LOCATION_NODE);
            serializer.text("\n");

            if(LOG_DEBUG) Log.d(TAG," Rules Exporter : location data =  " + mLocData);

            serializer.text(mLocData);
            serializer.text("\n");

            //End tag for <LOCATION>
            serializer.endTag(null, LOCATION_NODE);
            serializer.text("\n\n");
        } else {
            Log.w(TAG, " No Location data to write ");
        }
    }

    /**
     * This method appends Calendar events data to the XML containing all the
     * Rules
     *
     * @throws Exception
     */
    public void appendCalendarEventsData() throws Exception {
        if (LOG_DEBUG) {
            Log.d(TAG, "Fn: appendCalendarEventsData !!!");
        }
        if (mCalendarEventsData == null) {
            // Retrieve the Calendar events XML data from the shared preference
            SharedPreferences preference = mContext.getSharedPreferences(
                    CALENDAR_EVENTS_SHARED_PREFERENCE, Context.MODE_PRIVATE);
            mCalendarEventsData = preference.getString(
                    CALENDAR_EVENTS_XML_CONTENT, EMPTY_STRING);
        }
        if (mCalendarEventsData != null && mCalendarEventsData.length() > 0) {
            // Start tag for <CALENDAR_EVENTS>
            serializer.startTag(null, CALENDAR_EVENTS_NODE);
            serializer.text("\n");
            if (LOG_DEBUG) {
                Log.d(TAG, "Rules Exporter : calendar events data = "
                        + mCalendarEventsData);
            }
            serializer.text(mCalendarEventsData);
            serializer.text("\n");
            // End tag for <CALENDAR_EVENTS>
            serializer.endTag(null, CALENDAR_EVENTS_NODE);
            serializer.text("\n\n");
        } else {
            Log.w(TAG, "No Calendar events data to write");
        }
    }
    




}
