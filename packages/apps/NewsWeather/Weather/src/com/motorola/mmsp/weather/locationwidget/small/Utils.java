package com.motorola.mmsp.weather.locationwidget.small;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.motorola.mmsp.weather.sinaweather.app.PersistenceData;
import com.motorola.mmsp.weather.sinaweather.service.WeatherDatabaseHelper;
import com.motorola.mmsp.weather.sinaweather.service.WeatherServiceConstant;

public class Utils
{
	public static String nextCity(Context context, String currentCity)
	{
		ArrayList<String> cityIds = PersistenceData.getCityids(context);
		int size = cityIds.size();
		int pos = 0;
		for (; pos < size; pos++)
		{
			if(currentCity.equals(cityIds.get(pos)))
				break;
		}
		
		String ret = null;
		if(pos == size - 1)
		{
			//conot found
			ret = cityIds.get(0);
		} else
		{
			ret = cityIds.get(pos + 1);
		}
		return ret;
	}
	
	public static String previousCity(Context context, String currentCity)
	{
		ArrayList<String> cityIds = PersistenceData.getCityids(context);
		int size = cityIds.size();
		int pos = 0;
		for (; pos < size; pos++)
		{
			if(currentCity.equals(cityIds.get(pos)))
				break;
		}
		String ret = null;
		if(pos == 0)
		{
			ret = cityIds.get(size - 1);
		} else
		{
			ret = cityIds.get(pos - 1);
		}
		 
		return ret;
	}
	
	// judge whether this widget is associated with a city
	public static String getCityIdByWidgetId(Context context, int widgetId)
	{
		Cursor cursor = null;
		try {
			String[] projection = new String[] { WeatherDatabaseHelper.WIDGET_CITY_ID};
			String selection = WeatherDatabaseHelper.WIDGET_ID + "=?";
			String[] selectionArgs = new String[] { String.valueOf(widgetId) };
			cursor = context.getContentResolver().query(GlobalDef.uri_widgetCity,
					projection, selection, selectionArgs, null);
			
			if(cursor != null && cursor.getCount() > 0)
			{
				if(cursor.moveToFirst())
				{
					String ret = cursor.getString(0);
					return ret;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(cursor != null) {
				cursor.close();
			}
		}
		
		return null;
	}
	
	public static String getLastWidgetCityId(Context context)
	{
		Cursor cursor = null;
		try {
			String[] projection = new String[] { WeatherDatabaseHelper.WIDGET_CITY_ID };
			String sortOrder = WeatherDatabaseHelper.WIDGET_ID;
			cursor = context.getContentResolver().query(GlobalDef.uri_widgetCity,
					projection, null, null, sortOrder);

			if(cursor != null && cursor.getCount() > 0)
			{
				if(cursor.moveToLast())
				{
					return cursor.getString(0);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(cursor != null) {
				cursor.close();
			}
		}		
		return null;
	}
	
	public static String getNextCityId(Cursor cursor, String cityId)
	{
		ArrayList<String> cityIdList = new ArrayList<String>();
		if(cursor != null)
		{
			while(cursor.moveToNext())
			{
				cityIdList.add(cursor.getString(cursor.getColumnIndex("cityId")));
			}
		}
		
		int size = cityIdList.size();
		if(size == 0)
		{
			return null;
		}
		
		if(cityId == null)
		{
			return cityIdList.get(0);
		}
		
		int pos = 0;
		for (; pos < size; pos++)
		{
			String id = cityIdList.get(pos);
			if(cityId.equals(id))
				break;
		}
		
		String ret = null;
		if(pos == size - 1)
		{
			//can not found
			ret = cityIdList.get(0);
		} else
		{
			ret = cityIdList.get(pos + 1);
		}
		return ret;
	}
}
