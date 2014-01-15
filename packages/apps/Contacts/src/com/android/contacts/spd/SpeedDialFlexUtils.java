/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.android.contacts.spd;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.SystemProperties;
import android.util.Xml;

/**
 * Parses and Reads flex file from /etc folder.
 *
 * MOT FID 35850-Flexing SDN's IKCBS-2075
 *
 * @author Jyothi Asapu - a22850
 */
public class SpeedDialFlexUtils {
    private static final String TAG = "SpeedDialFlexUtils";

    private static final String AP_FLEX_KEY = "ApFlexVersion";
    private static final String UNKNOWN = "UNKNOWN";
    public static SharedPreferences mSharedPreferences = null;
    private static final String SHARED_PREFS_FILE = "SpeedDialList";

    /**
     * Checks Flex version was modified or not.
     *
     * @param context
     * @return
     */
    public static boolean checkApFlexNotModified(Context context) {
        return getApFlexVerPreference(context)
                .equals(getApFlexVersion(context));
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        if (mSharedPreferences == null) {
            mSharedPreferences = context.getSharedPreferences(
                    SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        }

        return mSharedPreferences;
    }

    private static String getApFlexVerPreference(Context context) {
        return getSharedPreferences(context).getString(AP_FLEX_KEY, "null");
    }

    /**
     * Sets Flex Version to Shared Preferences.
     *
     * @param context
     * @param flexVer
     */
    public static void setApFlexVerPreference(Context context, String flexVer) {

        getSharedPreferences(context).edit().putString(AP_FLEX_KEY, flexVer)
                .apply();
    }

    /**
     * Gets Flex Version from system properties
     *
     * @param context
     * @param flexVer
     */
    public static String getApFlexVersion(Context context) {
        String pkgVersion = SystemProperties.get("ro.build.config.version",
                UNKNOWN);
        String appVersion = SystemProperties.get("ro.build.config.version.app",
                UNKNOWN);
        String mediaVersion = SystemProperties.get(
                "ro.build.config.version.media", UNKNOWN);

        String ret;
        if (pkgVersion.equals(UNKNOWN) && appVersion.equals(UNKNOWN)
                && mediaVersion.equals(UNKNOWN)) {
            ret = UNKNOWN;
        } else { // Filter "Unknown" item(s) from UI
            StringBuilder sb = new StringBuilder();
            if (!pkgVersion.equals(UNKNOWN)) {
                sb.append(pkgVersion);
            }

            if (!appVersion.equals(UNKNOWN)) {
                if (sb.length() != 0) {
                    sb.append("\n");
                }
                sb.append(appVersion);
            }

            if (!mediaVersion.equals(UNKNOWN)) {
                if (sb.length() != 0) {
                    sb.append("\n");
                }
                sb.append(mediaVersion);
            }

            ret = sb.toString();
        }

        return ret;
    }
}
