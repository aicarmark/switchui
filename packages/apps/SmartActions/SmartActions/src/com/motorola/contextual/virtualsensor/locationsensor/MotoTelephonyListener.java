/*
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2010/08/27 IKCTXTAW-19		   Initial version
 */
package com.motorola.contextual.virtualsensor.locationsensor;

import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

/**
 *<code><pre>
 * CLASS:
 *  telephony network callback interface
 *
 * RESPONSIBILITIES:
 *
 * COLABORATORS:
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */

public interface MotoTelephonyListener {

    public void onSignalStrengthChangedSignificantly(int signalStrength);

    public void onCellTowerChanged(GsmCellLocation location);

    public void onCellTowerChanged(CdmaCellLocation location);

}
