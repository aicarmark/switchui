/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR              Author      Description
 * 2011-05-17   IKMAIN-15824    C23137      initial
 */

package com.motorola.filemanager.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.motorola.filemanager.R;

/**
 * Dialog to extract Zip file with or without encryption
 */
public class ExtractDialog extends AlertDialog implements View.OnClickListener {

    //private final String LOGTAG ="ZipDialog";

    private View mView;
    private EditText mPassword;

    public ExtractDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.extract_dialog, null);
        setView(mView);
        setInverseBackgroundForced(true);

        setTitle(R.string.extract);
        setIcon(0); // No icon in title bar
        mView.findViewById(R.id.type).setVisibility(View.VISIBLE);
        mPassword = (EditText) mView.findViewById(R.id.password);
        mPassword.setText("");

        ((CheckBox) mView.findViewById(R.id.show_password)).setOnClickListener(this);

        super.onCreate(savedInstanceState);
    }

    public void onClick(View view) {
        boolean isChecked = false;
        if (view instanceof CheckBox) {
            isChecked = ((CheckBox) view).isChecked();
        }
        mPassword.setInputType(InputType.TYPE_CLASS_TEXT |
                (isChecked ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        : InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }

    public String getPassword() {
        return mPassword.getText().toString();
    }
}
