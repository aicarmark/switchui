package com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper;

import com.motorola.mmsp.weather.livewallpaper.utility.Device;
import com.motorola.mmsp.weather.livewallpaper.utility.WeatherInfoProvider;
import com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper.LiveWallpaperService.CubeEngine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;

public abstract class ScrDisplayBase {

    private static String TAG = "LiveWallpaperService";

    protected Bitmap mBmp = null;
    protected Canvas mCanvas = null;
    protected ForeignPackage      mForeignPackage;
    protected WeatherInfoProvider mWnp;
    protected boolean             mVisible;
    protected boolean             mIsPrepare = true;
    protected Context             mContext;
    protected Device              mDevice;
    protected CubeEngine          mCubeEngine;
    protected String              mCurPlayFile = "";

    public ScrDisplayBase(){	
        
    }

    public void setVisible( boolean v ){
        mVisible = v; 
        //Log.i(TAG, "mVisible = "+mVisible);
    }

    public void updateWeatherNewsProvider( WeatherInfoProvider wnp ){
        mWnp       = wnp;
    }

    public void setParams(WeatherInfoProvider wnp, Context c, Device d, ForeignPackage fp, CubeEngine ce){
        mWnp       = wnp;
        mContext   = c;
        mDevice    = d;
        mCubeEngine= ce;
        mForeignPackage = fp;
    }

    public void drawFrame() {
        SurfaceHolder holder = mCubeEngine.getSurfaceHolder();
        try {
            mCanvas = holder.lockCanvas();
            if (mCanvas != null) {
                //draw something
                drawCube(mCanvas);
            }
        } finally {
            try {
                if (mCanvas != null) {
                    holder.unlockCanvasAndPost(mCanvas);
                }
            } catch(IllegalArgumentException e) {
                Log.d(TAG,"ScrDisplayBase illegal argument.");
            }
        }
    }

    abstract public void desiredSizeChanged();

    abstract public void offsetsChanged(float xOffset, float yOffset,
               float xStep, float yStep, int xPixels, int yPixels);

    abstract public void setVideo( String path );

    abstract public void unInit();

    abstract protected void drawCube(Canvas canvas);
    abstract public void start(boolean isPreview);
    abstract public void pause();

    public void unRegisterTimer(){
        Log.d(TAG,"ScrDisplayBase unRegisterTimer");
    }

    abstract public void updateVideo(boolean isPreview);
    
    public final Canvas getCanvas(){
        return mCanvas;
    }
    
    protected String getOrientationString(){  
        return mDevice.getOrientation(mContext);
    }
    
    public void resetFilePath(){
        mCurPlayFile = "";
    }
    
    protected boolean isSameFile(String file){
    	 Log.d(TAG,"===============file="+file+"---------------curfile="+mCurPlayFile);
        if( file == null || ( !mCurPlayFile.equals("") && mCurPlayFile.equals(file)) ){
            Log.d(TAG,"ScrDisplayBase they are the same file or are null.");
            return true;
        }
        else{
            Log.d(TAG,"ScrDisplayBase they are the different files.");
            return false;
        }
    }
    
}
