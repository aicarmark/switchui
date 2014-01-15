/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calendar;

import android.app.Activity;
import android.app.backup.BackupManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.CalendarContract;
import android.provider.CalendarContract.CalendarCache;
import android.provider.SearchRecentSuggestions;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.calendar.DefaultCalendarDialogPreference.CalendarAccountItem;
import com.android.calendar.alerts.AlertReceiver;
import com.android.calendar.R;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.ContentObserver;
import android.provider.CalendarContract.Calendars;

public class GeneralPreferences extends PreferenceFragment implements
        OnSharedPreferenceChangeListener, OnPreferenceChangeListener {
    // The name of the shared preferences file. This name must be maintained for historical
    // reasons, as it's what PreferenceManager assigned the first time the file was created.
    static final String SHARED_PREFS_NAME = "com.android.calendar_preferences";
    private static final String TAG = "GeneralPreferences";
    // Preference keys
    public static final String KEY_HIDE_DECLINED = "preferences_hide_declined";
    public static final String KEY_WEEK_START_DAY = "preferences_week_start_day";
    public static final String KEY_SHOW_WEEK_NUM = "preferences_show_week_num";
    public static final String KEY_DAYS_PER_WEEK = "preferences_days_per_week";
    public static final String KEY_SKIP_SETUP = "preferences_skip_setup";

    public static final String KEY_CLEAR_SEARCH_HISTORY = "preferences_clear_search_history";

    public static final String KEY_ALERTS_CATEGORY = "preferences_alerts_category";
    public static final String KEY_ALERTS = "preferences_alerts";
    public static final String KEY_ALERTS_VIBRATE = "preferences_alerts_vibrate";
    public static final String KEY_ALERTS_VIBRATE_WHEN = "preferences_alerts_vibrateWhen";
    public static final String KEY_ALERTS_RINGTONE = "preferences_alerts_ringtone";
    public static final String KEY_ALERTS_POPUP = "preferences_alerts_popup";

    public static final String KEY_SHOW_CONTROLS = "preferences_show_controls";

    public static final String KEY_DEFAULT_REMINDER = "preferences_default_reminder";
    public static final int NO_REMINDER = -1;
    public static final String NO_REMINDER_STRING = "-1";
    public static final int REMINDER_DEFAULT_TIME = 10; // in minutes

    public static final String KEY_DEFAULT_CELL_HEIGHT = "preferences_default_cell_height";

    /** Key to SharePreference for default view (CalendarController.ViewType) */
    public static final String KEY_START_VIEW = "preferred_startView";
    /**
     *  Key to SharePreference for default detail view (CalendarController.ViewType)
     *  Typically used by widget
     */
    public static final String KEY_DETAILED_VIEW = "preferred_detailedView";
    public static final String KEY_DEFAULT_CALENDAR = "preference_defaultCalendar";

    /* to support default calendar
     * _id, sync_account and _sync_acount_type can uniquely identify a calendar
     * account, as 1.The id is reused when remove and add accounts. 2.The same
     * account can be add as google or exchange account.
     */
    static final String KEY_DEFAULT_CALENDAR_ACCOUNT = "preferences_default_calendar_account";
    static final String KEY_DEFAULT_CALENDAR_ID = "preferences_default_calendar_id";
    static final String KEY_DEFAULT_CALENDAR_TYPE = "preferences_default_calendar_type";
    
    //Lunar Calendar
    public static final String KEY_SHOW_HOLIDAY = "preferences_show_holiday";
    public static final boolean DEFAULT_VALUE_OF_SHOW_HOLIDAY = false;

    public static final String KEY_SHOW_LUNAR = "preferences_show_lunar";
    public static final boolean DEFAULT_VALUE_OF_SHOW_LUNAR = false;
    
    public static final String KEY_CONFERENCE_CALL_INFO = "preference_conference_call_info";
    public static final String KEY_CONFERENCE_NUMBER = "preference_conference_number";
    public static final String KEY_CONFERENCE_ID = "preference_conference_id";
    public static final String KEY_ENABLE_QUICK_CONFERENCE_DIALING = "preferences_enable_quick_conference_dialing";
    // to support default calendar view
    // Not supported in ics
    static final String KEY_DEFAULT_CALENDAR_VIEW_OLD = "preferences_default_calendar_view";
    static final String KEY_DEFAULT_CALENDAR_VIEW_ID = "preferences_default_calendar_view_id";
    
    // These must be in sync with the array preferences_week_start_day_values
    public static final String WEEK_START_DEFAULT = "-1";
    public static final String WEEK_START_SATURDAY = "7";
    public static final String WEEK_START_SUNDAY = "1";
    public static final String WEEK_START_MONDAY = "2";

    // These keys are kept to enable migrating users from previous versions
    private static final String KEY_ALERTS_TYPE = "preferences_alerts_type";
    private static final String ALERT_TYPE_ALERTS = "0";
    private static final String ALERT_TYPE_STATUS_BAR = "1";
    private static final String ALERT_TYPE_OFF = "2";
    static final String KEY_HOME_TZ_ENABLED = "preferences_home_tz_enabled";
    static final String KEY_HOME_TZ = "preferences_home_tz";

    // Default preference values
    public static final int DEFAULT_START_VIEW = CalendarController.ViewType.WEEK;
    public static final int DEFAULT_DETAILED_VIEW = CalendarController.ViewType.DAY;
    public static final boolean DEFAULT_SHOW_WEEK_NUM = false;
    //For ics
    static final String KEY_DEFAULT_CALENDAR_VIEW = "preferences_default_calendarview";

    CheckBoxPreference mAlert;
    ListPreference mVibrateWhen;
    RingtonePreference mRingtone;
    CheckBoxPreference mPopup;
    CheckBoxPreference mUseHomeTZ;
    CheckBoxPreference mHideDeclined;
    ListPreference mHomeTZ;
    ListPreference mWeekStart;
    ListPreference mDefaultReminder;
    DefaultCalendarDialogPreference mDefaultCal;
    ListPreference mDefaultCalView;
    // added by amt_sunli 2012-11-28 SWITCHUITWO-148 begin
    CheckBoxPreference mPreferencesShowLunar;
    // added by amt_sunli 2012-11-28 SWITCHUITWO-148 end
    
    private ContentResolver mContentResolver;
    
    Activity mActivity;

    private static CharSequence[][] mTimezones;

    /** Return a properly configured SharedPreferences instance */
    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Set the default shared preferences in the proper context */
    public static void setDefaultValues(Context context) {
        PreferenceManager.setDefaultValues(context, SHARED_PREFS_NAME, Context.MODE_PRIVATE,
                R.xml.general_preferences, false);
    }
    //Begin Motorola
    //Create an observer so that we can update the default calendar
    //when the calendar account changed.
    private ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateDefaultCalendarSettings(getSharedPreferences(mActivity));
        }
    };
    //End Motorola

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mActivity = getActivity();

        // Make sure to always use the same preferences file regardless of the package name
        // we're running under
        final PreferenceManager preferenceManager = getPreferenceManager();
        final SharedPreferences sharedPreferences = getSharedPreferences(mActivity);
        preferenceManager.setSharedPreferencesName(SHARED_PREFS_NAME);
        
        mContentResolver = mActivity.getContentResolver();
        
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.general_preferences);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        // added by amt_sunli 2012-11-29 SWITCHUITWO-159 begin
        if (preferenceScreen != null) {
            mPreferencesShowLunar = (CheckBoxPreference)preferenceScreen.findPreference(KEY_SHOW_LUNAR);
        }
        // added by amt_sunli 2012-11-29 SWITCHUITWO-159 end

        /*
         * If Locale is Chinese, show the Lunar & Holiday preference checkbox,
         * otherwise, remove the two preference items.
         */
        if (!Utils.isDefaultLocaleChinese() && preferenceScreen != null) {
            PreferenceCategory generalSettings = (PreferenceCategory) preferenceScreen
                    .findPreference("pref_key_general_settings");
            Preference showHoliday = findPreference(KEY_SHOW_HOLIDAY);
            Preference showLunar = findPreference(KEY_SHOW_LUNAR);
            if (generalSettings != null && showHoliday != null) {
                //Only Lunar checkbox for Korea
                generalSettings.removePreference(showHoliday);
                if (!Utils.isLocaleKorean() && showLunar != null) {
                    generalSettings.removePreference(showLunar);
                    preferenceScreen.removePreference(generalSettings);
                    // added by amt_sunli 2012-11-29 SWITCHUITWO-159 begin
                    mPreferencesShowLunar = null;
                    // added by amt_sunli 2012-11-29 SWITCHUITWO-159 end
                }
            }
        }

        if (Utils.getConfigBool(mActivity, R.bool.tablet_config)) {
            PreferenceCategory generalConference = (PreferenceCategory) preferenceScreen
                      .findPreference("pref_key_general_conference");
            if (generalConference != null) {
                preferenceScreen.removePreference(generalConference);
            }

        }
        
        mAlert = (CheckBoxPreference) preferenceScreen.findPreference(KEY_ALERTS);
        mVibrateWhen = (ListPreference) preferenceScreen.findPreference(KEY_ALERTS_VIBRATE_WHEN);
        Vibrator vibrator = (Vibrator) mActivity.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            PreferenceCategory mAlertGroup = (PreferenceCategory) preferenceScreen
                    .findPreference(KEY_ALERTS_CATEGORY);
            mAlertGroup.removePreference(mVibrateWhen);
        } else {
            mVibrateWhen.setSummary(mVibrateWhen.getEntry());
        }

        mRingtone = (RingtonePreference) preferenceScreen.findPreference(KEY_ALERTS_RINGTONE);
        mPopup = (CheckBoxPreference) preferenceScreen.findPreference(KEY_ALERTS_POPUP);
        mUseHomeTZ = (CheckBoxPreference) preferenceScreen.findPreference(KEY_HOME_TZ_ENABLED);
        mHideDeclined = (CheckBoxPreference) preferenceScreen.findPreference(KEY_HIDE_DECLINED);
        mWeekStart = (ListPreference) preferenceScreen.findPreference(KEY_WEEK_START_DAY);
        mDefaultReminder = (ListPreference) preferenceScreen.findPreference(KEY_DEFAULT_REMINDER);
        mHomeTZ = (ListPreference) preferenceScreen.findPreference(KEY_HOME_TZ);
        String tz = mHomeTZ.getValue();
		mDefaultCal = (DefaultCalendarDialogPreference) preferenceScreen.findPreference(KEY_DEFAULT_CALENDAR);
        mWeekStart.setSummary(mWeekStart.getEntry());
        mDefaultReminder.setSummary(mDefaultReminder.getEntry());
        // Begin motorola
        mDefaultCalView = (ListPreference) preferenceScreen.findPreference(KEY_DEFAULT_CALENDAR_VIEW);
        // End motorola
//        if (mTimezones == null) {
//            mTimezones = (new TimezoneAdapter(mActivity, tz, System.currentTimeMillis()))
//                    .getAllTimezones();
//        }
//        mHomeTZ.setEntryValues(mTimezones[0]);
//        mHomeTZ.setEntries(mTimezones[1]);
        //Begin Motorola
        if (TimezoneAdapter.timezonesEmpty()) {
            TimezoneAdapter.initTimezones(mActivity, tz);
        }
        CharSequence[][] timezonesArray = TimezoneAdapter.getAllTimezones();
        mHomeTZ.setEntryValues(timezonesArray[0]);
        mHomeTZ.setEntries(timezonesArray[1]);
        //End Motorola
        CharSequence tzName = mHomeTZ.getEntry();
        if (TextUtils.isEmpty(tzName)) {
            tzName = Utils.getTimeZone(mActivity, null);
        }
        mHomeTZ.setSummary(tzName);

        migrateOldPreferences(sharedPreferences);
        // Begin motorola
        CharSequence defaultCal = mDefaultCalView.getEntry();
        if (!TextUtils.isEmpty(defaultCal)) {
          mDefaultCalView.setSummary(defaultCal);
        }
        // End motorola
        updateChildPreferences();
        updateDefaultCalendarSettings(sharedPreferences);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        setPreferenceListeners(this);
		mContentResolver.registerContentObserver(Calendars.CONTENT_URI, true, mObserver);
    }

    /**
     * Sets up all the preference change listeners to use the specified
     * listener.
     */
    private void setPreferenceListeners(OnPreferenceChangeListener listener) {
        mUseHomeTZ.setOnPreferenceChangeListener(listener);
        mHomeTZ.setOnPreferenceChangeListener(listener);
        mWeekStart.setOnPreferenceChangeListener(listener);
        mDefaultReminder.setOnPreferenceChangeListener(listener);
        mRingtone.setOnPreferenceChangeListener(listener);
        mHideDeclined.setOnPreferenceChangeListener(listener);
        mVibrateWhen.setOnPreferenceChangeListener(listener);
        // added by amt_sunli 2012-11-29 SWITCHUITWO-159 begin
        if (mPreferencesShowLunar != null) {
            // added by amt_sunli 2012-11-28 SWITCHUITWO-148 begin
            mPreferencesShowLunar.setOnPreferenceChangeListener(listener);
            // added by amt_sunli 2012-11-28 SWITCHUITWO-148 end
        }
        // added by amt_sunli 2012-11-28 SWITCHUITWO-159 end
        // Begin motorla
        mDefaultCalView.setOnPreferenceChangeListener(listener);
        // End motorola
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        mContentResolver.unregisterContentObserver(mObserver);
        setPreferenceListeners(null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Activity a = getActivity();
        if (key.equals(KEY_ALERTS)) {
            updateChildPreferences();
            if (a != null) {
                Intent intent = new Intent();
                intent.setClass(a, AlertReceiver.class);
                if (mAlert.isChecked()) {
                    intent.setAction(AlertReceiver.ACTION_DISMISS_OLD_REMINDERS);
                } else {
                    intent.setAction(CalendarContract.ACTION_EVENT_REMINDER);
                }
                a.sendBroadcast(intent);
            }
        } else if (key.equals(KEY_DEFAULT_CALENDAR_ID)) {
            updateDefaultCalendarSettings(sharedPreferences);
        }

        if (a != null) {
            BackupManager.dataChanged(a.getPackageName());
        }
    }

    /**
     * Handles time zone preference changes
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String tz;
        if (preference == mUseHomeTZ) {
            if ((Boolean)newValue) {
                tz = mHomeTZ.getValue();
            } else {
                tz = CalendarCache.TIMEZONE_TYPE_AUTO;
            }
            Utils.setTimeZone(getActivity(), tz);
            return true;
        } else if (preference == mHideDeclined) {
            mHideDeclined.setChecked((Boolean) newValue);
            Activity act = getActivity();
            Intent intent = new Intent(Utils.getWidgetScheduledUpdateAction(act));
            intent.setDataAndType(CalendarContract.CONTENT_URI, Utils.APPWIDGET_DATA_TYPE);
            act.sendBroadcast(intent);
            return true;
        } else if (preference == mHomeTZ) {
            tz = (String) newValue;
            // We set the value here so we can read back the entry
            mHomeTZ.setValue(tz);
            mHomeTZ.setSummary(mHomeTZ.getEntry());
            Utils.setTimeZone(getActivity(), tz);
        } else if (preference == mWeekStart) {
            mWeekStart.setValue((String) newValue);
            mWeekStart.setSummary(mWeekStart.getEntry());
        } else if (preference == mDefaultReminder) {
            mDefaultReminder.setValue((String) newValue);
            mDefaultReminder.setSummary(mDefaultReminder.getEntry());
        } else if (preference == mRingtone) {
            // TODO update this after b/3417832 is fixed
            return true;
        } else if (preference == mVibrateWhen) {
            mVibrateWhen.setValue((String)newValue);
            mVibrateWhen.setSummary(mVibrateWhen.getEntry());
        // added by amt_sunli 2012-11-28 SWITCHUITWO-148 begin
        } else if (preference == mPreferencesShowLunar) {
            mPreferencesShowLunar.setChecked((Boolean) newValue);
            Activity mActivity = getActivity();
            Intent mIntent = new Intent("com.motorola.lunarchanged");
            mActivity.sendBroadcast(mIntent);
        // added by amt_sunli 2012-11-28 SWITCHUITWO-148 end
        // Begin motorla
        } else if (preference == mDefaultCalView) {
            //update the default view value
            mDefaultCalView.setValue((String) newValue);
            mDefaultCalView.setSummary(mDefaultCalView.getEntry());
        // End motorola
        } else {
            return true;
        }
        return false;
    }

    /**
     * If necessary, upgrades previous versions of preferences to the current
     * set of keys and values.
     * @param prefs the preferences to upgrade
     */
    private void migrateOldPreferences(SharedPreferences prefs) {
        // If needed, migrate vibration setting from a previous version
        if (!prefs.contains(KEY_ALERTS_VIBRATE_WHEN) &&
                prefs.contains(KEY_ALERTS_VIBRATE)) {
            int stringId = prefs.getBoolean(KEY_ALERTS_VIBRATE, false) ?
                    R.string.prefDefault_alerts_vibrate_true :
                    R.string.prefDefault_alerts_vibrate_false;
            mVibrateWhen.setValue(getActivity().getString(stringId));
        }
        // If needed, migrate the old alerts type settin
        if (!prefs.contains(KEY_ALERTS) && prefs.contains(KEY_ALERTS_TYPE)) {
            String type = prefs.getString(KEY_ALERTS_TYPE, ALERT_TYPE_STATUS_BAR);
            if (type.equals(ALERT_TYPE_OFF)) {
                mAlert.setChecked(false);
                mPopup.setChecked(false);
                mPopup.setEnabled(false);
            } else if (type.equals(ALERT_TYPE_STATUS_BAR)) {
                mAlert.setChecked(true);
                mPopup.setChecked(false);
                mPopup.setEnabled(true);
            } else if (type.equals(ALERT_TYPE_ALERTS)) {
                mAlert.setChecked(true);
                mPopup.setChecked(true);
                mPopup.setEnabled(true);
            }
            // clear out the old setting
            prefs.edit().remove(KEY_ALERTS_TYPE).commit();
        }
        
        //Motorola Begin
        // If needed, migrate default view setting from a previous version
        if (prefs.contains(KEY_DEFAULT_CALENDAR_VIEW_ID)
                && prefs.contains(KEY_DEFAULT_CALENDAR_VIEW_OLD)) {
            int defaultCalendarId = prefs.getInt(KEY_DEFAULT_CALENDAR_VIEW_ID, 0);
            Log.d(TAG, "defaultCalendarId: " + defaultCalendarId);
            mDefaultCalView.setValueIndex(defaultCalendarId);
            prefs.edit().remove(KEY_DEFAULT_CALENDAR_VIEW_ID).commit();
            prefs.edit().remove(KEY_DEFAULT_CALENDAR_VIEW_OLD).commit();
        }
        //Motorola End
        //Motorola Begin
        // If needed, migrate default view setting from a previous version
        if (prefs.contains(KEY_DEFAULT_CALENDAR_VIEW_ID)
                && prefs.contains(KEY_DEFAULT_CALENDAR_VIEW_OLD)) {
            int defaultCalendarId = prefs.getInt(KEY_DEFAULT_CALENDAR_VIEW_ID, 0);
            Log.d(TAG, "defaultCalendarId: " + defaultCalendarId);
            mDefaultCalView.setValueIndex(defaultCalendarId);
            prefs.edit().remove(KEY_DEFAULT_CALENDAR_VIEW_ID).commit();
            prefs.edit().remove(KEY_DEFAULT_CALENDAR_VIEW_OLD).commit();
        }
        //Motorola End
    }

    /**
     * Keeps the dependent settings in sync with the parent preference, so for
     * example, when notifications are turned off, we disable the preferences
     * for configuring the exact notification behavior.
     */
    private void updateChildPreferences() {
        if (mAlert.isChecked()) {
            mVibrateWhen.setEnabled(true);
            mRingtone.setEnabled(true);
            mPopup.setEnabled(true);
        } else {
            mVibrateWhen.setValue(
                    getActivity().getString(R.string.prefDefault_alerts_vibrate_false));
            mVibrateWhen.setEnabled(false);
            mRingtone.setEnabled(false);
            mPopup.setEnabled(false);
        }
    }


    @Override
    public boolean onPreferenceTreeClick(
            PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();
        if (KEY_CLEAR_SEARCH_HISTORY.equals(key)) {
            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
                    Utils.getSearchAuthority(getActivity()),
                    CalendarRecentSuggestionsProvider.MODE);
            suggestions.clearHistory();
            Toast.makeText(getActivity(), R.string.search_history_cleared,
                    Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }
    
    //Begin Motorola
    /**
     * Get default calendar value.
     */
    public static CalendarAccountItem getDefaultCalendar(Context context) {
        SharedPreferences prefs = getSharedPreferences(context);
        //String "No default calendar"
        String none = context.getString(R.string.no_default_calendars_summary);
        String noneLabel = context.getString(R.string.default_calendar_none_string);
        String account = prefs.getString(
                KEY_DEFAULT_CALENDAR_ACCOUNT, none);//sync_account
        String calType = prefs
                .getString(KEY_DEFAULT_CALENDAR_TYPE, noneLabel);// _sync_acount_type
        int calId = prefs.getInt(KEY_DEFAULT_CALENDAR_ID,
                DefaultCalendarDialogPreference.NONE_DEFAULT_CALENDAR);//_id

        if (calId == DefaultCalendarDialogPreference.NONE_DEFAULT_CALENDAR) {// No default calendar.
            return null;
        }
        // Find it if the account saved is valid.
        CalendarAccountItem temp = DefaultCalendarDialogPreference
                .getCalAccount(context, account, calType, calId);
        // if invalid, get the first valid account.
        if (temp == null) {
            temp = DefaultCalendarDialogPreference
                    .getFirstValidCalAccount(context);
        }
        return temp;
    }
    /**
     * Set default calendar value sync with user input
     * and calendars account status.
     */
    private void updateDefaultCalendarSettings(SharedPreferences sharedPrefs) {
        CalendarAccountItem calAccount = null;
        if (mDefaultCal == null || sharedPrefs == null) {
            return;
        }
        SharedPreferences.Editor editor = sharedPrefs.edit();
        String noDefaultCal = mActivity.getString(R.string.default_calendar_none_string);

        calAccount = getDefaultCalendar(mActivity);

        if (calAccount != null) {// find valid default calendar.
            Log.d(TAG, "calAccount: " + calAccount.account + " id: "
                    + calAccount.id);
            mDefaultCal.setSummary(calAccount.account);
            setDefaultCalendarPreferenceValue(editor, calAccount.id,
                        calAccount.account, calAccount.type);
        } else {//No default calendar.
            mDefaultCal.setSummary(noDefaultCal);
        }
    }
    /**
     * Set default calendar value sync with user input
     */
    public static void setDefaultCalendarPreferenceValue(
            SharedPreferences.Editor editor, int id, String account, String type) {
        if (editor == null)
            return;
        editor.putInt(KEY_DEFAULT_CALENDAR_ID, id);
        editor.putString(
                KEY_DEFAULT_CALENDAR_ACCOUNT,
                account);
        editor.putString(KEY_DEFAULT_CALENDAR_TYPE,
                type);
        editor.apply();
    }

    private class updateConfFlagInBackground extends AsyncTask<SharedPreferences, Integer, Integer> {
        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Integer doInBackground(SharedPreferences... params) {
            if (params.length != 1) {
                return null;
            }
            SharedPreferences sharedPrefs = params[0];
            if (sharedPrefs == null) {
                return null;
            }
            Uri uri = Uri.parse("content://" + CalendarContract.AUTHORITY + "/conference_call_info/flag");
            ContentResolver cr = getActivity().getContentResolver();
            ContentValues updateValues = new ContentValues();
            boolean isEnabled = sharedPrefs.getBoolean(
                    KEY_ENABLE_QUICK_CONFERENCE_DIALING, false/*default true*/);
            updateValues.put("MyConferenceFlag",isEnabled);
            cr.update(uri, updateValues, null, null);
            return null;
        }
    }

    private void updateConferenceCallSettings(SharedPreferences sharedPrefs) {
        PreferenceScreen prefScreen = getPreferenceScreen();
        if (prefScreen == null) return;

        Preference prefConfCallInfo = prefScreen.findPreference(KEY_CONFERENCE_CALL_INFO);
        if (prefConfCallInfo != null) {
            boolean isEnabled = sharedPrefs.getBoolean(
                    KEY_ENABLE_QUICK_CONFERENCE_DIALING, false/*default true*/);
            prefConfCallInfo.setEnabled(isEnabled);

            // Conference number is a mandatory field. If it's null or empty, we must have the user to
            // enter conference call info.
            String telNumber = sharedPrefs.getString(KEY_CONFERENCE_NUMBER, null);
            if ((telNumber != null) && (telNumber.length() > 0)) {
                StringBuilder strBuilder = new StringBuilder();
                strBuilder.append(telNumber);
                String confId = sharedPrefs.getString(KEY_CONFERENCE_ID, null);
                if ((confId != null) && (confId.length() > 0)) {
                    strBuilder.append("; " + confId);
                }
                prefConfCallInfo.setSummary(strBuilder.toString());
            } else {
                prefConfCallInfo.setSummary(R.string.preference_conference_call_info_summary);
            }
        }
    }
    //End Motorola

}
