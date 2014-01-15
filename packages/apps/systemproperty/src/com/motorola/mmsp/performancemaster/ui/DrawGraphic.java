/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * pjw346                       04/09/2012                  Initial release 
 *         
 */

package com.motorola.mmsp.performancemaster.ui;

import com.motorola.mmsp.performancemaster.engine.Log;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLongClickListener;
import java.util.ArrayList;

@SuppressWarnings("unused")
public class DrawGraphic extends View implements OnLongClickListener {

    private final static String TAG = "DrawGraphic";

    private Paint mPaintGraphicBackgroud, mPaintFillMarginBackgroud, mPaintLines, mPaintText,
            mPaintDrawLine;

    private int mXLeft, mXRight, mYTop, mYBottom, mGraphicHeight, mGraphicWidth;

    private int mXSpace = 10, mYSpace = 10;

    private int mPointInterval = 5, mTotalPointIntervals = 10;

    private ArrayList<Float> mCpuUsed;

    private Paint[][] mPaintSkin = {
            /*
             * paintGraphicBackgroud, paintFillMarginBackgroud, paintLines,
             * paintCoordinate, paintText, paintDrawLine,
             */
            {
                    getPaint(Color.BLACK, Paint.Align.CENTER, 4, false),
                    getPaint(Color.BLACK, Paint.Align.CENTER, 4, false),
                    getPaint(Color.parseColor("#007D39"), Paint.Align.CENTER, 1, false),
                    getPaint(Color.BLACK, Paint.Align.RIGHT, 8, true),
                    getPaint(Color.GREEN, Paint.Align.RIGHT, 2, true),
            },// first skin
            {
                    getPaint(Color.parseColor("#B0C4DE"), Paint.Align.CENTER, 4, false),
                    getPaint(Color.BLACK, Paint.Align.CENTER, 4, false),
                    getPaint(Color.BLACK, Paint.Align.CENTER, 1, false),
                    getPaint(Color.BLUE, Paint.Align.RIGHT, 8, true),
                    getPaint(Color.parseColor("#FFD700"), Paint.Align.RIGHT, 2, true),
            },// second skin
            {
                    getPaint(Color.parseColor("#DEB887"), Paint.Align.CENTER, 4, false),
                    getPaint(Color.BLACK, Paint.Align.CENTER, 4, false),
                    getPaint(Color.parseColor("#FFA500"), Paint.Align.CENTER, 1, false),
                    getPaint(Color.parseColor("#FF4500"), Paint.Align.RIGHT, 8, true),
                    getPaint(Color.parseColor("#006400"), Paint.Align.RIGHT, 2, true),
            },// third skin
    };

    private int nChoiceSkin = 0;

    // This constructor must be specified when the view is loaded from a xml
    // file, like in this case.
    public DrawGraphic(Context context, AttributeSet attrs) {
        super(context, attrs);

        initializeGraphic();
    }

    public void updateCpuVector(ArrayList<Float> alCpuUsed) {
        this.mCpuUsed = alCpuUsed;
    }

    /**
     * @param rGraphic the size of graphic
     * @param pSpace the margin between graphic and data draw area
     */
    public void setDrawGraphicParams(Rect rGraphic, Point pSpace) {

        mXLeft = (int) rGraphic.left;
        mXRight = (int) rGraphic.right;
        mYTop = (int) rGraphic.top;
        mYBottom = (int) rGraphic.bottom;
        mGraphicWidth = mXRight - mXLeft;
        mGraphicHeight = mYBottom - mYTop;

        // adjust x_space and xRight
        if (mGraphicWidth % 10 > 0) {
            mXRight = (int) (rGraphic.right - (mGraphicWidth % 10));
            mXSpace = pSpace.x + (mGraphicWidth % 10);
            mGraphicWidth = mXRight - mXLeft;
        } else {
            mXSpace = pSpace.x;
        }

        // adjust y_space and yBottom
        if (mGraphicHeight % 10 > 0) {
            mYBottom = (int) (rGraphic.bottom - (mGraphicHeight % 10));
            mYSpace = pSpace.y + (mGraphicHeight % 10);
            mGraphicHeight = mYBottom - mYTop;
        } else {
            mYSpace = pSpace.y;
        }

        // calc point total in graphic
        mTotalPointIntervals = (int) Math.ceil(mGraphicWidth / mPointInterval);

        // set default skin
        initPaintSkin(nChoiceSkin);

        // redraw graphic
        this.requestLayout();
        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Graphic background
        canvas.drawRect(new Rect(mXLeft, mYTop, mXRight, mYBottom), mPaintGraphicBackgroud);

        // Horizontal graphic grid lines
        for (int m = 1; m < mGraphicHeight / 20; m++) {
            float fY = (float) (mYTop + (float) ((mYBottom - mYTop) / (mGraphicHeight / 20 /* 10 */))
                    * m);
            canvas.drawLine(mXLeft, fY, mXRight, fY, mPaintLines);
        }

        // Vertical graphic grid lines
        for (int n = 1; n < mGraphicWidth / 20; n++) {
            float fX = (float) (mXLeft + (float) ((mXRight - mXLeft) / (mGraphicWidth / 20/* 10 */))
                    * n);
            canvas.drawLine(fX, mYTop, fX, mYBottom, mPaintLines);
        }

        // Graphic background
        canvas.drawRect(new Rect(mXLeft - mXSpace, mYTop, mXLeft, mYBottom),
                mPaintFillMarginBackgroud);// left
        canvas.drawRect(new Rect(mXRight, mYTop, mXRight + mXSpace, mYBottom),
                mPaintFillMarginBackgroud);// right
        canvas.drawRect(
                new Rect(mXLeft - mXSpace, mYBottom, mXRight + mXSpace, mYBottom + mYSpace),
                mPaintFillMarginBackgroud);// bottom
        canvas.drawRect(new Rect(mXLeft - mXSpace, mYTop - mYSpace, mXRight + mXSpace, mYTop),
                mPaintFillMarginBackgroud);// top

        // Vertical edges
        canvas.drawLine(mXLeft, mYTop, mXLeft, mYBottom, mPaintLines);
        canvas.drawLine(mXRight, mYBottom, mXRight, mYTop, mPaintLines);

        // Horizontal edges
        canvas.drawLine(mXLeft, mYTop, mXRight, mYTop, mPaintLines);
        canvas.drawLine(mXLeft, mYBottom, mXRight, mYBottom, mPaintLines);

        // draw curve
        if (mCpuUsed != null) {
            // draw CPU used
            drawLineFloat(mCpuUsed, canvas, mPaintDrawLine);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    @Override
    public boolean onLongClick(View v) {
        if (nChoiceSkin++ == mPaintSkin.length - 1)
            nChoiceSkin = 0;

        initPaintSkin(nChoiceSkin);

        return false;
    }

    private void initializeGraphic() {
        // set default value
        mXLeft = (int) mXSpace;
        mXRight = (int) mXSpace + 100;
        mYTop = (int) mYSpace;
        mYBottom = (int) mYSpace + 100;
        mGraphicWidth = mXRight - mXLeft;
        mGraphicHeight = mYBottom - mYTop;
        mTotalPointIntervals = (int) Math.ceil(mGraphicWidth / mPointInterval);

        // set default skin
        initPaintSkin(nChoiceSkin);
    }

    private void initPaintSkin(int nSkin) {
        // adjust text size
        mPaintSkin[nSkin][4].setTextSize((float) mYSpace - 6);

        mPaintGraphicBackgroud = mPaintSkin[nSkin][0];
        mPaintFillMarginBackgroud = mPaintSkin[nSkin][1];
        mPaintLines = mPaintSkin[nSkin][2];
        mPaintText = mPaintSkin[nSkin][3];
        mPaintDrawLine = mPaintSkin[nSkin][4];

    }

    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            // Measure the view
            result = (int) (2 * mXSpace + mGraphicWidth);

            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by
                // measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    /**
     * Determines the height of this view
     * 
     * @param measureSpec A measureSpec packed into an int type
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
            result = (int) (2 * mYSpace + mGraphicHeight);

            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by
                // measureSpec
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    private void drawLineFloat(ArrayList<Float> y, Canvas canvas, Paint paint) {
                
        if (y.size() > 1) {
            for (int m = 0; m < (y.size() - 1) && m < mTotalPointIntervals; m++) {
                canvas.drawLine(mXRight - mPointInterval * m, mYBottom - y.get(m).floatValue()
                        * mGraphicHeight / 100, mXRight - mPointInterval * m - mPointInterval,
                        mYBottom - y.get(m + 1).floatValue() * mGraphicHeight / 100, paint);
            }
        }
        else if (y.size() == 1)
        {
            float []pts = {mXRight-1.0f, 
                           mYBottom - y.get(0).floatValue()* mGraphicHeight / 100};
            canvas.drawPoints(pts, paint);
        }
    }

    private Paint getPaint(int color, Paint.Align align, int nSize, boolean b) {
        Paint p = new Paint();
        p.setColor(color);
        p.setStrokeWidth((float) nSize);
        p.setTextSize((float) nSize);
        p.setTextAlign(align);
        p.setAntiAlias(b);
        return p;
    }
}
