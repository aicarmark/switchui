package com.motorola.contextual.smartprofile.locations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Persistence;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.virtualsensor.locationsensor.dbhelper.LocationDatabase;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * This class provides utility methods for performing several operations related
 * to location condition publisher
 *
 * @author wkh346
 *
 */
public class LocationUtils implements LocConstants, DbSyntax {

    private static final String TAG = LocationUtils.class.getSimpleName();

    /**
     * This method returns the config String that it creates from passed
     * poiTagsList
     *
     * @param poiTagsList
     *            - the list of selected poi Tags
     * @return - the config String. null is returned if poiTagsList is either
     *         null or empty
     */
    public static final String getConfig(ArrayList<String> poiTagsList) {
        String config = null;
        if (poiTagsList != null && !poiTagsList.isEmpty()) {
            Collections.sort(poiTagsList);
            StringBuilder builder = new StringBuilder();
            builder.append(CONFIG_VERSION).append(BLANK_SPC)
                    .append(CURRENT_VERSION).append(CONFIG_DELIMITER)
                    .append(SELECTED_LOCATIONS_TAGS).append(BLANK_SPC)
                    .append(poiTagsList.get(0));
            int size = poiTagsList.size();
            for (int index = 1; index < size; index++) {
                builder.append(OR_STRING).append(poiTagsList.get(index));
            }
            config = builder.toString();
        }
        return config;
    }

    /**
     * This method returns the description String that it creates from passed
     * config
     *
     * @param context
     *            - the application's context
     * @param config
     *            - the config String
     * @return - the description String. null is returned if config String is
     *         either null or empty
     */
    public static final String getDescription(Context context, String config) {
        String description = null;
        if (config != null && !config.isEmpty()) {
            ArrayList<String> poiTagsList = new ArrayList<String>();
            extractLocationsFromConfig(config, poiTagsList);
            if (!poiTagsList.isEmpty()) {
                Cursor cursor = null;
                try {
                    StringBuilder selection = new StringBuilder();
                    int size = poiTagsList.size();
                    int index = 0;
                    selection.append(LocationDatabase.PoiTable.Columns.POI)
                            .append(EQUALS_TO).append(SINGLE_QUOTE)
                            .append(poiTagsList.get(index))
                            .append(SINGLE_QUOTE);
                    for (index = 1; index < size; index++) {
                        selection.append(OR_STRING)
                                .append(LocationDatabase.PoiTable.Columns.POI)
                                .append(EQUALS_TO).append(SINGLE_QUOTE)
                                .append(poiTagsList.get(index))
                                .append(SINGLE_QUOTE);
                    }
                    String[] projection = new String[] {
                            LocationDatabase.PoiTable.Columns.POI,
                            LocationDatabase.PoiTable.Columns.ADDR,
                            LocationDatabase.PoiTable.Columns.NAME };
                    cursor = context.getContentResolver().query(
                            Uri.parse(LOC_POI_URI), projection,
                            selection.toString(), null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        ArrayList<String> poiNamesList = new ArrayList<String>();
                        do {
                            String name = cursor
                                    .getString(cursor
                                            .getColumnIndexOrThrow(LocationDatabase.PoiTable.Columns.NAME));
                            if (name == null) {
                                name = cursor
                                        .getString(cursor
                                                .getColumnIndexOrThrow(LocationDatabase.PoiTable.Columns.ADDR));
                            }
                            poiNamesList.add(name);
                        } while (cursor.moveToNext());
                        description = getDescription(context, poiNamesList);
                    }
                } catch (Exception exception) {
                    Log.e(TAG,
                            "getDescription error while reading location database");
                    exception.printStackTrace();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
        return description;
    }

    /**
     * This method returns the description String that it creates from passed
     * poiNamesList
     *
     * @param context
     *            - the application's context
     * @param poiNamesList
     *            - the list of names of selected pois (locations)
     * @return - the description String. null is returned if poiNamesList is
     *         either null or empty
     */
    public static final String getDescription(Context context,
            ArrayList<String> poiNamesList) {
        String description = null;
        if (poiNamesList != null && !poiNamesList.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            builder.append(poiNamesList.get(0));
            String orString = BLANK_SPC + context.getString(R.string.or)
                    + BLANK_SPC;
            int size = poiNamesList.size();
            for (int index = 1; index < size; index++) {
                builder.append(orString).append(poiNamesList.get(index));
            }
            description = builder.toString();
        }
        return description;
    }

    /**
     * This method extracts the poiTags(locations) from passed config String and
     * adds these poiTags to passed poiTagsList
     *
     * @param config
     *            - the config String
     * @param poiTagsList
     *            - the list to which the extracted poiTags are added
     */
    public static final void extractLocationsFromConfig(String config,
            ArrayList<String> poiTagsList) {
        if (config != null && !config.isEmpty() && config.contains(SELECTED_LOCATIONS_TAGS) && poiTagsList != null) {
            int start = config.indexOf(CONFIG_DELIMITER);
            if (start != -1) {
                start++;
                String[] locationsArray = config.substring(start).trim()
                        .substring(SELECTED_LOCATIONS_TAGS.length()).trim()
                        .split(OR_STRING);
                if (locationsArray != null) {
                    for (String location : locationsArray) {
                        poiTagsList.add(location);
                    }
                }
            }
        }
    }

    /**
     * This method returns the current location tag stored in a shared
     * preference
     *
     * @param context
     *            - the application's context
     * @return - the current location tag specified by
     *         {@link LocConstants#KEY_CURRENT_LOCATION_TAG}
     */
    public static final String getCurrentLocationTag(Context context) {
        return Persistence.retrieveValue(context, KEY_CURRENT_LOCATION_TAG);
    }

    /**
     * This method stores the current location tag in shared preference
     *
     * @param context
     *            - the application's context
     * @param poiTag
     *            - the location tag specified by
     *            {@link LocConstants#KEY_CURRENT_LOCATION_TAG}
     */
    public static final void setCurrentLocationTag(Context context,
            String poiTag) {
        Persistence.commitValue(context, KEY_CURRENT_LOCATION_TAG, poiTag);
        if (LOG_INFO) {
            Log.i(TAG, "setCurrentLocationTag current location tag set to "
                    + poiTag);
        }
    }

    /**
     * This method returns the current state of passed config
     *
     * @param context
     *            - the application's context
     * @param config
     *            - the config String
     * @return - the current state
     */
    public static final String getConfigState(Context context, String config) {
        return getConfigState(context, config, getCurrentLocationTag(context));
    }

    /**
     * This method returns the current state of passed config
     *
     * @param context
     *            - the application's context
     * @param config
     *            - the config String
     * @param currentLocationTag
     *            - the current location tag stored in shared preference
     *            identified by {@link LocConstants#KEY_CURRENT_LOCATION_TAG}
     * @return - the current state
     */
    public static final String getConfigState(Context context, String config,
            String currentLocationTag) {
        String state = FALSE;
        if (currentLocationTag != null && !currentLocationTag.isEmpty()) {
            ArrayList<String> poiTagsList = new ArrayList<String>();
            extractLocationsFromConfig(config, poiTagsList);
            if (!poiTagsList.isEmpty()
                    && poiTagsList.contains(currentLocationTag)) {
                state = TRUE;
            }
        }
        return state;
    }

    /**
     * This method broadcasts the notify intent for various configs of location
     * condition publisher
     *
     * @param context
     *            - the application's context
     * @param currentLocationTag
     *            - the current location tag
     */
    public static final void sendNotifyIntent(Context context,
            String currentLocationTag) {
        setCurrentLocationTag(context, currentLocationTag);
        ArrayList<String> configsList = (ArrayList<String>) Persistence
                .retrieveValuesAsList(context, KEY_LOCATION_CONFIGS);
        if (!configsList.isEmpty()) {
            HashMap<String, String> configsStatesMap = new HashMap<String, String>();
            for (String config : configsList) {
                configsStatesMap.put(config,
                        getConfigState(context, config, currentLocationTag));
            }
            Intent notifyIntent = CommandHandler.constructNotification(
                    configsStatesMap, LOCATION_PUBLISHER_KEY);
            context.sendBroadcast(notifyIntent, PERM_CONDITION_PUBLISHER_ADMIN);
            if (LOG_INFO) {
                Set<String> keySet = configsStatesMap.keySet();
                for (String config : keySet) {
                    Log.i(TAG,
                            "sendNotifyIntent notify send for config " + config
                                    + " with state "
                                    + configsStatesMap.get(config));
                }
            }
        }
    }

    /**
     * This method generates the new config from old config
     *
     * @param context
     *            - the application's context
     * @param oldConfig
     *            - the old config {@link Intent#toUri(int)}
     * @return - the new config String
     */
    public static final String getNewConfigFromOldConfig(Context context,
            String oldConfig) {
        if (LOG_INFO) {
            Log.i(TAG, "getNewConfigFromOldConfig oldConfig " + oldConfig);
        }
        String newConfig = oldConfig;
        if (oldConfig != null && oldConfig.contains(INTENT_PREFIX)) {
            // This is old config and if this doesn't get parsed then null shall
            // be returned
            newConfig = null;
            try {
                Intent configIntent = Intent.parseUri(oldConfig, 0);
                if (configIntent != null) {
                    String poiTags = configIntent
                            .getStringExtra(EXTRA_CURRENT_POITAG);
                    if (poiTags != null && !poiTags.isEmpty()) {
                        // In older versions the separator is locale dependent
                        String orSplitString = BLANK_SPC
                                + context.getString(R.string.or) + BLANK_SPC;
                        String[] poiTagsArray = poiTags.split(orSplitString);
                        if (poiTagsArray != null) {
                            newConfig = getConfig(new ArrayList<String>(
                                    Arrays.asList(poiTagsArray)));
                        }
                    }
                }
            } catch (Exception exception) {
                Log.e(TAG,
                        "getNewConfigFromOldConfig error while parsing oldConfig "
                                + oldConfig);
                exception.printStackTrace();
            }
        }
        if (LOG_INFO) {
            Log.i(TAG, "getNewConfigFromOldConfig newConfig " + newConfig);
        }
        return newConfig;
    }

    /**
     * This method finds out whether there exists any config with the poiTag
     *
     * @param context
     *            - application's context
     * @param poiTag
     *            - the poi tag for location
     * @return - true if there exists a config with poiTag, false otherwise
     */
    public static final boolean isPoiTagInUse(Context context, String poiTag) {
        boolean result = false;
        List<String> configsList = Persistence.retrieveValuesAsList(context,
                KEY_LOCATION_CONFIGS);
        for (String config : configsList) {
            if (config.contains(poiTag)) {
                result = true;
                break;
            }
        }
        return result;
    }
    
    /** checks if this location is part of any existing rules in Smart Rules DB.
     *  If this location poiTag is being used in any rule will return false else
     *  return true.
     *
     * @param poiTag - poi tag of the location that user is trying to delete
     * @return - true or false
     */
    public static boolean ifPoiCanBeDeleted(Context context, final String poiTag) {
        boolean result = true;
        if(LOG_DEBUG) Log.d(TAG, "ifPoiCanBeDeleted poiTag = "+poiTag);
        String whereClause = ConditionTable.Columns.CONDITION_CONFIG + LIKE + Q + LIKE_WILD + poiTag + LIKE_WILD + Q;

        Cursor cursor = context.getContentResolver().query(ConditionTable.CONTENT_URI, new String[] {ID}, whereClause, null, null);
        if(cursor != null) {
            if(cursor.getCount() > 0) {
                if(LOG_DEBUG) Log.d(TAG, "cursor count is "+cursor.getCount()+" return false");
                result = false;
            }
            cursor.close();
        } else
            Log.e(TAG, "Cursor is null for "+whereClause);

        return result;
    }


    /**
     * This method broadcast intent requesting core to send refresh for rules
     * with location
     *
     * @param context
     *            - application's context
     */
    public static final void initiateRefreshRequest(Context context) {
        Intent intent = new Intent(ACTION_CONDITION_PUBLISHER_EVENT);
        intent.putExtra(EXTRA_PUB_KEY, LOCATION_PUBLISHER_KEY);
        intent.putExtra(EXTRA_EVENT_TYPE, ASYNC_REFRESH);
        context.sendBroadcast(intent, PERM_CONDITION_PUBLISHER_ADMIN);
    }

}
