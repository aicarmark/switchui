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
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.motorola.contextual.pickers.EditTextFragment;
import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * This fragment presents an auto reply text Message selection.
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment - fragment for interacting with a Smart Actions container activity
 *
 * RESPONSIBILITIES:
 *  Present an auto reply text Message selection.
 *
 * COLLABORATORS:
 *  AutoReplyTextActivity.java - Coordinates auto reply text fragments and collects results
 *  AutoReplyTextWhoFragment.java - Chooses the contacts to interact with
 *  AutoReplyTextWhatFragment.java - Chooses the contact methods to react to
 *  ContactsChooserFragment.java - Chooses a specific set of contacts
 *  EditTextFragment.java - Inputs custom user text
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class AutoReplyTextMessageFragment extends PickerFragment implements OnClickListener {
    protected static final String TAG = AutoReplyTextMessageFragment.class.getSimpleName();

    /**
     * Enum of list view item selections.
     */
    public enum Selection {
        MSG_DRIVING (R.string.auto_reply_msg_driving, -1),
        MSG_WHEN_AVAILABLE (R.string.auto_reply_msg_when_available, -1),
        MSG_AWAY (R.string.auto_reply_msg_away, -1),
        MSG_CUSTOM (R.string.auto_reply_msg_custom, R.string.auto_reply_msg_custom_desc);

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
         * Converts message to Selection values.
         * If message is empty, MSG_CUSTOM is returned, but not a valid selection.
         *
         * @param res Resources object
         * @param message Text message
         * @return Corresponding AutoReplyTextMessageFragment.Selection value.
         */
        public static Selection fromMessage(final Resources res, final String message) {
            Selection sel = MSG_CUSTOM;
            if (!TextUtils.isEmpty(message)) {
                if (MSG_DRIVING.getLabel(res).equals(message)) {
                    sel = MSG_DRIVING;
                } else if (MSG_WHEN_AVAILABLE.getLabel(res).equals(message)) {
                    sel = MSG_WHEN_AVAILABLE;
                } else if (MSG_AWAY.getLabel(res).equals(message)) {
                    sel = MSG_AWAY;
                }
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
        /** Saved instance state key for custom message */
        String CUSTOM_MESSAGE = "com.motorola.contextual.pickers.actions.KEY_CUSTOM_MESSAGE";
    }

    /** Title question for this Fragment */
    private String mTitle = null;

    /** Title question resource ID for this Fragment, if it exists */
    private int mTitleResourceId = -1;

    /** Index of initial list item selection */
    private int mInitialCheckedListItem = -1;

    /** User-entered text message */
    private String mCustomMessage = null;

    /** Single-selection list view */
    private ListView mListView = null;

    /** Source of data for the list view items */
    private List<ListItem> mItems = null;

    public interface AutoReplyTextMessageCallback {
        void handleAutoReplyTextMessageFragment(Fragment fragment, Object returnValue);
    }

    private AutoReplyTextMessageCallback mAutoReplyTextMessageCallback;

    public static AutoReplyTextMessageFragment newInstance(final int titleResId, final int checkedListItem,
            final String customMessage) {

        Bundle args = new Bundle();
        args.putInt(Key.TITLE_RESOURCE_ID, titleResId);
        args.putInt(Key.CHECKED_LIST_ITEM, checkedListItem);
        args.putString(Key.CUSTOM_MESSAGE, customMessage);

        AutoReplyTextMessageFragment f = new AutoReplyTextMessageFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mAutoReplyTextMessageCallback = (AutoReplyTextMessageCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    " must implement AutoReplyTextMessageCallback");
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null) { 
            mTitleResourceId = getArguments().getInt(Key.TITLE_RESOURCE_ID, -1);
            mInitialCheckedListItem = getArguments().getInt(Key.CHECKED_LIST_ITEM, -1);
            mCustomMessage = getArguments().getString(Key.CUSTOM_MESSAGE);
        }
    }
    
    @Override
    public void handleResult(final Object returnValue, final PickerFragment fragment) {
        mAutoReplyTextMessageCallback.handleAutoReplyTextMessageFragment(fragment, returnValue);
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
                String desc = item.getDescription(res);
                if (!TextUtils.isEmpty(desc)) {
                    desc = desc.replace("\n", "<BR/>");
                }
                if (Selection.MSG_CUSTOM.equals(item) && (mCustomMessage != null)) {
                    desc = mCustomMessage.replace("\n", "<BR/>");
                }
                mItems.add(item.ordinal(),
                    new ListItem(-1, item.getLabel(res).replace("\n", "<BR/>"), desc,
                            ListItem.typeONE, null, null));
            }

            // Create the actual view to show
            mContentView = new Picker.Builder(mHostActivity)
                    .setTitle(Html.fromHtml(mTitle))
                    .setSingleChoiceItems(mItems, mInitialCheckedListItem, this)
                    .setPositiveButton(R.string.iam_done, this)
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
        mCustomMessage = savedInstanceState.getString(Key.CUSTOM_MESSAGE);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putString(Key.TITLE, mTitle);
        outState.putInt(Key.TITLE_RESOURCE_ID, mTitleResourceId);
        if (mListView != null) {
            outState.putInt(Key.CHECKED_LIST_ITEM, mListView.getCheckedItemPosition());
        }
        outState.putString(Key.CUSTOM_MESSAGE, mCustomMessage);
        super.onSaveInstanceState(outState);
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
            // Handle list item click
            if (which == Selection.MSG_CUSTOM.ordinal()) {
                // Ensure that the member variables reflect the current state
                // before this Fragment is placed in the back stack
                mInitialCheckedListItem = mListView.getCheckedItemPosition();

                // Launch next fragment to enter custom text message
                final Fragment nextFragment = EditTextFragment.newInstance(mTitle,
                        R.string.iam_done, mCustomMessage,
                        R.string.smart_actions_sms_signature_append);
                launchNextFragment(nextFragment, mTitleResourceId, false);
            }
            break;
        }
    }

    /**
     * Handles button click by returning message.
     */
    private void onPositiveButtonClicked() {
        final int position = mListView.getCheckedItemPosition();
        
        if (position >= 0 && position < Selection.values().length) {
            final Selection select = Selection.values()[position];
            String msg = null;
            switch (select) {
            case MSG_DRIVING:
            case MSG_WHEN_AVAILABLE:
            case MSG_AWAY:
                msg = select.getLabel(getResources());
                break;
            case MSG_CUSTOM:
                msg = mCustomMessage;
                break;
            default:
                Log.w(TAG, "Unrecognized selection: " + select.name());
                break;
            }
            mHostActivity.onReturn(msg, this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // This is a special code to handle the special case when the user
        // pressed return from the EditTextFragment without entering any text.
        // In that case, we'll de-select the custom message option.
        if (mInitialCheckedListItem == Selection.MSG_CUSTOM.ordinal()) {
            if (TextUtils.isEmpty(mCustomMessage) && !((AutoReplyTextActivity)mHostActivity).isCustomMessageSet()) {
                mListView.setItemChecked(Selection.MSG_CUSTOM.ordinal(), false);
            }
        }
    }
}
