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

/**
 * battery mode data model
 * 
 * @author BNTW34
 */
public class BatteryModeData {
    public static final String BRIGHTNESS = "brightness";
    public static final String BRIGHTNESS_MODE = "brightness_mode";
    public static final String TIMEOUT = "timeout";
    public static final String WIFI = "wifi";
    public static final String BLUETOOTH = "bluetooth";
    public static final String MOBILEDATA = "mobiledata";
    public static final String SYNC = "sync";
    public static final String RADIO = "radio";
    public static final String HAPTIC = "haptic";
    public static final String VIBRATION = "vibration";
    public static final String ROTATION = "rotation";
    public static final int PRESET_MODE_GENERAL = 1;
    public static final int PRESET_MODE_SAVER = 2;
    public static final int PRESET_MODE_NIGHT = 3;
    public static final int PRESET_MODE_PERFORMANCE = 4;
    
    public static final int BRIGHTNESS_MIN_VALUE = 8;
    public static final int BRIGHTNESS_LEVEL_1 = 12;
    public static final int BRIGHTNESS_LEVEL_2 = 40;
    public static final int BRIGHTNESS_LEVEL_3 = 100;

    private long id;
    private String modeName;
    private boolean bPreset;
    private int presetType;
    private int brightness; // [-1, 100], -1 for auto brightness
    private int timeout; // -1 for never timeout
    private boolean bWiFiOn;
    private boolean bBluetoothOn;
    private boolean bMobileDataOn;
    private boolean bSyncOn;
    private boolean bRadioOn;
    private boolean bHapticOn;
    private boolean bVibrationOn;
    private boolean bRotationOn;

    // used by the ArrayAdapter in ListView
    @Override
    public String toString() {
        return ("id=" + id + " nam=" + modeName + " b=" + brightness
        + " t=" + timeout + " wifi=" + bWiFiOn + " bt=" + bBluetoothOn
        + " md=" + bMobileDataOn + " sync=" + bSyncOn + " Hap=" + bHapticOn
        + " vib=" + bVibrationOn + " rot=" + bRotationOn);
    }

    public long getId() {
        return id;
    }

    public void setId(long i) {
        this.id = i;
    }

    public String getModeName() {
        return modeName;
    }

    public void setModeName(String str) {
        this.modeName = str;
    }

    public boolean getPreset() {
        return bPreset;
    }

    public void setPreset(boolean b) {
        this.bPreset = b;
    }

    public int getPresetType() {
        return presetType;
    }

    public void setPresetType(int type) {
        this.presetType = type;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int val) {
        this.brightness = val;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int val) {
        this.timeout = val;
    }

    public boolean getWiFiOn() {
        return bWiFiOn;
    }

    public void setWiFiOn(boolean b) {
        this.bWiFiOn = b;
    }

    public boolean getBluetoothOn() {
        return bBluetoothOn;
    }

    public void setBluetoothOn(boolean b) {
        this.bBluetoothOn = b;
    }

    public boolean getMobileDataOn() {
        return bMobileDataOn;
    }

    public void setMobileDataOn(boolean b) {
        this.bMobileDataOn = b;
    }

    public boolean getSyncOn() {
        return bSyncOn;
    }

    public void setSyncOn(boolean b) {
        this.bSyncOn = b;
    }

    public boolean getRadioOn() {
        return bRadioOn;
    }

    public void setRadioOn(boolean b) {
        this.bRadioOn = b;
    }

    public boolean getHapticOn() {
        return bHapticOn;
    }

    public void setHapticOn(boolean b) {
        this.bHapticOn = b;
    }

    public boolean getVibrationOn() {
        return bVibrationOn;
    }

    public void setVibrationOn(boolean b) {
        this.bVibrationOn = b;
    }

    public boolean getRotationOn() {
        return bRotationOn;
    }

    public void setRotationOn(boolean b) {
        this.bRotationOn = b;
    }

    public boolean equals(BatteryModeData rhs) {
        return (this.brightness == rhs.getBrightness()
                && this.timeout == rhs.getTimeout()
                && this.bBluetoothOn == rhs.getBluetoothOn()
                && this.bMobileDataOn == rhs.getMobileDataOn()
                && this.bSyncOn == rhs.getSyncOn()
                && this.bHapticOn == rhs.getHapticOn()
                && this.bVibrationOn == rhs.getVibrationOn()
                && this.bRotationOn == rhs.getRotationOn() && this.bWiFiOn == rhs.getWiFiOn());
    }
    
    public BatteryModeData clone() {
        BatteryModeData retData = new BatteryModeData();
        if (retData != null) {
            retData.setId(id);
            retData.setModeName(modeName);
            retData.setPreset(bPreset);
            retData.setPresetType(presetType);
            retData.setBrightness(brightness);
            retData.setTimeout(timeout);
            retData.setBluetoothOn(bBluetoothOn);
            retData.setMobileDataOn(bMobileDataOn);
            retData.setSyncOn(bSyncOn);
            retData.setHapticOn(bHapticOn);
            retData.setVibrationOn(bVibrationOn);
            retData.setRotationOn(bRotationOn);
            retData.setWiFiOn(bWiFiOn);
        }
        
        return retData;
    }
}
