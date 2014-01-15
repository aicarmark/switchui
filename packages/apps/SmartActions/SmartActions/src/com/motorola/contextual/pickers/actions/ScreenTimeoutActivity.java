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

package com.motorola.contextual.pickers.actions;

import java.net.URISyntaxException;

import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.ScreenTimeout;
import com.motorola.contextual.actions.Utils;
import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.smartrules.R;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;

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

public class ScreenTimeoutActivity extends MultiScreenPickerActivity implements Constants {

    private static final String TAG = TAG_PREFIX + ScreenTimeoutActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setActionBarTitle(getString(R.string.screen_timeout_title));
        setContentView(createPicker().getView());
    }

    private Picker createPicker() {
        Picker alert = null;

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

        // Setup item list to use with picker
        ListItem[] listItems = new ListItem[entries.length];
        for (int i = 0; i < entries.length; i++) {
            listItems[i] = new ListItem(null, entries[i], null, ListItem.typeONE, null, null);
        }

        if (LOG_INFO) Log.i(TAG, "Saved timeout value is " + savedValue + "," + entryToSelect);
        Picker.Builder builder = new Picker.Builder(this);
        builder.setTitle(Html.fromHtml(getString(R.string.screen_timeout_prompt)))
        .setOnKeyListener(Utils.sDisableSearchKey)
        .setSingleChoiceItems(listItems, entryToSelect, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int item) {
                Intent intent = new Intent();
                intent.putExtra(EXTRA_CONFIG, ScreenTimeout.getConfig(values[item]));
                intent.putExtra(EXTRA_DESCRIPTION, entries[item]);
                setResult(RESULT_OK, intent);
                finishActivity();
            }
        });
        alert = builder.create();

        return alert;
    }

    /**
     * Dismisses the dialog and activity
     */
    private void finishActivity() {
        finish();
    }
}
