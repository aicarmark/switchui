/*
 * @(#)CalendarActivity.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * wkh346        2011/08/22 NA                Initial version
 *
 */

package com.motorola.contextual.smartprofile.sensors.calendareventsensor;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.provider.CalendarContract.Instances;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.app.ActionBar;
import android.app.Fragment;
import android.view.MenuItem;

import com.motorola.contextual.pickers.conditions.calendar.AgendaWindowAdapter;
import com.motorola.contextual.pickers.conditions.calendar.CalendarDialogListAdapter;
import com.motorola.contextual.pickers.conditions.calendar.CalendarDialogObserver;
import com.motorola.contextual.pickers.conditions.calendar.CalendarEventDatabase;
import com.motorola.contextual.pickers.conditions.calendar.CalendarEventSensorConstants;
import com.motorola.contextual.pickers.conditions.calendar.CalendarEventUtils;
import com.motorola.contextual.pickers.conditions.calendar.CalendarPreference;
import com.motorola.contextual.pickers.conditions.calendar.AgendaWindowAdapter.CurrentItem;
import com.motorola.contextual.pickers.conditions.calendar.CalendarEventUtils.ConfigData;
import com.motorola.contextual.smartrules.fragment.EditFragment;

import com.motorola.contextual.smartrules.R;

/**
 * This class is responsible for displaying the list view and saving the user
 * selection in calendar events database.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Extends Activity
 *      Implements View.OnClickListener
 *      Implements CalendarEventSensorConstants
 *
 * RESPONSIBILITIES:
 * This class is responsible for displaying the list view and saving the user selection
 * in calendar events database.
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class CalendarActivity extends PreferenceActivity implements
    View.OnClickListener, CalendarEventSensorConstants,
    CalendarDialogObserver {

    /**
     * Id of the calendar events dialog
     */
    private static final int DIALOG_ID_EVENTS = 1;

    /**
     * TAG for logging the messages
     */
    private static final String TAG = "CalendarActivity";

    /**
     * Constant value for first visible time
     */
    private static final long FIRST_VISIBLE_TIME = 0;

    /**
     * Message code for indicating that set result has been called
     */
    private static final int MSG_SET_RESULT_COMPLETE = 0;

    /**
     * Reference to the ArrayList of IDs of the selected calendars
     */
    private ArrayList<Integer> mCalendarIds;

    /**
     * Reference to the adapter of {@link mSelectedCalendarsDialog}
     */
    private CalendarDialogListAdapter mDialogListAdapter;

    /**
     * ArrayList of preselected calendar IDs obtained from edit URI
     */
    private ArrayList<Integer> mPreSelectedCalendarIds;

    /**
     * Boolean to identify whether activity is opened in edit mode
     */
    private boolean mEditMode = false;

    /**
     * Reference to the all day events check box
     */
    private CheckBox mAllDayEventsCheckbox;

    /**
     * Reference to the multiple participants events check box
     */
    private CheckBox mMultipleParticipantsEventsCheckbox;

    /**
     * Reference to accepted events check box
     */
    private CheckBox mAcceptedEventsCheckbox;

    /**
     * This will be true when onSaveInstanceState is called
     */
    private boolean mDisableActionBar = false;

    /**
     * Handler for handling the messages posted from secondary threads
     */
    private Handler mHandler;

    /**
     * Adapter for the calendar events dialog
     */
    private AgendaWindowAdapter mAgendaWindowAdapter;

    /**
     * Boolean to identify if calendar events dialog has been created
     */
    private boolean mCalendarEventsDialogCreated = false;

    /**
     * Reference to {@link CalendarPreference} object
     */
    private CalendarPreference mCalendarPreference;

    /**
     * Boolean for indicating whether set result has been initiated
     */
    private boolean mPreparingAndSendingResult = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar_main);
        addPreferencesFromResource(R.xml.calendar_preference);
        Button calendarEventsButton = (Button) findViewById(R.id.calendar_main_events_button);
        calendarEventsButton.setOnClickListener(this);
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.calendareventsensor);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();
        setupActionBarItemsVisibility(true);
        mAllDayEventsCheckbox = (CheckBox) findViewById(R.id.calendar_checkbox_allday_events);
        mMultipleParticipantsEventsCheckbox = (CheckBox) findViewById(R.id.calendar_checkbox_multiple_participants_events);
        mAcceptedEventsCheckbox = (CheckBox) findViewById(R.id.calendar_checkbox_accepted_events);
        LinearLayout allDayEventsLayout = (LinearLayout) findViewById(R.id.calendar_layout_allday_events);
        LinearLayout multipleParticipantsEventsLayout = (LinearLayout) findViewById(R.id.calendar_layout_multiple_participants_events);
        LinearLayout acceptedEventsLayout = (LinearLayout) findViewById(R.id.calendar_layout_accepted_events);
        if (mAllDayEventsCheckbox != null
                && mMultipleParticipantsEventsCheckbox != null
                && mAcceptedEventsCheckbox != null
                && allDayEventsLayout != null
                && multipleParticipantsEventsLayout != null
                && acceptedEventsLayout != null) {
            mAgendaWindowAdapter = new AgendaWindowAdapter(this);
            allDayEventsLayout.setOnClickListener(this);
            multipleParticipantsEventsLayout.setOnClickListener(this);
            acceptedEventsLayout.setOnClickListener(this);
            mAllDayEventsCheckbox.setChecked(false);
            mMultipleParticipantsEventsCheckbox.setChecked(false);
            mAcceptedEventsCheckbox.setChecked(false);
            mAllDayEventsCheckbox.setOnClickListener(this);
            mMultipleParticipantsEventsCheckbox.setOnClickListener(this);
            mAcceptedEventsCheckbox.setOnClickListener(this);
            initialize();
        } else {
            Log.e(TAG, "onCreate widget missing in layout xml file");
        }
        mHandler = new CalendarEventsHandler();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDisableActionBar = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDisableActionBar = false;
        if (mDialogListAdapter.isInitialized()) {
            if (LOG_INFO) {
                Log.i(TAG,
                        "onResume calling reinitialize on calendars dialog adapter");
            }
            mDialogListAdapter.reinitialize(this, mPreSelectedCalendarIds);
        }
    }

    /**
     * onOptionsItemSelected() handles the back press of icon in the ICS action
     * bar.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            result = true;
            break;
        case R.id.edit_save:
            if (!mPreparingAndSendingResult) {
                mPreparingAndSendingResult = true;
                Thread workerThread = new Thread(new Runnable() {

                    public void run() {
                        sendResult(RESULT_OK);
                        Message msg = new Message();
                        msg.what = MSG_SET_RESULT_COMPLETE;
                        mHandler.sendMessage(msg);
                    }
                });
                workerThread.start();
            }
            result = true;
            break;
        case R.id.edit_cancel:
            if (!mPreparingAndSendingResult) {
                sendResult(RESULT_CANCELED);
            }
            result = true;
            finish();
            break;
        }
        return result;
    }

    /**
     * Method for enabling and disabling the save button in ActionBar
     *
     * @param enableSaveButton
     *            - true if save button has to be enabled, false otherwise
     */
    protected void setupActionBarItemsVisibility(boolean enableSaveButton) {
        if (mDisableActionBar) {
            return;
        }
        int editFragmentOption = EditFragment.EditFragmentOptions.DEFAULT;
        if (enableSaveButton) {
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_ENABLED;
        } else {
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_DISABLED;
        }
        // Add menu items from fragment
        Fragment fragment = EditFragment.newInstance(editFragmentOption, false);
        getFragmentManager().beginTransaction()
                .replace(R.id.edit_fragment_container, fragment, null).commit();
    }

    /**
     * The handler class that is used by worker threads for posting the messages
     * to the main thread
     *
     * <CODE><PRE>
     *
     * CLASS:
     *      Extends Handler
     *
     * RESPONSIBILITIES:
     * This class is responsible for handling the messages posted by worker threads
     *
     * COLABORATORS:
     *     SmartProfile - Implements the preconditions available across the system
     *
     * USAGE:
     *     See each method.
     *
     * </PRE></CODE>
     */
    private class CalendarEventsHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_SET_RESULT_COMPLETE: {
                mPreparingAndSendingResult = false;
                finish();
                break;
            }
            default: {
                super.handleMessage(msg);
            }
            }
        }

    }

    /**
     * Returns String containing IDs of selected calendars separated by "OR"
     *
     * @return String containing IDs of selected calendars separated by "OR"
     */
    private final String getCalendarIdsString() {
        ArrayList<Integer> selectedCalendarIds;
        if (mPreSelectedCalendarIds != null && mEditMode) {
            selectedCalendarIds = mPreSelectedCalendarIds;
        } else {
            selectedCalendarIds = mCalendarIds;
        }
        String calendarIds = CalendarEventUtils
                .getStringOfCalendarIdsFromList(selectedCalendarIds);
        if (LOG_DEBUG) {
            Log.d(TAG, "getCalendarIdsString calendarIds = " + calendarIds);
        }
        return calendarIds;
    }

    /**
     * Initialize the UI by extracting data from the edit URI
     */
    private void initialize() {
        Intent intent = getIntent();
        String configString = intent.getStringExtra(EXTRA_CONFIG);
        if (configString != null && !configString.isEmpty()) {
            mEditMode = true;
            ConfigData configData = CalendarEventUtils
                    .getConfigData(configString);
            if (configData != null) {
                mPreSelectedCalendarIds = CalendarEventUtils
                        .getListOfCalendarIdsFromString(configData.mCalendarIds);
                mAllDayEventsCheckbox
                        .setChecked(configData.mExcludeAllDayEvents == CalendarEventDatabase.SELECTED);
                mMultipleParticipantsEventsCheckbox
                        .setChecked(configData.mOnlyIncludeEventsWithMultiplePeople == CalendarEventDatabase.SELECTED);
                mAcceptedEventsCheckbox
                        .setChecked(configData.mOnlyIncludeAcceptedEvents == CalendarEventDatabase.SELECTED);
            }
            if (LOG_INFO) {
                Log.i(TAG, "initialize configString = " + configString);
            }
        }
        if (mPreSelectedCalendarIds != null) {
            mDialogListAdapter = new CalendarDialogListAdapter(this, this,
                    mPreSelectedCalendarIds);
        } else {
            mDialogListAdapter = new CalendarDialogListAdapter(this, this, null);
        }
        mCalendarPreference = (CalendarPreference) findPreference(getString(R.string.calendar_main_preference));
        if (mCalendarPreference != null && mDialogListAdapter != null) {
            mCalendarPreference.initialize(mDialogListAdapter, this);
            mCalendarPreference.setTitle(getString(R.string.calendars));
        } else {
            if (mDialogListAdapter == null) {
                Log.e(TAG, "initialize failed to create mDialogListAdapter");
            }
            if (mCalendarPreference == null) {
                Log.e(TAG, "initialize mCalendarPreference is null");
            }
        }
        buildQuerySelection();
    }


    public void onClick(View view) {
        if (mPreparingAndSendingResult) {
            return;
        }
        switch (view.getId()) {
        case R.id.calendar_main_events_button: {
            if (mCalendarEventsDialogCreated) {
                buildQuerySelection();
                refresh(FIRST_VISIBLE_TIME, true);
            }
            showDialog(DIALOG_ID_EVENTS);
            break;
        }
        case R.id.calendar_layout_allday_events: {
            mAllDayEventsCheckbox
            .setChecked(!mAllDayEventsCheckbox.isChecked());
            break;
        }
        case R.id.calendar_layout_multiple_participants_events: {
            mMultipleParticipantsEventsCheckbox
            .setChecked(!mMultipleParticipantsEventsCheckbox
                        .isChecked());
            break;
        }
        case R.id.calendar_layout_accepted_events: {
            mAcceptedEventsCheckbox.setChecked(!mAcceptedEventsCheckbox
                                               .isChecked());
            break;
        }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_ID_EVENTS) {
            AlertDialog dialog = null;
            if (mAgendaWindowAdapter != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.calendar_events_matching_search));
                builder.setPositiveButton(getString(R.string.ok),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog,
                    int which) {
                        refresh(FIRST_VISIBLE_TIME, false);
                        dialog.dismiss();
                    }
                });
                builder.setAdapter(mAgendaWindowAdapter,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog,
                    int which) {
                        // nothing to be done here
                    }
                });
                builder.setOnKeyListener(new DialogInterface.OnKeyListener() {

                    public boolean onKey(DialogInterface dialog, int keyCode,
                    KeyEvent event) {
                        switch (keyCode) {
                        case KeyEvent.KEYCODE_BACK: {
                            refresh(FIRST_VISIBLE_TIME, false);
                            mAgendaWindowAdapter.onDialogDismiss();
                            dialog.dismiss();
                            return true;
                        }
                        case KeyEvent.KEYCODE_SEARCH: {
                            return true;
                        }
                        case KeyEvent.KEYCODE_ENTER:
                        case KeyEvent.KEYCODE_DPAD_CENTER: {
                            if (event.getAction() == KeyEvent.ACTION_UP) {
                                if (mAgendaWindowAdapter
                                .getCurrentSelectedItemType() == CurrentItem.POSITIVE_BUTTON_ITEM) {
                                    refresh(FIRST_VISIBLE_TIME, false);
                                    mAgendaWindowAdapter.onDialogDismiss();
                                    dialog.dismiss();
                                } else {
                                    mAgendaWindowAdapter.onKeyPressed(keyCode,
                                                                      event);
                                }
                            }
                            return true;
                        }
                        case KeyEvent.KEYCODE_DPAD_UP:
                        case KeyEvent.KEYCODE_DPAD_DOWN: {
                            if (event.getAction() == KeyEvent.ACTION_UP) {
                                mAgendaWindowAdapter.onKeyPressed(keyCode,
                                                                  event);
                            }
                            return false;
                        }
                        }
                        return false;
                    }
                });
                builder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    public void onItemSelected(AdapterView<?> adapterView,
                    View view, int position, long rowId) {
                        mAgendaWindowAdapter.onItemSelected(adapterView, view,
                                                            position, rowId);
                    }

                    public void onNothingSelected(AdapterView<?> adapterView) {
                        mAgendaWindowAdapter.onItemSelected(adapterView, null,
                                                            -1, -1);
                    }
                });
                dialog = builder.create();
                mAgendaWindowAdapter.initialize(dialog.getListView());
                mCalendarEventsDialogCreated = true;
                buildQuerySelection();
                refresh(FIRST_VISIBLE_TIME, true);
            }
            return dialog;

        }
        return null;
    }

    /**
     * Calling this will refresh the list view and will set the selection to the
     * event whose start time matches with beginTime. If beginTime is less than
     * or equal to zero then selection is set to the first visible event or
     * current time
     *
     * @param beginTime
     *            - The start time of an event. Pass {@link FIRST_VISIBLE_TIME}
     *            if you want to set the selection to first visible time or
     *            current time
     * @param forced
     *            - if true, new query is performed by
     *            {@link AgendaWindowAdapter}, if false, only set selection is
     *            called by {@link AgendaWindowAdapter}
     */
    public void refresh(long beginTime, boolean forced) {
        if (LOG_DEBUG) {
            Log.d(TAG, "refresh beginTime = " + beginTime);
        }
        Time time = new Time(Time.getCurrentTimezone());
        if (beginTime <= 0) {
            time.set(System.currentTimeMillis());
        } else {
            time.set(beginTime);
        }
        mAgendaWindowAdapter.refresh(time, forced);
    }

    /**
     * Builds the selection String for performing the SQL query on Instances
     * table
     */
    private void buildQuerySelection() {
        StringBuilder builder = new StringBuilder();
        ArrayList<Integer> calendarIds = null;
        if (mCalendarIds != null) {
            calendarIds = mCalendarIds;
        } else if (mPreSelectedCalendarIds != null) {
            calendarIds = mPreSelectedCalendarIds;
        }
        if (calendarIds != null) {
            String visibleCalendarsSelection = Instances.VISIBLE + EQUALS_TO
                                               + INVERTED_COMMA + VISIBLE_CALENDARS_ONLY + INVERTED_COMMA;
            if (calendarIds
                    .contains(CalendarDialogListAdapter.ALL_CALENDARS_ID)) {
                builder.append(visibleCalendarsSelection);
            } else if (calendarIds.size() <= 0) {
                String nullCalendarsSelection = Instances.VISIBLE + EQUALS_TO
                                                + INVERTED_COMMA + CALENDARS_NONE + INVERTED_COMMA;
                builder.append(nullCalendarsSelection);
            } else if (calendarIds.size() == 1) {
                builder.append(visibleCalendarsSelection);
                String calendarIdSelection = AND + Instances.CALENDAR_ID
                                             + EQUALS_TO + INVERTED_COMMA + calendarIds.get(0)
                                             + INVERTED_COMMA;
                builder.append(calendarIdSelection);
            } else {
                builder.append(visibleCalendarsSelection);
                builder.append(AND + LEFT_PAREN);
                int i;
                int size = calendarIds.size();
                for (i = 0; i < size - 1; i++) {
                    String calendarIdSelection = Instances.CALENDAR_ID
                                                 + EQUALS_TO + INVERTED_COMMA + calendarIds.get(i)
                                                 + INVERTED_COMMA + OR;
                    builder.append(calendarIdSelection);
                }
                String calendarIdSelection = Instances.CALENDAR_ID + EQUALS_TO
                                             + INVERTED_COMMA + calendarIds.get(i) + INVERTED_COMMA
                                             + RIGHT_PAREN;
                builder.append(calendarIdSelection);
            }
        } else {
            String visibleCalendarsSelection = Instances.VISIBLE + EQUALS_TO
                                               + INVERTED_COMMA + VISIBLE_CALENDARS_ONLY + INVERTED_COMMA;
            builder.append(visibleCalendarsSelection);
        }
        if (mAllDayEventsCheckbox.isChecked()) {
            String allDayEventsSelection = Instances.ALL_DAY + EQUALS_TO
                                           + INVERTED_COMMA + NOT_ALL_DAY_EVENT + INVERTED_COMMA;
            if (builder.length() > 0) {
                builder.append(AND).append(allDayEventsSelection);
            } else {
                builder.append(allDayEventsSelection);
            }
        }
        if (mAcceptedEventsCheckbox.isChecked()) {
            String acceptedEventsSelection = Instances.SELF_ATTENDEE_STATUS
                                             + EQUALS_TO + INVERTED_COMMA + Instances.STATUS_CONFIRMED
                                             + INVERTED_COMMA;
            if (builder.length() > 0) {
                builder.append(AND).append(acceptedEventsSelection);
            } else {
                builder.append(acceptedEventsSelection);
            }
        }
        mAgendaWindowAdapter
        .setMultipleParticipants(mMultipleParticipantsEventsCheckbox
                                 .isChecked());
        if (LOG_DEBUG) {
            Log.d(TAG,
                  "buildQuerySelection query selection = "
                  + builder.toString());
        }
        mAgendaWindowAdapter.setQuerySelection(builder.toString());
    }

    /**
     * Returns String containing display names of selected calendars
     *
     * @return String containing display names of selected calendars
     */
    private final String getListOfCalendarNames() {
        ArrayList<Integer> selectedCalendarIds;
        if (mPreSelectedCalendarIds != null && mEditMode) {
            selectedCalendarIds = mPreSelectedCalendarIds;
        } else {
            selectedCalendarIds = mCalendarIds;
        }
        return CalendarEventUtils.getDescription(this, selectedCalendarIds);
    }

    /**
     * Builds the Intent containing the data required for creating the rule.
     * This Intent is set as result for the calling activity
     *
     * @param resultCode
     *            - The result code to propagate back to the originating
     *            activity, often RESULT_CANCELED or RESULT_OK
     */
    private synchronized void sendResult(int resultCode) {
        Intent intent = new Intent();
        if (resultCode == RESULT_OK) {
            String configString = CalendarEventUtils.getConfigString(
                    getCalendarIdsString(), mAllDayEventsCheckbox.isChecked(),
                    mMultipleParticipantsEventsCheckbox.isChecked(),
                    mAcceptedEventsCheckbox.isChecked());
            if (LOG_INFO) {
                Log.i(TAG, "sendResult configString = " + configString);
            }
            intent.putExtra(EXTRA_CONFIG, configString);
            intent.putExtra(EXTRA_DESCRIPTION, getListOfCalendarNames());
        }
        setResult(resultCode, intent);
    }

    public void onPositiveButtonClickOnCalendarDialog() {
        if (mPreSelectedCalendarIds != null) {
            mPreSelectedCalendarIds.clear();
            mPreSelectedCalendarIds = null;
        }
        mCalendarIds = mDialogListAdapter.operationCompleted();
        mCalendarPreference.setSummary(mDialogListAdapter
                .getSelectedCalendarName(this));
        if (mCalendarIds != null && mCalendarIds.size() > 0) {
            setupActionBarItemsVisibility(true);
        } else {
            setupActionBarItemsVisibility(false);
        }
        if (mCalendarEventsDialogCreated) {
            buildQuerySelection();
            refresh(FIRST_VISIBLE_TIME, true);
        }
    }

    public void onNegativeButtonClickOnCalendarDialog() {
        mDialogListAdapter.dialogCancelled();
    }

    public void onAllItemsUnChecked() {
        mCalendarPreference.setPositiveButtonEnabled(false);

    }

    public void onItemChecked() {
        mCalendarPreference.setPositiveButtonEnabled(true);

    }

    public void onInitComplete() {
        if (mCalendarPreference != null) {
            if (mDialogListAdapter.getNumberOfSelectedCalendars() == 0) {
                setupActionBarItemsVisibility(false);
            } else {
                setupActionBarItemsVisibility(true);
            }
            mCalendarPreference.setSummary(mDialogListAdapter
                    .getSelectedCalendarName(this));
        }
    }

}
