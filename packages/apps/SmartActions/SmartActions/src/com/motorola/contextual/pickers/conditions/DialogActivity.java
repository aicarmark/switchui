/*
 * @(#)DialogActivity.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
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

package com.motorola.contextual.pickers.conditions;


import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;

import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.smartprofile.Constants;

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
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public abstract class DialogActivity extends MultiScreenPickerActivity implements Constants {

    private static final String TAG = DialogActivity.class.getSimpleName();

    /**
     * mItems - Used to hold the UI strings of each precondition
     * 		This is used to show the list of Precondition supported options to the user
     *  	E.g. : For WiFi : "Connected" and "Not Connected" are stored in mItems
     */
    // TODO Refactor based on cjd comment below
    // cjd - perhaps there should be an instance of a Precondition or Trigger class, which contains,
    //    an Item, Description and Mode Description, etc. That is, not clear why there is an array of each
    //    of these types which then holds presumably different instances of Trigger items, Descriptions, etc.
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
     * mRuleConstructor - Instance of RuleConstructor sub-class, to be instantiated by individual
     *                    PreConditions.
     *
     */

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
    protected int mIcon = 0;

    /**
     * mIcon - Used to hold the icons of each precondition
     */
    protected int mIcons[] = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(LOG_DEBUG) Log.d(TAG, "OnCreate called");

        setContentView(createPicker().getView());
    }

    /**
     * Fills the RuleConstructor members
     * when an item from the dialog is chosen
     */
    private final Picker createPicker() {
        Picker dialog = null;

        final Intent incomingIntent = getIntent();
        if (incomingIntent != null) {

            final String configExtra = incomingIntent.getStringExtra(EXTRA_CONFIG);

            // edit case
            if ((configExtra != null) && (mCheckedItem== -1)) {
                setCheckedItem(configExtra);
            }
        } else {
            if(LOG_DEBUG) Log.d(TAG, " No Configured Current Mode ");
        }


        final Picker.Builder builder = new Picker.Builder(this);
        builder.setTitle(Html.fromHtml(mTitle));
        builder.setIcon(mIcon);

        builder.setOnKeyListener(new OnKeyListener() {

            public boolean onKey(final DialogInterface dialog, final int keyCode, final KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_SEARCH)
                    return true;
                return false;
            }
        });

        // Setup item list to use with picker
        final ListItem[] listItems = new ListItem[mItems.length];
        for (int i = 0; i < mItems.length; i++) {
            listItems[i] = new ListItem((mIcons != null) ? mIcons[i] : mIcon, mItems[i], null, ListItem.typeONE, null, null);
        }

        builder.setSingleChoiceItems(listItems, mCheckedItem, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int pos) {

                if(LOG_DEBUG) Log.d(TAG, "onClick called :" + pos + ":" + mItems[pos] +
                        ":" + mDescription[pos] + ":" + mModeDescption[pos]);

                final Intent returnIntent = new Intent();
                returnIntent.putExtra(EXTRA_CONFIG, mModeDescption[pos]);
                returnIntent.putExtra(EXTRA_DESCRIPTION, mDescription[pos]);
                if(LOG_INFO) Log.i(TAG, "resultsIntent : " + returnIntent.toUri(0));
                setResult(RESULT_OK, returnIntent);

                // Dismiss the dialog
                finish();
            }
        });

        dialog = builder.create();
        return dialog;
    }

    /** Sets the selected item based on the state
    *
    * @param state
    */
    protected final void setCheckedItem(final String config) {
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
}
