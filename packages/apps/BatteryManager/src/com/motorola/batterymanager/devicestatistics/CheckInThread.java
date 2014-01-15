/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.batterymanager.devicestatistics;

import java.util.ArrayList;
import java.util.Iterator;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import com.motorola.batterymanager.Utils;

/**
 * Thread controlled by DeviceStatisticsSvc to log passive device statistics to
 * Checkin DB
 */

class CheckInThread extends Thread {
    final static String TAG = "CheckInThread";

    private final Context mContext;
    private long mCheckinTime;
    private Handler mServiceHandler;

    /**
     * Constructor
     * @param ctx Current Context
     * @param svcHandler Handler to message service back
     */
    public CheckInThread(Context ctx, Handler svcHandler) { 
        mContext = ctx;
        mServiceHandler = svcHandler;
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
           Utils.Log.setLogFlush(true);
           Utils.Log.setContentResolver(mContext.getContentResolver());

           final long currentTime = System.currentTimeMillis();
           mCheckinTime = currentTime/1000;

           // Start Collecting Stats and Checkin to DB
           collectDeviceStatistics();

           // Flush the logs if not done
           Utils.Log.flush(mContext.getContentResolver());

           // Update checkin time
           DevStatPrefs prefs = DevStatPrefs.getInstance(mContext);
           prefs.setDevStatLastCheckinTime(currentTime);

       } catch (WorkException wEx) {
           Utils.Log.d(TAG, "Exception @ work");
       } finally {
           // Indicate Service about work complete
           sendWorkComplete();
       }
    }

    private void sendWorkComplete() {
        Message msg = mServiceHandler.obtainMessage();
        msg.what = DeviceStatisticsSvc.DEVSTATS_WORK_DONE_ACTION;
        mServiceHandler.sendMessage(msg);
    }

    /**
     * Logs data receieved from battstats with additional details
     */
    private void collectDeviceStatistics() throws WorkException {
        try {
            CollectStatistics stats = new CollectStatistics();
            ArrayList<String> logList = stats.getDeviceStats(mContext, mCheckinTime);
            logStats(logList);
        } catch (Exception e) {
           // TODO: Specific exceptions are to addressed. For now catch
           // generic exceptions.
           throw new WorkException("General Exceptions");
        } 

    }

   /**
    * @throws WorkException
    */
    private void logStats(ArrayList<String> logs) throws WorkException {
        Iterator<String> stat = logs.iterator();
        while(stat.hasNext()) {
            Utils.Log.i(DevStatPrefs.CHECKIN_EVENT_ID, stat.next());
        }
    }

    private class WorkException extends Exception {
       private static final long serialVersionUID = 1L;

       public WorkException(String err) {
           super(err);
       }
   }
} 
