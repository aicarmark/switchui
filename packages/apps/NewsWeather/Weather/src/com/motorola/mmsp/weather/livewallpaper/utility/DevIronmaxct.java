package com.motorola.mmsp.weather.livewallpaper.utility;

import android.app.Activity;
import android.content.Context;

import com.motorola.mmsp.weather.livewallpaper.utility.GlobalDef.TYPE;
import com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper.ScrDisplayBase;
import com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper.ScrDisplayPicture;

public class DevIronmaxct extends Device {

    public DevIronmaxct(){
        EDITION = TYPE.IRONMAXCT;
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
