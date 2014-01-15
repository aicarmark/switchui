/*
 * Copyright (C) 2012, Motorola, Inc,
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * May 8, 2012	MXDN83       Created file
 **********************************************************
 */

package com.motorola.contextual.pickers;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View.OnClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 * This custom list item class encapsulates all the data
 * required to render each list item in the pickers.
 * This is intended for views with a
 * primary TextView, a secondary TextView, an ImageView,
 * a specific clickable View, a seekbar and a View that is only
 * shown if a non-null View.OnClickListener is supplied.
 * <code><pre>
 *
 * CLASS:
 *
 * RESPONSIBILITIES:
 * Encapsulates a list item
 *
 * COLLABORATORS:
 *  N/A
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class ListItem {
    public int mIconDrawableResID;
    public Drawable mIcon;
    public Uri mIconUri = null;
    public CharSequence mLabel;
    public CharSequence mDesc;
    public int mChipColor;
    public int mType;
    //This variable defines what this item represents
    //For example: this could be used as mode constants for
    //Airplane, Charging, Display, Dock, Storage etc
    //Or any other object that needs to be associated with
    //the item to be used when constructing the rule.
    public Object mMode;
    public OnClickListener mLabelClickListener;
//    public OnSeekBarChangeListener mSeekBarChangeListener;
    public Boolean mEnabled = true;
    public SeekBarParams mSeekBarParams;
    // Type 1 - no label click listener
    public static int typeONE = 1;
    // Type 2 - selecting label does some action
    public static int typeTWO = 2;
    // Type 3 - no checkbox/radio button
    public static int typeTHREE = 3;
    // Type 4 - slider item, which has label text and a slider underneath
    public static int typeFOUR = 4;
    // Type 5 - slider item, which has label text and a slider underneath and no checkbox/radio button
    public static int typeFIVE = 5;

    public ListItem(int resId, CharSequence label, CharSequence desc, int type,
            Object item, OnClickListener listener) {
    	this(resId, label, desc, type, item, listener, 0);
    }

    public ListItem(int resId, CharSequence label, CharSequence desc, int type,
            Object item, OnClickListener listener, int chipColor) {
        mIconDrawableResID = resId;
        mLabel = label;
        mDesc = desc;
        mChipColor = chipColor;
        mType = type;
        mMode = item;
        mLabelClickListener = listener;
    }

    public ListItem(Drawable icon, CharSequence label, CharSequence desc, int type,
            Object item, OnClickListener listener) {
        mIcon = icon;
        mLabel = label;
        mDesc = desc;
        mType = type;
        mMode = item;
        mLabelClickListener = listener;
    }

    public ListItem(int resId, CharSequence label, int type, Object item,
            int seekBarMax, OnSeekBarChangeListener seekBarListener) {
        mIconDrawableResID = resId;
        mLabel = label;
        mType = type;
        mMode = item;
        if(seekBarListener != null) {
            mSeekBarParams = new SeekBarParams();
            mSeekBarParams.max = seekBarMax;
            mSeekBarParams.seekBarChangeListener = seekBarListener;
        }
    }

    public ListItem(CharSequence label, CharSequence desc, int type,
            Object item, OnClickListener listener, Uri iconUri) {
        mIconUri = iconUri;
        mIconDrawableResID = -1;
        mIcon = null;
        mLabel = label;
        mDesc = desc;
        mType = type;
        mMode = item;
        mLabelClickListener = listener;
    }

    public class SeekBarParams {
        int max;
        public int currentProgress;
        public OnSeekBarChangeListener seekBarChangeListener;
   }
}
