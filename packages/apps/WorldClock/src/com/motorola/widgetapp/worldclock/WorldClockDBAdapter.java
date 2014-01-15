package com.motorola.widgetapp.worldclock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Locale;
import java.util.TimeZone;

import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

public class WorldClockDBAdapter {
    private static final String TAG = "WorldClockDBAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private final Context mCtx;

    private static final String TABLE_WORLDCLOCK_WIDGETS = "widgets";
    private static final String FIELD_APPWIDGET_ID = "appWidgetId";
    private static final String FIELD_WIDGET_DATA = "data";

    private static final String DATABASE_CREATE_WIDGETS_TABLE = "CREATE TABLE "
            + TABLE_WORLDCLOCK_WIDGETS + " (" + "_id" + " INTEGER PRIMARY KEY,"
            + FIELD_APPWIDGET_ID + " INTEGER," + FIELD_WIDGET_DATA + " BLOB"
            + ");";
    private static final String DATABASE_CREATE_TIMEZONES = "CREATE TABLE timezones (_id INTEGER PRIMARY KEY AUTOINCREMENT,timezone_id TEXT NOT NULL,"
            + "timezone_string_id INTEGER,display_name TEXT NOT NULL,raw_offset INTEGER);";
    private static final String DATABASE_CREATE_TIMEZONES_INDEX = "CREATE INDEX timezonesIndex ON timezones (timezone_id);";
    private static final String DATABASE_CREATE_CURRENTLOCALE = "CREATE TABLE currentlocale (_id INTEGER PRIMARY KEY AUTOINCREMENT,locale TEXT UNIQUE ON CONFLICT REPLACE);";

    private static final String DATABASE_DROP_TIMEZONES = "DROP TABLE IF EXISTS timezones";
    private static final String DATABASE_DROP_TIMEZONES_INDEX = "DROP INDEX IF EXISTS timezonesIndex";

    private static final String DATABASE_NAME = "cityclock.db";
    private static final String DATABASE_TABLE_TIMEZONES = "timezones";
    private static final String DATABASE_TABLE_CURRENTLOCALE = "currentlocale";
    private static final int DATABASE_VERSION = 6;

    public static final String KEY_ROWID = "_id";
    public static final String KEY_RAW_OFFSET = "raw_offset";
    public static final String KEY_DISPLAY_NAME = "display_name";
    public static final String KEY_TIMEZONE_ID = "timezone_id";
    public static final String KEY_TIMEZONE_STRING_ID = "timezone_string_id";

    // timezones key
    // KEY_ROWID, KEY_TIMEZONE_ID, TIMEZONES_INDEX_TIMEZONE_STRING_ID, KEY_DISPLAY_NAME, KEY_RAW_OFFSET same as
    // uiconfig
    // timezones index
    public static final int TIMEZONES_INDEX_TIMEZONE_ID = 1;
    public static final int TIMEZONES_INDEX_TIMEZONE_STRING_ID = 2;
    public static final int TIMEZONES_INDEX_DISPLAY_NAME = 3;
    public static final int TIMEZONES_INDEX_RAW_OFFSET = 4;

    // currentlocale key
    public static final String KEY_LOCALE = "locale";
    // currentlocale index
    public static final int CURRENTLOCALE_INDEX_LOCALE = 1;

    // Xml
    private static final String XMLTAG_TIMEZONE = "timezone";

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private final Context mCtx;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mCtx = context;
        }

        public void onCreate(SQLiteDatabase db) {
            Log.w(TAG, "DatabaseHelper's onCreate()");

            db.execSQL(DATABASE_CREATE_TIMEZONES);
            db.execSQL(DATABASE_CREATE_TIMEZONES_INDEX);
            db.execSQL(DATABASE_CREATE_CURRENTLOCALE);
            db.execSQL(DATABASE_CREATE_WIDGETS_TABLE);

            getTimezonesDataFromXml(db);
            getDefaultCurrentLocale(db);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + "!");
            db.execSQL(DATABASE_DROP_TIMEZONES);
            db.execSQL(DATABASE_DROP_TIMEZONES_INDEX);

            db.execSQL(DATABASE_CREATE_TIMEZONES);
            db.execSQL(DATABASE_CREATE_TIMEZONES_INDEX);

            getTimezonesDataFromXml(db);

            if (oldVersion == 4) {
                upgradeToVersion5(db);
                oldVersion += 1;
            }

            if (oldVersion == 5) {
                updateDisplayNameWithTimeZoneId(db);
                oldVersion += 1;
            }
        }

        public void UpdateTimezonesTable(SQLiteDatabase db) {
            Cursor mCursor = db.query(DATABASE_TABLE_TIMEZONES, new String[] {
                    KEY_ROWID, KEY_TIMEZONE_ID, KEY_TIMEZONE_STRING_ID, KEY_DISPLAY_NAME,
                    KEY_RAW_OFFSET }, null, null, null, null, null);
            if (mCursor != null) {
                if (mCursor.moveToFirst()) {
                    db.beginTransaction();
                    try {
                        int timeZoneStringIDIndex = mCursor.getColumnIndex(KEY_TIMEZONE_STRING_ID);
                        int displayNameIndex = mCursor.getColumnIndex(KEY_DISPLAY_NAME);
                        do {
                            int timeZoneStringID = mCursor.getInt(timeZoneStringIDIndex);
                            String displayName = mCursor.getString(displayNameIndex);
                            if (timeZoneStringID != 0) {
                                String newDisplayName = mCtx.getResources().getString(
                                        timeZoneStringID);

                                ContentValues values = new ContentValues();
                                values.put(KEY_DISPLAY_NAME, newDisplayName);
                                db.update(DATABASE_TABLE_TIMEZONES, values, KEY_TIMEZONE_STRING_ID
                                        + "=" + timeZoneStringID, null);
                            }
                        } while (mCursor.moveToNext());
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                }
                mCursor.close();
            }
        }

        private int getTimeZoneStringIdFromCityName(SQLiteDatabase db, String cityName) {
            if (cityName.length() == 0) {
                return 0;
            }

            int matchTimeZoneStringID = 0;
            String cityNameWithoutCountry = cityName.split("/")[0];

            Cursor mCursor = db.query(true, DATABASE_TABLE_TIMEZONES, new String[] { KEY_ROWID,
                    KEY_TIMEZONE_ID, KEY_TIMEZONE_STRING_ID, KEY_DISPLAY_NAME }
                    , KEY_DISPLAY_NAME + " LIKE " + "'%" + cityNameWithoutCountry + "%'",
                    null, null, null, null, null);
            if (mCursor == null) {
                return 0;
            }
            mCursor.moveToFirst();

            int count = mCursor.getCount();
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    int displayNameIndex = mCursor.getColumnIndex(KEY_DISPLAY_NAME);
                    String queriedCityName = mCursor.getString(displayNameIndex);
                    if (cityName.equalsIgnoreCase(queriedCityName)) {
                        int timeZoneStringIdIndex = mCursor.getColumnIndex(KEY_TIMEZONE_STRING_ID);
                        matchTimeZoneStringID = mCursor.getInt(timeZoneStringIdIndex);
                        break;
                    }
                    mCursor.moveToNext();
                }

            }
            mCursor.close();
            return matchTimeZoneStringID;
        }

        private void upgradeToVersion5(SQLiteDatabase db) {
            WidgetDataHolder widgetData = null;
            Cursor mCursor = db.query(TABLE_WORLDCLOCK_WIDGETS, new String[] { FIELD_APPWIDGET_ID,
                    FIELD_WIDGET_DATA }, null, null, null, null, null);
            if (mCursor != null) {
                if (mCursor.moveToFirst()) {
                    int appWidgetIdIndex = mCursor.getColumnIndex(FIELD_APPWIDGET_ID);
                    int widgetDataIndex = mCursor.getColumnIndex(FIELD_WIDGET_DATA);

                    do {
                        try {
                            byte[] data = mCursor.getBlob(widgetDataIndex);
                            if (data != null) {
                                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                                ObjectInputStream ois = new ObjectInputStream(bais);
                                widgetData = (WidgetDataHolder) ois.readObject();
                            }

                            Log.d(TAG, "upgradeToVersion5, appWidgetIdIndex:" + mCursor.getInt(appWidgetIdIndex));
                            if (widgetData != null) {
                                if (widgetData.mTimeZoneStringId == null) {
                                    // Rebuild mTimeZoneStringId and update table if mTimeZoneStringId not found
                                    Log.d(TAG, "upgradeToVersion5, mTimeZoneStringId not found");
                                    widgetData.mTimeZoneStringId = new int[WorldClockPreferenceActivity.MAX_CITY_NUM];
                                    for (int i = 0; i < WorldClockPreferenceActivity.MAX_CITY_NUM; i++) {
                                        if (widgetData.mDisplayName != null) {
                                            widgetData.mTimeZoneStringId[i] =
                                                    getTimeZoneStringIdFromCityName(db, widgetData.mDisplayName[i]);
                                        }
                                    }

                                    ContentValues values = new ContentValues();
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                                    oos.writeObject(widgetData);
                                    //data = baos.toByteArray();
                                    values.put(FIELD_WIDGET_DATA, baos.toByteArray());

                                    db.update(TABLE_WORLDCLOCK_WIDGETS, values, FIELD_APPWIDGET_ID
                                            + "=" + mCursor.getInt(appWidgetIdIndex), null);
                                    //mCursor.updateBlob(widgetDataIndex, data);
                                }
                            }
                        } catch (IOException ioe) {
                            Log.e(TAG, "Restoreing widgetData encountered problem", ioe);
                        } catch (ClassNotFoundException nnfe) {
                            Log.e(TAG, "Restoreing widgetData encountered problem", nnfe);
                        }
                    } while (mCursor.moveToNext());
                }
                //mCursor.commitUpdates();
                mCursor.close();
            }
        }

        private void updateDisplayNameWithTimeZoneId(SQLiteDatabase db) {
            WidgetDataHolder widgetData = null;
            Cursor mCursor = db.query(TABLE_WORLDCLOCK_WIDGETS, new String[] { FIELD_APPWIDGET_ID,
                    FIELD_WIDGET_DATA }, null, null, null, null, null);
            if (mCursor != null) {
                if (mCursor.moveToFirst()) {
                    int appWidgetIdIndex = mCursor.getColumnIndex(FIELD_APPWIDGET_ID);
                    int widgetDataIndex = mCursor.getColumnIndex(FIELD_WIDGET_DATA);

                    do {
                        try {
                            byte[] data = mCursor.getBlob(widgetDataIndex);
                            if (data != null) {
                                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                                ObjectInputStream ois = new ObjectInputStream(bais);
                                widgetData = (WidgetDataHolder) ois.readObject();
                            }

                            Log.d(TAG, "updateDisplayNameWithTimeZoneId, appWidgetIdIndex:" + mCursor.getInt(appWidgetIdIndex));
                            if (widgetData != null) {
                                if (widgetData.mTimeZoneStringId != null) {
                                    for (int i = 0; i < WorldClockPreferenceActivity.MAX_CITY_NUM; i++) {
                                        Log.d(TAG, "updateDisplayNameWithTimeZoneId, mTimeZoneId[" + i + "]:" + widgetData.mTimeZoneId[i]
                                                + ", mTimeZoneStringId[" + i +"]:" + widgetData.mTimeZoneStringId[i]);
                                        if (widgetData.mTimeZoneStringId[i] != 0) {
                                            // Update mTimeZoneStringId and mDisplayName if mTimeZoneStringId not 0
                                            Cursor cursor = db.query(true, DATABASE_TABLE_TIMEZONES, new String[] { KEY_ROWID,
                                                    KEY_TIMEZONE_ID, KEY_TIMEZONE_STRING_ID, KEY_DISPLAY_NAME }
                                                    , KEY_TIMEZONE_ID + "='" + widgetData.mTimeZoneId[i] + "'",
                                                    null, null, null, null, null);
                                            if (cursor != null) {
                                                cursor.moveToFirst();

                                                int count = cursor.getCount();
                                                if (count > 0) {
                                                    int displayNameIndex = cursor.getColumnIndex(KEY_DISPLAY_NAME);
                                                    int timeZoneStringIdIndex = cursor.getColumnIndex(KEY_TIMEZONE_STRING_ID);
                                                    String matchCityName = cursor.getString(displayNameIndex);
                                                    int matchTimeZoneStringId = cursor.getInt(timeZoneStringIdIndex);

                                                    if (count > 1) {
                                                        // Query display name if multi entries matches the timezone id
                                                        for (int j = 0; j < count; j++) {
                                                            String queriedCityName = cursor.getString(displayNameIndex);
                                                            if (widgetData.mDisplayName[i].equalsIgnoreCase(queriedCityName)) {
                                                                matchCityName = cursor.getString(displayNameIndex);
                                                                matchTimeZoneStringId = cursor.getInt(timeZoneStringIdIndex);
                                                                break;
                                                            }
                                                            cursor.moveToNext();
                                                        }
                                                    }
                                                    Log.d(TAG, "updateDisplayNameWithTimeZoneId, update display name to:"
                                                            + matchCityName + ", TimeZoneStringId to:" + matchTimeZoneStringId);
                                                    widgetData.mDisplayName[i] = matchCityName;
                                                    widgetData.mTimeZoneStringId[i] = matchTimeZoneStringId;
                                                } else if (count == 0) {
                                                    // Not found matched timezone id, will update display name to empty
                                                    Log.d(TAG, "updateDisplayNameWithTimeZoneId, update display name to empty");
                                                    widgetData.mTimeZoneStringId[i] = 0;
                                                    widgetData.mAdjustDSTStatus[i] = false;
                                                    widgetData.mDisplayName[i] = "";
                                                }
                                                cursor.close();
                                            }
                                        }
                                    }

                                    ContentValues values = new ContentValues();
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                                    oos.writeObject(widgetData);
                                    values.put(FIELD_WIDGET_DATA, baos.toByteArray());

                                    db.update(TABLE_WORLDCLOCK_WIDGETS, values, FIELD_APPWIDGET_ID
                                            + "=" + mCursor.getInt(appWidgetIdIndex), null);
                                }
                            }
                        } catch (IOException ioe) {
                            Log.e(TAG, "Restoreing widgetData encountered problem", ioe);
                        } catch (ClassNotFoundException nnfe) {
                            Log.e(TAG, "Restoreing widgetData encountered problem", nnfe);
                        }
                    } while (mCursor.moveToNext());
                }
                mCursor.close();
            }
        }

        private void getTimezonesDataFromXml(SQLiteDatabase db) {
            try {
                XmlResourceParser xrp = mCtx.getResources().getXml(
                        R.xml.widget_worldclock_timezones);
                while (xrp.next() != XmlResourceParser.START_TAG)
                    ;
                xrp.next();
                while (xrp.getEventType() != XmlResourceParser.END_TAG) {
                    while (xrp.getEventType() != XmlResourceParser.START_TAG) {
                        if (xrp.getEventType() == XmlResourceParser.END_DOCUMENT) {
                            return;
                        }
                        xrp.next();
                    }
                    if (xrp.getName().equals(XMLTAG_TIMEZONE)) {
                        String id = xrp.getAttributeValue(0);// timezone ID
                        String displayNameResourceString = xrp.nextText(); // Display
                        // City
                        // Sting
                        // Id
                        /* Get the display name accoring to the string ID */
                        int stringId = mCtx.getResources().getIdentifier(
                                displayNameResourceString, "string",
                                mCtx.getPackageName());

                        if (stringId != 0) {
                            String displayName = mCtx.getResources().getString(
                                    stringId);
                            int rawOffset = TimeZone.getTimeZone(id)
                                    .getRawOffset();
                            ContentValues initialValues = new ContentValues();
                            initialValues.put(KEY_TIMEZONE_ID, id);
                            initialValues.put(KEY_TIMEZONE_STRING_ID, stringId);
                            initialValues.put(KEY_DISPLAY_NAME, displayName);
                            initialValues.put(KEY_RAW_OFFSET, rawOffset);

                            db.insert(DATABASE_TABLE_TIMEZONES, null,
                                    initialValues);
                        }
                    }
                    while (xrp.getEventType() != XmlResourceParser.END_TAG) {
                        xrp.next();
                    }
                    xrp.next();
                }
                xrp.close();
            } catch (XmlPullParserException xppe) {
                Log.e(TAG, "Ill-formatted timezones.xml file");
            } catch (java.io.IOException ioe) {
                Log.e(TAG, "Unable to read timezones.xml file");
            }
        }

        private void getDefaultCurrentLocale(SQLiteDatabase db) {
            ContentValues initialValues = new ContentValues();
            initialValues.put(KEY_LOCALE, Locale.getDefault().toString());
            db.insert(DATABASE_TABLE_CURRENTLOCALE, null, initialValues);
        }

    }

    public WorldClockDBAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    public WorldClockDBAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }

    public long insertTimezones(String timezone_id, String display_name,
            int raw_offset) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TIMEZONE_ID, timezone_id);
        initialValues.put(KEY_DISPLAY_NAME, display_name);
        initialValues.put(KEY_RAW_OFFSET, raw_offset);
        return mDb.insert(DATABASE_TABLE_TIMEZONES, null, initialValues);
    }

    public Cursor queryTimezones() {
        Cursor mCursor = mDb.query(DATABASE_TABLE_TIMEZONES, new String[] {
                KEY_ROWID, KEY_TIMEZONE_ID, KEY_DISPLAY_NAME, KEY_RAW_OFFSET },
                null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public Cursor queryCurrentlocale() {
        Cursor mCursor = mDb.query(DATABASE_TABLE_CURRENTLOCALE, new String[] {
                KEY_ROWID, KEY_LOCALE }, null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /*
     * public Cursor queryTimezones(long rowId) throws SQLException { Cursor
     * mCursor = mDb.query(true, DATABASE_TABLE_TIMEZONES, new String[] {
     * KEY_ROWID, KEY_TIMEZONE_ID, KEY_DISPLAY_NAME, KEY_RAW_OFFSET }, KEY_ROWID
     * + "=" + rowId, null, null, null, null, null); if (mCursor != null) {
     * mCursor.moveToFirst(); } return mCursor; }
     */

    public Cursor queryTimezones(int rawOffset) throws SQLException {
        Cursor mCursor = mDb.query(true, DATABASE_TABLE_TIMEZONES,
                new String[] { KEY_ROWID, KEY_TIMEZONE_ID, KEY_DISPLAY_NAME,
                        KEY_RAW_OFFSET }, KEY_RAW_OFFSET + "=" + rawOffset,
                null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public static String formatStringForSQL(String name) {
        if (!TextUtils.isEmpty(name)) {
            name = name.replaceAll("'", "''").replaceAll("%", "%%");
        }
        return name;
    }

    public Cursor queryCities(String input) throws SQLException {
        input = formatStringForSQL(input);
        Cursor mCursor = mDb.query(true, DATABASE_TABLE_TIMEZONES,
                new String[] { KEY_ROWID, KEY_TIMEZONE_ID, KEY_TIMEZONE_STRING_ID, KEY_DISPLAY_NAME,
                        KEY_RAW_OFFSET }, KEY_DISPLAY_NAME + " LIKE " + "'%"
                        + input + "%'", null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public boolean updateCurrentlocale(long rowId, String locale) {
        ContentValues args = new ContentValues();
        args.put(KEY_LOCALE, locale);
        return mDb.update(DATABASE_TABLE_CURRENTLOCALE, args, KEY_ROWID + "="
                + rowId, null) > 0;
    }

    public long insertWidgetData(int appWidgetId, WidgetDataHolder data) {
        ContentValues values = new ContentValues();
        values.put(FIELD_APPWIDGET_ID, appWidgetId);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(data);
            values.put(FIELD_WIDGET_DATA, baos.toByteArray());
        } catch (IOException ioe) {
            Log.e(TAG, "Saving data error");
        }

        return mDb.insert(TABLE_WORLDCLOCK_WIDGETS, null, values);
    }

    public int updateWidgetData(int appWidgetId, WidgetDataHolder data) {
        ContentValues values = new ContentValues();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(data);
            values.put(FIELD_WIDGET_DATA, baos.toByteArray());
        } catch (IOException ioe) {
            Log.e(TAG, "Saving data error");
        }

        return mDb.update(TABLE_WORLDCLOCK_WIDGETS, values, FIELD_APPWIDGET_ID
                + "=" + appWidgetId, null);
    }

    public void updateCityName() {
        mDbHelper.UpdateTimezonesTable(mDb);
    }

    public int[] queryAllWidgetId() {
        Cursor c = mDb.query(TABLE_WORLDCLOCK_WIDGETS,
                new String[] { FIELD_APPWIDGET_ID }, null, null, null, null,
                null);
        if (c == null)
            return null;

        int count = c.getCount();
        if (count == 0) {
            c.close();
            return null;
        }

        int widgetDataIndex = c.getColumnIndex(FIELD_APPWIDGET_ID);
        int appWidgetIds[] = new int[count];
        for (int i = 0; i < count; i++) {
            c.moveToNext();
            appWidgetIds[i] = c.getInt(widgetDataIndex);
        }

        c.close();
        return appWidgetIds;
    }

    public WidgetDataHolder queryWidgetData(int appWidgetId) {

        Serializable widgetData = null;

        Cursor c = mDb.query(TABLE_WORLDCLOCK_WIDGETS,
                new String[] { FIELD_WIDGET_DATA }, FIELD_APPWIDGET_ID + "="
                        + appWidgetId, null, null, null, null);
        /* new String[] { FIELD_APPWIDGET_ID, FIELD_WIDGET_DATA } */
        try {
            if (c == null || c.getCount() != 1)
                return null;

            int widgetDataIndex = c.getColumnIndex(FIELD_WIDGET_DATA);

            c.moveToNext();
            byte[] data = c.getBlob(widgetDataIndex);
            if (data != null) {
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais);
                widgetData = (Serializable) ois.readObject();
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Restoreing widgetData encountered problem", ioe);
        } catch (ClassNotFoundException nnfe) {
            Log.e(TAG, "Restoreing widgetData encountered problem", nnfe);
        } finally {
            if (c != null) {
                c.close();
            }
        }

        return (WidgetDataHolder) widgetData;
    }

    public int deleteWidgetData(int appWidgetId) {
        return mDb.delete(TABLE_WORLDCLOCK_WIDGETS, FIELD_APPWIDGET_ID + "="
                + appWidgetId, null);
    }

    public void clearWorldClockTable() {
        try {
            mDb.delete(TABLE_WORLDCLOCK_WIDGETS, null, null);
        } catch (SQLiteException e) {
            Log.e(TAG, "Could not delete slideshow folder path from database",
                    e);
        }
    }

    public boolean insertOrUpdateWidgetData(int appWidgetId,
            WidgetDataHolder data) {
        Cursor c = mDb.query(TABLE_WORLDCLOCK_WIDGETS,
                new String[] { FIELD_APPWIDGET_ID }, FIELD_APPWIDGET_ID + "="
                        + appWidgetId, null, null, null, null);
        if (c == null)
            return false;

        int count = c.getCount();
        c.close();

        if (count == 0) {
            insertWidgetData(appWidgetId, data);
        } else if (count == 1) {
            updateWidgetData(appWidgetId, data);
        } else {
            return false;
        }

        return true;
    }

    static class WidgetDataHolder implements Serializable {

        private static final long serialVersionUID = 2L;
        public long _id;
        public boolean[] mAdjustDSTStatus;
        public String[] mDisplayName;
        public String[] mTimeZoneId;
        public int[] mTimeZoneStringId;
        public int mCurrentDisplayCity;
        public String mCurrentLocale;
        public boolean mClockStyle;
        public boolean mUseCurrentLocation;
        public boolean[] misCityHidden;
    }
}
