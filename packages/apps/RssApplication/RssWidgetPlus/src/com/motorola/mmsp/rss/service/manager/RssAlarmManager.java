package com.motorola.mmsp.rss.service.manager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.SimpleFormatter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.motorola.mmsp.rss.common.RssConstant;

public class RssAlarmManager {
	private static final String TAG = "RssAlarmManager";
	private static final String KEY_WIDGET_ID = "widgetId";
	private static final String KEY_AUTO_UPDATE = "autoUpdate";
	private static final String KEY_REQUEST_CODE = "request_code";
	private static final String KEY_SET_ALARM_DATE = "current_date";
	private static final String ALARM_PREFERENCE = "AlarmPreference";
	private static final long DAY_TO_MILLISECONDS = 3600 * 24 * 1000;
	private static RssAlarmManager sInstance;
	private Context mContext;
	private AlarmManager mAlarmManager;
	private SharedPreferences mAlarmPreference;
//	private HashMap<Integer, PendingIntent> mWidgetPendingMap = new HashMap<Integer, PendingIntent>();
	
	public static RssAlarmManager getInstance(Context context) {
		if(sInstance == null) {
			sInstance = new RssAlarmManager(context);
		}
		return sInstance;
	}
	
	private RssAlarmManager(Context context) {
		mContext = context;
		mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		mAlarmPreference = context.getSharedPreferences(ALARM_PREFERENCE, Context.MODE_PRIVATE);
		sInstance = this;
	}
	
	public void addAlarmforNewsCheck() {
		long currentDateTime = 0;
		String currentDate = null;
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			currentDateTime = formatter.parse(formatter.format(date)).getTime();
			currentDate = formatter.format(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Log.d(TAG, "current Date is " + formatter.format(new Date()));
		long theFirstDateForChecking = currentDateTime + DAY_TO_MILLISECONDS;
		formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
		Log.d(TAG, "theDateForChecking is " + formatter.format(new Date(theFirstDateForChecking)));
		Intent intent = new Intent(RssConstant.Intent.INTENT_RSSSERVICE_VALIDITY_CHECKING);
		int requestCode = (int)System.currentTimeMillis();
		PendingIntent pending = PendingIntent.getService(mContext, requestCode, intent, 0);
		Log.d(TAG, "start an alarm to check news validity");
		mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, theFirstDateForChecking, AlarmManager.INTERVAL_DAY, pending);
		//store the alarm 
		SharedPreferences.Editor editor = mAlarmPreference.edit();
		editor.putInt(KEY_REQUEST_CODE, requestCode);
		editor.putLong(KEY_SET_ALARM_DATE, currentDateTime);
		editor.commit();
//		mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, currentDateTime, DAY_TO_MILLISECONDS, pending);
	}
	public void deleteAlarmforNewsCheck(){
		int requestCode = mAlarmPreference.getInt(KEY_REQUEST_CODE, -1);
		Log.d(TAG, "requestCode = " + requestCode);
		Intent intent = new Intent(RssConstant.Intent.INTENT_RSSSERVICE_VALIDITY_CHECKING);
		PendingIntent pending = PendingIntent.getService(mContext, requestCode, intent, 0);
		mAlarmManager.cancel(pending);
	}
	public void updateAlarmforNewsCheck(){
		Log.d(TAG, "updateAlarmforNewsCheck");
		long lastDate = mAlarmPreference.getLong(KEY_SET_ALARM_DATE, 0);
		long currentDateTime = 0;
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			currentDateTime = formatter.parse(formatter.format(date)).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Log.d(TAG, "User changes the system Date or not : " + (currentDateTime != lastDate));
//		if(currentDateTime != lastDate){
			deleteAlarmforNewsCheck();
			addAlarmforNewsCheck();
//		}
	}
	public void addAlarm(int widgetId, long updateFrequency) {
		Log.d(TAG, "add an alarm, widgetId is " + widgetId + " update frequency is " + updateFrequency);
		Intent intent = new Intent();
		intent.setAction(RssConstant.Intent.INTENT_RSSSERVICE_STARTREFRESH);
		intent.putExtra(KEY_WIDGET_ID, widgetId);
		intent.putExtra(KEY_AUTO_UPDATE, true);
		PendingIntent refreshPending = PendingIntent.getService(mContext, widgetId, intent, 0);
		SharedPreferences.Editor editor = mAlarmPreference.edit();
		editor.putBoolean(String.valueOf(widgetId), true);
		editor.commit();
//		mWidgetPendingMap.put(widgetId, refreshPending);
		Log.d(TAG, "start alarm manager, current time is " + System.currentTimeMillis());
		mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + updateFrequency, updateFrequency, refreshPending);
	}
	
	public void deleteAlarm(int widgetId) {
		Log.d(TAG, "delete an alarm, widgetId is " + widgetId);
		if(mAlarmPreference.getBoolean(String.valueOf(widgetId), false)) {
			Log.d(TAG, "remove alarm start");
			Intent intent = new Intent();
			intent.setAction(RssConstant.Intent.INTENT_RSSSERVICE_STARTREFRESH);
			intent.putExtra(KEY_WIDGET_ID, widgetId);
			intent.putExtra(KEY_AUTO_UPDATE, true);
			PendingIntent refreshPending = PendingIntent.getService(mContext, widgetId, intent, 0);
			mAlarmPreference.edit().remove(String.valueOf(widgetId)).commit();
			mAlarmManager.cancel(refreshPending);
		} else {
			Log.d(TAG, "this alarm is not exist, no need remove");
		}
	}
	
	public void updateAlarm(int widgetId, long updateFrequency) {
		Log.d(TAG, "update an alarm");
		deleteAlarm(widgetId);
		addAlarm(widgetId, updateFrequency);
	}
	
	public boolean alartSetted(int widgetId){
		return mAlarmPreference.getBoolean(String.valueOf(widgetId), false);
	}
}
