/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.batterymanager.devicestatistics;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.content.Context;
import android.util.Log;

public class SysClassNetUtils {
    final static String TAG = "SysClassNetUtils";
    final static String SYS_CLASS_NET_PATH = "sys/class/net";
    final static String CARRIER = "/carrier";
    final static String RXBYTES = "/statistics/rx_bytes";
    final static String TXBYTES = "/statistics/tx_bytes";

    final static String WIFI = "/tiwlan0";

    private static long mMobileRxBytes;
    private static long mMobileTxBytes;
    private static long mWifiRxBytes;
    private static long mWifiTxBytes;

    /**
     * Checkin data is cumulative, since phone first boot
     */
    public static String getWifiRxTxBytes(Context ctx) {
        DevStatPrefs prefs = DevStatPrefs.getInstance(ctx);
        long cumRxBytes = prefs.getWifiCumRxBytes();
        long cumTxBytes = prefs.getWifiCumTxBytes();

        String logStr = null;
        // Need not consume bytes in checkin server, if both are 0
        if(cumRxBytes!=0L || cumTxBytes!=0L) {
            logStr = new String("wifi_rx=" + cumRxBytes + ";wifi_tx=" + cumTxBytes + ";");
        }
        return logStr;
    }

    public static String getMobileRxTxBytes(Context ctx) {
        DevStatPrefs prefs = DevStatPrefs.getInstance(ctx);
        long cumRxBytes = prefs.getMobileCumRxBytes();
        long cumTxBytes = prefs.getMobileCumTxBytes();

        String logStr = null;
        // Need not consume bytes in checkin server, if both are 0
        if(cumRxBytes!=0L || cumTxBytes!=0L) {
            logStr = new String("mobile_rx=" + cumRxBytes + ";mobile_tx=" + cumTxBytes + ";");
        }   
        return logStr;
    }   

    public static void updateNetStats(Context ctx) {
        updateMobileRxTxBytes(ctx);
        updateWifiRxTxBytes(ctx);
    }

    public static void updateWifiRxTxBytes(Context ctx) {
        try {
            DevStatPrefs prefs = DevStatPrefs.getInstance(ctx);
            long prevRxBytes = prefs.getWifiRxBytes();
            long prevTxBytes = prefs.getWifiTxBytes();

            // Do not proceed if wifi is down for the next consecutive update
            if (prevRxBytes == 0 && prevTxBytes == 0 && 
                  isNetUp(new File(SYS_CLASS_NET_PATH + WIFI)) == false) {
                return;
            }

            long cumRxBytes = prefs.getWifiCumRxBytes();
            long cumTxBytes = prefs.getWifiCumTxBytes();
            getWifiBytesFromSys();
            long currRxBytes = mWifiRxBytes;
            long currTxBytes = mWifiTxBytes;

            android.util.Log.d(TAG, "updateWifiRxTxBytes;(Before) prevRxBytes=" + prevRxBytes +
                "prevTxBytes=" + prevTxBytes + "cumRxBytes=" + cumRxBytes + "cumTxBytes=" +
                cumTxBytes + "currRxBytes=" + currRxBytes + "currTxBytes=" + currTxBytes);

            // No point in updating if current RX/TX is 0; 
            if (currRxBytes != 0) {
                prefs.setWifiCumRxBytes(cumRxBytes + (currRxBytes - prevRxBytes)); 
            }

            if (currTxBytes != 0) {
               prefs.setWifiCumTxBytes(cumTxBytes + (currTxBytes - prevTxBytes)); 
            }

            prefs.setWifiRxBytes(currRxBytes);
            prefs.setWifiTxBytes(currTxBytes);
        } catch (IOException e) {
           // Nothing to be done
        }
    }

    public static void updateMobileRxTxBytes(Context ctx) {
        try {
            Log.d(TAG,"Inside updateMobileRxTxBytes...");
            DevStatPrefs prefs = DevStatPrefs.getInstance(ctx);
            // Bytes transferred since first boot
            long cumRxBytes = prefs.getMobileCumRxBytes();
            long cumTxBytes = prefs.getMobileCumTxBytes();

            // Bytes transferred last time read
            long prevRxBytes = prefs.getMobileRxBytes();
            long prevTxBytes = prefs.getMobileTxBytes();

            // Current bytes transferred
            getMobileBytesFromSys();
            long currRxBytes = mMobileRxBytes;
            long currTxBytes = mMobileTxBytes;

            android.util.Log.d(TAG, "updateMobileRxTxBytes;(Before) prevRxBytes=" + prevRxBytes +
                "prevTxBytes=" + prevTxBytes + "cumRxBytes=" + cumRxBytes + "cumTxBytes=" +
                cumTxBytes + "currRxBytes=" + currRxBytes + "currTxBytes=" + currTxBytes);

           // No point in updating if current RX/TX is 0;
           if (currRxBytes != 0) {
               prefs.setMobileCumRxBytes(cumRxBytes + (currRxBytes - prevRxBytes));
           }

           if (currTxBytes != 0) {
               prefs.setMobileCumTxBytes(cumTxBytes + (currTxBytes - prevTxBytes));
           }

           prefs.setMobileRxBytes(currRxBytes);
           prefs.setMobileTxBytes(currTxBytes);

        } catch (IOException e) {
            // Do Nothing
        }
    }

    private static boolean isMobileNet(File file) {
        return (file.getPath().contains("rmnet") || file.getPath().contains("ppp0"));
    }

    private static void getMobileBytesFromSys() throws IOException {
        mMobileRxBytes=0L; mMobileTxBytes=0L;
        File[] netFiles = new File(SYS_CLASS_NET_PATH).listFiles();
        if (netFiles != null) {
            for (File net : netFiles) {
                if (isMobileNet(net) && isNetUp(net)) {
                    mMobileRxBytes += readTotalBytes(net, RXBYTES);
                    mMobileTxBytes += readTotalBytes(net, TXBYTES);
                }
            }
        }
        Log.d(TAG, "getMobileBytesFromSys:" + mMobileRxBytes + "," + mMobileTxBytes);
    }

    private static void getWifiBytesFromSys() throws IOException {
        mWifiRxBytes = 0L; mWifiTxBytes = 0L;
        File net = new File(SYS_CLASS_NET_PATH + WIFI);
        if (isNetUp(net)) {
            mWifiRxBytes = readTotalBytes(net, RXBYTES);
            mWifiTxBytes = readTotalBytes(net, TXBYTES);
        }
        Log.d(TAG, "getWifiBytesFromSys:" + mWifiRxBytes + "," + mWifiTxBytes);
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
}
