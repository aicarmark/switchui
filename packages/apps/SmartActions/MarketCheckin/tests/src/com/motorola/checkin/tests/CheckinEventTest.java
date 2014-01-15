/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/14   IKCTXTAW-481    Initial version
 */

package com.motorola.checkin.tests;

import java.util.List;
import java.util.Random;

import android.content.ContentValues;
import android.test.AndroidTestCase;
import android.test.mock.MockContentResolver;

import com.motorola.checkin.CheckinEvent;
import com.motorola.checkin.CheckinSegment;
import com.motorola.checkin.tests.CheckinProvider.CheckinRow;

/**
 * Two ways to run the tests:
 * 1. Type "ant test" in project root directory
 * 2. From Eclipse, right click on project and select "Run As->Android JUnit Test"
 *
 * This test suite is to be run on devices that have MMI SDK.
 * It tests whether CheckinEvent is working as it should on devices with MMI SDK.
 * It does not test the error handling of CheckinEvent on devices without MMI SDK.
 * This test suite is expected to fail when run on devices that do not have MMI SDK, and
 * it's not meant to be run on devices that do not have MMI SDK.
 * A separate test case is needed for such a case, to be run specifically
 * on devices without MMI SDK.
 */
public class CheckinEventTest extends AndroidTestCase {

    // check-in keys
    private static final String COL_ID = "ID";
    private static final String COL_VER = "ver";
    private static final String COL_TIME = "time";

    private MockContentResolver mMockResolver;
    private CheckinProvider mCheckinProvider;
    private static final String AUTHORITY_CHECK_IN = "com.motorola.android.server.checkin";
    private Random mRandom = new Random();

    private String getRandomString() {
        return String.valueOf(mRandom.nextDouble());
    }

    private boolean getRandomBoolean() {
        boolean randomBoolean = false;
        int randomInt = mRandom.nextInt();
        if (randomInt % 2 == 0) {
            randomBoolean = true;
        }
        return randomBoolean;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mCheckinProvider = new CheckinProvider();
        mMockResolver = new MockContentResolver();
        mMockResolver.addProvider(AUTHORITY_CHECK_IN, mCheckinProvider);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mMockResolver = null;
        mCheckinProvider.cleanup();
        mCheckinProvider = null;
    }

    @Override
    public void testAndroidTestCaseSetupProperly() {
        super.testAndroidTestCaseSetupProperly();
        assertNotNull(mMockResolver);
        assertNotNull(mCheckinProvider);
    }

    /**
     * Tests whether MMI SDK is available by checking if
     * CheckinEvent is initialized properly.
     */
    public void testIsInitialized() {
        assertTrue(CheckinEvent.isInitialized());
    }

    public void testCheckinEventConstructor() {
        // check constructor with auto timestamp
        String tag = getRandomString();
        String eventName = getRandomString();
        String version = getRandomString();

        long before = System.currentTimeMillis();
        CheckinEvent checkinEvent = new CheckinEvent(tag, eventName, version);
        long after = System.currentTimeMillis();

        assertNotNull(checkinEvent);
        assertEquals(tag, checkinEvent.getTagName());
        assertEquals(eventName, checkinEvent.getEventName());
        assertEquals(version, checkinEvent.getVersion());
        assertTrue(before <= checkinEvent.getTimestamp());
        assertTrue(after >= checkinEvent.getTimestamp());

        // check constructor with explicit timestamp
        tag = getRandomString();
        eventName = getRandomString();
        version = getRandomString();
        long timestamp = mRandom.nextLong();

        checkinEvent = new CheckinEvent(tag, eventName, version, timestamp);
        assertEquals(tag, checkinEvent.getTagName());
        assertEquals(eventName, checkinEvent.getEventName());
        assertEquals(version, checkinEvent.getVersion());
        assertEquals(timestamp, checkinEvent.getTimestamp());
    }

    public void testCheckinEventSetValue() {
        // input values
        final String tag = getRandomString();
        final String eventName = getRandomString();
        final String version = getRandomString();
        final long timestamp = mRandom.nextLong();

        final String boolKey = "boolKey";
        final String doubleKey = "doubleKey";
        final String intKey = "intKey";
        final String longKey = "longKey";
        final String stringKey = "stringKey";

        final boolean boolValue = getRandomBoolean();
        final double doubleValue = mRandom.nextDouble();
        final int intValue = mRandom.nextInt();
        final long longValue = mRandom.nextLong();
        final String stringValue = getRandomString();

        // expected output
        StringBuilder sb = new StringBuilder();
        sb.append("[ID=" + eventName + ";");
        sb.append("ver=" + version + ";");
        sb.append("time=" + timestamp + ";");
        sb.append(boolKey + "=" + boolValue + ";");
        sb.append(doubleKey + "=" + doubleValue + ";");
        sb.append(intKey + "=" + intValue + ";");
        sb.append(longKey + "=" + longValue + ";");
        sb.append(stringKey + "=" + stringValue + ";");
        sb.append("]");
        final String expectedOutput = sb.toString();

        // set CheckinEvent values
        CheckinEvent checkinEvent = new CheckinEvent(tag, eventName, version, timestamp);
        checkinEvent.setValue(boolKey, boolValue);
        checkinEvent.setValue(doubleKey, doubleValue);
        checkinEvent.setValue(intKey, intValue);
        checkinEvent.setValue(longKey, longValue);
        checkinEvent.setValue(stringKey, stringValue);

        // verify output
        StringBuilder serializer = checkinEvent.serializeEvent();
        assertNotNull(serializer);
        assertEquals(expectedOutput, serializer.toString());
    }

    public void testCheckinEventPublish() {
        // input values
        final String tag = getRandomString();
        final String eventName = getRandomString();
        final String version = getRandomString();
        final long timestamp = mRandom.nextLong();

        final String boolKey = "boolKey";
        final String doubleKey = "doubleKey";
        final String intKey = "intKey";
        final String longKey = "longKey";
        final String stringKey = "stringKey";

        final boolean boolValue = getRandomBoolean();
        final double doubleValue = mRandom.nextDouble();
        final int intValue = mRandom.nextInt();
        final long longValue = mRandom.nextLong();
        final String stringValue = getRandomString();

        // set CheckinEvent values
        CheckinEvent checkinEvent = new CheckinEvent(tag, eventName, version, timestamp);
        checkinEvent.setValue(boolKey, boolValue);
        checkinEvent.setValue(doubleKey, doubleValue);
        checkinEvent.setValue(intKey, intValue);
        checkinEvent.setValue(longKey, longValue);
        checkinEvent.setValue(stringKey, stringValue);

        // publish
        checkinEvent.publish(mMockResolver);
        List<CheckinRow> checkinRows = mCheckinProvider.getCheckinValues();
        assertNotNull(checkinRows);

        // there should be only one check-in row
        assertEquals(1, checkinRows.size());

        // verify the check-in row
        CheckinRow row = checkinRows.get(0);
        assertNotNull(row);

        assertEquals(tag, row.mCheckinTag);
        ContentValues cv = row.mCheckinEvent;
        assertEquals(eventName, cv.getAsString(COL_ID));
        assertEquals(version, cv.getAsString(COL_VER));
        assertEquals(String.valueOf(timestamp), cv.getAsString(COL_TIME));
        assertEquals(String.valueOf(boolValue), cv.getAsString(boolKey));
        assertEquals(String.valueOf(doubleValue), cv.getAsString(doubleKey));
        assertEquals(String.valueOf(intValue), cv.getAsString(intKey));
        assertEquals(String.valueOf(longValue), cv.getAsString(longKey));
        assertEquals(stringValue, cv.getAsString(stringKey));
        assertEquals(8, cv.size());
    }

    public void testCheckinSegmentConstructor() {
        final String segmentName = getRandomString();
        CheckinSegment checkinSegment = new CheckinSegment(segmentName);
        assertEquals(segmentName, checkinSegment.getSegmentName());
    }

    public void testCheckinSegmentSetValue() {
        // input values
        final String tag = getRandomString();
        final String eventName = getRandomString();
        final String version = getRandomString();
        final long timestamp = mRandom.nextLong();

        final String segmentName = getRandomString();
        final String boolKey = "boolKey";
        final String doubleKey = "doubleKey";
        final String intKey = "intKey";
        final String longKey = "longKey";
        final String stringKey = "stringKey";

        final boolean boolValue = getRandomBoolean();
        final double doubleValue = mRandom.nextDouble();
        final int intValue = mRandom.nextInt();
        final long longValue = mRandom.nextLong();
        final String stringValue = getRandomString();

        // expected output
        StringBuilder sb = new StringBuilder();
        // CheckinEvent
        sb.append("[ID=" + eventName + ";");
        sb.append("ver=" + version + ";");
        sb.append("time=" + timestamp + ";");
        sb.append("]");
        // CheckinSegment
        sb.append("[ID=" + segmentName + ";");
        sb.append(boolKey + "=" + boolValue + ";");
        sb.append(doubleKey + "=" + doubleValue + ";");
        sb.append(intKey + "=" + intValue + ";");
        sb.append(longKey + "=" + longValue + ";");
        sb.append(stringKey + "=" + stringValue + ";");
        sb.append("]");
        final String expectedOutput = sb.toString();

        // set CheckinEvent values
        CheckinEvent checkinEvent = new CheckinEvent(tag, eventName, version, timestamp);
        // add CheckinSegment
        CheckinSegment checkinSegment = new CheckinSegment(segmentName);
        checkinSegment.setValue(boolKey, boolValue);
        checkinSegment.setValue(doubleKey, doubleValue);
        checkinSegment.setValue(intKey, intValue);
        checkinSegment.setValue(longKey, longValue);
        checkinSegment.setValue(stringKey, stringValue);
        checkinEvent.addSegment(checkinSegment);

        // verify output
        StringBuilder serializer = checkinEvent.serializeEvent();
        assertNotNull(serializer);
        assertEquals(expectedOutput, serializer.toString());
    }

    public void testCheckinSegmentPublish() {
        // input values
        final String tag = getRandomString();
        final String eventName = getRandomString();
        final String version = getRandomString();
        final long timestamp = mRandom.nextLong();


        final String boolKey = "boolKey";
        final String doubleKey = "doubleKey";
        final String intKey = "intKey";
        final String longKey = "longKey";
        final String stringKey = "stringKey";

        final String segment1Name = getRandomString();
        final boolean boolValue1 = getRandomBoolean();
        final double doubleValue1 = mRandom.nextDouble();
        final int intValue1 = mRandom.nextInt();
        final long longValue1 = mRandom.nextLong();
        final String stringValue1 = getRandomString();

        final String segment2Name = getRandomString();
        final boolean boolValue2 = getRandomBoolean();
        final double doubleValue2 = mRandom.nextDouble();
        final int intValue2 = mRandom.nextInt();
        final long longValue2 = mRandom.nextLong();
        final String stringValue2 = getRandomString();

        // set CheckinEvent values
        CheckinEvent checkinEvent = new CheckinEvent(tag, eventName, version, timestamp);
        // add CheckinSegment1
        CheckinSegment checkinSegment1 = new CheckinSegment(segment1Name);
        checkinSegment1.setValue(boolKey, boolValue1);
        checkinSegment1.setValue(doubleKey, doubleValue1);
        checkinSegment1.setValue(intKey, intValue1);
        checkinSegment1.setValue(longKey, longValue1);
        checkinSegment1.setValue(stringKey, stringValue1);
        checkinEvent.addSegment(checkinSegment1);
        // add CheckinSegment2
        CheckinSegment checkinSegment2 = new CheckinSegment(segment2Name);
        checkinSegment2.setValue(boolKey, boolValue2);
        checkinSegment2.setValue(doubleKey, doubleValue2);
        checkinSegment2.setValue(intKey, intValue2);
        checkinSegment2.setValue(longKey, longValue2);
        checkinSegment2.setValue(stringKey, stringValue2);
        checkinEvent.addSegment(checkinSegment2);

        // publish
        checkinEvent.publish(mMockResolver);
        List<CheckinRow> checkinRows = mCheckinProvider.getCheckinValues();
        assertNotNull(checkinRows);

        // there should be only one check-in row
        assertEquals(1, checkinRows.size());

        // verify the check-in row
        CheckinRow row = checkinRows.get(0);
        assertNotNull(row);
        assertEquals(tag, row.mCheckinTag);

        // CheckinEvent
        ContentValues cv = row.mCheckinEvent;
        assertNotNull(cv);
        assertEquals(eventName, cv.getAsString(COL_ID));
        assertEquals(version, cv.getAsString(COL_VER));
        assertEquals(String.valueOf(timestamp), cv.getAsString(COL_TIME));
        assertEquals(3, cv.size());

        // CheckinSegment
        assertEquals(2, row.mSegments.size());
        // CheckinSegment1
        cv = row.mSegments.get(0);
        assertEquals(segment1Name, cv.getAsString(COL_ID));
        assertEquals(String.valueOf(boolValue1), cv.getAsString(boolKey));
        assertEquals(String.valueOf(doubleValue1), cv.getAsString(doubleKey));
        assertEquals(String.valueOf(intValue1), cv.getAsString(intKey));
        assertEquals(String.valueOf(longValue1), cv.getAsString(longKey));
        assertEquals(stringValue1, cv.getAsString(stringKey));
        assertEquals(6, cv.size());

        // CheckinSegment2
        cv = row.mSegments.get(1);
        assertEquals(segment2Name, cv.getAsString(COL_ID));
        assertEquals(String.valueOf(boolValue2), cv.getAsString(boolKey));
        assertEquals(String.valueOf(doubleValue2), cv.getAsString(doubleKey));
        assertEquals(String.valueOf(intValue2), cv.getAsString(intKey));
        assertEquals(String.valueOf(longValue2), cv.getAsString(longKey));
        assertEquals(stringValue2, cv.getAsString(stringKey));
        assertEquals(6, cv.size());
    }
}
