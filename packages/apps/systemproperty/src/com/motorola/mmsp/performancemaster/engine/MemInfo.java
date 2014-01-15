/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *                             Modification     Tracking
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * dfb746                      16/04/2012                   Initial release
 */

package com.motorola.mmsp.performancemaster.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.motorola.mmsp.performancemaster.engine.Job.JobDoneListener;

import android.content.Context;
import android.os.Environment;
import android.os.Parcelable;
import android.os.StatFs;
import android.os.storage.StorageManager;

import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.StorageVolume;

import android.util.Log;

public class MemInfo extends InfoBase implements JobDoneListener {
    private static final String TAG = "MemInfo";

    private long mRamTotal;
    private long mRamFree;
    private long mRomTotal;
    private long mRomFree;
    private long mSdTotal = 0;
    private long mSdFree;

    private static int UPDATE_INTERVAL = 3000;

    private final static String MEM_INFO_FILE = "/proc/meminfo";

    private Timer mUpdateTimer = null;

    private TimerTask mTimeTask = null;

    private Job mUpdateJob = null;

    public String SD_CARD_EXT_DIR = null;

    private boolean mSdcardAvail = false;

    private IMountService mMntSvc = null;

    public MemInfo() {
        super();
        initMountServiceAndCheck();
        initStorageValue();

        long[] values = getRAMInfo();
        mRamTotal = values[0];
        mRamFree = values[1];

        values = getInternalStorageInfo();
        mRomTotal = values[0];
        mRomFree = values[1];

        values = getExternalStorageInfo();
        mSdTotal = values[0];
        mSdFree = values[1];

        Log.e(TAG, "MemInfo  mSdTotal = " + mSdTotal + " mSdFree =" + mSdFree);

        mUpdateJob = new Job(MemInfo.this) {
            @Override
            public void doJob() {
                // get mem info
                initStorageValue();
                long[] ramValues = getRAMInfo();
                long[] romValues = getInternalStorageInfo();

                long[] sdValues = getExternalStorageInfo();

                synchronized (MemInfo.this) {
                    mRamTotal = ramValues[0];
                    mRamFree = ramValues[1];

                    mRomTotal = romValues[0];
                    mRomFree = romValues[1];

                    mSdTotal = sdValues[0];
                    mSdFree = sdValues[1];

                }
            }
        };
    }

    private void initStorageValue() {


        try {

            Parcelable[] volumes = mMntSvc.getVolumeList();
            for (Parcelable s : volumes) {

                if (((StorageVolume) s).isRemovable()
                        && !(((StorageVolume) s).getPath().startsWith("/mnt/usbdisk"))) {
                    mSdcardAvail = true;
                    SD_CARD_EXT_DIR = ((StorageVolume) s).getPath();
                    Log.e(TAG, "sdcard   name = " + SD_CARD_EXT_DIR);
                    break;
                }

            }
        } catch (Exception re) {
            Log.e(TAG, "Error in getExternalStorageDirectories()");
        }
    }

    private void initMountServiceAndCheck() {
        try {
            if (mMntSvc == null) {
                mMntSvc = IMountService.Stub.asInterface(ServiceManager
                        .getService("mount"));
            }
            if (mMntSvc == null) {
                Log.e(TAG, "Fail to get MountService!");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Exception thrown in getting MountSerivce! " + ex);
        }
    }

    @Override
    public void onJobDone() {
        synchronized (MemInfo.this) {
            onInfoUpdate();
        }
    }

    @Override
    protected void startInfoUpdate() {
        // TODO Auto-generated method stub
        mUpdateTimer = new Timer();
        mTimeTask = new TimerTask() {
            public void run() {
                Worker.getInstance().addJob(mUpdateJob);
            }
        };
        mUpdateTimer.schedule(this.mTimeTask, 0, UPDATE_INTERVAL);
    }

    @Override
    protected void stopInfoUpdate() {
        // TODO Auto-generated method stub
        if (mUpdateTimer != null) {
            mUpdateTimer.cancel();
            mTimeTask = null;
        }
    }

    /**
     * get RAM info from /proc/meminfo
     * 
     * @return long[2] element 0 for total ram, and element 1 for free ram, null
     *         if there's exception
     */
    private long[] getRAMInfo() {
        BufferedReader inStream;
        long[] ramInfo = new long[] {
                0, 0
        };
        try {
            inStream = new BufferedReader(new FileReader(MEM_INFO_FILE));
            String strMemTotal = inStream.readLine();
            String strMemFree = inStream.readLine();
            String strBuffers = inStream.readLine();
            String strCached = inStream.readLine();

            if ((strMemTotal != null) && strMemTotal.startsWith("MemTotal:")) {
                ramInfo[0] = Long.parseLong(strMemTotal.split("[ ]+", 3)[1]);
            }

            if ((strMemFree != null) && strMemFree.startsWith("MemFree:")) {
                ramInfo[1] += Long.parseLong(strMemFree.split("[ ]+", 3)[1]);
            }

            if ((strBuffers != null) && strBuffers.startsWith("Buffers:")) {
                ramInfo[1] += Long.parseLong(strBuffers.split("[ ]+", 3)[1]);
            }

            if ((strCached != null) && strCached.startsWith("Cached:")) {
                ramInfo[1] += Long.parseLong(strCached.split("[ ]+", 3)[1]);
            }

            inStream.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException");
            return null;
        } catch (IOException e) {
            Log.e(TAG, "IOException");
            return null;
        }

        return ramInfo;
    }

    /**
     * get ROM info
     * 
     * @return long[2] element 0 for total rom, and element 1 for free rom
     */
    private long[] getInternalStorageInfo() {
        return getStorageInfo(Environment.getDataDirectory());
    }

    /**
     * check if SDCard exist
     * 
     * @return true sd exist, false no sdcard
     */
    public boolean isSDExist() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    /**
     * get SD Card info
     * 
     * @return long[2] element 0 for total, and element 1 for free
     */
    private long[] getExternalStorageInfo() {

        if (mSdcardAvail) {
            return getStorageInfo(new File(SD_CARD_EXT_DIR));
        } else {
            return new long[] {
                    0, 0
            };
        }

    }

    private long[] getStorageInfo(File path) {
        long[] values = new long[] {
                0, 0
        };
        if (path != null) {
            StatFs stat = new StatFs(path.getAbsolutePath());
            long blockSize = stat.getBlockSize();

            values[0] = stat.getBlockCount() * blockSize;
            values[1] = stat.getAvailableBlocks() * blockSize;
        }

        return values;
    }

    public long getRamTotal() {
        return mRamTotal;
    }

    public long getRamFree() {
        return mRamFree;
    }

    public long getRomTotal() {
        return mRomTotal;
    }

    public long getRomFree() {
        return mRomFree;
    }

    public long getSdTotal() {
        return mSdTotal;
    }

    public long getSdFree() {
        return mSdFree;
    }

    @Override
    public String toString() {
        return "MemInfo: " + this.mRamTotal + "/" + this.mRamFree + "/"
                + this.mRomTotal + "/" + this.mRomFree + "/" + this.mSdTotal + "/"
                + this.mSdFree;
    }
}
