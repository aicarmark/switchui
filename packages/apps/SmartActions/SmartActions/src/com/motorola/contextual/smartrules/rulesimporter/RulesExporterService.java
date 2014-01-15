/*
 * @(#)RulesExporterService.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18172        2011/04/29 NA				  Initial version
 */

package com.motorola.contextual.smartrules.rulesimporter;


import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/**
 * This Service is currently exposed to export the XML String for a specific
 * set of Rules based on Source List to the server
 *
 *
 */
public class RulesExporterService extends Service implements Constants, DbSyntax {


    private static final String TAG = RulesExporterService.class.getSimpleName();

    @SuppressWarnings("unused")
    private RulesExporter rexp;

    private Context mContext;

    // Accepts tasks to run executing only one at a time queuing the rest.
    private static final ExecutorService mQueueExecutor = Executors.newSingleThreadExecutor();


    /** onStartCommand()
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (LOG_DEBUG) Log.d(TAG, "Started");

        mContext = this;

        if(LOG_DEBUG) Log.d(TAG,"Launching Executor");

        // Accepts tasks to run executing only one at a time queuing the rest.
        mQueueExecutor.submit(new ProcessRulesToExport(mHandler, intent));

        return START_NOT_STICKY;

    }

    /** onBind()
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     *
     * Runnable to process the Rules Importer for Precanned Rules
     *
     */
    private  final class ProcessRulesToExport implements Runnable {
        Handler callBackHandle = null;
        // SOURCE list for the Rules to be encapsulated in the XML
        private ArrayList<String>  sourceList;
        private String timeFrameData;
        private String locationData;
        private String calendarEventsData;
        private boolean mIsExportToSDCard;
        /** constructor
         */
        public ProcessRulesToExport(Handler callBack, Intent intent) {
            callBackHandle = callBack;
            sourceList = intent.getStringArrayListExtra(EXTRA_SOURCE_LIST);
            timeFrameData = intent.getStringExtra(EXTRA_TIMEFRAME_DATA);
            locationData = intent.getStringExtra(EXTRA_LOCATION_DATA);
            calendarEventsData = intent.getStringExtra(EXTRA_CALENDAR_EVENTS_DATA);
            mIsExportToSDCard = intent.getBooleanExtra(EXTRA_IS_EXPORT_TO_SDCARD, false);
            
            if(sourceList != null){
            	if (LOG_DEBUG) Log.d (TAG," source List size: " + sourceList.size());
            } else {
            	Log.w(TAG, "SourceList is null");
            }
            
            if(timeFrameData == null) {
            	Log.w(TAG, "timeFrameData is null");
            } 

            if(locationData == null) {
                Log.w(TAG, "locationData is null");
            }
        }

        /**
        * Thread started to upload the Rules to the server
        */
        public void run() {

            // Priority should be one less than that of ConditionBuilder
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 3);
            Thread.currentThread().setName("RulesExporter");

            if(LOG_DEBUG) Log.d(TAG,"Construct the XML");
            
            if(mIsExportToSDCard){
            	//Export Rules to the SD Card
            	if (LOG_DEBUG) Log.d(TAG,"Export Rules to SD Card");
	            new RulesExporter().startRulesExporter(mContext);
            	
            }else{
            	if (LOG_INFO) Log.d(TAG,"Export Rules to the Server");
            
	            // Creates XML for specific set of Rules based on SOURCE
	            // and then uploads those Rules to the server
	           
	            new RulesExporter().startRulesExporter(mContext,sourceList,timeFrameData, locationData, calendarEventsData);
            }
            
            Message responseMsg = Message.obtain();
            responseMsg.setTarget(callBackHandle);
            responseMsg.sendToTarget();
        }
    }

    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            stopSelf();
        }
    };
}

