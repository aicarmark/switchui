package com.motorola.mmsp.weather.livewallpaper.utility;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.motorola.mmsp.weather.livewallpaper.utility.GlobalDef.TYPE;
import com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper.ScrDisplayBase;

public abstract class Device {
    static private String TAG = "Device";
    /*
    static private DevTinQ sTinQ = null;
    static private DevIronmax sIronmax = null;	
    static private DevCarbonplay sCarbonplay = null;
    static private DevCarbonsplash sCarbonsplash = null;
    static private DevIronmaxct sIronmaxct = null;
    static private DevIronmaxtd sIronmaxtd = null;
    static private DevTinboost sTinboost = null;	
    static private Device sCurDevice = null;
    */

    static protected TYPE EDITION = TYPE.NONE;

    protected ScrDisplayBase mDisplayType = null;	

    public int TIP_STROKE_WIDTH;
    public int TIP_TEXT_SIZE;
    public int BIG_BIG_TEXT_SIZE;
    public int BIG_SMALL_TEXT_SIZE;
    public int BIG_SPACING;
    public int SMALL_BIG_TEXT_SIZE;
    public int SMALL_SMALL_TEXT_SIZE;
    public int SMALL_SPACING;
    public int HD_OFFSET_X;
    public int HD_OFFSET_Y;
    public int LD_OFFSET_X;
    public int LD_OFFSET_Y;

    /*
    static {
    getDevice(TYPE.IRONMAX);
    }
    */

    /*
	static public Device getDevice(TYPE type){	
		//dynamic
		if( TYPE.TINQ == type ){
			if( sTinQ == null )
				sTinQ = new DevTinQ();
			sCurDevice = sTinQ;
			Log.e(TAG,"current device is tinq");
		}
		else if( TYPE.IRONMAX == type ){
			if( sIronmax == null )
				sIronmax = new DevIronmax();
			sCurDevice = sIronmax;
			Log.e(TAG,"current device is ironmax");
		}
		else if( TYPE.CARBONPLAY == type ){
			if( sCarbonplay == null )
				sCarbonplay = new DevCarbonplay();
			sCurDevice = sCarbonplay;
			Log.e(TAG,"current device is carbonplay");
		}
		else if( TYPE.CARBONSPALSH == type ){
			if( sCarbonsplash == null )
				sCarbonsplash = new DevCarbonsplash();
			sCurDevice = sCarbonsplash;
			Log.e(TAG,"current device is carbonsplash");
		}
		//static
		else if( TYPE.IRONMAXCT == type ){
			if( sIronmaxct == null )
				sIronmaxct = new DevIronmaxct();
			sCurDevice = sIronmaxct;
			Log.e(TAG,"current device is ironmaxct");
		}
		else if( TYPE.IRONMAXTD == type ){
			if( sIronmaxtd == null )
				sIronmaxtd = new DevIronmaxtd();
			sCurDevice = sIronmaxtd;
			Log.e(TAG,"current device is ironmaxtd");
		}
		else if( TYPE.TINBOOST == type ){
			if( sTinboost == null )
				sTinboost = new DevTinboost();
			sCurDevice = sTinboost;
			Log.e(TAG,"current device is tinboost");
		}
		else {
			sIronmax = new DevIronmax();
			sCurDevice = sIronmax;
			Log.e(TAG,"current device is ironmax defaultly");
		}
		
		return sCurDevice;
	}
	
	static public Device getCurDevice(){		
		return sCurDevice;
	}
	
	static public TYPE getCurType(){
		return EDITION;
	}
	*/

    abstract public String getOrientation(Context c);
    abstract public ScrDisplayBase getDisplayType();
    abstract public void removeDisplayType();
    abstract public void setOrientationStatus(Activity act);
}
