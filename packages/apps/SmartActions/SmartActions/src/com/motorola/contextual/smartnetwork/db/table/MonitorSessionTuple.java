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

import com.motorola.contextual.smartnetwork.db.Tuple;

public class MonitorSessionTuple extends Tuple {
    public static final String[] COLUMNS = { COL_ID, COL_FK_TOP_LOCATION, COL_START_TIME,
                                 COL_END_TIME
                                           };

    public MonitorSessionTuple() {
        super(COLUMNS);
    }
}
