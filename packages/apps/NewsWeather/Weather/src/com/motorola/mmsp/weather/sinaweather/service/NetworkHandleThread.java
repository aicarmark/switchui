package com.motorola.mmsp.weather.sinaweather.service;

import java.util.ArrayList;
import java.util.Map;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.motorola.mmsp.weather.R;
import com.motorola.mmsp.weather.sinaweather.service.WeatherUtils.UpdateResult;

class NetworkHandleThread extends Thread {
	
	private static String TAG = "WeatherService";
	private UpdateService mUpdateService = null;
	
	private Intent mIntent = null;
	private Handler mHandler = null;
	private ThreadTimeout mTimeout = null;
	private boolean mIsRunning = false;
	private String mQueryNameForTimeout = "";
	private Handler mHandlerFromService = null;
	
	
	public NetworkHandleThread(UpdateService service, Intent intent, Handler handler) {
		mUpdateService = service;
		mIntent = intent;
		mTimeout = new ThreadTimeout(this);
		Looper looper = service.getMainLooper();
		mHandler = new Handler(looper);
		mHandlerFromService = handler;
	}

	public void run() {
		Log.v(TAG, WeatherUtils.getTID() + "NetworkQueueRunable->run " + mIntent.getAction());
		mIsRunning = true;
		mHandler.removeCallbacks(mTimeout);
		try {
			if ((mIntent.getAction().equals(WeatherServiceConstant.ACTION_SEARCH_CITY_REQUEST))) {
				Log.v(TAG, WeatherUtils.getTID() + "onStart ACTION_SEARCH_CITY_REQUEST");
				handleSearchCityRequest(mIntent);
				
			} else if (WeatherServiceConstant.ACTION_CITIES_ADD_REQUEST.equals(mIntent.getAction())) {
				Log.v(TAG, WeatherUtils.getTID() + "onStart ACTION_CITIES_ADD_REQUEST");
				handleCityAddRequest(mIntent);

			} else if ((mIntent != null && WeatherServiceConstant.ACTION_WEATHER_UPDATE_REQUEST.equals(mIntent.getAction()))) {
				Log.v(TAG, WeatherUtils.getTID() + "onStart ACTION_WEATHER_UPDATING mUpdateAllIsRunning = " + UpdateService.mIsUpdateAllRunning);
				handleWeatherUpdateRequest(mIntent);

			} else if (mIntent != null && WeatherServiceConstant.ACTION_WEATHER_TICK.equals(mIntent.getAction())) {
				Log.v(TAG, WeatherUtils.getTID() + "onStart ACTION_WEATHER_TICK");
				handleWeatherTick(mIntent);

			} else if (WeatherServiceConstant.ACTION_EXTERNAL_WEATHER_QUERY.equals(mIntent.getAction())) {
				Log.v(TAG, WeatherUtils.getTID() + "Start servering external query");
				handleExternalWeatherQuery(mIntent);
			}
		} finally {
			if (mIsRunning) {
				mIsRunning = false;
				UpdateService.mNetworkReqHandleThreadCounter--;
				Log.v(TAG, WeatherUtils.getTID() + " NETWORK_REQ_HANDLE_THREDS_COUNTER-- " + UpdateService.mNetworkReqHandleThreadCounter);
				if (UpdateService.NETWORK_QUEUE.size() > 0) {
					Log.v(TAG, WeatherUtils.getTID() + " invoke dispatchNetworkRequest()");
					mUpdateService.dispatchNetworkRequest();
				}
				mHandler.removeCallbacks(mTimeout);
			}
		}
	}

	private void handleSearchCityRequest(final Intent intent) { 
		mHandlerFromService.postDelayed(mTimeout, WeatherServiceConstant.SECOND_PER_MINUTE * WeatherServiceConstant.MILLISECOND_PER_SECOND);
		String searchCity = mIntent.getStringExtra(WeatherServiceConstant.SEARCH_CITY_REQUEST_NAME);
		ArrayList<Map> items = WeatherUtils.searchCity(mUpdateService, searchCity);
		Intent returnIntent = new Intent(WeatherServiceConstant.ACTION_SEARCH_CITY_RESPONSE);
		returnIntent.putExtra(WeatherServiceConstant.SEARCH_CITY_RESPONSE_DATA, items);
		mUpdateService.sendBroadcast(returnIntent);
	}
	
	private void handleCityAddRequest(final Intent intent) { 
		mHandlerFromService.postDelayed(mTimeout, WeatherServiceConstant.SECOND_PER_MINUTE * 3 * WeatherServiceConstant.MILLISECOND_PER_SECOND);
		mUpdateService.fireCityUpdating();
		final Intent _intent = mIntent;
		String widgetId =  _intent.getExtras().getString("widgetId");
		Log.i("Weather", "NetworkHandleThread handleCityAddRequest widgetId = " + widgetId);
		
		String cityId = _intent.getExtras().getString(WeatherServiceConstant.CITY_ID);
		mUpdateService.fireCityChange("add", cityId, false, widgetId);
		UpdateResult result = WeathersManager.addWeather(mUpdateService, cityId);
		if (result == UpdateResult.FAILED){
			mUpdateService.mUpdateFailedTime = System.currentTimeMillis();
			Log.d("WeatherService", "result == UpdateResult.FAILED mUpdateFailedTime = " + mUpdateService.mUpdateFailedTime);
		}
		mUpdateService.fireCityChange("add", cityId, false, widgetId);
	}
	
	private void handleWeatherUpdateRequest(final Intent intent) {
		mHandlerFromService.postDelayed(mTimeout, WeatherServiceConstant.SECOND_PER_MINUTE * 6 * WeatherServiceConstant.MILLISECOND_PER_SECOND);
		mUpdateService.startAlarm();
		if (!UpdateService.mIsUpdateAllRunning) {
			mUpdateService.updateAll(Boolean.parseBoolean(mIntent.getStringExtra("Manual")));
		}
	}
	
	private void handleWeatherTick(final Intent mIntent) {
		mHandlerFromService.postDelayed(mTimeout, WeatherServiceConstant.SECOND_PER_MINUTE * 6 * WeatherServiceConstant.MILLISECOND_PER_SECOND);
		mUpdateService.startAlarm();
		if (!UpdateService.mIsUpdateAllRunning) {
			mUpdateService.updateAll(false);
		}
		Intent broadcast = new Intent(WeatherServiceConstant.ACTION_EXTERNAL_WEATHER_UPDATE_ALARM);
		mUpdateService.sendBroadcast(broadcast);
	}
	
	private void handleExternalWeatherQuery(final Intent intent) { 
		mHandlerFromService.postDelayed(mTimeout, WeatherServiceConstant.SECOND_PER_MINUTE * 2 * WeatherServiceConstant.MILLISECOND_PER_SECOND);
		Bundle bundle = mIntent.getExtras();
		mQueryNameForTimeout = bundle.getString("cityname").trim();
		long timediff = bundle.getLong("timedifference", 0);
		Log.v(TAG, WeatherUtils.getTID() + "queryName = " + mQueryNameForTimeout + " " + "timedifference = " + mQueryNameForTimeout);
		if (mQueryNameForTimeout != null && !mQueryNameForTimeout.equals("")) {
			String safeQueryName = mQueryNameForTimeout.replaceAll(" ", "");
			externalQueryByName(safeQueryName, timediff);
		} else {
			Log.v(TAG, WeatherUtils.getTID() + "!!!!!! invalid parameter passed from client");
		}
	}
	
	private void externalQueryByName(String queryName, long timediff) {
		Log.d(TAG, WeatherUtils.getTID() + "externalQueryByName");
		String cityId = WeatherUtils.getCityIdByQueryName(mUpdateService, queryName);
		Log.d(TAG, WeatherUtils.getTID() + "Fetch the data from internet");
		if (cityId == null || cityId.equals("")) {
			ArrayList<Map> items = WeatherUtils.externalSearchCity(mUpdateService, queryName);
			if (items == null || items.size() <= 0) {
				Log.d(TAG, WeatherUtils.getTID() + "city item is invalid, Can not find the city");
				//Broadcast, handle the situation, can not find the city from Internet
				mUpdateService.fireExternalQueryFailure(queryName, WeatherUtils.EXT_QUE_CAN_NOT_GET_CITY);
				return;
			}
			Map map = items.get(0);
			cityId = (String) map.get("location");
			Log.v(TAG, WeatherUtils.getTID() + "after lookup from Internet cityId = " + cityId);
		}
		if (cityId == null || cityId.equals("")) {
			Log.d(TAG, WeatherUtils.getTID() + "City data is invalid from Internet");
			// Broadcast, handle the situation, the city can not be located. 
			mUpdateService.fireExternalQueryFailure(queryName, WeatherUtils.EXT_QUE_CITY_IS_INVALID);
			return;
		}
		
		//Search Weather info from local DB first. If it does not exist, search from Internet
		String langId = mUpdateService.getString(R.string.langid);
		if (!WeatherUtils.isWeatherInfoValid(mUpdateService, cityId, timediff, langId)) {
			WeatherUtils.updateWeather(mUpdateService, cityId, true, queryName);
		} else {
			mUpdateService.getContentResolver().notifyChange(Uri.withAppendedPath(Uri.parse("content://" + WeatherServiceConstant.AUTHORITY + "/forecast"), queryName), null);
		    Log.d(TAG, WeatherUtils.getTID() + "notify  content://" + WeatherServiceConstant.AUTHORITY + "/forecast/" + queryName);
		}
	}
	
	private class ThreadTimeout implements Runnable {
		Thread mThr = null;
		ThreadTimeout(Thread thread) {
			mThr = thread;
		}
		public void run() {
			Log.v(TAG, WeatherUtils.getTID() + " Network handle thread timeout");
			if (mIsRunning) {
				UpdateService.mNetworkReqHandleThreadCounter--;
				Log.v(TAG, WeatherUtils.getTID() + " Reduce NETWORK_REQ_HANDLE_THREDS_COUNTER in timeout handler " 
						                                      + UpdateService.mNetworkReqHandleThreadCounter + " Interrupt related thread ~~~~~~~~~~~~~~~~~");
				mThr.interrupt();
				mIsRunning = false;
				if (UpdateService.mIsUpdateAllRunning && WeatherServiceConstant.ACTION_WEATHER_UPDATE_REQUEST.equals(mIntent.getAction())) {
					UpdateService.mIsUpdateAllRunning = false;
				}
				if ((mIntent.getAction().equals(WeatherServiceConstant.ACTION_SEARCH_CITY_REQUEST))) {
					Log.v(TAG, WeatherUtils.getTID() + "ACTION_SEARCH_CITY_REQUEST timeout");
					Intent returnIntent = new Intent(WeatherServiceConstant.ACTION_SEARCH_CITY_RESPONSE);
					mUpdateService.sendBroadcast(returnIntent);
				} else if (WeatherServiceConstant.ACTION_CITIES_ADD_REQUEST.equals(mIntent.getAction())) {
					Log.v(TAG, WeatherUtils.getTID() + "ACTION_CITIES_ADD_REQUEST timeout");
					String cityId = mIntent.getExtras().getString(WeatherServiceConstant.CITY_ID);
					mUpdateService.fireCityChange("add", cityId, false);
				} else if (WeatherServiceConstant.ACTION_WEATHER_UPDATE_REQUEST.equals(mIntent.getAction())) {
					Log.v(TAG, WeatherUtils.getTID() + "ACTION_WEATHER_UPDATING timeout");
                                        mUpdateService.fireWeatherChagne(WeathersManager.UPDATE_RESULT_ALL_FAILED);

				} else if (WeatherServiceConstant.ACTION_WEATHER_TICK.equals(mIntent.getAction())) {
					Log.v(TAG, WeatherUtils.getTID() + "ACTION_WEATHER_TICK timeout");
					Intent broadcast = new Intent(WeatherServiceConstant.ACTION_EXTERNAL_WEATHER_UPDATE_ALARM);
					mUpdateService.sendBroadcast(broadcast);
				} else if (WeatherServiceConstant.ACTION_EXTERNAL_WEATHER_QUERY.equals(mIntent.getAction())) {
					Log.v(TAG, WeatherUtils.getTID() + "External query timeout");
					mUpdateService.fireExternalQueryFailure(mQueryNameForTimeout, WeatherUtils.EXT_QUE_NETWORK_UNAVAILABLE);
				}
				if (UpdateService.NETWORK_QUEUE.size() > 0) {
					Log.v(TAG, WeatherUtils.getTID() + " invoke dispatchNetworkRequest()in timeout handler");
					mUpdateService.dispatchNetworkRequest();
				}
			}
		}
	}
}

