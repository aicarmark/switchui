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

package com.motorola.contextual.pickers.actions;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;

import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.Utils;
import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.Picker;
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

public abstract class MultiSelectDialogActivity extends MultiScreenPickerActivity implements
                      DialogInterface.OnCancelListener, Constants {

    private static final String TAG = TAG_PREFIX + MultiSelectDialogActivity.class.getSimpleName();

    protected CharSequence[] mItems = null;
    protected boolean[] mCheckedItems = null;
    protected int mIconId = 0;
    protected CharSequence mTitle = null;
    protected CharSequence mMessage = null;
    protected Picker mPicker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createPicker().getView());
    }

    private Picker createPicker() {
            String configUri = getIntent().getStringExtra(EXTRA_CONFIG);
            // cjd - what if configURI is null or whatever? throw new IllegalArgumentException(EXTRA_CONFIG+" param required in intent)
            mCheckedItems = getSettingFromConfigUri(configUri);

            // Setup item list to use with picker
            ListItem[] listItems = new ListItem[mItems.length];
            for (int i = 0; i < mItems.length; i++) {
                listItems[i] = new ListItem(mIconId, mItems[i], null, ListItem.typeONE, null, null);
            }

            Picker.Builder builder = new Picker.Builder(this);
            builder.setTitle(Html.fromHtml(mTitle.toString()))
                   .setOnKeyListener(Utils.sDisableSearchKey)
                   .setMultiChoiceItems(listItems, mCheckedItems, new DialogInterface.OnMultiChoiceClickListener() {

                       public void onClick(DialogInterface dialog, int pos, boolean isChecked) {
                           mCheckedItems[pos] = isChecked;
                       }
                   })
                  .setPositiveButton(this.getResources().getString(R.string.iam_done),
                      new DialogInterface.OnClickListener() {

                         public void onClick(DialogInterface dialog, int id) {
                            sendResult(RESULT_OK);
                         }
                      });

            mPicker = builder.create();

        return mPicker;
    }

    /**
     * This method will be invoked when the dialog is canceled
     */
    public void onCancel(DialogInterface dialog) {
    	// why the dialog param if not used? 
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
                	// cjd - this stuff drives me nuts from an internationalization perspective.
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
        	// cjd - should this be a log condition or something more drastic? not sure, just asking.
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

        finish();
    }
}
