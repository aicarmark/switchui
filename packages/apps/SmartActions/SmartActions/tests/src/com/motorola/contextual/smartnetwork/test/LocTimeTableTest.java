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

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.mock.MockContentProvider;

import com.motorola.contextual.smartnetwork.db.table.LocTimeTable;
import com.motorola.contextual.smartnetwork.db.table.LocTimeTuple;
import com.motorola.contextual.smartnetwork.test.mockobjects.TableContext;

public class LocTimeTableTest extends AndroidTestCase {

    private static final String AUTHORITY_LOCATION_SENSOR =
        "com.motorola.contextual.virtualsensor.locationsensor";

    private TableContext mTestContext;
    private LocTimeTable mLocTimeTable;
    private LocTimeContentProvider mTestProvider;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTestContext = new TableContext(getContext());
        mTestProvider = new LocTimeContentProvider();
        mTestContext.addProvider(AUTHORITY_LOCATION_SENSOR, mTestProvider);
        mLocTimeTable = new LocTimeTable();
    }

    @Override
    protected void tearDown() throws Exception {
        mTestContext = null;
        mTestProvider = null;
        mLocTimeTable = null;
        super.tearDown();
    }

    @Override
    public void testAndroidTestCaseSetupProperly() {
        super.testAndroidTestCaseSetupProperly();
        assertNotNull(mTestContext);
        assertNotNull(mTestProvider);
        assertNotNull(mLocTimeTable);
    }

    private static class LocTimeContentProvider extends MockContentProvider {
        Cursor mQueryResult;

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                            String sortOrder) {
            return mQueryResult;
        }
    }

    public void testQuerySingleRowSelectAll() {
        Cursor cursor = makeLocTimeCursor(1);
        assertNotNull(cursor);
        mTestProvider.mQueryResult = cursor;

        // select all tuples
        List<LocTimeTuple> tuples = mLocTimeTable.query(mTestContext, LocTimeTuple.COLUMNS, null,
                                    null, null, null, null, null);
        assertNotNull(tuples);
        // klocwork
        if (tuples != null) {
            assertEquals(1, tuples.size());
            assertLocTimeTuples(cursor, tuples);
        }
    }

    public void testQueryMultiRowsSelectAll() {
        Random random = new Random();
        int count = random.nextInt(100) + 1;
        Cursor cursor = makeLocTimeCursor(count);
        assertNotNull(cursor);
        mTestProvider.mQueryResult = cursor;

        // select all tuples
        List<LocTimeTuple> tuples = mLocTimeTable.query(mTestContext, LocTimeTuple.COLUMNS, null,
                                    null, null, null, null, null);
        assertNotNull(tuples);
        // klocwork
        if (tuples != null) {
            assertEquals(count, tuples.size());
            assertLocTimeTuples(cursor, tuples);
        }
    }

    /**
     * Asserts that the values of the tuples match the values in the cursor
     * @param cursor cursor containing expected values
     * @param tuples list of tuples to verify
     */
    private static void assertLocTimeTuples(Cursor cursor, List<LocTimeTuple> tuples) {
        assertNotNull(cursor);
        assertNotNull(tuples);
        assertTrue(cursor.getCount() > 0);
        assertEquals(cursor.getCount(), tuples.size());
        assertTrue(cursor.moveToFirst());
        for (LocTimeTuple tuple : tuples) {
            assertNotNull(tuple);
            assertEquals(cursor.getLong(cursor.getColumnIndex(LocTimeTuple.COL_ID)),
                         tuple.getId());
            assertEquals(cursor.getString(cursor.getColumnIndex(LocTimeTuple.COL_WIFI_SSID)),
                         tuple.getString(LocTimeTuple.COL_WIFI_SSID));
            assertEquals(cursor.getString(cursor.getColumnIndex(LocTimeTuple.COL_CELL_TOWER)),
                         tuple.getString(LocTimeTuple.COL_CELL_TOWER));
            assertEquals(cursor.getLong(cursor.getColumnIndex(LocTimeTuple.COL_START_TIME)),
                         tuple.getLong(LocTimeTuple.COL_START_TIME));
            assertEquals(cursor.getLong(cursor.getColumnIndex(LocTimeTuple.COL_END_TIME)),
                         tuple.getLong(LocTimeTuple.COL_END_TIME));
            assertEquals(cursor.getLong(cursor.getColumnIndex(LocTimeTuple.COL_DURATION)),
                         tuple.getLong(LocTimeTuple.COL_DURATION));
            cursor.moveToNext();
        }
    }

    /**
     * Generate a Cursor with LocTime rows containing random values
     * @param count number of rows to generate
     * @return a Cursor containing randomly generated LocTime rows
     */
    private static Cursor makeLocTimeCursor(int count) {
        Random random = new Random();
        long id, startTime, endTime, duration;
        String wifiSsid, cellTower;
        final int maxDuration = 1000000;
        // COL_ID, COL_WIFI_SSID, COL_CELL_TOWER, COL_START_TIME, COL_END_TIME, COL_DURATION
        MatrixCursor cursor = new MatrixCursor(LocTimeTuple.COLUMNS);
        for (int i = 0; i < count; i++) {
            id = random.nextLong();
            if (id < 0) {
                id = id * -1L;
            } else if (id == 0) {
                id = 1L;
            }
            wifiSsid = String.valueOf(random.nextDouble());
            cellTower = String.valueOf(random.nextFloat());
            startTime = random.nextLong();
            if (startTime < 0) startTime = startTime * -1L;
            // don't use randomLong for duration, as the sum may wrap around to a negative number
            duration = random.nextInt(maxDuration) + 1L;
            endTime = startTime + duration;

            cursor.newRow().add(id).add(wifiSsid).add(cellTower).add(startTime).add(endTime)
            .add(duration);
        }
        cursor.moveToFirst();

        return cursor;
    }
}
