package com.motorola.mmsp.weather.locationwidget.small;

import java.util.HashMap;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.motorola.mmsp.plugin.widget.PluginWidgetHostView;
import com.motorola.mmsp.plugin.widget.PluginWidgetModel;
import com.motorola.mmsp.weather.locationwidget.small.WeatherManager.WeatherInfo;

public class WeatherWidgetModel extends PluginWidgetModel
{
	private static final String TAG = "Weather";
	
	private SharedPreferences prefs;	
	private Context mPackageContext;
	
	private WeatherManager mWeatherManager;
	
	private static final int mPreferenceMode = Context.MODE_PRIVATE;
	
	private PowerManager mPowerManager;
	private int mOldUnit = 3;
	
	private ContentObserver mTemperatureUnitUpdateObserver;
	
	private static final int CHANGE_TEMPERATURE_UNIT = 1;
	private static final int CHANGE_TIME = 2;
	private static final int CHANGE_WEATHER_INFO = 3;	
	private static final int DISPLAY_WEATHER_ANIMATION = 4;	
	protected int mChangeType = -1;
	
	public static final String WIDGET_ID = "widget_id";	
	public static final String CITY_ID = "city_id";
	
	public WeatherWidgetModel(Context packageContext)
	{
		mWeatherManager = WeatherManager.getInstace(packageContext);
		mWeatherManager.setWeatherServiceEdition(packageContext);
		
		mPackageContext = packageContext;
		mPowerManager = (PowerManager) mPackageContext.getSystemService(Context.POWER_SERVICE);
		
		prefs = mPackageContext.getSharedPreferences(GlobalDef.CITYIDSHAREDPFE, mPreferenceMode);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(GlobalDef.BROADCAST_WEAHER_UPDATE);
		filter.addAction(GlobalDef.BROADCAST_DETAILVIEWEXIT);
		filter.addAction(GlobalDef.BROADCAST_CITYLIST_UPDITING);
		filter.addAction(GlobalDef.BROADCAST_CITYLIST_UPDATE_FINISH);
		filter.addAction(Intent.ACTION_TIME_CHANGED);
		filter.addAction(Intent.ACTION_DATE_CHANGED);
		filter.addAction(Intent.ACTION_TIME_TICK);
		filter.addAction(Intent.ACTION_BOOT_COMPLETED);
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction("com.motorola.mmsp.motohome.unlockfinished");
		mPackageContext.registerReceiver(this, filter);
		
		mTemperatureUnitUpdateObserver = new ContentObserver(new Handler())
		{
			@Override
			public void onChange(boolean selfChange)
			{
				super.onChange(selfChange);
				if(selfChange){
					return;
				}
				Log.d(TAG, "mTemperatureUnitUpdateObserver onChange");
				temperatureUnitChanged();
			}
		};
		mPackageContext.getContentResolver().registerContentObserver(Uri.parse("content://" + GlobalDef.AUTHORITY + "/setting/temperature_uint"),
				false, mTemperatureUnitUpdateObserver);
		
	}
	
	private void temperatureUnitChanged()
	{
		int unit = queryTemperatureUnit(mPackageContext);
		if (unit < 0)
		{
			unit = 0;
		}		
		
		if (unit != mOldUnit)
		{
			Log.d(TAG, "temperature Unit Changed");
			mOldUnit = unit;
			
			mChangeType = CHANGE_TEMPERATURE_UNIT;
			updateAllWidegt();
		}
	}
	
	private int queryTemperatureUnit(Context context)
	{
		int ret = -1;
		
		String selections = "settingName = ?";
		String[] selectionargs = new String[] { "temperature_uint" };
		Cursor c = null;
		try {
			c = GlobalDef.query(context, GlobalDef.uri_setting,
					selections, selectionargs, null);
	
			if (c != null && c.getCount() > 0)
			{
				if (c.moveToFirst())
				{
					ret = c.getInt(c.getColumnIndex("settingValue"));
				}
			}
		}
        catch(Exception e){
        	e.printStackTrace();
        	Log.d(TAG, "queryTemperatureUnit exception");
        }
        finally{
	        // close Cursor
	        if( c != null )
	        c.close();
        }
		return ret;
	}
	
	@Override
	public void destoryModel()
	{
		mPackageContext.unregisterReceiver(this);
		mPackageContext.getContentResolver().unregisterContentObserver(mTemperatureUnitUpdateObserver);
	}

	@Override
	public void loadModel()
	{
		
	}
	
	@Override
	public void onReceive(Context context, Intent intent)
	{
		super.onReceive(context, intent);
		String action = intent.getAction();

		Log.i(TAG, "onReceive action = " + action);
		if (action.equals(GlobalDef.BROADCAST_WEAHER_UPDATE))
		{
			Log.i(TAG, "BROADCAST_WEAHER_UPDATE");
			mChangeType = CHANGE_WEATHER_INFO;
			updateAllWidegt();

		} else if (action.equals(GlobalDef.BROADCAST_DETAILVIEWEXIT))
		{
			Log.i(TAG, "BROADCAST_DETAILVIEWEXIT");

			mChangeType = CHANGE_WEATHER_INFO;
			onDetailViewExit(context, intent);

		} else if (action.equals(GlobalDef.BROADCAST_CITYLIST_UPDITING))
		{
			Log.i(TAG, "BROADCAST_CITYLIST_UPDITING");
			cityListUpdating(context);
		} else if (action.equals(GlobalDef.BROADCAST_CITYLIST_UPDATE_FINISH))
		{
			String type = intent.getStringExtra("type");
			Log.d(TAG, "city list update finished, update type is " + type);
			if(type.equals("rearrange")) {
				Log.d(TAG, "rearrange city list, no need update cityids and animation");
				return;
			}
			Log.i(TAG,
					"WeatherNewsWidgetModel onReceive BROADCAST_CITYLIST_UPDATE_FINISH");
			Cursor cursor = null;
			try {
				cursor = GlobalDef.query(context, GlobalDef.uri_citylist,
						null, null, null);
	
				if(cursor != null)
				{
					int count = cursor.getCount();
					if (count == 0)
					{
						clearData();
					} else if (count == 1)
					{
						Log.d(TAG, "onReceive city list count = 1");
						cursor.moveToFirst();
						String firstCityId = cursor.getString(cursor.getColumnIndex("cityId"));
						
						ContentResolver resolver = mPackageContext.getContentResolver();
						HashMap<Integer, String> widgetCityMap = allWidgetCityMap();
						for (int id : widgetCityMap.keySet())
						{
							ContentValues values = new ContentValues();
							values.put(WIDGET_ID, id);
							values.put(CITY_ID, firstCityId);

							resolver.update(GlobalDef.uri_widgetCity, values, 
									WIDGET_ID + "=?", new String[]{String.valueOf(id)});
						}			
					} else
					{
						cursor.moveToFirst();
						String firstCityId = cursor.getString(cursor.getColumnIndex("cityId"));
						
						HashMap<Integer, String> widgetCityMap = allWidgetCityMap();	
						ContentResolver resolver = mPackageContext.getContentResolver();
						for (Integer id : widgetCityMap.keySet())
						{
							String cityId = widgetCityMap.get(id);
							if(!findCityIdFromCursor(cursor, cityId))
							{
								ContentValues values = new ContentValues();
								values.put(WIDGET_ID, id);
								values.put(CITY_ID, firstCityId);
								
								Log.i(TAG, "onReceive update widegtid = " + id + "cityid = " + firstCityId);
								resolver.update(GlobalDef.uri_widgetCity, values, 
										WIDGET_ID + "=?", new String[]{String.valueOf(id)});
							}
						}
					}
				
				}
			}
			catch(Exception e){
				e.printStackTrace();
			}
			finally{
				if (cursor != null){
					cursor.close();
				}
			}
			mChangeType = CHANGE_WEATHER_INFO;
			updateAllWidegt();

		} else if (action.equals(Intent.ACTION_TIME_CHANGED)
				|| action.equals(Intent.ACTION_TIME_TICK)
				|| action.equals(Intent.ACTION_DATE_CHANGED))
		{
			if (mPowerManager.isScreenOn())
			{
				Log.d("sun", "receive CHANGE_TIME");
				mChangeType = CHANGE_TIME;
				updateAllWidegt();
			}
		}else if(action.equals(Intent.ACTION_SCREEN_ON))
		{
			mChangeType = CHANGE_TIME;
			updateAllWidegt();
		}
		else if(action.equals("com.motorola.mmsp.motohome.unlockfinished"))
		{
			mChangeType = DISPLAY_WEATHER_ANIMATION;
			updateAllWidegt();
		}

	}
	
	private void clearData()
	{
		ContentResolver resolver = mPackageContext.getContentResolver();
		HashMap<Integer, String> widgetCityMap = allWidgetCityMap();
		
		for (int id : widgetCityMap.keySet())
		{
			String nil = null;
			ContentValues values = new ContentValues();
			values.put(WIDGET_ID, id);
			values.put(CITY_ID, nil);
			resolver.update(GlobalDef.uri_widgetCity, values, 
					WIDGET_ID + "=?", new String[]{String.valueOf(id)});
			
			System.out.println("clear data widgetid = " + id);
		}				
	}
	public void updateAllWidegt()
	{
		Log.d(TAG, "updateAllWidegt");
		HashMap<Integer, String> widgetCityMap = allWidgetCityMap();
		Set<Integer> widgetIds = widgetCityMap.keySet();
		
		for (Integer widgetId : widgetIds)
		{
			String cityId = widgetCityMap.get(widgetId);
			
			Log.i(TAG, "update widget widgetId = " + widgetId + ", cityId = " + cityId);
			
			this.notifyUpdateView(widgetId);
		}
	}
	
		/**
		 * When select a city in the weather detail window,this function will
		 * implement.
		 */
	private void onDetailViewExit(Context context, Intent intent)
	{
		Bundle bundle = intent.getExtras();
		int widgetId = bundle.getInt("widgetId");
		String cityId = bundle.getString("cityId");

		Log.i(TAG, "weather detail exit widgetId = " + widgetId + ", cityId = " 
				+ cityId);

		if (TextUtils.isEmpty(cityId))
		{
			return;
		}
			
		ContentResolver resolver = mPackageContext.getContentResolver();

		ContentValues values = new ContentValues();
		values.put(WIDGET_ID, widgetId);
		values.put(CITY_ID, cityId);
		
		resolver.update(GlobalDef.uri_widgetCity, values, 
				WIDGET_ID + "=?", new String[]{String.valueOf(widgetId)});
		
		this.notifyUpdateView(widgetId);
	}
		
		/**
		 * If the cityId is using by other widget,it will return true ,else return
		 * false.
		 */
		private boolean isCityIdExist(String cityId, SharedPreferences preferences, int[] appWidgetIds) {
			if (cityId == null || preferences == null || appWidgetIds == null)
				return false;
			boolean flag = false;
			int n = appWidgetIds.length;
			for (int i = 0; i < n; i++) {
				int widgetId = appWidgetIds[i];
				String cityIdExist = preferences.getString(GlobalDef.WIDGET_CITYID + widgetId, GlobalDef.INVALIDCITYID);
				if (cityId.equals(cityIdExist)) {
					flag = true;
					break;
				}
			}
			return flag;
		}
		
	
	private boolean findCityIdFromCursor(Cursor cursor, String cityId)
	{

		boolean ret = false;
		Log.d(TAG, "WeatherWidgetModel findCityIdFromCursor " + cityId + ", cursor size = " + cursor.getCount());
		if(cityId == null){
			return ret;
		}
		if(cursor != null)
		{
			if(cursor.moveToFirst())
			{
				do
				{
					String city_id = cursor.getString(cursor.getColumnIndex("cityId"));
					Log.d(TAG, "city_id" + city_id);
					if(cityId.equals(city_id))
					{
						ret = true;
						break;
					}
				}
				while(cursor.moveToNext());
			}
		}
		Log.d(TAG, "WeatherWidgetModel findCityIdFromCursor return " + ret);
		return ret;
	
	}
		
	
	public void update(WeatherWidgetLayout layout)
	{
		int widgetId = ((PluginWidgetHostView) layout.getParent()).getPluginWidgetHostId();
		String cityId = Utils.getCityIdByWidgetId(mPackageContext, widgetId);
		switch (mChangeType)
		{
		case CHANGE_TEMPERATURE_UNIT:
			if (cityId == null)
			{
				layout.showWeatherNoCity(widgetId);
			} 
			else
			{
				Log.d(TAG, "update CHANGE_TEMPERATURE_UNIT");
				WeatherInfo info = mWeatherManager.getInfoWithCityId(
						cityId, prefs);
				if (info != null){
					layout.updateTemperature(info, cityId);
				}
			}
			
			break;
		case CHANGE_TIME:
			layout.updateTime();
			break;
			
		case CHANGE_WEATHER_INFO:
			if (cityId == null)
			{
				layout.showWeatherNoCity(widgetId);
			} 
			else
			{
				WeatherInfo info = mWeatherManager.getInfoWithCityId(cityId, prefs);
				if (info != null){
					layout.showWeatherInfo(info, cityId, widgetId);
				}
			}			
			break;
			
		case DISPLAY_WEATHER_ANIMATION:
				layout.showWeatherAnimation(widgetId);
			break;
		}		
	}

	private void cityListUpdating(Context context)
	{

	}

	public void deleteWidget(int widgetId)
	{
		HashMap<Integer, String> widgetCityMap = allWidgetCityMap();
		Set<Integer> widgetIds = widgetCityMap.keySet();

		for (Integer id : widgetIds)
		{
			if(id == widgetId)
			{
				String cityId = widgetCityMap.get(id);
				
				Log.i(TAG, "remove widgetId = " + id + ", cityId = " + cityId);
				
				ContentResolver resolver = mPackageContext.getContentResolver();
				resolver.delete(GlobalDef.uri_widgetCity, 
						WIDGET_ID + "=?", new String[]{String.valueOf(widgetId)});
			}
		}		
	}
	
	public HashMap<Integer,String> allWidgetCityMap()
	{
		HashMap<Integer,String> widgetCityMap = new HashMap<Integer, String>();
		
		ContentResolver resolver = mPackageContext.getContentResolver();
		String[] colums = new String[]{WIDGET_ID, CITY_ID};
		Cursor cursor = null;
		try {
			cursor = resolver.query(GlobalDef.uri_widgetCity, colums,
					null, null, null);
			
			if(cursor != null)
			{
				while(cursor.moveToNext())
				{
					int widgetId = cursor.getInt(0);
					String cityId = cursor.getString(1);
					widgetCityMap.put(widgetId, cityId);
				}
				
			}
		}
		catch(Exception e){
			e.printStackTrace();
			Log.d(TAG, "allWidgetCityMap exception");
		}
		finally{
			if (cursor != null){
				cursor.close();
			}
		}

		return widgetCityMap;
	}
}
