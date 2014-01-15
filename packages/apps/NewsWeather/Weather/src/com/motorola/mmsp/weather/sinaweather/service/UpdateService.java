package com.motorola.mmsp.weather.sinaweather.service;

import java.util.ArrayList;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class UpdateService extends Service{	

	private static String TAG = "WeatherService";   
    public long mUpdateFailedTime = 0;
    private Handler mServiceHandler = null;	
    
    public static boolean mIsUpdateAllRunning = false; 
    private static boolean mIsLocationWidgetEnabled = true;
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (intent != null && ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
				boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
				Log.v("WeatherService", WeatherUtils.getTID() + "ConnectivityManager.CONNECTIVITY_ACTION connect = " + !noConnectivity);
				if (!noConnectivity) {
					String value = WeatherUtils.getSettingByName(UpdateService.this, WeatherServiceConstant.SETTING_UPDATE_FREQUENCY);
	            			long hour = 0;
	            			if (value != null && !value.equals("")) {
	            			hour = Long.valueOf(value);
	            		}
	            		if (hour != 0 && mUpdateFailedTime != 0 
	            			&& mUpdateFailedTime < System.currentTimeMillis() 
	            			&& mUpdateFailedTime + hour * WeatherServiceConstant.MILLISECOND_PER_HOUR > System.currentTimeMillis()) {
	            			Log.v("WeatherService", WeatherUtils.getTID() + "RE Update" + new Date(mUpdateFailedTime).toLocaleString());
						new Thread(new Runnable() {
						    public void run() {
						    	updateAll(false);
						    }
						}).start();
					}	            	
	            		}			
/*
			} else if (intent != null && Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction())) {
				Log.v(TAG, WeatherUtils.getTID() + "ACTION_LOCALE_CHANGED, update all weather information");
				new Thread(new Runnable() {
				    public void run() {
				    	updateAll(false);
				    }
				}).start();
*/
			} else if (intent != null && intent.getAction().equals(WeatherServiceConstant.ACTION_LOCATION_WIDGET_ENABLE)) {
				Log.v(TAG, WeatherUtils.getTID() + "set mIsLocationWidgetEnabled as true");
				mIsLocationWidgetEnabled = true;
			} else if (intent != null && intent.getAction().equals(WeatherServiceConstant.ACTION_LOCATION_WIDGET_DISABLE)) {
				Log.v(TAG, WeatherUtils.getTID() + "set mIsLocationWidgetEnabled as false");
				mIsLocationWidgetEnabled = false;
			} else if(intent.getAction().equals(Intent.ACTION_PACKAGE_DATA_CLEARED))
			{
				if(intent.getDataString().contains("com.motorola.mmsp.motohomex"))
				{
//					getContentResolver().delete(WeatherWidgetContentProvider.WidgetCity.CONTENT_URI, null, null);
					getContentResolver().delete(DatabaseHelper.widgetcity, null, null);
				}
			}
			
		}
	};
	
    private Runnable mFirstUpdateAllRunnable = new Runnable () {
		public void run() {
			Log.v(TAG, WeatherUtils.getTID() + "mFirstUpdateAllRunnable run starts");		
			int updateFreq = 0;
			String updateFreqStr = WeatherUtils.getSettingByName(UpdateService.this, WeatherServiceConstant.SETTING_UPDATE_FREQUENCY);
			if (updateFreqStr != null && !updateFreqStr.equals("")) {
			    updateFreq = Integer.parseInt(updateFreqStr);
			}
			Log.v(TAG, WeatherUtils.getTID() + "updateFreq = " + updateFreq + ", if updateFreq !=0, it will update all");
			if (updateFreq != 0) {
			    updateAll(false);
			    if (mUpdateFailedTime != 0) {
			    	Log.v(TAG, WeatherUtils.getTID() + "First update all failed, try again !!!!!! mUpdateFailedTime = " + mUpdateFailedTime);
			    	mServiceHandler.postDelayed(mUpdateDelayRunnable, 50 * 1000);
			    }
			}
			Log.v(TAG, WeatherUtils.getTID() + "mFirstUpdateAllRunnable run exits");
		}  	
    };
	
    private Runnable mUpdateDelayRunnable = new Runnable () {
		public void run() {
			updateAfterPowerOn(mUpdateAllDoubleCheckRunnable);
		}  	
    };
    
    private Runnable mUpdateAllDoubleCheckRunnable = new Runnable () {
		public void run() {
			if (mUpdateFailedTime != 0) { 
				Log.v(TAG, WeatherUtils.getTID() + "update all double check ~~~");
			    updateAll(false);
			}
		}  	
    };
    
	private Runnable mUpdateTimeOut = new Runnable() {
		public void run() {
			Log.d("WeatherService", WeatherUtils.getTID() + "Updated time out");
			mIsUpdateAllRunning = false;
			fireWeatherChagne(WeathersManager.UPDATE_RESULT_ALL_FAILED);
		}
	};
/*
	ServiceUpdateFreqObserver updateFreqObs = new ServiceUpdateFreqObserver(this, new Handler());
	ServiceAutoCityObserver autoCityObs = new ServiceAutoCityObserver(this, new Handler());
	ServiceTemperatureUnitUpdateObserver  temperatureUnitObs = new ServiceTemperatureUnitUpdateObserver(this, new Handler());
*/

	public static ArrayList<Intent> ALL_REQUEST_QUEUE = new ArrayList<Intent>(); 
	public static ArrayList<Intent> NETWORK_QUEUE = new ArrayList<Intent>();
	
	private final static int NETWORK_REQ_HANDLE_THREDS_NUM_MAX = 3;
	public static int mNetworkReqHandleThreadCounter = 0;
	
	//@Override
	public void onCreate() {
		Log.v("WeatherService", WeatherUtils.getTID() + "on Create start service begin - 2011-12-06");
		super.onCreate();
		mServiceHandler = new Handler();		
		startAlarm();
		IntentFilter filter = new IntentFilter();
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		//filter.addAction(Intent.ACTION_LOCALE_CHANGED);
		filter.addAction(WeatherServiceConstant.ACTION_LOCATION_WIDGET_ENABLE);
		filter.addAction(WeatherServiceConstant.ACTION_LOCATION_WIDGET_DISABLE);
		registerReceiver(mReceiver, filter);
		
		IntentFilter packageDataCleared = new IntentFilter();
		packageDataCleared.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
		packageDataCleared.addDataScheme("package");
		registerReceiver(mReceiver, packageDataCleared);
		
		updateAfterPowerOn(mFirstUpdateAllRunnable);
			
//		String updateFreqStr = WeatherUtils.getSettingByName(UpdateService.this, WeatherServiceConstant.SETTING_UPDATE_FREQUENCY);
//		int updateFreq = 0;
//		if (updateFreqStr != null && !updateFreqStr.equals("")) {
//		    updateFreq = Integer.parseInt(updateFreqStr);
//		}
//		if (updateFreq != 0) {
//			updateAll(false);
//			if (mUpdateFailedTime != 0) {
//			    myHandler.postDelayed( mUpdateAllDoubleCheckRunnable, 50 * 1000);
//			}
//	    }
		Log.v("WeatherService", WeatherUtils.getTID() + "on Create start service end");
	}
	
	public void updateAfterPowerOn(Runnable runnable) {
		Log.v(TAG, WeatherUtils.getTID() + "updateAfterPowerOn, create and start an independent thread to update");
		Thread thr = new Thread(runnable);
		thr.start();
	}
	
	// @Override
	public void onStart(final Intent intent, int startId) {
		super.onStart(intent, startId);
		if (intent == null || intent.getAction() == null) {
			Log.v("WeatherService", WeatherUtils.getTID() + "onStart intent action is invalid");
		} else {
			Log.v("WeatherService", WeatherUtils.getTID() + "onStart add intent into ALL_REQUEST_QUEUE");
			activeAllRquestHandleThread(intent);
		}
		
		return;
	}
	
	//@Override
	public void onDestroy() {
		super.onDestroy();	
		unregisterReceiver(mReceiver);
		
		Intent operator = new Intent(WeatherServiceConstant.ACTION_WEATHER_TICK);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, operator, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(pendingIntent);
	}
	
	private void activeAllRquestHandleThread(final Intent intent) {
		appendAllRequestQueue(intent);
		startRequestAllThread();
	}
	
	private synchronized void startRequestAllThread() {
		if (!AllRequestHandleThread.isAllRequestThreadRun()) {
			Log.v("WeatherService", WeatherUtils.getTID() + "start handleAllRequestQueue ");
			AllRequestHandleThread.setAllRequestThreadRunning(true);
			new AllRequestHandleThread(this).start(); 
		} else {
			Log.v("WeatherService", WeatherUtils.getTID() + "handleAllRequestQueue has been running already.");
		}
	}

	private synchronized void appendAllRequestQueue(Intent intent) {
		ALL_REQUEST_QUEUE.add(intent);
	}
	
	public synchronized void removeAllRequestQueueHead() {
		ALL_REQUEST_QUEUE.remove(0);
	}
	private synchronized void appendNetworkQueue(Intent intent) {
		NETWORK_QUEUE.add(intent);
	}
	
	private synchronized void removeNetworktQueueHead() {
		NETWORK_QUEUE.remove(0);
	}
	
	public void activeNetworkHandleThread(final Intent intent) {
		Log.d(TAG, WeatherUtils.getTID() + "activeNetworkHandleThread, move intent to NetworkQueue");
		appendNetworkQueue(intent);
		dispatchNetworkRequest();
	}
	
	public synchronized void dispatchNetworkRequest() {
		Log.v("WeatherService", WeatherUtils.getTID() + " dispatchNetworkRequest, NETWORK_QUEUE.size() = " + NETWORK_QUEUE.size());
		if (NETWORK_QUEUE.size() > 0) {
			int availableThrNum = NETWORK_REQ_HANDLE_THREDS_NUM_MAX - mNetworkReqHandleThreadCounter;
			Log.v("WeatherService", WeatherUtils.getTID() + " availableThrNum = " + availableThrNum);
			for (int i=0; i<availableThrNum; i++) {
				Intent intent = NETWORK_QUEUE.get(0);
				mNetworkReqHandleThreadCounter++;
				Thread thr = new Thread(new NetworkHandleThread(this, intent, mServiceHandler));
				thr.start();
				removeNetworktQueueHead();
				if (NETWORK_QUEUE.size() == 0) {
					break;
				}
			}
		}
	}

//	private synchronized void startRequestNetworkThread() {
//		if (!isHandleNetworkRunning) {
//			Log.v("WeatherService", WeatherUtils.getTID() + "start startRequestNetworkThread ");
//			setNetworkThreadRunning(true);
//			new Thread(handleNetworkQueue).start(); 
//		} else {
//			Log.v("WeatherService", WeatherUtils.getTID() + "handleNetworkThread has been running already.");
//		}
//	}
	
	//@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public void fireWeatherChagne(int result){
		Log.d("WeatherService", WeatherUtils.getTID() + "fireWeatherChagne ");
		Intent broadcast = new Intent(WeatherServiceConstant.ACTION_WEATHER_UPDATED_RESPONSE);
		broadcast.putExtra("result", result);
		Log.d("WeatherService", WeatherUtils.getTID() + "broadcast com.motorola.mmsp.weather.updated.response");
		sendBroadcast(broadcast);
	}
	
	public void fireCityUpdating() {
		Log.d("WeatherService", WeatherUtils.getTID() + "fireCityUpdate() ");
		Intent broadcast = new Intent(WeatherServiceConstant.ACTION_CITIES_UPDATING);
		sendBroadcast(broadcast);
	}
	
	public void fireCityChange(String type, String id, boolean isTimeout) {
		Log.d("WeatherService", WeatherUtils.getTID() + "fireCityChange() ");
		Intent broadcast = new Intent(WeatherServiceConstant.ACTION_CITIES_UPDATED_RESPONSE);
		broadcast.putExtra("type", type);
		broadcast.putExtra("id", id);
		broadcast.putExtra("isTimeout", isTimeout);
		Log.d("WeatherService", WeatherUtils.getTID() +  "broadcast com.motorola.mmsp.weather.cities.updated.response");
		sendBroadcast(broadcast);
	}
	
	public void fireCityChange(String type, String id, boolean isTimeout, String widgetId) {
		Log.d("WeatherService", WeatherUtils.getTID() + "fireCityChange() ");
		Intent broadcast = new Intent(WeatherServiceConstant.ACTION_CITIES_UPDATED_RESPONSE);
		broadcast.putExtra("type", type);
		broadcast.putExtra("id", id);
		broadcast.putExtra("isTimeout", isTimeout);
		broadcast.putExtra("widgetId", widgetId);
		
		Log.d(TAG, WeatherUtils.getTID() +  "broadcast com.motorola.mmsp.weather.cities.updated.response");
		sendBroadcast(broadcast);
	}

	public void fireExternalQueryFailure(String queryName, int errNo) {
		Log.d("WeatherService", WeatherUtils.getTID() + "broadcast ACTION_EXTERNAL_WEATHER_QUERY_FAILURE " 
				                                      + "queryName = " + queryName 
				                                      + "errNo = " + errNo);
		Intent broadcast = new Intent(WeatherServiceConstant.ACTION_EXTERNAL_WEATHER_QUERY_FAILURE);
		broadcast.putExtra("queryName", queryName);
		broadcast.putExtra("errNo", errNo);
		sendBroadcast(broadcast);
	}
	
	public void startAlarm() {		
        Log.d("WeatherService", WeatherUtils.getTID() + "startAlarm ");
		Intent operator = new Intent(WeatherServiceConstant.ACTION_WEATHER_TICK);
		operator.setClass(this, UpdateService.class);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0, operator, PendingIntent.FLAG_UPDATE_CURRENT);
		Date d = new Date();
		long hour = 0; 
		String updateFreqStr = WeatherUtils.getSettingByName(UpdateService.this, WeatherServiceConstant.SETTING_UPDATE_FREQUENCY);
		if (updateFreqStr != null && !updateFreqStr.equals("")) {
			hour = Long.parseLong(updateFreqStr);
		}
        d.setTime(System.currentTimeMillis() + hour * AlarmManager.INTERVAL_HOUR);
        long updateTimes = d.getTime();
        
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		if (hour != 0) {
			Log.v("WeatherService", WeatherUtils.getTID() + "UpdateServie next update time = " + d.toLocaleString());
			alarm.set(AlarmManager.RTC, updateTimes, pendingIntent);
		} else {
			alarm.cancel(pendingIntent);
		}
	}
	
	public void updateAll(final boolean manual) {
		Log.d("WeatherService", WeatherUtils.getTID() + "updateAll begin");
		if (!mIsUpdateAllRunning && (mIsLocationWidgetEnabled || manual)) {
			mServiceHandler.removeCallbacks(mUpdateTimeOut);
			mServiceHandler.postDelayed(mUpdateTimeOut, (WeathersManager.getCityIds(this).size() + 1) * 
					(WeatherServiceConstant.SECOND_PER_MINUTE / 2) * WeatherServiceConstant.MILLISECOND_PER_SECOND);
			Log.v("WeatherService", WeatherUtils.getTID() + "updateAll is running");
			mIsUpdateAllRunning = true;
			final long currTime = System.currentTimeMillis();
//			new Thread(new Runnable() {
//				public void run() {
//					Log.v("WeatherService", WeatherUtils.getTID() + "updateAll begin");
					int result = WeathersManager.updateAll(UpdateService.this);
					fireWeatherChagne(result);
					if (result == WeathersManager.UPDATE_RESULT_ALL_FAILED || result == WeathersManager.UPDATE_RESULT_PART_FAILED) {
						mUpdateFailedTime = currTime;
					} else {
						mUpdateFailedTime = 0;
					}
					mIsUpdateAllRunning = false;
					mServiceHandler.removeCallbacks(mUpdateTimeOut);
					Log.v("WeatherService", WeatherUtils.getTID() + "updateAll end, result = " + result);
//				}
//			}).start();	
		} else {
			Log.v(TAG, WeatherUtils.getTID() + "updateAll end, mIsUpdateAllRunning = " + mIsUpdateAllRunning 
                    + " mIsLocationWidgetEnabled = " + mIsLocationWidgetEnabled + " manual = " + manual);
		}
	}
}

/*
class ServiceUpdateFreqObserver extends ContentObserver {

	private Context context = null;
	public static String mUpdateFreq = null;
	
	public ServiceUpdateFreqObserver(Context context, Handler handler) {
		super(handler);
		this.context = context;
	}
	
	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		Log.d("WeatherService", WeatherUtils.getTID() + "WeatherServiceUpdateFreqObserver onChange");
		ContentResolver cr = context.getContentResolver();
		Cursor c = cr.query(DatabaseHelper.setting, null, "settingName" + "=?", new String[]{WeatherServiceConstant.SETTING_UPDATE_FREQUENCY}, null);
		if (c != null && c.getCount() > 0) {
			c.moveToFirst();
			mUpdateFreq = c.getString(1);
			Log.d("WeatherService", WeatherUtils.getTID() + "service update new update frequency = " + mUpdateFreq);
		}

		if (c != null) {
			c.close();
		}
	}
}

class ServiceAutoCityObserver extends ContentObserver {

	private Context context = null;
	public static String mIsAutoCity = null;
	
	public ServiceAutoCityObserver(Context context, Handler handler) {
		super(handler);
		this.context = context;
	}
	
	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		Log.d("WeatherService", WeatherUtils.getTID() + "WeatherServiceAutoCityObserver onChange");
		ContentResolver cr = context.getContentResolver();
		Cursor c = cr.query(DatabaseHelper.setting, null, null, null, null);
		if (c != null && c.getCount() > 0) {
			c.moveToFirst();
			mIsAutoCity = c.getString(1);
			Log.d("WeatherService", WeatherUtils.getTID() + "service update autocity = " + mIsAutoCity);
		}

		if (c != null) {
			c.close();
		}
	}
}

class ServiceTemperatureUnitUpdateObserver extends ContentObserver {

	private Context context = null;
	public static String mUpdateUi = null;

	public ServiceTemperatureUnitUpdateObserver(Context context, Handler handler) {
		super(handler);
		this.context = context;
	}

	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		Log.d("WeatherService", "ServiceTemperatureUnitUpdate onChange");
		ContentResolver cr = context.getContentResolver();
		Cursor c = cr.query(Uri.withAppendedPath(DatabaseHelper.setting, WeatherServiceConstant.SETTING_TEMP_UNIT), null, null, null, null);
		if (c != null && c.getCount() > 0) {
			c.moveToFirst();
			mUpdateUi = c.getString(1);
			Log.d("WeatherService", "service update new update ServiceTemperatureUnitUpdate = " + mUpdateUi);
		}

		if (c != null) {
			c.close();
		}
	}
}
*/
