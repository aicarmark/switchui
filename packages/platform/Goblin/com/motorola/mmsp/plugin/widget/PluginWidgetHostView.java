/**
 * FILE: PluginWidgetHostView.java
 *
 * DESC: The host view is to house plugin customized layout.
 * 
 * Widget plugin would be formated as Jar / APK packages.
 */

package com.motorola.mmsp.plugin.widget;

import android.util.Log;
import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;


public class PluginWidgetHostView extends FrameLayout {
    /**
     * 
     */
    private int mId;
    
    /**
     * 
     */
    private boolean mHasPerformedLongPress;
    
    /**
     * 
     */
    private CheckForLongPress mPendingCheckForLongPress;
    
    /**
     * 
     */
    private View mView = null;

    /**
     * @param mPluginWidget
     * @param view
     */
    public PluginWidgetHostView(PluginWidget mPluginWidget, View view) {
        super(view.getContext());
        // TODO Auto-generated constructor stub

        mView = view;
        android.widget.FrameLayout.LayoutParams layoutparams = new android.widget.FrameLayout.LayoutParams(
                -1, -1);
        addView(view,layoutparams);
    }
    
    
    /**
     *
     * @return int
     */
    public int getPluginWidgetHostId() {
    	return mId;
    }
    
    /**
     *
     * @return void
     */
    public void setPluginWidgetHostId(int id) {
    	mId = id;
    }
    
    /* (non-Javadoc)
     * @see android.view.ViewGroup#onInterceptTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Consume any touch events for ourselves after longpress is triggered
        if (mHasPerformedLongPress) {
            mHasPerformedLongPress = false;
            return true;
        }
            
        // Watch for longpress events at this level to make sure
        // users can always pick up this widget
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                postCheckForLongClick();
                break;
            }
            
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mHasPerformedLongPress = false;
                if (mPendingCheckForLongPress != null) {
                    removeCallbacks(mPendingCheckForLongPress);
                }
                break;
        }
        
        // Otherwise continue letting touch events fall through to children
        return false;
    }
    
    class CheckForLongPress implements Runnable {
        private int mOriginalWindowAttachCount;

        public void run() {
            if (hasWindowFocus()
                    && mOriginalWindowAttachCount == getWindowAttachCount()
                    && !mHasPerformedLongPress) {
                if (performLongClick()) {
                    mHasPerformedLongPress = true;
                }
            }
        }

        public void rememberWindowAttachCount() {
            mOriginalWindowAttachCount = getWindowAttachCount();
        }
    }

    /**
     * return void 
     */
    private void postCheckForLongClick() {
        mHasPerformedLongPress = false;

        if (mPendingCheckForLongPress == null) {
            mPendingCheckForLongPress = new CheckForLongPress();
        }
        mPendingCheckForLongPress.rememberWindowAttachCount();
        postDelayed(mPendingCheckForLongPress, ViewConfiguration.getLongPressTimeout());
    }

    /* (non-Javadoc)
     * @see android.view.View#cancelLongPress()
     */
    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mHasPerformedLongPress = false;
        if (mPendingCheckForLongPress != null) {
            removeCallbacks(mPendingCheckForLongPress);
        }
    }
    
    
    /**
     * 
     * @return PluginWidgetDragListener
     */
    public PluginWidgetDragListener getDragListener(){
        return (mView instanceof PluginWidgetDragListener) ? (PluginWidgetDragListener)mView : null;
    }
    
    /**
     * 
     * @return PluginWidgetModelListener
     */
    public PluginWidgetModelListener getModelListener(){
        return (mView instanceof PluginWidgetModelListener) ? (PluginWidgetModelListener)mView : null;
    }
    
    /**
     * 
     * @return PluginWidgetScrollListener
     */
    public PluginWidgetScrollListener getScrollListener(){
        return (mView instanceof PluginWidgetScrollListener) ? (PluginWidgetScrollListener)mView : null;
    }
    
    /**
     * 
     * @return PluginWidgetStatusListener
     */
    public PluginWidgetStatusListener getStatusListener(){
        return (mView instanceof PluginWidgetStatusListener) ? (PluginWidgetStatusListener)mView : null;
    }
    
    
    
}

