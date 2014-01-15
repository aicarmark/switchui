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

package com.motorola.contextual.smartnetwork.test.mockobjects;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.motorola.android.wrapper.mock.MockAlarmManager;

public class TestAlarmManager extends MockAlarmManager {

    public static class Alarm {
        public final int mType;
        public final long mTriggerAtTime;
        public final long mInterval;
        public final PendingIntent mOperation;

        private Alarm(int type, long triggerAtTime, long interval, PendingIntent operation) {
            mType = type;
            mTriggerAtTime = triggerAtTime;
            mInterval = interval;
            mOperation = operation;
        }

        /**
         * Using the context provided, broadcasts the original intent wrapped in the PendingIntent
         * that was used to create the alarm. Has the same effect as the alarm expiring.
         * @param context Test context to use for broadcasting the intent
         * @throws CanceledException
         */
        public void fireBroadcast(final AlarmContext context) throws CanceledException {
            /* since we can't fire PendingIntent directly to our test context, and since we can't
             * even extract the Intent out from the PendingIntent, implement a work around by
             * using OnFinished callback to broadcast the alarm intent to the test context
             */
            PendingIntent.OnFinished callback = new PendingIntent.OnFinished() {

                public void onSendFinished(PendingIntent pendingIntent, Intent intent, int resultCode,
                String resultData, Bundle resultExtras) {
                    context.sendBroadcast(intent);
                }
            };

            mOperation.send(0, callback, null);
        }
    }

    private final List<Alarm> mAlarmList = new LinkedList<Alarm>();

    public TestAlarmManager(Context context) {
        super(context);
    }

    @Override
    public void cancel(PendingIntent operation) {
        if (operation != null) {
            ListIterator<Alarm> iter = mAlarmList.listIterator();
            Alarm data;
            while (iter.hasNext()) {
                data = iter.next();
                if (operation.equals(data.mOperation)) {
                    iter.remove();
                }
            }
        }
    }

    @Override
    public void set(int type, long triggerAtTime, PendingIntent operation) {
        mAlarmList.add(new Alarm(type, triggerAtTime, 0, operation));
    }

    @Override
    public void setInexactRepeating(int type, long triggerAtTime, long interval,
                                    PendingIntent operation) {
        mAlarmList.add(new Alarm(type, triggerAtTime, interval, operation));
    }

    @Override
    public void setRepeating(int type, long triggerAtTime, long interval, PendingIntent operation) {
        mAlarmList.add(new Alarm(type, triggerAtTime, interval, operation));
    }

    /**
     * Returns the oldest set alarm and removes it from the alarm list
     * @return the oldest set alarm
     */
    public Alarm getAlarm() {
        Alarm alarm = null;
        if (!mAlarmList.isEmpty()) {
            alarm = mAlarmList.remove(0);
        }
        return alarm;
    }

    /**
     * @return true if there are alarms pending, false otherwise
     */
    public boolean hasAlarm() {
        return !mAlarmList.isEmpty();
    }

    /**
     * @return number of pending alarms
     */
    public int getAlarmCount() {
        return mAlarmList.size();
    }

    /**
     * Clears all alarms
     */
    public void clearAlarms() {
        mAlarmList.clear();
    }

}
