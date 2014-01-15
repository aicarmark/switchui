/*
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2010/08/27 IKCTXTAW-19		   Initial version
 * w04917        2012/02/17 IKHSS7-8262       Use the new Checkin API.
 */
package com.motorola.contextual.virtualsensor.locationsensor;

import static com.motorola.contextual.virtualsensor.locationsensor.Constants.*;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
/**
 *<code><pre>
 * CLASS:
 *  Our application class, Getting init before anything started.
 *
 * RESPONSIBILITIES:
 *
 * COLABORATORS:
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */
public class LocationSensorApp {

    private static final String TAG = "LSApp_App";

    private String mVersion = null;
    Context mContext;

    //private RemoteResourceManager mRemoteResourceManager;
    AppPreferences mAppPref;

    private LocationSensorManager mLSMan = null;  // available after location sensor manager service started.
    //private NoCellLocationManager mLSMan = null;  // available after location sensor manager service started.
    private VsmProxy mVSMProxy = null;
    //public LocationGraph mGraph;

    public LocationSensorApp(Context ctx) {
        mContext = ctx;
        //mVersion = Utils.getVersionString(mContext, PACKAGE_NAME);  // ls not a standalone pkg anymore

        // Setup Prefs
        mAppPref = new AppPreferences(this);

        mVSMProxy = new VsmProxy(this);

        // XXX for location graph
        // mGraph = new LocationGraph(this);
        // mGraph.buildGraphIfEmpty();

        LSAppLog.d(TAG, "LSAPP constructor");
    }

    public void setLSMan(LocationSensorManager lsman) {
        mLSMan = lsman;
        mVSMProxy.start(); // notify upper layer client location app is ready!
        LSAppLog.d(TAG, "setLSMan : LSMan ready....spread out!!!");
    }

    /**
     * return the reference to location manager
     * @return
     */
    public LocationSensorManager getLSMan() {
        return mLSMan;
    }

    /**
     * get the reference to VSM proxy
     */
    public VsmProxy getVsmProxy() {
        return mVSMProxy;
    }

    /**
     * out logger
     * @author e51141
     *
     */
    public static class LSAppLog {
        public static void i(String tag, String msg) {
            Log.i(tag, msg);
        }
        public static void e(String tag, String msg) {
            Log.e(tag, msg);
        }
        public static void d(String tag, String msg) {
            if (LOG_VERBOSE) Log.d(tag, msg);
        }
        public static void pd(String tag, String msg) {
            Log.d(tag, msg);  // always platform debug logging
        }
        public static void dbg(Context ctx, String direction, String... msg) {
            //if(!LOG_VERBOSE) return;

            ContentValues values = new ContentValues();
            StringBuilder sb = new StringBuilder();

            if (direction.equals(DEBUG_OUT)) {
                values.put(DEBUG_STATE, msg[0]);
            }

            for (String s : msg) {
                sb.append(s);
                sb.append(" : ");
            }

            values.put(DEBUG_COMPKEY, VSM_PKGNAME);
            values.put(DEBUG_COMPINSTKEY,PACKAGE_NAME);
            values.put(DEBUG_DIRECTION, direction);
            values.put(DEBUG_DATA, sb.toString());
            if (LOG_VERBOSE) Log.d("LSAPP_DBG", sb.toString());
            try {
                ctx.getContentResolver().insert(DEBUG_DATA_URI, values);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    /**
     * @return current app version
     */
    public String getVersion() {
        if (mVersion != null) {
            return mVersion;
        } else {
            return "";
        }
    }

    @Deprecated
    /**
     * start location manage
     */
    public void startLocationManager() {
        Intent myIntent = new Intent(mContext, LocationSensorManager.class);
        //Intent myIntent = new Intent(this, NoCellLocationManager.class);
        myIntent.putExtra(INTENT_PARM_STARTED_FROM_BOOT, false);
        ComponentName component = mContext.startService(myIntent);
        if (component != null) {
            LSAppLog.d(TAG, "Location Sensor Services started: " + component.toShortString());
        } else {
            LSAppLog.d(TAG, "Location Sensor Services start failed.");
        }
    }

    public static void sendMessage(Handler hdl, int what, Object obj, Bundle data) {
        LSAppLog.e(TAG, "Sending Message to " + hdl + ": msg :" + what);
        Message msg = hdl.obtainMessage();
        msg.what = what;
        if (obj != null)
            msg.obj = obj;
        if (data != null)
            msg.setData(data);
        hdl.sendMessage(msg);
    }
}
