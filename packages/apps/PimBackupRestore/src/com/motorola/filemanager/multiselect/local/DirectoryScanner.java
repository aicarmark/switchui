/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date       CR                Author      Description
 * 2010-03-23   IKSHADOW-2425   A20815      initial
 */

package com.motorola.filemanager.multiselect.local;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.motorola.filemanager.multiselect.FileManager;
import com.motorola.filemanager.multiselect.FileManagerApp;
import com.motorola.filemanager.multiselect.utils.IconifiedText;
import com.motorola.sdcardbackuprestore.Constants;

public class DirectoryScanner extends Thread {
  private static final String TAG = "DirectoryScanner: ";
  private File currentDirectory;
  volatile boolean cancel = false;
  private Context mContext;
  private Handler mHandler;
  private ArrayList<String> mFilter = new ArrayList<String>();

  public ArrayList<String> getFilter() {
	return mFilter;
  }

  public void setFilter(ArrayList<String> filter) {
	  if (filter == null) {
		return;
	  }
	  mFilter.clear();
	  for (int i = 0; i < filter.size(); i++) {
		  mFilter.add(filter.get(i));
	  }
  }

DirectoryScanner(File directory, Context context, Handler handler) {
    super("Directory Scanner");
    currentDirectory = directory;
    this.mContext = context;
    this.mHandler = handler;
    if (FileManager.mFilter != null && FileManager.mFilter.size() != 0) {
		setFilter(FileManager.mFilter);
	} else {
	    mFilter.add(Constants.vmgExtern);
	    mFilter.add(Constants.vcardExtern);
	    mFilter.add(Constants.qnExtern);
	    mFilter.add(Constants.calExtern);
	    mFilter.add(Constants.calCompatExtern);
	    mFilter.add(Constants.bookmarkExtern);
	}
  }

  public synchronized void requestStop() {
    cancel = true;
    FileManagerApp.log(TAG + "Request stop");
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
    FileManagerApp.log(TAG + "Scanning directory " + currentDirectory);
    /*
     * if(mContext == null || mHandler == null){ return; }
     */
    if (cancel) {
      FileManagerApp.log(TAG + "Scan aborted");
      clearData();
      return;
    }
    mHandler.obtainMessage(FileManagerActivity.MESSAGE_SET_LOADING_PROGRESS).sendToTarget();

    File[] files = currentDirectory.listFiles(new FilenameFilter() {
		
		public boolean accept(File dir, String filename) {
			// TODO Auto-generated method stub
			if (new File(dir + "/" + filename).isDirectory()) {
				return true;
			} else {
				for (int i = 0; i < mFilter.size(); i++) {
					if (filename.endsWith(mFilter.get(i))) {
						return true;
					}
				}
			}
			return false;
		}
	});
    IconifiedText tmp = null;
    List<IconifiedText> listDir = new ArrayList<IconifiedText>();
    List<IconifiedText> listFile = new ArrayList<IconifiedText>();

    if (files != null) {
      for (File currentFile : files) {
        if (cancel) {
          clearData();
          return;
        }
        // need to show hide files ---start with dot
        // if(currentFile.getName().startsWith(".")){
        // continue;
        // }
        /*
         * if(mContext == null || mHandler == null){ return; }
         */
        try {
          tmp = IconifiedText.buildIconItem(mContext, currentFile);
          if (currentFile.isDirectory()) {
            listDir.add(tmp);
          } else {
            listFile.add(tmp);
          }
        } catch (Exception e) {
          clearData();
          return;
        }
      }
    }
    Collections.sort(listDir, IconifiedText.returnSort(FileManagerApp.NAMESORT));
    Collections.sort(listFile, IconifiedText.returnSort(FileManagerApp.NAMESORT));
    if (listDir instanceof ArrayList<?> && listFile instanceof ArrayList<?>) {
      IconifiedText.sortFiles((ArrayList<IconifiedText>) listDir,
          (ArrayList<IconifiedText>) listFile);
    }
    if (!cancel) {
      FileManagerApp.log(TAG + "Sending data back to main thread");
      FileManagerActivity.DirectoryContents contents = new FileManagerActivity.DirectoryContents();
      contents.listDir = listDir;
      contents.listFile = listFile;
      Message msg = mHandler.obtainMessage(FileManagerActivity.MESSAGE_SHOW_DIRECTORY_CONTENTS);
      msg.obj = contents;
      msg.sendToTarget();
    }
    clearData();
  }
}
