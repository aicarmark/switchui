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
package com.motorola.filemanager.local.threads;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.local.LocalBaseFileManagerFragment;
import com.motorola.filemanager.ui.IconifiedText;

public class DirectoryScanner extends Thread {
    private static final String TAG = "DirectoryScanner: ";
    private File currentDirectory;
    public volatile boolean cancel = false;
    private Context mContext;
    private FileManagerApp mFileManagerApp;
    private Handler mHandler;
    private int mMode;
    private boolean isTitleFragmentSort;

    public DirectoryScanner(File directory, Context context, Handler handler, int i) {
        super("Directory Scanner");
        currentDirectory = directory;
        this.mContext = context;
        mFileManagerApp = (FileManagerApp) mContext.getApplicationContext();
        this.mHandler = handler;
        this.mMode = i;
        isTitleFragmentSort = false;
        cancel = false;
    }

    public synchronized void requestStop() {
        cancel = true;
        FileManagerApp.log(TAG + "Request stop", false);
        return;
    }

    private void clearData() {
        mContext = null;
        mHandler = null;
    }

    @Override
    public void run() {
        FileManagerApp.log(TAG + "Scanning directory " + currentDirectory, false);
        if (cancel) {
            FileManagerApp.log(TAG + "Scan aborted", false);
            clearData();
            return;
        }
        if (mHandler == null) {
            return;
        }

        mHandler.obtainMessage(LocalBaseFileManagerFragment.MESSAGE_SET_LOADING_PROGRESS)
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
                try {
                    if (currentFile.isDirectory()) {
                        listDir.add(IconifiedText.buildIconItem(mContext, currentFile, mMode));
                    } else {
                        listFile.add(IconifiedText.buildIconItem(mContext, currentFile, mMode));
                    }
                } catch (Exception e) {
                    clearData();
                    return;
                }
            }
        }
        if (!isTitleFragmentSort) {
            switch (mFileManagerApp.getSortMode()) {
                case FileManagerApp.TYPESORT :
                    if (mFileManagerApp.mIsDescendingType == false) {
                        Collections.sort(listDir, IconifiedText.mTypeSort);
                        Collections.sort(listFile, IconifiedText.mTypeSort);
                    } else {
                        Collections
                                .sort(listDir, Collections.reverseOrder(IconifiedText.mTypeSort));
                        Collections.sort(listFile,
                                Collections.reverseOrder(IconifiedText.mTypeSort));
                    }
                    break;
                case FileManagerApp.DATESORT :
                    if (mFileManagerApp.mIsDescendingDate == false) {
                        Collections.sort(listDir, IconifiedText.mTimeSort);
                        Collections.sort(listFile, IconifiedText.mTimeSort);
                    } else {
                        Collections
                                .sort(listDir, Collections.reverseOrder(IconifiedText.mTimeSort));
                        Collections.sort(listFile,
                                Collections.reverseOrder(IconifiedText.mTimeSort));
                    }
                    break;
                case FileManagerApp.SIZESORT :
                    if (mFileManagerApp.mIsDescendingSize == false) {
                        Collections.sort(listDir, IconifiedText.mSizeSort);
                        Collections.sort(listFile, IconifiedText.mSizeSort);
                    } else {
                        Collections
                                .sort(listDir, Collections.reverseOrder(IconifiedText.mSizeSort));
                        Collections.sort(listFile,
                                Collections.reverseOrder(IconifiedText.mSizeSort));
                    }
                    break;
                case FileManagerApp.NAMESORT :
                    if (mFileManagerApp.mIsDescendingName == false) {
                        Collections.sort(listDir, IconifiedText.mNameSort);
                        Collections.sort(listFile, IconifiedText.mNameSort);
                    } else {
                        Collections
                                .sort(listDir, Collections.reverseOrder(IconifiedText.mNameSort));
                        Collections.sort(listFile,
                                Collections.reverseOrder(IconifiedText.mNameSort));
                    }
                    break;
            }
        }
        if (!cancel) {
            try {
                FileManagerApp.log(TAG + "Sending data back to main thread", false);
                LocalBaseFileManagerFragment.DirectoryContents contents =
                        new LocalBaseFileManagerFragment.DirectoryContents();
                contents.listDir = listDir;
                contents.listFile = listFile;
                contents.currentPath = currentDirectory;
                Message msg =
                        mHandler.obtainMessage(LocalBaseFileManagerFragment.MESSAGE_SHOW_DIRECTORY_CONTENTS);
                msg.obj = contents;
                msg.sendToTarget();
            } catch (Exception e) {
                FileManagerApp.log(
                        TAG + "Sending data back to main thread error: " + e.getMessage(), true);
            }
        }
        clearData();
    }

    public boolean setTitleFragmentSortMode(boolean titleFragmentSortFlag) {
        isTitleFragmentSort = titleFragmentSortFlag;
        return isTitleFragmentSort;
    }
}
