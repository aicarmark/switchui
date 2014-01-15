/*
 * @(#)DialogActivity.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2010/11/26  NA                Initial version of DialogActivity
 *                                            for PreConditions
 *
 */

package com.motorola.contextual.smartprofile;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;

import com.motorola.contextual.smartrules.R;

/**
 * This class is a generic class to handle dialog display functionality for dialog based
 * Preconditions.
 *
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends Activity
 *     Implements DialogInterface.OnCancelListener
 *     Implements Constants
 *
 * RESPONSIBILITIES:
 * This class is a generic class to handle dialog display functionality for dialog based
 * Preconditions. Individual Preconditions derive from this class and set their respective
 * UI strings to be displayed in the dialog. This class makes use of RuleConstructor for
 * constructing the rules based on the chosen user option in the dialog.
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public abstract class DialogActivity extends Activity implements DialogInterface.OnCancelListener,
    Constants {

    private static final String TAG = DialogActivity.class.getSimpleName();
    private static final int DIALOG_ID = 1;

    /**
     * mItems - Used to hold the UI strings of each precondition
     * 		This is used to show the list of Precondition supported options to the user
     *  	E.g. : For WiFi : "Connected" and "Not Connected" are stored in mItems
     */
    protected String[] mItems;

    /**
     * mDescription - Used to hold the UI description of each precondition
     * 		      E.g. :"When WiFi is connected", "When WiFi is disconnected" are
     * 		      stored in mDescription
     */
    protected String[] mDescription;


    /**
     * mModeDescption - Used to hold the mode description of each precondition
     * 		        for rule construction. This is used to make sure
     * 			rules do not change with the changes in the UI.
     * 			E.g: WiFi UI string could be "Not Connected" or "When
     * 			WiFi is not connected" or something else, which might
     * 			keep varying based on CXD requirements.
     * 			However, mModeDescption contains "NotConnected" string
     * 			which is used in rule construction, no matter whatever
     * 			is the UI string.
     *
     */
    protected String[] mModeDescption;


    /**
     * mTitle - Title of individual Precondition dialog
     */
    protected String mTitle;

    /**
     * mCheckedItem - Used for Precondition editing.
     */
    protected int mCheckedItem = -1;

    /**
     * mIcon - Icon of individual Precondition
     */
    protected int mIcon;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(LOG_DEBUG) Log.d(TAG, "OnCreate called");
    }

    /**
     * Used to show the dialog
     * To be called from the sub classes as soon as they are launched
     */
    protected final void showDialog() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        showDialog(DIALOG_ID);
    }

    /** Sets the selected item based on the state
    *
    * @param state
    */
    protected final void setCheckedItem(String config) {
        if (LOG_INFO) {
            Log.i(TAG, "setCheckedItem "
                  + " config = " + config);
        }

        for(int index = 0; index < mItems.length; index++) {
            if(config.equals(mModeDescption[index])) {
                mCheckedItem = index;
                break;
            }
        }

        //BOTA upgrade fix. Old rules contain mItems[index]
        if (mCheckedItem == -1) {
            for(int index = 0; index < mItems.length; index++) {
                if(config.equals(mItems[index])) {
                    mCheckedItem = index;
                    break;
                }
            }
        }
    }

    /**
     * Called on creating the dialog. Fills the RuleConstructor members
     * when an item from the dialog is chosen
     */
    @Override
    protected final Dialog onCreateDialog(final int id, final Bundle args) {
        AlertDialog dialog = null;
        if (id == DIALOG_ID) {

            Intent incomingIntent = getIntent();
            if (incomingIntent != null) {

                String configExtra = incomingIntent.getStringExtra(EXTRA_CONFIG);

                // edit case
                if(configExtra != null) {
                    setCheckedItem(configExtra);
                }
            } else {
                if(LOG_DEBUG) Log.d(TAG, " No Configured Current Mode ");
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(mTitle);
            builder.setIcon(mIcon);

            builder.setOnKeyListener(new OnKeyListener() {

                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_SEARCH)
                        return true;
                    return false;
                }
            });

            builder.setSingleChoiceItems(mItems, mCheckedItem, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int pos) {

                    if(LOG_DEBUG) Log.d(TAG, "onClick called :" + pos + ":" + mItems[pos] +
                                           ":" + mDescription[pos] + ":" + mModeDescption[pos]);

                    Intent returnIntent = new Intent();
                    returnIntent.putExtra(EXTRA_CONFIG, mModeDescption[pos]);
                    returnIntent.putExtra(EXTRA_DESCRIPTION, mDescription[pos]);
                    if(LOG_INFO) Log.i(TAG, "resultsIntent : " + returnIntent.toUri(0));
                    setResult(RESULT_OK, returnIntent);

                    // Dismiss the dialog
                    dismissDialog(DIALOG_ID);
                    finish();
                }
            });
            builder.setNegativeButton(this.getResources().getString(R.string.cancel),
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Dismiss the dialog
                    dismissDialog(DIALOG_ID);
                    finish();
                }
            });

            dialog = builder.create();
            dialog.setOnCancelListener(this);
        }
        return dialog;

    }

    /**
     * This is called when the user cancels the dialog
     * @param  dialog
     */
    public final void onCancel(final DialogInterface dialog) {
        if(!isFinishing()) {
            finish();
        }
    }
}
