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
package com.motorola.filemanager;

import java.io.File;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.motorola.filemanager.local.LocalColumnViewFrameLeftFragment;
import com.motorola.filemanager.local.LocalDetailedInfoFileManagerFragment;
import com.motorola.filemanager.local.LocalLeftFileManagerFragment;
import com.motorola.filemanager.local.LocalRightFileManagerFragment;
//import com.motorola.filemanager.local.threads.Zip;
import com.motorola.filemanager.samba.RemoteContentFragment;
import com.motorola.filemanager.samba.RemoteContentLeftFragment;
import com.motorola.filemanager.samba.RemoteDetailedInfoFileManagerFragment;
import com.motorola.filemanager.samba.SambaExplorer;
import com.motorola.filemanager.samba.service.SambaTransferHandler;
import com.motorola.filemanager.ui.CustomDialogFragment;
import com.motorola.filemanager.ui.IconifiedText;
import com.motorola.filemanager.ui.IconifiedTextView;
import com.motorola.filemanager.ui.ShadowBuilder;
import com.motorola.filemanager.utils.FileUtils;

public class MainFileManagerActivity extends Activity implements OnDragListener, OnTouchListener {
    private static String TAG = "MainFileManagerActivity ";
    public final static String ACTION_COPY = "com.mot.FileManagerIntents.ACTION_COPY";
    public final static String ACTION_PASTE = "com.mot.FileManagerIntents.ACTION_PASTE";
    public final static String ACTION_CUT = "com.mot.FileManagerIntents.ACTION_CUT";
    public final static String ACTION_SHOWHOMEPAGE =
            "com.mot.FileManagerIntents.ACTION_SHOWHOMEPAGE";
    public final static String ACTION_SHOWDOWNLOADPAGE =
            "com.mot.FileManagerIntents.ACTION_SHOWDOWNLOADPAGE";
    public final static String ACTION_RESTART_ACTIVITY =
            "com.mot.FileManagerIntents.ACTION_RESTART_ACTIVITY";
    public final static String ACTION_MOVEFINISHED =
            "com.mot.FileManagerIntents.ACTION_MOVEFINISHED";
    public final static String ACTION_SELECTFILE = "com.mot.FileManagerIntents.ACTION_SELECTFILE";
    public final static String ACTION_SELECTFOLDER =
            "com.mot.FileManagerIntents.ACTION_SELECTFOLDER";
    // USB Host
    public final static String ACTION_USBDISKMOUNTED =
            "com.motorola.intent.action.USB_DISK_MOUNTED_NOTIFY";
    // USB Host End

    public final static String ACTION_TRANSFERFINISHED =
            "com.mot.FileManagerIntents.ACTION_TRANSFERFINISHED";
    public final static String EXTRA_KEY_TRANSFERRESULT = "TRANSFERRESULT";
    public final static String EXTRA_KEY_TRANSFERREDNUM = "TRANSFERREDNUM";

    public final static String EXTRA_KEY_PASTEREASON = "PASTEREASON";
    public final static String EXTRA_VAL_REASONCOPY = "REASONCOPY";
    public final static String EXTRA_VAL_REASONCUT = "REASONCUT";

    public final static String EXTRA_KEY_COPYRESULT = "COPYRESULT";
    public final static String EXTRA_VAL_COPYSUCESSED = "COPYSUCESSED";
    public final static String EXTRA_VAL_COPYFAILED = "COPYFAILED";

    public final static String EXTRA_KEY_SOURCEFILENAME = "SOURCEFILENAME";
    public final static String EXTRA_KEY_DESTFILENAME = "DESTFILENAME";
    public final static String EXTRA_KEY_HOMESYNC_TITLE = "HOMESYNC_TITLE";
    public final static String EXTRA_KEY_HOMESYNC_STEP = "HOMESYNC_STEP";

    public static final String EXTRA_KEY_DISKNUMBER = "DISK_NUMBER";
    public static final String EXTRA_KEY_CURRENT_USBPATH = "CURRENT_USBPATH";
    public static final String EXTRA_KEY_DISK[] = new String[]{"DISK1", "DISK2", "DISK3"};

    public static final String HIDEUNKOWNSETTING = "HideUnkownSetting";
    private Context mContext = null;
    private Activity mInstance = null;
    public static boolean eMMCEnabled = false;

    private MenuItem mListview;
    private MenuItem mGridview;
    private MenuItem mColumnview;
    private MenuItem mViewitem;
    private MenuItem mCancelbutton;
    private MenuItem mPastebutton;
    private MenuItem mPlacebutton;
    private MenuItem mNewfolder;
    private MenuItem mMultiselect;
    private MenuItem mSearchbutton;
    private MenuItem mSortItem;
    private MenuItem mSortName;
    private MenuItem mSortType;
    private MenuItem mSortDate;
    private MenuItem mSortSize;
    private MenuItem mCopybutton;
    private MenuItem mMovebutton;
    private MenuItem mRenamebutton;
    private MenuItem mCompressbutton;
    private MenuItem mPrintbutton;
    private MenuItem mDeletebutton;

    private LinearLayout mLLyout;
    private LinearLayout mCLyout;
    public final static String EXTRA_KEY_RESULT = "FILEPATH";
    public final static String EXTRA_KEY_AUTHINFO = "AUTHINFO";
    private ActionBar mActionbar;
    public SearchView mSearchView = null;
    private ArrayList<Integer> mSearchFragments = new ArrayList<Integer>();
    private TextView mActionBarTitle = null;
    protected Handler mCurrentHandler = null;

    // Drag and Drop variables
    protected ClipData mData;
    protected ShadowBuilder mShadow;
    protected static final boolean DEBUG_DRAG_DROP = false; // MUST NOT SUBMIT SET TO TRUE
    private static final int NO_DROP_TARGET = -1;
    // Total height of the top and bottom scroll zones, in pixels
    protected static final int SCROLL_ZONE_SIZE = 64;
    // The amount of time to scroll by one pixel, in ms
    protected static final int SCROLL_SPEED = 4;
    protected static final int DRAG_LOCATION_OFFSET = 12;
    protected static final int IS_DRAG_OFFSET = 4;
    protected static final int RAW_TOUCH_OFFSET = -999;// Initalize offset
    // True if a drag is currently in progress
    protected boolean mDragInProgress = false;
    // The adapter position that the user's finger is hovering over
    protected int mDropTargetAdapterPosition = NO_DROP_TARGET;
    // The target view the user's finger is hovering over
    protected IconifiedTextView mDropTargetView;
    public int mDragItemHeight = -1;
    protected int prevrawTouchY = -1;
    protected boolean mTargetScrolling;
    final static public int MESSAGE_SELECT_DRAG_LOCATION = 522;
    protected boolean mIsDragMode = false;
    protected boolean mIsDropSuccessful = false;
    protected FileManagerApp mFileManagerApp = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFileManagerApp = (FileManagerApp) getApplication();
        setContentView(R.layout.main_file_manager);
        mLLyout = (LinearLayout) findViewById(R.id.home_page);
        mCLyout = (LinearLayout) findViewById(R.id.details);
        mLLyout.setOnDragListener(this);
        mFileManagerApp.setPhoneStoragePath(FileManagerApp.INDEX_INTERNAL_MEMORY, -1);
        mContext = this;
        mInstance = this;
        setConfig(getResources().getConfiguration().orientation);
        HomePageLeftFileManagerFragment df =
                new HomePageLeftFileManagerFragment(R.layout.homepage_expandlist, false);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.home_page, df);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
        mActionbar = getActionBar();

        if (mActionbar != null) {

            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

            mActionbar.setDisplayHomeAsUpEnabled(true);
            mActionbar.setDisplayShowCustomEnabled(true);
            mActionbar.setDisplayShowTitleEnabled(false);

            View customView =
                    ((Activity) mContext).getLayoutInflater().inflate(
                            R.layout.action_bar_custom_view, null);
            mActionbar.setCustomView(customView);
            mSearchView = (SearchView) customView.findViewById(R.id.search_view);
            mActionBarTitle = (TextView) customView.findViewById(R.id.action_bar_title);
            if (mSearchView != null) {
                mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
                mSearchView.setSubmitButtonEnabled(true);
                mSearchView.setVisibility(View.GONE);
                mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {

                    public boolean onClose() {
                        hideSearchWidget();
                        // Clear the search text when user close the widget.
                        mSearchView.setQuery(null, false);
                        return true;
                    }
                });
            }
            if (mActionBarTitle != null) {
                mActionBarTitle.setText(R.string.select_file);
                mActionBarTitle.setVisibility(View.VISIBLE);
            }
        }

        initActivity();
        IntentFilter iFilt = new IntentFilter();
        iFilt.addAction(MainFileManagerActivity.ACTION_USBDISKMOUNTED);
        registerReceiver(mBroadCaseReveiver, iFilt);

        StorageManager storageManager = null;
         storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
         if (storageManager != null) {
             storageManager.registerListener(mStorageEventListener);
         }
        mCurrentHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                MainFileManagerActivity.this.handleMessage(msg);
            }
        };
    }

    private void handleMessage(Message message) {
        switch (message.what) {
            case FileManagerApp.MESSAGE_SHORTCUT_REMOVED : {
                Fragment leftFragment = getFragmentManager().findFragmentById(R.id.home_page);
                if (leftFragment != null) {
                    if (leftFragment.getClass().getName().equals(
                            HomePageLeftFileManagerFragment.class.getName())) {
                        ((HomePageLeftFileManagerFragment) leftFragment).updateShortcuts();
                        FileManagerApp.log(
                                TAG + " received and processed MESSAGE_SHORTCUT_REMOVED", false);
                    }
                } else {
                    FileManagerApp
                            .log( TAG +
                                        " received MESSAGE_SHORTCUT_REMOVED, didnt process because left view doesnt display shortcuts",
                                    false);
                }
                break;
            }
        }
    }

    StorageEventListener mStorageEventListener = new StorageEventListener() {
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            String currentPath = mFileManagerApp.getPhoneStoragePath();
            if ((path != null) && (oldState != null) && (newState != null)) {
                FileManagerApp.log("Received storage state changed notification that " + path +
                        " changed state from " + oldState + " to " + newState, false);
                if (newState.equals(Environment.MEDIA_MOUNTED) ||
                        newState.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                    if (FileManagerApp.isHomePage()) {
                        FileManagerApp.log("in  onStorageStateChanged SD mount init home page",
                                false);
                        HomePageLeftFileManagerFragment tit =
                                (HomePageLeftFileManagerFragment) getFragmentManager()
                                        .findFragmentById(R.id.home_page);
                        if (tit != null) {
                            tit.initHomePage();
                        }
                    }
                }
                if (!newState.equals(Environment.MEDIA_MOUNTED) &&
                        !newState.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                    if (FileManagerApp.isHomePage() && path.equals(currentPath)) {
                        FileManagerApp.log("in  onStorageStateChanged SD not mount init home page",
                                false);
                        HomePageLeftFileManagerFragment tit =
                                (HomePageLeftFileManagerFragment) getFragmentManager()
                                        .findFragmentById(R.id.home_page);
                        if (tit != null) {
                            tit.initHomePage();
                            if (mFileManagerApp.SD_CARD_DIR.length() != 0) {
                                mFileManagerApp.setPhoneStoragePath(mFileManagerApp.SD_CARD_DIR);
                            }
                        }
                        LocalRightFileManagerFragment det =
                                (LocalRightFileManagerFragment) getFragmentManager()
                                        .findFragmentById(R.id.details);
                        if (det != null) {
                            det.initHomePage();
                        }
                    }
                }
            }
        }
    };

    private void setConfig(int orientation) {
        TypedValue outValue = new TypedValue();
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            getResources().getValue(R.fraction.port_size_tf, outValue, false);
            float weight_port_tf = outValue.getFloat();
            getResources().getValue(R.fraction.port_size_cf, outValue, false);
            float weight_port_cf = outValue.getFloat();
            ((LinearLayout.LayoutParams) mLLyout.getLayoutParams()).weight = weight_port_tf;
            ((LinearLayout.LayoutParams) mCLyout.getLayoutParams()).weight = weight_port_cf;
        } else {
            getResources().getValue(R.fraction.land_size_tf, outValue, false);
            float weight_land_tf = outValue.getFloat();
            getResources().getValue(R.fraction.land_size_cf, outValue, false);
            float weight_land_cf = outValue.getFloat();
            ((LinearLayout.LayoutParams) mLLyout.getLayoutParams()).weight = weight_land_tf;
            ((LinearLayout.LayoutParams) mCLyout.getLayoutParams()).weight = weight_land_cf;
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBroadCaseReveiver);
        SambaTransferHandler.setUserAuth(null);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setConfig(newConfig.orientation);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Fragment rightFragment = getFragmentManager().findFragmentById(R.id.details);
            Fragment leftFragment = getFragmentManager().findFragmentById(R.id.home_page);

            //If remote detailed view just return super.onKeyDown as we poping up
            if (rightFragment != null) {
                if ((rightFragment.getClass().getName()
                        .equals(RemoteDetailedInfoFileManagerFragment.class.getName()))) {
                    return super.onKeyDown(keyCode, event);
                }
            }

            if (FileManagerApp.getBrowseMode() == FileManagerApp.PAGE_CONTENT_SEARCH) {
                boolean result = true;
                if (mFileManagerApp.getViewMode() == FileManagerApp.COLUMNVIEW) {
                    if (rightFragment != null) {
                        if (rightFragment.getClass().getName().equals(
                                LocalColumnViewFrameLeftFragment.class.getName())) {
                            result =
                                    ((LocalColumnViewFrameLeftFragment) rightFragment).upOneLevel();
                        }
                    }
                } else {
                    if (leftFragment != null) {
                        if (leftFragment.getClass().getName().equals(
                                LocalLeftFileManagerFragment.class.getName())) {
                            result = ((LocalLeftFileManagerFragment) leftFragment).upOneLevel();
                        }
                    }
                }
                if (result == false) {
                    FileManagerApp.log(TAG + "search upOneLevel returned false", false);
                    // User is at top level of fragment that started after search. Need to pop
                    if (!mSearchFragments.isEmpty()) {
                        FileManagerApp.log(TAG + "search popping one backstack off ", false);
                        getFragmentManager().popBackStack(
                                mSearchFragments.get(mSearchFragments.size() - 1),
                                FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        mSearchFragments.remove(mSearchFragments.size() - 1);
                    }
                }
                return true;
            }

            //For detailed view we want the super class handler,as we are poping back the stack
            if (rightFragment != null) {
                if (!(rightFragment.getClass().getName()
                            .equals(LocalDetailedInfoFileManagerFragment.class.getName()))) {

                    if (leftFragment != null) {
                        if (leftFragment.getClass().getName().equals(
                                RemoteContentLeftFragment.class.getName())) {
                            RemoteContentLeftFragment det = (RemoteContentLeftFragment) leftFragment;
                            if (det != null) {
                                det.upOneLevel();
                            }
                        } else {
                            ((BaseFileManagerFragment) leftFragment).upOneLevel();
                        }
                    }
                    //Fragment rightFragment = getFragmentManager().findFragmentById(R.id.details);
                    if (rightFragment.getClass().getName()
                            .equals(RemoteContentFragment.class.getName())) {
                        RemoteContentFragment det = (RemoteContentFragment) rightFragment;
                        if (det != null) {
                            if (det.upOneLevel() == true) {
                                return true;
                            }
                        }
                    }
                    if (((BaseFileManagerFragment) rightFragment).upOneLevel() == true) {
                        return true;
                    }
                } else {
                    // Popping the detailed view needs to occur first so that it will not be visible when the left fragment
                    // is updating the screen (for proper highlight of items to work)
                    getFragmentManager().popBackStack();
                    if (leftFragment != null) {
                        File currDir = FileManagerApp.getCurrDir();
                        String curDetailedInfoPath =
                                ((LocalDetailedInfoFileManagerFragment) rightFragment).getItemPath();
                        if (currDir != null && curDetailedInfoPath != null) {
                            boolean result = curDetailedInfoPath.contains(currDir.getAbsolutePath());
                            if (result) {
                                ((BaseFileManagerFragment) leftFragment).updateLeftFragment(currDir
                                        .getParentFile());
                            } else {
                                ((BaseFileManagerFragment) leftFragment).updateHighlightedItem(currDir
                                        .getAbsolutePath());
                            }
                        }
                    }
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void rtnPickerResult(String filePath, String authInfo) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(MainFileManagerActivity.EXTRA_KEY_RESULT, filePath);
        if (authInfo != null) {
            returnIntent.putExtra(MainFileManagerActivity.EXTRA_KEY_AUTHINFO, authInfo);
        }
        rtnPickerResult(returnIntent);
    }

    public static boolean getEMMCEnabled() {
        // Update later based on tablet APi's
        // eMMCEnabled= (SystemProperties.getInt("ro.mot.internalsdcard", 0) !=
        // 0);
        return eMMCEnabled;
    }

    public void rtnPickerResult(Intent iData) {
        mFileManagerApp.mLaunchMode = FileManagerApp.NORMAL_MODE;
        if (mInstance != null) {
            mInstance.setResult(RESULT_OK, iData);
            mInstance.finish();
        }
    }

    public static void setViewType(int viewtype) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Create all menus regardless of the state and hide them as needed in
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mListview = (menu.findItem(R.id.listView));
        mGridview = (menu.findItem(R.id.gridView));
        mColumnview = menu.findItem(R.id.columnView);
        mViewitem = (menu.findItem(R.id.viewItem));
        mCancelbutton = (menu.findItem(R.id.cancelbutton));
        mPastebutton = (menu.findItem(R.id.pastebutton));
        mPlacebutton = (menu.findItem(R.id.placebutton));
        mNewfolder = (menu.findItem(R.id.new_folder));
        mSearchbutton = (menu.findItem(R.id.search));
        mSortItem = (menu.findItem(R.id.sortItem));
        mSortName = (menu.findItem(R.id.sortByName));
        mSortType = (menu.findItem(R.id.sortByType));
        mSortDate = (menu.findItem(R.id.sortByDate));
        mSortSize = (menu.findItem(R.id.sortBySize));
        mCopybutton = (menu.findItem(R.id.copybutton));
        mMovebutton = (menu.findItem(R.id.movebutton));
        mRenamebutton = (menu.findItem(R.id.renamebutton));
        mCompressbutton = (menu.findItem(R.id.compressbutton));
        mPrintbutton = (menu.findItem(R.id.printbutton));
        mDeletebutton = (menu.findItem(R.id.deletebutton));
        mMultiselect = (menu.findItem(R.id.multimode));
        switch (mFileManagerApp.getUserSelectedViewMode()) {
            case FileManagerApp.LISTVIEW :
                mListview.setChecked(true);
                mViewitem.setTitle(R.string.menu_view_list);
                mViewitem.setIcon(R.drawable.ic_menu_list_holo);
                mSortItem.setVisible(false);
                break;
            case FileManagerApp.GRIDVIEW :
                mGridview.setChecked(true);
                mViewitem.setTitle(R.string.menu_view_grid);
                mViewitem.setIcon(R.drawable.ic_menu_grid_holo);
                mSortItem.setVisible(true);
                setSortCheckItem();
                mSortItem.setTitle(getSortItem());
                break;
            case FileManagerApp.COLUMNVIEW :
                mColumnview.setChecked(true);
                mViewitem.setTitle(R.string.menu_view_column);
                mViewitem.setIcon(R.drawable.ic_menu_column_holo);
                mSortItem.setVisible(true);
                setSortCheckItem();
                mSortItem.setTitle(getSortItem());
        }
        Fragment leftFragment = getFragmentManager().findFragmentById(R.id.home_page);
        Fragment det = getFragmentManager().findFragmentById(R.id.details);
        // for Deatled view - clean option menu for now
        if (det != null) {
            if (!(det.getClass().getName().equals(RemoteDetailedInfoFileManagerFragment.class
                    .getName()))) {

                if (mFileManagerApp.mLaunchMode != FileManagerApp.NORMAL_MODE) {
                    mCancelbutton.setVisible(true);
                    mNewfolder.setVisible(false);
                    mMultiselect.setVisible(false);
                    mPastebutton.setVisible(false);
                    mPlacebutton.setVisible(false);
                    showSearchMenu();
                    clearActionbarPasteModeUi();
                    //not normal mode,not show any of these options
                    hideLeftPanelActionItems();

                } else {
                    // Normal Mode
                    mNewfolder.setVisible(true);
                    if (FileManagerApp.getPasteMode() == FileManagerApp.NO_PASTE) {
                        // No Paste Mode.
                        mCancelbutton.setVisible(false);
                        mPastebutton.setVisible(false);
                        mPlacebutton.setVisible(false);
                        mMultiselect.setVisible(true);
                        showSearchMenu();
                        clearActionbarPasteModeUi();
                    } else if (FileManagerApp.getPasteMode() == FileManagerApp.COPY_MODE) {
                        // Copy Mode.
                        mCancelbutton.setVisible(true);
                        mPastebutton.setVisible(true);
                        mPlacebutton.setVisible(false);
                        mMultiselect.setVisible(false);
                        hideSearchMenu();
                        setActionbarPasteModeUi();
                        hideLeftPanelActionItems();
                    } else {
                        // Move Mode.
                        mCancelbutton.setVisible(true);
                        mPlacebutton.setVisible(true);
                        mPastebutton.setVisible(false);
                        mMultiselect.setVisible(false);
                        hideSearchMenu();
                        setActionbarPasteModeUi();
                        hideLeftPanelActionItems();
                    }
                }
                IconifiedText highligtedItem = null;
                if (FileManagerApp.isHomePage()) {
                    if (mFileManagerApp.getViewMode() == FileManagerApp.COLUMNVIEW &&
                            (det.getClass().getName().equals(LocalColumnViewFrameLeftFragment.class
                                    .getName()))) {

                        if (((LocalColumnViewFrameLeftFragment) det).getTotalColumns() < 2) {
                            hideLeftPanelActionItems();
                        } else {
                            highligtedItem = ((BaseFileManagerFragment) det).getHighlightedItem();
                        }
                    } else {
                        hideLeftPanelActionItems();
                    }
                } else {
                    highligtedItem = ((BaseFileManagerFragment) leftFragment).getHighlightedItem();
                }
                mCompressbutton.setEnabled(true);

                if ((highligtedItem != null) && (highligtedItem.isDirectory())) {
                    //disable print for directory per CXD 
                    mPrintbutton.setIcon(R.drawable.ic_menu_print_disabled_holo);
                    mPrintbutton.setEnabled(false);
                } else if (highligtedItem != null) {
                    mPrintbutton.setIcon(R.drawable.ic_menu_print_holo);
                    mPrintbutton.setEnabled(true);
                    if (FileUtils.isZipFile(highligtedItem.getText())) {
                        //disable compress if highlighted file is zip file
                        mCompressbutton.setIcon(R.drawable.ic_menu_compress_disabled_holo);
                        mCompressbutton.setEnabled(false);
                        //disable print for zip file 
                        mPrintbutton.setIcon(R.drawable.ic_menu_print_disabled_holo);
                        mPrintbutton.setEnabled(false);
                    }
                }

            } else {
                mCancelbutton.setVisible(false);
                mNewfolder.setVisible(false);
                mMultiselect.setVisible(false);
                mListview.setVisible(false);
                mGridview.setVisible(false);
                mViewitem.setVisible(false);
                mPastebutton.setVisible(false);
                mPlacebutton.setVisible(false);
                mSortItem.setVisible(false);
                hideSearchMenu();
                hideLeftPanelActionItems();
            }
        }

        // for Search case need to show widget if search results are showing
        if (leftFragment != null) {
            if (leftFragment.getClass().getName().equals(
                    LocalLeftFileManagerFragment.class.getName())) {
                if (((LocalLeftFileManagerFragment) leftFragment).isShowingSearchResults() &&
                        (FileManagerApp.getPasteMode() == FileManagerApp.NO_PASTE)) {
                    showSearchWidget(false, ((LocalLeftFileManagerFragment) leftFragment)
                            .getSearchedString());
                }
            }
        }

        return true;
    }

    private void showSearchMenu() {
        if (mSearchbutton != null) {
            mSearchbutton.setVisible(true);
        }
        // Hide the actual widget on the left. Only show it when the search icon is selected.
        if (mSearchView != null) {
            mSearchView.setVisibility(View.GONE);
            mSearchView.clearFocus();
        }
    }

    private void hideSearchMenu() {
        if (mSearchbutton != null) {
            mSearchbutton.setVisible(false);
        }
        if (mSearchView != null) {
            mSearchView.setVisibility(View.GONE);
            mSearchView.clearFocus();
        }
    }

    public void showSearchWidget(boolean focus, String text) {
        if (mSearchbutton != null) {
            mSearchbutton.setVisible(false);
        }
        if (mSearchView != null) {
            mSearchView.setVisibility(View.VISIBLE);
            if (focus) {
                mSearchView.requestFocus();
            } else {
                mSearchView.clearFocus();
            }
            mSearchView.setQuery(text, false);
        }
        if (mActionBarTitle != null) {
            mActionBarTitle.setVisibility(View.GONE);
        }
    }

    public void hideSearchWidget() {
        // mSearchView onClose() can be called even the widget is not visble.
        // Need to check paste mode for that case.
        if (FileManagerApp.getPasteMode() == FileManagerApp.NO_PASTE) {
            showSearchMenu();
        } else {
            hideSearchMenu();
        }

        if (mActionBarTitle != null) {
            mActionBarTitle.setVisibility(View.VISIBLE);
        }
    }

    private void setActionbarPasteModeUi() {
        if (mActionbar != null) {
            mActionbar.setBackgroundDrawable(getResources().getDrawable(
                    R.drawable.list_selector_multiselect_holo_light));
        }
        if (mActionBarTitle != null) {
            String numOfCopied =
                    getResources()
                            .getString(R.string.copied, FileManagerApp.getPasteFiles().size());
            mActionBarTitle.setText(numOfCopied);
            mActionBarTitle.setVisibility(View.VISIBLE);
        }
    }

    private void clearActionbarPasteModeUi() {
        if (mActionbar != null) {
            mActionbar.setBackgroundDrawable(null);
        }
        if (mActionBarTitle != null) {
            mActionBarTitle.setText(R.string.select_file);
            mActionBarTitle.setVisibility(View.VISIBLE);
        }
    }

    public void disableViewModeSelection() {
        if (mViewitem != null) {
            mViewitem.setEnabled(false);
            switch (mFileManagerApp.getUserSelectedViewMode()) {
                case FileManagerApp.LISTVIEW :
                    mViewitem.setIcon(R.drawable.ic_menu_list_disabled_holo);
                    break;
                case FileManagerApp.GRIDVIEW :
                    mViewitem.setIcon(R.drawable.ic_menu_grid_disabled_holo);
                    break;
                case FileManagerApp.COLUMNVIEW :
                    mViewitem.setIcon(R.drawable.ic_menu_column_disabled_holo);
                    break;
            }
        }
    }

    public void enableViewModeSelection() {
        if (mViewitem != null) {
            mViewitem.setEnabled(true);
            switch (mFileManagerApp.getUserSelectedViewMode()) {
                case FileManagerApp.LISTVIEW :
                    mViewitem.setIcon(R.drawable.ic_menu_list_holo);
                    break;
                case FileManagerApp.GRIDVIEW :
                    mViewitem.setIcon(R.drawable.ic_menu_grid_holo);
                    break;
                case FileManagerApp.COLUMNVIEW :
                    mViewitem.setIcon(R.drawable.ic_menu_column_holo);
                    break;
            }
        }
    }

    private BroadcastReceiver mBroadCaseReveiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Fragment rightFragment = getFragmentManager().findFragmentById(R.id.details);
            String action = intent.getAction();
            if ((action != null) && (action.equals(MainFileManagerActivity.ACTION_TRANSFERFINISHED))) {
                if ((rightFragment != null)) {
                    if ((rightFragment.getClass().getName().equals(RemoteContentFragment.class
                            .getName()))) {
                        ((RemoteContentFragment) rightFragment).onNewIntent(intent);
                    } else {
                        ((BaseFileManagerFragment) rightFragment).onNewIntent(intent);
                    }
                }

                if (MainFileManagerActivity.EXTRA_VAL_REASONCUT.equals(intent
                        .getStringExtra(MainFileManagerActivity.EXTRA_KEY_PASTEREASON))) {
                    cancelPaste();
                    Bundle data = intent.getExtras();
                    ArrayList<String> oldFilePath =
                            data.getStringArrayList(MainFileManagerActivity.EXTRA_KEY_SOURCEFILENAME);
                    ArrayList<String> newFilePath =
                            FileUtils.getCopyMoveDestinationPath(oldFilePath, data
                                    .getString(MainFileManagerActivity.EXTRA_KEY_DESTFILENAME));
                    for (int i = 0; i < oldFilePath.size(); i++) {
                        String oldPath = oldFilePath.get(i);
                        String newPath = newFilePath.get(i);
                        FileManagerApp.log(TAG + " path before move is " + oldPath +
                                " path after move is" + newPath, false);
                        if ((newPath != null) && (!newPath.startsWith(FileManagerApp.SAMBA_DIR))) {
                            mFileManagerApp.syncShortcut(oldPath, newPath);
                            mFileManagerApp.syncRecentFile(oldPath, newPath);
                        }
                    }
                }

            }
        }
    };

    private void cancelPaste() {
        FileManagerApp.setPasteMode(FileManagerApp.NO_PASTE);
    }

    private void setSortCheckItem() {
        switch (mFileManagerApp.getSortMode()) {
            case FileManagerApp.NAMESORT :
                mSortName.setChecked(true);
                break;
            case FileManagerApp.TYPESORT :
                mSortType.setChecked(true);
                break;

            case FileManagerApp.DATESORT :
                mSortDate.setChecked(true);
                break;

            case FileManagerApp.SIZESORT :
                mSortSize.setChecked(true);
                break;
            default :
                break;
        }
    }

    private int getSortItem() {
        int sortItem;
        switch (mFileManagerApp.getSortMode()) {
            case FileManagerApp.NAMESORT :
                sortItem = R.string.menu_sort_by_name;
                break;
            case FileManagerApp.TYPESORT :
                sortItem = R.string.menu_sort_by_type;
                break;

            case FileManagerApp.DATESORT :
                sortItem = R.string.menu_sort_by_date;
                break;

            case FileManagerApp.SIZESORT :
                sortItem = R.string.menu_sort_by_size;
                break;
            default :
                sortItem = R.string.menu_sort_by_name;
                break;
        }

        return sortItem;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Prevent casting FC issue for Samba.
        Fragment fragment = getFragmentManager().findFragmentById(R.id.details);
        if (fragment.getClass().getName().equals(RemoteContentFragment.class.getName())) {
            return false;
        }

        //For remote detailed view we we are poping back the stack, no special handler and no search support
        if ((fragment.getClass().getName().equals(RemoteDetailedInfoFileManagerFragment.class
                .getName()))) {
            getFragmentManager().popBackStack();
            return true;
        }
        BaseFileManagerFragment rightFragment = (BaseFileManagerFragment) fragment;
        BaseFileManagerFragment leftFragment =
                (BaseFileManagerFragment) getFragmentManager().findFragmentById(R.id.home_page);
        BaseFileManagerFragment leftOpFrag = null;
        if ((leftFragment != null) && !FileManagerApp.isHomePage()) {

            leftOpFrag = leftFragment;
        } else {
            if (mFileManagerApp.getViewMode() == FileManagerApp.COLUMNVIEW) {
                if (((LocalColumnViewFrameLeftFragment) rightFragment).getTotalColumns() > 1) {
                    leftOpFrag = rightFragment;
                }
            }
        }

        switch (item.getItemId()) {
            case R.id.new_folder :
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                DialogFragment newFragment =
                        CustomDialogFragment.newInstance(CustomDialogFragment.DIALOG_NEW_FOLDER,
                                false, mContext, true);
                // Show the dialog.
                newFragment.show(ft, "dialog");
                return true;

            case R.id.multimode :
                if (rightFragment != null) {
                    rightFragment.handleSelectMultiple();
                }
                return true;
            case R.id.listView :
                if (!item.isChecked()) {
                    item.setChecked(true);
                    mViewitem.setTitle(R.string.menu_view_list);
                    mViewitem.setIcon(R.drawable.ic_menu_list_holo);
                    mSortItem.setVisible(false);
                    mFileManagerApp.setUserSelectedViewMode(FileManagerApp.LISTVIEW);
                    if (rightFragment != null) {
                        rightFragment.switchView(FileManagerApp.LISTVIEW, true);
                    }
                }
                return true;
            case R.id.gridView :
                if (!item.isChecked()) {
                    item.setChecked(true);
                    mViewitem.setTitle(R.string.menu_view_grid);
                    mViewitem.setIcon(R.drawable.ic_menu_grid_holo);
                    mFileManagerApp.setUserSelectedViewMode(FileManagerApp.GRIDVIEW);
                    if (rightFragment != null) {
                        //handle sort in grid view
                        mSortItem.setVisible(true);
                        setSortCheckItem();
                        mSortItem.setTitle(getSortItem());
                        rightFragment.switchView(FileManagerApp.GRIDVIEW, true);
                    }
                }
                return true;

            case R.id.columnView :
                if (!item.isChecked()) {
                    item.setChecked(true);
                    mViewitem.setTitle(R.string.menu_view_column);
                    mViewitem.setIcon(R.drawable.ic_menu_column_holo);
                    mSortItem.setVisible(false);
                    mFileManagerApp.setUserSelectedViewMode(FileManagerApp.COLUMNVIEW);
                    if (rightFragment != null) {
                        rightFragment.switchView(FileManagerApp.COLUMNVIEW, true);
                    }
                    /*LocalColumnViewFrameLeftFragment df =
                        new LocalColumnViewFrameLeftFragment(R.layout.column_view);        
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.replace(R.id.details, df);                    
                    //ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    //ft.addToBackStack(null);
                    int o = getFragmentManager().getBackStackEntryCount() ;
                    ft.commit();*/
                }
                return true;

            case R.id.sortByName :
                if (!item.isChecked()) {
                    item.setChecked(true);
                    mSortItem.setTitle(R.string.menu_sort_by_name);
                    if (rightFragment != null) {
                        rightFragment.setUpViewSorting(FileManagerApp.NAMESORT);
                    }
                }
                return true;
            case R.id.sortByType :
                if (!item.isChecked()) {
                    item.setChecked(true);
                    mSortItem.setTitle(R.string.menu_sort_by_type);
                    if (rightFragment != null) {
                        rightFragment.setUpViewSorting(FileManagerApp.TYPESORT);
                    }
                }
                return true;
            case R.id.sortByDate :
                if (!item.isChecked()) {
                    item.setChecked(true);
                    mSortItem.setTitle(R.string.menu_sort_by_date);
                    if (rightFragment != null) {
                        rightFragment.setUpViewSorting(FileManagerApp.DATESORT);
                    }
                }
                return true;

            case R.id.sortBySize :
                if (!item.isChecked()) {
                    item.setChecked(true);
                    mSortItem.setTitle(R.string.menu_sort_by_size);
                    if (rightFragment != null) {
                        rightFragment.setUpViewSorting(FileManagerApp.SIZESORT);
                    }
                }
                return true;
            case android.R.id.home :
                if (FileManagerApp.getBrowseMode() == FileManagerApp.PAGE_CONTENT_SEARCH) {
                    if (leftFragment != null) {
                        if (leftFragment.getClass().getName().equals(
                                LocalLeftFileManagerFragment.class.getName())) {
                            if (mSearchFragments.isEmpty()) {
                                // No extra fragments were added during search so just let the upOneLevel
                                // handle exiting the search view
                                // Else for ColumnView, only time left fragment will be local left is when
                                // search results are showing so it's ok to let the local left's upOneLevel
                                // handle this case
                                leftFragment.upOneLevel();
                            } else if (mFileManagerApp.getUserSelectedViewMode() == FileManagerApp.COLUMNVIEW) {
                                // Can only be this case is search results are showing
                                clearSearchBackStack();
                                leftFragment.upOneLevel();
                            } else {
                                FileManagerApp.setBrowseMode(FileManagerApp.PAGE_CONTENT_NORMAL);
                                // There were some extra fragments added during search. Need to pop them
                                // and browse up one level from current folder.
                                // Both left and right fragment will be re-initialized.
                                if (((LocalLeftFileManagerFragment) leftFragment)
                                        .isShowingSearchResults()) {
                                    // Dismiss the search results
                                    FileManagerApp
                                            .setCurrDir(((LocalLeftFileManagerFragment) leftFragment)
                                                    .getSearchedDirectory());
                                } else {
                                    if ((rightFragment != null) &&
                                            (rightFragment.getClass().getName()
                                                    .equals(LocalDetailedInfoFileManagerFragment.class
                                                            .getName()))) {
                                        String curDetailedInfoPath =
                                                ((LocalDetailedInfoFileManagerFragment) rightFragment)
                                                        .getItemPath();
                                        File curDetailedInfoFile =
                                                FileUtils.getFile(curDetailedInfoPath);
                                        FileManagerApp.setCurrDir(curDetailedInfoFile
                                                .getParentFile());
                                    } else {
                                        // Set currDir to the parent folder
                                        FileManagerApp.setCurrDir(FileManagerApp.getCurrDir()
                                                .getParentFile());
                                    }
                                }
                                if ((rightFragment != null) &&
                                        (rightFragment.getClass().getName()
                                                .equals(LocalRightFileManagerFragment.class
                                                        .getName()))) {
                                    // If detailed view is not showing, then popping the fragments will not
                                    // trigger right fragment to update. Need to call handleBrowse to update display.
                                    if (mFileManagerApp.getUserSelectedViewMode() != mFileManagerApp
                                            .getViewMode()) {
                                        switch (mFileManagerApp.getUserSelectedViewMode()) {
                                            case FileManagerApp.LISTVIEW :
                                                FileManagerApp.log(TAG + "returning to list view ",
                                                        false);
                                                ((LocalRightFileManagerFragment) rightFragment)
                                                        .switchView(FileManagerApp.LISTVIEW, true);
                                                break;
                                            case FileManagerApp.GRIDVIEW :
                                                FileManagerApp.log(TAG + "returning to grid view ",
                                                        false);
                                                ((LocalRightFileManagerFragment) rightFragment)
                                                        .switchView(FileManagerApp.GRIDVIEW, true);
                                                break;
                                            case FileManagerApp.COLUMNVIEW :
                                                FileManagerApp.log(TAG +
                                                        "returning to column view ", false);
                                                ((LocalRightFileManagerFragment) rightFragment)
                                                        .switchView(FileManagerApp.COLUMNVIEW, true);
                                                break;
                                        }
                                    } else {
                                        ((LocalRightFileManagerFragment) rightFragment)
                                                .handleBrowse();
                                    }
                                    ((LocalRightFileManagerFragment) rightFragment).showScreen();
                                }
                                getFragmentManager().popBackStack(mSearchFragments.get(0),
                                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                // re-initialize mSearchFragments to get rid of all previous ID's
                                mSearchFragments = new ArrayList<Integer>();
                                // Since fragments are popped, onActivityCreated/onResume will be re-called
                                // and will force the fragments to update to the current directory
                            }
                            enableViewModeSelection();
                            return true;
                        } else if (mFileManagerApp.getViewMode() == FileManagerApp.COLUMNVIEW) {
                            // Fall through to let normal case upOneLevel handle the screen navigation
                            FileManagerApp.setBrowseMode(FileManagerApp.PAGE_CONTENT_NORMAL);
                            clearSearchBackStack();
                        }
                    }
                }
                //For detailed view we we are poping back the stack, no special handler
                if ((rightFragment.getClass().getName()
                        .equals(LocalDetailedInfoFileManagerFragment.class.getName()))) {
                    String curDetailedInfoPath =
                            ((LocalDetailedInfoFileManagerFragment) rightFragment).getItemPath();
                    File curDetailedInfoFile = FileUtils.getFile(curDetailedInfoPath);
                    FileManagerApp.setCurrDir(curDetailedInfoFile.getParentFile());
                    getFragmentManager().popBackStack();
                    if (leftFragment != null) {
                        /*if (leftFragment.getClass().getName()
                                .equals(LocalLeftFileManagerFragment.class.getName())){*/
                        leftFragment
                                .updateLeftFragment(FileManagerApp.getCurrDir().getParentFile());
                        //}
                    }
                    return true;
                }
                if (leftFragment != null) {
                    (leftFragment).upOneLevel();
                }
                if (rightFragment != null) {
                    if (rightFragment.upOneLevel() == true) {
                        return true;
                    }
                }
                return true;
            case R.id.copybutton :

                if (leftOpFrag != null) {
                    leftOpFrag.handleCopy(false);
                    invalidateOptionsMenu();
                }
                return true;

            case R.id.movebutton :
                if (leftOpFrag != null) {
                    leftOpFrag.handleMove(false);
                    invalidateOptionsMenu();
                }
                return true;

            case R.id.renamebutton :
                if (leftOpFrag != null) {
                    FragmentTransaction ft1 = getFragmentManager().beginTransaction();
                    DialogFragment newFragment1 =
                            CustomDialogFragment.newInstance(CustomDialogFragment.DIALOG_RENAME, false,
                                    mContext, leftOpFrag.getFileName(false), true);
                    // Show the dialog.
                    newFragment1.show(ft1, "dialog");
                }
                return true;
            case R.id.compressbutton:
                FragmentTransaction ftc = getFragmentManager().beginTransaction();
                DialogFragment newFragmentc =
                        CustomDialogFragment.newInstance(CustomDialogFragment.DIALOG_ZIP_OPTION, false,
                                mContext, true);
                // Show the dialog.
                newFragmentc.show(ftc, "dialog");
                return true;

            case R.id.deletebutton :
                if (leftOpFrag != null) {
                    if (leftOpFrag.getClass().getName().equals(
                            LocalLeftFileManagerFragment.class.getName())) {
                        IconifiedText highligtedItem =
                                ((LocalLeftFileManagerFragment) leftOpFrag).getHighlightedItem();
                        ((LocalLeftFileManagerFragment) leftOpFrag)
                                .setNextHighlightItemPos(highligtedItem);
                    }

                    FragmentTransaction ft6 = getFragmentManager().beginTransaction();
                    CustomDialogFragment newFragment6 =
                            CustomDialogFragment.newInstance(CustomDialogFragment.DIALOG_DELETE,
                                    false, mContext, true);
                    // Show the dialog.
                    newFragment6.show(ft6, "dialog");
                }
                return true;

            case R.id.printbutton :
                if (leftOpFrag != null) {
                    leftOpFrag.handlePrint(false);
                }

                return true;

            case R.id.cancelbutton :
                if (mFileManagerApp.mLaunchMode == FileManagerApp.NORMAL_MODE) {
                    FileManagerApp.setPasteMode(FileManagerApp.NO_PASTE);
                    FileManagerApp.setPasteFiles(null);
                    invalidateOptionsMenu();
                } else {
                    finish();
                }
                return true;

            case R.id.placebutton :
            case R.id.pastebutton :
                if (rightFragment != null) {
                    rightFragment.handlePaste(FileManagerApp.getPasteFiles(), FileManagerApp
                            .getCurrDir().toString());
                    FileManagerApp.setPasteMode(FileManagerApp.NO_PASTE);
                    FileManagerApp.setPasteFiles(null);
                    invalidateOptionsMenu();
                }
                return true;
            case R.id.search :
                showSearchWidget(true, null);
                return true;
            default :
                return super.onOptionsItemSelected(item);
        }
    }

    public Context getCurContext() {
        return mContext;
    }

    protected void initActivity() {

        Intent intent = getIntent();
        String intentAction = intent.getAction();

        IntentFilter iFilt = new IntentFilter();
        iFilt.addAction(MainFileManagerActivity.ACTION_TRANSFERFINISHED);
        registerReceiver(mBroadCaseReveiver, iFilt);

        if (intentAction != null) {
            if (intentAction.equals(ACTION_SELECTFILE)) {
                mFileManagerApp.setLaunchMode(FileManagerApp.SELECT_FILE_MODE);
                String extraTitle = intent.getStringExtra(EXTRA_KEY_HOMESYNC_TITLE);
                String extraStep = intent.getStringExtra(EXTRA_KEY_HOMESYNC_STEP);
                if (extraTitle == null) {
                    // make cancel button visible
                    findViewById(R.id.homesync_bar).setVisibility(View.GONE);
                    FileManagerApp.setShowFolderViewIcon(true);
                } else {
                    // make cancel button invisible
                    findViewById(R.id.homesync_bar).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.homesync_title)).setText(extraTitle);
                    ((TextView) findViewById(R.id.homesync_step)).setText(extraStep);
                    FileManagerApp.setShowFolderViewIcon(false);
                    /*
                     * if(isAttDrmContentAvailable()) { IconifiedText
                     * protectedcontent = new
                     * IconifiedText(getString(R.string.protected_content),
                     * getResources().getDrawable(R.drawable.
                     * ic_launcher_folder_protected_content));
                     * protectedcontent.setIsNormalFile(false);
                     * mHomePageStaticItems.add(protectedcontent); }
                     */
                }
            } else if (intentAction.equals(ACTION_SELECTFOLDER)) {
                mFileManagerApp.mLaunchMode = FileManagerApp.SELECT_FOLDER_MODE;
                String extraTitle = intent.getStringExtra(EXTRA_KEY_HOMESYNC_TITLE);
                String extraStep = intent.getStringExtra(EXTRA_KEY_HOMESYNC_STEP);
                if (extraTitle == null) {
                    // mFileChooserPanel.setVisibility(View.VISIBLE);
                    findViewById(R.id.homesync_bar).setVisibility(View.GONE);
                    FileManagerApp.setShowFolderViewIcon(true);
                } else {
                    // mFileChooserPanel.setVisibility(View.GONE);
                    findViewById(R.id.homesync_bar).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.homesync_title)).setText(extraTitle);
                    ((TextView) findViewById(R.id.homesync_step)).setText(extraStep);
                    FileManagerApp.setShowFolderViewIcon(false);
                    /*
                     * if(isAttDrmContentAvailable()) { IconifiedText
                     * protectedcontent = new
                     * IconifiedText(getString(R.string.protected_content),
                     * getResources().getDrawable(R.drawable.
                     * ic_launcher_folder_protected_content));
                     * protectedcontent.setIsNormalFile(false);
                     * mHomePageStaticItems.add(protectedcontent); }
                     */
                }
            } else if (intentAction.equals(Intent.ACTION_GET_CONTENT)) {
                // FileManagerApp.log(TAG + "intent is " + intent.getExtras());
                mFileManagerApp.mLaunchMode = FileManagerApp.SELECT_GET_CONTENT;
                mFileManagerApp.saveContentIntent(intent);
            } else {
                mFileManagerApp.mLaunchMode = FileManagerApp.NORMAL_MODE;
                /*
                 * if(isAttDrmContentAvailable()) { IconifiedText
                 * protectedcontent = new
                 * IconifiedText(getString(R.string.protected_content),
                 * getResources
                 * ().getDrawable(R.drawable.ic_launcher_folder_protected_content
                 * )); protectedcontent.setIsNormalFile(false);
                 * mHomePageStaticItems.add(protectedcontent); }
                 */
                // mFileChooserPanel.setVisibility(View.GONE);
                FileManagerApp.setShowFolderViewIcon(true);
            }
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(Intent.ACTION_SEARCH)) {
                FileManagerApp.log("onNewIntent action is ACTION_SEARCH ", false);
                String searchedString = mSearchView.getQuery().toString();
                FileManagerApp.log("onNewIntent action is ACTION_SEARCH searchedString=" +
                        searchedString, false);
                Fragment leftFragment = getFragmentManager().findFragmentById(R.id.home_page);
                if (leftFragment != null) {
                    if (leftFragment.getClass().getName().equals(
                            LocalLeftFileManagerFragment.class.getName())) {
                        if (((LocalLeftFileManagerFragment) leftFragment).isShowingSearchResults()) {
                            // Maintain previous search. Start new fragment and add to list
                            // Since searching where search results are shown, start search at the
                            // same level as the previous search
                            FileManagerApp
                                    .log("Search Result already showing, starting a new fragment",
                                            false);
                            LocalLeftFileManagerFragment df =
                                    new LocalLeftFileManagerFragment(R.layout.title_file_list,
                                            ((LocalLeftFileManagerFragment) leftFragment)
                                                    .getSearchedDirectory(), searchedString);
                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                            ft.replace(R.id.home_page, df);
                            ft.addToBackStack(null);
                            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                            int id = ft.commit();
                            addSearchFragments(id);
                        } else {
                            ((LocalLeftFileManagerFragment) leftFragment).startSearchDirectory(
                                    FileManagerApp.getCurrDir(), searchedString);
                        }
                    } else { // Home Fragment is in view
                        // 1st left fragment, no need to add to list of search fragments
                        LocalLeftFileManagerFragment df =
                                new LocalLeftFileManagerFragment(R.layout.title_file_list,
                                        FileManagerApp.getCurrDir(), searchedString);
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.replace(R.id.home_page, df);
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                        ft.commit();
                        if ((FileManagerApp.getBrowseMode() == FileManagerApp.PAGE_CONTENT_SEARCH) &&
                                (mFileManagerApp.getViewMode() == FileManagerApp.COLUMNVIEW)) {
                            Fragment rightFragment =
                                    getFragmentManager().findFragmentById(R.id.details);
                            if (rightFragment != null) {
                                if (rightFragment.getClass().getName().equals(
                                        LocalColumnViewFrameLeftFragment.class.getName())) {
                                    df.setSearchModeTopDirectory(((LocalColumnViewFrameLeftFragment) rightFragment)
                                                    .getSearchModeTopDirectory());
                                }
                            }
                        }
                    }
                } else {
                    FileManagerApp.log(
                            "onNewIntent failed to start search because leftFragment is null!",
                            true);
                }
            } else if (action.equals(MainFileManagerActivity.ACTION_SHOWDOWNLOADPAGE)) {
                mFileManagerApp.setPhoneStoragePath(FileManagerApp.INDEX_INTERNAL_MEMORY, -1);
            }
        }
    }

    /* Xqh748 Dialog fragment class */
    public void showDialog(int id, String text) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction. We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        DialogFragment newFragment = CustomDialogFragment.newInstance(id, false, mContext);
        // Show the dialog.
        newFragment.show(ft, "dialog");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        SambaExplorer.log("onActivityResult request:" + requestCode + " result:" + resultCode,
                false);
        Fragment rightFragment = getFragmentManager().findFragmentById(R.id.details);
        if (rightFragment == null) {
            return;
        }

        switch (requestCode) {
            case FileManagerApp.Samba_Explorer_LAUNCH :
                if (rightFragment.getClass().getName()
                        .equals(RemoteContentFragment.class.getName())) {
                    RemoteContentFragment det = (RemoteContentFragment) rightFragment;
                    det.sendRemoveMsgwithDelay();
                }
                break;

            case FileManagerApp.Samba_Explorer_PRINT :
                if (rightFragment.getClass().getName()
                        .equals(RemoteContentFragment.class.getName())) {
                    RemoteContentFragment det = (RemoteContentFragment) rightFragment;
                    det.sendRemoveMsgwithDelay();
                }
                break;
        }
    }

    public boolean checkConnAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = null;
        if (cm != null) {
            netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null) {
                if (!netInfo.isConnected() || netInfo.getType() != ConnectivityManager.TYPE_WIFI) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    public void addSearchFragments(int id) {
        mSearchFragments.add(id);
    }

    public boolean isSearchFragmentsEmpty() {
        return mSearchFragments.isEmpty();
    }

    public void clearSearchBackStack() {
        if (!mSearchFragments.isEmpty()) {
            FileManagerApp.log(TAG + "purging backstack from Search", false);
            getFragmentManager().popBackStack(mSearchFragments.get(0),
                    FragmentManager.POP_BACK_STACK_INCLUSIVE);
            // re-initialize mSearchFragments to get rid of all previous ID's
            mSearchFragments = new ArrayList<Integer>();
        }
    }

    @Override
    public boolean onDrag(View view, DragEvent event) {
        boolean result = false;
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED :
                result = onDragStarted(event);
                mIsDragMode = true;
                break;
            case DragEvent.ACTION_DRAG_ENTERED :
                // The drag has entered the ListView window
                FileManagerApp.log(TAG + "DRAG ENTERED (target = " + mDropTargetAdapterPosition +
                        ")", false);
                break;
            case DragEvent.ACTION_DRAG_EXITED :
                // The drag has left the building
                FileManagerApp.log(TAG + " DRAG EXITED (target = " + mDropTargetAdapterPosition +
                        ")", false);
                onDragExited();
                break;
            case DragEvent.ACTION_DRAG_ENDED :
                // The drag is over
                FileManagerApp.log(TAG + " DRAG ENDED", false);
                onDragEnded();
                mIsDragMode = false;
                break;
            case DragEvent.ACTION_DRAG_LOCATION :
                // We're moving around within our window; handle scroll, if necessary
                onDragLocation(event);
                break;
            case DragEvent.ACTION_DROP :
                // The drag item was dropped
                FileManagerApp.log(TAG + "DROP", false);
                result = onDrop(event);
                break;
            default :
                break;
        }
        return result;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Save the touch location to draw the drag overlay at the correct location
            mShadow.setTouchX((int) event.getX());
        }
        // don't do anything, let the system process the event
        return false;
    }

    // All functions are overriden by child calls depending on what they want to do for Drag and Drop

    /**
     * Called when  Target View gets a DRAG_STARTED event
     */

    protected boolean onDragStarted(DragEvent event) {
        mIsDropSuccessful = false;
        return true;
    }

    /**
     * Called when our Target View gets a DRAG_EXITED event
     */

    protected void onDragExited() {

    }

    /**
     * Called while dragging;  highlight possible drop targets, and autoscroll the list.
     */
    protected void onDragLocation(DragEvent event) {
        BaseFileManagerFragment leftFragment2 =
                (BaseFileManagerFragment) getFragmentManager().findFragmentById(R.id.home_page);
        leftFragment2.onDragLocation(event);
    }

    protected void onDragEnded() {
        BaseFileManagerFragment leftFragment =
                (BaseFileManagerFragment) getFragmentManager().findFragmentById(R.id.home_page);
        leftFragment.onDragEnded();
    }

    /** Called when dropped in the target **/
    protected boolean onDrop(DragEvent event) {
        BaseFileManagerFragment leftFragment =
                (BaseFileManagerFragment) getFragmentManager().findFragmentById(R.id.home_page);
        return leftFragment.onDrop(event);
    }

    public boolean getIsDragMode() {
        return mIsDragMode;
    }

    public void setIsDropSuccess(boolean isDropSuccessful) {
        mIsDropSuccessful = isDropSuccessful;
    }

    public boolean getIsDropSuccess() {
        return mIsDropSuccessful;
    }

    private void hideLeftPanelActionItems() {
        //copy mode, also not show any of these options
        mCopybutton.setVisible(false);
        mMovebutton.setVisible(false);
        mRenamebutton.setVisible(false);
        mPrintbutton.setVisible(false);
        mDeletebutton.setVisible(false);
        mCompressbutton.setVisible(false);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU :
                FileManagerApp.log(TAG + " MENU long press", false);
                return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

}
