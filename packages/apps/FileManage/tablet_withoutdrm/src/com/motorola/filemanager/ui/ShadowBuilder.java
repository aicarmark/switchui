/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR              Author      Description
 * 2011-11-04       XQH748      initial
 */

package com.motorola.filemanager.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.view.View;
import android.view.View.DragShadowBuilder;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.R;

//Drag & drop UI
public class ShadowBuilder extends DragShadowBuilder {
    private Drawable sBackground = null;
    /** Paint information for the move/copy text */
    private TextPaint sTextPaint = null;
    /** The x location of any touch event; used to ensure the drag overlay is drawn correctly */
    private int sTouchX;

    /** Width of the draggable view */
    private final int mDragWidth;
    /** Height of the draggable view */
    private final int mDragHeight;

    private String mText;
    private PointF mTextPoint;
    private Context mContext;

    private String mCountText;

    private int mOldOrientation = Configuration.ORIENTATION_UNDEFINED;

    /** Margin applied to the right of count text */
    private float sCountMargin = 0;

    /** Vertical offset of the drag view */
    private int sDragOffset;

    public ShadowBuilder(View view, int count, Context context) {
        super(view);
        Resources res = view.getResources();
        int newOrientation = res.getConfiguration().orientation;
        mContext = context;

        if (((FileManagerApp) mContext.getApplicationContext()).getViewMode() == FileManagerApp.GRIDVIEW) {
            mDragWidth = view.getWidth();
            mDragHeight = view.getHeight() / 2;
        } else {
            mDragWidth = view.getWidth() / 2;
            mDragHeight = view.getHeight();
        }

        // TODO: Can we define a layout for the contents of the drag area?
        if (mOldOrientation != newOrientation) {
            mOldOrientation = newOrientation;

            sBackground = res.getDrawable(R.drawable.bg_dragdrop);
            sBackground.setBounds(0, 0, mDragWidth, mDragHeight);

            sDragOffset = (int) res.getDimension(R.dimen.item_list_drag_offset);

            sTextPaint = new TextPaint();
            float textSize;
            textSize = res.getDimension(R.dimen.sel_item_list_drag_text_font_size);
            sTextPaint.setTextSize(textSize);
            sTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
            sTextPaint.setAntiAlias(true);

        }

        // Calculate layout positions
        Rect b = new Rect();
        //mText = res.getQuantityString(R.plurals.move_messages, count, count);
        mCountText = res.getQuantityString(R.plurals.item, count, count);
        mText = "   " + /* mContext.getString(R.string.move_shadow) + " " +*/mCountText;
        if (sTextPaint != null) {
            sTextPaint.getTextBounds(mText, 0, mText.length(), b);
        }
        mTextPoint = new PointF(sCountMargin, (float) (mDragHeight - b.top) / 2);

    }

    public void setTouchX(int touchX) {
        sTouchX = touchX;
    }

    public int getTouchX() {
        return sTouchX;
    }

    @Override
    public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
        shadowSize.set(mDragWidth, mDragHeight);
        shadowTouchPoint.set(sTouchX + mDragWidth / 2, (mDragHeight / 2) + sDragOffset);
    }

    @Override
    public void onDrawShadow(Canvas canvas) {
        super.onDrawShadow(canvas);
        sBackground.draw(canvas);
        canvas.drawText(mText, mTextPoint.x, mTextPoint.y, sTextPaint);
        //canvas.drawText(mCountText, mCountPoint.x, mCountPoint.y, sCountPaint);
        /* canvas.drawText(mText, mCountPoint.x, mCountPoint.y, sTextPaint);
         canvas.drawText(mCountText, mTextPoint.x, mTextPoint.y, sCountPaint);*/
    }
}
