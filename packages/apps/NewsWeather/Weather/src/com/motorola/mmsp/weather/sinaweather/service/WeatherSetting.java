package com.motorola.mmsp.weather.sinaweather.service;

import java.io.Serializable;

public class WeatherSetting implements Serializable{

	private static final long serialVersionUID = 8833018121741378901L;
	
	private String mUpdateFreq = null;
	private String mTempUnit = null;
	
	public String getmUpdateFreq() {
		return mUpdateFreq;
	}
	public void setmUpdateFreq(String mUpdateFreq) {
		this.mUpdateFreq = mUpdateFreq;
	}
	public String getmTempUnit() {
		return mTempUnit;
	}
	public void setmTempUnit(String mTempUnit) {
		this.mTempUnit = mTempUnit;
	}

}
