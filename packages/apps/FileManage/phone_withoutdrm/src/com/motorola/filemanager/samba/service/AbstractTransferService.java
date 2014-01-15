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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.motorola.filemanager.R;
import com.motorola.filemanager.samba.SambaDownloadDialog;
import com.motorola.filemanager.samba.SambaExplorer;

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
    private String mTransOngoingIndicator;
    private String mTransCompleteIndicator;

    public AbstractTransferService(OnCanelTransListener listener) {
	mListener = listener;
	SambaExplorer.log("AbstractTransferService,mHandler is: "
		+ mListener.getClass());
    }

    public AbstractTransferService(Context context,
	    OnCanelTransListener listener) {
	mContext = context;
	mListener = listener;
	SambaExplorer.log("AbstractTransferService,mHandler is: "
		+ mListener.getClass());

	getTransferOngoingIndicator();
	getTransferCompleteIndicator();
    }

    public String getTransferOngoingIndicator() {
	mTransOngoingIndicator = mContext.getResources().getString(
		R.string.file_transfer)
		+ " ";
	return mTransOngoingIndicator;
    }

    public String getTransferCompleteIndicator() {
	mTransCompleteIndicator = mContext.getResources().getString(
		R.string.file_transfer_complete)
		+ " ";
	return mTransCompleteIndicator;
    }

    protected boolean loadingCanceled() {
	return mListener.isCanceled();
    }

    public int getTransMode(String mode) {
	return NOT_SUPPORT_MODE;
    }

    /**
     * Callback interface that to check whether current action should be
     * canceled.
     */
    public interface OnCanelTransListener {
	public boolean isCanceled();
    }

    /**
     * renames files within the same local disk
     * 
     * @param destLocal
     *            The local path should be in absolute way
     * @param srcLocal
     *            The local path should be in absolute way
     * 
     * @return TRANSFER_FAIL - Transfer of 1 file failed TRANSFER_COMPLETE -
     *         Transfer of 1 file succeeded TRANSFER_MULTI_FAIL - Transfer of
     *         multiple files failed TRANSFER_MULTI_COMPLETE - Transfer of
     *         multiple files succeeded
     */
    public abstract int localRename(String destLocal, String srcRemote);

    /**
     * transfers file from Local to local disk
     * 
     * @param destLocal
     *            The local path should be in absolute way
     * @param srcLocal
     *            The local path should be in absolute way
     * @param deleteAfter
     *            Whether to delete the source file after transferring
     * @return TRANSFER_FAIL - Transfer of 1 file failed TRANSFER_COMPLETE -
     *         Transfer of 1 file succeeded TRANSFER_MULTI_FAIL - Transfer of
     *         multiple files failed TRANSFER_MULTI_COMPLETE - Transfer of
     *         multiple files succeeded
     */
    public abstract int local2Local(String destLocal, String srcRemote,
	    boolean deleteAfter);

    /**
     * transfer file from remote server to local disk
     * 
     * @param destLocal
     *            The local path should be in absolute way
     * @param srcRemote
     *            The remote server url
     * @param deleteAfter
     *            Whether to delete the source file after transferring
     * @return TRANSFER_FAIL - Transfer of 1 file failed TRANSFER_COMPLETE -
     *         Transfer of 1 file succeeded TRANSFER_MULTI_FAIL - Transfer of
     *         multiple files failed TRANSFER_MULTI_COMPLETE - Transfer of
     *         multiple files succeeded
     */
    public abstract int remote2Local(String destLocal, String srcRemote,
	    boolean deleteAfter);

    /**
     * transfer file from local disk to remote server
     * 
     * @param srcLocal
     *            The local path should be in absolute way
     * @param destRemote
     *            The remote server url
     * @param deleteAfter
     *            Whether to delete the source file after transferring
     * @return TRANSFER_FAIL - Transfer of 1 file failed TRANSFER_COMPLETE -
     *         Transfer of 1 file succeeded TRANSFER_MULTI_FAIL - Transfer of
     *         multiple files failed TRANSFER_MULTI_COMPLETE - Transfer of
     *         multiple files succeeded
     */
    public abstract int local2Remote(String destRemote, String srcLocal,
	    boolean deleteAfter);

    /**
     * copy file from remote server to remote server
     * 
     * @param srcRemote
     *            The remote server url with identified path
     * @param destRemote
     *            The remote server url with identified path
     * @param deleteAfter
     *            Whether to delete the source file after transferring
     * @return TRANSFER_FAIL - Transfer of 1 file failed TRANSFER_COMPLETE -
     *         Transfer of 1 file succeeded TRANSFER_MULTI_FAIL - Transfer of
     *         multiple files failed TRANSFER_MULTI_COMPLETE - Transfer of
     *         multiple files succeeded
     */
    public abstract int remote2Remote(String destRemote, String srcRemote,
	    boolean deleteAfter);

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
    private Notification mNotifResult = null;
    private boolean refreshTicker = false;
    private int transFiles = 0;

    static final int BASE_SIZE = 1024;

    private long length = 0;

    protected void setLength(long v) {
	length = v;
	return;
    }

    protected long getLength() {
	return length;
    }

    protected void showNotification(String transferTitle, String filenameStr,
	    long max, long step, boolean indet, String toFolder, String file,
	    String fileType) {
	try {
	    if (mNotificationManager == null) {
		mNotificationManager = (NotificationManager) mContext
			.getSystemService(Context.NOTIFICATION_SERVICE);
	    }

	    String indicateText = transferTitle + filenameStr;
	    long now = System.currentTimeMillis();
	    boolean needUpdate = false;
	    int barProgress = 0;

	    if (mNotif == null) {
		refreshTicker = true;
		mNotif = new Notification();
		mNotif.icon = R.drawable.stat_sys_download;
		mNotif.tickerText = indicateText;
		mNotif.flags |= Notification.FLAG_AUTO_CANCEL;
		mNotif.flags |= Notification.FLAG_NO_CLEAR;
		mNotif.flags |= Notification.FLAG_ONGOING_EVENT;
		RemoteViews nmView = new RemoteViews(mContext.getPackageName(),
			R.layout.notify);
		mNotif.contentView = nmView;
		PendingIntent contentIntent = PendingIntent.getActivity(
			mContext, 0,
			createPendDownloadIntent(toFolder, file, fileType),
			PendingIntent.FLAG_UPDATE_CURRENT);
		mNotif.contentIntent = contentIntent;
		pastTicker = now;
		// For first one, clear past result Notification
		mNotificationManager.cancel(R.layout.notify + 1);
	    }

	    if (max != TRANS_FLAG) {
		if ((now - pastOngoing) > INTERVAL_ONGOING) {
		    // Update notification view
		    barProgress = (int) ((100 * step) / max);
		    String percent = mContext.getResources().getString(
			    R.string.percent_sign);
		    String appName = mContext.getResources().getString(
			    R.string.app_name);
		    mNotif.contentView.setTextViewText(R.id.TextView01, appName
			    + ": " + transferTitle);
		    mNotif.contentView.setTextViewText(R.id.TextView02,
			    filenameStr);
		    mNotif.contentView.setTextViewText(R.id.ProgressPercent,
			    barProgress + percent);
		    mNotif.contentView.setProgressBar(R.id.ProgressBar01, 100,
			    barProgress, indet);
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
		case TRANS_START:
		    // Update content intent of ongoing notification while
		    // beginning to
		    // transfer
		    Intent intent1 = createPendDownloadIntent();
		    intent1.putExtra(SambaDownloadDialog.TO_FOLDER, toFolder);
		    intent1.putExtra(SambaDownloadDialog.FILE_NAME, file);
		    intent1.putExtra(SambaDownloadDialog.PROGRESS_STEP, 0);
		    intent1.putExtra(SambaDownloadDialog.FILE_SIZE, getLength());
		    PendingIntent contentIntent1 = PendingIntent.getActivity(
			    mContext, 0, intent1,
			    PendingIntent.FLAG_UPDATE_CURRENT);
		    mNotif.contentIntent = contentIntent1;
		    break;
		case TRANS_END:
		    // reset flag if transfer completed
		    refreshTicker = false;
		    // Show result notification while file is transferred
		    // successfully
		    transFiles++;
		    Intent intent2 = createPendDownloadIntent();
		    intent2.putExtra(SambaDownloadDialog.TO_FOLDER, toFolder);
		    intent2.putExtra(SambaDownloadDialog.FILE_NAME, file);
		    intent2.putExtra(SambaDownloadDialog.PROGRESS_STEP, 100);
		    intent2.putExtra(SambaDownloadDialog.FILE_SIZE, getLength());
		    PendingIntent contentIntent2 = PendingIntent.getActivity(
			    mContext, 0, intent2,
			    PendingIntent.FLAG_UPDATE_CURRENT);
		    mNotif.contentIntent = contentIntent2;
		    if ((now - pastTicker) > INTERVAL_TICKER) {
			pastTicker = now;
			String text = mContext.getResources().getText(
				R.string.file_transfer_complete)
				+ " " + file;
			showResultNotification(text, mNotif.contentIntent);

			// If next transfer is slow, then refresh ticker once
			refreshTicker = true;
		    }
		    break;

		case TRANS_FAIL:
		case TRANS_IGNORE:
		default:
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

		if (SambaDownloadDialog.getIsProgressView()) {
		    Intent broadcastIntent = new Intent(
			    SambaDownloadDialog.PROGRESS_INTENT);
		    broadcastIntent.putExtra(SambaDownloadDialog.PROGRESS_STEP,
			    barProgress);
		    broadcastIntent.putExtra(SambaDownloadDialog.FILE_NAME,
			    file);
		    broadcastIntent.putExtra(SambaDownloadDialog.TO_FOLDER,
			    toFolder);
		    broadcastIntent
			    .putExtra(SambaDownloadDialog.FILE_SIZE, max);
		    mContext.sendBroadcast(broadcastIntent);
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
	    String text = String
		    .format(mContext.getResources().getString(
			    R.string.transfer_result), transFiles);
	    SambaExplorer.log(text);

	    Intent intent = new Intent(SambaDownloadDialog.COMPLETE_INTENT,
		    null);
	    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
		    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
		    | Intent.FLAG_ACTIVITY_NO_HISTORY
		    | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
	    intent.setClassName(mContext, getDownloadDialogClassName());
	    intent.putExtra(SambaDownloadDialog.TRANS_FILES, transFiles);
	    PendingIntent contentIntent = PendingIntent.getActivity(mContext,
		    0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	    showResultNotification(text, contentIntent);

	    Intent broadcastIntent = new Intent(
		    SambaDownloadDialog.COMPLETE_INTENT);
	    broadcastIntent.putExtra(SambaDownloadDialog.TRANS_FILES,
		    transFiles);
	    mContext.sendBroadcast(broadcastIntent);
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
    protected void showResultNotification(String text,
	    PendingIntent contentIntent) {

	if (mNotificationManager == null) {
	    mNotificationManager = (NotificationManager) mContext
		    .getSystemService(Context.NOTIFICATION_SERVICE);
	}

	if (mNotifResult == null) {
	    mNotifResult = new Notification(R.drawable.stat_sys_download_done,
		    text, System.currentTimeMillis());
	    mNotifResult.flags |= Notification.FLAG_AUTO_CANCEL;
	} else {
	    mNotificationManager.cancel(R.layout.notify + 1);
	    mNotifResult.tickerText = text;
	}

	mNotifResult.setLatestEventInfo(mContext, mContext.getResources()
		.getText(R.string.app_name), text, contentIntent);
	mNotificationManager.notify(R.layout.notify + 1, mNotifResult);
    }

    protected Intent createPendDownloadIntent() {
	Intent intent = new Intent(SambaDownloadDialog.PRG_VIEW_INTENT, null);
	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
		| Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
		| Intent.FLAG_ACTIVITY_NO_HISTORY
		| Intent.FLAG_ACTIVITY_NO_USER_ACTION);
	intent.setClassName(mContext, getDownloadDialogClassName());
	return intent;
    }

    /* package */
    protected Intent createPendDownloadIntent(String toFolder, String file,
	    String fileType) {
	Intent intent = createPendDownloadIntent();
	// new Intent(Intent.ACTION_MAIN, null);
	// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
	// | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
	// | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
	intent.putExtra(SambaDownloadDialog.TO_FOLDER, toFolder);
	intent.putExtra(SambaDownloadDialog.FILE_NAME, file);
	intent.putExtra(SambaDownloadDialog.PROGRESS_STEP, 0);
	intent.putExtra(SambaDownloadDialog.FILE_SIZE, (long) 0);

	// intent.setClassName(mContext, getDownloadDialogClassName());
	return intent;
    }

    static String getDownloadDialogClassName() {
	return SambaDownloadDialog.class.getName();
    }

    protected void addTransFileCount(int count) {
	transFiles = transFiles + count;
    }
}
