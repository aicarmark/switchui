package com.motorola.mmsp.weather.sinaweather.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.motorola.mmsp.weather.sinaweather.service.WeatherInfo.Forecast;
import com.motorola.mmsp.weather.sinaweather.service.WebServiceHelper.ParseException;

public class WeatherUtils {	
	public enum UpdateResult{
		SUCCESS,
		FAILED,
	};
	public enum AnimaitonArea {
		WIDGET,
		SCREEN,
	};
	public static int EXT_QUE_CAN_NOT_GET_CITY = 1;
	public static int EXT_QUE_CITY_IS_INVALID = 2;
	public static int EXT_QUE_CAN_NOT_GET_WEATHER = 3;
	public static int EXT_QUE_WEATHER_IS_INVALID = 4;
	public static int EXT_QUE_NETWORK_UNAVAILABLE = 5;
	
	private static String weatherSev = "http://data.3g.sina.com.cn/api/index.php?wm=ib009&cid=14";
	private static final String SEARCH_CITY_URL = "http://data.3g.sina.com.cn/api/index.php?wm=ib009&cid=14";
	
	public static String getTID() {
		return "TID " + String.valueOf(Thread.currentThread().getId()) + "  ";
	}
	
	public  static WeatherInfo updateWeather(Context context, String cityId) {
		Log.d("WeatherService", WeatherUtils.getTID() + "WeatherUtils updateWeather by cityId, 2 parameters");		
		return updateWeather(context, cityId, false, null);
	}
	
	//for internal - isExternal is false and queryName is null
	//for external - isExternal is true and queryName is the external queryName
	public static WeatherInfo updateWeather(Context context, String cityId, boolean isExternal, String queryName) {
		Log.d("WeatherService", WeatherUtils.getTID() + "WeatherUtils updateWeather by cityId, 3 parameters");
		WeatherInfo weather = null;
		try {
			String url = getApiUri(cityId);
			InputStream in = WebServiceHelper.getConnect(context,url);
			if (in != null) {
				WeatherInfo temp = new WeatherInfo();
				parseWeaherInfo(in, temp, cityId);
				if (temp.infomation.city != null && !temp.infomation.city.equals("")) {
					weather = temp;
					Calendar calendar = Calendar.getInstance();
					weather.infomation.requestTime = String.valueOf(calendar.getTimeInMillis());
					int hour = calendar.get(Calendar.HOUR_OF_DAY);
					if (hour >= 6 && hour < 19) {
						weather.infomation.dayNight = WeatherInfo.DAY;
					} else {
						weather.infomation.dayNight = WeatherInfo.NIGHT;
					}
					DatabaseHelper.writeWeather(context, weather, cityId, isExternal);
				    if (isExternal) {
						context.getContentResolver().notifyChange(Uri.withAppendedPath(Uri.parse("content://" + WeatherServiceConstant.AUTHORITY + "/forecast"), queryName), null);
						Log.d("WeatherService", WeatherUtils.getTID() + "notify  content://" + WeatherServiceConstant.AUTHORITY + "/forecast/" + queryName);
				    }
				} else {
					Log.d("WeatherService", WeatherUtils.getTID() + "data from internet is invalid!!!!!!");
					if (isExternal) {
						((UpdateService) context).fireExternalQueryFailure(queryName, WeatherUtils.EXT_QUE_WEATHER_IS_INVALID);
					}
				}
				in.close();
				
			} else {
				Log.d("WeatherService", WeatherUtils.getTID() + "in is null, data from internet is invalid !!!!!!!! ");
				if (isExternal) {
					((UpdateService) context).fireExternalQueryFailure(queryName, WeatherUtils.EXT_QUE_CAN_NOT_GET_WEATHER);
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return weather;
	}

	//@Override
	public static WeatherInfo getWeather(Context context, String cityId) {
		WeatherInfo weather = null;
		
		try {
			weather = DatabaseHelper.readWeather(context, cityId);
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return weather;
	}
	/*
	public static void parseWeaherInfo(InputStream content, WeatherInfo weatherInfo) {		
		try {
			if (content == null && weatherInfo != null) {
				weatherInfo.error = "100";
				return;
			}
			DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
			DocumentBuilder dombuilder = domfac.newDocumentBuilder();
			Document doc = dombuilder.parse(content);
			//File f = new File("sdcard/api_sina.xml");
			//Document doc =dombuilder.parse(f); 
			
			Element root = doc.getDocumentElement();
			Node node = root;
			
			if (node != null) {
				weatherInfo.error = getNamedItemValue(node, "errno");
				if (weatherInfo.error != null && !weatherInfo.error.equals("")){
					return;
				}
			}			
			
			while(node != null && !node.getNodeName().equals("root")){
				node = node.getFirstChild();
			}
			
			Node items = null;
			if (node != null) {
				NodeList root_items = node.getChildNodes();
				for (int i = 0; root_items != null && i < root_items.getLength(); i++) {
					Node n = root_items.item(i);
					if (n.getNodeName().equals("items")) {
						items = n;
						break;
					}
				}
			}
			if (items != null) {
				weatherInfo.infomation.unit = String.valueOf(0);
				NodeList nodes = items.getChildNodes();
				for (int i=0, item = 0; nodes != null && i < nodes.getLength(); i++) {
					Node n = nodes.item(i);
					if (n != null && n.getNodeName() != null && n.getNodeName().equals("item")) {
						parseWeaherInfo(n,item++,weatherInfo);
					}
				}
			}	
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}*/
	public static void parseWeaherInfo(InputStream content, WeatherInfo weatherInfo, String cityId) {		
		try {
			if (content == null && weatherInfo != null) {
				weatherInfo.error = "100";
				return;
			}
			DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
			DocumentBuilder dombuilder = domfac.newDocumentBuilder();
			Document doc = dombuilder.parse(content);

			Element root = doc.getDocumentElement();
			Node rootNode = root;
			
			if (rootNode != null) {
				weatherInfo.error = getNamedItemValue(rootNode, "errno");
				if (weatherInfo.error != null && !weatherInfo.error.equals("")){
					return;
				}
			}			
			
			while(rootNode != null && !rootNode.getNodeName().equals("root")){
				rootNode = rootNode.getFirstChild();
			}
			
			Node correctItemsNode = findItemsNodeHasValidCity(rootNode, cityId);
			
			if (correctItemsNode != null) {
				weatherInfo.infomation.unit = String.valueOf(0);
				NodeList nodes = correctItemsNode.getChildNodes();
				for (int i=0, item = 0; nodes != null && i < nodes.getLength(); i++) {
					Node n = nodes.item(i);
					if (n != null && n.getNodeName() != null && n.getNodeName().equals("item")) {
						parseWeaherInfo(n,item++,weatherInfo);
					}
				}
			}	
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static Node findItemsNodeHasValidCity(Node rootNode, String cityName) {
		Node forceReturnNode = null;
		if (rootNode != null) {
		    Log.d("WeatherService", WeatherUtils.getTID() + "@@@@@@@@@@  findItemsNodeHasValidCity begin @@@@@@@@@@");
		    NodeList itemsNodeList = rootNode.getChildNodes();
		
		    for (int i = 0; itemsNodeList != null && i < itemsNodeList.getLength(); i++) {
		        Log.d("WeatherService", WeatherUtils.getTID() + "i = " + i);
		        Node itemsNode = itemsNodeList.item(i);
		        if (itemsNode != null && itemsNode.getNodeName() != null && itemsNode.getNodeName().equals("items")) {
		            NodeList forecastNodeList = itemsNode.getChildNodes();
		            if (forceReturnNode == null) {
		                forceReturnNode = itemsNode;
		            }
		            for (int j = 0; forecastNodeList != null && j < forecastNodeList.getLength(); j++) {
		                Node forecast = forecastNodeList.item(j);
		                if (forecast != null && forecast.getNodeName() != null && forecast.getNodeName().equals("item")) {
		                    String city = getNamedItemValue(forecast, "city");
		                    Log.d("WeatherService", WeatherUtils.getTID() + "city = " + city);
		                    if (city != null && city.equals(cityName)) {
		                        Log.d("WeatherService", WeatherUtils.getTID() + "@@@@@@@@@@  findItemsNodeHasValidCity end, find the correct city, choose " + i + " item @@@@@@@@@@");
		                        forceReturnNode = itemsNode;
		                        break;
		                    }
					    }
				    }
			    }
		    }
		}
		Log.d("WeatherService", WeatherUtils.getTID() + "@@@@@@@@@@  findItemsNodeHasValidCity end, can not find the correct city, just choose the first items @@@@@@@@@@");
		return forceReturnNode;
	}
	
	private static void parseWeaherInfo(Node node, int i, WeatherInfo weatherInfo) {
		Forecast forecast = weatherInfo.newForecast();
		NodeList nodes = node.getChildNodes();
		for (int item = 0; item < nodes.getLength(); item ++) {
			Node n = nodes.item(item);
			String name = n.getNodeName();
			String value = null;
			if (n.getChildNodes() != null) {
				if (n.getChildNodes().item(0) != null) {
					value = n.getChildNodes().item(0).getNodeValue();
				}
			}
			if (name.equals("city")) {
				if (i == 0) {
					weatherInfo.infomation.city = value;
				}
			} else if (name.equals("obsdate") || name.equals("date")) {
				forecast.date = value;
				if (i == 0) {
					weatherInfo.infomation.date = value;
				}
			} else if (name.equals("status")) {
				//forecast.condition = value;				
			} else if (name.equals("status1")) {
				forecast.condition = value;
				if (i == 0) {
					weatherInfo.current.condition = value;
				}
			} else if (name.equals("daycode")) {
				forecast.day = value;
			} else if (name.equals("power")) {
				forecast.windPower = value;
			} else if (name.equals("hightemperature") || name.equals("temperature1")) {
				forecast.high = value;
			} else if (name.equals("lowtemperature") || name.equals("temperature2")) {
				forecast.low = value;
			} else if (name.equals("tgd1")) {
                                if (i == 0) {
				    weatherInfo.current.realFeel = value;
                                }
			}
		}
		
		if (i < weatherInfo.forecasts.size()) {
			weatherInfo.forecasts.set(i, forecast);
		} else {
			weatherInfo.forecasts.add(i, forecast);
		}
	}

	//@Override
	public static String getApiUri(String city) {
		String encodeCity = null;
		try {
			encodeCity = URLEncoder.encode(city, "UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return weatherSev + "&word=" + encodeCity + "&ver=2&day=5";
	}



	//@Override
	public static ArrayList<Map> searchCity(Context context, String searchString) {
		Log.d("WeatherService", WeatherUtils.getTID() + "searchCity begin");
		if (searchString != null && !searchString.equals("")) {
			searchString = searchString.trim();
		}
		ArrayList<Map> cities = new ArrayList<Map>();
		try {
			String queryString = SEARCH_CITY_URL + "&word=" + URLEncoder.encode(searchString, "UTF8") + "&ver=1&day=1";
			Log.d("WeatherService", WeatherUtils.getTID() + "query city url is " + queryString);
			DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
			DocumentBuilder dombuilder = domfac.newDocumentBuilder();
			InputStream content = WebServiceHelper.getConnect(context,queryString);
			
			if (content == null) {
				Log.d("WeatherService", WeatherUtils.getTID() + "searchCity end, cities is null");
				return cities;
			}
				
			Document doc = dombuilder.parse(content);
			Element root = doc.getDocumentElement();
			Node node = root;
			while(node != null && (!node.getNodeName().equals("citylist") && !node.getNodeName().equals("root"))){
				node = node.getFirstChild();
			}			
			
			NodeList nodes = null;
			if (node != null) {
				nodes = node.getChildNodes();
			}
			
			for (int i=0; nodes != null && i < nodes.getLength(); i++) {
				Node n = nodes.item(i);
				if (n.getNodeName().equals("location")) {
					Node attrCity = null;;
					if (n.getAttributes() != null && (attrCity =n.getAttributes().getNamedItem("city")) != null){
						HashMap<String,String> map = new HashMap<String,String>();
						map.put("city", attrCity.getNodeValue());
						map.put("location", attrCity.getNodeValue());
						cities.add(map);
					}					
				} else if (n.getNodeName().equals("items"))	{
					NodeList items = n.getChildNodes();
					Node item1 = null;
					if ((items != null) && ((item1 = items.item(1))!= null)) {
						String city = getNamedItemValue(item1, "city");
						if (city != null && !city.equals("")) {
							HashMap<String,String> map = new HashMap<String,String>();
							map.put("city", city);
							map.put("location", city);
							cities.add(map);
						}
					}
				}
			}
		    content.close();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
//		String cityId = "";
//		cityId = (String) cities.get(0).get("location");
//		if (!cityId.equals("")) {
//			DatabaseHelper.storeCityQueryName(context, searchString, cityId);
//		}
		Log.d("WeatherService", WeatherUtils.getTID() + "searchCity end, return cities");
		return cities;
	}
	
	public static ArrayList<Map> externalSearchCity(Context context, String searchString) {	
		Log.d("WeatherService", WeatherUtils.getTID() + "externalSearchCity begin");
		if (searchString != null && !searchString.equals("")) {
			searchString = searchString.trim();
		}
		
		ArrayList<Map> cities = new ArrayList<Map>();
		try {
			String queryString = SEARCH_CITY_URL + "&word=" + URLEncoder.encode(searchString, "UTF8") + "&ver=1&day=1";
			Log.d("WeatherService", WeatherUtils.getTID() + "query city url is " + queryString);
			DocumentBuilderFactory domfac = DocumentBuilderFactory.newInstance();
			DocumentBuilder dombuilder = domfac.newDocumentBuilder();
			InputStream content = WebServiceHelper.getConnect(context,queryString);
			
			if (content == null) {
				Log.d("WeatherService", WeatherUtils.getTID() + "searchCity end, cities is null");
				return cities;
			}
				
			Document doc = dombuilder.parse(content);
			Element root = doc.getDocumentElement();
			Node node = root;
			while(node != null && (!node.getNodeName().equals("citylist") && !node.getNodeName().equals("root"))){
				node = node.getFirstChild();
			}			
			
			NodeList nodes = null;
			if (node != null) {
				nodes = node.getChildNodes();
			}
			
			for (int i=0; nodes != null && i < nodes.getLength(); i++) {
				Node n = nodes.item(i);
				if (n.getNodeName().equals("location")) {
					Node attrCity = null;;
					if (n.getAttributes() != null && (attrCity =n.getAttributes().getNamedItem("city")) != null){
						HashMap<String,String> map = new HashMap<String,String>();
						map.put("city", attrCity.getNodeValue());
						map.put("location", attrCity.getNodeValue());
						cities.add(map);
					}					
				} else if (n.getNodeName().equals("items"))	{
					NodeList items = n.getChildNodes();
					Node item1 = null;
					if ((items != null) && ((item1 = items.item(1))!= null)) {
						String city = getNamedItemValue(item1, "city");
						if (city != null && !city.equals("")) {
							HashMap<String,String> map = new HashMap<String,String>();
							map.put("city", city);
							map.put("location", city);
							cities.add(map);
						}
					}
				}
			}
		content.close();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String cityId = "";
		if (cities != null && !cities.isEmpty()) {
		    cityId = (String) cities.get(0).get("location");
		    Log.d("WeatherService", WeatherUtils.getTID() + "cityId = " + cityId);
		}
		if (!cityId.equals("")) {
			String updateTime = String.valueOf(System.currentTimeMillis());
			DatabaseHelper.writeCityQueryName(context, searchString, cityId, updateTime);
		}
		Log.d("WeatherService", WeatherUtils.getTID() + "externalSearchCity end, return cities");
		return cities;
	}

	private static String getNamedItemValue(Node node, String itemName) {
		String value = null;
		if (node != null) {
			NodeList nodes = node.getChildNodes();
			for (int i = 0 ; nodes != null && i < nodes.getLength(); i++) {
				Node item = nodes.item(i);
				if (item != null) {
					String s = item.getNodeName();
					if (s != null && s.equals(itemName)) {
						value = item.getTextContent();
					}
				}
			}
		}
		
		return value;
	}

	public static String getCityIdByQueryName(Context context, String queryName) {
		return DatabaseHelper.getCityIdByQueryName(context, queryName);
	}
	
	public static String getCityIdByLatlon(Context context, String strLatlon) {
		return DatabaseHelper.getCityIdByLatlon(context, strLatlon);
	}
	
	public static boolean isWeatherInfoValid(Context context, String cityId, long timediff, String langId) {
		return DatabaseHelper.isWeatherInfoValid(context, cityId, timediff, langId);
	}
	
	public static String getSettingByName(Context context, String settingName) {
		return DatabaseHelper.readSetting(context, settingName);
	}
	
	public static void writeSetting(Context context, String settingName, String settingValue) {
		DatabaseHelper.writeSetting(context, settingName, settingValue);
	}
	
	
	public static String getCentigrade(String F) {
		if (F == null) {
			return "";
		}
		int c = (int ) (Math.round(1.0f * (Integer.parseInt(F)- 32) * 5 / 9));
		return "" + c;
	}
	
	public static String getFahrenheit(String c) {
		if (c == null) {
			return "";
		}
		int f = (int ) (Math.round(1.0f * Integer.parseInt(c)*9 / 5 + 32));
		return "" + f;
	}
}

