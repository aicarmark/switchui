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

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * The fragment that implements the calendar event configuration screen.
 *
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment
 *
 * RESPONSIBILITIES:
 * Implements the screen to configure the types of calendar events to use.
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
public class CalendarEventConfigFragment extends PickerFragment implements CalendarEventSensorConstants, DialogInterface.OnClickListener {

    private enum ExcludeEventOption {
        ALL_DAY(EXTRA_ALLDAY_EVENTS, R.string.calendar_event_config_all_day),
        ONLY_INVITEE(EXTRA_MULTIPLE_PARTICIPANTS, R.string.calendar_event_config_only_invitee),
        HAVENT_ACCEPTED(EXTRA_ACCEPTED_EVENTS, R.string.calendar_event_config_unaccepted),
        ;

        public final String key;
        public final int titleId;

        ExcludeEventOption(final String key, final int titleId) {
            this.key = key;
            this.titleId = titleId;
        }
    }

    private ListView mListView;

    //Config intent constants
    private static final String INPUT_CONFIGS_INTENT = "INPUT_CONFIG_INTENT";
    private static final String OUTPUT_CONFIGS_INTENT = "OUTPUT_CONFIG_INTENT";

    /**
     * Factory-style constructor
     *
     * @param inputConfig - input configurations to this picker
     * @param outputConfig - resulted output configurations from this picker
     */
    public static CalendarEventConfigFragment newInstance(final Intent inputConfigs, final Intent outputConfigs) {
        //Bundle inputConfigs, outputConfigs to pass over to onCreate
        Bundle args = new Bundle();
        if (inputConfigs != null) {
            args.putParcelable(INPUT_CONFIGS_INTENT, inputConfigs);
        }

        if (outputConfigs != null) {
            args.putParcelable(OUTPUT_CONFIGS_INTENT, outputConfigs);
        }

        CalendarEventConfigFragment f = new CalendarEventConfigFragment();
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
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        // Build list of option items
        final int numItems = ExcludeEventOption.values().length + 1; // make room for the special last item
        final ListItem[] items = new ListItem[numItems];
        final boolean[] checked = new boolean[numItems];

        for (int i = 0; i < numItems - 1; i++) {
            final ExcludeEventOption option = ExcludeEventOption.values()[i];
            items[i] = new ListItem(0, getString(option.titleId),
                                    null, ListItem.typeONE, option, null);
            if (mInputConfigs != null) {
                checked[i] = mInputConfigs.getBooleanExtra(option.key, false);
            }
        }
        items[numItems - 1] = createShowMatchingEventsItem();

        final Picker picker = new Picker.Builder(getActivity())
            .setTitle(Html.fromHtml(getString(R.string.calendar_event_config_prompt)))
            .setMultiChoiceItems(items, checked, null)
            .setPositiveButton(getString(R.string.iam_done), this)
            .setIsBottomButtonAlwaysEnabled(true)
            .create();

        mListView = (ListView)picker.getView().findViewById(R.id.list);

        return picker.getView();
    }

    private ListItem createShowMatchingEventsItem() {
        return new ListItem(R.drawable.ic_calendar_sensor_w,
                            getString(R.string.calendar_event_config_show_matching),
                            null, ListItem.typeTHREE, null,
                            new View.OnClickListener() {

                                public void onClick(final View v) {
                                    showMatchingEvents();
                                }
                            });

    }

    private void updateConfigWithSelections() {
        final SparseBooleanArray checked = mListView.getCheckedItemPositions();
        for (int i = 0; i < ExcludeEventOption.values().length; i++) {
            mInputConfigs.putExtra(ExcludeEventOption.values()[i].key, checked.get(i));
        }
    }

    /**
     * Required by DialogInterface.OnClickListener
     */
    public void onClick(final DialogInterface dialog, final int which) {
        updateConfigWithSelections();
        mHostActivity.onReturn(mInputConfigs, this);
    }

    private void showMatchingEvents() {
        updateConfigWithSelections();
        new MatchingEventsDialog(getActivity(), mInputConfigs).show();
    }
}
