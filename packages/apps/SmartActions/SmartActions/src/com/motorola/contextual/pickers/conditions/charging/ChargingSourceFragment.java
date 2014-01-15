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

import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * This fragment presents a charging source chooser.
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment - Fragment base class
 *
 * RESPONSIBILITIES:
 *  Presents various charging sources options to choose from
 *  and sends the chosen option back to the host activity.
 *
 * COLLABORATORS:
 *  N/A
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class ChargingSourceFragment extends PickerFragment implements OnClickListener,
        ChargingConstants{

    /**
     * All subclasses of Fragment must include a public empty constructor. The framework
     * will often re-instantiate a fragment class when needed, in particular during state
     * restore, and needs to be able to find this constructor to instantiate it. If the
     * empty constructor is not available, a runtime exception will occur in some cases
     * during state restore.
     */
    public ChargingSourceFragment() {
    	super();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
        final Bundle savedInstanceState) {

        if(mContentView == null) {
            final ListItem[] items = new ListItem[] {
                new ListItem(R.drawable.ic_charging_sensor_w,
                        getString(R.string.anysourcecharging), null, ListItem.typeONE, USB_AC_CHARGING, null),
                new ListItem(R.drawable.ic_wall_charging_sensor_w,
                                getString(R.string.accharging), null, ListItem.typeONE, AC_CHARGING, null),
                new ListItem(R.drawable.ic_usb_charging_sensor_w,
                        getString(R.string.usbcharging), getString(R.string.usbcharging_desc),
                        		ListItem.typeONE, USB_CHARGING, null)
            };

            // If there's a charging mode selected then the corresponding item should be checked
            if (mSelectedItem == null) {
            	mSelectedItem = USB_AC_CHARGING;
            }

            int selectedPos = -1;
            if(mSelectedItem != null) {
                for(int i=0; i<items.length ; i++) {
                    if(((String)items[i].mMode).equals(mSelectedItem)) {
                        selectedPos = i;
                        break;
                    }
                }
            }
            final Picker picker = new Picker.Builder(getActivity())
            .setTitle(Html.fromHtml(getString(R.string.chargingstatus_prompt)))
            .setSingleChoiceItems(items, selectedPos, null)
            .setPositiveButton(getString(R.string.continue_prompt), this).create();
            mContentView = picker.getView();
        }
        return mContentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mHostActivity.getActionBar().setTitle(getString(R.string.chargingsource_title));
    }

    /**
     * Handles the done bottom button click event
     *
     * Required by DialogInterface.onClickListener interface
     */
    public void onClick(final DialogInterface dialog, final int which) {
        final ListView list = (ListView)mContentView.findViewById(R.id.list);
        if (list != null) {
            final int pos = list.getCheckedItemPosition();
            if(pos >= 0 && pos < list.getAdapter().getCount()) {
                final ListItem item = (ListItem)list.getAdapter().getItem(pos);
                mHostActivity.onReturn(item.mMode, this);
            }else {
                mHostActivity.onReturn(null, this);
            }
        }
    }
}
