/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 * w04917 (Brian Lee)        2012/06/21   IKCTXTAW-480    Move alarm to TopLocationManagerService
 * w04917 (Brian Lee)        2012/07/09   IKCTXTAW-487    Add NetworkConditionDeterminer & refactor
 */

package com.motorola.contextual.smartnetwork;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;

public class ChargerReceiver extends BroadcastReceiver {
    public static final long DELAY = DateUtils.HOUR_IN_MILLIS;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SmartNetwork.isEnabled() && intent != null) {
            String action = intent.getAction();

            if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                // schedule TopLocationManagerService to run after DELAY
                long scheduledTime = System.currentTimeMillis() + DELAY;
                SmartNetworkService.scheduleService(context, scheduledTime);
            } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                // cancel the scheduled TopLocationManagerService
                SmartNetworkService.cancelService(context);
            }
        }
    }
}
