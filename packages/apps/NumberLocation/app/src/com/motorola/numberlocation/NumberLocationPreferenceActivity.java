/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.motorola.numberlocation;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import com.motorola.numberlocation.R;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class NumberLocationPreferenceActivity extends PreferenceActivity
		implements OnSharedPreferenceChangeListener {



	private static final String TAG = "NumberLocationPreferenceActivity";
	
	CheckBoxPreference mAutoUpdate;
	ListPreference mAutoUpdateInterval;
	Preference mLastUpgradeTimestamp;
	Preference mNumberLocationDatabaseVersion;
	
	boolean mAutoUpdateValue;
	String mAutoUpdateIntervalValue;
	String mLastUpgradeTimestampValue;
	String mNumberLocationDatabaseVersionValue;
	

	/** Return a properly configured SharedPreferences instance */
	public static SharedPreferences getSharedPreferences(Context context) {
		return context.getSharedPreferences(NumberLocationConst.NUMBER_LOCATION_PREFERENCE,
				Context.MODE_PRIVATE);
	}

	/** Set the default shared preferences in the proper context */
	public static void setDefaultValues(Context context) {
		PreferenceManager.setDefaultValues(context, NumberLocationConst.NUMBER_LOCATION_PREFERENCE,
				Context.MODE_PRIVATE, R.xml.preferences, false);
	}

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		PreferenceManager preferenceManager = getPreferenceManager();
		preferenceManager.setSharedPreferencesName(NumberLocationConst.NUMBER_LOCATION_PREFERENCE);

		addPreferencesFromResource(R.xml.preferences);

		PreferenceScreen preferenceScreen = getPreferenceScreen();
		preferenceScreen.getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);

		mAutoUpdate = (CheckBoxPreference) preferenceScreen
		.findPreference(NumberLocationConst.KEY_AUTO_UPDATE);
		mAutoUpdateValue = preferenceScreen.getSharedPreferences().getBoolean(NumberLocationConst.KEY_AUTO_UPDATE, NumberLocationConst.DEFAULT_VALUE_OF_AUTO_UPDATE);

		mAutoUpdateInterval = (ListPreference) preferenceScreen
				.findPreference(NumberLocationConst.KEY_AUTO_UPDATE_INTERVAL);
		String summary = getApplicationContext().getResources().getStringArray(
				R.array.auto_update_interval_text)[Integer
				.parseInt(mAutoUpdateInterval.getValue()) - 1];
		mAutoUpdateInterval.setSummary(summary);
		mAutoUpdateIntervalValue = preferenceScreen.getSharedPreferences().getString(NumberLocationConst.KEY_AUTO_UPDATE_INTERVAL, NumberLocationConst.AUTO_UPDATE_INTERVAL_1_M);
		//temp switch off auto upgrade.
		preferenceScreen.removePreference(findPreference(NumberLocationConst.KEY_PREFERENCES_SETTING));
		
		mLastUpgradeTimestamp = (Preference) preferenceScreen.findPreference(NumberLocationConst.KEY_LAST_UPGRADE_TIMESTAMP);
		mLastUpgradeTimestampValue = preferenceScreen.getSharedPreferences().getString(NumberLocationConst.KEY_LAST_UPGRADE_TIMESTAMP, 
				getApplicationContext().getText(R.string.title_last_upgrade_timestamp_default).toString());
		if(mLastUpgradeTimestampValue.equals(getApplicationContext().getText(R.string.title_last_upgrade_timestamp_default).toString())){
			mLastUpgradeTimestamp.setSummary(mLastUpgradeTimestampValue);
		}else{
			mLastUpgradeTimestamp.setSummary(transferTimestamp(mLastUpgradeTimestampValue));
		}
		
		mNumberLocationDatabaseVersion = (Preference) preferenceScreen.findPreference(NumberLocationConst.KEY_DATABASE_VERSION);
		mNumberLocationDatabaseVersionValue = NumberLocationUtilities.getNumberLocationDatabaseVersion(getApplicationContext());
		mNumberLocationDatabaseVersion.setSummary(mNumberLocationDatabaseVersionValue);
	}
	
	public boolean is24HourFormat() {
		ContentResolver cv = this.getContentResolver();
		String strTimeFormat = android.provider.Settings.System.getString(cv,
				android.provider.Settings.System.TIME_12_24);
		if(TextUtils.isEmpty(strTimeFormat)||null==strTimeFormat)
			return false;
		if (strTimeFormat.equals("24"))
			return true;
		return false;
	}
	public String transferTimestamp(String timestampInMillis) {
		if(timestampInMillis == null)
			return null;
		int hour = 0;
		String ampm = null;
		String result = null;
		
		boolean is24Hour = is24HourFormat();
		Calendar todaysDate = new GregorianCalendar();
		todaysDate.setTimeInMillis(Long.valueOf(timestampInMillis));
		
		int year = todaysDate.get(Calendar.YEAR);
		int month = todaysDate.get(Calendar.MONTH) + 1;
		int day = todaysDate.get(Calendar.DAY_OF_MONTH);
		if(is24Hour){
			hour = todaysDate.get(Calendar.HOUR_OF_DAY);
		}else{
			if(todaysDate.get(Calendar.AM_PM)== Calendar.AM){
				ampm = this.getApplicationContext().getText(R.string.string_calendar_am).toString();
			}else{
				ampm = this.getApplicationContext().getText(R.string.string_calendar_pm).toString();
			}
			hour = todaysDate.get(Calendar.HOUR);
		}
		int minute = todaysDate.get(Calendar.MINUTE);
		int second = todaysDate.get(Calendar.SECOND);
		DecimalFormat df = new DecimalFormat("00");
		if(is24Hour){
		  result = year + "-" + df.format(month) + "-" + df.format(day) + " " + df.format(hour) + ":"
				+ df.format(minute) + ":" + df.format(second);
		}else{
			result = year + "-" + df.format(month) + "-" + df.format(day) + " " + df.format(hour) + ":"
			+ df.format(minute) + ":" + df.format(second) + " " + ampm;
		}
		return result;
	}
	
	public void showWarningDialog(String title, String warning) {
		View view = getLayoutInflater().inflate(R.layout.alert_update_dialog, null);
		TextView text_String_result = (TextView) view.findViewById(R.id.string_update_result);
		text_String_result.setText(warning);

		new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(title).setView(view).setPositiveButton(
				this.getApplicationContext().getText(R.string.button_close), null).show();
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		String summary = getApplicationContext().getResources().getStringArray(
				R.array.auto_update_interval_text)[Integer
				.parseInt(mAutoUpdateInterval.getValue()) - 1];
		mAutoUpdateInterval.setSummary(summary);
        if (key.equals(NumberLocationConst.KEY_AUTO_UPDATE)) {
            boolean newSetting = sharedPreferences.getBoolean(NumberLocationConst.KEY_AUTO_UPDATE, NumberLocationConst.DEFAULT_VALUE_OF_AUTO_UPDATE);
            if(mAutoUpdateValue != newSetting) {
            	mAutoUpdateValue = newSetting;
            	if(mAutoUpdateValue ==true){
            		showWarningDialog(this.getApplicationContext().getText(R.string.title_network_cost_warning).toString(),
            				this.getApplicationContext().getText(R.string.string_network_cost_warning).toString());
            	}
                Log.d(TAG, " mAutoUpdateValue changed!");
    			// Notify service to start the timer.
    			Intent i = new Intent(NumberLocationConst.ACTION_AUTO_CHECK_SETTING_CHANGED);
    			i.setClassName(getApplicationContext(), NumberLocationConst.CLASS_NUMBERLOCATIONSERVICE);
//    			Intent ii = new Intent
    			i.putExtra(NumberLocationConst.EXTRA_AUTO_UPDATE_SWITCH, newSetting);
    			i.putExtra(NumberLocationConst.EXTRA_AUTO_UPDATE_INTERVAL_TIME, sharedPreferences.getString(NumberLocationConst.KEY_AUTO_UPDATE_INTERVAL, NumberLocationConst.AUTO_UPDATE_INTERVAL_1_M));
    			startService(i);
            }
        }
        if (key.equals(NumberLocationConst.KEY_AUTO_UPDATE_INTERVAL)) {
        	String newSetting = sharedPreferences.getString(NumberLocationConst.KEY_AUTO_UPDATE_INTERVAL, NumberLocationConst.AUTO_UPDATE_INTERVAL_1_M);
        	if(!mAutoUpdateIntervalValue.equals(newSetting)) {
        		mAutoUpdateIntervalValue = newSetting;
        		Log.d(TAG, " mAutoUpdateIntervalValue changed!");
    			// Notify service to start the timer.
    			Intent i = new Intent(NumberLocationConst.ACTION_AUTO_CHECK_SETTING_CHANGED);
    			i.setClassName(getApplicationContext(), NumberLocationConst.CLASS_NUMBERLOCATIONSERVICE);
    			i.putExtra(NumberLocationConst.EXTRA_AUTO_UPDATE_SWITCH, sharedPreferences.getBoolean(NumberLocationConst.KEY_AUTO_UPDATE, NumberLocationConst.DEFAULT_VALUE_OF_AUTO_UPDATE));
    			i.putExtra(NumberLocationConst.EXTRA_AUTO_UPDATE_INTERVAL_TIME, newSetting);
    			startService(i);
        	}
        }		
	}
	

}
