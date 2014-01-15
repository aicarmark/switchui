package com.motorola.mmsp.weather.livewallpaper.utility;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;

import com.motorola.mmsp.weather.livewallpaper.utility.GlobalDef.TYPE;
import com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper.ScrDisplayAnimation;
import com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper.ScrDisplayBase;

public class DevTinQ extends Device{

    public DevTinQ(){
        EDITION = TYPE.TINQ;
        TIP_STROKE_WIDTH = 4;
        TIP_TEXT_SIZE = 40;
        BIG_BIG_TEXT_SIZE = 18;
        BIG_SMALL_TEXT_SIZE = 16;
        BIG_SPACING = 22;
        SMALL_BIG_TEXT_SIZE = 15;
        SMALL_SMALL_TEXT_SIZE = 12;
        SMALL_SPACING = 18;
        HD_OFFSET_X = 320-30;
        HD_OFFSET_Y = 100;
        LD_OFFSET_X = 480-65;
        LD_OFFSET_Y = 100;
    }

    @Override
    public String getOrientation(Context c){
        return GlobalDef.LD;
    }

    @Override
    public ScrDisplayBase getDisplayType(){
        if( mDisplayType == null )
            mDisplayType = new ScrDisplayAnimation();
        return mDisplayType;
    }

    @Override
    public void removeDisplayType(){
        if( mDisplayType != null )
            mDisplayType = null;
    }

    @Override
    public void setOrientationStatus(Activity act) {
        if( act != null )
            act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }
}
