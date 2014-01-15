/*
 * @(#)ScreenTimeoutActivity.java
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

import java.net.URISyntaxException;

import com.motorola.contextual.smartrules.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

/**
 * This class allows the user to select the Screen Timeout to be set as part of Rule activation.
 * <code><pre>
 * CLASS:
 *     Extends Activity
 *
 * RESPONSIBILITIES:
 *     Shows a dialog allowing the user to select the Timeout value.
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

public class ScreenTimeoutActivity extends Activity implements DialogInterface.OnCancelListener,
    Constants {

    private static final String TAG = TAG_PREFIX + ScreenTimeoutActivity.class.getSimpleName();
    private static final int SCREEN_TIMEOUT_DIALOG_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (savedInstanceState == null) {
            showDialog(SCREEN_TIMEOUT_DIALOG_ID);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        AlertDialog alert = null;
        if (id == SCREEN_TIMEOUT_DIALOG_ID) {

            final String entries[] = this.getResources().getStringArray(
                                         R.array.screen_timeout_entries);
            final int values[] = this.getResources().getIntArray(R.array.screen_timeout_values);

            int entryToSelect = -1;
            int savedValue = 0;
            Intent intent = getIntent();
            String configUri = intent.getStringExtra(Constants.EXTRA_CONFIG);
            if (configUri != null) {
                // edit case
                try {
                    Intent configIntent = Intent.parseUri(configUri, 0);
                    savedValue = configIntent.getIntExtra(EXTRA_TIMEOUT, 0);
                    for (int i = 0; i < values.length; ++i) {
                        if (values[i] == savedValue) {
                            entryToSelect = i;
                            break;
                        }
                    }
                } catch (URISyntaxException e) {
                    Log.w(TAG, "Received Exception when parseUri");
                }
            }

            if (LOG_INFO) Log.i(TAG, "Saved timeout value is " + savedValue + "," + entryToSelect);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.screen_timeout))
            .setIcon(R.drawable.ic_dialog_screen_timeout)
            .setOnKeyListener(Utils.sDisableSearchKey)
            .setSingleChoiceItems(entries, entryToSelect, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int item) {
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_CONFIG, ScreenTimeout.getConfig(values[item]));
                    intent.putExtra(EXTRA_DESCRIPTION, entries[item]);
                    setResult(RESULT_OK, intent);
                    finishActivity();
                }
            })
            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int id) {
                    finishActivity();
                }
            });
            alert = builder.create();
            alert.setOnCancelListener(this);
        }
        return alert;
    }

    public void onCancel(DialogInterface dialog) {
        if (!isFinishing()) {
            finish();
        }
    }

    /**
     * Dismisses the dialog and activity
     */
    private void finishActivity() {
        dismissDialog(SCREEN_TIMEOUT_DIALOG_ID);
        finish();
    }
}
