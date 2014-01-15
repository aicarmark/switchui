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
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;
import com.motorola.filemanager.samba.service.DownloadServer;
import com.motorola.filemanager.samba.service.DownloadServer.MultiDelayedMsg;

public class FileTransferDialog extends Activity {

    final static String TAG = "FileTransferDialog";

    final static public int MESSAGE_PROGRESS_UPDATE = 500;
    final static public int MESSAGE_TRANSFER_COMPLETE = 501;
    final static public int MESSAGE_TRANSFER_START = 502;
    final static public int MESSAGE_FILE_COUNT_UPDATE = 503;

    public final static String FILE_NAME =
            "com.motorola.filemanager.filetransfer.FileTransferDialog.FILE_NAME";
    public final static String PROGRESS =
            "com.motorola.filemanager.filetransfer.FileTransferDialog.PROGRESS";
    public final static String TRANSFER_DIALOG_INTENT =
            "com.motorola.filemanager.filetransfer.FileTransferDialog.TRANSFER_DIALOG_INTENT";
    public final static String ERROR_DIALOG_INTENT =
            "com.motorola.filemanager.filetransfer.FileTransferDialog.ERROR_DIALOG_INTENT";
    public final static String ERROR_MESSAGE =
            "com.motorola.filemanager.filetransfer.FileTransferDialog.ERROR_MESSAGE";
    public final static String PASTE_REASON =
            "com.motorola.filemanager.filetransfer.FileTransferDialog.PASTE_REASON";

    public DownloadServer mDownloadServer = null;
    private ListView mTransferListView = null;
    private TextView mEmptyListTextView = null;
    private FileTransferListAdapter mTransferListAdapter = null;
    private Context mContext = null;
    private FileTransferDialogFragment mDialogFragment = null;
    private ArrayList<FileTransferItem> mTransferList = new ArrayList<FileTransferItem>();
    private View mDialogContentView = null;
    private String mStartAction = null;
    private Intent mIntentReceived = null;

    private ProgressDialog mCancleDialog = null;

    private Handler mHandler;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mDownloadServer = ((DownloadServer.LocalBinder) service).getService();
            if (mDownloadServer != null) {
                mDownloadServer.setTransferDialogHandler(mHandler);
                if (mStartAction != null) {
                    if (mStartAction.equals(TRANSFER_DIALOG_INTENT)) {
                        showFileTransferDialog();
                    } else if (mStartAction.equals(ERROR_DIALOG_INTENT)) {
                        showErrorDialog();
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mDownloadServer = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIntentReceived = getIntent();
        mStartAction = mIntentReceived.getAction();
        mContext = this;
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                FileTransferDialog.this.handleMessage(msg);
            }
        };
        bindService(new Intent(this, DownloadServer.class), mConnection, Context.BIND_AUTO_CREATE);

    }

    private void initDialog() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mDialogContentView = inflater.inflate(R.layout.file_transfer_dialog, null);
        mTransferListView = (ListView) mDialogContentView.findViewById(R.id.filetransfer_file_list);
        mEmptyListTextView =
                (TextView) mDialogContentView.findViewById(R.id.filetransfer_empty_list);
        mTransferListAdapter = new FileTransferListAdapter(mContext);
        if (mTransferListView != null) {
            mTransferListView.setAdapter(mTransferListAdapter);
        }
        mDialogFragment =
                new FileTransferDialogFragment(FileTransferDialogFragment.TRANSFER_DIALOG_ID,
                        mDialogContentView);
    }

    private void showFileTransferDialog() {
        initDialog();
        if (addCurrentTransferRequest()) {
            addWaitingTransferRequests();
            mTransferListAdapter.setTransferList(mTransferList);
            showlist();
            mTransferListAdapter.notifyDataSetChanged();
        } else {
            showEmptylist();
        }
        mDialogFragment.show(getFragmentManager(), null);
    }

    private boolean addCurrentTransferRequest() {
        if (mDownloadServer.getServiceState() == DownloadServer.ServiceState.STATE_IDLE) {
            return false;
        } else {
            mTransferList.add(new FileTransferItem(mDownloadServer.getCurrTransferItem(),
                    FileTransferItem.TransferStatus.ACTIVE));
            return true;
        }
    }

    private void addWaitingTransferRequests() {
        LinkedList<MultiDelayedMsg> transferList = mDownloadServer.getTransferList();
        for (MultiDelayedMsg item : transferList) {
            mTransferList.add(new FileTransferItem(item, FileTransferItem.TransferStatus.QUEUED));
        }
    }

    private void showlist() {
        if (mTransferListView != null) {
            mTransferListView.setVisibility(View.VISIBLE);
        }

        if (mEmptyListTextView != null) {
            mEmptyListTextView.setVisibility(View.GONE);
        }
    }

    private void showEmptylist() {
        if (mTransferListView != null) {
            mTransferListView.setVisibility(View.GONE);
        }

        if (mEmptyListTextView != null) {
            mEmptyListTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        mDownloadServer.setTransferDialogHandler(null);
        unbindService(mConnection);
        super.onDestroy();
    }

    private void handleMessage(Message message) {
        switch (message.what) {
            case MESSAGE_PROGRESS_UPDATE :
                Bundle data = message.getData();
                updateCurrentTransferProgress(data.getString(FILE_NAME), data.getInt(PROGRESS));
                break;
            case MESSAGE_TRANSFER_COMPLETE :
                hideCancelDialog();
                removeActiveItem();
                break;
            case MESSAGE_TRANSFER_START :
                refreshList();
                break;
            case MESSAGE_FILE_COUNT_UPDATE :
                mTransferListAdapter.notifyDataSetChanged();
                break;
        }
    }

    public void refreshList() {
        mTransferList = new ArrayList<FileTransferItem>();
        if (addCurrentTransferRequest()) {
            addWaitingTransferRequests();
            mTransferListAdapter.setTransferList(mTransferList);
            showlist();
            mTransferListAdapter.notifyDataSetChanged();
        } else {
            showEmptylist();
        }
    }

    public void removeActiveItem() {
        mTransferList.remove(getActiveTransferItem());
        mTransferListAdapter.notifyDataSetChanged();
        if (mTransferList.size() == 0) {
            showEmptylist();
        }
    }

    public void updateCurrentTransferProgress(String filename, int progress) {
        FileTransferItem activeItem;
        activeItem = getActiveTransferItem();
        if (activeItem != null) {
            activeItem.setProgress(progress);
            activeItem.setCurrTransferFile(filename);
            mTransferListAdapter.notifyDataSetChanged();
        }
    }

    public FileTransferItem getActiveTransferItem() {

        for (FileTransferItem item : mTransferList) {
            if (item.getTransferStatus() == FileTransferItem.TransferStatus.ACTIVE) {
                return item;
            }
        }
        return null;
    }

    public void showCancelDialog() {
        if (isFinishing()) {
            return;
        }
        hideCancelDialog();
        mCancleDialog = new ProgressDialog(mContext);
        if (mCancleDialog != null) {
            mCancleDialog.setMessage(getResources().getString(R.string.canceling));
            mCancleDialog.setCancelable(true);
            mCancleDialog.show();
        }
    }

    public void hideCancelDialog() {
        if (mCancleDialog != null) {
            mCancleDialog.dismiss();
            mCancleDialog = null;
        }
    }

    private void showErrorDialog() {
        if (mIntentReceived != null) {
            mDialogFragment =
                    new FileTransferDialogFragment(FileTransferDialogFragment.ERROR_DIALOG_ID, null);
            String errorMessage = mIntentReceived.getStringExtra(ERROR_MESSAGE);
            String pasteReason = mIntentReceived.getStringExtra(PASTE_REASON);
            if (errorMessage != null) {
                errorMessage =
                        errorMessage + " " + getResources().getString(R.string.error_occurred);
                mDialogFragment.setErrorDialogMessage(errorMessage);
            }
            if (pasteReason != null) {
                mDialogFragment.setPasteReason(pasteReason);
            }
            mDialogFragment.show(getFragmentManager(), null);
        }
    }

    private class FileTransferDialogFragment extends DialogFragment {

        private static final int TRANSFER_DIALOG_ID = 1;
        private static final int ERROR_DIALOG_ID = 2;

        private View mCustomView;
        private int mDialogID;
        private AlertDialog mDialog = null;
        private String mErrorMessage = null;
        private String mPasteReason = null;

        public FileTransferDialogFragment(int dialogID, View customView) {
            mCustomView = customView;
            mDialogID = dialogID;
        }

        public void setErrorDialogMessage(String errorMessage) {
            mErrorMessage = errorMessage;
        }

        public void setPasteReason(String pasteReason) {
            mPasteReason = pasteReason;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            switch (mDialogID) {
                case TRANSFER_DIALOG_ID :
                    mDialog =
                            new AlertDialog.Builder(getActivity())
                                    .setIcon(null)
                                    .setView(mCustomView)
                                    .setTitle(R.string.download_dialog_title)
                                    .setNeutralButton(R.string.done_button,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int whichButton) {
                                                    finish();
                                                }
                                            }).create();
                    break;
                case ERROR_DIALOG_ID :
                    mDialog =
                            new AlertDialog.Builder(getActivity())
                                    .setIcon(null)
                                    .setNeutralButton(android.R.string.ok,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int whichButton) {
                                                    finish();
                                                }
                                            }).create();

                    if ((mPasteReason != null) &&
                            (MainFileManagerActivity.EXTRA_VAL_REASONCUT).equals(mPasteReason)) {
                        mDialog.setTitle(mContext.getResources().getString(R.string.move_stopped));
                    } else {
                        mDialog.setTitle(mContext.getResources().getString(R.string.copy_stopped));
                    }
                    if (mErrorMessage != null) {
                        mDialog.setMessage(mErrorMessage);
                    }

                    break;
            }
            return mDialog;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            finish();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            finish();
        }
    }

}
