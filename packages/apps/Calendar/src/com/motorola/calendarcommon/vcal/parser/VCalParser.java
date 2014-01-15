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
 * 19-Dec-2011    a21263       Enhancement to support vCal1.0
 **********************************************************
 */

package com.motorola.calendarcommon.vcal.parser;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import android.text.TextUtils;
import android.util.Log;

import com.android.calendarcommon.ICalendar;
import com.motorola.calendarcommon.vcal.common.CalendarEvent;


/**
 * abstract class for ical/vcal parser
 * @hide
 */
public abstract class VCalParser {
    public static final String TAG = "VCalParser";
    public static final String VCAL1 = "1.0";
    public static final String VCAL2 = "2.0";

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
                            if (propName.equalsIgnoreCase("uid")){
                                event.uid = new String(p.getValue());
                            }
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

    /**
     * replcae escape characters from the string
     */
    private static String replace(String line) {
        if (line  == null) {
            return null;
        }
        char[] ch = line.toCharArray();
        int len = ch.length;
        StringBuilder sb =new StringBuilder();
        boolean wait = false;
        for (int i=0; i<len; i++){
            if(ch[i] == '\\') {
                wait = true;
                continue;
            } else {
                if (wait == true && (ch[i] == 'n' || ch[i] == 'N')) {
                    sb.append("\n");
                } else {
                    sb.append(ch[i]);
                }
                wait = false;
            }
        }
        return sb.toString();
    }

    /**
     * Decodes a quoted-printable string into its original. Escaped characters are converted
     * back to their original representation.
     *
     * @param str
     *        quoted-printable string to convert into its original form
     *
     * @throws UnsupportedEncodingException
     *         thrown if quoted-printable decoding is unsuccessful
     * @return original string
     */
    private static String decodeQuotedPrintable(String str) throws UnsupportedEncodingException {
        if (str == null) {
            return null;
        }

        // UTF-8 is compatible with US_ASCII
        byte[] bytes = str.getBytes("UTF-8");
        if (bytes == null) {
            return null;
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i];
            if (b == '=') {
                /* 2012-09-25, added by yul for SWITCHUITWOV-244*/
                if((bytes[i+1]=='n')&&(bytes[i+2]=='=')){
			i++;
			continue;
                }
                /* 2012-09-25, add end */
                try {
                    int u = Character.digit((char) bytes[++i], 16);
                    int l = Character.digit((char) bytes[++i], 16);
                    if (u == -1 || l == -1) {
                        throw new UnsupportedEncodingException();
                    }
                    buffer.write((char) ((u << 4) + l));
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new UnsupportedEncodingException();
                }
            } else {
                buffer.write(b);
            }
        }

        return new String(buffer.toByteArray(), "UTF-8");
    }


    /**
      * Processes the given Calendar Property to a string
      * @param property Property to be processed
      * @return details Calendar property as string
      */
    public static String getUnescapedPropertyValue(ICalendar.Property property) {
        if(property == null) {
            return null;
        }

        String encoding = "";
        ICalendar.Parameter paramEncoding = property.getFirstParameter("ENCODING");
        if(paramEncoding != null) {
            encoding = paramEncoding.value;
        } else {
            //Try "QUOTED-PRINTABLE"
            paramEncoding = property.getFirstParameter("QUOTED-PRINTABLE");
            if(paramEncoding != null) {
                //encoding=QUOTED-PRINTABLE
                encoding = "QUOTED-PRINTABLE";
            }
        }

        String s = null;
        if (encoding.equalsIgnoreCase("QUOTED-PRINTABLE")) {
            try {
                s = decodeQuotedPrintable(property.getValue());
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Can't decode QUOTED-PRINTABLE string");
                s = null;
            }
            if( s !=null){
                s = s.replaceAll("\r\n", "\n");
            }
        } else {
            s = replace(property.getValue());
        }

        return s;
    }
}
