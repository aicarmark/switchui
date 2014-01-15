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
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;
import android.widget.LinearLayout;
import android.widget.Scroller;

public class ColumnView extends LinearLayout {

    public boolean mAlwaysOverrideTouch = true;
    protected ColumnViewFrameAdapter mAdapter;
    protected int mCurrentX;
    protected int mNextX;
    private int mMaxX = Integer.MAX_VALUE;
    private int mDisplayOffset = 0;
    protected Scroller mScroller;
    protected boolean mIsSomeDrag;
    private GestureDetector mGesture;
    private boolean mDataChanged = false;

    public ColumnView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    private synchronized void initView() {
        mDisplayOffset = 0;
        mCurrentX = 0;
        mNextX = 0;
        mMaxX = Integer.MAX_VALUE;
        mScroller = new Scroller(getContext());
        mGesture = new GestureDetector(getContext(), mOnGesture);
    }

    private DataSetObserver mDataObserver = new DataSetObserver() {

        @Override
        public void onChanged() {
            synchronized (ColumnView.this) {
                mDataChanged = true;
            }
            invalidate();
            requestLayout();
        }

        @Override
        public void onInvalidated() {
            reset();
            invalidate();
            requestLayout();
        }

    };
    protected boolean isScroll = false;

    public ColumnViewFrameAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(ColumnViewFrameAdapter adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataObserver);
        }
        mAdapter = adapter;
        mAdapter.registerDataSetObserver(mDataObserver);
        reset();
    }

    private synchronized void reset() {
        initView();
        removeAllViewsInLayout();
        requestLayout();
    }

    private void positionItems(final int dx) {
        if (getChildCount() > 0) {
            mDisplayOffset += dx;
            int left = mDisplayOffset;
            int right = 0;
            int temp = isAnyDragging();

            if (temp != -1) {
                View child = getChildAt(temp);
                left = child.getLeft();
                right = child.getRight();
                /*if(temp ==0){
                    for (int i = 0; i < getChildCount(); i++) {
                        child = getChildAt(i);
                        int childWidth = child.getMeasuredWidth();
                        child.layout(0, 0, childWidth, child.getMeasuredHeight());                    
                        left += childWidth;
                    }
                }else{*/
                if ((temp == getChildCount() - 1)) {
                    right = getWidth();
                }
                for (int i = temp; i >= 0; i--) {
                    child = getChildAt(i);
                    int childWidth = child.getMeasuredWidth();
                    left = right - childWidth;
                    child.layout(left, 0, right, child.getMeasuredHeight());
                    right -= childWidth;
                }
                //}

                //((SplitView) (child)).setDragging(false);
            } else {

                int right1 = getWidth() + mDisplayOffset;
                if (mDataChanged) {
                    //mDataChanged = false;
                    for (int i = getChildCount() - 1; i >= 0; i--) {
                        View child = getChildAt(i);
                        int childWidth = child.getMeasuredWidth();

                        if (i == 0 && (right1 - childWidth > 0)) {
                            child.layout(0, 0, right1, child.getMeasuredHeight());
                        } else {
                            child.layout(right1 - childWidth, 0, right1, child.getMeasuredHeight());
                            //}
                            /*if (i == 0) {                               
                                mMinX = mCurrentX + child.getLeft();                                
                            }*/
                            if (i == getChildCount() - 1) {
                                //mMaxX = mCurrentX+right1;
                                mMaxX = mCurrentX + right1 - getWidth();
                                //mDisplayOffset -= child.getMeasuredWidth();
                            }
                            right1 -= childWidth;
                        }
                    }
                }

                else {
                    for (int i = 0; i < getChildCount(); i++) {
                        View child = getChildAt(i);
                        int childWidth = child.getMeasuredWidth();

                        getWidth();

                        //int offset  = r-g;
                        child.layout(left, 0, left + childWidth, child.getMeasuredHeight());
                        left += childWidth;
                        /*if (i == 0) {                            
                           mMinX = mCurrentX + child.getLeft();                            
                        }*/
                        if (i == mAdapter.getCount() - 1) {
                            getWidth();
                            mMaxX = mCurrentX + child.getRight() - getWidth() + 50;
                        }

                        //}
                    }

                }
            }
        }

    }

    @Override
    protected synchronized void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (mAdapter == null) {
            return;
        }

        if (mScroller.computeScrollOffset()) {
            int scrollx = mScroller.getCurrX();
            mNextX = scrollx;
        }

        if (mNextX < 0) {
            mNextX = 0;
            mScroller.forceFinished(true);
        }
        if (mNextX > mMaxX) {
            mNextX = mMaxX;
            mScroller.forceFinished(true);
        }

        int dx = mCurrentX - mNextX;
        positionItems(dx);

        mCurrentX = mNextX;

        if (!mScroller.isFinished()) {
            post(new Runnable() {
                @Override
                public void run() {
                    requestLayout();

                }
            });
        }
    }

    private int isAnyDragging() {
        int isDrag = -1;
        try {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getClass().getName().equals(ResizableView.class.getName())) {
                    if (((ResizableView) child).isDragging()) {
                        isDrag = i;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isDrag;
    }

    public void setAnyDragging(boolean isDrag) {
        mIsSomeDrag = isDrag;
    }

    public boolean getAnyDragging() {
        return mIsSomeDrag;
    }

    public void scrollTo(int distanceX) {
        mDataChanged = false;
        synchronized (ColumnView.this) {
            mNextX += distanceX;
        }
        isScroll = true;
        requestLayout();
    }

    public int getFirstVisiblePosition() {
        int pos = -1;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getRight() >= 0) {
                pos = i;
                return pos;
            }

        }
        return pos;
    }

    public int getLastVisiblePosition() {
        int pos = -1;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getLeft() >= getWidth()) {
                pos = i;
                return pos;
            }

        }
        return pos;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mGesture.onTouchEvent(ev);
        return true;

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        mGesture.onTouchEvent(ev);
        return false;

    }

    protected boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        synchronized (ColumnView.this) {
            mScroller.fling(mNextX, 0, (int) -velocityX, 0, 0, mMaxX, 0, 0);
        }
        requestLayout();

        return true;
    }

    protected boolean onDown(MotionEvent e) {
        mScroller.forceFinished(true);
        return true;
    }

    private OnGestureListener mOnGesture = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            if (e1.getAction() == MotionEvent.AXIS_VSCROLL) {
                return super.onFling(e1, e2, velocityX, velocityY);
            }
            return ColumnView.this.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            float y1 = e1.getY();
            float y2 = e2.getY();
            float x1 = e1.getX();
            float x2 = e2.getX();
            mDataChanged = false;
            float abs_y = Math.abs(y1 - y2);
            float abs_x = Math.abs(x1 - x2);

            if (abs_y > abs_x) {
                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            synchronized (ColumnView.this) {
                mNextX += (int) distanceX;
            }
            isScroll = true;
            requestLayout();

            return true;

        }

    };

}
