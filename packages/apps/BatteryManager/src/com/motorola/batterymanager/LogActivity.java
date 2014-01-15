/**
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: LogActivity.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 11-06-09       A24178       Created file
 *                -Ashok
 **********************************************************
 */

package com.motorola.batterymanager;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.motorola.batterymanager.R;

public class LogActivity extends Activity {

    private final static String LOG_TAG = "PowerProfileLog";

    public final static String START_ACTION = 
        "android.intent.action.batteryprofile.LOG_ACT_START";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.powerprofilelog);
    }

    @Override
    public void onResume() {
        super.onResume();

        TextView log_view = (TextView) findViewById(R.id.log_text);  

        final SharedPreferences prof_log = getSharedPreferences(BatteryProfile.LOG_STORE,
                MODE_WORLD_READABLE );
        StringBuilder sb = new StringBuilder(getApplicationContext().getString(R.string.logtitle));

        int idx = prof_log.getInt(BatteryProfile.KEY_LOG_INDEX, 0);
        String str = prof_log.getString(BatteryProfile.LOG_KEYS_ARRAY[idx], null);

        if(str == null) {
            for(int i = 0; i < idx; ++i) {
                str = prof_log.getString(BatteryProfile.LOG_KEYS_ARRAY[i], null);

                if(str != null) {
                    sb.append(str);
                }else {
                    Log.i(LOG_TAG, "Logger: Failure reading data logs - missing string");
                    break;
                }
            }
        }else {
            sb.append(str);

            for(int i = (idx+1)%BatteryProfile.LOG_KEYS_ARRAY.length; i != idx;) {
                str = prof_log.getString(BatteryProfile.LOG_KEYS_ARRAY[i], null);

                if(str != null) {
                    sb.append(str);
                }else {
                    Log.i(LOG_TAG, "Logger: Failure reading data logs - missing string");
                    break;
                }
                i = (i + 1) % BatteryProfile.LOG_KEYS_ARRAY.length;
            }
        }

        log_view.setText(sb.toString());
    }
}

