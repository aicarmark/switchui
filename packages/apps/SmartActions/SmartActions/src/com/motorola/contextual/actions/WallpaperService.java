/*
 * @(#)WallpaperService.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * rdq478        2011/05/24  IKMAIN-17917     Wallpaper data cleanup service
 * rdq478        2011/05/31  IKINTNETAPP-320  Fixed findbugs warning & changed
 *                                            binary search method.
 * rdq478        2011/07/13  IKINTNETAPP-386  Add default wp in do not delete list
 * rdq478        2011/08/16  IKSTABLE6-6826   Add save & restore in the service
 * rdq478        2011/09/22  IKSTABLE6-10141  Add workaround to avoid getting wrong wp
 * rdq478        2011/10/18  IKSTABLE6-18450  Changed to use PNG compression
 * rdq478        2011/11/04  IKMAIN-31823     Fixed null pointer exception
 * XPR643        2012/06/19 Smart Actions 2.1 Support SetWallpaperPickerActivity
 */

package com.motorola.contextual.actions;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.IntentService;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.pickers.actions.SetWallpaperPickerActivity;  // Smart Actions 2.1

/**
 * This class handle the request to save, restore and delete unused wallpaper images.  <code><pre>
 *
 * CLASS:
 *     Extends {@link IntentService}.
 *
 * RESPONSIBILITIES:
 *     When received the request:
 *     (1). save new wallpaper and restore the default
 *     (2). it will query Actions table to get list of active images
 *     (3). it also obtains a list of images in Wallpapers folder
 *     (4). delete all images in the folder that are not in active list
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class WallpaperService extends IntentService implements Constants {

    private final static String TAG = TAG_PREFIX + WallpaperService.class.getSimpleName();
    private final static Uri ACTIONS_TABLE_URI = Uri.parse("content://" + "com.motorola.contextual.smartrules" + "/" + "Action" + "/");
    private final static String WALLPAPER_NAME = "_WP.png";

    public WallpaperService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getStringExtra(EXTRA_URI) != null) {
            setWallpaper(intent);
        } else if (intent.getBooleanExtra(EXTRA_SAVE_DEFAULT, false)) {
            /*
             * This is the workaround for CR IKSTABLE6-10141. The issue is:
             * after factory reset or flashing a new software, the first time
             * the user set wallpaper, the wallpaper FW would send 2
             * WALLPAPER_CHANGED intents. (1) is sent during CREATING state and
             * (2) is sent after the wallpaper changed completed. From app side
             * we don't know the different as they are the same intent, so when
             * WALLPAPER QA receives the first intent, it will call WP manager
             * to get WP drawable. Since the WP FW is still in creating state,
             * the current wallpaper drawable is return instead of the new
             * wallpaper.
             *
             * Thus, our workaround is to force wallpaper swap during boot
             * completed and if our storage is empty, so that whenever user
             * change set wallpaper we will always get only one intent.
             */
            swapWallpaper();
        } else if (intent.getBooleanExtra(EXTRA_WP_SAVE_CURRENT, false)) {
            WallpaperManager manager = WallpaperManager.getInstance(this);
            if (manager.getWallpaperInfo() == null) {
                saveWallpaper(WP_DIR, WP_CURRENT, manager.getDrawable());
            }
        } else if (intent.getBooleanExtra(EXTRA_WP_PERFORM_CLEANUP, false)) {
            wallpaperCleanup();
        } else {
            saveAndRestoreWallpaper();
        }
    }

    /**
     * Set the wallpaper to the filename obtained from the intent passed
     *
     * @param intent
     *            - intent containing the filename as uri extra
     */
    private void setWallpaper(Intent intent) {
        String uri = intent.getStringExtra(EXTRA_URI);
        WallpaperManager wallManager = WallpaperManager.getInstance(this);
        if (wallManager.getWallpaperInfo() == null) {
            Drawable wallpaper = null;
            if (intent.getBooleanExtra(EXTRA_RESTORE_DEFAULT, false)) {
                setwallpaper.setListener(this, false);
            } else {
                setwallpaper.setListener(this, true);
            }
            if (intent.getBooleanExtra(EXTRA_SAVE_DEFAULT, false)) {
                wallpaper = wallManager.getDrawable();
            }
            setWallpaper(uri);
            if (wallpaper != null) {
                saveWallpaper(WP_DIR, WP_DEFAULT, wallpaper);
            }
        }
    }

    /**
     * Set the wallpaper to the filename passed as uri
     *
     * @param uri
     */
    private void setWallpaper(String fileName) {
        WallpaperManager wallManager = WallpaperManager.getInstance(this);
        // now set the wallpaper to new wallpaper
        FileInputStream readImage = null;
        try {
            /*
             * Reading <time>_WP.jpg and set it as wallpaper
             */
            readImage = FileUtils.openFileInput(this, WP_DIR, fileName);
            if (readImage == null) {
                Log.e(TAG, "Could not read wallpaper image stream.");
                return;
            }
            wallManager.setStream(readImage);
            if (LOG_INFO) {
                Log.i(TAG, "setWallpaper has set the wallpaper from "
                      + fileName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error occur while trying to set wallpaper.");
        } finally {
            if (readImage != null) {
                try {
                    readImage.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error occur while trying to close file stream.");
                }
            }
        }
    }

    /**
     * This method performs the file operations for storing the image to a file
     *
     * @param directoryName
     *            - The name of the directory where the file has to be save
     * @param fileName
     *            - The file name
     * @param wallpaper
     *            - The reference to wallpaper drawable
     * @return status - true if save is successful, false otherwise
     */
    private boolean saveWallpaper(String directoryName, String fileName,
                                  Drawable wallpaper) {
        boolean status = false;
        if (directoryName != null && !directoryName.isEmpty()
                && fileName != null && !fileName.isEmpty() && wallpaper != null) {
            byte[] imgBuffer = FileUtils.toByteArray(wallpaper);
            if (imgBuffer != null) {
                FileUtils.storeBuffer(this, imgBuffer, directoryName, fileName);
                status = true;
            }
        }
        if (LOG_INFO) {
            Log.i(TAG, "saveWallpaper operation status = " + status
                  + " directory name = " + directoryName + " file name = "
                  + fileName);
        }
        return status;
    }

    /**
     * This method saves, restores and deletes wallpaper.
     */
    private void saveAndRestoreWallpaper() {
        String fileName = saveWallpaper();
        setWallpaper(WP_CURRENT);
        boolean status = fileName != null;
        sendResult(status, fileName);
        if (LOG_INFO) {
            Log.i(TAG, "saveAndRestoreWallpaper operation complete");
        }
    }

    /**
     * Do a wallpaper swap with the same image.
     */
    private void swapWallpaper() {

        WallpaperManager wallManager = WallpaperManager.getInstance(this);
        Drawable wallpaper = wallManager.getDrawable();

        if (wallpaper == null) {
            Log.e(TAG, "Unable to get wallpaper drawable.");
            return;
        }

        Bitmap bmpWP = ((BitmapDrawable) wallpaper).getBitmap();

        try {
            wallManager.setBitmap(bmpWP);

            // save default wallpaper
            byte[] imgBuffer = FileUtils.toByteArray(wallpaper);
            FileUtils.storeBuffer(this, imgBuffer, WP_DIR, WP_DEFAULT);
            if (LOG_INFO) {
                Log.i(TAG, "swapWallpaper operation complete");
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not swap wallpaper due to IOException.");
        }
    }

    /**
     * Get the current newly set wallpaper and then save it in the
     * internal storage. If everything fine, a file name should return back.
     * @return either File Name or NULL
     */
    private String saveWallpaper() {
        WallpaperManager wallManager = WallpaperManager.getInstance(this);
        if (wallManager.getWallpaperInfo() == null) {
            /*
             * Store the buffer in the internal storage. The file name is
             * started by timestamp in seconds.
             */
            long ctime = System.currentTimeMillis() / 1000;
            String fileName = String.valueOf(ctime).concat(WALLPAPER_NAME);
            if (saveWallpaper(WP_DIR, fileName, wallManager.getDrawable())) {
                return fileName;
            }
        }
        Log.e(TAG, "Save wallpaper failed.");
        return null;
    }

    /**
     * This method will perform cleanup by deleting any unuse
     * wallpaper images.
     */
    private void wallpaperCleanup() {
        ArrayList<String> activeWallpapers = getListOfActiveWallpapers();
        ArrayList<String> savedWallpapers = FileUtils.listFiles(this, WP_DIR);
        deleteUnusedWallpapers(activeWallpapers, savedWallpapers);
    }

    /**
     * Get the list of currently active wallpapers uses in QuickAction.
     * @return list of the wallpaper file names.
     */
    private ArrayList<String> getListOfActiveWallpapers() {
        String[] projections = { "ActionDesc" };
        String whereClause = "ActPubKey=\'com.motorola.contextual.actions.setwallpaper\'";
        Cursor cursor = null;
        ArrayList<String> activeWallpapersList = new ArrayList<String>();
        try {
            cursor = getContentResolver().query(ACTIONS_TABLE_URI, projections,
                    whereClause, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String fileName = cursor.getString(0);
                    if (fileName != null && !fileName.isEmpty()) {
                        activeWallpapersList.add(fileName);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            Log.e(TAG,
                    "getListOfActiveWallpapers error occured while obtaining a list of active wallpapers");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return activeWallpapersList;
    }

    /**
     * Send result back to wallpaper activity
     * @param status - true if success otherwise false
     * @param fileName - wallpaper file name
     */
    private void sendResult(boolean status, String fileName) {
        Intent resultIntent = new Intent();
        resultIntent.setAction(SETTING_WP_ACTION);
        resultIntent.putExtra(EXTRA_URI, fileName);
        resultIntent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(resultIntent);
    }

    /**
     * Perform a search in active list, if the current saved file is not in
     * active list, we should delete it.
     *
     * @param activeWallpapers
     *            - list of active wallpapers
     * @param savedWallpapers
     *            - list of saved wallpapers
     */
    private void deleteUnusedWallpapers(ArrayList<String> activeWallpapers,
            ArrayList<String> savedWallpapers) {
        if (activeWallpapers != null && savedWallpapers != null
                && savedWallpapers.size() > 0) {
            for (String fileName : savedWallpapers) {
                if (!fileName.equals(WP_DEFAULT)
                        && !fileName.equals(SetWallpaperPickerActivity  // Smart Actions 2.1
                                .getSelectedWallpaperFileName())) {
                    if (!activeWallpapers.contains(fileName)) {
                        boolean result = FileUtils.deleteFile(this, WP_DIR,
                                fileName);
                        if (LOG_INFO && result) {
                            Log.i(TAG, "deleteUnusedWallpapers deleted "
                                    + fileName);
                        }
                    }
                }
            }
        }
    }

}
