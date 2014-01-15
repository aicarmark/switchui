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

package com.motorola.FileManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.motorola.FileManager.utils.IconifiedText;

public class DirectoryScanner extends Thread {
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
	Log.d("", "w23001-Request stop");
	return;
    }

    private void clearData() {
	mContext = null;
	mHandler = null;
    }

    @Override
    public void run() {
	Log.d("", "w23001-Scanning directory " + currentDirectory);
	if (cancel) {
	    Log.d("", "w23001-Scan aborted");
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
		IconifiedText.returnSort(FileManagerActivity.NAMESORT));
	Collections.sort(listFile,
		IconifiedText.returnSort(FileManagerActivity.NAMESORT));
	if (listDir instanceof ArrayList<?> && listFile instanceof ArrayList<?>) {
	    IconifiedText.sortFiles((ArrayList<IconifiedText>) listDir,
		    (ArrayList<IconifiedText>) listFile);
	}
	if (!cancel) {
	    Log.d("", "w23001-Sending data back to main thread");
	    FileManagerActivity.DirectoryContents contents = new FileManagerActivity.DirectoryContents();
	    contents.listDir = listDir;
	    contents.listFile = listFile;
	    Message msg = mHandler
		    .obtainMessage(FileManagerActivity.MESSAGE_SHOW_DIRECTORY_CONTENTS);
	    msg.obj = contents;
	    msg.sendToTarget();
	}
	clearData();
    }
}
