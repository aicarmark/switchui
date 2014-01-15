/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.devicestatistics;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import android.content.Context;
import android.net.TrafficStats;
import android.util.Log;

public class SysClassNetUtils {
    final static String TAG = "SysClassNetUtils";
    final static private boolean DUMP = false;
    final static String SYS_CLASS_NET_PATH = "sys/class/net";
    final static String CARRIER = "/carrier";
    final static String RXBYTES = "/statistics/rx_bytes";
    final static String TXBYTES = "/statistics/tx_bytes";

    final static int CUMULATIVE_INDEX = 0;
    final static int OFF_INDEX = 1;
    final static int ON_INDEX = 2;
    final static int MAX_INDEX = 3;
    final static String[] ID_SUFFIX = { "", "SOff", "SOn" };

    final static String[] WIFI_INTERFACES = new String[] {
            "/tiwlan0",
            "/eth0",
            "/wlan0",
    };

    private final static int INVALID_INDEX = -1;
    private final static int PPP_INDEX = 0;
    private final static int RMNET_INDEX = 1;
    private final static int QMI_INDEX = 2;
    private final static int INTERFACE_ARRAY_SIZE = 3;

    private static long mMobileRxBytes;
    private static long mMobileTxBytes;
    private static long mWifiRxBytes;
    private static long mWifiTxBytes;
    private static long mMobileRxPkts;
    private static long mMobileTxPkts;
    private static long mWifiRxPkts;
    private static long mWifiTxPkts;

    private static ScreenStateSaver sWifiLastScreenState;
    private static ScreenStateSaver sMobileLastScreenState;
    private static ScreenStateSaver sWifiPktLastScreenState;
    private static ScreenStateSaver sMobilePktLastScreenState;

    synchronized static final void init(Context context) {
        if ( sWifiLastScreenState == null ) {
            sWifiLastScreenState = new ScreenStateSaver(context);
            sMobileLastScreenState = new ScreenStateSaver(context);
        }
        if (sWifiPktLastScreenState == null)
            sWifiPktLastScreenState = new ScreenStateSaver(context);
        if (sMobilePktLastScreenState == null)
            sMobilePktLastScreenState = new ScreenStateSaver(context);
    }

    /**
     * Checkin data is cumulative, since phone first boot
     */
    public static String[][] getWifiRxTxBytes(Context ctx) {
        DevStatPrefs prefs = DevStatPrefs.getInstance(ctx);
        long[] cumRxBytes = prefs.getWifiCumRxBytes();
        long[] cumTxBytes = prefs.getWifiCumTxBytes();

        String result[][] = new String[MAX_INDEX][];
        for (int i=0; i<MAX_INDEX; i++) {
            // IKCTXTAW-411 : Always report DataSizes even if some or all fields are 0.
            result[i] = new String[] { "wifi_rx", String.valueOf(cumRxBytes[i]),
                    "wifi_tx", String.valueOf(cumTxBytes[i]) };
        }
        return result;
    }

    public static String[][] getMobileRxTxBytes(Context ctx) {
        DevStatPrefs prefs = DevStatPrefs.getInstance(ctx);
        long[] cumRxBytes = prefs.getMobileCumRxBytes();
        long[] cumTxBytes = prefs.getMobileCumTxBytes();

        String result[][] = new String[MAX_INDEX][];
        for (int i=0; i<MAX_INDEX; i++) {
            // IKCTXTAW-411 : Always report DataSizes even if some or all fields are 0.
            result[i] = new String[] { "mobile_rx", String.valueOf(cumRxBytes[i]),
                    "mobile_tx", String.valueOf(cumTxBytes[i]) };
        }
        return result;
    }

    public static String[][] getWifiRxTxPkts(Context ctx) {
        DevStatPrefs prefs = DevStatPrefs.getInstance(ctx);
        long[] cumRxPkts = prefs.getWifiCumRxPkts();
        long[] cumTxPkts = prefs.getWifiCumTxPkts();

        String result[][] = new String[MAX_INDEX][];
        for (int i=0; i<MAX_INDEX; i++) {
            result[i] = new String[] { "wifi_pkt_rx", String.valueOf(cumRxPkts[i]),
                    "wifi_pkt_tx", String.valueOf(cumTxPkts[i]) };
        }
        return result;
    }

    public static String[][] getMobileRxTxPkts(Context ctx) {
        DevStatPrefs prefs = DevStatPrefs.getInstance(ctx);
        long[] cumRxPkts = prefs.getMobileCumRxPkts();
        long[] cumTxPkts = prefs.getMobileCumTxPkts();

        String result[][] = new String[MAX_INDEX][];
        for (int i=0; i<MAX_INDEX; i++) {
            result[i] = new String[] { "mobile_pkt_rx", String.valueOf(cumRxPkts[i]),
                    "mobile_pkt_tx", String.valueOf(cumTxPkts[i]) };
        }
        return result;
    }

    public static void updateNetStats(Context ctx) {
        updateMobileRxTxBytes(ctx);
        updateWifiRxTxBytes(ctx);
        updateMobileRxTxPkts(ctx);
        updateWifiRxTxPkts(ctx);
    }

    public static void updateWifiRxTxBytes(Context ctx) {
        init(ctx);
        int lastScreenState = sWifiLastScreenState.getAndSetScreenState();

        DevStatPrefs prefs = DevStatPrefs.getInstance(ctx);
        long prevRxBytes = prefs.getWifiRxBytes();
        long prevTxBytes = prefs.getWifiTxBytes();
        long currRxBytes = 0;
        long currTxBytes = 0;
        boolean found = false;

        for(int i = 0; i < WIFI_INTERFACES.length; ++i) {
            String wifi = WIFI_INTERFACES[i];
            try {
                // Do not proceed if wifi is down for the next consecutive update
                if (prevRxBytes == 0 && prevTxBytes == 0 && 
                        isNetUp(new File(SYS_CLASS_NET_PATH + wifi)) == false) {
                    continue;
                }
                long[] cumRxBytes = prefs.getWifiCumRxBytes();
                long[] cumTxBytes = prefs.getWifiCumTxBytes();
                getWifiBytesFromSys(wifi);
                currRxBytes = mWifiRxBytes;
                currTxBytes = mWifiTxBytes;

                if (DUMP) {
                    android.util.Log.d(TAG, "updateWifiRxTxBytes;(Before) prevRxBytes=" +
                            prevRxBytes + "prevTxBytes=" + prevTxBytes + "cumRxBytes=" +
                            Arrays.toString(cumRxBytes) + "cumTxBytes=" +
                            Arrays.toString(cumTxBytes) + "currRxBytes=" + currRxBytes +
                            "currTxBytes=" + currTxBytes);
                }

                if (currRxBytes != 0) {
                    long addBytes = currRxBytes>=prevRxBytes? currRxBytes-prevRxBytes : currRxBytes;
                    updateCumulative(lastScreenState, cumRxBytes, addBytes);
                    prefs.setWifiCumRxBytes(cumRxBytes);
                    found = true;
                }
                if (currTxBytes != 0) {
                    long addBytes = currTxBytes>=prevTxBytes? currTxBytes-prevTxBytes : currTxBytes;
                    updateCumulative(lastScreenState, cumTxBytes, addBytes);
                    prefs.setWifiCumTxBytes(cumTxBytes);
                    found = true;
                }
                if(found) break;
            } catch (IOException e) {
                // Nothing to be done
            }
        }
        if(found) {
            prefs.setWifiRxBytes(currRxBytes);
            prefs.setWifiTxBytes(currTxBytes);
        } else {
            sWifiLastScreenState.setLastScreenState(ScreenStateSaver.UNKNOWN);
        }
    }

    public static void updateMobileRxTxBytes(Context ctx) {
        init(ctx);
        int lastScreenState = sMobileLastScreenState.getAndSetScreenState();

        try {
            if(DUMP) Log.d(TAG,"Inside updateMobileRxTxBytes...");
            DevStatPrefs prefs = DevStatPrefs.getInstance(ctx);
            // Bytes transferred since first boot
            long[] cumRxBytes = prefs.getMobileCumRxBytes();
            long[] cumTxBytes = prefs.getMobileCumTxBytes();

            // Bytes transferred last time read
            long prevRxBytes = prefs.getMobileRxBytes();
            long prevTxBytes = prefs.getMobileTxBytes();

            // Current bytes transferred
            getMobileBytesFromSys();
            long currRxBytes = mMobileRxBytes;
            long currTxBytes = mMobileTxBytes;

            if (DUMP) {
                android.util.Log.d(TAG, "updateMobileRxTxBytes;(Before) prevRxBytes=" +
                        prevRxBytes + "prevTxBytes=" + prevTxBytes + "cumRxBytes=" +
                        Arrays.toString(cumRxBytes) + "cumTxBytes=" + Arrays.toString(cumTxBytes) +
                        "currRxBytes=" + currRxBytes + "currTxBytes=" + currTxBytes);
            }

           // No point in updating if current RX/TX is 0;
           if (currRxBytes != 0) {
               long addBytes = currRxBytes>=prevRxBytes? currRxBytes-prevRxBytes : currRxBytes;
               updateCumulative(lastScreenState, cumRxBytes, addBytes);
               prefs.setMobileCumRxBytes(cumRxBytes);
           }

           if (currTxBytes != 0) {
               long addBytes = currTxBytes>=prevTxBytes? currTxBytes-prevTxBytes : currTxBytes;
               updateCumulative(lastScreenState, cumTxBytes, addBytes);
               prefs.setMobileCumTxBytes(cumTxBytes);
           }

           prefs.setMobileRxBytes(currRxBytes);
           prefs.setMobileTxBytes(currTxBytes);

        } catch (IOException e) {
            // Do Nothing
        }
    }

    public static void updateWifiRxTxPkts(Context ctx) {
        init(ctx);
        int lastScreenState = sWifiPktLastScreenState.getAndSetScreenState();

        DevStatPrefs prefs = DevStatPrefs.getInstance(ctx);
        long prevRxPkts = prefs.getWifiRxPkts();
        long prevTxPkts = prefs.getWifiTxPkts();
        long[] cumRxPkts = prefs.getWifiCumRxPkts();
        long[] cumTxPkts = prefs.getWifiCumTxPkts();
        getWifiPktsFromSys();
        long currRxPkts = mWifiRxPkts;
        long currTxPkts = mWifiTxPkts;

        if (currRxPkts >= 0) {
            long addPkts = currRxPkts >= prevRxPkts ? currRxPkts - prevRxPkts : currRxPkts;
            updateCumulative(lastScreenState, cumRxPkts, addPkts);
            prefs.setWifiCumRxPkts(cumRxPkts);
            if (DUMP) {
                Log.d(TAG, "updateWifiRxTxPkts: " + "+" + addPkts
                        + ";" + "cumRxPkts=" + Arrays.toString(cumRxPkts));
            }
        }
        if (currTxPkts >= 0) {
            long addPkts = currTxPkts >= prevTxPkts ? currTxPkts - prevTxPkts : currTxPkts;
            updateCumulative(lastScreenState, cumTxPkts, addPkts);
            prefs.setWifiCumTxPkts(cumTxPkts);
            if (DUMP) {
                Log.d(TAG, "updateWifiRxTxPkts: " + "+" + addPkts
                        + ";" + "cumTxPkts=" + Arrays.toString(cumTxPkts));
            }
        }
        prefs.setWifiRxPkts(currRxPkts);
        prefs.setWifiTxPkts(currTxPkts);
        if (DUMP) {
            Log.d(TAG, "updateWifiRxTxPkts: " + "currRxPkts=" + currRxPkts
                    + ";" + "currTxPkts=" + currTxPkts);
        }
    }

    public static void updateMobileRxTxPkts(Context ctx) {
        init(ctx);
        int lastScreenState = sMobilePktLastScreenState.getAndSetScreenState();

        DevStatPrefs prefs = DevStatPrefs.getInstance(ctx);
        long[] cumRxPkts = prefs.getMobileCumRxPkts();
        long[] cumTxPkts = prefs.getMobileCumTxPkts();
        long prevRxPkts = prefs.getMobileRxPkts();
        long prevTxPkts = prefs.getMobileTxPkts();
        getMobilePktsFromSys();
        long currRxPkts = mMobileRxPkts;
        long currTxPkts = mMobileTxPkts;

        if (currRxPkts >= 0) {
            long addPkts = currRxPkts >= prevRxPkts ? currRxPkts - prevRxPkts : currRxPkts;
            updateCumulative(lastScreenState, cumRxPkts, addPkts);
            prefs.setMobileCumRxPkts(cumRxPkts);
            if (DUMP) {
                Log.d(TAG, "updateMobileRxTxPkts: " + "+" + addPkts
                        + ";" + "cumRxPkts=" + Arrays.toString(cumRxPkts));
            }
        }

        if (currTxPkts >= 0) {
            long addPkts = currTxPkts >= prevTxPkts ? currTxPkts - prevTxPkts : currTxPkts;
            updateCumulative(lastScreenState, cumTxPkts, addPkts);
            prefs.setMobileCumTxPkts(cumTxPkts);
            if (DUMP) {
                Log.d(TAG, "updateMobileRxTxPkts: " + "+" + addPkts
                        + ";" + "cumTxPkts=" + Arrays.toString(cumTxPkts));
            }
        }

        prefs.setMobileRxPkts(currRxPkts);
        prefs.setMobileTxPkts(currTxPkts);
        if (DUMP) {
            Log.d(TAG, "updateMobileRxTxPkts: " + "currRxPkts=" + currRxPkts
                    + ";" + "currTxPkts=" + currTxPkts);
        }
    }

    private static void getMobileBytesFromSys() throws IOException {
        mMobileRxBytes=0L; mMobileTxBytes=0L;

        MobileInterfaceData[] interfaceData = new MobileInterfaceData[INTERFACE_ARRAY_SIZE];
        for (int i=0; i<INTERFACE_ARRAY_SIZE; i++) {
            interfaceData[i] = new MobileInterfaceData();
        }

        File[] netFiles = new File(SYS_CLASS_NET_PATH).listFiles();
        if (netFiles != null) {
            for (File net : netFiles) {
                interfaceData[PPP_INDEX].addInterfaceData("ppp", net );
                interfaceData[RMNET_INDEX].addInterfaceData("rmnet", net );
                interfaceData[QMI_INDEX].addInterfaceData("qmi", net );
            }
        }

        // If rmnetX is there, then it alone should be used
        //    because some phones have both rmnetX and qmiX, say on LTE network in Spyder
        // Else if ppp or qmiX is there, then that should be used
        //    ppp and qmiX won't be present simultaneously
        //    ppp is for CDMA phones, while qmiX is for SpyderU(and probably SpyderLTE) on EDGE/3G
        // mmiX is NOT used.

        int useIndex = INVALID_INDEX;
        if (interfaceData[RMNET_INDEX].present) {
            useIndex = RMNET_INDEX;
        } else if (interfaceData[PPP_INDEX].present) {
            useIndex = PPP_INDEX;
        } else if (interfaceData[QMI_INDEX].present) {
            useIndex = QMI_INDEX;
        }

        if (useIndex != INVALID_INDEX) {
            mMobileRxBytes += interfaceData[useIndex].rx;
            mMobileTxBytes += interfaceData[useIndex].tx;
        }

        if (DUMP) Log.d(TAG, "getMobileBytesFromSys:" + mMobileRxBytes + "," + mMobileTxBytes);
    }

    private static void getWifiBytesFromSys(String wifi) throws IOException {
        mWifiRxBytes = 0L;
        mWifiTxBytes = 0L;
        File net = new File(SYS_CLASS_NET_PATH + wifi);
        if (isNetUp(net)) {
            mWifiRxBytes = readTotalBytes(net, RXBYTES);
            mWifiTxBytes = readTotalBytes(net, TXBYTES);
        }
        if (DUMP) Log.d(TAG, "getWifiBytesFromSys:" + mWifiRxBytes + "," + mWifiTxBytes);
    }

    private static void getMobilePktsFromSys() {
        mMobileRxPkts = TrafficStats.getMobileRxPackets();
        mMobileTxPkts = TrafficStats.getMobileTxPackets();
        if (DUMP) Log.d(TAG, "getMobilePktsFromSys:" + mMobileRxPkts + "," + mMobileTxPkts);
    }

    private static void getWifiPktsFromSys() {
        getMobilePktsFromSys();
        mWifiRxPkts = TrafficStats.getTotalRxPackets() - mMobileRxPkts;
        mWifiTxPkts = TrafficStats.getTotalTxPackets() - mMobileTxPkts;
        if (DUMP) Log.d(TAG, "getWifiPktsFromSys:" + mWifiRxPkts + "," + mWifiTxPkts);
    }

    private static boolean isNetUp(File file) {
        StringBuffer strbuf = new StringBuffer();
        strbuf.append(file.getPath()).append(CARRIER);
        return new File(strbuf.toString()).canRead();
    }

    private static long readTotalBytes(File file, String whatBytes) {
        StringBuffer sb = new StringBuffer();
        sb.append(file.getPath()).append(whatBytes);
        RandomAccessFile raf = null;
        try {
            raf = getFile(sb.toString());
            return Long.valueOf(raf.readLine());
        } catch (Exception e) {
            return 0L;
        } finally {
            try {
                if (raf != null) raf.close();   
            } catch (IOException e) {
                // Nothing to be done
            }   
        }   
    }   

    private static RandomAccessFile getFile(String filename) throws IOException {
        File f = new File(filename);
        return new RandomAccessFile(f, "r");
    }

    private static final void updateCumulative(long lastScreenState, long[] cum, long add) {
        cum[CUMULATIVE_INDEX] += add;
        if (lastScreenState != ScreenStateSaver.UNKNOWN) {
            cum[lastScreenState==ScreenStateSaver.ON ? ON_INDEX : OFF_INDEX] += add;
        }
    }

    private final static class MobileInterfaceData {
        boolean present;
        long tx;
        long rx;

        void addInterfaceData(String pattern, File net) {
            if (net.getPath().contains(pattern) && isNetUp(net)) {
                present = true;
                rx += readTotalBytes(net, RXBYTES);
                tx += readTotalBytes(net, TXBYTES);
            }
        }
    }
}
