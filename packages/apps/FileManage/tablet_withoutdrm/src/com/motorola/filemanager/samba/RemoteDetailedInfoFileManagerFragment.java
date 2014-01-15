/* Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR
 */
package com.motorola.filemanager.samba;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.R;
import com.motorola.filemanager.ui.IconifiedText;

public class RemoteDetailedInfoFileManagerFragment extends RemoteContentBaseFragment {

    public RemoteDetailedInfoFileManagerFragment(int fragmentLayoutID) {
        super();
        // TODO Auto-generated constructor stub
    }

    final static String TAG = "DetailInfoFragment";
    protected Context mContext;
    private View mContentView;
    private IconifiedText mItem;
    private ImageView mThumbnailView;
    private TextView mPathView;
    private TextView mNameView;
    private TextView mSizeView;
    private TextView mDateView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        mContentView = inflater.inflate(R.layout.filemanager_details_view, container, false);
        mThumbnailView = (ImageView) mContentView.findViewById(R.id.detail_thumbnail);
        mPathView = (TextView) mContentView.findViewById(R.id.detail_path);
        mNameView = (TextView) mContentView.findViewById(R.id.detail_name);
        mSizeView = (TextView) mContentView.findViewById(R.id.detail_size);
        mDateView = (TextView) mContentView.findViewById(R.id.detail_date);

        mSmbExplorer = new SambaExplorer(mContentView, mRemoteHandler, this);

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
        DetailedInfoLoader detailedInfoLoader = new DetailedInfoLoader();
        detailedInfoLoader.start();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //setButtonWeights(newConfig.orientation);
    }

    public void showContent() {
        String path = null;
        if (mItem != null) {
            path = mItem.getPathInfo();
            SmbFile file = mSmbExplorer.getSmbFile(path);
            if (file != null) {
                if (path != null) {
                    FileManagerApp.log(TAG + "showContent:filePath= " + path, false);
                }
            }
            String name = null;
            String sizeStr = null;
            String dateStr = null;

            if (mNameView != null) {
                name = file.getName();
                //if (name != null){
                // mNameView.setText(name);
                //}
            }
            if (mSizeView != null) {
                // Calculate the file size. If reminder is more than 0 we should show
                // increase the size by 1, otherwise we should show the size as it is.
                long size = 0;
                try {
                    size = file.length() / 1024;
                } catch (SmbException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                long rem = 0;
                try {
                    rem = file.length() % 1024;
                } catch (SmbException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (rem > 0) {
                    size++;
                }
                sizeStr = mContext.getResources().getString(R.string.size_kb, Long.toString(size));
                //mSizeView.setText(sizeStr);
            }

            if (mDateView != null) {
                dateStr = "";
                SimpleDateFormat formatter =
                        (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM);
                Date date = new Date();
                long time = 0;
                try {
                    time = file.lastModified();
                } catch (SmbException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                date.setTime(time);
                dateStr = formatter.format(date);

                // if (dateStr != null){
                //mDateView.setText(dateStr);
                // }
            }
            if (mContext != null) {
                detailViewSetText(path, name, sizeStr, dateStr);
            }

            if (mThumbnailView != null) {
                mThumbnailView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (!mItem.isDirectory()) {
                            createTmpFilePath(TMPPATH);
                            mSmbExplorer.openDeteailFile(mItem.getPathInfo());
                        } else {
                            mSmbExplorer.setCurPath(mItem.getPathInfo() + "/");
                            getFragmentManager().popBackStack();
                        }
                    }
                });
            }
        }
    }

    private void detailViewSetText(final String path, final String strName, final String strSize,
                                   final String strDate) {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            //@Override
            public void run() {

                mPathView.setText(path);
                mThumbnailView.setImageDrawable(mItem.getIcon());
                mNameView.setText(strName);
                mSizeView.setText(strSize);
                mDateView.setText(strDate);
            }
        });
    }

    public class DetailedInfoLoader extends Thread {

        public DetailedInfoLoader() {

        }

        @Override
        public void run() {

            showContent();
        }

    }

    public void setItem(IconifiedText item) {
        mItem = item;
    }

}
