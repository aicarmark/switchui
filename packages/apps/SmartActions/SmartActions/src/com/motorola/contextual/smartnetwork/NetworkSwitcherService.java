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

package com.motorola.contextual.smartnetwork;

import android.app.IntentService;
import android.content.Intent;

import com.motorola.contextual.smartnetwork.db.table.TopLocationTable;

public class NetworkSwitcherService extends IntentService {
    @SuppressWarnings("unused")
	private static final String TAG = "NetworkSwitcherSvc";

    public static final String INTENT_SWITCH_NETWORK =
        "com.motorola.contextual.smartnetwork.INTENT_SWITCH_NETWORK";
    public static final String INTENT_RESTORE_NETWORK =
        "com.motorola.contextual.smartnetwork.INTENT_RESTORE_NETWORK";

    public NetworkSwitcherService() {
        super(NetworkSwitcherService.class.getSimpleName());
    }

    /**
     * @param networkCondition Network condition from TopLocationTable
     * @return true if network switch is needed, false otherwise
     */
    public static boolean isNetworkSwitchNeeded(String networkCondition) {
        boolean networkSwitch = false;
        if (TopLocationTable.NETWORK_CONDITION_BAD.equals(networkCondition)) {
            networkSwitch = true;
        }
        return networkSwitch;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // TODO: switch/restore network accordingly
    }

}
