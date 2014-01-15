/*
 * @(#)SharedPrefWrapper.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21034        2012/08/03  NA                  Initial version
 *
 */
package com.motorola.contextual.smartrules.psf.mediator;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * This class provides methods related to saving data using shared preferences.
 *  <code><pre>
 * CLASS:
 *
 *
 * RESPONSIBILITIES:
 *    Provides methods to save/retrieve a boolean from shared preferences
 *
 * COLLABORATORS:
 *     SharedPreferences Android class
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */


public final class SharedPrefWrapper {

    private static final String PACKAGE = SharedPrefWrapper.class.getPackage().getName();

    /** Commits a string value to persistent storage
     * @param context
     * @param key
     * @param value
     */
    public static void commitBooleanValue(Context context, String key, boolean value) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.apply();

    }

    /** Retrieves a String value from persistent storage
     * @param context
     * @param key
     * @return
     */
    public static boolean retrieveBooleanValue(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        return preferences.getBoolean(key, false);
    }

}
