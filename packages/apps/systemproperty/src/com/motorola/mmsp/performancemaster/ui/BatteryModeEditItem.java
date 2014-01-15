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

import com.motorola.mmsp.performancemaster.R;

public class BatteryModeEditItem {
    public static final int ITEM_TAG_BRIGHTNESS = 0;
    public static final int ITEM_TAG_TIMEOUT = 1;
    public static final int ITEM_TAG_BLUETOOTH = 2;
    public static final int ITEM_TAG_WIFI = 3;
    public static final int ITEM_TAG_MOBILEDATA = 4;
    public static final int ITEM_TAG_SYNC = 5;
    public static final int ITEM_TAG_ROTATION = 6;
    public static final int ITEM_TAG_VIBRATION = 7;
    public static final int ITEM_TAG_HAPTIC = 8;

    public static final int[] iconResEnabled = {
            R.drawable.ic_bm_brightness,
            R.drawable.ic_bm_timeout_enable,
            R.drawable.ic_bm_bluetooth_enable,
            R.drawable.ic_bm_wifi_enable,
            R.drawable.ic_bm_mobiledata_enable,
            R.drawable.ic_bm_sync_enable,
            R.drawable.ic_bm_rotation_enable,
            R.drawable.ic_bm_vibration_enable,
            R.drawable.ic_bm_haptic_enable,
    };

    public static final int[] iconResDisabled = {
            R.drawable.ic_bm_brightness,
            R.drawable.ic_bm_timeout_disable,
            R.drawable.ic_bm_bluetooth_disable,
            R.drawable.ic_bm_wifi_disable,
            R.drawable.ic_bm_mobiledata_disable,
            R.drawable.ic_bm_sync_disable,
            R.drawable.ic_bm_rotation_disable,
            R.drawable.ic_bm_vibration_disable,
            R.drawable.ic_bm_haptic_disable,
    };

    public static final int[] strRes = {
            R.string.bm_parm_brightness,
            R.string.bm_parm_timeout,
            R.string.bm_parm_bluetooth,
            R.string.bm_parm_wifi,
            R.string.bm_parm_mobiledata,
            R.string.bm_parm_sync,
            R.string.bm_parm_rotation,
            R.string.bm_parm_vibration,
            R.string.bm_parm_haptic,
    };

    private int tag;
    private int value;
    private boolean status;

    public int getTag() {
        return tag;
    }

    public void setTag(int t) {
        tag = t;
    }

    public int getInt() {
        return value;
    }

    public void setInt(int i) {
        value = i;
    }

    public boolean getBoolean() {
        return status;
    }

    public void setBoolean(boolean b) {
        status = b;
    }

    @Override
    public String toString() {
        return String.valueOf(tag) + "=" + String.valueOf(value) + "/" + String.valueOf(status);
    }
}
