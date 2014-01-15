/*
 * @(#)setwallpaper.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * rdq478       2011/07/13   IKINTNETAPP-386    Initial version to support stateful
 *                                              action for setwallpaper.
 * rdq478       2011/08/16   IKSTABLE6-6826     Add enable/disable listener
 * rdq478       2011/09/22   IKSTABLE6-10141   Moved default wp name to Constants
 */

package com.motorola.contextual.actions;


import java.util.ArrayList;
import java.util.List;
import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.content.Intent;
import android.app.WallpaperManager;
import android.util.Log;

/**
 * This class extends the StatefulAction class for setwallpaper <code><pre>
 * CLASS:
 *     extends StatefulAction
 *
 * RESPONSIBILITIES:
 *     Access internal storage to get and save wallpaper.
 *
 * COLLABORATORS:
 *     None
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public final class setwallpaper extends StatefulAction implements Constants {

    private static final String TAG = TAG_PREFIX + setwallpaper.class.getSimpleName();
    public static final String WALLPAPER_ACTION_KEY = ACTION_KEY_PREFIX + setwallpaper.class.getSimpleName();
    private static final String LISTENER_REGISTERED_KEY = WALLPAPER_ACTION_KEY
            + "ListenerRegistered";
    private static final String REQUEST_COUNT_KEY = WALLPAPER_ACTION_KEY
            + "RequestCount";

    private String mUri = null;

    /**
     * Set to listener to accept or ignore wallpaper change intent. Currently,
     * it is also call from SetWallpaperActivity.java to enable/disable listener
     * during editing.
     *
     * @param context
     * @param value
     *            - either true or false
     */
    public static void setListener(Context context, boolean value) {
        Persistence.commitValue(context, LISTENER_REGISTERED_KEY, value);
    }

    /**
     * This method returns the value of LISTENER_REGISTERED_KEY stored in shared
     * preferences
     *
     * @param context
     * @return - the value of LISTENER_REGISTERED_KEY
     */
    public static boolean getListener(Context context) {
        return Persistence.retrieveBooleanValue(context,
                                                LISTENER_REGISTERED_KEY);
    }

    /**
     * Increase or decrease the request count by 1 based on the boolean increase
     *
     * @param context
     * @param increase
     *            - if true the request count is increased by 1, otherwise it is
     *            decreased by 1
     */
    private static void setRequestCount(Context context, boolean increase) {
        int requestCount = getRequestCount(context);
        if (increase) {
            Persistence.commitValue(context, REQUEST_COUNT_KEY,
                                    (requestCount + 1));
        } else if (requestCount > 0) {
            Persistence.commitValue(context, REQUEST_COUNT_KEY,
                                    (requestCount - 1));
        }
    }

    /**
     * This method returns the request count value stored in shared preferences
     *
     * @param context
     * @return - the request count
     */
    private static int getRequestCount(Context context) {
        return Persistence.retrieveIntValue(context, REQUEST_COUNT_KEY);
    }


    @Override
    public boolean setState(Context context, Intent intent) {
        mUri = intent.getStringExtra(EXTRA_URI);
        if (LOG_INFO) {
            Log.i(TAG, "setState mUri = " + mUri);
        }
        WallpaperManager wallManager = WallpaperManager.getInstance(context);
        if (wallManager.getWallpaperInfo() != null) {
            Log.e(TAG,
                  "Could not set wallpaper due to the current wallpaper is Live Wallpaper.");
            return false;
        }
        if (!FileUtils.isFileExist(context, WP_DIR, mUri)) {
            Log.e(TAG, "Wallpaper file not found");
            return false;
        }
        setRequestCount(context, true);
        Intent service = new Intent(context, WallpaperService.class);
        service.putExtra(EXTRA_URI, mUri);
        service.putExtra(EXTRA_SAVE_DEFAULT,
                         intent.getBooleanExtra(EXTRA_SAVE_DEFAULT, false));
        service.putExtra(EXTRA_RESTORE_DEFAULT,
                         intent.getBooleanExtra(EXTRA_RESTORE_DEFAULT, false));
        context.startService(service);
        return true;
    }


    @Override
    public String getState(Context context) {
        return mUri;
    }

    @Override
    public String getSettingString(Context context) {
        return ((mUri != null) ? mUri : context
                .getString(R.string.change_wallpaper));

    }

    @Override
    public String getDefaultSetting(Context context) {
        return getConfig(WP_DEFAULT);
    }

    @Override
    public String getActionString(Context context) {
        return context.getString(R.string.change_wallpaper);
    }

    @Override
    public Status handleSettingChange(Context context, Object obj) {
        Status status = Status.FAILURE;
        int requestCount = getRequestCount(context);
        boolean listenerRegistered = getListener(context);
        if (requestCount > 0) {
            setRequestCount(context, false);
            status = Status.NO_CHANGE;
        } else {
            status = listenerRegistered ? Status.SUCCESS : Status.NO_CHANGE;

        }
        if (LOG_INFO) {
            Log.i(TAG, "handleSettingChange request count = " + requestCount
                  + " listener registered = " + listenerRegistered
                  + " returning status = " + status);
        }
        return status;
    }

    @Override
    public String getActionKey() {
        return WALLPAPER_ACTION_KEY;
    }

    @Override
    public String getBroadcastAction() {
        return Intent.ACTION_WALLPAPER_CHANGED;
    }


    @Override
    public String getDescription(Context context, Intent configIntent) {
        String description = configIntent.getStringExtra(EXTRA_URI);
        if (description == null) {
            description = context.getString(R.string.change_wallpaper);
        }
        return description;
    }

    @Override
    public String getUpdatedConfig(Context context, Intent configIntent) {
        return getConfig(configIntent.getStringExtra(EXTRA_URI));
    }

    @Override
    public List<String> getConfigList(Context context) {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getDescriptionList(Context context) {
        return new ArrayList<String>();
    }

    @Override
    String getDefaultSetting(Context context, Intent defaultIntent) {
        return getUpdatedConfig(context, defaultIntent);
    }

    @Override
    public boolean validateConfig(Intent configIntent) {
        return configIntent.getStringExtra(EXTRA_URI) != null;
    }

    /**
     * Method to return config based on supplied parameters
     *
     * @param uri Wallpaper uri
     * @return Config
     */
    public static String getConfig(String uri) {
        Intent newConfigIntent = new Intent();
        newConfigIntent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        newConfigIntent.putExtra(EXTRA_URI, uri);
        return newConfigIntent.toUri(0);
    }
}
