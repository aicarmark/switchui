/*
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: VCalComposer.java
 * Description: Composes ical
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 08-Dec-2011    a21263       Created file
 **********************************************************
 */

package com.motorola.calendarcommon.vcal.composer;

/**
 * @hide
 */
public interface VCalComposer {
    /**
     * Compose vCal(v1.0) or iCal(v2.0) event based on calendar event
     * @return vCal or iCal utf-8 stream
     */
    public byte[] composeVCal();
}
