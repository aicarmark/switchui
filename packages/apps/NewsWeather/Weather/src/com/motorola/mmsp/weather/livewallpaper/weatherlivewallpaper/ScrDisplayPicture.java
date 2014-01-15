package com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper;

import com.motorola.mmsp.weather.livewallpaper.utility.GlobalDef;
import com.motorola.mmsp.weather.livewallpaper.utility.WeatherInfoProvider.WeatherInfo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.SurfaceHolder;

public class ScrDisplayPicture extends ScrDisplayBase {

    private static String TAG = "LiveWallpaperService";

    private float mOffset = (float)0.5;

    public ScrDisplayPicture() {
        super();
    }

    public void desiredSizeChanged(){

    } 

    public void offsetsChanged(float xOffset, float yOffset,
            float xStep, float yStep, int xPixels, int yPixels){
        if( xStep == 0.0 )
            return;

        mOffset = xOffset;
        Log.d(TAG,"ScrDisplayPicture xOffset = "+xOffset+", xStep = "+xStep+", xPixels = "+xPixels);
        drawFrame();
    }

    public void setVideo( String path ){
        Log.d(TAG,"ScrDisplayPicture " + path);
        if( mForeignPackage != null ){
            Drawable d = mForeignPackage.getDrawable(path);
            if( d != null ){
                Bitmap bmp = ((BitmapDrawable)d).getBitmap();
                if( bmp.equals(mBmp) == false ){
                    mBmp = Bitmap.createBitmap(bmp); 
                    Log.d(TAG,"ScrDisplayPicture szie = ("+bmp.getWidth()+",  "+bmp.getHeight()+")");
                }
                mIsPrepare = true;
            } else {
                Log.e(TAG,"ScrDisplayPicture get AssetFileDescriptor from mForeignPackage failly.");
            }
        }
    }

    public void unInit(){    
        mIsPrepare = false;
    } 

    protected void drawCube(Canvas canvas){
        Rect screenRc = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());

        Log.d(TAG,"ScrDisplayPicture drawCube");
        if( mBmp != null && mBmp.isRecycled() == false ){
            Rect rc = new Rect();
            rc.left   = (int)(mBmp.getWidth()/2*mOffset);
            rc.right  = (int)(mBmp.getWidth()/2+rc.left);
            rc.top    = 0;
            rc.bottom = canvas.getHeight();
            Log.d(TAG,"ScrDisplayPicture left = "+rc.left+", right = "+rc.right);
            canvas.drawBitmap( mBmp, rc, screenRc, null);	
        }
        mCubeEngine.updateWeatherTextInfo( canvas );

        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
    }

    public void drawFrame() {
        super.drawFrame();
    }

    public void start(boolean isPreview){
        mIsPrepare = true;
        if( mIsPrepare ){
            mCubeEngine.configurationChanged();
            mWnp.updateWeatherInfoFromDB(mContext);
            updateVideo(isPreview);
        }
    }

    public void unRegisterTimer(){
        super.unRegisterTimer();
    }

    public void updateVideo(boolean isPreview){
        WeatherInfo info = mWnp.getWeatherInfo();

        // set daytime or night
        String strDayTime = null;
        if( info.isDay == GlobalDef.NIGHT )
            strDayTime = "night";
        else 
            strDayTime = "day";

        String file = "R.drawable." + mCubeEngine.getWeatherTypeStr( info ) + "_" + 
                strDayTime + "_" + getOrientationString() + "_large";
        if( !isSameFile(file) ){
            mCurPlayFile = file;
            //set video
            setVideo( file );
            drawFrame();
        }
    }

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
	}
}
