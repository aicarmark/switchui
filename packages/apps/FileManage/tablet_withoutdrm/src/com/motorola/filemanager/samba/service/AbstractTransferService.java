/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 *  Date            CR                Author               Description
 *  2010-03-23  IKSHADOW-2074        E12758                   initial
 */
package com.motorola.filemanager.samba.service;

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.RemoteViews;

import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;
import com.motorola.filemanager.filetransfer.FileTransferDialog;
import com.motorola.filemanager.samba.SambaExplorer;
import com.motorola.filemanager.samba.service.DownloadServer.MultiDelayedMsg;

public abstract class AbstractTransferService {
    public final static int NOT_SUPPORT_MODE = 0;

    public final static int SAMBA_MODE = 1;
    public final static int FTP_MODE = 2;
    final static int TRANSFER_DOWN = 1;
    final static int TRANSFER_UP = 2;
    final static int TRANSFER_CANCEL = 3;
    final static int TRANSFER_STOP = 4;
    private OnCanelTransListener mListener = null;
    protected Context mContext;

    public class TransferResults {
        public int result;
        public int numOfItemsTransferred;
    }

    public AbstractTransferService(OnCanelTransListener listener) {
        mListener = listener;
        SambaExplorer.log("AbstractTransferService,mHandler is: " + mListener.getClass(), false);
    }

    public AbstractTransferService(Context context, OnCanelTransListener listener) {
        mContext = context;
        mListener = listener;
        SambaExplorer.log("AbstractTransferService,mHandler is: " + mListener.getClass(), false);

        getTransferOngoingIndicator();
        getTransferCompleteIndicator();
    }

    public ArrayList<String> getTransferOngoingIndicator() {
        ArrayList<String> resultStrings = new ArrayList<String>();
        MultiDelayedMsg item = ((DownloadServer) mContext).getCurrTransferItem();
        if (item != null) {
            String description_text1 = null;
            int fileCount = item.getFileCount();
            if ((MainFileManagerActivity.EXTRA_VAL_REASONCUT).equals(item.getPasteReason())) {
                description_text1 =
                        mContext.getResources().getQuantityString(R.plurals.move_items_to,
                                fileCount, fileCount);
            } else {
                description_text1 =
                        mContext.getResources().getQuantityString(R.plurals.copy_items_to,
                                fileCount, fileCount);
            }
            if (description_text1 != null) {
                resultStrings.add(description_text1);
            }

            String description_text2 = item.getTransferDest();
            description_text2 = description_text2 + getTotalSizeStr(item.getTotalFileSize());
            if (description_text2 != null) {
                resultStrings.add(description_text2);
            }

        }
        return resultStrings;
    }

    public ArrayList<String> getTransferCompleteIndicator() {
        return getTransferOngoingIndicator();
        /*
        mTransCompleteIndicator =
                mContext.getResources().getString(R.string.file_transfer_complete) + " ";
        return mTransCompleteIndicator;
        */
    }

    public int getNotificationIcon() {

        MultiDelayedMsg item = ((DownloadServer) mContext).getCurrTransferItem();
        if (item != null) {
            if ((MainFileManagerActivity.EXTRA_VAL_REASONCUT).equals(item.getPasteReason())) {
                return R.drawable.ic_menu_move_holo;
            } else {
                return R.drawable.ic_menu_copy_holo;
            }
        }
        return android.R.drawable.stat_sys_download;
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

    protected boolean loadingCanceled() {
        return mListener.isCurrTransferCanceled();
    }

    public int getTransMode(String mode) {
        return NOT_SUPPORT_MODE;
    }

    /**
     *Callback interface that to check whether current action should be
     * canceled.
     */
    public interface OnCanelTransListener {
        public boolean isCurrTransferCanceled();
    }

    /**
     * creates an instance of TransferResults based on the values passed in
     * @param result
     *             The result value to set
     * @param num
     *             The number value to set
     * @return TransferResults - instance of TransferResults that's created
     */
    public TransferResults createTransferResults(int result, int num) {
        TransferResults transferResults = new TransferResults();
        transferResults.result = result;
        transferResults.numOfItemsTransferred = num;
        return transferResults;
    }

    /**
     *renames files within the same local disk
     *
     * @param destLocal
     *            The local path should be in absolute way
     * @param srcLocal
     *            The local path should be in absolute way
     * @return TransferResults
     *             result:
     *                 TRANSFER_FAIL - Transfer failed
     *                 TRANSFER_COMPLETE - Transfer succeeded
     *             numOfItemsTransferred - number of items transferred
     *                 failed cases do not show this value in msg to user
     */
    public abstract TransferResults localRename(String destLocal, String srcRemote);

    /**
     *transfers file from Local to local disk
     *
     * @param destLocal
     *            The local path should be in absolute way
     * @param srcLocal
     *            The local path should be in absolute way
     * @param deleteAfter
     *            Whether to delete the source file after transferring
     * @return TransferResults
     *             result:
     *                 TRANSFER_FAIL - Transfer failed
     *                 TRANSFER_COMPLETE - Transfer succeeded
     *             numOfItemsTransferred - number of items transferred
     *                 failed cases do not show this value in msg to user
     */
    public abstract TransferResults local2Local(String destLocal, String srcRemote, boolean deleteAfter);

    /**
     *transfer file from remote server to local disk
     *
     * @param destLocal
     *            The local path should be in absolute way
     * @param srcRemote
     *            The remote server url
     * @param deleteAfter
     *            Whether to delete the source file after transferring
     * @return TransferResults
     *             result:
     *                 TRANSFER_FAIL - Transfer failed
     *                 TRANSFER_COMPLETE - Transfer succeeded
     *             numOfItemsTransferred - number of items transferred
     *                 failed cases do not show this value in msg to user
     */
    public abstract TransferResults remote2Local(String destLocal, String srcRemote, boolean deleteAfter);

    /**
     *transfer file from local disk to remote server
     *
     * @param srcLocal
     *            The local path should be in absolute way
     * @param destRemote
     *            The remote server url
     * @param deleteAfter
     *            Whether to delete the source file after transferring
     * @return TransferResults
     *             result:
     *                 TRANSFER_FAIL - Transfer failed
     *                 TRANSFER_COMPLETE - Transfer succeeded
     *             numOfItemsTransferred - number of items transferred
     *                 failed cases do not show this value in msg to user
     */
    public abstract TransferResults local2Remote(String destRemote, String srcLocal, boolean deleteAfter);

    /**
     *copy file from remote server to remote server
     *
     * @param srcRemote
     *            The remote server url with identified path
     * @param destRemote
     *            The remote server url with identified path
     * @param deleteAfter
     *            Whether to delete the source file after transferring
     * @return TransferResults
     *             result:
     *                 TRANSFER_FAIL - Transfer failed
     *                 TRANSFER_COMPLETE - Transfer succeeded
     *             numOfItemsTransferred - number of items transferred
     *                 failed cases do not show this value in msg to user
     */
    public abstract TransferResults remote2Remote(String destRemote, String srcRemote, boolean deleteAfter);

    public final static int TRANS_FLAG = -1;
    public final static int TRANS_START = 0;
    public final static int TRANS_END = 1;
    public final static int TRANS_FAIL = 2;
    public final static int TRANS_IGNORE = 3;

    private final static int INTERVAL_ONGOING = 1200;
    private final static int INTERVAL_TICKER = 3000;
    private static NotificationManager mNotificationManager = null;
    private static long pastOngoing = 0;
    private static long pastTicker = 0;

    private Notification mNotif = null;
    private Notification.Builder mNotifResult = null;
    private Notification mNotifError = null;
    private boolean refreshTicker = false;
    private int transFiles = 0;
    private int mErrorNotifID = R.layout.notify + 2;

    static final int BASE_SIZE = 1024;

    protected void showNotification(ArrayList<String> descriptionText, long max, long step,
                                    boolean indet, String toFolder, String file, String fileType) {
        try {
            if (mNotificationManager == null) {
                mNotificationManager =
                        (NotificationManager) mContext
                                .getSystemService(Context.NOTIFICATION_SERVICE);
            }

            String indicateText = descriptionText.get(0) + descriptionText.get(1);
            long now = System.currentTimeMillis();
            boolean needUpdate = false;
            int barProgress = 0;
            if (mNotif == null) {
                mNotif = new Notification();
                mNotif.flags |= Notification.FLAG_AUTO_CANCEL;
                mNotif.flags |= Notification.FLAG_NO_CLEAR;
                mNotif.flags |= Notification.FLAG_ONGOING_EVENT;
                RemoteViews nmView = new RemoteViews(mContext.getPackageName(), R.layout.notify);
                mNotif.contentView = nmView;
                PendingIntent contentIntent =
                        PendingIntent.getActivity(mContext, 0,
                                createPendDownloadIntent(toFolder, file, fileType),
                                PendingIntent.FLAG_UPDATE_CURRENT);
                mNotif.contentIntent = contentIntent;
                pastTicker = now;
                // For first one, clear past result Notification
                mNotificationManager.cancel(R.layout.notify + 1);
            }
            mNotif.icon = getNotificationIcon();
            mNotif.tickerText = indicateText;

            if (max != TRANS_FLAG) {
                if ((now - pastOngoing) > INTERVAL_ONGOING) {
                    // Update notification view
                    if (max != 0) {
                        barProgress = (int) ((100 * step) / max);
                    } else {
                        barProgress = 0;
                    }
                    mNotif.contentView.setTextViewText(R.id.FileInfo, file);
                    mNotif.contentView.setImageViewResource(R.id.Icon01, getNotificationIcon());
                    mNotif.contentView.setTextViewText(R.id.DescriptionText1,
                            descriptionText.get(0));
                    mNotif.contentView.setTextViewText(R.id.DescriptionText2,
                            descriptionText.get(1));
                    mNotif.contentView.setTextViewText(R.id.ProgressPercent, barProgress + "%");
                    mNotif.contentView.setProgressBar(R.id.ProgressBar01, 100, barProgress, indet);
                    needUpdate = true;

                    // If transfer is slow, then refresh ticker once
                    if (refreshTicker && (now - pastTicker) > INTERVAL_TICKER) {
                        pastTicker = now;
                        refreshTicker = false;
                        // update ticker text to show transferring ticker again
                        mNotif.tickerText = indicateText;
                    }
                }
            } else {
                switch ((int) step) {
                    case TRANS_START :
                        // Update content intent of ongoing notification while beginning to
                        // transfer
                        PendingIntent contentIntent =
                                PendingIntent.getActivity(mContext, 0,
                                        createPendDownloadIntent(toFolder, file, fileType),
                                        PendingIntent.FLAG_UPDATE_CURRENT);
                        mNotif.contentIntent = contentIntent;
                        Handler dialogHandler =
                                ((DownloadServer) mContext).getTransferDialogHandler();
                        if (dialogHandler != null) {
                            Message msg = Message.obtain();
                            msg.what = FileTransferDialog.MESSAGE_TRANSFER_START;
                            dialogHandler.sendMessage(msg);
                        }
                        break;
                    case TRANS_END :
                        // reset flag if transfer completed
                        refreshTicker = false;
                        // Show result notification while file is transferred successfully
                        transFiles++;
                        if ((now - pastTicker) > INTERVAL_TICKER) {
                            pastTicker = now;
                            String text =
                                    mContext.getResources()
                                            .getText(R.string.file_transfer_complete) + " " + file;
                            showResultNotification(text, mNotif.contentIntent);

                            // If next transfer is slow, then refresh ticker once
                            refreshTicker = true;
                        }
                        break;

                    case TRANS_FAIL :
                    case TRANS_IGNORE :
                    default :
                        // reset flag if transfer completed
                        refreshTicker = false;
                        break;
                }
            }

            if (needUpdate) {
                // set update notification time
                pastOngoing = now;

                // Show or update notification
                mNotificationManager.notify(R.layout.notify, mNotif);

                Handler dialogHandler = ((DownloadServer) mContext).getTransferDialogHandler();
                if (dialogHandler != null) {
                    Message msg = Message.obtain();
                    msg.what = FileTransferDialog.MESSAGE_PROGRESS_UPDATE;
                    Bundle extras = new Bundle();
                    extras.putString(FileTransferDialog.FILE_NAME, file);
                    extras.putInt(FileTransferDialog.PROGRESS, barProgress);
                    msg.setData(extras);
                    dialogHandler.sendMessage(msg);
                }
            }

            // Thread.currentThread().yield();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // On completed, show transfer result notification
    public void onComplete() {
        try {
            if (mNotificationManager != null) {
                mNotificationManager.cancel(R.layout.notify);
            }
            String text =
                    mContext.getResources().getQuantityString(R.plurals.transfer_result,
                            transFiles, transFiles);
            SambaExplorer.log(text, false);

            MultiDelayedMsg item = ((DownloadServer) mContext).getCurrTransferItem();

            if ((item != null) && (transFiles != item.getFileCount())) {
                showErrorNotification();
            } else {
                Intent intent = new Intent(FileTransferDialog.TRANSFER_DIALOG_INTENT, null);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                        Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
                intent.setClassName(mContext, getDownloadDialogClassName());

                PendingIntent contentIntent =
                        PendingIntent.getActivity(mContext, 0, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT);

                showResultNotification(text, contentIntent);
            }

            Handler dialogHandler = ((DownloadServer) mContext).getTransferDialogHandler();
            if (dialogHandler != null) {
                Message msg = Message.obtain();
                msg.what = FileTransferDialog.MESSAGE_TRANSFER_COMPLETE;
                dialogHandler.sendMessage(msg);
            }

            setpastOngoing(0);
            setpastTicker(0);
            transFiles = 0;
            refreshTicker = false;
            mNotif = null;
            mNotifResult = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setpastOngoing(long ongoing) {
        pastOngoing = ongoing;
    }

    private static void setpastTicker(long ticker) {
        pastTicker = ticker;
    }

    // Show toast notification for transfer result
    protected void showResultNotification(String text, PendingIntent contentIntent) {

        if (mNotificationManager == null) {
            mNotificationManager =
                    (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        if (mNotifResult == null) {
            mNotifResult = new Notification.Builder(mContext);
            mNotifResult.setSmallIcon(android.R.drawable.stat_sys_download_done);
            mNotifResult.setWhen(System.currentTimeMillis());
            mNotifResult.setAutoCancel(true);
        } else {
            mNotificationManager.cancel(R.layout.notify + 1);
        }
        mNotifResult.setContentText(text);
        mNotifResult.setContentTitle(mContext.getResources().getText(R.string.app_name));
        mNotifResult.setContentIntent(contentIntent);
        mNotificationManager.notify(R.layout.notify + 1, mNotifResult.getNotification());
    }

    private void showErrorNotification() {

        if (mNotificationManager == null) {
            mNotificationManager =
                    (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        ArrayList<String> descriptionText = getTransferOngoingIndicator();
        String indicateText = descriptionText.get(0) + descriptionText.get(1);

        mNotifError = new Notification();
        mNotifError.icon = getNotificationIcon();
        mNotifError.tickerText = indicateText;
        mNotifError.flags |= Notification.FLAG_AUTO_CANCEL;
        RemoteViews nmView = new RemoteViews(mContext.getPackageName(), R.layout.notify);
        mNotifError.contentView = nmView;

        Intent intent = new Intent(FileTransferDialog.ERROR_DIALOG_INTENT, null);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        intent.putExtra(FileTransferDialog.ERROR_MESSAGE, indicateText);
        MultiDelayedMsg item = ((DownloadServer) mContext).getCurrTransferItem();
        if (item != null) {
            intent.putExtra(FileTransferDialog.PASTE_REASON, item.getPasteReason());
        }
        intent.setClassName(mContext, getDownloadDialogClassName());

        PendingIntent contentIntent =
                PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mNotifError.contentIntent = contentIntent;
        mNotifError.contentView.setTextViewText(R.id.FileInfo,
                mContext.getResources().getString(R.string.error_occurred));
        mNotifError.contentView.setImageViewResource(R.id.Icon01, getNotificationIcon());
        mNotifError.contentView.setTextViewText(R.id.DescriptionText1, descriptionText.get(0));
        mNotifError.contentView.setTextViewText(R.id.DescriptionText2, descriptionText.get(1));
        mNotifError.contentView.setViewVisibility(R.id.ProgressBarContainer, View.GONE);
        mNotificationManager.notify(mErrorNotifID, mNotifError);
        incErrorNotifID();
    }

    private void incErrorNotifID() {
        if (mErrorNotifID < Integer.MAX_VALUE) {
            mErrorNotifID++;
        } else {
            mErrorNotifID = R.layout.notify + 2;
        }
    }

    protected Intent createPendDownloadIntent(String toFolder, String file, String fileType) {
        Intent intent = new Intent(FileTransferDialog.TRANSFER_DIALOG_INTENT, null);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        intent.setClassName(mContext, getDownloadDialogClassName());
        return intent;
    }

    static String getDownloadDialogClassName() {
        return FileTransferDialog.class.getName();
    }

    protected void addTransFileCount(int count) {
        transFiles = transFiles + count;
    }
}
