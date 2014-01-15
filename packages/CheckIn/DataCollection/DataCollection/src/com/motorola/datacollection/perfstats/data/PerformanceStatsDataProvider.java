/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date         CR Number       Brief Description
 * -------------------------   ----------   -------------   ------------------------------
 * w04917 (Brian Lee)          2012/01/20   IKCTXTAW-359    Initial version
 *
 */

package com.motorola.datacollection.perfstats.data;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.motorola.datacollection.perfstats.PerformanceStatsService;

/**
 * @author w04917 (Brian Lee)
 * Provides simplified access to persisten data related to PerformanceStats,
 * and cahces the data to reduce DB and file system access
 */
public class PerformanceStatsDataProvider extends ContentProvider {
    private static final String TAG = "PerfStatsData";

    /* call method identifiers */
    private static final String CALL_GET_LOGGED_BYTES = "GET_LOGGED_BYTES";
    public static final String CALL_UPDATE_LOGGED_BYTES = "GET_UPDATE_LOGGED_BYTES";
    private static final String CALL_GET_LOG_FILTER_DATA = "GET_LOG_FILTER_DATA";
    private static final String CALL_SET_LOG_FILTER_DATA = "SET_LOG_FILTER_DATA";

    public static final String KEY_COMPONENT = "component";
    public static final String KEY_ACTION = "action";
    public static final String KEY_LOGGED_BYTES = "logged_bytes";

    /* logged bytes cache.
     * The logged bytes cache only holds the logged bytes for a single day.
     * This date is stored in mLoggedBytesCacheDate.
     * An alternative method is to store cache in a tree-structure,
     * with date, component, and action representing each level of the tree and the loggedbytes
     * as a leaf. We can then traverse the tree to get the sum of each level. The disadvantage
     * of the tree approach is that we'll have to calculate the sum everytime we're asked. */
    private String mLoggedBytesCacheDate;
    private int mLoggedBytesGlobal;
    /* use component as key */
    private final HashMap<String, Integer> mLoggedBytesComponent = new HashMap<String, Integer>();
    /* use component.action as key */
    private final HashMap<String, Integer> mLoggedBytesAction = new HashMap<String, Integer>();
    private static final String DELIM = ".";


    /* Temporary memory storage for storing start times and log filter data.
     * Not using persistent storage because it needs to be fast, and it's not a big deal
     * to lose it.
     * Uses COMPONENT.ACTION as key, which is generated with the makeKey() helper function
     */
    private final HashMap<String, Bundle> mLogFilterData = new HashMap<String, Bundle>();

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
        /* read logged bytes */
        refreshLoggedBytes();

        return true;
    }

    private Bundle getLoggedBytes(Bundle extras) {
        Bundle data = null;
        if (extras != null) {
            String component = extras.getString(KEY_COMPONENT);
            String action = extras.getString(KEY_ACTION);
            if (component != null && !component.isEmpty() && action != null && !action.isEmpty()) {
                Integer value = null;

                int componentLoggedBytes = 0;
                value = mLoggedBytesComponent.get(component);
                if (value != null) {
                    componentLoggedBytes = value;
                }

                int actionLoggedBytes = 0;
                String key = makeKey(component, action);
                value = mLoggedBytesAction.get(key);
                if (value != null) {
                    actionLoggedBytes = value;
                }

                data = new Bundle();
                data.putInt(KEY_LOGGED_BYTES, mLoggedBytesGlobal);
                data.putInt(KEY_COMPONENT, componentLoggedBytes);
                data.putInt(KEY_ACTION, actionLoggedBytes);
            }
        }
        return data;
    }

    private void refreshLoggedBytes() {
        mLoggedBytesGlobal = 0;
        mLoggedBytesComponent.clear();
        mLoggedBytesAction.clear();
        mLoggedBytesCacheDate = PerformanceStatsDb.getDate();

        PerformanceStatsDb db = new PerformanceStatsDb(getContext());
        mLoggedBytesGlobal = db.getGlobalLoggedBytes(mLoggedBytesCacheDate);
        mLoggedBytesComponent.putAll(db.getComponentLoggedBytes(mLoggedBytesCacheDate));
        mLoggedBytesAction.putAll(db.getActionLoggedBytes(mLoggedBytesCacheDate));
        db.close();
    }

    private void updateLoggedBytes(Bundle extras) {
        if (extras != null) {
            String component = extras.getString(KEY_COMPONENT);
            String action = extras.getString(KEY_ACTION);
            int loggedBytes = extras.getInt(KEY_LOGGED_BYTES, 0);
            if (component != null && !component.isEmpty() && action != null && !action.isEmpty() &&
                    loggedBytes > 0) {
                PerformanceStatsDb db = new PerformanceStatsDb(getContext());
                boolean success = db.updateLoggedBytes(component, action, loggedBytes);
                db.close();
                if (success) {
                    refreshLoggedBytes();
                }
            }
        }
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
        if (CALL_GET_LOGGED_BYTES.equals(method)) {
            bundle = getLoggedBytes(extras);
        } else if (CALL_UPDATE_LOGGED_BYTES.equals(method)) {
            updateLoggedBytes(extras);
        } else if (CALL_SET_LOG_FILTER_DATA.equals(method)) {
            setLogFilterData(extras);
            //just return a null bundle
        } else if (CALL_GET_LOG_FILTER_DATA.equals(method)) {
            bundle = getLogFilterData(extras);
        }
        return bundle;
    }

    private void setLogFilterData(Bundle data) {
        if (data != null) {
            String component = data.getString(KEY_COMPONENT);
            String action = data.getString(KEY_ACTION);
            if (component != null && !component.isEmpty() && action != null && !action.isEmpty()) {
                /* replace any existing start times for multiple starts */
                mLogFilterData.put(makeKey(component,action), data);
            }
        }
    }

    private Bundle getLogFilterData(Bundle extras) {
        Bundle savedData = null;
        if (extras != null) {
            String component = extras.getString(KEY_COMPONENT);
            String action = extras.getString(KEY_ACTION);
            if (component != null && !component.isEmpty() && action != null && !action.isEmpty()) {
                savedData = mLogFilterData.remove(makeKey(component,action));
            }
        }
        return savedData;
    }

    /**
     * Generates a simple key by merging component and action with a delimiter.
     * Used in generating key for mActionStartTimes hashmap
     * @param component component name
     * @param action action
     * @return COMPONENT.ACTION string
     */
    static String makeKey(String component, String action) {
        return component + DELIM + action;
    }
}
