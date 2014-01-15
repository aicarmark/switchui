package com.motorola.mmsp.weather.sinaweather.service;

import java.io.IOException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.motorola.mmsp.weather.R;
import com.motorola.mmsp.weather.sinaweather.service.WeatherInfo.Forecast;

public class DatabaseHelper {
	public static final Uri condtions = Uri.parse("content://" + WeatherServiceConstant.AUTHORITY + "/conditions");
	public static final Uri forecast = Uri.parse("content://" + WeatherServiceConstant.AUTHORITY + "/forecast");
	public static final Uri city = Uri.parse("content://" + WeatherServiceConstant.AUTHORITY + "/city");
	public static final Uri setting = Uri.parse("content://" + WeatherServiceConstant.AUTHORITY + "/setting");
	public static final Uri citylist = Uri.parse("content://" + WeatherServiceConstant.AUTHORITY + "/citylist");
	public static final Uri appinfo = Uri.parse("content://" + WeatherServiceConstant.AUTHORITY + "/appinfo");
	public static final Uri widgetcity = Uri.parse("content://" + WeatherServiceConstant.AUTHORITY + "/widget_city");
	
	public synchronized static void deleteWeatherByCityId(Context context,String cityId) {
		if (cityId == null || cityId.equals("")) {
			return;
		}
		ContentResolver cr =  context.getContentResolver();
		
		try{
			cr.delete(condtions, WeatherDatabaseHelper.CITY_ID + " = ?", new String[]{cityId});
			cr.delete(forecast, WeatherDatabaseHelper.CITY_ID + " = ?", new String[]{cityId});
			cr.delete(city, WeatherDatabaseHelper.CITY_ID + " = ?", new String[]{cityId});
			cr.delete(setting, WeatherDatabaseHelper.SETTINTNAME + " = ?", new String[]{cityId});
		}catch(Exception e){
			e.printStackTrace();
		}

	}
	
	public synchronized static void deleteSettingBySettingName(Context context, String settingName) {
		ContentResolver cr =  context.getContentResolver();
		try{
			cr.delete(setting, WeatherDatabaseHelper.SETTINTNAME + " = ?", new String[]{settingName});
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public synchronized static void writeSetting(Context context, String settingName, String settingValue) {
		
		Log.d("WeatherService", WeatherUtils.getTID() + "writeSetting begin");
		ContentResolver cr = context.getContentResolver();
		ContentValues values = new ContentValues();
		values.put(WeatherDatabaseHelper.SETTINTNAME, settingName);
		values.put(WeatherDatabaseHelper.SETTINGVALUE, settingValue);
		Cursor c = null;
        try {
            c = cr.query(setting, null, WeatherDatabaseHelper.SETTINTNAME + " = ?", new String[] { settingName }, null);
            if (c == null || c.getCount() <= 0) {
                Log.d("WeatherService", WeatherUtils.getTID() + " setting name = " + settingName + " setting Value = " + settingValue);
                cr.insert(setting, values);
            } else if (c != null && c.getCount() > 0) {
                Log.d("WeatherService", WeatherUtils.getTID() + "update existed setting value");
                cr.update(DatabaseHelper.setting, values, WeatherDatabaseHelper.SETTINTNAME + "=?", new String[] { settingName });
            } else {
                Log.d("WeatherService", WeatherUtils.getTID() + "unknown situation.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
		    if (c != null) {
			    c.close();
		    }
        }
		Log.d("WeatherService", WeatherUtils.getTID() + "writeSetting end");
	}
	
	public synchronized static String readSetting(Context context, String settingName) {
		
		Log.d("WeatherService", WeatherUtils.getTID() + "readSetting begin");
		String settingValue = "";
		ContentResolver cr = context.getContentResolver();
		Cursor c = null;
		try {
		    c = cr.query(setting, null, WeatherDatabaseHelper.SETTINTNAME + " = ?", new String[]{settingName}, null);
		    if (c != null && c.getCount() > 0) {
			    c.moveToFirst();
			    settingValue = c.getString(c.getColumnIndex(WeatherDatabaseHelper.SETTINGVALUE));
		    }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
		Log.d("WeatherService", WeatherUtils.getTID() + "readSetting end");
		return settingValue;
	}
	
	public synchronized static void writeWeather(Context context, WeatherInfo weather, String cityId) throws IOException {
		
		Log.d("WeatherService", WeatherUtils.getTID() + "DatabaseHelper writeWeather 3 parameters");
		DatabaseHelper.writeWeather(context, weather, cityId, false);
	}
	
	public synchronized static void writeWeather(Context context, WeatherInfo weather, String cityId, boolean isExternal) throws IOException {
		Log.d("WeatherService", WeatherUtils.getTID() + "DatabaseHelper writeWeather 4 parameters");
		if (weather == null) {
			Log.d("WeatherService", WeatherUtils.getTID() + "weather is null, so return directly");
			return;
		}
		String langId = context.getString(R.string.langid);
		int extFlag = isExternal ? 1 : 0;
		
		ContentResolver cr =  context.getContentResolver();
		ContentValues conditionValues = new ContentValues();
		conditionValues.put(WeatherDatabaseHelper.CITY_ID, cityId);
		conditionValues.put(WeatherDatabaseHelper.CITY_NAME, weather.infomation.city);		
		conditionValues.put(WeatherDatabaseHelper.ADMINAREA, weather.infomation.adminArea);
		conditionValues.put(WeatherDatabaseHelper.DATE, weather.infomation.date);
		conditionValues.put(WeatherDatabaseHelper.TIME, weather.infomation.time);
		conditionValues.put(WeatherDatabaseHelper.UPDATE_TIME, weather.infomation.requestTime);
		conditionValues.put(WeatherDatabaseHelper.ISDAY, weather.infomation.dayNight == WeatherInfo.DAY);
		conditionValues.put(WeatherDatabaseHelper.TEMPERATURE, weather.current.temperatrue);
		conditionValues.put(WeatherDatabaseHelper.UNIT, weather.infomation.unit);
		conditionValues.put(WeatherDatabaseHelper.WEATHER_TYPE, weather.current.weatherType);
		conditionValues.put(WeatherDatabaseHelper.CONDITION, weather.current.condition);
		conditionValues.put(WeatherDatabaseHelper.URL, weather.infomation.url);
		conditionValues.put(WeatherDatabaseHelper.REALFEEL, weather.current.realFeel);
		conditionValues.put(WeatherDatabaseHelper.LANGUAGE , langId);
		conditionValues.put(WeatherDatabaseHelper.TIMEZONEACCU, weather.current.timezone);		
		
		Cursor c = null; 
                try {
                    c = cr.query(condtions, new String[] {WeatherDatabaseHelper.CITY_ID, WeatherDatabaseHelper.ISEXTERNAL },
                                 WeatherDatabaseHelper.CITY_ID + "=?", new String[] { cityId }, null);
                    if (c == null || c.getCount() <= 0) {
                    conditionValues.put(WeatherDatabaseHelper.ISEXTERNAL, extFlag);
                    cr.insert(condtions, conditionValues);
                } else {
                    // Make sure external query does not impact internal data
                    // If the weather info has existed and is internal, do not update isExternal field
                    c.moveToFirst();
                    if (0 == Integer.parseInt(c.getString(1))) {
                        extFlag = 0;
                    }
                    conditionValues.put(WeatherDatabaseHelper.ISEXTERNAL, extFlag);
                    cr.update(condtions, conditionValues, "cityId = ?", new String[] { cityId });
                }

	        cr.delete(forecast, WeatherDatabaseHelper.CITY_ID + " = ?", new String[] {cityId});
		for (int i = 0; i < weather.forecasts.size(); i ++) {
			ContentValues forecastvalues = new ContentValues();
			Forecast day = weather.forecasts.get(i);
			forecastvalues.put(WeatherDatabaseHelper.CITY_ID, cityId);
			forecastvalues.put(WeatherDatabaseHelper.DAY_INDEX, i);
			forecastvalues.put(WeatherDatabaseHelper.DAY, day.day);
			forecastvalues.put(WeatherDatabaseHelper.LOW, day.low);			
			forecastvalues.put(WeatherDatabaseHelper.HIGH, day.high);
			forecastvalues.put(WeatherDatabaseHelper.CONDITION, day.condition);
			forecastvalues.put(WeatherDatabaseHelper.UNIT, weather.infomation.unit);
			forecastvalues.put(WeatherDatabaseHelper.WEATHER_TYPE, day.weatherType);
			forecastvalues.put(WeatherDatabaseHelper.DATE, day.date);
			forecastvalues.put(WeatherDatabaseHelper.WINDPOWER, day.windPower);
			forecastvalues.put(WeatherDatabaseHelper.SUNRISE, day.sunrise);
			forecastvalues.put(WeatherDatabaseHelper.SUNSET, day.sunset);
			forecastvalues.put(WeatherDatabaseHelper.LANGUAGE , langId);
			forecastvalues.put(WeatherDatabaseHelper.ISEXTERNAL, extFlag);
			cr.insert(forecast, forecastvalues);
		}	
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (c != null) {
                    c.close();
                }
            }	
	}
	
	public synchronized static WeatherInfo readWeather(Context context,String cityId) throws IOException, ClassNotFoundException {
		
		Log.d("WeatherService", WeatherUtils.getTID() + "DatabaseHelper readWeather");
		ContentResolver cr =  context.getContentResolver();
		WeatherInfo weather = null;
		Cursor c = null;
        try {
            c = cr.query(condtions, null, WeatherDatabaseHelper.CITY_ID + " = ?", new String[] { cityId }, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                weather = new WeatherInfo();
                weather.infomation.cityId = c.getString(0);
                weather.infomation.requestCity = weather.infomation.cityId;
                weather.infomation.city = c.getString(1);
                weather.infomation.adminArea = c.getString(2);
                weather.infomation.date = c.getString(3);
                weather.infomation.time = c.getString(4);
                weather.infomation.requestTime = c.getString(5);
                weather.infomation.dayNight = c.getInt(6) != 0 ? WeatherInfo.DAY : WeatherInfo.NIGHT;
                weather.current.temperatrue = c.getString(7);
                weather.infomation.unit = c.getString(8);
                weather.current.weatherType = c.getInt(9);
                weather.current.condition = c.getString(10);
                weather.infomation.url = c.getString(11);
                weather.current.realFeel = c.getString(12);
                weather.current.timezone = c.getString(13);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }

        try {
            c = cr.query(forecast, null, WeatherDatabaseHelper.CITY_ID + " = ?", new String[] { cityId }, null);
            if (c != null && c.getCount() > 0 && weather != null) {
                for (int i = 0; i < c.getCount(); i++) {
                    c.moveToNext();
                    Forecast day = weather.newForecast();
                    // cityid
                    // dayIndex
                    day.day = c.getString(2);
                    day.low = c.getString(3);
                    day.high = c.getString(4);
                    day.condition = c.getString(5);
                    // unit
                    day.weatherType = c.getInt(7);
                    day.date = c.getString(8);
                    if (i >= weather.forecasts.size()) {
                        weather.forecasts.add(i, day);
                    } else {
                        weather.forecasts.set(i, day);
                    }
                }
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
	
	public synchronized static void writeCityQueryName(Context context, String queryName, String cityId, String updateTime) {
		
		Log.d("WeatherService", WeatherUtils.getTID() + "writeCityQueryName begin");
		ContentResolver cr = context.getContentResolver();
		ContentValues values = new ContentValues();
	    values.put(WeatherDatabaseHelper.QUERYNAME, queryName);
	    values.put(WeatherDatabaseHelper.CITY_ID, cityId);
	    values.put(WeatherDatabaseHelper.UPDATE_TIME, updateTime);
		
	    Cursor c = null;
	    try {
	        c = cr.query(city, null, WeatherDatabaseHelper.QUERYNAME + "=?", new String[]{queryName}, null);
	        //queryName does not exist
	        if (c == null || c.getCount() <= 0) {
	            Log.d("WeatherService", WeatherUtils.getTID() + " writeCityQueryName->insert queryName = " + queryName + 
					                                        " cityId = " + cityId + " updateTime = " + updateTime);
	            cr.insert(city, values);
	        } else if (c != null && c.getCount() > 0) {
	            c.moveToFirst();
	            //queryName exists, but with different cityId
	            if (!(c.getString(c.getColumnIndex(WeatherDatabaseHelper.CITY_ID)).equals(cityId))){
	                 Log.d("WeatherService", WeatherUtils.getTID() + " writeCityQueryName->update update city table queryName = " + queryName + 
					                                            " cityId = " + cityId + " updateTime = " + updateTime);
	                 cr.update(city, values, WeatherDatabaseHelper.QUERYNAME + "=?", new String[]{queryName});
			
	                 //QueryName exists and with the same cityId
	            } else {
				    Log.d("WeatherService", WeatherUtils.getTID() + "writeCityQueryName->queryName already exists in the local DB.");
			    }
		    } 
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
		Log.d("WeatherService", WeatherUtils.getTID() + "writeCityQueryName end");
	}
	
	public synchronized static String getCityIdByQueryName(Context context, String queryName) {
	    Log.d("WeatherService", WeatherUtils.getTID() + "getCityIdByQueryName begin");
		String cityId = "";
		ContentResolver cr = context.getContentResolver();
		Cursor c = null; 
		try {
		    c = cr.query(city, null, WeatherDatabaseHelper.QUERYNAME + " = ?", new String[]{queryName}, null);
		    if (c != null && c.getCount() > 0) {
		        c.moveToFirst();
		        long sysTime = System.currentTimeMillis();
		        long offset = WeatherServiceConstant.MILLISECOND_PER_HOUR * 24 * WeatherDatabaseHelper.EXTERNAL_CITY_NAME_EXPIRED_PERIOD;
		        long expiredPeriod = sysTime - offset;
		        Log.d("WeatherService", WeatherUtils.getTID() + "expiredPeriod = " + expiredPeriod + " sysTime = " + sysTime + " offset = " + offset);
		        long updateTime = Long.valueOf(c.getString(c.getColumnIndex(WeatherDatabaseHelper.UPDATE_TIME)));
		        if (updateTime > expiredPeriod) {
		            cityId = c.getString(c.getColumnIndex(WeatherDatabaseHelper.CITY_ID));
		        } else {
		            Log.d("WeatherService", WeatherUtils.getTID() + "queryName and cityId pair has been expired!!!");
		        }
		    }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
		Log.d("WeatherService", WeatherUtils.getTID() + "getCityIdByQueryName end, cityId = " + cityId);
		return cityId;
	}
	
	public synchronized static String getCityIdByLatlon(Context context, String strLatlon) {
		String cityId = "";
		ContentResolver cr = context.getContentResolver();
		Cursor c = null;
		try {
		    c = cr.query(city, null, WeatherDatabaseHelper.QUERYNAME + " = ? " , new String[]{strLatlon}, null);
		    if (c != null && c.getCount() > 0) {
			    c.moveToFirst();
			    long sysTime = System.currentTimeMillis();
			    long offset = WeatherServiceConstant.MILLISECOND_PER_HOUR*24*WeatherDatabaseHelper.EXTERNAL_CITY_NAME_EXPIRED_PERIOD;
			    long expiredPeriod = sysTime - offset;
			    Log.d("WeatherService", WeatherUtils.getTID() + "expiredPeriod = " + expiredPeriod + " sysTime = " + sysTime + " offset = " + offset);
			    long updateTime = Long.valueOf(c.getString(c.getColumnIndex(WeatherDatabaseHelper.UPDATE_TIME)));
			    if (updateTime > expiredPeriod) {
			        cityId = c.getString(c.getColumnIndex(WeatherDatabaseHelper.CITY_ID));
			    }
		    }        
		} catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }

		return cityId;
	}
	
	public synchronized static boolean isWeatherInfoValid(Context context, String cityId, long timediff, String langId) {

		boolean isValid = false;
		if (cityId == null || cityId.length() == 0 || langId == null || timediff == 0) {
			Log.d("WeatherService", WeatherUtils.getTID() + "isWeatherInfoValid, cityId or langId is null or timediff is 0, return directly.");
			return isValid;
		}
		long time = 0;
		if (timediff == 0) {
			String settingValue = "";
			ContentResolver cr = context.getContentResolver();
			Cursor c = null;
			try {
				c = cr.query(setting, null, WeatherDatabaseHelper.SETTINTNAME + " = ?", new String[]{WeatherServiceConstant.SETTING_UPDATE_FREQUENCY}, null);
				if (c != null && c.getCount() > 0) {
					c.moveToFirst();
					settingValue = c.getString(c.getColumnIndex(WeatherDatabaseHelper.SETTINGVALUE));
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
			
			long hour = 0;
			if (settingValue != null && !settingValue.equals("")) {
				hour = Long.parseLong(settingValue);	
			}
			time = System.currentTimeMillis() - hour * WeatherServiceConstant.MILLISECOND_PER_HOUR;
		} else {
			time = System.currentTimeMillis() - timediff;
		}
		ContentResolver cr = context.getContentResolver();
		String where = WeatherDatabaseHelper.CITY_ID + " = ? and " 
				        + WeatherDatabaseHelper.LANGUAGE + " = ? and " 
				        + WeatherDatabaseHelper.UPDATE_TIME + " > ?";
		String[] argument = new String[]{cityId, langId, String.valueOf(time)};
		Cursor c = null;
		try {
		    c = cr.query(condtions, null, where, argument, null);
		    Log.d("WeatherService", WeatherUtils.getTID() + "cityId= " + cityId + " langId= " + langId + " time= " + time);
		    if (c != null && c.getCount() > 0) {
		        isValid = true;	
		    }
		} catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        if (c != null) {
                c.close();
	        }
	    }
		Log.d("WeatherService", WeatherUtils.getTID() + "isWeatherInfoValid: " + isValid );
		return isValid;
	}
	
	/*
	public static void setCityIds(Context context, ArrayList<String> cityIds) {
		Log.v("WeatherService", "TID " + String.valueOf(Thread.currentThread().getId()) + "  " + "setCityIds begin");
		ContentResolver cr =  context.getContentResolver();
		ContentValues values = new ContentValues();
		int count = cityIds != null ? cityIds.size() : 0;
		for (int i = 0; i < count; i++) {
			values.put(String.valueOf(i), cityIds.get(i));
		}
		cr.insert(cities_private, values);
		Log.v("WeatherService", "TID " + String.valueOf(Thread.currentThread().getId()) + "  " + "setCityIds end");
	}
	
	public static ArrayList<String> getCityIds(Context context) {
		Log.v("WeatherService", "TID " + String.valueOf(Thread.currentThread().getId()) + "  " + "getCityIds begin");
		ArrayList<String> cityIds = new ArrayList<String>();
		ContentResolver cr =  context.getContentResolver();
		Cursor c = cr.query(cities, new String[]{WeatherDatabaseHelper.CITY_ID}, null, null, null);
		if (c != null && c.getCount() > 0) {
			while (c.moveToNext()){
				cityIds.add(c.getString(0));
			}
		}
		if (c != null) {
			c.close();
		}
		Log.v("WeatherService", "TID " + String.valueOf(Thread.currentThread().getId()) + "  " + "getCityIds end");
		return cityIds;
	}
	*/
}
