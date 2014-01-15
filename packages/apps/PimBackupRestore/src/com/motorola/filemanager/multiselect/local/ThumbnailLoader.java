/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date       CR                Author      Description
 * 2010-03-23   IKSHADOW-2425   A20815      initial
 * 2011-03-22   IKSTABLEFOUR-8621 dtw768    Added memory check to make thumbnail thread exit when available heap memory is less than 1M
 */

package com.motorola.filemanager.multiselect.local;

import java.io.File;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;

import com.motorola.filemanager.multiselect.FileManagerApp;
import com.motorola.filemanager.multiselect.utils.FileUtils;
import com.motorola.filemanager.multiselect.utils.IconifiedText;
import com.motorola.filemanager.multiselect.utils.MimeTypeUtil;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Thumbnails;

public class ThumbnailLoader extends Thread {
  private static final String TAG = "ThumbnailLoader: ";
  private static final String PROCESSNAME = "com.motorola.filemanager";
  private static int mMemoryBuf = 2;
  /* We need at least 2 MB of available memory to continue thumbnail generation */

  private Context mContext;
  List<IconifiedText> listFile;
  volatile boolean cancel = false;
  File file;
  Handler handler;

  private int thumbnailWidth = 256;
  private int thumbnailHeight = 256;

  ThumbnailLoader(File file, List<IconifiedText> list, Handler handler) {
    super("Thumbnail Loader");
    listFile = list;
    this.file = file;
    this.handler = handler;
  }

  public void setContext(Context mC) {
    mContext = mC;
  }

  public synchronized void requestStop() {
    cancel = true;
    FileManagerApp.log(TAG + "thumbnail Request stop");
    // listFile = null;
    // mContext = null;
    return;
  }

  @Override
  public void run() {
    if (cancel) {
      FileManagerApp.log(TAG + "Thumbnail loader canceled");
      listFile = null;
      mContext = null;
     // handler.obtainMessage(FileManagerActivity.MESSAGE_LOAD_THUMBNAIL_END).sendToTarget();
      return;
    }
    int count = listFile.size();
    if (count == 0) {
      FileManagerApp.log(TAG + "count is zero");
      listFile = null;
      mContext = null;
      //handler.obtainMessage(FileManagerActivity.MESSAGE_LOAD_THUMBNAIL_END).sendToTarget();
      return;
    }
    if (mContext == null) {
     // handler.obtainMessage(FileManagerActivity.MESSAGE_LOAD_THUMBNAIL_END).sendToTarget();
      return;
    }
    ActivityManager activityManager =
        (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
    List<RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
    if (runningAppProcesses == null) {
      FileManagerApp.log(TAG + "runningAppProcess null, exit to avoid low memory");
      listFile = null;
      mContext = null;
      //handler.obtainMessage(FileManagerActivity.MESSAGE_LOAD_THUMBNAIL_END).sendToTarget();
      return;
    }

    int pids[] = new int[1];
    for (RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
      if (runningAppProcessInfo.processName.equals(PROCESSNAME)) {
        pids[0] = runningAppProcessInfo.pid;
        break;
      }
    }

    int heapSize = activityManager.getMemoryClass();
    BitmapFactory.Options options = new BitmapFactory.Options();
    FileManagerApp.log(TAG + "Scanning for thumbnails (files=" + count + ")");
    for (int x = 0; x < count; x++) {
      if (cancel) {
        FileManagerApp.log(TAG + "Thumbnail loader canceled");
        listFile = null;
        mContext = null;
        //handler.obtainMessage(FileManagerActivity.MESSAGE_LOAD_THUMBNAIL_END).sendToTarget();
        return;
      }
      android.os.Debug.MemoryInfo[] memoryInfoArray = activityManager.getProcessMemoryInfo(pids);
      if (memoryInfoArray == null) {
        FileManagerApp.log(TAG +
            "Cannot determine how much memory File manager is using, exit to avoid low memory");
        listFile = null;
        mContext = null;
       // handler.obtainMessage(FileManagerActivity.MESSAGE_LOAD_THUMBNAIL_END).sendToTarget();
        return;
      }

      int totalUsedPss = memoryInfoArray[0].getTotalPss();
      if (totalUsedPss > (heapSize - mMemoryBuf) * 1024) {
        FileManagerApp.log(TAG + "File manager memory is low, exit!");
       // handler.obtainMessage(FileManagerActivity.MESSAGE_LOAD_THUMBNAIL_END).sendToTarget();
        return;
      }
      IconifiedText text = null;
      try {
        text = listFile.get(x);
      } catch (Exception e) {
        FileManagerApp.log(TAG + "Outofbounds");
        listFile = null;
        mContext = null;
        //handler.obtainMessage(FileManagerActivity.MESSAGE_LOAD_THUMBNAIL_END).sendToTarget();
        return;
      }
      if (text == null) {
       // handler.obtainMessage(FileManagerActivity.MESSAGE_LOAD_THUMBNAIL_END).sendToTarget();
        return;
      }
      if (FileUtils.getFile(file, text.getText()).isDirectory()) {
        continue;
      }

      String fileNameWithPath = text.getPathInfo();
      File mFile = new File(fileNameWithPath);
      String mimeType = MimeTypeUtil.getMimeType(mContext, mFile.getName());

      Bitmap bitmap = null;
      // We only obtain thumbnail for image and video
      if (mimeType == null) {
        continue;
      } else if (mimeType.indexOf("image") == 0) { // thumbnail for image
        try {
          options.inJustDecodeBounds = true;
          options.outWidth = 0;
          options.outHeight = 0;
          options.inSampleSize = 1;
          BitmapFactory.decodeFile(fileNameWithPath, options);

          if (options.outWidth > 0 && options.outHeight > 0) {
            int widthFactor = (options.outWidth + thumbnailWidth - 1) / thumbnailWidth;
            int heightFactor = (options.outHeight + thumbnailHeight - 1) / thumbnailHeight;
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
            options.inTempStorage = tempStorage; // Use tempstorage to decode
            bitmap = BitmapFactory.decodeFile(fileNameWithPath, options);
          }
        } catch (Exception e) {
          // That's okay, guess it just wasn't a bitmap.
        } catch (OutOfMemoryError e) {
          FileManagerApp.log(TAG + "Thumbnail loader out of memory");
          cancel = true;
          //handler.obtainMessage(FileManagerActivity.MESSAGE_LOAD_THUMBNAIL_END).sendToTarget();
          return;
        }
      } else if (mimeType.indexOf("video") == 0) { // thumbnail for video
        /*String ext = FileUtils.getExtension(mFile.getName());
        if(FileUtils.needQueryDB(ext.toLowerCase())){
          boolean t = FileUtils.isInAudioDatabase(mContext, mFile.getPath());
          if(t)//audio file with 3gp/3gpp/mp4
            continue;
        }*/
        try {
                    bitmap = ThumbnailUtils.createVideoThumbnail( FileUtils.getFile(file, text.getText()).getPath(), MediaStore.Images.Thumbnails.MICRO_KIND);
        } catch (Exception ex) {
          FileManagerApp.log(TAG + "Video thumbnail is not retrieved");
                } catch (OutOfMemoryError e){
                    FileManagerApp.log(TAG + "Video Thumbnail loader out of memory");
                    cancel = true;
                    return;
        }
      } else {
        continue;
      }

      if (bitmap != null) {
        BitmapDrawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);
        drawable.setGravity(Gravity.CENTER);
        drawable.setBounds(0, 0, thumbnailWidth, thumbnailHeight);
        FileManagerApp.log(TAG + "thumbnail =" + x);
        // Message msg =
        // handler.obtainMessage(FileManagerActivity.MESSAGE_ICON_CHANGED);
        Message msg =
            handler
                .obtainMessage(FileManagerActivity.MESSAGE_ICON_CHANGED, x, (int) (this.getId()));
        if (msg != null) {
          msg.obj = drawable;
          if (!cancel) {
            // text.setIcon(drawable);
            if (msg != null) {
              msg.sendToTarget();
            }
          }
        }
      }
    }
//handler.obtainMessage(FileManagerActivity.MESSAGE_LOAD_THUMBNAIL_END).sendToTarget();
    FileManagerApp.log(TAG + "Done scanning for thumbnails");
    listFile = null;
    mContext = null;
  }
}
