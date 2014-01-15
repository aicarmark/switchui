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
package com.motorola.filemanager.local;

import java.io.File;
import java.util.ArrayList;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Message;
import android.view.DragEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.filemanager.BaseFileManagerFragment;
import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.HomePageLeftFileManagerFragment;
import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;
import com.motorola.filemanager.local.threads.SearchDirectory;
import com.motorola.filemanager.samba.service.DownloadServer;
import com.motorola.filemanager.ui.IconifiedText;
import com.motorola.filemanager.ui.IconifiedTextListAdapter;
import com.motorola.filemanager.utils.FileUtils;

public class LocalLeftFileManagerFragment extends LocalFileOperationsFragment {
    private static String TAG = "LocalLeftFileManagerFragment: ";
    private File mClickedfile = null;
    public static LocalDetailedInfoFileManagerFragment mDetailInfoFragment =
            new LocalDetailedInfoFileManagerFragment(R.layout.filemanager_details_view);

    private static int INVALID_POS = -1;
    private int mNextItemPos = INVALID_POS;
    private SearchDirectory mSearchDirectory = null;
    private boolean mShowingSearchResults = false;
    private File mSearchedDirectory = null;
    private File mSearchModeTopDirectory = null;
    private DirectoryContents mSearchResults = null;
    private LinearLayout mSearchHeadingView;
    private TextView mSearchResultTextView;
    private TextView mSearchResultCountView;
    private TextView mEmptyText;

    public LocalLeftFileManagerFragment(int fragmentLayoutID) {
        super(fragmentLayoutID);
        // TODO Auto-generated constructor stub
    }

    public LocalLeftFileManagerFragment(int fragmentLayoutID, File clickedFile,
                                        String searchedString) {
        super(fragmentLayoutID);
        mClickedfile = clickedFile;
        mCurrentSearchedString = searchedString;
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void initFileListView() {
        mFileListView = (AbsListView) mContentView.findViewById(R.id.file_list_t);
        if (mFileListView == null) {
            return;
        }
        mSearchHeadingView = (LinearLayout) mContentView.findViewById(R.id.search_heading);
        mSearchResultTextView = (TextView) mContentView.findViewById(R.id.search_result_text);
        mSearchResultCountView = (TextView) mContentView.findViewById(R.id.search_result_count);
        mEmptyText = (TextView) mContentView.findViewById(R.id.empty_text);
        mEmptyText.setVisibility(View.GONE);

        mContext = this.getActivity();
        FileManagerApp.setHomePage(false);
        if (FileManagerApp.getBrowseMode() == FileManagerApp.PAGE_CONTENT_SEARCH) {
            // Coming hear means it is launching a new fragment because the user clicked on a folder
            // that was in the search result or the user did a search again in the search results
            // screen. Remember this folder level so we know when to exit.
            if (mSearchModeTopDirectory == null) {
                mSearchModeTopDirectory = mClickedfile;
            }
        }
        if (mCurrentSearchedString != null) {
            // Launched with search request
            if (mShowingSearchResults) {
                // Returning to search results.
                showDirectoryContents(mSearchResults);
                return;
            }
            FileManagerApp.log(TAG + "initFileListView mCurrentSearchedString = " +
                    mCurrentSearchedString, false);
            if (mClickedfile != null) {
                startSearchDirectory(mClickedfile, mCurrentSearchedString);
            } else {
                startSearchDirectory(FileManagerApp.getCurrDir(), mCurrentSearchedString);
            }
            return;
        } else if (mClickedfile == null) {
            mClickedfile = FileManagerApp.getCurrDir().getParentFile();
        }
        FileManagerApp.log(TAG + "initFileListView mClickedfile = " + mClickedfile, false);
        browseTo(mClickedfile);
        if (FileManagerApp.getBrowseMode() == FileManagerApp.PAGE_CONTENT_SEARCH) {
            // Coming hear means it is launching a new fragment because the user clicked on a folder
            // that was in the search result. Remember this folder level so we know when to exit.
            mSearchModeTopDirectory = mClickedfile;
        }
    }

    public void initHomePage(boolean isBack) {
        // Show the search icon instead of the widget
        if (mContext != null) {
            ((MainFileManagerActivity) mContext).hideSearchWidget();
        }
        HomePageLeftFileManagerFragment df =
                new HomePageLeftFileManagerFragment(R.layout.homepage_expandlist, isBack);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.home_page, df);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    @Override
    public void updateLeftFragment(File clickedFile) {
        FileManagerApp.setHomePage(false);
        if (mShowingSearchResults == true) {
            // If update was requested from the Right fragment while the search results are showing,
            // add a new fragment so that the search results can be maintained.
            LocalLeftFileManagerFragment df =
                    new LocalLeftFileManagerFragment(R.layout.title_file_list, clickedFile, null);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.home_page, df);
            ft.addToBackStack(null);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            int id = ft.commit();
            try {
                ((MainFileManagerActivity) getActivity()).addSearchFragments(id);
                ((MainFileManagerActivity) getActivity()).enableViewModeSelection();
            } catch (Exception e) {
                FileManagerApp.log(TAG + "Fail to store additional search fragments ID", false);
            }
        } else {
            if (FileUtils.isRootDirectory(FileManagerApp.getCurrDir().getParent())) {
                initHomePage(true);
            } else {
                mClickedfile = clickedFile;
                browseTo(clickedFile);
            }
        }

    }

    @Override
    public void refreshList(final File aDirectory) {
        if (!FileManagerApp.isHomePage()) {
            if (mDirectoryScanner != null) {
                mDirectoryScanner.setTitleFragmentSortMode(true);
            }
            super.refreshList(aDirectory);
        }
    }

    @Override
    protected void handleMessage(Message message) {
        switch (message.what) {
            case MESSAGE_SHOW_DIRECTORY_CONTENTS :
                hideLoadingDialog();
                if (message.obj != null) {
                    FileManagerApp.log(TAG + "obj not null", false);
                    showDirectoryContents((DirectoryContents) message.obj);
                }
                int position = mFileListAdapter.getPosition(mCurrHighlightedItem);
                // In long lists, to move the selected item to top in left fragement
                mFileListView.setSelection(position);
                if (((MainFileManagerActivity) getActivity()).getIsDragMode()) {
                    updatehighlightSourceLeft();
                }
                break;
            case MESSAGE_ICON_CHANGED :
                super.handleMessage(message);
                break;

            case MESSAGE_SELECT_DRAG_LOCATION :
                IconifiedText text = (IconifiedText) message.obj;
                if (text != null) {
                    mFileListAdapter.clearCurrentHighlightedItem(mCurrHighlightedItem);
                    mFileListAdapter.setHighlightedItem(text, false);
                    mCurrHighlightedItem = text;
                    //setItemChecked(int position)
                    String path = text.getPathInfo();
                    File file = FileUtils.getFile(path);
                    if (file.isDirectory() && !mDetailInfoFragment.isVisible()) {
                        updateRightFragment(text);
                    } else if (mDetailInfoFragment.isVisible() && file.isDirectory()) {
                        FileManagerApp.setCurrDir(file);
                        getFragmentManager().popBackStack();
                    } else {
                        showDetailInfoFragment(text);
                    }
                }
                break;

            case MESSAGE_MOVE_FINISHED :
                File curDir = FileManagerApp.getCurrDir();
                if (curDir != null) {
                    /* As the moved item is originally from left fragment,
                     * make sure left fragment's display is updated */
                    refreshLeft(true);
                }

                if (message.arg1 == DownloadServer.TRANSFER_COMPLETE) {
                    Toast.makeText(mContext,
                            mContext.getResources().getQuantityString(R.plurals.move_success,
                                    message.arg2,
                                    message.arg2),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, R.string.error_moving, Toast.LENGTH_SHORT).show();
                }
                break;

            case MESSAGE_DELETE_FINISHED :
                hideLoadingDialog();
                boolean goOneLevelUp = false;
                Fragment fragment = getFragmentManager().findFragmentById(R.id.details);

                /* locate the next item to be displayed in left panel, if not,
                 * go up one level
                 */
                IconifiedTextListAdapter adapter =
                        (IconifiedTextListAdapter) mFileListView.getAdapter();
                if ((mNextItemPos != INVALID_POS) && (adapter != null)) {
                    mCurrHighlightedItem = (IconifiedText) adapter.getItem(mNextItemPos);
                    if (mCurrHighlightedItem == null) {
                        goOneLevelUp = true;
                    }
                } else {
                    goOneLevelUp = true;
                }
                if (goOneLevelUp == false) {
                    refreshLeft(true);

                    if (mCurrHighlightedItem.isDirectory()) {
                        // update current path
                        // update the right Fragment
                        File newCurPath = new File(mCurrHighlightedItem.getPathInfo());
                        if (newCurPath != null) {
                            FileManagerApp.setCurrDir(newCurPath);

                            /* the deleted is a file, need to pop out detail view
                             */
                            if ((fragment.getClass().getName()
                                    .equals(LocalDetailedInfoFileManagerFragment.class.getName()))) {
                                getFragmentManager().popBackStackImmediate();
                            } else {
                                updateRightFragment();
                            }
                        }
                    } else {
                        //the newly highlighted item on left panel is a file, show/update detail view on right panel
                        showDetailInfoFragment(mCurrHighlightedItem);
                        //update the path since the old path is no longer accessible
                        //this is needed to handle the back key after back from detail view screen
                        File oldCurPath = FileManagerApp.getCurrDir();
                        if (oldCurPath != null) {
                            FileManagerApp.setCurrDir(oldCurPath.getParentFile());
                        }
                    }
                } else {
                    //no next item in current level, go up one level
                    upOneLevel();
                    if ((fragment.getClass().getName()
                            .equals(LocalDetailedInfoFileManagerFragment.class.getName()))) {
                        FileManagerApp.setCurrDir(FileManagerApp.getCurrDir().getParentFile());
                        getFragmentManager().popBackStackImmediate();
                    } else {
                        LocalBaseFileManagerFragment det =
                                (LocalBaseFileManagerFragment) getFragmentManager()
                                        .findFragmentById(R.id.details);
                        det.upOneLevel();
                    }
                }

                if (message.arg1 == ARG_DELETE_COMPLETED) {
                    Toast.makeText(mContext,
                            mContext.getResources().getQuantityString(R.plurals.delete_success,
                                    message.arg2,
                                    message.arg2),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, R.string.delete_failed, Toast.LENGTH_SHORT).show();
                }
                break;
            case MESSAGE_ZIP_SUCCESSFUL :
                hideProgressInfoDialog();
                //build the new item using zip output file and add it to the file list
                // better for performance especially when the list is long
                File file = (File) message.obj;
                mFileListAdapter.addItem(IconifiedText.buildIconItem(mContext, file, FileManagerApp
                        .getStorageMediumMode()));
                mFileListAdapter.notifyDataSetChanged();
                break;

            case MESSAGE_ZIP_FAILED :
                //need to overwrite the handler of this message in LocalFileOperation since that handler
                //can't be used for left screen
                hideProgressInfoDialog();
                Toast.makeText(mContext, R.string.error_zipping_file, Toast.LENGTH_SHORT).show();
                break;

            case MESSAGE_ZIP_STOPPED :
                //need to overwrite the handler of this message in LocalFileOperation since that handler
                //can't be used for left screen
                hideProgressInfoDialog();
                Toast.makeText(mContext, R.string.stop_zip_file, Toast.LENGTH_SHORT).show();
                break;

            default :
                super.handleMessage(message);
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (FileManagerApp.getBrowseMode() == FileManagerApp.PAGE_CONTENT_SEARCH) {
            FileManagerApp.log(TAG + "onResume called in PAGE_CONTENT_SEARCH mode", false);
            if ((mCurrHighlightedItem != null) && !mDetailInfoFragment.isVisible()) {
                updateRightFragment(mCurrHighlightedItem);
            }
            // Need the search widget to continue to show with the searched text but not focused
            if (mShowingSearchResults && (mContext != null)) {
                ((MainFileManagerActivity) mContext)
                        .showSearchWidget(false, mCurrentSearchedString);
                // Force search results to be in list view
                setupSearchViewMode();
            }
        } else if (mShowingSearchResults) {
            // User pressed home key so Search is existed by popping the additional fragments
            // Need to refresh to the parent directory of the current directory
            FileManagerApp.log(TAG +
                    "onResume called with mShowingSearchResults = true in Normal mode", false);
            if (mFileManagerApp.getViewMode() == FileManagerApp.COLUMNVIEW) {
                // Right fragment already updated in MainFileManagerActivity
                // Just need to return the left to home page
                initHomePage(true);
            } else {
                mClickedfile = FileManagerApp.getCurrDir().getParentFile();
                browseTo(mClickedfile);
            }
            mShowingSearchResults = false;
            mSearchResults = null;
        }
    }

    AdapterView.OnItemClickListener fileListListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            IconifiedText text = (IconifiedText) mFileListAdapter.getItem(position);
            //det.clearActionMode();
            if (text != null) {
                mFileListAdapter.clearCurrentHighlightedItem(mCurrHighlightedItem);
                mFileListAdapter.setHighlightedItem(text, false);
                mCurrHighlightedItem = text;
                String path = text.getPathInfo();
                File file = FileUtils.getFile(path);
                if (file.isDirectory() && !mDetailInfoFragment.isVisible()) {
                    updateRightFragment(position);
                } else if (mDetailInfoFragment.isVisible() && file.isDirectory()) {
                    FileManagerApp.setCurrDir(file);
                    getFragmentManager().popBackStack();
                } else {
                    showDetailInfoFragment(text);
                }
                if (getActivity() != null) {
                    getActivity().invalidateOptionsMenu();
                }
            }
        }
    };

    private void createColumnFragment() {
        LocalColumnViewFrameLeftFragment df =
                new LocalColumnViewFrameLeftFragment(R.layout.column_view);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.details, df);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();

    }

    @Override
    public void updateRightFragment() {
        if (mFileManagerApp.getViewMode() == FileManagerApp.COLUMNVIEW) {
            createColumnFragment();
        } else {
            LocalRightFileManagerFragment df = LocalRightFileManagerFragment.newInstance();
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.details, df);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }
    }

    private void updateRightFragment(int position) {
        IconifiedText text = (IconifiedText) mFileListAdapter.getItem(position);
        updateRightFragment(text);
    }

    private void updateRightFragment(IconifiedText text) {
        LocalFileOperationsFragment det = null;
        try {
            det = (LocalFileOperationsFragment) getFragmentManager().findFragmentById(R.id.details);
        } catch (Exception e) {
            FileManagerApp
                    .log(TAG +
                            "updateRightFragment - Details fragment cannot be cast to LocalFileOperationsFragment",
                            true);
            // This case happens when switching from Column View. When Right Fragment is reactivated, it will load the
            // current path so set it to the path we want
            String path = text.getPathInfo();
            FileManagerApp.setCurrDir(FileUtils.getFile(path));
            return;
        }

        if (det != null) {
            if (!((MainFileManagerActivity) getActivity()).getIsDragMode()) {
                det.clearActionMode();
            }
            String path = text.getPathInfo();
            int last = path.lastIndexOf("/");
            if (last != -1) {
                det.openClickedFile(text);
            } else {
                initHomePage(false);
            }

        } else {
            updateRightFragment();
        }
    }

    @Override
    protected void showDirectoryContents(DirectoryContents contents) {
        super.showDirectoryContents(contents);
        mFileListView.setAdapter(mFileListAdapter);
        mFileListView.setOnItemClickListener(fileListListener);
        if (!mListDir.isEmpty() || !mListFile.isEmpty()) {
            mEmptyText.setVisibility(View.GONE);
            mFileListView.setVisibility(View.VISIBLE);
        } else {
            mEmptyText.setVisibility(View.VISIBLE);
            mEmptyText.setText(getString(R.string.this_folder_is_empty));
            mFileListView.setVisibility(View.GONE);
        }
        if ((FileManagerApp.getBrowseMode() == FileManagerApp.PAGE_CONTENT_SEARCH) &&
                (mShowingSearchResults == true)) {
            mSearchResults = contents;
            mSearchHeadingView.setVisibility(View.VISIBLE);
            mSearchResultTextView.setText(getString((R.string.search_result), mDisplayText));
            mSearchResultCountView.setText(Integer.toString(mFileListAdapter.getCount()));
            mEmptyText.setText(getString(R.string.empty_search_result));

            setupSearchViewMode();
            if (mFileListAdapter.getCount() > 0) {
                IconifiedText text = null;
                if (mCurrHighlightedItem != null) {
                    // Match will be found if returning to list
                    text =
                            mFileListAdapter.updateHighlightedItem(mCurrHighlightedItem
                                    .getPathInfo(), false);
                }
                if (text == null) {
                    // Match was not found. Highlight and load the 1st item in the list
                    mFileListAdapter.clearCurrentHighlightedItem(mCurrHighlightedItem);
                    text = (IconifiedText) mFileListAdapter.getItem(0);
                    mFileListAdapter.setHighlightedItem(text, false);
                    mCurrHighlightedItem = text;
                }

                String path = text.getPathInfo();
                File file = FileUtils.getFile(path);
                if (file.isDirectory() && !mDetailInfoFragment.isVisible()) {
                    FileManagerApp.setCurrDir(file);
                    updateRightFragment(text);
                } else if (mDetailInfoFragment.isVisible() && file.isDirectory()) {
                    FileManagerApp.setCurrDir(file);
                    getFragmentManager().popBackStack();
                } else {
                    showDetailInfoFragment(text);
                }
            } else {
                // No items found
                BaseFileManagerFragment det =
                        (BaseFileManagerFragment) getFragmentManager()
                                .findFragmentById(R.id.details);
                if (det != null) {
                    det.blankScreen();
                }
            }
            // Need the search widget to continue to show with the searched text but not focused
            if (mContext != null) {
                ((MainFileManagerActivity) mContext)
                        .showSearchWidget(false, mCurrentSearchedString);
            }
        } else {
            String curPath = null;
            if (mDetailInfoFragment.isVisible()) {
                curPath = mDetailInfoFragment.getItemPath();
            } else {
                File curDir = FileManagerApp.getCurrDir();
                if (curDir != null) {
                    curPath = curDir.getAbsolutePath();
                }
            }
            updateHighlightedItem(curPath);
            mSearchHeadingView.setVisibility(View.GONE);
            // Show the search icon instead of the widget when the fragment is updated.
            if (mContext != null) {
                ((MainFileManagerActivity) mContext).hideSearchWidget();
            }
        }
    }

    @Override
    public boolean upOneLevel() {
        try {
            if (FileManagerApp.getBrowseMode() == FileManagerApp.PAGE_CONTENT_SEARCH) {
                if (mShowingSearchResults) {
                    // Search results are showing. Exit search results and show the
                    // folder list or home page for the searched directory
                    if (mSearchedDirectory != null) {
                        mShowingSearchResults = false;
                        mSearchResults = null;
                        try {
                            if (((MainFileManagerActivity) getActivity()).isSearchFragmentsEmpty()) {
                                // All search results are backed out so return to normal mode
                                FileManagerApp.setBrowseMode(FileManagerApp.PAGE_CONTENT_NORMAL);
                            } else if (mSearchModeTopDirectory != null) {
                                if ((mSearchedDirectory.getAbsolutePath())
                                        .equals(mSearchModeTopDirectory.getAbsolutePath())) {
                                    // This fragment was launched with the search results as the 1st screen. Pop it out.
                                    return false;
                                }
                            }
                            ((MainFileManagerActivity) getActivity()).enableViewModeSelection();
                        } catch (Exception e) {
                            FileManagerApp
                                    .log(TAG + "Failed to check Search Fragments count", true);
                        }
                        if (mFileManagerApp.getUserSelectedViewMode() != FileManagerApp.COLUMNVIEW) {
                            // Switching to column view will update left fragment so no need to do here
                            if (FileUtils.isRootDirectory(mSearchedDirectory.getParent())) {
                                initHomePage(true);
                            } else {
                                browseTo(mSearchedDirectory.getParentFile());
                            }
                        }
                        // update the right fragment
                        if (mDetailInfoFragment.isVisible()) {
                            FileManagerApp.setCurrDir(mSearchedDirectory);
                            getFragmentManager().popBackStack();
                        } else {
                            BaseFileManagerFragment det =
                                    (BaseFileManagerFragment) getFragmentManager()
                                            .findFragmentById(R.id.details);
                            if (det != null) {
                                switch (mFileManagerApp.getUserSelectedViewMode()) {
                                    case FileManagerApp.LISTVIEW :
                                        // No need to switch view
                                        FileManagerApp.log(TAG + "returning to list view ", false);
                                        det.openClickedFile(mSearchedDirectory.getAbsolutePath(),
                                                null);
                                        det.showScreen();
                                        break;
                                    case FileManagerApp.GRIDVIEW :
                                        // Just do the switch to Grid View but no need to
                                        // Browse inside the switchView statement since it
                                        // will be performed by below code
                                        FileManagerApp.log(TAG + "returning to grid view ", false);
                                        det.switchView(FileManagerApp.GRIDVIEW, false);
                                        det.openClickedFile(mSearchedDirectory.getAbsolutePath(),
                                                null);
                                        det.showScreen();
                                        break;
                                    case FileManagerApp.COLUMNVIEW :
                                        FileManagerApp
                                                .log(TAG + "returning to column view ", false);
                                        FileManagerApp.setCurrDir(mSearchedDirectory);
                                        det.switchView(FileManagerApp.COLUMNVIEW, true);
                                        break;
                                }
                            }
                        }
                        return true;
                    }
                } else if (mSearchModeTopDirectory != null) {
                    // This is only possible if user clicked a folder to traverse down from the
                    // search results. If we are already showing this user selected folder, then
                    // exit this fragment and go back to the search results stored in the older
                    // fragment
                    File currDir = null;
                    if (mClickedfile != null) {
                        currDir = mClickedfile;
                    } else {
                        currDir = FileManagerApp.getCurrDir().getParentFile();
                    }
                    if (currDir != null) {
                        String currDirPath = currDir.getAbsolutePath();
                        if ((currDirPath != null) &&
                                (currDirPath.equals(mSearchModeTopDirectory.getAbsolutePath()))) {
                            // let the activity take care of popping the stack
                            return false;
                        } else {
                            mClickedfile = currDir.getParentFile();
                            browseTo(mClickedfile);
                            LocalRightFileManagerFragment det =
                                    (LocalRightFileManagerFragment) getFragmentManager()
                                            .findFragmentById(R.id.details);
                            det.upOneLevel();
                            return true;
                        }
                    }
                }
            }
            // this is the normal case, no search at all       

            File currDir = null;
            if (mClickedfile != null) {
                if (FileUtils.isRootDirectory(mClickedfile.getParent())) {
                    initHomePage(true);
                    return true;
                } else {
                    if (mFileListAdapter.getCount() > 0) {
                        File clickParent = mClickedfile.getParentFile();
                        if (clickParent != null) {
                            browseTo(clickParent);
                            mClickedfile = clickParent;
                            return true;
                        }
                    }
                }
            } else {
                currDir = FileManagerApp.getCurrDir();
            }
            if (currDir != null) {
                File leftCurrDir = currDir.getParentFile();
                if (leftCurrDir != null) {
                    if (FileUtils.isRootDirectory(leftCurrDir.getParent())) {
                        initHomePage(true);
                        return true;
                    } else {
                        if (mFileListAdapter.getCount() > 0) {
                            if (leftCurrDir.getParentFile() != null) {
                                browseTo(leftCurrDir.getParentFile());
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            FileManagerApp.log(TAG + "Exception recieved in upOneLevel", true);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void showDetailInfoFragment(IconifiedText item) {
        FileManagerApp.log(TAG + "showDetailInfoFragment()", false);

        mDetailInfoFragment.setItem(item);
        if (!mDetailInfoFragment.isVisible()) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.details, mDetailInfoFragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.addToBackStack(null);
            ft.commit();
        } else {
            mDetailInfoFragment.showContent();
        }
    }

    public void selectPosition(int position) {
        updateRightFragment(position);
    }

    public void startSearchDirectory(File searchedDirectory, String searchedString) {
        if ((searchedString == null) || (searchedString.length() == 0)) {
            FileManagerApp.log(TAG + "startSearchDirectory - searchedString is blank", false);
            return;
        }
        if (searchedDirectory != null) {
            stopSearchThread();
            FileManagerApp.log(TAG + "startSearchDirectory - searchedDirectory is " +
                    searchedDirectory, false);
            // update mode
            int mode = FileManagerApp.INDEX_INTERNAL_MEMORY;
            mSearchDirectory =
                    new SearchDirectory(searchedDirectory,
                            ((MainFileManagerActivity) getActivity()).getCurContext(),
                            mCurrentHandler, searchedString, mode);
            if (mSearchDirectory != null) {
                synchronized (mDirectorySearchLock) {
                    mSearchDirectory.start();
                }
            }
            FileManagerApp.setBrowseMode(FileManagerApp.PAGE_CONTENT_SEARCH);
            mShowingSearchResults = true;
            mCurrentSearchedString = searchedString;
            mSearchedDirectory = searchedDirectory;
            // update mDisplayText here since it is used in the loading dialog
            createBreadCrumbPath(mSearchedDirectory.getAbsolutePath());
            int index = mDisplayText.lastIndexOf(BaseFileManagerFragment.DELIM);
            if (index > 0) {
                mDisplayText = mDisplayText.substring(index + DELIM.length());
            }

            //setupSearchViewMode();
            showLoadingDialog("");
        } else {
            FileManagerApp.log(TAG + "startSearchDirectory - searchedDirectory is null", true);
        }
    }

    private void stopSearchThread() {
        FileManagerApp.log(TAG + "stopSearchThread", false);
        if (mSearchDirectory != null) {
            synchronized (mDirectorySearchLock) {
                if (mSearchDirectory.isAlive()) {
                    FileManagerApp.log(TAG + "Sear thread still alive stop", false);
                    mSearchDirectory.requestStop();
                }
                mSearchDirectory = null;
            }
        }
        // dismiss the dialog if it is shown
        hideLoadingDialog();
    }

    public boolean isShowingSearchResults() {
        return mShowingSearchResults;
    }

    public File getSearchedDirectory() {
        return mSearchedDirectory;
    }

    public String getSearchedString() {
        return mCurrentSearchedString;
    }

    public File getSearchModeTopDirectory() {
        return mSearchModeTopDirectory;
    }

    public void setSearchModeTopDirectory(File dir) {
        mSearchModeTopDirectory = dir;
    }

    private void setupSearchViewMode() {
        if (mContext != null) {
            ((MainFileManagerActivity) mContext).disableViewModeSelection();
        }
        // Force search results to be in list view
        if (mFileManagerApp.getViewMode() != FileManagerApp.LISTVIEW) {
            BaseFileManagerFragment rightFragment =
                    (BaseFileManagerFragment) getFragmentManager().findFragmentById(R.id.details);
            if (rightFragment != null) {
                try {
                    // Force View Mode to LISTVIEW
                    rightFragment.switchView(FileManagerApp.LISTVIEW, false);
                } catch (Exception e) {
                    FileManagerApp.log(TAG + "failed to set view mode", true);
                    return;
                }
            }
        }
    }

    @Override
    protected boolean onDragStarted(DragEvent event) {
        int itemCount = mFileListView.getChildCount();
        // Lazily initialize the height of our list items
        if (itemCount > 0 && mDragItemHeight < 0) {
            mDragItemHeight = mFileListView.getChildAt(0).getHeight();
        }
        return true;
    }

    @Override
    protected void onDragEnded() {
        super.onDragEnded();
        exitMultiSelect();
    }

    //Highlight green the selected items in left for drag and drop, so user knows he cannot open it
    public void updatehighlightSourceLeft() {
        ArrayList<String> pasteFiles = FileManagerApp.getPasteFiles();
        try {
            for (int i = 0; i < pasteFiles.size(); i++) {
                int pos = 0;
                for (IconifiedText item : mFileListAdapter.getItems()) {
                    if (pasteFiles.get(i).equals(item.getPathInfo())) {
                        mFileListAdapter.setItemChecked(pos);
                    }
                    pos++;
                }
            }
        } catch (Exception e) {
            return;
        }
    }

    public void setNextHighlightItemPos(IconifiedText item) {
        int pos = INVALID_POS;

        IconifiedTextListAdapter adapter = (IconifiedTextListAdapter) mFileListView.getAdapter();
        if (adapter != null) {
            int itemCount = adapter.getCount();
            if (itemCount > 1) {
                pos = adapter.getPosition(item) + 1;
                if (pos >= adapter.getCount()) {
                    pos = 0;
                }
            }
            mNextItemPos = pos;
        }
    }
}
