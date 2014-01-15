package com.motorola.mmsp.weather.sinaweather.service;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;

public class WeatherDatabaseHelper extends SQLiteOpenHelper {
	static final String DATABASE_NAME = "Weather.db";
	static final int DATABASE_VERSION = 1;
	
	static final String TABLE_FORECAST = "forecast";
	static final String TABLE_CONDITIONS = "conditions";
	static final String TABLE_CITY = "city";
	static final String TABLE_SETTING = "setting";
	static final String TABLE_APPINFO = "appinfo";
	static final String TABLE_WIDGETCITY = "widget_city";
	
	static final String CITY_ID = "cityId";
	static final String CITY_INDEX = "cityIndex";
	static final String CITY_NAME = "cityName";
	static final String ADMINAREA = "adminArea";
	
	static final String DATE = "date";
	static final String TIME = "time";
	static final String UPDATE_TIME = "updateTime";
	static final String ISDAY = "isDay";
	
	static final String TEMPERATURE = "temperature";
	static final String UNIT = "unit"; 	
	static final String WEATHER_TYPE = "weatherType";
	static final String CONDITION = "condition";
	
	static final String URL = "url";
	static final String ERRORNO = "errorNo";
	static final String REALFEEL = "reelFeel";
	
	static final String DAY = "day";
	static final String DAY_INDEX = "dayIndex";
	static final String LOW = "low";
	static final String HIGH = "high";
	//add for external query service
	static final String WINDPOWER = "windPower";
	static final String SUNRISE = "sunrise";
	static final String SUNSET = "sunset";
	static final String LANGUAGE = "language";
	static final String ISEXTERNAL = "isExternal";
	static final String QUERYNAME = "queryName";
	
	static final String SETTINTNAME = "settingName";
	static final String SETTINGVALUE = "settingValue";
	static final String SETTING_TEMP_UNIT = "temperature_uint";
	static final String SETTING_UPDATE_FREQUENCE = "setting_update_frequency";
	static final String SETTING_DISCLAIMER = "disclaimer";
	
	public static final String WIDGET_ID = "widget_id";
	public static final String WIDGET_CITY_ID = "city_id";
	
	static final String TIMEZONEACCU = "timezoneAccu";
	
	static final String APPMARK = "appmark";
	
	static final int EXTERNAL_CITY_LIMIT = 100;
	static final int EXTERNAL_CITY_NAME_EXPIRED_PERIOD = 30; // 30 days
	
    static final String APP_MARK_ACCU = "accu";
    static final String APP_MARK_SINA = "sina";
    
    private static WeatherDatabaseHelper mInstance = null;
    
    private Context mContext = null;

    public static synchronized WeatherDatabaseHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new WeatherDatabaseHelper(context);
            mInstance.setContext(context);
        }
        return mInstance;
    }
	
    private void setContext(Context context) {
        mContext = context;
    }
    
    private void createForecastTables(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE " + TABLE_FORECAST + " (" +
        		 CITY_ID + " TEXT," + 
                 DAY_INDEX + " INTEGER," +
                 DAY + " TEXT," +
                 LOW + " INTEGER," +
                 HIGH + " INTEGER," +
                 CONDITION +" TEXT," +
                 UNIT + " INTEGER," +
                 WEATHER_TYPE + " INTEGER DEFAULT 0," +
                 DATE + " TEXT, " +
                 //add for external query service
                 WINDPOWER + " TEXT," + 
                 SUNRISE + " TEXT," +
                 SUNSET + " TEXT," +
                 LANGUAGE + " TEXT," +
                 ISEXTERNAL + " INTEGER" +
                 ");");
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	private void createConditionsTables(SQLiteDatabase db) {
	    try {
            db.execSQL("CREATE TABLE " + TABLE_CONDITIONS + " (" +
        		CITY_ID + " TEXT," +
        		CITY_NAME + " TEXT," +
        		ADMINAREA + " TEXT," +
        		DATE + " TEXT," +
        		TIME + " TEXT," +
        		UPDATE_TIME + " INTEGER," +
        		ISDAY + " INTEGER," +
        		TEMPERATURE + " INTEGER," +
        		UNIT + " INTEGER," +
        		WEATHER_TYPE + " INTEGER," +
        		CONDITION + " TEXT," + 
        		URL + " TEXT," + 
        		REALFEEL + " TEXT, " + 
        		ERRORNO + " INTEGER, " +         		
                LANGUAGE + " INTEGER," +
                ISEXTERNAL + " INTEGER," + 
                TIMEZONEACCU + " TEXT" + 
                ");");
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	private void createCityTables(SQLiteDatabase db) {
	    try {
		    db.execSQL("CREATE TABLE " + TABLE_CITY + " (" +
				QUERYNAME + " TEXT," + 
				CITY_ID + " TEXT," + 
				UPDATE_TIME + " INTEGER" + 
				");");
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	private void createSettingTables(SQLiteDatabase db) {
	    try {
		    db.execSQL("CREATE TABLE " + TABLE_SETTING + " (" +
				SETTINTNAME + " TEXT," + 
				SETTINGVALUE + " TEXT" + 
				");");
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	private void createAppInfoTables(SQLiteDatabase db) {
	    try {
		    db.execSQL("CREATE TABLE " + TABLE_APPINFO + " (" +
				APPMARK + " TEXT" + 
				");");
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	private void createWidgetCityTable(SQLiteDatabase db) {
		try {
			String sql = "CREATE TABLE " +
					TABLE_WIDGETCITY + " (" 
					+ WIDGET_ID + " integer, " 
					+ WIDGET_CITY_ID +" text);";
			db.execSQL(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private WeatherDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	

	
	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d("WeatherService", "WeatherDatabaseHelper oncreate, create tables");
		createConditionsTables(db);
		createForecastTables(db);
		createCityTables(db);
		createSettingTables(db);
		createWidgetCityTable(db);
		createAppInfoTables(db);
		initAppInfoTable(db);
		fetchCDAValues(mContext, db);
	}
	
	private void initAppInfoTable(SQLiteDatabase db) {
	    try {
		    db.execSQL("insert into " + TABLE_APPINFO + " values (" + "'" + APP_MARK_SINA + "'" + ");");
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
		
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}
	
	public synchronized boolean isSettingIntialized() {
	        Log.d("WeatherService", "============= isSettingIntialized begin");
	        boolean isInited = false;

	        ContentResolver cr = mContext.getContentResolver();
	        Cursor c = null;
	        try {
	            c = cr.query(Uri.parse("content://" + WeatherServiceConstant.AUTHORITY + "/setting"), null, SETTINTNAME + " = ?", new String[]{SETTING_TEMP_UNIT}, null);
	            if (c != null && c.getCount() > 0) {
	                isInited = true;
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        } finally {
	            if (c != null) {
	                c.close();
	            }
	        }
	        Log.d("WeatherService", "============= isSettingIntialized end, isInited = " + isInited);
	        return isInited;
	}
	
    public void fetchCDAValues(Context context, SQLiteDatabase database) {
        if (!isSettingIntialized()) {
            String xmlstr = null;
            boolean bUnit = false;
            boolean bFrequency = false;
            boolean bDisclaimer = false;

            TypedValue isCDA = new TypedValue();
            TypedValue contentCDA = new TypedValue();
            Resources r = context.getResources();
            try {        
                r.getValue("@MOTOFLEX@isCDAValid", isCDA, false);
                if (isCDA.coerceToString().toString().equalsIgnoreCase("true")) {
                	Log.v("WeatherService", "@@@@@@@@@@@@@ isCDA.coerceToString() is true @@@@@@@@@@@@@");
                    r.getValue("@MOTOFLEX@getMotoWeatherSetting", contentCDA, false);
                    xmlstr = contentCDA.coerceToString().toString();
                } else {
                	Log.v("WeatherService", "@@@@@@@@@@@@@ isCDA.coerceToString() is false @@@@@@@@@@@@@");
                }
            } catch (Exception e) {
            	Log.v("WeatherService", "@@@@@@@@@@@@@ exception occurs !!!!!!!!!!!!! @@@@@@@@@@@@@");
                e.printStackTrace();
            }
            Element root = null;
            DocumentBuilderFactory dbf = null;
            DocumentBuilder db = null;

            String iniTempUnitSQL  = "insert into " + TABLE_SETTING + " (" + SETTINTNAME + "," + SETTINGVALUE + ")" + " values (" + "'" + SETTING_TEMP_UNIT + "'" + ", " + "'" + "0" + "'" + ");";
            String initFreqSQL     = "insert into " + TABLE_SETTING + " (" + SETTINTNAME + "," + SETTINGVALUE + ")" + " values (" + "'" + SETTING_UPDATE_FREQUENCE + "'" + ", " + "'" + "0" + "'" + ");";
            String initDisclaimSQL = "insert into " + TABLE_SETTING + " (" + SETTINTNAME + "," + SETTINGVALUE + ")" + " values (" + "'" + SETTING_DISCLAIMER + "'" + ", " + "'" + "true" + "'" + ");";
            if ((null != xmlstr) && (!"".equals(xmlstr))) {
                Log.v("WeatherService", "fetchCDAValues: " + xmlstr);
                try {
                    dbf = DocumentBuilderFactory.newInstance();
                    db = dbf.newDocumentBuilder();
                    Document xmldoc = db.parse(new InputSource(new StringReader(xmlstr)));
                    root = xmldoc.getDocumentElement();
                    NodeList list = root.getChildNodes();
                    if (list != null) {
                        int len = list.getLength();
                        for (int i = 0; i < len; i++) {
                            Node currentNode = list.item(i);
                            String name = currentNode.getNodeName();
                            String value = currentNode.getTextContent();
                            if (value != null && name != null && name.equals("temperature-unit")) {
                                bUnit = true;
                                Log.d("WeatherService", "value is " + value);
                                value = reHandleTemUnitFlex(value);
                                Log.v("WeatherService", "@@@@@@@@@@@@@ init SETTING_TEMP_UNIT with flex value @@@@@@@@@@@@@" + value);
                                database.execSQL("insert into " + TABLE_SETTING + " (" + SETTINTNAME + "," + SETTINGVALUE + ")" + " values (" + "'" + SETTING_TEMP_UNIT + "'" + ", " + "'" + value + "'" + ");");
                                //DatabaseHelper.writeSetting(context, SETTING_TEMP_UNIT, value);
                            } else if (value != null && name != null && name.equals("weather-update-frequency")) {
                                bFrequency = true;
                                Log.d("WeatherService", "value is " + value);
                                value = reHandleFrequencyFlex(value);
                                Log.v("WeatherService", "@@@@@@@@@@@@@ init SETTING_UPDATE_FREQUENCE with flex value @@@@@@@@@@@@@" + value);
                                database.execSQL("insert into " + TABLE_SETTING + " (" + SETTINTNAME + "," + SETTINGVALUE + ")" + " values (" + "'" + SETTING_UPDATE_FREQUENCE + "'" + ", " + "'" + value + "'" + ");");
                                //DatabaseHelper.writeSetting(context, SETTING_UPDATE_FREQUENCE, value);
                            } else if (value != null && name != null && name.equals("weather-disclaimer")) {
                                bDisclaimer = true;
                                Log.d("WeatherService", "value is " + value);
                                value = reHandleDisclaimerFlex(value);
                                Log.v("WeatherService", "@@@@@@@@@@@@@ init SETTING_DISCLAIMER with flex value @@@@@@@@@@@@@" + value);
                                database.execSQL("insert into " + TABLE_SETTING + " (" + SETTINTNAME + "," + SETTINGVALUE + ")" + " values (" + "'" + SETTING_DISCLAIMER + "'" + ", " + "'" + value + "'" + ");");
                                //DatabaseHelper.writeSetting(context, SETTING_DISCLAIMER, value);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
            	Log.v("WeatherService", "@@@@@@@@@@@@@ get nothing from flex file @@@@@@@@@@@@@");
            }
            
            try {
                if (!bUnit) {
                    Log.v("WeatherService", "@@@@@@@@@@@@@ init SETTING_TEMP_UNIT @@@@@@@@@@@@@");
                    database.execSQL(iniTempUnitSQL);
                    // DatabaseHelper.writeSetting(context, SETTING_TEMP_UNIT,
                    // "0");
                }
                if (!bFrequency) {
                    Log.v("WeatherService", "@@@@@@@@@@@@@ init SETTING_UPDATE_FREQUENCE @@@@@@@@@@@@@");
                    database.execSQL(initFreqSQL);
                }
                if (!bDisclaimer) {
                    Log.v("WeatherService", "@@@@@@@@@@@@@ init SETTING_DISCLAIMER @@@@@@@@@@@@@");
                    database.execSQL(initDisclaimSQL);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }
    
    private String reHandleTemUnitFlex(String values) {
    	int value = 0;
    	try {
    		value = Integer.parseInt(values);
    	} catch (Exception e) {
			e.printStackTrace();
		}
    	if(value == 0 || value == 1) {
    		return values;
    	} else {
    		return String.valueOf(0);
    	}
    }
    
    private String reHandleFrequencyFlex(String values) {
    	int value = 0;
    	try {
    		value = Integer.parseInt(values);
    	} catch (Exception e) {
			e.printStackTrace();
		}
    	if(value == 0 || value == 1 || value == 3 || value == 6 || value == 12 || value == 24) {
    		return values;
    	} else {
    		return String.valueOf(0);
    	}
    }
    
    private String reHandleDisclaimerFlex(String values) {
    	if(values.equals("true") || values.equals("false")) {
    		return values;
    	} else {
    		return "true";
    	}
    }
	
	public static boolean isTableExisted(Context context, String tableName) {
		Log.d("WeatherService", "WeatherDatabaseHelper isTableExisted, table name = " + tableName);
		boolean existed = false;
		if (tableName != null && !tableName.equals("")) {
			SQLiteDatabase db = null;
			Cursor cursor = null;
			try {
				db = getInstance(context).getReadableDatabase();
				String sql = "select count(*) from sqlite_master where type ='table' and name ='" + tableName.trim() + "' ";
				cursor = db.rawQuery(sql, null);
				cursor.moveToFirst();
				if (cursor.moveToNext()) {
					int count = cursor.getInt(0);
					if (count > 0) {
						existed = true;
					}
				}

			} catch (Exception e) {
				// TODO: handle exception
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
		}
		return existed;
	}

}
