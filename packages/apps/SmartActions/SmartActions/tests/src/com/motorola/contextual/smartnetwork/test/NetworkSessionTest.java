/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/07/11   IKCTXTAW-487    Initial version
 */

package com.motorola.contextual.smartnetwork.test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.TelephonyManager;
import android.test.AndroidTestCase;
import android.text.format.DateUtils;

import com.motorola.android.wrapper.TelephonyManagerWrapper;
import com.motorola.contextual.smartnetwork.NetworkMonitorService;
import com.motorola.contextual.smartnetwork.NetworkSession;
import com.motorola.contextual.smartnetwork.NetworkSession.BounceStat;
import com.motorola.contextual.smartnetwork.NetworkSession.NetworkStats;
import com.motorola.contextual.smartnetwork.NetworkSession.SignalStrengthValue;
import com.motorola.contextual.smartnetwork.db.SmartNetworkDbSchema;
import com.motorola.contextual.smartnetwork.db.SmartNetworkSqliteDb;
import com.motorola.contextual.smartnetwork.db.table.DataConnectionStateTable;
import com.motorola.contextual.smartnetwork.db.table.DataConnectionStateTuple;
import com.motorola.contextual.smartnetwork.db.table.MonitorSessionTable;
import com.motorola.contextual.smartnetwork.db.table.MonitorSessionTuple;
import com.motorola.contextual.smartnetwork.db.table.SignalStrengthTable;
import com.motorola.contextual.smartnetwork.db.table.SignalStrengthTuple;
import com.motorola.contextual.smartnetwork.db.table.TopLocationTable;
import com.motorola.contextual.smartnetwork.db.table.TopLocationTuple;
import com.motorola.contextual.smartnetwork.test.mockobjects.DbContext;

public class NetworkSessionTest extends AndroidTestCase implements SmartNetworkDbSchema {
    private DbContext mTestContext;

    // Telephony manager's network type is from 0~15
    private static final int MAX_NETWORK_TYPE = 16;
    private static final int MAX_RANDOM_TUPLES = 30;
    private static final long TIME_INTERVAL = 30 * DateUtils.SECOND_IN_MILLIS;

    // the base sqlite db
    private SQLiteDatabase mBaseDb;
    private NetworkSession mSession;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTestContext = new DbContext(getContext());
        mBaseDb = new SmartNetworkSqliteDb(mTestContext).getWritableDatabase();
        mSession = new NetworkSession();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mBaseDb.close();
        mBaseDb = null;

        // all db references must be released before deleting the db files
        mTestContext.cleanup();
        mTestContext = null;
        mSession = null;
    }

    @Override
    public void testAndroidTestCaseSetupProperly() {
        super.testAndroidTestCaseSetupProperly();
        assertNotNull(mTestContext);
        assertNotNull(mBaseDb);
        assertNotNull(mSession);
    }

    public void testLogSessionStart() {
        // first insert TopLocation row to reference
        Random random = new Random();
        TopLocationTuple topTuple = TopLocationTableTest.makeTopLocationTuple(random);
        TopLocationTable topTable = new TopLocationTable();
        final long locationId = topTable.insert(mTestContext, topTuple);
        assertTrue(locationId > 0);

        final long startTime = System.currentTimeMillis();
        mSession.mLocationId = locationId;
        mSession.mStartTime = startTime;
        mSession.mEndTime = 0;
        mSession.mTimeSpent = topTuple.getLong(COL_TIME_SPENT);
        mSession.mRank = topTuple.getInt(COL_RANK);;
        mSession.mRankUpdated = topTuple.getLong(COL_RANK_UPDATED);

        mSession.logSessionStart(mTestContext);
        assertTrue(mSession.mSessionId > 0);
        final long sessionId = mSession.mSessionId;

        // verify MonitorSession table
        String selection = COL_ID + " = ?";
        String[] selectionArgs = { String.valueOf(sessionId) };
        MonitorSessionTable monitorTable = new MonitorSessionTable();
        List<MonitorSessionTuple> monitorTuples = monitorTable.query(mTestContext, null, selection,
                selectionArgs, null, null, null, null);
        assertNotNull(monitorTuples);
        // klocwork
        if (monitorTuples == null) return;
        assertEquals(1, monitorTuples.size());
        MonitorSessionTuple monitorTuple = monitorTuples.get(0);
        assertNotNull(monitorTuple);
        // klocwork
        if (monitorTuple == null) return;

        assertEquals(sessionId, monitorTuple.getLong(COL_ID));
        assertEquals(locationId, monitorTuple.getLong(COL_FK_TOP_LOCATION));
        assertEquals(startTime, monitorTuple.getLong(COL_START_TIME));
        assertEquals(0, monitorTuple.getLong(COL_END_TIME));
    }

    public void testLogSessionEnd() {
        // first insert TopLocation row to reference
        Random random = new Random();
        TopLocationTuple topTuple = TopLocationTableTest.makeTopLocationTuple(random);
        TopLocationTable topTable = new TopLocationTable();
        final long locationId = topTable.insert(mTestContext, topTuple);
        assertTrue(locationId > 0);
        final long startTime = System.currentTimeMillis();
        final long endTime = startTime + random.nextInt(1000000) + 1;
        // log session start first
        mSession.mLocationId = locationId;
        mSession.mStartTime = startTime;
        mSession.mEndTime = 0;
        mSession.mTimeSpent = topTuple.getLong(COL_TIME_SPENT);
        mSession.mRank = topTuple.getInt(COL_RANK);;
        mSession.mRankUpdated = topTuple.getLong(COL_RANK_UPDATED);

        mSession.logSessionStart(mTestContext);
        assertTrue(mSession.mSessionId > 0);
        final long sessionId = mSession.mSessionId;

        // log session end now
        mSession.mEndTime = endTime;
        mSession.logSessionEnd(mTestContext);

        // verify MonitorSession table
        String selection = COL_ID + " = ?";
        String[] selectionArgs = { String.valueOf(sessionId) };
        MonitorSessionTable monitorTable = new MonitorSessionTable();
        List<MonitorSessionTuple> monitorTuples = monitorTable.query(mTestContext, null, selection,
                selectionArgs, null, null, null, null);
        assertNotNull(monitorTuples);
        // klocwork
        if (monitorTuples == null) return;
        assertEquals(1, monitorTuples.size());
        MonitorSessionTuple monitorTuple = monitorTuples.get(0);
        assertNotNull(monitorTuple);
        // klocwork
        if (monitorTuple == null) return;

        assertEquals(sessionId, monitorTuple.getLong(COL_ID));
        assertEquals(locationId, monitorTuple.getLong(COL_FK_TOP_LOCATION));
        assertEquals(startTime, monitorTuple.getLong(COL_START_TIME));
        assertEquals(endTime, monitorTuple.getLong(COL_END_TIME));
    }

    public void testClear() {
        Random random = new Random();
        mSession.mSessionId = random.nextLong();
        mSession.mStartTime = random.nextLong();
        mSession.mEndTime = random.nextLong();
        mSession.mLocationId = random.nextLong();
        mSession.mTimeSpent = random.nextLong();
        mSession.mRankUpdated = random.nextLong();
        mSession.mRank = random.nextInt();

        mSession.clear();

        assertEquals(0, mSession.mSessionId);
        assertEquals(0, mSession.mStartTime);
        assertEquals(0, mSession.mEndTime);
        assertEquals(0, mSession.mLocationId);
        assertEquals(0, mSession.mTimeSpent);
        assertEquals(0, mSession.mRankUpdated);
        assertEquals(0, mSession.mRank);
    }

    /**
     * Helper function to generate a random network type
     * @param random
     * @return properly formatted random network type
     */
    private String getRandomNetworkType(Random random) {
        String networkType = null;
        int randomType = random.nextInt(MAX_NETWORK_TYPE);
        networkType = TelephonyManagerWrapper.getNetworkTypeName(randomType);
        assertNotNull(networkType);
        networkType = NetworkMonitorService.formatNetworkType(networkType);
        assertNotNull(networkType);
        return networkType;
    }

    /**
     * Helper function to map int network type to NetworkSession String network type
     * @param type network type from TelephonyManager
     * @return String representation of network type formatted for NetworkSession
     */
    private String getNetworkType(int type) {
        String networkType = TelephonyManagerWrapper.getNetworkTypeName(type);
        networkType = NetworkMonitorService.formatNetworkType(networkType);
        assertNotNull(networkType);
        return networkType;
    }

    /**
     * Tests NetworkSession.getDistinctDataTypes()
     * Expected Result: Only distinct data types for the current session is returned
     */
    public void testGetDistinctDataTypes() {
        // use reflections to get to the private method
        Method method = null;
        try {
            method = NetworkSession.class.getDeclaredMethod("getDistinctDataTypes", Context.class);
            method.setAccessible(true);
        } catch (Exception e) {
            fail("Unable to find method.");
            return;
        }

        Random random = new Random();
        // populate with random data connection types with unrelated session id
        int max = random.nextInt(MAX_RANDOM_TUPLES) + 1;
        DataConnectionStateTable dataTable = new DataConnectionStateTable();
        DataConnectionStateTuple tempTuple = null;
        for (int i = 0; i < max; i++) {
            tempTuple = DataConnectionStateTableTest.makeDataConnectionStateTuple(random, mBaseDb);
            tempTuple.put(COL_NETWORK_TYPE, getRandomNetworkType(random));
            assertTrue(dataTable.insert(mTestContext, tempTuple) > 0);
        }

        // populate with random data connection types with related session id
        max = random.nextInt(MAX_RANDOM_TUPLES) + 1;
        HashMap<String, Boolean> typeMap = new HashMap<String, Boolean>();
        tempTuple = DataConnectionStateTableTest.makeDataConnectionStateTuple(random, mBaseDb);
        final long sessionId = tempTuple.getLong(COL_FK_MONITOR_SESSION);
        mSession.mSessionId = sessionId;
        assertTrue(sessionId > 0);
        String randomType = null;
        for (int i = 0; i < max; i++) {
            randomType = getRandomNetworkType(random);
            tempTuple.put(COL_NETWORK_TYPE, randomType);
            assertTrue(dataTable.insert(mTestContext, tempTuple) > 0);
            // remember all the different types we put in for verifications
            typeMap.put(randomType, false);
        }

        // run method
        String[] dataTypes = null;
        try {
            dataTypes = (String[]) method.invoke(mSession, mTestContext);
        } catch (Exception e) {
            fail("Unable to invoke method: " + e.toString());
        }

        // verify that results only contain distinct data types with the current session id
        assertNotNull(dataTypes);
        assertTrue(dataTypes.length > 0);
        for (int i = 0; i < dataTypes.length; i++) {
            assertNotNull(dataTypes[i]);
            if (typeMap.containsKey(dataTypes[i])) {
                // mark that it was found
                typeMap.put(dataTypes[i], true);
            } else {
                fail(dataTypes[i] + " was never inserted.");
            }
        }

        // check that all distinct types have been marked
        Set<Map.Entry<String, Boolean>> entrySet = typeMap.entrySet();
        assertNotNull(entrySet);
        if (entrySet == null) return;
        for (Map.Entry<String, Boolean> entry : entrySet) {
            if (entry.getValue() == false) {
                fail(entry.getKey() + " was not found.");
            }
        }
    }

    /**
     * Tests getNetworkStats() functionality with a simple data set.
     * State            Type    Signal Changes
     * CONNECTED        LTE     great/good/mid/poor
     * DISCONNECTED     LTE
     * CONNECTED        3G      great
     * DISCONNECTED     3G
     * CONNECTED        LTE     great
     */
    public void testGetNetworkStats() {
        // use reflections to get to the private method
        Method method = null;
        try {
            method = NetworkSession.class.getDeclaredMethod("getNetworkStats", Context.class);
            method.setAccessible(true);
        } catch (Exception e) {
            fail("Unable to find method.");
            return;
        }

        Random random = new Random();
        final long startTime = System.currentTimeMillis();
        // monitor session generation
        MonitorSessionTable monitorTable = new MonitorSessionTable();
        MonitorSessionTuple monitorTuple =
            MonitorSessionTableTest.makeMonitorSessionTuple(random, mBaseDb);
        monitorTuple.put(COL_START_TIME, startTime);
        monitorTuple.put(COL_END_TIME, 0);
        final long sessionId = monitorTable.insert(mTestContext, monitorTuple);
        assertTrue(sessionId > 0);
        mSession.mSessionId = sessionId;

        // data connection state tuple initialization
        DataConnectionStateTable dataTable = new DataConnectionStateTable();
        DataConnectionStateTuple dataTuple = new DataConnectionStateTuple();
        dataTuple.put(COL_FK_MONITOR_SESSION, sessionId);

        // signal strength tuple initialization
        SignalStrengthTable signalTable = new SignalStrengthTable();
        SignalStrengthTuple signalTuple = new SignalStrengthTuple();
        signalTuple.put(COL_FK_MONITOR_SESSION, sessionId);
        signalTuple.put(COL_SIGNAL_TYPE, NetworkSession.SIGNAL_TYPE_CDMA);

        final String networkTypeLte = getNetworkType(TelephonyManager.NETWORK_TYPE_LTE);
        final String networkType3g = getNetworkType(TelephonyManager.NETWORK_TYPE_EHRPD);

        /*
         * LTE Connection
         */
        long timestamp = startTime;
        long durationLte = 0;
        dataTuple.put(COL_STATE, NetworkSession.DATA_CONNECTED);
        dataTuple.put(COL_NETWORK_TYPE, networkTypeLte);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // great signal
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_GREAT);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // good signal
        timestamp += TIME_INTERVAL;
        durationLte += TIME_INTERVAL;
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_GOOD);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        signalTable.insert(mTestContext, signalTuple);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // mid signal
        timestamp += TIME_INTERVAL;
        durationLte += TIME_INTERVAL;
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_MID);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        signalTable.insert(mTestContext, signalTuple);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // poor signal
        timestamp += TIME_INTERVAL;
        durationLte += TIME_INTERVAL;
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_POOR);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        signalTable.insert(mTestContext, signalTuple);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // disconnect LTE
        timestamp += TIME_INTERVAL;
        durationLte += TIME_INTERVAL;
        dataTuple.put(COL_STATE, NetworkSession.DATA_DISCONNECTED);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        /*
         * 3G connection
         */
        long duration3g = 0;
        dataTuple.put(COL_STATE, NetworkSession.DATA_CONNECTED);
        dataTuple.put(COL_NETWORK_TYPE, networkType3g);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // great signal
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_GREAT);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // disconnect 3G
        timestamp += TIME_INTERVAL;
        duration3g += TIME_INTERVAL;
        dataTuple.put(COL_STATE, NetworkSession.DATA_DISCONNECTED);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        /*
         * LTE
         */
        dataTuple.put(COL_STATE, NetworkSession.DATA_CONNECTED);
        dataTuple.put(COL_NETWORK_TYPE, networkTypeLte);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // great signal
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_GOOD);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // update timestamp before ending session
        timestamp += TIME_INTERVAL;
        durationLte += TIME_INTERVAL;

        // update end time for MonitorSessionTuple
        monitorTuple = new MonitorSessionTuple();
        final long endTime = timestamp;
        mSession.mEndTime = endTime;
        monitorTuple.put(COL_END_TIME, endTime);
        String whereClause = COL_ID + " = " + sessionId;
        monitorTable.update(mTestContext, monitorTuple, whereClause, null);

        // invoke method
        Collection<NetworkStats> networkStats = null;
        try {
            networkStats = (Collection<NetworkStats>) method.invoke(mSession, mTestContext);
        } catch (Exception e) {
            fail("Unable to invoke method: " + e.toString());
        }
        assertNotNull(networkStats);
        if (networkStats == null) return;

        // verify result. Should be only two network stats, LTE and EHRPD
        assertEquals(2, networkStats.size());

        // check the NetworkStat for each type
        int indexGreat = SignalStrengthValue.SIG_GREAT.ordinal();
        int indexGood = SignalStrengthValue.SIG_GOOD.ordinal();
        int indexMid = SignalStrengthValue.SIG_MID.ordinal();
        int indexPoor = SignalStrengthValue.SIG_POOR.ordinal();
        int indexNone = SignalStrengthValue.SIG_NONE.ordinal();
        for (NetworkStats stat : networkStats) {
            assertNotNull(stat);
            if (stat == null) return;
            if (networkTypeLte.equals(stat.mNetworkType)) {
                assertEquals(durationLte, stat.mTotalTime);
                assertEquals(2, stat.mCount);
                assertNotNull(stat.mSignalStrength);
                if (stat.mSignalStrength == null) return;
                // 1 great, 2 good, 1 mid, 1 poor
                assertEquals(TIME_INTERVAL, stat.mSignalStrength[indexGreat]);
                assertEquals(TIME_INTERVAL * 2, stat.mSignalStrength[indexGood]);
                assertEquals(TIME_INTERVAL, stat.mSignalStrength[indexMid]);
                assertEquals(TIME_INTERVAL, stat.mSignalStrength[indexPoor]);
                assertEquals(0, stat.mSignalStrength[indexNone]);
            } else if (networkType3g.equals(stat.mNetworkType)) {
                assertEquals(duration3g, stat.mTotalTime);
                assertEquals(1, stat.mCount);
                assertNotNull(stat.mSignalStrength);
                if (stat.mSignalStrength == null) return;
                // 1 great
                assertEquals(TIME_INTERVAL, stat.mSignalStrength[indexGreat]);
                assertEquals(0, stat.mSignalStrength[indexGood]);
                assertEquals(0, stat.mSignalStrength[indexMid]);
                assertEquals(0, stat.mSignalStrength[indexPoor]);
                assertEquals(0, stat.mSignalStrength[indexNone]);
            } else {
                fail("Unexpected network type: " + stat.mNetworkType);
            }
        }
    }

    /**
     * Tests getNetworkStats() functionality with no disconnect message
     * State            Type    Signal Changes
     * CONNECTED        LTE     great
     * CONNECTED        LTE
     * CONNECTED        LTE     good/poor
     * CONNECTED        3G      good
     * CONNECTED        LTE     mid
     */
    public void testGetNetworkStatsNoDisconnect() {
        // use reflections to get to the private method
        Method method = null;
        try {
            method = NetworkSession.class.getDeclaredMethod("getNetworkStats", Context.class);
            method.setAccessible(true);
        } catch (Exception e) {
            fail("Unable to find method.");
            return;
        }

        Random random = new Random();
        final long startTime = System.currentTimeMillis();
        // monitor session generation
        MonitorSessionTable monitorTable = new MonitorSessionTable();
        MonitorSessionTuple monitorTuple =
            MonitorSessionTableTest.makeMonitorSessionTuple(random, mBaseDb);
        monitorTuple.put(COL_START_TIME, startTime);
        monitorTuple.put(COL_END_TIME, 0);
        final long sessionId = monitorTable.insert(mTestContext, monitorTuple);
        assertTrue(sessionId > 0);
        mSession.mSessionId = sessionId;

        // data connection state tuple initialization
        DataConnectionStateTable dataTable = new DataConnectionStateTable();
        DataConnectionStateTuple dataTuple = new DataConnectionStateTuple();
        dataTuple.put(COL_FK_MONITOR_SESSION, sessionId);

        // signal strength tuple initialization
        SignalStrengthTable signalTable = new SignalStrengthTable();
        SignalStrengthTuple signalTuple = new SignalStrengthTuple();
        signalTuple.put(COL_FK_MONITOR_SESSION, sessionId);
        signalTuple.put(COL_SIGNAL_TYPE, NetworkSession.SIGNAL_TYPE_CDMA);

        final String networkTypeLte = getNetworkType(TelephonyManager.NETWORK_TYPE_LTE);
        final String networkType3g = getNetworkType(TelephonyManager.NETWORK_TYPE_EHRPD);

        /*
         * LTE Connection
         */
        long timestamp = startTime;
        long durationLte = 0;
        dataTuple.put(COL_STATE, NetworkSession.DATA_CONNECTED);
        dataTuple.put(COL_NETWORK_TYPE, networkTypeLte);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // great signal
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_GREAT);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // connect LTE
        timestamp += TIME_INTERVAL;
        durationLte += TIME_INTERVAL;
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // connect LTE
        timestamp += TIME_INTERVAL;
        durationLte += TIME_INTERVAL;
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // good signal
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_GOOD);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // poor signal
        timestamp += TIME_INTERVAL;
        durationLte += TIME_INTERVAL;
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_POOR);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        timestamp += TIME_INTERVAL;
        durationLte += TIME_INTERVAL;

        /*
         * 3G connection
         */
        long duration3g = 0;
        dataTuple.put(COL_STATE, NetworkSession.DATA_CONNECTED);
        dataTuple.put(COL_NETWORK_TYPE, networkType3g);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // good signal
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_GOOD);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        timestamp += TIME_INTERVAL;
        duration3g += TIME_INTERVAL;

        /*
         * LTE
         */
        dataTuple.put(COL_STATE, NetworkSession.DATA_CONNECTED);
        dataTuple.put(COL_NETWORK_TYPE, networkTypeLte);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // mid signal
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_MID);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // update timestamp before ending session
        timestamp += TIME_INTERVAL;
        durationLte += TIME_INTERVAL;

        // update end time for MonitorSessionTuple
        monitorTuple = new MonitorSessionTuple();
        final long endTime = timestamp;
        mSession.mEndTime = endTime;
        monitorTuple.put(COL_END_TIME, endTime);
        String whereClause = COL_ID + " = " + sessionId;
        monitorTable.update(mTestContext, monitorTuple, whereClause, null);

        // invoke method
        Collection<NetworkStats> networkStats = null;
        try {
            networkStats = (Collection<NetworkStats>) method.invoke(mSession, mTestContext);
        } catch (Exception e) {
            fail("Unable to invoke method: " + e.toString());
        }
        assertNotNull(networkStats);
        if (networkStats == null) return;

        // verify result. Should be only two network stats, LTE and EHRPD
        assertEquals(2, networkStats.size());

        // check the NetworkStat for each type
        int indexGreat = SignalStrengthValue.SIG_GREAT.ordinal();
        int indexGood = SignalStrengthValue.SIG_GOOD.ordinal();
        int indexMid = SignalStrengthValue.SIG_MID.ordinal();
        int indexPoor = SignalStrengthValue.SIG_POOR.ordinal();
        int indexNone = SignalStrengthValue.SIG_NONE.ordinal();
        for (NetworkStats stat : networkStats) {
            assertNotNull(stat);
            if (stat == null) return;
            if (networkTypeLte.equals(stat.mNetworkType)) {
                assertEquals(durationLte, stat.mTotalTime);
                assertEquals(2, stat.mCount);
                assertNotNull(stat.mSignalStrength);
                if (stat.mSignalStrength == null) return;
                // 2 great, 1 good, 1 mid, 1 poor
                assertEquals(TIME_INTERVAL * 2, stat.mSignalStrength[indexGreat]);
                assertEquals(TIME_INTERVAL, stat.mSignalStrength[indexGood]);
                assertEquals(TIME_INTERVAL, stat.mSignalStrength[indexMid]);
                assertEquals(TIME_INTERVAL, stat.mSignalStrength[indexPoor]);
                assertEquals(0, stat.mSignalStrength[indexNone]);
            } else if (networkType3g.equals(stat.mNetworkType)) {
                assertEquals(duration3g, stat.mTotalTime);
                assertEquals(1, stat.mCount);
                assertNotNull(stat.mSignalStrength);
                if (stat.mSignalStrength == null) return;
                // 1 good
                assertEquals(0, stat.mSignalStrength[indexGreat]);
                assertEquals(TIME_INTERVAL, stat.mSignalStrength[indexGood]);
                assertEquals(0, stat.mSignalStrength[indexMid]);
                assertEquals(0, stat.mSignalStrength[indexPoor]);
                assertEquals(0, stat.mSignalStrength[indexNone]);
            } else {
                fail("Unexpected network type: " + stat.mNetworkType);
            }
        }
    }

    /**
     * Tests getNetworkStats() functionality with screen on/off change.
     * Signal strength change during screen off state should be ignored.
     * State            Type    Signal Changes
     * CONNECTED        LTE     great/screen off/good/great/screen on/mid
     * DISCONNECTED     LTE
     * CONNECTED        3G      great/screen off/screen on/good
     */
    public void testGetNetworkStatsWithScreenChange() {
        // use reflections to get to the private method
        Method method = null;
        try {
            method = NetworkSession.class.getDeclaredMethod("getNetworkStats", Context.class);
            method.setAccessible(true);
        } catch (Exception e) {
            fail("Unable to find method.");
            return;
        }

        Random random = new Random();
        final long startTime = System.currentTimeMillis();
        // monitor session generation
        MonitorSessionTable monitorTable = new MonitorSessionTable();
        MonitorSessionTuple monitorTuple =
            MonitorSessionTableTest.makeMonitorSessionTuple(random, mBaseDb);
        monitorTuple.put(COL_START_TIME, startTime);
        monitorTuple.put(COL_END_TIME, 0);
        final long sessionId = monitorTable.insert(mTestContext, monitorTuple);
        assertTrue(sessionId > 0);
        mSession.mSessionId = sessionId;

        // data connection state tuple initialization
        DataConnectionStateTable dataTable = new DataConnectionStateTable();
        DataConnectionStateTuple dataTuple = new DataConnectionStateTuple();
        dataTuple.put(COL_FK_MONITOR_SESSION, sessionId);

        // signal strength tuple initialization
        SignalStrengthTable signalTable = new SignalStrengthTable();
        SignalStrengthTuple signalTuple = new SignalStrengthTuple();
        signalTuple.put(COL_FK_MONITOR_SESSION, sessionId);
        signalTuple.put(COL_SIGNAL_TYPE, NetworkSession.SIGNAL_TYPE_CDMA);

        final String networkTypeLte = getNetworkType(TelephonyManager.NETWORK_TYPE_LTE);
        final String networkType3g = getNetworkType(TelephonyManager.NETWORK_TYPE_EHRPD);

        /*
         * LTE Connection
         */
        long timestamp = startTime;
        long durationLte = 0;
        dataTuple.put(COL_STATE, NetworkSession.DATA_CONNECTED);
        dataTuple.put(COL_NETWORK_TYPE, networkTypeLte);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // great signal
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_GREAT);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // screen off
        timestamp += TIME_INTERVAL;
        durationLte += TIME_INTERVAL;
        signalTuple.put(COL_SIGNAL_TYPE, NetworkSession.SIGNAL_SCREEN_OFF);
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.SIGNAL_LEVEL_UNKNOWN);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        signalTable.insert(mTestContext, signalTuple);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // good (screen off)
        timestamp += TIME_INTERVAL;
        durationLte += TIME_INTERVAL;
        signalTuple.put(COL_SIGNAL_TYPE, NetworkSession.SIGNAL_TYPE_CDMA);
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_GOOD);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        signalTable.insert(mTestContext, signalTuple);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // great (screen off)
        timestamp += TIME_INTERVAL;
        durationLte += TIME_INTERVAL;
        signalTuple.put(COL_SIGNAL_TYPE, NetworkSession.SIGNAL_TYPE_CDMA);
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_GREAT);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        signalTable.insert(mTestContext, signalTuple);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // screen on
        timestamp += TIME_INTERVAL;
        durationLte += TIME_INTERVAL;
        signalTuple.put(COL_SIGNAL_TYPE, NetworkSession.SIGNAL_SCREEN_ON);
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.SIGNAL_LEVEL_UNKNOWN);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        signalTable.insert(mTestContext, signalTuple);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // mid signal
        timestamp += TIME_INTERVAL;
        durationLte += TIME_INTERVAL;
        signalTuple.put(COL_SIGNAL_TYPE, NetworkSession.SIGNAL_TYPE_CDMA);
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_MID);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        signalTable.insert(mTestContext, signalTuple);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // disconnect LTE
        timestamp += TIME_INTERVAL;
        durationLte += TIME_INTERVAL;
        dataTuple.put(COL_STATE, NetworkSession.DATA_DISCONNECTED);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        /*
         * 3G connection
         */
        long duration3g = 0;
        dataTuple.put(COL_STATE, NetworkSession.DATA_CONNECTED);
        dataTuple.put(COL_NETWORK_TYPE, networkType3g);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // great signal
        signalTuple.put(COL_SIGNAL_TYPE, NetworkSession.SIGNAL_TYPE_CDMA);
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_GREAT);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // screen off
        timestamp += TIME_INTERVAL;
        duration3g += TIME_INTERVAL;
        signalTuple.put(COL_SIGNAL_TYPE, NetworkSession.SIGNAL_SCREEN_OFF);
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.SIGNAL_LEVEL_UNKNOWN);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        signalTable.insert(mTestContext, signalTuple);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // screen on
        timestamp += TIME_INTERVAL;
        duration3g += TIME_INTERVAL;
        signalTuple.put(COL_SIGNAL_TYPE, NetworkSession.SIGNAL_SCREEN_ON);
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.SIGNAL_LEVEL_UNKNOWN);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        signalTable.insert(mTestContext, signalTuple);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // good
        timestamp += TIME_INTERVAL;
        duration3g += TIME_INTERVAL;
        signalTuple.put(COL_SIGNAL_TYPE, NetworkSession.SIGNAL_TYPE_CDMA);
        signalTuple.put(COL_SIGNAL_LEVEL, NetworkSession.CDMA_SIGNAL_GOOD);
        signalTuple.put(COL_TIMESTAMP, timestamp);
        signalTable.insert(mTestContext, signalTuple);
        assertTrue(signalTable.insert(mTestContext, signalTuple) > 0);

        // update timestamp before ending session
        timestamp += TIME_INTERVAL;
        duration3g += TIME_INTERVAL;

        // update end time for MonitorSessionTuple
        monitorTuple = new MonitorSessionTuple();
        final long endTime = timestamp;
        mSession.mEndTime = endTime;
        monitorTuple.put(COL_END_TIME, endTime);
        String whereClause = COL_ID + " = " + sessionId;
        monitorTable.update(mTestContext, monitorTuple, whereClause, null);

        // invoke method
        Collection<NetworkStats> networkStats = null;
        try {
            networkStats = (Collection<NetworkStats>) method.invoke(mSession, mTestContext);
        } catch (Exception e) {
            fail("Unable to invoke method: " + e.toString());
        }
        assertNotNull(networkStats);
        if (networkStats == null) return;

        // verify result. Should be only two network stats, LTE and EHRPD
        assertEquals(2, networkStats.size());

        // check the NetworkStat for each type
        int indexGreat = SignalStrengthValue.SIG_GREAT.ordinal();
        int indexGood = SignalStrengthValue.SIG_GOOD.ordinal();
        int indexMid = SignalStrengthValue.SIG_MID.ordinal();
        int indexPoor = SignalStrengthValue.SIG_POOR.ordinal();
        int indexNone = SignalStrengthValue.SIG_NONE.ordinal();
        for (NetworkStats stat : networkStats) {
            assertNotNull(stat);
            if (stat == null) return;
            if (networkTypeLte.equals(stat.mNetworkType)) {
                assertEquals(durationLte, stat.mTotalTime);
                assertEquals(1, stat.mCount);
                assertNotNull(stat.mSignalStrength);
                if (stat.mSignalStrength == null) return;
                // 1 great, 1 mid
                assertEquals(TIME_INTERVAL, stat.mSignalStrength[indexGreat]);
                assertEquals(0, stat.mSignalStrength[indexGood]);
                assertEquals(TIME_INTERVAL, stat.mSignalStrength[indexMid]);
                assertEquals(0, stat.mSignalStrength[indexPoor]);
                assertEquals(0, stat.mSignalStrength[indexNone]);
            } else if (networkType3g.equals(stat.mNetworkType)) {
                assertEquals(duration3g, stat.mTotalTime);
                assertEquals(1, stat.mCount);
                assertNotNull(stat.mSignalStrength);
                if (stat.mSignalStrength == null) return;
                // 1 great, 1 good
                assertEquals(TIME_INTERVAL, stat.mSignalStrength[indexGreat]);
                assertEquals(TIME_INTERVAL, stat.mSignalStrength[indexGood]);
                assertEquals(0, stat.mSignalStrength[indexMid]);
                assertEquals(0, stat.mSignalStrength[indexPoor]);
                assertEquals(0, stat.mSignalStrength[indexNone]);
            } else {
                fail("Unexpected network type: " + stat.mNetworkType);
            }
        }
    }

    /**
     * Tests NetworkSession.getBounceStats()
     * Bounce between different network types and make sure the correct BounceStats are generated
     * LTE -> EHRPD
     * EHRPD -> LTE
     * LTE -> EHRPD
     * EHRPD -> 1xRTT
     * 1xRTT -> LTE
     * -------------
     * LTE -> EHRPD 2
     * EHRPD -> LTE 1
     * EHRPD -> 1xRTT 1
     * 1xRTT -> LTE 1
     */
    public void testGetBounceStats() {
        // use reflections to get to the private method
        Method method = null;
        try {
            method = NetworkSession.class.getDeclaredMethod("getBounceStats", Context.class);
            method.setAccessible(true);
        } catch (Exception e) {
            fail("Unable to find method.");
            return;
        }

        Random random = new Random();
        final long startTime = System.currentTimeMillis();
        // monitor session generation
        MonitorSessionTable monitorTable = new MonitorSessionTable();
        MonitorSessionTuple monitorTuple =
            MonitorSessionTableTest.makeMonitorSessionTuple(random, mBaseDb);
        monitorTuple.put(COL_START_TIME, startTime);
        monitorTuple.put(COL_END_TIME, 0);
        final long sessionId = monitorTable.insert(mTestContext, monitorTuple);
        assertTrue(sessionId > 0);
        mSession.mSessionId = sessionId;

        // data connection state tuple initialization
        DataConnectionStateTable dataTable = new DataConnectionStateTable();
        DataConnectionStateTuple dataTuple = new DataConnectionStateTuple();
        dataTuple.put(COL_FK_MONITOR_SESSION, sessionId);

        final String networkTypeLte = getNetworkType(TelephonyManager.NETWORK_TYPE_LTE);
        final String networkType3g = getNetworkType(TelephonyManager.NETWORK_TYPE_EHRPD);
        final String networkType1x = getNetworkType(TelephonyManager.NETWORK_TYPE_1xRTT);

        // connect LTE
        long timestamp = startTime;
        dataTuple.put(COL_STATE, NetworkSession.DATA_CONNECTED);
        dataTuple.put(COL_NETWORK_TYPE, networkTypeLte);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);
        // disconnect LTE
        timestamp += TIME_INTERVAL;
        dataTuple.put(COL_STATE, NetworkSession.DATA_DISCONNECTED);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // connect 3G
        dataTuple.put(COL_STATE, NetworkSession.DATA_CONNECTED);
        dataTuple.put(COL_NETWORK_TYPE, networkType3g);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);
        // disconnect 3G
        timestamp += TIME_INTERVAL;
        dataTuple.put(COL_STATE, NetworkSession.DATA_DISCONNECTED);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // connect LTE
        dataTuple.put(COL_STATE, NetworkSession.DATA_CONNECTED);
        dataTuple.put(COL_NETWORK_TYPE, networkTypeLte);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);
        // disconnect LTE
        timestamp += TIME_INTERVAL;
        dataTuple.put(COL_STATE, NetworkSession.DATA_DISCONNECTED);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // connect 3G
        dataTuple.put(COL_STATE, NetworkSession.DATA_CONNECTED);
        dataTuple.put(COL_NETWORK_TYPE, networkType3g);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);
        // disconnect 3G
        timestamp += TIME_INTERVAL;
        dataTuple.put(COL_STATE, NetworkSession.DATA_DISCONNECTED);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // connect 1xRTT
        dataTuple.put(COL_STATE, NetworkSession.DATA_CONNECTED);
        dataTuple.put(COL_NETWORK_TYPE, networkType1x);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);
        // disconnect 1xRTT
        timestamp += TIME_INTERVAL;
        dataTuple.put(COL_STATE, NetworkSession.DATA_DISCONNECTED);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // connect LTE
        dataTuple.put(COL_STATE, NetworkSession.DATA_CONNECTED);
        dataTuple.put(COL_NETWORK_TYPE, networkTypeLte);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // update end time for MonitorSessionTuple
        monitorTuple = new MonitorSessionTuple();
        final long endTime = timestamp;
        mSession.mEndTime = endTime;
        monitorTuple.put(COL_END_TIME, endTime);
        String whereClause = COL_ID + " = " + sessionId;
        monitorTable.update(mTestContext, monitorTuple, whereClause, null);

        // invoke method
        Collection<BounceStat> bounceStats = null;
        try {
            bounceStats = (Collection<BounceStat>) method.invoke(mSession, mTestContext);
        } catch (Exception e) {
            fail("Unable to invoke method: " + e.toString());
        }
        assertNotNull(bounceStats);
        if (bounceStats == null) return;

        // verify result. There should be 3 bounce stats
        assertEquals(3, bounceStats.size());

        // check each BounceStat
        boolean foundLte = false, found3g = false, found1x = false;

        Integer value = null;
        for (BounceStat stat : bounceStats) {
            assertNotNull(stat);
            if (stat == null) return;
            if (networkTypeLte.equals(stat.mFrom)) {
                foundLte = true;
                // LTE -> EHRPD
                value = stat.mToCount.get(networkType3g);
                assertNotNull(value);
                if (value == null) return;
                assertEquals(2, value.intValue());
            } else if (networkType3g.equals(stat.mFrom)) {
                found3g = true;
                // EHRPD -> LTE
                value = stat.mToCount.get(networkTypeLte);
                assertNotNull(value);
                if (value == null) return;
                assertEquals(1, value.intValue());

                // EHRPD -> 1xRTT
                value = stat.mToCount.get(networkType1x);
                assertNotNull(value);
                if (value == null) return;
                assertEquals(1, value.intValue());

            } else if (networkType1x.equals(stat.mFrom)) {
                found1x = true;
                // 1xRTT -> LTE
                value = stat.mToCount.get(networkTypeLte);
                assertNotNull(value);
                if (value == null) return;
                assertEquals(1, value.intValue());
            } else {
                fail("Unexpected from network type: " + stat.mFrom);
            }
        }
        assertTrue(foundLte);
        assertTrue(found3g);
        assertTrue(found1x);
    }

    /**
     * Tests NetworkSession.getBounceStats()
     * Bounce between different network types and make sure the correct BounceStats are generated.
     * Simulate the case where no disconnect message comes in and duplicate connect msg comes in
     * LTE -> LTE
     * LTE -> EHRPD
     * EHRPD -> EHRPD
     * EHRPD -> LTE
     * LTE -> LTE
     * LTE -> EHRPD
     * EHRPD -> 1xRTT
     * 1xRTT -> 1xRTT
     * 1xRTT -> LTE
     * -------------
     * LTE -> EHRPD 2
     * EHRPD -> LTE 1
     * EHRPD -> 1xRTT 1
     * 1xRTT -> LTE 1
     */
    public void testGetBounceStatsNoDisconnect() {
        // use reflections to get to the private method
        Method method = null;
        try {
            method = NetworkSession.class.getDeclaredMethod("getBounceStats", Context.class);
            method.setAccessible(true);
        } catch (Exception e) {
            fail("Unable to find method.");
            return;
        }

        Random random = new Random();
        final long startTime = System.currentTimeMillis();
        // monitor session generation
        MonitorSessionTable monitorTable = new MonitorSessionTable();
        MonitorSessionTuple monitorTuple =
            MonitorSessionTableTest.makeMonitorSessionTuple(random, mBaseDb);
        monitorTuple.put(COL_START_TIME, startTime);
        monitorTuple.put(COL_END_TIME, 0);
        final long sessionId = monitorTable.insert(mTestContext, monitorTuple);
        assertTrue(sessionId > 0);
        mSession.mSessionId = sessionId;

        // data connection state tuple initialization
        DataConnectionStateTable dataTable = new DataConnectionStateTable();
        DataConnectionStateTuple dataTuple = new DataConnectionStateTuple();
        dataTuple.put(COL_FK_MONITOR_SESSION, sessionId);

        final String networkTypeLte = getNetworkType(TelephonyManager.NETWORK_TYPE_LTE);
        final String networkType3g = getNetworkType(TelephonyManager.NETWORK_TYPE_EHRPD);
        final String networkType1x = getNetworkType(TelephonyManager.NETWORK_TYPE_1xRTT);

        // connect LTE
        long timestamp = startTime;
        dataTuple.put(COL_STATE, NetworkSession.DATA_CONNECTED);
        dataTuple.put(COL_NETWORK_TYPE, networkTypeLte);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // connect LTE
        timestamp += TIME_INTERVAL;
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // connect 3G
        timestamp += TIME_INTERVAL;
        dataTuple.put(COL_NETWORK_TYPE, networkType3g);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // connect 3G
        timestamp += TIME_INTERVAL;
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // connect LTE
        timestamp += TIME_INTERVAL;
        dataTuple.put(COL_NETWORK_TYPE, networkTypeLte);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // connect LTE
        timestamp += TIME_INTERVAL;
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // connect 3G
        timestamp += TIME_INTERVAL;
        dataTuple.put(COL_NETWORK_TYPE, networkType3g);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // connect 1xRTT
        timestamp += TIME_INTERVAL;
        dataTuple.put(COL_NETWORK_TYPE, networkType1x);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // connect 1xRTT
        timestamp += TIME_INTERVAL;
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // connect LTE
        timestamp += TIME_INTERVAL;
        dataTuple.put(COL_NETWORK_TYPE, networkTypeLte);
        dataTuple.put(COL_TIMESTAMP, timestamp);
        assertTrue(dataTable.insert(mTestContext, dataTuple) > 0);

        // update end time for MonitorSessionTuple
        monitorTuple = new MonitorSessionTuple();
        final long endTime = timestamp;
        mSession.mEndTime = endTime;
        monitorTuple.put(COL_END_TIME, endTime);
        String whereClause = COL_ID + " = " + sessionId;
        monitorTable.update(mTestContext, monitorTuple, whereClause, null);

        // invoke method
        Collection<BounceStat> bounceStats = null;
        try {
            bounceStats = (Collection<BounceStat>) method.invoke(mSession, mTestContext);
        } catch (Exception e) {
            fail("Unable to invoke method: " + e.toString());
        }
        assertNotNull(bounceStats);
        if (bounceStats == null) return;

        // verify result. There should be 3 bounce stats
        assertEquals(3, bounceStats.size());

        // check each BounceStat
        boolean foundLte = false, found3g = false, found1x = false;

        Integer value = null;
        for (BounceStat stat : bounceStats) {
            assertNotNull(stat);
            if (stat == null) return;
            if (networkTypeLte.equals(stat.mFrom)) {
                foundLte = true;
                // LTE -> EHRPD
                value = stat.mToCount.get(networkType3g);
                assertNotNull(value);
                if (value == null) return;
                assertEquals(2, value.intValue());
            } else if (networkType3g.equals(stat.mFrom)) {
                found3g = true;
                // EHRPD -> LTE
                value = stat.mToCount.get(networkTypeLte);
                assertNotNull(value);
                if (value == null) return;
                assertEquals(1, value.intValue());

                // EHRPD -> 1xRTT
                value = stat.mToCount.get(networkType1x);
                assertNotNull(value);
                if (value == null) return;
                assertEquals(1, value.intValue());

            } else if (networkType1x.equals(stat.mFrom)) {
                found1x = true;
                // 1xRTT -> LTE
                value = stat.mToCount.get(networkTypeLte);
                assertNotNull(value);
                if (value == null) return;
                assertEquals(1, value.intValue());
            } else {
                fail("Unexpected from network type: " + stat.mFrom);
            }
        }
        assertTrue(foundLte);
        assertTrue(found3g);
        assertTrue(found1x);
    }
}
