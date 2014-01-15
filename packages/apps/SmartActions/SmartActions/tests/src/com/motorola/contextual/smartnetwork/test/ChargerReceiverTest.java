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

import android.app.AlarmManager;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.test.AndroidTestCase;

import com.motorola.android.wrapper.SystemWrapper;
import com.motorola.contextual.smartnetwork.ChargerReceiver;
import com.motorola.contextual.smartnetwork.SmartNetwork;
import com.motorola.contextual.smartnetwork.test.mockobjects.AlarmContext;
import com.motorola.contextual.smartnetwork.test.mockobjects.TestAlarmManager;
import com.motorola.contextual.smartnetwork.test.mockobjects.TestAlarmManager.Alarm;

public class ChargerReceiverTest extends AndroidTestCase {
    private AlarmContext mAlarmContext;
    private static final String INTENT_MANAGE_SMART_NETWORK =
        "com.motorola.contextual.smartnetwork.INTENT_MANAGE_SMART_NETWORK";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SystemWrapper.clearMockSystemServices();
        mAlarmContext = new AlarmContext(getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mAlarmContext.cleanup();
        SystemWrapper.clearMockSystemServices();
    }

    @Override
    public void testAndroidTestCaseSetupProperly() {
        super.testAndroidTestCaseSetupProperly();
        assertTrue(SmartNetwork.isEnabled());
        assertNotNull(mAlarmContext);
    }

    public void testChargerPlug() {
        TestAlarmManager am = new TestAlarmManager(mAlarmContext);
        SystemWrapper.setMockSystemService(Context.ALARM_SERVICE, am);

        // send plug intent
        long timeBefore = System.currentTimeMillis();
        ChargerReceiver chargerReceiver = new ChargerReceiver();
        chargerReceiver.onReceive(mAlarmContext, new Intent(Intent.ACTION_POWER_CONNECTED));
        long timeAfter = System.currentTimeMillis();

        // get the alarm that was scheduled
        Alarm alarm = am.getAlarm();
        assertNotNull(alarm);
        if (alarm != null) {
            // make sure there were no other alarms set
            assertFalse(am.hasAlarm());

            // verify alarm data
            assertNotNull(alarm.mOperation);
            assertEquals(AlarmManager.RTC_WAKEUP, alarm.mType);
            assertEquals(0, alarm.mInterval);
            long triggerTime = alarm.mTriggerAtTime;
            assertTrue(timeBefore + ChargerReceiver.DELAY <= triggerTime);
            assertTrue(timeAfter + ChargerReceiver.DELAY >= triggerTime);

            // fire the alarm
            try {
                alarm.fireBroadcast(mAlarmContext);
            } catch (CanceledException e) {
                fail("Alarm was cancelled already.");
            }

            // verify the intent
            Intent alarmIntent = mAlarmContext.receiveBroadcast();
            assertNotNull(alarmIntent);
            if (alarmIntent != null) {
                String alarmAction = alarmIntent.getAction();
                assertNotNull(alarmAction);
                assertEquals(INTENT_MANAGE_SMART_NETWORK, alarmAction);
            }
        }
    }

    public void testChargerUnplug() {
        TestAlarmManager am = new TestAlarmManager(mAlarmContext);
        SystemWrapper.setMockSystemService(Context.ALARM_SERVICE, am);

        // send plug intent
        ChargerReceiver chargerReceiver = new ChargerReceiver();
        chargerReceiver.onReceive(mAlarmContext, new Intent(Intent.ACTION_POWER_CONNECTED));

        // send unplug intent
        chargerReceiver.onReceive(mAlarmContext, new Intent(Intent.ACTION_POWER_DISCONNECTED));

        // make sure there are no alarms set
        assertFalse(am.hasAlarm());
    }

}
