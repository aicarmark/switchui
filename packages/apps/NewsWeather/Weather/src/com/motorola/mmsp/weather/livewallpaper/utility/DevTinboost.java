package com.motorola.mmsp.weather.livewallpaper.utility;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;

import com.motorola.mmsp.weather.livewallpaper.utility.GlobalDef.TYPE;
import com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper.ScrDisplayBase;
import com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper.ScrDisplayPicture;

public class DevTinboost extends Device{

    public DevTinboost(){
        EDITION = TYPE.TINBOOST;
        TIP_STROKE_WIDTH = 4;
        TIP_TEXT_SIZE = 40;
        BIG_BIG_TEXT_SIZE = 18;
        BIG_SMALL_TEXT_SIZE = 16;
        BIG_SPACING = 22;
        SMALL_BIG_TEXT_SIZE = 15;
        SMALL_SMALL_TEXT_SIZE = 12;
        SMALL_SPACING = 18;
        HD_OFFSET_X = 320-30;
        HD_OFFSET_Y = 60;
        LD_OFFSET_X = 480-105;
        LD_OFFSET_Y = 60; 
    }

    @Override
    public String getOrientation(Context c){
        return GlobalDef.HD;
    }

    @Override
    public ScrDisplayBase getDisplayType(){
        if( mDisplayType == null )
            mDisplayType = new ScrDisplayPicture();
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
