/*
 * @(#)Persistence.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/16  NA                 Initial version
 *
 */
package com.motorola.contextual.smartprofile;

import java.util.ArrayList;
import java.util.List;

import com.motorola.contextual.smartprofile.Constants;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * This class provides methods related to saving data to Persistent Storage
 *  <code><pre>
 * CLASS:
 *
 *
 * RESPONSIBILITIES:
 *    Provides methods to save/retrieve/remove a string or array of strings
 *    to/from persistent storage
 *
 * COLLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */


public final class Persistence implements Constants {

    private static final String VALUE_SEPARATOR = "--";
    private static final String PACKAGE = (Constants.class.getPackage()!= null)? Constants.class.getPackage().getName():null;

    /** Commits a string value to persistent storage
     * @param context
     * @param key
     * @param value
     */
    public static void commitValue(Context context, String key, String value) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.commit();

    }

    /** Commits a boolean value to persistent storage
     * @param context
     * @param key
     * @param value
     */
    public static void commitValue(Context context, String key, boolean value) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.commit();

    }

    /** Commits an integer value to persistent storage
     * @param context
     * @param key
     * @param value
     */
    public static void commitValue(Context context, String key, int value) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, value);
        editor.commit();

    }

    /** Commits an array of string values to persistent storage
     * @param context
     * @param key
     * @param values
     */
    public static void commitValues(Context context, String key, String[] values) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        SharedPreferences.Editor editor = preferences.edit();
        StringBuilder sb = new StringBuilder(); // check String Builder code
        sb.append(values[0]);
        for (int i = 1; i < values.length; ++i) {
            sb.append(VALUE_SEPARATOR);
            sb.append(values[i]);
        }
        editor.putString(key, sb.toString());
        editor.commit();
    }

    /** Commits an array of string values to persistent storage
     * @param context
     * @param key
     * @param values
     */
    public static void commitValuesAsList(Context context, String key, List<String> values) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        SharedPreferences.Editor editor = preferences.edit();
        StringBuilder sb = new StringBuilder(); // check String Builder code
        if(!values.isEmpty()) {
            sb.append(values.get(0));
            int size = values.size();
            for (int i = 1; i < size; ++i) {
                sb.append(VALUE_SEPARATOR);
                sb.append(values.get(i));
            }
        }
        editor.putString(key, sb.toString());
        editor.commit();
    }

    /** Retrieves a String value from persistent storage
     * @param context
     * @param key
     * @return
     */
    public static String retrieveValue(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        return preferences.getString(key, null);
    }

    /** Retrieves a boolean value from persistent storage
     * @param context
     * @param key
     * @return
     */
    public static boolean retrieveBooleanValue(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        return preferences.getBoolean(key, false);
    }

    /** Retrieves an integer value from persistent storage
     * @param context
     * @param key
     * @return
     */
    public static int retrieveIntValue(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        return preferences.getInt(key, 0);
    }

    /** Retrieves an array of String values from persistent storage
     * @param context
     * @param key
     * @return
     */
    public static String[] retrieveValues(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        String info = preferences.getString(key, null);
        return (info != null) ? info.split(VALUE_SEPARATOR) : null;
    }

    /** Retrieves a list of String values from persistent storage
     * @param context
     * @param key
     * @return
     */
    public static List<String> retrieveValuesAsList(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        String info = preferences.getString(key, null);
        List<String> list = new ArrayList<String>();
        if(LOG_DEBUG) Log.d("Persistence", "retrieveValuesAsList : " +  info);
        if (info != null && !info.isEmpty()) {
            String[] elements = info.split(VALUE_SEPARATOR);
            list = new ArrayList<String>();
            if(elements.length != 0)  {
                for (String e: elements)
                    list.add(e);
            }
        } else {
            //return an empty list
        }
        return list;
    }

    /** Removes a String value from persistent storage
     * @param context
     * @param key
     * @return
     */
    public static String removeValue(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        String info = preferences.getString(key, null);
        if (info != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(key);
            editor.commit();
            return info;
        }
        return null;
    }

    /** Removes an array of String values from Persistent storage
     * @param context
     * @param key
     * @return
     */
    public static String[] removeValues(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        String info = preferences.getString(key, null);
        if (info != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(key);
            editor.commit();
            return info.split(VALUE_SEPARATOR);
        }
        return null;
    }
}
