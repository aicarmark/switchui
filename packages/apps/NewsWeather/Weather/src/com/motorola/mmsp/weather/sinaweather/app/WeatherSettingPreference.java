package com.motorola.mmsp.weather.sinaweather.app;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;

import com.motorola.mmsp.weather.R;

public class WeatherSettingPreference extends ListPreference {

	private String mSettingName = "";
    private Context mContext = null;
    
	public WeatherSettingPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		Log.d("WeatherWidget", "WeatherSettingPreference construction");
		mContext = context;
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WeatherSettingPreference); 
		mSettingName = a.getString(R.styleable.WeatherSettingPreference_settingName); 
                a.recycle();
		Log.d("WeatherWidget", "WeatherSettingPreference contruct end, settingName = " + mSettingName);
	}

    
//    public void setSettingName(String settingName) {
//    	Log.d("WeatherWidget", "WeatherSettingPreference->setSettingName settingName = " + settingName);
//    	mSettingName = settingName;
//    }
    
    public String getSettingName() {
    	Log.d("WeatherWidget", "WeatherSettingPreference->getSettingName settingName = " + mSettingName);
    	return mSettingName;
    }

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		Log.d("WeatherWidget", "WeatherSettingPreference->onSetInitialValue ");
		super.onSetInitialValue(restoreValue, defaultValue);
		restoreSetting();
		setSummary(getEntry());
	}
/*
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		Log.d("WeatherWidget", "WeatherSettingPreference->onDialogClosed ");
		super.onDialogClosed(positiveResult);
		if (positiveResult) {
			setSummary(getEntry());
			saveSetting(getValue());
		}
	}
*/

	@Override
	protected void onDialogClosed(boolean positiveResult) {
	    Log.d("WeatherWidget", "WeatherSettingPreference->onDialogClosed ");
	    super.onDialogClosed(positiveResult);
	    if (positiveResult) {
	        setSummary(getEntry());
	        saveSetting(getValue());
	        OnPreferenceChangeListener onChangeListener = getOnPreferenceChangeListener();
	        if (onChangeListener != null) {
	            onChangeListener.onPreferenceChange(this, getValue());
	        }
	    }
	}
	
	@Override
	protected boolean callChangeListener(Object newValue) {
   	    return true;
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		Log.d("WeatherWidget", "WeatherSettingPreference->onPrepareDialogBuilder ");
		restoreSetting();
		super.onPrepareDialogBuilder(builder);
	}

	public void restoreSetting() {
		Log.d("WeatherWidget", "WeatherSettingPreference->restoreSetting mSettingName = " + mSettingName);
		String settingValue = DatabaseHelper.readSetting(mContext, mSettingName);
		if (settingValue != null && !settingValue.equals("")) {
			setValue(settingValue);
		} else {
			Log.d("WeatherWidget", "WeatherSettingPreference->restoreSetting settingValue is not valid.");
		}
		
	}

	private void saveSetting(String value) {
		Log.d("WeatherWidget", "WeatherSettingPreference->saveSetting mSettingName = " + mSettingName + " value " + value);
		DatabaseHelper.writeSetting(mContext, mSettingName, value);
	}

}
