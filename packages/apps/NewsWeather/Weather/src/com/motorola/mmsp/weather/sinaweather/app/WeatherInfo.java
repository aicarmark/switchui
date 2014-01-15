package com.motorola.mmsp.weather.sinaweather.app;

import java.io.Serializable;
import java.util.ArrayList;

public class WeatherInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8855018121741368901L;	
	public static final int DAY = 1;
	public static final int NIGHT = 2;

	public class Current implements Serializable{
		private static final long serialVersionUID = -6421777982736425368L;
		public String mCondition;
		public String mTemperatrue;
		public int mWeatherType;
		public String mRealFeel;
		public String mTimezone;
	}

	public class Forecast implements Serializable{
		private static final long serialVersionUID = -6849835582865680296L;
		public String mDay;
		public String mLow;
		public String mHigh;
		public String mCondition;
		public int mWeatherType;
		public String mDate;
		//add for external query
		public String mWindPower;
		public String mSunRise;
		public String mSunSet;
	}
	
	public class Infomation implements Serializable{
		private static final long serialVersionUID = 389832693032272329L;
		public String mCity;
		public String mCityId;
		public String mAdminArea;
		public String mDate;
		public String mTime;
		public String mUnit;
		public String mRequestCity;
		public String mRequestTime;
		public int mDayNight;
		public String mUrl;
	}
	
	public String mError = null;
	public Infomation mInfomation = new Infomation();
	public Current mCurrent = new Current();
	public ArrayList<Forecast> mForecasts = new ArrayList<Forecast>(10);
	
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
