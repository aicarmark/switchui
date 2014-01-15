/*
 * @(#)TimeFrameRepeatPreference.java
 *
 * (c) COPYRIGHT 2010-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/02/24   NA               Initial Version
 *
 */
package com.motorola.contextual.smartprofile.sensors.timesensor;

import com.motorola.contextual.commonutils.StringUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

/** This class extends the EditTextPreference to customize the behavior.
 *  The positive button in the dialog should be disabled if there is no text in the edit box.
 *
 * CLASS:
 *  Extends EditTextPreference
 *
 * RESPONSIBILITIES:
 *  1. Standard EditTextPreference with disabled positive button if there is no text
 *
 * COLABORATORS:
 *  None
 *
 * USAGE:
 *  See each method.
 *
 */
public class TimeFrameNamePreference extends EditTextPreference implements TextWatcher {

    public TimeFrameNamePreference(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
    }


    public void onTextChanged(CharSequence s, int start, int before, int count){}

    public void beforeTextChanged(CharSequence s, int start, int before, int count){}

    public void afterTextChanged(Editable s) {
        setPositiveButtonEnabled(!StringUtils.isEmpty(getEditText().getText().toString()));
    }

    /**
     * Method to enable/disable positive button in the dialog
     * @param enabled boolean to enable/disable the button
     */
    protected void setPositiveButtonEnabled(boolean enabled) {
        Dialog dlg = getDialog();
        if(dlg instanceof AlertDialog) {
            AlertDialog alertDlg = (AlertDialog)dlg;
            alertDlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(enabled);
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        getEditText().addTextChangedListener(this);
        setPositiveButtonEnabled(!StringUtils.isEmpty(getEditText().getText().toString()));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        getEditText().removeTextChangedListener(this);
        super.onDialogClosed(positiveResult);
    }
}
