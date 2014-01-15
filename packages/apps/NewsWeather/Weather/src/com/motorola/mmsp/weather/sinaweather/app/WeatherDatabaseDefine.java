package com.motorola.mmsp.weather.sinaweather.app;


class WeatherDatabaseDefine{
	
	static final String DATABASE_NAME = "Weather.db";
	static final int DATABASE_VERSION = 1;
	
	static final String TABLE_FORECAST = "forecast";
	static final String TABLE_CONDITIONS = "conditions";
	static final String TABLE_CITY = "city";
	static final String TABLE_SETTING = "setting";
	
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
}