package com.motorola.mmsp.weather.sinaweather.service;

import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;

public class WeatherProvider extends ContentProvider{
	
	private static final int URI_TABLE_CONDITIONS = 1;
	private static final int URI_TABLE_FORECAST = 2;
	private static final int URI_TABLE_CITY = 3;
	private static final int URI_TABLE_FORECAST_CITY = 4;
	private static final int URI_TABLE_CONDITIONS_CITY = 5;
	private static final int URI_TABLE_SETTING = 6;
	private static final int URI_TABLE_CITYLIST = 7;
	private static final int URI_TABLE_APPINFO = 8;
	private static final int URI_TABLE_WIDGETCITY = 9;
	private static final int URI_TABLE_WIDGETCITY_ID = 10;

	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	
	static {
		URI_MATCHER.addURI(WeatherServiceConstant.AUTHORITY, "conditions", URI_TABLE_CONDITIONS);
		URI_MATCHER.addURI(WeatherServiceConstant.AUTHORITY, "forecast", URI_TABLE_FORECAST);
		URI_MATCHER.addURI(WeatherServiceConstant.AUTHORITY, "city", URI_TABLE_CITY);
		URI_MATCHER.addURI(WeatherServiceConstant.AUTHORITY, "forecast/*", URI_TABLE_FORECAST_CITY);
		URI_MATCHER.addURI(WeatherServiceConstant.AUTHORITY, "conditions/*", URI_TABLE_CONDITIONS_CITY);
		URI_MATCHER.addURI(WeatherServiceConstant.AUTHORITY, "setting", URI_TABLE_SETTING);
		URI_MATCHER.addURI(WeatherServiceConstant.AUTHORITY, "citylist", URI_TABLE_CITYLIST);
		URI_MATCHER.addURI(WeatherServiceConstant.AUTHORITY, "appinfo", URI_TABLE_APPINFO);
		URI_MATCHER.addURI(WeatherServiceConstant.AUTHORITY, "widget_city", URI_TABLE_WIDGETCITY);
		URI_MATCHER.addURI(WeatherServiceConstant.AUTHORITY, "widget_city/#", URI_TABLE_WIDGETCITY_ID);
	}
	
	private SQLiteOpenHelper mOpenHelper = null;
	private SQLiteDatabase mWritableDatabase = null;
	private SQLiteDatabase mReadableDatabase = null;
	
	@Override
	public boolean onCreate() {
		mOpenHelper = WeatherDatabaseHelper.getInstance(getContext());
		return (mOpenHelper != null);
	}
	
    public synchronized SQLiteDatabase getWritableDatabase() {
        if (mWritableDatabase ==  null) {
            mWritableDatabase = mOpenHelper.getWritableDatabase();
        }
        return mWritableDatabase;
    }
    
    public SQLiteDatabase getReadableDatabase() {
        if (mReadableDatabase == null) {
            mReadableDatabase = mOpenHelper.getReadableDatabase();
        }
        return mReadableDatabase;
    }
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs){
		Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider delete");
		SQLiteDatabase db = getWritableDatabase();
		int affectRows = 0;
		switch (URI_MATCHER.match(uri)) {
		case URI_TABLE_CONDITIONS:
		{
			affectRows = db.delete(WeatherDatabaseHelper.TABLE_CONDITIONS, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(DatabaseHelper.condtions, null);
            Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider notify conditions table changed");
			break;
		}
		case URI_TABLE_FORECAST:
		{
			affectRows = db.delete(WeatherDatabaseHelper.TABLE_FORECAST, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(DatabaseHelper.forecast, null);
            Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider notify forecast table changed");
			break;
		}
		case URI_TABLE_CITY:
		{
			affectRows = db.delete(WeatherDatabaseHelper.TABLE_CITY, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(DatabaseHelper.city, null);
            Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider notify city table changed");
			break;
		}
		case URI_TABLE_SETTING:
		{
			affectRows = db.delete(WeatherDatabaseHelper.TABLE_SETTING, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(DatabaseHelper.setting, null);
            Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider notify setting table changed");
			break;
		}
		case URI_TABLE_APPINFO:
		{
			affectRows = db.delete(WeatherDatabaseHelper.TABLE_APPINFO, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(DatabaseHelper.appinfo, null);
            Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider notify appinfo table changed");
			break;
		}
		case URI_TABLE_WIDGETCITY: 
		{
			affectRows = db.delete(WeatherDatabaseHelper.TABLE_WIDGETCITY, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(DatabaseHelper.widgetcity, null);
            Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider notify widgetcity table changed");
			break;
		}
		case URI_TABLE_WIDGETCITY_ID:
		{
			String id = uri.getPathSegments().get(1);
			affectRows = db.delete(WeatherDatabaseHelper.TABLE_WIDGETCITY, "id"
			+ "=" + id + (!TextUtils.isEmpty(selection) ? " AND (" + selection
			+ ")" : ""), selectionArgs);
			getContext().getContentResolver().notifyChange(DatabaseHelper.widgetcity, null);
            Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider notify widgetcity table changed");
		}
		default:
			break;
		}
		return affectRows;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider insert");
		SQLiteDatabase db = getWritableDatabase();
		Uri myUri = null;
		long rowId = 0;
		switch (URI_MATCHER.match(uri)) {
		case URI_TABLE_CONDITIONS:
		{
			Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider insert conditions table");
			
			//clean the data when the table is too big
			Cursor c = null;
			if (1 == Integer.parseInt((values.getAsString(WeatherDatabaseHelper.ISEXTERNAL)))) {
				try {
			        c = db.query(WeatherDatabaseHelper.TABLE_CONDITIONS, new String[]{WeatherDatabaseHelper.CITY_ID}, WeatherDatabaseHelper.ISEXTERNAL + "=?", 
			                	                                         new String[]{"1"}, null, null, WeatherDatabaseHelper.UPDATE_TIME + " ASC");

			        if (c != null && c.getCount() >= WeatherDatabaseHelper.EXTERNAL_CITY_LIMIT) {	
			            String sqlCleanForecast = "delete from " + WeatherDatabaseHelper.TABLE_FORECAST + 
					                          " where " + WeatherDatabaseHelper.CITY_ID + 
					                          " in (select " + WeatherDatabaseHelper.CITY_ID + 
					                          " from " + WeatherDatabaseHelper.TABLE_CONDITIONS + 
					                          " where " + WeatherDatabaseHelper.ISEXTERNAL + "=1 " + 
					                          " order by " + WeatherDatabaseHelper.UPDATE_TIME + " ASC" + 
					                          " limit " + (WeatherDatabaseHelper.EXTERNAL_CITY_LIMIT / 2) + ")";
			            Log.d("WeatherService", WeatherUtils.getTID() + "execute sqlCleanForecast " + sqlCleanForecast);
			            db.execSQL(sqlCleanForecast);
					
			            String sqlCleanConditions = "delete from " + WeatherDatabaseHelper.TABLE_CONDITIONS + 
		                                        " where " + WeatherDatabaseHelper.CITY_ID + 
		                                        " in (select " + WeatherDatabaseHelper.CITY_ID + 
		                                        " from " + WeatherDatabaseHelper.TABLE_CONDITIONS + 
		                                        " where " + WeatherDatabaseHelper.ISEXTERNAL + "=1 " + 
		                                        " order by " + WeatherDatabaseHelper.UPDATE_TIME + " ASC" + 
		                                        " limit " + (WeatherDatabaseHelper.EXTERNAL_CITY_LIMIT / 2) + ")";
			            Log.d("WeatherService", WeatherUtils.getTID() + "execute sqlCleanConditions " + sqlCleanConditions);
			            db.execSQL(sqlCleanConditions);
			        }
		        } catch (Exception e) {
		            e.printStackTrace();
		        } finally {
	                if (c!= null) {
	                    c.close();
	                }
		        }
			}
			
			rowId = db.insert(WeatherDatabaseHelper.TABLE_CONDITIONS, 
					WeatherDatabaseHelper.CITY_ID, values);
			break;
		}
		case URI_TABLE_FORECAST:
		{
			Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider insert forecast table");
			rowId = db.insert(WeatherDatabaseHelper.TABLE_FORECAST, 
					WeatherDatabaseHelper.CITY_ID, values);
			break;
		}
		case URI_TABLE_CITY:
		{
			Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider insert city table");
			
			//Clean the table when the table size is too big
			Cursor c = null;
			try {
			    c = db.query(WeatherDatabaseHelper.TABLE_CITY, null, null, null, null, null, null);
			    if (c != null && c.getCount() >= WeatherDatabaseHelper.EXTERNAL_CITY_LIMIT) {
			        c.moveToFirst();
			        String where = WeatherDatabaseHelper.QUERYNAME + " = ?";
			        for (int i = 0; i < (WeatherDatabaseHelper.EXTERNAL_CITY_LIMIT / 2); i++) {
			            db.delete(WeatherDatabaseHelper.TABLE_CITY, where, new String[] { c.getString(0) });
			            c.moveToNext();
			        }
			    }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (c!= null) {
                    c.close();
                }
            }
			rowId = db.insert(WeatherDatabaseHelper.TABLE_CITY, WeatherDatabaseHelper.CITY_ID, values);
			Log.d("WeatherService", WeatherUtils.getTID() + "after insert city table, rowId = " + rowId);

			break;
		}
		case URI_TABLE_SETTING:
		{
			Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider insert setting table");
			rowId = db.insert(WeatherDatabaseHelper.TABLE_SETTING, 
					WeatherDatabaseHelper.SETTINTNAME, values);
			Log.d("WeatherService", WeatherUtils.getTID() + "after insert setting table, rowId = " + rowId);
			Log.d("WeatherService", WeatherUtils.getTID() + "setting name = " + values.getAsString(WeatherDatabaseHelper.SETTINTNAME));
			if (rowId > 0 && values.getAsString(WeatherDatabaseHelper.SETTINTNAME).equals(WeatherServiceConstant.SETTING_UPDATE_FREQUENCY)) {
				getContext().getContentResolver().notifyChange(Uri.withAppendedPath(DatabaseHelper.setting, 
						                                       WeatherServiceConstant.SETTING_UPDATE_FREQUENCY), null);
				Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider notify update frequency changed");
			}
			if (rowId > 0 && values.getAsString(WeatherDatabaseHelper.SETTINTNAME).equals(WeatherServiceConstant.SETTING_AUTO_CITY)) {
				getContext().getContentResolver().notifyChange(Uri.withAppendedPath(DatabaseHelper.setting, WeatherServiceConstant.SETTING_AUTO_CITY), null);
				Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider notify auto city changed");
			}
			if (rowId > 0 ) {
				getContext().getContentResolver().notifyChange(Uri.parse("content://" + WeatherServiceConstant.AUTHORITY + "/setting"), null);
				Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider notify setting table changed");
			}
			if (rowId > 0 && values.getAsString(WeatherDatabaseHelper.SETTINTNAME).equals(WeatherServiceConstant.SETTING_CITIYIDS)) {
			    getContext().getContentResolver().notifyChange(Uri.withAppendedPath(DatabaseHelper.setting, WeatherServiceConstant.SETTING_CITIYIDS), null);
			    Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider notify city list changed, uri = content://" + WeatherServiceConstant.AUTHORITY + "/setting/weather_cityIds" );
		    }
			break;
		}
		case URI_TABLE_WIDGETCITY:
		{
			rowId = db.insert(WeatherDatabaseHelper.TABLE_WIDGETCITY, null, values);
			if (rowId > 0)
			{
				Uri itemUri = ContentUris.withAppendedId(DatabaseHelper.widgetcity, rowId);

				Log.d("WeatherService", "insert into WeatherWidget table");
				this.getContext().getContentResolver().notifyChange(itemUri, null);
			}
			break;
		}
			
			/*
		case URI_TABLE_CITIES_PRIVATE:
			db.beginTransaction();
			db.delete(WeatherDatabaseHelper.TABLE_CITIES, null, null);
			int count = values.size();
			for (int i = 0; i < count; i++) {
				ContentValues v = new ContentValues();
				v.put(WeatherDatabaseHelper.CITY_INDEX, i);
				v.put(WeatherDatabaseHelper.CITY_ID, values.getAsString(String.valueOf(i)));
				db.insert(WeatherDatabaseHelper.TABLE_CITIES, 
						WeatherDatabaseHelper.CITY_ID, v);
			}
			db.setTransactionSuccessful();
			db.endTransaction();			
			rowId = -1;
			break;
			*/
		case URI_TABLE_APPINFO:
		{
			Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider insert appinfo table");
			rowId = db.insert(WeatherDatabaseHelper.TABLE_APPINFO, 
					WeatherDatabaseHelper.APPMARK, values);
			break;
		}
		default:
			break;
		}
		if (rowId > 0) {
			myUri = ContentUris.withAppendedId(uri, rowId);
		}
		return myUri;
	}
	
	@Override
	public synchronized Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		SQLiteDatabase db = getReadableDatabase();
		Cursor cursor = null;
		switch (URI_MATCHER.match(uri)) {
		case URI_TABLE_CONDITIONS:
		{
			Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider query URI_TABLE_CONDITIONS");
			cursor = db.query(WeatherDatabaseHelper.TABLE_CONDITIONS, 
					projection, selection, selectionArgs, null, null, sortOrder);
			break;
		}
		case URI_TABLE_FORECAST:
		{
			Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider query URI_TABLE_FORECAST");
			cursor = db.query(WeatherDatabaseHelper.TABLE_FORECAST, 
					projection, selection, selectionArgs, null, null, sortOrder);
			break;
		}
		case URI_TABLE_FORECAST_CITY:
		{
			Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider query URI_TABLE_FORECAST_CITY");
			String queryName = uri.getLastPathSegment();
			String cityId = "";
			Cursor c = null;
			try {
				c = db.query(WeatherDatabaseHelper.TABLE_CITY, new String[]{"cityId, queryName"}, "queryName = ?", new String[]{queryName}, null, null, sortOrder);
	            if (c != null && c.getCount() > 0) {
	    			c.moveToFirst();
	    			cityId = c.getString(0);
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
			
    		if (selection == null) {
    			selection = "";
    		}
    		if (selection.length() > 0) {
    			selection += " and cityId = '" + cityId +"'";
    		} else {
    			selection += " cityId = '" + cityId +"'";
    		}
			cursor = db.query(WeatherDatabaseHelper.TABLE_FORECAST, 
					projection, selection, selectionArgs, null, null, sortOrder);
			break;
		}
		case URI_TABLE_CONDITIONS_CITY:
		{
			Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider query URI_TABLE_CONDITIONS_CITY");
			String queryName = uri.getLastPathSegment();
			String cityId = "";
			Cursor c = null;
			try {
				c = db.query(WeatherDatabaseHelper.TABLE_CITY, new String[]{"cityId, queryName"}, "queryName = ?", 
						            new String[]{queryName}, null, null, sortOrder);
	            if (c != null && c.getCount() > 0) {
	    			c.moveToFirst();
	    			cityId = c.getString(0);
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
			
    		if (selection == null) {
    			selection = "";
    		}
    		if (selection.length() > 0) {
    			selection += " and cityId = '" + cityId +"'";
    		} else {
    			selection += " cityId = '" + cityId +"'";
    		}
			cursor = db.query(WeatherDatabaseHelper.TABLE_CONDITIONS, projection, selection, selectionArgs, null, null, sortOrder);
			break;
		}
		case URI_TABLE_CITY:
		{
			Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider query URI_TABLE_CITY");
			cursor = db.query(WeatherDatabaseHelper.TABLE_CITY, projection, selection, selectionArgs, null, null, sortOrder);
			break;
		}
		case URI_TABLE_CITYLIST:
		{
			ArrayList<ArrayList> rows = new ArrayList<ArrayList>();
			
			String cityIdsStr = "";
			Cursor c = null;
			try {
				c = db.query(WeatherDatabaseHelper.TABLE_SETTING, 
						projection, WeatherDatabaseHelper.SETTINTNAME + " = ?", new String[]{"weather_cityIds"}, null, null, sortOrder);
				
				if (c != null && c.getCount() > 0) {
					c.moveToFirst();
					cityIdsStr = c.getString(c.getColumnIndex(WeatherDatabaseHelper.SETTINGVALUE));
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
			
			ArrayList<String> cityIds = getStringList(cityIdsStr);
			
			int count = cityIds != null ? cityIds.size() : 0;
			for (int i=0; i < count; i++) {
				ArrayList<String> row = new ArrayList<String>();
				row.add(String.valueOf(i));
				row.add(cityIds.get(i));
				rows.add(row);
			}
			cursor = new ArrayListCursor(new String[]{WeatherDatabaseHelper.CITY_INDEX, WeatherDatabaseHelper.CITY_ID}, rows);
			break;
		}
		case URI_TABLE_SETTING:
		{
			cursor = db.query(WeatherDatabaseHelper.TABLE_SETTING, 
					projection, selection, selectionArgs, null, null, sortOrder);
			break;
		}
		case URI_TABLE_APPINFO:
		{
			cursor = db.query(WeatherDatabaseHelper.TABLE_APPINFO, 
					projection, selection, selectionArgs, null, null, sortOrder);
			break;
		}
		case URI_TABLE_WIDGETCITY:
		{
			cursor = db.query(WeatherDatabaseHelper.TABLE_WIDGETCITY, projection, selection, selectionArgs, null, null, sortOrder);
			break;
		}			
		case URI_TABLE_WIDGETCITY_ID:
		{
			String id = uri.getPathSegments().get(1);
			cursor = db.query(WeatherDatabaseHelper.TABLE_WIDGETCITY, projection, "id"
			+ "=" + id + (!TextUtils.isEmpty(selection) ? " AND (" + selection
			+ ")" : ""), selectionArgs, null, null, sortOrder);
//			if(uri.getPathSegments().size() > 0) {
//				String id = uri.getPathSegments().get(1);
//				cursor = db.query(WeatherDatabaseHelper.TABLE_WIDGETCITY, projection, "id"
//				+ "=" + id + (!TextUtils.isEmpty(selection) ? " AND (" + selection
//				+ ")" : ""), selectionArgs, null, null, sortOrder);
//			}			
			break;
		}			
		default:
			break;
		}
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider update");
		SQLiteDatabase db = getWritableDatabase();
		int affectedRows = 0;
		switch (URI_MATCHER.match(uri)) {
		case URI_TABLE_CONDITIONS:
		{
			affectedRows = db.update(WeatherDatabaseHelper.TABLE_CONDITIONS, values, selection, selectionArgs);
			break;
		}
		case URI_TABLE_FORECAST:
		{
			affectedRows = db.update(WeatherDatabaseHelper.TABLE_FORECAST, values, selection, selectionArgs);
			break;
		}
		case URI_TABLE_CITY:
		{
			Log.d("WeatherService", WeatherUtils.getTID() + "update city table");
			affectedRows = db.update(WeatherDatabaseHelper.TABLE_CITY, values, selection, selectionArgs);
			Log.d("WeatherService", WeatherUtils.getTID() + "after update city table, affectedRows = " + affectedRows);

			break;
		}
		case URI_TABLE_SETTING:
		{
			Log.d("WeatherService", WeatherUtils.getTID() + "update setting table");
			affectedRows = db.update(WeatherDatabaseHelper.TABLE_SETTING, 
					values, selection, selectionArgs);
			Log.d("WeatherService", WeatherUtils.getTID() + "after update setting table, affectedRows = " + affectedRows 
					                     + " setting name = " + values.getAsString(WeatherDatabaseHelper.SETTINTNAME));
			
			if (affectedRows > 0 && values.getAsString(WeatherDatabaseHelper.SETTINTNAME).equals(WeatherServiceConstant.SETTING_UPDATE_FREQUENCY)) {
				getContext().getContentResolver().notifyChange(Uri.withAppendedPath(DatabaseHelper.setting, 
						                                       WeatherServiceConstant.SETTING_UPDATE_FREQUENCY), null);
				Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider notify update frequency changed");
			}
			if (affectedRows > 0 && values.getAsString(WeatherDatabaseHelper.SETTINTNAME).equals(WeatherServiceConstant.SETTING_AUTO_CITY)) {
				getContext().getContentResolver().notifyChange(Uri.withAppendedPath(DatabaseHelper.setting, 
						                                       WeatherServiceConstant.SETTING_AUTO_CITY), null);
				Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider notify auto city changed");
			}
			if (affectedRows > 0 && values.getAsString(WeatherDatabaseHelper.SETTINTNAME).equals(WeatherServiceConstant.SETTING_TEMP_UNIT)) {
				getContext().getContentResolver().notifyChange(Uri.withAppendedPath(DatabaseHelper.setting, 
						                                       WeatherServiceConstant.SETTING_TEMP_UNIT), null);
				Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider notify temperature_uint changed");
			}
			if (affectedRows > 0 ) {
				getContext().getContentResolver().notifyChange(Uri.parse("content://" + WeatherServiceConstant.AUTHORITY + "/setting"), null);
				Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider notify setting table changed");
			}
			if (affectedRows > 0 && values.getAsString(WeatherDatabaseHelper.SETTINTNAME).equals(WeatherServiceConstant.SETTING_CITIYIDS)) {
			    getContext().getContentResolver().notifyChange(Uri.withAppendedPath(DatabaseHelper.setting, WeatherServiceConstant.SETTING_CITIYIDS), null);
			    Log.d("WeatherService", WeatherUtils.getTID() + "WeatherProvider notify city list changed");
		    }
		    
			break;
		}
		case URI_TABLE_APPINFO:
		{
			affectedRows = db.update(WeatherDatabaseHelper.TABLE_APPINFO, 
					values, selection, selectionArgs);
			break;
		}
		case URI_TABLE_WIDGETCITY:
		{
			affectedRows = db.update(WeatherDatabaseHelper.TABLE_WIDGETCITY, values, selection,
					selectionArgs);
					break;
		}
		case URI_TABLE_WIDGETCITY_ID: 
		{
			String id = uri.getPathSegments().get(1);
			affectedRows = db.update(WeatherDatabaseHelper.TABLE_WIDGETCITY, values, "id"
			+ "=" + id + (!TextUtils.isEmpty(selection) ? " AND (" + selection
			+ ")" : ""), selectionArgs);
			break;
		}			
		default:
			break;
		}
		return affectedRows;
	}
	
	private static ArrayList<String> getStringList(String s) {
		ArrayList<String> list = new ArrayList<String>();
		if (s != null && !s.equals("") && !s.equals("[]")) {
			String ss = s.replace('[', '\0');
			ss = ss.replace(']', '\0');
			String items[] = ss.split(",");
			for (int i=0; i < items.length; i++) {
				list.add(items[i].trim());
			}
		}
		return list;
	}
}

//class WeatherDatabaseHelper extends SQLiteOpenHelper {
//	static final String DATABASE_NAME = "Weather.db";
//	static final int DATABASE_VERSION = 1;
//	
//	static final String TABLE_FORECAST = "forecast";
//	static final String TABLE_CONDITIONS = "conditions";
//	static final String TABLE_CITY = "city";
//	static final String TABLE_SETTING = "setting";
//	static final String TABLE_APPINFO = "appinfo";
//	static final String TABLE_WIDGETCITY = "widget_city";
//	
//	static final String CITY_ID = "cityId";
//	static final String CITY_INDEX = "cityIndex";
//	static final String CITY_NAME = "cityName";
//	static final String ADMINAREA = "adminArea";
//	
//	static final String DATE = "date";
//	static final String TIME = "time";
//	static final String UPDATE_TIME = "updateTime";
//	static final String ISDAY = "isDay";
//	
//	static final String TEMPERATURE = "temperature";
//	static final String UNIT = "unit"; 	
//	static final String WEATHER_TYPE = "weatherType";
//	static final String CONDITION = "condition";
//	
//	static final String URL = "url";
//	static final String ERRORNO = "errorNo";
//	static final String REALFEEL = "reelFeel";
//	
//	static final String DAY = "day";
//	static final String DAY_INDEX = "dayIndex";
//	static final String LOW = "low";
//	static final String HIGH = "high";
//	//add for external query service
//	static final String WINDPOWER = "windPower";
//	static final String SUNRISE = "sunrise";
//	static final String SUNSET = "sunset";
//	static final String LANGUAGE = "language";
//	static final String ISEXTERNAL = "isExternal";
//	static final String QUERYNAME = "queryName";
//	
//	static final String SETTINTNAME = "settingName";
//	static final String SETTINGVALUE = "settingValue";
//	static final String SETTING_TEMP_UNIT = "temperature_uint";
//	static final String SETTING_UPDATE_FREQUENCE = "setting_update_frequency";
//	static final String SETTING_DISCLAIMER = "disclaimer";
//	
//	public static final String WIDGET_ID = "widget_id";
//	public static final String WIDGET_CITY_ID = "city_id";
//	
//	static final String TIMEZONEACCU = "timezoneAccu";
//	
//	static final String APPMARK = "appmark";
//	
//	static final int EXTERNAL_CITY_LIMIT = 100;
//	static final int EXTERNAL_CITY_NAME_EXPIRED_PERIOD = 30; // 30 days
//	
//    static final String APP_MARK_ACCU = "accu";
//    static final String APP_MARK_SINA = "sina";
//    
//    private static WeatherDatabaseHelper mInstance = null;
//    
//    private Context mContext = null;
//
//    public static synchronized WeatherDatabaseHelper getInstance(Context context) {
//        if (mInstance == null) {
//            mInstance = new WeatherDatabaseHelper(context);
//            mInstance.setContext(context);
//        }
//        return mInstance;
//    }
//	
//    private void setContext(Context context) {
//        mContext = context;
//    }
//    
//    private void createForecastTables(SQLiteDatabase db) {
//        try {
//            db.execSQL("CREATE TABLE " + TABLE_FORECAST + " (" +
//        		 CITY_ID + " TEXT," + 
//                 DAY_INDEX + " INTEGER," +
//                 DAY + " TEXT," +
//                 LOW + " INTEGER," +
//                 HIGH + " INTEGER," +
//                 CONDITION +" TEXT," +
//                 UNIT + " INTEGER," +
//                 WEATHER_TYPE + " INTEGER DEFAULT 0," +
//                 DATE + " TEXT, " +
//                 //add for external query service
//                 WINDPOWER + " TEXT," + 
//                 SUNRISE + " TEXT," +
//                 SUNSET + " TEXT," +
//                 LANGUAGE + " TEXT," +
//                 ISEXTERNAL + " INTEGER" +
//                 ");");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//	}
//	
//	private void createConditionsTables(SQLiteDatabase db) {
//	    try {
//            db.execSQL("CREATE TABLE " + TABLE_CONDITIONS + " (" +
//        		CITY_ID + " TEXT," +
//        		CITY_NAME + " TEXT," +
//        		ADMINAREA + " TEXT," +
//        		DATE + " TEXT," +
//        		TIME + " TEXT," +
//        		UPDATE_TIME + " INTEGER," +
//        		ISDAY + " INTEGER," +
//        		TEMPERATURE + " INTEGER," +
//        		UNIT + " INTEGER," +
//        		WEATHER_TYPE + " INTEGER," +
//        		CONDITION + " TEXT," + 
//        		URL + " TEXT," + 
//        		REALFEEL + " TEXT, " + 
//        		ERRORNO + " INTEGER, " +         		
//                LANGUAGE + " INTEGER," +
//                ISEXTERNAL + " INTEGER," + 
//                TIMEZONEACCU + " TEXT" + 
//                ");");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//	}
//	
//	private void createCityTables(SQLiteDatabase db) {
//	    try {
//		    db.execSQL("CREATE TABLE " + TABLE_CITY + " (" +
//				QUERYNAME + " TEXT," + 
//				CITY_ID + " TEXT," + 
//				UPDATE_TIME + " INTEGER" + 
//				");");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//	}
//	
//	private void createSettingTables(SQLiteDatabase db) {
//	    try {
//		    db.execSQL("CREATE TABLE " + TABLE_SETTING + " (" +
//				SETTINTNAME + " TEXT," + 
//				SETTINGVALUE + " TEXT" + 
//				");");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//	}
//	
//	private void createAppInfoTables(SQLiteDatabase db) {
//	    try {
//		    db.execSQL("CREATE TABLE " + TABLE_APPINFO + " (" +
//				APPMARK + " TEXT" + 
//				");");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//	}
//	
//	private void createWidgetCityTable(SQLiteDatabase db) {
//		try {
//			String sql = "CREATE TABLE " +
//					TABLE_WIDGETCITY + " (" 
//					+ WIDGET_ID + " integer, " 
//					+ CITY_ID +" text);";
//			db.execSQL(sql);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//	}
//	
//	private WeatherDatabaseHelper(Context context) {
//		super(context, DATABASE_NAME, null, DATABASE_VERSION);
//	}
//	
//
//	
//	@Override
//	public void onCreate(SQLiteDatabase db) {
//		Log.d("WeatherService", "WeatherDatabaseHelper oncreate, create tables");
//		createConditionsTables(db);
//		createForecastTables(db);
//		createCityTables(db);
//		createSettingTables(db);
//		createWidgetCityTable(db);
//		createAppInfoTables(db);
//		initAppInfoTable(db);
//		fetchCDAValues(mContext, db);
//	}
//	
//	private void initAppInfoTable(SQLiteDatabase db) {
//	    try {
//		    db.execSQL("insert into " + TABLE_APPINFO + " values (" + "'" + APP_MARK_SINA + "'" + ");");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//	}
//		
//	@Override
//	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//		
//	}
//	
//	public synchronized boolean isSettingIntialized() {
//	        Log.d("WeatherService", "============= isSettingIntialized begin");
//	        boolean isInited = false;
//
//	        ContentResolver cr = mContext.getContentResolver();
//	        Cursor c = null;
//	        try {
//	            c = cr.query(Uri.parse("content://" + WeatherServiceConstant.AUTHORITY + "/setting"), null, SETTINTNAME + " = ?", new String[]{SETTING_TEMP_UNIT}, null);
//	            if (c != null && c.getCount() > 0) {
//	                isInited = true;
//	            }
//	        } catch (Exception e) {
//	            e.printStackTrace();
//	        } finally {
//	            if (c != null) {
//	                c.close();
//	            }
//	        }
//	        Log.d("WeatherService", "============= isSettingIntialized end, isInited = " + isInited);
//	        return isInited;
//	}
//	
//    public void fetchCDAValues(Context context, SQLiteDatabase database) {
//        if (!isSettingIntialized()) {
//            String xmlstr = null;
//            boolean bUnit = false;
//            boolean bFrequency = false;
//            boolean bDisclaimer = false;
//
//            TypedValue isCDA = new TypedValue();
//            TypedValue contentCDA = new TypedValue();
//            Resources r = context.getResources();
//            try {        
//                r.getValue("@MOTOFLEX@isCDAValid", isCDA, false);
//                if (isCDA.coerceToString().equals("true")) {
//                	Log.v("WeatherService", "@@@@@@@@@@@@@ isCDA.coerceToString() is true @@@@@@@@@@@@@");
//                    r.getValue("@MOTOFLEX@getMotoWeatherSetting", contentCDA, false);
//                    xmlstr = contentCDA.coerceToString().toString();
//                } else {
//                	Log.v("WeatherService", "@@@@@@@@@@@@@ isCDA.coerceToString() is false @@@@@@@@@@@@@");
//                }
//            } catch (Exception e) {
//            	Log.v("WeatherService", "@@@@@@@@@@@@@ exception occurs !!!!!!!!!!!!! @@@@@@@@@@@@@");
//                e.printStackTrace();
//            }
//            Element root = null;
//            DocumentBuilderFactory dbf = null;
//            DocumentBuilder db = null;
//
//            String iniTempUnitSQL  = "insert into " + TABLE_SETTING + " (" + SETTINTNAME + "," + SETTINGVALUE + ")" + " values (" + "'" + SETTING_TEMP_UNIT + "'" + ", " + "'" + "0" + "'" + ");";
//            String initFreqSQL     = "insert into " + TABLE_SETTING + " (" + SETTINTNAME + "," + SETTINGVALUE + ")" + " values (" + "'" + SETTING_UPDATE_FREQUENCE + "'" + ", " + "'" + "0" + "'" + ");";
//            String initDisclaimSQL = "insert into " + TABLE_SETTING + " (" + SETTINTNAME + "," + SETTINGVALUE + ")" + " values (" + "'" + SETTING_DISCLAIMER + "'" + ", " + "'" + "true" + "'" + ");";
//            if ((null != xmlstr) && (!"".equals(xmlstr))) {
//                Log.v("WeatherService", "fetchCDAValues: " + xmlstr);
//                try {
//                    dbf = DocumentBuilderFactory.newInstance();
//                    db = dbf.newDocumentBuilder();
//                    Document xmldoc = db.parse(new InputSource(new StringReader(xmlstr)));
//                    root = xmldoc.getDocumentElement();
//                    NodeList list = root.getChildNodes();
//                    if (list != null) {
//                        int len = list.getLength();
//                        for (int i = 0; i < len; i++) {
//                            Node currentNode = list.item(i);
//                            String name = currentNode.getNodeName();
//                            String value = currentNode.getTextContent();
//                            if (value != null && name != null && name.equals("temperature-unit")) {
//                                bUnit = true;
//                                Log.v("WeatherService", "@@@@@@@@@@@@@ init SETTING_TEMP_UNIT with flex value @@@@@@@@@@@@@");
//                                database.execSQL("insert into " + TABLE_SETTING + " (" + SETTINTNAME + "," + SETTINGVALUE + ")" + " values (" + "'" + SETTING_TEMP_UNIT + "'" + ", " + "'" + value + "'" + ");");
//                                //DatabaseHelper.writeSetting(context, SETTING_TEMP_UNIT, value);
//                            } else if (value != null && name != null && name.equals("weather-update-frequency")) {
//                                bFrequency = true;
//                                Log.v("WeatherService", "@@@@@@@@@@@@@ init SETTING_UPDATE_FREQUENCE with flex value @@@@@@@@@@@@@");
//                                database.execSQL("insert into " + TABLE_SETTING + " (" + SETTINTNAME + "," + SETTINGVALUE + ")" + " values (" + "'" + SETTING_UPDATE_FREQUENCE + "'" + ", " + "'" + value + "'" + ");");
//                                //DatabaseHelper.writeSetting(context, SETTING_UPDATE_FREQUENCE, value);
//                            } else if (value != null && name != null && name.equals("weather-disclaimer")) {
//                                bDisclaimer = true;
//                                Log.v("WeatherService", "@@@@@@@@@@@@@ init SETTING_DISCLAIMER with flex value @@@@@@@@@@@@@");
//                                database.execSQL("insert into " + TABLE_SETTING + " (" + SETTINTNAME + "," + SETTINGVALUE + ")" + " values (" + "'" + SETTING_DISCLAIMER + "'" + ", " + "'" + value + "'" + ");");
//                                //DatabaseHelper.writeSetting(context, SETTING_DISCLAIMER, value);
//                            }
//                        }
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            } else {
//            	Log.v("WeatherService", "@@@@@@@@@@@@@ get nothing from flex file @@@@@@@@@@@@@");
//            }
//            
//            try {
//                if (!bUnit) {
//                    Log.v("WeatherService", "@@@@@@@@@@@@@ init SETTING_TEMP_UNIT @@@@@@@@@@@@@");
//                    database.execSQL(iniTempUnitSQL);
//                    // DatabaseHelper.writeSetting(context, SETTING_TEMP_UNIT,
//                    // "0");
//                }
//                if (!bFrequency) {
//                    Log.v("WeatherService", "@@@@@@@@@@@@@ init SETTING_UPDATE_FREQUENCE @@@@@@@@@@@@@");
//                    database.execSQL(initFreqSQL);
//                }
//                if (!bDisclaimer) {
//                    Log.v("WeatherService", "@@@@@@@@@@@@@ init SETTING_DISCLAIMER @@@@@@@@@@@@@");
//                    database.execSQL(initDisclaimSQL);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        
//    }
//	
//	public static boolean isTableExisted(Context context, String tableName) {
//		Log.d("WeatherService", "WeatherDatabaseHelper isTableExisted, table name = " + tableName);
//		boolean existed = false;
//		if (tableName != null && !tableName.equals("")) {
//			SQLiteDatabase db = null;
//			Cursor cursor = null;
//			try {
//				db = getInstance(context).getReadableDatabase();
//				String sql = "select count(*) from sqlite_master where type ='table' and name ='" + tableName.trim() + "' ";
//				cursor = db.rawQuery(sql, null);
//				cursor.moveToFirst();
//				if (cursor.moveToNext()) {
//					int count = cursor.getInt(0);
//					if (count > 0) {
//						existed = true;
//					}
//				}
//
//			} catch (Exception e) {
//				// TODO: handle exception
//			} finally {
//				if (cursor != null) {
//					cursor.close();
//				}
//			}
//		}
//		return existed;
//	}
//}


