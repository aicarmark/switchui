/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/07/09   IKCTXTAW-487    Initial version
 */

package com.motorola.contextual.smartnetwork;

import java.util.List;

import android.content.Context;
import android.util.Log;

import com.motorola.contextual.smartnetwork.db.SmartNetworkDbSchema;
import com.motorola.contextual.smartnetwork.db.table.MonitorSessionTable;
import com.motorola.contextual.smartnetwork.db.table.MonitorSessionTuple;
import com.motorola.contextual.smartnetwork.db.table.TopLocationTable;
import com.motorola.contextual.smartnetwork.db.table.TopLocationTuple;

public class NetworkConditionDeterminer implements SmartNetworkDbSchema {
    // TAG has to be less than 23 chars
    private static final String TAG = "NetworkCondDeterminer";
    private static final boolean LOGD = true;

    private final long fMaxArchiveTime;
    private final long fMaxMonitorTime;
    private final long fMonitorPeriod;

    /**
     * @param maxMonitorTime max amount of monitoring time allowed in monitorPeriod
     * before stopping monitoring a location
     * @param monitorPeriod monitoring period to use for recent data lookup
     * @param maxArchiveTime how long to keep old data around before purging
     */
    public NetworkConditionDeterminer(long maxMonitorTime, long monitorPeriod,
                                      long maxArchiveTime) {
        fMaxMonitorTime = maxMonitorTime;
        fMonitorPeriod = monitorPeriod;
        fMaxArchiveTime = maxArchiveTime;
    }

    /**
     * Determine the network conditions based on the data collected in the past MonitorPeriod
     * @param context context to use
     */
    public void determineNetworkConditions(Context context) {
        if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Determine network conditions.");
        }
        // get the total monitored time per location in the past fMonitorPeriod
        long startTime = System.currentTimeMillis() - fMonitorPeriod;
        final String col_monitor_time = "monitor_time";
        String sql = SELECT + COL_FK_TOP_LOCATION+ CS
                     + SUM + LP + COL_END_TIME + MINUS + COL_START_TIME + RP + AS + col_monitor_time
                     + CS + COL_NETWORK_CONDITION
                     + FROM + TBL_MONITOR_SESSION + JOIN + TBL_TOP_LOCATION
                     + ON + TBL_TOP_LOCATION + "." + COL_ID + EQUALS + COL_FK_TOP_LOCATION
                     + WHERE + COL_START_TIME + GT_OR_EQUAL + startTime
                     + AND + COL_END_TIME + GREATER_THAN + COL_START_TIME
                     + GROUP_BY + COL_FK_TOP_LOCATION;
        MonitorSessionTable monitorTable = new MonitorSessionTable();
        List<MonitorSessionTuple> monitorTuples = monitorTable.rawQuery(context, sql, null);

        if (monitorTuples != null && !monitorTuples.isEmpty()) {
            // update each top location's network condition according to monitor session value
            TopLocationTable locationTable = new TopLocationTable();
            TopLocationTuple updateTuple = new TopLocationTuple();
            String whereClause = TopLocationTuple.COL_ID + " = ?";
            String[] whereArgs = new String[1];

            for (MonitorSessionTuple monitorTuple : monitorTuples) {
                whereArgs[0] = monitorTuple.getString(COL_FK_TOP_LOCATION);
                long monitorTime = monitorTuple.getLong(col_monitor_time);

                // set the state to verify if we didn't accumulate enough monitoring time
                String networkCondition = TopLocationTable.NETWORK_CONDITION_VERIFY;

                if (monitorTime >= fMaxMonitorTime) {
                    // set the state to good/bad and don't monitor anymore
                    networkCondition = TopLocationTable.NETWORK_CONDITION_GOOD;
                }
                updateTuple.put(COL_NETWORK_CONDITION, networkCondition);
                updateTuple.put(COL_PREVIOUS_NETWORK_CONDITION,
                                monitorTuple.getString(COL_NETWORK_CONDITION));

                locationTable.update(context, updateTuple, whereClause, whereArgs);
                if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Updated " + whereArgs[0] + " to " + networkCondition
                          + ". Previously "
                          + monitorTuple.getString(COL_NETWORK_CONDITION));
                }
            }
        } else if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "No recent top locations to determine network condition on.");
        }
    }

    /**
     * Purge old data older than MaxArchiveTime
     * @param context context to use
     */
    public void purgeOldData(Context context) {
        long deleteTime = System.currentTimeMillis() - fMaxArchiveTime;
        String whereClause = COL_START_TIME + LT_OR_EQUAL + deleteTime;

        // deleting from MonitorSessionTable will cascade and delete other table rows as well
        MonitorSessionTable table = new MonitorSessionTable();
        int deleted = table.delete(context, whereClause, null);
        if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Deleted " + deleted + " rows.");
        }
    }

    /**
     * Check if any of the top locations hasn't been monitored recently.
     * If it hasn't been monitored, set the network condition to unknown.
     * @param context context to use
     */
    public void updateObsoleteLocations(Context context) {
        // get all TopLocations that hasn't been monitored in the past fMonitorPeriod
        long startTime = System.currentTimeMillis() - fMonitorPeriod;
        String sql = SELECT + COL_ID + CS + COL_NETWORK_CONDITION
                     + FROM + TBL_TOP_LOCATION + WHERE + NOT_EXISTS
                     + LP + SELECT + COL_FK_TOP_LOCATION + FROM + TBL_MONITOR_SESSION
                     + WHERE + COL_FK_TOP_LOCATION + EQUALS + TBL_TOP_LOCATION + "." + COL_ID
                     + AND + COL_START_TIME + GT_OR_EQUAL + startTime
                     + AND + COL_END_TIME + GREATER_THAN + COL_START_TIME + RP;
        TopLocationTable table = new TopLocationTable();
        List<TopLocationTuple> tuples = table.rawQuery(context, sql, null);

        if (tuples != null && !tuples.isEmpty()) {
            // update each location's current/previous network condition
            TopLocationTuple updateTuple = new TopLocationTuple();
            updateTuple.put(COL_NETWORK_CONDITION,
                            TopLocationTable.NETWORK_CONDITION_UNKNOWN);
            String whereClause = TopLocationTuple.COL_ID + " = ?";
            String[] whereArgs = new String[1];

            for (TopLocationTuple tuple : tuples) {
                updateTuple.put(COL_PREVIOUS_NETWORK_CONDITION,
                                tuple.getString(COL_NETWORK_CONDITION));
                whereArgs[0] = tuple.getString(COL_ID);
                table.update(context, updateTuple, whereClause, whereArgs);
                if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Updated " + whereArgs[0] + " to "
                          + TopLocationTable.NETWORK_CONDITION_UNKNOWN + ". Previously "
                          + tuple.getString(COL_NETWORK_CONDITION));
                }
            }
        } else if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "All top locations are current.");
        }
    }
}
