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

package com.motorola.filemanager.local;

import java.io.File;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Message;
import android.view.ActionMode;
import android.view.View;
import android.widget.AbsListView;
import android.widget.TextView;

import com.motorola.filemanager.BaseFileManagerFragment;
import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.HomePageLeftFileManagerFragment;
import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;
import com.motorola.filemanager.ui.ColumnViewFrameAdapter;
import com.motorola.filemanager.ui.IconifiedText;

public class LocalColumnViewFragment extends LocalFileOperationsFragment {
    private static String TAG = "LocalLeftFileManagerFragment: ";
    private File mClickedfile = null;
    private File mCurrDir = null;
    private String mHighlightedFile = null;
    private TextView mEmptyText;
    private boolean mIsTop = false;

    public LocalColumnViewFragment(int fragmentLayoutID) {
        super(fragmentLayoutID);
        // TODO Auto-generated constructor stub
    }

    public LocalColumnViewFragment(File clickedFile) {
        super(R.layout.column_view_column);
        mClickedfile = clickedFile;
        // TODO Auto-generated constructor stub
    }

    public LocalColumnViewFragment(File clickedFile, String highlightedFile, boolean isTop) {
        super(R.layout.column_view_column);
        mClickedfile = clickedFile;
        mHighlightedFile = highlightedFile;
        mIsTop = isTop;
        // TODO Auto-generated constructor stub
    }

    @Override
    public File getClickedFile() {
        return mClickedfile;
    }

    public void setIsTop(boolean isTop) {
        mIsTop = isTop;
    }

    @Override
    public File getDropPath() {
        File temp = null;
        if (mDropTargetPath != null) {
            temp = new File(mDropTargetPath);
        }
        if (temp != null) {
            if (temp.isDirectory()) {
                return temp;
            } else {
                return temp.getParentFile();
            }
        }
        return mClickedfile;
    }

    @Override
    protected void initFileListView() {
        mFileListView = (AbsListView) mContentView.findViewById(R.id.column_listview);
        mFileListView.setEmptyView(mFileListView.findViewById(android.R.id.empty));
        mEmptyText = (TextView) mContentView.findViewById(R.id.empty_text);

        mFileListView.setVisibility(View.VISIBLE);
        mFileListView.setOnDragListener(this);
        mContext = this.getActivity();

        if (mClickedfile != null) {
            browseTo(mClickedfile);
            mCurrDir = mClickedfile;
            FileManagerApp.setCurrDir(mClickedfile);
        } else {
            mCurrDir = FileManagerApp.getCurrDir();
            mClickedfile = mCurrDir;
            browseTo(mCurrDir);
        }

    }

    @Override
    public void startMultiSelect(boolean isContextMenu) {
        this.clearHighlight();
        super.startMultiSelect(isContextMenu);
        if (mCurrentActionMode != null) {
            Fragment rightFragment =
                    getActivity().getFragmentManager().findFragmentById(R.id.details);
            FragmentTransaction ft = purgeLowerLayerColumns();
            ft.commit();
            FileManagerApp.setCurrDir(mCurrDir);
            if ((rightFragment != null)) {
                ColumnViewFrameAdapter colAdpter =
                        ((LocalColumnViewFrameLeftFragment) rightFragment).getAdapter();
                if (colAdpter != null && colAdpter.getCount() != 0) {
                    int pos = colAdpter.getItemPos(this);
                    colAdpter.setMultiselectPos(pos);
                    colAdpter.setselectPos(pos);
                }
            }
        }
    }

    @Override
    protected void showContextMenu(Object view, final int pos) {
        Fragment rightFragment = getActivity().getFragmentManager().findFragmentById(R.id.details);
        if ((rightFragment != null)) {
            ColumnViewFrameAdapter colAdpter =
                    ((LocalColumnViewFrameLeftFragment) rightFragment).getAdapter();
            if (colAdpter != null && colAdpter.getCount() != 0) {
                int position = colAdpter.getItemPos(this);
                colAdpter.setselectPos(position);
            }
        }
        super.showContextMenu(view, pos);
    }

    public void initHomePage(boolean isBack) {
        HomePageLeftFileManagerFragment df =
                new HomePageLeftFileManagerFragment(R.layout.column_view_column, isBack);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.home_page, df);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    public void updatePanel(File clickedFile) {
        mClickedfile = clickedFile;
        browseTo(clickedFile);
    }

    @Override
    protected void refreshList() {
        if (mClickedfile != null) {
            super.refreshList(mClickedfile);
        } else {
            super.refreshList();
        }
    }

    @Override
    public void refreshList(final File aDirectory) {
        if (mClickedfile != null) {
            super.refreshList(mClickedfile);
        } else {
            super.refreshList(aDirectory);
        }
    }

    @Override
    protected void handleMessage(Message message) {
        switch (message.what) {
            case MESSAGE_SHOW_DIRECTORY_CONTENTS :
                if (message.obj != null) {
                    FileManagerApp.log(TAG + "obj not null", false);
                    showDirectoryContents((DirectoryContents) message.obj);
                }
                this.getAdapter().setIsTopColumnMode(mIsTop);
                int position = mFileListAdapter.getPosition(mCurrHighlightedItem);
                // In long lists, to move the selected item to top in left fragement
                mFileListView.setSelection(position);
                break;
            case MESSAGE_ICON_CHANGED :
                super.handleMessage(message);
                break;
            default :
                super.handleMessage(message);
                break;
        }
    }

    private FragmentTransaction purgeLowerLayerColumns() {
        try {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment rightFragment = getFragmentManager().findFragmentById(R.id.details);
            ColumnViewFrameAdapter coladapter =
                    ((LocalColumnViewFrameLeftFragment) rightFragment).getAdapter();
            int pos = coladapter.getItemPos(this);
            int count = coladapter.getCount();
            for (int i = count - 1; i >= pos + 1; i--) {
                Fragment frag = coladapter.getItem(i);
                ft.remove(frag);
                ((LocalColumnViewFrameLeftFragment) rightFragment).removeFromList(i);
            }
            return ft;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void refreshLeft(boolean isFullRefresh) {
        // TODO Auto-generated method stub        
        if (!isFullRefresh) {
            //If we need to update only the items set in the directory
            mFileListAdapter.setNoItemsInfo(new File(mCurrHighlightedItem.getPathInfo()));
        } else {
            //For move case, its better to do full refresh since we need to update both src 
            //and dest folders with updated no. of items and some items needs to be cleared inleft
            updateLeftFragment(FileManagerApp.getCurrDir().getParentFile());
        }

    }

    @Override
    public void openClickedFile(String fileAbsolutePath, IconifiedText text) {
        try {
            File clickedFile = new File(fileAbsolutePath);
            BaseFileManagerFragment rightfrag =
                    (BaseFileManagerFragment) getFragmentManager().findFragmentById(R.id.details);
            ActionMode actMode = rightfrag.getActionMode();
            mFileListAdapter.clearCurrentHighlightedItem(mCurrHighlightedItem);
            if (!((MainFileManagerActivity) getActivity()).getIsDragMode() && actMode != null) {
                actMode.finish();
            }

            if (mHighlightedFile != null) {
                mFileListAdapter.clearHighlightedItem();
                mHighlightedFile = fileAbsolutePath;
                mFileListAdapter.updateHighlightedItem(mHighlightedFile, false);
            } else {
                mFileListAdapter.clearHighlightedItem();
                mFileListAdapter.setHighlightedItem(text, false);
            }
            mCurrHighlightedItem = text;
            getActivity().invalidateOptionsMenu();
            if (clickedFile != null) {
                try {
                    Fragment rightFragment = getFragmentManager().findFragmentById(R.id.details);
                    FragmentTransaction ft = purgeLowerLayerColumns();
                    mIsTop = false;
                    this.getAdapter().setIsTopColumnMode(false);
                    if (FileManagerApp.getBrowseMode() == FileManagerApp.PAGE_CONTENT_SEARCH) {
                        File searchModeTopDirectory =
                                ((LocalColumnViewFrameLeftFragment) rightFragment)
                                        .getSearchModeTopDirectory();
                        String searchModeTopDirectoryPath =
                                (searchModeTopDirectory.getParentFile().getAbsolutePath())
                                        .concat("/");
                        if (!clickedFile.getAbsolutePath().contains(searchModeTopDirectoryPath)) {
                            FileManagerApp.setBrowseMode(FileManagerApp.PAGE_CONTENT_NORMAL);
                            ((MainFileManagerActivity) getActivity()).clearSearchBackStack();
                        }
                    }
                    if (clickedFile.isDirectory()) {
                        //Disable Context Menu icon in previous column
                        LocalColumnViewFragment columnFrag =
                                new LocalColumnViewFragment(clickedFile, null, true);
                        ft.add(R.id.column_view_frame, columnFrag);
                        ft.commit();
                        ((LocalColumnViewFrameLeftFragment) rightFragment).addToList(columnFrag);
                    } else {
                        showDetailInfoFragment(text, ft);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void openClickedFile(IconifiedText text) {
        String fileFullPath = text.getPathInfo();
        openClickedFile(fileFullPath, text);
    }

    public void clearHighlight() {
        mFileListAdapter.clearCurrentHighlightedItem(mCurrHighlightedItem);
        mCurrHighlightedItem = null;
        mHighlightedFile = null;
    }

    public void showDetailInfoFragment(IconifiedText item, FragmentTransaction ft) {
        FileManagerApp.log(TAG + "showDetailInfoFragment()", false);
        LocalDetailedInfoFileManagerFragment detailInfoFragment =
                new LocalDetailedInfoFileManagerFragment(R.layout.detail_view_column);
        detailInfoFragment.setItem(item);
        Fragment rightFragment = getFragmentManager().findFragmentById(R.id.details);
        ft.add(R.id.column_view_frame, detailInfoFragment);
        ft.commit();
        ((LocalColumnViewFrameLeftFragment) rightFragment).addToList(detailInfoFragment);
    }

    @Override
    public void showDetailInfoFragment(IconifiedText item) {
        try {
            FragmentTransaction ft = purgeLowerLayerColumns();
            showDetailInfoFragment(item, ft);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void showDetailInfoFragmentFromContextMenu(IconifiedText item) {
        if (mCurrHighlightedItem != null) {
            mFileListAdapter.clearCurrentHighlightedItem(mCurrHighlightedItem);
        }
        mCurrHighlightedItem = mFileListAdapter.updateHighlightedItem(item.getPathInfo(), true);
        showDetailInfoFragment(item);
    }

    @Override
    protected void showDirectoryContents(DirectoryContents contents) {
        super.showDirectoryContents(contents);
        mFileListView.setAdapter(mFileListAdapter);
        if (mHighlightedFile != null) {
            String text = mHighlightedFile;
            mFileListAdapter.clearHighlightedItem();
            /*Better to do this way since we have / not have "/" in the end*/
            File f1 = new File(mHighlightedFile);
            File f2 = new File(FileManagerApp.getCurrDir().getPath());
            if (mIsTop && !f1.equals(f2)) {
                if (mCurrHighlightedItem != null) {
                    mFileListAdapter.clearCurrentHighlightedItem(mCurrHighlightedItem);
                }
                mCurrHighlightedItem = mFileListAdapter.updateHighlightedItem(text, true);
                showDetailInfoFragment(mCurrHighlightedItem);
            } else {
                updateHighlightedItem(FileManagerApp.getCurrDir().getPath());
                mFileListAdapter.updateHighlightedItem(text, false);
            }
        } else {
            mFileListAdapter.clearHighlightedItem();
            mFileListAdapter.setHighlightedItem(mCurrHighlightedItem, false);
        }

        mFileListView.setOnItemClickListener(fileListListener);
        mFileListView.setOnItemLongClickListener(fileListListener2);
        if (!mListDir.isEmpty() || !mListFile.isEmpty()) {
            mEmptyText.setVisibility(View.GONE);
            mFileListView.setVisibility(View.VISIBLE);
        } else {
            mEmptyText.setVisibility(View.VISIBLE);
            mFileListView.setVisibility(View.GONE);
        }

    }

}
