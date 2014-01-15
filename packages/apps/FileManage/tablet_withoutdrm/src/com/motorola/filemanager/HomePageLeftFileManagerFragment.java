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
import java.util.List;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ClipData.Item;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.motorola.filemanager.local.LocalBaseFileManagerFragment;
import com.motorola.filemanager.local.LocalColumnViewFrameLeftFragment;
import com.motorola.filemanager.local.LocalLeftFileManagerFragment;
import com.motorola.filemanager.local.LocalRightFileManagerFragment;
import com.motorola.filemanager.local.threads.ThumbnailLoader;
import com.motorola.filemanager.samba.RemoteContentFragment;
import com.motorola.filemanager.ui.CustomDialogFragment;
import com.motorola.filemanager.ui.ExpandableHomeListAdapter;
import com.motorola.filemanager.ui.IconifiedText;

public class HomePageLeftFileManagerFragment extends BaseFileManagerFragment {
    private int mCurPosition = 0;
    private static int mCurrentGroupId = ExpandableHomeListAdapter.GROUP_DEVICE_POS;
    protected ExpandableHomeListAdapter mHomePageAdapter = null;
    private static List<IconifiedText> mHomePageHeaderItems = new ArrayList<IconifiedText>();
    private static List<IconifiedText> mHomePageStaticItems = new ArrayList<IconifiedText>();
    private static List<IconifiedText> mRecentFileItems = new ArrayList<IconifiedText>();
    private static List<IconifiedText> mShortcutItems = new ArrayList<IconifiedText>();

    private static String TAG = "HomePageLeftFileManagerFragment ";
    private Context mContext;
    private boolean mIsBack = false;
    protected ExpandableListView mExpandableListView = null;

    private ThumbnailLoader mThumbnailLoader = null;
    private Object mThumbnailLock = new Object();
    private IconifiedText mCurrHighlightedItem = null;
    private IconifiedText mPrevHighlightedItem = null;

    private int mContextShortcutPos;
    private PopupMenu mPopupMenu;

    public HomePageLeftFileManagerFragment() {
        super();
    }

    public HomePageLeftFileManagerFragment(int fragmentLayoutID, boolean isBack) {
        super(fragmentLayoutID);
        mIsBack = isBack;
        // TODO Auto-generated constructor stub
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        super.onCreateView(inflater, container, savedInstanceState);
        mExpandableListView = (ExpandableListView) mContentView.findViewById(R.id.expandList);
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
        return mContentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Current position should survive screen rotations.
        if (savedInstanceState != null) {
            mCurPosition = savedInstanceState.getInt("listPosition");
        }

        mContext = getActivity();

        initHomePageView();
    }

    @Override
    protected void handleMessage(Message message) {
        switch (message.what) {

            case FileManagerApp.MESSAGE_NEW_FOR_SHORTCUT :
                Fragment rightFrag = getFragmentManager().findFragmentById(R.id.details);
                if (rightFrag.getClass().getName().equals(
                        LocalRightFileManagerFragment.class.getName())) {
                    BaseFileManagerFragment det =
                            (BaseFileManagerFragment) getFragmentManager().findFragmentById(
                                    R.id.details);
                    File file = FileManagerApp.getCurrDir();
                    if (file != null) {
                        det.openClickedFile(file.getAbsolutePath(), null);
                        updateLeftFragment(file.getParentFile());
                    }
                }
                break;

            case FileManagerApp.MESSAGE_SHORTCUT_REMOVED :
                updateShortcuts();
                FileManagerApp.log(TAG +
                        " received MESSAGE_SHORTCUT_REMOVED, mShortcutItems.size()=" +
                        mShortcutItems.size(), false);
                break;
            case LocalBaseFileManagerFragment.MESSAGE_ICON_CHANGED :
                try {
                    if (mThumbnailLoader != null &&
                            ((int) mThumbnailLoader.getId()) == message.arg2) {
                        mRecentFileItems.get(message.arg1).setIcon((Drawable) message.obj);
                        if (mHomePageAdapter != null) {
                            mHomePageAdapter.notifyDataSetChanged();
                        }
                    } else if (mThumbnailLoader != null) {
                        FileManagerApp.log(TAG + "Thumbnail nochange" + message.arg2 + " " +
                                mThumbnailLoader.getId(), false);
                    }
                } catch (Exception e) {
                    FileManagerApp.log(TAG + "exception" + e.getMessage(), true);
                }
                break;
            case MESSAGE_SELECT_DRAG_LOCATION :

                IconifiedText text = (IconifiedText) message.obj;
                int childPosition = message.arg1;
                int childPosition1 = message.arg2;
                if (mHomePageAdapter != null) {
                    if (childPosition1 < mHomePageStaticItems.size()) {
                        handleDeviceSelection(text, childPosition);
                    } else if ((childPosition1 > mHomePageStaticItems.size()) &&
                            (childPosition1 <= mHomePageStaticItems.size() + mShortcutItems.size())) {
                        handleShortcutSelection(text, childPosition);
                    }

                }
                break;
            case MESSAGE_SELECT_CONTEXT_MENU :
                showShortcutContextMenu(message.obj, message.arg1);
                break;
        }
    }

    protected void showShortcutContextMenu(Object view, final int pos) {
        mContextShortcutPos = pos;

        if (mPopupMenu != null) {
            mPopupMenu.dismiss();
        }
        mPopupMenu = new PopupMenu(getActivity(), (View) view);
        Menu menu = mPopupMenu.getMenu();
        mPopupMenu.getMenuInflater().inflate(R.menu.shortcut_contextmenu, menu);
        mPopupMenu.show();
        mPopupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            //@Override
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                FragmentTransaction ft1 = getFragmentManager().beginTransaction();
                switch (item.getItemId()) {
                    case R.id.ShortcutContextMenuRename :
                        DialogFragment newFragment1 =
                                CustomDialogFragment
                                        .newInstance(CustomDialogFragment.DIALOG_SHORTCUT_RENAME,
                                                true, mContext);
                        // Show the dialog.
                        newFragment1.show(ft1, "dialog");
                        break;

                    case R.id.ShortcutContextMenuDelete :
                        DialogFragment newFragment2 =
                                CustomDialogFragment
                                        .newInstance(CustomDialogFragment.DIALOG_SHORTCUT_DELETE,
                                                true, mContext);
                        // Show the dialog.
                        newFragment2.show(ft1, "dialog");
                        break;
                    default :
                        break;
                }
                return true;
            }
        });
    }

    public void initHomePageView() {
        mContext = this.getActivity();
        initHomePage();
        if (!mIsBack) {
            File curdir =
                    new File("/mnt/sdcard");
            FileManagerApp.setCurrDir(curdir);
            mCurPosition = 0;
            updateRightFragment();

            if (mCurrHighlightedItem != null) {
                mCurrHighlightedItem.setHighlighted(false);
                mCurrHighlightedItem = null;
                mHomePageAdapter.notifyDataSetChanged();

            }
            mCurrentGroupId = ExpandableHomeListAdapter.GROUP_DEVICE_POS;
        }
        // Only need to et for mHomePageAdapter since when returning to Home Page, it is only possible
        // to be showing one of the foldres in the Home Page list
        mCurrHighlightedItem = mHomePageAdapter.getChild(mCurrentGroupId, mCurPosition);

        mHomePageAdapter.setHighlightedItem(mCurrHighlightedItem);

    }

    public void initHomePage() {
        mHomePageHeaderItems.clear();
        mHomePageStaticItems.clear();
        mRecentFileItems.clear();
        mShortcutItems.clear();
        FileManagerApp.setHomePage(true);
        mHomePageStaticItems.add(IconifiedText.buildHomePageItems(mContext,
                FileManagerApp.INDEX_INTERNAL_MEMORY, getString(R.string.internal_device_storage)));
        if (true) {
            mHomePageStaticItems.add(IconifiedText.buildHomePageItems(mContext,
                    FileManagerApp.INDEX_SD_CARD, getString(R.string.sd_card)));
        }
        for (int i = 0; i < FileManagerApp.MAX_USB_DISK_NUM; i++) {
            if (mFileManagerApp.isUsbDiskMounted(i)) {
                mHomePageStaticItems.add(IconifiedText.buildHomePageItems(mContext,
                        FileManagerApp.INDEX_USB, getString(R.string.usb_storage) + " " + (i + 1)));
                FileManagerApp.log(TAG + "initHomepage(), USB disk is added. Index=" + i, false);
            }
        }

        mHomePageStaticItems.add(IconifiedText.buildHomePageItems(mContext,
                FileManagerApp.INDEX_SAMBA, getString(R.string.remote_files)));

        mHomePageHeaderItems.add(IconifiedText.buildHomePageItems(mContext,
                FileManagerApp.INDEX_HEADER, getString(R.string.devices)));
        mHomePageHeaderItems.add(IconifiedText.buildHomePageItems(mContext,
                FileManagerApp.INDEX_HEADER, getString(R.string.shortcuts)));
        mHomePageHeaderItems.add(IconifiedText.buildHomePageItems(mContext,
                FileManagerApp.INDEX_HEADER, getString(R.string.recentfile)));

        updateRecentFiles();
        FileManagerApp.log(TAG + "number of recent files=" + mRecentFileItems.size(), false);
        updateShortcuts();
        FileManagerApp.log(TAG + "number of shortcut=" + mShortcutItems.size(), false);

        mHomePageAdapter =
                new ExpandableHomeListAdapter(getActivity(), mHomePageHeaderItems, mCurrentHandler);

        if (mHomePageAdapter != null) {
            mHomePageAdapter.setChildItems(mRecentFileItems,
                    ExpandableHomeListAdapter.GROUP_RECENTFILE_POS);
            mHomePageAdapter.setChildItems(mShortcutItems,
                    ExpandableHomeListAdapter.GROUP_SHORTCUT_POS);
            mHomePageAdapter.setChildItems(mHomePageStaticItems,
                    ExpandableHomeListAdapter.GROUP_DEVICE_POS);
            if (mExpandableListView != null) {
                mExpandableListView.setAdapter(mHomePageAdapter);
                mExpandableListView.setOnChildClickListener(childListListener);
                mExpandableListView.expandGroup(ExpandableHomeListAdapter.GROUP_DEVICE_POS);
                mExpandableListView.expandGroup(ExpandableHomeListAdapter.GROUP_SHORTCUT_POS);
                mExpandableListView.expandGroup(ExpandableHomeListAdapter.GROUP_RECENTFILE_POS);
            }
            mHomePageAdapter.notifyDataSetChanged();
        }

    }

    private void getRecentFilesFromPreferences() {
        SharedPreferences sp =
                getActivity().getSharedPreferences(FileManagerApp.FILE_MANAGER_PREFERENCES, 0);
        if (sp == null) {
            return;
        }
        String thePreference = sp.getString(FileManagerApp.FILE_MANAGER_PREFERENCE_RECENT_FILE, "");
        FileManagerApp.log(TAG + "key=" + FileManagerApp.FILE_MANAGER_PREFERENCE_RECENT_FILE +
                ",preference=" + thePreference, false);
        if (thePreference == null || thePreference.length() == 0) {
        } else {
            String[] items = thePreference.split(",");
            if (items != null) {
                try {
                    mRecentFileItems.clear();
                    for (int i = 0; i < items.length &&
                            i < FileManagerApp.MAX_NUMBER_OF_RECENT_FILES; ++i) {
                        items[i].trim();
                        if (items[i].length() == 0) {
                            continue;
                        }
                        IconifiedText item = IconifiedText.buildRecentFileItem(mContext, items[i]);
                        if (item != null) {
                            mRecentFileItems.add(item);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void getShortcutsFromPreferences() {
        SharedPreferences sp =
                getActivity().getSharedPreferences(FileManagerApp.FILE_MANAGER_PREFERENCES, 0);
        if (sp == null) {
            return;
        }
        int totalShortcuts =
                sp.getInt(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_TOTAL_NUMBER, 0);
        FileManagerApp.log(TAG + "totalShortcuts=" + totalShortcuts, false);
        try {
            mShortcutItems.clear();

            for (int i = 1; i <= totalShortcuts; ++i) {
                String shortcutName =
                        sp.getString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_NAME +
                                String.valueOf(i), "");
                String shortcutPath =
                        sp.getString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_PATH +
                                String.valueOf(i), "");
                shortcutName.trim();
                shortcutPath.trim();
                if (shortcutName.length() == 0 || shortcutPath.length() == 0) {
                    continue;
                }
                IconifiedText item =
                        IconifiedText.buildShortcutItem(mContext, shortcutName, shortcutPath);
                if (item != null) {
                    mShortcutItems.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateRecentFiles() {
        getRecentFilesFromPreferences();
        if (mHomePageAdapter != null) {
            mHomePageAdapter.notifyDataSetChanged();
        }
        mThumbnailLoader = new ThumbnailLoader(mRecentFileItems, mCurrentHandler);
        if (mThumbnailLoader != null) {
            synchronized (mThumbnailLock) {
                mThumbnailLoader.setContext(mContext);
                mThumbnailLoader.start();
            }
        }
    }

    @Override
    public void updateShortcuts() {
        getShortcutsFromPreferences();
        if (mHomePageAdapter != null) {
            mHomePageAdapter.notifyDataSetChanged();
        }

    }

    //Drag and drop processing
    boolean processDrop(DragEvent event) { // Attempt to parse clip data      

        try {
            ClipData data = event.getClipData();
            if (data != null) {
                if (data.getItemCount() > 0) {
                    Item item = data.getItemAt(0);
                    String srcData = (String) item.getText();
                    if (srcData != null) {
                        BaseFileManagerFragment det =
                                (BaseFileManagerFragment) getFragmentManager().findFragmentById(
                                        R.id.details);
                        if (det != null) {
                            if (FileManagerApp.getStorageMediumMode() == Integer.parseInt(srcData)) {
                                FileManagerApp.setPasteMode(FileManagerApp.MOVE_MODE);
                            } else {
                                FileManagerApp.setPasteMode(FileManagerApp.COPY_MODE);
                            }
                            det.handlePaste(FileManagerApp.getPasteFiles(), FileManagerApp
                                    .getCurrDir().getPath());
                            FileManagerApp.setPasteMode(FileManagerApp.NO_PASTE);
                            FileManagerApp.setPasteFiles(null);
                        }

                        return true;
                    }
                }
            }
        } catch (Exception e) {
        }

        return false;
    }

    ExpandableListView.OnChildClickListener childListListener =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mHomePageAdapter == null) {
                        return false;
                    }
                    IconifiedText text = mHomePageAdapter.getChild(groupPosition, childPosition);
                    if (groupPosition == ExpandableHomeListAdapter.GROUP_DEVICE_POS) {
                        handleDeviceSelection(text, childPosition);
                    } else if (groupPosition == ExpandableHomeListAdapter.GROUP_SHORTCUT_POS) {
                        handleShortcutSelection(text, childPosition);
                    } else if (groupPosition == ExpandableHomeListAdapter.GROUP_RECENTFILE_POS) {
                        handleRecentfileSelection(text, childPosition);
                    }
                    return true;
                }
            };

    private void handleDeviceSelection(IconifiedText text, int position) {
        mHomePageAdapter.clearCurrentHighlightedItem(mCurrHighlightedItem);
        mPrevHighlightedItem = mCurrHighlightedItem;
        mHomePageAdapter.setHighlightedItem(text);
        mCurrHighlightedItem = text;
        mCurPosition = position;
        mCurrentGroupId = ExpandableHomeListAdapter.GROUP_DEVICE_POS;
        int storageMode = FileManagerApp.INDEX_INTERNAL_MEMORY;
        String storageName = "";
        if (text != null) {
            storageMode = text.getStorageMode();
            storageName = text.getText();
        }
        FileManagerApp fmApp = mFileManagerApp;

        switch (storageMode) {
            case FileManagerApp.INDEX_INTERNAL_MEMORY :
                fmApp.setPhoneStoragePath(FileManagerApp.INDEX_INTERNAL_MEMORY, -1);
                File curdir =
                        new File(fmApp
                                .getDeviceStorageVolumePath(FileManagerApp.INDEX_INTERNAL_MEMORY));
                FileManagerApp.setCurrDir(curdir);
                updateRightFragment();
                break;
            case FileManagerApp.INDEX_SD_CARD :
                fmApp.setPhoneStoragePath(FileManagerApp.INDEX_SD_CARD, -1);
                File curdir1 =
                        new File(fmApp.getDeviceStorageVolumePath(FileManagerApp.INDEX_SD_CARD));
                FileManagerApp.setCurrDir(curdir1);
                updateRightFragment();
                break;
            case FileManagerApp.INDEX_USB :
                updateUsbPath(storageName);
                updateRightFragment();
                break;
            case FileManagerApp.INDEX_SAMBA :
                if (!((MainFileManagerActivity) getActivity()).checkConnAvailable()) {
                    (new AlertDialog.Builder(mContext)).setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(R.string.wifi).setMessage(R.string.wifi_unavaiable)
                            .setPositiveButton(R.string.open_wifi,
                                    new DialogInterface.OnClickListener() {
                                        //@Override
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent =
                                                    new Intent(Settings.ACTION_WIFI_SETTINGS);
                                            mContext.startActivity(intent);
                                            //mWiFiDialogDisplaying = false;
                                        }
                                    })
                            .setNegativeButton(android.R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        //@Override
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mHomePageAdapter
                                                    .clearCurrentHighlightedItem(mCurrHighlightedItem);
                                            mHomePageAdapter
                                                    .setHighlightedItem(mPrevHighlightedItem);
                                            mCurrHighlightedItem = mPrevHighlightedItem;

                                        }
                                    }).create().show();

                } else {
                    updateRemoteRightFragment(position);
                }
                break;
        }
    }

    private void handleRecentfileSelection(IconifiedText text, int position) {
        if (text != null) {
            String file = text.getText();
            String path = text.getPathInfo();
            FileManagerApp.log(TAG + "recent file=" + file + ",path=" + path, false);
            if (isStorageDeviceMounted(path)) {
                File sf = new File(path);
                if (sf.exists()) {
                    openFile(sf);
                } else {
                    FileManagerApp.log(TAG + " " + path + " does not exist.", true);
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.setMessage(R.string.recent_file_deleted);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setMessage(R.string.recent_file_storage_unmount);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }

    public void updateColumnFragment() {
        LocalColumnViewFrameLeftFragment df =
                new LocalColumnViewFrameLeftFragment(R.layout.column_view);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.details, df);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();

    }

    private void handleShortcutSelection(IconifiedText text, int position) {
        if (text != null) {
            mHomePageAdapter.clearCurrentHighlightedItem(mCurrHighlightedItem);
            mHomePageAdapter.setHighlightedItem(text);
            mCurrHighlightedItem = text;
            mCurPosition = position;
            mCurrentGroupId = ExpandableHomeListAdapter.GROUP_SHORTCUT_POS;
            final String path = text.getPathInfo();
            FileManagerApp.log(TAG + "selected shortcut path =" + path, false);
            if (isStorageDeviceMounted(path)) {
                File sf = new File(path);
                if (sf.exists()) {
                    FileManagerApp.setCurrDir(sf);
                    Fragment rightFragment = getFragmentManager().findFragmentById(R.id.details);
                    if ((rightFragment != null) &&
                            (rightFragment.getClass().getName()
                                    .equals(LocalRightFileManagerFragment.class.getName()))) {
                        if (sf.isDirectory()) {
                            ((BaseFileManagerFragment) rightFragment).clearActionMode();
                        }
                        ((BaseFileManagerFragment) rightFragment).openClickedFile(path, null);
                        updateLeftFragment(sf.getParentFile());
                    } else {
                        for (int i = 0; i < getFragmentManager().getBackStackEntryCount(); ++i) {
                            getFragmentManager().popBackStackImmediate();
                        }
                        if (mFileManagerApp.getViewMode() == FileManagerApp.COLUMNVIEW) {
                            updateRightFragment();
                        } else {
                            LocalRightFileManagerFragment det =
                                    LocalRightFileManagerFragment.newInstance();
                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                            ft.replace(R.id.details, det);
                            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                            ft.commit();
                            Message msg = Message.obtain();
                            if ((msg != null) && (mCurrentHandler != null)) {
                                msg.what = FileManagerApp.MESSAGE_NEW_FOR_SHORTCUT;
                                mCurrentHandler.sendMessage(msg);
                            }
                        }
                    }
                } else {
                    FileManagerApp.log(TAG + " " + path + " does not exist.", true);
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setNegativeButton(android.R.string.cancel, null);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (mFileManagerApp.removeShortcut(path)) {
                                updateShortcuts();
                            }
                        }
                    });
                    builder.setMessage(R.string.shortcut_deleted);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mFileManagerApp.removeShortcut(path)) {
                            updateShortcuts();
                        }
                    }
                });
                builder.setMessage(R.string.shortcut_storage_unmount);
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }

    private boolean isStorageDeviceMounted(String filepath) {
        if (filepath == null || filepath.length() == 0) {
            return false;
        }
        FileManagerApp fmApp = mFileManagerApp;
        if (filepath.indexOf(fmApp.SD_CARD_DIR) == 0) {
            return true;
        } else if (filepath.indexOf(fmApp.SD_CARD_EXT_DIR) == 0) {
            if (fmApp.isSDcardMounted()) {
                return true;
            } else {
                return false;
            }
        } else {
            for (int i = 0; i < FileManagerApp.MAX_USB_DISK_NUM; i++) {
                if (filepath.indexOf(mFileManagerApp.mUsbDiskListPath[i]) == 0) {
                    if (fmApp.isUsbDiskMounted(i)) {
                        FileManagerApp.log(TAG + "USB disk " + i + "is mounted", false);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }
    }

    private void updateUsbPath(String storageName) {
        FileManagerApp fmApp = mFileManagerApp;
        if (storageName != null) {
            if (storageName.equals(getString(R.string.usb_storage) + " " +
                    (FileManagerApp.USB_DISK_LIST_INDEX_1 + 1))) {
                fmApp.setPhoneStoragePath(FileManagerApp.INDEX_USB,
                        FileManagerApp.USB_DISK_LIST_INDEX_1);
                File curdir2 =
                        new File(fmApp
                                .getDeviceStorageVolumePath(FileManagerApp.USB_DISK_LIST_INDEX_1 +
                                        FileManagerApp.INDEX_USB));
                FileManagerApp.setCurrDir(curdir2);
                FileManagerApp.log(TAG + "onItemClick, setPhoneStoragePath with index=" +
                        FileManagerApp.USB_DISK_LIST_INDEX_1, false);
            } else if (storageName.equals(getString(R.string.usb_storage) + " " +
                    (FileManagerApp.USB_DISK_LIST_INDEX_2 + 1))) {
                fmApp.setPhoneStoragePath(FileManagerApp.INDEX_USB,
                        FileManagerApp.USB_DISK_LIST_INDEX_2);
                FileManagerApp.log(TAG + "onItemClick, setPhoneStoragePath with index=" +
                        FileManagerApp.USB_DISK_LIST_INDEX_2, false);
                File curdir2 =
                        new File(fmApp
                                .getDeviceStorageVolumePath(FileManagerApp.USB_DISK_LIST_INDEX_2 +
                                        FileManagerApp.INDEX_USB));
                FileManagerApp.setCurrDir(curdir2);
            } else if (storageName.equals(getString(R.string.usb_storage) + " " +
                    (FileManagerApp.USB_DISK_LIST_INDEX_3 + 1))) {
                fmApp.setPhoneStoragePath(FileManagerApp.INDEX_USB,
                        FileManagerApp.USB_DISK_LIST_INDEX_3);
                FileManagerApp.log(TAG + "onItemClick, setPhoneStoragePath with index=" +
                        FileManagerApp.USB_DISK_LIST_INDEX_3, false);
                File curdir2 =
                        new File(fmApp
                                .getDeviceStorageVolumePath(FileManagerApp.USB_DISK_LIST_INDEX_3 +
                                        FileManagerApp.INDEX_USB));
                FileManagerApp.setCurrDir(curdir2);
            } else if (storageName.equals(getString(R.string.usb_storage) + " " +
                    (FileManagerApp.USB_DISK_LIST_INDEX_4 + 1))) {
                fmApp.setPhoneStoragePath(FileManagerApp.INDEX_USB,
                        FileManagerApp.USB_DISK_LIST_INDEX_4);
                FileManagerApp.log(TAG + "onItemClick, setPhoneStoragePath with index=" +
                        FileManagerApp.USB_DISK_LIST_INDEX_4, false);
                File curdir2 =
                        new File(fmApp
                                .getDeviceStorageVolumePath(FileManagerApp.USB_DISK_LIST_INDEX_4 +
                                        FileManagerApp.INDEX_USB));
                FileManagerApp.setCurrDir(curdir2);
            }
        }

    }

    @Override
    public void updateLeftFragment(File clickedFile) {
        if (mFileManagerApp.getViewMode() == FileManagerApp.COLUMNVIEW) {
            // Left fragment remains as Home screen in Column View so do nothing
            return;
        }
        LocalLeftFileManagerFragment df =
                new LocalLeftFileManagerFragment(R.layout.title_file_list, clickedFile, null);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.home_page, df);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    public void updateRightColumnFragment() {
        BaseFileManagerFragment det = null;
        try {
            //prevent the forceclose due to casting failure
            det = (BaseFileManagerFragment) getFragmentManager().findFragmentById(R.id.details);
        } catch (Exception e) {
        }
        if (det != null) {
            det.clearActionMode();
            det.openClickedFile(FileManagerApp.getCurrDir().getPath(), null);
        } else {
            LocalColumnViewFrameLeftFragment df =
                    new LocalColumnViewFrameLeftFragment(R.layout.column_view);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.details, df);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }
    }

    @Override
    public void updateRightFragment() {
        if (mFileManagerApp.getViewMode() == FileManagerApp.COLUMNVIEW) {
            updateRightColumnFragment();
        } else {
            BaseFileManagerFragment det = null;
            try {
                //prevent the forceclose due to casting failure
                det = (BaseFileManagerFragment) getFragmentManager().findFragmentById(R.id.details);
            } catch (Exception e) {
            }
            if (det != null) {
                det.clearActionMode();
                det.openClickedFile(FileManagerApp.getCurrDir().getPath(), null);
            } else {
                LocalRightFileManagerFragment df = LocalRightFileManagerFragment.newInstance();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.details, df);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }

            // Show the search icon instead of the widget when the fragment is updated.
            if ((FileManagerApp.getPasteMode() == FileManagerApp.NO_PASTE) && (mContext != null)) {
                ((MainFileManagerActivity) mContext).hideSearchWidget();
            }
        }
    }

    private void updateRemoteRightFragment(int position) {
        BaseFileManagerFragment det = null;
        try {
            //prevent the forceclose due to casting failure
            det = (BaseFileManagerFragment) getFragmentManager().findFragmentById(R.id.details);
        } catch (Exception e) {
        }
        if (det != null) {
            det.clearActionMode();
        }
        RemoteContentFragment df = RemoteContentFragment.newInstance(position);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.details, df);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("listPosition", mCurPosition);
    }

    /* Drag and Drop Processing */

    @Override
    protected boolean onDragStarted(DragEvent event) {
        int itemCount = mExpandableListView.getChildCount();
        // Lazily initialize the height of our list items
        if (itemCount > 0 && mDragItemHeight < 0 && mExpandableListView.getChildAt(0) != null) {
            mDragItemHeight = mExpandableListView.getChildAt(0).getHeight();
        }
        return true;
    }

    @Override
    protected boolean onDrop(DragEvent event) {
        int rawTouchY = (int) event.getY();
        int targetAdapterPosition = getItemDragOrDrop(rawTouchY);
        // Fow now handle only Device List, shortcut will be added in next drop since shorcut handling is slightly differnt
        if (!(targetAdapterPosition > mHomePageStaticItems.size())) {
            processDrop(event);
        } else if ((targetAdapterPosition > mHomePageStaticItems.size()) &&
                (targetAdapterPosition <= mHomePageStaticItems.size() + mShortcutItems.size())) { // It is a shortcut
            BaseFileManagerFragment det =
                    (BaseFileManagerFragment) getFragmentManager().findFragmentById(R.id.details);
            if (det != null) {
                ArrayList<String> pasteFiles = FileManagerApp.getPasteFiles();
                for (int i = 0; i < pasteFiles.size(); i++) {
                    File temp = new File(pasteFiles.get(i));
                    det.addShortcut(temp);
                }
                FileManagerApp.setPasteMode(FileManagerApp.NO_PASTE);
                FileManagerApp.setPasteFiles(null);
                ((MainFileManagerActivity) getActivity()).setIsDropSuccess(true);
            }
        }

        return true;
    }

    private int getItemDragOrDrop(int Y) {
        int position = -1;
        int offset = 0;
        int rawTouchY = Y;

        if (mExpandableListView != null && mExpandableListView.getCount() > 1 &&
                mExpandableListView.getChildAt(0) != null) {
            offset = mExpandableListView.getChildAt(0).getTop();

            int targetScreenPosition = (rawTouchY - offset) / mDragItemHeight;

            int firstVisibleItem = mExpandableListView.getFirstVisiblePosition();
            int targetAdapterPosition = firstVisibleItem + targetScreenPosition;
            FileManagerApp.log(TAG + "DROP TARGET " + mDropTargetAdapterPosition + " -> " +
                    targetAdapterPosition, false);
            return targetAdapterPosition;
        } else {
            return position;
        }
    }

    @Override
    protected void onDragLocation(DragEvent event) {
        super.onDragLocation(event);
        // Find out which item we're in and highlight as appropriate

        //Logic to check if we are still dragging the shadow or we are hovering on a view for sometime.
        //boolean isDrag = true;
        // isDrag = checkisDrag(rawTouchY);

        int rawTouchY = (int) event.getY();
        IconifiedText newActTarget = null;
        int targetAdapterPosition = getItemDragOrDrop(rawTouchY);
        try {
            // Fow now handle only Device List, shortcut will be added in next drop since shorcut handling is slightly differnt
            if (!(targetAdapterPosition > mHomePageStaticItems.size())) {
                newActTarget =
                        mHomePageAdapter.getChild(ExpandableHomeListAdapter.GROUP_DEVICE_POS,
                                targetAdapterPosition - 1);
                // This can be null due to a bug in the framework (checking on that)
                // In any event, we're no longer dragging in the list view if newTarget is null
                if (newActTarget == null) {
                    FileManagerApp.log(TAG + "DRAG EXITED", false);
                    onDragExited();
                    return;
                }
                // Save away our current position and view
                mDropTargetAdapterPosition = targetAdapterPosition;
                mDropTargetView = newActTarget;
                mDropTargetPath = mDropTargetView.getPathInfo();
                if (newActTarget != null) {
                    int storageMode = newActTarget.getStorageMode();
                    int currMode = FileManagerApp.getStorageMediumMode();
                    /* Detect how long the shadow is hovering on a target list item in this case, if its a device, 
                     * it is more than the offset , it means we need to open the device for further processing*/
                    File dir = new File(newActTarget.getPathInfo());
                    File curr = FileManagerApp.getCurrDir();
                    if (currMode != storageMode && storageMode != FileManagerApp.INDEX_SAMBA &&
                            (!dir.equals(curr))) {
                        sendHovertoTarget(newActTarget, targetAdapterPosition);//the drag shadow is hovering send event to open the device in this case                     
                    }

                }
            } else if ((targetAdapterPosition > mHomePageStaticItems.size()) &&
                    (targetAdapterPosition <= mHomePageStaticItems.size() + mShortcutItems.size())) { // It is a shortcut
                int childPos = targetAdapterPosition;
                targetAdapterPosition -= mHomePageStaticItems.size() + 1;
                newActTarget =
                        mHomePageAdapter.getChild(ExpandableHomeListAdapter.GROUP_SHORTCUT_POS,
                                targetAdapterPosition - 1);
                // This can be null due to a bug in the framework (checking on that)
                // In any event, we're no longer dragging in the list view if newTarget is null
                if (newActTarget == null) {
                    FileManagerApp.log(TAG + " DRAG EXITED", false);
                    onDragExited();
                    return;
                }
                // Save away our current position and view
                mDropTargetAdapterPosition = targetAdapterPosition;
                mDropTargetView = newActTarget;
                mDropTargetPath = mDropTargetView.getPathInfo();
                File dir = new File(newActTarget.getPathInfo());
                File curr = FileManagerApp.getCurrDir();
                if (newActTarget != null && !(dir.equals(curr))) {
                    sendHovertoTarget(newActTarget, childPos);
                }

            }
            // This is implementation of drag-under-scroll since Framework is not supporting it yet
            dragUnderScroll(mExpandableListView, rawTouchY);

        } catch (Exception e) {

        }
    }

    public void renameCurrentShortcut(String newShortcutName) {
        if (getActivity() != null) {
            if ((mFileManagerApp != null) &&
                    (mFileManagerApp.renameShortcut(mContextShortcutPos, newShortcutName))) {
                updateShortcuts();
            }
        }
    }

    public void deleteCurrentShortcut() {
        if (getActivity() != null) {
            if ((mFileManagerApp != null) &&
                    (mFileManagerApp.removeShortcutByPosition(mContextShortcutPos))) {
                updateShortcuts();
            }
        }
    }

}
