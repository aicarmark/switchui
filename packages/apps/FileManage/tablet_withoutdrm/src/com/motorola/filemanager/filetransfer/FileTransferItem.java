/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 */
package com.motorola.filemanager.filetransfer;

import com.motorola.filemanager.samba.service.DownloadServer.MultiDelayedMsg;

public class FileTransferItem {

    private MultiDelayedMsg mTransferData = null;
    private TransferStatus mTransferStatus;
    private int mProgress;
    private String mCurrTransferFile;

    public enum TransferStatus {
        ACTIVE, QUEUED, COMPLETED, FAILED
    }

    public FileTransferItem(MultiDelayedMsg item, TransferStatus status) {
        mTransferStatus = status;
        mTransferData = item;
        mProgress = 0;

        // Just get the name.
        String[] split_str = item.getTransferSrcList().get(0).split("/");
        int size = split_str.length;
        mCurrTransferFile = split_str[size - 1];
    }

    public MultiDelayedMsg getTransferData() {
        return mTransferData;
    }

    public TransferStatus getTransferStatus() {
        return mTransferStatus;
    }

    public void setProgress(int progress) {
        mProgress = progress;
    }

    public void setCurrTransferFile(String filename) {
        mCurrTransferFile = filename;
    }

    public int getProgress() {
        return mProgress;
    }

    public String getCurrTransferFile() {
        return mCurrTransferFile;
    }

}
