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

import android.app.Activity;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.motorola.filemanager.BaseFileManagerFragment;
import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;
import com.motorola.filemanager.utils.FileUtils;

public class ActionModeHandler implements ActionMode.Callback {

    public interface ActionModeListener {
        public boolean onActionItemClicked(MenuItem item);
    }
    MainFileManagerActivity mActivity;
    ActionModeListener mListener;
    AbsListView mFileListView;
    View mCustomView;
    CustomMenu mCustomMenu;

    public ActionModeHandler(MainFileManagerActivity activity, AbsListView fileListView,
                             boolean isContextMenu) {
        mActivity = activity;
        mFileListView = fileListView;
    }

    public ActionModeHandler(MainFileManagerActivity activity) {
        mActivity = activity;
        mFileListView = null;
    }

    public ActionMode startActionMode() {
        Activity a = mActivity;

        mCustomMenu = new CustomMenu(a, null);
        View customView = LayoutInflater.from(a).inflate(R.layout.action_mode, null);
        mCustomView = customView;
        mCustomMenu.addDropDownMenu((Button) customView.findViewById(R.id.selection_menu),
                R.menu.selection);

        final ActionMode actionMode = a.startActionMode(this);
        actionMode.setCustomView(customView);
        mCustomMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            //@Override
            public boolean onMenuItemClick(MenuItem item) {
                return onActionItemClicked(actionMode, item);
            }
        });
        return actionMode;
    }

    public void setActionModeListener(ActionModeListener listener) {
        mListener = listener;
    }

    //@Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        boolean result = false;
        if (mListener != null) {
            result = mListener.onActionItemClicked(item);
        }
        return result;
    }

    //@Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.multi_select_menu, menu);
        return true;
    }

    //@Override
    public void onDestroyActionMode(ActionMode mode) {
        BaseFileManagerFragment rightFragment =
                (BaseFileManagerFragment) mActivity.getFragmentManager().findFragmentById(
                        R.id.details);

        rightFragment.exitMultiSelect();

    }

    //@Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

        // Default menus.
        menu.findItem(R.id.Copy).setVisible(true);
        menu.findItem(R.id.Copy).setEnabled(true);
        menu.findItem(R.id.Move).setVisible(true);
        menu.findItem(R.id.Move).setEnabled(true);
        menu.findItem(R.id.Delete).setVisible(true);
        menu.findItem(R.id.Delete).setEnabled(true);

        ArrayList<String> selectedFiles = null;
        if (mFileListView != null && mFileListView.getAdapter() != null) {

            selectedFiles =
                    ((IconifiedTextListAdapter) mFileListView.getAdapter()).getSelectFiles();
            if (mCustomView != null) {
                Button selected = (Button) mCustomView.findViewById(R.id.selection_menu);
                if (selected != null) {
                    selected.setText(mActivity.getResources().getString(R.string.selected,
                            selectedFiles.size()));
                }
            }
            if (selectedFiles.size() == 0) {
                // Disable default menus (Enter action mode from action bar menu)
                menu.findItem(R.id.Copy).setEnabled(false);
                menu.findItem(R.id.Move).setEnabled(false);
                menu.findItem(R.id.Delete).setEnabled(false);

                menu.findItem(R.id.Zip).setVisible(false);
                menu.findItem(R.id.Rename).setVisible(false);
                menu.findItem(R.id.Info).setVisible(false);
                menu.findItem(R.id.Print).setVisible(false);

            } else if (selectedFiles.size() == 1) {
                String[] inputFileNames = null;
                selectedFiles =
                        ((IconifiedTextListAdapter) mFileListView.getAdapter()).getSelectFiles();
                inputFileNames = new String[selectedFiles.size()];
                selectedFiles.toArray(inputFileNames);
                if (isZipFile(inputFileNames[0])) {
                    // Do not allow user to re-zip an existing zip
                    menu.findItem(R.id.Zip).setVisible(false);
                } else {
                    menu.findItem(R.id.Zip).setVisible(true);
                }

                String filePath = selectedFiles.get(0);
                if (filePath != null) {
                    if (!FileUtils.getFile(filePath).isDirectory() ||
                            (FileUtils.isPrintIntentHandled(mActivity.getApplicationContext(),
                                    FileUtils.getFile(filePath)))) {
                        menu.findItem(R.id.Print).setVisible(true);
                    } else {
                        menu.findItem(R.id.Print).setVisible(false);

                    }
                }
                menu.findItem(R.id.Rename).setVisible(true);
                menu.findItem(R.id.Info).setVisible(false);
            } else {
                menu.findItem(R.id.Zip).setVisible(true);
                menu.findItem(R.id.Rename).setVisible(false);
                menu.findItem(R.id.Info).setVisible(false);
                menu.findItem(R.id.Print).setVisible(false);
            }
            return true;
        } else {
            return false;
        }

    }

    private boolean isZipFile(String fileName) {
        if (fileName == null) {
            return false;
        } else {
            return fileName.endsWith(".zip");
        }
    }

}
