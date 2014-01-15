/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number          Brief Description
 * ------------- ---------- -----------------  ------------------------------
 * E11636        2012/06/19 Smart Actions 2.1  Initial Version
 */
package com.motorola.contextual.pickers.actions;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.Utils;
import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * This class implements the Alert fragment of the Reminder picker.
 *
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment
 *
 * RESPONSIBILITIES:
 * Implements the chooser for the alert styles of the Reminder picker.
 *
 * COLLABORATORS:
 *  ReminderActivity.java - container Activity for the Reminder picker
 *  ReminderAlertFragment.java - alert style chooser for the Reminder picker
 *  ReminderMessageFragment.java - message composer for the Reminder picker
 *  WhenFragment.java - chooser for start or end trigger condition
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class ReminderAlertFragment extends PickerFragment implements Constants, OnClickListener {

    /**
     * The Enumeration of Alert choices
     */
    private enum ReminderAlerts {
        VIBRATE (EXTRA_VIBRATE, R.drawable.zz_moto_lockscreen_sd_vibrate, R.string.vibrate),
        SOUND (EXTRA_SOUND, R.drawable.zz_moto_lockscreen_sd_sound, R.string.play_sound);

        private final String mKey;
        private final int mIcon;
        private final int mTitle;

        /**
         * Constructor for the ReminderAlerts enumeration.
         *
         * @param key - the name of the enum
         * @param icon - the icon of the enum
         * @param title - the UI label of the enum
         */
        ReminderAlerts(final String key, final int icon, final int title) {
            mKey = key;
            mIcon = icon;
            mTitle = title;
        }

        /**
         * @return the key of the enum
         */
        public String key() {
            return mKey;
        }

        /**
         * @return the icon of the enum
         */
        public int icon() {
            return mIcon;
        }

        /**
         * @return the title of the enum
         */
        public int title() {
            return mTitle;
        }
    }

    private ListView mListView;
    
    private static final String INPUT_CONFIGS_INTENT = "INPUT_CONFIGS_INTENT";
    private static final String OUTPUT_CONFIGS_INTENT = "OUTPUT_CONFIGS_INTENT";

    public static ReminderAlertFragment newInstance(final Intent inputConfigs, final Intent outputConfigs) {

        Bundle args = new Bundle();

        if (inputConfigs != null) {
            args.putParcelable(INPUT_CONFIGS_INTENT, inputConfigs);
        }

        if (outputConfigs != null) {
            args.putParcelable(OUTPUT_CONFIGS_INTENT, outputConfigs);
        }

        ReminderAlertFragment f = new ReminderAlertFragment();
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
   
        final ListItem[] items = new ListItem[ReminderAlerts.values().length];
        final boolean[] checked = new boolean[ReminderAlerts.values().length];

        for (int i = 0; i < ReminderAlerts.values().length; i++) {
            items[i] = new ListItem(ReminderAlerts.values()[i].icon(),
                                    getString(ReminderAlerts.values()[i].title()), 
                                    null,
                                    ListItem.typeONE, 
                                    ReminderAlerts.values()[i], 
                                    null);
                    
            if (mOutputConfigs != null && mOutputConfigs.getExtras() != null) {
                checked[i] = mOutputConfigs.getBooleanExtra(ReminderAlerts.values()[i].key(), true);
            } else if (mInputConfigs != null && mInputConfigs.getExtras() != null) {
                checked[i] = mInputConfigs.getBooleanExtra(ReminderAlerts.values()[i].key(), false);
            } else {
                checked[i] = true;
            }
        }

        // Put up the multi-select list of choices.
        mContentView = new Picker.Builder(getActivity())
        .setTitle(Html.fromHtml(getString(R.string.reminder_alert_prompt)))
        .setOnKeyListener(Utils.sDisableSearchKey)
        .setMultiChoiceItems(items, checked, null)
        .setPositiveButton(getString(R.string.continue_prompt), this)
        .setIsBottomButtonAlwaysEnabled(true)
        .create()
        .getView();

        mListView = (ListView) mContentView.findViewById(R.id.list);
        
        return mContentView;
    }

    /**
     * Required by OnClickListener
     */
    public void onClick(final DialogInterface dialog, final int which) {
        updateOutputConfigs();
        mHostActivity.onReturn(mOutputConfigs, this);
    }
     
    private void updateOutputConfigs() {
        //In some cases where multiple resumes have been made, mListView may be null
        final SparseBooleanArray checked;
        if (mListView != null){
            checked = mListView.getCheckedItemPositions();
        } else {
            //If we cant fill up the SparseBooleanArray, we should return
            return;
        }

        for (int i = 0; i < ReminderAlerts.values().length; i++) {
            mOutputConfigs.putExtra(ReminderAlerts.values()[i].key(), checked.get(i));
        }
        if (checked.get(ReminderAlerts.VIBRATE.ordinal()) && checked.get(ReminderAlerts.SOUND.ordinal())) {
            mOutputConfigs.putExtra(EXTRA_DESCRIPTION, getString(R.string.vibrate_and_play));
        } else if (checked.get(ReminderAlerts.VIBRATE.ordinal())) {
            mOutputConfigs.putExtra(EXTRA_DESCRIPTION, getString(R.string.vibrate));
        } else if (checked.get(ReminderAlerts.SOUND.ordinal())) {
            mOutputConfigs.putExtra(EXTRA_DESCRIPTION, getString(R.string.play_sound));
        }
    }
    
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        updateOutputConfigs();
        outState.putParcelable(INPUT_CONFIGS_INTENT, mInputConfigs);
        outState.putParcelable(OUTPUT_CONFIGS_INTENT, mOutputConfigs);
        super.onSaveInstanceState(outState);
    }
}
