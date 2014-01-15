/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2011/05/06   IKCTXTAW-272    Initial version
 *
 */

package com.motorola.meter;

import android.util.Log;

/** Follows the Motorola Android Logging Standards
 * @link https://sites.google.com/a/motorola.com/platform/development-1/guidelines-and-process/coding/androidlogging
 *
 * Also refer to Andriod Logging Standards
 * @link http://source.android.com/source/code-style.html#log-sparingly
 *
 * Log.isLoggable will return true/false depending on setprop values
 * adb shell setprop log.tag.<YOUR_TAG> VERBOSE/DEBUG/INFO/WARN/ERROR/SUPPRESS
 * example) adb shell setprop log.tag.Meter DEBUG
 *
 * @author w04917 (Brian Lee)
 */
public class Logger {
    /* android.util.Config.DEBUG will always be false in standard Android SDK builds,
     * but it will(should) be true in Platform debug builds. It will be calse on release builds.
     * Or you can manually turn it on and recompile.
     */
    private static final boolean LOCALD = android.util.Config.DEBUG || false;
    private static final boolean LOCALV = false;

    /** Verbose logs should never be checked-in to main-dev or stable builds
     * @param tag TAG
     * @param msg Message
     */
    public static final void v(String tag, String msg) {
        if (LOCALV && Log.isLoggable(tag, Log.VERBOSE)) {
            Log.v(tag, msg);
        }
    }

    /** Verbose logs should never be checked-in to main-dev or stable builds
     * @param tag TAG
     * @param msg Message
     * @param tr Throwable
     */
    public static final void v(String tag, String msg, Throwable tr) {
        if (LOCALV && Log.isLoggable(tag, Log.VERBOSE)) {
            Log.v(tag, msg, tr);
        }
    }

    /**
     * @param tag TAG
     * @param msg Message
     */
    public static final void d(String tag, String msg) {
        if (LOCALD && Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, msg);
        }
    }

    /**
     * @param tag TAG
     * @param msg Message
     * @param tr Throwable
     */
    public static final void d(String tag, String msg, Throwable tr) {
        if (LOCALD && Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, msg, tr);
        }
    }

    /**
     * @param tag TAG
     * @param msg Message
     */
    public static final void i(String tag, String msg) {
        Log.i(tag, msg);
    }

    /**
     * @param tag TAG
     * @param msg Message
     * @param tr Throwable
     */
    public static final void i(String tag, String msg, Throwable tr) {
        Log.i(tag, msg, tr);
    }

    /**
     * @param tag TAG
     * @param msg Message
     */
    public static final void w(String tag, String msg) {
        Log.w(tag, msg);
    }

    /**
     * @param tag TAG
     * @param msg Message
     * @param tr Throwable
     */
    public static final void w(String tag, String msg, Throwable tr) {
        Log.w(tag, msg, tr);
    }

    /**
     * @param tag TAG
     * @param msg Message
     */
    public static final void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    /**
     * @param tag TAG
     * @param msg Message
     * @param tr Throwable
     */
    public static final void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
    }
}
