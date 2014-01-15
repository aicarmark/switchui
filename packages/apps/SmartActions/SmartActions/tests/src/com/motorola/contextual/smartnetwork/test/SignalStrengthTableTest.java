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

import java.util.List;
import java.util.Random;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import com.motorola.contextual.smartnetwork.db.SmartNetworkDbSchema;
import com.motorola.contextual.smartnetwork.db.SmartNetworkSqliteDb;
import com.motorola.contextual.smartnetwork.db.table.SignalStrengthTable;
import com.motorola.contextual.smartnetwork.db.table.SignalStrengthTuple;
import com.motorola.contextual.smartnetwork.test.mockobjects.DbContext;

public class SignalStrengthTableTest extends AndroidTestCase implements SmartNetworkDbSchema {

    private DbContext mTestContext;

    // the base sqlite db
    private SQLiteDatabase mBaseDb;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTestContext = new DbContext(getContext());
        mBaseDb = new SmartNetworkSqliteDb(mTestContext).getWritableDatabase();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
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
    }

    /**
     * Returns a SignalStrengthTuple with randomly populated values, and
     * automatically inserts a randomly generated MonitorSession row into the MonitorSession table
     * to reference
     * @param random random object to use
     * @param db SQLiteDatabase to insert the referenced MonitorSession row into
     * @return SignalStrengthTuple with randomly populated values
     */
    static SignalStrengthTuple makeSignalStrengthTuple(Random random, SQLiteDatabase db) {
        // insert MonitorSession row to reference
        ContentValues monitorSessionValues =
            MonitorSessionTableTest.makeMonitorSessionValues(random, db);
        final long monitorSessionRowId = db.insert(TBL_MONITOR_SESSION, null, monitorSessionValues);
        assertTrue(monitorSessionRowId > 0);

        SignalStrengthTuple tuple = new SignalStrengthTuple();
        tuple.put(COL_FK_MONITOR_SESSION, monitorSessionRowId);
        tuple.put(COL_SIGNAL_TYPE, String.valueOf(random.nextGaussian()));
        tuple.put(COL_SIGNAL_LEVEL, String.valueOf(random.nextGaussian()));
        tuple.put(COL_TIMESTAMP, random.nextLong());
        return tuple;
    }

    /**
     * Returns ContentValues for SignalStrength with randomly populated values, and
     * automatically inserts a randomly generated MonitorSession row into the MonitorSession table
     * to reference
     * @param random random object to use
     * @param db SQLiteDatabase to insert the referenced MonitorSession row into
     * @return ContentValues for SignalStrength with randomly populated values
     */
    static ContentValues makeSignalStrengthValues(Random random, SQLiteDatabase db) {
        // insert MonitorSession row to reference
        ContentValues monitorSessionValues =
            MonitorSessionTableTest.makeMonitorSessionValues(random, db);
        final long monitorSessionRowId = db.insert(TBL_MONITOR_SESSION, null, monitorSessionValues);
        assertTrue(monitorSessionRowId > 0);

        ContentValues cv = new ContentValues();
        cv.put(COL_FK_MONITOR_SESSION, monitorSessionRowId);
        cv.put(COL_SIGNAL_TYPE, String.valueOf(random.nextGaussian()));
        cv.put(COL_SIGNAL_LEVEL, String.valueOf(random.nextGaussian()));
        cv.put(COL_TIMESTAMP, random.nextLong());
        return cv;
    }

    /**
     * @param rowId expected row id
     * @param tuple tuple holding expected values
     * @param cursor cursor to verify
     */
    static void assertTupleCursor(long rowId, SignalStrengthTuple tuple, Cursor cursor) {
        assertNotNull(tuple);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getCount());
        assertEquals(rowId, cursor.getLong(cursor.getColumnIndex(COL_ID)));
        assertEquals(tuple.getLong(COL_FK_MONITOR_SESSION),
                     cursor.getLong(cursor.getColumnIndex(COL_FK_MONITOR_SESSION)));
        assertEquals(tuple.getString(COL_SIGNAL_TYPE),
                     cursor.getString(cursor.getColumnIndex(COL_SIGNAL_TYPE)));
        assertEquals(tuple.getString(COL_SIGNAL_LEVEL),
                     cursor.getString(cursor.getColumnIndex(COL_SIGNAL_LEVEL)));
        assertEquals(tuple.getLong(COL_TIMESTAMP),
                     cursor.getLong(cursor.getColumnIndex(COL_TIMESTAMP)));
    }

    /**
     * @param rowId expected row id
     * @param values ContentValues holding expected values
     * @param tuple tuple to verify
     */
    static void assertValuesTuple(long rowId, ContentValues values,
                                  SignalStrengthTuple tuple) {
        assertNotNull(values);
        assertNotNull(tuple);
        // values does not have row id so add 1 to compare number of fields/values
        assertEquals(values.size() + 1, tuple.size());
        assertEquals(rowId, tuple.getId());
        assertEquals(TestHelper.getLong(values, COL_FK_MONITOR_SESSION),
                     tuple.getLong(COL_FK_MONITOR_SESSION));
        assertEquals(values.getAsString(COL_SIGNAL_TYPE),
                     tuple.getString(COL_SIGNAL_TYPE));
        assertEquals(values.getAsString(COL_SIGNAL_LEVEL),
                     tuple.getString(COL_SIGNAL_LEVEL));
        assertEquals(TestHelper.getLong(values, COL_TIMESTAMP),
                     tuple.getLong(COL_TIMESTAMP));
    }

    /**
     * @param rowId expected row id
     * @param values ContentValues holding expected values
     * @param cursor cursor to verify
     */
    static void assertValuesCursor(long rowId, ContentValues values, Cursor cursor) {
        assertNotNull(values);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getCount());
        assertEquals(rowId, cursor.getLong(cursor.getColumnIndex(COL_ID)));
        assertEquals(TestHelper.getLong(values, COL_FK_MONITOR_SESSION),
                     cursor.getLong(cursor.getColumnIndex(COL_FK_MONITOR_SESSION)));
        assertEquals(values.getAsString(COL_SIGNAL_TYPE),
                     cursor.getString(cursor.getColumnIndex(COL_SIGNAL_TYPE)));
        assertEquals(values.getAsString(COL_SIGNAL_LEVEL),
                     cursor.getString(cursor.getColumnIndex(COL_SIGNAL_LEVEL)));
        assertEquals(TestHelper.getLong(values, COL_TIMESTAMP),
                     cursor.getLong(cursor.getColumnIndex(COL_TIMESTAMP)));
    }

    public void testInsert() {
        // generate random tuple
        Random random = new Random();
        SignalStrengthTuple tuple = makeSignalStrengthTuple(random, mBaseDb);
        assertNotNull(tuple);

        // insert tuple
        SignalStrengthTable table = new SignalStrengthTable();
        final long rowId = table.insert(mTestContext, tuple);
        assertTrue(rowId > 0);

        // verify using base db
        Cursor cursor = mBaseDb.query(TBL_SIGNAL_STRENGTH, null, null, null, null, null,
                                      null);
        assertTupleCursor(rowId, tuple, cursor);
        cursor.close();
    }

    public void testQuery() {
        // generate and insert a random SignalStrength row using base db
        Random random = new Random();
        ContentValues values = makeSignalStrengthValues(random, mBaseDb);
        final long rowId = mBaseDb.insert(TBL_SIGNAL_STRENGTH, null, values);

        // query tuple
        SignalStrengthTable table = new SignalStrengthTable();
        List<SignalStrengthTuple> tuples = table.query(mTestContext, null, null, null, null,
                                           null, null, null);

        // verify values
        assertNotNull(tuples);
        // klocwork
        if (tuples == null) return;
        assertFalse(tuples.isEmpty());
        assertEquals(1, tuples.size());
        SignalStrengthTuple tuple = tuples.get(0);
        assertValuesTuple(rowId, values, tuple);
    }

    public void testRawQuery() {
        // generate and insert random SignalStrength rows using base db
        Random random = new Random();
        ContentValues values1 = makeSignalStrengthValues(random, mBaseDb);
        final long row1 = mBaseDb.insert(TBL_SIGNAL_STRENGTH, null, values1);
        ContentValues values2 = makeSignalStrengthValues(random, mBaseDb);
        final long row2 = mBaseDb.insert(TBL_SIGNAL_STRENGTH, null, values2);

        // query tuple
        SignalStrengthTable table = new SignalStrengthTable();
        String sql = "SELECT * FROM " + TBL_SIGNAL_STRENGTH + " WHERE " + COL_ID + " = ?";
        String[] selectionArgs = { String.valueOf(row1) };
        List<SignalStrengthTuple> tuples = table.rawQuery(mTestContext, sql, selectionArgs);

        // verify values
        assertNotNull(tuples);
        // klocwork
        if (tuples == null) return;
        assertFalse(tuples.isEmpty());
        assertEquals(1, tuples.size());
        SignalStrengthTuple tuple = tuples.get(0);
        assertValuesTuple(row1, values1, tuple);
    }

    public void testUpdate() {
        // generate and insert a random SignalStrength row using base db
        Random random = new Random();
        ContentValues originalValues = makeSignalStrengthValues(random, mBaseDb);
        final long rowId = mBaseDb.insert(TBL_SIGNAL_STRENGTH, null, originalValues);

        // update strength and timestamp
        SignalStrengthTuple tuple = new SignalStrengthTuple();
        tuple.put(COL_SIGNAL_TYPE, String.valueOf(random.nextGaussian()));
        tuple.put(COL_TIMESTAMP, random.nextLong());

        // update table
        SignalStrengthTable table = new SignalStrengthTable();
        int updated = table.update(mTestContext, tuple, null, null);
        assertEquals(1, updated);

        // query and verify using base db
        Cursor cursor = mBaseDb.query(TBL_SIGNAL_STRENGTH, null, null, null, null, null, null);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getCount());
        // original values
        assertEquals(rowId, cursor.getLong(cursor.getColumnIndex(COL_ID)));
        assertEquals(TestHelper.getLong(originalValues, COL_FK_MONITOR_SESSION),
                     cursor.getLong(cursor.getColumnIndex(COL_FK_MONITOR_SESSION)));
        assertEquals(originalValues.getAsString(COL_SIGNAL_LEVEL),
                     cursor.getString(cursor.getColumnIndex(COL_SIGNAL_LEVEL)));
        // updated values
        try {
            assertEquals(tuple.getString(COL_SIGNAL_TYPE),
                         cursor.getString(cursor.getColumnIndex(COL_SIGNAL_TYPE)));
            assertEquals(tuple.getLong(COL_TIMESTAMP),
                         cursor.getLong(cursor.getColumnIndex(COL_TIMESTAMP)));
        } catch (IllegalArgumentException iae) {
            fail(iae.toString());
        }
        cursor.close();
    }

    public void testDelete() {
        // generate and insert random SignalStrength rows using base db
        Random random = new Random();
        ContentValues values1 = makeSignalStrengthValues(random, mBaseDb);
        final long row1 = mBaseDb.insert(TBL_SIGNAL_STRENGTH, null, values1);
        ContentValues values2 = makeSignalStrengthValues(random, mBaseDb);
        final long row2 = mBaseDb.insert(TBL_SIGNAL_STRENGTH, null, values2);

        // delete row 1
        SignalStrengthTable table = new SignalStrengthTable();
        String whereClause = COL_ID + " = ?";
        String[] whereArgs = { String.valueOf(row1) };
        int deleted = table.delete(mTestContext, whereClause, whereArgs);
        assertEquals(1, deleted);

        // query verify using base db
        Cursor cursor = mBaseDb.query(TBL_SIGNAL_STRENGTH, null, null, null, null, null, null);
        assertValuesCursor(row2, values2, cursor);
        cursor.close();
    }
}
