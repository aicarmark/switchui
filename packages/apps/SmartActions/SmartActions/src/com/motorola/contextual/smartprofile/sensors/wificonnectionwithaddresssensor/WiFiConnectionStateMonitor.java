package com.motorola.contextual.smartprofile.sensors.wificonnectionwithaddresssensor;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.motorola.contextual.smartrules.monitorservice.CommonStateMonitor;

/**
 * This class extends {@link CommonStateMonitor} and overrides methods related
 * to registered broadcast receiver {@link WiFiConnectionReceiver}
 *
 * @author wkh346
 *
 */
public class WiFiConnectionStateMonitor extends CommonStateMonitor implements
        WiFiNetworksRuleConstants {

    private static final String TAG = WiFiConnectionStateMonitor.class
            .getSimpleName();

    private static BroadcastReceiver mReceiver = null;

    @Override
    public BroadcastReceiver getReceiver() {
        if (mReceiver == null) {
            mReceiver = new WiFiConnectionReceiver();
        }
        return mReceiver;
    }

    @Override
    public void setReceiver(BroadcastReceiver receiver) {
        if (LOG_INFO) {
            Log.i(TAG, "setReceiver setting receiver to " + receiver);
        }
        mReceiver = receiver;
    }

    @Override
    public String getType() {
        return RECEIVER;
    }

    @Override
    public ArrayList<String> getStateMonitorIdentifiers() {
        ArrayList<String> intentActions = new ArrayList<String>();
        intentActions.add(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        return intentActions;
    }

}
