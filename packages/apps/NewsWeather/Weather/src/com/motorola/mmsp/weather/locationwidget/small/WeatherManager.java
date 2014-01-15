package com.motorola.mmsp.weather.locationwidget.small;


import java.util.HashMap;

import com.motorola.mmsp.weather.sinaweather.app.DatabaseHelper;
import com.motorola.mmsp.weather.sinaweather.app.PersistenceData;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

public class WeatherManager {
	public static final int WEATHER_STATE_NORMAL = 0;
	public static final int WEATHER_STATE_NO_WEATHER_INFORMATION = 1;

	// public static final int WEATHER_STATE_NO_CITY = 2;

	public static class WeatherInfo {
		public int mState;
		public String mTemperature;
		public String mCityName;
		public String mTimeZone;
		public String mWeather;
		public String mSunRise;
		public String mSunSet;
	}

	private static final String LOG_TAG = GlobalDef.LOG_TAG;
	private Context mContext = null;
	private String mWeatherServiceEdition = null;
	private HashMap<Integer, WeatherInfo> mWeatherMap;

	private static WeatherManager weatherManager;
	private WeatherManager(Context context) {
		mContext = context;
		mWeatherMap = new HashMap<Integer, WeatherInfo>();
	}
	public static WeatherManager getInstace(Context context)
	{
		if(weatherManager == null)
		{
			weatherManager = new WeatherManager(context);
		}		
		return weatherManager;
	}

	public void setInfo(int widgetId, WeatherInfo info) {
		mWeatherMap.put(widgetId, info);
	}

	public void deleteInfo(int widgetId) {
		if (mWeatherMap.containsKey(widgetId)) {
			mWeatherMap.remove(widgetId);
		}
	}

	public WeatherInfo getInfo(int widgetId, String cityId, SharedPreferences sp) {
		if (mWeatherMap.containsKey(widgetId)) {
			return mWeatherMap.get(widgetId);
		} else {
			return createWeatherInfo(widgetId, cityId, sp);
		}
	}
	
	public WeatherInfo getInfoWithCityId(String cityId, SharedPreferences sp) {
			return createWeatherInfo(cityId, sp);
	}

	public boolean isAccuWeather() {
		return GlobalDef.EDITION_ACCU.equals(mWeatherServiceEdition);
	}

	public void clearAllInfo() {
		mWeatherMap.clear();
	}

	private WeatherInfo createWeatherInfo(String cityId,
			SharedPreferences sp)
	{
		WeatherInfo info = new WeatherInfo();
		
		// query database and set item
		GlobalDef.MyLog.w(LOG_TAG, "database operation");
		String selections = "cityId= ?";
		String[] selectionargs = new String[] { cityId };
		Cursor c = null;
		try {
			c = GlobalDef.query(mContext, GlobalDef.uri_condtions,
					selections, selectionargs, null);
			String cityName = null;
			if (c != null)
			{
				if (c.moveToFirst())
				{// normal
					cityName = c.getString(c.getColumnIndex("cityName"));
					String weatherType = c.getString(c
							.getColumnIndex("weatherType"));
					String condition = c.getString(c.getColumnIndex("condition"));
					String timezone = c.getString(c.getColumnIndex("timezoneAccu"));
					
					GlobalDef.MyLog.d(LOG_TAG, "weatherType=" + weatherType
							+ " condition=" + condition + " timezone=" + timezone);
					
					int temperatureType = c.getInt(c.getColumnIndex("unit"));
	//				int setting_unit = sp.getInt(GlobalDef.TEMPUNIT, 0);
					
					String units = DatabaseHelper.readSetting(mContext,
							PersistenceData.SETTING_TEMP_UNIT);
					if (units.equals("")) {
						units = "0";
					}
					int setting_unit = Integer.parseInt(units);
					
					String temperature = getTemperature(c, temperatureType,
							setting_unit);
					String weatherCodition = null;
					String sunrise = null;
					String sunset = null;
					
					GlobalDef globalDef = new GlobalDef(mContext);
					if (weatherType == null && condition == null)
					{
						weatherCodition = "sunny";
					} else
					{
						if (GlobalDef.chineseValid(condition))
						{
							if (weatherType != null && !weatherType.equals("0"))
							{
								weatherCodition = globalDef
										.getWeatherConditionWithIndex(weatherType);
							} else
							{
								GlobalDef.MyLog.d(LOG_TAG, "match by Chinese");
								GlobalDef.MyLog.d(LOG_TAG, "condition = " + condition);
								weatherCodition = globalDef
										.getWeatherConditionWithIndex(condition);
							}
						} else
						{
							GlobalDef.MyLog.d(LOG_TAG, "not with Chinese");
							weatherCodition = globalDef
									.getWeatherConditionWithIndex(weatherType);
						}
	
						if (weatherCodition != null)
						{
							if (GlobalDef.EDITION_ACCU
									.equals(mWeatherServiceEdition))
							{
								selections = "cityId= ? and dayIndex = 0";
								selectionargs = new String[] { cityId };
								Cursor cur = GlobalDef.query(mContext,
										GlobalDef.uri_forecast, selections,
										selectionargs, null);
								if (cur != null)
								{
									// GlobalDef.MyLog.d(LOG_TAG,"c.getCount()="+c.getCount());
									if (cur.moveToFirst())
									{
										sunrise = cur.getString(cur
												.getColumnIndex("sunrise"));
										sunset = cur.getString(cur
												.getColumnIndex("sunset"));
									}
									cur.close();
									cur = null;
								}
							}
							GlobalDef.MyLog.d(LOG_TAG, "sunrise=" + sunrise
									+ " sunset=" + sunset);
						}
					}
	
					info.mState = WEATHER_STATE_NORMAL;
					info.mCityName = cityName;
					info.mTemperature = temperature;
					info.mTimeZone = parseTimeZoneString(timezone);
					info.mWeather = weatherCodition;
					info.mSunRise = sunrise;
					info.mSunSet = sunset;
	
				} else
				{// no weather information
					GlobalDef.MyLog.d(LOG_TAG, "cityId =" + cityId);
					selections = "settingName = ?";
					selectionargs = new String[] { cityId };
					final Cursor cur = GlobalDef.query(mContext,
							GlobalDef.uri_setting, selections, selectionargs, null);
					if (cur != null)
					{
						if (cur.moveToFirst())
						{
							cityName = cur.getString(cur
									.getColumnIndex("settingValue"));
							GlobalDef.MyLog.d(LOG_TAG, "cityName =" + cityName);
						}
						cur.close();
					}
					info.mState = WEATHER_STATE_NO_WEATHER_INFORMATION;
					info.mCityName = cityName;
				}
			} 
			else {
				GlobalDef.MyLog.e(LOG_TAG,
						"query WeatherCondition result curson = null ,unexpected");
				return null;
			}
		}
        catch(Exception e){
        	e.printStackTrace();
        }
        finally{
	        // close Cursor
	        if( c != null )
	        c.close();
        }
		GlobalDef.MyLog.d(LOG_TAG, "mWeather = " + info.mWeather
				+ "  city name= " + info.mCityName);
		return info;
	}
	
	private WeatherInfo createWeatherInfo(int widgetId, String cityId,
			SharedPreferences sp)
	{
		WeatherInfo info = new WeatherInfo();
		
		// query database and set item
		GlobalDef.MyLog.w(LOG_TAG, "database operation");
		String selections = "cityId= ?";
		String[] selectionargs = new String[] { cityId };
		Cursor c = null;
		try {
			c = GlobalDef.query(mContext, GlobalDef.uri_condtions,
					selections, selectionargs, null);
			String cityName = null;
			if (c != null)
			{
				if (c.moveToFirst())
				{// normal
					cityName = c.getString(c.getColumnIndex("cityName"));
					String weatherType = c.getString(c
							.getColumnIndex("weatherType"));
					String condition = c.getString(c.getColumnIndex("condition"));
					String timezone = c.getString(c.getColumnIndex("timezoneAccu"));
					
					GlobalDef.MyLog.d(LOG_TAG, "weatherType=" + weatherType
							+ " condition=" + condition + " timezone=" + timezone);
					
					int temperatureType = c.getInt(c.getColumnIndex("unit"));
	//				int setting_unit = sp.getInt(GlobalDef.TEMPUNIT, 0);
					
					String units = DatabaseHelper.readSetting(mContext,
							PersistenceData.SETTING_TEMP_UNIT);
					if (units.equals("")) {
						units = "0";
					}
					int setting_unit = Integer.parseInt(units);
					
					String temperature = getTemperature(c, temperatureType,
							setting_unit);
					String weatherCodition = null;
					String sunrise = null;
					String sunset = null;
					
					GlobalDef globalDef = new GlobalDef(mContext);
					if (weatherType == null && condition == null)
					{
						weatherCodition = "sunny";
					} else
					{
						if (GlobalDef.chineseValid(condition))
						{
							if (weatherType != null && !weatherType.equals("0"))
							{
								weatherCodition = globalDef
										.getWeatherConditionWithIndex(weatherType);
							} else
							{
								GlobalDef.MyLog.d(LOG_TAG, "match by Chinese");
								weatherCodition = globalDef
										.getWeatherConditionWithIndex(condition);
							}
						} else
						{
							GlobalDef.MyLog.d(LOG_TAG, "not with Chinese");
							weatherCodition = globalDef
									.getWeatherConditionWithIndex(weatherType);
						}
	
						if (weatherCodition != null)
						{
							if (GlobalDef.EDITION_ACCU
									.equals(mWeatherServiceEdition))
							{
								selections = "cityId= ? and dayIndex = 0";
								selectionargs = new String[] { cityId };
								Cursor cur = null;
								try {
									cur = GlobalDef.query(mContext,
											GlobalDef.uri_forecast, selections,
											selectionargs, null);
									if (cur != null)
									{
										// GlobalDef.MyLog.d(LOG_TAG,"c.getCount()="+c.getCount());
										if (cur.moveToFirst())
										{
											sunrise = cur.getString(cur
													.getColumnIndex("sunrise"));
											sunset = cur.getString(cur
													.getColumnIndex("sunset"));
										}
									}
								}
						        catch(Exception e){
						        	e.printStackTrace();
						        }
						        finally{
							        // close Cursor
							        if( cur != null )
							        cur.close();
						        }
							}
							GlobalDef.MyLog.d(LOG_TAG, "sunrise=" + sunrise
									+ " sunset=" + sunset);
						}
					}
	
					info.mState = WEATHER_STATE_NORMAL;
					info.mCityName = cityName;
					info.mTemperature = temperature;
					info.mTimeZone = parseTimeZoneString(timezone);
					info.mWeather = weatherCodition;
					info.mSunRise = sunrise;
					info.mSunSet = sunset;
	
				} else
				{// no weather information
					GlobalDef.MyLog.d(LOG_TAG, "cityId =" + cityId);
					selections = "settingName = ?";
					selectionargs = new String[] { cityId };
					Cursor cur = null;
					try {
						cur = GlobalDef.query(mContext,
								GlobalDef.uri_setting, selections, selectionargs, null);
						if (cur != null)
						{
							if (cur.moveToFirst())
							{
								cityName = cur.getString(cur
										.getColumnIndex("settingValue"));
								GlobalDef.MyLog.d(LOG_TAG, "cityName =" + cityName);
							}
						}
						info.mState = WEATHER_STATE_NO_WEATHER_INFORMATION;
						info.mCityName = cityName;
					}
			        catch(Exception e){
			        	e.printStackTrace();
			        }
			        finally{
				        // close Cursor
				        if( cur != null )
				        cur.close();
			        }
				}
			} else
			{
				GlobalDef.MyLog.e(LOG_TAG,
						"query WeatherCondition result curson = null ,unexpected");
				return null;
			}
		}
        catch(Exception e){
        	e.printStackTrace();
        }
        finally{
	        // close Cursor
	        if( c != null )
	        c.close();
        }
		mWeatherMap.put(widgetId, info);
		return info;
	}
	
	private String parseTimeZoneString(String tzRawStr) {
		if (tzRawStr == null || tzRawStr.equals("")) {
			return null;
		}
		StringBuilder tzStr = new StringBuilder();
		if (tzRawStr.contains("-")) {
			tzStr.append("GMT");
		} else {
			tzStr.append("GMT+");
		}
		if (tzRawStr.contains(".")) {
			
			String tzStr1 = tzRawStr.substring(0, tzRawStr.indexOf("."));
			tzStr.append(tzStr1 + ":");
			String tzStr2 = tzRawStr.substring(tzRawStr.indexOf("."));
			int min = -1;
			try {
				min = (int) (Float.parseFloat(tzStr2) * (float) 60);
			} catch (Exception e) {
				e.printStackTrace();
				GlobalDef.MyLog.e(LOG_TAG, "exception occurs when parse timezone");
				return null;
			}
			if (min < 10) {
				tzStr.append('0');
			} 
			tzStr.append(min);
		} else {
			tzStr.append(tzRawStr);
		}

		return tzStr.toString();
	}
	
	/**
	 * Return the temperature with unit from database base on the
	 * temperatureType and setting_unit.
	 */
	private String getTemperature(Cursor c, int temperatureType, int setting_unit) {
		if (c == null) {
			return "";
		}
		if (1 == temperatureType) {// accu
			Integer tem_i = c.getInt(c.getColumnIndex("temperature"));
			if (tem_i != null) {
				GlobalDef.MyLog.d(LOG_TAG, "temperature=" + tem_i.toString() + " unit = F");
				if (setting_unit == 0) {// C
					tem_i = Math.round(5.0f * (tem_i - 32) / 9);
				}
				StringBuilder temperature = new StringBuilder().append(tem_i);
				if (setting_unit == 0) {// C
					temperature.append(GlobalDef.getTemperatureUnit(GlobalDef.UNIT_C));
				}

				else {// F
					temperature.append(GlobalDef.getTemperatureUnit(GlobalDef.UNIT_F));
				}
				return temperature.toString();

			} else {
				GlobalDef.MyLog.d(LOG_TAG, "temperature= null");
				return "";
			}
		} else {// sina (Math.round(1.0f * Integer.parseInt(c)*9 / 5 + 32));
			String reelFeel = c.getString(c.getColumnIndex("reelFeel"));
			if (reelFeel != null) {
				float tem_f = Float.parseFloat(reelFeel);
				int temp = Math.round(tem_f);
				GlobalDef.MyLog.d(LOG_TAG, "reelFeel=" + reelFeel + " temp=" + temp + " unit = C");
				if (setting_unit == 1) {// F
					temp = Math.round(temp * 9.0f / 5 + 32);
				}
				GlobalDef.MyLog.d(LOG_TAG, "setting_unit =" + setting_unit + " temp=" + temp);
				StringBuilder temperature = new StringBuilder().append(temp);
				if (setting_unit == 0) { // C
					temperature.append(GlobalDef.getTemperatureUnit(GlobalDef.UNIT_C));
				} else {// F
					temperature.append(GlobalDef.getTemperatureUnit(GlobalDef.UNIT_F));
				}
				return temperature.toString();
			} else {
				GlobalDef.MyLog.d(LOG_TAG, "reelFeel= null");
				return "";
			}
		}
	}

	public void setWeatherServiceEdition(Context context) {
		Cursor cur = null;
		try {
			cur = GlobalDef.query(context, GlobalDef.uri_appinfo, null, null, null);
			if (cur != null) {
				if (cur.moveToFirst()) {
					mWeatherServiceEdition = cur.getString(cur.getColumnIndex("appmark"));
				}
			}
		}
        catch(Exception e){
        	e.printStackTrace();
        }
        finally{
	        // close Cursor
	        if( cur != null )
	        	cur.close();
        }
	}
	
}
