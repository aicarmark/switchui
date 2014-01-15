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

package com.motorola.contextual.pickers.actions;

import java.util.ArrayList;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;

import com.motorola.contextual.actions.ActionHelper;
import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.Utils;
import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.Picker;
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

public abstract class BinaryDialogActivity extends MultiScreenPickerActivity implements Constants {

    private static final String TAG = TAG_PREFIX + BinaryDialogActivity.class.getSimpleName();

    private interface State {
        final int NO_ITEM_SELECTED = -1;
        final int OFF = 0;
        final int ON = 1;
    }

    private CharSequence[] mItems;
    private ArrayList<ListItem> mListItems = null;
    protected int mCheckedItem = State.NO_ITEM_SELECTED;

    private int[] mItemIconIds = null;
    private CharSequence[] mItemMsgs = null;
    protected CharSequence mButtonText = null;

    private CharSequence[] mBlockDescriptions;

    protected CharSequence mTitle;
    protected int mIconId = 0;
    protected String mActionKey;
    protected String mActionString;
    protected String mSettingString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBlockDescriptions = new CharSequence[] {getString(R.string.on), getString(R.string.off)};
        setContentView(createPicker().getView());
    }

    /** This method sets the on & off text for the dialog
     *
     * @param onText
     * @param offText
     */
    protected final void setItems(String onText, String offText) {
        mItems = new CharSequence[] {onText, offText};
    }

    protected final void setItems(ListItem on, ListItem off) {
        mListItems = new ArrayList<ListItem>();
        mListItems.add(on);
        mListItems.add(off);
    }

    /** This method sets the on & off icon ids for the dialog
     *
     * @param on
     * @param off
     */
    protected final void setItemIcons(int on, int off) {
        mItemIconIds = new int[] {on, off};
    }

    /** This method sets the on & off messages for the dialog
     *
     * @param on
     * @param off
     */
    protected final void setItemMsgs(String on, String off) {
        mItemMsgs = new CharSequence[] {on, off};
    }

    /** Returns the state based on the item that was checked
     *
     * @param checkedItem
     * @return state
     */
    protected final boolean getState(int checkedItem) {
        return (checkedItem == State.OFF) ? true : false;
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
        mCheckedItem = (state) ? State.OFF : State.ON ;
    }

    /** Returns the selected item
     *
     * @return
     */
    protected final int getCheckedItem() {
        return mCheckedItem;
    }

    /**
     * This method is called when an item is clicked in the dialog fragment
     *
     * @param pos
     *            - the item's position or index
     */
    protected void onDialogItemClicked(int pos) {
        mCheckedItem = pos;
        if (mButtonText == null) {
            onButtonClicked();
        }
    }

    protected void onButtonClicked() {
        if (mCheckedItem != State.NO_ITEM_SELECTED) {
            Intent intent = new Intent();

            if (LOG_INFO) {
                Log.i(TAG, "onButtonClicked = " + getConfig(getState(mCheckedItem)));
            }

            intent.putExtra(EXTRA_CONFIG, getConfig(getState(mCheckedItem)));
            intent.putExtra(EXTRA_DESCRIPTION, mBlockDescriptions[mCheckedItem]);
            setResult(RESULT_OK, intent);
        }

        finish();
    }

    private Picker createPicker() {
        Picker dialog = null;
        Intent configIntent = null;

        String config = getIntent().getStringExtra(Constants.EXTRA_CONFIG);
        if (config != null) {
            // edit case
            // TODO Refactor based on cjd comment below
            // cjd - again, having an interface for onEditConfig, onNewConfig interface methods may help separate these sort of
            //     required, but currently unstructured implementations.
            boolean state = true;
            configIntent = ActionHelper.getConfigIntent(config);
            if (configIntent != null) {
                state = configIntent.getBooleanExtra(EXTRA_STATE, true);
            }

            setCheckedItem(state);
        }

        // Setup item list to use with picker
        if (mListItems == null) {
            mListItems = new ArrayList<ListItem>();
            for (int i = 0; i < mItems.length; i++) {
                int listItemType = ((mItemMsgs != null) && (mItemMsgs[i] != null)) ?
                        ListItem.typeTWO : ListItem.typeONE;

                mListItems.add(new ListItem(
                        mItemIconIds != null ? mItemIconIds[i] : mIconId,
                                mItems[i],
                                null, listItemType, null, null));
            }
            if ((getHelpHTMLFileUrl() != null) && (configIntent == null)) {
                mListItems.add(new ListItem(R.drawable.ic_info_details,
                        getString(R.string.help_me_choose), null,
                        ListItem.typeTHREE,
                        null, new onHelpItemSelected()));
            }
        }
        else if (mItems == null) {
            if ((getHelpHTMLFileUrl() != null) && (configIntent == null)) {
                mListItems.add(new ListItem(R.drawable.ic_info_details,
                        getString(R.string.help_me_choose), null,
                        ListItem.typeTHREE,
                        null, new onHelpItemSelected()));
            }

            mItems = new CharSequence[mListItems.size()];
            for (int i = 0; i < mListItems.size(); i++) {
                mItems[i] = mListItems.get(i).mLabel;
            }
        }


        // Create picker, it will build the content view
        Picker.Builder builder = new Picker.Builder(this);
        builder.setTitle(Html.fromHtml(mTitle.toString()))
        .setIcon(mIconId)
        .setOnKeyListener(Utils.sDisableSearchKey)
        .setSingleChoiceItems(mListItems, getCheckedItem(),
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int pos) {
                onDialogItemClicked(pos);
            }
        });

        if (mButtonText != null) {
            builder.setPositiveButton(mButtonText,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int pos) {
                    onButtonClicked();
                }
            });
        }

        dialog = builder.create();
        return dialog;
    }

}
