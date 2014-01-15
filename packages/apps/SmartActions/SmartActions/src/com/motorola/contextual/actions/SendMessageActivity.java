/*
 * @(#)SendMessageActivity.java
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

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;
import android.app.ActionBar;
import android.app.Fragment;
import android.view.MenuItem;

import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.commonutils.*;
import com.motorola.contextual.commonutils.chips.AddressEditTextView;
import com.motorola.contextual.commonutils.chips.AddressUtil;
import com.motorola.contextual.commonutils.chips.AddressValidator;
import com.motorola.contextual.commonutils.chips.RecipientAdapter;
import com.motorola.contextual.smartrules.fragment.EditFragment;

/**
 * This class allows the user to compose a SMS to be sent as part of Rule activation. <code><pre>
 * CLASS:
 *     Extends Activity
 *
 * RESPONSIBILITIES:
 *     Shows editable text boxes for entering To/Content fields.
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
public class SendMessageActivity extends PreferenceActivity  implements  Constants,
    TextWatcher {

    private static final String TAG = TAG_PREFIX + SendMessageActivity.class.getSimpleName();

    private EditText mMessage;
    private TimingPreference mRuleTimingPref = null;
    private MultiAutoCompleteTextView mToView;
    private RecipientAdapter mAddressAdapterTo;
    private boolean mDisableActionBar = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sms);

        addPreferencesFromResource(R.xml.time_preference_actions);

        mToView = (MultiAutoCompleteTextView)findViewById(R.id.to);
        mToView.requestFocus();
        mToView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        mToView.setValidator(new AddressValidator());
        mToView.setThreshold(1);
        mAddressAdapterTo = new RecipientAdapter(this, (AddressEditTextView) mToView);
        mToView.setAdapter((RecipientAdapter) mAddressAdapterTo);

        mRuleTimingPref = (TimingPreference)findPreference(getString(R.string.Timing));
        mMessage = (EditText)findViewById(R.id.compose);
        mMessage.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mMessage.setOnFocusChangeListener(new OnFocusChangeListener() {

            public void onFocusChange(View v, boolean hasFocus) {
                String message = mMessage.getText().toString();
                if (hasFocus) {
                    if ((message == null || message.length() == 0)) {
                        mMessage.setText(SPACE + COLON + getString(R.string.smart_actions_sms_signature));
                    }
                    //Setting the signature is a one time operation
                    //So, remove the focus change listener after adding the signature
                    mMessage.setOnFocusChangeListener(null);
                }
            }
        });

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.send_a_text_message);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();
        setupActionBarItemsVisibility(false);

        Intent intent = getIntent();
        String config = intent.getStringExtra(Constants.EXTRA_CONFIG);
        Intent configIntent = ActionHelper.getConfigIntent(config);
        if (configIntent != null) {
            // edit case
            mMessage.setText(configIntent.getStringExtra(EXTRA_MESSAGE));
            String numbers = configIntent.getStringExtra(EXTRA_NUMBER);
            String names = configIntent.getStringExtra(EXTRA_NAME);
            String knownFlag = configIntent.getStringExtra(EXTRA_KNOWN_FLAG);
            if(numbers != null)
                new Utils.PopulateWidget(this, numbers, names, knownFlag, mToView).execute();

            if(mRuleTimingPref != null)
                mRuleTimingPref.setSelection(configIntent.getBooleanExtra(EXTRA_RULE_ENDS, false));

        }
        mToView.setHint(getString(R.string.To));
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
        mMessage.addTextChangedListener(this);
        mToView.addTextChangedListener(this);
    }

    /** onOptionsItemSelected()
      *  handles key presses of ICS action bar items.
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
            if (LOG_DEBUG) Log.d(TAG, "OK button clicked");
            saveAction();
            result = true;
            break;
        case R.id.edit_cancel:
            if (LOG_DEBUG) Log.d(TAG, "Cancel button clicked");
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


    @Override
    protected void onPause() {
        mMessage.removeTextChangedListener(this);
        mToView.removeTextChangedListener(this);
        super.onPause();
        mDisableActionBar = true;
    }

    /**
     * This is required by TextWatcher interface `
     */
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        enableSaveButton();
    }

    /**
     * This is required by TextWatcher interface `
     */
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Nothing to do
    }

    /**
     * This is required by TextWatcher interface `
     */
    public void afterTextChanged(Editable s) {
        // Nothing to do
    }

    /**
     * Enables the save button when a set of conditions are met
     */
    private void enableSaveButton() {
        setupActionBarItemsVisibility(!StringUtils.isTextEmpty(mToView.getText()) && !StringUtils.isTextEmpty(mMessage.getText()));
    }

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

        String message = mMessage.getText().toString();

        Intent intent = new Intent();

        //Config
        intent.putExtra(EXTRA_CONFIG, SendMessage.getConfig(phoneNumber, phoneName,
                AddressUtil.getKnownFlagsAsString(mToView.getText().toString(), StringUtils.COMMA_STRING), message,
                mRuleTimingPref.getSelection()));

        //Description
        String description = SendMessage.getDescription(SendMessageActivity.this,
                phoneName, StringUtils.COMMA_STRING, message);
        intent.putExtra(EXTRA_DESCRIPTION, description);

        intent.putExtra(EXTRA_RULE_ENDS, mRuleTimingPref.getSelection());
        setResult(RESULT_OK, intent);
        finish();
    }

}
