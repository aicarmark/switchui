package com.motorola.mmsp.weather.sinaweather.service;

public class WeatherServiceConstant {
	public static final String AUTHORITY = "com.motorola.mmsp.weather.service.WeatherProvider";
	public static final String ACTION_SEARCH_CITY_REQUEST = "com.motorola.mmsp.weather.search.city.request";
	public static final String ACTION_SEARCH_CITY_RESPONSE = "com.motorola.mmsp.weather.search.city.response";
	public static final String SEARCH_CITY_REQUEST_NAME = "name";
	public static final String SEARCH_CITY_RESPONSE_DATA = "response_data";
	
	public static final String ACTION_CITIES_ADD_REQUEST = "com.motorola.mmsp.weather.cities.add.request";
	public static final String ACTION_CITIES_DELETE_REQUEST = "com.motorola.mmsp.weather.cities.delete.request";
	public static final String CITY_NAME = "city";
	public static final String CITY_ID = "cityId";
	public static final String ACTION_CITIES_UPDATED_RESPONSE = "com.motorola.mmsp.weather.cities.updated.response";
	public static final String ACTION_CITIES_REARRANGED = "com.motorola.mmsp.weather.cities.rearranged";
	
	public static final String ACTION_WEATHER_UPDATE_REQUEST = "com.motorola.mmsp.weather.updating.request";
	public static final String ACTION_WEATHER_UPDATED_RESPONSE = "com.motorola.mmsp.weather.updated.response";
	
	public static final String ACTION_TEMP_UNIT = "com.motorola.mmsp.weather.temp.unit";
	public static final String ACTION_UPDATE_FREQUENCY = "com.motorola.mmsp.weather.update.frequency";
	
	public static final String ACTION_ENALBE_SERVER_UPDATE = "com.motorola.mmsp.weather.enalbe_server_update"; 
	public static final String ENABLE_SERVER = "enalbe_server";
	
	
	public static final String ACTION_WEATHER_REQUEST = "com.motorola.mmsp.weather.weather.request";
	
	public static final String ACTION_EXTERNAL_WEATHER_QUERY = "com.motorola.mmsp.weather.external.query";
	public static final String ACTION_EXTERNAL_WEATHER_QUERY_FAILURE = "com.motorola.mmsp.weather.external.query.failure";
	public static final String ACTION_EXTERNAL_WEATHER_UPDATE_ALARM = "com.motorola.mmsp.weather.external.update_alarm";
	
	public static final String ACTION_TIME_CHANGE = "com.motorola.mmsp.weather.time.changed";
	public static final String ACTION_WEATHER_TICK = "com.motorola.mmsp.weather.tick";
	
	public static final String ACTION_SETTING_INIT = "com.motorola.mmsp.weather.setting.init";
	public static final String ACTION_SETTING_UPDATED_RESPONSE = "com.motorola.mmsp.weather.setting.updated.response";
	
	public static final String ACTION_LOCATION_WIDGET_ENABLE  = "com.motorola.mmsp.locationwidget.enable";
	public static final String ACTION_LOCATION_WIDGET_DISABLE = "com.motorola.mmsp.locationwidget.disable";
	
	public static final String SETTING_UPDATE_FREQUENCY = "setting_update_frequency";
	public static final String SETTING_TEMP_UNIT = "temperature_uint";
	public static final String SETTING_AUTO_CITY = "setting_auto_city";
	public static final String SETTING_CITIYIDS = "weather_cityIds";
	
	
	public static final String ACTION_CITIES_UPDATING = "com.motorola.mmsp.weather.cities.updating";
	
	public static final long MILLISECOND_PER_HOUR = 3600000l;
	public static final int SECOND_PER_MINUTE = 60;
	public static final int MILLISECOND_PER_SECOND = 1000;
}
