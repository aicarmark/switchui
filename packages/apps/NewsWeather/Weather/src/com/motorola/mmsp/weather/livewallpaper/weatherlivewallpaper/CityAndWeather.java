package com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper;


import com.motorola.mmsp.weather.livewallpaper.utility.Device;

import android.content.SharedPreferences;
import android.widget.TextView;

interface CityAndWeather {
    public String onUpdateCityWeather();//update weather info and animation
    public void onMySharedPreferenceChanged(SharedPreferences sp, String key);
    public Device getCurDevice();
    public String onUpdateCityWeatherData();//only update weather info
}
