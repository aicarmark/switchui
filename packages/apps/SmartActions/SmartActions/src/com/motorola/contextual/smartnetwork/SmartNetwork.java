/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 * w04917 (Brian Lee)        2012/08/15   IKCTXTAW-539    Disable SmartNetwork
 */

package com.motorola.contextual.smartnetwork;

public class SmartNetwork {
    /*
    private static final String BUILD_TYPE_ENG = "eng";
    private static final String BUILD_TYPE_USER_DEBUG = "userdebug";
    private static final String BUILD_TYPE_USER = "user";
    private static final String TAG_TYPE_TEST = "test-keys";
    private static final String TAG_TYPE_RELEASE = "release-keys";
     */

    private static final boolean sEnabled;

    static {
        sEnabled = false;
        /*
        // only enable it on engineering, user-debug, and test builds
        if (Build.TYPE.equals(BUILD_TYPE_ENG) || Build.TYPE.equals(BUILD_TYPE_USER_DEBUG)
                || Build.TAGS.equals(TAG_TYPE_TEST)) {
            sEnabled = true;
        } else {
            sEnabled = false;
        }
        */
    }

    /**
     * @return true if SmartNetwork is enabled, false otherwise
     */
    public static boolean isEnabled() {
        return sEnabled;
    }
}
