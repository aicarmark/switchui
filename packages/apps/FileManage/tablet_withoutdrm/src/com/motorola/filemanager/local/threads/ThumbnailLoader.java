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

package com.motorola.filemanager.local.threads;

import java.io.File;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;

import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.local.LocalBaseFileManagerFragment;
import com.motorola.filemanager.ui.IconifiedText;
import com.motorola.filemanager.utils.FileUtils;
import com.motorola.filemanager.utils.MimeTypeUtil;

public class ThumbnailLoader extends Thread {
    private static final String TAG = "ThumbnailLoader: ";
    private static final String PROCESSNAME = "com.motorola.filemanager";

    private Context mContext;
    List<IconifiedText> listFile;
    public volatile boolean cancel = false;
    File file;
    Handler handler;

    private int thumbnailWidth = 256;
    private int thumbnailHeight = 256;

    public ThumbnailLoader(List<IconifiedText> list, Handler handler) {
        super("Thumbnail Loader");
        listFile = list;
        File file = null;
        this.file = file;
        this.handler = handler;
    }

    public void setContext(Context mC) {
        mContext = mC;
    }

    public synchronized void requestStop() {
        cancel = true;
        FileManagerApp.log(TAG + "thumbnail Request stop", false);
        return;
    }

    @Override
    public void run() {
        if (cancel) {
            FileManagerApp.log(TAG + "Thumbnail loader canceled", false);
            listFile = null;
            mContext = null;
            return;
        }
        int count = listFile.size();
        if (count == 0) {
            FileManagerApp.log(TAG + "count is zero", false);
            listFile = null;
            mContext = null;
            return;
        }
        if (mContext == null) {
            return;
        }
        ActivityManager activityManager =
                (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();

        int pids[] = new int[1];
        for (RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
            if (runningAppProcessInfo.processName.equals(PROCESSNAME)) {
                pids[0] = runningAppProcessInfo.pid;
                break;
            }
        }

        int heapSize = activityManager.getMemoryClass();
        BitmapFactory.Options options = new BitmapFactory.Options();
        FileManagerApp.log(TAG + "Scanning for thumbnails (files=" + count + ")", false);
        for (int x = 0; x < count; x++) {
            if (cancel) {
                FileManagerApp.log(TAG + "Thumbnail loader canceled", false);
                listFile = null;
                mContext = null;
                return;
            }
            android.os.Debug.MemoryInfo[] memoryInfoArray =
                    activityManager.getProcessMemoryInfo(pids);
            int totalUsedPss = memoryInfoArray[0].getTotalPss();
            if (totalUsedPss > (heapSize - 1) * 1024) {
                FileManagerApp.log(TAG + "File manager memory is low, exit!", true);
                return;
            }
            IconifiedText text = null;
            try {
                text = listFile.get(x);
                file = (FileUtils.getFile(text.getPathInfo())).getParentFile();
            } catch (Exception e) {
                FileManagerApp.log(TAG + "Outofbounds", true);
                listFile = null;
                mContext = null;
                return;
            }
            if (file != null) {
                if (FileUtils.getFile(file, text.getText()).isDirectory()) {
                    continue;
                }
            }
            String fileNameWithPath = text.getPathInfo();
            File mFile = new File(fileNameWithPath);
            String mimeType = MimeTypeUtil.getMimeType(mContext, mFile.getName());

            //blm013: porting of IKSTABLEFOURV-9320 for Video
            Bitmap bitmap = null;
            // We only obtain thumbnail for image and video thumbnail
            if (mimeType == null) {
                continue;
            } else if (mimeType.indexOf("image") == 0) { // thumbnail for image
                try {
                    options.inJustDecodeBounds = true;
                    options.outWidth = 0;
                    options.outHeight = 0;
                    options.inSampleSize = 1;
                    BitmapFactory.decodeFile(FileUtils.getFile(file, text.getText()).getPath(),
                            options);

                    if (options.outWidth > 0 && options.outHeight > 0) {
                        int widthFactor = (options.outWidth + thumbnailWidth - 1) / thumbnailWidth;
                        int heightFactor =
                                (options.outHeight + thumbnailHeight - 1) / thumbnailHeight;
                        widthFactor = Math.max(widthFactor, heightFactor);
                        widthFactor = Math.max(widthFactor, 1);

                        options.inSampleSize = 1;
                        if (widthFactor > 1) {
                            while (widthFactor > 0) {
                                widthFactor >>= 1;
                                options.inSampleSize <<= 1;
                            }
                        }
                        options.inJustDecodeBounds = false;
                        options.inPurgeable = true;
                        byte[] tempStorage = new byte[16 * 1024];
                        options.inTempStorage = tempStorage; //Use tempstorage to decode

                        bitmap =
                                BitmapFactory.decodeFile(FileUtils.getFile(file, text.getText())
                                        .getPath(), options);
                    }
                } catch (Exception e) {
                    // That's okay, guess it just wasn't a bitmap.
                } catch (OutOfMemoryError e) {
                    FileManagerApp.log(TAG + "Thumbnail loader out of memory", true);
                    cancel = true;
                    return;
                }
            } else if (mimeType.indexOf("video") == 0) { // thumbnail for video
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(FileUtils.getFile(file, text.getText()).getPath());
                    bitmap = retriever.getFrameAtTime();
                } catch (Exception ex) {
                    FileManagerApp.log(TAG + "Video thumbnail is not retrieved", true);
                } finally {
                    retriever.release();
                }
            } else if (mimeType.indexOf("audio") == 0) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(FileUtils.getFile(file, text.getText()).getPath());
                    byte[] albumArt = retriever.getEmbeddedPicture();
                    bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length);
                } catch (Exception ex) {
                    FileManagerApp.log(TAG + "Album Art is not retrieved", true);
                } finally {
                    retriever.release();
                }
            }

            else {
                continue;
            }

            if (bitmap != null) {
                BitmapDrawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);
                drawable.setGravity(Gravity.CENTER);
                drawable.setBounds(0, 0, thumbnailWidth, thumbnailHeight);
                FileManagerApp.log(TAG + "thumbnail =" + x, false);
                //Message  msg = handler.obtainMessage(FileManagerActivity.MESSAGE_ICON_CHANGED);
                Message msg =
                        handler.obtainMessage(LocalBaseFileManagerFragment.MESSAGE_ICON_CHANGED, x,
                                (int) (this.getId()));
                msg.obj = drawable;
                if (!cancel) {
                    //text.setIcon(drawable);
                    msg.sendToTarget();
                }
            }
        }

        FileManagerApp.log(TAG + "Done scanning for thumbnails", false);
        listFile = null;
        mContext = null;
    }
}
