/*
 * @(#)TimeFrameService.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a15776       2010/12/01  NA                Initial version
 *
 */
package com.motorola.contextual.smartprofile.sensors.timesensor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.motorola.contextual.commonutils.StringUtils;
import com.motorola.contextual.pickers.conditions.timeframe.TimeFramesRefreshHandler;
import com.motorola.contextual.smartprofile.CommandHandler;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/**
 * TimeMode service is started by the receiver upon receiving one of the following
 *
 * CLASS:
 *  Extends Service and implements TimeFrameConstants
 *
 * RESPONSIBILITIES:
 *  This service will be started by the {@link TimeFrameBroadcastReceiver} upon receiving one of
 *  the following intents
 *     1. android.intent.action.BOOT_COMPLETED
 *     2. com.motorola.ca.alarm.REGISTER_PENDING_INTENTS - For testing purpose
 *     3. android.intent.action.TIMEZONE_CHANGED
 *     4. android.intent.action.DATE_CHANGED
 *  Whenever this service is started, this service reads the time frames from the database and
 *  register the pending intent for each of the time frames. It uses the {@link TimeFrames}
 *  class to hold the time frames and navigate through it for easy access.
 *
 * COLLABORATORS:
 *  None
 */
public class TimeFrameService extends Service implements TimeFrameConstants {

    private static final String TAG =  TimeFrameService.class.getSimpleName();
    private Context mContext = null;

    // requests type that the service can handle
    public static final int REQ_TYPE_INIT                     = 1;
    public static final int REQ_TYPE_MODE_ADDED_OR_EDITED     = 2;
    public static final int REQ_TYPE_MODE_EDIT     			  = 3;
    public static final int REQ_TYPE_IMPORT     			  = 4;
    public static final int REQ_TYPE_UPDATE_SMARTRULES    	  = 5;
    public static final int REQ_TYPE_ALARM_MANAGER            = 6;

    public static final String SERV_REQUEST_ADDED   = "android.intent.action.TIME_MODE_ADDED";
    public static final String REGISTRATION_COMPLETED_RESPONSE = "REGISTERED";
    public static final String TF_TRUE = "TRUE";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null)
            Log.w(TAG, "On StartCommand with: " + intent.toUri(0));
        mContext = getApplicationContext();
        //get the request type and execute that request
        int reqType = getRequestType(intent);
        executeRequest(reqType,intent);
        return START_NOT_STICKY;
    }

    // handler to receive asynchronous responses
    Handler myServiceHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (LOG_INFO) Log.i(TAG,"Worker thread completed");
            stopSelf();
        }
    };


    /**
     * Method to determine why the service was started - could be for initialization or for a new
     * time frame addition
     *
     * @param intent - Intent that started this service
     * @return       - request type - REQ_TYPE_INIT or REQ_TYPE_MODE_ADDED_OR_EDITED
     */
    int getRequestType(Intent intent) {

        if (intent == null) {
            return REQ_TYPE_INIT;
        }
        String action = intent.getAction();
        int reqType;

        if ((action != null) && action.equals(SERV_REQUEST_ADDED)) {
            reqType = REQ_TYPE_MODE_ADDED_OR_EDITED;
        } else if ((action != null) && action.equals(TF_MODIFY_INTENT_ACTION)) {
            reqType = REQ_TYPE_MODE_EDIT;
        } else if ((action != null) && action.equals(TIMEFRAME_IMPORT_ACTION)) {
            reqType = REQ_TYPE_IMPORT;
        } else if ((action != null) && action.equals(TIMEFRAME_UPDATE_SMARTRULES_ACTION)) {
            reqType = REQ_TYPE_UPDATE_SMARTRULES;
        } else if (action != null && action.equals(TIMEFRAME_INTENT_ACTION)) {
            reqType = REQ_TYPE_ALARM_MANAGER;
        } else {
            reqType = REQ_TYPE_INIT;
        }
        return reqType;
    }

    /**
     * Execute the incoming request in a new thread
     *
     * @param reqType - Request Type
     * @param intent  - Incoming intent
     */
    void executeRequest(int reqType, Intent intent) {
        Thread t = new Thread(new WorkerThread(myServiceHandler,mContext,reqType,intent));
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (LOG_INFO) Log.i( TAG, "TimeMode service stopped.");
        super.onDestroy();
    }

    /**
     * Helper class to read the database and register all the time frames with the
     * alarm manager
     */
    private final class WorkerThread implements Runnable {
        Handler mCallbackHandler;
        Context mContext;
        int     mReqType;
        Intent  mIntent;

        public WorkerThread(Handler handler, Context context, int type, Intent intent) {
            mCallbackHandler = handler;
            mContext = context;
            mReqType = type;
            mIntent  = intent;
        }

        public void run() {
            // Do not allow multiple of instances of the thread running at the same time, to maintain
            // the database in the proper state.
            synchronized (WorkerThread.class) {
                TimeFrames timeframes;
                switch(mReqType) {
                    // Register all the time frames, if the request type is REQ_TYPE_INIT
                    // This is invoked by the TimeFrameBroadcastReceiver, every time there is a
                    // 1. BOOT_COMPLETE
                    // 2. DATE_CHANGE
                case REQ_TYPE_INIT:
                {
                    if (LOG_DEBUG) Log.d(TAG, "REQ_TYPE_INIT");
                    timeframes = new TimeFrames().getData(mContext);
                    if (timeframes == null) {
                        Log.e(TAG, "getData returned null timeFrames");
                        break;
                    }
                    if (LOG_DEBUG) Log.d(TAG, "Register all frames, so that it can be monitored");
                    timeframes.registerAllTimeFrames(mContext);
                    break;
                }

                // If a new time frame has been added, register the new one - This is not required
                // since there is a static broadcast receiver now and the new or edited time frames
                // register with the alarm manager register from the activity's context itself
                case REQ_TYPE_MODE_ADDED_OR_EDITED:
                {
                    if (LOG_INFO) Log.i(TAG, "REQ_TYPE_MODE_ADDED_OR_EDITED");
                    String frameName = mIntent.getStringExtra(EXTRA_FRIENDLY_NAME);
                    timeframes = new TimeFrames().getData(mContext);
                    if (timeframes == null) {
                        Log.e(TAG, "getData returned null timeFrames");
                        break;
                    }
                    TimeFrame timeframe = timeframes.getTimeFramebyName(frameName);
                    if (timeframe == null) {
                        Log.e(TAG, "getModebyName returned null timeframe");
                        break;
                    }
                    //register all the intents with the alarm manager
                    if(TimeUtil.isTimeFrameRegistered(mContext, frameName)) {
                        timeframe.regsiterAllIntents();
                    }

                    TimeUtil.exportTimeFrameData(mContext);
                    break;
                }
                // Edit an existing Time frame or add a new one
                case REQ_TYPE_MODE_EDIT:
                {
                    if (LOG_INFO) Log.i(TAG, "TF_MODIFY_INTENT_ACTION");
                    // extract the time frame name and details
                    String name = mIntent.getStringExtra(EXTRA_NAME);
                    String start = mIntent.getStringExtra(EXTRA_START);
                    String end = mIntent.getStringExtra(EXTRA_END);
                    boolean alldayflag = mIntent.getBooleanExtra(EXTRA_ALLDAYFLAG, false);
                    int daysOfWeek = mIntent.getIntExtra(EXTRA_DAYS, 0);
                    TimeUtil.writeTimeframeToDb(mContext, name, name, start, end, alldayflag, daysOfWeek);
                    TimeUtil.exportTimeFrameData(mContext);
                    break;
                }

                case REQ_TYPE_IMPORT:
                {
                    // Need to check the case when the imported rules have
                    // been modified by the user in between export-import
                    // Currently updating the time-frame info in DB blindly.

                    if (LOG_DEBUG) Log.d(TAG, "TIMEFRAME_IMPORT_ACTION");
                    // extract the time frame name and details
                    String tfData = mIntent.getStringExtra(EXTRA_TIMEFRAME_DATA);

                    if ((tfData == null) || (StringUtils.isEmpty(tfData))) {
                        Log.w(TAG,"TimeFrame JSON Data is null/empty !!!");
                    } else {

                        if(LOG_INFO) Log.i(TAG, "received TF data : " + tfData);

                        TimeFrameDBAdapter dbAdapter = null;
                        Cursor cursor = null;
                        TimeFramesRefreshHandler asyncRefresh = new TimeFramesRefreshHandler();
                        try {
                            dbAdapter = new TimeFrameDBAdapter(mContext);
                            JSONArray tfArray = new JSONArray(tfData);

                            for (int i=0; i < tfArray.length(); i++) {

                                JSONObject tfJSON = tfArray.getJSONObject(i);

                                // To sanitize JSON Object before converting it to TimeFrame
                                // Object, else Exception will be thrown for JSONObj init
                                String startTime = tfJSON.getString(TimeFrameTableColumns.START);
                                String endTime = tfJSON.getString(TimeFrameTableColumns.END);

                                if ((startTime == null) || (startTime.length() == 0) || (startTime.equals("0"))) {
                                    tfJSON.put(TimeFrameTableColumns.START, DAY_START_TIME);
                                    if(LOG_DEBUG) Log.d(TAG, "Updating stime !!!");
                                }

                                if ((endTime == null) || (endTime.length() == 0) || (endTime.equals("0"))) {
                                    tfJSON.put(TimeFrameTableColumns.END, DAY_END_TIME);
                                    if(LOG_DEBUG) Log.d(TAG, "Updating etime !!!");
                                }

                                TimeFrameTuple tfTuple = new TimeFrameTuple(tfJSON);

                                String name = tfTuple.getName();
                                String intName = tfTuple.getInternalName();
                                String daysOfWeek = String.valueOf(tfTuple.getDaysOfWeek());
                                String start = tfTuple.getStartTime();
                                String end = tfTuple.getEndTime();
                                Boolean alldayflag = Boolean.getBoolean(tfTuple.getAllDayFlag());

                                cursor = dbAdapter.checkTimeFrame(name, intName);

                                if ((cursor != null) && (cursor.getCount() > 0)) {
                                    // Entry already exists in Timeframe DB
                                    // so just go ahead and update it
                                    cursor.moveToFirst();

                                    if(LOG_DEBUG) Log.d(TAG, "Updating TimeFrame DB >> name : " + name + ", intName : " + intName);
                                    // deregister old timeframes associated with this name
                                    TimeFrame oldTf = new TimeFrame(mContext, cursor);
                                    oldTf.deRegsiterAllIntents();

                                    dbAdapter.updateRow(cursor.getString(cursor.getColumnIndexOrThrow(TimeFrameTableColumns.NAME)),
                                                        tfTuple);

                                } else {
                                    // Insert the entry into Timeframe DB
                                    // as it does not exist there
                                    if(LOG_DEBUG) Log.d(TAG, "Inserting into TimeFrame DB >> name : " + name + ", intName : " + intName);

                                    dbAdapter.insertRow(tfTuple);
                                }

                                if(cursor != null) {
                                    cursor.close();
                                }

                                //Update Smart Rule DB with new Name
                                asyncRefresh.constructAndPostAsyncRefresh(mContext, intName);

                                //Launching the RulesExporter to update the Shared preferences
                                TimeUtil.exportTimeFrameData(mContext);

                                // register the new time with the alarm manager
                                // convert the comma separated day string, to an array of days of week
                                String[] days = new TimeFrameDaysOfWeek(daysOfWeek)
                                .toCommaSeparatedString(mContext, true)
                                .split(SHORT_DAY_SEPARATOR);

                                // In case this is a Single day time frame, the day will be in long format
                                // convert this to short format
                                TimeFrameDaysOfWeek tfDow = new TimeFrameDaysOfWeek();
                                days[0] = tfDow.getShortFormat(days[0]);
                                TimeFrame timeFrame = new TimeFrame(mContext, name, intName,
                                                                    days, start, end, alldayflag);
                                timeFrame.deRegsiterAllIntents();
                                if (TimeUtil.isTimeFrameRegistered(mContext,
                                                                   name)) {
                                    if (LOG_INFO) {
                                        Log.i(TAG,
                                              "WorkerThread.run REQ_TYPE_IMPORT registering timeframe = "
                                              + name);
                                    }
                                    timeFrame.regsiterAllIntents();
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (dbAdapter != null) {
                                dbAdapter.close();
                            }
                            if (cursor != null && !cursor.isClosed()) {
                                cursor.close();
                            }
                        }
                    }
                    break;
                }

                case REQ_TYPE_UPDATE_SMARTRULES:
                {
                    if (LOG_INFO) Log.i(TAG, "REQ_TYPE_UPDATE_SMARTRULES");

                    // Retrieve the data from the Import Intent
                    Bundle bundle = mIntent.getExtras();
                    if (bundle != null) {
                        Serializable ruleKeySourcedata = bundle.getSerializable(EXTRA_RULE_INFO);
                        if (ruleKeySourcedata != null) {
                            @SuppressWarnings("unchecked")
                            HashMap<String, ArrayList<String>> ruleKeySourceMap = (HashMap<String, ArrayList<String>>)ruleKeySourcedata;

                            ArrayList<String> status = ruleKeySourceMap.get(KEY_RULE_STATUS);
                            ArrayList<String> key = ruleKeySourceMap.get(KEY_RULE_KEY);

                            if ((status != null) && (key != null)) {
                                int size = key.size();
                                if(LOG_INFO) Log.i(TAG,"Status size :"+status.size()+", Key size"+size);
                                if(status.size() == size) {
                                    for (int indx = 0; indx < size; indx++) {
                                        if(Boolean.parseBoolean(status.get(indx))) {
                                            TimeUtil.updateConditionTableForARule(mContext,key.get(indx));
                                        } else {
                                            if(LOG_DEBUG) Log.d(TAG,"Rule insertion status is false !!!, key :"+key.get(indx));
                                        }
                                    }
                                } else {
                                    if(LOG_DEBUG) Log.d(TAG, "Size mismatch >> status:"+status.size()+",key:"+size);
                                }
                            } else {
                                Log.e(TAG, "Status or Key is null!!");
                            }
                        } else {
                            Log.e(TAG,"ruleKeySourcedata is null");
                        }
                    } else {
                        Log.e(TAG,"Extras in null from the Rules Imported Intent");
                    }
                    break;
                }

                case REQ_TYPE_ALARM_MANAGER: {
                    // extract the time frame name, time frame state and the time frame's internal name
                    String extraName = mIntent.getStringExtra(TimeFrameConstants.EXTRA_FRAME_NAME);
                    String extraFlag     = mIntent.getStringExtra(TimeFrameConstants.EXTRA_ACTIVE_FLAG);
                    String friendlyName  = mIntent.getStringExtra(TimeFrameConstants.EXTRA_FRIENDLY_NAME);
                    if (LOG_INFO) Log.i(TAG, "REQ_TYPE_ALARM_MANAGER. extraName: "+extraName+", extraFlag: "+
                                            extraFlag+", friendlyName: "+friendlyName);

                    timeframes = new TimeFrames().getData(mContext);
                    if((timeframes != null) && (extraName != null) && (extraFlag != null)) {
                        TimeFrame timeframe = timeframes.getTimeFrameByInternalName(extraName.substring(TIME_FRAME_INTENT_PREFIX.length()));
                        String intName = extraName.substring(TIME_FRAME_INTENT_PREFIX.length());
                        if (timeframe != null)
                            timeframe.setTimeFrameActiveFlag(extraFlag.equals(ACTIVE_TRUE) ? true : false);
                        TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(mContext);
                        if(extraFlag.equals(TF_TRUE)) {
                            dbAdapter.setTimeFrameAsActive(intName);
                        } else {
                            dbAdapter.setTimeFrameAsInactive(intName);
                        }

                        ArrayList<String> configs = TimeFramesDetailComposer.getConfigListByInternalName(mContext, intName);
                        HashMap<String, String> configStateMap = new HashMap<String, String>();

                        int size = configs.size();
                        for(int index = 0; index < size; index++) {
                        	String config = configs.get(index);
                            configStateMap.put(config, TimeFramesDetailComposer.getStateForConfig(mContext, extraFlag, config));
                        }
                        if(!configs.isEmpty()) {
                            //make a new intent with the extracted details and broadcast
                            Intent newIntent = CommandHandler.constructNotification(configStateMap, TIMEFRAME_PUBLISHER_KEY);
                            mContext.sendBroadcast(newIntent, PERM_CONDITION_PUBLISHER_ADMIN);
                        }
                        if(LOG_INFO) Log.i(TAG, "postNotify : " +   " : " + configs + " : " + configStateMap.toString());
                    }
                    break;
                }

                default:
                    break;
                }

                Message response = Message.obtain();
                response.obj = REGISTRATION_COMPLETED_RESPONSE;
                response.setTarget(mCallbackHandler);
                response.sendToTarget();
            }
        }
    }
}
