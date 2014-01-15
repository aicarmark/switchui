/*
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: VCalComposer_V1.java
 * Description: Composes ical
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 27-Dec-2011    a21263       Created file
 **********************************************************
 */

package com.motorola.calendar.share.vcalendar.composer;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Reminders;
import android.util.Log;
import android.util.Config;
import android.text.TextUtils;
import android.text.format.Time;
import android.text.format.DateUtils;

import com.android.calendarcommon.EventRecurrence;

import com.motorola.calendar.share.vcalendar.common.CalendarEvent;
import com.motorola.calendar.share.vcalendar.common.Duration;

public class VCalComposer_V1 implements VCalComposer {

    private final static String TAG = "VCalComposer_V1";
    private final static boolean DEVELOPMENT = Config.DEBUG;
    private CalendarEvent mCe;
    private ByteArrayOutputStream mOS;
    private ICalBuilder mBuilder;
    /**
    * Constructor
    * @param ce
    */
    public VCalComposer_V1(CalendarEvent ce){
        mCe = ce;
        mOS = new ByteArrayOutputStream();
        mBuilder= new ICalBuilder(mOS);
    }

    /**
     * composes ical based on calendar event
     * @return ical text
     */
    public String composeVCal() {
        if (mCe == null) {
            if (DEVELOPMENT) {
                Log.v(TAG, "Calendar event empty");
            }
            return null;
        }

        // VCALENDAR header
        appendHeader();

        appendEventBody();

        // VCALENDAR footer
        appendFooter();

        String result = null;
        try {
            mOS.flush();
            result = mOS.toString("utf-8");
            if (DEVELOPMENT) {
                Log.v(TAG, "composeVCal :\n" + result);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }

    /**
    * appends attendees to builder object
    */
    private void appendAttendees() {

        Iterator<CalendarEvent.Attendee> it = mCe.attendees.iterator();
        ArrayList<String> params = new ArrayList<String>();
        while(it.hasNext()) {
            params.clear();
            CalendarEvent.Attendee att = it.next();

            if(!TextUtils.isEmpty(att.email)) {
                params.add("ROLE");
                if (att.relationship == Attendees.RELATIONSHIP_ORGANIZER) {
                    params.add("ORGANIZER");
                } else {
                    params.add("ATTENDEE");
                }

                params.add("EXPECT");
                String expecttype = "FYI";
                switch(att.type) {
                    case Attendees.TYPE_REQUIRED:
                        expecttype = "REQUIRE";
                        break;
                    case Attendees.TYPE_OPTIONAL:
                         expecttype= "REQUEST";
                        break;
                    default:
                        break;
                }
                params.add(expecttype);

                String statusvalue = null;
                switch(att.status) {
                    case Attendees.ATTENDEE_STATUS_NONE:
                        break;
                    case Attendees.ATTENDEE_STATUS_ACCEPTED:
                        statusvalue = "ACCEPTED";
                        break;
                    case Attendees.ATTENDEE_STATUS_DECLINED:
                        statusvalue = "DECLINED";
                        break;
                    case Attendees.ATTENDEE_STATUS_INVITED:
                        break;
                    case Attendees.ATTENDEE_STATUS_TENTATIVE:
                        statusvalue = "TENTATIVE";
                        break;
                    default:
                        break;
                }
                if(statusvalue != null) {
                    params.add("PARTSTAT");
                    params.add(statusvalue);
                }

                String address;
                if(!TextUtils.isEmpty(att.name)) {
                    address = att.name + "<" + att.email + ">";
                } else {
                    address = att.email;
                }
                mBuilder.appendProperty("ATTENDEE", address, params.toArray(new String[params.size()]));
            }
        }
    }
    /**
    * append extended property to builder object
    */
    private void appendExtendedProperty() {
        Iterator<CalendarEvent.ExtProp> it = mCe.extendedProperties.iterator();
        while(it.hasNext()) {
            CalendarEvent.ExtProp extProp = it.next();
            mBuilder.appendProperty(extProp.name, extProp.value);
        }
    }

    /**
     * append event body to builder object
     */
    private void appendEventBody() {
        // Header
        mBuilder.appendProperty(ICalNames.PROP_BEGIN, ICalNames.OBJ_NAME_EVENT);

        mBuilder.appendProperty(ICalNames.PROP_SUMMARY, mCe.summary);
        mBuilder.appendProperty(ICalNames.PROP_LOCATION, mCe.location);
        mBuilder.appendProperty(ICalNames.PROP_DESCRIPTION, mCe.description);

        // Transparency, busy-bit
        mBuilder.appendProperty(ICalNames.PROP_TRANSPARENCY,
                mCe.isTransparent ? ICalNames.TRANSP_TRANSPARENT : ICalNames.TRANSP_OPAQUE);

        // Event status
        String status;
        switch (mCe.status) {
            case Events.STATUS_CONFIRMED:
                status = ICalNames.STATUS_CONFIRMED;
                break;
            case Events.STATUS_TENTATIVE:
                status = ICalNames.STATUS_TENTATIVE;
                break;
            default:
                status = ICalNames.STATUS_NEEDS_ACTION;
                break;
        }
        mBuilder.appendProperty(ICalNames.PROP_STATUS, status);

        // Start time, end time (always use UTC). Per vCal1.0, DTEND is mandatory.
        appendDateTime(ICalNames.PROP_START_DATETIME, mCe.dtStart);
        appendDateTime(ICalNames.PROP_END_DATETIME, mCe.dtEnd);

        // Recurrence rules and duration.
        if (!TextUtils.isEmpty(mCe.rrule)) {
            appendRrule(mCe.rrule);
            // Don't support EXRULE now since calendar app doesn't support it.
            // mBuilder.appendProperty("EXRULE", mCe.exrule);

            String duration;
            if (!TextUtils.isEmpty(mCe.duration)) {
                mBuilder.appendProperty(ICalNames.PROP_DURATION, mCe.duration);
                duration = mCe.duration;
            } else {
                // Change for icecream
                long end = mCe.dtEnd;
                long start = mCe.dtStart;

                if ((start > 0L) && (end > start)) {
                    if (mCe.isAllDay) {
                        long days = (end - start + DateUtils.DAY_IN_MILLIS - 1) / DateUtils.DAY_IN_MILLIS;
                        duration = "P" + days + "D";
                    } else {
                        long seconds = (end - start) / DateUtils.SECOND_IN_MILLIS;
                        duration = "P" + seconds + "S";
                    }
                    mBuilder.appendProperty(ICalNames.PROP_DURATION, duration);
                } else {
                    throw new IllegalStateException("ICal file has incorrect DURATION and DTEND value");
                }
            }
            if (mCe.dtEnd == CalendarEvent.FIELD_EMPTY_INT) {
                long dtEnd = mCe.dtStart + getEndTime(duration);
                appendDateTime(ICalNames.PROP_END_DATETIME, dtEnd);
            }
        }

        // UID
        mBuilder.appendProperty(ICalNames.PROP_UID, mCe.uid);

        appendAttendees();
        appendAlarms();
        // Don't support extended properties now since some property value
        // has no relationship with iCal at all. They're just used for internal logic.
        // TODO:
        // Maybe we can refer to the mobile phone code to set some properites specially
        // for Exchange server. But since now we only need support ics file instead of
        // text/calendar MIME type in the message body, we needn not do that currently.
        // If we do need implement that later, please ensure we can check the account type
        // to make sure we're handling ActiveSync account. Because different account type
        // has different definitions for some fields such as transparence value and import
        // flag etc.
        // appendExtendedProperty();

        // Footer
        mBuilder.appendProperty(ICalNames.PROP_END, ICalNames.OBJ_NAME_EVENT);
    }

    /**
     * Converts one of the internal day constants (SU, MO, etc.) to the
     * two-letter string representing that constant.
     *
     * @param day one the internal constants SU, MO, etc.
     * @return the two-letter string for the day ("SU", "MO", etc.)
     *
     * @throws IllegalArgumentException Thrown if the day argument is not one of
     * the defined day constants.
     */
    private static String day2String(int day) {
        switch (day) {
            case EventRecurrence.SU:
                return "SU";
            case EventRecurrence.MO:
                return "MO";
            case EventRecurrence.TU:
                return "TU";
            case EventRecurrence.WE:
                return "WE";
            case EventRecurrence.TH:
                return "TH";
            case EventRecurrence.FR:
                return "FR";
            case EventRecurrence.SA:
                return "SA";
            default:
                throw new IllegalArgumentException("bad day argument: " + day);
        }
    }

    private void appendRrule(String rrule) {
        StringBuilder strBuilder = new StringBuilder();
        EventRecurrence recurrence = new EventRecurrence();

        try {
            recurrence.parse(rrule);
            switch (recurrence.freq) {
                case EventRecurrence.DAILY:
                    strBuilder.append('D');
                    if (recurrence.interval > 0) {
                        strBuilder.append(recurrence.interval);
                    } else {
                        strBuilder.append(1);
                    }
                    break;

                case EventRecurrence.WEEKLY:
                    strBuilder.append('W');
                    if (recurrence.interval > 0) {
                        strBuilder.append(recurrence.interval);
                    } else {
                        strBuilder.append(1);
                    }
                    for (int i = 0; i < recurrence.bydayCount; ++i) {
                        strBuilder.append(' ').append(day2String(recurrence.byday[i]));
                    }
                    break;

                case EventRecurrence.MONTHLY:
                    // vCalendar1.0 only supports BYDAY or BYMONTHDAY for MONTHLY recurrence.
                    if (recurrence.bydayCount > 0) {
                        strBuilder.append("MP");
                        if (recurrence.interval > 0) {
                            strBuilder.append(recurrence.interval);
                        } else {
                            strBuilder.append(1);
                        }
                        if (recurrence.bysetposCount > 0) {
                            for (int i = 0; i < recurrence.bysetposCount; ++i) {
                                strBuilder.append(' ');
                                if (recurrence.bysetpos[i] < 0) {
                                    strBuilder.append((-1)*(recurrence.bysetpos[i])).append('-');
                                } else {
                                    strBuilder.append(recurrence.bysetpos[i]).append('+');
                                }
                                for (int j = 0; j < recurrence.bydayCount; ++j) {
                                    strBuilder.append(' ').append(day2String(recurrence.byday[i]));
                                }
                            }
                        } else {
                            for (int j = 0; j < recurrence.bydayCount; ++j) {
                                strBuilder.append(' ');
                                if (recurrence.bydayNum[j] < 0) {
                                    strBuilder.append((-1)*(recurrence.bydayNum[j])).append('-');
                                } else {
                                    strBuilder.append(recurrence.bydayNum[j]).append('+');
                                }
                                strBuilder.append(' ').append(day2String(recurrence.byday[j]));
                            }
                        }
                    } else if (recurrence.bymonthdayCount > 0) {
                        strBuilder.append("MD");
                        if (recurrence.interval > 0) {
                            strBuilder.append(recurrence.interval);
                        } else {
                            strBuilder.append(1);
                        }
                        for (int i = 0; i < recurrence.bymonthdayCount; ++i) {
                            strBuilder.append(' ');
                            if (recurrence.bymonthday[i] < 0) {
                                strBuilder.append((-1)*(recurrence.bymonthday[i])).append('-');
                            } else {
                                strBuilder.append(recurrence.bymonthday[i]).append('+');
                            }
                        }
                    }
                    break;

                case EventRecurrence.YEARLY:
                    // vCalendar1.0 only supports BYMONTH or BYYEARDAY for YEALY recurrence.
                    if (recurrence.bymonthCount > 0) {
                        strBuilder.append("YM");
                        if (recurrence.interval > 0) {
                            strBuilder.append(recurrence.interval);
                        } else {
                            strBuilder.append(1);
                        }
                        for (int i = 0; i < recurrence.bymonthCount; ++i) {
                            strBuilder.append(' ').append(recurrence.bymonth[i]);
                        }
                    } else if (recurrence.byyeardayCount > 0) {
                        strBuilder.append("YD");
                        if (recurrence.interval > 0) {
                            strBuilder.append(recurrence.interval);
                        } else {
                            strBuilder.append(1);
                        }
                        for (int i = 0; i < recurrence.byyeardayCount; ++i) {
                            strBuilder.append(' ');
                            if (recurrence.byyearday[0] < 0) {
                                strBuilder.append((-1)*(recurrence.byyearday[i])).append('-');
                            } else {
                                strBuilder.append(recurrence.byyearday[i]).append('+');
                            }
                        }
                    }
                    break;

                default:
                    break;
            }

            if (strBuilder.length() > 0) {
                // vCalendar1.0 only supports DAILY, WEEKLY, MONTHLY and YEALY frequency
                if (recurrence.freq == EventRecurrence.DAILY ||
                        recurrence.freq == EventRecurrence.WEEKLY ||
                        recurrence.freq == EventRecurrence.MONTHLY ||
                        recurrence.freq == EventRecurrence.YEARLY) {
                    if (!TextUtils.isEmpty(recurrence.until)) {
                        strBuilder.append(' ').append(recurrence.until);
                    } else {
                        // for vCalendar1.0, #0 means repeat forever.
                        // From current EventRecurrence implementation, it's impossible to
                        // tell whether COUNT is 0 or there's no COUNT property at all. Since
                        // "COUNT=0" is a rare case, we always treat "recurrence.count=0" as
                        // repeat forever now.
                        strBuilder.append('#').append(recurrence.count);
                    }
                }
                mBuilder.appendRrule(strBuilder.toString());
            }
        } catch (Exception e) {
            if (DEVELOPMENT) {
                Log.e(TAG, "Error in recurrence rule");
            }
        }
    }

    /**
     * Append Date-Time for date time property of iCalendar.
     *
     * @param propName The property name.
     * @param dtTime Time in milliseconds from epoch.
     */
    private void appendDateTime(String propName, long dtTime) {
        if (DEVELOPMENT) {
            Log.v(TAG, "appendDateTime, [" + propName + ", " + dtTime + "]");
        }
        if (dtTime != -1L) {
            Time t;
            // Always stores UTC value
            t = new Time(Time.TIMEZONE_UTC);
            t.set(dtTime);
            mBuilder.appendDateTime(propName, null, t.format2445() + "Z");
        }
    }

    /**
    * append ical header to  builder object
    */
    private void appendHeader() {
        mBuilder.appendProperty(ICalNames.PROP_BEGIN, ICalNames.OBJ_NAME_CALENDAR);
        mBuilder.appendProperty(ICalNames.PROP_PRODUCT_ID, getProdID());
        mBuilder.appendProperty(ICalNames.PROP_VERSION, getVersion());
        // Don't append Daylight and Timezone since we'll always use UTC time
    }

    private void appendFooter() {
        mBuilder.appendProperty(ICalNames.PROP_END, ICalNames.OBJ_NAME_CALENDAR);
    }

   /**
    * @return version of the vCal 1.0
    */
    private String getVersion(){
        return "1.0";
    }
    /**
    * @return product id
    */
    private String getProdID(){
        return "-//Motorola Corporation//Android//EN";
    }
    /**
    * generate time in seconds from the duration string
    */
    private long getEndTime(String duration){
        long seconds = 30*DateUtils.MINUTE_IN_MILLIS;
        Duration durationParser = new Duration();
        if(duration != null) {
            try {
                durationParser.parse(duration);
                seconds = 0L;
            } catch(IllegalArgumentException ex) {
                Log.w(TAG, ex);
            }
        }
        return durationParser.addTo(seconds);
    }

    /**
     * Appends alarms to the list
     */
    private void appendAlarms() {
        Iterator<CalendarEvent.Alarm> it = mCe.alarms.iterator();
        while (it.hasNext()) {
            CalendarEvent.Alarm alm = it.next();
            // Only handle alert now
            if (alm.method == Reminders.METHOD_ALERT) {
                Time t = new Time(Time.TIMEZONE_UTC);
                t.set(mCe.dtStart-alm.minutes*DateUtils.MINUTE_IN_MILLIS);
                // Append a display alarm which happens at (dtStart-minutes) with
                // 5 minutes duration, repeat only once and with empty display text.
                mBuilder.appendProperty("DALARM",
                        new StringBuilder(t.format2445()).append('Z').append(";PT5M").
                        append(";1").append("").toString());
            }
        }
    }
}
