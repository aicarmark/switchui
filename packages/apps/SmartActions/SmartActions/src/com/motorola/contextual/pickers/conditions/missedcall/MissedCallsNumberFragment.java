/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * XPR643        2012/06/11 Smart Actions 2.1 Initial Version
 */
package com.motorola.contextual.pickers.conditions.missedcall;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * This fragment presents a number of missed calls selection.
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment - fragment for interacting with a Smart Actions container activity
 *
 * RESPONSIBILITIES:
 *  Present a number of missed calls selection.
 *
 * COLLABORATORS:
 *  MissedCallsActivity.java - Coordinates missed calls fragments and collects results
 *  MissedCallsWhoFragment.java - Chooses the contacts to interact with
 *  ContactsChooserFragment.java - Chooses a specific set of contacts
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class MissedCallsNumberFragment extends PickerFragment implements OnClickListener {
    protected static final String TAG = MissedCallsNumberFragment.class.getSimpleName();

    /**
     * Enum of list view item selections.
     */
    public enum Selection {
        MISSED_CALLS_1 (R.string.missed_calls_num_1, -1),
        MISSED_CALLS_2 (R.string.missed_calls_num_2, -1),
        MISSED_CALLS_3 (R.string.missed_calls_num_3, -1),
        MISSED_CALLS_4 (R.string.missed_calls_num_4, -1),
        MISSED_CALLS_5 (R.string.missed_calls_num_5, -1);

        /** Default number of missed calls: 3 */
        public static final Selection DEFAULT_MISSED_CALLS = MISSED_CALLS_3;

        /** List item primary text resource ID */
        private int mLabelResourceId = -1;
        /** List item secondary text resource ID */
        private int mDescResourceId = -1;

        /**
         * Constructs enum value.
         *
         * @param labelResourceId List item label resource ID
         * @param descResourceId List item description resource ID
         */
        private Selection(final int labelResourceId, final int descResourceId) {
            mLabelResourceId = labelResourceId;
            mDescResourceId = descResourceId;
        }

        /**
         * Converts missed calls values to Selection values.
         *
         * @param numberOfMissedCalls Number of calls missed
         * @return Corresponding Selection value
         */
        public static Selection fromNumberOfMissedCalls(final int numberOfMissedCalls) {
            final int maxCalls = Selection.values().length;
            // Convert zero or negative input values to default number of missed calls
            Selection sel = DEFAULT_MISSED_CALLS;
            if (numberOfMissedCalls > maxCalls) {
                // Set the maximum supported number of missed calls
                sel = Selection.values()[maxCalls - 1];
            } else if (numberOfMissedCalls > 0){
                sel = Selection.values()[numberOfMissedCalls - 1];
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
         * Retrieves number of missed calls.
         *
         * @return Number of calls missed
         */
        public int getNumberOfMissedCalls() {
            return this.ordinal() + 1;
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

    /** Index of initial list item selection: 3 missed calls */
    private int mInitialCheckedListItem = Selection.MISSED_CALLS_3.ordinal();

    /** Single-selection list view */
    private ListView mListView = null;

    /** Source of data for the list view items */
    private List<ListItem> mItems = null;

    public interface MissedCallsNumberCallback {
        void handleMissedCallsNumberFragment(Fragment fragment, Object returnValue);
    }

    private MissedCallsNumberCallback mMissedCallsNumberCallback;
    
    public static MissedCallsNumberFragment newInstance(final int titleResId, final int buttonTextResId,
        final int checkedListItem) {
        Bundle args = new Bundle();
        
        args.putInt(Key.TITLE_RESOURCE_ID, titleResId);
        args.putInt(Key.BUTTON_TEXT_RESOURCE_ID, buttonTextResId);
        args.putInt(Key.CHECKED_LIST_ITEM, checkedListItem);
        
        MissedCallsNumberFragment f = new MissedCallsNumberFragment();
        f.setArguments(args);
        return f;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mMissedCallsNumberCallback = (MissedCallsNumberCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    " must implement MissedCallsNumberCallback");
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mTitleResourceId = getArguments().getInt(Key.TITLE_RESOURCE_ID, -1);
            mButtonTextResourceId = getArguments().getInt(Key.BUTTON_TEXT_RESOURCE_ID, -1);
            mInitialCheckedListItem = getArguments().getInt(Key.CHECKED_LIST_ITEM, -1);
        }
    }
    
    @Override
    public void handleResult(final Object returnValue, final PickerFragment fragment) {
        mMissedCallsNumberCallback.handleMissedCallsNumberFragment(fragment, returnValue);
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
