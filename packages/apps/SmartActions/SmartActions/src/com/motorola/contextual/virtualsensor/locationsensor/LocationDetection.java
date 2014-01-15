/*
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2010/08/27 IKCTXTAW-19       Initial version
 * w04917        2012/07/16 IKCTXTAW-492      Move TRANSIENT and add HIDDEN_LOCATION_RULE
 */
package com.motorola.contextual.virtualsensor.locationsensor;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.*;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.CellLocation;
import android.util.Log;

import com.motorola.android.wrapper.SystemWrapper;
import com.motorola.android.wrapper.LocationManagerWrapper;

import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabase;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabase.PoiTable.Tuple;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
*<code><pre>
* CLASS:
*  implements location detection(arrival and leaving) logic based on location updates from platform.
*  this is a singleton instance this class.
*
* RESPONSIBILITIES:
*  This class is responsible for detecting arriving or leaving of any meaningful location.
* it requests location updates from platform and calculate distances to any POIs to detect location.
* Detection is purely distance driven...and distance is calculated from location fix got from platform.
*
* COLABORATORS:
*  VSM, notify VSM upon meaningful location detection(arrival and leaving).
*
* USAGE:
* 	See each method.
*
*</pre></code>
*/


public class LocationDetection {
    private static final String TAG = "LSAPP_LSDet";
    public interface Msg {   // diff with ones in locman
        int START_DETECTION 		= 1000;     // start monitoring
        int STOP_DETECTION	 		= 1001;     // start monitoring
        int PROXIMITY_ALERT 		= 1003;
        int ENTERING_POI		 	= 1004; 	// fire event, done
        int LEAVING_POI		 		= 1005; 	// fire event, done
        int POI_REFRESH				= 1006;
        int LOCATION_PENDINGINTENT	= 1007;
        int START_SCAN				= 1008;
        int INIT_COMPLETE			= 1009;
        int MOTION_TRIGGER			= 1010;	    // run detection upon com.motorola.intent.action.STILL/MOTION
        int NULL 					= 99999;
    };

    /**
     * RUNNING set upon request location fix, reset inside stop detection.
     * FIX_PENDING: set when loc req pending, reset after on detection location update.
     * BEACON_WAITING: set when start leaving active scan. reset inside check beacon scan result.
     */
    private enum DetectionState {
        HAS_LOCATION_RULE(1<<0), RUNNING(1<<1), LOCATIONFIX_PENDING(1<<2), BEACON_WAITING(1<<3),
        HAS_HIDDEN_LOCATION_RULE(1<<4);
        private final int flag;
        DetectionState(int flag) {
            this.flag = flag;
        }
        public int getFlag() {
            return flag;
        }
    };
    private int mStateFlag = 0;

    private  WorkHandler mWorkHandler;
    private  MessageHandler mHandler;

    private Context mContext;
    private LocationSensorManager mLSMan;

    @SuppressWarnings("unused")
    private Handler mLSManHdl;   // save lsman's handle so that we can send message back to parent later.

    public DetectionController mDetCtrl;
    PoiAdapter mPoiAdapter;  // expose to pkg

    private LocationManagerWrapper mLocationManager;
    //private LocationMonitor mLocMon;
    private TelephonyMonitor mTelMon;
    BeaconSensors mBeacon;   // those are injected in components, not this module's private, should be pkg private!

    private volatile LocationTimerTask mTimerTask;   // periodical update trigger engine.

    private PendingIntent mProximityAlertIntent = null;
    private ProximityAlertReceiver mProxyRecver;

    private Tuple mCurPoiTuple = null;  // changed only inside enter leaving poi and checkDiscoveredLocationPoi
    private Location mCurLoc = null;    // this to indicate we got at least one location fix.

    private PoiWifi mCurPoiWifi;   // for storing ssid bag, an addition to poi tuple.

    private static final int LEAVING_WIFI_DEBOUNCE_COUNT = 1;  // debounce count, reduce the count to 1 scan is good enough.
    private int mWifiLeavingDebounceCount = 0;  // debounce for wifi leaving....assert leaving only when two consecutive leave happened. Reset when leaving.
    private int mDebounceCount = 0;  // entering, great than 2, leaving, less than -2
    boolean notifiedVSMOnRestart = false;

    private BroadcastReceiver mLocationRuleRecv;    // listen to location rule intent

    @SuppressWarnings("unused")
    private LocationDetection() {} // hide default constructor

    /**
     * constructor, take the location sensor manager context and the handler so we can notify location sensor manager.
     * @param ctx  location sensor manager context.
     * @param hdl  location sensor message handler.
     */
    public LocationDetection(final Context ctx, final Handler hdl) {
        mWorkHandler = new WorkHandler(TAG);
        mHandler = new MessageHandler(mWorkHandler.getLooper());

        mContext = ctx;
        mLSMan = (LocationSensorManager)ctx;
        //mLocMan = (NoCellLocationManager)ctx;
        mLSManHdl = hdl;

        mDetCtrl = new DetectionController();
        mPoiAdapter = new PoiAdapter(mContext, mHandler);
        mCurPoiWifi = new PoiWifi();
        mTimerTask = new LocationTimerTask(mLSMan, this);   // create the timer task.
        // mLocMon = new LocationMonitor(mLSMan, mHandler);  // need detection's own loc monitor here...use my mhandler here !

        // leverage proximity alert
        mLocationManager = (LocationManagerWrapper)SystemWrapper.getSystemService(mContext, Context.LOCATION_SERVICE);
        //mProxyRecver = new ProximityAlertReceiver(mContext, mHandler);
        //mProxyRecver.start();
        //addProximityAlerts();
        //LocationUpdatePendingIntentReceiver locRecver = new LocationUpdatePendingIntentReceiver(mContext, mHandler);
        //locRecver.start();

        // listen to location rules.
        initLocationRuleReceiver();

        mHandler.sendEmptyMessage(Msg.INIT_COMPLETE);
    }

    /**
     * initialize reference to Telephony monitor...
     * outside from constructor due to two way dependency between the two components.
     * @param telmon
     * @param beacon
     */
    public void setTelMonBeacon(final TelephonyMonitor telmon, final BeaconSensors beacon) {
        mTelMon = telmon;
        mBeacon = beacon;
        mBeacon.startPassiveBeaconListen(mHandler);  // always listen to wifi scan.
    }

    public Handler getDetectionHandler() {
        return mHandler;
    }

    private void initComplete() {
        mPoiAdapter.refreshPoiList();
    }

    /**
     * called from location manager on destroy
     */
    public void cleanup() {
        mPoiAdapter.unregisterPoiObserver();
        stopDetection();
        //mWorkHandler.finalize();
        mTimerTask.clean();
    }

    /**
     * get current poi tuple
     */
    public Tuple getCurrentPoiTuple() {
        return mCurPoiTuple;
    }

    /**
     * get the timer task
     */
    public LocationTimerTask getDetectionTimerTask() {
        return mTimerTask;
    }

    /**
     * get poi adapter
     */
    public PoiAdapter getPoiAdapter() {
        return mPoiAdapter;
    }


    public PoiWifi getPoiWifi() {
        return mCurPoiWifi;
    }

    /**
     * Strategy Pattern: detection algorithm using either lat lng or wifi match.
     * detection controller encapsulates all the detection logics needed to detect a poi.
     * include:
     *   1. find a cell is a poi cell, and start detection engine.
     *   2. find any wifi belongs to any poi, and detect by wifi match.
     *   3. start / stop periodical update needed for detection.
     *   4. process other conditions for detection, e.g, wifi on/off, airplane mode, pno, smart scan, etc.
     *   4. wifissid=[{"wifibssid":"5c:0e:8b:30:f2:c1","wifissid":"MMI-Internet"},{"wifibssid":"5c:0e:8b:30:f2:c0","wifissid":"DSA-Wireless"},...]
     */
    public class DetectionController {
        private final static int DETALG_GOOGLEFIX = 0;
        private final static int DETALG_WIFIMATCHING = 1 << 0;

        private Tuple mCurPoi = null;
        private Tuple mNextPoi = null;

        public DetectionController() {
        }

        public Tuple getCurPoi() {
            return mCurPoi;
        }

        public Tuple setCurPoi(Tuple poituple) {
            mCurPoi = poituple;
            return mCurPoi;
        }

        public Tuple setNextPoi(Tuple nextpoituple) {
            mNextPoi = nextpoituple;
            return mNextPoi;
        }

        public Tuple getNextPoi() {
            return mNextPoi;
        }

        public String getPoiNames() {
            String ret = "CurPoi ";
            if(mCurPoi != null) ret += mCurPoi.getPoiName();
            if(mNextPoi != null) ret += " NextPoi " + mNextPoi.getPoiName();
            return ret;
        }

        public boolean currentlyInsidePoi() {
            return mCurPoi != null;
        }

        /**
         * can this poi be detected by wifi match ?
         * 100 is the no. of chars for at least two wifi ssid, check above wifissid string format.
         */
        public boolean poiHasWifi(Tuple poituple) {
            return (poituple != null && poituple.getWifiSsid() != null && (poituple.getWifiSsid().trim().length() > 100));
        }

        /**
         * startPeriodicalUpdate, to save power, do not use location request if all poi's have wifi
         */
        private void startPeriodicalUpdate() {
            mTimerTask.startPeriodicalUpdate();
        }

        private void stopLocationUpdate() {
            mTimerTask.stopPeriodicalUpdate(true, false);
        }
        private void stopWifiScan() {
            mTimerTask.stopPeriodicalUpdate(false, true);
        }
        private void stopPeriodicalUpdate() {
            stopLocationUpdate();
            stopWifiScan();
        }

        /**
         * stop detection upon wifi connected
         */
        public boolean stopIfWifiConnected() {
            if (currentlyInsidePoi() && ( mBeacon.getWifiConnState() == 1 )) {
                //stopPeriodicalUpdate(true, true);  // light weight stop detection by stop location request.
                if(LOG_INFO) Log.i(TAG, "runDetection: stop loc req upon wifi connected and inside poi and clear fix pending flag.");
                return true;
            }
            return false;
        }

        /**
         * stop detection upon no movement
         */
        public boolean stopIfNoMovement() {
            //if (currentlyInsidePoi() && poiHasWifi(mCurPoi) && mTimerTask.isEndOfMovement()) {
            if (currentlyInsidePoi() && mTimerTask.isEndOfMovement(LocationTimerTask.EXTENDED_STATION)) {
                if(LOG_INFO) Log.i(TAG, "runDetection: stop loc req upon inside poi using wifi and no movement.");
                //stopPeriodicalUpdate(true, true);
                return true;
            }else if( !currentlyInsidePoi() && mTimerTask.isEndOfMovement(2*LocationTimerTask.EXTENDED_STATION)) {
                // IKHSS7-42128 need to stop wifi scan if user is at home, which is close by work, but did not
                // set home as a POI. Ideally, should just remove the inside POI requirement. However, for locations
                // where only 2 wifis exist, it might need multiple round of scans to pick all wifis. This might need
                // more than 10 minutes. Hence, for entering, double the no movement checking time.
                if(LOG_INFO) Log.i(TAG, "runDetection: stop loc req if not in any poi and no movement.");
                return true;
            }
            return false;
        }

        /**
         * stop detection if airplane mode
         *   1. no poi
         *   2. airplane mode on
         */
        private boolean stopIfAirplaneMode() {
            boolean airplaneOn = Utils.isAirplaneModeOn((Context)mLSMan);
            if(!hasPoiAndLocationRules() || airplaneOn) {
                if(LOG_DEBUG) Log.d(TAG, "stopIfAirplaneMode : airplane : " + airplaneOn);
                //stopPeriodicalUpdate(true, true);
                return true;
            }
            return false;
        }

        /**
         * stop detection if pno registered
         */
        private boolean stopIfPnoRegistered(Tuple nextpoituple) {
            if(nextpoituple != null && nextpoituple.getPnoRegistration()) {
                if(LOG_DEBUG) Log.d(TAG, "runDetection: stop loc req when PNO is registered already. ");
                //stopPeriodicalUpdate(true, true);  // ensure we done at least one scan before shut down!!!
                return true;
            }
            return false;
        }

        /**
         * leave poi if wifi off
         */
        private boolean leaveIfWifiOff() {
            return (mNextPoi == null && currentlyInsidePoi() && poiHasWifi(mCurPoi) &&
                    !mBeacon.isWifiEnabled() && !mTimerTask.getPhoneModel().isSmartScanSupported());
        }

        /**
         * find next poi by cells, return true if we have next poi to detect, stop otherwise.
         * if currently not in a poi, and not nearby a poi, and we do not have any invalid cell, and we have at least got one latlng,
         * then we can say we are not anywhere near a poi, stop.
         * @param  current cell and bouncing cells
         * @return true if we find a poi to detect, false otherwise.
         */
        private boolean findNextPoiByCell(String curcell, Set<String> bouncingset) {
            boolean ret = true;  // we need to run unless we can stop.

            Tuple poituple = findPoiByMatchingCell(curcell, bouncingset);
            setNextPoi(poituple);   // set the next poi tuple.

            String bouncingCellIdsNearby = Utils.convertSetToString(bouncingset); // & delimited.
            if (bouncingCellIdsNearby == null) {  // caused by fake cell push
                ret = false;
            } else {
                if ( !currentlyInsidePoi() && poituple == null) {
                    if(LOG_INFO) Log.i(TAG, "startDetection: celltower changed : do nothing because not in a poi and cells are not POI cells ::" + bouncingCellIdsNearby);
                    // stopPeriodicalUpdate(false, true);  // stop wifi scan updates
                    ret = false;
                }
            }
            return ret;
        }

        /**
         * register next poi's strongest ssids to pno scan
         */
        private void registerPoiSsid(Tuple nextpoituple) {
            if(!currentlyInsidePoi() && nextpoituple != null) {
                // if reg suc, next cell change will fall into the above if condition, which will stop periodical update.
                mTimerTask.getPhoneModel().registerPnoSsid(nextpoituple);
                if(LOG_DEBUG) Log.d(TAG, "runDetection: register nextpoi topssid. " + nextpoituple.getTopSsid().toString());
            }
        }

        /**
         * unregister current poi's pno ssid
         * clean up pno scan list when no pno scan needed inside a POI.
         */
        private void unregisterPoiSsid(Tuple poituple) {
            mTimerTask.getPhoneModel().unregisterPnoSsid(poituple);
        }

        /**
         * Run detection is just start to requesting location update based on following conditions.
         * 1. if wifi is connected within an POI, do not request location update to save power.
         * 2. if wifi disconnected, run a full cycle of req, fix, dist, stop.
         * @param reason
         */
        public void runDetection(String reason) {

            if(stopIfWifiConnected() ||
                    stopIfNoMovement() ||
                    stopIfPnoRegistered(mNextPoi) ||
                    stopIfAirplaneMode()
              ) {
                stopPeriodicalUpdate();  // light weight stop detection by stop location request.
                if(LOG_INFO) Log.i(TAG, "runDetection: stop loc req and wifi scan " + reason);
                return;
            }

            registerPoiSsid(mNextPoi);

            setStateFlag(DetectionState.RUNNING.getFlag(), true);

            startPeriodicalUpdate();

            mLSMan.getMetricsLogger().logDetectionRequestLocationUpdate();
            if(LOG_INFO) Log.i(TAG, "runDetection: State:" + mStateFlag + " " + reason);
        }

        /**
         * calculate distance to poi based on lat lng.
         * @param lat  getLatitude()*1E6)/1E6
         * @param lgt  getLongitude()*1E6)/1E6
         * @param accuracy
         */
        public void detectPoiUponLocationFix(double lat, double lgt, long accuracy) {

            if(LOG_INFO) Log.i(TAG, "detectPOIUponLocationFix :: lat=" + lat + " ,lgt=" + lgt + " ,accuracy=" +accuracy);
            mLSMan.getMetricsLogger().logDetectionReceiveLocationUpdate(lat, lgt, (int) accuracy);

            // already in monitoring...assert if distance radius valid.
            Tuple nextPoiTuple = checkPoiDistance(lat, lgt, accuracy);

            if (currentlyInsidePoi() && !poiHasWifi(mCurPoi)) {
                detectLeavingPoi(lat, lgt, accuracy, nextPoiTuple);
            } else if(!currentlyInsidePoi() && !poiHasWifi(nextPoiTuple)) {
                detectEnteringPoi(lat, lgt, accuracy, nextPoiTuple);
            }

            // stop location tracking when no poi cells and currently not in any POI
            // never stop listening in no cell solution...always getting location updates.
            // if (mLSMan instanceof com.motorola.contextual.virtualsensor.locationsensor.LocationSensorManager) {
            if (!currentlyInsidePoi() && nextPoiTuple == null && null == findPoiByMatchingCell(mTelMon.getValueJSONString(), mTelMon.getBouncingCells())) {
                if(LOG_INFO) Log.i(TAG, "detectPOIUponLocationFix : STOP Fast Dection : not moving or leaving any POI...stop location monitoring");
                setNextPoi(null);
                //stopPeriodicalUpdate(true, false);  // stop loc provider
                stopLocationUpdate();
                // notifyVSMUponRestart();  // notify should always happen after beacon scan. no trust on incomplete cell info.
            }
            return;
        }

        /**
         * fine break the logic to differentiate between entering and leaving.
         * we should have larger entering distance...and smaller leaving distance.
         * return valid poi tuple if location is within a POI
         * return null if location is out of any POI
         * Do not change any global variable.
         */
        public Tuple checkPoiDistance(double lat, double lgt, long accuracy) {
            float dist, enteringDistMin = Integer.MAX_VALUE;
            long enteringR, leavingR;
            Tuple enteringPoiTuple = null, leavingPoiTuple = null;

            List<Tuple> poilist = mPoiAdapter.getPoiList();
            synchronized (poilist) {  // returned poi list is already synch wrapped...readonly, no needed to sync.
                // find the best match, not the first match.
                for (Tuple entry : poilist) {
                    dist = Utils.distanceTo(lat, lgt, entry.getLat(), entry.getLgt());
                    enteringR = tuneDetectionRadius(entry.getRadius(), accuracy, false);
                    if(LOG_INFO) Log.i(TAG, "checkPoiDistance :: " + entry.getPoiName() +  " dist=" + dist + " enteringR=" + enteringR+ " fix lat:lgt:accu=" + lat+","+lgt+","+accuracy + " Poi Lat:lgt:accuracy=" + entry.getLat()+","+entry.getLgt()+","+entry.getRadius());

                    // check entering any POI first.
                    if (dist < enteringR && dist < enteringDistMin) {
                        enteringDistMin = dist;
                        enteringPoiTuple = entry;
                        if(LOG_INFO) Log.i(TAG, "checkPoiDistance :: entering POI :" + entry.getPoiName() + " dist:enterR:enterDistMin: " + dist+ ":"+enteringR + ":" + enteringDistMin);
                    }
                }
            }

            // check whether we are leaving any poi
            if (currentlyInsidePoi()) {
                leavingR = tuneDetectionRadius(mCurPoi.getRadius(), accuracy, true);
                dist = Utils.distanceTo(lat, lgt, mCurPoi.getLat(), mCurPoi.getLgt());
                if (dist < leavingR) {
                    leavingPoiTuple = mCurPoi;
                    updatePoiCellsWithCurrentBouncingCells(false);  // picking up missing cell along the way out as long as its within radius.
                    if(LOG_INFO) Log.i(TAG, "checkPoiDistance ::  keep current POI=" + mCurPoi.getPoiName() + " dist=" + dist + " accu=" + accuracy + " leavingR=" + leavingR  );
                } else {
                    leavingPoiTuple = null;
                    if(LOG_INFO) Log.i(TAG, "checkPoiDistance :: delayed leaving thru beacon check...current POI=" + mCurPoi.getPoiName() + " dist=" + dist + " accu=" + accuracy + " leavingR=" + leavingR  );
                }
            }

            if (enteringPoiTuple != null) {
                if(LOG_INFO) Log.i(TAG, "checkPoiDistance : entering :" + enteringPoiTuple.getPoiName());
                setNextPoi(enteringPoiTuple);
                return enteringPoiTuple;
            } else if (leavingPoiTuple != null) {
                if(LOG_INFO) Log.i(TAG, "checkPoiDistance : keeping :" + leavingPoiTuple.getPoiName());
                return leavingPoiTuple;
            } else {
                if(LOG_INFO) Log.i(TAG, "checkPoiDistance : moving to unknown location after failed to find location dist to any poi...");
                return null;
            }
        }

        private long tuneDetectionRadius(long poiAccuracy, long fixAccuracy,  boolean leaving) {
            if (leaving) {
                return Math.max(fixAccuracy, poiAccuracy) + TARGET_RADIUS;
            }
            //enteringR = Math.min(2*TARGET_RADIUS, accuracy+TARGET_RADIUS);
            // case 1: fine poiAccu,   fine fixAccu,   R=min(2*50, fixAccu+50)
            // case 2: fine poiAccu,   coarse fixAccu, R=min(2*50, fixAccu+50)
            // case 3: coarse poiAccu, fine fixAccu:   R=poiAccu
            // case 4: coarse poiAccu, coarse fixAccu: R=poiAccu
            long enteringR = (poiAccuracy > 1000 || poiAccuracy == 0) ? Math.max(poiAccuracy, fixAccuracy) : Math.min(2*TARGET_RADIUS, fixAccuracy+TARGET_RADIUS);
            return Math.max(enteringR, LOCATION_DETECTING_DIST_RADIUS);  // can not detect within less than 200 meter.
        }

        /**
         * currently inside an POI, got new fix, detect whether we are leaving, or switching poi.
         */
        private void detectLeavingPoi(double lat, double lgt, long accuracy, Tuple nextPoiTuple) {

            // keeping POI, nothing changed
            int dist = 0;
            if (nextPoiTuple != null && mCurPoi.equals(nextPoiTuple)) {
                mLSMan.getMetricsLogger().logStayInPoi();
                keepingPOI(nextPoiTuple);
                dist = (int)Utils.distanceTo(lat, lgt, nextPoiTuple.getLat(), nextPoiTuple.getLgt());
                if(LOG_INFO) Log.i(TAG, " detectLeavingPoi: remain in POI:" + mCurPoi.getPoiName() + " [ Distance="+dist + " ,lat=" + lat + " ,lgt=" + lgt + " ,accuracy=" +accuracy);
            }
            // switching POI : ( current in a POI and move to another POI)
            else if (nextPoiTuple != null && !mCurPoi.equals(nextPoiTuple)) {
                // given radius is only 100 meters, we should never have overlapped POIs...
                if(LOG_INFO) Log.i(TAG, "detectLeavingPoi : switch POI: " + mCurPoi.getPoiName() + " ==>" + nextPoiTuple.getPoiName());
                mLSMan.getMetricsLogger().logSwitchPoi();
                switchingPOI(nextPoiTuple);  // decompose to leave and enter
            }
            // leaving a POI : ( current in a POI and moved out of _any_ POI)
            else if (nextPoiTuple == null) {
                dist = (int) Utils.distanceTo(lat, lgt, mCurPoi.getLat(), mCurPoi.getLgt());
                mLSMan.getMetricsLogger().logLeavingDistance(dist);
                if(LOG_INFO) Log.i(TAG, "detectLeavingPoi : no wifi info, Really Leaving POI >>>:" + mCurPoi.getPoiName() + "{lat:lgt:accuracy:"+lat+":"+lgt+":"+accuracy+"}");
                leavingPOI();   // leave poi, will do stop detection at the end.
            }
        }

        /**
         * currently not in any POI, got new fix, detect entering a POI.
         */
        private void detectEnteringPoi(double lat, double lgt, long accuracy, Tuple nextPoiTuple) {
            // entering a POI : (current not in any poi and moved in)
            if (nextPoiTuple != null) {
                mBeacon.startBeaconScan(null, mHandler);   // do a wifi scan if detect by google fix.
                float dist = Utils.distanceTo(lat, lgt, nextPoiTuple.getLat(), nextPoiTuple.getLgt());
                if(LOG_INFO) Log.i(TAG, " detectEnteringPoi: Moving into POI : " + nextPoiTuple.getPoiName() + "[ Distance="+dist + " ,lat=" + lat + " ,lgt=" + lgt + " ,accuracy=" +accuracy);
                mLSMan.getMetricsLogger().logEnterPoi();
                enteringPOI(nextPoiTuple);
            }
        }

        /**
         * Detection Strategy Algorithm: if wifi info available, use wifi match algorithm, otherwise, degrade to goog fix.
         *
         * sometimes, you get wild different location fix even though you did not move, this is called thrashing.
         * to filter out thrashing, use surrounding beacons to filter out thrashing wild location fixes. The
         * business rules to avoid thrashing are:
         *
         * 1. if any scanned bssid matches any known bssid for this POI, then stay, do not leave.
         * 2. if scan bssid does not match any bssid known in the poi, goto step 3, check for ssid match.
         * 3. if the ssid scanned matches any known ssid for the POI, then stay, do not leave.
         *
         * @return true if really leaving, false otherwise.
         */
        private boolean checkLeavingThrashing() {
            if(LOG_INFO) Log.i(TAG, "checkLeavingThrashing : poi wifi ssid: " + mCurPoi.getWifiSsid());
            // check whether we can use wifi match algorithm, if not, quit any wifi algorithm.
            if(!poiHasWifi(mCurPoi)) {
                updatePoiCellScannedWifiUponEntering();  // this will be one time update
                if(LOG_INFO) Log.i(TAG, "checkLeavingThrashing : poi does not have wifi info, but we got wifi scan...do nothing abt wifi: " + mCurPoi.getPoiName());
                return false;
            }

            // hack to debounce empty scan result
            // case: two empty scan happened back by back, before detection got chance to process it.
            int emptyscans = mBeacon.mWifiScanResult.getEmptyScans();
            if(LOG_DEBUG) Log.d(TAG, "checkLeavingThrashing : debounce empty scans: " + emptyscans);
            // IKHSS7-45876, debounce cnt set to 2, debounce 4 empty scans in short period. There are chances AP could
            // back to sleep in between, but we will catch in next round of scan
            if ( emptyscans > 0 && mWifiLeavingDebounceCount <= 2 ){  // if empty scan, and we have not checked debounce
                if(LOG_INFO) Log.i(TAG, "checkLeavingThrashing : got empty scan result, start my scan...empty scans: " + emptyscans);
                mWifiLeavingDebounceCount += 1;     // now set the flag saying we have checked the debounce
                mHandler.removeMessages(BEACONSCAN_RESULT);      // clear all scan available message before this scan result come back.
                //mLSMan.holdWakeLock("empty scan happened, re-scan");
                mBeacon.startBeaconScan(null, mHandler);   // start a wifi scan immediately upon 
                return false;
            }
            //mBeacon.mWifiScanResult.clearEmptyScans();  // when we proceed, always clear the flag.
            mWifiLeavingDebounceCount = 0;   // reset count when we take wifi scan result as valid.

            boolean leaving = false;
            if (!mBeacon.mWifiScanResult.matchBssid(mCurPoiWifi.mWifiMap.keySet())) {
                // XXX comment out, no drive test needed, just walk to the other side of building.
                if ( !mBeacon.mWifiScanResult.matchSsid(mCurPoiWifi.mBagSsid)) {
                    leaving = true;
                }
            }

            if (leaving) {
                if(LOG_INFO) Log.i(TAG, "checkLeavingThrashing : wifi leaving counts :" + mWifiLeavingDebounceCount);
                if(LOG_INFO) Log.i(TAG, "checkLeavingThrashing : no match...really Leaving POI :" + mCurPoi.getPoiName());
                leavingPOI();   // should we post a message or just call leave directly
                runDetection("Leaving POI, start detection to verify leaving");
                return true;   // now really left
            } else {
                if(LOG_INFO) Log.i(TAG, "checkLeavingThrashing : match, not leaving....reset wifi leaving counts :" + mCurPoi.getPoiName());
            }
            // when reach here, either matchmac true, or no mac match, but bag ssid matches.
            if(LOG_INFO) Log.i(TAG, "checkLeavingThrashing : match...thrashing Leaving  POI :" + mCurPoi.getPoiName());
            mLSMan.getMetricsLogger().logFalseLeavePoi();
            return false;
        }

        /**
         * call path: from beacon scan available msg, check beacon scan result, match beacons.
         * scan surrounding wifis as a way to detect entering or leaving a location only when data connection is bad.
         * wifi is the least priority that work only when data connection is not there: google update is the first priority.
         * if wifi matches poi's, then entering is asserted.
         * if inside a poi, and none wifi matches, leaving can be asserted quickly.
         * @return true if decision is made( entering/leaving), false if no decision has been made.
         *
         * Note: update of POI wifi's only happended from lsman after location capture 15 minutes.
         */
        boolean matchBeacons() {
            // check leaving if already inside a POI. will assert leaving if not beacon match.
            if (currentlyInsidePoi()) {
                if(LOG_INFO) Log.i(TAG, "matchBeacons: checking leaving by calling check leaving thrashing Poi:" + mCurPoi.getPoiName());
                return checkLeavingThrashing();   // will change poi state if no match leaving.
            }

            // now match entering beacons.
            boolean match = false;
            int maxmatches = 2;
            int curmatches = 0;
            Tuple poituple = null;
            Set<String> scannedbeaconset = mBeacon.getLastWifiScanSsidMap().keySet();

            // String indexOf match is easy and quick for bssid b/c bssid is fixed in length!
            List<Tuple> poilist = mPoiAdapter.getPoiList();
            // poituple = getMatchingPoiOfCell(Utils.convertSetToString(mTelMon.getBouncingCells()));
            synchronized (poilist) {  // returned poi list is already synch wrapped...no sync needed here.
                // for each  poi, check the scanned wifi, find the poi with max matches,
                for (Tuple entry : poilist) {
                    curmatches = Utils.intersectSetJsonArray(scannedbeaconset, null, entry.getWifiSsid());
                    if ( curmatches >= maxmatches) {
                        match = true;
                        poituple = entry;
                        maxmatches = curmatches;  // update the max
                    }
                }
            }

            if (match) {   // now match count at least 2, take anything greater than 2!
                if(LOG_INFO) Log.i(TAG, "matchBeacons: Beacon match...entering POI :" + poituple.getPoiName());
                enteringPOI(poituple);
                runDetection("Beacon match poi, runDetection...");
                return true;
            } else {
                if(LOG_INFO) Log.i(TAG, "matchBeacons: Beacon no match any poi...:");
                return false;
            }
        }

        /**
         * callback when wifi scan available.
         */
        void detectPoiUponBeaconScanResult() {
            matchBeacons();

            // now if not in any poi, and cell off, turn off periodical wifi scan.
            if(!findNextPoiByCell(mTelMon.getValueJSONString(), mTelMon.getBouncingCells())) {
                if(LOG_INFO) Log.i(TAG, "detectPoiUponBeaconScanResult:  no match poi, stop wifi scan timer");
                stopWifiScan();
            } else if(mDetCtrl.stopIfNoMovement()) {
                if(LOG_INFO) Log.i(TAG, "detectPoiUponBeaconScanResult: stop loc req upon inside poi using wifi and movement sensor available.");
                stopWifiScan();
            }
            return;
        }
    }

    /**
     * inner class that encapsulate all poi wifi information.
     */
    public static class PoiWifi {
        private Map<String, String> mWifiMap;   // cur poi's scanned wifi populated from db. Updated inside entering/leavingPOI
        private Set<String> mBagSsid;           // a set of ssids that are ssid bag. i.e., dup ssid name. Updated upon poi change.
        private Map<String, Integer> mWifiSS;   // the signal strength for wifi

        private Comparator<String> mCmp = new Comparator<String>() {
            public int compare(String s1, String s2) {
                int v1 = mWifiSS.get(s1);
                int v2 = mWifiSS.get(s2);
                //return v1-v2;   // nature order 1,2,3
                return v2-v1;   // reverse order 3,2,1
            }
        };

        public PoiWifi() {
            mWifiMap = new ConcurrentHashMap<String, String>();   // do not need to be strict exact,
            mBagSsid = new HashSet<String>();
            mWifiSS = new HashMap<String, Integer>();
        }

        public void clear() {
            mWifiMap.clear();   // clear the map
            mBagSsid.clear();
            mWifiSS.clear();
        }

        /**
         * update the content with poi ssid
         */
        public void updateCurPoi(String ssidjsonarray) {
            clear();
            JSONUtils.convertJSonArrayToMap(ssidjsonarray, mWifiMap, mBagSsid, mWifiSS);
        }

        /**
         * sort the ssid based on signal strength
         * @return an arraylist with ssid sorted with signal strength.
         */
        public List<String> getStrongSSID() {
            List<String> l = new ArrayList<String>();
            l.addAll(mWifiSS.keySet());
            Collections.sort(l, mCmp);
            return l;
        }
    }


    /**
     * whether location listening monitoring is on-going, only refered from UI status.
     */
    public boolean isDetectionMonitoringON() {
        if(LOG_DEBUG) Log.d(TAG, "isDetectionMonitoringON: " + mStateFlag + " and CurPoi=" + mCurPoiTuple);
        return isStateFlagSet(DetectionState.RUNNING.getFlag());
    }

    /**
     * set the state flag, if reset fix pending flag, stop location listener cause we got the fix.
     * @param flag, the flag bit defined previous
     * @param set, true if we are setting this flag bit, false we are clearing this flag bit.
     */
    private void setStateFlag(int flag, boolean set) {
        if (set) {
            mStateFlag |= flag;
        } else {
            mStateFlag &= (~flag);
        }
        if(LOG_INFO) Log.i(TAG, "setStateFlag: flag=" + flag + " Set(true)Reset(false)="+set+ " mStateFlag=" + mStateFlag);
    }

    /**
     * check whether certain flag is set
     */
    private boolean isStateFlagSet(int flag) {
        return (mStateFlag & flag) == flag;
    }

    /**
     * messge looper
     */
    final class MessageHandler extends Handler {
        public MessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            processMessage(msg);
        }
    }
    private void processMessage(android.os.Message msg) {
        switch (msg.what) {
        case Msg.NULL:
            if(LOG_DEBUG) Log.d(TAG, "processMessage() : MSG_NULL");
            break;
        case Msg.INIT_COMPLETE:
            if(LOG_DEBUG) Log.d(TAG, "processMessage() : INIT_COMPLETE");
            initComplete();
            break;
        case Msg.START_DETECTION:   // from tel mon when cell tower changed
            if(LOG_DEBUG) Log.d(TAG, "processMessage() : START_DETECTION");
            startDetection();
            break;
        case LOCATION_UPDATE:   // use the global definition
            if(LOG_DEBUG) Log.d(TAG, "processMessage() : LOCATION_UPDATE available from Location monitor");
            onDetectionLocationUpdate();
            break;
        case Msg.ENTERING_POI:
        case Msg.LEAVING_POI:
            updateDetectionState((Tuple)msg.obj);
            break;
        case Msg.PROXIMITY_ALERT:
            if(LOG_DEBUG) Log.d(TAG, "processMessage() : PROXIMITY_ALERT pending intent...runDetection with listening update...");
            mDetCtrl.runDetection(" Run detection upon ProximityAlert: {ProximityAlert:true}");
            break;
        case Msg.POI_REFRESH:
            if(LOG_DEBUG) Log.d(TAG, "processMessage() : POI_REFRESH from poi adapter....");
            onPoiRefresh();
            break;
        case Msg.START_SCAN:
            if(LOG_DEBUG) Log.d(TAG, "processMessage() : START_SCAN from timer task....");
            startScan();
            break;
        case BEACONSCAN_RESULT:
            if(LOG_DEBUG) Log.d(TAG, "processMessage() : BEACONSCAN_RESULT Async done....check beacon scan results.");
            processBeaconScanResult();
            break;
        case WIFI_CONNECTED:
            if(LOG_DEBUG) Log.d(TAG, "processMessage() : Wifi connect state changed.");
            handleWifiConnStateChanged((String)msg.obj);
            break;
        case WIFI_STATE_CHANGED:
            if(LOG_DEBUG) Log.d(TAG, "processMessage() : WIFI_STATE_CHANGED...run detection again");
            mDetCtrl.runDetection(" upon wifi state changed...");
            break;
        case Msg.LOCATION_PENDINGINTENT:
            if(LOG_DEBUG) Log.d(TAG, "processMessage(): Location Fix pending intent received");
            handleLocationFixFromPendingIntent((Location)msg.obj);
            break;
        case Msg.MOTION_TRIGGER:
            if(LOG_DEBUG) Log.d(TAG, "processMessage() : MOTION_TRIGGER...run detection again");
            mDetCtrl.runDetection(" upon recvd motion trigger");
            break;
        default:
            if(LOG_DEBUG) Log.d(TAG, "processMessage() : wrong message what=" + msg.what);
            break;

        }
    }

    /**
     * this determines whether an nearby cell id is invalid, returns true if any nearby cell is invalid
     * @param bouncingCellIdsNearby
     * @return true if all bouncing cells are valid, false otherwise
     */
    @Deprecated
    private boolean areAllCellsGood(String bouncingCellIdsNearby) {
        return (bouncingCellIdsNearby != null && TelephonyMonitor.isCellJsonValid(bouncingCellIdsNearby));  // all cells are good
    }

    /**
     * check if any POI tagged by the user (in the persistent store) has a matching cell ID
     * we could have no cell updates for a while, if phone is in sleep. We need to sort pois based on
     * the num of matches of bouncing cells. Then select the first one that has current cell matches also.
     * @param curcell the cell of current cell update
     * @param bouncingset the bouncing cell set
     * @return tuple of the matching POI.
     */
    private Tuple findPoiByMatchingCell(String curcell, Set<String> bouncingset) {
        List<Tuple> poilist = mPoiAdapter.getPoiList();
        //Set<String> bouncingset = mTelMon.getBouncingCells();
        //String curcell = mTelMon.getValueJSONString();

        if(LOG_DEBUG) Log.d(TAG, "cellChangeMatchAnyPoi : bouncing cells:: " + bouncingset);
        if (bouncingset.size() == 0 || poilist.size() == 0) {
            if(LOG_DEBUG) Log.d(TAG, "cellChangeMatchAnyPoi null bouncing cells...or fake cell push..:: or empty poi: " + poilist.size());
            return null;
        }

        Tuple bestcurcellpoi = null;
        int bestcurcellmatch = 0;
        Tuple bestnocurcellpoi = null;
        int bestnocurcellmatch = 0;

        synchronized (poilist) { // compound actions, client side locking is needed
            for (Tuple entry : poilist ) {
                Set<String> poicellset = Utils.convertStringToSet(entry.getCellJson());

                int matches = Utils.getMatchCount(poicellset, bouncingset);
                if(poicellset.contains(curcell)) {   // in the matched category
                    if(matches > bestcurcellmatch) { // update matched category
                        bestcurcellpoi = entry;
                        bestcurcellmatch = matches;
                    }
                } else { // in the NOT matched category
                    if(matches > bestnocurcellmatch) { // update matched category
                        bestnocurcellpoi = entry;
                        bestnocurcellmatch = matches;
                    }
                }
            }
        }

        if(bestcurcellpoi != null) {
            if(LOG_DEBUG) Log.d(TAG, "cellChangeMatchAnyPoi with current cell:: " + bestcurcellpoi.getPoiName() + " " + bestcurcellpoi.get_id() + " " + bestcurcellpoi.getName());
            return bestcurcellpoi;
        } else if(bestnocurcellpoi != null) {
            if(LOG_DEBUG) Log.d(TAG, "cellChangeMatchAnyPoi with NO current cell:: " + bestnocurcellpoi.getPoiName() + " " + bestnocurcellpoi.get_id() + " " + bestnocurcellpoi.getName());
            return bestnocurcellpoi;
        } else {
            if(LOG_DEBUG) Log.d(TAG, "cellChangeMatchAnyPoi :: not a poi cell");
            // XXX hack here to test PNO. return poilist.get(0);
            return null;
        }
    }

    /**
     * find the poi by connected wifi bssid, return the found poi tuple, or null otherwise.
     */
    private Tuple getMatchingPoiByConnWifi(String bssid) {
        Tuple poituple = null;
        List<Tuple> poilist = mPoiAdapter.getPoiList();
        String dbconnwifi = null;

        if(LOG_DEBUG) Log.d(TAG, "getMatchingPoiByConnWifi :" + bssid);
        if (null == bssid || poilist.size() == 0) {
            if(LOG_DEBUG) Log.d(TAG, "getMatchingPoiByConnWifi invalid bssid or empty poi: " + poilist.size());
            return null;
        }
        synchronized (poilist) { // returned poi list is already synch wrapped...readonly, no needed to sync.
            for (Tuple entry : poilist ) {
                //Set<String> dbWifiSet = Utils.convertStringToSet(entry.getWifiConnMac());  // connected wifi only
                //if (dbWifiSet.contains(bssid)) {
                // String indexOf match is easy and quick for bssid b/c bssid is fixed in length!
                dbconnwifi = entry.getWifiConnMac();  // connected wifi only
                if (dbconnwifi != null && dbconnwifi.indexOf(bssid) >= 0) {
                    poituple = entry;
                    if(LOG_DEBUG) Log.d(TAG, "getMatchingPoiByConnWifi matches tuple :: " + poituple.getPoiName() + " ::value::" + poituple.toString());
                    break;  // first match breaks.
                }
            }
        }
        return poituple;
    }

    /** evaluate if detection needs to be started because the new cell tower we just changed into may
     * be near a POI. As a reminder, this detection is looking for a WiFi access point match against
     * the given POI returned in getMatchingPoiOfCell.
     *
     * start location detection only when we are currently in a POI or about into a POI based on cell ids.
     * because this is cell id driven, we know when we are in neary by cells for a poi.
     */
    public void startDetection() {

        if(LOG_INFO) Log.i(TAG, "startDetection: celltower changed and startDetection...");

        if(!hasPoiAndLocationRules()) {
            if(LOG_INFO) Log.i(TAG, "startDetection: celltower changed : stop location detection...no POIs");
            return;
        }

        if (!mDetCtrl.findNextPoiByCell(mTelMon.getValueJSONString(), mTelMon.getBouncingCells())) {
            if(LOG_INFO) Log.i(TAG, "startDetection: return because not in a poi and cells are not POI cells");
            return;
        }

        if (mDetCtrl.leaveIfWifiOff()) {
            if(LOG_INFO) Log.i(TAG, "startDetection: leave poi upon new cell when wifi off...conti run detection.");
            leavingPOI();
        }

        // Start location monitoring by poi cells or currently in a POI.
        mDetCtrl.runDetection("Run Detection upon cell tower change event : " + mDetCtrl.getPoiNames());
    }

    /**
     * stop location detection by remove location update request to save power.
     * called from two places: 1. dist off after location fix, 2. leaving poi asserted.
     */
    private void stopDetection() {
        notifyVSMUponRestart();  // notify vsm when detection is done, if we have not notified.
        mStateFlag = 0;  // clear all bit, where running flag be reset to 0
        mDetCtrl.stopPeriodicalUpdate();
        if(LOG_DEBUG) Log.d(TAG, "STOP Fast Detection: Current POI null and Cell tower has no POI");
        mLSMan.getMetricsLogger().logNoWhere();
    }

    /**
     * call back upon location fix available.
     *   1. find the closest poi to this fix by distance calculation.
     *   2. if the closest poi has any wifi info captured previously, use wifi match rather than distance match, as fix might not accurate.
     *   3. otherwise, degrade to pure distance driven algorithm.
     */
    private void onDetectionLocationUpdate() {
        mCurLoc = mTimerTask.getCurrentLocation();
        if (mCurLoc == null) {
            if(LOG_DEBUG) Log.d(TAG, "onDetectionLocationUpdate :: current location is null");
            return;
        }
        double lat = (int)(mCurLoc.getLatitude()*1E6)/1E6;   // 1E6 is good enough
        double lgt = (int)(mCurLoc.getLongitude()*1E6)/1E6;
        long accuracy = mCurLoc.hasAccuracy() ? (long)mCurLoc.getAccuracy() : 0;  // get the accuracy
        mDetCtrl.detectPoiUponLocationFix(lat, lgt, accuracy);
    }

    /**
     * check whether discovered location is a configured meaningful location in Poi table.
     * return poitag, if match; null otherwise.
     */
    public String checkDiscoveredLocationPoi(double lat, double lgt, long accuracy, String curpoi, boolean serviceRestarted) {
        String poitag = null;

        LocationDatabase.PoiTable.Tuple poituple = mDetCtrl.checkPoiDistance(lat, lgt, accuracy);  // consistently use detection's logic
        if (poituple != null) {
            mDetCtrl.setNextPoi(poituple);   // set next poi so next round of detection knows what poi to detect.
            poitag = poituple.getPoiName();
        }
        if (mDetCtrl.currentlyInsidePoi()) {
            poitag = mCurPoiTuple.getPoiName();
        }
        mDetCtrl.runDetection(" Location discovered and start detection...");
        return poitag;
    }

    /**
     * called upon entering/leaving POI. return false if still debouncing, return true if affirmative.
     * @param entering
     * @return
     */
    private boolean detectionDebouncing(boolean entering) {
        if (entering) {
            mDebounceCount++;
            if (mDebounceCount > 2) {
                mDebounceCount = 0;
                return true;
            }
        } else {
            mDebounceCount--;
            if (mDebounceCount < -2) {
                mDebounceCount = 0;
                return true;
            }
        }
        if(LOG_DEBUG) Log.d(TAG, "detectionDebouncing : entering =" + entering + " still deboucing with count=" + mDebounceCount);
        return false;
    }

    /**
     * take a detailed log when notify VSM.
     */
    @Deprecated
    private String notifyVSMLog() {
        long now = System.currentTimeMillis();
        String matchlog = " poi="+mCurPoiTuple.getPoiName()+",timestamp="+now;
        if (mCurLoc!=null) {
            // within any POI's radius ?
            double lat = (int)(mCurLoc.getLatitude()*1E6)/1E6;   // 1E6 is good enough
            double lgt = (int)(mCurLoc.getLongitude()*1E6)/1E6;
            long accuracy = mCurLoc.hasAccuracy() ? (long)mCurLoc.getAccuracy() : 0;  // get the accuracy
            matchlog += ",lat="+lat+",lgt="+lgt+",accuracy="+accuracy;
        }
        return matchlog;
    }

    /**
     * detected entering POI, notify VSM and update cur poi
     */
    private void enteringPOI(final Tuple entry) {
        if ( ! mLSMan.isUserConsentOnMotoLocation()) {
            if(LOG_INFO) Log.i(TAG, "enteringPOI : user does not consent on, do nothing ");
            return;
        }

        String poi = entry.getPoiName();
        mCurPoiTuple = entry;
        mDetCtrl.setCurPoi(mCurPoiTuple);
        mLSMan.mLSApp.mAppPref.setString(AppPreferences.POI, poi);
        mDetCtrl.unregisterPoiSsid(mDetCtrl.getCurPoi());
        // this will update poi wifi map, must go together with the setting of cur poi tuple.
        updatePoiCellScannedWifiUponEntering();

        mContext.sendBroadcast(new Intent(VSM_LOCATION_CHANGE).putExtra("status", "arrive"), PERMISSION);
        mLSMan.mLSApp.getVsmProxy().sendVSMLocationUpdate(poi);

        if(LOG_INFO) Log.i(TAG, "enteringPOI : notify VSM >>>Entering POI:" + entry.getPoiName());
    }

    /**
     * detected leaving POI, notify VSM and update cur poi
     */
    private void leavingPOI() {
        if (!detectionDebouncing(false)) {
            //return;
        }

        final String exitpoi = mCurPoiTuple.getPoiName();

        mCurPoiTuple = null;
        mDetCtrl.setCurPoi(mCurPoiTuple);
        mCurPoiWifi.clear();   // clear poi wifi, must go together with the setting of cur poi tuple.
        mLSMan.mLSApp.mAppPref.setString(AppPreferences.POI, TRANSIENT);
        mLSMan.getMetricsLogger().logLeavePoi();

        mContext.sendBroadcast(new Intent(VSM_LOCATION_CHANGE).putExtra("status", "leave"), PERMISSION);
        mLSMan.mLSApp.getVsmProxy().sendVSMLocationUpdate(TRANSIENT);
        if(LOG_INFO) Log.i(TAG," leavingPOI : notify VSM >>>Leaving POI:" + exitpoi);
    }

    /**
     * detected switching POI, notify VSM and update cur poi
     *   given radius is only 100 meters, we should never have overlapped POIs...
     */
    private void switchingPOI(final Tuple entry) {
        String leftpoi = mCurPoiTuple.getPoiName();
        leavingPOI();         // first, leave current POI
        enteringPOI(entry);   // second, entering new poi
        if(LOG_INFO) Log.i(TAG, "switchingPOI :: notify VSM : >>> switchingPOI POI from <<< ::" + leftpoi + " to >>>" + entry.getPoiName());
    }

    /**
     * detected keeping POI, do nothing
     */
    private void keepingPOI(final Tuple entry) {
        String poi = mCurPoiTuple.getPoiName();
        updatePoiCellsWithCurrentBouncingCells(false);
        if(LOG_INFO) Log.i(TAG, "keepingPOI :: POI : " + poi + " did not change, just updating poi celljsons.");
    }

    /**
     * Process got randomly killed, ensure async state update intent delivered robusted.
     * This must be called after match beacon performed, to ensure transient is valid, not a thrashing.
     *
     * Notify vsm upon either of restarted.
     * if ls restarted, inside POI, ls will notify vsm upon detect poi.
     *                  not in POI, need to notify transient upon no cell match.
     * if vsm restarted, inside POI, lsman will handle it through VSM_INIT_COMPLETE and onStartCommand.
     *                   not in POI, reset the notified flag and ls det will notify transient upon no cell match.
     */
    private void notifyVSMUponRestart() {   // this func called only when no cell match.
        //if (!notifiedVSMOnRestart && mCurPoiTuple == null) {
        //    String poistatus = mLSMan.mLSApp.mAppPref.getString(AppPreferences.POI);
        //    if (poistatus != null && !TRANSIENT.equals(poistatus)) {
        //        if(LOG_DEBUG) Log.d(TAG, "notifyVSMUponRestart : transient");
        //        mLSMan.mLSApp.getVsmProxy().sendVSMLocationUpdate(TRANSIENT);
        //        notifiedVSMOnRestart = true;
        //    }
        //}

        // KUT-53024, always notify VSM for transient when after re-started
        //if(!hasPoiAndLocationRules()) {
        //    return;   // if no poi at all, no need to notify vsm about transient.
        //}

        if(!notifiedVSMOnRestart) { // one time bcast upon restarted!
            String poistatus = null;
            if(mCurPoiTuple == null) { // transient if not in poi
                poistatus = TRANSIENT;
            } else {  // should not be here.
                poistatus = mCurPoiTuple.getPoiName();  // use cur poi name
            }

            setNotifyVSMOnRestart(true);
            if(LOG_DEBUG) Log.d(TAG, "notifyVSMUponRestart : "+ poistatus);
            mLSMan.mLSApp.getVsmProxy().sendVSMLocationUpdate(poistatus);
        }
    }

    /**
     * reset the notified vsm flag, upon recv VSM_INIT_COMPLETE
     * not using sync because it is not critical to get stale value here.
     */
    public void setNotifyVSMOnRestart(boolean flag) {
        notifiedVSMOnRestart = flag;
    }

    /**
     * called from enteringPOI only.
     * update poi celljsons and scanned wifi upon entering. Scanned wifi come from db, so guaranteed to be correct.
     * collect wifi from loctime table by two means: by lat/lng distance match, or by wifi fuzzy match.
     * If privacy is enforced and we write 0 to loctime table, we need to use wifi fuzzy match all the time.
     */
    private void updatePoiCellScannedWifiUponEntering() {
        JSONArray poiWifiSsidArray = null;
        String poiWifiSsid = mCurPoiTuple.getWifiSsid();
        String curWifiSsid = mBeacon.getLastWifScanSsid(true);
        if (poiWifiSsid == null) {
            // poiWifiSsid = mBeacon.getLastWifScanSsid(false);  // do not take junk !. ok, take whatever we have for the first time.
            poiWifiSsid = curWifiSsid;
        }

        if (poiWifiSsid != null) {
            if(LOG_DEBUG) Log.d(TAG, "updatePoiCellScannedWifiUponEntering : using Wifi :" + poiWifiSsid);
            poiWifiSsidArray = JSONUtils.getJsonArray(poiWifiSsid);
            // XXX Looking into discovery for wifi record is a bad idea. get fresh directly from current scan.
            // mLSMan.mStore.collectCellJsonWifiSsid(poiWifiSsid, poiWifiSsidArray, null);
            JSONUtils.mergeJsonArrays(poiWifiSsidArray, JSONUtils.getJsonArray(curWifiSsid), true);  // update ss
            mCurPoiTuple.setWifiSsid(poiWifiSsidArray.toString());   // set the scanned wifi only
        } else {  // use wifi match as consolidation algo.
            //poiWifiSsidArray = new JSONArray();
            //mLSMan.mStore.collectCellJsonWifiSsid(mCurPoiTuple.getLat(), mCurPoiTuple.getLgt(), mCurPoiTuple.getRadius(), poiWifiSsidArray, null);
            if(LOG_DEBUG) Log.d(TAG, "updatePoiCellScannedWifiUponEntering : no wifi around poi, do nothing");
        }
        if(LOG_INFO) Log.i(TAG, "updatePoiCellScannedWifiUponEntering : Updated Wifi :" + mCurPoiTuple.getWifiSsid());

        updatePoiCellsWithCurrentBouncingCells(true);  // update POI's cell once entering into a POI with its bouncing cells
        mCurPoiWifi.updateCurPoi(mCurPoiTuple.getWifiSsid());
    }

    /**
     * update the cell set of poi with current bouncing cells.
     */
    private boolean updatePoiCellsWithCurrentBouncingCells(boolean forceSync) {
        // only use the current cell info, not the entire bouncing cells!! why ?
        //String curcelljson = mTelMon.getValueJSONString();
        String bouncingcelljsons = Utils.convertSetToString(mTelMon.getBouncingCells());
        return updatePoiCellJsons(bouncingcelljsons, forceSync);
    }

    /**
     * merge update all celljsons for this poi, if any
     * @return if any merge has done, false otherwise
     */
    private boolean updatePoiCellJsons(String celljsons, boolean forceSync) {
        // only merge update whenever in a POI
        if (mCurPoiTuple != null) {
            String curPoiCells = mCurPoiTuple.getCellJson();
            String mergedCells = Utils.mergeSetStrings(curPoiCells, celljsons);
            if (mergedCells != null) {
                mCurPoiTuple.setCellJson(mergedCells);
//IKHSS7-15504:wtnp74:we do not need to update location name here--BEGIN
                mPoiAdapter.updatePoi(mCurPoiTuple,true);
//IKHSS7-15504:wtnp74:we do not need to update location name here--END
                if(LOG_DEBUG) Log.d(TAG, "updatePoiCellJsons :: POI : " + mCurPoiTuple.getPoiName() + " celljsons:: " + mergedCells);
                return true;
            } else if (forceSync) {
//IKHSS7-15504:wtnp74:we do not need to update location name here--BEGIN
                mPoiAdapter.updatePoi(mCurPoiTuple,true);
//IKHSS7-15504:wtnp74:we do not need to update location name here--END
                if(LOG_DEBUG) Log.d(TAG, "updatePoiCellJsons :: force sync by wifi update. POI : " + mCurPoiTuple.getPoiName());
                return true;
            }
        }
        return false;
    }

    /**
     * update the poi's wifi set after location discovery.
     * @param matchedpoi
     * @param wifimacs  use the passed in wifi list from lsman exact at the time of discovery.
     * wifissid=[{"wifibssid":"00:14:6c:14:ec:fa","wifissid":"PInternet"}, {}, ...]
     */
    public void updatePoiBeaconAfterDiscovery(String matchedpoi, String wifissids) {
        if (mCurPoiTuple != null) {
            if(LOG_DEBUG) Log.d(TAG, "updatePoiBeaconAfterDiscovery: discovered poi:" + matchedpoi + " CurPOI:" + mCurPoiTuple.toString());

            mCurPoiTuple.setWifiSsid((JSONUtils.mergeJsonArrayStrings(mCurPoiTuple.getWifiSsid(), wifissids)));
            mCurPoiWifi.updateCurPoi(mCurPoiTuple.getWifiSsid());  // update the ssid bag also.
            // XXX no update of POI's lat/lng/radius.
            //LocationDatabase.LocTimeTable.Tuple bestlocpoi = mLSMan.mStore.findMostAccuracyPoiTuple(mCurPoiTuple.getPoiName());
            mPoiAdapter.updatePoi(mCurPoiTuple);
        }
    }

    /**
     * Wifi scan periodically, so this callback func will be run async periodically.
     * if already in POI, check left if none beacon matches.
     * if not in POI, checck  POI match
     */
    public void processBeaconScanResult() {
        mDetCtrl.detectPoiUponBeaconScanResult();
        // On every wifi scan, request a cell location update; when we shutdown NLP, cell update also disappeared.
        //CellLocation.requestLocationUpdate();
        notifyVSMUponRestart();  // notify vsm when check beacon done, if we have not notified before.
    }


    /**
     * got Wifi conn state changed, if inside a POI, update POI connected Wifi info.
     * Note, only update connected wifi only when scanned wifi has seen then connected wifi, to avoid issues with stale cur poi.
     *
     * if POI not asserted, lookup all poi info and assert POI if there is a match.
     * Wifi connected will always stop location request because run detection will stop location request.
     * If we assert POI here with msg ENTER_POI, run detection will be called after entering poi, which will stop location req.
     * wifissid=[{"wifibssid":"00:14:6c:14:ec:fa","wifissid":"PInternet"}, {}, ...]
     *
     * Note: if cur poi disagree with the poi from connected wifi, assert POI from connected wifi for now.
     */
    private void handleWifiConnStateChanged(String bssid) {
        if (bssid != null) {  // valid bssid, wifi connected !
            Tuple poi = getMatchingPoiByConnWifi(bssid);
            // case 1, this connected wifi was not associated with any POI. this is a new connected wifi.
            if (null == poi) {
                // now if we are current already inside a poi, update this information into poi table.
                if (mCurPoiTuple != null) {
                    String scannedwifi = mCurPoiTuple.getWifiSsid();
                    if (scannedwifi != null && scannedwifi.indexOf(bssid) >= 0) {  // promote to connected wifi only from scanned wifi pool.
                        String dbWifis = mCurPoiTuple.getWifiConnMac();
                        if (dbWifis != null && dbWifis.length() > 0) {
                            dbWifis += Utils.DELIM + bssid;
                        } else {
                            dbWifis = bssid;
                        }
                        mCurPoiTuple.setWifiConnMac(dbWifis);
                        mPoiAdapter.updatePoi(mCurPoiTuple);
                        if(LOG_DEBUG) Log.d(TAG, "handleWifiConnStateChanged: update bssid into cur poi" + mCurPoiTuple.toString());
                    }  // end of check scanned wifi
                    // we are not inside any poi, do nothing for this connected wifi.
                } else {
                    if(LOG_DEBUG) Log.d(TAG, "handleWifiConnStateChanged: conn happened outside any poi, do nothing");
                    
                    // Assumption: Physically user is arrived at POI
                    // Device connected to wifi before program could detect that
                    // user arrived in POI (via wifi scan / cell tower change)
                    startScan();
                }
                // case 2, this connected wifi was associated with some POI.
            } else {
                // I am not inside any POI for now, assert POI based on wifi connection info.
                if (mCurPoiTuple == null) {
                    // XXX for mobile station, compare poi scan list to current scan list, assert POI only when at least more than 2 matches.
                    if (Utils.intersectSetJsonArray(mBeacon.getLastWifiScanSsidMap().keySet(), null, poi.getWifiSsid()) >= FUZZY_MATCH_MIN) {
                        if(LOG_DEBUG) Log.d(TAG, "handleWifiConnStateChanged: poi found from db: assert POI as conned wifi is not mobile station" + poi.toString());
                        sendPoiChangedMessage(poi);
                        return;  // return, run detection will be called inside entering Poi.
                    } else {
                        if(LOG_DEBUG) Log.d(TAG, "handleWifiConnStateChanged: poi found from db:" + poi.toString() + " do not assert POI as conned wifi is mobile station");
                    }
                } else if (mCurPoiTuple != null && mCurPoiTuple.getPoiName().equals(poi.getPoiName())) {
                    if(LOG_DEBUG) Log.d(TAG, "handleWifiConnStateChanged: poi found from db = current poi: " + poi.toString() + " current poi:"+ mCurPoiTuple.toString());
                } else {
                    // XXX if cur poi not agree with poi from connected wifi, which poi should we take ?
                    // Per venki, can not take wifi conn's due to mobile station issue....always take poi from lat/lng distance.
                    if(LOG_DEBUG) Log.d(TAG, "handleWifiConnStateChanged: poi found from db: not agree with cur poi :" + poi.toString() + " not agree with cur poi:"+ mCurPoiTuple);
                }
            }
            // this is wifi disconnected when got null as paramaters
        } else {
            if(LOG_DEBUG) Log.d(TAG, "handleWifiConnStateChanged: wifi disconnected !!!");
        }
        // now wifi connect state changed, adjust detection state accordingly by stopping location request inside run detection.
        mDetCtrl.runDetection(" Run Detection Upon wifi conn state changed");
    }

    /**
     * upon POI table refresh, if there is an POI without cell info, that means the poi is newly tagged POI.
     * If the last know location is the same as this tagged POI, means user just tagged current location as POI.
     * Assert the entering of POI immediately !!!
     */
    private void onPoiRefresh() {
        boolean foundPoi = false;
        Location curloc;
        double lat, lgt, accuracy;

        List<Tuple> poilist = mPoiAdapter.getPoiList();
        for (Tuple entry : poilist ) {
            if ( entry.getCellJson() == null &&    // this is a newly manually tagged poi. no cell info
                    (null != (curloc = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)))) {
                lat = curloc.getLatitude();
                lgt = curloc.getLongitude();
                accuracy = curloc.getAccuracy();
                if(LOG_DEBUG) Log.d(TAG, "onPoiRefresh: lastknownlocation:" + curloc.toString());

                if ((!Utils.compareDouble(lat, entry.getLat()) && !Utils.compareDouble(lgt, entry.getLgt())) ||
                        Utils.distanceTo(curloc.getLatitude(), curloc.getLongitude(), entry.getLat(), entry.getLgt()) <= Math.max(LOCATION_DETECTING_DIST_RADIUS, accuracy)) {
                    if(LOG_DEBUG) Log.d(TAG, "onPoiRefresh: LastKnownLocation matches POI:" + entry.toString());
                    // only looking to entering POI when currently not in a POI, or current diff than found. 
                    // Otherwise, if getCellJson is null, repeative. entering, update, onPoiRefresh.
                    // IKHSS6UPGR-10933, IKHSS6UPGR-11099, IKHSS7-34625
                    if( mCurPoiTuple == null || ! mCurPoiTuple.getPoiName().equals(entry.getPoiName())){
                        foundPoi = true;
                        mBeacon.startBeaconScan(null, mHandler);
                        updateDetectionState(entry);
                    }
                    break;
                }
            }

            // if anything of cur poi has changed, update cur poi object, leave cur poi if needed. IKSTABLE6-4701
            // note, we are relying on field poi for object equality check.
            if( mCurPoiTuple != null && mCurPoiTuple.getPoiName().equals(entry.getPoiName()) &&
                    Utils.compareDouble(mCurPoiTuple.getLat(), entry.getLat()) && Utils.compareDouble(mCurPoiTuple.getLgt(), entry.getLgt()) &&
                    Utils.distanceTo(mCurPoiTuple.getLat(), mCurPoiTuple.getLgt(), entry.getLat(), entry.getLgt()) > Math.max(LOCATION_DETECTING_DIST_RADIUS, mCurPoiTuple.getRadius())
              ) {
                if(LOG_DEBUG) Log.d(TAG, "onPoiRefresh: Current POI changed...leaving POI" + entry.toString());
                leavingPOI();
                break;
            }
        }

        // if currently in a poi, and consent got revoked, leave poi immediately
        if ( mCurPoiTuple != null && ! mLSMan.isUserConsentOnMotoLocation()) {
            if(LOG_DEBUG) Log.d(TAG, "onPoiRefresh: Current in POI and user revoke consent, leaving POI");
            leavingPOI();
            return;
        }

        if (!foundPoi) {
            mDetCtrl.runDetection("Run detection upon POI_REFRESH...");
        }
    }

    /**
     * return whether there is any POIs needed for detection
     * @return false if poi adapter list size is 0, true otherwise
     */
    public boolean hasPoiAndLocationRules() {
        if (mPoiAdapter.getPoiList().size() == 0) {
            if(LOG_DEBUG) Log.d(TAG, "hasPoi:...no POIs !");
            return false;
        }

        if (!hasLocationRules()) {
            if(LOG_DEBUG) Log.d(TAG, "no location based rules !");
            return false;
        }

        // check whether user consent on motorola consent.
        if ( ! mLSMan.isUserConsentOnMotoLocation() ) {
            if(LOG_DEBUG) Log.d(TAG, "user did not consent on moto location policy !");
            return false;
        }

        return true;
    }

    /**
     * listen to check whether there is location base rules
     */
    public boolean hasLocationRules() {
        return isStateFlagSet(DetectionState.HAS_LOCATION_RULE.getFlag()) ||
                isStateFlagSet(DetectionState.HAS_HIDDEN_LOCATION_RULE.getFlag());
    }

    /**
     * init location rule bcast recver
     */
    private void initLocationRuleReceiver() {
        mLocationRuleRecv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onLocationRuleIntent(intent);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(LOCATION_RULE);
        filter.addAction(HIDDEN_LOCATION_RULE);
        mContext.registerReceiver(mLocationRuleRecv, filter);

        if ( hasLocationRulesInPref() ) {
            setStateFlag(DetectionState.HAS_LOCATION_RULE.getFlag(), true);
        } else {
            setStateFlag(DetectionState.HAS_LOCATION_RULE.getFlag(), false);
        }

        if ( hasHiddenLocationRulesInPref() ) {
            setStateFlag(DetectionState.HAS_HIDDEN_LOCATION_RULE.getFlag(), true);
        } else {
            setStateFlag(DetectionState.HAS_HIDDEN_LOCATION_RULE.getFlag(), false);
        }
    }
    /**
     * read once loation rule pref inside constructor
     */
    private boolean hasLocationRulesInPref() {
        String hasRules = mLSMan.mLSApp.mAppPref.getString(AppPreferences.HAS_LOCATION_RULE);
        if ( hasRules == null || "0".equals(hasRules)) {
            return false;
        }
        return true;
    }

    /**
     * read once loation rule pref inside constructor
     */
    private boolean hasHiddenLocationRulesInPref() {
        String hasRules = mLSMan.mLSApp.mAppPref.getString(AppPreferences.HAS_HIDDEN_LOCATION_RULE);
        if ( hasRules == null || "0".equals(hasRules)) {
            return false;
        }
        return true;
    }

    /**
     * handle of loation base rule start/stop from smartprofile.
     */
    private void onLocationRuleIntent(Intent intent) {
        String PUBLISH_EXTRA = "com.motorola.contextual"+".PUBLISH";

        if ( LOCATION_RULE.equals(intent.getAction())) {
            if("start".equals(intent.getStringExtra(PUBLISH_EXTRA)) ) {
                if(LOG_DEBUG) Log.d(TAG, "onLocationRuleIntent : LOCATION_RULE : start");
                setStateFlag(DetectionState.HAS_LOCATION_RULE.getFlag(), true);
                mLSMan.mLSApp.mAppPref.setString(AppPreferences.HAS_LOCATION_RULE, "1");
            } else if("stop".equals(intent.getStringExtra(PUBLISH_EXTRA))) {
                if(LOG_DEBUG) Log.d(TAG, "onLocationRuleIntent : LOCATION_RULE : stop");
                setStateFlag(DetectionState.HAS_LOCATION_RULE.getFlag(), false);
                mLSMan.mLSApp.mAppPref.setString(AppPreferences.HAS_LOCATION_RULE, "0");
            } else {
                if(LOG_DEBUG) Log.d(TAG, "onLocationRuleIntent : do not care:");
            }
        } else if ( HIDDEN_LOCATION_RULE.equals(intent.getAction())) {
            if("start".equals(intent.getStringExtra(PUBLISH_EXTRA)) ) {
                if(LOG_DEBUG) Log.d(TAG, "onLocationRuleIntent : HIDDEN_LOCATION_RULE : start");
                setStateFlag(DetectionState.HAS_HIDDEN_LOCATION_RULE.getFlag(), true);
                mLSMan.mLSApp.mAppPref.setString(AppPreferences.HAS_HIDDEN_LOCATION_RULE, "1");
            } else if("stop".equals(intent.getStringExtra(PUBLISH_EXTRA))) {
                if(LOG_DEBUG) Log.d(TAG, "onLocationRuleIntent : HIDDEN_LOCATION_RULE : stop");
                setStateFlag(DetectionState.HAS_HIDDEN_LOCATION_RULE.getFlag(), false);
                mLSMan.mLSApp.mAppPref.setString(AppPreferences.HAS_HIDDEN_LOCATION_RULE, "0");
            }
        }
    }

    /**
     * set the current detection status with the poi tuple. If poituple is null, means leaving current meaningful location.
     * @param poituple
     */
    private void  updateDetectionState(final Tuple poituple) {
        if (poituple != null) {
            enteringPOI(poituple);
            mDetCtrl.runDetection(" Run detection upon updateDetectionState="+poituple.getPoiName());
        } else if (mCurPoiTuple != null) {
            // run thru full path of loc fix, distance check, and wifi check before really leaving.
            mDetCtrl.runDetection(" Run detection upon discovery telling us to leave POI="+mCurPoiTuple.getPoiName());
        }
    }

    /**
     * post a entering or leaving message to self so that changing of global state got sequentialized to avoid race condition.
     * @param poituple
     */
    private void sendPoiChangedMessage(final Tuple poituple) {
        Message msg = mHandler.obtainMessage();
        if (poituple != null) {
            msg.what = Msg.ENTERING_POI;
        } else {
            msg.what = Msg.LEAVING_POI;
        }
        msg.obj = poituple;
        mHandler.sendMessage(msg);
    }

    /**
     * just request a periodical scan result upon periodical timer intent.
     */
    private void startScan() {
        mBeacon.startBeaconScan(null, mHandler);
        mDetCtrl.runDetection(" upon scan timer expired");  // always run through detection logic
    }

    /**
     * looping thru all the POIs and add alert
     */
    @Deprecated
    private void addProximityAlerts() {
        if (false) { // comment out until battery test find out whether this drain battery.
            Intent alertintent = new Intent(LOCATION_DETECTION_POI);
            mProximityAlertIntent = PendingIntent.getBroadcast(mContext, 0, alertintent,  PendingIntent.FLAG_UPDATE_CURRENT);
            if(LOG_DEBUG) Log.d(TAG, "addProximityAlerts : setting with intent :" + alertintent.toString());

            for (Tuple entry : mPoiAdapter.getPoiList() ) {
                mLocationManager.addProximityAlert(entry.getLat(), entry.getLgt(), MONITOR_RADIUS, -1, mProximityAlertIntent);
                if(LOG_DEBUG) Log.d(TAG, "addProximityAlerts :: " + entry.getPoiName() + "  :: lat:lgt:=" + entry.getLat()+ ":"+entry.getLgt());
            }
        }
    }

    // Utilize location manager Proximity Alert from android directly
    final static private class ProximityAlertReceiver extends BroadcastReceiver {
        private final Context mContext;
        private final Handler mHandler;
        private boolean mStarted = false;

        public ProximityAlertReceiver(final Context ctx, final Handler handler) {
            this.mContext = ctx;
            this.mHandler = handler;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Message msg = mHandler.obtainMessage();
            if(LOG_DEBUG) Log.d(TAG, "ProximityAlertReceiver : onReceiver :" + action);

            if (LOCATION_DETECTION_POI.equals(action)) {
                msg.what = Msg.PROXIMITY_ALERT;
                mHandler.sendMessage(msg);
            }
        }

        synchronized final void start() {
            if (!mStarted) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(LOCATION_DETECTION_POI);
                mContext.registerReceiver(this, filter);
                mStarted = true;
                if(LOG_DEBUG) Log.d(TAG, "ProximityAlertReceiver : started and registered");
            }
        }

        synchronized final void stop() {
            if (mStarted) {
                mContext.unregisterReceiver(this);
                mStarted = false;
                if(LOG_DEBUG) Log.d(TAG, "ProximityAlertReceiver : stoped and unregistered");
            }
        }
    }

    /**
     * handle location fix available pending intent. Not used for now!
     */
    @Deprecated
    private void handleLocationFixFromPendingIntent(Location loc) {
        // just take a log for now
        if(LOG_DEBUG) Log.d(TAG, "handleLocationFixFromPendingIntent: onLocationChanged() :" + loc.toString());
    }

    /**
     *  The Bcast receiver for requesting location update thru pendingIntent
     */
    @Deprecated
    final static private class LocationUpdatePendingIntentReceiver extends BroadcastReceiver {
        private final Context mContext;
        private final Handler mHandler;
        private boolean mStarted = false;

        public LocationUpdatePendingIntentReceiver(final Context ctx, final Handler handler) {
            this.mContext = ctx;
            this.mHandler = handler;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Message msg = mHandler.obtainMessage();
            if(LOG_DEBUG) Log.d(TAG, "LocationUpdateReceiver : onReceiver :" + action);

            if (LOCATION_UPDATE_AVAILABLE.equals(action)) {
                if (intent.getExtras() != null) {
                    Location loc = (Location)intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
                    msg.what = Msg.LOCATION_PENDINGINTENT;
                    msg.obj = loc;
                    mHandler.sendMessage(msg);
                }
            }
        }

        synchronized final void start() {
            if (!mStarted) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(LOCATION_UPDATE_AVAILABLE);
                mContext.registerReceiver(this, filter);
                mStarted = true;
                if(LOG_DEBUG) Log.d(TAG, "LocationUpdateReceiver : started and registered");
            }
        }

        synchronized final void stop() {
            if (mStarted) {
                mContext.unregisterReceiver(this);
                mStarted = false;
                if(LOG_DEBUG) Log.d(TAG, "LocationUpdateReceiver : stoped and unregistered");
            }
        }
    }
}
