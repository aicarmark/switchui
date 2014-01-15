/*
 * Copyright (C) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 02-Feb-2010    tbdc37       Ported from SyncML
 **********************************************************
 */

package com.motorola.calendar.share.vcalendar.parser;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.android.calendarcommon.ICalendar;
import android.text.TextUtils;

import com.motorola.calendar.share.vcalendar.common.CalendarEvent;



/**
* abstract class for ical/vcal parser
*/
public abstract class VCalParser {
    public static final int           VCAL1   = 1;
    public static final int           VCAL2   = 2;

    public abstract Collection<CalendarEvent> parse(final String strVcal) throws ICalendar.FormatException;

    public abstract Collection<CalendarEvent> parse(ICalendar.Component vCalendar) throws ICalendar.FormatException;

    protected void handleExtendedProperties(ICalendar.Component calendarComp, CalendarEvent event) {

        Set<String> propNames = calendarComp.getPropertyNames();
        Iterator<String> propNameIt = propNames.iterator();
        while(propNameIt.hasNext()) {
            // Don't try to translate propName to upper case.
            // We need respect the value in the iCal file.
            String propName = propNameIt.next();
            //TODO
            if(isExtentedProperty(propName)) {
                List<ICalendar.Property> extProps = calendarComp.getProperties(propName);
                if (extProps != null) {
                    Iterator<ICalendar.Property> exPropsIt = extProps.iterator();

                    while(exPropsIt.hasNext()) {
                        ICalendar.Property p = exPropsIt.next();
                        // Change for icecream
                        if (!TextUtils.isEmpty(p.getValue())) {
                            CalendarEvent.ExtProp exProp = new CalendarEvent.ExtProp();
                            exProp.name = propName;
                            exProp.value = p.getValue();
                            event.extendedProperties.add(exProp);
                            event.hasExtendedProperties = true;
                        }
                    }
                }
            }
        }
    }

    protected boolean isExtentedProperty(String prop) {
        if (TextUtils.isEmpty(prop)){
            return false;
        }

        prop = prop.toUpperCase();
        // don't support VTODO now
        if(prop.startsWith("X-") ||
                 prop.equals("UID") ||
                 prop.equals("DTSTAMP") ||
                 prop.equals("SEQUENCE")) {
             return true;
        }
        return false;
    }
}
