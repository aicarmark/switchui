// ********************************************************** //
// PROJECT:     APR (Automatic Panic Recording)
// DESCRIPTION: 
//   The purpose of APR is to gather the panics in the device
//   and record statics about those panics to a centralized
//   server, for automated tracking of the quality of a program.
//   To achieve this, several types of messages are required to
//   be sent at particular intervals.  This package is responsible
//   for sending the data in the correct format, at the right 
//   intervals.
// ********************************************************** //
// Change History
// ********************************************************** //
// Author         Date       Tracking  Description
// ************** ********** ********  ********************** //
// Lisq 10/08/2011 1.0       Initial Version
//
// ********************************************************** //
package com.motorola.motoapr.service;


import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;

import java.io.IOException;


public class APRFLEX{
	
	static final String TAG = "APRFLEX";

      static Context mContext = null;

	 static  String switchvalue = null;
	 static  String durationvalue =null;
	  static  String destinationvalue = null;
   public APRFLEX( Context context ) {
	   
	   mContext = context;
	fetchFlexValues(mContext);


	APRDebug.APRLog( TAG, "query switchvalue =  " + switchvalue + "  durationvalue = " + durationvalue + " destinationvalue =  " + destinationvalue);
        
	if(null != switchvalue)
	{
	   if (switchvalue.equalsIgnoreCase("true") ) 
	   {

		APRPreferences.APRPrefsSetEnabledState( true );
		APRPreferences.APRSetReadFlexSuccess(true);
	    }
	   else if(switchvalue.equalsIgnoreCase("false") )
	   {
		APRPreferences.APRPrefsSetEnabledState( false );
		APRPreferences.APRSetReadFlexSuccess(true);
	   }
	} else {
		APRDebug.APRLog( TAG, "after fetchFlexValues(), switchvalue is null! will set read_flex_success flag to false!");
		APRPreferences.APRSetReadFlexSuccess(false);
	}
	boolean aprenable = APRPreferences.APRPrefsGetEnabledState();
	APRDebug.APRLog( TAG, "APRFLEX aprenable =  " + aprenable);

	if(null != durationvalue ){
		APRPeriodicMessageAndTimer.APRSetSpeed(durationvalue);
		APRPreferences.APRSetDurationValue(durationvalue);
	}

	long speed = APRPeriodicMessageAndTimer.APRGetSpeed();
	APRDebug.APRLog( TAG, "APRFLEX speed =  " + speed);


	if(null != destinationvalue )
	APRPreferences.APRSetCarrierNumber( destinationvalue );
	String sms_number = APRPreferences.APRGetCarrierNumber();
	APRDebug.APRLog( TAG, "APRFLEX sms_number =  " + sms_number);
					   	   
   }


     public void fetchFlexValues(Context context) {

            String xmlstr = null;
            boolean bUnit = false;
            boolean bFrequency = false;
            boolean bDisclaimer = false;

            TypedValue isCDA = new TypedValue();
            TypedValue contentCDA = new TypedValue();
            Resources r = context.getResources();
            try {        
                r.getValue("@MOTOFLEX@isCDAValid", isCDA, false);
                if (isCDA.coerceToString().toString().equalsIgnoreCase("TRUE")) {
                	Log.v("apr", "@@@@@@@@@@@@@ isCDA.coerceToString() is true @@@@@@@@@@@@@");
                    r.getValue("@MOTOFLEX@getAPRSetting", contentCDA, false); 
                    xmlstr = contentCDA.coerceToString().toString();
                } else {
                	Log.v("apr", "@@@@@@@@@@@@@ isCDA.coerceToString() is false @@@@@@@@@@@@@");
                }
            } catch (Exception e) {
            	Log.v("apr", "@@@@@@@@@@@@@ exception occurs !!!!!!!!!!!!! @@@@@@@@@@@@@");
                e.printStackTrace();
            }
            Element root = null;
            DocumentBuilderFactory dbf = null;
            DocumentBuilder db = null;

            if ((null != xmlstr) && (!"".equals(xmlstr))) {
                APRDebug.APRLog( TAG, "fetchCDAValues: " + xmlstr);
                try {
                    dbf = DocumentBuilderFactory.newInstance();
                    db = dbf.newDocumentBuilder();
                    Document xmldoc = db.parse(new InputSource(new StringReader(xmlstr)));
                    root = xmldoc.getDocumentElement();
                    NodeList list = root.getChildNodes();
                    if (list != null) {
                        int len = list.getLength();
                        for (int i = 0; i < len; i++) {
                            Node currentNode = list.item(i);
                            String name = currentNode.getNodeName();
                            String value = currentNode.getTextContent();
                            if (value != null && name != null && name.equals("apr-switch")) {
                                bUnit = true;
                                  APRDebug.APRLog( TAG, "APRFLEX get aprenable =  " + value);
					switchvalue = value;
				     
                            } else if (value != null && name != null && name.equals("apr-duration")) {
                                bFrequency = true;
					durationvalue = value;
                                APRDebug.APRLog( TAG, "APRFLEX get speed =  " + value);

                            } else if (value != null && name != null && name.equals("apr-destination")) {
                                bDisclaimer = true;
				     destinationvalue = value;
                                APRDebug.APRLog( TAG, "APRFLEX sms_number =  " + value);

                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
            	APRDebug.APRLog( TAG,"@@@@@@@@@@@@@ get nothing from flex file @@@@@@@@@@@@@");
            }
            
            try {
                if (!bUnit) {
                    APRDebug.APRLog( TAG, "@@@@@@@@@@@@@ init apr-switch @@@@@@@@@@@@@");

                }
                if (!bFrequency) {
                    APRDebug.APRLog( TAG,"@@@@@@@@@@@@@ init apr-duration@@@@@@@@@@@@@");
                }
                if (!bDisclaimer) {
                   APRDebug.APRLog( TAG, "@@@@@@@@@@@@@ init apr-destination @@@@@@@@@@@@@");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        
    }


    


}
