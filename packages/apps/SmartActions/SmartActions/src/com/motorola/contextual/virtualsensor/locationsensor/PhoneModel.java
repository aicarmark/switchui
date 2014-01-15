package com.motorola.contextual.virtualsensor.locationsensor;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.LS_BACKGROUND_SCAN_CHECK;
import static com.motorola.contextual.virtualsensor.locationsensor.Constants.LS_BACKGROUND_SCAN_EXIST;
import static com.motorola.contextual.virtualsensor.locationsensor.Constants.LS_BACKGROUND_SCAN_START;
import static com.motorola.contextual.virtualsensor.locationsensor.Constants.PERMISSION;
import static com.motorola.contextual.virtualsensor.locationsensor.Constants.PNO_LIST;
import static com.motorola.contextual.virtualsensor.locationsensor.Constants.PNO_SSID;
import static com.motorola.contextual.virtualsensor.locationsensor.Constants.PNO_UPDATE;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabase.PoiTable;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabase.PoiTable.Tuple;

/**
*<code><pre>
* CLASS:
*  To detect the phone the app is running on, i.e., a motorola phone with some APIs supported
*
* RESPONSIBILITIES:
*  Determine whether the phone framework supports certain add-on features, APIs that available.
*
* COLABORATORS:
*  We have a receiver on the phone framework that listens to the query intent and send back query results.
*  For runtime that does not have our stub, no response, we then view this as feature absent.
*
* USAGE:
* 	See each method.
*
* TEST:
*   1. return good poituple from getMatchingPoiOfCell()
*   2. comment out bag ssid match in checkLeavingThrashing()
*   3. from the other side of build, walk into your cube.
*
*</pre></code>
*/
public class PhoneModel extends BroadcastReceiver {

    private static final String TAG = "LSAPP_PHONE";

    private Context mContext;
    private LocationSensorManager mLSMan;
    private LocationDetection mLSDet;
    private final Handler mTimerTaskHdle;
    private Tuple mNextPoiTuple;   // indicate what is the next poi, so when async pno reg ack back, we can set flag. need closure here!
    private WifiManager mWifiMan;

    private boolean mIsPnoScanSupported = false;
    private boolean mPnoScanAckWaiting = false;   // waiting for pno reg ack knowedge
    private boolean mIsBackgroundScanSupported = false;
    private boolean mBackgroundScanStarted = false;

    /**
     * Constructor
     * @param context
     * @param hdl
     */
    public PhoneModel(Context ctx, LocationDetection lsdet, Handler hdl) {
        mContext = ctx;
        mTimerTaskHdle = hdl;
        mLSMan = (LocationSensorManager)mContext;
        mLSDet = lsdet;

        mWifiMan = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(PNO_LIST);   // pno scan
        filter.addAction(LS_BACKGROUND_SCAN_EXIST);  // smart background scan
        mContext.registerReceiver(this, filter);

        // ping framework
        pingFrameworkStub();
    }

    /**
     * called from onDestroy to clean up.
     */
    public void clean() {
        mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LSAppLog.d(TAG, ": onReceiver :" + action);
        Message msg = mTimerTaskHdle.obtainMessage();

        if (PNO_LIST.equals(action)) {
            mIsPnoScanSupported = true;   // pno supported
            mPnoScanAckWaiting = false; // ack recvd
            String pnossids = intent.getStringExtra(PNO_SSID);
            if(pnossids != null && pnossids.length() > 0) {
                //mLSDet.mPoiAdapter.setHomeWorkPnoFlag(true);  // set the pno enabled flag upon receiving confirmation.
                mLSDet.mPoiAdapter.setPnoFlag(mNextPoiTuple, true);
                LSAppLog.pd(TAG, ": onReceive : PNO_LIST registered:" + mNextPoiTuple.getTopSsid() + " Ack:" + intent.getStringExtra(PNO_SSID));
            } else {
                LSAppLog.pd(TAG, ": onReceive : PNO_LIST empty :" + intent.getStringExtra(PNO_SSID));
            }
        } else if (LS_BACKGROUND_SCAN_EXIST.equals(action)) {
            mIsBackgroundScanSupported = true;  // bkgrd scan supported.
            updatePreferrence();  // XXX bad, IO inside receiver.
            LSAppLog.pd(TAG, ": onReceive : background scan supported ! " + LS_BACKGROUND_SCAN_EXIST);
        } else {
            LSAppLog.d(TAG, "unknown intent " + intent.toString());
        }
    }

    /**
     * check bkgrd support by sending async intent to framework stub, waiting for confirmation intent.
     * called upon class initialization.
     */
    public void pingFrameworkStub() {
        LSAppLog.pd(TAG, " checking feature : background scan : " + LS_BACKGROUND_SCAN_CHECK);
        mContext.sendBroadcast(new Intent(LS_BACKGROUND_SCAN_CHECK), PERMISSION);
        // sendPnoSsidList("");  XXX do not use PNO for now.
    }

    /**
     * check whether smart scan supported
     */
    public boolean isSmartScanSupported() {
        return mIsBackgroundScanSupported;
    }

    /**
     * register strongest ssid to PNO by async bcast intent
     * Expected to recv confirmation intent from PNO to confirm the reg is effective.
     */
    public void registerPnoSsid(PoiTable.Tuple nextpoituple) {
        // pno not supported, or pno reg ack waiting, or next poi is null
        if(!mIsPnoScanSupported || mPnoScanAckWaiting || null == nextpoituple) {
            return;
        }

        if(nextpoituple.getPnoRegistration()) {
            LSAppLog.d(TAG, "registerPNOSsid: do nothing, next poi already registered:" + nextpoituple.getTopSsid());
            return;
        }

        List<String> poissids = nextpoituple.getTopSsid();
        JSONArray jsonarray = new JSONArray();
        //List<String> hmwkssid = mLSDet.mPoiAdapter.getHomeWorkStrongestSsid();  // could be empty but never null.
        for(String ssid : poissids) {
            try {
                JSONObject entry = new JSONObject();
                entry.put(PNO_SSID, ssid);
                jsonarray.put(entry);
            } catch(JSONException e) {
                LSAppLog.e(TAG, "registerPNOSsid Exception :" + ssid + e.toString());
            }
        }

        mPnoScanAckWaiting = true;
        mNextPoiTuple = nextpoituple;  // note down which tuple registered.
        sendPnoSsidList(jsonarray.toString());
        LSAppLog.pd(TAG, "registerPNOSsid : " + jsonarray.toString());
    }

    /**
     * un-register PNO ssid. can ignore the ACK.
     */
    public void unregisterPnoSsid(PoiTable.Tuple curtuple) {
        if(!mIsPnoScanSupported || mPnoScanAckWaiting) {
            return;
        }
        LSAppLog.pd(TAG, "unregisterPNOSsid upon entering POI");
        //mLSDet.mPoiAdapter.setHomeWorkPnoFlag(false);
        mLSDet.mPoiAdapter.setPnoFlag(curtuple, false);
        sendPnoSsidList("");  // to clear, send empty string
    }

    /**
     * broadcast intent to pno
     */
    private void sendPnoSsidList(String ssidlist) {
        Intent ssid_intent = new Intent(PNO_UPDATE);
        ssid_intent.putExtra(PNO_SSID, ssidlist);

        mContext.sendBroadcast(ssid_intent);
        LSAppLog.pd(TAG, "sendPNOSsidList:" + ssidlist);
    }

    /**
     * if the framework stub is a aidl service, bind to it.
     */
    @Deprecated
    private void bindFrameworkStub() {
        ServiceConnection conn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                LSAppLog.d(TAG, " bind framework stub connected:" + name);
            }
            public void onServiceDisconnected(ComponentName name) {
                LSAppLog.d(TAG, " bind framework stub disconnected:  " + name);
            }
        };

        Intent intent = new Intent(LS_BACKGROUND_SCAN_CHECK);
        boolean bind = mContext.bindService(intent, conn, 0);
        if(bind == true) {
            LSAppLog.d(TAG, " framework stub exist: ");
        } else {
            LSAppLog.d(TAG, " framework stub does not exist: ");
        }
    }

    /**
     * start background smart scan if it is supported
     * @param start, if true, start the background scan, if false, stop it.
     * @return true when bkgrd scan started when wifi disabled and bkgrd scan is supported.
     * because bkgrd scan automatically off when wifi is on, check the case when wifi is on-off
     * in which bkgrd scan got turned off.
     */
    public boolean setBackgroundScan(boolean start) {

        // if smart scan not supported, or wifi is on, we can not do background smart scan
        // bkgrd scan automatically nullified when wifi is on
        if( !mIsBackgroundScanSupported || mWifiMan.isWifiEnabled()) {
            mBackgroundScanStarted = false;  // wifi on automatically reset my flag.
            return false;
        }

        // now it is supported, and wifi off
        Intent intent = new Intent(LS_BACKGROUND_SCAN_START);
        intent.putExtra("START", start);

        //if( !mBackgroundScanStarted && start ) {
        if( start ) {  // IKHSS6-13660, auto scan not happening continuous
            mBackgroundScanStarted = true;
            LSAppLog.pd(TAG, " setBackgroundScan : started!!");
            mContext.sendBroadcast(intent, PERMISSION);
        } else if( !start ) {  // && mBackgroundScanStarted)
            mBackgroundScanStarted = false;
            mContext.sendBroadcast(intent, PERMISSION);
            LSAppLog.pd(TAG, " setBackgroundScan : stopped!!");
        } else {
            LSAppLog.pd(TAG, " setBackgroundScan : already started, do nothing.");
        }

        return true;   // either to be started or already started.
    }


    /**
     * update shared preference to note down background scan supported.
     */
    private void updatePreferrence() {
        //PreferenceManager.setDefaultValues(context, resId, readAgain);
        mLSMan.mLSApp.mAppPref.setString(AppPreferences.BACKGROUND_SCAN, "1");
    }
}
