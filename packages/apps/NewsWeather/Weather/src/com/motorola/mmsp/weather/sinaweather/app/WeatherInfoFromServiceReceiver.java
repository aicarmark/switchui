package com.motorola.mmsp.weather.sinaweather.app;

import com.motorola.mmsp.weather.sinaweather.service.UpdateService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WeatherInfoFromServiceReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(final Context context, final Intent intent) {
		if (intent == null || intent.getAction() == null) {
			return;
		}

		String action = intent.getAction();
		Log.d("WeatherWidget", "Receiver.onReceive()" + action + " data=" + intent.getDataString());
		if (action.equals(Constant.ACTION_WEATHER_UPDATED_RESPONSE)) {
			MoreWeatherActivity.mIsWeatherUpdating = false;
			new Thread(new Runnable() {				
				//@Override
				public void run() {
					WeathersManager.getInstance(context).updateWeatherAndCityIdsCacheFromDB(context);
					Intent newIntent = new Intent(WeatherWidget.ACTION_WEATHER_UPDATED);
					newIntent.putExtras(intent);
					context.sendBroadcast(newIntent);
				}
			}).start();
			
		} else if (action.equals(Constant.ACTION_CITIES_UPDATED_RESPONSE)) {
			String type = intent.getStringExtra("type");
			String id = intent.getStringExtra("id");
			if (type != null && type.equals("add")) {
				WeathersManager.getInstance(context).addWeatherToCache(context, id);
			} else if (type != null && type.equals("delete")) {
				WeathersManager.getInstance(context).deleteWeatherFromCache(context, id);
			} else {
				Log.d("WeatherWidget", "city rearrange or other unknown condition");
				WeathersManager.getInstance(context).updateCityIdsAndWeatherCache(context);
			}
			
			Intent newIntent = new Intent(WeatherWidget.ACTION_CITIES_UPDATED);
			newIntent.putExtras(intent);
			Log.d("WeatherWidget", "broadcast ACTION_CITIES_UPDATED");
			context.sendBroadcast(newIntent);
		} else if (action.equals(WeatherWidget.ACTION_WEATHER_UPDATED)||action.equals(WeatherWidget.ACTION_CITIES_UPDATED)){
			MoreWeatherActivity.mIsWeatherUpdating = false;
		} else if (action.equals(Intent.ACTION_PACKAGE_DATA_CLEARED) && 
				   intent.getDataString().contains("com.motorola.mmsp.sinaweather.service")) {
			Log.d("WeatherWidget", "weather service cleared data, so app need to clear cache");
			WeathersManager wMgr = WeathersManager.getInstance(context);
			if (wMgr != null) {
				wMgr.clearCache();
			}
			if (CitiesActivity.mCityIds != null && CitiesActivity.mCityIds.size() > 0) {
				CitiesActivity.mCityIds.clear();
			}
		} 
		else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
			Log.d("WeatherWidget", "WeatherWidget BOOT_COMPLETED");
			Intent bootIntent = new Intent(context, UpdateService.class);
            context.startService(bootIntent);
		}
	}

}
