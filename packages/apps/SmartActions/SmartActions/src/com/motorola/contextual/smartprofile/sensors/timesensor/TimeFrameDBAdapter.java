/*
 * @(#)TimeFrameDBAdapter.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a15776       2010/02/21   NA               Incorporated review comments
 * a15776       2010/12/01   NA               Initial Version
 *
 */
package com.motorola.contextual.smartprofile.sensors.timesensor;

import java.security.SecureRandom;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.motorola.contextual.smartrules.db.DbSyntax;

/**
 * This class abstracts the queries to the Time frame database
 * <code><pre>
 * CLASS:
 *     Any class that needs to query the time frame database should get an instance of this class
 *     and do queries on that instance
 *     Implements {@link TimeFrameConstants}
 *
 * RESPONSIBILITIES:
 *     Creates the database
 *     Opens the database for read operations
 *     Opens the database for write operations
 *     Upgrading the database
 *     Dropping and recreating the database
 *     High level methods for querying the time frame database
 *
 * COLLABORATORS:
 *    {@link TimeFrameEditActivity} - Creates/Edits new time frames and update time frame database
 *    {@link TimeFramesCheckListActivity} - Deletes time frames and update time frame database
 *    {@link TimeFrameService} - Reads the time frame details from time frame database and register
 *                               Pending Intents with the Alarm Manager
 *
 * USAGE:
 * See each method
 *
 * </pre></code>
 */
public class TimeFrameDBAdapter implements TimeFrameConstants, DbSyntax, TimeFrameXmlSyntax {
    private static final String TAG = TimeFrameDBAdapter.class.getSimpleName();
    /** _id is not used. But this field is required for SimpleCursorAdapter to work */
    private static final String _ID = TimeFrameTableColumns.ID;
    /** Time frame name given by the user and is visible to the world */
    private static final String TIME_FRAME_NAME = TimeFrameTableColumns.NAME;
    /** Internally generated time frame name. This wont be visible to the world */
    private static final String TIME_FRAME_NAME_INTERNAL = TimeFrameTableColumns.NAME_INT;
    /** start time of the time frame. This is of form hh:mm in 24 hour format */
    private static final String TIME_START = TimeFrameTableColumns.START;
    /** end time of the time frame. This is of form hh:mm in 24 hour format */
    private static final String TIME_END = TimeFrameTableColumns.END;
    /** flag to differentiate a time frame with name and a time frame without name. Time frames
     * without names will have the flag set "i", and wont be displayed in the list
     */
    private static final String TIME_VISIBILITY = TimeFrameTableColumns.VISIBLE;
    /** Flag to tell whether this time frame is an all time frame */
    private static final String TIME_ALL_DAY_FLAG = TimeFrameTableColumns.ALL_DAY;
    /** Days of the week, on which this time frame is to be repeated */
    private static final String TIME_DAYS_OF_WEEK = TimeFrameTableColumns.DAYS_OF_WEEK;

    private static final String TIME_REGISTERED = TimeFrameTableColumns.REGISTERED;

    private static final String TIME_ACTIVE = TimeFrameTableColumns.ACTIVE;

    /** Base Constant for the preset time frame unique identifier */
    private static final int   TIME_FRAME_CONSTANT_BASE  = 0x100;
    /**
     * Projection String used in the database queries
     */
    private static final String[] COLUMNS = {
        _ID,
        TIME_FRAME_NAME,
        TIME_FRAME_NAME_INTERNAL,
        TIME_START,
        TIME_END,
        TIME_VISIBILITY,
        TIME_ALL_DAY_FLAG,
        TIME_DAYS_OF_WEEK,
        TIME_REGISTERED,
        TIME_ACTIVE
    };

    /** Time frame Database Name */
    private static final String DATABASE_NAME       = "time_frames.db";
    /** Time frame table name */
    private static final String TIMEFRAMES_TABLE    = "timeframes";
    /** version */
    private static final int    DATABASE_VERSION    = 5;

    //table creation statement
    private static final String CREATE_TABLE_SQL =
        CREATE_TABLE + SPACE + TIMEFRAMES_TABLE + SPACE + LP
        + ID + SPACE + INTEGER_TYPE + COMMA + SPACE
        + TIME_FRAME_NAME + SPACE + TEXT_TYPE + PRIMARY_KEY + COMMA + SPACE
        + TIME_FRAME_NAME_INTERNAL + SPACE + TEXT_TYPE + COMMA + SPACE
        + TIME_START + SPACE + TEXT_TYPE + COMMA + SPACE
        + TIME_END + SPACE + TEXT_TYPE + COMMA + SPACE
        + TIME_VISIBILITY + SPACE + TEXT_TYPE + COMMA + SPACE
        + TIME_ALL_DAY_FLAG + SPACE + TEXT_TYPE + COMMA + SPACE
        + TIME_DAYS_OF_WEEK + SPACE + INTEGER_TYPE + COMMA + SPACE
        + TIME_REGISTERED + SPACE + INTEGER_TYPE + COMMA + SPACE
        + TIME_ACTIVE + SPACE + INTEGER_TYPE + RP + SEMI_COLON;

    /**
     * Statement for dropping the table
     */
    private static final String DROP_TABLE_SQL = DROP_IF_TABLE_EXISTS
            + TIMEFRAMES_TABLE;

    /** Name for the day change time frame */
    private static final String DAYCHANGE_TIMEFRAME    = "tf_day_change";

    /** Handle to the Database helper */
    private static DatabaseHelper sDbHelper;

    /**
     * Initializes the DB Helper
     *
     * @param context
     */
    private static synchronized void initializeDbHelper(Context context) {
        if (sDbHelper == null) {
            sDbHelper = new DatabaseHelper(context);
        }
    }
    /**
     * Constructor that takes in the Context
     *
     * @param ctx - Application context
     */
    public TimeFrameDBAdapter(Context context) {
        initializeDbHelper(context);
    }

    /**
     * This class extends the SQLiteOpenHelper and provide methods to create and upgrade database
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        private Context mContext;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if (LOG_DEBUG) Log.d(TAG, "DB Create statememt :" + CREATE_TABLE_SQL);
            db.execSQL(CREATE_TABLE_SQL);
            insertPreloadedTimeModes(db, mContext);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion,
                                int newVersion) {
            if (LOG_INFO) {
                Log.i(TAG, "onDowngrade invoked with oldVersion " + oldVersion
                      + " newVersion " + newVersion);
            }
            if (newVersion < oldVersion) {
                try {
                    db.execSQL(DROP_TABLE_SQL);
                    onCreate(db);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (LOG_INFO) Log.i(TAG, "Upgrading database from version " + oldVersion
                                    + " to " + newVersion);

            boolean status = true;
            for (int version = oldVersion + 1; version <= newVersion && status; version++) {
                status = upgradeTo(db, version);
            }
        }

        /**
         * Call this function to upgrade the db to version 'version'
         * only when you are sure that db is on version 'version-1'
         * @param db      - db to work on
         * @param version - new version
         */
        private boolean upgradeTo(SQLiteDatabase db, int version) {
            if(LOG_INFO) Log.i(TAG, "Upgrading to " + version);
            boolean status = false;

            switch (version) {
            case 2:
                status = addColumn(db, TIMEFRAMES_TABLE, TIME_ALL_DAY_FLAG, TEXT_TYPE);
                break;
            case 3:
                status = addColumn(db, TIMEFRAMES_TABLE, TIME_DAYS_OF_WEEK, INTEGER_TYPE);
                break;
            case 4:
                status = addColumn(db, TIMEFRAMES_TABLE, TIME_REGISTERED, INTEGER_TYPE + NOT_NULL_CONSTRAINT
                                   + DEFAULT + "(" + TIMEFRAME_REGISTERED + ")");
                break;
            case 5:
                status = addColumn(db, TIMEFRAMES_TABLE, TIME_ACTIVE, INTEGER_TYPE + NOT_NULL_CONSTRAINT
                                   + DEFAULT + OPEN_B  + TIMEFRAME_INACTIVE + CLOSE_B);
                break;
            default:
                Log.e(TAG, "Incorrect version.  Database not upgraded to " + version);
            }

            return status;
        }

        /**
         * Adds a single column into the table
         * @param db                - db to act on
         * @param dbTable           - Table in db to work on
         * @param columnName        - column name to add
         * @param columnDefinition  - new column's definition
         */
        private boolean addColumn(SQLiteDatabase db, String dbTable, String columnName,
                                  String columnDefinition) {

            boolean status = true;

            try {
                db.beginTransaction();
                db.execSQL(ALTER_TABLE + dbTable + ADD_COLUMN + columnName + SPACE + columnDefinition + SEMI_COLON);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Log.e(TAG, "Exception in TimeFrameDBAdapter::addColumn");
                status = false;
                e.printStackTrace();
            } finally {
                db.endTransaction();
            }

            return status;
        }
    }


    /**
     *
     */
    public void close() {
        if (LOG_DEBUG) Log.d(TAG, "close");
        // Does nothing, since we want to reuse the db instance
        // retained for backward compatibility
    }

    /**
     * Utility method to log the cursor
     *
     * @param cursor - Cursor to log
     */
    private void logCursor(Cursor cursor) {
        if (cursor == null) {
            Log.e(TAG, "Null Cursor");
            return;
        }
        Log.i(TAG, "Number of records returned by query : " + cursor.getCount());
        DatabaseUtils.dumpCursor(cursor);
        return;
    }

    /**
     * Wrapper for the data base query
     *
     * @param whereClause - where clause to be used in the query
     * @param args        - arguments to whereClause
     *
     * @return - result Cursor
     */
    private Cursor getTimeFrames(String whereClause, String[] args) {
        Cursor cursor = null;
        try {
            cursor = sDbHelper.getReadableDatabase().query(TIMEFRAMES_TABLE, COLUMNS,
                     whereClause, args, null, null, null);
            if (LOG_DEBUG) logCursor(cursor);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cursor;
    }

    /*

    /**
     * Utility method to create pre-loaded time frames.
     *
     * @param db      - Handle to the SQLiteDatabase
     * @param context - Application Context
     *
     * NOTE
     * This will be changed, once CXD makes a decision on the pre loaded time frames. This method
     * wont be required, if there wont be any pre loaded time frames
     *
     */
    private static void insertPreloadedTimeModes(SQLiteDatabase db, Context context) {

        ContentValues initialValues1 = new ContentValues();

        initialValues1.put(TIME_FRAME_NAME,MORNING_TIMEFRAME);
        initialValues1.put(TIME_START,"5:00");
        initialValues1.put(TIME_END,"8:00");
        initialValues1.put(TIME_FRAME_NAME_INTERNAL, TIME_FRAME_CONSTANT_BASE + 1 + PERIOD + MORNING_TIMEFRAME);
        initialValues1.put(TIME_VISIBILITY, FLAG_VISIBLE);
        initialValues1.put(TIME_ALL_DAY_FLAG, ALL_DAY_FLAG_FALSE);
        initialValues1.put(TIME_DAYS_OF_WEEK, ALLDAY);
        initialValues1.put(TIME_REGISTERED, TIMEFRAME_UNREGISTERED);
        initialValues1.put(TIME_ACTIVE, TIMEFRAME_INACTIVE);

        ContentValues initialValues2 = new ContentValues();
        initialValues2.put(TIME_FRAME_NAME,EVENING_TIMEFRAME);
        initialValues2.put(TIME_START,"18:00");
        initialValues2.put(TIME_END,"22:00");
        initialValues2.put(TIME_FRAME_NAME_INTERNAL, TIME_FRAME_CONSTANT_BASE + 2 + PERIOD + EVENING_TIMEFRAME);
        initialValues2.put(TIME_VISIBILITY, FLAG_VISIBLE);
        initialValues2.put(TIME_ALL_DAY_FLAG, ALL_DAY_FLAG_FALSE);
        initialValues2.put(TIME_DAYS_OF_WEEK, ALLDAY);
        initialValues2.put(TIME_REGISTERED, TIMEFRAME_UNREGISTERED);
        initialValues2.put(TIME_ACTIVE, TIMEFRAME_INACTIVE);

        ContentValues initialValues3 = new ContentValues();
        initialValues3.put(TIME_FRAME_NAME,WEEKEND_TIMEFRAME);
        initialValues3.put(TIME_START,"10:00");
        initialValues3.put(TIME_END,"19:00");
        initialValues3.put(TIME_FRAME_NAME_INTERNAL, TIME_FRAME_CONSTANT_BASE + 3 + PERIOD + WEEKEND_TIMEFRAME);
        initialValues3.put(TIME_VISIBILITY, FLAG_VISIBLE);
        initialValues3.put(TIME_ALL_DAY_FLAG, ALL_DAY_FLAG_FALSE);
        initialValues3.put(TIME_DAYS_OF_WEEK, SATURDAY|SUNDAY);
        initialValues3.put(TIME_REGISTERED, TIMEFRAME_UNREGISTERED);
        initialValues3.put(TIME_ACTIVE, TIMEFRAME_INACTIVE);

        //Get a random integer between 0 and 59
        // End time will be set to a number between 04:00 and 04:59
        SecureRandom randomGenerator = new SecureRandom();
        randomGenerator.setSeed(System.currentTimeMillis());
        String endTime = "04:" + Integer.toString(randomGenerator.nextInt(60));

        ContentValues initialValues4 = new ContentValues();
        initialValues4.put(TIME_FRAME_NAME,NIGHT_TIMEFRAME);
        initialValues4.put(TIME_START,"22:00");
        initialValues4.put(TIME_END,endTime);
        initialValues4.put(TIME_FRAME_NAME_INTERNAL, TIME_FRAME_CONSTANT_BASE + 4 + PERIOD + NIGHT_TIMEFRAME);
        initialValues4.put(TIME_VISIBILITY, FLAG_VISIBLE);
        initialValues4.put(TIME_ALL_DAY_FLAG, ALL_DAY_FLAG_FALSE);
        initialValues4.put(TIME_DAYS_OF_WEEK, ALLDAY);
        initialValues4.put(TIME_REGISTERED, TIMEFRAME_UNREGISTERED);
        initialValues4.put(TIME_ACTIVE, TIMEFRAME_INACTIVE);

        ContentValues initialValues5 = new ContentValues();
        initialValues5.put(TIME_FRAME_NAME,WORK_TIMEFRAME);
        initialValues5.put(TIME_START,"8:00");
        initialValues5.put(TIME_END,"18:00");
        initialValues5.put(TIME_FRAME_NAME_INTERNAL, TIME_FRAME_CONSTANT_BASE + 5 + PERIOD + WORK_TIMEFRAME);
        initialValues5.put(TIME_VISIBILITY, FLAG_VISIBLE);
        initialValues5.put(TIME_ALL_DAY_FLAG, ALL_DAY_FLAG_FALSE);
        initialValues5.put(TIME_DAYS_OF_WEEK, MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY);
        initialValues5.put(TIME_REGISTERED, TIMEFRAME_UNREGISTERED);
        initialValues5.put(TIME_ACTIVE, TIMEFRAME_INACTIVE);

        /*
         * This time frame is designed to count the number of days
         * if the phone is active at least once between midnight and noon.
         * virtual sensor to count days will increment once everyday at the start
         * of this time frame.
         */
        String name = DAYCHANGE_TIMEFRAME;
        ContentValues initialValues6 = new ContentValues();
        initialValues6.put(TIME_FRAME_NAME,name);
        initialValues6.put(TIME_START,"00:00");
        initialValues6.put(TIME_END,"12:00");
        initialValues6.put(TIME_FRAME_NAME_INTERNAL, name);
        initialValues6.put(TIME_VISIBILITY, FLAG_INVISIBLE);
        initialValues6.put(TIME_ALL_DAY_FLAG, ALL_DAY_FLAG_FALSE);
        initialValues6.put(TIME_DAYS_OF_WEEK, ALLDAY);
        initialValues6.put(TIME_REGISTERED, TIMEFRAME_UNREGISTERED);
        initialValues6.put(TIME_ACTIVE, TIMEFRAME_INACTIVE);

        try {
            db.insert(TIMEFRAMES_TABLE, null, initialValues1);
            db.insert(TIMEFRAMES_TABLE, null, initialValues2);
            db.insert(TIMEFRAMES_TABLE, null, initialValues3);
            db.insert(TIMEFRAMES_TABLE, null, initialValues4);
            db.insert(TIMEFRAMES_TABLE, null, initialValues5);
            db.insert(TIMEFRAMES_TABLE, null, initialValues6);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // nothing special to be done
        }
        return;
    }

    /**
     * Inserts a time frame record in the table
     *
     * @param tfTuple - Information of the time frame table row to be inserted
     */
    public void insertRow(TimeFrameTuple tfTuple) {
        try {
            ContentValues contentValues = tfTuple.toContentValues();
            contentValues.put(TIME_REGISTERED, TIMEFRAME_UNREGISTERED);
            contentValues.put(TIME_ACTIVE, TIMEFRAME_INACTIVE);
            sDbHelper.getWritableDatabase().insert(TIMEFRAMES_TABLE, null, contentValues);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Updates a time frame record in the table
     *
     * @param oldName - Name of the time frame to be updated
     * @param tfTuple - Information of the table row to be inserted
     */
    public void updateRow(String oldName, TimeFrameTuple tfTuple) {

        if (LOG_INFO) Log.i(TAG,"Updating record " + oldName);
        try {
            ContentValues initialValues = tfTuple.toContentValues();
            String whereClause = TIME_FRAME_NAME + EQUALS + ANY;
            sDbHelper.getWritableDatabase().update(TIMEFRAMES_TABLE,initialValues,whereClause, new String[] {oldName});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Queries the record from the table, with the given internal name
     * and provides the corresponding friendly name
     *
     * @param name - Internal name of the time frame to be queried
     * @return String - Friendly name for the corresponding internal name
     */
    public String getFriendlyNameForInternalName(String name) {
        if(LOG_INFO) Log.i(TAG,"Fn:getFriendlyNameForInternalName. Name is "+name);

        //Null check added for fixing IKSTABLE6-1137
        if (name == null)
            return null;

        Cursor cursor = null;
        String friendlyName = null;
        String whereClause = TIME_FRAME_NAME_INTERNAL + EQUALS + ANY;

        try {
            cursor = sDbHelper.getReadableDatabase().query(TIMEFRAMES_TABLE, new String[] {TIME_FRAME_NAME},
                     whereClause, new String[] {name}, null, null, null);

            if ((cursor != null) && (cursor.getCount() > 0)) {
                if(LOG_INFO) Log.i(TAG,"Got some entries");

                // Entry already exists in Timeframe DB
                // so just go ahead get the friendly name
                cursor.moveToFirst();
                friendlyName = cursor.getString(cursor.
                                                getColumnIndexOrThrow(TimeFrameTableColumns.NAME));

                Log.i(TAG,"friendlyName : "+friendlyName);

            } else {
                Log.w(TAG,"InternalName :"+ name +"does not exist in Timeframe DB");
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return friendlyName;
    }

    /**
     * Queries the record from the table, with the given name
     *
     * @param name - Name of the time frame to be queried
     * @return Cursor
     */
    public Cursor getTimeframe(String name) {
        //String whereClause = TIME_FRAME_NAME + EQUALS + SINGLE_Q + '?' + SINGLE_Q;
        String whereClause = TIME_FRAME_NAME + EQUALS + ANY;
        return getTimeFrames(whereClause, new String[] {name});
    }


    /**
     * Queries the record from the table, with the given name
     *
     * @param name - Name of the time frame to be queried
     * @return Cursor
     */
    public Cursor getTimeframeFromInternalName(String intName) {
        String whereClause = TIME_FRAME_NAME_INTERNAL + EQUALS + ANY;
        return getTimeFrames(whereClause, new String[] {intName});
    }

    /**
     * Queries the record from the table, with the given name
     * or internal name
     * @param name - Name of the time frame to be queried
     * @param intName - Internal name of the time frame to be queried
     * @return Cursor
     */
    public Cursor checkTimeFrame(String name, String intName) {
        String whereClause = TIME_FRAME_NAME + EQUALS + ANY + OR +
                             TIME_FRAME_NAME_INTERNAL + EQUALS + ANY;
        return getTimeFrames(whereClause, new String[] {name, intName});
    }

    /**
     * Method to query and return the all the visible time frames
     * @return - Cursor that has all the visible time frame entries
     */
    public Cursor getVisibleTimeframes() {
        String whereClause = TIME_VISIBILITY + EQUALS + Q + FLAG_VISIBLE + Q ;
        return getTimeFrames(whereClause,null);
    }

    /**
     * Method to query and return the all time frames
     *
     * @return - Cursor that has all the time frame entries
     */
    public Cursor getAllTimeframes() {
        return getTimeFrames(null,null);
    }

    /**
     * Deletes the record from the table, with the given name
     * @param name - Name of the time frame to be deleted
     */
    public void deleteTimeframe(String name) {
        String whereClause = TIME_FRAME_NAME + EQUALS + ANY;
        try {
            int count = sDbHelper.getWritableDatabase().delete(TIMEFRAMES_TABLE, whereClause,
                    new String[] {name});
            if (LOG_INFO) Log.i(TAG, count + " rows deleted with name as " + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setTimeFrameAsRegistered(String internalName) {
        String whereClause = TIME_FRAME_NAME_INTERNAL + EQUALS + ANY;
        if (LOG_INFO) {
            Log.i(TAG, " setTimeFrameAsRegistered updating record as registered"
                  + internalName);
        }
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(TIME_REGISTERED, TIMEFRAME_REGISTERED);
            sDbHelper.getWritableDatabase().update(TIMEFRAMES_TABLE, contentValues, whereClause,
                                                   new String[] { internalName });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setTimeFrameAsUnregistered(String internalName) {
        String whereClause = TIME_FRAME_NAME_INTERNAL + EQUALS + ANY;
        if (LOG_INFO) {
            Log.i(TAG,
                  " setTimeFrameAsUnregistered updating record as unregistered"
                  + internalName);
        }
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(TIME_REGISTERED, TIMEFRAME_UNREGISTERED);
            sDbHelper.getWritableDatabase().update(TIMEFRAMES_TABLE, contentValues, whereClause,
                                                   new String[] { internalName });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setTimeFrameAsActive(String internalName) {
        String whereClause = TIME_FRAME_NAME_INTERNAL + EQUALS + ANY;
        if (LOG_INFO) {
            Log.i(TAG, " setTimeFrameAsActive updating record as active"
                  + internalName);
        }
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(TIME_ACTIVE, TIMEFRAME_ACTIVE);
            sDbHelper.getWritableDatabase().update(TIMEFRAMES_TABLE, contentValues, whereClause,
                                                   new String[] { internalName });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setTimeFrameAsInactive(String internalName) {
        String whereClause = TIME_FRAME_NAME_INTERNAL + EQUALS + ANY;
        if (LOG_INFO) {
            Log.i(TAG,
                  " setTimeFrameAsInactive updating record as inactive"
                  + internalName);
        }
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(TIME_ACTIVE, TIMEFRAME_INACTIVE);
            sDbHelper.getWritableDatabase().update(TIMEFRAMES_TABLE, contentValues, whereClause,
                                                   new String[] { internalName });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
