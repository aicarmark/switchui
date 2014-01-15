
package com.motorola.devicestatistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.IBinder;
import android.util.Log;

public class TrafficCollectionService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    public static final String MOBILE = "mobile";
    public static final String WIFI = "wifi";

    private static final String TAG = TrafficCollectionService.class.getSimpleName();
    private final static boolean DUMP = true; // log used.
    private Timer mTimer;
    private static final Long mInterval = 30 * 60 * 1000L;
    private HashMap<Integer, UidInfo> mInitialTrafficList;
    private HashMap<Integer, UidInfo> mTrafficList;
    private String mNetworkType;
    private Boolean mCharging;
    private static TrafficCollectionService mInstance;
    private Object lock;

    public UidInfo getUidInfo(int uid) {
        return mTrafficList.get(uid);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new Utils.RunInBackgroundThread() {
            public void run() {
                if (Watchdog.isDisabled()) return;

                mInitialTrafficList = new HashMap<Integer, UidInfo>();
                mTrafficList = new HashMap<Integer, UidInfo>();
                ConnectivityManager connectivityManager = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetInfo != null) {
                    if (activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE
                            && activeNetInfo.getState() == NetworkInfo.State.CONNECTED) {
                        setNetworkType(MOBILE);
                    } else {
                        setNetworkType(WIFI);
                    }
                } else {
                    setNetworkType(MOBILE);
                }
                // default not charging.
                mCharging = false;
                initialTraffic();
                mTimer = new Timer();
                lock = new Object();
            }
        };
    }

    @Override
    public int onStartCommand(Intent startInt, int flags, int startId) {
        new Utils.RunInBackgroundThread() {
            public void run() {
                if (Watchdog.isDisabled()) return;

                if (mTimer == null) mTimer = new Timer();
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        refreshTrafficList();
                    }
                }, 0, mInterval);
                mInstance = TrafficCollectionService.this;
            }
        };
        return Service.START_STICKY;

    }

    public static TrafficCollectionService getService() {
        return mInstance;
    }

    public void initialTraffic() {
        mInitialTrafficList.clear();
        PackageManager packageManager = getPackageManager();
        ArrayList<Integer> uids = new ArrayList<Integer>();
        for (ApplicationInfo appInfo : packageManager
                .getInstalledApplications(0)) {
            int uid = appInfo.uid;
            long bytes = (TrafficStats.getUidRxBytes(uid) > 0 ? TrafficStats
                    .getUidRxBytes(uid) : 0)
                    + (TrafficStats.getUidTxBytes(uid) > 0 ? TrafficStats
                            .getUidTxBytes(uid) : 0);
            long packets = (TrafficStats.getUidRxPackets(uid) > 0 ? TrafficStats
                    .getUidRxPackets(uid) : 0)
                    + (TrafficStats.getUidTxPackets(uid) > 0 ? TrafficStats
                            .getUidTxPackets(uid) : 0);
            if (!uids.contains(uid)) {
                // few applications can refer to same uid,
                // sum each uid stats only once
                uids.add(uid);
                if (bytes > 0) {
                    UidInfo uidInfo = new UidInfo();
                    uidInfo.mUid = uid;
                    uidInfo.mByteTotalPerUid = bytes;
                    uidInfo.mPacketTotalPerUid = packets;
                    mInitialTrafficList.put(uidInfo.mUid, uidInfo);
                    if (DUMP) {
                        Log.d(TAG, "Initial " + appInfo.packageName
                                + "(uid=" + uid + "): "
                                + bytes + " bytes/" + packets + " packets.");
                    }
                }
            }
        }
    }

    public void refreshTrafficList() {
        synchronized (lock) {
            PackageManager packageManager = getPackageManager();
            ArrayList<Integer> uids = new ArrayList<Integer>();
            for (ApplicationInfo appInfo : packageManager
                    .getInstalledApplications(0)) {
                int uid = appInfo.uid;
                Long bytes = (TrafficStats.getUidRxBytes(uid) > 0 ? TrafficStats
                        .getUidRxBytes(uid) : 0)
                        + (TrafficStats.getUidTxBytes(uid) > 0 ? TrafficStats
                                .getUidTxBytes(uid) : 0);
                Long packets = (TrafficStats.getUidRxPackets(uid) > 0 ? TrafficStats
                        .getUidRxPackets(uid) : 0)
                        + (TrafficStats.getUidTxPackets(uid) > 0 ? TrafficStats
                                .getUidTxPackets(uid) : 0);
                if (!uids.contains(uid)) {
                    // few applications can refer to same uid,
                    // sum each uid stats only once
                    uids.add(uid);
                    if (bytes > 0) {
                        Long currentByte = 0L;
                        Long currentPacket = 0L;
                        UidInfo init = mInitialTrafficList.get(uid);
                        UidInfo current = mTrafficList.get(uid);
                        // appname doesn't exist in mInitialTrafficList.
                        if (init == null) {
                            if (DUMP) {
                                Log.d(TAG, appInfo.packageName + "(uid=" + uid
                                        + ") doesn't exist in mInitialTrafficList.");
                            }
                            // appname doesn't exist in old mTrafficList or
                            // mTrafficList is empty. Add data directly.
                            if (current == null) {
                                addItemToTrafficList(appInfo, packageManager,
                                        bytes, packets);
                            }
                            // appname exists in mTrafficList. If byte
                            // increased, modify it.
                            else {
                                if (bytes > current.mByteTotalPerUid) {
                                    modifyItemOfTrfficList(appInfo, packageManager,
                                            bytes, packets);
                                }
                            }
                        }
                        // appname exist in mInitialTrafficList.
                        else {
                            if (DUMP) {
                                Log.d(TAG, appInfo.packageName
                                        + "(uid=" + uid + ") exist in mInitialTrafficList.");
                            }
                            currentByte = bytes - init.mByteTotalPerUid;
                            currentPacket = packets - init.mPacketTotalPerUid;
                            if (currentByte > 0) {
                                // appname doesn't exist in old mTrafficList.
                                if (current == null) {
                                    addItemToTrafficList(appInfo, packageManager,
                                            currentByte, currentPacket);
                                }
                                // appname exists in mTrafficList.
                                else {
                                    if (currentByte > current.mByteTotalPerUid) {
                                        modifyItemOfTrfficList(appInfo,
                                                packageManager, currentByte,
                                                currentPacket);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    class UidInfo {
        int mUid;
        // for bytes number statistics. Always be available.
        long mByteTotalPerUid;
        long mByteMobilePerUid;
        long mByteWifiPerUid;
        // for byte on battery;
        long mByteMobileOnBattery;
        long mByteWifiOnBattery;
        // for packets number statistics. May not be available in some kernel version.
        // Have not add into db.
        long mPacketTotalPerUid;
        long mPacketMobilePerUid;
        long mPacketWifiPerUid;
    }

    private void addItemToTrafficList(ApplicationInfo appInfo,
            PackageManager packageManager, Long bytes, Long packets) {
        if (mTrafficList.containsKey(appInfo.uid))
            return;
        UidInfo uidInfo = new UidInfo();
        uidInfo.mUid = appInfo.uid;
        uidInfo.mByteTotalPerUid = bytes;
        uidInfo.mPacketTotalPerUid = packets;
        // It's mobile traffic.
        if (MOBILE.equals(mNetworkType)) {
            uidInfo.mByteMobilePerUid = bytes;
            uidInfo.mByteWifiPerUid = (Long) 0L;
            uidInfo.mPacketMobilePerUid = packets;
            uidInfo.mPacketWifiPerUid = (Long) 0L;
            if (DUMP) {
                Log.d(TAG, "Add mobile traffic consume apk: "
                        + appInfo.packageName + "(uid="
                        + appInfo.uid + "): " + bytes
                        + " bytes/" + packets + " packets");
            }
            if (!mCharging) {
                uidInfo.mByteMobileOnBattery = bytes;
                if (DUMP) Log.d(TAG, "Add to mByteMobileOnBattery.");
            }
        }
        // It's wifi traffic.
        else {
            uidInfo.mByteMobilePerUid = (Long) 0L;
            uidInfo.mByteWifiPerUid = bytes;
            uidInfo.mPacketMobilePerUid = (Long) 0L;
            uidInfo.mPacketWifiPerUid = packets;
            if (DUMP) {
                Log.d(TAG, "Add wifi traffic consume apk: "
                        + appInfo.packageName + "(uid="
                        + appInfo.uid + "): " + bytes
                        + " bytes/" + packets + " packets");
            }
            if (!mCharging) {
                uidInfo.mByteWifiOnBattery = bytes;
                if (DUMP) Log.d(TAG, "Add to mByteWifiOnBattery.");
            }
        }
        mTrafficList.put(uidInfo.mUid, uidInfo);
    }

    private void modifyItemOfTrfficList(ApplicationInfo appInfo,
            PackageManager packageManager, Long bytes, Long packets) {
        int uid = appInfo.uid;
        UidInfo uidInfo = mTrafficList.get(uid);
        if (uidInfo == null) {
            if (DUMP) {
                Log.d(TAG, "No such uid " + appInfo.uid
                        + " in mTrafficList to modify.");
            }
            return;
        }
        long oldByteTotal = uidInfo.mByteTotalPerUid;
        long oldPacketTotal = uidInfo.mPacketTotalPerUid;
        uidInfo.mByteTotalPerUid = bytes;
        uidInfo.mPacketTotalPerUid = packets;
        if (oldByteTotal > bytes)
            return;
        // It's mobile traffic.
        if (MOBILE.equals(mNetworkType)) {
            uidInfo.mByteMobilePerUid += (bytes - oldByteTotal);
            uidInfo.mPacketMobilePerUid += (packets - oldPacketTotal);
            if (DUMP) {
                Log.d(TAG, "Modify Mobile traffic: "
                        + appInfo.packageName + "(uid=" + appInfo.uid + "): "
                        + "+" + (bytes - oldByteTotal)
                        + "=" + uidInfo.mByteMobilePerUid + " bytes/"
                        + "+" + (packets - oldPacketTotal)
                        + "=" + uidInfo.mPacketMobilePerUid + " packets");
            }
            if (!mCharging) {
                uidInfo.mByteMobileOnBattery += (bytes - oldByteTotal);
                if (DUMP) Log.d(TAG, "Add to mByteMobileOnBattery.");
            }
        } else { // It's wifi traffic.
            uidInfo.mByteWifiPerUid += (bytes - oldByteTotal);
            uidInfo.mPacketWifiPerUid += (packets - oldPacketTotal);
            if (DUMP) {
                Log.d(TAG, "Modify Wifi traffic: "
                        + appInfo.packageName + "(uid=" + appInfo.uid + "): "
                        + "+" + (bytes - oldByteTotal)
                        + "=" + uidInfo.mByteWifiPerUid + " bytes/"
                        + "+" + (packets - oldPacketTotal)
                        + "=" + uidInfo.mPacketWifiPerUid + " packets");
            }
            if (!mCharging) {
                uidInfo.mByteWifiOnBattery += (bytes - oldByteTotal);
                if (DUMP) Log.d(TAG, "Add to mByteWifiOnBattery.");
            }
        }
        mTrafficList.put(appInfo.uid, uidInfo);
    }

    public void setNetworkType(String type) {
        mNetworkType = type;
    }

    public void setCharging(Boolean charging) {
        mCharging = charging;
    }

    @Override
    public void onDestroy() {
        if (mInitialTrafficList != null) {
            mInitialTrafficList.clear();
        }
        if (mTrafficList != null) {
            mTrafficList.clear();
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        super.onDestroy();
    }
}
