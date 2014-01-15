/*
 * @(#)AutoSmsActivity.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2011/08/12  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;



import com.motorola.contextual.commonutils.*;
import com.motorola.contextual.commonutils.chips.AddressEditTextView;
import com.motorola.contextual.commonutils.chips.AddressUtil;
import com.motorola.contextual.commonutils.chips.AddressValidator;
import com.motorola.contextual.commonutils.chips.RecipientAdapter;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;

import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.fragment.EditFragment;

/**
 * This class allows the user to select a set of numbers who will be sent an automatic text reply
 * when a missed call or text is received from them.
 * <code><pre>
 * CLASS:
 *     Extends Activity
 *
 * RESPONSIBILITIES:
 *     Allows the user to respond to missed calls/texts/both from a set of contacts
 *     Shows editable message to be sent to the set of contacts
 *     Allows the user to select the set of contacts who will receive an auto text reply.
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class AutoSmsActivity extends Activity implements Constants, TextWatcher {

    private static final String TAG = TAG_PREFIX + AutoSmsActivity.class.getSimpleName();

    private static final int RESPOND_TO_ALL = 0;
    private static final int RESPOND_TO_KNOWN = 1;
    private static final int RESPOND_TO_SPECIFIC = 2;

    private String mInternalName;
    private int mRespondTo;
    private String mMessage;
    private String mNumbers;
    private String mNames;

    private MultiAutoCompleteTextView mToView;
    private RecipientAdapter mAddressAdapterTo;
    private Spinner mIncomingEventsSpinner;
    private Spinner mIncomingNumbersSpinner;
    private EditText mComposedMessage;
    private boolean mDisableActionBar = false;

    private String[] mSpinnerItemHeadings;
    private String[] mSpinnerItemDescriptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.auto_sms);

        mToView = (MultiAutoCompleteTextView)findViewById(R.id.to);
        mToView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        mToView.setValidator(new AddressValidator());
        mToView.setThreshold(1);
        mAddressAdapterTo = new RecipientAdapter(this, (AddressEditTextView) mToView);
        mToView.setAdapter((RecipientAdapter) mAddressAdapterTo);
        mToView.setVisibility(View.GONE);

        mIncomingEventsSpinner = (Spinner) findViewById(R.id.respond_to_event);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.auto_sms_respond_to_event, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mIncomingEventsSpinner.setAdapter(adapter);


        mSpinnerItemHeadings = getResources().getStringArray(R.array.auto_sms_respond_to_contacts);
        mSpinnerItemDescriptions = getResources().getStringArray(R.array.auto_sms_respond_to_contacts_desc);

        mIncomingNumbersSpinner = (Spinner) findViewById(R.id.respond_to_contact);
        CustomSpinnerAdapter customAdapter = new CustomSpinnerAdapter(this, R.layout.two_line_spinner_item,
                R.id.heading, mSpinnerItemHeadings);
        mIncomingNumbersSpinner.setAdapter(customAdapter);

        mIncomingNumbersSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view,
            int position, long id) {
                enableSaveButton();
                switch (mIncomingNumbersSpinner.getSelectedItemPosition()) {
                case RESPOND_TO_ALL:
                case RESPOND_TO_KNOWN:
                    mToView.setVisibility(View.GONE);
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    if((imm != null) && (imm.isActive(mToView))) {
                        imm.hideSoftInputFromWindow(mToView.getWindowToken(), 0);
                    }
                    break;
                case RESPOND_TO_SPECIFIC:
                    mToView.setVisibility(View.VISIBLE);
                    mToView.requestFocus();
                    InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    if(im != null) {
                        im.showSoftInput(mToView, InputMethodManager.SHOW_IMPLICIT);
                    }
                    break;
                default:
                    Log.e(TAG, "Invalid number selected");
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {
                //Do nothing
            }
        });

        mIncomingNumbersSpinner.setSelection(RESPOND_TO_KNOWN);

        mComposedMessage = (EditText) findViewById(R.id.compose);
        mComposedMessage.setOnFocusChangeListener(new OnFocusChangeListener() {

            public void onFocusChange(View v, boolean hasFocus) {
                String message = mComposedMessage.getText().toString();
                if (hasFocus) {
                    if ((message == null || message.length() == 0)) {
                        mComposedMessage.setText(SPACE + COLON + getString(R.string.smart_actions_sms_signature));
                    }
                    //Setting the signature is a one time operation
                    //So, remove the focus change listener after adding the signature
                    mComposedMessage.setOnFocusChangeListener(null);
                }
            }
        });

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.auto_reply_text);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();
        setupActionBarItemsVisibility(false);


        Intent intent = getIntent();
        String config = intent.getStringExtra(Constants.EXTRA_CONFIG);
        Intent configIntent = ActionHelper.getConfigIntent(config);
        if (configIntent != null) {
            // edit case


            mInternalName = configIntent.getStringExtra(EXTRA_INTERNAL_NAME);
            mIncomingEventsSpinner.setSelection(configIntent.getIntExtra(EXTRA_RESPOND_TO, RESPOND_TO_CALLS_AND_TEXTS));
            mComposedMessage.setText(configIntent.getStringExtra(EXTRA_SMS_TEXT));

            String numbers = configIntent.getStringExtra(EXTRA_NUMBERS);
            if (numbers != null) {
                if (numbers.equals(ALL_CONTACTS)) {
                    mIncomingNumbersSpinner.setSelection(RESPOND_TO_ALL);
                } else if (numbers.equals(KNOWN_CONTACTS)) {
                    mIncomingNumbersSpinner.setSelection(RESPOND_TO_KNOWN);
                } else {
                    mIncomingNumbersSpinner.setSelection(RESPOND_TO_SPECIFIC);
                    mToView.setVisibility(View.VISIBLE);
                    mToView.requestFocus();
                    String names = configIntent.getStringExtra(EXTRA_NAME);
                    String knownFlag = configIntent.getStringExtra(EXTRA_KNOWN_FLAG);
                    if(numbers != null)
                        new Utils.PopulateWidget(this, numbers, names, knownFlag, mToView).execute();
                }
            } else {
                Log.e(TAG, "Error while restoring because numbers not found");
            }
        }
        mToView.setHint(getString(R.string.touch_here_to_add_contacts));

        enableSaveButton();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
    protected void onResume() {
        mComposedMessage.addTextChangedListener(this);
        mToView.addTextChangedListener(this);
        mDisableActionBar = false;
        super.onResume();
    }

    @Override
    protected void onPause() {
        mComposedMessage.removeTextChangedListener(this);
        mToView.removeTextChangedListener(this);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(mToView.getWindowToken(), 0);
        super.onPause();
        mDisableActionBar = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {
        // Do nothing
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        enableSaveButton();
    }

    public void afterTextChanged(Editable s) {
        // Do nothing
    }

    /**
     * Method to save the parameters selected by the user
     */
    private void saveAction() {
        mNumbers = getNumbers();
        if (mNumbers == null) {
            // Dont allow the user to save if number is not entered, throw a toast
            Toast.makeText(this, getString(R.string.no_number),Toast.LENGTH_SHORT).show();
            return;
        }
        mNames = (!mNumbers.equals(ALL_CONTACTS) && !mNumbers.equals(KNOWN_CONTACTS)) ?
                 (AddressUtil.getNamesAsString(mToView.getText().toString(), StringUtils.COMMA_STRING)) : null;
        if (mInternalName == null)
            mInternalName = Utils.getUniqId();
        mRespondTo = mIncomingEventsSpinner.getSelectedItemPosition();
        mMessage = mComposedMessage.getText().toString();

        if (LOG_INFO)
            Log.i(TAG, "Saving action with internal name: " + mInternalName + ", respond flag: " + mRespondTo +
                  ", message: " + mMessage + " and numbers: "+mNumbers);

        setResult(RESULT_OK, prepareResultIntent());
        finish();
    }

    /**
     * Method to create the intent to be sent to ModeManager which is stored in the rule
     * @return intent Intent containing the details of the Action composed by the user
     */
    private Intent prepareResultIntent() {
        String description = DOUBLE_QUOTE + mMessage + DOUBLE_QUOTE;


        Intent intent = new Intent();

        //Config
        intent.putExtra(EXTRA_CONFIG, AutoSms.getConfig(mInternalName, mNumbers, mNames, mMessage,
                AddressUtil.getKnownFlagsAsString(mToView.getText().toString(), StringUtils.COMMA_STRING), mRespondTo));

        intent.putExtra(EXTRA_DESCRIPTION, description);
        intent.putExtra(EXTRA_RULE_ENDS, false);
        return intent;

    }

    /**
     * Enables the save button when a set of conditions are met
     */
    private void enableSaveButton() {
        setupActionBarItemsVisibility((!StringUtils.isTextEmpty(mToView.getText()) ||
                                       (mIncomingNumbersSpinner.getSelectedItemPosition() == RESPOND_TO_ALL) ||
                                       (mIncomingNumbersSpinner.getSelectedItemPosition() == RESPOND_TO_KNOWN)) &&
                                      !StringUtils.isTextEmpty(mComposedMessage.getText()));
    }

    /**
     * Method to return the numbers selected by the user
     * @return Numbers selected by the user
     */
    private String getNumbers() {
        int selected = mIncomingNumbersSpinner.getSelectedItemPosition();
        String numbers = null;
        switch (selected) {
        case RESPOND_TO_ALL:
            numbers = ALL_CONTACTS;
            break;
        case RESPOND_TO_KNOWN:
            numbers = KNOWN_CONTACTS;
            break;
        case RESPOND_TO_SPECIFIC:
        	String inputContacts = mToView.getText().toString();
            String phoneName = AddressUtil.getNamesAsString(inputContacts, StringUtils.COMMA_STRING);
            numbers = AddressUtil.getNumbersAsString(inputContacts, StringUtils.COMMA_STRING);
            if(LOG_INFO) {
                Log.i(TAG, "Selected names: "+phoneName+", Selected numbers: "+numbers);
            }
            if(StringUtils.isEmpty(phoneName)&& StringUtils.isEmpty(numbers)) {
                return null;
            }
            break;
        }
        return numbers;
    }

    private class CustomSpinnerAdapter extends ArrayAdapter<String> {
        private LayoutInflater mInflator;

        public CustomSpinnerAdapter(Context context, int resource, int textViewResourceId,
                                    String[] objects) {
            super(context, resource, textViewResourceId, objects);
            mInflator = getLayoutInflater();
        }

        @Override
        public View getDropDownView(int position, View convertView,
                                    ViewGroup parent) {
            if (convertView == null)
                convertView = mInflator.inflate(R.layout.two_line_spinner_item, parent, false);

            TextView heading = (TextView)convertView.findViewById(R.id.heading);
            heading.setText(mSpinnerItemHeadings[position]);

            CheckedTextView description = (CheckedTextView)convertView.findViewById(R.id.desc);
            description.setText(mSpinnerItemDescriptions[position]);
            description.setChecked(mIncomingNumbersSpinner.getSelectedItemPosition() == position);

            return convertView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = mInflator.inflate(R.layout.two_line_spinner_item, parent, false);

            TextView heading = (TextView)convertView.findViewById(R.id.heading);
            heading.setText(mSpinnerItemHeadings[position]);

            return heading;
        }

    }

}
