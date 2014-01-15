package com.motorola.acousticwarning.mmcp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.content.Intent;
import android.util.TypedValue;
import android.content.res.Resources;
import java.util.Arrays;
import java.util.List;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemProperties;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xmlpull.v1.*;
import java.io.StringReader;
import com.android.internal.util.XmlUtils;
import org.xml.sax.InputSource;


/*
 * Copyright (C) 2011, Motorola, Inc,
 * All Rights Reserved
 */

public class AcousticWarningUtil {
    public static final String TAG = "AcousticWarningUtil";
    public static final boolean DEBUG = true;
    //private static boolean DEBUG = (SystemProperties.getInt("ro.debuggable", 0) == 1);
    public static final String CDA_TAG_ACOUSTICSETTING = "@MOTOFLEX@getAcousticSetting";
    public static final String TAG_ACOUSTICSETTING = "AcousticSetting";
    public static final String TAG_ACOUSTIC_LEVEL = "AcousticLevel";
    public static final String TAG_ACOUSTIC_FM_STREAM_TYPE_VALUE = "AcousticFmStreamTypeValue";
    public static final String TAG_ACOUSTIC_FM_STATE_INTENT = "AcousticFmStateIntent";
    public static final String TAG_ACOUSTIC_FM_STATE_EXTRA = "AcousticFmStateExtra";
	
    public static String getValueCDA(Context context, String tag){
    	String xmlString = "";
	String xmlResult = "";
	String defaultValue = "false";
        boolean needCDA = false;
	//Get string for item corresponding to "@MOTOFLEX@getAcousticLevel"
        xmlString = getResultFromCDA(context, CDA_TAG_ACOUSTICSETTING);
        if (!xmlString.trim().equals("")) {
			needCDA = true;
		}
        
        xmlResult =  getAcousticWarningFromCDA(xmlString, tag);
        if("".equals(xmlResult)){
        	xmlResult = defaultValue;
        }
	return xmlResult;
    }

    public static String getResultFromCDA(Context context, String cmd) {
	TypedValue isCDA = new TypedValue();
	TypedValue contentCDA = new TypedValue();
	Resources r = context.getResources();
	String xmlresult = "";
	try {
	    r.getValue("@MOTOFLEX@isCDAValid", isCDA, false);
	    if (isCDA.coerceToString().toString().equalsIgnoreCase("true")) {
	    r.getValue(cmd, contentCDA, false);
	    xmlresult = contentCDA.coerceToString().toString();
				
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	if (!xmlresult.trim().equals("")) {
	    return xmlresult;
	} else {
	    String NoCDA = "";
	    return NoCDA;
	}
    }

    public static String getAcousticWarningFromCDA(String xmlString, String tag){
        Element root = null;
	DocumentBuilderFactory dbf = null;
	DocumentBuilder db = null;
	String xmlresult ="";
    	if((null != xmlString) && ("" != xmlString)){
    	    try{
    		XmlPullParserFactory  factory = XmlPullParserFactory.newInstance();
    		factory.setNamespaceAware(true);
    		XmlPullParser parser = factory.newPullParser();
    		parser.setInput(new StringReader(xmlString));
    		XmlUtils.beginDocument(parser, TAG_ACOUSTICSETTING);
    		dbf = DocumentBuilderFactory.newInstance();
		db = dbf.newDocumentBuilder();
		Document xmldoc = db.parse(new InputSource(new StringReader(
						xmlString)));
		root = xmldoc.getDocumentElement();
		NodeList prcVersions = root.getElementsByTagName(tag);
		Element currentNode = (Element) prcVersions.item(0);
		xmlresult = currentNode.getFirstChild().getNodeValue();
    	    }catch(Exception e){
    		e.printStackTrace();
    	    }
    	    if (!xmlresult.trim().equals("")) {
    	    	return xmlresult;
    	    } else {
    		String NoCDA = "";
    		return NoCDA;
    	    }
    	}
    	return xmlresult;
    }   
}
