/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 */
package com.motorola.filemanager.filetransfer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.motorola.filemanager.R;

public class FileTransferItemView extends LinearLayout {

    private ImageView mIcon;
    private TextView mDescriptionText_1;
    private TextView mDescriptionText_2;
    private TextView mInfoText;
    private ProgressBar mProgressbar;
    private LinearLayout mProgressContainer;
    private TextView mPrecentageText;

    public FileTransferItemView(Context context) {
        super(context);
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            inflater.inflate(R.layout.file_transfer_item, this, true);
        }
        mIcon = (ImageView) findViewById(R.id.filetransfer_item_icon);
        mDescriptionText_1 = (TextView) findViewById(R.id.filetransfer_description_text_1);
        mDescriptionText_2 = (TextView) findViewById(R.id.filetransfer_description_text_2);
        mInfoText = (TextView) findViewById(R.id.filetransfer_info_text);
        mProgressbar = (ProgressBar) findViewById(R.id.filetransfer_progressBar);
        mProgressContainer = (LinearLayout) findViewById(R.id.filetransfer_progress_container);
        mPrecentageText = (TextView) findViewById(R.id.filetransfer_precentage_text);
    }

    public void showProgress() {
        if (mProgressContainer != null) {
            mProgressContainer.setVisibility(View.VISIBLE);
        }
    }

    public void hideProgress() {
        if (mProgressContainer != null) {
            mProgressContainer.setVisibility(View.GONE);
        }
    }

    public void setProgress(int progress) {
        if (mProgressbar != null) {
            mProgressbar.setProgress(progress);
        }
    }

    public void setPrecentageText(String precentage) {
        if (mPrecentageText != null) {
            mPrecentageText.setText(precentage);
        }
    }

    public void setInfoText(String info) {
        if (mInfoText != null) {
            mInfoText.setText(info);
        }
    }

    public void setDescriptionText1(String text) {
        if (mDescriptionText_1 != null) {
            mDescriptionText_1.setText(text);
        }
    }

    public void setDescriptionText2(String text) {
        if (mDescriptionText_2 != null) {
            mDescriptionText_2.setText(text);
        }
    }

    public void setIcon(Drawable icon) {
        mIcon.setImageDrawable(icon);
    }

}
