/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2011/05/06   IKCTXTAW-272    Initial version
 * w04917 (Brian Lee)        2011/10/13   IKCTXTAW-368    Update check-in string format
 * w04917 (Brian Lee)        2011/02/13   IKHSS6-6143     Put handler on background thread
 * w04917 (Brian Lee)        2012/02/14   IKCTXTAW-441    Use the new Checkin API
 * w04917 (Brian Lee)        2012/02/21   IKCTXTAW-442    Detailed meter data checkin event change.
 *
 */

package com.motorola.datacollection.meter.checkin;

import java.util.Collection;
import java.util.List;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.motorola.collectionservice.Callback_Client;
import com.motorola.collectionservice.CollectionServiceAPI;
import com.motorola.collectionservice.ICollectionService;
import com.motorola.data.event.api.Segment;
import com.motorola.datacollection.CheckinEventHelper;
import com.motorola.meter.Logger;

/** The Check-in app for MeterReader
 * @author w04917 (Brian Lee)
 *
 */
public class MeterReaderCheckIn extends Service {
    private static final String TAG = "MeterReaderCheckIn";
    public static final String INTENT_CHECK_IN_POWER_TOTAL =
        "com.motorola.meter.reader.intent.CHECK_IN_POWER_TOTAL";
    public static final String INTENT_CHECK_IN_POWER_DETAIL =
        "com.motorola.meter.reader.intent.CHECK_IN_POWER_DETAIL";
    private static final int MESSAGE_READING_CALLBACK = 1;

    private boolean checkInTotal = false;
    private boolean checkInDetail = false;

    private Thread mBackgroundThread;

    private ICollectionService collectionService;
    private boolean isBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Logger.d(TAG,"onServiceConnected");
            collectionService = ICollectionService.Stub.asInterface(service);
            boolean success = false;
            try {
                if (collectionService != null) {
                    success = collectionService.requestAllReadings(meterReaderCallback);
                } else {
                    Logger.e(TAG, "CollectionService is null onServiceConnected!");
                }
            } catch (RemoteException e) {
                Logger.e(TAG, "Unable to check-in. RemoteException in requestAllReadings");
            }
            if (!success) {
                stopSelf();
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            Logger.d(TAG,"onServiceDisconnected");
            collectionService = null;
        }
    };

    /* callback invoked from MeterReader service */
    private Callback_Client meterReaderCallback = new Callback_Client.Stub() {
        public void readingCallback(List<Bundle> readings) {
            if (readings != null && readings.size() > 0 && mHandler != null) {
                Message msg = mHandler.obtainMessage(MESSAGE_READING_CALLBACK, readings);
                mHandler.sendMessage(msg);
            } else {
                Logger.w(TAG, "Meter readings obtained are corrupt or handler is gone.");
                stopSelf();
            }
        }

        public void readingErrorCallback(List<String> meterTypes) {
            Logger.w(TAG, "Unable to get any meter readings.");
            stopSelf();
        }

        public void availableMetersCallback(List<String> meterTypes) {}
    };

    private Handler mHandler;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void bind() {
        if (!isBound) {
            Logger.d(TAG,"bind");
            isBound = bindService(new Intent(CollectionServiceAPI.INTENT_BIND_FRAMEWORK),
                                  mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void unbind() {
        if (isBound) {
            Logger.d(TAG,"unbind");
            isBound = false;
            unbindService(mConnection);
            collectionService = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mHandler != null) {
            if (intent != null && intent.getAction() != null) {
                String action = intent.getAction();
                if (action == null) {
                    Logger.d(TAG, "intent.getAction returned null");
                } else if (action.equals(INTENT_CHECK_IN_POWER_TOTAL)) {
                    Logger.d(TAG, "INTENT_CHECK_IN_POWER_TOTAL");
                    checkInTotal = true;
                    bind();
                } else if (action.equals(INTENT_CHECK_IN_POWER_DETAIL)) {
                    Logger.d(TAG,"INTENT_CHECK_IN_POWER_DETAIL");
                    checkInDetail = true;
                    bind();
                } else {
                    Logger.d(TAG,"Unknown Intent");
                }
            }
        } else {
            /* shouldn't really happen but if mHandler is null, the service should be stopped
             * so that a subsequent start call will result in a new creation attempt */
            Log.e(TAG, "No background handler.");
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mBackgroundThread  = new Thread() {
            public void run() {
                Looper.prepare();
                mHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        messageHandler(msg);
                    }
                };
                synchronized(this) {
                    notifyAll();
                }
                Looper.loop();
            }
        };

        mBackgroundThread.start();

        synchronized(mBackgroundThread) {
            if (mHandler == null) {
                try {
                    mBackgroundThread.wait();
                }  catch (InterruptedException e) {
                    Log.w(TAG, "Background handler initialization interrupted!");
                }
            }
        }

        if (mHandler == null) {
            Log.e(TAG, "Fatal Error - Unable to create background handler.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbind();
        if (mHandler != null) {
            mHandler.getLooper().quit();
        }
    }

    @SuppressWarnings("unchecked")
    private void messageHandler(Message msg) {
        switch(msg.what) {
        case MESSAGE_READING_CALLBACK:
            /* check-in and exit */
            Logger.d(TAG,"MESSAGE_READING_CALLBACK");
            List<Bundle> readings = (List<Bundle>)msg.obj;

            if (readings == null || readings.size() == 0) {
                Logger.w(TAG, "Readings list is null or empty!");
            } else {
                /* if two flags are set, that means we got a request for one
                 * while waiting for the response for the other.
                 * In that case, we want to check in both.
                 * onBind will only get called once in this case,
                 * and hence the call back will only get called once too.
                 */
                CheckInMeterData data = new CheckInMeterData(readings);

                if (checkInTotal) {
                    checkInTotal = false;
                    Logger.d(TAG, CheckInMeterData.CHECK_IN_TAG_POWER_TOTAL);
                    /* get check-in segments for all meters */
                    Collection<Segment> segmentList = data.getSegmentsTotal();
                    if (segmentList != null && !segmentList.isEmpty()) {
                        /* create check-in event */
                        CheckinEventHelper checkinEvent = null;

                        try {
                            checkinEvent = new CheckinEventHelper(
                                CheckInMeterData.CHECK_IN_TAG_POWER_TOTAL,
                                CheckInMeterData.CHECK_IN_EVENT_POWER_TOTAL,
                                CheckInMeterData.CHECK_IN_EVENT_POWER_TOTAL_VERSION);
                        } catch (IllegalArgumentException iae) {
                            Log.w(TAG, "Unable to create total event.");
                        }

                        if (checkinEvent != null) {
                            /* add segments for each meter */
                            checkinEvent.addSegments(segmentList);
                            /* we only need to check-in if there are segments to check-in */
                            if (checkinEvent.hasSegments()) {
                                try {
                                    checkinEvent.publish(getContentResolver());
                                } catch (Exception e) {
                                    Logger.d(TAG, checkinEvent.toString());
                                    Log.w(TAG, "Unable to check-in total.");
                                    Logger.d(TAG, e.toString());
                                }
                            }
                        }
                    }
                }

                if (checkInDetail) {
                    checkInDetail = false;
                    Logger.d(TAG, CheckInMeterData.CHECK_IN_TAG_POWER_DETAIL);
                    /* get check-in events for all meters */
                    Collection<CheckinEventHelper> checkinEventList = data.getCheckinEventsDetail();

                    if (checkinEventList != null && !checkinEventList.isEmpty()) {
                        for (CheckinEventHelper checkinEvent : checkinEventList) {
                            if (checkinEvent != null) {
                                try {
                                    checkinEvent.publish(getContentResolver());
                                } catch (Exception e) {
                                    Logger.d(TAG, checkinEvent.toString());
                                    Log.w(TAG, "Unable to check-in detail.");
                                    Logger.d(TAG, e.toString());
                                }
                            }
                        }
                    }
                }
            } //end of ELSE for empty readings
            stopSelf();
            break;
        } //end of SWITCH statement
    } //end of messageHandler()
}