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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.filemanager.HomePageLeftFileManagerFragment;
import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;
import com.motorola.filemanager.ui.RemoteCustomDialogFragment;
import com.motorola.filemanager.utils.FileUtils;

public class RemoteContentBaseFragment extends Fragment {
    public static final String TAG = "RemoteContentBaseFragment: ";
    View mContentView;

    protected SambaExplorer mSmbExplorer;
    protected SambaDeviceList mSmbDevice;
    protected SambaGroupList mSmbGroup;

    protected ListView mSmbFileView = null;
    protected ListView mSmbDeviceView = null;
    protected ListView mSmbGroupView = null;
    protected LinearLayout mSmbGroupLayout = null;
    protected LinearLayout mSmbFileLayout = null;
    public LinearLayout mRemoteDeviceLayout = null;
    public boolean whetherInFileList = false;
    protected TextView mCurrentFolder;
    protected File tmplFilePath = null;
    public Handler mRemoteHandler;
    public static final String SAMABA_HOST_INFO = "SAMABA_HOST_INFO";
    public static final String SAMABA_HOST_NAME = "SAMABA_HOST_NAME";
    public static final String PROGRESS_DIALOG_MODE = "PROGRESS_DIALOG_MODE";
    public static final String IS_CONTEXT_MENU = "IS_CONTEXT_MENU";
    public static final String SAMABA_FILE_NAME = "SAMABA_FILE_NAME";
    public static final int PROGRESS_DIALOG_SCANNING_DOMAIN = 0;
    public static final int PROGRESS_DIALOG_SCANNING_SERVER = 1;
    public static final int PROGRESS_DIALOG_CONNECTING_SERVER = 2;
    public static final int PROGRESS_DIALOG_LOADING_FILE = 3;

    public static final int MESSAGE_LAUNCH_SAMBA_EXPLORE = 400;
    public static final int MESSAGE_RETURN_TO_DEVICE = 401;
    public static final int MESSAGE_SHOW_FILE_LIST_VIEW = 402;
    public static final int MESSAGE_SHOW_LOADING_DIALOG = 403;
    public static final int MESSAGE_HIDE_LOADING_DIALOG = 404;
    public static final int MESSAGE_SEARCH_SAMBA_GROUP = 405;
    public static final int MESSAGE_DELAY_REMOVE_TEMPFILE = 406;
    public static final int MESSAGE_COPY_FINISHED = 407;
    public static final int MESSAGE_MOVE_FINISHED = 408;
    public static final int MESSAGE_SHOW_FILE_INFO_VIEW = 409;

    Context mContext;

    private ProgressDialog loadingDialog = null;
    protected TextView mCurrentPath;
    protected boolean mDeleteTmpFile = false;
    public static final String TMPPATH = ".tempPath";
    /*
     * <string-array name="progress_dialog_indicater"> <item>Scanning for
     * domains...</item> <item>Scanning for servers...</item> <item>Connecting
     * for server...</item> <item>loading...</item> </string-array>
     */
    protected CharSequence[] mProgressDiagIndicator = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();
        if (mContext != null) {
            mContext.registerReceiver(mWiFiReceiver, new IntentFilter(
                    WifiManager.NETWORK_STATE_CHANGED_ACTION));
            SambaExplorer.log(" onActivityCreated, register WIFI receiver.", false);
        }
    }

    public void showLoadingDialog(CharSequence msg, int mode) {
        try {
            if ((getActivity() != null) && (getActivity().isFinishing())) {
                return;
            }
            hideLoadingDialog();
            createLoadDialog(msg, mode);
            if (!getActivity().isFinishing() && (loadingDialog != null)) {
                try {
                    loadingDialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        } catch (Exception e) {

        }
    }

    public void hideLoadingDialog() {
        if ((getActivity() != null) && (!getActivity().isFinishing()) && (loadingDialog != null)) {
            try {
                loadingDialog.dismiss();
            } catch (Exception e) {
                return;
            } finally {
                loadingDialog = null;
            }
        }
    }

    public void createLoadDialog(CharSequence msg, int mode) {
        if (mContext == null) {
            return;
        }
        loadingDialog = new ProgressDialog(mContext);
        loadingDialog.setMessage(msg);
        loadingDialog.setIndeterminate(true);
        loadingDialog.setCancelable(true);
        switch (mode) {
            case PROGRESS_DIALOG_SCANNING_SERVER :
                loadingDialog.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        mSmbGroup.disableServerDiscovery();
                    }
                });
                break;
            case PROGRESS_DIALOG_CONNECTING_SERVER :
                loadingDialog.setCancelable(false);
                break;
        }
    }

    public void showServerList() {
        whetherInFileList = false;
        if (mSmbFileLayout != null) {
            mSmbFileLayout.setVisibility(View.GONE);
        }
        if (mSmbGroupLayout != null) {
            mSmbGroupLayout.setVisibility(View.GONE);
        }
        if (mRemoteDeviceLayout != null) {
            mRemoteDeviceLayout.setVisibility(View.VISIBLE);
        }
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    public void showGroupList() {
        whetherInFileList = false;
        if (mSmbFileLayout != null) {
            mSmbFileLayout.setVisibility(View.GONE);
        }
        if (mRemoteDeviceLayout != null) {
            mRemoteDeviceLayout.setVisibility(View.GONE);
        }
        if (mSmbGroupLayout != null) {
            mSmbGroupLayout.setVisibility(View.VISIBLE);
        }

        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    public void showSmbFileList() {
        whetherInFileList = true;
        if (mRemoteDeviceLayout != null) {
            mRemoteDeviceLayout.setVisibility(View.GONE);
        }
        if (mSmbGroupLayout != null) {
            mSmbGroupLayout.setVisibility(View.GONE);
        }
        if (mSmbFileLayout != null) {
            mSmbFileLayout.setVisibility(View.VISIBLE);
        }
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    public void startSmabaExplore(String host) {
        if (mSmbExplorer != null) {
            mSmbExplorer.stepInServer(host);
        }
    }

    public void startGroupSearch() {
        SambaExplorer.log(TAG + "startGroupSearch", false);

        if (mSmbGroup != null) {
            mSmbGroup.listSmbGroup();
        }
    }

    public SambaExplorer getSmbExplorer() {
        return mSmbExplorer;
    }

    public void updateSmbExplorer(String path) {

        if (mSmbExplorer != null) {
             mSmbExplorer.setCurPath(path);
             mSmbExplorer.browseTo(path);

        }
    }

    public void showDialog(int id, boolean isContextMenu) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        DialogFragment newFragment =
                RemoteCustomDialogFragment.newInstance(id, isContextMenu, mContext);
        // Show the dialog.
        if (newFragment != null) {
            try {
                newFragment.show(ft, "dialog");
            } catch (Exception e) {
            }
        }

    }

    public void showDialog(int id, boolean isContextMenu, String fileName) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        DialogFragment newFragment =
                RemoteCustomDialogFragment.newInstance(id, isContextMenu, mContext, fileName);
        // Show the dialog.
        if (newFragment != null) {
            try {
                newFragment.show(ft, "dialog");
            } catch (Exception e) {
            }
        }

    }

    public void sendRemoveMsgwithDelay() {
        mRemoteHandler.sendEmptyMessageAtTime(MESSAGE_DELAY_REMOVE_TEMPFILE, 500);
    }

    @Override
    public void onResume() {
        super.onResume();
        SambaExplorer.log(" onResume...", false);
        if (whetherInFileList) {
            deleteTmpFile();
        }
    }

    public void deleteTmpFile() {
        if (mDeleteTmpFile) {
            SambaExplorer.log("deleteTmpFile... ", false);
            mSmbExplorer.deleteLocalTmpFile();
            mDeleteTmpFile = false;
        }
    }

    protected void handleMessage(Message message) {
        // to be overridden
    }

    final BroadcastReceiver mWiFiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if ((action != null) && (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION))) {
                    if (!((MainFileManagerActivity) mContext).checkConnAvailable()) {
                        Toast.makeText(mContext, R.string.check_wifi, Toast.LENGTH_SHORT).show();
                        return;
                    }

                }
            }
        }
    };

    public String getBroadcastAddress() throws IOException {
        if (mContext == null) {
            return null;
        }
        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (wifi == null) {
            return null;
        }

        DhcpInfo dhcp = wifi.getDhcpInfo();
        if (dhcp == null) {
            return null;
        }

        int ipAddress = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;

        String dotIPAddress = "";
        dotIPAddress =
                Integer.toString((ipAddress & 0x000000ff)) + "." +
                        Integer.toString((ipAddress & 0x0000ff00) >>> 8) + "." +
                        Integer.toString((ipAddress & 0x00ff0000) >>> 16) + "." +
                        Integer.toString((ipAddress & 0xff000000) >>> 24);

        SambaExplorer.log(TAG + " WiFi BroadCast IP Address" + dotIPAddress, false);

        return dotIPAddress;
    }

    public String getTmpFilePath() {
        try {
            return tmplFilePath.getCanonicalPath();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public void setSambaPreferences(String key, String value) {
        SharedPreferences.Editor editor = getActivity().getPreferences(0).edit();
        if (editor != null) {
            editor.putString(key, value);
            editor.apply();
            if (mSmbDevice != null) {
                mSmbDevice.updateServerList();
            }
        }
    }

    public String getPrefValueWithKey(String key) {

        String retVal = null;
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getPreferences(0);
            if ((prefs != null) && (prefs.getAll() != null)) {
                return (String) prefs.getAll().get(key);
            }
        }
        return retVal;
    }

    public String getPrefKeyWithPrefInfo(String prefInfo) {
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getPreferences(0);
            if (prefs != null) {
                Map<String, String> mServerMap = (Map<String, String>) prefs.getAll();
                Iterator<?> it = mServerMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry e = (Map.Entry) it.next();
                    String tmp = e.getKey().toString();
                    if (tmp.contains(prefInfo)) {
                        return tmp;
                    }
                }
            }
        }
        return null;
    }

    public String getPrefValueWithPrefInfo(String prefInfo) {
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getPreferences(0);
            if (prefs != null) {
                Map<String, String> mServerMap = (Map<String, String>) prefs.getAll();
                Iterator<?> it = mServerMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry e = (Map.Entry) it.next();
                    String tmp = e.getValue().toString();
                    if (tmp.contains(prefInfo)) {
                        return tmp;
                    }
                }
            }
        }
        return null;
    }

    public void createTmpFilePath(String filename) {
        String mSdCardPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();

        tmplFilePath = FileUtils.getFile(mSdCardPath, filename);
        SambaExplorer.log(TAG + " in createLocalPath SdCardPath ! " + mSdCardPath, false);
        if (!tmplFilePath.exists()) {
            if (tmplFilePath.mkdirs() == false) {
                SambaExplorer.log(TAG + "mkdirs failed " + tmplFilePath.getName(), true);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity() != null) {
            getActivity().unregisterReceiver(mWiFiReceiver);
            SambaExplorer.log(" onDestroy, unregister WIFI receiver.", false);
        }
    }

    public void initHomePage(boolean isBack) {
        HomePageLeftFileManagerFragment df =
                new HomePageLeftFileManagerFragment(R.layout.homepage_expandlist, isBack);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.home_page, df);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    public CharSequence[] getProgressDialogIndicator(int resId) {
        return getResources().getTextArray(resId);
    }

    public void makeToast(int msgId) {
        if ((mContext != null) && (getActivity() != null) && (!getActivity().isFinishing())) {
            try {
                Toast.makeText(mContext, msgId, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
            }
        }
    }

}
