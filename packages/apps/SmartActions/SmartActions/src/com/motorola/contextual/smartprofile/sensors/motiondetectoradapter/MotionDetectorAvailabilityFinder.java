/*
 * @(#)MotionDetectorAvailabilityFinder.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/04/23  NA                  Initial version of MotionDetectorAvailabilityFinder
 *
 */

package com.motorola.contextual.smartprofile.sensors.motiondetectoradapter;

import com.motorola.contextual.smartprofile.Constants;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

/**
 * Receiver to be invoked whenever an availability check for Motion Detection
 * needs to be run
 *
 * <code><pre>
 * CLASS:
 *     Extends BroadcastReceiver
 *     Implements Constants
 *
 * RESPONSIBILITIES:
 *    Checks for the availability of Motion Detection and disables Motion Detection Adapter
 *    if Motion Detection is not found
 *
 * COLABORATORS:
 *     Smart Actions Framework
 *
 * USAGE:
 *      See individual methods.
 *
 * </pre></code>
 **/

public class MotionDetectorAvailabilityFinder  implements Constants {

    private static final String TAG =   MotionDetectorAvailabilityFinder.class.getSimpleName();
    private static final String MOTION_PACKAGE = "com.motorola.contextual.Motion";

    /**
     * Method to check if Motion Detection is present on the device or not
     * and disable Motion Detection Adapter in case FW is not found
     *
     * @param context Caller's context
     */
    public static void checkAndDisableMDAdapter (Context context) {
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(new ComponentName(context, MotionDetectorAdapterActivity.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        try {
            pm.getPackageInfo(MOTION_PACKAGE, PackageManager.GET_ACTIVITIES);
        } catch (NameNotFoundException e) {
            Log.w(TAG, "MD not present on the device");
            pm.setComponentEnabledSetting(new ComponentName(context, MotionDetectorAdapterActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                          PackageManager.DONT_KILL_APP);
            return;
        }
        if(MotionDetectorAdapterDetailComposer.isNewArchMD(context)) {
            Log.w(TAG, "MD new arch - no need of adapter");
            pm.setComponentEnabledSetting(new ComponentName(context, MotionDetectorAdapterActivity.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                          PackageManager.DONT_KILL_APP);
        }
    }
}
