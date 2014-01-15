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
package com.motorola.filemanager.ui;

import java.io.File;
import java.text.Collator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.R;
import com.motorola.filemanager.utils.MimeTypeUtil;

public class IconifiedText {
    private String mText = "";
    private String mInfo = "";
    private boolean mIsOnline = true; // For motoConnect device online/offline
    private String mInfoTime = "";
    private int mInfoDay = 0;
    private int mMode = FileManagerApp.INDEX_INTERNAL_MEMORY;
    private Drawable mIcon;
    private long mSize = 0;
    private long mTime = 0;

    private String mAuthInfo = null;
    private boolean mIsChecked = false;
    private boolean mSelectable = true;
    private boolean mIsHighlighted = false;
    private boolean mIsTopColumnMode = true;

    private String mFilePath = "";

    private int mFieldTag = 0; // 1 specially meanings for add server // 2 for
    // unknown group
    private boolean mIsNormalFile = true;
    private boolean mIsDirectory = true;
    private String mMiMeType = NONEMIMETYPE;
    public final static String DIRECTORYMIMETYPE = "DIRECTORYMIMETYPE";
    public final static String NONEMIMETYPE = "NONEMIMETYPE";
    public final static String WRONG_TIMEINFO_1 = "Jan 1, 1980";
    public final static String WRONG_TIMEINFO_2 = "Dec 31, 1979";
    private static Context mContext;
    private static FileManagerApp mFileManagerApp;
    private int mOnDropLocationTotal;

    public IconifiedText(String text, String info, Drawable bullet, int mode) {
        mIcon = bullet;
        mText = text;
        mInfo = info;
        mMiMeType = NONEMIMETYPE;
        mMode = mode;
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

    public IconifiedText(String filename, int mode, Drawable icon) {
        // TODO Auto-generated constructor stub
        mIcon = icon;
        mText = filename;
        mMode = mode;

    }

    public int getStorageMode() {
        return mMode;
    }

    public void setHighlighted(boolean isHighlighted) {
        mIsHighlighted = isHighlighted;
    }

    public boolean isHighlighted() {
        return mIsHighlighted;
    }

    public void setOpenFolderIcon(Context context) {
        if (isDirectory()) {
            setIcon(context.getResources().getDrawable(R.drawable.ic_launcher_folder_open));
        }
    }

    public void clearOpenFolderIcon(Context context) {
        if (isDirectory()) {
            setIcon(context.getResources().getDrawable(R.drawable.ic_launcher_folder));
        }
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

    public String getTypeDesc(Context context) {
        String typeDesc = context.getString(R.string.typ_file);
        if (mMiMeType != null) {
            if (mMiMeType.equals(DIRECTORYMIMETYPE)) {
                typeDesc = context.getString(R.string.typ_folder);
            } else if (mMiMeType.startsWith("image/")) {
                typeDesc = context.getString(R.string.typ_image);
            } else if (mMiMeType.startsWith("audio/")) {
                typeDesc = context.getString(R.string.typ_audio);
            } else if (mMiMeType.startsWith("video/")) {
                typeDesc = context.getString(R.string.typ_video);
            } else if (mMiMeType.contains("html")) {
                // Check html before text since html mime type is "text/html"
                typeDesc = context.getString(R.string.typ_html);
            } else if (mMiMeType.startsWith("text/")) {
                typeDesc = context.getString(R.string.typ_text);
            } else if (mMiMeType.contains("msword") || mMiMeType.contains("wordprocessing")) {
                typeDesc = context.getString(R.string.typ_doc);
            } else if (mMiMeType.contains("ms-excel") || mMiMeType.contains("spreadsheet")) {
                typeDesc = context.getString(R.string.typ_xml);
            } else if (mMiMeType.contains("ms-powerpoint") || mMiMeType.contains("presentation")) {
                typeDesc = context.getString(R.string.typ_ppt);
            } else if (mMiMeType.contains("pdf")) {
                typeDesc = context.getString(R.string.typ_pdf);
            } else if (mMiMeType.contains("ms-outlook")) {
                typeDesc = context.getString(R.string.typ_msg);
            } else if (mMiMeType.contains("zip")) {
                typeDesc = context.getString(R.string.typ_zip);
            }
        }
        return typeDesc;
    }

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

    public void setIsTopColumn(boolean isTopcolumn) {
        mIsTopColumnMode = isTopcolumn;
    }

    public boolean isTopColumn() {
        return mIsTopColumnMode;
    }

    public void setPathInfo(String filepath) {
        mFilePath = filepath;
    }

    public String getPathInfo() {
        return mFilePath;
    }

    public int getDayInfo() {
        return mInfoDay;
    }

    public String getTimeInfo() {
        return mInfoTime;
    }

    public void setTimeInfo(String time) {
        if (time.startsWith(WRONG_TIMEINFO_1) || time.startsWith(WRONG_TIMEINFO_2)) {
            // Not display the wrong time info.
            mInfoTime = "";
        } else {
            mInfoTime = time;
        }
    }

    public void setDayInfo(int day) {

        mInfoDay = day;
        // }
    }

    public String getSizeInfo() {
        return mInfoTime;
    }

    public void setSizeInfo(String size) {
    }

    public boolean isNormalFile() {
        return mIsNormalFile;
    }

    public void setIsNormalFile(Boolean isNormalFile) {
        mIsNormalFile = isNormalFile;
    }

    public void setIsDirectory(Boolean isDirectory) {
        mIsDirectory = isDirectory;
    }

    public boolean isDirectory() {
        return mIsDirectory;
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

    public static IconifiedText buildHomePageItems(Context context, int mode, String filename) {
        Drawable fileIcon =
                context.getResources().getDrawable(R.drawable.ic_thb_mimetype_unknown_file);
        mContext = context;
        mFileManagerApp = ((FileManagerApp) mContext.getApplicationContext());
        switch (mode) {
            case FileManagerApp.INDEX_INTERNAL_MEMORY :
                fileIcon = context.getResources().getDrawable(R.drawable.ic_thb_device);
                break;
            case FileManagerApp.INDEX_SD_CARD :
                fileIcon = context.getResources().getDrawable(R.drawable.ic_thb_external_sdcard);
                break;
            case FileManagerApp.INDEX_SAMBA :
                fileIcon = context.getResources().getDrawable(R.drawable.ic_thb_shared_folder);
                break;
            case FileManagerApp.INDEX_USB :
                fileIcon = context.getResources().getDrawable(R.drawable.ic_thb_usb_disk);
                break;
            default :
                break;
        }
        IconifiedText newIconifiedText = new IconifiedText(filename, mode, fileIcon);
        return newIconifiedText;
    }

    public static IconifiedText buildIconItem(Context context, String str, int mode) {
        String filename = str;
        Drawable fileIcon = null;
        String mimeType = NONEMIMETYPE;
        if (filename.endsWith("/")) {
            fileIcon = context.getResources().getDrawable(R.drawable.ic_launcher_folder);
            mimeType = DIRECTORYMIMETYPE;
        } else {
            mimeType = MimeTypeUtil.getMimeType(context, filename);
            if (mimeType == null) {
                mimeType = NONEMIMETYPE;
            }
            fileIcon = getFileIcon(context, null, filename, mimeType);
            if (fileIcon == null) {
                mimeType = NONEMIMETYPE;
                fileIcon =
                        context.getResources().getDrawable(R.drawable.ic_thb_mimetype_unknown_file);
            }
        }
        String properties = "";
        IconifiedText newIconifiedText = new IconifiedText(filename, properties, fileIcon, mode);
        newIconifiedText.setMiMeType(mimeType);
        newIconifiedText.setPathInfo(str);
        return newIconifiedText;
    }

    private static String getDeviceName(String path) {
        int pos = 0;
        String name = "";
        if (path == null) {
            return name;
        }
        try {
            pos = path.indexOf(mFileManagerApp.SD_CARD_DIR);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (pos == 0) {
            name = mContext.getString(R.string.internal_device_storage);
            return name;
        }
        try {
            pos = path.indexOf(((FileManagerApp) mContext.getApplicationContext()).SD_CARD_EXT_DIR);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (pos == 0) {
            name = mContext.getString(R.string.sd_card);
            return name;
        }
        try {
            for (int i = 0; i < FileManagerApp.MAX_USB_DISK_NUM; i++) {
                if (path.indexOf(mFileManagerApp.mUsbDiskListPath[i]) == 0) {
                    name = mContext.getString(R.string.usb_storage) + " " + (i + 1);
                    return name;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return name;
    }

    public static IconifiedText buildShortcutItem(Context context, String shortcutName,
                                                  String shortcutPath) {
        Drawable fileIcon =
                context.getResources().getDrawable(R.drawable.ic_launcher_shortcut_folder);
        String mimeType = NONEMIMETYPE;
        String info = getDeviceName(shortcutPath);

        IconifiedText newIconifiedText = new IconifiedText(shortcutName, fileIcon);
        newIconifiedText.setMiMeType(mimeType);
        newIconifiedText.setPathInfo(shortcutPath);
        newIconifiedText.setInfo(info);
        newIconifiedText.setIsDirectory(true);
        return newIconifiedText;
    }

    public static IconifiedText buildRecentFileItem(Context context, String filePath) {
        mContext = context;
        mFileManagerApp = ((FileManagerApp) mContext.getApplicationContext());
        Drawable fileIcon =
                context.getResources().getDrawable(R.drawable.ic_thb_mimetype_unknown_file);
        String mimeType = NONEMIMETYPE;
        String info = getDeviceName(filePath);
        File file = new File(filePath);
        if (file.exists()) {
            if (file.isDirectory()) {
            } else {
                mimeType = MimeTypeUtil.getMimeType(context, file.getName());
                if (mimeType == null) {
                    mimeType = NONEMIMETYPE;
                }
                fileIcon = getFileIcon(context, file, file.getName(), mimeType);
                if (fileIcon == null) {
                    mimeType = NONEMIMETYPE;
                    fileIcon =
                            context.getResources().getDrawable(
                                    R.drawable.ic_thb_mimetype_unknown_file);
                }
            }
            if (fileIcon == null) {
                // Check if it's .zip file.
                if (file.getName().endsWith(".zip")) {
                    fileIcon = context.getResources().getDrawable(R.drawable.ic_thb_mimetype_zip);
                } else {
                    mimeType = NONEMIMETYPE;
                    fileIcon =
                            context.getResources().getDrawable(
                                    R.drawable.ic_thb_mimetype_unknown_file);
                }
            }
            IconifiedText newIconifiedText = new IconifiedText(file.getName(), fileIcon);
            newIconifiedText.setInfo(info);
            newIconifiedText.setPathInfo(filePath);
            return newIconifiedText;
        } else {
            int last = filePath.lastIndexOf("/");
            String text = filePath;

            if (last >= 0 && last < filePath.length() - 1) {
                text = filePath.substring(last + 1);
            }
            IconifiedText newIconifiedText = new IconifiedText(text, fileIcon);
            newIconifiedText.setInfo(info);
            newIconifiedText.setPathInfo(filePath);
            return newIconifiedText;
        }
    }

    public static IconifiedText buildIconItem(Context context, File file, int mode) {
        Drawable fileIcon = null;
        String mimeType = NONEMIMETYPE;
        String info = "";
        String infoTime = "";
        SimpleDateFormat formatter =
                (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM);
        // SimpleDateFormat formatter1 = (SimpleDateFormat)
        // DateFormat.getDateInstance(DateFormat.DAY_OF_WEEK_FIELD);
        Date date = new Date();
        long time = file.lastModified();
        long size;
        date.setTime(time);
        Calendar cal = Calendar.getInstance();
        // cal.setTime(time);

        // date.g
        // Calendar.get()
        infoTime = formatter.format(date);
        cal.setTime(date);
        int dt = cal.get(Calendar.DATE);
        String pathInfo = "";
        // infoDay = formatter1.format(date);
        if (file.isDirectory()) {
            fileIcon = context.getResources().getDrawable(R.drawable.ic_launcher_folder);
            mimeType = DIRECTORYMIMETYPE;
            if (file.listFiles() == null) {
                size = 0;
            } else {
                size = file.listFiles().length;
            }
            info = context.getResources().getQuantityString(R.plurals.item, (int) size, (int) size);
            pathInfo = file.getAbsolutePath() + "/";
        } else {
            mimeType = MimeTypeUtil.getMimeType(context, file.getName());
            if (mimeType == null) {
                mimeType = NONEMIMETYPE;
            }
            fileIcon = getFileIcon(context, file, file.getName(), mimeType);
            if (fileIcon == null) {
                mimeType = NONEMIMETYPE;
                fileIcon =
                        context.getResources().getDrawable(R.drawable.ic_thb_mimetype_unknown_file);
            }

            // Calculate the file size. If reminder is more than 0 we should show
            // increase the size by 1, otherwise we should show the size as it is.
            size = file.length() / 1024;
            long rem = file.length() % 1024;
            if (rem > 0) {
                size++;
            }
            info = context.getResources().getString(R.string.size_kb, Long.toString(size));
            pathInfo = file.getAbsolutePath();
        }
        IconifiedText newIconifiedText = new IconifiedText(file.getName(), info, fileIcon, mode);
        newIconifiedText.setMiMeType(mimeType);
        newIconifiedText.setPathInfo(pathInfo);
        newIconifiedText.setTimeInfo(infoTime);
        newIconifiedText.setTime(time);
        newIconifiedText.setDayInfo(dt);
        newIconifiedText.setSize(size);
        newIconifiedText.setIsDirectory(file.isDirectory());

        return newIconifiedText;
    }

    private static Drawable getFileIcon(Context context, File file, String fileName, String mimeType) {
        Drawable fileIcon = null;

        if (mimeType.contains("image/")) {
            fileIcon = context.getResources().getDrawable(R.drawable.ic_missing_thumbnail_picture);
        } else if (mimeType.contains("video/")) {
            fileIcon = context.getResources().getDrawable(R.drawable.ic_missing_thumbnail_video);
        } else if (mimeType.contains("audio/")) {
            fileIcon = context.getResources().getDrawable(R.drawable.ic_missing_thumbnail_music);
        } else if (mimeType.contains("html")) {
            // Check html before text since html mime type is "text/html"
            fileIcon = context.getResources().getDrawable(R.drawable.ic_thb_mimetype_html);
        } else if (mimeType.startsWith("text/")) {
            fileIcon = context.getResources().getDrawable(R.drawable.ic_thb_mimetype_txt);
        } else if (mimeType.contains("msword") || mimeType.contains("wordprocessing")) {
            fileIcon = context.getResources().getDrawable(R.drawable.ic_thb_mimetype_document);
        } else if (mimeType.contains("ms-excel") || mimeType.contains("spreadsheet")) {
            fileIcon = context.getResources().getDrawable(R.drawable.ic_thb_mimetype_spreadsheet);
        } else if (mimeType.contains("ms-powerpoint") || mimeType.contains("presentation")) {
            fileIcon = context.getResources().getDrawable(R.drawable.ic_thb_mimetype_presentation);
        } else if (mimeType.contains("pdf")) {
            fileIcon = context.getResources().getDrawable(R.drawable.ic_thb_mimetype_pdf);
        } else if (mimeType.contains("ms-outlook")) {
            fileIcon = context.getResources().getDrawable(R.drawable.ic_thb_mimetype_outlook);
        } else if ((file != null) && (file.getName().endsWith(".apk"))) {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageinfo =
                    pm.getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_ACTIVITIES);
            if (packageinfo != null) {
                ApplicationInfo appInfo = packageinfo.applicationInfo;
                fileIcon = pm.getApplicationIcon(appInfo);
            }
        } else if (mimeType.contains("zip")) {
            fileIcon = context.getResources().getDrawable(R.drawable.ic_thb_mimetype_zip);
        } else {
            String uristring = "file:///" + fileName;
            Intent tempIntent = new Intent(Intent.ACTION_VIEW);
            tempIntent.setDataAndType(Uri.parse(uristring), mimeType);
            try {
                fileIcon = context.getPackageManager().getActivityIcon(tempIntent);
            } catch (android.content.pm.PackageManager.NameNotFoundException ex) {
            }
        }
        return fileIcon;
    }

    public static IconifiedText buildDirIconItem(Context context, String str) {
        Drawable fileIcon = null;
        String mimeType = DIRECTORYMIMETYPE;

        if (fileIcon == null) {
            fileIcon = context.getResources().getDrawable(R.drawable.ic_launcher_folder);
        }
        IconifiedText newIconifiedText = new IconifiedText(str, fileIcon);
        newIconifiedText.setMiMeType(mimeType);
        newIconifiedText.setPathInfo(str);
        return newIconifiedText;
    }

    public static IconifiedText buildIconItemWithoutInfo(Context context, String str, int tag) {
        String filename = str;
        Drawable fileIcon = null;

        if (tag == 0) {
            fileIcon = context.getResources().getDrawable(R.drawable.ic_thb_computer);
        } else if (tag == 1) {
            fileIcon = context.getResources().getDrawable(R.drawable.ic_thb_add);
        } else if (tag == 2) {
            fileIcon = context.getResources().getDrawable(R.drawable.ic_launcher_folder);
        }
        IconifiedText item = new IconifiedText(filename, fileIcon, tag);
        return item;
    }

    public static Comparator<IconifiedText> mNameSort = new Comparator<IconifiedText>() {
        public int compare(IconifiedText object1, IconifiedText object2) {
            if ((object1 != null) && (object2 != null)) {
                Collator aCollator = Collator.getInstance();
                return aCollator.compare(object1.getText().toLowerCase(), object2.getText()
                        .toLowerCase());
            }
            throw new IllegalArgumentException();
        }
    };

    public static Comparator<IconifiedText> mTypeSort = new Comparator<IconifiedText>() {
        public int compare(IconifiedText object1, IconifiedText object2) {
            if ((object1 != null) && (object2 != null)) {
                return object1.getTypeDesc(mContext).toLowerCase()
                        .compareTo(object2.getTypeDesc(mContext).toLowerCase());
            }
            throw new IllegalArgumentException();
        }
    };

    public static Comparator<IconifiedText> mSizeSort = new Comparator<IconifiedText>() {
        public int compare(IconifiedText object1, IconifiedText object2) {
            if ((object1 != null) && (object2 != null)) {
                return object1.getSize() < object2.getSize() ? -1 : (object1.getSize() == object2
                        .getSize() ? 0 : 1);
            }
            throw new IllegalArgumentException();
        }
    };

    public static Comparator<IconifiedText> mTimeSort = new Comparator<IconifiedText>() {
        public int compare(IconifiedText object1, IconifiedText object2) {
            if ((object1 != null) && (object2 != null)) {
                return object1.getTime() < object2.getTime() ? -1 : (object1.getTime() == object2
                        .getTime() ? 0 : 1);
            }
            throw new IllegalArgumentException();
        }
    };

    public void enterOnDropLocation() {
        //Increment the value since we are still on that text for this ondropLocation
        //From OnDrag event
        mOnDropLocationTotal++;
    }

    public void resetDropLocVal() {
        mOnDropLocationTotal = 0;
    }

    public int getDropLocVal() {
        return mOnDropLocationTotal;
    }

}
