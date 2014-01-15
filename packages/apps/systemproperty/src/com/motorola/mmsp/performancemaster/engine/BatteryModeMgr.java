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

package com.motorola.mmsp.performancemaster.engine;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;

import com.motorola.mmsp.performancemaster.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Battery mode logic wrapper, used by BatteryModeActivity handles battery mode
 * switch/add/remove/edit
 */
public class BatteryModeMgr {
    private static final String LOG_TAG_MODE = "BattModeMgr: ";
    
    // current mode preference
    public static final String BATTERY_MODE_PREFS = "battery_mode_prefs";
    public static final String BATTERY_CURR_MODE = "curr_mode";
    public static final String BATTERY_CURR_MODE_ID = "curr_mode_id";
    
    // battery left time disclaimer preference
    public static final String BATTERY_DISCLAIMER_PREFS = "disclaimer_prefs";
    public static final String BATTERY_DISCLAIMER_SHOWN = "disclaimer_shown";

    // user defined broadcast
    public static final String ACTION_BATTERY_MODE_CHANGED = "com.motorola.mmsp.performancemaster.bm.ACTION_MODE_CHANGED";
    public static final String ACTION_BATTERY_MODE_CHANGE_COMPLETED = "com.motorola.mmsp.performancemaster.bm.ACTION_MODE_CHANGE_COMPLETED";
    
    // system broadcast intent
    private static final String ACTION_SYNC_CONN_STATUS_CHANGED = "com.android.sync.SYNC_CONN_STATUS_CHANGED";

    // depends on com.android.providers.settings implementation
    private static final String SETTINGS_SECURE_MOBILEDATA = "mobile_data";
    private static final String SETTINGS_SYNC = "sync";
    
    // maximum battery mode number = 10
    public static final int MAX_BATTERY_MODE_NUM = 10;

    private static final int PRESET_MODE_INDEX_GENERAL = 0;
    private static final int PRESET_MODE_INDEX_SAVER = 1;
    private static final int PRESET_MODE_INDEX_NIGHT = 2;
    
    public static final int APPLY_CHANGE_RET_SUCCESS = 0;
    public static final int APPLY_CHANGE_RET_SUCCESS_PENDING = 1;
    public static final int APPLY_CHANGE_RET_FAIL = 2;
    
    private static final int TOGGLE_NONE = 0;
    private static final int TOGGLE_ON = 1;
    private static final int TOGGLE_OFF = 2;
    
    // BatteryModeMgr is a singleton 
    private static BatteryModeMgr mSingleInstance = null;
    private static int mRefCount = 0;
    private boolean mInitialized = false;
    private static Context mContext;
    
    // control utilities
    private BatteryModeDataSource mModesDAO;
    private BatteryModeData mCurrMode;
    private ArrayList<BatteryModeData> mPresetModes;
    private BatteryCtrlUtil mCtrlUtils;
    
    // change map is toggled by ourselves
    // it indicates whether the related change is toggled by us
    private HashMap<String, Boolean> mChangeMap;
    
    // user initiated mode parameter switching
    // switching to new mode/apply changes to current mode
    private boolean mIsSwitching;
    private boolean mUIToast;
    
    // airplane mode toggle flag;
    private boolean mAirplaneToggle;
    
    // pending change action
    private int mBtAction;
    private int mWifiAction;

    // setting observers (all 8 options except sync)
    private SettingsObserver mBrightnessObserver;
    private SettingsObserver mBrightnessModeObserver;
    private SettingsObserver mTimeoutObserver;
    private SettingsObserver mHapticObserver;
    private SettingsObserver mRotationObserver;
    private SettingsObserver mMobileDataObserver;
    private SettingsObserver mBluetoothObserver;
    private SettingsObserver mWiFiObserver;
    private SettingsObserver mVibrationObserver;
    
    /**
     *  system changes: system broadcast receiver
     */
    private SysBroadcastReceiver mSysBroadcastReceiver;

    private class SysBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (arg1 == null) {
                return;
            }

            String action = arg1.getAction();
            if (action == null) {
                return;
            }
            
            // flag: switch changed by other(settings, 3rd party app)
            boolean bChangeByOther = false;
            int wifiStatus = 0;
            int btStatus = 0;
            boolean bBtWifi = false;
            
            if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                wifiStatus = arg1.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                            WifiManager.WIFI_STATE_UNKNOWN);
                // wifiStatus: 0 - disabling, 1 - disabled, 2 - enabling, 3 - enabled, 4 - unknown
                Log.i(LOG_TAG_MODE, "[SYS]-->WIFI_STATE_CHANGED_ACTION  wifiStatus=" + wifiStatus
                        + " wifiAction=" + mWifiAction);
                
                // WiFi final status: 1(disabled), 3(enabled)
                
                // WiFi state machine
                if (wifiStatus == WifiManager.WIFI_STATE_ENABLED && mWifiAction == TOGGLE_ON) {
                    mCurrMode.setWiFiOn(true);
                    mWifiAction = TOGGLE_NONE;
                    bBtWifi = true;
                }
                
                if (wifiStatus == WifiManager.WIFI_STATE_DISABLED && mWifiAction == TOGGLE_OFF) {
                    mCurrMode.setWiFiOn(false);
                    mWifiAction = TOGGLE_NONE;
                    bBtWifi = true;
                }
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                // btStatus: 10 - off, 12 - on
                //           11 - turning on, 13 - turning off
                btStatus = arg1.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.STATE_ON);
                
                Log.i(LOG_TAG_MODE, "[SYS]-->Bluetooth STATE_CHANGED btStatus=" + btStatus
                        + " btAction=" + mBtAction);
                
                // Bluetooth final status: 10(off), 12(on)
                
                // BT state machine
                if (btStatus == BluetoothAdapter.STATE_OFF && mBtAction == TOGGLE_OFF) {
                    mCurrMode.setBluetoothOn(false);
                    mBtAction = TOGGLE_NONE;
                    bBtWifi = true;
                }
                
                if (btStatus == BluetoothAdapter.STATE_ON && mBtAction == TOGGLE_ON) {
                    mCurrMode.setBluetoothOn(true);
                    mBtAction = TOGGLE_NONE;
                    bBtWifi = true;
                }
            } else if (action.equals(ACTION_SYNC_CONN_STATUS_CHANGED)) {
                // sync
                Log.i(LOG_TAG_MODE, "[SYS]-->ACTION_SYNC_CONN_STATUS_CHANGED");
                if (mChangeMap.get(BatteryModeData.SYNC)) {
                    mChangeMap.put(BatteryModeData.SYNC, false);
                } else {
                    // the only bChangeByOther == true
                    bChangeByOther = true;
                }
            } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                boolean bState = arg1.getBooleanExtra("state", false);
                Log.i(LOG_TAG_MODE, "[SYS]-->ACTION_AIRPLANE_MODE_CHANGED bState=" + bState);
                
                boolean bAirplane = mCtrlUtils.isAirplaneOn();
                boolean bBtSetting = mCtrlUtils.isBluetoothOn_Setting();
                boolean bBtOff = mCtrlUtils.isBluetoothOff();
                boolean bWifiSetting = mCtrlUtils.isWifiOn_Setting();
                boolean bWifiOff = mCtrlUtils.isWifiOff();
                boolean bMobileData = mCtrlUtils.isMobileDataEnabled();
                
                Log.i(LOG_TAG_MODE, "bAirplane=" + bAirplane
                        + " BtSetting=" + bBtSetting + " btOff=" + bBtOff
                        + " WifiSetting=" + bWifiSetting + " WifiOff=" + bWifiOff
                        + " MobileData=" + bMobileData 
                        + " Switching=" + mIsSwitching);
                
                mCurrMode.setRadioOn(!bAirplane);

                // BatteryLeftTime need to know airplane mode status
                broadcastModeChange();
                
                // currently, mode switching is in process
                if (mIsSwitching) {
                    // cancel UI toast
                    mUIToast = false;
                    
                    if (bAirplane) {
                        Toast.makeText(mContext, R.string.bm_airplane_on, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mContext, R.string.bm_airplane_off, Toast.LENGTH_SHORT).show();
                    }
                }
                
                if (bAirplane) {                    
                    // airplane on, some turning off may happen
                    if (!bBtOff) {
                        mBtAction = TOGGLE_OFF;
                        mAirplaneToggle = true;
                    }
                    
                    if (!bWifiOff) {
                        mWifiAction = TOGGLE_OFF;
                        mAirplaneToggle = true;
                    }
                } else {
                    // airplane off, some turning on may happen
                    if (bBtSetting) {
                        mBtAction = TOGGLE_ON;
                        mAirplaneToggle = true;
                    }
                    
                    if (bWifiSetting) {
                        mWifiAction = TOGGLE_ON;
                        mAirplaneToggle = true;
                    }
                }
                
                if (mAirplaneToggle) {
                    Log.e(LOG_TAG_MODE, "Airplane mode switch BEGIN+++");
                }
            }

            if (bChangeByOther) {
                // SYNC status change, change by others
                onSettingChanged(SETTINGS_SYNC);
            }
            
            if (bBtWifi) {
                Log.e(LOG_TAG_MODE, "SysBroadcast mBtAction=" + mBtAction
                        + " mWifiAction=" + mWifiAction
                        + " mIsSwitching=" + mIsSwitching);
                // our mode switching complete
                if (mBtAction == TOGGLE_NONE
                        && mWifiAction == TOGGLE_NONE
                        && mIsSwitching) {
                    cancelModeChangeTimeout();
                    broadcastModeCompleted();
                    mIsSwitching = false;
                }
                
                // airplane handle
                if (mAirplaneToggle) {
                    handleAirplaneToggle();
                }
            }
        } // end of onReceive()

        public void start() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(ACTION_SYNC_CONN_STATUS_CHANGED);
            filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            mContext.getApplicationContext().registerReceiver(this, filter);
            
            Log.e(LOG_TAG_MODE, "start SYS broadcast REGISTER");
        }

        public void stop() {
            mContext.getApplicationContext().unregisterReceiver(this);
            
            Log.e(LOG_TAG_MODE, "stop SYS broadcast UNREGISTER");
        }
    }
    
    private static final int MODE_CHANGE_TIMEOUT_MS = 10000;
    private ModeChangeTimeoutRun mModeChangeTimeout;
    private class ModeChangeTimeoutRun implements Runnable {

        @Override
        public void run() {
            if (mIsSwitching) {
                Log.e(LOG_TAG_MODE, "***ERROR*** MODE_CHANGE TIMEOUT!!!!");
                mIsSwitching = false;
                broadcastModeCompleted();
            }   
        }
    }

    /**
     * setting changes: brightness, timeout, haptic, rotation, 
     * mobile data, vibration
     *
     */
    private class SettingsObserver extends ContentObserver {
        private String mOptionName = new String();

        public SettingsObserver(String strOption, Handler h) {
            super(h);
            mOptionName = strOption;
        }

        public void start() {
            Uri uri = Settings.System.getUriFor(mOptionName);
            mContext.getContentResolver().registerContentObserver(uri,
                    true,
                    this);

            Log.i(LOG_TAG_MODE, "startObserver name=" + mOptionName + " uri=" + uri.toString());
        }

        public void startSecure() {
            Uri uri = Settings.Secure.getUriFor(mOptionName);
            mContext.getContentResolver().registerContentObserver(uri,
                    true,
                    this);
            Log.i(LOG_TAG_MODE, "startObserver name=" + mOptionName + " uri=" + uri.toString());
        }

        public void stop() {
            mContext.getContentResolver().unregisterContentObserver(this);
            Log.i(LOG_TAG_MODE, "stopObserver name" + mOptionName);
        }

        /**
         * Setting changes
         */
        @Override
        public void onChange(boolean selfChange) {
            Log.i(LOG_TAG_MODE, "SettingsObserver.onChange "
                    + " optionName=" + mOptionName
                    + " selfChange=" + selfChange);

            boolean bChangeByOther = false;

            if (mChangeMap == null) {
                Log.e(LOG_TAG_MODE, "mChangeMap==null");
                return;
            }

            if (mOptionName.equals(Settings.System.SCREEN_BRIGHTNESS)) {
                // 1. brightness
                if (mChangeMap.get(BatteryModeData.BRIGHTNESS)) {
                    mChangeMap.put(BatteryModeData.BRIGHTNESS, false);
                } else {
                    bChangeByOther = true;
                }
            } else if (mOptionName.equals(Settings.System.SCREEN_BRIGHTNESS_MODE)) {
                // 1.1 brightness mode
                if (mChangeMap.get(BatteryModeData.BRIGHTNESS_MODE)) {
                    mChangeMap.put(BatteryModeData.BRIGHTNESS_MODE, false);
                } else {
                    bChangeByOther = true;
                }
            } else if (mOptionName.equals(Settings.System.SCREEN_OFF_TIMEOUT)) {
                // 2. timeout
                if (mChangeMap.get(BatteryModeData.TIMEOUT)) {
                    mChangeMap.put(BatteryModeData.TIMEOUT, false);
                } else {
                    bChangeByOther = true;
                }
            } else if (mOptionName.equals(Settings.System.HAPTIC_FEEDBACK_ENABLED)) {
                // 3. haptic
                if (mChangeMap.get(BatteryModeData.HAPTIC)) {
                    mChangeMap.put(BatteryModeData.HAPTIC, false);
                } else {
                    bChangeByOther = true;
                }
            } else if (mOptionName.equals(Settings.System.ACCELEROMETER_ROTATION)) {
                // 4. rotation
                if (mChangeMap.get(BatteryModeData.ROTATION)) {
                    mChangeMap.put(BatteryModeData.ROTATION, false);
                } else {
                    bChangeByOther = true;
                }
            } else if (mOptionName.equals(SETTINGS_SECURE_MOBILEDATA)) {
                Log.e(LOG_TAG_MODE, "onSettingChanged SECURE.MOBILE_DATA=====");
                
                // 5. mobile data
                if (mChangeMap.get(BatteryModeData.MOBILEDATA)) {
                    mChangeMap.put(BatteryModeData.MOBILEDATA, false);
                } else {
                    bChangeByOther = true;
                }
            } else if (mOptionName.equals(Settings.Secure.BLUETOOTH_ON)) {
                // 6. bluetooth
                if (mChangeMap.get(BatteryModeData.BLUETOOTH)) {
                    mChangeMap.put(BatteryModeData.BLUETOOTH, false);
                    // bluetooth on/off is pending, do not change switch progress
                } else {
                    bChangeByOther = true;
                }
            } else if (mOptionName.equals(Settings.Secure.WIFI_ON)) {
                // 7. wifi
                if (mChangeMap.get(BatteryModeData.WIFI)) {
                    mChangeMap.put(BatteryModeData.WIFI, false);
                    // wifi on/off is pending, do not change switch progress
                } else {
                    bChangeByOther = true;
                }
            } else if (mOptionName.equals(Settings.System.VIBRATE_WHEN_RINGING)) {
                // 8. vibration
                if (mChangeMap.get(BatteryModeData.VIBRATION)) {
                    mChangeMap.put(BatteryModeData.VIBRATION, false);
                } else {
                    bChangeByOther = true;
                }
            }
            
            if (bChangeByOther) {
                // others changes
                onSettingChanged(mOptionName);
            } 
        } // end of onChange()
    }
    
    // TODO: check with, toggle airplane mode, battery mode will switch to [general] somewhere
    /**
     * Airplane Toggle ==> BT/Wifi ON/OFF ==> handleAirplaneToggle()
     * if current mode == general mode, update the general mode data, save to database 
     * if current mode != general mode, switch to general mode, update the general mode data, save to database
     * notify UI via broadcast ACTION_BATTERY_MODE_CHANGE
     * UI get current mode data from database
     */
    private void handleAirplaneToggle() {
        boolean bMobileData = mCtrlUtils.isMobileDataEnabled();
        mCurrMode.setMobileDataOn(bMobileData);
        
        Log.e(LOG_TAG_MODE, "handleAirplaneToggle bMobileData=" + bMobileData);
        Log.e(LOG_TAG_MODE, "handleAirplaneToggle wifi=" + mCurrMode.getWiFiOn());
        Log.e(LOG_TAG_MODE, "handleAirplaneToggle bt=" + mCurrMode.getBluetoothOn());
        
        // save current mode to general mode, both database and memory
        saveGeneralMode();
        
        // if current mode is not general mode, switch current mode to general mode
        BatteryModeData generalMode = mPresetModes.get(PRESET_MODE_INDEX_GENERAL);
        if (generalMode.getId() != mCurrMode.getId()) {
            mCurrMode = generalMode;
            
            // save current mode id to preference
            saveCurrModeToPrefs();
        }
        
        broadcastModeChange();
        
        if (mBtAction == TOGGLE_NONE
                && mWifiAction == TOGGLE_NONE) {
            Log.e(LOG_TAG_MODE, "Airplane mode switch END---");
            mAirplaneToggle = false;
        }
    }

    public void onSettingChanged(String optName) {
        if (mCtrlUtils == null || mCurrMode == null || optName == null) {
            return;
        }

        // check whether it is our intended changes
        Log.i(LOG_TAG_MODE, "ByOther: onSettingChanged begin to get+++");
        
        // get radio status
        boolean radio = !mCtrlUtils.isAirplaneOn();

        boolean bChanged = false;
        String str = "";
        
        if (optName.equals(Settings.System.SCREEN_BRIGHTNESS)
                || optName.equals(Settings.System.SCREEN_BRIGHTNESS_MODE)) {
            int brightness = mCtrlUtils.getBrightness();
            if (mCurrMode.getBrightness() != brightness) {
                mCurrMode.setBrightness(brightness);
                Log.e(LOG_TAG_MODE, "onSettingChanged new brightness=" + brightness);
                bChanged = true;
                str += BatteryModeData.BRIGHTNESS;
            }
        } else if (optName.equals(Settings.System.SCREEN_OFF_TIMEOUT)) {
            int timeout = mCtrlUtils.getTimeout(); 
            if (mCurrMode.getTimeout() != timeout) {
                mCurrMode.setTimeout(timeout);
                bChanged = true;
                str += "+";
                str += BatteryModeData.TIMEOUT;
            }
        } else if (optName.equals(Settings.Secure.BLUETOOTH_ON)) {
            boolean bluetooth = mCtrlUtils.isBluetoothOn_Setting();
            if (mCurrMode.getBluetoothOn() != bluetooth) {
                mCurrMode.setBluetoothOn(bluetooth);
                bChanged = true;
                str += "+";
                str += BatteryModeData.BLUETOOTH;
            }
        } else if (optName.equals(SETTINGS_SECURE_MOBILEDATA)) {
            boolean mobileData = mCtrlUtils.isMobileDataEnabled();
            if (mCurrMode.getMobileDataOn() != mobileData) {
                mCurrMode.setMobileDataOn(mobileData);
                bChanged = true;
                str += "+";
                str += BatteryModeData.MOBILEDATA;
            }
        } else if (optName.equals(SETTINGS_SYNC)) {
            boolean sync = mCtrlUtils.isSyncOn();
            if (mCurrMode.getSyncOn() != sync) {
                mCurrMode.setSyncOn(sync);
                bChanged = true;
                str += "+";
                str += BatteryModeData.SYNC;
            }
        } else if (optName.equals(Settings.System.HAPTIC_FEEDBACK_ENABLED)) {
            boolean haptic = mCtrlUtils.isHapticOn();
            if (mCurrMode.getHapticOn() != haptic) {
                mCurrMode.setHapticOn(haptic);
                bChanged = true;
                str += "+";
                str += BatteryModeData.HAPTIC;
            }
        } else if (optName.equals(Settings.System.VIBRATE_WHEN_RINGING)) {
            boolean vibration = mCtrlUtils.isVibrateOn();
            if (mCurrMode.getVibrationOn() != vibration) {
                mCurrMode.setVibrationOn(vibration);
                bChanged = true;
                str += "+";
                str += BatteryModeData.VIBRATION;
            }
        } else if (optName.equals(Settings.System.ACCELEROMETER_ROTATION)) {
            boolean rotation = mCtrlUtils.isRotationOn();
            if (mCurrMode.getRotationOn() != rotation) {
                mCurrMode.setRotationOn(rotation);
                bChanged = true;
                str += "+";
                str += BatteryModeData.ROTATION;
            }
        } else if (optName.equals(Settings.Secure.WIFI_ON)) {
            boolean wifi = mCtrlUtils.isWifiOn_Setting();
            if (mCurrMode.getWiFiOn() != wifi) {
                mCurrMode.setWiFiOn(wifi);
                bChanged = true;
                str += "+";
                str += BatteryModeData.WIFI;
            }
        }
        
        Log.i(LOG_TAG_MODE, "onSettingChanged end of get--- " 
                + " radio=" + radio
                + " bChanged=" + bChanged);
        
        // set radio mode, BatteryLeftTime need to known radio status
        mCurrMode.setRadioOn(radio);

        if (bChanged) {
            // if any value changed, we assume changed by others
            // auto switching begin...
            Log.e(LOG_TAG_MODE, "onSettingChanged by other, about:" + str);

            // current setting changes
            // general is just the current setting
            // so change general mode to databases
            // change both database and memory
            saveGeneralMode();

            BatteryModeData generalMode = mPresetModes.get(PRESET_MODE_INDEX_GENERAL);
            if (null == generalMode) {
                return;
            }
            
            if (mCurrMode.getId() != generalMode.getId()) {
                // if current mode is not general mode, switching mode
                Log.e(LOG_TAG_MODE, "onSettingChanged {AUTO SWITCH}[general]");
                switchModeAuto(generalMode);
            } else {
                // current mode is general mode, also need to notify BatteryLeftTime
                broadcastModeChange();
            }
        }
    }

    public static BatteryModeMgr getInstance() {
        if (mSingleInstance == null) {
            mSingleInstance = new BatteryModeMgr();
        }

        return mSingleInstance;
    }

    public static void setContext(Context context) {
        if (mContext == null) {
            mContext = context;
        }
    }

    private BatteryModeMgr() {
        super();
        
        Log.e(LOG_TAG_MODE, "BatteryModeMgr constructor");

        mModesDAO = new BatteryModeDataSource(mContext);

        mCtrlUtils = new BatteryCtrlUtil(mContext);

        // 1. brightness
        mBrightnessObserver = new SettingsObserver(Settings.System.SCREEN_BRIGHTNESS, new Handler());
        mBrightnessModeObserver = new SettingsObserver(Settings.System.SCREEN_BRIGHTNESS_MODE,
                new Handler());

        // 2. timeout
        mTimeoutObserver = new SettingsObserver(Settings.System.SCREEN_OFF_TIMEOUT, new Handler());

        // 3. haptic
        mHapticObserver = new SettingsObserver(Settings.System.HAPTIC_FEEDBACK_ENABLED,
                new Handler());

        // 4. rotation
        mRotationObserver = new SettingsObserver(Settings.System.ACCELEROMETER_ROTATION,
                new Handler());

        // 5. mobile data
        mMobileDataObserver = new SettingsObserver(SETTINGS_SECURE_MOBILEDATA, new Handler());

        // 6. bluetooth
        mBluetoothObserver = new SettingsObserver(Settings.Secure.BLUETOOTH_ON, new Handler());

        // 7. wifi
        mWiFiObserver = new SettingsObserver(Settings.Secure.WIFI_ON, new Handler());

        // 8. vibration
        mVibrationObserver = new SettingsObserver(Settings.System.VIBRATE_WHEN_RINGING, new Handler());

        // 9. sync
        mSysBroadcastReceiver = new SysBroadcastReceiver();
        
        // battery mode switching flags
        mChangeMap = new HashMap<String, Boolean>();
        if (mChangeMap != null) {
            mChangeMap.put(BatteryModeData.BRIGHTNESS, false);
            mChangeMap.put(BatteryModeData.BRIGHTNESS_MODE, false);
            mChangeMap.put(BatteryModeData.TIMEOUT, false);
            mChangeMap.put(BatteryModeData.BLUETOOTH, false);
            mChangeMap.put(BatteryModeData.MOBILEDATA, false);
            mChangeMap.put(BatteryModeData.SYNC, false);
            mChangeMap.put(BatteryModeData.HAPTIC, false);
            mChangeMap.put(BatteryModeData.VIBRATION, false);
            mChangeMap.put(BatteryModeData.ROTATION, false);
            mChangeMap.put(BatteryModeData.WIFI, false);
            mChangeMap.put(BatteryModeData.RADIO, false);
        }
        mIsSwitching = false;
        mAirplaneToggle = false;
        mBtAction = TOGGLE_NONE;
        mWifiAction = TOGGLE_NONE;
    }

    public void init() {
        mRefCount++;
        if (mInitialized) {
            return;
        }

        try {
            mModesDAO.open();

            List<BatteryModeData> modeList = mModesDAO.getAllPresetMode();

            // no battery mode, create 3 preload battery mode
            if (modeList.size() == 0) {
                createPresetMode();
            } else {
                mPresetModes = (ArrayList<BatteryModeData>) modeList;
                mPresetModes.get(PRESET_MODE_INDEX_GENERAL).setPresetType(
                        BatteryModeData.PRESET_MODE_GENERAL);
                mPresetModes.get(PRESET_MODE_INDEX_NIGHT).setPresetType(
                        BatteryModeData.PRESET_MODE_NIGHT);
                mPresetModes.get(PRESET_MODE_INDEX_SAVER).setPresetType(
                        BatteryModeData.PRESET_MODE_SAVER);

                mCurrMode = mPresetModes.get(PRESET_MODE_INDEX_GENERAL);
            }

            // close database when not use, open-close paired
            mModesDAO.close();

            startAllObservers();

            mInitialized = true;
        } catch (Exception e) {
            Log.e(LOG_TAG_MODE, "BatteryModeMgr init exception");
            e.printStackTrace();
        }
    }

    public void deinit() {
        Log.e(LOG_TAG_MODE, "deinit called mRefCount=" + mRefCount + " mInitialized=" + mInitialized);
        mRefCount--;
        if (mInitialized && mRefCount == 0) {
            stopAllObservers();
            mInitialized = false;
        }
    }

    private void startAllObservers() {
        Log.e(LOG_TAG_MODE, "startAllObservers");
        // start observers
        // 1. brightness
        mBrightnessObserver.start();
        mBrightnessModeObserver.start();

        // 2. screen timeout
        mTimeoutObserver.start();

        // 3. haptic feedback
        mHapticObserver.start();

        // 4. auto rotation
        mRotationObserver.start();

        // 5. mobile data
        mMobileDataObserver.startSecure();

        // 6. bluetooth
        mBluetoothObserver.startSecure();

        // 7. wifi
        mWiFiObserver.startSecure();

        // 8. vibration
        mVibrationObserver.start();

        // 9. sync
        mSysBroadcastReceiver.start();
    }

    private void stopAllObservers() {
        Log.e(LOG_TAG_MODE, "stopAllObservers");
        // 1. brightness
        mBrightnessObserver.stop();
        mBrightnessModeObserver.stop();

        // 2. screen timeout
        mTimeoutObserver.stop();

        // 3. haptic feeback
        mHapticObserver.stop();

        // 4. auto rotation
        mRotationObserver.stop();

        // 5. mobile data
        mMobileDataObserver.stop();

        // 6. bluetooth
        mBluetoothObserver.stop();

        // 7. wifi
        mWiFiObserver.stop();

        // 8. vibration
        mVibrationObserver.stop();
        
        // 9. sync
        mSysBroadcastReceiver.stop();
    }
    
    /**
     * in modes.db we do not store RF radio status (on/off)
     * BatteryModeMgr do not care about RF radio status change
     * for we do not change RF radio status in Battery UI.
     * But BatteryLeftTime need to know RF radio status
     * @return RF radio status(on/off)
     */
    public boolean getRadioOn() {
        if (mCtrlUtils == null) {
            Log.e(LOG_TAG_MODE, "getRadioOn mCtrlUtils == null");
            return true;
        }
        
        return !mCtrlUtils.isAirplaneOn();
    }

    private void createPresetMode() {
        BatteryModeData mode;

        mPresetModes = new ArrayList<BatteryModeData>();

        // get airplane mode
        boolean radio = !mCtrlUtils.isAirplaneOn();

        // general mode is created from current settings, id = 1
        mode = mModesDAO.createBatteryMode(mContext.getString(R.string.bm_mode_general));
        mode.setPreset(true);
        mode.setPresetType(BatteryModeData.PRESET_MODE_GENERAL);
        mode.setBrightness(mCtrlUtils.getBrightness());
        mode.setBluetoothOn(mCtrlUtils.isBluetoothOn());
        mode.setTimeout(mCtrlUtils.getTimeout());
        mode.setWiFiOn(mCtrlUtils.isWifiOn());
        mode.setMobileDataOn(mCtrlUtils.isMobileDataEnabled());
        mode.setSyncOn(mCtrlUtils.isSyncOn());
        mode.setRadioOn(radio);
        mode.setHapticOn(mCtrlUtils.isHapticOn());
        mode.setVibrationOn(mCtrlUtils.isVibrateOn());
        mode.setRotationOn(mCtrlUtils.isRotationOn());
        mModesDAO.modifyBatteryMode(mode);

        mCurrMode = mode;
        saveCurrModeToPrefs();
        mPresetModes.add(mode);

        // saver mode, id = 2
        mode = mModesDAO.createBatteryMode(mContext.getString(R.string.bm_mode_saver));
        mode.setPreset(true);
        mode.setPresetType(BatteryModeData.PRESET_MODE_SAVER);
        mode.setBrightness(30);
        mode.setBluetoothOn(false);
        mode.setTimeout(30000);
        mode.setWiFiOn(true);
        mode.setMobileDataOn(false);
        mode.setRadioOn(radio);
        mode.setSyncOn(false);
        mode.setHapticOn(false);
        mode.setVibrationOn(true);
        mode.setRotationOn(false);
        mModesDAO.modifyBatteryMode(mode);
        mPresetModes.add(mode);

        // night mode, id = 3
        mode = mModesDAO.createBatteryMode(mContext.getString(R.string.bm_mode_night));
        mode.setPreset(true);
        mode.setPresetType(BatteryModeData.PRESET_MODE_NIGHT);
        mode.setBrightness(20);
        mode.setBluetoothOn(false);
        mode.setTimeout(15000);
        mode.setWiFiOn(false);
        mode.setMobileDataOn(false);
        mode.setSyncOn(false);
        mode.setRadioOn(radio);
        mode.setHapticOn(false);
        mode.setVibrationOn(true);
        mode.setRotationOn(false);
        mModesDAO.modifyBatteryMode(mode);
        mPresetModes.add(mode);
        
        Log.i(LOG_TAG_MODE, "createPresetMode end!");
    }

    /**
     * @param id
     * @return -1 for invalid preset mode id
     */
    public int getPresetModeType(long id) {
        if (mPresetModes != null) {
            for (int i = 0; i < mPresetModes.size(); i++) {
                if (mPresetModes.get(i).getId() == id) {
                    return mPresetModes.get(i).getPresetType();
                }
            }
        }

        return -1;
    }
    
    public String getModeUIName(BatteryModeData mode) {
        String str = new String();
        
        if (!mode.getPreset()) {
            str = mode.getModeName();
        } else {
            int type = getPresetModeType(mode.getId());
            switch(type) {
                case BatteryModeData.PRESET_MODE_GENERAL:
                    str = mContext.getString(R.string.bm_mode_general);
                    break;
                case BatteryModeData.PRESET_MODE_NIGHT:
                    str = mContext.getString(R.string.bm_mode_night);
                    break;
                case BatteryModeData.PRESET_MODE_SAVER:
                    str = mContext.getString(R.string.bm_mode_saver);
                    break;
                case BatteryModeData.PRESET_MODE_PERFORMANCE:
                    str = mContext.getString(R.string.bm_mode_performance);
                    break;
                default:
                    break;
            }
        }
        
        return str;
    }

    public boolean isModeNameUsed(String modeName) {
        if (modeName == null) {
            Log.e(LOG_TAG_MODE, "isModeNameUsed parm wrong");
            return false;
        }

        mModesDAO.open();
        boolean bUsed = mModesDAO.isModeNameUsed(modeName);
        mModesDAO.close();

        return bUsed;
    }

    public BatteryModeData createCustomizeMode(BatteryModeData val) {
        mModesDAO.open();
        int num = mModesDAO.getAllBatteryMode().size();

        String modeName = mContext.getString(R.string.bm_mode_prefix) + String.valueOf(num);
        BatteryModeData data = mModesDAO.createBatteryMode(modeName);
        boolean radio = !mCtrlUtils.isAirplaneOn();

        data.setModeName(val.getModeName());

        data.setBrightness(val.getBrightness());
        data.setTimeout(val.getTimeout());
        data.setBluetoothOn(val.getBluetoothOn());
        data.setWiFiOn(val.getWiFiOn());
        data.setSyncOn(val.getSyncOn());
        data.setMobileDataOn(val.getMobileDataOn());
        data.setRadioOn(radio);
        data.setHapticOn(val.getHapticOn());
        data.setVibrationOn(val.getVibrationOn());
        data.setRotationOn(val.getRotationOn());

        mModesDAO.modifyBatteryMode(data);

        mModesDAO.close();

        return data;
    }
    
    private void setModeChangeTimeout() {
        if (mModeChangeTimeout == null) {
            mModeChangeTimeout = new ModeChangeTimeoutRun();
        }
        
        Log.e(LOG_TAG_MODE, "setModeChangeTimeout ????");
        
        new Handler().postDelayed(mModeChangeTimeout, MODE_CHANGE_TIMEOUT_MS);
    }
    
    private void cancelModeChangeTimeout() {
        Log.e(LOG_TAG_MODE, "cancelModeChangeTimeout !!!!");
        
        new Handler().removeCallbacks(mModeChangeTimeout);
    }

    // TODO: maybe, we can apply mode parameter change according to current state
    // TODO: that is to say, get value from current state, and apply change according to newMode
    /**
     * apply mode parameter changes
     * @param newMode
     * @return
     * 
     * APPLY_CHANGE_RET_SUCESS, apply mode success, no pending
     * APPLY_CHANGE_RET_SUCESS_PENDING, apply mode success, but pending
     * APPLY_CHANGE_RET_FAIL, apply mode failed, must try it again.
     */
    private int applyModeParmChanges(BatteryModeData newMode) {
        Log.i(LOG_TAG_MODE, "applyChanges begin+++");
        
        boolean btStatus = mCtrlUtils.isBluetoothOn();
        boolean wifiStatus = mCtrlUtils.isWifiOn();
        
        Log.i(LOG_TAG_MODE, "currMode=" + mCurrMode.toString());
        Log.i(LOG_TAG_MODE, "newMode=" + newMode.toString());
        // wifi, bt status may be not accurate
        Log.e(LOG_TAG_MODE, "applyChanges wifi=" + wifiStatus
                + " bt=" + btStatus);
        
        int ret = APPLY_CHANGE_RET_SUCCESS;
        
        // BT change is asynchronous
        if (newMode.getBluetoothOn() != btStatus) {
            Log.i(LOG_TAG_MODE, "begin to toggle bt");
            
            if (mCtrlUtils.toggleBluetooth(newMode.getBluetoothOn())) {
                mChangeMap.put(BatteryModeData.BLUETOOTH, true);
                mIsSwitching = true;
                ret = APPLY_CHANGE_RET_SUCCESS_PENDING;
                
                if (newMode.getBluetoothOn()) {
                    mBtAction = TOGGLE_ON;
                } else {
                    mBtAction = TOGGLE_OFF;
                }
            } else {
                Log.e(LOG_TAG_MODE, "applyModeParmChanges, BT failed");
                Toast.makeText(mContext, R.string.bm_bt_fail, Toast.LENGTH_SHORT).show();
                return APPLY_CHANGE_RET_FAIL;
            }
        }
        
        // WiFi change is asynchronous
        if (newMode.getWiFiOn() != wifiStatus) {
            Log.i(LOG_TAG_MODE, "begin to toggle wifi");
            
            if (mCtrlUtils.toggleWifi(newMode.getWiFiOn())) {
                mChangeMap.put(BatteryModeData.WIFI, true);
                mIsSwitching = true;
                ret = APPLY_CHANGE_RET_SUCCESS_PENDING;
                
                if (newMode.getWiFiOn()) {
                    mWifiAction = TOGGLE_ON;
                } else {
                    mWifiAction = TOGGLE_OFF;
                }
            } else {
                Log.e(LOG_TAG_MODE, "applyModeParmChanges, Wifi failed");
                Toast.makeText(mContext, R.string.bm_wifi_fail, Toast.LENGTH_SHORT).show();
                return APPLY_CHANGE_RET_FAIL;
            }
        }
        
        // TODO: check with: will the following changes fail somewhere? if fail, we need to handle failed state
        // TODO: check brightness changes auto-->manual, manual-->auto
        int newBrightness = newMode.getBrightness();
        int oldBrightness = mCurrMode.getBrightness();
        if (newBrightness != oldBrightness) {
            mCtrlUtils.setBrightness(newMode.getBrightness());
            if (newBrightness == -1 || oldBrightness == -1) {
                // auto<->manual mode switch
                mChangeMap.put(BatteryModeData.BRIGHTNESS_MODE, true);
            }
            mChangeMap.put(BatteryModeData.BRIGHTNESS, true);
        }

        if (newMode.getTimeout() != mCurrMode.getTimeout()) {
            mCtrlUtils.setTimeout(newMode.getTimeout());
            mChangeMap.put(BatteryModeData.TIMEOUT, true);
        }
        
        // TODO: whether mobile data turn will fail somewhere
        if (newMode.getMobileDataOn() != mCurrMode.getMobileDataOn()) {
            mCtrlUtils.setMobileDataEnabled(newMode.getMobileDataOn());
            mChangeMap.put(BatteryModeData.MOBILEDATA, true);
        }
        
        // TODO: whether account sync turn will fail some where
        if (newMode.getSyncOn() != mCurrMode.getSyncOn()) {
            mCtrlUtils.toggleSync(newMode.getSyncOn());
            mChangeMap.put(BatteryModeData.SYNC, true);
        }

        if (newMode.getHapticOn() != mCurrMode.getHapticOn()) {
            mCtrlUtils.toggleHaptic(newMode.getHapticOn());
            mChangeMap.put(BatteryModeData.HAPTIC, true);
        }

        if (newMode.getVibrationOn() != mCurrMode.getVibrationOn()) {
            mCtrlUtils.toggleVibrate(newMode.getVibrationOn());
            mChangeMap.put(BatteryModeData.VIBRATION, true);
        }

        if (newMode.getRotationOn() != mCurrMode.getRotationOn()) {
            mCtrlUtils.toggleRotation(newMode.getRotationOn());
            mChangeMap.put(BatteryModeData.ROTATION, true);
        }

        Log.i(LOG_TAG_MODE, "applyChanges end---ret=" + ret + 
                " mBtAction=" + mBtAction + 
                " mWifiAction=" + mWifiAction);
        
        return ret;
    }

    public int saveCustomizeMode(BatteryModeData data) {
        int ret = APPLY_CHANGE_RET_SUCCESS;
        
        if (mCurrMode == null) {
            Log.e(LOG_TAG_MODE, "saveCustomizeMode currMode==null");
            mCurrMode = getCurrModeFromPrefs();
        }
        
        mUIToast = false;
        
        // apply mode parameter changes if the mode is the current mode
        if (mCurrMode.getId() == data.getId()) {    
            ret = applyModeParmChanges(data);
            
            if (ret == APPLY_CHANGE_RET_SUCCESS
                    || ret == APPLY_CHANGE_RET_SUCCESS_PENDING) {
                // pending or not? just save the final status
                mCurrMode = data;
                saveGeneralMode();
                broadcastModeChange();
                mIsSwitching = true;
                
                // save customize mode to database
                mModesDAO.open();
                mModesDAO.modifyBatteryMode(data);
                mModesDAO.close();
            }
            
            if (ret == APPLY_CHANGE_RET_SUCCESS) {
                // nothing to complete
                mIsSwitching = false;
                broadcastModeCompleted();
            } else if (ret == APPLY_CHANGE_RET_SUCCESS_PENDING) {
                mIsSwitching = true;
                setModeChangeTimeout();
            } else {
                mIsSwitching = false;
                // TODO: check with, if apply changes fail, do we need to recover from error?
                Log.e(LOG_TAG_MODE, "saveCustomizeMode failed");
            }
        } else {
            // change battery mode data without apply changes
            // save customize mode to database
            mModesDAO.open();
            mModesDAO.modifyBatteryMode(data);
            mModesDAO.close();
        }
        
        return ret;
    }

    public void saveGeneralMode() {
        if (mCurrMode == null) {
            Log.e(LOG_TAG_MODE, "saveGeneralMode mCurrMode==null");
            return;
        }
        
        Log.e(LOG_TAG_MODE, "saveGeneralMode mCurrMode=" + mCurrMode.toString());

        mModesDAO.open();
        BatteryModeData data = mPresetModes.get(PRESET_MODE_INDEX_GENERAL);
        if (data != null) {
            // only save parameter data
            // 1. memory
            data.setBrightness(mCurrMode.getBrightness());
            data.setTimeout(mCurrMode.getTimeout());
            data.setWiFiOn(mCurrMode.getWiFiOn());
            data.setBluetoothOn(mCurrMode.getBluetoothOn());
            data.setMobileDataOn(mCurrMode.getMobileDataOn());
            data.setSyncOn(mCurrMode.getSyncOn());
            data.setHapticOn(mCurrMode.getHapticOn());
            data.setVibrationOn(mCurrMode.getVibrationOn());
            data.setRotationOn(mCurrMode.getRotationOn());
            data.setRadioOn(mCurrMode.getRadioOn());
            // 2. database
            mModesDAO.modifyBatteryMode(data);
        } else {
            Log.e(LOG_TAG_MODE, "preset general mode not found");
        }
        mModesDAO.close();
    }
    
    private void broadcastModeChange() {
        Intent intent = new Intent();
        intent.setAction(ACTION_BATTERY_MODE_CHANGED);
        
        mContext.sendBroadcast(intent);
        
        Log.e(LOG_TAG_MODE, "[broadcast]--> MODE_CHANGED");
    }
    
    private void broadcastModeCompleted() {
        Intent intent = new Intent();
        intent.setAction(ACTION_BATTERY_MODE_CHANGE_COMPLETED);
        
        mContext.sendBroadcast(intent);
        
        if (mUIToast) {
            Log.e(LOG_TAG_MODE, "$$$$ complete: SWITCHED_TOAST $$$$");
            String str = String.format(mContext.getString(R.string.bm_mode_switching), 
                    getModeUIName(mCurrMode));
            Toast.makeText(mContext, str, Toast.LENGTH_SHORT).show();
        }
        
        Log.e(LOG_TAG_MODE, "[broadcast]--> MODE_CHANGE_COMPLETED");
    }
    
    /**
     * Now, the function is only intended for auto switch to general mode
     * @param newMode
     * @return
     */
    public int switchModeAuto(BatteryModeData newMode) {
        if (newMode == null || mCurrMode == null) {
            Log.e(LOG_TAG_MODE, "parameter error");
            return APPLY_CHANGE_RET_FAIL;
        }

        Log.e(LOG_TAG_MODE, "auto switch to <mode>=" + newMode.getModeName());
        
        int ret = APPLY_CHANGE_RET_SUCCESS;
        // auto switch, does not need to toast
        mUIToast = false;
        
        // DELETE for: auto switch to general mode, did not need to apply changes
        /*
        // intensive working, apply mode changes
        ret = applyModeParmChanges(newMode);
        
        // apply changes failed, bail out...
        if (ret == APPLY_CHANGE_RET_FAIL) {
            Log.e(LOG_TAG_MODE, "auto switch, apply failed***");
            // TODO: check with, do we need to recover from error?
            return ret;
        }
        */

        // pending or not? just save the final status
        // current mode value changed
        mCurrMode = newMode;

        // save current mode id to preferences
        saveCurrModeToPrefs();
        
        // DELETE for: general mode already saved pre this function call
        /*
        // save current mode to database [general mode]
        // save both in memory and database   
        saveGeneralMode();
        */ 
        
        // broadcast battery mode change
        broadcastModeChange();
        
        mIsSwitching = true;
        
        if (ret == APPLY_CHANGE_RET_SUCCESS) {
            // nothing to complete
            mIsSwitching = false;
            broadcastModeCompleted();
        } else if (ret == APPLY_CHANGE_RET_SUCCESS_PENDING) {
            setModeChangeTimeout();
        }
        
        return ret;
    }

    public int switchMode(BatteryModeData newMode) {
        if (newMode == null || mCurrMode == null) {
            Log.e(LOG_TAG_MODE, "parameter error");
            return APPLY_CHANGE_RET_FAIL;
        }

        Log.e(LOG_TAG_MODE, "manual switch to <mode>=" + newMode.getModeName());
        
        // intensive working, apply mode changes
        int ret = applyModeParmChanges(newMode);
        
        // apply changes failed, bail out
        if (ret == APPLY_CHANGE_RET_FAIL) {
            Log.e(LOG_TAG_MODE, "manual switch failed***");
            // TODO: check with: do we need to recover from error?
            return ret;
        }
        
        // need UI toast "switching to ... Foo mode"
        mUIToast = true;
        
        // pending or not? just save the final status
        // current mode value changed
        mCurrMode = newMode;

        // save current mode id to preferences
        saveCurrModeToPrefs();
        
        // save current mode to database [general mode]
        // save both in memory and database   
        saveGeneralMode(); 
        
        // broadcast battery mode change
        broadcastModeChange();
        
        mIsSwitching = true;
        
        // when manual switching, ignore airplane toggle. 
        mAirplaneToggle = false;
        
        if (ret == APPLY_CHANGE_RET_SUCCESS) {
            // nothing to complete
            mIsSwitching = false;
            broadcastModeCompleted();
        } else if (ret == APPLY_CHANGE_RET_SUCCESS_PENDING){
            setModeChangeTimeout();
        }
        
        return ret;
    }

    public void removeMode(long id) {
        mModesDAO.open();
        mModesDAO.deleteBatteryMode(id);
        mModesDAO.close();
    }
    
    public BatteryModeData checkCurrModeOnStart() {
        // get current mode from preference and database
        getCurrModeFromPrefs();
        
        // get current setting values
        int brightness = mCtrlUtils.getBrightness();
        int timeout = mCtrlUtils.getTimeout();
        boolean btStatus = mCtrlUtils.isBluetoothOn();
        Log.i(LOG_TAG_MODE, "checkCurrModeOnStart btStatus=" + btStatus);
        boolean mobileData = mCtrlUtils.isMobileDataEnabled();
        boolean sync = mCtrlUtils.isSyncOn();
        boolean haptic = mCtrlUtils.isHapticOn();
        boolean vibration = mCtrlUtils.isVibrateOn();
        boolean rotation = mCtrlUtils.isRotationOn();
        boolean wifiStatus = mCtrlUtils.isWifiOn();
        Log.i(LOG_TAG_MODE, "checkCurrModeOnStart wifiStatus=" + wifiStatus);
        
        // compare each setting value with current mode
        boolean bMisMatch = false;
        Log.i(LOG_TAG_MODE, "checkCurrModeOnStart currMode=" + mCurrMode.toString());
        if (mCurrMode.getBrightness() != brightness) {
            Log.e(LOG_TAG_MODE, "checkCurrModeOnStart brightness not match");
            mPresetModes.get(PRESET_MODE_INDEX_GENERAL).setBrightness(brightness);
            bMisMatch = true;
        }
        if (mCurrMode.getTimeout() != timeout) {
            Log.e(LOG_TAG_MODE, "checkCurrModeOnStart timeout not match");
            mPresetModes.get(PRESET_MODE_INDEX_GENERAL).setTimeout(timeout);
            bMisMatch = true;
        }
        if (mCurrMode.getBluetoothOn() != btStatus) {
            Log.e(LOG_TAG_MODE, "checkCurrModeOnStart bluetooth not match");
            mPresetModes.get(PRESET_MODE_INDEX_GENERAL).setBluetoothOn(btStatus);
            bMisMatch = true;
        }
        if (mCurrMode.getMobileDataOn() != mobileData) {
            Log.e(LOG_TAG_MODE, "checkCurrModeOnStart mobiledata not match");
            mPresetModes.get(PRESET_MODE_INDEX_GENERAL).setMobileDataOn(mobileData);
            bMisMatch = true;
        }
        if (mCurrMode.getSyncOn() != sync) {
            Log.e(LOG_TAG_MODE, "checkCurrModeOnStart sync not match");
            mPresetModes.get(PRESET_MODE_INDEX_GENERAL).setSyncOn(sync);
            bMisMatch = true;
        }
        if (mCurrMode.getHapticOn() != haptic) {
            Log.e(LOG_TAG_MODE, "checkCurrModeOnStart haptic not match");
            mPresetModes.get(PRESET_MODE_INDEX_GENERAL).setHapticOn(haptic);
            bMisMatch = true;
        }
        if (mCurrMode.getVibrationOn() != vibration) {
            Log.e(LOG_TAG_MODE, "checkCurrModeOnStart vibration not match");
            mPresetModes.get(PRESET_MODE_INDEX_GENERAL).setVibrationOn(vibration);
            bMisMatch = true;
        }
        if (mCurrMode.getRotationOn() != rotation) {
            Log.e(LOG_TAG_MODE, "checkCurrModeOnStart rotation not match");
            mPresetModes.get(PRESET_MODE_INDEX_GENERAL).setRotationOn(rotation);
            bMisMatch = true;
        }
        if (mCurrMode.getWiFiOn() != wifiStatus) {
            Log.e(LOG_TAG_MODE, "checkCurrModeOnStart wifi not match");
            mPresetModes.get(PRESET_MODE_INDEX_GENERAL).setWiFiOn(wifiStatus);
            bMisMatch = true;
        }
        
        // if any setting value mismatch, auto change to general
        // very dangerous, change to "general" mode on start
        if (bMisMatch) {
            Log.e(LOG_TAG_MODE, "====DANGEROUS====: MISMATCH auto change to [GENERAL]");
            mCurrMode = mPresetModes.get(PRESET_MODE_INDEX_GENERAL);
            
            saveGeneralMode();
            saveCurrModeToPrefs();
        }  
        
        return mCurrMode;
    }
    
    public boolean isModeSwiching() {
        return mIsSwitching;
    }
    
    public BatteryModeData getCurrMode() {
        boolean bluetooth = mCtrlUtils.isBluetoothOn();
        boolean wifi = mCtrlUtils.isWifiOn();
        if (mCurrMode != null) {
            mCurrMode.setBluetoothOn(bluetooth);
            mCurrMode.setWiFiOn(wifi);
            Log.i(LOG_TAG_MODE, "getCurrMode currMode=" + mCurrMode.toString());
            return mCurrMode.clone();
        } else {
            return getCurrModeFromPrefs();
        }
    }

    public BatteryModeData getCurrModeFromPrefs() {
        SharedPreferences sharePrefs = mContext.getSharedPreferences(BATTERY_MODE_PREFS, 0);
        long currModeId = sharePrefs.getLong(BATTERY_CURR_MODE_ID, -1);

        mModesDAO.open();
        if (currModeId != -1) {
            mCurrMode = mModesDAO.getBatteryMode(currModeId);
        } else {
            Log.e(LOG_TAG_MODE, "mode id == -1");
        }

        if (mCurrMode == null) {
            Log.e(LOG_TAG_MODE, "getCurrModeFromPrefs mCurrMode null");
            // inconsistent with preference
            // force the first mode as the current mode.
            List<BatteryModeData> list = mModesDAO.getAllBatteryMode();
            if (list != null) {
                mCurrMode = list.get(0);
            }

            saveCurrModeToPrefs();
        }
        mModesDAO.close();
        
        boolean bluetooth = mCtrlUtils.isBluetoothOn();
        boolean wifi = mCtrlUtils.isWifiOn();
        mCurrMode.setBluetoothOn(bluetooth);
        mCurrMode.setWiFiOn(wifi);
        Log.i(LOG_TAG_MODE, "getCurrModeFromPrefs currMode=" + mCurrMode.toString());
        return mCurrMode.clone();
    }

    public BatteryModeData getModeFromId(long id) {
        mModesDAO.open();
        BatteryModeData data = mModesDAO.getBatteryMode(id);
        mModesDAO.close();
        return data;
    }

    public void saveCurrModeToPrefs() {
        SharedPreferences sharePrefs = mContext.getSharedPreferences(BATTERY_MODE_PREFS, 0);
        SharedPreferences.Editor editor = sharePrefs.edit();
        if (mCurrMode != null) {
            editor.putLong(BATTERY_CURR_MODE_ID, mCurrMode.getId());
            editor.putString(BATTERY_CURR_MODE, mCurrMode.getModeName());
        }
        editor.commit();
    }

    public List<BatteryModeData> getAllMode() {
        mModesDAO.open();
        List<BatteryModeData> list = mModesDAO.getAllBatteryMode();
        mModesDAO.close();
        return list;
    }
    
    public int getModeNums() {
        int num = 0;
        mModesDAO.open();
        num = mModesDAO.getAllBatteryMode().size();
        mModesDAO.close();
        return num;
    }
    
    public void setAutoBrightness(boolean bAuto) {
        if (mCtrlUtils == null) {
            return;
        }
        
        int currBrightness = mCtrlUtils.getBrightness();
        
        if (currBrightness != -1 && bAuto) {
            mCtrlUtils.setAutoBrightness(bAuto);
            mChangeMap.put(BatteryModeData.BRIGHTNESS_MODE, true);
        } else if (currBrightness == -1 && !bAuto) {
            mCtrlUtils.setAutoBrightness(bAuto);
            mChangeMap.put(BatteryModeData.BRIGHTNESS_MODE, true);
        }
    }
    
    public int recoverBrightnessMode() {
        if (mCtrlUtils == null || mCurrMode == null) {
            Log.e(LOG_TAG_MODE, "recoverBrightness NEVER reach");
            return -2;
        }
        
        int modeBrightness = mCurrMode.getBrightness();
        boolean modeAuto = (modeBrightness == -1);
        int modeValue = 0;
        if (modeBrightness != -1) {
            modeValue = modeBrightness;
        }
        boolean bCurrAuto = mCtrlUtils.getAutoBrightness();
        int nCurrValue = mCtrlUtils.getBrightnessValue();
        
        Log.e(LOG_TAG_MODE, "recoverBrightnessMode currModeBright=" + modeBrightness
                + " bCurrAuto=" + bCurrAuto + " nCurrValue=" + nCurrValue);
        
        // check to see if we should change mode
        if (modeAuto != bCurrAuto) {
            Log.i(LOG_TAG_MODE, "recoverBrightnessMode: change brightness mode to=" + modeAuto);
            mCtrlUtils.setAutoBrightness(modeAuto);
            mChangeMap.put(BatteryModeData.BRIGHTNESS_MODE, true);
        }
        
        // check to see if we need to change value
        if (!modeAuto) {
            Log.e(LOG_TAG_MODE, "???recoverBrightnessMode: change brightness value to=" + modeValue + " current value=" + nCurrValue);
            mCtrlUtils.setBrightnessValue(modeValue);
            if (modeValue != nCurrValue) {
                // brightness value will change
                mChangeMap.put(BatteryModeData.BRIGHTNESS, true);
            }        
        } 
        
        return modeBrightness;
    }
}
