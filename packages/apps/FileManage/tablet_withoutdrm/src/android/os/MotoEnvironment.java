/*
  * Copyright (C) 2009/2010 Motorola Inc.
  * All Rights Reserved.
  * Motorola Confidential Restricted.
  */

// BEGIN Motorola, a18273, 12/12/2011, IKHALFMWK-586
// All motorola extended api should be defined in motorola's namespace
// Change standard android.os package is not corrected. To fix this
// this class is deprecated and should not be use any more.
// use com.motorola.android.storage.MotoEnvironment instead
// Add <uses-library android:name="com.motorola.android.storage"/> in
// application AndroidManifest.xml to use it.

package android.os;

import java.io.File;
import android.util.Log;
import android.util.AndroidRuntimeException;
/**
 * trh867, Feature 34534
 * Provides access to external alternate storage.
 */
public class MotoEnvironment {

    private static final String TAG = "MotoEnvironment";

    static void warning() {
        Log.e(TAG, "android.os.MotoEnvironment is deprecated," +
                "use com.motorola.android.storage.MotoEnvironment instead!");
        throw new AndroidRuntimeException("Using deprecated API");
    }

    static File getDirectory(String variableName, String defaultPath) {
        warning();
        return null;
    }

    /**
     * Gets the external alternate storage directory.
     */
    public static File getExternalAltStorageDirectory() {
        warning();
        return null;
    }

    /**
     * decide if the given path is the external storage directory.
     */
    public static boolean isExternalStorageDirectory(String path) {
        warning();
        return false;
    }

    /**
     * decide if the given path is in the external storage directory.
     */
    public static boolean isInExternalStorageDirectory(String path) {
        warning();
        return false;
    }

    /**
     * getExternalAltStorageState() returns MEDIA_REMOVED if the media is not present.
     */
    public static final String MEDIA_REMOVED = "deprecated!!!";

    /**
     * getExternalAltStorageState() returns MEDIA_UNMOUNTED if the media is present
     * but not mounted.
     */
    public static final String MEDIA_UNMOUNTED = "deprecated!!!";

    /**
     * getExternalAltStorageState() returns MEDIA_CHECKING if the media is present
     * and being disk-checked
     */
    public static final String MEDIA_CHECKING = "deprecated!!!";

    /**
     * getExternalAltStorageState() returns MEDIA_NOFS if the media is present
     * but is blank or is using an unsupported filesystem
     */
    public static final String MEDIA_NOFS = "deprecated!!!";

    /**
     * getExternalAltStorageState() returns MEDIA_MOUNTED if the media is present
     * and mounted at its mount point with read/write access.
     */
    public static final String MEDIA_MOUNTED = "deprecated!!!";

    /**
     * getExternalAltStorageState() returns MEDIA_MOUNTED_READ_ONLY if the media is present
     * and mounted at its mount point with read only access.
     */
    public static final String MEDIA_MOUNTED_READ_ONLY = "deprecated!!!";

    /**
     * getExternalAltStorageState() returns MEDIA_SHARED if the media is present
     * not mounted, and shared via USB mass storage.
     */
    public static final String MEDIA_SHARED = "deprecated!!!";

    /**
     * getExternalAltStorageState() returns MEDIA_BAD_REMOVAL if the media was
     * removed before it was unmounted.
     */
    public static final String MEDIA_BAD_REMOVAL = "deprecated!!!";

    // BEGIN Motorola, a23250, 12/02/2010, IKMAIN-9593
    /**
     * getExternalStorageState() returns MEDIA_BROKEN if the media was
     * broken
     * @hide
     */
    public static final String MEDIA_BROKEN = "deprecated!!!";
    // END IKMAIN-9593

    /**
     * getExternalAltStorageState() returns MEDIA_UNMOUNTABLE if the media is present
     * but cannot be mounted.  Typically this happens if the file system on the
     * media is corrupted.
     */
    public static final String MEDIA_UNMOUNTABLE = "deprecated!!!";

    /**
     * Gets the current state of the external alternate storage device.
     */
    public static String getExternalAltStorageState() {
        warning();
        return null;
    }

    /**
     * Gets the current state of the underline storage device for the given file
     */
    public static String getStorageState(String path) {
        warning();
        return null;
    }

    /**
    *Gets removed External Storage volumes.
    */
   public static String[] getRemovedExternalStorage() {
       warning();
        return null;
   }

    // BEGIN Motorola, a5705c, 09/16/2010, IKOLYMP-5250
    /**
     * Generates the raw path to an application's data
     * @hide
     */
    public static File getExternalAltStorageAppDataDirectory(String packageName) {
        warning();
        return null;
    }

    /**
     * Generates the raw path to an application's media
     * @hide
     */
    public static File getExternalAltStorageAppMediaDirectory(String packageName) {
        warning();
        return null;
    }
    // END IKOLYMP-5250
}
// END IKHALFMWK-586
