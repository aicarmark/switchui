package com.motorola.mmsp.weather.sinaweather.service;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.motorola.mmsp.weather.R;

class AllRequestHandleThread extends Thread {
	
	private static String TAG = "WeatherService";
	private static boolean mIsAllRequestQueueThreadRunning = false;
	private UpdateService mUpdateService = null;
	
	AllRequestHandleThread(UpdateService service) {
		mUpdateService = service;
	}
	
	public void run() {
		
		while (true) {
			Log.d(TAG, WeatherUtils.getTID() + "start handleAllRequestQueue loop");
			
			if (UpdateService.ALL_REQUEST_QUEUE.size() > 0) {
				Log.d(TAG, WeatherUtils.getTID() + "ALL_REQUEST_QUEUE has intent to handle");
				final Intent intent = UpdateService.ALL_REQUEST_QUEUE.get(0);
				if (intent != null && (intent.getAction().equals(WeatherServiceConstant.ACTION_SEARCH_CITY_REQUEST) || 
						WeatherServiceConstant.ACTION_CITIES_ADD_REQUEST.equals(intent.getAction()) ||   
						WeatherServiceConstant.ACTION_WEATHER_TICK.equals(intent.getAction())|| 
						WeatherServiceConstant.ACTION_EXTERNAL_WEATHER_QUERY.equals(intent.getAction()))) {
					
					mUpdateService.activeNetworkHandleThread(intent);
					
				} else if (WeatherServiceConstant.ACTION_WEATHER_UPDATE_REQUEST.equals(intent.getAction())) {
					Log.d(TAG, WeatherUtils.getTID() + "Start servering ACTION_WEATHER_UPDATE_REQUEST");
					handleWeatherUpdateRequest(intent);

				} else if (WeatherServiceConstant.ACTION_EXTERNAL_WEATHER_QUERY.equals(intent.getAction())){
					Log.v(TAG, WeatherUtils.getTID() + "Start servering external query");
					handleExternalWeatherQuery(intent);

				}  else if (WeatherServiceConstant.ACTION_CITIES_DELETE_REQUEST.equals(intent.getAction())) {
					Log.v(TAG, WeatherUtils.getTID() + "onStart ACTION_CITIES_DELETE_REQUEST");
					handleCityDeleteRequest(intent);

				} else if (WeatherServiceConstant.ACTION_CITIES_REARRANGED.equals(intent.getAction())) {
					Log.v(TAG, WeatherUtils.getTID() + "onStart ACTION_CITIES_REARRANGED");
					handleCityRearrange(intent);

				}  else if (WeatherServiceConstant.ACTION_ENALBE_SERVER_UPDATE.equals(intent.getAction())) {
					Log.v(TAG, WeatherUtils.getTID() + "onStart ACTION_ENALBE_SERVER_UPDATE");
					handleEnableServerUpdate(intent);

				} else if (WeatherServiceConstant.ACTION_WEATHER_REQUEST.equals(intent.getAction())) {
					Log.v(TAG, WeatherUtils.getTID() + "onStart ACTION_WEATHER_REQUEST");
					handleWeatherRequest(intent);

				} else if (intent != null && WeatherServiceConstant.ACTION_UPDATE_FREQUENCY.equals(intent.getAction())) {
					Log.v(TAG, WeatherUtils.getTID() + "onStart ACTION_UPDATE_FREQUENCY");
					handleUpdateFrequency(intent);
				} 
				
				mUpdateService.removeAllRequestQueueHead();
			
			} else {
				Log.d(TAG, WeatherUtils.getTID() + "ALL_REQUEST_QUEUE is empty");
				break;
			}
		}
		setAllRequestThreadRunning(false);
		Log.d(TAG, WeatherUtils.getTID() + "ALL_REQUEST_QUEUE handling thread stop");
	}
	
	public synchronized static void setAllRequestThreadRunning(boolean isRunning) {
		mIsAllRequestQueueThreadRunning = isRunning;
	}
	
	public synchronized static boolean isAllRequestThreadRun() {
		return mIsAllRequestQueueThreadRunning;
	}
	
	private void handleWeatherUpdateRequest(final Intent intent) {
		if (!UpdateService.mIsUpdateAllRunning) {
			Log.d(TAG, WeatherUtils.getTID() + "there is no thread making update, so move intent to NetworkQueue");
			mUpdateService.activeNetworkHandleThread(intent);
		} else {
			Log.d(TAG, WeatherUtils.getTID() + "update is ongoing, so ignore this request");
		}
	}
	
	private void handleExternalWeatherQuery(final Intent intent) {
		Bundle bundle = intent.getExtras();
		String queryName = bundle.getString("cityname");
		long timediff = bundle.getLong("timedifference", 0);
		Log.v(TAG, WeatherUtils.getTID() + "queryName = " + queryName + " " + "timedifference = " + timediff);
		if (queryName != null && !queryName.equals("")) {
			Log.d(TAG, WeatherUtils.getTID() + "query by queryName" + queryName);
			if (isWeatherInfoValidByName(queryName, timediff)) {
				Log.d(TAG, WeatherUtils.getTID() + "isWeatherInfoValidByName = true");
				mUpdateService.getContentResolver().notifyChange(Uri.withAppendedPath(Uri.parse("content://" + WeatherServiceConstant.AUTHORITY + "/forecast/"), queryName), null);
				Log.d(TAG, WeatherUtils.getTID() + "notify content://" + WeatherServiceConstant.AUTHORITY + "/forecast/" + queryName);
			} else {
				Log.d(TAG, WeatherUtils.getTID() + "isWeatherInfoValidByName = false, add into Network thread");
				mUpdateService.activeNetworkHandleThread(intent);
			}
		} 
	}
	
	private void handleCityDeleteRequest(final Intent intent) {
		//mUpdateService.fireCityUpdating();
		String cityId = intent.getExtras().getString(WeatherServiceConstant.CITY_ID);
		WeathersManager.deleteWeather(mUpdateService, cityId);
		mUpdateService.fireCityChange("delete", cityId, false);
	}
	
	private void handleCityRearrange(final Intent intent) {
		//TODO handle the request
		mUpdateService.fireCityChange("rearrange", "", false);
	}
	
	private void handleEnableServerUpdate(final Intent intent) {
		boolean bEnable = intent.getBooleanExtra(WeatherServiceConstant.ENABLE_SERVER, false);
		if (bEnable && !UpdateService.mIsUpdateAllRunning) {
			mUpdateService.fireWeatherChagne(WeathersManager.UPDATE_RESULT_SUCCESS);
		} else if (!bEnable) {
			WeathersManager.setEnable(false);
		}
	}
	
	private void handleWeatherRequest(final Intent intent) {
		if (!UpdateService.mIsUpdateAllRunning) {
			mUpdateService.fireWeatherChagne(WeathersManager.UPDATE_RESULT_SUCCESS);
		}
	}
	
	private void handleUpdateFrequency(final Intent intent) {
		mUpdateService.startAlarm();
	}
	
    private boolean isWeatherInfoValidByName(String queryName, long timediff) {
    	boolean isValid = false;
		Log.d(TAG, WeatherUtils.getTID() + "isWeatherInfoValidByName");
		String cityId = WeatherUtils.getCityIdByQueryName(mUpdateService, queryName);
		String langId = mUpdateService.getString(R.string.langid);
		isValid = WeatherUtils.isWeatherInfoValid(mUpdateService, cityId, timediff, langId);
    	return isValid;
    }
}