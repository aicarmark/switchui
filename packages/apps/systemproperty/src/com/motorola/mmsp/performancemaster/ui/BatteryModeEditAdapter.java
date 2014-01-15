/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *                             Modification     Tracking
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * bntw34                      02/05/2012                   Initial release
 */

package com.motorola.mmsp.performancemaster.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import com.motorola.mmsp.performancemaster.R;

import java.util.ArrayList;

public class BatteryModeEditAdapter extends ArrayAdapter<BatteryModeEditItem> {
    private BatteryModeEditActivity mContext;
    private ArrayList<BatteryModeEditItem> mEditList;

    public BatteryModeEditAdapter(BatteryModeEditActivity context,
            ArrayList<BatteryModeEditItem> values) {
        super(context, R.layout.bm_grid_item, values);
        mContext = context;
        mEditList = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = null;

        BatteryModeEditItem data = mEditList.get(position);

        v = (View) new BatteryModeEditItemView(mContext, data);

		switch (position) {
		case 0:
		case 3:
			v.setBackgroundResource(R.drawable.line_left_down);
			break;
		case 1:
		case 4:
			v.setBackgroundResource(R.drawable.line_center_down);
			break;
		case 2:
		case 5:
			v.setBackgroundResource(R.drawable.line_right_down);
			break;
		case 6:
		case 7:
			v.setBackgroundResource(R.drawable.line_right_right);
			break;
		case 8:
			v.setBackgroundResource(R.drawable.line_last);
			break;
		default:
			break;
		}
		
		return v;
	}

    /**
     * called by BatteryEditActivity
     * 
     * @return the edit mode values
     */
    public ArrayList<BatteryModeEditItem> getValues() {
        return mEditList;
    }
}
