/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
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
import com.motorola.contextual.smartnetwork.db.table.TopLocationTable;
import com.motorola.contextual.smartnetwork.db.table.TopLocationTuple;
import com.motorola.contextual.smartnetwork.test.mockobjects.DbContext;

public class TopLocationTableTest extends AndroidTestCase implements SmartNetworkDbSchema {

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
     * @param random Random object to use
     * @return TopLocationTuple with randomly populated values
     */
    static TopLocationTuple makeTopLocationTuple(Random random) {
        TopLocationTuple tuple = new TopLocationTuple();
        tuple.put(COL_POI, String.valueOf(random.nextGaussian()));
        tuple.put(COL_WIFI_SSID, String.valueOf(random.nextGaussian()));
        tuple.put(COL_CELL_TOWERS, String.valueOf(random.nextGaussian()));
        tuple.put(COL_NETWORK_CONDITION, String.valueOf(random.nextInt(3)));
        tuple.put(COL_PREVIOUS_NETWORK_CONDITION, String.valueOf(random.nextInt(3)));
        tuple.put(COL_NETWORK_CONDITION_UPDATED, random.nextLong());
        tuple.put(COL_TIME_SPENT, random.nextLong());
        tuple.put(COL_RANK, random.nextInt());
        tuple.put(COL_RANK_UPDATED, random.nextLong());
        return tuple;
    }

    /**
     * @param random Random object to use
     * @return ContentValues for TopLocation with randomly populated values
     */
    static ContentValues makeTopLocationValues(Random random) {
        ContentValues cv = new ContentValues();
        cv.put(COL_POI, String.valueOf(random.nextGaussian()));
        cv.put(COL_WIFI_SSID, String.valueOf(random.nextGaussian()));
        cv.put(COL_CELL_TOWERS, String.valueOf(random.nextGaussian()));
        cv.put(COL_NETWORK_CONDITION, String.valueOf(random.nextInt(3)));
        cv.put(COL_PREVIOUS_NETWORK_CONDITION, String.valueOf(random.nextInt(3)));
        cv.put(COL_NETWORK_CONDITION_UPDATED, random.nextLong());
        cv.put(COL_TIME_SPENT, random.nextLong());
        cv.put(COL_RANK, random.nextInt());
        cv.put(COL_RANK_UPDATED, random.nextLong());
        return cv;
    }

    /**
     * @param rowId expected row id
     * @param tuple tuple holding expected values
     * @param cursor cursor to verify
     */
    static void assertTupleCursor(long rowId, TopLocationTuple tuple, Cursor cursor) {
        assertNotNull(tuple);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getCount());
        assertEquals(rowId, cursor.getLong(cursor.getColumnIndex(COL_ID)));
        assertEquals(tuple.getString(COL_POI),
                     cursor.getString(cursor.getColumnIndex(COL_POI)));
        assertEquals(tuple.getString(COL_WIFI_SSID),
                     cursor.getString(cursor.getColumnIndex(COL_WIFI_SSID)));
        assertEquals(tuple.getString(COL_CELL_TOWERS),
                     cursor.getString(cursor.getColumnIndex(COL_CELL_TOWERS)));
        assertEquals(tuple.getString(COL_NETWORK_CONDITION),
                     cursor.getString(cursor.getColumnIndex(COL_NETWORK_CONDITION)));
        assertEquals(tuple.getString(COL_PREVIOUS_NETWORK_CONDITION),
                     cursor.getString(cursor.getColumnIndex(COL_PREVIOUS_NETWORK_CONDITION)));
        assertEquals(tuple.getLong(COL_NETWORK_CONDITION_UPDATED),
                     cursor.getLong(cursor.getColumnIndex(COL_NETWORK_CONDITION_UPDATED)));
        assertEquals(tuple.getLong(COL_TIME_SPENT),
                     cursor.getLong(cursor.getColumnIndex(COL_TIME_SPENT)));
        assertEquals(tuple.getInt(COL_RANK),
                     cursor.getInt(cursor.getColumnIndex(COL_RANK)));
        assertEquals(tuple.getLong(COL_RANK_UPDATED),
                     cursor.getLong(cursor.getColumnIndex(COL_RANK_UPDATED)));
    }

    /**
     * @param rowId expected row id
     * @param values ContentValues holding expected values
     * @param tuple tuple to verify
     */
    static void assertValuesTuple(long rowId, ContentValues values, TopLocationTuple tuple) {
        assertNotNull(values);
        assertNotNull(tuple);
        // values does not have row id so add 1 to compare number of fields/values
        assertEquals(values.size() + 1, tuple.size());
        assertEquals(rowId, tuple.getId());
        assertEquals(values.getAsString(COL_POI), tuple.getString(COL_POI));
        assertEquals(values.getAsString(COL_WIFI_SSID), tuple.getString(COL_WIFI_SSID));
        assertEquals(values.getAsString(COL_CELL_TOWERS), tuple.getString(COL_CELL_TOWERS));
        assertEquals(values.getAsString(COL_NETWORK_CONDITION),
                     tuple.getString(COL_NETWORK_CONDITION));
        assertEquals(values.getAsString(COL_PREVIOUS_NETWORK_CONDITION),
                     tuple.getString(COL_PREVIOUS_NETWORK_CONDITION));
        assertEquals(TestHelper.getLong(values, COL_NETWORK_CONDITION_UPDATED),
                     tuple.getLong(COL_NETWORK_CONDITION_UPDATED));
        assertEquals(TestHelper.getLong(values, COL_TIME_SPENT),
                     tuple.getLong(COL_TIME_SPENT));
        assertEquals(TestHelper.getInt(values, COL_RANK), tuple.getInt(COL_RANK));
        assertEquals(TestHelper.getLong(values, COL_RANK_UPDATED),
                     tuple.getLong(COL_RANK_UPDATED));
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
        assertEquals(values.getAsString(COL_POI),
                     cursor.getString(cursor.getColumnIndex(COL_POI)));
        assertEquals(values.getAsString(COL_WIFI_SSID),
                     cursor.getString(cursor.getColumnIndex(COL_WIFI_SSID)));
        assertEquals(values.getAsString(COL_CELL_TOWERS),
                     cursor.getString(cursor.getColumnIndex(COL_CELL_TOWERS)));
        assertEquals(values.getAsString(COL_NETWORK_CONDITION),
                     cursor.getString(cursor.getColumnIndex(COL_NETWORK_CONDITION)));
        assertEquals(values.getAsString(COL_PREVIOUS_NETWORK_CONDITION),
                     cursor.getString(cursor.getColumnIndex(COL_PREVIOUS_NETWORK_CONDITION)));
        assertEquals(TestHelper.getLong(values, COL_NETWORK_CONDITION_UPDATED),
                     cursor.getLong(cursor.getColumnIndex(COL_NETWORK_CONDITION_UPDATED)));
        assertEquals(TestHelper.getLong(values, COL_TIME_SPENT),
                     cursor.getLong(cursor.getColumnIndex(COL_TIME_SPENT)));
        assertEquals(TestHelper.getInt(values, COL_RANK),
                     cursor.getInt(cursor.getColumnIndex(COL_RANK)));
        assertEquals(TestHelper.getLong(values, COL_RANK_UPDATED),
                     cursor.getLong(cursor.getColumnIndex(COL_RANK_UPDATED)));
    }

    public void testInsert() {
        // generate random tuple
        Random random = new Random();
        TopLocationTuple tuple = makeTopLocationTuple(random);
        assertNotNull(tuple);

        // insert tuple
        TopLocationTable table = new TopLocationTable();
        final long rowId = table.insert(mTestContext, tuple);
        assertTrue(rowId > 0);

        // verify using base db
        Cursor cursor = mBaseDb.query(TBL_TOP_LOCATION, null, null, null, null, null, null);
        assertTupleCursor(rowId, tuple, cursor);
        cursor.close();
    }

    public void testQuery() {
        // generate and insert a random TopLocation row using base db
        Random random = new Random();
        ContentValues values = makeTopLocationValues(random);
        final long rowId = mBaseDb.insert(TBL_TOP_LOCATION, null, values);

        // query tuple
        TopLocationTable table = new TopLocationTable();
        List<TopLocationTuple> tuples = table.query(mTestContext, null, null, null, null, null,
                                        null, null);

        // verify values
        assertNotNull(tuples);
        // klocwork
        if (tuples == null) return;
        assertFalse(tuples.isEmpty());
        assertEquals(1, tuples.size());
        TopLocationTuple tuple = tuples.get(0);
        assertValuesTuple(rowId, values, tuple);
    }

    public void testRawQuery() {
        // generate and insert random TopLocation rows using base db
        Random random = new Random();
        ContentValues values1 = makeTopLocationValues(random);
        final long row1 = mBaseDb.insert(TBL_TOP_LOCATION, null, values1);
        ContentValues values2 = makeTopLocationValues(random);
        final long row2 = mBaseDb.insert(TBL_TOP_LOCATION, null, values2);

        // query tuple
        TopLocationTable table = new TopLocationTable();
        String sql = "SELECT * FROM " + TBL_TOP_LOCATION + " WHERE " + COL_ID + " = ?";
        String[] selectionArgs = { String.valueOf(row1) };
        List<TopLocationTuple> tuples = table.rawQuery(mTestContext, sql, selectionArgs);

        // verify values
        assertNotNull(tuples);
        // klocwork
        if (tuples == null) return;
        assertFalse(tuples.isEmpty());
        assertEquals(1, tuples.size());
        TopLocationTuple tuple = tuples.get(0);
        assertValuesTuple(row1, values1, tuple);
    }

    public void testUpdate() {
        // generate and insert a random TopLocation row using base db
        Random random = new Random();
        ContentValues originalValues = makeTopLocationValues(random);
        final long rowId = mBaseDb.insert(TBL_TOP_LOCATION, null, originalValues);

        // update cell towers and last updated field
        TopLocationTuple tuple = new TopLocationTuple();
        tuple.put(COL_CELL_TOWERS, String.valueOf(random.nextGaussian()));
        tuple.put(COL_NETWORK_CONDITION_UPDATED, random.nextLong());

        // update table
        TopLocationTable table = new TopLocationTable();
        int updated = table.update(mTestContext, tuple, null, null);
        assertEquals(1, updated);

        // query and verify using base db
        Cursor cursor = mBaseDb.query(TBL_TOP_LOCATION, null, null, null, null, null, null);
        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(1, cursor.getCount());
        // original values
        assertEquals(rowId, cursor.getLong(cursor.getColumnIndex(COL_ID)));
        assertEquals(originalValues.getAsString(COL_POI),
                     cursor.getString(cursor.getColumnIndex(COL_POI)));
        assertEquals(originalValues.getAsString(COL_WIFI_SSID),
                     cursor.getString(cursor.getColumnIndex(COL_WIFI_SSID)));
        assertEquals(originalValues.getAsString(COL_NETWORK_CONDITION),
                     cursor.getString(cursor.getColumnIndex(COL_NETWORK_CONDITION)));
        assertEquals(originalValues.getAsString(COL_PREVIOUS_NETWORK_CONDITION),
                     cursor.getString(cursor.getColumnIndex(COL_PREVIOUS_NETWORK_CONDITION)));
        assertEquals(TestHelper.getLong(originalValues, COL_TIME_SPENT),
                     cursor.getLong(cursor.getColumnIndex(COL_TIME_SPENT)));
        assertEquals(TestHelper.getInt(originalValues, COL_RANK),
                     cursor.getInt(cursor.getColumnIndex(COL_RANK)));
        assertEquals(TestHelper.getLong(originalValues, COL_RANK_UPDATED),
                     cursor.getLong(cursor.getColumnIndex(COL_RANK_UPDATED)));

        // updated values
        try {
            assertEquals(tuple.getString(COL_CELL_TOWERS),
                         cursor.getString(cursor.getColumnIndex(COL_CELL_TOWERS)));
            assertEquals(tuple.getLong(COL_NETWORK_CONDITION_UPDATED),
                         cursor.getLong(cursor.getColumnIndex(COL_NETWORK_CONDITION_UPDATED)));
        } catch (IllegalArgumentException iae) {
            fail(iae.toString());
        }
        cursor.close();
    }

    public void testDelete() {
        // generate and insert random TopLocation rows using base db
        Random random = new Random();
        ContentValues values1 = makeTopLocationValues(random);
        final long row1 = mBaseDb.insert(TBL_TOP_LOCATION, null, values1);
        ContentValues values2 = makeTopLocationValues(random);
        final long row2 = mBaseDb.insert(TBL_TOP_LOCATION, null, values2);

        // delete row 1
        TopLocationTable table = new TopLocationTable();
        String whereClause = COL_ID + " = ?";
        String[] whereArgs = { String.valueOf(row1) };
        int deleted = table.delete(mTestContext, whereClause, whereArgs);
        assertEquals(1, deleted);

        // query verify using base db
        Cursor cursor = mBaseDb.query(TBL_TOP_LOCATION, null, null, null, null, null, null);
        assertValuesCursor(row2, values2, cursor);
        cursor.close();
    }

    public void testUnimplemented() {
        fail("Test not yet implemented");
        /* TODO:
         * testCascadingDelete()
         */
    }
}
