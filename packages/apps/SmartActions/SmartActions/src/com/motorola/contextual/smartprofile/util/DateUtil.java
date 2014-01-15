/*
 * @(#)DateUtil.java
 *
 * (c) COPYRIGHT 2010 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2010/09/11 NA				  Initial version
 *
 */
package com.motorola.contextual.smartprofile.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.content.Context;
import android.provider.Settings;

/** This class hosts a number of distinct date routines mostly for formatting.
 *
 *<code><pre>
 * CLASS:
 * 	no extends, no implements
 *
 * RESPONSIBILITIES:
 * This class is entirely utility routines, all static, nothing instance-based.
 *
 * COLABORATORS:
 * 	None
 *
 * USAGE:
 *  See individual routines.
 *</pre></code>
 */
public class DateUtil {

//	private static final String TAG = "DateUtil";

    // cjd - there are date formats for Locale.  Please see those
    private static final String DEFAULT_DATE_FORMAT = "MM/dd/yy";
    private static final String TIME_FORMAT_24HR = "H:mm";
    private static final String TIME_FORMAT_12HR = "h:mma";

    /** formats a GregorianCalendar instance.
     *
     * To get current time:
     * <code><pre>
     * String s = formatDate(new GregorianCalendar());
     * </code></pre>
     * @param cal - calendar instance
     */
    public static String formatDateTime(GregorianCalendar cal) {

        // cjd - there are date formats for Locale.  Please see those
        return formatDate(cal, "MM/dd/yy hh:mm:sss a");
    }

    /** convenience method for formatIntlDate using a long date
     * @see formatIntlDateTime(GregorianCalendar)
     *
     * @param context - context
     * @param date - @see java.util.Date
     * @return string format that is international
     */
    public static String formatIntlDateTime(Context context, long date) {
        return formatIntlDateTime(context, toGregorianCalendar(date));
    }

    /** formats one of the common international date format (MM/dd/yy)
     *
     * @param context - context
     * @param cal - GregorianCalendar instance
     * @return string format that is international
     */
    public static String formatIntlDateTime(Context context, GregorianCalendar cal) {

        return formatDate(cal, getSystemDateFormat(context)+" "+getSystemTimeFormat(context));
    }

    /** convenience method for formatIntlDate using a long date
     * @see formatIntlDate(GregorianCalendar)
     *
     * @param context - context
     * @param date - @see java.util.Date
     * @return string format that is international
     */
    public static String formatIntlDate(Context context, long date) {

        return formatIntlDate(context, toGregorianCalendar(date));
    }

    /** formats one of the common international date format (dd-MMM-yy)
     *
     * @param context - context
     * @param cal - GregorianCalendar instance
     * @return - formatted date
     */
    public static String formatIntlDate(Context context, GregorianCalendar cal) {
        return formatDate(cal, getSystemDateFormat(context));
    }

    /** converts a long date to a GregorianCalendar instance
     *
     * @param date - date to be converted (see new Date().getTime().
     * @return - GregorianCalendar instance of the date.
     */
    public static GregorianCalendar toGregorianCalendar(long date) {

        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(date);
        return gc;
    }

    /** formats time portion only of a GregorianCalendar instance: MM/dd h:mma
     *
     * @param gc - gregorian calendar instance.
     * @return string format that is short.
     */
    public static String formatShortDateTime(GregorianCalendar gc) {

        // cjd - there are date formats for Locale.  Please see those
        String s = formatDate(gc, "MM/dd h:mma");
        s = s.substring(0,s.length()-1);
        String t = s.substring(0,s.length()-1);
        s = t.concat(s.substring(s.length()-1).toLowerCase());
        return s;
    }

    /** formats time portion only of a GregorianCalendar instance: h:mma
     *
     * @param cal
     * @return
     */
    public static String formatTimeOnly(GregorianCalendar gc) {

        return formatDate(gc, "h:mma").toLowerCase();
    }


    /** convenience method - formats time and date of a long date
     * format (see Date().getTime().
     *
     * @param date - input date value in milliseconds.
     * @return - date/time in format: MM/dd/yy h:mma.
     */
    public static String formatShort(long date) {

        return formatShort(toGregorianCalendar(date));
    }

    /** formats short time and date of a GregorianCalendar instance
     *
     * @param cal - input GregorianCalendar instance
     * @return - date/time in format: MM/dd/yy h:mma.
     */
    public static String formatShort(GregorianCalendar cal) {

        // cjd - there are date formats for Locale.  Please see those
        return formatDate(cal, "MM/dd/yy h:mma").toLowerCase();
    }

    /** formats a date
     * TODO: only tested "MM/dd/yy" format - need to test others.
     *
     * @param gc - GregorianCalendar instance
     * @param format - format to be returned.
     * @return - string formated in the "format" parm format.
     */
    public static String formatDate(GregorianCalendar gc, final String format) {
        // TODO: only tested "MM/dd/yy" format - need to test others.

        Date date = gc.getTime();
        DateFormat format1 = new SimpleDateFormat( format );
        return format1.format(date);
    }

    /** returns GregorianCalendar from a MM/dd/yy field.
     *
     * @param s - string date format in DDMMYYYY format
     * @return null if null input or invalid input, or GregorianCalendar.
     */
    public static GregorianCalendar toCalFromDD_MMM_YYYY(final String s) {

        if (s ==null) return null;
        int date = Integer.valueOf(s.substring(0,2));
        int month = toGregorianCalendarMonthNo(s.substring(3,6));
        int year = toYYYY(Integer.valueOf(s.substring(7,9)));

        GregorianCalendar gc = new GregorianCalendar();
        gc.clear(); // clear time (and date)
        gc.set(year, month, date);
        // note: gc will not update until you do a "get" even after it is "set. So, must do a "get" prior to use.
        gc.get(Calendar.DAY_OF_MONTH);
        return gc;
    }

    /** takes in a 3-long month name string and returns a GregorianCalendar month number. Note:
     * GregorianCalendar Month numbers are zero-based.
     *
     * @param month - 3 char month string
     * @return month number or 0 if not found
     */
    public static int toGregorianCalendarMonthNo(final String _month) {

        // ****** NOTE: MONTH NUMBERS IN GREGORIAN DATES ARE ZERO-BASED Calendar.January=0, not 1 **********
        String month = _month.toUpperCase();
        int monthNo = -1;
        if      (month.equals("JAN")) monthNo = Calendar.JANUARY;
        else if (month.equals("FEB")) monthNo = Calendar.FEBRUARY;
        else if (month.equals("MAR")) monthNo = Calendar.MARCH;
        else if (month.equals("APR")) monthNo = Calendar.APRIL;
        else if (month.equals("MAY")) monthNo = Calendar.MAY;
        else if (month.equals("JUN")) monthNo = Calendar.JUNE;
        else if (month.equals("JUL")) monthNo = Calendar.JULY;
        else if (month.equals("AUG")) monthNo = Calendar.AUGUST;
        else if (month.equals("SEP")) monthNo = Calendar.SEPTEMBER;
        else if (month.equals("OCT")) monthNo = Calendar.OCTOBER;
        else if (month.equals("NOV")) monthNo = Calendar.NOVEMBER;
        else if (month.equals("DEC")) monthNo = Calendar.DECEMBER;
        return monthNo;
    }

    /** converts 2-digit year to 4 digit.
     * @param year2Digig - 2 digit year
     * @return 4-digit year
     */
    public static int toYYYY(int year2Digit) {

        if (year2Digit > 99) return 0;
        if (year2Digit <70) return 2000+year2Digit;
        return 1900+year2Digit;
    }

    /** returns the time difference from the current time and the passed time
     *
     * @param olderTime - the old time in the last
     * @return - the elapsed time from the time passed
     */
    public static long elapsedTimeInMins(long olderTime) {

        return ((new Date().getTime() - olderTime)/(60*1000));
    }

    /** getter - date format string based on system date format set by the user
     *
     * @param context Context
     * @return the date format string based on the system date format set by the user.
     */
    public static String getSystemDateFormat(Context context) {

        String dateFormat = Settings.System.getString(context.getContentResolver(), Settings.System.DATE_FORMAT);

        if(dateFormat == null || dateFormat.length() == 0) {
            dateFormat = DEFAULT_DATE_FORMAT;
        }

        return dateFormat;
    }

    /** getter - time format string based on system time format of 12 or 24
     *
     * @param context Context
     * @return the time format string based on the system time format set by the user.
     */
    public static String getSystemTimeFormat(Context context) {

        String timeFormat = Settings.System.getString(context.getContentResolver(), Settings.System.TIME_12_24);

        if(timeFormat != null && timeFormat.contains("24")) {
            timeFormat = TIME_FORMAT_24HR;
        }
        else {
            timeFormat = TIME_FORMAT_12HR;
        }
        return timeFormat;
    }
}
