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

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.test.mock.MockContentProvider;

import com.motorola.contextual.smartnetwork.db.Tuple;
import com.motorola.contextual.smartnetwork.db.table.PoiTable;
import com.motorola.contextual.smartnetwork.db.table.PoiTuple;
import com.motorola.contextual.smartnetwork.test.mockobjects.TableContext;

public class PoiTableTest extends AndroidTestCase {

    private static final String AUTHORITY_POI =
        "com.motorola.contextual.virtualsensor.locationsensor";

    private TableContext mTestContext;
    private PoiTable mPoiTable;
    private PoiContentProvider mTestProvider;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTestContext = new TableContext(getContext());
        mTestProvider = new PoiContentProvider();
        mTestContext.addProvider(AUTHORITY_POI, mTestProvider);
        mPoiTable = new PoiTable();
    }

    @Override
    protected void tearDown() throws Exception {
        mTestContext = null;
        mTestProvider = null;
        mPoiTable = null;
        super.tearDown();
    }

    @Override
    public void testAndroidTestCaseSetupProperly() {
        super.testAndroidTestCaseSetupProperly();
        assertNotNull(mTestContext);
        assertNotNull(mTestProvider);
        assertNotNull(mPoiTable);
    }

    private static class PoiContentProvider extends MockContentProvider {
        Cursor mQueryResult;

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                            String sortOrder) {
            return mQueryResult;
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            // set the query result cursor using values from ContentValues used for insert
            String[] keys = values.keySet().toArray(new String[values.size() + 1]);
            // make space for row id
            keys[keys.length - 1] = Tuple.COL_ID;
            // generate a row id
            final long rowId = System.currentTimeMillis();

            MatrixCursor cursor = new MatrixCursor(keys);
            MatrixCursor.RowBuilder rowBuilder = cursor.newRow();
            // set all values except the row id
            for (int i = 0; i < values.size(); i++) {
                rowBuilder.add(values.get(keys[i]));
            }
            // add row id last
            rowBuilder.add(rowId);

            mQueryResult = cursor;
            Uri insertedUri = ContentUris.withAppendedId(uri, rowId);

            return insertedUri;
        }
    }

    public void testQuerySingleRowSelectAll() {
        Cursor cursor = makePoiCursor(1);
        assertNotNull(cursor);
        mTestProvider.mQueryResult = cursor;

        // select all tuples
        List<PoiTuple> tuples = mPoiTable.query(mTestContext, PoiTuple.COLUMNS, null,
                                                null, null, null, null, null);
        assertNotNull(tuples);
        // klocwork
        if (tuples == null) return;
        assertEquals(1, tuples.size());
        assertPoiTuples(cursor, tuples);
    }

    public void testQueryMultiRowsSelectAll() {
        Random random = new Random();
        int count = random.nextInt(100) + 1;
        Cursor cursor = makePoiCursor(count);
        assertNotNull(cursor);
        mTestProvider.mQueryResult = cursor;

        // select all tuples
        List<PoiTuple> tuples = mPoiTable.query(mTestContext, PoiTuple.COLUMNS, null, null, null,
                                                null, null, null);
        assertNotNull(tuples);
        // klocwork
        if (tuples == null) return;
        assertEquals(count, tuples.size());
        assertPoiTuples(cursor, tuples);
    }

    public void testInsert() {
        final PoiTuple inputTuple = makePoiTuple();
        assertNotNull(inputTuple);

        // insert row
        final long rowId = mPoiTable.insert(mTestContext, inputTuple);
        assertTrue(rowId > 0);

        // verify row
        List<PoiTuple> tuples = mPoiTable.query(mTestContext, PoiTuple.COLUMNS, null, null, null,
                                                null, null, null);
        assertNotNull(tuples);
        // klocwork
        if (tuples == null) return;
        assertEquals(1, tuples.size());

        PoiTuple resultTuple = tuples.get(0);
        assertNotNull(resultTuple);
        assertEquals(rowId, resultTuple.getId());
        assertEquals(inputTuple.getString(PoiTuple.COL_POI),
                     resultTuple.getString(PoiTuple.COL_POI));
        assertEquals(inputTuple.getDouble(PoiTuple.COL_LAT),
                     resultTuple.getDouble(PoiTuple.COL_LAT));
        assertEquals(inputTuple.getDouble(PoiTuple.COL_LGT),
                     resultTuple.getDouble(PoiTuple.COL_LGT));
        assertEquals(inputTuple.getInt(PoiTuple.COL_RADIUS),
                     resultTuple.getInt(PoiTuple.COL_RADIUS));
        assertEquals(PoiTable.POI_TYPE,
                     resultTuple.getString(PoiTuple.COL_POI_TYPE));
        assertEquals(inputTuple.getString(PoiTuple.COL_CELL_TOWERS),
                     resultTuple.getString(PoiTuple.COL_CELL_TOWERS));
        assertEquals(inputTuple.getString(PoiTuple.COL_CONNECTED_WIFI),
                     resultTuple.getString(PoiTuple.COL_CONNECTED_WIFI));
        assertEquals(inputTuple.getString(PoiTuple.COL_WIFI_SSID),
                     resultTuple.getString(PoiTuple.COL_WIFI_SSID));
    }

    /**
     * Asserts that the values of the tuples match the values in the cursor
     * @param cursor cursor containing expected values
     * @param tuples list of tuples to verify
     */
    private static void assertPoiTuples(Cursor cursor, List<PoiTuple> tuples) {
        assertNotNull(cursor);
        assertNotNull(tuples);
        assertTrue(cursor.getCount() > 0);
        assertEquals(cursor.getCount(), tuples.size());
        assertTrue(cursor.moveToFirst());
        for (PoiTuple tuple : tuples) {
            assertNotNull(tuple);
            assertEquals(cursor.getLong(cursor.getColumnIndex(PoiTuple.COL_ID)),
                         tuple.getId());
            assertEquals(cursor.getString(cursor.getColumnIndex(PoiTuple.COL_POI)),
                         tuple.getString(PoiTuple.COL_POI));
            assertEquals(cursor.getDouble(cursor.getColumnIndex(PoiTuple.COL_LAT)),
                         tuple.getDouble(PoiTuple.COL_LAT));
            assertEquals(cursor.getDouble(cursor.getColumnIndex(PoiTuple.COL_LGT)),
                         tuple.getDouble(PoiTuple.COL_LGT));
            assertEquals(cursor.getInt(cursor.getColumnIndex(PoiTuple.COL_RADIUS)),
                         tuple.getInt(PoiTuple.COL_RADIUS));
            assertEquals(cursor.getString(cursor.getColumnIndex(PoiTuple.COL_CELL_TOWERS)),
                         tuple.getString(PoiTuple.COL_CELL_TOWERS));
            assertEquals(cursor.getString(cursor.getColumnIndex(PoiTuple.COL_CONNECTED_WIFI)),
                         tuple.getString(PoiTuple.COL_CONNECTED_WIFI));
            assertEquals(cursor.getString(cursor.getColumnIndex(PoiTuple.COL_WIFI_SSID)),
                         tuple.getString(PoiTuple.COL_WIFI_SSID));
            cursor.moveToNext();
        }
    }

    /**
     * Generate a Cursor with Poi rows containing random values
     * @param count number of rows to generate
     * @return a Cursor containing randomly generated Poi rows
     */
    private static Cursor makePoiCursor(int count) {
        Random random = new Random();
        long id, radius;
        double lat, lgt;
        String poi, poiType, cellTowers, connectedWifi, wifiSsid;
        final int maxRadius = 2000;
        // COL_ID, COL_POI, COL_LAT, COL_LGT, COL_RADIUS, COL_POI_TYPE, COL_CELL_TOWERS, COL_CONNECTED_WIFI, COL_WIFI_SSID
        MatrixCursor cursor = new MatrixCursor(PoiTuple.COLUMNS);
        for (int i = 0; i < count; i++) {
            id = random.nextLong();
            if (id < 0) {
                id = id * -1L;
            } else if (id == 0) {
                id = 1L;
            }
            poi = String.valueOf(random.nextGaussian());
            lat = random.nextDouble();
            lgt = random.nextDouble();
            radius = random.nextInt(maxRadius) + 1;
            poiType = String.valueOf(random.nextGaussian());
            cellTowers = String.valueOf(random.nextGaussian());
            connectedWifi = String.valueOf(random.nextGaussian());
            wifiSsid = String.valueOf(random.nextGaussian());
            cursor.newRow().add(id).add(poi).add(lat).add(lgt).add(radius).add(poiType)
            .add(cellTowers).add(connectedWifi).add(wifiSsid);
        }
        cursor.moveToFirst();

        return cursor;
    }

    /**
     * @return a new PoiTuple with randomly generated values, without row id
     */
    private static PoiTuple makePoiTuple() {
        PoiTuple tuple = new PoiTuple();
        Random random = new Random();
        final int maxRadius = 2000;
        tuple.put(PoiTuple.COL_POI, String.valueOf(random.nextGaussian()));
        tuple.put(PoiTuple.COL_LAT, random.nextDouble());
        tuple.put(PoiTuple.COL_LGT, random.nextDouble());
        tuple.put(PoiTuple.COL_RADIUS, random.nextInt(maxRadius) + 1);
        tuple.put(PoiTuple.COL_POI_TYPE, String.valueOf(random.nextGaussian()));
        tuple.put(PoiTuple.COL_CELL_TOWERS, String.valueOf(random.nextGaussian()));
        tuple.put(PoiTuple.COL_CONNECTED_WIFI, String.valueOf(random.nextGaussian()));
        tuple.put(PoiTuple.COL_WIFI_SSID, String.valueOf(random.nextGaussian()));
        return tuple;
    }

    public void testUnimplemented() {
        fail("Test not yet implemented");
        /* TODO:
         * testUpdate()
         * testUpdateComplex()
         * testDelete()
         * testDeleteComplex()
         */
    }
}
