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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.motorola.mmsp.performancemaster.engine.BatteryInfo;
import com.motorola.mmsp.performancemaster.engine.BatteryModeData;
import com.motorola.mmsp.performancemaster.engine.BatteryModeMgr;
import com.motorola.mmsp.performancemaster.engine.Log;
import com.motorola.mmsp.performancemaster.engine.SysPropEngine;
import com.motorola.mmsp.performancemaster.engine.InfoBase.InfoListener;
import com.motorola.mmsp.performancemaster.ui.BatteryWidgetService.LocalBinder;
import com.motorola.mmsp.performancemaster.R;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class BatteryModeActivity extends Activity implements View.OnClickListener {

    private static final String LOG_TAG = "BatteryMain: ";
    public static final int MSG_UPDATE_BATTERY_INFO = 1;
    private static final int BATTERY_RED_PERCENT = 10;
    private static final int BATTERY_ORANGE_PERCENT = 30;
    private static final float BATTERY_FULL_VOLTAGE = (float)4.2;
    private static final float BATTERY_EMPTY_VOLTAGE = (float)3.0;

    private BatteryModeMgr mBatteryModeMgr;
    private BatteryModeData mCurrMode;

    // SystemMgrEngine
    private BatteryInfo mBatteryInfo;
    private SysPropEngine engine = null;

    // List View
    //private ListView mModeOptimizeView;
    private GridView mModeOptimizeView;
    private BatteryModeOptimizeAdapter mOptimizeAdapter;

    // Mode Button
    private Button mModeSwitchButton;

    // Battery related views
    private ImageView ivBattPercent;
    private ImageView ivBattCharging;
    private int ivTotalHeight;
    private TextView tvBattPercent;
    private int mBattPercent;
    private boolean mBattCharging;
    private TextView tvBattTemperature;
    private TextView tvBattHealth;
    private TextView tvBattVoltage;
    private TextView tvBattRemaining = null;

    // optimize text view
    private TextView tvOptimize;

    // battery left time
    private BattRemainingUpdate mBatteryRemainingUpdate = null;
    private BatteryWidgetService mService;
    private boolean mBound;
    
    private MyReceiver mModeReceiver = new MyReceiver();
    
    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (arg1 == null) {
                return;
            }
            
            String action = arg1.getAction();
            if (action == null) {
                return;
            }
            
            if (action.equals(BatteryModeMgr.ACTION_BATTERY_MODE_CHANGED)) {
                // battery mode change
                updateUI();
            }
        }
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            if (mService != null && mCurrMode != null) {
                Log.i(LOG_TAG, "ServiceConnected-->calcLeftTime");
                mService.calcLeftTime(mCurrMode);
                mBattPercent = mService.getBatteryPercent();
                mBattCharging = mService.getBatteryCharing();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };   

    Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE_BATTERY_INFO) {
                updateBattInfo();
            }
        }
    };

    private InfoListener mBattInfoListener = new InfoListener() {
        @Override
        public void onInfoUpdate() {
            Message msg = new Message();

            msg.what = MSG_UPDATE_BATTERY_INFO;
            myHandler.sendMessage(msg);
        }
    };

    /**
     * Maybe, we should use Map to store "battery mode data"
     * 
     * @param mode
     * @return
     */
    private ArrayList<BatteryModeOptimizeItem> modeToOptimizeList(BatteryModeData mode) {
        ArrayList<BatteryModeOptimizeItem> optimizeList = new ArrayList<BatteryModeOptimizeItem>();

        BatteryModeOptimizeItem item = new BatteryModeOptimizeItem();
        
        int iconRes = 0;

        // brightness
        int brightness = mode.getBrightness();
        if (brightness == -1) {
            iconRes = R.drawable.ic_bm_brightness_auto;
        } else {
            if (brightness < BatteryModeData.BRIGHTNESS_LEVEL_2) {
                iconRes = R.drawable.ic_bm_brightness_1;
            } else if (brightness < BatteryModeData.BRIGHTNESS_LEVEL_3) {
                iconRes = R.drawable.ic_bm_brightness_2;
            } else if (brightness == 100){
                iconRes = R.drawable.ic_bm_brightness_3;
            }
        }        
        item.setIconId(iconRes);
        item.setTextId(R.string.bm_parm_brightness);
        if (mode.getBrightness() == -1) {
            item.setValue(this.getString(R.string.bm_brightness_auto));
        } else {
            item.setValue(String.valueOf(mode.getBrightness()) + "%");
        }
        item.setTag(BatteryModeOptimizeItem.ITEM_TAG_BRIGHTNESS);
        optimizeList.add(item);

        // timeout
        item = new BatteryModeOptimizeItem();
        switch(mode.getTimeout()) {
            case 15000:
                iconRes = R.drawable.ic_bm_timeout_15s;
                break;
            case 30000:
                iconRes = R.drawable.ic_bm_timeout_30s;
                break;
            case 60000:
                iconRes = R.drawable.ic_bm_timeout_1m;
                break;
            case 120000:
                iconRes = R.drawable.ic_bm_timeout_2m;
                break;
            case 300000:
                iconRes = R.drawable.ic_bm_timeout_5m;
                break;
            case 600000:
                iconRes = R.drawable.ic_bm_timeout_10m;
                break;
            case 1800000:
                iconRes = R.drawable.ic_bm_timeout_30m;
                break;
            case -1:
                iconRes = R.drawable.ic_bm_timeout_never;
                break;
        }
        item.setIconId(iconRes);
        item.setTextId(R.string.bm_parm_timeout);
        if (mode.getTimeout() == -1) {
            item.setValue(this.getString(R.string.bm_timeout_never));
        } else {
            int seconds = mode.getTimeout() / 1000;
            String str = "";
            if (seconds % 60 == 0) {
                str = String.valueOf(seconds / 60) + " "
                + this.getString(R.string.bm_minute);
            } else {
                str = String.valueOf(seconds % 60) + " "
                + this.getString(R.string.bm_second);
            }
            item.setValue(str);
        }
        item.setTag(BatteryModeOptimizeItem.ITEM_TAG_TIMEOUT);
        optimizeList.add(item);
        
        // Bluetooth
        item = new BatteryModeOptimizeItem();
        if (mode.getBluetoothOn()) {
            iconRes = R.drawable.ic_bm_bluetooth_enable;
        } else {
            //iconRes = R.drawable.ic_bm_bluetooth_disable;
            iconRes = R.drawable.ic_bm_bluetooth;
        }
        item.setIconId(iconRes);
        item.setTextId(R.string.bm_parm_bluetooth);
        item.setValue(this.getString(mode.getBluetoothOn() ? R.string.bm_status_on
                : R.string.bm_status_off));
        item.setHighlight(mode.getBluetoothOn());
        item.setTag(BatteryModeOptimizeItem.ITEM_TAG_BLUETOOTH);
        optimizeList.add(item);

        // WiFi
        item = new BatteryModeOptimizeItem();
        if (mode.getWiFiOn()) {
            iconRes = R.drawable.ic_bm_wifi_enable;
        } else {
            //iconRes = R.drawable.ic_bm_wifi_disable;
            iconRes = R.drawable.ic_bm_wifi;
        }
        item.setIconId(iconRes);
        item.setTextId(R.string.bm_parm_wifi);
        item.setValue(this.getString(mode.getWiFiOn() ? R.string.bm_status_on
                : R.string.bm_status_off));
        item.setHighlight(mode.getWiFiOn());
        item.setTag(BatteryModeOptimizeItem.ITEM_TAG_WIFI);
        optimizeList.add(item);

        // Mobile data
        item = new BatteryModeOptimizeItem();
        if (mode.getMobileDataOn()) {
            iconRes = R.drawable.ic_bm_mobiledata_enable;
        } else {
            //iconRes = R.drawable.ic_bm_mobiledata_disable;
            iconRes = R.drawable.ic_bm_mobiledata;
        }
        item.setIconId(iconRes);
        item.setTextId(R.string.bm_parm_mobiledata);
        item.setValue(this.getString(mode.getMobileDataOn() ? R.string.bm_status_on
                : R.string.bm_status_off));
        item.setHighlight(mode.getMobileDataOn());
        item.setTag(BatteryModeOptimizeItem.ITEM_TAG_MOBILEDATA);
        optimizeList.add(item);

        // Sync
        item = new BatteryModeOptimizeItem();
        if (mode.getSyncOn()) {
            iconRes = R.drawable.ic_bm_sync_enable;
        } else {
            //iconRes = R.drawable.ic_bm_sync_disable;
            iconRes = R.drawable.ic_bm_sync;
        }
        item.setIconId(iconRes);
        item.setTextId(R.string.bm_parm_sync);
        item.setValue(this.getString(mode.getSyncOn() ? R.string.bm_status_on
                : R.string.bm_status_off));
        item.setHighlight(mode.getSyncOn());
        item.setTag(BatteryModeOptimizeItem.ITEM_TAG_SYNC);
        optimizeList.add(item);
        
        // Rotation
        item = new BatteryModeOptimizeItem();
        if (mode.getRotationOn()) {
            iconRes = R.drawable.ic_bm_rotation_enable;
        } else {
            //iconRes = R.drawable.ic_bm_rotation_disable;
            iconRes = R.drawable.ic_bm_rotation;
        }
        item.setIconId(iconRes);
        item.setTextId(R.string.bm_parm_rotation);
        item.setValue(this.getString(mode.getRotationOn() ? R.string.bm_status_on
                : R.string.bm_status_off));
        item.setHighlight(mode.getRotationOn());
        item.setTag(BatteryModeOptimizeItem.ITEM_TAG_ROTATION);
        optimizeList.add(item);
        
        // Vibrate
        item = new BatteryModeOptimizeItem();
        if (mode.getVibrationOn()) {
            iconRes = R.drawable.ic_bm_vibration_enable;
        } else {
            //iconRes = R.drawable.ic_bm_vibration_disable;
            iconRes = R.drawable.ic_bm_vibration;
        }
        item.setIconId(iconRes);
        item.setTextId(R.string.bm_parm_vibration);
        item.setValue(this.getString(mode.getVibrationOn() ? R.string.bm_status_on
                : R.string.bm_status_off));
        item.setHighlight(mode.getVibrationOn());
        item.setTag(BatteryModeOptimizeItem.ITEM_TAG_VIBRATION);
        optimizeList.add(item);

        // Haptic
        item = new BatteryModeOptimizeItem();
        if (mode.getHapticOn()) {
            iconRes = R.drawable.ic_bm_haptic_enable;
        } else {
            //iconRes = R.drawable.ic_bm_haptic_disable;
            iconRes = R.drawable.ic_bm_haptic;
        }
        item.setIconId(iconRes);
        item.setTextId(R.string.bm_parm_haptic);
        item.setValue(this.getString(mode.getHapticOn() ? R.string.bm_status_on
                : R.string.bm_status_off));
        item.setHighlight(mode.getHapticOn());
        item.setTag(BatteryModeOptimizeItem.ITEM_TAG_HAPTIC);
        optimizeList.add(item);

        return optimizeList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.e(LOG_TAG, "onCreate");

        // main layout
        setContentView(R.layout.bm_mode_main);
        
        // system property engine
        engine = SysPropEngine.getInstance();
        mBatteryInfo = engine.getBattInfo();

        // battery mode manager
        BatteryModeMgr.setContext(this.getApplicationContext());
        mBatteryModeMgr = BatteryModeMgr.getInstance();
        mBatteryModeMgr.init();

        mCurrMode = mBatteryModeMgr.getCurrModeFromPrefs();
        Log.i(LOG_TAG, "mCurrMode=" + mCurrMode.toString());

        mModeOptimizeView = (GridView) findViewById(R.id.bm_mode_optimize_view);

        ArrayList<BatteryModeOptimizeItem> optimizeList = modeToOptimizeList(mCurrMode);
        mOptimizeAdapter = new BatteryModeOptimizeAdapter(this, optimizeList);
        mModeOptimizeView.setAdapter(mOptimizeAdapter);
        //mModeOptimizeView.setCacheColorHint(0);

        mModeSwitchButton = (Button) findViewById(R.id.bm_mode_switch_btn);
        if (mModeSwitchButton != null) {
            mModeSwitchButton.setOnClickListener(this);
        }
        
        ivBattPercent = (ImageView) findViewById(R.id.bm_batt_percent);
        if (ivBattPercent != null) {
            ivTotalHeight = ivBattPercent.getLayoutParams().height;
        }
        ivBattCharging = (ImageView) findViewById(R.id.bm_batt_charing);
        tvBattPercent = (TextView) findViewById(R.id.bm_usage_percent);
        tvBattTemperature = (TextView) findViewById(R.id.bm_temperature);
        // tvBattHealth = (TextView) findViewById(R.id.bm_health);
        tvBattVoltage = (TextView) findViewById(R.id.bm_voltage);
        tvBattRemaining = (TextView) findViewById(R.id.bm_remaining_time);

        myHandler.sendEmptyMessage(MSG_UPDATE_BATTERY_INFO);

        tvOptimize = (TextView) findViewById(R.id.bm_optimize_tv);
        //tvOptimize.setOnClickListener(this);

        mBatteryRemainingUpdate = new BattRemainingUpdate(this, tvBattRemaining);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(BatteryModeMgr.ACTION_BATTERY_MODE_CHANGED);
        
        this.registerReceiver(mModeReceiver, filter);
    }
    
    private void checkDisclaimer() {
        SharedPreferences sharePrefs = getSharedPreferences(BatteryModeMgr.BATTERY_DISCLAIMER_PREFS, 0);
        int shown = sharePrefs.getInt(BatteryModeMgr.BATTERY_DISCLAIMER_SHOWN, -1);
        if (shown != 1) {
            Intent i = new Intent(this, DisclaimerActivity.class);
            
            startActivity(i);
        }
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        // TabSpec can not bind to service, use Application Context
        boolean ret = getApplicationContext().bindService(new Intent(this, BatteryWidgetService.class), mConnection,
                Context.BIND_AUTO_CREATE);
        Log.i(LOG_TAG, "onStart bindservice ret=" + ret);
        
        engine.getBattInfo().registerListener(mBattInfoListener);
        
        checkDisclaimer();
    }

    @Override
    protected void onStop() {     
        super.onStop();
        
        if (mBound) {
            getApplicationContext().unbindService(mConnection);
            mBound = false;
            Log.i(LOG_TAG, "onStop unbindservice");
        }
        
        engine.getBattInfo().unregisterListener(mBattInfoListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        Log.e(LOG_TAG, "onDestroy");
        
        if (mBatteryRemainingUpdate != null) {
            mBatteryRemainingUpdate.stop();
        }
        
        this.unregisterReceiver(mModeReceiver);
        mBatteryModeMgr.deinit();
    }
    
    private void updateUI() {
        // when mode change, activity resume
        mCurrMode = mBatteryModeMgr.getCurrModeFromPrefs();
        Log.i(LOG_TAG, "updateUI currMode=" + mCurrMode.toString());
        if (mCurrMode != null) {
            // switch button
            if (mModeSwitchButton != null) {
                mModeSwitchButton.setText(mBatteryModeMgr.getModeUIName(mCurrMode));
            }
            
            // battery remain time
            if (mBound && mService != null) {
                mService.calcLeftTime(mCurrMode);
                mBattPercent = mService.getBatteryPercent();
                mBattCharging = mService.getBatteryCharing();
            }
            
            // update other battery info
            updateBattInfo();
        }

        mOptimizeAdapter.setListData(modeToOptimizeList(mCurrMode));
    }

    private void updateBattInfo() {
        int rawLevel = mBatteryInfo.getRawlevel();
        int scale = mBatteryInfo.getScale();
        int health = mBatteryInfo.getHealth();
        int temperature = mBatteryInfo.getTemperature();
        int voltage = mBatteryInfo.getVoltage();
        int plugged = mBatteryInfo.getPlugged();

        // percent
        int level = 0;
        if (rawLevel > 0 && scale > 0) {
            level = (rawLevel * 100) / scale;
        } else {
            level = 0;
        }

        if (tvBattPercent != null) {
            mBattPercent = level;
            tvBattPercent.setText(String.valueOf(level) + "%");
        }

        if (ivBattPercent != null) {
            if (level < BATTERY_RED_PERCENT) {
                ivBattPercent.setImageResource(R.drawable.bm_power_red);
            } else if (level < BATTERY_ORANGE_PERCENT) {
                ivBattPercent.setImageResource(R.drawable.bm_power_orange);
            } else {
                ivBattPercent.setImageResource(R.drawable.bm_power_green);
            }
            //Log.i(LOG_TAG, "ivTotalHeight=" + ivTotalHeight);
            FrameLayout.LayoutParams mLayoutParams = (FrameLayout.LayoutParams)
                    ivBattPercent.getLayoutParams();
            mLayoutParams.height = ivTotalHeight * level / 100;
            ivBattPercent.setLayoutParams(mLayoutParams);
        }
        
        mBattCharging = (plugged != 0 ? true : false);
        if (ivBattCharging != null) {
            ivBattCharging.setVisibility(mBattCharging ? View.VISIBLE : View.INVISIBLE);
        }

        DecimalFormat voltageFormat = new DecimalFormat("#.#");

        // voltage
        if (tvBattVoltage != null) {
            Log.i(LOG_TAG, "VOLTAGE=" + voltage + " RAW=" + rawLevel + " SCALE=" + scale);
            // IronPrimeTD voltage is not in mV
            float fVoltage;
            if (voltage < 1000) {
                fVoltage = ((float) mBattPercent * (BATTERY_FULL_VOLTAGE - BATTERY_EMPTY_VOLTAGE)) / 100;
                fVoltage += BATTERY_EMPTY_VOLTAGE;
            } else {
                fVoltage = (float) voltage / 1000;
            }
            
            tvBattVoltage.setText(this.getString(R.string.battery_voltage_str)
                    + ": "
                    + voltageFormat.format(fVoltage)
                    + this.getString(R.string.bm_voltage_unit));
        }

        // temperature
        if (tvBattTemperature != null) {
            //Log.i(LOG_TAG, "temperature=" + temperature);
            tvBattTemperature.setText(this.getString(R.string.battery_temperature_str)
                    + ": "
                    + String.valueOf(Math.round((float) temperature / 10))
                    + (char) (0x00B0)
                    + "C");
        }

        // health
        String strHealth = this.getString(R.string.battery_health_str);
        strHealth += ": ";
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_DEAD:
                strHealth += this.getString(R.string.battery_health_dead_str);
                break;
            case BatteryManager.BATTERY_HEALTH_GOOD:
                strHealth += this.getString(R.string.battery_health_good_str);
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                strHealth += this.getString(R.string.battery_health_overheat_str);
                break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                strHealth += this.getString(R.string.battery_health_overvoltage_str);
                break;
            case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                strHealth += this.getString(R.string.battery_health_unknown_str);
                break;
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                strHealth += this.getString(R.string.battery_health_failure_str);
                break;

        }

        if (tvBattHealth != null) {
            tvBattHealth.setText(strHealth);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == tvOptimize){
            editCurrMode();
        } else if (v == mModeSwitchButton) {
            Intent intent = new Intent(this, BatteryModeSelectActivity.class);
            intent.putExtra(BatteryModeSelectActivity.EXTRA_BATT_PERCENT, mBattPercent);

            startActivity(intent);
        }
    }

    public void onListItemClicked() {
        editCurrMode();
    }

    private void editCurrMode() {
        if (mCurrMode == null) {
            Log.e(LOG_TAG, "editCurrMode null");
            return;
        }

        if (!mCurrMode.getPreset()) {
            long id = mCurrMode.getId();
            String modeName = mCurrMode.getModeName();

            Log.i(LOG_TAG, "XXXedit mode id=" + id + "name=" + modeName);
            Intent intent = new Intent(this, BatteryModeEditActivity.class);

            intent.setAction(BatteryModeEditActivity.ACTION_EDIT_MODE);
            intent.putExtra(BatteryModeEditActivity.EXTRA_ID, mCurrMode.getId());
            intent.putExtra(BatteryModeEditActivity.EXTRA_BATT_PERCENT, mBattPercent);

            startActivity(intent);
        }
    }
}
