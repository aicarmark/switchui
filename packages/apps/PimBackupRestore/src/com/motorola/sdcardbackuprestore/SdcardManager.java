package com.motorola.sdcardbackuprestore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;


public class SdcardManager {

    private static final String TAG = "SDCard Manager";
    private static final String MOUNTED = "mounted";
    private static final boolean has_internal_sdcard = false;
    public static final int INNER = 0;
    public static final int OUTTER = 1;
    public static final int UNKNOWN = -1;

    public static String getStoragePath(Context mContext, boolean isAlt) {
        String SD_card_dir = null;
        String SD_card_ext_dir = null;
        try {
            StorageManager sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
            if (sm == null) {
                Log.d(TAG,"storage error -1: get storemanager error");
                return null;
            }
            StorageVolume[] volumes = sm.getVolumeList();
            String path = null;
            for (StorageVolume sv: volumes) {
                path = sv.getPath();
                if (sv.isEmulated() || !sv.isRemovable()) {
                    SD_card_dir = path;
                } else {
                    SD_card_ext_dir = path;
                }
                // modified by amt_jiayuanyuan 2012-12-24 SWITCHUITWO-338 begin
                if((SD_card_dir != null && SD_card_ext_dir != null)){
                	break;
                }
                // modified by amt_jiayuanyuan 2012-12-24 SWITCHUITWO-338 end
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (isAlt) {
            return SD_card_ext_dir;
        } else {
            return SD_card_dir;
        }
    }

    public static boolean isSdcardMounted(Context context, boolean isAltSdcard) {
        if (!SdcardManager.hasInternalSdcard(context)) {
            isAltSdcard = false;
        }
        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        String status = "";
        String storagePath = getStoragePath(context, isAltSdcard);

        if (null == storagePath || 0 == storagePath.length()) {
            Log.d(TAG, "storage path is not valid");
            return false;
        }
        status = sm.getVolumeState(storagePath);
        if (null != status && status.equals(MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }
    
    public static boolean noSdcard(Context context) {
        boolean no_sdcard;
        if (SdcardManager.hasInternalSdcard(context)) {
            no_sdcard = !SdcardManager.isSdcardMounted(context, true) && !SdcardManager.isSdcardMounted(context, false);
        } else {
            no_sdcard = !SdcardManager.isSdcardMounted(context, false);
        }
        return no_sdcard;
    }
        
    public static String getFileString(File file, String charset) throws Exception{
        FileInputStream is = null;
        byte[] data = null;
        if (file.exists()) {
            is = new FileInputStream(file);
            int fileLen = is.available();
            if (fileLen > 0) {
                data = new byte[fileLen];
                is.read(data);
            }
            is.close();
            if (data == null) {
                return "";
            }
            if (charset != null && !charset.equals("")) {
                return new String(data, charset);
            } else {
                return new String(data);
            }
        }
        return "";
    }
    
    public static int saveInSdcard(Context context, File dir, String name, String data, boolean isAltSdcard) {
        if (!SdcardManager.hasInternalSdcard(context)) {
            isAltSdcard = false;
        }
        if (!checkSdcardWritable(context, isAltSdcard)) {
            return R.id.sdcard_read_only;
        }
        if (isSdcardMounted(context, isAltSdcard)) {
            try {
                // Below starts the normal SD card backup of saving file into SD card
                File file=new File(dir, name);
                if(!file.exists()) {file.createNewFile();}
                PrintWriter out1 = new PrintWriter(new FileWriter(file, true), true);
                out1.println(data);
                if (out1.checkError()) {
                    File sdCard = getStorageDirectory(context, isAltSdcard);
                    StatFs stat = new StatFs(sdCard.getAbsolutePath());
                    if (stat.getBlockSize() * ((long) stat.getAvailableBlocks() - 4) <= data.length()) {
                        Log.d(TAG, "insufficient_space, Not enough free space in SD card");
                        return R.id.insufficient_space;
                    }
                    Log.d(TAG, "write_sdcard_error, Not enough free space in SD card");
                    return R.id.insufficient_space;
                }
                new Utility(context).mediaScanFolder(dir);
                new Utility(context).mediaScanFolder(file);
                out1.close();
            } catch (FileNotFoundException e) {
                Log.e(TAG, "FileNotFoundException of a file saving in SD card", e);
                return R.id.write_sdcard_error;
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException of a file saving in SD card", e);
                return R.id.write_sdcard_error;
            } catch (IOException e) {
                Log.e(TAG, "IOException of a file saving in SD card", e);
                return R.id.insufficient_space;
            }
        } else {
            return R.id.no_sdcard;
        }
        return R.id.success;
    }

    public static File createFolder(Context context, String folderName, boolean isAltSdcard) {
        Log.v(TAG, "Enter createFolder()");

        File sdCard = getStorageDirectory(context, isAltSdcard);
        File handle = new File(sdCard, folderName);

        if (handle != null) {
            if (!handle.exists()) {
                handle.mkdir();    
                new Utility(context).mediaScanFolder(handle);
                Log.v(TAG, "Backup directory NOT exists, create new folder status: "
                      + handle.toString());
            } else {
                Log.v(TAG, "Backup directory already exists.");
            }
        }
        return handle;
    }
    
    public static boolean deleteFolder(File dir) {
        Log.d(TAG, "deleteFolder dir="+dir);
        boolean isSuccess;
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children == null) {
                return dir.delete();
            }
            for (int i=0; i<children.length; i++) {
                Log.d(TAG, "deleteFolder [i]="+i);
                Log.d(TAG, "deleteFolder children[i]="+children[i]);
                if (children[i].isDirectory()) {
                    isSuccess = deleteFolder(children[i]);
                    Log.d(TAG, "deleteFolder isDirectory isSuccess="+isSuccess);
                    if (!isSuccess) {
                        return false;
                    }
                    
                } else if (children[i].isFile()) {
                    isSuccess = children[i].delete();
                    Log.d(TAG, "deleteFolder isFile isSuccess="+isSuccess);
                    if (!isSuccess) {
                        return false;
                    }
                }
            }
            isSuccess = dir.delete();
            Log.d(TAG, "deleteFolder dir isSuccess="+isSuccess);
            if (!isSuccess) {
                return false;
            }
         } 
        return true;
         // The directory is now empty so delete it 
         //return dir.delete(); 
     }

    public static Boolean checkSdcardWritable(Context context, boolean isAltSdcard) {
        String path = getStorageDirectory(context, isAltSdcard).getAbsolutePath();
        File file = new File(path);

        if (file != null) {
            return file.canWrite();
        } else {
            return false;
        }
    }

    public static boolean isFolderEmpty(File folder, int action) {
        String suffix = "";
        if (action == Constants.SMS_ACTION) {
            suffix = Constants.vmgExtern;
        } else if (action == Constants.CONTACTS_ACTION) {
            suffix = Constants.vcardExtern;
        }

        File[] fileList = folder.listFiles();
        if ( null == fileList )  return false;
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].getName().endsWith(suffix) == true) {
                Log.v(TAG, "Find same type file on SD card");
                return false;
            }
        }
        return true;
    }

    public static boolean emptyFolder(File folder, int action) {
        String suffix = "";
        if (action == Constants.SMS_ACTION) {
            suffix = Constants.vmgExtern;
        } else if (action == Constants.CONTACTS_ACTION) {
            suffix = Constants.vcardExtern;
        }
        File[] fileList = folder.listFiles();
        if ( null == fileList )  return false;
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].getName().endsWith(suffix) == true) {
                if (!fileList[i].delete()) {
                    return false; // delete file failed
                }
            }
        }
        return true;
    }

    public static int checkSdcardInitCondition(Context context, boolean isAltSdcard, String default_folder) {
        /*
         * There is no phone internal SD card in YangTze products 
         * so set param isAltSdcard false;
         * isAltSdcard = false;
         */
        if (!isSdcardMounted(context, isAltSdcard)) {
            Log.e(TAG, "SD card is not mounted");
            return R.id.no_sdcard;
        }
        if (checkSdcardWritable(context, isAltSdcard) == false) {
            Log.e(TAG, "SD card is read-only");
            return R.id.sdcard_read_only;
        }
        File sdCard = null;
        sdCard = getStorageDirectory(context, isAltSdcard);
        StatFs stat = new StatFs(sdCard.getAbsolutePath());
        if (stat.getBlockSize() * ((long) stat.getAvailableBlocks() - 4) <= 0) {
        Log.d(TAG, "Not enough free space in SD card");
        return R.id.insufficient_space;
    }

        return R.id.success;
    }
    
    public static File getStorageDirectory(Context context, boolean isAltSdcard) {
        if (hasInternalSdcard(context)) {
            return isAltSdcard ? new File(getStoragePath(context, true)) : new File(getStoragePath(context, false));
        } else {
            return new File(getStoragePath(context, false));
        }
    }

    public static int isAltSdcard(Context context, String path) {
        if (path != null && path.startsWith(getStoragePath(context, true))) {
            return OUTTER;
        } else if (path != null && path.startsWith(getStoragePath(context, false))) {
            if (hasInternalSdcard(context)) {
                return INNER;
            } else {
                return OUTTER;
            }
        } else {
            return UNKNOWN;
        }
    }


    public static boolean hasInternalSdcard(Context mContext) {
        StorageManager sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        if (sm == null) {
            Log.d(TAG,"storage error -1: get storemanager error");
        }
        StorageVolume[] volumes = sm.getVolumeList();
        // MountService grantee primary volume as the first.
        // check if exist non primary non usbdisk storage exist
        for (int i=1; i<volumes.length; i++) {
            StorageVolume sv = (StorageVolume)volumes[i];
            if (!(sv.getPath().startsWith("/mnt/usbdisk"))) {
                File file = new File(sv.getPath());
                return file != null;
            }
        }
        return false;
    }

}
