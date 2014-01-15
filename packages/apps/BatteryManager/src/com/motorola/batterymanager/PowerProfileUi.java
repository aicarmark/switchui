/**
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: PowerProfileUi.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 11-06-09       A24178       Created file
 *                -Ashok
 * 02-15-10       A24178       IKMAP-6046: Notification UI changes
 *                -Ashok
 **********************************************************
 */

package com.motorola.batterymanager;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import com.motorola.batterymanager.R;

public class PowerProfileUi extends PreferenceActivity {

    private final static String LOG_TAG = "PowerProfileUi";
    private final static String BATTERY_USE_KEY = "batteryuse";

    private final static int BATTERY_UPDATE = 0;

    private BatteryUsePreference mPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.powerprofile);
        mPreference = (BatteryUsePreference)getPreferenceManager()
                .findPreference(BATTERY_USE_KEY);

        mPreference.setOnClickListener(new BatteryUsePreference.MyPreferenceClickListener() {

            public void onClick(String key, View view) {
                if(key.equals(BATTERY_USE_KEY)) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        ComponentName batteryUseComponent = new ComponentName(
                            "com.android.settings", "com.android.settings.Settings$PowerUsageSummaryActivity");

                        intent.setComponent(batteryUseComponent);
                        PowerProfileUi.this.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        getListView().setItemsCanFocus(true);

        // Notification Change
        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences myPref = getSharedPreferences(BatteryProfile.OPTIONS_STORE,
                                            Context.MODE_PRIVATE);

        Preference uiPref = getPreferenceManager().findPreference(UiConstants.UI_PREF_KEY_BATTMODE);

        int currMode = myPref.getInt(BatteryProfile.KEY_OPTION_PRESET_MODE,
                                        BatteryProfile.DEFAULT_PRESET_MODE);
        boolean isPreset = myPref.getBoolean(BatteryProfile.KEY_OPTION_IS_PRESET,
                                        BatteryProfile.DEFAULT_OPTION_SELECT);

        if(isPreset) {
            if(currMode == BatteryProfile.OPTION_PRESET_MAXSAVER_MODE) {
                uiPref.setSummary(R.string.preset_maxbatt_title);
            }else if(currMode == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
                uiPref.setSummary(R.string.preset_nighttime_title);
            }else if(currMode == BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE) {
                uiPref.setSummary(R.string.preset_perfmode_title);
            }
        }else {
            uiPref.setSummary(R.string.customs_mode_title);
        }

        Intent initIntent =
            registerReceiver(mBatteryChangedReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mBatteryChangedReceiver.onReceive(this, initIntent);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mBatteryChangedReceiver);
    }
    
    // 2011.11.23 jrw647 added to fix cr 4511
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }
    // 2011.11.23 jrw647 added end
    
    private BroadcastReceiver mBatteryChangedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN);

                int percent = (int) ((level / (float)scale) * 100);
                mHandler.obtainMessage(BATTERY_UPDATE, percent, status).sendToTarget();
            }
        }

    };

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if(msg.what == BATTERY_UPDATE) {
                int percent = msg.arg1;
                int status = msg.arg2;

                int chargeImage = 0;
                if(status == BatteryManager.BATTERY_STATUS_UNKNOWN) {
                    chargeImage = R.drawable.ic_pane_battery_error;
                }else if(status == BatteryManager.BATTERY_STATUS_FULL) {
                    chargeImage = R.drawable.ic_pane_battery_complete;
                }else if(status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    chargeImage = R.drawable.ic_pane_battery_charge;
                }
                mPreference.setStatus(percent, chargeImage);
            }
        }
    };

    /** DEBUG
    private final static int MENU_LOG = 0;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_LOG, 0, this.getString(R.string.logs));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem mItem) {
        if(mItem.getItemId() == MENU_LOG) {
            Intent logIntent = new Intent(LogActivity.START_ACTION);
            logIntent.setClassName("com.motorola.batterymanager",
                                    "com.motorola.batterymanager.LogActivity");
            startActivity(logIntent);
        }
        return super.onOptionsItemSelected(mItem);
    }
    DEBUG **/

}

