
package com.motorola.devicestatistics;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkAndChargeRcv extends BroadcastReceiver {
    private TrafficCollectionService mServiceInstance;
    private static final String TAG = NetworkAndChargeRcv.class.getSimpleName();
    private final static boolean DUMP = true; // log used.

    @Override
    public void onReceive(final Context context, final Intent intent) {
        new Utils.RunInBackgroundThread() {
            public void run() {
                onReceiveImpl(context, intent);
            }
        };
    }

    private void onReceiveImpl(Context context, Intent intent) {
        if (Watchdog.isDisabled()) return;

        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;

        mServiceInstance = TrafficCollectionService.getService();
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (DUMP) Log.d(TAG, "get broadcast: " + action);
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetInfo != null) {
                if (activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE
                        && activeNetInfo.getState() == NetworkInfo.State.CONNECTED) {
                    // need refresh trafficList first. then set.
                    if (mServiceInstance != null) {
                        mServiceInstance.refreshTrafficList();
                        mServiceInstance.setNetworkType(mServiceInstance.MOBILE);
                    }
                } else {
                    if (mServiceInstance != null) {
                        mServiceInstance.refreshTrafficList();
                        mServiceInstance.setNetworkType(mServiceInstance.WIFI);
                    }
                }
            } else {
                if (mServiceInstance != null) {
                    mServiceInstance.refreshTrafficList();
                    mServiceInstance.setNetworkType(mServiceInstance.MOBILE);
                    if (DUMP) Log.d(TAG, "Now active connection is null");
                }
            }
        }
        else if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            if (DUMP) Log.d(TAG, "get broadcast: " + action);
            if (mServiceInstance != null) {
                mServiceInstance.refreshTrafficList();
                mServiceInstance.setCharging(true);
            }
        }
        else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            if (DUMP) Log.d(TAG, "get broadcast: " + action);
            if (mServiceInstance != null) {
                mServiceInstance.refreshTrafficList();
                mServiceInstance.setCharging(false);
            }
        }
        else if (action.equals("android.intent.action.BOOT_COMPLETED")) {
            if (DUMP) Log.d(TAG, "get broadcast: " + action);
            Intent i = new Intent(context, TrafficCollectionService.class);
            context.startService(i);
        }
    }
}
