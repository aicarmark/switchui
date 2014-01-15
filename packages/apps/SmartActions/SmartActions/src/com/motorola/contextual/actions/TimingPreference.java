/*
 * @(#)TimingPreference.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2010/02/21   NA               Initial Version
 *
 */

package com.motorola.contextual.actions;

import com.motorola.contextual.smartrules.R;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;

/** This class extends the basic ListPreference to let the user select the Timing Preference for
 *  executing the action, whether the action has to be executed in the beginning or at the end
 *  of rule.
 *
 * CLASS:
 *  Extends ListPreference that displays a list of entries as a dialog.
 *
 * RESPONSIBILITIES:
 *  1. Displays the list of Timing Preferences for executing the action, whether the action has to be
 *     executed in the beginning or at the end of rule.
 *
 * COLABORATORS:
 *  extends ListPreference
 *
 * USAGE:
 *  See each method.
 *
 *
 */
public class TimingPreference extends ListPreference implements Constants {

	String[] mValues;
	int mSelectedPos = 0;

	private static final String TAG =  TimingPreference.class.getSimpleName();

    public TimingPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mValues = new String[] {
        		context.getString(R.string.timing_beginning),
        		context.getString(R.string.timing_end)
            };

        setDialogIcon(R.drawable.ic_dialog_menu_generic);
        setEntries(mValues);
        setEntryValues(mValues);
        setSummary(mValues[mSelectedPos]);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            setSummary(mValues[mSelectedPos]);
        }
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        CharSequence[] entries = getEntries();

        builder.setTitle(R.string.Timing);

        builder.setSingleChoiceItems(entries, mSelectedPos, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int pos) {

            	if (LOG_INFO) Log.i(TAG, "onClick called :" + pos + ":" + mValues[pos]);
                mSelectedPos = pos;
                setSummary(mValues[mSelectedPos]);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel,
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            	dialog.dismiss();

            }
        });

        builder.setPositiveButton(null, null);

    }

    /**
     * Set the user Timing selection when the user tries to edit the rule
     * @param selection - The user Timing selection
     */
    public void setSelection(boolean selection) {

    	mSelectedPos = (selection == true) ? 1 : 0;
    	if (LOG_DEBUG) Log.d(TAG, "setSelection :" + mSelectedPos);
        setSummary(mValues[mSelectedPos]);
    }

    /**
     * Set the application specific summary value descriptions
     * @param values - application specific summary values
     */
    public void setSummaryValues(String[] values) {

    	mValues[0] = values[0];
    	mValues[1] = values[1];
    	setEntries(mValues);
        setEntryValues(mValues);
    	setSummary(mValues[mSelectedPos]);
    }


    /**
     * Retrieves the last user Timing selection
     * @return - The last user Timing selection
     */
    public boolean getSelection() {
    	if (LOG_DEBUG) Log.d(TAG, "getSelection :" + mSelectedPos);
        return ((mSelectedPos == 1) ? true : false);
    }
}

