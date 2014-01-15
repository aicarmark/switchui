/*
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: ICalNames.java
 * Description: See class comment.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 02-Mar-2009    e12128       Created file
 **********************************************************
 */
package com.motorola.calendarcommon.vcal.composer;

/**
 * @hide
 */
public interface ICalNames {
    String ACTION_DISPLAY = "DISPLAY";

    String BOOLEAN_FALSE = "FALSE";
    String BOOLEAN_TRUE = "TRUE";

    String CAL_ADDRESS_PREFIX = "MAILTO:";

    String CLASS_PUBLIC = "PUBLIC";
    String CLASS_PRIVATE = "PRIVATE";
    String CLASS_CONFIDENTIAL = "CONFIDENTIAL";

    String DESCRIPTION_REMINDER = "REMINDER";

    String FORMAT_ALARM_TRIGGER = "-PT%dM";

    String CONTENT_TYPE_PARAM_METHOD = "method";
    String CONTENT_TYPE_PARAM_NAME = "name";
    String METHOD_CANCEL = "CANCEL";
    String METHOD_REPLY = "REPLY";
    String METHOD_REQUEST = "REQUEST";

    String OBJ_NAME_CALENDAR = "VCALENDAR";
    String OBJ_NAME_EVENT = "VEVENT";
    String OBJ_NAME_TIME_ZONE = "VTIMEZONE";
    String OBJ_NAME_TZ_DAYLIGHT = "DAYLIGHT";
    String OBJ_NAME_TZ_STANDARD = "STANDARD";
    String OBJ_NAME_VALARM = "VALARM";

    String PARAM_COMMON_NAME = "CN";
    String PARAM_PARTICIPANT_STATUS = "PARTSTAT";
    String PARAM_RELATED = "RELATED";
    String PARAM_ROLE = "ROLE";
    String PARAM_CUTYPE = "CUTYPE";
    String PARAM_RSVP = "RSVP";
    String PARAM_TZ_ID = "TZID";

    String PARAM_VALUE_FALSE = "FALSE";
    String PARAM_VALUE_TRUE = "TRUE";

    String PRIORITY_LOW = "9";
    String PRIORITY_NORMAL = "5";
    String PRIORITY_HIGH = "1";

    String PROP_ACTION = "ACTION";
    String PROP_ATTENDEE = "ATTENDEE";
    String PROP_BEGIN = "BEGIN";
    String PROP_CLASS = "CLASS";
    String PROP_CATEGORIES = "CATEGORIES";
    String PROP_COMMENT = "COMMENT";
    String PROP_DATE_START = "DTSTART";
    String PROP_DATE_STAMP = "DTSTAMP";
    String PROP_DESCRIPTION = "DESCRIPTION";
    String PROP_END = "END";
    String PROP_END_DATETIME = "DTEND";
    String PROP_EXCEPTION_DATE = "EXDATE";
    String PROP_LOCATION = "LOCATION";
    String PROP_METHOD = "METHOD";
    String PROP_ORGANIZER = "ORGANIZER";
    String PROP_PRODUCT_ID = "PRODID";
    String PROP_PRIORITY = "PRIORITY";
    String PROP_RDATE = "RDATE";
    String PROP_RECURRENCE_ID = "RECURRENCE-ID";
    String PROP_RECURRENCE_RULE = "RRULE";
    String PROP_SEQUENCE = "SEQUENCE";
    String PROP_START_DATETIME = "DTSTART";
    String PROP_STATUS = "STATUS";
    String PROP_SUMMARY = "SUMMARY";
    String PROP_TRANSPARENCY = "TRANSP";
    String PROP_TRIGGER = "TRIGGER";
    String PROP_TZ_ID = "TZID";
    String PROP_TZ_OFFSET_FROM = "TZOFFSETFROM";
    String PROP_TZ_OFFSET_TO = "TZOFFSETTO";
    String PROP_UID = "UID";
    String PROP_VERSION = "VERSION";
    String PROP_DURATION = "DURATION";
    String PROP_URL = "URL";

    String PROP_X_MS_APPT_SEQUENCE = "X-MICROSOFT-CDO-APPT-SEQUENCE";
    String PROP_X_MS_OWNERAPPTID = "X-MICROSOFT-CDO-OWNERAPPTID";
    String PROP_X_MS_BUSYSTATUS = "X-MICROSOFT-CDO-BUSYSTATUS";
    String PROP_X_MS_INTENDEDSTATUS = "X-MICROSOFT-CDO-INTENDEDSTATUS";
    String PROP_X_MS_ALLDAYEVENT = "X-MICROSOFT-CDO-ALLDAYEVENT";
    String PROP_X_MS_IMPORTANCE = "X-MICROSOFT-CDO-IMPORTANCE";
    String PROP_X_MS_INSTTYPE = "X-MICROSOFT-CDO-INSTTYPE";

    String PROP_PREFIX_MAILTO = "MAILTO:";

    String REC_BY_DAY = "BYDAY";
    String REC_BY_DAY_OF_MONTH = "BYMONTHDAY";
    String REC_BY_MONTH = "BYMONTH";
    String REC_BY_SET_POS = "BYSETPOS";
    String REC_COUNT = "COUNT";
    String REC_FREQ = "FREQ";
    String REC_INTERVAL = "INTERVAL";
    String REC_UNTIL = "UNTIL";

    String REC_TYPE_DAILY = "DAILY";
    String REC_TYPE_WEEKLY = "WEEKLY";
    String REC_TYPE_MONTHLY = "MONTHLY";
    String REC_TYPE_YEARLY = "YEARLY";

    String REC_WEEK_START= "WKST";

    String REC_WEEKDAY_SUNDAY= "SU";
    String REC_WEEKDAY_MONDAY= "MO";
    String REC_WEEKDAY_TUESDAY= "TU";
    String REC_WEEKDAY_WEDNESDAY= "WE";
    String REC_WEEKDAY_THURSDAY= "TH";
    String REC_WEEKDAY_FRIDAY= "FR";
    String REC_WEEKDAY_SATURDAY= "SA";

    String RELATED_START = "START";

    String STATUS_ACCEPTED = "ACCEPTED";
    String STATUS_CANCELLED = "CANCELLED";
    String STATUS_CONFIRMED = "CONFIRMED";
    String STATUS_DECLINED = "DECLINED";
    String STATUS_TENTATIVE = "TENTATIVE";
    // Change for ice-cream
    String STATUS_NEEDS_ACTION  = "NEEDS ACTION";
    String STATUS_DELEGATED     = "DELEGATED";
    String STATUS_COMPLETED     = "COMPLETED";
    String STATUS_IN_PROCESS    = "IN-PROCESS";

    String TRANSP_OPAQUE = "OPAQUE";
    String TRANSP_TRANSPARENT = "TRANSPARENT";

    String ROLE_CHAIR           = "CHAIR";
    String ROLE_REQUIRED        = "REQ-PARTICIPANT";
    String ROLE_OPTIONAL        = "OPT-PARTICIPANT";
    String ROLE_NON_PARTCIPANT  = "NON-PARTICIPANT";

    String CUTYPE_INDIVIDUAL    = "INDIVIDUAL";
    String CUTYPE_GROUP         = "GROUP";
    String CUTYPE_RESOURCE      = "RESOURCE";
    String CUTYPE_ROOM          = "ROOM";
}
