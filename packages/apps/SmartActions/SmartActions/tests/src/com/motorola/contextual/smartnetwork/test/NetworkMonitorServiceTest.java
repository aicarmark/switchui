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

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Random;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.test.ServiceTestCase;
import android.util.Log;

import com.motorola.android.wrapper.SystemWrapper;
import com.motorola.android.wrapper.TelephonyManagerWrapper;
import com.motorola.contextual.smartnetwork.NetworkMonitorService;
import com.motorola.contextual.smartnetwork.NetworkSession;
import com.motorola.contextual.smartnetwork.PoiHandler;
import com.motorola.contextual.smartnetwork.db.table.DataConnectionStateTable;
import com.motorola.contextual.smartnetwork.db.table.DataConnectionStateTuple;
import com.motorola.contextual.smartnetwork.db.table.MonitorSessionTable;
import com.motorola.contextual.smartnetwork.db.table.MonitorSessionTuple;
import com.motorola.contextual.smartnetwork.db.table.ServiceStateTable;
import com.motorola.contextual.smartnetwork.db.table.ServiceStateTuple;
import com.motorola.contextual.smartnetwork.db.table.SignalStrengthTable;
import com.motorola.contextual.smartnetwork.db.table.SignalStrengthTuple;
import com.motorola.contextual.smartnetwork.db.table.TopLocationTable;
import com.motorola.contextual.smartnetwork.db.table.TopLocationTuple;
import com.motorola.contextual.smartnetwork.test.mockobjects.ServiceContext;
import com.motorola.contextual.smartnetwork.test.mockobjects.TestPowerManager;
import com.motorola.contextual.smartnetwork.test.mockobjects.TestTelephonyManager;

public class NetworkMonitorServiceTest extends ServiceTestCase<NetworkMonitorService> {
    private static final long MAX_SLEEP_TIME = 1000; //ms
    private ServiceContext mTestContext;
    private TestTelephonyManager mTestTelephonyManager;
    private TestPowerManager mTestPowerManager;

    // check-in keys
    private static final String COL_TAG = "tag";
    private static final String COL_ID = "ID";
    private static final String COL_VER = "ver";
    private static final String COL_TIME = "time";

    public NetworkMonitorServiceTest() {
        super(NetworkMonitorService.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTestContext = new ServiceContext(getSystemContext());
        mTestTelephonyManager = new TestTelephonyManager(mTestContext);
        mTestPowerManager = new TestPowerManager(mTestContext);
        SystemWrapper.setMockSystemService(Context.TELEPHONY_SERVICE, mTestTelephonyManager);
        SystemWrapper.setMockSystemService(Context.POWER_SERVICE, mTestPowerManager);
        setContext(mTestContext);
    }

    @Override
    protected void tearDown() throws Exception {
        SystemWrapper.clearMockSystemServices();
        mTestTelephonyManager = null;
        mTestPowerManager = null;
        mTestContext.cleanup();
        mTestContext = null;
        super.tearDown();
    }

    @Override
    public void testServiceTestCaseSetUpProperly() throws Exception {
        super.testServiceTestCaseSetUpProperly();
        assertNotNull(mTestContext);
        assertNotNull(mTestTelephonyManager);
        assertEquals(mTestTelephonyManager,
                     SystemWrapper.getSystemService(mTestContext, Context.TELEPHONY_SERVICE));
        assertNotNull(mTestPowerManager);
        assertEquals(mTestPowerManager,
                     SystemWrapper.getSystemService(mTestContext, Context.POWER_SERVICE));
    }

    /**
     * Test entering of top location
     */
    public void testEnterTopLocation() {
        // insert a TopLocation with verify flag
        Random random = new Random();
        TopLocationTuple tuple = TopLocationTableTest.makeTopLocationTuple(random);
        tuple.put(TopLocationTuple.COL_NETWORK_CONDITION,
                  TopLocationTable.NETWORK_CONDITION_VERIFY);
        TopLocationTable table = new TopLocationTable();
        final long rowId = table.insert(mTestContext, tuple);

        // start the service with the inserted location
        Intent entryIntent = new Intent(
            NetworkMonitorService.INTENT_START_NETWORK_MONITOR);
        entryIntent.putExtra(PoiHandler.EXTRA_LOCATION, rowId);
        startService(entryIntent);

        // verify listeners are registered with telephony manager
        PhoneStateListener listener = mTestTelephonyManager.getListener();
        assertNotNull(listener);
        BroadcastReceiver receiver = mTestContext.getReceiver();
        assertNotNull(receiver);
    }

    /**
     * Test exiting of top location
     */
    public void testExitTopLocation() {
        // insert a TopLocation with verify flag
        Random random = new Random();
        TopLocationTuple tuple = TopLocationTableTest.makeTopLocationTuple(random);
        tuple.put(TopLocationTuple.COL_NETWORK_CONDITION,
                  TopLocationTable.NETWORK_CONDITION_VERIFY);
        TopLocationTable table = new TopLocationTable();
        final long rowId = table.insert(mTestContext, tuple);
        final int rank = tuple.getInt(TopLocationTuple.COL_RANK);
        final long timeSpent = tuple.getLong(TopLocationTuple.COL_TIME_SPENT);
        final long rankUpdated = tuple.getLong(TopLocationTuple.COL_RANK_UPDATED);
        // start the service with the inserted location
        final long timeBefore = System.currentTimeMillis();
        Intent entryIntent = new Intent(
            NetworkMonitorService.INTENT_START_NETWORK_MONITOR);
        entryIntent.putExtra(PoiHandler.EXTRA_LOCATION, rowId);
        entryIntent.putExtra(PoiHandler.EXTRA_RANK, rank);
        entryIntent.putExtra(PoiHandler.EXTRA_TIME_SPENT, timeSpent);
        entryIntent.putExtra(PoiHandler.EXTRA_RANK_UPDATED, rankUpdated);

        startService(entryIntent);

        // verify listeners are registered with telephony manager
        PhoneStateListener listener = mTestTelephonyManager.getListener();
        assertNotNull(listener);
        BroadcastReceiver receiver = mTestContext.getReceiver();
        assertNotNull(receiver);

        // sleep
        try {
            Thread.sleep(MAX_SLEEP_TIME);
        } catch (InterruptedException e) {
            fail("Sleep interrupted");
        }

        // leave the location
        Intent exitIntent = new Intent(
            NetworkMonitorService.INTENT_STOP_NETWORK_MONITOR);
        startService(exitIntent);

        // sleep
        try {
            Thread.sleep(MAX_SLEEP_TIME);
        } catch (InterruptedException e) {
            fail("Sleep interrupted");
        }

        // verify listeners are deregistered
        assertNull(mTestTelephonyManager.getListener());
        assertNull(mTestContext.getReceiver());

        // verify check-in values
        List<ContentValues> checkinValues = mTestContext.getCheckinValues();
        final long timeAfter = System.currentTimeMillis();
        assertNotNull(checkinValues);
        if (checkinValues == null) return;
        // there should be only one check-in row
        assertEquals(1, checkinValues.size());

        // verify the check-in row
        ContentValues cv = checkinValues.get(0);
        Log.d("NetworkTest", cv.toString());
        assertEquals(11, cv.size());
        // tag
        assertEquals(NetworkSession.CHECKIN_TAG, cv.getAsString(COL_TAG));
        // event name
        assertEquals(NetworkSession.EVENT_NAME, cv.getAsString(COL_ID));
        // version
        assertEquals(NetworkSession.CHECKIN_VERSION, cv.getAsString(COL_VER));
        // check-in time
        Long longVal = cv.getAsLong(COL_TIME);
        assertNotNull(longVal);
        if (longVal == null) return;
        assertTrue(timeBefore <= longVal);
        assertTrue(timeAfter >= longVal);

        // start time
        longVal = cv.getAsLong(NetworkSession.EVENT_FIELD_START_TIME);
        assertNotNull(longVal);
        if (longVal == null) return;
        final long startTime = longVal.longValue();
        assertTrue(timeBefore <= longVal);
        assertTrue(timeAfter >= longVal);
        // end time
        longVal = cv.getAsLong(NetworkSession.EVENT_FIELD_END_TIME);
        assertNotNull(longVal);
        if (longVal == null) return;
        final long endTime = longVal.longValue();
        assertTrue(timeBefore <= longVal);
        assertTrue(timeAfter >= longVal);
        assertTrue(timeAfter > timeBefore);
        // duration
        longVal = cv.getAsLong(NetworkSession.EVENT_FIELD_DURATION);
        assertNotNull(longVal);
        if (longVal == null) return;
        assertEquals(endTime - startTime, longVal.longValue());

        // location id
        longVal = cv.getAsLong(NetworkSession.EVENT_FIELD_LOCATION_ID);
        assertNotNull(longVal);
        if (longVal == null) return;
        assertEquals(rowId, longVal.longValue());

        // rank
        longVal = cv.getAsLong(NetworkSession.EVENT_FIELD_RANK);
        assertNotNull(longVal);
        if (longVal == null) return;
        assertEquals(rank, longVal.longValue());

        // rank updated
        longVal = cv.getAsLong(NetworkSession.EVENT_FIELD_RANK_UPDATED);
        assertNotNull(longVal);
        if (longVal == null) return;
        assertEquals(rankUpdated, longVal.longValue());

        // time spent
        longVal = cv.getAsLong(NetworkSession.EVENT_FIELD_TIME_SPENT);
        assertNotNull(longVal);
        if (longVal == null) return;
        assertEquals(timeSpent, longVal.longValue());
    }

    /**
     * Helper function to looks up monitor session id using location id.
     * Expects only 1 matching row.
     *
     * @param locationId location id to look up
     * @return row id of the first matching monitor session row
     */
    private long getMonitorSessionId(long locationId) {
        MonitorSessionTable monitorSessionTable = new MonitorSessionTable();
        String[] columns = { MonitorSessionTuple.COL_ID };
        String selection = MonitorSessionTuple.COL_FK_TOP_LOCATION + " = ?";
        String[] selectionArgs = { String.valueOf(locationId) };
        List<MonitorSessionTuple> monitorTuples = monitorSessionTable.query(mTestContext, columns,
                selection, selectionArgs, null, null, null, null);
        assertNotNull(monitorTuples);
        // klocwork
        if (monitorTuples == null) return -1;

        assertEquals(1, monitorTuples.size());
        MonitorSessionTuple monitorTuple = monitorTuples.get(0);
        assertNotNull(monitorTuple);
        // klocwork
        if (monitorTuple == null) return -1;

        long sessionId = monitorTuple.getLong(MonitorSessionTuple.COL_ID);
        return sessionId;
    }

    /**
     * Test data connection state change in top location
     */
    public void testDataConnectionStateChange() {
        // insert a TopLocation with verify flag
        Random random = new Random();
        TopLocationTuple locationTuple = TopLocationTableTest.makeTopLocationTuple(random);
        locationTuple.put(TopLocationTuple.COL_NETWORK_CONDITION,
                          TopLocationTable.NETWORK_CONDITION_VERIFY);
        TopLocationTable topLocationTable = new TopLocationTable();
        final long locationId = topLocationTable.insert(mTestContext, locationTuple);

        // start the service with the inserted location
        Intent entryIntent = new Intent(
            NetworkMonitorService.INTENT_START_NETWORK_MONITOR);
        entryIntent.putExtra(PoiHandler.EXTRA_LOCATION, locationId);
        startService(entryIntent);

        // verify listeners are registered with telephony manager
        PhoneStateListener listener = mTestTelephonyManager.getListener();
        assertNotNull(listener);
        if (listener == null) return;

        // set DataConnectionState
        final int dataState;
        switch((random.nextInt(4) + 1) % 4) {
        case 0:
            dataState = TelephonyManager.DATA_CONNECTED;
            break;
        case 1:
            dataState = TelephonyManager.DATA_CONNECTING;
            break;
        case 2:
            dataState = TelephonyManager.DATA_DISCONNECTED;
            break;
        default:
            dataState = TelephonyManager.DATA_SUSPENDED;
        }
        final String dataStateName;
        switch(dataState) {
        case TelephonyManager.DATA_DISCONNECTED:
            dataStateName = NetworkSession.DATA_DISCONNECTED;
            break;
        case TelephonyManager.DATA_CONNECTING:
            dataStateName = NetworkSession.DATA_CONNECTING;
            break;
        case TelephonyManager.DATA_CONNECTED:
            dataStateName = NetworkSession.DATA_CONNECTED;
            break;
        case TelephonyManager.DATA_SUSPENDED:
            dataStateName = NetworkSession.DATA_SUSPENDED;
            break;
        default:
            dataStateName = String.valueOf(dataState);
        }

        // set network type;
        final int networkType;
        switch ((random.nextInt(4) +1) % 4) {
        case 0:
            networkType = TelephonyManager.NETWORK_TYPE_CDMA;
            break;
        case 1:
            networkType = TelephonyManager.NETWORK_TYPE_HSDPA;
            break;
        case 2:
            networkType = TelephonyManager.NETWORK_TYPE_LTE;
            break;
        default:
            networkType = TelephonyManager.NETWORK_TYPE_UMTS;
        }

        // invoke data connection state change
        long timeBefore = System.currentTimeMillis();
        listener.onDataConnectionStateChanged(dataState, networkType);
        long timeAfter = System.currentTimeMillis();

        // data insertion happens on background thread so sleep. no good way to wait() here...
        try {
            Thread.sleep(MAX_SLEEP_TIME);
        } catch (InterruptedException e) {
            fail("Sleep interrupted.");
        }

        long sessionId = getMonitorSessionId(locationId);
        assertTrue(sessionId > 0);
        // verify data connection state table
        DataConnectionStateTable dataConnectionTable = new DataConnectionStateTable();
        String selection = DataConnectionStateTuple.COL_FK_MONITOR_SESSION + " = ?";
        String[] selectionArgs = { String.valueOf(sessionId) };
        List<DataConnectionStateTuple> tuples = dataConnectionTable.query(mTestContext, null,
                                                selection, selectionArgs, null, null, null, null);
        assertNotNull(tuples);
        // klocwork
        if (tuples != null) {
            assertEquals(1, tuples.size());
            DataConnectionStateTuple dataTuple = tuples.get(0);
            assertNotNull(dataTuple);
            // klocwork
            if (dataTuple != null) {
                assertEquals(sessionId, dataTuple.getLong(
                                 DataConnectionStateTuple.COL_FK_MONITOR_SESSION));
                assertEquals(dataStateName,
                             dataTuple.getString(DataConnectionStateTuple.COL_STATE));
                String networkTypeName = TelephonyManagerWrapper.getNetworkTypeName(networkType);
                networkTypeName = NetworkMonitorService.formatNetworkType(networkTypeName);
                assertEquals(networkTypeName,
                             dataTuple.getString(DataConnectionStateTuple.COL_NETWORK_TYPE));
                assertTrue(timeBefore <= dataTuple.getLong(DataConnectionStateTuple.COL_TIMESTAMP));
                assertTrue(timeAfter >= dataTuple.getLong(DataConnectionStateTuple.COL_TIMESTAMP));
            }
        }
    }

    /**
     * Test service state change in top location
     */
    public void testServiceStateChange() {
        // insert a TopLocation with verify flag
        Random random = new Random();
        TopLocationTuple locationTuple = TopLocationTableTest.makeTopLocationTuple(random);
        locationTuple.put(TopLocationTuple.COL_NETWORK_CONDITION,
                          TopLocationTable.NETWORK_CONDITION_VERIFY);
        TopLocationTable topLocationTable = new TopLocationTable();
        final long locationId = topLocationTable.insert(mTestContext, locationTuple);

        // start the service with the inserted location
        Intent entryIntent = new Intent(
            NetworkMonitorService.INTENT_START_NETWORK_MONITOR);
        entryIntent.putExtra(PoiHandler.EXTRA_LOCATION, locationId);
        startService(entryIntent);

        // verify listeners are registered with telephony manager
        PhoneStateListener listener = mTestTelephonyManager.getListener();
        assertNotNull(listener);
        // klocwork
        if (listener == null) return;

        // set ServiceState
        final int serviceStateValue;
        switch((random.nextInt(4) + 1) % 4) {
        case 0:
            serviceStateValue = ServiceState.STATE_IN_SERVICE;
            break;
        case 1:
            serviceStateValue = ServiceState.STATE_OUT_OF_SERVICE;
            break;
        case 2:
            serviceStateValue = ServiceState.STATE_EMERGENCY_ONLY;
            break;
        default:
            serviceStateValue = ServiceState.STATE_POWER_OFF;
        }
        ServiceState serviceState = new ServiceState();
        serviceState.setState(serviceStateValue);

        // invoke service state change
        long timeBefore = System.currentTimeMillis();
        listener.onServiceStateChanged(serviceState);
        long timeAfter = System.currentTimeMillis();

        // data insertion happens on background thread so sleep. no good way to wait() here...
        try {
            Thread.sleep(MAX_SLEEP_TIME);
        } catch (InterruptedException e) {
            fail("Sleep interrupted.");
        }

        long sessionId = getMonitorSessionId(locationId);
        assertTrue(sessionId > 0);
        // verify service state table
        ServiceStateTable dataConnectionTable = new ServiceStateTable();
        String selection = ServiceStateTuple.COL_FK_MONITOR_SESSION + " = ?";
        String[] selectionArgs = { String.valueOf(sessionId) };
        List<ServiceStateTuple> tuples = dataConnectionTable.query(mTestContext, null,
                                         selection, selectionArgs, null, null, null, null);
        assertNotNull(tuples);
        // klocwork
        if (tuples != null) {
            assertEquals(1, tuples.size());
            ServiceStateTuple serviceTuple = tuples.get(0);
            assertNotNull(serviceTuple);
            // klocwork
            if (serviceTuple != null) {
                assertEquals(sessionId, serviceTuple.getLong(
                                 ServiceStateTuple.COL_FK_MONITOR_SESSION));
                assertEquals(TelephonyManagerWrapper.getServiceStateName(serviceStateValue),
                             serviceTuple.getString(ServiceStateTuple.COL_STATE));
                assertTrue(timeBefore <= serviceTuple.getLong(ServiceStateTuple.COL_TIMESTAMP));
                assertTrue(timeAfter >= serviceTuple.getLong(ServiceStateTuple.COL_TIMESTAMP));
            }
        }
    }

    /**
     * Creates a SignalStrength object with randomly populated values.
     * Required because SignalStrength constructor is hidden.
     * Uses the following hidden constructor:
     *    public SignalStrength(int gsmSignalStrength, int gsmBitErrorRate, int cdmaDbm,
     *                    int cdmaEcio,int evdoDbm, int evdoEcio, int evdoSnr, boolean gsm)
     * @param random Random object to use
     * @return SignalStrength object
     */
    private static SignalStrength makeSignalStrength(Random random) {
        // Since the constructor for SignalStrength is hidden, use reflections to create it:
        SignalStrength signalStrength = null;
        Constructor<SignalStrength> c = null;
        try {
            c = SignalStrength.class.getDeclaredConstructor(
                    Integer.TYPE, // gsmSignalStrength
                    Integer.TYPE, // gsmBitErrorRate
                    Integer.TYPE, // cdmaDbm
                    Integer.TYPE, // cdmaEcio
                    Integer.TYPE, // evdoDbm
                    Integer.TYPE, // evdoEcio
                    Integer.TYPE, // evdoSnr
                    Boolean.TYPE // gsm
                );
        } catch (Exception e) {
            fail("Unable to get constructor: " + e.toString());
        }
        assertNotNull(c);
        if (c != null) {
            final int gsmSignalStrength = random.nextInt();
            final int gsmBitErrorRate  = random.nextInt();
            final int cdmaDbm = random.nextInt();
            final int cdmaEcio = random.nextInt();
            final int evdoDbm = random.nextInt();
            final int evdoEcio = random.nextInt();
            final int evdoSnr = random.nextInt();
            final boolean gsm = random.nextBoolean();

            try {
                signalStrength = (SignalStrength) c.newInstance(gsmSignalStrength, gsmBitErrorRate,
                                 cdmaDbm, cdmaEcio, evdoDbm, evdoEcio, evdoSnr, gsm);
            } catch (Exception e) {
                fail("Unale to create new instance: " + e.toString());
            }
            assertNotNull(signalStrength);
            // klockwork
            if (signalStrength != null) {
                assertEquals(cdmaDbm, signalStrength.getCdmaDbm());
                assertEquals(cdmaEcio, signalStrength.getCdmaEcio());
                assertEquals(evdoDbm, signalStrength.getEvdoDbm());
                assertEquals(evdoEcio, signalStrength.getEvdoEcio());
                assertEquals(evdoSnr, signalStrength.getEvdoSnr());
                assertEquals(gsmBitErrorRate, signalStrength.getGsmBitErrorRate());
                assertEquals(gsmSignalStrength, signalStrength.getGsmSignalStrength());
                assertEquals(gsm, signalStrength.isGsm());
            }
        }

        return signalStrength;
    }

    /**
     * Test signal strength change in top location
     */
    public void testSignalStrengthChange() {
        // insert a TopLocation with verify flag
        Random random = new Random();
        TopLocationTuple locationTuple = TopLocationTableTest.makeTopLocationTuple(random);
        locationTuple.put(TopLocationTuple.COL_NETWORK_CONDITION,
                          TopLocationTable.NETWORK_CONDITION_VERIFY);
        TopLocationTable topLocationTable = new TopLocationTable();
        final long locationId = topLocationTable.insert(mTestContext, locationTuple);
        assertTrue(locationId > 0);

        // set screen to on so signal strength change will go through
        mTestPowerManager.setScreenOn(true);

        // start the service with the inserted location
        Intent entryIntent = new Intent(
            NetworkMonitorService.INTENT_START_NETWORK_MONITOR);
        entryIntent.putExtra(PoiHandler.EXTRA_LOCATION, locationId);
        startService(entryIntent);

        // verify listeners are registered with telephony manager
        PhoneStateListener listener = mTestTelephonyManager.getListener();
        assertNotNull(listener);
        // klocwork
        if (listener == null) return;

        // set SignalStrength
        final SignalStrength signalStrength = makeSignalStrength(random);
        assertNotNull(signalStrength);
        // klocwork
        if (signalStrength == null) return;

        // invoke signal strength change
        long timeBefore = System.currentTimeMillis();
        listener.onSignalStrengthsChanged(signalStrength);
        long timeAfter = System.currentTimeMillis();

        // data insertion happens on background thread so sleep. no good way to wait() here...
        try {
            Thread.sleep(MAX_SLEEP_TIME);
        } catch (InterruptedException e) {
            fail("Sleep interrupted.");
        }

        long sessionId = getMonitorSessionId(locationId);
        assertTrue(sessionId > 0);
        // verify signal strength
        SignalStrengthTable dataConnectionTable = new SignalStrengthTable();
        String selection = SignalStrengthTuple.COL_FK_MONITOR_SESSION + " = ?";
        String[] selectionArgs = { String.valueOf(sessionId) };
        List<SignalStrengthTuple> tuples = dataConnectionTable.query(mTestContext, null,
                                           selection, selectionArgs, null, null, null, null);
        assertNotNull(tuples);
        // klocwork
        if (tuples == null) return;
        assertEquals(1, tuples.size());
        SignalStrengthTuple signalTuple = tuples.get(0);
        assertNotNull(signalTuple);
        // klocwork
        if (signalTuple == null) return;
        assertEquals(sessionId, signalTuple.getLong(SignalStrengthTuple.COL_FK_MONITOR_SESSION));
        if (signalStrength.isGsm()) {
            assertEquals(NetworkSession.SIGNAL_TYPE_GSM,
                         signalTuple.getString(SignalStrengthTuple.COL_SIGNAL_TYPE));
            assertEquals(String.valueOf(signalStrength.getGsmSignalStrength()),
                         signalTuple.getString(SignalStrengthTuple.COL_SIGNAL_LEVEL));
        } else {
            assertEquals(NetworkSession.SIGNAL_TYPE_CDMA,
                         signalTuple.getString(SignalStrengthTuple.COL_SIGNAL_TYPE));
            assertEquals(String.valueOf(signalStrength.getCdmaDbm()),
                         signalTuple.getString(SignalStrengthTuple.COL_SIGNAL_LEVEL));
        }
        assertTrue(timeBefore <= signalTuple.getLong(SignalStrengthTuple.COL_TIMESTAMP));
        assertTrue(timeAfter >= signalTuple.getLong(SignalStrengthTuple.COL_TIMESTAMP));
    }

    /**
     * Test screen off in top location
     */
    public void testScreenOff() {
        // insert a TopLocation with verify flag
        Random random = new Random();
        TopLocationTuple locationTuple = TopLocationTableTest.makeTopLocationTuple(random);
        locationTuple.put(TopLocationTuple.COL_NETWORK_CONDITION,
                          TopLocationTable.NETWORK_CONDITION_VERIFY);
        TopLocationTable topLocationTable = new TopLocationTable();
        final long locationId = topLocationTable.insert(mTestContext, locationTuple);

        // start the service with the inserted location
        Intent entryIntent = new Intent(
            NetworkMonitorService.INTENT_START_NETWORK_MONITOR);
        entryIntent.putExtra(PoiHandler.EXTRA_LOCATION, locationId);
        startService(entryIntent);

        // verify screen receiver is registered
        BroadcastReceiver screenReceiver = mTestContext.getReceiver();
        assertNotNull(screenReceiver);
        // klocwork
        if (screenReceiver == null) return;

        // send screen off
        long timeBefore = System.currentTimeMillis();
        screenReceiver.onReceive(mTestContext, new Intent(Intent.ACTION_SCREEN_OFF));
        long timeAfter = System.currentTimeMillis();

        // data insertion happens on background thread so sleep. no good way to wait() here...
        try {
            Thread.sleep(MAX_SLEEP_TIME);
        } catch (InterruptedException e) {
            fail("Sleep interrupted.");
        }

        long sessionId = getMonitorSessionId(locationId);
        assertTrue(sessionId > 0);
        // verify screen off marker
        SignalStrengthTable dataConnectionTable = new SignalStrengthTable();
        String selection = SignalStrengthTuple.COL_FK_MONITOR_SESSION + " = ?";
        String[] selectionArgs = { String.valueOf(sessionId) };
        List<SignalStrengthTuple> tuples = dataConnectionTable.query(mTestContext, null,
                                           selection, selectionArgs, null, null, null, null);
        assertNotNull(tuples);
        // klocwork
        if (tuples == null) return;
        assertEquals(1, tuples.size());
        SignalStrengthTuple signalTuple = tuples.get(0);
        assertNotNull(signalTuple);
        assertEquals(sessionId, signalTuple.getLong(SignalStrengthTuple.COL_FK_MONITOR_SESSION));
        assertEquals(NetworkSession.SIGNAL_SCREEN_OFF,
                     signalTuple.getString(SignalStrengthTuple.COL_SIGNAL_TYPE));
        assertEquals(NetworkSession.SIGNAL_LEVEL_UNKNOWN,
                     signalTuple.getString(SignalStrengthTuple.COL_SIGNAL_LEVEL));
        assertTrue(timeBefore <= signalTuple.getLong(SignalStrengthTuple.COL_TIMESTAMP));
        assertTrue(timeAfter >= signalTuple.getLong(SignalStrengthTuple.COL_TIMESTAMP));
    }

    /**
     * Test screen on in top location
     */
    public void testScreenOn() {
        // insert a TopLocation with verify flag
        Random random = new Random();
        TopLocationTuple locationTuple = TopLocationTableTest.makeTopLocationTuple(random);
        locationTuple.put(TopLocationTuple.COL_NETWORK_CONDITION,
                          TopLocationTable.NETWORK_CONDITION_VERIFY);
        TopLocationTable topLocationTable = new TopLocationTable();
        final long locationId = topLocationTable.insert(mTestContext, locationTuple);

        // start the service with the inserted location
        Intent entryIntent = new Intent(
            NetworkMonitorService.INTENT_START_NETWORK_MONITOR);
        entryIntent.putExtra(PoiHandler.EXTRA_LOCATION, locationId);
        startService(entryIntent);

        // verify screen receiver is registered
        BroadcastReceiver screenReceiver = mTestContext.getReceiver();
        assertNotNull(screenReceiver);
        // klocwork
        if (screenReceiver == null) return;

        // send screen on
        long timeBefore = System.currentTimeMillis();
        screenReceiver.onReceive(mTestContext, new Intent(Intent.ACTION_SCREEN_ON));
        long timeAfter = System.currentTimeMillis();

        // data insertion happens on background thread so sleep. no good way to wait() here...
        try {
            Thread.sleep(MAX_SLEEP_TIME);
        } catch (InterruptedException e) {
            fail("Sleep interrupted.");
        }

        long sessionId = getMonitorSessionId(locationId);
        assertTrue(sessionId > 0);
        // verify screen on marker
        SignalStrengthTable dataConnectionTable = new SignalStrengthTable();
        String selection = SignalStrengthTuple.COL_FK_MONITOR_SESSION + " = ?";
        String[] selectionArgs = { String.valueOf(sessionId) };
        List<SignalStrengthTuple> tuples = dataConnectionTable.query(mTestContext, null,
                                           selection, selectionArgs, null, null, null, null);
        assertNotNull(tuples);
        // klocwork
        if (tuples == null) return;
        assertEquals(1, tuples.size());
        SignalStrengthTuple signalTuple = tuples.get(0);
        assertNotNull(signalTuple);
        assertEquals(sessionId, signalTuple.getLong(SignalStrengthTuple.COL_FK_MONITOR_SESSION));
        assertEquals(NetworkSession.SIGNAL_SCREEN_ON,
                     signalTuple.getString(SignalStrengthTuple.COL_SIGNAL_TYPE));
        assertEquals(NetworkSession.SIGNAL_LEVEL_UNKNOWN,
                     signalTuple.getString(SignalStrengthTuple.COL_SIGNAL_LEVEL));
        assertTrue(timeBefore <= signalTuple.getLong(SignalStrengthTuple.COL_TIMESTAMP));
        assertTrue(timeAfter >= signalTuple.getLong(SignalStrengthTuple.COL_TIMESTAMP));
    }
}
