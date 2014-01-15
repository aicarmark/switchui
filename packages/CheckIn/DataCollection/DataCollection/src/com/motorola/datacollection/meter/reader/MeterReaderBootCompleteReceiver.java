/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2011/05/06   IKCTXTAW-272    Initial version
 *
 */

package com.motorola.datacollection.meter.reader;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;

import com.motorola.datacollection.meter.checkin.MeterReaderCheckIn;

/**
 * Listens for BOOT_COMPLETED and does the following:
 *    1. Send a delayed intent for peripherals to register
 *    2. schedule a periodic METER_READER_POWER_TOTAL check in
 *    3. schedule a periodic METER_READER_POWER_DETAIL check in
 *
 * @author w04917 (Brian Lee)
 *
 */
public class MeterReaderBootCompleteReceiver extends BroadcastReceiver {
    private static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    private static final String INTENT_METER_READER_READY = "com.motorola.datacollection.intent.METER_READER_READY";
    private static final long DELAY_METER_READER_READY = DateUtils.MINUTE_IN_MILLIS;

    /* interval value has to be one of AlarmManager constants to have the phone
     * phase-align it with other pending alarms to reduce wake-ups. Otherwise, it's the same as calling
     * setExactRepeating without phase-aligning.
     * For more info, refer to the AlarmManager documentation at
     * http://developer.android.com/reference/android/app/AlarmManager.html
     */
    private static final long CHECK_IN_INTERVAL_POWER_TOTAL = AlarmManager.INTERVAL_HOUR;
    private static final long CHECK_IN_INTERVAL_POWER_DETAIL = AlarmManager.INTERVAL_HALF_DAY;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context != null && intent != null && intent.getAction() != null) {
            String action = intent.getAction();

            if (BOOT_COMPLETED.equals(action)) {
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                /* schedule an alarm to notify peripherals to register their meters */
                Intent alarmIntent = new Intent();
                alarmIntent.setAction(INTENT_METER_READER_READY);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
                long currentTime = System.currentTimeMillis();
                long firstWake = currentTime + DELAY_METER_READER_READY;
                am.set(AlarmManager.RTC_WAKEUP, firstWake, pendingIntent);

                /* schedule check-in for POWER_TOTAL */
                alarmIntent = new Intent();
                alarmIntent.setAction(MeterReaderCheckIn.INTENT_CHECK_IN_POWER_TOTAL);
                pendingIntent = PendingIntent.getService(context, 0, alarmIntent, 0);
                firstWake = currentTime + CHECK_IN_INTERVAL_POWER_TOTAL;
                /* use RTC (as opposed to RTC_WAKEUP) and InexactRepeating to conserver power */
                am.setInexactRepeating(AlarmManager.RTC, firstWake, CHECK_IN_INTERVAL_POWER_TOTAL, pendingIntent);

                /* schedule check-in for POWER_DETAIL */
                alarmIntent = new Intent();
                alarmIntent.setAction(MeterReaderCheckIn.INTENT_CHECK_IN_POWER_DETAIL);
                pendingIntent = PendingIntent.getService(context, 0, alarmIntent, 0);
                firstWake = currentTime + CHECK_IN_INTERVAL_POWER_DETAIL;
                /* use RTC (as opposed to RTC_WAKEUP) and InexactRepeating to conserver power */
                am.setInexactRepeating(AlarmManager.RTC, firstWake, CHECK_IN_INTERVAL_POWER_DETAIL, pendingIntent);
            }
        }
    }
}
