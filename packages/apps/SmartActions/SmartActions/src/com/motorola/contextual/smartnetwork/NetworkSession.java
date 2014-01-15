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

package com.motorola.contextual.smartnetwork;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.util.Log;

import com.motorola.checkin.CheckinEvent;
import com.motorola.checkin.CheckinSegment;
import com.motorola.contextual.smartnetwork.db.table.DataConnectionStateTable;
import com.motorola.contextual.smartnetwork.db.table.DataConnectionStateTuple;
import com.motorola.contextual.smartnetwork.db.table.MonitorSessionTable;
import com.motorola.contextual.smartnetwork.db.table.MonitorSessionTuple;
import com.motorola.contextual.smartnetwork.db.table.SignalStrengthTable;
import com.motorola.contextual.smartnetwork.db.table.SignalStrengthTuple;
import com.motorola.contextual.smartnetwork.db.table.TopLocationTuple;

public class NetworkSession implements Cloneable {
    private static final String TAG = NetworkSession.class.getSimpleName();
    private static final boolean LOGD = true;

    public static final String SIGNAL_SCREEN_OFF = "screen_off";
    public static final String SIGNAL_SCREEN_ON = "screen_on";
    public static final String SIGNAL_TYPE_GSM = "gsm";
    public static final String SIGNAL_TYPE_CDMA = "cdma";
    public static final String SIGNAL_LEVEL_UNKNOWN = "unknown";

    public static final String DATA_DISCONNECTED = "DATA_DISCONNECTED";
    public static final String DATA_CONNECTING = "DATA_CONNECTING";
    public static final String DATA_CONNECTED = "DATA_CONNECTED";
    public static final String DATA_SUSPENDED = "DATA_SUSPENDED";

    public static final int CDMA_SIGNAL_GREAT = -75;
    public static final int CDMA_SIGNAL_GOOD = -85;
    public static final int CDMA_SIGNAL_MID = -95;
    public static final int CDMA_SIGNAL_POOR = -100;

    public static final int GSM_SIGNAL_MIN = 0;
    public static final int GSM_SIGNAL_MAX = 98;
    public static final int GSM_SIGNAL_GREAT = 16;
    public static final int GSM_SIGNAL_GOOD = 8;
    public static final int GSM_SIGNAL_MID = 4;

    // checkin constants
    public static final String CHECKIN_TAG = "MOT_CA_SMART_NETWORK";
    public static final String CHECKIN_VERSION = "0.2";
    // checkin event
    public static final String EVENT_NAME = "NetworkMonitor";
    public static final String EVENT_FIELD_START_TIME = "start";
    public static final String EVENT_FIELD_END_TIME = "end";
    public static final String EVENT_FIELD_DURATION = "dur";
    public static final String EVENT_FIELD_LOCATION_ID = "loc_id";
    public static final String EVENT_FIELD_RANK = "rank";
    public static final String EVENT_FIELD_TIME_SPENT = "accum";
    public static final String EVENT_FIELD_RANK_UPDATED = "rank_updated";
    // checkin segment
    public static final String SEGMENT_NETWORK = "network";
    public static final String SEGMENT_FIELD_NETWORK = "type";
    public static final String SEGMENT_FIELD_COUNT = "count";
    public static final String SEGMENT_FIELD_DURATION = "duration";
    public static final String SEGMENT_BOUNCE = "bounce";
    public static final String SEGMENT_FIELD_FROM = "from";
    public static final String SEGMENT_FIELD_TO = "to";

    public long mSessionId;
    public long mStartTime, mEndTime, mLocationId, mTimeSpent, mRankUpdated;
    public int mRank;

    public enum SignalStrengthValue {
        SIG_NONE,
        SIG_POOR,
        SIG_MID,
        SIG_GOOD,
        SIG_GREAT
    };

    /**
     * Store Network Bounce data
     *
     */
    public static class BounceStat {
        public final String mFrom;
        public final HashMap<String, Integer> mToCount = new HashMap<String, Integer>();

        BounceStat(String from) {
            mFrom = from;
        }

        /**
         * increment the count of TO network
         * @param to the TO network
         */
        void increment(String to) {
            if (mToCount.containsKey(to)) {
                Integer count = mToCount.get(to);
                if (count != null) {
                    mToCount.put(to, count.intValue() + 1);
                }
            } else {
                mToCount.put(to, 1);
            }
        }
    }

    /**
     * Stores signal strength data for a specified network type
     *
     */
    public static class NetworkStats {
        public final String mNetworkType;
        public int mCount = 0; // times switched into this network type
        public long mTotalTime = 0; // time spent in this network type
        public long[] mSignalStrength = new long[SignalStrengthValue.values().length];

        NetworkStats(String networkType) {
            mNetworkType = networkType;
            for (int i = 0; i < mSignalStrength.length; i++) {
                mSignalStrength[i] = 0;
            }
        }

        /**
         * Updates the signal strength bin using the signal strength values in the given location
         * between the given times
         * @param sessionId the session id
         * @param startTime start time
         * @param endTime end time
         */
        private void updateSignalStrength(Context context, final long sessionId,
                                          final long startTime, final long endTime) {
            SignalStrengthTable table = new SignalStrengthTable();
            String[] columns = { SignalStrengthTuple.COL_SIGNAL_TYPE,
                                 SignalStrengthTuple.COL_SIGNAL_LEVEL,
                                 SignalStrengthTuple.COL_TIMESTAMP
                               };
            String selection = SignalStrengthTuple.COL_FK_MONITOR_SESSION + " = ? AND "
                               + SignalStrengthTuple.COL_TIMESTAMP + " >= ? AND "
                               + SignalStrengthTuple.COL_TIMESTAMP + " <= ?";
            String[] selectionArgs = { String.valueOf(sessionId), String.valueOf(startTime),
                                       String.valueOf(endTime)
                                     };

            // get all tuples between start and end time
            List<SignalStrengthTuple> tuples = table.query(context, columns, selection,
                                               selectionArgs, null, null,
                                               SignalStrengthTuple.COL_TIMESTAMP, null);
            if (tuples != null && !tuples.isEmpty()) {
                String prevType = null;
                String prevLevel = null;
                long prevTime = 0;
                String currentType = null;
                String currentLevel = null;
                long currentTime = 0;

                for (SignalStrengthTuple tuple : tuples) {
                    currentType = tuple.getString(SignalStrengthTuple.COL_SIGNAL_TYPE);
                    currentLevel = tuple.getString(SignalStrengthTuple.COL_SIGNAL_LEVEL);
                    currentTime = tuple.getLong(SignalStrengthTuple.COL_TIMESTAMP);
                    if (prevType != null) {
                        if (SIGNAL_SCREEN_OFF.equals(prevType)) {
                            /* ignore everything that came during screen off
                             * until screen on comes in.
                             */
                            if (!SIGNAL_SCREEN_ON.equals(currentType)) {
                                // maintain the SCEEN_OFF state until SCREEN_ON comes in
                                currentType = SIGNAL_SCREEN_OFF;
                            }
                        } else if (SIGNAL_SCREEN_ON.equals(prevType)) {
                            // start fresh, no calculation to be done
                        } else {
                            // find and update the duration of the previous signal strength
                            try {
                                int signalBin = getSignalStrengthBin(prevType,
                                                                     Integer.parseInt(prevLevel));
                                mSignalStrength[signalBin] = mSignalStrength[signalBin] +
                                                             (currentTime - prevTime);
                            } catch (NumberFormatException nfe) {
                                Log.e(TAG, "Unable to get signal level for "
                                      + prevType + ". value: " + prevLevel);
                            }
                        }
                    }
                    prevType = currentType;
                    prevLevel = currentLevel;
                    prevTime = currentTime;
                }
                // update the last signal strength using endTime
                currentTime = endTime;
                if (SIGNAL_TYPE_GSM.equals(prevType) || SIGNAL_TYPE_CDMA.equals(prevType)) {
                    try {
                        int signalBin = getSignalStrengthBin(prevType,
                                                             Integer.parseInt(prevLevel));
                        mSignalStrength[signalBin] = mSignalStrength[signalBin] +
                                                     (currentTime - prevTime);
                    } catch (NumberFormatException nfe) {
                        Log.e(TAG, "Unable to get last signal level for "
                              + prevType + ". value: " + prevLevel);
                    }
                }
            }
        }

        public static int getSignalStrengthBin(String signalType, int signalLevel) {
            SignalStrengthValue value = SignalStrengthValue.SIG_NONE;
            if (SIGNAL_TYPE_GSM.equals(signalType)) {
                if (signalLevel < GSM_SIGNAL_MIN || signalLevel > GSM_SIGNAL_MAX) {
                    value = SignalStrengthValue.SIG_NONE;
                } else if (signalLevel >= GSM_SIGNAL_GREAT) value = SignalStrengthValue.SIG_GREAT;
                else if (signalLevel >= GSM_SIGNAL_GOOD)  value = SignalStrengthValue.SIG_GOOD;
                else if (signalLevel >= GSM_SIGNAL_MID)  value = SignalStrengthValue.SIG_MID;
                else value = SignalStrengthValue.SIG_POOR;
            } else if (SIGNAL_TYPE_CDMA.equals(signalType)) {
                if (signalLevel >= CDMA_SIGNAL_GREAT) value = SignalStrengthValue.SIG_GREAT;
                else if (signalLevel >= CDMA_SIGNAL_GOOD) value = SignalStrengthValue.SIG_GOOD;
                else if (signalLevel >= CDMA_SIGNAL_MID)  value = SignalStrengthValue.SIG_MID;
                else if (signalLevel >= CDMA_SIGNAL_POOR)  value = SignalStrengthValue.SIG_POOR;
                else value = SignalStrengthValue.SIG_NONE;
            }
            return value.ordinal();
        }
    }

    @Override
    protected NetworkSession clone() {
        NetworkSession cloned = new NetworkSession();
        cloned.mSessionId = mSessionId;
        cloned.mStartTime = mStartTime;
        cloned.mEndTime = mEndTime;
        cloned.mLocationId = mLocationId;
        cloned.mTimeSpent = mTimeSpent;
        cloned.mRankUpdated = mRankUpdated;
        cloned.mRank = mRank;
        return cloned;
    }

    /**
     * Logs the start of a network monitoring session and generates a session id
     * @param context
     */
    public void logSessionStart(Context context) {
        MonitorSessionTuple tuple = new MonitorSessionTuple();
        tuple.put(MonitorSessionTuple.COL_FK_TOP_LOCATION, mLocationId);
        tuple.put(MonitorSessionTuple.COL_START_TIME, mStartTime);
        MonitorSessionTable table = new MonitorSessionTable();
        mSessionId = table.insert(context, tuple);
    }

    /**
     * Logs the end of the network monitoring session
     * @param context
     */
    public void logSessionEnd(Context context) {
        MonitorSessionTuple tuple = new MonitorSessionTuple();
        tuple.put(MonitorSessionTuple.COL_END_TIME, mEndTime);

        String whereClause = MonitorSessionTuple.COL_ID + "=?";
        String[] whereArgs = { String.valueOf(mSessionId) };

        MonitorSessionTable table = new MonitorSessionTable();
        int updated = table.update(context, tuple, whereClause, whereArgs);
        if (updated != 1) {
            Log.e(TAG, "Unable to update session end time.");
        }
    }

    /**
     * Clears all session variables
     */
    public void clear() {
        mSessionId = 0;
        mStartTime = 0;
        mEndTime = 0;
        mLocationId = 0;
        mTimeSpent = 0;
        mRankUpdated = 0;
        mRank = 0;
    }

    /**
     * Checks in all data collected during this network monitoring session
     * @param context
     */
    public void checkin(Context context) {
        CheckinEvent event = new CheckinEvent(CHECKIN_TAG, EVENT_NAME, CHECKIN_VERSION);
        event.setValue(EVENT_FIELD_START_TIME, mStartTime);
        event.setValue(EVENT_FIELD_END_TIME, mEndTime);
        event.setValue(EVENT_FIELD_DURATION, mEndTime - mStartTime);
        event.setValue(EVENT_FIELD_LOCATION_ID, mLocationId);
        event.setValue(EVENT_FIELD_RANK, mRank);
        event.setValue(EVENT_FIELD_TIME_SPENT, mTimeSpent);
        event.setValue(EVENT_FIELD_RANK_UPDATED, mRankUpdated);

        // NETWORK segment - network count, duration, signal strength
        Collection<NetworkStats> networkStats = getNetworkStats(context);
        SignalStrengthValue[] sigValues = SignalStrengthValue.values();
        for (NetworkStats networkStat : networkStats) {
            // ignore stats with 0 count or time, which means they just have DISCONNECT msg
            if (networkStat.mCount > 0 && networkStat.mTotalTime > 0) {
                CheckinSegment segment = new CheckinSegment(SEGMENT_NETWORK);
                segment.setValue(SEGMENT_FIELD_NETWORK, networkStat.mNetworkType);
                segment.setValue(SEGMENT_FIELD_COUNT, networkStat.mCount);
                segment.setValue(SEGMENT_FIELD_DURATION, networkStat.mTotalTime);
                for (SignalStrengthValue sigValue : sigValues) {
                    segment.setValue(sigValue.name(),
                                     networkStat.mSignalStrength[sigValue.ordinal()]);
                }
                event.addSegment(segment);
            } else {
                Log.w(TAG, "Lone network: " + networkStat.mNetworkType
                      + ", count: " + networkStat.mCount
                      + ", duration: " + networkStat.mTotalTime);
            }
        }

        // Bounce segment - from network, to network, count
        Collection<BounceStat> bounceStats = getBounceStats(context);
        for (BounceStat bounceStat : bounceStats) {
            // for each bounce stat, generate a new segment of from-to pair
            Set<Map.Entry<String, Integer>> toCountSet = bounceStat.mToCount.entrySet();
            for(Map.Entry<String, Integer> toCount : toCountSet) {
                CheckinSegment segment = new CheckinSegment(SEGMENT_BOUNCE);
                segment.setValue(SEGMENT_FIELD_FROM, bounceStat.mFrom);
                segment.setValue(SEGMENT_FIELD_TO, toCount.getKey());
                segment.setValue(SEGMENT_FIELD_COUNT, toCount.getValue());
                event.addSegment(segment);
            }
        }

        if (LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
            StringBuilder sb = event.serializeEvent();
            if (sb != null) {
                Log.d(TAG, "Checking in: " + sb.toString());
            }
        }
        event.publish(context.getContentResolver());
    }

    /**
     * Get the network stats for this instance
     * @param context
     * @return collection of network stats
     */
    private Collection<NetworkStats> getNetworkStats(Context context) {

        // map each data connection type to a NetworkStats object
        HashMap<String, NetworkStats> networkStatsMap = new HashMap<String, NetworkStats>();
        String[] dataTypes = getDistinctDataTypes(context);

        if (dataTypes != null) {
            for (String dataType : dataTypes) {
                networkStatsMap.put(dataType, new NetworkStats(dataType));
            }
        }

        if (!networkStatsMap.isEmpty()) {
            // select all rows from data connection table that were inserted during this instance
            List<DataConnectionStateTuple> dataTuples = getDataConnectionTuples(context);

            if (dataTuples != null) {
                String currentNetworkType = null;
                String currentState = null;
                String newNetworkType = null;
                String newState = null;
                long stateStartTime = 0;

                // go through each data connection tuple and calculate the values
                for(int i = 0; i < dataTuples.size(); i++) {
                    DataConnectionStateTuple dataTuple = dataTuples.get(i);
                    newNetworkType = dataTuple.getString(DataConnectionStateTuple.COL_NETWORK_TYPE);
                    newState = dataTuple.getString(DataConnectionStateTuple.COL_STATE);

                    if (newNetworkType != null && newState != null) {
                        if (DATA_CONNECTED.equals(newState)) {
                            if (currentNetworkType != null
                                    && !currentNetworkType.equals(newNetworkType)
                                    && DATA_CONNECTED.equals(currentState)) {
                                /* if we didn't get a DISCONNECT for the current state yet but
                                 * just received a CONNECT for a new network type,
                                 * treat this as a DISCONNECT of the old type followed by a CONNECT
                                 * for the new type
                                 */
                                // update stat as if we got a disconnect
                                NetworkStats stats = networkStatsMap.get(currentNetworkType);
                                long stateEndTime = dataTuple.getLong(
                                                        DataConnectionStateTuple.COL_TIMESTAMP);
                                stats.mCount++;
                                stats.mTotalTime += stateEndTime - stateStartTime;
                                stats.updateSignalStrength(context, mSessionId, stateStartTime,
                                                           stateEndTime);
                            }

                            /* it is possible to get CONNECT for the same type, in which case
                             * we keep the existing state start time
                             */
                            if (currentNetworkType == null
                                    || !currentNetworkType.equals(newNetworkType)) {
                                // mark the start time of the new network type
                                currentNetworkType = newNetworkType;
                                stateStartTime = dataTuple.getLong(
                                                     DataConnectionStateTuple.COL_TIMESTAMP);
                            }

                            if (i == dataTuples.size() -1) {
                                /* if this is the last item, there will be no disconnect
                                 * so just add it
                                 */
                                NetworkStats stats = networkStatsMap.get(currentNetworkType);
                                if (stats != null) {
                                    stats.mCount++;
                                    stats.mTotalTime += mEndTime - stateStartTime;
                                    stats.updateSignalStrength(context, mSessionId,
                                                               stateStartTime, mEndTime);
                                } else {
                                    Log.w(TAG, "Unable to find stats for " + currentNetworkType);
                                }
                            }
                        } else if (DATA_DISCONNECTED.equals(newState)) {
                            if (currentNetworkType != null
                                    && currentNetworkType.equals(newNetworkType)) {
                                // update the stats
                                NetworkStats stats = networkStatsMap.get(currentNetworkType);
                                long stateEndTime = dataTuple.getLong(
                                                        DataConnectionStateTuple.COL_TIMESTAMP);
                                stats.mCount++;
                                stats.mTotalTime += stateEndTime - stateStartTime;
                                stats.updateSignalStrength(context, mSessionId, stateStartTime,
                                                           stateEndTime);
                                currentNetworkType = null;
                            }
                        } else {
                            // ignore other states for now
                        }
                        currentState = newState;
                    } else {
                        Log.e(TAG, "Null types and state for data tuple: " + dataTuple.getId());
                    }
                }
            }
        }

        return networkStatsMap.values();
    }


    /**
     * @param context
     * @return a String array of distinct data connection types we saw during the current session
     */
    private String[] getDistinctDataTypes(Context context) {
        String[] dataTypes = null;
        DataConnectionStateTable dataTable = new DataConnectionStateTable();
        // first find out the distinct data connection types we saw
        String[] columns = { DataConnectionStateTuple.COL_NETWORK_TYPE };
        String selection = DataConnectionStateTuple.COL_FK_MONITOR_SESSION + " = ?";
        String[] selectionArgs = { String.valueOf(mSessionId) };
        List<DataConnectionStateTuple> dataTuples = dataTable.query(context, columns, selection,
                selectionArgs, TopLocationTuple.COL_NETWORK_TYPE, null, null, null);
        if (dataTuples != null && !dataTuples.isEmpty()) {
            dataTypes = new String[dataTuples.size()];
            for (int i = 0; i < dataTuples.size(); i++) {
                DataConnectionStateTuple tuple = dataTuples.get(i);
                dataTypes[i] = tuple.getString(DataConnectionStateTuple.COL_NETWORK_TYPE);
            }
        } else {
            Log.e(TAG, "Unable to get data connection types.");
        }
        return dataTypes;
    }

    /**
     * @param context
     * @return list of all data connection tuples at this location during the current session
     */
    private List<DataConnectionStateTuple> getDataConnectionTuples(Context context) {
        String[] columns = { DataConnectionStateTuple.COL_STATE,
                             DataConnectionStateTuple.COL_NETWORK_TYPE,
                             DataConnectionStateTuple.COL_TIMESTAMP
                           };
        String selection = DataConnectionStateTuple.COL_FK_MONITOR_SESSION + " = ?";
        String[] selectionArgs = { String.valueOf(mSessionId) };
        DataConnectionStateTable dataTable = new DataConnectionStateTable();
        return dataTable.query(context, columns, selection, selectionArgs, null, null, null, null);
    }

    /**
     * @param context
     * @return Collection of bounce stats data of the current session
     */
    private Collection<BounceStat> getBounceStats(Context context) {
        List<DataConnectionStateTuple> dataTuples = getDataConnectionTuples(context);
        HashMap<String, BounceStat> bounceMap = new HashMap<String, BounceStat>();
        if (dataTuples != null && !dataTuples.isEmpty()) {
            String fromNetwork = null;
            String fromState = null;
            String toNetwork = null;
            String toState = null;
            BounceStat bounceStat = null;
            // go through each row and count the network switch
            for (DataConnectionStateTuple dataTuple : dataTuples) {
                toNetwork = dataTuple.getString(DataConnectionStateTuple.COL_NETWORK_TYPE);
                toState = dataTuple.getString(DataConnectionStateTuple.COL_STATE);
                if (fromNetwork != null) {
                    // get the bounce stat for this fromNetwork
                    bounceStat = bounceMap.get(fromNetwork);
                    if (bounceStat == null) {
                        // if it doesn't exist, create it
                        bounceStat = new BounceStat(fromNetwork);
                        bounceMap.put(fromNetwork, bounceStat);
                    }

                    // we only need to deal with DATA_CONNECTED to increment count
                    if (DATA_CONNECTED.equals(toState) && toNetwork != null) {
                        if (toNetwork.equals(fromNetwork)) {
                            /* if we connect to the same network type,
                             * increment only if prev stat was disconnected
                             */
                            if (DATA_DISCONNECTED.equals(fromState)) {
                                bounceStat.increment(toNetwork);
                            }
                        } else {
                            /* if we connect to a new network type,
                             * increment regardless of prev state
                             */
                            bounceStat.increment(toNetwork);
                        }
                    }
                }
                fromNetwork = toNetwork;
                fromState = toState;
            }
        }
        return bounceMap.values();
    }
}