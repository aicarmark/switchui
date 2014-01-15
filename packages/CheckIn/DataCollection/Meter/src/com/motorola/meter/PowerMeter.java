/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2011/05/06   IKCTXTAW-272    Initial version
 *
 */

package com.motorola.meter;

import android.content.Context;

/** Parent class of all Meters related to Power measurement.
 * This allows us to add any power-specific functions
 * to all power meters in a general way, if needed
 *
 * @author w04917 (Brian Lee)
 */
public abstract class PowerMeter extends Meter {

    public PowerMeter(Context context) {
        super(context);
    }
}
