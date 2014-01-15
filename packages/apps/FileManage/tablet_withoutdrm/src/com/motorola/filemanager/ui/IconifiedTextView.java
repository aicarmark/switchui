/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR              Author      Description
 * 2011-05-23   IKTABLETMAIN-348    XQH748      initial
 */
package com.motorola.filemanager.ui;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.R;

public class IconifiedTextView extends LinearLayout {
    public static enum TEXTVIEW_TYPE {
        TITLE_ONLY, TITLE_WITH_CONTEXT, NON_TITLE;
    }
    private TextView mText;
    IconifiedText mIconifiedText;
    private ImageView mIcon;
    private TextView mInfoView;
    private TextView mInfoTime;
    private RelativeLayout mInfoTimeContainer;
    private TextView mType;
    private TextView mSize;
    private LinearLayout mLLyout;
    private RelativeLayout mTLyout;
    private RelativeLayout mSLyout;
    private RelativeLayout mContextIcon;
    private TEXTVIEW_TYPE mTextViewType;
    private Context mContext = null;

    public IconifiedTextView(Context context, IconifiedText aIconifiedText,
                             TEXTVIEW_TYPE textviewType, Handler handler) {        
        super(context);
        mContext = context;
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mTextViewType = textviewType;
        mIconifiedText = aIconifiedText;

        if (mTextViewType != TEXTVIEW_TYPE.NON_TITLE) {
            inflater.inflate(R.layout.tfilelist_item, this, true);
            if (mTextViewType == TEXTVIEW_TYPE.TITLE_WITH_CONTEXT) {
                ImageView tConMenuIcon = (ImageView) findViewById(R.id.title_contextMenu);
                if (tConMenuIcon != null) {
                    tConMenuIcon.setVisibility(View.VISIBLE);
                }
            }
        } else if (((FileManagerApp) mContext.getApplicationContext()).getViewMode() == FileManagerApp.COLUMNVIEW) {
            inflater.inflate(R.layout.col_filelist_item, this, true);
        } else {
            if (((FileManagerApp) mContext.getApplicationContext()).getViewMode() == FileManagerApp.GRIDVIEW &&
                    aIconifiedText.isNormalFile()) {
                inflater.inflate(R.layout.grid_item, this, true);
            } else {
                inflater.inflate(R.layout.filelist_item, this, true);
            }

        }
        mType = (TextView) findViewById(R.id.info_type);
        mSize = (TextView) findViewById(R.id.info_size);
        mLLyout = (LinearLayout) findViewById(R.id.text_info);
        mTLyout = (RelativeLayout) findViewById(R.id.text_type);
        mSLyout = (RelativeLayout) findViewById(R.id.text_size);
        mIcon = (ImageView) findViewById(R.id.icon);
        mText = (TextView) findViewById(R.id.text);
        mInfoView = (TextView) findViewById(R.id.info);
        mInfoTime = (TextView) findViewById(R.id.info_time);
        mInfoTimeContainer = (RelativeLayout) findViewById(R.id.text_time);
        mContextIcon = (RelativeLayout) findViewById(R.id.filebrowser_contextMenuContainer);

        if ((mTextViewType == TEXTVIEW_TYPE.NON_TITLE) &&
                !(((FileManagerApp) mContext.getApplicationContext()).getViewMode() == FileManagerApp.GRIDVIEW) &&
                !(((FileManagerApp) mContext.getApplicationContext()).getViewMode() == FileManagerApp.COLUMNVIEW)) {
            setConfig(getResources().getConfiguration().orientation);
        }
    }

    public void setConfig(int orientation) {
        TypedValue outValue = new TypedValue();
        View parent = (View) this.getParent();
        boolean isGridparent = false;
        if (parent != null && parent.getId() == R.id.file_grid) {
            isGridparent = true;
        }
        if ((mTextViewType == TEXTVIEW_TYPE.NON_TITLE) &&
                !(((FileManagerApp) mContext.getApplicationContext()).getViewMode() == FileManagerApp.GRIDVIEW) &&
                !(((FileManagerApp) mContext.getApplicationContext()).getViewMode() == FileManagerApp.COLUMNVIEW) &&
                !isGridparent) {

            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                getResources().getValue(R.fraction.rel_layout_name_por, outValue, false);
                float weight_llyout = outValue.getFloat();
                getResources().getValue(R.fraction.rel_weight_textinfod_port, outValue, false);
                float weight_time = outValue.getFloat();
                if ((mInfoTimeContainer != null) &&
                        ((LinearLayout.LayoutParams) mInfoTimeContainer.getLayoutParams() != null)) {
                    ((LinearLayout.LayoutParams) mInfoTimeContainer.getLayoutParams()).weight =
                            weight_time;
                }
                ((LinearLayout.LayoutParams) mLLyout.getLayoutParams()).weight = weight_llyout;

                getResources().getValue(R.fraction.rel_weight_contextmenu_port, outValue, false);
                float weight_contextmenu = outValue.getFloat();
                ((LinearLayout.LayoutParams) mContextIcon.getLayoutParams()).weight =
                        weight_contextmenu;

                mTLyout.setVisibility(View.GONE);
                mSLyout.setVisibility(View.GONE);
            } else {
                getResources().getValue(R.fraction.rel_layout_name, outValue, false);
                float weight_llyout = outValue.getFloat();
                getResources().getValue(R.fraction.rel_weight_textinfod, outValue, false);
                float weight_time = outValue.getFloat();
                if ((mInfoTimeContainer != null) &&
                        ((LinearLayout.LayoutParams) mInfoTimeContainer.getLayoutParams() != null)) {
                    ((LinearLayout.LayoutParams) mInfoTimeContainer.getLayoutParams()).weight =
                            weight_time;
                }
                ((LinearLayout.LayoutParams) mLLyout.getLayoutParams()).weight = weight_llyout;
                getResources().getValue(R.fraction.rel_layout_contextmenu, outValue, false);
                float weight_contextmenu = outValue.getFloat();
                ((LinearLayout.LayoutParams) mContextIcon.getLayoutParams()).weight =
                        weight_contextmenu;
                mTLyout.setVisibility(View.VISIBLE);
                mSLyout.setVisibility(View.VISIBLE);
            }
        }
    }

    public void setPosition(int position) {
    }

    public void setText(String words) {
        mText.setText(words);
    }

    public IconifiedText getIconText() {
        return mIconifiedText;
    }

    public CharSequence getText() {
        return mText.getText();
    }

    public void setDisabled(boolean isOnline) {
        if (isOnline) {
            mText.setClickable(true);
            mText.setEnabled(true);
        } else {
            mText.clearFocus();
            mText.setEnabled(false);
        }
    }

    public void hideType() {
        mSize.setVisibility(View.VISIBLE);
    }

    public void hideSize() {
        mSize.setVisibility(View.GONE);
    }

    public void setInfo(String info, boolean isDirectory) {
        if ((mTextViewType == TEXTVIEW_TYPE.NON_TITLE) &&
                !(((FileManagerApp) mContext.getApplicationContext()).getViewMode() == FileManagerApp.COLUMNVIEW)) {
            if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT) {
                if ((((FileManagerApp) mContext.getApplicationContext()).getViewMode() == FileManagerApp.GRIDVIEW) ||
                        (info == null) || (info.length() == 0)) {
                    mInfoView.setVisibility(View.GONE);
                } else {
                    mInfoView.setVisibility(View.VISIBLE);
                }
                mSize.setVisibility(View.VISIBLE);
                if (isDirectory) {
                    mInfoView.setText(info);
                    if (!(((FileManagerApp) mContext.getApplicationContext()).getViewMode() == FileManagerApp.GRIDVIEW)) {
                        mSize.setText(R.string.blank_size);
                    }
                } else {

                    if (!(((FileManagerApp) mContext.getApplicationContext()).getViewMode() == FileManagerApp.GRIDVIEW)) {
                        mSize.setText(info);
                    }
                    mInfoView.setVisibility(View.GONE);
                }
            } else {
                if (!(((FileManagerApp) mContext.getApplicationContext()).getViewMode() == FileManagerApp.GRIDVIEW)) {
                    mSize.setVisibility(View.GONE);
                    mInfoView.setText(info);
                    mInfoView.setVisibility(View.VISIBLE);
                } else {
                    mInfoView.setVisibility(View.GONE);
                }
            }
        } else {
            mInfoView.setText(info);
            mInfoView.setVisibility(View.VISIBLE);
        }
    }

    public void setType(String info, boolean isDirectory) {
        if ((mTextViewType == TEXTVIEW_TYPE.NON_TITLE) &&
                !(((FileManagerApp) mContext.getApplicationContext()).getViewMode() == FileManagerApp.COLUMNVIEW)) {
            if ((((FileManagerApp) mContext.getApplicationContext()).getViewMode() == FileManagerApp.GRIDVIEW) ||
                    (info == null) ||
                    (info.length() == 0) ||
                    ((getResources().getConfiguration().orientation) == Configuration.ORIENTATION_PORTRAIT)) {
                mType.setVisibility(View.GONE);
            } else {
                mType.setVisibility(View.VISIBLE);
            }

            if (!(((FileManagerApp) mContext.getApplicationContext()).getViewMode() == FileManagerApp.GRIDVIEW)) {
                mType.setText(info);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setConfig(newConfig.orientation);

    }

    public void setIcon(Drawable bullet) {
        mIcon.setImageDrawable(bullet);
    }

    public void setSize(String size) {
        mSize.setText(size);
    }

    public void setTime(String time, int day) {
        if (mInfoTime != null) {
            mInfoTime.setText(time);
        }
    }

    public void setTimeInfoTextColor(int color) {
        if (mInfoTime != null) {
            mInfoTime.setTextColor(color);
        }
    }

    public void setInfoTextColor(int color) {
        if (mInfoView != null) {
            mInfoView.setTextColor(color);
        }
    }
}
