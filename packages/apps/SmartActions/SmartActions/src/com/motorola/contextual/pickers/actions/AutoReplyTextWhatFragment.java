/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * XPR643        2012/05/31 Smart Actions 2.1 Initial Version
 */
package com.motorola.contextual.pickers.actions;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * This fragment presents an auto reply text What selection
 * to specify contact methods to react to, including text messages and calls.
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment - fragment for interacting with a Smart Actions container activity
 *
 * RESPONSIBILITIES:
 *  Present an auto reply text What selection.
 *
 * COLLABORATORS:
 *  AutoReplyTextActivity.java - Coordinates auto reply text fragments and collects results
 *  AutoReplyTextWhoFragment.java - Chooses the contacts to interact with
 *  AutoReplyTextMessageFragment.java - Specifies a text message to reply with
 *  ContactsChooserFragment.java - Chooses a specific set of contacts
 *  EditTextFragment.java - Inputs custom user text
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class AutoReplyTextWhatFragment extends PickerFragment implements OnClickListener {
    protected static final String TAG = AutoReplyTextWhatFragment.class.getSimpleName();

    /**
     * Constants for communication methods.
     */
    private interface RespondToValues {
        //Auto SMS related
        int CALLS_AND_TEXTS = 0;
        int CALLS = 1;
        int TEXTS = 2;
    }

    /**
     * Enum of list view item selections.
     */
    public enum Selection {
        REPLY_TEXT (R.string.auto_reply_what_texts, -1, RespondToValues.TEXTS),
        REPLY_CALLS (R.string.auto_reply_what_calls, -1, RespondToValues.CALLS),
        REPLY_TEXT_AND_CALLS (R.string.auto_reply_what_both, -1, RespondToValues.CALLS_AND_TEXTS);

        /** List item primary text resource ID */
        private int mLabelResourceId = -1;
        /** List item secondary text resource ID */
        private int mDescResourceId = -1;

        /** Corresponding RESPOND_TO value */
        private int mRespondToValue = -1;

        /**
         * Constructs enum value.
         *
         * @param labelResourceId List item label resource ID
         * @param descResourceId List item description resource ID
         * @param respondToValue Smart Actions engine RESPOND_TO value
         */
        private Selection(final int labelResourceId, final int descResourceId,
                final int respondToValue) {
            mLabelResourceId = labelResourceId;
            mDescResourceId = descResourceId;
            mRespondToValue = respondToValue;
        }

        /**
         * Converts RESPOND_TO values to Selection values.
         *
         * @param respondTo RESPOND_TO value
         * @return Corresponding Selection value
         */
        public static Selection fromRespondTo(final int respondTo) {
            Selection sel = REPLY_TEXT_AND_CALLS;
            switch (respondTo) {
            case RespondToValues.TEXTS:
                sel = REPLY_TEXT;
                break;
            case RespondToValues.CALLS:
                sel = REPLY_CALLS;
                break;
            case RespondToValues.CALLS_AND_TEXTS:
                break;
            default:
                Log.w(TAG, "Unable to convert RESPOND_TO value "
                        + String.valueOf(respondTo));
                break;
            }
            return sel;
        }

        /**
         * Returns the corresponding RESPOND_TO value.
         *
         * @return Corresponding RESPOND_TO value
         */
        public int getRespondToValue() {
            return mRespondToValue;
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

    /** Index of initial list item selection */
    private int mInitialCheckedListItem = Selection.REPLY_TEXT_AND_CALLS.ordinal();

    /** Single-selection list view */
    private ListView mListView = null;

    /** Source of data for the list view items */
    private List<ListItem> mItems = null;

    public interface AutoReplyTextWhatCallback {
        void handleAutoReplyTextWhatFragment(Fragment fragment, Object returnValue);
    }

    private AutoReplyTextWhatCallback mAutoReplyTextWhatCallback;

    public static AutoReplyTextWhatFragment newInstance(final int titleResId, final int checkedListItem) {
        
        Bundle args = new Bundle();
        args.putInt(Key.TITLE_RESOURCE_ID, titleResId);
        args.putInt(Key.CHECKED_LIST_ITEM, checkedListItem);

        AutoReplyTextWhatFragment f = new AutoReplyTextWhatFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mAutoReplyTextWhatCallback = (AutoReplyTextWhatCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    " must implement AutoReplyTextWhatCallback");
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
        mAutoReplyTextWhatCallback.handleAutoReplyTextWhatFragment(fragment, returnValue);
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
     * Restores prior instance state in onCreateView.
     *
     * @param savedInstanceState Bundle containing prior instance state
     */
    @Override
    protected void restoreInstanceState(final Bundle savedInstanceState) {
        mTitle = savedInstanceState.getString(Key.TITLE);
        mTitleResourceId = savedInstanceState.getInt(Key.TITLE_RESOURCE_ID, -1);
        mInitialCheckedListItem = savedInstanceState.getInt(Key.CHECKED_LIST_ITEM, -1);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Key.TITLE, mTitle);
        outState.putInt(Key.TITLE_RESOURCE_ID, mTitleResourceId);
        if (mListView != null) {
            outState.putInt(Key.CHECKED_LIST_ITEM, mListView.getCheckedItemPosition());
        }
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
