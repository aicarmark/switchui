package com.motorola.mmsp.weather.sinaweather.app;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;

import com.motorola.mmsp.weather.R;
import com.motorola.mmsp.weather.sinaweather.app.WeatherInfo.Forecast;

public class WeatherUtils {	
	public enum UpdateResult{
		SUCCESS,
		FAILED,
	};
	public enum AnimaitonArea {
		WIDGET,
		SCREEN,
	};	
		
	public static int getWeatherDrawable(Context context,WeatherInfo weatherInfo) {
		int icon = 0;
		if (weatherInfo != null) {
			Integer iconI = XmlValuesMap.getValue(context, R.anim.sina_images, weatherInfo.mCurrent.mCondition, "drawable");
			if (iconI != null) {
				icon = iconI.intValue();
			}
		}		
		return icon;
	}	

	public static CharSequence getUpdateInterval(String secondTime, String format2) {
		
		CharSequence time="0";
		try {
		
			SimpleDateFormat mDf2 = new SimpleDateFormat(format2);
			Date phoneTime = mDf2.parse(secondTime);
			
			
			time = DateUtils.getRelativeTimeSpanString(phoneTime.getTime(),
					System.currentTimeMillis(),
					DateUtils.MINUTE_IN_MILLIS,
					DateUtils.FORMAT_ABBREV_RELATIVE);

			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return  time;
	}
	public static String getTime(String mFormat) {
		// Calendar mCal = Calendar.getInstance();
		// mWeekOfYear = mCal.get(Calendar.WEEK_OF_YEAR);
		Date date = new Date();
		SimpleDateFormat myFormatter = new SimpleDateFormat(mFormat);
		return myFormatter.format(date);
	}

	public static boolean isDayOrNight(String mFirstTime, int mHour, int mMinute, String mFormat2) {
		try {
			String firstTime = mFirstTime.trim();
			long hour;
			long minute;
			if (firstTime.contains("PM")) {
				firstTime = firstTime.replace("PM", "");

				String time[] = firstTime.split(":");
				hour = Integer.valueOf(time[0]) + 12;

				if (time[1].contains("0") && time[1].indexOf("0") == 0) {
					String a =time[1].replace("0", "").trim();
					if("".equals(a)){
						minute=0;
					}else{
					minute = Integer.valueOf(a.trim());
					}
				} else {
					minute = Integer.valueOf(time[1].trim());
				}

			} else {
				firstTime = firstTime.replace("AM", "");
				String time[] = firstTime.split(":");
				hour = Integer.valueOf(time[0]);

				if (time[1].contains("0") && time[1].indexOf("0") == 0) {
					String a =time[1].replace("0", "").trim();
					if("".equals(a)){
						minute=0;
					}else{
					minute = Integer.valueOf(a.trim());
					}
				} else {
					minute = Integer.valueOf(time[1].trim());
				}
			}

			if ( hour - mHour < 0||hour - mHour == 0 && minute - mMinute <= 0 ) {
				return true;
			} else {
				return false;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	//@Override
	public static int getWeatherDrawable(Context context, Forecast forecast) {
		int icon = 0;
		if (forecast != null) {
			Integer iconI = XmlValuesMap.getValue(context, R.anim.sina_images, forecast.mCondition, "drawable");
			if (iconI != null) {
				icon = iconI.intValue();
			}
		}		
		return icon;
	}
	
	public static int getBgDrawable(Context context,String condition, int dayNight, WeatherUtils.AnimaitonArea area) {
		int resid = -1;
		if (condition != null) {
            if (area == WeatherUtils.AnimaitonArea.SCREEN) {
				if (dayNight == WeatherInfo.DAY) {
					resid = XmlValuesMap.getValue(context, R.anim.sina_images, condition, "layout_marginTop");
				} else if (dayNight == WeatherInfo.NIGHT) {
					resid = XmlValuesMap.getValue(context, R.anim.sina_images, condition, "layout_marginLeft");
				}
			}						
		}
		return resid;
	}

	//@Override
	public static WeatherInfo getWeatherFromLocalDB(Context context, String cityId) {
		Log.d("WeatherWidget", "WeatherUtil->getWeatehr from local db begin, cityId=" + cityId);
		WeatherInfo weather = null;
		try {
			weather = DatabaseHelper.readWeather(context, cityId);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		if (weather != null) {
			Log.d("WeatherWidget", "WeatherUtil->getWeatehr from local db end, temperature = " 
					+ weather.mCurrent.mTemperatrue + " condition = "
					+ weather.mCurrent.mCondition + "city = " 
					+ weather.mInfomation.mCity + " adminArea = " 
					+ weather.mInfomation.mAdminArea + " cityId " 
					+ weather.mInfomation.mCityId);
		} else {
			Log.d("WeatherWidget", "WeatherUtil->getWeatehr from local db end, weather == null");
		}
		
		return weather;
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


class XmlValuesMap {
	public static int getValue(Context context,int fileId, String nodeName, String attrName) {
		int icon = 0;
		XmlPullParser parser = context.getResources().getXml(fileId);
		try {
			while(parser.next() != XmlPullParser.END_DOCUMENT) {
				String name = parser.getName();
				if (name != null && name.equals(nodeName)) {
					int count = parser.getAttributeCount();
					for (int i=0; i < count; i++) {
						String attr_Name = parser.getAttributeName(i);
						if (attr_Name != null && attr_Name.equals(attrName)) {
						
							if(parser.getAttributeValue(i).contains("@")){
							icon = Integer.parseInt(parser.getAttributeValue(i).replace("@", ""));
							}else{
								icon= Float.valueOf(parser.getAttributeValue(i).replace("dip", "")).intValue();
							}
							break;
						}
					}
				}
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return icon;
	}	
}

