/*
 * Copyright (c) 2011 Motorola, Inc. All Rights Reserved The contents of this
 * file are Motorola Confidential Restricted (MCR). Revision history (newest
 * first): Date CR Author Description 2011-09-13 IKTABLETMAIN-4834 w17952
 * initial
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

import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;
import com.motorola.filemanager.samba.RemoteContentFragment;
import com.motorola.filemanager.utils.FileUtils;

public class SambaActionModeHandler implements ActionMode.Callback {

    public interface ActionModeListener {
        public boolean onActionItemClicked(MenuItem item);
    }

    private final MainFileManagerActivity mActivity;
    private ActionModeListener mListener;
    private AbsListView mFileListView;
    private View mCustomView;
    private CustomMenu mCustomMenu;

    public SambaActionModeHandler(MainFileManagerActivity activity, AbsListView fileListView) {
        mActivity = activity;
        mFileListView = fileListView;

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
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onActionItemClicked(actionMode, item);
            }
        });
        return actionMode;
    }

    public void setActionModeListener(ActionModeListener listener) {
        mListener = listener;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        boolean result = false;
        if (mListener != null) {
            result = mListener.onActionItemClicked(item);
        }
        return result;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.multi_select_menu, menu);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        RemoteContentFragment det = null;
        if (mActivity == null) {
            return;
        }
        try {
            det =
                    (RemoteContentFragment) mActivity.getFragmentManager().findFragmentById(
                            R.id.details);
            det.getSmbExplorer().outSmbExploreMultiSelectMode();
        } catch (Exception e) {
        }

    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu.findItem(R.id.Zip).setVisible(false);

        // Default menus.
        menu.findItem(R.id.Copy).setVisible(true);
        menu.findItem(R.id.Copy).setEnabled(true);
        menu.findItem(R.id.Move).setVisible(true);
        menu.findItem(R.id.Move).setEnabled(true);
        menu.findItem(R.id.Delete).setVisible(true);
        menu.findItem(R.id.Delete).setEnabled(true);
        ArrayList<String> selectedFiles = null;

        if (mFileListView.getAdapter() != null) {

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

                menu.findItem(R.id.Rename).setVisible(true);
                menu.findItem(R.id.Info).setVisible(false);
                String filePath = selectedFiles.get(0);
                if (filePath != null) {
                    if (FileUtils.getFile(filePath).isDirectory() ||
                            (!FileUtils.isPrintIntentHandled(mActivity.getApplicationContext(),
                                    FileUtils.getFile(filePath)))) {
                        menu.findItem(R.id.Print).setVisible(false);
                    } else {
                        menu.findItem(R.id.Print).setVisible(true);
                    }
                }

            } else {
                menu.findItem(R.id.Zip).setVisible(true);
                menu.findItem(R.id.Rename).setVisible(false);
                menu.findItem(R.id.Info).setVisible(false);
                menu.findItem(R.id.Print).setVisible(false);
            }
        }
        return true;
    }

}
