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

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import jcifs.netbios.NbtAddress;
import jcifs.smb.NtlmPasswordAuthentication;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.motorola.filemanager.R;
import com.motorola.filemanager.samba.service.SambaTransferHandler;
import com.motorola.filemanager.ui.IconifiedText;
import com.motorola.filemanager.ui.IconifiedTextListAdapter;

public class SambaDeviceList implements OnItemClickListener, OnCreateContextMenuListener {
    private static final String TAG = "SambaDeviceList: ";

    private EditText mIP;
    private EditText mUsername;
    private EditText mPassword;
    public Dialog loginDialog;
    private RemoteContentBaseFragment mParentFragment;

    private String mHostIP = null;
    private String mHostAccount = null;
    private String mHostPwd = null;
    private String mDisplayName = null;
    private Context mContext = null; // SambaServerList.getSmbContext();

    private static final int MENU_OPEN = Menu.FIRST + 1;
    private static final int MENU_REMOVE = Menu.FIRST + 2;
    private static final int MENU_EDIT = Menu.FIRST + 3;

    private Handler mHandler;
    private IconifiedTextListAdapter mServerListAdapter = null;
    private ArrayList<IconifiedText> mListDevice = new ArrayList<IconifiedText>();
    private ListView mSavedServerview = null;
    private Map<String, String> mServerMap = null;

    private CharSequence[] mListDevicePreference = null;// {"Add Server"};//,"Search Total Network"};

    public ArrayList<IconifiedText> getDeviceList() {
        return mListDevice;
    }

    public SambaDeviceList(View smbDeviceView, RemoteContentBaseFragment parent,
                           final Handler handler) {
        mParentFragment = parent;
        mContext = mParentFragment.getActivity();
        // mSavedServerview =
        // (ListView)mParentFragment.findViewById(R.id.SavedServerList);
        if (smbDeviceView instanceof ListView) {
            mSavedServerview = (ListView) smbDeviceView;
            mSavedServerview.setOnItemClickListener(this);
            mSavedServerview.setOnCreateContextMenuListener(this);
            setSmbDeviceViewAdapter();
            updateServerList();
        }
        mHandler = handler;
    }

    public SambaDeviceList(View smbDeviceView, RemoteContentBaseFragment parent) {
        mParentFragment = parent;
        mContext = mParentFragment.getActivity().getApplicationContext();
        if (smbDeviceView instanceof ListView) {
            mSavedServerview = (ListView) smbDeviceView;
            mSavedServerview.setOnItemClickListener(this);
            mSavedServerview.setOnCreateContextMenuListener(this);
            SambaTransferHandler.setSambaConfig();
            setSmbDeviceViewAdapter();
            updateServerList();
        }
    }

    private void setSmbDeviceViewAdapter() {
        mServerListAdapter = new IconifiedTextListAdapter(mContext, mHandler);
        mServerListAdapter.setListItems(mListDevice, mSavedServerview.hasTextFilter());
        mSavedServerview.setAdapter(mServerListAdapter);
    }

    private String getPrefValueWithKey(String key) {
        SharedPreferences prefs = mParentFragment.getActivity().getPreferences(0);
        return (String) prefs.getAll().get(key);
    }

    public void updateServerList() {
        SharedPreferences prefs = mParentFragment.getActivity().getPreferences(0);
        mServerMap = (Map<String, String>) prefs.getAll();
        mListDevicePreference =
                mParentFragment.getResources().getTextArray(R.array.add_server_indicater);
        setServerListSize(mServerMap.size() + mListDevicePreference.length);
        // setServerListSize(mServerMap.size());
    }

    private void setServerListSize(int size) {
        mListDevice.clear();
        if (size > 0) {
            // if(size >mListDevicePreference.length){
            mListDevice.ensureCapacity(size);
            for (int i = 0; i < mListDevicePreference.length; i++) {
                IconifiedText item =
                        IconifiedText.buildIconItemWithoutInfo(mContext,
                                (String) mListDevicePreference[i], 1);
                item.setIsNormalFile(false);
                mListDevice.add(item);
            }

            Iterator<?> it = mServerMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                IconifiedText item =
                        IconifiedText.buildIconItemWithoutInfo(mContext, (String) e.getKey(), 0);
                item.setIsNormalFile(false);
                mListDevice.add(item);
            }
        }
        updateAdapter();
    }

    private void parseSmbUrl(String smbUrl) {
        if (smbUrl == null || !smbUrl.startsWith(SambaExplorer.SMB_FILE_PRIFIX)) {
            return;
        }
        formatServerInfo(true);

        mHostIP = getHostName(smbUrl);// smbUrl.substring(smbUrl.lastIndexOf("@")+1,smbUrl.lastIndexOf("/"));
        mHostAccount = getHostAccount(smbUrl); // smbUrl.substring(smbUrl.lastIndexOf("smb://")+6,
        // smbUrl.lastIndexOf(":"));
        mHostPwd = getHostPwd(smbUrl);// smbUrl.substring(smbUrl.lastIndexOf(":")+1,
        // smbUrl.lastIndexOf("@"));
        mDisplayName = mHostIP;

    }

    private String getHostName(String smbUrl) {
        int i = smbUrl.lastIndexOf('\0');
        if (i == -1) {
            return "";
        } else {
            return smbUrl.substring(i + 1);
        }
    }

    public static String getHostAccount(String smbUrl) {
        int i = smbUrl.lastIndexOf(SambaExplorer.SMB_FILE_PRIFIX);
        int j = smbUrl.indexOf('\0');
        if (i == -1 || j == -1) {
            return "";
        } else {
            return smbUrl.substring(i + SambaExplorer.SMB_FILE_PRIFIX.length(), j);
        }
    }

    public static String getHostPwd(String smbUrl) {
        int i = smbUrl.indexOf('\0');
        int j = smbUrl.lastIndexOf('\0');
        if (i == -1 || j == -1) {
            return "";
        } else {
            return smbUrl.substring(i + 1, j);
        }
    }

    private String parseIpText(String ipText) {
        if ((ipText == null) || (ipText.length() == 0)) {
            return null;
        }

        if (ipText.startsWith("\\\\")) {
            ipText = ipText.substring(2);
        }
        if (ipText.startsWith("\\")) {
            ipText = ipText.substring(1);
        }
        if (ipText.startsWith("//")) {
            ipText = ipText.substring(2);
        }
        if (ipText.startsWith("/")) {
            ipText = ipText.substring(1);
        }
        if (ipText.endsWith("\\")) {
            ipText = ipText.substring(0, ipText.length() - 1);
        }
        if (ipText.endsWith("/")) {
            ipText = ipText.substring(0, ipText.length() - 1);
        }
        return ipText;
    }

    private String addServerInfoFormat() {
        String ipText = mIP.getText().toString();
        String username = mUsername.getText().toString();
        String password = mPassword.getText().toString();

        String serverurl = null;

        if (ipText.length() == 0) {
            return null;
        }

        ipText = parseIpText(ipText);
        mDisplayName = ipText;
        if (ipText != null) {
            SambaExplorer.log(
                    TAG + "addServerInfoFormat ipText: " + ipText + " " + ipText.length(), false);
        } else {
            SambaExplorer.log(TAG + "addServerInfoFormat ipText is null", false);
        }
        serverurl = SambaExplorer.SMB_FILE_PRIFIX + username + "\0" + password + "\0" + ipText;
        return serverurl;
    }

    private void formatServerInfo(boolean def) {
        mHostIP = def ? null : "";
        mHostAccount = def ? null : "Guest";
        mHostPwd = def ? null : "Guest";
        mDisplayName = def ? mDisplayName : mHostIP;
    }

    public void createNewServerDialog() {
        formatServerInfo(true);
        createServerEditDialog(true, R.string.add_samba_server_title);
    }

    private void serverInfoEditor(String serverinfo) {
        parseSmbUrl(serverinfo);
        createServerEditDialog(false, R.string.edit_samba_server_title);
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

    private void createServerEditDialog(final boolean bConnect, int titleId) {
        LayoutInflater factory = LayoutInflater.from(mParentFragment.getActivity());
        final View addServerInfoView = factory.inflate(R.layout.alert_dialog_new_server, null);

        final Dialog serverEditDialog =
                new AlertDialog.Builder(mParentFragment.getActivity()).setTitle(titleId)
                        .setView(addServerInfoView).create();

        mIP = (EditText) addServerInfoView.findViewById(R.id.hostname_edit);
        mUsername = (EditText) addServerInfoView.findViewById(R.id.username_edit);
        mPassword = (EditText) addServerInfoView.findViewById(R.id.password_edit);

        mIP.setText(mHostIP);
        mUsername.setText(mHostAccount);
        mPassword.setText(mHostPwd);

        Button mAddServerBtn = (Button) addServerInfoView.findViewById(R.id.AddServerOk);
        mAddServerBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String tempUrl = addServerInfoFormat();
                if (tempUrl != null) {
                    mParentFragment.setSambaPreferences(mDisplayName, tempUrl);
                    serverEditDialog.dismiss();
                    if (bConnect) {
                        connectSmbServer(tempUrl);
                    }
                } else {
                    makeToast(R.string.host_name_empty_indicator);
                    return;
                }
            }
        });

        Button mCancel = (Button) addServerInfoView.findViewById(R.id.AddServerCancel);
        mCancel.setOnClickListener(new OnClickListener() {
            // @Override
            @Override
            public void onClick(View v) {
                serverEditDialog.dismiss();
            }
        });

        serverEditDialog.setOwnerActivity(mParentFragment.getActivity());
        serverEditDialog.show();
    }

    private void updateAdapter() {
        SambaExplorer.log(TAG + " updateAdapter !", false);
        mServerListAdapter.notifyDataSetChanged();
    }

    OnMenuItemClickListener smbDeviceMenuItemClickListener = new OnMenuItemClickListener() {
        // @Override
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();

            if (mServerListAdapter == null) {
                SambaExplorer.log(TAG +
                        "In onContextItemSelected...mServerListAdapteris null return ", false);
                return false;
            }
            String mItemName = mListDevice.get(menuInfo.position).getText();

            String hostInfo = getPrefValueWithKey(mItemName);// ((RemoteFileManager)
            // mParentFragment).getMyPrefValue(mItemName);
            SambaExplorer.log(TAG + "selected file is ... " + hostInfo, false);

            switch (item.getItemId()) {
                case MENU_OPEN : // Open
                    SambaExplorer.log(TAG + "Connect... ", false);
                    connectSmbServer(hostInfo);
                    return true;
                case MENU_REMOVE : // Remove
                    SambaExplorer.log(TAG + "Delete... ", false);
                    SharedPreferences.Editor editor =
                            mParentFragment.getActivity().getPreferences(0).edit();
                    editor.remove(mListDevice.get(menuInfo.position).getText());
                    editor.apply();
                    updateServerList();
                    return true;
                case MENU_EDIT : // Edit
                    SambaExplorer.log(TAG + "Edit... ", false);
                    serverInfoEditor(hostInfo);
                    return true;
            }
            return false;
        }
    };

    // @Override
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            return;
        }

        IconifiedText it = (IconifiedText) mServerListAdapter.getItem(info.position);
        if (it.getFieldTag() == 0) {
            menu.add(0, MENU_OPEN, 0, R.string.menu_open).setOnMenuItemClickListener(
                    smbDeviceMenuItemClickListener);
            menu.add(0, MENU_REMOVE, 0, R.string.menu_remove).setOnMenuItemClickListener(
                    smbDeviceMenuItemClickListener);
            menu.add(0, MENU_EDIT, 0, R.string.menu_edit).setOnMenuItemClickListener(
                    smbDeviceMenuItemClickListener);
            // menu.add(0,3,3,"Info").setOnMenuItemClickListener(smbDeviceMenuItemClickListener);
        }
    }

    // @Override
    @Override
    public void onItemClick(AdapterView l, View v, int position, long id) {
        IconifiedText it = mListDevice.get(position);

        if (it.getFieldTag() == 0) {
            mDisplayName = it.getText();
            String hostPath = getPrefValueWithKey(mDisplayName);
            SambaExplorer.log("onListItemClick item is :" + hostPath, false);
            connectSmbServer(hostPath);
        } else {
            SambaExplorer.log(TAG + "onItemClick Add server", false);
            addServerSelectDialog();
        }
    }

    private void tryToListGroup() {
        SambaExplorer.log(TAG + "tryToListGroup: ", false);

        if (mParentFragment.getClass().getName().equals(RemoteContentLeftFragment.class.getName())) {
            //user click the Add server from left panel
            Fragment rightFragment =
                    mParentFragment.getActivity().getFragmentManager()
                            .findFragmentById(R.id.details);
            if ((rightFragment != null) &&
                    rightFragment.getClass().getName()
                            .equals(RemoteContentFragment.class.getName())) {

                Handler handler = ((RemoteContentFragment) rightFragment).getHandler();
                if (handler != null) {
                    handler.sendMessage(formatTransferMsg(null,
                            RemoteContentBaseFragment.MESSAGE_SEARCH_SAMBA_GROUP));
                }
            }
        } else {
            mHandler.sendMessage(formatTransferMsg(null,
                    RemoteContentBaseFragment.MESSAGE_SEARCH_SAMBA_GROUP));
        }

    }

    private void connectSmbServer(String hostPath) {
        SambaExplorer.log(TAG + "connectSmbServer: " + hostPath, false);
        parseSmbUrl(hostPath);
        if (mHostAccount.length() == 0 && mHostPwd.length() == 0) {
            SambaTransferHandler.setUserAuth(NtlmPasswordAuthentication.ANONYMOUS);
        } else {
            SambaTransferHandler.provideLoginCredentials(null, mHostAccount, mHostPwd);
        }
        checkConnAvailable();
        if (mParentFragment.getClass().getName().equals(RemoteContentLeftFragment.class.getName())) {
            //user click a host from left panel
            Fragment rightFragment =
                    mParentFragment.getActivity().getFragmentManager()
                            .findFragmentById(R.id.details);
            if ((rightFragment != null) &&
                    rightFragment.getClass().getName()
                            .equals(RemoteContentFragment.class.getName())) {

                Handler handler = ((RemoteContentFragment) rightFragment).getHandler();
                if (handler != null) {
                    handler.sendMessage(formatTransferMsg("smb://" + mHostIP + "/",
                            RemoteContentBaseFragment.MESSAGE_LAUNCH_SAMBA_EXPLORE));
                }
            }
        } else {
            mHandler.sendMessage(formatTransferMsg("smb://" + mHostIP + "/",
                    RemoteContentBaseFragment.MESSAGE_LAUNCH_SAMBA_EXPLORE));
        }
    }

    public static String getHostIP(String hostName) throws UnknownHostException {
        String hostIP = NbtAddress.getByName(hostName).getInetAddress().toString();
        SambaExplorer.log(TAG + "getAllByAddress: " + hostIP, false);

        if (hostIP.startsWith("/")) {
            hostIP = hostIP.substring(1);
        }

        return hostIP;
    }

    public void addServerSelectDialog() {
        new AlertDialog.Builder(mParentFragment.getActivity()).setTitle(R.string.add_server)
                .setItems(R.array.select_add_server_way, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // String[] items =
                        // mParentFragment.getResources().getStringArray(R.array.select_add_server_way);
                        switch (which) {
                            case 0 : // Manually
                                createNewServerDialog();
                                break;
                            case 1 : // Via search
                                // discoveryServerList();
                                tryToListGroup();
                                break;
                            case 2 : // Via search
                                // discoveryServerList();
                                break;
                        }
                    }
                }).show();
    }

    private Message formatTransferMsg(String hostInfo, int msgId) {
        Message msg = Message.obtain();
        if ((hostInfo != null) && (hostInfo.startsWith("smb://"))) {
            Bundle extras = new Bundle();
            extras.putString(RemoteContentBaseFragment.SAMABA_HOST_INFO, hostInfo);
            extras.putString(RemoteContentBaseFragment.SAMABA_HOST_NAME, mDisplayName);
            msg.what = msgId;// RemoteFileManager.MESSAGE_LAUNCH_SAMBA_EXPLORE ;
            msg.setData(extras);
        }
        msg.what = msgId;
        return msg;
    }

    private int checkConnAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) mParentFragment.getActivity().getBaseContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (cm == null) {
            SambaExplorer.log(TAG + "SambaDownloadServer: " +
                    "unable to get CONNECTIVITY_SERVICE connection, skipping copy attempt", false);
            return 0;
        } else {
            networkInfo = cm.getActiveNetworkInfo();
            if ((networkInfo == null) || networkInfo.getType() != ConnectivityManager.TYPE_WIFI ||
                    !networkInfo.isConnected()) {
                SambaExplorer.log(TAG + "SambaDownloadServer: " +
                        "No valid network connectivity, ignoring copy request.", false);
                return 0;
            }
        }
        return 1;
    }
}
