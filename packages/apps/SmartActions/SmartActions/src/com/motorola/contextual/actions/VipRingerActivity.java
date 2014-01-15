/*
 * @(#)VipRingerActivity.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/08/03  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;


import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.Toast;

import com.motorola.contextual.commonutils.StringUtils;
import com.motorola.contextual.commonutils.chips.AddressEditTextView;
import com.motorola.contextual.commonutils.chips.AddressUtil;
import com.motorola.contextual.commonutils.chips.AddressValidator;
import com.motorola.contextual.commonutils.chips.RecipientAdapter;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.fragment.EditFragment;

/**
 * This class allows the user to select Ringer settings for specific contacts (VIPs)
 * <code><pre>
 * CLASS:
 *     Extends Activity
 *
 * RESPONSIBILITIES:
 *     Shows a screen allowing the user to select Ringer & Vibrate Settings.
 *     Allows the user to select specific numbers for which these Ringer settings are applicable
 *     A preview tone for the ringer loudness is played when user moves the volume slider.
 *     Sends the intent containing the setting to Rules Builder.
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */


public class VipRingerActivity extends Activity implements Constants, TextWatcher {
    private static final String TAG = TAG_PREFIX + VipRingerActivity.class.getSimpleName();

    private static final int LEVEL_MAX = 0;
    private static final int LEVEL_HIGH = 1;
    private static final int LEVEL_MEDIUM = 2;

    public static boolean stopRing = false;
    private boolean mDisableActionBar = false;

    private CheckBox mVibrateCheckBox;
    private Spinner mVolumeLevel;

    private MultiAutoCompleteTextView mToView;
    private RecipientAdapter mAddressAdapterTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.vip_ringer);

        mToView = (MultiAutoCompleteTextView)findViewById(R.id.to);
        mToView.requestFocus();
        mToView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        mToView.setValidator(new AddressValidator());
        mToView.setThreshold(1);
        mAddressAdapterTo = new RecipientAdapter(this, (AddressEditTextView) mToView);
        mToView.setAdapter((RecipientAdapter) mAddressAdapterTo);
        mToView.setHint(getString(R.string.touch_here_to_add_contacts));

        mVolumeLevel = (Spinner) findViewById(R.id.ringer_volume_level);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.volume_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mVolumeLevel.setAdapter(adapter);

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.vip_ringer);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();
        setupActionBarItemsVisibility(false);

        setupUI();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mDisableActionBar = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDisableActionBar = false;
        mToView.addTextChangedListener(this);
    }

    @Override
    protected void onPause() {
        mToView.removeTextChangedListener(this);
        super.onPause();
        mDisableActionBar = true;
    }

    /** onOptionsItemSelected()
      * handles key presses in ICS action bar items.
      */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            result = true;
            break;
        case R.id.edit_save:
            saveAction();
            result = true;
            break;
        case R.id.edit_cancel:
            result = true;
            finish();
            break;
        }
        return result;
    }

    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {
        // Do nothing
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        setupActionBarItemsVisibility(StringUtils.isTextEmpty(mToView.getText()) ? false : true);
    }

    public void afterTextChanged(Editable s) {
        // Do nothing
    }

    /**
     * This method sets up visibility for the action bar items.
     * @param enableSaveButton - whether save button needs to be enabled
     */
    protected void setupActionBarItemsVisibility(boolean enableSaveButton) {
        if(mDisableActionBar) return;
        int editFragmentOption = EditFragment.EditFragmentOptions.DEFAULT;
        if(enableSaveButton)
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_ENABLED;
        else
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_DISABLED;
        // Add menu items from fragment
    	Fragment fragment = EditFragment.newInstance(editFragmentOption, false);
        getFragmentManager().beginTransaction().replace(R.id.edit_fragment_container, fragment, null).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Sets up the UI of the Activity depending upon fireUri received from ModeManager
     */
    private void setupUI() {

        mVibrateCheckBox = (CheckBox)findViewById(R.id.ringer_vibrate);
        mVibrateCheckBox.setChecked(true);

        Intent intent = getIntent();
        String config = intent.getStringExtra(Constants.EXTRA_CONFIG);
        Intent configIntent = ActionHelper.getConfigIntent(config);
        if (configIntent != null) {
            // edit case

            boolean vibSetting = configIntent.getBooleanExtra(EXTRA_VIBE_STATUS, false);

            mVibrateCheckBox.setChecked(vibSetting);

            setSelectedRingerVolume(configIntent);

            String numbers = configIntent.getStringExtra(EXTRA_NUMBER);
            String names = configIntent.getStringExtra(EXTRA_NAME);
            String knownFlag = configIntent.getStringExtra(EXTRA_KNOWN_FLAG);
            new Utils.PopulateWidget(this, numbers, names, knownFlag, mToView).execute();
        }

    }

    /**
     * Method to save the parameters selected by the user
     *
     */
    private void saveAction() {
        String inputContacts = mToView.getText().toString();
        String phoneName = AddressUtil.getNamesAsString(inputContacts, StringUtils.COMMA_STRING);
        String phoneNumber = AddressUtil.getNumbersAsString(inputContacts, StringUtils.COMMA_STRING);
        if(LOG_INFO) {
            Log.i(TAG, phoneName);
            Log.i(TAG, phoneNumber);
        }

        // Dont allow the user to save if number is not
        // entered, throw a toast
        if(StringUtils.isEmpty(phoneName)&& StringUtils.isEmpty(phoneNumber)) {

            Toast.makeText(this,
                           getString(R.string.no_number),Toast.LENGTH_SHORT).show();
            return;

        }

        Intent intent = new Intent();
        int volumeLevel = getSelectedRingerVolumeLevel(mVolumeLevel.getSelectedItemPosition());


        //Config
        intent.putExtra(EXTRA_CONFIG, VipRinger.getConfig(Utils.getUniqId(), phoneNumber,
                phoneName, volumeLevel, mVibrateCheckBox.isChecked(),
                AddressUtil.getKnownFlagsAsString(mToView.getText().toString(), StringUtils.COMMA_STRING)));

        //Description
        String description = VipRinger.getDescription(VipRingerActivity.this, phoneName,
                StringUtils.COMMA_STRING, volumeLevel);
        intent.putExtra(EXTRA_DESCRIPTION, description);

        setResult(RESULT_OK, intent);
        finish();
    }
 

    /** Returns the ringer volume level configured by the user.
     *   Max - 100%, High - 75%, Medium - 50% 
     * @param level
     * @return
     */
    private static int getSelectedRingerVolumeLevel(int level) {
        int volumeLevel = VipRinger.VOLUME_LEVEL_MAX;

        if (level == LEVEL_HIGH ) {
            volumeLevel = VipRinger.VOLUME_LEVEL_HIGH;
        } else if (level == LEVEL_MEDIUM ) {
            volumeLevel = VipRinger.VOLUME_LEVEL_MEDIUM;
        }

        return volumeLevel;
    }

    /** Updates the UI with the volume level selected by the user in edit mode.
     * 
     * @param configIntent
     */
    private void setSelectedRingerVolume(Intent configIntent) {

        int level = 0;
        double version = configIntent.getDoubleExtra(EXTRA_CONFIG_VERSION, VipRinger.getInitialVersion());
        if (version == VipRinger.getInitialVersion()) {
            int maxVolume = VipRinger.getMaxRingerVolume(this);
            int ringerVolume = configIntent.getIntExtra(EXTRA_RINGER_VOLUME, maxVolume);
            level = getSelectedLevel(ringerVolume , maxVolume);

        } else {

            level = getSelectedLevel(configIntent.getIntExtra(EXTRA_VOLUME_LEVEL, VipRinger.VOLUME_LEVEL_MAX));
        }
        mVolumeLevel.setSelection(level);
    }

    /** Returns the level to be set in the UI based on the passed in volume level.
     * 
     * @param volumeLevel
     * @return
     */
    private static int getSelectedLevel(int volumeLevel) {
        int level = LEVEL_MAX;
        if (volumeLevel == VipRinger.VOLUME_LEVEL_HIGH) {
            level = LEVEL_HIGH;
        } else if (volumeLevel == VipRinger.VOLUME_LEVEL_MEDIUM) {
           level = LEVEL_MEDIUM;
        }
        return level;
    }

    /** Returns the level to be set in the UI based on the passed in ringer volume and max volume.
     * 
     * @param ringerVolume
     * @param maxVolume
     * @return
     */
    private static int getSelectedLevel(int ringerVolume, int maxVolume) {
        int level = LEVEL_MAX;
        int highVolume = 3* maxVolume/4;
        int mediumVolume = maxVolume/2;
        if (ringerVolume == highVolume) {
            level = LEVEL_HIGH;
        } else if (ringerVolume == mediumVolume) {
            level = LEVEL_MEDIUM;
        } 
        return level;
    }
}
