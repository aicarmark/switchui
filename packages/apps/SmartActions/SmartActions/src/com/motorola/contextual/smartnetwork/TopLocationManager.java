/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/07/03   IKCTXTAW-480    Initial version
 * w04917 (Brian Lee)        2012/07/09   IKCTXTAW-487    Add NetworkConditionDeterminer & refactor
 * w04917 (Brian Lee)        2012/07/16   IKCTXTAW-492    Purge obsolete TopLocations
 */

package com.motorola.contextual.smartnetwork;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.motorola.contextual.smartnetwork.db.table.ConsolidatedLocTimeTable;
import com.motorola.contextual.smartnetwork.db.table.LocTimeTable;
import com.motorola.contextual.smartnetwork.db.table.LocTimeTuple;
import com.motorola.contextual.smartnetwork.db.table.PoiTable;
import com.motorola.contextual.smartnetwork.db.table.PoiTuple;
import com.motorola.contextual.smartnetwork.db.table.TopLocationTable;
import com.motorola.contextual.smartnetwork.db.table.TopLocationTuple;

public class TopLocationManager {
    private static final String TAG = TopLocationManager.class.getSimpleName();
    private static final boolean LOGD = true;

    // prefix for poi key
    private static final String POI_PREFIX = "TopLocation_";

    private final int fMaxTopLocations;
    private final long fLocTimeSearchPeriod;

    /**
     * @param maxTopLocations how many top locations to store
     * @param locTimeSearchPeriod Search period in ms for LocTime table search. Purges data older
     * than this period. (i.e. Search within last month and purge top locations older than a month).
     */
    public TopLocationManager(int maxTopLocations, long locTimeSearchPeriod) {
        fMaxTopLocations = maxTopLocations;
        fLocTimeSearchPeriod = locTimeSearchPeriod;
    }

    /**
     * Goes through the LocTime table rows within the past locTimeSearchPeriod
     * and updates the TopLocations accordingly.
     * @param context context to use
     * @return true if TopLocations were successfully inserted/update, false otherwise. Also
     * returns false if there were no top locations to insert/update.
     */
    public boolean manageTopLocations(Context context) {
        boolean success = false;

        if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "manageTopLocations()");
        }
        TopLocationTable topLocationTable = new TopLocationTable();
        List<LocTimeTuple> sortedLocations = getConsolidatedSortedLocTimeTuples(context);
        if (sortedLocations != null && !sortedLocations.isEmpty()) {
            int count = 0;
            final long currentTime = System.currentTimeMillis();

            // first reset all the ranks
            TopLocationTuple updateTuple = new TopLocationTuple();
            updateTuple.put(TopLocationTuple.COL_RANK, 0);
            // don't update timestamp since we want to purge obsolete locations
            int updated = topLocationTable.update(context, updateTuple, null, null);
            if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Reset ranks on " + updated + " records.");
            }

            // go through the newly sorted locations and update/insert as necessary
            for (LocTimeTuple locationTuple : sortedLocations) {
                if (count >= fMaxTopLocations) break;
                /* if the duration is 0, stop because that means it's a consolidated location.
                 * the tuples are sorted by duration already as well.
                 */
                if (locationTuple.getLong(LocTimeTuple.COL_DURATION) <= 0) {
                    if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "No more distinct locations.");
                    }
                    break;
                }
                count++;
                /* TODO: use the main location's LocTime row id for poi name for now.
                 * Need to find out better way/key. maybe use wifi bssid with strongest signal.
                 * Right now, if the oldest LocTime row id we're using gets purged, we'll
                 * have a problem... it will use the next oldest row id as PoiName,
                 * but it will consolidate to the same location anyway...
                 */
                String poi = POI_PREFIX + locationTuple.getId();
                String wifi = locationTuple.getString(LocTimeTuple.COL_WIFI_SSID);
                String cellTower = locationTuple.getString(LocTimeTuple.COL_CELL_TOWER);
                long timeSpent = locationTuple.getLong(LocTimeTuple.COL_DURATION);
                if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "top location #" + count
                          + ", poi: " + poi
                          + ", duration: " + timeSpent
                          + ", wifi: " + wifi
                          + ", cellTower: " + cellTower);
                }

                // check if the location already exists in TopLocation table
                String[] columns = { TopLocationTuple.COL_ID };
                // use poi name as key, as it's supposed to be unique in both tables
                String selection = TopLocationTuple.COL_POI +  " = ?";
                String[] selectionArgs = { poi };
                List<TopLocationTuple> tuples = topLocationTable.query(context, columns, selection,
                                                selectionArgs, null, null, null, null);
                // only insert it if it doesn't exist, which means it's completely new
                if (tuples == null || tuples.isEmpty()) {
                    // insert the location into TopLocation table
                    TopLocationTuple topLocationTuple = new TopLocationTuple();
                    topLocationTuple.put(TopLocationTuple.COL_POI, poi);
                    topLocationTuple.put(TopLocationTuple.COL_WIFI_SSID, wifi);
                    topLocationTuple.put(TopLocationTuple.COL_CELL_TOWERS, cellTower);
                    topLocationTuple.put(TopLocationTuple.COL_NETWORK_CONDITION,
                                         TopLocationTable.NETWORK_CONDITION_UNKNOWN);
                    topLocationTuple.put(TopLocationTuple.COL_PREVIOUS_NETWORK_CONDITION,
                                         TopLocationTable.NETWORK_CONDITION_UNKNOWN);
                    topLocationTuple.put(TopLocationTuple.COL_NETWORK_CONDITION_UPDATED, currentTime);
                    topLocationTuple.put(TopLocationTuple.COL_TIME_SPENT, timeSpent);
                    topLocationTuple.put(TopLocationTuple.COL_RANK, count);
                    topLocationTuple.put(TopLocationTuple.COL_RANK_UPDATED, currentTime);

                    long rowId = topLocationTable.insert(context, topLocationTuple);
                    if (rowId <= 0) {
                        Log.e(TAG, "Unable to insert " + poi + " into top location table.");
                    }

                    // insert the location into POI table
                    PoiTuple poiTuple = new PoiTuple();
                    poiTuple.put(PoiTuple.COL_CELL_TOWERS, cellTower);
                    poiTuple.put(PoiTuple.COL_POI, poi);
                    poiTuple.put(PoiTuple.COL_WIFI_SSID, wifi);
                    PoiTable poiTable = new PoiTable();
                    rowId = poiTable.insert(context, poiTuple);
                    if (rowId <= 0) {
                        Log.e(TAG, "Unable to insert " + poi + " into poi table.");
                    } else {
                        success = true;
                    }
                } else if (tuples.size() == 1) {
                    TopLocationTuple oldTuple = tuples.get(0);
                    long rowId = oldTuple.getId();
                    if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, poi + " already exists. Updating row " + rowId);
                    }

                    updateTuple = new TopLocationTuple();
                    updateTuple.put(TopLocationTuple.COL_TIME_SPENT, timeSpent);
                    updateTuple.put(TopLocationTuple.COL_RANK, count);
                    updateTuple.put(TopLocationTuple.COL_RANK_UPDATED, currentTime);
                    String whereClause = TopLocationTuple.COL_ID + " = ? ";
                    String[] whereArgs = { String.valueOf(rowId) };
                    updated = topLocationTable.update(context, updateTuple, whereClause, whereArgs);
                    if (updated <= 0) {
                        Log.e(TAG, "Unable to update " + poi);
                    } else {
                        success = true;
                    }
                } else {
                    Log.e(TAG, "Incorrect number of rows: " + tuples.size());
                }
            }
        }
        // purge obsolete locations
        String whereClause = TopLocationTuple.COL_RANK_UPDATED + " <= "
                             + String.valueOf(System.currentTimeMillis() - fLocTimeSearchPeriod);
        int deleted = topLocationTable.delete(context, whereClause, null);
        if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Deleted " + deleted + " obsolete rows.");
        }
        return success;
    }

    /**
     * Compare duration of two LocTimeTuple to sort it by duration in descending order
     * @param context context to use
     */
    private class LocationDurationComparator implements Comparator<LocTimeTuple> {
        public int compare(LocTimeTuple tuple1, LocTimeTuple tuple2) {
            long duration1 = tuple1.getLong(LocTimeTuple.COL_DURATION);
            long duration2 = tuple2.getLong(LocTimeTuple.COL_DURATION);

            if (duration1 < duration2) {
                return 1;
            } else if (duration1 > duration2) {
                return -1;
            } else {
                // if duration is the same, most recent location takes precedence
                long startTime1 = tuple1.getLong(LocTimeTuple.COL_START_TIME);
                long startTime2 = tuple2.getLong(LocTimeTuple.COL_START_TIME);
                if (startTime1 < startTime2) {
                    return 1;
                } else if (startTime1 > startTime2) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }
    }

    /**
     * Consolidates LocTime table rows within the past LocTimeSearchPeriod and sorts them
     * by total time spent in each consolidated location
     * @param context context to use
     * @return consolidated LocTimeTuples with combined duration, sorted by duration DESC
     */
    private List<LocTimeTuple> getConsolidatedSortedLocTimeTuples(Context context) {
        LocTimeTable locTimeTable = new LocTimeTable();
        String selection = LocTimeTuple.COL_START_TIME + " >= ?";
        // get all rows from LocTimeTable within last fLoctimeSearchPeriod
        String[] selectionArgs = { String.valueOf(System.currentTimeMillis()
                                   - fLocTimeSearchPeriod)
                                 };
        /* TODO: use ASC to use oldest location as primary location for consolidation.
         * Need to find out what to use for look up when looking up TopLocations table.
         * How do we check if the location from consolidated table already exists in
         * TopLocations table? may need to put in consolidated location information or extra
         * wifi information in TopLocations table.
         */
        String orderBy = LocTimeTuple.COL_ID + " ASC";
        List<LocTimeTuple> locTimeTuples = locTimeTable.query(context, LocTimeTuple.COLUMNS,
                                           selection, selectionArgs, null, null, orderBy, null);

        if (locTimeTuples != null && !locTimeTuples.isEmpty()) {
            // map of location id to its consolidated location id
            HashMap<Long, Long> consolidatedLocations = new HashMap<Long, Long>();

            // go through each LocTime row and consolidate
            for (LocTimeTuple locTimeTuple : locTimeTuples) {
                long locationId = locTimeTuple.getId();
                long timeSpent = 0;
                // was this location already consolidated?
                if (!consolidatedLocations.containsKey(locationId)) {
                    // get all the locations that consolidates to this location
                    List<LocTimeTuple> consolidatedTuples = getConsolidatedLocations(locationId,
                                                            context);
                    if (consolidatedTuples != null) {
                        for (LocTimeTuple consolidatedTuple : consolidatedTuples) {
                            // map and remember each consolidated location
                            consolidatedLocations.put(consolidatedTuple.getId(), locationId);
                            long duration = consolidatedTuple.getLong(LocTimeTuple.COL_DURATION);
                            if (duration > 0) {
                                // add the time
                                timeSpent += consolidatedTuple.getLong(LocTimeTuple.COL_DURATION);
                            } else {
                                Log.e(TAG, "Location " + consolidatedTuple.getId()
                                      + " has wrong duration: " + duration);
                            }
                            if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                                Log.d(TAG, "Consolidate location " + consolidatedTuple.getId()
                                      + " to location " + locationId);
                            }
                        }
                    }
                }
                // update the time spent at this location. if consolidated already, set it to 0.
                locTimeTuple.put(LocTimeTuple.COL_DURATION, timeSpent);
            }

            // sort the location by duration in descending order
            Collections.sort(locTimeTuples, new LocationDurationComparator());
        } else {
            Log.e(TAG, "Unable to get LocTime rows.");
        }

        return locTimeTuples;
    }

    /**
     * Get all consolidated locations from LocTime table that consolidates to the given location
     * @param locationId location id
     * @param context context to use
     * @return a list of all LocTimeTuples that consolidate to the given location id
     */
    private static List<LocTimeTuple> getConsolidatedLocations(long locationId, Context context) {
        ConsolidatedLocTimeTable table = new ConsolidatedLocTimeTable();
        // bug in LocationSensorProvider only looks at selection and ignore selectionArgs
        String selection = LocTimeTuple.COL_ID + " = " + locationId;
        String orderBy = LocTimeTuple.COL_ID + " DESC";
        List<LocTimeTuple> tuples = table.query(context, LocTimeTuple.COLUMNS, selection, null,
                                                null, null, orderBy, null);
        return tuples;
    }
}
