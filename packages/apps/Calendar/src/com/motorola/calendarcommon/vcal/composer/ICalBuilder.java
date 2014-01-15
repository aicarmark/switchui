/*
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: ICalBuilder.java
 * Description: See class comment.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 02-Mar-2009    e12128       Created file
 **********************************************************
 */
package com.motorola.calendarcommon.vcal.composer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import android.util.Log;
import android.text.TextUtils;

/**
 * This class encapsulates a lot of the rules and logic for streaming an RFC2445 (iCal) VCALENDAR
 * card to a given output stream.
 * @hide
 */
public class ICalBuilder {
    private static final String TAG ="ICalBuilder";

    private static final int MINUTES_IN_A_DAY  = 60 * 24;
    private static final int MINUTES_IN_A_HOUR = 60;

    /** How dates are formatted in calendar responses. */
    public static final String DATE_FORMAT_CALENDAR     = "yyyyMMdd'T'HHmmss'Z'";

    /**
     * This inner class holds constants for ActiveSync Recurrence week day masks
     */
    interface AsRecurrenceSpecials {
        int LAST_DAY_OF_MONTH = 127;
        int LAST_WEEK_OF_MONTH = 5;
    }

    /**
     * This inner class holds constants for ActiveSync Recurrence week day masks
     */
    interface AndroidRecurrence {
        int LAST_DAY_OF_MONTH = -1;
        int LAST_WEEK_OF_MONTH = -1;
    }

    /**
     * Constructs a new instance on a given stream.
     * @param stream The OutputStream on which to stream the output.
     */
    public ICalBuilder(OutputStream stream) {
        mStream = stream;
    }

    /**
     * Appends the given AttendeeBean as an ATTENDEE property of type CalAddress.
     * @param att The AttendeeBean to append.
     * @param requestResponse true if a response is request, regardless of the attendee's setting.
     */
    public void appendAttendee(AttendeeInfo att, boolean requestResponse) {
        if (att.getType() == Constants.CAL_ATTENDEE_TYPE_RESOURCE) {
            appendCalAddressProperty(ICalNames.PROP_ATTENDEE,
                    att.getEmail(),
                    att.getName(),
                    att.isResponseRequested() || requestResponse,
                    mapTypeToRole(att.getType()),
                    mapStatus(att.getStatus()),
                    ICalNames.CUTYPE_RESOURCE); // Put cuType if attendee is type resource.
        } else {
            appendCalAddressProperty(ICalNames.PROP_ATTENDEE,
                    att.getEmail(),
                    att.getName(),
                    att.isResponseRequested() || requestResponse,
                    mapTypeToRole(att.getType()),
                    mapStatus(att.getStatus())); // No cuType for other attendees.
        }
    }

    /**
     * Appends a CalAddress-type property with the given name, value and parameters.
     * @param propName The name of the property to output.
     * @param email The email address to use as the value of the property.
     * @param commonName The common name (CN) parameter, if any.
     * @param needsResponse The RSVP param, if any.
     * @param role The ROLE param, if any.
     * @param status The PARTSTAT param, if any.
     */
    public void appendCalAddressProperty(String propName, String email, String commonName, boolean needsResponse, String role, String status) {
        appendCalAddressProperty(propName, email,
                ICalNames.PARAM_COMMON_NAME, commonName,
                ICalNames.PARAM_RSVP, needsResponse ? ICalNames.PARAM_VALUE_TRUE : ICalNames.PARAM_VALUE_FALSE,
                ICalNames.PARAM_PARTICIPANT_STATUS, status,
                ICalNames.PARAM_ROLE, role
        );
    }

    /**
     * Appends a CalAddress-type property with the given name, value and parameters.
     * @param propName The name of the property to output.
     * @param email The email address to use as the value of the property.
     * @param commonName The common name (CN) parameter, if any.
     * @param needsResponse The RSVP param, if any.
     * @param role The ROLE param, if any.
     * @param status The PARTSTAT param, if any.
     * @param cuType The CUTYPE param, if any.
     */
    public void appendCalAddressProperty(String propName, String email, String commonName, boolean needsResponse, String role, String status, String cuType) {
        appendCalAddressProperty(propName, email,
                ICalNames.PARAM_COMMON_NAME, commonName,
                ICalNames.PARAM_RSVP, needsResponse ? ICalNames.PARAM_VALUE_TRUE : ICalNames.PARAM_VALUE_FALSE,
                ICalNames.PARAM_PARTICIPANT_STATUS, status,
                ICalNames.PARAM_ROLE, role,
                ICalNames.PARAM_CUTYPE, cuType
        );
    }

    /**
     * Appends a CalAddress-type property with the given name, value and common name parameter.
     * @param propName The name of the property to output.
     * @param email The email address to use as the value of the property.
     * @param commonName The common name (CN) parameter, if any.
     */
    public void appendCalAddressProperty(String propName, String email, String commonName) {
        appendCalAddressProperty(propName, email, ICalNames.PARAM_COMMON_NAME, commonName);
    }

    /**
     * Appends a CalAddress-type property with the given name, value and optional parameters.
     * @param propName The name of the property to output.
     * @param email The email address to use as the value of the property.
     * @param params The parameters for the property, alternating as name/value pairs..
     */
    public void appendCalAddressProperty(String propName,  String email, String... params) {
        appendProperty(propName, ICalNames.CAL_ADDRESS_PREFIX + email, params);
    }

    /**
     * Appends a general property with the given name, value and optional parameters.
     * @param propName The name of the property to output.
     * @param value The value to use as the value of the property.
     * @param params The parameters for the property, alternating as name/value pairs..
     */
    public void appendProperty(String propName, CharSequence value, String... params) {
        appendProperty(propName, value, true, params);
    }

    /**
     * Take an ActiveSync Email or Calendar time string, and appends it as a VCAL format one.
     * @param sb The StringBuilder to append to.
     * @param val A timestamp of the form YYYYMMDDTHHMMSS[Z] or YYYY-MM-DDYHH:MM:SS.MMM[Z]
     * @return A timestamp of the form YYYYMMDDTHHMMSS[Z]
     */
    public static void appendNormalizedDateTime(StringBuilder sb, String val) {
        int len = val.length();
        if (len == DATE_FORMAT_CALENDAR.length()) {
            // Already in the correct form.
            sb.append(val);
        } else {
            // Append the chars we want and discard the rest.
            FOR:
            for (int i = 0; i < len; i++) {
                 char c = val.charAt(i);
                 switch (c) {
                     case ':':
                     case '-':
                         continue;
                     case '.':
                         if (val.endsWith("Z")) {
                             sb.append('Z');
                         }
                         break FOR;
                     default:
                         sb.append(c);
                 }
             }
        }
    }

    /**
     * Appends a date-time property.
     * @param propName The name of the property to output.
     * @param timeZoneId The time tone ID to use a a parameter, or null.
     * @param value The value of the date-time in the form YYYYMMDDYHHMMSSZ or YYYY-MM-DDYHH:MM:SS.MMMZ
     */
    public void appendDateTime(String propName, String timeZoneId, String value) {
        StringBuilder sb = new StringBuilder(DATE_FORMAT_CALENDAR.length());
        appendNormalizedDateTime(sb, value);
        appendProperty(propName, sb, timeZoneId == null ? null : new String[] { ICalNames.PARAM_TZ_ID, timeZoneId});
    }

    /**
     * Appends a property whose value is a list, each element separated by commas.
     * @param propName The name of the property to output.
     * @param values the list of values to use for the property value.
     */
    public void appendListProperty(String propName, List<? extends CharSequence> values) {
        if (values != null && !values.isEmpty()) {
            StringBuilder sb = new StringBuilder(100);
            for (CharSequence obj : values) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                escapeText(sb, obj);
            }
            appendProperty(propName, sb, false);
        }
    }

    /**
     * Append a CLASS property from the given ActiveSync sensitivity value.
     * @param sensitivity The ActiveSync sensitivity value. See {@link Constants}.CAL_SENSITIVITY_*.
     */
    public void appendClass(int sensitivity) {
        if (sensitivity != -1) {
            String calVal;
            if (sensitivity == Constants.CAL_SENSITIVITY_NORMAL) {
                calVal = ICalNames.CLASS_PUBLIC;
            } else if (sensitivity == Constants.CAL_SENSITIVITY_CONFIDENTIAL) {
                calVal = ICalNames.CLASS_CONFIDENTIAL;
            } else {
                // PERSONAL or PRIVATE
                calVal = ICalNames.CLASS_PRIVATE;
            }
            appendProperty(ICalNames.PROP_CLASS, calVal, false);
        }
    }

    /**
     * Append a TRANSP property from the given ActiveSync busy status value.
     * @param calBusyStatus The ActiveSync calendar busy status value. See {@link Constants}.CAL_BUSY_STATUS_*.
     */
    public void appendTransparency(int calBusyStatus) {
        String transp = Constants.CAL_BUSY_STATUS_FREE == calBusyStatus
            ? ICalNames.TRANSP_TRANSPARENT
            : ICalNames.TRANSP_OPAQUE;
        appendProperty(ICalNames.PROP_TRANSPARENCY, transp, false);
    }

    /**
     * Append a PRIORITY property from the given ActiveSync importance value.
     * @param importance The ActiveSync busy status value. See {@link Constants}.EM_IMPORTANCE_*.
     */
    public void appendPriorityProperty(int importance) {
        String val;
        switch (importance) {
        case Constants.EM_IMPORTANCE_NORMAL:
            val = ICalNames.PRIORITY_NORMAL;
            break;
        case Constants.EM_IMPORTANCE_HIGH:
            val = ICalNames.PRIORITY_HIGH;
            break;
        default:
            val = ICalNames.PRIORITY_LOW;
        }
        appendProperty(ICalNames.PROP_PRIORITY, val, false);
    }

    /**
     * Append a STATUS property from the given METHOD value..
     * @param method The METHOD to use. The status is mapped as follows:
     * <pre>{@literal
     *CANCEL -> CANCELLED
     *REPLY -> CONFIRMED
     *REQUEST -> CONFIRMED
     * }</pre>
     */
    public void appendStatusProperty(String method) {
        String status;
        if (ICalNames.METHOD_CANCEL.equalsIgnoreCase(method)) {
            status = ICalNames.STATUS_CANCELLED;
        } else {
            status = ICalNames.STATUS_CONFIRMED;
        }
        appendProperty(ICalNames.PROP_STATUS, status, false);
    }

    /**
     * Appends a complete VALARM object based on the given time period in minute.
     * @param minutes The time in minutes before the start time at which to set the alarm.
     */
    public void appendAlarm(int minutes) {
        if (minutes != -1) {
            appendProperty(ICalNames.PROP_BEGIN, ICalNames.OBJ_NAME_VALARM, false);
            appendProperty(ICalNames.PROP_ACTION, ICalNames.ACTION_DISPLAY, false);
            appendProperty(ICalNames.PROP_DESCRIPTION, ICalNames.DESCRIPTION_REMINDER, false);
            String val = String.format(ICalNames.FORMAT_ALARM_TRIGGER, minutes);
            appendProperty(ICalNames.PROP_TRIGGER, val, false, ICalNames.PARAM_RELATED, ICalNames.RELATED_START);
            appendProperty(ICalNames.PROP_END, ICalNames.OBJ_NAME_VALARM, false);
        }
    }

    // cavin - added for yahoo
    /**
     * Appends the given AttendeeInfo as an ATTENDEE property of type CalAddress
     * with additional parameter.
     * @param att The AttendeeBean to append.
     * @param requestResponse true if a response is request, regardless of the attendee's setting.
     * @param extraParam The extra parameter to be added to the ATTENDEE property.
     * It's normally a proprietary parameter like "X-YAHOO-MAILING-LIST".
     * @param extraValue The value of <code>extraParameter</code>.
     */
    public void appendAttendee(AttendeeInfo att, boolean requestResponse, String extraParam, String extraValue) {
        if (att.getType() == Constants.CAL_ATTENDEE_TYPE_RESOURCE) {
            appendCalAddressProperty(ICalNames.PROP_ATTENDEE, att.getEmail(),
                    ICalNames.PARAM_COMMON_NAME, att.getName(),
                    ICalNames.PARAM_RSVP, att.isResponseRequested() || requestResponse ? ICalNames.PARAM_VALUE_TRUE : ICalNames.PARAM_VALUE_FALSE,
                    ICalNames.PARAM_PARTICIPANT_STATUS, mapStatus(att.getStatus()),
                    ICalNames.PARAM_ROLE, mapTypeToRole(att.getType()),
                    ICalNames.PARAM_CUTYPE, ICalNames.CUTYPE_RESOURCE,
                    extraParam, extraValue
            );
        } else {
            appendCalAddressProperty(ICalNames.PROP_ATTENDEE, att.getEmail(),
                    ICalNames.PARAM_COMMON_NAME, att.getName(),
                    ICalNames.PARAM_RSVP, att.isResponseRequested() || requestResponse ? ICalNames.PARAM_VALUE_TRUE : ICalNames.PARAM_VALUE_FALSE,
                    ICalNames.PARAM_PARTICIPANT_STATUS, mapStatus(att.getStatus()),
                    ICalNames.PARAM_ROLE, mapTypeToRole(att.getType()),
                    extraParam, extraValue
            );
        }
    }

    // cavin - added for yahoo
    /**
     * Append a complet VALARM object for a given time period in minute and action.
     * @param minutes The time in minutes before the start time at which to set the alarm.
     * @param action Action of the alarm. It can be standard ones like DISPLAY and EMAIL
     * or proprietary one line X-YAHOO-CALENDAR-ACTION-IM.
     * @param email Email address of the action if not null.
     */
    public void appendAlarm(int minutes, String action, String email) {
        if (minutes != -1) {
            appendProperty(ICalNames.PROP_BEGIN, ICalNames.OBJ_NAME_VALARM, false);
            appendProperty(ICalNames.PROP_ACTION, action, false);
            appendProperty(ICalNames.PROP_DESCRIPTION, ICalNames.DESCRIPTION_REMINDER, false);
            String val = minsToDurationString(minutes);
            appendProperty(ICalNames.PROP_TRIGGER, val, false, ICalNames.PARAM_RELATED, ICalNames.RELATED_START);
            if (email != null) {
                // If email is not null then add attendee property.
                appendProperty(ICalNames.PROP_ATTENDEE, email, true);
            }
            appendProperty(ICalNames.PROP_END, ICalNames.OBJ_NAME_VALARM, false);
        }
    }

    // cavin - added for yahoo
    /**
     * Convert a duration mins to an iCal duration string such as "-PT15M",
     * "-P2D", "+PT5M", "+P2D", etc.
     * @param mins The duration minutes. A positive number means "prior to" event
     * start time and negative means "after" the event has occurred.
     * @return The converted iCal duration string.
     */
    private String minsToDurationString(int mins) {
        StringBuilder ret = new StringBuilder();
        // If mins > 0 prefix a '-' (ie, "prior to meeting").
        if (mins > 0) {
            ret.append("-P");
        } else {
            ret.append("+P");
            mins = - mins; // make it positive
        }

        // Use H if mins is more than 1 hr and use D if it's more than a day.
        if (mins >= MINUTES_IN_A_DAY) {
            ret.append(String.valueOf(mins/MINUTES_IN_A_DAY)).append("D");
        } else {
            // Need a 'T' for hr, min and sec.
            ret.append('T');
            if (mins >= MINUTES_IN_A_HOUR) {
                ret.append(String.valueOf(mins/MINUTES_IN_A_HOUR)).append("H");
            } else {
                ret.append(String.valueOf(mins)).append("M");
            }
        }
        return ret.toString();
    }

    /**
     * Appends a complete VTIMEZONE object based on the given timezone argument.
     * @param timeZone The ASTimeZone object used to derive the time zone data.
     * @return The time zone ID. or null if the argument was null. The ID is derived from the
     * StandardName property of the time zone, if it has one, or other from an incrementing sequence count.
     */
    public String appendTimeZone(ASTimeZone timeZone) {
        if (timeZone == null) {
            return null;
        }

        String timeZoneId = timeZone.getStandardName();
        if (TextUtils.isEmpty(timeZoneId)) {
            timeZoneId = "tz" + (++mTimeZoneIndex);
        }

        appendProperty(ICalNames.PROP_BEGIN, ICalNames.OBJ_NAME_TIME_ZONE, false);

        appendProperty(ICalNames.PROP_TZ_ID, timeZoneId);

        appendProperty(ICalNames.PROP_BEGIN, ICalNames.OBJ_NAME_TZ_STANDARD, false);
        ASTimeZone.SystemTime standardStart = timeZone.getStandardDate();
        appendTZDateTime(ICalNames.PROP_DATE_START, standardStart);
        appendOffset(ICalNames.PROP_TZ_OFFSET_FROM, -(timeZone.getBias() + timeZone.getDaylightBias()));
        appendOffset(ICalNames.PROP_TZ_OFFSET_TO, -(timeZone.getBias() + timeZone.getStandardBias()));
        appendRecRuleForTZ(standardStart);
        appendProperty(ICalNames.PROP_END, ICalNames.OBJ_NAME_TZ_STANDARD, false);

        appendProperty(ICalNames.PROP_BEGIN, ICalNames.OBJ_NAME_TZ_DAYLIGHT, false);
        ASTimeZone.SystemTime daylightStart = timeZone.getDaylightDate();
        appendTZDateTime(ICalNames.PROP_DATE_START, daylightStart);
        appendOffset(ICalNames.PROP_TZ_OFFSET_FROM, -(timeZone.getBias() + timeZone.getStandardBias()));
        appendOffset(ICalNames.PROP_TZ_OFFSET_TO, -(timeZone.getBias() + timeZone.getDaylightBias()));
        appendRecRuleForTZ(daylightStart);
        appendProperty(ICalNames.PROP_END, ICalNames.OBJ_NAME_TZ_DAYLIGHT, false);

        appendProperty(ICalNames.PROP_END, ICalNames.OBJ_NAME_TIME_ZONE, false);

        return timeZoneId;
    }

    /**
     * Maps an ActiveSync attendee status to the iCal equivalent.
     * @param easStatus The EAS code: {@link Constants#CAL_ATTENDEE_STATUS_ACCEPT} etc.
     * @return The iCal string: {@link ICalNames#STATUS_ACCEPTED} etc.
     */
    private String mapStatus(int easStatus) {
        switch (easStatus) {
        case Constants.CAL_ATTENDEE_STATUS_TENTATIVE: return ICalNames.STATUS_TENTATIVE;
        case Constants.CAL_ATTENDEE_STATUS_ACCEPT: return ICalNames.STATUS_ACCEPTED;
        case Constants.CAL_ATTENDEE_STATUS_DECLINE: return ICalNames.STATUS_DECLINED;
        }
        return ICalNames.STATUS_NEEDS_ACTION;
    }

    /**
     * Maps an ActiveSync attendee type to the iCal equivalent role.
     * @param easType The EAS code: {@link Constants#CAL_ATTENDEE_TYPE_REQUIRED} etc.
     * @return The iCal string: {@link ICalNames#ROLE_REQUIRED} etc.
     */
    private String mapTypeToRole(int easType) {
        switch (easType) {
        case Constants.CAL_ATTENDEE_TYPE_OPTIONAL: return ICalNames.ROLE_OPTIONAL;
        case Constants.CAL_ATTENDEE_TYPE_REQUIRED: return ICalNames.ROLE_REQUIRED;
        case Constants.CAL_ATTENDEE_TYPE_RESOURCE: return ICalNames.ROLE_NON_PARTCIPANT;
        }
        return ICalNames.ROLE_REQUIRED;
    }

    /**
     * Appends the given SystemTime to the output as a property of the form 16010101THHMMSS.
     * @param propName The name of the property to append.
     * @param time The local time of a daylight/standard transition.
     */
    private void appendTZDateTime(String propName, ASTimeZone.SystemTime time) {
        StringBuilder sb  = new StringBuilder(40);
        sb.append(propName);
        sb.append(PROPERTY_VALUE_SEPARATOR);
        appendTZDateTime(sb, time);
        printFolded(sb.toString());
    }

    /**
     * Appends an offset property of the given name as an offset of the form [-]HHMM.
     * @param propName The name of the property to append.
     * @param offset A signed offset in minutes.
     * @return A signed hour and minute offset.
     */
    private void appendOffset(String propName, int offset) {
        StringBuilder sb  = new StringBuilder(40);
        sb.append(propName);
        sb.append(PROPERTY_VALUE_SEPARATOR);
        appendOffset(sb, offset);
        printFolded(sb.toString());
    }

    /**
     * Appends a boolean-values property of the given name, if the value is non-null.
     * @param propName The name of the property to add.
     * @param value The Boolean value, or null to skip the property.
     */
    public void appendBoolean(String propName, Boolean value) {
        if (value != null) {
            appendProperty(propName, value.booleanValue() ? ICalNames.BOOLEAN_TRUE : ICalNames.BOOLEAN_FALSE, false);
        }
    }

    // Maps calendar busy statuses to MS names.
    private static final String[] X_MS_BUSYSTATUS = { "FREE", "TENTATIVE", "BUSY", "OOF"};

    /**
     * Appends the busy status of the meeting in ICS, property is X-MICROSOFT-CDO-BUSYSTATUS or
     * X-MICROSOFT-CDO-INTENDEDSTATUS.
     *
     * X-MICROSOFT-CDO-BUSYSTATUS reflects the meeting status if new meeting request is
     * automatically placed in Calendar on OWA.
     * X-MICROSOFT-CDO-INTENDEDSTATUS reflects the busy status that the meeting organizer
     * intends the attendee's copy of the meeting to have. E.g, organizer sends a meeting
     * request, which (organizer's meeting) shows time as OOF, OOF is put to
     * X-MICROSOFT-CDO-INTENDEDSTATUS. After attendee accept this meeting, attendee's
     * calendar shows time as OOF.
     *
     * @param calBusyStatus The calendar busy status to be appended.
     * @param isIntendedStatus true to append X-MICROSOFT-CDO-INTENDEDSTATUS,
     *            false to append X-MICROSOFT-CDO-BUSYSTATUS.
     */
    public void appendMSBusyStatus(int calBusyStatus, boolean isIntendedStatus) {
        if (calBusyStatus != -1) {
            String propName = isIntendedStatus ? ICalNames.PROP_X_MS_INTENDEDSTATUS
                    : ICalNames.PROP_X_MS_BUSYSTATUS;
            appendProperty(propName, X_MS_BUSYSTATUS[calBusyStatus], false);
        }
    }

    /**
     * Appends a property with the given name of the recurrence rule type, based on the given ASRecurrence.
     * @param propName The name of the property to append.
     * @param rec The ASRecurrence rule to derive the property value from.
     */
    public void appendRecurrence(String propName, ASRecurrence rec) {
        StringBuilder sb = new StringBuilder(40);
        appendRecurrenceRule(sb, rec);
        appendProperty(propName, sb, false);
    }

    /**
     * Appends the exceptions dates from the given list, if any.
     * @param exceptions The (possibly empty) list of exception dates.
     */
    public void appendExceptions(List<? extends ExceptionInfo> exceptions) {
        if (!exceptions.isEmpty()) {
            StringBuilder sb = new StringBuilder(40);
            for (ExceptionInfo except : exceptions) {
                if (except.isDeleted()) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(except.getExceptionStartTime());
                }
            }
            appendProperty(ICalNames.PROP_EXCEPTION_DATE, sb, false);
        }
    }

    /**
     * Appends the RRULE value equivalent to the given ASRecurrence to the given StringBuilder.
     * @param sb The StringBuilder to append to.
     * @param rec The ASRecurrence rule to derive the property value from.
     */
    public static void appendRecurrenceRule(StringBuilder sb, ASRecurrence rec) {
        int val;
        int type = rec.getType();
        String typeStr = TYPES[type];
        // DAILY, WEEKLY etc.
        sb.append(ICalNames.REC_FREQ).append('=').append(typeStr);

        // Either a count of occurrences, or the until date, but not both.
        int occurrences = rec.getOccurrences();
        String until = rec.getUntil();
        if (occurrences != -1 && until != null) {
            throw new IllegalArgumentException("Only one of OCCURRENCES or UNTIL must be set.");
        }

        if (occurrences != -1) {
            sb.append(';').append(ICalNames.REC_COUNT).append('=').append(occurrences);
        }
        if (until != null) {
            // Workaround an Android bug that doesn't like years outside of
            // the range 1970-2036
            int year = Integer.parseInt(until.substring(0, 4));
            if (year > 2036) {
                until = "2036" + until.substring(4);
            } else if (year < 1970) {
                until = "1970" + until.substring(4);
            }
            sb.append(';').append(ICalNames.REC_UNTIL).append('=').append(until);
        }

        // The interval count;  Default is 1 so don't include if 1
        val = rec.getInterval();
        if (val > 1) {
            sb.append(';').append(ICalNames.REC_INTERVAL).append('=').append(val);
        }

        // This is a bit mask of the days of the week, bit 0 = Sunday
        int dayOfWeek = rec.getDayOfWeek();
        int weekOfMonth = rec.getWeekOfMonth();
        // Convert to RRULE representation of "last week of month"
        if (weekOfMonth == AsRecurrenceSpecials.LAST_WEEK_OF_MONTH) {
            weekOfMonth = AndroidRecurrence.LAST_WEEK_OF_MONTH;
        }
        int monthOfYear = rec.getMonthOfyear();
        int dayOfMonth = rec.getDayOfMonth();
        switch (type) {
        case ASRecurrence.TYPE_DAILY:
        case ASRecurrence.TYPE_WEEKLY:
            if (dayOfWeek != -1) {
                sb.append(';').append(ICalNames.REC_BY_DAY).append('=');appendByWeekDayList(sb, dayOfWeek);
            }
            break;
        case ASRecurrence.TYPE_MONTHLY:
            if (dayOfMonth != -1) {
                sb.append(';').append(ICalNames.REC_BY_DAY_OF_MONTH).append('=').append(dayOfMonth);
            }
            break;
        case ASRecurrence.TYPE_MONTHLY_BY_DAY:
            if (dayOfWeek != -1) {
                sb.append(';').append(ICalNames.REC_BY_DAY).append('=');appendByWeekDayList(sb, dayOfWeek);
            }
            if (weekOfMonth != -1) {
                sb.append(';').append(ICalNames.REC_BY_SET_POS).append('=').append(weekOfMonth);
            }
            break;
        case ASRecurrence.TYPE_YEARLY:
            if (monthOfYear != -1) {
                sb.append(';').append(ICalNames.REC_BY_MONTH).append('=').append(monthOfYear);
            }
            if (dayOfMonth != -1) {
                sb.append(';').append(ICalNames.REC_BY_DAY_OF_MONTH).append('=').append(dayOfMonth);
            }
            break;
        case ASRecurrence.TYPE_YEARLY_BY_DAY:
            if (dayOfWeek != -1) {
                sb.append(';').append(ICalNames.REC_BY_DAY).append('=');appendByWeekDayList(sb, dayOfWeek);
            }
            if (weekOfMonth != -1) {
                sb.append(';').append(ICalNames.REC_BY_SET_POS).append('=').append(weekOfMonth);
            }
            if (monthOfYear != -1) {
                sb.append(';').append(ICalNames.REC_BY_MONTH).append('=').append(monthOfYear);
            }
            break;
        }
        sb.append(';').append(ICalNames.REC_WEEK_START).append('=').append(ICalNames.REC_WEEKDAY_SUNDAY);
    }

    /**
     * Appends a property based on the given values.
     * @param propName The name of the property.
     * @param value The value. If null, nothing is appended.
     * @param escapeValue true if the value should be escaped, false to use it as is.
     * @param params The parameters, alternating name/value pairs.
     */
    private void appendProperty(String propName, CharSequence value, boolean escapeValue, String... params) {
        if (value != null) {
            StringBuilder sb  = new StringBuilder(40);
            sb.append(propName);
            if (params != null) {
                int paramLen = params.length;
                if (paramLen % 2 != 0) {
                    throw new IllegalArgumentException("Number of params arguments must be even");
                }
                for (int i = 0; i < paramLen; i += 2) {
                    String paramValue = params[i+1];
                    if (!TextUtils.isEmpty(paramValue)) {
                        sb.append(PARAMETER_SEPARATOR);
                        sb.append(params[i]).append(PARAMETER_VALUE_SEPARATOR);
                        if ((params[i] != null) && (params[i].equalsIgnoreCase(ICalNames.PARAM_TZ_ID))) {
                            escapeTZIDParam(sb, paramValue);
                        } else {
                            escapeParam(sb, paramValue);
                        }
                    }
                }
            }

            // BEGIN Motorola, xrfb74, 08/01/2012, IKCBSMMCPPRC-1631
            if (!ICalNames.PROP_SUMMARY.equals(propName) 
                    &&!ICalNames.PROP_LOCATION.equals(propName)
                    &&!ICalNames.PROP_DESCRIPTION.equals(propName)){
                sb.append(PROPERTY_VALUE_SEPARATOR);
            }
            // END IKCBSMMCPPRC-1631
            if (escapeValue) {
                if ((propName != null) && (propName.equalsIgnoreCase(ICalNames.PROP_TZ_ID))) {
                    escapeTZIDText(sb, value);
                } else {
                    escapeText(sb, value);
                }
            } else {
                sb.append(value);
            }
            // BEGIN Motorola, xrfb74, 08/01/2012, IKCBSMMCPPRC-1631
            if (ICalNames.PROP_SUMMARY.equals(propName) 
                    || ICalNames.PROP_LOCATION.equals(propName)
                    || ICalNames.PROP_DESCRIPTION.equals(propName)){
                int idx = 0;
                while ((idx = sb.indexOf("\\")) != -1){
                    sb.deleteCharAt(idx);
                }
            }
            // END IKCBSMMCPPRC-1631

            printFolded(sb.toString());
        }
    }

    // Maps an ActiveSync recurrence type to a VCAL FREQ property.
    private static final String[] TYPES = {
        ICalNames.REC_TYPE_DAILY,
        ICalNames.REC_TYPE_WEEKLY,
        ICalNames.REC_TYPE_MONTHLY,
        ICalNames.REC_TYPE_MONTHLY,
        "",
        ICalNames.REC_TYPE_YEARLY,
        ICalNames.REC_TYPE_YEARLY,
    };

    // Maps a day of the week (0 = Sunday) to the VAL name, e.g. SU
    private static final String[] WEEKDAYS = {
        ICalNames.REC_WEEKDAY_SUNDAY,
        ICalNames.REC_WEEKDAY_MONDAY,
        ICalNames.REC_WEEKDAY_TUESDAY,
        ICalNames.REC_WEEKDAY_WEDNESDAY,
        ICalNames.REC_WEEKDAY_THURSDAY,
        ICalNames.REC_WEEKDAY_FRIDAY,
        ICalNames.REC_WEEKDAY_SATURDAY,
    };

    /**
     * Appends a comma-separated list of VCAL weekdays from the ActiveSync days bitmask given.
     * @param sb The StringBuilder to append the list to.
     * @param dayOfWeek The day of week mask: 1=SUNDAY, 2=MONDAY, 4=TUEDSAY etc.
     */
    private static void appendByWeekDayList(StringBuilder sb, int dayOfWeek) {
        int mask = 1;
        boolean notFirst = false;
        for (int i = 0; i < 7; i++) {
            if ((dayOfWeek & mask) != 0) {
                if (notFirst) {
                    sb.append(',');
                }
                sb.append(WEEKDAYS[i]);
                notFirst = true;
            }
            mask <<= 1;
        }
    }

    /**
     * Appends an RRULE property that is derived from the given SystemTime values.
     * @param sysTime The SystemTime to use. Note that ActiveSync uses SystemTime in
     * a very idiosyncratic way. In particular, the Day field is actually used as an
     * "occurrence in month" value, in the range 1-5, where 5 means "last occurrence in
     * the month.
     */
    private void appendRecRuleForTZ(ASTimeZone.SystemTime sysTime) {
        if (sysTime.isValid()) {
            // Only include the RRULE if it looks as though the SystemTime is a real one.
            StringBuilder sb = new StringBuilder(60);
            sb.append(ICalNames.REC_FREQ).append('=').append(ICalNames.REC_TYPE_YEARLY).append(';');
            sb.append(ICalNames.REC_INTERVAL).append("=1;");
            int occurrenceInMonth = sysTime.getDay();
            if (occurrenceInMonth == 5) {
                occurrenceInMonth = -1;
            }
            sb.append(ICalNames.REC_BY_DAY).append('=').append(occurrenceInMonth).append(WEEKDAYS[sysTime.getDayOfWeek()]).append(';');
            sb.append(ICalNames.REC_BY_MONTH).append('=').append(sysTime.getMonth());
            appendProperty(ICalNames.PROP_RECURRENCE_RULE, sb, false);
        }
    }


    /**
     * Appends the given value as an  offset of the form [-]HHMM.
     * @param sb The StringBuilder to append to.
     * @param offset A signed offset in minutes.
     * @return A signed hour and minute offset.
     */
    private static void appendOffset(StringBuilder sb, int offset) {
        boolean neg = offset < 0;
        if (neg) {
            sb.append('-');
            offset = -offset;
        } else {
            sb.append('+');
        }
        appendDigits(sb, offset / 60, 2);
        appendDigits(sb, offset % 60, 2);
    }

    /**
     * Appends the given SystemTime to the StringBuilder as a String of the form 16010101THHMMSS for use in a TimeZone
     * @param time The local time of a daylight/standard transition.
     * @return The hours, minutes and seconds of the transition, on a fixed date.
     */
    private static void appendTZDateTime(StringBuilder sb, ASTimeZone.SystemTime time) {
        sb.append("16010101");
        sb.append('T');
        appendDigits(sb, time.getHour(), 2);
        appendDigits(sb, time.getMinute(), 2);
        appendDigits(sb, time.getSecond(), 2);
    }

    /**
     * Appends the given value as a 0-padded string to the StringBuilder.
     * @param sb The StringBuilder to take the digits.
     * @param value The value to add.
     * @param digits The minimum number of digits the number should occupy.
     */
    private static void appendDigits(StringBuilder sb, int value, int digits) {
        String str = String.valueOf(value);
        for (int i = str.length(); i < digits; i++) {
            sb.append('0');
        }
        sb.append(str);
    }

    /**
     * Appends the input value escaped according to VCAL rules for escaping property values
     * to the given StringBuilder.
     * @param sb The StringBuilder to append to.
     * @param value The value to escape.
     * @return The escaped value.
     */
    private void escapeText(StringBuilder sb, CharSequence value) {
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\\' || c == ';' || c == ',') {
                sb.append('\\');
                sb.append(c);
            } else if (c== '\t' || (c >= ' ' && c != 0x7f)) {
                sb.append(c);
            } else if (c == '\r') {
                continue;
            } else {
                sb.append('?');
            }
        }
    }

    /* TZID paramter value has more strict constraints, refer to RFC5545(2445) for TZID definition */
    private void escapeTZIDText(StringBuilder sb, CharSequence value) {
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\\') {
                sb.append('\\');
                sb.append(c);
            } else if (c == '"' || c == ':' || c == ';' || c == ',') {
                sb.append('?');
            } else if (c== '\t' || (c >= ' ' && c != 0x7f)) {
                sb.append(c);
            } else {
                sb.append('?');
            }
        }
    }

    /**
     * Appends the input value escaped according to VCAL rules for escaping parameter values to the StringBuilder.
     * @param sb The StringBuilder to append to.
     * @param value The value to escape.
     * @return The escaped value.
     */
    private void escapeParam(StringBuilder sb, String value) {
        int len = value.length();
        sb.append('"');
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            // Replace control characters and "
            if (c == '"' || c < ' ' || c == 0x7f) {
                c= '?';
            }
            sb.append(c);
        }
        sb.append('"');
    }

    /* TZID paramter value has more strict constraints, refer to RFC5545(2445) for TZID definition */
    private void escapeTZIDParam(StringBuilder sb, String value) {
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            // Replace control characters and '"', ';', ',', ':'
            if (c == '"' || c == ';' || c == ',' || c == ':' || c < ' ' || c == 0x7f) {
                c= '?';
            }
            sb.append(c);
        }
    }

    /**
     * Caculate the actual length of the folded line by avoiding breaking at the middle of
     * a utf8 char number.
     * Char. number range | UTF-8 octet sequence
     * (hexadecimal) | (binary)
     * --------------------+---------------------------------------------
     * 0000 0000-0000 007F | 0xxxxxxx
     * 0000 0080-0000 07FF | 110xxxxx 10xxxxxx
     * 0000 0800-0000 FFFF | 1110xxxx 10xxxxxx 10xxxxxx
     * 0001 0000-0010 FFFF | 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
     * @param in The utf8 byte array
     * @param offset The offset in the array from which to caculate the actual line length
     * @param maxLen The maximum length for each folded line.
     * @return the folded line length which should be no more than maxLen
     */
    private static int getFoldedLineLength(final byte[] in, int offset, int maxLen) {
        if ((in == null) || (in.length == 0) || (offset >= in.length) || (maxLen <= 0)) {
            return 0;
        }

        int index;
        int buffLen = in.length;
        if (offset + maxLen >= buffLen) {
            index = buffLen;
        } else {
            index = offset + maxLen;
            int value = ((int)in[index]) & 0xC0;
            while ((value == 0x80) && (index > offset)) {
                --index;
                value = ((int)in[index]) & 0xD0;
            }
        }

        return (index - offset);
    }


    /**
     * Prints the give value to the output stream, obeying VCAL folding rules: if the
     * value is longer than 75 chars, stuff with the sequence \n\r<space> every 75 chars.
     * Note that this stuffing occurs AFTER UTF-8 conversion, so a UTF-8 sequence might
     * end up with the stuffing sequence in the middle of it.
     * @param value The String to output as a folded, UTF-8 byte sequence.
     */
    private void printFolded(String value) {
        byte[] bytes;
        try {
            bytes = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("no utf-8 support!");
        }

        int len = bytes.length;
        int lineLen = LINE_LEN_LIMIT;
        int start = 0;
        try {
            while (start < len) {
                int foldLen = getFoldedLineLength(bytes, start, lineLen);
                mStream.write(bytes, start, foldLen);
                // Always followed by a newline
                mStream.write('\r');
                mStream.write('\n');
                start += foldLen;
                if (start < len) {
                    // If we have more to go, write the folding char and modify the lineLen to one less.
                    mStream.write(FOLDING_CHAR);
                    lineLen = LINE_LEN_LIMIT - 1;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e + "Coudn't write to output stream");
        }
    }

   /**
     * Appends a RRULE to the ical without adding escape text
     * @param rrule The rrule read from the calendar database event table.
     */
    public void appendRrule(String rrule){
         appendProperty(ICalNames.PROP_RECURRENCE_RULE, rrule, false);
    }

    // Where we're outputting to
    private final OutputStream mStream;

    // The generated index of time zone IDs
    private int mTimeZoneIndex;

    private static final char FOLDING_CHAR                  = ' ';
    private static final int  LINE_LEN_LIMIT                = 75;
    private static final char PARAMETER_SEPARATOR           = ';';
    private static final char PARAMETER_VALUE_SEPARATOR     = '=';
    private static final char PROPERTY_VALUE_SEPARATOR      = ':';



}
