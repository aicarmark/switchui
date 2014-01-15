/** 
 * Copyright (C) 2009, Motorola, Inc, 
 * All Rights Reserved 
 * Class name: ModeInfoUi.java 
 * Description: What the class does. 
 * 
 * Modification History: 
 **********************************************************
 * Date           Author       Comments
 * Mar 17, 2010	      A24178      Created file
 **********************************************************
 */

package com.motorola.batterymanager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TextView;

import com.motorola.batterymanager.R;

/**
 * @author A24178
 *
 */
public class ModeInfoUi extends Activity {
    
    private final static String LOG_TAG = "ModeInfoUi";
    
    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setTitle(R.string.moreinfo_text);
        setContentView(R.layout.modeinfo);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        TextView tvTitle = (TextView) findViewById(R.id.mode_title);
        TextView tvDesc = (TextView) findViewById(R.id.mode_long_desc);

        int titleRes, descRes;
        titleRes = descRes = R.string.moreinfo_text;
        int mode = getIntent().getIntExtra(UiConstants.UI_MODE_KEY, 
                BatteryProfile.DEFAULT_PRESET_MODE);
        
        if(mode == BatteryProfile.OPTION_PRESET_MAXSAVER_MODE) {
            titleRes = R.string.preset_maxbatt_title;
            descRes = R.string.presets_maxsaver_desc;
        }else if(mode == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
            titleRes = R.string.preset_nighttime_title;
            // Locale select
            if(DateFormat.is24HourFormat(this)) {
                descRes = R.string.presets_nighttime_desc_24;
            }else {
                descRes = R.string.presets_nighttime_desc;
            }
        }else if(mode == BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE) {
            titleRes = R.string.preset_perfmode_title; 
            descRes = R.string.presets_perfmode_desc;
        }
        tvTitle.setText(titleRes);
        tvDesc.setText(descRes);
    }
}
