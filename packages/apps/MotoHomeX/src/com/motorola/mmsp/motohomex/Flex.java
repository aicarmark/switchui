/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
*Create by ncqp34 at Dec-31 for flex mechanism
*
*/
package com.motorola.mmsp.motohomex;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.text.Collator;

import android.util.TypedValue;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.content.res.Resources;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import com.android.internal.util.XmlUtils;
import com.motorola.mmsp.motohomex.LauncherSettings.Favorites;
import com.motorola.mmsp.motohomex.LauncherProvider;
/*Added by ncqp34 at Mar-30 for fake app*/
import org.xmlpull.v1.XmlPullParserException;
import java.io.IOException;
import android.net.Uri;

import com.motorola.mmsp.motohomex.R;
/*ended by ncqp34*/
public final class Flex{

    private static final String TAG = "Home-Flex";
    private static final String TAG_CDA_VALID = "@MOTOFLEX@isCDAValid";
    //Constant for HomeSetting
    private static final String CDA_TAG_HOMESETTING = "@MOTOFLEX@getHomeSetting";
    private static final String TAG_HOMESETTING = "homesetting";
    private static final String TAG_VERSION_PRC = "versionPRC";
    private static final String TAG_ORIENTATION_ENABLE = "orientationEnable" ;
    /* 2012-5-22 add by e13775 for SWITCHUI-1209 */ 
    private static final String TAG_SEACH_BAR_HIDE = "searchBarHide";
    /* 2012-5-22 add by e13775 for SWITCHUI-1209 end*/ 
    //Constant for Market
    private static final String ANDROIDMARKET_PACKAGE = "com.android.vending";
    private static final String ANDROIDMARKET_CLASS = "com.android.vending.AssetBrowserActivity";
    private static final String MOTOROLAMARKET_PACKAGE = "com.moto.mobile.appstore";
    private static final String MOTOROLAMARKET_CLASS = "com.moto.mobile.appstore.activity.MainActivity";
    //Constant for menuOrder
    private static final String CDA_TAG_MAINMENUSORT = "@MOTOFLEX@getMainMenuSort";
    private static final String TAG_MENUORDER= "main-menu-order";
    private static final String TAG_APP= "app";
    private static ArrayList<String> mApps_list;
    private static ApplicationInfoComparator CT_APPS_COMPARATOR ; 
    //Constant for Wallpaper
    private static final String TAG_WALLPAPERLIST = "wallpaper-list";
    private static final String TAG_WALLPAPER = "wallpaper";
    private static final String TAG_ATTRIBUTE = "default";
    private static final String CDA_TAG_WALLPAPER = "@MOTOFLEX@getWallpaper";
    public static ArrayList<String> mThumbsFromAPHidden;
    public static  ArrayList<String> mImagesFromAPHidden;
    private static String HiddenWallpaperPath ;
    private static final String DEFAULT_FLEX_WALLPAPER_PATH = "/hidden/data/CDA/content/wallpaper/";
    public static int mDefaultPos = -1;
    /* 2012-5-22 add by e13775 for SWITCHUI-1209 */ 
    private static boolean mFirstTime = true;
    private static boolean mSearchBarHide = false;
    /* 2012-5-22 add by e13775 for SWITCHUI-1209 */ 


    public static boolean isFlexValid(Context context ){
	try {
	    TypedValue isCDA = new TypedValue();
	    Resources r = context.getResources();
	    r.getValue("@MOTOFLEX@isCDAValid", isCDA, false);
	    if (isCDA.coerceToString().toString().equalsIgnoreCase("true")){ 
	        return true;
	    }
	} catch (Exception e) {
		e.printStackTrace();
	}
	return false;	
    }
    /**
     *Function to retrieve the flex item string
     *Param cmd means the xml tag of the item,such as "@MOTOFLEX@getHomeSetting"
     */
    public static boolean getBoolItem(String cmd ,Context context){
        try{
	    String  xmlresult = getResultFromCDA(cmd ,context);
	    if (xmlresult.equalsIgnoreCase("true")){
	      return true;
	    }
	}catch (Exception e) {
	    e.printStackTrace();
	}
	return false;
    }

    /**
     *Function to retrieve the flex item string
     *Param cmd means the xml tag of the item,such as "@MOTOFLEX@getHomeSetting"
     */
    public static String getResultFromCDA(String cmd ,Context context) {
	TypedValue contentCDA = new TypedValue();
	String xmlresult = "";
	if(context == null) return xmlresult;
	Resources r = context.getResources();
	try {
		if (isFlexValid(context)) {
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

    /**
     *Function to retrieve the sub flex item string
     *Param xmlString : the whole flex string by the root tag,return by function getResultFromCDA()
     *Param xmlTag : means the root tag of a flex item,such as "homesetting"
     *Param subTag : means the sub tag of a sub flex item,such as "orientationEnable"
     *Example : getSubItemFromCDA(String xmlString, TAG_HOMESETTING,TAG_ORIENTATION_ENABLE )
     */
    public static String getSubItemFromCDA(String xmlString, String xmlTag, String subTag){
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
    		XmlUtils.beginDocument(parser, xmlTag);
		*/
		/*Seems not use end*/

    		dbf = DocumentBuilderFactory.newInstance();
		db = dbf.newDocumentBuilder();
		Document xmldoc = db.parse(new InputSource(new StringReader(xmlString)));
		root = xmldoc.getDocumentElement();
		NodeList orientEnable = root.getElementsByTagName(subTag);
		Element currentNode = (Element) orientEnable.item(0);
		xmlresult = currentNode.getFirstChild().getNodeValue();
    	    }catch(Exception e){
    			e.printStackTrace();
    	    }
    	
	    if (!xmlresult.trim().equals("")) {
    		return xmlresult;
    	    } else {//noCDA or the flex not configure of there is no this flex item,return "";
    	            String NoCDA = "";
    		    return NoCDA;
    	    }
    	}
    	return xmlresult;
    }

    /*Function to get One boolean flex item ,defaultValue for default*/
    public static boolean getItemValue(Context context ,String flexId, String xmlTag, String subXmlTag, boolean defaultValue)	 {
    	String xmlString = "";
	String xmlResult = "";
        xmlString = getResultFromCDA(flexId,context);
        xmlResult = getSubItemFromCDA(xmlString , xmlTag, subXmlTag);
	Log.d(TAG,"getOrientationSupport-xmlResult == "+ xmlResult);
	if(xmlResult.equalsIgnoreCase("true")){
	    return true;
	}else if(xmlResult.equalsIgnoreCase("false")){
	    return false;
	}

       	return defaultValue;
     }

    /*Function to getOrientation flex,true for default*/
    public static boolean getOrientationSupport(Context context){
	return getItemValue(context ,CDA_TAG_HOMESETTING, TAG_HOMESETTING ,TAG_ORIENTATION_ENABLE,true);
     }

    /* 2012-5-22 add by e13775 for SWITCHUI-1209 */ 
    /*Function to get search bar flex, flase for default*/
    public static boolean getSearchBarHideSetting(Context context){
        /*if (mFirstTime){
            mSearchBarHide = getItemValue(context, CDA_TAG_HOMESETTING, TAG_HOMESETTING, TAG_SEACH_BAR_HIDE, false);
            //Log.d(TAG,"First time get search bar hide setting, mSearchBarHide = " + mSearchBarHide);
            mFirstTime = false;
        }
        //Log.d(TAG,"Return mSearchBarHide = " + mSearchBarHide);
        return mSearchBarHide;*/
        return true;
    }
    /* 2012-5-22 add by e13775 for SWITCHUI-1209 end */ 

   /*Function to getPRCversion flex,false for default*/
    public static boolean getMarketVersion(Context context){
	return getItemValue(context ,CDA_TAG_HOMESETTING, TAG_HOMESETTING ,TAG_VERSION_PRC ,false);
    }

    /*Function to get market intent ,google market for default*/
    public static Intent getMarketIntent(Context context){
	boolean isPRC = getMarketVersion(context);
        Intent intent = new Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
	
	String marketPackage = isPRC ? ANDROIDMARKET_PACKAGE : MOTOROLAMARKET_PACKAGE;
	String marketClassName = isPRC ? ANDROIDMARKET_CLASS :MOTOROLAMARKET_CLASS ;
        intent.setClassName(marketPackage, marketClassName);
	return intent;
    }


    /*Added for wallpaper flex begin*/
    /*Added by ncqp34 at Jan-09-2012 for wallpaper flex*/
    public static String getWallpaperFlex(Context context){
	String xmlstr = "";
	xmlstr = Flex.getResultFromCDA(CDA_TAG_WALLPAPER,context);
	Log.d(TAG,"wallpaper-flex ==" + xmlstr);
	return xmlstr;
    }
	/**
	 * @hide
	 * Return whether have flex wallpapers
	 */
	public static boolean isHiddenhasWallpaper() {
		File fThumb = new File(HiddenWallpaperPath);
		File[] fThumbs = fThumb.listFiles();
		if ((fThumbs != null) && (fThumbs.length > 0))
			return true;
		else
			return false;
	}

	/**
	 * @hide
	 * Return count of flex wallpapers
	 */
	public static int getHiddenhasWallpaperCount() {
		if (mImagesFromAPHidden != null)
			return mImagesFromAPHidden.size();
		else
			return 0;
	}

	/**
	 * @hide
	 * Add wallpaper into list
	 */
	public static  void addWallpapersFromHidden(String xmlstr ,ArrayList<Integer> mThumbs ,ArrayList<Integer> mImages){
		Element root = null;
		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;
		if ((null != xmlstr) && ("" != xmlstr)) {
			try {
				XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
				factory.setNamespaceAware(true);
				XmlPullParser parser = factory.newPullParser();
				parser.setInput(new StringReader(xmlstr));
				XmlUtils.beginDocument(parser, TAG_WALLPAPERLIST);
				
				dbf = DocumentBuilderFactory.newInstance();
				db = dbf.newDocumentBuilder();
				Document xmldoc = db.parse(new InputSource(new StringReader(xmlstr)));
				root = xmldoc.getDocumentElement();
				NodeList wallpaperList = root
						.getElementsByTagName(TAG_WALLPAPER);
				int wallpaperListLen = wallpaperList.getLength();
				Log.d(TAG,"wallpaperListLen==" + wallpaperListLen);

				mThumbsFromAPHidden = new ArrayList<String>(
						wallpaperListLen + 2);
				mImagesFromAPHidden = new ArrayList<String>(
						wallpaperListLen + 2);
				for (int i = 0; i < wallpaperListLen; i++) {
					Element currentNode = (Element) wallpaperList.item(i);
					//Added by ncqp34 at Feb-13-2012 for return null 
					//from currentNode.getElementsByTagName(TAG_WALLPAPER)*/		
					String wallpaperName = wallpaperList.item(i).getFirstChild().getNodeValue();
					//String wallpaperName = currentNode.getElementsByTagName(TAG_WALLPAPER).item(0)
					//		.getFirstChild().getNodeValue();
					addWallpaperByName(wallpaperName, i , mThumbs, mImages);

					Log.d(TAG,"getAttribute-before--mDefaultPos==" + mDefaultPos);
					Log.d(TAG,"getAttributeValuse-==" + currentNode.getAttribute("default"));
					if (currentNode.getAttribute("default").equals("true")){
						mDefaultPos = i;
					}

					}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void addWallpaperByName(String wallpaperFullname, int i ,
					ArrayList<Integer> mThumbs ,ArrayList<Integer> mImages){
		int idx = wallpaperFullname.lastIndexOf('.');
		String wallpapername = wallpaperFullname.substring(0, idx);
		HiddenWallpaperPath = wallpapername.substring(0,wallpapername.lastIndexOf("/")+1);
		Log.d(TAG,"HiddenWallpaperPath  ==" + HiddenWallpaperPath);	
		if(HiddenWallpaperPath == null || "".equals(HiddenWallpaperPath)){
			HiddenWallpaperPath = DEFAULT_FLEX_WALLPAPER_PATH;
			Log.d(TAG,"HiddenWallpaperPath  DEFAULT==" + HiddenWallpaperPath);
		}
		String wallpaperTypename = wallpaperFullname.substring(idx + 1);
		String wallpaperThumbname = wallpapername + "_small."
				+ wallpaperTypename;
		mThumbs.add(i);
		mThumbsFromAPHidden.add(wallpaperThumbname);
		mImages.add(i);
		mImagesFromAPHidden.add(wallpaperFullname);
	}
	/*ended by ncqp34*/

    /*Separate wallpaper flex end*/
    public static Bitmap getDefaultWallpaperBitmapFromCDA(Context context) {
        String xmlstr = "";
        xmlstr = getResultFromCDA(CDA_TAG_WALLPAPER ,context);
        Log.d(TAG, "getDefaultWallpaperBitmapFromCDA xmlstr:" + xmlstr);
        if (xmlstr.trim().equals("")) {
            // no CDA default wallpaper
            return null;
        }

        String defaultPath = null;

        // analysis default wallpaper
        Element root = null;
        DocumentBuilderFactory dbf = null;
        DocumentBuilder db = null;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xmlstr));
            XmlUtils.beginDocument(parser, TAG_WALLPAPERLIST);
            
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            Document xmldoc = db.parse(new InputSource(new StringReader(xmlstr)));
            root = xmldoc.getDocumentElement();
            NodeList wallpaperList = root.getElementsByTagName(TAG_WALLPAPER);
            int len = wallpaperList.getLength();

            // get default one
            for (int i=0; i<len; i++) {
                Element ele = (Element) wallpaperList.item(i);
                if (ele.getAttribute("default").equals("true")) {
                    defaultPath = ele.getElementsByTagName(TAG_WALLPAPER).item(0).getFirstChild().getNodeValue();
                    break;
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "getDefaultWallpaperBitmapFrom CDA met an exception:" + e);
            e.printStackTrace();
        }

        Log.d(TAG, "getDefaultWallpaperBitmapFromCDA path:" + defaultPath);
        if (defaultPath == null) {
            return null;
        }

        try {
            Bitmap bmp = BitmapFactory.decodeFile(defaultPath);
            Log.d(TAG, "getDefaultWallpaperBitmapFromCDA bmp:" + bmp);
            return bmp;
        } catch (Exception e) {
            Log.e(TAG, "getDefaultWallpaperBitmapFromCDA decode met an exception:" + e);
            e.printStackTrace();
        }

        return null;
    }
    /** jimmyli end */

    /*ended for wallpaper flex*/


    /*MenuOrder flex begin*/

    /**
     * Load the Menu Sort form CDA
     * @param xmlstr the value of Menu Sort from CDA
     */
    public static final Comparator<ApplicationInfo> getMenuOrderComparator(Context context){

        Comparator<ApplicationInfo> APPS_COMPARATOR; 
        if( hasMenuSort(context) ){
            APPS_COMPARATOR = CT_APPS_COMPARATOR;

        }else {
            APPS_COMPARATOR = LauncherModel.APP_NAME_COMPARATOR;
        }

	return APPS_COMPARATOR;
    }
    
    /**
     * check if has Menu Sort from CDA or default XML
     */
    private static boolean hasMenuSort(Context context){
    
        boolean isSort = true;
        boolean needCDA = false;
        String xmlstr = "";
        mApps_list = new ArrayList<String>();
        
        xmlstr = getResultFromCDA(CDA_TAG_MAINMENUSORT ,context);
	Log.d(TAG,"hasMenuSort---xmlstr ==" + xmlstr);
        if (!xmlstr.trim().equals("")){
            needCDA = true;	
        }
        
        if (needCDA) {
            loadMenuSortFromHidden(xmlstr);
        } else {
            
            Resources r = context.getResources();
            final String[] extras = r.getStringArray(R.array.menu_order);
            if( extras.length > 0 ){
                for (String extra : extras) {
                    
                    mApps_list.add(extra);
                }
            }else{
                
                isSort = false;
            }
        }
        if ( isSort ){
            CT_APPS_COMPARATOR = new ApplicationInfoComparator(mApps_list);	
        }
        return isSort;
    }


    private static void loadMenuSortFromHidden(String xmlstr) {
        
        Element root = null;
        DocumentBuilderFactory dbf = null;
        DocumentBuilder db = null;
        
        try {
        
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xmlstr));
            XmlUtils.beginDocument(parser, TAG_MENUORDER);
            
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            
            Document xmldoc = db.parse(new InputSource(new StringReader(xmlstr)));
            root = xmldoc.getDocumentElement();
            
            NodeList appsList = root.getElementsByTagName(TAG_APP);
            int appsListLen = appsList.getLength();
            
            for (int i = 0; i < appsListLen; i++) {
            
                Element currentNode = (Element) appsList.item(i);
		/*Added by ncqp34 at Mar-12-2012 for menu flex*/
                //String appName = currentNode.getElementsByTagName(TAG_APP)
                //    .item(0).getFirstChild().getNodeValue();
                String appName = currentNode.getFirstChild().getNodeValue();
		Log.d(TAG,"loadMenuSortFromHidden---appName ==" + appName);
		/*ended by ncqp34*/
                //add the app to the list
                mApps_list.add(appName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

      static class ApplicationInfoComparator implements Comparator<ApplicationInfo> {

            private static HashMap<String, Integer> mAppPositionMap = new HashMap<String,Integer>();       
            
            ApplicationInfoComparator(ArrayList<String> mList) {
            
                for (int i=0; i < mList.size(); i++) {                	 
                    Integer value= new Integer(i);         
                    mAppPositionMap.put(mList.get(i),value);               
                }   
                return;
            }

            public final int compare(ApplicationInfo a, ApplicationInfo b) {
            
                String str_a = a.title.toString();
                String str_b = b.title.toString();
                int len_a = str_a.length();
                int len_b = str_b.length();
                Integer int_a; 
                Integer int_b;    
                int_a =  mAppPositionMap.get(a.componentName.getClassName());
                int_b = mAppPositionMap.get(b.componentName.getClassName());              
                /* setup 1:sort according to  ct app list */
                if ((null ==int_a) && (null !=int_b)) 
                    return 1;
                else  if ((null !=int_a) && (null==int_b))
                    return -1;
                else if ((null !=int_a) && (null !=int_b)){              
                    if (int_a.intValue()>int_b.intValue())
                        return 1;
                    else 
                        return -1;
                }
                return Collator.getInstance().compare(a.title.toString(), b.title.toString());
            }
        }

    /*MenuOrder flex end*/
    /*ended by ncqp34*/	

   /*Added by ncqp34 at Mar-14 for fake app*/
    //Constant for fake app
    private static final String CDA_TAG_DOWNLOAD_APP = "@MOTOFLEX@getDownloadApplication"; //"@MOTOFLEX@getMainMenuSort";
    private static final String TAG_APPLICATION = "DownloadApplications";//"application";//"main-menu-order" ;

    public static void getFakeAppFromCDA(SQLiteDatabase db, Context context){
	boolean needCDA = isFlexValid(context) ;
	
        String xmlstr = getResultFromCDA(CDA_TAG_DOWNLOAD_APP , context);
        if(! xmlstr.trim().equals("")){
	    needCDA = true;
	}

	if(needCDA){
	   loadDldAppFromCDA(db, xmlstr, context);
	}else{
	   //Whether need support from default xml
	   return;
	}
    }

    private static void loadDldAppFromCDA(SQLiteDatabase db, String xmlResult ,Context context){

	ArrayList <ApplicationInfo> fakeApps = new ArrayList <ApplicationInfo> () ;
	try{
	XmlPullParserFactory factory;
        factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        factory.setNamespaceAware(true);
        parser.setInput(new StringReader(xmlResult));    
 	XmlUtils.beginDocument(parser,TAG_APPLICATION );
        final int depth = parser.getDepth();
	int type;
	  while (((type = parser.next()) != XmlPullParser.END_TAG ||
                        parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

              if (type != XmlPullParser.START_TAG) {
                  continue;
              }
              final String name = parser.getName();
              Log.d(TAG, "load fake application xml tag name:" + name);
              String packageName="";
              String className="";
              String iconPath = "";
		String title = "";
		String uri = "";	
		for(int j=0;j<parser.getAttributeCount();j++){
                            if(parser.getAttributeName(j).equals("packageName"))
				packageName = parser.getAttributeValue(j);
                            if(parser.getAttributeName(j).equals("className"))
				className = parser.getAttributeValue(j);
                            else if(parser.getAttributeName(j).equals("icon"))
				iconPath = parser.getAttributeValue(j);
                            else if(parser.getAttributeName(j).equals("label"))
				title= parser.getAttributeValue(j);
                            else if(parser.getAttributeName(j).equals("uri"))
                                uri=parser.getAttributeValue(j);
		} 
		Log.d(TAG,"FakeApp info :packageName ==" + packageName + ",className ==" + className + 
		", iconPath==" + iconPath +  ",title ==" +title +  ",uri ==" + uri);

             Bitmap bmp = null;
	     try{
	      	bmp = BitmapFactory.decodeFile(iconPath);
	        Log.d(TAG,"FakeApp--icon size:width ==" + bmp.getWidth() + " , height ==" + bmp.getHeight());
	     } catch(Exception e){
		Log.w(TAG,"FakeApp--get flex icon error");	
		//Drawable d = resources.getDrawableForDensity(iconId, mIconDpi);
	     }
	     ComponentName cn = new ComponentName(packageName,className );

	     Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(uri));
	     intent.putExtra(FakeAppModel.EXTRA_COMPONENT, cn.flattenToString());
	
	     Log.d(TAG,"FakeApp--cn=" + cn.flattenToString());
	     ContentValues value = new ContentValues();
	     value.put(FakeAppProvider.FakeApplication.COMPONENT_NAME ,cn.flattenToString());
	     value.put(FakeAppProvider.FakeApplication.TITLE ,title);
	     value.put(FakeAppProvider.FakeApplication.ICON ,iconPath);
	     value.put(FakeAppProvider.FakeApplication.URI ,uri);
	     value.put(FakeAppProvider.FakeApplication.HIDDEN ,0);

	     db.insert(FakeAppProvider.TABLE_FAKE_APPLICATION, null, value);
              
	  }
	}catch (XmlPullParserException e) {
                Log.w(TAG, "Got exception parsing fake application.", e);
        } catch (IOException e) {
                Log.w(TAG, "Got exception parsing fake application.", e);
        } catch (RuntimeException e) {
                Log.w(TAG, "Got exception parsing fake application.", e);
        }

	//return fakeApps;
    }
   /*ended by ncqp34*/
}
