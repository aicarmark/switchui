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

public class BatteryModeListItem {
    public static final int BATTERY_MODE_GENERAL = 0;
    public static final int BATTERY_MODE_SAVER = 1;
    public static final int BATTERY_MODE_NIGHT = 2;
    public static final int BATTERY_MODE_PERFORMANCE = 3;
    public static final int BATTERY_MODE_CUSTOMIZE = 4;
    public static final int BATTERY_MODE_NONE = 5;

    private long id;
    private int modeType;
    private String text;

    @Override
    public String toString() {
        return text;
    }

    public long getId() {
        return id;
    }

    public void setId(long i) {
        this.id = i;
    }

    public int getModeType() {
        return modeType;
    }

    public void setModeType(int t) {
        this.modeType = t;
    }

    public String getText() {
        return text;
    }

    public void setText(String s) {
        this.text = s;
    }
}
