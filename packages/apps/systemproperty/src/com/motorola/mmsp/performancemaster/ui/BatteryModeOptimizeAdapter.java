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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.motorola.mmsp.performancemaster.R;

import java.util.ArrayList;

public class BatteryModeOptimizeAdapter extends ArrayAdapter<BatteryModeOptimizeItem> {
    private BatteryModeActivity mContext;
    private ArrayList<BatteryModeOptimizeItem> mOptimizeList;
    private LayoutInflater mInflater;

    public BatteryModeOptimizeAdapter(BatteryModeActivity context,
            ArrayList<BatteryModeOptimizeItem> values) {
        super(context, R.layout.bm_grid_item, values);

        mContext = context;
        mOptimizeList = values;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getItemViewType(int position) {
        // type of view for given position
        // used to provide correct convertView
        return super.getItemViewType(position);
    }

    @Override
    public int getViewTypeCount() {
        // how many types to expect
        return super.getViewTypeCount();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            //convertView = mInflater.inflate(R.layout.bm_grid_item, parent, false);
            convertView = mInflater.inflate(R.layout.bm_grid_item, null);
                        
            switch (position) {
    		case 0:
    		case 3:
               convertView.setBackgroundDrawable((Drawable)mContext.getResources().getDrawable(R.drawable.line_left_down));
    			break;
    		case 1:
    		case 4:
    			convertView.setBackgroundDrawable( (Drawable)mContext.getResources().getDrawable(R.drawable.line_center_down));
    			break;
    		case 2:
    		case 5:
    			convertView.setBackgroundDrawable( (Drawable)mContext.getResources().getDrawable(R.drawable.line_right_down));
    			break;
    		case 6:
    		case 7:
    			convertView.setBackgroundDrawable((Drawable)mContext.getResources().getDrawable(R.drawable.line_right_right));
            	        break;
    		case 8:
    			convertView.setBackgroundDrawable( (Drawable)mContext.getResources().getDrawable(R.drawable.line_last));
    			   break;
    		default:
    			break;
    		}
        }

        BatteryModeOptimizeItem itemData = mOptimizeList.get(position);
        if (itemData != null) {
            ((ImageView) convertView.findViewById(R.id.grid_item_icon))
                    .setImageResource(itemData.getIconId());
            ((TextView) convertView.findViewById(R.id.grid_item_name)).setText(itemData
                    .getTextId());
            
            /*TextView tvStatus = (TextView) convertView.findViewById(R.id.grid_item_status);
            tvStatus.setVisibility(View.INVISIBLE);
           
            if (tag == BatteryModeOptimizeItem.ITEM_TAG_BRIGHTNESS
                    || tag == BatteryModeOptimizeItem.ITEM_TAG_TIMEOUT) {
                tvStatus.setText(itemData.getValue());
                
                if (itemData.getHighlight()) {
                    tvStatus.setTextColor(Color.rgb(51, 181, 229));
                } else {
                    tvStatus.setTextColor(Color.rgb(255, 255, 255));
                }
            } else {
                tvStatus.setVisibility(View.INVISIBLE);
            }
            */
        }
        
        /*
        if (convertView != null) {
            convertView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mContext.onListItemClicked();
                }
            });
        }
        */

        return convertView;
    }

    public void setListData(ArrayList<BatteryModeOptimizeItem> values) {
        mOptimizeList = values;
        notifyDataSetChanged();
    }
}
