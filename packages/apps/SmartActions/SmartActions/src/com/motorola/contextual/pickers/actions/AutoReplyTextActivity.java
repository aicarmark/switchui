/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * XPR643        2012/05/23 Smart Actions 2.1 Initial Version
 */
package com.motorola.contextual.pickers.actions;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.motorola.contextual.actions.ActionHelper;
import com.motorola.contextual.actions.ActionHelper.Params;
import com.motorola.contextual.actions.AutoSms;
import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.Utils;

import com.motorola.contextual.pickers.ContactsChooserFragment;
import com.motorola.contextual.pickers.ContactsChooserFragment.ContactsInfo;
import com.motorola.contextual.pickers.EditTextFragment;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.pickers.PublisherLaunchHandler;

import com.motorola.contextual.smartrules.R;

/**
 * This activity presents an auto reply text UI flow.
 * <code><pre>
 *
 * CLASS:
 *  extends MultiScreenPickerActivity - Fragment display and transition activity
 *
 * RESPONSIBILITIES:
 *  Present an auto reply text UI flow.
 *
 * COLLABORATORS:
 *  AutoReplyTextWhoFragment.java - Chooses the contacts to interact with
 *  AutoReplyTextWhatFragment.java - Chooses the contact methods to react to
 *  AutoReplyTextMessageFragment.java - Specifies a text message to reply with
 *  ContactsChooserFragment.java - Chooses a specific set of contacts
 *  EditTextFragment.java - Inputs custom user text
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class AutoReplyTextActivity extends MultiScreenPickerActivity
        implements PublisherLaunchHandler, Constants, AutoReplyTextWhoFragment.AutoReplyTextWhoCallback,
                                                      AutoReplyTextWhatFragment.AutoReplyTextWhatCallback,
                                                      AutoReplyTextMessageFragment.AutoReplyTextMessageCallback,
                                                      ContactsChooserFragment.ContactsChooserCallback,
                                                      EditTextFragment.EditTextCallback {
    protected static final String TAG = AutoReplyTextActivity.class.getSimpleName();

    /**
     * Keys for saving and restoring instance state bundle.
     */
    private interface Key {
        /** Saved instance state key for saving internal name */
        String INTERNAL_NAME = "com.motorola.contextual.pickers.actions.KEY_INTERNAL_NAME";
        /** Saved instance state key for saving respond-to value */
        String RESPOND_TO = "com.motorola.contextual.pickers.actions.KEY_RESPOND_TO";
        /** Saved instance state key for saving text message */
        String MESSAGE = "com.motorola.contextual.pickers.actions.KEY_MESSAGE";
        /** Saved instance state key for saving phone numbers */
        String NUMBERS = "com.motorola.contextual.pickers.actions.KEY_NUMBERS";
        /** Saved instance state key for saving contact names */
        String NAMES = "com.motorola.contextual.pickers.actions.KEY_NAMES";
        /** Saved instance state key for saving whether each contact exists in phonebook */
        String PHONEBOOK_CONTACT_FLAGS = "com.motorola.contextual.pickers.actions.KEY_PHONEBOOK_CONTACT_FLAGS";
        /** Saved instance state key for saving contacts to populate ContactsChooserFragment */
        String CONTACTS_STRING = "com.motorola.contextual.pickers.actions.KEY_CONTACTS_STRING";
    }

    /**
     * Fragment holder and associated text.
     */
    private class FragmentInfo {
        /** Next fragment to launch after handling any prior fragment's return value */
        public Fragment frag = null;
        /** Next fragment's title question text resource ID */
        public int nameResId = -1;
    };
    private FragmentInfo mNextFragInfo = null;

    /** Contact names and phone numbers */
    private ContactsInfo mConInfo = null;

    /** Activity result Intent passed back to the calling activity */
    private Intent mResultIntent = null;

    /** Flag whether Activity is ready to finish */
    private boolean mIsDone = false;

    /** Flag to indicate that the user created a custom message */
    private boolean mCustomMessageSet = false;

    /** Text message to be sent */
    private String mMessage = null;

    /** SMS-related internal name */
    private String mInternalName = null;
    /** Contact method to respond to, such as calls or text messages */
    private int mRespondTo = AutoReplyTextWhatFragment.Selection.REPLY_TEXT_AND_CALLS.getRespondToValue();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.auto_reply_text_title));
        mNextFragInfo = new FragmentInfo();
        mConInfo = new ContactsInfo();
        if (savedInstanceState == null) {
            // Initial activity launch
            final Intent intent = getIntent();
            final String config = intent.getStringExtra(EXTRA_CONFIG);
            final Intent configIntent = ActionHelper.getConfigIntent(config);
            if (configIntent != null) {
                // Assign current values to state variables
                onEditConfig();
            }
            // First config also handles edit case, when state variables are assigned current values
            onFirstConfig();
        }
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Key.INTERNAL_NAME, mInternalName);
        outState.putInt(Key.RESPOND_TO, mRespondTo);
        outState.putString(Key.MESSAGE, mMessage);
        outState.putString(Key.NUMBERS, mConInfo.getPhoneNumbers());
        outState.putString(Key.NAMES, mConInfo.getNames());
        outState.putString(Key.PHONEBOOK_CONTACT_FLAGS, mConInfo.getKnownFlags());
        outState.putString(Key.CONTACTS_STRING, mConInfo.getContactsString());
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mInternalName = savedInstanceState.getString(Key.INTERNAL_NAME);
        mRespondTo = savedInstanceState.getInt(Key.RESPOND_TO, AutoReplyTextWhatFragment.Selection.REPLY_TEXT_AND_CALLS.getRespondToValue());
        mMessage = savedInstanceState.getString(Key.MESSAGE);
        mConInfo.setPhoneNumbers(savedInstanceState.getString(Key.NUMBERS));
        mConInfo.setNames(savedInstanceState.getString(Key.NAMES));
        mConInfo.setKnownFlags(savedInstanceState.getString(Key.PHONEBOOK_CONTACT_FLAGS));
        mConInfo.setContactsString(savedInstanceState.getString(Key.CONTACTS_STRING));
    }

    /**
     * Required by PublisherLaunchHandler interface.
     */
    public void onEditConfig() {
        // edit case
        final String config = getIntent().getStringExtra(EXTRA_CONFIG);
        final Intent configIntent = ActionHelper.getConfigIntent(config);
        mInternalName = configIntent.getStringExtra(Params.EXTRA_INTERNAL_NAME);
        mRespondTo = configIntent.getIntExtra(Params.EXTRA_RESPOND_TO, AutoReplyTextWhatFragment.Selection.REPLY_TEXT_AND_CALLS.getRespondToValue());
        mMessage = configIntent.getStringExtra(Params.EXTRA_SMS_TEXT);
        mConInfo.setPhoneNumbers(configIntent.getStringExtra(Params.EXTRA_PHONE_NUMBERS));
        mConInfo.setNames(configIntent.getStringExtra(Params.EXTRA_NAME));
        mConInfo.setKnownFlags(configIntent.getStringExtra(Params.EXTRA_KNOWN_FLAG));
    }

    /**
     * Keep this method package-private.
     *
     * @return true if a custom message is set.
     */
    boolean isCustomMessageSet() {
        return mCustomMessageSet;
    }

    /**
     * Required by PublisherLaunchHandler interface.
     */
    public void onFirstConfig() {
        // Launch first Fragment upon initial Activity launch
        int checkedListItem = (mConInfo.getPhoneNumbers() == null) ? 0 : AutoReplyTextWhoFragment.Selection
                .fromReplyTo(mConInfo.getPhoneNumbers()).ordinal();

        if (getFragmentManager().findFragmentByTag(getString(R.string.auto_reply_question_who)) == null) {
            launchNextFragment(AutoReplyTextWhoFragment.newInstance(R.string.auto_reply_question_who, checkedListItem),
                    R.string.auto_reply_question_who, true);
        }
    }

    /**
     * Handles return from AutoReplyTextWhoFragment.
     *
     * @param fragment Fragment returning the value
     * @param returnValue Value from Fragment
     */
    public void handleAutoReplyTextWhoFragment(final Fragment fragment, final Object returnValue) {
        if (returnValue instanceof String) {
            final String position = (String) returnValue;
            final AutoReplyTextWhoFragment.Selection selection =
                    AutoReplyTextWhoFragment.Selection.valueOf(position);
            switch (selection) {
            case REPLY_ALL:
            {
                // Set up next fragment
                final int checkedListItem = AutoReplyTextWhatFragment.Selection.fromRespondTo(mRespondTo).ordinal();
                mNextFragInfo.nameResId = R.string.auto_reply_question_what;
                mNextFragInfo.frag = AutoReplyTextWhatFragment.newInstance(mNextFragInfo.nameResId, checkedListItem);
                mConInfo.setPhoneNumbers(Params.ALL_CONTACTS);
                mConInfo.setNames(null);
                break;
            }
            case REPLY_PHONEBOOK:
            {
                // Set up next fragment
                final int checkedListItem = AutoReplyTextWhatFragment.Selection.fromRespondTo(mRespondTo).ordinal();
                mNextFragInfo.nameResId = R.string.auto_reply_question_what;
                mNextFragInfo.frag = AutoReplyTextWhatFragment.newInstance(mNextFragInfo.nameResId, checkedListItem);
                mConInfo.setPhoneNumbers(Params.KNOWN_CONTACTS);
                mConInfo.setNames(null);
                break;
            }
            case REPLY_SPECIFIC_CONTACTS:
            {
                if (mConInfo.getContactsString() == null) {
                    mConInfo.computeContactsString();
                }
                mNextFragInfo.nameResId = R.string.auto_reply_question_contacts;
                mNextFragInfo.frag = ContactsChooserFragment.newInstance(mNextFragInfo.nameResId,
                        R.string.continue_prompt, mConInfo.getContactsString());
                break;
            }
            default:
                Log.w(TAG, "Unrecognized selection from "
                        + fragment.getClass().getSimpleName() + ": "
                        + selection.name());
                break;
            }
        } else {
            Log.e(TAG, fragment.getClass().getSimpleName()
                    + " did not return expected instanceof String");
        }
    }

    /**
     * Handles return value from AutoReplyTextWhatFragment.
     * More specifically, converts the UI selection to values understood by the
     * Smart Actions engine and sets up the next fragment to be launched.
     *
     * @param fragment Fragment returning the value
     * @param returnValue Value from Fragment
     */
    public void handleAutoReplyTextWhatFragment(final Fragment fragment, final Object returnValue) {
        // Check for a valid returnValue type before proceeding to handle the fragment result
        if (returnValue instanceof String) {
            final String position = (String) returnValue;
            final AutoReplyTextWhatFragment.Selection selection =
                    AutoReplyTextWhatFragment.Selection.valueOf(position);

            // Set up next default fragment
            String customMsg = mMessage;
            int checkedListItem = AutoReplyTextMessageFragment.Selection.fromMessage(getResources(), mMessage).ordinal();
            if (checkedListItem == AutoReplyTextMessageFragment.Selection.MSG_CUSTOM.ordinal()) {
                // Don't select any list items if custom message is selected but does not exist
                if (TextUtils.isEmpty(customMsg)) {
                    checkedListItem = -1;
                }
            } else {
                // If not custom message, don't display the message in the custom list item
                customMsg = null;
            }

            mNextFragInfo.nameResId = R.string.auto_reply_question_msg;
            mNextFragInfo.frag = AutoReplyTextMessageFragment.newInstance(mNextFragInfo.nameResId, checkedListItem, customMsg);

            mRespondTo = selection.getRespondToValue();
        } else {
            Log.e(TAG, fragment.getClass().getSimpleName()
                    + " did not return expected instanceof String");
        }
    }

    /**
     * Handles return from AutoReplyTextMessageFragment.
     *
     * @param fragment Fragment returning the value
     * @param returnValue Value from Fragment
     */
    public void handleAutoReplyTextMessageFragment(final Fragment fragment, final Object returnValue) {
        if (returnValue instanceof String) {
            mMessage = (String) returnValue;
            mIsDone = true;
        } else {
            Log.e(TAG, fragment.getClass().getSimpleName()
                    + " did not return expected instanceof String");
        }
    }

    /**
     * Handles return from ContactsChooserFragment.
     *
     * @param fragment Fragment returning the value
     * @param returnValue Value from Fragment
     */
    public void handleContactsChooserFragment(final Fragment fragment, final Object returnValue) {
        if (returnValue instanceof String) {
            mConInfo.setContactsString((String) returnValue);
            // Compute CSV lists for phone numbers, names, and known phonebook contacts flags
            mConInfo.computeCsvListsFromContactsString();

            // Set up next default fragment
            final int checkedListItem = AutoReplyTextWhatFragment.Selection.fromRespondTo(mRespondTo).ordinal();
            mNextFragInfo.nameResId = R.string.auto_reply_question_what;
            mNextFragInfo.frag = AutoReplyTextWhatFragment.newInstance(mNextFragInfo.nameResId, checkedListItem);
        } else {
            Log.e(TAG, fragment.getClass().getSimpleName()
                    + " did not return expected instanceof String");
        }
    }

    /**
     * Handles return from EditTextFragment.
     *
     * @param fragment Fragment returning the value
     * @param returnValue Value from Fragment
     */
    public void handleEditTextFragment(final Fragment fragment, final Object returnValue) {
        if (returnValue instanceof String) {
            mMessage = (String) returnValue;
            if (!TextUtils.isEmpty(mMessage)) {
                mCustomMessageSet = true;
                mIsDone = true;
            }
        } else {
            Log.e(TAG, fragment.getClass().getSimpleName()
                    + " did not return expected instanceof String");
        }
    }

    /**
     * Returns values from fragments.
     * Passes a fragment reference and its return value to container Activity.
     *
     * @param returnValue Value from Fragment
     * @param fragment Fragment returning the value
     */
    @Override
    public void onReturn(final Object returnValue, final PickerFragment fragment) {
        if (fragment == null) {
            Log.w(TAG, "null return fragment");
        } else if (returnValue == null) {
            Log.w(TAG, "null return value");
        } else if (fragment instanceof EditTextFragment) {
            handleEditTextFragment(fragment, returnValue);
        } else {
            fragment.handleResult(returnValue, fragment);
        }

        // Launch next Fragment, pop Fragment, or set result and finish Activity
        if (mNextFragInfo.frag == null) {
            if (mIsDone) {
                // Set Activity result and finish
                saveAction();
                setResult(RESULT_OK, mResultIntent);
                finish();
            } else {
                // Pop Fragment off back stack, or finish if no back stack
                final FragmentManager fragmentMan = getFragmentManager();
                if (fragmentMan.getBackStackEntryCount() > 0) {
                    fragmentMan.popBackStack();
                } else {
                    Log.e(TAG, "Bad state: Cannot return activity result");
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        } else {
            // Display next Fragment
            launchNextFragment(mNextFragInfo.frag, mNextFragInfo.nameResId, false);
            mNextFragInfo.frag = null;
            mNextFragInfo.nameResId = -1;
        }
    }

    /**
     * Saves the parameters selected by the user.
     */
    private void saveAction() {
        if (mInternalName == null) {
            mInternalName = Utils.getUniqId();
        }
        mResultIntent = prepareResultIntent();
    }

    /**
     * Creates the intent sent to the ModeManager stored in the rule.
     *
     * @return intent Intent containing the details of the Action composed by the user
     */
    private Intent prepareResultIntent() {
        final String description = new StringBuilder()
            .append(DOUBLE_QUOTE).append(mMessage).append(DOUBLE_QUOTE)
            .toString();

        final Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG, AutoSms.getConfig(mInternalName, mConInfo.getPhoneNumbers(),
                                                mConInfo.getNames(), mMessage,
                                                mConInfo.getKnownFlags(), mRespondTo));

        intent.putExtra(EXTRA_DESCRIPTION, description);
        intent.putExtra(EXTRA_RULE_ENDS, false);
        return intent;
    }
}
