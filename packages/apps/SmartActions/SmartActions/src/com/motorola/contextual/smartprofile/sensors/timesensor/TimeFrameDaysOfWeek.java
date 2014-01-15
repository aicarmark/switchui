/*
 * @(#)TimeFrameDaysOfWeek.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a15776       2010/02/21   NA               Incorporated review comments
 * a15776       2011/02/01   NA               Initial Version
 *
 */

package com.motorola.contextual.smartprofile.sensors.timesensor;

import com.motorola.contextual.smartrules.R;

import android.content.Context;

import java.text.DateFormatSymbols;
import java.util.Calendar;

/**
 * Utility class to manipulate the days of week in the form of a bit mask
 * <code><pre>
 *
 *  CLASS:
 *   Implements {@link TimeFrameConstants}
 *
 *  RESPONSIBILITIES:
 *   1. Provides helper methods to get the days of the week given a bitmap.
 *   2. Provides helper method to convert day string in Short format to long format and vice versa
 *
 *  COLLABORATORS:
 *   None
 *
 * </pre></code>
 *
 */
public class TimeFrameDaysOfWeek implements TimeFrameConstants {

    private static String NO_DAY_STRING = "";

    private static int[] DAY_MAP = new int[] {
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY,
    };

    // Bit-mask of all repeating days
    private int mDays;

    //default constructor
    TimeFrameDaysOfWeek() {
        mDays = NODAY;
    }

    //constructor to take bit-mask
    public TimeFrameDaysOfWeek(int days) {
        mDays = days;
    }

    // constructor to take comma separated list
    public TimeFrameDaysOfWeek(String days) {
        String day[] = days.split(SHORT_DAY_SEPARATOR);
    
        String[] shortWeekDays = new DateFormatSymbols().getShortWeekdays();
        String[] longWeekDays  = new DateFormatSymbols().getWeekdays();

        String[] arrayOfShortWeekdays = new String[] {
            shortWeekDays[Calendar.MONDAY],
            shortWeekDays[Calendar.TUESDAY],
            shortWeekDays[Calendar.WEDNESDAY],
            shortWeekDays[Calendar.THURSDAY],
            shortWeekDays[Calendar.FRIDAY],
            shortWeekDays[Calendar.SATURDAY],
            shortWeekDays[Calendar.SUNDAY]
        };
        String[] arrayOfLongWeekdays = new String[] {
            longWeekDays[Calendar.MONDAY],
            longWeekDays[Calendar.TUESDAY],
            longWeekDays[Calendar.WEDNESDAY],
            longWeekDays[Calendar.THURSDAY],
            longWeekDays[Calendar.FRIDAY],
            longWeekDays[Calendar.SATURDAY],
            longWeekDays[Calendar.SUNDAY]
        };

        // iterate through each of the days and set the mask for that day
        for (int i = 0; i < day.length; i++) {
            // mShortWeekdays & mLongWeekdays are arrays of Strings and this comparison
            // is against a string and not one character, ruling out the chance to use
            // switch()
            if ((day[i].equalsIgnoreCase(arrayOfShortWeekdays[0]))
                    || (day[i].equalsIgnoreCase(arrayOfLongWeekdays[0]))) {
                mDays = mDays | MONDAY;
                continue;
            }
            else if ((day[i].equalsIgnoreCase(arrayOfShortWeekdays[1]))
                     || (day[i].equalsIgnoreCase(arrayOfLongWeekdays[1]))) {
                mDays = mDays | TUESDAY;
                continue;
            }
            else if ((day[i].equalsIgnoreCase(arrayOfShortWeekdays[2]))
                     || (day[i].equalsIgnoreCase(arrayOfLongWeekdays[2]))) {
                mDays = mDays | WEDNESDAY;
                continue;
            }
            else if ((day[i].equalsIgnoreCase(arrayOfShortWeekdays[3]))
                     || (day[i].equalsIgnoreCase(arrayOfLongWeekdays[3]))) {
                mDays = mDays | THURSDAY;
                continue;
            }
            else if ((day[i].equalsIgnoreCase(arrayOfShortWeekdays[4]))
                     || (day[i].equalsIgnoreCase(arrayOfLongWeekdays[4]))) {
                mDays = mDays | FRIDAY;
                continue;
            }
            else if ((day[i].equalsIgnoreCase(arrayOfShortWeekdays[5]))
                     || (day[i].equalsIgnoreCase(arrayOfLongWeekdays[5]))) {
                mDays = mDays | SATURDAY;
                continue;
            }
            else if ((day[i].equalsIgnoreCase(arrayOfShortWeekdays[6]))
                     || (day[i].equalsIgnoreCase(arrayOfLongWeekdays[6]))) {
                mDays = mDays | SUNDAY;
                continue;
            }
            else {
                mDays = ALLDAY;
            }
        }
    }

    /**
     * This method converts the bit mask to the corresponding comma separated string list
     * @param context - Context, Required to  get the "Never" string from the resource file
     * @param showNever - True if we need to show "Never" if no day has been selected
     * @return - String representation of the bit mask
     */
    public String toCommaSeparatedString(Context context, boolean showNever) {
        StringBuilder ret = new StringBuilder();
        // no days
        if (mDays == NODAY) {
            return showNever ? (context.getString(R.string.never)) : NO_DAY_STRING;
        }
        // every day
        if (mDays == ALLDAY) {
            return (context.getString(R.string.everyday));
        }

        // count selected days
        int dayCount = 0, days = mDays;
        while (days > 0) {
            if ((days & 1) == 1) dayCount++;
            days >>= 1;
        }
        // short or long form?
        DateFormatSymbols dfs = new DateFormatSymbols();

        String[] dayList = (dayCount > 1) ? dfs.getShortWeekdays() : dfs.getWeekdays();

        // selected days
        for (int i = 0; i < 7; i++) {
            if ((mDays & (1 << i)) != 0) {
                ret.append(dayList[DAY_MAP[i]]);
                dayCount -= 1;
                if (dayCount > 0) ret.append(SHORT_DAY_SEPARATOR);
            }
        }
        return ret.toString();
    }

    /**
     * Checks if a particular day is set
     * @param day - Day to check in the bit mask
     * @return - True if the day is set
     */
    private boolean isSet(int day) {
        return ((mDays & (1 << day)) > 0);
    }

    /**
     * Sets/Clears the day in the bit mask
     * @param day - Day to set/clear
     * @param set - boolean that determines if the days is to be set or cleared
     */
    public void set(int day, boolean set) {
        if (set) {
            mDays |= (1 << day);
        } else {
            mDays &= ~(1 << day);
        }
    }

    /**
     * Sets the bit mask with the given value
     * @param dow - Bit mask
     */
    public void set(TimeFrameDaysOfWeek dow) {
        mDays = dow.mDays;
    }

    /**
     * Extracts the bit mask
     * @return - Bit mask
     */
    public int getCoded() {
        return mDays;
    }

    /**
     * Returns days of week encoded in an array of booleans.
     * @return - Boolean Array
     */
    public boolean[] getBooleanArray() {
        boolean[] ret = new boolean[7];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = isSet(i);
        }
        return ret;
    }

    /**
     * Converts the day in the string format to the Constant defined in Calendar
     * @param day - Day of Week
     * @return  - Integer equivalent of day
     */
    public int convertStrToCalendarDay(String day) {
        String[] shortWeekDays = new DateFormatSymbols().getShortWeekdays();

        String[] arrayOfShortWeekdays = new String[] {
            shortWeekDays[Calendar.MONDAY],
            shortWeekDays[Calendar.TUESDAY],
            shortWeekDays[Calendar.WEDNESDAY],
            shortWeekDays[Calendar.THURSDAY],
            shortWeekDays[Calendar.FRIDAY],
            shortWeekDays[Calendar.SATURDAY],
            shortWeekDays[Calendar.SUNDAY]
        };
        for (int i=0; i < arrayOfShortWeekdays.length; i++) {
            if (day.equalsIgnoreCase(arrayOfShortWeekdays[i]))
                return DAY_MAP[i];
        }
        return TIME_FRAME_DAY_NONE;
    }

    /**
     * Convert the day in long format to short format
     * @param day - Day of Week in long format
     * @return Day of Week in short format
     */
    public String getShortFormat(String day) {
        String[] shortWeekDays = new DateFormatSymbols().getShortWeekdays();
        String[] longWeekDays  = new DateFormatSymbols().getWeekdays();

        String[] arrayOfShortWeekdays = new String[] {
            shortWeekDays[Calendar.MONDAY],
            shortWeekDays[Calendar.TUESDAY],
            shortWeekDays[Calendar.WEDNESDAY],
            shortWeekDays[Calendar.THURSDAY],
            shortWeekDays[Calendar.FRIDAY],
            shortWeekDays[Calendar.SATURDAY],
            shortWeekDays[Calendar.SUNDAY]
        };
        String[] arrayOfLongWeekdays = new String[] {
            longWeekDays[Calendar.MONDAY],
            longWeekDays[Calendar.TUESDAY],
            longWeekDays[Calendar.WEDNESDAY],
            longWeekDays[Calendar.THURSDAY],
            longWeekDays[Calendar.FRIDAY],
            longWeekDays[Calendar.SATURDAY],
            longWeekDays[Calendar.SUNDAY]
        };
        for (int i=0; i < arrayOfLongWeekdays.length; i++) {
            if (day.equalsIgnoreCase(arrayOfLongWeekdays[i]))
                return arrayOfShortWeekdays[i];
        }
        //Everyday & Never are same in long & short form
        return day;
    }

    /**
     * Convert the day in short format to long format
     * @param day - Day of Week in short format
     * @return Day of Week in long format
     */
    public String getLongFormat(String day) {
        String[] shortWeekDays = new DateFormatSymbols().getShortWeekdays();
        String[] longWeekDays  = new DateFormatSymbols().getWeekdays();

        String[] arrayOfShortWeekdays = new String[] {
            shortWeekDays[Calendar.MONDAY],
            shortWeekDays[Calendar.TUESDAY],
            shortWeekDays[Calendar.WEDNESDAY],
            shortWeekDays[Calendar.THURSDAY],
            shortWeekDays[Calendar.FRIDAY],
            shortWeekDays[Calendar.SATURDAY],
            shortWeekDays[Calendar.SUNDAY]
        };
        String[] arrayOfLongWeekdays = new String[] {
            longWeekDays[Calendar.MONDAY],
            longWeekDays[Calendar.TUESDAY],
            longWeekDays[Calendar.WEDNESDAY],
            longWeekDays[Calendar.THURSDAY],
            longWeekDays[Calendar.FRIDAY],
            longWeekDays[Calendar.SATURDAY],
            longWeekDays[Calendar.SUNDAY]
        };
        for (int i=0; i < arrayOfShortWeekdays.length; i++) {
            if (day.equalsIgnoreCase(arrayOfShortWeekdays[i]))
                return arrayOfLongWeekdays[i];
        }
        //Everyday & Never are same in long & short form
        return day;
    }
}
