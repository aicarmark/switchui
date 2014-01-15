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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import jcifs.smb.SmbFile;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.motorola.filemanager.FileManager;
import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.FileManagerContent;
import com.motorola.filemanager.R;
import com.motorola.filemanager.samba.service.DownloadServer;
import com.motorola.filemanager.utils.FileUtils;
import com.motorola.filemanager.utils.IconifiedTextListAdapter;

public class RemoteFileManager extends Activity {
    private static final String TAG = "RemoteFileManager: ";

    private SambaExplorer mSmbExplorer;
    private SambaDeviceList mSmbDevice;
    private SambaGroupList mSmbGroup;

    private ListView mSmbDeviceView = null;
    private ListView mSmbGroupView = null;
    private LinearLayout mSmbGroupLayout = null;
    private LinearLayout mSmbFileLayout = null;
    private LinearLayout mRemoteDeviceLayout = null;
    private boolean whetherInFileList = false;
    private Button mCurrentFolder = null;
    private View mMultiSelectedPanel;
    private File tmplFilePath = null;
    public static final String SAMABA_HOST_INFO = "SAMABA_HOST_INFO";
    public static final String SAMABA_HOST_NAME = "SAMABA_HOST_NAME";

    private static final int MENU_ADD_SERVER = Menu.FIRST;
    private static final int MENU_NEW_FOLDER = Menu.FIRST + 1;
    private static final int MENU_SELECTALL = 11;
    private static final int MENU_UNSELECTALL = 12;
    private static final int MENU_COPYSELECTED = 13;
    private static final int MENU_MULTISELECT = 15;
    private static final int MENU_RENAMESELECTED = 16;
    private static final int MENU_OPEN_SELECTED = 17;
    private static final int MENU_INFO_SELECTED = 18;
    private static final int OPT_MENU_VIEW_SWITCH = 19;
    private static final int OPT_MENU_SORT = 20;
    private static final int OPT_MENU_SORT_BY_NAME = 21;
    private static final int OPT_MENU_SORT_BY_TYPE = 22;
    private static final int OPT_MENU_SORT_BY_TIME = 23;
    private static final int OPT_MENU_SORT_BY_SIZE = 24;

    public static final String PROGRESS_DIALOG_MODE = "PROGRESS_DIALOG_MODE";

    protected static final int PROGRESS_DIALOG_SCANNING_DOMAIN = 0;
    protected static final int PROGRESS_DIALOG_SCANNING_SERVER = 1;
    protected static final int PROGRESS_DIALOG_CONNECTING_SERVER = 2;
    protected static final int PROGRESS_DIALOG_LOADING_FILE = 3;

    /*
     * <string-array name="progress_dialog_indicater"> <item>Scanning for
     * domains...</item> <item>Scanning for servers...</item> <item>Connecting
     * for server...</item> <item>loading...</item> </string-array>
     */
    private CharSequence[] mProgressDiagIndicator = null;
    private ProgressDialog loadingDialog = null;

    protected static final int MESSAGE_DELAY_REMOVE_TEMPFILE = 2;
    public static final int Samba_Explorer_LAUNCH = 200;

    protected static final int MESSAGE_LAUNCH_SAMBA_EXPLORE = 400;
    protected static final int MESSAGE_RETURN_TO_DEVICE = 401;
    protected static final int MESSAGE_SHOW_FILE_LIST_VIEW = 402;
    protected static final int MESSAGE_SHOW_LOADING_DIALOG = 403;
    protected static final int MESSAGE_HIDE_LOADING_DIALOG = 404;
    protected static final int MESSAGE_SEARCH_SAMBA_GROUP = 405;
    protected static final int MESSAGE_HIDE_MULISELECTED_PANEL = 406;
    static final public int MESSAGE_COPY_FINISHED = 407;
    static final public int MESSAGE_MOVE_FINISHED = 408;
    protected static final int MESSAGE_SET_LOCAL_MOVEMODE = 409;
    protected static final int MESSAGE_SET_LOCAL_COPYMODE = 410;

    private int mLocalLaunchMode = FileManagerApp.NORMAL_MODE;
    private int mLocalPasteMode = FileManagerApp.NO_PASTE;

    private Context mContext;

    @Override
    protected void onCreate(Bundle icicle) {
	super.onCreate(icicle);
	setContentView(R.layout.remote_filemanager);
	mContext = this;

	mSmbFileLayout = (LinearLayout) findViewById(R.id.SmbExploreLayout);

	mRemoteDeviceLayout = (LinearLayout) findViewById(R.id.SavedSmbDevice);
	mSmbDeviceView = (ListView) findViewById(R.id.SavedServerList);
	mSmbExplorer = new SambaExplorer(mRemoteHandler, this);
	mSmbDevice = new SambaDeviceList(mSmbDeviceView, this, mRemoteHandler);

	mSmbGroupLayout = (LinearLayout) findViewById(R.id.SmbGroupLayout);
	mSmbGroupView = (ListView) findViewById(R.id.SmbGroupList);
	mSmbGroup = new SambaGroupList(mSmbGroupView, this, mRemoteHandler);

	showServerList();
	createTmpFilePath(".tempPath");
	setLocalLaunchMode(FileManagerApp.getLaunchMode());
	setLocalPasteMode(FileManagerApp.getPasteMode());

	Button sambaServerTitle = (Button) findViewById(R.id.show_server_view);
	if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FILE_MODE) {
	    sambaServerTitle.setText(R.string.select_file);
	} else if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FOLDER_MODE) {
	    sambaServerTitle.setText(R.string.select_folder);
	}

	mCurrentFolder = (Button) findViewById(R.id.show_folder_view);

	findViewById(R.id.show_server_view).setOnClickListener(
		new View.OnClickListener() {
		    // @Override
		    public void onClick(View v) {
			showFolderViewDialogForGroupList();
		    }
		});
	findViewById(R.id.show_folder_view).setOnClickListener(
		new View.OnClickListener() {
		    // @Override
		    public void onClick(View v) {
			showFolderViewDialog();
		    }
		});

	findViewById(R.id.show_folder_view_group).setOnClickListener(
		new View.OnClickListener() {
		    // @Override
		    public void onClick(View v) {
			showFolderViewDialogForGroupList();
		    }
		});

	mMultiSelectedPanel = findViewById(R.id.smb_multi_select_panel);
	((Button) mMultiSelectedPanel.findViewById(R.id.button_smb_multi_move))
		.setOnClickListener(new View.OnClickListener() {
		    // @Override
		    public void onClick(View v) {
			ArrayList<String> tmpMoveList = new ArrayList<String>();
			for (String src : mSmbExplorer.getSmbSelectFiles()) {
			    tmpMoveList.add(new File(src).getName());
			}
			move(tmpMoveList);
			outMultiSelectMode();
		    }
		});
	((Button) mMultiSelectedPanel
		.findViewById(R.id.button_smb_multi_delete))
		.setOnClickListener(new View.OnClickListener() {
		    // @Override
		    public void onClick(View v) {
			mSmbExplorer
				.createDialog(SambaExplorer.DIALOG_DELETE_SELECTED_FILES);
		    }
		});
	((Button) mMultiSelectedPanel
		.findViewById(R.id.button_smb_multi_cancel))
		.setOnClickListener(new View.OnClickListener() {
		    // @Override
		    public void onClick(View v) {
			outMultiSelectMode();
		    }
		});

	mProgressDiagIndicator = getProgressDialogIndicator(R.array.progress_dialog_indicater);
    }

    @Override
    protected void onStart() {
	super.onStart();
	registerReceiver(mWiFiReceiver, new IntentFilter(
		WifiManager.NETWORK_STATE_CHANGED_ACTION));
	SambaExplorer.log(" onStart, register WIFI receiver.");
    }

    @Override
    protected void onStop() {
	super.onStop();
	unregisterReceiver(mWiFiReceiver);
	SambaExplorer.log(" onStop, unregister WIFI receiver.");
    }

    private CharSequence[] getProgressDialogIndicator(int resId) {
	return getResources().getTextArray(resId);
    }

    private boolean checkConnAvailable() {
	ConnectivityManager cm = (ConnectivityManager) getBaseContext()
		.getSystemService(Context.CONNECTIVITY_SERVICE);
	NetworkInfo netInfo = null;
	if (cm != null) {
	    netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null) {
		if (!netInfo.isConnected()
			|| netInfo.getType() != ConnectivityManager.TYPE_WIFI) {
		    return false;
		}
	    } else {
		return false;
	    }
	} else {
	    return false;
	}
	return true;
    }

    private final BroadcastReceiver mWiFiReceiver = new BroadcastReceiver() {
	@Override
	public void onReceive(Context context, Intent intent) {
	    if (intent != null) {
		String action = intent.getAction();
		if ((action != null)
			&& (action
				.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION))) {
		    if (!checkConnAvailable()) {
			if (FileManager.getCurrentContent().equals(
				FileManager.ID_PROTECTED)) {
			    showServerList();
			    Toast.makeText(mContext, R.string.check_wifi,
				    Toast.LENGTH_SHORT).show();
			}
			return;
		    }
		    // setSmbLocalAddress();
		    // Toast.makeText(mContext, R.string.wifi_is_avaiable,
		    // Toast.LENGTH_SHORT).show();
		}
	    }
	}
    };

    public String getBroadcastAddress() throws IOException {
	WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	if (wifi == null) {
	    return null;
	}

	DhcpInfo dhcp = wifi.getDhcpInfo();
	if (dhcp == null) {
	    return null;
	}

	int ipAddress = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;

	String dotIPAddress = "";
	dotIPAddress = Integer.toString((ipAddress & 0x000000ff)) + "."
		+ Integer.toString((ipAddress & 0x0000ff00) >>> 8) + "."
		+ Integer.toString((ipAddress & 0x00ff0000) >>> 16) + "."
		+ Integer.toString((ipAddress & 0xff000000) >>> 24);

	Log.e("WiFi BroadCast IP Address", dotIPAddress);

	return dotIPAddress;
    }

    private String getIp(Context context) {
	WifiManager wifiManager = (WifiManager) context
		.getSystemService(Context.WIFI_SERVICE);
	WifiInfo wifiInfo = wifiManager.getConnectionInfo();
	int ipAddress = wifiInfo.getIpAddress();
	String dotIPAddress = "";
	dotIPAddress = Integer.toString((ipAddress & 0x000000ff)) + "."
		+ Integer.toString((ipAddress & 0x0000ff00) >>> 8) + "."
		+ Integer.toString((ipAddress & 0x00ff0000) >>> 16) + "."
		+ Integer.toString((ipAddress & 0xff000000) >>> 24);

	SambaExplorer.log(TAG + "IP " + dotIPAddress);

	return dotIPAddress;
    }

    private Handler mRemoteHandler = new Handler() {
	@Override
	public void handleMessage(Message msg) {
	    switch (msg.what) {
	    case MESSAGE_LAUNCH_SAMBA_EXPLORE:
		String hostInfo = msg.getData().getString(SAMABA_HOST_INFO);
		showLoadingDialog(
			mProgressDiagIndicator[PROGRESS_DIALOG_CONNECTING_SERVER],
			PROGRESS_DIALOG_CONNECTING_SERVER);
		startSmabaExplore(hostInfo);
		return;
	    case MESSAGE_RETURN_TO_DEVICE:
		showServerList();
		return;
	    case MESSAGE_SHOW_FILE_LIST_VIEW:
		SambaExplorer.log(TAG + " stepInServer success !");
		hideLoadingDialog();
		showSmbFileList();
		String path = mSmbExplorer.getCurPath();
		path = path.substring(0, path.lastIndexOf("/"));
		path = path.substring(path.lastIndexOf("/"));
		path = path.substring(1);
		mCurrentFolder.setText(path);
		if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FILE_MODE) {
		    mCurrentFolder.setText(R.string.select_file);
		} else if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FOLDER_MODE) {
		    mCurrentFolder.setText(R.string.select_folder);
		}
		return;
	    case MESSAGE_HIDE_LOADING_DIALOG:
		hideLoadingDialog();
		return;
	    case MESSAGE_SHOW_LOADING_DIALOG:
		int msgMode = msg.getData().getInt(PROGRESS_DIALOG_MODE);
		showLoadingDialog(mProgressDiagIndicator[msgMode], msgMode);
		return;
	    case MESSAGE_DELAY_REMOVE_TEMPFILE:
		SambaExplorer.log(TAG + " MESSAGE_DELAY_REMOVE_TEMPFILE!");
		mDeleteTmpFile = true;
		return;
	    case MESSAGE_SEARCH_SAMBA_GROUP:
		startGroupSearch();
		showGroupList();
		return;
	    case MESSAGE_HIDE_MULISELECTED_PANEL:
		mMultiSelectedPanel.setVisibility(View.GONE);
		mSmbExplorer.outSmbExploreMultiSelectMode();
		return;
	    case MESSAGE_SET_LOCAL_MOVEMODE:
		setLocalPasteMode(FileManagerApp.MOVE_MODE);
		return;
	    case MESSAGE_SET_LOCAL_COPYMODE:
		setLocalPasteMode(FileManagerApp.COPY_MODE);
		return;
	    case MESSAGE_COPY_FINISHED:
		if (msg.arg1 == DownloadServer.TRANSFER_COMPLETE
			|| msg.arg1 == DownloadServer.TRANSFER_MULTI_COMPLETE) {
		    Toast.makeText(mContext, R.string.copy_sucessfully,
			    Toast.LENGTH_SHORT).show();
		} else {
		    Toast.makeText(mContext, R.string.copy_failed,
			    Toast.LENGTH_SHORT).show();
		}
		return;
	    case MESSAGE_MOVE_FINISHED:
		hideLoadingDialog();
		mSmbExplorer.browseTo(mSmbExplorer.getCurPath());
		if (msg.arg1 == DownloadServer.TRANSFER_COMPLETE
			|| msg.arg1 == DownloadServer.TRANSFER_MULTI_COMPLETE) {
		    Toast.makeText(mContext, R.string.move_sucess,
			    Toast.LENGTH_SHORT).show();
		} else if (msg.arg1 == DownloadServer.TRANSFER_MULTI_FAIL) {
		    Toast.makeText(mContext, R.string.error_moving_multiple,
			    Toast.LENGTH_SHORT).show();
		} else {
		    Toast.makeText(mContext, R.string.error_moving,
			    Toast.LENGTH_SHORT).show();
		}
		return;
	    }
	}
    };

    private void startSmabaExplore(String host) {
	mSmbExplorer.stepInServer(host);
    }

    private void startGroupSearch() {
	SambaExplorer.log(TAG + "startGroupSearch");
	mSmbGroup.listSmbGroup();
    }

    private void createTmpFilePath(String filename) {
	String mSdCardPath = FileManagerApp
		.getDeviceStorageVolumePath(FileManagerApp.mIndexSDcard);

	tmplFilePath = FileUtils.getFile(mSdCardPath, filename);
	SambaExplorer.log(TAG + " in createLocalPath SdCardPath ! "
		+ mSdCardPath);
	if (!tmplFilePath.exists()) {
	    if (tmplFilePath.mkdirs() == false) {
		SambaExplorer.log(TAG + "mkdirs failed "
			+ tmplFilePath.getName());
	    }
	}
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

    private void showSmbFileList() {
	whetherInFileList = true;
	mRemoteDeviceLayout.setVisibility(View.GONE);
	mSmbGroupLayout.setVisibility(View.GONE);
	mSmbFileLayout.setVisibility(View.VISIBLE);
	if (FileManagerApp.getPasteMode() != FileManagerApp.NO_PASTE) {
	    FileManagerContent.displayPasteButton();
	}

    }

    private void showServerList() {
	whetherInFileList = false;
	mSmbFileLayout.setVisibility(View.GONE);
	mSmbGroupLayout.setVisibility(View.GONE);
	mRemoteDeviceLayout.setVisibility(View.VISIBLE);
	if (FileManagerApp.getPasteMode() != FileManagerApp.NO_PASTE) {
	    FileManagerContent.hidePasteButton();
	}
    }

    public void showGroupList() {
	whetherInFileList = false;
	mSmbFileLayout.setVisibility(View.GONE);
	mRemoteDeviceLayout.setVisibility(View.GONE);
	mSmbGroupLayout.setVisibility(View.VISIBLE);
	if (FileManagerApp.getPasteMode() != FileManagerApp.NO_PASTE) {
	    FileManagerContent.hidePasteButton();
	}
    }

    public void setLocalLaunchMode(int launchMode) {
	mLocalLaunchMode = launchMode;
    }

    public int getLocalLaunchMode() {
	return mLocalLaunchMode;
    }

    public void setLocalPasteMode(int inPasteMode) {
	mLocalPasteMode = inPasteMode;
    }

    public int getLocalPasteMode() {
	return mLocalPasteMode;
    }

    @Override
    public void onDestroy() {
	super.onDestroy();
	SambaExplorer.log(" onDestroy...");
	deleteTmpFile();
	// stopDownloadServer();
    }

    @Override
    public void onResume() {
	super.onResume();
	SambaExplorer.log(" onResume...");
	FileManagerApp.setLaunchMode(getLocalLaunchMode());
	FileManagerApp.setPasteMode(getLocalPasteMode());
	if (whetherInFileList) {
	    deleteTmpFile();
	}
    }

    private void deleteTmpFile() {
	if (mDeleteTmpFile) {
	    SambaExplorer.log("deleteTmpFile... ");
	    mSmbExplorer.deleteLocalTmpFile();
	    mDeleteTmpFile = false;
	}
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
	super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	super.onCreateOptionsMenu(menu);
	if (FileManagerApp.getLaunchMode() != FileManagerApp.NORMAL_MODE) {
	    return true;
	}
	menu.add(0, MENU_ADD_SERVER, 0, R.string.menu_add_server)
		.setIcon(android.R.drawable.ic_menu_add).setShortcut('0', 'a');
	menu.add(0, MENU_NEW_FOLDER, 0, R.string.menu_new_folder)
		.setIcon(android.R.drawable.ic_menu_add).setShortcut('0', 'n');
	menu.add(0, MENU_COPYSELECTED, 0, R.string.menu_copy_selected).setIcon(
		R.drawable.ic_menu_copy);
	menu.add(0, MENU_RENAMESELECTED, 0, R.string.menu_rename_selected)
		.setIcon(android.R.drawable.ic_menu_edit);
	menu.add(0, MENU_OPEN_SELECTED, 0, R.string.menu_open_selected)
		.setIcon(R.drawable.ic_menu_view);
	menu.add(0, MENU_INFO_SELECTED, 0, R.string.menu_info_selected)
		.setIcon(android.R.drawable.ic_menu_info_details);
	menu.add(0, MENU_MULTISELECT, 0, R.string.menu_multi_select).setIcon(
		R.drawable.ic_menu_select_items);
	menu.add(0, MENU_SELECTALL, 0, R.string.menu_select_all).setIcon(
		R.drawable.ic_menu_select_items);
	menu.add(0, MENU_UNSELECTALL, 0, R.string.menu_unselect_all).setIcon(
		R.drawable.ic_menu_unselect_items);
	menu.add(0, OPT_MENU_VIEW_SWITCH, 0, R.string.menu_view_grid).setIcon(
		R.drawable.ic_menu_grid_view);
	menu.add(0, OPT_MENU_SORT, 0, R.string.menu_sort_by).setIcon(
		R.drawable.ic_menu_sort_by);
	return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
	super.onPrepareOptionsMenu(menu);
	if (FileManagerApp.getLaunchMode() != FileManagerApp.NORMAL_MODE) {
	    return true;
	}

	if (whetherInFileList) {
	    menu.findItem(MENU_ADD_SERVER).setVisible(false);

	    int type = mSmbExplorer.getCurSmbFileType();
	    if (type == SmbFile.TYPE_FILESYSTEM || type == SmbFile.TYPE_SHARE) {
		menu.findItem(MENU_NEW_FOLDER).setVisible(true);

		// share folder should support multiply select
		if (mSmbExplorer.getSmbSelectMode() == IconifiedTextListAdapter.MULTI_SELECT_MODE) {
		    menu.findItem(MENU_MULTISELECT).setVisible(false);
		    menu.findItem(MENU_SELECTALL).setVisible(true);
		    menu.findItem(MENU_UNSELECTALL).setVisible(true);
		    if (mSmbExplorer.isAllSmbFilesSelected()) {
			menu.findItem(MENU_SELECTALL).setVisible(false);
		    } else {
			menu.findItem(MENU_SELECTALL).setVisible(true);
		    }
		    if (mSmbExplorer.isSomeSmbFileSelected()) {
			menu.findItem(MENU_COPYSELECTED).setVisible(true);
			menu.findItem(MENU_UNSELECTALL).setVisible(true);
		    } else {
			menu.findItem(MENU_COPYSELECTED).setVisible(false);
			menu.findItem(MENU_UNSELECTALL).setVisible(false);
		    }

		    if (mSmbExplorer.isSingleSmbFileSelected()) {
			menu.findItem(MENU_RENAMESELECTED).setVisible(true);
			menu.findItem(MENU_OPEN_SELECTED).setVisible(true);
			menu.findItem(MENU_INFO_SELECTED).setVisible(true);
		    } else {
			menu.findItem(MENU_RENAMESELECTED).setVisible(false);
			menu.findItem(MENU_OPEN_SELECTED).setVisible(false);
			menu.findItem(MENU_INFO_SELECTED).setVisible(false);
		    }
		} else {
		    menu.findItem(MENU_MULTISELECT).setVisible(true);
		    menu.findItem(MENU_SELECTALL).setVisible(false);
		    menu.findItem(MENU_UNSELECTALL).setVisible(false);
		    menu.findItem(MENU_COPYSELECTED).setVisible(false);
		    menu.findItem(MENU_RENAMESELECTED).setVisible(false);
		    menu.findItem(MENU_OPEN_SELECTED).setVisible(false);
		    menu.findItem(MENU_INFO_SELECTED).setVisible(false);
		}
	    } else {
		menu.findItem(MENU_NEW_FOLDER).setVisible(false);

		menu.findItem(MENU_OPEN_SELECTED).setVisible(false);
		menu.findItem(MENU_INFO_SELECTED).setVisible(false);
		menu.findItem(MENU_MULTISELECT).setVisible(false);
		menu.findItem(MENU_SELECTALL).setVisible(false);
		menu.findItem(MENU_UNSELECTALL).setVisible(false);
		menu.findItem(MENU_COPYSELECTED).setVisible(false);
		menu.findItem(MENU_RENAMESELECTED).setVisible(false);
	    }
	    menu.findItem(OPT_MENU_VIEW_SWITCH).setVisible(true);
	    menu.findItem(OPT_MENU_SORT).setVisible(true);
	    if (FileManagerApp.isGridView()) {
		menu.findItem(OPT_MENU_VIEW_SWITCH).setIcon(
			R.drawable.ic_menu_list_view);
		menu.findItem(OPT_MENU_VIEW_SWITCH).setTitle(
			R.string.menu_view_list);
	    } else {
		menu.findItem(OPT_MENU_VIEW_SWITCH).setIcon(
			R.drawable.ic_menu_grid_view);
		menu.findItem(OPT_MENU_VIEW_SWITCH).setTitle(
			R.string.menu_view_grid);
	    }
	} else {
	    if (mSmbGroupLayout.isShown()) {
		menu.findItem(MENU_ADD_SERVER).setVisible(false);
	    } else {
		menu.findItem(MENU_ADD_SERVER).setVisible(true);
	    }
	    menu.findItem(MENU_NEW_FOLDER).setVisible(false);
	    menu.findItem(MENU_OPEN_SELECTED).setVisible(false);
	    menu.findItem(MENU_INFO_SELECTED).setVisible(false);
	    menu.findItem(MENU_MULTISELECT).setVisible(false);
	    menu.findItem(MENU_SELECTALL).setVisible(false);
	    menu.findItem(MENU_UNSELECTALL).setVisible(false);
	    menu.findItem(MENU_COPYSELECTED).setVisible(false);
	    menu.findItem(MENU_RENAMESELECTED).setVisible(false);
	    menu.findItem(OPT_MENU_VIEW_SWITCH).setVisible(false);
	    menu.findItem(OPT_MENU_SORT).setVisible(false);
	}
	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case MENU_ADD_SERVER:
	    mSmbDevice.createNewServerDialog();
	    return true;
	case MENU_NEW_FOLDER:
	    mSmbExplorer.createDialog(SambaExplorer.DIALOG_NEW_FOLDER);
	    return true;
	case MENU_COPYSELECTED:
	    ArrayList<String> tmpCopyList = new ArrayList<String>();
	    for (String src : mSmbExplorer.getSmbSelectFiles()) {
		tmpCopyList.add(new File(src).getName());
	    }
	    copy(tmpCopyList);
	    outMultiSelectMode();
	    return true;
	case MENU_RENAMESELECTED:
	    mSmbExplorer
		    .createDialog(SambaExplorer.DIALOG_RENAME_SELECTED_FILE);
	    return true;
	case MENU_OPEN_SELECTED:
	    mSmbExplorer.openSelectedFile(new File(mSmbExplorer
		    .getSmbSelectFiles().get(0)).getName());
	    return true;
	case MENU_INFO_SELECTED:
	    mSmbExplorer.showSelectedFileInfo();
	    return true;
	case MENU_SELECTALL:
	    mSmbExplorer.smbExploreSelectAll();
	    updateSmbMultiPanel();
	    return true;
	case MENU_UNSELECTALL:
	    mSmbExplorer.smbExploreUnSelectAll();
	    updateSmbMultiPanel();
	    return true;
	case MENU_MULTISELECT:
	    setMultiSelectMode();
	    updateSmbMultiPanel();
	    return true;
	case OPT_MENU_VIEW_SWITCH:
	    mSmbExplorer.switchView();
	    return true;
	case OPT_MENU_SORT:
	    showSortMethodDialog();
	    return true;
	case OPT_MENU_SORT_BY_NAME:
	    ((FileManagerApp) getApplication())
		    .setSortMode(FileManagerApp.NAMESORT);
	    mSmbExplorer.browseTo(mSmbExplorer.getCurPath());
	    return true;
	case OPT_MENU_SORT_BY_TYPE:
	    ((FileManagerApp) getApplication())
		    .setSortMode(FileManagerApp.TYPESORT);
	    mSmbExplorer.browseTo(mSmbExplorer.getCurPath());
	    return true;
	case OPT_MENU_SORT_BY_TIME:
	    ((FileManagerApp) getApplication())
		    .setSortMode(FileManagerApp.TIMESORT);
	    mSmbExplorer.browseTo(mSmbExplorer.getCurPath());
	    return true;
	case OPT_MENU_SORT_BY_SIZE:
	    ((FileManagerApp) getApplication())
		    .setSortMode(FileManagerApp.SIZESORT);
	    mSmbExplorer.browseTo(mSmbExplorer.getCurPath());
	    return true;
	}
	return false;
    }

    private void move(ArrayList<String> files) {
	mSmbExplorer.moveFile(files);
    }

    private void copy(ArrayList<String> files) {
	mSmbExplorer.copyFile(files);
    }

    private void setMultiSelectMode() {
	mMultiSelectedPanel.setVisibility(View.VISIBLE);
	mSmbExplorer.setSmbExploreMultiSelectMode();
    }

    private void outMultiSelectMode() {
	mMultiSelectedPanel.setVisibility(View.GONE);
	mSmbExplorer.outSmbExploreMultiSelectMode();
    }

    void updateSmbMultiPanel() {
	if (mSmbExplorer.isSomeSmbFileSelected()) {
	    mMultiSelectedPanel.findViewById(R.id.button_smb_multi_move)
		    .setEnabled(true);
	    mMultiSelectedPanel.findViewById(R.id.button_smb_multi_delete)
		    .setEnabled(true);
	} else {
	    mMultiSelectedPanel.findViewById(R.id.button_smb_multi_move)
		    .setEnabled(false);
	    mMultiSelectedPanel.findViewById(R.id.button_smb_multi_delete)
		    .setEnabled(false);
	}
    }

    public void setSambaPreferences(String key, String value) {
	SharedPreferences.Editor editor = getPreferences(0).edit();
	editor.putString(key, value);
	editor.apply();
	mSmbDevice.updateServerList();
    }

    public String getPrefValueWithKey(String key) {
	SharedPreferences prefs = getPreferences(0);
	return (String) prefs.getAll().get(key);
    }

    public String getPrefKeyWithPrefInfo(String prefInfo) {
	SharedPreferences prefs = this.getPreferences(0);
	Map<String, String> mServerMap = (Map<String, String>) prefs.getAll();
	Iterator<?> it = mServerMap.entrySet().iterator();
	while (it.hasNext()) {
	    Map.Entry e = (Map.Entry) it.next();
	    String tmp = e.getKey().toString();
	    if (tmp.contains(prefInfo)) {
		return tmp;
	    }
	}
	return null;
    }

    public String getPrefValueWithPrefInfo(String prefInfo) {
	SharedPreferences prefs = this.getPreferences(0);
	Map<String, String> mServerMap = (Map<String, String>) prefs.getAll();
	Iterator<?> it = mServerMap.entrySet().iterator();
	while (it.hasNext()) {
	    Map.Entry e = (Map.Entry) it.next();
	    String tmp = e.getValue().toString();
	    if (tmp.contains(prefInfo)) {
		return tmp;
	    }
	}
	return null;
    }

    private boolean mDeleteTmpFile = false;

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
	    Intent intent) {
	SambaExplorer.log("onActivityResult request:" + requestCode
		+ " result:" + resultCode);
	switch (requestCode) {
	case Samba_Explorer_LAUNCH:
	    mRemoteHandler.sendEmptyMessageAtTime(
		    MESSAGE_DELAY_REMOVE_TEMPFILE, 500);
	    break;
	}
    }

    @Override
    protected void onNewIntent(Intent intent) {
	ArrayList<String> filename = null;
	String intentAction = intent.getAction();
	if (intentAction == null) {
	    return;
	}
	if (intentAction.equals(FileManager.ACTION_RESTART_ACTIVITY)) {
	    showServerList();
	} else if (intentAction.equals(FileManagerContent.ACTION_DELETE)) {
	    ArrayList<String> deletFiles = intent
		    .getStringArrayListExtra(FileManagerContent.EXTRA_KEY_SOURCEFILENAME);
	    mSmbExplorer.deletSmbFileNoBrowse(deletFiles);
	} else if (intentAction
		.equals(FileManagerContent.ACTION_TRANSFERFINISHED)) {
	    int transferResult = intent.getIntExtra(
		    FileManagerContent.EXTRA_KEY_TRANSFERRESULT, 0);
	    if (FileManagerContent.EXTRA_VAL_REASONCUT.equals(intent
		    .getStringExtra(FileManagerContent.EXTRA_KEY_PASTEREASON))) {
		mRemoteHandler.obtainMessage(MESSAGE_MOVE_FINISHED,
			transferResult, 0).sendToTarget();
	    } else {
		mRemoteHandler.obtainMessage(MESSAGE_COPY_FINISHED,
			transferResult, 0).sendToTarget();
		mSmbExplorer.browseTo(mSmbExplorer.getCurPath());
	    }
	} else if (intentAction.equals(FileManagerContent.ACTION_PASTE)) {
	    setLocalPasteMode(FileManagerApp.getPasteMode());
	    filename = intent
		    .getStringArrayListExtra(FileManagerContent.EXTRA_KEY_SOURCEFILENAME);
	    if (filename == null) {
		SambaExplorer
			.log("onNewIntent action = ACTION_PASTE, filename = null");
		return;
	    }
	    if ((FileManagerContent.EXTRA_VAL_REASONCUT).equals(intent
		    .getStringExtra(FileManagerContent.EXTRA_KEY_PASTEREASON))) {
		showLoadingDialog(getString(R.string.moving),
			PROGRESS_DIALOG_LOADING_FILE);
	    }
	    // if (filename.get(0).startsWith("smb://")) {
	    // ToDo: samba to samba
	    Intent iDownload = new Intent(DownloadServer.ACTION_DOWNLOAD);
	    iDownload.putStringArrayListExtra(
		    FileManagerContent.EXTRA_KEY_SOURCEFILENAME, filename);
	    iDownload.putExtra(FileManagerContent.EXTRA_KEY_DESTFILENAME,
		    mSmbExplorer.getCurPath());
	    iDownload.putExtra(FileManagerContent.EXTRA_KEY_PASTEREASON, intent
		    .getStringExtra(FileManagerContent.EXTRA_KEY_PASTEREASON));
	    getBaseContext().sendBroadcast(iDownload);
	    /*
	     * } else { // ToDo: upload Intent iDownload = new
	     * Intent(DownloadServer.ACTION_DOWNLOAD);
	     * iDownload.putStringArrayListExtra
	     * (FileManager.EXTRA_KEY_SOURCEFILENAME, filename);
	     * iDownload.putExtra
	     * (FileManager.EXTRA_KEY_DESTFILENAME,mSmbExplorer .getCurPath());
	     * iDownload.putExtra(FileManager.EXTRA_KEY_PASTEREASON,
	     * intent.getStringExtra(FileManager.EXTRA_KEY_PASTEREASON));
	     * getBaseContext().sendBroadcast(iDownload); }
	     */
	}
    }

    private void createLoadDialog(CharSequence msg, int mode) {
	loadingDialog = new ProgressDialog(this);
	loadingDialog.setMessage(msg);
	loadingDialog.setIndeterminate(true);
	loadingDialog.setCancelable(true);
	switch (mode) {
	case PROGRESS_DIALOG_SCANNING_SERVER:
	    loadingDialog.setOnCancelListener(new OnCancelListener() {
		// @Override
		public void onCancel(DialogInterface dialog) {
		    mSmbGroup.disableServerDiscovery();
		}
	    });
	    break;
	case PROGRESS_DIALOG_CONNECTING_SERVER:
	    loadingDialog.setCancelable(false);
	    break;
	}
    }

    private void showLoadingDialog(CharSequence msg, int mode) {
	createLoadDialog(msg, mode);
	if (loadingDialog != null) {
	    loadingDialog.show();
	}
    }

    private void hideLoadingDialog() {
	if (loadingDialog != null) {
	    loadingDialog.dismiss();
	}
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	SambaExplorer.log(TAG + "onKeyDown... ");
	if (keyCode == KeyEvent.KEYCODE_MENU) {
	    if (event != null) {
		if (event.getRepeatCount() > 0) {
		    return true;
		}
	    }
	}
	if (whetherInFileList) {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
		if (mSmbExplorer.getSmbSelectMode() == IconifiedTextListAdapter.MULTI_SELECT_MODE) {
		    outMultiSelectMode();
		} else {
		    if (mSmbExplorer.getPathDepth() > 0) {
			mSmbExplorer.upOneLevel();
		    } else {
			showServerList();
		    }
		}
		return true;
	    }
	} else {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
		if (mSmbGroupLayout.isShown()) {
		    mSmbGroup.upOneLevel();
		    return true;
		}
	    }
	}
	return false;
    }

    private String[] currentDirs;
    private DialogInterface.OnClickListener folderViewDialogListener = new DialogInterface.OnClickListener() {
	// @Override
	public void onClick(DialogInterface dialog, int item) {
	    if (item == 0) {
		finish();
	    } else if (item == 1) {
		showServerList();
	    } else {
		showLoadingDialog(
			mProgressDiagIndicator[PROGRESS_DIALOG_LOADING_FILE],
			PROGRESS_DIALOG_LOADING_FILE);
		mSmbExplorer.browseTo(currentDirs[item - 1], item - 2);
	    }
	    dialog.dismiss();
	}
    };

    private void showFolderViewDialogForGroupList() {
	String[] parts = new String[2];// {"Home","Shared folders",} ;
	parts[0] = this.getString(R.string.home);
	parts[1] = this.getString(R.string.remote_files);
	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	builder.setTitle(R.string.select_folder);
	builder.setSingleChoiceItems(parts, parts.length - 1,
		folderViewDialogListener);
	builder.setNeutralButton(android.R.string.cancel,
		new DialogInterface.OnClickListener() {
		    // @Override
		    public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
		    }
		});
	AlertDialog alert = builder.create();
	alert.show();
    }

    private void showFolderViewDialog() {
	String currentDirectory = mSmbExplorer.getCurPath();
	String[] parts = { this.getString(R.string.home), };
	if (!currentDirectory.equals("smb://")) {
	    currentDirectory = currentDirectory.replace("smb:/", "");
	    parts = currentDirectory.split("/");
	    parts[0] = this.getString(R.string.home);
	}
	currentDirs = new String[parts.length];
	currentDirs[0] = "smb:/";
	for (int i = 1; i < parts.length; i++) {
	    currentDirs[i] = currentDirs[i - 1] + "/" + parts[i];
	}

	String[] parts1 = new String[parts.length + 1];
	parts1[0] = parts[0];
	parts1[1] = this.getString(R.string.remote_files);
	System.arraycopy(parts, 1, parts1, 2, parts.length - 1);

	AlertDialog alert = createSingleChoiceDiag(R.string.select_folder,
		parts1, parts1.length - 1, folderViewDialogListener);
	alert.show();
    }

    private AlertDialog createSingleChoiceDiag(int titleId,
	    CharSequence[] items, int checkedItem,
	    DialogInterface.OnClickListener listener) {
	AlertDialog dialog = null;
	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	if (titleId > 0) {
	    builder.setTitle(titleId);
	}
	builder.setSingleChoiceItems(items, checkedItem, listener);
	builder.setNeutralButton(android.R.string.cancel,
		new DialogInterface.OnClickListener() {
		    // @Override
		    public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
		    }
		});
	dialog = builder.create();
	return dialog;
    }

    private DialogInterface.OnClickListener sortByDialogListener = new DialogInterface.OnClickListener() {
	// @Override
	public void onClick(DialogInterface dialog, int item) {
	    ((FileManagerApp) getApplication())
		    .setSortMode(FileManagerApp.NAMESORT + item);
	    mSmbExplorer.browseTo(mSmbExplorer.getCurPath());
	    dialog.dismiss();
	}
    };

    private void showSortMethodDialog() {
	String[] parts = this.getResources().getStringArray(
		R.array.sort_by_method_selector);
	AlertDialog dialog = createSingleChoiceDiag(0, parts,
		FileManagerApp.getSortMode() - FileManagerApp.NAMESORT,
		sortByDialogListener);
	dialog.show();
    }
}
