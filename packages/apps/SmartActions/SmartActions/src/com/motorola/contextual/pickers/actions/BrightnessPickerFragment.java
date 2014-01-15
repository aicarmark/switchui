/*
 * @(#)CalendarActivity.java
 *
 * (c) COPYRIGHT 2010-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * MXDN83        2012/05/30 NA                Initial version
 *
 */
package com.motorola.contextual.pickers.actions;

import java.util.ArrayList;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.motorola.contextual.actions.Brightness;
import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.Persistence;
import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * This fragment presents a list of different brightness options to choose from.
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment - picker fragment base class
 *
 * RESPONSIBILITIES:
 * This fragment presents a list of different brightness options to choose from.
 *
 * COLLABORATORS:
 *  N/A
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class BrightnessPickerFragment extends PickerFragment implements
    Constants, OnSeekBarChangeListener, DialogInterface.OnClickListener {

    private ArrayList<ListItem> mItems;
    private int mBrightness;
    private boolean isDisplayCurveSupported = false;

    private static final String INPUT_CONFIGS_INTENT = "INPUT_CONFIGS_INTENT";
    private static final String OUTPUT_CONFIGS_INTENT = "OUTPUT_CONFIGS_INTENT";
    
    public static BrightnessPickerFragment newInstance(final Intent inputConfigs, final Intent outputConfigs) {
        Bundle args = new Bundle();

        if (inputConfigs != null) {
            args.putParcelable(INPUT_CONFIGS_INTENT, inputConfigs);
        }

        if (outputConfigs != null) {
            args.putParcelable(OUTPUT_CONFIGS_INTENT, outputConfigs);
        }

        BrightnessPickerFragment f = new BrightnessPickerFragment();
        f.setArguments(args);
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            if (getArguments().getParcelable(INPUT_CONFIGS_INTENT) != null) {
                mInputConfigs = (Intent) getArguments().getParcelable(INPUT_CONFIGS_INTENT);
            }

            if (getArguments().getParcelable(OUTPUT_CONFIGS_INTENT) != null) {
                mOutputConfigs = (Intent) getArguments().getParcelable(OUTPUT_CONFIGS_INTENT);
            }
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
        final Bundle savedInstanceState) {
        if(mContentView == null) {
            isDisplayCurveSupported = Persistence.retrieveBooleanValue(
                    mHostActivity, DISPLAY_CURVE_SUPPORT_KEY);
            mItems = new ArrayList<ListItem>();
            mItems.add(new ListItem(-1, getString(R.string.manual_adjust),
                            ListItem.typeFOUR, new Integer(Brightness.Mode.MANUAL), BACKLIGHT_RANGE, this));
            mItems.add(new ListItem(-1, getString(R.string.auto_adjust),
                            null, ListItem.typeONE, new Integer(Brightness.Mode.AUTOMATIC), null));
            if(isDisplayCurveSupported) {
                mItems.add(new ListItem(-1, getString(R.string.smart_adjust),
                                getString(R.string.smart_adjust_desc),
                                ListItem.typeONE, new Integer(Brightness.Mode.SMART), null));
            }
            int selectedPos = -1;
            if (mInputConfigs != null) {
                mSelectedItem = new Integer(mInputConfigs.getIntExtra(EXTRA_MODE,
                    AUTOMATIC_NOT_SUPPORTED));
                switch(((Integer)mSelectedItem).intValue()) {
                case Brightness.Mode.MANUAL:
                    selectedPos = 0;
                    mBrightness = mInputConfigs.getIntExtra(EXTRA_BRIGHTNESS, 0);
                    break;
                case Brightness.Mode.AUTOMATIC:
                    selectedPos = 1;
                    break;
                case Brightness.Mode.SMART:
                    selectedPos = 2;
                    break;
                }
            }else {
                mItems.add(new ListItem(R.drawable.ic_info_details, getString(R.string.help_me_choose),
                        null, ListItem.typeTHREE, null, mHostActivity.new onHelpItemSelected()));
            }
            if (mBrightness == 0) {
                // By default set it to 50%
                mItems.get(0).mSeekBarParams.currentProgress = BACKLIGHT_RANGE / 2;
            } else {
                mItems.get(0).mSeekBarParams.currentProgress = mBrightness - MINIMUM_BACKLIGHT;
            }
            //TODO: figure out how to update the help files instead of hardcoding the url
            mHostActivity.setHelpHTMLFileUrl(BrightnessActivity.class);
            final Picker picker = new Picker.Builder(mHostActivity)
            .setTitle(Html.fromHtml(getString(R.string.brightness_prompt)))
            .setSingleChoiceItems(mItems, selectedPos, null)
            .setPositiveButton(getString(R.string.iam_done), this).create();
            mContentView = picker.getView();

        }
        return mContentView;
    }

    /**
     * preview the brightness setting for the current position
     * and set the listitem seekbar params currentProgress
     * to the current progress so UI is updated when the list
     * adapter is called to render the list again
     *
     * Required by SeekBar.onSeekBarChangeListener interface
     */
    public void onProgressChanged(final SeekBar seekBar, final int progress,
            final boolean fromUser) {
        // Preview the brightness setting for that position
        mBrightness = progress + MINIMUM_BACKLIGHT;
        ((BrightnessActivity)mHostActivity).setBrightnessValue(mBrightness);
        if(fromUser) {
            mItems.get(0).mSeekBarParams.currentProgress = progress;
        }
    }

    /**
     * Required by SeekBar.onSeekBarChangeListener interface
     */
    public void onStartTrackingTouch(final SeekBar seekBar) {

    }

    /**
     * sets the item checked once user stops dragging the seekbar
     *
     * Required by SeekBar.onSeekBarChangeListener interface
     */
    public void onStopTrackingTouch(final SeekBar seekBar) {
        final ListView list = (ListView)getView().findViewById(R.id.list);
        list.setItemChecked(0, true);
    }

    /**
     * Handles the done bottom button click event
     *
     * Required by DialogInterface.onClickListener interface
     */
    public void onClick(final DialogInterface dialog, final int which) {
        final ListView list = (ListView)getView().findViewById(R.id.list);
        final int pos = list.getCheckedItemPosition();
        if(pos >= 0 && pos < mItems.size()) {
            final ListItem item = mItems.get(pos);
            if(mOutputConfigs == null) {
                mOutputConfigs = new Intent();
            }
            final int mode = ((Integer)item.mMode).intValue();
            mOutputConfigs.putExtra(EXTRA_MODE, mode);
            if(mode == Brightness.Mode.MANUAL) {
                mOutputConfigs.putExtra(EXTRA_BRIGHTNESS, mBrightness);
            }
            mHostActivity.onReturn(mOutputConfigs, this);
        }
    }
}
