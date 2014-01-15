package com.motorola.mmsp.weather.livewallpaper.utility;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import com.motorola.mmsp.weather.livewallpaper.utility.GlobalDef.TYPE;
import com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper.ScrDisplayAnimation;
import com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper.ScrDisplayBase;

public class DevIronmax extends Device{

    public DevIronmax(){
        EDITION = TYPE.IRONMAX;
        TIP_STROKE_WIDTH = 5;
        TIP_TEXT_SIZE = 50;
        BIG_BIG_TEXT_SIZE = 40;
        BIG_SMALL_TEXT_SIZE = 24;
        BIG_SPACING = 36;
        SMALL_BIG_TEXT_SIZE = 30;
        SMALL_SMALL_TEXT_SIZE = 18;
        SMALL_SPACING = 27;
        HD_OFFSET_X = 480-30;
        HD_OFFSET_Y = 100;
        LD_OFFSET_X = 854-105;
        LD_OFFSET_Y = 100;
    }

    @Override
    public String getOrientation(Context c){
        String orientation = GlobalDef.HD;

        //judge orientation
        if (c.getResources().getConfiguration().orientation == 
                Configuration.ORIENTATION_PORTRAIT) {   //hd
            orientation = GlobalDef.HD;
        } else { 
            orientation = GlobalDef.LD;
        }
        return orientation;
    }

    @Override
    public ScrDisplayBase getDisplayType(){
        if( mDisplayType == null ){
            mDisplayType = new ScrDisplayAnimation();
            Log.d("LiveWallpaperService","create ScrDisplayAnimation class");
        }
        return mDisplayType;
    }

    @Override
    public void removeDisplayType(){
        if( mDisplayType != null )
            mDisplayType = null;
    }

    @Override
    public void setOrientationStatus(Activity act) {
    }
}
