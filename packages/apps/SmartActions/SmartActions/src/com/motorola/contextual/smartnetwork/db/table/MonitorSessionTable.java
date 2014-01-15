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

package com.motorola.contextual.smartnetwork.db.table;

import com.motorola.contextual.smartnetwork.db.SmartNetworkDbSchema;


public class MonitorSessionTable extends SmartNetworkDbTable<MonitorSessionTuple> {

    public String getTableName() {
        return SmartNetworkDbSchema.TBL_MONITOR_SESSION;
    }

    @Override
    protected MonitorSessionTuple createNewTuple() {
        return new MonitorSessionTuple();
    }
}
