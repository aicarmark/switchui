/*
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2011/01/17  IKCTXTAW-126     Initial version
 */

package com.motorola.contextual.virtualsensor.locationsensor;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabase;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabaseHelper;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabase.LocTimeTable.Tuple;


/**
 *<code><pre>
 * CLASS:
 *  this class wraps all the unit test cases.
 *
 * RESPONSIBILITIES:
 *  for conducting unit test on the phone.
 *
 * COLABORATORS:
 *  LocationSensorApp, LocatinSensorManager, LocationStore, LocationDetection.
 *
 * USAGE:
 *  See each method.
 *
 *</pre></code>
 */

public class TestCases {
    private static final String TAG = "LSAPP_TEST";

    private final static String VERIFY_STMT = "select name, accuracy, bestaccuracy, accuname from loctime";
    private final static String INSERT_STMT = "insert into loctime(Lat, Lgt, Accuracy, BestAccuracy, Name, AccuName, StartTime, Count) ";

    private final static String A1 = INSERT_STMT + " values(42.001, -88.001, 40, 40, 'A1', 'A1', 1295984234000, 0)";
    private final static String A2 = INSERT_STMT + " values(42.001, -88.003, 70, 70, 'A2', 'A2', 1295984234000, 0)";
    private final static String A3 = INSERT_STMT + " values(42.001, -88.002, 1500, 1500, 'A3', 'A3', 1295984234000, 0)";
    private final static String A4 = INSERT_STMT + " values(42.001, -88.004, 150, 150, 'A4', 'A4', 1295984234000, 0)";
    private final static String A5 = INSERT_STMT + " values(42.001, -88.001, 30, 30, 'A5', 'A5', 1296160807744, 0)";

    protected LocationSensorApp     mLSApp;
    LocationDatabaseHelper mDbHelper;
    SQLiteDatabase mDb;


    @SuppressWarnings("unused")
    private TestCases() {} // hide default constructor

    public TestCases(LocationSensorApp lsapp) {
        mLSApp = lsapp;
        mDbHelper = LocationDatabaseHelper.getInstance(mLSApp.mContext);
        mDb = mDbHelper.getWritableDatabase();
    }

    /**
     * the main test, test all. the same as python unittest.main()
     */
    public void main() {
        //testDiscoveredLocation();
        testCheckinData();
        //testFindClosestOverlappingLocation(42.411159, -88.007088, 75);
        //testConsolidate();
        //testMockLocationConsolidate();
        //testIncrementalConsolidate();
        //testWifiMatch();
        //testContentProviderUri();
        //testConsolidateUri();
    }

    /**
     * return the data connection status.
     * @return
     */
    public String testDataConnection() {
        return " Network provider enabled="+mLSApp.getLSMan().mLocMon.isNetworkProviderEnabled()+
               " DataConn="+mLSApp.getLSMan().mTelMon.isDataConnectionGood();
    }

    /**
     * manually force a checkin.
     */
    public void testCheckinData() {
        LSAppLog.d(TAG, "current loc: " + mLSApp.getLSMan().mCurLocTuple.toString());
        mLSApp.getLSMan().mStore.checkinUponLeavingLocation(mLSApp.getLSMan().mCurLocTuple);
    }

    /**
     * test db overflow by continuously injection records..
     */
    public void testDBOverflow() {
        try {
            for (int i=0; i<1000; i++) {
                mLSApp.getLSMan().mStore.logServiceRestarted();
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            // do nothing
            LSAppLog.d(TAG, "testDBOverflow Exception: " + e.toString());
        }
    }

    /**
     * test find closest overlapping with given lat/lng/accuracy
     */
    public void testFindClosestOverlappingLocation(double lat, double lgt, long accuracy) {
        mLSApp.getLSMan().mStore.findClosestOverlappingLocation(lat, lgt, accuracy);
    }

    /**
     * test broadcast intent
     */
    public void testDiscoveredLocation() {
        String accuname = "232 Creekside Ct Lindenhurst, IL 60046 ";
        LSAppLog.d(TAG, "testDiscoveredLocation :" + accuname);
        mLSApp.getLSMan().broadcastDiscoveredLocation(0, 42.42339, -88.012941, 60, accuname, null, null);
    }

    /**
     * test consolidate algorithm
     */
    public void testConsolidate() {
        mLSApp.getLSMan().mStore.consolidateLocations(false);
    }

    /**
     * dump the cursor
     */
    private void dumpCursor() {
        try {
            Thread.sleep(10000);  // sleep for 10 second because I am in UI thread
        } catch (Exception e) {
            // swallow it, do nothing;
        }

        Cursor c = mLSApp.mContext.getContentResolver().query(LocationSensorProvider.LOCTIME_CONTENT_URI, new String[] { "name", "accuracy", "bestaccuracy", "accuname"}, null, null, null);
        if (c.moveToFirst()) {
            do {
                LSAppLog.d(TAG, c.getString(0) + ":" + c.getLong(1) + ":"+ c.getLong(2) + ":" + c.getString(3));
            } while (c.moveToNext());
        }
        c.close();
    }

    /**
     * test consolidation with mock locations
     */
    private void testMockLocationConsolidate() {
        LSAppLog.d(TAG, "-----------test case A 1 2 3 4 --------------");
        mDb.execSQL("delete from loctime");
        mDb.execSQL(A1);
        mDb.execSQL(A2);
        mDb.execSQL(A3);
        mDb.execSQL(A4);
        testConsolidate();
        dumpCursor();

        LSAppLog.d(TAG, "-----------test case A 3 2 1 4 --------------");
        mDb.execSQL("delete from loctime");
        mDb.execSQL(A3);
        mDb.execSQL(A2);
        mDb.execSQL(A1);
        mDb.execSQL(A4);
        testConsolidate();
        //dumpCursor(mDb.rawQuery(verification, null));
        dumpCursor();

        LSAppLog.d(TAG, "-----------test case A 4 1 3 2 --------------");
        mDb.execSQL("delete from loctime");
        mDb.execSQL(A4);
        mDb.execSQL(A1);
        mDb.execSQL(A3);
        mDb.execSQL(A2);
        testConsolidate();
        dumpCursor();

        testIncrementalConsolidate();
    }

    /**
     * test consolidation with mock locations
     */
    private void testIncrementalConsolidate() {
        mDb.execSQL(A5);   // A5 at the same spot as A1, so whatever consolidated to A1 should consolidate to A5.
        testConsolidate();
        dumpCursor();
    }

    /**
     * test wifi beacon match
     */
    private void testWifiMatch() {
        mLSApp.getLSMan().mDetection.mDetCtrl.matchBeacons();
    }

    /**
     * test content provider consolidate URI
     */
    private void testConsolidateUri() {
        long id = 36;
        String CONSOLIDATE_URI = "content://com.motorola.contextual.virtualsensor.locationsensor/loctime/consolidate";
        String where = "( _ID = " + id + " )";
        String order = " _ID DESC";
        Cursor c = mLSApp.mContext.getContentResolver().query(Uri.parse(CONSOLIDATE_URI), null, where, null, order);
        try {
            if (c != null && c.moveToFirst()) {
                do {
                    Tuple t = LocationDatabase.LocTimeTable.toTuple(c);
                    LSAppLog.d(TAG, "find consolidate entry :" + t.toString());
                } while (c.moveToNext());
            } else {
                LSAppLog.d(TAG, "find consolidate entry  : Empty Loc talbe : " + where);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c!=null) c.close();
        }
    }

    /**
     * test content provider cURI
     */
    private void testContentProviderUri() {
        long id = 100;
        String LOCTIME_URI = "content://com.motorola.contextual.virtualsensor.locationsensor/loctime";
        String where = "( _ID <= " + id + " )";
        String order = " _ID DESC";
        Cursor c = mLSApp.mContext.getContentResolver().query(Uri.parse(LOCTIME_URI), null, where, null, order);
        try {
            if (c != null && c.moveToFirst()) {
                do {
                    Tuple t = LocationDatabase.LocTimeTable.toTuple(c);
                    LSAppLog.d(TAG, "find loctime entry :" + t.getLat() + ":" + t.getAccuName() + ":" + t.toString());
                } while (c.moveToNext());
            } else {
                LSAppLog.d(TAG, "find loctime entry  : Empty Loc talbe : " + where);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c!=null) c.close();
        }
    }
}
