/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date       CR                Author      Description
 * 2010-03-23   IKSHADOW-2425   A20815      initial
 */

package com.motorola.filemanager.multiselect.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.motorola.sdcardbackuprestore.R;

import com.motorola.filemanager.multiselect.FileManagerApp;

public class IconifiedTextView extends LinearLayout {
  private TextView mText;
  private ImageView mIcon;
  private TextView mInfoView;
  private TextView mInfoTime;

  public IconifiedTextView(Context context, IconifiedText aIconifiedText) {
    super(context);
    LayoutInflater inflater =
        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    if (FileManagerApp.isGridView() && aIconifiedText.isNormalFile()) {
      inflater.inflate(R.layout.grid_item, this, true);
    } else {
      inflater.inflate(R.layout.filelist_item, this, true);
    }
    mIcon = (ImageView) findViewById(R.id.icon);
    mText = (TextView) findViewById(R.id.text);
    mInfoView = (TextView) findViewById(R.id.info);
    mInfoTime = (TextView) findViewById(R.id.info_time);
  }

  public void setText(String words) {
    mText.setText(words);
  }

  public void setDisabled(boolean isOnline) {
    if (isOnline) {
      setEnabled(true);
      mText.setEnabled(true);
      mInfoView.setEnabled(true);
    } else {
      setEnabled(false);
      mText.setEnabled(false);
      mInfoView.setEnabled(false);
    }
  }

  public void setInfo(String info) {
    if (FileManagerApp.isGridView() || (info == null) || (info.length() == 0)) {
      mInfoView.setVisibility(View.GONE);
    } else {
      mInfoView.setVisibility(View.VISIBLE);
    }
    mInfoView.setText(info);
  }

  public void setIcon(Drawable bullet) {
    mIcon.setImageDrawable(bullet);
  }

  public void setTime(String time) {
    mInfoTime.setText(time);
  }
}
