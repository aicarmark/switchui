/*
 * Copyright (C) 2012, Motorola, Inc,
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * May 10, 2012	  MXDN83       Created file
 **********************************************************
 */

package com.motorola.contextual.pickers.conditions.charging;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.motorola.contextual.pickers.CustomListAdapter;
import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * This fragment presents a charging status chooser.
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment - Fragment base class for pickers
 *
 * RESPONSIBILITIES:
 *  Presents various charging status options to choose from
 *  and sends the chosen option back to the host activity.
 *
 * COLLABORATORS:
 *  N/A
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class ChargingStatusFragment extends PickerFragment implements
        OnClickListener, ChargingConstants {

    //reference to the list view
    private ListView mListView;

    /**
     * All subclasses of Fragment must include a public empty constructor. The framework
     * will often re-instantiate a fragment class when needed, in particular during state
     * restore, and needs to be able to find this constructor to instantiate it. If the
     * empty constructor is not available, a runtime exception will occur in some cases
     * during state restore.
     */
    public ChargingStatusFragment() {
    	super();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
        final Bundle savedInstanceState) {
        if(mContentView == null) {

            final ListItem[] items = new ListItem[] {
                new ListItem(R.drawable.ic_charging_sensor_w,
                		// cjd - by convention chargingoption should be charging_option
                        getString(R.string.chargingoption),null,
                        ListItem.typeTWO, USB_AC_CHARGING,
                        new View.OnClickListener() {
                            public void onClick(final View v) {
                                ((ChargingActivity)mHostActivity).launchChargingSourceFragment();
                            }
                        }
                ),
                new ListItem(R.drawable.ic_not_charging_sensor_w,
                		// cjd - by convention notchargingoption should be not_charging_option
                        getString(R.string.notchargingoption), null, ListItem.typeONE, NOT_CHARGING, null)
            };
            int selectedPos = -1;
            if(mSelectedItem != null) {
            	// cjd simpler coding would be
            	// selectedPos = ( NOT_CHARGING.equals(mSelectedItem) ? 1 : 0);
                if(mSelectedItem.equals(NOT_CHARGING)) {
                    selectedPos = 1;
                }else {
                    selectedPos = 0;
                }
            }
            final Picker picker = new Picker.Builder(getActivity())
            .setTitle(Html.fromHtml(getString(R.string.chargingstatus_prompt)))
            .setSingleChoiceItems(items, selectedPos, null)
            .setPositiveButton(getString(R.string.iam_done), this).create();
            mContentView = picker.getView();
            mListView = (ListView) mContentView.findViewById(R.id.list);
        }
        return mContentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mHostActivity.getActionBar().setTitle(getString(R.string.chargingstatus_title));
        //If there's a charging mode selected then the corresponding item should be checked
        String desc = "";
        if(mSelectedItem != null) {
            int selectedPos = -1;
            if(mSelectedItem.equals(NOT_CHARGING)) {
            	// cjd - perhaps a an internal class called something like "Selection" could store these values
                selectedPos = 1;
                desc = ((ChargingActivity)mHostActivity).getDescriptionForMode(USB_AC_CHARGING);
            } else {
                selectedPos = 0;
                desc = ((ChargingActivity)mHostActivity).getDescriptionForMode((String) mSelectedItem);
            }
            // cjd - this is kinda odd that this is done here rather than just putting the
            //   mListView.setItemChecked([1 or 0], true); statement inside the "if" statement above in each case.
            //   That is, not sure what the value is of creating and setting selectedPos above is only to use it here once. Seems
            //   like the code would be smaller, simpler just doing it above.
            if(selectedPos >= 0) {
            	// cjd - perhaps should null check mListView here before use.
                mListView.setItemChecked(selectedPos, true);
            }
        } else {
        	desc = ((ChargingActivity)mHostActivity).getDescriptionForMode(USB_AC_CHARGING);
        }
    	// cjd - perhaps should null check mListView here before use.
        ((ListItem)mListView.getItemAtPosition(0)).mDesc = desc;
        ((CustomListAdapter)mListView.getAdapter()).notifyDataSetChanged();
    }


    /**
     * Handles the done bottom button click event
     *
     * Required by DialogInterface.onClickListener interface
     */
    public void onClick(final DialogInterface dialog, final int which) {
        final ListView list = (ListView)getView().findViewById(R.id.list);
        if(list != null) {
        	// cjd - would it be clearer to use constants which wrap the values of 0 and 1 here - i.e. I don't know what 1 or 0 means?
            if(list.getCheckedItemPosition() == 0) {
                if(mSelectedItem == null || mSelectedItem.equals(NOT_CHARGING)) {
                    mSelectedItem = USB_AC_CHARGING;
                }
            }else if(list.getCheckedItemPosition() == 1){
                mSelectedItem = NOT_CHARGING;
            }
            mHostActivity.onReturn(mSelectedItem, this);
        }
    }
}
