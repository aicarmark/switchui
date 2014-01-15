package com.motorola.mmsp.weather.locationwidget.small;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import locationwidget.config.DeviceConfig;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.RemoteViews;

import com.motorola.mmsp.weather.R;
import com.motorola.mmsp.weather.locationwidget.small.WeatherManager.WeatherInfo;

public class WeatherNewsProvider extends AppWidgetProvider {
	private static final String LOG_TAG = GlobalDef.LOG_TAG;

	private static String ADD_A_CITY = "Add a city";
	private static String LOADING = "Loading";
	private static String NO_WEATHER_INFORMATION = "No weather information";
	private static String NO_NEWS = "No available news";
	private static String NO_NETWORK = "Unable to connect to the network,please check your network conguration";
	private static String AM = "AM";
	private static String PM = "PM";

	private static WeatherManager mWeatherManager = null;
	private static NewsManager mNewsManager = null;

	private static boolean mIsScreenOrientationChange = false;
	private static boolean mIsLanguageChange = false;
	private static boolean mIsPortrait = true;
	private static boolean mIsDeviceNetworkConnect = true;

	private static boolean mIsNeedToStartRssService = false;
	// private static boolean mIsRssServiceConnectNetwork = false;
	// private static String[] mFeeds = null;
	// private static Cursor mRssNewsCursor = null;

	private static String mFirstLoadingCityId = null;
	// private static String mWeatherServiceEdition = null;
	// private static String mRssServiceOrigin = null;

	private static final int mPreferenceMode = Context.MODE_PRIVATE;

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		String action = intent.getAction();
		if (action == null) {
			return;
		}
		if (action.equals(GlobalDef.BROADCAST_WEAHER_UPDATE)) {
			GlobalDef.MyLog.i(LOG_TAG, "----" + action);
			onWeatherServiceUpdate(context, intent);
		} else if (action.equals(GlobalDef.BROADCAST_DETAILVIEWEXIT)) {
			GlobalDef.MyLog.i(LOG_TAG, "----" + action);
			onDetailViewExit(context, intent);
		} else if (action.equals(GlobalDef.BROADCAST_CITYLIST_UPDITING)) {
			GlobalDef.MyLog.i(LOG_TAG, "----" + action);
			onCityListUpdating(context);
		} else if (action.equals(GlobalDef.BROADCAST_CITYLIST_UPDATE_FINISH)) {
			GlobalDef.MyLog.i(LOG_TAG, "----" + action);
			onCityListChanged(context);
		} else if (action.equals(GlobalDef.BROADCAST_SETWIDGETSIZE) || action.equals(GlobalDef.BROADCAST_SETWIDGETSIZE_NEW)) {
			GlobalDef.MyLog.i(LOG_TAG, "----" + action);
			onWidgetSizeChanged(context, intent);
		} else if (action.equals(GlobalDef.BROADCAST_RSS_UPDATE)) {
			GlobalDef.MyLog.i(LOG_TAG, "----" + action);
			onRssUpdate(context, intent);
		} else if (action.equals(GlobalDef.BROADCAST_RSS_NETWORK_STATE)) {
			GlobalDef.MyLog.i(LOG_TAG, "----" + action);
			onRssNetworkStateChange(context, intent);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		updateResourceStrings(context);
		mIsScreenOrientationChange = false;
		final int n = appWidgetIds.length;
		GlobalDef.MyLog.i(LOG_TAG, "----onUpdate widgetCount=" + n);
		
		if (n == 1) {// add a new widget
			SharedPreferences shared = context.getSharedPreferences(GlobalDef.CITYIDSHAREDPFE, mPreferenceMode);
			if (!shared.contains(GlobalDef.WIDGET_CITYID + appWidgetIds[0])) {
				mIsNeedToStartRssService = true;
				GlobalDef.MyLog.d(LOG_TAG, "new widget id");
			} else {
				GlobalDef.MyLog.d(LOG_TAG, "widget id is existed");
			}
		}
		for (int i = 0; i < n; i++) {
			int appWidgetId = appWidgetIds[i];
			updateAppWidget(context, appWidgetManager, appWidgetId);
		}
	}

	@Override
	public void onEnabled(Context context) {
		GlobalDef.MyLog.d(LOG_TAG, "onEnabled--------");
		startUpdateTimeService(context);
		Intent intent = new Intent(GlobalDef.ACTION_LOCATION_WIDGET_ENABLE);
		context.sendBroadcast(intent);
		// getAndSaveTempUnit(context);
		// setScreenCondition(context);
	}

	@Override
	public void onDisabled(Context context) {
		GlobalDef.MyLog.d(LOG_TAG, "onDisabled--------");
		stopUpdateTimeService(context);
		Intent intent = new Intent(GlobalDef.ACTION_LOCATION_WIDGET_DISABLE);
		context.sendBroadcast(intent);
		// stopRssService(context);
		mWeatherManager.clearAllInfo();
		if (mNewsManager != null) {
			mNewsManager.clearAllInfo();
		}
		// System.exit(0);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		if (context == null || appWidgetIds == null) {
			return;
		}
		final int n = appWidgetIds.length;
		SharedPreferences shared = context.getSharedPreferences(GlobalDef.CITYIDSHAREDPFE, mPreferenceMode);
		for (int index = 0; index < n; index++) {
			int appWidgetId = appWidgetIds[index];
			GlobalDef.MyLog.d(LOG_TAG, "appWidgetId=" + appWidgetId + " deleted");
			removeInfo(shared, appWidgetId, true);
			mWeatherManager.deleteInfo(appWidgetId);
			if (mNewsManager != null) {
				mNewsManager.deleteNewsList(appWidgetId);
			}
		}
	}

	public static void init(Context context) {
		if (DeviceConfig.WITH_RSS) {
			mNewsManager = new NewsManager(context);
		} else {
			mNewsManager = null;
		}
		mWeatherManager = WeatherManager.getInstace(context);
		mWeatherManager.setWeatherServiceEdition(context);
		startUpdateTimeService(context);
		getAndSaveTempUnit(context);
		saveDateFormatOrder(context, null);
		setScreenCondition(context);
		updateResourceStrings(context);
	}

	private void updateAllWidgetsWithStartRssService(Context context) {
		AppWidgetManager am = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = am.getAppWidgetIds(new ComponentName(context, WeatherNewsProvider.class));
		for (int temp : appWidgetIds)
			GlobalDef.MyLog.d(LOG_TAG, "updateAllWidgetsWithStartRssService widget count=" + appWidgetIds.length + " id=" + temp);
		final int n = appWidgetIds.length;
		for (int i = 0; i < n; i++) {
			mIsNeedToStartRssService = true;
			int appWidgetId = appWidgetIds[i];
			updateAppWidget(context, am, appWidgetId);
		}
	}

	public static void updateAllWidgets(Context context) {
		AppWidgetManager am = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = am.getAppWidgetIds(new ComponentName(context, WeatherNewsProvider.class));
		for (int temp : appWidgetIds)
			GlobalDef.MyLog.d(LOG_TAG, "updateAllWidgets widget count=" + appWidgetIds.length + " id=" + temp);
		final int n = appWidgetIds.length;
		for (int i = 0; i < n; i++) {
			int appWidgetId = appWidgetIds[i];
			updateAppWidget(context, am, appWidgetId);
		}
	}

	public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
		GlobalDef.MyLog.i(LOG_TAG, "updateAppWidget appWidgetId=" + appWidgetId);
		if (appWidgetId == 0 || context == null) {
			return;
		}
		SharedPreferences shared = context.getSharedPreferences(GlobalDef.CITYIDSHAREDPFE, mPreferenceMode);
		Editor editor = shared.edit();
		/**
		 * if can not get the information, it will get the default widget type,
		 * here is always get the default
		 */
		String widgetType = shared.getString(GlobalDef.WIDGET_TYPE + appWidgetId, GlobalDef.WIDGET_TYPE_DEFAULT);
		boolean isTypeBig = widgetType.equals(GlobalDef.WIDGET_TYPE_BIG);

		int layoutIndex = shared.getInt(GlobalDef.WIDGET_SPANY + appWidgetId, 0);
		if (layoutIndex == 0) {
			int[] temp = getWidgetSizeFormHome(context, appWidgetId);
			layoutIndex = temp[1];
			editor.putInt(GlobalDef.WIDGET_SPANX + appWidgetId, temp[0]);
			editor.putInt(GlobalDef.WIDGET_SPANY + appWidgetId, temp[1]);
			editor.commit();
		}
		GlobalDef.MyLog.d(LOG_TAG, "weatherType=" + widgetType + " layoutIndex=" + layoutIndex);
		int requestNewsCount = 0;
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_small_4_1);
//		if (mNewsManager != null) {
//			switch (layoutIndex) {
//			case 1:
//				views = new RemoteViews(context.getPackageName(), R.layout.widget_small_4_1);
//				break;
//			case 2:
//				if (isTypeBig) {
//					views = new RemoteViews(context.getPackageName(), R.layout.widget_big_4_2);
//				} else {
//					requestNewsCount = 1;
//					views = new RemoteViews(context.getPackageName(), R.layout.widget_small_4_2);
//				}
//				break;
//			case 3:
//				if (isTypeBig) {
//					requestNewsCount = 1;
//					views = new RemoteViews(context.getPackageName(), R.layout.widget_big_4_3);
//				} else {
//					requestNewsCount = 2;
//					views = new RemoteViews(context.getPackageName(), R.layout.widget_small_4_3);
//				}
//				break;
//			case 4:
//			default:
//				if (isTypeBig) {
//					requestNewsCount = 2;
//					views = new RemoteViews(context.getPackageName(), R.layout.widget_big_4_4);
//				} else {
//					requestNewsCount = 3;
//					views = new RemoteViews(context.getPackageName(), R.layout.widget_small_4_4);
//				}
//			}
//		} else {
//			if (isTypeBig) {
//				views = new RemoteViews(context.getPackageName(), R.layout.widget_big_4_2);
//			} else {
//				views = new RemoteViews(context.getPackageName(), R.layout.widget_small_4_1);
//			}
//		}

		String cityId = shared.getString(GlobalDef.WIDGET_CITYID + appWidgetId, GlobalDef.INVALIDCITYID);
		GlobalDef.MyLog.d(LOG_TAG, "shared get cityId=" + cityId);
		if (cityId.equals(GlobalDef.INVALIDCITYID)) {
			if (mFirstLoadingCityId != null) {
				GlobalDef.MyLog.d(LOG_TAG, "~~mFirstLoadingCityId != null");
				cityId = mFirstLoadingCityId;
				editor.putString(GlobalDef.WIDGET_CITYID + appWidgetId, mFirstLoadingCityId);
				editor.commit();
			} else if (!shared.contains(GlobalDef.WIDGET_CITYID + appWidgetId)) {
				GlobalDef.MyLog.d(LOG_TAG, "~~!shared.contains(GlobalDef.WIDGET_CITYID)");
				// get city from citylist
				Cursor c = null;
				try {
					c = GlobalDef.query(context, GlobalDef.uri_citylist, null, null, null);
					if (c != null && c.getCount() > 0) {
						int position = getNewWidgetCityPosition(context, c, shared);
						if (c.moveToPosition(position)) {
							String new_widget_cityid = c.getString(c.getColumnIndex("cityId"));
							if (new_widget_cityid == null) {
								new_widget_cityid = GlobalDef.INVALIDCITYID;
							}
							GlobalDef.MyLog.d(LOG_TAG, "assign cityid to widget =" + new_widget_cityid);
							editor.putString(GlobalDef.WIDGET_CITYID + appWidgetId, new_widget_cityid);
							editor.commit();
							cityId = new_widget_cityid;
						}
					} else {
						editor.putString(GlobalDef.WIDGET_CITYID + appWidgetId, GlobalDef.INVALIDCITYID);
						editor.commit();
					}
				}
		        catch(Exception e){
		        	e.printStackTrace();
		        }
		        finally{
			        // close Cursor
			        if (c != null) {
			        	c.close();
			        	c = null;
			        }
		        }
			} else {
				GlobalDef.MyLog.d(LOG_TAG, "~~mFirstLoadingCityId == null && shared.contains");
			}
		}

		GlobalDef.MyLog.d(LOG_TAG, "final cityId=" + cityId);

		String cityName = null;
		// set weather part
		if (cityId.equals(GlobalDef.INVALIDCITYID)) {// no city in cityList
			setWidgetWeatherNoCity(context, views, appWidgetId, layoutIndex, requestNewsCount);
		} else if (cityId.equals(mFirstLoadingCityId)) {
			setWidgetWeatherLoading(context, views, appWidgetId, layoutIndex, requestNewsCount);
		} else {
			cityName = setWidgetWeather(context, views, appWidgetId, cityId, layoutIndex, requestNewsCount, isTypeBig, shared);
		}

		GlobalDef.MyLog.d(LOG_TAG, "cityName = " + cityName);
		if (mNewsManager != null) {
			if (cityName != null) {
				cityName = cityName.toLowerCase();
				cityName = filterCityName(cityName);
			}
			if (mIsNeedToStartRssService && mFirstLoadingCityId == null) {
				startRssService(context, cityName, appWidgetId);
				mIsNeedToStartRssService = false;
			}
			// set news part
//			setWidgetNews(context, views, appWidgetId, cityName, layoutIndex, requestNewsCount);
		}
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}

	private static void setWidgetWeatherNoCity(Context context, RemoteViews views, int appWidgetId, int layoutIndex, int requestNewsCount) {
		GlobalDef.MyLog.i(LOG_TAG, "setWidgetContentNoCity");
		if (context == null || views == null) {
			return;
		}
		Intent intent = new Intent("motorola.weather.action.CitiesActivity");
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		views.setOnClickPendingIntent(R.id.weather, pendingIntent);

		views.setTextViewText(R.id.no_city, ADD_A_CITY);
		views.setViewVisibility(R.id.no_city, View.VISIBLE);
		views.setViewVisibility(R.id.city_name, View.GONE);
		//views.setViewVisibility(R.id.date, View.GONE);
		views.setViewVisibility(R.id.temperature, View.GONE);
		
		views.setViewVisibility(R.id.date, View.GONE);
		views.setViewVisibility(R.id.time, View.GONE);
		views.setViewVisibility(R.id.apm, View.GONE);
		
		updateTime(null, context, views);
		
		
//		if (requestNewsCount == 0 && mNewsManager != null) {
//			views.setViewVisibility(R.id.rss_icon, View.VISIBLE);
//		} else {
//			views.setViewVisibility(R.id.rss_icon, View.GONE);
//		}
		
		int src = GlobalDef.getSrcIdWithWeatherCondition("sunny");// default
		views.setImageViewResource(R.id.weahter_condition, src);
		setImageAlpha(views, R.id.weahter_condition);
	}

	private static String setWidgetWeatherNoInformation(Context context, RemoteViews views, 
			int appWidgetId, int layoutIndex, int requestNewsCount, String cityId, String cityName) {
		GlobalDef.MyLog.i(LOG_TAG, "setWidgetWeatherNoInformation");
		if (context == null || views == null) {
			return null;
		}

		PendingIntent pendingIntent = getWeatherDetialActivity(context, appWidgetId, cityId);
		views.setOnClickPendingIntent(R.id.weather, pendingIntent);

		views.setViewVisibility(R.id.no_city, View.VISIBLE);
		// views.setViewVisibility(R.id.cityname, View.GONE);
		views.setViewVisibility(R.id.date, View.GONE);
		views.setViewVisibility(R.id.temperature, View.GONE);
		views.setViewVisibility(R.id.time, View.GONE);
		views.setViewVisibility(R.id.apm, View.GONE);

//		if (requestNewsCount == 0 && mNewsManager != null) {
//			views.setViewVisibility(R.id.rss_icon, View.VISIBLE);
//		} else {
//			views.setViewVisibility(R.id.rss_icon, View.GONE);
//		}
		
		int src = GlobalDef.getSrcIdWithWeatherCondition("sunny");
		views.setImageViewResource(R.id.weahter_condition, src);
		setImageAlpha(views, R.id.weahter_condition);
		views.setTextViewText(R.id.no_city, NO_WEATHER_INFORMATION);
		if (cityName != null) {
			views.setViewVisibility(R.id.city_name, View.VISIBLE);
			views.setTextViewText(R.id.city_name, cityName);
			setTextViewWidthLimit(context, views);
		} else {
			views.setViewVisibility(R.id.city_name, View.GONE);
		}
		return cityName;
	}

	private static void setWidgetWeatherLoading(Context context, RemoteViews views, int appWidgetId, int layoutIndex, int requestNewsCount) {
		GlobalDef.MyLog.i(LOG_TAG, "setWidgetWeatherLoading");
		if (context == null || views == null) {
			return;
		}
		Intent intent = new Intent("com.motorola.mmsp.weather.app.CitiesActivity");
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		views.setOnClickPendingIntent(R.id.weather, pendingIntent);

		views.setViewVisibility(R.id.no_city, View.VISIBLE);
		views.setViewVisibility(R.id.city_name, View.GONE);
		//views.setViewVisibility(R.id.date, View.GONE);
		views.setViewVisibility(R.id.temperature, View.GONE);
		views.setViewVisibility(R.id.time, View.GONE);
		views.setViewVisibility(R.id.apm, View.GONE);

//		if (requestNewsCount == 0 && mNewsManager != null) {
//			views.setViewVisibility(R.id.rss_icon, View.VISIBLE);
//		} else {
//			views.setViewVisibility(R.id.rss_icon, View.GONE);
//		}
		
		int src = GlobalDef.getSrcIdWithWeatherCondition("sunny");
		views.setImageViewResource(R.id.weahter_condition, src);
		setImageAlpha(views, R.id.weahter_condition);
		views.setTextViewText(R.id.no_city, LOADING);
	}

	private static String setWidgetWeather(Context context, RemoteViews views, int appWidgetId, 
			String cityId, int layoutIndex, int requestNewsCount, boolean isTypeBig, SharedPreferences shared) {
		GlobalDef.MyLog.i(LOG_TAG, "setWidgetWeather");
		if (context == null || views == null || shared == null || mWeatherManager == null) {
			return null;
		}

		WeatherInfo info = mWeatherManager.getInfo(appWidgetId, cityId, shared);
		if (info == null) {
			setWidgetWeatherNoCity(context, views, appWidgetId, layoutIndex, requestNewsCount);
			return null;
		} else if (info.mState != WeatherManager.WEATHER_STATE_NORMAL) {
			return setWidgetWeatherNoInformation(context, views, appWidgetId, layoutIndex, requestNewsCount, cityId, info.mCityName);
		}

		if (info.mCityName != null) {
			views.setTextViewText(R.id.city_name, info.mCityName);
			setTextViewWidthLimit(context, views);
		} else {
			views.setTextViewText(R.id.city_name, "");
		}

		if (info.mTemperature != null) {
			views.setTextViewText(R.id.temperature, info.mTemperature);
		} else {
			views.setTextViewText(R.id.temperature, "");
		}

		views.setViewVisibility(R.id.no_city, View.GONE);
		views.setViewVisibility(R.id.city_name, View.VISIBLE);
		views.setViewVisibility(R.id.date, View.GONE);
		views.setViewVisibility(R.id.temperature, View.VISIBLE);
		views.setViewVisibility(R.id.time, View.VISIBLE);
		views.setViewVisibility(R.id.apm, View.VISIBLE);
		
//		if (requestNewsCount == 0 && mNewsManager != null) {
//			views.setViewVisibility(R.id.rss_icon, View.VISIBLE);
//		} else {
//			views.setViewVisibility(R.id.rss_icon, View.GONE);
//		}
		
		if (isTypeBig && layoutIndex != 1) {
			//views.setViewVisibility(R.id.date, View.VISIBLE);
		} else {
			//views.setViewVisibility(R.id.date, View.GONE);
		}
		PendingIntent pendingIntent = getWeatherDetialActivity(context, appWidgetId, cityId);
		views.setOnClickPendingIntent(R.id.weather, pendingIntent);

		TimeZone tz = null;
		String timezone = info.mTimeZone; 

		GlobalDef.MyLog.d(LOG_TAG, "timezone string = " + timezone);
		
		if (timezone == null || timezone.equals("")) {
			tz = TimeZone.getDefault();
		} else {
			tz = TimeZone.getTimeZone(timezone);
		}
		GlobalDef.MyLog.d(LOG_TAG, "$$$ TimeZome = " + tz.getID());
		Calendar cal = Calendar.getInstance(tz);
		Date curDate = cal.getTime();
		/********* test time string ********/
		String time111 = android.text.format.DateFormat.getTimeFormat(context).format(curDate);
		GlobalDef.MyLog.d(LOG_TAG, "test time string = " + time111);
		
		updateTime(timezone, context, views);

		String weatherCodition = info.mWeather;
		if (weatherCodition != null) {
			String sunrise = null;
			String sunset = null;
			int sunrise_hour = 6;
			int sunrise_minute = 0;
			int sunset_hour = 18;
			int sunset_minute = 0;
			if (mWeatherManager.isAccuWeather()) {
				sunrise = info.mSunRise;
				sunset = info.mSunSet;
				if (sunrise != null) {
					if (GlobalDef.CONSTANT_ILLUMINATION.equals(sunrise)) {
						sunrise_hour = -1;
						sunrise_minute = 0;
					} else if (GlobalDef.CONSTANT_DARKNESS.equals(sunrise)) {
						sunrise_hour = 25;
						sunrise_minute = 0;
					} else {
						int[] temp = getTimes(sunrise);
						sunrise_hour = temp[0];
						sunrise_minute = temp[1];
					}

				}
				if (sunset != null) {
					if (GlobalDef.CONSTANT_ILLUMINATION.equals(sunset)) {
						sunset_hour = 25;
						sunset_minute = 0;
					} else if (GlobalDef.CONSTANT_DARKNESS.equals(sunset)) {
						sunset_hour = -1;
						sunset_minute = 0;
					} else {
						int[] temp = getTimes(sunset);
						sunset_hour = temp[0];
						sunset_minute = temp[1];
					}
				}
			}
			GlobalDef.MyLog.d(LOG_TAG, "sunrise=" + sunrise + " sunset=" + sunset);
			GlobalDef.MyLog.d(LOG_TAG, "sunrise_hour=" + sunrise_hour + " sunrise_minute=" + sunrise_minute);
			GlobalDef.MyLog.d(LOG_TAG, "sunset_hour=" + sunset_hour + " sunset_minute=" + sunset_minute);
			// location hour and minute
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			int minute = cal.get(Calendar.MINUTE);
			GlobalDef.MyLog.d(LOG_TAG, "current time=" + hour + ":" + minute);
			if (hour < sunrise_hour || hour > sunset_hour || (hour == sunrise_hour && minute < sunrise_minute) || (hour == sunset_hour && minute >= sunset_minute)) {
				weatherCodition = weatherCodition + "_night";
			}
		}
		// weatherCodition = "thunder";
		GlobalDef.MyLog.d(LOG_TAG, "weatherCodition =" + weatherCodition);
		int srcId = GlobalDef.getSrcIdWithWeatherCondition(weatherCodition);
		views.setImageViewResource(R.id.weahter_condition, srcId);
		setImageAlpha(views, R.id.weahter_condition);

		return info.mCityName;
	}
	
	public static void updateTime(String timeZoneId, Context context,RemoteViews views)
	{
		TimeZone timeZone = null;
		if (TextUtils.isEmpty(timeZoneId))
		{
			timeZone = TimeZone.getDefault();
		} else
		{
			timeZone = TimeZone.getTimeZone(timeZoneId);
		}

		Calendar cal = Calendar.getInstance(timeZone);
		Date now = cal.getTime();

		ContentResolver cr = context.getContentResolver();
		String timeFormat = Settings.System.getString(cr,
				Settings.System.TIME_12_24);

		SimpleDateFormat dateFormat = null;
		String date = null;
		String apm = null;
		if ("24".equals(timeFormat))
		{
			dateFormat = new SimpleDateFormat("yyyy-MM-dd&HH:mm");
			dateFormat.setTimeZone(timeZone);
			date = dateFormat.format(now);
			apm = "";
		} else
		{
			dateFormat = new SimpleDateFormat("yyyy-MM-dd&h:mm");
			dateFormat.setTimeZone(timeZone);
			date = dateFormat.format(now);
			if (cal.get(Calendar.AM_PM) == 0)
			{
				apm = AM;
			} else
			{
				apm = PM;
			}
		}

		String[] dateAndtime = date.split("&");
		
		views.setTextViewText(R.id.date, dateAndtime[0]);
		views.setTextViewText(R.id.time, dateAndtime[1]);
		views.setTextViewText(R.id.apm, apm);
	}

	/**
	 * if set in code,it will display abnormal when land screen to port screen.
	 */
	private static void setTextViewWidthLimit(Context context, RemoteViews views) {
		// int widgetWidth =
		// context.getResources().getDimensionPixelSize(R.dimen.widget_width);
		// GlobalDef.MyLog.w(LOG_TAG, "widgetWidth = "+widgetWidth);
		// views.setInt(R.id.cityname, "setMaxWidth", widgetWidth / 2);
	}

	private static void setImageAlpha(RemoteViews views, int srcId) {
		// if the format of weather resource is jpg, need open the following
		// code for set the image alpha.
		if (DeviceConfig.PICTURE_SUFFIX_JPG.equals(DeviceConfig.WEATHER_RES_SUFFIX)) {
			GlobalDef.MyLog.w(LOG_TAG, "setImageAlpha = 230(0~255)");
			views.setInt(srcId, "setAlpha", 230);
		}
	}

	/** Get the entry of the detail information. */
	private static PendingIntent getWeatherDetialActivity(Context context, int widgetId, String cityId) {
		if (context == null || widgetId == 0 || cityId == null)
			return null;
		PendingIntent pendingIntent = null;
		Intent intent = new Intent("motorola.weather.action.MoreWeatherActivity");
		// intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		intent.putExtra("widgetId", widgetId);
		intent.putExtra("cityId", cityId);
		pendingIntent = PendingIntent.getActivity(context, widgetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);//
		return pendingIntent;
	}

	// need to register the permission
	/**
	 * Get the widget size from home application. It will return the default
	 * size if fail to obtain.
	 */
	private static int[] getWidgetSizeFormHome(Context context, int appWidgetId) {
		Uri uri = Uri.parse("content://com.android.launcher.settings/favorites");
		int[] span = new int[2];
		span[0] = GlobalDef.WIDGET_INIT_SPANX;
		span[1] = GlobalDef.WIDGET_INIT_SPANY;
		Cursor c = null;
		try {
			c = GlobalDef.query(context, uri, "appWidgetId = ? ", new String[] { "" + appWidgetId }, null);
			if (c != null && c.moveToFirst()) {
				int id = c.getInt(c.getColumnIndex("appWidgetId"));
				span[0] = c.getInt(c.getColumnIndex("spanX"));
				span[1] = c.getInt(c.getColumnIndex("spanY"));
				GlobalDef.MyLog.i(LOG_TAG, "from home appWidgetId = " + id + " " + span[0] + " " + span[1]);
			} else {
				GlobalDef.MyLog.i(LOG_TAG, "from home fail,set widget size to default");
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
		return span;
	}

	/**
	 * Return the first city id which is not being used. If all the city id are
	 * used, it will return the first id on the city list.
	 */
	private static int getNewWidgetCityPosition(Context context, Cursor c, SharedPreferences shared) {
		AppWidgetManager am = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = am.getAppWidgetIds(new ComponentName(context, WeatherNewsProvider.class));
		ArrayList<String> addCityList = new ArrayList<String>();
		int n = appWidgetIds.length;
		for (int i = 0; i < n; i++) {
			int appWidgetId = appWidgetIds[i];
			String widget_cityId = shared.getString(GlobalDef.WIDGET_CITYID + appWidgetId, GlobalDef.INVALIDCITYID);
			if (!GlobalDef.INVALIDCITYID.equals(widget_cityId)) {
				addCityList.add(widget_cityId);
			}
		}
		// n = addCityList.size();
		// for(int i = 0;i< n;i++){
		// String temp = addCityList.get(i);
		// GlobalDef.MyLog.e(LOG_TAG, temp);
		// }
		n = c.getCount();
		// record the widget counts of each cityId
		int[] cityIdConunt = new int[n];
		boolean isCirculateOnce = false;
		int position = 0;
		for (int i = 0; i < n; i++) {
			if (c.moveToPosition(i)) {
				if (isCirculateOnce) {// circulate once
					String widget_cityid = c.getString(c.getColumnIndex("cityId"));
					if (!addCityList.contains(widget_cityid)) {
						position = i;
						mIsNeedToStartRssService = true;
						break;
					}
				} else {// circulate always
					int m = addCityList.size();
					String widget_cityid = c.getString(c.getColumnIndex("cityId"));
					for (int j = 0; j < m; j++) {
						if (widget_cityid != null && widget_cityid.equals(addCityList.get(j))) {
							cityIdConunt[i]++;
						}
					}
				}

			}
		}
		if (!isCirculateOnce) {
			int min = Integer.MAX_VALUE;
			for (int i = 0; i < n; i++) {
				GlobalDef.MyLog.d(LOG_TAG, "cityId " + i + " = " + cityIdConunt[i]);
				if (cityIdConunt[i] < min) {
					min = cityIdConunt[i];
					position = i;
				}
			}
			if (min == 0) {
				mIsNeedToStartRssService = true;
			}
		}
		GlobalDef.MyLog.d(LOG_TAG, "position=" + position);
		return position;
	}

	/** Get and save the temperature unit from the weather setting. */
	public static boolean getAndSaveTempUnit(Context context) {
		if (null == context) {
			return false;
		}
		boolean isChange = false;
		int unit = queryTempUnit(context);
		if (unit < 0) {
			unit = 0;
		}
		SharedPreferences shared = context.getSharedPreferences(GlobalDef.CITYIDSHAREDPFE, mPreferenceMode);
		int oldUnit = shared.getInt(GlobalDef.TEMPUNIT, -1);
		if (unit != oldUnit) {
			isChange = true;
			final Editor editor = shared.edit();
			editor.putInt(GlobalDef.TEMPUNIT, unit);
			editor.commit();
		}
		return isChange;
	}

	public static final String KEY_DATE_FORMAT_ORDER = "dateFormatOder";

	public static String getPreDateFormatOrder(Context context) {
		SharedPreferences shared = context.getSharedPreferences(GlobalDef.CITYIDSHAREDPFE, mPreferenceMode);
		String dateFormatOrder = shared.getString(KEY_DATE_FORMAT_ORDER, "");
		return dateFormatOrder;
	}

	/**
	 * If dateFormatOrder is not null,save the dateFormatOrder,else,save the
	 * current dateFormatOrder of system.
	 */
	public static void saveDateFormatOrder(Context context, String dateFormatOrder) {
		SharedPreferences shared = context.getSharedPreferences(GlobalDef.CITYIDSHAREDPFE, mPreferenceMode);
		if (dateFormatOrder == null) {
			char[] temp = android.text.format.DateFormat.getDateFormatOrder(context);
			dateFormatOrder = new String(temp);
			Editor editor = shared.edit();
			editor.putString(KEY_DATE_FORMAT_ORDER, dateFormatOrder);
			editor.commit();
		} else {
			Editor editor = shared.edit();
			editor.putString(KEY_DATE_FORMAT_ORDER, dateFormatOrder);
			editor.commit();
		}
	}

	private static int queryTempUnit(Context context) {
		int res = -1;
		if (context == null) {
			return res;
		}
		String selections = "settingName = ?";
		String[] selectionargs = new String[] { "temperature_uint" };
		Cursor c = null;
		try {
			 c = GlobalDef.query(context, GlobalDef.uri_setting, selections, selectionargs, null);
	
			if (c != null) {
				if (c.moveToFirst()) {
					res = c.getInt(c.getColumnIndex("settingValue"));
					String temp = c.getString(1);
					GlobalDef.MyLog.d(LOG_TAG, "temp unit string =" + temp);
	
				}
			} else {
				GlobalDef.MyLog.d(LOG_TAG, "temp unit query result:cur == null");
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
		GlobalDef.MyLog.d(LOG_TAG, "temp unit=" + res);
		return res;

	}

	/**
	 * If the cityId is using by other widget,it will return true ,else return
	 * false.
	 */
	private boolean isCityIdExist(String cityId, SharedPreferences shared, int[] appWidgetIds) {
		if (cityId == null || shared == null || appWidgetIds == null)
			return false;
		boolean flag = false;
		int n = appWidgetIds.length;
		for (int i = 0; i < n; i++) {
			int widgetId = appWidgetIds[i];
			String cityIdExist = shared.getString(GlobalDef.WIDGET_CITYID + widgetId, GlobalDef.INVALIDCITYID);
			if (cityId.equals(cityIdExist)) {
				flag = true;
				break;
			}
		}
		return flag;
	}

	// /**If the rss setting contain the specific topic.*/
	// private boolean isTopicActive(String topic){
	// if(mFeeds == null || topic == null)
	// return false;
	// int n = mFeeds.length;
	// for(int i=0;i<n;i++){
	// if(topic.equals(mFeeds[i]))
	// return true;
	// }
	// return false;
	// }
	/** Parse the string to array. */
	// private void setCurrentFeeds(String feed){
	// if(DEBUG) Log.d(LOG_TAG,"feeds from rss : "+feed);
	// if(feed!=null && !feed.equals("")){
	// mFeeds = feed.split(",", 10);
	// for(String temp :mFeeds)
	// if(DEBUG) Log.d(LOG_TAG,"current mFeeds contain: "+temp);
	// }
	// else {
	// mFeeds = null;
	// if(DEBUG) Log.d(LOG_TAG,"invalid mFeeds");
	// }
	// }

	public static void onTempUnitChanged(Context context) {
		GlobalDef.MyLog.d(LOG_TAG, "onTempUnitChanged");
		boolean isChange = getAndSaveTempUnit(context);
		if (isChange) {
			mWeatherManager.clearAllInfo();
			updateAllWidgets(context);
		} else {
			GlobalDef.MyLog.d(LOG_TAG, "tempUnit is same as before");
		}
	}

	/** The network condition of the device */
	public static void onNetworkChange(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		if (info != null && info.isConnected()) {
			GlobalDef.MyLog.e(LOG_TAG, "OK!network connected!!!!");
			mIsDeviceNetworkConnect = true;
		} else {
			GlobalDef.MyLog.e(LOG_TAG, "NO!network disconnected!!!!");
			mIsDeviceNetworkConnect = false;
		}
	}

	public static void onConfigurationChange(Context context, boolean withLanguageChange) {
		if (!withLanguageChange) {
			DisplayMetrics dm = context.getResources().getDisplayMetrics();
			int width = dm.widthPixels;
			int height = dm.heightPixels;
			if (mIsPortrait != (width < height)) {
				mIsScreenOrientationChange = true;
				mIsPortrait = width < height;
				GlobalDef.MyLog.w(LOG_TAG, "isPortrait = " + mIsPortrait);
				// updateAllWidgets(context);
				// motohome will send broadcast cause widget update after rotate
				// screen
			} else {
				updateAllWidgets(context);
			}
		} else {
			mIsLanguageChange = true;
			if (mNewsManager != null) {
				mNewsManager.clearAllInfo();
				updateResourceStrings(context);
				// update the news base on current language
				startRssService(context, null, 0);
				updateAllWidgets(context);
			}
		}
	}

	public static void onHomeResume(Context context) {
		Cursor c = null;
		try {
			c = GlobalDef.query(context, GlobalDef.uri_citylist, null, null, null);
			GlobalDef.MyLog.i(LOG_TAG, "---- onHomeResume -------------------- ");
			if (c != null && c.getCount() > 0) {
				String type = GlobalDef.WIDGET_TYPE_DEFAULT;
				if (GlobalDef.WIDGET_TYPE_BIG.equals(type)) {
					char[] temp = android.text.format.DateFormat.getDateFormatOrder(context);
					String dateOrder = new String(temp);
					String dateOrder_old = getPreDateFormatOrder(context);
					if (!dateOrder_old.equals(dateOrder)) {
						GlobalDef.MyLog.d(LOG_TAG, "dateFormat change!");
						saveDateFormatOrder(context, dateOrder);
						updateAllWidgets(context);
					}
				}
			} else {
				GlobalDef.MyLog.i(LOG_TAG, "---- onHomeResume, city list is empty ");
				AppWidgetManager am = AppWidgetManager.getInstance(context);
				int[] appWidgetIds = am.getAppWidgetIds(new ComponentName(context, WeatherNewsProvider.class));
				SharedPreferences shared = context.getSharedPreferences(GlobalDef.CITYIDSHAREDPFE, mPreferenceMode);
				if (isAnyCityExist(shared, appWidgetIds)) {
					GlobalDef.MyLog.i(LOG_TAG, "---- onHomeResume, remove all info and move to add a city status ");
					for (int i = 0; i < appWidgetIds.length; i++) {
						removeInfo(shared, appWidgetIds[i], false);
						if (mWeatherManager != null) {
							mWeatherManager.deleteInfo(appWidgetIds[i]);
						}
						if (mNewsManager != null) {
							mNewsManager.deleteNewsList(appWidgetIds[i]);
						}
						updateAppWidget(context, am, appWidgetIds[i]);
					}
				} else {
					GlobalDef.MyLog.i(LOG_TAG, "---- isAnyCityExist = false, do not need to remove info ");
				}
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
	}
	
	public static void onDataClear(Context context) {
		GlobalDef.MyLog.i(LOG_TAG, "------- onDataClear, weather service data cleared, move to add a ctiy status ");
		AppWidgetManager am = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = am.getAppWidgetIds(new ComponentName(context, WeatherNewsProvider.class));
		SharedPreferences shared = context.getSharedPreferences(GlobalDef.CITYIDSHAREDPFE, mPreferenceMode);
		for (int i=0; i<appWidgetIds.length; i++) {
			removeInfo(shared, appWidgetIds[i], false);
			if (mWeatherManager != null) {
			    mWeatherManager.deleteInfo(appWidgetIds[i]);
			}
			if (mNewsManager != null) {
				mNewsManager.deleteNewsList(appWidgetIds[i]);
			}
			updateAppWidget(context, am, appWidgetIds[i]);
		}
	}
	
	private static boolean isAnyCityExist(SharedPreferences shared, int[] appWidgetIds) {
		if (shared == null || appWidgetIds == null) {
			return false;
		}
		boolean flag = false;
		int n = appWidgetIds.length;
		for (int i = 0; i < n; i++) {
			String cityIdExist = shared.getString(GlobalDef.WIDGET_CITYID + appWidgetIds[i], GlobalDef.INVALIDCITYID);
			if (!GlobalDef.INVALIDCITYID.equals(cityIdExist)) {
				flag = true;
				break;
			}
		}
		return flag;
	}
	
	/** When the news need to update ,the function will implement. */
	private void onRssUpdate(Context context, Intent intent) {
		if (mNewsManager != null) {
			Bundle b = intent.getExtras();
			String feeds = b.getString(GlobalDef.KEY_CURRENT_FEED);
			String origin = b.getString(GlobalDef.RSS_ORIGIN_KEY);
			mNewsManager.setFeeds(feeds);
			mNewsManager.setOrigin(origin);
			mNewsManager.clearAllInfo();
			mNewsManager.finishConnect();
			updateAllWidgets(context);
		}
	}

	/**
	 * If rss service start to connect to the Internet,it will cause this
	 * function implement.
	 */
	private void onRssNetworkStateChange(Context context, Intent intent) {
		if (mNewsManager != null) {
			Bundle b = intent.getExtras();
			String state = b.getString(GlobalDef.KEY_NETWORK_STATE);
			GlobalDef.MyLog.d(LOG_TAG, "network state: " + state);
			if (GlobalDef.RSS_NETWORK_STATE_START.equals(state)) {
				mNewsManager.startConnect();
				String feed = b.getString(GlobalDef.KEY_CURRENT_FEED);
				String origin = b.getString(GlobalDef.RSS_ORIGIN_KEY);
			}
			updateAllWidgets(context);
		}
	}

	/** This function will implement when weather need to update. */
	private void onWeatherServiceUpdate(Context context, Intent intent) {
		mWeatherManager.clearAllInfo();
		if (mIsLanguageChange && mNewsManager != null) {
			if (mNewsManager.isTopicActive("l")) {
				updateAllWidgetsWithStartRssService(context);
			} else {
				updateAllWidgets(context);
			}
			mIsLanguageChange = false;
		} else {
			updateAllWidgets(context);
		}
	}

	/**
	 * When select a city in the weather detail window,this function will
	 * implement.
	 */
	private void onDetailViewExit(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		int widgetId = bundle.getInt("widgetId");
		String cityIdNew = bundle.getString("cityId");
		GlobalDef.MyLog.d(LOG_TAG, "detailview exit widgetId=" + widgetId + " cityId=" + cityIdNew);
		if (cityIdNew == null || cityIdNew.equals("")) {
			return;
		}
		AppWidgetManager am = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = am.getAppWidgetIds(new ComponentName(context, WeatherNewsProvider.class));
		for (int temp : appWidgetIds) {
			GlobalDef.MyLog.d(LOG_TAG, "find widget count=" + appWidgetIds.length + " id=" + temp);
		}
		final int n = appWidgetIds.length;
		boolean find = false;
		for (int i = 0; i < n; i++) {
			int appWidgetId = appWidgetIds[i];
			if (appWidgetId == widgetId) {
				find = true;
				break;
			}
		}
		if (find) {
			SharedPreferences shared = context.getSharedPreferences(GlobalDef.CITYIDSHAREDPFE, mPreferenceMode);
			String cityIdOld = shared.getString(GlobalDef.WIDGET_CITYID + widgetId, GlobalDef.INVALIDCITYID);
			GlobalDef.MyLog.d(LOG_TAG, "cityIdOld = " + cityIdOld);
			if (!cityIdOld.equals(cityIdNew)) {
				GlobalDef.MyLog.d(LOG_TAG, "detailview exit city changed from " + cityIdOld + " to " + cityIdNew);
				if (!isCityIdExist(cityIdNew, shared, appWidgetIds)) {
					mIsNeedToStartRssService = true;
				}
				Editor editor = shared.edit();
				editor.putString(GlobalDef.WIDGET_CITYID + widgetId, cityIdNew);
				editor.commit();
				mWeatherManager.deleteInfo(widgetId);
				if (mNewsManager != null) {
					mNewsManager.deleteNewsList(widgetId);
				}
				updateAppWidget(context, am, widgetId);
			} else {
				GlobalDef.MyLog.d(LOG_TAG, "detailview exit cityId=" + cityIdNew + " found,  no changed ");
			}
		}
	}

	/**
	 * When add a city to the city list, this function will implement. Note when
	 * this function implement,the weather information will start to be
	 * download.
	 */
	private void onCityListUpdating(Context context) {
		mFirstLoadingCityId = getFirstLoadingCityId(context);
		if (mFirstLoadingCityId != null) {
			updateAllWidgets(context);
		}
	}

	/**
	 * When finish download the weather information, this function will
	 * implement.
	 */
	private void onCityListChanged(Context context) {
		GlobalDef.MyLog.d(LOG_TAG, "onCityListChanged");
		final SharedPreferences shared = context.getSharedPreferences(GlobalDef.CITYIDSHAREDPFE, mPreferenceMode);
		Cursor c = null;
		try {
			c = GlobalDef.query(context, GlobalDef.uri_citylist, null, null, null);
			AppWidgetManager am = AppWidgetManager.getInstance(context);
			int[] appWidgetIds = am.getAppWidgetIds(new ComponentName(context, WeatherNewsProvider.class));
			for (int temp : appWidgetIds)
				GlobalDef.MyLog.d(LOG_TAG, "find widget count=" + appWidgetIds.length + " id=" + temp);
			final int n = appWidgetIds.length;
			boolean find = false;
			String firstLoadingCity = mFirstLoadingCityId;
			mFirstLoadingCityId = null;
			for (int i = 0; i < n; i++) {
				int appWidgetId = appWidgetIds[i];
				String widget_cityId = shared.getString(GlobalDef.WIDGET_CITYID + appWidgetId, GlobalDef.INVALIDCITYID);
				GlobalDef.MyLog.d(LOG_TAG, "widget_cityId=" + widget_cityId);
				if (widget_cityId == null || widget_cityId.equals(GlobalDef.INVALIDCITYID)) {
					GlobalDef.MyLog.d(LOG_TAG, "no information to update");
					if ( c == null || c.getCount() == 0) {
						// no cityid in sharedPreferences
						continue;
					} else {
						removeInfo(shared, appWidgetId, false);
						mWeatherManager.deleteInfo(appWidgetId);
						if (mNewsManager != null) {
							mNewsManager.deleteNewsList(appWidgetId);
						}
						updateAppWidget(context, am, appWidgetId);
					}
				}
	
				find = false;
				String cityid = null;
				if (c != null && c.moveToFirst()) {
					do {
						cityid = c.getString(c.getColumnIndexOrThrow("cityId"));
						if (widget_cityId.equals(cityid)) {
							find = true;
							break;
						}
					} while (c.moveToNext());
				}
				if (!find) {// the city was deleted
					GlobalDef.MyLog.d(LOG_TAG, "cityId= " + widget_cityId + " was deleted ,update");
					removeInfo(shared, appWidgetId, false);
					mWeatherManager.deleteInfo(appWidgetId);
					if (mNewsManager != null) {
						mNewsManager.deleteNewsList(appWidgetId);
					}
					updateAppWidget(context, am, appWidgetId);
				} else if (widget_cityId.equals(firstLoadingCity)) {
					GlobalDef.MyLog.d(LOG_TAG, "cityId= " + widget_cityId + " finish loading ,update widget at id " + appWidgetId);
	//				mFirstLoadingCityId = null;
					// removeInfo to cause the isNeedToStartRssService be set true in getposition function
					removeInfo(shared, appWidgetId, false);
					updateAppWidget(context, am, appWidgetId);
					// when more than one widget on home screen,then can all finish their loading condition
	//				mFirstLoadingCityId = widget_cityId;
				} else {
					GlobalDef.MyLog.d(LOG_TAG, "cityId= " + widget_cityId + " was found ,update widegt when weather was changed from no weather info");
					WeatherInfo info = mWeatherManager.getInfo(appWidgetId, widget_cityId, shared);
					if(info.mState != WeatherManager.WEATHER_STATE_NORMAL){
						mWeatherManager.deleteInfo(appWidgetId);
						updateAllWidgets(context);
					}
				}
			}
	//		mFirstLoadingCityId = null;
		}
        catch(Exception e){
        	e.printStackTrace();
        }
        finally{
	        // close Cursor
	        if( c != null )
	        c.close();
        }
	}

	/** Resize function of home. */
	private void onWidgetSizeChanged(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
		int appWidgetId = extras.getInt("appWidgetId");
		int spanX = extras.getInt("spanX");
		int spanY = extras.getInt("spanY");
		if ((spanX < 0) || (spanX > 0xff)) {
			spanX = GlobalDef.WIDGET_INIT_SPANX;
		}
		if ((spanY < 0) || (spanY > 0xff)) {
			spanY = GlobalDef.WIDGET_INIT_SPANY;
		}
		GlobalDef.MyLog.d(LOG_TAG, "set widget size appWidgetId=" + appWidgetId + " spanX=" + spanX + " spanY=" + spanY);

		SharedPreferences shared = context.getSharedPreferences(GlobalDef.CITYIDSHAREDPFE, mPreferenceMode);
		int spanXOld = shared.getInt(GlobalDef.WIDGET_SPANX + appWidgetId, GlobalDef.WIDGET_INIT_SPANX);
		int spanYOld = shared.getInt(GlobalDef.WIDGET_SPANY + appWidgetId, GlobalDef.WIDGET_INIT_SPANY);
		if (spanYOld != spanY || spanXOld != spanX) {
			if (spanY > spanYOld && mNewsManager != null) {
				mNewsManager.deleteNewsList(appWidgetId);
			}
			Editor editor = shared.edit();
			editor.putInt(GlobalDef.WIDGET_SPANX + appWidgetId, spanX);
			editor.putInt(GlobalDef.WIDGET_SPANY + appWidgetId, spanY);
			editor.commit();
			AppWidgetManager am = AppWidgetManager.getInstance(context);
			updateAppWidget(context, am, appWidgetId);
		} else if (mIsScreenOrientationChange) {
			// prevent the widget freezed after rotate the screen
			AppWidgetManager am = AppWidgetManager.getInstance(context);
			updateAppWidget(context, am, appWidgetId);
		} else {
			GlobalDef.MyLog.d(LOG_TAG, "size is no changed");
		}
	}

	// start the service to get system response
	public static void startUpdateTimeService(Context context) {
		GlobalDef.MyLog.d(LOG_TAG, "startUpdateTimeService");
		Intent updateService = new Intent(context, WidgetUpdateService.class);
		context.startService(updateService);
	}

	public void stopUpdateTimeService(Context context) {
		GlobalDef.MyLog.d(LOG_TAG, "stopUpdateTimeService");
		Intent updateService = new Intent(context, WidgetUpdateService.class);
		context.stopService(updateService);
	}

	public static void startRssService(Context context, String cityName, int widgetId) {
		Intent rssIntent = new Intent(GlobalDef.SERVICE_INTENT_RSS);
		Bundle bundle = new Bundle();
		if (cityName != null) {
			bundle.putString("arcsoft.location.city", cityName);
		}
		if (widgetId != 0) {
			bundle.putInt("arcsoft.rssNews.widgetId", widgetId);
		}
		rssIntent.putExtras(bundle);
		context.startService(rssIntent);
		if (cityName != null) {
			GlobalDef.MyLog.w(LOG_TAG, "startRssService " + cityName + " " + widgetId);
		} else {
			GlobalDef.MyLog.w(LOG_TAG, "startRssService " + "null " + widgetId);
		}
	}

	public void stopRssService(Context context) {
		GlobalDef.MyLog.w(LOG_TAG, "stopRssService");
		Intent rssIntent = new Intent(GlobalDef.SERVICE_INTENT_RSS);
		context.stopService(rssIntent);
	}

	/** Clear the native information. */
	private static void removeInfo(SharedPreferences shared, int widgetId, boolean isDelete) {
		Editor editor = shared.edit();
		if (shared.contains(GlobalDef.WIDGET_CITYID + widgetId)) {
			editor.remove(GlobalDef.WIDGET_CITYID + widgetId);
		}
		if (isDelete) {
			if (shared.contains(GlobalDef.WIDGET_SPANX + widgetId)) {
				editor.remove(GlobalDef.WIDGET_SPANX + widgetId);
			}
			if (shared.contains(GlobalDef.WIDGET_SPANY + widgetId)) {
				editor.remove(GlobalDef.WIDGET_SPANY + widgetId);
			}
			if (shared.contains(GlobalDef.WIDGET_TYPE + widgetId)) {
				editor.remove(GlobalDef.WIDGET_TYPE + widgetId);
			}
		}
		editor.commit();
	}

	public static int[] getAllWidgetIds(Context context) {
		AppWidgetManager am = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = am.getAppWidgetIds(new ComponentName(context, WeatherNewsProvider.class));
		return appWidgetIds;
	}

	/** When add the first city, it will be find. */
	public String getFirstLoadingCityId(Context context) {
		String cityId = null;
		int n1 = 0;
		int n2 = 0;
		String selections = "isExternal = ?";
		String[] selectionargs = new String[] {"0"};
		
		Cursor c1 = null;
		try {
			c1 = GlobalDef.query(context, GlobalDef.uri_citylist, null, null, null);
			Cursor c2 = null;
			try {
				c2 = GlobalDef.query(context, GlobalDef.uri_condtions, selections, selectionargs, null);
				n1 = c1 != null ? c1.getCount() : 0;
				n2 = c2 != null ? c2.getCount() : 0;
				GlobalDef.MyLog.d(LOG_TAG, "c1.getCount() = " + n1);
				GlobalDef.MyLog.d(LOG_TAG, "c2.getCount() = " + n2);
			}
	        catch(Exception e){
	        	e.printStackTrace();
	        }
	        finally{
		        // close Cursor
		        if( c2 != null )
		        c2.close();
	        }
			if (n1 == 1 && n2 == 0) {
				if (c1.moveToFirst()) {
					cityId = c1.getString(c1.getColumnIndex("cityId"));
					SharedPreferences shared = context.getSharedPreferences(GlobalDef.CITYIDSHAREDPFE, mPreferenceMode);
					int[] appWidgetIds = getAllWidgetIds(context);
					if ( isCityIdExist(cityId, shared, appWidgetIds) ) {
						cityId = null;
						GlobalDef.MyLog.d(LOG_TAG, "truthless loading state");
					} else {
						GlobalDef.MyLog.d(LOG_TAG, "loading cityId = " + cityId);
					}
				}
			}
		}
        catch(Exception e){
        	e.printStackTrace();
        }
        finally{
	        // close Cursor
	        if( c1 != null )
	        c1.close();
        }
		if ( n1 == 0 && n2 == 0 ) {
			selections = "settingName = ?";
			selectionargs = new String[] { "setting_auto_city" };
			Cursor c = null;
			try {
				c = GlobalDef.query(context, GlobalDef.uri_setting, selections, selectionargs, null);
				if (c != null) {
					if (c.moveToFirst()) {
						String temp = c.getString(c.getColumnIndex("settingValue"));
						GlobalDef.MyLog.d(LOG_TAG, "setting_auto_city =" + temp);
						if ("true".equals(temp)) {
							cityId = GlobalDef.AUTOCITYID;
						}
					}
				} else {
					GlobalDef.MyLog.d(LOG_TAG, "setting_auto_city :cur == null");
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
		}
		return cityId;
	}

	/** Get the native string base on device language . */
	public static void updateResourceStrings(Context context) {
		ADD_A_CITY = context.getResources().getString(R.string.add_a_city);
		LOADING = context.getResources().getString(R.string.loading);
		NO_WEATHER_INFORMATION = context.getResources().getString(R.string.no_weather_information);
		NO_NEWS = context.getResources().getString(R.string.no_news);
		NO_NETWORK = context.getResources().getString(R.string.no_network);
		AM = context.getResources().getString(R.string.apm_am);
		PM = context.getResources().getString(R.string.apm_pm);
	}

	/**
	 * modify the style of city names. For example ,xi'an -> xian,new york ->
	 * new+york
	 */
	public static String filterCityName(String city) {
		if (city != null) {
			return city.replace("'", "").replace(" ", "+");
		} else {
			return city;
		}
	}

	/**
	 * get the hour and minute of day from the specify string format,for
	 * examples 06:30 AM,11:04 PM.
	 */
	public static int[] getTimes(String str) {
		if (str == null) {
			return new int[] { 0, 0 };
		}
		int breakpoint_hour = str.indexOf(":");
		int breakpoint_minute = str.indexOf(" ");
		try {
			int hour = Integer.parseInt((String) str.subSequence(0, breakpoint_hour));
			int minute = Integer.parseInt((String) str.subSequence(breakpoint_hour + 1, breakpoint_minute));
			if (str.contains("PM")) {
				hour += 12;
			}
			return new int[] { hour, minute };
		} catch (Exception e) {
			e.printStackTrace();
			return new int[] { 25, 0 };
			// hour is always small than 25,then set the weather to night
		}
		// GlobalDef.MyLog.d(LOG_TAG, "breakpoint_hour=" +breakpoint_hour +
		// " breakpoint_minute=" + breakpoint_minute);
	}

	/** return the custom data String .week + time */
	public static String getDateWithCountry(Context context, TimeZone timezone, Date curDate, String week) {
		// DateFormat sdf = DateFormat.getDateInstance(DateFormat.MEDIUM,
		// context.getResources().getConfiguration().locale);
		char[] temp = android.text.format.DateFormat.getDateFormatOrder(context);
		String dateOrder = new String(temp);
		DateFormat sdf = android.text.format.DateFormat.getDateFormat(context);
		sdf.setTimeZone(timezone);
		String date = week + ", " + sdf.format(curDate);
		GlobalDef.MyLog.d(LOG_TAG, "date=" + date + " dateOrder=" + dateOrder);
		return date;
	}

	public static void setScreenCondition(Context context) {
		mIsScreenOrientationChange = false;
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		int width = dm.widthPixels;
		int height = dm.heightPixels;
		mIsPortrait = width < height;
		GlobalDef.MyLog.w(LOG_TAG, "isPortrait = " + mIsPortrait);
	}

	private static String getBlankNewsState() {
		String state = NO_NEWS;
		if (mNewsManager != null && mNewsManager.isNetWorkConnecting()) {
			state = LOADING;
		}
		if (!mIsDeviceNetworkConnect) {
			state = NO_NETWORK;
		}
		return state;
	}

//	private static int[] getNewsSource(int index) {
//		int[] sources = null;
//		switch (index) {
//		case 0:
//			sources = new int[] { R.id.news_first, R.id.news_rss_icon_first, R.id.news_time_first, 
//					R.id.newstitle_first, R.id.newscontent_first, R.id.news_loading_first };
//			break;
//		case 1:
//			sources = new int[] { R.id.news_second, R.id.news_rss_icon_second, R.id.news_time_second, 
//					R.id.newstitle_second, R.id.newscontent_second, R.id.news_loading_second };
//			break;
//		case 2:
//			sources = new int[] { R.id.news_third, R.id.news_rss_icon_third, R.id.news_time_third, 
//					R.id.newstitle_third, R.id.newscontent_third, R.id.news_loading_third };
//			break;
//		default:
//			sources = new int[] { R.id.news_first, R.id.news_rss_icon_first, R.id.news_time_first, 
//					R.id.newstitle_first, R.id.newscontent_first, R.id.news_loading_first };
//		}
//		return sources;
//	}

	/** Set the news part. */
//	private static void setWidgetNews(Context context, RemoteViews views, int appWidgetId, String cityName, int layoutIndex, int requestNewsCount) {
//		GlobalDef.MyLog.i(LOG_TAG, "setWidgetNews");
//		if (context == null || views == null || mNewsManager == null) {
//			return;
//		}
//
//		GlobalDef.MyLog.d(LOG_TAG, "requestNewsCount=" + requestNewsCount);
//		if (0 == requestNewsCount) { // case 2
//			String mPackage = "com.motorola.mmsp.rssnews";
//			String mClass = ".NewsDetail";
//			Intent startIntent = new Intent();
//			Bundle b = new Bundle();
//			startIntent.setComponent(new ComponentName(mPackage, mPackage + mClass));
//			startIntent.setAction(Intent.ACTION_VIEW);// ���Ƚ���Ҫ��������ʾȨ�޴���
//			startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			if (cityName != null) {
//				b.putString(GlobalDef.KEY_CITY_NAME, cityName);
//			}
//			b.putInt(GlobalDef.KEY_WIDGET_ID, appWidgetId);
//			startIntent.putExtras(b);
//			PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, startIntent, PendingIntent.FLAG_CANCEL_CURRENT);
//
//			views.setOnClickPendingIntent(R.id.rss_icon, pendingIntent);
//			GlobalDef.MyLog.d(LOG_TAG, "not need to show news");
//			return;
//		}
//
//		ArrayList<NewsInfo> list = mNewsManager.getNewsList(appWidgetId, cityName, requestNewsCount);
//		int obtainNewsCount = 0;
//		if (list != null) {
//			obtainNewsCount = list.size();
//		}
//		obtainNewsCount = Math.min(obtainNewsCount, requestNewsCount);
//		int[] newsId = new int[obtainNewsCount];
//		setNewsTextParamters(context);
//		int i = 0;
//		for (i = 0; i < obtainNewsCount; i++) {
//			int[] sources = getNewsSource(i);// layout,icon,time,title,content,loading
//			newsId[i] = list.get(i).mNewsId;
//			String topic = list.get(i).mNewsTopic;
//			String title = list.get(i).mNewsTitle;
//			String content = list.get(i).mNewsContent;
//			String time = list.get(i).mNewsTime;
//			// GlobalDef.MyLog.d(LOG_TAG, "_id=" + newsId[i] + " topic=" + topic+
//			// " title=" + title);
//			int srcId = GlobalDef.getTopicIconId(topic, mNewsManager.getOrigin());
//			views.setViewVisibility(sources[1], View.VISIBLE);
//			views.setImageViewResource(sources[1], srcId);
//			if (time == null) {
//				time = "";
//			}
//			views.setTextViewText(sources[2], time);
//			String[] sTemp = createNewTitleAndContent(title, content);
//
//			// int start = 0;
//			// int end = sTemp[0].length();
//			// SpannableString word = new SpannableString(sTemp[0]);
//			// word.setSpan(new AbsoluteSizeSpan(18), start,
//			// end,Spannable.SPAN_INCLUSIVE_INCLUSIVE);
//			// word.setSpan(new StyleSpan(Typeface.BOLD), start, end,
//			// Spannable.SPAN_INCLUSIVE_INCLUSIVE);
//			// word.setSpan(new BackgroundColorSpan(0x700000ff), start, end,
//			// Spannable.SPAN_INCLUSIVE_INCLUSIVE);
//			// views.setCharSequence(sources[3], "setText", word);
//
//			views.setTextViewText(sources[3], sTemp[0]);
//			views.setTextViewText(sources[4], sTemp[1]);
//			views.setViewVisibility(sources[5], View.GONE);
//		}
//		while (i < requestNewsCount) {
//			int[] sources = getNewsSource(i);// layout,icon,time,title,content,loading
//			views.setViewVisibility(sources[1], View.GONE);
//			views.setTextViewText(sources[2], "");
//			views.setTextViewText(sources[3], "");
//			views.setTextViewText(sources[4], "");
//			views.setViewVisibility(sources[5], View.VISIBLE);
//			String state = getBlankNewsState();
//			GlobalDef.MyLog.d(LOG_TAG, "index=" + i + " state:" + state);
//			views.setTextViewText(sources[5], state);
//			i++;
//		}
//
//		Bundle bundle = null;
//		ArrayList<Integer> ids = new ArrayList<Integer>();
//		for (i = 0; i < requestNewsCount; i++) {
//			int[] sources = getNewsSource(i);
//			String mPackage = "com.motorola.mmsp.rssnews";
//			String mClass = ".NewsDetail";
//			Intent startIntent = new Intent();
//			startIntent.setComponent(new ComponentName(mPackage, mPackage + mClass));
//			startIntent.setAction(Intent.ACTION_VIEW);// ���Ƚ���Ҫ��������ʾȨ�޴���
//			startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			bundle = new Bundle();
//
//			if (i < obtainNewsCount) {
//				ids.clear();
//				ids.add(newsId[i]);
//				bundle.putIntegerArrayList(GlobalDef.KEY_NEWSID_LIST, ids);
//				if (list != null) {
//					int position = list.get(i).mNewsPosition;
//					bundle.putInt(GlobalDef.KEY_NEWS_SELECT_POS, position);
//					GlobalDef.MyLog.d(LOG_TAG, "position = " + position);
//				}
//			}
//			if (cityName != null) {
//				bundle.putString(GlobalDef.KEY_CITY_NAME, cityName);
//			}
//			bundle.putInt(GlobalDef.KEY_WIDGET_ID, appWidgetId);
//			startIntent.putExtras(bundle);//
//			PendingIntent pendingIntent = null;
//			pendingIntent = PendingIntent.getActivity(context, appWidgetId + sources[0], startIntent, PendingIntent.FLAG_CANCEL_CURRENT);
//			views.setOnClickPendingIntent(sources[0], pendingIntent);
//		}
//	}

	private static int mMaxLines;
	private static int mLineWidth_first;
	private static int mLineWidth_last;
	private static int mLineWidth_middle;
	private static TextPaint mTitlePaint = null;
	private static TextPaint mContentPaint = null;

//	public static void setNewsTextParamters(Context context) {
//		GlobalDef.MyLog.w("shiao", "setNewsTextParamters");
//		Resources res = context.getResources();
//		mMaxLines = res.getInteger(R.integer.max_lines);
//		mLineWidth_first = res.getDimensionPixelOffset(R.dimen.first_line_width);
//		mLineWidth_last = res.getDimensionPixelOffset(R.dimen.last_line_width);
//		mLineWidth_middle = res.getDimensionPixelOffset(R.dimen.middle_line_width);
//		GlobalDef.MyLog.d("shiao", "mMaxLines=" + mMaxLines);
//		GlobalDef.MyLog.d("shiao", "mLineWidth_first=" + mLineWidth_first);
//		GlobalDef.MyLog.d("shiao", "mLineWidth_middle=" + mLineWidth_middle);
//		GlobalDef.MyLog.d("shiao", "mLineWidth_last=" + mLineWidth_last);
//		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		// long start = System.currentTimeMillis();
//		LinearLayout layout_temp = (LinearLayout) inflater.inflate(R.layout.text_views, null);
//		// long used = System.currentTimeMillis()-start;
//		// GlobalDef. Log.d("shiao", "time used =" + used);
//		if (layout_temp != null) {
//			TextView tv = (TextView) layout_temp.findViewById(R.id.newstitle_first);
//			if (tv != null) {
//				mTitlePaint = tv.getPaint();
//			} else {
//				mTitlePaint = new TextPaint();
//			}
//			tv = (TextView) layout_temp.findViewById(R.id.newscontent_first);
//			if (tv != null) {
//				mContentPaint = tv.getPaint();
//			} else {
//				mContentPaint = new TextPaint();
//			}
//		} else {
//			mTitlePaint = new TextPaint();
//			mContentPaint = new TextPaint();
//		}
//	}

	/**
	 * line start form 0. return the max width of the specified line.
	 * */
	private static int getLineWidth(int line, int maxLines) {
		int lineWidth = 0;
		if (line == 0) {
			lineWidth = mLineWidth_first;
		} else if (line == maxLines - 1) {
			lineWidth = mLineWidth_last;
		} else {
			lineWidth = mLineWidth_middle;
		}
		return lineWidth;
	}

	/**
	 * Return the new title and content to achieve the effect of the text
	 * surrounding
	 */
	private static String[] createNewTitleAndContent(String title, String content) {
		if (title == null) {
			title = "";
		}
		if (content == null) {
			content = "";
		}

		TextPaint paint = mTitlePaint;
		int max_lines = mMaxLines;
		int lineWidth = 0;
		int index_start = 0;
		int index_end = 0;
		String text = title;
		StaticLayout layout;
		String newTitle = "";
		String newContent = "";
		boolean isTitleFinish = false;
		for (int i = 0; i < max_lines; i++) {
			// GlobalDef. Log.i("shiao","ascent="+ascent+" descent="+descent);
			lineWidth = getLineWidth(i, max_lines);
			// GlobalDef. Log.d("shiao","lineWidth="+lineWidth);
			layout = new StaticLayout(text, paint, lineWidth, Alignment.ALIGN_CENTER, 1, 0, true);
			index_start = layout.getLineStart(0);
			index_end = layout.getLineEnd(0);
			// GlobalDef. Log.d("shiao","text="+text);
			// GlobalDef.
			// Log.d("shiao","index_start="+index_start+" index_end="+index_end+" layout.getLineCount()="+layout.getLineCount());
			String lineText = text.substring(index_start, index_end);
			int lineTextWidth = (int) paint.measureText(lineText);
			GlobalDef.MyLog.d("shiao", "lineTextWidth=" + lineTextWidth + " print text :" + lineText);
			if (i == max_lines - 1 && layout.getLineCount() > 1) {
				String tail = "...";
				int tailWidth = (int) paint.measureText(tail);
				int offsetPels = Math.max(0, tailWidth - (mLineWidth_last - lineTextWidth));
				int offset = 0;
				if (offsetPels > 0) {
					String temp;
					for (int m = 1; m <= index_end; m++) {
						temp = lineText.substring(index_end - m, index_end);
						float wid = paint.measureText(temp);
						GlobalDef.MyLog.d("shiao", "temp=" + temp + " width=" + wid);
						if (wid >= offsetPels) {
							offset = m;
							break;
						}
					}
				}
				GlobalDef.MyLog.d("shiao", "tailWidth=" + tailWidth + " offset=" + offset);
				if (!isTitleFinish) {
					newTitle = newTitle + lineText.substring(0, index_end - offset) + tail;
				} else {
					newContent = newContent + lineText.substring(0, index_end - offset) + tail;
				}
			} else {
				if (!isTitleFinish) {
					if (layout.getLineCount() > 1) {
						newTitle = newTitle + lineText + "\n";
					} else {
						newTitle = newTitle + lineText;
					}
				} else {
					if (layout.getLineCount() > 1) {
						newContent = newContent + lineText + "\n";
					} else {
						newContent = newContent + lineText;
					}
				}
			}

			if (layout.getLineCount() > 1) {
				text = text.substring(index_end);
			} else if (!isTitleFinish) {// change to content
				isTitleFinish = true;
				text = content;
				paint = mContentPaint;
			} else {
				GlobalDef.MyLog.i("shiao", "finish title and content");
				break;
			}
		}
		return new String[] { newTitle, newContent };
	}
}
