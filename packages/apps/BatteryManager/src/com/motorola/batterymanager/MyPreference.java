/** 
 * Copyright (C) 2009, Motorola, Inc, 
 * All Rights Reserved 
 * Class name: MyPreference.java 
 * Description: What the class does. 
 * 
 * Modification History: 
 **********************************************************
 * Date           Author       Comments
 * Mar 17, 2010	      A24178      Created file
 **********************************************************
 */

package com.motorola.batterymanager;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RadioButton;

import com.motorola.batterymanager.R;


/**
 * @author A24178
 *
 */
// pacakge-scope
class MyPreference extends Preference implements OnClickListener {
    //package-scope
    interface MyPreferenceClickListener {
        void onClick(String key, View view);
    }
    
    // Things to remember
    private MyPreferenceClickListener mListener;
    private RadioButton mRadio;
    private boolean mChecked;

    public MyPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MyPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyPreference(Context context) {
        super(context);
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View view = super.getView(convertView, parent);
        
        mRadio = (RadioButton) view.findViewById(com.motorola.batterymanager.R.id.radio);
        ImageView iv = (ImageView) view.findViewById(com.motorola.batterymanager.R.id.infoimg);
        View dv = view.findViewById(com.motorola.batterymanager.R.id.ptext);

        mRadio.setOnClickListener(this);
        iv.setOnClickListener(this);
        dv.setOnClickListener(this);
        
        mRadio.setChecked(mChecked);
        return view;
    }
    
    /* (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    public void onClick(View view) {
        if(mListener != null) {
            mListener.onClick(getKey(), view);
        }
    }
    
    public void setOnClickListener(MyPreferenceClickListener listener) {
        mListener = listener;
    }
    
    public void setRadioChecked(boolean checked) {
        mChecked = checked;
        if(mRadio != null) {
            mRadio.setChecked(mChecked);
        }
    }
}
