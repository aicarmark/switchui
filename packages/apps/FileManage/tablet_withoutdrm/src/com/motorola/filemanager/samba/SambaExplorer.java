/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 *  Date        CR                Author          Description
 *  2010-03-23  IKSHADOW-2074     E12758          initial
 *  2011-10-20                    w17952          modify for tablet 2 panel implementation
 */
package com.motorola.filemanager.samba;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;
import com.motorola.filemanager.local.SortingSetUpInterface;
import com.motorola.filemanager.networkdiscovery.network.NetInfo;
import com.motorola.filemanager.samba.service.DownloadServer;
import com.motorola.filemanager.samba.service.SambaTransferHandler;
import com.motorola.filemanager.ui.CustomDialogFragment;
import com.motorola.filemanager.ui.IconifiedText;
import com.motorola.filemanager.ui.IconifiedTextListAdapter;
import com.motorola.filemanager.ui.RemoteCustomDialogFragment;
import com.motorola.filemanager.ui.SambaActionModeHandler;
import com.motorola.filemanager.ui.SambaActionModeHandler.ActionModeListener;
import com.motorola.filemanager.utils.FileUtils;
import com.motorola.filemanager.utils.MimeTypeUtil;

public class SambaExplorer implements OnItemClickListener {
    private static final String TAG = "SambaExplorer";
    public static final String SMB_FILE_PRIFIX = "smb://";

    private EditText mUsername;
    private EditText mPassword;

    private int mSelectedFilePosition = 0;
    private String mWaitingAuthItem = null;
    private HashMap<String, String> mIpMap = new HashMap<String, String>();

    private static final int MESSAGE_SMB_Exception = 1;
    private static final int MESSAGE_ENTER_NEW_FOLDER_SUCCESS = 2;
    private static final int MESSAGE_FILE_DELETED = 3;
    private static final int MESSAGE_DELETE_FAILED = 4;
    private static final int MESSAGE_RENAME_DUP = 5;
    private static final int MESSAGE_RENAME_ERR = 6;
    private static final int MESSAGE_ERROR_ADDNEW = 7;
    private static final int MESSAGE_RENAME_COMPLETE = 8;
    private static final int MESSAGE_DELETE_FINISHED = 9;

    private int mSelectMode = IconifiedTextListAdapter.NONE_SELECT_MODE;

    private static final int RESULT_OK = 1;

    private Context mContext = null;
    protected FileManagerApp mFileManagerApp = null;
    private RemoteContentBaseFragment mParentFragment;
    private IconifiedTextListAdapter mContentListAdapter;
    private ArrayList<IconifiedText> mListContent = new ArrayList<IconifiedText>();
    private String mHostPath = null;
    private ProgressDialog mLoadingDialog = null;
    private AbsListView mSmbFileview;
    private AbsListView mSmbFileListView;
    private AbsListView mSmbFileGridView;
    private View mSmbView;
    private int mStepsBack = 0;
    private Handler mHandler;
    private int mSmbFileType = 0;
    private String mLaunchFileName;

    private static final int INVALID_PATH_LEVEL = -100;
    List<IconifiedText> mListDir = new ArrayList<IconifiedText>();
    List<IconifiedText> mListFile = new ArrayList<IconifiedText>();

    private SambaActionModeHandler mActionModeHandler = null;
    private ActionMode mCurrentActionMode = null;
    private ViewHolder mHolder;
    private PopupMenu mPopupMenu;
    final static public int MESSAGE_SELECT_CONTEXT_MENU = 520;
    public static final int PRINT_MODE = 0;
    public static final int LAUNCH_MODE = 1;
    private Dialog mInfoDialog = null;
    private View mInfoView = null;
    private TextView mEmptyText = null;
    public IconifiedText mCurrHighlightedItem = null;

    /*
     * private static class SmbDirectoryContents { List<IconifiedText> listDir;
     * List<IconifiedText> listFile; }
     */

    public List<IconifiedText> getDirList() {
        return mListDir;
    }

    public List<IconifiedText> getFileList() {
        return mListFile;
    }

    public AbsListView getFileListView() {
        return mSmbFileview;
    }

    public IconifiedTextListAdapter getListAdpater() {
        return mContentListAdapter;
    }

    public SambaExplorer(View smbView, final Handler handler, RemoteContentBaseFragment parent) {
        mParentFragment = parent;
        mHandler = handler;
        mSmbView = smbView;
        mContext = mParentFragment.getActivity();
        mFileManagerApp = (FileManagerApp) mParentFragment.getActivity().getApplication();

        if (parent.getClass().getName()
                .equals(RemoteDetailedInfoFileManagerFragment.class.getName())) {
            return;
        }

        if (parent.getClass().getName().equals(RemoteContentLeftFragment.class.getName())) {
            mSmbFileListView = (AbsListView) smbView.findViewById(R.id.SmbFileList_l);
            mSmbFileGridView = (AbsListView) smbView.findViewById(R.id.SmbFileGrid_l);
        } else {
            mSmbFileListView = (AbsListView) smbView.findViewById(R.id.SmbFileList);
            mSmbFileGridView = (AbsListView) smbView.findViewById(R.id.SmbFileGrid);
        }
        mContentListAdapter = new IconifiedTextListAdapter(mContext, mExploreMsgHandler);
        mContentListAdapter.setListItems(mListContent, mSmbFileListView.hasTextFilter());
        mSmbFileListView.setAdapter(mContentListAdapter);
        mSmbFileListView.setOnItemClickListener(this);
        mSmbFileListView.setOnItemLongClickListener(fileListListener2);
        mSmbFileListView.setTextFilterEnabled(true);
        mSmbFileGridView.setAdapter(mContentListAdapter);
        mSmbFileGridView.setOnItemClickListener(this);
        mSmbFileGridView.setTextFilterEnabled(true);
        mSmbFileGridView.setOnItemLongClickListener(fileListListener2);
        if (parent.getClass().getName().equals(RemoteContentFragment.class.getName())) {
            updateViewMode();
        } else {
            mSmbFileGridView.setVisibility(View.GONE);
            mSmbFileview = mSmbFileListView;
        }
        mEmptyText = (TextView) mSmbView.findViewById(R.id.empty_text);
        mStepsBack = 0;
        loadconfigration();
        setSmbUpSorting(mNameSortIface, R.id.textinfo, R.id.name_view_icon, FileManagerApp.NAMESORT);
        setSmbUpSorting(mDateSortIface, R.id.textinfod, R.id.date_view_icon,
                FileManagerApp.DATESORT);
        setSmbUpSorting(mSizeSortIface, R.id.textinfos, R.id.size_view_icon,
                FileManagerApp.SIZESORT);
        setSmbUpSorting(mTypeSortIface, R.id.textinfot, R.id.type_view_icon,
                FileManagerApp.TYPESORT);
    }

    static class ViewHolder {
        RelativeLayout txtViewCol;
        RelativeLayout dateViewCol;
        RelativeLayout sizeViewCol;
        RelativeLayout typeViewCol;
        ImageView size_view_icon;
        ImageView date_view_icon;
        ImageView name_view_icon;
        ImageView type_view_icon;

        private View baseView;

        public ViewHolder(View baseView) {
            this.baseView = baseView;
        }

        public RelativeLayout getTextCol() {
            txtViewCol = (RelativeLayout) baseView.findViewById(R.id.textinfo);
            return txtViewCol;
        }

        public RelativeLayout getDateCol() {
            dateViewCol = (RelativeLayout) baseView.findViewById(R.id.textinfod);
            return dateViewCol;
        }

        public RelativeLayout getTypeCol() {
            typeViewCol = (RelativeLayout) baseView.findViewById(R.id.textinfot);
            return typeViewCol;
        }

        public RelativeLayout getSizeCol() {
            sizeViewCol = (RelativeLayout) baseView.findViewById(R.id.textinfos);
            return sizeViewCol;
        }

        public ImageView getImageView_size() {
            size_view_icon = (ImageView) baseView.findViewById(R.id.size_view_icon);
            return size_view_icon;
        }

        public ImageView getImageView_date() {
            date_view_icon = (ImageView) baseView.findViewById(R.id.date_view_icon);
            return date_view_icon;
        }

        public ImageView getImageView_type() {
            type_view_icon = (ImageView) baseView.findViewById(R.id.type_view_icon);
            return type_view_icon;
        }

        public ImageView getImageView_name() {
            name_view_icon = (ImageView) baseView.findViewById(R.id.name_view_icon);
            return name_view_icon;
        }

        public TextView getFolder() {
            return (TextView) getTextCol().findViewById(R.id.current_folder);
        }
    }

    private void hideSmbSortIcon() {
        mHolder = new ViewHolder(mSmbView);
        mHolder.getImageView_name().setVisibility(View.GONE);
        mHolder.getImageView_date().setVisibility(View.GONE);
        mHolder.getImageView_type().setVisibility(View.GONE);
        mHolder.getImageView_size().setVisibility(View.GONE);
    }

    private void setSmbUpSorting(final SortingSetUpInterface iface, final int columnId,
                                 final int imageViewId, final int sortMode) {

        RelativeLayout column = (RelativeLayout) mSmbView.findViewById(columnId);
        column.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ImageView icon = null;
                hideSmbSortIcon();
                switch (imageViewId) {
                    case R.id.name_view_icon :
                        icon = mHolder.getImageView_name();
                        break;
                    case R.id.date_view_icon :
                        icon = mHolder.getImageView_date();
                        break;
                    case R.id.size_view_icon :
                        icon = mHolder.getImageView_size();
                        break;
                    case R.id.type_view_icon :
                        icon = mHolder.getImageView_type();
                        break;
                    default :
                        break;
                }
                if (icon != null) {
                    icon.setVisibility(View.VISIBLE);
                    if (!iface.isDescending()) {
                        iface.setDescending(true);
                        icon.setImageResource(android.R.drawable.arrow_down_float);
                    } else {
                        iface.setDescending(false);
                        icon.setImageResource(android.R.drawable.arrow_up_float);
                    }
                }
                setUpSmbViewSorting(sortMode);
            }

        });
    }

    SortingSetUpInterface mNameSortIface = new SortingSetUpInterface() {
        //@Override
        public boolean isDescending() {
            return mFileManagerApp.mIsDescendingName;
        }

        //@Override
        public void setDescending(boolean isDescending) {
            mFileManagerApp.mIsDescendingName = isDescending;
        }
    };

    SortingSetUpInterface mDateSortIface = new SortingSetUpInterface() {
        //@Override
        public boolean isDescending() {
            return mFileManagerApp.mIsDescendingDate;
        }

        //@Override
        public void setDescending(boolean isDescending) {
            mFileManagerApp.mIsDescendingDate = isDescending;
        }
    };

    SortingSetUpInterface mSizeSortIface = new SortingSetUpInterface() {
        //@Override
        public boolean isDescending() {
            return mFileManagerApp.mIsDescendingSize;
        }

        //@Override
        public void setDescending(boolean isDescending) {
            mFileManagerApp.mIsDescendingSize = isDescending;
        }
    };

    SortingSetUpInterface mTypeSortIface = new SortingSetUpInterface() {
        //@Override
        public boolean isDescending() {
            return mFileManagerApp.mIsDescendingType;
        }

        //@Override
        public void setDescending(boolean isDescending) {
            mFileManagerApp.mIsDescendingType = isDescending;
        }
    };

    public void setSmbUpSortArrows() {
        hideSmbSortIcon();
        mHolder = new ViewHolder(mSmbView);
        ImageView icon = null;
        boolean isDescending = false;
        switch (((FileManagerApp) mParentFragment.getActivity().getApplication()).getSortMode()) {
            case FileManagerApp.NAMESORT :
                icon = mHolder.getImageView_name();
                if (mFileManagerApp.mIsDescendingName) {
                    isDescending = true;
                }
                break;
            case FileManagerApp.TYPESORT :
                icon = mHolder.getImageView_type();
                if (mFileManagerApp.mIsDescendingType) {
                    isDescending = true;
                }
                break;
            case FileManagerApp.DATESORT :
                icon = mHolder.getImageView_date();
                if (mFileManagerApp.mIsDescendingDate) {
                    isDescending = true;
                }
                break;
            case FileManagerApp.SIZESORT :
                icon = mHolder.getImageView_size();
                if (mFileManagerApp.mIsDescendingSize) {
                    isDescending = true;
                }
                break;
            default :
                break;
        }
        if (icon != null) {
            icon.setVisibility(View.VISIBLE);

            if (isDescending) {
                icon.setImageResource(android.R.drawable.arrow_down_float);
            } else {
                icon.setImageResource(android.R.drawable.arrow_up_float);
            }
        }
    }

    public void setUpSmbViewSorting(final int sortMode) {
        ((FileManagerApp) mParentFragment.getActivity().getApplication()).setSortMode(sortMode);
        new SmbSortFilesTask().execute();
    }

    private class SmbSortFilesTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... v) {
            switch (mFileManagerApp.getSortMode()) {
                case FileManagerApp.NAMESORT :
                    if (mFileManagerApp.mIsDescendingName == false) {
                        Collections.sort(mListDir, IconifiedText.mNameSort);
                        Collections.sort(mListFile, IconifiedText.mNameSort);
                    } else {
                        Collections.sort(mListDir,
                                Collections.reverseOrder(IconifiedText.mNameSort));
                        Collections.sort(mListFile,
                                Collections.reverseOrder(IconifiedText.mNameSort));
                    }
                    break;

                case FileManagerApp.DATESORT :
                    if (mFileManagerApp.mIsDescendingDate == false) {
                        Collections.sort(mListDir, IconifiedText.mTimeSort);
                        Collections.sort(mListFile, IconifiedText.mTimeSort);
                    } else {
                        Collections.sort(mListDir,
                                Collections.reverseOrder(IconifiedText.mTimeSort));
                        Collections.sort(mListFile,
                                Collections.reverseOrder(IconifiedText.mTimeSort));
                    }
                    break;

                case FileManagerApp.SIZESORT :
                    if (mFileManagerApp.mIsDescendingSize == false) {
                        Collections.sort(mListDir, IconifiedText.mSizeSort);
                        Collections.sort(mListFile, IconifiedText.mSizeSort);
                    } else {
                        Collections.sort(mListDir,
                                Collections.reverseOrder(IconifiedText.mSizeSort));
                        Collections.sort(mListFile,
                                Collections.reverseOrder(IconifiedText.mSizeSort));
                    }
                    break;

                case FileManagerApp.TYPESORT :
                    if (mFileManagerApp.mIsDescendingType == false) {
                        Collections.sort(mListDir, IconifiedText.mTypeSort);
                        Collections.sort(mListFile, IconifiedText.mTypeSort);
                    } else {
                        Collections.sort(mListDir,
                                Collections.reverseOrder(IconifiedText.mTypeSort));
                        Collections.sort(mListFile,
                                Collections.reverseOrder(IconifiedText.mTypeSort));
                    }
                    break;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            smbContendUpdate();
        }
    }

    public void updateViewMode() {
        if (mFileManagerApp.getViewMode() == FileManagerApp.GRIDVIEW) {
            mSmbFileListView.setVisibility(View.GONE);
            mSmbFileview = mSmbFileGridView;
            MainFileManagerActivity.setViewType(1);
        } else {
            mSmbFileGridView.setVisibility(View.GONE);
            mSmbFileview = mSmbFileListView;
            MainFileManagerActivity.setViewType(0);
        }
        mSmbFileview.setVisibility(View.VISIBLE);
    }

    public void switchView(int mode) {
        if (((FileManagerApp) mParentFragment.getActivity().getApplication()).getViewMode() == FileManagerApp.GRIDVIEW) {
            ((FileManagerApp) mParentFragment.getActivity().getApplication()).setViewMode(mode);
            mSmbFileGridView.setVisibility(View.GONE);
            mSmbFileview = mSmbFileListView;
            MainFileManagerActivity.setViewType(0);
            setSmbUpSortArrows();
        } else {
            ((FileManagerApp) mParentFragment.getActivity().getApplication()).setViewMode(mode);
            mSmbFileListView.setVisibility(View.GONE);
            mSmbFileview = mSmbFileGridView;
            MainFileManagerActivity.setViewType(1);
        }
        mSmbFileview.setVisibility(View.VISIBLE);

    }

/*  Function is currently not used. Keeping it commented out in case we may need it later.
    private ArrayList<IconifiedText> getAdapterList() {
        return mListContent;
    }*/

    public void setCurPath(String path) {
        SambaExplorer.log("setCurPath: " + path, false);
        mHostPath = path;
    }

    public String getCurPath() {
        return mHostPath;
    }

    private void setWaitingAuthItem(String item) {
        SambaExplorer.log("setWaitingAuthItem:  " + item, false);
        mWaitingAuthItem = item;
    }

    private String getWaitingAuthItem() {
        return mWaitingAuthItem;
    }

    private void setCurSmbFileType(int type) {
        log("setCurSmbFileType : " + getSmbFileType(type), false);
        mSmbFileType = type;
    }

    public int getCurSmbFileType() {
        return mSmbFileType;
    }

    public int getPathDepth() {
        if (mStepsBack == 0) {
            setCurSmbFileType(0);
        }
        return mStepsBack;
    }

    public void setPathDepth(int depth) {
        log("setPathDepth depth: " + depth, false);
        mStepsBack = depth;
    }

    private void setSmbBroadcastAddress() {
        String mBroadCastAddress = null;
        try {
            mBroadCastAddress = mParentFragment.getBroadcastAddress();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        jcifs.Config.setProperty("jcifs.netbios.baddr", mBroadCastAddress);
    }

    private void loadconfigration() {
        InputStream stream = mParentFragment.getResources().openRawResource(R.raw.jcifs);
        try {
            jcifs.Config.load(stream);
            SambaExplorer.log(TAG + "load config successfully", false);
        } catch (IOException ex) {
            SambaExplorer.log(TAG + "load jcifs" + ex, true);
            ex.printStackTrace();
        }
        NetInfo wifiNetInfo = new NetInfo(mContext);
        String ipAddress = wifiNetInfo.getIp();
        String wlanInterfaceName = wifiNetInfo.getWlanInterface(ipAddress);
        if (wlanInterfaceName == null) {
            return;
        }

        String[] winsIpAddresses = new String[2];
        for (int i = 1; i <= 2; i++) {
            winsIpAddresses[i - 1] = getSystemProperty("dhcp." + wlanInterfaceName + ".wins" + i);
        }

        String winsString = null;
        if (validateIP4Address(winsIpAddresses[0])) {
            if (validateIP4Address(winsIpAddresses[1])) {
                winsString = winsIpAddresses[0] + "," + winsIpAddresses[1];
            } else {
                winsString = winsIpAddresses[0];
            }
        } else if (validateIP4Address(winsIpAddresses[1])) {
            winsString = winsIpAddresses[1];
        }

        if (winsString != null) {
            jcifs.Config.setProperty("jcifs.netbios.wins", winsString);
            jcifs.Config.setProperty("jcifs.resolveOrder", "LMHOSTS,WINS,BCAST,DNS");
        }
    }

    private boolean validateIP4Address(String ipAddress) {
        if (ipAddress == null) {
            return false;
        }
        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            for (String s : parts) {
                int i = Integer.parseInt(s);
                if ((i < 0) || (i > 255)) {
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public String getSystemProperty(String s) {
        String line = null;
        Process ifc = null;
        BufferedReader bis = null;
        InputStreamReader isr = null;
        try {
            ifc = Runtime.getRuntime().exec("getprop " + s);
            isr = new InputStreamReader(ifc.getInputStream());
            bis = new BufferedReader(isr);
            line = bis.readLine();
        } catch (java.io.IOException e) {
            log(TAG + "there is IOException: " + e, true);
        } finally {
            try {
                if (isr != null) {
                    isr.close();
                }
                if (bis != null) {
                    bis.close();
                }
                if (ifc != null) {
                    ifc.destroy();
                }
            } catch (Exception ex) {
                log(TAG + "When the process is destroyed, there is one exception: " + ex, true);
            }
        }
        return line;
    }

    private void listServerInGroup(String Group) {
        browseTo(formatHostPath(Group), 0);
    }

    private void listShareFolderInServer(String serverPath) {
        String Host = getIpText(serverPath);
        setWaitingAuthItem(Host);
        String hostIP = null;
        try {
            hostIP = SambaDeviceList.getHostIP(Host);
        } catch (UnknownHostException e) {
            log("stepInServer UnknownHostException: " + e, true);
            setSmbExceptionType(2); // temp value 2 for SmbException
            mExploreMsgHandler.sendMessage(formatTransferMsg(MESSAGE_SMB_Exception));
            return;
        }
        mIpMap.put(hostIP, Host);
        log("stepInServer mIpMap.size() : " + mIpMap.size() + " " + mIpMap.get(hostIP), false);

        serverPath = SMB_FILE_PRIFIX + hostIP + "/";
        browseTo(formatHostPath(serverPath), 0);
    }

    public boolean stepInGroup(String groupPath) {
        setCurPath(SMB_FILE_PRIFIX);
        setSmbBroadcastAddress();
        listServerInGroup(groupPath);
        return false;
    }
    class listServer extends Thread {
        String mPath = null;

        listServer(String ServerPath) {
            mPath = ServerPath;
        }

        public void run() {
            listShareFolderInServer(mPath);
        }
    }

    public boolean stepInServer(String serverPath) {
        log("stepInServer input host is :" + serverPath, false);
        if (serverPath == null) {
            log("input host is : null", false);
            return false;
        } else {
            setCurPath(SMB_FILE_PRIFIX);
            if (serverPath.equals(SMB_FILE_PRIFIX)) {
                setSmbBroadcastAddress();
                listServerInGroup(serverPath);
            } else {
                listServer list = new listServer(serverPath);
                list.start();
            }
        }
        return true;
    }

    private String formatHostPath(String host) {
        if (!host.startsWith("smb:/")) {
            if (host.startsWith("/")) {
                host = "smb:/" + host + "/";
            } else {
                host = "smb://" + host + "/";
            }
        }
        return host;
    }

    private Handler mExploreMsgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SMB_Exception :
                    log("mExploreMsgHandler MESSAGE_SMB_Exception : ", false);
                    hideLoadingDialog();
                    if ((mParentFragment != null) && (mParentFragment.getActivity() != null)) {
                        mParentFragment.getActivity().runOnUiThread(showAlertDialogRunnalbe);
                    }
                    break;
                case MESSAGE_ENTER_NEW_FOLDER_SUCCESS :
                    log("mExploreMsgHandler MESSAGE_ENTER_NEW_FOLDER_SUCCESS : ", false);
                    hideLoadingDialog();
                    // ForceUpdate();
                    mHandler.sendMessage(formatTransferMsg(RemoteContentBaseFragment.MESSAGE_SHOW_FILE_LIST_VIEW));
                    break;
                case MESSAGE_SELECT_CONTEXT_MENU :
                    showContextMenu(msg.obj, msg.arg1);
                    break;
                case MESSAGE_DELETE_FINISHED :
                    if (msg.arg1 == MESSAGE_FILE_DELETED) {
                        Toast.makeText(mContext, mContext.getResources().getQuantityString(
                                R.plurals.delete_success, msg.arg2, msg.arg2), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mContext, R.string.delete_failed, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case MESSAGE_RENAME_DUP :
                    boolean isContext =
                            msg.getData().getBoolean(RemoteContentBaseFragment.IS_CONTEXT_MENU);
                    String fileName =
                            msg.getData().getString(RemoteContentBaseFragment.SAMABA_FILE_NAME);
                    FragmentTransaction ft1 =
                            mParentFragment.getFragmentManager().beginTransaction();
                    DialogFragment newFragment1 =
                            RemoteCustomDialogFragment.newInstance(
                                    CustomDialogFragment.DIALOG_RENAME_EXIST, isContext, mContext,
                                    fileName);
                    // Show the dialog.
                    newFragment1.show(ft1, "dialog");
                    break;
                case MESSAGE_RENAME_ERR :
                    makeToast(mParentFragment.getResources()
                            .getString(R.string.error_renaming_file));
                    break;
                case MESSAGE_RENAME_COMPLETE :
                    makeToast(mParentFragment.getResources().getString(R.string.file_renamed));
                    browseTo(getCurPath());
                    if (mCurrentActionMode != null) {
                        mCurrentActionMode.finish();
                    }

                    break;
                case MESSAGE_ERROR_ADDNEW :
                    makeToast(R.string.error_creating_new_folder);
                    break;
            }
        }
    };

    private Runnable showAlertDialogRunnalbe = new Runnable() {
        // @Override
        @Override
        public void run() {
            if ((mParentFragment == null) || mParentFragment.getActivity().isFinishing()) {
                // user already left Shared folders screen. Do not show dialog
                return;
            }
            // TODO Auto-generated method stub
            if (getSmbExceptionType() == 1) {
                makeReAuthDialog();
            } else {
                if (getCurSmbFileType() == SmbFile.TYPE_WORKGROUP) {
                    makeAlertDialog(R.string.no_group_found);
                } else {
                    makeAlertDialog(R.string.server_not_reachable);
                }
            }
        }
    };

    private synchronized void listSmbFiles(String filePath) throws SmbException {
        SmbFile f = getSmbFile(filePath);
        if (f == null) {
            log("SmbFile is null in listSmbFiles", false);
            return;
        }

        log("in listSmbFiles ", false);
        SmbFile[] l = null;
        l = f.listFiles();
        if (l != null) {
            log(" f.listFiles count is: " + l.length, false);
        }
        // ForceUpdate();
        ArrayList<IconifiedText> listDir = new ArrayList<IconifiedText>();
        ArrayList<IconifiedText> listFile = new ArrayList<IconifiedText>();
        IconifiedText newItem = null;
        Date date = new Date();
        long time;
        long size;
        String infoTime;
        SimpleDateFormat formatter =
                (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM);
        for (int i = 0; l != null && i < l.length; i++) {
            String name = l[i].getName();
            if (name.endsWith("/")) {
                name = name.substring(0, name.length() - 1);
            }

            time = l[i].lastModified();
            date.setTime(time);
            infoTime = formatter.format(date);
            if (l[i].isDirectory()) {
                newItem = IconifiedText.buildDirIconItem(mContext, name);
                if (f.getType() != SmbFile.TYPE_SERVER) {
                    newItem.setTime(time);
                    newItem.setTimeInfo(infoTime);
                }
                newItem.setPathInfo(l[i].getCanonicalPath());
                String serverName = getServerName(getCurPath());
                String authInfo =
                        ((RemoteContentBaseFragment) mParentFragment)
                                .getPrefValueWithPrefInfo(serverName);
                newItem.setAuthInfo(authInfo);
                size = Long.MAX_VALUE;
                try {
                    size = l[i].listFiles().length;
                } catch (Exception e) {
                }
                newItem.setSize(size);
                if (size == Long.MAX_VALUE) {
                    newItem.setInfo(mParentFragment.getResources().getString(R.string.unknown));
                } else {
                    newItem.setInfo(mParentFragment.getResources().getQuantityString(
                            R.plurals.item, (int) size, (int) size));
                }
                newItem.setIsDirectory(true);
                listDir.add(newItem);
            } else {
                newItem = IconifiedText.buildIconItem(mContext, name, FileManagerApp.INDEX_SAMBA);
                newItem.setTime(time);
                newItem.setTimeInfo(infoTime);
                size = l[i].length() / 1024;
                newItem.setSize(size);
                newItem.setPathInfo(l[i].getCanonicalPath());
                String serverName = getServerName(getCurPath());
                String authInfo =
                        ((RemoteContentBaseFragment) mParentFragment)
                                .getPrefValueWithPrefInfo(serverName);
                newItem.setAuthInfo(authInfo);
                // Calculate the file size. If reminder is more than 0 we should show
                // increase the size by 1, otherwise we should show the size as it is.
                long rem = l[i].length() % 1024;
                if (rem > 0) {
                    size++;
                }
                newItem.setInfo(mParentFragment.getResources().getString(R.string.size_kb,
                        Long.toString(size)));
                newItem.setIsDirectory(false);

                listFile.add(newItem);
            }
        }

        if (mParentFragment.getClass().getName().equals(RemoteContentFragment.class.getName())) {
            new SmbSortFilesTask().execute();
        }
        mListDir.clear();
        mListFile.clear();

        mListDir = listDir;
        mListFile = listFile;
    }

    private synchronized void smbContendUpdate() {
        log("smbContendUpdate!", false);
        mListContent.ensureCapacity(mListDir.size() + mListFile.size());
        mListContent.clear();
        mListContent.addAll(mListDir);
        mListContent.addAll(mListFile);
        if (mListContent.isEmpty()) {
            mEmptyText.setVisibility(View.VISIBLE);
        } else {
            mEmptyText.setVisibility(View.GONE);
        }

        mContentListAdapter.setListItems(mListContent, mSmbFileview.hasTextFilter());
        mContentListAdapter.notifyDataSetChanged();

        if (mParentFragment.getClass().getName().equals(RemoteContentLeftFragment.class.getName())) {
            mCurrHighlightedItem = mContentListAdapter.updateHighlightedItem(getCurPath(), false);
        }
    }

    private class listSmbFilesTask extends AsyncTask<String, Integer, Boolean> {
        private String mFilePath;
        private boolean bStepIn = false;
        private int mPathLevel;

        @Override
        protected Boolean doInBackground(String... Paths) {
            log("in listSmbFilesTask", false);
            try {
                int count = Paths.length;
                if (count > 1) {
                    mFilePath = Paths[0];
                    mPathLevel = Integer.parseInt(Paths[1]);
                    bStepIn = true;
                } else if (count > 0) {
                    mFilePath = Paths[0];
                    bStepIn = false;
                } else {
                    log("listSmbFilesTask doInBackground error input!", true);
                    return false;
                }
                log("listSmbFilesTask doInBackground !" + mFilePath + " " + bStepIn, false);

                listSmbFiles(mFilePath);

                if (bStepIn == true) {
                    if (getCurSmbFileType() == SmbFile.TYPE_WORKGROUP) {
                        setPathDepth(mPathLevel);
                        setCurPath(SMB_FILE_PRIFIX);
                    } else if (getCurSmbFileType() == SmbFile.TYPE_SERVER) {
                        setPathDepth(mPathLevel);
                        setCurPath(mFilePath);
                        savePreferencServer(mFilePath);
                    } else {
                        setPathDepth(mPathLevel);
                        setCurPath(mFilePath);
                    }
                }

            } catch (SmbAuthException e) {
                log("SmbAuthException !" + e + " " + e.getMessage(), true);
                setSmbExceptionType(1); // temp value 1 for SmbAuthException
                mExploreMsgHandler.sendMessage(formatTransferMsg(MESSAGE_SMB_Exception));
                return false;
            } catch (SmbException e) {
                log("SmbException! " + e, true);
                setSmbExceptionType(2); // temp value 2 for SmbException
                mExploreMsgHandler.sendMessage(formatTransferMsg(MESSAGE_SMB_Exception));
                return false;
            } catch (Exception e) {
                log("Exception! " + e, true);                
                return false;
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            if (result) {
                smbContendUpdate();
                if (mParentFragment.getClass().getName()
                        .equals(RemoteContentFragment.class.getName())) {
                    ((RemoteContentFragment) mParentFragment).updateRemoteLeftPanel();
                }
                mExploreMsgHandler.sendMessage(formatTransferMsg(MESSAGE_ENTER_NEW_FOLDER_SUCCESS));
                // ForceUpdate();
            }
        }
    }

    private void makeReAuthDialog() {
        if (!mParentFragment.getActivity().isFinishing()) {
            new AlertDialog.Builder(mParentFragment.getActivity())
                    .setTitle(R.string.server_logon_failure)
                    .setMessage(R.string.server_logon_failure_indication)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    createReAuthDialog();
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    SambaExplorer.log(" makeReAuthDialog Cancel: " +
                                            getSmbFileType(getCurSmbFileType()), false);
                                    if (getCurSmbFileType() == SmbFile.TYPE_SERVER) {
                                        upCurPath();
                                        // mHandler.sendMessage(formatTransferMsg(RemoteFileManager.MESSAGE_RETURN_TO_DEVICE));
                                    }
                                }
                            }).show();
        }
    }

    private void createReAuthDialog() {
        LayoutInflater factory = LayoutInflater.from(mParentFragment.getActivity());
        final View reauthInfoView = factory.inflate(R.layout.reauth_info, null);

        final Dialog serverEditDialog =
                new AlertDialog.Builder(mParentFragment.getActivity())
                        .setTitle(R.string.server_reauth_title).setView(reauthInfoView).create();

        mUsername = (EditText) reauthInfoView.findViewById(R.id.Reauthname);
        mPassword = (EditText) reauthInfoView.findViewById(R.id.ReauthPassword);

        Button mAddServerBtn = (Button) reauthInfoView.findViewById(R.id.ReauthOk);
        mAddServerBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mUsername.getText().toString().isEmpty() ||
                        mPassword.getText().toString().isEmpty()) {
                    // Toast a message
                    Toast.makeText(mContext, R.string.enter_Credentials, Toast.LENGTH_SHORT).show();
                    return;
                }
                addPreferencServer(mUsername.getText().toString(), mPassword.getText().toString());
                serverEditDialog.dismiss();
                SambaTransferHandler.provideLoginCredentials(null, mUsername.getText().toString(),
                        mPassword.getText().toString());

                String hostpath = getCurPath();
                if (hostpath.endsWith("/")) {
                    hostpath = hostpath + getWaitingAuthItem();
                } else {
                    hostpath = hostpath + "/" + getWaitingAuthItem();
                }

                if (getCurSmbFileType() == SmbFile.TYPE_SERVER) {
                    stepInServer(hostpath);
                } else {
                    browseTo(formatHostPath(hostpath), getPathDepth() + 1);
                }
            }
        });

        Button mCancel = (Button) reauthInfoView.findViewById(R.id.ReauthCancel);
        mCancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                SambaExplorer.log(" createReAuthDialog mCancel: " +
                        getSmbFileType(getCurSmbFileType()), false);
                if (getCurSmbFileType() == SmbFile.TYPE_SERVER) {
                    upCurPath();
                }
                serverEditDialog.dismiss();
            }
        });
        serverEditDialog.setOwnerActivity(mParentFragment.getActivity());
        serverEditDialog.show();
    }

    private int mSmbExceptionType = 0;

    private void setSmbExceptionType(int type) {
        // temp value 1 for SmbAuthException
        // temp value 2 for SmbException
        mSmbExceptionType = type;
    }

    private int getSmbExceptionType() {
        return mSmbExceptionType;
    }

    public class Browser extends Thread {
        private String mFilepath = null;
        int mPathLevel = -100;

        public Browser(String filePath) {
            mFilepath = filePath;
        }

        public Browser(String filePath, int pathLevel) {
            mFilepath = filePath;
            mPathLevel = pathLevel;
        }

        public void run() {
            log("browseTo filePath : " + mFilepath, false);

            try {
                mFilepath = prepareFileBrowse(mFilepath);
            } catch (SmbException e1) {
                log("browseTo SmbException : " + mFilepath + e1, true);
            }
            if (mPathLevel != INVALID_PATH_LEVEL) {
                new listSmbFilesTask().execute(mFilepath, String.valueOf(mPathLevel));
            } else {
                new listSmbFilesTask().execute(mFilepath);
            }
        }
    }

    public class Deleter extends Thread {
        private ArrayList<String> mFiles = null;

        public Deleter(ArrayList<String> file) {
            mFiles = file;
        }

        public void run() {
            log("delete filePath : " + mFiles, false);

            try {
                deletSelectSmbFiles(mFiles);
            } catch (Exception e1) {
                log("delete Exception : " + mFiles, true);
            }

        }
    }

    public boolean browseTo(String filePath) {
        log("browseTo filePath : " + filePath, false);
        showLoadingDialog();
        Browser b = new Browser(filePath);
        b.start();
        return true;
    }

    public boolean browseTo(String filePath, int pathLevel) {
        log("browseTo filePath : " + filePath + " pathLevel " + pathLevel, false);
        Browser b = new Browser(filePath, pathLevel);
        b.start();
        return true;
    }

    public void delete() {
        ArrayList<String> dFiles = null;

        if (mCurrentActionMode != null) {
            dFiles = mContentListAdapter.getSelectFiles();
        } else {
            dFiles = new ArrayList<String>();
            dFiles.add(getFilePath(getWaitingAuthItem()));
        }
        if (mCurrentActionMode != null) {
            mCurrentActionMode.finish();
        }
        Deleter deleter = new Deleter(dFiles);
        deleter.start();
    }

    public void rename(String newName, boolean isContext) {
        if ((newName.length() == 0) || (newName.lastIndexOf(".") == 0)) {
            FragmentTransaction ft1 = mParentFragment.getFragmentManager().beginTransaction();
            DialogFragment newFragment1 =
                    RemoteCustomDialogFragment.newInstance(
                            CustomDialogFragment.DIALOG_RENAME_EMPTY, isContext, mContext);
            // Show the dialog.
            newFragment1.show(ft1, "dialog");
        } else {
            renameFileOrFolder(getFilePath(getWaitingAuthItem()), newName, isContext);
        }
    }

    public void handleRename(String newName, boolean isContext) {
        try {
            rename(newName, isContext);
        } catch (Exception e) {
            if (mCurrentActionMode != null) {
                mCurrentActionMode.finish();
            }
        }

    }

    private String prepareFileBrowse(String filePath) throws SmbException {
        SmbFile f = getSmbFile(filePath);
        if (f == null) {
            log("SmbFile is null in prepareFileBrowse", false);
            return filePath;
        }
        int type = f.getType();
        setCurSmbFileType(type);
        log("prepareFileBrowse " + filePath + " type is: " + getSmbFileType(getCurSmbFileType()),
                false);

        if (f.isDirectory()) {
            if (!filePath.endsWith("/")) {
                filePath = filePath + "/";
            }
        }
        return filePath;
    }

    private View inflateDialogView(int resId) {
        return LayoutInflater.from(mParentFragment.getActivity()).inflate(resId, null);
    }

    public ArrayList<String> getSmbSelectFiles() {
        return mContentListAdapter.getSelectFiles();
    }

    private void showLoadingDialog() {
        mHandler.sendMessage(formatShowProgressDialogMsg(
                RemoteContentFragment.MESSAGE_SHOW_LOADING_DIALOG,
                RemoteContentFragment.PROGRESS_DIALOG_LOADING_FILE));

    }

    private void hideLoadingDialog() {
        mHandler.sendMessage(formatTransferMsg(RemoteContentFragment.MESSAGE_HIDE_LOADING_DIALOG));
    }

    private void renameFileOrFolder(String oldFileName, String newFileName, boolean isContext) {
        SmbFile oldFile = getSmbFile(oldFileName);
        SmbFile newFile = null;
        if (oldFile != null) {
            String newPath = oldFile.getParent() + newFileName;
            newFile = getSmbFile(newPath);
        }
        if ((oldFile == null) || (newFile == null)) {
            log("SmbFile is null in renameFileorFolder", true);
            // Need an error message toast?
            if (mCurrentActionMode != null) {
                mCurrentActionMode.finish();
            }
            return;
        }
        Renamer rename = new Renamer(oldFile, newFile, isContext);
        rename.start();
        //Toast.makeText(mContext, R.string.file_renamed, Toast.LENGTH_SHORT).show();
        //browseTo(getCurPath());
        //if (mCurrentActionMode != null) {
        //    mCurrentActionMode.finish();
        //}
    }

/*  Function is currently not used. Keeping it commented out in case we may need it later.
    private void renameCurrentFolder(String oldFileName, String newFileName, boolean isContext) {
        SmbFile oldFile = getSmbFile(oldFileName);
        SmbFile newFile = null;

        if (oldFile == null) {
            log("oldFile is null in renameFileorFolder", true);
            // Need an error message toast?
            return;
        }
        newFile = getSmbFile(oldFile.getParent() + newFileName);
        if (newFile == null) {
            log("newFile is null in renameFileorFolder", true);
            // Need an error message toast?
            return;
        }

        Renamer rename = new Renamer(oldFile, newFile, isContext);
        rename.start();
        Toast.makeText(mContext, R.string.folder_renamed, Toast.LENGTH_SHORT).show();
        browseTo(newFile.getCanonicalPath(), getPathDepth());
    }*/

    private class Renamer extends Thread {
        SmbFile mOldFile, mNewFile;
        boolean mIsContext;

        public Renamer(SmbFile oldFile, SmbFile newFile, boolean isContext) {
            mOldFile = oldFile;
            mNewFile = newFile;
            mIsContext = isContext;
        }

        public void run() {
            try {
                if (mNewFile.exists()) {
                    log("File name duplicated rename failure ! " + mNewFile, false);
                    mExploreMsgHandler.sendMessage(formatTransferMsg(MESSAGE_RENAME_DUP,
                            mIsContext, mNewFile.getName()));
                    return;
                } else {
                    mOldFile.renameTo(mNewFile);
                    mExploreMsgHandler.sendMessage(formatTransferMsg(MESSAGE_RENAME_COMPLETE));
                }
                return;
            } catch (SmbException e) {
                log("rename SmbException ! " + e, true);
                mExploreMsgHandler.sendMessage(formatTransferMsg(MESSAGE_RENAME_ERR));

            } catch (NotFoundException e) {
                log("rename NotFoundException ! " + e, true);
                mExploreMsgHandler.sendMessage(formatTransferMsg(MESSAGE_RENAME_ERR));
            }
            return;
        }

    }

    private String getFilePath(String fileName) {
        String seperator = "/";
        StringBuffer buf = new StringBuffer();
        if (getCurPath().endsWith(seperator)) {
            buf.append(getCurPath() + fileName);
        } else {
            buf.append(getCurPath() + seperator + fileName);
        }

        String mPath = buf.toString();
        return mPath;
    }

    public SmbFile getSmbFile(String fileName) {
        log("In getSmbFile create SmbFile ! " + fileName, false);
        SmbFile f = null;
        try {
            if (SambaTransferHandler.getUserAuth() != null) {
                log("create SmbFile with userAuth: " + SambaTransferHandler.getUserAuth(), false);
                f = new SmbFile(fileName, SambaTransferHandler.getUserAuth());
            } else {
                log("create SmbFile with ANONYMOUS !" + NtlmPasswordAuthentication.ANONYMOUS, false);
                f = new SmbFile(fileName, NtlmPasswordAuthentication.ANONYMOUS);
            }
        } catch (Exception e) {
            log("In getSmbFile Exception" + e, true);
        }
        return f;
    }

    private void makeAlertDialog(int resId) {
        new AlertDialog.Builder(mParentFragment.getActivity()).setMessage(resId)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();
    }

    private void addPreferencServer(String user, String passwd) {
        String serverName = getServerName(getCurPath());
        String tempUrl = formatServerUrl(serverName, user, passwd);
        String tempName =
                ((RemoteContentBaseFragment) mParentFragment).getPrefKeyWithPrefInfo(serverName);
        if (tempName == null) {
            tempName = serverName;
        }
        ((RemoteContentBaseFragment) mParentFragment).setSambaPreferences(tempName, tempUrl);
    }

    public String getServerName(String host) {
        String serverName = null;
        String curpath = host;
        if (curpath.equals(SMB_FILE_PRIFIX)) {
            serverName = getWaitingAuthItem();
        } else {
            serverName = getIpText(curpath);
            if (mIpMap.containsKey(serverName)) {
                serverName = mIpMap.get(serverName);
            }
        }
        return serverName;
    }

    private String formatServerUrl(String server, String username, String password) {
        if (server == null) {
            return null;
        }
        String serverurl = SMB_FILE_PRIFIX + username + "\0" + password + "\0" + server;
        return serverurl;
    }

    private String getIpText(String path) {
        if (path != null && path.startsWith(SMB_FILE_PRIFIX)) {
            int start = path.indexOf(SMB_FILE_PRIFIX);
            String temp = path.substring(start + 6);
            int end = temp.indexOf("/");
            String ipText = temp;

            if (end > 0) {
                ipText = temp.subSequence(start, end).toString();
            }
            return ipText;
        }
        return path;
    }

    public void upOneLevel() {
        mFileManagerApp.clearSortFlags();
        hideSmbSortIcon();
        if (getPathDepth() > 0) {
            setPathDepth(getPathDepth() - 1);
        }
        upCurPath();
        log("In upOneLevel mHostPath : " + getCurPath(), false);
        browseTo(getCurPath());
    }

    private String upCurPath() {
        StringBuffer buf = new StringBuffer();
        String tempStr = getCurPath();
        if (tempStr.equals(SMB_FILE_PRIFIX)) {
            setCurPath(tempStr);
            log("In upCurPath: " + tempStr, false);
            return tempStr;
        } else {
            tempStr = tempStr.substring(0, tempStr.length() - 1);
            buf.append(tempStr.substring(0, tempStr.lastIndexOf('/')));
            buf.append('/');
            tempStr = buf.toString();
            setCurPath(tempStr);
            log("In upCurPath: " + tempStr, false);
            return tempStr;
        }
    }

    public static void log(String msg, boolean flag) {
        FileManagerApp.log(TAG + msg, flag);
    }

    private void makeToast(String msg) {
        if ((mContext != null) && (mParentFragment != null) &&
                (mParentFragment.getActivity() != null) &&
                (!mParentFragment.getActivity().isFinishing())) {
            try {
                Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
            }
        }
    }

    private void makeToast(int msgId) {
        if ((mContext != null) && (mParentFragment != null) &&
                (mParentFragment.getActivity() != null) &&
                (!mParentFragment.getActivity().isFinishing())) {
            try {
                Toast.makeText(mContext, msgId, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
            }
        }
    }

    public void onItemClick(AdapterView l, View v, int position, long id) {
        // IconifiedText it = mListContent.get(position);
        try {
            if (mContentListAdapter == null) {
                return;
            }
            IconifiedText it = (IconifiedText) mContentListAdapter.getItem(position);
            setWaitingAuthItem(it.getText());
            String mFilePath = getFilePath(getWaitingAuthItem());

            if (mCurrentActionMode != null) {
                mContentListAdapter.setItemChecked(position);
                mSelectedFilePosition = position;
                mCurrentActionMode.invalidate();
            } else {
                if (mParentFragment.getClass().getName()
                        .equals(RemoteContentLeftFragment.class.getName())) {
                    mContentListAdapter.clearCurrentHighlightedItem(mCurrHighlightedItem);
                    mContentListAdapter.setHighlightedItem(it, false);
                    mCurrHighlightedItem = it;
                }
                if (it.getMiMeType().equals(IconifiedText.NONEMIMETYPE)) {
                    if (mFileManagerApp.mLaunchMode == FileManagerApp.SELECT_FILE_MODE) {
                        String serverName = getServerName(getCurPath());
                        String authInfo =
                                ((RemoteContentFragment) mParentFragment)
                                        .getPrefValueWithPrefInfo(serverName);
                        ((MainFileManagerActivity) (mParentFragment.getActivity()))
                                .rtnPickerResult(mFilePath, authInfo);
                        return;
                    } else if (mFileManagerApp.mLaunchMode == FileManagerApp.SELECT_FOLDER_MODE) {
                        return;
                    }

                    log("onItemClick no application found to open" + it.getText(), false);
                    makeToast(R.string.application_not_available);
                } else {
                    log("onListItemClick item is: " + mFilePath, false);

                    FileOpener opener = new FileOpener(mFilePath);

                    opener.start();
                }
            }
        } catch (Exception e) {

        }
    }

    void showSelectedFileInfo(IconifiedText item) {
        createInfodialog(item);
        InfoLoader infoLoader = new InfoLoader(mListContent.get(mSelectedFilePosition));
        infoLoader.start();
    }

    private void createInfodialog(IconifiedText item) {
        mInfoView = inflateDialogView(R.layout.file_info);
        mInfoDialog =
                new AlertDialog.Builder(mParentFragment.getActivity()).setView(mInfoView).create();

        mInfoDialog.setTitle(R.string.file_info_title);
        ImageView infoIcon = (ImageView) mInfoView.findViewById(R.id.item_info_icon);
        if (infoIcon != null) {
            infoIcon.setImageDrawable(item.getIcon());
        }
        TextView itemName = (TextView) mInfoView.findViewById(R.id.item_name);
        if (itemName != null) {
            itemName.setText(item.getText());
        }
        TextView itemType = (TextView) mInfoView.findViewById(R.id.item_type);
        if (itemType != null) {
            itemType.setText(mParentFragment.getActivity().getResources().getString(R.string.type) +
                    ": " + item.getTypeDesc(mContext));
        }

        ((Button) mInfoView.findViewById(R.id.item_info_ok))
                .setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (mInfoDialog != null) {
                            mInfoDialog.dismiss();
                            mInfoDialog = null;
                        }
                    }
                });
    }

    public void showFileInfodialog() {
        if (mInfoDialog != null) {
            mInfoDialog.show();
        }
    }

    private void getFileInfo(IconifiedText item) {
        SmbFile file = getSmbFile(getFilePath(item.getText()));
        try {
            if (file.isDirectory()) {

                mInfoDialog.setTitle(R.string.file_info_title_dir);
                ((TextView) mInfoView.findViewById(R.id.item_folder)).setText(mParentFragment
                        .getActivity().getResources().getString(R.string.folder) +
                        file.getParent());
                file = getSmbFile(getFilePath(item.getText() + "/"));
                SmbFile[] infolderFiles = file.listFiles();
                ((TextView) mInfoView.findViewById(R.id.item_size)).setText(mParentFragment
                        .getResources().getQuantityString(R.plurals.smb_folder_contains,
                                infolderFiles.length, infolderFiles.length));
            } else {
                ((TextView) mInfoView.findViewById(R.id.item_folder)).setText(mParentFragment
                        .getResources().getString(R.string.folder) + file.getParent());
                // Calculate the file size. If reminder is more than 0 we should show
                // increase the size by 1, otherwise we should show the size as it is.
                long size = file.length() / 1024;
                long rem = file.length() % 1024;
                if (rem > 0) {
                    size++;
                }
                String sizeStr =
                        mContext.getResources().getString(R.string.size_kb, Long.toString(size));
                ((TextView) mInfoView.findViewById(R.id.item_size)).setText(mParentFragment
                        .getResources().getString(R.string.size) + sizeStr);
            }

            Date date = new Date(file.lastModified());
            DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(mContext);
            DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(mContext);
            String format = dateFormat.format(date) + " " + timeFormat.format(date);
            ((TextView) mInfoView.findViewById(R.id.item_last_modified)).setText(mParentFragment
                    .getResources().getString(R.string.last_modified_at) + format);

            if (file.canRead()) {
                ((TextView) mInfoView.findViewById(R.id.read_auth)).setText(R.string.readable_yes);
            } else {
                ((TextView) mInfoView.findViewById(R.id.read_auth)).setText(R.string.readable_no);
            }

            if (file.canWrite()) {
                ((TextView) mInfoView.findViewById(R.id.write_auth)).setText(R.string.writable_yes);
            } else {
                ((TextView) mInfoView.findViewById(R.id.write_auth)).setText(R.string.writable_no);
            }

            if (file.isHidden()) {
                ((TextView) mInfoView.findViewById(R.id.hidden_status))
                        .setText(R.string.hidden_yes);
            } else {
                ((TextView) mInfoView.findViewById(R.id.hidden_status)).setText(R.string.hidden_no);
            }

        } catch (SmbException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log("SmbException: " + e, true);
        }

        mHandler.sendMessage(formatTransferMsg(RemoteContentBaseFragment.MESSAGE_SHOW_FILE_INFO_VIEW));

    }

    private String getSmbFileType(int type) {
        String typeName = null;
        switch (type) {
            case SmbFile.TYPE_FILESYSTEM :
                typeName = "[TYPE_FILESYSTEM]";
                break;
            case SmbFile.TYPE_WORKGROUP :
                typeName = ("[TYPE_WORKGROUP]");
                break;
            case SmbFile.TYPE_SERVER :
                typeName = ("[TYPE_SERVER]");
                break;
            case SmbFile.TYPE_SHARE :
                typeName = ("[TYPE_SHARE]");
                break;
            case SmbFile.TYPE_NAMED_PIPE :
                typeName = ("[TYPE_NAMEDPIPE]");
                break;
            case SmbFile.TYPE_PRINTER :
                typeName = ("[TYPE_PRINTER]");
                break;
            case SmbFile.TYPE_COMM :
                typeName = ("[TYPE_COMM]");
                break;
        }
        return typeName;
    }

    private String savePreferencServer(String contextFile) {
        contextFile = provideLoginInfoWithPref(contextFile);
        if (SambaTransferHandler.getUserAuth() == NtlmPasswordAuthentication.ANONYMOUS) {
            addPreferencServer("", "");
        }
        return contextFile;
    }

    void openSelectedFile(String fileName) {
        FileOpener openFile = new FileOpener(getFilePath(fileName));
        openFile.start();
    }

    private class InfoLoader extends Thread {
        IconifiedText mItem;

        public InfoLoader(IconifiedText item) {
            mItem = item;
        }

        public void run() {

            getFileInfo(mItem);
        }
    }

    private class FilePrinter extends Thread {
        String contextFile = null;

        public FilePrinter(String file) {
            contextFile = file;
        }

        public void run() {

            if (contextFile == null) {
                return;
            } else if (contextFile.startsWith(SMB_FILE_PRIFIX)) {

                SmbFile file = getSmbFile(contextFile);
                if (file == null) {
                    log("error in printFile: file no longer exist", true);
                    return;
                }
                try {
                    if (file.isDirectory()) {
                        return;
                    } else {
                        if (mFileManagerApp.mLaunchMode == FileManagerApp.SELECT_FILE_MODE) {
                            String serverName = getServerName(getCurPath());
                            String authInfo = mParentFragment.getPrefValueWithPrefInfo(serverName);
                            ((MainFileManagerActivity) (mParentFragment.getActivity()))
                                    .rtnPickerResult(file.getCanonicalPath(), authInfo);
                            return;
                        } else if (mFileManagerApp.mLaunchMode == FileManagerApp.SELECT_FOLDER_MODE) {
                            return;
                        }
                        startLoadingFileDialog();
                        downloadTmpFileThread thread =
                                new downloadTmpFileThread(contextFile, PRINT_MODE);
                        thread.start();
                    }
                } catch (Exception e) {
                    log("In printFile Exception" + e, true);
                }
            }
        }

    }
    private class FileOpener extends Thread {

        String contextFile = null;

        public FileOpener(String file) {
            contextFile = file;
        }

        public void run() {

            if (contextFile == null) {
                return;
            } else if (contextFile.startsWith(SMB_FILE_PRIFIX)) {

                SmbFile file = getSmbFile(contextFile);
                int type = SmbFile.TYPE_SHARE;
                if (file == null) {
                    log("SmbFile is null in openFile", false);
                    setCurSmbFileType(type);
                    setSmbExceptionType(1); // temp value 1 for SmbAuthException
                    mExploreMsgHandler.sendMessage(formatTransferMsg(MESSAGE_SMB_Exception));
                    return;
                }
                try {
                    try {
                        type = file.getType();
                    } catch (Exception e) {
                        log("Exception in getType()", true);
                    }
                    if (type == SmbFile.TYPE_SERVER) {
                        setCurSmbFileType(type);
                        contextFile = provideLoginInfoWithPref(contextFile);
                        if (SambaTransferHandler.getUserAuth() == NtlmPasswordAuthentication.ANONYMOUS) {
                            addPreferencServer("", "");
                        }
                        stepInServer(contextFile);
                        return;
                    }

                    if (file.isDirectory()) {
                        showLoadingDialog();
                        browseTo(contextFile, getPathDepth() + 1);
                    } else {
                        if (mFileManagerApp.mLaunchMode == FileManagerApp.SELECT_FILE_MODE) {
                            String serverName = getServerName(getCurPath());
                            String authInfo = mParentFragment.getPrefValueWithPrefInfo(serverName);
                            ((MainFileManagerActivity) (mParentFragment.getActivity()))
                                    .rtnPickerResult(file.getCanonicalPath(), authInfo);
                            return;
                        } else if (mFileManagerApp.mLaunchMode == FileManagerApp.SELECT_FOLDER_MODE) {
                            return;
                        }

                        startLoadingFileDialog();
                        downloadTmpFileThread thread =
                                new downloadTmpFileThread(contextFile, LAUNCH_MODE);
                        thread.start();
                    }
                } catch (SmbAuthException e) {
                    // setSambaServerName(getIpText(contextFile));
                    setCurSmbFileType(type);
                    log("In openFile SmbAuthException !" + e, true);
                    setSmbExceptionType(1); // temp value 1 for SmbAuthException
                    mExploreMsgHandler.sendMessage(formatTransferMsg(MESSAGE_SMB_Exception));
                } catch (SmbException e) {
                    log("In openFile SmbException" + e, true);
                    makeToast(R.string.not_found_network_name);
                } catch (Exception e) {
                    log("In openFile Exception" + e, true);
                    makeToast(R.string.not_found_network_name);
                }
            }
        }
    }

    private String provideLoginInfoWithPref(String HostName) {
        log("provideLoginWithPrefInfo:  " + HostName, false);

        String Host = getIpText(HostName);

        if (mIpMap.containsKey(Host)) {
            Host = mIpMap.get(Host);
        }

        String tempUrl = mParentFragment.getPrefValueWithPrefInfo(Host);

        if (tempUrl != null) {
            SambaTransferHandler.provideLoginCredentials(null,
                    SambaDeviceList.getHostAccount(tempUrl), SambaDeviceList.getHostPwd(tempUrl));
        } else {
            SambaTransferHandler.setUserAuth(NtlmPasswordAuthentication.ANONYMOUS);
        }

        HostName = SMB_FILE_PRIFIX + Host + "/";
        return HostName;
    }

    private void createLoadingFileDialog() {
        if ((mParentFragment != null) && (mParentFragment.getActivity() != null)) {
            mLoadingDialog = new ProgressDialog(mParentFragment.getActivity());

            if (mLoadingDialog != null) {
                mLoadingDialog.setMessage(mParentFragment.getResources()
                        .getString(R.string.loading));
                mLoadingDialog.setIndeterminate(true);
                mLoadingDialog.setCancelable(true);
            }
        }
    }

    private void startLoadingFileDialog() {
        try {
            createLoadingFileDialog();
            if (mLoadingDialog != null) {

                mLoadingDialog.show();
            }
        } catch (Exception e) {
        }
    }

    private void stopLoadingFileDialog() {
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
        }
    }

    private class downloadTmpFileThread extends Thread implements OnCancelListener {
        private volatile boolean mCanceled = false;
        private String path;
        private int mDownloadMode = LAUNCH_MODE;

        public downloadTmpFileThread(String filePath, int downloadMode) {
            super("downloadTmpFileThread");
            path = filePath;
            mDownloadMode = downloadMode;
        }

        @Override
        public void run() {
            if (mLoadingDialog != null) {
                mLoadingDialog.setOnCancelListener(this);
            }
            DownloadHandleFile(path);

        }

        public void onCancel(DialogInterface dialog) {
            log(" downloadTmpFileThread onCancel !", false);
            mCanceled = true;
        }

        private void printFile(File aFile) {
            log("printFile: !" + aFile.getName(), false);
            if (!aFile.exists()) {
                log("printFile not exist !", true);
                return;
            }
            Intent intent = new Intent("com.motorola.android.intent.action.PRINT");
            intent.setType(MimeTypeUtil.getMimeType(mContext, aFile.getName()));
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(aFile));

            try {
                mParentFragment.startActivityForResult(intent, FileManagerApp.Samba_Explorer_PRINT);
            } catch (ActivityNotFoundException e) {
                log("openFile ActivityNotFoundException: !" + e, true);
                deleteLocalTmpFile();
            }

        }

        private void DownloadHandleFile(String filename) {
            SmbFile file = getSmbFile(filename);
            if (file == null) {
                log("SmbFile null in viewFile", false);
                stopLoadingFileDialog();
                // need error message toast?
                return;
            }

            setLaunchFileName(file.getName());
            File desLocal = getLocalTmpFile();
            if (desLocal == null) {
                log(" downloadTmpFileThread downloadFile failed because it is null!", true);
                stopLoadingFileDialog();
            } else if (downloadFile(file, desLocal) == 1) {
                log(" downloadTmpFileThread downloadFile completed !", false);
                stopLoadingFileDialog();

                if (mDownloadMode == PRINT_MODE) {
                    printFile(desLocal);
                } else {
                    launchFile(desLocal);
                }
            } else {
                log(" downloadTmpFileThread downloadFile failed,delete it !", true);
                stopLoadingFileDialog();
                if (desLocal.delete() == false) {
                    SambaExplorer.log(TAG + "delete failed: " + desLocal.getName(), true);
                }
            }
        }

        private int downloadFile(SmbFile remote, File localfile) {
            if (!localfile.exists()) {
                try {
                    SmbFileInputStream in = new SmbFileInputStream(remote);
                    FileOutputStream out = new FileOutputStream(localfile);

                    byte[] b = new byte[8192];
                    int n  = 0;
                    while ((n = in.read(b)) > 0) {
                        out.write(b, 0, n);
                        if (mCanceled) {
                            in.close();
                            out.close();
                            if (localfile.delete() == false) {
                                FileManagerApp.log(
                                        TAG + "Unable to delete file" + localfile.getName(), true);
                            }
                            return 0;
                        }
                    }

                    in.close();
                    out.close();
                } catch (Exception e) {
                    SambaExplorer.log(TAG + "download failed: " + e, true);
                    e.printStackTrace();
                    if (localfile.delete() == false) {
                        SambaExplorer.log(TAG + "delete failed: " + localfile.getName(), true);
                    }
                    return 0;
                }

                SambaExplorer.log(TAG + "Downloaded: " + remote.getName(), false);
            } else {
                SambaExplorer.log(TAG + "cowardly skipping file: " + remote.getName(), false);
            }

            return 1;
        }
    }

    public String getLaunchFileName() {
        return mLaunchFileName;
    }

    private void setLaunchFileName(String filename) {
        mLaunchFileName = filename;
    }

    private File getLocalTmpFile() {
        String local = mParentFragment.getTmpFilePath();
        if (local != null) {
            log("getLocalTmpFile local path: !" + local, false);
            File desLocal = FileUtils.getFile(local, getLaunchFileName());
            return desLocal;
        } else {
            return null;
        }
    }

    private void launchFile(File aFile) {
        log("openFile: !" + aFile.getName(), false);
        try {
            if (!aFile.exists()) {
                log("openFile not exist !", true);
                return;
            }
            Intent intent = new Intent(android.content.Intent.ACTION_VIEW);

            Uri data = FileUtils.getUri(aFile);
            String type = MimeTypeUtil.getMimeType(mParentFragment.getActivity(), aFile.getName());
            intent.setDataAndType(data, type);

            // Were we in GET_CONTENT mode?
            Intent originalIntent = mParentFragment.getActivity().getIntent();

            if (originalIntent != null) {
                String originalIntentAction = originalIntent.getAction();
                if ((originalIntentAction != null) &&
                        originalIntentAction.equals(Intent.ACTION_GET_CONTENT)) {
                    // In that case, we should probably just return the requested data.
                    mParentFragment.getActivity().setResult(RESULT_OK, intent);
                    return;
                }
            }

            try {
                mParentFragment
                        .startActivityForResult(intent, FileManagerApp.Samba_Explorer_LAUNCH);
            } catch (ActivityNotFoundException e) {
                log("openFile ActivityNotFoundException: !" + e, true);
                deleteLocalTmpFile();
            }
        } catch (Exception e) {

        }
    }

    public void deleteLocalTmpFile() {
        File desLocal = getLocalTmpFile();
        if (desLocal != null) {
            log("deleteLocalTmpFile: !" + desLocal.getName(), false);
            if (desLocal.exists()) {
                if (desLocal.delete() == false) {
                    SambaExplorer.log(TAG + "Delete failed: " + desLocal.getName(), true);
                }
            }
        } else {
            log("deleteLocalTmpFile: LocalTmpFile is null", false);
        }
    }

    public void deleteFolder() {
        SmbFile curFolder = getSmbFile(getCurPath());
        if (curFolder == null) {
            log("curFolder is null in deleteFolder. Nothing to delete, exiting function", false);
            return;
        }

        try {
            curFolder.delete();
        } catch (SmbException e) {
            log("SmbException" + e, true);
            makeToast(mParentFragment.getString(R.string.delete_failed, curFolder.getName()));
            return;
        }
        browseTo(curFolder.getParent(), getPathDepth() - 1);
    }

    public void addNewFolder(String foldername) {
        SmbFile newFolder = getSmbFile(getFilePath(foldername));
        if (newFolder == null) {
            log("SmbFile is null in addNewFolder", true);
            makeToast(R.string.error_creating_new_folder);
            return;
        }
        folderAdder folderadder = new folderAdder(newFolder);
        folderadder.start();
    }

    private class folderAdder extends Thread {
        SmbFile mNewFolder;

        public folderAdder(SmbFile newFolder) {
            mNewFolder = newFolder;
        }

        public void run() {
            try {

                mNewFolder.mkdir();

            } catch (SmbException e) {
                log("SmbException" + e, true);
                mExploreMsgHandler.sendMessage(formatTransferMsg(MESSAGE_ERROR_ADDNEW));
                return;
            }
            browseTo(getCurPath());
        }
    }

    public boolean deletSmbFileNoBrowse(ArrayList<String> deletFiles) {
        boolean bDelOk = true;
        if (deletFiles != null) {
            for (String filename : deletFiles) {
                log("deletSelectSmbFiles: " + filename, false);
                if (tryDelSmbFile(filename)) {
                } else {
                    bDelOk = false;
                    break;
                }
            }
        }
        return bDelOk;
    }

    private boolean tryDelSmbFile(String filename) {
        SmbFile targetFile = getSmbFile(filename);
        if (targetFile == null) {
            log("targetFile is null in tryDelSmbFile. Exiting function", false);
            return false;
        }

        try {
            if (targetFile.isDirectory()) {
                targetFile = getSmbFile(filename + "/");
                if (targetFile == null) {
                    log("2nd targetFile is null in tryDelSmbFile. Exiting function", false);
                    return false;
                }
            }
            targetFile.delete();
            // makeToast(mParentFragment.getString(R.string.delete_success,targetFile.getName()));
            return true;
        } catch (SmbException e) {
            log("SmbException" + e, true);
            // makeToast(mParentFragment.getString(R.string.delete_failed,targetFile.getName()));
            return false;
        }
    }

    private void deletSelectSmbFiles(ArrayList<String> deletFiles) {
        if (deletSmbFileNoBrowse(deletFiles)) {
            mExploreMsgHandler.obtainMessage(MESSAGE_DELETE_FINISHED, MESSAGE_FILE_DELETED, deletFiles.size())
                    .sendToTarget();
            browseTo(getCurPath());
        } else {
            mExploreMsgHandler.obtainMessage(MESSAGE_DELETE_FINISHED, MESSAGE_DELETE_FAILED, deletFiles.size())
                    .sendToTarget();
        }
    }

    protected boolean isSomeSmbFileSelected() {
        return mContentListAdapter.isSomeFileSelected();
    }

    protected boolean isSingleSmbFileSelected() {
        if (mContentListAdapter.isSingleFileSelected() != -1) {
            return true;
        } else {
            return false;
        }
    }

    protected void setSmbExploreMultiSelectMode() {
        if (mParentFragment.getClass().getName().equals(RemoteContentLeftFragment.class.getName())) {
            return;
        }

        mSelectMode = IconifiedTextListAdapter.MULTI_SELECT_MODE;
        mContentListAdapter.setSelectMode(IconifiedTextListAdapter.MULTI_SELECT_MODE);
        mActionModeHandler =
                new SambaActionModeHandler((MainFileManagerActivity) mParentFragment.getActivity(),
                        mSmbFileview);
        if (mActionModeHandler == null) {
            return;
        }

        mActionModeHandler.setActionModeListener(new ActionModeListener() {
            public boolean onActionItemClicked(MenuItem item) {
                return handleActionItemClicked(item);
            }
        });
        mCurrentActionMode = mActionModeHandler.startActionMode();
    }

    public void outSmbExploreMultiSelectMode() {
        mSelectMode = IconifiedTextListAdapter.NONE_SELECT_MODE;
        mContentListAdapter.setSelectMode(IconifiedTextListAdapter.NONE_SELECT_MODE);
        smbExploreUnSelectAll();
        mActionModeHandler = null;
        mCurrentActionMode = null;
    }

    protected void smbExploreSelectAll() {
        mContentListAdapter.selectAll();
        if (mCurrentActionMode != null) {
            mCurrentActionMode.invalidate();
        }
    }

    protected void smbExploreUnSelectAll() {
        mContentListAdapter.unSelectAll();
        if (mCurrentActionMode != null) {
            mCurrentActionMode.invalidate();
        }
    }

    public void handleSmbSelectMultiple() {
        if (mCurrentActionMode != null) {
            mCurrentActionMode.invalidate();
        } else {
            setSmbExploreMultiSelectMode();
        }
    }

    protected int getSmbSelectMode() {
        return mSelectMode;
    }

    protected void copyFile(ArrayList<String> Files, boolean isPathIncluded) {
        if (isPathIncluded == false) {
            ArrayList<String> filePaths = getFilesListPath(Files);
            if (filePaths != null && filePaths.size() > 0) {
                FileManagerApp.setPasteFiles(filePaths);
                FileManagerApp.setPasteMode(FileManagerApp.COPY_MODE);
            }
        } else {
            if (Files != null && Files.size() > 0) {
                FileManagerApp.setPasteFiles(Files);
                FileManagerApp.setPasteMode(FileManagerApp.COPY_MODE);
            }
        }
    }

    protected void moveFile(ArrayList<String> Files, boolean isPathIncluded) {
        if (isPathIncluded == false) {
            ArrayList<String> filePaths = getFilesListPath(Files);

            if (filePaths != null && filePaths.size() > 0) {
                FileManagerApp.setPasteFiles(filePaths);
                FileManagerApp.setPasteMode(FileManagerApp.MOVE_MODE);
            }
        } else {
            if (Files != null && Files.size() > 0) {
                FileManagerApp.setPasteFiles(Files);
                FileManagerApp.setPasteMode(FileManagerApp.MOVE_MODE);
            }
        }
    }

    protected void pasteFile() {

        ArrayList<String> filename = FileManagerApp.getPasteFiles();
        Intent iDownload = new Intent(DownloadServer.ACTION_DOWNLOAD);
        iDownload.putStringArrayListExtra(MainFileManagerActivity.EXTRA_KEY_SOURCEFILENAME,
                filename);
        iDownload.putExtra(MainFileManagerActivity.EXTRA_KEY_DESTFILENAME, getCurPath());
        if (FileManagerApp.getPasteMode() == FileManagerApp.COPY_MODE) {
            iDownload.putExtra(MainFileManagerActivity.EXTRA_KEY_PASTEREASON,
                    MainFileManagerActivity.EXTRA_VAL_REASONCOPY);
            mParentFragment.showLoadingDialog(
                    mParentFragment.getResources().getString(R.string.copying),
                    RemoteContentBaseFragment.PROGRESS_DIALOG_LOADING_FILE);
        } else {
            iDownload.putExtra(MainFileManagerActivity.EXTRA_KEY_PASTEREASON,
                    MainFileManagerActivity.EXTRA_VAL_REASONCUT);
            mParentFragment.showLoadingDialog(
                    mParentFragment.getResources().getString(R.string.moving),
                    RemoteContentBaseFragment.PROGRESS_DIALOG_LOADING_FILE);
        }
        mParentFragment.getActivity().getBaseContext().sendBroadcast(iDownload);
        if (mCurrentActionMode != null) {
            mCurrentActionMode.finish();
        }
    }

    private ArrayList<String> getFilesListPath(ArrayList<String> Files) {
        ArrayList<String> filePaths = new ArrayList<String>();;
        for (String file : Files) {
            filePaths.add(getFilePath(file));
        }
        return filePaths;
    }

    private Message formatTransferMsg(int msgID) {
        Message msg = Message.obtain();
        msg.what = msgID;
        return msg;
    }

    private Message formatTransferMsg(int msgID, boolean isContextMenu, String fileName) {
        Message msg = Message.obtain();
        Bundle extras = new Bundle();
        extras.putBoolean(RemoteContentFragment.IS_CONTEXT_MENU, isContextMenu);
        extras.putString(RemoteContentFragment.SAMABA_FILE_NAME, fileName);
        msg.what = msgID;
        msg.setData(extras);
        return msg;
    }

    private Message formatShowProgressDialogMsg(int msgId, int diagMode) {
        Message msg = Message.obtain();
        Bundle extras = new Bundle();
        extras.putInt(RemoteContentFragment.PROGRESS_DIALOG_MODE, diagMode);
        msg.what = msgId;// RemoteFileManager.MESSAGE_LAUNCH_SAMBA_EXPLORE ;
        msg.setData(extras);
        return msg;
    }

    AdapterView.OnItemLongClickListener fileListListener2 =
            new AdapterView.OnItemLongClickListener() {
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                               long id) {
                    IconifiedTextListAdapter adapter =
                            (IconifiedTextListAdapter) parent.getAdapter();
                    if (mFileManagerApp.mLaunchMode == FileManagerApp.NORMAL_MODE) {
                        if ((mCurrentActionMode == null) &&
                                (FileManagerApp.getPasteMode() == FileManagerApp.NO_PASTE)) {
                            adapter.setItemChecked(position);
                            IconifiedText it = (IconifiedText) adapter.getItem(position);
                            setWaitingAuthItem(it.getText());
                            setSmbExploreMultiSelectMode();
                        }
                        return true;
                    } else {
                        return false;
                    }
                }

            };

    public boolean handleActionItemClicked(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.Copy :
                log("Copy... ", false);
                ArrayList<String> cFiles = mContentListAdapter.getSelectFiles();

                copyFile(cFiles, true);
                if (mCurrentActionMode != null) {
                    mCurrentActionMode.finish();
                }
                if (mContext != null) {
                    ((Activity) mContext).invalidateOptionsMenu();
                }
                break;
            case R.id.Delete :
                log("Delete... ", false);
                mParentFragment.showDialog(CustomDialogFragment.DIALOG_DELETE, false);
                break;
            case R.id.Rename :
                log("Rename... ", false);
                String fileName = mContentListAdapter.getSelectFiles().get(0);
                if (fileName.endsWith("/")) {
                    //If directory
                    fileName = fileName.substring(0, fileName.length() - 1);
                }
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                mParentFragment.showDialog(CustomDialogFragment.DIALOG_RENAME, false, fileName);
                if (mCurrentActionMode != null) {
                    mCurrentActionMode.finish();
                }
                break;

            case R.id.Move :
                ArrayList<String> mFiles = mContentListAdapter.getSelectFiles();
                moveFile(mFiles, true);
                if (mCurrentActionMode != null) {
                    mCurrentActionMode.finish();
                }
                if (mContext != null) {
                    ((Activity) mContext).invalidateOptionsMenu();
                }
                break;
            case R.id.Print :
                FilePrinter filePrinter =
                        new FilePrinter(mContentListAdapter.getSelectFiles().get(0));
                filePrinter.start();
                if (mCurrentActionMode != null) {
                    mCurrentActionMode.finish();
                }
                break;
            case R.id.Selectall :
                smbExploreSelectAll();
                break;
            case R.id.Unselectall :
                smbExploreUnSelectAll();
                break;
            /*            case R.id.Info :
                            int pos = isSingleFileSelected();
                            if (pos != -1) {
                                IconifiedText item = (IconifiedText) (mContentListAdapter).getItem(pos);
                                createInfodialog(item);
                                InfoLoader infoLoader = new InfoLoader(item);
                                infoLoader.start();
                            }
            		if (mCurrentActionMode != null) {
                                mCurrentActionMode.finish();
                            }
                            break;*/

            default :
                if (mCurrentActionMode != null) {
                    mCurrentActionMode.finish();
                }
                break;
        }
        return true;
    }

    protected void showContextMenu(Object view, final int pos) {
        final IconifiedText selectedItem = (IconifiedText) mContentListAdapter.getItem(pos);

        if (selectedItem == null) {
            return;
        } else {
            selectedItem.getText();
            if (mPopupMenu != null) {
                mPopupMenu.dismiss();
            }
            mPopupMenu = new PopupMenu(mParentFragment.getActivity(), (View) view);
            Menu menu = mPopupMenu.getMenu();
            mPopupMenu.getMenuInflater().inflate(R.menu.contextmenu, menu);
            menu.findItem(R.id.ContextMenuCompress).setVisible(false);
            menu.findItem(R.id.ContextMenuAddShortcut).setVisible(false);
            if (selectedItem.isDirectory() ||
                    !(FileUtils.isPrintIntentHandled(mContext,
                            FileUtils.getFile(selectedItem.getPathInfo())))) {
                menu.findItem(R.id.ContextMenuPrint).setVisible(false);
            }
            mPopupMenu.show();
            mPopupMenu
                    .setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {

                            setWaitingAuthItem(selectedItem.getText());
                            switch (item.getItemId()) {
                                case R.id.ContextMenuOpen :
                                    openSelectedFile(selectedItem.getText());
                                    break;
                                case R.id.ContextMenuCopy :
                                    log("Copy... ", false);
                                    ArrayList<String> cFiles = new ArrayList<String>();

                                    cFiles.add(getWaitingAuthItem());
                                    copyFile(cFiles, false);
                                    mContentListAdapter.setItemChecked(pos);
                                    if (mContext != null) {
                                        ((Activity) mContext).invalidateOptionsMenu();
                                    }
                                    break;
                                case R.id.ContextMenuDelete :
                                    FragmentTransaction ft =
                                            mParentFragment.getFragmentManager().beginTransaction();
                                    DialogFragment newFragment =
                                            RemoteCustomDialogFragment.newInstance(
                                                    CustomDialogFragment.DIALOG_DELETE, true,
                                                    mContext);
                                    // Show the dialog.
                                    newFragment.show(ft, "dialog");
                                    break;
                                case R.id.ContextMenuMove :
                                    ArrayList<String> mFiles = new ArrayList<String>();
                                    mFiles.add(getWaitingAuthItem());
                                    moveFile(mFiles, false);
                                    mContentListAdapter.setItemChecked(pos);
                                    if (mContext != null) {
                                        ((Activity) mContext).invalidateOptionsMenu();
                                    }
                                    break;

                                case R.id.ContextMenuRename :
                                    FragmentTransaction ft1 =
                                            mParentFragment.getFragmentManager().beginTransaction();
                                    DialogFragment newFragment1 =
                                            RemoteCustomDialogFragment.newInstance(
                                                    CustomDialogFragment.DIALOG_RENAME, true,
                                                    mContext, selectedItem.getText());
                                    // Show the dialog.
                                    newFragment1.show(ft1, "dialog");
                                    break;
                                case R.id.ContextMenuInfo :
                                    IconifiedText file = null;
                                    file = (IconifiedText) (mContentListAdapter).getItem(pos);
                                    if (file != null) {
                                        RemoteContentLeftFragment.mDetailInfoFragment.setItem(file);
                                        FragmentTransaction ft3 =
                                                mParentFragment.getFragmentManager()
                                                        .beginTransaction();
                                        ft3.replace(R.id.details,
                                                RemoteContentLeftFragment.mDetailInfoFragment);
                                        ft3.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                                        ft3.addToBackStack(null);
                                        ft3.commit();
                                    }
                                    break;
                                case R.id.ContextMenuPrint :
                                    FilePrinter filePrinter =
                                            new FilePrinter(getFilePath(selectedItem.getText()));
                                    filePrinter.start();

                                    break;
                            }

                            return true;
                        }
                    });

        }
    }

    //For Detailed View usesage
    public void openDeteailFile(String file) {
        startLoadingFileDialog();
        downloadTmpFileThread thread = new downloadTmpFileThread(file, SambaExplorer.LAUNCH_MODE);
        thread.start();
    }

    public int isSingleFileSelected() {
        if (mContentListAdapter != null) {
            return (mContentListAdapter).isSingleFileSelected();
        } else {
            return -1;
        }
    }

    public void setConfig(int orientation) {
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (mSmbFileGridView != null) {
                ((GridView) mSmbFileGridView).setColumnWidth((int) mParentFragment.getResources()
                        .getDimension(R.dimen.grid_view_width_port));
            }
        } else {
            if (mSmbFileGridView != null) {
                ((GridView) mSmbFileGridView).setColumnWidth((int) mParentFragment.getResources()
                        .getDimension(R.dimen.grid_view_width));
            }
        }
    }
}
