/*
 * Copyright (C) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 02-Feb-2010    tbdc37       Ported from SyncML
 **********************************************************
 *
 */
package com.motorola.calendar.share.vcalendar.common;

import java.util.Iterator;
import java.util.Vector;

/**
 *
 * @author e12085
 */
public class CalendarEvent {
    public static final int FIELD_EMPTY_INT = -1;
    /**
    * Class to hold Attendees and its params
    */
    public static class Attendee {
        public String name = "";
        public String email = "";

        public int relationship = 0;

        public int type = 0;

        public int status = 0;
    }
    /**
    * Class to hold extended property name and value
    */
    public static class ExtProp {
        public String name = "";
        public String value = "";
    }
    /**
    * Class to hold Alarm params
    */
    public static class Alarm {
        //Epoch
        public long alarmTime;

        public int state = 0;
        public long minutes;

        public int method;
    }

    public String summary = "";
    public String location = "";
    public String description = "";
    public int status;
    public long dtStart = FIELD_EMPTY_INT;
    public long dtEnd = FIELD_EMPTY_INT;
    public long lastDate = FIELD_EMPTY_INT;
    public long due = FIELD_EMPTY_INT;
    public String duration = "";
    public boolean isAllDay;
    public boolean isTransparent;
    public String rrule = "";
    public String exrule = "";
    public String tz = "UTC";
    public boolean hasAlarm = false;
    public boolean hasExtendedProperties = false;

    public String timeZone = null;
    public long dtStamp = FIELD_EMPTY_INT;
    public String uid = "";
    public boolean isVtodo = false;
    public long comlpetedDate = FIELD_EMPTY_INT;

    //Epoch time
    //public Vector<Long> rdates = new Vector<Long>();
    //Epoch time
    //public Vector<Long> exdate = new Vector<Long>();
    public String rdate = "";
    public String exdate = "";

    public String organizer = "";
    public Vector<Attendee> attendees = new Vector<Attendee>();

    public Vector<ExtProp> extendedProperties = new Vector<ExtProp>();

    public Vector<Alarm> alarms = new Vector<Alarm>();

    public Alarm alarm = null;
    /**
    * Convert CalendarEvent to readable string
    */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n");
        sb.append("Summary=" + summary + "\n");
        sb.append("Location=" + location + "\n");
        sb.append("Description=" + description + "\n");
        sb.append("Status=" + status + "\n");
        sb.append("Allday=" + isAllDay + "\n");
        sb.append("Transparent=" + isTransparent + "\n");
        sb.append("RRULE=" + rrule + "\n");
        sb.append("EXRULE=" + exrule + "\n");
        sb.append("DTSTART=" + dtStart + "\n");
        sb.append("DTEND=" + dtEnd + "\n");
        sb.append("TZ=" + tz + "\n");
        sb.append("DURATION=" + duration + "\n");
        sb.append("LASTDATE=" + lastDate + "\n");
        sb.append("ORGANIZER=" + organizer + "\n");

        Iterator<Attendee> it = attendees.iterator();
        while(it.hasNext()) {
            sb.append("Attendee: \n");
            Attendee att = it.next();
            sb.append("\tName:" + att.name + "\n");
            sb.append("\tEmail:" + att.email + "\n");
            sb.append("\tRelationship:" + att.relationship + "\n");
            sb.append("\tType:" + att.type + "\n");
            sb.append("\tStatus:" + att.status + "\n");
        }
        sb.append("Extended Properties:\n");
        Iterator<ExtProp> extIt = extendedProperties.iterator();
        while(extIt.hasNext()) {
            ExtProp ext = extIt.next();
            sb.append("Name:" + ext.name + "\n");
            sb.append("Value:" + ext.value + "\n");
        }
        Iterator<Alarm> alarmIt = alarms.iterator();
        while(alarmIt.hasNext()) {
            sb.append("Alarm: \n");
            Alarm alarm = alarmIt.next();
            sb.append("Time:" + alarm.alarmTime + "\n");
            sb.append("State:" + alarm.state + "\n");
            sb.append("Minutes:" + alarm.minutes + "\n");
            sb.append("Method:" + alarm.method + "\n");
        }
        sb.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n");
        return sb.toString();
    }
}
