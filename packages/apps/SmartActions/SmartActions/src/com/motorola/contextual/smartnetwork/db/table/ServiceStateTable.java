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

import com.motorola.contextual.smartnetwork.db.SmartNetworkDbSchema;


public class ServiceStateTable extends SmartNetworkDbTable<ServiceStateTuple> {

    public String getTableName() {
        return SmartNetworkDbSchema.TBL_SERVICE_STATE;
    }

    @Override
    protected ServiceStateTuple createNewTuple() {
        return new ServiceStateTuple();
    }

}
