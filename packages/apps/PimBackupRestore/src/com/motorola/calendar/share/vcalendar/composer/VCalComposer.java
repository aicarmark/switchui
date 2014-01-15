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

package com.motorola.calendar.share.vcalendar.composer;

public interface VCalComposer {
    /**
     * Compose vCal(v1.0) or iCal(v2.0) event based on calendar event
     * @return vCal or iCal string stream
     */
    public String composeVCal();
}
