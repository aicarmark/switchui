/*
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2010/08/27 IKCTXTAW-19		   Initial version
 */
package com.motorola.contextual.virtualsensor.locationsensor;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.format.DateUtils;

import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabase;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabase.PoiTable;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabase.LocTimeTable.Tuple;


/**
 *<code><pre>
 * CLASS:
 *  implements content provider for location database
 *
 * RESPONSIBILITIES:
 *
 * COLABORATORS:
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */
public class LocationSensorProvider extends ContentProvider {
    private final static String TAG = "LSAPP_Prov";
    public static final String AUTHORITY = PACKAGE_NAME;
    public static final String JSON_CONTENT_DIRECTORY = "jsonvalue";
    public static final String AGGREGATION_CONTENT_DIRECTORY = "aggregate";
    public static final String POITAG_CONTENT_DIRECTORY = "poi";
    public static final String CONSOLIDATE_CONTENT_DIRECTORY = "consolidate";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    public static final Uri CELL_CONTENT_URI = Uri.withAppendedPath(CONTENT_URI, LocationDatabase.CellTable.TABLE_NAME);
    public static final Uri CELL_CONTENT_AGGREGATE_URI = Uri.withAppendedPath(CELL_CONTENT_URI, AGGREGATION_CONTENT_DIRECTORY);

    public static final Uri LOCTIME_CONTENT_URI = Uri.withAppendedPath(CONTENT_URI, LocationDatabase.LocTimeTable.TABLE_NAME);
    public static final Uri LOCTIME_CONTENT_AGGREGATE_URI = Uri.withAppendedPath(LOCTIME_CONTENT_URI, AGGREGATION_CONTENT_DIRECTORY);
    public static final Uri LOCTIME_CONTENT_POITAG_URI = Uri.withAppendedPath(LOCTIME_CONTENT_URI, POITAG_CONTENT_DIRECTORY);
    public static final Uri POI_CONTENT_URI = Uri.withAppendedPath(CONTENT_URI, LocationDatabase.PoiTable.TABLE_NAME);
    public static final Uri LOCTIME_CONTENT_CONSOLIDATE_URI = Uri.withAppendedPath(LOCTIME_CONTENT_URI, CONSOLIDATE_CONTENT_DIRECTORY); // consolidate Uri get all the locs around

    private static String[] LOCTIME_DB_COLUMNS = LocationDatabase.LocTimeTable.Columns.getNames();

    private interface LocationUri {
        int CELLTOWER 				= 101;
        int ID 						= 102;
        int NAME 					= 103;
        int JSON 					= 104;
        int TIMESTAMP 				= 105;
        int COUNT 					= 106;
        int AGGREGATION 			= 107;
        int LOCTIME 				= 201;
        int LOCTIME_AGGREGATION 	= 202;
        int LOCTIME_POITAG			= 203;
        int CELLSENSOR				= 301;
        int POI						= 401;
        int LOCTIME_AGGREGATION_COARSE	= 402;
        int LOCTIME_CONSOLIDATE		= 403;
    }

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private Context mContext;
    private volatile LocationDatabase mLocationDatabase = null;

    static {
        // Contacts URI matching table
        final UriMatcher matcher = sURIMatcher;

        matcher.addURI(AUTHORITY, LocationDatabase.CellTable.TABLE_NAME, LocationUri.CELLTOWER);
        matcher.addURI(AUTHORITY, LocationDatabase.CellTable.TABLE_NAME +"/#", LocationUri.ID);
        matcher.addURI(AUTHORITY, LocationDatabase.CellTable.TABLE_NAME +"/name", LocationUri.NAME);
        matcher.addURI(AUTHORITY, LocationDatabase.CellTable.TABLE_NAME +"/jsonvalue", LocationUri.JSON);  // align with JSON_CONTENT_DIRECTORY
        matcher.addURI(AUTHORITY, LocationDatabase.CellTable.TABLE_NAME +"/" + AGGREGATION_CONTENT_DIRECTORY, LocationUri.AGGREGATION);  // align with JSON_CONTENT_DIRECTORY

        matcher.addURI(AUTHORITY, LocationDatabase.LocTimeTable.TABLE_NAME, LocationUri.LOCTIME);
        matcher.addURI(AUTHORITY, LocationDatabase.LocTimeTable.TABLE_NAME +"/" + AGGREGATION_CONTENT_DIRECTORY, LocationUri.LOCTIME_AGGREGATION);  // align with JSON_CONTENT_DIRECTORY
        matcher.addURI(AUTHORITY, LocationDatabase.LocTimeTable.TABLE_NAME +"/" + POITAG_CONTENT_DIRECTORY, LocationUri.LOCTIME_POITAG);  // align with JSON_CONTENT_DIRECTORY
        matcher.addURI(AUTHORITY, LocationDatabase.LocTimeTable.TABLE_NAME +"/" + CONSOLIDATE_CONTENT_DIRECTORY, LocationUri.LOCTIME_CONSOLIDATE);  // consolidate Uri get all the locs around

        matcher.addURI(AUTHORITY, LocationDatabase.PoiTable.TABLE_NAME, LocationUri.POI);

    }

    @Override
    public boolean onCreate() {
        mContext = getContext();
        //mLocationDatabase = LocationDatabase.getInstance(mContext);
        return true;
    }

    /**
     * lazy initialization, for instant field, double check idom
     * @return the reference to database
     */
    private LocationDatabase getLocationDatabase() {
        LocationDatabase dbhandle = mLocationDatabase;
        if(dbhandle == null) {
            synchronized(this) {
                dbhandle = mLocationDatabase; // double check
                if(dbhandle == null) {
                    mLocationDatabase = dbhandle = LocationDatabase.getInstance(mContext);  // called once
                }
            }
        }
        return dbhandle;
    }

    private static final String DIR_PREFIX = 	"vnd.android.cursor.dir/vnd.locationsensor.";
    private static final String ITEM_PREFIX = 	"vnd.android.cursor.item/vnd.locationsensor.";

    @Override
    public String getType(Uri uri) {
        final int match = sURIMatcher.match(uri);
        LSAppLog.d(TAG, "getType : " + uri.toString() + "Macthes " + match);
        switch (match) {
        case LocationUri.CELLTOWER:
            return DIR_PREFIX+"celltower";		// "vnd.android.cursor.dir/vnd.locationsensor.celltower";
        case LocationUri.ID:
            return ITEM_PREFIX+"celltower"; 	// "vnd.android.cursor.item/vnd.locationsensor.celltower";
        case LocationUri.NAME:
            return ITEM_PREFIX+"name";   		// "vnd.android.cursor.item/vnd.locationsensor.name";
        case LocationUri.JSON:
            return ITEM_PREFIX+"jsonvalue";		// "vnd.android.cursor.item/vnd.locationsensor.jsonvalue";
        case LocationUri.TIMESTAMP:
            return ITEM_PREFIX+"time";			// "vnd.android.cursor.item/vnd.locationsensor.time";
        case LocationUri.COUNT:
            return ITEM_PREFIX+"count";			// vnd.android.cursor.item/vnd.locationsensor.count";
        case LocationUri.AGGREGATION:
            return DIR_PREFIX+"celltower";		// "vnd.android.cursor.dir/vnd.locationsensor.celltower";

        case LocationUri.LOCTIME:
            return DIR_PREFIX+"loctime";		// "vnd.android.cursor.dir/vnd.locationsensor.loctime";
        case LocationUri.LOCTIME_AGGREGATION :
            return DIR_PREFIX+"locagg";	//"vnd.android.cursor.dir/vnd.locationsensor.loctime";
        case LocationUri.LOCTIME_POITAG :
            return DIR_PREFIX+"locpoi";	//"vnd.android.cursor.dir/vnd.locationsensor.loctime";
        case LocationUri.LOCTIME_CONSOLIDATE :
            return DIR_PREFIX+"consolidate";	//"vnd.android.cursor.dir/vnd.locationsensor.loctime";

        case LocationUri.CELLSENSOR :
            return DIR_PREFIX+"cellsensor";	//"vnd.android.cursor.dir/vnd.locationsensor.cellsensor";
        case LocationUri.POI :
            return DIR_PREFIX+"poi";	//"vnd.android.cursor.dir/vnd.locationsensor.poi";
        }
        throw new UnsupportedOperationException("Unknown uri: " + uri);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String groupby = null;
        String rawsql = null;
        SQLiteDatabase db = null;
        Cursor c = null;

        int match = sURIMatcher.match(uri);
        LSAppLog.d(TAG, "query: " + uri.toString() + "Macthes " + match);
        switch (match) {
        case LocationUri.LOCTIME:
            qb.setTables(LocationDatabase.LocTimeTable.TABLE_NAME);
            //qb.appendWhere(LocationDatabase.LocTimeTable.Columns.LAT + "!= 0 AND " + LocationDatabase.LocTimeTable.Columns.LGT + "!= 0 ");
            LSAppLog.d(TAG, "query :: Matching LOCTIME :" + qb.toString());
            break;

        case LocationUri.LOCTIME_AGGREGATION_COARSE:
            qb.setTables(LocationDatabase.LocTimeTable.TABLE_NAME);
            qb.appendWhere(LocationDatabase.LocTimeTable.Columns.LAT + "!= 0 AND " + LocationDatabase.LocTimeTable.Columns.LGT + "!= 0 ");
            String sumcount = " sum(Count) AS FreqCount";
            String[] sumproj = new String[LOCTIME_DB_COLUMNS.length+1];
            System.arraycopy(LOCTIME_DB_COLUMNS, 0, sumproj, 0, LOCTIME_DB_COLUMNS.length);
            sumproj[sumproj.length-1] = sumcount;
            projection = sumproj;
            groupby = " ACCUNAME";
            sortOrder = "FreqCount Desc";
            LSAppLog.d(TAG, "query LOCTIME_AGGREGATION:");
            break;

        case LocationUri.LOCTIME_POITAG:
            qb.setTables(LocationDatabase.LocTimeTable.TABLE_NAME);
            LSAppLog.d(TAG, "query :: Matching LOCTIME_POITAG");
            break;

        case LocationUri.POI:
            qb.setTables(LocationDatabase.PoiTable.TABLE_NAME);
            LSAppLog.d(TAG, "query :: Matching PoiTable selection:" + selection);
            break;

        case LocationUri.LOCTIME_AGGREGATION:
            rawsql = "select *, sum(Count) as FreqCount from (select * from loctime where lat!=0 order by accuname, accuracy desc) as ordloctime group by accuname order by FreqCount Desc";
            db = getLocationDatabase().mDbAdapter.getDb();
            c = db.rawQuery(rawsql, null);
            LSAppLog.d(TAG, "query LOCTIME_AGGREGATION:" + rawsql);
            return c;  // return in the middle of switch case.

        case LocationUri.LOCTIME_CONSOLIDATE:   // this Uri is only for query all the locs that consolidable to the passing in
            LSAppLog.d(TAG, "query LOCTIME_CONSOLIDATE:" + selection);
            return getLocationsByWifiMatch(selection);

        default:
            throw new IllegalArgumentException("Unknown URL ::" + uri);
        }

        c = qb.query(getLocationDatabase().mDbAdapter.getDb(), projection, selection, selectionArgs, groupby, null, sortOrder, null);
        if (c != null) {
            c.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return c;
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        final SQLiteDatabase db = getLocationDatabase().mDbAdapter.getDb();
        final int matchedUriId = sURIMatcher.match(uri);
        switch (matchedUriId) {
        case LocationUri.ID:
            count = db.delete(LocationDatabase.CellTable.TABLE_NAME, selection, selectionArgs);
            getContext().getContentResolver().notifyChange(uri, null);
            break;
        case LocationUri.POI:
            count = db.delete(LocationDatabase.PoiTable.TABLE_NAME, selection, selectionArgs);
            if (count > 0) {
                getContext().getContentResolver().notifyChange(uri, null);
            }
            break;
        default:
            throw new UnsupportedOperationException("Cannot delete that URL: " + uri);
        }
        return count;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowId = 0;
        int match = sURIMatcher.match(uri);
        Uri insertedUri = null;
        LSAppLog.d(TAG, "insert: " + uri.toString() + "Macthes " + match);
        //DatabaseUtils.InsertHelper(db, TABLE_NAME).insert(values);
        switch (match) {
        case LocationUri.CELLTOWER:
            rowId = getLocationDatabase().mDbAdapter.getDb().insert(LocationDatabase.CellTable.TABLE_NAME, null, values);
            if (rowId > 0) {
                insertedUri = ContentUris.withAppendedId(uri, rowId);
                getContext().getContentResolver().notifyChange(insertedUri, null);
            }
            break;
        case LocationUri.LOCTIME:
            //rowId = mLocationDatabase.mDbAdapter.getDb().insert(LocationDatabase.LocTimeTable.TABLE_NAME, null, values);
            rowId = getLocationDatabase().mDbAdapter.insertWithOnConflict(LocationDatabase.LocTimeTable.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            if (rowId > 0) {
                insertedUri = ContentUris.withAppendedId(uri, rowId);
                getContext().getContentResolver().notifyChange(insertedUri, null);
            }
            break;

        case LocationUri.LOCTIME_POITAG:
            // indicate static configuration with accuracy -1
            values.put(LocationDatabase.LocTimeTable.Columns.ACCURACY, -1);
            //values.put(LocationDatabase.LocTimeTable.Columns.LAT, 0);
            //values.put(LocationDatabase.LocTimeTable.Columns.LGT, 0);
            values.put(LocationDatabase.LocTimeTable.Columns.STARTTIME, System.currentTimeMillis());  // insert using nowtime
            values.put(LocationDatabase.LocTimeTable.Columns.ENDTIME, System.currentTimeMillis());
            values.put(LocationDatabase.LocTimeTable.Columns.COUNT, 1);
            values.put(LocationDatabase.LocTimeTable.Columns.CELLJSONVALUE, "static configured POI tag");

            rowId = getLocationDatabase().mDbAdapter.getDb().insert(LocationDatabase.LocTimeTable.TABLE_NAME, null, values);
            if (rowId > 0) {
                insertedUri = ContentUris.withAppendedId(uri, rowId);
                getContext().getContentResolver().notifyChange(insertedUri, null);
            }
            LSAppLog.d(TAG, "insert POITAG :" + values.toString());
            break;

        case LocationUri.POI:
            if(values.getAsDouble(LocationDatabase.PoiTable.Columns.LAT) != null &&    // prevent NPE during autoboxing
                    values.getAsDouble(LocationDatabase.PoiTable.Columns.LGT) != null &&
                    values.getAsLong(LocationDatabase.PoiTable.Columns.RADIUS) != null &&
                    ! isPoiOverlapping(-1,
                                       values.getAsDouble(LocationDatabase.PoiTable.Columns.LAT),   // autoboxing
                                       values.getAsDouble(LocationDatabase.PoiTable.Columns.LGT),
                                       values.getAsLong(LocationDatabase.PoiTable.Columns.RADIUS))
              ) {
                rowId = getLocationDatabase().mDbAdapter.insertWithOnConflict(LocationDatabase.PoiTable.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                if (rowId > 0) {
                    insertedUri = ContentUris.withAppendedId(uri, rowId);
                    getContext().getContentResolver().notifyChange(insertedUri, null);
                }
            } else {
                LSAppLog.pd(TAG, "insert POI : bad data or overlapping "
                            + ":" + values.getAsDouble(LocationDatabase.PoiTable.Columns.LAT)
                            + ":" + values.getAsDouble(LocationDatabase.PoiTable.Columns.LGT)
                            + ":" + values.getAsDouble(LocationDatabase.PoiTable.Columns.RADIUS));
                insertedUri = ContentUris.withAppendedId(uri, -1);
                getContext().getContentResolver().notifyChange(insertedUri, null);
            }
            break;

        default:
            break;
        }

        LSAppLog.d(TAG, "inserted entry: " + insertedUri);
        return insertedUri;
    }


    @Override
    public int update(Uri url, ContentValues values, String selection, String[] selectionArgs) {
        String where;
        int count;
        final int matchedUriId = sURIMatcher.match(url);
        switch (matchedUriId) {
        case LocationUri.CELLTOWER:
            where = selection;
            count = getLocationDatabase().mDbAdapter.getDb().update(LocationDatabase.CellTable.TABLE_NAME, values, where, selectionArgs);
            break;

        case LocationUri.ID:
            where = ( LocationDatabase.CellTable.TABLE_NAME + "._ID  = " + ContentUris.parseId(url));
            count = getLocationDatabase().mDbAdapter.getDb().update(LocationDatabase.CellTable.TABLE_NAME, values, where, selectionArgs);
            break;

        case LocationUri.LOCTIME:
        case LocationUri.LOCTIME_POITAG:
            where = selection;
            //count = mLocationDatabase.mDbAdapter.getDb().update(LocationDatabase.LocTimeTable.TABLE_NAME, values, where, selectionArgs);
            count = getLocationDatabase().mDbAdapter.update(LocationDatabase.LocTimeTable.TABLE_NAME, values, where, selectionArgs);
            break;

        case LocationUri.POI:
            if(values.getAsLong(LocationDatabase.PoiTable.Columns._ID) != null &&
                    values.getAsDouble(LocationDatabase.PoiTable.Columns.LAT) != null &&    // prevent NPE during autoboxing
                    values.getAsDouble(LocationDatabase.PoiTable.Columns.LGT) != null &&
                    values.getAsLong(LocationDatabase.PoiTable.Columns.RADIUS) != null &&

                    ! isPoiOverlapping(values.getAsLong(LocationDatabase.PoiTable.Columns._ID),
                                       values.getAsDouble(LocationDatabase.PoiTable.Columns.LAT),   // autoboxing
                                       values.getAsDouble(LocationDatabase.PoiTable.Columns.LGT),
                                       values.getAsLong(LocationDatabase.PoiTable.Columns.RADIUS))
              ) {
                // need merge update celljsons here!
                where = selection;
                count = getLocationDatabase().mDbAdapter.update(LocationDatabase.PoiTable.TABLE_NAME, values, where, selectionArgs);
                LSAppLog.d(TAG, "update POI success: " + where);
            } else {
                LSAppLog.pd(TAG, "update POI failed: bad data or overlapping POIs");
                count = 0;
            }
            break;

        default:
            throw new UnsupportedOperationException("Cannot update URL: " + url);
        }

        getContext().getContentResolver().notifyChange(url, null);
        LSAppLog.d(TAG, "updated entry: " + url);
        return count;
    }

    /**
     * query the loctime table, using wifi fuzzy match, get all locations around the passed in loc with wifi fuzzy match.
     */
    private Cursor getLocationsByWifiMatch(String selection) {

        MatrixCursor mc = new MatrixCursor(LOCTIME_DB_COLUMNS);  // never empty

        String wifissid = getLocationWifiSsid(selection);
        Set<String> wifiset = null;
        LSAppLog.d(TAG, "getLocationsByWifiMatch: " + wifissid);
        if (wifissid == null) {
            return mc;  // return empty cursor, not null cursor.
        } else {
            Map<String, String> curlocWifiMap = new HashMap<String, String>(); // key=bssid
            JSONUtils.convertJSonArrayToMap(wifissid, curlocWifiMap, null, null);
            wifiset = curlocWifiMap.keySet();
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(LocationDatabase.LocTimeTable.TABLE_NAME);
        long recently = System.currentTimeMillis() - (30*DateUtils.DAY_IN_MILLIS);
        String where = "( " +  LocationDatabase.LocTimeTable.Columns.STARTTIME + " >= " + recently + " )";
        String orderby = "_ID DESC";    // lastest entry on top.
        Tuple t = null;

        Cursor c = qb.query(getLocationDatabase().mDbAdapter.getDb(), null, where, null, null, null, orderby, null);
        try {
            if (c != null && c.moveToFirst()) {
                do {
                    t = LocationDatabase.LocTimeTable.toTuple(c);
                    if (locationWifiMatch(wifiset, t)) {
                        MatrixCursor.RowBuilder nrow = mc.newRow();  // add a row to the end of cursor and return the builder
                        LocationDatabase.LocTimeTable.toMatrixCursorRow(t, nrow);  // build the row
                        LSAppLog.d(TAG, "getLocationsByWifiMatch  add one match record:" +  t.toString());
                    }
                } while (c.moveToNext());
            } else {
                LSAppLog.d(TAG, "findRecentLocations : Empty Loc talbe : " + where);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return mc;
    }

    /**
     * get the selected location(current location) wifi string
     * I can not move this function to other component. If I do that,
     * I will create dependency from standalone content provider to another upper layer components.
     */
    private String getLocationWifiSsid(String selection) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(LocationDatabase.LocTimeTable.TABLE_NAME);
        qb.appendWhere(selection);  // use the passed in selection criteria
        Cursor c = qb.query(getLocationDatabase().mDbAdapter.getDb(), null, selection, null, null, null, null, null);
        try {
            if (c != null && c.moveToFirst()) {
                Tuple t = LocationDatabase.LocTimeTable.toTuple(c);
                LSAppLog.d(TAG, "getLocationWifiSsid:" + t.toString());
                if (t != null) {
                    String wifissid = t.getWifiSsid();
                    return wifissid;
                }
            }
        } catch (Exception e) {
            LSAppLog.e(TAG, e.toString());
        } finally {
            if (c != null)
                c.close();
        }
        return null;
    }

    /**
     * why should fuzzy match logic inside content provider ? this is only for consolidate uri.
     * the location are close(consolidable) to current location based on wifi comparison.
     * @return true if the two locations are close. false otherwise
     */
    private boolean locationWifiMatch(Set<String> s, Tuple t) {
        boolean matches = false;
        int matchcount = Utils.intersectSetJsonArray(s, null, t.getWifiSsid());
        if ( matchcount >= FUZZY_MATCH_MIN) {
            matches = true;
            LSAppLog.d(TAG, "locationWifiMatch: matches count: " + matchcount);
        }
        return matches;
    }

    /**
     * check whether the passed in new POI is overlapping with any existing POI. IKSTABLE6-7455
     * @param id : if check insertion, set to -1, if check update, filter out location with passed id(current location)
     * @param lat, lng, accuracy, the geo co-ordinates of the new poi
     * @return true if there exists a poi overlapping, false otherwise
     */
    private boolean isPoiOverlapping(long id, double lat, double lng, long accuracy) {
        boolean ret = false;   // no overlapping by default
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(LocationDatabase.PoiTable.TABLE_NAME);
        Cursor c = qb.query(getLocationDatabase().mDbAdapter.getDb(), null, null, null, null, null, null);
        try {
            if (c != null && c.moveToFirst()) {
                do {
                    PoiTable.Tuple t = LocationDatabase.PoiTable.toTuple(c);
                    if(id >= 0 && t != null && t.get_id() == id)   // ignore current location during update
                        continue;

                    if (t != null && Utils.consolidatableLocations(t.getLat(), t.getLgt(), t.getRadius(), lat, lng, accuracy)) {
                        ret = true;   // consolidatable, poi overlapping, return !
                        LSAppLog.pd(TAG, "isPoiOverlapping: overlap with : " + t.toString());
                        break;
                    }
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            LSAppLog.e(TAG, e.toString());
        } finally {
            if (c != null)
                c.close();
        }
        LSAppLog.d(TAG, "isPoiOverlapping: is there any overlapping ? " + ret);
        return ret;
    }
}
