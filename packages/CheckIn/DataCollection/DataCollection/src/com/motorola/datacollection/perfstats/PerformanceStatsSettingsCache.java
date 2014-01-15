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

import android.content.ContentProvider;
import android.content.ContentQueryMap;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.motorola.kpi.perfstats.LogSetting;

/**
 * @author w04917 (Brian Lee)
 * Provides simplified access to syncable settings related to PerformanceStats,
 * and cahces the data to reduce DB and file system access
 */
public class PerformanceStatsSettingsCache extends ContentProvider {
    private static final String TAG = "PerfStatsSettings";
    /* syncable settings provider */
    private static final Uri SETTINGS_URI =
        Uri.parse("content://com.motorola.blur.setupprovider/app_settings");
    private static final String COL_NAME = "name";
    private static final String COL_VALUE = "value";

    private static final String SETTING_HEADER = "perfstats";
    private static final String SETTING_DELIM = ".";

    // values in syncable setting
    private static final String SETTING_VALUE_ENABLED = "1";

    private static final int VALUE_ENABLED = 1;
    private static final int VALUE_DISABLED = -1;
    private static final int VALUE_UNSET = 0;

    private static final String[] SETTINGS_ENABLERS = {
        LogSetting.KEY_ENABLED,
        LogSetting.KEY_LOG_CPU_METRIC,
        LogSetting.KEY_LOG_MEMORY_METRIC,
        LogSetting.KEY_LOG_DISK_METRIC,
        LogSetting.KEY_LOG_NETWORK_METRIC,
        LogSetting.KEY_LOG_PROCESS_CPU_METRIC
    };

    private static final String[] SETTINGS_GENERIC = {
        LogSetting.KEY_SWITCH,
        LogSetting.KEY_LOG_LIMIT
    };

    private static final String[] SETTINGS_OVERRIDABLE = {
        LogSetting.KEY_MAX_NUM_PROCESS,
        LogSetting.KEY_THRESHOLD,
        LogSetting.KEY_LOG_RATE_NUM,
        LogSetting.KEY_LOG_RATE_DEN
    };

    /* call method identifiers */
    static final String CALL_GET_ENABLED = "GET_ENABLED";
    static final String CALL_GET_SETTINGS = "GET_SETTINGS";
    static final String CALL_GET_SINGLE_SETTING = "GET_SINGLE_SETTINGS";

    private ContentQueryMap mSettingsMap;

    static final String KEY_METRIC = "metric";

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
        boolean success = false;
        Cursor cursor = null;

        /* read all PerfStats related settings */
        String[] projection = new String[] { COL_NAME, COL_VALUE};
        String selection = COL_NAME + " LIKE ?";
        String[] selectionArgs =  new String[] { SETTING_HEADER + "%" };
        String sortOrder = COL_NAME + " ASC";
        cursor = getContext().getContentResolver().query(
                     SETTINGS_URI, projection, selection, selectionArgs, sortOrder);
        if (cursor != null) {
            mSettingsMap = new ContentQueryMap(cursor, COL_NAME, true, null);
            /* don't close cursor, ContentQueryMap needs it to observe changes */
        }

        success = (mSettingsMap != null);

        if (!success) {
            Log.w(TAG, "onCreate() failed!");
        }
        return success;
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
        if (CALL_GET_ENABLED.equals(method)) {
            bundle = getEnabled(extras);
        } else if (CALL_GET_SETTINGS.equals(method)) {
            bundle = getSettings(extras);
        }  else if (CALL_GET_SINGLE_SETTING.equals(method)) {
            bundle = getSingleSetting(arg);
        }
        return bundle;
    }

    private Bundle getEnabled(Bundle extras) {
        Bundle bundle = new Bundle();
        boolean enabled = false;
        if (mSettingsMap != null) {
            String globalSwitch = getGlobalValue(LogSetting.KEY_SWITCH);

            /* check the global switch first.
             * the global switch is an additional safeguard to prevent
             * unintentional switching on of perfStats, which may happen
             * with enabler-only mechanism in the cases where (but not limited to)
             * globalEnabler is undefined and a new component is added with ON value */
            if (SETTING_VALUE_ENABLED.equals(globalSwitch)) {
                /* now check individual enabler settings, which may or may not be defined */
                String globalValue = getGlobalValue(LogSetting.KEY_ENABLED);
                String componentValue = null;
                String actionValue = null;

                /* extras may be null or empty in cases where we just want to know if the global
                 * flag is on. (i.e. when handling date change or disk mounts)
                 */
                if (extras != null && !extras.isEmpty()) {
                    String component = extras.getString(LogSetting.KEY_COMPONENT);
                    String action = extras.getString(LogSetting.KEY_ACTION);
                    componentValue = getComponentValue(component, LogSetting.KEY_ENABLED);
                    actionValue = getActionValue(component, action, LogSetting.KEY_ENABLED);
                }
                int enableValue = isEnabled(
                                      new String[] { globalValue, componentValue, actionValue });
                /* only enable if explicitly enabled */
                enabled = (enableValue == VALUE_ENABLED);
            }
        }
        bundle.putBoolean(LogSetting.KEY_ENABLED, enabled);
        return bundle;
    }

    private Bundle getSingleSetting(String key) {
        Bundle bundle = null;
        if (key != null && !key.isEmpty()) {
            String value = getValue(formatKey(key));
            if (value != null) {
                bundle = new Bundle();
                bundle.putString(key, value);
            }
        }
        return bundle;
    }

    /**
     * @param extras Bundle containing component and action name
     * @return Bundle containing the settings the specified component-action pair should take.
     * ENABLER and OVERRIDABLE setting values are stored using the settings key.
     * GENERIC settings are stored using [component].[action].key format, since we need to
     * distinguish settings value for global, component, and action.
     */
    private Bundle getSettings(Bundle extras) {
        Bundle bundle = null;
        if (mSettingsMap != null && extras != null && !extras.isEmpty()) {
            String component = extras.getString(LogSetting.KEY_COMPONENT);
            String action = extras.getString(LogSetting.KEY_ACTION);
            if (component != null && !component.isEmpty() && action != null && !action.isEmpty()) {
                bundle = new Bundle();
                /* ENABLER settings have special type of override rules */
                for (String key : SETTINGS_ENABLERS) {
                    String globalValue = getGlobalValue(key);
                    String componentValue = getComponentValue(component, key);
                    String actionValue = getActionValue(component, action, key);
                    int enableValue = isEnabled(
                                          new String[] { globalValue, componentValue, actionValue });
                    /*
                     * if (enabled == VALUE_UNSET), load default values
                     */
                    bundle.putBoolean(key, (enableValue == VALUE_ENABLED));
                }

                /* GENERIC settings are not overridable.
                 * Each settings retains its own value */
                for (String key : SETTINGS_GENERIC) {
                    String globalValue = getGlobalValue(key);
                    if (globalValue != null && !globalValue.isEmpty()) {
                        bundle.putString(key, globalValue);
                    }

                    String componentValue = getComponentValue(component, key);
                    if (componentValue != null && !componentValue.isEmpty()) {
                        bundle.putString(LogSetting.KEY_COMPONENT + SETTING_DELIM + key,
                                         componentValue);
                    }

                    String actionValue = getActionValue(component, action, key);
                    if (actionValue != null && !actionValue.isEmpty()) {
                        bundle.putString(LogSetting.KEY_COMPONENT + SETTING_DELIM +
                                         LogSetting.KEY_ACTION + SETTING_DELIM + key,
                                         actionValue);
                    }
                }

                /* OVERRIDABLE settings can be overriden by children */
                for (String key : SETTINGS_OVERRIDABLE) {
                    String value = getActionValue(component, action, key);
                    if (value == null || value.isEmpty()) {
                        value = getComponentValue(component, key);
                        if (value == null || value.isEmpty()) {
                            value = getGlobalValue(key);
                            if (value == null || value.isEmpty()) {
                                //use default value for this key if it exists
                            }
                        }
                    }
                    if (value != null && !value.isEmpty()) {
                        bundle.putString(key, value);
                    }
                }
            }
        }
        return bundle;
    }

    /**
     * @param values Array of values
     * @return DISABLED if any of the values are set to values other than enable.
     *         ENABLED if there's no disable and there's at least one enable set.
     *         UNSET if nothing's set.
     */
    private int isEnabled(String[] values) {
        int result = VALUE_UNSET;
        if (values != null) {
            boolean enabled = false;
            boolean disabled = false;
            for (String value : values) {
                if (value != null) {
                    if (value.equals(SETTING_VALUE_ENABLED)) {
                        enabled = true;
                    } else {
                        disabled = true;
                        break;
                    }
                }
                //value would be null if it wasn't defined
            }
            if (disabled) {
                result = VALUE_DISABLED;
            } else if (enabled) {
                result = VALUE_ENABLED;
            }
        }
        return result;
    }

    private String getGlobalValue(String key) {
        return getValue(SETTING_HEADER + SETTING_DELIM + key);
    }

    private String getComponentValue(String component, String key) {
        String value = null;
        if (component != null && !component.isEmpty()) {
            value = getValue(SETTING_HEADER + SETTING_DELIM + component + SETTING_DELIM + key);
        }
        return value;
    }

    private String getActionValue(String component, String action, String key) {
        String value = null;
        if (component != null && !component.isEmpty() && action != null && !action.isEmpty()) {
            value = getValue(SETTING_HEADER + SETTING_DELIM +
                             component + SETTING_DELIM + action + SETTING_DELIM + key);
        }
        return value;
    }

    private String getValue(String key) {
        String value = null;
        if (mSettingsMap != null) {
            ContentValues cv = mSettingsMap.getValues(key);
            if (cv != null) {
                value = cv.getAsString(COL_VALUE);
            }
        }
        return value;
    }

    /**
     * @param basicKey key used for syncable settings db look up
     * @return basicKey prepended with perfstats header
     */
    static  String formatKey(String basicKey) {
        if (basicKey != null && !basicKey.isEmpty()) {
            basicKey = SETTING_HEADER + SETTING_DELIM + basicKey;
        }
        return basicKey;
    }
}
