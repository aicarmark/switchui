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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.motorola.mmsp.performancemaster.engine.Log;
import com.motorola.mmsp.performancemaster.R;

import java.util.ArrayList;

/**
 * Battery mode ListAdapter
 * 
 * @author BNTW34
 */
public class BatteryModeListAdapter extends ArrayAdapter<BatteryModeListItem> {

    private static final String LOG_TAG = "BatterySelect: ";
    private BatteryModeSelectActivity mContext;
    private ArrayList<BatteryModeListItem> mModeList;
    private BatteryModeListItem mCurrMode;
    private LayoutInflater mInflater;

    public BatteryModeListAdapter(BatteryModeSelectActivity context,
            ArrayList<BatteryModeListItem> values) {
        super(context, R.layout.bm_mode_list_item, values);
        mContext = context;
        mModeList = values;
        mInflater = (LayoutInflater) this.mContext
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.bm_mode_list_item, 
                    parent, false);
        }

        BatteryModeListItem data = mModeList.get(position);
        if (data != null) {
            convertView.setTag(data);
            int resId = 0;
            ImageView ivIcon = (ImageView) convertView.findViewById(R.id.bm_mode_list_icon);
            TextView tvLabel = (TextView) convertView.findViewById(R.id.bm_mode_list_text);
            RadioButton ivStatus = (RadioButton) convertView.findViewById(R.id.bm_mode_list_status);
            ImageView ivEdit = (ImageView) convertView.findViewById(R.id.bm_mode_list_edit);
            View divider = (View) convertView.findViewById(R.id.bm_mode_list_divider);
            // 1. icon
            switch (data.getModeType()) {
                case BatteryModeListItem.BATTERY_MODE_GENERAL:
                    resId = R.drawable.ic_bm_mode_general;
                    break;
                case BatteryModeListItem.BATTERY_MODE_NIGHT:
                    resId = R.drawable.ic_bm_mode_super;
                    break;
                case BatteryModeListItem.BATTERY_MODE_PERFORMANCE:
                    resId = R.drawable.ic_bm_mode_performance;
                    break;
                case BatteryModeListItem.BATTERY_MODE_SAVER:
                    resId = R.drawable.ic_bm_mode_saver;
                    break;
                case BatteryModeListItem.BATTERY_MODE_CUSTOMIZE:
                    resId = R.drawable.ic_bm_mode_custom;
                    break;
                case BatteryModeListItem.BATTERY_MODE_NONE:
                    resId = R.drawable.ic_bm_mode_add;
                    break;
                default:
                    break;
            }
            ivIcon.setImageResource(resId);

            // 2. text
            tvLabel.setText(data.getText());

            // 3. status & edit icon action
            if (resId != R.drawable.ic_bm_mode_add) {
                if (data.getId() == mCurrMode.getId()) {
                    // status icon
                	ivStatus.setChecked(true);
                    
                    // edit icon for customize mode only
                    if (data.getModeType() == BatteryModeListItem.BATTERY_MODE_CUSTOMIZE) {
                        // edit icon
                        ivEdit.setVisibility(View.VISIBLE);
                        divider.setVisibility(View.VISIBLE);
                        ivEdit.setTag(data);
                        // edit listener
                        ivEdit.setOnClickListener(new View.OnClickListener() {
                            
                            @Override
                            public void onClick(View v) {
                                Log.i(LOG_TAG, "edit btn clicked");
                                BatteryModeListItem data = (BatteryModeListItem) v.getTag();
                                if (data != null && data.getId() == mCurrMode.getId()) {
                                    mContext.onEditMode(data.getId());
                                }
                            }
                        });
                    } else {
                        ivEdit.setVisibility(View.INVISIBLE);
                        divider.setVisibility(View.INVISIBLE);
                    }                 
                } else {
                    // status icon
                	ivStatus.setChecked(false);
                    ivStatus.setTag(data);
                    // switch listener
                    ivStatus.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.i(LOG_TAG, "status btn clicked");
                            BatteryModeListItem data = (BatteryModeListItem) v.getTag();
                            if (data != null && data.getId() != mCurrMode.getId()) {
                                //mContext.onSwitchMode(data.getId());
                                setCurrMode(data);
                                mContext.onSetBarIcons(data.getId());
                            }
                        }
                    });
                    
                    // edit icon
                    ivEdit.setVisibility(View.INVISIBLE);
                    divider.setVisibility(View.INVISIBLE);
                }
            } else {
                // add icon: NOT USED!!!
                ivStatus.setVisibility(View.INVISIBLE);
            }
            
            /*
            // 4. remove icon action
            if (resId == R.drawable.ic_bm_mode_custom) {
                ivRemove.setImageResource(R.drawable.ic_bm_mode_remove);
                ivRemove.setTag(data);
                ivRemove.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Log.i(LOG_TAG, "remove btn clicked");
                        BatteryModeListItem d = (BatteryModeListItem) v.getTag();
                        mContext.onRemoveMode(d.getId());
                    }
                });
            } else {
                ivRemove.setVisibility(View.INVISIBLE);
            }
            */

            // 5. ListView item click
            if (convertView != null && data.getModeType() != BatteryModeListItem.BATTERY_MODE_NONE) {
                convertView.setTag(data);
                /*convertView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Log.i(LOG_TAG, "list item clicked: switch");
                        BatteryModeListItem data = (BatteryModeListItem) v.getTag();
                        if (data != null && data.getId() != mCurrMode.getId()) {
                            //mContext.onSwitchMode(data.getId());
                            setCurrMode(data);
                            mContext.onSetBarIcons(data.getId());
                        }
                    }
                });*/
            } else if (convertView != null && data.getModeType() == BatteryModeListItem.BATTERY_MODE_NONE) {
                // add icon action, NOT USED!!!
                /*
                v.setTag(data);
                v.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Log.i(LOG_TAG, "list item clicked: add");
                        mContext.onAddMode();
                    }
                });
                */
            }
        } // data != null

        return convertView;
    }

    public void setItemClicked(int pos) {
        BatteryModeListItem data = mModeList.get(pos);
        if (data != null && data.getId() != mCurrMode.getId()) {
            setCurrMode(data);
            mContext.onSetBarIcons(data.getId());
        }
    }

    public void removeMode(long id) {
        for (int i = 0; i < mModeList.size(); i++) {
            if (mModeList.get(i).getId() == id) {
                mModeList.remove(i);
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void addMode(BatteryModeListItem newItem) {
        mModeList.add(mModeList.size() - 1, newItem);
        notifyDataSetChanged();
    }

    public void setCurrMode(BatteryModeListItem newMode) {
        mCurrMode = newMode;
        notifyDataSetChanged();
    }
    
    public long getSelectedModeId() {
        return mCurrMode.getId();
    }

    public void changeMode(BatteryModeListItem item) {
        for (int i = 0; i < mModeList.size(); i++) {
            if (mModeList.get(i).getId() == item.getId()) {
                mModeList.set(i, item);
                notifyDataSetChanged();
            }
        }
    }
}
