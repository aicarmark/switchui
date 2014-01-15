/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number          Brief Description
 * ------------- ---------- -----------------  ------------------------------
 * BHT364        2012/07/03 Smart Actions 2.1  Initial Version
 */

package com.motorola.contextual.pickers.conditions.calendar;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.pickers.conditions.calendar.CalendarEventUtils.ConfigData;
import com.motorola.contextual.smartrules.R;

import java.util.List;

/**
 * The Activity that hosts the calendar picker.
 *
 * <code><pre>
 *
 * CLASS:
 *  extends MultiScreenPickerActivity
 *
 * RESPONSIBILITIES:
 * Hosts the fragments that implement the calendar picker.
 *
 * COLLABORATORS:
 *  Calendar - Manages information about available calendars
 *  CalendarActivity - the Activity hosting the calendar picker fragments
 *  CalendarEventConfigFragment - the fragment to configure the types of calendar events to use
 *  CalendarPickerFragment - the fragment to pick the specific calendar
 *  MatchingEventsDialog - the dialog that presents the matching events for the users to choose.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class CalendarActivity extends MultiScreenPickerActivity implements CalendarEventSensorConstants, 
                                                                           CalendarPickerFragment.CalendarPickerDelegate {

    private static final String TAG = CalendarActivity.class.getSimpleName();

    private int mRuleId = -1;
    private Intent mConfig;
    private List<Calendar> mAvailableCalendars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setActionBarTitle(getString(R.string.calendareventsensor_title));
        setTitle(R.string.calendar_picker_title);
        initializeConfig();

        showLoading(true);

        // Load the calendars from the content provider in the background
        // and then launch the appropriate fragment on the UI thread.
        new AsyncTask<Void, Void, List<Calendar>>() {
            @Override
            protected List<Calendar> doInBackground(Void... params) {
                return Calendar.getCalendars(CalendarActivity.this);
            }

            @Override
            protected void onPostExecute(List<Calendar> result) {
                mAvailableCalendars = result;
                launchInitialFragment();
            }
        }.execute();
    }

    private void showLoading(boolean loading) {
        // TODO: show busy spinner?
        
    }

    private void launchInitialFragment() {
        assert mAvailableCalendars != null;
        showLoading(false);
        if (mAvailableCalendars.size() > 1) {
            launchCalendarPicker(true);
        } else {
            // Only one calendar available, so default to "all calendars"
            // and skip right to the config picker
            mConfig.removeExtra(EXTRA_CALENDARS);
            launchEventConfig(true);
        }
    }
    
    public List<Calendar> getCalendars() {
        return mAvailableCalendars;
    }

    private void launchCalendarPicker(boolean first) {
        launchNextFragment(CalendarPickerFragment.newInstance(mConfig, null), R.string.calendar_picker_title, first);
    }

    private void launchEventConfig(boolean first) {
        launchNextFragment(CalendarEventConfigFragment.newInstance(mConfig, null), R.string.calendar_picker_title, first);
    }

    private void initializeConfig() {
        mConfig = (Intent)getIntent().clone();
        ConfigData configData;
        String configString = mConfig.getStringExtra(EXTRA_CONFIG);
        if (configString != null && !configString.isEmpty()) {
            configData = CalendarEventUtils.getConfigData(configString);
            if (configData != null ) {
               mConfig.putExtra(EXTRA_ALLDAY_EVENTS, configData.mExcludeAllDayEvents == CalendarEventDatabase.SELECTED);
               mConfig.putExtra(EXTRA_MULTIPLE_PARTICIPANTS, configData.mOnlyIncludeEventsWithMultiplePeople == CalendarEventDatabase.SELECTED);
               mConfig.putExtra(EXTRA_ACCEPTED_EVENTS, configData.mOnlyIncludeAcceptedEvents == CalendarEventDatabase.SELECTED);
            } else {
               // Need to figure out the logic for failure or the error log is sufficient.
               Log.e(TAG, "initialize configString = " + configString);
            }
        }
    }

    @Override
    public void onReturn(Object returnValue, PickerFragment fromFragment) {
        if (returnValue == null) {
            //Just go back to the previous fragment without committing any changes.
            if (getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
            } else {
                finish();
            }
        }

        if (fromFragment instanceof CalendarPickerFragment) {
            Intent result = (Intent)returnValue;
            mConfig.putExtra(EXTRA_CALENDARS, result.getStringExtra(EXTRA_CALENDARS));
            launchEventConfig(false);

        } else if (fromFragment instanceof CalendarEventConfigFragment) {
            Intent result = (Intent)returnValue;
            mConfig.putExtra(EXTRA_ALLDAY_EVENTS, result.getBooleanExtra(EXTRA_ALLDAY_EVENTS, false));
            mConfig.putExtra(EXTRA_MULTIPLE_PARTICIPANTS, result.getBooleanExtra(EXTRA_MULTIPLE_PARTICIPANTS, false));
            mConfig.putExtra(EXTRA_ACCEPTED_EVENTS, result.getBooleanExtra(EXTRA_ACCEPTED_EVENTS, false));

            saveAndReturnResult(mConfig);

        } else {
            Log.e(TAG, "onReturn() called from an unknown fragment: " + fromFragment);
            finish();
        }
    }

    private void saveAndReturnResult(final Intent config) {
        new AsyncTask<Void, Void, Intent>() {

            @Override
            protected Intent doInBackground(Void... params) {
                if (persistResult(config)) {
                    Intent result = new Intent();

                    String configString = CalendarEventUtils.getConfigString(
                            getCalendarIdsString(config),
                            isCalendarDBSelected(config, EXTRA_ALLDAY_EVENTS),
                            isCalendarDBSelected(config, EXTRA_MULTIPLE_PARTICIPANTS),
                            isCalendarDBSelected(config, EXTRA_ACCEPTED_EVENTS));
                    if (LOG_INFO) {
                        Log.i(TAG, "sendResult configString = " + configString);
                    }
                    result.putExtra(EXTRA_CONFIG, configString);
                    result.putExtra(EXTRA_DESCRIPTION, getListOfCalendarNames(config));

                    return result;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Intent result) {
                final int status = result != null ? RESULT_OK : RESULT_CANCELED;
                setResult(status, result);
                finish();
            }
        }.execute();
    }

    private boolean persistResult(Intent config) {
        CalendarEventDatabase database = CalendarEventDatabase.getInstance(this);
        if (database == null) {
            Log.e(TAG, "Unable to get instance of calendar events database");
            return false;
        }

        String calendarIds = getCalendarIdsString(config);
        int allDayEvents = calendarDBSelected(config, EXTRA_ALLDAY_EVENTS);
        int acceptedEvents = calendarDBSelected(config, EXTRA_ACCEPTED_EVENTS);
        int multipleParticipantsEvents = calendarDBSelected(config, EXTRA_MULTIPLE_PARTICIPANTS);

        mRuleId = database.insertAndUpdateRow(this, calendarIds, allDayEvents, acceptedEvents, multipleParticipantsEvents, false);
        return mRuleId != CalendarEventDatabase.INVALID_RULE_ID;
    }

    private int calendarDBSelected(Intent config, String extra) {
        return config.getBooleanExtra(extra, false) ? CalendarEventDatabase.SELECTED : CalendarEventDatabase.NOT_SELECTED;
    }

    private boolean isCalendarDBSelected(Intent config, String extra) {
        return config.getBooleanExtra(extra, false);
    }

    /**
     * Returns String containing display names of selected calendars
     *
     * @return String containing display names of selected calendars
     */
    private final String getListOfCalendarNames(Intent config) {
        return CalendarEventUtils.getDescription(this, getCalendarIdsString(config));
    }

    private String getCalendarIdsString(Intent config) {
        return config.getStringExtra(EXTRA_CALENDARS);
    }
}
