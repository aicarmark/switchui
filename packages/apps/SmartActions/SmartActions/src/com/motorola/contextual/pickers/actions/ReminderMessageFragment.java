/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number          Brief Description
 * ------------- ---------- -----------------  ------------------------------
 * E11636        2012/06/19 Smart Actions 2.1  Initial Version
 */
package com.motorola.contextual.pickers.actions;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.pickers.OneOffPickerFragment;
import com.motorola.contextual.pickers.UIUtils;
import com.motorola.contextual.smartrules.R;


/**
 * This class handles the compose message UI for the guided reminder picker.  It doesn't use
 * Picker class to create the "standard" list chooser.  It extends the OneOffPickerFragment
 * to create this special purpose UI.
 *
 * <code><pre>
 *
 * CLASS:
 *  extends OneOffPickerFragment
 *
 * RESPONSIBILITIES:
 * Implements the message composer fragment of the Reminder picker.
 *
 * COLLABORATORS:
 *  ReminderActivity.java - container Activity for the Reminder picker
 *  ReminderAlertFragment.java - alert style chooser for the Reminder picker
 *  ReminderMessageFragment.java - message composer for the Reminder picker
 *  WhenFragment.java - chooser for start or end trigger condition
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class ReminderMessageFragment extends OneOffPickerFragment implements Constants {

    private Intent mInputConfigs;
    private Intent mOutputConfigs;
    private TextView mTextMessage;
    
    private static final String INPUT_CONFIGS_INTENT = "INPUT_CONFIGS_INTENT";
    private static final String OUTPUT_CONFIGS_INTENT = "OUTPUT_CONFIGS_INTENT";

    public static ReminderMessageFragment newInstance(final Intent inputConfigs, final Intent outputConfigs) {
        
        Bundle args = new Bundle();

        if (inputConfigs != null) {
            args.putParcelable(INPUT_CONFIGS_INTENT, inputConfigs);
        }

        if (outputConfigs != null) {
            args.putParcelable(OUTPUT_CONFIGS_INTENT, outputConfigs);
        }
        
        ReminderMessageFragment f = new ReminderMessageFragment();
        f.setArguments(args);
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null) {
            if (getArguments().getParcelable(INPUT_CONFIGS_INTENT) != null) {
                mInputConfigs = (Intent) getArguments().getParcelable(INPUT_CONFIGS_INTENT);
            }

            if (getArguments().getParcelable(OUTPUT_CONFIGS_INTENT) != null) {
                mOutputConfigs = (Intent) getArguments().getParcelable(OUTPUT_CONFIGS_INTENT);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        setPrompt(getString(R.string.reminder_message_prompt));
        setActionString(getString(R.string.continue_prompt));
        return v;
    }

    @Override
    protected View createCustomContentView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.text_message_input, container, false);
        mTextMessage = (TextView)v.findViewById(R.id.compose);
        if (mInputConfigs != null) {
            String message = mInputConfigs.getStringExtra(EXTRA_MESSAGE);
            if (message != null && message.length() != 0) {
                mTextMessage.setText(message);
            }
        }
        updateButtonState();
        mTextMessage.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                updateButtonState();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        return v;
    }

    private void updateButtonState() {
        CharSequence text = mTextMessage.getText();
        enableButton(text != null && TextUtils.getTrimmedLength(text) > 0);
    }

    @Override
    public void onClick(View v) {
        CharSequence text = mTextMessage.getText();

        if (text != null && text.length() != 0) {
            String message = text.toString().trim();
            mOutputConfigs.putExtra(EXTRA_MESSAGE, message);
            mHostActivity.onReturn(mOutputConfigs, this);
        }

        // Force-hide the keyboard.  It doesn't automatically do that.
        UIUtils.hideKeyboard(mHostActivity, mTextMessage);
    }
}
