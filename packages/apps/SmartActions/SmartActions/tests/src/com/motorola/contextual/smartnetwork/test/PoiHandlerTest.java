/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 * w04917 (Brian Lee)        2012/07/16   IKCTXTAW-492    Use contants from LocationSensor
 */

package com.motorola.contextual.smartnetwork.test;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.TRANSIENT;

import java.util.Random;

import android.content.Intent;
import android.test.ServiceTestCase;

import com.motorola.contextual.smartnetwork.NetworkMonitorService;
import com.motorola.contextual.smartnetwork.PoiHandler;
import com.motorola.contextual.smartnetwork.db.table.TopLocationTable;
import com.motorola.contextual.smartnetwork.db.table.TopLocationTuple;
import com.motorola.contextual.smartnetwork.test.mockobjects.ServiceContext;

public class PoiHandlerTest extends ServiceTestCase<PoiHandler> {
    private ServiceContext mTestContext;

    public PoiHandlerTest() {
        super(PoiHandler.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTestContext = new ServiceContext(getSystemContext());
        setContext(mTestContext);
    }

    @Override
    protected void tearDown() throws Exception {
        mTestContext.cleanup();
        mTestContext = null;
        super.tearDown();
    }

    @Override
    public void testAndroidTestCaseSetupProperly() {
        super.testAndroidTestCaseSetupProperly();
        assertNotNull(mTestContext);
    }

    private Intent makePoiIntent(String poi) {
        Intent intent = new Intent(PoiHandler.INTENT_POI_EVENT);
        intent.putExtra(PoiHandler.EXTRA_POI, poi);
        return intent;
    }

    /**
     * Enter a location with verify flag
     */
    public void testEnterPoiVerify() {
        // insert a TopLocation with verify flag
        Random random = new Random();
        TopLocationTuple tuple = TopLocationTableTest.makeTopLocationTuple(random);
        tuple.put(TopLocationTuple.COL_NETWORK_CONDITION,
                  TopLocationTable.NETWORK_CONDITION_VERIFY);
        TopLocationTable table = new TopLocationTable();
        final long rowId = table.insert(mTestContext, tuple);

        // make a POI entry intent to the location we just created above
        Intent entryIntent = makePoiIntent(tuple.getString(TopLocationTuple.COL_POI));
        startService(entryIntent);

        // verify response intent
        Intent entryResponse = mTestContext.getStartServiceIntent();
        assertNotNull(entryResponse);
        // klocwork
        if (entryResponse != null) {
            assertEquals(NetworkMonitorService.INTENT_START_NETWORK_MONITOR,
                         entryResponse.getAction());
            long locationId = entryResponse.getLongExtra(PoiHandler.EXTRA_LOCATION, -1);
            assertEquals(rowId, locationId);
        }
    }

    /**
     * Exit a location
     */
    public void testExitPoi() {
        Intent exitIntent = makePoiIntent(TRANSIENT);
        startService(exitIntent);

        // verify response Intent
        Intent exitResponse = mTestContext.getStartServiceIntent();
        assertNotNull(exitResponse);
        // klocwork
        if (exitResponse != null) {
            assertEquals(NetworkMonitorService.INTENT_STOP_NETWORK_MONITOR,
                         exitResponse.getAction());
        }
    }

    /**
     * Enter a location with good flag
     */
    public void testPoiGood() {
        // insert a TopLocation with good flag
        Random random = new Random();
        TopLocationTuple tuple = TopLocationTableTest.makeTopLocationTuple(random);
        tuple.put(TopLocationTuple.COL_NETWORK_CONDITION,
                  TopLocationTable.NETWORK_CONDITION_GOOD);
        TopLocationTable table = new TopLocationTable();
        final long rowId = table.insert(mTestContext, tuple);

        // make a POI entry intent to the location we just created above
        Intent entryIntent = makePoiIntent(tuple.getString(TopLocationTuple.COL_POI));
        startService(entryIntent);

        // verify response intent
        Intent entryResponse = mTestContext.getStartServiceIntent();
        assertNull(entryResponse);
    }

    public void testUnimplemented() {
        fail("Test not implemented yet");
        // testPoiBad()
    }
}
