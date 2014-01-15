/*
 * Copyright (C) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 02-Feb-2010    tbdc37       Ported from SyncML
 **********************************************************
 */

package com.motorola.calendar.share.vcalendar.parser;

import java.util.Collection;
import java.util.TimeZone;
import java.util.Vector;
import java.util.List;
import java.util.Iterator;

import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Reminders;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.Config;

//import org.apache.commons.codec.net.QuotedPrintableCodec;
//import org.apache.commons.codec.DecoderException;
import com.android.calendarcommon.ICalendar;

import com.motorola.calendar.share.vcalendar.common.CalendarEvent;

/**
  * This Class Parses Version 1.0 VCal file
  */
public class VCalParser_V1 extends VCalParser {
    private static final String TAG = "VCALENDAR";
    private static final boolean DEVELOPMENT = Config.DEBUG;

    private static final String VCAL_DURATION_P = "P";  //vCalendar protocol, all duration value is started by "P"
    private static final String VCAL_DURATION_S = "S";  //vCalendar protocol, "S" means Second

    /**
      * Constructs a new  VCalParser_V1 object
      */
    public VCalParser_V1() {
    }

   /**
     * Parses a given vcal data to CalendarEvent collection
     * @param strVcal Vcal data to be parsed as string
     * @return collection of calendarEvents
     */
    @Override
   public Collection<CalendarEvent> parse(final String strVcal) throws ICalendar.FormatException {
      // Commented for now to use CalendarUtils APIs
       ICalendar.Component vCalendar = ICalendar.parseCalendar(strVcal);
       return parse(vCalendar);
       //return null;
   }

    @Override
    public Collection<CalendarEvent> parse(ICalendar.Component vCalendar) throws ICalendar.FormatException {
        Vector<CalendarEvent> calendarEvents = new Vector<CalendarEvent>();

        //VERSION
        ICalendar.Property prop = vCalendar.getFirstProperty("VERSION");
        if(prop == null) {
            throw new ICalendar.FormatException("VERSION missing.");
        }
        String version = prop.getValue();
        if(version == null || !version.equals("1.0")) {
            throw new ICalendar.FormatException("Incorrect version.");
        }

        //vCalendar specific properties
        //DAYLIGHT
        prop = vCalendar.getFirstProperty("DAYLIGHT");
        if(prop != null) {
            //
        }

        //TZ
        //TZ:-05
        //TZ:+05:30
        int tzBias = 0;
        prop = vCalendar.getFirstProperty("TZ");
        if(prop != null) {
            String tz = prop.getValue();
            int len = tz.length();
            if(len > 2) {
                char c = tz.charAt(0);
                int sign = c == '-' ? -1 : 1;
                if(c == '-' || c == '+') {
                    tz = tz.substring(1);
                    int factor = 600;
                    for(int i = 0; i < len - 1 && factor > 0; i++) {
                        c = tz.charAt(i);
                        if(Character.isDigit(c)) {
                            tzBias += factor * (c - 48);
                            factor /= 10;
                        } else if(c == ':') {
                            factor = 10;
                        }
                    }
                }
                tzBias *= sign;
            }
        }

        List<ICalendar.Component> components = vCalendar.getComponents();
        Iterator<ICalendar.Component> it = components.iterator();
        while(it.hasNext()) {
            ICalendar.Component c = it.next();
            String name = c.getName();
            if (name != null) {
                if(name.equalsIgnoreCase("VEVENT")) {
                    CalendarEvent evt = processVEvent(c, tzBias);
                    if(evt != null) {
                        calendarEvents.add(evt);
                        if (DEVELOPMENT) {
                            Log.v(TAG,"Parse VEvent = " + evt.toString());
                        }
                    }
                } else {
                    // Don't support VTODO now
                    //Just ignore it!
                }
            } else {
                if (DEVELOPMENT) {
                    Log.v(TAG, "Component name null");
                }
            }
        }

        return calendarEvents;
    }

    /**
      * Constructs a Calendar VEVENT from componenent
      * @param e Individiual ICalendar Componenet
      * @param tzBias timezone information
      * @return Calendar VEVENT
      */
    private CalendarEvent processVEvent(ICalendar.Component e, int tzBias) {
        CalendarEvent event = new CalendarEvent();

        //set default timezone
        event.tz = TimeZone.getDefault().getID();

        //ATTENDEE
        List<ICalendar.Property> attendeeList = e.getProperties("ATTENDEE");
        if(attendeeList != null) {
            Iterator<ICalendar.Property> itAttendee = attendeeList.iterator();
            while(itAttendee.hasNext()) {
                ICalendar.Property attendee = itAttendee.next();
                handleAttendee(attendee, event);
            }
        }

        //DESCRIPTION
        ICalendar.Property prop = e.getFirstProperty("DESCRIPTION");
        String description = processString(prop);
        if(description != null) {
            event.description = description;
        }

        prop = e.getFirstProperty("EXDATE");
        if(prop != null) {
            //TODO
        }

        prop = e.getFirstProperty("EXRULE");
        if(prop != null) {
            //TODO
        }

        prop = e.getFirstProperty("LOCATION");
        String location = processString(prop);
        if(location != null) {
            event.location = location;
        }

        prop = e.getFirstProperty("RDATE");
        if(prop != null) {
            //TODO
        }

        prop = e.getFirstProperty("DTSTART");
        long dtstart = processDateTime(prop, tzBias);
        event.dtStart = dtstart;

        //DTEND
        prop = e.getFirstProperty("DTEND");
        long dtend = processDateTime(prop, tzBias);
        event.dtEnd = dtend;

        prop = e.getFirstProperty("DUE");
        if (prop != null ) {
            event.due = processDateTime(prop, tzBias);
            event.dtEnd = event.due;
        }

        prop = e.getFirstProperty("COMPLETED");
        if (prop != null) {
            event.comlpetedDate = processDateTime(prop, tzBias);
        }

        prop = e.getFirstProperty("DURATION");
        if (prop != null) {
             event.duration = prop.getValue();
        } else if (!event.rrule.equals("")) {   //if calendar has rrule, the duration is needed for Google calendar
            long duration = (event.dtEnd - event.dtStart) / DateUtils.SECOND_IN_MILLIS;
            event.duration = VCAL_DURATION_P + duration + VCAL_DURATION_S;
        }

        // Same as google calendar, For recurring events, we must make sure that we use duration rather
        // than dtend. So, set dtEnd to invalid value and it wouldn't be inserted to db
        if (!event.rrule.equals("")) {
            event.dtEnd = CalendarEvent.FIELD_EMPTY_INT;
        }

        //NEEDS ACTION - default
        //SENT
        //TENTATIVE
        //CONFIRMED
        //DECLINED
        //DELEGATED
        prop = e.getFirstProperty("STATUS");
        if(prop != null) {
            event.status = Events.STATUS_TENTATIVE;
            String value = prop.getValue();
            if(value != null) {
                if(value.equalsIgnoreCase("CONFIRMED")) {
                    event.status = Events.STATUS_CONFIRMED;
                } else {
                    event.status = Events.STATUS_TENTATIVE;
                }
            }
        }

        //SUMMARY
        prop = e.getFirstProperty("SUMMARY");
        String summary = processString(prop);
        if(summary != null) {
            event.summary = summary;
        }

        //TRANSP
        //>=1
        //0
        prop = e.getFirstProperty("TRANSP");
        if(prop != null) {
            event.isTransparent = false;
            String value = prop.getValue();
            if(value != null) {
                event.isTransparent = !value.equals("0");
            }
        }

        List<ICalendar.Property> alarmList = e.getProperties("DALARM");
        if(alarmList != null) {
            Iterator<ICalendar.Property> itAlarm = alarmList.iterator();
            if (alarmList.size() > 0)
                event.hasAlarm = true;

            while(itAlarm.hasNext()) {
                ICalendar.Property alarm = itAlarm.next();
                handleAlarms(alarm, event, tzBias);
            }
        }

        handleExtendedProperties(e, event);

        return event;
    }

    /**
      * Processes Event date and time , converts to milli seconds
      * @param property individual icalendar property
      * @param tzBias timezone information
      * @return time in milliseconds
      */
    private long processDateTime(ICalendar.Property property, int tzBias) {
        if(property != null) {
            //TODO
            String rfc2445Time = property.getValue();
            if (DEVELOPMENT) {
                Log.v(TAG, "Date:" + rfc2445Time);
            }
            if(rfc2445Time != null) {
                if(rfc2445Time.endsWith("Z")) {
                    //UTC
                    Time t = new Time(Time.TIMEZONE_UTC);
                    t.parse(rfc2445Time);
                    return t.toMillis(false);
                } else {
                    //LOCAL time
                    //TODO
                    Time t = new Time(Time.TIMEZONE_UTC);
                    t.parse(rfc2445Time);
                    long millis = t.toMillis(false);
                    millis -= 1000L * 60 * tzBias;
                    return millis;
                }
            } else {
                //Oops
                return -1;
            }
        }
        return -1;
    }

    /**
      * Processes the given Calendar Property to a string
      * @param property Property to be processed
      * @return details Calendar property as string
      */
    private String processString(ICalendar.Property property) {
        if(property == null) {
            return null;
        }

        /*
        String encoding = "";
        ICalendar.Parameter paramEncoding = property.getFirstParameter("ENCODING");
        if(paramEncoding != null) {
            encoding = paramEncoding.value;
            if (DEVELOPMENT) {
                Log.v(TAG, "Encoding:" + encoding);
            }
        } else {
            //Try "QUOTED-PRINTABLE"
            paramEncoding = property.getFirstParameter("QUOTED-PRINTABLE");
            if(paramEncoding != null) {
                //encoding=QUOTED-PRINTABLE
                encoding = "QUOTED-PRINTABLE";
                if (DEVELOPMENT) {
                    Log.v(TAG, "Encoding=QUOTED-PRINTABLE");
                }
            }
        }

        String s = null;
        if(encoding.equalsIgnoreCase("QUOTED-PRINTABLE")) {
            if (DEVELOPMENT) {
                Log.v(TAG, "Decoding QP");
            }
            QuotedPrintableCodec qpCodec = new QuotedPrintableCodec();
            try {
                s = qpCodec.decode(property.getValue());
                if( s !=null){
                     s = s.replaceAll("\r\n", "\n");
                }
            } catch(DecoderException de) {
                Log.e(TAG, "", de);
            }
        } else {
            s = property.getValue();
        }
        return s;
        */
        return property.getValue();
    }

    /**
      * handles attendedd details
      * @param attendee Ical attendee property
      * @param attendees
      **/
    private void handleAttendee(ICalendar.Property attendee, CalendarEvent event) {
        //TODo
        CalendarEvent.Attendee att = new CalendarEvent.Attendee();
        Vector<CalendarEvent.Attendee> attendees = event.attendees;

        ICalendar.Parameter param = attendee.getFirstParameter("ROLE");
        if(param != null) {
            String value = param.value;
            if(value != null) {
                if(value.equalsIgnoreCase("ATTENDEE")) {
                    att.relationship = Attendees.RELATIONSHIP_ATTENDEE;
                } else if(value.equalsIgnoreCase("ORGANIZER")) {
                    att.relationship = Attendees.RELATIONSHIP_ORGANIZER;
                } else {
                    att.relationship = Attendees.RELATIONSHIP_NONE;
                }
            }
        }

        param = attendee.getFirstParameter("STATUS");
        if(param != null) {
            String value = param.value;
            if(value != null) {
                if(value.equalsIgnoreCase("ACCEPTED")) {
                    att.status = Attendees.ATTENDEE_STATUS_ACCEPTED;
                } else if(value.equalsIgnoreCase("TENTATIVE")) {
                    att.status = Attendees.ATTENDEE_STATUS_TENTATIVE;
                } else if(value.equalsIgnoreCase("DECLINED")) {
                    att.status = Attendees.ATTENDEE_STATUS_DECLINED;
                } else {
                    att.status = Attendees.ATTENDEE_STATUS_NONE;
                }
            }
        }

        att.type = Attendees.TYPE_REQUIRED;

        //It could be:
        //jsmith@hot1.com
        //or
        //John Smith <jsmith@host1.com>
        String cn_email = attendee.getValue();
        if(cn_email != null) {
            int index1 = cn_email.indexOf("<");
            int index2 = cn_email.indexOf(">");
            if(index1 != -1) {
                if(index2 != -1) {
                    String cn = cn_email.substring(0, index1);
                    String email = cn_email.substring(index1 + 1, index2);
                    att.name = cn.trim();
                    att.email = email.trim();
                }
            } else {
                if(cn_email.indexOf("@") != -1) {
                    att.email = cn_email;
                } else {
                    att.name = cn_email;
                }
            }
        }

        if (att.relationship == Attendees.RELATIONSHIP_ORGANIZER) {
            event.organizer = att.email;
            att.status = Attendees.ATTENDEE_STATUS_ACCEPTED;
            attendees.add(att);
        } else if (!att.email.equalsIgnoreCase(event.organizer)) {
            // don't add duplicate organizer info
            attendees.add(att);
        }
    }
    /**
    * read ical alarm component and update Calendar event
    * @param attendee
    * @param event
    * @tzBias
    */
    private void handleAlarms(ICalendar.Property attendee, CalendarEvent event,
            int tzBias) {
        // TODo
        CalendarEvent.Alarm att = new CalendarEvent.Alarm();

        String value = "";
        value = attendee.getValue();
        String Values[] = value.split(";");

        if (Values.length <= 0)
            return;

        long alarmtime = 0;
        String rfc2445Time = Values[0];
        if (rfc2445Time != null) {
            if (rfc2445Time.endsWith("Z")) {
                // UTC
                Time t = new Time(Time.TIMEZONE_UTC);
                t.parse(rfc2445Time);
                alarmtime = t.toMillis(false);
            } else {
                // LOCAL time
                // TODO
                Time t = new Time(Time.TIMEZONE_UTC);
                t.parse(rfc2445Time);
                long millis = t.toMillis(false);
                millis -= 1000L * 60 * tzBias;
                alarmtime = millis;
            }

            long premin = event.dtStart - alarmtime;
            att.minutes = premin / 1000 / 60;
            att.method = Reminders.METHOD_ALERT;

            event.alarms.add(att);
        }
    }
}
