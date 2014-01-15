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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.filetransfer.FileTransferDialog;
import com.motorola.filemanager.samba.SambaExplorer;
import com.motorola.filemanager.samba.service.AbstractTransferService.OnCanelTransListener;
import com.motorola.filemanager.utils.FileUtils;

public class DownloadServer extends Service implements Runnable, OnCanelTransListener {
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
    private MultiDelayedMsg mCurrTransferItem = null;
    private boolean mIsCancelCurrTransfer = false;

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

    public ServiceState getServiceState() {
        return mServiceState;
    }

    void setServerMode(final ServerMode newmode) {
        mServerMode = newmode;
    }

    ServerMode getServerMode() {
        return mServerMode;
    }

    public ArrayList<String> getCurrTransferSrc() {
        return mSrcPathList;
    }

    public String getCurrTransferDest() {
        return mDestPath;
    }

    public String getCurrPasteReason() {
        return mPasteReason;
    }

    public MultiDelayedMsg getCurrTransferItem() {
        return mCurrTransferItem;
    }

    private Handler mTransferDialogHandler = null;

    public void setTransferDialogHandler(Handler dialogHandler) {
        mTransferDialogHandler = dialogHandler;
    }

    public Handler getTransferDialogHandler() {
        return mTransferDialogHandler;
    }

    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SambaExplorer.log("SambaDownloadServer: " + "received: " + intent.toString(), false);
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(ACTION_DOWNLOAD)) {
                    handleActionDownload(intent);
                } else if (action.equals(ACTION_STOP)) {
                    SambaExplorer.log(TAG + "ACTION_STOP received", false);
                    handleActionStop();
                } else if (action.equals(ACTION_CANCEL)) {
                    SambaExplorer.log(TAG + "ACTION_CANCEL received", false);
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
        mServiceHandler.removeMessages(TRANSFER_DOWN);
        setCancelCurrTransferRequest();
    }

    public void cancelCurrentTransfer(MultiDelayedMsg currTransfer) {
        if (mCurrTransferItem == currTransfer) {
            handleActionCancel();
        }
    }

    public void cancelQueuedTransfer(MultiDelayedMsg currTransfer) {
        if (mQueuedMsg != null) {
            mQueuedMsg.remove(currTransfer);
        }
    }

    private void handleActionDownload(Intent intent) {
        SambaExplorer.log(TAG + "handleDownloadMsg ", false);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            SambaExplorer.log(TAG + "keys: " + extras.keySet(), false);
            addQueueMsg(extras);
            if (getServiceState() == ServiceState.STATE_IDLE) {
                setServiceState(ServiceState.STATE_ACTIVE);
                handleQueueMsg();
            }
        }
    }

    private void handleQueueMsg() {
        if (mQueuedMsg.size() != 0) {
            SambaExplorer.log(TAG + "handleQueueMsg mQueuedMsg: " + mQueuedMsg.size(), false);
            mCurrTransferItem = pollQueueMsg();

            // Message msg = formatTransferMsg(mDestPath,mSrcPath) ;
            if (mCurrTransferItem != null) {
                Message msg = Message.obtain();
                msg.what = mCurrTransferItem.mTransDirection;
                msg.setData(mCurrTransferItem.mMsgData);
                mServiceHandler.sendMessage(msg);
            }
        } else {
            SambaExplorer.log(TAG + "handleQueueMsg: " + "mQueuedMsg empty", false);
            setServiceState(ServiceState.STATE_IDLE);
        }
    }

    public LinkedList<MultiDelayedMsg> getTransferList() {
        return mQueuedMsg;
    }

    private class CountTransferFiles {

        private MultiDelayedMsg mTransferItem;
        private int mFileCount;
        private long mTotalFileSize;
        private LinkedList<SmbFile> mSmbFileCountList = null;
        private LinkedList<File> mLocalFileCountList = null;
        private final static int SMB_FILE_TYPE = 1;
        private final static int LOCAL_FILE_TYPE = 2;
        private final static int UNKNOWN_FILE_TYPE = -1;
        private int mTransferFileType = UNKNOWN_FILE_TYPE;

        public CountTransferFiles(MultiDelayedMsg transferItem) {

            mTransferItem = transferItem;
            mFileCount = 0;
            mTotalFileSize = 0;
            mSmbFileCountList = new LinkedList<SmbFile>();
            mLocalFileCountList = new LinkedList<File>();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (mTransferItem != null) {
                        mTransferFileType = getTransferFileType(mTransferItem);
                        if (mTransferFileType != UNKNOWN_FILE_TYPE) {
                            processSrcList(mTransferItem.mSrcList);
                            processCountList();

                            mTransferItem.setFileCount(mFileCount);
                            mTransferItem.setTotalFileSize(mTotalFileSize);
                            if (mTransferDialogHandler != null) {
                                Message msg = Message.obtain();
                                msg.what = FileTransferDialog.MESSAGE_FILE_COUNT_UPDATE;
                                mTransferDialogHandler.sendMessage(msg);
                            }
                        }
                    }
                }
            }).start();
        }

        private void processCountList() {
            if (mTransferFileType == SMB_FILE_TYPE) {
                processSmbCountList();
            } else {
                processLocalFileCountList();
            }
        }

        private void processLocalFileCountList() {
            File file = mLocalFileCountList.poll();
            while (file != null) {
                mFileCount++;
                if (file.isDirectory()) {
                    addLocalFileChildToList(file);
                } else {
                    long size = file.length();
                    mTotalFileSize = mTotalFileSize + size;
                }
                file = mLocalFileCountList.poll();
            }
        }

        private void processSmbCountList() {
            SmbFile sFile = mSmbFileCountList.poll();
            while (sFile != null) {
                mFileCount++;
                try {
                    if (sFile.isDirectory()) {
                        addSmbChildToList(sFile);
                    } else {
                        long size = sFile.length();
                        mTotalFileSize = mTotalFileSize + size;
                    }
                } catch (SmbException e) {
                    SambaExplorer.log(TAG + "SmbException: processSmbCountList() Failed", true);
                }
                sFile = mSmbFileCountList.poll();
            }
        }

        private void processSrcList(ArrayList<String> srcList) {
            if (srcList != null) {
                for (String file : srcList) {
                    mFileCount++;
                    if (isTransferItemDirectory(file)) {
                        addChildItemsToCountList(file);
                    } else {
                        addTotalFileSize(file);
                    }
                }
            }
        }

        private void addTotalFileSize(String file) {
            if (mTransferFileType == SMB_FILE_TYPE) {
                SmbFile sFile = getSmbFile(file);
                if (sFile != null) {
                    try {
                        long size = sFile.length();
                        mTotalFileSize = mTotalFileSize + size;
                    } catch (SmbException e) {
                        SambaExplorer.log(TAG + "SmbException: length() Failed", true);
                    }
                }
            } else {
                File f = new File(file);
                if (f != null) {
                    long size = f.length();
                    mTotalFileSize = mTotalFileSize + size;
                }
            }
        }

        private void addChildItemsToCountList(String file) {
            if (mTransferFileType == SMB_FILE_TYPE) {
                addSmbChildToList(getSmbFile(file));
            } else {
                addLocalFileChildToList(new File(file));
            }
        }

        private void addLocalFileChildToList(File file) {
            File[] childList = null;
            if (file != null) {
                childList = file.listFiles();
                if (childList != null) {
                    for (File f : childList) {
                        mLocalFileCountList.add(f);
                    }
                }
            }
        }

        private void addSmbChildToList(SmbFile file) {
            SmbFile[] childList = null;
            try {
                if (file != null) {
                    childList = file.listFiles();
                    if (childList != null) {
                        for (SmbFile sfile : childList) {
                            mSmbFileCountList.add(sfile);
                        }
                    }
                }
            } catch (Exception e) {
                SambaExplorer.log(TAG + "Failed to add Smb Child to List", true);
            }
        }

        private boolean isTransferItemDirectory(String file) {
            if (mTransferFileType == SMB_FILE_TYPE) {
                SmbFile sFile = getSmbFile(file);
                if (sFile == null) {
                    return false;
                } else {
                    try {
                        return sFile.isDirectory();
                    } catch (SmbException e) {
                        SambaExplorer.log(TAG + "SmbException: isDirectory() Failed", true);
                        return false;
                    }
                }
            } else {
                File localFile = new File(file);
                return localFile.isDirectory();
            }
        }

        public SmbFile getSmbFile(String fileName) {
            SmbFile f = null;
            try {
                if (SambaTransferHandler.getUserAuth() != null) {
                    f = new SmbFile(fileName, SambaTransferHandler.getUserAuth());
                } else {
                    f = new SmbFile(fileName, NtlmPasswordAuthentication.ANONYMOUS);
                }
            } catch (Exception e) {
                SambaExplorer.log(TAG + "Failed to creat SmbFile", true);
            }
            return f;
        }

        private int getTransferFileType(MultiDelayedMsg item) {
            if (item.mSrcList != null) {
                String firstFile = item.mSrcList.get(0);
                if (firstFile != null) {
                    if (firstFile.startsWith(STR_SAMBA_PREFIX)) {
                        return SMB_FILE_TYPE;
                    } else {
                        return LOCAL_FILE_TYPE;
                    }
                }
            }
            return UNKNOWN_FILE_TYPE;
        }

    }

    public static class MultiDelayedMsg {
        ArrayList<String> mSrcList;
        String mDes;
        Bundle mMsgData;
        NtlmPasswordAuthentication mAuthInfo;
        int mTransDirection;
        String mPasteReason;
        int mFileCount;
        long mTotalFileSize;
        long mNumOfTransferredByte;

        MultiDelayedMsg(ArrayList<String> s, String d, Bundle data, int direction,
                        String pasteReason) {
            mSrcList = s;
            mDes = d;
            mMsgData = data;
            mTransDirection = direction;
            mAuthInfo = SambaTransferHandler.getUserAuth();
            mPasteReason = pasteReason;
            mFileCount = 0;
            mTotalFileSize = 0;
            mNumOfTransferredByte = 0;
        }

        public ArrayList<String> getTransferSrcList() {
            return mSrcList;
        }

        public String getTransferDest() {
            return mDes;
        }

        public String getPasteReason() {
            return mPasteReason;
        }

        public int getTransferDirection() {
            return mTransDirection;
        }

        public void setFileCount(int count) {
            mFileCount = count;
        }

        public int getFileCount() {
            return mFileCount;
        }

        public void setTotalFileSize(long size) {
            mTotalFileSize = size;
        }

        public long getTotalFileSize() {
            return mTotalFileSize;
        }

        public void addNumOfTransferredByte(int byteTransferred) {
            mNumOfTransferredByte = mNumOfTransferredByte + byteTransferred;
        }

        public long getNumOfTransferredByte() {
            return mNumOfTransferredByte;
        }
    }

    private void addQueueMsg(Bundle data) {
        String mDesPath = data.getString(MainFileManagerActivity.EXTRA_KEY_DESTFILENAME);
        ArrayList<String> mSrcList =
                data.getStringArrayList(MainFileManagerActivity.EXTRA_KEY_SOURCEFILENAME);
        String pasteReason = data.getString(MainFileManagerActivity.EXTRA_KEY_PASTEREASON);
        // if(mSrcList!=null){
        // for(int i = 0;i < mSrcList.size();i++){
        // mQueuedMsg.add(new DelayedMsg((String)mSrcList.get(i),mDesPath,data));
        // }
        // }

        if (mSrcList != null && mDesPath != null) {
            String tempSrc = mSrcList.get(0);
            int direction = formatTransferMsgCode(mDesPath, tempSrc);
            MultiDelayedMsg transferItem =
                    new MultiDelayedMsg(mSrcList, mDesPath, data, direction, pasteReason);
            mQueuedMsg.add(transferItem);
            new CountTransferFiles(transferItem);
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

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public DownloadServer getService() {
            return DownloadServer.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        SambaExplorer.log(TAG + "onCreate()", false);
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
        SambaExplorer.log(TAG + "onStart()", false);
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
            case SAMBA_DOWNLOAD :
                enableSamba();
                break;
            case SAMBA_FILE_PICKER :
                SambaExplorer.log(TAG + "SAMBA_FILE_PICKER", false);
                enableSamba();
                handleActionDownload(intent);
                // mServiceHandler.sendEmptyMessage(TRANSFER_STOP);
                break;
            default :
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

    @Override
    public void run() {
        SambaExplorer.log("SambaDownloadServer: " + "tranferHandler.running()", false);
        Looper.prepare();
        mServiceLooper = Looper.myLooper();
        mServiceHandler = new ServiceHandler();
        Looper.loop();
        SambaExplorer.log("SambaDownloadServer: " + "looper().quit()", false);
    }

    private final class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            SambaExplorer.log(TAG + "tranferHandler.received(): " + msg, false);
            WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            WifiLock wl = wm.createWifiLock("moto/samba/transfile");
            AbstractTransferService.TransferResults result;
            // parseMsgData(msg.getData());
            switch (msg.what) {
                case TRANSFER_LOCAL :
                    SambaExplorer.log(TAG + "ServiceHandler TRANSFER_LOCAL", false);
                    // result = mTransfer.localCopy(mDestPath,mSrcPath);
                    // AbstractTransferService.cancelNotification();
                    result = transferLocal();
                    notifyLoadResult(msg.getData(), result);
                    break;

                case TRANSFER_DOWN :
                    SambaExplorer.log(TAG + "ServiceHandler TRANSFER_DOWN", false);
                    wl.acquire();
                    // result = mTransfer.download(mDestPath,mSrcPath);
                    // AbstractTransferService.cancelNotification();
                    result = transferDown();
                    notifyLoadResult(msg.getData(), result);
                    wl.release();
                    break;

                case TRANSFER_UP :
                    SambaExplorer.log(TAG + "ServiceHandler TRANSFER_UP", false);
                    wl.acquire();
                    // result = mTransfer.upload(mDestPath,mSrcPath);
                    // AbstractTransferService.cancelNotification();
                    result = transferUp();
                    notifyLoadResult(msg.getData(), result);
                    wl.release();
                    break;

                case TRANSFER_IN_SERVER :
                    SambaExplorer.log(TAG + "ServiceHandler TRANSFER_IN_SERVER", false);
                    wl.acquire();
                    // result = mTransfer.cpAtServer(mDestPath,mSrcPath);
                    // AbstractTransferService.cancelNotification();
                    result = transferInServer();
                    notifyLoadResult(msg.getData(), result);
                    wl.release();
                    break;

                case TRANSFER_STOP :
                    this.getLooper().quit();
                    return;
            }
            clearCancelCurrTransferRequest(); // Cancel request is for currTransfer only, so always clear it.
            handleQueueMsg();
        }
    }

    private AbstractTransferService.TransferResults transferLocal() {
        AbstractTransferService.TransferResults ret = mTransfer.createTransferResults(TRANSFER_COMPLETE, 0);
        SambaTransferHandler.TransferResults result;
        for (String src : mSrcPathList) {
            if ((MainFileManagerActivity.EXTRA_VAL_REASONCUT).equals(mPasteReason)) {
                // Move case
                result = mTransfer.localMove(mDestPath, src);
            } else {
                result = mTransfer.localCopy(mDestPath, src);
                // AbstractTransferService.cancelNotification();
            }
            if (result.result == TRANSFER_FAIL) {
                ret.result = TRANSFER_FAIL;
            }
            ret.numOfItemsTransferred += result.numOfItemsTransferred;
        }
        if ((MainFileManagerActivity.EXTRA_VAL_REASONCUT).equals(mPasteReason)) {
            String srcPath =
                    (mSrcPathList.get(0)).substring(0, (mSrcPathList.get(0)).lastIndexOf("/"));
            // For Move case only, scan the source directory as well
            if (srcPath != null) {
                FileUtils.mediaScanFolder(this, FileUtils.getFile(srcPath));
            }
        }
        // For both Move & Copy use case, scan the local destination path
        if (mDestPath != null) {
            FileUtils.mediaScanFolder(this, FileUtils.getFile(mDestPath));
        }
        return ret;
    }

    private AbstractTransferService.TransferResults transferDown() {
        AbstractTransferService.TransferResults ret = mTransfer.createTransferResults(TRANSFER_COMPLETE, 0);
        SambaTransferHandler.TransferResults result;
        for (String src : mSrcPathList) {
            if ((MainFileManagerActivity.EXTRA_VAL_REASONCUT).equals(mPasteReason)) {
                // Move case
                result = mTransfer.downloadMove(mDestPath, src);
            } else {
                result = mTransfer.downloadOnly(mDestPath, src);
                // AbstractTransferService.cancelNotification();
            }
            if (result.result == TRANSFER_FAIL) {
                ret.result = TRANSFER_FAIL;
            }
            ret.numOfItemsTransferred += result.numOfItemsTransferred;
        }
        // For both Move & Copy use case, scan the local destination path
        if (mDestPath != null) {
            FileUtils.mediaScanFolder(this, FileUtils.getFile(mDestPath));
        }
        return ret;
    }

    private AbstractTransferService.TransferResults transferUp() {
        AbstractTransferService.TransferResults ret = mTransfer.createTransferResults(TRANSFER_COMPLETE, 0);
        SambaTransferHandler.TransferResults result;
        for (String src : mSrcPathList) {
            if ((MainFileManagerActivity.EXTRA_VAL_REASONCUT).equals(mPasteReason)) {
                // Move case
                result = mTransfer.uploadMove(mDestPath, src);
            } else {
                result = mTransfer.uploadOnly(mDestPath, src);
                // AbstractTransferService.cancelNotification();
            }
            if (result.result == TRANSFER_FAIL) {
                ret.result = TRANSFER_FAIL;
            }
            ret.numOfItemsTransferred += result.numOfItemsTransferred;
        }
        if ((MainFileManagerActivity.EXTRA_VAL_REASONCUT).equals(mPasteReason)) {
            // For Move case only, scan the local source directory
            String srcPath =
                    (mSrcPathList.get(0)).substring(0, (mSrcPathList.get(0)).lastIndexOf("/"));
            // For Move case only, scan the source directory as well
            if (srcPath != null) {
                FileUtils.mediaScanFolder(this, FileUtils.getFile(srcPath));
            }
        }
        return ret;
    }

    private AbstractTransferService.TransferResults transferInServer() {
        AbstractTransferService.TransferResults ret = mTransfer.createTransferResults(TRANSFER_COMPLETE, 0);
        SambaTransferHandler.TransferResults result;
        for (String src : mSrcPathList) {
            if ((MainFileManagerActivity.EXTRA_VAL_REASONCUT).equals(mPasteReason)) {
                // Move case
                result = mTransfer.mvAtServer(mDestPath, src);
            } else {
                result = mTransfer.cpAtServer(mDestPath, src);
                // AbstractTransferService.cancelNotification();
            }
            if (result.result == TRANSFER_FAIL) {
                ret.result = TRANSFER_FAIL;
            }
            ret.numOfItemsTransferred += result.numOfItemsTransferred;
        }
        return ret;
    }

    private void notifyLoadResult(Bundle resultData, AbstractTransferService.TransferResults result) {
        // Execute onComplete of transferHandler
        mTransfer.onComplete();

        Intent intent = new Intent(MainFileManagerActivity.ACTION_TRANSFERFINISHED);
        intent.putExtras(resultData);
        intent.putExtra(MainFileManagerActivity.EXTRA_KEY_TRANSFERRESULT, result.result);
        intent.putExtra(MainFileManagerActivity.EXTRA_KEY_TRANSFERREDNUM, result.numOfItemsTransferred);
        sendBroadcast(intent);
    }

    private static final class Transfer {
        private AbstractTransferService mTransImplement;

        public Transfer(AbstractTransferService service) {
            this.mTransImplement = service;
        }

        public AbstractTransferService.TransferResults localCopy(String destLocal, String srcLocal) {
            return mTransImplement.local2Local(destLocal, srcLocal, false);
        }

        public AbstractTransferService.TransferResults localMove(String destLocal, String srcLocal) {
            String[] fName = null;
            String[] dName = null;
            try {
                fName = srcLocal.split("/");
                dName = destLocal.split("/");
            } catch (Exception e) {
                e.printStackTrace();
                return createTransferResults(TRANSFER_FAIL, 0);
            }

            if (fName[2].equals(dName[2])) { // Files are on same disk
                return mTransImplement.localRename(destLocal, srcLocal);
            } else { // Files are on different disk
                return mTransImplement.local2Local(destLocal, srcLocal, true);
            }
        }

        public AbstractTransferService.TransferResults downloadOnly(String destLocal, String srcRemote) {
            return mTransImplement.remote2Local(destLocal, srcRemote, false);
        }

        public AbstractTransferService.TransferResults downloadMove(String destLocal, String srcRemote) {
            return mTransImplement.remote2Local(destLocal, srcRemote, true);
        }

        public AbstractTransferService.TransferResults uploadOnly(String destRemote, String srcLocal) {
            return mTransImplement.local2Remote(destRemote, srcLocal, false);
        }

        public AbstractTransferService.TransferResults uploadMove(String destRemote, String srcLocal) {
            return mTransImplement.local2Remote(destRemote, srcLocal, true);
        }

        public AbstractTransferService.TransferResults cpAtServer(String destRemote, String srcRemote) {
            return mTransImplement.remote2Remote(destRemote, srcRemote, false);
        }

        public AbstractTransferService.TransferResults mvAtServer(String destRemote, String srcRemote) {
            return mTransImplement.remote2Remote(destRemote, srcRemote, true);
        }

        public void onComplete() {
            mTransImplement.onComplete();
        }

        public AbstractTransferService.TransferResults createTransferResults(int result, int num) {
            AbstractTransferService.TransferResults transferResults = mTransImplement.createTransferResults(result, num);
            return transferResults;
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
            case SAMBA_MODE :
                sPrefix = STR_SAMBA_PREFIX;
                break;
            case FTP_MODE :
                sPrefix = STR_FTP_PREFIX;
                break;
            default :
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
            SambaExplorer.log("formatTransferMsgCode:dest " + dest + ": " + d, false);
        }
        if (src.startsWith(sPrefix)) {
            s = 0x1;
            SambaExplorer.log("formatTransferMsgCode:src " + src + ": " + s, false);
        }
        d = (d << 1) | s;
        SambaExplorer.log("formatTransferMsgCode: " + d + " " + dest + " " + src + " " + sPrefix,
                false);

        switch (d) {
            case 0 :
                what = TRANSFER_LOCAL;
                break;
            case 0x01 :
                what = TRANSFER_DOWN;
                break;
            case 0x02 :
                what = TRANSFER_UP;
                break;
            case 0x03 :
                what = TRANSFER_IN_SERVER;
                break;
        }
        return what;
    }

    private void setCancelCurrTransferRequest() {
        mIsCancelCurrTransfer = true;
    }

    private void clearCancelCurrTransferRequest() {
        mIsCancelCurrTransfer = false;
    }

    @Override
    public boolean isCurrTransferCanceled() {
        return mIsCancelCurrTransfer;
    }
}
