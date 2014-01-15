/**
 * Copyright (C) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 *
 * This file was Created by contacts tech team for storage operation on the device
 */

package com.motorola.contacts.vcard;

import java.io.File;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.content.Context;


public class StorageUtils {
    private final static String LOG_TAG = "StorageUtils";

    /**
     * Get the SDCard Directory on the  device.
     *
     * Motorola Device supports several kinds of storage, like SD-card, internal storage, usbdisk
     *
     *This API returns SDCard directory if the current device supports SDCard, otherwise return null
     */
    public static File getSDCardDirectory(Context mContext) {
/* to-pass-build, Xinyu Liu/dcjf34  
        File[] availabeStorages = MotoEnvironment.getExternalStorageDirectories();
        for(int i = 0; i < availabeStorages.length; i ++ ){
            if(MotoEnvironment.MEDIA_TYPE_SDCARD.equals(MotoEnvironment.getExternalStorageMediaType(availabeStorages[i].getPath()))){
                return availabeStorages[i];
            }
        }
*/
        String SD_CARD_DIR = null;
        File sdcard = null;

        boolean isEmmcFlag = isEmmc(mContext);
        if(isEmmcFlag)
        {
            SD_CARD_DIR = getRemovableSd(mContext);
            sdcard = new File(SD_CARD_DIR);
            if(sdcard == null)
            {
                SD_CARD_DIR = getInternalSd(mContext);
                sdcard = new File(SD_CARD_DIR);
            }
        }
        else
        {
            SD_CARD_DIR = "/mnt/sdcard";
            sdcard = new File(SD_CARD_DIR);
        }
        return sdcard;
    }

    public static boolean isEmmc(Context mContext) {
        StorageManager sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        if (sm == null) {
            return false;
        }
        StorageVolume[] volumes = sm.getVolumeList();
        if (volumes.length > 1) {
                return true;
        }
        return false;
    }

    public static String getRemovableSd(Context mContext) {
        StorageManager sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        if (sm == null) {
            return null;
        }

        StorageVolume[] volumes = sm.getVolumeList();
        for (StorageVolume sv: volumes) {
            // only monitor non emulated and non usb disk storage
            if (sv.isRemovable()) {
                String path = sv.getPath();
                return path;
            }
        }
        //error case
        return Environment.getExternalStorageDirectory().toString();
    }

    public static String getInternalSd(Context mContext) {
        StorageManager sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        if (sm == null) {
            return null;
        }
        StorageVolume[] volumes = sm.getVolumeList();
        for (StorageVolume sv: volumes) {
            // only monitor non emulated and non usb disk storage
            if (sv.isEmulated()) {
                String path = sv.getPath();
                return path;
            }
        }
        //error case
        return Environment.getExternalStorageDirectory().toString();
    }


}
