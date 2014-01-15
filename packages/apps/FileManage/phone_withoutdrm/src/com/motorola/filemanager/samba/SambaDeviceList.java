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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.motorola.filemanager.R;
import com.motorola.filemanager.samba.service.SambaTransferHandler;
import com.motorola.filemanager.utils.IconifiedText;
import com.motorola.filemanager.utils.IconifiedTextListAdapter;

public class SambaDeviceList implements OnItemClickListener,
	OnCreateContextMenuListener {
    private static final String TAG = "SambaDeviceList: ";

    private EditText mIP;
    private EditText mUsername;
    private EditText mPassword;
    public Dialog loginDialog;
    private Activity mParentActivity;

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

    public SambaDeviceList(View smbDeviceView, Activity parent,
	    final Handler handler) {
	mParentActivity = parent;
	mContext = mParentActivity.getApplicationContext();
	// mSavedServerview =
	// (ListView)mParentActivity.findViewById(R.id.SavedServerList);
	if (smbDeviceView instanceof ListView) {
	    mSavedServerview = (ListView) smbDeviceView;
	    mSavedServerview.setOnItemClickListener(this);
	    mSavedServerview.setOnCreateContextMenuListener(this);
	    setSmbDeviceViewAdapter();
	    updateServerList();
	}
	mHandler = handler;
    }

    public SambaDeviceList(View smbDeviceView, Activity parent) {
	mParentActivity = parent;
	mContext = mParentActivity.getApplicationContext();
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
	mServerListAdapter = new IconifiedTextListAdapter(mContext);
	mServerListAdapter.setListItems(mListDevice,
		mSavedServerview.hasTextFilter());
	mSavedServerview.setAdapter(mServerListAdapter);
    }

    private String getPrefValueWithKey(String key) {
	SharedPreferences prefs = mParentActivity.getPreferences(0);
	return (String) prefs.getAll().get(key);
    }

    public void updateServerList() {
	SharedPreferences prefs = mParentActivity.getPreferences(0);
	mServerMap = (Map<String, String>) prefs.getAll();
	SambaExplorer.log(TAG + "updateServerList..mServerMap: " + mServerMap);

	mListDevicePreference = mParentActivity.getResources().getTextArray(
		R.array.add_server_indicater);
	setServerListSize(mServerMap.size() + mListDevicePreference.length);
	// setServerListSize(mServerMap.size());
    }

    private void setServerListSize(int size) {
	mListDevice.clear();
	if (size > 0) {
	    // if(size >mListDevicePreference.length){
	    mListDevice.ensureCapacity(size);
	    for (int i = 0; i < mListDevicePreference.length; i++) {
		IconifiedText item = IconifiedText.buildIconItemWithoutInfo(
			mContext, (String) mListDevicePreference[i], 1);
		item.setIsNormalFile(false);
		mListDevice.add(item);
	    }

	    Iterator<?> it = mServerMap.entrySet().iterator();
	    while (it.hasNext()) {
		Map.Entry e = (Map.Entry) it.next();
		IconifiedText item = IconifiedText.buildIconItemWithoutInfo(
			mContext, (String) e.getKey(), 0);
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

	SambaExplorer.log("parseSmbUrl mHostIP: " + mHostIP + " mHostAccount: "
		+ mHostAccount + " mHostPwd: " + mHostPwd);
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
	    return smbUrl.substring(i + SambaExplorer.SMB_FILE_PRIFIX.length(),
		    j);
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
	    SambaExplorer.log(TAG + "addServerInfoFormat ipText: " + ipText
		    + " " + ipText.length());
	} else {
	    SambaExplorer.log(TAG + "addServerInfoFormat ipText is null");
	}
	serverurl = SambaExplorer.SMB_FILE_PRIFIX + username + "\0" + password
		+ "\0" + ipText;
	SambaExplorer.log(TAG + "addServerInfoFormat: " + serverurl);
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
	Toast.makeText(mContext.getApplicationContext(), msgId,
		Toast.LENGTH_SHORT).show();
    }

    private void createServerEditDialog(final boolean bConnect, int titleId) {
	LayoutInflater factory = LayoutInflater.from(mParentActivity);
	final View addServerInfoView = factory.inflate(
		R.layout.alert_dialog_new_server, null);

	final Dialog serverEditDialog = new AlertDialog.Builder(mParentActivity)
		.setTitle(titleId).setView(addServerInfoView).create();

	mIP = (EditText) addServerInfoView.findViewById(R.id.hostname_edit);
	mUsername = (EditText) addServerInfoView
		.findViewById(R.id.username_edit);
	mPassword = (EditText) addServerInfoView
		.findViewById(R.id.password_edit);

	mIP.setText(mHostIP);
	mUsername.setText(mHostAccount);
	mPassword.setText(mHostPwd);

	Button mAddServerBtn = (Button) addServerInfoView
		.findViewById(R.id.AddServerOk);
	mAddServerBtn.setOnClickListener(new OnClickListener() {
	    // @Override
	    public void onClick(View v) {
		String tempUrl = addServerInfoFormat();
		if (tempUrl != null) {
		    ((RemoteFileManager) mParentActivity).setSambaPreferences(
			    mDisplayName, tempUrl);
		    SambaExplorer.log(TAG + "Add server info: " + tempUrl
			    + mIP.getText().toString());
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

	Button mCancel = (Button) addServerInfoView
		.findViewById(R.id.AddServerCancel);
	mCancel.setOnClickListener(new OnClickListener() {
	    // @Override
	    public void onClick(View v) {
		serverEditDialog.dismiss();
	    }
	});

	serverEditDialog.setOwnerActivity(mParentActivity);
	serverEditDialog.show();
    }

    private void updateAdapter() {
	SambaExplorer.log(TAG + " updateAdapter !");
	mServerListAdapter.notifyDataSetChanged();
    }

    OnMenuItemClickListener smbDeviceMenuItemClickListener = new OnMenuItemClickListener() {
	// @Override
	public boolean onMenuItemClick(MenuItem item) {
	    AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
		    .getMenuInfo();

	    if (mServerListAdapter == null) {
		SambaExplorer
			.log(TAG
				+ "In onContextItemSelected...mServerListAdapteris null return ");
		return false;
	    }
	    String mItemName = mListDevice.get(menuInfo.position).getText();

	    String hostInfo = getPrefValueWithKey(mItemName);// ((RemoteFileManager)
							     // mParentActivity).getMyPrefValue(mItemName);
	    SambaExplorer.log(TAG + "selected file is ... " + hostInfo);

	    switch (item.getItemId()) {
	    case MENU_OPEN: // Open
		SambaExplorer.log(TAG + "Connect... ");
		connectSmbServer(hostInfo);
		return true;
	    case MENU_REMOVE: // Remove
		SambaExplorer.log(TAG + "Delete... ");
		SharedPreferences.Editor editor = mParentActivity
			.getPreferences(0).edit();
		editor.remove(mListDevice.get(menuInfo.position).getText());
		editor.apply();
		updateServerList();
		return true;
	    case MENU_EDIT: // Edit
		SambaExplorer.log(TAG + "Edit... ");
		serverInfoEditor(hostInfo);
		return true;
	    }
	    return false;
	}
    };

    // @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
	    ContextMenuInfo menuInfo) {
	AdapterView.AdapterContextMenuInfo info;
	try {
	    info = (AdapterView.AdapterContextMenuInfo) menuInfo;
	} catch (ClassCastException e) {
	    return;
	}

	IconifiedText it = (IconifiedText) mServerListAdapter
		.getItem(info.position);
	if (it.getFieldTag() == 0) {
	    menu.add(0, MENU_OPEN, 0, R.string.menu_open)
		    .setOnMenuItemClickListener(smbDeviceMenuItemClickListener);
	    menu.add(0, MENU_REMOVE, 0, R.string.menu_remove)
		    .setOnMenuItemClickListener(smbDeviceMenuItemClickListener);
	    menu.add(0, MENU_EDIT, 0, R.string.menu_edit)
		    .setOnMenuItemClickListener(smbDeviceMenuItemClickListener);
	    // menu.add(0,3,3,"Info").setOnMenuItemClickListener(smbDeviceMenuItemClickListener);
	}
    }

    // @Override
    public void onItemClick(AdapterView l, View v, int position, long id) {
	IconifiedText it = mListDevice.get(position);

	if (it.getFieldTag() == 0) {
	    mDisplayName = it.getText();
	    String hostPath = getPrefValueWithKey(mDisplayName);
	    SambaExplorer.log("onListItemClick item is :" + hostPath);
	    connectSmbServer(hostPath);
	} else {
	    SambaExplorer.log(TAG + "onItemClick Add server");
	    addServerSelectDialog();
	}
    }

    private void tryToListGroup() {
	SambaExplorer.log(TAG + "tryToListGroup: ");
	mHandler.sendMessage(formatTransferMsg(null,
		RemoteFileManager.MESSAGE_SEARCH_SAMBA_GROUP));
    }

    private void connectSmbServer(String hostPath) {
	SambaExplorer.log(TAG + "connectSmbServer: " + hostPath);
	parseSmbUrl(hostPath);
	if (mHostAccount.length() == 0 && mHostPwd.length() == 0) {
	    SambaTransferHandler
		    .setUserAuth(NtlmPasswordAuthentication.ANONYMOUS);
	} else {
	    SambaTransferHandler.provideLoginCredentials(null, mHostAccount,
		    mHostPwd);
	}
	checkConnAvailable();

	SambaExplorer.log(TAG + "logon samba server :" + mHostIP
		+ " with userAuth: " + SambaTransferHandler.getUserAuth());
	mHandler.sendMessage(formatTransferMsg("smb://" + mHostIP + "/",
		RemoteFileManager.MESSAGE_LAUNCH_SAMBA_EXPLORE));
    }

    public static String getHostIP(String hostName) throws UnknownHostException {
	String hostIP = null;
	try {
	    hostIP = NbtAddress.getByName(hostName).getInetAddress().toString();
	} catch (ExceptionInInitializerError e) {
	    SambaExplorer.log(TAG + "getHostIP ExceptionInInitializerError "
		    + e);
	    return null;
	} catch (Exception e) {
	    SambaExplorer.log(TAG
		    + "getHostIP: initialize NbtAddress exception");
	    e.printStackTrace();
	    return null;
	}
	SambaExplorer.log(TAG + "getAllByAddress: " + hostIP);

	if (hostIP.startsWith("/")) {
	    hostIP = hostIP.substring(1);
	}
	return hostIP;
    }

    private void addServerSelectDialog() {
	new AlertDialog.Builder(mParentActivity)
		.setTitle(R.string.add_server)
		.setItems(R.array.select_add_server_way,
			new DialogInterface.OnClickListener() {
			    // @Override
			    public void onClick(DialogInterface dialog,
				    int which) {
				// String[] items =
				// mParentActivity.getResources().getStringArray(R.array.select_add_server_way);
				switch (which) {
				case 0: // Manually
				    createNewServerDialog();
				    break;
				case 1: // Via search
					// discoveryServerList();
				    tryToListGroup();
				    break;
				case 2: // Via search
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
	    extras.putString(RemoteFileManager.SAMABA_HOST_INFO, hostInfo);
	    extras.putString(RemoteFileManager.SAMABA_HOST_NAME, mDisplayName);
	    msg.what = msgId;// RemoteFileManager.MESSAGE_LAUNCH_SAMBA_EXPLORE ;
	    msg.setData(extras);
	}
	msg.what = msgId;
	return msg;
    }

    private int checkConnAvailable() {
	ConnectivityManager cm = (ConnectivityManager) mParentActivity
		.getBaseContext()
		.getSystemService(Context.CONNECTIVITY_SERVICE);
	NetworkInfo networkInfo = null;
	if (cm == null) {
	    SambaExplorer
		    .log(TAG
			    + "SambaDownloadServer: "
			    + "unable to get CONNECTIVITY_SERVICE connection, skipping copy attempt");
	    return 0;
	} else {
	    networkInfo = cm.getActiveNetworkInfo();
	    if ((networkInfo == null)
		    || networkInfo.getType() != ConnectivityManager.TYPE_WIFI
		    || !networkInfo.isConnected()) {
		SambaExplorer
			.log(TAG
				+ "SambaDownloadServer: "
				+ "No valid network connectivity, ignoring copy request.");
		return 0;
	    }
	}
	return 1;
    }
}
