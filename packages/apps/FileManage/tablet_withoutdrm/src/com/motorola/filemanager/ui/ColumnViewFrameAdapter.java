/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR              Author      Description
 * 2011-11-04       XQH748      initial
 */

package com.motorola.filemanager.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.FragmentManager;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;

import com.motorola.filemanager.local.LocalFileOperationsFragment;

public class ColumnViewFrameAdapter extends BaseAdapter implements OnClickListener {
    //Context mContext;
    //Handler mHandler;
    //LayoutInflater mInflater;
    //FragmentManager mFragmentMgr;
    int mMultiSelPos = -1;
    int mSelPos = -1;
    private List<LocalFileOperationsFragment> mItems = new ArrayList<LocalFileOperationsFragment>();

    public ColumnViewFrameAdapter(Context applicationContext, Handler handler,
                                  LayoutInflater inflater, FragmentManager mfr) {
        // TODO Auto-generated constructor stub
        /*mContext = applicationContext;
        mHandler = handler;
        mInflater = inflater;
        mFragmentMgr = mfr;*/
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public int getItemPos(LocalFileOperationsFragment mDragCol) {

        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).equals(mDragCol)) {
                return i;
            }
        }
        return -1;

    }

    public void setMultiselectPos(int pos) {
        mMultiSelPos = pos;
    }

    public int getMultiselectPos() {
        return mMultiSelPos;
    }

    public void setselectPos(int pos) {
        mSelPos = pos;
    }

    public int getselectPos() {
        return mSelPos;
    }

    public void addItem(LocalFileOperationsFragment it) {
        mItems.add(it);
    }

    public void setListItems(List<LocalFileOperationsFragment> lit) {
        mItems = lit;
    }

    public LocalFileOperationsFragment getItem(int position) {
        if (position > getCount()) {
            return null;
        } else {
            return mItems.get(position);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LocalFileOperationsFragment fragment = mItems.get(position);
        View childView = fragment.getView();
        return childView;
    }

    @Override
    public void onClick(View arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return mItems.size();
    }

}
