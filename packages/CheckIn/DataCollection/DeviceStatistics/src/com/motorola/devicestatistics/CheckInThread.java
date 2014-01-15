/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.devicestatistics;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Message;

import com.motorola.devicestatistics.CheckinHelper.DsCheckinEvent;
import com.motorola.devicestatistics.StatsCollector.DataTypes;
import com.motorola.devicestatistics.eventlogs.ScreenState;

/**
 * Thread controlled by DeviceStatisticsSvc to log passive device statistics to
 * Checkin DB
 */

class CheckInThread extends Thread {
    final static String TAG = "CheckInThread";

    private final Context mContext;
    private long mCheckinTime;
    private Message mResult;
    private int mReason;
    private final String mUsbStateInfo;

    /**
     * Constructor
     * @param ctx Current Context
     * @param result message to send back on complete 
     */
    public CheckInThread(Context ctx, Message result, int reason, String usbStateExtra) {
        mContext = ctx;
        mResult = result;
        mReason = reason;
        mUsbStateInfo = usbStateExtra;
    }

    /**
     * Run Method
     */
    @Override
    public void run() {
        doWork();
    }

    private void doWork() {
       try {
           final long currentTime = System.currentTimeMillis();
           mCheckinTime = currentTime/1000;

           // Start Collecting Stats and Checkin to DB
           collectDeviceStatistics();
       } catch (WorkException wEx) {
           Utils.Log.d(TAG, "Exception @ work");
       } finally {
           // Indicate Service about work complete
           sendWorkComplete();
       }
    }

    private void sendWorkComplete() {
        mResult.sendToTarget();
    }

    /**
     * Logs data receieved from battstats with additional details
     */
    private void collectDeviceStatistics() throws WorkException {
        boolean triggeredByUsb = mUsbStateInfo != null ? true : false;
        boolean battcapReported = false;

        try {
            ArrayList<DsCheckinEvent> logList = new ArrayList<DsCheckinEvent>();

            ScreenState.checkinScreenStateStats(mContext);
                                                               
            if((mReason & DevStatPrefs.STARTUP_DEV_STATS_LOG) != 0) { 
                CollectStatistics stats = new CollectStatistics();
                stats.getDeviceStats(mContext, mCheckinTime, mUsbStateInfo, logList);

                StatsUploader uploader = new StatsUploader(mContext);
                uploader.upload(DataTypes.BATTERY_STATS, mCheckinTime, triggeredByUsb);

                if (!triggeredByUsb) {
                    DevStatUtils.addBattCapLog(mCheckinTime, logList, mContext);
                    battcapReported = true;

                    // When triggeredByUsb is false, this is a midnight checkin
                    // Update checkin time. This is used to determine the next checkin, and for
                    // determining phone calls made betwen this time and the next checkin.
                    DevStatPrefs prefs = DevStatPrefs.getInstance(mContext);
                    prefs.setDevStatLastDailyCheckinTime((mCheckinTime * 1000));
                }
            }

            if((mReason & DevStatPrefs.STARTUP_BATT_CAP_LOG) != 0) {
                if (battcapReported == false) {
                    DevStatUtils.addBattCapLog(mCheckinTime, logList, mContext);
                }
            }

            if((mReason & DevStatPrefs.STARTUP_APP_STATS_LOG) != 0) {
                PkgStatsUtils.addUsageStats(mContext, mCheckinTime, logList);
            }


            if(!triggeredByUsb && ((mReason & DevStatPrefs.STARTUP_PMAPS_LOG) != 0)) {
                PkgStatsUtils.getMapperDump(mContext, mCheckinTime, logList);
            }

            logStats(logList);
        } catch (Exception e) {
           // TODO: Specific exceptions are to addressed. For now catch
           // generic exceptions.
           Utils.Log.d(TAG, "Exception collecting stats,", e);
           throw new WorkException("Exception collecting device stats");
        } 

    }

   /**
    * @throws WorkException
    */
    private void logStats(ArrayList<DsCheckinEvent> logs) throws WorkException {
        int N = logs.size();
        ContentResolver cr = mContext.getContentResolver();
        for(int i = 0; i < N; ++i) {
            DsCheckinEvent checkinEvent = logs.get(i);
            Utils.Log.v(checkinEvent.getTagName(), checkinEvent.serializeEvent().toString());
            checkinEvent.publish(cr);
        }
    }

    private static class WorkException extends Exception {
       private static final long serialVersionUID = 1L;

       public WorkException(String err) {
           super(err);
       }
   }
}

