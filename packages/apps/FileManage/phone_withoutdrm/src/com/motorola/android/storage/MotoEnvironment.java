/*
 * Copyright (C) 2011 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 */

package com.motorola.android.storage;

import android.content.Context;
import android.os.Environment;
import android.os.Parcelable;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.StorageVolume;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;

/**
 * This class provides Motorola extension APIs for third-party applications. It
 * augments the functionality provided by {@link andoid.os.Environment}. Native
 * applicatons should use Android Standard APIs.
 */
public class MotoEnvironment {
    private static final String TAG = "MotoEnvironment";
    private static IMountService mMntSvc = null;

    // Media types returned by getExternalStorageMediaType()
    public final static String MEDIA_TYPE_SDCARD = "SDCARD";
    public final static String MEDIA_TYPE_FLASH = "FLASH";
    public final static String MEDIA_TYPE_USBDISK = "USBDISK";
    public final static String MEDIA_TYPE_UNKNOW = "UNKNOWN";
    public static final String MEDIA_TYPE_EMULATED = "EMULATED";

    // More APIs for feature 36502
    /**
     * Gets the current state of the specified shared storage device.
     * 
     * @param mountPoint
     *            The mount point for the shared storage device. The mount point
     *            string <em>must</em> have been obtained using
     *            {@link #getExternalStorageDirectories}. Hardcoded mount point
     *            strings can cause unexpected results.
     * 
     * @return The current state of the specified shared storage device: one of
     *         <code>android.os.Environment.MEDIA_<em>XXX</em></code>.
     */
    public static String getExternalStorageState(String mountPoint) {
	initMountServiceAndCheck();
	try {
	    return mMntSvc.getVolumeState(mountPoint);
	} catch (Exception ex) {
	    Log.e(TAG, "Error in getVolumeList() " + ex);
	    return Environment.MEDIA_REMOVED;
	}
    }

    /**
     * Gets all supported shared storage directories, regardless of their
     * current state. Use {@link #getExternalStorageState} to determine whether
     * the storage device corresponding to a given directory is mounted. <br/>
     * <br/>
     * Use {@link java.io.File#getPath} on an array entry to get the mount point
     * string needed by the other methods in this class.
     * 
     * @return An array of {@link java.io.File} objects. Each entry identifies a
     *         shared storage directory.
     */
    public static File[] getExternalStorageDirectories() {
	ArrayList<File> rtnArray = new ArrayList<File>();

	initMountServiceAndCheck();
	try {
	    Parcelable[] volumes = mMntSvc.getVolumeList();
	    for (Parcelable s : volumes) {
		Log.d(TAG, "volume = " + ((StorageVolume) s).getPath());
		rtnArray.add(new File(((StorageVolume) s).getPath()));
	    }
	} catch (Exception re) {
	    Log.e(TAG, "Error in getExternalStorageDirectories()");
	}

	return (File[]) rtnArray.toArray(new File[rtnArray.size()]);
    }

    /**
     * Indicates whether the specified storage device is removable.
     * 
     * @param mountPoint
     *            The mount point for the storage device. The mount point string
     *            <em>must</em> have been obtained using
     *            {@link #getExternalStorageDirectories}. Hardcoded mount point
     *            strings can cause unexpected results.
     * 
     * @return <code>true</code> if the specified storage device is removable.
     */
    public static boolean isExternalStorageRemovable(String mountPoint) {
	boolean isRemovable = false;
	StorageVolume volume = getStorageVolumeFromPath(mountPoint);
	if (volume != null)
	    isRemovable = volume.isRemovable();
	return isRemovable;
    }

    /**
     * Gets the readable display name of the specified storage device. Multiple
     * languages are supported.
     * 
     * <em>For use only by native applications.</em>
     * 
     * @param mountPoint
     *            The mount point for the storage device. The mount point string
     *            <em>must</em> have been obtained using
     *            {@link #getExternalStorageDirectories}. Hardcoded mount point
     *            strings can cause unexpected results.
     * 
     * @return The display name of the storage device, or null if the supplied
     *         mount point string is invalid.
     * @hide
     */
    public static String getExternalStorageDisplayName(String mountPoint,Context context) {
	String res = null;
	StorageVolume volume = getStorageVolumeFromPath(mountPoint);
	if (volume != null)
	    res = volume.getDescription(context);
	return res;
    }

    /**
     * Gets the media type of the specified storage device.
     * 
     * @param mountPoint
     *            The mount point for the storage device. The mount point string
     *            <em>must</em> have been obtained using
     *            {@link #getExternalStorageDirectories}. Hardcoded mount point
     *            strings can cause unexpected results.
     * 
     * @return The media type of the specified storage device: one of
     *         <code>MotoEnvironment.MEDIA_TYPE_<i>XXX</i></code>.
     */
    public static String getExternalStorageMediaType(String mountPoint) {
	if (getStorageVolumeFromPath(mountPoint) == null)
	    return MEDIA_TYPE_UNKNOW;

	if (isExternalStorageRemovable(mountPoint)) {
	    return mountPoint.startsWith("/mnt/usbdisk") ? MEDIA_TYPE_USBDISK
		    : MEDIA_TYPE_SDCARD;
	} else {
	    return MEDIA_TYPE_FLASH;
	}
    }

    private static StorageVolume getStorageVolumeFromPath(String mountPoint) {
	StorageVolume rtn = null;
	initMountServiceAndCheck();
	try {
	    Parcelable[] list = mMntSvc.getVolumeList();
	    if (list != null) {
		for (Parcelable p : list) {
		    if (p != null) {
			StorageVolume volume = (StorageVolume) p;
			if (volume != null
				&& volume.getPath().equals(mountPoint)) {
			    rtn = volume;
			    break;
			}
		    }
		}
	    }
	} catch (Exception ex) {
	    Log.e(TAG, "Error in getVolumeList() " + ex);
	}
	return rtn;
    }

    private static void initMountServiceAndCheck() {
	try {
	    if (mMntSvc == null) {
		mMntSvc = IMountService.Stub.asInterface(ServiceManager
			.getService("mount"));
	    }
	    if (mMntSvc == null) {
		Log.e(TAG, "Fail to get MountService!");
	    }
	} catch (Exception ex) {
	    Log.e(TAG, "Exception thrown in getting MountSerivce! " + ex);
	}
    }
}
