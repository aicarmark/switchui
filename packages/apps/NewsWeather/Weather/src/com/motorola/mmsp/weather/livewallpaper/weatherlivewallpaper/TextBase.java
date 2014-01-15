package com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper;

import java.util.ArrayList;

import com.motorola.mmsp.weather.livewallpaper.utility.Device;
import com.motorola.mmsp.weather.livewallpaper.utility.GlobalDef;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.Layout.Alignment;
import android.util.Log;


public abstract class TextBase {
    public final static int BIG = 0;
    public final static int SMALL = 1;

    protected Canvas mCanvas;
    protected Context mContext;
    protected TextPaint mBigPaint;
    protected TextPaint mSmallPaint;
    protected boolean mVisible = true;
    protected ArrayList<Float>  mLineSpacing = null;
    protected Device mDevice = null;
    static protected int   mLeft;
    static protected float mTop;

    abstract protected TextPaint getTypePaint( int type );

    public TextBase( Canvas c, Context ct, Device d ){
        mCanvas  = c;
        mContext = ct;
        mDevice = d;
    }

    public void updateCanvas( Canvas c ){
        mCanvas = c;
    }

    public void drawText( int index, String str, int type ){
        /*
        Log.i("drawText", "mCanvas = "+mCanvas+
            ", mVisible = "+mVisible+", "+
            ", mLineSpacing"+mLineSpacing);
        */
        if( (mCanvas != null) && mVisible && (str != null) && mLineSpacing != null ){
            mCanvas.drawText( getMaxStr(str, type), mLeft, mLineSpacing.get(index), getTypePaint(type) );
            //Log.e("drawText",str+" ["+mLeft+", "+mLineSpacing.get(index)+"]");
        }
    }
    
    private String getMaxStr(final String str, int type){
        int lineWidth = mLeft-30;
        StaticLayout layout = new StaticLayout(
                str, getTypePaint(type), lineWidth, Alignment.ALIGN_CENTER, 1, 0, true);   	
        int index_start = layout.getLineStart(0);
        int index_end   = layout.getLineEnd(0);
        String maxstr = str.substring(index_start, index_end );
        if( !str.equals(maxstr) ){
            maxstr = maxstr.substring(0, maxstr.length()-4);
            maxstr += "...";
        }
        return maxstr;
    }
    
    public void drawTipText( String str, int x, int y ){
        if( (mCanvas != null) && mVisible && (mDevice != null) ){
            final Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setAntiAlias(true);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Align.LEFT);	
            paint.setStrokeWidth(mDevice.TIP_STROKE_WIDTH);
            paint.setTextSize(mDevice.TIP_TEXT_SIZE);
            //get text bound
            Rect r = new Rect();  
            paint.getTextBounds(str, 0, str.length(), r);
            //paint in the center
            mCanvas.drawText( str, (x-r.width())/2, (y-r.height())/2, paint);
        }
    }
    
    public void setVisiblity( boolean b ){
        mVisible = b;
        if( b ){
            mBigPaint.setAlpha(255);
            mSmallPaint.setAlpha(255);
            Log.d("LiveWallpaperService","TextBase visible");
        }
        else{
            mBigPaint.setAlpha(0);
            mSmallPaint.setAlpha(0);
            Log.d("LiveWallpaperService","TextBase unvisible");
        }
    }
    
    public boolean getVisiblity(){
        boolean b = mVisible;
        return b;
    }

    protected void initPaint(){
        mSmallPaint = new TextPaint();
        mSmallPaint.setColor(Color.WHITE);
        mSmallPaint.setAntiAlias(true);
        mSmallPaint.setStrokeCap(Paint.Cap.BUTT);
        mSmallPaint.setStyle(Paint.Style.FILL);
        mSmallPaint.setTextAlign(Align.RIGHT);

        mBigPaint = new TextPaint();
        mBigPaint.setColor(Color.WHITE);	
        mBigPaint.setAntiAlias(true);
        mBigPaint.setStrokeCap(Paint.Cap.BUTT);
        mBigPaint.setStyle(Paint.Style.FILL);
        mBigPaint.setTextAlign(Align.RIGHT);
        mBigPaint.setTypeface(Typeface.DEFAULT_BOLD);

        if( mDevice != null ){
            if( mDevice.getOrientation(mContext).equals(GlobalDef.HD) == true ){
                mLeft = mDevice.HD_OFFSET_X;
                mTop = mDevice.HD_OFFSET_Y;
                setOffset( mDevice.HD_OFFSET_X, mDevice.HD_OFFSET_Y );
            }
            else{
                mLeft = mDevice.LD_OFFSET_X;
                mTop = mDevice.LD_OFFSET_Y;
                setOffset( mDevice.LD_OFFSET_X, mDevice.LD_OFFSET_Y );
            }
        }
    }

    static public void setOffset( int x , int y ){
        mLeft = x;
        mTop  = y;
    }
}
