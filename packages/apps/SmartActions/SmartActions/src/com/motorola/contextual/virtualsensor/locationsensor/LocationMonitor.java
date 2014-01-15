/*
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2010/08/27 IKCTXTAW-19		   Initial version
 */
package com.motorola.contextual.virtualsensor.locationsensor;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.*;

import android.app.PendingIntent;
import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;

import com.motorola.android.wrapper.SystemWrapper;
import com.motorola.android.wrapper.LocationManagerWrapper;
import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *<code><pre>
 * CLASS:
 *  Wrapper of all underlying location tracking tech into this location monitor
 *
 * RESPONSIBILITIES:
 *  Location tracking and reporting
 *
 * COLABORATORS:
 *	Telephony, GPS, WiFi
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */

public final class LocationMonitor {

    private static final String TAG = "LSAPP_LocMon";

    private Context mContext;
    private LocationSensorManager mLSMan;
    private AtomicInteger mLocationState;
    private Handler mCallerHandler;
    private LocationManagerWrapper mLocationManager;

    private LocationProvider mGpsProvider;
    private LocationProvider mNetworkProvider;

    private MyLocationListener mLocListener;
    private MyGpsStatusListener mGpsStatusListener;
    private AtomicInteger mGpsEnabled;

    //private GeoPoint mPoint;
    private Location mLoc = null;
    private Location mLastLoc = null;
    private Location mGpsLastLoc = null;

    // WakeLock
    private PowerManager.WakeLock sCpuWakeLock = null;

    @SuppressWarnings("unused")
    private LocationMonitor() {} // hide default constructor

    /**
     * Constructor called from Location Manager
     * @param ctx
     * @param hdl
     */
    public LocationMonitor(final LocationSensorManager lsman, final Handler hdl) {
        mContext = lsman;
        mLSMan = lsman;
        mLocationState = new AtomicInteger();
        mLocationState.set(0);
        mGpsEnabled = new AtomicInteger();   // default is 0

        mCallerHandler = hdl;
        mLocationManager = (LocationManagerWrapper)SystemWrapper.getSystemService(mContext, Context.LOCATION_SERVICE);
        mNetworkProvider = mLocationManager.getProvider(LocationManager.NETWORK_PROVIDER);
        mGpsProvider = mLocationManager.getProvider(LocationManager.GPS_PROVIDER);

        mLocListener = new MyLocationListener();
        mGpsStatusListener = new MyGpsStatusListener();
        mLocationManager.addGpsStatusListener(mGpsStatusListener);  // always on GPS status listener
    }

    /**
     * Destructor, clean up all the listeners.
     */
    public void cleanup() {
        stopLocationUpdate();
        mLocationManager.removeGpsStatusListener(mGpsStatusListener);
    }

    @SuppressWarnings("unused")
	@Deprecated
    private void getWakeLock() {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            sCpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            sCpuWakeLock.acquire();
        }
    }
    @SuppressWarnings("unused")
	@Deprecated
    private void releaseWakeLock() {
        if (sCpuWakeLock != null) {
            sCpuWakeLock.release();
            sCpuWakeLock = null;
        }
    }

    /**
     * Current location getter
     * @return Current Location
     */
    public Location getCurrentLocation() {
        if (mLoc == null) {
            //mLoc = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            LSAppLog.d(TAG, "mloc is null...Location fix not available yet...use last known location if have to:: ");
        }
        return mLoc;
    }

    /**
     * Getter of last location fix
     * @return
     */
    public Location getLastLocation() {
        return mLastLoc;
    }

    /**
     * @return true if network provider enabled, false otherwise
     */
    public boolean isNetworkProviderEnabled() {
        if (mNetworkProvider == null || false == mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            LSAppLog.d(TAG, "isNetworkProviderEnabled : NETWORK_PROVIDER not enabled, please enable it");
            return false;
        }
        return true;
    }

    /**
     * @return true if GPS provider enabled, false otherwise
     */
    public boolean isGpsProviderEnabled() {
        if (mGpsProvider == null || false == mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            LSAppLog.d(TAG, "isGpsProviderEnabled : GPS_PROVIDER not enabled, please enable it");
            return false;
        }
        LSAppLog.d(TAG, "isGpsProviderEnabled : GPS_PROVIDER enabled..");
        return true;
    }


    /**
     * Use location network provider. with GPS Flyweight.
     * @param minTime, the possible time notification can rest, required by google api.
     * @param minDistance, the delta before notification
     * @param intent, the pending intent to be fired once location fix available.
     * @return true of successfully started location tracking. false otherwise.
     */
    public boolean startLocationUpdate(long minTime, float minDistance, PendingIntent intent) {
        boolean ret = false;

        if (false == isNetworkProviderEnabled() && false == isGpsProviderEnabled()) {
            LSAppLog.d(TAG, "startLocationUpdate : NETWORK_PROVIDER and GPS not enabled, please enable it");
            return ret;
        }

        if (mLocationState.get() == 1) {  // bail out if we are already monitoring.
            LSAppLog.d(TAG, "startLocationUpdate : minTime:minDistance=" + minTime+":"+minDistance + ":: already in monitoring state...return quickly without reset current location");
            return ret;  // already started
        }
        mLocationState.set(1);

        /* Deprecated....not using GPS at all for now
        if (mGpsProvider != null && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(mGpsProvider.getName(),
                                                    LOCATION_UPDATE_INTERVAL_MILLIS,
                                                    LOCATION_UPDATE_MIN_DIST_METERS,
                                                    mLocListener
                                                    //mCallerHandler.getLooper()
                                                   );
            LSAppLog.i(TAG, "startLocationUpdate :: taking a measurement with GPS provider");
        }*/
        if (mNetworkProvider != null && mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            if (intent != null) {
                mLocationManager.requestLocationUpdates(mNetworkProvider.getName(),
                                                        minTime,
                                                        minDistance,
                                                        intent
                                                       );
            }

            mLocationManager.requestLocationUpdates(mNetworkProvider.getName(),
                                                    minTime,
                                                    minDistance,
                                                    mLocListener,
                                                    mCallerHandler.getLooper()
                                                   );
        }

        LSAppLog.pd(TAG, "startLocationUpdate : minTime:minDistance=" + minTime+":"+minDistance + ":: network provider enabled:" + mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        return true;
    }

    private void resetLocPerStartStop() {
        //reset the current loc info when we start
        mLastLoc = mLoc;
        mLoc = null;
        LSAppLog.d(TAG, "resetLocPerStartStop...mLoc is null now...waiting for async updated");
    }

    /**
     * stop location track and remove listener.
     */
    public void stopLocationUpdate() {
        mLocationState.set(0);    // reset state flag upon stopping.
        mLocationManager.removeUpdates(mLocListener);  // no harm if you remove a listener even though it has not been registered.
        LSAppLog.pd(TAG, "stopLocationUpdate :: stop request location update after removed listener...");
    }

    private class MyLocationListener implements LocationListener {
        public void onLocationChanged (Location location) {
            //getWakeLock();

            LSAppLog.pd(TAG, "onLocationChanged() :" + location.toString());
            if (mLSMan.isDataConnectionGood()) {
                mLastLoc = mLoc;
                mLoc = location;        // always use the latest measurement
                sendNotification(LOCATION_UPDATE);  // send to location detection. Global msg space.
            } else {
                LSAppLog.pd(TAG, "onLocationChanged() : NO Data Connection, Drop stale location udpate !");
            }

            //releaseWakeLock();
        }

        public void onProviderDisabled (String provider) {   }
        public void onProviderEnabled (String provider) { }
        public void onStatusChanged (String provider, int status, Bundle extras) { }
    }

    private class MyGpsStatusListener implements GpsStatus.Listener {
        // GPS enabled callback
        public void onGpsStatusChanged(int event) {
            switch (event) {
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                mGpsEnabled.set(1);
                // reset before setting new loc
                mLoc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);  // take the fix that is available
                LSAppLog.d(TAG, "onGpsStatusChanged: warmup upon First fix available: " + mLoc);
                // now fake a polling time expire to really get the, this should happen only once upon GPS turned on!
                // sendNotification(LocationSensorManager.Msg.START_MONITORING); // Ugly, that is why when gps enabled, we got many locations.
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
                mGpsEnabled.set(0);
                LSAppLog.d(TAG, "onGpsStatusChanged: Gps disabled");
                break;
            default:
                break;
            }
            return;
        }
    }

    private void sendNotification(int what) {
        LSAppLog.d(TAG, "Sending Message to caller..either LSMan or LSDet..by this instance's caller handler");
        Message msg = mCallerHandler.obtainMessage();
        msg.what = what;
        msg.obj = Long.valueOf(System.currentTimeMillis());
        //msg.setData(data);
        mCallerHandler.sendMessage(msg);
    }
}
