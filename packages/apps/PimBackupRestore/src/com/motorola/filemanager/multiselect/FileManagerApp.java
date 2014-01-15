/*
 * Copyright (c) 2010 Motorola, Inc. All Rights Reserved The contents of this
 * file are Motorola Confidential Restricted (MCR). Revision history (newest
 * first): Date CR Author Description 2010-03-26 IKSHADOW-2612 A20815 initial
 */

package com.motorola.filemanager.multiselect;

import java.util.ArrayList;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.motorola.sdcardbackuprestore.Constants;
import com.motorola.sdcardbackuprestore.SdcardManager;

public class FileManagerApp extends Application {
  public static final int NORMAL_MODE = 250;
  public static final int SELECT_FILE_MODE = 251;
  public static final int SELECT_FOLDER_MODE = 252;
  public static final int SELECT_GET_CONTENT = 253;

  //IKDINARATD-457, a22121, 2011-07-05
  public static int mLaunchMode = NORMAL_MODE;

  private static String TAG = "FileManagerApp";
  public static final boolean DBG = true;

  public final static String ROOT = "/";
  public static String ROOT_DIR ;
  static {
	    String [] subparts;
		subparts = Environment.getExternalStorageDirectory().toString().split("/");
	    ROOT_DIR = ROOT + subparts[1];
  }
  
  /*
  public final static String SD_CARD_DIR = ROOT_DIR + "/sdcard";
  public final static String SD_CARD_EXT_DIR = ROOT_DIR + "/sdcard-ext";
  */
  //public final static String SD_CARD_DIR = Constants.SDCARD_PREFIX;
  //public final static String SD_CARD_EXT_DIR = Constants.ALT_SDCARD_PREFIX;
  public final String SD_CARD_DIR;
  public final String SD_CARD_EXT_DIR;

  public static final int MAX_USB_DISK_NUM = 3;
  private static boolean mUsbDiskConnected = false;
  private static int mUsbDiskNum = 0;
  private static String mUsbDiskListPath[] = new String[MAX_USB_DISK_NUM];
  public String mCurrentUsbPath;

  public static final int NO_PASTE = 350;
  public static final int MOVE_MODE = 351;
  public static final int COPY_MODE = 352;
  private static int mPasteMode = NO_PASTE;
  private static ArrayList<String> mPasteFiles;
  private static boolean mIsGridView = false;
  private static boolean mShowFolderViewIcon = true;
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

  private static String mStoragePath;
  private Intent mContentIntent;
  private String mCallingPackageName = null;
  private Constants mConstants = null;

  public static final int STORAGE_MODE_EXTENAL_SDCARD = 1;
  public static final int STORAGE_MODE_EXTENAL_ALT_SDCARD = 2;
  public static final int STORAGE_MODE_EXTENAL_USB_DISK = 3;

  private static int mStorgeMediumMode = 0;

  private static FileManagerApp sInstance = null;

  public static FileManagerApp getInstance(Context context){
    if (null == sInstance) {
        sInstance = new FileManagerApp(context);
    }

    return sInstance;
  }
  public static void setLaunchMode(int launchMode) {
    mLaunchMode = launchMode;
  }

  public static int getLaunchMode() {
    return mLaunchMode;
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
    editor.commit();
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
    editor.commit();
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
    editor.commit();
  }

  public static int getSearchOption() {
    return mSearchOption;
  }

  public static boolean isShowFolderViewIcon() {
    return mShowFolderViewIcon;
  }

  public static void setShowFolderViewIcon(boolean show) {
    mShowFolderViewIcon = show;
  }

    public FileManagerApp() {
        super();
        mConstants = null;
        SD_CARD_DIR = "";
        SD_CARD_EXT_DIR = "";
    }

  public FileManagerApp(Context context) {
    super();
    
    mConstants = new Constants(context);
    SD_CARD_DIR = mConstants.SDCARD_PREFIX;
    SD_CARD_EXT_DIR = mConstants.ALT_SDCARD_PREFIX;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    //    startMotoConnectDownloadServer();
    SharedPreferences setting = getSharedPreferences(SETTINGS, 0);
    boolean isGridView = setting.getBoolean(VIEWMODE, false);
    setIsGridView(isGridView);
    int sortMode = setting.getInt(SORTMODE, NAMESORT);
    setmSortMode(sortMode);
  }

  @Override
  public void onTerminate() {
    super.onTerminate();
    //stopMotoConnectDownloadServer();
  }

  private String getStoreLocationPath(int cardId, int offset) {
    String path = null;
    switch (cardId) {
      case STORAGE_MODE_EXTENAL_SDCARD : // ID_EXTSDCARD
        path = SD_CARD_DIR;
        break;
      case STORAGE_MODE_EXTENAL_ALT_SDCARD : // ID_LOCAL
        path = SD_CARD_EXT_DIR;
        break;
      case STORAGE_MODE_EXTENAL_USB_DISK : // ID_USB
        path = mUsbDiskListPath[offset];
      default :
        break;
    }
    return path;
  }

  public static String getPhoneStoragePath() {
    log("Get mStoragePath " + mStoragePath);
    return mStoragePath;
  }

  public static void setPhoneStoragePath(String path) {
    mStoragePath = path;
  }

  public void setPhoneStoragePath(int cardId, int offset) {
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
    return mUsbDiskListPath[i];
  }

  public static void setUsbDiskPath(String usbDiskPath, int i) {
    mUsbDiskListPath[i] = usbDiskPath;
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

}
