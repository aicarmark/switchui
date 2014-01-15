/*
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: VCalComposer_V2.java
 * Description: Composes ical
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 20-Feb-2010    tbdc37       Created file
 **********************************************************
 */

package com.motorola.calendarcommon.vcal.composer;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimeZone;

import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Reminders;
import android.util.Log;
import android.util.Config;
import android.text.TextUtils;
import android.text.format.Time;
import android.text.format.DateUtils;

import com.motorola.calendarcommon.vcal.common.CalendarEvent;
import com.motorola.calendarcommon.vcal.common.Duration;

/**
 * @hide
 */
public class VCalComposer_V2 implements VCalComposer {

    private final static String TAG = "VCalComposer_V2";
    private final static boolean DEVELOPMENT = Config.DEBUG;
    private CalendarEvent mCe;
    private ASTimeZone mAsTimeZone;
    private ByteArrayOutputStream mOS;
    private ICalBuilder mBuilder;
    /**
    * Constructor
    * @param ce
    */
    public VCalComposer_V2(CalendarEvent ce){
        mCe = ce;
        mOS = new ByteArrayOutputStream();
        mBuilder= new ICalBuilder(mOS);
    }

    /**
     * composes ical based on calendar event
     * @return ical text
     */
    public byte[] composeVCal() {
        if (mCe == null) {
            if (DEVELOPMENT) {
                Log.v(TAG, "Calendar event empty");
            }
            return null;
        }

        // VCALENDAR header
        appendHeader();

        // VTIMEZONE
        if (!TextUtils.isEmpty(mCe.tz)) {
            if (DEVELOPMENT) {
                Log.v(TAG, "Append timezone :" + mCe.tz);
            }
            TimeZone jTz = TimeZone.getTimeZone(mCe.tz);
            mAsTimeZone = getTimeZone(jTz);
            appendTimeZone(mAsTimeZone);
        }

        appendEventBody();

        // VCALENDAR footer
        appendFooter();

        try {
            mOS.flush();
            if (DEVELOPMENT) {
                Log.v(TAG, "composeVCal :\n" + mOS.toString("utf-8"));
            }
            return mOS.toByteArray();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
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
            String propName = att.relationship == Attendees.RELATIONSHIP_ORGANIZER ?
                    "ORGANIZER" : "ATTENDEE";

            if(!TextUtils.isEmpty(att.name)) {
                params.add("CN");
                params.add(att.name);
            }


            params.add("ROLE");
            String roletype = "NON-PARTICIPANT";
            switch(att.type) {
            case Attendees.TYPE_NONE:
                roletype = "NON-PARTICIPANT";
                break;
            case Attendees.TYPE_REQUIRED:
                roletype = "REQ-PARTICIPANT";
                break;
            case Attendees.TYPE_OPTIONAL:
                roletype = "OPT-PARTICIPANT";
                break;
            default:
                break;
            }
            params.add(roletype);


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
            if(!TextUtils.isEmpty(att.email)) {
                mBuilder.appendProperty(propName,att.email,params.toArray(new String[params.size()]));
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
            case Events.STATUS_CANCELED:
                status = ICalNames.STATUS_CANCELLED;
                break;
            case Events.STATUS_TENTATIVE:
            default:
                status = ICalNames.STATUS_TENTATIVE;
                break;
        }
        mBuilder.appendProperty(ICalNames.PROP_STATUS, status);

        // Start time, end time, and time stamp.
        if (TextUtils.isEmpty(mCe.tz)) {
            appendDateTime(ICalNames.PROP_START_DATETIME, mCe.dtStart);
            // Change for icecream
            // Per rfc5545, dtend and duration MUST NOT occur in the same VEVNENT envelope.
            if (TextUtils.isEmpty(mCe.rrule)) {
                appendDateTime(ICalNames.PROP_END_DATETIME, mCe.dtEnd);
            }
        } else {
            // ASTimeZone's standard name is the TZID in VTIMEZONE.
            appendDateTime(ICalNames.PROP_START_DATETIME, mCe.tz, mAsTimeZone.getStandardName(), mCe.dtStart);
            if (TextUtils.isEmpty(mCe.rrule)) {
                appendDateTime(ICalNames.PROP_END_DATETIME, mCe.tz, mAsTimeZone.getStandardName(), mCe.dtEnd);
            }
        }
        //In the case of an iCalendar object that specifies a
        //"METHOD" property, this property specifies the date and time that
        //the instance of the iCalendar object was created.
        Time time = new Time(Time.TIMEZONE_UTC);
        time.set(System.currentTimeMillis());
        mBuilder.appendDateTime(ICalNames.PROP_DATE_STAMP, null, time.format2445());

        // Recurrence rules and duration.
        if (!TextUtils.isEmpty(mCe.rrule)) {
            mBuilder.appendRrule(mCe.rrule);
            mBuilder.appendProperty("EXRULE", mCe.exrule);
            if (!TextUtils.isEmpty(mCe.duration)) {
                mBuilder.appendProperty(ICalNames.PROP_DURATION, mCe.duration);
            } else {
                // Change for icecream
                long end = mCe.dtEnd;
                long start = mCe.dtStart;
                String duration;

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
            // Change for icecream
            // Per rfc5545, dtend and duration MUST NOT occur in the same VEVNENT envelope.
            /*
            if (mCe.dtEnd == CalendarEvent.FIELD_EMPTY_INT) {
                if (!TextUtils.isEmpty(mCe.duration)) {
                    long dtEnd = mCe.dtStart + getEndTime(mCe.duration);
                    appendDateTime(ICalNames.PROP_END_DATETIME, dtEnd);
                }
            }
            */
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
     * Append Date-Time for date time property of iCalendar.
     *
     * @param propName The property name.
     * @param jTzId The Java Timezone Id
     * @param vTzId The Timezone Id in VTIMEZONE
     * @param dtTime Time in milliseconds from epoch.
     */
    private void appendDateTime(String propName, String jTzId, String vTzId, long dtTime) {
        if (DEVELOPMENT) {
            Log.v(TAG, "appendDateTime, [" + propName + ", " + jTzId + ", " + vTzId + ", " + dtTime + "]");
        }
        if (dtTime != -1L) {
            Time t;
            if (jTzId == null) {
                // If there's no timezone info, just output the times in UTC
                t = new Time(Time.TIMEZONE_UTC);
                t.set(dtTime);
                mBuilder.appendDateTime(propName, null, t.format2445() + "Z");
            } else {
                t = new Time(jTzId);
                t.set(dtTime);
                mBuilder.appendDateTime(propName, vTzId, t.format2445());
            }
        }
    }

    private void appendDateTime(String propName, long dtTime) {
        appendDateTime(propName, null, null, dtTime);
    }

    /**
    * append timezone property to builder object
    */
    private void appendTimeZone(ASTimeZone asTimezone) {
        if (asTimezone != null) {
            mBuilder.appendTimeZone(asTimezone);
        }
    }

    /**
    * append ical header to  builder object
    */
    private void appendHeader() {
        mBuilder.appendProperty(ICalNames.PROP_BEGIN, ICalNames.OBJ_NAME_CALENDAR);
        mBuilder.appendProperty(ICalNames.PROP_METHOD, "REQUEST");
        mBuilder.appendProperty(ICalNames.PROP_PRODUCT_ID, getProdID());
        mBuilder.appendProperty(ICalNames.PROP_VERSION, getVersion());
    }

    private void appendFooter() {
        mBuilder.appendProperty(ICalNames.PROP_END, ICalNames.OBJ_NAME_CALENDAR);
    }

   /**
    * @return version of the ical 2.0
    */
    private String getVersion(){
        return "2.0";
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
          long seconds = 0;
          Duration durationParser = new Duration();
          if(duration != null) {
               try {
                    durationParser.parse(duration);
               } catch(IllegalArgumentException ex) {
                    Log.w(TAG, ex);
                    return seconds;
               }
          }
          return durationParser.addTo(seconds);
    }

    /**
     * Converts TimeZone object to ASTimeZone
     *
     * @param timezone
     * @return ASTimeZone
     */
    private ASTimeZone getTimeZone(TimeZone timezone) {
        final long MILLISECONDS_PER_YEAR = 1461L * 24 * 60 * 60 * 1000 / 4;
        long msStartTime = mCe.dtStart;
        int year = 1970 + (int) (msStartTime / MILLISECONDS_PER_YEAR);
        return ASTimeZone.newFromTimeZone(timezone, year);
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
                mBuilder.appendAlarm((int) alm.minutes);
            }
        }
    }
}
