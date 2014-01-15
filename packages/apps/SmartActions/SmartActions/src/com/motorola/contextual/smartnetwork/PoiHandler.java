/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 * w04917 (Brian Lee)        2012/07/12   IKCTXTAW-487    Add NetworkConditionDeterminer & refactor
 * w04917 (Brian Lee)        2012/07/16   IKCTXTAW-492    Use contants from LocationSensor
 */

package com.motorola.contextual.smartnetwork;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.TRANSIENT;

import java.util.List;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartnetwork.db.table.TopLocationTable;
import com.motorola.contextual.smartnetwork.db.table.TopLocationTuple;

public class PoiHandler extends IntentService {
    private static final String TAG = PoiHandler.class.getSimpleName();
    private static final boolean LOGD = true;

    public static final String INTENT_POI_EVENT =
        "com.motorola.contextual.smartnetwork.INTENT_POI_EVENT";
    public static final String EXTRA_POI = "poi";

    public static final String INTENT_POI_ACTION =
        "com.motorola.contextual.smartnetwork.INTENT_POI_ACTION";
    public static final String EXTRA_LOCATION = "location";
    public static final String EXTRA_RANK = "rank";
    public static final String EXTRA_TIME_SPENT = "time_spent";
    public static final String EXTRA_RANK_UPDATED = "rank_updated";

    public PoiHandler() {
        super(PoiHandler.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String poi = getPoi(intent);
        if (TRANSIENT.equals(poi)) {
            if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Leaving poi.");
            }
            leavePoi();
        } else if (poi != null && !poi.isEmpty()) {
            if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Entering poi: " + poi);
            }
            enterPoi(poi);
        } else {
            Log.e(TAG, "Invalid poi value.");
        }
    }

    private String getPoi(Intent intent) {
        String poi = null;
        if (intent != null) {
            if (INTENT_POI_EVENT.equals(intent.getAction())) {
                poi = intent.getStringExtra(EXTRA_POI);
            }
        }
        return poi;
    }

    private void enterPoi(String poi) {
        // look up poi in TopLocationTable
        TopLocationTable table = new TopLocationTable();
        String[] columns = { TopLocationTuple.COL_ID,
                             TopLocationTuple.COL_NETWORK_CONDITION,
                             TopLocationTuple.COL_RANK,
                             TopLocationTuple.COL_TIME_SPENT,
                             TopLocationTuple.COL_RANK_UPDATED
                           };
        String selection = TopLocationTuple.COL_POI + " = ?";
        String[] selectionArgs = { poi };
        List<TopLocationTuple> tuples = table.query(this, columns, selection,
                                        selectionArgs, null, null, null, null);

        // check result tuple
        if (tuples != null && tuples.size() == 1) {
            TopLocationTuple tuple = tuples.get(0);
            if (tuple != null) {
                // act according to network condition
                String networkCondition = tuple.getString(
                                              TopLocationTuple.COL_NETWORK_CONDITION);

                if (NetworkMonitorService.isMonitoringNeeded(networkCondition)) {
                    Intent serviceIntent = new Intent(
                        NetworkMonitorService.INTENT_START_NETWORK_MONITOR);
                    serviceIntent.setClass(this, NetworkMonitorService.class);
                    serviceIntent.putExtra(EXTRA_LOCATION, tuple.getId());
                    serviceIntent.putExtra(EXTRA_RANK, tuple.getInt(TopLocationTuple.COL_RANK));
                    serviceIntent.putExtra(EXTRA_TIME_SPENT,
                                           tuple.getLong(TopLocationTuple.COL_TIME_SPENT));
                    serviceIntent.putExtra(EXTRA_RANK_UPDATED,
                                           tuple.getLong(TopLocationTuple.COL_RANK_UPDATED));
                    startService(serviceIntent);
                } else if (NetworkSwitcherService.isNetworkSwitchNeeded(networkCondition)) {
                    // TODO: start network switcher
                }
            } else {
                Log.e(TAG, "Tuple is null.");
            }
        } else if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Ignoring unrelated poi: " + poi);
        }
    }

    private void leavePoi() {
        Intent serviceIntent = new Intent(NetworkMonitorService.INTENT_STOP_NETWORK_MONITOR);
        serviceIntent.setClass(this, NetworkMonitorService.class);
        startService(serviceIntent);
        // TODO: start network switcher and restore network
    }
}
