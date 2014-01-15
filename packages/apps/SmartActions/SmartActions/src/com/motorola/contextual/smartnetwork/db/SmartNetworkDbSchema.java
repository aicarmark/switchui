/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 * w04917 (Brian Lee)        2012/07/09   IKCTXTAW-487    Add MonitorSession
 */

package com.motorola.contextual.smartnetwork.db;

public interface SmartNetworkDbSchema extends DbSyntax {

    // row id
    public static final String COL_ID = "_id";

    /* TopLocation table
     * Stores information about top locations found in LocTime table
     */
    public static final String TBL_TOP_LOCATION = "TopLocation";
    public static final String COL_POI = "poi";
    public static final String COL_WIFI_SSID = "wifi_ssid";
    public static final String COL_CELL_TOWERS = "cell_towers";
    public static final String COL_NETWORK_CONDITION = "network_cond";
    public static final String COL_PREVIOUS_NETWORK_CONDITION = "prev_network_cond";
    public static final String COL_NETWORK_CONDITION_UPDATED = "network_condition_updated";
    public static final String COL_TIME_SPENT = "time_spent";
    public static final String COL_RANK = "rank";
    public static final String COL_RANK_UPDATED = "rank_and_time_updated";
    static final String CREATE_TBL_TOP_LOCATION =
        CREATE_TABLE + TBL_TOP_LOCATION + LP
        + COL_ID + INTEGER_TYPE + PRIMARY_KEY + AUTO_INCREMENT + CONT
        + COL_POI + TEXT_TYPE + UNIQUE + NOT_NULL + CONT
        + COL_WIFI_SSID + TEXT_TYPE + UNIQUE + NOT_NULL + CONT
        + COL_CELL_TOWERS + TEXT_TYPE + CONT
        + COL_NETWORK_CONDITION + TEXT_TYPE + NOT_NULL + CONT
        + COL_PREVIOUS_NETWORK_CONDITION + TEXT_TYPE + CONT
        + COL_NETWORK_CONDITION_UPDATED + DATE_TIME_TYPE + NOT_NULL + CONT
        + COL_TIME_SPENT + INTEGER_TYPE + NOT_NULL + CONT
        + COL_RANK + INTEGER_TYPE + NOT_NULL + CONT
        + COL_RANK_UPDATED + DATE_TIME_TYPE + NOT_NULL + RP;

    /* MonitorSession table
     *
     */
    public static final String TBL_MONITOR_SESSION = "MonitorSession";
    public static final String COL_FK_TOP_LOCATION = FK + TBL_TOP_LOCATION;
    public static final String COL_START_TIME = "start_time";
    public static final String COL_END_TIME = "end_time";
    static final String CREATE_TBL_MONITOR_SESSION =
        CREATE_TABLE + TBL_MONITOR_SESSION + LP
        + COL_ID + INTEGER_TYPE + PRIMARY_KEY + AUTO_INCREMENT + CONT
        + COL_FK_TOP_LOCATION + INTEGER_TYPE + NOT_NULL + REFERENCES
        + TBL_TOP_LOCATION + LP + COL_ID + RP
        + ON_DELETE_CASCADE + ON_UPDATE_CASCADE + CONT
        + COL_START_TIME + DATE_TIME_TYPE + NOT_NULL + CONT
        + COL_END_TIME + DATE_TIME_TYPE + NOT_NULL + DEFAULT_0 + RP;
    /* index on COL_FK_TOP_LOCATION */
    static final String INDEX_FK_TOP_LOCATION_SESSION_TBL = "IndexFkTopLocationSessionTbl";
    static final String CREATE_INDEX_FK_TOP_LOCATION_ON_TBL_MONITOR_SESSION =
        CREATE_INDEX + INDEX_FK_TOP_LOCATION_SESSION_TBL + ON + TBL_MONITOR_SESSION + LP
        + COL_FK_TOP_LOCATION + RP;
    /* index on COL_START_TIME */
    static final String INDEX_START_TIME_SESSION_TBL = "IndexStartTimeSessionTbl";
    static final String CREATE_INDEX_START_TIME_TBL_MONITOR_SESSION =
        CREATE_INDEX + INDEX_START_TIME_SESSION_TBL + ON + TBL_MONITOR_SESSION + LP
        + COL_START_TIME + RP;

    /* DataConnectionState table
     *
     */
    public static final String TBL_DATA_CONNECTION_STATE = "DataConnectionState";
    public static final String COL_FK_MONITOR_SESSION = FK + TBL_MONITOR_SESSION;
    public static final String COL_STATE = "state";
    public static final String COL_NETWORK_TYPE = "network_type";
    public static final String COL_TIMESTAMP = "timestamp";
    static final String CREATE_TBL_DATA_CONNECTION_STATE =
        CREATE_TABLE + TBL_DATA_CONNECTION_STATE + LP
        + COL_ID + INTEGER_TYPE + PRIMARY_KEY + AUTO_INCREMENT + CONT
        + COL_FK_MONITOR_SESSION + INTEGER_TYPE + NOT_NULL + REFERENCES
        + TBL_MONITOR_SESSION + LP + COL_ID + RP
        + ON_DELETE_CASCADE + ON_UPDATE_CASCADE + CONT
        + COL_STATE + TEXT_TYPE + NOT_NULL + CONT
        + COL_NETWORK_TYPE + TEXT_TYPE + NOT_NULL + CONT
        + COL_TIMESTAMP + DATE_TIME_TYPE + NOT_NULL + RP;
    /* index on COL_FK_MONITOR_SESSION */
    static final String INDEX_FK_MONITOR_SESSION_DATA_TBL = "IndexFkMonitorSessionDataTbl";
    static final String CREATE_INDEX_FK_MONITOR_SESSION_ON_TBL_DATA_CONNECTION_STATE =
        CREATE_INDEX + INDEX_FK_MONITOR_SESSION_DATA_TBL + ON + TBL_DATA_CONNECTION_STATE + LP
        + COL_FK_MONITOR_SESSION + RP;

    /* ServiceState table
     *
     */
    public static final String TBL_SERVICE_STATE = "ServiceState";
    static final String CREATE_TBL_SERVICE_STATE =
        CREATE_TABLE + TBL_SERVICE_STATE + LP
        + COL_ID + INTEGER_TYPE + PRIMARY_KEY + AUTO_INCREMENT + CONT
        + COL_FK_MONITOR_SESSION + INTEGER_TYPE + NOT_NULL + REFERENCES
        + TBL_MONITOR_SESSION + LP + COL_ID + RP
        + ON_DELETE_CASCADE + ON_UPDATE_CASCADE + CONT
        + COL_STATE + TEXT_TYPE + NOT_NULL + CONT
        + COL_TIMESTAMP + DATE_TIME_TYPE + NOT_NULL + RP;
    /* index on COL_FK_MONITOR_SESSION */
    static final String INDEX_FK_MONITOR_SESSION_SERVICE_TBL = "IndexFkMonitorSessionServiceTbl";
    static final String CREATE_INDEX_FK_MONITOR_SESSION_ON_TBL_SERVICE_STATE =
        CREATE_INDEX + INDEX_FK_MONITOR_SESSION_SERVICE_TBL + ON + TBL_SERVICE_STATE + LP
        + COL_FK_MONITOR_SESSION + RP;

    /* SignalStrength table
     *
     */
    public static final String TBL_SIGNAL_STRENGTH = "SignalStrength";
    public static final String COL_SIGNAL_TYPE = "signal_type";
    public static final String COL_SIGNAL_LEVEL = "signal_level";
    static final String CREATE_TBL_SIGNAL_STRENGTH =
        CREATE_TABLE + TBL_SIGNAL_STRENGTH + LP
        + COL_ID + INTEGER_TYPE + PRIMARY_KEY + AUTO_INCREMENT + CONT
        + COL_FK_MONITOR_SESSION + INTEGER_TYPE + NOT_NULL + REFERENCES
        + TBL_MONITOR_SESSION + LP + COL_ID + RP
        + ON_DELETE_CASCADE + ON_UPDATE_CASCADE + CONT
        + COL_SIGNAL_TYPE + TEXT_TYPE + NOT_NULL + CONT
        + COL_SIGNAL_LEVEL + TEXT_TYPE + NOT_NULL + CONT
        + COL_TIMESTAMP + DATE_TIME_TYPE + NOT_NULL + RP;
    /* index on COL_FK_MONITOR_SESSION */
    static final String INDEX_FK_MONITOR_SESSION_SIGNAL_TBL = "IndexFkMonitorSessionSignalTbl";
    static final String CREATE_INDEX_FK_MONITOR_SESSION_ON_TBL_SIGNAL_STRENGTH =
        CREATE_INDEX + INDEX_FK_MONITOR_SESSION_SIGNAL_TBL + ON + TBL_SIGNAL_STRENGTH + LP
        + COL_FK_MONITOR_SESSION + RP;

    static final String[] CREATE_DATABASE = {
        CREATE_TBL_TOP_LOCATION,

        CREATE_TBL_MONITOR_SESSION,
        CREATE_INDEX_FK_TOP_LOCATION_ON_TBL_MONITOR_SESSION,
        CREATE_INDEX_START_TIME_TBL_MONITOR_SESSION,

        CREATE_TBL_DATA_CONNECTION_STATE,
        CREATE_INDEX_FK_MONITOR_SESSION_ON_TBL_DATA_CONNECTION_STATE,

        CREATE_TBL_SERVICE_STATE,
        CREATE_INDEX_FK_MONITOR_SESSION_ON_TBL_SERVICE_STATE,

        CREATE_TBL_SIGNAL_STRENGTH,
        CREATE_INDEX_FK_MONITOR_SESSION_ON_TBL_SIGNAL_STRENGTH
    };
}
