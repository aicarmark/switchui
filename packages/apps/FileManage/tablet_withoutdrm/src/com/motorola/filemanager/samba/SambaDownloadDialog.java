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
package com.motorola.filemanager.samba;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.motorola.filemanager.R;
import com.motorola.filemanager.samba.service.DownloadServer;
import com.motorola.filemanager.utils.MimeTypeUtil;

public class SambaDownloadDialog extends Activity {
    private static final String TAG = "SambaDownloadDialog: ";
    public static final String TO_FOLDER = "To:";
    public static final String FILE_NAME = "File:";
    public static final String FILE_SIZE = "File Size:";
    public static final String PROGRESS_STEP = "progress step:";
    public static final String TRANS_FILES = "Trans Files:";

    private static final String NONEMIMETYPE = "zNONEMIMETYPE";
    public final static String PROGRESS_INTENT = "com.motorola.filemanager.download.step";
    public final static String COMPLETE_INTENT = "com.motorola.filemanager.download.complete";
    public final static String PRG_VIEW_INTENT = "com.motorola.filemanager.download.progress";

    private static boolean isProgressView;

    private String mToFolder;
    private String mFileName;
    private ProgressBar mProgressBar = null;
    private TextView mStatus = null;
    private TextView mPercentage = null;
    private TextView mTVfilename = null;
    private ImageView mTVfileIcon = null;
    private TextView mText = null;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(COMPLETE_INTENT)) {
                    SambaExplorer.log(TAG + " refresh to complete dialog", false);
                    showResultView();
                    getResultParameter(intent);
                } else if (action.equals(PROGRESS_INTENT)) {
                    if (isProgressView) {
                        getProgressParameter(intent);
                    }
                } else if (action.equals(PRG_VIEW_INTENT)) {
                    SambaExplorer.log(TAG + " Prg view intent is received", false);
                    showProgressView();
                    getProgressParameter(intent);
                }
            }
        }
    };

    public static void setIsProgressView(boolean progressView) {
        isProgressView = progressView;
    }

    public static boolean getIsProgressView() {
        return isProgressView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        String action = getIntent().getAction();
        if (action != null) {
            if (action.equals(COMPLETE_INTENT)) {
                SambaExplorer.log(TAG + " Create complete result dialog", false);
                showResultView();
                getResultParameter(getIntent());
            } else if (action.equals(PRG_VIEW_INTENT)) {
                SambaExplorer.log(TAG + " Create progress dialog", false);
                try {
                    showProgressView();
                } catch (Exception e) {
                }
                getProgressParameter(getIntent());
            }
        }
        this.registerReceiver(mReceiver, new IntentFilter(PROGRESS_INTENT));
        this.registerReceiver(mReceiver, new IntentFilter(COMPLETE_INTENT));
        this.registerReceiver(mReceiver, new IntentFilter(PRG_VIEW_INTENT));
    }

    // Change current view to progress view
    public void showProgressView() {
        setIsProgressView(true);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View myView = inflater.inflate(R.layout.pending_download, null);
        FrameLayout fl = (FrameLayout) findViewById(android.R.id.custom);
        fl.addView(myView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        mProgressBar = (ProgressBar) findViewById(R.id.ProgressBar01);
        mStatus = (TextView) findViewById(R.id.transfer_status);
        mPercentage = (TextView) findViewById(R.id.transfer_percentage);
        mTVfilename = ((TextView) findViewById(R.id.filename));
        mTVfileIcon = ((ImageView) findViewById(R.id.file_mime_icon));
        // getWindow().setTitle(getText(R.string.file_transfer));
        // getWindow().getAttributes().setTitle(getText(R.string.file_transfer));
    }

    // Change current view to result view
    public void showResultView() {
        setIsProgressView(false);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        View myView = inflater.inflate(R.layout.pending_download_result, null);
        FrameLayout fl = (FrameLayout) findViewById(android.R.id.custom);

        if (fl != null) {
            fl.addView(myView, new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
        }
        mText = ((TextView) findViewById(R.id.result_text));
    }

    public void onCancel(View v) {
        Intent iDownload = new Intent(DownloadServer.ACTION_CANCEL);
        sendBroadcast(iDownload);
        finish();
    }

    public void onOk(View v) {
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(mReceiver);
        setIsProgressView(false);
        finish();
    }

    private void getProgressParameter(Intent intent) {
        try {
            String toFolder = intent.getStringExtra(TO_FOLDER);
            if (toFolder != null) {
                if (!toFolder.equals(mToFolder)) {
                    mToFolder = toFolder;
                    int index = mToFolder.lastIndexOf('/', mToFolder.length() - 3) + 1;
                    String toString =
                            this.getResources().getString(R.string.to) + " " +
                                    mToFolder.substring(index);
                    ((TextView) findViewById(R.id.destination_folder)).setText(toString);
                }
            }

            String filename = intent.getStringExtra(FILE_NAME);
            if (filename != null) {
                if (!filename.equals(mFileName)) {
                    mFileName = filename;
                    mTVfilename.setText(mFileName);
                    Drawable fileIcon = null;
                    String uristring = "file:///" + mFileName;
                    String mimeType = NONEMIMETYPE;
                    mimeType = MimeTypeUtil.getMimeType(this, mFileName);
                    if (mimeType == null) {
                        mimeType = NONEMIMETYPE;
                    }
                    Intent tempIntent = new Intent(Intent.ACTION_VIEW);
                    tempIntent.setDataAndType(Uri.parse(uristring), mimeType);
                    try {
                        fileIcon = this.getPackageManager().getActivityIcon(tempIntent);
                    } catch (android.content.pm.PackageManager.NameNotFoundException ex) {
                        fileIcon =
                                getResources().getDrawable(R.drawable.ic_thb_mimetype_unknown_file);
                    }
                    if (fileIcon != null) {
                        mTVfileIcon.setImageDrawable(fileIcon);
                        // mTVfilename
                        // .setCompoundDrawables(fileIcon, null, null, null);
                    }
                }
            }

            if (mProgressBar != null) {
                long size = intent.getLongExtra(FILE_SIZE, 0) / 1024;
                size = size < 1 ? 1 : size;
                String sizeString =
                        this.getResources().getString(R.string.size) + " " +
                                String.valueOf(size) + " " +
                                this.getResources().getString(R.string.size_k);
                // ((TextView) (mDownloadDialogView.findViewById(R.id.filesize)))
                // .setText(sizeString);

                ((TextView) findViewById(R.id.filesize)).setText(sizeString);
                int step = intent.getIntExtra(PROGRESS_STEP, 100);

                mProgressBar.setProgress(step);
                if (step != 100) {
                    mStatus.setText(R.string.transfer_progress);
                } else {
                    mStatus.setText(R.string.transfer_complete);
                }
                mPercentage.setText(String.valueOf(step) + "%");
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
    }

    public void getResultParameter(Intent intent) {
        try {
            int transFiles = intent.getIntExtra(TRANS_FILES, 0);
            if (mText != null) {
                String outText =
                        getResources().getQuantityString(R.plurals.transfer_result,
                                transFiles, transFiles);
                ((TextView) findViewById(R.id.result_text)).setText(outText);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
