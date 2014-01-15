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


public class SignalStrengthTable extends SmartNetworkDbTable<SignalStrengthTuple> {

    public String getTableName() {
        return SmartNetworkDbSchema.TBL_SIGNAL_STRENGTH;
    }

    @Override
    protected SignalStrengthTuple createNewTuple() {
        return new SignalStrengthTuple();
    }

}
