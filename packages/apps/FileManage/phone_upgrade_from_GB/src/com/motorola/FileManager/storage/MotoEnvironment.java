/*
 * Copyright (C) 2009/2010 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 */

package com.motorola.FileManager.storage;

import com.motorola.FileManager.storage.IMountService;
import android.os.Environment;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

/**
 * trh867, Feature 34534 Provides access to external alternate storage.
 */
public class MotoEnvironment {

    private static final String TAG = "MotoEnvironment";

    private static IMountService mMntSvc = null;

    //modify by amt_xulei for SWITCHUITWO-7
    //reason:CT JB testload SDcard and internal storage path
    // is /storage/sdcard0 and /storage/sdcard1
//    private static final File EXTERNAL_STORAGE_DIRECTORY = getDirectory(
//	    "EXTERNAL_STORAGE", "/mnt/sdcard");
    private static final File EXTERNAL_STORAGE_DIRECTORY = getDirectory(
    	    "EXTERNAL_STORAGE", "/storage/sdcard0");
    
//    private static final File EXTERNAL_ALT_STORAGE_DIRECTORY = getDirectory(
//	    "EXTERNAL_STORAGE_ALT", "/mnt/ext_sdcard");
    private static final File EXTERNAL_ALT_STORAGE_DIRECTORY = getDirectory(
    	    "EXTERNAL_STORAGE_ALT", "/storage/ext_sdcard");
    //end modify
    
    static File getDirectory(String variableName, String defaultPath) {
	String path = System.getenv(variableName);
	return path == null ? new File(defaultPath) : new File(path);
    }

    /**
     * Gets the external alternate storage directory.
     */
    public static File getExternalAltStorageDirectory() {
	return EXTERNAL_ALT_STORAGE_DIRECTORY;
    }

    /**
     * decide if the given path is the external storage directory.
     */
    public static boolean isExternalStorageDirectory(String path) {
	/*
	 * does not support check symbol lin since it's obsoleted and will go
	 * away in G*
	 */
	String firstMount = EXTERNAL_STORAGE_DIRECTORY.toString();
	String secondMount = EXTERNAL_ALT_STORAGE_DIRECTORY.toString();
	return (firstMount.equals(path) || secondMount.equals(path));
    }

    /**
     * decide if the given path is in the external storage directory.
     */
    public static boolean isInExternalStorageDirectory(String path) {
	// not include null folder
	if (path == null)
	    return false;
	// not include folder like /mnt/sdcard-ext/aaa/,/mnt/sdcard-ext/aaa//
	if (path.endsWith("/"))
	    return false;
	/*
	 * does not support check symbol link since it's obsoleted and will go
	 * away in G*
	 */
	String firstMount = EXTERNAL_STORAGE_DIRECTORY.toString();
	String secondMount = EXTERNAL_ALT_STORAGE_DIRECTORY.toString();
	return ((path.startsWith(firstMount + "/") || path
		.startsWith(secondMount + "/")));
    }

    /**
     * getExternalAltStorageState() returns MEDIA_REMOVED if the media is not
     * present.
     */
    public static final String MEDIA_REMOVED = "removed";

    /**
     * getExternalAltStorageState() returns MEDIA_UNMOUNTED if the media is
     * present but not mounted.
     */
    public static final String MEDIA_UNMOUNTED = "unmounted";

    /**
     * getExternalAltStorageState() returns MEDIA_CHECKING if the media is
     * present and being disk-checked
     */
    public static final String MEDIA_CHECKING = "checking";

    /**
     * getExternalAltStorageState() returns MEDIA_NOFS if the media is present
     * but is blank or is using an unsupported filesystem
     */
    public static final String MEDIA_NOFS = "nofs";

    /**
     * getExternalAltStorageState() returns MEDIA_MOUNTED if the media is
     * present and mounted at its mount point with read/write access.
     */
    public static final String MEDIA_MOUNTED = "mounted";

    /**
     * getExternalAltStorageState() returns MEDIA_MOUNTED_READ_ONLY if the media
     * is present and mounted at its mount point with read only access.
     */
    public static final String MEDIA_MOUNTED_READ_ONLY = "mounted_ro";

    /**
     * getExternalAltStorageState() returns MEDIA_SHARED if the media is present
     * not mounted, and shared via USB mass storage.
     */
    public static final String MEDIA_SHARED = "shared";

    /**
     * getExternalAltStorageState() returns MEDIA_BAD_REMOVAL if the media was
     * removed before it was unmounted.
     */
    public static final String MEDIA_BAD_REMOVAL = "bad_removal";

    // BEGIN Motorola, a23250, 12/02/2010, IKMAIN-9593
    /**
     * getExternalStorageState() returns MEDIA_BROKEN if the media was broken
     * 
     * @hide
     */
    public static final String MEDIA_BROKEN = "broken";
    // END IKMAIN-9593

    /**
     * getExternalAltStorageState() returns MEDIA_UNMOUNTABLE if the media is
     * present but cannot be mounted. Typically this happens if the file system
     * on the media is corrupted.
     */
    public static final String MEDIA_UNMOUNTABLE = "unmountable";

    // BEGIN Motorola, a5705c, 09/16/2010, IKOLYMP-5250
//    private static final File EXTERNAL_ALT_STORAGE_ANDROID_DATA_DIRECTORY = new File(
//	    new File(SystemProperties.get("EXTERNAL_ALT_STORAGE_MOUNT",
//		    "/mnt/ext_sdcard"), "Android"), "data");
    private static final File EXTERNAL_ALT_STORAGE_ANDROID_DATA_DIRECTORY = new File(
    	    new File(SystemProperties.get("EXTERNAL_ALT_STORAGE_MOUNT",
    		    "/storage/ext_sdcard"), "Android"), "data");

//    private static final File EXTERNAL_ALT_STORAGE_ANDROID_MEDIA_DIRECTORY = new File(
//	    new File(SystemProperties.get("EXTERNAL_ALT_STORAGE_MOUNT",
//		    "/mnt/ext_sdcard"), "Android"), "media");
    private static final File EXTERNAL_ALT_STORAGE_ANDROID_MEDIA_DIRECTORY = new File(
    	    new File(SystemProperties.get("EXTERNAL_ALT_STORAGE_MOUNT",
    		    "/storage/ext_sdcard"), "Android"), "media");

    // END IKOLYMP-5250

    /**
     * Gets the current state of the external alternate storage device.
     */
    public static String getExternalAltStorageState() {
	try {
	    if (mMntSvc == null) {
		Log.d("", "getExternalAltStorageState-mMntSvc is null");
		mMntSvc = IMountService.Stub.asInterface(ServiceManager
			.getService("mount"));
	    }
	    return mMntSvc.getVolumeState(getExternalAltStorageDirectory()
		    .toString());
	} catch (Exception rex) {
	    return Environment.MEDIA_REMOVED;
	}
    }

    /**
     * Gets the current state of the underline storage device for the given file
     */
    public static String getStorageState(String path) {
	String firstMount = EXTERNAL_STORAGE_DIRECTORY.toString();
	String secondMount = EXTERNAL_ALT_STORAGE_DIRECTORY.toString();
	try {
	    if (mMntSvc == null) {
		mMntSvc = IMountService.Stub.asInterface(ServiceManager
			.getService("mount"));
	    }
	    // BEGIN Motorola, wbdq68, Mar/08/2011, IKSTABLEFIVE-649, port to
	    // main-dev
	    if (path.startsWith(firstMount + "/"))
		return mMntSvc.getVolumeState(firstMount);
	    else if (path.startsWith(secondMount + "/"))
		return mMntSvc.getVolumeState(secondMount);
	    else
		return Environment.MEDIA_REMOVED;
	    // END IKSTABLEFIVE-649
	} catch (Exception rex) {
	    return Environment.MEDIA_REMOVED;
	}
    }

    /**
     * Gets removed External Storage volumes.
     */
    public static String[] getRemovedExternalStorage() {
	String state = getExternalAltStorageState();
	String state_innerSDcard = Environment.getExternalStorageState();

	ArrayList<String> slist = new ArrayList<String>();

	if (Environment.MEDIA_REMOVED.equals(state_innerSDcard)) {
	    slist.add(EXTERNAL_STORAGE_DIRECTORY.toString());
	}

	if (Environment.MEDIA_REMOVED.equals(state)) {
	    slist.add(EXTERNAL_ALT_STORAGE_DIRECTORY.toString());
	}
	String[] sarray = new String[slist.size()];
	return slist.toArray(sarray);
    }

    // BEGIN Motorola, a5705c, 09/16/2010, IKOLYMP-5250
    /**
     * Generates the raw path to an application's data
     * 
     * @hide
     */
    public static File getExternalAltStorageAppDataDirectory(String packageName) {
	return new File(EXTERNAL_ALT_STORAGE_ANDROID_DATA_DIRECTORY,
		packageName);
    }

    /**
     * Generates the raw path to an application's media
     * 
     * @hide
     */
    public static File getExternalAltStorageAppMediaDirectory(String packageName) {
	return new File(EXTERNAL_ALT_STORAGE_ANDROID_MEDIA_DIRECTORY,
		packageName);
    }
    // END IKOLYMP-5250
}
