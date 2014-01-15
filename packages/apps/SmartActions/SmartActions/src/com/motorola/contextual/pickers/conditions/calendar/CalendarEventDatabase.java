/*
 * @(#)CalendarEventDatabase.java
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

import com.motorola.contextual.smartprofile.Constants;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Singleton class that provides the methods for interacting with the calendar
 * events database.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Extends SQLiteOpenHelper
 *      Implements CalendarEventSensorConstants
 *
 * RESPONSIBILITIES:
 * This class is responsible providing methods for interacting with the calendar events database.
 * This is a Singleton class.
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class CalendarEventDatabase extends SQLiteOpenHelper implements
    CalendarEventSensorConstants {

    /**
     * The singleton instance of this class
     */
    private static CalendarEventDatabase mDatabaseHelper = null;

    /**
     * Variable to identify if an option is selected
     */
    public static final int SELECTED = 1;

    /**
     * Variable to identify if an option is not selected
     */
    public static final int NOT_SELECTED = 0;

    /**
     * Variable to identify that rule id is invalid
     */
    public static final int INVALID_RULE_ID = -1;

    /**
     * Value for marking a rule as transitioning
     */
    public static final int RULE_TRANSITIONING = 2;

    /**
     * Value for marking a rule as active
     */
    public static final int RULE_ACTIVE = 1;

    /**
     * Value for marking a rule as inactive
     */
    public static final int RULE_INACTIVE = 0;

    /**
     * TAG for logging the messages
     */
    private static final String TAG = "CalendarEventDatabase";

    /**
     * Name of calendar events database
     */
    public static final String CALENDAR_DATABASE_NAME = "calendar_event_sensor.db";

    /**
     * Name of calendar events table
     */
    public static final String CALENDAR_TABLE_NAME = "calendar_event_rules";

    /**
     * The integer primary key column of the table
     */
    public static final String COLUMN_ID = "id";

    /**
     * Column for storing calendar IDs. The value will be a String containing
     * calendar IDs separated by "OR"
     */
    public static final String COLUMN_CALENDAR_IDS = "calendar_ids";

    /**
     * Column for storing user selection for all day events. The value will
     * either {@link SELECTED} or {@link NOT_SELECTED}
     */
    public static final String COLUMN_ALL_DAY_EVENTS = "all_day_events";

    /**
     * Column for storing user selection for accepted events. The value will
     * either {@link SELECTED} or {@link NOT_SELECTED}
     */
    public static final String COLUMN_SHOW_ACCEPTED_EVENTS = "show_accepted_events";

    /**
     * Column for storing user selection for events with multiple participants.
     * The value will either {@link SELECTED} or {@link NOT_SELECTED}
     */
    public static final String COLUMN_MULTIPLE_PARTICIPANTS = "multiple_participants";

    /**
     * Column for storing IDs of calendar events. The value will be a String
     * containing IDs of calendar events separated by "OR"
     */
    public static final String COLUMN_EVENT_IDS = "event_ids";

    /**
     * Column for storing the status of the rule. The value will be 1 if rule is
     * active and 0 if rule is inactive
     */
    public static final String COLUMN_ACTIVE = "rule_active";

    /**
     * The unique ID of a rule. The value can be an integer greater than zero
     */
    public static final String COLUMN_RULE_ID = "rule_id";

    /**
     * The version of calendar events database
     */
    public static final int CALENDAR_EVENTS_DB_VERSION = 1;

    /**
     * Boolean to specify whether the calendar events database entries shall be
     * backed up
     */
    private static final boolean EXPORT_CALENDAR_RULES_FOR_BACKUP = true;

    /**
     * Interface for specifying various strings used in Database creation,
     * update etc
     *
     */
    public interface CalendarDbSytax {

        /**
         * For creating the table
         */
        public static final String CREATE_TABLE = "CREATE TABLE";

        /**
         * The integer primary key
         */
        public static final String INTEGER_PRIMARY_KEY = "INTEGER PRIMARY KEY";

        /**
         * The text type
         */
        public static final String TEXT = "TEXT";

        /**
         * The integer type
         */
        public static final String INTEGER = "INTEGER";

        /**
         * Syntax for dropping a table
         */
        public static final String DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS";
    }

    /**
     * constant for defining default rule id
     */
    public static final int DEFAULT_RULE_ID = 1;

    /**
     * Returns the singleton instance of this class
     *
     * @param context
     *            - context of the activity or service
     * @return Singleton instance of this class
     */
    public synchronized static CalendarEventDatabase getInstance(Context context) {
        if (mDatabaseHelper == null) {
            mDatabaseHelper = new CalendarEventDatabase(context,
                    CALENDAR_DATABASE_NAME, null, CALENDAR_EVENTS_DB_VERSION);
        }
        return mDatabaseHelper;
    }

    /**
     * Constructor
     *
     * @param context
     *            - context of the activity or service
     * @param name
     *            - name of the database
     * @param factory
     *            - to use for creating cursor objects, or null for the default
     * @param version
     *            - number of the database (starting at 1); if the database is
     *            older, onUpgrade(SQLiteDatabase, int, int) will be used to
     *            upgrade the database
     */
    private CalendarEventDatabase(Context context, String name,
                                  CursorFactory factory, int version) {
        super(context, CALENDAR_DATABASE_NAME, null, CALENDAR_EVENTS_DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db);
    }

    /**
     * This method creates the table defined by {@link #CALENDAR_TABLE_NAME}
     *
     * @param db
     *            - the reference to SQLiteDatabase instance
     */
    private synchronized void createTable(SQLiteDatabase db) {
        StringBuilder createTableSql = new StringBuilder();
        try {
            createTableSql.append(CalendarDbSytax.CREATE_TABLE).append(SPACE)
                    .append(CALENDAR_TABLE_NAME).append(SPACE)
                    .append(LEFT_PAREN).append(COLUMN_ID).append(SPACE)
                    .append(CalendarDbSytax.INTEGER_PRIMARY_KEY).append(COMMA)
                    .append(COLUMN_CALENDAR_IDS).append(SPACE)
                    .append(CalendarDbSytax.TEXT).append(COMMA)
                    .append(COLUMN_ALL_DAY_EVENTS).append(SPACE)
                    .append(CalendarDbSytax.INTEGER).append(COMMA)
                    .append(COLUMN_SHOW_ACCEPTED_EVENTS).append(SPACE)
                    .append(CalendarDbSytax.INTEGER).append(COMMA)
                    .append(COLUMN_MULTIPLE_PARTICIPANTS).append(SPACE)
                    .append(CalendarDbSytax.INTEGER).append(COMMA)
                    .append(COLUMN_EVENT_IDS).append(SPACE)
                    .append(CalendarDbSytax.TEXT).append(COMMA)
                    .append(COLUMN_ACTIVE).append(SPACE)
                    .append(CalendarDbSytax.INTEGER).append(COMMA)
                    .append(COLUMN_RULE_ID).append(SPACE)
                    .append(CalendarDbSytax.INTEGER).append(RIGHT_PAREN)
                    .append(SEMI_COLON);
            db.execSQL(createTableSql.toString());
        } catch (Exception exception) {
            exception.printStackTrace();
            Log.e(TAG, "createTable error while excecuting the SQL "
                    + createTableSql);
        }
    }

    /**
     * This method drops the table defined by {@link #CALENDAR_TABLE_NAME}
     *
     * @param db
     *            - the reference to SQLiteDatabase instance
     */
    private synchronized void dropTable(SQLiteDatabase db) {
        StringBuilder dropTableSql = new StringBuilder();
        try {
            dropTableSql.append(CalendarDbSytax.DROP_TABLE_IF_EXISTS)
                    .append(SPACE).append(CALENDAR_TABLE_NAME);
            db.execSQL(dropTableSql.toString());
        } catch (Exception exception) {
            exception.printStackTrace();
            Log.e(TAG, "dropTable error while executing SQL " + dropTableSql);
        }

    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (LOG_INFO) {
            Log.i(TAG, "onDowngrade invoked oldVersion " + oldVersion
                    + " newVersion " + newVersion);
        }
        if (newVersion < oldVersion) {
            dropTable(db);
            createTable(db);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.e(TAG, "onUpgrade called, But unimplemented");

        /**
         * Recommendations for onUpgrade 1. Create a back up of the table if the
         * upgrade operation is not straight forward i.e., they cannot be
         * executed with single SQL command. Examples of straight forward
         * upgrades are adding a column, droping a table etc.. Examples of
         * non-straight forward upgrades are renaming a column, dropping a
         * column, changing the type etc... 2. Create a new table with required
         * changes 3. Restore the data from the backed up table 4. Delete the
         * old table 5. If the upgrade operation is straight forward, tables
         * need not be backed up and deleted 6. Remember onUpgrade can get
         * called for any versions, i.e., 1 to 2, 2 to 3 or 1 to 3.
         *
         * Refer to timeframe sensor DB's onUpgrade() for a sample
         * implementation
         */
    }

    /**
     * Checks in the database whether a row shall be updated or a new row shall
     * be inserted for current selection. Returns the value of
     * {@link #COLUMN_RULE_ID} column of the current row
     *
     * @param calendarIds
     *            - IDs of selected calendars separated by "OR"
     * @param allDayEvents
     *            - {@link #SELECTED} or {@link #NOT_SELECTED}
     * @param acceptedEvents
     *            - {@link #SELECTED} or {@link #NOT_SELECTED}
     * @param multipleParticipantsEvents
     *            - {@link #SELECTED} or {@link #NOT_SELECTED}
     * @param export
     *            - if true then config is exported
     * @return The value of {@link #COLUMN_RULE_ID} column of the current row
     */
    public synchronized int insertAndUpdateRow(Context context,
            String calendarIds, int allDayEvents, int acceptedEvents,
            int multipleParticipantsEvents, boolean export) {
        if (calendarIds != null && calendarIds.equals(NULL_STRING)) {
            calendarIds = null;
        }
        int ruleId = ruleExists(calendarIds, allDayEvents, acceptedEvents,
                multipleParticipantsEvents);
        if (ruleId == INVALID_RULE_ID) {
            ruleId = getRuleId();
        } else {
            if (LOG_INFO) {
                Log.i(TAG,
                        "insertAndUpdateRow returning with existing rule id = "
                                + ruleId);
            }
            return ruleId;
        }
        insertAndUpdateRow(ruleId, calendarIds, allDayEvents, acceptedEvents,
                multipleParticipantsEvents);
        if (EXPORT_CALENDAR_RULES_FOR_BACKUP && export) {
            exportRulesForBackup(context);
        }
        return ruleId;
    }

    /**
     * This method inserts the data in calendar events database
     *
     * @param ruleId
     *            - ruleId corresponding to this configuration
     * @param calendarIds
     *            - IDs of selected calendars separated by "OR"
     * @param allDayEvents
     *            - {@link #SELECTED} or {@link #NOT_SELECTED}
     * @param acceptedEvents
     *            - {@link #SELECTED} or {@link #NOT_SELECTED}
     * @param multipleParticipantsEvents
     *            - {@link #SELECTED} or {@link #NOT_SELECTED}
     */
    private synchronized void insertAndUpdateRow(int ruleId, String calendarIds,
            int allDayEvents, int acceptedEvents, int multipleParticipantsEvents) {
        if (calendarIds != null && calendarIds.equals(NULL_STRING)) {
            calendarIds = null;
        }
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            if (db != null) {
                ContentValues rowData = new ContentValues();
                rowData.put(COLUMN_CALENDAR_IDS, calendarIds);
                rowData.put(COLUMN_ALL_DAY_EVENTS, allDayEvents);
                rowData.put(COLUMN_SHOW_ACCEPTED_EVENTS, acceptedEvents);
                rowData.put(COLUMN_MULTIPLE_PARTICIPANTS,
                        multipleParticipantsEvents);
                String eventIds = null;
                rowData.put(COLUMN_EVENT_IDS, eventIds);
                rowData.put(COLUMN_ACTIVE, RULE_INACTIVE);
                rowData.put(COLUMN_RULE_ID, ruleId);
                db.insert(CALENDAR_TABLE_NAME, null, rowData);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,
                    "insertAndUpdateRow failed to insert a new row in calendar events database");
            ruleId = INVALID_RULE_ID;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * This method starts the service for exporting the calendar database
     * entries
     *
     * @param context
     *            - application's context
     */
    private void exportRulesForBackup(Context context) {
        Intent serviceIntent = new Intent(CALENDAR_EVENTS_EXPORT_ACTION);
        serviceIntent.setClass(context, CalendarEventService.class);
        context.startService(serviceIntent);
    }

    /**
     * Checks in the database if an entry exists that matches the current
     * selection. Returns the value of {@link #COLUMN_RULE_ID} column of the row
     * found. If no match is found, {@link #INVALID_RULE_ID} is returned
     *
     * @param calendarIds
     *            - IDs of selected calendars separated by
     *            {@link CalendarEventSensorConstants#OR}
     * @param allDayEvents
     *            - {@link #SELECTED} or {@link #NOT_SELECTED}
     * @param acceptedEvents
     *            - {@link #SELECTED} or {@link #NOT_SELECTED}
     * @param multipleParticipantsEvents
     *            - {@link #SELECTED} or {@link #NOT_SELECTED}
     * @return The value of {@link #COLUMN_RULE_ID} column of the row found. If
     *         no match is found, {@link #INVALID_RULE_ID} is returned
     */
    private synchronized final int ruleExists(String calendarIds,
            int allDayEvents, int acceptedEvents, int multipleParticipantsEvents) {
        if (calendarIds != null && calendarIds.equals(NULL_STRING)) {
            calendarIds = null;
        }
        SQLiteDatabase db = null;
        int existingRuleId = INVALID_RULE_ID;
        Cursor cursor = null;
        try {
            db = getReadableDatabase();
            if (db != null) {
                cursor = db.query(CALENDAR_TABLE_NAME, new String[] {
                        COLUMN_ALL_DAY_EVENTS, COLUMN_CALENDAR_IDS,
                        COLUMN_MULTIPLE_PARTICIPANTS,
                        COLUMN_SHOW_ACCEPTED_EVENTS, COLUMN_RULE_ID }, null,
                        null, null, null, null);
            }
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String calendarIdsFromDb = cursor.getString(cursor
                                               .getColumnIndexOrThrow(COLUMN_CALENDAR_IDS));
                    int allDayEventsFromDb = cursor.getInt(cursor
                                                           .getColumnIndexOrThrow(COLUMN_ALL_DAY_EVENTS));
                    int acceptedEventsFromDb = cursor
                                               .getInt(cursor
                                                       .getColumnIndexOrThrow(COLUMN_SHOW_ACCEPTED_EVENTS));
                    int multipleParticipantsEventsFromDb = cursor
                                                           .getInt(cursor
                                                                   .getColumnIndexOrThrow(COLUMN_MULTIPLE_PARTICIPANTS));
                    int ruleId = cursor.getInt(cursor
                                               .getColumnIndexOrThrow(COLUMN_RULE_ID));
                    if (allDayEvents == allDayEventsFromDb
                            && acceptedEvents == acceptedEventsFromDb
                            && multipleParticipantsEvents == multipleParticipantsEventsFromDb) {
                        if (calendarIds == null && calendarIdsFromDb == null) {
                            existingRuleId = ruleId;
                            break;
                        } else if (calendarIds != null
                                   && calendarIdsFromDb != null
                                   && calendarIds.equals(calendarIdsFromDb)) {
                            existingRuleId = ruleId;
                            break;
                        } else if (calendarIds != null
                                   && calendarIdsFromDb != null
                                   && calendarIds.length() == calendarIdsFromDb
                                   .length()) {
                            String[] calendarIdsArr = calendarIds.split(OR);
                            String[] calendarIdsFromDbArr = calendarIdsFromDb
                                                            .split(OR);
                            boolean fullMatch = false;
                            for (String calId : calendarIdsArr) {
                                fullMatch = false;
                                for (String calIdFromDb : calendarIdsFromDbArr) {
                                    if (calId.equals(calIdFromDb)) {
                                        fullMatch = true;
                                        break;
                                    }
                                }
                                if (!fullMatch) {
                                    break;
                                }
                            }
                            if (fullMatch) {
                                existingRuleId = ruleId;
                                break;
                            }
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
            Log.e(TAG, "ruleExists failed to open calendar events database");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "ruleExists failed to query CALENDAR_TABLE_NAME");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        return existingRuleId;
    }

    /**
     * Genarates the rule id for a new row. Returns the rule id
     *
     * @return The rule id for a new row
     */
    private synchronized final int getRuleId() {
        int ruleId = INVALID_RULE_ID;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getReadableDatabase();
            if (db != null) {
                cursor = db.query(CALENDAR_TABLE_NAME,
                        new String[] { COLUMN_RULE_ID }, null, null, null,
                        null, null);
            }
            if (cursor != null && cursor.moveToFirst()) {
                int[] ruleIdArr = new int[cursor.getCount()];
                int index = 0;
                do {
                    int id = cursor.getInt(0);
                    ruleIdArr[index] = id;
                    index++;
                } while (cursor.moveToNext());
                cursor.close();
                boolean ruleIdDecided = false;
                // try to find out which rule ID is available. The loop starts
                // from value 1 and tries to find out the nearest available rule
                // ID
                int tentativeRuleId = DEFAULT_RULE_ID;
                while (!ruleIdDecided) {
                    for (int id : ruleIdArr) {
                        if (id == tentativeRuleId) {
                            ruleIdDecided = false;
                            break;
                        } else {
                            ruleIdDecided = true;
                        }
                    }
                    if (!ruleIdDecided) {
                        tentativeRuleId++;
                    } else {
                        ruleId = tentativeRuleId;
                    }
                }
            } else {
                ruleId = DEFAULT_RULE_ID;
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
            Log.e(TAG, "getRuleId failed to open calendar events database");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getRuleId failed to query CALENDAR_TABLE_NAME");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        if (LOG_DEBUG) {
            Log.d(TAG, "getRuleId ruleId = " + ruleId);
        }
        return ruleId;
    }

    /**
     * Update the row with rule id = ruleId
     *
     * @param ruleId
     *            - The value of {@link #COLUMN_RULE_ID} column
     * @param active
     *            - To identify if a rule is active or not
     * @param eventIds
     *            - String containing event IDs of the active events separated
     *            by {@link CalendarEventSensorConstants#OR}
     */
    public synchronized void updateRow(int ruleId, int active, String eventIds) {
        if (LOG_INFO) {
            Log.i(TAG, "updateRow called with ruleId = " + ruleId
                  + " active = " + active + " eventIds = " + eventIds);
        }
        SQLiteDatabase db = null;
        try {
            db = getReadableDatabase();
            if (db != null) {
                ContentValues rowData = new ContentValues();
                rowData.put(COLUMN_EVENT_IDS, eventIds);
                rowData.put(COLUMN_ACTIVE, active);
                String whereClause = COLUMN_RULE_ID + EQUALS_TO
                        + INVERTED_COMMA + ruleId + INVERTED_COMMA;
                db.update(CALENDAR_TABLE_NAME, rowData, whereClause, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "updateRow failed to open calendar events database");
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * This method searches for a row containing specified configuration and if
     * the row is identified, it is deleted
     *
     * @param calendarIds
     *            - String containing IDs of selected calendars separated by
     *            {@link CalendarEventSensorConstants#OR}
     * @param allDayEvents
     *            - {@link #SELECTED} or {@link #NOT_SELECTED}
     * @param acceptedEvents
     *            - {@link #SELECTED} or {@link #NOT_SELECTED}
     * @param multipleParticipantsEvents
     *            - {@link #SELECTED} or {@link #NOT_SELECTED}
     */
    public synchronized void deleteRow(String calendarIds, int allDayEvents,
            int acceptedEvents, int multipleParticipantsEvents) {
        if (calendarIds != null && calendarIds.equals(NULL_STRING)) {
            calendarIds = null;
        }
        int ruleId = ruleExists(calendarIds, allDayEvents, acceptedEvents,
                multipleParticipantsEvents);
        if (ruleId != INVALID_RULE_ID) {
            SQLiteDatabase db = null;
            try {
                db = getWritableDatabase();
                if (db != null) {
                    String whereClause = COLUMN_RULE_ID + EQUALS_TO
                            + INVERTED_COMMA + ruleId + INVERTED_COMMA;
                    int numberOfRowsDeleted = db.delete(CALENDAR_TABLE_NAME,
                            whereClause, null);
                    if (LOG_INFO) {
                        Log.i(TAG, "deleteRow deleted " + numberOfRowsDeleted
                                + " rows for rule id " + ruleId);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "deleteRow exception happened while deleting row");
            } finally {
                if (db != null) {
                    db.close();
                }
            }
        }
    }

    /**
     * This method finds out if {@link #CALENDAR_TABLE_NAME} is empty or not
     *
     * @return - true if table is empty, false otherwise
     */
    public synchronized boolean isEmpty() {
        boolean result = false;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getWritableDatabase();
            if (db != null) {
                cursor = db.query(CALENDAR_TABLE_NAME,
                        new String[] { COLUMN_RULE_ID }, null, null, null,
                        null, null);
                if (cursor != null) {
                    result = !cursor.moveToFirst();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "isEmpty exception happened while reading database");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }
        if (LOG_INFO) {
            Log.i(TAG, "isEmpty returning " + result);
        }
        return result;
    }

    /**
     * This method deleted all rows in {@link #CALENDAR_TABLE_NAME} table
     */
    public synchronized void deleteAllRows() {
        SQLiteDatabase db = null;
        try {
            db = getWritableDatabase();
            if (db != null) {
                int numberOfRowsDeleted = db.delete(CALENDAR_TABLE_NAME, null,
                        null);
                if (LOG_INFO) {
                    Log.i(TAG, "deleteAllRows deleted " + numberOfRowsDeleted
                            + " rows");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,
                    "deleteAllRows exception happened while deleting all rows");
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    /**
     * This method returns the current state of the specified configuration
     *
     * @param calendarIds
     *            - String containing IDs of selected calendars separated by
     *            {@link CalendarEventSensorConstants#OR}
     * @param allDayEvents
     *            - {@link #SELECTED} or {@link #NOT_SELECTED}
     * @param acceptedEvents
     *            - {@link #SELECTED} or {@link #NOT_SELECTED}
     * @param multipleParticipantsEvents
     *            - {@link #SELECTED} or {@link #NOT_SELECTED}
     * @return - {@link Constants#TRUE} if the state is {@link #RULE_ACTIVE},
     *         {@link Constants#FALSE} otherwise
     */
    public synchronized String getRuleState(String calendarIds,
            int allDayEvents, int acceptedEvents, int multipleParticipantsEvents) {
        if (calendarIds != null && calendarIds.equals(NULL_STRING)) {
            calendarIds = null;
        }
        String state = FALSE;
        int ruleId = ruleExists(calendarIds, allDayEvents, acceptedEvents,
                multipleParticipantsEvents);
        if (ruleId != INVALID_RULE_ID) {
            SQLiteDatabase db = null;
            Cursor cursor = null;
            try {
                db = getReadableDatabase();
                if (db != null) {
                    String selection = COLUMN_RULE_ID + EQUALS_TO
                            + INVERTED_COMMA + ruleId + INVERTED_COMMA;
                    cursor = db.query(CALENDAR_TABLE_NAME, new String[] {
                            COLUMN_RULE_ID, COLUMN_ACTIVE }, selection, null,
                            null, null, null);
                }
                if (cursor != null && cursor.moveToFirst()) {
                    int ruleActiveState = cursor.getInt(cursor
                            .getColumnIndexOrThrow(COLUMN_ACTIVE));
                    if (ruleActiveState == RULE_ACTIVE) {
                        state = TRUE;
                    }
                }
            } catch (SQLiteException e) {
                e.printStackTrace();
                Log.e(TAG,
                        "getRuleState failed to open calendar events database");
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "getRuleState failed to query CALENDAR_TABLE_NAME");
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                if (db != null) {
                    db.close();
                }
            }
        }
        return state;
    }

    /**
     * Method for querying the calendar events database
     *
     * @param projection
     *            - A list of which columns to return. Passing null will return
     *            all columns, which is discouraged to prevent reading data from
     *            storage that isn't going to be used.
     * @param selection
     *            - A filter declaring which rows to return, formatted as an SQL
     *            WHERE clause (excluding the WHERE itself). Passing null will
     *            return all rows for the given table.
     * @param selectionArgs
     *            - You may include ?s in selection, which will be replaced by
     *            the values from selectionArgs, in order that they appear in
     *            the selection. The values will be bound as Strings.
     * @param sortOrder
     *            - How to order the rows, formatted as an SQL ORDER BY clause
     *            (excluding the ORDER BY itself). Passing null will use the
     *            default sort order, which may be unordered.
     * @return - A {@link Cursor} object, which is positioned before the first
     *         entry if the query is successful, null otherwise.
     */
    public synchronized Cursor query(String[] projection, String selection,
                                     String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = getReadableDatabase();
            if (db != null) {
                cursor = db.query(CALENDAR_TABLE_NAME, projection, selection,
                        selectionArgs, null, null, sortOrder);
            }
            if (LOG_INFO) {
                if (cursor != null) {
                    Log.i(TAG, "query cursor.getCount = " + cursor.getCount());
                }
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
            Log.e(TAG, "query failed to open calendar events database");
            cursor = null;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "query failed to query CALENDAR_TABLE_NAME");
            cursor = null;
        } finally {
            if (db != null) {
                db.close();
            }
        }
        return cursor;
    }

    /**
     * Method for printing the contents of the database. Useful while debugging
     */
    public synchronized final void printDb() {
        if (LOG_INFO) {
            SQLiteDatabase db = null;
            Cursor cursor = null;
            try {
                db = getReadableDatabase();
                if (db != null) {
                    cursor = db.query(CALENDAR_TABLE_NAME, null, null, null,
                            null, null, null);
                }
                if (cursor != null && cursor.moveToFirst()) {
                    Log.i(TAG,
                          "printDb database contents = "
                          + DatabaseUtils.dumpCursorToString(cursor));
                }
            } catch (SQLiteException e) {
                e.printStackTrace();
                Log.e(TAG, "printDb failed to open calendar events database");
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "printDb failed to query CALENDAR_TABLE_NAME");
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                if (db != null) {
                    db.close();
                }
            }
        }
    }

}
