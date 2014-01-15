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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.R;

public class ResizableView extends LinearLayout implements OnTouchListener {

    private int mHandleId;
    private View mHandle;
    private int mPrimaryContentId;
    private View mPrimaryContent;

    private boolean mDragging;
    private boolean mIsResized = false;
    private float mDragStartX;
    private int mnewWidth = -1;

    boolean isDrage = false;
    private int mPosition = -1;
    private String TAG = "ResizableView";

    public ResizableView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray viewAttrs = context.obtainStyledAttributes(attrs, R.styleable.ResizableView);

        RuntimeException e = null;
        mHandleId = viewAttrs.getResourceId(R.styleable.ResizableView_handle, 0);
        if (mHandleId == 0) {
            e =
                    new IllegalArgumentException(viewAttrs.getPositionDescription() +
                            ": The required attribute handle must refer to a valid child view.");
        }

        mPrimaryContentId = viewAttrs.getResourceId(R.styleable.ResizableView_content, 0);
        if (mPrimaryContentId == 0) {
            e =
                    new IllegalArgumentException(viewAttrs.getPositionDescription() +
                            ": The required attribute primaryContent must refer to a valid child view.");
        }
        viewAttrs.recycle();

        if (e != null) {
            throw e;
        }

    }

    void setnewWidth(int newWidth) {
        mnewWidth = newWidth;
    }

    int getnewWidth() {
        return mnewWidth;
    }

    public boolean isDragging() {
        return mDragging;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mHandle = findViewById(mHandleId);
        if (mHandle == null) {
            String name = getResources().getResourceEntryName(mHandleId);
            throw new RuntimeException(
                    "Your Panel must have a child View whose id attribute is 'R.id." + name + "'");

        }
        mPrimaryContent = findViewById(mPrimaryContentId);
        if (mPrimaryContent == null) {
            String name = getResources().getResourceEntryName(mPrimaryContentId);
            throw new RuntimeException(
                    "Your Panel must have a child View whose id attribute is 'R.id." + name + "'");

        }
        mHandle.setOnTouchListener(this);

    }

    public void setPosition(int pos) {
        mPosition = pos;
    }

    public int getPosition() {
        return mPosition;
    }

    public void setDragging(boolean val) {
        mDragging = val;
        ColumnView CLView = (ColumnView) this.getParent();
        CLView.setAnyDragging(val);
    }

    @Override
    public boolean onTouch(View view, MotionEvent me) {
        try {
            // Only capture drag events if we start
            if (view != mHandle) {
                return false;
            }
            if (me.getAction() == MotionEvent.ACTION_DOWN) {
                mDragging = true;
                mDragStartX = me.getX();
                ((View) view.getParent()).findViewById(R.id.arrow_left).setVisibility(View.VISIBLE);
                ((View) view.getParent()).findViewById(R.id.arrow_right)
                        .setVisibility(View.VISIBLE);
                ((View) view.getParent()).findViewById(R.id.arrow_left2)
                        .setVisibility(View.VISIBLE);
                ((View) view.getParent()).findViewById(R.id.arrow_right2).setVisibility(
                        View.VISIBLE);
                return true;
            } else if (me.getAction() == MotionEvent.ACTION_UP) {
                mDragging = false;
                ((View) view.getParent()).findViewById(R.id.arrow_left).setVisibility(View.GONE);
                ((View) view.getParent()).findViewById(R.id.arrow_right).setVisibility(View.GONE);
                ((View) view.getParent()).findViewById(R.id.arrow_left2).setVisibility(View.GONE);
                ((View) view.getParent()).findViewById(R.id.arrow_right2).setVisibility(View.GONE);
            } else if (me.getAction() == MotionEvent.ACTION_MOVE) {
                if (mDragStartX != me.getX()) {
                    int offset = (int) (me.getX() - mDragStartX);
                    int widd = getWidth();
                    setPrimaryContentWidth(widd + offset);
                }
                return true;
            }
        } catch (Exception e) {
            FileManagerApp.log(TAG + e.getMessage(), true);
            e.printStackTrace();
        }
        return true;
    }

    public int getPrimaryContentSize() {

        return mPrimaryContent.getMeasuredWidth();

    }

    public boolean setPrimaryContentSize(int newSize) {
        return setPrimaryContentWidth(newSize);

    }

    public boolean isResized() {
        return mIsResized;
    }

    private void setIsResized(boolean isResize) {
        mIsResized = isResize;
    }

    private boolean setPrimaryContentWidth(int newWidth) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) getLayoutParams();
        int width = params.width;

        if (newWidth < 200) {
            newWidth = 200;
        }
        params.width = newWidth;

        setLayoutParams(params);
        setnewWidth(width);
        isDrage = true;
        setIsResized(true);
        return true;
    }

};
