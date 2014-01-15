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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.CalendarAlerts;
import android.content.ContentValues;
import com.android.calendarcommon.ICalendar;
import com.android.calendarcommon.ICalendar.FormatException;
import com.android.calendarcommon.RecurrenceSet;
import android.text.TextUtils;
import android.text.format.Time;
import android.text.format.DateUtils;
import android.util.TimeFormatException;
import android.util.Log;
import android.util.Config;

import com.motorola.calendar.share.vcalendar.common.CalendarEvent;
import com.motorola.calendar.share.vcalendar.common.Duration;


/**
* Implements parser methods
*/
public class VCalParser_V2 extends VCalParser {
    private static final String       TAG     = "VCALENDAR_V2";
    private static final boolean DEVELOPMENT = Config.DEBUG;
    protected String mTimeZoneId = null;
    private ArrayList<String> mTZList = new ArrayList<String>();
    private int mStandardOffset = -1;
    private int mDayLightOffset = -1;

    /**
    * Constructor
    */
    public VCalParser_V2() {
    }
    /**
    * parses string based ical
    * @param strVcal vcal data
    * @return Collection of CalendarEvent
    */
    @Override
    public Collection<CalendarEvent> parse(final String strVcal)
        throws ICalendar.FormatException {
        return parseIcal(strVcal);
    }

    @Override
    public Collection<CalendarEvent> parse(ICalendar.Component iCalendar)
        throws ICalendar.FormatException {
        return parseIcal(iCalendar);
    }

   /**
    * parses string based ical
    * @param ical vcal data
    * @return Collection of CalendarEvent
    */
    private Collection<CalendarEvent> parseIcal(final String ical) throws ICalendar.FormatException {
        // Commented for now to use CalendarUtils APIs
         ICalendar.Component iCalendar = ICalendar.parseCalendar(ical);
         return parseIcal(iCalendar);
         // return null;
    }

    private Collection<CalendarEvent> parseIcal(ICalendar.Component iCalendar) throws ICalendar.FormatException {
        Vector<CalendarEvent> calendarEvents = new Vector<CalendarEvent>();
        List<ICalendar.Component> components = iCalendar.getComponents();

        if (DEVELOPMENT) {
            Log.v(TAG, "Size of components:" + components.size());
        }

        // We need to handle VTIMEZONE first. This is ugly!
        Iterator<ICalendar.Component> it = components.iterator();
        while (it.hasNext()) {
            ICalendar.Component c = it.next();
            String name = c.getName();
            if (name.equalsIgnoreCase("VTIMEZONE")) {
                if (DEVELOPMENT) {
                    Log.v(TAG, "Found VTIMEZONE");
                }
                processVTimeZone(c);
                it.remove();
                break;
            }
        }
        

        it = components.iterator();
        while (it.hasNext()) {
            ICalendar.Component c = it.next();
            String name = c.getName();
            if (DEVELOPMENT) {
                Log.v(TAG, "Component name:" + name);
            }
            if (name != null) {
                if (name.equalsIgnoreCase("VEVENT")) {
                    CalendarEvent evt = processVEvent(c);
                    
                    if (evt != null) {
                        evt.timeZone = mTimeZoneId;
                        calendarEvents.add(evt);
                        if (DEVELOPMENT) {
                            Log.v(TAG, evt.toString());
                        }
                    }
                } else {
                    // Don't support VTODO now
                    // Oops! Unsupported component
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
     * find timezone id in the given timzone id list
     * @param tzId the timezone id to be found
     * @param availableIds the available timezone ids to be searched from
     * @return the timezone id that's been found, or null if not found
     */
    private String findTimeZoneId(final String tzId, final String[] availableIds) {
        if ((tzId == null) || (availableIds == null))
            return null;

        for(final String s : availableIds) {
            TimeZone tz = TimeZone.getTimeZone(s);
            String displayName = tz.getDisplayName();
            String id = tz.getID();
            if (displayName.compareToIgnoreCase(tzId) == 0 ||
                id.compareToIgnoreCase(tzId) == 0 ) {
                return id;
            }
        }

        return null;
    }

    /**
    * process Timezone information from the vcal
    * @param c component
    */
    private void processVTimeZone(ICalendar.Component c) {
        ICalendar.Property tzId = c.getFirstProperty("TZID");
        if (tzId != null) {

            String strTzId = tzId.getValue();
            if (strTzId != null) {
                if (mTZList.size()>0) {
                    return; //do not calculate it again
                }
            }else{
                return;
            }

            if (DEVELOPMENT) {
                Log.v(TAG, "TZID=" + strTzId);
            }

            List<ICalendar.Component> tzComponents = c.getComponents();
            if (tzComponents != null) {
                mTZList.add(strTzId);
                // We will choose first of the timezone info
                if(mTZList.size() > 0){
                    if(mWindowsToJavaTime.containsKey(strTzId)){
                        mTimeZoneId = mWindowsToJavaTime.get(strTzId);
                    }else{
                        mTimeZoneId = strTzId;
                    }
                    String[] timezones = TimeZone.getAvailableIDs();
                    String timeZoneId = findTimeZoneId(mTimeZoneId, timezones);
                    if (timeZoneId != null) {
                        mTimeZoneId = timeZoneId;
                        if (DEVELOPMENT) {
                            Log.v(TAG,"id name = "+mTimeZoneId);
                        }
                    }
                }
                //TODO: below usage
                /*
                Iterator<ICalendar.Component> it = tzComponents.iterator();
                int standardOffset = CalendarUtils.getOffsetMillis(timezoneComp, true);
                int daylightOffset = CalendarUtils.getOffsetMillis(timezoneComp, false);
                */
            } else {
                if (DEVELOPMENT) {
                    Log.v(TAG, "No tz comp");
                }
            }
        } else {
            if (DEVELOPMENT) {
                Log.v(TAG, "Oops, TZID is missing.");
            }
        }
    }
    /**
    * replcae escape characters from the string
    */
    private String replace(String line) {
        if(line  == null){
                return null;
        }
        char[] ch = line.toCharArray();
        int len = ch.length;
        StringBuilder sb =new StringBuilder();
        boolean wait = false;
        for(int i=0;i<len;i++){
              if(ch[i] == '\\'){
                wait = true;
                continue;
                }
                else{
                   if(wait == true && ch[i] == 'n' ){
                        sb.append("\n");
                   }else{
                        sb.append(ch[i]);
                   }
                        wait = false;
                }
            }
            return sb.toString();
    }
    /**
    * process component Event
    * @param c ICalendar Component
    * @return CalendarEvent
    * @throws FormatException
    */
    private CalendarEvent processVEvent(ICalendar.Component c) throws ICalendar.FormatException {
        if (true) {
            Log.v(TAG, "Entering processVEvent");
        }
        ICalendar.Property prop = null;
        CalendarEvent event = new CalendarEvent();

        boolean allDay = false;
        prop = c.getFirstProperty("DTSTART");
        if (prop != null) {
            allDay |= handleDate(prop, true, false, event);
        }

        prop = c.getFirstProperty("DTEND");
        if (prop != null) {
            allDay &= handleDate(prop, false, false, event);
        } else {
            allDay = false;
        }

        event.isAllDay = allDay;

        processComponent(c, event);

        // if there's no DTEND or DTEND is invalid, we'll try to caculate it by using DTSTART+DURATION
        if ((event.dtEnd < event.dtStart) && (event.duration != null)) {
            Duration duration = new Duration();
            duration.parse(event.duration);
        // modified by amt_zhangsongshun 2012-01-31 SWITCHUITWO-598 begin    
            event.dtEnd = event.dtStart + duration.getMillis();
        // modified by amt_zhangsongshun 2012-01-31 SWITCHUITWO-598 end       
        }

        // according to SDK API description for Time.parse, date-time string includes only date
        // and no time field (such as 20100224) will be treated as allDay. So event with DTSTART
        // 20100224T000000 and DTEND 20100225T000000 will not be treated as all day event according to
        // current algorithm (although it should be). We'll do some amendment for this case here.
        if (!allDay) {
            long duration = event.dtEnd -event.dtStart;
            if (duration == DateUtils.DAY_IN_MILLIS) {
                Time t = new Time(mTimeZoneId);
                t.set(event.dtStart);
                if ((t.hour == 0) && (t.minute == 0) && (t.second == 0)) {
                    event.isAllDay = true;
                    // reset start time and end time and set time zone to UTC, so this event will be
                    // shown on the same day on different time zones. That's required by all-day event.
                    t.timezone = Time.TIMEZONE_UTC;
                    event.dtStart = t.normalize(true);

                    t.clear(mTimeZoneId);
                    t.set(event.dtEnd);
                    t.timezone = Time.TIMEZONE_UTC;
                    event.dtEnd = t.normalize(true);

                    mTimeZoneId = Time.TIMEZONE_UTC;
                }
            }
        }

        return event;
     }

    private CalendarEvent processVTodo(ICalendar.Component c) throws ICalendar.FormatException {
        if (DEVELOPMENT) {
             Log.v(TAG, "Entering processVTodo");
        }

        ICalendar.Property prop = null;
        CalendarEvent event = new CalendarEvent();

        boolean allDay = false;
        prop = c.getFirstProperty("DTSTART");
        if (prop != null) {
            allDay |= handleDate(prop, true, false, event);
        }

        prop = c.getFirstProperty("DUE");
        if (prop != null) {
            allDay &= handleDate(prop, false, false, event);
        } else {
            allDay = false;
        }

        prop = c.getFirstProperty("COMPLETED");
        if (prop != null) {
            allDay &= handleDate(prop, false, true, event);
        } else {
            allDay = false;
        }

        event.isAllDay = allDay;

        processComponent(c, event);

        return event;
    }

    private void processComponent(ICalendar.Component c, CalendarEvent event) {
        ICalendar.Property prop = c.getFirstProperty("SUMMARY");
        if (prop != null) {
            // TODO Handle property parameters
            event.summary = replace(prop.getValue());
        }

        prop = c.getFirstProperty("DESCRIPTION");
        if (prop != null) {
            event.description = replace(prop.getValue());
        }

        // give default status as TENTATIVE
        event.status = Events.STATUS_TENTATIVE;
        prop = c.getFirstProperty("STATUS");
        // TENTATIVE, CONFIRMED, CANCELLED
        if (prop != null) {
            String status = prop.getValue();
            status = status.toUpperCase();
            if (status.equals("TENTATIVE")) {
                event.status = Events.STATUS_TENTATIVE;
            } else if (status.equals("CONFIRMED")) {
                event.status = Events.STATUS_CONFIRMED;
            } else if (status.equals("CANCELLED")) {
                event.status = Events.STATUS_CANCELED;
            }
        }

        prop = c.getFirstProperty("TRANSP");
        event.isTransparent = false;
        if (prop != null) {
            String transp = prop.getValue();
            if (transp.equalsIgnoreCase("TRANSPARENT")) {
                event.isTransparent = true;
            }
        }

        prop = c.getFirstProperty("LOCATION");
        if (prop != null) {
            event.location = replace(prop.getValue());
        }

        prop = c.getFirstProperty("DURATION");
        if (prop != null) {
            event.duration = prop.getValue();
        }

        // RRULE
        prop = c.getFirstProperty("RRULE");
        if (prop != null) {
            String rrule = prop.getValue();
            event.rrule = rrule;
        }

        // EXRULE
        prop = c.getFirstProperty("EXRULE");
        if (prop != null) {
            String exrule = prop.getValue();
            event.exrule = exrule;
        }

/*
  List<ICalendar.Property> rdateList = c.getProperties("RDATE");
  if (rdateList != null) {
  handleDateList(rdateList, event.rdates);
  }

  List<ICalendar.Property> exdateList = c.getProperties("EXDATE");
  if (exdateList != null) {
  handleDateList(exdateList, event.exdate);
  }
*/

        // ORGANIZER & ATTENDEE
        prop = c.getFirstProperty("ORGANIZER");
        if (prop != null) {
            handleAttendee(prop, event, true);
        }

        List<ICalendar.Property> attendeeList = c.getProperties("ATTENDEE");
        if (attendeeList != null) {
            Iterator<ICalendar.Property> itAttendee = attendeeList.iterator();
            while (itAttendee.hasNext()) {
                ICalendar.Property attendee = itAttendee.next();
                handleAttendee(attendee, event, false);
            }
        }

        List<ICalendar.Component> evtComponents = c.getComponents();
        if(evtComponents != null) {
            Iterator<ICalendar.Component> it = evtComponents.iterator();
            while(it.hasNext()) {
                ICalendar.Component comp = it.next();
                String name = comp.getName();
                if(name != null && name.equalsIgnoreCase("VALARM")) {
                     handleAlarm(comp, event);
                } else {
                    //TODO
                }
            }
        }

        //Last date
        //TODO: Calculate last date for recurrent event
        prop = c.getFirstProperty("RDATE");
        if(prop != null) {
            event.rdate= prop.getValue();
        }
        prop = c.getFirstProperty("EXDATE");
        if(prop != null) {
            event.exdate = prop.getValue();
        }

        if(event.rrule != null && !event.rrule.equals("")) {
            event.lastDate = event.dtEnd;
        }

        prop = c.getFirstProperty("UID");
        if(prop != null) {
            event.uid = prop.getValue();
        }

        try {
            calculateLastDate(event);
        } catch(Exception e) {
            e.printStackTrace();
        }

        //Extended properties, ugly!
        handleExtendedProperties(c, event);

        return;
    }
    private final static HashMap<String, String> mWindowsToJavaTime =
         new HashMap<String, String>();

    static {
        mWindowsToJavaTime.put("Chennai, Kolkata, Mumbai, New Delhi", "Asia/Calcutta");
        mWindowsToJavaTime.put("Asia/Kolkata", "Asia/Calcutta");
        mWindowsToJavaTime.put("Kuwait, Riyadh", "Asia/Kuwait");
        mWindowsToJavaTime.put("Abu Dhabi, Muscat", "Asia/Muscat");
        mWindowsToJavaTime.put("Canberra, Melbourne, Sydney", "Australia/Melbourne");
        mWindowsToJavaTime.put("Belgrade, Bratislava, Budapest, Ljubljana, Prague", "Europe/Belgrade");
        mWindowsToJavaTime.put("Sarajevo, Skopje, Warsaw, Zagreb", "Europe/Warsaw");
        mWindowsToJavaTime.put("Magadan, Solomon Islands, New Caledonia", "Pacific/Guadalcanal");
        mWindowsToJavaTime.put("Guadalajara, Mexico City, Monterrey", "America/Guatemala");//No Exact map
        mWindowsToJavaTime.put("Beijing, Chongqing, Hong Kong SAR, Urumqi", "Asia/Chungking");
        mWindowsToJavaTime.put("Fiji Islands, Kamchatka, Marshall Islands", "Pacific/Fiji");
        mWindowsToJavaTime.put("Helsinki, Kiev, Riga, Sofia, Tallinn, Vilnius", "Europe/Helsinki");
        mWindowsToJavaTime.put("Dublin, Edinburgh, Lisbon, London", "Europe/London");
        mWindowsToJavaTime.put("Chihuahua, La Paz, Mazatlan", "America/Mazatlan");
        mWindowsToJavaTime.put("Almaty, Novosibirsk", "Asia/Novosibirsk");
        mWindowsToJavaTime.put("Auckland, Wellington", "Pacific/Auckland");
        mWindowsToJavaTime.put("wfoundland and Labrador", "America/Goose_Bay");
        mWindowsToJavaTime.put("Brussels, Copenhagen, Madrid, Paris", "Europe/Paris");
        mWindowsToJavaTime.put("Moscow, St. Petersburg, Volgograd", "Europe/Moscow");
        mWindowsToJavaTime.put("Buenos Aires, Georgetown", "America/Buenos_Aires");
        mWindowsToJavaTime.put("Bogota, Lima, Quito", "America/Bogota");
        mWindowsToJavaTime.put("Caracas, La Paz", "America/Caracas");
        mWindowsToJavaTime.put("Midway Island, Samoa", "Pacific/Midway");
        mWindowsToJavaTime.put("Bangkok, Hanoi, Jakarta", "Asia/Bangkok");
        mWindowsToJavaTime.put("Kuala Lumpur, Singapore", "Asia/Singapore");
        mWindowsToJavaTime.put("Harare, Pretoria", "Africa/Harare");
        mWindowsToJavaTime.put("Sri Jayawardenepura", "Asia/Colombo");
        mWindowsToJavaTime.put("Osaka, Sapporo, Tokyo", "Asia/Tokyo");
        mWindowsToJavaTime.put("Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna", "Europe/Amsterdam");
        mWindowsToJavaTime.put("Islamabad, Karachi, Tashkent", "Asia/Karachi");
        mWindowsToJavaTime.put("Guam, Port Moresby", "Pacific/Guam");
        mWindowsToJavaTime.put("Pacific Time (US & Canada)", "US/Pacific");
        mWindowsToJavaTime.put("Mountain Time (US & Canada)", "US/Mountain");
        mWindowsToJavaTime.put("Central Time (US & Canada)", "US/Central");
        mWindowsToJavaTime.put("Eastern Time (US & Canada)", "US/Eastern");
    }
    /**
    * parses date from ical
    * @param prop
    * @param event
    * @param isDtStart
    * @param isDtComplete
    * @return true if the date is allday else false
    * @throws FormatException
    */
    private boolean handleDate(ICalendar.Property prop, boolean isDtStart,
            boolean isDtComplete, CalendarEvent event) throws ICalendar.FormatException {
        String strRFC2445 = prop.getValue();
        boolean dateOnly = false;
        ICalendar.Parameter param = prop.getFirstParameter("TZID");
        if(param != null) {
            //TZID exists
            String tzIdToFound = replaceDoubleQuotes(param.value);
            if (DEVELOPMENT) {
                Log.v(TAG,"tzid :"+tzIdToFound);
            }
            //Get tz from java.util.TimeZone first
            String[] availableIds = TimeZone.getAvailableIDs();
            String tzId = findTimeZoneId(tzIdToFound, availableIds);
            if (tzId == null) {
                if(mWindowsToJavaTime.containsKey(tzIdToFound)){
                    // define a new temp variable to avoid dirtying tzIdToFound
                    String tzIdStr = mWindowsToJavaTime.get(tzIdToFound);
                    tzId = findTimeZoneId(tzIdStr, availableIds);
                }

                if (tzId == null) {
                    if (mStandardOffset != -1){
                        String[] timezones = TimeZone.getAvailableIDs(mStandardOffset);
                        tzId = findTimeZoneId(tzIdToFound, timezones);
                    }
                }
            }

            if (tzId != null) {
                //tz shouldn't be GMT
  //              TimeZone tz = TimeZone.getTimeZone(tzId);
                Time t = new Time(tzId);
                t.parse(strRFC2445);
                dateOnly |= t.allDay;
                if (DEVELOPMENT) {
                    Log.v(TAG, "strRFC2445:" + strRFC2445);
                    Log.v(TAG, "startTime:" + t.toString());
                }
                if(isDtStart) {
                    event.dtStart = t.normalize(true);
                } else if (isDtComplete){
                    event.comlpetedDate = t.normalize(true);
                } else {
                    event.dtEnd = t.normalize(true);
                    event.due = event.dtEnd;
                }
                mTimeZoneId = tzId;
            } else {
                //Not found in system TZ registry...
                //We are on our own...
                //TODO
	            if (DEVELOPMENT) {
	                Log.v(TAG, "Not able to find time do it default way");
				}
                dateOnly = setTimeValues(strRFC2445, isDtStart,isDtComplete, event);
            }
        } else {
            //UTC or floating time
            dateOnly = setTimeValues(strRFC2445, isDtStart,isDtComplete, event);
        }
        return dateOnly;
    }

    private boolean setTimeValues(String strRFC2445, boolean isDtStart,
			boolean isDtComplete, CalendarEvent event) throws ICalendar.FormatException {
        boolean dateOnly = false;
        Time t = null;
        if(strRFC2445.endsWith("Z") || strRFC2445.endsWith("z")) {
            //UTC
            t = new Time(Time.TIMEZONE_UTC);
        } else {
            //Floating time, just use local timezone.
            //TODO:
            //Calendar cal = Calendar.getInstance();
            //TimeZone tz = cal.getTimeZone();
            t = new Time();
        }
        try {
            t.parse(strRFC2445);
        } catch (TimeFormatException e) {
            throw new ICalendar.FormatException("Cannot parse time string :" + strRFC2445);
        }
        dateOnly |= t.allDay;
        if (t.allDay) {// IKSTABLEFOURV-4913 .ics preview time is different from event time
            // when get the all day meeting invite ics from a gmail, there's no propertites like below:
            // DTSTART;TZID=China Standard Time:20110315T083035
            // DTEND;TZID=China Standard Time:20110315T093035
            // consequently, it maybe mistaken as a NOT all day event
            // however, t.parse(strRFC2445) can be used to check whether time is date only
            // if so, we still take it as an all day event
            t.timezone = Time.TIMEZONE_UTC;
        }
        mTimeZoneId = t.timezone;
        if(isDtStart) {
            event.dtStart = t.normalize(true);
        } else if (isDtComplete){
            event.comlpetedDate = t.normalize(true);
        } else {
            event.dtEnd = t.normalize(true);
            event.due = event.dtEnd ;
        }
        return dateOnly;
    }

    /**
    * fetch attendees and convert to CalendarEvent attendees
    * @param attendee
    * @param attendees
    * @param isOrganizer
    */
    private void handleAttendee(ICalendar.Property attendee, CalendarEvent event,
                                boolean isOrganizer) {

        CalendarEvent.Attendee att = new CalendarEvent.Attendee();
        Vector<CalendarEvent.Attendee> attendees = event.attendees;

        ICalendar.Parameter parameter = attendee.getFirstParameter("CN");
        if (parameter != null) {
            att.name = replaceDoubleQuotes(parameter.value);
        }

        parameter = attendee.getFirstParameter("ROLE");
        if (parameter != null) {
            String role = replaceDoubleQuotes(parameter.value.toUpperCase());
            if (role.equals("REQ-PARTICIPANT")) {
                att.type = Attendees.TYPE_REQUIRED;
            } else if (role.equals("OPT-PARTICIPANT")) {
                att.type = Attendees.TYPE_OPTIONAL;
            } else {
                att.type = Attendees.TYPE_NONE;
            }
        }

        att.relationship = isOrganizer ? Attendees.RELATIONSHIP_ORGANIZER
            : Attendees.RELATIONSHIP_ATTENDEE;

        parameter = attendee.getFirstParameter("PARTSTAT");
        if (parameter != null) {
            String partStat = replaceDoubleQuotes(parameter.value.toUpperCase());
            if (partStat.equals("ACCEPTED")) {
                att.status = Attendees.ATTENDEE_STATUS_ACCEPTED;
            } else if (partStat.equals("DECLINED")) {
                att.status = Attendees.ATTENDEE_STATUS_DECLINED;
            } else if (partStat.equals("TENTATIVE")) {
                att.status = Attendees.ATTENDEE_STATUS_TENTATIVE;
            } else {
                att.status = Attendees.ATTENDEE_STATUS_NONE;
            }
        }

        String email = replaceDoubleQuotes(attendee.getValue());
        if(email.startsWith("mailto:") || email.startsWith("MAILTO:")){
        int index = email.indexOf(":");
        email = email.substring(index+1,email.length());
        }
        att.email = email;
        if (isOrganizer) {
            event.organizer = email;
            att.status = Attendees.ATTENDEE_STATUS_ACCEPTED;
            attendees.add(att);
        } else if (!email.equalsIgnoreCase(event.organizer)) {
            // don't add duplicate organizer info
            attendees.add(att);
        }
    }

    /**
    * handle alarm string
    * @param c
    * @param e
    */
    private void handleAlarm(ICalendar.Component c, CalendarEvent e) {
        e.hasAlarm = true;
        CalendarEvent.Alarm alarm = new CalendarEvent.Alarm();
        alarm.state = CalendarAlerts.STATE_SCHEDULED;
        ICalendar.Property prop = c.getFirstProperty("ACTION");
        if(prop != null) {
            String s = prop.getValue();
            if(s.equalsIgnoreCase("DISPLAY")) {
                alarm.method = Reminders.METHOD_ALERT;
            } else if(s.equalsIgnoreCase("EMAIL")) {
                alarm.method = Reminders.METHOD_EMAIL;
            } else {
                alarm.method = Reminders.METHOD_DEFAULT;
            }
        }
        prop = c.getFirstProperty("TRIGGER");
        if(prop != null) {
            ICalendar.Parameter valueType = prop.getFirstParameter("VALUE");
            if(valueType != null) {
                //date time
                if(valueType.value != null) {
                    if(valueType.value.equalsIgnoreCase("DATE-TIME")) {
                        String absAlarmTime = prop.getValue();
                        if(absAlarmTime != null) {
                            handleAbsoluteTrigger(alarm,absAlarmTime,e);
                        }
                    } else {
                        //TODO
                    }
                } else {
                    //Oops
                }
            } else {
                //duration
                boolean relationship_start = true;
                ICalendar.Parameter relationship = prop.getFirstParameter("RELATED");
                if(relationship != null) {
                    String relVal = replaceDoubleQuotes(relationship.value);
                    if(relVal != null && relVal.equalsIgnoreCase("END")) {
                        relationship_start = false;
                    }
                }

                String strDuration = prop.getValue();
                if(strDuration != null) {
                    Duration durationParser = new Duration();
                    try {
                    durationParser.parse(strDuration);
                    long millis = durationParser.addTo(0);
                    int minutes =(int) millis/(60*1000);
                    int seconds = (int) millis/1000;
                    alarm.minutes = minutes;
                    alarm.alarmTime = (relationship_start ? e.dtStart : e.dtEnd) -
                        (seconds * durationParser.sign == '+' ? -1 : 1);
                    } catch(Exception ex) {
                         ex.printStackTrace();
                         handleAbsoluteTrigger(alarm,strDuration,e);
                         e.alarms.add(alarm);
                         return;
                    }
                } else {
                    //Oops
                }
            }
        }
        e.alarms.add(alarm);
    }

    private void handleAbsoluteTrigger(CalendarEvent.Alarm alarm,
            String absAlarmTime, CalendarEvent e) {
        Time t = new Time();
        t.parse(absAlarmTime);
        t.normalize(true);
        long millis = t.toMillis(true);
        alarm.alarmTime = millis;
        // TODO
        // Calculate Alarm.minutes
        long milliseconds = 0l;
        if (e.dtStart != CalendarEvent.FIELD_EMPTY_INT) {
            milliseconds = e.dtStart - millis;
        } else {
            //This situation will occur when the parsed data is
            //of todo type..it may not have start time.
            //So alarm will be calculated from due date
            if (DEVELOPMENT) {
                 Log.v(TAG,"dtend : " +e.dtEnd);
                 Log.v(TAG,"mills : " +millis);
            }
            milliseconds = e.dtEnd - millis;
        }

        long minutes = milliseconds / 60 /1000;
        alarm.minutes = minutes;
    }
    /**
    * calculate last date of occurence
    * @param event
    */
    private void calculateLastDate(CalendarEvent event) throws Exception {
        ContentValues values = new ContentValues();
        values.put(Events.DTSTART, event.dtStart);
        values.put(Events.DTEND, event.dtEnd);
        values.put(Events.RRULE, event.rrule);
        values.put(Events.DURATION, event.duration);
        values.put(Events.EVENT_TIMEZONE, event.tz);
        values.put(Events.RDATE, event.rdate);
        values.put(Events.EXRULE, event.exrule);
        values.put(Events.EXDATE, event.exdate);

        long dtstartMillis = values.getAsLong(Events.DTSTART);
        long lastMillis = -1;

        // Can we use dtend with a repeating event?  What does that even
        // mean?
        // NOTE: if the repeating event has a dtend, we convert it to a
        // duration during event processing, so this situation should not
        // occur.
        Long dtEnd = values.getAsLong(Events.DTEND);
        if (dtEnd != null) {
            lastMillis = dtEnd;
        } else {
            // find out how long it is
            Duration duration = new Duration();
            String durationStr = values.getAsString(Events.DURATION);
            if (durationStr != null) {
                duration.parse(durationStr);
            }

            RecurrenceSet recur = new RecurrenceSet(values);

            if (recur.hasRecurrence()) {
                // the event is repeating, so find the last date it
                // could appear on

                String tz = values.getAsString(Events.EVENT_TIMEZONE);

                if (TextUtils.isEmpty(tz)) {
                    // floating timezone
                    tz = Time.TIMEZONE_UTC;
                }
                Time dtstartLocal = new Time(tz);

                dtstartLocal.set(dtstartMillis);

                RecurrenceProcessor rp = new RecurrenceProcessor();
                lastMillis = rp.getLastOccurence(dtstartLocal, recur);
                if (lastMillis == -1) {
                    //return lastMillis;  // -1
                }
            } else {
                // the event is not repeating, just use dtstartMillis
                lastMillis = dtstartMillis;
            }
            // that was the beginning of the event.  this is the end.
            lastMillis += duration.getMillis();
            if(lastMillis != -1) {
                event.lastDate = lastMillis;
            }
        }
    }
    /**
     * replaces leading and trailing double quotes from the string
     * @param text
     * @return
     */
     private String replaceDoubleQuotes(String text){
     if(!TextUtils.isEmpty(text)){
           int len = text.length();
           if(text.startsWith("\"") && text.endsWith("\"")){
               text = String.copyValueOf(text.toCharArray(), 1, len-2);
           }
               return text;
     }else{
           return text;
     }
    }

// TODO delete function
//
//     private int parseOffset(String offsetStr) {
//         if (TextUtils.isEmpty(offsetStr)){
//             return -1;
//         }
//         boolean neg = offsetStr.startsWith("-");
//         int startHours = neg || offsetStr.startsWith("+") ? 1 : 0;
//         int startMins = startHours+2;
//         int hh = Integer.parseInt(offsetStr.substring(startHours, startMins));
//         int mm = Integer.parseInt(offsetStr.substring(startMins, startMins+2));
//         int offset = (hh*60 + mm)*1000*60;
//         return neg ? -offset : offset;
//     }
}
