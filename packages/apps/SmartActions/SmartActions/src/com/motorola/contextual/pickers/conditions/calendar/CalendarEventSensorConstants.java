/*
 * @(#)CalendarEventSensorConstants.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * wkh346        2011/08/22 NA                Initial version
 *
 */

package com.motorola.contextual.pickers.conditions.calendar;

import com.motorola.contextual.smartprofile.Constants;

/**
 * This interface provides the constants used throughout the calendar events
 * precondition
 *
 * <CODE><PRE>
 *
 * INTERFACE:
 *      Extends Constants
 *
 * RESPONSIBILITIES:
 * This interface is responsible for providing the constants used throughout the
 * calendar events precondition
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * </PRE></CODE>
 */
public interface CalendarEventSensorConstants extends Constants {

    public static final String SPACE = " ";
    public static final String EXTRA_CALENDARS = "CALENDARS";
    public static final String EXTRA_ACCEPTED_EVENTS = "ACCEPTED_EVENTS";
    public static final String EXTRA_ALLDAY_EVENTS = "ALLDAY_EVENTS";
    public static final String EXTRA_SELECT_ALL_EVENTS = "SELECT_ALL_EVENTS";
    public static final String EXTRA_MULTIPLE_PARTICIPANTS = "MULTIPLE_PARTICIPANTS";
    public static final String EXTRA_EVENT_IDS = "EVENT_IDS";
    public static final String ACTION_SCHEDULE_EVENT_AWARE = "com.motorola.smartactions.calendarevents.action.SCHEDULE_EVENT_AWARE";
    public static final String ACTION_EVENTS_TABLE_CHANGED = "com.motorola.smartactions.calendarevents.action.EVENTS_TABLE_CHANGED";
    public static final String EQUALS_TO = "=";
    public static final String NOT_EQUAL_TO = "!=";
    public static final String GREATER_THAN = ">";
    public static final String INVERTED_COMMA = "'";
    public static final String FORWARD_SLASH = "/";
    public static final String COMMA = ",";
    public static final String SEMI_COLON = ";";
    public static final String AND = " AND ";
    public static final String OR = OR_STRING;
    public static final String VISIBLE_CALENDARS_ONLY = "1";
    public static final String NOT_ALL_DAY_EVENT = "0";
    public static final String CALENDARS_NONE = "-1";
    public static final String EXTRA_NOTIFY_SA_CORE = "NOTIFY_SA_CORE";
    public static final String CALENDAR_EVENTS_EXPORT_ACTION = "com.motorola.contextual.smartprofile.sensors.calendareventsensor.EXPORT";
    public static final String NULL_STRING = "null";
    public static final String CALENDAR_EVENTS_XML_CONTENT = "com.motorola.contextual.calendarevents";
    public static final String CALENDAR_EVENTS_SHARED_PREFERENCE = "com.motorola.contextual.calendareventssharedpreference";
    public static final String CALENDAR_EVENTS_IMPORT_ACTION = "com.motorola.contextual.smartprofile.sensors.calendareventsensor.IMPORT";
    public static final String EXTRA_CALENDAR_EVENTS_DATA = "com.motorola.contextual.smartprofile.sensors.calendareventsensor.intent.extra.CALENDAR_EVENTS_DATA";
    // Condition publisher related
    public static final String CONFIG_DELIMITER = ";";
    public static final String ACTION_STORE_CONFIG = "com.motorola.smartactions.calendarevents.action.STORE_CONFIG";
    public static final String ACTION_REMOVE_CONFIG = "com.motorola.smartactions.calendarevents.action.REMOVE_CONFIG";
    public static final String ACTION_REFRESH_CONFIG = "com.motorola.smartactions.calendarevents.action.REFRESH_CONFIG";
    public static final String CALENDAR_EVENTS_PUBLISHER_KEY = "com.motorola.contextual.smartprofile.calendareventsensor";
    public static final String EVENTS_FROM_CALENDARS = "events_from_calendars";
    public static final String EXCLUDE_ALL_DAY_EVENTS = "exclude_all_day_events";
    public static final String INCLUDE_ACCEPTED_EVENTS_ONLY = "include_accepted_events_only";
    public static final String INCLUDE_EVENTS_WITH_MULTIPLE_PEOPLE_ONLY = "include_events_with_multiple_people_only";
    public static final String NOTIFY_STRICTLY_ON_TIME = "notify_strictly_on_time";
    public static final double INVALID_VERSION = 0.0;
    public static final String STATE_MONITOR = "com.motorola.contextual.pickers.conditions.calendar.CalendarEventStateMonitor";
    public static final String KEY_CALENDAR_EVENTS_CONFIGS = "calendar_events_configs";
}
