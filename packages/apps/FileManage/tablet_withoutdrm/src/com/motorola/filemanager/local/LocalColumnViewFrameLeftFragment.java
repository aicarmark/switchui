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
import java.util.ArrayList;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Message;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.motorola.filemanager.BaseFileManagerFragment;
import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.HomePageLeftFileManagerFragment;
import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;
import com.motorola.filemanager.ui.ColumnView;
import com.motorola.filemanager.ui.ColumnViewFrameAdapter;
import com.motorola.filemanager.ui.IconifiedText;
import com.motorola.filemanager.utils.FileUtils;

public class LocalColumnViewFrameLeftFragment extends BaseFileManagerFragment {
    private static String TAG = "LocalLeftFileManagerFragment: ";
    protected ColumnView mFileColListView;
    protected LocalFileOperationsFragment mDragCol = null;
    protected ColumnViewFrameAdapter mColumnListAdapter = null;
    private int mDropColumn = -1;
    private int mDragPos = -1;
    private String mDetailInfo = null;

    private ArrayList<LocalFileOperationsFragment> mColumnListItems =
            new ArrayList<LocalFileOperationsFragment>();
    private File mSearchModeTopDirectory = null;

    public LocalColumnViewFrameLeftFragment(int fragmentLayoutID) {
        super(fragmentLayoutID);
        // TODO Auto-generated constructor stub
    }

    public LocalColumnViewFrameLeftFragment(int fragmentLayoutID, String detailInfo) {
        super(fragmentLayoutID);
        mDetailInfo = detailInfo;
        // TODO Auto-generated constructor stub
    }

    public ArrayList<LocalFileOperationsFragment> getColList() {
        return mColumnListItems;
    }

    public void addToList(LocalFileOperationsFragment column) {
        mColumnListItems.add(column);
        mColumnListAdapter.notifyDataSetChanged();
    }

    public ColumnViewFrameAdapter getAdapter() {
        return mColumnListAdapter;
    }

    public void removeFromList(int colPos) {
        mColumnListItems.remove(colPos);
        mColumnListAdapter.notifyDataSetChanged();
    }

    public int getTotalColumns() {
        return mColumnListAdapter.getCount();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();
        clearColumns();
        initFileListView();
        initFileListAdapter();
    }

    @Override
    public void onNewIntent(Intent intent) {
        int count = mColumnListAdapter.getCount();
        LocalFileOperationsFragment rightCol = mColumnListAdapter.getItem(count - 1);
        rightCol.onNewIntent(intent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected void handleMessage(Message message) {
        switch (message.what) {
            default :
                super.handleMessage(message);
                break;
        }
    }

    @Override
    public void setDragCol(BaseFileManagerFragment dragCol) {
        mDragCol = (LocalFileOperationsFragment) dragCol;
    }

    public LocalFileOperationsFragment getDragCol() {
        return mDragCol;
    }

    protected void clearColumns() {
        try {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            int count = mColumnListAdapter.getCount();
            for (int i = count - 1; i >= 0; i--) {
                Fragment frag = mColumnListAdapter.getItem(i);
                ft.remove(frag);
                removeFromList(i);
            }
            ft.commit();

        } catch (Exception e) {

        }

    }

    protected void initFileListAdapter() {
        mColumnListAdapter =
                new ColumnViewFrameAdapter(mContext, mCurrentHandler, getActivity()
                        .getLayoutInflater(), getFragmentManager());
        File currDir = FileManagerApp.getCurrDir();
        mColumnListAdapter.setListItems(mColumnListItems);
        mFileColListView.setAdapter(mColumnListAdapter);
        ArrayList<File> parentPaths = new ArrayList<File>();
        File temp = null;
        mFileColListView.setOnDragListener(this);

        if (FileManagerApp.getBrowseMode() == FileManagerApp.PAGE_CONTENT_SEARCH) {
            Fragment leftFragment = getFragmentManager().findFragmentById(R.id.home_page);
            if (leftFragment != null) {
                if (leftFragment.getClass().getName().equals(LocalLeftFileManagerFragment.class
                        .getName())) {
                    if (((LocalLeftFileManagerFragment) leftFragment).isShowingSearchResults()) {
                        mSearchModeTopDirectory = currDir.getParentFile();
                    } else {
                        mSearchModeTopDirectory =
                                ((LocalLeftFileManagerFragment) leftFragment)
                                        .getSearchModeTopDirectory();
                    }
                }
            }
        }

        if (mDetailInfo != null) {
            File tempDet = new File(mDetailInfo);
            currDir = tempDet.getParentFile();
        }
        do {
            parentPaths.add(currDir);
            currDir = currDir.getParentFile();
        } while (currDir != null && !FileUtils.isRootDirectory(currDir.getPath()));

        initHomePage(true);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        for (int i = parentPaths.size(); i > 0; i--) {
            temp = parentPaths.get(i - 1);
            LocalColumnViewFragment columnFrag = null;
            if (i > 1) {
                columnFrag =
                        new LocalColumnViewFragment(temp, parentPaths.get(i - 2).getPath(), false);
            } else {
                //the last column (show deepest folder content), no need to highlight
                if (mDetailInfo != null) {
                    columnFrag = new LocalColumnViewFragment(temp, mDetailInfo, true);
                } else {
                    columnFrag = new LocalColumnViewFragment(temp, null, true);
                }

            }
            ft.add(R.id.column_view_frame, columnFrag);
            mColumnListItems.add(columnFrag);
            mColumnListAdapter.notifyDataSetChanged();
        }

        ft.commit();

    }

    private IconifiedText isDetailInfoShowing() {
        try {
            if (mColumnListAdapter != null) {
                LocalFileOperationsFragment detView =
                        mColumnListAdapter.getItem(mColumnListAdapter.getCount() - 1);
                if (detView instanceof LocalDetailedInfoFileManagerFragment) {
                    return ((LocalDetailedInfoFileManagerFragment) detView).getItem();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void switchView(int mode, boolean rebrowse) {
        mFileManagerApp.setViewMode(mode);
        IconifiedText detailInfoPath = isDetailInfoShowing();
        LocalRightFileManagerFragment df1 = LocalRightFileManagerFragment.newInstance();
        FragmentTransaction ft1 = getFragmentManager().beginTransaction();
        ft1.replace(R.id.details, df1);
        ft1.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft1.commit();
        File parentFile = FileManagerApp.getCurrDir().getParentFile();
        if (detailInfoPath != null) {
            LocalLeftFileManagerFragment df = null;
            df =
                    new LocalLeftFileManagerFragment(R.layout.title_file_list, FileManagerApp
                            .getCurrDir(), null);

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.home_page, df);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            if (FileManagerApp.getBrowseMode() == FileManagerApp.PAGE_CONTENT_SEARCH) {
                ft.addToBackStack(null);
                df.setSearchModeTopDirectory(mSearchModeTopDirectory);
            }
            ft.commit();
            df.showDetailInfoFragment(detailInfoPath);

        } else {

            if (!FileUtils.isRootDirectory(parentFile.getPath()) && rebrowse) {
                LocalLeftFileManagerFragment df =
                        new LocalLeftFileManagerFragment(R.layout.title_file_list, parentFile, null);
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.home_page, df);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                if (FileManagerApp.getBrowseMode() == FileManagerApp.PAGE_CONTENT_SEARCH) {
                    ft.addToBackStack(null);
                    df.setSearchModeTopDirectory(mSearchModeTopDirectory);
                }
                ft.commit();
            }
        }

    }

    protected void initFileListView() {
        mFileColListView = (ColumnView) mContentView.findViewById(R.id.column_view_frame);
        mFileColListView.setVisibility(View.VISIBLE);
        mContext = this.getActivity();
    }

    public void initHomePage(boolean isBack) {
        HomePageLeftFileManagerFragment df =
                new HomePageLeftFileManagerFragment(R.layout.homepage_expandlist, isBack);
        FragmentTransaction ft1 = getFragmentManager().beginTransaction();
        ft1.replace(R.id.home_page, df);
        if (FileManagerApp.getBrowseMode() == FileManagerApp.PAGE_CONTENT_SEARCH) {
            Fragment leftFragment = getFragmentManager().findFragmentById(R.id.home_page);
            // Check if transition is from search results screen or not
            if (leftFragment != null) {
                if (leftFragment.getClass().getName().equals(LocalLeftFileManagerFragment.class
                        .getName())) {
                    FileManagerApp.log(TAG + "Adding Home Page to back stack for Column View",
                            false);
                    ft1.addToBackStack(null);
                    ft1.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    int id = ft1.commit();
                    if (((LocalLeftFileManagerFragment) leftFragment).isShowingSearchResults()) {
                        // Since all Fragment Transactions need to be added to the back stack, only
                        // remember those that are resulted from the Search Results Screen
                        try {
                            ((MainFileManagerActivity) getActivity()).addSearchFragments(id);
                        } catch (Exception e) {
                            FileManagerApp
                                    .log(TAG + "Fail to store additional search fragments ID", true);
                        }
                    }
                    return;
                }
            }
        }
        // otherwise no need to add to back stack
        ft1.commit();
    }

    public LocalFileOperationsFragment getLeftMostFragment() {
        LocalFileOperationsFragment rightFragmentCast = null;
        int count = mColumnListAdapter.getCount();
        if (count > 1) {
            rightFragmentCast = mColumnListAdapter.getItem(count - 2);
            mDragCol = mColumnListAdapter.getItem(count - 1);
            mDropColumn = count - 2;
        }
        return rightFragmentCast;
    }

    @Override
    public String getFileName(boolean isContext) {
        String fileName = null;
        LocalFileOperationsFragment rightFragmentCast = getLeftMostFragment();
        if (rightFragmentCast != null) {
            fileName = rightFragmentCast.getFileName(isContext);
        }
        return fileName;
    }

    @Override
    public String getZipFileName(boolean isContext) {
        String fileName = null;
        LocalFileOperationsFragment rightFragmentCast = getLeftMostFragment();
        if (rightFragmentCast != null) {
            fileName = rightFragmentCast.getZipFileName(isContext);
        }
        return fileName;
    }

    @Override
    public void handleCopy(boolean isContextMenu) {
        LocalFileOperationsFragment rightFragmentCast = getLeftMostFragment();
        if (rightFragmentCast != null) {
            rightFragmentCast.handleCopy(false);
        }
    }

    @Override
    public void handleDelete(boolean isContext) {
        LocalFileOperationsFragment rightFragmentCast = null;
        boolean isLeftHandle = false;
        if (mCurrentActionMode == null) {
            rightFragmentCast = getLeftMostFragment();
            isLeftHandle = true;
        } else {
            rightFragmentCast = getColumnFragment();
        }
        if (rightFragmentCast != null) {
            rightFragmentCast.handleDelete(false);
        }
        if (isLeftHandle) {
            int count = mColumnListAdapter.getCount();
            if (count > 1) {
                try {
                    FileManagerApp.setCurrDir(FileManagerApp.getCurrDir().getParentFile());
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    Fragment rightFragment = getFragmentManager().findFragmentById(R.id.details);
                    ColumnViewFrameAdapter coladapter =
                            ((LocalColumnViewFrameLeftFragment) rightFragment).getAdapter();
                    for (int i = count - 1; i > count - 2; i--) {
                        Fragment frag = coladapter.getItem(i);
                        ft.remove(frag);
                        ((LocalColumnViewFrameLeftFragment) rightFragment).removeFromList(i);
                    }
                    ft.commit();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

    }

    @Override
    public void setUpViewSorting(final int sortMode) {
        for (int i = 0; i < mColumnListAdapter.getCount(); i++) {
            LocalFileOperationsFragment rightFragmentCast = mColumnListAdapter.getItem(i);
            if (rightFragmentCast != null) {
                rightFragmentCast.setUpViewSorting(sortMode);
            }
        }
    }

    @Override
    public void handleSelectMultiple() {
        LocalFileOperationsFragment rightFragmentCast = getColumnFragment();
        if (rightFragmentCast != null) {
            rightFragmentCast.startMultiSelect(false);
        }
    }

    @Override
    public IconifiedText getHighlightedItem() {
        LocalFileOperationsFragment rightFragmentCast = getLeftMostFragment();
        if (rightFragmentCast != null) {
            return rightFragmentCast.getHighlightedItem();
        }
        return null;
    }

    @Override
    public void handlePrint(boolean isContextMenu) {
        LocalFileOperationsFragment rightFragmentCast = getLeftMostFragment();
        if (rightFragmentCast != null) {
            rightFragmentCast.handlePrint(isContextMenu);
        }
    }

    @Override
    public void handleMove(boolean isContextMenu) {
        LocalFileOperationsFragment rightFragmentCast = getLeftMostFragment();
        if (rightFragmentCast != null) {
            rightFragmentCast.handleMove(false);
        }
    }

    @Override
    public void handlePaste(ArrayList<String> sourceFile, String destDir) {
        LocalFileOperationsFragment rightFragmentCast = null;
        try {
            mDropColumn = mDragPos;
            if (mDropColumn != -1) {
                rightFragmentCast = mColumnListAdapter.getItem(mDropColumn);

            } else {
                rightFragmentCast = getColumnFragment();
                mDropColumn = mColumnListAdapter.getCount() - 1;
            }
            /* int tPos = rightFragmentCast.getDropTargetPosition();
             IconifiedText item = (IconifiedText) rightFragmentCast.mFileListAdapter.getItem(tPos);*/
            if (rightFragmentCast != null) {
                rightFragmentCast
                        .handlePaste(sourceFile, rightFragmentCast.getDropPath().getPath());
            }
            if (mCurrentActionMode != null) {
                mCurrentActionMode.finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clearActionMode() {
        if (mCurrentActionMode != null) {
            mCurrentActionMode.finish(); // adapter.setItemChecked(position);
        }
    }

    @Override
    public void addShortcut(File aFile) {
        LocalFileOperationsFragment rightFragmentCast = null;
        rightFragmentCast = getColumnFragment();

        if (rightFragmentCast != null) {
            rightFragmentCast.addShortcut(aFile);
        }
        if (mCurrentActionMode != null) {
            mCurrentActionMode.finish();
            mColumnListAdapter.setselectPos(-1);
        }

    }

    @Override
    public void refreshLeft(boolean isFullrefresh) {
        // TODO Auto-generated method stub
        LocalFileOperationsFragment rightFragmentCast = null;
        try {
            rightFragmentCast = mColumnListAdapter.getItem(mDropColumn);
            if (rightFragmentCast != null) {
                rightFragmentCast.refreshList(); //Somehow highlight is messed up, so commenting now, 
                //the # items will not be updated in this case, so it will be bug for now.
            }

            if (mDragCol != null) {
                // Check if the column is still present before refresh
                if (mColumnListAdapter.getItemPos(mDragCol) != -1) {
                    mDragCol.refreshList();
                }
            }
            mDragCol = null;
            if (mDropColumn > 0) {
                rightFragmentCast = mColumnListAdapter.getItem(mDropColumn - 1);
                if (rightFragmentCast != null) {
                    rightFragmentCast.refreshLeft(false);
                }
            }
            mDropColumn = -1;

        } catch (Exception e) {

        }

    }

    public LocalFileOperationsFragment getColumnFragment() {
        LocalFileOperationsFragment rightFragmentCast = null;
        int count = mColumnListAdapter.getCount();
        if (count >= 1) {
            rightFragmentCast = mColumnListAdapter.getItem(count - 1);
        }
        return rightFragmentCast;
    }

    @Override
    public void exitMultiSelect() {
        mActionModeHandler = null;
        mCurrentActionMode = null;
        int multiPos = mColumnListAdapter.getMultiselectPos();
        if (multiPos >= 0 && multiPos < mColumnListAdapter.getCount()) {
            mColumnListAdapter.getItem(multiPos).exitMultiSelect();
        }
    }

    @Override
    protected boolean onDragStarted(DragEvent event) {
        boolean ret = super.onDragStarted(event);
        return ret;
    }

    @Override
    protected void onDragEnded() {
        super.onDragEnded();
        mDragPos = -1;
    }

    @Override
    public void openClickedFile(String sPath, IconifiedText text) {
        // TODO Auto-generated method stub
        try {
            int pos = mColumnListAdapter.getselectPos();
            if (pos >= 0) {
                if (!((MainFileManagerActivity) getActivity()).getIsDragMode() &&
                        mCurrentActionMode != null) {
                    mCurrentActionMode.finish();
                }
                mColumnListAdapter.setselectPos(-1);
            }

            clearColumns();
            File currDir = FileManagerApp.getCurrDir();
            ArrayList<File> parentPaths = new ArrayList<File>();
            File temp = null;
            do {
                parentPaths.add(currDir);
                currDir = currDir.getParentFile();
            } while (currDir != null && !FileUtils.isRootDirectory(currDir.getPath()));

            for (int i = parentPaths.size(); i > 0; i--) {
                synchronized (this) {
                    LocalColumnViewFragment columnFrag = null;
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    temp = parentPaths.get(i - 1);
                    if (i > 1) {
                        columnFrag =
                                new LocalColumnViewFragment(temp, parentPaths.get(i - 2).getPath(),
                                        false);
                    } else {
                        //the last column (show deepest folder content), no need to highlight
                        columnFrag = new LocalColumnViewFragment(temp, null, true);
                    }
                    if (columnFrag != null) {
                        ft.add(R.id.column_view_frame, columnFrag);
                        mColumnListItems.add(columnFrag);
                        ft.commit();
                        mColumnListAdapter.notifyDataSetChanged();
                    }
                }
            }

        } catch (Exception e) {

        }
    }

    /**
     * Called while dragging;  highlight possible drop targets, and autoscroll the list.
     */
    @Override
    protected void onDragLocation(DragEvent event) {
        try {
            int position = getColumnDragOrDrop(event);
            mDragPos = position;
            LocalFileOperationsFragment rightFragmentCast = mColumnListAdapter.getItem(position);

            if (rightFragmentCast != null) {
                rightFragmentCast.onDragLocation(event);
            }
            int rawTouchX = (int) event.getX();
            dragOverHorizontalScroll(rawTouchX, event, position);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private int getColumnDragOrDrop(DragEvent event) {
        int position = -1;
        Rect viewRect = new Rect();
        for (int i = 0; i < mFileColListView.getChildCount(); i++) {
            View child = mFileColListView.getChildAt(i);
            int left = child.getLeft();
            int right = child.getRight();
            int top = child.getTop();
            int bottom = child.getBottom();
            viewRect.set(left, top, right, bottom);
            event.getX();
            event.getY();
            if (viewRect.contains((int) event.getX(), (int) event.getY())) {
                position = i;
                return position;
            }
        }

        return position;
    }

    @Override
    public boolean handleDrop(DragEvent event) {
        return onDrop(event);
    }

    @Override
    protected boolean onDrop(DragEvent event) {
        try {
            getFragmentManager().findFragmentById(R.id.home_page);
            int position = mDragPos;
            LocalFileOperationsFragment rightFragmentCast = mColumnListAdapter.getItem(position);

            if (rightFragmentCast != null) {
                mDropColumn = position;
                /**To Do Bug: Need to fix, so when dropped in a folder location, will drop it inside the folder instead of the column itself */
                boolean val = rightFragmentCast.onDrop(event);
                if (val) {
                    if (mDragInProgress) {
                        mDragInProgress = false;
                        // Stop any scrolling that was going on
                        stopScrolling();
                    }
                    if (mCurrentActionMode != null) {
                        mCurrentActionMode.finish();
                        mColumnListAdapter.setselectPos(-1);
                    }
                } else {
                    mDragPos = -1;
                }
                return val;
            }
        } catch (Exception e) {
            e.printStackTrace();
            FileManagerApp.log(e.getMessage(), true);
        }
        mDragPos = -1;
        return false;
    }

    @Override
    public boolean upOneLevel() {
        try {
            if (FileManagerApp.getCurrDir() != null) {
                File prevDirectory = FileManagerApp.getCurrDir().getParentFile();

                if (FileManagerApp.getBrowseMode() == FileManagerApp.PAGE_CONTENT_SEARCH) {
                    if (mSearchModeTopDirectory != null) {
                        if ((mSearchModeTopDirectory.getAbsolutePath()).equals(prevDirectory
                                .getAbsolutePath())) {
                            return false;
                        }
                    }
                }

                if (prevDirectory == null) {
                    getActivity().finish();
                    return true;
                } else {
                    if (FileUtils.isRootDirectory(prevDirectory.getAbsolutePath()) &&
                            (mColumnListAdapter.getCount() < 2)) {//Possible a det fragment is triggered from first col

                        getActivity().finish();
                        return false;
                    } else {
                        // Start to pop out last column
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        LocalFileOperationsFragment lastcolumn =
                                mColumnListAdapter.getItem(mColumnListAdapter.getCount() - 1);
                        try {
                            //Do not set prev dir if last fragment is det view
                            if (!lastcolumn.getClass().getName()
                                    .equals(LocalDetailedInfoFileManagerFragment.class.getName())) {
                                FileManagerApp.setCurrDir(prevDirectory);
                            }
                            ft.remove(lastcolumn);
                            mColumnListItems.remove(lastcolumn);
                            mColumnListAdapter.notifyDataSetChanged();
                            ft.commit();
                            lastcolumn.onDestroy();
                            //Update highlight of prev directory
                            LocalColumnViewFragment prevcolumn =
                                    (LocalColumnViewFragment) mColumnListAdapter
                                            .getItem(mColumnListAdapter.getCount() - 1);
                            prevcolumn.setIsTop(true);
                            prevcolumn.clearHighlight();
                            prevcolumn.refreshList(); // Need to get all context menu back
                            return true;
                        } catch (Exception e) {
                            FileManagerApp.log(TAG + "Exception in up one level", true);
                            return false;
                        }
                    }
                }
            }
        } catch (Exception e) {

        }
        return false;
    }

    protected void dragOverHorizontalScroll(int rawTouchX, DragEvent event, int position) {
        int scrollDiff = rawTouchX - (mFileColListView.getWidth() - SCROLL_ZONE_SIZE_X);
        boolean scrollDown = (scrollDiff > 0);
        int width = mFileColListView.getWidth();
        boolean isScroll = true;
        boolean scrollUp = (SCROLL_ZONE_SIZE_X > rawTouchX);
        int dropPos = getColumnDragOrDrop(event);
        View childDrop = mFileColListView.getChildAt(dropPos);
        if ((dropPos == mFileColListView.getChildCount() - 1)) {
            if (childDrop.getRight() <= width) {
                isScroll = false; //Do not scroll if we are at the end of the list and reached end of screen
            }
        } else {
            if (dropPos == 0) {
                if (childDrop.getLeft() >= 0) {
                    isScroll = false;//Do not scroll if we are at the beginning of the list and reached beg of screen
                }
            }
        }
        if (isScroll && scrollDown) {
            mFileColListView.scrollTo(SCROLL_OFFSET);
            FileManagerApp.log(TAG + " START TARGET SCROLLING DOWN", false);
            mTargetScrolling = true;
        } else if (isScroll && scrollUp) {
            mFileColListView.scrollTo(-SCROLL_OFFSET);
            FileManagerApp.log(TAG + " START TARGET SCROLLING UP", false);
            mTargetScrolling = true;
        } else if (!scrollUp && !scrollDown) {
            stopScrolling();
        }
    }

    protected void stopScrolling() {
        if (mTargetScrolling) {
            mTargetScrolling = false;
            FileManagerApp.log(TAG + " ========== STOP TARGET SCROLLING", false);
            mFileColListView.scrollTo(0, 0);
        }
    }

    @Override
    protected void onDragExited() {
        stopScrolling();
    }

    public File getSearchModeTopDirectory() {
        return mSearchModeTopDirectory;
    }
}
