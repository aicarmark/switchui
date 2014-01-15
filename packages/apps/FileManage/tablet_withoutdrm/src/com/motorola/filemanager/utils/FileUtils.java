/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR              Author      Description
 * 2011-05-23   IKTABLETMAIN-348    XQH748      initial
 */
package com.motorola.filemanager.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Images.ImageColumns;

import com.motorola.filemanager.FileManagerApp;

public class FileUtils {
    /** TAG for log messages. */
    static final String TAG = "FileUtils";

    /**
     * Whether the filename is a video file.
     *
     * @param filename
     * @return
     */
    /*
     * public static boolean isVideo(String filename) { String mimeType =
     * getMimeType(filename); if (mimeType != null &&
     * mimeType.startsWith("video/")) { return true; } else { return false; } }
     */

    /**
     * Whether the URI is a local one.
     *
     * @param uri
     * @return
     */
    public static boolean isLocal(String uri) {
        if (uri != null && !uri.startsWith("http://")) {
            return true;
        }
        return false;
    }

    /**
     * Gets the extension of a file name, like ".png" or ".jpg".
     *
     * @param uri
     * @return Extension including the dot("."); "" if there is no extension;
     *         null if uri was null.
     */
    public static String getExtension(String uri) {
        if (uri == null) {
            return null;
        }

        int dot = uri.lastIndexOf(".");
        if (dot >= 0) {
            return uri.substring(dot);
        } else {
            // No extension.
            return "";
        }
    }

    /**
     * Returns true if uri is a media uri.
     *
     * @param uri
     * @return
     */
    public static boolean isMediaUri(String uri) {
        if (uri.startsWith(Audio.Media.INTERNAL_CONTENT_URI.toString()) ||
                uri.startsWith(Audio.Media.EXTERNAL_CONTENT_URI.toString()) ||
                uri.startsWith(Video.Media.INTERNAL_CONTENT_URI.toString()) ||
                uri.startsWith(Video.Media.EXTERNAL_CONTENT_URI.toString())) {
            return true;
        } else {
            return false;
        }
    }

    public static String getFullPath(String curdir, String file) {
        String separator = "/";
        if (curdir.endsWith("/")) {
            separator = "";
        }
        String fullPath = curdir + separator + file;
        return fullPath;
    }

    public static boolean isMediaFile(Context context, String fileName) {
        String mimeType = MimeTypeUtil.getMimeType(context, fileName);
        FileManagerApp.log(TAG + "isMediaFile type: " + " " + mimeType, false);
        if (mimeType == null) {
            return false;
        }
        if ((mimeType.indexOf("image") == 0) || (mimeType.indexOf("video") == 0) ||
                (mimeType.indexOf("audio") == 0)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Convert File into Uri.
     * @param file
     * @return uri
     */
    public static Uri getUri(File file) {
        if (file != null) {
            return Uri.fromFile(file);
        }
        return null;
    }

    /**
     * Convert Uri into File.
     * @param uri
     * @return file
     */
    public static File getFile(Uri uri) {
        if (uri != null) {
            String filepath = uri.getPath();
            if (filepath != null) {
                return new File(filepath);
            }
        }
        return null;
    }

    /**
     * Returns the path only (without file name).
     * @param file
     * @return
     */
    public static File getPathWithoutFilename(File file) {
        if (file != null) {
            if (file.isDirectory()) {
                // no file to be split off. Return everything
                return file;
            } else {
                String filename = file.getName();
                String filepath = file.getAbsolutePath();

                // Construct path without file name.
                String pathwithoutname =
                        filepath.substring(0, filepath.length() - filename.length());
                if (pathwithoutname.endsWith("/")) {
                    pathwithoutname = pathwithoutname.substring(0, pathwithoutname.length() - 1);
                }
                return new File(pathwithoutname);
            }
        }
        return null;
    }

    /**
     * Constructs a file from a path and file name.
     *
     * @param curdir
     * @param file
     * @return
     */
    public static File getFile(String curdir, String file) {
        String separator = "/";
        if (curdir.endsWith("/")) {
            separator = "";
        }
        File clickedFile = new File(curdir + separator + file);
        return clickedFile;
    }

    public static File getFile(File curdir, String file) {
        return getFile(curdir.getAbsolutePath(), file);
    }

    public static File getFile(String filepath) {
        return new File(filepath);
    }

    public static long getFileSize(File filepath) {
        // Calculate the file size. If reminder is more than 0 we should show
        // increase the size by 1, otherwise we should show the size as it is.
        long size = filepath.length() / 1024;
        long rem = filepath.length() % 1024;
        if (rem > 0) {
            size++;
        }
        return size;
    }

    public static long getDirSize(File filepath) {
        long size = 0;
        try {
            size = getDirTotSize(filepath);
            long rem = size % 1024;
            size = size / 1024;
            if (rem > 0) {
                size++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    public static long getDirTotSize(File filepath) {
        long size = 0;
        try {
            File[] files = filepath.listFiles();
            if (files != null) {
              for (int i = 0; i < files.length; i++) {
                  if (files[i].isDirectory()) {
                      size += getDirTotSize(files[i]);
                  } else {
                      size += files[i].length();
                  }
              }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    public static void startMediaScan(Context context, String filePath) {
        new MediaScannerNotifier(context, filePath);
    }

    static class MediaScannerNotifier implements MediaScannerConnectionClient {
        private MediaScannerConnection mConnection;
        private String mFile;

        public MediaScannerNotifier(Context context, String file) {
            mFile = file;
            mConnection = new MediaScannerConnection(context, this);
            mConnection.connect();
        }

        public void onMediaScannerConnected() {
            mConnection.scanFile(mFile, null);
        }

        public void onScanCompleted(String path, Uri uri) {
            mConnection.disconnect();
        }
    }
    private static final String ACTION_MEDIA_SCANNER_SCAN_FOLDER =
            "com.motorola.internal.intent.action.MEDIA_SCANNER_SCAN_FOLDER";

    public static boolean mediaScanFolder(Context context, File folder) {
        Intent intent = new Intent(ACTION_MEDIA_SCANNER_SCAN_FOLDER);
        Uri uri = Uri.fromFile(folder);
        intent.setData(uri);
        FileManagerApp.log(TAG + "mediaScanFolder " + folder.toString() + " " + intent.toString(),
                false);

        try {
            context.sendBroadcast(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public static void mediaScanFile(Activity activity, String newFileName, String currentDirectory) {
        // Scan the file , in case it is not in directory
        Intent myIntent =
                new Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri
                        .parse("file://" + currentDirectory + "/" + newFileName));
        try {
            activity.sendBroadcast(myIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
        FileManagerApp.log(TAG + "mediaScanFile " + currentDirectory + "/" + newFileName + " " +
                myIntent.toString(), false);
    }

    /**
     * Delete a media file from MediaStore
     *
     * @param File file
     * @return
     */

    public static void deleteFileFromMediaStore(File file, Context context) {
        // delete the file from the DB.
        // First check what kind of media is this Mime type
        // Images
        if (file == null) {
            return;
        }
        String mimeType = MimeTypeUtil.getMimeType(context, file.getName());
        long id;
        Uri contentUri = null;
        int idx = -1;
        Uri mediaUri = null;
        if (mimeType != null && (mimeType.indexOf("image") == 0)) {
            idx = 0;
            mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (mimeType != null && (mimeType.indexOf("audio") == 0)) {
            idx = 1;
            mediaUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        } else if (mimeType != null && (mimeType.indexOf("video") == 0)) {
            idx = 2;
            mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }
        id = getMediaContentIdByFilePath(context, file.getAbsolutePath(), idx);
        if (id > 0) {
            contentUri = ContentUris.withAppendedId(mediaUri, id);
            FileManagerApp.log(TAG + "Delete File: content uri: " + contentUri + " " + mimeType,
                    false);
            context.getContentResolver().delete(contentUri, null, null);
        }
    }

    public static boolean checkFileToSetScanFlag(File file, boolean scanMediaFiles, Context context) {
        // if mScanMediaFiles is true ,no need to check other files
        if (!scanMediaFiles) {
            if (file.isDirectory()) {
                scanMediaFiles = true;
            } else {
                scanMediaFiles = FileUtils.isMediaFile(context, file.getName());
            }
        }
        return scanMediaFiles;
    }

    public static long getMediaContentIdByFilePath(Context context, String filePath, int mediaMode) {
        long id = 0;
        String[] args = new String[]{filePath};
        String where = null;
        String[] cols = null;
        Cursor cs = null;

        try {
            switch (mediaMode) {
                case 0 :// image
                    where = MediaColumns.DATA + " like ? ";
                    cols = new String[]{BaseColumns._ID,};
                    cs =
                            context.getContentResolver()
                                    .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                            cols,
                                            where,
                                            args,
                                            null);
                    break;
                case 1 :// audio
                    where = MediaColumns.DATA + " like ? ";
                    cols = new String[]{BaseColumns._ID,};
                    cs =
                            context.getContentResolver()
                                    .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                            cols,
                                            where,
                                            args,
                                            null);
                    break;
                case 2 :// video
                    where = MediaColumns.DATA + " like ? ";
                    cols = new String[]{BaseColumns._ID,};
                    cs =
                            context.getContentResolver()
                                    .query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                            cols,
                                            where,
                                            args,
                                            null);
                    break;

                default :
                    return id;
            }
            if (cs != null && cs.getCount() != 0) {
                cs.moveToFirst();
                id = cs.getLong(0);
            }
        } catch (UnsupportedOperationException ex) {
            ex.printStackTrace();
        } finally {
            if (cs != null) {
                cs.close();
                cs = null;
            }
        }

        return id;
    }

    public static boolean isRootDirectory(String dir) {
        if (dir == null || dir.equals(FileManagerApp.ROOT_DIR)/** ||dir.equals(* FileManagerApp.OS_ROOT_DIR)*/
        ) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isZipFile(String fileName) {
        if (fileName == null) {
            return false;
        } else {
            return fileName.endsWith(".zip") || fileName.endsWith(".gz");
        }
    }

    public static Bitmap cropImageFromImageFile(String filePath, Bundle myExtras) {
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
            Bitmap croppedImage =
                    Bitmap.createScaledBitmap(bitmap, myExtras.getInt("outputX", 96), myExtras
                            .getInt("outputY", 96), false);
            if (croppedImage != null) {
                FileManagerApp.log(TAG + "croppedImage is: " + croppedImage.getHeight(), false);
                return croppedImage;
            } else {
                FileManagerApp.log(TAG + "croppedImage is null", true);
            }
        } else {
            FileManagerApp.log(TAG + "srcImage is null", true);
        }
        return null;
    }

    /*This API is for query DB for Media store attributes. It can be used in the future to add more tags
       by adding a 3rd argument That will indicate the attribute we would like to query*/
    public static String getMetadataAttr(File file, Context context) {
        String attribute = null;
        Double longitude = null;
        Double latitude = null;
        Uri mediaUri = null;
        String[] ALL_PROJECTION = null;
        String where = "";
        String mimeType = MimeTypeUtil.getMimeType(context, file.getName());

        if (mimeType != null && (mimeType.indexOf("image") == 0)) {
            mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            ALL_PROJECTION =
                    new String[]{MediaColumns.DATA, MediaStore.Images.ImageColumns.LATITUDE,
                            MediaStore.Images.ImageColumns.LONGITUDE};
            where = "Images._DATA LIKE '" + file.getPath() + "%'";
        }

        else if (mimeType != null && (mimeType.indexOf("video") == 0)) {
            mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            ALL_PROJECTION =
                    new String[]{MediaColumns.DATA, MediaStore.Video.VideoColumns.LATITUDE,
                            MediaStore.Video.VideoColumns.LONGITUDE};
            where = "Video._DATA LIKE '" + file.getPath() + "%'";
        } else /*Audio and other file types do not have the location attribute*/{
            return attribute;
        }

        // Query the image or video Columns by the file path
        Cursor c = context.getContentResolver().query(mediaUri, ALL_PROJECTION, where, null, null);
        if (c != null && c.getCount() > 0) {
            c.moveToFirst();
            //get the longitude and latitude from the column by index
            longitude = c.getDouble(c.getColumnIndex(ImageColumns.LONGITUDE));
            latitude = c.getDouble(c.getColumnIndex(ImageColumns.LATITUDE));
            c.close();
        }
        //Get the String location by reversing longitude and latitude
        Geocoder gc = new Geocoder(context, Locale.getDefault());
        try {
            if (gc != null) {
                List<Address> addresses = gc.getFromLocation(latitude, longitude, 1);
                StringBuilder sb = new StringBuilder();

                if (addresses.size() > 0) {
                    Address address = addresses.get(0);

                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                        sb.append(address.getAddressLine(i)).append("\n");
                    }
                    sb.append(address.getCountryName());
                }
                attribute = sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return attribute;
    }

    public static boolean isPrintIntentHandled(Context context, final File file) {
        boolean return_value = false;
        Intent intent = new Intent("com.motorola.android.intent.action.PRINT");
        intent.setType(MimeTypeUtil.getMimeType(context, file.getName()));
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> localeReceivers = pm.queryIntentActivities(intent, 0);
        if (localeReceivers.isEmpty()) {
            FileManagerApp.log(TAG + "isPrintIntentHandled: no, it is not.", false);
        } else {
            return_value = true;
            FileManagerApp.log(TAG + "isPrintIntentHandled: yes, it is.", false);
        }
        return (return_value);
    }

    public static ArrayList<String> getCopyMoveDestinationPath(ArrayList<String> srcList,
                                                               String destDir) {

        ArrayList<String> resultList = new ArrayList<String>();

        if ((srcList != null) && (destDir != null)) {
            for (String src : srcList) {
                String[] splitBuff = src.split("/");
                if (src.endsWith("/")) {
                    // Add "/" for new path when original path of the moved folder
                    // has "/" at the end so the formats of both match
                    resultList.add(destDir + splitBuff[splitBuff.length - 1] + "/");
                } else {
                    resultList.add(destDir + splitBuff[splitBuff.length - 1]);
                }
            }
        }

        return resultList;
    }
}
