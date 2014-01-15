/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR              Author      Description
 * 2011-05-23   IKTABLETMAIN-348    DTW768      initial
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
import com.motorola.filemanager.local.LocalRightFileManagerFragment;
import com.motorola.filemanager.ui.IconifiedText;

public class SearchDirectory extends Thread {
    private static final String TAG = "SearchDirectory: ";
    private File currentDirectory;
    volatile boolean cancel = false;
    private Context mContext;
    private FileManagerApp mFileManagerApp;
    private Handler mHandler;
    private String mFilterString = "";
    private int mMode;
    private List<IconifiedText> mDirList = new ArrayList<IconifiedText>();
    private List<IconifiedText> mFileList = new ArrayList<IconifiedText>();

    public SearchDirectory(File directory, Context context, Handler handler, String filterString,
                           int mode) {
        super("SearchDirectory");
        this.currentDirectory = directory;
        this.mContext = context;
        mFileManagerApp = (FileManagerApp) mContext.getApplicationContext();
        this.mHandler = handler;
        this.mFilterString = filterString;
        this.mMode = mode;
    }

    public synchronized void requestStop() {
        cancel = true;
        FileManagerApp.log(TAG + "Request stop search", false);
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
        FileManagerApp.log(TAG + "Scanning directory " + currentDirectory.getName(), false);
        if (mContext == null || mHandler == null) {
            return;
        }
        mHandler.obtainMessage(LocalBaseFileManagerFragment.MESSAGE_SET_LOADING_PROGRESS)
                .sendToTarget();
        scanDirectory(currentDirectory);
        Collections.sort(mDirList, IconifiedText.mNameSort);
        Collections.sort(mFileList, IconifiedText.mNameSort);
        switch (((FileManagerApp) mContext.getApplicationContext()).getSortMode()) {
            case FileManagerApp.TYPESORT :
                if (mFileManagerApp.mIsDescendingType == false) {
                    Collections.sort(mDirList, IconifiedText.mTypeSort);
                    Collections.sort(mFileList, IconifiedText.mTypeSort);
                } else {
                    Collections.sort(mDirList, Collections.reverseOrder(IconifiedText.mTypeSort));
                    Collections.sort(mFileList, Collections.reverseOrder(IconifiedText.mTypeSort));
                }
                break;
            case FileManagerApp.DATESORT :
                if (mFileManagerApp.mIsDescendingType == false) {
                    Collections.sort(mDirList, IconifiedText.mTimeSort);
                    Collections.sort(mFileList, IconifiedText.mTimeSort);
                } else {
                    Collections.sort(mDirList, Collections.reverseOrder(IconifiedText.mTimeSort));
                    Collections.sort(mFileList, Collections.reverseOrder(IconifiedText.mTimeSort));
                }
                break;
            case FileManagerApp.SIZESORT :
                if (mFileManagerApp.mIsDescendingType == false) {
                    Collections.sort(mDirList, IconifiedText.mSizeSort);
                    Collections.sort(mFileList, IconifiedText.mSizeSort);
                } else {
                    Collections.sort(mDirList, Collections.reverseOrder(IconifiedText.mSizeSort));
                    Collections.sort(mFileList, Collections.reverseOrder(IconifiedText.mSizeSort));
                }
                break;
            case FileManagerApp.NAMESORT :
                if (mFileManagerApp.mIsDescendingType == false) {
                    Collections.sort(mDirList, IconifiedText.mNameSort);
                    Collections.sort(mFileList, IconifiedText.mNameSort);
                } else {
                    Collections.sort(mDirList, Collections.reverseOrder(IconifiedText.mNameSort));
                    Collections.sort(mFileList, Collections.reverseOrder(IconifiedText.mNameSort));
                }
                break;
        }

        if (!cancel) {
            FileManagerApp.log(
                    TAG + "Sending data back to main thread, total results=" + mDirList.size() +
                            mFileList.size(), false);
            LocalRightFileManagerFragment.DirectoryContents contents =
                    new LocalRightFileManagerFragment.DirectoryContents();
            contents.listDir = mDirList;
            contents.listFile = mFileList;
            contents.currentPath = currentDirectory;
            Message msg =
                    mHandler.obtainMessage(LocalBaseFileManagerFragment.MESSAGE_SHOW_DIRECTORY_CONTENTS);
            msg.obj = contents;
            msg.sendToTarget();
        }
        clearData();
    }

    private void scanDirectory(File directory) {
        // FileManagerApp.log(TAG + "Scanning directory " +
        // directory.getName());
        if (cancel) {
            FileManagerApp.log(TAG + "Scan aborted", false);
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
                        mDirList.add(IconifiedText.buildIconItem(mContext, currentFile, mMode));
                    }
                    scanDirectory(currentFile);
                } else {
                    if (currentFile.getName().indexOf(mFilterString) >= 0) {
                        mFileList.add(IconifiedText.buildIconItem(mContext, currentFile, mMode));
                    }
                }
            }
        }
    }
}
