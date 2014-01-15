/*
 * @(#)Persistence.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/18  NA                  Initial version
 *
 */
package com.motorola.contextual.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;

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
 *     None
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */


public final class Persistence implements Constants {

    private static final String VALUE_SEPARATOR = "--";
    public static final String FW_PACKAGE = "com.motorola.contextual.fw";

    /** Commits a string value to persistent storage
     * @param context
     * @param key
     * @param value
     */
    public static void commitValue(Context context, String key, String value) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.apply();

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
        editor.apply();

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
        editor.apply();

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
        editor.apply();
    }

    /**
     * Commits a map of key-value pairs to persistence
     * @param context
     * @param key
     * @param value
     */
    public static void commitValues(Context context, HashMap<String, Integer> hashMap) {
        if (!hashMap.isEmpty()) {
            SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
            SharedPreferences.Editor editor = preferences.edit();
            Iterator<Entry<String, Integer>> iterator = hashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<String, Integer> entry = iterator.next();
                editor.putInt(entry.getKey(), entry.getValue());
            }
            editor.apply();
        }
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
        if (info != null) {
            String[] elements = info.split(VALUE_SEPARATOR);
            list = new ArrayList<String>();
            for (String e: elements)
                list.add(e);
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
            editor.apply();
            return info;
        }
        return null;
    }

    /** Removes a Boolean value from persistent storage
     * @param context
     * @param key
     * @return
     */
    public static Boolean removeBooleanValue(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        Boolean info = preferences.getBoolean(key, false);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(key);
        editor.apply();
        return info;
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
            editor.apply();
            return info.split(VALUE_SEPARATOR);
        }
        return null;
    }

    /**
     * Retrieves a boolean from Smart Actions framework persistence
     *
     * @param context
     * @param prefName
     * @param prefKey
     * @return Boolean value
     */
    public static boolean retrieveBooleanFromFramework (Context context, String prefName, String prefKey) {
        Context fwContext = null;
        boolean active = false;
        // Get the context of SmartActions FW package)
        try {
            fwContext = context.createPackageContext(FW_PACKAGE, 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        if (fwContext != null) {
            SharedPreferences preferences = fwContext.getSharedPreferences(
                    prefName, Context.MODE_WORLD_READABLE);
            
            active =  preferences.getBoolean(prefKey, false);
        }
        return active;
    }

    /**
     * A value committed to persistence which can be read by other apps
     * This is done to share values to Smart Actions framework
     * @param context Caller context
     * @param prefName Preference file name
     * @param prefKey Preference key
     * @param value Preference value
     */
    public static void commitSharedValue(Context context, String prefName, String prefKey, int value) {
        SharedPreferences preferences = context.getSharedPreferences(prefName, Context.MODE_WORLD_READABLE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(prefKey, value);
        editor.apply();
    }

    /**
     * Removes shared preferences that can be read by other apps
     * @param context Caller context
     * @param prefName Preference file name
     * @param prefKey Preference key
     * @return Removed value from persistence
     */
    public static int removeSharedValue(Context context, String prefName, String prefKey) {
        SharedPreferences preferences = context.getSharedPreferences(prefName, Context.MODE_WORLD_READABLE);
        int savedMode = -1;
        if (preferences.contains(prefKey)) {
            savedMode = preferences.getInt(prefKey, -1);
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(prefKey);
            editor.apply();
        }
        return savedMode;
    }
}
