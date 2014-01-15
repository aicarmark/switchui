/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * XPR643        2012/06/11 Smart Actions 2.1 Initial Version
 */
package com.motorola.contextual.pickers;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.motorola.contextual.smartrules.R;

/**
 * This fragment presents an volume level selection.
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment - fragment for interacting with a Smart Actions container activity
 *
 * RESPONSIBILITIES:
 *  Present a volume level selection.
 *
 * COLLABORATORS:
 *  VipRingerActivity.java - Coordinates VIP ringer fragments and collects results
 *  ContactsChooserFragment.java - Chooses a specific set of contacts
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class VolumeLevelChooserFragment extends PickerFragment implements OnClickListener {
    protected static final String TAG = VolumeLevelChooserFragment.class.getSimpleName();

    /** Max volume level as fraction of maximum */
    public static double VOLUME_FRACTION_MAX = 1.00;
    /** High volume level as fraction of maximum */
    public static double VOLUME_FRACTION_HIGH = 0.75;
    /** Medium volume level as fraction of maximum */
    public static double VOLUME_FRACTION_MEDIUM = 0.50;

    /**
     * Enum of list view item selections.
     */
    public enum Selection {
        VOLUME_MAX (R.string.max, -1, VOLUME_FRACTION_MAX),
        VOLUME_HIGH (R.string.high, -1, VOLUME_FRACTION_HIGH),
        VOLUME_MEDIUM (R.string.medium, -1, VOLUME_FRACTION_MEDIUM);

        /** List item primary text resource ID */
        private int mLabelResourceId = -1;
        /** List item secondary text resource ID */
        private int mDescResourceId = -1;
        /** Volume fraction of maximum */
        double mVolumeFactor = 1.0;

        /**
         * Constructs enum value.
         *
         * @param labelResourceId List item label resource ID
         * @param descResourceId List item description resource ID
         * @param volumeFactor Volume fraction of maximum
         */
        private Selection(final int labelResourceId, final int descResourceId,
                final double volumeFactor) {
            mLabelResourceId = labelResourceId;
            mDescResourceId = descResourceId;
            mVolumeFactor = volumeFactor;
        }

        /**
         * Converts volume level values to Selection values.
         *
         * @param audioMan AudioManager instance
         * @param volumeLevel Volume level value
         * @return Corresponding Selection value
         */
        public static Selection fromVolume(final AudioManager audioMan,
                final int volumeLevel) {
            // Compute volume levels
            // Max: 100% of maximum volume
            final int maxVolume = audioMan.getStreamMaxVolume(AudioManager.STREAM_RING);
            // High: 75% of maximum volume
            final int highVolume = (3 * maxVolume) >> 2;
            // Medium: 50% of maximum volume
            final int mediumVolume = maxVolume >> 1;

            // Convert volume level to a volume category
            Selection sel = VOLUME_MAX;
            if (volumeLevel == mediumVolume) {
                sel = VOLUME_MEDIUM;
            } else if (volumeLevel == highVolume) {
                sel = VOLUME_HIGH;
            }
            return sel;
        }

        /**
         * Converts volume level values to Selection values.
         *
         * @param percentVolume Volume level value in percentage
         * @return Corresponding Selection value
         */
        public static Selection fromVolumeLevel(final int percentVolume) {
            double fraction = (double)percentVolume/100.0;
            Selection sel;
            if (fraction > VOLUME_HIGH.mVolumeFactor) {
                sel = VOLUME_MAX;
            } else if (fraction > VOLUME_MEDIUM.mVolumeFactor) {
                sel = VOLUME_HIGH;
            } else {
                sel = VOLUME_MEDIUM;
            }
            return sel;
        }

        /**
         * Retrieves label.
         *
         * @param res Resources object
         * @return Label, if valid; otherwise, null
         */
        public String getLabel(final Resources res) {
            String label = null;
            if ((res != null) && (mLabelResourceId >= 0)) {
                label = res.getString(mLabelResourceId);
            }
            return label;
        }

        /**
         * Retrieves description.
         *
         * @param res Resources object
         * @return Description, if valid; otherwise, null
         */
        public String getDescription(final Resources res) {
            String desc = null;
            if ((res != null) && (mDescResourceId >= 0)) {
                desc = res.getString(mDescResourceId);
            }
            return desc;
        }

        /**
         * Retrieves volume level in percentage.
         *
         * @return Volume level value
         */
        public int getVolumeLevel() {
            return new Double(mVolumeFactor * 100).intValue();
        }

        /**
         * Retrieves volume level value.
         *
         * @param audioMan AudioManager instance
         * @return Volume level value
         */
        public int getVolume(final AudioManager audioMan) {
            final int maxVolume = audioMan.getStreamMaxVolume(AudioManager.STREAM_RING);
            return new Double(mVolumeFactor * maxVolume).intValue();
        }
    }

    /**
     * Keys for saving and restoring instance state bundle.
     */
    private interface Key {
        /** Saved instance state key for saving title */
        String TITLE = "com.motorola.contextual.pickers.actions.KEY_TITLE";
        /** Saved instance state key for saving title resource ID */
        String TITLE_RESOURCE_ID = "com.motorola.contextual.pickers.actions.KEY_TITLE_RESOURCE_ID";
        /** Saved instance state key for saving button text resource ID */
        String BUTTON_TEXT_RESOURCE_ID = "com.motorola.contextual.pickers.actions.KEY_BUTTON_TEXT_RESOURCE_ID";
        /** Saved instance state key for saving title */
        String CHECKED_LIST_ITEM = "com.motorola.contextual.pickers.actions.KEY_CHECKED_LIST_ITEM";
    }

    /** Title question for this Fragment */
    private String mTitle = null;

    /** Title question resource ID for this Fragment, if it exists */
    private int mTitleResourceId = -1;

    /** Confirmation button text resource ID */
    private int mButtonTextResourceId = R.string.iam_done;

    /** Index of initial list item selection */
    private int mInitialCheckedListItem = Selection.VOLUME_MAX.ordinal();

    /** Single-selection list view */
    private ListView mListView = null;

    /** Source of data for the list view items */
    private List<ListItem> mItems = null;

    /** Callback interface */
    public interface VolumeLevelChooserCallback {
        public void handleVolumeLevelChooserFragment(Fragment fragment, Object returnValue);
    }

    private VolumeLevelChooserCallback mVolumeLevelChooserCallback;

    /** factory method to instantiate fragment */
    public static VolumeLevelChooserFragment newInstance(final int titleResId,
                                                         final int buttonTextResId,
                                                         final int checkedListItem) {
        Bundle args = new Bundle();
        args.putInt(Key.TITLE_RESOURCE_ID, titleResId);
        args.putInt(Key.BUTTON_TEXT_RESOURCE_ID, buttonTextResId);
        args.putInt(Key.CHECKED_LIST_ITEM, checkedListItem);

        VolumeLevelChooserFragment f = new VolumeLevelChooserFragment();
        f.setArguments(args);
        return f;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mVolumeLevelChooserCallback = (VolumeLevelChooserCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    " must implement VolumeLevelChooserCallback");
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mTitleResourceId = getArguments().getInt(Key.TITLE_RESOURCE_ID);
            mButtonTextResourceId = getArguments().getInt(Key.BUTTON_TEXT_RESOURCE_ID);
            mInitialCheckedListItem = getArguments().getInt(Key.CHECKED_LIST_ITEM);
        }
    }

    @Override
    public void handleResult(final Object returnValue, final PickerFragment fragment) {
        mVolumeLevelChooserCallback.handleVolumeLevelChooserFragment(fragment, returnValue);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (mContentView == null) {
            // If a title resource ID was supplied, always retrieve directly from resources
            if (mTitleResourceId >= 0) {
                mTitle = getString(mTitleResourceId);
            }

            // Populate list view data
            final Resources res = getResources();
            mItems = new ArrayList<ListItem>(Selection.values().length);
            for (final Selection item : Selection.values()) {
                mItems.add(item.ordinal(),
                    new ListItem(-1, item.getLabel(res), item.getDescription(res),
                            ListItem.typeONE, null, null));
            }

            // Create the actual view to show
            mContentView = new Picker.Builder(mHostActivity)
                    .setTitle(Html.fromHtml(mTitle))
                    .setSingleChoiceItems(mItems, mInitialCheckedListItem, null)
                    .setPositiveButton(mButtonTextResourceId, this)
                    .create()
                    .getView();

            // Retrieve the list view
            mListView = (ListView) mContentView.findViewById(R.id.list);
        }
        return mContentView;
    }

    /**
     * Restores prior instance state in onCreateView.
     *
     * @param savedInstanceState Bundle containing prior instance state
     */
    @Override
    protected void restoreInstanceState(final Bundle savedInstanceState) {
        mTitle = savedInstanceState.getString(Key.TITLE);
        mTitleResourceId = savedInstanceState.getInt(Key.TITLE_RESOURCE_ID, -1);
        mButtonTextResourceId = savedInstanceState.getInt(Key.BUTTON_TEXT_RESOURCE_ID, -1);
        mInitialCheckedListItem = savedInstanceState.getInt(Key.CHECKED_LIST_ITEM, -1);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Key.TITLE, mTitle);
        outState.putInt(Key.TITLE_RESOURCE_ID, mTitleResourceId);
        outState.putInt(Key.BUTTON_TEXT_RESOURCE_ID, mButtonTextResourceId);
        outState.putInt(Key.CHECKED_LIST_ITEM, mListView.getCheckedItemPosition());
    }

    /**
     * Required by OnClickListener interface.
     * Handles click of button.
     *
     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
     *
     * @param dialog Object that implements DialogInterface
     * @param which Identifier of button that was clicked
     */
    public void onClick(final DialogInterface dialog, final int which) {
        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            onPositiveButtonClicked();
            break;
        case DialogInterface.BUTTON_NEUTRAL:
        case DialogInterface.BUTTON_NEGATIVE:
            break;
        default:
            // Do nothing for list items
            break;
        }
    }

    /**
     * Handle button click by returning message.
     */
    private void onPositiveButtonClicked() {
        final int position = mListView.getCheckedItemPosition();
        if (position >= 0 && position < Selection.values().length) {
            final String select = Selection.values()[position].name();
            mHostActivity.onReturn(select, this);
        }
    }
}
