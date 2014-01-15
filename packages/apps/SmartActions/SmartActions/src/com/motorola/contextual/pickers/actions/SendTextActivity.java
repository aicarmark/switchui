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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.actions.ActionHelper;
import com.motorola.contextual.actions.ActionHelper.RuleTriggerTime;
import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.SendMessage;

import com.motorola.contextual.commonutils.StringUtils;
import com.motorola.contextual.pickers.ContactsChooserFragment;
import com.motorola.contextual.pickers.ContactsChooserFragment.ContactsInfo;
import com.motorola.contextual.pickers.EditTextFragment;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.pickers.PublisherLaunchHandler;

import com.motorola.contextual.smartrules.R;

/**
 * This activity presents a send text message UI flow.
 * <code><pre>
 *
 * CLASS:
 *  extends MultiScreenPickerActivity - Fragment display and transition activity
 *
 * RESPONSIBILITIES:
 *  Present a send text message UI flow.
 *
 * COLLABORATORS:
 *  ContactsChooserFragment.java - Chooses a specific set of contacts
 *  EditTextFragment.java - Inputs custom user text
 *  WhenFragment.java - Specifies if rule action executes at start or end
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class SendTextActivity extends MultiScreenPickerActivity implements PublisherLaunchHandler,
                                                                           Constants,
                                                                           WhenFragment.WhenCallback,
                                                                           ContactsChooserFragment.ContactsChooserCallback,
                                                                           EditTextFragment.EditTextCallback {
    protected static final String TAG = SendTextActivity.class.getSimpleName();

    /**
     * Keys for saving and restoring instance state bundle.
     */
    private interface Key {
        /** Saved instance state key for saving text message */
        String MESSAGE = "com.motorola.contextual.pickers.actions.KEY_MESSAGE";
        /** Saved instance state key for saving phone numbers */
        String NUMBERS = "com.motorola.contextual.pickers.actions.KEY_NUMBERS";
        /** Saved instance state key for saving contact names */
        String NAMES = "com.motorola.contextual.pickers.actions.KEY_NAMES";
        /** Saved instance state key for saving whether each contact exists in phonebook */
        String PHONEBOOK_CONTACT_FLAGS = "com.motorola.contextual.pickers.actions.KEY_PHONEBOOK_CONTACT_FLAGS";
        /** Saved instance state key for saving rule trigger times */
        String RULE_TRIGGER = "com.motorola.contextual.pickers.actions.KEY_RULE_TRIGGER";
        /** Saved instance state key for saving contacts to populate ContactsChooserFragment */
        String CONTACTS_STRING = "com.motorola.contextual.pickers.actions.KEY_CONTACTS_STRING";
    }

    /** Activity result Intent passed back to the calling activity */
    private Intent mResultIntent = null;

    /** Flag whether Activity is ready to finish */
    private static boolean mIsDone = false;

    /**
     * Fragment holder and associated text.
     */
    private class FragmentInfo {
        /** Next fragment to launch after handling any prior fragment's return value */
        public Fragment frag = null;
        /** Next fragment's title question text resource ID */
        public int nameResId = -1;
    };
    private static FragmentInfo mNextFragInfo = null;

    /** Contact names and phone numbers */
    private static ContactsInfo mConInfo = null;

    /** Text message to be sent */
    private String mMessage = null;

    /** When rule should execute: start  (default), end, or both start and end */
    private static RuleTriggerTime mRuleTrigger = RuleTriggerTime.START;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.send_a_text_message_title));
        mNextFragInfo = new FragmentInfo();
        mConInfo = new ContactsInfo();
        if (savedInstanceState == null) {
            // Initial activity launch
            final Intent intent = getIntent();
            final String config = intent.getStringExtra(Constants.EXTRA_CONFIG);
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
        outState.putString(Key.MESSAGE, mMessage);
        outState.putString(Key.NUMBERS, mConInfo.getPhoneNumbers());
        outState.putString(Key.NAMES, mConInfo.getNames());
        outState.putString(Key.PHONEBOOK_CONTACT_FLAGS, mConInfo.getKnownFlags());
        outState.putString(Key.RULE_TRIGGER, mRuleTrigger.name());
        outState.putString(Key.CONTACTS_STRING, mConInfo.getContactsString());
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mMessage = savedInstanceState.getString(Key.MESSAGE);
        mConInfo.setPhoneNumbers(savedInstanceState.getString(Key.NUMBERS));
        mConInfo.setNames(savedInstanceState.getString(Key.NAMES));
        mConInfo.setKnownFlags(savedInstanceState.getString(Key.PHONEBOOK_CONTACT_FLAGS));
        mRuleTrigger = RuleTriggerTime.valueOf(savedInstanceState.getString(Key.RULE_TRIGGER));
        mConInfo.setContactsString(savedInstanceState.getString(Key.CONTACTS_STRING));
    }

    /**
     * Required by PublisherLaunchHandler interface.
     */
    public void onEditConfig() {
        // edit case
        final String config = getIntent().getStringExtra(Constants.EXTRA_CONFIG);
        final Intent configIntent = ActionHelper.getConfigIntent(config);
        mMessage = configIntent.getStringExtra(EXTRA_MESSAGE);
        mConInfo.setPhoneNumbers(configIntent.getStringExtra(EXTRA_NUMBER));
        mConInfo.setNames(configIntent.getStringExtra(EXTRA_NAME));
        mConInfo.setKnownFlags(configIntent.getStringExtra(EXTRA_KNOWN_FLAG));
        // Translate rule trigger times
        final boolean isAtRuleEnd = configIntent.getBooleanExtra(EXTRA_RULE_ENDS, false);
        if (isAtRuleEnd) {
            mRuleTrigger = RuleTriggerTime.END;
        }
    }

    /**
     * Required by PublisherLaunchHandler interface.
     */
    public void onFirstConfig() {
        // Launch first Fragment upon initial Activity launch
        if (mConInfo.getContactsString() == null) {
            // Existing rule action if non-null CSV lists for names and phone numbers
            mConInfo.computeContactsString();
        }
        // New rule action will have null contacts string
        launchNextFragment(ContactsChooserFragment.newInstance(
                R.string.send_text_question_contacts,
                R.string.continue_prompt,
                mConInfo.getContactsString()),
            R.string.send_text_question_contacts,
            true);
    }

    /**
     * Handles return from WhenFragment.
     *
     * @param fragment Fragment returning the value
     * @param returnValue Value from Fragment
     */
    public void handleWhenFragment(final Fragment fragment, final Object returnValue) {
        if (returnValue instanceof Intent) {
            final Intent outputConfigs = (Intent) returnValue;
            final boolean isSendAtRuleEnd = outputConfigs.getBooleanExtra(EXTRA_RULE_ENDS, false);
            mRuleTrigger = (isSendAtRuleEnd ? RuleTriggerTime.END : RuleTriggerTime.START);
            mIsDone = true;
        } else {
            Log.e(TAG, fragment.getClass().getSimpleName()
                    + " did not return expected instanceof Intent");
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

            // Set up next fragment
            mNextFragInfo.nameResId = R.string.send_text_question_msg;
            mNextFragInfo.frag = EditTextFragment.newInstance(
                    getString(mNextFragInfo.nameResId),
                    R.string.continue_prompt,
                    mMessage,
                    -1);
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

            // Prepare arguments to next fragment
            final Intent inputConfigs = new Intent();
            final Intent outputConfigs = new Intent();

            // Default value is when rule STARTS
            inputConfigs.putExtra(EXTRA_RULE_ENDS, (mRuleTrigger == RuleTriggerTime.END));

            // Set up next fragment
            mNextFragInfo.nameResId = R.string.send_text_question_when;
            mNextFragInfo.frag = WhenFragment.newInstance(inputConfigs, outputConfigs, mNextFragInfo.nameResId, R.string.iam_done);
        } else {
            Log.e(TAG, fragment.getClass().getSimpleName()
                    + " did not return expected instanceof String");
        }
    }

    /**
     * Returns values from fragments.
     * Passes a fragment reference and its return value to container Activity.
     *
     * @param fragment Fragment returning the value
     * @param returnValue Value from Fragment
     */
    @Override
    public void onReturn(final Object returnValue, final PickerFragment fragment) {
        if (fragment == null) {
            Log.w(TAG, "null return fragment");
        } else if (returnValue == null) {
            Log.w(TAG, "null return value");
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
     * Utility method to get user readable description of names and message.
     *
     * @param context Caller's context
     * @param names Names of the contacts
     * @param delimiter Delimiter separating the names
     * @param message Message to be sent
     * @return User readable description
     */
    public static String getDescription(Context context, String names, String delimiter,
            String message) {
        // Copied from com.motorola.contextual.actions.SendMessage
        String[] nameArr = names.split(StringUtils.COMMA_STRING);
        StringBuilder descriptionBuilder = new StringBuilder();
        if (nameArr.length > 1) {
            descriptionBuilder.append(nameArr[0]).append(SPACE)
            .append(context.getString(R.string.plus))
            .append(SPACE).append(nameArr.length - 1);
        } else {
            descriptionBuilder.append(nameArr[0]);
        }
        descriptionBuilder.append(NEW_LINE).append(DOUBLE_QUOTE)
        .append(message).append(DOUBLE_QUOTE);
        return context.getString(R.string.send_text) + descriptionBuilder.toString();
    }

    /**
     * Saves the parameters selected by the user.
     */
    private void saveAction() {
        mResultIntent = prepareResultIntent();
    }

    /**
     * Creates the intent sent to the ModeManager stored in the rule.
     *
     * @return intent Intent containing the details of the Action composed by the user
     */
    private Intent prepareResultIntent() {
        final String description = getDescription(this,
                mConInfo.getNames(), StringUtils.COMMA_STRING, mMessage);
        final boolean isAtRuleEnd = (mRuleTrigger == RuleTriggerTime.END);

        final Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG,
                SendMessage.getConfig(mConInfo.getPhoneNumbers(), mConInfo.getNames(),
                        mConInfo.getKnownFlags(), mMessage,
                        isAtRuleEnd));
        intent.putExtra(EXTRA_DESCRIPTION, description);
        intent.putExtra(EXTRA_RULE_ENDS, isAtRuleEnd);

        return intent;
    }
}
