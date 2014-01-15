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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.ListView;

import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.pickers.conditions.calendar.CalendarEventUtils.ConfigData;
import com.motorola.contextual.smartrules.R;


/**
 * This fragment implements the picker to choose a specific calendar.
 *
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment
 *
 * RESPONSIBILITIES:
 * Implements the screen to pick a specific calendar.
 *
 * COLLABORATORS:
 *  Calendar - Manages information about available calendars
 *  CalendarActivity - the Activity hosting the calendar picker fragments
 *  CalendarEventConfigFragment - the fragment to configure the types of calendar events to use
 *  CalendarPickerFragment - the fragment to pick the specific calendar
 *  MatchingEventsDialog - the dialog that presents the matching events for the users to choose.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class CalendarPickerFragment extends PickerFragment implements CalendarEventSensorConstants, OnClickListener {

    private static List<Calendar> mCalendars;
    private List<Integer> mSelectedCalendarIds;
    private ListView mListView;

    //Config intent constants
    private static final String INPUT_CONFIGS_INTENT = "INPUT_CONFIG_INTENT";
    private static final String OUTPUT_CONFIGS_INTENT = "OUTPUT_CONFIG_INTENT";

    public interface CalendarPickerDelegate {
        List<Calendar> getCalendars();
    }
    
    private CalendarPickerDelegate mCalendarPickerDelegate;

    /**
     * Factory-style constructor.
     *
     * @param inputConfigs - the input configurations to this picker
     * @param outputConfigs - the output configurations from this picker
     */
    public static CalendarPickerFragment newInstance(final Intent inputConfigs, final Intent outputConfigs) {

        //Bundle up inputConfigs, outputConfigs to pass to onCreate
        Bundle args = new Bundle();
        if (inputConfigs != null) {
            args.putParcelable(INPUT_CONFIGS_INTENT, inputConfigs);
        }
        if (outputConfigs != null) {
            args.putParcelable(OUTPUT_CONFIGS_INTENT, outputConfigs);
        }
        CalendarPickerFragment f = new CalendarPickerFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set mInputConfigs and mOutputConfigs
        if (getArguments() != null) {
            if (getArguments().getParcelable(INPUT_CONFIGS_INTENT) != null) {
                mInputConfigs = (Intent) getArguments().getParcelable(INPUT_CONFIGS_INTENT);
            }

            if (getArguments().getParcelable(OUTPUT_CONFIGS_INTENT) != null) {
                mOutputConfigs = (Intent) getArguments().getParcelable(OUTPUT_CONFIGS_INTENT);
            }
        }
        //Handle getting of config data
        String configString = mInputConfigs.getStringExtra(EXTRA_CONFIG);
        if (configString != null && !configString.isEmpty()) {
            ConfigData configData = CalendarEventUtils.getConfigData(configString);
            if (configData != null) {
                mSelectedCalendarIds = CalendarEventUtils
                        .getListOfCalendarIdsFromString(configData.mCalendarIds);
            }
        }
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCalendarPickerDelegate = (CalendarPickerDelegate) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    " must implement CalendarPickerDelegate");
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
    
        List<Calendar> calendars = mCalendarPickerDelegate.getCalendars();
        assert calendars != null;
        mCalendars = calendars;
        
        final boolean allSelected = mSelectedCalendarIds == null;
        final int numEntries = mCalendars.size() + 1; // need room for the "All calendars" item
        final ListItem[] items = new ListItem[numEntries];
        final boolean[] checked = new boolean[numEntries];

        // All calendars item
        items[0] = new ListItem(0, getString(R.string.all_calendars), null, ListItem.typeONE, null, null);
        checked[0] = allSelected;

        for (int i = 0; i < mCalendars.size(); i++) {
            final Calendar calendar = mCalendars.get(i);
            final String label = getResources().getString(R.string.calender_picker_event_in, calendar.displayName);
            //HSHIEH: Calendar provider is giving alpha value of zero. This is a workaround.
            calendar.color = Color.rgb(Color.red(calendar.color), Color.green(calendar.color), Color.blue(calendar.color));
            items[i+1] = new ListItem(0, label, null, ListItem.typeONE, calendar, null, calendar.color);
            checked[i+1] = allSelected || mSelectedCalendarIds.contains(calendar.id);
        }

        final Picker picker = new Picker.Builder(getActivity())
            .setTitle(Html.fromHtml(getString(R.string.calendar_picker_prompt)))
            .setMultiChoiceItems(items, checked, true, null)
            .setPositiveButton(getString(R.string.continue_prompt), this)
            .create();

        mListView = (ListView)picker.getView().findViewById(R.id.list);

        return picker.getView();
    }

    /**
     * Required by DialogInterface.OnClickListener
     */
    public void onClick(final DialogInterface dialog, final int which) {
        mSelectedCalendarIds = getSelectedCalendarIds();
        final Intent result = new Intent();
        result.putExtra(EXTRA_CALENDARS, Calendar.getCalendarIdsString(mSelectedCalendarIds));
        mHostActivity.onReturn(result, this);
    }

    private List<Integer> getSelectedCalendarIds() {
        final SparseBooleanArray checked = mListView.getCheckedItemPositions();
        if (checked.get(0)) {
            // All calendars selected
            return null;
        }

        final List<Integer> selected = new ArrayList<Integer>();
        for (int i = 0; i < mCalendars.size(); i++) {
            if (checked.get(i + 1)) {
                selected.add(mCalendars.get(i).id);
            }
        }
        return selected;
    }
}
