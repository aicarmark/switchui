/*
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: Recurrence.java
 * Description: See class comment.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 02-Mar-2009    e12128       Created file
 **********************************************************
 */
package com.motorola.calendar.share.vcalendar.composer;


/**
 * This class is a container for the recurrence data defined by ActiveSync
 */
public interface ASRecurrence {
    public static final int TYPE_DAILY          = 0;
    public static final int TYPE_WEEKLY         = 1;
    public static final int TYPE_MONTHLY        = 2;
    public static final int TYPE_MONTHLY_BY_DAY = 3;
    // No value 4 defined
    public static final int TYPE_YEARLY         = 5;
    public static final int TYPE_YEARLY_BY_DAY  = 6;

    public static final int DAY_NUM_SUNDAY      = 0;
    public static final int DAY_NUM_MONDAY      = 1;
    public static final int DAY_NUM_TUESDAY     = 2;
    public static final int DAY_NUM_WEDNESDAY   = 3;
    public static final int DAY_NUM_THURSDAY    = 4;
    public static final int DAY_NUM_FRIDAY      = 5;
    public static final int DAY_NUM_SATURDAY    = 6;

    public static final int DAY_MASK_SUNDAY      = 1 << DAY_NUM_SUNDAY;
    public static final int DAY_MASK_MONDAY      = 1 << DAY_NUM_MONDAY;
    public static final int DAY_MASK_TUESDAY     = 1 << DAY_NUM_TUESDAY;
    public static final int DAY_MASK_WEDNESDAY   = 1 << DAY_NUM_WEDNESDAY;
    public static final int DAY_MASK_THURSDAY    = 1 << DAY_NUM_THURSDAY;
    public static final int DAY_MASK_FRIDAY      = 1 << DAY_NUM_FRIDAY;
    public static final int DAY_MASK_SATURDAY    = 1 << DAY_NUM_SATURDAY;

    public static final int DAY_MASK_WEEKDAYS    = DAY_MASK_MONDAY |  DAY_MASK_TUESDAY |  DAY_MASK_WEDNESDAY |  DAY_MASK_THURSDAY |  DAY_MASK_FRIDAY;
    public static final int DAY_MASK_WEEKENDDAYS = DAY_MASK_SUNDAY |  DAY_MASK_SATURDAY;
    public static final int DAY_MASK_ALLDAYS     = DAY_MASK_WEEKDAYS | DAY_MASK_WEEKENDDAYS;

    /**
     * @return The type of the recurrence. See the TYPE_* constants.
     */
    public int getType();

    /**
     * @return The interval of the recurrence. 1 means "every", 2 means "every other" etc.
     */
    public int getInterval();

    /**
     * @return The day number(s) in the week, See the DAY_MASK_* masks.
     */
    public int getDayOfWeek();

    /**
     * @return The day number in the month.
     */
    public int getDayOfMonth();

    /**
     * @return The week number within in the (for "first Sunday" type patterns). 1-5, where 5 means "last".
     */
    public int getWeekOfMonth();

    /**
     * @return The month in the year, 1-12.
     */
    public int getMonthOfyear();

    /**
     * @return The date-time after which no recurrences will happen.
     */
    public String getUntil();

    /**
     * @return The total number of recurrences.
     */
    public int getOccurrences();

}


