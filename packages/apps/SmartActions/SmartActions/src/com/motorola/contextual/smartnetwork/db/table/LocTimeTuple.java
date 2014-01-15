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

package com.motorola.contextual.smartnetwork.db.table;

import com.motorola.contextual.smartnetwork.db.Tuple;

public class LocTimeTuple extends Tuple {
    // column names from LocationSensor LocTime Table
    public static final String COL_WIFI_SSID = "wifissid";
    public static final String COL_CELL_TOWER = "CellJsonValue";
    public static final String COL_START_TIME = "StartTime";
    public static final String COL_END_TIME = "EndTime";
    public static final String COL_DURATION = "Count";

    public static final String[] COLUMNS = { COL_ID, COL_WIFI_SSID, COL_CELL_TOWER, COL_START_TIME,
                                 COL_END_TIME, COL_DURATION
                                           };

    public LocTimeTuple() {
        super(COLUMNS);
    }
}
