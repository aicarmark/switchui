package com.motorola.datacollection.perfstats.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.motorola.datacollection.perfstats.DailyStats;
import com.motorola.datacollection.perfstats.PerformanceStatsService;

public class PerformanceStatsDb extends SQLiteOpenHelper {
    private static final String TAG = "PerfStatsDb";

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "PerfStatsData.db";

    /* LoggedBytes table
     * Used for storing total logged bytes per day
     */
    private static final String TBL_LOGGED_BYTES = "tbl_logged_bytes";
    private static final String LOGGED_COL_DATE = "date";
    private static final String LOGGED_COL_COMPONENT = "component";
    private static final String LOGGED_COL_ACTION = "action";
    private static final String LOGGED_COL_LOGGED = "logged";
    private static final String CREATE_TBL_LOGGED_BYTES =
        "CREATE TABLE " + TBL_LOGGED_BYTES + " (" +
        LOGGED_COL_DATE + " INTEGER NOT NULL, " +
        LOGGED_COL_COMPONENT + " TEXT NOT NULL, " +
        LOGGED_COL_ACTION + " TEXT NOT NULL, " +
        LOGGED_COL_LOGGED + " INTEGER NOT NULL DEFAULT 0, " +
        "PRIMARY KEY("
        + LOGGED_COL_DATE + " ,"
        + LOGGED_COL_COMPONENT + " ,"
        + LOGGED_COL_ACTION + "));";

    /* Action Stats table
     * Used for storing action specific data per day.
     */
    private static final String TBL_ACTION_STATS = "tbl_action_stats";
    private static final String ACTION_COL_DATE = "date";
    private static final String ACTION_COL_COMPONENT = "component";
    private static final String ACTION_COL_ACTION = "action";
    private static final String ACTION_COL_THRESHOLD = "threshold";
    private static final String ACTION_COL_NUM_GOOD = "num_good";
    private static final String ACTION_COL_NUM_BAD = "num_bad";
    private static final String ACTION_COL_MIN_TIME = "min_time";
    private static final String ACTION_COL_MAX_TIME = "max_time";
    private static final String ACTION_COL_AVG_TIME = "avg_time";
    private static final String CREATE_TBL_ACTION =
        "CREATE TABLE "  + TBL_ACTION_STATS + " (" +
        ACTION_COL_DATE + " INTEGER NOT NULL, " +
        ACTION_COL_COMPONENT + " TEXT NOT NULL, " +
        ACTION_COL_ACTION + " TEXT NOT NULL, " +
        ACTION_COL_THRESHOLD + " INTEGER NOT NULL, " +
        ACTION_COL_NUM_GOOD + " INTEGER NOT NULL DEFAULT 0, " +
        ACTION_COL_NUM_BAD + " INTEGER NOT NULL DEFAULT 0, " +
        ACTION_COL_MIN_TIME + " INTEGER NOT NULL DEFAULT 0, " +
        ACTION_COL_MAX_TIME + " INTEGER NOT NULL DEFAULT 0, " +
        ACTION_COL_AVG_TIME + " INTEGER NOT NULL DEFAULT 0, " +
        "PRIMARY KEY("
        + ACTION_COL_DATE + " ,"
        + ACTION_COL_COMPONENT + " ,"
        + ACTION_COL_ACTION + " ,"
        + ACTION_COL_THRESHOLD + "));";

    private static final String DATE_FORMAT = "yyyyMMdd";
    private static final SimpleDateFormat sDateFormat = new SimpleDateFormat(DATE_FORMAT);

    public PerformanceStatsDb(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TBL_LOGGED_BYTES);
        db.execSQL(CREATE_TBL_ACTION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub

    }

    /**
     * @return Current string value used for the date entry PerformanceStats db
     */
    static String getDate() {
        return sDateFormat.format(new Date());
    }

    Map<String, Integer> getComponentLoggedBytes(String date) {
        HashMap<String, Integer> componentMap = new HashMap<String, Integer>();

        String sql = "SELECT " + LOGGED_COL_COMPONENT + ", SUM(" + LOGGED_COL_LOGGED + ")" +
                     " FROM " + TBL_LOGGED_BYTES +
                     " WHERE " + LOGGED_COL_DATE + "=?" +
                     " GROUP BY " + LOGGED_COL_COMPONENT;

        String[] selectionArgs = { date };

        SQLiteDatabase db = null;
        try {
            db = getReadableDatabase();
        } catch (SQLiteException sqle) {
            Log.w(TAG, "Unable to get readable database.");
        }

        if (db != null) {
            Cursor cursor = null;
            try {
                cursor = db.rawQuery(sql, selectionArgs);
            } catch (SQLiteException sqle) {
                Log.w(TAG, "Unable to query data.");
            }

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        try {
                            String component = cursor.getString(0);
                            int loggedBytes = cursor.getInt(1);
                            componentMap.put(component, loggedBytes);
                        } catch (NumberFormatException nfe) {
                            Log.w(TAG, "Stored component logged bytes is corrupt.");
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
            db.close();
        }
        return componentMap;
    }

    Map<String, Integer> getActionLoggedBytes(String date) {
        HashMap<String, Integer> actionMap = new HashMap<String, Integer>();

        String sql = "SELECT " + LOGGED_COL_COMPONENT + ", " + LOGGED_COL_ACTION +
                     ", " + LOGGED_COL_LOGGED +
                     " FROM " + TBL_LOGGED_BYTES +
                     " WHERE " + LOGGED_COL_DATE + "=?";

        String[] selectionArgs = { date };

        SQLiteDatabase db = null;
        try {
            db = getReadableDatabase();
        } catch (SQLiteException sqle) {
            Log.w(TAG, "Unable to get readable database.");
        }

        if (db != null) {
            Cursor cursor = null;
            try {
                cursor = db.rawQuery(sql, selectionArgs);
            } catch (SQLiteException sqle) {
                Log.w(TAG, "Unable to query data.");
            }

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        try {
                            String component = cursor.getString(0);
                            String action = cursor.getString(1);
                            int loggedBytes = cursor.getInt(2);
                            String key = PerformanceStatsDataProvider.makeKey(component, action);
                            actionMap.put(key, loggedBytes);
                        } catch (NumberFormatException nfe) {
                            Log.w(TAG, "Stored component logged bytes is corrupt.");
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
            db.close();
        }
        return actionMap;
    }

    int getGlobalLoggedBytes(String date) {
        int loggedBytes = 0;

        String sql = "SELECT SUM(" + LOGGED_COL_LOGGED + ") FROM " + TBL_LOGGED_BYTES +
                     " WHERE " + LOGGED_COL_DATE + "=?";

        String[] selectionArgs = { date };

        SQLiteDatabase db = null;
        try {
            db = getReadableDatabase();
        } catch (SQLiteException sqle) {
            Log.w(TAG, "Unable to get readable database.");
        }

        if (db != null) {
            Cursor cursor = null;
            try {
                cursor = db.rawQuery(sql, selectionArgs);
            } catch (SQLiteException sqle) {
                Log.w(TAG, "Unable to query data.");
            }

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    try {
                        loggedBytes = cursor.getInt(0);
                    } catch (NumberFormatException nfe) {
                        Log.w(TAG, "Stored logged bytes are corrupt.");
                    }
                }
                cursor.close();
            }
            db.close();
        }

        return loggedBytes;
    }

    /**
     * We want to be able to update the logged bytes only through the
     * dataProvider, so we can update the cache. Hence the reason for package visibility.
     * @param component Component name, cannot be null
     * @param action Action name, cannot be null
     * @param loggedBytes newly logged bytes
     * @return whether or not update was successful
     */
    boolean updateLoggedBytes(String component, String action, int loggedBytes) {
        boolean success = false;
        boolean sqlException = false;

        if (component != null && !component.isEmpty() && loggedBytes > 0 &&
                action != null && !action.isEmpty()) {
            SQLiteDatabase db = null;
            try {
                db = getWritableDatabase();
            } catch (SQLiteException sqle) {
                sqlException = true;
                Log.w(TAG, "Unable to get writable database.");
            }

            if (db != null) {
                /* check if the action row exits already */
                String date = getDate();
                String[] columns = new String[] { LOGGED_COL_LOGGED };
                String selection = LOGGED_COL_DATE + " = ? AND "
                                   + LOGGED_COL_COMPONENT + " = ? AND "
                                   + LOGGED_COL_ACTION + " = ?";
                String[] selectionArgs = new String[] { date, component, action };

                Cursor cursor = null;
                try {
                    cursor = db.query(TBL_LOGGED_BYTES, columns, selection, selectionArgs,
                                      null, null, null);
                } catch (SQLiteException sqle) {
                    sqlException = true;
                    Log.w(TAG, "Unable to query database.");
                }

                boolean update = false;
                if (cursor != null) {
                    update = cursor.moveToFirst();
                    cursor.close();
                }

                /* insert or update logged bytes table */
                if (!sqlException) {
                    if (update) {
                        if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "updateLoggedBytes() - update logged bytes.");
                        }
                        String sql = "UPDATE " + TBL_LOGGED_BYTES
                                     + " SET " + LOGGED_COL_LOGGED + " = "
                                     + LOGGED_COL_LOGGED + " + " + loggedBytes
                                     + " WHERE "
                                     + LOGGED_COL_DATE + "= " + date + " AND "
                                     + LOGGED_COL_COMPONENT + "= \"" + component + "\" AND "
                                     + LOGGED_COL_ACTION + "= \"" + action + "\"";
                        try {
                            db.execSQL(sql);
                        } catch (SQLiteException sqle) {
                            sqlException = true;
                            Log.w(TAG, "Unable to update logged bytes.");
                        }
                    } else {
                        ContentValues cv = new ContentValues();
                        cv.put(LOGGED_COL_DATE, date);
                        cv.put(LOGGED_COL_COMPONENT, component);
                        cv.put(LOGGED_COL_ACTION, action);
                        cv.put(LOGGED_COL_LOGGED, loggedBytes);
                        try {
                            db.insert(TBL_LOGGED_BYTES, null, cv);
                        } catch (SQLiteException sqle) {
                            sqlException = true;
                            Log.w(TAG, "Unable to insert new logged bytes.");
                        }
                    }

                    /* if there was an SQLiteException due to phone storage full or something,
                     * nothing much we can do about it other than gracefully exiting.
                     * This can potentially make us checkin more stuffs,
                     * but we can't save our logged bytes anyhow.
                     */
                    success = !sqlException;
                }
                db.close();
            } // (db != null)
        }
        return success;
    }

    public void updateActionStats(String component, String action, long executionTime,
                                  long threshold) {
        if (component != null && !component.isEmpty()
                && action != null && !action.isEmpty() && executionTime > 0) {
            SQLiteDatabase db = null;
            boolean sqlException = false;

            try {
                db = getWritableDatabase();
            } catch (SQLiteException sqle) {
                sqlException = true;
                Log.w(TAG, "Unable to get writable database.");
            }

            if (db != null) {
                boolean update = false;
                String date = getDate();
                /* check if action row if already exists */
                String[] columns = new String[] {
                    ACTION_COL_NUM_GOOD, ACTION_COL_NUM_BAD,
                    ACTION_COL_MIN_TIME, ACTION_COL_MAX_TIME, ACTION_COL_AVG_TIME
                };
                String selection = ACTION_COL_DATE + "=? AND "
                                   + ACTION_COL_COMPONENT + "=? AND "
                                   + ACTION_COL_ACTION + "=? AND "
                                   + ACTION_COL_THRESHOLD + "=?";
                String[] selectionArgs = new String[] { date, component, action,
                                                        String.valueOf(threshold)
                                                      };
                Cursor cursor = null;

                try {
                    cursor = db.query(TBL_ACTION_STATS, columns, selection, selectionArgs,
                                      null, null, null);
                } catch (SQLiteException sqle) {
                    sqlException = true;
                    Log.w(TAG, "Unable to query database.");
                }

                int numGood = 0;
                int numBad = 0;
                int numCount = 0;
                long minTime = 0;
                long maxTime = 0;
                long avgTime = 0;
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        update = true;
                        try {
                            numGood = cursor.getInt(0);
                            if (numGood < 0) {
                                numGood = 0;
                            }
                            numBad = cursor.getInt(1);
                            if (numBad < 0) {
                                numBad = 0;
                            }
                            minTime = cursor.getLong(2);
                            maxTime = cursor.getLong(3);
                            avgTime = cursor.getLong(4);
                            numCount = numGood + numBad;
                        } catch (SQLiteException sqle) {
                            sqlException = true;
                            Log.w(TAG, "Existing action stats is corrupt.");
                        }
                    }
                    cursor.close();
                }

                /* nothing much we can do for the day if there's a SQL exception */
                if (!sqlException) {
                    if (executionTime < threshold) {
                        numGood++;
                    } else {
                        numBad++;
                    }
                    if (minTime <= 0 || executionTime < minTime) {
                        minTime = executionTime;
                    }
                    if (maxTime <=0 || executionTime > maxTime) {
                        maxTime = executionTime;
                    }
                    avgTime = ((avgTime * numCount) + executionTime) / (numCount + 1);
                    /* insert or update action table */
                    if (update) {
                        if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "updateActionStats() - update");
                        }
                        String sql = "UPDATE " + TBL_ACTION_STATS + " SET "
                                     + ACTION_COL_NUM_GOOD + " = " + numGood + ", "
                                     + ACTION_COL_NUM_BAD + " = " + numBad + ", "
                                     + ACTION_COL_MIN_TIME + " = " + minTime + ", "
                                     + ACTION_COL_MAX_TIME + " = " + maxTime + ", "
                                     + ACTION_COL_AVG_TIME + " = " + avgTime
                                     + " WHERE " + ACTION_COL_DATE + "= " + date + " AND "
                                     + ACTION_COL_COMPONENT + " = \"" + component + "\" AND "
                                     + ACTION_COL_ACTION + "= \"" + action + "\" AND "
                                     + ACTION_COL_THRESHOLD + "= " + threshold;
                        try {
                            db.execSQL(sql);
                        } catch (SQLiteException sqle) {
                            sqlException = true;
                            Log.w(TAG, "Unable to update action stats.");
                        }
                    } else {
                        ContentValues cv = new ContentValues();
                        cv.put(ACTION_COL_DATE, date);
                        cv.put(ACTION_COL_COMPONENT, component);
                        cv.put(ACTION_COL_ACTION, action);
                        cv.put(ACTION_COL_THRESHOLD, threshold);
                        cv.put(ACTION_COL_NUM_GOOD, numGood);
                        cv.put(ACTION_COL_NUM_BAD, numBad);
                        cv.put(ACTION_COL_MIN_TIME, executionTime);
                        cv.put(ACTION_COL_MAX_TIME, executionTime);
                        cv.put(ACTION_COL_AVG_TIME, executionTime);
                        try {
                            db.insert(TBL_ACTION_STATS, null, cv);
                        } catch (SQLiteException sqle) {
                            sqlException = true;
                            Log.w(TAG, "Unable to insert a new action stats row.");
                        }
                    }
                }
                db.close();
            } // (db != null)
        }
    }

    /**
     * Removes entries from LoggedBytes table that are no longer needed
     * (basically rows that are not today's)
     */
    public void clearOldLoggedBytes() {
        SQLiteDatabase db = null;

        try {
            db = getWritableDatabase();
        } catch (SQLiteException sqle) {
            Log.w(TAG, "Unable to get writable database.");
        }

        if (db != null) {
            String where = LOGGED_COL_DATE + " < ?";
            String[] whereArgs = { getDate() };

            try {
                db.delete(TBL_LOGGED_BYTES, where, whereArgs);
            } catch (SQLiteException sqle) {
                Log.w(TAG, "Unable to clear logged bytes table.");
            }
            db.close();
        }
    }

    /**
     * Removes entries from action stats table that are no longer needed
     * (basically rows that are not today's)
     */
    public void clearOldActionStats() {
        SQLiteDatabase db = null;

        try {
            db = getWritableDatabase();
        } catch (SQLiteException sqle) {
            Log.w(TAG, "Unable to get writable database.");
        }

        if (db != null) {
            String where = ACTION_COL_DATE + " < ?";
            String[] whereArgs = { getDate() };

            try {
                db.delete(TBL_ACTION_STATS, where, whereArgs);
            } catch (SQLiteException sqle) {
                Log.w(TAG, "Unable to clear action stats table.");
            }
            db.close();
        }
    }

    /**
     * @return collection of DailyStats that contains
     * action stats data (date's other than today) for checking in.
     */
    public Collection<DailyStats> getOldActionStats() {
        ArrayList<DailyStats> statList = null;
        SQLiteDatabase db = null;
        try {
            db = getReadableDatabase();
        } catch (SQLiteException sqle) {
            Log.w(TAG, "Unable to get readable database.");
        }

        if (db != null) {
            String[] columns = { ACTION_COL_DATE, ACTION_COL_COMPONENT, ACTION_COL_ACTION,
                                 ACTION_COL_THRESHOLD, ACTION_COL_NUM_GOOD, ACTION_COL_NUM_BAD,
                                 ACTION_COL_MIN_TIME, ACTION_COL_MAX_TIME, ACTION_COL_AVG_TIME
                               };
            String selection = ACTION_COL_DATE + " < ?";
            String[] selectionArgs = { getDate() };
            String orderBy = ACTION_COL_DATE + " ASC, " +
                             ACTION_COL_COMPONENT + " ASC, " +
                             ACTION_COL_ACTION + " ASC, " +
                             ACTION_COL_THRESHOLD + " ASC";
            Cursor cursor = null;
            try {
                cursor = db.query(TBL_ACTION_STATS, columns, selection, selectionArgs,
                                  null, null, orderBy);
            } catch (SQLiteException sqle) {
                Log.w(TAG, "Unable to query action stats table.");
            }

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    statList = new ArrayList<DailyStats>(cursor.getCount());
                    do {
                        try {
                            String date = cursor.getString(0);
                            String component = cursor.getString(1);
                            String action = cursor.getString(2);
                            long threshold = cursor.getLong(3);
                            int numGood = cursor.getInt(4);
                            int numBad = cursor.getInt(5);
                            long minTime = cursor.getLong(6);
                            long maxTime = cursor.getLong(7);
                            long avgTime = cursor.getLong(8);
                            DailyStats stat = new DailyStats(date, component, action, threshold,
                                                             numGood, numBad, minTime, maxTime, avgTime);
                            statList.add(stat);
                        } catch (Exception e) {
                            Log.w(TAG, "Unable to parse action stats row.");
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
            db.close();
        }
        return statList;
    }
}
