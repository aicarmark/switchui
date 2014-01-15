/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 *  Date          CR                Author       Description
 *  2010-03-23    IKSHADOW-2074     E12758       initial
 */
package com.motorola.filemanager.samba;

import java.util.ArrayList;
import java.util.List;

import jcifs.smb.SmbFile;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;
import com.motorola.filemanager.samba.service.DownloadServer;
import com.motorola.filemanager.ui.IconifiedText;
import com.motorola.filemanager.ui.IconifiedTextListAdapter;

public class RemoteContentFragment extends RemoteContentBaseFragment {
    private static final String TAG = "RemoteFileManager: ";
    private View mContentView;

    static public class DirectoryContents {
        public List<IconifiedText> listDir;
        public List<IconifiedText> listFile;
    }

    private MenuItem listview;
    private MenuItem gridview;
    private MenuItem viewitem;
    private MenuItem multiselect;
    private MenuItem newfolder;
    private MenuItem mCancelbutton;
    private MenuItem mPastebutton;
    private MenuItem mPlacebutton;
    private MenuItem mSortItem;
    private MenuItem mSortName;
    private MenuItem mSortType;
    private MenuItem mSortDate;
    private MenuItem mSortSize;

    List<IconifiedText> mListDir = new ArrayList<IconifiedText>();
    List<IconifiedText> mListFile = new ArrayList<IconifiedText>();
    private TableLayout mTablelayout;
    private RelativeLayout mDLayout;
    private RelativeLayout mLLyout;
    private RelativeLayout mTLayout;
    private RelativeLayout mSLayout;
    private String DELIM = " > ";
    protected String mDisplayText = null;
    private ImageView mTView2 = null;
    private ImageView mTView3 = null;
    // private ImageView mTView4 = null;
    private View mIconPlaceHolder = null;
    protected FileManagerApp mFileManagerApp = null;

    public static RemoteContentFragment newInstance(int index) {
        RemoteContentFragment f = new RemoteContentFragment();
        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("index", index);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();
        if (mContext != null) {
            // First invalidateOptionsMenu of the home page as we will use a different options.
            ((Activity) mContext).invalidateOptionsMenu();
        }
        if (mSmbExplorer.getCurPath() != null) {
            mSmbExplorer.browseTo(mSmbExplorer.getCurPath());
        }
    }

    public Handler getHandler() {
        return mRemoteHandler;

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

            mSLayout.setVisibility(View.GONE);
            mTLayout.setVisibility(View.GONE);
            mTView2.setVisibility(View.GONE);
            mTView3.setVisibility(View.GONE);
            // mTView4.setVisibility(View.GONE);
        } else {
            getResources().getValue(R.fraction.rel_weight_textinfod, outValue, false);
            float weight_date = outValue.getFloat();

            ((LinearLayout.LayoutParams) mDLayout.getLayoutParams()).weight = weight_date;
            ((LinearLayout.LayoutParams) mLLyout.getLayoutParams()).weight = weight_one;

            getResources().getValue(R.fraction.rel_layout_contextmenu, outValue, false);
            float weight_contextmenu = outValue.getFloat();
            ((LinearLayout.LayoutParams) mIconPlaceHolder.getLayoutParams()).weight =
                    weight_contextmenu;

            mSLayout.setVisibility(View.VISIBLE);
            mTLayout.setVisibility(View.VISIBLE);
            mTView2.setVisibility(View.VISIBLE);
            mTView3.setVisibility(View.VISIBLE);
            // mTView4.setVisibility(View.VISIBLE);
        }
        if (mSmbExplorer != null) {
            mSmbExplorer.setConfig(orientation);
        }
    }

    private boolean isSomeFileSelected() {
        IconifiedTextListAdapter adapter;
        adapter = (IconifiedTextListAdapter) mSmbFileView.getAdapter();
        if (adapter != null) {
            return adapter.isSomeFileSelected();
        } else {
            return false;
        }
    }

    public void handleDelete() {
        mSmbExplorer.delete();
    }

    public void handleRename(String newName, boolean isContext) {

        mSmbExplorer.handleRename(newName, isContext);
    }

    public void createNewFolder(String foldername) {
        if (!TextUtils.isEmpty(foldername)) {
            mSmbExplorer.addNewFolder(foldername);
        }
        // If Folder name is empty
        else {
            Toast.makeText(mContext, R.string.renaming_folder_empty, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        //For detailed view we don't need to go through create view so just return
        //it will be the only case that mContentView will not be null
        if (mContentView != null) {
            return mContentView;
        }

        mContentView = inflater.inflate(R.layout.remote_filemanager, container, false);
        mFileManagerApp = (FileManagerApp) getActivity().getApplication();
        setHasOptionsMenu(true);

        mRemoteHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                RemoteContentFragment.this.handleMessage(msg);
            }
        };
        mRemoteDeviceLayout = (LinearLayout) mContentView.findViewById(R.id.SavedSmbDevice);
        mSmbDeviceView = (ListView) mContentView.findViewById(R.id.SavedServerList);
        mSmbDevice = new SambaDeviceList(mSmbDeviceView, this, mRemoteHandler);

        mSmbExplorer = new SambaExplorer(mContentView, mRemoteHandler, this);
        mSmbFileLayout = (LinearLayout) mContentView.findViewById(R.id.SmbExploreLayout);
        mSmbFileView = (ListView) mContentView.findViewById(R.id.SmbFileList);

        mSmbGroupLayout = (LinearLayout) mContentView.findViewById(R.id.SmbGroupLayout);
        mSmbGroupView = (ListView) mContentView.findViewById(R.id.SmbGroupList);
        mSmbGroup = new SambaGroupList(mSmbGroupView, this, mRemoteHandler);
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
        // mTView4 = (ImageView) mContentView.findViewById(R.id.view4);
        setConfig(getResources().getConfiguration().orientation);
        if (((FileManagerApp) getActivity().getApplication()).getViewMode() == FileManagerApp.GRIDVIEW) {
            mTablelayout.setVisibility(View.GONE);
        }
        showServerList();
        createTmpFilePath(TMPPATH);

        mCurrentFolder = (TextView) mContentView.findViewById(R.id.current_folder);
        mCurrentPath = (TextView) mContentView.findViewById(R.id.path);

        mProgressDiagIndicator = getProgressDialogIndicator(R.array.progress_dialog_indicater);
        return mContentView;
    }

    private String upCurPath() {
        StringBuffer buf = new StringBuffer();
        String tempStr = mSmbExplorer.getCurPath();
        if (tempStr.equals(SambaExplorer.SMB_FILE_PRIFIX)) {
            return tempStr;

        } else {
            tempStr = tempStr.substring(0, tempStr.length() - 1);
            buf.append(tempStr.substring(0, tempStr.lastIndexOf('/')));
            buf.append('/');
            tempStr = buf.toString();
            return tempStr;
        }
    }

    public void updateRemoteLeftPanel() {
        try {
            Fragment leftFragment = getFragmentManager().findFragmentById(R.id.home_page);
            RemoteContentLeftFragment det = null;
            if (leftFragment != null) {
                if (leftFragment.getClass().getName().equals(RemoteContentLeftFragment.class
                        .getName())) {

                    det = (RemoteContentLeftFragment) leftFragment;
                } else {
                    det = RemoteContentLeftFragment.newInstance(0);
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.replace(R.id.home_page, det);
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                    ft.commit();
                }
            } else {
                det = RemoteContentLeftFragment.newInstance(0);
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.home_page, det);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }
            if ((det != null) && (mSmbFileLayout.getVisibility() == View.VISIBLE) &&
                    (mSmbExplorer.getPathDepth() > 0)) {
                // if we are viewing the deep level down
                String upPath = upCurPath();
                det.updateSmbExplorer(upPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleMessage(Message msg) {

        switch (msg.what) {
            case MESSAGE_SEARCH_SAMBA_GROUP :
                startGroupSearch();
                showGroupList();
                return;
            case MESSAGE_LAUNCH_SAMBA_EXPLORE :
                String hostInfo = msg.getData().getString(SAMABA_HOST_INFO);
                showLoadingDialog(mProgressDiagIndicator[PROGRESS_DIALOG_CONNECTING_SERVER],
                        PROGRESS_DIALOG_CONNECTING_SERVER);
                startSmabaExplore(hostInfo);
                return;
            case MESSAGE_RETURN_TO_DEVICE :
                showServerList();
                return;
            case MESSAGE_SHOW_FILE_INFO_VIEW :
                mSmbExplorer.showFileInfodialog();
                return;
            case MESSAGE_SHOW_FILE_LIST_VIEW :
                SambaExplorer.log(TAG + " stepInServer success !", false);
                hideLoadingDialog();
                showSmbFileList();
                String path = mSmbExplorer.getCurPath();
                if (path == null) {
                    return;
                }
                int position = FileManagerApp.SAMBA_DIR.length();
                String child_path = null;
                try {
                    child_path = path.substring(position);
                    if (child_path != null) {
                        child_path = child_path.replaceAll("/", DELIM);
                        mDisplayText = getString(R.string.shared_folders) + DELIM + child_path;
                        mCurrentPath.setText(mDisplayText);
                    }
                    ((MainFileManagerActivity) getActivity()).invalidateOptionsMenu();
                    if (mFileManagerApp.mLaunchMode == FileManagerApp.SELECT_FILE_MODE) {
                        mCurrentFolder.setText(R.string.select_file);
                    } else if (mFileManagerApp.mLaunchMode == FileManagerApp.SELECT_FOLDER_MODE) {
                        mCurrentFolder.setText(R.string.select_folder);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            case MESSAGE_HIDE_LOADING_DIALOG :
                hideLoadingDialog();
                return;
            case MESSAGE_SHOW_LOADING_DIALOG :
                int msgMode = msg.getData().getInt(PROGRESS_DIALOG_MODE);
                showLoadingDialog(mProgressDiagIndicator[msgMode], msgMode);
                return;
            case MESSAGE_DELAY_REMOVE_TEMPFILE :
                SambaExplorer.log(TAG + " MESSAGE_DELAY_REMOVE_TEMPFILE!", false);
                mDeleteTmpFile = true;
                return;

            case MESSAGE_COPY_FINISHED :
                hideLoadingDialog();
                mSmbExplorer.browseTo(mSmbExplorer.getCurPath());
                if (msg.arg1 == DownloadServer.TRANSFER_COMPLETE) {
                    Toast.makeText(mContext, mContext.getResources().getQuantityString(
                            R.plurals.copy_success, msg.arg2, msg.arg2), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, R.string.error_copying, Toast.LENGTH_SHORT).show();
                }
                return;

            case MESSAGE_MOVE_FINISHED :
                hideLoadingDialog();
                mSmbExplorer.browseTo(mSmbExplorer.getCurPath());
                if (msg.arg1 == DownloadServer.TRANSFER_COMPLETE) {
                    Toast.makeText(mContext, mContext.getResources().getQuantityString(
                            R.plurals.move_success, msg.arg2, msg.arg2), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, R.string.error_moving, Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SambaExplorer.log(" onDestroy...", false);
        if (mSmbExplorer != null) {
            mSmbExplorer.outSmbExploreMultiSelectMode();
        }
        deleteTmpFile();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setConfig(newConfig.orientation);
    }

    @Override
    public void onCreateOptionsMenu(Menu aMenu, MenuInflater inflater) {
        super.onCreateOptionsMenu(aMenu, inflater);
        return;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (menu.findItem(R.id.search) != null) {
            menu.findItem(R.id.search).setVisible(false);
        }
        if (menu.findItem(R.id.compressbutton) != null) {
            menu.findItem(R.id.compressbutton).setVisible(false);
        }
        if (menu.findItem(R.id.columnView) != null) {
            menu.findItem(R.id.columnView).setVisible(false);
        }

        if (menu.findItem(R.id.copybutton) != null) {
            menu.findItem(R.id.copybutton).setVisible(false);
        }

        if (menu.findItem(R.id.movebutton) != null) {
            menu.findItem(R.id.movebutton).setVisible(false);
        }

        if (menu.findItem(R.id.renamebutton) != null) {
            menu.findItem(R.id.renamebutton).setVisible(false);
        }

        if (menu.findItem(R.id.printbutton) != null) {
            menu.findItem(R.id.printbutton).setVisible(false);
        }

        if (menu.findItem(R.id.deletebutton) != null) {
            menu.findItem(R.id.deletebutton).setVisible(false);
        }
        listview = menu.findItem(R.id.listView);
        gridview = menu.findItem(R.id.gridView);
        viewitem = menu.findItem(R.id.viewItem);
        multiselect = menu.findItem(R.id.multimode);
        newfolder = menu.findItem(R.id.new_folder);
        mCancelbutton = (menu.findItem(R.id.cancelbutton));
        mPastebutton = (menu.findItem(R.id.pastebutton));
        mPlacebutton = (menu.findItem(R.id.placebutton));
        mSortItem = (menu.findItem(R.id.sortItem));
        mSortName = (menu.findItem(R.id.sortByName));
        mSortType = (menu.findItem(R.id.sortByType));
        mSortDate = (menu.findItem(R.id.sortByDate));
        mSortSize = (menu.findItem(R.id.sortBySize));

        if (whetherInFileList) {
            // if file list is on display
            //When browsing to child directory Name sort is the sort mode
            mSmbExplorer.setUpSmbViewSorting(FileManagerApp.NAMESORT);
            switch (((FileManagerApp) getActivity().getApplication()).getUserSelectedViewMode()) {
                case FileManagerApp.LISTVIEW :
                    listview.setChecked(true);
                    viewitem.setTitle(R.string.menu_view_list);
                    viewitem.setIcon(R.drawable.ic_menu_list_holo);
                    mSortItem.setVisible(false);
                    mSmbExplorer.setSmbUpSortArrows();
                    break;
                case FileManagerApp.GRIDVIEW :
                    gridview.setChecked(true);
                    viewitem.setTitle(R.string.menu_view_grid);
                    viewitem.setIcon(R.drawable.ic_menu_grid_holo);
                    mSortItem.setVisible(true);
                    mSortName.setChecked(true);
                    mSortItem.setTitle(R.string.menu_sort_by_name);
                    break;
                default : //In case of coming from Column view mode, set to list view.
                    listview.setChecked(true);
                    viewitem.setTitle(R.string.menu_view_list);
                    viewitem.setIcon(R.drawable.ic_menu_list_holo);
                    mSortItem.setVisible(false);
                    mSmbExplorer.setSmbUpSortArrows();
                    break;
            }
            if (FileManagerApp.getPasteMode() == FileManagerApp.NO_PASTE) {
                // No Paste Mode.
                if (mCancelbutton != null) {
                    mCancelbutton.setVisible(false);
                }
                if (mPastebutton != null) {
                    mPastebutton.setVisible(false);
                }
                if (mPlacebutton != null) {
                    mPlacebutton.setVisible(false);
                }
            } else if (FileManagerApp.getPasteMode() == FileManagerApp.COPY_MODE) {
                // Copy Mode.
                if (mCancelbutton != null) {
                    mCancelbutton.setVisible(true);
                }
                if (mPastebutton != null) {
                    mPastebutton.setVisible(true);
                }
                if (mPlacebutton != null) {
                    mPlacebutton.setVisible(false);
                }
            } else {
                // Move Mode.
                if (mCancelbutton != null) {
                    mCancelbutton.setVisible(true);
                }
                if (mPastebutton != null) {
                    mPastebutton.setVisible(false);
                }
                if (mPlacebutton != null) {
                    mPlacebutton.setVisible(true);
                }
            }

            int type = mSmbExplorer.getCurSmbFileType();
            if (type == SmbFile.TYPE_FILESYSTEM || type == SmbFile.TYPE_SHARE) {
                if (newfolder != null) {
                    newfolder.setVisible(true);
                }

                if (multiselect != null) {
                    if (FileManagerApp.getPasteMode() == FileManagerApp.NO_PASTE) {
                        multiselect.setVisible(true);
                    } else {
                        multiselect.setVisible(false);
                    }
                }

            } else {
                if (newfolder != null) {
                    newfolder.setVisible(false);
                }

                if (multiselect != null) {
                    multiselect.setVisible(false);
                }
            }

        } else {
            // if group/server list is on display

            if (listview != null) {
                listview.setVisible(false);
            }
            if (gridview != null) {
                gridview.setVisible(false);
            }
            if (viewitem != null) {
                viewitem.setVisible(false);
            }
            if (newfolder != null) {
                newfolder.setVisible(false);
            }
            if (multiselect != null) {
                multiselect.setVisible(false);
            }
            if (mCancelbutton != null) {
                mCancelbutton.setVisible(false);
            }
            if (mPastebutton != null) {
                mPastebutton.setVisible(false);
            }
            if (mPlacebutton != null) {
                mPlacebutton.setVisible(false);
            }
            if (mSortItem != null) {
                mSortItem.setVisible(false);
            }
        }
    }

    private void setSmbSortCheckItem() {
        switch (((FileManagerApp) getActivity().getApplication()).getSortMode()) {
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

    private int getSmbSortItem() {
        int sortItem;
        switch (((FileManagerApp) getActivity().getApplication()).getSortMode()) {
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
        switch (item.getItemId()) {
            case R.id.new_folder :
                showDialog(R.id.new_folder, false);
                return true;

            case R.id.multimode :
                setMultiSelectMode();

                return true;
            case R.id.listView :
                if (!item.isChecked()) {
                    item.setChecked(true);
                    viewitem.setTitle(R.string.menu_view_list);
                    viewitem.setIcon(R.drawable.ic_menu_list_holo);
                    mSortItem.setVisible(false);
                    ((FileManagerApp) getActivity().getApplication())
                            .setUserSelectedViewMode(FileManagerApp.LISTVIEW);
                    if (mSmbExplorer != null) {
                        mContentView.findViewById(R.id.file_top).setVisibility(View.VISIBLE);
                        mSmbExplorer.switchView(FileManagerApp.LISTVIEW);
                    }
                }
                return true;
            case R.id.gridView :
                if (!item.isChecked()) {
                    item.setChecked(true);
                    viewitem.setTitle(R.string.menu_view_grid);
                    viewitem.setIcon(R.drawable.ic_menu_grid_holo);
                    mContentView.findViewById(R.id.file_top).setVisibility(View.GONE);
                    mSortItem.setVisible(true);
                    setSmbSortCheckItem();
                    mSortItem.setTitle(getSmbSortItem());
                    ((FileManagerApp) getActivity().getApplication())
                            .setUserSelectedViewMode(FileManagerApp.GRIDVIEW);
                    if (mSmbExplorer != null) {
                        mSmbExplorer.switchView(FileManagerApp.GRIDVIEW);
                    }
                }
                return true;

            case R.id.sortByName :
                if (!item.isChecked()) {
                    item.setChecked(true);
                    mSortItem.setTitle(R.string.menu_sort_by_name);
                    if (mSmbExplorer != null) {
                        mSmbExplorer.setUpSmbViewSorting(FileManagerApp.NAMESORT);
                    }
                }
                return true;
            case R.id.sortByType :
                if (!item.isChecked()) {
                    item.setChecked(true);
                    mSortItem.setTitle(R.string.menu_sort_by_type);
                    if (mSmbExplorer != null) {
                        mSmbExplorer.setUpSmbViewSorting(FileManagerApp.TYPESORT);
                    }
                }
                return true;
            case R.id.sortByDate :
                if (!item.isChecked()) {
                    item.setChecked(true);
                    mSortItem.setTitle(R.string.menu_sort_by_date);
                    if (mSmbExplorer != null) {
                        mSmbExplorer.setUpSmbViewSorting(FileManagerApp.DATESORT);
                    }
                }
                return true;
            case R.id.sortBySize :
                if (!item.isChecked()) {
                    item.setChecked(true);
                    mSortItem.setTitle(R.string.menu_sort_by_size);
                    if (mSmbExplorer != null) {
                        mSmbExplorer.setUpSmbViewSorting(FileManagerApp.SIZESORT);
                    }
                }
                return true;

            case android.R.id.home :
                Fragment leftFragment = getFragmentManager().findFragmentById(R.id.home_page);
                if (leftFragment != null) {
                    if (leftFragment.getClass().getName()
                            .equals(RemoteContentLeftFragment.class.getName())) {

                        ((RemoteContentLeftFragment) leftFragment).upOneLevel();
                    }
                }
                if (upOneLevel() == true) {
                    return true;
                }

                return true;
            case R.id.cancelbutton :
                if (mFileManagerApp.mLaunchMode == FileManagerApp.NORMAL_MODE) {
                    FileManagerApp.setPasteMode(FileManagerApp.NO_PASTE);
                    FileManagerApp.setPasteFiles(null);
                    getActivity().invalidateOptionsMenu();
                } else {
                    getActivity().finish();
                }
                return true;
            case R.id.placebutton :
            case R.id.pastebutton :
                if (mSmbExplorer != null) {
                    mSmbExplorer.pasteFile();
                }
                FileManagerApp.setPasteMode(FileManagerApp.NO_PASTE);
                FileManagerApp.setPasteFiles(null);
                getActivity().invalidateOptionsMenu();
                return true;
            default :
                return true;
        }
    }

    private void setMultiSelectMode() {
        mSmbExplorer.setSmbExploreMultiSelectMode();
    }

    private void outMultiSelectMode() {
        mSmbExplorer.outSmbExploreMultiSelectMode();
    }

    public boolean upOneLevel() {
        if (whetherInFileList == true) {
            if (mSmbExplorer.getSmbSelectMode() == IconifiedTextListAdapter.MULTI_SELECT_MODE) {
                outMultiSelectMode();
            } else {
                if ((mSmbExplorer.getPathDepth()) > 0) {
                    mSmbExplorer.upOneLevel();
                } else {
                    showServerList();
                }
            }
        } else {
            if (mRemoteDeviceLayout.isShown()) {
                // when back is pressed, do not exit FileManager app,
                // instead, show default(local) content screen.
                initHomePage(false);

            } else if (mSmbGroupLayout.isShown()) {
                mSmbGroup.upOneLevel();
            }
        }
        return true;
    }

    public void onNewIntent(Intent intent) {

        ArrayList<String> filename = null;
        String intentAction = intent.getAction();
        if (intentAction == null) {
            return;
        }
        if (intentAction.equals(MainFileManagerActivity.ACTION_RESTART_ACTIVITY)) {
            showServerList();
        } else if (intentAction.equals(MainFileManagerActivity.ACTION_TRANSFERFINISHED)) {
            int transferResult =
                    intent.getIntExtra(MainFileManagerActivity.EXTRA_KEY_TRANSFERRESULT, 0);
            int transferredNum =
                    intent.getIntExtra(MainFileManagerActivity.EXTRA_KEY_TRANSFERREDNUM, 0);
            if (MainFileManagerActivity.EXTRA_VAL_REASONCUT.equals(intent
                    .getStringExtra(MainFileManagerActivity.EXTRA_KEY_PASTEREASON))) {
                mRemoteHandler.obtainMessage(MESSAGE_MOVE_FINISHED, transferResult, transferredNum)
                        .sendToTarget();
            } else {
                mRemoteHandler.obtainMessage(MESSAGE_COPY_FINISHED, transferResult, transferredNum)
                        .sendToTarget();
            }
        }

        else if (intentAction.equals(MainFileManagerActivity.ACTION_PASTE)) {

            filename =
                    intent.getStringArrayListExtra(MainFileManagerActivity.EXTRA_KEY_SOURCEFILENAME);
            if (filename == null) {
                SambaExplorer.log("onNewIntent action = ACTION_PASTE, filename = null", false);
                return;
            }
            if ((MainFileManagerActivity.EXTRA_VAL_REASONCUT).equals(intent
                    .getStringExtra(MainFileManagerActivity.EXTRA_KEY_PASTEREASON))) {

                showLoadingDialog(getString(R.string.moving), PROGRESS_DIALOG_LOADING_FILE);
            } else {
                showLoadingDialog(getString(R.string.copying), PROGRESS_DIALOG_LOADING_FILE);
            }

            Intent iDownload = new Intent(DownloadServer.ACTION_DOWNLOAD);
            iDownload.putStringArrayListExtra(MainFileManagerActivity.EXTRA_KEY_SOURCEFILENAME,
                    filename);
            iDownload.putExtra(MainFileManagerActivity.EXTRA_KEY_DESTFILENAME,
                    mSmbExplorer.getCurPath());
            iDownload.putExtra(MainFileManagerActivity.EXTRA_KEY_PASTEREASON,
                    intent.getStringExtra(MainFileManagerActivity.EXTRA_KEY_PASTEREASON));
            getActivity().getBaseContext().sendBroadcast(iDownload);

        }
    }

}
