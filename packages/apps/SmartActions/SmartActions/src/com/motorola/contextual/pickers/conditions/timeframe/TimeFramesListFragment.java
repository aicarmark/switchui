/*
 * Copyright (C) 2010-2012, Motorola, Inc,
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * MXDN83        2012/06/18 Smart Actions 2.1 Created file
 * XPR643        2012/08/07 Smart Actions 2.2 New architecture for data I/O
 */

package com.motorola.contextual.pickers.conditions.timeframe;

import java.util.ArrayList;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.motorola.contextual.pickers.CustomListAdapter;
import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.pickers.conditions.location.LocationActivity;
import com.motorola.contextual.pickers.conditions.timeframe.TimeFrameActivity.TimeList;
import com.motorola.contextual.smartprofile.SmartProfileConfig;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrame;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrameConstants;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrameDBAdapter;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrameDaysOfWeek;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrameTableColumns;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrames;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeUtil;
import com.motorola.contextual.smartrules.R;

/**
 * This fragment displays the list of timeframes from the database
 *
 *<code><pre>
 * CLASS:
 *  extends PickerFragment.
 *
 *  implements
 *      Constants - for the constants used
 *      TimeFrameConstants - timeframe specific constants
 *      DialogInterface.OnClickListener - done btn click listener
 *      View.OnClickListener - click listener for 'Add a location'
 *
 * RESPONSIBILITIES:
 *  displays the list of poi tagged locations.
 *
 * COLABORATORS:
 *  None
 *
 * USAGE:
 *  None
 *</pre></code>
 **/
public class TimeFramesListFragment extends PickerFragment implements
TimeFrameConstants, OnClickListener, android.content.DialogInterface.OnClickListener,
OnMultiChoiceClickListener {

    protected static final String TAG = TimeFramesListFragment.class.getSimpleName();
    public static int EDIT_TIMEFRAME_REQ_CODE = 1;
    public static int ADD_TIMEFRAME_REQ_CODE = 2;
    private ArrayList<ListItem> mItems;
    private int mListType;
    private boolean[] mCheckedItems;
    private ListView mListView;
    /**
     * comma separated time frame names, selected earlier, passed by the puzzle
     * builder
     */
    private String mPassedTimeframeInternalNames;
    /** List of time frames - internal name from the current selection */
    private final TimeList mIntTimeSelected = new TimeList();
    /** Database adapter */
    private TimeFrameDBAdapter mDbAdapter;
    private Cursor mTimeFramesCursor;

    //Config intent constants
    private static final String INPUT_CONFIGS_INTENT = "INPUT_CONFIG_INTENT";
    private static final String OUTPUT_CONFIGS_INTENT = "OUTPUT_CONFIG_INTENT";

    /**
     * All subclasses of Fragment must include a public empty constructor. The framework
     * will often re-instantiate a fragment class when needed, in particular during state
     * restore, and needs to be able to find this constructor to instantiate it. If the
     * empty constructor is not available, a runtime exception will occur in some cases
     * during state restore.
     */
    public TimeFramesListFragment() {
        super();
    }

    /**
     * The true factory-style constructor. Forwards the passed in intents to onCreate.
     *
     * @param mInputConfigs - used to pass data to this fragment
     * @param mOutputConfigs - used to pass data to the host activity
     */
    public static TimeFramesListFragment newInstance(final Intent inputConfigs, final Intent outputConfigs) {
        Bundle args = new Bundle();

        if (inputConfigs != null) {
            args.putParcelable(INPUT_CONFIGS_INTENT, inputConfigs);
        }

        if (outputConfigs != null) {
            args.putParcelable(OUTPUT_CONFIGS_INTENT, outputConfigs);
        }

        TimeFramesListFragment f = new TimeFramesListFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (getArguments().getParcelable(INPUT_CONFIGS_INTENT) != null) {
                mInputConfigs = (Intent) getArguments().getParcelable(INPUT_CONFIGS_INTENT);
            }

            if (getArguments().getParcelable(OUTPUT_CONFIGS_INTENT) != null) {
                mOutputConfigs = (Intent) getArguments().getParcelable(OUTPUT_CONFIGS_INTENT);
            }
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // mInputConfigs could be null (IKJBREL1-4044).  The fragment's empty
        // constructor is called and mInputConfigs is not initialized.  OnCreateView
        // is called twice with a valid mInputConfigs in the 2nd time.  So the UI still
        // looks fine.
        if(mContentView == null && mInputConfigs != null) {
            final Picker.Builder pickerBuilder = new Picker.Builder(mHostActivity);
            String title = "";
            //Check the mode, true - coming from profile, false - from the picker list
            if(mInputConfigs.getBooleanExtra(LocationActivity.MODE_STRING, false)) {
                mListType = ListItem.typeTHREE;
                title = getString(R.string.timeframes_secondary_text);
            } else {
                mListType = ListItem.typeTWO;
                pickerBuilder.setPositiveButton(R.string.iam_done, this);
                title = getString(R.string.timeframe_prompt);
            }
            pickerBuilder.setTitle(Html.fromHtml(title));
            // Extract the useful extras coming in the intent. These are
            // required for showing the previous user selections
            mPassedTimeframeInternalNames = mInputConfigs.getStringExtra(INT_CURRENT_SELECTION);

            String config = mInputConfigs.getStringExtra(EXTRA_CONFIG);
            SmartProfileConfig profileConfig = new SmartProfileConfig(config);

            String value = profileConfig.getValue(TIMEFRAME_NAME);

            mPassedTimeframeInternalNames = ((config != null) && (config.contains(TIMEFRAME_CONFIG_STRING)) && (value != null)) ?
                    TimeUtil.trimBraces(value) : null;

                    if (LOG_DEBUG) {
                        Log.d(TAG, " mPassedTimeframeIntTags = "
                                + mPassedTimeframeInternalNames);
                    }

                    // if there is/are earlier selections, extract them. time frames
                    // names are separated by OR. e.g If the user has earlier selected two time frames
                    // named Morning and Weekend,then mPassedTimeframeTags will have Morning OR Weekend.
                    if ((mPassedTimeframeInternalNames != null)
                            && (mPassedTimeframeInternalNames.length() != 0)) {
                        final String[] passedIntTimeframeTagssArray = mPassedTimeframeInternalNames
                                .split(OR_STRING);
                        // add the selections in the list
                        for (int i = 0; i < passedIntTimeframeTagssArray.length; i++) {
                            mIntTimeSelected.add(passedIntTimeframeTagssArray[i]);
                        }
                    }
                    // It is possible that the time frames used in the rule has been
                    // deleted now. So remove those time frames from selection during editing
                    updateSelectedListOfTimeFrames();

                    buildListItems(true);
                    pickerBuilder.setMultiChoiceItems(mItems, mCheckedItems, this);
                    final Picker picker = pickerBuilder.create();
                    mContentView = picker.getView();
                    mListView = (ListView) mContentView.findViewById(R.id.list);
        }
        return mContentView;
    }

    /**
     * Gets the cursor, builds the mItems to be passed onto the list view, called the
     * first time and whenever there's a change in the content
     */
    private void buildListItems(final boolean firstTime) {
        final String newLineChar = "<br>";
        mItems = new ArrayList<ListItem>();
        if (mDbAdapter == null) {
            mDbAdapter = new TimeFrameDBAdapter(mHostActivity);
        }
        // TODO cjd - this cursor acquisition must be done in a non-UI thread, AsyncTask or whatever
        mTimeFramesCursor = mDbAdapter.getVisibleTimeframes();
        if (mTimeFramesCursor != null) {
            try {
                //create the array of cursor's size+1 to accommodate the 'add new timeframe'
                //action item at the bottom, even though it's not semantically included
                //in the list of selectable timeframes so adapter doesn't throw
                //index out of bounds exception
                mCheckedItems = new boolean[mTimeFramesCursor.getCount()+1];
                if (mTimeFramesCursor.moveToFirst()) {
                    int i=0;
                    final int nameIndex=mTimeFramesCursor.getColumnIndex(TimeFrameTableColumns.NAME), internalNameIndex=mTimeFramesCursor.getColumnIndex(TimeFrameTableColumns.NAME_INT), allDayIndex = mTimeFramesCursor.getColumnIndex(TimeFrameTableColumns.ALL_DAY), startTimeIndex = mTimeFramesCursor.getColumnIndex(TimeFrameTableColumns.START), endTimeIndex = mTimeFramesCursor.getColumnIndex(TimeFrameTableColumns.END), daysOfWeekIndex = mTimeFramesCursor.getColumnIndex(TimeFrameTableColumns.DAYS_OF_WEEK);
                    StringBuilder descStr;
                    String internalName;
                    do {
                        try {
                            final String name = mTimeFramesCursor.getString(nameIndex); //This is the untranslated name that we need in order to get the right time from from adapter
                            final String translatedName = TimeUtil.getTranslatedTextForId(mHostActivity, name);//This is the pretty name
                            internalName = mTimeFramesCursor.getString(internalNameIndex);
                            //Build the first line in the description string, which is time
                            descStr = new StringBuilder();
                            if( mTimeFramesCursor.getString(allDayIndex).equalsIgnoreCase(ALL_DAY_FLAG_TRUE)) {
                                // if its an all day time frame, display the string accordingly
                                descStr.append(getString(R.string.all_day_event));
                            } else {
                                // if its not an all day time frame, format the start time and
                                // time and display
                                String text1, text2;
                                text1 = mTimeFramesCursor.getString(startTimeIndex);
                                text2 = mTimeFramesCursor.getString(endTimeIndex);
                                final String dt1 = TimeUtil.getDisplayTime(mHostActivity, text1);
                                final String dt2 = TimeUtil.getDisplayTime(mHostActivity, text2);
                                descStr.append(dt1 + getString(R.string.time_seperator) + dt2);
                            }
                            descStr.append(newLineChar);
                            //Build the second line in the description string, which is day of the week
                            final int daysOfWeek = mTimeFramesCursor.getInt(daysOfWeekIndex);
                            final String days = new TimeFrameDaysOfWeek(daysOfWeek)
                            .toCommaSeparatedString(mHostActivity, true);
                            descStr.append(days);
                            mItems.add(new ListItem(-1, translatedName, descStr.toString(), mListType,
                                    internalName, new View.OnClickListener() {
                                public void onClick(final View v) {
                                    showEditTimeFrame(name);
                                }
                            }));
                            if(mIntTimeSelected.contains(internalName)) {
                                mCheckedItems[i] = true;
                            } else {
                                mCheckedItems[i] = false;
                            }
                            i++;
                        }catch(final Exception e) {
                            //should never happen but having a guard anyways looking at
                            //some of the bizzare null database column values
                            e.printStackTrace();
                        }
                    } while(mTimeFramesCursor.moveToNext());

                }else {
                    // TODO display that there are no entries in the table,show toast for now
                    //showErrorMessage(TIME_ZERO_ROWS);
                    Toast.makeText(mHostActivity, TIME_ZERO_ROWS, Toast.LENGTH_LONG);
                }
                if(!firstTime) {
                    final CustomListAdapter adapter = (CustomListAdapter)mListView.getAdapter();
                    adapter.setItemsList(mItems);
                    adapter.setCheckedItems(mCheckedItems);
                    adapter.notifyDataSetChanged();
                }
            } catch (final Exception e) {
            } finally {
                mTimeFramesCursor.close();
            }
        }else {
            Log.e(TAG, NULL_CURSOR);
            //showErrorMessage(NULL_CURSOR);
        }
    }

    /**
     * Shows the edit time frame fragment
     *
     * @param name - time frame name
     */
    protected void showEditTimeFrame(final String name) {
        // lets the user to add/edit the time frame. so start the edit activity
        int reqCode = EDIT_TIMEFRAME_REQ_CODE;
        if(name == null) {
            //Adding a new timeframe, update the request code
            reqCode = ADD_TIMEFRAME_REQ_CODE;
        }
        final Intent intent = new Intent(TIME_FRAME_EDIT_INTENT);
        intent.putExtra(EXTRA_FRAME_NAME, name);
        startActivityForResult(intent, reqCode);
    }

    /**
     * Scans through the list of time frames selected (mIntTimeSelected)and
     * removes the deleted time frames from the selection list.Updates the
     * mIntTimeSelected, to have only valid time frames. There is no need for a
     * parameter here
     */
    private void updateSelectedListOfTimeFrames() {
        final TimeFrames modes = new TimeFrames().getData(mHostActivity);

        if (modes == null) {
            return;
        }
        // loop through all the time frames and check if the time frame selected
        // is present in the available time frames
        for (int i = 0; i < mIntTimeSelected.size(); i++) {
            final TimeFrame mode = modes.getTimeFrameByInternalName(mIntTimeSelected
                    .get(i));
            if (mode == null) {
                // since a time frame is not found, remove and adjust the position
                mIntTimeSelected.remove(i);
                i--;
            }
        }
    }

    /**
     * Handles 'Add a time frame' click event
     * Required by android.view.View.OnClickListener interface
     */
    public void onClick(final View v) {
        //No longer it is part of list showEditTimeFrame(null);
    }

    /**
     * Handles the bottom done button onclick event
     * Returns the time frame list of currently selected items back
     * to the host activity
     * Required by DialogInterface.onClickListener interface
     */
    public void onClick(final DialogInterface dialog, final int which) {
        mOutputConfigs.putExtra(INT_CURRENT_SELECTION, mIntTimeSelected);
        mHostActivity.onReturn(mOutputConfigs, this);
    }

    /**
     * Handles list item click/select events - updates the selected list
     * of timeframes based on isChecked true/false
     * Required by DialogInterface.onMultiChoiceClickListener interface
     */
    public void onClick(final DialogInterface dialog, final int which, final boolean isChecked) {
        if(isChecked) {
            mIntTimeSelected.add((String)mItems.get(which).mMode);
        }else {
            mIntTimeSelected.remove(mItems.get(which).mMode);
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if(requestCode == EDIT_TIMEFRAME_REQ_CODE && resultCode == Activity.RESULT_OK) {
            //Timeframe edited or deleted - either case rebuild list items
            buildListItems(false);
        }else if(requestCode == ADD_TIMEFRAME_REQ_CODE && resultCode == Activity.RESULT_OK) {
            if(intent != null) {
                //Add the new time frame so it'll be checked
                final String internalName = intent.getStringExtra(CURRENT_SELECTION);
                mIntTimeSelected.add(internalName);
            }
            buildListItems(false);
        }
    }
}
