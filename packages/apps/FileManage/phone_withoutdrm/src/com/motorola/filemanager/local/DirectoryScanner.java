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

package com.motorola.filemanager.local;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.utils.IconifiedText;

public class DirectoryScanner extends Thread {
    private static final String TAG = "DirectoryScanner: ";
    private File currentDirectory;
    volatile boolean cancel = false;
    private Context mContext;
    private Handler mHandler;

    DirectoryScanner(File directory, Context context, Handler handler) {
	super("Directory Scanner");
	currentDirectory = directory;
	this.mContext = context;
	this.mHandler = handler;
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
	mHandler.obtainMessage(FileManagerActivity.MESSAGE_SET_LOADING_PROGRESS)
		.sendToTarget();

	File[] files = currentDirectory.listFiles();
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
		    if (currentFile.isDirectory()) {
			listDir.add(IconifiedText.buildIconItem(mContext,
				currentFile));
		    } else {
			listFile.add(IconifiedText.buildIconItem(mContext,
				currentFile));
		    }
		} catch (Exception e) {
		    clearData();
		    return;
		}
	    }
	}
	Collections.sort(listDir,
		IconifiedText.returnSort(FileManagerApp.NAMESORT));
	Collections.sort(listFile,
		IconifiedText.returnSort(FileManagerApp.NAMESORT));
	if (listDir instanceof ArrayList<?> && listFile instanceof ArrayList<?>) {
	    IconifiedText.sortFiles((ArrayList<IconifiedText>) listDir,
		    (ArrayList<IconifiedText>) listFile);
	}
	if (!cancel) {
	    try {
		FileManagerApp.log(TAG + "Sending data back to main thread");
		FileManagerActivity.DirectoryContents contents = new FileManagerActivity.DirectoryContents();
		contents.listDir = listDir;
		contents.listFile = listFile;
		Message msg = mHandler
			.obtainMessage(FileManagerActivity.MESSAGE_SHOW_DIRECTORY_CONTENTS);
		msg.obj = contents;
		msg.sendToTarget();
	    } catch (Exception e) {
		FileManagerApp.log(TAG
			+ "Sending data back to main thread error: "
			+ e.getMessage());
	    }
	}
	clearData();
    }
}
