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

public class PoiTuple extends Tuple {
    // column names from LocationSensor poi Table
    public static final String COL_POI = "poi"; // UNIQUE
    public static final String COL_LAT = "lat";
    public static final String COL_LGT = "lgt";
    public static final String COL_RADIUS = "radius";
    public static final String COL_POI_TYPE = "poitype";
    public static final String COL_CELL_TOWERS = "celljsons";
    public static final String COL_CONNECTED_WIFI = "wificonnmac";
    public static final String COL_WIFI_SSID = "wifissid";

    public static final String[] COLUMNS = { COL_ID, COL_POI, COL_LAT, COL_LGT, COL_RADIUS,
                                 COL_POI_TYPE, COL_CELL_TOWERS, COL_CONNECTED_WIFI, COL_WIFI_SSID
                                           };

    public PoiTuple() {
        super(COLUMNS);
    }
}
