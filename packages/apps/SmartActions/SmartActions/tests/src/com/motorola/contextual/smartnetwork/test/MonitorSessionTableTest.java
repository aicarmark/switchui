/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/07/09   IKCTXTAW-487    Initial version
 */

package com.motorola.contextual.smartnetwork.test;

import java.util.List;
import java.util.Random;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.text.format.DateUtils;

import com.motorola.contextual.smartnetwork.db.SmartNetworkDbSchema;
import com.motorola.contextual.smartnetwork.db.SmartNetworkSqliteDb;
import com.motorola.contextual.smartnetwork.db.table.MonitorSessionTable;
import com.motorola.contextual.smartnetwork.db.table.MonitorSessionTuple;
import com.motorola.contextual.smartnetwork.test.mockobjects.DbContext;

public class MonitorSessionTableTest extends AndroidTestCase implements SmartNetworkDbSchema {

    private DbContext mTestContext;

    private static final int DAY_IN_MS = (int) DateUtils.DAY_IN_MILLIS;
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
     * Returns a MonitorSessionTuple with randomly populated values, and
     * automatically inserts a randomly generated TopLocation row into the TopLocation table
     * to reference
     * @param random random object to use
     * @param db SQLiteDatabase to insert the referenced TopLocation row into
     * @return MonitorSessionTuple with randomly populated values
     */
    static MonitorSessionTuple makeMonitorSessionTuple(Random random, SQLiteDatabase db) {
        // first insert TopLocation row to reference
        ContentValues topLocationValues = TopLocationTableTest.makeTopLocationValues(random);
        // insert row
        final long topLocationRowId = db.insert(TBL_TOP_LOCATION, null, topLocationValues);
        assertTrue(topLocationRowId > 0);

        MonitorSessionTuple tuple = new MonitorSessionTuple();
        tuple.put(COL_FK_TOP_LOCATION, topLocationRowId);
        long startTime = random.nextLong();
        if (startTime < 0) {
            startTime = startTime * -1;
        }
        tuple.put(COL_START_TIME, startTime);
        long endTime = startTime + random.nextInt(3 * DAY_IN_MS);
        tuple.put(COL_END_TIME, endTime);
        return tuple;
    }

    /**
     * Returns ContentValues for MonitorSession with randomly populated values, and
     * automatically inserts a randomly generated TopLocation row into the TopLocation table
     * to reference
     * @param random random object to use
     * @param db SQLiteDatabase to insert the referenced TopLocation row into
     * @return ContentValues for MonitorSession with randomly populated values
     */
    static ContentValues makeMonitorSessionValues(Random random, SQLiteDatabase db) {
        // first insert TopLocation row to reference
        ContentValues topLocationValues = TopLocationTableTest.makeTopLocationValues(random);
        // insert row
        final long topLocationRowId = db.insert(TBL_TOP_LOCATION, null, topLocationValues);
        assertTrue(topLocationRowId > 0);

        ContentValues cv = new ContentValues();
        cv.put(COL_FK_TOP_LOCATION, topLocationRowId);
        long startTime = random.nextLong();
        if (startTime < 0) {
            startTime = startTime * -1;
        }
        cv.put(COL_START_TIME, startTime);
        long endTime = startTime + random.nextInt(3 * DAY_IN_MS);
        cv.put(COL_END_TIME, endTime);
        return cv;
    }

    /**
     * @param rowId expected row id
     * @param tuple tuple holding expected values
     * @param cursor cursor to verify
     */
    static void assertTupleCursor(long rowId, MonitorSessionTuple tuple, Cursor cursor) {
        assertNotNull(tuple);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getCount());
        assertEquals(rowId, cursor.getLong(cursor.getColumnIndex(COL_ID)));
        assertEquals(tuple.getLong(COL_FK_TOP_LOCATION),
                     cursor.getLong(cursor.getColumnIndex(COL_FK_TOP_LOCATION)));
        assertEquals(tuple.getLong(COL_START_TIME),
                     cursor.getLong(cursor.getColumnIndex(COL_START_TIME)));
        assertEquals(tuple.getLong(COL_END_TIME),
                     cursor.getLong(cursor.getColumnIndex(COL_END_TIME)));
    }

    /**
     * @param rowId expected row id
     * @param values ContentValues holding expected values
     * @param tuple tuple to verify
     */
    static void assertValuesTuple(long rowId, ContentValues values, MonitorSessionTuple tuple) {
        assertNotNull(values);
        assertNotNull(tuple);
        // values does not have row id so add 1 to compare number of fields/values
        assertEquals(values.size() + 1, tuple.size());
        assertEquals(rowId, tuple.getId());
        assertEquals(TestHelper.getLong(values, COL_FK_TOP_LOCATION),
                     tuple.getLong(COL_FK_TOP_LOCATION));
        assertEquals(TestHelper.getLong(values, COL_START_TIME),
                     tuple.getLong(COL_START_TIME));
        assertEquals(TestHelper.getLong(values, COL_END_TIME),
                     tuple.getLong(COL_END_TIME));
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
        assertEquals(TestHelper.getLong(values, COL_FK_TOP_LOCATION),
                     cursor.getLong(cursor.getColumnIndex(COL_FK_TOP_LOCATION)));
        assertEquals(TestHelper.getLong(values, COL_START_TIME),
                     cursor.getLong(cursor.getColumnIndex(COL_START_TIME)));
        assertEquals(TestHelper.getLong(values, COL_END_TIME),
                     cursor.getLong(cursor.getColumnIndex(COL_END_TIME)));
    }

    public void testInsert() {
        // generate random tuple
        Random random = new Random();
        MonitorSessionTuple tuple = makeMonitorSessionTuple(random, mBaseDb);
        assertNotNull(tuple);

        // insert tuple
        MonitorSessionTable table = new MonitorSessionTable();
        final long rowId = table.insert(mTestContext, tuple);
        assertTrue(rowId > 0);

        // verify using base db
        Cursor cursor = mBaseDb.query(TBL_MONITOR_SESSION, null, null, null, null, null, null);
        assertTupleCursor(rowId, tuple, cursor);
        cursor.close();
    }

    public void testQuery() {
        // generate and insert a random MonitorSession row using base db
        Random random = new Random();
        ContentValues values = makeMonitorSessionValues(random, mBaseDb);
        final long rowId = mBaseDb.insert(TBL_MONITOR_SESSION, null, values);

        // query tuple
        MonitorSessionTable table = new MonitorSessionTable();
        List<MonitorSessionTuple> tuples = table.query(mTestContext, null, null, null, null,
                                           null, null, null);

        // verify values
        assertNotNull(tuples);
        // klocwork
        if (tuples != null) {
            assertFalse(tuples.isEmpty());
            assertEquals(1, tuples.size());
            MonitorSessionTuple tuple = tuples.get(0);
            assertValuesTuple(rowId, values, tuple);
        }
    }

    public void testRawQuery() {
        // generate and insert random MonitorSession rows using base db
        Random random = new Random();
        ContentValues values1 = makeMonitorSessionValues(random, mBaseDb);
        final long row1 = mBaseDb.insert(TBL_MONITOR_SESSION, null, values1);
        ContentValues values2 = makeMonitorSessionValues(random, mBaseDb);
        final long row2 = mBaseDb.insert(TBL_MONITOR_SESSION, null, values2);

        // query tuple
        MonitorSessionTable table = new MonitorSessionTable();
        String sql = "SELECT * FROM " + TBL_MONITOR_SESSION + " WHERE " + COL_ID + " = ?";
        String[] selectionArgs = { String.valueOf(row1) };
        List<MonitorSessionTuple> tuples = table.rawQuery(mTestContext, sql, selectionArgs);

        // verify values
        assertNotNull(tuples);
        // klocwork
        if (tuples != null) {
            assertFalse(tuples.isEmpty());
            assertEquals(1, tuples.size());
            MonitorSessionTuple tuple = tuples.get(0);
            assertValuesTuple(row1, values1, tuple);
        }
    }

    public void testUpdate() {
        // generate and insert a random MonitorSession row using base db
        Random random = new Random();
        ContentValues originalValues = makeMonitorSessionValues(random, mBaseDb);
        final long rowId = mBaseDb.insert(TBL_MONITOR_SESSION, null, originalValues);

        // update end time
        MonitorSessionTuple tuple = new MonitorSessionTuple();
        tuple.put(COL_END_TIME, random.nextLong());

        // update table
        MonitorSessionTable table = new MonitorSessionTable();
        int updated = table.update(mTestContext, tuple, null, null);
        assertEquals(1, updated);

        // query and verify using base db
        Cursor cursor = mBaseDb.query(TBL_MONITOR_SESSION, null, null, null, null, null, null);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getCount());
        // original values
        assertEquals(rowId, cursor.getLong(cursor.getColumnIndex(COL_ID)));
        assertEquals(TestHelper.getLong(originalValues, COL_FK_TOP_LOCATION),
                     cursor.getLong(cursor.getColumnIndex(COL_FK_TOP_LOCATION)));
        assertEquals(TestHelper.getLong(originalValues, COL_START_TIME),
                     cursor.getLong(cursor.getColumnIndex(COL_START_TIME)));
        // updated values
        try {
            assertEquals(tuple.getLong(COL_END_TIME),
                         cursor.getLong(cursor.getColumnIndex(COL_END_TIME)));
        } catch (IllegalArgumentException iae) {
            fail(iae.toString());
        }
        cursor.close();
    }

    public void testDelete() {
        // generate and insert random MonitorSession rows using base db
        Random random = new Random();
        ContentValues values1 = makeMonitorSessionValues(random, mBaseDb);
        final long row1 = mBaseDb.insert(TBL_MONITOR_SESSION, null, values1);
        ContentValues values2 = makeMonitorSessionValues(random, mBaseDb);
        final long row2 = mBaseDb.insert(TBL_MONITOR_SESSION, null, values2);

        // delete row 1
        MonitorSessionTable table = new MonitorSessionTable();
        String whereClause = COL_ID + " = ?";
        String[] whereArgs = { String.valueOf(row1) };
        int deleted = table.delete(mTestContext, whereClause, whereArgs);
        assertEquals(1, deleted);

        // query verify using base db
        Cursor cursor = mBaseDb.query(TBL_MONITOR_SESSION, null, null, null, null, null, null);
        assertValuesCursor(row2, values2, cursor);
        cursor.close();
    }
}
