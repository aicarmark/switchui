/*
 * @(#)TimeFrameTimePreference.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21383       2010/05/12   NA               Initial version
 *
 */

package com.motorola.contextual.smartprofile.sensors.timesensor;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;
import com.motorola.contextual.smartrules.R;

/** This class extends the basic DialogPreference to let the user select the time for the time frame
 * It also provides methods to get the user selected results in the form of integers
 *
 * CLASS:
 *  Extends DialogPreference that displays a TimePicker dialog.
 *
 * RESPONSIBILITIES:
 *  1. Displays a TimePicker dialog to user and lets the user select single/multiple dayss
 *
 * COLABORATORS:
 *  None
 *
 * USAGE:
 *  See each method.
 *
 *
 */
public class TimeFrameTimePreference extends DialogPreference {

    private TimePicker mTimePicker;
    private String mCurrentHour = null;
    private String mCurrentMinute = null;
    private Context mContext = null;
    private String mPrefKey = "time";
    private static final String INITIAL_HOUR = "00";
    private static final String INITIAL_MINUTE = "00";


    public TimeFrameTimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public TimeFrameTimePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context);

    }

    private void initialize(Context context) {
        mContext = context;
        setPositiveButtonText(R.string.set);
        setNegativeButtonText(R.string.cancel);

    }

    /**
     * This method sets the hour and minute variables.
     * @param hour
     * @param minute
     */
    public void setTime(String hour, String minute) {
        mCurrentHour = hour;
        mCurrentMinute = minute;
    }

    /**
     * Returns the hour
     * @return
     */
    public int getHour() {
        return Integer.parseInt(mCurrentHour);
    }

    /**
     * Returns the minute
     * @return
     */
    public int getMinute() {
        return Integer.parseInt(mCurrentMinute);
    }

    /**
     * Method to set SharedPreferences key to store start/end time
     * @param prefKey
     */
    public void setPrefKey(String prefKey) {
        mPrefKey = prefKey;
    }

    /**
     * Creates the content view for the dialog (if a custom content view is required).
     */
    @Override
    protected View onCreateDialogView() {
        mTimePicker = new TimePicker(getContext());
        mTimePicker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
        if(mCurrentHour != null && mCurrentMinute != null) {
            mTimePicker.setCurrentHour(Integer.parseInt(mCurrentHour));
            mTimePicker.setCurrentMinute(Integer.parseInt(mCurrentMinute));
        } else {
            mTimePicker.setCurrentHour(Integer.parseInt(INITIAL_HOUR));
            mTimePicker.setCurrentMinute(Integer.parseInt(INITIAL_MINUTE));
        }
        return mTimePicker;
    }


    /**
     * Called when the dialog is dismissed and should be used to save data to the SharedPreferences.
     */
    @Override
    public void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        mTimePicker.clearFocus();
        if (positiveResult) {
            mCurrentHour = mTimePicker.getCurrentHour().toString();
            mCurrentMinute = mTimePicker.getCurrentMinute().toString();
            setSummary(TimeUtil.getDisplayTime(mContext, mTimePicker.getCurrentHour(), mTimePicker.getCurrentMinute()));

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            if(preferences != null) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(mPrefKey, TimeUtil.getDbTime(Integer.parseInt(mCurrentHour), Integer.parseInt(mCurrentMinute)));
                editor.commit();
            }
        }
    }

}

