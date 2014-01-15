/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.filemanager.multiselect.local;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.motorola.filemanager.multiselect.FileManagerApp;
import com.motorola.filemanager.multiselect.utils.IconifiedText;

public class SearchDirectory extends Thread {
  private static final String TAG = "SearchDirectory: ";
  private File currentDirectory;
  volatile boolean cancel = false;
  private Context mContext;
  private Handler mHandler;
  private String mFilterString = "";
  private List<IconifiedText> mDirList = new ArrayList<IconifiedText>();
  private List<IconifiedText> mFileList = new ArrayList<IconifiedText>();

  SearchDirectory(File directory, Context context, Handler handler, String filterString) {
    super("SearchDirectory");
    this.currentDirectory = directory;
    this.mContext = context;
    this.mHandler = handler;
    this.mFilterString = filterString;
  }

  public synchronized void requestStop() {
    cancel = true;
    FileManagerApp.log(TAG + "Request stop search");
    // mContext = null;
    // mHandler = null;
    return;

  }

  private void clearData() {
    mContext = null;
    mHandler = null;
  }

  @Override
  public void run() {
    FileManagerApp.log(TAG + "Scanning directory " + currentDirectory.getName());
    if (mContext == null || mHandler == null) {
      return;
    }
    mHandler.obtainMessage(FileManagerActivity.MESSAGE_SET_LOADING_PROGRESS).sendToTarget();
    scanDirectory(currentDirectory);
    Collections.sort(mDirList, IconifiedText.returnSort(FileManagerApp.NAMESORT));
    Collections.sort(mFileList, IconifiedText.returnSort(FileManagerApp.NAMESORT));
    if (mDirList instanceof ArrayList<?> && mFileList instanceof ArrayList<?>) {
      IconifiedText.sortFiles((ArrayList<IconifiedText>) mDirList,
          (ArrayList<IconifiedText>) mFileList);
    }
    if (!cancel) {
      FileManagerApp.log(TAG + "Sending data back to main thread, total results=" +
          mDirList.size() + mFileList.size());
      FileManagerActivity.DirectoryContents contents = new FileManagerActivity.DirectoryContents();
      contents.listDir = mDirList;
      contents.listFile = mFileList;
      Message msg = mHandler.obtainMessage(FileManagerActivity.MESSAGE_SHOW_DIRECTORY_CONTENTS);
      msg.obj = contents;
      msg.sendToTarget();
    }
    clearData();
  }

  private void scanDirectory(File directory) {
    // FileManagerApp.log(TAG + "Scanning directory " + directory.getName());
    if (cancel) {
      FileManagerApp.log(TAG + "Scan aborted");
      clearData();
      return;
    }
    File[] files = directory.listFiles();
    if (files != null) {
      for (File currentFile : files) {
        if (cancel) {
          clearData();
          return;
        }
        if (currentFile.isDirectory()) {
          if (currentFile.getName().indexOf(mFilterString) >= 0) {
            mDirList.add(IconifiedText.buildIconItem(mContext, currentFile));
          }
          scanDirectory(currentFile);
        } else {
          if (currentFile.getName().indexOf(mFilterString) >= 0) {
            mFileList.add(IconifiedText.buildIconItem(mContext, currentFile));
          }
        }
      }
    }
  }
}
