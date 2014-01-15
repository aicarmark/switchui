/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * pjw346                       05/10/2012                  Initial release 
 *         
 */

package com.motorola.mmsp.performancemaster.ui;


import com.motorola.mmsp.performancemaster.engine.Log;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TabHost;
import android.widget.TextView;

public class MainTabHost extends TabHost implements OnGestureListener {

    private String TAG = "MainTabHost";

    private static final int DELTA_FLEEP = 200;

    private GestureDetector mGestureDetector = null;

    public MainTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);

        mGestureDetector = new GestureDetector(context, this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event)) {
            event.setAction(MotionEvent.ACTION_CANCEL);
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void addTab(TabSpec tabSpec) {
        super.addTab(tabSpec);
        
        //tabWidget.getChildAt(nIndex).getLayoutParams().height = 48;
        TextView tv = (TextView) getTabWidget().getChildAt(getTabWidget().getChildCount()-1)
                      .findViewById(android.R.id.title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        tv.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        tv.setText(tv.getText().toString().toUpperCase());
        tv.setSingleLine(true);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        int nCurrentTabId = 0;
        if (e1.getX() - e2.getX() <= -DELTA_FLEEP) {// from left to right
            nCurrentTabId = getCurrentTab() - 1;
            if (nCurrentTabId < 0) {
                nCurrentTabId = getTabWidget().getTabCount() - 1;
            }
            setCurrentTab(nCurrentTabId);
        } else if (e1.getX() - e2.getX() >= DELTA_FLEEP) {// from right to left
            nCurrentTabId = getCurrentTab() + 1;
            if (nCurrentTabId >= getTabWidget().getTabCount()) {
                nCurrentTabId = 0;
            }
            setCurrentTab(nCurrentTabId);
        }
        
        Log.e(TAG," onFling(), CurrentTabId=" + nCurrentTabId);

        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }
}
