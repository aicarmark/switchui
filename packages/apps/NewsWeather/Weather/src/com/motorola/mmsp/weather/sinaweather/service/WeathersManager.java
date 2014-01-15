package com.motorola.mmsp.weather.sinaweather.service;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

import com.motorola.mmsp.weather.sinaweather.service.WeatherUtils.UpdateResult;

public class WeathersManager {

	public static final String AUTO_CITY = "auto_city";
	private static boolean isEnable = true;
	
	public static int UPDATE_RESULT_SUCCESS = 1;
	public static int UPDATE_RESULT_PART_FAILED = 2;
	public static int UPDATE_RESULT_ALL_FAILED = 3;
	
	public static void setEnable(boolean bEnable) {
		isEnable = bEnable;
	}
	
	public static UpdateResult addWeather(Context context, String cityId) {
		return updateWeather(context, cityId);
	}
	
	public static void deleteWeather(Context context, String cityId) {
		DatabaseHelper.deleteWeatherByCityId(context, cityId);
	}
	
	public static UpdateResult updateWeather(Context context,String cityId) {
		WeatherInfo weather = null;
		UpdateResult result = UpdateResult.SUCCESS;
		weather = WeatherUtils.updateWeather(context,cityId);
		if (weather == null || weather.infomation.city == null) {
			result = UpdateResult.FAILED;
			weather = WeatherUtils.getWeather(context, cityId);
		}
		if (weather == null) {
			weather = new WeatherInfo();
		}
		weather.infomation.requestCity = cityId;
		return result;
	}
	
	public static int updateAll(Context context) {
		if (!isEnable) {
			return UPDATE_RESULT_ALL_FAILED;
		}
		
		int failCount = 0;
		int sucCount = 0;
		int allResult;
		ArrayList<String> cityIds = getCityIds(context);
		if (cityIds != null && !cityIds.isEmpty()) {
			for (String cityId : cityIds) {
				UpdateResult result = updateWeather(context, cityId);
				Log.d("WeatherService", WeatherUtils.getTID() + 
						"update " + cityId + " 's weather " + "result = " + (result == UpdateResult.SUCCESS? "success" : "fail") );
				if (result != UpdateResult.SUCCESS) {
					failCount ++;
				} else {
					sucCount ++;
				}
			}
		}
		
		if (sucCount > 0) {
			if (failCount > 0) {
				allResult = UPDATE_RESULT_PART_FAILED;
			} else {
				allResult = UPDATE_RESULT_SUCCESS;
			}
		} else {
			if (failCount > 0) {
				allResult = UPDATE_RESULT_ALL_FAILED;
			} else {
				allResult = UPDATE_RESULT_SUCCESS;
			}
		}
		
		return allResult;
	}

	public static ArrayList<String> getCityIds(Context context) {
		String s = WeatherUtils.getSettingByName(context, WeatherServiceConstant.SETTING_CITIYIDS);
		return getStringList(s);
	}
	
	private static ArrayList<String> getStringList(String s) {
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
}
