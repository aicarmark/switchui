/*
 * @(#)NotificationActivity.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/10  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import com.motorola.contextual.commonutils.*;
import com.motorola.contextual.smartrules.R;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.app.ActionBar;
import android.app.Fragment;
import android.view.MenuItem;
import com.motorola.contextual.smartrules.fragment.EditFragment;

/**
 * This class allows the user to select a notification to be shown as part of Rule activation.
 * <code><pre>
 * CLASS:
 *     Extends Activity
 *
 * RESPONSIBILITIES:
 *     Shows an editable text box for entering the notification text.
 *     Allows the user to optionally enable vibrate/sound alert as part
 *     of the notification.
 *     Sends the intent containing the user input to Rules Builder.
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class NotificationActivity extends PreferenceActivity implements  Constants,
    CheckBox.OnCheckedChangeListener, TextWatcher {

    private static final String TAG = TAG_PREFIX + NotificationActivity.class.getSimpleName();

    private CheckBox mVibCheckbox = null;
    private CheckBox mSoundCheckbox = null;
    private TimingPreference mRuleTimingPref = null;
    private EditText mEnterText = null;
    private boolean mDisableActionBar = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification);
        addPreferencesFromResource(R.xml.time_preference_actions);

        mEnterText = (EditText)findViewById(R.id.enter_text);
        mEnterText.setImeOptions(EditorInfo.IME_ACTION_DONE);

        mVibCheckbox = (CheckBox)findViewById(R.id.vibrate_checkbox);
        mVibCheckbox.setOnCheckedChangeListener(this);

        mSoundCheckbox = (CheckBox)findViewById(R.id.sound_checkbox);
        mSoundCheckbox.setOnCheckedChangeListener(this);

        mRuleTimingPref = (TimingPreference)findPreference(getString(R.string.Timing));
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.notif_label);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();
        setupActionBarItemsVisibility(false);

        Intent intent = getIntent();
        Intent configIntent = ActionHelper.getConfigIntent(intent.getStringExtra(Constants.EXTRA_CONFIG));
        if (configIntent != null) {
            // edit case
            mEnterText.setText(configIntent.getStringExtra(EXTRA_MESSAGE));
            mVibCheckbox.setChecked(configIntent.getBooleanExtra(EXTRA_VIBRATE, false));
            mSoundCheckbox.setChecked(configIntent.getBooleanExtra(EXTRA_SOUND, false));
            if(mRuleTimingPref != null)
               mRuleTimingPref.setSelection(configIntent.getBooleanExtra(EXTRA_RULE_ENDS, false));

        }
        enableSaveButton();


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
        mEnterText.addTextChangedListener(this);
    }

    @Override
    protected void onPause() {
        mEnterText.removeTextChangedListener(this);
        super.onPause();
        mDisableActionBar = true;
    }

    /** onOptionsItemSelected()
      *  handles key presses in ICS action bar items.
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
            String message = mEnterText.getText().toString().trim();
            Intent intent = new Intent();
            String description = null;
            boolean isVibCheckboxChecked = mVibCheckbox.isChecked();
            boolean isSoundCheckboxChecked = mSoundCheckbox.isChecked();
            if(message.length() == 0) {
                if(isVibCheckboxChecked) {
                    if(isSoundCheckboxChecked) {
                        description = getString(R.string.vibrate_and_play);
                    } else {
                        description = getString(R.string.vibrate);
                    }
                } else {
                    if(isSoundCheckboxChecked) {
                        description = getString(R.string.play_sound);
                    }
                }
            } else
                //IKCORE8-4696 - double quotes removed
                description = message;

            if (LOG_INFO) Log.i(TAG, "isVibCheckboxChecked:" + isVibCheckboxChecked +
                    ", isSoundCheckboxChecked:" + isSoundCheckboxChecked);
            intent.putExtra(EXTRA_CONFIG, SetNotification.getConfig(message, isVibCheckboxChecked,
                    isSoundCheckboxChecked, mRuleTimingPref.getSelection()));
            intent.putExtra(EXTRA_RULE_ENDS, mRuleTimingPref.getSelection());
            intent.putExtra(EXTRA_DESCRIPTION, description);
            setResult(RESULT_OK, intent);
            finish();
            result = true;
            break;
        case R.id.edit_cancel:
            result = true;
            finish();
            break;
        }
        return result;
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

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        enableSaveButton();

    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        enableSaveButton();
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Nothing to do
    }

    public void afterTextChanged(Editable s) {
        // Nothing to do
    }

    /** Enables the save button if certain constraints are met
     *
     */
    private void enableSaveButton() {
        setupActionBarItemsVisibility(!StringUtils.isTextEmpty(mEnterText.getText()) ||
                                      mVibCheckbox.isChecked() || mSoundCheckbox.isChecked());

    }

}
