package com.motorola.contextual.pickers.conditions.calendar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.Persistence;
import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.CalendarContract.Calendars;
import android.util.Log;

/**
 * This class contains utility functions for performing various set of
 * operations
 *
 * @author wkh346
 *
 */
public class CalendarEventUtils implements CalendarEventSensorConstants {

    private static final String TAG = CalendarEventUtils.class.getSimpleName();

    /**
     * Number of calendars for which full display name shall be shown
     */
    private static final int NUM_CAL_NAME_SHOWN = 2;

    /**
     * This class represents the data structure for holding information about a
     * particular configuration
     *
     * @author wkh346
     *
     */
    public static class ConfigData {
        public String mCalendarIds = NULL_STRING;
        public int mExcludeAllDayEvents = CalendarEventDatabase.NOT_SELECTED;
        public int mOnlyIncludeEventsWithMultiplePeople = CalendarEventDatabase.NOT_SELECTED;
        public int mOnlyIncludeAcceptedEvents = CalendarEventDatabase.NOT_SELECTED;

        /**
         * Constructor
         */
        public ConfigData() {
            // Nothing to be done here
        }

        /**
         * Constructor
         *
         * @param calendarIds
         *            - String containing IDs of selected calendar separated by
         *            {@link CalendarEventSensorConstants#OR}
         * @param excludeAllDayEvents
         *            - true if this check box is selected
         * @param onlyIncludeEventsWithMultiplePeople
         *            - true if this check box is selected
         * @param onlyIncludeAcceptedEvents
         *            - true if this check box is selected
         */
        public ConfigData(String calendarIds, boolean excludeAllDayEvents,
                boolean onlyIncludeEventsWithMultiplePeople,
                boolean onlyIncludeAcceptedEvents) {
            mCalendarIds = calendarIds;
            mExcludeAllDayEvents = excludeAllDayEvents ? CalendarEventDatabase.SELECTED
                    : CalendarEventDatabase.NOT_SELECTED;
            mOnlyIncludeEventsWithMultiplePeople = onlyIncludeEventsWithMultiplePeople ? CalendarEventDatabase.SELECTED
                    : CalendarEventDatabase.NOT_SELECTED;
            mOnlyIncludeAcceptedEvents = onlyIncludeAcceptedEvents ? CalendarEventDatabase.SELECTED
                    : CalendarEventDatabase.NOT_SELECTED;
        }
    }

    /**
     * This method generates a ConfigData object from a String containing the
     * configuration
     *
     * @param configString
     *            - the String containing the configuration
     * @return - a ConfigData object if configString is parsed successfully,
     *         null otherwise
     */
    public static final ConfigData getConfigData(String configString) {
        String calendarIds = null;
        boolean excludeAllDayEvents = false;
        boolean onlyIncludeEventsWithMultiplePeople = false;
        boolean onlyIncludeAcceptedEvents = false;
        ConfigData configData = null;
        if (configString != null && !configString.isEmpty()) {
            double versionNumber = getVersionNumberFromConfig(configString);
            if (versionNumber != INVALID_VERSION) {
                calendarIds = getCalendarIdsStringFromConfig(configString);
                excludeAllDayEvents = configuredForExcludingAllDayEvents(configString);
                onlyIncludeEventsWithMultiplePeople = configuredForOnlyIncludingEventsMultiplePeople(configString);
                onlyIncludeAcceptedEvents = configuredForOnlyIncludingAcceptedEvents(configString);
                configData = new ConfigData(calendarIds, excludeAllDayEvents,
                        onlyIncludeEventsWithMultiplePeople,
                        onlyIncludeAcceptedEvents);
            }
        }
        if (configData == null) {
            Log.e(TAG, "getConfigData error while parsing configString "
                    + configString);
        }
        return configData;
    }

    /**
     * Extracts version number from config
     *
     * @param config
     *            - the configuration String
     * @return - the version number if extraction is successful,
     *         {@link CalendarEventSensorConstants#INVALID_VERSION} otherwise
     */
    private static final double getVersionNumberFromConfig(String config) {
        double version = INVALID_VERSION;
        if (config != null && !config.isEmpty()) {
            int end = config.indexOf(CONFIG_DELIMITER);
            if (end != -1) {
                String versionNumberString = config.substring(0, end).trim()
                        .substring(CONFIG_VERSION.length()).trim();
                try {
                    version = Double.parseDouble(versionNumberString);
                } catch (NumberFormatException exception) {
                    Log.e(TAG, "error while parsing config " + config
                            + " for version");
                    exception.printStackTrace();
                }
            }
        }
        return version;
    }

    /**
     * Extracts String containing calendar IDs, from config
     *
     * @param config
     *            - the configuration String
     * @return - the calendar IDs separated by
     *         {@link CalendarEventSensorConstants#OR} or
     *         {@link CalendarEventSensorConstants#NULL_STRING}. If extraction
     *         is unsuccessful then also
     *         {@link CalendarEventSensorConstants#NULL_STRING} is returned
     */
    private static final String getCalendarIdsStringFromConfig(String config) {
        String calendarIds = NULL_STRING;
        if (config != null && !config.isEmpty()) {
            // skipping delimiter for version
            int start = config.indexOf(CONFIG_DELIMITER) + 1;
            int end = config.indexOf(CONFIG_DELIMITER, start);
            if (end != -1) {
                calendarIds = config.substring(start, end).trim()
                        .substring(EVENTS_FROM_CALENDARS.length()).trim();
            }
        }
        return calendarIds;
    }

    /**
     * Finds out if configuration is set for excluding the all day events
     *
     * @param config
     *            - the configuration String
     * @return - true if configuration is set for excluding all day events,
     *         false otherwise
     */
    private static final boolean configuredForExcludingAllDayEvents(
            String config) {
        boolean excludeAllDayEvents = false;
        int start = -1;
        // skipping delimiters for version, calendars
        for (int index = 0; index < 2; index++) {
            start = config.indexOf(CONFIG_DELIMITER, start + 1);
        }
        if (start != -1) {
            start++;
            int end = config.indexOf(CONFIG_DELIMITER, start);
            if (end != -1) {
                excludeAllDayEvents = config.substring(start, end).trim()
                        .substring(EXCLUDE_ALL_DAY_EVENTS.length()).trim()
                        .equals(TRUE);
            }
        }
        return excludeAllDayEvents;
    }

    /**
     * Finds out if configuration is set for only including the events with
     * multiple people
     *
     * @param config
     *            - the configuration String
     * @return - true if configuration is for only including the events with
     *         multiple people, false otherwise
     */
    private static final boolean configuredForOnlyIncludingEventsMultiplePeople(
            String config) {
        boolean onlyIncludeEventsWithMultiplePeople = false;
        int start = -1;
        // skipping delimiters for version, calendars, all day
        for (int index = 0; index < 3; index++) {
            start = config.indexOf(CONFIG_DELIMITER, start + 1);
        }
        if (start != -1) {
            start++;
            int end = config.indexOf(CONFIG_DELIMITER, start);
            if (end != -1) {
                onlyIncludeEventsWithMultiplePeople = config
                        .substring(start, end)
                        .trim()
                        .substring(
                                INCLUDE_EVENTS_WITH_MULTIPLE_PEOPLE_ONLY
                                        .length()).trim().equals(TRUE);
            }
        }
        return onlyIncludeEventsWithMultiplePeople;
    }

    /**
     * Finds out if configuration is set for only including the accepted events
     *
     * @param config
     *            - the configuration String
     * @return - true if configuration is set for only including the accepted
     *         events, false otherwise
     */
    private static final boolean configuredForOnlyIncludingAcceptedEvents(
            String config) {
        boolean onlyIncludeAcceptedEvents = false;
        int start = -1;
        // skipping delimiters for version, calendars, all day, multiple people
        for (int index = 0; index < 4; index++) {
            start = config.indexOf(CONFIG_DELIMITER, start + 1);
        }
        if (start != -1) {
            start++;
            int end = config.indexOf(CONFIG_DELIMITER, start);
            if (end == -1) {
                end = config.length();
            }
            onlyIncludeAcceptedEvents = config.substring(start, end).trim()
                    .substring(INCLUDE_ACCEPTED_EVENTS_ONLY.length()).trim()
                    .equals(TRUE);
        }
        return onlyIncludeAcceptedEvents;
    }

    /**
     * This method composes the configuration String for specific selections
     * made by user
     *
     * @param calendarIds
     *            - String containing IDs of selected calendar separated by
     *            {@link CalendarEventSensorConstants#OR}
     * @param excludeAllDayEvents
     *            - true if this check box is selected
     * @param onlyIncludeEventsWithMultiplePeople
     *            - true if this check box is selected
     * @param onlyIncludeAcceptedEvents
     *            - true if this check box is selected
     * @return - the configuration String
     */
    public static final String getConfigString(String calendarIds,
            boolean excludeAllDayEvents,
            boolean onlyIncludeEventsWithMultiplePeople,
            boolean onlyIncludeAcceptedEvents) {
        StringBuilder builder = new StringBuilder();
        if (calendarIds == null || calendarIds.isEmpty()) {
            calendarIds = NULL_STRING;
        }
        builder.append(CONFIG_VERSION).append(SPACE).append(CURRENT_VERSION)
                .append(CONFIG_DELIMITER).append(EVENTS_FROM_CALENDARS)
                .append(SPACE).append(calendarIds).append(CONFIG_DELIMITER)
                .append(EXCLUDE_ALL_DAY_EVENTS).append(SPACE)
                .append(excludeAllDayEvents ? TRUE : FALSE)
                .append(CONFIG_DELIMITER)
                .append(INCLUDE_EVENTS_WITH_MULTIPLE_PEOPLE_ONLY).append(SPACE)
                .append(onlyIncludeEventsWithMultiplePeople ? TRUE : FALSE)
                .append(CONFIG_DELIMITER).append(INCLUDE_ACCEPTED_EVENTS_ONLY)
                .append(SPACE).append(onlyIncludeAcceptedEvents ? TRUE : FALSE);
        return builder.toString();
    }

    /**
     * This method returns same config with additional
     * {@link CalendarEventSensorConstants#NOTIFY_STRICTLY_ON_TIME} field
     *
     * @param configWithoutNotifyField
     *            - config without optional
     *            {@link CalendarEventSensorConstants#NOTIFY_STRICTLY_ON_TIME}
     *            field
     * @return - config with additional
     *         {@link CalendarEventSensorConstants#NOTIFY_STRICTLY_ON_TIME}
     *         field
     */
    public static final String getConfigWithNotifyField(
            String configWithoutNotifyField) {
        StringBuilder builder = new StringBuilder();
        builder.append(configWithoutNotifyField).append(CONFIG_DELIMITER)
                .append(NOTIFY_STRICTLY_ON_TIME).append(SPACE).append(FALSE);
        return builder.toString();
    }

    /**
     * This method broadcasts the notify intent
     *
     * @param context
     *            - the application's context
     * @param configsStatesMap
     *            - the HashMap with configuration as key and state as value
     */
    public static final void sendNotifyIntent(Context context,
            HashMap<String, String> configsStatesMap) {
        // For inference the configs have additional parameter called
        // notify_strictly_on_time set to false. The calendar service and
        // database are not aware of this field since it's useless for them.
        // When calendar service decides to notify core then it constructs hash
        // map of configs with states but these configs do not have additional
        // notify_strictly_on_time field. This method checks whether
        // subscription happened for normal config or for config with additional
        // field and then modifies the config states map accordingly.
        if (configsStatesMap != null && !configsStatesMap.isEmpty()) {
            List<String> configsList = Persistence.retrieveValuesAsList(
                    context, KEY_CALENDAR_EVENTS_CONFIGS);
            Iterator<Map.Entry<String, String>> iterator = configsStatesMap
                    .entrySet().iterator();
            HashMap<String, String> additionalConfigsStatesMap = new HashMap<String, String>();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                String config = entry.getKey();
                String state = entry.getValue();
                if (!configsList.contains(config)) {
                    iterator.remove();
                }
                String configWithNotifyField = getConfigWithNotifyField(config);
                if (configsList.contains(configWithNotifyField)) {
                    additionalConfigsStatesMap
                            .put(configWithNotifyField, state);
                }
            }
            if (!additionalConfigsStatesMap.isEmpty()) {
                Set<String> keySet = additionalConfigsStatesMap.keySet();
                for (String config : keySet) {
                    configsStatesMap.put(config,
                            additionalConfigsStatesMap.get(config));
                }
            }
            if (!configsStatesMap.isEmpty()) {
                Intent notifyIntent = CommandHandler.constructNotification(
                        configsStatesMap, CALENDAR_EVENTS_PUBLISHER_KEY);
                context.sendBroadcast(notifyIntent, PERM_CONDITION_PUBLISHER_ADMIN);
                if (LOG_INFO) {
                    Set<String> keySet = configsStatesMap.keySet();
                    for (String config : keySet) {
                        String state = configsStatesMap.get(config);
                        if (TRUE.equals(state)) {
                            Log.i(TAG,
                                    "sendNotifyIntent activation intent broadcasted for config = "
                                            + config);
                        } else if (FALSE.equals(state)) {
                            Log.i(TAG,
                                    "sendNotifyIntent de-activation intent broadcasted for config = "
                                            + config);
                        }
                    }
                }
            }
        }
    }

    /**
     * This method broadcasts the refresh response intent
     *
     * @param context
     *            - the application's context
     * @param config
     *            - the String containing the configuration
     * @param description
     *            - the String containing the description
     * @param responseId
     *            - same as requestId
     * @param state
     *            - {@link Constants#TRUE} if the configuration is active,
     *            {@link Constants#FALSE} otherwise
     */
    public static final void sendRefreshResponseIntent(Context context,
            String config, String description, String responseId, String state) {
        Intent refreshIntent = new Intent(ACTION_CONDITION_PUBLISHER_EVENT);
        refreshIntent.putExtra(EXTRA_EVENT_TYPE, REFRESH_RESPONSE);
        refreshIntent.putExtra(EXTRA_PUB_KEY, CALENDAR_EVENTS_PUBLISHER_KEY);
        refreshIntent.putExtra(EXTRA_RESPONSE_ID, responseId);
        String status = (description != null) ? SUCCESS : FAILURE;
        refreshIntent.putExtra(EXTRA_STATUS, status);
        refreshIntent.putExtra(EXTRA_DESCRIPTION, description);
        refreshIntent.putExtra(EXTRA_STATE, state);
        refreshIntent.putExtra(EXTRA_CONFIG, config);
        context.sendBroadcast(refreshIntent, PERM_CONDITION_PUBLISHER_ADMIN);
        if (LOG_INFO) {
            Log.i(TAG,
                    "sendRefreshResponseIntent send refresh response for config = "
                            + config + " with description = " + description
                            + " with state = " + state + " and status "
                            + status);
        }
    }

    /**
     * This method generates an ArrayList of Integer containing the calendar IDs
     * specified in calendarIds String
     *
     * @param calendarIds
     *            - the String containing calendar IDs separated by
     *            {@link CalendarEventSensorConstants#OR}
     * @return - the ArrayList of Integer containing the calendar IDs. null is
     *         returned if calendarIds is either null or empty
     */
    public static final ArrayList<Integer> getListOfCalendarIdsFromString(
            String calendarIds) {
        ArrayList<Integer> calendarIdsList = null;
        String[] calendarIdsArr = null;
        if (calendarIds != null && !calendarIds.isEmpty()
                && !calendarIds.equals(NULL_STRING)) {
            calendarIdsList = new ArrayList<Integer>();
            calendarIdsArr = calendarIds.split(OR);
            for (String id : calendarIdsArr) {
                calendarIdsList.add(Integer.valueOf(Integer.parseInt(id)));
            }
            Collections.sort(calendarIdsList);
        }
        return calendarIdsList;
    }

    /**
     * This method generated the configuration String from list of calendar IDs
     *
     * @param calendarIdsList
     *            - the list of calendar IDs
     * @return - the String containing the calendar IDs separated by
     *         {@link CalendarEventSensorConstants#OR}. null is returned if
     *         calendarIdsList is null or calendarIdsList contains
     *         {@link CalendarDialogListAdapter#ALL_CALENDARS_ID}
     */
    public static final String getStringOfCalendarIdsFromList(
            ArrayList<Integer> calendarIdsList) {
        StringBuilder calendarIds = new StringBuilder();
        if (calendarIdsList == null
                || calendarIdsList
                        .contains(CalendarDialogListAdapter.ALL_CALENDARS_ID)) {
            return null;
        } else if (calendarIdsList.size() == 1) {
            calendarIds.append(calendarIdsList.get(0));
        } else {
            Collections.sort(calendarIdsList);
            int i;
            for (i = 0; i < calendarIdsList.size() - 1; i++) {
                calendarIds.append(calendarIdsList.get(i)).append(OR);
            }
            calendarIds.append(calendarIdsList.get(i));
        }
        return calendarIds.toString();
    }

    /**
     * This method generated the description String
     *
     * @param context
     *            - the application's context
     * @param calendarIdsList
     *            - the list of calendar IDs
     * @return - the description String
     */
    public static final String getDescription(Context context,
            ArrayList<Integer> calendarIdsList) {
        String description = null;
        String calendarNames = null;
        StringBuilder calendarNamesBuilder = new StringBuilder();
        if (calendarIdsList == null
                || calendarIdsList
                        .contains(CalendarDialogListAdapter.ALL_CALENDARS_ID)) {
            calendarNames = context.getString(R.string.all_calendars_small);
        } else {
            int selectedCalendarIdsSize = calendarIdsList.size();
            StringBuilder selectionBuilder = new StringBuilder();
            selectionBuilder.append(Calendars._ID + EQUALS_TO + INVERTED_COMMA
                    + calendarIdsList.get(0) + INVERTED_COMMA);
            if (selectedCalendarIdsSize <= NUM_CAL_NAME_SHOWN) {
                for (int i = 1; i < selectedCalendarIdsSize; i++) {
                    selectionBuilder.append(OR + Calendars._ID + EQUALS_TO
                            + INVERTED_COMMA + calendarIdsList.get(i)
                            + INVERTED_COMMA);
                }
            }
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(
                        Calendars.CONTENT_URI,
                        new String[] { Calendars._ID,
                                Calendars.CALENDAR_DISPLAY_NAME },
                        selectionBuilder.toString(), null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        int displayNameColumnIndex = cursor
                                .getColumnIndexOrThrow(Calendars.CALENDAR_DISPLAY_NAME);
                        int idColumnIndex = cursor
                                .getColumnIndexOrThrow(Calendars._ID);
                        String displayName = null;
                        int id = cursor.getInt(idColumnIndex);
                        if (id == CalendarDialogListAdapter.PHONE_CALENDAR_ID) {
                            displayName = context
                                    .getString(R.string.calendar_phone_display_name);
                        } else {
                            displayName = cursor
                                    .getString(displayNameColumnIndex);
                        }
                        if (selectedCalendarIdsSize <= NUM_CAL_NAME_SHOWN) {
                            if (cursor.isFirst()) {
                                calendarNamesBuilder.append(displayName);
                            } else {
                                // The names of the calendars shall be displayed
                                // in this case separated by or
                                calendarNamesBuilder.append(SPACE)
                                        .append(context.getString(R.string.or))
                                        .append(SPACE).append(displayName);
                            }
                        } else {
                            // The number of selected calendars are more than
                            // NUM_CAL_NAME_SHOWN and cursor count is one
                            calendarNamesBuilder
                                    .append(displayName)
                                    .append(SPACE)
                                    .append(context.getString(R.string.or))
                                    .append(SPACE)
                                    .append(String
                                            .valueOf(selectedCalendarIdsSize - 1))
                                    .append(SPACE)
                                    .append(context.getString(R.string.more));
                            break;
                        }
                    } while (cursor.moveToNext());
                    calendarNames = calendarNamesBuilder.toString();
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                Log.e(TAG, "getDescription failed to read "
                        + Calendars.CONTENT_URI + " with selection "
                        + selectionBuilder.toString());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (calendarNames != null && !calendarNames.isEmpty()
                && !calendarNames.equals(NULL_STRING)) {
            description = context.getString(R.string.from_small) + SPACE
                    + calendarNames;
        }
        return description;
    }

    /**
     * This method generated the description String
     *
     * @param context
     *            - the application's context
     * @param calendarIds
     *            - the String containing calendar IDs separated by
     *            {@link CalendarEventSensorConstants#OR}
     * @return - the description String
     */
    public static final String getDescription(Context context,
            String calendarIds) {
        return getDescription(context,
                getListOfCalendarIdsFromString(calendarIds));
    }

    /**
     * This method converts the old configuration to new configuration
     *
     * @param oldConfig
     *            - the intent.toUri of edit uri intent
     * @return - new configuration String
     */
    public static final String getNewConfigFromOldConfig(String oldConfig) {
        String newConfig = oldConfig;
        if (oldConfig != null && oldConfig.contains(INTENT_PREFIX)) {
            // This is old config and if this doesn't get parsed then null shall
            // be returned
            newConfig = null;
            try {
                Intent configIntent = Intent.parseUri(oldConfig, 0);
                String calendarIds = configIntent
                        .getStringExtra(EXTRA_CALENDARS);
                boolean excludeAllDayEvents = true;
                boolean onlyIncludeEventsWithMultiplePeople = true;
                boolean onlyIncludeAcceptedEvents = true;
                String allDayEventsString = configIntent
                        .getStringExtra(EXTRA_ALLDAY_EVENTS);
                if (allDayEventsString == null
                        || allDayEventsString.equals(FALSE)) {
                    excludeAllDayEvents = false;
                }
                String multipleParticipantsString = configIntent
                        .getStringExtra(EXTRA_MULTIPLE_PARTICIPANTS);
                if (multipleParticipantsString == null
                        || multipleParticipantsString.equals(FALSE)) {
                    onlyIncludeEventsWithMultiplePeople = false;
                }
                String acceptedEventsString = configIntent
                        .getStringExtra(EXTRA_ACCEPTED_EVENTS);
                if (acceptedEventsString == null
                        || acceptedEventsString.equals(FALSE)) {
                    onlyIncludeAcceptedEvents = false;
                }
                newConfig = getConfigString(calendarIds, excludeAllDayEvents,
                        onlyIncludeEventsWithMultiplePeople,
                        onlyIncludeAcceptedEvents);
            } catch (Exception exception) {
                exception.printStackTrace();
                Log.e(TAG,
                        "getNewConfigFromOldConfig failed to parse oldConfig = "
                                + oldConfig);
            }
        }
        return newConfig;
    }

    /**
     * This method queries the database and returns the state of the config
     *
     * @param context
     *            - the application's context
     * @param config
     *            - the configuration whose state needs to be returned
     * @return - the state of passed config
     */
    public static final String getConfigState(Context context, String config) {
        String state = FALSE;
        ConfigData configData = getConfigData(config);
        if (configData != null) {
            CalendarEventDatabase database = CalendarEventDatabase
                    .getInstance(context);
            state = database.getRuleState(configData.mCalendarIds,
                    configData.mExcludeAllDayEvents,
                    configData.mOnlyIncludeAcceptedEvents,
                    configData.mOnlyIncludeEventsWithMultiplePeople);
        }
        return state;
    }

    /**
     * This method starts calendar event service for specified action
     *
     * @param context
     *            - application's context
     * @param action
     *            - intent action
     */
    public static final void startServiceForAction(Context context,
            String action) {
        if (action != null && !action.isEmpty()) {
            if (LOG_INFO) {
                Log.i(TAG,
                        "startServiceForAction starting calendar event service for action "
                                + action);
            }
            Intent serviceIntent = new Intent(action);
            serviceIntent.setClass(context, CalendarEventService.class);
            context.startService(serviceIntent);
        }
    }

    /**
     * This method starts calendar event service for
     * {@link Constants#SA_CORE_INIT_COMPLETE} action
     *
     * @param context
     *            - application's context
     */
    public static final void notifyServiceForCoreInitComplete(Context context) {
        startServiceForAction(context, SA_CORE_INIT_COMPLETE);
    }

    /**
     * This method starts calendar event service for
     * {@link CalendarEventSensorConstants#ACTION_SCHEDULE_EVENT_AWARE} action
     *
     * @param context
     *            - application's context
     */
    public static final void notifyServiceForSchedulingEventAware(
            Context context) {
        startServiceForAction(context, ACTION_SCHEDULE_EVENT_AWARE);
    }

    /**
     * This method starts calendar event service for
     * {@link CalendarEventSensorConstants#ACTION_EVENTS_TABLE_CHANGED} action
     *
     * @param context
     *            - application's context
     */
    public static final void notifyServiceOfCalendarDbChange(Context context) {
        startServiceForAction(context, ACTION_EVENTS_TABLE_CHANGED);
    }

    /**
     * This method starts calendar event service for
     * {@link CalendarEventSensorConstants#CALENDAR_EVENTS_IMPORT_ACTION} action
     *
     * @param context
     *            - application's context
     * @param intent
     *            - intent containing
     *            {@link CalendarEventSensorConstants#EXTRA_CALENDAR_EVENTS_DATA}
     *            data
     */
    public static final void notifyServiceToImportRules(Context context,
            Intent intent) {
        if (LOG_INFO) {
            Log.i(TAG,
                    "notifyServiceToImportRules starting calendar event service for action "
                            + CALENDAR_EVENTS_IMPORT_ACTION);
        }
        Intent serviceIntent = new Intent(CALENDAR_EVENTS_IMPORT_ACTION);
        serviceIntent.putExtra(EXTRA_CALENDAR_EVENTS_DATA,
                intent.getStringExtra(EXTRA_CALENDAR_EVENTS_DATA));
        serviceIntent.setClass(context, CalendarEventService.class);
        context.startService(serviceIntent);
    }

    /**
     * If this method returns true then registrations will alarm manager shall
     * be of type RTC_WAKEUP
     *
     * @param context
     *            - application's context
     * @return - true or false
     */
    public static final boolean strictlyNotifyOnTime(Context context) {
        boolean result = false;
        List<String> configsList = Persistence.retrieveValuesAsList(context,
                KEY_CALENDAR_EVENTS_CONFIGS);
        for (String config : configsList) {
            if (!config.contains(NOTIFY_STRICTLY_ON_TIME)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * This method finds out if config can be removed from db or not
     *
     * @param context
     *            - application's context
     * @param config
     *            - the config string
     * @return - true or false
     */
    public static final boolean canRemoveConfigFromDb(Context context,
            String config) {
        // This if for inference and meeting configs. In inference config
        // notify_strictly_on_time is false so that registrations with alarm
        // manager happen with type RTC where as for meeting config the
        // registrations shall happen with type RTC_WAKEUP. This is for
        // resolving the case where cancel comes for inference config and
        // meeting config is registered and vice versa
        boolean result = true;
        String configWithNotifyField = null;
        String configWithoutNotifyField = null;
        if (config.contains(NOTIFY_STRICTLY_ON_TIME)) {
            configWithNotifyField = config;
            configWithoutNotifyField = configWithNotifyField.substring(0,
                    configWithNotifyField.lastIndexOf(CONFIG_DELIMITER));
        } else {
            configWithoutNotifyField = config;
            configWithNotifyField = getConfigWithNotifyField(configWithoutNotifyField);
        }
        List<String> configsList = Persistence.retrieveValuesAsList(context,
                KEY_CALENDAR_EVENTS_CONFIGS);
        if (configsList.contains(configWithNotifyField)
                || configsList.contains(configWithoutNotifyField)) {
            result = false;
        }
        if (LOG_INFO) {
            Log.i(TAG, "canRemoveConfig returning " + result + " for config "
                    + config);
        }
        return result;
    }

    /**
     * This method validates the config. Currently the validation is done for
     * version only
     *
     * @param config
     *            - the config String
     * @return - if validation is successful then same config is returned
     *         otherwise null is returned
     */
    public static final String validateConfig(String config) {
        String validatedConfig = null;
        if (config != null && !config.isEmpty()) {
            validatedConfig = getNewConfigFromOldConfig(config);
            if (validatedConfig == null
                    || getVersionNumberFromConfig(validatedConfig) == INVALID_VERSION) {
                validatedConfig = null;
            }
        }
        if (LOG_INFO) {
            Log.i(TAG, "validateConfig returning validated config "
                    + validatedConfig + " for config " + config);
        }
        return validatedConfig;
    }

}
