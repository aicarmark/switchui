/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * XPR643        2012/06/11 Smart Actions 2.1 Initial Version
 */
package com.motorola.contextual.pickers.actions;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.actions.ActionHelper;
import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.Utils;
import com.motorola.contextual.actions.VipRinger;

import com.motorola.contextual.commonutils.StringUtils;
import com.motorola.contextual.pickers.ContactsChooserFragment;
import com.motorola.contextual.pickers.ContactsChooserFragment.ContactsInfo;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.pickers.PublisherLaunchHandler;
import com.motorola.contextual.pickers.VolumeLevelChooserFragment;

import com.motorola.contextual.smartrules.R;

/**
 * This activity presents a VIP ringer UI flow.
 * <code><pre>
 *
 * CLASS:
 *  extends MultiScreenPickerActivity - Fragment display and transition activity
 *
 * RESPONSIBILITIES:
 *  Present a VIP ringer UI flow.
 *
 * COLLABORATORS:
 *  ContactsChooserFragment.java - Chooses a specific set of contacts
 *  VolumeLevelChooserFragment.java - Specifies ringer volume levels max, high, and medium
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class VipRingerActivity extends MultiScreenPickerActivity
        implements PublisherLaunchHandler,
                   Constants,
                   ContactsChooserFragment.ContactsChooserCallback,
                   VolumeLevelChooserFragment.VolumeLevelChooserCallback {
    
    protected static final String TAG = VipRingerActivity.class.getSimpleName();

    /**
     * Keys for saving and restoring instance state bundle.
     */
    private interface Key {
        /** Saved instance state key for saving phone numbers */
        String NUMBERS = "com.motorola.contextual.pickers.actions.KEY_NUMBERS";
        /** Saved instance state key for saving contact names */
        String NAMES = "com.motorola.contextual.pickers.actions.KEY_NAMES";
        /** Saved instance state key for saving whether each contact exists in phonebook */
        String PHONEBOOK_CONTACT_FLAGS = "com.motorola.contextual.pickers.actions.KEY_PHONEBOOK_CONTACT_FLAGS";
        /** Saved instance state key for saving contacts to populate ContactsChooserFragment */
        String CONTACTS_STRING = "com.motorola.contextual.pickers.actions.KEY_CONTACTS_STRING";
        /** Saved instance state key for saving ringer volume level */
        String RINGER_VOLUME = "com.motorola.contextual.pickers.actions.KEY_RULE_TRIGGER";
    }

    /** Activity result Intent passed back to the calling activity */
    private Intent mResultIntent = null;

    /** Flag whether Activity is ready to finish */
    private boolean mIsDone = false;

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

    /** Ringer volume level in percentage */
    private int mRingerLevel = VipRinger.VOLUME_LEVEL_MAX;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.vip_ringer_title));
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
        outState.putString(Key.NUMBERS, mConInfo.getPhoneNumbers());
        outState.putString(Key.NAMES, mConInfo.getNames());
        outState.putString(Key.PHONEBOOK_CONTACT_FLAGS, mConInfo.getKnownFlags());
        outState.putString(Key.CONTACTS_STRING, mConInfo.getContactsString());
        outState.putInt(Key.RINGER_VOLUME, mRingerLevel);
    }

    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mConInfo.setPhoneNumbers(savedInstanceState.getString(Key.NUMBERS));
        mConInfo.setNames(savedInstanceState.getString(Key.NAMES));
        mConInfo.setKnownFlags(savedInstanceState.getString(Key.PHONEBOOK_CONTACT_FLAGS));
        mConInfo.setContactsString(savedInstanceState.getString(Key.CONTACTS_STRING));
        mRingerLevel = savedInstanceState.getInt(Key.RINGER_VOLUME, -1);
    }

    /**
     * Required by PublisherLaunchHandler interface.
     */
    public void onEditConfig() {
        // edit case
        final String config = getIntent().getStringExtra(Constants.EXTRA_CONFIG);
        final Intent configIntent = ActionHelper.getConfigIntent(config);
        mConInfo.setPhoneNumbers(configIntent.getStringExtra(EXTRA_NUMBER));
        mConInfo.setNames(configIntent.getStringExtra(EXTRA_NAME));
        mConInfo.setKnownFlags(configIntent.getStringExtra(EXTRA_KNOWN_FLAG));
        mRingerLevel = getRingerLevel(configIntent);
    }

    /**
     * Updates the UI with the volume level selected by the user in edit mode.
     *
     * @param configIntent
     */
    private int getRingerLevel(Intent configIntent) {
        double version = configIntent.getDoubleExtra(EXTRA_CONFIG_VERSION, VipRinger.getInitialVersion());
        if (version == VipRinger.getInitialVersion()) {
            // VipRinger.getMaxRingerVolume() does not work here (either it's buggy or
            // the semantics is different.
            AudioManager audioManager = (AudioManager)getSystemService(Activity.AUDIO_SERVICE);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
            int volume = configIntent.getIntExtra(EXTRA_RINGER_VOLUME, maxVolume);
            return (volume * 100) / maxVolume;
        } else {
            return configIntent.getIntExtra(EXTRA_VOLUME_LEVEL, VipRinger.VOLUME_LEVEL_MAX);
        }
    }

    /**
     * Required by PublisherLaunchHandler interface.
     */
    public void onFirstConfig() {
        // Launch first Fragment upon initial Activity launch
        if (mConInfo.getContactsString() == null) {
            // Existing rule action
            mConInfo.computeContactsString();
        }
        // New rule action will have null contacts string
        launchNextFragment(ContactsChooserFragment.newInstance(
                R.string.vip_ringer_question_contacts,
                R.string.continue_prompt,
                mConInfo.getContactsString()), 
            R.string.vip_ringer_question_contacts, 
            true);
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
            final int checkedListItem = VolumeLevelChooserFragment.Selection.fromVolumeLevel(mRingerLevel).ordinal();
            mNextFragInfo.nameResId = R.string.vip_ringer_question_volume;
            mNextFragInfo.frag = VolumeLevelChooserFragment.newInstance(
                    mNextFragInfo.nameResId,
                    R.string.iam_done,
                    checkedListItem);
        } else {
            Log.e(TAG, fragment.getClass().getSimpleName()
                    + " did not return expected instanceof String");
        }
    }

    /**
     * Handles return from VolumeLevelChooserFragment.
     *
     * @param fragment Fragment returning the value
     * @param returnValue Value from Fragment
     */
    public void handleVolumeLevelChooserFragment(final Fragment fragment, final Object returnValue) {
        if (returnValue instanceof String) {
            final String position = (String) returnValue;
            final VolumeLevelChooserFragment.Selection selection =
                    VolumeLevelChooserFragment.Selection.valueOf(position);
            mRingerLevel = (selection != null ? selection.getVolumeLevel() : VipRinger.VOLUME_LEVEL_MAX);
            mIsDone = true;
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
        final Intent intent = new Intent();

        //Config
        // Always vibrate
        intent.putExtra(EXTRA_CONFIG, VipRinger.getConfig(Utils.getUniqId(), mConInfo.getPhoneNumbers(),
                mConInfo.getNames(), mRingerLevel, true, mConInfo.getKnownFlags()));

        //Description
        final String description = VipRinger.getDescription(VipRingerActivity.this,
                mConInfo.getNames(), StringUtils.COMMA_STRING, mRingerLevel);
        intent.putExtra(EXTRA_DESCRIPTION, description);
        return intent;
    }
}
