/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 * w04917 (Brian Lee)        2012/07/16   IKCTXTAW-492    Use new intent from LocationSensor
 */

package com.motorola.contextual.smartnetwork.test;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.INTENT_EXTRA_POI;
import static com.motorola.contextual.virtualsensor.locationsensor.Constants.INTENT_POI_EVENT;
import static com.motorola.contextual.virtualsensor.locationsensor.Constants.TRANSIENT;

import java.util.Random;

import android.content.Intent;
import android.test.AndroidTestCase;

import com.motorola.contextual.smartnetwork.PoiHandler;
import com.motorola.contextual.smartnetwork.PoiReceiver;
import com.motorola.contextual.smartnetwork.SmartNetwork;
import com.motorola.contextual.smartnetwork.test.mockobjects.ServiceContext;

public class PoiReceiverTest extends AndroidTestCase {
    private ServiceContext mTestContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTestContext = new ServiceContext(getContext());
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
        assertTrue(SmartNetwork.isEnabled());
        assertNotNull(mTestContext);
    }

    private Intent makePoiIntent(String poi) {
        Intent intent = new Intent(INTENT_POI_EVENT);
        intent.putExtra(INTENT_EXTRA_POI, poi);
        return intent;
    }

    public void testPoiEntry() {
        Random random = new Random();
        final String inputPoi = String.valueOf(random.nextLong());

        Intent poiIntent = makePoiIntent(inputPoi);
        PoiReceiver receiver = new PoiReceiver();
        receiver.onReceive(mTestContext, poiIntent);

        Intent response = mTestContext.getStartServiceIntent();
        assertNotNull(response);
        // klocwork
        if (response == null) return;
        String responsePoi = response.getStringExtra(PoiHandler.EXTRA_POI);
        assertNotNull(responsePoi);
        assertEquals(inputPoi, responsePoi);
    }

    public void testPoiExit() {
        Intent poiIntent = makePoiIntent(TRANSIENT);
        PoiReceiver receiver = new PoiReceiver();
        receiver.onReceive(mTestContext, poiIntent);

        Intent response = mTestContext.getStartServiceIntent();
        assertNotNull(response);
        // klocwork
        if (response == null) return;
        String responsePoi = response.getStringExtra(PoiHandler.EXTRA_POI);
        assertNotNull(responsePoi);
        assertEquals(TRANSIENT, responsePoi);
    }
}
