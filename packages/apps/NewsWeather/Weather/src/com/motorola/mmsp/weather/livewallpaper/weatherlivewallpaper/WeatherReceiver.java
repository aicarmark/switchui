package com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper;

import com.motorola.mmsp.weather.livewallpaper.utility.GlobalDef;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class WeatherReceiver extends BroadcastReceiver {  

    private final static String TAG = "LiveWallpaperService";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if( action == null )
            return;

        if( action.equals(GlobalDef.BROADCAST_CITYLIST_UPDATE) ){
            Log.i(TAG,"WeatherReceiver =====BROADCAST_CITYLIST_UPDATE=====");
            if( LiveWallpaperService.mCubeEngine != null ){
                LiveWallpaperService.mCubeEngine.updateCityList();
                LiveWallpaperService.mCubeEngine.onUpdateCityWeather();
            }
        }
        else if( action.equals(GlobalDef.BROADCAST_WEAHER_UPDATE) ){
            Log.i(TAG,"WeatherReceiver =====BROADCAST_WEAHER_UPDATE=====");
            if( LiveWallpaperService.mCubeEngine != null ) {
                LiveWallpaperService.mCubeEngine.updateCityList();
            }
        }
    }
}
