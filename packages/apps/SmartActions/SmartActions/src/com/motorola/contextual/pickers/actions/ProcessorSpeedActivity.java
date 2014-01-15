/*
 * @(#)ProcessorSpeedActivity.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * crgw47        2012/06/22  NA               Initial version
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
import com.motorola.contextual.actions.Persistence;
import com.motorola.contextual.actions.Utils;
import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.smartrules.R;

/**
 * This class allows the user to select a processor speed value to be set as part of Rule activation
 * <code><pre>
 * CLASS:
 *     Extends MultiScreenPickerActivity.
 *
 * RESPONSIBILITIES:
 *     Display the Processor speed picker
 *     The selected processor speed setting is returned in an intent to Rules Builder
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class ProcessorSpeedActivity extends MultiScreenPickerActivity implements Constants {

    private static final String TAG = TAG_PREFIX + ProcessorSpeedActivity.class.getSimpleName();

    private interface CpuMode {
        public static final int INVALID = -1;
        public static final int PERFORMANCE = 0;
        public static final int POWER_SAVER = 1;
    }

    private static boolean isCpuPowerSaveSupported = false;
    private ArrayList<ListItem> mListItems = null;
    private int mMode = CpuMode.INVALID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (LOG_INFO) Log.i(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.processor_speed_title));

        setHelpHTMLFileUrl(this.getClass());
        setContentView(createPicker().getView());
    }

    private Picker createPicker() {
        Picker alert = null;

        Intent configIntent = ActionHelper.getConfigIntent(getIntent().getStringExtra(EXTRA_CONFIG));

        //See if power saver is available
        isCpuPowerSaveSupported = Persistence.retrieveBooleanValue(getApplicationContext(), CPU_POWERSAVER_SUPPORT_KEY);

        // Setup item list to use with picker
        mListItems = new ArrayList<ListItem>();
        mListItems.add(new ListItem(null, getString(R.string.ps_performance), null, ListItem.typeONE, CpuMode.PERFORMANCE, null));
        if (isCpuPowerSaveSupported) {
            mListItems.add(new ListItem(null, getString(R.string.ps_powersaver), getString(R.string.ps_powersaver_desc), ListItem.typeONE, CpuMode.POWER_SAVER, null));
        }
        if ((getHelpHTMLFileUrl() != null) && (configIntent == null)) {
            mListItems.add(new ListItem(R.drawable.ic_info_details, getString(R.string.help_me_choose), null, ListItem.typeTHREE, null, new onHelpItemSelected()));
        }

        int checkedItem = -1;
        if (configIntent != null) {
            mMode = configIntent.getIntExtra(EXTRA_MODE, CpuMode.INVALID);
            if (mMode != CpuMode.INVALID) {
                for (int i = 0; i < mListItems.size(); i++) {
                    if ((Integer)mListItems.get(i).mMode == mMode) {
                        checkedItem = i;
                        break;
                    }
                }
            }
        }

        Picker.Builder builder = new Picker.Builder(this);
        builder.setTitle(Html.fromHtml(getString(R.string.processor_speed_prompt)))
        .setOnKeyListener(Utils.sDisableSearchKey)
        .setSingleChoiceItems(mListItems, checkedItem, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                ListItem listItem = mListItems.get(item);
                if ((listItem != null) && (listItem.mMode != null)) {
                        mMode = (Integer)listItem.mMode;
                }
            }
        })
        .setPositiveButton(getString(R.string.iam_done),
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int pos) {
                handleUserAction();
            }
        });

        alert = builder.create();
        return alert;
    }

    /**
     * Prepares the result intent to be returned to Rules Builder via activity result
     */
    private Intent prepareResultIntent() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG, getConfig(mMode));

        final String description =
                (mMode == CpuMode.PERFORMANCE) ? getString(R.string.ps_performance)
                                               : getString(R.string.ps_powersaver);

        intent.putExtra(EXTRA_MODE, mMode);
        intent.putExtra(EXTRA_DESCRIPTION, description);

        return intent;
    }

    /** Returns the state based on the item that was checked
    *
    * @param checkedItem
    * @return state
    */
  protected final static String getConfig(int mode) {
      Intent intent = new Intent();
      intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
      intent.putExtra(EXTRA_MODE, mode);
      if (LOG_DEBUG) Log.d(TAG, "getConfig : " +  intent.toUri(0));
      return intent.toUri(0);
  }

    /**
     * This method is called when positive button is clicked
     */
    public void handleUserAction() {
        setResult(RESULT_OK, prepareResultIntent());
        finish();
    }
}
