/*
 * @(#)BinaryDialogActivity.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/09  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import com.motorola.contextual.smartrules.R;

/**
 * Base class for actions which want to show an On/Off Dialog <code><pre>
 * CLASS:
 *     Extends Activity
 *
 * RESPONSIBILITIES:
 *     Shows a dialog allowing the user to select On/Off for that setting.
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

public abstract class BinaryDialogActivity extends Activity implements Constants {

    private static final int DIALOG_ID = 1;
    private static final int NO_ITEM_SELECTED = -1;
    private static final String DIALOG_FRAGMENT_TAG = "BINARY_DIALOG";

    private CharSequence[] mItems;
    protected int mCheckedItem = NO_ITEM_SELECTED;

    protected CharSequence mTitle;
    protected int mIconId = 0;
    protected String mActionKey;
    protected String mActionString;
    protected String mSettingString;

    private BinaryDialogFragment mDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    /** This method sets the on & off text for the dialog
     *
     * @param onText
     * @param offText
     */
    protected final void setItems(String onText, String offText) {
        mItems = new CharSequence[] {onText, offText};
    }

    /** Returns the state based on the item that was checked
    *
    * @param checkedItem
    * @return state
    */
    protected final boolean getState(int checkedItem) {
        return (checkedItem == 0) ? true : false;
    }

    /** Returns the config based on the state
    *
    * @param state
    * @return config
    */
   public String getConfig(boolean state) {
       Intent intent = new Intent();
       intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
       intent.putExtra(EXTRA_STATE, state);
       return intent.toUri(0);
   }

    /** Sets the selected item based on the state
     *
     * @param state
     */
    protected final void setCheckedItem(boolean state) {
        mCheckedItem = (state) ? 0 : 1 ;
    }

    /** Returns the selected item
     *
     * @return
     */
    protected final int getCheckedItem() {
        return mCheckedItem;
    }

    /**
     * This method creates the dialog fragment and executes show on it
     */
    protected void showDialog() {
        mDialogFragment = BinaryDialogFragment.newInstance(DIALOG_ID);
        mDialogFragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
    }

    /**
     * This method is called when an item is clicked in the dialog fragment
     *
     * @param pos
     *            - the item's position or index
     */
    protected void onDialogItemClicked(int pos) {
        Intent intent = new Intent();
        
        intent.putExtra(EXTRA_CONFIG, getConfig(getState(pos)));
        intent.putExtra(EXTRA_DESCRIPTION, mItems[pos]);
        setResult(RESULT_OK, intent);
        if (mDialogFragment != null) {
            mDialogFragment.dismiss();
        }
        finish();
    }

    /**
     * This method is called when negative button is clicked in the dialog
     * fragment
     */
    protected void onNegativeButtonClicked() {
        if (mDialogFragment != null) {
            mDialogFragment.dismiss();
        }
        finish();
    }

    /**
     * This method is called when the dialog is canceled
     */
    protected void onDialogCancelled() {
        if (!isFinishing()) {
            finish();
        }
    }

    /**
     * This class extends DialogFragment and is responsible for showing alert
     * dialog
     *
     * @author wkh346
     *
     */
    public static class BinaryDialogFragment extends DialogFragment {

        /**
         * The string for adding and obtaining the dialog id from set of
         * arguments
         */
        private static final String BINARY_DIALOG_ID = "BINARY_DIALOG_ID";

        /**
         * Reference to the activity hosting the dialog fragment
         */
        private BinaryDialogActivity mActivity;

        /**
         * public empty constructor
         */
        public BinaryDialogFragment() {
            // Nothing to be done here
        }

        /**
         * This method returns a newly created and initialized instance of
         * dialog fragment
         *
         * @param dialogId
         *            - unique id for identifying a dialog
         * @return - a newly created and initialized instance of dialog fragment
         */
        public static BinaryDialogFragment newInstance(int dialogId) {
            BinaryDialogFragment dialogFragment = new BinaryDialogFragment();
            Bundle arguments = new Bundle();
            arguments.putInt(BINARY_DIALOG_ID, dialogId);
            dialogFragment.setArguments(arguments);
            return dialogFragment;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            mActivity.onDialogCancelled();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
        	
            AlertDialog dialog = null;
            int id = getArguments().getInt(BINARY_DIALOG_ID);
            mActivity = (BinaryDialogActivity) getActivity();
            if (id == DIALOG_ID) {
                String config = mActivity.getIntent().getStringExtra(Constants.EXTRA_CONFIG);
                if (config != null) {      
                    // edit case
                    boolean state = true;
                    Intent configIntent = ActionHelper.getConfigIntent(config);
                    if (configIntent != null) {
                        state = configIntent.getBooleanExtra(EXTRA_STATE, true);
                    }
                    mActivity.setCheckedItem(state);
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                builder.setTitle(mActivity.mTitle)
                .setIcon(mActivity.mIconId)
                .setOnKeyListener(Utils.sDisableSearchKey)
                .setSingleChoiceItems(mActivity.mItems,
                                      mActivity.getCheckedItem(),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                    int pos) {
                        mActivity.onDialogItemClicked(pos);
                    }
                })
                .setNegativeButton(
                    this.getResources().getString(R.string.cancel),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog,
                    int id) {
                        mActivity.onNegativeButtonClicked();
                    }
                });
                dialog = builder.create();
            }
            return dialog;
        }
    }

}
