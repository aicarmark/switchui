/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR              Author      Description
 * 2011-10-03   IKTABLETMAIN-348    XQH748      initial
 */

package com.motorola.filemanager.local;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore.Files;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Toast;

import com.motorola.filemanager.BaseFileManagerFragment;
import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;
import com.motorola.filemanager.local.threads.DirectoryScanner;
import com.motorola.filemanager.local.threads.ThumbnailLoader;
import com.motorola.filemanager.ui.IconifiedText;
import com.motorola.filemanager.ui.IconifiedTextListAdapter;

public abstract class LocalBaseFileManagerFragment extends BaseFileManagerFragment {
    static public class DirectoryContents {
        public List<IconifiedText> listDir;
        public List<IconifiedText> listFile;
        public File currentPath;
    }
    final static String TAG = "BaseBrowseFragment";
    protected AbsListView mFileListView;
    static final public int MESSAGE_SHOW_DIRECTORY_CONTENTS = 500;
    static final public int MESSAGE_ICON_CHANGED = 502;
    protected File mContextFile = null;
    protected Object mDirectorySearchLock = new Object();
    private Object mDirectoryScannerLock = new Object();
    private Object mThumbnailLock = new Object();
    protected boolean mScanMediaFiles = false;
    protected IconifiedText mCurrHighlightedItem = null;

    protected IconifiedTextListAdapter mFileListAdapter = null;
    private ArrayList<IconifiedText> mFileListItems = new ArrayList<IconifiedText>();
    protected List<IconifiedText> mListDir = new ArrayList<IconifiedText>();
    protected List<IconifiedText> mListFile = new ArrayList<IconifiedText>();
    protected Files mFiles = new Files();

    protected static int mDisplayWidth = 500;
    protected static int mDisplayHeight = 500;
    protected String mCurrentSearchedString = "";
    protected DirectoryScanner mDirectoryScanner;
    private ArrayList<IconifiedText> mDirectoryEntries = new ArrayList<IconifiedText>();
    private ThumbnailLoader mThumbnailLoader;

    // Set progress bar, arg1 = current value, arg2 = max value
    static final public int MESSAGE_SET_LOADING_PROGRESS = 510;

    public LocalBaseFileManagerFragment() {
        super();
    }

    public LocalBaseFileManagerFragment(int fragmentLayoutID) {
        super(fragmentLayoutID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        mContentView = inflater.inflate(mfragmentLayoutID, container, false);
        mContext = getActivity();
        mMainActivity = (MainFileManagerActivity) mContext;
        mFileManagerApp = (FileManagerApp) mMainActivity.getApplication();
        onCreateViewInit();
        return mContentView;
    }

    /*
     * To be overwirtten by sub-Class if needed.
     */
    @Override
    protected void onCreateViewInit() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();
        initFileListView();
        initFileListAdapter();
    }

    /*
     * To be overwirtten by sub-Class to setup FileListView.
     */
    protected void initFileListView() {
    }

    protected void initFileListAdapter() {
        mFileListAdapter = new IconifiedTextListAdapter(getActivity(), mCurrentHandler);
        mFileListAdapter.setListItems(mFileListItems, false);
        mFileListAdapter.notifyDataSetChanged();
    }

    protected void browseTo(final File aDirectory) {
        if (aDirectory == null) {
            FileManagerApp.log(TAG + "browseTo null directory, finish the activity", true);
            getActivity().finish();
            return;
        }
        refreshList(aDirectory);
    }

    @Override
    public void refreshList(final File aDirectory) {
        stopActivityThread();
        synchronized (mDirectoryScannerLock) {
            // Start a new scanner
            mDirectoryScanner =
                    new DirectoryScanner(aDirectory, this.getActivity(), mCurrentHandler,
                            FileManagerApp.getStorageMediumMode());
            if (mDirectoryScanner != null) {
                mDirectoryScanner.start();
            }
        }

    }

    @Override
    public void refreshLeft(boolean isFullRefresh) {
        // TODO Auto-generated method stub        
        if (!isFullRefresh) {
            //If we need to update only the items set in the directory
            mFileListAdapter.setNoItemsInfo(FileManagerApp.getCurrDir());
        } else {
            //For move case, its better to do full refresh since we need to update both src 
            //and dest folders with updated no. of items and some items needs to be cleared inleft
            updateLeftFragment(FileManagerApp.getCurrDir().getParentFile());
        }

    }

    protected void refreshList() {
        stopActivityThread();
        synchronized (mDirectoryScannerLock) {
            // Start a new scanner
            mDirectoryScanner =
                    new DirectoryScanner(FileManagerApp.getCurrDir(), this.getActivity(),
                            mCurrentHandler, FileManagerApp.getStorageMediumMode());
            if (mDirectoryScanner != null) {
                mDirectoryScanner.start();
            }
        }

    }

    @Override
    public void updateHighlightedItem(String curPath) {
        if (curPath != null) {
            if (mCurrHighlightedItem != null) {
                mFileListAdapter.clearCurrentHighlightedItem(mCurrHighlightedItem);
            }
            mCurrHighlightedItem = mFileListAdapter.updateHighlightedItem(curPath, false);
            if (getActivity() != null) {
                getActivity().invalidateOptionsMenu();
            }
        } else {
            FileManagerApp.log(TAG + "Failed to update highlighted item", true);
        }
    }

    @Override
    public IconifiedText getHighlightedItem() {
        return mCurrHighlightedItem;
    }

    protected void stopActivityThread() {
        FileManagerApp.log(TAG + "stopActivityThread", false);
        if (mDirectoryScanner != null) {
            synchronized (mDirectoryScannerLock) {
                if (mDirectoryScanner.isAlive()) {
                    FileManagerApp.log(TAG + "Dir thread still alive stop", false);
                    mDirectoryScanner.requestStop();
                }
                mDirectoryScanner = null;
            }
        }
        // dismiss the dialog if it is shown
        if (mThumbnailLoader != null) {
            synchronized (mThumbnailLock) {
                if (mThumbnailLoader.isAlive()) {
                    FileManagerApp.log(TAG + "Thumb thread still alive stop", false);
                    mThumbnailLoader.requestStop();
                }
                mThumbnailLoader = null;
            }
        }
        mListDir.clear();
        mListFile.clear();
        emptyList();
    }

    private synchronized void emptyList() {
        mDirectoryEntries.clear();
        updateAdapter(false);
        FileManagerApp.log(TAG + "emptylist", false);
    }

    protected void updateAdapter(boolean delay) {
        FileManagerApp.log(TAG + " updateAdapter !", false);
        try {
            if (mFileListAdapter != null) {
                /* Temporary disabling the delay mechanism
                if (delay) {
                    mCurrentHandler.postDelayed(FileViewRequestFocusRunnalbe, 1000);
                } else {*/
                getActivity().runOnUiThread(FileViewRequestFocusRunnalbe);
                //}
            }
        } catch (Exception e) {

        }
    }
    private Runnable FileViewRequestFocusRunnalbe = new Runnable() {
        public void run() {
            mFileListAdapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void handleMessage(Message message) {
        switch (message.what) {
            case MESSAGE_SHOW_DIRECTORY_CONTENTS :
                hideLoadingDialog();
                if (message.obj != null) {
                    FileManagerApp.log(TAG + "obj not null", false);
                    showDirectoryContents((DirectoryContents) message.obj);
                }
                break;
            case MESSAGE_ICON_CHANGED :
                try {
                    if (mThumbnailLoader != null &&
                            ((int) mThumbnailLoader.getId()) == message.arg2) {
                        mDirectoryEntries.get(message.arg1).setIcon((Drawable) message.obj);
                        updateAdapter(true);
                    } else if (mThumbnailLoader != null) {
                        FileManagerApp.log(TAG + "Thumbnail nochange" + message.arg2 + " " +
                                mThumbnailLoader.getId(), false);
                    }
                } catch (Exception e) {
                    FileManagerApp.log(TAG + "exception" + e.getMessage(), true);
                }
                break;
        }
    }

    /*
     * To be overwirtten by sub-Class if needed.
     */
    protected void onCancelledFileListResult() {
        try {
            getActivity().runOnUiThread(new Runnable() {
                //@Override
                public void run() {
                    hideLoadingDialog();
                }
            });
        } catch (Exception e) {
            FileManagerApp.log("Exception in onCancelledFileListResult", true);
            e.printStackTrace();
        }
    }

    protected void contentUpdate() {
        FileManagerApp.log(TAG + " contentUpdate()", false);
        mFileListItems.ensureCapacity(mListDir.size() + mListFile.size());
        mFileListItems.clear();
        mFileListItems.addAll(mListDir);
        mFileListItems.addAll(mListFile);
        mFileListAdapter.setListItems(mFileListItems, false);
        mFileListAdapter.notifyDataSetChanged();
    }

    private void addItemsToAdapter(ArrayList<IconifiedText> mDirectoryEntries) {
        mFileListAdapter = new IconifiedTextListAdapter(mContext, mCurrentHandler);
        mFileListItems.clear();
        for (int i = 0; i < mDirectoryEntries.size(); i++) {
            mFileListItems.add(mDirectoryEntries.get(i));
        }
        mFileListAdapter.setListItems(mFileListItems, false);
        mFileListAdapter.notifyDataSetChanged();
    }

    protected void showDirectoryContents(DirectoryContents contents) {
        mDirectoryScanner = null;
        mListDir = contents.listDir;
        mListFile = contents.listFile;

        //synchronized (mDirectoryScannerLock) {
        mDirectoryEntries.ensureCapacity(mListDir.size() + mListFile.size());
        mDirectoryEntries.clear();
        for (int i = 0; i < mListDir.size(); i++) {
            mDirectoryEntries.add(mListDir.get(i));
        }
        for (int i = 0; i < mListFile.size(); i++) {
            mDirectoryEntries.add(mListFile.get(i));
        }
        addItemsToAdapter(mDirectoryEntries);

        mThumbnailLoader = new ThumbnailLoader(mDirectoryEntries, mCurrentHandler);
        if (mThumbnailLoader != null) {
            synchronized (mThumbnailLock) {
                mThumbnailLoader.setContext(mContext);
                mThumbnailLoader.start();
            }
        }
    }

    @Override
    public String getFileName(boolean isContext) {
        String fileName = null;
        try {
            if (isContext) {
                fileName = mContextFile.getName();
            } else {
                if (mFileListView.getAdapter() != null) {
                    fileName =
                            ((IconifiedTextListAdapter) mFileListView.getAdapter())
                                    .getSelectFiles().get(0);
                    if (fileName.endsWith("/")) {
                        //If directory
                        fileName = fileName.substring(0, fileName.length() - 1);
                    }
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                }
            }
            FileManagerApp.log(TAG + "file name is: " + fileName, false);
        } catch (Exception e) {
            FileManagerApp.log(TAG + "Exception when getting file name ", true);
        }
        return fileName;
    }

    protected ProgressDialog loadingDialog = null;

    protected void showLoadingDialog(String dialog) {
        try {
            if (getActivity().isFinishing()) {
                return;
            }
            hideLoadingDialog();
            loadingDialog = new ProgressDialog(getActivity());
            if (FileManagerApp.getBrowseMode() == FileManagerApp.PAGE_CONTENT_SEARCH) {
                String text =
                        String.format(mContext.getResources().getString(R.string.search_progess),
                                "\"" + mCurrentSearchedString + "\"", mDisplayText);
                loadingDialog.setMessage(text);
                loadingDialog.setTitle(text);
            } else {
                loadingDialog.setMessage(dialog);
            }
            loadingDialog.setIndeterminate(true);
            loadingDialog.setCancelable(true);
            loadingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    browseTo(FileManagerApp.getPrevBrowseDir());
                }
            });

            if (!getActivity().isFinishing() && (loadingDialog != null)) {
                loadingDialog.show();
            }
        } catch (Exception e) {
            FileManagerApp.log("Exception in showLoadingDialog", true);
            e.printStackTrace();
            return;
        }

    }

    protected void hideLoadingDialog() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    protected void showToast(int resId) {
        final int tmpResId = resId;
        if (mContext != null) {
            ((Activity) mContext).runOnUiThread(new Runnable() {
                //@Override
                public void run() {
                    Toast.makeText(mContext, tmpResId, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    protected IconifiedText getIconifiedTextItem(int pos) {
        if (mFileListAdapter != null) {
            return (IconifiedText) mFileListAdapter.getItem(pos);
        } else {
            return null;
        }
    }

    protected IconifiedTextListAdapter getAdapter() {
        return mFileListAdapter;
    }

    @Override
    public boolean upOneLevel() {
        return false;
    }

}
