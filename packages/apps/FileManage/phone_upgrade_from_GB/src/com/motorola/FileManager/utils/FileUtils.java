/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date       CR                Author      Description
 * 2010-03-23   IKSHADOW-2425   A20815      initial
 */

package com.motorola.FileManager.utils;

import java.io.File;

import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Video;

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
	return uri != null && !uri.startsWith("http://");
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
	return uri.startsWith(Audio.Media.INTERNAL_CONTENT_URI.toString())
		|| uri.startsWith(Audio.Media.EXTERNAL_CONTENT_URI.toString())
		|| uri.startsWith(Video.Media.INTERNAL_CONTENT_URI.toString())
		|| uri.startsWith(Video.Media.EXTERNAL_CONTENT_URI.toString());
    }

    public static boolean isMediaFile(Context context, String fileName) {
	String mimeType = MimeTypeUtil.getMimeType(context, fileName);

	if (mimeType == null) {
	    return false;
	}
	return (mimeType.indexOf("image") == 0)
		|| (mimeType.indexOf("video") == 0)
		|| (mimeType.indexOf("audio") == 0);
    }

    /**
     * Convert File into Uri.
     * 
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
     * 
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
     * 
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
		String pathwithoutname = filepath.substring(0,
			filepath.length() - filename.length());
		if (pathwithoutname.endsWith("/")) {
		    pathwithoutname = pathwithoutname.substring(0,
			    pathwithoutname.length() - 1);
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
}
