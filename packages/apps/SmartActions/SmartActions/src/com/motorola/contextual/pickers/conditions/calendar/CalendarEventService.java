/*
 * @(#)CalendarEventService.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * wkh346        2011/08/22 NA                Initial version
 *
 */

package com.motorola.contextual.pickers.conditions.calendar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.motorola.contextual.pickers.conditions.calendar.CalendarEventUtils.ConfigData;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;

/**
 * This class is responsible for broadcasting the Intents for the rules that
 * have become active and that have become inactive. This class checks whether a
 * calendar event, that has started, belongs to particular rule. If yes, then it
 * activates the rule by broadcasting the Intent. This class also checks whether
 * a calendar event, that has ended, belongs to particular rule that is active
 * currently. If yes, then it deactivates the rule by broadcasting the Intent.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Extends {@link IntentService}
 *      Implements {@link CalendarEventSensorConstants}
 *
 * RESPONSIBILITIES:
 * This class is responsible for broadcasting the Intents for the rules that
 * have become active and that have become inactive. This class checks whether a
 * calendar event, that has started, belongs to particular rule. If yes, then it
 * activates the rule by broadcasting the Intent. This class also checks whether
 * a calendar event, that has ended, belongs to particular rule that is active
 * currently. If yes, then it deactivates the rule by broadcasting the Intent.
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public final class CalendarEventService extends IntentService implements
    CalendarEventSensorConstants {

    /**
     * TAG for logging messages
     */
    private static final String TAG = "CalendarEventService";

    /**
     * constant for defining invalid event id
     */
    private static final String INVALID_EVENT_ID = "-1";

    /**
     * One day duration in milliseconds
     */
    private static final long ONE_DAY_WINDOW = 24 * 60 * 60 * 1000;

    /**
     * Checkpoint delay in milliseconds
     */
    private static final int CHECK_POINT_DELAY = 2000;

    /**
     * Projection for querying calendar table
     */
    private static final String[] CALENDAR_PROJECTION = new String[] { Calendars._ID };

    /**
     * Projection for querying instances table
     */
    private static final String[] INSTANCES_PROJECTION = new String[] {
            Instances._ID, Instances.BEGIN, Instances.END, Instances.EVENT_ID,
            Events.TITLE, Events.ALL_DAY, Events.CALENDAR_ID };

    /**
     * Order for querying instances table
     */
    private static final String INSTANCES_SORT_ORDER = "begin ASC, end ASC";

    /**
     * This is to figure the type of mismatch due to which the event doesn't
     * belong to the config
     *
     * @author wkh346
     *
     */
    private enum MismatchType {
        /**
         * Event belongs to the rule
         */
        NO_MISMATCH,
        /**
         * Event doesn't belong to the rule because calendar ids don't match
         */
        CALENDAR_IDS_MISMATCH,
        /**
         * Event doen't belong to the rule because rule is created for excluding
         * all day events and current event is an all day event
         */
        ALL_DAY_EVENT_MISMATCH,
        /**
         * Event doesn't belong to the rule because rule is created for
         * including events with multiple people only and current event doesn't
         * have attendees
         */
        MULTIPLE_ATTENDEES_MISMATCH,
        /**
         * Event doesn't belong to the rule because rule is created for accepted
         * events only and current event is not an accepted event
         */
        ACCEPTED_EVENT_MISMATCH
    }

    /**
     * Constructor
     */
    public CalendarEventService() {
        super(TAG);
    }

    /**
     * Constructor
     *
     * @param name
     *            - Used to name the worker thread, important only for
     *            debugging.
     */
    public CalendarEventService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(CALENDAR_EVENTS_EXPORT_ACTION)) {
                exportRules();
            } else if (action.equals(CALENDAR_EVENTS_IMPORT_ACTION)) {
                importRules(intent);
            } else if (action.equals(ACTION_STORE_CONFIG)) {
                storeConfig(intent.getStringExtra(EXTRA_CONFIG));
            } else if (action.equals(ACTION_REMOVE_CONFIG)) {
                removeConfig(intent.getStringExtra(EXTRA_CONFIG));
            } else if (action.equals(ACTION_REFRESH_CONFIG)) {
                refreshConfig(intent.getStringExtra(EXTRA_CONFIG),
                        intent.getStringExtra(EXTRA_REQUEST_ID));
            } else if (action.equals(SA_CORE_INIT_COMPLETE)) {
                handleCoreInitComplete();
            } else if (action.equals(ACTION_SCHEDULE_EVENT_AWARE)
                    || action.equals(ACTION_EVENTS_TABLE_CHANGED)) {
                refreshEventAware();
            }
        }
    }

    /**
     * This method handles init complete event by deactivating all configs and
     * refreshing event aware functionality
     */
    private void handleCoreInitComplete() {
        deactivateAllRules(false);
        refreshEventAware();
    }

    /**
     * This method inserts the configuration to calendar events database if it
     * doesn't exist already
     *
     * @param configString
     *            - the String containing the configuration
     */
    private void storeConfig(String configString) {
        ConfigData configData = CalendarEventUtils.getConfigData(configString);
        if (configData != null) {
            CalendarEventDatabase database = CalendarEventDatabase
                    .getInstance(this);
            int ruleId = database.insertAndUpdateRow(this,
                    configData.mCalendarIds, configData.mExcludeAllDayEvents,
                    configData.mOnlyIncludeAcceptedEvents,
                    configData.mOnlyIncludeEventsWithMultiplePeople, true);
            if (LOG_INFO) {
                Log.i(TAG, "storeConfig rule id " + ruleId
                        + " assigned to config = " + configString);
            }
            refreshEventAware();
        }
    }

    /**
     * This method removes the configuration from calendar events database if it
     * already exists
     *
     * @param configString
     *            - the String containing the configuration
     */
    private void removeConfig(String configString) {
        if (ALL_CONFIGS.equals(configString)) {
            CalendarEventDatabase database = CalendarEventDatabase
                    .getInstance(this);
            database.deleteAllRows();
            removeExistingAlarm();
        } else {
            ConfigData configData = CalendarEventUtils
                    .getConfigData(configString);
            if (configData != null) {
                CalendarEventDatabase database = CalendarEventDatabase
                        .getInstance(this);
                database.deleteRow(configData.mCalendarIds,
                        configData.mExcludeAllDayEvents,
                        configData.mOnlyIncludeAcceptedEvents,
                        configData.mOnlyIncludeEventsWithMultiplePeople);
                if (database.isEmpty()) {
                    removeExistingAlarm();
                } else {
                    refreshEventAware();
                }
                if (LOG_INFO) {
                    Log.i(TAG, "removeConfig removed config = " + configString);
                }
            }
        }
    }

    /**
     * This method performs refresh for given configuration and response ID
     *
     * @param configString
     *            - the configuration String
     * @param responseId
     *            - the response ID
     */
    private void refreshConfig(String configString, String responseId) {
        if (responseId != null && configString != null && !responseId.isEmpty()
                && !configString.isEmpty()) {
            ConfigData configData = CalendarEventUtils
                    .getConfigData(configString);
            if (configData != null) {
                CalendarEventDatabase database = CalendarEventDatabase
                        .getInstance(this);
                String state = database.getRuleState(configData.mCalendarIds,
                        configData.mExcludeAllDayEvents,
                        configData.mOnlyIncludeAcceptedEvents,
                        configData.mOnlyIncludeEventsWithMultiplePeople);
                String description = CalendarEventUtils.getDescription(this,
                        configData.mCalendarIds);
                CalendarEventUtils.sendRefreshResponseIntent(this,
                        configString, description, responseId, state);
            }
        }
    }

    /**
     * This method exports the calendar events database entries
     */
    private void exportRules() {
        CalendarEventDatabase database = CalendarEventDatabase
                .getInstance(this);
        Cursor cursor = null;
        JSONArray jsonArray = new JSONArray();
        try {
            cursor = database.query(new String[] {
                    CalendarEventDatabase.COLUMN_CALENDAR_IDS,
                    CalendarEventDatabase.COLUMN_ALL_DAY_EVENTS,
                    CalendarEventDatabase.COLUMN_MULTIPLE_PARTICIPANTS,
                    CalendarEventDatabase.COLUMN_SHOW_ACCEPTED_EVENTS }, null,
                    null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    JSONObject jsonObject = new JSONObject();
                    String calendarsIds = cursor
                            .getString(cursor
                                    .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_CALENDAR_IDS));
                    if (calendarsIds == null || calendarsIds.isEmpty()) {
                        calendarsIds = NULL_STRING;
                    }
                    jsonObject.put(CalendarEventDatabase.COLUMN_CALENDAR_IDS,
                            calendarsIds);
                    jsonObject
                            .put(CalendarEventDatabase.COLUMN_ALL_DAY_EVENTS,
                                    cursor.getInt(cursor
                                            .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_ALL_DAY_EVENTS)));
                    jsonObject
                            .put(CalendarEventDatabase.COLUMN_MULTIPLE_PARTICIPANTS,
                                    cursor.getInt(cursor
                                            .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_MULTIPLE_PARTICIPANTS)));
                    jsonObject
                            .put(CalendarEventDatabase.COLUMN_SHOW_ACCEPTED_EVENTS,
                                    cursor.getInt(cursor
                                            .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_SHOW_ACCEPTED_EVENTS)));
                    jsonArray.put(jsonObject);
                } while (cursor.moveToNext());
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            Log.e(TAG,
                    "exportRules failed to read data from calendar events database");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (jsonArray.length() > 0) {
            SharedPreferences sharedPreference = getSharedPreferences(
                    CALENDAR_EVENTS_SHARED_PREFERENCE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreference.edit();
            editor.clear();
            editor.putString(CALENDAR_EVENTS_XML_CONTENT, jsonArray.toString());
            editor.commit();
            Intent intent = new Intent(NOTIFY_DATA_CHANGE);
            sendBroadcast(intent);
            if (LOG_INFO) {
                Log.i(TAG, "exportRules exported calendar events rules "
                        + jsonArray.toString());
            }
        }
    }

    /**
     * This method imports the entries to calendar events database
     *
     * @param intent
     *            - the incoming intent
     */
    private void importRules(Intent intent) {
        String jsonString = intent.getStringExtra(EXTRA_CALENDAR_EVENTS_DATA);
        if (jsonString != null && !jsonString.isEmpty()) {
            try {
                CalendarEventDatabase database = CalendarEventDatabase
                        .getInstance(this);
                JSONArray jsonArray = new JSONArray(jsonString);
                int length = jsonArray.length();
                for (int index = 0; index < length; index++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(index);
                    String calendarIds = jsonObject
                            .getString(CalendarEventDatabase.COLUMN_CALENDAR_IDS);
                    database.insertAndUpdateRow(
                            this,
                            calendarIds,
                            jsonObject
                                    .getInt(CalendarEventDatabase.COLUMN_ALL_DAY_EVENTS),
                            jsonObject
                                    .getInt(CalendarEventDatabase.COLUMN_SHOW_ACCEPTED_EVENTS),
                            jsonObject
                                    .getInt(CalendarEventDatabase.COLUMN_MULTIPLE_PARTICIPANTS),
                            false);
                }
                if (length > 0) {
                    refreshEventAware();
                }
                if (LOG_INFO) {
                    Log.i(TAG, "importRules imported calendar events rules "
                            + jsonArray.toString());
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    /**
     * Mark all the rules in calendar event database as inactive. Sticky
     * broadcast is not send if broadcastNotRequired is true
     *
     * @param notifySACore
     *            - this should be true if SA core has to be notified
     */
    private void deactivateAllRules(boolean notifySACore) {
        if (LOG_INFO) {
            Log.i(TAG, "deactivateAllRules notifySACore = " + notifySACore);
        }
        CalendarEventDatabase database = CalendarEventDatabase
                .getInstance(this);
        if (database == null) {
            Log.e(TAG,
                    "deactivateAllRules couldn't get instance of calendar events database");
            return;
        }
        Cursor cursor = null;
        try {
            cursor = database.query(new String[] {
                    CalendarEventDatabase.COLUMN_RULE_ID,
                    CalendarEventDatabase.COLUMN_CALENDAR_IDS,
                    CalendarEventDatabase.COLUMN_ALL_DAY_EVENTS,
                    CalendarEventDatabase.COLUMN_MULTIPLE_PARTICIPANTS,
                    CalendarEventDatabase.COLUMN_SHOW_ACCEPTED_EVENTS }, null,
                    null, null);
            if (cursor != null && cursor.moveToFirst()) {
                HashMap<String, String> configsStatesMap = null;
                do {
                    int ruleId = cursor
                            .getInt(cursor
                                    .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_RULE_ID));
                    database.updateRow(ruleId,
                            CalendarEventDatabase.RULE_INACTIVE, null);
                    if (notifySACore) {
                        if (configsStatesMap == null) {
                            configsStatesMap = new HashMap<String, String>();
                        }
                        String calendarIds = cursor
                                .getString(cursor
                                        .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_CALENDAR_IDS));
                        boolean excludeAllDayEvents = cursor
                                .getInt(cursor
                                        .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_ALL_DAY_EVENTS)) == CalendarEventDatabase.SELECTED;
                        boolean onlyIncludeEventsWithMultiplePeople = cursor
                                .getInt(cursor
                                        .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_MULTIPLE_PARTICIPANTS)) == CalendarEventDatabase.SELECTED;
                        boolean onlyIncludeAcceptedEvents = cursor
                                .getInt(cursor
                                        .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_SHOW_ACCEPTED_EVENTS)) == CalendarEventDatabase.SELECTED;
                        configsStatesMap.put(CalendarEventUtils
                                .getConfigString(calendarIds,
                                        excludeAllDayEvents,
                                        onlyIncludeEventsWithMultiplePeople,
                                        onlyIncludeAcceptedEvents), FALSE);
                    }
                } while (cursor.moveToNext());
                if (notifySACore) {
                    CalendarEventUtils.sendNotifyIntent(this, configsStatesMap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,
                    "deactivateAllRules failed to query calendar events database ");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (LOG_DEBUG) {
            database.printDb();
        }
    }

    /**
     * This method extracts the event data and invoke
     * {@link #matchEventDataWithRules(String, int, int, boolean, long, long[])}
     *
     * @param eventIds
     *            - long array holding the event IDs of calendar events that are
     *            currently active
     */
    private void triggerRules(long[] eventIds) {
        if (LOG_INFO) {
            for (long id : eventIds) {
                Log.i(TAG, "triggerRules id = " + id);
            }
        }
        for (long id : eventIds) {
            String selection = Events._ID + EQUALS_TO + INVERTED_COMMA + id
                               + INVERTED_COMMA;
            ContentResolver contentResolver = getContentResolver();
            Cursor eventCursor = null;
            try {
                eventCursor = contentResolver
                              .query(Events.CONTENT_URI, new String[] { Events._ID,
                                      Events.ALL_DAY, Events.SELF_ATTENDEE_STATUS,
                                      Events.CALENDAR_ID
                                                                      }, selection, null, null);
                if (eventCursor != null && eventCursor.moveToFirst()) {
                    String calendarId = String
                                        .valueOf(eventCursor.getInt(eventCursor
                                                 .getColumnIndexOrThrow(Events.CALENDAR_ID)));
                    int allDay = eventCursor.getInt(eventCursor
                                                    .getColumnIndexOrThrow(Events.ALL_DAY));
                    int status = eventCursor
                                 .getInt(eventCursor
                                         .getColumnIndexOrThrow(Events.SELF_ATTENDEE_STATUS));
                    boolean multipleParticipants = false;
                    String attendeeTableSelection = Attendees.EVENT_ID
                                                    + EQUALS_TO + INVERTED_COMMA + id + INVERTED_COMMA;
                    Cursor attendeesCursor = contentResolver.query(
                                                 Attendees.CONTENT_URI, new String[] {
                                                     Attendees.EVENT_ID,
                                                     Attendees.ATTENDEE_RELATIONSHIP
                                                 },
                                                 attendeeTableSelection, null, null);
                    if (attendeesCursor != null
                            && attendeesCursor.getCount() > 1) {
                        multipleParticipants = true;
                    }
                    if (attendeesCursor != null) {
                        attendeesCursor.close();
                    }
                    if (LOG_DEBUG) {
                        Log.d(TAG, "triggerRules calendarId = " + calendarId);
                        Log.d(TAG, "triggerRules allDay = " + allDay);
                        Log.d(TAG, "triggerRules status = " + status);
                        Log.d(TAG, "triggerRules multipleParticipants = "
                              + multipleParticipants);
                    }
                    matchEventDataWithRules(calendarId, allDay, status,
                                            multipleParticipants, id, eventIds);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "triggerRules failed to query " + Events.CONTENT_URI
                      + " with selection " + selection);
            } finally {
                if (eventCursor != null) {
                    eventCursor.close();
                }
            }
        }
        broadcastIntents();
    }

    /**
     * Matches the event data with existing rules. Update the event IDs for
     * which the rule is active or inactive
     *
     * @param calendarId
     *            - value of Events.CALENDAR_ID column
     * @param allDay
     *            - value of Events.ALL_DAY column
     * @param status
     *            - value of Events.SELF_ATTENDEE_STATUS column
     * @param multipleParticipants
     *            - true if the event involves multiple participants, false
     *            otherwise
     * @param id
     *            - the ID of current event
     * @param eventIds
     *            - long array of IDs of currently active events
     */
    private void matchEventDataWithRules(String calendarId, int allDay,
                                         int status, boolean multipleParticipants, long id, long[] eventIds) {
        CalendarEventDatabase database = CalendarEventDatabase
                                         .getInstance(this);
        if (database == null) {
            Log.e(TAG,
                  "matchEventDataWithRules couldn't get instance of calendar events database");
            return;
        }
        Cursor dbCursor = null;
        try {
            dbCursor = database.query(null, null, null, null);
            if (dbCursor != null && dbCursor.moveToFirst()) {
                do {
                    int ruleId = dbCursor
                                 .getInt(dbCursor
                                         .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_RULE_ID));
                    String calendarIds = dbCursor
                                         .getString(dbCursor
                                                    .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_CALENDAR_IDS));
                    String activeEventIds = dbCursor
                                            .getString(dbCursor
                                                       .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_EVENT_IDS));
                    int allDaySelection = dbCursor
                                          .getInt(dbCursor
                                                  .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_ALL_DAY_EVENTS));
                    int acceptedEventsSelection = dbCursor
                                                  .getInt(dbCursor
                                                          .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_SHOW_ACCEPTED_EVENTS));
                    int multipleParticipantsSelection = dbCursor
                                                        .getInt(dbCursor
                                                                .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_MULTIPLE_PARTICIPANTS));
                    int ruleStatus = dbCursor
                            .getInt(dbCursor
                                    .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_ACTIVE));
                    if (LOG_DEBUG) {
                        Log.d(TAG, "matchEventDataWithRules ruleId = " + ruleId);
                        Log.d(TAG, "matchEventDataWithRules calendarIds = "
                              + calendarIds);
                        Log.d(TAG, "matchEventDataWithRules activeEventIds = "
                              + activeEventIds);
                        Log.d(TAG, "matchEventDataWithRules allDaySelection = "
                              + allDaySelection);
                        Log.d(TAG,
                              "matchEventDataWithRules acceptedEventsSelection = "
                              + acceptedEventsSelection);
                        Log.d(TAG,
                              "matchEventDataWithRules multipleParticipantsSelection = "
                              + multipleParticipantsSelection);
                        Log.d(TAG, "matchEventDataWithRules ruleStatus = "
                              + ruleStatus);
                    }
                    activeEventIds = updateActiveEventIds(activeEventIds,
                                                          eventIds);
                    // update the row with new event IDs
                    database.updateRow(ruleId, ruleStatus, activeEventIds);
                    if (LOG_DEBUG) {
                        Log.d(TAG,
                              "matchEventDataWithRules activeEventIds after updation = "
                              + activeEventIds);
                    }
                    boolean eventBelongsToRule = eventBelongsToRule(
                                                     calendarIds, allDaySelection,
                                                     acceptedEventsSelection,
                                                     multipleParticipantsSelection, calendarId, allDay,
                                                     status, multipleParticipants);
                    boolean ruleIsActiveForEvent = false;
                    if (activeEventIds != null
                            && activeEventIds.contains(String.valueOf(id))) {
                        ruleIsActiveForEvent = true;
                    }
                    if (eventBelongsToRule) {
                        if (ruleStatus != CalendarEventDatabase.RULE_INACTIVE) {
                            if (!ruleIsActiveForEvent) {
                                if (activeEventIds != null) {
                                    activeEventIds = activeEventIds + OR
                                            + String.valueOf(id);
                                } else {
                                    // case of transition from one meeting to
                                    // another running back to back
                                    activeEventIds = String.valueOf(id);
                                    ruleStatus = CalendarEventDatabase.RULE_TRANSITIONING;
                                }
                                database.updateRow(ruleId, ruleStatus,
                                        activeEventIds);
                            }
                        } else {
                            if (activeEventIds != null) {
                                activeEventIds = activeEventIds + OR
                                                 + String.valueOf(id);
                            } else {
                                activeEventIds = String.valueOf(id);
                            }
                            database.updateRow(ruleId, ruleStatus,
                                    activeEventIds);
                        }
                    } else {
                        if (ruleIsActiveForEvent) {
                            activeEventIds = removeIdFromActiveEventIds(
                                                 activeEventIds, id);
                            database.updateRow(ruleId, ruleStatus,
                                    activeEventIds);
                        }
                    }
                    if (LOG_INFO) {
                        Log.i(TAG, "matchEventDataWithRules ruleId = " + ruleId
                                + " ruleStatus = " + ruleStatus
                                + " eventBelongsToRule = " + eventBelongsToRule
                                + " ruleIsActiveForEvent = "
                                + ruleIsActiveForEvent + " activeEventIds = "
                                + activeEventIds);
                    }
                } while (dbCursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,
                  "matchEventDataWithRules failed to query calendar events database");
        } finally {
            if (dbCursor != null) {
                dbCursor.close();
            }
        }
    }

    /**
     * Finds out if event belongs to the current rule
     *
     * @param calendarIds
     *            - String containing calendar IDs, for which the rule is
     *            active, separated by {@link CalendarEventSensorConstants#OR}
     * @param allDaySelection
     *            - {@link CalendarEventDatabase#SELECTED} or
     *            {@link CalendarEventDatabase#NOT_SELECTED}
     * @param acceptedEventsSelection
     *            - {@link CalendarEventDatabase#SELECTED} or
     *            {@link CalendarEventDatabase#NOT_SELECTED}
     * @param multipleParticipantsSelection
     *            - {@link CalendarEventDatabase#SELECTED} or
     *            {@link CalendarEventDatabase#NOT_SELECTED}
     * @param calendarId
     *            - value of Events.CALENDAR_ID column
     * @param allDay
     *            - value of Events.ALL_DAY column
     * @param status
     *            - value of Events.SELF_ATTENDEE_STATUS column
     * @param multipleParticipants
     *            - true if the event involves multiple participants, false
     *            otherwise
     * @return true if event belongs to the rule, false otherwise
     */
    private boolean eventBelongsToRule(String calendarIds, int allDaySelection,
                                       int acceptedEventsSelection, int multipleParticipantsSelection,
                                       String calendarId, int allDay, int status,
            boolean multipleParticipants) {
        boolean eventBelongsToRule = false;
        MismatchType mismatchType = MismatchType.NO_MISMATCH;
        if (calendarIds == null || calendarIds.contains(calendarId)) {
            if (allDaySelection == CalendarEventDatabase.SELECTED) {
                if (allDay == 0) {
                    eventBelongsToRule = true;
                }
            } else {
                eventBelongsToRule = true;
            }
            if (eventBelongsToRule) {
                eventBelongsToRule = false;
                if (acceptedEventsSelection == CalendarEventDatabase.SELECTED) {
                    if (status == Events.STATUS_CONFIRMED) {
                        eventBelongsToRule = true;
                    }
                } else {
                    eventBelongsToRule = true;
                }
                if (eventBelongsToRule) {
                    eventBelongsToRule = false;
                    if (multipleParticipantsSelection == CalendarEventDatabase.SELECTED) {
                        if (multipleParticipants) {
                            eventBelongsToRule = true;
                        }
                    } else {
                        eventBelongsToRule = true;
                    }
                    if (!eventBelongsToRule) {
                        mismatchType = MismatchType.MULTIPLE_ATTENDEES_MISMATCH;
                    }
                } else {
                    mismatchType = MismatchType.ACCEPTED_EVENT_MISMATCH;
                }
            } else {
                mismatchType = MismatchType.ALL_DAY_EVENT_MISMATCH;
            }
        } else {
            mismatchType = MismatchType.CALENDAR_IDS_MISMATCH;
        }
        if (LOG_INFO) {
            Log.i(TAG, "eventBelongsToRule " + eventBelongsToRule
                    + " mismatchType " + mismatchType);
        }
        return eventBelongsToRule;
    }

    /**
     * Broadcasts the intents for rules getting activated and deactivated
     */
    private void broadcastIntents() {
        CalendarEventDatabase database = CalendarEventDatabase
                .getInstance(this);
        Cursor dbCursor = null;
        try {
            dbCursor = database.query(null, null, null, null);
            if (dbCursor != null && dbCursor.moveToFirst()) {
                HashMap<String, String> configsStatesMap = new HashMap<String, String>();
                ArrayList<String> configsUnderTransition = new ArrayList<String>();
                do {
                    int ruleId = dbCursor
                            .getInt(dbCursor
                                    .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_RULE_ID));
                    String activeEventIds = dbCursor
                            .getString(dbCursor
                                    .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_EVENT_IDS));
                    int ruleStatus = dbCursor
                            .getInt(dbCursor
                                    .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_ACTIVE));
                    String calendarIds = dbCursor
                            .getString(dbCursor
                                    .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_CALENDAR_IDS));
                    boolean excludeAllDayEvents = dbCursor
                            .getInt(dbCursor
                                    .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_ALL_DAY_EVENTS)) == CalendarEventDatabase.SELECTED;
                    boolean onlyIncludeEventsWithMultiplePeople = dbCursor
                            .getInt(dbCursor
                                    .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_MULTIPLE_PARTICIPANTS)) == CalendarEventDatabase.SELECTED;
                    boolean onlyIncludeAcceptedEvents = dbCursor
                            .getInt(dbCursor
                                    .getColumnIndexOrThrow(CalendarEventDatabase.COLUMN_SHOW_ACCEPTED_EVENTS)) == CalendarEventDatabase.SELECTED;
                    if (activeEventIds == null) {
                        database.updateRow(ruleId,
                                CalendarEventDatabase.RULE_INACTIVE, null);
                        configsStatesMap.put(CalendarEventUtils
                                .getConfigString(calendarIds,
                                        excludeAllDayEvents,
                                        onlyIncludeEventsWithMultiplePeople,
                                        onlyIncludeAcceptedEvents), FALSE);
                    } else if (ruleStatus != CalendarEventDatabase.RULE_ACTIVE) {
                        database.updateRow(ruleId,
                                CalendarEventDatabase.RULE_ACTIVE,
                                activeEventIds);
                        String config = CalendarEventUtils.getConfigString(
                                calendarIds, excludeAllDayEvents,
                                onlyIncludeEventsWithMultiplePeople,
                                onlyIncludeAcceptedEvents);
                        if (ruleStatus == CalendarEventDatabase.RULE_TRANSITIONING) {
                            configsStatesMap.put(config, FALSE);
                            configsUnderTransition.add(config);
                        } else {
                            configsStatesMap.put(config, TRUE);
                        }
                    }
                } while (dbCursor.moveToNext());
                CalendarEventUtils.sendNotifyIntent(this, configsStatesMap);
                configsStatesMap.clear();
                if (!configsUnderTransition.isEmpty()) {
                    for (String config : configsUnderTransition) {
                        configsStatesMap.put(config, TRUE);
                    }
                    CalendarEventUtils.sendNotifyIntent(this, configsStatesMap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,
                    "broadcastIntents failed to query calendar events database");
        } finally {
            if (dbCursor != null) {
                dbCursor.close();
            }
        }
        if (LOG_DEBUG) {
            database.printDb();
        }
    }

    /**
     * Updates the String containing event IDs of calendar events separated by
     * {@link CalendarEventSensorConstants#OR}. This method removes the event
     * IDs of calendar events that have ended
     *
     * @param activeEventIds
     *            - String containing event IDs of calendar events for which a
     *            particular rule is active
     * @param eventIds
     *            - The long array containing event IDs of currently active
     *            calendar events
     * @return - The updated String obtained after removing the event IDs of
     *         calendar events that have ended. Returns null if all events have
     *         ended
     */
    private String updateActiveEventIds(String activeEventIds, long[] eventIds) {
        if (activeEventIds != null) {
            String[] activeEventIdsArr = activeEventIds.split(OR);
            for (int i = 0; i < activeEventIdsArr.length; i++) {
                boolean eventPresent = false;
                for (long eventId : eventIds) {
                    if (String.valueOf(eventId).equals(activeEventIdsArr[i])) {
                        eventPresent = true;
                        break;
                    }
                }
                if (!eventPresent) {
                    activeEventIdsArr[i] = INVALID_EVENT_ID;
                }
            }
            StringBuilder newActiveEventIds = new StringBuilder();
            for (String activeEventId : activeEventIdsArr) {
                if (!activeEventId.equals(INVALID_EVENT_ID)) {
                    if (newActiveEventIds.length() > 0) {
                        newActiveEventIds.append(OR + activeEventId);
                    } else {
                        newActiveEventIds.append(activeEventId);
                    }
                }
            }
            if (newActiveEventIds.length() > 0) {
                return newActiveEventIds.toString();
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * Removes the id from activeEventIds and returns the updated activeEventIds
     *
     * @param activeEventIds
     *            - String containing event IDs of calendar events separated by
     *            {@link CalendarEventSensorConstants#OR}
     * @param id
     *            - the ID of calendar event to be removed from activeEventIds
     * @return the updated activeEventIds String
     */
    private String removeIdFromActiveEventIds(String activeEventIds, long id) {
        if (activeEventIds != null) {
            String[] activeEventIdsArr = activeEventIds.split(OR);
            for (int i = 0; i < activeEventIdsArr.length; i++) {
                if (activeEventIdsArr[i].equals(String.valueOf(id))) {
                    activeEventIdsArr[i] = INVALID_EVENT_ID;
                    break;
                }
            }
            StringBuilder newActiveEventIds = new StringBuilder();
            for (String activeEventId : activeEventIdsArr) {
                if (!activeEventId.equals(INVALID_EVENT_ID)) {
                    if (newActiveEventIds.length() > 0) {
                        newActiveEventIds.append(OR + activeEventId);
                    } else {
                        newActiveEventIds.append(activeEventId);
                    }
                }
            }
            if (newActiveEventIds.length() > 0) {
                return newActiveEventIds.toString();
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * This class acts as data structure for holding event's information
     *
     * @author wkh346
     *
     */
    private static class Instance {

        public long id = -1;
        public long begin = -1;
        public long end = -1;
        public long eventId = -1;
        public String eventTitle = "";
        public boolean allDay = false;

        /**
         * Constructor
         *
         * @param id
         *            - instance id
         * @param begin
         *            - begin time in milliseconds
         * @param end
         *            - end time in milliseconds
         * @param eventId
         *            - event id
         * @param eventTitle
         *            - event title
         * @param allDay
         *            - true if event is all day event
         */
        public Instance(long id, long begin, long end, long eventId,
                String eventTitle, boolean allDay) {
            this.id = id;
            this.begin = begin;
            this.end = end;
            this.eventId = eventId;
            this.eventTitle = eventTitle;
            this.allDay = allDay;
        }

        /**
         * This method creates a readable string for logging purpose
         *
         * @param context
         *            - application's context
         * @return - readable String
         */
        public String getInstanceInfo(Context context) {
            String beginDateAndTime = DateUtils.formatDateTime(context, begin,
                    getDateTimeFormatFlag(context));
            String endDateAndTime = DateUtils.formatDateTime(context, end,
                    getDateTimeFormatFlag(context));
            StringBuilder builder = new StringBuilder();
            builder.append("instanceId ").append(id)
                    .append(" beginDateAndTime ").append(beginDateAndTime)
                    .append(" endDateAndTime ").append(endDateAndTime)
                    .append(" eventId ").append(eventId).append(" eventTitle ")
                    .append(eventTitle).append(" allDay ").append(allDay);
            return builder.toString();
        }
    }

    /**
     * Method to find ids of all events that the user is attending now
     */
    private void queryInstancesAndNotify() {
        long currentMillis = System.currentTimeMillis();
        ContentResolver resolver = getContentResolver();
        Cursor instancesCursor = null;
        Cursor calendarCursor = null;
        try {
            String calendarSelection = Calendars.VISIBLE + EQUALS_TO + "1";
            calendarCursor = resolver.query(Calendars.CONTENT_URI,
                    CALENDAR_PROJECTION, calendarSelection, null, null);
            long[] visibleCalendarIds = null;
            if (calendarCursor != null && calendarCursor.moveToFirst()) {
                visibleCalendarIds = new long[calendarCursor.getCount()];
                int index = 0;
                do {
                    visibleCalendarIds[index] = calendarCursor
                            .getLong(calendarCursor
                                    .getColumnIndexOrThrow(Calendars._ID));
                    index++;
                } while (calendarCursor.moveToNext());
            }
            ArrayList<Instance> busyInstances = new ArrayList<Instance>();
            long nextCheckPointTime = currentMillis + ONE_DAY_WINDOW;
            if (visibleCalendarIds != null && visibleCalendarIds.length > 0) {
                Arrays.sort(visibleCalendarIds);
                long beginMillis = currentMillis;
                long endMillis = currentMillis + ONE_DAY_WINDOW;
                String instancesSelection = Events.DELETED + NOT_EQUAL_TO + "1"
                        + AND + Instances.END + GREATER_THAN
                        + String.valueOf(currentMillis);
                StringBuilder path = new StringBuilder();
                path.append(beginMillis).append(FORWARD_SLASH)
                        .append(endMillis);
                Uri instancesUri = Uri.withAppendedPath(Instances.CONTENT_URI,
                        path.toString());
                if (LOG_INFO) {
                    Log.i(TAG,
                            "queryInstances query uri "
                                    + instancesUri.toString());
                }
                instancesCursor = resolver.query(instancesUri,
                        INSTANCES_PROJECTION, instancesSelection, null,
                        INSTANCES_SORT_ORDER);
                if (instancesCursor != null && instancesCursor.moveToFirst()) {
                    do {
                        long id = instancesCursor.getLong(instancesCursor
                                .getColumnIndexOrThrow(Instances._ID));
                        long begin = instancesCursor.getLong(instancesCursor
                                .getColumnIndexOrThrow(Instances.BEGIN));
                        long end = instancesCursor.getLong(instancesCursor
                                .getColumnIndexOrThrow(Instances.END));
                        long eventId = instancesCursor.getLong(instancesCursor
                                .getColumnIndexOrThrow(Instances.EVENT_ID));
                        String eventTitle = instancesCursor
                                .getString(instancesCursor
                                        .getColumnIndexOrThrow(Instances.TITLE));
                        boolean allDay = instancesCursor.getInt(instancesCursor
                                .getColumnIndexOrThrow(Instances.ALL_DAY)) != 0;
                        long calendarId = instancesCursor
                                .getLong(instancesCursor
                                        .getColumnIndexOrThrow(Instances.CALENDAR_ID));
                        if (Arrays.binarySearch(visibleCalendarIds, calendarId) > -1) {
                            // For those instances that the user is attending
                            // now
                            if (begin <= currentMillis && end > currentMillis) {
                                busyInstances.add(new Instance(id, begin, end,
                                        eventId, eventTitle, allDay));
                            }
                            // Get the next check point time
                            if (begin > currentMillis
                                    && begin < nextCheckPointTime) {
                                nextCheckPointTime = begin;
                            }
                            if (end > currentMillis && end < nextCheckPointTime) {
                                nextCheckPointTime = end;
                            }
                        }
                    } while (instancesCursor.moveToNext());
                }
            }
            // Schedule the AlarmManager for next check point
            scheduleNextCheck(nextCheckPointTime + CHECK_POINT_DELAY);
            // Calculate the longest end time among busy instances and
            // send notification
            calculateBusyInstancesAndNotify(busyInstances);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (calendarCursor != null) {
                calendarCursor.close();
            }
            if (instancesCursor != null) {
                instancesCursor.close();
            }
        }
    }

    /**
     * Schedule the AlarmManager for next check point.
     *
     * @param triggerTime
     *            The time to set the alarm.
     */
    private void scheduleNextCheck(long triggerTime) {
        AlarmManager alarmManager = (AlarmManager) this
                .getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ACTION_SCHEDULE_EVENT_AWARE);
        PendingIntent pending = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_NO_CREATE);
        if (pending != null) {
            alarmManager.cancel(pending);
        }
        pending = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        int type = CalendarEventUtils.strictlyNotifyOnTime(this) ? AlarmManager.RTC_WAKEUP
                : AlarmManager.RTC;
        if (LOG_INFO) {
            Log.i(TAG,
                    "scheduleNextCheck at: "
                            + DateUtils.formatDateTime(this, triggerTime,
                                    getDateTimeFormatFlag(this))
                            + " with type " + type);
        }
        alarmManager.set(type, triggerTime, pending);
    }

    /**
     * This method notifies {@link CalendarEventService} whenever an event
     * starts running or stops running
     *
     * @param busyInstances
     *            - the list of active events
     */
    private void calculateBusyInstancesAndNotify(
            ArrayList<Instance> busyInstances) {
        if (busyInstances == null || busyInstances.size() == 0) {
            if (LOG_INFO) {
                Log.i(TAG, "calculateBusyInstancesAndNotify notifying as free");
            }
            deactivateAllRules(true);
        } else {
            // For those instances that the user is attending now are added to
            // busyInstances
            if (LOG_INFO) {
                Log.i(TAG, "calculateBusyInstancesAndNotify notifying as busy");
            }
            long[] eventIds = new long[busyInstances.size()];
            int index = 0;
            for (Instance instance : busyInstances) {
                eventIds[index] = instance.eventId;
                index++;
                if (LOG_INFO) {
                    Log.i(TAG, "calculateBusyInstancesAndNotify instance info "
                            + instance.getInstanceInfo(this));
                }
            }
            triggerRules(eventIds);
        }
    }

    /**
     * This method cancels all pending intents registered with alarm manager for
     * {@link CalendarEventSensorConstants#ACTION_SCHEDULE_EVENT_AWARE}
     */
    private void removeExistingAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ACTION_SCHEDULE_EVENT_AWARE);
        PendingIntent pending = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_NO_CREATE);
        if (pending != null) {
            alarmManager.cancel(pending);
            if (LOG_INFO) {
                Log.i(TAG,
                        "removeExistingAlarm removed existing alarm from AlarmManager");
            }
        }
    }

    /**
     * This method refreshes event aware functionality by invoking
     * {@link #removeExistingAlarm()} and {@link #queryInstancesAndNotify()}
     */
    private void refreshEventAware() {
        if (LOG_INFO) {
            Log.i(TAG,
                    "refreshEventAware will remove existing alarms and query instances again");
        }
        removeExistingAlarm();
        queryInstancesAndNotify();
    }

    /**
     * This method returns appropriate flag needed by {@link DateUtils} for
     * formatting date
     *
     * @param context
     *            - application's context
     * @return - flag needed by {@link DateUtils} for formatting date
     */
    private static int getDateTimeFormatFlag(Context context) {
        int flags = DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_TIME;
        if (DateFormat.is24HourFormat(context)) {
            flags |= DateUtils.FORMAT_24HOUR;
        }
        return flags;
    }

}