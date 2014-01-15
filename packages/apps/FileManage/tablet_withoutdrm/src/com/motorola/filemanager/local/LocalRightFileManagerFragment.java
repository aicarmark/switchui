/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR              Author      Description
 * 2011-10-03   IKTABLETMAIN-348    XQH748      initial
 */
package com.motorola.filemanager.local;

import java.io.File;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;
import com.motorola.filemanager.local.search.database.SearchContentDatabase;
import com.motorola.filemanager.ui.CustomDialogFragment;
import com.motorola.filemanager.ui.IconifiedText;
import com.motorola.filemanager.ui.IconifiedTextListAdapter;
import com.motorola.filemanager.utils.FileUtils;

public class LocalRightFileManagerFragment extends LocalFileOperationsFragment {
    private AbsListView mListView;
    private AbsListView mGridView;
    private Context mContext;
    //    private static final String SEARCHED_DIRECTORY = "searched_directory";

    private TextView mEmptyText;
    private TextView mCurrentFolder;

    private TextView mCurrentPath;
    private String mSdCardPath = "";
    private static String TAG = "LocalRightFileManagerFragment: ";
    private TableLayout mTablelayout;
    private RelativeLayout mDLayout = null;
    private RelativeLayout mLLyout = null;
    private RelativeLayout mTLayout = null;
    private RelativeLayout mSLayout = null;
    private ImageView mTView2 = null;
    private ImageView mTView3 = null;
    private String mRootDir = "/";

    //private View mTView4 = null;

    public LocalRightFileManagerFragment() {
        super(R.layout.content_file_list);
        // TODO Auto-generated constructor stub
    }

    //Adding to  nvalidateOptionsMenu, when we back from Detailed view.
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();
        if (mContext != null) {
            // First invalidateOptionsMenu of the home page as we will use a different options.
            ((Activity) mContext).invalidateOptionsMenu();
        }
        if (FileManagerApp.getCurrDir() != null) {
            if (FileManagerApp.getBrowseMode() == FileManagerApp.PAGE_CONTENT_SEARCH) {
                Fragment leftFragment = getFragmentManager().findFragmentById(R.id.home_page);
                if (leftFragment != null) {
                    if (leftFragment.getClass().getName().equals(LocalLeftFileManagerFragment.class
                            .getName())) {
                        if (((LocalLeftFileManagerFragment) leftFragment).isShowingSearchResults()) {
                            FileManagerApp.log(TAG +
                                    "forcing to return to list view in onActivityCreated", false);
                            switchView(FileManagerApp.LISTVIEW, true);
                            return;
                        }
                    }
                }
            }

            if (mFileManagerApp.getUserSelectedViewMode() != mFileManagerApp.getViewMode()) {
                // Possible scenario is when returning from detailed view popping while / after search
                switch (mFileManagerApp.getUserSelectedViewMode()) {
                    case FileManagerApp.LISTVIEW :
                        // Just do the switch to List View but no need to
                        // Browse inside the switchView statement since it
                        // will be performed by below code
                        FileManagerApp.log(TAG + "returning to list view ", false);
                        switchView(FileManagerApp.LISTVIEW, false);
                        break;
                    case FileManagerApp.GRIDVIEW :
                        // Just do the switch to Grid View but no need to
                        // Browse inside the switchView statement since it
                        // will be performed by below code
                        FileManagerApp.log(TAG + "returning to grid view ", false);
                        switchView(FileManagerApp.GRIDVIEW, false);
                        break;
                    case FileManagerApp.COLUMNVIEW :
                        FileManagerApp.log(TAG + "returning to column view ", false);
                        switchView(FileManagerApp.COLUMNVIEW, true);
                        return;
                }
            }
            browseTo(FileManagerApp.getCurrDir());
            showScreen();
        }
    }

    public static LocalRightFileManagerFragment newInstance() {
        LocalRightFileManagerFragment f = new LocalRightFileManagerFragment();
        return f;
    }

    private void setConfig(int orientation) {
        TypedValue outValue = new TypedValue();
        getResources().getValue(R.fraction.rel_layout_name, outValue, false);
        float weight_one = outValue.getFloat();
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            getResources().getValue(R.fraction.rel_weight_textinfod_port, outValue, false);
            float weight_date = outValue.getFloat();
            ((LinearLayout.LayoutParams) mDLayout.getLayoutParams()).weight = weight_date;

            getResources().getValue(R.fraction.rel_layout_name_por, outValue, false);
            weight_one = outValue.getFloat();
            ((LinearLayout.LayoutParams) mLLyout.getLayoutParams()).weight = weight_one;

            getResources().getValue(R.fraction.rel_weight_contextmenu_port, outValue, false);
            float weight_contextmenu = outValue.getFloat();
            ((LinearLayout.LayoutParams) mIconPlaceHolder.getLayoutParams()).weight =
                    weight_contextmenu;

            if (mGridView != null) {
                ((GridView) mGridView).setColumnWidth((int) getResources()
                        .getDimension(R.dimen.grid_view_width_port));
            }
            mSLayout.setVisibility(View.GONE);
            mTLayout.setVisibility(View.GONE);
            mTView2.setVisibility(View.GONE);
            mTView3.setVisibility(View.GONE);
            //mTView4.setVisibility(View.GONE);
        } else {
            getResources().getValue(R.fraction.rel_weight_textinfod, outValue, false);
            float weight_date = outValue.getFloat();

            ((LinearLayout.LayoutParams) mDLayout.getLayoutParams()).weight = weight_date;
            ((LinearLayout.LayoutParams) mLLyout.getLayoutParams()).weight = weight_one;

            getResources().getValue(R.fraction.rel_layout_contextmenu, outValue, false);
            float weight_contextmenu = outValue.getFloat();
            ((LinearLayout.LayoutParams) mIconPlaceHolder.getLayoutParams()).weight =
                    weight_contextmenu;

            if (mGridView != null) {
                ((GridView) mGridView).setColumnWidth((int) getResources()
                        .getDimension(R.dimen.grid_view_width));
            }

            mSLayout.setVisibility(View.VISIBLE);
            mTLayout.setVisibility(View.VISIBLE);
            mTView2.setVisibility(View.VISIBLE);
            mTView3.setVisibility(View.VISIBLE);
            //mTView4.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreateViewInit() {
        mTablelayout = (TableLayout) mContentView.findViewById(R.id.file_top);
        mDLayout = (RelativeLayout) mContentView.findViewById(R.id.textinfod);
        mLLyout = (RelativeLayout) mContentView.findViewById(R.id.textinfo);
        mSLayout = (RelativeLayout) mContentView.findViewById(R.id.textinfos);
        mTLayout = (RelativeLayout) mContentView.findViewById(R.id.textinfot);
        mIconPlaceHolder = mContentView.findViewById(R.id.placeholder_view1);
        if (isSomeFileSelected() || mFileManagerApp.mLaunchMode != FileManagerApp.NORMAL_MODE) {
            mIconPlaceHolder.setVisibility(View.GONE);
        } else {
            mIconPlaceHolder.setVisibility(View.VISIBLE);
        }
        mTView2 = (ImageView) mContentView.findViewById(R.id.view2);
        mTView3 = (ImageView) mContentView.findViewById(R.id.view3);
        //mTView4 = mContentView.findViewById(R.id.view4);
        //setConfig(getResources().getConfiguration().orientation);
        if (mFileManagerApp.getViewMode() == FileManagerApp.GRIDVIEW) {
            mTablelayout.setVisibility(View.GONE);
        }
    }

    public void handleOpen() {
        if (mFileListView.getAdapter() != null) {
            ArrayList<String> tempOpen =
                    ((IconifiedTextListAdapter) (mFileListView.getAdapter())).getSelectFiles();
            if (tempOpen.size() > 0) {
                String filename = tempOpen.get(0);
                File tempFile = new File(filename);
                if (tempFile.isDirectory()) {
                    browseTo(tempFile);
                } else {
                    openFile(tempFile);
                }
            }
        }
    }

    public void handleBrowse() {
        browseTo(FileManagerApp.getCurrDir());
    }

    public Dialog infoDialog = null;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setConfig(newConfig.orientation);

    }

    @Override
    public void onResume() {
        super.onResume();

        FileManagerApp.log(TAG + "onResume called", false);
        if (mFileListView.getAdapter() != null) {
            if (!(((IconifiedTextListAdapter) mFileListView.getAdapter()).isSelectMode())) {
                FileManagerApp.log(TAG + "onResume called, mCurrentContent ==nomulti", false);
                if ((progressInfoDialog != null) || (loadingDialog != null)) {
                    FileManagerApp.log(TAG +
                            "onResume called, operation on-going, do not refreshList", false);
                } else {
                    refreshList();
                }
            } else {
                FileManagerApp.log(TAG + "onResume called, mCurrentContent ==multi", false);
            }
        } else {
            FileManagerApp.log(TAG +
                    "onResume called, mCurrentContent ==PAGE_CONTENT_NORMAL ADAPTER blank", false);
            refreshList();
        }
    }

    public void initHomePage() {
        try {
            updateViewMode();
            mCurrentFolder = (TextView) mContentView.findViewById(R.id.current_folder);
            mCurrentPath = (TextView) mContentView.findViewById(R.id.path);
            setUpSorting(mNameSortIface,
                    R.id.textinfo,
                    R.id.folder_view_icon,
                    FileManagerApp.NAMESORT);

            setUpSorting(mDateSortIface,
                    R.id.textinfod,
                    R.id.date_view_icon,
                    FileManagerApp.DATESORT);

            setUpSorting(mSizeSortIface,
                    R.id.textinfos,
                    R.id.size_view_icon,
                    FileManagerApp.SIZESORT);

            setUpSorting(mTypeSortIface,
                    R.id.textinfot,
                    R.id.type_view_icon,
                    FileManagerApp.TYPESORT);

            mFileListView.setEmptyView(mFileListView.findViewById(android.R.id.empty));
            mFileListView.setTextFilterEnabled(true);

            File browseto = new File(mRootDir);

            mSdCardPath = mFileManagerApp.getPhoneStoragePath();
            if (!TextUtils.isEmpty(mSdCardPath)) {
                browseto = new File(mSdCardPath);
            }
            checkStorageState();

            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SHARED);
            intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            intentFilter.addDataScheme("file");
            getActivity().registerReceiver(mSDCardReceiver, intentFilter);

            ActionBar actionBar = getActivity().getActionBar();
            if (actionBar != null) {
                mFileListView.setSystemUiVisibility(View.STATUS_BAR_VISIBLE);
                actionBar.show();
            }

            Intent theIntent = getActivity().getIntent();
            FileManagerApp.log(TAG + "onCreate received intent=" + theIntent, false);
            if (Intent.ACTION_SEARCH.equals(theIntent.getAction())) {
                FileManagerApp.log(TAG + "onCreate action is ACTION_SEARCH ", false);
                /*                String searchedString = theIntent.getStringExtra(SearchManager.QUERY);
                                Bundle appData = theIntent.getBundleExtra(SearchManager.APP_DATA);
                                if (appData != null) {
                                    m_FirstSearchedDirectory = appData.getString(SEARCHED_DIRECTORY);
                                    if (m_FirstSearchedDirectory == null) {
                                        if (MainFileManagerActivity.getEMMCEnabled()) {
                                            // Comment out since (MotoEnvironment is not supported
                                            // if
                                            // (MotoEnvironment.getExternalAltStorageState().equals(Environment.MEDIA_MOUNTED)){
                                            if (false) {// This case will not be executed
                                                // FileManagerApp.log(TAG
                                                // +"getExternalAltStorageState() returns" +
                                                // MotoEnvironment.getExternalAltStorageState());
                                                gSearchString = searchedString;
                                                showSearchOptionDialog();
                                                FileManagerApp.log(TAG + "Search option is" +
                                                        FileManagerApp.getSearchOption());
                                                if (FileManagerApp.getSearchOption() == FileManagerApp.INTERNALSTORAGE) {
                                                    m_FirstSearchedDirectory = FileManagerApp.SD_CARD_DIR;
                                                } else {
                                                    m_FirstSearchedDirectory = FileManagerApp.SD_CARD_EXT_DIR;
                                                }
                                            } else {
                                                m_FirstSearchedDirectory = FileManagerApp.SD_CARD_DIR;
                                                (new AlertDialog.Builder(mContext)).setTitle(R.string.sd_card)
                                                        .setIcon(android.R.drawable.ic_dialog_alert).setMessage(
                                                                R.string.sd_missing_search).setNeutralButton(
                                                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                                                    public void onClick(DialogInterface dialog,
                                                                                        int which) {
                                                                    }
                                                                }).create().show();
                                            }
                                        } else {
                                            m_FirstSearchedDirectory = FileManagerApp.SD_CARD_DIR;
                                        }
                                    }
                                    File currentDirectory = new File(m_FirstSearchedDirectory);
                                    FileManagerApp.setCurrDir(currentDirectory);
                                } else {
                                    m_FirstSearchedDirectory = FileManagerApp.SD_CARD_DIR;
                                    File currentDirectory = new File(m_FirstSearchedDirectory);
                                    FileManagerApp.setCurrDir(currentDirectory);
                                }

                                startSearchDirectory(FileManagerApp.getCurrDir(), searchedString);*/
            } else if (Intent.ACTION_VIEW.equals(theIntent.getAction())) {
                Uri uri = getActivity().getIntent().getData();
                FileManagerApp.log(TAG + "intent is ACTION_VIEW " + "uri=" + uri, false);
                Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
                if (cursor == null) {
                    FileManagerApp.log(TAG + "intent is ACTION_VIEW " + "cursor is null", true);
                    getActivity().finish();
                } else {
                    cursor.moveToFirst();

                    cursor.getColumnIndexOrThrow(SearchContentDatabase.KEY_FOLDER);
                    int dIndex = cursor.getColumnIndexOrThrow(SearchContentDatabase.KEY_FULL_PATH);
                    browseto = new File(cursor.getString(dIndex));
                    if (browseto.isFile()) {
                        FileManagerApp.log(TAG + "opening file " + browseto, false);
                        openFile(browseto);
                        getActivity().finish();
                    } else {
                        FileManagerApp.log(TAG + "browse folder " + browseto, false);
                        FileManagerApp.setCurrDir(browseto);
                        browseTo(browseto);
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
            } else {
                if (!TextUtils.isEmpty(browseto.getName())) {
                    browseTo(browseto);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

    }

    @Override
    public void initFileListView() {
        mContext = this.getActivity();
        mListView = (AbsListView) mContentView.findViewById(R.id.file_list);
        mGridView = (AbsListView) mContentView.findViewById(R.id.file_grid);
        mEmptyText = (TextView) mContentView.findViewById(R.id.empty_text);
        initHomePage();
        mListView.setOnDragListener(this);
        mGridView.setOnDragListener(this);
        mListView.setFocusable(true);
        mListView.setFocusableInTouchMode(true);
        mGridView.setFocusable(true);
        mGridView.setFocusableInTouchMode(true);
        mListView.setOnItemClickListener(fileListListener);
        mListView.setOnItemLongClickListener(fileListListener2);
        mGridView.setOnItemClickListener(fileListListener);
        mGridView.setOnItemLongClickListener(fileListListener2);
        setConfig(getResources().getConfiguration().orientation);
    }

    /*    @Override
        public void openClickedFile(String fileAbsolutePath, boolean isTitleFragEmpty, int pos) {
            super.openClickedFile(fileAbsolutePath, isTitleFragEmpty, pos);
        }*/

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        try {
            String action = intent.getAction();
            FileManagerApp.log(TAG + "onNewIntent action= " + action, false);
            if (action == null) {
                return;
            }
            if (action.equals(MainFileManagerActivity.ACTION_RESTART_ACTIVITY)) {
                updateViewMode();
                FileManagerApp.log(TAG + "onNewIntent " + mFileManagerApp.getPhoneStoragePath(),
                        false);
                checkStorageState();
                browseTo(new File(mFileManagerApp.getPhoneStoragePath()));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private void checkStorageState() {
        if (FileManagerApp.getStorageMediumMode() == FileManagerApp.INDEX_INTERNAL_MEMORY) {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_SHARED)) {
                    mEmptyText.setText(R.string.sd_is_used_by_other);
                } else {
                    mEmptyText.setText(R.string.sd_missing);
                }
            }
        } else if (FileManagerApp.getStorageMediumMode() == FileManagerApp.INDEX_INTERNAL_MEMORY) {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_SHARED)) {
                    mEmptyText.setText(R.string.sd_is_used_by_other);
                } else {
                    mEmptyText.setText(R.string.sd_missing);
                }
            }
        }
    }

    @Override
    protected boolean rename(File oldFile, File newFile, boolean isContext) {
        int toast = 0;
        boolean result = false;
        if (newFile.exists()) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            DialogFragment newFragment =
                    CustomDialogFragment.newInstance(CustomDialogFragment.DIALOG_RENAME_EXIST,
                            isContext,
                            mContext,
                            newFile.getName());
            // Show the dialog.
            newFragment.show(ft, "dialog");
        } else {
            if (oldFile.renameTo(newFile)) {
                if (FileManagerApp.getCurrDir().exists()) {
                    refreshList();
                } else {
                    browseTo(newFile);
                }

                if (newFile.isDirectory()) {
                    toast = R.string.folder_renamed;
                    //for folder renamed, need to check if it affect any shortcut
                    //and update shortcut if changed.
                    checkImpactForShortcut(oldFile, newFile);
                } else {
                    toast = R.string.file_renamed;
                }
                //for both folder or file, need to check if its rename change impacts
                //recent file path, if so, below function will update recent file
                checkImpactForRecentFile(oldFile, newFile);
                result = true;
            } else {
                if (oldFile.isDirectory()) {
                    toast = R.string.error_renaming_folder;
                } else {
                    toast = R.string.error_renaming_file;
                }
            }
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
            if (mCurrentActionMode != null) {
                mCurrentActionMode.finish();
            }
        }
        return result;
    }
    private final BroadcastReceiver mSDCardReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_SHARED)) {
                if (!getActivity().isFinishing()) {
                    mEmptyText.setText(R.string.sd_is_used_by_other);
                    refreshList();
                }
            } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                if (!getActivity().isFinishing()) {
                    mEmptyText.setText(R.string.this_folder_is_empty);
                    refreshList();
                }
            } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                if (!getActivity().isFinishing()) {
                    if (FileManagerApp.getStorageMediumMode() == FileManagerApp.INDEX_INTERNAL_MEMORY) {
                        mEmptyText.setText(R.string.internal_device_storage_unmounted);
                    } else if (FileManagerApp.getStorageMediumMode() == FileManagerApp.INDEX_SD_CARD) {
                        mEmptyText.setText(R.string.sd_unmounted);
                    }
                    refreshList();
                }
            }
        }
    };

    @Override
    protected void handleMessage(Message message) {
        switch (message.what) {
            case MESSAGE_SHOW_DIRECTORY_CONTENTS :
                hideLoadingDialog();
                if (message.obj != null) {
                    FileManagerApp.log(TAG + "obj not null", false);
                    showDirectoryContents((DirectoryContents) message.obj);
                }
                break;
            case MESSAGE_SET_LOADING_PROGRESS :
                try {
                    showLoadingDialog(getString(R.string.loading));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case MESSAGE_REFRESH :
                refreshList();
                break;
            case MESSAGE_CANCEL_PROGRESS :
                hideLoadingDialog();
                break;
            case MESSAGE_SET_DELETING_PROGRESS :
                showLoadingDialog(getString(R.string.deleting));
                break;
            case MESSAGE_ACTION_MODE_DESTROY :
                mIconPlaceHolder.setVisibility(View.VISIBLE);
                break;
            default :
                super.handleMessage(message);
                break;
        }
    }

    @Override
    protected IconifiedText getIconifiedTextItem(int pos) {
        if (mFileListAdapter != null) {
            return (IconifiedText) mFileListAdapter.getItem(pos);
        } else {
            return null;
        }
    }

    public boolean isListNotEmpty() {
        if ((mFileListView.getAdapter() != null) && (mFileListView.getAdapter().getCount() > 0)) {
            return true;
        }
        return false;
    }

    @Override
    protected void showDirectoryContents(DirectoryContents contents) {
        try {
            if (contents != null) {
                super.showDirectoryContents(contents);
                mFileListView.setAdapter(mFileListAdapter);
                mFileListView.setTextFilterEnabled(true);

                if (!mListDir.isEmpty() || !mListFile.isEmpty()) {
                    mEmptyText.setVisibility(View.GONE);
                    setUpViewSorting(FileManagerApp.NAMESORT);
                    if (mFileManagerApp.getViewMode() == FileManagerApp.LISTVIEW) {
                        setUpSortArrows();
                    } else if (mFileManagerApp.getViewMode() == FileManagerApp.GRIDVIEW) {
                        ((Activity) mContext).invalidateOptionsMenu();
                    }

                } else {
                    mEmptyText.setVisibility(View.VISIBLE);
                    mFileManagerApp.clearSortFlags();
                    hideSortIcon();
                }

                createBreadCrumbPath(FileManagerApp.getCurrDir().getAbsolutePath());
                if (mDisplayText != null) {
                    mCurrentPath.setText(mDisplayText);
                }
                mEmptyText.setText(R.string.this_folder_is_empty);

                if (mFileManagerApp.mLaunchMode == FileManagerApp.SELECT_FILE_MODE) {
                    mCurrentFolder.setText(R.string.select_file);
                } else if (mFileManagerApp.mLaunchMode == FileManagerApp.SELECT_FOLDER_MODE) {
                    mCurrentFolder.setText(R.string.select_folder);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDestroy() {
        FileManagerApp.log(TAG + "ondestroy called", false);
        // Stop the scanner.
        stopActivityThread();
        getActivity().unregisterReceiver(mSDCardReceiver);
        super.onDestroy();
    }

    private void updateViewMode() {
        if (mFileManagerApp.getViewMode() == FileManagerApp.GRIDVIEW) {
            mListView.setVisibility(View.GONE);
            mFileListView = mGridView;
            MainFileManagerActivity.setViewType(1);
        } else {
            mGridView.setVisibility(View.GONE);
            mFileListView = mListView;
            MainFileManagerActivity.setViewType(0);
        }
        mFileListView.setVisibility(View.VISIBLE);
    }

    @Override
    public void switchView(int mode, boolean rebrowse) {
        try {
            mFileManagerApp.setViewMode(mode);
        } catch (Exception e) {
            FileManagerApp.log(TAG + "failed to set view mode", true);
            return;
        }
        if (mode == FileManagerApp.COLUMNVIEW) {
            updateColumnFragment();
        } else {
            setUpViewSorting(mFileManagerApp.getSortMode());
            if (mode == FileManagerApp.LISTVIEW) {
                mFileListView.setVisibility(View.GONE);
                mGridView.setVisibility(View.GONE);
                mContentView.findViewById(R.id.file_top).setVisibility(View.VISIBLE);
                mFileListView = mListView;
                mGridView.setAdapter(null);
                setUpSortArrows();

            } else {
                mFileListView.setVisibility(View.GONE);
                mListView.setVisibility(View.GONE);
                mContentView.findViewById(R.id.file_top).setVisibility(View.GONE);
                mFileListView = mGridView;
                mListView.setAdapter(null);
            }
            mFileListView.setVisibility(View.VISIBLE);
            mFileListView.setAdapter(mFileListAdapter);
            mFileListView.setEmptyView(mFileListView.findViewById(android.R.id.empty));
            mFileListView.setTextFilterEnabled(true);

        }
    }

    @Override
    protected void browseTo(final File aDirectory) {
        super.browseTo(aDirectory);
    }

    @Override
    public boolean upOneLevel() {
        try {
            mFileManagerApp.clearSortFlags();
            hideSortIcon();
            if (FileManagerApp.getCurrDir() != null) {
                File prevDirectory = FileManagerApp.getCurrDir().getParentFile();

                if (prevDirectory == null) {
                    getActivity().finish();
                    return true;
                } else {
                    if (FileUtils.isRootDirectory(prevDirectory.getAbsolutePath())) {
                        getActivity().finish();
                        return false;
                    } else {
                        FileManagerApp.setCurrDir(prevDirectory);
                        browseTo(prevDirectory);
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            FileManagerApp.log("Exception recieved in upOneLevel", true);
            e.printStackTrace();
            return false;
        }
    }

    private void setUpSortArrows() {
        hideSortIcon();
        ImageView icon = null;
        boolean isDescending = false;

        switch (mFileManagerApp.getSortMode()) {
            case FileManagerApp.NAMESORT :
                icon = (ImageView) mContentView.findViewById(R.id.folder_view_icon);
                if (mFileManagerApp.mIsDescendingName) {
                    isDescending = true;
                }

                break;
            case FileManagerApp.TYPESORT :
                icon = (ImageView) mContentView.findViewById(R.id.type_view_icon);
                if (mFileManagerApp.mIsDescendingType) {
                    isDescending = true;
                }
                break;

            case FileManagerApp.DATESORT :
                icon = (ImageView) mContentView.findViewById(R.id.date_view_icon);
                if (mFileManagerApp.mIsDescendingDate) {
                    isDescending = true;
                }
                break;

            case FileManagerApp.SIZESORT :
                icon = (ImageView) mContentView.findViewById(R.id.size_view_icon);
                if (mFileManagerApp.mIsDescendingSize) {
                    isDescending = true;
                }
                break;
            default :
                break;
        }
        if (icon != null) {
            icon.setVisibility(View.VISIBLE);

            if (isDescending) {
                icon.setImageResource(android.R.drawable.arrow_down_float);
            } else {
                icon.setImageResource(android.R.drawable.arrow_up_float);
            }
        }
    }

    @Override
    protected void stopActivityThread() {
        FileManagerApp.log(TAG + "stopActivityThread", false);
        super.stopActivityThread();

/*        if (mZip != null) {
            mZip = null;
        }
        if (mUnzip != null) {
            mUnzip = null;
        }*/
        mEmptyText.setVisibility(View.GONE);
        hideLoadingDialog();
    }

    @Override
    public void blankScreen() {
        mTablelayout.setVisibility(View.INVISIBLE);
        mFileListView.setVisibility(View.INVISIBLE);
        mCurrentPath.setVisibility(View.INVISIBLE);
    }

    @Override
    public void showScreen() {
        if (mFileManagerApp.getViewMode() == FileManagerApp.LISTVIEW) {
            mTablelayout.setVisibility(View.VISIBLE);
        }
        mFileListView.setVisibility(View.VISIBLE);
        mCurrentPath.setVisibility(View.VISIBLE);
    }

    /*    private DialogInterface.OnClickListener searchOptionDialogListener =
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        FileManagerApp.log(TAG + "searchOptionDialogListener set item=" + item);
                    }
                };

        private void showSearchOptionDialog() {
            String[] parts = this.getResources().getStringArray(R.array.search_storage_selector);
            AlertDialog dialog =
                    createSearchChoiceDialog(R.string.search_folder, parts, FileManagerApp
                            .getSearchOption(), searchOptionDialogListener);
            dialog.show();
        }

        private AlertDialog createSearchChoiceDialog(int titleId, CharSequence[] items,
                                                     int checkedItem,
                                                     DialogInterface.OnClickListener listener) {
            AlertDialog dialog = null;
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(MainFileManagerActivity.getCurContext());
            if (titleId > 0) {
                builder.setTitle(titleId);
            }
            builder.setSingleChoiceItems(items, checkedItem, listener);
            builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (FileManagerApp.getSearchOption() == FileManagerApp.INTERNALSTORAGE) {
                        m_FirstSearchedDirectory = FileManagerApp.SD_CARD_DIR;
                        FileManagerApp.log(TAG +
                                "In createSearchChoiceDialog get Search option is in internal storage");
                    } else {
                        m_FirstSearchedDirectory = FileManagerApp.SD_CARD_EXT_DIR;
                        FileManagerApp.log(TAG +
                                "In createSearchChoiceDialog get Search option is in sd card");
                    }
                    FileManagerApp.log(TAG +
                            "In createSearchChoiceDialog m_FirstSearchedDirectory is: " +
                            m_FirstSearchedDirectory);
                    File currentDirectory = new File(m_FirstSearchedDirectory);
                    FileManagerApp.setCurrDir(currentDirectory);
                    startSearchDirectory(FileManagerApp.getCurrDir(), gSearchString);
                    FileManagerApp.log(TAG + "In createSearchChoiceDialog" + gSearchString);
                    gSearchString = null;
                    dialog.dismiss();
                }
            });
            dialog = builder.create();
            return dialog;
        }*/

}
