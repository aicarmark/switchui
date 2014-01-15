/*
 * @(#)FileUtils.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * rdq478       2011/05/24  IKMAIN-17917        Initial Version
 * rdq478       2011/09/22  IKSTABLE6-10141     Add method isFileExist()
 * rdq478       2011/10/18  IKSTABLE6-18450     Changed to use PNG compression
 */

package com.motorola.contextual.actions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * This class implement common file utility functions.  <code><pre>
 *
 * CLASS:
 *
 * RESPONSIBILITIES:
 *     This class implements common functions that can be of use by multiple
 *          action modules
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */


public class FileUtils {
    private static final String TAG = Constants.TAG_PREFIX + FileUtils.class.getSimpleName();

    /**
     * Store buffer to a file in Internal Storage.
     * @param context
     * @param buffer - buffer to store
     * @param dirName - folder name
     * @param fileName - file name
     * @return true if successful
     */
    public static boolean storeBuffer(Context context, final byte[] buffer, final String dirName, final String fileName) {
        boolean result = false;

        if ((context == null) || (buffer == null) || (dirName == null) || (fileName == null)) {
            Log.e(TAG, "Invalid input parameters.");
            return result;
        }

        /*
         * Check if directory exist or not. If not, this should create one.
         * If it already exist, it should return none null file object.
         */
        File wpdir = context.getDir(dirName, Context.MODE_PRIVATE);
        if (wpdir == null) {
            Log.e(TAG, "Error unable to create/open private directory.");
        }
        else {
            FileOutputStream storeImage = null;

            try {
                storeImage = new FileOutputStream(new File(wpdir, fileName));
                storeImage.write(buffer);
                result = true;
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Not able to open file for writing.");
            } catch (IOException e) {
                Log.e(TAG, "Error occur while writting buffer to file.");
            } finally {
                if (storeImage != null) {
                    try {
                        storeImage.close();
                    } catch(IOException e) {
                        Log.e(TAG, "Error occur while trying to close file.");
                    }
                }
            }
        }

        return result;
    }

    /**
     * Open file for reading.
     * @param context
     * @param dirName - folder name
     * @param fileName - file name
     * @return FileInputStream if successful or NULL otherwise
     */
    public static FileInputStream openFileInput(Context context, final String dirName, final String fileName) {

        if ((context == null) || (dirName == null) || (fileName == null)) {
            Log.e(TAG, "Invalid input parameters.");
            return null;
        }

        File wpdir = context.getDir(dirName, Context.MODE_PRIVATE);

        if ((wpdir == null) || (!wpdir.isDirectory())) {
            Log.e(TAG, "Error unable to open private directory.");
        }
        else {
            try {
                FileInputStream stream = new FileInputStream(new File(wpdir, fileName));
                return stream;
            } catch(FileNotFoundException e) {
                Log.e(TAG, "File not found, dirName = " + dirName + " fileName = " + fileName);
            }
        }

        return null;
    }

    /**
     * Get list of file names in a directory.
     * @param context
     * @param dirName - folder name
     * @return list of file names or NULL in error case
     */
    public static ArrayList<String> listFiles(Context context, final String dirName) {
        if ((context == null) || (dirName == null)) {
            Log.e(TAG, "Invalid input parameters.");
            return null;
        }

        File wpdir = context.getDir(dirName, Context.MODE_PRIVATE);
        if ((wpdir == null) || (!wpdir.isDirectory())) {
            Log.e(TAG, "Error unable to open directory " + dirName);
            return null;
        }

        return new ArrayList<String>(Arrays.asList(wpdir.list()));
    }

    /**
     * Delete a file.
     * @param context
     * @param dirName - folder name
     * @param fileName - file name
     * @return true if successful
     */
    public static boolean deleteFile(Context context, final String dirName, final String fileName) {
        if ((context == null) || (dirName == null) || (fileName == null)) {
            Log.e(TAG, "Invalid input parameters.");
            return false;
        }

        File wpdir = context.getDir(dirName, Context.MODE_PRIVATE);
        if ((wpdir == null) || (!wpdir.isDirectory())) {
            Log.e(TAG, "Error unable to open directory " + dirName);
            return false;
        }

        File wpFile = new File(wpdir, fileName);
        if ((wpFile != null) && wpFile.exists() && wpFile.isFile()) {
            return wpFile.delete();
        }
        else {
            Log.e(TAG, "Wallpaper file = " + fileName + " not exists.");
        }

        return false;
    }

    /**
     * Check if file exist.
     * @param context
     * @param dirName
     * @param fileName
     * @return true if file exist
     */
    public static boolean isFileExist(final Context context, final String dirName, final String fileName) {
        if ((context == null) || (dirName == null) || (fileName == null)) {
            Log.e(TAG, "Invalid input parameters.");
            return false;
        }

        File wpdir = context.getDir(dirName, Context.MODE_PRIVATE);
        if ((wpdir == null) || (!wpdir.isDirectory())) {
            Log.e(TAG, "Error unable to open directory " + dirName);
            return false;
        }

        File wpFile = new File(wpdir, fileName);
        if ((wpFile != null) && wpFile.exists() && wpFile.isFile()) {
            return true;
        }

        return false;
    }

    /**
     * Convert bitmap to byte array.
     * @param bitmap
     * @return byte array
     */
    public static byte[] toByteArray(final Bitmap bitmap) {

        if (bitmap == null) {
            Log.e(TAG, "Invalid input parameters.");
            return null;
        }

        byte[] buffer = null;

        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            buffer = stream.toByteArray();
            stream.close();
        } catch(IOException e) {
            Log.e(TAG, "Could not close output stream.");
        }

        return buffer;
    }

    /**
     * Convert drawable to byte array.
     * @param drawable
     * @return byte array
     */
    public static byte[] toByteArray(final Drawable drawable) {

        if (drawable == null) {
            Log.e(TAG, "Invalid input parameters.");
            return null;
        }

        Bitmap bitmapImage = ((BitmapDrawable) drawable).getBitmap();

        if (bitmapImage == null) {
            Log.e(TAG, "Unable to get bitmap from drawable.");
            return null;
        }

        return toByteArray(bitmapImage);
    }
}
