/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 * w04917 (Brian Lee)        2012/07/09   IKCTXTAW-487    Add unknown state
 */

package com.motorola.contextual.smartnetwork.db.table;

import com.motorola.contextual.smartnetwork.db.SmartNetworkDbSchema;


public class TopLocationTable extends SmartNetworkDbTable<TopLocationTuple> {
    public static final String NETWORK_CONDITION_GOOD = "good";
    public static final String NETWORK_CONDITION_BAD = "bad";
    public static final String NETWORK_CONDITION_VERIFY = "verify";
    public static final String NETWORK_CONDITION_UNKNOWN = "unknown";

    public String getTableName() {
        return SmartNetworkDbSchema.TBL_TOP_LOCATION;
    }

    @Override
    protected TopLocationTuple createNewTuple() {
        return new TopLocationTuple();
    }
}
