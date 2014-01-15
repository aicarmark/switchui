/*
 * Copyright (C) 2010-2012, Motorola, Inc,
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * MXDN83        2012/06/17 Smart Actions 2.1 Created file
 * XPR643        2012/08/07 Smart Actions 2.2 New architecture for data I/O
 */

package com.motorola.contextual.pickers.conditions.timeframe;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartprofile.SmartProfileConfig;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrame;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrameConstants;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrameXmlSyntax;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrames;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeUtil;
import com.motorola.contextual.smartrules.R;

/**
 * This activity presents timeframes list fragment.
 * <code><pre>
 *
 * CLASS:
 *  extends MultiScreenPickerActivity - activity base class for pickers
 *
 * RESPONSIBILITIES:
 *  Launches the timeframes list fragment.
 *  This activity can be launched from the rule builder and
 *  from the user profile screen. If launched from rule builder,
 *  it sends the chosen timeframe back to the rule builder activity.
 *
 * COLLABORATORS:
 *  N/A
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class TimeFrameActivity extends MultiScreenPickerActivity
    implements TimeFrameConstants, TimeFrameXmlSyntax{

    /**
     * Class for better readability
     */
    public static class TimeList extends ArrayList<String> {
        private static final long serialVersionUID = -1941007612710493271L;
    };

    private static final String MODE_STRING="mode";
    private static final String TAG = TimeFrameActivity.class.getSimpleName();
    // intent to show the list of time frames
    // cjd - don't default visibility - make this private if not used elsewhere. also make it 'static final'.
    String TIMEFRAME_LIST_INTENT = "com.motorola.contextual.timeframeslist";

    //Boolean to differentiate between picker and profile list modes
    private boolean mProfileListMode = true;
    private PickerFragment mTimeFramesListFragment;
    private TimeList mIntTimeSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.time_frames_title));

        mInputConfigs = getIntent();
        if(mInputConfigs == null) {
            return;
        }
        if(mInputConfigs.getCategories() == null) {
            String action = mInputConfigs.getAction();
            if ((action != null) && !(action.equals(TIMEFRAME_LIST_INTENT))) {
                //Launched from rules builder
                mProfileListMode = false;
            }
            mInputConfigs.putExtra(MODE_STRING, mProfileListMode);
            mTimeFramesListFragment = TimeFramesListFragment.newInstance(mInputConfigs, mOutputConfigs);
            this.launchNextFragment(mTimeFramesListFragment, R.string.timeframes, true);
        }
    }

    @Override
    public void onReturn(Object returnValue, PickerFragment fromFragment) {
        if(fromFragment == mTimeFramesListFragment) {
            if(returnValue instanceof Intent) {
                mIntTimeSelected = (TimeList) mOutputConfigs
                        .getStringArrayListExtra(INT_CURRENT_SELECTION);
                if(mIntTimeSelected != null) {

                    // Check for empty strings in mIntTimeSelected
                    // String ArrayList and remove those entries
                    Iterator<String> iter = mIntTimeSelected.iterator();
                    while (iter.hasNext()) {
                       String s = iter.next();
                       if (s.isEmpty()) {
                           iter.remove();
                       }
                    }

                    if (mIntTimeSelected.size() == 0) {
                        Toast.makeText(this, "No selection. Select a time frame",
                                       Toast.LENGTH_SHORT).show();
                    } else {
                        // Getting the virtual sensor strings involve querying database
                        // for the configured time frames. So do that in a different
                        // thread to avoid any ANR
                        Thread configThread = new Thread(new GetConfigStrings(mIntTimeSelected));
                        // The thread is needed here to composes the VSENSOR string
                        // required by the rules builder.
                        // To compose the VSESNOR it has to do database lookups to get
                        // the time frame details. All these are done as part of
                        // setConfigureResult(). This is the reason for running this in a
                        // separate thread.
                        configThread.start();
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.menu_add: {
                if (mTimeFramesListFragment instanceof TimeFramesListFragment) {
                    final TimeFramesListFragment fragment = (TimeFramesListFragment)mTimeFramesListFragment;
                    fragment.showEditTimeFrame(null); // null because new timeframe screen
                }
            }
            break;
        }
        return result;
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
        TimeFrames timeFrames = new TimeFrames().getData(this);
        if (timeFrames == null) {
            // this should never happen. This method would never be called
            // if the user cannot select anything
            Log.e(TAG, "No timeframes to select");
            return;
        }

        int size = intTimeSelected.size();
        // construct an OR separated list of all the time frames that the
        // user selected
        if (size > 1) {
            // loop is run for .size - 1, since only for those many times
            // "OR" should be added
            // the last element is handled separately outside the loop
            for (i = 0; i < size - 1; i++) {
                String name = intTimeSelected.get(i);
                // timeTag Name to hold the "understandable" name list
                String friendlyName = TimeUtil.getTranslatedTextForId(
                                          this,
                                          timeFrames.getFriendlyNameForTimeFrame(name));
                if (friendlyName != null)
                    timeTagName.append(friendlyName).append(BLANK_SPC)
                    .append(getString(R.string.or))
                    .append(BLANK_SPC);
                // timeTagIntName to hold the internal name list
                timeTagIntName.append(name).append(OR_STRING);
            }

            if (LOG_DEBUG) { Log.d(TAG, "Results for size = " + size); }

            String friendlyName = TimeUtil.getTranslatedTextForId(this,
                                  timeFrames.getFriendlyNameForTimeFrame(
                                          intTimeSelected.get(i)));
            if (friendlyName != null)
                timeTagName.append(friendlyName);
            timeTagIntName.append(intTimeSelected.get(i));
        } else if (size == 1) {
            // if there is only one time frame selected by the user no
            // "OR"ing required
            String friendlyName = TimeUtil.getTranslatedTextForId(this,
                                  timeFrames.getFriendlyNameForTimeFrame(
                                          intTimeSelected.get(0)));
            if (friendlyName != null)
                timeTagName.append(friendlyName);
            timeTagIntName.append(intTimeSelected.get(0));
        } else {
            // Save/OK is disabled if the user hasn't selected anything
            if (LOG_DEBUG) { Log.d(TAG, "No selection. This should never happen"); }
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

    /** cjd - is this method ever used?
     * Creates the VSENSOR string for the time frames selected by the user
     *
     * @param timeFrames
     *            - List of time frames the user has selected
     * @return VSENSOR string
     */
    private String createSingleOrMultipleTimeVsmXml(ArrayList<String> timeFrames) {
        String resXml;
        StringBuilder resXmlBuf = new StringBuilder();
        String currSelection;

        if (LOG_DEBUG) Log.d(TAG, "Creating Multiple Time VSM");

        for (int i = 0; i < timeFrames.size(); i++) {
            currSelection = timeFrames.get(i);
            resXmlBuf = resXmlBuf.append(getXmlForTimeFrame(currSelection));
        }
        resXml = resXmlBuf.toString();
        if (LOG_DEBUG) Log.d(TAG, "XML String generated for the condition is  "+ "<VSENSOR>" + resXml + "</VSENSOR>");
        return VSENSOR_TAG_START + resXml + VSENSOR_TAG_END;
    }

    /**
     * Creates the VSENSOR string for a given time frame
     *
     * @param currSelection
     *            - Time frames name (internal name)
     * @return VSENSOR string
     */
    private String getXmlForTimeFrame(String currSelection) {
        if (LOG_DEBUG) Log.d(TAG, "Generating xml for vsesnor");
        // read all the time frames into the list
        TimeFrames timeFrames = new TimeFrames().getData(this);
        TimeFrame timeFrame = null;
        // get the container for the selected time frame
        if (timeFrames != null) {
            timeFrame = timeFrames.getTimeFrameByInternalName(currSelection);
        }
        // the time frame is not expected to be null. But just in case it is
        // null, log error
        if (timeFrame == null) {
            Log.e(TAG, "Null timeFrame Name");
            return null;
        }

        // get the rules for the selected time frame
        String rules[] = timeFrame.getDvsRules(VIRTUAL_SENSOR_STRING
                                               + currSelection);

        // Generate the rule set for this time frame
        StringBuilder ruleSet = new StringBuilder();
        for (int i = 0; i < rules.length; i++) {
            ruleSet.append(RULE_TAG_START).append(rules[i])
            .append(RULE_TAG_END);
        }
        if (LOG_DEBUG) Log.d(TAG, "Generated Rule Set is : " + ruleSet.toString());

        // construct the xml for vsensor
        StringBuilder frameXmlBuilder = new StringBuilder()
        .append(VIRTUAL_SENSOR_TAG_START).append(VIRTUAL_SENSOR_NAME)
        .append(currSelection).append(QUOTE_ESCAPE).append(SPACE)
        .append(SENSOR_PERSISTENCE).append(VALUE_PERSISTENCE)
        .append(VENDOR).append(VERSION_NO).append(DESCRIPTION)
        .append(INITIAL_VALUE).append(PERMISSIONS_START_TAG)
        .append(POSSIBLE_VALUE_TRUE)
        .append(PERMISSIONS_ATTRIBUTE_END_TAG).append(PERM_START_TAG)
        .append(PERM_NAME).append(PERM_END_TAG)
        .append(PERMISSIONS_END_TAG).append(POSSIBLE_VALUES)
        .append(RULE_MECHANISM).append(RULE_SET_TAG_START)
        .append(ruleSet.toString()).append(RULE_SET_TAG_END)
        .append(VIRTUAL_SENSOR_TAG_END);

        // now, convert the string builder to string
        String frameXml = frameXmlBuilder.toString();

        if (LOG_INFO) Log.i(TAG, "XML generated for sensor " + currSelection + " is " + frameXml);
        return frameXml;
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
}
