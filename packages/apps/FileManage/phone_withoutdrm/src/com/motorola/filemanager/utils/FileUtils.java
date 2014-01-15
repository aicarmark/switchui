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

package com.motorola.filemanager.utils;

import java.io.File;

import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Video;

import android.provider.MediaStore;
import android.widget.Toast;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.provider.Settings;
import android.app.Activity;
import com.motorola.filemanager.R;
import android.util.Log;
import android.database.Cursor;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.local.FileManagerActivity;

public class FileUtils {
    /** TAG for log messages. */
    static final String TAG = "FileUtils";
    // IKAROWANA-81 begin tqr743
    public static final int MIME_NONE = 0;
    public static final int MIME_TEXT = 1;
    public static final int MIME_IMAGE = 2;
    public static final int MIME_VIDEO = 3;
    public static final int MIME_AUDIO = 4;
    public static final int MIME_APK = 5;
    public static final int MIME_HTML = 6;
    public static final int MIME_PDF = 7;
    public static final int MIME_DOC = 8;
    public static final int MIME_PPT = 9;
    public static final int MIME_EXCEL = 10;
    public static final int MIME_ZIP = 11;

    /* set cdma ringtone */
    public static final int NULL_RINGTONE = 0;
    public static final int CDMA_CALL_RINGTONE = 1;
    public static final int GSM_CALL_RINGTONE = 2;
    public static final int CDMA_MESSAGE_RINGTONE = 4;
    public static final int GSM_MESSAGE_RINGTONE = 8;

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
	    return "";
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
	if (uri.startsWith(Audio.Media.INTERNAL_CONTENT_URI.toString())
		|| uri.startsWith(Audio.Media.EXTERNAL_CONTENT_URI.toString())
		|| uri.startsWith(Video.Media.INTERNAL_CONTENT_URI.toString())
		|| uri.startsWith(Video.Media.EXTERNAL_CONTENT_URI.toString())) {
	    return true;
	} else {
	    return false;
	}
    }

    public static boolean isMediaFile(Context context, String fileName) {
	String mimeType = MimeTypeUtil.getMimeType(context, fileName);
	FileManagerApp.log(TAG + "isMediaFile type: " + " " + mimeType);
	if (mimeType == null) {
	    return false;
	}
	if ((mimeType.indexOf("image") == 0)
		|| (mimeType.indexOf("video") == 0)
		|| (mimeType.indexOf("audio") == 0)) {
	    return true;
	} else {
	    return false;
	}
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

    public static boolean needQueryDB(String ext) {
	if (ext.equals(".3gpp") || ext.equals(".3gp") || ext.equals(".mp4"))
	    return true;
	else
	    return false;
    }

    public static String getMediaMimeTypeByDataBase(Context context, String path) {
	if (path == null)
	    return null;
	Cursor cursor = null;
	String mimeType = null;

	try {
	    String[] cols = new String[] { MediaStore.Audio.Media._ID, };
	    String selection = MediaStore.Audio.Media.DATA + " like ?";
	    String[] selectArgs = new String[] { path };
	    cursor = query(context,
		    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols,
		    selection, selectArgs, null);

	    if (cursor != null && cursor.getCount() != 0) {
		mimeType = "audio/*";
	    }

	    String[] cols1 = new String[] { MediaStore.Video.Media._ID, };
	    String selection1 = MediaStore.Video.Media.DATA + " like ?";
	    String[] selectArgs1 = new String[] { path };
	    cursor = query(context,
		    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols1,
		    selection1, selectArgs1, null);
	    if (cursor != null && cursor.getCount() != 0) {
		mimeType = "video/*";
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	} finally {
	    if (cursor != null) {
		cursor.close();
		cursor = null;
	    }
	}
	return mimeType;
    }

    public static int getMimeId(String mimeType) {
	if (mimeType == null)
	    return MIME_NONE;
	if (mimeType.indexOf("text") == 0) {
	    if (mimeType.equals("text/htm") || mimeType.equals("text/html"))
		return MIME_HTML;
	    else
		return MIME_TEXT;
	}

	if (mimeType.indexOf("image") == 0)
	    return MIME_IMAGE;
	if (mimeType.indexOf("video") == 0)
	    return MIME_VIDEO;
	if (mimeType.indexOf("audio") == 0)
	    return MIME_AUDIO;
	if (mimeType.indexOf("application") == 0) {
	    if (mimeType.equals("application/vnd.android.package-archive"))
		return MIME_APK;
	    if (mimeType.equals("application/pdf"))
		return MIME_PDF;
	    if (mimeType.equals("application/msword")
		    || mimeType
			    .equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
		return MIME_DOC;
	    if (mimeType.equals("application/vnd.ms-powerpoint")
		    || mimeType
			    .equals("application/vnd.openxmlformats-officedocument.presentationml.presentation"))
		return MIME_PPT;
	    if (mimeType.equals("application/vnd.ms-excel")
		    || mimeType
			    .equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
		return MIME_EXCEL;
	    if (mimeType.equals("application/zip"))
		return MIME_ZIP;
	}
	return MIME_NONE;
    }

    public static int getMimeId(Context context, File fi) {
	String ext = getExtension(fi.getName());
	String mimeType = MimeTypeUtil.getMimeType(context, ext);

	if (isMediaFile(context, fi.getName())
		&& needQueryDB(ext.toLowerCase())) {
	    String t = getMediaMimeTypeByDataBase(context, fi.getPath());
	    if (t != null)
		mimeType = t;

	}
	return FileUtils.getMimeId(mimeType);
    }

    public static boolean isInAudioDatabase(Context context, String ring) {
	if (ring == null)
	    return false;
	Cursor cursor = null;
	boolean isAudio = false;

	try {
	    String[] cols = new String[] { MediaStore.Audio.Media._ID, };
	    String selection = MediaStore.Audio.Media.DATA + " like ?";
	    String[] selectArgs = new String[] { ring };
	    cursor = query(context,
		    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols,
		    selection, selectArgs, null);
	    isAudio = (cursor != null && cursor.getCount() != 0);
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	} finally {
	    if (cursor != null) {
		cursor.close();
		cursor = null;
	    }
	}

	return isAudio;
    }

    public static boolean isInImageDatabase(Context context, String imageFile) {
	if (imageFile == null) {
	    return false;
	}

	Cursor cursor = null;
	boolean isImage = false;

	try {
	    String[] cols = new String[] { MediaStore.Images.Media._ID, };
	    String selection = MediaStore.Images.Media.DATA + " like ?";
	    String[] selectArgs = new String[] { imageFile };
	    cursor = query(context,
		    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cols,
		    selection, selectArgs, null);

	    isImage = (cursor != null && cursor.getCount() != 0);
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	} finally {
	    if (cursor != null) {
		cursor.close();
		cursor = null;
	    }
	}

	return isImage;
    }

    public static void setAs(Activity activity, File file, int type) {
	if (!file.exists()) {
	    Toast.makeText(
		    activity,
		    activity.getString(R.string.file_not_exist, file.getName()),
		    Toast.LENGTH_LONG).show();
	    return;
	}

	String mimeType = MimeTypeUtil.getMimeType(activity,
		getExtension(file.getName()));

	if (FileUtils.getMimeId(mimeType) == FileUtils.MIME_AUDIO) {
	    if (NULL_RINGTONE != type
		    && setRingtone(activity, file.getAbsolutePath(), type)) {
		if ((CDMA_CALL_RINGTONE == (type & CDMA_CALL_RINGTONE))
			|| (GSM_CALL_RINGTONE == (type & GSM_CALL_RINGTONE)))
		    Toast.makeText(
			    activity,
			    activity.getString(
				    R.string.success_set_call_ringstone,
				    file.getName()), Toast.LENGTH_SHORT).show();
		if (FileManagerActivity.mSupportDualMode) {
		    if (CDMA_MESSAGE_RINGTONE == (type & CDMA_MESSAGE_RINGTONE))
			Toast.makeText(
				activity,
				activity.getString(
					R.string.success_set_cdma_message_ringstone,
					file.getName()), Toast.LENGTH_SHORT)
				.show();
		    if (GSM_MESSAGE_RINGTONE == (type & GSM_MESSAGE_RINGTONE))
			Toast.makeText(
				activity,
				activity.getString(
					R.string.success_set_gsm_message_ringstone,
					file.getName()), Toast.LENGTH_SHORT)
				.show();
		} else {
		    if (GSM_MESSAGE_RINGTONE == (type & GSM_MESSAGE_RINGTONE))
			Toast.makeText(
				activity,
				activity.getString(
					R.string.success_set_message_ringstone,
					file.getName()), Toast.LENGTH_SHORT)
				.show();
		}
	    }
	    return;
	}

	if (FileUtils.getMimeId(mimeType) == FileUtils.MIME_IMAGE) {
	    Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
	    intent.setDataAndType(Uri.fromFile(file), mimeType);
	    intent.putExtra("mimeType", mimeType);
	    activity.startActivity(Intent.createChooser(intent,
		    activity.getText(R.string.setImage)));
	}
    }

    public static Cursor query(Context context, Uri uri, String[] projection,
	    String selection, String[] selectionArgs, String sortOrder) {

	try {
	    ContentResolver resolver = context.getContentResolver();
	    if (resolver == null) {
		return null;
	    }
	    return resolver.query(uri, projection, selection, selectionArgs,
		    sortOrder);
	} catch (UnsupportedOperationException ex) {
	    return null;
	}
    }

    static boolean setRingtone(Context context, String ring, int ringtone_type) {

	if (ring == null)
	    return false;

	Cursor cursor = null;
	boolean result = true;

	try {
	    ContentResolver resolver = context.getContentResolver();
	    String[] cols = new String[] { MediaStore.Audio.Media._ID,
		    MediaStore.Audio.Media.DATA, };
	    String selection = MediaStore.Audio.Media.DATA + " like ?";
	    String[] selectArgs = new String[] { ring };
	    cursor = query(context,
		    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols,
		    selection, selectArgs, null);
	    int id = -1;
	    if (cursor == null || cursor.getCount() == 0) {
		result = false;
		Log.d(TAG, "filemanager set ringtone no record" + cursor);
		// if this file still not sync in media's
		// database, we
		// will not allow to set it into database
	    } else {
		cursor.moveToFirst();
		id = cursor.getInt(0);

		Uri ringUri = ContentUris.withAppendedId(
			MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

		if (CDMA_CALL_RINGTONE == (ringtone_type & CDMA_CALL_RINGTONE)) {
		    ContentValues values = new ContentValues(2);
		    values.put(MediaStore.Audio.Media.IS_RINGTONE, "1");
		    values.put(MediaStore.Audio.Media.IS_ALARM, "1");
		    resolver.update(ringUri, values, null, null);
		    Settings.System.putString(resolver,
			    Settings.System.RINGTONE, ringUri.toString());
		}
		if (GSM_CALL_RINGTONE == (ringtone_type & GSM_CALL_RINGTONE)) {
		    ContentValues values = new ContentValues(2);
		    values.put(MediaStore.Audio.Media.IS_RINGTONE, "1");
		    values.put(MediaStore.Audio.Media.IS_ALARM, "1");
		    resolver.update(ringUri, values, null, null);
		    if (FileManagerActivity.mSupportDualMode) {
			Settings.System.putString(resolver, "gsm_ringtone",
				ringUri.toString());
		    } else {
			Settings.System.putString(resolver, "ringtone",
				ringUri.toString());
		    }
		}
		if (CDMA_MESSAGE_RINGTONE == (ringtone_type & CDMA_MESSAGE_RINGTONE)) {
		    ContentValues values = new ContentValues(1);
		    values.put(MediaStore.Audio.Media.IS_NOTIFICATION, "1");
		    resolver.update(ringUri, values, null, null);
		    Settings.System.putString(resolver,
			    "cdma_message_ringtone", ringUri.toString());
		}
		if (GSM_MESSAGE_RINGTONE == (ringtone_type & GSM_MESSAGE_RINGTONE)) {
		    ContentValues values = new ContentValues(1);
		    values.put(MediaStore.Audio.Media.IS_NOTIFICATION, "1");
		    resolver.update(ringUri, values, null, null);
		    Settings.System.putString(resolver, "gsm_message_ringtone",
			    ringUri.toString());
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	} finally {
	    if (cursor != null) {
		cursor.close();
		cursor = null;
	    }
	}

	return result;
    }
}
