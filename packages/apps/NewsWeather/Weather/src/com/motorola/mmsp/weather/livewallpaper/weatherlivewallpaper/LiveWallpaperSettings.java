package com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.motorola.mmsp.weather.R;

public class LiveWallpaperSettings extends PreferenceActivity
                   implements SharedPreferences.OnSharedPreferenceChangeListener
{
    static private CityAndWeather mCityAndWeather;

    private SharedPreferences mSharedPreferences;
    private boolean mIsUpdateWeather = false;
    @Override
    protected void onCreate(Bundle icicle){
        super.onCreate(icicle);
        init();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if( hasFocus && mIsUpdateWeather ){
            mIsUpdateWeather = false;

            initCityPrefrence();
        }
    }

    private void initCityPrefrence(){
        if( mCityAndWeather != null ){
            String city = mCityAndWeather.onUpdateCityWeatherData();
    
            String key = getResources().getString(R.string.location_key);
            Preference lp = (Preference)findPreference(key);
            if( lp != null )
                lp.setSummary(city);
        } 
    }

    private void init(){
        getPreferenceManager().setSharedPreferencesName(LiveWallpaperService.SHARED_PREFS_NAME);
        addPreferencesFromResource(R.xml.livewallpaper_settings);
        mSharedPreferences = getPreferenceManager().getSharedPreferences();
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        String displayKey = getResources().getString(R.string.display_key);
        String sizeKey = getResources().getString(R.string.size_key);
        onSharedPreferenceChanged(mSharedPreferences, displayKey);
        onSharedPreferenceChanged(mSharedPreferences, sizeKey);
        initCityPrefrence();

        try{
            mCityAndWeather.getCurDevice().setOrientationStatus(this);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onDestroy(){
        getPreferenceManager().getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {

        String str = getResources().getString(R.string.location_key);
        if( preference.getKey().equals(str) ){
            //start city list activity
            Intent intent = new Intent();  
            intent.setClass(this, LivewallpaperCityList.class);  
            startActivity(intent); 
            mIsUpdateWeather = true;
        }
        return false;
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,String key){
        String summary = sharedPreferences.getString( key, "1" );
        int value = Integer.parseInt( summary );
        String item = null;
        if( key.equals( getResources().getString(R.string.display_key) ) )
            item = getResources().getStringArray(R.array.text_display_list)[value];
        else if( key.equals( getResources().getString(R.string.size_key) ) )
            item = getResources().getStringArray(R.array.text_size_list)[value];

        ListPreference lp = (ListPreference)findPreference(key);
        if( item != null && lp != null )
            lp.setSummary(item);

        if(mCityAndWeather != null)
            mCityAndWeather.onMySharedPreferenceChanged(sharedPreferences, key);
    }

    static public void registerCityAndWeather( CityAndWeather c){
        mCityAndWeather = c;
    }

    static public void unRegisterCityAndWeather( CityAndWeather c){
        mCityAndWeather = null;
    }
}
