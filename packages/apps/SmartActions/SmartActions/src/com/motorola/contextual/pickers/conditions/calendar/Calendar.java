/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number          Brief Description
 * ------------- ---------- -----------------  ------------------------------
 * BHT364        2012/07/03 Smart Actions 2.1  Initial Version
 */
package com.motorola.contextual.pickers.conditions.calendar;

import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract.Calendars;
import android.util.Log;

import com.motorola.contextual.smartrules.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Small data class to manage information about available calendars
 * from the calendar content provider
 *
 * <code><pre>
 *
 * CLASS:
 *  implements CalendarEventSensorConstants
 *
 * RESPONSIBILITIES:
 * Manages information about available calendars from the calendar
 * content provider.
 *
 * COLLABORATORS:
 *  Calendar - Manages information about available calendars
 *  CalendarActivity - the Activity hosting the calendar picker fragments
 *  CalendarEventConfigFragment - the fragment to configure the types of calendar events to use
 *  CalendarPickerFragment - the fragment to pick the specific calendar
 *  MatchingEventsDialog - the dialog that presents the matching events for the users to choose.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class Calendar implements CalendarEventSensorConstants {

    private static final String TAG = "Calendar";

    // The internal phone calendar is always the first entry
    private static final int PHONE_CALENDAR_ID = 1;

    /**
     * Returns a list of Calendar objects describing the
     * available calendars from the calendar content provider
     *
     * @param context - application context
     * @return a non-null list of available calendar
     */
    static List<Calendar>getCalendars(Context context) {
        List<Calendar> calendars = new ArrayList<Calendar>();
        Cursor cursor = null;
        String selection = Calendars.VISIBLE + "='" + VISIBLE_CALENDARS_ONLY + "'";
        try {
            cursor = context.getContentResolver()
                    .query(Calendars.CONTENT_URI,
                            new String[] {
                                Calendars._ID,
                                Calendars.CALENDAR_COLOR,
                                Calendars.CALENDAR_DISPLAY_NAME,
                                Calendars.ACCOUNT_NAME
                            }, selection, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int idColumn = cursor.getColumnIndexOrThrow(Calendars._ID);
                int colorColumn = cursor.getColumnIndexOrThrow(Calendars.CALENDAR_COLOR);
                int displayNameColumn = cursor.getColumnIndexOrThrow(Calendars.CALENDAR_DISPLAY_NAME);
                int accountColumn = cursor.getColumnIndexOrThrow(Calendars.ACCOUNT_NAME);
                do {
                    Calendar calendar= new Calendar(
                            cursor.getInt(idColumn),
                            cursor.getInt(colorColumn),
                            cursor.getString(displayNameColumn),
                            cursor.getString(accountColumn));
                    if (calendar.id == PHONE_CALENDAR_ID) {
                        calendar.displayName = context.getResources().getString(R.string.calendar_phone_display_name);
                    }
                    calendars.add(calendar);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to fetch calendar information: " + Calendars.CONTENT_URI + " with selection " + selection, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return calendars;
    }

    /**
     * Parse a calendar Id string and construct a list of the ids.
     *
     * @param calendarIdsString a string containing the calendar ids seperated by " OR "'s
     * @return a list of calendar ids or null if there are none
     */
    static List<Integer> getCalendarIds(String calendarIdsString) {
        List<Integer> ids = null;
        if (calendarIdsString != null) {
            ids = new ArrayList<Integer>();
            String[] calendarIds = calendarIdsString.split(OR);
            for (String id : calendarIds) {
                ids.add(Integer.parseInt(id));
            }
        }
        return ids;
    }

    /**
     * Convert the list of calendar ids into a string
     *
     * @param selectedIds
     * @return a string with the ids seperated by " OR "'s or null if the list is empty or null
     */
    static String getCalendarIdsString(List<Integer> selectedIds) {
        if (selectedIds == null || selectedIds.isEmpty()) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        for (Integer id : selectedIds) {
            if (result.length() > 0)
                result.append(OR);
            result.append(id);
        }
        return result.toString();
    }

    /**
     * Return the calendar from the given list that matches the give id, or null
     * if it isn't in the list
     */
    static Calendar findById(List<Calendar> calendars, int id) {
        for (Calendar calendar : calendars) {
            if (calendar.id == id)
                return calendar;
        }
        return null;
    }

    // Package private to allow access to these variables within this package.
    int id;              // Calendar id
    int color;           // Display color
    String displayName;  // Display name
    String account;      // Account this calendar belongs to

    /**
     * Constructor.
     *
     * @param id - id of the calendar
     * @param color - display color to use
     * @param displayName - the display name of the calendar
     * @param account - the account this calendar belongs to
     */
    public Calendar(int id, int color, String displayName, String account) {
        this.id = id;
        this.color = color;
        this.displayName = displayName;
        this.account = account;
    }
}
