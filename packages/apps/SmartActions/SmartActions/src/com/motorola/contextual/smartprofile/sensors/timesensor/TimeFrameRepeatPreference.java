/*
 * @(#)TimeFrameRepeatPreference.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a15776       2010/02/21   NA               Incorporated review comments
 * a15776       2011/02/01   NA               Initial Version
 *
 */

package com.motorola.contextual.smartprofile.sensors.timesensor;

import android.app.AlertDialog.Builder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.widget.Button;
import android.os.Bundle;

import java.text.DateFormatSymbols;
import java.util.Calendar;

import com.motorola.contextual.smartrules.R;

/** This class extends the basic ListPreference to let the user select the days for the time frame
 * It also provides methods to get the user selected results in the form of easily readable strings
 *
 * CLASS:
 *  Extends ListPreference that displays a list of entries as a dialog.
 *
 * RESPONSIBILITIES:
 *  1. Displays the list of days in the week, to user and let the user select single/multiple days
 *
 * COLABORATORS:
 *  None
 *
 * USAGE:
 *  See each method.
 *
 * NOTE: Most part of the code for this class is taken from the Alarm Application
 *
 */
public class TimeFrameRepeatPreference extends ListPreference implements TimeFrameConstants {
    // Initial value that can be set with the values saved in the database.
    private TimeFrameDaysOfWeek mDaysOfWeek = new TimeFrameDaysOfWeek(ALLDAY);
    // New value that will be set if a positive result comes back from the dialog.
    private TimeFrameDaysOfWeek mNewDaysOfWeek = new TimeFrameDaysOfWeek(ALLDAY);

    public TimeFrameRepeatPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        String[] weekdays = new DateFormatSymbols().getWeekdays();
        String[] values = new String[] {
            weekdays[Calendar.MONDAY],
            weekdays[Calendar.TUESDAY],
            weekdays[Calendar.WEDNESDAY],
            weekdays[Calendar.THURSDAY],
            weekdays[Calendar.FRIDAY],
            weekdays[Calendar.SATURDAY],
            weekdays[Calendar.SUNDAY],
        };
        setEntries(values);
        setEntryValues(values);
        setSummary(mDaysOfWeek.toCommaSeparatedString(getContext(), true));
        setPositiveButtonText(R.string.save);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            mDaysOfWeek.set(mNewDaysOfWeek);
            setSummary(mDaysOfWeek.toCommaSeparatedString(getContext(), true));
        }
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        CharSequence[] entries = getEntries();
        mNewDaysOfWeek.set(mDaysOfWeek);//rqnh68 each time open dialog, we can make new equals old
        builder.setMultiChoiceItems(
            entries, mDaysOfWeek.getBooleanArray(),
        new DialogInterface.OnMultiChoiceClickListener() {
            public void onClick(DialogInterface dialog, int which,
            boolean isChecked) {
                mNewDaysOfWeek.set(which, isChecked);

		// Disable 'save' button if none days are selected
		Button saveButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
		if (saveButton != null) {
		    if (mNewDaysOfWeek.getCoded() == TimeFrameDaysOfWeek.NODAY) {
			saveButton.setEnabled(false);
		    } else {
			saveButton.setEnabled(true);
		    }
		}
            }
        });
    }

    /**
     * Set the days of the week represented in the bit mask
     * @param dow - Bit mask representing the selected days of the week
     */
    public void setDaysOfWeek(TimeFrameDaysOfWeek dow) {
        mDaysOfWeek.set(dow);
        mNewDaysOfWeek.set(dow);
        setSummary(dow.toCommaSeparatedString(getContext(), true));
    }

    /**
     * Set the days of the week represented in the bit mask
     * @param daysOfWeek - Bit mask representing the selected days of the week
     */
    public void setDaysOfWeek(int daysOfWeek) {
        TimeFrameDaysOfWeek days = new TimeFrameDaysOfWeek(daysOfWeek);
        setDaysOfWeek(days);
    }

    /**
     * Retrieves the selected days of the week
     * @return - Bit mask representing the selected days of the week
     */
    public TimeFrameDaysOfWeek getDaysOfWeek() {
        return mDaysOfWeek;
    }

    @Override
    protected void showDialog (Bundle state) {
       super.showDialog(state);
       Button saveButton = ((AlertDialog)getDialog()).getButton(DialogInterface.BUTTON_POSITIVE);
       if (saveButton != null) {
           if (mDaysOfWeek.getCoded() == TimeFrameDaysOfWeek.NODAY) {
               saveButton.setEnabled(false);
           }
       }
    }

}

