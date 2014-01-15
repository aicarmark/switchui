package com.motorola.mmsp.weather.livewallpaper.utility;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;

import com.motorola.mmsp.weather.livewallpaper.utility.GlobalDef.TYPE;
import com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper.ScrDisplayAnimation;
import com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper.ScrDisplayBase;

public class DevCarbonsplash extends Device{

    public DevCarbonsplash(){
        EDITION = TYPE.CARBONSPALSH;
        TIP_STROKE_WIDTH = 5;
        TIP_TEXT_SIZE = 50;
        BIG_BIG_TEXT_SIZE = 48;
        BIG_SMALL_TEXT_SIZE = 28;
        BIG_SPACING = 44;
        SMALL_BIG_TEXT_SIZE = 36;
        SMALL_SMALL_TEXT_SIZE = 24;
        SMALL_SPACING = 32;
        HD_OFFSET_X = 800-130;
        HD_OFFSET_Y = 298;
        LD_OFFSET_X = 1280-150;
        LD_OFFSET_Y = 266;
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
    }
}
