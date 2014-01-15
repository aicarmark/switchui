/*
 * Copyright (C) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package motorola.app.admin;

import android.app.admin.DevicePolicyManager;
import android.util.Log;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.content.Context;


/**
 * Extended public interface for managing policies enforced on a device.  Most clients
 * of this class must have published a {@link DeviceAdminReceiver} that the user
 * has currently enabled.
 */
/**
 * IKMAIN-6027 Separate EDM SDK APIs from framework library, the changes below is to
 * block old 3rd party client to provison phone.
 */
public class DevicePolicyManagerExt {

    private static String TAG = "DevicePolicyManagerExt";
    private static String EDM_VERSION = "1.4.0";
    private boolean edm_feature_enable = false;

    public DevicePolicyManagerExt() {
        Log.d(TAG, "MOT EDM API - DevicePolicyManagerExt");
    }

    public void getActiveSyncDeviceID(DevicePolicyManager dpm) {

        Log.d(TAG, "MOT EDM API - getActiveSyncID");
    }

    /**
    * This method is used to retrieve the EDM Version
    *
    * @param
    */
    public String getEdmVersion() {
        if (edm_feature_enable) {
            return EDM_VERSION;
        } else {
            return null;
        }
    }
}
