/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 *  Date        CR                Author          Description
 *  2010-03-23  IKSHADOW-2074     E12758          initial
 */
package com.motorola.filemanager.samba;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Looper;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.filemanager.FileManager;
import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.FileManagerContent;
import com.motorola.filemanager.R;
import com.motorola.filemanager.samba.service.SambaTransferHandler;
import com.motorola.filemanager.utils.FileUtils;
import com.motorola.filemanager.utils.IconifiedText;
import com.motorola.filemanager.utils.IconifiedTextListAdapter;
import com.motorola.filemanager.utils.MimeTypeUtil;
import com.motorola.filemanager.networkdiscovery.network.NetInfo;

public class SambaExplorer implements OnItemClickListener,
	OnCreateContextMenuListener {
    private static final String TAG = "SambaExplorer";
    private static final String SAMABA_EXPLORER_DOWNLOAD_ACTION = "com.motorola.filemanager.samba.DOWNLOAD";
    private static final String EXTRA_SAMABA_DOWNLOAD_STATUS = "SambaDownloadStatus";
    private static final String EXTRA_SAMABA_DOWNLOAD_RESULT = "SambaDownloadResult";

    public static final String SMB_FILE_PRIFIX = "smb://";

    private EditText mUsername;
    private EditText mPassword;

    private int mSelectedFilePosition = 0;
    private String mWaitingAuthItem = null;
    private HashMap<String, String> mIpMap = new HashMap<String, String>();

    public static final int DIALOG_NEW_FOLDER = 1;
    private static final int DIALOG_DELETE = 2;
    private static final int DIALOG_RENAME = 3;
    public static final int DIALOG_DELETE_FOLDER = 4;
    public static final int DIALOG_RENAME_FOLDER = 5;
    public static final int DIALOG_DELETE_SELECTED_FILES = 6;
    public static final int DIALOG_RENAME_SELECTED_FILE = 7;
    public static final int DIALOG_FILE_INFO = 8;

    private static final int MENU_OPEN = Menu.FIRST + 1;
    private static final int MENU_DELETE = Menu.FIRST + 2;
    private static final int MENU_RENAME = Menu.FIRST + 3;
    private static final int MENU_COPY = Menu.FIRST + 4;
    private static final int MENU_MOVE = Menu.FIRST + 5;
    private static final int MENU_INFO = Menu.FIRST + 6;

    private static final int MESSAGE_SMB_Exception = 1;
    private static final int MESSAGE_ENTER_NEW_FOLDER_SUCCESS = 2;
    private static final int MESSAGE_FILE_DELETED = 3;
    private static final int MESSAGE_DELETE_FAILED = 4;
    private static final int MESSAGE_RENAME_DUP = 5;
    private static final int MESSAGE_RENAME_ERR = 6;
    private static final int MESSAGE_ERROR_ADDNEW = 7;
    private static final int MESSAGE_RENAME_COMPLETE = 8;
    private static final int MESSAGE_DELETE_FINISHED = 9;
    private static final int MESSAGE_FILE_RENAMED = 10;
    private static final int MESSAGE_FOLDER_RENAMED = 11;
    private static final int MESSAGE_SHOW_FILE_INFO_VIEW = 12;

    private int mSelectMode = IconifiedTextListAdapter.NONE_SELECT_MODE;

    private static final int RESULT_OK = 1;

    private Context mContext = null; // SambaServerList.getSmbContext();
    private Activity mParentActivity;
    private IconifiedTextListAdapter mContendListAdapter;
    private ArrayList<IconifiedText> mListContent = new ArrayList<IconifiedText>();
    private static String mHostPath = null;
    private ProgressDialog mLoadingDialog = null;
    private AbsListView mSmbFileview;
    private AbsListView mSmbFileListView;
    private AbsListView mSmbFileGridView;

    private int mStepsBack = 0;
    private Handler mHandler;
    private int mSmbFileType = 0;
    private String mLaunchFileName;

    private boolean mContextFileIsFolder = false;
    private static final int INVALID_PATH_LEVEL = -100;

    List<IconifiedText> mListDir = new ArrayList<IconifiedText>();
    List<IconifiedText> mListFile = new ArrayList<IconifiedText>();

    private View mInfoView = null;
    private Dialog mInfoDialog = null;

    private String mHostIP = null;
    private Object syncFlag = new Object();

    /*
     * private static class SmbDirectoryContents { List<IconifiedText> listDir;
     * List<IconifiedText> listFile; }
     */

    public SambaExplorer(final Handler handler, final Activity parent) {
	mParentActivity = parent;
	mContext = mParentActivity.getBaseContext();
	mSmbFileListView = (AbsListView) mParentActivity
		.findViewById(R.id.SmbFileList);
	mSmbFileGridView = (AbsListView) mParentActivity
		.findViewById(R.id.SmbFileGrid);
	mContendListAdapter = new IconifiedTextListAdapter(mContext);
	mContendListAdapter.setListItems(mListContent,
		mSmbFileListView.hasTextFilter());
	mSmbFileListView.setAdapter(mContendListAdapter);
	mSmbFileListView.setOnItemClickListener(this);
	mSmbFileListView.setOnCreateContextMenuListener(this);
	mSmbFileListView.setTextFilterEnabled(true);
	mSmbFileGridView.setAdapter(mContendListAdapter);
	mSmbFileGridView.setOnItemClickListener(this);
	mSmbFileGridView.setOnCreateContextMenuListener(this);
	mSmbFileGridView.setTextFilterEnabled(true);
	updateViewMode();
	mHandler = handler;
	mStepsBack = 0;
	loadconfigration();
    }

    public void updateViewMode() {
	if (FileManagerApp.isGridView()) {
	    mSmbFileListView.setVisibility(View.GONE);
	    mSmbFileview = mSmbFileGridView;
	} else {
	    mSmbFileGridView.setVisibility(View.GONE);
	    mSmbFileview = mSmbFileListView;
	}
	mSmbFileview.setVisibility(View.VISIBLE);
    }

    public void switchView() {
	if (FileManagerApp.isGridView()) {
	    ((FileManagerApp) mParentActivity.getApplication())
		    .setGridView(false);
	    mSmbFileGridView.setVisibility(View.GONE);
	    mSmbFileview = mSmbFileListView;
	} else {
	    ((FileManagerApp) mParentActivity.getApplication())
		    .setGridView(true);
	    mSmbFileListView.setVisibility(View.GONE);
	    mSmbFileview = mSmbFileGridView;
	}
	mSmbFileview.setVisibility(View.VISIBLE);
	browseTo(getCurPath());
    }

    private ArrayList<IconifiedText> getAdapterList() {
	return mListContent;
    }

    private void setCurPath(String path) {
	SambaExplorer.log("setCurPath: " + path);
	mHostPath = path;
    }

    public String getCurPath() {
	return mHostPath;
    }

    private void setWaitingAuthItem(String item) {
	SambaExplorer.log("setWaitingAuthItem:  " + item);
	mWaitingAuthItem = item;
    }

    private String getWaitingAuthItem() {
	return mWaitingAuthItem;
    }

    private void setCurSmbFileType(int type) {
	log("setCurSmbFileType : " + getSmbFileType(type));
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

    private void setPathDepth(int depth) {
	log("setPathDepth depth: " + depth);
	mStepsBack = depth;
    }

    BroadcastReceiver myReceiver = new BroadcastReceiver() {
	@Override
	public void onReceive(Context context, Intent intent) {
	    final String action = intent.getAction();
	    log("received broadcast " + action);
	    if (SAMABA_EXPLORER_DOWNLOAD_ACTION.equals(action)) {
		final int downLoadtatus = intent.getIntExtra(
			EXTRA_SAMABA_DOWNLOAD_STATUS, 0);
		if (downLoadtatus == 0) {
		    makeToast(R.string.copy_failed);
		} else {
		    makeToast(intent
			    .getStringExtra(EXTRA_SAMABA_DOWNLOAD_RESULT));
		}
	    }
	}
    };

    private void setSmbBroadcastAddress() {
	String mBroadCastAddress = null;
	try {
	    mBroadCastAddress = ((RemoteFileManager) mParentActivity)
		    .getBroadcastAddress();
	} catch (IOException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}
	jcifs.Config.setProperty("jcifs.netbios.baddr", mBroadCastAddress);
    }

    private void loadconfigration() {
	InputStream stream = mParentActivity.getResources().openRawResource(
		R.raw.jcifs);
	try {
	    jcifs.Config.load(stream);
	    SambaExplorer.log(TAG + "load config successfully");
	} catch (IOException ex) {
	    SambaExplorer.log(TAG + "load jcifs" + ex);
	    ex.printStackTrace();
	}

	NetInfo wifiNetInfo = new NetInfo(mContext);
	String ipAddress = wifiNetInfo.getIp();
	String wlanInterfaceName = wifiNetInfo.getWlanInterface(ipAddress);
	if (wlanInterfaceName == null)
	    return;

	String[] winsIpAddresses = new String[2];
	for (int i = 1; i <= 2; i++) {
	    winsIpAddresses[i - 1] = getSystemProperty("dhcp."
		    + wlanInterfaceName + ".wins" + i);
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
	    jcifs.Config.setProperty("jcifs.resolveOrder",
		    "LMHOSTS,WINS,BCAST,DNS");
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
		log(TAG
			+ "When the process is destroyed, there is one exception: "
			+ ex);
	    }
	}
	return line;
    }

    private void listServerInGroup(String Group) {
	browseTo(formatHostPath(Group), 0);
    }

    private class GetHostIp extends Thread {
	String host = null;

	public GetHostIp(String host) {
	    this.host = host;
	}

	@Override
	public void run() {
	    synchronized (syncFlag) {
		SambaExplorer.log(TAG + " getHostIPTask, doInBackground ");
		try {
		    mHostIP = SambaDeviceList.getHostIP(host);
		} catch (ExceptionInInitializerError e) {
		    SambaExplorer
			    .log(TAG
				    + " getHostIPTask ExceptionInInitializerError "
				    + e);
		} catch (Exception e) {
		    SambaExplorer.log(TAG + " getHostIPTask:  exception " + e);
		} finally {
		    syncFlag.notifyAll();
		}
	    }
	}
    }

    private void listShareFolderInServer(String serverPath) {
	String Host = getIpText(serverPath);
	setWaitingAuthItem(Host);
	SambaExplorer.log(TAG + " listShareFolderInServer synchronized ");
	synchronized (syncFlag) {
	    try {
		SambaExplorer.log(TAG
			+ " listShareFolderInServer run getHostIPTask ");
		GetHostIp getIpOp = new GetHostIp(Host);
		getIpOp.start();
		syncFlag.wait();

		if (mHostIP == null) {
		    log("SambaExplore: listShareFolderInServer, hostIp is null");
		    setSmbExceptionType(2); // temp value 2 for SmbException
		    mExploreMsgHandler
			    .sendMessage(formatTransferMsg(MESSAGE_SMB_Exception));
		    return;
		}
		SambaExplorer.log(TAG + " listShareFolderInServer  hostIP = "
			+ mHostIP);
		mIpMap.put(mHostIP, Host);
		log("stepInServer mIpMap.size() : " + mIpMap.size() + " "
			+ mIpMap.get(mHostIP));

		serverPath = SMB_FILE_PRIFIX + mHostIP + "/";
		SambaExplorer
			.log(TAG
				+ " listShareFolderInServer run getHostIPTask finish, serverPath is : "
				+ serverPath);
		browseTo(formatHostPath(serverPath), 0);
	    } catch (Exception e) {
		log("stepInServer Exception: " + e);
		setSmbExceptionType(2); // temp value 2 for SmbException
		mExploreMsgHandler
			.sendMessage(formatTransferMsg(MESSAGE_SMB_Exception));
		return;
	    }
	}
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
	    Looper.prepare();
	    listShareFolderInServer(mPath);
	    Looper.loop();
	}
    }

    public boolean stepInServer(String serverPath) {
	log("stepInServer input host is :" + serverPath);
	if (serverPath == null) {
	    log("input host is : null");
	    return false;
	} else {
	    setCurPath(SMB_FILE_PRIFIX);
	    if (serverPath.equals(SMB_FILE_PRIFIX)) {
		setSmbBroadcastAddress();
		listServerInGroup(serverPath);
	    } else {
		// listShareFolderInServer(serverPath);
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
	    case MESSAGE_SMB_Exception:
		log("mExploreMsgHandler MESSAGE_SMB_Exception : ");
		hideLoadingDialog();
		mParentActivity.runOnUiThread(showAlertDialogRunnalbe);
		break;
	    case MESSAGE_ENTER_NEW_FOLDER_SUCCESS:
		log("mExploreMsgHandler MESSAGE_ENTER_NEW_FOLDER_SUCCESS : ");
		hideLoadingDialog();
		// ForceUpdate();
		mHandler.sendMessage(formatTransferMsg(RemoteFileManager.MESSAGE_SHOW_FILE_LIST_VIEW));
		break;
	    case MESSAGE_DELETE_FINISHED:
		if (msg.arg1 == MESSAGE_FILE_DELETED) {
		    Toast.makeText(
			    mContext,
			    mContext.getResources().getQuantityString(
				    R.plurals.delete_success, msg.arg2,
				    msg.arg2), Toast.LENGTH_SHORT).show();
		} else {
		    Toast.makeText(mContext, R.string.delet_failed,
			    Toast.LENGTH_SHORT).show();
		}
		break;

	    case MESSAGE_RENAME_DUP:
		Toast.makeText(mContext, R.string.filename_dup,
			Toast.LENGTH_SHORT).show();
		break;
	    case MESSAGE_RENAME_ERR:
		Toast.makeText(mContext, R.string.error_renaming_file,
			Toast.LENGTH_SHORT).show();
		break;
	    case MESSAGE_RENAME_COMPLETE:
		if (msg.arg1 == MESSAGE_FILE_RENAMED) {
		    Toast.makeText(mContext, R.string.file_renamed,
			    Toast.LENGTH_SHORT).show();
		} else {
		    Toast.makeText(mContext, R.string.folder_renamed,
			    Toast.LENGTH_SHORT).show();
		}
		browseTo(getCurPath());
		break;
	    case MESSAGE_SHOW_FILE_INFO_VIEW:
		log("mExploreMsgHandler MESSAGE_SHOW_FILE_INFO_VIEW : ");
		showFileInfodialog();
		break;
	    case MESSAGE_ERROR_ADDNEW:
		makeToast(R.string.error_creating_new_folder);
		break;
	    }
	}
    };

    private Runnable showAlertDialogRunnalbe = new Runnable() {
	// @Override
	public void run() {
	    if ((mParentActivity == null) || mParentActivity.isFinishing()) {
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

    // private class getSmbFileListThread extends Thread {
    // private String mFilePath;
    // private int mPathLevel;
    // private boolean bStepIn = false;
    //
    // public getSmbFileListThread(String file,int pathLevel){
    // mFilePath = file;
    // mPathLevel = pathLevel;
    // bStepIn = true;
    // }
    //
    // public getSmbFileListThread(String file){
    // mFilePath = file;
    // mPathLevel = getPathDepth();
    // bStepIn = false;
    // }
    // @Override
    // public void run() {
    //
    // try {
    // log("getSmbFileListThread run !" );
    // listSmbFiles(mFilePath);
    //
    // if(bStepIn == true){
    // if(getCurSmbFileType()==SmbFile.TYPE_WORKGROUP){
    // setPathDepth(mPathLevel);
    // setCurPath(SMB_FILE_PRIFIX);
    // }else if(getCurSmbFileType()==SmbFile.TYPE_SERVER){
    // setPathDepth(mPathLevel);
    // setCurPath(mFilePath);
    // savePreferencServer(mFilePath);
    // }else{
    // setPathDepth(mPathLevel);
    // setCurPath(mFilePath);
    // }
    // }
    // mExploreMsgHandler.sendMessage(formatTransferMsg(MESSAGE_ENTER_NEW_FOLDER_SUCCESS));
    //
    // } catch (SmbAuthException e) {
    // log("SmbAuthException !" + e + " " + e.getMessage());
    // setSmbExceptionType(1); //temp value 1 for SmbAuthException
    // mExploreMsgHandler.sendMessage(formatTransferMsg(MESSAGE_SMB_Exception));
    // } catch (SmbException e) {
    // log("SmbException! " + e);
    // setSmbExceptionType(2); //temp value 2 for SmbException
    // mExploreMsgHandler.sendMessage(formatTransferMsg(MESSAGE_SMB_Exception));
    // }catch(Exception e){
    // log("Exception! " + e);
    // setSmbExceptionType(2); //temp value 2 for SmbException
    // mExploreMsgHandler.sendMessage(formatTransferMsg(MESSAGE_SMB_Exception));
    // }
    //
    // }
    // }

    private synchronized void listSmbFiles(String filePath) throws SmbException {
	SmbFile f = getSmbFile(filePath);
	if (f == null) {
	    log("SmbFile is null in listSmbFiles");
	    return;
	}

	log("in listSmbFiles " + f);
	SmbFile[] l = null;
	l = f.listFiles();
	if (l != null) {
	    log(" f.listFiles count is: " + l.length);
	}
	// ForceUpdate();
	ArrayList<IconifiedText> listDir = new ArrayList<IconifiedText>();
	ArrayList<IconifiedText> listFile = new ArrayList<IconifiedText>();
	IconifiedText newItem = null;
	Date date = new Date();
	long time;
	long size;
	String infoTime;
	SimpleDateFormat formatter = (SimpleDateFormat) DateFormat
		.getDateInstance(DateFormat.MEDIUM);
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
		String authInfo = ((RemoteFileManager) mParentActivity)
			.getPrefValueWithPrefInfo(serverName);
		newItem.setAuthInfo(authInfo);
		size = Long.MAX_VALUE;
		try {
		    size = l[i].listFiles().length;
		} catch (Exception e) {
		}
		newItem.setSize(size);
		if (size == Long.MAX_VALUE) {
		    newItem.setInfo(mParentActivity.getResources().getString(
			    R.string.unknown));
		} else {
		    newItem.setInfo(size
			    + " "
			    + mParentActivity.getResources().getString(
				    R.string.items));
		}
		listDir.add(newItem);
	    } else {
		newItem = IconifiedText.buildIconItem(mContext, name);
		newItem.setTime(time);
		newItem.setTimeInfo(infoTime);
		size = l[i].length() / 1024;
		newItem.setSize(size);
		newItem.setPathInfo(l[i].getCanonicalPath());
		String serverName = getServerName(getCurPath());
		String authInfo = ((RemoteFileManager) mParentActivity)
			.getPrefValueWithPrefInfo(serverName);
		newItem.setAuthInfo(authInfo);
		// Calculate the file size. If reminder is more than 0 we should
		// show
		// increase the size by 1, otherwise we should show the size as
		// it is.
		long rem = l[i].length() % 1024;
		if (rem > 0) {
		    size++;
		}
		newItem.setInfo(mParentActivity.getResources().getString(
			R.string.size_kb, Long.toString(size)));
		listFile.add(newItem);
	    }
	}

	IconifiedText.sortFiles(listDir, listFile);
	mListDir.clear();
	mListFile.clear();

	mListDir = listDir;
	mListFile = listFile;
    }

    private synchronized void smbContendUpdate() {
	log("smbContendUpdate!");
	mListContent.ensureCapacity(mListDir.size() + mListFile.size());
	mListContent.clear();
	mListContent.addAll(mListDir);
	mListContent.addAll(mListFile);
	mContendListAdapter.setListItems(mListContent,
		mSmbFileview.hasTextFilter());
	mContendListAdapter.notifyDataSetChanged();
    }

    private class listSmbFilesTask extends AsyncTask<String, Integer, Boolean> {
	private String mFilePath;
	private boolean bStepIn = false;
	private int mPathLevel;

	@Override
	protected Boolean doInBackground(String... Paths) {
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
		    log("listSmbFilesTask doInBackground error input!");
		    return false;
		}
		log("listSmbFilesTask doInBackground !" + mFilePath + " "
			+ bStepIn);

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
		log("SmbAuthException !" + e + " " + e.getMessage());
		setSmbExceptionType(1); // temp value 1 for SmbAuthException
		mExploreMsgHandler
			.sendMessage(formatTransferMsg(MESSAGE_SMB_Exception));
		return false;
	    } catch (SmbException e) {
		log("SmbException! " + e);
		setSmbExceptionType(2); // temp value 2 for SmbException
		mExploreMsgHandler
			.sendMessage(formatTransferMsg(MESSAGE_SMB_Exception));
		return false;
	    } catch (Exception e) {
		log("Exception! " + e);
		setSmbExceptionType(2); // temp value 2 for SmbException
		mExploreMsgHandler
			.sendMessage(formatTransferMsg(MESSAGE_SMB_Exception));
		return false;
	    }
	    return true;
	}

	protected void onPostExecute(Boolean result) {
	    if (result) {
		smbContendUpdate();
		mExploreMsgHandler
			.sendMessage(formatTransferMsg(MESSAGE_ENTER_NEW_FOLDER_SUCCESS));
		// ForceUpdate();
	    }
	}
    }

    private void makeReAuthDialog() {
	if (!mParentActivity.isFinishing()) {
	    new AlertDialog.Builder(mParentActivity)
		    .setTitle(R.string.server_logon_failure)
		    .setMessage(R.string.server_logon_failure_indication)
		    .setPositiveButton(android.R.string.ok,
			    new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
					int whichButton) {
				    createReAuthDialog();
				}
			    })
		    .setNegativeButton(android.R.string.cancel,
			    new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog,
					int whichButton) {
				    SambaExplorer
					    .log(" makeReAuthDialog Cancel: "
						    + getSmbFileType(getCurSmbFileType()));
				    if (getCurSmbFileType() == SmbFile.TYPE_SERVER) {
					upCurPath();
					// mHandler.sendMessage(formatTransferMsg(RemoteFileManager.MESSAGE_RETURN_TO_DEVICE));
				    }
				}
			    }).show();
	}
    }

    private void createReAuthDialog() {
	LayoutInflater factory = LayoutInflater.from(mParentActivity);
	final View reauthInfoView = factory.inflate(R.layout.reauth_info, null);

	final Dialog serverEditDialog = new AlertDialog.Builder(mParentActivity)
		.setTitle(R.string.server_reauth_title).setView(reauthInfoView)
		.create();

	mUsername = (EditText) reauthInfoView.findViewById(R.id.Reauthname);
	mPassword = (EditText) reauthInfoView.findViewById(R.id.ReauthPassword);

	Button mAddServerBtn = (Button) reauthInfoView
		.findViewById(R.id.ReauthOk);
	mAddServerBtn.setOnClickListener(new OnClickListener() {
	    public void onClick(View v) {
		if (mUsername.getText().toString().isEmpty()
			|| mPassword.getText().toString().isEmpty()) {
		    // Toast a message
		    Toast.makeText(mContext, R.string.enter_Credentials,
			    Toast.LENGTH_SHORT).show();
		    return;
		}
		addPreferencServer(mUsername.getText().toString(), mPassword
			.getText().toString());
		serverEditDialog.dismiss();
		SambaTransferHandler.provideLoginCredentials(null, mUsername
			.getText().toString(), mPassword.getText().toString());

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

	Button mCancel = (Button) reauthInfoView
		.findViewById(R.id.ReauthCancel);
	mCancel.setOnClickListener(new OnClickListener() {
	    public void onClick(View v) {
		SambaExplorer.log(" createReAuthDialog mCancel: "
			+ getSmbFileType(getCurSmbFileType()));
		if (getCurSmbFileType() == SmbFile.TYPE_SERVER) {
		    upCurPath();
		}
		serverEditDialog.dismiss();
	    }
	});
	serverEditDialog.setOwnerActivity(mParentActivity);
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
	    Looper.prepare();
	    log("browseTo filePath : " + mFilepath);

	    try {
		mFilepath = prepareFileBrowse(mFilepath);
	    } catch (SmbException e1) {
		log("browseTo SmbException : " + mFilepath + e1);
	    }
	    if (mPathLevel != INVALID_PATH_LEVEL) {
		new listSmbFilesTask().execute(mFilepath,
			String.valueOf(mPathLevel));
	    } else {
		new listSmbFilesTask().execute(mFilepath);
	    }
	    Looper.loop();
	}
    }

    public boolean browseTo(String filePath) {
	log("browseTo filePath : " + filePath);
	showLoadingDialog();
	/*
	 * try { filePath = prepareFileBrowse(filePath); } catch (SmbException
	 * e1) { log("browseTo SmbException : " + filePath + e1); return false;
	 * }
	 * 
	 * new listSmbFilesTask().execute(filePath);
	 */
	Browser b = new Browser(filePath);
	b.start();

	return true;
    }

    public boolean browseTo(String filePath, int pathLevel) {
	log("browseTo filePath : " + filePath + " pathLevel " + pathLevel);
	/*
	 * try { filePath = prepareFileBrowse(filePath); } catch (SmbException
	 * e1) { log("browseTo SmbException : " + filePath + e1); return false;
	 * } new listSmbFilesTask().execute(filePath,
	 * String.valueOf(pathLevel));
	 */
	Browser b = new Browser(filePath, pathLevel);
	b.start();

	return true;
    }

    private String prepareFileBrowse(String filePath) throws SmbException {
	SmbFile f = getSmbFile(filePath);
	if (f == null) {
	    log("SmbFile is null in prepareFileBrowse");
	    return filePath;
	}

	mHandler.sendMessage(formatTransferMsg(RemoteFileManager.MESSAGE_HIDE_MULISELECTED_PANEL));
	// outSmbExploreMultiSelectMode();
	int type = f.getType();
	setCurSmbFileType(type);
	log("prepareFileBrowse " + filePath + " type is: "
		+ getSmbFileType(getCurSmbFileType()));

	if (f.isDirectory()) {
	    if (!filePath.endsWith("/")) {
		filePath = filePath + "/";
	    }
	}
	return filePath;
    }

    private Dialog contextDialogBuilder(int titleId, final View dView,
	    String msg, final int id) {
	return new AlertDialog.Builder(mParentActivity)
		.setTitle(titleId)
		.setView(dView)
		.setMessage(msg)
		.setPositiveButton(android.R.string.ok,
			new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog,
				    int whichButton) {
				switch (id) {
				case DIALOG_NEW_FOLDER:
				    final EditText et1 = (EditText) dView
					    .findViewById(R.id.foldername);
				    addNewFolder(et1.getText().toString());
				    break;
				case DIALOG_DELETE:
				    ArrayList<String> dFiles = new ArrayList<String>();
				    dFiles.add(getFilePath(getWaitingAuthItem()));
				    // deletSelectSmbFiles(dFiles);
				    Deleter deleter = new Deleter(dFiles);
				    deleter.start();
				    break;
				case DIALOG_DELETE_FOLDER:
				    deleteFolder();
				    break;
				case DIALOG_DELETE_SELECTED_FILES:
				    ArrayList<String> deletFiles = mContendListAdapter
					    .getSelectFiles();
				    for (int i = 0; i < deletFiles.size(); i++) {
					log("delSelectedFilesDialog: "
						+ deletFiles.get(i));
				    }
				    // deletSelectSmbFiles(deletFiles);
				    Deleter delete = new Deleter(deletFiles);
				    delete.start();
				    break;
				case DIALOG_RENAME:
				    final EditText et2 = (EditText) dView
					    .findViewById(R.id.foldername);
				    String newFileName = getFilePath(et2
					    .getText().toString());
				    renameFileOrFolder(
					    getFilePath(getWaitingAuthItem()),
					    newFileName);
				    break;
				case DIALOG_RENAME_FOLDER:
				    final EditText et5 = (EditText) dView
					    .findViewById(R.id.foldername);
				    renameCurrentFolder(getFilePath(""), et5
					    .getText().toString());
				    break;
				}
			    }
			})
		.setNegativeButton(android.R.string.cancel,
			new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog,
				    int whichButton) {
			    }
			}).create();
    }

    private View inflateDialogView(int resId) {
	return LayoutInflater.from(mParentActivity).inflate(resId, null);
    }

    protected void createDialog(int id) {
	switch (id) {
	case DIALOG_NEW_FOLDER:
	    View view1 = inflateDialogView(R.layout.dialog_new_folder);
	    Dialog newFolderDialog = contextDialogBuilder(
		    R.string.menu_new_folder, view1, null, DIALOG_NEW_FOLDER);
	    newFolderDialog.show();
	    break;
	case DIALOG_DELETE:
	    String delMsg = mContextFileIsFolder ? mParentActivity.getString(
		    R.string.really_delete_folder, getWaitingAuthItem())
		    : mParentActivity.getString(R.string.really_delete,
			    getWaitingAuthItem());
	    Dialog deleteFileDialog = contextDialogBuilder(R.string.delet,
		    null, delMsg, DIALOG_DELETE);
	    deleteFileDialog.show();
	    break;
	case DIALOG_RENAME:
	    View view2 = inflateDialogView(R.layout.dialog_new_folder);
	    final EditText et2 = (EditText) view2.findViewById(R.id.foldername);
	    et2.setText(getWaitingAuthItem());
	    TextView tv = (TextView) view2.findViewById(R.id.foldernametext);
	    if (mContextFileIsFolder) {
		tv.setText(R.string.folder_name);
	    } else {
		tv.setText(R.string.file_name);
	    }
	    Dialog renameFileDialog = contextDialogBuilder(
		    R.string.menu_rename, view2, null, DIALOG_RENAME);
	    renameFileDialog.show();
	    break;
	case DIALOG_RENAME_SELECTED_FILE:
	    View view4 = inflateDialogView(R.layout.dialog_new_folder);
	    final EditText et4 = (EditText) view4.findViewById(R.id.foldername);
	    et4.setText(mContendListAdapter.getSelectFiles().get(0));
	    TextView tv1 = (TextView) view4.findViewById(R.id.foldernametext);
	    if (mContextFileIsFolder) {
		tv1.setText(R.string.folder_name);
	    } else {
		tv1.setText(R.string.file_name);
	    }
	    Dialog renameSelectFileDialog = contextDialogBuilder(
		    R.string.menu_rename, view4, null, DIALOG_RENAME);
	    renameSelectFileDialog.show();
	    break;
	case DIALOG_DELETE_FOLDER:
	    SmbFile file = getSmbFile(getCurPath());
	    if (file != null) {
		Dialog deleteFolderDialog = contextDialogBuilder(
			R.string.menu_rename, null, mParentActivity.getString(
				R.string.really_delete_folder, file.getName()
					.replaceAll("/", "")),
			DIALOG_DELETE_FOLDER);
		deleteFolderDialog.show();
	    } else {
		// File path is not valid. Show error message
		makeToast(R.string.delet_failed);
	    }
	    break;
	case DIALOG_RENAME_FOLDER:
	    file = getSmbFile(getCurPath());
	    if (file != null) {
		View view3 = inflateDialogView(R.layout.dialog_new_folder);
		final EditText et5 = (EditText) view3
			.findViewById(R.id.foldername);
		et5.setText(file.getName().replaceAll("/", ""));
		Dialog renameFolerDialog = contextDialogBuilder(R.string.delet,
			view3, null, DIALOG_RENAME_FOLDER);
		renameFolerDialog.show();
	    } else {
		// File path is not valid. Show error message
		makeToast(R.string.error_renaming_folder);
	    }
	    break;
	case DIALOG_DELETE_SELECTED_FILES:
	    Dialog delSelectedFilesDialog = contextDialogBuilder(
		    R.string.delet, null,
		    mParentActivity.getString(R.string.really_delete_selected),
		    DIALOG_DELETE_SELECTED_FILES);
	    delSelectedFilesDialog.show();
	    break;
	// case DIALOG_SHOW_FILE_INFO:
	// showFileInfo();
	// break;
	}
    }

    public ArrayList<String> getSmbSelectFiles() {
	return mContendListAdapter.getSelectFiles();
    }

    private void showLoadingDialog() {
	mHandler.sendMessage(formatShowProgressDialogMsg(
		RemoteFileManager.MESSAGE_SHOW_LOADING_DIALOG,
		RemoteFileManager.PROGRESS_DIALOG_LOADING_FILE));
    }

    private void hideLoadingDialog() {
	mHandler.sendMessage(formatTransferMsg(RemoteFileManager.MESSAGE_HIDE_LOADING_DIALOG));
    }

    private void renameFileOrFolder(String oldFileName, String newFileName) {
	SmbFile oldFile = getSmbFile(oldFileName);
	SmbFile newFile = getSmbFile(newFileName);
	if ((oldFile == null) || (newFile == null)) {
	    log("SmbFile is null in renameFileorFolder");
	    // Need an error message toast?
	    return;
	}
	/*
	 * if (rename(oldFile, newFile)) { Toast.makeText(mContext,
	 * R.string.file_renamed, Toast.LENGTH_SHORT).show();
	 * browseTo(getCurPath()); }
	 */
	Renamer rename = new Renamer(oldFile, newFile, false);
	rename.start();
    }

    private void renameCurrentFolder(String oldFileName, String newFileName) {
	SmbFile oldFile = getSmbFile(oldFileName);
	SmbFile newFile = null;

	if (oldFile == null) {
	    log("oldFile is null in renameFileorFolder");
	    // Need an error message toast?
	    return;
	}
	newFile = getSmbFile(oldFile.getParent() + newFileName);
	if (newFile == null) {
	    log("newFile is null in renameFileorFolder");
	    // Need an error message toast?
	    return;
	}

	/*
	 * if (rename(oldFile, newFile)) { Toast.makeText(mContext,
	 * R.string.folder_renamed, Toast.LENGTH_SHORT).show();
	 * browseTo(newFile.getCanonicalPath(), getPathDepth()); }
	 */
	Renamer rename = new Renamer(oldFile, newFile, true);
	rename.start();
    }

    /*
     * private boolean rename(SmbFile oldFile, SmbFile newFile) { try { if
     * (newFile.exists()) { log("File name duplicated rename failure ! " +
     * newFile); Toast.makeText(mContext, R.string.filename_dup,
     * Toast.LENGTH_SHORT).show(); return false; } else {
     * oldFile.renameTo(newFile); } return true; } catch (SmbException e) {
     * log("rename SmbException ! " + e); Toast.makeText(mContext,
     * R.string.error_renaming_file, Toast.LENGTH_SHORT).show();
     * 
     * } catch (NotFoundException e) { log("rename NotFoundException ! " + e);
     * Toast.makeText(mContext, R.string.error_renaming_file,
     * Toast.LENGTH_SHORT).show(); } return false; }
     */

    private class Renamer extends Thread {
	SmbFile mOldFile, mNewFile;
	boolean mIsFolder;

	public Renamer(SmbFile oldFile, SmbFile newFile, boolean isFolder) {
	    mOldFile = oldFile;
	    mNewFile = newFile;
	    mIsFolder = isFolder;
	}

	public void run() {
	    try {
		if (mNewFile.exists()) {
		    log("File name duplicated rename failure ! " + mNewFile);
		    mExploreMsgHandler
			    .sendMessage(formatTransferMsg(MESSAGE_RENAME_DUP));
		    return;
		} else {
		    mOldFile.renameTo(mNewFile);
		    if (!mIsFolder) {
			mExploreMsgHandler.obtainMessage(
				MESSAGE_RENAME_COMPLETE, MESSAGE_FILE_RENAMED,
				MESSAGE_FILE_RENAMED).sendToTarget();
		    } else {
			mExploreMsgHandler.obtainMessage(
				MESSAGE_RENAME_COMPLETE,
				MESSAGE_FOLDER_RENAMED, MESSAGE_FOLDER_RENAMED)
				.sendToTarget();
		    }

		}
		return;
	    } catch (SmbException e) {
		log("rename SmbException ! " + e);
		mExploreMsgHandler
			.sendMessage(formatTransferMsg(MESSAGE_RENAME_ERR));

	    } catch (NotFoundException e) {
		log("rename NotFoundException ! " + e);
		mExploreMsgHandler
			.sendMessage(formatTransferMsg(MESSAGE_RENAME_ERR));
	    }
	    return;
	}

    }

    private String getFilePath(String fileName) {
	String seperator = "/";
	StringBuffer buf = new StringBuffer();
	if (getCurPath() != null) {
	    if (getCurPath().endsWith(seperator)) {
		buf.append(getCurPath() + fileName);
	    } else {
		buf.append(getCurPath() + seperator + fileName);
	    }
	}

	String mPath = buf.toString();
	return mPath;
    }

    public SmbFile getSmbFile(String fileName) {
	log("In getSmbFile create SmbFile ! " + fileName);
	SmbFile f = null;
	try {
	    if (SambaTransferHandler.getUserAuth() != null) {
		log("create SmbFile with userAuth: "
			+ SambaTransferHandler.getUserAuth());
		f = new SmbFile(fileName, SambaTransferHandler.getUserAuth());
	    } else {
		log("create SmbFile with ANONYMOUS !"
			+ NtlmPasswordAuthentication.ANONYMOUS);
		f = new SmbFile(fileName, NtlmPasswordAuthentication.ANONYMOUS);
	    }
	} catch (Exception e) {
	    log("In getSmbFile Exception" + e);
	}
	return f;
    }

    private void makeAlertDialog(int resId) {
	new AlertDialog.Builder(mParentActivity)
		.setMessage(resId)
		.setPositiveButton(android.R.string.ok,
			new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog,
				    int whichButton) {
			    }
			}).show();
    }

    private void addPreferencServer(String user, String passwd) {
	SambaExplorer.log("addPreferencServer:user " + user + " passwd "
		+ passwd);
	String serverName = getServerName(getCurPath());
	String tempUrl = formatServerUrl(serverName, user, passwd);
	String tempName = ((RemoteFileManager) mParentActivity)
		.getPrefKeyWithPrefInfo(serverName);
	if (tempName == null) {
	    tempName = serverName;
	}
	((RemoteFileManager) mParentActivity).setSambaPreferences(tempName,
		tempUrl);
	SambaExplorer.log("addPreferencServer url: " + tempUrl);

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

    private String formatServerUrl(String server, String username,
	    String password) {
	if (server == null) {
	    return null;
	}
	String serverurl = SMB_FILE_PRIFIX + username + "\0" + password + "\0"
		+ server;
	SambaExplorer.log("formatServerUrl: " + serverurl);
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
	if (getPathDepth() > 0) {
	    setPathDepth(getPathDepth() - 1);
	}
	upCurPath();
	log("In upOneLevel mHostPath : " + getCurPath());
	browseTo(getCurPath());
    }

    private void upCurPath() {
	StringBuffer buf = new StringBuffer();
	String tempStr = getCurPath();
	if (tempStr.equals(SMB_FILE_PRIFIX)) {
	    setCurPath(tempStr);
	    log("In upCurPath: " + tempStr);
	    return;
	} else {
	    tempStr = tempStr.substring(0, tempStr.length() - 1);
	    buf.append(tempStr.substring(0, tempStr.lastIndexOf('/')));
	    buf.append('/');
	    tempStr = buf.toString();
	    setCurPath(tempStr);
	    log("In upCurPath: " + tempStr);
	}
    }

    public static void log(String msg) {
	FileManagerApp.log(TAG + msg);
    }

    private void makeToast(String msg) {
	Toast.makeText(mParentActivity.getApplicationContext(), msg,
		Toast.LENGTH_SHORT).show();
    }

    private void makeToast(int msgId) {
	Toast.makeText(mParentActivity.getApplicationContext(), msgId,
		Toast.LENGTH_SHORT).show();
    }

    public void onItemClick(AdapterView l, View v, int position, long id) {
	// IconifiedText it = mListContent.get(position);
	if (mContendListAdapter == null) {
	    return;
	}
	IconifiedText it = (IconifiedText) mContendListAdapter
		.getItem(position);
	setWaitingAuthItem(it.getText());
	String mFilePath = getFilePath(getWaitingAuthItem());

	if (mSelectMode == IconifiedTextListAdapter.MULTI_SELECT_MODE) {
	    mSelectedFilePosition = position;
	    mContendListAdapter.setItemChecked(position);
	    ((RemoteFileManager) mParentActivity).updateSmbMultiPanel();
	} else {
	    if (it.getMiMeType().equals(IconifiedText.NONEMIMETYPE)) {
		if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FILE_MODE) {
		    String serverName = getServerName(getCurPath());
		    String authInfo = ((RemoteFileManager) mParentActivity)
			    .getPrefValueWithPrefInfo(serverName);
		    FileManager.rtnPickerResult(mFilePath, authInfo);
		    return;
		} else if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FOLDER_MODE) {
		    return;
		}

		log("onItemClick no application found to open" + it.getText());
		makeToast(R.string.application_not_available);
	    } else {
		log("onListItemClick item is: " + mFilePath);
		FileOpener openFile = new FileOpener(mFilePath);
		openFile.start();
	    }
	}
    }

    private class GetIsDir extends Thread {
	boolean isDir = false;
	SmbFile temp = null;

	public GetIsDir(SmbFile f) {
	    temp = f;
	}

	@Override
	public void run() {
	    synchronized (syncFlag) {
		SambaExplorer.log(TAG + " GetIsDir, doInBackground ");
		try {
		    mContextFileIsFolder = temp.isDirectory();
		} catch (SmbException e) {
		    SambaExplorer.log(TAG + " GetIsDir SmbException " + e);
		} catch (Exception e) {
		    SambaExplorer.log(TAG + " GetIsDir:  exception " + e);
		}
	    }
	}
    }

    OnMenuItemClickListener smbFileMenuItemClickListener = new OnMenuItemClickListener() {
	// @Override
	public boolean onMenuItemClick(MenuItem item) {
	    AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item
		    .getMenuInfo();

	    if (mContendListAdapter == null) {
		log("In onContextItemSelected... " + mContendListAdapter);
		return false;
	    }
	    IconifiedText it = (IconifiedText) mContendListAdapter
		    .getItem(menuInfo.position);
	    setWaitingAuthItem(it.getText());

	    // setWaitingAuthItem(mListContent.get(menuInfo.position).getText());
	    String mFilePath = getFilePath(getWaitingAuthItem());
	    try {
		SmbFile f = getSmbFile(mFilePath);
		if (f != null) {
		    GetIsDir getIsDir = new GetIsDir(f);
		    getIsDir.start();
		} else {
		    log("SmbFile is null in smbFileMenuItemClickListener");
		}
	    } catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }

	    log("selected file is ... " + mFilePath);

	    switch (item.getItemId()) {
	    case MENU_OPEN:
		FileOpener openFile = new FileOpener(mFilePath);
		openFile.start();
		return true;
	    case MENU_MOVE:
		ArrayList<String> mFiles = new ArrayList<String>();
		;
		mFiles.add(getWaitingAuthItem());
		moveFile(mFiles);
		return true;
	    case MENU_COPY:
		log("Copy... " + mFilePath);
		ArrayList<String> cFiles = new ArrayList<String>();
		;
		cFiles.add(getWaitingAuthItem());
		copyFile(cFiles);
		return true;
	    case MENU_DELETE:
		log("Delete... " + mFilePath);
		createDialog(DIALOG_DELETE);
		return true;
	    case MENU_RENAME:
		log("Rename... " + mFilePath);
		createDialog(DIALOG_RENAME);
		return true;
	    case MENU_INFO:
		log("get info... " + mFilePath);
		// showFileInfo(mListContent.get(menuInfo.position));
		// showFileInfodialog(mListContent.get(menuInfo.position));
		createInfodialog(mListContent.get(menuInfo.position));
		InfoLoader infoLoader = new InfoLoader(
			mListContent.get(menuInfo.position));
		infoLoader.start();
		return true;
	    }
	    return false;
	}
    };

    public void onCreateContextMenu(ContextMenu menu, View v,
	    ContextMenuInfo menuInfo) {
	if (FileManagerApp.getLaunchMode() != FileManagerApp.NORMAL_MODE) {
	    return;
	}
	AdapterView.AdapterContextMenuInfo info;
	try {
	    info = (AdapterView.AdapterContextMenuInfo) menuInfo;
	} catch (ClassCastException e) {
	    return;
	}

	if (mContendListAdapter == null) {
	    log("In onContextItemSelected... " + mContendListAdapter);
	    return;
	}
	IconifiedText it = (IconifiedText) mContendListAdapter
		.getItem(info.position);
	setWaitingAuthItem(it.getText());
	String mFilePath = getFilePath(getWaitingAuthItem());
	try {
	    SmbFile f = getSmbFile(mFilePath);
	    if (f != null) {
		GetIsDir getIsDir = new GetIsDir(f);
		getIsDir.start();
	    } else {
		log("SmbFile is null in smbFileContMenuItemClickListener");
	    }
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	int type = getCurSmbFileType();
	if (type == SmbFile.TYPE_FILESYSTEM || type == SmbFile.TYPE_SHARE) {
	    if (mContextFileIsFolder) {
		menu.setHeaderTitle(R.string.context_menu_title_folder);
	    } else {
		menu.setHeaderTitle(R.string.context_menu_title_file);
	    }

	    menu.add(0, MENU_OPEN, 0, R.string.menu_open)
		    .setOnMenuItemClickListener(smbFileMenuItemClickListener);
	    menu.add(0, MENU_COPY, 0, R.string.menu_copy)
		    .setOnMenuItemClickListener(smbFileMenuItemClickListener);
	    menu.add(0, MENU_MOVE, 0, R.string.menu_move)
		    .setOnMenuItemClickListener(smbFileMenuItemClickListener);
	    menu.add(0, MENU_DELETE, 0, R.string.menu_delete)
		    .setOnMenuItemClickListener(smbFileMenuItemClickListener);
	    menu.add(0, MENU_RENAME, 0, R.string.menu_rename)
		    .setOnMenuItemClickListener(smbFileMenuItemClickListener);
	    menu.add(0, MENU_INFO, 0, R.string.menu_info)
		    .setOnMenuItemClickListener(smbFileMenuItemClickListener);
	}
    }

    void showSelectedFileInfo() {
	// showFileInfodialog(mListContent.get(mSelectedFilePosition));
	createInfodialog(mListContent.get(mSelectedFilePosition));
	InfoLoader infoLoader = new InfoLoader(
		mListContent.get(mSelectedFilePosition));
	infoLoader.start();
    }

    private void createInfodialog(IconifiedText item) {

	mInfoView = inflateDialogView(R.layout.file_info);

	mInfoDialog = new AlertDialog.Builder(mParentActivity).setView(
		mInfoView).create();
	mInfoDialog.setTitle(R.string.file_info_title);

	((ImageView) mInfoView.findViewById(R.id.item_info_icon))
		.setImageDrawable(item.getIcon());

	SmbFile file = getSmbFile(getFilePath(item.getText()));

	((TextView) mInfoView.findViewById(R.id.item_name)).setText(file
		.getName());
	((TextView) mInfoView.findViewById(R.id.item_type)).setText(item
		.getTypeDesc(mContext));

	((Button) mInfoView.findViewById(R.id.item_info_ok))
		.setOnClickListener(new View.OnClickListener() {
		    // @Override
		    public void onClick(View v) {
			if (mInfoDialog != null) {
			    mInfoDialog.dismiss();
			}
		    }
		});
	// infoDialog.show();

    }

    private void showFileInfodialog() {
	if (mInfoDialog != null) {
	    mInfoDialog.show();
	}
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

    private void getFileInfo(IconifiedText item) {
	SmbFile file = getSmbFile(getFilePath(item.getText()));

	try {
	    if (file.isDirectory()) {
		mInfoDialog.setTitle(R.string.file_info_title_dir);
		((TextView) mInfoView.findViewById(R.id.item_folder))
			.setText(mParentActivity.getResources().getString(
				R.string.folder)
				+ file.getParent());
		file = getSmbFile(getFilePath(item.getText() + "/"));
		SmbFile[] infolderFiles = file.listFiles();
		((TextView) mInfoView.findViewById(R.id.item_size))
			.setText(mParentActivity.getResources().getString(
				R.string.smb_folder_contains,
				infolderFiles.length));
	    } else {
		((TextView) mInfoView.findViewById(R.id.item_folder))
			.setText(mParentActivity.getResources().getString(
				R.string.folder)
				+ file.getParent());
		String size = "";
		size = Long.toString(file.length() / 1024);
		size = mParentActivity.getResources().getString(
			R.string.size_kb, size);
		((TextView) mInfoView.findViewById(R.id.item_size))
			.setText(mParentActivity.getResources().getString(
				R.string.size)
				+ size);
	    }

	    Date date = new Date(file.lastModified());
	    DateFormat dateFormat = android.text.format.DateFormat
		    .getMediumDateFormat(mContext);
	    DateFormat timeFormat = android.text.format.DateFormat
		    .getTimeFormat(mContext);
	    String format = dateFormat.format(date) + " "
		    + timeFormat.format(date);
	    ((TextView) mInfoView.findViewById(R.id.item_last_modified))
		    .setText(mParentActivity.getResources().getString(
			    R.string.last_modified_at)
			    + format);
	    /*
	     * else {
	     * ((TextView)mInfoView.findViewById(R.id.item_last_modified))
	     * .setText
	     * (mParentActivity.getResources().getString(R.string.last_modified_at
	     * ) + time.toLocaleString()); }
	     */

	    if (file.canRead()) {
		((TextView) mInfoView.findViewById(R.id.read_auth))
			.setText(R.string.readable_yes);
	    } else {
		((TextView) mInfoView.findViewById(R.id.read_auth))
			.setText(R.string.readable_no);
	    }

	    if (file.canWrite()) {
		((TextView) mInfoView.findViewById(R.id.write_auth))
			.setText(R.string.writable_yes);
	    } else {
		((TextView) mInfoView.findViewById(R.id.write_auth))
			.setText(R.string.writable_no);
	    }

	    if (file.isHidden()) {
		((TextView) mInfoView.findViewById(R.id.hidden_status))
			.setText(R.string.hidden_yes);
	    } else {
		((TextView) mInfoView.findViewById(R.id.hidden_status))
			.setText(R.string.hidden_no);
	    }

	} catch (SmbException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    log("SmbException: " + e);
	}
	mExploreMsgHandler
		.sendMessage(formatTransferMsg(MESSAGE_SHOW_FILE_INFO_VIEW));
    }

    private String getSmbFileType(int type) {
	String typeName = null;
	switch (type) {
	case SmbFile.TYPE_FILESYSTEM:
	    typeName = "[TYPE_FILESYSTEM]";
	    break;
	case SmbFile.TYPE_WORKGROUP:
	    typeName = ("[TYPE_WORKGROUP]");
	    break;
	case SmbFile.TYPE_SERVER:
	    typeName = ("[TYPE_SERVER]");
	    break;
	case SmbFile.TYPE_SHARE:
	    typeName = ("[TYPE_SHARE]");
	    break;
	case SmbFile.TYPE_NAMED_PIPE:
	    typeName = ("[TYPE_NAMEDPIPE]");
	    break;
	case SmbFile.TYPE_PRINTER:
	    typeName = ("[TYPE_PRINTER]");
	    break;
	case SmbFile.TYPE_COMM:
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

    private class FileOpener extends Thread {

	String contextFile = null;

	public FileOpener(String file) {
	    contextFile = file;
	}

	public void run() {
	    Looper.prepare();

	    if (contextFile.startsWith(SMB_FILE_PRIFIX)) {
		log("openFile " + contextFile);

		SmbFile file = getSmbFile(contextFile);
		int type = SmbFile.TYPE_SHARE;
		if (file == null) {
		    log("SmbFile is null in openFile");
		    setCurSmbFileType(type);
		    setSmbExceptionType(1); // temp value 1 for SmbAuthException
		    mExploreMsgHandler
			    .sendMessage(formatTransferMsg(MESSAGE_SMB_Exception));
		    return;
		}
		try {
		    type = file.getType();
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
			if (browseTo(contextFile, getPathDepth() + 1)) {
			}
		    } else {
			if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FILE_MODE) {
			    String serverName = getServerName(getCurPath());
			    String authInfo = ((RemoteFileManager) mParentActivity)
				    .getPrefValueWithPrefInfo(serverName);
			    FileManager.rtnPickerResult(
				    file.getCanonicalPath(), authInfo);
			    return;
			} else if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FOLDER_MODE) {
			    return;
			}
			// createLoadingDialog();
			startLoadingFileDialog();
			downloadTmpFileThread thread = new downloadTmpFileThread(
				contextFile);
			thread.start();
		    }
		} catch (SmbAuthException e) {
		    // setSambaServerName(getIpText(contextFile));
		    setCurSmbFileType(type);
		    log("In openFile SmbAuthException !" + e);
		    setSmbExceptionType(1); // temp value 1 for SmbAuthException
		    mExploreMsgHandler
			    .sendMessage(formatTransferMsg(MESSAGE_SMB_Exception));
		} catch (SmbException e) {
		    log("In openFile SmbException" + e);
		    makeToast(R.string.not_found_network_name);
		}
	    }
	    Looper.loop();
	}
    }

    private String provideLoginInfoWithPref(String HostName) {
	log("provideLoginWithPrefInfo:  " + HostName);

	String Host = getIpText(HostName);

	if (mIpMap.containsKey(Host)) {
	    Host = mIpMap.get(Host);
	}

	String tempUrl = ((RemoteFileManager) mParentActivity)
		.getPrefValueWithPrefInfo(Host);
	log("in provideLoginWithPrefInfo getMyPrefValue " + Host + " : "
		+ tempUrl);

	if (tempUrl != null) {
	    SambaTransferHandler.provideLoginCredentials(null,
		    SambaDeviceList.getHostAccount(tempUrl),
		    SambaDeviceList.getHostPwd(tempUrl));
	} else {
	    SambaTransferHandler
		    .setUserAuth(NtlmPasswordAuthentication.ANONYMOUS);
	}

	HostName = SMB_FILE_PRIFIX + Host + "/";
	return HostName;
    }

    private void createLoadingFileDialog() {
	mLoadingDialog = new ProgressDialog(mParentActivity);
	mLoadingDialog.setMessage(mParentActivity.getResources().getString(
		R.string.loading));
	mLoadingDialog.setIndeterminate(true);
	mLoadingDialog.setCancelable(true);
    }

    private void startLoadingFileDialog() {
	createLoadingFileDialog();
	if (mLoadingDialog != null) {
	    mLoadingDialog.show();
	}
    }

    private void stopLoadingFileDialog() {
	if (mLoadingDialog != null) {
	    mLoadingDialog.dismiss();
	}
    }

    private class downloadTmpFileThread extends Thread implements
	    OnCancelListener {
	private volatile boolean mCanceled = false;
	private String path;

	public downloadTmpFileThread(String filePath) {
	    super("downloadTmpFileThread");
	    path = filePath;
	}

	// @Override
	public void run() {
	    while (mLoadingDialog == null) {
		try {
		    Thread.sleep(100);
		    log(" downloadTmpFileThread waiting mLoadingDialog ready!");
		} catch (InterruptedException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
	    mLoadingDialog.setOnCancelListener(this);
	    viewFile(path);
	}

	public void onCancel(DialogInterface dialog) {
	    log(" downloadTmpFileThread onCancel !");
	    mCanceled = true;
	}

	private void viewFile(String filename) {
	    SmbFile file = getSmbFile(filename);
	    if (file == null) {
		log("SmbFile null in viewFile");
		stopLoadingFileDialog();
		// need error message toast?
		return;
	    }

	    setLaunchFileName(file.getName());
	    File desLocal = getLocalTmpFile();
	    if (desLocal == null) {
		log(" downloadTmpFileThread downloadFile failed because it is null!");
		stopLoadingFileDialog();
	    } else if (downloadFile(file, desLocal) == 1) {
		log(" downloadTmpFileThread downloadFile completed !");
		stopLoadingFileDialog();
		launchFile(desLocal);
	    } else {
		log(" downloadTmpFileThread downloadFile failed,delete it !");
		stopLoadingFileDialog();
		if (desLocal.delete() == false) {
		    SambaExplorer.log(TAG + "delete failed: "
			    + desLocal.getName());
		}
	    }
	}

	private int downloadFile(SmbFile remote, File localfile) {
	    if (!localfile.exists()) {
		try {
		    SmbFileInputStream in = new SmbFileInputStream(remote);
		    FileOutputStream out = new FileOutputStream(localfile);

		    byte[] b = new byte[8192];
		    int n, tot = 0;
		    while ((n = in.read(b)) > 0) {
			out.write(b, 0, n);
			tot += n;
			if (mCanceled) {
			    in.close();
			    out.close();
			    if (localfile.delete() == false) {
				FileManagerApp.log(TAG
					+ "Unable to delete file"
					+ localfile.getName());
			    }
			    return 0;
			}
		    }

		    in.close();
		    out.close();
		} catch (Exception e) {
		    SambaExplorer.log(TAG + "download failed: " + e);
		    e.printStackTrace();
		    if (localfile.delete() == false) {
			SambaExplorer.log(TAG + "delete failed: "
				+ localfile.getName());
		    }
		    return 0;
		}

		SambaExplorer.log(TAG + "Downloaded: " + remote.getName());
	    } else {
		SambaExplorer.log(TAG + "cowardly skipping file: "
			+ remote.getName());
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
	String local = ((RemoteFileManager) mParentActivity).getTmpFilePath();
	if (local != null) {
	    log("getLocalTmpFile local path: !" + local);
	    File desLocal = FileUtils.getFile(local, getLaunchFileName());
	    return desLocal;
	} else {
	    return null;
	}
    }

    private void launchFile(File aFile) {
	log("openFile: !" + aFile.getName());
	if (!aFile.exists()) {
	    log("openFile not exist !");
	    return;
	}
	Intent intent = new Intent(android.content.Intent.ACTION_VIEW);

	Uri data = FileUtils.getUri(aFile);
	String type = MimeTypeUtil
		.getMimeType(mParentActivity, aFile.getName());
	intent.setDataAndType(data, type);

	// Were we in GET_CONTENT mode?
	Intent originalIntent = mParentActivity.getIntent();

	if (originalIntent != null) {
	    String originalIntentAction = originalIntent.getAction();
	    if ((originalIntentAction != null)
		    && originalIntentAction.equals(Intent.ACTION_GET_CONTENT)) {
		// In that case, we should probably just return the requested
		// data.
		mParentActivity.setResult(RESULT_OK, intent);
		return;
	    }
	}

	if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_GET_CONTENT) {
	    FileManagerApp.log(TAG
		    + "FileManagerApp.mLaunchMode is SELECT_GET_CONTENT");
	    Intent sIntent = ((FileManagerApp) mParentActivity.getApplication())
		    .getContentIntent();
	    Bundle myExtras = sIntent.getExtras();
	    // special case for handle contact pick picture from files
	    Bitmap mCroppedImage = null;
	    if (myExtras != null
		    && (myExtras.getParcelable("data") != null || myExtras
			    .getBoolean("return-data"))) {
		mCroppedImage = cropImageFromImageFile(aFile.getPath(),
			myExtras);
	    }
	    String packageName = ((FileManagerApp) mParentActivity
		    .getApplication()).getCallingPackageName();
	    FileManagerApp.log(TAG
		    + "SambaExplorer.launchFile: Package Name called us is "
		    + packageName);
	    if (mCroppedImage != null) {
		Bundle extras = new Bundle();
		extras.putParcelable("data", mCroppedImage);
		Intent contactIntent = (new Intent()).setAction("inline-data");
		contactIntent.putExtras(extras);
		FileManager.rtnPickerResult(contactIntent);
	    } else if (packageName.equals("com.android.contacts")) {
		Toast.makeText(
			((RemoteFileManager) mParentActivity).getBaseContext(),
			R.string.crop_Image_error, Toast.LENGTH_SHORT).show();
	    }
	    FileManager.rtnPickerResult(intent);
	    return;
	}
	try {
	    mParentActivity.startActivityForResult(intent,
		    RemoteFileManager.Samba_Explorer_LAUNCH);
	} catch (ActivityNotFoundException e) {
	    log("openFile ActivityNotFoundException: !" + e);
	    deleteLocalTmpFile();
	}
    }

    private Bitmap cropImageFromImageFile(String filePath, Bundle myExtras) {
	BitmapFactory.Options options = new BitmapFactory.Options();
	options.inJustDecodeBounds = true;
	BitmapFactory.decodeFile(filePath, options);
	int totalSize = (options.outHeight) * (options.outWidth) * 2;
	int downSamplingRate = 1;
	while (totalSize > 2 * 1024 * 1024) {
	    downSamplingRate *= 2;
	    totalSize /= 4;
	}
	options = new BitmapFactory.Options();
	options.inJustDecodeBounds = false;
	options.inSampleSize = downSamplingRate;
	options.outHeight = 480;
	options.outWidth = 640;
	Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
	if (bitmap != null) {
	    Bitmap croppedImage = Bitmap.createScaledBitmap(bitmap,
		    myExtras.getInt("outputX", 96),
		    myExtras.getInt("outputY", 96), false);
	    if (croppedImage != null) {
		FileManagerApp.log(TAG + "croppedImage is: "
			+ croppedImage.getHeight());
		return croppedImage;
	    } else {
		FileManagerApp.log(TAG + "croppedImage is null");
	    }
	} else {
	    FileManagerApp.log(TAG + "srcImage is null");
	}
	return null;
    }

    public void deleteLocalTmpFile() {
	File desLocal = getLocalTmpFile();
	if (desLocal != null) {
	    log("deleteLocalTmpFile: !" + desLocal.getName());
	    if (desLocal.exists()) {
		if (desLocal.delete() == false) {
		    SambaExplorer.log(TAG + "Delete failed: "
			    + desLocal.getName());
		}
	    }
	} else {
	    log("deleteLocalTmpFile: LocalTmpFile is null");
	}
    }

    public void deleteFolder() {
	SmbFile curFolder = getSmbFile(getCurPath());
	if (curFolder == null) {
	    log("curFolder is null in deleteFolder. Nothing to delete, exiting function");
	    return;
	}

	try {
	    curFolder.delete();
	} catch (SmbException e) {
	    log("SmbException" + e);
	    makeToast(mParentActivity.getString(R.string.delet_failed,
		    curFolder.getName()));
	    return;
	}
	browseTo(curFolder.getParent(), getPathDepth() - 1);
    }

    public void addNewFolder(String foldername) {
	SmbFile newFolder = getSmbFile(getFilePath(foldername));
	if (newFolder == null) {
	    log("SmbFile is null in addNewFolder");
	    makeToast(R.string.error_creating_new_folder);
	    return;
	}

	/*
	 * try { newFolder.mkdir(); } catch (SmbException e) {
	 * log("SmbException" + e);
	 * makeToast(R.string.error_creating_new_folder); return; }
	 * browseTo(getCurPath());
	 */
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
		log("SmbException" + e);
		mExploreMsgHandler
			.sendMessage(formatTransferMsg(MESSAGE_ERROR_ADDNEW));
		return;
	    }
	    browseTo(getCurPath());
	}
    }

    public boolean deletSmbFileNoBrowse(ArrayList<String> deletFiles) {
	boolean bDelOk = true;
	if (deletFiles != null) {
	    for (String filename : deletFiles) {
		log("deletSelectSmbFiles: " + filename);
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
	    log("targetFile is null in tryDelSmbFile. Exiting function");
	    return false;
	}

	try {
	    if (targetFile.isDirectory()) {
		targetFile = getSmbFile(filename + "/");
		if (targetFile == null) {
		    log("2nd targetFile is null in tryDelSmbFile. Exiting function");
		    return false;
		}
	    }
	    targetFile.delete();
	    // makeToast(mParentActivity.getString(R.string.delet_sucessfully,targetFile.getName()));
	    return true;
	} catch (SmbException e) {
	    log("SmbException" + e);
	    // makeToast(mParentActivity.getString(R.string.delet_failed,targetFile.getName()));
	    return false;
	}
    }

    public class Deleter extends Thread {
	private ArrayList<String> mFiles = null;

	public Deleter(ArrayList<String> file) {
	    mFiles = file;
	}

	public void run() {
	    log("delete filePath : " + mFiles);

	    try {
		deletSelectSmbFiles(mFiles);
	    } catch (Exception e1) {
		log("delete Exception : " + mFiles);
	    }

	}
    }

    /*
     * private void deletSelectSmbFiles(ArrayList<String> deletFiles) { if
     * (deletSmbFileNoBrowse(deletFiles)) { if (deletFiles.size()>1) {
     * makeToast(
     * mParentActivity.getResources().getString(R.string.files_delet_sucessfully
     * )); } else if (mContextFileIsFolder) {
     * makeToast(mParentActivity.getResources
     * ().getString(R.string.folder_delet_sucessfully)); } else {
     * makeToast(mParentActivity
     * .getResources().getString(R.string.file_delet_sucessfully)); }
     * browseTo(getCurPath()); } else {
     * makeToast(mParentActivity.getResources().
     * getString(R.string.delet_failed)); } }
     */
    private void deletSelectSmbFiles(ArrayList<String> deletFiles) {
	if (deletSmbFileNoBrowse(deletFiles)) {
	    mExploreMsgHandler.obtainMessage(MESSAGE_DELETE_FINISHED,
		    MESSAGE_FILE_DELETED, deletFiles.size()).sendToTarget();
	    browseTo(getCurPath());
	} else {
	    mExploreMsgHandler.obtainMessage(MESSAGE_DELETE_FINISHED,
		    MESSAGE_DELETE_FAILED, deletFiles.size()).sendToTarget();
	}
    }

    protected boolean isAllSmbFilesSelected() {
	return mContendListAdapter.isAllFilesSelected(true);
    }

    protected boolean isSomeSmbFileSelected() {
	return mContendListAdapter.isSomeFileSelected();
    }

    protected boolean isSingleSmbFileSelected() {
	return mContendListAdapter.isSingleFileSelected();
    }

    protected void setSmbExploreMultiSelectMode() {
	mSelectMode = IconifiedTextListAdapter.MULTI_SELECT_MODE;
	mContendListAdapter
		.setSelectMode(IconifiedTextListAdapter.MULTI_SELECT_MODE);
    }

    public void outSmbExploreMultiSelectMode() {
	mSelectMode = IconifiedTextListAdapter.NONE_SELECT_MODE;
	mContendListAdapter
		.setSelectMode(IconifiedTextListAdapter.NONE_SELECT_MODE);
	smbExploreUnSelectAll();
    }

    protected void smbExploreSelectAll() {
	mContendListAdapter.selectAll();
    }

    protected void smbExploreUnSelectAll() {
	mContendListAdapter.unSelectAll();
    }

    protected int getSmbSelectMode() {
	return mSelectMode;
    }

    protected void copyFile(ArrayList<String> Files) {
	ArrayList<String> filePaths = getFilesListPath(Files);
	Intent intent = new Intent(mParentActivity,
		com.motorola.filemanager.FileManagerContent.class);
	intent.setAction(FileManagerContent.ACTION_COPY);
	intent.putStringArrayListExtra(
		FileManagerContent.EXTRA_KEY_SOURCEFILENAME, filePaths);
	mHandler.sendMessage(formatTransferMsg(RemoteFileManager.MESSAGE_SET_LOCAL_COPYMODE));
	mParentActivity.startActivity(intent);
    }

    protected void moveFile(ArrayList<String> Files) {
	ArrayList<String> filePaths = getFilesListPath(Files);
	Intent intent = new Intent(mParentActivity,
		com.motorola.filemanager.FileManagerContent.class);
	intent.setAction(FileManagerContent.ACTION_CUT);
	intent.putStringArrayListExtra(
		FileManagerContent.EXTRA_KEY_SOURCEFILENAME, filePaths);
	mHandler.sendMessage(formatTransferMsg(RemoteFileManager.MESSAGE_SET_LOCAL_MOVEMODE));
	mParentActivity.startActivity(intent);
    }

    private ArrayList<String> getFilesListPath(ArrayList<String> Files) {
	ArrayList<String> filePaths = new ArrayList<String>();
	;
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

    private Message formatShowProgressDialogMsg(int msgId, int diagMode) {
	Message msg = Message.obtain();
	Bundle extras = new Bundle();
	extras.putInt(RemoteFileManager.PROGRESS_DIALOG_MODE, diagMode);
	msg.what = msgId;// RemoteFileManager.MESSAGE_LAUNCH_SAMBA_EXPLORE ;
	msg.setData(extras);
	return msg;
    }
}
