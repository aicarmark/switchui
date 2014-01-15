/*
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: AttendeeInfo.java
 * Description: See class comment.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 25-Feb-2010    e12128       Created file
 **********************************************************
 */
package com.motorola.calendar.share.vcalendar.composer;

/**
 * This interface encapsulates the properties that characterize an ActiveSync
 * event "attendee".
 */
public interface AttendeeInfo {

    /** @return the Common Name of the attendee. */
    String getName();

    /** @return the email address of the attendee. */
    String getEmail();

    /** @return true if a response is requested from the attendee. */
    boolean isResponseRequested();

    /** @return the attendee's type; see {@link Constants#CAL_ATTENDEE_TYPE_REQUIRED} etc. */
    int getType();

    /** @return the attendee's status; see {@link Constants#CAL_ATTENDEE_STATUS_ACCEPT} etc. */
    int getStatus();
}
