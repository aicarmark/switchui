/* Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR
 */
package com.motorola.filemanager.local;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;
import com.motorola.filemanager.ui.IconifiedText;
import com.motorola.filemanager.utils.FileUtils;

public class LocalDetailedInfoFileManagerFragment extends LocalFileOperationsFragment {

    public LocalDetailedInfoFileManagerFragment() {
        super();
    }

    public LocalDetailedInfoFileManagerFragment(int fragmentLayoutID) {
        super(fragmentLayoutID);
        // TODO Auto-generated constructor stub
    }

    final static String TAG = "DetailInfoFragment";
    private IconifiedText mItem;
    private ImageView mThumbnailView;
    private TextView mPathView;
    private TextView mNameView;
    private TextView mSizeView;
    private TextView mDateView;
    private TextView mLocationView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        super.onCreateView(inflater, container, savedInstanceState);
        mThumbnailView = (ImageView) mContentView.findViewById(R.id.detail_thumbnail);
        mPathView = (TextView) mContentView.findViewById(R.id.detail_path);
        mNameView = (TextView) mContentView.findViewById(R.id.detail_name);
        mSizeView = (TextView) mContentView.findViewById(R.id.detail_size);
        mDateView = (TextView) mContentView.findViewById(R.id.detail_date);
        mLocationView = (TextView) mContentView.findViewById(R.id.detail_location);
        return mContentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mContext = getActivity();
        if (mContext != null) {
            // First invalidateOptionsMenu of the home page as we will use a different options
            ((Activity) mContext).invalidateOptionsMenu();
        }
        showContent();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //setButtonWeights(newConfig.orientation);
    }

    public void showContent() {
        if (mItem != null) {
            if (mThumbnailView != null) {
                mThumbnailView.setImageDrawable(mItem.getIcon());
            }
            String path = mItem.getPathInfo();
            final File file = FileUtils.getFile(path);
            if (file != null) {
                if (mPathView != null) {
                    if (path != null) {
                        createBreadCrumbPath(path);
                        mPathView.setText(mDisplayText);
                        FileManagerApp.log(TAG + "showContent:filePath= " + path, false);
                    }
                }

                if (mNameView != null) {
                    String name = file.getName();
                    if (name != null) {
                        mNameView.setText(name);
                    }
                }
                String info = "";
                if (mSizeView != null) {
                    long size = 0;
                    if (file.isDirectory()) {
                        size = FileUtils.getDirSize(file);

                    } else {
                        size = FileUtils.getFileSize(file);
                    }
                    info = mContext.getResources().getString(R.string.size_kb, Long.toString(size));
                    mSizeView.setText(info);
                }

                if (mDateView != null) {
                    String dateStr = "";
                    SimpleDateFormat formatter =
                            (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM);
                    Date date = new Date();
                    long time = file.lastModified();
                    date.setTime(time);
                    dateStr = formatter.format(date);
                    if (dateStr != null) {
                        mDateView.setText(dateStr);
                    }
                }

                if (mLocationView != null) {
                    String location = FileUtils.getMetadataAttr(file, mContext);
                    if (location != null) {
                        mLocationView.setText(location);
                    }
                }

                if (mThumbnailView != null) {
                    mThumbnailView.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (!file.isDirectory()) {
                                openFile(file);
                            } else {
                                FileManagerApp.setCurrDir(file);
                                getFragmentManager().popBackStack();
                            }
                        }
                    });
                }
            }
        }
    }

    public void setItem(IconifiedText item) {
        mItem = item;
    }

    public String getItemPath() {
        if (mItem == null) {
            return null;
        } else {
            return mItem.getPathInfo();
        }
    }

    public IconifiedText getItem() {
        return mItem;
    }

    @Override
    public void switchView(int mode, boolean rebrowse) {
        try {
            mFileManagerApp.setViewMode(mode);
        } catch (Exception e) {
            FileManagerApp.log(TAG + "failed to set view mode", true);
            return;
        }
        if (mode == FileManagerApp.COLUMNVIEW) {
            updateColumnFragment();
        }
    }

    @Override
    public void updateColumnFragment() {

        LocalColumnViewFrameLeftFragment df =
                new LocalColumnViewFrameLeftFragment(R.layout.column_view, mItem.getPathInfo());
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.details, df);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();

        // Show the search icon instead of the widget when the fragment is updated.
        if (mContext != null) {
            ((MainFileManagerActivity) mContext).hideSearchWidget();
        }
    }

}
