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

public class BatteryModeOptimizeItem {
    public static final int ITEM_TAG_BRIGHTNESS = 0;
    public static final int ITEM_TAG_TIMEOUT = 1;
    public static final int ITEM_TAG_BLUETOOTH = 2;
    public static final int ITEM_TAG_WIFI = 3;
    public static final int ITEM_TAG_MOBILEDATA = 4;
    public static final int ITEM_TAG_SYNC = 5;
    public static final int ITEM_TAG_ROTATION = 6;
    public static final int ITEM_TAG_VIBRATION = 7;
    public static final int ITEM_TAG_HAPTIC = 8;
    
    private int iconId;
    private int textId;
    private String value;
    private boolean highlight;
    private int tag;
    
    public int getTag() {
        return tag;
    }
    
    public void setTag(int t) {
        tag = t;
    }
    
    public int getIconId() {
        return iconId;
    }

    public void setIconId(int i) {
        iconId = i;
    }

    public int getTextId() {
        return textId;
    }

    public void setTextId(int i) {
        textId = i;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String s) {
        value = s;
    }
    
    public boolean getHighlight() {
        return highlight;
    }
    
    public void setHighlight(boolean b) {
        highlight = b;
    }

    @Override
    public String toString() {
        return value;
    }
}
