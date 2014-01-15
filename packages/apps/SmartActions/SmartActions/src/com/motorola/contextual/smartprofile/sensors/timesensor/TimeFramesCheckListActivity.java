/*
 * @(#)TimeFramesCheckListActivity.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a15776       2010/12/01   NA               Initial Version
 *
 */

package com.motorola.contextual.smartprofile.sensors.timesensor;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.contextual.smartprofile.SmartProfileConfig;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.fragment.EditFragment;

/**
 * This class extends the list activity which shows the list of all the time
 * frames created by the user and allows the user to select 1 or more time
 * frames
 *
 * CLASS: Extends ListActivity which provides basic list building, scrolling,
 * etc.
 *
 * RESPONSIBILITIES: 1. Show list of time frames created by the user 2. Allows
 * the user to pick one or more time frames 3. Compose the virtual sensor
 * strings that would be passed back to the calling method/class
 *
 * COLLABORATORS: None
 *
 * USAGE: See each method.
 */
public class TimeFramesCheckListActivity extends ListActivity implements
    TimeFrameXmlSyntax, TimeFrameConstants, SimpleCursorAdapter.ViewBinder,
    OnClickListener, DbSyntax {
    private static final String TAG = TimeFramesCheckListActivity.class
                                      .getSimpleName();

    public interface MenuOptions {
        final int EDIT = 0;
        final int DELETE = EDIT + 1;
        final int ALL_LOCATIONS = DELETE + 1;
    }

    /** used to store the values for each row */
    private static class KeyValues {
        String timeName;
        String internalName;
    };

    /**
     * Local class for better readability
     */
    private static class TimeList extends ArrayList<String> {
        private static final long serialVersionUID = -1941007612710493271L;
    };

    /** Application Context */
    private Context mContext;
    /**
     * comma separated time frame names, selected earlier, passed by the puzzle
     * builder
     */
    private String mPassedTimeframeInternalNames;
    /** List of time frames - internal name from the current selection */
    private TimeList mIntTimeSelected = new TimeList();
    /** Database adapter */
    private TimeFrameDBAdapter mDbAdapter;
    /**
     * Flag to indicate if this activity is launched for rules builder or
     * profiles of Smart Rules. mShowCheckBox will be true if the activity is
     * launched for the rules builder where the user should be shown the check
     * box to make selections
     */
    private boolean mShowCheckBox;
    private Cursor mTimeFramesCursor;
    private boolean mDisableActionBar = false;

    @Override
    /** onCreate()
     */
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mContext = this;
        // do not display title
        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.time_frame_list);
        Intent intent = getIntent();
        if (intent == null) {
            Log.e(TAG, "Invoked with Null intent. This is an error");
            return;
        }

        /*if (intent.getBooleanExtra("NEW", false)) {
            Intent outBound = new Intent(TimeFramesCheckListActivity.this, TimeFrameEditActivity.class);
            mContext.startActivity(outBound);
            finish();
        }*/

        if (LOG_DEBUG)
            Log.d(TAG, "intent is " + getIntent().toUri(0));

        if (intent.getCategories() == null) {
            String action = intent.getAction();
            if ((action != null) && !(action.equals(TIMEFRAME_LIST_INTENT))) {
                if (LOG_DEBUG)
                    Log.d(TAG, "Launched for rules builder");
                mShowCheckBox = true;
                // Extract the useful extras coming in the intent. These are
                // required for showing
                // the previous user selections
                mPassedTimeframeInternalNames = intent
                                                .getStringExtra(INT_CURRENT_SELECTION);

                String config = intent.getStringExtra(EXTRA_CONFIG);
                SmartProfileConfig profileConfig = new SmartProfileConfig(config);

                String value = profileConfig.getValue(TIMEFRAME_NAME);
                
                mPassedTimeframeInternalNames = ((config != null) && (config.contains(TIMEFRAME_CONFIG_STRING)) && (value != null)) ? 
                                                                               TimeUtil.trimBraces(value) : null;

                if (LOG_DEBUG) {
                    Log.d(TAG, " mPassedTimeframeIntTags = "
                          + mPassedTimeframeInternalNames);
                }

                // if there is/are earlier selections, extract them. time frames
                // names are separated
                // by OR. e.g If the user has earlier selected two time frames
                // named Morning and
                // Weekend,then mPassedTimeframeTags will have Morning OR
                // Weekend.
                if ((mPassedTimeframeInternalNames != null)
                        && (mPassedTimeframeInternalNames.length() != 0)) {
                    String[] passedIntTimeframeTagssArray = mPassedTimeframeInternalNames
                                                            .split(OR_STRING);

                    // add the selections in the list
                    for (int i = 0; i < passedIntTimeframeTagssArray.length; i++) {
                        mIntTimeSelected.add(passedIntTimeframeTagssArray[i]);
                    }
                }
                // It is possible that the time frames used in the rule has been
                // deleted now. So
                // remove those time frames from selection during editing
                updateSelectedListOfTimeFrames();
            }
        }

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.time_frames_heading);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();
        setupActionBarItemsVisibility(false);
    }

    /**
     * onOptionsItemSelected() handles key presses in ICS action bar.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            result = true;
            break;
        case R.id.edit_add_button:
            try {
                Intent intent = new Intent();
                intent.setClass(mContext, TimeFrameEditActivity.class);
                startActivityForResult(intent, 0);
            } catch (Exception e) {
                Log.e(TAG, "Cannot launch Edit Activity");
                e.printStackTrace();
            }
            result = true;
            break;
        case R.id.edit_save:
            if (LOG_INFO)
                Log.i(TAG, "OK button clicked");
            if (mIntTimeSelected.size() == 0) {
                Toast.makeText(mContext, "No selection. Select a time frame",
                               Toast.LENGTH_SHORT).show();
            } else {
                // Getting the virtual sensor strings involve querying database
                // for the
                // configured time frames. So do that in a different thread to
                // avoid any
                // ANR
                Thread configThread = new Thread(new GetConfigStrings(mIntTimeSelected));
                // The thread is needed here to composes the VSENSOR string
                // required by the rules builder.
                // To compose the VSESNOR it has to do database lookups to get
                // the time
                // frame details. All these are done as part of
                // setConfigureResult()
                // This is the reason for running this in a separate thread.
                configThread.start();
            }
            setupActionBarItemsVisibility(false);
            result = true;
            break;
        case R.id.edit_cancel:
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            result = true;
            finish();
            break;
        }
        return result;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (LOG_INFO) Log.i(TAG, "onSaveInstanceState");
        mDisableActionBar = true;
    }

    /**
     * This method sets up visibility for the action bar items.
     *
     * @param enableSaveButton
     *            - whether save button needs to be enabled
     */
    protected void setupActionBarItemsVisibility(boolean enableSaveButton) {
        if(mDisableActionBar) return;
        int editFragmentOption = EditFragment.EditFragmentOptions.DEFAULT;
        if (mShowCheckBox && enableSaveButton)
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_ENABLED;
        else if (mShowCheckBox && !enableSaveButton)
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_DISABLED;
        // Add menu items from fragment
        Fragment fragment = EditFragment.newInstance(editFragmentOption, true);
        getFragmentManager().beginTransaction()
        .replace(R.id.edit_fragment_container, fragment, null).commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                if (data != null) {

                    if (LOG_INFO)
                        Log.i(TAG, "In onActivityResult : friendly name : "
                              + data.getStringExtra(CURRENT_SELECTION));
                    if (!mIntTimeSelected.contains(data
                                                   .getStringExtra(CURRENT_SELECTION))) {
                        mIntTimeSelected.add(data
                                             .getStringExtra(CURRENT_SELECTION));
                    }

                }
            }
        }

    }

    /**
     * onDestroy()
     */
    @Override
    public void onDestroy() {
        if (LOG_DEBUG)
            Log.d(TAG, "In onDestroy");
        super.onDestroy();
        mDisableActionBar = true;
    }

    /**
     * onResume()
     */
    @Override
    protected void onResume() {
        if (LOG_INFO)
            Log.d(TAG, "In onResume");
        super.onResume();
        mDisableActionBar = false;
        displayTimeframesList();
    }

    /**
     * Deletes a time frame with the given name
     *
     * @param name
     *            - Name of the time frame to be deleted
     */
    public boolean deleteTimeFrame(String name) {
        boolean result = false;

        if (mDbAdapter == null) {
            mDbAdapter = new TimeFrameDBAdapter(mContext);
        }
        Cursor cursor = mDbAdapter.getTimeframe(name);

        if (cursor == null) {
            Log.e(TAG, "getTimeframeRow returned null cursor");
        } else {
            try {
                if (cursor.getCount() > 0) {
                    if (LOG_INFO)
                        Log.i(TAG,
                              "Number of timeframes returned :"
                              + cursor.getCount());
                    cursor.moveToFirst();
                    int msgId;
                    if (!isTimeFrameUsedInSmartRules(cursor.getString(cursor
                                                     .getColumnIndex(TimeFrameTableColumns.NAME_INT)))) {
                        TimeFrame timeframe;
                        timeframe = new TimeFrame(mContext, cursor);
                        // delete the time frame from the list. this will
                        // unregister the pending intents
                        // and will also make the state of the time frame to
                        // false, so any rule using
                        // this time frame can be at right state
                        timeframe.deleteSelf();
                        // delete the time frame from the database table
                        mDbAdapter.deleteTimeframe(name);
                        msgId = R.string.timeframe_deleted;
                        result = true;
                    } else {
                        if (LOG_INFO)
                            Log.i(TAG,
                                  "Unable to delete timeframe as it is being used");
                        msgId = R.string.timeframe_delete_error;
                    }
                    Toast.makeText(mContext,
                            msgId,
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while deleting time row");
                e.printStackTrace();
            } finally {
                cursor.close();
            }
        }

        return result;
    }

    /**
     *
     * @param intName
     *            - Value present in column intName maintained in timeframes.db
     * @return - true if the timeframe is used by any of the Smart Rules - false
     *         if none is using the timeframe identified by the argument
     */
    private boolean isTimeFrameUsedInSmartRules(String intName) {
        boolean result = false;
        List<String> configList = TimeFramesDetailComposer.getConfigListByInternalName(mContext, intName);
        result = (configList.isEmpty()) ? false : true;
        return result;
    }

    /**
     * displays the list of time-frames available in the db
     */
    private void displayTimeframesList() {
        if (mDbAdapter == null) {
            mDbAdapter = new TimeFrameDBAdapter(mContext);
        }

        // get all the time frames from the database
        mTimeFramesCursor = mDbAdapter.getVisibleTimeframes();
        // If the cursor is null it shows the error message in the display
        // display the time frames in the layout
        if (mTimeFramesCursor != null) {
            if (mTimeFramesCursor.moveToFirst()) {
                this.startManagingCursor(mTimeFramesCursor);
                if (LOG_DEBUG)
                    Log.d(TAG, "cursor is not null dumping cursor");
                if (LOG_DEBUG)
                    DatabaseUtils.dumpCursor(mTimeFramesCursor);
                hideErrorMessage();
                populateTimeframesListView(mTimeFramesCursor);
            } else {
                mTimeFramesCursor.close();
                // display that there are no entries in the table
                showErrorMessage(TIME_ZERO_ROWS);
            }
        } else {
            // if the cursor is null display error
            showErrorMessage(NULL_CURSOR);
        }
        mDbAdapter.close();
        mDbAdapter = null;
    }

    /**
     * populates the list view
     *
     * @param cursor
     *            - cursor to populate the list view
     */
    private void populateTimeframesListView(Cursor cursor) {
        if (LOG_DEBUG)
            Log.d(TAG, "Populating the timeName tags view");

        String[] from = { TimeFrameTableColumns.NAME,
                          TimeFrameTableColumns.START, TimeFrameTableColumns.END,
                          TimeFrameTableColumns.DAYS_OF_WEEK
                        };

        int[] to = { R.id.placelist_first_text_line,
                     R.id.placelist_second_text_line,
                     R.id.placelist_third_text_line, R.id.placelist_checkbox
                   };

        // setup the adapter, view binder, etc.
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
                R.layout.time_mode_list_with_checkbox, cursor, from, to);

        adapter.setViewBinder(this);
        ListAdapter oldAdapter=getListAdapter();
        if (oldAdapter!=null)
        {
            Cursor oldCursor = ((SimpleCursorAdapter) oldAdapter).getCursor();
            if ((oldCursor != null) && (!oldCursor.isClosed())) {
                this.stopManagingCursor(oldCursor);
                oldCursor.close();
            }
        }
        setListAdapter(adapter);
    }

    /**
     * Displays an error message if the cursor was null or empty
     *
     * @param errorMsg
     *            - Message to be displayed to the user.
     */
    private void showErrorMessage(String errorMsg) {
        RelativeLayout errRl;
        TextView errTextView = (TextView) findViewById(R.id.failmessage_text);
        if (errTextView != null) {
            errRl = (RelativeLayout) errTextView.getParent();
            errRl.setVisibility(View.VISIBLE);
            errTextView.setText(errorMsg);
        }
    }

    /**
     * hides the error message layout when it does not requires display.
     */
    private void hideErrorMessage() {
        RelativeLayout errRl;
        TextView errTextView = (TextView) findViewById(R.id.failmessage_text);
        if (errTextView != null) {
            errRl = (RelativeLayout) errTextView.getParent();
            errRl.setVisibility(View.GONE);
        }
    }

    /**
     * sets the view value for each row.
     *
     * @param view
     *            - the view to bind the data to
     * @param cursor
     *            - cursor with data
     * @param columnIndex
     *            - column index for which the view value is being set
     * @return true
     */
    // @Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

        boolean boundHere = false;
        int id = view.getId();

        // first line shows the time frame name
        if (id == R.id.placelist_first_text_line && view instanceof TextView) {
            ((TextView) view).setText(TimeUtil.getTranslatedTextForId(mContext,
                                      cursor.getString(cursor
                                              .getColumnIndex(TimeFrameTableColumns.NAME))));
            KeyValues info = new KeyValues();
            info.timeName = cursor.getString(cursor
                                             .getColumnIndex(TimeFrameTableColumns.NAME));
            info.internalName = cursor.getString(cursor
                                                 .getColumnIndex(TimeFrameTableColumns.NAME_INT));
            // set tag for later use
            ((View) (view.getParent()).getParent()).setTag(info);
            boundHere = true;
            RelativeLayout leftWrapper = (RelativeLayout) view.getParent();
            leftWrapper.setOnClickListener(this);
            leftWrapper.setOnCreateContextMenuListener(this);

            LinearLayout rightWrapper = (LinearLayout) ((LinearLayout) leftWrapper
                                        .getParent()).findViewById(R.id.right_wrapper);
            rightWrapper.setOnClickListener(this);

        }
        // second line shows from time - end time
        else if (id == R.id.placelist_second_text_line
                 && view instanceof TextView) {
            String displayText;
            String allDay = cursor.getString(cursor
                                             .getColumnIndex(TimeFrameTableColumns.ALL_DAY));
            if (allDay.equalsIgnoreCase(ALL_DAY_FLAG_TRUE)) {
                // if its an all day time frame, display the string accordingly
                ((TextView) view).setText(getString(R.string.all_day_event));
                displayText = getString(R.string.all_day_event);
            } else {
                // if its not an all day time frame, format the start time and
                // time and display
                String text1, text2;
                text1 = cursor.getString(cursor
                                         .getColumnIndex(TimeFrameTableColumns.START));
                text2 = cursor.getString(cursor
                                         .getColumnIndex(TimeFrameTableColumns.END));
                String dt1 = TimeUtil.getDisplayTime(mContext, text1);
                String dt2 = TimeUtil.getDisplayTime(mContext, text2);
                ((TextView) view).setText(dt1
                                          + getString(R.string.time_seperator) + dt2);
                displayText = dt1 + getString(R.string.time_seperator) + dt2;
            }
            ((TextView) view).setText(displayText);
            boundHere = true;
        }
        // third line shows the list of repeat days
        else if (id == R.id.placelist_third_text_line
                 && view instanceof TextView) {
            // append the repeat days to the time
            int daysOfWeek = cursor.getInt(cursor
                                           .getColumnIndex(TimeFrameTableColumns.DAYS_OF_WEEK));
            String text1 = new TimeFrameDaysOfWeek(daysOfWeek)
            .toCommaSeparatedString(mContext, true);
            ((TextView) view).setText(text1);
            boundHere = true;
        } else if (id == R.id.placelist_checkbox && view instanceof CheckBox) {
            if (mShowCheckBox) {
                CheckBox checkBox = (CheckBox) view;
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(false);
                // If this time frame is one in the list that's passed in the
                // extra,
                // make it checked !
                if (mIntTimeSelected.size() > 0) {
                    if (mIntTimeSelected.contains(cursor.getString(cursor
                                                  .getColumnIndex(TimeFrameTableColumns.NAME_INT)))) {
                        String name = cursor.getString(cursor
                                                       .getColumnIndex(TimeFrameTableColumns.NAME));
                        if (LOG_DEBUG)
                            Log.d(TAG, "One of the selected timeframes: "
                                  + name);
                        ((CheckBox) view).setChecked(true);
                    }
                    setupActionBarItemsVisibility(true);
                } else {
                    setupActionBarItemsVisibility(false);
                }
                boundHere = true;
            } else {
                CheckBox checkBox = (CheckBox) view;
                checkBox.setVisibility(View.GONE);
                LinearLayout l = (LinearLayout) view.getParent();
                View imgView = l.findViewById(R.id.divider);
                imgView.setVisibility(View.GONE);
            }
        }
        return boundHere;
    }

    /**
     * Method to translate the user selections to the puzzle builder
     * understandable strings, with all the necessary informations for the
     * virtual sensor creation and later editing of time frame condition in the
     * rule
     */
    public void setConfigureResult(TimeList intTimeSelected) {
        Intent intent = new Intent();
        StringBuilder timeTagName = new StringBuilder();
        StringBuilder timeTagIntName = new StringBuilder();
        int i;
        TimeFrames timeFrames = new TimeFrames().getData(mContext);
        if (timeFrames == null) {
            // this should never happen. This method would never be called
            // if the user
            // cannot select anything
            Log.e(TAG, "No timeframes to select");
            return;
        }

        int size = intTimeSelected.size();
        // construct an OR separated list of all the time frames that the
        // user selected
        if (size > 1) {
               // cjd - why .size -1? - perhaps a comment here to explain?
                // loop is run for .size - 1, since only for those many times
                // "OR" should be added
                // the last element is handled separately outside the loop
                for (i = 0; i < size - 1; i++) {
                    String name = intTimeSelected.get(i);
                    // timeTag Name to hold the "understandable" name list
                    String friendlyName = TimeUtil.getTranslatedTextForId(
                                              mContext,
                                              timeFrames.getFriendlyNameForTimeFrame(name));
                    if (friendlyName != null)
                        timeTagName.append(friendlyName).append(BLANK_SPC)
                        .append(getString(R.string.or))
                        .append(BLANK_SPC);
                    // timeTagIntName to hold the internal name list
                    timeTagIntName.append(name).append(OR_STRING);
                }

                if (LOG_DEBUG)
                    Log.d(TAG, "Results for size = " + size);

                String friendlyName = TimeUtil.getTranslatedTextForId(mContext,
                                      timeFrames.getFriendlyNameForTimeFrame(intTimeSelected
                                              .get(i)));
                if (friendlyName != null)
                    timeTagName.append(friendlyName);
                timeTagIntName.append(intTimeSelected.get(i));
            } else if (size == 1) {
                // if there is only one time frame selected by the user no
                // "OR"ing required
                String friendlyName = TimeUtil.getTranslatedTextForId(mContext,
                                      timeFrames.getFriendlyNameForTimeFrame(intTimeSelected
                                              .get(0)));
                if (friendlyName != null)
                    timeTagName.append(friendlyName);
                timeTagIntName.append(intTimeSelected.get(0));
            } else {
                // Save/OK is disabled if the user hasn't selected anything
                if (LOG_DEBUG)
                    Log.d(TAG, "No selection. This should never happen");
            }
        // compose the final fireUri. This would be used to update the UI when
        // this condition
        // is edited in the future

        // fill-in other extras
        String orSplitString = BLANK_SPC + getString(R.string.or) + BLANK_SPC;
        StringBuilder descriptionBuilder = new StringBuilder();
        String[] selectedTimeFrames = timeTagName.toString().split(
                                          orSplitString);
        if (selectedTimeFrames.length <= 2) {
            for (int index = 0; index < selectedTimeFrames.length; index++) {
                if (index == 0) {
                    descriptionBuilder.append(selectedTimeFrames[index]);
                } else {
                    descriptionBuilder.append(orSplitString).append(
                        selectedTimeFrames[index]);
                }
            }
        } else {
            descriptionBuilder.append(selectedTimeFrames[0])
            .append(orSplitString)
            .append(selectedTimeFrames.length - 1).append(BLANK_SPC)
            .append(getString(R.string.more));
        }
        // populate the VSENSOR extra needed to create dvs
        intent.putExtra(STATE_PUBLISHER_KEY, TIMEFRAME_PUBLISHER_KEY);

        String config = TIMEFRAME_CONFIG_STRING + OPEN_B + timeTagIntName + CLOSE_B;

        if (LOG_INFO)
            Log.i(TAG, "Config = " + config);

        SmartProfileConfig profileConfig = new SmartProfileConfig(config);
        profileConfig.addNameValuePair(CONFIG_VERSION, TIMEFRAME_CONFIG_VERSION);

        intent.putExtra(EXTRA_CONFIG, profileConfig.getConfigString());
        intent.putExtra(EXTRA_DESCRIPTION, descriptionBuilder.toString());
        if (LOG_DEBUG)
            Log.d(TAG, "Complete Intent is " + intent.toUri(0));
        setResult(RESULT_OK, intent);
    }

    /**
     * Scans through the list of time frames selected (mIntTimeSelected)and
     * removes the deleted time frames from the selection list.Updates the
     * mIntTimeSelected, to have only valid time frames. There is no need for a
     * parameter here
     */
    private void updateSelectedListOfTimeFrames() {
        TimeFrames modes = new TimeFrames().getData(mContext);

        if (modes == null) {
            Log.e(TAG, "getData returned zero modes");
            return;
        }
        // loop through all the time frames and check if the time frame selected
        // is present in the
        // available time frames
        for (int i = 0; i < mIntTimeSelected.size(); i++) {
            TimeFrame mode = modes.getTimeFrameByInternalName(mIntTimeSelected
                             .get(i));
            if (mode == null) {
                if (LOG_DEBUG)
                    Log.d(TAG, mIntTimeSelected.get(i)
                          + " seems to be deleted."
                          + " Removing from selection");
                mIntTimeSelected.remove(i);
                // since a time frame is removed, adjust the position
                i--;
            } else {
                if (LOG_DEBUG)
                    Log.d(TAG, mIntTimeSelected.get(i) + " found");
            }
        }
    }

    // define the handler to receive the asynchronous message after virtual
    // sensor string creation
    private Handler mHandler = new Handler() {
        // messages will be sent to this handler only by a single thread. just
        // finish in that case
        public void handleMessage(Message msg) {
            finish();
        }
    };

    /**
     * Helper class to get the vsensor strings for the user selected time frames
     *
     * CLASS - Implements Runnable
     *
     * USAGE - Instantiate this class whenever there is a need to create vsensor
     * strings
     */
    private final class GetConfigStrings implements Runnable {
        private TimeList mTimeSelected = new TimeList();
        public GetConfigStrings(TimeList timeSelected) {
            for (int i = 0; i < timeSelected.size(); i++) {
                mTimeSelected.add(timeSelected.get(i));
            }
        }
        // @Override
        public void run() {
            setConfigureResult(mTimeSelected);
            Message responseMsg = Message.obtain();
            responseMsg.setTarget(mHandler);
            responseMsg.sendToTarget();
        }
    }

    /**
     * Handles the click of items via a physical keyboard in a list row.
     */
    @Override
    protected void onListItemClick(ListView list, View view, int position,
                                   long id) {
        mTimeFramesCursor.moveToPosition(position);
        KeyValues clickedRowInfo = new KeyValues();
        clickedRowInfo.timeName = mTimeFramesCursor.getString(mTimeFramesCursor
                                  .getColumnIndex(TimeFrameTableColumns.NAME));
        clickedRowInfo.internalName = mTimeFramesCursor
                                      .getString(mTimeFramesCursor
                                              .getColumnIndex(TimeFrameTableColumns.NAME_INT));

        if (mShowCheckBox) {
            handleCheckBoxListItemSelection(view, clickedRowInfo);
        } else {
            // if this activity is not for the rules builder, then touching a
            // time frame row
            // lets the user to edit the time frame. so start the edit activity
            Intent intent = new Intent(TIME_FRAME_EDIT_INTENT);
            intent.putExtra(EXTRA_FRAME_NAME, clickedRowInfo.timeName);
            startActivityForResult(intent, 0);
        }
    }

    /**
     * onClick()
     */
    public void onClick(View view) {
        KeyValues clickedRowInfo = (KeyValues) ((View) view.getParent())
                                   .getTag();
        int viewId = view.getId();

        if (viewId == R.id.left_wrapper && view instanceof RelativeLayout) {
            // if this activity is not for the rules builder, then touching a
            // time frame row
            // lets the user to edit the time frame. so start the edit activity
            Intent intent = new Intent(TIME_FRAME_EDIT_INTENT);
            intent.putExtra(EXTRA_FRAME_NAME, clickedRowInfo.timeName);
            startActivityForResult(intent, 0);
        } else if (viewId == R.id.right_wrapper && view instanceof LinearLayout) {
            if (mShowCheckBox) {
                handleCheckBoxListItemSelection(view, clickedRowInfo);
            }
        }

    }

    /**
     * handles the selection of list items when the check box is being
     * displayed.
     *
     * @param view
     *            - View item
     * @param clickedRowInfo
     *            - tag info of the row selected
     */
    private void handleCheckBoxListItemSelection(View view,
            KeyValues clickedRowInfo) {
        // if this activity is for rules builder, then every touch on the row
        // should
        // toggle the state of the check box. accordingly the time frame should
        // be included
        // or excluded from the selection
        CheckBox checkBox = (CheckBox) view
                            .findViewById(R.id.placelist_checkbox);

        KeyValues info = clickedRowInfo;

        // if the check box is not checked already, make it to "checked" and
        // include the
        // time frame in the selection
        if (!checkBox.isChecked()) {
            mIntTimeSelected.add(info.internalName);
            if (LOG_DEBUG)
                Log.d(TAG, "Checked " + info.timeName);
            checkBox.setChecked(true);
        } else {
            // remove the time frame from the selection
            mIntTimeSelected.remove(info.internalName);
            if (LOG_DEBUG)
                Log.d(TAG, "UnChecked " + info.timeName);
            checkBox.setChecked(false);
        }
        view.setTag(clickedRowInfo);
        if (LOG_DEBUG) {
            Log.d(TAG, "mIntTimeSelected = " + mIntTimeSelected.toString());
        }
        // enable the OK button, only if the user has made some selection
        if (mIntTimeSelected.size() > 0) {
            // If there is at least one selection, enable OK button
            setupActionBarItemsVisibility(true);
        } else {
            setupActionBarItemsVisibility(false);
        }
    }

    private KeyValues rowValues = null;

    /**
     * onCreateContextMenu()
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenuInfo menuInfo) {

        rowValues = (KeyValues) ((View) view.getParent()).getTag();
        if (rowValues != null) {

            // Sets the menu header to be the title of the selected note.
            menu.setHeaderTitle(TimeUtil.getTranslatedTextForId(mContext,
                                rowValues.timeName));

            menu.add(0, MenuOptions.EDIT, 0, R.string.edit);
            menu.add(0, MenuOptions.DELETE, 0, R.string.delete);
        }
    }

    /**
     * onContextItemSelected()
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MenuOptions.EDIT:
            Intent editActivity = new Intent(TIME_FRAME_EDIT_INTENT);
            editActivity.putExtra(EXTRA_FRAME_NAME, rowValues.timeName);
            this.startActivityForResult(editActivity, 0);
            return true;

        case MenuOptions.DELETE:
            if (LOG_INFO)
                Log.i(TAG, "Delete selected");
            if (deleteTimeFrame(rowValues.timeName)) {
                displayTimeframesList();
                TimeUtil.exportTimeFrameData(mContext);
            }
            return true;

        default:
            return super.onContextItemSelected(item);
        }
    }

}
