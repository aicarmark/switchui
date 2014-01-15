package com.motorola.mmsp.weather.sinaweather.app;

import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

import com.motorola.mmsp.sinaweather.app.WeatherScreenConfig;
import com.motorola.mmsp.weather.R;


public class WeatherSettingActivity extends PreferenceActivity{
	
	private String mTempValue = null;
	private String mFrequencyValue = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d("WeatherWidget", "WeatherSettingActivity->onCreate begin ");
		super.onCreate(savedInstanceState);

		int screenMode = -100;
		screenMode = WeatherScreenConfig.getWeatherScreenConfig(WeatherScreenConfig.SETTING_VIEW_INDEX);
		Log.d("WeatherWidget", "screenMode = " + screenMode );
		if (screenMode == WeatherScreenConfig.LANDSCAPE_ONLY) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else if (screenMode == WeatherScreenConfig.PORTRAIT_ONLY) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}


		addPreferencesFromResource(R.xml.weather_setting);
		initUI();
		Log.d("WeatherWidget", "WeatherSettingActivity->onCreate end ");
	}
	
	public void initUI() {
		Log.d("WeatherWidget", "WeatherSettingActivity->initUI begin ");
		final Map<String,String> tempUnitSummry = new HashMap<String,String>();		
		tempUnitSummry.put("0", getString(R.string.centigrade));
		tempUnitSummry.put("1", getString(R.string.fahrenheit));
		
//		SharedPreferences sp = PersistenceData.getSharedPreferences(this);
		
		final Preference dateTime = findPreference("date_and_time");
		if (dateTime != null) {
			dateTime.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent("android.settings.DATE_SETTINGS");
					intent.addCategory("android.intent.category.DEFAULT");
					startActivity(intent);
					return true;
				}
			});
		}
		
		final Preference city = findPreference("country_and_city");
		if (city != null) {
			city.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent(WeatherSettingActivity.this,CitiesActivity.class);
					startActivity(intent);
					return true;
				}
			});
		}
		
		final ListPreference temp = (ListPreference)findPreference(PersistenceData.SETTING_TEMP_UNIT);
		if (temp != null) {

//			((WeatherSettingPreference) temp).setSettingName(PersistenceData.SETTING_TEMP_UNIT);
			mTempValue = DatabaseHelper.readSetting(this, PersistenceData.SETTING_TEMP_UNIT);//sp.getString(temp.getKey(), "0");
			if (mTempValue.equals("")) {
				mTempValue = "0";
			}
/*			Log.d("WeatherWidget", "mTempValue = " + mTempValue);
			
			temp.setSummary(tempUnitSummry.get(mTempValue));
			CharSequence[] tempEntries = temp.getEntryValues();
			for (int i = 0; i < tempEntries.length; i++) {
				CharSequence value = tempEntries[i];
				if (value != null && value.equals(mTempValue)) {
					temp.setValueIndex(i);
					break;
				}
			}
*/			
			temp.setSummary(tempUnitSummry.get(mTempValue));
			temp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				public boolean onPreferenceChange(Preference preference, Object newValue) {				
                    Log.d("WeatherWidget", "SETTING_TEMP_UNIT onPreferenceChange");
					if (mTempValue != null && mTempValue.equals((String)newValue)) {

					} else {					
//						temp.setSummary(tempUnitSummry.get(newValue));
						Intent intent =  new Intent(Constant.ACTION_TEMP_UNIT);
			        	sendBroadcast(intent);
					}
					mTempValue = (String)newValue;
					return true;
				}
			});
		}
		
		
		final ListPreference frequency = (ListPreference)findPreference(PersistenceData.SETTING_UPDATE_FREQUENCE);
		if (frequency != null) {

//			((WeatherSettingPreference) frequency).setSettingName(PersistenceData.SETTING_UPDATE_FREQUENCE);
			mFrequencyValue = DatabaseHelper.readSetting(this, PersistenceData.SETTING_UPDATE_FREQUENCE);//sp.getString(frequency.getKey(), "0");
			if (mFrequencyValue.equals("")) {
				mFrequencyValue = "0";
			}
//			Log.d("WeatherWidget", "mFrequencyValue = " + mFrequencyValue);
			
			final CharSequence[] frequencyEntries = frequency.getEntries();
			/*
			for (int i = 0; i < frequencyEntries.length; i++) {
				frequencyEntries[i] = String.format(frequencyEntries[i].toString(), getString(R.string.hour));
			}
			frequency.setEntries(frequencyEntries);
			*/
			final CharSequence[] frequencyEntryValues = frequency.getEntryValues();	
/*
			for (int i = 0; i < frequencyEntryValues.length; i++) {
				CharSequence value = frequencyEntryValues[i];
				if (value != null && value.equals(mFrequencyValue)) {
					frequency.setValueIndex(i);
					frequency.setSummary(frequencyEntries[i]);
					break;
				}
			}
*/						
			for (int i = 0; i < frequencyEntryValues.length; i++) {
				CharSequence value = frequencyEntryValues[i];
				if (value != null && value.equals((String) mFrequencyValue)) {
					frequency.setSummary(frequencyEntries[i]);
					break;
				}
			}
			
			frequency.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				public boolean onPreferenceChange(Preference preference, Object newValue) {				
                    Log.d("WeatherWidget", "SETTING_UPDATE_FREQUENCE onPreferenceChange");
					if (mFrequencyValue != null && mFrequencyValue.equals((String)newValue)) {
						
					} else {
/*	
						for (int i = 0; i < frequencyEntryValues.length; i++) {
							CharSequence value = frequencyEntryValues[i];
							if (value != null && value.equals((String)newValue)) {
								frequency.setSummary(frequencyEntries[i]);
								break;
							}
						}
*/
						Intent intent =  new Intent(Constant.ACTION_UPDATE_FREQUENCY);
			        	startService(intent);
					}
					mFrequencyValue = (String)newValue;
					return true;
				}
			});
		}		
		Log.d("WeatherWidget", "WeatherSettingActivity->initUI end ");
		//CheckBoxPreference corousel = (CheckBoxPreference) findPreference(PersistenceData.SETTING_CAROUSEL);
		//Intent operator = new Intent(ACTION_SWTICH_PEROID);
		//startService(operator);
		
//		final Preference brandingInfo = findPreference("about");
//		if (brandingInfo != null) {
//			brandingInfo.setOnPreferenceClickListener(new OnPreferenceClickListener() {
//				public boolean onPreferenceClick(Preference preference) {
//					Intent intent = new Intent(WeatherSettingActivity.this,BrandingInfo.class);
//					startActivity(intent);
//					return true;
//				}
//			});
//		}			
	}
}



