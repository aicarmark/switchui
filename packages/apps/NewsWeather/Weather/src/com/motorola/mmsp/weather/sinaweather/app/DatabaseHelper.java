package com.motorola.mmsp.weather.sinaweather.app;

import java.io.IOException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.motorola.mmsp.weather.sinaweather.app.WeatherInfo.Forecast;

public class DatabaseHelper {
	public static final Uri condtions = Uri.parse("content://" + Constant.AUTHORITY + "/conditions");
	public static final Uri forecast = Uri.parse("content://" + Constant.AUTHORITY + "/forecast");
	public static final Uri city = Uri.parse("content://" + Constant.AUTHORITY + "/city");
	public static final Uri setting = Uri.parse("content://" + Constant.AUTHORITY + "/setting");
	public static final Uri citylist = Uri.parse("content://" + Constant.AUTHORITY + "/citylist");
	public static final Uri appinfo = Uri.parse("content://" + Constant.AUTHORITY + "/appinfo");
	
//	public synchronized static void deleteFile(Context context, String cityId) {
//		if (cityId == null || cityId.equals("")) {
//			return;
//		}
//		ContentResolver cr =  context.getContentResolver();
//		cr.delete(condtions, WeatherDatabaseDefine.CITY_ID + " = ?", new String[]{cityId});
//		cr.delete(forecast, WeatherDatabaseDefine.CITY_ID + " = ?", new String[]{cityId});
//		cr.delete(city, WeatherDatabaseDefine.CITY_ID + " = ?", new String[]{cityId});
//	}
	
	public synchronized static void deleteSetting(Context context, String settingName) {
		ContentResolver cr =  context.getContentResolver();
		cr.delete(setting, WeatherDatabaseDefine.SETTINTNAME + " = " + settingName, null);
	}
	
	public synchronized static void writeSetting(Context context, String settingName, String settingValue) {
		Log.d("WeatherWidget", "writeSetting begin, settingName = " + settingName + " settingValue = " + settingValue);
		ContentResolver cr = context.getContentResolver();
		ContentValues values = new ContentValues();
		values.put(WeatherDatabaseDefine.SETTINTNAME, settingName);
		values.put(WeatherDatabaseDefine.SETTINGVALUE, settingValue);
		Cursor c = null;
		try {
		    c = cr.query(setting, null, WeatherDatabaseDefine.SETTINTNAME + " = ?", new String[]{settingName}, null);
		    if (c == null || c.getCount() <= 0) {
		        Log.d("WeatherWidget", " setting name = " + settingName + " setting Value = " + settingValue);
		        cr.insert(setting, values);
		    } else if (c != null && c.getCount() > 0) {
		        Log.d("WeatherWidget", "update existed setting value");
		        cr.update(setting, values, WeatherDatabaseDefine.SETTINTNAME + "=?", new String[]{settingName});
		    } else {
		        Log.d("WeatherWidget", "unknown situation.");
		    }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
		Log.d("WeatherWidget",  "writeSetting end");

	}
	
	public synchronized static String readSetting(Context context, String settingName) {
		Log.d("WeatherWidget", "readSetting begin");
		String settingValue = "";
		ContentResolver cr = context.getContentResolver();
		Cursor c = null;
		try {
		    c = cr.query(setting, null, WeatherDatabaseDefine.SETTINTNAME + " = ?", new String[]{settingName}, null);
		    if (c != null && c.getCount() > 0) {
		        c.moveToFirst();
		        settingValue = c.getString(c.getColumnIndex(WeatherDatabaseDefine.SETTINGVALUE));
		        Log.d("WeatherWidget", "settingValue = " + settingValue);
		    } else {
		        Log.d("WeatherWidget", "can not get value by " + settingName);
		    }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
		Log.d("WeatherWidget", "readSetting end");
		return settingValue;
	}
	
	public synchronized static boolean isSettingIntialized(Context context) {
		Log.d("WeatherWidget", "isSettingIntialized begin");
		boolean isInited = false;
		ContentResolver cr = context.getContentResolver();
		Cursor c = null;
		try {
		    c = cr.query(setting, null, WeatherDatabaseDefine.SETTINTNAME + " = ?", new String[]{PersistenceData.SETTING_TEMP_UNIT}, null);
		    if (c != null && c.getCount() > 0) {
		        isInited = true;
		    }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
		Log.d("WeatherWidget", "isSettingIntialized end");
		return isInited;
	}

	public static WeatherInfo readWeather(Context context,String cityId) throws IOException, ClassNotFoundException {
		Log.d("WeatherWidget", "DatabaseHelper readWeather");
		ContentResolver cr =  context.getContentResolver();
		WeatherInfo weather = null;
		Cursor c = null;
        try {
            c = cr.query(condtions, null, WeatherDatabaseDefine.CITY_ID + " = ?", new String[] { cityId }, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                weather = new WeatherInfo();
                weather.mInfomation.mCityId = c.getString(0);
                weather.mInfomation.mRequestCity = weather.mInfomation.mCityId;
                weather.mInfomation.mCity = c.getString(1);
                weather.mInfomation.mAdminArea = c.getString(2);
                weather.mInfomation.mDate = c.getString(3);
                weather.mInfomation.mTime = c.getString(4);
                weather.mInfomation.mRequestTime = c.getString(5);
                weather.mInfomation.mDayNight = c.getInt(6) != 0 ? WeatherInfo.DAY : WeatherInfo.NIGHT;
                weather.mCurrent.mTemperatrue = c.getString(7);
                weather.mInfomation.mUnit = c.getString(8);
                weather.mCurrent.mWeatherType = c.getInt(9);
                weather.mCurrent.mCondition = c.getString(10);
                weather.mInfomation.mUrl = c.getString(11);
                weather.mCurrent.mRealFeel = c.getString(12);
                weather.mCurrent.mTimezone = c.getString(16);
            } else {
                Log.d("WeatherWidget", "Can not get Weather info from condition table");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
        try {
            c = cr.query(forecast, null, WeatherDatabaseDefine.CITY_ID + " = ?", new String[] { cityId }, null);
            if (c != null && c.getCount() > 0 && weather != null) {
                for (int i = 0; i < c.getCount(); i++) {
                    c.moveToNext();
                    Forecast day = weather.newForecast();
                    // cityid
                    // dayIndex
                    day.mDay = c.getString(2);
                    day.mLow = c.getString(3);
                    day.mHigh = c.getString(4);
                    day.mCondition = c.getString(5);
                    // unit
                    day.mWeatherType = c.getInt(7);
                    day.mDate = c.getString(8);
                    day.mSunRise = c.getString(10);
                    day.mSunSet = c.getString(11);

                    if (i >= weather.mForecasts.size()) {
                        weather.mForecasts.add(i, day);
                    } else {
                        weather.mForecasts.set(i, day);
                    }
                }
            } else {
                Log.d("WeatherWidget", "Can not get Weather info from forecast table");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
		
		return weather;
	}
}

