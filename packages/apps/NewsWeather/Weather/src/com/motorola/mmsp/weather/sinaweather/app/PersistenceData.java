package com.motorola.mmsp.weather.sinaweather.app;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;

public class PersistenceData {
	private static final String AUTO_CITY = "setting_auto_city";
	private static final String PACKAGE = "com.motorola.mmsp.sinaweather.app_preferences";
	//private static final String PACKAGE = "com.motorola.mmsp.weather.app_preferences";
	private static final String CITIES = "weather_cities";
	private static final String CITIYIDS = "weather_cityIds";	
	
	private static final String EverSetDefaultValueName = "cdavalues.txt";
	public static final String EverSetDefaultValuePath = "/data/data/com.motorola.mmsp.weather/files/" + EverSetDefaultValueName;
	 
	public static final String SETTING_UPDATE_FREQUENCE = "setting_update_frequency";
	public static final String SETTING_TEMP_UNIT = "temperature_uint";
	public static final String SETTING_ROAMING = "roaming";
	public static final String SETTING_CAROUSEL = "carousel";
	public static final String SETTING_DISCLAIMER = "disclaimer"; 
	
	public static final String CITY_WEATHER_TIME = "city_weather_time";
	
	private PersistenceData() {
		
	}

//	public static SharedPreferences getSharedPreferences(Context context) {
//		return context.getSharedPreferences(PACKAGE, 0);
//	}
	
	public static int getIndexByCityId(Context context, String cityId) {
		int index = 0;
		ArrayList<String> cityIds = getCityids(context);
		for (int i=0; i<cityIds.size(); i++) {
			if (cityId.equals(cityIds.get(i))) {
				index = i;
				Log.d("WeatherWidget", "Match the city, index = " + index);
				break;
			}
		}
		return index;
	}
	
	public static String getCityIdByIndex(Context context, int index) {
		String cityId = "";
		ArrayList<String> cityIds = getCityids(context);
		if (index >= 0 && index < cityIds.size()) {
			cityId = cityIds.get(index);
			Log.d("WeatherWidget", "Match the city, cityId = " + cityId);
		}
		return cityId;
	}
	

	public static ArrayList<String> getCityids(Context context) {
		return getStringList(context, CITIYIDS);
	}
	

	public static String getCityName(Context context,String  cityid) {
		return DatabaseHelper.readSetting(context, cityid);
	    //return getSharedPreferences(context).getString(cityid, "");
	}
	

	public static void setCity(Context context, String cityId, String cityName) {
		Log.d("WeatherWidget", "setCity, cityId = " + cityId + "cityName = " + cityName);
		DatabaseHelper.writeSetting(context, cityId, cityName);
	}
	
	private static ArrayList<String> getCities(Context context) {
		return getStringList(context, CITIES);
	}
	
	private static void setCities(Context context, ArrayList<String> cities) {
		setStringList(context, CITIES, cities);
	}
	

	
	public static void setCityids(Context context, ArrayList<String> cities) {
		setStringList(context, CITIYIDS, cities);
	}
	
	public static void setStringList(Context context, String key, ArrayList<String> cities) {
		Log.d("WeatherWidget", "setStringList, key = " + key + "cities = " + cities.toString());
		DatabaseHelper.writeSetting(context, key, cities.toString());
//		Editor edit = getSharedPreferences(context).edit();
//		edit.putString(key,cities.toString());
//		edit.commit();
	}
	
	public static ArrayList<String> getStringList(Context context, String key) {
		//String s = getSharedPreferences(context).getString(key, null);
		String s = DatabaseHelper.readSetting(context, key);
		ArrayList<String> list = new ArrayList<String>();
		if (s != null && !s.equals("") && !s.equals("[]")) {
			String ss = s.replace('[', '\0');
			ss = ss.replace(']', '\0');
			String items[] = ss.split(",");
			for (int i=0; i < items.length; i++) {
				list.add(items[i].trim());
			}
		}
		return list;
	}
	
	public static boolean isCityListEmpty(Context context) {
		boolean isEmpty = false;
		String s = DatabaseHelper.readSetting(context, CITIYIDS);
		if (s == null || s.equals("")) {
			isEmpty = true;
		} else {
			isEmpty = false;
		}
		return isEmpty;
	}
	
	public static boolean getAutoCity (Context context) {
		String value = DatabaseHelper.readSetting(context, AUTO_CITY);
		boolean isAutoCity = false;
		if (value != null && !value.equals("")) {
		    isAutoCity = Boolean.parseBoolean(value);	
		}
		return isAutoCity;
		//return getSharedPreferences(context).getBoolean(AUTO_CITY, false);
	}
	
	public static void setAutoCity (Context context, boolean b) {
		DatabaseHelper.writeSetting(context, AUTO_CITY, (b ? "true" : "false"));
//		Editor edit = getSharedPreferences(context).edit();
//		edit.putBoolean(AUTO_CITY, b);
//		edit.commit();
	}
	
	public static void setDisclaimer(Context context, boolean b) {
		DatabaseHelper.writeSetting(context, SETTING_DISCLAIMER, (b ? "true" : "false"));
//		Editor edit = getSharedPreferences(context).edit();
//		edit.putBoolean(SETTING_DISCLAIMER, b);
//		edit.commit();
	}
	
	public static boolean getDisclaimer(Context context) {
		String value = DatabaseHelper.readSetting(context, SETTING_DISCLAIMER);
		boolean isDisclaimed = false;
		if (value != null && !value.equals("")) {
			isDisclaimed = Boolean.parseBoolean(value);	
		}
		return isDisclaimed;
//		return getSharedPreferences(context).getBoolean(SETTING_DISCLAIMER, true);
	}
	
	public static void fetchCDAValues(Context context) {
		if (!DatabaseHelper.isSettingIntialized(context)) {
			String xmlstr = null;
			boolean bUnit = false;
			boolean bFrequency = false;
			boolean bDisclaimer = false;
			// Editor edit = getSharedPreferences(context).edit();

			TypedValue isCDA = new TypedValue();
        	TypedValue contentCDA = new TypedValue();
			Resources r = context.getResources();
			try {
        		r.getValue("@FIHCDA@isCDAValid", isCDA, false);                                               
            	if(isCDA.coerceToString().equals("true")){
            		r.getValue("@FIHCDA@getMotoWeatherSetting", contentCDA, false);
					xmlstr = contentCDA.coerceToString().toString();
				}
        	} catch (Exception e){
        		e.printStackTrace();
        	} 
			Element root = null;
        	DocumentBuilderFactory dbf = null;
        	DocumentBuilder db = null;
		
			if((null != xmlstr) && (!"".equals(xmlstr))) {
				Log.v("WeatherWidget", "fetchCDAValues: " + xmlstr);
				try {
					dbf = DocumentBuilderFactory.newInstance();
            		db = dbf.newDocumentBuilder();
            		Document xmldoc = db.parse(new InputSource(new StringReader(xmlstr)));
            		root = xmldoc.getDocumentElement();
					NodeList list = root.getChildNodes();
					if (list != null) {
						int len = list.getLength();
			    		for(int i = 0; i < len; i++) {
							Node currentNode = list.item(i);
							String name = currentNode.getNodeName();
							String value = currentNode.getTextContent();
							if (value != null && name != null && name.equals("temperature-unit")) {
								bUnit = true;
                                DatabaseHelper.writeSetting(context,
										SETTING_TEMP_UNIT, value);
								//edit.putString(SETTING_TEMP_UNIT, value);
							} else if (value != null && name != null && name.equals("weather-update-frequency")) {
								bFrequency = true;
                                DatabaseHelper.writeSetting(context,
										SETTING_UPDATE_FREQUENCE, value);
								//edit.putString(SETTING_UPDATE_FREQUENCE, value);
							}else if (value != null && name != null && name.equals("weather-disclaimer")) {
								bDisclaimer = true;
                                DatabaseHelper.writeSetting(context,
										SETTING_DISCLAIMER, value);
								//edit.putBoolean(SETTING_DISCLAIMER, Boolean.parseBoolean(value));
							}
						}
					}					
            	} catch (Exception e) {
                 	e.printStackTrace();
            	} 
			} 
			
			if (!bUnit) {
                DatabaseHelper.writeSetting(context, SETTING_TEMP_UNIT, "0");
				//edit.putString(SETTING_TEMP_UNIT, "0");
			}
			if (!bFrequency) {
                DatabaseHelper.writeSetting(context, SETTING_UPDATE_FREQUENCE,
						"0");
				//edit.putString(SETTING_UPDATE_FREQUENCE, "0");
			}
			if (!bDisclaimer) {
				DatabaseHelper
						.writeSetting(context, SETTING_DISCLAIMER, "true");
//				edit.putBoolean(SETTING_DISCLAIMER, true);
			}
//			edit.commit();
//			
//			try {
//				OutputStream os = context.openFileOutput(EverSetDefaultValueName, Context.MODE_PRIVATE);
//				os.write(1);
//				os.close();
//				Log.v("WeatherWidget", "Fetch values from CDA");
//				return true;
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		}
//		return false;
	}
}
