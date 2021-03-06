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

package com.motorola.contextual.smartnetwork.db.table;

import com.motorola.contextual.smartnetwork.db.Tuple;

public class SignalStrengthTuple extends Tuple {
    public static final String[] COLUMNS = { COL_ID, COL_FK_MONITOR_SESSION, COL_SIGNAL_TYPE,
                                 COL_SIGNAL_LEVEL, COL_TIMESTAMP
                                           };

    public SignalStrengthTuple() {
        super(COLUMNS);
    }
}
