/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR              Author      Description
 * 2011-05-23   IKTABLETMAIN-348    XQH748      initial
 */
package com.motorola.filemanager.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.motorola.filemanager.R;

public class FileBrowserContextMenu extends PopupMenu implements AdapterView.OnItemClickListener {
    private Activity mContext;
    private FileBrowserContextMenuAdapter mContextMenuAdapter;
    private ContextMenuItemClickListener mListener;
    private String mSelectedFile;

    public FileBrowserContextMenu(Activity context, String file, View view) {
        super(context, view);
        mContext = context;
        mContext.getLayoutInflater().inflate(R.layout.context_menu, null);
        mContextMenuAdapter = new FileBrowserContextMenuAdapter();
        mSelectedFile = file;
    }

    public void addMenuItem(String itemName, int id) {
        if (mContextMenuAdapter != null) {
            mContextMenuAdapter.addItem(new FileBrowserContextMenuItem(itemName, id));
        }
    }

    @Override
    public void show() {
        // setupView();
        super.show();
    }

    /* @Override
     public void showAsDropDown(View anchor) {
       setupView();
       super.showAsDropDown(anchor);
     }

     @Override
     public void showAsDropDown(View anchor, int xoff, int yoff) {
       setupView();
       super.showAsDropDown(anchor, xoff, yoff);
     }
     
     @Override
     public void showAtLocation(View view, int gravity, int xoff, int yoff){
       setupView();
       super.showAtLocation(view, gravity, xoff, yoff);
     }

     private void setupView() {
       float width = mContext.getResources().getDimension(R.dimen.context_menu_size);
       setWindowLayoutMode((int) width, LayoutParams.WRAP_CONTENT);
       setFocusable(true);
      setWidth((int) width);
       mContextMenuListView.setAdapter(mContextMenuAdapter);
       mContextMenuListView.setOnItemClickListener(this);
       setContentView(mContentView);
     }*/

    public void setOnItemClickListener(ContextMenuItemClickListener listener) {
        mListener = listener;
    }

    private static class FileBrowserContextMenuItem {
        private String mItemName;
        private int mItemId;

        public FileBrowserContextMenuItem(String itemName, int id) {
            mItemName = itemName;
            mItemId = id;
        }

        public String getItemName() {
            return mItemName;
        }

        public int getItemId() {
            return mItemId;
        }
    }

    private class FileBrowserContextMenuAdapter extends BaseAdapter {
        private ArrayList<FileBrowserContextMenuItem> menuItems =
                new ArrayList<FileBrowserContextMenuItem>();

        //@Override
        public int getCount() {
            return menuItems.size();
        }

        public void addItem(FileBrowserContextMenuItem item) {
            menuItems.add(item);
        }

        //@Override
        public Object getItem(int pos) {
            return menuItems.get(pos);
        }

        //@Override
        public long getItemId(int pos) {

            FileBrowserContextMenuItem item;

            if ((item = menuItems.get(pos)) != null) {
                return item.getItemId();
            } else {
                return -1;
            }
        }

        //@Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            LayoutInflater inflater = mContext.getLayoutInflater();
            if (inflater != null) {
                if (convertView == null) {
                    convertView = inflater.inflate(R.layout.context_menu_item, null);
                    textView = (TextView) convertView.findViewById(R.id.context_menu_item);
                    convertView.setTag(textView);
                } else {
                    textView = (TextView) convertView.getTag();
                }

                textView.setText(menuItems.get(position).getItemName());
                return convertView;
            } else {
                return null;
            }
        }
    }

    //@Override
    public void onItemClick(AdapterView<?> arg0, View view, int pos, long id) {

        dismiss();
        if (mListener != null) {
            mListener.onContextMenuItemClick((int) id, mSelectedFile);
        }
    }

    public interface ContextMenuItemClickListener {
        public void onContextMenuItemClick(int menuId, String selectedFile);
    }

}
