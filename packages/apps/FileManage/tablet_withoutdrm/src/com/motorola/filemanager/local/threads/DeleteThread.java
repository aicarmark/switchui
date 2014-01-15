/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR              Author      Description
 * 2011-10-03   IKTABLETMAIN-348    XQH748      initial
 */
package com.motorola.filemanager.local.threads;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.local.LocalFileOperationsFragment;
import com.motorola.filemanager.utils.FileUtils;

public class DeleteThread extends Thread {
    private ArrayList<String> mDeleteFiles;
    private File mParentFile;
    private Handler mHandler = null;
    private String TAG = "DeleteThread";
    private Activity mActivity;
    private int numOfItemsDeleted = 0;

    public DeleteThread(ArrayList<String> deletFiles, Handler handler, Activity activity,
                        Context context) {
        super();
        mDeleteFiles = deletFiles;
        mHandler = handler;
        mActivity = activity;
        // assume all delete files are in the same folder
        // just use one to get the parent folder
        if (!mDeleteFiles.isEmpty()) {
            try {
                mParentFile = new File(mDeleteFiles.get(0));
                if (mParentFile != null) {
                    mParentFile = mParentFile.getParentFile();
                }
            } catch (IndexOutOfBoundsException ex) {
                return;
            }
        }
    }

    @Override
    public void run() {
        boolean result = true;
        numOfItemsDeleted = 0;
        if (mDeleteFiles == null) {
            FileManagerApp.log(TAG + "DeleteThread mDeleteFiles is null, no operation to be done",
                    false);
            return;
        }
        File file = null;
        for (String filename : mDeleteFiles) {
            file = new File(filename);
            if (file.exists()) {
                result = delete(file) && result;
            }
        }
        if (file != null) {
            // scan the parent folder of the file(s) just deleted
            File parentFile = file.getParentFile();
            if (parentFile != null) {
                FileUtils.mediaScanFolder(mActivity, parentFile);
            }
        }
        if (result) {
            mHandler.obtainMessage(LocalFileOperationsFragment.MESSAGE_DELETE_FINISHED,
                    LocalFileOperationsFragment.ARG_DELETE_COMPLETED, numOfItemsDeleted).sendToTarget();
        } else {
            mHandler.obtainMessage(LocalFileOperationsFragment.MESSAGE_DELETE_FINISHED,
                    LocalFileOperationsFragment.ARG_DELETE_FAILED, numOfItemsDeleted).sendToTarget();
        }
    }

    private boolean delete(File file) {
        boolean result = true;

        if (file.isDirectory()) {
            File[] list = file.listFiles();
            if (list != null) {
                for (File tempFile : list) {
                    result = delete(tempFile) && result;
                    // Now that we have deleted the file, if this is media file we need to
                    // delete from Media store
                    // new DeleteFileFromMediaStoreTask().execute(tempFile);
                }
            }
            if (result) {
                try {
                    result = file.delete() && result;
                    numOfItemsDeleted++;
                    // Send Intent to media scanner to scan the folder
                } catch (Exception e) {
                    e.printStackTrace();
                    result = false;
                }
            }
        } else {
            try {
                result = file.delete();
                numOfItemsDeleted++;
            } catch (Exception e) {
                e.printStackTrace();
                result = false;
            }
        }
        return result;
    }
    //Send Intent to media scanner to scan the folder
    // mediaScanFolder(getActivity(), currentDirectory);

}
