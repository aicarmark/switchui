/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 */
package com.motorola.filemanager.filetransfer;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;

import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;
import com.motorola.filemanager.samba.service.DownloadServer.MultiDelayedMsg;

public class FileTransferListAdapter extends BaseAdapter {

    private ArrayList<FileTransferItem> mTransferList = new ArrayList<FileTransferItem>();
    private Context mContext = null;

    public FileTransferListAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mTransferList.size();
    }

    public void setTransferList(ArrayList<FileTransferItem> list) {
        mTransferList = list;
    }

    @Override
    public Object getItem(int pos) {
        return mTransferList.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        if (inflater != null) {
            if (convertView == null) {
                convertView = new FileTransferItemView(mContext);
            }
            FileTransferItem item = mTransferList.get(position);
            if ((item != null) && (convertView != null)) {
                updateDescriptionText(convertView, item);
                updateTransferIcon(convertView, item);
                updateInfoText(convertView, item);
                updateProgress(convertView, item);
                setupCancelButton(convertView, item);
            }
            return convertView;
        } else {
            return null;
        }
    }

    private void setupCancelButton(View convertView, FileTransferItem item) {

        final MultiDelayedMsg cancelItem = item.getTransferData();
        final FileTransferItem tmpItem = item;
        LinearLayout buttonContainer =
                (LinearLayout) convertView.findViewById(R.id.filetransfer_cancel_button_container);

        if ((cancelItem != null) && (buttonContainer != null)) {
            buttonContainer.setOnClickListener(new View.OnClickListener() {
                //@Override
                public void onClick(View v) {

                    if (tmpItem.getTransferStatus() == FileTransferItem.TransferStatus.ACTIVE) {
                        if (((FileTransferDialog) mContext).mDownloadServer != null) {
                            ((FileTransferDialog) mContext).mDownloadServer
                                    .cancelCurrentTransfer(cancelItem);
                            ((FileTransferDialog) mContext).showCancelDialog();
                        }
                    } else {
                        if (((FileTransferDialog) mContext).mDownloadServer != null) {
                            ((FileTransferDialog) mContext).mDownloadServer
                                    .cancelQueuedTransfer(cancelItem);
                            ((FileTransferDialog) mContext).refreshList();
                        }

                    }

                }
            });
        }
    }

    private void updateProgress(View convertView, FileTransferItem item) {
        if (item.getTransferStatus() == FileTransferItem.TransferStatus.ACTIVE) {
            ((FileTransferItemView) convertView).setProgress(item.getProgress());
            String precentage =
                    mContext.getResources().getString(R.string.progress_precentage,
                            item.getProgress());
            if (precentage != null) {
                ((FileTransferItemView) convertView).setPrecentageText(precentage);
            }
            ((FileTransferItemView) convertView).showProgress();
        } else {
            ((FileTransferItemView) convertView).hideProgress();
        }
    }

    private void updateInfoText(View convertView, FileTransferItem item) {
        if (item.getTransferStatus() == FileTransferItem.TransferStatus.ACTIVE) {
            ((FileTransferItemView) convertView).setInfoText(item.getCurrTransferFile());
        } else {
            String wait_str = mContext.getResources().getString(R.string.waiting);
            ((FileTransferItemView) convertView).setInfoText(wait_str);
        }
    }

    private void updateTransferIcon(View convertView, FileTransferItem item) {
        Drawable icon = null;

        if ((MainFileManagerActivity.EXTRA_VAL_REASONCUT).equals(item.getTransferData()
                .getPasteReason())) {
            icon = mContext.getResources().getDrawable(R.drawable.ic_menu_move_holo);
        } else {
            icon = mContext.getResources().getDrawable(R.drawable.ic_menu_copy_holo);
        }

        if (icon != null) {
            ((FileTransferItemView) convertView).setIcon(icon);
        }
    }

    private void updateDescriptionText(View convertView, FileTransferItem item) {

        String description_text1 = null;
        int fileCount = item.getTransferData().getFileCount();
        if ((MainFileManagerActivity.EXTRA_VAL_REASONCUT).equals(item.getTransferData()
                .getPasteReason())) {
            description_text1 =
                    mContext.getResources().getQuantityString(R.plurals.move_items_to, fileCount,
                            fileCount);
        } else {
            description_text1 =
                    mContext.getResources().getQuantityString(R.plurals.copy_items_to, fileCount,
                            fileCount);
        }
        if (description_text1 != null) {
            ((FileTransferItemView) convertView).setDescriptionText1(description_text1);
        }

        String description_text2 = item.getTransferData().getTransferDest();
        description_text2 =
                description_text2 + getTotalSizeStr(item.getTransferData().getTotalFileSize());
        if (description_text2 != null) {
            ((FileTransferItemView) convertView).setDescriptionText2(description_text2);
        }

    }

    private String getTotalSizeStr(long totalFileSize) {
        String result = null;
        long fileSizeInKB = totalFileSize / 1024;
        long rem = totalFileSize % 1024;
        if (rem > 0) {
            fileSizeInKB++;
        }

        if (fileSizeInKB >= 1024) {
            long fileSizeInMB = fileSizeInKB / 1024;
            result =
                    "(" +
                            mContext.getResources().getString(R.string.size_mb,
                                    Long.toString(fileSizeInMB)) + ")";
        } else {
            result =
                    "(" +
                            mContext.getResources().getString(R.string.size_kb,
                                    Long.toString(fileSizeInKB)) + ")";
        }
        return result;
    }
}
