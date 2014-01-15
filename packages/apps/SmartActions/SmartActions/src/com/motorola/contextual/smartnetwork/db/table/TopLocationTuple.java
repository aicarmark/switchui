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

public class TopLocationTuple extends Tuple {
    public static final String[] COLUMNS = { COL_ID, COL_POI, COL_WIFI_SSID, COL_CELL_TOWERS,
                                 COL_NETWORK_CONDITION, COL_PREVIOUS_NETWORK_CONDITION, COL_NETWORK_CONDITION_UPDATED,
                                 COL_TIME_SPENT, COL_RANK, COL_RANK_UPDATED
                                           };

    public TopLocationTuple() {
        super(COLUMNS);
    }
}
