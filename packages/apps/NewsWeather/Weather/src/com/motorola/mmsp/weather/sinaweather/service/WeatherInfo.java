package com.motorola.mmsp.weather.sinaweather.service;

import java.io.Serializable;
import java.util.ArrayList;

public class WeatherInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8855018121741368901L;	
	public static final int DAY = 1;
	public static final int NIGHT = 2;
	
	public static final int WEATHER_DOWNLOAD_SUCCESSFUL = 1;
	public static final int WEATHER_DOWNLOAD_FAIL = -1;

	public class Current implements Serializable{
		private static final long serialVersionUID = -6421777982736425368L;
		public String condition;
		public String temperatrue;
		public String realFeel;
		public int weatherType;
		public String timezone;
	}

	public class Forecast implements Serializable{
		private static final long serialVersionUID = -6849835582865680296L;
		public String day;
		public String low;
		public String high;
		public String condition;
		public int weatherType;
		public String date;
		//add for external query
		public String windPower;
		public String sunrise;
		public String sunset;
	}
	
	public class Infomation implements Serializable{
		private static final long serialVersionUID = 389832693032272329L;
		public String city;
		public String cityId;
		public String adminArea;
		public String date;
		public String time;
		public String unit;
		public String requestCity;
		public String requestTime;
		public int dayNight;
		public String url;
	}
	
	public String error = null;
	public int errorNum = 0;
	public Infomation infomation = new Infomation();
	public Current current = new Current();
	public ArrayList<Forecast> forecasts = new ArrayList<Forecast>(10);
	
	/*
	public WeatherInfo() {
		for (int i = 0; i < ITEM_COUNT; i++) {
			forecasts.add(newForecast());
		}
	}*/
	
	public Forecast newForecast() {
		return new Forecast();
	}
}
