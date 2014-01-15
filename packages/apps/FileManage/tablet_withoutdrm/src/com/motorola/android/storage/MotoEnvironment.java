/*
  * Copyright (C) 2011 Motorola Inc.
  * All Rights Reserved.
  * Motorola Confidential Restricted.
  */

package com.motorola.android.storage;

import android.content.res.Resources;
import android.os.Environment;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.SystemProperties;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to privde Motorola extention APIs for both
 * native and 3-rd party applications to get more functions from
 * {@link andoid.os.Environment}
 */
public class MotoEnvironment {
    private static final String TAG = "MotoEnvironment";
    private static IMountService mMntSvc = null;

    // Media type returned by getExternalStorageMediaType()
    public final static String MEDIA_TYPE_SDCARD = "SDCARD";
    public final static String MEDIA_TYPE_FLASH = "FLASH";
    public final static String MEDIA_TYPE_USBDISK = "USBDISK";
    public final static String MEDIA_TYPE_UNKNOW = "UNKNOWN";

    /**
     * @Deprecated
     * Gets the external alternate storage directory.
     */
    public static File getExternalAltStorageDirectory() {
        if (SystemProperties.getInt("ro.mot.internalsdcard", 0) != 0) {
            return android.os.MotoEnvironment.getExternalAltStorageDirectory();
        } else {
            return null;
        }
    }


    /**
     * @Deprecated
     * Gets the current state of the external alternate storage device.
     */
    public static String getExternalAltStorageState() {
        if (SystemProperties.getInt("ro.mot.internalsdcard", 0) != 0) {
            return android.os.MotoEnvironment.getExternalAltStorageState();
        } else {
            return Environment.MEDIA_REMOVED;
        }
    }

    // More APIs for feature 36502
    /**
     * Gets the current state of the underline storage device for the given file
     *
     * @param mountPoint The mount point for the external storage that MUST get
     * from API getExternalStorageDirectories()
     *
     * @return the state, one of Environment.MEDIA_XXX
     */
    public static String getExternalStorageState(String mountPoint) {
        try {
            if (mMntSvc == null) {
                mMntSvc = IMountService.Stub.asInterface(ServiceManager
                                                         .getService("mount"));
            }
            return mMntSvc.getVolumeState(mountPoint);

        } catch (RemoteException rex) {
            Log.e(TAG, "Error in getVolumeList() " + rex);
            return Environment.MEDIA_REMOVED;
        }
    }

    /**
     * Get all the supported storage directories
     *
     * @return all the path of supported external storage
     */
    public static File[] getExternalStorageDirectories() {
        List<File> storages = new ArrayList<File>();

        if (mMntSvc == null) {
            mMntSvc = IMountService.Stub.asInterface(ServiceManager.getService("mount"));
        }

        try {
            String [] volumes = mMntSvc.getVolumeList();

            for (int i=0; i<volumes.length; i++) {

                if (volumes[i] != null)
                    storages.add(new File(volumes[i]));
                else
                    Log.e(TAG, "Error in getExternalStorageDirectories(): the volume cannot be NULL");
            }
        } catch (RemoteException rex) {
            Log.e(TAG, "Error in getVolumeList()" + rex);
        }

        return storages.toArray(new File[storages.size()]);
    }

    /**
     * Return whether the speicified external storage is removable
     *
     * @param mountPoint The mount point for the external storage that MUST get
     * from API getExternalStorageDirectories(). A hardcoded param
     * (eg. "/mnt/sdcard/") may get unexpected result!
     *
     * @return TRUE if external storage mountPoint is removable
     */
    public static boolean isExternalStorageRemovable(String mountPoint) {
        // BEGIN Motorola, a18273, IKHALFMWK-363
        try {
            if (mMntSvc == null) {
                mMntSvc = IMountService.Stub.asInterface(ServiceManager
                                                         .getService("mount"));
            }
            return mMntSvc.isVolumeRemovable(mountPoint);
        } catch (RemoteException rex) {
            Log.e(TAG, "Error in getVolumeList()" + rex);
            return true;
        }
        // END IKHALFMWK-363
    }

    /**
     * @hide Native applications used only.
     * Get the readable display name of the speicified external storage.
     * It is multiple languages supported.
     *
     * @param mountPoint The mount point for the external storage that MUST get
     * from API getExternalStorageDirectories().
     *
     * @return the name of storage if mountPoint is valid or null if invalid
     */
    public static String getExternalStorageDisplayName(String mountPoint) {
        String res = null;
        File [] availableStorages = getExternalStorageDirectories();

        if (!isValidMountPoint(mountPoint)) {
            res = null;
        // Generally, the only one storage is always called external sdcard
        } /*else if(availableStorages.length == 1 ||
                    MEDIA_TYPE_SDCARD.equals(getExternalStorageMediaType(mountPoint))) {
            res = Resources.getSystem().getString(
                com.motorola.filemanager.R.string.removable_sdcard);
        } else if (MEDIA_TYPE_FLASH.equals(getExternalStorageMediaType(mountPoint))){
            res = Resources.getSystem().getString(
            		com.motorola.filemanager.R.string.internal_storage);
        } */// TODO add usbdis support
        return res;
    }

    /**
     * Get the media type of the speicified external storage.
     *
     * @param mountPoint The mount point for the external storage that MUST get
     * from API getExternalStorageDirectories().
     *
     * @return one of value in MEDIA_TYPE_XXX
     */
    public static String getExternalStorageMediaType(String mountPoint) {
        if(isValidMountPoint(mountPoint)) {
            if (mountPoint.contains("sdcard") && isExternalStorageRemovable(mountPoint)) {
                return MEDIA_TYPE_SDCARD;
            } else if (mountPoint.contains("usbdisk")) {
                return MEDIA_TYPE_USBDISK;
            } else {
                return MEDIA_TYPE_FLASH;
            }
        } else {
            return MEDIA_TYPE_UNKNOW;
        }
    }

    private static boolean isValidMountPoint(String mountPoint) {
        boolean isMountPointValid = false;
        File [] availableStorages = getExternalStorageDirectories();

        if (mountPoint != null) {
            for (File storage : availableStorages) {
                if (storage.equals(new File(mountPoint))) {
                    isMountPointValid = true;
                    break;
                }
            }
        }
        return isMountPointValid;
    }
}
