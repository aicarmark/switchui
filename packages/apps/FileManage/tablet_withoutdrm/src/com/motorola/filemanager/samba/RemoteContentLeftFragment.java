/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 *  Date          CR                Author       Description
 *  2010-03-23    IKSHADOW-2074     E12758       initial
 */
package com.motorola.filemanager.samba;

import java.util.ArrayList;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;
import com.motorola.filemanager.samba.service.DownloadServer;
import com.motorola.filemanager.ui.IconifiedText;
import com.motorola.filemanager.ui.IconifiedTextListAdapter;

public class RemoteContentLeftFragment extends RemoteContentBaseFragment {
    private static final String TAG = "RemoteContentLeftFragment: ";
    public static RemoteDetailedInfoFileManagerFragment mDetailInfoFragment =
            new RemoteDetailedInfoFileManagerFragment(R.layout.filemanager_details_view);

    public static RemoteContentLeftFragment newInstance(int index) {
        RemoteContentLeftFragment f = new RemoteContentLeftFragment();
        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt("index", index);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        mContentView = inflater.inflate(R.layout.remote_filemanager_left, container, false);

        mRemoteHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                RemoteContentLeftFragment.this.handleMessage(msg);
            }
        };
        mRemoteDeviceLayout = (LinearLayout) mContentView.findViewById(R.id.SavedSmbDevice);
        mSmbDeviceView = (ListView) mContentView.findViewById(R.id.SavedServerList);
        mSmbDevice = new SambaDeviceList(mSmbDeviceView, this, mRemoteHandler);

        mSmbExplorer = new SambaExplorer(mContentView, mRemoteHandler, this);
        mSmbFileLayout = (LinearLayout) mContentView.findViewById(R.id.SmbExploreLayout);
        mSmbFileView = (ListView) mContentView.findViewById(R.id.SmbFileList);

        mSmbGroupLayout = (LinearLayout) mContentView.findViewById(R.id.SmbGroupLayout);
        mSmbGroupView = (ListView) mContentView.findViewById(R.id.SmbGroupList);
        mSmbGroup = new SambaGroupList(mSmbGroupView, this, mRemoteHandler);

        showServerList();
        mProgressDiagIndicator = getProgressDialogIndicator(R.array.progress_dialog_indicater);
        return mContentView;
    }

    AdapterView.OnItemClickListener fileListListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String path = null;
            IconifiedText text = null;
            IconifiedTextListAdapter mFileListAdapter = mSmbExplorer.getListAdpater();
            if (mFileListAdapter != null) {
                text = (IconifiedText) mFileListAdapter.getItem(position);
                path = text.getPathInfo();
                mSmbExplorer.getSmbFile(path);
                mFileListAdapter.clearCurrentHighlightedItem(mSmbExplorer.mCurrHighlightedItem);
                mFileListAdapter.setHighlightedItem(text, false);
                mSmbExplorer.mCurrHighlightedItem = text;
            }
            if (text != null && text.isDirectory() && (!mDetailInfoFragment.isVisible())) {
                updateRightFragment(position);
            } else if (mDetailInfoFragment.isVisible() && text.isDirectory()) {
                if (path != null) {
                    //we need to check if the directory end with "/" as  for the 
                    //root directory right under server name it is not
                    if (!path.endsWith("/")) {
                        path = path + "/";
                    }
                    mSmbExplorer.setCurPath(path);
                    getFragmentManager().popBackStack();
                }
            } else {
                showDetailInfoFragment(text);
            }

        }
    };

    private void updateRightFragment(int position) {
        RemoteContentFragment det =
                (RemoteContentFragment) getFragmentManager().findFragmentById(R.id.details);
        if (det != null) {
            IconifiedTextListAdapter mFileListAdapter = mSmbExplorer.getListAdpater();
            if (mFileListAdapter != null) {
                IconifiedText text = (IconifiedText) mFileListAdapter.getItem(position);

                text.getText();
                String path = text.getPathInfo();
                det.getSmbExplorer().setCurPath(path);
                det.updateSmbExplorer(path);
            }

        } else {
            RemoteContentFragment df = RemoteContentFragment.newInstance(position);
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.details, df);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.commit();
        }

    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_LAUNCH_SAMBA_EXPLORE :
                String hostInfo = msg.getData().getString(SAMABA_HOST_INFO);
                showLoadingDialog(mProgressDiagIndicator[PROGRESS_DIALOG_CONNECTING_SERVER],
                        PROGRESS_DIALOG_CONNECTING_SERVER);
                startSmabaExplore(hostInfo);
                return;
            case MESSAGE_RETURN_TO_DEVICE :
                showServerList();
                return;
            case MESSAGE_SHOW_FILE_LIST_VIEW :
                SambaExplorer.log(TAG + " stepInServer success !", false);
                hideLoadingDialog();
                showSmbFileList();
                if (mSmbExplorer.getFileListView() != null) {
                    mSmbExplorer.getFileListView().setOnItemClickListener(fileListListener);
                }
                return;
            case MESSAGE_HIDE_LOADING_DIALOG :
                hideLoadingDialog();
                return;
            case MESSAGE_SHOW_LOADING_DIALOG :
                int msgMode = msg.getData().getInt(PROGRESS_DIALOG_MODE);
                showLoadingDialog(mProgressDiagIndicator[msgMode], msgMode);
                return;

            case MESSAGE_SEARCH_SAMBA_GROUP :
                startGroupSearch();
                showGroupList();
                return;

            case MESSAGE_COPY_FINISHED :
                hideLoadingDialog();
                mSmbExplorer.browseTo(mSmbExplorer.getCurPath());
                if (msg.arg1 == DownloadServer.TRANSFER_COMPLETE) {
                    Toast.makeText(mContext, mContext.getResources().getQuantityString(
                            R.plurals.copy_success, msg.arg2, msg.arg2), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, R.string.error_copying, Toast.LENGTH_SHORT).show();
                }
                return;

            case MESSAGE_MOVE_FINISHED :
                hideLoadingDialog();
                mSmbExplorer.browseTo(mSmbExplorer.getCurPath());
                if (msg.arg1 == DownloadServer.TRANSFER_COMPLETE) {
                    Toast.makeText(mContext, mContext.getResources().getQuantityString(
                            R.plurals.move_success, msg.arg2, msg.arg2), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, R.string.error_moving, Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    private boolean isRightPanelwillBacktoRoot() {
        Fragment rightFragment = getFragmentManager().findFragmentById(R.id.details);
        if ((rightFragment != null) &&
                rightFragment.getClass().getName().equals(RemoteContentFragment.class.getName())) {
            RemoteContentFragment det = (RemoteContentFragment) rightFragment;
            if (det != null) {
                if (det.getSmbExplorer().getPathDepth() == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRightPanelonServerdisplay() {
        Fragment rightFragment = getFragmentManager().findFragmentById(R.id.details);
        if ((rightFragment != null) &&
                rightFragment.getClass().getName().equals(RemoteContentFragment.class.getName())) {
            RemoteContentFragment det = (RemoteContentFragment) rightFragment;
            if (det != null) {
                if (det.mRemoteDeviceLayout.isShown()) {
                    return true;
                }
            }
        }
        return false;
    }

    // the function to go up one level
    public void upOneLevel() {
        if ((!mSmbFileLayout.isShown()) || (isRightPanelonServerdisplay() == true)) {
            initHomePage(false);
        }
        if (isRightPanelwillBacktoRoot() == true) {
            showServerList();
        }
    }

    public void onNewIntent(Intent intent) {

        ArrayList<String> filename = null;
        String intentAction = intent.getAction();
        if (intentAction == null) {
            return;
        }
        if (intentAction.equals(MainFileManagerActivity.ACTION_RESTART_ACTIVITY)) {
            showServerList();
        } else if (intentAction.equals(MainFileManagerActivity.ACTION_TRANSFERFINISHED)) {
            int transferResult =
                    intent.getIntExtra(MainFileManagerActivity.EXTRA_KEY_TRANSFERRESULT, 0);
            int transferredNum =
                    intent.getIntExtra(MainFileManagerActivity.EXTRA_KEY_TRANSFERREDNUM, 0);
            if (MainFileManagerActivity.EXTRA_VAL_REASONCUT.equals(intent
                    .getStringExtra(MainFileManagerActivity.EXTRA_KEY_PASTEREASON))) {
                mRemoteHandler.obtainMessage(MESSAGE_MOVE_FINISHED, transferResult, transferredNum)
                        .sendToTarget();
            } else {
                mRemoteHandler.obtainMessage(MESSAGE_COPY_FINISHED, transferResult, transferredNum)
                        .sendToTarget();
                mSmbExplorer.browseTo(mSmbExplorer.getCurPath());
            }
        }

        else if (intentAction.equals(MainFileManagerActivity.ACTION_PASTE)) {
            filename =
                    intent.getStringArrayListExtra(MainFileManagerActivity.EXTRA_KEY_SOURCEFILENAME);
            if (filename == null) {
                SambaExplorer.log("onNewIntent action = ACTION_PASTE, filename = null", false);
                return;
            }
            if ((MainFileManagerActivity.EXTRA_VAL_REASONCUT).equals(intent
                    .getStringExtra(MainFileManagerActivity.EXTRA_KEY_PASTEREASON))) {

                showLoadingDialog(getString(R.string.moving), PROGRESS_DIALOG_LOADING_FILE);
            }
            /*if (filename.get(0).startsWith("smb://")) {
                // source is from samba, to be pasted to samba
                Intent iDownload = new Intent(DownloadServer.ACTION_DOWNLOAD);
                iDownload.putStringArrayListExtra(MainFileManagerActivity.EXTRA_KEY_SOURCEFILENAME,
                        filename);
                iDownload.putExtra(MainFileManagerActivity.EXTRA_KEY_DESTFILENAME, mSmbExplorer
                        .getCurPath());
                iDownload.putExtra(MainFileManagerActivity.EXTRA_KEY_PASTEREASON, intent
                        .getStringExtra(MainFileManagerActivity.EXTRA_KEY_PASTEREASON));
                getActivity().getBaseContext().sendBroadcast(iDownload);

            } else { // upload*/
            // Same code commenting for findbugs
            Intent iDownload = new Intent(DownloadServer.ACTION_DOWNLOAD);
            iDownload.putStringArrayListExtra(MainFileManagerActivity.EXTRA_KEY_SOURCEFILENAME,
                    filename);
            iDownload.putExtra(MainFileManagerActivity.EXTRA_KEY_DESTFILENAME,
                    mSmbExplorer.getCurPath());
            iDownload.putExtra(MainFileManagerActivity.EXTRA_KEY_PASTEREASON,
                    intent.getStringExtra(MainFileManagerActivity.EXTRA_KEY_PASTEREASON));
            getActivity().getBaseContext().sendBroadcast(iDownload);
            //}
        }
    }

    public static class DetailedInfo extends Thread {

        public DetailedInfo(IconifiedText item) {
            mDetailInfoFragment.setItem(item);
        }

        @Override
        public void run() {

            mDetailInfoFragment.showContent();
        }
    }

    public void showDetailInfoFragment(IconifiedText item) {
        FileManagerApp.log(TAG + "showDetailInfoFragment()", false);

        mDetailInfoFragment.setItem(item);
        if (!mDetailInfoFragment.isVisible()) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.details, mDetailInfoFragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.addToBackStack(null);
            ft.commit();
        } else {

            DetailedInfo detailedInfoLoader = new DetailedInfo(item);
            detailedInfoLoader.start();
        }
    }

}
