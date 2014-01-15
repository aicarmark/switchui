/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 *  Date            CR               Author               Description
 *  2010-03-23  IKSHADOW-2074        E12758                 initial
 */
package com.motorola.filemanager.samba.service;

import java.util.ArrayList;
import java.util.LinkedList;

import jcifs.smb.NtlmPasswordAuthentication;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.motorola.filemanager.FileManagerContent;
import com.motorola.filemanager.samba.SambaExplorer;
import com.motorola.filemanager.samba.service.AbstractTransferService.OnCanelTransListener;

public class DownloadServer extends Service implements Runnable,
	OnCanelTransListener {
    public final static String DOWNLOAD_MODE = "com.motorola.filemanager.DownloadServer";

    public final static int SAMBA_DOWNLOAD = 100;
    public final static int SAMBA_FILE_PICKER = 101;
    public final static String ACTION_DOWNLOAD = "com.motorola.filemanager.samba.download";
    public final static String ACTION_FILE_PICKER = "com.motorola.filemanager.samba.file.picker";

    public final static String ACTION_CANCEL = "com.motorola.filemanager.samba_cancel";
    public final static String ACTION_STOP = "com.motorola.filemanager.samba_stop";

    private final static String TAG = "DownloadServer";

    private ServiceState mServiceState = ServiceState.STATE_IDLE;
    private ServerMode mServerMode = ServerMode.SAMBA_MODE;

    private static Context mContext;

    public final static String STR_SAMBA_PREFIX = "smb://";
    public final static String STR_FTP_PREFIX = "ftp://";

    final static int TRANSFER_DOWN = 1;
    final static int TRANSFER_UP = 2;
    final static int TRANSFER_IN_SERVER = 3;
    final static int TRANSFER_STOP = 4;
    final static int TRANSFER_CANCEL = 5;
    final static int TRANSFER_LOCAL = 6;

    public final static int TRANSFER_FAIL = 0;
    public final static int TRANSFER_COMPLETE = 1;
    public final static int TRANSFER_MULTI_FAIL = 2;
    public final static int TRANSFER_MULTI_COMPLETE = 3;

    private LinkedList<MultiDelayedMsg> mQueuedMsg = null;

    private ArrayList<String> mSrcPathList = null;
    private String mDestPath = null;
    private String mPasteReason = "";

    // protected final Thread mDownloadThread ;

    private Transfer mTransfer;
    private volatile ServiceHandler mServiceHandler;
    private volatile Looper mServiceLooper;

    public enum ServiceState {
	STATE_IDLE, STATE_ACTIVE, STATE_STOP
    }

    public enum ServerMode {
	SAMBA_MODE, FTP_MODE, DEFAULT_MODE
    }

    void setServiceState(ServiceState newstate) {
	mServiceState = newstate;
    }

    ServiceState getServiceState() {
	return mServiceState;
    }

    void setServerMode(final ServerMode newmode) {
	mServerMode = newmode;
    }

    ServerMode getServerMode() {
	return mServerMode;
    }

    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
	@Override
	public void onReceive(Context context, Intent intent) {
	    SambaExplorer.log("SambaDownloadServer: " + "received: "
		    + intent.toString());
	    String action = intent.getAction();
	    if (action != null) {
		if (action.equals(ACTION_DOWNLOAD)) {
		    handleActionDownload(intent);
		} else if (action.equals(ACTION_STOP)) {
		    SambaExplorer.log(TAG + "ACTION_STOP received");
		    handleActionStop();
		} else if (action.equals(ACTION_CANCEL)) {
		    SambaExplorer.log(TAG + "ACTION_CANCEL received");
		    handleActionCancel();
		}
	    }
	}
    };

    private void handleActionStop() {
	setServiceState(ServiceState.STATE_STOP);
	mServiceHandler.sendEmptyMessage(TRANSFER_STOP);
    }

    private void handleActionCancel() {
	setServiceState(ServiceState.STATE_STOP);
	mServiceHandler.removeMessages(TRANSFER_DOWN);
	mServiceHandler.sendEmptyMessage(TRANSFER_CANCEL);
    }

    private void handleActionDownload(Intent intent) {
	SambaExplorer.log(TAG + "handleDownloadMsg ");
	Bundle extras = intent.getExtras();
	if (extras != null) {
	    SambaExplorer.log(TAG + "keys: " + extras.keySet());
	    addQueueMsg(extras);
	    if (getServiceState() == ServiceState.STATE_IDLE) {
		setServiceState(ServiceState.STATE_ACTIVE);
		handleQueueMsg();
	    }
	}
    }

    private void handleQueueMsg() {
	if (mQueuedMsg.size() != 0) {
	    SambaExplorer.log(TAG + "handleQueueMsg mQueuedMsg: "
		    + mQueuedMsg.size());
	    MultiDelayedMsg dMsg = pollQueueMsg();
	    // Message msg = formatTransferMsg(mDestPath,mSrcPath) ;
	    if (dMsg != null) {
		Message msg = Message.obtain();
		msg.what = dMsg.mTransDirection;
		msg.setData(dMsg.mMsgData);
		mServiceHandler.sendMessage(msg);
	    }
	} else {
	    SambaExplorer.log(TAG + "handleQueueMsg: " + "mQueuedMsg empty");
	    setServiceState(ServiceState.STATE_IDLE);
	}
    }

    private static class MultiDelayedMsg {
	ArrayList<String> mSrcList;
	String mDes;
	Bundle mMsgData;
	NtlmPasswordAuthentication mAuthInfo;
	int mTransDirection;
	String mPasteReason;

	MultiDelayedMsg(ArrayList<String> s, String d, Bundle data,
		int direction, String pasteReason) {
	    mSrcList = s;
	    mDes = d;
	    mMsgData = data;
	    mTransDirection = direction;
	    mAuthInfo = SambaTransferHandler.getUserAuth();
	    mPasteReason = pasteReason;
	}
    }

    private void addQueueMsg(Bundle data) {
	String mDesPath = data
		.getString(FileManagerContent.EXTRA_KEY_DESTFILENAME);
	ArrayList<String> mSrcList = data
		.getStringArrayList(FileManagerContent.EXTRA_KEY_SOURCEFILENAME);
	String pasteReason = data
		.getString(FileManagerContent.EXTRA_KEY_PASTEREASON);
	// if(mSrcList!=null){
	// for(int i = 0;i < mSrcList.size();i++){
	// mQueuedMsg.add(new
	// DelayedMsg((String)mSrcList.get(i),mDesPath,data));
	// }
	// }

	if (mSrcList != null && mDesPath != null) {
	    String tempSrc = mSrcList.get(0);
	    int direction = formatTransferMsgCode(mDesPath, tempSrc);
	    mQueuedMsg.add(new MultiDelayedMsg(mSrcList, mDesPath, data,
		    direction, pasteReason));
	}
    }

    private MultiDelayedMsg pollQueueMsg() {
	// DelayedMsg qMsg = null;
	MultiDelayedMsg qMsg = null;
	if (mQueuedMsg.size() != 0) {
	    qMsg = mQueuedMsg.poll();
	    if (qMsg != null) {
		SambaTransferHandler.setDownloadAuth(qMsg.mAuthInfo);
		setDesAndSrc(qMsg.mSrcList, qMsg.mDes);
		mPasteReason = qMsg.mPasteReason;
	    }
	} else {

	}
	return qMsg;
    }

    private void setDesAndSrc(ArrayList<String> src, String Des) {
	mSrcPathList = src;
	mDestPath = Des;
    }

    @Override
    public IBinder onBind(Intent intent) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public void onCreate() {
	SambaExplorer.log(TAG + "onCreate()");
	setmContext(this);
	Thread serviceThread = new Thread(null, this, "Download Service");
	serviceThread.start();

	IntentFilter iFilt = new IntentFilter();
	iFilt.addAction(ACTION_DOWNLOAD);
	iFilt.addAction(ACTION_STOP);
	iFilt.addAction(ACTION_CANCEL);
	registerReceiver(mIntentReceiver, iFilt);
	super.onCreate();
	mQueuedMsg = new LinkedList<MultiDelayedMsg>();
    }

    public static void setmContext(Context cntxt) {
	mContext = cntxt;
    }

    @Override
    public void onStart(Intent intent, int startId) {
	SambaExplorer.log(TAG + "onStart()");
	waitForLooper();
	if (intent == null) {
	    return;
	}

	Bundle args = intent.getExtras();
	if (args == null) {
	    return;
	}
	int mode;
	mode = args.getInt(DOWNLOAD_MODE);
	switch (mode) {
	case SAMBA_DOWNLOAD:
	    enableSamba();
	    break;
	case SAMBA_FILE_PICKER:
	    SambaExplorer.log(TAG + "SAMBA_FILE_PICKER");
	    enableSamba();
	    handleActionDownload(intent);
	    // mServiceHandler.sendEmptyMessage(TRANSFER_STOP);
	    break;
	default:
	    break;
	}
    }

    private void waitForLooper() {
	while (mServiceHandler == null) {
	    synchronized (this) {
		try {
		    wait(100);
		} catch (InterruptedException e) {
		}
	    }
	}
    }

    private void enableSamba() {
	mTransfer = new Transfer(new SambaTransferHandler(mContext, this));
	setServerMode(ServerMode.SAMBA_MODE);
    }

    // @Override
    public void run() {
	SambaExplorer.log("SambaDownloadServer: " + "tranferHandler.running()");
	Looper.prepare();
	mServiceLooper = Looper.myLooper();
	mServiceHandler = new ServiceHandler();
	Looper.loop();
	SambaExplorer.log("SambaDownloadServer: " + "looper().quit()");
    }

    private final class ServiceHandler extends Handler {
	@Override
	public void handleMessage(Message msg) {
	    SambaExplorer.log(TAG + "tranferHandler.received(): " + msg);
	    WifiManager wm = (WifiManager) mContext
		    .getSystemService(Context.WIFI_SERVICE);
	    WifiLock wl = wm.createWifiLock("moto/samba/transfile");
	    int result;
	    // parseMsgData(msg.getData());
	    switch (msg.what) {
	    case TRANSFER_LOCAL:
		SambaExplorer.log(TAG + "ServiceHandler TRANSFER_LOCAL");
		// result = mTransfer.localCopy(mDestPath,mSrcPath);
		// AbstractTransferService.cancelNotification();
		result = transferLocal();
		notifyLoadResult(msg.getData(), result);
		break;

	    case TRANSFER_DOWN:
		SambaExplorer.log(TAG + "ServiceHandler TRANSFER_DOWN");
		wl.acquire();
		// result = mTransfer.download(mDestPath,mSrcPath);
		// AbstractTransferService.cancelNotification();
		result = transferDown();
		notifyLoadResult(msg.getData(), result);
		wl.release();
		break;

	    case TRANSFER_UP:
		SambaExplorer.log(TAG + "ServiceHandler TRANSFER_UP");
		wl.acquire();
		// result = mTransfer.upload(mDestPath,mSrcPath);
		// AbstractTransferService.cancelNotification();
		result = transferUp();
		notifyLoadResult(msg.getData(), result);
		wl.release();
		break;

	    case TRANSFER_IN_SERVER:
		SambaExplorer.log(TAG + "ServiceHandler TRANSFER_IN_SERVER");
		wl.acquire();
		// result = mTransfer.cpAtServer(mDestPath,mSrcPath);
		// AbstractTransferService.cancelNotification();
		result = transferInServer();
		notifyLoadResult(msg.getData(), result);
		wl.release();
		break;

	    case TRANSFER_STOP:
		this.getLooper().quit();
		return;
	    }
	    handleQueueMsg();
	}
    }

    private int transferLocal() {
	int ret = TRANSFER_COMPLETE; // Success code
	int result = TRANSFER_COMPLETE;
	for (String src : mSrcPathList) {
	    if ((FileManagerContent.EXTRA_VAL_REASONCUT).equals(mPasteReason)) {
		// Move case
		result = mTransfer.localMove(mDestPath, src);
	    } else {
		result = mTransfer.localCopy(mDestPath, src);
		// AbstractTransferService.cancelNotification();
	    }
	    if ((result != TRANSFER_COMPLETE)
		    && (result != TRANSFER_MULTI_COMPLETE)
		    && (ret != TRANSFER_MULTI_FAIL)) {
		// If result is (multi) complete, then no need to update message
		// If ret is already set to multi fail, the don't overwrite the
		// message
		ret = result;
	    }
	}
	if ((ret == TRANSFER_FAIL) && mSrcPathList.size() > 1) {
	    // 1 file failed out of multiple attempts. Display the multi fail
	    // message
	    ret = TRANSFER_MULTI_FAIL;
	}
	if ((FileManagerContent.EXTRA_VAL_REASONCUT).equals(mPasteReason)) {
	    // For Move case only, scan the source directory as well
	    ((FileManagerContent) FileManagerContent.getActivityGroup())
		    .mediaScanFolder("file://"
			    + (mSrcPathList.get(0)).substring(0,
				    (mSrcPathList.get(0)).lastIndexOf("/")));
	}
	// For both Move & Copy use case, scan the local destination path
	((FileManagerContent) FileManagerContent.getActivityGroup())
		.mediaScanFolder("file://" + mDestPath);
	return ret;
    }

    private int transferDown() {
	int ret = TRANSFER_COMPLETE; // Success code
	int result = TRANSFER_COMPLETE;
	for (String src : mSrcPathList) {
	    if ((FileManagerContent.EXTRA_VAL_REASONCUT).equals(mPasteReason)) {
		// Move case
		result = mTransfer.downloadMove(mDestPath, src);
	    } else {
		result = mTransfer.downloadOnly(mDestPath, src);
		// AbstractTransferService.cancelNotification();
	    }
	    if ((result != TRANSFER_COMPLETE)
		    && (result != TRANSFER_MULTI_COMPLETE)
		    && (ret != TRANSFER_MULTI_FAIL)) {
		// If result is (multi) complete, then no need to update message
		// If ret is already set to multi fail, the no need to update
		// message
		ret = result;
	    }
	}
	if ((ret == TRANSFER_FAIL) && mSrcPathList.size() > 1) {
	    // 1 file failed out of multiple attempts. Display the multi fail
	    // message
	    ret = TRANSFER_MULTI_FAIL;
	}
	// For both Move & Copy use case, scan the local destination path
	((FileManagerContent) FileManagerContent.getActivityGroup())
		.mediaScanFolder("file://" + mDestPath);
	return ret;
    }

    private int transferUp() {
	int ret = TRANSFER_COMPLETE; // Success code
	int result = TRANSFER_COMPLETE;
	for (String src : mSrcPathList) {
	    if ((FileManagerContent.EXTRA_VAL_REASONCUT).equals(mPasteReason)) {
		// Move case
		result = mTransfer.uploadMove(mDestPath, src);
	    } else {
		result = mTransfer.uploadOnly(mDestPath, src);
		// AbstractTransferService.cancelNotification();
	    }
	    if ((result != TRANSFER_COMPLETE)
		    && (result != TRANSFER_MULTI_COMPLETE)
		    && (ret != TRANSFER_MULTI_FAIL)) {
		// If result is (multi) complete, then no need to update message
		// If ret is already set to multi fail, the no need to update
		// message
		ret = result;
	    }
	}
	if ((ret == TRANSFER_FAIL) && mSrcPathList.size() > 1) {
	    // 1 file failed out of multiple attempts. Display the multi fail
	    // message
	    ret = TRANSFER_MULTI_FAIL;
	}
	if ((FileManagerContent.EXTRA_VAL_REASONCUT).equals(mPasteReason)) {
	    // For Move case only, scan the local source directory
	    ((FileManagerContent) FileManagerContent.getActivityGroup())
		    .mediaScanFolder("file://"
			    + (mSrcPathList.get(0)).substring(0,
				    (mSrcPathList.get(0)).lastIndexOf("/")));
	}
	return ret;
    }

    private int transferInServer() {
	int ret = TRANSFER_COMPLETE; // Success code
	int result = TRANSFER_COMPLETE;
	for (String src : mSrcPathList) {
	    if ((FileManagerContent.EXTRA_VAL_REASONCUT).equals(mPasteReason)) {
		// Move case
		result = mTransfer.mvAtServer(mDestPath, src);
	    } else {
		result = mTransfer.cpAtServer(mDestPath, src);
		// AbstractTransferService.cancelNotification();
	    }
	    if ((result != TRANSFER_COMPLETE)
		    && (result != TRANSFER_MULTI_COMPLETE)
		    && (ret != TRANSFER_MULTI_FAIL)) {
		// If result is (multi) complete, then no need to update message
		// If ret is already set to multi fail, the no need to update
		// message
		ret = result;
	    }
	}
	if ((ret == TRANSFER_FAIL) && mSrcPathList.size() > 1) {
	    // 1 file failed out of multiple attempts. Display the multi fail
	    // message
	    ret = TRANSFER_MULTI_FAIL;
	}
	return ret;
    }

    private void notifyLoadResult(Bundle resultData, int result) {
	// Execute onComplete of transferHandler
	mTransfer.onComplete();

	Intent intent = new Intent(FileManagerContent.ACTION_TRANSFERFINISHED);
	intent.putExtras(resultData);
	intent.putExtra(FileManagerContent.EXTRA_KEY_TRANSFERRESULT, result);
	sendBroadcast(intent);
    }

    private static final class Transfer {
	private AbstractTransferService mTransImplement;

	public Transfer(AbstractTransferService service) {
	    this.mTransImplement = service;
	}

	public int localCopy(String destLocal, String srcLocal) {
	    return mTransImplement.local2Local(destLocal, srcLocal, false);
	}

	public int localMove(String destLocal, String srcLocal) {
	    String[] fName = null;
	    String[] dName = null;
	    try {
		fName = srcLocal.split("/");
		dName = destLocal.split("/");

		if (fName[2].equals(dName[2])) { // Files are on same disk
		    return mTransImplement.localRename(destLocal, srcLocal);
		} else { // Files are on different disk
		    return mTransImplement.local2Local(destLocal, srcLocal,
			    true);
		}
	    } catch (Exception e) {
		e.printStackTrace();
		return TRANSFER_FAIL;
	    }
	}

	public int downloadOnly(String destLocal, String srcRemote) {
	    return mTransImplement.remote2Local(destLocal, srcRemote, false);
	}

	public int downloadMove(String destLocal, String srcRemote) {
	    return mTransImplement.remote2Local(destLocal, srcRemote, true);
	}

	public int uploadOnly(String destRemote, String srcLocal) {
	    return mTransImplement.local2Remote(destRemote, srcLocal, false);
	}

	public int uploadMove(String destRemote, String srcLocal) {
	    return mTransImplement.local2Remote(destRemote, srcLocal, true);
	}

	public int cpAtServer(String destRemote, String srcRemote) {
	    return mTransImplement.remote2Remote(destRemote, srcRemote, false);
	}

	public int mvAtServer(String destRemote, String srcRemote) {
	    return mTransImplement.remote2Remote(destRemote, srcRemote, true);
	}

	public void onComplete() {
	    mTransImplement.onComplete();
	}
    }

    @Override
    public void onDestroy() {
	waitForLooper();
	unregisterReceiver(mIntentReceiver);
	mServiceLooper.quit();
	SambaTransferHandler.setUserAuth(null);
    }

    private String getModePrefix() {
	String sPrefix = null;
	switch (getServerMode()) {
	case SAMBA_MODE:
	    sPrefix = STR_SAMBA_PREFIX;
	    break;
	case FTP_MODE:
	    sPrefix = STR_FTP_PREFIX;
	    break;
	default:
	    break;
	}
	return sPrefix;
    }

    private int formatTransferMsgCode(String dest, String src) {
	int what = 0;
	int d = 0, s = 0;
	String sPrefix = getModePrefix();
	if (sPrefix == null) {
	    return 0;
	}
	if (dest.startsWith(sPrefix)) {
	    d = 0x1;
	    SambaExplorer.log("formatTransferMsgCode:dest " + dest + ": " + d);
	}
	if (src.startsWith(sPrefix)) {
	    s = 0x1;
	    SambaExplorer.log("formatTransferMsgCode:src " + src + ": " + s);
	}
	d = (d << 1) | s;
	SambaExplorer.log("formatTransferMsgCode: " + d + " " + dest + " "
		+ src + " " + sPrefix);

	switch (d) {
	case 0:
	    what = TRANSFER_LOCAL;
	    break;
	case 0x01:
	    what = TRANSFER_DOWN;
	    break;
	case 0x02:
	    what = TRANSFER_UP;
	    break;
	case 0x03:
	    what = TRANSFER_IN_SERVER;
	    break;
	}
	return what;
    }

    // @Override
    public boolean isCanceled() {
	// TODO Auto-generated method stub
	if (mServiceHandler == null) {
	    SambaExplorer.log("mServiceHandler is null ");
	    return false;
	}
	if (mServiceHandler.hasMessages(TRANSFER_CANCEL)) {
	    return true;
	}
	return false;
    }
}
