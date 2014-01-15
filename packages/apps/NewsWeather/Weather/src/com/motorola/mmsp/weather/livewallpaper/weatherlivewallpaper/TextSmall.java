package com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper;

import java.util.ArrayList;

import com.motorola.mmsp.weather.livewallpaper.utility.Device;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextPaint;

public class TextSmall extends TextBase {

    public TextSmall( Canvas c, Context ct, Device d ){
        super(c, ct, d );

        initPaint();
    }

    @Override
    public void drawText( int index, String str, int type ) {
        super.drawText( index, str, type );
    }

    @Override
    protected TextPaint getTypePaint( int type ){
        switch( type ){
            case BIG:
                return mBigPaint;
            case SMALL:
                return mSmallPaint;
            default:
                return mBigPaint;
        }
    }

    public void initPaint(){
        super.initPaint();

        try{
            mBigPaint.setStrokeWidth(2);
            mBigPaint.setTextSize(mDevice.SMALL_BIG_TEXT_SIZE);

            mSmallPaint.setStrokeWidth(1);
            mSmallPaint.setTextSize(mDevice.SMALL_SMALL_TEXT_SIZE);	

            int spacing = mDevice.SMALL_SPACING;
            mLineSpacing = new ArrayList<Float>();
            mLineSpacing.add( mTop );
            mLineSpacing.add( mTop+spacing*1 );
            mLineSpacing.add( mTop+spacing*2 );
            mLineSpacing.add( mTop+spacing*3 );
            mLineSpacing.add( mTop+spacing*3+33 );
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
