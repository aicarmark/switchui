/*
 * Copyright (C) 2012, Motorola, Inc,
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * 
 * Modification History: 
 **********************************************************
 * Date           Author       Comments
 * May 2, 2012	  MXDN83       Created file
 **********************************************************
 */

package com.motorola.contextual.pickers;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewParent;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListView;

/**
 * Custom linear layout that will allow custom layout for list items.
 * <code><pre>
 *
 * CLASS:
 *  extends LinearLayout - framework LinearLayout
 *
 * RESPONSIBILITIES:
 * Allows custom layouts that implements checkable interface, can be
 * used in a single/multi selection lists. Each list item can use this
 * layout to define a custom layout with checkable interface.
 *
 * COLLABORATORS:
 *  N/A
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class CustomLinearLayout extends LinearLayout implements Checkable {
    /**
     * Constructor to set custom linear layout, based on the list selection mode
     * either radio button or checkbox drawable is selected & cached.
     *
     * @param context activity context
     * @param attrs AttributeSet to construct the linear layout
     */
    public CustomLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = null;
        typedArray = context.getTheme().obtainStyledAttributes(new int[] {android.R.attr.listChoiceIndicatorMultiple});
        if ((typedArray != null) && (typedArray.length() > 0)) {
            mCheckDrawable = typedArray.getDrawable(0);
        }else {
            mCheckDrawable = null;
        }
        typedArray.recycle();
        // Cache the radio button drawable.
        typedArray = context.getTheme().obtainStyledAttributes(new int[] {android.R.attr.listChoiceIndicatorSingle});
        if ((typedArray != null) && (typedArray.length() > 0)) {
            mRadioDrawable = typedArray.getDrawable(0);
        }else {
            mRadioDrawable = null;
        }
        typedArray.recycle();
        mIsChecked = false;

    }

    private CheckedTextView mCheckedTextView;
    private final Drawable mCheckDrawable;
    private final Drawable mRadioDrawable;
    private boolean mIsChecked;

    /**
     * @see android.widget.Checkable#isChecked()
     */
    public boolean isChecked() {
        return mIsChecked;
    }

    /**
     * @see android.widget.Checkable#setChecked(boolean)
     */
    public void setChecked(boolean checked) {
        mIsChecked = checked;
        if (mCheckedTextView != null) {
            mCheckedTextView.setChecked(mIsChecked);
        }
    }

    /**
     * @see android.widget.Checkable#toggle()
     */
    public void toggle() {
        setChecked(!mIsChecked);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mCheckedTextView = (CheckedTextView) findViewById(android.R.id.text1);
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mCheckedTextView != null) {
            ViewParent p = getParent();
            if (p instanceof ListView) {
                int choiceMode = ((ListView) p).getChoiceMode();
                switch (choiceMode) {
                case ListView.CHOICE_MODE_MULTIPLE:
                    mCheckedTextView.setCheckMarkDrawable(mCheckDrawable);
                    break;

                case ListView.CHOICE_MODE_SINGLE:
                    mCheckedTextView.setCheckMarkDrawable(mRadioDrawable);
                    break;

                default:
                    mCheckedTextView.setCheckMarkDrawable(null);
                    break;
                }
                
            }
        }
    }
}
