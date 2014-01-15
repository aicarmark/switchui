/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *                             Modification     Tracking
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * bntw34                      02/05/2012                   Initial release
 */

package com.motorola.mmsp.performancemaster.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.motorola.mmsp.performancemaster.engine.BatteryModeData;
import com.motorola.mmsp.performancemaster.engine.Log;
import com.motorola.mmsp.performancemaster.R;

public class BatteryModeEditItemView extends LinearLayout implements View.OnClickListener{
    private static final String LOG_TAG = "BatteryEditAdapter: ";
    private BatteryModeEditActivity mContext;
    private BatteryModeEditItem mData;

    // the array is in the setting apk res
    // can not get via public interface
    // please refer: packages/apps/settings/res/values/arrays.xml
    private static final int[] mTimeoutValueList = {
            15000, 30000, 60000, 120000, 300000, 600000, 1800000, -1
    };

    private static final int[] mBrightnessValueList = {
            12, 40, 100, -1
    };

    private ImageView ivIcon;
    private TextView tvText;
    private int mHighlightRGB;
    private int mNormalRGB;
    
    /*
    private void changeBrightness(int val) {
        Window w = mContext.getWindow();
        
        // preview brightness changes at this window
        // get the current window attributes
        WindowManager.LayoutParams layoutpars = w.getAttributes();
        
        if (val > 0) {
            if (val < BatteryModeData.BRIGHTNESS_MIN_VALUE) {
                val = BatteryModeData.BRIGHTNESS_MIN_VALUE;
            }

            
            // set the brightness of this window
            // 0.0 - 1.0
            layoutpars.screenBrightness = val / (float) 100;
            // apply attribute changes to this window
            w.setAttributes(layoutpars);
            
            // disable auto mode
            //mContext.onSetAutoBrightness(false);
        } else {
            // set auto mode
            mContext.onSetAutoBrightness(true);
            
            layoutpars.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            w.setAttributes(layoutpars);
        }
    }
    */
    
    /*
    private void changeBrightness(int val) {
        Window w = mContext.getWindow();
        
        if (val < BatteryModeData.BRIGHTNESS_MIN_VALUE) {
            val = BatteryModeData.BRIGHTNESS_MIN_VALUE;
        }

        // preview brightness changes at this window
        // get the current window attributes
        WindowManager.LayoutParams layoutpars = w.getAttributes();
        // set the brightness of this window
        // 0.0 - 1.0
        layoutpars.screenBrightness = val / (float) 100;
        // apply attribute changes to this window
        w.setAttributes(layoutpars);
    }
    */
    
    /**
     * only support 15s, 30s, 1min, 2min, 5min, 10min, 30min, never
     * @param seconds
     * @return
     */
    private String getTimeoutString(int seconds) {
        String str = "";
        if (seconds < 0) {
            str = mContext.getString(R.string.bm_timeout_never);
        } else {
            seconds = seconds / 1000;

            if (seconds == 0) {
                Log.e(LOG_TAG, "getTimeout seconds == 0");
            }

            if (seconds % 60 == 0) {
                str = String.valueOf(seconds / 60) + " "
                                + mContext.getString(R.string.bm_minute);
            } else {
                str = String.valueOf(seconds % 60) + " "
                                + mContext.getString(R.string.bm_second);
            }
        }

        return str;
    }
    
    private void refreshView() {
        int parmTag = mData.getTag();

        if (parmTag == BatteryModeEditItem.ITEM_TAG_BRIGHTNESS) {
            // name
            tvText.setText(R.string.bm_parm_brightness);
            tvText.setTextColor(mHighlightRGB);
            
            // value
            int currBrightness = mData.getInt();
            int resId = 0;
            if (currBrightness == -1) {
                resId = R.drawable.ic_bm_brightness_auto;
            } else {
                if (currBrightness < BatteryModeData.BRIGHTNESS_LEVEL_2) {
                    resId = R.drawable.ic_bm_brightness_1;
                } else if (currBrightness < BatteryModeData.BRIGHTNESS_LEVEL_3) {
                    resId = R.drawable.ic_bm_brightness_2;
                } else {
                    resId = R.drawable.ic_bm_brightness_3;
                }
            }
            
            // icon
            ivIcon.setImageResource(resId);
        } else if (parmTag == BatteryModeEditItem.ITEM_TAG_TIMEOUT) {
            // name
            tvText.setText(R.string.bm_parm_timeout);
            tvText.setTextColor(mHighlightRGB);
            
            // value
            int currTimeout = mData.getInt();
            
            // icon
            int timeoutIcon = 0;
            switch(currTimeout) {
                case 15000:
                    timeoutIcon = R.drawable.ic_bm_timeout_15s;
                    break;
                case 30000:
                    timeoutIcon = R.drawable.ic_bm_timeout_30s;
                    break;
                case 60000:
                    timeoutIcon = R.drawable.ic_bm_timeout_1m;
                    break;
                case 120000:
                    timeoutIcon = R.drawable.ic_bm_timeout_2m;
                    break;
                case 300000:
                    timeoutIcon = R.drawable.ic_bm_timeout_5m;
                    break;
                case 600000:
                    timeoutIcon = R.drawable.ic_bm_timeout_10m;
                    break;
                case 1800000:
                    timeoutIcon = R.drawable.ic_bm_timeout_30m;
                    break;
                case -1:
                    timeoutIcon = R.drawable.ic_bm_timeout_never;
                    break;
            }
            
            ivIcon.setImageResource(timeoutIcon);
        } else {
            // name
            tvText.setText(BatteryModeEditItem.strRes[parmTag]);
            if (mData.getBoolean()) {
                tvText.setTextColor(mHighlightRGB);
            } else {
                tvText.setTextColor(mNormalRGB);
            }
                        
            // icon
            if (mData.getBoolean()) {
                ivIcon.setImageResource(BatteryModeEditItem.iconResEnabled[parmTag]);
            } else {
                ivIcon.setImageResource(BatteryModeEditItem.iconResDisabled[parmTag]);
            }
        } 
    }
    
    /**
     * constructor
     * @param context
     * @param data from the Adapter, member mData reference the actual data in Adapter
     */
    public BatteryModeEditItemView(BatteryModeEditActivity context, BatteryModeEditItem data) {
        super(context);
        mContext = context;
        mData = data;
        
        mHighlightRGB = Color.rgb(51, 181, 229);
        mNormalRGB = Color.rgb(255, 255, 255);

        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        inflater.inflate(R.layout.bm_grid_item, this, true);
        ivIcon = (ImageView) findViewById(R.id.grid_item_icon);
        tvText = (TextView) findViewById(R.id.grid_item_name);
        
        refreshView();

        this.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int parmTag = mData.getTag();
        
        if (parmTag == BatteryModeEditItem.ITEM_TAG_BRIGHTNESS) {
            int currBrightness = mData.getInt();
            
            // find the next brightess
            int i = 0;
            for (; i < mBrightnessValueList.length; i++) {
                if (mBrightnessValueList[i] > currBrightness) {
                    break;
                }
            }
            
            if (i == mBrightnessValueList.length) {
                // the max brightness value is -1
                // use -1 's index
                i = mBrightnessValueList.length - 1;
            }
            
            mData.setInt(mBrightnessValueList[i]);
            
            // change brightness immediately
            mContext.changeBrightness(mBrightnessValueList[i]);
        } else if (parmTag == BatteryModeEditItem.ITEM_TAG_TIMEOUT) {
            int currTimeout = mData.getInt();
            
            // find the next timeout
            int i = 0;
            for (; i < mTimeoutValueList.length; i++) {
                if (mTimeoutValueList[i] > currTimeout) {
                    break;
                }
            }
            
            if (i == mTimeoutValueList.length) {
                // the max timeout value is -1
                // use -1's index
                i = mTimeoutValueList.length - 1;
            }
            
            mData.setInt(mTimeoutValueList[i]);
        } else {
            boolean bCurrStatus = mData.getBoolean();
            
            // toggle status
            mData.setBoolean(!bCurrStatus);
        }
        
        // refresh the view
        refreshView();
        
        // calculate left time immediately
        mContext.onCalcLeftTime();
    }
}
