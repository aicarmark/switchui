package com.motorola.mmsp.weather.locationwidget.small;

import android.app.Application;
import android.content.Intent;
import android.content.res.Configuration;

public class WidgetApplication extends Application {
	private static final String LOG_TAG = GlobalDef.LOG_TAG;

	@Override
	public void onCreate() {
		super.onCreate();
		GlobalDef.MyLog.d(LOG_TAG, "----application onCreate");
		initWidgetParamters();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		GlobalDef.MyLog.d(LOG_TAG, "----application onTerminate");
	}

	public void initWidgetParamters() {
		startWeatherServie();
//		getRssSetting();
		GlobalDef.init(this);
		WeatherNewsProvider.init(this);
	}

	private void startWeatherServie() {
		GlobalDef.MyLog.w(LOG_TAG, "startWeatherServie " + GlobalDef.SERVICE_INTENT_WEATHER);
		Intent weatherIntent = new Intent(GlobalDef.SERVICE_INTENT_WEATHER);
		startService(weatherIntent);
	}

	private void getRssSetting() {
		GlobalDef.MyLog.w(LOG_TAG, "sendBroadcast " + GlobalDef.BROADCAST_GETRSS_SETTING);
		Intent intent = new Intent(GlobalDef.BROADCAST_GETRSS_SETTING);
		sendBroadcast(intent);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Intent intent = new Intent(GlobalDef.BROADCAST_CITYLIST_UPDITING);
		sendBroadcast(intent);
		super.onConfigurationChanged(newConfig);
	}
}