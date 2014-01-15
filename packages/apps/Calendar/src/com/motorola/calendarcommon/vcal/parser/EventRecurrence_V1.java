/*
 * Copyright (C) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 19-Dec-2011    a21263       Initial version
 **********************************************************
 */
package com.motorola.calendarcommon.vcal.parser;

import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;

import android.text.format.Time;
import android.text.TextUtils;
import android.util.TimeFormatException;
import android.util.Log;

import com.android.calendarcommon.EventRecurrence;

/**
 * @hide
 */
public class EventRecurrence_V1 {
    private static final String TAG = "EventRecurrence_V1";
    private final EventRecurrence evtRecur = new EventRecurrence();

    /** maps a two-character weekday string to an integer constant */
    private static final HashMap<String, Integer> sParseWeekdayMap = new HashMap<String, Integer>();
    static {
        sParseWeekdayMap.put("SU", EventRecurrence.SU);
        sParseWeekdayMap.put("MO", EventRecurrence.MO);
        sParseWeekdayMap.put("TU", EventRecurrence.TU);
        sParseWeekdayMap.put("WE", EventRecurrence.WE);
        sParseWeekdayMap.put("TH", EventRecurrence.TH);
        sParseWeekdayMap.put("FR", EventRecurrence.FR);
        sParseWeekdayMap.put("SA", EventRecurrence.SA);
    }

    private static HashMap<String, RRuleParser> sRRuleParser;
    static {
       sRRuleParser = new HashMap<String, RRuleParser>();
       sRRuleParser.put("D[0-9]+", new ParseDaily());
       sRRuleParser.put("W[0-9]+", new ParseWeekly());
       sRRuleParser.put("MP[0-9]+", new ParseMonthlyByPos());
       sRRuleParser.put("MD[0-9]+", new ParseMonthlyByDay());
       sRRuleParser.put("YM[0-9]+", new ParseYearlyByMonth());
       sRRuleParser.put("YD[0-9]+", new ParseYearlyByDay());
    }

    /**
     * Thrown when a recurrence string provided can not be parsed according
     * to vCal-1.0 spec.
     */
    public static class InvalidFormatException extends RuntimeException {
        InvalidFormatException(String s) {
            super(s);
        }
    }

    static abstract class RRuleParser {
        /**
         * Parses a repeat rule defined by vCal-1.0 spec
         *
         * @param rRule The repeat rule without frequence part
         * @param recur The EventRecurrence into which the result is stored.
         */
        public abstract void parse(String rRule, EventRecurrence recur);

        /**
         * Parses the string argument as a decimal integer, with range-checking.
         *
         * @param str string containing the representation to be parsed
         * @param minVal Minimum allowed value
         * @param maxVal Maximum allowed value
         *
         * @return the integer value represented by the argument in decimal
         *
         * @throws InvalidFormatException
         *         if the string does not contain a parzable integer or the
         *         parsed integer value is not in range
         */
        public static int parseIntRange(String str, int minVal, int maxVal) {
            try {
                int value = Integer.parseInt(str);
                if (value < minVal || value > maxVal) {
                    throw new InvalidFormatException("Integer value out of range");
                }
                return value;
            } catch (NumberFormatException nfe) {
                throw new InvalidFormatException("String format for int is wrong");
            }
        }

        /**
         * Check whether the Date string format is well formed.
         *
         * @param str the date string to be checked
         *
         * @return the original date str value
         *
         @ @throws InvalidFormatException if the date format is invalid
         */
        public static String parseDate(String str) {
            try {
                // Parse the time to validate it.  The result isn't retained.
                Time date = new Time(Time.TIMEZONE_UTC);
                date.parse(str);
            } catch (TimeFormatException tfe) {
                throw new InvalidFormatException("Invalid date value: " + str);
            }

            return str;
        }

        /**
         * Parse the duration (COUNT) or end date (UNTIL) value
         *
         * @param str the string to be parsed
         * @param recur The EventRecurrence into which the result is stored
         */
        public static void parseDurationOrEndDate(String str, EventRecurrence recur) {
            try {
                if (str.charAt(0) == '#') {
                    int count = parseIntRange(str.substring(1), 0, Integer.MAX_VALUE);
                    // count = 0 means repeating forever
                    if (count != 0) {
                        recur.count = count;
                    }
                } else {
                    recur.until = parseDate(str);
                }
            } catch (InvalidFormatException e) {
                // Treat the event as repeating forever by ignoring the exception
            }
        }
    }

    private static class ParseDaily extends RRuleParser {
        @Override
        public void parse(String rRule, EventRecurrence recur) {
            String[] parts = rRule.split("[ ]+");
            recur.freq = EventRecurrence.DAILY;
            // Parse "D[0-9]+"
            recur.interval = parseIntRange(parts[0].substring(1), 1, Integer.MAX_VALUE);

            // Parse duration or end date
            if (parts.length > 1) {
                parseDurationOrEndDate(parts[parts.length-1], recur);
            }
        }
    }

    private static class ParseWeekly extends RRuleParser {
        @Override
        public void parse(String rRule, EventRecurrence recur) {
            String[] parts = rRule.split("[ ]+");
            recur.freq = EventRecurrence.WEEKLY;
            // Parse "W[0-9]+"
            recur.interval = parseIntRange(parts[0].substring(1), 1, Integer.MAX_VALUE);

            // Parse week day "MO SU TU ..."
            HashMap<String, Integer> weekDayMap = new HashMap<String, Integer>();
            int i = 1;
            for (; i < parts.length; ++i) {
                Integer weekDay = sParseWeekdayMap.get(parts[i]);
                if (weekDay != null) {
                    weekDayMap.put(parts[i], weekDay);
                }
            }

            recur.bydayCount = weekDayMap.size();
            if (recur.bydayCount > 0) {
                recur.byday = new int[recur.bydayCount];
                recur.bydayNum = new int[recur.bydayCount];
                Collection<Integer> values = weekDayMap.values();
                int j = 0;
                for (Integer weekDay : values) {
                    recur.byday[j++] = weekDay.intValue();
                }
            }

            // Parse duration or end date
            if (i < parts.length) {
                parseDurationOrEndDate(parts[parts.length-1], recur);
            }
        }
    }

    private static class ParseMonthlyByPos extends RRuleParser {
        @Override
        public void parse(String rRule, EventRecurrence recur) {
            String[] parts = rRule.split("[ ]+");
            recur.freq = EventRecurrence.MONTHLY;
            // Parse "MP[0-9]+"
            recur.interval = parseIntRange(parts[0].substring(2), 1, Integer.MAX_VALUE);

            // Parse Weekday ( "1, -1 MO" or " 1 MO -1 SU" etc)
            ArrayList<Entry<Integer, Integer>> entryList
                    = new ArrayList<Entry<Integer, Integer>>();
            ArrayList<Integer> occurList = new ArrayList<Integer>();
            ArrayList<Integer> weekDayList = new ArrayList<Integer>();
            int i = 1;
            do {
                occurList.clear();
                weekDayList.clear();
                while (i < parts.length) {
                    Integer occur = getOccurence(parts[i]);
                    if (occur != null) {
                        occurList.add(occur);
                        ++i;
                    } else {
                        break;
                    }
                }

                while (i < parts.length) {
                    Integer weekDay = sParseWeekdayMap.get(parts[i]);
                    if (weekDay != null) {
                        weekDayList.add(weekDay);
                        ++i;
                    } else {
                        break;
                    }
                }

                if (!weekDayList.isEmpty()) {
                    if (!occurList.isEmpty()) {
                        for (Integer occurence : occurList)
                            for (Integer weekDay : weekDayList) {
                                entryList.add(new SimpleEntry<Integer, Integer>(
                                            occurence, weekDay));
                            }
                    } else {
                        for (Integer weekDay : weekDayList) {
                            entryList.add(new SimpleEntry<Integer, Integer>(
                                        Integer.valueOf(0), weekDay));
                        }
                    }
                }
            } while ((i < parts.length) && !weekDayList.isEmpty());

            recur.bydayCount = entryList.size();
            if (recur.bydayCount > 0) {
                recur.byday = new int[recur.bydayCount];
                recur.bydayNum = new int[recur.bydayCount];
                int j = 0;
                for (Entry<Integer, Integer> entry : entryList) {
                    recur.bydayNum[j] = entry.getKey().intValue();
                    recur.byday[j] = entry.getValue().intValue();
                    ++j;
                }
            }

            // Parse duration or end date
            if (i < parts.length) {
                parseDurationOrEndDate(parts[parts.length-1], recur);
            }
        }

        private static Integer getOccurence(String str) {
            int length = str.length();
            if (length > 0) {
                int sign = 1;
                if (str.endsWith("+")) {
                    str = str.substring(0, length-1);
                } else if (str.endsWith("-")) {
                    sign = -1;
                    str = str.substring(0, length-1);
                }

                try {
                    int value = parseIntRange(str, 1, 5);
                    return Integer.valueOf(value*sign);
                } catch (InvalidFormatException e) {
                    // Return null value by ignoring the exception
                }
            }

            return null;
        }
    }

    private static class ParseMonthlyByDay extends RRuleParser {
        @Override
        public void parse(String rRule, EventRecurrence recur) {
            String[] parts = rRule.split("[ ]+");
            recur.freq = EventRecurrence.MONTHLY;
            // Parse "MD[0-9]+"
            recur.interval = parseIntRange(parts[0].substring(2), 1, Integer.MAX_VALUE);

            // Parse MonthDay such as 1, 15, -1, LD etc
            ArrayList<Integer> monthDayList = new ArrayList<Integer>();
            int i = 1;
            for (; i < parts.length; ++i) {
                Integer monthDay = getMonthDay(parts[i]);
                if (monthDay != null) {
                    monthDayList.add(monthDay);
                } else if ("LD".equalsIgnoreCase(parts[i])) {
                    monthDayList.add(Integer.valueOf(-1));
                } else {
                    break;
                }
            }

            recur.bymonthdayCount = monthDayList.size();
            if (recur.bymonthdayCount > 0) {
                recur.bymonthday = new int[recur.bymonthdayCount];
                int j = 0;
                for (Integer monthDay : monthDayList) {
                    recur.bymonthday[j++] = monthDay.intValue();
                }
            }

            // Parse duration or end date
            if (i < parts.length) {
                parseDurationOrEndDate(parts[parts.length-1], recur);
            }
        }

        private static Integer getMonthDay(String str) {
            int length = str.length();
            if (length > 0) {
                int sign = 1;
                if (str.endsWith("+")) {
                    str = str.substring(0, length-1);
                } else if (str.endsWith("-")) {
                    sign = -1;
                    str = str.substring(0, length-1);
                }

                try {
                    int value = parseIntRange(str, 1, 31);
                    return Integer.valueOf(value*sign);
                } catch (InvalidFormatException e) {
                    // Return null value by ignoring the exception
                }
            }

            return null;
        }
    }

    private static class ParseYearlyByMonth extends RRuleParser {
        @Override
        public void parse(String rRule, EventRecurrence recur) {
            String[] parts = rRule.split("[ ]+");
            recur.freq = EventRecurrence.YEARLY;
            // Parse "YM[0-9]+"
            recur.interval = parseIntRange(parts[0].substring(2), 1, Integer.MAX_VALUE);

            // Parse ByMonth such as 1, 2, 3, ...
            ArrayList<Integer> monthList = new ArrayList<Integer>();
            int i = 1;
            for (; i < parts.length; ++i) {
                Integer month = getMonth(parts[i]);
                if (month != null) {
                    monthList.add(month);
                } else {
                    break;
                }
            }

            recur.bymonthCount = monthList.size();
            if (recur.bymonthCount > 0) {
                recur.bymonth = new int[recur.bymonthCount];
                int j = 0;
                for (Integer month : monthList) {
                    recur.bymonth[j++] = month.intValue();
                }
            }

            // Parse duration or end date
            if (i < parts.length) {
                parseDurationOrEndDate(parts[parts.length-1], recur);
            }
        }

        private static Integer getMonth(String str) {
            try {
                int value = parseIntRange(str, 1, 12);
                return Integer.valueOf(value);
            } catch (InvalidFormatException e) {
                // Return null value by ignoring the exception
            }

            return null;
        }
    }

    private static class ParseYearlyByDay extends RRuleParser {
        @Override
        public void parse(String rRule, EventRecurrence recur) {
            String[] parts = rRule.split("[ ]+");
            recur.freq = EventRecurrence.YEARLY;
            // Parse "YD[0-9]+"
            recur.interval = parseIntRange(parts[0].substring(2), 1, Integer.MAX_VALUE);

            // Parse ByYearDay such as 1, 2, 3, ...
            ArrayList<Integer> yearDayList = new ArrayList<Integer>();
            int i = 1;
            for (; i < parts.length; ++i) {
                Integer yearDay = getYearDay(parts[i]);
                if (yearDay != null) {
                    yearDayList.add(yearDay);
                } else {
                    break;
                }
            }

            recur.byyeardayCount = yearDayList.size();
            if (recur.byyeardayCount > 0) {
                recur.byyearday = new int[recur.byyeardayCount];
                int j = 0;
                for (Integer yearDay : yearDayList) {
                    recur.byyearday[j++] = yearDay.intValue();
                }
            }

            // Parse duration or end date
            if (i < parts.length) {
                parseDurationOrEndDate(parts[parts.length-1], recur);
            }
        }

        private static Integer getYearDay(String str) {
            int length = str.length();
            if (length > 0) {
                int sign = 1;
                if (str.endsWith("+")) {
                    str = str.substring(0, length-1);
                } else if (str.endsWith("-")) {
                    sign = -1;
                    str = str.substring(0, length-1);
                }

                try {
                    int value = parseIntRange(str, 1, 366);
                    return Integer.valueOf(value*sign);
                } catch (InvalidFormatException e) {
                    // Return null value by ignoring the exception
                }
            }

            return null;
        }
    }

    public void parse(String rRule) {
        if (TextUtils.isEmpty(rRule)) {
            throw new IllegalArgumentException("RRULE is empty");
        }

        String freq;
        int index = rRule.indexOf(' ');
        if (index != -1) {
            freq = rRule.substring(0, index);
        } else {
            freq = rRule;
        }

        RRuleParser parser = null;
        Set<Entry<String, RRuleParser>> entrySet = sRRuleParser.entrySet();
        for (Entry<String, RRuleParser> entry : entrySet) {
            String key = entry.getKey();
            if (freq.matches(key)) {
                parser = entry.getValue();
                break;
            }
        }

        if (parser == null) {
            throw new InvalidFormatException("Can't find parser for the RRULE");
        }

        // Reset EventRecurrence so the parser object can be reused multiple times
        {
            evtRecur.until = null;
            evtRecur.freq = 0;
            evtRecur.count = 0;
            evtRecur.interval = 0;
            evtRecur.bysecondCount = 0;
            evtRecur.byminuteCount = 0;
            evtRecur.byhourCount = 0;
            evtRecur.bydayCount = 0;
            evtRecur.bymonthdayCount = 0;
            evtRecur.byyeardayCount = 0;
            evtRecur.byweeknoCount = 0;
            evtRecur.bymonthCount = 0;
            evtRecur.bysetposCount = 0;
        }

        parser.parse(rRule, evtRecur);

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, evtRecur.toString());
        }
    }

    @Override
    public String toString() {
        return evtRecur.toString();
    }
}
