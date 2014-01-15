/*
 * @(#)CalendarPreference.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * wkh346        2011/10/11 NA                Initial version
 *
 */

package com.motorola.contextual.pickers.conditions.calendar;

import com.motorola.contextual.smartrules.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.BaseAdapter;
import android.widget.Button;

/**
 * This class is responsible for displaying the calendar dialog and accepting
 * the user input on dialog
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Extends {@link ListPreference}
 *      Implements CalendarEventSensorConstants
 *
 * RESPONSIBILITIES:
 * This class is responsible for displaying the calendar dialog and accepting the user input on dialog
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class CalendarPreference extends ListPreference implements
    CalendarEventSensorConstants {

    /**
     * Reference to the adapter of calendar dialog
     */
    private BaseAdapter mAdapter;

    /**
     * Reference to the observer of calendar dialog
     */
    private CalendarDialogObserver mObserver;

    /**
     * Acitivity's context
     */
    private Context mContext;

    /**
     * Constructor
     *
     * @param context
     *            - Activity's context
     * @param attrs
     *            - layout parameters in form of attributes
     */
    public CalendarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    /**
     * Method for initializing the preference
     *
     * @param adapter
     *            - adapter for the dialog
     * @param observer
     *            - observer for the dialog
     */
    public void initialize(BaseAdapter adapter, CalendarDialogObserver observer) {
        mAdapter = adapter;
        mObserver = observer;
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        builder.setTitle(mContext.getString(R.string.calendars));
        builder.setPositiveButton(mContext.getString(R.string.save),
                                  mPositiveButtonListener);
        builder.setNegativeButton(mContext.getString(R.string.cancel),
                                  mNegativeButtonListener);
        builder.setOnKeyListener(mKeyListener);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

            public void onCancel(DialogInterface dialog) {
                // This will get called when user taps outside the dialog
                if (mObserver != null) {
                    mObserver.onNegativeButtonClickOnCalendarDialog();
                }
            }
        });
        builder.setAdapter(mAdapter, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                // nothing to be done here
            }
        });

    }

    /**
     * Dialog's positive button onClickListener
     */
    private DialogInterface.OnClickListener mPositiveButtonListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int which) {
            if (mObserver != null) {
                mObserver.onPositiveButtonClickOnCalendarDialog();
            }
            dialog.dismiss();
        }
    };

    /**
     * Dialog's negative button onClickListener
     */
    private DialogInterface.OnClickListener mNegativeButtonListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int which) {
            if (mObserver != null) {
                mObserver.onNegativeButtonClickOnCalendarDialog();
            }
            dialog.dismiss();

        }
    };

    /**
     * Dialog's onKeyListener
     */
    private DialogInterface.OnKeyListener mKeyListener = new DialogInterface.OnKeyListener() {

        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_SEARCH: {
                if (mObserver != null) {
                    mObserver.onNegativeButtonClickOnCalendarDialog();
                }
                dialog.dismiss();
                return true;
            }
            }
            return false;
        }
    };

    /**
     * Method for enabling and disabling the positive button of the dialog
     *
     * @param enabled
     *            - true if positive button has to be enabled, false otherwise
     */
    public void setPositiveButtonEnabled(boolean enabled) {
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            Button positiveButton = dialog
                                    .getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setEnabled(enabled);
            }
        }
    }

}
