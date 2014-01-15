package com.motorola.mmsp.weather.livewallpaper.utility;

import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.net.Uri;

public class GlobalDef {
	public static final String AUTHORITY = "com.motorola.mmsp.weather.service.WeatherProvider";
    public final static String BROADCAST_CITYLIST_UPDITING= "com.motorola.mmsp.weather.cities.updating";
    public final static String BROADCAST_WEAHER_UPDATE = "com.motorola.mmsp.weather.updated.response";
    public final static String BROADCAST_CITYLIST_UPDATE = "com.motorola.mmsp.weather.cities.updated.response";
    public final static String BROADCAST_SCREENT_ON = "android.intent.action.SCREEN_ON";
    public final static String BROADCAST_TIME_TICK = Intent.ACTION_TIME_TICK;

    public final static String BROADCAST_UPDATE_WEATHERNEWSWIDGET_TIME = "com.arcsoft.weathernews.updateweathertime";
    public final static String BROADCAST_DETAILVIEWEXIT = "com.motorola.mmsp.motoswitch.action.weatherdetailview.exit";

    public final static String CITYIDSHAREDPFE = "com.arcsoft.weathernews.cityid_sharePrfce";
    public final static String INVALIDCITYID = "invalid_cityid";
    public final static String CITY_ACTIVITY = "motorola.weather.action.CitiesActivity";

    public final static String ACCU_SERVICE_PACKAGE      = "com.motorola.mmsp.newsweather.accuweather.service";
    public final static String SINA_SERVICE_PACKAGE      = "com.motorola.mmsp.newsweather.sinaweather.service";
    public final static String WEATHER_ANIMATION_PACKAGE = "com.motorola.mmsp.weatheranimationres";
    public final static String DATA_DIR = "/data/data/com.motorola.mmsp.weatherlivewallpaper";

    public final static String CITYSHAREDPFE = "com.motorola.mmsp.newsweather.livewallpaper.weatherlivewallpaper.citysharedpfe";
    public final static String cur_city_id = "cur_city_id";
    public final static String cur_city_name = "cur_city_name";
    public final static String cur_city_index = "cur_city_index";
    public final static String auto_city_id = "auto_city";
    public final static int    GPS_LOCATION_INDEX = -2;

    public static final String  TEMPERATURE_UNIT = "temperature_uint";
    public static final int     UNIT_C = 0;
    public static final int     UNIT_F = 1;
    public final static int     INVALID_TEMPERATURE = 10000;
    //table string and basic uri
    public final static String table_forecast   = "content://" + AUTHORITY + "/forecast/";
    public final static String table_conditions = "content://" + AUTHORITY + "/conditions";
    public final static String table_city       = "content://" + AUTHORITY + "/city";
    public final static String table_setting    = "content://" + AUTHORITY + "/setting";
    public final static String table_citylist   = "content://" + AUTHORITY + "/citylist";
    public final static String current_city     = "cur_city";
    public final static Uri uri_forecast  = Uri.parse(table_forecast);
    public final static Uri uri_condtions = Uri.parse(table_conditions);
    public final static Uri uri_city      = Uri.parse(table_city);
    public final static Uri uri_setting   = Uri.parse(table_setting);
    public final static Uri uri_citylist  = Uri.parse(table_citylist);

    public final static Uri uri_settingIDs=Uri.withAppendedPath(uri_setting, "weather_cityIds");

    public final static int ACCU_SERVICE = 1;
    public final static int SINA_SERVICE = 0;

    public final static String CNST_ILLUMINATION = "Cnst Ill";
    public final static String CNST_DARKNESS = "Cnst Dark";

    public final static int UNKWON   = -1;	
    public final static int DAYTIME = 1;
    public final static int NIGHT   = 0;	

    public final static String HD = "hd";
    public final static String LD = "ld"; 

    public static class CityInfo {
        public String id;
        public String name;

        CityInfo(String id, String name){
            this.id   = id;
            this.name = name;
        }
    }

    public static enum TYPE{NONE, TINQ, TINBOOST, IRONMAX, IRONMAXCT, IRONMAXTD, CARBONPLAY, CARBONSPALSH};
}
