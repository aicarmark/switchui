/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *                             Modification     Tracking
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * Li Shuqian/txvd74           04/12/2012                  Initial release
 */

package com.motorola.mmsp.performancemaster.ui;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.

import com.motorola.mmsp.performancemaster.R;

import android.content.Context;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import android.graphics.RectF;

import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

import android.view.View;

public class PercentView extends View {
    private final static String TAG = "PercentView";

    private Paint mPaintFillCircle;

    private Paint mTextPaint;
    private String mText = "";
    private int mTextSize;

    private RectF mRectF;

    private long mPercentCur = 0; // the current percent
    private long mPercentDraw; // the percent to display

    private Drawable mPercentBg, mPercentInside;
    private float mDensity;

    private final float BG_X = 0, BG_Y = 115, DRAWPER_X = 11, DRAWPER_Y = 104,
            DRAW_SIZE = 115, DRAWTEXT_X = 44, DRAWTEXT_Y = 63;
    private int mTextSizedp = 17;

    /**
     * Constructor. This version is only needed if you will be instantiating the
     * object manually (not from a layout XML file).
     * 
     * @param context
     */
    public PercentView(Context context) {
        super(context);

    }

    public PercentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /*
     * get the color thouth the index
     */
    private int getColor(int index) {

        switch (index) {
            case 1:
                return getResources().getColor(R.color.percentview_blue_color);
            case 2:
                return getResources().getColor(R.color.percentview_cambridge_blue_color);
            case 3:
                return getResources().getColor(R.color.percentview_yellow_color);
            case 4:
                return getResources().getColor(R.color.percentview_gray_color);
            default:
                return getResources().getColor(R.color.percentview_gray_color);
        }
    }

    public void initLabelView(int mColor) {

        mDensity = getResources().getDisplayMetrics().density; // the display
                                                               // density

        mPaintFillCircle = new Paint(); // the paint to draw the percent

        mPaintFillCircle.setAntiAlias(true);

        mPaintFillCircle.setColor(getColor(mColor));

        mPaintFillCircle.setStyle(Paint.Style.FILL);

        mRectF = new RectF();

        mRectF.top = dptopx(DRAWPER_X);
        mRectF.bottom = dptopx(DRAWPER_Y);

        mRectF.left = dptopx(DRAWPER_X);
        mRectF.right = dptopx(DRAWPER_Y);

        Log.e(TAG,
                " mRectF.toString() ="
                        + mRectF.toString());

        mTextPaint = new Paint(); // the paint to draw the text
        mTextPaint.setAntiAlias(true);

        // Must manually scale the desired text size to match screen density
        mTextPaint.setTextSize(mTextSizedp * mDensity);

        mTextSize = (int) (mTextSizedp * mDensity);

        Log.e(TAG,
                " setTextSize = " + mTextSize +
                        " mRectF.toString()= " + mRectF.toString());
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setShadowLayer(1f, 1f, 0, Color.BLACK);

        mPercentBg = getResources().getDrawable(R.drawable.memory_ap_bg);
        mPercentInside = getResources().getDrawable(R.drawable.memory_blue);

        Rect mNowRect = new Rect(); // rect to draw the backgroud and the
                                    // pervent draw on it
        mNowRect.top = dptopx(BG_X);
        mNowRect.bottom = dptopx(BG_Y);

        mNowRect.left = dptopx(BG_X);
        mNowRect.right = dptopx(BG_Y);

        mPercentBg.setBounds(mNowRect);
        Log.e(TAG,
                " mNowRect.toString() ="
                        + mNowRect.toString());

        mPercentInside.setBounds(mNowRect);

        //
        // dp to px:
        //
        // displayMetrics = context.getResources().getDisplayMetrics();
        // return (int)((dp * displayMetrics.density) + 0.5);
        //
        // px to dp:
        //
        // displayMetrics = context.getResources().getDisplayMetrics();
        // return (int) ((px/displayMetrics.density)+0.5);
        //
        //

    }

    private int dptopx(float dp) {
        return (int) ((dp * mDensity) + 0.5);

    }

    /*
     * set the text to display the percent like: 20%
     */
    public void setUsedPerText(long per, String strPer) {

        mPercentCur = per;

        mText = strPer;

        requestLayout();
        invalidate();
    }

    /**
     * @see android.view.View#measure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec),
                measureHeight(heightMeasureSpec));
    }

    /**
     * Determines the width of this view
     * 
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {

            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by
                // measureSpec
                result = Math.min(result, specSize);
            }
        }

        return dptopx(DRAW_SIZE);
    }

    /**
     * Determines the height of this view
     * 
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = specSize;

            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by
                // measureSpec
                result = Math.min(result, specSize);
            }
        }

        return dptopx(DRAW_SIZE);
    }

    /**
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int mtextlength = mText.length();
        float mTextx = DRAWTEXT_X;
        if(mtextlength == 2){
            mTextx += 4;
        }else if(mtextlength == 4){
            mTextx -= 6;
        }

        mPercentBg.draw(canvas);

        if (mPercentDraw > mPercentCur) {
            mPercentDraw--;
            invalidate();
        } else {
            // mPerPercent++;
            // mPerPercent += 8;

            // while(mPerPercent > mPercent)
            // mPerPercent--;
            mPercentDraw = mPercentCur;
        }

        canvas.drawArc(mRectF, -90, mPercentDraw, true, mPaintFillCircle);

        mPercentInside.draw(canvas);
        
        canvas.drawText(mText, dptopx(mTextx),
                dptopx(DRAWTEXT_Y),
                mTextPaint);

    }
}
