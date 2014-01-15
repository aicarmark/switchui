/*--------------------------------------------------------------------------------------------------
 *--------------------------------------------------------------------------------------------------
 *
 *                            Motorola Confidential Proprietary
 *                    Template ID and version: TMP_LFC_50069  Version 2.0
 *                     (c) Copyright Motorola 2009, All Rights Reserved
 *
 *
 * Revision History:
 *                             Modification     Tracking
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * Chris Gremo/g10535           01/01/2009                  Initial release
 * dfb746                       16/04/2012                  Modifying for used in system property project
 *
 * Portability: This module is platform independent
 */

package com.motorola.mmsp.performancemaster.engine;

public class Log {
    private Log() {
    }

    private static final boolean DEBUG = true;
    private static final int MAX_MSG_SIZE = 1024;
    private static final String APP_MGR_TAG = "MMSP_SYS_PROP";

    public static void v(String tag, String message) {
        if (DEBUG) {
            if (android.util.Log.isLoggable(tag, android.util.Log.VERBOSE))
                android.util.Log.v(APP_MGR_TAG, createMessage(tag, message));
        }
    }

    public static void d(String tag, String message) {
        if (DEBUG) {
            if (android.util.Log.isLoggable(tag, android.util.Log.DEBUG))
                android.util.Log.d(APP_MGR_TAG, createMessage(tag, message));
        }
    }

    public static void i(String tag, String message) {
        if (android.util.Log.isLoggable(tag, android.util.Log.INFO))
            android.util.Log.i(APP_MGR_TAG, createMessage(tag, message));
    }

    public static void w(String tag, String message) {
        if (android.util.Log.isLoggable(tag, android.util.Log.WARN))
            android.util.Log.w(APP_MGR_TAG, createMessage(tag, message));
    }

    public static void e(String tag, String message) {
        if (android.util.Log.isLoggable(tag, android.util.Log.ERROR))
            android.util.Log.e(APP_MGR_TAG, createMessage(tag, message));
    }

    static String createMessage(String tag, String message) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(tag).append('\t').append(message);
        return (sb.length() < MAX_MSG_SIZE) ? sb.toString() : sb.substring(0,
                MAX_MSG_SIZE);
    }
}
