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
import java.text.Collator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.motorola.FileManager.FileManagerActivity;
import com.motorola.FileManager.R;

//import com.zecter.api.parcelable.ZumoFile;

public class IconifiedText {
    private String mText = "";
    private String mInfo = "";
    private boolean mIsOnline = true; // For motoConnect device online/offline
    private String mMCServerId = "";
    // private ZumoFile mZumoFile = null;
    private String mInfoTime = "";
    private Drawable mIcon;
    private long mSize = 0;
    private long mTime = 0;

    private String mAuthInfo = null;
    private boolean mIsChecked = false;
    private boolean mSelectable = true;

    private String mFilePath = "";

    private int mFieldTag = 0; // 1 specially meanings for add server // 2 for
			       // unknown group
    private boolean mIsNormalFile = true;
    private String mMiMeType = NONEMIMETYPE;
    public final static String DIRECTORYMIMETYPE = "DIRECTORYMIMETYPE";
    public final static String DRMMIMETYPE = "DRMMIMETYPE";
    public final static String NONEMIMETYPE = "NONEMIMETYPE";
    public final static String WRONG_TIMEINFO_1 = "Jan 1, 1980";
    public final static String WRONG_TIMEINFO_2 = "Dec 31, 1979";

    public IconifiedText(String text, String info, Drawable bullet) {
	mIcon = bullet;
	mText = text;
	mInfo = info;
	mMiMeType = NONEMIMETYPE;
    }

    public IconifiedText(String text, Drawable bullet) {
	mIcon = bullet;
	mText = text;
	mMiMeType = NONEMIMETYPE;
    }

    public IconifiedText(String text, Drawable bullet, int tag) {
	mIcon = bullet;
	mText = text;
	mMiMeType = NONEMIMETYPE;
	mFieldTag = tag;
    }

    public boolean isSelectable() {
	return mSelectable;
    }

    public void setSelectable(boolean selectable) {
	mSelectable = selectable;
    }

    public int getFieldTag() {
	return mFieldTag;
    }

    public String getText() {
	return mText;
    }

    public void setText(String text) {
	mText = text;
    }

    public void setIcon(Drawable icon) {
	mIcon = icon;
    }

    public Drawable getIcon() {
	return mIcon;
    }

    public String getMiMeType() {
	return mMiMeType;
    }

    public void setMCServerID(String id) {
	mMCServerId = id;
    }

    public String getMCServerID() {
	return mMCServerId;
    }

    /*
     * public void setZumoFile(ZumoFile f) { mZumoFile = f; }
     * 
     * public ZumoFile getZumoFile() { return mZumoFile; }
     */
    public void setInfo(String info) {
	mInfo = info;
    }

    public String getInfo() {
	return mInfo;
    }

    public boolean getIsOnline() {
	return mIsOnline;
    }

    public void setIsOnline(boolean isOnline) {
	mIsOnline = isOnline;
    }

    public void setMiMeType(String mimeType) {
	mMiMeType = mimeType;
    }

    public boolean isChecked() {
	return mIsChecked;
    }

    public void setChecked(boolean ischecked) {
	mIsChecked = ischecked;
    }

    public void setPathInfo(String filepath) {
	mFilePath = filepath;
    }

    public String getPathInfo() {
	return mFilePath;
    }

    public String getTimeInfo() {
	return mInfoTime;
    }

    public void setTimeInfo(String time) {
	if (time.startsWith(WRONG_TIMEINFO_1)
		|| time.startsWith(WRONG_TIMEINFO_2)) {
	    // Not display the wrong time info.
	    mInfoTime = "";
	} else {
	    mInfoTime = time;
	}
    }

    public boolean isNormalFile() {
	return mIsNormalFile;
    }

    public void setIsNormalFile(Boolean isNormalFile) {
	mIsNormalFile = isNormalFile;
    }

    public void setTime(long time) {
	mTime = time;
    }

    public long getTime() {
	return mTime;
    }

    public void setSize(long size) {
	mSize = size;
    }

    public long getSize() {
	return mSize;
    }

    public void setAuthInfo(String authInfo) {
	mAuthInfo = authInfo;
    }

    public String getAuthInfo() {
	return mAuthInfo;
    }

    public static IconifiedText buildIconItem(Context context, String str) {
	String filename = str;
	Drawable fileIcon = null;
	String mimeType = NONEMIMETYPE;
	if (filename.endsWith("/")) {
	    fileIcon = context.getResources().getDrawable(R.drawable.ic_file);
	    mimeType = DIRECTORYMIMETYPE;
	} else {
	    /*String uristring = "file:///" + filename;
	    mimeType = MimeTypeUtil.getMimeType(context, filename);
	    if (mimeType == null) {
		mimeType = NONEMIMETYPE;
	    }
	    Intent tempIntent = new Intent(Intent.ACTION_VIEW);
	    tempIntent.setDataAndType(Uri.parse(uristring), mimeType);
	    try {
		fileIcon = context.getPackageManager().getActivityIcon(
			tempIntent);
	    } catch (android.content.pm.PackageManager.NameNotFoundException ex) {
	    }*/
	    if (fileIcon == null) {
		if (filename.endsWith(".zip")) { // Check if it's .zip file.
		    fileIcon = context.getResources().getDrawable(
			    R.drawable.ic_file);
		} else if (filename.endsWith(".dcf")) { // Check if it's drm
							// file
		    fileIcon = context.getResources().getDrawable(
			    R.drawable.ic_file);
		    mimeType = DRMMIMETYPE;
		} else {
		    mimeType = NONEMIMETYPE;
		    fileIcon = context.getResources().getDrawable(
			    R.drawable.ic_file);
		}
	    }
	}
	String properties = "";
	IconifiedText newIconifiedText = new IconifiedText(filename,
		properties, fileIcon);
	newIconifiedText.setMiMeType(mimeType);
	newIconifiedText.setPathInfo(str);
	return newIconifiedText;
    }

    public static IconifiedText buildIconItem(Context context, File file) {
	Drawable fileIcon = null;
	String mimeType = NONEMIMETYPE;
	String info = "";
	String infoTime = "";
	SimpleDateFormat formatter = (SimpleDateFormat) DateFormat
		.getDateInstance(DateFormat.MEDIUM);
	Date date = new Date();
	long time = file.lastModified();
	long size;
	date.setTime(time);
	infoTime = formatter.format(date);
	String pathInfo = "";

	if (file.isDirectory()) {
	    fileIcon = context.getResources().getDrawable(R.drawable.ic_file);
	    mimeType = DIRECTORYMIMETYPE;
	    if (file.listFiles() == null) {
		size = 0;
	    } else {
		size = file.listFiles().length;
	    }
	    if (size == 1) {
		info = size
			+ " "
			+ context.getResources().getString(
				R.string.empty_folder);
	    } else {
		info = size
			+ " "
			+ context.getResources().getString(
				R.string.empty_folder);
	    }
	    pathInfo = file.getAbsolutePath() + "/";
	} else {
	    /*String uristring = "file:///" + file.getName();
	    mimeType = MimeTypeUtil.getMimeType(context, file.getName());
	    if (mimeType == null) {
		mimeType = NONEMIMETYPE;
	    }
	    Intent tempIntent = new Intent(Intent.ACTION_VIEW);
	    tempIntent.setDataAndType(Uri.parse(uristring), mimeType);
	    try {
		fileIcon = context.getPackageManager().getActivityIcon(
			tempIntent);
	    } catch (android.content.pm.PackageManager.NameNotFoundException ex) {
	    }
	    if (file.getName().endsWith(".apk")) {
		PackageManager pm = context.getPackageManager();
		PackageInfo packageinfo = pm.getPackageArchiveInfo(
			file.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
		if (packageinfo != null) {
		    ApplicationInfo appInfo = packageinfo.applicationInfo;
		    fileIcon = pm.getApplicationIcon(appInfo);
		}
	    }*/
	    // Calculate the file size. If reminder is more than 0 we should
	    // show
	    // increase the size by 1, otherwise we should show the size as it
	    // is.
	    size = file.length() / 1024;
	    long rem = file.length() % 1024;
	    if (rem > 0) {
		size++;
	    }
	    info = context.getResources().getString(R.string.empty_folder,
		    Long.toString(size));
	    pathInfo = file.getAbsolutePath();
	    if (fileIcon == null) {
		if (file.getName().endsWith(".zip")) { // Check if it's .zip
						       // file.
		    fileIcon = context.getResources().getDrawable(
			    R.drawable.ic_file);
		} else if (file.getName().endsWith(".dcf")) { // Check if it's
							      // drm file
		    fileIcon = context.getResources().getDrawable(
			    R.drawable.ic_file);
		    mimeType = DRMMIMETYPE;
		} else {
		    mimeType = NONEMIMETYPE;
		    fileIcon = context.getResources().getDrawable(
			    R.drawable.ic_file);
		}
	    }
	}
	IconifiedText newIconifiedText = new IconifiedText(file.getName(),
		info, fileIcon);
	newIconifiedText.setMiMeType(mimeType);
	newIconifiedText.setPathInfo(pathInfo);
	newIconifiedText.setTimeInfo(infoTime);
	newIconifiedText.setTime(time);
	newIconifiedText.setSize(size);
	return newIconifiedText;
    }

    public static IconifiedText buildDirIconItem(Context context, String str) {
	Drawable fileIcon = null;
	String mimeType = DIRECTORYMIMETYPE;

	if (fileIcon == null) {
	    fileIcon = context.getResources().getDrawable(R.drawable.ic_file);
	}
	IconifiedText newIconifiedText = new IconifiedText(str, fileIcon);
	newIconifiedText.setMiMeType(mimeType);
	newIconifiedText.setPathInfo(str);
	return newIconifiedText;
    }

    public static IconifiedText buildIconItemWithoutInfo(Context context,
	    String str, int tag) {
	String filename = str;
	Drawable fileIcon = null;

	if (tag == 0) {
	    fileIcon = context.getResources().getDrawable(R.drawable.ic_file);
	} else if (tag == 1) {
	    fileIcon = context.getResources().getDrawable(R.drawable.ic_file);
	} else if (tag == 2) {
	    fileIcon = context.getResources().getDrawable(R.drawable.ic_file);
	} else if (tag == 3) {
	    fileIcon = context.getResources().getDrawable(R.drawable.ic_file);
	}

	IconifiedText item = new IconifiedText(filename, fileIcon, tag);
	return item;
    }

    public static Comparator<IconifiedText> returnSort(int sortType) {
	switch (sortType) {
	case FileManagerActivity.NAMESORT:
	    return mNameSort;
	case FileManagerActivity.TYPESORT:
	    return mTypeSort;
	case FileManagerActivity.SIZESORT:
	    return mSizeSort;
	case FileManagerActivity.TIMESORT:
	    return mTimeSort;
	default:
	    return mNameSort;
	}
    }

    public static void sortFiles(ArrayList<IconifiedText> listDir,
	    ArrayList<IconifiedText> listFile) {
	Collections.sort(listDir, mNameSort);
	Collections.sort(listFile, mNameSort);
    }

    private static Comparator<IconifiedText> mNameSort = new Comparator<IconifiedText>() {
	@Override
	public int compare(IconifiedText object1, IconifiedText object2) {
	    if ((object1 != null) && (object2 != null)) {
		Collator aCollator = Collator.getInstance(Locale.US);
		return aCollator.compare(object1.getText().toLowerCase(),
			object2.getText().toLowerCase());
	    }
	    throw new IllegalArgumentException();
	}
    };

    private static Comparator<IconifiedText> mTypeSort = new Comparator<IconifiedText>() {
	@Override
	public int compare(IconifiedText object1, IconifiedText object2) {
	    if ((object1 != null) && (object2 != null)) {
		return object1.getMiMeType().toLowerCase()
			.compareTo(object2.getMiMeType().toLowerCase());
	    }
	    throw new IllegalArgumentException();
	}
    };

    private static Comparator<IconifiedText> mSizeSort = new Comparator<IconifiedText>() {
	@Override
	public int compare(IconifiedText object1, IconifiedText object2) {
	    if ((object1 != null) && (object2 != null)) {
		return object1.getSize() < object2.getSize() ? -1 : (object1
			.getSize() == object2.getSize() ? 0 : 1);
	    }
	    throw new IllegalArgumentException();
	}
    };

    private static Comparator<IconifiedText> mTimeSort = new Comparator<IconifiedText>() {
	@Override
	public int compare(IconifiedText object1, IconifiedText object2) {
	    if ((object1 != null) && (object2 != null)) {
		return object1.getTime() < object2.getTime() ? -1 : (object1
			.getTime() == object2.getTime() ? 0 : 1);
	    }
	    throw new IllegalArgumentException();
	}
    };

}
