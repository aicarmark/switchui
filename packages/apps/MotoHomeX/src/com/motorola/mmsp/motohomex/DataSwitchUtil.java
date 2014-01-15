/*
 * Copyright (C) 2011, Motorola, Inc,
 * All Rights Reserved
 */

package com.motorola.mmsp.motohomex;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.util.Log;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
/*Added by ncqp34 at at Jan-17-2012 for dataSwitch flex*/
import android.content.res.Resources;
import android.util.TypedValue;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
/*ended by ncqp34*/
import com.motorola.mmsp.motohomex.R;

/*
 * Copyright (C) 2011, Motorola, Inc,
 * All Rights Reserved 
 */

public class DataSwitchUtil {
     
    public static String ACTION_DATA_CHARGE_DIALOG = "com.motorola.dataalert.SHOW_DATA_CHARGE_DIALOG";
    public static String ACTION_DATA_CONNECTION_DIALOG = "com.motorola.dataalert.SHOW_DATA_CONNECTION_DIALOG";
    public static String ACTION_WIFI_DIALOG = "com.motorola.dataalert.SHOW_WIFI_DIALOG";
    /*Added by ncqp34 at Jan-17-2012 for dataSwitch flex*/
    private static final String CDA_TAG_HOMESETTING = "@MOTOFLEX@getHomeSetting";
    private static final String TAG_HOMESETTING = "homesetting";
    private static final String TAG_DATA_ALERT_ENABLE = "dataAlertEnable" ;
    public static String alertEnable = null;
    private static final String TAG = "DataSwitchUtil";
    /*ended by ncqp34*/

        static public boolean isDialogNeedToShow(String packageName, Context context){
    	/*Added by ncqp34 at Jan-17-2012 for dataSwitch flex*/
	if(alertEnable == null){
	    alertEnable = getDataAlertEanble(context) ? "true" : "false";
	    Log.d(TAG,"alertEnable ==" + alertEnable);
	}
	if(alertEnable.equals("false")){
	    Log.d(TAG,"alertEnable false retur");
	    return false;
	}
	/*ended by ncqp34*/

        //We need to show a dialog ONLY if 3 conditions are met
        //1.Theres should not be any active data(cellular/wifi) connection
        //2.App should request for data connection
        //3.App should not be listed in the white list, //Delete this option in Home.
           
        boolean isDialogNeedToShow = false;

        boolean isActiveConnectionExists = isActiveDataConnectionExists(context);
        boolean isAppRequiredDataConn = isAppRequiredDataConnection(packageName ,context);
        /*2012-0109, DJHV84 Screen WhiteList in Home of Data Switch*/
        //boolean isAppInWhiteList = isInWhiteList(packageName ,context); 
        
        //if(!isActiveConnectionExists && isAppRequiredDataConn && !isAppInWhiteList){
        /*DJHV83 end*/
        if(!isActiveConnectionExists && isAppRequiredDataConn){
            isDialogNeedToShow = true;
        }
        return isDialogNeedToShow; 
    }
    
    static public boolean isActiveDataConnectionExists(Context context){
        Log.d("Shiv", " isActiveDataConnectionExists()");
        
        ConnectivityManager connMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        
        if(netInfo != null){
            Log.d("Shiv", " netInfo.isConnected" + netInfo.isConnected());
            Log.d("Shiv", " isAvailable()" + netInfo.isAvailable());
            Log.d("Shiv", " isConnectedOrConnecting()" + netInfo.isConnectedOrConnecting());
        }         
        return ((netInfo != null) && netInfo.isConnected());
    }
    
    static public boolean isAppRequiredDataConnection(String packageName, Context context){
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            if (pkgInfo != null) {
                String permissions[] = pkgInfo.requestedPermissions;
                boolean showDialog = false;
                if(permissions!=null){
                    for (String permit : permissions) {
                        if ("android.permission.INTERNET".equals(permit)) {
                            showDialog = true;
                            break;
                        }
                    }
                }
                return showDialog;
            }
        }catch (PackageManager.NameNotFoundException e) {
            Log.e("Shiv", "isAppRequiredDataConnection() NameNotFoundException");
        }
        return false;
    }

    /*2012-01-09, DJHV83 Screen WhiteList in Home of Data Switch*/
    /*static public boolean isInWhiteList(String packageName, Context context) {
        List<String>whiteList = Arrays.asList(context.getResources().getStringArray(R.array.whitelisted_apps));
        if((whiteList != null) && (whiteList.contains(packageName))) {
            return true;
        }
        return false;
    }*/
    /*DJHV83 end*/

    /*Added by ncqp34 at Jan-17-2012 for dataSwitch flex*/
    public static boolean getDataAlertEanble(Context context){
    	String xmlString = "";
	String xmlResult = "";
        boolean needCDA = false;
	String defaultValue = "true";
	boolean alertEnable = true;
        xmlString = getResultFromCDA(CDA_TAG_HOMESETTING ,context);
	Log.d(TAG,"getDataAlertEnable-xmlString == "+ xmlString);
        xmlResult = getSubItemFromCDA(xmlString ,TAG_DATA_ALERT_ENABLE ,context);
	Log.d(TAG,"getDataAlertEnable-xmlResult == "+ xmlResult);
        if("".equals(xmlResult)){
		xmlResult = defaultValue;
        }
	if(xmlResult.equalsIgnoreCase("true")){
	    alertEnable = true;
	}else if(xmlResult.equalsIgnoreCase("false")){
            alertEnable = false;
	}

       	return alertEnable;
    }

    public static String getResultFromCDA(String cmd ,Context context) {
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
 
    public static String getSubItemFromCDA(String xmlString ,String itemTag ,Context context){
    	Element root = null;
	DocumentBuilderFactory dbf = null;
	DocumentBuilder db = null;
	String xmlresult ="";
    	if((null != xmlString) && ("" != xmlString)){
    	    try{
		/*Seems not use begin*/
		/*
    		XmlPullParserFactory  factory = XmlPullParserFactory.newInstance();
    		factory.setNamespaceAware(true);
    		XmlPullParser parser = factory.newPullParser();
    		parser.setInput(new StringReader(xmlString));
    		XmlUtils.beginDocument(parser, TAG_HOMESETTING);
		*/
		/*Seems not use end*/
    		dbf = DocumentBuilderFactory.newInstance();
		db = dbf.newDocumentBuilder();
		Document xmldoc = db.parse(new InputSource(new StringReader(
					xmlString)));
		root = xmldoc.getDocumentElement();
		NodeList orientEnable = root.getElementsByTagName(itemTag);
		Element currentNode = (Element) orientEnable.item(0);
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
    /*ended by ncqp34*/

}
