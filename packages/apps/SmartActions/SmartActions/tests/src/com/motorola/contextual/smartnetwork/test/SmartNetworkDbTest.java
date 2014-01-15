/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 * w04917 (Brian Lee)        2012/07/09   IKCTXTAW-487    Add NetworkSession
 */

package com.motorola.contextual.smartnetwork.test;

import java.util.Random;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import com.motorola.contextual.smartnetwork.db.DbWrapper;
import com.motorola.contextual.smartnetwork.db.SmartNetworkDbSchema;
import com.motorola.contextual.smartnetwork.db.SmartNetworkSqliteDb;
import com.motorola.contextual.smartnetwork.test.mockobjects.DbContext;

public class SmartNetworkDbTest extends AndroidTestCase implements SmartNetworkDbSchema {
    private DbContext mTestContext;

    // the db interface to test
    private DbWrapper mSmartNetworkDb;

    // the base sqlite db
    private SQLiteDatabase mBaseDb;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTestContext = new DbContext(getContext());
        SmartNetworkSqliteDb db = new SmartNetworkSqliteDb(mTestContext);
        mSmartNetworkDb = db;
        mBaseDb = db.getWritableDatabase();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mSmartNetworkDb.close();
        mSmartNetworkDb = null;
        mBaseDb.close();
        mBaseDb = null;

        // all db references must be released before deleting the db files
        mTestContext.cleanup();
        mTestContext = null;
    }

    @Override
    public void testAndroidTestCaseSetupProperly() {
        super.testAndroidTestCaseSetupProperly();
        assertNotNull(mTestContext);
        assertNotNull(mBaseDb);
        assertNotNull(mSmartNetworkDb);
    }

    public void testInsertTopLocation() {
        Random random = new Random();
        ContentValues values = TopLocationTableTest.makeTopLocationValues(random);
        // insert row
        long rowId = mSmartNetworkDb.insert(TBL_TOP_LOCATION, values);
        assertTrue(rowId > 0);

        // query via base db
        Cursor cursor = mBaseDb.query(TBL_TOP_LOCATION, null, null, null, null, null, null, null);
        TopLocationTableTest.assertValuesCursor(rowId, values, cursor);
        cursor.close();
    }

    public void testQueryTopLocation() {
        Random random = new Random();
        ContentValues values = TopLocationTableTest.makeTopLocationValues(random);

        // insert using base db
        long rowId = mBaseDb.insert(TBL_TOP_LOCATION, null, values);

        // query rows
        Cursor cursor = mSmartNetworkDb.query(TBL_TOP_LOCATION, null, null, null, null, null, null,
                                              null);
        TopLocationTableTest.assertValuesCursor(rowId, values, cursor);
    }

    public void testUpdateTopLocation() {
        Random random = new Random();
        ContentValues originalValues = TopLocationTableTest.makeTopLocationValues(random);

        // insert using base db
        long rowId = mBaseDb.insert(TBL_TOP_LOCATION, null, originalValues);

        // only update wifi ssid and last_updated
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(COL_WIFI_SSID, String.valueOf(random.nextGaussian()));
        updatedValues.put(COL_NETWORK_CONDITION_UPDATED, random.nextLong());

        mSmartNetworkDb.update(TBL_TOP_LOCATION, updatedValues, null, null);

        // query and verify using base db
        Cursor cursor = mBaseDb.query(TBL_TOP_LOCATION, null, null, null, null, null, null, null);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getCount());

        // original values
        assertEquals(rowId, cursor.getLong(cursor.getColumnIndex(COL_ID)));
        assertEquals(originalValues.getAsString(COL_POI),
                     cursor.getString(cursor.getColumnIndex(COL_POI)));
        assertEquals(originalValues.getAsString(COL_CELL_TOWERS),
                     cursor.getString(cursor.getColumnIndex(COL_CELL_TOWERS)));
        assertEquals(originalValues.getAsString(COL_NETWORK_CONDITION),
                     cursor.getString(cursor.getColumnIndex(COL_NETWORK_CONDITION)));
        assertEquals(originalValues.getAsString(COL_PREVIOUS_NETWORK_CONDITION),
                     cursor.getString(cursor.getColumnIndex(COL_PREVIOUS_NETWORK_CONDITION)));
        assertEquals(TestHelper.getLong(originalValues, COL_TIME_SPENT),
                     cursor.getLong(cursor.getColumnIndex(COL_TIME_SPENT)));
        assertEquals(TestHelper.getInt(originalValues, COL_RANK),
                     cursor.getInt(cursor.getColumnIndex(COL_RANK)));
        assertEquals(TestHelper.getLong(originalValues,COL_RANK_UPDATED),
                     cursor.getLong(cursor.getColumnIndex(COL_RANK_UPDATED)));

        // updated values
        assertEquals(updatedValues.getAsString(COL_WIFI_SSID),
                     cursor.getString(cursor.getColumnIndex(COL_WIFI_SSID)));
        assertEquals(TestHelper.getLong(updatedValues, COL_NETWORK_CONDITION_UPDATED),
                     cursor.getLong(cursor.getColumnIndex(COL_NETWORK_CONDITION_UPDATED)));
        cursor.close();
    }

    public void testDeleteTopLocation() {
        Random random = new Random();
        ContentValues values = TopLocationTableTest.makeTopLocationValues(random);

        // insert using base db
        long rowId = mBaseDb.insert(TBL_TOP_LOCATION, null, values);

        // delete
        int deleted = mSmartNetworkDb.delete(TBL_TOP_LOCATION, "1", null);
        assertEquals(1, deleted);

        // query and verify using base db
        Cursor cursor = mBaseDb.query(TBL_TOP_LOCATION, null, null, null, null, null, null, null);
        assertNotNull(cursor);
        assertFalse(cursor.moveToFirst());
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    public void testInsertDataConnectionState() {
        Random random = new Random();

        ContentValues values = DataConnectionStateTableTest.makeDataConnectionStateValues(random,
                               mBaseDb);
        final long rowId = mSmartNetworkDb.insert(TBL_DATA_CONNECTION_STATE, values);

        // query via base db and verify
        Cursor cursor = mBaseDb.query(TBL_DATA_CONNECTION_STATE, null, null, null, null, null, null, null);
        DataConnectionStateTableTest.assertValuesCursor(rowId, values, cursor);
        cursor.close();
    }

    public void testQueryDataConnectionState() {
        Random random = new Random();
        ContentValues values = DataConnectionStateTableTest.makeDataConnectionStateValues(random,
                               mBaseDb);

        // insert using base db
        long rowId = mBaseDb.insert(TBL_DATA_CONNECTION_STATE, null, values);

        // query rows
        Cursor cursor = mSmartNetworkDb.query(TBL_DATA_CONNECTION_STATE, null, null, null, null,
                                              null, null, null);
        DataConnectionStateTableTest.assertValuesCursor(rowId, values, cursor);
    }

    public void testUpdateDataConnectionState() {
        Random random = new Random();
        ContentValues values = DataConnectionStateTableTest.makeDataConnectionStateValues(random,
                               mBaseDb);

        // insert using base db
        long rowId = mBaseDb.insert(TBL_DATA_CONNECTION_STATE, null, values);

        // only update state and timestamp
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(COL_STATE, String.valueOf(random.nextGaussian()));
        updatedValues.put(COL_TIMESTAMP, random.nextLong());

        mSmartNetworkDb.update(TBL_DATA_CONNECTION_STATE, updatedValues, null, null);

        // query and verify using base db
        Cursor cursor = mBaseDb.query(TBL_DATA_CONNECTION_STATE, null, null, null, null, null,
                                      null, null);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getCount());

        // original values
        assertEquals(rowId, cursor.getLong(cursor.getColumnIndex(COL_ID)));
        assertEquals(TestHelper.getLong(values, COL_FK_MONITOR_SESSION),
                     cursor.getLong(cursor.getColumnIndex(COL_FK_MONITOR_SESSION)));
        assertEquals(values.getAsString(COL_NETWORK_TYPE),
                     cursor.getString(cursor.getColumnIndex(COL_NETWORK_TYPE)));
        // updated values
        assertEquals(updatedValues.getAsString(COL_STATE),
                     cursor.getString(cursor.getColumnIndex(COL_STATE)));
        assertEquals(TestHelper.getLong(updatedValues, COL_TIMESTAMP),
                     cursor.getLong(cursor.getColumnIndex(COL_TIMESTAMP)));
        cursor.close();
    }

    public void testDeleteDataConnectionState() {
        Random random = new Random();
        ContentValues values = DataConnectionStateTableTest.makeDataConnectionStateValues(random,
                               mBaseDb);

        // insert using base db
        long rowId = mBaseDb.insert(TBL_DATA_CONNECTION_STATE, null, values);

        // delete
        int deleted = mSmartNetworkDb.delete(TBL_DATA_CONNECTION_STATE, "1", null);
        assertEquals(1, deleted);

        // query and verify using base db
        Cursor cursor = mBaseDb.query(TBL_DATA_CONNECTION_STATE, null, null, null, null, null,
                                      null, null);
        assertNotNull(cursor);
        assertFalse(cursor.moveToFirst());
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    public void testInsertServiceState() {
        Random random = new Random();

        ContentValues values = ServiceStateTableTest.makeServiceStateValues(random, mBaseDb);
        final long rowId = mSmartNetworkDb.insert(TBL_SERVICE_STATE, values);

        // query via base db and verify
        Cursor cursor = mBaseDb.query(TBL_SERVICE_STATE, null, null, null, null, null, null, null);
        ServiceStateTableTest.assertValuesCursor(rowId, values, cursor);
        cursor.close();
    }

    public void testQueryServiceState() {
        Random random = new Random();
        ContentValues values = ServiceStateTableTest.makeServiceStateValues(random, mBaseDb);

        // insert using base db
        long rowId = mBaseDb.insert(TBL_SERVICE_STATE, null, values);

        // query rows
        Cursor cursor = mSmartNetworkDb.query(TBL_SERVICE_STATE, null, null, null, null,
                                              null, null, null);
        ServiceStateTableTest.assertValuesCursor(rowId, values, cursor);
    }

    public void testUpdateServiceState() {
        Random random = new Random();
        ContentValues values = ServiceStateTableTest.makeServiceStateValues(random, mBaseDb);

        // insert using base db
        long rowId = mBaseDb.insert(TBL_SERVICE_STATE, null, values);

        // only update state and timestamp
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(COL_STATE, String.valueOf(random.nextGaussian()));
        updatedValues.put(COL_TIMESTAMP, random.nextLong());

        mSmartNetworkDb.update(TBL_SERVICE_STATE, updatedValues, null, null);

        // query and verify using base db
        Cursor cursor = mBaseDb.query(TBL_SERVICE_STATE, null, null, null, null, null,
                                      null, null);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getCount());

        // original values
        assertEquals(rowId, cursor.getLong(cursor.getColumnIndex(COL_ID)));
        assertEquals(TestHelper.getLong(values, COL_FK_MONITOR_SESSION),
                     cursor.getLong(cursor.getColumnIndex(COL_FK_MONITOR_SESSION)));
        // updated values
        assertEquals(updatedValues.getAsString(COL_STATE),
                     cursor.getString(cursor.getColumnIndex(COL_STATE)));
        assertEquals(TestHelper.getLong(updatedValues, COL_TIMESTAMP),
                     cursor.getLong(cursor.getColumnIndex(COL_TIMESTAMP)));
        cursor.close();
    }

    public void testDeleteServiceState() {
        Random random = new Random();
        ContentValues values = ServiceStateTableTest.makeServiceStateValues(random, mBaseDb);

        // insert using base db
        long rowId = mBaseDb.insert(TBL_SERVICE_STATE, null, values);

        // delete
        int deleted = mSmartNetworkDb.delete(TBL_SERVICE_STATE, "1", null);
        assertEquals(1, deleted);

        // query and verify using base db
        Cursor cursor = mBaseDb.query(TBL_SERVICE_STATE, null, null, null, null, null,
                                      null, null);
        assertNotNull(cursor);
        assertFalse(cursor.moveToFirst());
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    public void testInsertSignalStrength() {
        Random random = new Random();

        ContentValues values = SignalStrengthTableTest.makeSignalStrengthValues(random, mBaseDb);
        final long rowId = mSmartNetworkDb.insert(TBL_SIGNAL_STRENGTH, values);

        // query via base db and verify
        Cursor cursor = mBaseDb.query(TBL_SIGNAL_STRENGTH, null, null, null, null, null, null, null);
        SignalStrengthTableTest.assertValuesCursor(rowId, values, cursor);
        cursor.close();
    }

    public void testQuerySignalStrength() {
        Random random = new Random();
        ContentValues values = SignalStrengthTableTest.makeSignalStrengthValues(random, mBaseDb);

        // insert using base db
        long rowId = mBaseDb.insert(TBL_SIGNAL_STRENGTH, null, values);

        // query rows
        Cursor cursor = mSmartNetworkDb.query(TBL_SIGNAL_STRENGTH, null, null, null, null,
                                              null, null, null);
        SignalStrengthTableTest.assertValuesCursor(rowId, values, cursor);
    }

    public void testUpdateSignalStrength() {
        Random random = new Random();
        ContentValues values = SignalStrengthTableTest.makeSignalStrengthValues(random, mBaseDb);

        // insert using base db
        long rowId = mBaseDb.insert(TBL_SIGNAL_STRENGTH, null, values);

        // only update strength and timestamp
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(COL_SIGNAL_TYPE, String.valueOf(random.nextGaussian()));
        updatedValues.put(COL_TIMESTAMP, random.nextLong());

        mSmartNetworkDb.update(TBL_SIGNAL_STRENGTH, updatedValues, null, null);

        // query and verify using base db
        Cursor cursor = mBaseDb.query(TBL_SIGNAL_STRENGTH, null, null, null, null, null,
                                      null, null);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getCount());

        // original values
        assertEquals(rowId, cursor.getLong(cursor.getColumnIndex(COL_ID)));
        assertEquals(TestHelper.getLong(values, COL_FK_MONITOR_SESSION),
                     cursor.getLong(cursor.getColumnIndex(COL_FK_MONITOR_SESSION)));
        assertEquals(values.getAsString(COL_SIGNAL_LEVEL),
                     cursor.getString(cursor.getColumnIndex(COL_SIGNAL_LEVEL)));
        // updated values
        assertEquals(updatedValues.getAsString(COL_SIGNAL_TYPE),
                     cursor.getString(cursor.getColumnIndex(COL_SIGNAL_TYPE)));
        assertEquals(TestHelper.getLong(updatedValues, COL_TIMESTAMP),
                     cursor.getLong(cursor.getColumnIndex(COL_TIMESTAMP)));
        cursor.close();
    }

    public void testDeleteSignalStrength() {
        Random random = new Random();
        ContentValues values = SignalStrengthTableTest.makeSignalStrengthValues(random, mBaseDb);

        // insert using base db
        long rowId = mBaseDb.insert(TBL_SIGNAL_STRENGTH, null, values);

        // delete
        int deleted = mSmartNetworkDb.delete(TBL_SIGNAL_STRENGTH, "1", null);
        assertEquals(1, deleted);

        // query and verify using base db
        Cursor cursor = mBaseDb.query(TBL_SIGNAL_STRENGTH, null, null, null, null, null,
                                      null, null);
        assertNotNull(cursor);
        assertFalse(cursor.moveToFirst());
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    public void testUnimplemented() {
        fail("Test not yet implemented");
        /* TODO:
         * testQueryTopLocationComplex()
         * testUpdateTopLocationComplex()
         * testDeleteTopLocationComplex()
         * testQueryDataConnectionStateComplex()
         * testUpdateDataConnectionStateComplex()
         * testDeleteDataConnectionStateComplex()
         * testQueryServiceStateComplex()
         * testUpdateServiceStateComplex()
         * testDeleteServiceStateComplex()
         * testQuerySignalStrengthComplex()
         * testUpdateSignalStrengthComplex()
         * testDeleteSignalStrengthComplex()
         */
    }
}
