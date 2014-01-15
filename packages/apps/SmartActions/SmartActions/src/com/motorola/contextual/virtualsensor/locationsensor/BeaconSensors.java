/*
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2010/08/27 IKCTXTAW-19        Initial version
 */
package com.motorola.contextual.virtualsensor.locationsensor;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.*;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;

import com.motorola.android.wrapper.SystemWrapper;
import com.motorola.android.wrapper.WifiManagerWrapper;
import com.motorola.contextual.cache.SystemProperty;
import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *<code><pre>
 * CLASS:
 *  implements Beacon(wifi and bt) scan logic for better location accuracy and better detection.
 *
 * RESPONSIBILITIES:
 *  turn on wifi and initiate wifi scan if possible.
 *
 * COLABORATORS:
 *   Location sensor manager and location detection.
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */
public final class BeaconSensors {
    public static final String TAG = "LSAPP_Beacon";

    private static final IntentFilter mWifiStautsFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
    private static final IntentFilter mWifiScanResultFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
    private static final IntentFilter mBTDeviceFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    private static final IntentFilter mBTScanDoneFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

    private LocationSensorManager mLSMan;

    // a list of message handler from the upper layer apps so we can send message to them. Now mainly lsman and detection.
    private List<Handler> mCallerHandlers;

    private AtomicInteger mWifiConnState;   // wifi connection state, disconnected(0), connected(1),
    private boolean mWifiScanInitedByMe = false;  // when wifi scan result came by, only take the one inited by me.

    // Algorithm: Automated self driven module to populate and parse {cell, surrounding} table.
    // 1. always use cell json string as key, and a string set of address as value.
    // 2. reverse lookup is hard, not a use case at all if cell id always available, e.g., which loc this ssid belongs to ?
    // 3. convert string
    public WiFiScanResultSet mWifiScanResult;     // contains latest scanned wifi bssid and ssid and encapsulated id comparation logic there.
    long mMyLastScanTimestamp;	// read-only outside the module.
    private String mPOI;        // the poi tag we are matching against.

    WifiManagerWrapper mWifiMan;       // Wifi manager
    protected List<ScanResult> mAPs;
    private enum WifiState { UNINITIALIZED, DISABLE, ENABLE }   // my wifi status
    private boolean mWifiOnByMe = false;

    private BluetoothAdapter mBTAdapter;  // bluetooth adapter.

    @SuppressWarnings("unused")
    private BeaconSensors() {} // hide default constructor

    /**
     * constructor, remember the reference to location sensor manager and telephone monitor.
     * @param lsman
     * @param telmon
     */
    public BeaconSensors(LocationSensorManager lsman, TelephonyMonitor telmon) {
        mLSMan = lsman;

        //mWifiMan = (WifiManager)mLSMan.getSystemService(Context.WIFI_SERVICE); // should always have bt
        mWifiMan = (WifiManagerWrapper)SystemWrapper.getSystemService((Context)mLSMan, Context.WIFI_SERVICE);

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBTAdapter != null) {
            /* first, set the bluetooth name for pairing */
            // mBTAdapter.setName(TAG); // IKSTABLEFIVE-1147, can not randomly set device name.
            LSAppLog.pd(TAG, "BTMan init() successfully");
        } else {
            // Device does not support Bluetooth
            LSAppLog.pd(TAG, "BTMan init() Could not get bluetooth adapter");
        }

        mPOI = null;
        //mCallerHandlers = Collections.synchronizedList(new ArrayList<Handler>());
        mCallerHandlers = new ArrayList<Handler>();  // should use hashMap, put-if-absent idiom ?

        mWifiConnState = new AtomicInteger();
        mWifiConnState.set(0);  // init to disconnected
        mWifiScanResult = new WiFiScanResultSet();  // inited inside constructor.

        mWifiStautsFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mLSMan.registerReceiver(mWifiStatusReceiver, mWifiStautsFilter);
        registerBeaconReceivers();  // always registered to wifi scan.
        mMyLastScanTimestamp = 0;   // init to zero
    }

    /**
     *  Wifi scan result listener
     */
    private BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
			processIntent(intent);
        }
    };

    /**
     * autonomous scan result intent receiver, register with permission so avoid spoofing.
     */
    private BroadcastReceiver mWifiAutonomousScanReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            LSAppLog.i(TAG, "mWifiAutonomousScanReceiver: " + intent.getAction());
            if (AUTONOMOUS_SCAN_RESULTS.equals(intent.getAction())
            ) {  // scan will happen every 5 min.
                processIntent(intent);  // notify the scan result
            }
        }
    };

    /**
     * Wifi status change listener to reset flag to indicate whether wifi turned on by me.
     */
    private BroadcastReceiver mWifiStatusReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            processIntent(intent);
        }
    };

    /**
     * For _EACH_ device, _ONE_ ACTION_FOUND Intent will be broadcast
     */
    private BroadcastReceiver mBTScanReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            processIntent(intent);
        }
    };

    /**
     * register listener every time we start a scan, and un-register once scan done.
     */
    private void registerBeaconReceivers() {
        // all async device scan listeners
        mLSMan.registerReceiver(mWifiScanReceiver, mWifiScanResultFilter);  // register receiver, harmless if already registered
        IntentFilter autoscanFilter = new IntentFilter();
        autoscanFilter.addAction(AUTONOMOUS_SCAN_RESULTS);
        mLSMan.registerReceiver(mWifiAutonomousScanReceiver, autoscanFilter, AUTONOMOUS_SCAN_PERMISSION, null);  // register receiver, harmless if already registered
        //mLSMan.registerReceiver(mBTScanReceiver, mBTDeviceFilter);
        //mLSMan.registerReceiver(mBTScanReceiver, mBTScanDoneFilter);
    }

    /**
     * remove the registered listener.
     */
    private void unregisterBeaconReceivers(BroadcastReceiver recv) {
        try {
            mLSMan.unregisterReceiver(recv);
        } catch (Exception e) {
            LSAppLog.d(TAG, e.toString());
        }
    }

    /**
     * clean up all registered listeners upon cleanup, when object is being destroyed.
     */
    public void cleanupReceivers() {
        try {
            mLSMan.unregisterReceiver(mWifiStatusReceiver); // wifi on off status
            mLSMan.unregisterReceiver(mWifiScanReceiver);   // wouldn't hurt if unregister two more times
        } catch (Exception e) {
            LSAppLog.d(TAG, e.toString());
        }
    }

    /**
     * This is the center msg processing logic to handle all async intents sent to this class.
     * Yes, the function is a bit long. But it encapsulate all the intent processing logic in one place.
     * We can do this because intents are different and each intent can only fall into one if branch.
     * so this is like switch with many branches because cannot switch on a value of type String.
     *
     * Todo: discuss with Craig for any possible changes.
     */
    public void processIntent(Intent intent) {
        String action = intent.getAction();

        // Wifi Scan result and stealth results.
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action) ||
                AUTONOMOUS_SCAN_RESULTS.equals(action)) {
            onBeaconScanFinished();  // notify the scan result
        }

        // Wifi enable/disable listener
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {  // Wifi Enabled Disabled
            // all I care is on....if anybody turn on wifi..cancel my pending action for me to always off wifi.
            // ignore state change caused by me ! (on while saved is disable, off when
            if (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0) == WifiManager.WIFI_STATE_ENABLED) {
                LSAppLog.pd(TAG, "mWifiStatusReceiver: get wifi state enable...don't care.");
                LocationSensorApp.sendMessage(mLSMan.getDetection().getDetectionHandler(), WIFI_STATE_CHANGED, null, null);
            } else if (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0) == WifiManager.WIFI_STATE_DISABLED) {
                mWifiOnByMe = false;
                LSAppLog.pd(TAG, "mWifiStatusReceiver: get wifi state disabled...clear and disable myself also!");
                LocationSensorApp.sendMessage(mLSMan.getDetection().getDetectionHandler(), WIFI_STATE_CHANGED, null, null);
            } else if (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0) == WifiManager.WIFI_STATE_DISABLING) {
                LSAppLog.pd(TAG, "mWifiStatusReceiver: get wifi state disabling...wait for disabled!");
            }
        }

        // Wifi connect/disconnect listener
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) { // Wifi connection
            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo != null) {
                if (networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                    if (intent.getStringExtra(WifiManager.EXTRA_BSSID) != null) {
                        mWifiConnState.set(1);
                        LSAppLog.pd(TAG, "mWifiStatusReceiver : Wifi connection state changed...connected!" + intent.getStringExtra(WifiManager.EXTRA_BSSID));
                        notifyWifiConnected(intent.getStringExtra(WifiManager.EXTRA_BSSID));
                    }
                }
                if (networkInfo.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTED) {
                    mWifiConnState.set(0);
                    LSAppLog.pd(TAG, "mWifiStatusReceiver : Wifi connection state changed...disconnected!");
                    notifyWifiConnected(null);   // send null to indicate wifi disconnected.
                }
            }
        }

        // Bluetooth find
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null) {
                LSAppLog.d(TAG, "mBTScanReceiver onRecive() Discovered device :" + device.getName() + ":" + device.getAddress());
            }
        }

        // Bluetooth discover
        if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            LSAppLog.d(TAG, "mBTScanReceiver DISCOVERY_FINISHED");
            unregisterBeaconReceivers(mBTScanReceiver);  // un-register this listener
            //doneBeaconScan(mBTScanReceiver);
        }
    }

    /**
     * This class encapsulate all wifi ids(data) and all the operations on top of those ids.
     * The object is populated from callbacks from wifi events (scan result, conn, etc).
     * 1. storing all wifi ids, ( scanned, connected, mac id, and ssids), and the timestamp.
     * 2. funcitons to compare and match wifi bssids and ssids.
     * 3. extendable for more wifi related information can be stored here.
     */
    public class WiFiScanResultSet {
        List<ScanResult> mAPs;
        Map<String, String> mWifiMap;  // the wifi map of the latest round of scan result. Key is bssid, value is ssid.
        JSONArray mWifiSSID;    // all the ssid of the surrounding wifis
        String mWifiConnMac;    // the connected wifi bssid, you can only have one connected wifi at any time.
        long mLastScanTime;
        int mEmptyScans = 0;

        public WiFiScanResultSet() {
            mWifiMap = Collections.synchronizedMap(new HashMap<String, String>());    // passed in idmap is inited somewhere.
            mWifiSSID = null;
            mWifiConnMac = null;
            mLastScanTime = System.currentTimeMillis();
        }

        @Deprecated
        void updateWithLastestScanResult(Map<String, String> map) {
            mWifiMap = map;    // update the map with the latest, if any.
        }

        /**
         * check to see whether the scan result is stale, as compare to current time
         * @return true if scan result is a minute older, stale. false otherwise.
         */
        public boolean isStale() {
            return System.currentTimeMillis() - mLastScanTime > DateUtils.MINUTE_IN_MILLIS;
        }

        /**
         * pass in a set of bssid, find out whether there is any match of passed in bssid in the latest scanned wifi.
         * @param bssid  the bssid arg normally from db poi information.
         * @return true if there are any matches, false otherwise.
         */
        public boolean matchBssid(Set<String> bssidset) {
            return Utils.fuzzyMatchSets(bssidset, mWifiMap.keySet(), true);
        }

        /**
         * pass in a set of ssid, which is poi's bag ssid, and match them against the latest scanned ssids.
         * @param ssidset  the bag ssid set from poi
         * @return true if there are any matches, false otherwise.
         */
        public boolean matchSsid(Set<String> ssidset) {
            return Utils.fuzzyMatchSetInMapValue(ssidset, mWifiMap) > 0 ? true : false;
        }

        /**
         * get wifi scan result into in memory data structure for quick reference.
         */
        public void getWifiScanResultJson() {
            mAPs = mWifiMan.getScanResults();  // if wifi is off, this could return null
            //mWifiMap.clear();
            //mWifiSSID = new JSONArray();  // this is used to update into db
            JSONArray wifissid = new JSONArray();  // use a tmp object to avoid concurrent modification exception.
            Map<String,String> wifimap = new HashMap<String,String>();  // use a tmp map to avoid concurrent modification

            if (null != mAPs && mAPs.size() > 0) {
                try {
                    for (ScanResult entry : mAPs) {
                        if(entry.level < WIFI_MIN_SS) {
                            continue;
                        }
                        JSONObject entryjson = new JSONObject();
                        entryjson.put(LS_JSON_WIFISSID, entry.SSID);   // should never be empty
                        entryjson.put(LS_JSON_WIFIBSSID, entry.BSSID);
                        entryjson.put(LS_JSON_WIFISS, entry.level);   // record ss, -100 the weaker.
                        wifissid.put(entryjson);
                        //mWifiMap.put(entry.BSSID, entry.SSID);   // put into map.
                        wifimap.put(entry.BSSID, entry.SSID);   // put into tmp map

                        LSAppLog.d(TAG, "getWifiJson: ScanResult :" + entry.BSSID + " " + entry.SSID + " " + entry.level);
                    }
                    mEmptyScans = 0;
                } catch (JSONException e) {
                    LSAppLog.e(TAG, "getWifiJson Exception:" + e.toString());
                    wifissid = null;
                    wifimap.clear();
                    //mWifiMap.clear();
                }
            } else {
                mEmptyScans += 1;
                LSAppLog.i(TAG, "ScanResult : empty result. cnt=" + mEmptyScans);
            }

            // set data after done to avoid concurrent modification exception.
            mWifiSSID = wifissid;
            mWifiMap = wifimap;
            mLastScanTime = System.currentTimeMillis();  // set the freshness
        }

        /**
         * check whether we have empty scan
         */
        public int getEmptyScans() {
            return mEmptyScans;
        }
        /**
         * clear empty scan cnt
         */
        public void clearEmptyScans() {
            mEmptyScans = 0;
        }

        /**
         * setter function for Wifi Scan Result set.
         */
        public void setWifiScanResultJson(String wifiJsonArray){
            try{
                JSONObject entry = null;
                Map<String,String> wifimap = new HashMap<String,String>();  // use a tmp map to avoid concurrent modification
                JSONArray wifissid = new JSONArray(wifiJsonArray);  // use a tmp object to avoid concurrent modification exception.
                for (int i=0; i<wifissid.length(); i++) {
                    entry = wifissid.getJSONObject(i);
                    wifimap.put(entry.getString(LS_JSON_WIFIBSSID), entry.getString(LS_JSON_WIFISSID));   // put into tmp map
                    LSAppLog.d(TAG, "setWifiScanResultJson: " + entry.toString());
                }
                mWifiSSID = wifissid;
                mWifiMap = wifimap;
            }catch(Exception e){
                LSAppLog.d(TAG, "setWifiScanResultJson Exception : " + e.toString());
            }
        }

        /**
         * setter function for the connected mac.
         */
        public void setConnectedWifi(String connmac){
            mWifiConnMac = connmac;
        }
    }

    /**
     * initiate a beacon scan to devices surround this poi
     * called from LSMan after 15 min when POI location is saved...or upon leave POI.
     * @param poi: the poi name when scan happened
     * @param hdl: the msg handle that will process scan results.
     * @return true after async actively drive a scan, note down scan time and init by me flag.
     */
    // TODO the return value here is totally misleading. It doesn't make any
    // sense. Should be removed.
    public boolean startBeaconScan(String poi, Handler hdl) {
        if (SystemProperty.isAirplaneMode()) {
            Log.e(TAG,
                    "Device is in airplane mode. Rejecting wifi scan request.");
            return false;
        }

        addCallerHandler(hdl);

        mPOI = poi;         // concurrently set poi ?

        mWifiScanInitedByMe = true;  // set init by me everytime we do startScan
        mMyLastScanTimestamp = System.currentTimeMillis();   // set timestamp everytime we do scan.

        boolean activescan = true;

        // enable Auto scan if we can
        if(mLSMan.getDetection().getDetectionTimerTask().getPhoneModel().setBackgroundScan(true)) {
            activescan = false;
            LSAppLog.pd(TAG, "startBeaconScan : started background scan as wifi off.");
        }

        mLSMan.holdWakeLock(5000);   // hold wakelock for 5 seconds when start scan.
        // call the common start scan API even when wifi off.
        mWifiMan.startScan();

        // need user's permission to enable bluetooth
        // if (mBTAdapter != null)
        //    mBTAdapter.startDiscovery();  // no effect if bt not enabled by user permission.

        LSAppLog.i(TAG, "startBeaconScan...scan started by me, set last scan time..");

        return activescan;
    }

    /*
     * Android will do wifi scan periodically(5 min) if wifi is on, passively listening to that.
     */
    public boolean startPassiveBeaconListen(Handler hdl) {
        addCallerHandler(hdl);
        return true;
    }

    /**
     * remove the passive wifi scan listener.
     * @param hdl  the message handler of the caller...could be lsman or detection.
     */
    public void stopPassiveBeaconListen(Handler hdl) {
        removeCallerHandler(hdl);
        try {
            // No need to unregister receiver, keep the wifi listener registered in hope to trigger cell tower changes timely.
            // unregisterBeaconReceivers(mWifiScanReceiver);
        } catch (Exception e) {
            LSAppLog.d(TAG, e.toString());
        }
        LSAppLog.d(TAG, "stopPassiveBeaconListen: stop passive wifi scan listening upon curpoi detected or left poi!");
    }

    /**
     * called from location sensor manager beacon scan result processing.
     * @param hdl
     */
    public void removeCallerHandler(Handler hdl) {
        synchronized (mCallerHandlers) {
            if (mCallerHandlers.contains(hdl)) {
                mCallerHandlers.remove(hdl);
            }
            LSAppLog.d(TAG, "RemoveCallerHander : caller:" + hdl + "Callers :" + mCallerHandlers.toString());
        }
    }

    /**
     * the caller want to receive wifi scan result thru message sent to its handler.
     * @param hdl  caller's handler.
     */
    private void addCallerHandler(Handler hdl) {
        synchronized (mCallerHandlers) {
            if (! mCallerHandlers.contains(hdl)) {
                mCallerHandlers.add(hdl);   // add handler as observer anyway
            }
        }
    }

    /**
     * notify all the registered handlers upon wifi scan result thru async message.
     */
    private void notifyAllRegisteredHandlers(int what, Object obj, Bundle data) {   // notify all the callers explicitly registered!
        synchronized (mCallerHandlers) {
            for (Handler hdl : mCallerHandlers) {
                LSAppLog.pd(TAG, "notifyAllCallers : notify caller POI=" + mPOI + " caller:" + hdl);
                LocationSensorApp.sendMessage(hdl, what, obj, data);
            }
        }
    }

    /**
     * Insert scan results into db. driven by scan result available and bt discovery finished from listeners!
     */
    private void onBeaconScanFinished() {
        boolean scanresult = false;

        // we always passively listen to scan result.
        //if (mCallerHandlers.size() == 0) {  // detection will always registered to beacon scan results.
        //    LSAppLog.d(TAG, "onBeaconScanFinished: nobody cares, do nothing...:" + mCallerHandlers.size());
        //    return;
        //}


        // make sure we do pro active wifi scan at most 1 per minute.
        long now = System.currentTimeMillis();
        if(!mLSMan.mDetection.hasPoiAndLocationRules()    // no poi to detect, no active scan, however, cause lastScanTimestamp not update
                || (now - mMyLastScanTimestamp < DateUtils.MINUTE_IN_MILLIS)) {  // my pro active scan gap by at least 1 min
            mWifiScanInitedByMe = true;
        }

        if ( !mWifiScanInitedByMe && mWifiMan.isWifiEnabled()) {  // only do active when wifi on
            mWifiScanInitedByMe = true;
            mMyLastScanTimestamp = now;  // note down the my scan timestamp!
            scanresult = mWifiMan.startScan();  // will get another scan result back
            LSAppLog.d(TAG, "onBeaconScanFinished: start my own scan on top of a stale scan..init by me and set time...return:" + scanresult);
        } else {
            mWifiScanInitedByMe = false;  // my scan is done, reset.
            mWifiScanResult.getWifiScanResultJson();
            notifyAllRegisteredHandlers(BEACONSCAN_RESULT, (Object)mPOI, null);
        }

        mLSMan.releaseWakeLock();  // release wake lock after getting result
    }

    /**
     * notify detection the connected wifi's bssid
     */
    private void notifyWifiConnected(String bssid) {
        mWifiScanResult.mWifiConnMac = bssid;
        LSAppLog.d(TAG, "notifyWifiConnected: " + mWifiScanResult.mWifiConnMac);
        LocationSensorApp.sendMessage(mLSMan.getDetection().getDetectionHandler(), WIFI_CONNECTED, bssid, null);
    }


    /**
     * @param freshonly if set, only return fresh results, otherwise, return null.
     * @return null : empty set, or not fresh when require fresh
     */
    public String getLastWifScanSsid(boolean freshonly) {
        if( mWifiScanResult.mWifiSSID == null ||
                (freshonly && mWifiScanResult.isStale())   // ask for fresh result, return null if cant get fresh
          ) {
            return null;
        }
        return mWifiScanResult.mWifiSSID.toString();
    }

    /**
     * @return a copy of scanned wifi ssids as a map
     */
    public Map<String, String> getLastWifiScanSsidMap() {
        //return Collections.unmodifiableMap(mWifiMap);
        return mWifiScanResult.mWifiMap;
    }

    /**
     * @return the current connected wifi mac
     */
    public String getConnectedWifi() {
        return mWifiScanResult.mWifiConnMac;
    }

    /**
     * No need to worry about success of turning it on, because if it doesn't come on,
     * we won't be burning any extra battery because WiFi listening is passive.
     *
     * Logic table:
     * getWiFiState()   onAction   onByMeFlag  Result
     * ---------------  --------   ----------  ------
     *       ON           ON           false    leave
     *       ON           OFF          true     turnOff
     *       ON           OFF          false    leave
     *       OFF          ON           true     turnOn
     *       OFF          OFF            x      leave
     * The Wifi State listener will reset onByMeFlag is wifi is on by me and user turned it off.
     * No need to worry if not called in pair....screen off will turn WiFi off always.
     *    Log: WifiService: setWifiEnabled enable=false, persist=true, pid=14941, uid=10006
     * @param onAction on or off
     *
     * Deprecated! do not mess with wifi state.
     */
    @Deprecated
    public void setWifi(boolean onAction) {
        WifiState curstate = WifiState.UNINITIALIZED;

        if (onAction == true) {
            mLSMan.getMetricsLogger().logRequestWifiOn();
        } else {
            mLSMan.getMetricsLogger().logRequestWifiOff();
        }

        // reduce states to binary states
        int state = mWifiMan.getWifiState();
        if (state == WifiManager.WIFI_STATE_DISABLED || state == WifiManager.WIFI_STATE_DISABLING) {
            curstate = WifiState.DISABLE;
        } else if (state == WifiManager.WIFI_STATE_ENABLED || state == WifiManager.WIFI_STATE_ENABLING) {
            curstate = WifiState.ENABLE;
        } else {
            LSAppLog.d(TAG, "setWifi: what I am supposed to do if unknown state");
            return;
        }

        // three do nothing situations.
        // 1.on : it it is already on, do nothing.
        // 2.off: if it is not me who turned wifi on, do nothing.
        // 3.Airplane mode on.
        // 4.Wifi hotspot on
        if ( (onAction  && curstate == WifiState.ENABLE)
                || (!onAction && curstate == WifiState.DISABLE)
                || (onAction && Utils.isAirplaneModeOn((Context)mLSMan))
           ) {
            LSAppLog.d(TAG, "setWifi: do nothing : action=" + onAction + ": curWifiState=" + curstate + ": or airplane mode on: or wifi hotspot on.");
            return;
        }

        if (onAction) {
            mWifiOnByMe = true;
            LSAppLog.d(TAG, "setWifi :: ON : saving Current wifi state before enabling : savedstate: " + curstate);
            mWifiMan.setWifiEnabled(true);    // on action
            mLSMan.getMetricsLogger().logActualWifiOn();
        } else if (mWifiOnByMe && mLSMan.getLSManState() != LocationSensorManager.LSManState.TIMEREXPIRED_WAITING4FIX) {
            WifiInfo wifiInfo = mWifiMan.getConnectionInfo();
            if (wifiInfo != null && SupplicantState.COMPLETED == wifiInfo.getSupplicantState()) {
                LSAppLog.d(TAG, "setWifi :: OFF: not off because wifi connection on:" + mWifiMan.getConnectionInfo());
            } else {
                mWifiOnByMe = false;
                mWifiMan.setWifiEnabled(false);   // restore
                mLSMan.getMetricsLogger().logActualWifiOff();
                LSAppLog.d(TAG, "setWifi :: OFF: restoring disabled WIFI : savedstate : " + curstate);
            }
        } else {
            LSAppLog.d(TAG, "setWifi :: OFF: leave wifi on because not turned on by me !");
        }
    }

    /**
     * get the current wifi state, reduce the states to two states, on and off.
     * @return
     */
    public boolean isWifiEnabled() {
        int state = mWifiMan.getWifiState();
        if (state == WifiManager.WIFI_STATE_ENABLED || state == WifiManager.WIFI_STATE_ENABLING) {
            return true;
        }
        return false;
    }

    /**
     * get the current wifi connection state,
     * @return true if wifi supplicant state is COMPLETE, false otherwise
     */
    public boolean isWifiConnected() {
        WifiInfo wifiInfo = mWifiMan.getConnectionInfo();
        if (wifiInfo != null && SupplicantState.COMPLETED == wifiInfo.getSupplicantState()) {
            LSAppLog.d(TAG, "isWifiConnected :: Yes, Wifi state is completed: " + wifiInfo.getSupplicantState());
            return true;
        }
        return false;
    }

    /**
     * return the current wifi conn state by checking class state variable directly.
     * State flag in updated inside wifi state change callback.
     * @return wifi conn state, 0:disconnected, 1:connected.
     */
    public int getWifiConnState() {
        return mWifiConnState.get();
    }
}
