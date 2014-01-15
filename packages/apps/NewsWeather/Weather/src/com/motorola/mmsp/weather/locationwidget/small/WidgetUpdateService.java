package com.motorola.mmsp.weather.locationwidget.small;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

// this class 
public class WidgetUpdateService extends Service {
	private static final String LOG_TAG = GlobalDef.LOG_TAG;
	ServiceTemperatureUnitUpdateObserver mTemperatureUnitObs = null;
	private TimeReceiver mTimeReceiver;
	private PowerManager mPowerManager;
	private boolean mTimeChanged = false;

	@Override
	public void onCreate() {
		GlobalDef.MyLog.d(LOG_TAG, "WidgetUpdateService onCreate");
		registerUpdateReceiver();
		mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		if (mTemperatureUnitObs == null) {
			mTemperatureUnitObs = new ServiceTemperatureUnitUpdateObserver(this);
			getContentResolver().registerContentObserver(Uri.parse("content://" + GlobalDef.AUTHORITY + "/setting/temperature_uint"),
					false, mTemperatureUnitObs);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		GlobalDef.MyLog.d(LOG_TAG, "WidgetUpdateService onStartCommand");
		return START_STICKY;
	}

	public void onDestroy() {
		GlobalDef.MyLog.d(LOG_TAG, "WidgetUpdateService onDestroy");
		unRegisterUpdateReceiver();
		if (mTemperatureUnitObs != null) {
			getContentResolver().unregisterContentObserver(mTemperatureUnitObs);
			mTemperatureUnitObs = null;
		}
	}

	private void registerUpdateReceiver() {
		GlobalDef.MyLog.d(LOG_TAG, "WidgetUpdateService register ACTION_TIME_TICK receiver");
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_TIME_TICK);
		filter.addAction(Intent.ACTION_TIME_CHANGED);
		filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		filter.addAction(Intent.ACTION_DATE_CHANGED);

		filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
		filter.addAction(Intent.ACTION_LOCALE_CHANGED);

		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

		filter.addAction(GlobalDef.BROADCAST_HOME_BACKTOHS);
		filter.addAction(GlobalDef.BROADCAST_HOME_SWITCH);
		IntentFilter filter2 = new IntentFilter();
		filter2.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
		filter2.addDataScheme("package");
		if (mTimeReceiver == null) {
			mTimeReceiver = new TimeReceiver();
			registerReceiver(mTimeReceiver, filter);
			registerReceiver(mTimeReceiver, filter2);
		}
	}

	private void unRegisterUpdateReceiver() {
		GlobalDef.MyLog.d(LOG_TAG, "WidgetUpdateService unregister ACTION_TIME_TICK receiver");
		if (mTimeReceiver != null) {
			unregisterReceiver(mTimeReceiver);
			mTimeReceiver = null;
		}
	}

	private class TimeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if (action == null) {
				return;
			}
			if (action.equals(Intent.ACTION_TIME_TICK) || action.equals(Intent.ACTION_TIME_CHANGED)
					|| action.equals(Intent.ACTION_TIMEZONE_CHANGED) || action.equals(Intent.ACTION_DATE_CHANGED)) {
				GlobalDef.MyLog.i(LOG_TAG, "----WidgetUpdateService " + action);
				if (mPowerManager.isScreenOn()) {
					updateAllWidget(context);
				} else {
					mTimeChanged = true;
					GlobalDef.MyLog.i(LOG_TAG, "WidgetUpdateService pm.isScreenOn() = false");
				}
			} else if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
				GlobalDef.MyLog.i(LOG_TAG, "----WidgetUpdateService " + action);
				WeatherNewsProvider.onConfigurationChange(context, false);
			} else if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
				GlobalDef.MyLog.i(LOG_TAG, "----WidgetUpdateService " + action);
				WeatherNewsProvider.onConfigurationChange(context, true);
			} else if (action.equals(Intent.ACTION_SCREEN_ON)) {
				GlobalDef.MyLog.i(LOG_TAG, "----WidgetUpdateService " + action);
				if (mTimeChanged) {
					updateAllWidget(context);
					mTimeChanged = false;
				}
			} else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
				GlobalDef.MyLog.i(LOG_TAG, "----WidgetUpdateService " + action);
			} else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				GlobalDef.MyLog.i(LOG_TAG, "----WidgetUpdateService " + action);
				WeatherNewsProvider.onNetworkChange(context);
			} else if (action.equals(GlobalDef.BROADCAST_HOME_BACKTOHS)
					|| action.equals(GlobalDef.BROADCAST_HOME_SWITCH)) {
				GlobalDef.MyLog.i(LOG_TAG, "----WidgetUpdateService " + action);
				WeatherNewsProvider.onHomeResume(context);
			} else if (action.equals(Intent.ACTION_PACKAGE_DATA_CLEARED) && 
					(intent.getDataString().contains("com.motorola.mmsp.accuweather.service") || 
					 intent.getDataString().contains("com.motorola.mmsp.sinaweather.service"))) {
				GlobalDef.MyLog.i(LOG_TAG, "----WidgetUpdateService " + action);
				WeatherNewsProvider.onDataClear(context);
			}
		}
	}

	private void updateAllWidget(Context context) {
		WeatherNewsProvider.updateAllWidgets(context);
	}

	class ServiceTemperatureUnitUpdateObserver extends ContentObserver {

		private Context mContext = null;

		public ServiceTemperatureUnitUpdateObserver(Context context) {
			super(new Handler());
			mContext = context;
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			GlobalDef.MyLog.i(LOG_TAG, "----ServiceTemperatureUnitUpdateObserver onChange,selfChange:" + selfChange);
			WeatherNewsProvider.onTempUnitChanged(mContext);
		}
	}
}
