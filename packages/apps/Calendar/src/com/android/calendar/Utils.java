/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calendar;

import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;

import android.app.Activity;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract.Events;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.SearchView;
import android.widget.Toast;

import com.android.calendar.CalendarController.ViewType;
import com.android.calendar.CalendarUtils.TimeZoneUtils;
import com.android.calendar.R;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class Utils {
    private static final boolean DEBUG = false;
    private static final String TAG = "CalUtils";

    // Set to 0 until we have UI to perform undo
    public static final long UNDO_DELAY = 0;

    // For recurring events which instances of the series are being modified
    public static final int MODIFY_UNINITIALIZED = 0;
    public static final int MODIFY_SELECTED = 1;
    public static final int MODIFY_ALL_FOLLOWING = 2;
    public static final int MODIFY_ALL = 3;

    // When the edit event view finishes it passes back the appropriate exit
    // code.
    public static final int DONE_REVERT = 1 << 0;
    public static final int DONE_SAVE = 1 << 1;
    public static final int DONE_DELETE = 1 << 2;
    // And should re run with DONE_EXIT if it should also leave the view, just
    // exiting is identical to reverting
    public static final int DONE_EXIT = 1 << 0;

    public static final String OPEN_EMAIL_MARKER = " <";
    public static final String CLOSE_EMAIL_MARKER = ">";

    public static final String INTENT_KEY_DETAIL_VIEW = "DETAIL_VIEW";
    public static final String INTENT_KEY_VIEW_TYPE = "VIEW";
    public static final String INTENT_VALUE_VIEW_TYPE_DAY = "DAY";
    public static final String INTENT_KEY_HOME = "KEY_HOME";

    public static final int MONDAY_BEFORE_JULIAN_EPOCH = Time.EPOCH_JULIAN_DAY - 3;
    public static final int DECLINED_EVENT_ALPHA = 0x66;
    public static final int DECLINED_EVENT_TEXT_ALPHA = 0xC0;
    
    // Begin Motorola
    // The extended capabilities related to Calendar app
    public static final int EXT_CAPABILITY_ADD_TO_TASKS = 1 << 0;
    public static final int EXT_CAPABILITY_PRINT_ICS = 1 << 1;

    public static final String ADD_EVENT_TASKS_ACTION = "com.motorola.blur.tasks.ACTION_NEW_TASKIFY";
    public static final String TASKIFY_FLAG = "taskify_flag";
    public static final String TASKIFY_STRICT_EVENT_UID = "taskify_strict_event_uid";
    public static final String PRINT_ICS_ACTION = "com.motorola.android.intent.action.PRINT";

    private static final String MIME_TYPE_ICAL = "text/x-vcalendar";
    private static final String MIME_TYPE_MSG_RFC822 = "message/rfc822";
    private static final String ANDROID_EMAIL_PKG = "com.android.email";
    private static final String GOOGLE_EMAIL_PKG = "com.google.android.email";
    private static final String MOTO_EMAIL_PKG = "com.motorola.motoemail";
    private static final String GMAIL_PKG = "com.google.android.gm";
    private static final String GOOGLE_ACCOUNT_TYPE = "com.google";
    private static final String EXCHANGE_ACCOUNT_TYPE = "com.android.exchange";
    private static final String COMPOSE_ACCT_ID = "account_id";
    // End Motorola

    private static final float SATURATION_ADJUST = 1.3f;
    private static final float INTENSITY_ADJUST = 0.8f;

    // Defines used by the DNA generation code
    static final int DAY_IN_MINUTES = 60 * 24;
    static final int WEEK_IN_MINUTES = DAY_IN_MINUTES * 7;
    // The work day is being counted as 6am to 8pm
    static int WORK_DAY_MINUTES = 14 * 60;
    static int WORK_DAY_START_MINUTES = 6 * 60;
    static int WORK_DAY_END_MINUTES = 20 * 60;
    static int WORK_DAY_END_LENGTH = (24 * 60) - WORK_DAY_END_MINUTES;
    static int CONFLICT_COLOR = 0xFF000000;
    static boolean mMinutesLoaded = false;

    // The name of the shared preferences file. This name must be maintained for
    // historical
    // reasons, as it's what PreferenceManager assigned the first time the file
    // was created.
    static final String SHARED_PREFS_NAME = "com.android.calendar_preferences";

    public static final String KEY_QUICK_RESPONSES = "preferences_quick_responses";

    public static final String APPWIDGET_DATA_TYPE = "vnd.android.data/update";

    static final String MACHINE_GENERATED_ADDRESS = "calendar.google.com";

    private static final TimeZoneUtils mTZUtils = new TimeZoneUtils(SHARED_PREFS_NAME);
    private static boolean mAllowWeekForDetailView = false;
    private static long mTardis = 0;

    public static int getViewTypeFromIntentAndSharedPref(Activity activity) {
        Intent intent = activity.getIntent();
        Bundle extras = intent.getExtras();
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(activity);

        if (TextUtils.equals(intent.getAction(), Intent.ACTION_EDIT)) {
            return ViewType.EDIT;
        }
        if (extras != null) {
            if (extras.getBoolean(INTENT_KEY_DETAIL_VIEW, false)) {
                // This is the "detail" view which is either agenda or day view
                return prefs.getInt(GeneralPreferences.KEY_DETAILED_VIEW,
                        GeneralPreferences.DEFAULT_DETAILED_VIEW);
            } else if (INTENT_VALUE_VIEW_TYPE_DAY.equals(extras.getString(INTENT_KEY_VIEW_TYPE))) {
                // Not sure who uses this. This logic came from LaunchActivity
                return ViewType.DAY;
            }
        }
        // Begin Motorola
        // Default to the last view
//        return prefs.getInt(
//                GeneralPreferences.KEY_START_VIEW, GeneralPreferences.DEFAULT_START_VIEW);
        int startActivity =  prefs.getInt(
                GeneralPreferences.KEY_START_VIEW, GeneralPreferences.DEFAULT_START_VIEW);
        String defaultView = prefs.getString(
                GeneralPreferences.KEY_DEFAULT_CALENDAR_VIEW, "0");

        int id = 0;
        try {
            id = Integer.parseInt(defaultView);
            if (id == 0){
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(GeneralPreferences.KEY_DEFAULT_CALENDAR_VIEW, "4");
                editor.commit();
                id = 4;
            }
        } catch (NumberFormatException e) {
            if (DEBUG) {
                Log.e(TAG, "Default view id is invalid");
            }
        }
        // id: 0 means that default view is set to None. So return the view user last entered.
        if (id > 0 ) {
            return id;
        }
        return startActivity;
        // End Motorola
    }

    /**
     * Gets the intent action for telling the widget to update.
     */
    public static String getWidgetUpdateAction(Context context) {
        return context.getPackageName() + ".APPWIDGET_UPDATE";
    }

    /**
     * Gets the intent action for telling the widget to update.
     */
    public static String getWidgetScheduledUpdateAction(Context context) {
        return context.getPackageName() + ".APPWIDGET_SCHEDULED_UPDATE";
    }

    /**
     * Gets the intent action for telling the widget to update.
     */
    public static String getSearchAuthority(Context context) {
        return context.getPackageName() + ".CalendarRecentSuggestionsProvider";
    }

    /**
     * Writes a new home time zone to the db. Updates the home time zone in the
     * db asynchronously and updates the local cache. Sending a time zone of
     * **tbd** will cause it to be set to the device's time zone. null or empty
     * tz will be ignored.
     *
     * @param context The calling activity
     * @param timeZone The time zone to set Calendar to, or **tbd**
     */
    public static void setTimeZone(Context context, String timeZone) {
        mTZUtils.setTimeZone(context, timeZone);
    }

    /**
     * Gets the time zone that Calendar should be displayed in This is a helper
     * method to get the appropriate time zone for Calendar. If this is the
     * first time this method has been called it will initiate an asynchronous
     * query to verify that the data in preferences is correct. The callback
     * supplied will only be called if this query returns a value other than
     * what is stored in preferences and should cause the calling activity to
     * refresh anything that depends on calling this method.
     *
     * @param context The calling activity
     * @param callback The runnable that should execute if a query returns new
     *            values
     * @return The string value representing the time zone Calendar should
     *         display
     */
    public static String getTimeZone(Context context, Runnable callback) {
        return mTZUtils.getTimeZone(context, callback);
    }

    /**
     * Formats a date or a time range according to the local conventions.
     *
     * @param context the context is required only if the time is shown
     * @param startMillis the start time in UTC milliseconds
     * @param endMillis the end time in UTC milliseconds
     * @param flags a bit mask of options See {@link DateUtils#formatDateRange(Context, Formatter,
     * long, long, int, String) formatDateRange}
     * @return a string containing the formatted date/time range.
     */
    public static String formatDateRange(
            Context context, long startMillis, long endMillis, int flags) {
        return mTZUtils.formatDateRange(context, startMillis, endMillis, flags);
    }

    public static String[] getSharedPreference(Context context, String key, String[] defaultValue) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        Set<String> ss = prefs.getStringSet(key, null);
        if (ss != null) {
            String strings[] = new String[ss.size()];
            return ss.toArray(strings);
        }
        return defaultValue;
    }

    public static String getSharedPreference(Context context, String key, String defaultValue) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getString(key, defaultValue);
    }

    public static int getSharedPreference(Context context, String key, int defaultValue) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getInt(key, defaultValue);
    }

    public static boolean getSharedPreference(Context context, String key, boolean defaultValue) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getBoolean(key, defaultValue);
    }

    /**
     * Asynchronously sets the preference with the given key to the given value
     *
     * @param context the context to use to get preferences from
     * @param key the key of the preference to set
     * @param value the value to set
     */
    public static void setSharedPreference(Context context, String key, String value) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        prefs.edit().putString(key, value).apply();
    }

    public static void setSharedPreference(Context context, String key, String[] values) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        for (int i = 0; i < values.length; i++) {
            set.add(values[i]);
        }
        prefs.edit().putStringSet(key, set).apply();
    }

    protected static void tardis() {
        mTardis = System.currentTimeMillis();
    }

    protected static long getTardis() {
        return mTardis;
    }

    static void setSharedPreference(Context context, String key, boolean value) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    static void setSharedPreference(Context context, String key, int value) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     * Save default agenda/day/week/month view for next time
     *
     * @param context
     * @param viewId {@link CalendarController.ViewType}
     */
    static void setDefaultView(Context context, int viewId) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        boolean validDetailView = false;
        if (mAllowWeekForDetailView && viewId == CalendarController.ViewType.WEEK) {
            validDetailView = true;
        } else {
            validDetailView = viewId == CalendarController.ViewType.AGENDA
                    || viewId == CalendarController.ViewType.DAY;
        }

        if (validDetailView) {
            // Record the detail start view
            editor.putInt(GeneralPreferences.KEY_DETAILED_VIEW, viewId);
        }

        // Record the (new) start view
        editor.putInt(GeneralPreferences.KEY_START_VIEW, viewId);
        editor.apply();
    }

    public static MatrixCursor matrixCursorFromCursor(Cursor cursor) {
        String[] columnNames = cursor.getColumnNames();
        if (columnNames == null) {
            columnNames = new String[] {};
        }
        MatrixCursor newCursor = new MatrixCursor(columnNames);
        int numColumns = cursor.getColumnCount();
        String data[] = new String[numColumns];
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            for (int i = 0; i < numColumns; i++) {
                data[i] = cursor.getString(i);
            }
            newCursor.addRow(data);
        }
        return newCursor;
    }

    /**
     * Compares two cursors to see if they contain the same data.
     *
     * @return Returns true of the cursors contain the same data and are not
     *         null, false otherwise
     */
    public static boolean compareCursors(Cursor c1, Cursor c2) {
        if (c1 == null || c2 == null) {
            return false;
        }

        int numColumns = c1.getColumnCount();
        if (numColumns != c2.getColumnCount()) {
            return false;
        }

        if (c1.getCount() != c2.getCount()) {
            return false;
        }

        c1.moveToPosition(-1);
        c2.moveToPosition(-1);
        while (c1.moveToNext() && c2.moveToNext()) {
            for (int i = 0; i < numColumns; i++) {
                if (!TextUtils.equals(c1.getString(i), c2.getString(i))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * If the given intent specifies a time (in milliseconds since the epoch),
     * then that time is returned. Otherwise, the current time is returned.
     */
    public static final long timeFromIntentInMillis(Intent intent) {
        // If the time was specified, then use that. Otherwise, use the current
        // time.
        Uri data = intent.getData();
        long millis = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, -1);
        if (millis == -1 && data != null && data.isHierarchical()) {
            List<String> path = data.getPathSegments();
            if (path.size() == 2 && path.get(0).equals("time")) {
                try {
                    millis = Long.valueOf(data.getLastPathSegment());
                } catch (NumberFormatException e) {
                    Log.i("Calendar", "timeFromIntentInMillis: Data existed but no valid time "
                            + "found. Using current time.");
                }
            }
        }
        if (millis <= 0) {
            millis = System.currentTimeMillis();
        }
        return millis;
    }

    /**
     * Formats the given Time object so that it gives the month and year (for
     * example, "September 2007").
     *
     * @param time the time to format
     * @return the string containing the weekday and the date
     */
    public static String formatMonthYear(Context context, Time time) {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_MONTH_DAY
                | DateUtils.FORMAT_SHOW_YEAR;
        long millis = time.toMillis(true);
        return formatDateRange(context, millis, millis, flags);
    }

    /**
     * Returns a list joined together by the provided delimiter, for example,
     * ["a", "b", "c"] could be joined into "a,b,c"
     *
     * @param things the things to join together
     * @param delim the delimiter to use
     * @return a string contained the things joined together
     */
    public static String join(List<?> things, String delim) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Object thing : things) {
            if (first) {
                first = false;
            } else {
                builder.append(delim);
            }
            builder.append(thing.toString());
        }
        return builder.toString();
    }

    /**
     * Returns the week since {@link Time#EPOCH_JULIAN_DAY} (Jan 1, 1970)
     * adjusted for first day of week.
     *
     * This takes a julian day and the week start day and calculates which
     * week since {@link Time#EPOCH_JULIAN_DAY} that day occurs in, starting
     * at 0. *Do not* use this to compute the ISO week number for the year.
     *
     * @param julianDay The julian day to calculate the week number for
     * @param firstDayOfWeek Which week day is the first day of the week,
     *          see {@link Time#SUNDAY}
     * @return Weeks since the epoch
     */
    public static int getWeeksSinceEpochFromJulianDay(int julianDay, int firstDayOfWeek) {
        int diff = Time.THURSDAY - firstDayOfWeek;
        if (diff < 0) {
            diff += 7;
        }
        int refDay = Time.EPOCH_JULIAN_DAY - diff;
        return (julianDay - refDay) / 7;
    }

    /**
     * Takes a number of weeks since the epoch and calculates the Julian day of
     * the Monday for that week.
     *
     * This assumes that the week containing the {@link Time#EPOCH_JULIAN_DAY}
     * is considered week 0. It returns the Julian day for the Monday
     * {@code week} weeks after the Monday of the week containing the epoch.
     *
     * @param week Number of weeks since the epoch
     * @return The julian day for the Monday of the given week since the epoch
     */
    public static int getJulianMondayFromWeeksSinceEpoch(int week) {
        // added by amt-sunli 2012-010-30 SWITCHUITWOV-274 begin
        if (week == 0) {
            return Time.EPOCH_JULIAN_DAY + week * 7;
        }
        // added by amt-sunli 2012-010-30 SWITCHUITWOV-274 end
    	return MONDAY_BEFORE_JULIAN_EPOCH + week * 7;
    }

    /**
     * Get first day of week as android.text.format.Time constant.
     *
     * @return the first day of week in android.text.format.Time
     */
    public static int getFirstDayOfWeek(Context context) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        String pref = prefs.getString(
                GeneralPreferences.KEY_WEEK_START_DAY, GeneralPreferences.WEEK_START_DEFAULT);

        int startDay;
        if (GeneralPreferences.WEEK_START_DEFAULT.equals(pref)) {
            startDay = Calendar.getInstance().getFirstDayOfWeek();
        } else {
            startDay = Integer.parseInt(pref);
        }

        if (startDay == Calendar.SATURDAY) {
            return Time.SATURDAY;
        } else if (startDay == Calendar.MONDAY) {
            return Time.MONDAY;
        } else {
            return Time.SUNDAY;
        }
    }

    /**
     * @return true when week number should be shown.
     */
    public static boolean getShowWeekNumber(Context context) {
        final SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getBoolean(
                GeneralPreferences.KEY_SHOW_WEEK_NUM, GeneralPreferences.DEFAULT_SHOW_WEEK_NUM);
    }

    /**
     * @return true when declined events should be hidden.
     */
    public static boolean getHideDeclinedEvents(Context context) {
        final SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getBoolean(GeneralPreferences.KEY_HIDE_DECLINED, false);
    }

    public static int getDaysPerWeek(Context context) {
        final SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getInt(GeneralPreferences.KEY_DAYS_PER_WEEK, 7);
    }

    /**
     * Determine whether the column position is Saturday or not.
     *
     * @param column the column position
     * @param firstDayOfWeek the first day of week in android.text.format.Time
     * @return true if the column is Saturday position
     */
    public static boolean isSaturday(int column, int firstDayOfWeek) {
        return (firstDayOfWeek == Time.SUNDAY && column == 6)
                || (firstDayOfWeek == Time.MONDAY && column == 5)
                || (firstDayOfWeek == Time.SATURDAY && column == 0);
    }

    /**
     * Determine whether the column position is Sunday or not.
     *
     * @param column the column position
     * @param firstDayOfWeek the first day of week in android.text.format.Time
     * @return true if the column is Sunday position
     */
    public static boolean isSunday(int column, int firstDayOfWeek) {
        return (firstDayOfWeek == Time.SUNDAY && column == 0)
                || (firstDayOfWeek == Time.MONDAY && column == 6)
                || (firstDayOfWeek == Time.SATURDAY && column == 1);
    }

    /**
     * Convert given UTC time into current local time. This assumes it is for an
     * allday event and will adjust the time to be on a midnight boundary.
     *
     * @param recycle Time object to recycle, otherwise null.
     * @param utcTime Time to convert, in UTC.
     * @param tz The time zone to convert this time to.
     */
    public static long convertAlldayUtcToLocal(Time recycle, long utcTime, String tz) {
        if (recycle == null) {
            recycle = new Time();
        }
        recycle.timezone = Time.TIMEZONE_UTC;
        recycle.set(utcTime);
        recycle.timezone = tz;
        return recycle.normalize(true);
    }

    public static long convertAlldayLocalToUTC(Time recycle, long localTime, String tz) {
        if (recycle == null) {
            recycle = new Time();
        }
        recycle.timezone = tz;
        recycle.set(localTime);
        recycle.timezone = Time.TIMEZONE_UTC;
        return recycle.normalize(true);
    }

    /**
     * Finds and returns the next midnight after "theTime" in milliseconds UTC
     *
     * @param recycle - Time object to recycle, otherwise null.
     * @param theTime - Time used for calculations (in UTC)
     * @param tz The time zone to convert this time to.
     */
    public static long getNextMidnight(Time recycle, long theTime, String tz) {
        if (recycle == null) {
            recycle = new Time();
        }
        recycle.timezone = tz;
        recycle.set(theTime);
        recycle.monthDay ++;
        recycle.hour = 0;
        recycle.minute = 0;
        recycle.second = 0;
        return recycle.normalize(true);
    }

    /**
     * Scan through a cursor of calendars and check if names are duplicated.
     * This travels a cursor containing calendar display names and fills in the
     * provided map with whether or not each name is repeated.
     *
     * @param isDuplicateName The map to put the duplicate check results in.
     * @param cursor The query of calendars to check
     * @param nameIndex The column of the query that contains the display name
     */
    public static void checkForDuplicateNames(
            Map<String, Boolean> isDuplicateName, Cursor cursor, int nameIndex) {
        isDuplicateName.clear();
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            String displayName = cursor.getString(nameIndex);
            // Set it to true if we've seen this name before, false otherwise
            if (displayName != null) {
                isDuplicateName.put(displayName, isDuplicateName.containsKey(displayName));
            }
        }
    }

    /**
     * Null-safe object comparison
     *
     * @param s1
     * @param s2
     * @return
     */
    public static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    public static void setAllowWeekForDetailView(boolean allowWeekView) {
        mAllowWeekForDetailView  = allowWeekView;
    }

    public static boolean getAllowWeekForDetailView() {
        return mAllowWeekForDetailView;
    }

    public static boolean getConfigBool(Context c, int key) {
        return c.getResources().getBoolean(key);
    }

    public static int getDisplayColorFromColor(int color) {
        // STOPSHIP - Finalize color adjustment algorithm before shipping

        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.min(hsv[1] * SATURATION_ADJUST, 1.0f);
        hsv[2] = hsv[2] * INTENSITY_ADJUST;
        return Color.HSVToColor(hsv);
    }

    // This takes a color and computes what it would look like blended with
    // white. The result is the color that should be used for declined events.
    public static int getDeclinedColorFromColor(int color) {
        int bg = 0xffffffff;
        int a = DECLINED_EVENT_ALPHA;
        int r = (((color & 0x00ff0000) * a) + ((bg & 0x00ff0000) * (0xff - a))) & 0xff000000;
        int g = (((color & 0x0000ff00) * a) + ((bg & 0x0000ff00) * (0xff - a))) & 0x00ff0000;
        int b = (((color & 0x000000ff) * a) + ((bg & 0x000000ff) * (0xff - a))) & 0x0000ff00;
        return (0xff000000) | ((r | g | b) >> 8);
    }

    // A single strand represents one color of events. Events are divided up by
    // color to make them convenient to draw. The black strand is special in
    // that it holds conflicting events as well as color settings for allday on
    // each day.
    public static class DNAStrand {
        public float[] points;
        public int[] allDays; // color for the allday, 0 means no event
        int position;
        public int color;
        int count;
    }

    // A segment is a single continuous length of time occupied by a single
    // color. Segments should never span multiple days.
    private static class DNASegment {
        int startMinute; // in minutes since the start of the week
        int endMinute;
        int color; // Calendar color or black for conflicts
        int day; // quick reference to the day this segment is on
    }

    /**
     * Converts a list of events to a list of segments to draw. Assumes list is
     * ordered by start time of the events. The function processes events for a
     * range of days from firstJulianDay to firstJulianDay + dayXs.length - 1.
     * The algorithm goes over all the events and creates a set of segments
     * ordered by start time. This list of segments is then converted into a
     * HashMap of strands which contain the draw points and are organized by
     * color. The strands can then be drawn by setting the paint color to each
     * strand's color and calling drawLines on its set of points. The points are
     * set up using the following parameters.
     * <ul>
     * <li>Events between midnight and WORK_DAY_START_MINUTES are compressed
     * into the first 1/8th of the space between top and bottom.</li>
     * <li>Events between WORK_DAY_END_MINUTES and the following midnight are
     * compressed into the last 1/8th of the space between top and bottom</li>
     * <li>Events between WORK_DAY_START_MINUTES and WORK_DAY_END_MINUTES use
     * the remaining 3/4ths of the space</li>
     * <li>All segments drawn will maintain at least minPixels height, except
     * for conflicts in the first or last 1/8th, which may be smaller</li>
     * </ul>
     *
     * @param firstJulianDay The julian day of the first day of events
     * @param events A list of events sorted by start time
     * @param top The lowest y value the dna should be drawn at
     * @param bottom The highest y value the dna should be drawn at
     * @param dayXs An array of x values to draw the dna at, one for each day
     * @param conflictColor the color to use for conflicts
     * @return
     */
    public static HashMap<Integer, DNAStrand> createDNAStrands(int firstJulianDay,
            ArrayList<Event> events, int top, int bottom, int minPixels, int[] dayXs,
            Context context) {

        if (!mMinutesLoaded) {
            if (context == null) {
                Log.wtf(TAG, "No context and haven't loaded parameters yet! Can't create DNA.");
            }
            Resources res = context.getResources();
            CONFLICT_COLOR = res.getColor(R.color.month_dna_conflict_time_color);
            WORK_DAY_START_MINUTES = res.getInteger(R.integer.work_start_minutes);
            WORK_DAY_END_MINUTES = res.getInteger(R.integer.work_end_minutes);
            WORK_DAY_END_LENGTH = DAY_IN_MINUTES - WORK_DAY_END_MINUTES;
            WORK_DAY_MINUTES = WORK_DAY_END_MINUTES - WORK_DAY_START_MINUTES;
            mMinutesLoaded = true;
        }

        if (events == null || events.isEmpty() || dayXs == null || dayXs.length < 1
                || bottom - top < 8 || minPixels < 0) {
            Log.e(TAG,
                    "Bad values for createDNAStrands! events:" + events + " dayXs:"
                            + Arrays.toString(dayXs) + " bot-top:" + (bottom - top) + " minPixels:"
                            + minPixels);
            return null;
        }

        LinkedList<DNASegment> segments = new LinkedList<DNASegment>();
        HashMap<Integer, DNAStrand> strands = new HashMap<Integer, DNAStrand>();
        // add a black strand by default, other colors will get added in
        // the loop
        DNAStrand blackStrand = new DNAStrand();
        blackStrand.color = CONFLICT_COLOR;
        strands.put(CONFLICT_COLOR, blackStrand);
        // the min length is the number of minutes that will occupy
        // MIN_SEGMENT_PIXELS in the 'work day' time slot. This computes the
        // minutes/pixel * minpx where the number of pixels are 3/4 the total
        // dna height: 4*(mins/(px * 3/4))
        int minMinutes = minPixels * 4 * WORK_DAY_MINUTES / (3 * (bottom - top));

        // There are slightly fewer than half as many pixels in 1/6 the space,
        // so round to 2.5x for the min minutes in the non-work area
        int minOtherMinutes = minMinutes * 5 / 2;
        int lastJulianDay = firstJulianDay + dayXs.length - 1;

        Event event = new Event();
        // Go through all the events for the week
        for (Event currEvent : events) {
            // if this event is outside the weeks range skip it
            if (currEvent.endDay < firstJulianDay || currEvent.startDay > lastJulianDay) {
                continue;
            }
            if (currEvent.drawAsAllday()) {
                addAllDayToStrands(currEvent, strands, firstJulianDay, dayXs.length);
                continue;
            }
            // Copy the event over so we can clip its start and end to our range
            currEvent.copyTo(event);
            if (event.startDay < firstJulianDay) {
                event.startDay = firstJulianDay;
                event.startTime = 0;
            }
            // If it starts after the work day make sure the start is at least
            // minPixels from midnight
            if (event.startTime > DAY_IN_MINUTES - minOtherMinutes) {
                event.startTime = DAY_IN_MINUTES - minOtherMinutes;
            }
            if (event.endDay > lastJulianDay) {
                event.endDay = lastJulianDay;
                event.endTime = DAY_IN_MINUTES - 1;
            }
            // If the end time is before the work day make sure it ends at least
            // minPixels after midnight
            if (event.endTime < minOtherMinutes) {
                event.endTime = minOtherMinutes;
            }
            // If the start and end are on the same day make sure they are at
            // least minPixels apart. This only needs to be done for times
            // outside the work day as the min distance for within the work day
            // is enforced in the segment code.
            if (event.startDay == event.endDay &&
                    event.endTime - event.startTime < minOtherMinutes) {
                // If it's less than minPixels in an area before the work
                // day
                if (event.startTime < WORK_DAY_START_MINUTES) {
                    // extend the end to the first easy guarantee that it's
                    // minPixels
                    event.endTime = Math.min(event.startTime + minOtherMinutes,
                            WORK_DAY_START_MINUTES + minMinutes);
                    // if it's in the area after the work day
                } else if (event.endTime > WORK_DAY_END_MINUTES) {
                    // First try shifting the end but not past midnight
                    event.endTime = Math.min(event.endTime + minOtherMinutes, DAY_IN_MINUTES - 1);
                    // if it's still too small move the start back
                    if (event.endTime - event.startTime < minOtherMinutes) {
                        event.startTime = event.endTime - minOtherMinutes;
                    }
                }
            }

            // This handles adding the first segment
            if (segments.size() == 0) {
                addNewSegment(segments, event, strands, firstJulianDay, 0, minMinutes);
                continue;
            }
            // Now compare our current start time to the end time of the last
            // segment in the list
            DNASegment lastSegment = segments.getLast();
            int startMinute = (event.startDay - firstJulianDay) * DAY_IN_MINUTES + event.startTime;
            int endMinute = Math.max((event.endDay - firstJulianDay) * DAY_IN_MINUTES
                    + event.endTime, startMinute + minMinutes);

            if (startMinute < 0) {
                startMinute = 0;
            }
            if (endMinute >= WEEK_IN_MINUTES) {
                endMinute = WEEK_IN_MINUTES - 1;
            }
            // If we start before the last segment in the list ends we need to
            // start going through the list as this may conflict with other
            // events
            if (startMinute < lastSegment.endMinute) {
                int i = segments.size();
                // find the last segment this event intersects with
                while (--i >= 0 && endMinute < segments.get(i).startMinute);

                DNASegment currSegment;
                // for each segment this event intersects with
                for (; i >= 0 && startMinute <= (currSegment = segments.get(i)).endMinute; i--) {
                    // if the segment is already a conflict ignore it
                    if (currSegment.color == CONFLICT_COLOR) {
                        continue;
                    }
                    // if the event ends before the segment and wouldn't create
                    // a segment that is too small split off the right side
                    if (endMinute < currSegment.endMinute - minMinutes) {
                        DNASegment rhs = new DNASegment();
                        rhs.endMinute = currSegment.endMinute;
                        rhs.color = currSegment.color;
                        rhs.startMinute = endMinute + 1;
                        rhs.day = currSegment.day;
                        currSegment.endMinute = endMinute;
                        segments.add(i + 1, rhs);
                        strands.get(rhs.color).count++;
                        if (DEBUG) {
                            Log.d(TAG, "Added rhs, curr:" + currSegment.toString() + " i:"
                                    + segments.get(i).toString());
                        }
                    }
                    // if the event starts after the segment and wouldn't create
                    // a segment that is too small split off the left side
                    if (startMinute > currSegment.startMinute + minMinutes) {
                        DNASegment lhs = new DNASegment();
                        lhs.startMinute = currSegment.startMinute;
                        lhs.color = currSegment.color;
                        lhs.endMinute = startMinute - 1;
                        lhs.day = currSegment.day;
                        currSegment.startMinute = startMinute;
                        // increment i so that we are at the right position when
                        // referencing the segments to the right and left of the
                        // current segment.
                        segments.add(i++, lhs);
                        strands.get(lhs.color).count++;
                        if (DEBUG) {
                            Log.d(TAG, "Added lhs, curr:" + currSegment.toString() + " i:"
                                    + segments.get(i).toString());
                        }
                    }
                    // if the right side is black merge this with the segment to
                    // the right if they're on the same day and overlap
                    if (i + 1 < segments.size()) {
                        DNASegment rhs = segments.get(i + 1);
                        if (rhs.color == CONFLICT_COLOR && currSegment.day == rhs.day
                                && rhs.startMinute <= currSegment.endMinute + 1) {
                            rhs.startMinute = Math.min(currSegment.startMinute, rhs.startMinute);
                            segments.remove(currSegment);
                            strands.get(currSegment.color).count--;
                            // point at the new current segment
                            currSegment = rhs;
                        }
                    }
                    // if the left side is black merge this with the segment to
                    // the left if they're on the same day and overlap
                    if (i - 1 >= 0) {
                        DNASegment lhs = segments.get(i - 1);
                        if (lhs.color == CONFLICT_COLOR && currSegment.day == lhs.day
                                && lhs.endMinute >= currSegment.startMinute - 1) {
                            lhs.endMinute = Math.max(currSegment.endMinute, lhs.endMinute);
                            segments.remove(currSegment);
                            strands.get(currSegment.color).count--;
                            // point at the new current segment
                            currSegment = lhs;
                            // point i at the new current segment in case new
                            // code is added
                            i--;
                        }
                    }
                    // if we're still not black, decrement the count for the
                    // color being removed, change this to black, and increment
                    // the black count
                    if (currSegment.color != CONFLICT_COLOR) {
                        strands.get(currSegment.color).count--;
                        currSegment.color = CONFLICT_COLOR;
                        strands.get(CONFLICT_COLOR).count++;
                    }
                }

            }
            // If this event extends beyond the last segment add a new segment
            if (endMinute > lastSegment.endMinute) {
                addNewSegment(segments, event, strands, firstJulianDay, lastSegment.endMinute,
                        minMinutes);
            }
        }
        weaveDNAStrands(segments, firstJulianDay, strands, top, bottom, dayXs);
        return strands;
    }

    // This figures out allDay colors as allDay events are found
    private static void addAllDayToStrands(Event event, HashMap<Integer, DNAStrand> strands,
            int firstJulianDay, int numDays) {
        DNAStrand strand = getOrCreateStrand(strands, CONFLICT_COLOR);
        // if we haven't initialized the allDay portion create it now
        if (strand.allDays == null) {
            strand.allDays = new int[numDays];
        }

        // For each day this event is on update the color
        int end = Math.min(event.endDay - firstJulianDay, numDays - 1);
        for (int i = Math.max(event.startDay - firstJulianDay, 0); i <= end; i++) {
            if (strand.allDays[i] != 0) {
                // if this day already had a color, it is now a conflict
                strand.allDays[i] = CONFLICT_COLOR;
            } else {
                // else it's just the color of the event
                strand.allDays[i] = event.color;
            }
        }
    }

    // This processes all the segments, sorts them by color, and generates a
    // list of points to draw
    private static void weaveDNAStrands(LinkedList<DNASegment> segments, int firstJulianDay,
            HashMap<Integer, DNAStrand> strands, int top, int bottom, int[] dayXs) {
        // First, get rid of any colors that ended up with no segments
        Iterator<DNAStrand> strandIterator = strands.values().iterator();
        while (strandIterator.hasNext()) {
            DNAStrand strand = strandIterator.next();
            if (strand.count < 1 && strand.allDays == null) {
                strandIterator.remove();
                continue;
            }
            strand.points = new float[strand.count * 4];
            strand.position = 0;
        }
        // Go through each segment and compute its points
        for (DNASegment segment : segments) {
            // Add the points to the strand of that color
            DNAStrand strand = strands.get(segment.color);
            int dayIndex = segment.day - firstJulianDay;
            int dayStartMinute = segment.startMinute % DAY_IN_MINUTES;
            int dayEndMinute = segment.endMinute % DAY_IN_MINUTES;
            int height = bottom - top;
            int workDayHeight = height * 3 / 4;
            int remainderHeight = (height - workDayHeight) / 2;

            int x = dayXs[dayIndex];
            int y0 = 0;
            int y1 = 0;

            y0 = top + getPixelOffsetFromMinutes(dayStartMinute, workDayHeight, remainderHeight);
            y1 = top + getPixelOffsetFromMinutes(dayEndMinute, workDayHeight, remainderHeight);
            if (DEBUG) {
                Log.d(TAG, "Adding " + Integer.toHexString(segment.color) + " at x,y0,y1: " + x
                        + " " + y0 + " " + y1 + " for " + dayStartMinute + " " + dayEndMinute);
            }
            strand.points[strand.position++] = x;
            strand.points[strand.position++] = y0;
            strand.points[strand.position++] = x;
            strand.points[strand.position++] = y1;
        }
    }

    /**
     * Compute a pixel offset from the top for a given minute from the work day
     * height and the height of the top area.
     */
    private static int getPixelOffsetFromMinutes(int minute, int workDayHeight,
            int remainderHeight) {
        int y;
        if (minute < WORK_DAY_START_MINUTES) {
            y = minute * remainderHeight / WORK_DAY_START_MINUTES;
        } else if (minute < WORK_DAY_END_MINUTES) {
            y = remainderHeight + (minute - WORK_DAY_START_MINUTES) * workDayHeight
                    / WORK_DAY_MINUTES;
        } else {
            y = remainderHeight + workDayHeight + (minute - WORK_DAY_END_MINUTES) * remainderHeight
                    / WORK_DAY_END_LENGTH;
        }
        return y;
    }

    /**
     * Add a new segment based on the event provided. This will handle splitting
     * segments across day boundaries and ensures a minimum size for segments.
     */
    private static void addNewSegment(LinkedList<DNASegment> segments, Event event,
            HashMap<Integer, DNAStrand> strands, int firstJulianDay, int minStart, int minMinutes) {
        if (event.startDay > event.endDay) {
            Log.wtf(TAG, "Event starts after it ends: " + event.toString());
        }
        // If this is a multiday event split it up by day
        if (event.startDay != event.endDay) {
            Event lhs = new Event();
            lhs.color = event.color;
            lhs.startDay = event.startDay;
            // the first day we want the start time to be the actual start time
            lhs.startTime = event.startTime;
            lhs.endDay = lhs.startDay;
            lhs.endTime = DAY_IN_MINUTES - 1;
            // Nearly recursive iteration!
            while (lhs.startDay != event.endDay) {
                addNewSegment(segments, lhs, strands, firstJulianDay, minStart, minMinutes);
                // The days in between are all day, even though that shouldn't
                // actually happen due to the allday filtering
                lhs.startDay++;
                lhs.endDay = lhs.startDay;
                lhs.startTime = 0;
                minStart = 0;
            }
            // The last day we want the end time to be the actual end time
            lhs.endTime = event.endTime;
            event = lhs;
        }
        // Create the new segment and compute its fields
        DNASegment segment = new DNASegment();
        int dayOffset = (event.startDay - firstJulianDay) * DAY_IN_MINUTES;
        int endOfDay = dayOffset + DAY_IN_MINUTES - 1;
        // clip the start if needed
        segment.startMinute = Math.max(dayOffset + event.startTime, minStart);
        // and extend the end if it's too small, but not beyond the end of the
        // day
        int minEnd = Math.min(segment.startMinute + minMinutes, endOfDay);
        segment.endMinute = Math.max(dayOffset + event.endTime, minEnd);
        if (segment.endMinute > endOfDay) {
            segment.endMinute = endOfDay;
        }

        segment.color = event.color;
        segment.day = event.startDay;
        segments.add(segment);
        // increment the count for the correct color or add a new strand if we
        // don't have that color yet
        DNAStrand strand = getOrCreateStrand(strands, segment.color);
        strand.count++;
    }

    /**
     * Try to get a strand of the given color. Create it if it doesn't exist.
     */
    private static DNAStrand getOrCreateStrand(HashMap<Integer, DNAStrand> strands, int color) {
        DNAStrand strand = strands.get(color);
        if (strand == null) {
            strand = new DNAStrand();
            strand.color = color;
            strand.count = 0;
            strands.put(strand.color, strand);
        }
        return strand;
    }

    /**
     * Sends an intent to launch the top level Calendar view.
     *
     * @param context
     */
    public static void returnToCalendarHome(Context context) {
        Intent launchIntent = new Intent(context, AllInOneActivity.class);
        launchIntent.setAction(Intent.ACTION_DEFAULT);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        launchIntent.putExtra(INTENT_KEY_HOME, true);
        context.startActivity(launchIntent);
    }

    /**
     * This sets up a search view to use Calendar's search suggestions provider
     * and to allow refining the search.
     *
     * @param view The {@link SearchView} to set up
     * @param act The activity using the view
     */
    public static void setUpSearchView(SearchView view, Activity act) {
        SearchManager searchManager = (SearchManager) act.getSystemService(Context.SEARCH_SERVICE);
        view.setSearchableInfo(searchManager.getSearchableInfo(act.getComponentName()));
        view.setQueryRefinementEnabled(true);
    }

    /**
     * Given a context and a time in millis since unix epoch figures out the
     * correct week of the year for that time.
     *
     * @param millisSinceEpoch
     * @return
     */
    public static int getWeekNumberFromTime(long millisSinceEpoch, Context context) {
        Time weekTime = new Time(getTimeZone(context, null));
        weekTime.set(millisSinceEpoch);
        weekTime.normalize(true);
        int firstDayOfWeek = getFirstDayOfWeek(context);
        // if the date is on Saturday or Sunday and the start of the week
        // isn't Monday we may need to shift the date to be in the correct
        // week
        if (weekTime.weekDay == Time.SUNDAY
                && (firstDayOfWeek == Time.SUNDAY || firstDayOfWeek == Time.SATURDAY)) {
            weekTime.monthDay++;
            weekTime.normalize(true);
        } else if (weekTime.weekDay == Time.SATURDAY && firstDayOfWeek == Time.SATURDAY) {
            weekTime.monthDay += 2;
            weekTime.normalize(true);
        }
        return weekTime.getWeekNumber();
    }

    /**
     * Formats a day of the week string. This is either just the name of the day
     * or a combination of yesterday/today/tomorrow and the day of the week.
     *
     * @param julianDay The julian day to get the string for
     * @param todayJulianDay The julian day for today's date
     * @param millis A utc millis since epoch time that falls on julian day
     * @param context The calling context, used to get the timezone and do the
     *            formatting
     * @return
     */
    public static String getDayOfWeekString(int julianDay, int todayJulianDay, long millis,
            Context context) {
        getTimeZone(context, null);
        int flags = DateUtils.FORMAT_SHOW_WEEKDAY;
        String dayViewText;
        if (julianDay == todayJulianDay) {
            dayViewText = context.getString(R.string.agenda_today,
                    mTZUtils.formatDateRange(context, millis, millis, flags).toString());
        } else if (julianDay == todayJulianDay - 1) {
            dayViewText = context.getString(R.string.agenda_yesterday,
                    mTZUtils.formatDateRange(context, millis, millis, flags).toString());
        } else if (julianDay == todayJulianDay + 1) {
            dayViewText = context.getString(R.string.agenda_tomorrow,
                    mTZUtils.formatDateRange(context, millis, millis, flags).toString());
        } else {
            dayViewText = mTZUtils.formatDateRange(context, millis, millis, flags).toString();
        }
        dayViewText = dayViewText.toUpperCase();
        return dayViewText;
    }

    // Calculate the time until midnight + 1 second and set the handler to
    // do run the runnable
    public static void setMidnightUpdater(Handler h, Runnable r, String timezone) {
        if (h == null || r == null || timezone == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Time time = new Time(timezone);
        time.set(now);
        long runInMillis = (24 * 3600 - time.hour * 3600 - time.minute * 60 -
                time.second + 1) * 1000;
        h.removeCallbacks(r);
        h.postDelayed(r, runInMillis);
    }

    // Stop the midnight update thread
    public static void resetMidnightUpdater(Handler h, Runnable r) {
        if (h == null || r == null) {
            return;
        }
        h.removeCallbacks(r);
    }

    /**
     * Returns a string description of the specified time interval.
     */
    public static String getDisplayedDatetime(long startMillis, long endMillis, long currentMillis,
            String localTimezone, boolean allDay, Context context) {
        // Configure date/time formatting.
        /*2012-11-23, add by amt_jiayuanyuan for SWITCHUITWO-112 */
        int flagsDate = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY;
        /*2012-11-23, add end*/
        int flagsTime = DateUtils.FORMAT_SHOW_TIME;
        if (DateFormat.is24HourFormat(context)) {
            flagsTime |= DateUtils.FORMAT_24HOUR;
        }

        Time currentTime = new Time(localTimezone);
        currentTime.set(currentMillis);
        Resources resources = context.getResources();
        String datetimeString = null;
        if (allDay) {
            // All day events require special timezone adjustment.
            long localStartMillis = convertAlldayUtcToLocal(null, startMillis, localTimezone);
            long localEndMillis = convertAlldayUtcToLocal(null, endMillis, localTimezone);
            if (singleDayEvent(localStartMillis, localEndMillis, currentTime.gmtoff)) {
                // If possible, use "Today" or "Tomorrow" instead of a full date string.
                int todayOrTomorrow = isTodayOrTomorrow(context.getResources(),
                        localStartMillis, currentMillis, currentTime.gmtoff);
                if (TODAY == todayOrTomorrow) {
                    datetimeString = resources.getString(R.string.today);
                } else if (TOMORROW == todayOrTomorrow) {
                    datetimeString = resources.getString(R.string.tomorrow);
                }
            }
            if (datetimeString == null) {
                // For multi-day allday events or single-day all-day events that are not
                // today or tomorrow, use framework formatter.
                Formatter f = new Formatter(new StringBuilder(50), Locale.getDefault());
                datetimeString = DateUtils.formatDateRange(context, f, startMillis,
                        endMillis, flagsDate, Time.TIMEZONE_UTC).toString();
            }
        } else {
            if (singleDayEvent(startMillis, endMillis, currentTime.gmtoff)) {
                // Format the time.
                String timeString = Utils.formatDateRange(context, startMillis, endMillis,
                        flagsTime);

                // If possible, use "Today" or "Tomorrow" instead of a full date string.
                int todayOrTomorrow = isTodayOrTomorrow(context.getResources(), startMillis,
                        currentMillis, currentTime.gmtoff);
                if (TODAY == todayOrTomorrow) {
                    // Example: "Today at 1:00pm - 2:00 pm"
                    datetimeString = resources.getString(R.string.today_at_time_fmt,
                            timeString);
                } else if (TOMORROW == todayOrTomorrow) {
                    // Example: "Tomorrow at 1:00pm - 2:00 pm"
                    datetimeString = resources.getString(R.string.tomorrow_at_time_fmt,
                            timeString);
                } else {
                    // Format the full date. Example: "Thursday, April 12, 1:00pm - 2:00pm"
                    String dateString = Utils.formatDateRange(context, startMillis, endMillis,
                            flagsDate);
                    datetimeString = resources.getString(R.string.date_time_fmt, dateString,
                            timeString);
                }
            } else {
                // For multiday events, shorten day/month names.
                // Example format: "Fri Apr 6, 5:00pm - Sun, Apr 8, 6:00pm"
                int flagsDatetime = flagsDate | flagsTime | DateUtils.FORMAT_ABBREV_MONTH |
                        DateUtils.FORMAT_ABBREV_WEEKDAY;
                datetimeString = Utils.formatDateRange(context, startMillis, endMillis,
                        flagsDatetime);
            }
        }
        return datetimeString;
    }

    /**
     * Returns the timezone to display in the event info, if the local timezone is different
     * from the event timezone.  Otherwise returns null.
     */
    public static String getDisplayedTimezone(long startMillis, String localTimezone,
            String eventTimezone) {
        String tzDisplay = null;
        if (!TextUtils.equals(localTimezone, eventTimezone)) {
            // Figure out if this is in DST
            TimeZone tz = TimeZone.getTimeZone(localTimezone);
            if (tz == null || tz.getID().equals("GMT")) {
                tzDisplay = localTimezone;
            } else {
                Time startTime = new Time(localTimezone);
                startTime.set(startMillis);
                tzDisplay = tz.getDisplayName(startTime.isDst != 0, TimeZone.SHORT);
            }
        }
        return tzDisplay;
    }

    /**
     * Returns whether the specified time interval is in a single day.
     */
    private static boolean singleDayEvent(long startMillis, long endMillis, long localGmtOffset) {
        if (startMillis == endMillis) {
            return true;
        }

        // An event ending at midnight should still be a single-day event, so check
        // time end-1.
        int startDay = Time.getJulianDay(startMillis, localGmtOffset);
        int endDay = Time.getJulianDay(endMillis - 1, localGmtOffset);
        return startDay == endDay;
    }

    // Using int constants as a return value instead of an enum to minimize resources.
    private static final int TODAY = 1;
    private static final int TOMORROW = 2;
    private static final int NONE = 0;

    /**
     * Returns TODAY or TOMORROW if applicable.  Otherwise returns NONE.
     */
    private static int isTodayOrTomorrow(Resources r, long dayMillis,
            long currentMillis, long localGmtOffset) {
        int startDay = Time.getJulianDay(dayMillis, localGmtOffset);
        int currentDay = Time.getJulianDay(currentMillis, localGmtOffset);

        int days = startDay - currentDay;
        if (days == 1) {
            return TOMORROW;
        } else if (days == 0) {
            return TODAY;
        } else {
            return NONE;
        }
    }

    /**
     * Create an intent for emailing attendees of an event.
     *
     * @param resources The resources for translating strings.
     * @param eventTitle The title of the event to use as the email subject.
     * @param body The default text for the email body.
     * @param toEmails The list of emails for the 'to' line.
     * @param ccEmails The list of emails for the 'cc' line.
     * @param ownerAccount The owner account to use as the email sender.
     */
    public static Intent createEmailAttendeesIntent(Resources resources, String eventTitle,
            String body, List<String> toEmails, List<String> ccEmails, String ownerAccount) {
        List<String> toList = toEmails;
        List<String> ccList = ccEmails;
        if (toEmails.size() <= 0) {
            if (ccEmails.size() <= 0) {
                // TODO: Return a SEND intent if no one to email to, to at least populate
                // a draft email with the subject (and no recipients).
                throw new IllegalArgumentException("Both toEmails and ccEmails are empty.");
            }

            // Email app does not work with no "to" recipient.  Move all 'cc' to 'to'
            // in this case.
            toList = ccEmails;
            ccList = null;
        }

        // Use the event title as the email subject (prepended with 'Re: ').
        String subject = null;
        if (eventTitle != null) {
            subject = resources.getString(R.string.email_subject_prefix) + eventTitle;
        }

        // Use the SENDTO intent with a 'mailto' URI, because using SEND will cause
        // the picker to show apps like text messaging, which does not make sense
        // for email addresses.  We put all data in the URI instead of using the extra
        // Intent fields (ie. EXTRA_CC, etc) because some email apps might not handle
        // those (though gmail does).
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("mailto");

        // We will append the first email to the 'mailto' field later (because the
        // current state of the Email app requires it).  Add the remaining 'to' values
        // here.  When the email codebase is updated, we can simplify this.
        if (toList.size() > 1) {
            for (int i = 1; i < toList.size(); i++) {
                // The Email app requires repeated parameter settings instead of
                // a single comma-separated list.
                uriBuilder.appendQueryParameter("to", toList.get(i));
            }
        }

        // Add the subject parameter.
        if (subject != null) {
            uriBuilder.appendQueryParameter("subject", subject);
        }

        // Add the subject parameter.
        if (body != null) {
            uriBuilder.appendQueryParameter("body", body);
        }

        // Add the cc parameters.
        if (ccList != null && ccList.size() > 0) {
            for (String email : ccList) {
                uriBuilder.appendQueryParameter("cc", email);
            }
        }

        // Insert the first email after 'mailto:' in the URI manually since Uri.Builder
        // doesn't seem to have a way to do this.
        String uri = uriBuilder.toString();
        if (uri.startsWith("mailto:")) {
            StringBuilder builder = new StringBuilder(uri);
            builder.insert(7, Uri.encode(toList.get(0)));
            uri = builder.toString();
        }

        // Start the email intent.  Email from the account of the calendar owner in case there
        // are multiple email accounts.
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SENDTO, Uri.parse(uri));
        emailIntent.putExtra("fromAccountString", ownerAccount);
        return Intent.createChooser(emailIntent, resources.getString(R.string.email_picker_label));
    }

    /**
     * Example fake email addresses used as attendee emails are resources like conference rooms,
     * or another calendar, etc.  These all end in "calendar.google.com".
     */
    public static boolean isValidEmail(String email) {
        return email != null && !email.endsWith(MACHINE_GENERATED_ADDRESS);
    }

    /**
     * Returns true if:
     *   (1) the email is not a resource like a conference room or another calendar.
     *       Catch most of these by filtering out suffix calendar.google.com.
     *   (2) the email is not equal to the sync account to prevent mailing himself.
     */
    public static boolean isEmailableFrom(String email, String syncAccountName) {
        return Utils.isValidEmail(email) && !email.equals(syncAccountName);
    }

    /**
     * Inserts a drawable with today's day into the today's icon in the option menu
     * @param icon - today's icon from the options menu
     */
    public static void setTodayIcon(LayerDrawable icon, Context c, String timezone) {
        DayOfMonthDrawable today;

        // Reuse current drawable if possible
        Drawable currentDrawable = icon.findDrawableByLayerId(R.id.today_icon_day);
        if (currentDrawable != null && currentDrawable instanceof DayOfMonthDrawable) {
            today = (DayOfMonthDrawable)currentDrawable;
        } else {
            today = new DayOfMonthDrawable(c);
        }
        // Set the day and update the icon
        Time now =  new Time(timezone);
        now.setToNow();
        now.normalize(false);
        today.setDayOfMonth(now.monthDay);
        icon.mutate();
        icon.setDrawableByLayerId(R.id.today_icon_day, today);
    }

    private static class CalendarBroadcastReceiver extends BroadcastReceiver {

        Runnable mCallBack;

        public CalendarBroadcastReceiver(Runnable callback) {
            super();
            mCallBack = callback;
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_DATE_CHANGED) ||
                    intent.getAction().equals(Intent.ACTION_TIME_CHANGED) ||
                    intent.getAction().equals(Intent.ACTION_LOCALE_CHANGED) ||
                    intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                if (mCallBack != null) {
                    mCallBack.run();
                }
            }
        }
    }

    public static BroadcastReceiver setTimeChangesReceiver(Context c, Runnable callback) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);

        CalendarBroadcastReceiver r = new CalendarBroadcastReceiver(callback);
        c.registerReceiver(r, filter);
        return r;
    }

    public static void clearTimeChangesReceiver(Context c, BroadcastReceiver r) {
        c.unregisterReceiver(r);
    }

    /**
     * Get a list of quick responses used for emailing guests from the
     * SharedPreferences. If not are found, get the hard coded ones that shipped
     * with the app
     *
     * @param context
     * @return a list of quick responses.
     */
    public static String[] getQuickResponses(Context context) {
        String[] s = Utils.getSharedPreference(context, KEY_QUICK_RESPONSES, (String[]) null);

        if (s == null) {
            s = context.getResources().getStringArray(R.array.quick_response_defaults);
        }

        return s;
    }
    
    /*  Begin Motorola
     *   BELOW are for Lunar calendar
     */
     //Lunar calendar
     final private static int LUNAR_START_YEAR = 1901;
     final private static int LUNAR_END_YEAR = 2100;

     public static class LunarDateInfo {
         public int lYear;
         public int lMonth;
         public int lDay;
         public boolean leap = false;
         public boolean lessSpanFlag = false;
         public LunarDateInfo() {
         }
     }

     public static class LunarInfo {
         /**
          * Provides functions to calculate Chinese lunar calendar date based on
          *   western calendar.
          * FIXME: Should it be in native code?
          */
         public String lunarStr;
         public int lunarType;
         public static final int LUNAR_TYPE_NORMAL = 0;
         public static final int LUNAR_TYPE_SOLAR_TEAM = 1;
         public static final int LUNAR_TYPE_FESTIVAL = 2;
         public static final int SOLAR_TYPE_HOLIDAY = 3;
         public LunarInfo() {
         }
     }

     /*  gLunarMonthDay stores days information from 1901 to 2100.
     *  In Lunar Calendar, each month has 29 days or 30 days, each year has 12
     *  or 13 months. Here uses higher 12 or 13 bits to represent each month
     *  in a year, 1: 30 days, 0: 29 days.
     */

     final private static int gLunarMonthDay[] =
     {
         0X4ae0, 0Xa570, 0X5268, 0Xd260, 0Xd950, 0X6aa8, 0X56a0, 0X9ad0, 0X4ae8, 0X4ae0,   //1901-1910
         0Xa4d8, 0Xa4d0, 0Xd250, 0Xd528, 0Xb540, 0Xd6a0, 0X96d0, 0X95b0, 0X49b8, 0X4970,   //1911-1920
         0Xa4b0, 0Xb258, 0X6a50, 0X6d40, 0Xada8, 0X2b60, 0X9570, 0X4978, 0X4970, 0X64b0,   //1921-1930
         0Xd4a0, 0Xea50, 0X6d48, 0X5ad0, 0X2b60, 0X9370, 0X92e0, 0Xc968, 0Xc950, 0Xd4a0,   //1931-1940
         0Xda50, 0Xb550, 0X56a0, 0Xaad8, 0X25d0, 0X92d0, 0Xc958, 0Xa950, 0Xb4a8, 0X6ca0,   //1941-1950
         0Xb550, 0X55a8, 0X4da0, 0Xa5b0, 0X52b8, 0X52b0, 0Xa950, 0Xe950, 0X6aa0, 0Xad50,   //1951-1960
         0Xab50, 0X4b60, 0Xa570, 0Xa570, 0X5260, 0Xe930, 0Xd950, 0X5aa8, 0X56a0, 0X96d0,   //1961-1970
         0X4ae8, 0X4ad0, 0Xa4d0, 0Xd268, 0Xd250, 0Xd528, 0Xb540, 0Xb6a0, 0X96d0, 0X95b0,   //1971-1980
         0X49b0, 0Xa4b8, 0Xa4b0, 0Xb258, 0X6a50, 0X6d40, 0Xada0, 0Xab60, 0X9570, 0X4978,   //1981-1990
         0X4970, 0X64b0, 0X6a50, 0Xea50, 0X6b28, 0X5ac0, 0Xab60, 0X9368, 0X92e0, 0Xc960,   //1991-2000
         0Xd4a8, 0Xd4a0, 0Xda50, 0X5aa8, 0X56a0, 0Xaad8, 0X25d0, 0X92d0, 0Xc958, 0Xa950,   //2001-2010
         0Xb4a0, 0Xb550, 0Xad50, 0X55a8, 0X4ba0, 0Xa5b0, 0X52b8, 0X52b0, 0Xa930, 0X74a8,   //2011-2020
         0X6aa0, 0Xad50, 0X4da8, 0X4b60, 0Xa570, 0Xa4e0, 0Xd260, 0Xe930, 0Xd530, 0X5aa0,   //2021-2030
         0X6b50, 0X96d0, 0X4ae8, 0X4ad0, 0Xa4d0, 0Xd258, 0Xd250, 0Xd520, 0Xdaa0, 0Xb5a0,   //2031-2040
         0X56d0, 0X4ad8, 0X49b0, 0Xa4b8, 0Xa4b0, 0Xaa50, 0Xb528, 0X6d20, 0Xada0, 0X55b0,   //2041-2050
         0X9370, 0X4978, 0X4970, 0X64b0, 0X6a50, 0Xea50, 0X6aa0, 0Xab60, 0Xaae0, 0X92e0,   //2051-2060
         0Xc970, 0Xc960, 0Xd4a8, 0Xd4a0, 0Xda50, 0X5aa8, 0X56a0, 0Xa6d0, 0X52e8, 0X52d0,   //2061-2070
         0Xa958, 0Xa950, 0Xb4a0, 0Xb550, 0Xad50, 0X55a0, 0Xa5d0, 0Xa5b0, 0X52b0, 0Xa938,   //2071-2080
         0X6930, 0X7298, 0X6aa0, 0Xad50, 0X4da8, 0X4b60, 0Xa570, 0X5270, 0Xd160, 0Xe930,   //2081-2090
         0Xd520, 0Xdaa0, 0X6b50, 0X56d0, 0X4ae0, 0Xa4e8, 0Xa2d0, 0Xd150, 0Xd928, 0Xd520    //2091-2100
     };

     // gLunarMonth stores leap months from 1901 to 2050.
     //   one byte stores two years leap month
     final private static byte gLunarMonth[] =
     {
         0X00, 0X50, 0X04, 0X00, 0X20,   //1901-1910
         0X60, 0X05, 0X00, 0X20, 0X70,   //1911-1920
         0X05, 0X00, 0X40, 0X02, 0X06,   //1921-1930
         0X00, 0X50, 0X03, 0X07, 0X00,   //1931-1940
         0X60, 0X04, 0X00, 0X20, 0X70,   //1941-1950
         0X05, 0X00, 0X30, -128, 0X06,   //1951-1960
         0X00, 0X40, 0X03, 0X07, 0X00,   //1961-1970
         0X50, 0X04, 0X08, 0X00, 0X60,   //1971-1980
         0X04, 0X0a, 0X00, 0X60, 0X05,   //1981-1990
         0X00, 0X30, -128, 0X05, 0X00,   //1991-2000
         0X40, 0X02, 0X07, 0X00, 0X50,   //2001-2010
         0X04, 0X09, 0X00, 0X60, 0X04,   //2011-2020
         0X00, 0X20, 0X60, 0X05, 0X00,   //2021-2030
         0X30, -80, 0X06, 0X00, 0X50,   //2031-2040
         0X02, 0X07, 0X00, 0X50, 0X03,   //2041-2050
         0X08, 0X00, 0X60, 0X04, 0X00,   //2051-2060
         0X30, 0X70, 0X05, 0X00, 0X40,   //2061-2070
         -128, 0X06, 0X00, 0X40, 0X03,   //2071-2080
         0X07, 0X00, 0X50, 0X04, 0X08,   //2081-2090
         0X00, 0X60, 0X04, 0X00, 0X20    //2091-2100
     };



     final private static int gLunarFestivalInfo[] = { 0x0100,// "0100"
             0x0101,// "0101"   //for chunjie
             0x010F,// "0115"
             0x0505,// "0505"
             0x0707,// "0707"
             0x070F,// "0715"
             0x080F,// "0815"
             0x0909,// "0909"
             0x0C08,// "1208"
             0x0C17,// "1223"
     };



     final private static int gSolarTermOffsetInfo[] ={
     0,21208,42467,63836,
     85337,107014,128867,150921,
     173149,195551,218072,240693,
     263343,285989,308563,331033,
     353350,375494,397447,419210,
     440795,462224,483532,504758
     };
     //Locale Chinese
     public static boolean isDefaultLocaleChinese() {
         Locale l = Locale.getDefault();
         return l.getLanguage().equals("zh");
     }

     public static boolean isDefaultLocaleTWHK() {
         Locale l = Locale.getDefault();
         if( (l.getCountry().equals("TW")) || (l.getCountry().equals("HK"))) {
         	return true;
         } else {
         	return false;
         }
     }
     
     public static boolean isTWHKBuild() {
     	String l = "zh";//MotoSystemProperties.get("ro.product.locale.region");
     	if((l != null) && l.equals("TW")) { 
     		return true;
     	} else {
     		return false;
     	}
     }
     
     //Locale Korea
     public static boolean isLocaleKorean() {
         Locale locale = Locale.getDefault();
         String curLanguage = locale.getLanguage();
         return curLanguage.equals("ko");
     }

     public static String formatMonthYear2(Context context, Time time) {
         final long start = time.toMillis(false /* use isDst */);
         long end = start;
         return DateUtils.formatDateRange(context, start, end,
                 DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NO_MONTH_DAY);
     }

     //fwr687 begin IKSF-3317 get solar holiday by date
     public static String getSolarHolidayByDate(Context context,int m, int d) {
     	String [] solarHolidayArray = null;
     	if(isTWHKBuild() && isDefaultLocaleTWHK()) {
     		solarHolidayArray = context.getResources().getStringArray(R.array.solar_holiday_tw);
     	} else {
     		solarHolidayArray = context.getResources().getStringArray(R.array.solar_holiday);
     	}
         String solarHoliday = "";
         for (int i = 0; i < solarHolidayArray.length; i++) {
             if (Integer.parseInt(solarHolidayArray[i].substring(0, 2)) == m+1
                     && Integer.parseInt(solarHolidayArray[i].substring(2, 4)) == d) {            	
                 solarHoliday = solarHolidayArray[i].substring(5);
             }
         }
         return solarHoliday;
     }
     //fwr687 end

     // get lunar festival by lunar date
     public static int getLunarFestivalByLunarDate(int m, int d, int days) {
         int temp;
         int retVal = -1;
         int lm = m;
         int ld = d;

         if((m==12)&&(d>=29)){//for chuxi, the one day before new year.
             LunarDateInfo  lunarDate = days2lunarDate(days+1);
             if((lunarDate.lMonth==1)&&(lunarDate.lDay==1)){
                 lm = 1;
                 ld = 0;
             }
         }
         temp=(lm<<8)|(ld&0xFF);
         for(int i=0;i<gLunarFestivalInfo.length;i++)
             if(gLunarFestivalInfo[i]==temp)
                 retVal=i;
                 //.lunar_festivals[i];
         return retVal;
     }

     //y year n solar term get solar term offset day by date (begin with 0)
     private static int getSolarTermOffsetDayByDate(int y, int n, Context c) {
     	
     	String[] termExArray= c.getResources().getStringArray(R.array.term_exceptions);
     	String exTerm = termExArray[n];
     	String year = (new Integer(y)).toString();
     	int index = -1;
     	if((index = exTerm.indexOf(year)) != -1){
     		int beginIndex = index + year.length()+1;
     		int endIndex = exTerm.indexOf(" ", beginIndex);
     		String day;
     		if(endIndex > 0) {
     		    day = exTerm.substring(beginIndex, endIndex);
     		} else {
     		    day = exTerm.substring(beginIndex);	
     		}
     		return ((new Integer(day)).intValue());
     	}
     	
         Calendar cal = Calendar.getInstance();
         cal.set(1900, 0, 6, 2, 5, 0);
         long temp = cal.getTime().getTime();
         if(n<0||n>23)
             temp = 0;
         cal.setTime(new Date((long) ((31556925974.7 * (y - 1900) + gSolarTermOffsetInfo[n] * 60000L) + temp)));
         return cal.get(Calendar.DAY_OF_MONTH);
     }

     public static int getSolarTermByDate(int y, int m, int d, Context c) {
//         Log.v("Utils"," getSolarTermByDate year= "+y+" month= "+m+" day= "+d);
         int solarTerm;
         Log.e(TAG,"m " + m + " d:" + d);
         if (m < 0 || m > 11) {
             solarTerm = -1;
             if (DEBUG)Log.v("Utils"," getSolarTermByDate invalid month");
         }
         m++;//for month 0-11;
         if (d == getSolarTermOffsetDayByDate(y, (m - 1) * 2, c)) {
             if (DEBUG)Log.v("Utils"," getSolarTermByDate get month = "+m+" solar_term_num= "+(m-1)*2+" day= "+d+ " got_terms= "+(m - 1) * 2);
             solarTerm = (m - 1) * 2;
         } else if (d == getSolarTermOffsetDayByDate(y, (m - 1) * 2 + 1, c)) {
             if (DEBUG)Log.v("Utils"," getSolarTermByDate get month = "+m+" solar_term_num= "+(m-1)*2+1+" day= "+d+ " got_terms= "+(m - 1) * 2+1);
             solarTerm = (m - 1) * 2 + 1;
         } else { // not solar term
             //Log.v("Utils"," getSolarTermByDate not match solar term ");
             solarTerm = -1;
         }
         return solarTerm;
     }

     /**
      * Gets the GanZhi and ShengXiao.
      * @param year
      * @return
      */
     final private static String lunarYearName(Context context, int year) {
         int num = year - 4;
         int temp = num % 12;
         if (DEBUG)Log.v(TAG,"year = "+year+" num = "+num+" temp = "+temp);

         String [] tianganArray= context.getResources().getStringArray(R.array.tiangan);
         String [] dizhiArray= context.getResources().getStringArray(R.array.dizhi);
         String [] shengxiaoArray= context.getResources().getStringArray(R.array.shengxiao);
         return (tianganArray[num % 10] + dizhiArray[temp] + "(" + shengxiaoArray[temp] + ")"+context.getResources().getString(R.string.year));
     }

     /**
      * Gets the month name in lunar.
      * @param month
      * @return
      */
     final private static String lunarMonthName(Context context, int month, boolean leap) {
         if (month<1 || month>12)
         {
             if (DEBUG)android.util.Log.e("CalUtils", "  illegal month: " + month);
             return "";
         }

         int num = month - 1;

         // Leap month has no special expression per CxD design.
         //        return leap ?
         //               (context.getString(R.string.leap) + context.getString(lunarMonths[num])) :
         //               context.getString(lunarMonths[num]);
         String [] lunarMonthArray= context.getResources().getStringArray(R.array.lunar_month);
         return lunarMonthArray[num];
     }

     /**
      * Gets the monthday name in lunar.
      * @param monthday
      * @return
      */
     final private static String lunarMonthdayName(Context context, int monthday) {
         if(monthday<1 || monthday>30)
         {
             if (DEBUG)android.util.Log.e("CalUtils", "  illegal monthday: " + monthday);
             return "";
         }

         int index1;
         index1 = monthday / 10;
         if(monthday == 10)
             index1 = 4;
         else if(monthday == 20)
             index1 = 5;

         String [] lunarDayPart1Array= context.getResources().getStringArray(R.array.lunar_day_part1);
         String [] lunarDayPart2Array= context.getResources().getStringArray(R.array.lunar_day_part2);
         return (lunarDayPart1Array[index1] + lunarDayPart2Array[monthday%10]);
     }


     /**
      *
      */
     private static LunarDateInfo days2lunarDate(int days) {
         int spanDays;
         LunarDateInfo lunarDate = new LunarDateInfo();

         spanDays = days;
         if(spanDays < 49)
         {
             lunarDate.lYear = LUNAR_START_YEAR - 1;
             if(spanDays < 19)
             {
                 lunarDate.lMonth = 11;
                 lunarDate.lDay = 11 + spanDays;
             }
             else
             {
                 lunarDate.lMonth = 12;
                 lunarDate.lDay = spanDays - 18;
             }

             if (DEBUG)android.util.Log.i("CalUtils", "  lunar date result: Y" + lunarDate.lYear + ", M" + lunarDate.lMonth + ", D" + lunarDate.lDay);
             lunarDate.lessSpanFlag = true;
             return lunarDate;
         }

         spanDays -= 49;
         lunarDate.lYear = LUNAR_START_YEAR;
         lunarDate.lMonth = 1;
         lunarDate.lDay = 1;

         // Just speed up the calculation if the date is later than Lunar Calendar Date 2009/1/1.
         //   The distance from (lunar) 1901/1/1 to 2009/1/1 is 39423 days.
         //   It is more common in using, and could be removed freely.
         if(spanDays > 39423)
         {
             lunarDate.lYear = 2009;
             spanDays -= 39423;
         }

         // Year
         int tmp = daysInLunarYear(lunarDate.lYear);
         while(spanDays >= tmp)
         {
             spanDays -= tmp;
             tmp = daysInLunarYear(++lunarDate.lYear);
         }

         // Month
         tmp = daysInLunarMonth(lunarDate.lYear, lunarDate.lMonth)  & 0x00ffff;
         int leapMonthInThisYear = leapMonthInYear(lunarDate.lYear);
         while(spanDays >= tmp)
         {
             spanDays -= tmp;
             if(lunarDate.lMonth == leapMonthInThisYear)
             {
                 //tmp  = HIWORD(daysInLunarMonth(lYear, lMonth));
                 tmp = daysInLunarMonth(lunarDate.lYear, lunarDate.lMonth) >> 16;
                 if(spanDays < tmp)
                 {
                     lunarDate.leap = true;
                     break;
                 }
                 spanDays -= tmp;
             }
             tmp = daysInLunarMonth(lunarDate.lYear, ++lunarDate.lMonth) & 0x00ffff;
         }

         // Day
         lunarDate.lDay += spanDays;
         return lunarDate;
     }

     //fwr687 begin
     public static String lunarYear(Context context, Time time) {
         String lunar = "";
         if (time.year < LUNAR_START_YEAR || time.year > LUNAR_END_YEAR)
         {
             if (DEBUG)android.util.Log.e("CalUtils", "  illegal year: " + time.year);
             return lunar;
         }

         // Gregorian Calendar date 1901/2/19 is Lunar Calendar date 1901/1/1.
         //   the days difference between Gregorian Calendar Date 1/1 and 2/19
         //   is 49 days.

         Calendar calen = Calendar.getInstance();
         calen.set(time.year, time.month, time.monthDay);
         float spanMills;
         spanMills = calen.getTimeInMillis();
         calen.set(1901, 0, 1);
         spanMills -= calen.getTimeInMillis();
         //fwr687 beign IKSF-6402
         BigDecimal num = new BigDecimal(spanMills / (86400000L)).setScale(0, BigDecimal.ROUND_HALF_UP);
         int days = num.intValue();
         //fwr687 end

         LunarDateInfo  lunarDate = days2lunarDate(days);
         if (DEBUG)android.util.Log.i("CalUtils", "  lunar date result: Y" + lunarDate.lYear );
         lunar = lunarYearName(context, lunarDate.lYear);

         return lunar;
     }
     //fwr687 end
     /**
      * Provides functions to calculate Chinese lunar calendar month based on
      *   western calendar.
      * FIXME: Should it be in native code?
      */
     public static String lunarMonth(Context context, Time time) {
         String lunar = "";
         if (time.year < LUNAR_START_YEAR || time.year > LUNAR_END_YEAR)
         {
             if (DEBUG)android.util.Log.e("CalUtils", "  illegal year: " + time.year);
             return lunar;
         }

         // Gregorian Calendar date 1901/2/19 is Lunar Calendar date 1901/1/1.
         //   the days difference between Gregorian Calendar Date 1/1 and 2/19
         //   is 49 days.

         Calendar calen = Calendar.getInstance();
         calen.set(time.year, time.month, time.monthDay);
         float spanMills;
         spanMills = calen.getTimeInMillis();
         calen.set(1901, 0, 1);
         spanMills -= calen.getTimeInMillis();
         //fwr687 beign IKSF-6402
         BigDecimal num = new BigDecimal(spanMills / (86400000L)).setScale(0, BigDecimal.ROUND_HALF_UP);
         int days = num.intValue();
         //fwr687 end

         // FIXME: Any DST issue?
         // if( calen.add(days) != time )
         //     android.util.Log.e("CalUtils", "    We got troubles in days diff!!!");
         // TODO: And shall we use the yeardays to calculate the span days?
         // if (year1 == year2) days = yearday2 - yearday1;
         // else {sum();}

         // android.util.Log.i("CalUtils", "  span mills and span days are: " + spanMills + ", " + days);

         LunarDateInfo  lunarDate = days2lunarDate(days);
         if (DEBUG)android.util.Log.i("CalUtils", "  lunar date result: Y" + lunarDate.lYear + ", M" + lunarDate.lMonth + ", D" + lunarDate.lDay);

         if (lunarDate.lessSpanFlag == true)
             lunar = lunarYearName(context, lunarDate.lYear) + lunarMonthName(context, lunarDate.lMonth, false);
         else
             lunar = lunarYearName(context, lunarDate.lYear) + lunarMonthName(context, lunarDate.lMonth, lunarDate.leap);
         return lunar;
     }



     public static LunarInfo monthlyViewLunarDate(Context context, Time time,boolean showHoliday) {
         if (time.year < LUNAR_START_YEAR || time.year > LUNAR_END_YEAR)
         {
             if (DEBUG)android.util.Log.e("CalUtils", "  illegal year: " + time.year);
             return null;
         }

         LunarInfo lunarData = new LunarInfo();

         // Gregorian Calendar date 1901/2/19 is Lunar Calendar date 1901/1/1.
         //   the days difference between Gregorian Calendar Date 1/1 and 2/19
         //   is 49 days.

         Calendar calen = Calendar.getInstance();
         calen.set(time.year, time.month, time.monthDay);
         float spanMills;
         spanMills = calen.getTimeInMillis();
         calen.set(1901, 0, 1);
         spanMills -= calen.getTimeInMillis();
         //fwr687 begin IKSF-6402
         BigDecimal num = new BigDecimal(spanMills / (86400000L)).setScale(0, BigDecimal.ROUND_HALF_UP);//fwr687 IKSF-6402
         int days =  num.intValue();
         //fwr687 end

         // FIXME: Any DST issue?
         // if( calen.add(days) != time )
         //     android.util.Log.e("CalUtils", "    We got troubles in days diff!!!");
         // TODO: And shall we use the yeardays to calculate the span days?
         // if (year1 == year2) days = yearday2 - yearday1;
         // else {sum();}

         // android.util.Log.i("CalUtils", "  span mills and span days are: " + spanMills + ", " + days);

         LunarDateInfo  lunarDate = days2lunarDate(days);
         //fwr687 begin IKSF-3317
         if(showHoliday){
             int lunarFestivalIndex = getLunarFestivalByLunarDate(lunarDate.lMonth,lunarDate.lDay,days);
             if(lunarFestivalIndex!=-1){
                 String [] lunarFestivalArray= context.getResources().getStringArray(R.array.lunar_festival);
                 lunarData.lunarStr =  lunarFestivalArray[lunarFestivalIndex];
                 lunarData.lunarType = LunarInfo.LUNAR_TYPE_FESTIVAL;
                 return lunarData;
             }
           //When the date of solar holiday overlap with lunar holiday,display the lunar holiday as higher priority
             String solarHoliday = Utils.getSolarHolidayByDate(context,time.month,time.monthDay);
             if(!TextUtils.isEmpty(solarHoliday)){
                 lunarData.lunarStr = solarHoliday;
                 lunarData.lunarType = LunarInfo.SOLAR_TYPE_HOLIDAY;
                 return lunarData;
             }
         }
         //fwr687 end
         int solarTermIndex=getSolarTermByDate(time.year, time.month, time.monthDay, context);
         if(solarTermIndex!=-1){
             String [] solarTermArray= context.getResources().getStringArray(R.array.solar_term);
             lunarData.lunarStr = solarTermArray[solarTermIndex];
             lunarData.lunarType = LunarInfo.LUNAR_TYPE_SOLAR_TEAM;
             return lunarData;
         }

         if (lunarDate.lDay == 1) {
             if (lunarDate.lessSpanFlag == true)
                 lunarData.lunarStr = lunarMonthName(context, lunarDate.lMonth, false);
             else
                 lunarData.lunarStr = lunarMonthName(context, lunarDate.lMonth, lunarDate.leap);
         } else
             lunarData.lunarStr = lunarMonthdayName(context, lunarDate.lDay);
         lunarData.lunarType = LunarInfo.LUNAR_TYPE_NORMAL;
         return lunarData;
     }

     /**
      * Calculates the days in a lunar calendar year.
      */
     public static int daysInLunarYear(int year) {
         int days = 0;
         for( int i=1; i<=12; i++ )
         {
             int tmp = daysInLunarMonth(year, i);
             days += (tmp >> 16);
             days += (tmp) & 0xFF;
         }
         return days;
     }

     /**
      * Gets days of LunarMonth in LunarYear. If LunarMonth is a leap month,
      *   higher 16 bit is the days of next LunarMonth; Otherwise, higher 16 bit is 0.
      */
     public static int daysInLunarMonth(int year, int month) {
         if (( year < LUNAR_START_YEAR ) || ( year > LUNAR_END_YEAR ))
             return 30;

         int high = 0;
         int low = 29;
         int iBit = 16 - month;

         int leapMonths = leapMonthInYear(year);
         if((leapMonths > 0) && (month > leapMonths))
             iBit --;

         int spanYears = year - LUNAR_START_YEAR;
         if((gLunarMonthDay[spanYears] & (1<<iBit)) != 0)
             low ++;

         if(month == leapMonths)
         {
             if((gLunarMonthDay[spanYears] & (1<<(iBit-1))) != 0)
                  high = 30;
             else
                  high = 29;
         }

         int result = high;
         result = result << 16;
         result |= low;
         return result;
     }

     /**
      * Gets the leap month in a lunar year.
      */
     public static int leapMonthInYear(int year) {
         int index = (year - LUNAR_START_YEAR) / 2;
         if (index >= gLunarMonth.length)
         {
             if (DEBUG)android.util.Log.e("CalUtils", "  Fault - leap month index illegal!");
             return 0;
         }
         byte flag = gLunarMonth[index];
         if((year - LUNAR_START_YEAR)%2 != 0)
              return (int) flag & 0xFF;
         else
              return (int) (flag>>4) & 0x0F;
     }
     /**
      * Provides functions to calculate Chinese lunar calendar date based on
      *   western calendar.
      * FIXME: Should it be in native code?
      */
     public static String lunarDate(Context context, Time time, boolean showHoliday) {
         String lunar = "";
         if (time.year < LUNAR_START_YEAR || time.year > LUNAR_END_YEAR)
         {
             if (DEBUG)android.util.Log.e("CalUtils", "  illegal year: " + time.year);
             return lunar;
         }

         // Gregorian Calendar date 1901/2/19 is Lunar Calendar date 1901/1/1.
         //   the days difference between Gregorian Calendar Date 1/1 and 2/19
         //   is 49 days.

         Calendar calen = Calendar.getInstance();
         calen.set(time.year, time.month, time.monthDay);
         float spanMills;
         spanMills = calen.getTimeInMillis();
         calen.set(1901, 0, 1);
         spanMills -= calen.getTimeInMillis();
         //fwr687 beign IKSF-6402
         BigDecimal num = new BigDecimal(spanMills / (86400000L)).setScale(0, BigDecimal.ROUND_HALF_UP);
         int days = num.intValue();
         //fwr687 end

         // FIXME: Any DST issue?
         // if( calen.add(days) != time )
         //     android.util.Log.e("CalUtils", "    We got troubles in days diff!!!");
         // TODO: And shall we use the yeardays to calculate the span days?
         // if (year1 == year2) days = yearday2 - yearday1;
         // else {sum();}

         // android.util.Log.i("CalUtils", "  span mills and span days are: " + spanMills + ", " + days);

         LunarDateInfo  lunarDate = days2lunarDate(days);

         if (DEBUG)android.util.Log.i("CalUtils", "  lunar date result: Y" + lunarDate.lYear + ", M" + lunarDate.lMonth + ", D" + lunarDate.lDay);
         if (lunarDate.lessSpanFlag == true)
             lunar =lunarYearName(context, lunarDate.lYear) + lunarMonthName(context, lunarDate.lMonth, false)
                     + lunarMonthdayName(context, lunarDate.lDay);
         else
             lunar = lunarYearName(context, lunarDate.lYear) + lunarMonthName(context, lunarDate.lMonth, lunarDate.leap)
                     + lunarMonthdayName(context, lunarDate.lDay);

         if(showHoliday){
             int lunarFestivalIndex = getLunarFestivalByLunarDate(lunarDate.lMonth,lunarDate.lDay,days);
             if(lunarFestivalIndex!=-1){
                 String [] lunarFestivalArray= context.getResources().getStringArray(R.array.lunar_festival);
                 lunar += " "+lunarFestivalArray[lunarFestivalIndex];
                 return lunar;
             }
         }

         int solarTermIndex=getSolarTermByDate(time.year, time.month, time.monthDay, context);
         if(solarTermIndex!=-1){
             String [] solarTermArray= context.getResources().getStringArray(R.array.solar_term);
             lunar += " "+solarTermArray[solarTermIndex];
             return lunar;
         }
         return lunar;

     }

     /**
      * Provides functions to calculate couple Chinese lunar calendar date based on
      * western calendar.
      */
     public static String lunarCoupleDate(Context context, Time start, Time end,
             boolean showHoliday) {
         String lunar = "";
         start.minute = 0;
         start.second = 0;
         start.hour = 0;
         end.minute = 0;
         end.second = 0;
         end.hour = 0;
         int isEqual = Time.compare(start, end);
         if (isEqual == 0) {
             return lunarDate(context, start, showHoliday);
         } else {
             lunar = lunarDate(context, start, showHoliday) + " - "
                     + lunarDate(context, end, showHoliday);
         }
         return lunar;
     }
     /**
      * Provides functions to calculate Chinese lunar calendar date based on
      *   western calendar.
      * FIXME: Should it be in native code?
      */
     public static String lunarDate(Context context, Time time) {
         String lunar = "";
         if (time.year < LUNAR_START_YEAR || time.year > LUNAR_END_YEAR)
         {
             if (DEBUG) Log.e(TAG, "Illegal year: " + time.year);
             return lunar;
         }

         // Gregorian Calendar date 1901/2/19 is Lunar Calendar date 1901/1/1.
         //   the days difference between Gregorian Calendar Date 1/1 and 2/19
         //   is 49 days.

         Calendar calen = Calendar.getInstance();
         calen.set(time.year, time.month, time.monthDay);
         float spanMills;
         spanMills = calen.getTimeInMillis();
         calen.set(1901, 0, 1);
         spanMills -= calen.getTimeInMillis();
         //fwr687 beign IKSF-6402
         BigDecimal num = new BigDecimal(spanMills / (86400000L)).setScale(0, BigDecimal.ROUND_HALF_UP);
         int days = num.intValue();
         //fwr687 end

         // FIXME: Any DST issue?
         // if( calen.add(days) != time )
         //     Log.e (TAG, "We got troubles in days diff!!!");
         // TODO: And shall we use the yeardays to calculate the span days?
         // if (year1 == year2) days = yearday2 - yearday1;
         // else {sum();}

         // Log.i(TAG, "Span mills and span days are: " + spanMills + ", " + days);

         LunarDateInfo  lunarDate = days2lunarDate(days);

         if (DEBUG) Log.i(TAG, "Lunar date result: Y" + lunarDate.lYear + ", M" + lunarDate.lMonth + ", D" + lunarDate.lDay);

         lunar = lunarYearName(context, lunarDate.lYear) + lunarMonthName(context, lunarDate.lMonth)
                 + lunarMonthdayName(context, lunarDate.lDay);

         //fwr687 end
         return lunar;
     }
     /**
      * Provides functions to calculate Chinese lunar calendar month and day based on
      *   western calendar.
      * FIXME: Should it be in native code?
      */
     public static String lunarMonthAndDay(Context context, Time time) {
         String lunar = "";
         if (time.year < LUNAR_START_YEAR || time.year > LUNAR_END_YEAR)
         {
             if (DEBUG) Log.e(TAG, "Illegal year: " + time.year);
             return lunar;
         }

         Calendar calen = Calendar.getInstance();
         calen.set(time.year, time.month, time.monthDay);
         float spanMills;
         spanMills = calen.getTimeInMillis();
         calen.set(1901, 0, 1);
         spanMills -= calen.getTimeInMillis();
         //fwr687 beign IKSF-6402
         BigDecimal num = new BigDecimal(spanMills / (86400000L)).setScale(0, BigDecimal.ROUND_HALF_UP);
         int days = num.intValue();
         //fwr687 end

         LunarDateInfo  lunarDate = days2lunarDate(days);

         if (DEBUG) Log.i(TAG, "Lunar date result: Y" + lunarDate.lYear + ", M" + lunarDate.lMonth + ", D" + lunarDate.lDay);

         lunar = lunarMonthName(context, lunarDate.lMonth)+ lunarMonthdayName(context, lunarDate.lDay);

         //fwr687 end
         return lunar;
     }

     public static String lunarMonthAndDay(Context context, Time time,
             boolean showHoliday) {
         String lunar = "";
         if (time.year < LUNAR_START_YEAR || time.year > LUNAR_END_YEAR) {
             if (DEBUG)
                 android.util.Log.e("CalUtils", "  illegal year: " + time.year);
             return lunar;
         }

         // Gregorian Calendar date 1901/2/19 is Lunar Calendar date 1901/1/1.
         // the days difference between Gregorian Calendar Date 1/1 and 2/19
         // is 49 days.

         Calendar calen = Calendar.getInstance();
         calen.set(time.year, time.month, time.monthDay);
         float spanMills;
         spanMills = calen.getTimeInMillis();
         calen.set(1901, 0, 1);
         spanMills -= calen.getTimeInMillis();
         //fwr687 beign IKSF-6402
         BigDecimal num = new BigDecimal(spanMills / (86400000L)).setScale(0, BigDecimal.ROUND_HALF_UP);
         int days = num.intValue();
         //fwr687 end

         // FIXME: Any DST issue?
         // if( calen.add(days) != time )
         // android.util.Log.e("CalUtils",
         // "    We got troubles in days diff!!!");
         // TODO: And shall we use the yeardays to calculate the span days?
         // if (year1 == year2) days = yearday2 - yearday1;
         // else {sum();}

         // android.util.Log.i("CalUtils", "  span mills and span days are: " +
         // spanMills + ", " + days);

         LunarDateInfo lunarDate = days2lunarDate(days);
         lunar = lunarMonthAndDay(context, time);

         if(showHoliday){
             int lunarFestivalIndex = getLunarFestivalByLunarDate(lunarDate.lMonth,lunarDate.lDay,days);
             if(lunarFestivalIndex!=-1){
                 String [] lunarFestivalArray= context.getResources().getStringArray(R.array.lunar_festival);
                 lunar += " "+lunarFestivalArray[lunarFestivalIndex];
                 return lunar;
             }
         }

         int solarTermIndex = getSolarTermByDate(time.year, time.month,
                 time.monthDay, context);
         if (solarTermIndex != -1) {
             String[] solarTermArray = context.getResources().getStringArray(
                     R.array.solar_term);
             lunar += " " + solarTermArray[solarTermIndex];
             return lunar;
         }
         return lunar;
     }

     /**
      * Gets the month name in lunar.
      * @param month
      * @return
      */
     public static String lunarMonthName(Context context, int month) {
         if (month<1 || month>12)
         {
             if (DEBUG) Log.e(TAG, "Illegal month: " + month);
             return "";
         }

         int num = month - 1;

         // Leap month has no special expression per CxD design.
         //        return leap ?
         //               (context.getString(R.string.leap) + context.getString(lunarMonths[num])) :
         //               context.getString(lunarMonths[num]);
         String [] lunarMonthArray= context.getResources().getStringArray(R.array.lunar_month);
         return lunarMonthArray[num];
     }

 /*
  *     End Motorola - Lunar Calendar
  */
     
  // BEGIN MOTOROLA - IKPIM-974: Support forward on event detail
     /**
      * Reply event or forward event with Calendar ICS file via email.
      *
      * @param context The context
      * @param icsUri The Uri of the ICS file
      * @param subject The subject of the Calendar event
      * @param messageBody The message body of the composed message
      * @param syncAccountType The sync account type of the calendar event
      * @param accountId The mail account id
      * @param toList The mail destination list
      * @param ccList The mail cc list
      */
     public static void mailEventIcs(Context context, Uri icsUri, String subject,
             String messageBody, String syncAccountType, long accountId,
             String[] toList, String[] ccList) {
         Intent intent = new Intent(Intent.ACTION_SEND);

         if (icsUri != null) {
             // Attach the ICS file in the message
             intent.putExtra(Intent.EXTRA_STREAM, icsUri);
             intent.putExtra("EXTRA_ATTACHMENT_SHARE_NAME", "invite.vcs");
             intent.setType(MIME_TYPE_ICAL);
         } else {
             intent.setType(MIME_TYPE_MSG_RFC822);
         }

         intent.addCategory(Intent.CATEGORY_DEFAULT);
         boolean bMatched = false; // Indicates whether we've found the matched Activity
         String packageName = null; // package name of the found Activity
         String className = null; // class name of the found Activity

         PackageManager pm = context.getPackageManager();
         List<ResolveInfo> infos = pm.queryIntentActivities(intent,
                 PackageManager.MATCH_DEFAULT_ONLY);
         for (ResolveInfo info : infos) {
             packageName = info.activityInfo.packageName;
             className = info.activityInfo.name;
             if (DEBUG)
                 Log.d(TAG, "package name is " + packageName + ", class name is " + className);

             if (GOOGLE_ACCOUNT_TYPE.equals(syncAccountType)) {
                 if (packageName.contains(GMAIL_PKG)) {
                     bMatched = true;
                     break;
                 }
             } else if (EXCHANGE_ACCOUNT_TYPE.equals(syncAccountType)) {
                 if (packageName.contains(ANDROID_EMAIL_PKG)
                         || packageName.contains(GOOGLE_EMAIL_PKG)
                         || packageName.contains(MOTO_EMAIL_PKG)) {
                     bMatched = true;
                     break;
                 }
             } else {
                 // For third -party apps, we don't know which account type and package name would
                 // be used. So we'll use the rough algorithm, just launch the application chooser
                 // to let user do the decision.
                 bMatched = false;
                 break;
             }
         }

         // Launch target Activity directly if bMatched is true, otherwise launch application chooser
         if (bMatched) {
             if (DEBUG)
                 Log.d(TAG, "set intent package name " + packageName + ", class name " + className);
             intent.setClassName(packageName, className);
         }

         // BEGIN MOTOROLA - IKHSS7-816: Do not specify account id, always use default email account
         //intent.putExtra(COMPOSE_ACCT_ID, accountId);
         // END MOTOROLA - IKHSS7-816

         intent.putExtra(Intent.EXTRA_SUBJECT, subject);
         if (DEBUG)
             Log.d(TAG, "showReplyComposeScreen, Intent.EXTRA_SUBJECT=" + subject);

         if (toList != null) {
             intent.putExtra(Intent.EXTRA_EMAIL, toList);
             if (DEBUG)
                 Log.d(TAG, "showReplyComposeScreen, Intent.EXTRA_EMAIL=" + toList[0]);
         }

         if (ccList != null) {
             intent.putExtra(Intent.EXTRA_CC, ccList);
         }

         intent.putExtra(Intent.EXTRA_TEXT, messageBody);
         if (DEBUG)
             Log.d(TAG, "showReplyComposeScreen, Intent.EXTRA_TEXT=" + messageBody);

         if (icsUri != null) {
             // Find the potential handlers and give them permission to read the Uri.
             for (ResolveInfo info : infos) {
                 PackageItemInfo activityInfo = info.activityInfo;
                 if (activityInfo != null) {
                     context.grantUriPermission(activityInfo.packageName, icsUri,
                             Intent.FLAG_GRANT_READ_URI_PERMISSION);
                 }
             }
         }

         context.startActivity(intent);
     }
     // END MOTOROLA - IKPIM-974

     public static void addEventToTasks(Fragment fragment, Uri eventUri, String strictEventUid,
             long startMillis, long endMillis, String title, String where, String organizer,
             boolean allDay, int reqId) {
         Context context = fragment.getActivity();

         if (TextUtils.isEmpty(title)) {
             title = context.getString(R.string.no_title_label);
         }

         int flags = DateUtils.FORMAT_SHOW_DATE;
         if (allDay) {
             flags |= DateUtils.FORMAT_UTC | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY;
         } else {
             flags |= DateUtils.FORMAT_SHOW_TIME;
             if (DateFormat.is24HourFormat(context)) {
                 flags |= DateUtils.FORMAT_24HOUR;
             }
         }

         String when = Utils.formatDateRange(context, startMillis, endMillis, flags);
         String strUri = eventUri.buildUpon().appendQueryParameter(TASKIFY_FLAG, "true")
                 .appendQueryParameter(TASKIFY_STRICT_EVENT_UID, strictEventUid)
                 .appendQueryParameter(EXTRA_EVENT_BEGIN_TIME, String.valueOf(startMillis))
                 .appendQueryParameter(EXTRA_EVENT_END_TIME, String.valueOf(endMillis))
                 .build().toString();
         String description = context.getString(R.string.view_event_organizer_label) + " " + organizer + "\n" + context.getString(R.string.when_label) + ": " + when;
         if (!TextUtils.isEmpty(where)) {
             description += "\n" + context.getString(R.string.where_label) + ": " + where;
         }

         // Send intent to Tasks app with the below extra data:
         // title: the event title
         // uri: the string of the event Uri with query parameters (startMills and endMills).
         //      Note: will parse the uri when lauching this activity from Tasks app.
         // description: the description format as per the requirement is
         //              Organizer: <the event organizer>
         //              When: <the when string of the event instance>
         //              Where: <the location of the event>
         Intent intent = new Intent(ADD_EVENT_TASKS_ACTION);
         intent.putExtra("title", title);
         intent.putExtra("uri", strUri);
         intent.putExtra("description", description);

         try {
             fragment.startActivityForResult(intent, reqId);
         } catch (ActivityNotFoundException e) {
             Log.e(TAG, "Cannot find activity ", e);
             Toast.makeText(context, R.string.failed_to_add_event_to_tasks, Toast.LENGTH_LONG).show();
         }
     }

     public static void handleAddEventToTasksResult(Fragment fragment, int resultCode, Intent data, String title) {
         Context context = fragment.getActivity();

         if (TextUtils.isEmpty(title)) {
             title = context.getString(R.string.no_title_label);
         }

         boolean result = false;
         if ((resultCode == Activity.RESULT_OK) && (data != null)) {
             result = data.getBooleanExtra("create_taskify_result", false);
             Log.d(TAG, "The result from Tasks app is: " + result);
         }

         String text = null;
         if (result) {
             final int TITLE_MAX_LEN = 20;
             if (title.length() > TITLE_MAX_LEN) {
                 title = title.substring(0, TITLE_MAX_LEN) + "...";
             }
             text = context.getString(R.string.adding_event_to_tasks, title);
         } else {
             text = context.getString(R.string.failed_to_add_event_to_tasks);
         }

         Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
     }

     /**
      * Try to find and get the exception event of a recurrence event as per the instance start time.
      * @param context the Context.
      * @param calendarId the Calendar Id.
      * @param originalEvent the sync id of the recurrence event.
      * @param instanceStartTime the instance start time.
      * @return the exception event Id if found or null if not found.
      */
     public static Long getRecurrenceException(Context context, long calendarId, String originalEvent, long instanceStartTime) {
         Long exceptionEventId = null;
         String selection = Events.CALENDAR_ID + "=? AND " + Events.ORIGINAL_SYNC_ID + "=? AND "
             + Events.ORIGINAL_INSTANCE_TIME + "=?";
         String[] selectionArgs = new String[] {String.valueOf(calendarId), originalEvent, String.valueOf(instanceStartTime)};
         ContentResolver contentResolver = context.getContentResolver();

         Log.v(TAG, "Query exception event, calendarId=" + calendarId + ", originalEvent=" + originalEvent +
             ", instanceStartTime=" + instanceStartTime);
         Cursor c = contentResolver.query(Events.CONTENT_URI, new String[] {Events._ID}, selection, selectionArgs, null);
         if (c != null) {
             try {
                 if (c.moveToFirst()) {
                     exceptionEventId = c.getLong(0);
                     Log.v(TAG, "Found exception event, exceptionEventId=" + exceptionEventId);
                 }
             } finally {
                 c.close();
             }
         }
         return exceptionEventId;
     }

     /**
      * Check if the event has been deleted.
      *
      * @param context The context
      * @param eventUri The Uri of the event
      * @return if the event has been deleted or not
      */
     public static boolean isEventDeleted(Context context, Uri eventUri) {
         boolean ret = true;
         ContentResolver cr = context.getContentResolver();
         Cursor c = cr.query(eventUri, new String[] { Events.STATUS, Events.DELETED }, null, null, null);
         if (c == null) {
             ret = true;
         } else {
             try {
                 if (c.moveToFirst()) {
                     boolean deleted = c.getInt(1) != 0;
                     //<!-- MOTOROLA MOD Start of IKTABLETMAIN-2483 -->
                     if (deleted) {
                     //<!-- MOTOROLA MOD End of IKTABLETMAIN-2483 -->
                         ret = true;
                     } else {
                         ret = false;
                     }
                 } else {
                     ret = true;
                 }
             } finally {
                 c.close();
             }
         }

         return ret;
     }
     // End Motorola
     /*2012-11-8, add by amt_sunli for SWITCHUITWOV-296 begin*/
     public static boolean isMOLCTModel() {
         boolean result = false;
         String name = Build.MODEL;
         if (!TextUtils.isEmpty(name)) {
             Log.d(TAG, "MODEL = " + name);
             result = name.trim().contains("XT788");//CT model name
         }
         Log.d(TAG, "isMOLCTModel:" + result);
         return result;
     }
     /*2012-11-8, add by amt_sunli for SWITCHUITWOV-296 end*/

     // added by amt_sunli 2012-12-06 SWITCHUITWO-64 begin
     private static int screenWidth = -1;
     private static int screenHeight = -1;

     public static int getScreenWidth() {
         return screenWidth;
     }

     public static void setScreenWidth(int screenWidth) {
         Utils.screenWidth = screenWidth;
     }

     public static int getScreenHeight() {
         return screenHeight;
     }

     public static void setScreenHeight(int screenHeight) {
         Utils.screenHeight = screenHeight;
     }
    // added by amt_sunli 2012-12-06 SWITCHUITWO-64 end
     // add by amt_sunli 2013-1-9 for feature 38173 begin
     /**
      * true is need to remove activesync, false is keep guest funcion in edit event page
      */
     public static boolean isNeedRemoveActiveSync = false;
     // add by amt_sunli 2013-1-9 for feature 38173 end
     // Begin Motorola
     /**
      * Get the extended capabilities related to Calendar, such as if support Add to tasks, print ICS, etc.
      *
      * @param context The context
      * @return The bitwise OR of EXT_CAPABILITY_XXX constant values which are also defined in this class.
      */
     public static int getCalendarExtCapabilities(Context context) {
         int capabilities = 0;
         PackageManager pm = context.getPackageManager();
         List<ResolveInfo> infos;

         // Check the Add to tasks capability
         Intent intentAddToTasks = new Intent();
         intentAddToTasks.setAction(ADD_EVENT_TASKS_ACTION);
         infos = pm.queryIntentActivities(intentAddToTasks, 0);
         if (!infos.isEmpty()) {
             capabilities |= EXT_CAPABILITY_ADD_TO_TASKS;
         }

         // Check the Print ICS capability
         Intent intentPrintIcs = new Intent();
         intentPrintIcs.setAction(PRINT_ICS_ACTION);
         intentPrintIcs.setType(MIME_TYPE_ICAL);
         infos = pm.queryIntentActivities(intentPrintIcs, 0);
         if (!infos.isEmpty()) {
             capabilities |= EXT_CAPABILITY_PRINT_ICS;
         }

         if (DEBUG) {
             Log.v(TAG, "Calendar extended capabilities: " + capabilities);
         }

         return capabilities;
     }

     /**
      * Print the Calendar ICS file.
      *
      * @param context The context
      * @param icsUri The Uri of the ICS file
      */
     public static void printIcs(Context context, Uri icsUri) {
         Intent intent = new Intent(PRINT_ICS_ACTION);
         intent.setType(MIME_TYPE_ICAL);
         intent.putExtra(Intent.EXTRA_STREAM, icsUri);
         try {
             context.startActivity(intent);
         } catch (ActivityNotFoundException e) {
             Log.e(TAG, "Cannot find activity ", e);
         }
     }

     /**
      * Share the Calendar ICS file.
      *
      * @param context The context
      * @param icsUri The Uri of the ICS file
      * @param subject The subject of the Calendar event
      * @param messageBody The message body of the composed message
      */
     public static void shareIcs(Context context, Uri icsUri, String subject, String messageBody) {
         Intent intent = new Intent(Intent.ACTION_SEND);
         intent.addCategory(Intent.CATEGORY_DEFAULT);
         Log.v(TAG, "ICSURI="+ icsUri);
         if (icsUri != null) {
             // Attach the ICS file in the message
             intent.putExtra(Intent.EXTRA_STREAM, icsUri);
             intent.putExtra("EXTRA_ATTACHMENT_SHARE_NAME", "invite.vcs");
             intent.setType(MIME_TYPE_ICAL);
         } else {
             intent.setType(MIME_TYPE_MSG_RFC822);
         }

         intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.forward_short) + " " + subject);
         intent.putExtra(Intent.EXTRA_TEXT, messageBody);

         if (icsUri != null) {
             // Find the potential handlers and give them permission to read the Uri.
             PackageManager pm = context.getPackageManager();
             List<ResolveInfo> infos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
             for (ResolveInfo info : infos) {
                 PackageItemInfo activityInfo = info.activityInfo;
                 if (activityInfo != null) {
                     context.grantUriPermission(activityInfo.packageName, icsUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                 }
             }
         }

         try {
             context.startActivity(Intent.createChooser(intent, context.getText(R.string.share_label)));
         } catch (ActivityNotFoundException e) {
             Log.e(TAG, "Cannot find activity ", e);
         }
     }
}
