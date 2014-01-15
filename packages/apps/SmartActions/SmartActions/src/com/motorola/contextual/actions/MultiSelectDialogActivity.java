/*
 * @(#)MultiSelectDialogActivity.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * rdq478       2011/09/28  IKMAIN-28588      Initial version
 * rdq478       2011/11/08  IKINTNETAPP-458   Changed to separate each item by 'and'
 */

package com.motorola.contextual.actions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.motorola.contextual.smartrules.R;

/**
 * This based class to allow user to build multiple selections activity.
 * <code><pre>
 * CLASS:
 *     Extends Activity
 *
 * RESPONSIBILITIES:
 *     Does show a dialog allowing user to make selection(s).
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

public abstract class MultiSelectDialogActivity extends Activity implements
                      DialogInterface.OnCancelListener, Constants {

    private static final String TAG = TAG_PREFIX + MultiSelectDialogActivity.class.getSimpleName();
    private static final int DIALOG_ID = 1;

    protected CharSequence[] mItems = null;
    protected boolean[] mCheckedItems = null;
    protected int mIconId = 0;
    protected CharSequence mTitle = null;
    protected CharSequence mMessage = null;
    protected AlertDialog mAlertDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Calls Activity's showDialog method
     */
    protected void showDialog() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        showDialog(DIALOG_ID);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {

        if (id == DIALOG_ID) {
            String configUri = getIntent().getStringExtra(EXTRA_CONFIG);
            mCheckedItems = getSettingFromConfigUri(configUri);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(mTitle)
                   .setIcon(mIconId)
                   .setMessage(mMessage)
                   .setOnKeyListener(Utils.sDisableSearchKey)
                   .setMultiChoiceItems(mItems, mCheckedItems, new DialogInterface.OnMultiChoiceClickListener() {

                       public void onClick(DialogInterface dialog, int pos, boolean isChecked) {
                           mCheckedItems[pos] = isChecked;

                           // disable Save button if nothing checked
                           mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isChecked());
                       }
                   })
                  .setPositiveButton(this.getResources().getString(R.string.save),
                      new DialogInterface.OnClickListener() {

                         public void onClick(DialogInterface dialog, int id) {
                            sendResult(RESULT_OK);
                         }
                      })
                  .setNegativeButton(this.getResources().getString(R.string.cancel),
                      new DialogInterface.OnClickListener() {

                         public void onClick(DialogInterface dialog, int id) {
                             sendResult(RESULT_CANCELED);
                         }
                      });

            mAlertDialog = builder.create();
            mAlertDialog.setOnCancelListener(this);
        }

        return mAlertDialog;
    }

    /**
     * This method will be invoked when the dialog is canceled
     */
    public void onCancel(DialogInterface dialog) {
        if (!isFinishing()) {
            finish();
        }
    }

    /**
     * Retrieves the previous setting from the configUri
     *
     * @param configUri
     *            - the intent uri containing relevant extras
     * @return boolean[] - list of true/false of checked/unchecked items
     */
    protected abstract boolean[] getSettingFromConfigUri(String configUri);

    /**
     * Get display string.
     * @return String - to display
     */
    protected String getDisplayString() {
        StringBuilder display = new StringBuilder();

        if (mCheckedItems == null) {
            Log.e(TAG, "Error mCheckedItems is null.");
            return display.toString();
        }

        int size = mCheckedItems.length;

        for (int i = 0; i < size; i++) {
            if (mCheckedItems[i]) {
                if (display.length() > 0) {
                    display.append(SPACE);
                    display.append(this.getResources().getString(R.string.and));
                    display.append(SPACE);
                }

                display.append(mItems[i]);
            }
        }

        return display.toString();
     }

    /**
     * Prepares the result intent to be returned to Rules Builder via activity result
     * @return Intent
     */
    protected abstract Intent prepareResultIntent();

    /**
     * Look through the list to see if there is any item checked or not.
     * @return true if at least one item checked.
     */
    protected boolean isChecked() {
        if (mCheckedItems == null) {
            Log.e(TAG, "Error mCheckedItems is null.");
            return false;
        }

        int size = mCheckedItems.length;

        for (int i = 0; i < size; i++) {
            if (mCheckedItems[i]) return true;
        }

        return false;
    }

    /**
     * Send result back to caller
     * @param result - result code OK or CANCEL
     * @return
     */
    protected void sendResult(int result) {
        Intent intent = prepareResultIntent();
        setResult(result, intent);

        Log.i(TAG, "send result: result = " + result + " intent = " + intent.toURI());

        dismissDialog(DIALOG_ID);
        finish();
    }
}
