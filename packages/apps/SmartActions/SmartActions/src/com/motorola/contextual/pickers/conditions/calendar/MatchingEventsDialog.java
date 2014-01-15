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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.CalendarContract.Instances;
import android.text.format.Time;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;

import com.motorola.contextual.smartrules.R;

import java.util.List;


/**
 * A wrapper class that encapsulates the Matching Events dialog that
 * shows the upcoming events that match the conditions of the rule
 * being edited.
 *
 * <code><pre>
 *
 * CLASS:
 *  implements CalendarEventSensorConstants
 *
 * RESPONSIBILITIES:
 * It mostly just setups the existing AgendaWindowAdapter with the
 * appropriate query from the rule's configuration.
 *
 * COLLABORATORS:
 *  Calendar - Manages information about available calendars
 *  CalendarActivity - the Activity hosting the  picker fragments
 *  CalendarEventConfigFragment - the fragment to configure the types of calendar events to use
 *  CalendarPickerFragment - the fragment to pick the specific calendar
 *  MatchingEventsDialog - the dialog that presents the matching events for the users to choose.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class MatchingEventsDialog implements CalendarEventSensorConstants {

    private final Context mContext;
    private Intent mConfig;
    private AgendaWindowAdapter mAdapter;
    private AlertDialog mDialog;

    /**
     * Constructor.
     *
     * @param context - application context
     * @param config - input configurations
     */
    public MatchingEventsDialog(Context context, Intent config) {
        mContext = context;
        mConfig = config;
        mAdapter = new AgendaWindowAdapter(mContext);
        buildDialog();
    }

    /**
     * Show this dialog.  Keeping it package-private.
     */
    void show() {
        refresh(true);
        mDialog.show();
    }

    private void buildDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(getString(R.string.calendar_events_matching_search));
        builder.setPositiveButton(getString(R.string.ok), null);
        builder.setAdapter(mAdapter, null);
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                switch (keyCode) {
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER: {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        if (mAdapter.getCurrentSelectedItemType() == AgendaWindowAdapter.CurrentItem.POSITIVE_BUTTON_ITEM) {
                            refresh(false);
                            mAdapter.onDialogDismiss();
                            dialog.dismiss();
                        } else {
                            mAdapter.onKeyPressed(keyCode, event);
                        }
                    }
                    return true;
                }
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN: {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        mAdapter.onKeyPressed(keyCode, event);
                    }
                    return false;
                }
                }
                return false;
            }
        });
        builder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long rowId) {
                mAdapter.onItemSelected(adapterView, view, position, rowId);
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                mAdapter.onItemSelected(adapterView, null, -1, -1);
            }
        });

        mDialog = builder.create();
        mAdapter.initialize(mDialog.getListView());
    }

    private void refresh(boolean force) {
        updateQuery();
        Time time = new Time(Time.getCurrentTimezone());
        time.set(System.currentTimeMillis());
        mAdapter.refresh(time, force);
    }

    private void updateQuery() {
        StringBuilder query = new StringBuilder();
        List<Integer> calendarIds = Calendar.getCalendarIds(mConfig.getStringExtra(EXTRA_CALENDARS));

        // Only include visible calendars
        query.append(Instances.VISIBLE).append("='").append(VISIBLE_CALENDARS_ONLY).append("'");

        // Select from only specified calendars
        if (calendarIds != null) {
            query.append(AND).append(LEFT_PAREN);
            boolean firstAdded = false;
            for (int id : calendarIds) {
                if (firstAdded) query.append(OR);
                query.append(Instances.CALENDAR_ID).append("='").append(id).append("'");
                firstAdded = true;
            }
            query.append(RIGHT_PAREN);
        }

        // Handle config options
        if (mConfig.getBooleanExtra(EXTRA_ALLDAY_EVENTS, false)) {
            query.append(AND).append(Instances.ALL_DAY).append("='").append(NOT_ALL_DAY_EVENT).append("'");
        }
        if (mConfig.getBooleanExtra(EXTRA_ACCEPTED_EVENTS, false)) {
            query.append(AND).append(Instances.SELF_ATTENDEE_STATUS).append("='").append(Instances.STATUS_CONFIRMED).append("'");
        }

        // Not sure why this is handled by the adapter itself, but
        // this was the way was set up
        mAdapter.setMultipleParticipants(mConfig.getBooleanExtra(EXTRA_MULTIPLE_PARTICIPANTS, false));

        // Update the adapter's query
        mAdapter.setQuerySelection(query.toString());
    }

    private CharSequence getString(int resourceStringId) {
        return mContext.getResources().getString(resourceStringId);
    }
}
