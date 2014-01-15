package com.motorola.mmsp.weather.livewallpaper.utility;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.motorola.mmsp.weather.R;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class WeatherInfoProvider {
    private static final String TAG = "LiveWallpaperService";
    private final static boolean DEBUG = true;
    public static final String ACTION_EXTERNAL_WEATHER_QUERY = "com.motorola.mmsp.weather.external.query";

    public class WeatherInfo{
        public  String city;
        public  String adminArea;
        public  String temperature;
        public  String time;
        public  String condition;
        public  String weekday;
        public  String lowTemp;
        public  String highTemp;
        public  String windPower;
        public  String timeZone;
        public  int    weatherType = 1;
        public  int    unit        = GlobalDef.ACCU_SERVICE;
        public  int    isDay       = GlobalDef.DAYTIME;
    }

    private static int mWeatherServiceType = GlobalDef.ACCU_SERVICE;
    private WeatherInfo mWeatherInfo = new WeatherInfo();

    public WeatherInfo getWeatherInfo(){
        //mWeatherInfo.time = getCurrentTime();
        return mWeatherInfo;
    }

    public static int getWeatherServiceType(){
        return mWeatherServiceType;
    }

    private String getCityIdFromName(Context context, String city)
    {
        if(context == null) 
        return null;
        ContentResolver localContentResolver = context.getContentResolver();
        String selection = "settingValue= ?";
        String[] selectionargs = new String[]{city};
        String id = null;
        Cursor c = null;
        try{
	        c = localContentResolver.query( GlobalDef.uri_setting, null, selection, selectionargs, null);
	        if( c!= null && c.moveToFirst())
	        id = c.getString(0);
        }
        catch(Exception e){
        	e.printStackTrace();
        }
        finally{
	        // close Cursor
	        if( c != null )
	        c.close();
        }
        return id;
    }

    public void resetWeatherInfo()
    {
        this.mWeatherInfo.city = "";
        this.mWeatherInfo.adminArea = "";
        this.mWeatherInfo.temperature = "-";
        this.mWeatherInfo.lowTemp = "-";
        this.mWeatherInfo.highTemp = "-";
        this.mWeatherInfo.unit = GlobalDef.ACCU_SERVICE;
        this.mWeatherInfo.time = "";
        this.mWeatherInfo.condition = "";
        this.mWeatherInfo.weekday = "";
        this.mWeatherInfo.windPower = "";
        this.mWeatherInfo.weatherType = 1; //sunny
        this.mWeatherInfo.timeZone = "";
        this.mWeatherInfo.isDay = GlobalDef.DAYTIME;
    }

    public void updateWeatherInfoFromDB(Context context) {
        if( context == null) return; 

        resetWeatherInfo();

        String city = getCurCityName(context);
        mWeatherInfo.city = city;
        String cityId = getCurCityId(context);//getCityIdFromName(context, city);

        if( getCurCityIndex(context) != -1 ){    
            if( cityId == null || cityId.length() <= 0 )
                cityId = "auto_city";
        } else {
            Log.i(TAG,"WeatherInfoProvider there is no city in DB.");
            return;
        }

        Log.i(TAG,"WeatherInfoProvider query weather info from id : "+cityId+", city : "+city);
        String selection = "cityId= ?";
        String[] selectionargs = new String[]{cityId};
        Cursor c = null;
        try{
	        c = queryWeather(context,GlobalDef.uri_condtions,selection,selectionargs);
	        if(c!=null && c.moveToFirst()) {
	            if(c.getString(1) != null && c.getString(1).length() > 0) {
	                mWeatherInfo.city = city = c.getString(1);
	            }
	            mWeatherInfo.unit = c.getInt(8);
	            mWeatherServiceType = mWeatherInfo.unit;
	            if(mWeatherServiceType == GlobalDef.SINA_SERVICE) {
	                mWeatherInfo.isDay = isDayTime( 6, 18 );
	            }
	            mWeatherInfo.condition = c.getString(10);
	            mWeatherInfo.weatherType = c.getInt(9);
	            mWeatherInfo.temperature = getTemperatureString( context, getTemperatureFromDB(c) );
	            mWeatherInfo.timeZone = c.getString(c.getColumnIndex("timezoneAccu"));
	        }
        }
        catch(Exception e){
        	e.printStackTrace();
        }
        finally{
	        // close Cursor
	        if( c != null )
	        c.close();
        }

        try {
	        c = queryWeather(context,GlobalDef.uri_forecast,selection,selectionargs);
	        if(c!=null && c.moveToFirst()) {
	            if( mWeatherServiceType == GlobalDef.ACCU_SERVICE ){
	                if( c.getString(10).equals(GlobalDef.CNST_ILLUMINATION) && 
	                        c.getString(11).equals(GlobalDef.CNST_ILLUMINATION) )
	                    mWeatherInfo.isDay = GlobalDef.DAYTIME;
	                else if( c.getString(10).equals(GlobalDef.CNST_DARKNESS) && 
	                        c.getString(11).equals(GlobalDef.CNST_DARKNESS) )
	                    mWeatherInfo.isDay = GlobalDef.NIGHT;
	                else{
	                    String start = c.getString(10).split(" ")[0];
	                    String end = c.getString(11).split(" ")[0];
	                    String [] s = start.split(":");
	                    String [] e = end.split(":");
	                    try{
	                        mWeatherInfo.isDay =isDayTime( Integer.parseInt(s[0]), Integer.parseInt(s[1]), 
	                        Integer.parseInt(e[0])+12, Integer.parseInt(e[1]) );
	                    }catch(Exception e1){
	                        e1.printStackTrace();
	                        mWeatherInfo.isDay = GlobalDef.DAYTIME;
	                    }
	                }
	            }
	        }
        }
        catch(Exception e){
        	e.printStackTrace();
        }
        finally{
	        // close Cursor
	        if( c != null )
	        c.close();
        }

        Log.d(TAG,"WeatherInfoProvider condition : "+mWeatherInfo.condition);
        Log.d(TAG,"WeatherInfoProvider weatherType : "+mWeatherInfo.weatherType);
        Log.d(TAG,"WeatherInfoProvider temperature : "+mWeatherInfo.temperature);
    }

    private int isDayTime( int starthour, int startmin, int endhour, int endmin ){
        int isDay = GlobalDef.NIGHT;
        String time = getCurrentTime();
        String hour = time.split(":")[0];
        String minite = time.split(":")[1];
        Log.d(TAG,"WeatherInfoProvider <accu> current time : "+time+";start : "+starthour+":"+startmin+",end : "+endhour+":"+endmin);
        if( hour != null && Integer.parseInt(hour) == starthour && Integer.parseInt(minite)>startmin )
            isDay = GlobalDef.DAYTIME;
        else if( hour != null && (Integer.parseInt(hour)>starthour && Integer.parseInt(hour)<endhour) )
            isDay = GlobalDef.DAYTIME;
        else if( hour != null && Integer.parseInt(hour) == endhour && Integer.parseInt(minite)<endmin )
            isDay = GlobalDef.DAYTIME;

        return isDay;
    }

    private String getCurrentTime(){
        TimeZone tz = null;
        Log.d(TAG,"WeatherInfoProvider time zone : "+mWeatherInfo.timeZone);
        if (mWeatherInfo.timeZone == null || mWeatherInfo.timeZone.equals("")) {
            tz = TimeZone.getDefault();
        } 
        else {
            if(mWeatherInfo.timeZone.contains("-"))
            tz = TimeZone.getTimeZone("GMT" + mWeatherInfo.timeZone);
            else
            tz = TimeZone.getTimeZone("GMT+" + mWeatherInfo.timeZone);
        }
        Calendar cal = Calendar.getInstance(tz);
        Date curDate = cal.getTime();	
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
        formatter.setTimeZone(tz);
        String str = formatter.format(curDate);

        return str;
    }

    private int isDayTime( int start, int end ){
        int isDay = GlobalDef.NIGHT;
        String time = getCurrentTime();
        String hour = time.split(":")[0];
        String min = time.split(":")[1];
        Log.d(TAG,"WeatherInfoProvider <sina> current time : "+time+";start : "+start+",end : "+end);
        if( hour != null && 
                Integer.parseInt(hour) == start && 
                Integer.parseInt(min) > 0 ) {
            isDay = GlobalDef.DAYTIME;
        }
        if( hour != null && 
                Integer.parseInt(hour) > start && 
                Integer.parseInt(hour) < end) {
            isDay = GlobalDef.DAYTIME;
        }
        else if(hour != null && 
                Integer.parseInt(min) < 0 && 
                Integer.parseInt(hour) == end) {
            isDay = GlobalDef.DAYTIME;
        }

        return isDay;
    }

    private int getTemperatureFromDB( Cursor c ){
        int curTemper = GlobalDef.INVALID_TEMPERATURE;
        if( mWeatherInfo.unit == GlobalDef.ACCU_SERVICE ) {
            curTemper = c.getInt(7);
        }
        else {
            String str = c.getString(12);
            if( str != null && str.length() > 0 )
            curTemper = Math.round(Float.parseFloat( str ));
        }

        return curTemper;
    }

    private String getTemperatureString( Context context, int temper ){
        String temperStr = null;

        //invaild temperature
        if( GlobalDef.INVALID_TEMPERATURE == temper )
            return temperStr;

        switch( getTemperatureUnit(context) ){
            case GlobalDef.UNIT_C:
                if( mWeatherInfo.unit == GlobalDef.ACCU_SERVICE )
                    temperStr = getTemeratureString( context, getTemperature_C( temper ), GlobalDef.UNIT_C);
                else
                    temperStr = getTemeratureString( context, temper, GlobalDef.UNIT_C);
                break;
            case GlobalDef.UNIT_F:
                if( mWeatherInfo.unit == GlobalDef.SINA_SERVICE )
                    temperStr = getTemeratureString( context, getTemperature_F( temper ), GlobalDef.UNIT_F);
                else
                    temperStr = getTemeratureString( context, temper, GlobalDef.UNIT_F);
                break;
        }
        return temperStr;
    }

    private int getTemperatureUnit(Context context){
        int unit = GlobalDef.ACCU_SERVICE;
        ContentResolver cr = context.getContentResolver();
        if( cr == null )
            return -1;
        String selections = "settingName = ?";
        String[] selectionargs = new String[] { GlobalDef.TEMPERATURE_UNIT };
        Cursor c = null;
        try {
	        c = cr.query(GlobalDef.uri_setting, null, selections, selectionargs, null);
	        if (c != null && c.getCount() > 0) {
	            c.moveToFirst();
	            String unitStr = c.getString(1);
	            if( unitStr != null && unitStr.length() > 0 )
	                unit = Integer.parseInt(unitStr);
	        }
        }
        catch(Exception e){
        	e.printStackTrace();
        }
        finally{
	        // close Cursor
	        if( c != null )
	        c.close();
        }

        return unit;
    }

    public static int getTemperature_C(int d){ 
        return Math.round((float) 5 * (d - 32) / 9);
    } 

    public static int getTemperature_F(int d){
        return Math.round(9.0F * d / 5 + 32);
    }

    private String getTemeratureString( Context context, int tem, int unit ){
        StringBuilder temperature = new StringBuilder().append(tem);
        if (unit == 0)
            temperature.append(context.getResources().getString(R.string.unit_str_c));
        else
            temperature.append(context.getResources().getString(R.string.unit_str_f));
        return temperature.toString();
    }

    private static Cursor queryWeather(Context context,Uri uri, String selections,String[] selectionargs) {
        if(context == null) 
            return null;
        if(DEBUG) 
            Log.d(TAG,"WeatherInfoProvider selections="+selections+" selectionargs="+selectionargs[0]);
        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(uri, null, selections, selectionargs, null);
        if (DEBUG && c!=null)
            Log.d(TAG, "WeatherInfoProvider GlobalDef.uri_condtions,  count=" + c.getCount());
        return c;
    }

    /*
    private static Cursor queryCityList(Context context) {
    Cursor c = null;
    if(context == null) return c;
    ContentResolver cr = context.getContentResolver();
    c = cr.query(GlobalDef.uri_citylist,
     null, null, null, null);
    return c;
    }
    */

    public static int getCurCityIndex(Context context){
        SharedPreferences sp = context.getSharedPreferences(GlobalDef.CITYSHAREDPFE, 0);
        return sp.getInt(GlobalDef.cur_city_index, -1);
    }

    public static String getCurCityId(Context context){
        SharedPreferences sp = context.getSharedPreferences(GlobalDef.CITYSHAREDPFE, 0);
        return sp.getString(GlobalDef.cur_city_id, "");
    }

    public static String getCurCityName(Context context){
        SharedPreferences sp = context.getSharedPreferences(GlobalDef.CITYSHAREDPFE, 0);
        return sp.getString(GlobalDef.cur_city_name, "");
    }

    public static void setCurCity(int index, String id, String city, Context context){
        SharedPreferences sp = context.getSharedPreferences(GlobalDef.CITYSHAREDPFE, 0);
        Editor e = sp.edit();
        e.putString(GlobalDef.cur_city_id, id);
        e.putString(GlobalDef.cur_city_name, city);
        e.putInt(GlobalDef.cur_city_index, index);
        e.commit();
    }

    public static int getWeatherServiceType(Context context){
        try{
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(GlobalDef.ACCU_SERVICE_PACKAGE, 1);
            return GlobalDef.ACCU_SERVICE;
        }catch (NameNotFoundException e){
            e.printStackTrace();
            return GlobalDef.SINA_SERVICE;
        }
    }

    public static ArrayList<GlobalDef.CityInfo> getExitCities(Context context){

        if(context == null) 
            return null;
        ContentResolver cr = context.getContentResolver();

        ArrayList<GlobalDef.CityInfo> localArrayList = new ArrayList<GlobalDef.CityInfo>();
        String selections = "settingName=?";
        String[] selectionargs = new String[]{ "weather_cityIds" };
        Cursor c = null;
        try {
	        c = cr.query(GlobalDef.uri_setting, null, selections, selectionargs, null);
	        Log.d(TAG,"WeatherInfoProvider Cursor c = "+c);
	        if ((c != null) && (c.moveToFirst())){
	            String str = c.getString(1);
	            String[] cityid = str.substring(1, str.length()-1).split(",");
	            for( int i = 0; i < cityid.length; ++i ){
	                cityid[i] = cityid[i].replace(" ", "");
	                if(cityid[i].equals(GlobalDef.auto_city_id) ){
	                    String auto_name = WeatherInfoProvider.getAutoCityName(context);
	                    GlobalDef.CityInfo info = new GlobalDef.CityInfo(GlobalDef.auto_city_id, auto_name);
	                    localArrayList.add( info );  
	                }
	                else{
	                    String selections1 = "settingName=?";
	                    String[] selectionargs1 = new String[]{ cityid[i] };
	                    Cursor c1 = null;
	                    try {
		                    c1 = cr.query(GlobalDef.uri_setting, null, selections1, selectionargs1, null);
		                    Log.d(TAG,"WeatherInfoProvider Cursor c1 = "+c);
		                    if ((c1 != null) && (c1.moveToFirst())){
		                        GlobalDef.CityInfo info = new GlobalDef.CityInfo(cityid[i], c1.getString(1));
		                        localArrayList.add( info );   
		                    }
	                    }
	                    catch(Exception e){
	                    	e.printStackTrace();
	                    }
	                    finally{
	            	        // close Cursor
	            	        if( c1 != null )
	            	        c1.close();
	                    }
	                }
	            }
	        }
        }
        catch(Exception e){
        	e.printStackTrace();
        }
        finally{
	        // close Cursor
	        if( c != null )
	        c.close();
        }
        return localArrayList;
    }

    public static boolean isSetAutoCity(Context context){	
        boolean res = false;
        if(context == null) return res;
        ContentResolver cr = context.getContentResolver();

        String selections = "settingName=?";
        String[] selectionargs = new String[]{ "setting_auto_city" };
        Cursor c = null;
        try {
	        c = cr.query(GlobalDef.uri_setting, null, selections, selectionargs, null);
	        if ((c != null) && (c.moveToFirst())){
	            if( c.getString(1).equals("true") )
	                res = true;
	            else
	                res = false;
	        }
	        else
	        {
	            res = false;
	        }
        }
        catch(Exception e){
        	e.printStackTrace();
        }
        finally{
	        // close Cursor
	        if( c != null )
	        c.close();
        }

        return res;
    }

    public static String getAutoCityName(Context context){
        String res = "";
        if(context == null) return res;

        String selection = "cityId= ?";
        String[] selectionargs = new String[]{GlobalDef.auto_city_id};
        Cursor c = null;
        try {
	        c = queryWeather(context,GlobalDef.uri_condtions,selection,selectionargs);
	        if(c!=null && c.moveToFirst()) {
	            res = c.getString(1);
	        }

        }
        catch(Exception e){
        	e.printStackTrace();
        }
        finally{
	        // close Cursor
	        if( c != null )
	        c.close();
        }

        return res;
    }
}
