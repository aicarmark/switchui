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

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.motorola.android.storage.MotoEnvironment;
import com.motorola.filemanager.samba.service.DownloadServer;

public class FileManagerApp extends Application {
    public static final int NORMAL_MODE = 250;
    public static final int SELECT_FILE_MODE = 251;
    public static final int SELECT_FOLDER_MODE = 252;
    public static final int SELECT_GET_CONTENT = 253;
    static final int ID_LOCAL = 0;
    static final int ID_REMOTE = 1;
    static final int ID_SDCARD = 2;
    static final int ID_USB = 3; // USB Host
    static final int ID_MOTOCONNECT = 4;
    public static final int INDEX_INTERNAL_MEMORY = 0;
    public static final int INDEX_SD_CARD = 1;
    public static final int INDEX_USB = 2;
    public static final int INDEX_SAMBA = 3;
    public static final int INDEX_HEADER = 4;
    public static final int MAX_NUMBER_OF_RECENT_FILES = 7;
    public static final int DEFAULT_SHORTCUTS = 4;
    public static final int MESSAGE_RECENT_FILE_REMOVED = 101;
    public static final int MESSAGE_SHORTCUT_REMOVED = 102;
    public static final int MESSAGE_NEW_FOR_SHORTCUT = 103;
    public static final int MESSAGE_NEW_FOR_RECENTFILE = 104;
    private File[] mStorageVolumes;
    protected static File mPreviousDirectory = null;
    protected static File mCurrentDirectory = new File("");

    public int mLaunchMode = NORMAL_MODE;
    private static String TAG = "FileManagerApp";
    public static enum INDEX_LOG_MODE {
        INDEX_LOG_NONE, INDEX_LOG_ERROR, INDEX_LOG_ALL;
    }

    public static INDEX_LOG_MODE DBG = INDEX_LOG_MODE.INDEX_LOG_ERROR;

    public final static String ROOT_DIR = "/mnt";
    public String SD_CARD_DIR = null;
    public String SD_CARD_EXT_DIR = null;
    public static final int MAX_USB_DISK_NUM = 4;
    public static final int USB_DISK_LIST_INDEX_1 = 0;
    public static final int USB_DISK_LIST_INDEX_2 = 1;
    public static final int USB_DISK_LIST_INDEX_3 = 2;
    public static final int USB_DISK_LIST_INDEX_4 = 3;
    public static final int PAGE_CONTENT_NORMAL = 1;
    public static final int PAGE_CONTENT_SEARCH = 2;
    public static int mBrowseMode = PAGE_CONTENT_NORMAL;
    public static boolean mUsbDiskConnected = false;
    public static int mUsbDiskNum = 0;
    public String mUsbDiskListPath[] = null;

    public String mCurrentUsbPath;

    public static final String FILE_MANAGER_PREFERENCES = "FILE_MANAGER_PREFERENCES";
    public static final String FILE_MANAGER_PREFERENCE_RECENT_FILE =
            "FILE_MANAGER_PREFERENCE_RECENT_FILE";
    public static final String FILE_MANAGER_PREFERENCE_SHORTCUT_TOTAL_NUMBER =
            "FILE_MANAGER_PREFERENCE_SHORTCUT_TOTAL_NUMBER";
    public static final String FILE_MANAGER_PREFERENCE_SHORTCUT_NAME =
            "FILE_MANAGER_PREFERENCE_SHORTCUT_NAME_";
    public static final String FILE_MANAGER_PREFERENCE_SHORTCUT_PATH =
            "FILE_MANAGER_PREFERENCE_SHORTCUT_PATH_";

    public static final int GRIDVIEW = 2;
    public static final int LISTVIEW = 1;
    public static final int COLUMNVIEW = 3;
    private int mViewMode = LISTVIEW;
    private static boolean mIsTitleFragment = false;
    private static boolean mShowFolderViewIcon = true;
    private static final String SETTINGS = "settings";
    private static final String VIEWMODE = "view mode";
    private static final String SORTMODE = "sort mode";

    public static final int NAMESORT = 300;
    public static final int TYPESORT = 301;
    public static final int DATESORT = 302;
    public static final int SIZESORT = 303;
    public static final int INTERNALSTORAGE = 0;
    public static final int SDCARD = 1;
    private static int mSearchOption = INTERNALSTORAGE;
    private static final String SEARCHOPTION = "search option";
    private static final String LAUNCHSETTING = "launch setting";
    static volatile boolean mLaunchSetting = false;

    public static enum INDEX_STORAGE_MODE {
        INDEX_INTERNAL_MEMORY, INDEX_EXTERNAL_SDCARD, INDEX_SAMBA, INDEX_USB, INDEX_MOTOCONNECT;
    }

    private String mStoragePath;
    private Intent mContentIntent;
    private static boolean mHomePage = false;

    private String mStorageExternal;

    private static int mStorgeMediumMode = 0;

    // Sort Changes

    public boolean mIsDescendingType = false;
    public boolean mIsDescendingName = false;
    public boolean mIsDescendingSize = false;
    public boolean mIsDescendingDate = false;

    public boolean mSearchIsDcendingType = false;
    public boolean mSearchIsDcendingName = false;
    public boolean mSearchIsDcendingSize = false;
    public boolean mSearchIsDcendingTime = false;

    public static final int Samba_Explorer_LAUNCH = 200;
    public static final int Samba_Explorer_PRINT = 201;
    public static String SAMBA_DIR = "smb://";
    private static ArrayList<String> mPasteFiles;
    public static final int NO_PASTE = 350;
    public static final int MOVE_MODE = 351;
    public static final int COPY_MODE = 352;
    private static int mPasteMode = NO_PASTE;

    public static void setPasteMode(int inPasteMode) {
        mPasteMode = inPasteMode;
    }

    public static int getPasteMode() {
        return mPasteMode;
    }

    public static void setPasteFiles(ArrayList<String> files) {
        mPasteFiles = files;
    }

    public static ArrayList<String> getPasteFiles() {
        return mPasteFiles;
    }

    public void clearSortFlags() {
        mIsDescendingType = false;
        mIsDescendingName = false;
        mIsDescendingSize = false;
        mIsDescendingDate = false;
        mSearchIsDcendingType = false;
        mSearchIsDcendingName = false;
        mSearchIsDcendingSize = false;
        mSearchIsDcendingTime = false;

    }

    public void setLaunchMode(int launchMode) {
        mLaunchMode = launchMode;
    }

    public int getLaunchMode() {
        return mLaunchMode;
    }

    public int getViewMode() {
        return mViewMode;
    }

    public void setViewMode(int view) {
        mViewMode = view;
    }

    public int getUserSelectedViewMode() {
        SharedPreferences setting = getSharedPreferences(SETTINGS, 0);
        int viewMode = setting.getInt(VIEWMODE, LISTVIEW);
        return viewMode;
    }

    public void setUserSelectedViewMode(int view) {
        // This should only be called from onOptionsItemSelected() in the Activity

        SharedPreferences viewSetting = getSharedPreferences(SETTINGS, 0);
        SharedPreferences.Editor editor = viewSetting.edit();
        editor.putInt(VIEWMODE, view);
        editor.apply();
    }

    public static boolean isHomePage() {
        return mHomePage;
    }

    public static void setHomePage(boolean isHomePage) {
        mHomePage = isHomePage;
    }

    /*public synchronized void resetGridViewMode() {
        SharedPreferences setting = getSharedPreferences(SETTINGS, 0);
        mViewMode = setting.getInt(VIEWMODE, LISTVIEW);
       
    }*/

    public static boolean isTitleFragment() {
        return mIsTitleFragment;
    }

    public void setSortMode(int sortmode) {
        SharedPreferences setting = getSharedPreferences(SETTINGS, 0);
        SharedPreferences.Editor editor = setting.edit();
        editor.putInt(SORTMODE, sortmode);
        editor.apply();
    }

    public int getSortMode() {
        SharedPreferences setting = getSharedPreferences(SETTINGS, 0);
        int sortMode = setting.getInt(SORTMODE, LISTVIEW);
        return sortMode;
    }

    public static boolean isShowFolderViewIcon() {
        return mShowFolderViewIcon;
    }

    public static void setShowFolderViewIcon(boolean show) {
        mShowFolderViewIcon = show;
    }

    public FileManagerApp() {
        super();
    }

    public synchronized void setSearchOption(int searchOption) {
        log("Set search option to" + searchOption, false);
        mSearchOption = searchOption;
        SharedPreferences setting = getSharedPreferences(SETTINGS, 0);
        SharedPreferences.Editor editor = setting.edit();
        editor.putInt(SEARCHOPTION, mSearchOption);
        editor.apply();
    }

    public static int getSearchOption() {
        return mSearchOption;
    }

    public synchronized void setLaunchSetting(Boolean mode) {
        log("Set launch setting to" + mode, false);
        mLaunchSetting = mode;
        SharedPreferences launchsetting = getSharedPreferences(SETTINGS, 0);
        SharedPreferences.Editor editor = launchsetting.edit();
        editor.putBoolean(LAUNCHSETTING, mLaunchSetting);
        editor.apply();
    }

    public synchronized void initLaunchSetting(Boolean mode) {
        log("Set launch setting to" + mode, false);
        mLaunchSetting = mode;
    }

    public static Boolean getLaunchSetting() {
        return mLaunchSetting;
    }

    public void initStorageValue() {
        try {
          /*  mUsbDiskListPath =
                    new String[]{getDeviceStorageVolumePath(USB_DISK_LIST_INDEX_1 + INDEX_USB),
                            getDeviceStorageVolumePath(USB_DISK_LIST_INDEX_2 + INDEX_USB),
                            getDeviceStorageVolumePath(USB_DISK_LIST_INDEX_3 + INDEX_USB),
                            getDeviceStorageVolumePath(USB_DISK_LIST_INDEX_4 + INDEX_USB)};*/
            SD_CARD_DIR = "/mnt/sdcard";
            SD_CARD_EXT_DIR = "/mnt/ext_sdcard";
        } catch (Exception e) {

        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initStorageValue();
        startDownloadServer();
        SharedPreferences setting = getSharedPreferences(SETTINGS, 0);
        initLaunchSetting(setting.getBoolean(LAUNCHSETTING, false));
        setSortMode(setting.getInt(SORTMODE, NAMESORT));
        try {
            //mStorageVolumes = MotoEnvironment.getExternalStorageDirectories();

            if (SD_CARD_DIR.length() == 0) {
                SD_CARD_DIR = "/mnt/sdcard";
            }

            if (SD_CARD_EXT_DIR.length() == 0) {
                SD_CARD_EXT_DIR = "/mnt/ext_sdcard";
            }

            /*if (mUsbDiskListPath == null || mUsbDiskListPath.length != MAX_USB_DISK_NUM) {
                mUsbDiskListPath = new String[MAX_USB_DISK_NUM];
            }
            for (int i = 0; i < MAX_USB_DISK_NUM; i++) {
                try {
                    mUsbDiskListPath[i] = getDeviceStorageVolumePath(INDEX_USB + i);
                    log("in onCreate, USB path= " + mUsbDiskListPath[i], false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }*/
        } catch (Exception e) {

        }
        if (!mLaunchSetting) {
            log("1st time launch. mLaunchSetting=" + mLaunchSetting, false);
            initShortcuts();
            setLaunchSetting(true);
        }

    }

    private void initShortcuts() {
        String[] shortcutPaths = new String[DEFAULT_SHORTCUTS];
        shortcutPaths[0] = SD_CARD_DIR + "/Download/";
        shortcutPaths[1] = SD_CARD_DIR + "/Movies/";
        shortcutPaths[2] = SD_CARD_DIR + "/Music/";
        shortcutPaths[3] = SD_CARD_DIR + "/Pictures/";
        String[] shortcutNames = new String[DEFAULT_SHORTCUTS];
        shortcutNames[0] = "Download";
        shortcutNames[1] = "Movies";
        shortcutNames[2] = "Music";
        shortcutNames[3] = "Pictures";
        SharedPreferences sp = getSharedPreferences(FileManagerApp.FILE_MANAGER_PREFERENCES, 0);
        int totalShortcuts =
                sp.getInt(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_TOTAL_NUMBER, 0);
        FileManagerApp.log(TAG + " current total shortcuts=" + totalShortcuts, false);
        /*Temp code for now to add default shortcuts.
          Will add handling code later after delete shortcut is done*/
        if (totalShortcuts == 0) {
            SharedPreferences.Editor spEditor = sp.edit();
            if (spEditor != null) {
                spEditor.putInt(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_TOTAL_NUMBER,
                        DEFAULT_SHORTCUTS);
                for (int i = 0; i < DEFAULT_SHORTCUTS; ++i) {
                    try {
                        spEditor.putString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_NAME +
                                String.valueOf(i + 1), shortcutNames[i]);
                        spEditor.putString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_PATH +
                                String.valueOf(i + 1), shortcutPaths[i]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                spEditor.apply();
                return;
            }
        }
    }

    public String getDeviceStorageVolumePath(int index) {
        String volumePath = "/mnt/sdcard";
        /*try {
            mStorageVolumes = MotoEnvironment.getExternalStorageDirectories();
            if ((mStorageVolumes != null) && (index < mStorageVolumes.length) && (index >= 0)) {
                volumePath = mStorageVolumes[index].getPath();
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }*/
        return volumePath;

    }

    private void stopDownloadServer() {
        log("stopService SambaDownloadServer", false);
        stopService(new Intent(this, DownloadServer.class));
    }

    private void startDownloadServer() {
        Bundle args = new Bundle();
        log("startService SambaDownloadServer", false);
        args.putInt(DownloadServer.DOWNLOAD_MODE, DownloadServer.SAMBA_DOWNLOAD);
        startService(new Intent(this, DownloadServer.class).putExtras(args));
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        stopDownloadServer();
    }

    private String getStoreLocationPath(int cardId, int offset) {
        String path = null;
        switch (cardId) {
            case INDEX_INTERNAL_MEMORY : // Internal Memory
                path = "/mnt/sdcard";
                break;
            case INDEX_SD_CARD : // TAB_INDEX_LOCAL
                setExternalStoragePath();
                path = "/mnt/sdcard-ext";
                break;
            default :
                break;
        }
        return path;
    }

    /* Will be replaced later with Google APIS */
    public void setExternalStoragePath() {
        mStorageExternal = "/mnt/sdcard";
    }

    /* Will be replaced later APIs */
    public String getExternalStoragePath() {
        return mStorageExternal;
    }

    public Boolean isSDcardMounted() {
        String state = "";
        if (SD_CARD_EXT_DIR.length() != 0) {
            state = MotoEnvironment.getExternalStorageState(SD_CARD_EXT_DIR);
            if (state != null) {
                log("SD card state =" + state, false);
                if (Environment.MEDIA_MOUNTED.equals(state) ||
                        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public String getPhoneStoragePath() {
        log("Get mStoragePath " + mStoragePath, false);
        return mStoragePath;
    }

    public void setPhoneStoragePath(String path) {
        mStoragePath = path;
    }

    public void setPhoneStoragePath(int cardId, int offset) {
        setStorageMediumMode(cardId);
        mStoragePath = getStoreLocationPath(cardId, offset);
        log("Set mStoragePath to " + mStoragePath, false);
    }

    private static void setStorageMediumMode(final int mode) {
        mStorgeMediumMode = mode;
    }

    public static int getStorageMediumMode() {
        return mStorgeMediumMode;
    }

    public void saveContentIntent(Intent cIntent) {
        mContentIntent = cIntent;
    }

    public Intent getContentIntent() {
        return mContentIntent;
    }

    public static void log(String info, boolean isError) {
        switch (DBG) {
            case INDEX_LOG_NONE :
                //don't print any log
                break;
            case INDEX_LOG_ERROR :
                //print only error log
                if (isError == true) {
                    Log.e(TAG, info);
                }
                break;
            case INDEX_LOG_ALL :
                //log for all
                if (isError == true) {
                    Log.e(TAG, info);
                } else {
                    Log.d(TAG, info);
                }
                break;
            default :
                break;
        }
    }

    public String getUsbDiskPath(int i) {
        if (0 <= i && i < MAX_USB_DISK_NUM && mUsbDiskListPath != null) {
            return mUsbDiskListPath[i];
        } else {
            return "";
        }
    }

    public boolean isUsbDiskMounted(int offset) {
        if (mUsbDiskListPath == null || offset < 0 || offset >= mUsbDiskListPath.length) {
            return false;
        }
        if (mUsbDiskListPath != null) {
            log("mUsbDiskListPath=" + mUsbDiskListPath[offset], false);
        }
        String state = "";
        if (mUsbDiskListPath[offset].length() != 0) {
            state = MotoEnvironment.getExternalStorageState(mUsbDiskListPath[offset]);
            if (state != null) {
                log("USB state=" + state, false);
                if (Environment.MEDIA_MOUNTED.equals(state) ||
                        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    public static void setUsbDiskConnected(boolean usbDiskConnected) {
        mUsbDiskConnected = usbDiskConnected;
    }

    public static int getUsbDisknum() {
        return mUsbDiskNum;
    }

    public static void setUsbDisknum(int usbDisknum) {
        mUsbDiskNum = usbDisknum;
    }

    public static int getBrowseMode() {
        return mBrowseMode;
    }

    public static void setBrowseMode(int launchMode) {
        mBrowseMode = launchMode;
    }

    public static File getCurrDir() {
        return mCurrentDirectory;
    }

    public static void setCurrDir(File currDir) {
        mCurrentDirectory = currDir;
    }

    public static File getPrevBrowseDir() {
        return mPreviousDirectory;
    }

    public static void setPrevBrowseDir(File prevDir) {
        mPreviousDirectory = prevDir;
    }

    public boolean addRecentFile(File file) {
        if (file == null || file.isDirectory()) {
            return false;
        }
        String recentFiles = "";
        SharedPreferences sp = getSharedPreferences(FileManagerApp.FILE_MANAGER_PREFERENCES, 0);
        if (sp == null) {
            return false;
        }
        String thePreference = sp.getString(FileManagerApp.FILE_MANAGER_PREFERENCE_RECENT_FILE, "");
        if (thePreference == null) {
            return false;
        }
        FileManagerApp.log(TAG + "key=" + FileManagerApp.FILE_MANAGER_PREFERENCE_RECENT_FILE +
                ",preference=" + thePreference, false);
        String[] items = thePreference.split(",");
        if (items != null) {
            for (int i = 0; i < items.length; ++i) {
                try {
                    if (items[i].equals(file.getAbsolutePath())) {
                        FileManagerApp.log(TAG + file.getAbsolutePath() +
                                " is in recent file list.", false);
                        return false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (items.length == 0) {
                recentFiles = file.getAbsolutePath();
            } else if (items.length < FileManagerApp.MAX_NUMBER_OF_RECENT_FILES) {
                recentFiles = file.getAbsolutePath() + "," + thePreference;
            } else {
                recentFiles = file.getAbsolutePath();
                for (int i = 0; i < FileManagerApp.MAX_NUMBER_OF_RECENT_FILES - 1; ++i) {
                    try {
                        recentFiles = recentFiles.concat(",").concat(items[i]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        SharedPreferences.Editor spEditor = sp.edit();
        if (spEditor == null) {
            return false;
        }
        spEditor.putString(FileManagerApp.FILE_MANAGER_PREFERENCE_RECENT_FILE, recentFiles);
        spEditor.apply();
        FileManagerApp
                .log(TAG + "saved to preference,key=" +
                        FileManagerApp.FILE_MANAGER_PREFERENCE_RECENT_FILE + ",value=" +
                        recentFiles, false);

        return true;
    }

    public boolean addShortcut(File file) {
        return addShortcut(file, file.getName());
    }

    public boolean addShortcut(File file, String shortcutName) {
        if (file == null || file.isFile()) {
            return false;
        }
        shortcutName.trim();
        if (shortcutName.length() == 0) {
            shortcutName = file.getName();
        }
        SharedPreferences sp = getSharedPreferences(FileManagerApp.FILE_MANAGER_PREFERENCES, 0);
        int totalShortcuts =
                sp.getInt(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_TOTAL_NUMBER, 0);
        FileManagerApp.log(TAG + "totalShortcuts=" + totalShortcuts, false);
        for (int i = 1; i <= totalShortcuts; ++i) {
            String savedShortcutName =
                    sp.getString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_NAME +
                            String.valueOf(i), "");
            String savedShortcutPath =
                    sp.getString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_PATH +
                            String.valueOf(i), "");
            savedShortcutName.trim();
            savedShortcutPath.trim();
            if (savedShortcutPath.length() == 0) {
                continue;
            }
            if (file.getAbsolutePath().equals(savedShortcutPath) ||
                    file.getAbsolutePath().equals(savedShortcutPath + "/")) {
                FileManagerApp.log(TAG + file.getAbsolutePath() + " is in shortcut list.", false);
                return false;
            }
        }
        totalShortcuts++;

        /* make the path ending with "/" to be consistant with predefined
         * initial shortcuts, also helps updating path after moving folders.
         * (when user select folders and click move from action bar,
         * the selected src folders' paths end with "/")
         */
        String path = file.getAbsolutePath();
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        SharedPreferences.Editor spEditor = sp.edit();
        spEditor.putInt(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_TOTAL_NUMBER,
                totalShortcuts);
        spEditor.putString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_NAME +
                String.valueOf(totalShortcuts), shortcutName);
        spEditor.putString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_PATH +
                String.valueOf(totalShortcuts), path);
        spEditor.apply();
        FileManagerApp.log(TAG + "saved to preference,key=" +
                FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_NAME +
                String.valueOf(totalShortcuts) + ",value=" + shortcutName, false);
        FileManagerApp.log(TAG + "saved to preference,key=" +
                FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_PATH +
                String.valueOf(totalShortcuts) + ",value=" + path, false);

        return true;
    }

    public boolean removeRecentFile(String file) {
        SharedPreferences sp = getSharedPreferences(FileManagerApp.FILE_MANAGER_PREFERENCES, 0);
        String thePreference = sp.getString(FileManagerApp.FILE_MANAGER_PREFERENCE_RECENT_FILE, "");
        FileManagerApp.log(TAG + " key=" + FileManagerApp.FILE_MANAGER_PREFERENCE_RECENT_FILE +
                ",current preference=" + thePreference, false);
        if (thePreference == null || thePreference.length() == 0) {
            return false;
        } else {
            String[] items = thePreference.split(",");
            if (items != null) {
                boolean bFound = false;
                int totalRecentFiles = items.length;
                for (int i = 0; i < items.length; ++i) {
                    if (items[i].equals(file)) {
                        bFound = true;
                        totalRecentFiles--;
                        for (int j = i; j < items.length - 1; ++j) {
                            items[j] = items[j + 1];
                        }
                    }
                }

                if (bFound) {
                    String value = "";

                    if (totalRecentFiles > 0) {
                        value = items[0];
                        for (int i = 1; i < totalRecentFiles; ++i) {
                            value.concat(",");
                            value.concat(items[i]);
                        }
                    }
                    SharedPreferences.Editor spEditor = sp.edit();
                    spEditor.putString(FileManagerApp.FILE_MANAGER_PREFERENCE_RECENT_FILE, value);
                    spEditor.apply();
                    FileManagerApp.log(TAG + " saved to preference,key=" +
                            FileManagerApp.FILE_MANAGER_PREFERENCE_RECENT_FILE + ", new value=" +
                            value, false);
                    return true;
                }
            }
        }
        return false;
    }

    private void saveShortcuts(int totalShortcuts, String[] names, String[] paths) {
        SharedPreferences sp = getSharedPreferences(FileManagerApp.FILE_MANAGER_PREFERENCES, 0);
        SharedPreferences.Editor spEditor = sp.edit();
        for (int i = 0; i < totalShortcuts; ++i) {
            spEditor.putString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_NAME +
                    String.valueOf(i + 1), names[i]);
            spEditor.putString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_PATH +
                    String.valueOf(i + 1), paths[i]);
        }
        spEditor.putInt(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_TOTAL_NUMBER,
                totalShortcuts);
        spEditor.apply();

    }

    public boolean removeShortcut(String file) {
        SharedPreferences sp = getSharedPreferences(FileManagerApp.FILE_MANAGER_PREFERENCES, 0);
        int totalShortcuts =
                sp.getInt(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_TOTAL_NUMBER, 0);
        FileManagerApp.log(TAG + "totalShortcuts=" + totalShortcuts, false);
        if (totalShortcuts == 0) {
            return false;
        }
        String[] allShortcutNames = new String[totalShortcuts];
        String[] allShortcutPaths = new String[totalShortcuts];
        boolean bFound = false;
        int newTotalShortcuts = totalShortcuts;
        int j = 0;
        for (int i = 0; i < totalShortcuts; ++i) {
            String shortcutName =
                    sp.getString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_NAME +
                            String.valueOf(i + 1), "");
            String shortcutPath =
                    sp.getString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_PATH +
                            String.valueOf(i + 1), "");
            shortcutName.trim();
            shortcutPath.trim();
            if (shortcutPath.equals(file)) {
                bFound = true;
                newTotalShortcuts--;
                continue;
            }
            allShortcutNames[j] = shortcutName;
            allShortcutPaths[j] = shortcutPath;
            ++j;
        }
        if (bFound) {
            saveShortcuts(newTotalShortcuts, allShortcutNames, allShortcutPaths);
            FileManagerApp.log(TAG + " shortcut=" + file + " is removed", false);
            return true;
        }
        return false;
    }

    public boolean removeShortcutByPosition(int position) {
        SharedPreferences sp = getSharedPreferences(FileManagerApp.FILE_MANAGER_PREFERENCES, 0);
        int totalShortcuts =
                sp.getInt(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_TOTAL_NUMBER, 0);
        if (totalShortcuts == 0) {
            return false;
        }
        if (position < 0 || position >= totalShortcuts) {
            return false;
        }
        String[] allShortcutNames = new String[totalShortcuts];
        String[] allShortcutPaths = new String[totalShortcuts];
        boolean bFound = false;
        int newTotalShortcuts = totalShortcuts;
        int j = 0;
        for (int i = 0; i < totalShortcuts; ++i) {
            String shortcutName =
                    sp.getString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_NAME +
                            String.valueOf(i + 1), "");
            String shortcutPath =
                    sp.getString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_PATH +
                            String.valueOf(i + 1), "");
            shortcutName.trim();
            shortcutPath.trim();
            if (i == position) {
                bFound = true;
                newTotalShortcuts--;
                continue;
            }
            allShortcutNames[j] = shortcutName;
            allShortcutPaths[j] = shortcutPath;
            ++j;
        }
        if (bFound) {
            saveShortcuts(newTotalShortcuts, allShortcutNames, allShortcutPaths);
            FileManagerApp.log(TAG + " shortcut position=" + position + " is removed ", false);
            return true;
        }
        return false;
    }

    public boolean syncRecentFile(String _fromPath, String _toPath) {
        SharedPreferences sp = getSharedPreferences(FileManagerApp.FILE_MANAGER_PREFERENCES, 0);
        String thePreference = sp.getString(FileManagerApp.FILE_MANAGER_PREFERENCE_RECENT_FILE, "");
        FileManagerApp.log(TAG + " key=" + FileManagerApp.FILE_MANAGER_PREFERENCE_RECENT_FILE +
                ",current preference=" + thePreference, false);
        if (thePreference == null || thePreference.length() == 0) {
            return false;
        } else {
            String[] items = thePreference.split(",");
            if (items != null) {
                boolean bFound = false;
                int totalRecentFiles = items.length;
                for (int i = 0; i < items.length; ++i) {
                    if (items[i].startsWith(_fromPath)) {
                        bFound = true;
                        String tmpNewPath = items[i].replace(_fromPath, _toPath);
                        File newPathFile = new File(tmpNewPath);
                        //check if file with new path exist before replacing
                        if ((newPathFile != null) && (newPathFile.exists())) {
                            items[i] = tmpNewPath;
                        }
                    }
                }

                if (bFound) {
                    String value = "";

                    value = items[0];
                    for (int i = 1; i < totalRecentFiles; ++i) {
                        value.concat(",");
                        value.concat(items[i]);
                    }
                    SharedPreferences.Editor spEditor = sp.edit();
                    spEditor.putString(FileManagerApp.FILE_MANAGER_PREFERENCE_RECENT_FILE, value);
                    spEditor.apply();
                    FileManagerApp.log(TAG + " saved to preference,key=" +
                            FileManagerApp.FILE_MANAGER_PREFERENCE_RECENT_FILE + ", new value=" +
                            value, false);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean syncShortcut(String _fromPath, String _toPath) {
        SharedPreferences sp = getSharedPreferences(FileManagerApp.FILE_MANAGER_PREFERENCES, 0);
        int totalShortcuts =
                sp.getInt(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_TOTAL_NUMBER, 0);
        FileManagerApp.log(TAG + "totalShortcuts=" + totalShortcuts, false);
        if (totalShortcuts == 0) {
            return false;
        }
        String[] allShortcutNames = new String[totalShortcuts];
        String[] allShortcutPaths = new String[totalShortcuts];
        boolean bFound = false;
        int newTotalShortcuts = totalShortcuts;
        int j = 0;
        for (int i = 0; i < totalShortcuts; ++i) {
            String shortcutName =
                    sp.getString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_NAME +
                            String.valueOf(i + 1), "");
            String shortcutPath =
                    sp.getString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_PATH +
                            String.valueOf(i + 1), "");
            shortcutName.trim();
            shortcutPath.trim();
            if (shortcutPath.startsWith(_fromPath)) {
                bFound = true;
                allShortcutNames[j] = shortcutName;
                String tmpNewPath = shortcutPath.replace(_fromPath, _toPath);
                File newPathFile = new File(tmpNewPath);
                //check if folder with new path exists
                if ((newPathFile != null) && (newPathFile.exists())) {
                    allShortcutPaths[j] = tmpNewPath;
                } else {
                    allShortcutPaths[j] = shortcutPath;
                }
            } else {
                allShortcutNames[j] = shortcutName;
                allShortcutPaths[j] = shortcutPath;
            }
            ++j;
        }
        if (bFound) {
            saveShortcuts(newTotalShortcuts, allShortcutNames, allShortcutPaths);
            FileManagerApp.log(TAG + " shortcut " + _fromPath + " is moved to " + _toPath, false);
            return true;
        }
        return false;
    }

    public boolean renameShortcut(int pos, String newName) {
        SharedPreferences sp = getSharedPreferences(FileManagerApp.FILE_MANAGER_PREFERENCES, 0);
        SharedPreferences.Editor spEditor = sp.edit();
        int totalShortcuts =
                sp.getInt(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_TOTAL_NUMBER, 0);
        FileManagerApp.log(TAG + "totalShortcuts=" + totalShortcuts, false);
        if ((totalShortcuts == 0) || (pos >= totalShortcuts)) {
            return false;
        } else {
            spEditor.putString(FileManagerApp.FILE_MANAGER_PREFERENCE_SHORTCUT_NAME +
                    String.valueOf(pos + 1), newName);
            spEditor.apply();
            return true;
        }
    }

}
