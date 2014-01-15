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
import android.content.ContentResolver;
import android.content.Context;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BatteryCtrlUtil {
    private static final String LOG_TAG_MODE = "BattCtrl: ";
    private static final int FULL_BRIGHTNESS_VAL = 255;

    private Context mContext;

    public BatteryCtrlUtil(Context context) {
        mContext = context;
    }

    /**
     * toggle vibrate
     * 
     * @param context
     * @param bOn
     */
    public void toggleVibrate(boolean bOn) {
        /*
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        if (bOn) {
            audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,
                    AudioManager.VIBRATE_SETTING_ON);
        } else {
            audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,
                    AudioManager.VIBRATE_SETTING_OFF);
        }
        */
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.VIBRATE_WHEN_RINGING, bOn ? 1 : 0);

    }

    /**
     * @return vibration on/off
     */
    public boolean isVibrateOn() {
        /*
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        return (AudioManager.VIBRATE_SETTING_ON == audioManager
                .getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER));
        */

        int val = 0;

        try {
            val = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.VIBRATE_WHEN_RINGING);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(LOG_TAG_MODE, "isVibrateOn exception");
            e.printStackTrace();
        }

        return (val == 1);
    }

    /**
     * bntw34 toggle auto sync
     * 
     * @param context
     * @param bOn
     */
    public void toggleSync(boolean bOn) {
        // master auto-sync applies to all account and provider
        ContentResolver.setMasterSyncAutomatically(bOn);
    }

    public boolean isSyncOn() {
        return ContentResolver.getMasterSyncAutomatically();
    }

    /**
     * bntw34 toggle haptic
     * 
     * @param context
     * @param bOn
     */
    public void toggleHaptic(boolean bOn) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, bOn ? 1 : 0);
    }

    public boolean isHapticOn() {
        int val = 0;

        try {
            val = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.HAPTIC_FEEDBACK_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(LOG_TAG_MODE, "isHapticOn exception");
            e.printStackTrace();
        }

        return (val == 1);
    }

    /**
     * bntw34 toggle rotation
     * 
     * @param context
     * @param bOn
     */
    public void toggleRotation(boolean bOn) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, bOn ? 1 : 0);
    }

    public boolean isRotationOn() {
        int val = 0;
        try {
            val = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(LOG_TAG_MODE, "isRotationOn exception");
            e.printStackTrace();
        }

        return (val == 1);
    }

    /**
     * bntw34 toggle Airplane mode
     * 
     * @param context
     * @param bOn
     */
    public void toggleAirplane(boolean bOn) {
        // TODO: remove due to security consideration
    }

    public boolean isAirplaneOn() {
        int val = 0;
        try {
            val = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(LOG_TAG_MODE, "isAirplane exception");
            e.printStackTrace();
        }

        return (val != 0);
    }

    /**
     * bntw34 toggle timeout
     * 
     * @param context
     * @param timeout in milliseconds, refers
     *            packages/apps/Settings/DisplaySetting.java
     *            R.xml.display_settings
     */
    public void setTimeout(int timeout) {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT,
                timeout);
    }

    public int getTimeout() {
        int val = 0;
        try {
            val = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(LOG_TAG_MODE, "getTimeout exception");
            e.printStackTrace();
        }

        return val;
    }

    /**
     * bntw34 toggle GPS
     * 
     * @param context
     * @param bOn requires WRITE_SECURE_SETTING permission which is signature
     *            protected
     */
    public void toggleGPS(boolean bOn) {
        Settings.Secure.setLocationProviderEnabled(mContext.getContentResolver(),
                LocationManager.GPS_PROVIDER, bOn);
    }

    public boolean isGPSOn() {
        return Settings.Secure.isLocationProviderEnabled(mContext.getContentResolver(),
                LocationManager.GPS_PROVIDER);

    }
    
    public void setAutoBrightness(boolean bAuto) {
        Log.e(LOG_TAG_MODE, "setAutoBrightness bAuto=" + bAuto);
        Settings.System.putInt(mContext.getContentResolver(), 
                Settings.System.SCREEN_BRIGHTNESS_MODE, 
                bAuto ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }
    
    public boolean getAutoBrightness() {
        boolean bAuto = false;
        
        // check brightness auto
        int val = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        
        bAuto = (val == 1);
        
        return bAuto;
    }
    
    public void setBrightnessValue(int val) {
        Log.e(LOG_TAG_MODE, "setBrightnessValue val=" + val);
        int setVal = (val * FULL_BRIGHTNESS_VAL / 100);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, 
                setVal);
    }
    
    public int getBrightnessValue() {
        int val = 0;
        try {
            val = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(LOG_TAG_MODE, "getBrightness exception");
            e.printStackTrace();
        }
        
        return ((val * 99 + FULL_BRIGHTNESS_VAL) / FULL_BRIGHTNESS_VAL);
    }

    /**
     * bntw34 sets brightness through system provider
     * 
     * @param context
     * @param val
     */
    public void setBrightness(int val) {
        Log.e(LOG_TAG_MODE, "setBrightness val=" + val);
        if (val == -1) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        } else {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            
            // do not set brightness lower than 8%, for it is so dim
            if (val < BatteryModeData.BRIGHTNESS_MIN_VALUE) {
                val = BatteryModeData.BRIGHTNESS_MIN_VALUE;
            }
            
            int setVal = (val * FULL_BRIGHTNESS_VAL / 100);
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    setVal);
        }      
    }

    public int getBrightness() {
        int val = 0; // invalid value

        // check brightness auto
        val = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        if (val == 1) {
            return -1;
        }

        try {
            val = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(LOG_TAG_MODE, "getBrightness exception");
            e.printStackTrace();
        }

        return ((val * 99 + FULL_BRIGHTNESS_VAL) / FULL_BRIGHTNESS_VAL);
    }

    /**
     * bntw34 toggles Wifi
     * 
     * @param context
     * @param enabled
     */
    public boolean toggleWifi(boolean enabled) {
        boolean bRet = false;
        Log.e(LOG_TAG_MODE, "toggleWifi getService****");
        WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        Log.e(LOG_TAG_MODE, "setWiFiEnable begin+++");
        bRet = wm.setWifiEnabled(enabled);
        Log.e(LOG_TAG_MODE, "setWiFiEnable end--- to " + enabled + " bRet=" + bRet);
        
        return bRet;
    }
    
    public boolean isWifiOn_Setting() {
        int val = 0;
        try {
            val = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.WIFI_ON);
            Log.i(LOG_TAG_MODE, "isWifiOn_Setting val=" + val);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(LOG_TAG_MODE, "isWifiOn_Setting SettingNotFoundException");
            e.printStackTrace();
        }
        
        // TODO: double check with the returning value from Settings.Secure.WIFI_ON
        return (val == 1 || val == 2);
    }

    public boolean isWifiOn() {
        WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        int state = wm.getWifiState();
        Log.i(LOG_TAG_MODE, "isWifiOn state=" + state);
        return (state != WifiManager.WIFI_STATE_DISABLED && state != WifiManager.WIFI_STATE_DISABLING);
    }
    
    public boolean isWifiOff() {
        WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        int state = wm.getWifiState();
        Log.i(LOG_TAG_MODE, "isWifiOff state=" + state);
        return (state == WifiManager.WIFI_STATE_DISABLED);
    }
    
    public int getWifiState() {
        int state = WifiManager.WIFI_STATE_UNKNOWN;
        WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            state = wm.getWifiState();
        }
        return state;
    }

    /**
     * bntw34 toggles Bluetooth
     * 
     * @param context
     * @param enabled
     */
    public boolean toggleBluetooth(boolean enabled) {
        Log.e(LOG_TAG_MODE, "toggleBluetooth getDefaultAdapter***");
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        boolean bRet = false;
        Log.e(LOG_TAG_MODE, "toggleBluetooth operate begin+++ to=" + enabled);
        if (adapter != null) {
            if (enabled) {
                bRet = adapter.enable();
                // in middle state, the call will return false
                Log.e(LOG_TAG_MODE, "BT enabled bRet=" + bRet);
            } else {
                bRet = adapter.disable();
                // in middle state, the call will return false
                Log.e(LOG_TAG_MODE, "BT disabled bRet=" + bRet);
            }
        }
        Log.e(LOG_TAG_MODE, "toggleBluetooth operate end---");
        
        return bRet;
    }
    
    public boolean isBluetoothOn_Setting() {
        int val = 0;
        try {
            val = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.BLUETOOTH_ON);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(LOG_TAG_MODE, "isBluetoothOn SettingNotFoundException");
            e.printStackTrace();
        }
        
        Log.i(LOG_TAG_MODE, "isBluetoothOn_Setting val=" + val);
        
        return (val == 1);
    }

    public boolean isBluetoothOn() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter != null) {
            int state = adapter.getState();
            Log.i(LOG_TAG_MODE, "isBluetoothOn state=" + state);
            return (state != BluetoothAdapter.STATE_OFF && state != BluetoothAdapter.STATE_TURNING_OFF);
            //return adapter.isEnabled();
        } else {
            Log.e(LOG_TAG_MODE, "BluetoothAdapter null");
            return false;
        }
    }
    
    public boolean isBluetoothOff() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter != null) {
            int state = adapter.getState();
            Log.i(LOG_TAG_MODE, "isBluetoothOn state=" + state);
            return (state == BluetoothAdapter.STATE_OFF);
            //return adapter.isEnabled();
        } else {
            Log.e(LOG_TAG_MODE, "BluetoothAdapter null");
            return false;
        }
    }
    
    public int getBluetoothState() {
        int state = BluetoothAdapter.STATE_OFF;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            state = adapter.getState();
        }
        Log.e(LOG_TAG_MODE, "getBluetoothState state=" + state);
        return state;
    }

    /**
     * bntw34 sets mobile data enabled/disabled using Java reflection technique
     * requires
     * 
     * @param context
     * @param enabled
     */
    public void setMobileDataEnabled(boolean enabled) {
        final ConnectivityManager conman = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            final Class<?> conmanClass = Class.forName(conman.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(conman);
            final Class<?> iConnectivityManagerClass =
                    Class.forName(iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod =
                    iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled",
                            Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);
            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
            Log.i(LOG_TAG_MODE, "setMobileDataEnabled=" + enabled);
        } catch (ClassNotFoundException e) {
            Log.e(LOG_TAG_MODE,
                    "setMobileData classnotfind exception");
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            Log.e(LOG_TAG_MODE,
                    "setMobileData nosuchfield exception");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(LOG_TAG_MODE,
                    "setMobileData illegal acess exception");
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Log.e(LOG_TAG_MODE,
                    "setMobileData nosuchmethod exception");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG_MODE,
                    "setMobileData illegalArgument exception");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.e(LOG_TAG_MODE,
                    "setMobileData invocation target exception");
            e.printStackTrace();
        }

        /*
        Method dataConnSwitchMethod;
        Class<? extends TelephonyManager> telephonyManagerClass;
        Object ITelephonyStub;
        Class<?> ITelephonyClass;
        TelephonyManager telephonyManager = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);

        try {
            // telephonyManagerClass =
            // Class.forName(telephonyManager.getClass().getName());
            telephonyManagerClass = telephonyManager.getClass();
            Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
            getITelephonyMethod.setAccessible(true);
            ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
            ITelephonyClass = Class.forName(ITelephonyStub.getClass().getName());

            if (enabled) {
                dataConnSwitchMethod = ITelephonyClass
                        .getDeclaredMethod("disableDataConnectivity");
            } else {
                dataConnSwitchMethod = ITelephonyClass
                        .getDeclaredMethod("enableDataConnectivity");
            }
            dataConnSwitchMethod.setAccessible(true);
            dataConnSwitchMethod.invoke(ITelephonyStub);
            Log.i(LOG_TAG_MODE, "setMobileDataEnabled enabled=" + enabled);
        } catch (ClassNotFoundException e) {
            Log.e(LOG_TAG_MODE,
                    "setMobileData classnotfind exception");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(LOG_TAG_MODE,
                    "setMobileData illegal acess exception");
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Log.e(LOG_TAG_MODE,
                    "setMobileData nosuchmethod exception");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG_MODE,
                    "setMobileData illegalArgument exception");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.e(LOG_TAG_MODE,
                    "setMobileData invocation target exception");
            e.printStackTrace();
        }
        */
    }

    public boolean isMobileDataEnabled() {
        boolean bOn = false;

        final ConnectivityManager conman = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            final Class<?> conmanClass = Class.forName(conman.getClass().getName());
            final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField.get(conman);
            final Class<?> iConnectivityManagerClass =
                    Class.forName(iConnectivityManager.getClass().getName());
            final Method getMobileDataEnabledMethod = iConnectivityManagerClass
                    .getDeclaredMethod("getMobileDataEnabled");
            getMobileDataEnabledMethod.setAccessible(true);
            bOn = ((Boolean) getMobileDataEnabledMethod.invoke(iConnectivityManager));
            Log.e(LOG_TAG_MODE, "isMobileDataEnabled bOn=" + bOn);
            return bOn;
        } catch (ClassNotFoundException e) {
            Log.e(LOG_TAG_MODE,
                    "getMobileData classnotfind exception");
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            Log.e(LOG_TAG_MODE,
                    "getMobileData nosuchfield exception");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG_MODE,
                    "getMobileData illegalArgument exception");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(LOG_TAG_MODE,
                    "getMobileData illegalAccess exception");
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            Log.e(LOG_TAG_MODE,
                    "getMobileData nosuchmethod exception");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.e(LOG_TAG_MODE,
                    "getMobileData invocationTarget exception");
            e.printStackTrace();
        }
        Log.e(LOG_TAG_MODE, "IsMobileDataEnabled ERROR=====");
        return false;

        /*
        boolean isEnabled = false;

        TelephonyManager telephonyManager = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);

        if (telephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED) {
            isEnabled = true;
        } else {
            isEnabled = false;
        }

        Log.i(LOG_TAG_MODE, "isMobileDataOn isEnabled=" + isEnabled);

        return isEnabled;
        */
    }
}
