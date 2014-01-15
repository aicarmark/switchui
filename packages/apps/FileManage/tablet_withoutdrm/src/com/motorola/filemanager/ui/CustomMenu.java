/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR              Author      Description
 * 2011-05-23   IKTABLETMAIN-348    W17952      initial
 */
package com.motorola.filemanager.ui;

import java.util.ArrayList;

import android.app.ActionBar;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.motorola.filemanager.R;

public class CustomMenu implements OnMenuItemClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = "FilterMenu";

    public static class DropDownMenu {
        private Button mButton;
        private PopupMenu mPopupMenu;
        private Menu mMenu;

        public DropDownMenu(Context context, Button button, int menuId,
                            OnMenuItemClickListener listener) {
            mButton = button;
            mButton.setBackgroundDrawable(context.getResources().getDrawable(
                    R.drawable.dropdown_normal_holo_dark));
            mPopupMenu = new PopupMenu(context, mButton);
            mMenu = mPopupMenu.getMenu();
            mPopupMenu.getMenuInflater().inflate(menuId, mMenu);
            mPopupMenu.setOnMenuItemClickListener(listener);
            mButton.setOnClickListener(new OnClickListener() {
                //@Override
                public void onClick(View v) {
                    mPopupMenu.show();
                }
            });
        }

        public MenuItem findItem(int id) {
            return mMenu.findItem(id);
        }

        public void setTitle(CharSequence title) {
            mButton.setText(title);
        }
    }

    private ActionBar mActionBar;
    private Context mContext;
    private ArrayList<DropDownMenu> mMenus;
    private OnMenuItemClickListener mListener;

    public CustomMenu(Context context, ActionBar actionBar) {
        mContext = context;
        mActionBar = actionBar;
        mMenus = new ArrayList<DropDownMenu>();
    }

    public DropDownMenu addDropDownMenu(Button button, int menuId) {
        button.setVisibility(View.VISIBLE);
        DropDownMenu menu = new DropDownMenu(mContext, button, menuId, this);
        mMenus.add(menu);
        return menu;
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        mListener = listener;
    }

    public void show() {
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
    }

    public void hide() {
        mActionBar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
    }

    public MenuItem findMenuItem(int id) {
        MenuItem item = null;
        for (DropDownMenu menu : mMenus) {
            item = menu.findItem(id);
            if (item != null) {
                return item;
            }
        }
        return item;
    }

    public void setMenuItemAppliedEnabled(int id, boolean applied, boolean enabled,
                                          boolean updateTitle) {
        MenuItem item = null;
        for (DropDownMenu menu : mMenus) {
            item = menu.findItem(id);
            if (item != null) {
                item.setCheckable(true);
                item.setChecked(applied);
                item.setEnabled(enabled);
                if (updateTitle) {
                    menu.setTitle(item.getTitle());
                }
            }
        }
    }

    // For non checkable menu items
    public void setMenuItemEnabled(int id, boolean applied, boolean enabled, boolean updateTitle) {
        MenuItem item = null;
        for (DropDownMenu menu : mMenus) {
            item = menu.findItem(id);
            if (item != null) {
                item.setEnabled(enabled);
                if (updateTitle) {
                    menu.setTitle(item.getTitle());
                }
            }
        }
    }

    public void setMenuItemVisibility(int id, boolean visibility) {
        MenuItem item = findMenuItem(id);
        if (item != null) {
            item.setVisible(visibility);
        }
    }

    //@Override
    public boolean onMenuItemClick(MenuItem item) {
        if (mListener != null) {
            return mListener.onMenuItemClick(item);
        }
        return false;
    }
}
