/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * XPR643        2012/06/13 Smart Actions 2.1 Initial Version
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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * This fragment presents a missed calls Who selection
 * to specify contacts to reply to.
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment - fragment for interacting with a Smart Actions container activity
 *
 * RESPONSIBILITIES:
 *  Present a missed calls Who selection.
 *
 * COLLABORATORS:
 *  MissedCallsActivity.java - Coordinates missed calls fragments and collects results
 *  MissedCallsNumberFragment.java - Chooses the threshold number of missed calls
 *  ContactsChooserFragment.java - Chooses a specific set of contacts
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class MissedCallsWhoFragment extends PickerFragment
        implements MissedCallConstants, OnClickListener {
    protected static final String TAG = MissedCallsWhoFragment.class.getSimpleName();

    /**
     * Enum of list view item selections.
     */
    public enum Selection {
        ALL_INCOMING (R.string.all_incoming_calls, -1),
        SPECIFIC_CONTACTS (R.string.specific_people, -1);

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
         * Converts contacts string values to Selection values.
         *
         * @param contactStrings Contacts string value
         * @return Corresponding Selection value; else null
         */
        public static Selection fromContactsString(final String contactsString) {
            Selection sel = SPECIFIC_CONTACTS;
            if (TextUtils.isEmpty(contactsString)) {
                sel = null;
            } else if (contactsString.equals(MISSED_CALL_ANY_NUMBER)) {
                sel = ALL_INCOMING;
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
         *
         * @return Description, if valid; otherwise, null
         */
        public String getDescription(final Resources res) {
            String desc = null;
            if ((res != null) && (mDescResourceId >= 0)) {
                desc = res.getString(mDescResourceId);
            }
            return desc;
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
        /** Saved instance state key for saving title */
        String CHECKED_LIST_ITEM = "com.motorola.contextual.pickers.actions.KEY_CHECKED_LIST_ITEM";
    }

    /** Title question for this Fragment */
    private String mTitle = null;

    /** Title question resource ID for this Fragment, if it exists */
    private int mTitleResourceId = -1;

    /** Index of initial list item selection; default none checked (-1) */
    private int mInitialCheckedListItem = -1;

    /** Single-selection list view */
    private ListView mListView = null;

    /** Source of data for the list view items */
    private List<ListItem> mItems = null;

    public interface MissedCallsWhoCallback {
        void handleMissedCallsWhoFragment(Fragment fragment, Object returnValue);
    }

    private MissedCallsWhoCallback mMissedCallsWhoCallback;
    
    public static MissedCallsWhoFragment newInstance(final int titleResId, final int checkedListItem) {
        Bundle args = new Bundle();
        
        args.putInt(Key.TITLE_RESOURCE_ID, titleResId);
        args.putInt(Key.CHECKED_LIST_ITEM, checkedListItem);
        
        MissedCallsWhoFragment f = new MissedCallsWhoFragment();
        f.setArguments(args);
        return f;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mMissedCallsWhoCallback = (MissedCallsWhoCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    " must implement MissedCallsWhoCallback");
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mTitleResourceId = getArguments().getInt(Key.TITLE_RESOURCE_ID, -1);
            mInitialCheckedListItem = getArguments().getInt(Key.CHECKED_LIST_ITEM, -1);
        }
    }
    
    @Override
    public void handleResult(final Object returnValue, final PickerFragment fragment) {
        mMissedCallsWhoCallback.handleMissedCallsWhoFragment(fragment, returnValue);
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
                    .setPositiveButton(R.string.continue_prompt, this)
                    .create()
                    .getView();

            // Retrieve the list view
            mListView = (ListView) mContentView.findViewById(R.id.list);
        }
        return mContentView;
    }

    /**
     * Required by OnClickListener interface.
     * Handles click of button.
     *
     * @param dialog Object that implements DialogInterface
     * @param which Identifier of button that was clicked
     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
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
     * Handles button click by returning message.
     */
    private void onPositiveButtonClicked() {
        final int position = mListView.getCheckedItemPosition();

        if (position >= 0 && position < Selection.values().length) {
            final String select = Selection.values()[position].name();

            // Save current checked list item for fragment back stack restoration
            mInitialCheckedListItem = position;

            mHostActivity.onReturn(select, this);
        }
    }
}
