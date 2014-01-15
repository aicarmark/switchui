package com.motorola.sdcardbackuprestore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import android.R.array;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.CursorJoiner.Result;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;
import com.motorola.sdcardbackuprestore.BackupRestoreService.NotificationAgent;
import android.content.pm.IPackageInstallObserver;

public class AppManager {
    
private final static String TAG = "App Manager";
    
    private Context mContext = null;
    private NotificationAgent mNotificationAgent = null;
    private volatile boolean mIsWorking = false;
    private volatile boolean mInterrupted = false;
    private int mTotalAppCounter = 0;
    private int mActualAppCounter = 0;
    private boolean mIsAltSdcard;
    private File mApkFolder = null;
    private Constants mConstants = null;
    private ArrayList<ApplicationInfo> mAppList = new ArrayList<ApplicationInfo>();
    PackageManager mPackageManager = null;
    private int mCurrentCounter = 0;
    private ArrayList<File> mFileList = null;
    private boolean mSyncLock = false;
    
    public AppManager(Context context, NotificationAgent notification) {
        mContext = context;
        mNotificationAgent = notification;
        mConstants = new Constants(context);
    }
    
    public boolean getIsWorking() {
        return mIsWorking;
    }
    
    public int getTotalCounter() {
        return mTotalAppCounter;
    }   

    public int getActualCounter() {
        return mActualAppCounter;
    }

    public void setInterrupted(boolean interrupted) {
        mInterrupted = interrupted;
    }
    
    public int BackupApp() {
        Log.v(TAG, "Enter BackupApp()");
        int result = R.id.success;
        if (!mInterrupted) {
            mIsWorking = true;
            result = Backup();
            mIsWorking = false;
        } else {
            result = R.id.cancel;
        }
        return result;
    }
    
    public int RestoreApp(int action, String folderName, String app_folder) {
        Log.v(TAG, "Enter RestoreApp()");
        int res = R.id.success;
        if (!mInterrupted) {
            mIsWorking = true;
            res = Restore(action, folderName, app_folder);
            mIsWorking = false;
        }
        // modified by amt_jiayuanyuan 2012-12-13 SWITCHUITWO-267 begin
        else{
        	 res = R.id.cancel;
        }
        // modified by amt_jiayuanyuan 2012-12-13 SWITCHUITWO-267 end
        return res;
    }
    
    private int Backup() {
        Log.v(TAG, "Enter Backup()");
        int result = R.id.success;
        if (BackupRestoreService.mBackupDest == -1) {
            BackupRestoreService.mBackupDest = new Utility(mContext).getBackupDest();
        }
        if (BackupRestoreService.mBackupDest == Constants.OUTER_STORAGE) {
            mIsAltSdcard = true;
        } else if (BackupRestoreService.mBackupDest == Constants.INNER_STORAGE) {
            mIsAltSdcard = false;
        } else {
            return R.id.no_sdcard;
        }
    	if (!deleteOldBackupFile()) {
    	    Log.e(TAG, "delete old backup apps failed");
    	    return R.id.unknown_error;
    	}
        result = SdcardManager.checkSdcardInitCondition(mContext, mIsAltSdcard, mConstants.DEFAULT_FOLDER);
        if (result != R.id.success) {
            return result;
        }
        File handler;
        if (null == (handler=SdcardManager.createFolder(mContext, mConstants.DEFAULT_FOLDER, mIsAltSdcard))) {
            Log.e(TAG, "Get SD Card unknown error.");
            result = R.id.unknown_error;
            return result;
        }            
        result = backupApp(handler);
        Log.v(TAG, "App backup complete.");
        return result;
    }
    
    private int Restore(int action, String folderName, String app_folder) {
        int res = R.id.success;
        File folder = null;
        if (SdcardManager.noSdcard(mContext)) {
            return R.id.no_sdcard;
        }
        if (action == Constants.RESTORE_ACTION) {
        	  // modified by amt_jiayuanyuan 2012-12-18 SWITCHUITWO-319 begin
            if (isInnerApkFileExist() && !isOuterApkFileExist()) {
                folder = new File(mConstants.SDCARD_PREFIX + "/" + folderName, app_folder);
            } else if ((isOuterApkFileExist() && !isInnerApkFileExist()) || (isOuterApkFileExist() && isInnerApkFileExist())) {
                folder = new File(mConstants.ALT_SDCARD_PREFIX + "/" + folderName, app_folder);
            } else {
                return R.id.empty_storage;
            }
            // modified by amt_jiayuanyuan 2012-12-18 SWITCHUITWO-319 end
        }
        try {
            if (folder.isDirectory()) {
                mFileList = getAppBackupFile(folder);
                mTotalAppCounter = mFileList.size();
                if (0 == mTotalAppCounter) {
                    return R.id.empty_storage;
                }
                res = restoreApp(action, folder);
            } else {
                Log.v(TAG, "Should not be here!");
                res = R.id.unknown_error;
            }
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "out of memory", e);
            return R.id.out_of_memory_error;
        } catch (Exception e) {
            if (e instanceof FileNotFoundException) {
                Log.e(TAG, "FileNotFoundException of an app folder reading in SD card", e);
                return R.id.read_sdcard_error;
            } else if (e instanceof IOException) {
                Log.e(TAG, "Read app folder error", e);
                return R.id.read_sdcard_error;                
            } else if (e instanceof NullPointerException) {
                Log.e(TAG, "No content in the folder", e);
                return R.id.empty_storage;
            } else {
                Log.e(TAG, e.toString());
                return R.id.unknown_error;
            }
        }
        return res;
    }
    
    private int backupApp(File dirHandler) {
        int result = R.id.success;
        int readCount = 1;
        String path = null;
        String fileName = null;
        String subject = null;
        ApplicationInfo appInfo = null;
        getInstallAppList();
        if (null == mAppList) {
            return R.id.read_phone_db_error;
        }
        mTotalAppCounter = mAppList.size();
        mNotificationAgent.sendMsgWithoutNotification(Constants.BACKUP_ACTION,
                Message.obtain(null, R.id.message_length, mTotalAppCounter, Constants.APP_ACTION));
    	mApkFolder = SdcardManager.createFolder(mContext, mConstants.DEFAULT_FOLDER + "/" +Constants.APP_FOLDER, mIsAltSdcard);
        for (int i = 0; !mInterrupted && (i < mTotalAppCounter); i++) {
            appInfo = mAppList.get(i);
            path = appInfo.sourceDir;
            fileName = path.substring(path.lastIndexOf("/") + 1);
            result = copyFile(new File(path), new File(mApkFolder, fileName));
            subject = fileName;
            if (subject == null || subject.equals("")) {
                subject = mContext.getString(R.string.app);
            }
            if ( Constants.APP_DISPLAY_LEN < subject.length() ) {
                subject = subject.substring(0, Constants.APP_DISPLAY_LEN) + mContext.getString(R.string.more_string_display);
            }
            if (result == R.id.success) {
                mNotificationAgent.sendMsgWithoutNotification(Constants.BACKUP_ACTION,
                        Message.obtain(null, R.id.under_progress, readCount, Constants.APP_ACTION,
                            mContext.getString(R.string.backuping, mContext.getString(R.string.app))+"\n\n" + subject));
                NotificationItem item = new NotificationItem();
                item.id = 1;
                item.totalCurrent = readCount;
                item.totalTotal = mTotalAppCounter;
                item.description = mContext.getString(R.string.backuping, mContext.getString(R.string.app));
                item.direction = Constants.BACKUP_ACTION;
                mNotificationAgent.sendNotificationProgress(item);
                readCount++;
            } else {
                Log.v(TAG, "write SD card return error while backup app");
                break;
            }
        }
        mActualAppCounter = readCount - 1;
        if (mInterrupted) {
            result = R.id.cancel;
        }
        return result;
    }
    
    private int restoreApp(int action, File folder) {
        int result = R.id.success;
        Uri uri = null;
        File file = null;
        int installFlags = 0;
        String pn = null;
        PackageInfo packageInfo = null;
        mCurrentCounter = 0;
        mActualAppCounter = 0;
        mPackageManager = mContext.getPackageManager();
        if (mFileList == null || mFileList.size() == 0) {
            return R.id.empty_storage;
        }
        mNotificationAgent.sendMsgWithoutNotification(action,
                Message.obtain(null, R.id.message_length, mTotalAppCounter, Constants.APP_ACTION));
        for (int i = 0; i < mFileList.size() && !mInterrupted; i++) {
            file = mFileList.get(i);
            uri = Uri.fromFile(file);
            packageInfo = mPackageManager.getPackageArchiveInfo(file.getPath(), 0);
            if (packageInfo == null) {
                mCurrentCounter ++;
                mNotificationAgent.sendMsgWithoutNotification(Constants.RESTORE_ACTION,
                        Message.obtain(null, R.id.under_progress, mCurrentCounter, Constants.APP_ACTION, file.getName()));
                continue;
            }
            pn = packageInfo.packageName;
            try {
                PackageInfo pi = mPackageManager.getPackageInfo(pn,
                        PackageManager.GET_UNINSTALLED_PACKAGES);
                if(pi != null) {
                    installFlags |= PackageManager.INSTALL_REPLACE_EXISTING;
                }
            } catch (NameNotFoundException e) {
            }
            PackageInstallObserver observer = new PackageInstallObserver();
            mPackageManager.installPackage(uri, observer, installFlags, pn);
            Log.d("installed package name", pn);
            mSyncLock = true;
            while(mSyncLock){
                synchronized (this) {
                    try {
                        wait(100);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Got interruptedException: ", e);
                    }
                }
            }
        }
        if (mInterrupted) {
            result = R.id.cancel;
        }
        return result;
    }
    
    class PackageInstallObserver extends IPackageInstallObserver.Stub {
        public void packageInstalled(String packageName, int returnCode) {
            if (returnCode == PackageManager.INSTALL_SUCCEEDED) {
                mActualAppCounter ++;
            }
            String mesgBody = mContext.getString(R.string.restoring, mContext.getString(R.string.app)) 
                        + "\n\n" + packageName;
            mCurrentCounter ++;
            mNotificationAgent.sendMsgWithoutNotification(Constants.RESTORE_ACTION,
                    Message.obtain(null, R.id.under_progress, mCurrentCounter, Constants.APP_ACTION, mesgBody));
            NotificationItem item = new NotificationItem();
            item.id = 1;
            item.totalCurrent = mCurrentCounter;
            item.totalTotal = mTotalAppCounter;
            item.description = mContext.getString(R.string.restoring, mContext.getString(R.string.app));
            item.direction = Constants.RESTORE_ACTION;
            mNotificationAgent.sendNotificationProgress(item);
            mSyncLock = false;
        }
    };
    
    private ArrayList<File> getAppBackupFile(File folder) {
        File[] fileList = null;
        ArrayList<File> fileArrayList = new ArrayList<File>();
        if (folder != null && folder.isDirectory()) {
            fileList = folder.listFiles();
        }
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].getName().endsWith(".apk")) {
                fileArrayList.add(fileList[i]);
            }
        }
        return fileArrayList;
    }
    
    private int copyFile(File fromFile, File toFile) {
        FileInputStream from = null;
        FileOutputStream to = null;
        int fileSize = 0;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            fileSize = from.available();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) {
               to.write(buffer, 0, bytesRead); // write
            }
        } catch (Exception e){
            if (checkSdcardIsFull(mIsAltSdcard, fileSize)) {
                 return R.id.insufficient_space;
            } else if (!SdcardManager.isSdcardMounted(mContext, true)) {
                return R.id.no_sdcard;
            } else {
                return R.id.copy_files_failed;
            }
        }
        return R.id.success;
    }
    
    private boolean deleteOldBackupFile() {
        File innerFolder = new File(mConstants.SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER, Constants.APP_FOLDER);
        File outerFolder = new File(mConstants.ALT_SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER, Constants.APP_FOLDER);
        // modified by amt_jiayuanyuan 2012-12-17 SWITCHUITWO-320 begin
        /*if (Utility.deleteDirFile(innerFolder, false, Constants.appExtern) && Utility.deleteDirFile(outerFolder, false, Constants.appExtern)) {
            return true;
        } else {
            return false;
        }*/
        if ( mIsAltSdcard == false ? Utility.deleteDirFile(innerFolder, false, Constants.appExtern) : Utility.deleteDirFile(outerFolder, false, Constants.appExtern)) {
                return true;
            } else {
                return false;
            }
        // modified by amt_jiayuanyuan 2012-12-17 SWITCHUITWO-320 end
        }
    
    private void getInstallAppList() {
        List<ApplicationInfo> ai= mContext.getPackageManager().getInstalledApplications(PackageManager.GET_SHARED_LIBRARY_FILES);
        File folder = new File(Constants.PREINSTALLFILE_PATH);
        File[] fileList = null;
        boolean isPreInstall = false;
        if (folder != null && folder.isDirectory()) {
            fileList = folder.listFiles();
        }
        for (int i = 0; i < ai.size(); i++) {
            isPreInstall = false;
            if ((ai.get(i).flags&ApplicationInfo.FLAG_SYSTEM)==0
                    && (ai.get(i).flags&ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)==0) {
                if (fileList != null && fileList.length != 0) {
                    for (int j = 0; j < fileList.length; j++) {
                        if (fileList[j].getName().equals(ai.get(i).packageName + ".md5")) {
                            isPreInstall = true;
                            break;
                        }
                    }
                }
                if (!isPreInstall) {
                    mAppList.add(ai.get(i));
                }
            }
        }
    }
    
    private boolean isInnerApkFileExist() {
        File innerFolder = new File(mConstants.SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER, Constants.APP_FOLDER);
        if (!innerFolder.exists() || !innerFolder.isDirectory()) {
            return false;
        } else {
            File[] list = innerFolder.listFiles();
            if (list == null) {
                return false;
            } else {
                for (int i = 0; i < list.length; i++) {
                    if (list[i].getName().endsWith(".apk")) {
                        return true;
                    }
                }
                return false;
            }
        }
    }
    
    private boolean isOuterApkFileExist() {
        File outerFolder = new File(mConstants.ALT_SDCARD_PREFIX + "/" + mConstants.DEFAULT_FOLDER, Constants.APP_FOLDER);
        if (!outerFolder.exists() || !outerFolder.isDirectory()) {
            return false;
        } else {
            File[] list = outerFolder.listFiles();
            if (list == null) {
                return false;
            } else {
                for (int i = 0; i < list.length; i++) {
                    if (list[i].getName().endsWith(".apk")) {
                        return true;
                    }
                }
                return false;
            }
        }
    }
    
    private boolean checkSdcardIsFull(boolean isAltSdcard, int fileSize) {
        File sdCard = null;
        if (isAltSdcard) {
            sdCard = new File(mConstants.ALT_SDCARD_PREFIX);
        } else {
            sdCard = new File(mConstants.SDCARD_PREFIX);
        }
        StatFs stat = new StatFs(sdCard.getAbsolutePath());
        if (stat.getBlockSize() * (long) stat.getAvailableBlocks() - fileSize <= 0) {
            Log.d(TAG, "Not enough free space in SD card");
            return true;
        }        
        return false;
    }
    
}
