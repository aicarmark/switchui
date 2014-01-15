package com.motorola.devicestatistics;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import  java.util.Arrays;

import com.motorola.devicestatistics.SysClassNetUtils;

public class TelephonyListener extends PhoneStateListener {

    private static final String TAG = "DevStats_TelephonyListener";
    private static final boolean LOGD = false; //ODO:
    private static Context sContext;
    private static TelephonyListener sTelephonyListener;

    synchronized public static final void startTelephonyListeners( Context context ) {
        sContext = context;
        sTelephonyListener = new TelephonyListener();

        TelephonyManager t = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        if (t != null) {
            t.listen( sTelephonyListener,
                LISTEN_SERVICE_STATE | LISTEN_DATA_CONNECTION_STATE );
        } else {
            if ( LOGD ) { Log.d (TAG, "Can't listen to telephony events"); }
        }
    }

     synchronized public static final void stopTelephonyListeners( Context context ) {
         sContext = context;
         TelephonyManager t = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
         if (t != null) {
             t.listen( sTelephonyListener, LISTEN_NONE );
         } else {
             if (LOGD) { Log.d (TAG, "Can't unlisten to Telephony events"); }
         }
     }

     @Override
     public final void onServiceStateChanged(ServiceState serviceState) {
        if ( LOGD ) { Log.d( TAG, "In onServiceStateChanged(), got " + serviceState.getState() ); }
        super.onServiceStateChanged(serviceState);

        updateDataStats();
    }

    @Override
    public void onDataConnectionStateChanged(int state, int networkType) {
        if ( LOGD ) { Log.d( TAG, "In onDataConnectionStateChanged(), got " + state + " " + networkType ); }
        super.onDataConnectionStateChanged(state, networkType);

        updateDataStats();
    }

    private void updateDataStats() {
        SysClassNetUtils.updateNetStats(sContext);
        if (LOGD) {
            String[][] wifiData = SysClassNetUtils.getWifiRxTxBytes(sContext);
            String[][] mobileData = SysClassNetUtils.getMobileRxTxBytes(sContext);
            String[][] wifiPkt = SysClassNetUtils.getWifiRxTxPkts(sContext);
            String[][] mobilePkt = SysClassNetUtils.getMobileRxTxPkts(sContext);
            Log.d(TAG, "After updation data stats : "+
                    Arrays.toString(wifiData)+Arrays.toString(mobileData)
                    + Arrays.toString(wifiPkt)+Arrays.toString(mobilePkt));
        }
    }
}
