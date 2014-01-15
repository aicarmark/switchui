package com.motorola.datacollection.freewifi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.motorola.datacollection.Utilities;
import com.motorola.datacollection.Watchdog;

/* FORMAT
 *      [ID=DC_FRWIFI;wf=xxx;ver=xx;time=xx ;ql=xx]
 *      wf = SSID (Name ) of the wifi network
 *      ql = quality of the signal see the mapping in fun getSignalQualityFromLevel
 */
public class WifiScanBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "DCE_WifiScanBroadcastReceiver";
    private static final boolean LOGD = Utilities.LOGD;
    private static final String APP_VER = Utilities.EVENT_LOG_VERSION;
    private static final long WIFI_LOG_DURATION_INMILLISEC = 2 * 3600000; // 3600000;
    private static final int MAX_WIFI_SSID = 5;
    private static long sWifiLogTime;
    private static HashMap<String, Integer> sWifiSignal = new HashMap<String, Integer>();
    private static HashMap<String, Integer> sWifiCount = new HashMap<String, Integer>();

    /*
     * This implementation relies only on , if user have switched on Wifi
     * scanning and not yet connected with any wifi network. In future this
     * can be modified to scan in some specific internal of time and check
     * if there was any "FREE" wifi network available and user did not
     * connect to it. The sample code to this this explicit scan is as
     * follows // Setup WiFi and start scan wifi = (WifiManager)
     * getSystemService(Context.WIFI_SERVICE); wifi.startScan();
     */

    @Override
    public void onReceive(Context context, final Intent intent) {
        // Called from main thread
        // ANRs were reported for SCAN_RESULTS intent, hence running on background thread
        new Utilities.BackgroundRunnable() {
            public void run() {
                onReceiveImpl( intent );
            }
        };
    }

    private final void onReceiveImpl(Intent intent) {
        // Called from background thread
        if (Watchdog.isDisabled()) return;
        long timeMs = System.currentTimeMillis();
        StringBuffer logOnlySB = null;
        if (LOGD) logOnlySB = new StringBuffer();

        if (intent == null)
            return;
        String action = intent.getAction();
        if (action == null)
            return;

        if (sWifiLogTime == 0) { // to ensure that for the very first time don't check in anything
            sWifiLogTime = System.currentTimeMillis();
        }

        WifiManager wifi = (WifiManager) Utilities.getContext()
                .getSystemService(Context.WIFI_SERVICE);

        if (wifi == null) {
            if (LOGD) { Log.d (TAG, "wifi is NULL. Returning..."); }
            return;
        }
        List<ScanResult> results = wifi.getScanResults();
        if (results != null) {
            for (ScanResult result : results) {
                // Remove the [ESS] that started cropping up from ICS baseline
                result.capabilities = result.capabilities.replace("[ESS]", "");

                if (result.capabilities.equalsIgnoreCase("")
                        || result.capabilities.equalsIgnoreCase(" ")) {

                    if (LOGD) {
                        logOnlySB.append("WIFI SSID:").append(result.SSID)
                                .append(",BSSID:").append(result.BSSID)
                                .append(",capabilities:")
                                .append(result.capabilities).append(",level:")
                                .append(result.level).append(",frequency:")
                                .append(result.frequency);

                        Log.d(TAG,
                                "The Free Wifi  Info is " + logOnlySB.toString());
                    }

                    if (sWifiCount.containsKey(result.SSID)) {
                        // lets modify the wifi signal hashmap entry with
                        // weighted mean
                        int weightedSignalStrength;
                        weightedSignalStrength = (sWifiSignal.get(result.SSID)
                                * sWifiCount.get(result.SSID) + result.level)
                                / (sWifiCount.get(result.SSID) + 1);
                        sWifiSignal.put(result.SSID, weightedSignalStrength);
                        // get number of occurrences for this wifi n/w,increment
                        // it and put back again
                        sWifiCount.put(result.SSID,
                                sWifiCount.get(result.SSID) + 1);

                    } else {
                        // this is first time we see this network, set value '1'
                        sWifiCount.put(result.SSID, 1);
                        sWifiSignal.put(result.SSID, result.level);
                    }

                    if (LOGD) {
                        Log.d(TAG,
                                "The Free Wifi not being checked in Info is "
                                        + logOnlySB.toString());
                        Log.d(TAG,
                                "The Free Wifi log will be checked in after "
                                        + (WIFI_LOG_DURATION_INMILLISEC - (System
                                                .currentTimeMillis() - sWifiLogTime))
                                        / (1000 * 60) + " minutes");
                        dumpMap(sWifiCount);
                        dumpMap(sWifiSignal);

                    }
                }
            }
        }

        // Log into Checkin DB
        if ((System.currentTimeMillis() - sWifiLogTime) >= WIFI_LOG_DURATION_INMILLISEC) {
            int numberOfSSID = 0;
            List<String> frwifi = new ArrayList<String>();
            for (Iterator<String> i = sortByValue(sWifiCount)
                    .iterator(); i.hasNext();) {

                // It does not makes sense to send all the free
                // wifi's.
                if (++numberOfSSID > MAX_WIFI_SSID)
                    break;

                String key = i.next();
                try {
                    // key can contain unsupported characters, hence urlencode it
                    frwifi.add("wf");
                    frwifi.add(URLEncoder.encode (key, "US-ASCII"));
                    frwifi.add("ql");
                    frwifi.add(getSignalQualityFromLevel(sWifiSignal.get(key)));
                } catch (UnsupportedEncodingException e) {
                    Log.e( TAG, Log.getStackTraceString(e) );
                    numberOfSSID = 0;
                    break; // fall through and clear sWifiCount/sWifiSignal
                }

            }

            // IKMAIN-30830, log only if at least 1 SSID is present
            if ( numberOfSSID > 0 ) {
                Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_4, "DC_FRWIFI", APP_VER, timeMs,
                        frwifi.toArray(new String[0]));
            }
            sWifiLogTime = System.currentTimeMillis();

            //clear the haspMaps for a new beginning
            sWifiSignal.clear();
            sWifiCount.clear();

            if (LOGD) {
                Log.d(TAG, "Free wifi event = checked-in time= " + sWifiLogTime);
            }
        }
    }

    private static void dumpMap(Map<String, Integer> mp) {
        // Called from background thread
        StringBuffer dumpMapSB = new StringBuffer();
        Iterator<Map.Entry<String, Integer>> it = mp.entrySet().iterator();
        int count = 0;
        dumpMapSB.append("------------------");
        while (it.hasNext()) {
            Map.Entry<String, Integer> pairs = it.next();
            dumpMapSB.append("\n entry" + count++ + ":" + pairs.getKey()
                    + " = " + pairs.getValue());
        }
        dumpMapSB.append("\n-------------------");
        Log.d(TAG, dumpMapSB.toString());

    }

    private static List<String> sortByValue(final Map<String, Integer> m) {
        // Called from background thread
        List<String> keys = new ArrayList<String>();
        keys.addAll(m.keySet());
        Collections.sort(keys, new Comparator<Object>() {
            @SuppressWarnings("unchecked")
            public int compare(Object o1, Object o2) {
                Object v1 = m.get(o1);
                Object v2 = m.get(o2);
                if (v1 == null || v2 == null) {
                    return 0;
                } else {
                    return ((Comparable<Object>) v2).compareTo(v1);
                }

            }
        });
        return keys;
    }

    private static String getSignalQualityFromLevel(int level) {
        // Called from background thread
        String signalQuality = null;
        if (level >= -60)
            signalQuality = "VST"; // this if for very strong.
        if (level >= -70 && level < -60)
            signalQuality = "ST"; // this if for strong.
        if (level >= -80 && level < -70)
            signalQuality = "MOD"; // this if for moderate.
        if (level >= -85 && level < -80)
            signalQuality = "PR";// this is for poor.
        if (level < -85)
            signalQuality = "WK"; // this is for weak.
        return signalQuality;

    }
}
