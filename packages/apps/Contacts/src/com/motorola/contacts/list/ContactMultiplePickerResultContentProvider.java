
package com.motorola.contacts.list;

import java.util.HashMap;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class ContactMultiplePickerResultContentProvider extends ContentProvider {

    static final String TAG = "ContactMultiplePickerResultContentProvider";

    /**
     * The authority we use to get to our sample provider.
     */
    public static final String AUTHORITY = "com.motorola.contacts.list.ContactMultiplePickerResult";

    private static final String DATABASE_NAME = "contactpicker.db";

    private static final int DATABASE_VERSION = 1;

    // A projection map used to select columns from the database
    private final HashMap<String, String> mResultProjectionMap;
    // Uri matcher to decode incoming URIs.
    private final UriMatcher mUriMatcher;

    // The incoming URI matches the result table URI pattern
    private static final int RESULTS = 1;
    // The incoming URI matches the result table session ID URI pattern
    private static final int RESULTS_REQUESTER = 2;
    // The incoming URI matches the result table session ID URI pattern
    private static final int RESULTS_SESSION_ID = 3;

    // Handle to a new DatabaseHelper.
    private DatabaseHelper mOpenHelper;

    /**
     * Definition of the contract for the result table of our provider.
     */
    public static final class Resultable implements BaseColumns {

        // This class cannot be instantiated
        private Resultable() {
        }

        /**
         * The table name offered by this provider
         */
        public static final String TABLE_NAME = "results";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/results");

        /**
         * The content URI base for a single row of data. Callers must append a
         * numeric row id to this Uri to retrieve a row
         */
        public static final Uri CONTENT_ID_URI_BASE = Uri.parse("content://" + AUTHORITY
                + "/results/");

        /**
         * The MIME type of {@link #CONTENT_URI}.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/contact";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * row.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/contact";

        /**
         * Column name for the single column holding our requester class name.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String COLUMN_NAME_REQUESTER = "requester";

        /**
         * Column name for the single column holding our session id.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String COLUMN_NAME_SESSION_ID = "session_id";

        /**
         * Column name for the single column holding our data.
         * <P>
         * Type: TEXT
         * </P>
         */
        public static final String COLUMN_NAME_DATA = "data";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = COLUMN_NAME_SESSION_ID + " ASC";

        /**
         * Intent extras related to multiple selector
         */
        public static final String SELECTION_RESULT_ALL_SELECTED = "com.android.contacts.SelectionResultAllSelected";
        public static final String SELECTTION_RESULT_INCLUDED = "com.android.contacts.SelectionResultIncluded";
        public static final String SELECTED_CONTACTS= "com.android.contacts.SelectedContacts";
        public static final String SELECTION_RESULT_SESSION_ID = "com.android.contacts.SelectionResultSessionId";
        public static final String SELECTED_CONTACTS_INCLUDED = "com.android.contacts.SelectedContactsIncluded";
    }

    /**
     * This class helps open, create, and upgrade the database file.
     */
    static class DatabaseHelper extends SQLiteOpenHelper {


        DatabaseHelper(Context context) {

            // calls the super constructor, requesting the default cursor
            // factory.
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /**
         * Creates the underlying database with table name and column names
         * taken from the NotePad class.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + Resultable.TABLE_NAME + " (" + Resultable._ID
                    + " INTEGER PRIMARY KEY,"
                    + Resultable.COLUMN_NAME_REQUESTER + " TEXT,"
                    + Resultable.COLUMN_NAME_SESSION_ID + " INTEGER,"
                    + Resultable.COLUMN_NAME_DATA + " TEXT" + ");");
        }

        /**
         * provider must consider what happens when the
         * underlying data store is changed.
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            // Logs that the database is being upgraded
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");

            // Kills the table and existing data
            db.execSQL("DROP TABLE IF EXISTS result");

            // Recreates the database with a new version
            onCreate(db);
        }
    }

    /**
     * Global provider initialization.
     */
    public ContactMultiplePickerResultContentProvider() {
        // Create and initialize URI matcher.
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(AUTHORITY, Resultable.TABLE_NAME, RESULTS);
        mUriMatcher.addURI(AUTHORITY, Resultable.TABLE_NAME+"/"+Resultable.COLUMN_NAME_SESSION_ID+"/#", RESULTS_SESSION_ID);
        mUriMatcher.addURI(AUTHORITY, Resultable.TABLE_NAME+"/"+Resultable.COLUMN_NAME_REQUESTER+"/*", RESULTS_REQUESTER);

        // Create and initialize projection map for all columns.  This is
        // simply an identity mapping.
        mResultProjectionMap = new HashMap<String, String>();
        mResultProjectionMap.put(Resultable._ID, Resultable._ID);
        mResultProjectionMap.put(Resultable.COLUMN_NAME_REQUESTER, Resultable.COLUMN_NAME_REQUESTER);
        mResultProjectionMap.put(Resultable.COLUMN_NAME_SESSION_ID, Resultable.COLUMN_NAME_SESSION_ID);
        mResultProjectionMap.put(Resultable.COLUMN_NAME_DATA, Resultable.COLUMN_NAME_DATA);
    }

    @Override
    public boolean onCreate() {
        // Creates a new helper object. Note that the database itself isn't opened until
        // something tries to access it, and it's only created if it doesn't already exist.
        mOpenHelper = new DatabaseHelper(getContext());

        // Assumes that any failures will be reported by a thrown exception.
        return true;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // Constructs a new query builder and sets its table name
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(Resultable.TABLE_NAME);

        switch (mUriMatcher.match(uri)) {
            case RESULTS:
                // If the incoming URI is for main table.
                qb.setProjectionMap(mResultProjectionMap);
                break;

            case RESULTS_SESSION_ID:
                // The incoming URI is for a single row.
                qb.setProjectionMap(mResultProjectionMap);
                qb.appendWhere(Resultable.COLUMN_NAME_SESSION_ID + "=?");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs,
                        new String[] { uri.getLastPathSegment() });
                break;
            case RESULTS_REQUESTER:
                // The incoming URI is for a single row.
                qb.setProjectionMap(mResultProjectionMap);
                qb.appendWhere(Resultable.COLUMN_NAME_REQUESTER + "=?");
                selectionArgs = DatabaseUtils.appendSelectionArgs(selectionArgs,
                        new String[] { uri.getLastPathSegment() });
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }


        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = Resultable.DEFAULT_SORT_ORDER;
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        Cursor c = qb.query(db, projection, selection, selectionArgs,
                null /* no group */, null /* no filter */, sortOrder);

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case RESULTS:
            case RESULTS_REQUESTER:
            case RESULTS_SESSION_ID:
                return Resultable.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {

        if (mUriMatcher.match(uri) != RESULTS) {
            // Can only insert into to main URI.
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int numValues = values.length;

        db.beginTransaction();
        try {

            for (int i = 0; i < numValues; i++) {
                long rowId = db.insert(Resultable.TABLE_NAME, null, values[i]);
                if (rowId < 0) {
                    throw new SQLException("Failed to insert row into " + uri);
                }

            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        getContext().getContentResolver().notifyChange(Resultable.CONTENT_URI, null);

        return numValues;

    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (mUriMatcher.match(uri) != RESULTS) {
            // Can only insert into to main URI.
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;

        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        if (values.containsKey(Resultable.COLUMN_NAME_DATA) == false) {
            values.put(Resultable.COLUMN_NAME_DATA, "");
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        long rowId = db.insert(Resultable.TABLE_NAME, null, values);

        // If the insert succeeded, the row ID exists.
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(Resultable.CONTENT_ID_URI_BASE, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String finalWhere;

        int count;

        switch (mUriMatcher.match(uri)) {
            case RESULTS:
                // If URI is result table, delete uses incoming where clause and args.
                count = db.delete(Resultable.TABLE_NAME, selection, selectionArgs);
                break;

                // If the incoming URI matches a result session ID, does the delete based on the
                // incoming data, but modifies the where clause to restrict it to the
                // particular session ID.
            case RESULTS_SESSION_ID:
                finalWhere = DatabaseUtils.concatenateWhere(
                        Resultable.COLUMN_NAME_SESSION_ID + " = " + ContentUris.parseId(uri), selection);
                count = db.delete(Resultable.TABLE_NAME, finalWhere, selectionArgs);
                break;

            case RESULTS_REQUESTER:
                finalWhere = DatabaseUtils.concatenateWhere(
                        Resultable.COLUMN_NAME_REQUESTER + " = " + ContentUris.parseId(uri), selection);
                count = db.delete(Resultable.TABLE_NAME, finalWhere, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // I do not think we need this function
        return 0;
    }

}
