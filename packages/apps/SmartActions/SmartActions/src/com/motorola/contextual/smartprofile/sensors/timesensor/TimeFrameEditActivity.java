/*
 * @(#)TimeFrameEditActivity.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a15776       2011/02/21   NA               Incorporated review comments
 * a15776       2011/02/01   NA               Initial Version
 *
 */
package com.motorola.contextual.smartprofile.sensors.timesensor;

import android.R.id;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import android.app.ActionBar;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;

import com.motorola.contextual.smartrules.fragment.EditFragment;
import com.motorola.contextual.smartrules.R;

/**
 * This class presents the UI to the user to create or edit the time frames. Once the user saves
 * the changes, this module also updates the time frame database
 *
 * <code><pre>
 * CLASS:
 *    Extends {@link PreferenceActivity},
 *    Implements {@link TimeFrameConstants}and { @link OnPreferenceChangeListener}
 *
 * RESPONSIBILITIES:
 * 1. Allow the user to create new time frames
 * 2. Allow the user to edit an existing time frame
 * 3. Update the time frame database when the user saves the changes to the time frame
 * 4. Invoke methods to register the new the pending intents for the time frame
 *
 * COLLABORATORS:
 * None
 *
 * </pre></code>
 *
 */
public class TimeFrameEditActivity extends PreferenceActivity implements
    TimeFrameConstants,
    OnPreferenceChangeListener {

    private static final String TAG = TimeFrameEditActivity.class.getSimpleName();
    /** Identifier for the Start Time preference */
    private TimeFrameTimePreference mStartTimePref;
    /** Identifier for the End Time preference */
    private TimeFrameTimePreference mEndTimePref;
    /** Identifier for the Days of Week preference */
    private TimeFrameRepeatPreference mDaysOfWeekPref;
    /** Start Time - Hour - 24 hour format*/
    private int mStartHour;
    /** Start Time - Minutes */
    private int mStartMinutes;
    /** End Time - Hour - 24 hour format*/
    private int mEndHour;
    /** Start Time - Minutes */
    private int mEndMinutes;
    /** Field to differentiate between creation of a new time frame or edit of an existing
     * time frame
     */
    @SuppressWarnings("unused")
    private boolean mCreateNewMode;
    /** Start time in HH:MM format - 24 hour format */
    private String  mFrom;
    /** End time in HH:MM format - 24 hour format */
    private String  mTo;
    /** Name of the time frame being created */
    private String  mNewTimeFrameName;
    /** Name of the time frame being edited */
    private String  mOldTimeFrame;
    /** String representing the week days on which this time frame is to repeat */
    private String  mFreq;
    /** Identifier for the Time frame name preference */
    private TimeFrameNamePreference mTimeFrameNamePref;
    /** Identifier for all/whole day event check box preference */
    private CheckBoxPreference mIsAllDayPreference;
    /** Application Context */
    private Context mContext;
    /** Flag to indicate if its an all day event */
    private boolean mIsAllDayEvent;
    /** Bit mask to represent the days to repeat */
    private int mDaysOfWeek;

    /** Used as key for storing user selected time in mStartTimePref*/
    private static final String TIME_FRAME_START_KEY = "timestart";
    /** Used as key for storing user selected time in mEndTimePref*/
    private static final String TIME_FRAME_END_KEY = "timeend";

    /** Used to monitor user selected time in mStartTimePref and mEndTimePref */
    private SharedPreferences mPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener mListener;
    private Preference mPreferenceClicked = null;
    private boolean mDisableActionBar = false;

    /* (non-Javadoc)
     * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.time_preference);
        // get start time and end time preferences
        mStartTimePref = (TimeFrameTimePreference)findPreference(getString(R.string.s_time));
        mEndTimePref   = (TimeFrameTimePreference)findPreference(getString(R.string.e_time));
        if(mStartTimePref != null)
            mStartTimePref.setPrefKey(TIME_FRAME_START_KEY);

        if(mEndTimePref != null)
            mEndTimePref.setPrefKey(TIME_FRAME_END_KEY);

        // Get each preference so we can retrieve the value later.
        mTimeFrameNamePref = (TimeFrameNamePreference)
                             findPreference(getString(R.string.timeframe_name_preference));
        //set the listener for name changes
        if(mTimeFrameNamePref != null) {
            mTimeFrameNamePref.setPositiveButtonText(R.string.save);
            mTimeFrameNamePref.setOnPreferenceChangeListener(
            new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference p,Object newValue) {
                    // Set the summary based on the new label.
                    if(newValue instanceof String) {
                        String name = ((String) newValue).trim();
                        p.setSummary(name);
                        mNewTimeFrameName = name;
                        return true;
                    } else
                        return false;

                }
            });
        }


        mIsAllDayPreference = (CheckBoxPreference)
                              findPreference(getString(R.string.all_day_event_pref));
        // by default set it to unchecked !
        if(mIsAllDayPreference != null) {
            mIsAllDayPreference.setChecked(false);
            mIsAllDayPreference.setOnPreferenceChangeListener(this);
        }
        // get the repeat/days of week preference
        mDaysOfWeekPref = (TimeFrameRepeatPreference)findPreference(getString(R.string.repeat_pref));
        if(mDaysOfWeekPref != null)
            mDaysOfWeekPref.setOnPreferenceChangeListener(this);
        getListView().setItemsCanFocus(true);

        // Grab the content view so we can modify it.
        FrameLayout content = (FrameLayout) getWindow().getDecorView().findViewById(id.content);

        // Get the main ListView and remove it from the content view.
        ListView lv = getListView();
        content.removeView(lv);

        // Create the new LinearLayout that will become the content view and
        // make it vertical.
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);

        // Have the ListView expand to fill the screen minus the save/cancel buttons
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT);
        lp.weight = 1;
        ((LinearLayout)(lv.getParent())).removeView(lv);
        ll.addView(lv, lp);

        // Need to inflate the save and cancel buttons for the action bar to show them.
        LayoutInflater.from(this).inflate(R.layout.save_cancel_time_frame, ll);
        
        // Replace the old content view with our new one.
        setContentView(ll);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();
        setupActionBarItemsVisibility(true);
        doStartActions();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (LOG_INFO) Log.i(TAG, "onSaveInstanceState");
        mDisableActionBar = true;
    }

    /**
     * This method sets up visibility for the action bar items.
     * @param enableSaveButton - whether save button needs to be enabled
     */
    protected void setupActionBarItemsVisibility(boolean enableSaveButton) {
        if(mDisableActionBar) return;
        int editFragmentOption = EditFragment.EditFragmentOptions.DEFAULT;
        if(enableSaveButton)
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_ENABLED;
        else
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_DISABLED;
        // Add menu items from fragment
    	Fragment fragment = EditFragment.newInstance(editFragmentOption, false);
        getFragmentManager().beginTransaction().replace(R.id.edit_fragment_container, fragment, null).commit();
    }

    /** onOptionsItemSelected()
     *  handles key presses in ICS action bar.
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
            if (LOG_INFO) Log.i(TAG, "OK button clicked");
            saveTimeFrame();
            result = true;
            break;
        case R.id.edit_cancel:
            result = true;
            finish();
            break;
        }
        return result;
    }

    /**
     * Parse the incoming intent and and initialize the state variables. Starts the thread to read
     * the data, if this activity is launched for Editing.
     */
    public void doStartActions() {
        mContext = getApplicationContext();

        Intent launchIntent = getIntent();
        if (launchIntent == null) {
            Log.e(TAG,"Started with null intent. This should never happen");
            return;
        }
        else {
            if (LOG_INFO) Log.i(TAG, "Intent Received: " + launchIntent.toUri(0));
        }
        // get the action in the intent
        String intentAction = launchIntent.getAction();

        // Differentiate the request type based on the incoming intent. For new mode creation the
        // action will be null
        if (intentAction == null) {
            mCreateNewMode = true;
            setTitle(getString(R.string.add_time_frame));
        }
        else if (intentAction.equals(TIME_FRAME_EDIT_INTENT)) {
            // get details of the time frame and display it for further editing. Launch a thread
            // since getting details involve database queries
            mCreateNewMode = false;
            setTitle(getString(R.string.edit_time_frame));
            mOldTimeFrame = launchIntent.getStringExtra(EXTRA_FRAME_NAME);
            Thread t = new Thread(new DbWorker(mHandler, DB_TASK_READ));
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        }
        else {
            // do nothing
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (LOG_INFO) Log.i(TAG, "onResume");
        mDisableActionBar = false;
        initializeSharedPreferenceChangeListener();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mPreferences != null) {
            mPreferences.unregisterOnSharedPreferenceChangeListener(mListener);
            mListener = null;

            SharedPreferences.Editor editor = mPreferences.edit();
            editor.remove(TIME_FRAME_START_KEY);
            editor.remove(TIME_FRAME_END_KEY);
            editor.commit();
        }

    }

    /**
     * Initializes OnSharedPreferenceChangeListener. This will be used to update
     * member variables related to start and end time when user changes those values
     */
    private void initializeSharedPreferenceChangeListener() {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        if(mListener == null) {
            mListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    if (mPreferenceClicked != null && mPreferences != null &&
                    mPreferenceClicked.equals(mStartTimePref)) {
                        mFrom = mPreferences.getString(TIME_FRAME_START_KEY, mFrom);
                        if ((mFrom != null) && (mFrom.length() != 0)) {
                            mStartHour    = Integer.parseInt(mFrom.split(TIME_SEPERATOR)[0]);
                            mStartMinutes = Integer.parseInt(mFrom.split(TIME_SEPERATOR)[1]);
                        }
                    } else {
                        if(LOG_DEBUG) Log.d(TAG, " Setting Start time failed ");
                    }

                    if (mPreferenceClicked != null && mPreferences != null &&
                    mPreferenceClicked.equals(mEndTimePref)) {
                        mTo = mPreferences.getString(TIME_FRAME_END_KEY, mTo);
                        if ((mTo != null) && (mTo.length() != 0)) {
                            mEndHour    = Integer.parseInt(mTo.split(TIME_SEPERATOR)[0]);
                            mEndMinutes = Integer.parseInt(mTo.split(TIME_SEPERATOR)[1]);
                        }
                    } else {
                        if(LOG_DEBUG) Log.d(TAG, " Setting End time failed ");
                    }

                }
            };
            if(mPreferences != null)
                mPreferences.registerOnSharedPreferenceChangeListener(mListener);
        }
    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {

        //Used to retrieve changed values for mStartTimePref and mEndTimePref
        mPreferenceClicked = preference;
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /**
     * Disable the time setting by the user. This needs to be done, when the user selects the
     * "All Day" check box
     */
    private void disableTimeSetting() {
        mStartTimePref.setEnabled(false);
        mEndTimePref.setEnabled(false);
        //mStartTimePref.setSummary(EMPTY_SUMMARY);
        //mEndTimePref.setSummary(EMPTY_SUMMARY);
    }

    /**
     * Enable the time setting by the user. This needs to be done, when the user un-checks the
     * "All Day" check box
     */
    private void enableTimeSetting() {

        mStartTimePref.setEnabled(true);
        // check required to display empty summary when the user creates a time frame with allday
        // flag initially and then un checks it.
        //Retrieve the Start and End time settings.

        if ((mFrom != null) && (mFrom.length() != 0)) {
            mStartTimePref.setSummary(TimeUtil.getDisplayTime(mContext, mStartHour, mStartMinutes));
        } else {
            mStartTimePref.setSummary(EMPTY_SUMMARY);
        }
        mEndTimePref.setEnabled(true);
        // check required to display empty summary when the user creates a time frame with allday
        // flag initially and then un checks it.
        if ((mTo != null) && (mTo.length() != 0)) {
            mEndTimePref.setSummary(TimeUtil.getDisplayTime(mContext, mEndHour, mEndMinutes));
        } else {
            mEndTimePref.setSummary(EMPTY_SUMMARY);
        }
    }
    /**
     * Saves the time frame with all the details entered.
     */
    private void saveTimeFrame() {
        if (LOG_DEBUG) Log.d(TAG, "Save the current mode !!");

        //do the basic check first before checking for name duplication
        if (basicTimeCheck() == false) {
            if (SHOW_TOASTS) {
                Toast.makeText(TimeFrameEditActivity.this,
                               getString(R.string.incomplete_details_error),Toast.LENGTH_SHORT)
                .show();
            }
            return;
        }

        if ( isNameDuplication(mNewTimeFrameName) == true ) {
            //Above condition will be true, even if the normal edit of a time frame
            //where the name is not changed. So differentiate that scenario
            if ( (mOldTimeFrame == null) || (!mOldTimeFrame.equals(mNewTimeFrameName))) {
                if (SHOW_TOASTS) {
                    Toast.makeText(TimeFrameEditActivity.this,
                                   getString(R.string.name_duplication_error),Toast.LENGTH_SHORT)
                    .show();
                }
                return;
            }
        }

        if (LOG_INFO) Log.i(TAG, "Basic check passed. Commit the changes to DB");
        // commit the changes and finish the activity
        Thread t = new Thread(new DbWorker(mHandler, DB_TASK_WRITE));
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /**
    * Method checks if the input from the user is proper.
    *
    * @return - True if the input is proper/valid
    *         - False otherwise
    */
    boolean basicTimeCheck() {

        // This method does the following checks
        // 1. Name is not empty
        // 2. At least 1 day is selected
        // 3. Start time and End Time are set

        if ((mNewTimeFrameName == null) || (mNewTimeFrameName.length() == 0))  {
            return false;
        }

        // get the handle to repeat days preference
        TimeFrameRepeatPreference daysOfWeek =
            (TimeFrameRepeatPreference)findPreference(getString(R.string.repeat_pref));
        //get the list of days selected byt he the user
        if(daysOfWeek != null)
            mFreq = daysOfWeek.getDaysOfWeek().toCommaSeparatedString(mContext,true);
        if (LOG_DEBUG) Log.d(TAG, "Selected Days: " + mFreq);

        // check if at least 1 day is selected by the user
        if ((mFreq.length() == 0) || (mFreq.equals(getString(R.string.never)))) {
            return false;
        }

        // if there is no "," then it is single day and is in long format. so convert it
        if (!mFreq.contains(SHORT_DAY_SEPARATOR) && !mFreq.equals(getString(R.string.everyday))) {
            // get the short format
            mFreq = new TimeFrameDaysOfWeek(mFreq).getShortFormat(mFreq);
        }

        //check if both start & end time is set
        if ((mIsAllDayEvent == false) && ((mFrom == null ) || (mTo == null))) {
            return false;
        }
        //check if both start & end time is set
        if ((mIsAllDayEvent == false) && ((mFrom.length() == 0 ) || (mTo.length() == 0))) {
            return false;
        }
        if (LOG_DEBUG) Log.d(TAG, "Exit Selected Days: " + mFreq);
        return true;
    }

    /**
     * This method updates the UI with the details of the time frame.
     */
    void updateUi() {
        if (LOG_DEBUG) Log.d(TAG, "updateUi Entry");

        // set the time frame name
        mTimeFrameNamePref.setSummary(TimeUtil.getTranslatedTextForId(mContext,mOldTimeFrame));
        mTimeFrameNamePref.setText(TimeUtil.getTranslatedTextForId(mContext,mOldTimeFrame));

        if (mIsAllDayEvent == true) {
            mIsAllDayPreference.setChecked(true);
            disableTimeSetting();
        }
        else {
            mIsAllDayPreference.setChecked(false);
            mStartTimePref.setEnabled(true);
            mEndTimePref.setEnabled(true);

            //set the start time for the time frame

            mStartHour    = Integer.parseInt(mFrom.split(TIME_SEPERATOR)[0]);
            mStartMinutes = Integer.parseInt(mFrom.split(TIME_SEPERATOR)[1]);
            mStartTimePref.setSummary(TimeUtil.getDisplayTime(mContext, mStartHour, mStartMinutes));
            mStartTimePref.setTime(mFrom.split(TIME_SEPERATOR)[0], mFrom.split(TIME_SEPERATOR)[1]);
            if (LOG_DEBUG) Log.d(TAG, "StartTime Summary :" +
                                     TimeUtil.getDisplayTime(mContext, mStartHour, mStartMinutes));

            //set the end time for the time frame
            mEndHour    = Integer.parseInt(mTo.split(TIME_SEPERATOR)[0]);
            mEndMinutes = Integer.parseInt(mTo.split(TIME_SEPERATOR)[1]);
            mEndTimePref.setSummary(TimeUtil.getDisplayTime(mContext, mEndHour, mEndMinutes));
            mEndTimePref.setTime(mTo.split(TIME_SEPERATOR)[0], mTo.split(TIME_SEPERATOR)[1]);
            if (LOG_DEBUG) Log.d(TAG, "EndTime Summary :" +
                                     TimeUtil.getDisplayTime(mContext, mEndHour, mEndMinutes));
        }
        if (LOG_DEBUG) Log.d(TAG, "Days of week being set : " + mFreq);
        mDaysOfWeekPref.setDaysOfWeek(mDaysOfWeek);
    }


    /** Helper method to find out if there is a name duplication Used before saving the new
     * time frame or edited time frame
     * @param - Time frame name to check for duplication
     * @return - True, if this is a duplicate name
     */
    boolean isNameDuplication(String timeFrameName) {
        boolean result = false;
        TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(mContext);
        Cursor cursor;
        // query with the current name. If the cursor returned by the query has any entries
        // it means the name given conflicts with the name of another time frame
        cursor = dbAdapter.getTimeframe(timeFrameName);
        if (cursor != null) {
            try {
                result = cursor.getCount() >= 1;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                cursor.close();
            }
        }
        dbAdapter.close();
        return result;
    }

    /**
     * Handler to process the asynchronous responses
     */
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.obj.equals(READ_SUCCESS)) {
                if (LOG_DEBUG) Log.d(TAG, "Received READ_SUCCESS");
                updateUi();
            }
            else if (msg.obj.equals(WRITE_SUCCESS)) {
                if (LOG_INFO) Log.i(TAG, "Received WRITE_SUCCESS : " + msg.getData().getString(CURRENT_SELECTION));
                Intent data = null;

                if((msg.getData() != null) && (msg.getData().getString(CURRENT_SELECTION) != null)) {
                    data = new Intent();
                    data.putExtra(CURRENT_SELECTION, msg.getData().getString(CURRENT_SELECTION));
                }
                setResult(RESULT_OK, data);
                finish();
            }
            else if (msg.obj.equals(FAILED)) {
                if (LOG_DEBUG) Log.d(TAG, "Received FAILED");
            }
            else {
                // do nothing
            }

        }
    };

    /**
     * Worker thread to do Database operations in a different thread.
     *
     * CLASS:
     *  Implements Runnable
     *
     * RESPONSIBILITIES:
     *  1. Read the time frame details from the database
     *  2. Write the time frame into the database
     *
     * COLABORATORS:
     *  None
     *
     * USAGE:
     *  See each method.
     *
     */
    private final class DbWorker implements Runnable {
        Handler mCallbackHandler = null;
        int mTaskCode;

        public DbWorker(Handler callBack, int taskCode) {
            if (LOG_DEBUG) Log.d(TAG,"Creating DB Query thread - DbWorker");
            mCallbackHandler = callBack;
            mTaskCode = taskCode;
        }

        /** run() handler
         */
        public void run() {
            // prevent parallel read & write
            synchronized (DbWorker.class) {
                if (mTaskCode == DB_TASK_READ) {
                    if (LOG_DEBUG) Log.d(TAG, "Read request to the worker thread.");
                    readTimeframeFromDb();
                }
                else if (mTaskCode == DB_TASK_WRITE) {
                    if (LOG_DEBUG) Log.d(TAG, "Write request to the worker thread.");
                    TimeFrameDaysOfWeek tfDow = new TimeFrameDaysOfWeek(mFreq);
                    int daysOfWeek = tfDow.getCoded();

                    int responseCode = TimeUtil.writeTimeframeToDb(mContext, mOldTimeFrame, mNewTimeFrameName, mFrom, mTo, mIsAllDayEvent, daysOfWeek);
                    String intName = getInternalName(mNewTimeFrameName);
                    if (LOG_INFO) Log.i(TAG, "Generated int name : " + intName);

                    if(responseCode == WRITE_SUCCESS)
                    {
                        if(LOG_DEBUG) Log.d(TAG, " Exporting Time Frames !!!");
                        TimeUtil.exportTimeFrameData(mContext);
                    }

                    Bundle bundle = null;
                    if(intName != null) {
                        bundle = new Bundle();
                        bundle.putString(CURRENT_SELECTION, intName);
                    }

                    sendResponseToHandler(responseCode, bundle);                    
                }
            }
        }

        /**
         * Extracts the internal name from the timeframe name
         *
         * @param tfName - Timeframe name
         * @return - returns internal name
         */
        private String getInternalName(String tfName) {
            Cursor cursor = null;
            TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(mContext);
            try {
                cursor = dbAdapter.getTimeframe(tfName);

                if (cursor == null) {
                    Log.e(TAG, "getTimeframeRow returned null cursor");
                } else {

                    if (cursor.getCount() >  0) {
                        if (LOG_INFO) Log.i(TAG, "Number of timeframes returned :" + cursor.getCount());
                        cursor.moveToFirst();

                        String intName = cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.NAME_INT));
                        if (LOG_INFO) Log.i(TAG, "Internal Name :" + intName);
                        return intName;
                    }

                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while querying timeframe");
                e.printStackTrace();
            } finally {
                if(cursor != null)
                    cursor.close();
                dbAdapter.close();
            }

            return null;
        }
        /**
         * Extracts the necessary details from the cursor
         *
         * @param cursor - Cursor that holds the time frame details
         */
        private void extractDataFromCursor(Cursor cursor) {
            mNewTimeFrameName = cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.NAME));
            mFrom = cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.START));
            mTo   = cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.END));
            String allDay = cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.ALL_DAY));
            mDaysOfWeek = cursor.getInt(cursor.getColumnIndex(TimeFrameTableColumns.DAYS_OF_WEEK));
            mFreq = new TimeFrameDaysOfWeek(mDaysOfWeek).toCommaSeparatedString(mContext, true);
            if (allDay.equalsIgnoreCase(ALL_DAY_FLAG_TRUE)) {
                mIsAllDayEvent = true;
                mFrom = "";
                mTo   = "";
            }
            else {
                mIsAllDayEvent = false;
            }
        }

        /**
         * This method queries the time frame table, get the details of the time frame and displays
         * it on the UI
         */
        private void readTimeframeFromDb() {
            TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(mContext);
            Cursor cursor;
            cursor = dbAdapter.getTimeframe(mOldTimeFrame);
            int responseCode = FAILED;
            if (cursor != null) {
                try {
                    if (cursor.getCount() == 0) {
                        if (LOG_DEBUG) Log.d(TAG, "Empty Timeframe table");
                    }
                    else {
                        if (LOG_DEBUG) Log.d(TAG, "Query returned :" + cursor.getCount());
                        if (cursor.moveToFirst()) {
                            extractDataFromCursor(cursor);
                            if (LOG_DEBUG) DatabaseUtils.dumpCursorToString(cursor);
                            if (LOG_DEBUG) Log.d(TAG, "Valid Mode. Return Success");
                            responseCode = READ_SUCCESS;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    responseCode = FAILED;
                } finally {
                    cursor.close();
                }
            }
            else {
                Log.e(TAG, "Unable to retrive the details of the time frame " + mOldTimeFrame);
                responseCode = FAILED;
            }
            dbAdapter.close();
            sendResponseToHandler(responseCode);
        }

        /**
         * Helper method to send the response code back to the handler
         *
         * @param responseCode - WRITE_SUCCESS or READ_SUCCESS or FAILED
         */
        void sendResponseToHandler(int responseCode) {
            sendResponseToHandler(responseCode, null);
        }
        /**
         * Helper method to send the response code back to the handler
         *
         * @param responseCode - WRITE_SUCCESS or READ_SUCCESS or FAILED
         * @param bundle - data to be sent back
         */
        void sendResponseToHandler(int responseCode, Bundle bundle) {
            Message response = Message.obtain();
            response.obj = responseCode;
            if(bundle != null)
                response.setData(bundle);
            response.setTarget(mCallbackHandler);
            response.sendToTarget();
        }
    }

    /**
     * Call back to monitor any change in the preferences in the activity
     *
     * @param preference - Preference that is being change
     * @param newValue   - New Value of the preference
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mTimeFrameNamePref)) {
            String name = ((String)newValue).trim();
            preference.setSummary(name);
            mNewTimeFrameName = name;
        }
        else if (preference.equals(mIsAllDayPreference)) {
            mIsAllDayEvent = !mIsAllDayEvent;
            if (mIsAllDayEvent == true) {
                disableTimeSetting();
            }
            else {
                enableTimeSetting();
            }
        }
        return true;
    }
}
