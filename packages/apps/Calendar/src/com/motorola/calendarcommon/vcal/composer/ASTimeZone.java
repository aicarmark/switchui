/*
 * Copyright (C) 2008, Motorola, Inc,
 * All Rights Reserved
 * Class name: ASTimeZone.java
 * Description: Please see the class comment.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 13-Feb-2009    e12128       Create header
 **********************************************************
 */

package com.motorola.calendarcommon.vcal.composer;


import android.util.Log;
import android.util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * This class represents the TimeZone field of an ActiveSync Calendar object. It supports the serialization and
 * deserialization into the Base64 format the EAS uses to transmit time zones in the protocol.
 * @hide
 */
public class ASTimeZone {

    // The length of the name fields. Note that these seems to not be set in the time zones return from EAS.
    private static final int NAME_LEN = 32;

    private static final String TAG = "ASTimeZone";
    private static final boolean DEVELOPMENT = Config.DEBUG;
    private static final boolean DUMP = DEVELOPMENT & false;

    private static final String DEFAULT_TIMEZONE = "Etc/UTC";

    //Time conversion factors
    private static final int MS_PER_SEC = 1000;
    private static final int SEC_PER_MIN = 60;
    private static final int MS_PER_MIN = MS_PER_SEC * SEC_PER_MIN;
    private static final int MIN_PER_HR = 60;
    private static final long MS_PER_HOUR = MIN_PER_HR * MS_PER_MIN;
    private static final long MS_PER_DAY = MS_PER_HOUR * 24;

    //ActiveSync time zone limitations
    private static final int MAX_CHARS_NAME = 32;

    /**
     * Convert Java time zone ID to approximate ActiveSync time zone.
     * @param timezone Java time zone ID
     * @param year The year we're interested in - used for daylight time calculation.
     * @return Approximate ActiveSync time zone, null if no match found, or parameter is null
     */
    public static ASTimeZone newFromTimeZone(TimeZone timezone, int year) {
        ASTimeZone asTimezone = null;
        if (timezone != null) {

            //Java time zone values in milliseconds ADDED to UTC
            int jBiasMs = timezone.getRawOffset();
            int jStdOffsetMs = 0;
            int jDstOffsetMs = timezone.getDSTSavings();
            //ActiveSync time zone values in minutes SUBTRACTED from UTC
            int aBiasMin = -convertMsToMin(jBiasMs);
            int aStdOffsetMin = -convertMsToMin(jStdOffsetMs);
            int aDstOffsetMin = -convertMsToMin(jDstOffsetMs);

            SystemTime stdDate = getStandardTime(timezone, year);
            if (stdDate == null) {
                // No standard time transition date so use all zeroes.
                stdDate = new SystemTime(0, 0, 0, 0, 0);
                aDstOffsetMin = 0;
            }
            SystemTime dstDate = getDaylightTime(timezone, year);
            if (dstDate == null) {
                // No daylight time transition date so use all zeroes.
                dstDate = new SystemTime(0, 0, 0, 0, 0);
                aDstOffsetMin = 0;
            }
            //Time zone names in long format
            String standardName = timezone.getDisplayName(false, TimeZone.LONG);
            String daylightName = timezone.getDisplayName(true, TimeZone.LONG);

            if (standardName.length() > MAX_CHARS_NAME) {
                standardName = standardName.substring(0, MAX_CHARS_NAME);
            }
            if (daylightName.length() > MAX_CHARS_NAME) {
                daylightName = daylightName.substring(0, MAX_CHARS_NAME);
            }
            asTimezone = new ASTimeZone(aBiasMin, standardName, stdDate, aStdOffsetMin,
                    daylightName, dstDate, aDstOffsetMin);

            if (DEVELOPMENT) { //added by LogFinder
                Log.v(TAG, "Java time zone " + timezone + " conversion to ActiveSync: " +
                        asTimezone);
            } //added by LogFinder

        }
        return asTimezone;
    }

    /**
     * Create an instance from the given input stream.
     * @param is A stream onto the packed binary representation of the Time Zone.
     * @return An instance populated from the fields read from the stream.
     * @throws IOException If something goes wrong.
     */
    public static ASTimeZone read(InputStream is) throws IOException {
        int bias = readInt(is);

        String stdName = readString(is, NAME_LEN);
        SystemTime stdDate = SystemTime.read(is);
        int stdBias = readInt(is);

        String dstName = readString(is, NAME_LEN);
        SystemTime dstDate = SystemTime.read(is);
        int dstBias = readInt(is);

        return new ASTimeZone(bias, stdName, stdDate, stdBias, dstName, dstDate, dstBias);
    }

    /**
     * Writes the current instance to the given stream. It is written as a packed
     * C struct format as defined in the EAS documentations. For transmission it should
     * be Base64 encoded.
     * @param os The stream to write to.
     * @throws IOException If something goes wrong with the I/O.
     */
    public void write(OutputStream os) throws IOException {
        writeInt(os, getBias());

        writeString(os, getStandardName(), NAME_LEN);
        getStandardDate().write(os);
        writeInt(os, getStandardBias());

        writeString(os, getDaylightName(), NAME_LEN);
        getDaylightDate().write(os);
        writeInt(os, getDaylightBias());

    }

    /**
     * Construct an instance from the complete set of fields for an ASTimeZone.
     * @param bias The standard bias from UTC in minutes. E.g. for GMT-8 the bias is 480.
     * @param standardName The name of the time zone not in daylight time.
     * @param standardDate The date at which the time zone reverts to standard time.
     * @param standardBias The difference between the bias param and the bias for standard time. Usually 0.
     * @param daylightName The name of the time zone in daylight time.
     * @param daylightDate The date at which the time zone changes to daylight time.
     * @param daylightBias The difference between the bias param and the bias for daylight time. Usually -60.
     */
    public ASTimeZone(int bias, String standardName, SystemTime standardDate, int standardBias,
            String daylightName, SystemTime daylightDate, int daylightBias) {
        super();
        mBias = bias;
        mStandardName = standardName;
        mStandardDate = standardDate;
        mStandardBias = standardBias;
        mDaylightName = daylightName;
        mDaylightDate = daylightDate;
        mDaylightBias = daylightBias;
    }

    @Override
    /**
     * Output the Time Zone data for debugging purposes. Here's a typical output, formatted for readability:
     * <pre>
     * ASTimeZone: [
     *    Bias: 480
     *    StandardName: ""
     *    StandardDate:
     *        SystemTime: [
     *             Year: 0
     *             Month: 10
     *             DayOfWeek: 0 // 0-Sun
     *             Day: 5 // n'th DayOfWeek of the month 1-4, 5 means last
     *             Hour: 2
     *             Minute: 0
     *             Second: 0
     *             Milliseconds: 0
     *         ]
     *    StandardBias: 0
     *    DaylightName: ""
     *    DaylightDate:
     *        SystemTime: [
     *             Year: 0
     *             Month: 4
     *             DayOfWeek: 0
     *             Day: 1
     *             Hour: 2
     *             Minute: 0
     *             Second: 0
     *             Milliseconds: 0
     *         ]
     *    DaylightBias: -60
     * ]
     * </pre>
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(200);
        sb.append("ASTimeZone: [").append("Bias: ").append(getBias()).append(" StandardName: ").append(
                '"').append(getStandardName()).append('"').append(" StandardDate: ").append(
                getStandardDate()).append(" StandardBias: ").append(getStandardBias()).

        append(" DaylightName: ").append('"').append(getDaylightName()).append('"').append(
                " DaylightDate: ").append(getDaylightDate()).append(" DaylightBias: ").append(
                getDaylightBias()).append(']');

        return sb.toString();
    }

    /**
     * Convert ActiveSync time zone to an approximate Java time zone ID.
     * @param year The year for which the timezone is to be matched.
     * @return Approximate Java time zone ID, or DEFAULT_TIMEZONE_JAVA if no match found
     */
    public String toJavaId(int year) {
        String tzid = null;
        //ActiveSync values in minutes SUBTRACTED from UTC
        int aBiasMin = getBias();
        int aStdOffsetMin = getStandardBias();
        int adDstOffsetMin = getDaylightBias();
        //Java values in milliseconds ADDED to UTC
        int stdBias = -convertMinToMs(aBiasMin);
        int jStdOffsetMs = -convertMinToMs(aStdOffsetMin);
        int jDstOffsetMs = -convertMinToMs(adDstOffsetMin);

        //Include bias adjustment for lack of Java support for standard time offsets
        String[] timezones = TimeZone.getAvailableIDs(stdBias + jStdOffsetMs);
        if (timezones.length == 0 && jStdOffsetMs != 0) {
            //If no matches found yet, use bias without standard time offset
            timezones = TimeZone.getAvailableIDs(stdBias);
        }

        SystemTime daylightDate = getDaylightDate();
        // A zero month means no DST.
        int daylightMonth = daylightDate.getMonth();
        if (daylightMonth == 0) {
            // No DST in this EAS timezone, so look for one that doesn't have DST
            for (String id : timezones) {
                TimeZone tz = TimeZone.getTimeZone(id);
                if (DUMP) {
                    Log.v(TAG, "id=" + id);
                }
                // No DST in this timezone, so we'll take it.
                if (tz.getDSTSavings() == 0) {
                    tzid = id;
                    break;
                }
            }
        } else {
            // Otherwise look for a timezone whose offsets at key dates match our one.
            // We test the day before and the day after the transition dates given in the EAS timezone
            long preDstDate = daylightDate.toActualDateMs(year) - MS_PER_DAY;
            long postDstDate = preDstDate + 2 * MS_PER_DAY;
            long preStdDate = getStandardDate().toActualDateMs(year) - MS_PER_DAY;
            long postStdDate = preStdDate + 2 * MS_PER_DAY;
            if (DUMP) {
                Log.v(TAG, "preDst=" + new Date(preDstDate) + "\npstDst=" + new Date(postDstDate) +
                        "\npreStd=" + new Date(preStdDate) + "\npstStd=" + new Date(postStdDate));
            }
            int dstBias = stdBias + jDstOffsetMs;

            // If we can't find an exact match for the dates in the EAS timezone info,
            // try to find one with the same offsets and hemisphere at least.
            // For northern hemisphere, use June 1 as the trial daylight date, for southern use Jan 1.
            int daylightSampleMonth = daylightMonth > 6 ? Calendar.JANUARY : Calendar.JUNE;
            Calendar cal = Calendar.getInstance();
            cal.set(year, daylightSampleMonth, 1);
            long daylightTrialDate = cal.getTimeInMillis();
            String fallBackId = null;

            // Loop through the timezones with the matching base offset and try to find one
            // that transitions to and from DST at the right time. We do this by comparing the
            // timezone's offsets at our 4 test dates with the biases we get from the EAS info.
            for (String id : timezones) {
                TimeZone tz = TimeZone.getTimeZone(id);
                if (DUMP) {
                    Log.v(TAG, "id=" + id);
                }
                if (tz.getOffset(preDstDate) == stdBias && tz.getOffset(postDstDate) == dstBias
                        && tz.getOffset(preStdDate) == dstBias
                        && tz.getOffset(postStdDate) == stdBias) {
                    Log.i(TAG, "Got an exact match for timezone info.");
                    tzid = id;
                    break;
                }
                if (fallBackId == null) {
                    // If we find one that has the correct DST and STD offsets but not at the
                    // correct transition dates, record that as a possible fallback in case
                    // we don't find an exact match.
                    if (tz.getOffset(daylightTrialDate) == dstBias) {
                        fallBackId = id;
                    }
                }
            }
            if (tzid == null && fallBackId != null) {
                Log.w(TAG,
                        "Falling back on timezone with correct offsets but wrong transition dates.");
                tzid = fallBackId;
            }
        }

        if (tzid == null) {
            if (timezones.length > 0) {
                Log.w(TAG,
                        "Falling back on timezone with correct base offset but wrong DST offset");
                tzid = timezones[0];
            } else {
                Log.w(TAG, "Falling back on the default timezone. Bad.");
                tzid = DEFAULT_TIMEZONE;
            }
        }

        if (DEVELOPMENT) { //added by LogFinder
            Log.v(TAG, "Converted ActiveSync to Java time zone ID: " + tzid);
        } //added by LogFinder

        return tzid;
    }

    /**
     * Returns a SystemTime that represents the time that daylight saving time starts for
     * the given timezone in the given year.
     * @param tz The TimeZone to use.
     * @param year The year to use.
     * @return The SystemTime of the transition, or null if there is no daylight time.
     */
    private static SystemTime getDaylightTime(TimeZone tz, int year) {
        Log.i(TAG, "getDaylight");
        boolean northernHemisphere = guessHemisphere(tz, year);
        Log.i(TAG, "northern: " + String.valueOf(northernHemisphere));
        Calendar cal = getDaylightDate(tz, year, northernHemisphere);
        if (cal == null) {
            Log.i(TAG, "didn't get cal");
            cal = getDaylightDate(tz, year, !northernHemisphere);
        }
        return getSystemTime(cal, -1);
    }

    /**
     * Returns a SystemTime that represents the time that daylight saving time ends for
     * the given timezone in the given year.
     * @param tz The TimeZone to use.
     * @param year The year to use.
     * @return The SystemTime of the transition, or null if there is no daylight time.
     */
    private static SystemTime getStandardTime(TimeZone tz, int year) {
        Log.i(TAG, "getStandard");
        boolean northernHemisphere = guessHemisphere(tz, year);
        Log.i(TAG, "northern: " + String.valueOf(northernHemisphere));
        Calendar cal = getStandardDate(tz, year, northernHemisphere);
        if (cal == null) {
            Log.i(TAG, "didn't get cal");
            cal = getStandardDate(tz, year, !northernHemisphere);
        }
        return getSystemTime(cal, 1);
    }

    /**
     * Returns true of we suspect the given TimeZone represents a place in the northern hemisphere.
     * @param tz The TimeZone to check.
     * @param year The year of interest.
     * @return true if we think this is a Northern Hemispehre Time Zone, else false.
     */
    private static boolean guessHemisphere(TimeZone tz, int year) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.set(year, Calendar.JULY, 1);
        return tz.inDaylightTime(cal.getTime());
    }

    /**
     * Converts the time represented by the Calendar to a SystemTime, with the
     * given offset applied to the hour.
     * @param cal The Calendar to convert.
     * @param hourOffset An offset to add to the hours of day in the Calendar.
     * @return The resultant SystemTime, or null if the Calendar argument is null.
     */
    private static SystemTime getSystemTime(Calendar cal, int hourOffset) {
        if (DEVELOPMENT) {
            Log.v(TAG, "getSystemTime offset: " + String.valueOf(hourOffset) + " cal: " + cal);
        }

        if (cal == null) {
            return null;
        }

        // Make the month base-1.
        int month = cal.get(Calendar.MONTH) + 1;
        // Sunday = 0; this calculation makes no assumptions about the value of Calendar.SUNDAY.
        int dayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7;

        // Day of the month is used to calculate the nth <day of week> of the month.
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

        // Apply the correction to the hour.
        int hour = cal.get(Calendar.HOUR_OF_DAY) + hourOffset;
        if (hour >= 24) {
            // This happens if the clocks go back at 00:00. The Calendar will come in as 23:00 and
            // the offset as +1. In this case we need to fix the time and bump the date.
            hour -= 24;
            dayOfWeek = (dayOfWeek + 1) % 7;
            dayOfMonth++;
        } else if (hour < 0) {
            // This could only happen if the clocks go forward at 23:00. In this case
            // the calendar would be 00:00 and the offset -1.
            hour += 24;
            dayOfWeek = (dayOfWeek + 6) % 7;
            dayOfMonth--;
            if (dayOfMonth == 0) {
                // For the purposes of calculating nthOfMonth, force it to be the "last <day of week> of month" case.
                dayOfMonth = 28;
            }
        }

        int nthOfMonth = (dayOfMonth - 1) / 7 + 1;
        // ActiveSync uses 5 to mean "last <day of week> of the month"
        if (nthOfMonth >= 4) {
            nthOfMonth = 5;
        }

        int minute = cal.get(Calendar.MINUTE);
        return new SystemTime(month, dayOfWeek, nthOfMonth, hour, minute);
    }

    /**
     * Returns a Calendar that represents the moment of transition from standard to
     * daylight saving time for the timezone and year given. Note that the hour will be the
     * local time of the moment of transition, so if daylight time starts at 2AM, the hour will
     * be 3AM.
     * @param tz The TimeZone to use.
     * @param year The year whose transition time we want.
     * @param northernHemisphere true to assume the TimeZone is in the Northern Hemisphere, false otherwise
     * @return The Calendar giving the moment of transition, or null if there is no DST in the given TimeZone.
     */
    private static Calendar getDaylightDate(TimeZone tz, int year, boolean northernHemisphere) {
        Calendar start = Calendar.getInstance(tz);

        // Do a binary search in the appropriate half of the year looking for the transition.
        start.clear();
        start.set(year, northernHemisphere ? Calendar.JANUARY : Calendar.JULY, 1);
        Date startTime = start.getTime();
        long startMs = startTime.getTime();
        boolean startIn = tz.inDaylightTime(startTime);

        Calendar end = Calendar.getInstance(tz);
        end.clear();
        end.set(year, northernHemisphere ? Calendar.JUNE : Calendar.DECEMBER, 31);
        Date endTime = end.getTime();
        long endMs = endTime.getTime();
        boolean endIn = tz.inDaylightTime(endTime);

        // If there's no difference between the extremes, or the end date isn't in DST give up
        if (startIn == endIn || !endIn) {
            return null;
        }

        // Date for testing the in-ness of the current candidate time.
        Date midTime = new Date();
        // When the times are 90 minutes apart, terminate
        final long threhsoldMs = (60 + 30) * 60 * 1000;
        while ((endMs - startMs) > threhsoldMs) {
            // Find the point halfway between the current extremes
            long midMs = (startTime.getTime() + endTime.getTime()) / 2;
            midTime.setTime(midMs);
            // Depending of if it's in or out, shift one of the end points.
            if (tz.inDaylightTime(midTime)) {
                endTime.setTime(midMs);
                endMs = midMs;
            } else {
                startTime.setTime(midMs);
                startMs = midMs;
            }
        }
        // Round the start time to the nearest hour
        startMs += MS_PER_HOUR - 1;
        startMs = (startMs / MS_PER_HOUR) * MS_PER_HOUR;

        // And that the value we'll return.
        start.setTimeInMillis(startMs);
        return start;
    }

    /**
     * Returns a Calendar that represents the moment of transition from daylight to
     * standard saving time for the timezone and year given. Note that the hour will be the
     * local time of the moment of transition, so if daylight time ends at 2AM, the hour will
     * be 1AM.
     * @param tz The TimeZone to use.
     * @param year The year whose transition time we want.
     * @param northernHemisphere true to assume the TimeZone is in the Northern Hemisphere, false otherwise
     * @return The Calendar giving the moment of transition, or null if there is no DST in the given TimeZone.
     */
    private static Calendar getStandardDate(TimeZone tz, int year, boolean northernHemisphere) {
        Calendar start = Calendar.getInstance(tz);
        start.clear();
        start.set(year, northernHemisphere ? Calendar.JULY : Calendar.JANUARY, 1);

        Calendar end = Calendar.getInstance(tz);
        end.clear();
        end.set(year, northernHemisphere ? Calendar.DECEMBER : Calendar.JUNE, 30);

        Date startTime = start.getTime();
        long startMs = startTime.getTime();
        boolean startIn = tz.inDaylightTime(startTime);
        Date endTime = end.getTime();
        long endMs = endTime.getTime();
        boolean endIn = tz.inDaylightTime(endTime);

        if (DEVELOPMENT) {
            Log.v(TAG, "start: " + startTime);
            Log.v(TAG, "end: " + endTime);
            Log.v(TAG, "startIn: " + String.valueOf(startIn) + " endIn: " + String.valueOf(endIn));
        }
        if (startIn == endIn || !startIn) {
            return null;
        }
        final long threhsoldMs = 59 * 60 * 1000;
        Date midTime = new Date();
        while ((endMs - startMs) > threhsoldMs) {
            long midMs = (startTime.getTime() + endTime.getTime()) / 2;
            midTime.setTime(midMs);
            if (!tz.inDaylightTime(midTime)) {
                endTime.setTime(midMs);
                endMs = midMs;
            } else {
                startTime.setTime(midMs);
                startMs = midMs;
            }
        }
        startMs += MS_PER_HOUR - 1;
        startMs = (startMs / MS_PER_HOUR) * MS_PER_HOUR;
        start.setTimeInMillis(startMs);
        return start;
    }

    /**
     * Convert minutes to milliseconds.
     * @param min Minutes
     * @return Equivalent milliseconds
     */
    private static int convertMinToMs(int min) {
        return min * MS_PER_MIN;
    }

    /**
     * Convert milliseconds to minutes, rounded down to the nearest whole minute.
     * @param ms Milliseconds
     * @return Whole minutes
     */
    private static int convertMsToMin(int ms) {
        return ms / MS_PER_MIN;
    }

    /**
     * This class represents a Window SYSTEMTIME struct, which is a component of a Time Zone.
     * It is used to represent the date and time of transitions between Standard and Daylight time.
     * As such, only the month, day and hour fields are typically used.
     * See {@linkplain http://msdn.microsoft.com/en-us/library/ms724950(VS.85).aspx}.<br>
     * Note: when used in ActiveSync timezones, the fields are used as follows:<br><br>
     * Year: not used.<br>
     * Month: month number of transition.<br>
     * Day of week: day of the week of the transition.<br>
     * Day: the n<sup>th</sup> Sunday number of the transition. 1-4, 5 = "last".<br>
     * Hour: the hour of the transition.<br>
     * Minute: the minute of the transition.<br>
     */
    public static class SystemTime {

        /**
         * Creates an instance from the given input stream. The data is encoded as a
         * sequence of little-endian two-byte integers.
         * @param is The input stream to read from.
         * @return An instance initialized from the data in the stream.
         * @throws IOException Id something goes wrong with the I/O.
         */
        public static SystemTime read(InputStream is) throws IOException {
            SystemTime result = new SystemTime();

            result.setYear(readShort(is));
            result.setMonth(readShort(is));
            result.setDayOfWeek(readShort(is));
            result.setDay(readShort(is));
            result.setHour(readShort(is));
            result.setMinute(readShort(is));
            result.setSecond(readShort(is));
            result.setMilliseconds(readShort(is));

            return result;
        }

        /**
         * Writes this instance to the given output stream. The data is encoded as a
         * sequence of little-endian two-byte integers.
         * @param is The output stream to write to.
         * @throws IOException Id something goes wrong with the I/O.
         */
        public void write(OutputStream os) throws IOException {
            writeShort(os, getYear());
            writeShort(os, getMonth());
            writeShort(os, getDayOfWeek());
            writeShort(os, getDay());
            writeShort(os, getHour());
            writeShort(os, getMinute());
            writeShort(os, getSecond());
            writeShort(os, getMilliseconds());
        }

        /**
         * Constructs an instance from the fields that are important to ASTimeZones.
         * @param month The month of the transition, 1-12.
         * @param dayOfWeek The day of week of the transition, 0=Sunday.
         * @param day The day of the month of the transition, 1-31.
         * @param hour The hour of the transition, 0-23.
         * @param minute The minute of the transition, 0-59.
         */
        public SystemTime(int month, int dayOfWeek, int day, int hour, int minute) {
            super();
            mMonth = month;
            mDayOfWeek = dayOfWeek;
            mDay = day;
            mHour = hour;
            mMinute = minute;
        }

        /**
         * ActiveSync uses a value of all zeroes to represent the transition date/time
         * between daylight and standard time for timezones that don't have DST.
         * This method returns true if the SystemTime instance looks as thought it contains
         * a valid date/time, or false if it looks like an all-zeroes one.
         * @return true for a real date/time, false for the all-zeroes value used to
         * represent an non-DST timezone's transition date.
         */
        public boolean isValid() {
            return getMonth() > 0 && getDay() > 0;
        }


        @Override
        /**
         * Return a string representation of the instance for debugging.
         */
        public String toString() {
            StringBuilder sb = new StringBuilder(200);
            sb.append("SystemTime: [").append("Year: ").append(getYear()).append(" Month: ").append(
                    getMonth()).append(" DayOfWeek: ").append(getDayOfWeek()).append(" Day: ").append(
                    getDay()).append(" Hour: ").append(getHour()).append(" Minute: ").append(
                    getMinute()).append(" Second: ").append(getSecond()).append(" Milliseconds: ").append(
                    getMilliseconds()).append(']');

            return sb.toString();
        }

        // Auto-generated getters and setters.
        public int getYear() {
            return mYear;
        }

        public void setYear(int year) {
            mYear = year;
        }

        public int getMonth() {
            return mMonth;
        }

        public void setMonth(int month) {
            mMonth = month;
        }

        public int getDayOfWeek() {
            return mDayOfWeek;
        }

        /**
         * Converts the "nth weekday of month" into an actual date for the given year.
         * @param year The year we want to get the concrete date for.
         * @return The date that the abstract description in this instance refers
         * to for the given year.
         */
        public long toActualDateMs(int year) {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            // Calendar uses base-0 months, MS uses base-1.
            cal.set(year, getMonth() - 1, 1, getHour(), getMinute(), getSecond());
            // Calendar uses 1 for SUNDAY, MS uses 0
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY + getDayOfWeek());
            int weekOfMonth = getDay();
            // MS uses 5 for "last of month", Calendar uses -1.
            if (weekOfMonth == 5) {
                weekOfMonth = -1;
            }
            cal.set(Calendar.DAY_OF_WEEK_IN_MONTH, weekOfMonth);
            return cal.getTimeInMillis();
        }

        private static final String[] WEEKDAYS = {
                "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        };

        public String getDayOfWeekStr() {
            return WEEKDAYS[mDayOfWeek];
        }

        public void setDayOfWeek(int dayOfWeek) {
            mDayOfWeek = dayOfWeek;
        }

        public int getDay() {
            return mDay;
        }

        public void setDay(int day) {
            mDay = day;
        }

        public int getHour() {
            return mHour;
        }

        public String getTimeStr() {
            return twoDigits(mHour) + ":" + twoDigits(mMinute) + ":" + twoDigits(mSecond);
        }

        private String twoDigits(int val) {
            return val < 10 ? "0" + String.valueOf(val) : String.valueOf(val);
        }

        public void setHour(int hour) {
            mHour = hour;
        }

        public int getMinute() {
            return mMinute;
        }

        public void setMinute(int minute) {
            mMinute = minute;
        }

        public int getSecond() {
            return mSecond;
        }

        public void setSecond(int second) {
            mSecond = second;
        }

        public int getMilliseconds() {
            return mMilliseconds;
        }

        public void setMilliseconds(int milliseconds) {
            mMilliseconds = milliseconds;
        }

        private SystemTime() {
            // For internal use.
        }

        private int mYear;
        private int mMonth; // 1-12
        private int mDayOfWeek; // 0-6 (Sunday is 0)
        private int mDay; // 1-31
        private int mHour; // 0-23
        private int mMinute; // 0-59
        private int mSecond; // 0-59
        private int mMilliseconds; // 0-999

    }

    // Data I/O. Note that we can't use Data[Input|Output]Stream as they use big-endian
    // encodings and we we need x86/ARM-style little-endian.

    /**
     * Returns the short read from the next two bytes of the stream.
     * @param is The input stream to use.
     * @return The short read from the first (LSB) and second (MSB) bytes from the stream, as an int.
     * @throws IOException
     */
    protected static int readShort(InputStream is) throws IOException {
        int b0 = is.read();
        int b1 = is.read();
        if (b1 == -1 || b0 == -1) {
            throw new IllegalStateException("eof");
        }
        return (short)(b0 | (b1 << 8));
    }

    /**
     * Writes the given int to the next two bytes of the stream.
     * @param os The output stream to use.
     * @param val The short to write to the stream in the order LSB, MSB.
     * @throws IOException
     */
    protected static void writeShort(OutputStream os, int val) throws IOException {
        os.write(val);
        os.write(val >> 8);
    }

    /**
     * Returns the int read from the next four bytes of the stream.
     * @param is The input stream to use.
     * @return The int read from the next four bytes, from least to most significant byte.
     * @throws IOException
     */
    protected static int readInt(InputStream is) throws IOException {
        int b0 = is.read();
        int b1 = is.read();
        int b2 = is.read();
        int b3 = is.read();
        if (b3 == -1 || b2 == -1 || b1 == -1 || b0 == -1) {
            throw new IllegalStateException("eof");
        }
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    /**
     * Writes the given int to the next four bytes of the stream.
     * @param os The output stream to use.
     * @param val The short to write to the stream in the order least to most significant byte.
     * @throws IOException
     */
    protected static void writeInt(OutputStream os, int val) throws IOException {
        os.write(val);
        os.write(val >> 8);
        os.write(val >> 16);
        os.write(val >> 24);
    }

    /**
     * Reads a string of Unicode characters from the stream, up to len characters long. A 0 character
     * terminates the string, but exactly len characters are always read.
     * @param is The stream to read from.
     * @param len The number of characters to read.
     * @return The String represented by the encoded characters.
     * @throws IOException
     */
    protected static String readString(InputStream is, int len) throws IOException {
        StringBuilder sb = new StringBuilder(len);
        boolean ended = false;
        for (int i = 0; i < len; i++) {
            int s = readShort(is);
            if (!ended) {
                if (s == 0) {
                    ended = true;
                } else {
                    sb.append((char)s);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Writes the given string to the stream as exactly len two-byte Unicode characters. If the
     * string is shorter than len, the output is padded with 0s. If it is longer, then it is
     * truncated.
     * @param os The stream to write to.
     * @param str The string to write.
     * @param len The exact number of characters to write, regardless of the length of str.
     * @throws IOException
     */
    protected static void writeString(OutputStream os, String str, int len) throws IOException {
        int strLen = str.length();
        int count = Math.min(strLen, len);
        for (int i = 0; i < count; i++) {
            writeShort(os, (short)str.charAt(i));
        }

        for (int i = count; i < len; i++) {
            writeShort(os, 0);
        }
    }

    public int getBias() {
        return mBias;
    }

    public void setBias(int bias) {
        mBias = bias;
    }

    public String getStandardName() {
        return mStandardName;
    }

    public void setStandardName(String standardName) {
        mStandardName = standardName;
    }

    public SystemTime getStandardDate() {
        return mStandardDate;
    }

    public void setStandardDate(SystemTime standardDate) {
        mStandardDate = standardDate;
    }

    public int getStandardBias() {
        return mStandardBias;
    }

    public void setStandardBias(int standardBias) {
        mStandardBias = standardBias;
    }

    public String getDaylightName() {
        return mDaylightName;
    }

    public void setDaylightName(String daylightName) {
        mDaylightName = daylightName;
    }

    public SystemTime getDaylightDate() {
        return mDaylightDate;
    }

    public void setDaylightDate(SystemTime daylightDate) {
        mDaylightDate = daylightDate;
    }

    public int getDaylightBias() {
        return mDaylightBias;
    }

    public void setDaylightBias(int daylightBias) {
        mDaylightBias = daylightBias;
    }

    // The number of minutes to add to the local time to get UTC. For example, the
    // bias for Pacific Standard Time (GMT-8) is 480.
    private int mBias;

    // The name of the time zone in standard time, e.g, "Pacific Standard Time"
    private String mStandardName;

    // The date and time that the time zone reverts to standard time, e.g. for the US
    // it will be 2AM on the first Sunday in November.
    private SystemTime mStandardDate;

    // The number of minutes to add to mBias to account for standard time. Typically 0.
    private int mStandardBias;

    // The name of the time zone in standard time, e.g, "Pacific Daylight Time"
    private String mDaylightName;

    // The date and time that the time zone transitions to daylight time, e.g. for the US
    // it will be 2AM on the second Sunday in March.
    private SystemTime mDaylightDate;

    // The number of minutes to add to mBias to account for standard time. Typically -60 for
    // time zones that put the clocks forward by an hour for daylight time.
    private int mDaylightBias;

}
