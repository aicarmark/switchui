/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date         CR Number       Brief Description
 * -------------------------   ----------   -------------   ------------------------------
 * w04917 (Brian Lee)          2012/02/07   IKCTXTAW-359    Initial version
 *
 */

package com.motorola.datacollection.perfstats;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * @author w04917 (Brian Lee)
 * Writes performance settings to syncable settings.
 */
public class PerformanceStatsSettingsWriter extends ContentProvider {
    private static final String TAG = "PerfStatsSettingWR";
    /* syncable settings provider */
    private static final Uri SETTINGS_URI =
        Uri.parse("content://com.motorola.blur.setupprovider/app_settings");
    private static final String COL_NAME = "name";
    private static final String COL_VALUE = "value";

    static final String SETTING_KEY = "key";
    static final String SETTING_VALUE = "value";

    /* call method identifiers */
    static final String CALL_SET_SINGLE_SETTING = "SET_SINGLE_SETTING";
    static final String CALL_SET_BULK_SETTINGS = "SET_BULK_SETTINGS";
    static final String CALL_DELETE_SETTINGS = "DELETE_SETTINGS";

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        /* not used */
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        /* not used */
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /* not used */
        return null;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /* not used */
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        /* not used */
        return 0;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "call method: " + method + ", arg: " + arg);
        }

        Bundle bundle = null;
        if (CALL_SET_SINGLE_SETTING.equals(method)) {
            setSingleSetting(extras);
        } else if (CALL_SET_BULK_SETTINGS.equals(method)) {
            setBulkSettings(extras);
        } else if (CALL_DELETE_SETTINGS.equals(method) && arg != null && !arg.isEmpty()) {
            deleteSettings(arg);
        }
        return bundle;
    }

    private void deleteSettings(String basicKey) {
        if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "clearSettings() key: " + formatKey(basicKey));
        }

        if (basicKey != null && !basicKey.isEmpty()) {
            String where = COL_NAME + " = ?";
            String[] selectionArgs = { formatKey(basicKey) };
            getContext().getContentResolver().delete(SETTINGS_URI, where, selectionArgs);
        }
    }

    private void setSingleSetting(Bundle extras) {
        if (extras != null) {
            String key = extras.getString(SETTING_KEY);
            String value = extras.getString(SETTING_VALUE);
            if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
                key = formatKey(key);
                if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "setSettings() key: " + key + ", value: " + value);
                }
                ContentValues cv = new ContentValues();
                cv.put(COL_NAME, key);
                cv.put(COL_VALUE, value);
                //syncable settings already takes care of duplicates by using replace for APP_SETTINGS
                getContext().getContentResolver().insert(SETTINGS_URI, cv);
            }
        }
    }

    private void setBulkSettings(Bundle extras) {
        if (extras != null) {
            String[] keys = extras.getStringArray(SETTING_KEY);
            String[] values = extras.getStringArray(SETTING_VALUE);
            if (keys != null && keys.length > 0 && values != null && values.length > 0
                    && keys.length == values.length) {
                ArrayList<ContentValues> valuesList = new ArrayList<ContentValues>(keys.length);
                for (int i = 0; i < keys.length; i++) {
                    if (keys[i] != null && !keys[i].isEmpty() &&
                            values[i] != null && !values[i].isEmpty()) {
                        String key = formatKey(keys[i]);
                        ContentValues cv = new ContentValues();
                        cv.put(COL_NAME, key);
                        cv.put(COL_VALUE, values[i]);
                        valuesList.add(cv);
                        if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "setBulkSettings: " + key + ", " + values[i]);
                        }
                    }
                }
                if (!valuesList.isEmpty()) {
                    //syncable settings already takes care of duplicates by using replace for APP_SETTINGS
                    getContext().getContentResolver().bulkInsert(SETTINGS_URI,
                            valuesList.toArray(new ContentValues[valuesList.size()]));
                }
            }
        }
    }

    /**
     * @param basicKey key used for syncable settings db look up
     * @return basicKey prepended with perfstats header
     */
    private String formatKey(String basicKey) {
        return PerformanceStatsSettingsCache.formatKey(basicKey);
    }
}
