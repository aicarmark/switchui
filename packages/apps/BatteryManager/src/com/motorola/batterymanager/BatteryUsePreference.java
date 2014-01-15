/**
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: BatteryUsePreference.java
 * Description: What the class does.
 *
 */

package com.motorola.batterymanager;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView.ScaleType;
import com.motorola.batterymanager.R;

public class BatteryUsePreference extends Preference implements View.OnClickListener {

    //package-scope
    interface MyPreferenceClickListener {
        void onClick(String key, View view);
    }
    private final static int VIEW_UPDATE = 0;
    private MyPreferenceClickListener mListener;
    private ImageView mFillView;
    private ImageView mSpaceView;
    private ImageView mOverlayView;
    private LinearLayout mLayout;
    private TextView mLevelView;

    private int mPercent;
    private int mOverlay;

    public BatteryUsePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public BatteryUsePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BatteryUsePreference(Context context) {
        super(context);
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View view = super.getView(convertView, parent);

        mLevelView = (TextView)view.findViewById(R.id.bu_level_text);
        mFillView = (ImageView)view.findViewById(R.id.filler);
        mSpaceView = (ImageView)view.findViewById(R.id.spacer);
        mOverlayView = (ImageView)view.findViewById(R.id.overlay);
        mLayout = (LinearLayout)view.findViewById(R.id.tank);

        View image = view.findViewById(R.id.bu_btn_img);
        image.setOnClickListener(this);
        // 2011.12.02 jrw647 modified to fix cr 4735
        //doUpdate();
        mHandler.obtainMessage(VIEW_UPDATE, 0, 0).sendToTarget();
        // 2011.12.02 jrw647 modified end
        return view;
    }

    public void onClick(View view) {
        if(mListener != null) {
            mListener.onClick(getKey(), view);
        }
    }

    public void setOnClickListener(MyPreferenceClickListener listener) {
        mListener = listener;
    }

    public void setStatus(int percent, int overlayRes) {
        mPercent = percent;
        mOverlay = overlayRes;
        doUpdate();
    }

    private void doUpdate() {
        if(mLayout == null) return; // TODO: currently here to prevent too early
                                   //        an update before view is inflated.
        int fill = 0;
        int space = 0;
        //int spaceImage;
        int fillImage;

        int cap = mLayout.getHeight();

        // 2012.01.06 jrw647 modified to fix cr5652
        // fill = cap * mPercent / 100;
        // space = cap - fill;

        if(mPercent <= 5) {// 2011.12.060 jrw647 modified to fix cr 4176 4617
            //spaceImage = R.drawable.battery_red_top;
            fillImage = R.drawable.battery_red_fill;
            fill = 0;
        }else if(mPercent <= 20) {// 2011.12.06 jrw647 modified to fix cr 4176 4617
            //spaceImage = R.drawable.battery_orange_top;
            if(mOverlay == 0) {
                fillImage = R.drawable.battery_orange_fill;
            } else {
                fillImage = R.drawable.battery_green_fill;
            }
            fill = cap * 20 / 100;
        }else {
            //spaceImage = R.drawable.battery_green_top;
            fillImage = R.drawable.battery_green_fill;
            if(mPercent <= 35) {
                fill = cap * 35 / 100;
            }else if(mPercent <= 49) {
                fill = cap * 49 / 100;
            }else if(mPercent <= 60) {
                fill = cap * 60 / 100;
            }else if(mPercent <=75) {
                fill = cap * 75 / 100;
            }else if(mPercent <=90) {
                fill = cap * 90 / 100;
            }else {
                fill = cap;
            }
        }
        space = cap - fill;
        // 2012.01.06 jrw647 modified to fix cr5652 end

        mLevelView.setText(getContext().getString(R.string.bu_pct_text, mPercent));

        LinearLayout.LayoutParams fl = (LinearLayout.LayoutParams) mFillView.getLayoutParams();
        fl.height = fill;
        mFillView.setLayoutParams(fl);
        if(mPercent <= 90)
            mFillView.setImageResource(fillImage);
        else
            mFillView.setImageResource(R.drawable.battery_charge_background_full);

        fl = (LinearLayout.LayoutParams) mSpaceView.getLayoutParams();
        fl.height = space;
        //mSpaceView.setImageResource(spaceImage);
        mSpaceView.setLayoutParams(fl);

        // 2011.11.18 jrw647 modified to fix cr 4469
        if(mPercent > 90) {
            //mSpaceView.setImageResource(fillImage);
            mSpaceView.setScaleType(ScaleType.FIT_XY);
        }else{
            mSpaceView.setScaleType(ScaleType.FIT_END);
        }
        // 2011.11.18 jrw647 modified end

        if(mOverlay == 0) {
            mOverlayView.setVisibility(View.INVISIBLE);
        }else {
            mOverlayView.setImageResource(mOverlay);
            mOverlayView.setVisibility(View.VISIBLE);
            mOverlayView.bringToFront();
        }
    }
    
    // 2011.12.02 jrw647 added to fix cr 4735
    private Handler mHandler = new Handler() {
        
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == VIEW_UPDATE) 
            {
                doUpdate();
            }
        }
    };
    // 2011.12.02 jrw647 added end
}

