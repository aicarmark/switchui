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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.motorola.filemanager.R;
import com.motorola.filemanager.networkdiscovery.DiscoveryEnabler;
import com.motorola.filemanager.networkdiscovery.DiscoveryObserver;
import com.motorola.filemanager.samba.service.SambaTransferHandler;
import com.motorola.filemanager.ui.IconifiedText;
import com.motorola.filemanager.ui.IconifiedTextListAdapter;

public class SambaGroupList implements OnItemClickListener, DiscoveryObserver {
    private static final String TAG = "SambaGroupList: ";
    private static final int MESSAGE_LIST_SMB_GROUP_SUCCESS = 1;
    private static final int MESSAGE_LIST_SERVER_SUCCESS = 3;

    private String mWaitingHandleItem = SambaExplorer.SMB_FILE_PRIFIX;

    public Dialog loginDialog;
    private RemoteContentBaseFragment mParentFragment;
    private Context mContext = null; // SambaServerList.getSmbContext();

    private Handler mHandler;
    private IconifiedTextListAdapter mGroupListAdapter = null;
    private ArrayList<IconifiedText> mGroupList = new ArrayList<IconifiedText>();
    private ListView mGroupListview = null;

    private List<IconifiedText> mTempListContend = new ArrayList<IconifiedText>();

    private GroupListContext mGroupListContext = new GroupListContext();

    public SambaGroupList(View smbDeviceView, RemoteContentBaseFragment parent,
                          final Handler handler) {
        mHandler = handler;
        mParentFragment = parent;
        mContext = mParentFragment.getActivity();
        if (smbDeviceView instanceof ListView) {
            mGroupListview = (ListView) smbDeviceView;
            mGroupListview.setOnItemClickListener(this);
            setSmbDeviceViewAdapter();
        }

    }

    public List<IconifiedText> getGroupList() {
        return mTempListContend;
    }

    private void setSmbDeviceViewAdapter() {
        mGroupListAdapter = new IconifiedTextListAdapter(mContext, mHandler);
        mGroupListAdapter.setListItems(mGroupList, mGroupListview.hasTextFilter());
        mGroupListview.setAdapter(mGroupListAdapter);
    }

    public void listSmbGroup() {
        showLoadingDialog(RemoteContentBaseFragment.PROGRESS_DIALOG_SCANNING_DOMAIN);
        // mGroupListContext.setState(new InitialState());
        mGroupListContext.setState(new GroupEnabledState());

        SmbFile sFile = getAnonymousSmbFile(SambaExplorer.SMB_FILE_PRIFIX);
        if (sFile != null) {
            getSmbGroupListThread thread =
                    new getSmbGroupListThread(sFile,
                            RemoteContentBaseFragment.PROGRESS_DIALOG_SCANNING_DOMAIN);
            thread.start();
        }
        (mParentFragment).showGroupList();
    }

    public void upOneLevel() {
        mGroupListContext.backWard();
    }

    private Handler mGroupListMsgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_LIST_SMB_GROUP_SUCCESS :
                    SambaExplorer.log("mGroupListMsgHandler MESSAGE_LIST_SMB_GROUP_SUCCESS : ",
                            false);
                    hideLoadingDialog();
                    groupListContendUpdate();
                    break;
                case MESSAGE_LIST_SERVER_SUCCESS :
                    SambaExplorer.log("mGroupListMsgHandler MESSAGE_LIST_SERVER_SUCCESS : ", false);
                    hideLoadingDialog();
                    groupListContendUpdate();
                    break;
            }
        }
    };

    private void addDefaultItemToGroupList() {
        mTempListContend.clear();

        IconifiedText item =
                IconifiedText.buildIconItemWithoutInfo(mContext,
                        mContext.getString(R.string.default_group), 2);
        item.setIsNormalFile(false);
        mTempListContend.add(item);

    }

    private synchronized void emptyGroupList() {
        mGroupList.clear();
        updateAdapter();
    }

    private void addGroupResultToList(SmbFile sFile) {
        String[] l = null;
        emptyGroupList();
        if (sFile != null) {
            try {
                l = sFile.list();
                if (l != null) {
                    SambaExplorer.log(TAG + " f.list count is: " + l.length, false);
                    if (l.length > 0) {
                        // mGroupListContext.setState(new GroupEnabledState());
                    }
                }
                // addDefaultItemToGroupList();

                ArrayList<IconifiedText> listGroup = new ArrayList<IconifiedText>();
                IconifiedText newItem = null;

                for (int i = 0; l != null && i < l.length; i++) {
                    SambaExplorer.log(TAG + "in listSmbGroup " + l[i], false);
                    String name = l[i];
                    if (name.endsWith("/")) {
                        name = name.substring(0, name.length() - 1);
                    }
                    newItem = IconifiedText.buildDirIconItem(mContext, name);
                    newItem.setIsNormalFile(false);
                    listGroup.add(newItem);
                }
                mTempListContend.clear();
                addDefaultItemToGroupList();
                mTempListContend.addAll(listGroup);

                mGroupListMsgHandler.sendMessage(formatTransferMsg(MESSAGE_LIST_SMB_GROUP_SUCCESS));
            } catch (SmbException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                mGroupListContext.setState(new GroupEnabledState());
                addDefaultItemToGroupList();
                mGroupListMsgHandler.sendMessage(formatTransferMsg(MESSAGE_LIST_SMB_GROUP_SUCCESS));
            } catch (Exception e) {

            }
        } else {
            mGroupListContext.setState(new GroupEnabledState());
            addDefaultItemToGroupList();
            mGroupListMsgHandler.sendMessage(formatTransferMsg(MESSAGE_LIST_SMB_GROUP_SUCCESS));
        }
    }

    private void addServerResultToList(SmbFile sFile) {
        SmbFile[] l = null;
        emptyGroupList();
        try {
            l = sFile.listFiles();
            if (l != null) {
                SambaExplorer.log(TAG + " f.list count is: " + l.length, false);
                if (l.length > 0) {
                    // mGroupListContext.setState(new ServerEnabledState());
                }
            }
            ArrayList<IconifiedText> listGroup = new ArrayList<IconifiedText>();
            IconifiedText newItem = null;

            for (int i = 0; l != null && i < l.length; i++) {
                SambaExplorer.log(TAG + "in listSmbGroup " + l[i], false);
                String name = l[i].getName();
                if (name.endsWith("/")) {
                    name = name.substring(0, name.length() - 1);
                }
                newItem = IconifiedText.buildDirIconItem(mContext, name);
                newItem.setIsNormalFile(false);
                listGroup.add(newItem);
            }
            mTempListContend.clear();
            mTempListContend.addAll(listGroup);

            mGroupListMsgHandler.sendMessage(formatTransferMsg(MESSAGE_LIST_SMB_GROUP_SUCCESS));
        } catch (SmbException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            makeToast(R.string.no_server_found);
        }
    }

    private synchronized void groupListContendUpdate() {
        SambaExplorer.log("groupListContendUpdate!", false);
        mGroupList.ensureCapacity(mTempListContend.size());
        mGroupList.clear();
        mGroupList.addAll(mTempListContend);
        updateAdapter();
    }

    private void updateAdapter() {
        SambaExplorer.log(TAG + " updateAdapter !", false);
        try {
            if (mParentFragment != null && !mParentFragment.isRemoving() &&
                    mParentFragment.getActivity() != null) {
                mParentFragment.getActivity().runOnUiThread(SmbFileViewRequestFocusRunnalbe);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Runnable SmbFileViewRequestFocusRunnalbe = new Runnable() {
        @Override
        public void run() {
            mGroupListAdapter.notifyDataSetChanged();
        }
    };

    private class getSmbGroupListThread extends Thread {
        private SmbFile sFile;
        private int mMode;

        public getSmbGroupListThread(SmbFile sFile, int mode) {
            this.sFile = sFile;
            this.mMode = mode;
        }

        @Override
        public void run() {
            Looper.prepare();
            if (mMode == RemoteContentBaseFragment.PROGRESS_DIALOG_SCANNING_DOMAIN) {
                addGroupResultToList(sFile);
            } else if (mMode == RemoteContentBaseFragment.PROGRESS_DIALOG_SCANNING_SERVER) {
                addServerResultToList(sFile);
            }
        }
    }

    private SmbFile getAnonymousSmbFile(String fileName) {
        SmbFile f = null;
        try {
            f = new SmbFile(fileName, NtlmPasswordAuthentication.ANONYMOUS);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return f;
    }

    private void listAllGroups() {
        showLoadingDialog(RemoteContentBaseFragment.PROGRESS_DIALOG_SCANNING_DOMAIN);
        mGroupListContext.setState(new GroupEnabledState());
        SmbFile sFile = getAnonymousSmbFile(SambaExplorer.SMB_FILE_PRIFIX);
        if (sFile != null) {
            getSmbGroupListThread thread =
                    new getSmbGroupListThread(sFile,
                            RemoteContentBaseFragment.PROGRESS_DIALOG_SCANNING_DOMAIN);
            thread.start();
        }
        ((RemoteContentFragment) mParentFragment).showGroupList();
    }

    public void listServerInGroup(String group) {
        SambaExplorer.log(TAG + "listServerInGroup: " + group, false);
        mGroupListContext.setState(new ServerEnabledState());
        showLoadingDialog(RemoteContentBaseFragment.PROGRESS_DIALOG_SCANNING_SERVER);
        SmbFile sFile = getAnonymousSmbFile(group);
        if (sFile != null) {
            getSmbGroupListThread thread =
                    new getSmbGroupListThread(sFile,
                            RemoteContentBaseFragment.PROGRESS_DIALOG_SCANNING_SERVER);
            thread.start();
        }
        mParentFragment.showGroupList();
    }

    @Override
    public void onItemClick(AdapterView l, View v, int position, long id) {
        IconifiedText it = mGroupList.get(position);
        SambaExplorer.log("onListItemClick item is :" + it.getText(), false);

        mWaitingHandleItem = SambaExplorer.SMB_FILE_PRIFIX + it.getText();
        if (it.getFieldTag() == 0) {
            mGroupListContext.goAhead();
        } else {
            enableServerDiscovery();
        }
    }

    public String getCurrentItem() {
        return mWaitingHandleItem;
    }

    private void tryToListFilesInServer(String host) {
        SambaExplorer.log(TAG + "tryToListFilesInServer: " + host, false);
        SambaTransferHandler.setUserAuth(NtlmPasswordAuthentication.ANONYMOUS);
        mHandler.sendMessage(formatTransferMsg(host,
                RemoteContentBaseFragment.MESSAGE_LAUNCH_SAMBA_EXPLORE));
        // mGroupListContext.setState(new InitialState());
    }

    private Message formatTransferMsg(int msgID) {
        Message msg = Message.obtain();
        msg.what = msgID;
        return msg;
    }

    private Message formatShowProgressDialogMsg(int msgId, int diagMode) {
        Message msg = Message.obtain();
        Bundle extras = new Bundle();
        extras.putInt(RemoteContentBaseFragment.PROGRESS_DIALOG_MODE, diagMode);
        msg.what = msgId;// RemoteContentFragment.MESSAGE_LAUNCH_SAMBA_EXPLORE ;
        msg.setData(extras);
        return msg;
    }

    private Message formatTransferMsg(String hostInfo, int msgId) {
        Message msg = Message.obtain();
        if ((hostInfo != null) && (hostInfo.startsWith("smb://"))) {
            Bundle extras = new Bundle();
            extras.putString(RemoteContentBaseFragment.SAMABA_HOST_INFO, hostInfo);
            msg.what = msgId;// RemoteContentFragment.MESSAGE_LAUNCH_SAMBA_EXPLORE ;
            msg.setData(extras);
        }
        msg.what = msgId;
        return msg;
    }

    private void backToDevice() {
        mGroupListContext.setState(new InitialState());
        mHandler.sendMessage(formatTransferMsg(RemoteContentBaseFragment.MESSAGE_RETURN_TO_DEVICE));
    }

    private abstract class AbsState {
        public abstract void handleGoAhead(GroupListContext context);

        public abstract void handleBackWard(GroupListContext context);
    }

    private class InitialState extends AbsState {
        @Override
        public void handleBackWard(GroupListContext context) {
            backToDevice();
        }

        @Override
        public void handleGoAhead(GroupListContext context) {
            listAllGroups();
        }
    }

    public class GroupEnabledState extends AbsState {
        @Override
        public void handleBackWard(GroupListContext context) {
            backToDevice();
        }

        @Override
        public void handleGoAhead(GroupListContext context) {
            listServerInGroup(mWaitingHandleItem);
        }
    }

    private class ServerEnabledState extends AbsState {
        @Override
        public void handleBackWard(GroupListContext context) {
            listAllGroups();
        }

        @Override
        public void handleGoAhead(GroupListContext context) {
            tryToListFilesInServer(mWaitingHandleItem);
        }
    }

    public static class GroupListContext {
        private AbsState mState;

        public void setState(AbsState state) {
            SambaExplorer.log(TAG + "GroupListContext current state: " + state, false);
            this.mState = state;
        }

        public void goAhead() {
            mState.handleGoAhead(this);
        }

        public void backWard() {
            mState.handleBackWard(this);
        }
    }

    private void showLoadingDialog(int diagMode) {
        mHandler.sendMessage(formatShowProgressDialogMsg(
                RemoteContentBaseFragment.MESSAGE_SHOW_LOADING_DIALOG, diagMode));
    }

    private void hideLoadingDialog() {
        mHandler.sendMessage(formatTransferMsg(RemoteContentBaseFragment.MESSAGE_HIDE_LOADING_DIALOG));
    }

    private void makeToast(int resId) {
        if ((mContext != null) && (mParentFragment != null) &&
                (mParentFragment.getActivity() != null) &&
                (!mParentFragment.getActivity().isFinishing())) {
            try {
                Toast.makeText(mContext, resId, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
            }
        }
    }

    DiscoveryEnabler mDiscoveryEnabler = null;

    private void enableServerDiscovery() {
        SambaExplorer.log(TAG + "enableServerDiscovery", false);
        mDiscoveryEnabler = new DiscoveryEnabler(mContext, this);
        mDiscoveryEnabler.startDiscovering();
    }

    public void disableServerDiscovery() {
        SambaExplorer.log(TAG + "disableServerDiscovery", false);
        if (mDiscoveryEnabler != null) {
            mDiscoveryEnabler.stopDiscovering();
        }
    }

    @Override
    public void discoveryStarted() {
        showLoadingDialog(RemoteContentBaseFragment.PROGRESS_DIALOG_SCANNING_SERVER);
        mGroupList.clear();
        updateAdapter();
    }

    @Override
    public void discoveryHostFound(String host) {
        IconifiedText newItem = null;
        newItem = IconifiedText.buildDirIconItem(mContext, host);
        newItem.setIsNormalFile(false);
        mGroupList.add(newItem);
        updateAdapter();
    }

    @Override
    public void discoveryFinished() {
        mHandler.sendMessage(formatTransferMsg(RemoteContentBaseFragment.MESSAGE_HIDE_LOADING_DIALOG));
        mGroupListContext.setState(new ServerEnabledState());

        if (mGroupList.size() == 0) {
            makeToast(R.string.no_server_found);
            // mGroupListContext.setState(new GroupEnabledState());
            return;
        }
    }
}
