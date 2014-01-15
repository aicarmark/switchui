/*
 * Copyright (c) 2010 Motorola, Inc. All Rights Reserved The contents of this
 * file are Motorola Confidential Restricted (MCR). Revision history (newest
 * first): Date CR Author Description 2010-03-26 IKSHADOW-2612 A20815 initial
 */

package com.motorola.filemanager;

import java.io.File;
import java.util.ArrayList;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

import com.motorola.android.storage.MotoEnvironment;
import com.motorola.filemanager.samba.service.DownloadServer;

public class FileManagerApp extends Application {
    public static final int NORMAL_MODE = 250;
    public static final int SELECT_FILE_MODE = 251;
    public static final int SELECT_FOLDER_MODE = 252;
    public static final int SELECT_GET_CONTENT = 253;

    // IKDINARATD-457, a22121, 2011-07-05
    public static int mLaunchMode = NORMAL_MODE;
    public static boolean mFavoriteMode = false;

    public static final int STORAGE_MODE_EXTENAL_SDCARD = 0;
    public static final int STORAGE_MODE_EXTENAL_ALT_SDCARD = 1;
    public static final int STORAGE_MODE_EXTENAL_USB_DISK = 2;

    public static int mIndexSDcard = -1;
    public static boolean mEMMCEnabled = false; // Edison does not have Internal
						// Memory so need a flag to
						// track

    public static int mIndexAltSDcard = -1;
    private static int[] mIndexUSBdriver = null;

    private static String TAG = "FileManagerApp";
    public static final boolean DBG = true;

    public final static String ROOT = "/";
    public final static String ROOT_DIR = "/mnt";
    public static String SD_CARD_DIR = null;
    public static String SD_CARD_EXT_DIR = null;
    public static String APPLICATION_DIR = null;
    public static String DOCUMENT_DIR = null;

    public static int MAX_USB_DISK_NUM = 8;

    public static boolean mUsbDiskConnected = false;
    public static int mUsbDiskNum = 0;
    public static String mUsbDiskListPath[] = null; // new
						    // String[MAX_USB_DISK_NUM];

    public static final int NO_PASTE = 350;
    public static final int MOVE_MODE = 351;
    public static final int COPY_MODE = 352;
    private static int mPasteMode = NO_PASTE;
    private static ArrayList<String> mPasteFiles;
    private static boolean mIsGridView = false;
    private static final String SETTINGS = "settings";
    private static final String VIEWMODE = "view mode";
    private static final String SORTMODE = "sort mode";
    private static final String SEARCHOPTION = "search option";
    public static final int NAMESORT = 300;
    public static final int TYPESORT = 301;
    public static final int TIMESORT = 302;
    public static final int SIZESORT = 303;
    private static int mSortMode = NAMESORT;

    public static final int INTERNALSTORAGE = 0;
    public static final int SDCARD = 1;
    private static int mSearchOption = INTERNALSTORAGE;

    private static File[] mStorageVolumes;
    private static String mStoragePath;
    private Intent mContentIntent;
    private String mCallingPackageName = null;

    private static int mStorgeMediumMode = 0;

    public static void setLaunchMode(int launchMode) {
	mLaunchMode = launchMode;
    }

    public static int getLaunchMode() {
	return mLaunchMode;
    }

    public static void setFavoriteMode(boolean favoriteMode) {
	mFavoriteMode = favoriteMode;
    }

    public static boolean getFavoriteMode() {
	return mFavoriteMode;
    }

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

    public static void setIsGridView(boolean isGridView) {
	mIsGridView = isGridView;
    }

    public void setGridView(boolean isGridView) {
	setIsGridView(isGridView);
	SharedPreferences viewSetting = getSharedPreferences(SETTINGS, 0);
	SharedPreferences.Editor editor = viewSetting.edit();
	editor.putBoolean(VIEWMODE, mIsGridView);
	editor.apply();
    }

    public void resetGridViewMode() {
	SharedPreferences setting = getSharedPreferences(SETTINGS, 0);
	boolean isGridView = setting.getBoolean(VIEWMODE, false);
	setIsGridView(isGridView);
	int sortMode = setting.getInt(SORTMODE, NAMESORT);
	setSortMode(sortMode);
    }

    public static boolean isGridView() {
	return mIsGridView;
    }

    public static void setmSortMode(int sortmode) {
	mSortMode = sortmode;
    }

    public void setSortMode(int sortmode) {
	setmSortMode(sortmode);
	SharedPreferences setting = getSharedPreferences(SETTINGS, 0);
	SharedPreferences.Editor editor = setting.edit();
	editor.putInt(SORTMODE, mSortMode);
	editor.apply();
    }

    public static int getSortMode() {
	return mSortMode;
    }

    public static void setmSearchOption(int searchOption) {
	mSearchOption = searchOption;
    }

    public void setSearchOption(int searchOption) {
	log("Set search option to" + searchOption);
	setmSearchOption(searchOption);
	SharedPreferences setting = getSharedPreferences(SETTINGS, 0);
	SharedPreferences.Editor editor = setting.edit();
	editor.putInt(SEARCHOPTION, mSearchOption);
	editor.apply();
    }

    public static int getSearchOption() {
	return mSearchOption;
    }

    public FileManagerApp() {
	super();
    }

    private void initStorageValue() {
	try {
	    mStorageVolumes = MotoEnvironment.getExternalStorageDirectories();
	    if (mUsbDiskListPath == null
		    || mUsbDiskListPath.length != MAX_USB_DISK_NUM) {
		mUsbDiskListPath = new String[MAX_USB_DISK_NUM];
	    }
	    if (mIndexUSBdriver == null
		    || mIndexUSBdriver.length != MAX_USB_DISK_NUM) {
		mIndexUSBdriver = new int[MAX_USB_DISK_NUM];
	    }

	    int usbList = 0;
	    if (mStorageVolumes != null) {
		for (int i = 0; i < mStorageVolumes.length; i++) {
		    String path = mStorageVolumes[i].toString();
		    String storageType = MotoEnvironment
			    .getExternalStorageMediaType(path);
		    if (storageType.equals(MotoEnvironment.MEDIA_TYPE_FLASH)
			    || storageType
				    .equals(MotoEnvironment.MEDIA_TYPE_EMULATED)) {
			mIndexSDcard = i;
			SD_CARD_DIR = path;
			mEMMCEnabled = true;
		    } else if (storageType
			    .equals(MotoEnvironment.MEDIA_TYPE_SDCARD)) {
			mIndexAltSDcard = i;
			SD_CARD_EXT_DIR = path;
		    } else if (storageType
			    .equals(MotoEnvironment.MEDIA_TYPE_USBDISK)) {
			mIndexUSBdriver[usbList] = i;
			mUsbDiskListPath[usbList] = path;
			usbList++;
		    }
		}
		APPLICATION_DIR = SD_CARD_DIR + "/Application";
		DOCUMENT_DIR = SD_CARD_DIR + "/Document";
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /* Edison does not have Internal memory, so its needed to have this variable */
    public static boolean getEMMCEnabled() {
	return mEMMCEnabled;
    }

    @Override
    public void onCreate() {
	super.onCreate();
	initStorageValue();
	startDownloadServer();
	// startMotoConnectDownloadServer();
	SharedPreferences setting = getSharedPreferences(SETTINGS, 0);
	boolean isGridView = setting.getBoolean(VIEWMODE, false);
	setIsGridView(isGridView);
	int sortMode = setting.getInt(SORTMODE, NAMESORT);
	setmSortMode(sortMode);
    }

    private void stopDownloadServer() {
	log("stopService SambaDownloadServer");
	stopService(new Intent(this, DownloadServer.class));
    }

    private void startDownloadServer() {
	Bundle args = new Bundle();
	log("startService SambaDownloadServer");
	args.putInt(DownloadServer.DOWNLOAD_MODE, DownloadServer.SAMBA_DOWNLOAD);
	startService(new Intent(this, DownloadServer.class).putExtras(args));
    }

    @Override
    public void onTerminate() {
	super.onTerminate();
	stopDownloadServer();
    }

    private static String getStoreLocationPath(int cardId, int offset) {
	String path = null;
	switch (cardId) {
	case STORAGE_MODE_EXTENAL_SDCARD: // Internal Memory
	    path = getDeviceStorageVolumePath(mIndexSDcard);
	    break;
	case STORAGE_MODE_EXTENAL_ALT_SDCARD: // TAB_INDEX_LOCAL
	    path = getDeviceStorageVolumePath(mIndexAltSDcard);
	    break;
	case STORAGE_MODE_EXTENAL_USB_DISK: // ID_USB
	    try {
		path = getDeviceStorageVolumePath(mIndexUSBdriver[offset]);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	default:
	    break;
	}
	return path;
    }

    public static String getDeviceStorageVolumePath(int index) {
	log(" FileManagerApp: in  getDeviceStorageVolumePath");
	String volumePath = "";
	try {
	    mStorageVolumes = MotoEnvironment.getExternalStorageDirectories();
	    if ((mStorageVolumes != null) && (index < mStorageVolumes.length)
		    && (index >= 0)) {
		volumePath = mStorageVolumes[index].getPath();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return volumePath;
    }

    public static String getPhoneStoragePath() {
	log("Get mStoragePath " + mStoragePath);
	return mStoragePath;
    }

    public static void setPhoneStoragePath(String path) {
	mStoragePath = path;
    }

    public static void setPhoneStoragePath(int cardId, int offset) {
	setStorageMediumMode(cardId);
	mStoragePath = getStoreLocationPath(cardId, offset);
	log("Set mStoragePath to " + mStoragePath);
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

    public void saveCallingPackageName(String CallingPackageName) {
	mCallingPackageName = CallingPackageName;
    }

    public String getCallingPackageName() {
	return mCallingPackageName;
    }

    public static void log(String info) {
	if (FileManagerApp.DBG) {
	    Log.w(TAG, info);
	}
    }

    public static String getUsbDiskPath(int i) {
	if (mUsbDiskListPath != null && 0 <= i && i < MAX_USB_DISK_NUM) {
	    return mUsbDiskListPath[i];
	} else {
	    return "";
	}
    }

    public static void setUsbDiskPath(String usbDiskPath, int i) {
	mUsbDiskListPath[i] = usbDiskPath;
    }

    public static boolean isUsbDiskMounted(int offset) {
	try {
	    if (mUsbDiskListPath == null || offset < 0
		    || offset >= mUsbDiskListPath.length) {
		return false;
	    }
	    if (mUsbDiskListPath != null) {
		log("mUsbDiskListPath=" + mUsbDiskListPath[offset]);
	    }
	    String state = "";
	    if (mUsbDiskListPath[offset].length() != 0) {
		state = MotoEnvironment
			.getExternalStorageState(mUsbDiskListPath[offset]);
		if (state != null) {
		    log("USB state=" + state);
		    if (Environment.MEDIA_MOUNTED.equals(state)
			    || Environment.MEDIA_MOUNTED_READ_ONLY
				    .equals(state)) {
			return true;
		    } else {
			return false;
		    }
		}
	    }
	} catch (Exception e) {
	    log(" Exception in isUsbDiskMounted: " + e);
	}
	return false;
    }

    public static boolean isUsbDiskConnected() {
	return mUsbDiskConnected;
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

    public String getQueueName() {
	return getResources().getString(R.string.app_name);
    }

    public static int getDialogSize(Context context) {
	DisplayMetrics metrics = context.getResources().getDisplayMetrics();
	int screenWidth = metrics.widthPixels;
	boolean isPortrait = screenWidth < metrics.heightPixels;
	int dialogSize = 0;
	TypedValue outValue = new TypedValue();
	if (isPortrait) {
	    context.getResources().getValue(
		    R.fraction.alert_dialog_portrait_width, outValue, false);
	    float fp = outValue.getFloat();
	    dialogSize = (int) (screenWidth * fp);
	} else {
	    context.getResources().getValue(
		    R.fraction.alert_dialog_landscape_width, outValue, false);
	    float fl = outValue.getFloat();
	    dialogSize = (int) (screenWidth * fl);
	}
	return dialogSize;
    }
}
