/*
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * Craig Detter  2010/08/27 IKCTXTAW-19		   Initial version
 */
package com.motorola.contextual.virtualsensor.locationsensor;

import android.content.Context;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

import com.motorola.android.wrapper.SystemWrapper;
import com.motorola.android.wrapper.TelephonyManagerWrapper;
import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;

/**
 *<code><pre>
 * CLASS:
 *  implements callback listeners for telephony network
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
public class MotoTelephonyStateListener extends PhoneStateListener {
    public static final String TAG = "PhoneStateListener";

    //private static int LISTEN_TO_EVENTS = LISTEN_SIGNAL_STRENGTH | LISTEN_CELL_LOCATION;
    private static int LISTEN_TO_EVENTS = LISTEN_CELL_LOCATION;

    private MotoTelephonyListener listener = null;
    private Context ctx;

    /**
     * presumably 0-100, but doc doesn't say
     */
    private GsmCellLocation lastLocationUpdateGsm = null;
    private CdmaCellLocation lastLocationUpdateCdma = null;

    public MotoTelephonyStateListener() {
        super();
    }

    /**
     * register this listener to telephony manager
     * @param context
     * @param listener
     */
    public void registerListener(Context context, MotoTelephonyListener listener) {
        TelephonyManagerWrapper tm = (TelephonyManagerWrapper) SystemWrapper.getSystemService(context, Context.TELEPHONY_SERVICE);
        // TelephonyManager tm  = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(this, LISTEN_TO_EVENTS);
        ctx = context;
        this.listener = listener;
    }

    public void unregisterListener() {
        TelephonyManagerWrapper tm = (TelephonyManagerWrapper) SystemWrapper.getSystemService(ctx, Context.TELEPHONY_SERVICE);
        // TelephonyManager tm  = (TelephonyManager)ctx.getSystemService(Context.TELEPHONY_SERVICE);
        tm.listen(this, LISTEN_NONE);
        LSAppLog.i(TAG, "unregisterListener :: LISTEN_NONE");
    }

    /**
     * @see android.telephony.PhoneStateListener#onCellLocationChanged(android.telephony.CellLocation)
     */
    @Override
    public void onCellLocationChanged(CellLocation location) {
        super.onCellLocationChanged(location);
        if (location instanceof GsmCellLocation) {
            lastLocationUpdateGsm = (GsmCellLocation)location;
            listener.onCellTowerChanged(lastLocationUpdateGsm);
        } else if (location instanceof CdmaCellLocation) {
            lastLocationUpdateCdma = (CdmaCellLocation)location;
            listener.onCellTowerChanged(lastLocationUpdateCdma);
        }
    }

    /**
     * @see android.telephony.PhoneStateListener#onSignalStrengthChanged(int)
     */
    @Override
    public void onSignalStrengthChanged(int asu) {
        // TODO: need to finish this coding
        // listener.onSignalStrengthChangedSignificantly(asu);
    }
}
