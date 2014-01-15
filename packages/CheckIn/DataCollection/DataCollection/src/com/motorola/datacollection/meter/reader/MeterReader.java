/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2011/05/06   IKCTXTAW-272    Initial version
 * w04917 (Brian Lee)        2011/05/11   IKCTXTAW-272    Error handling for non-responsive peripheral
 *                                                        and remove klocwork error. Debug output filename hardcoded.
 *
 */

package com.motorola.datacollection.meter.reader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.motorola.collectionservice.Callback_Client;
import com.motorola.collectionservice.CollectionServiceAPI;
import com.motorola.collectionservice.ICollectionService;
import com.motorola.meter.Callback_Meter;
import com.motorola.meter.Meter;
import com.motorola.meter.Logger;
import com.motorola.meter.reader.IMeterReader;

/** This is the main service that makes up the MeterReader Framework.
 * This service provides two interfaces:
 *    -MeterReader interface for Meters
 *    -CollectionService interface for apps
 * Meters can register to this service to have their readings read,
 * and apps can register to this service to request readings.
 *
 * @author w04917 (Brian Lee)
 *
 */
public class MeterReader extends Service {
    private static final String TAG = "MeterReader";
    /* android.util.Config.DEBUG will always be false in standard Android SDK builds,
     * but it will(should) be true in Platform builds. Or you can manually
     * turn it on and recompile.
     */
    private static final boolean FILE_DEBUG_MODE = android.util.Config.DEBUG || false;
    private static final String LOG_CONFIG_FILE_NAME = "/data/motorola/meter_reader/debug_output.cfg";
    private static final String LOG_OUTPUT_FILE_NAME = "/data/motorola/meter_reader/meterlog.txt";

    /* use RemoteCallbackList instead of HashMap. Looking up specific meters may be a little faster with HashMap,
     * but RemoteCallbackList keeps track of all registered callbacks, handles host process death/crash,
     * and has an underlying locking mechanism to deal with multithreaded incoming calls.
     * In other words, RemoteCallbackList is better than using HashMap.
     */

    /* callbacks from Meters so we can request data */
    private RemoteCallbackList<Callback_Meter> meterCallbacks;
    /* callbacks from CollectionService clients (apps) so we can notify when data is available */
    private RemoteCallbackList<Callback_Client> clientCallbacks;

    /* Handlers are attached to the thread that created it.
     * Normally, this shouldn't matter.
     * However, when running Android Unit Test,
     * the Meter, MeterReader, and the Application will all be
     * created in the same thread. This means that the handler will
     * be blocked from processing messages while trying to handle
     * the simulated components in an asynchronous way.
     * The solution is to make sure the handler runs in its own thread.
     */
    private Handler mHandler;
    private class ServiceHandlerThread extends Thread {
        CountDownLatch latch;

        public ServiceHandlerThread(CountDownLatch newLatch) {
            super();
            latch = newLatch;
        }

        public void run() {
            Looper.prepare();

            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    messageHandler(msg);
                }
            };
            latch.countDown();

            Looper.loop();
        }
    }

    private static final int MESSAGE_REQUEST_READING = 1;
    private static final int MESSAGE_REQUEST_ALL_READINGS = 2;
    private static final int MESSAGE_REQUEST_AVAILABLE_METERS = 3;

    private static final int MESSAGE_REPORT_DATA = 100;

    private static final int MESSAGE_READ_METER_TIMER_EXPIRED = 200;

    private static final long READ_METER_EXPIRE_TIME_MS = 1000;

    /* Interface for Meters (MeterReader Interface) */
    private final IMeterReader.Stub binderMeterReader = new IMeterReader.Stub() {

        /* dataType - full class name of the MeterReading type the Meter uses. This is used
         * so that we can look up specific peripherals, using it as the key.
         */
        public void registerMeter(Callback_Meter meter, String dataType) {
            if (meter != null && dataType != null) {
                Logger.d(TAG,"registerMeter with MeterReading type of " + dataType);
                meterCallbacks.register(meter, dataType);
            } else {
                Logger.d(TAG, "meter or dataType is null in registerMeter");
            }
        }

        public void unregisterMeter(Callback_Meter meter) {
            if (meter != null) {
                Logger.d(TAG,"unregisterMeter");
                meterCallbacks.unregister(meter);
            }
        }

        public void reportData(Bundle data) {
            /* switch thread since this isn't a oneway interface.
             * we do not want to block the meters here
             */
            Message msg = mHandler.obtainMessage(MESSAGE_REPORT_DATA, data);
            mHandler.sendMessage(msg);
        }
    };

    /* wrapper for request info, since we can only pass
     * one object as argument when passing messages to ourselves
     */
    private static class RequestInfo {
        ArrayList<String> dataTypes;
        Callback_Client callback;

        public RequestInfo(List<String> newDataTypes, Callback_Client newCallback) {
            dataTypes = new ArrayList<String>(newDataTypes);
            callback = newCallback;
        }
    }

    /* data structure to keep track of multi-meter request,
     * so we can group the results together and send it back to the client
     * who requested multiple meters
     */
    private static class PendingMeters {
        ArrayList<String> pending;
        ArrayList<Bundle> bundles;
    }

    /* Interface for Framework Client Apps (CollectionService Interface) */
    private final ICollectionService.Stub binderCollectionService = new ICollectionService.Stub() {

        /*
         * sets up a one time request for multiple meter types
         *
         * meterTypes - array of the full class name of the MeterReading subclasses that the client wishes to obtain as data
         *
         * Whenever we are accessing RemoteCallbackList.beginBroadcast() function,
         * we have to make sure we are doing it from the same thread.
         * To do so, we use a handler to get the request done in our serviceHandler thread.
         *
         * Returns true if request was queued successfully, false otherwise
         */
        public boolean requestReading(List<String> meterTypes, Callback_Client client) {
            String dataTypeList = "";

            if (FILE_DEBUG_MODE && meterTypes != null) {
                StringBuilder sb = new StringBuilder();
                for (String dataType : meterTypes) {
                    sb.append(dataType);
                    sb.append(" | ");
                }
                dataTypeList = sb.toString();
            }

            Logger.d(TAG, "requestReading for " + dataTypeList);

            boolean success = false;
            if (meterTypes != null && client != null && meterTypes.size() > 0) {
                Message msg = mHandler.obtainMessage(MESSAGE_REQUEST_READING, new RequestInfo(meterTypes, client));
                mHandler.sendMessage(msg);
                success = true;
            } else {
                Logger.w(TAG, "requestReading Error - meterTypes and Callback_Client cannot be null and meterTypes must have items");
            }
            return success;
        }

        /*
         * sets up a one time request for all available meter types
         *
         * dataTypes - array of the full class name of the MeterReading subclasses that the client wishes to obtain as data
         *
         * Whenever we are accessing RemoteCallbackList.beginBroadcast() function,
         * we have to make sure we are doing it from the same thread.
         * To do so, we use a handler to get the request done in our serviceHandler thread.
         *
         * Returns true if request was queued successfully, false otherwise
         */
        public boolean requestAllReadings(Callback_Client client) {
            boolean success = false;
            if (client != null) {
                Message msg = mHandler.obtainMessage(MESSAGE_REQUEST_ALL_READINGS, client);
                mHandler.sendMessage(msg);
                success = true;
            } else {
                Logger.w(TAG, "requestAllReadings Error - Callback_Client cannot be null");
            }
            return success;
        }

        /* requests all available data types */
        public boolean requestAvailableMeters(Callback_Client client) {
            boolean success = false;
            if (client != null) {
                Message msg = mHandler.obtainMessage(MESSAGE_REQUEST_AVAILABLE_METERS, client);
                mHandler.sendMessage(msg);
                success = true;
            } else {
                Logger.w(TAG, "requestAvailableMeters Error - Callback_Client cannot be null");
            }
            return success;
        }
    };

    /* MeterReader service provides two interfaces. One for Meters and one for Applications */
    @Override
    public IBinder onBind(Intent intent) {
        IBinder binder = null;

        if (intent != null) {
            String intentAction = intent.getAction();
            if (intentAction == null) {
                Logger.d(TAG,"intentAction is null onBind()");
            } else if (intentAction.equals(Meter.INTENT_BIND_METER)) {
                binder = binderMeterReader;
            } else if (intentAction.equals(CollectionServiceAPI.INTENT_BIND_FRAMEWORK)) {
                binder = binderCollectionService;
            }
        }

        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        /* make sure we only return from the function
         * after mHandler is created,
         * by setting a latch
         */
        final CountDownLatch latch = new CountDownLatch(1);
        new ServiceHandlerThread(latch).start();
        boolean success = false;
        try {
            success = latch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Logger.e(TAG, "Interrupted while creating ServiceHandlerThread!");
        }
        if (!success) {
            Logger.e(TAG, "Handler creation failed due to timeout!");
        }

        meterCallbacks = new RemoteCallbackList<Callback_Meter>();
        clientCallbacks = new RemoteCallbackList<Callback_Client>();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        /* During unit test sometimes when tests fail,
         * onCreate and onDestroy get called immediately after each other,
         * during setup() and teardown().
         * For some reason in this case,
         * variables initialized in onCreate are actually null.
         * So make sure they are not null before accessing them.
         */
        if (mHandler != null) {
            /* we always have to quit the looper */
            mHandler.getLooper().quit();
        }
        if (meterCallbacks != null) {
            meterCallbacks.kill();
        }
        if (clientCallbacks != null) {
            clientCallbacks.kill();
        }
    }

    /** For Unit Testing purpose.
    * @return READ_METER_EXPIRE_TIME_MS value
    */
    public static long getReadMeterExpireTimeMs() {
        return READ_METER_EXPIRE_TIME_MS;
    }

    private void messageHandler(Message msg) {
        Callback_Client client;

        switch (msg.what) {
        case MESSAGE_REQUEST_READING:
            Logger.d(TAG, "MESSAGE_REQUEST_READING");
            RequestInfo info = (RequestInfo)msg.obj;

            if (info == null) {
                Logger.e(TAG, "RequestInfo is null.");
            } else if (info.dataTypes == null) {
                Logger.e(TAG, "RequestInfo.dataTypes is null.");
            } else if (info.callback == null) {
                Logger.e(TAG, "RequestInfo.callback is null.");
            } else if (queueMeterRead(info.dataTypes, info.callback) != true) {
                try {
                    info.callback.readingErrorCallback(info.dataTypes);
                } catch (RemoteException e) {
                    Logger.d(TAG, "RemoteException on readingErrorCallback");
                }
            }
            break;

        case MESSAGE_REQUEST_ALL_READINGS:
            Logger.d(TAG, "MESSAGE_REQUEST_ALL_READINGS");
            client = (Callback_Client)msg.obj;
            if (client != null) {
                ArrayList<String> meters = getRegisteredMeterTypes();
                if (meters == null || queueMeterRead(meters, client) != true) {
                    try {
                        client.readingErrorCallback(null);
                    } catch (RemoteException e) {
                        Logger.d(TAG, "RemoteException on readingErrorCallback");
                    }
                }
            }
            break;

        case MESSAGE_REQUEST_AVAILABLE_METERS:
            Logger.d(TAG, "MESSAGE_REQUEST_AVAILABLE_METERS");
            client = (Callback_Client)msg.obj;
            if (client != null) {
                try {
                    client.availableMetersCallback(getRegisteredMeterTypes());
                } catch (RemoteException e) {
                    Logger.d(TAG, "RemoteException on availableMetersCallback");
                }
            }
            break;

        case MESSAGE_REPORT_DATA:
            Logger.d(TAG, "MESSAGE_REPORT_DATA");
            handleDataReport((Bundle)msg.obj);
            break;

        case MESSAGE_READ_METER_TIMER_EXPIRED:
            Logger.d(TAG, "MESSAGE_READ_METER_TIMER_EXPIRED");

            /* if peripheral did not respond, send an empty bundle
             * that just has the dataType to signify the error
             */
            String dataType = String.valueOf(msg.obj);
            Bundle bundle = new Bundle();
            bundle.putString(Meter.METER_DATA_TYPE, dataType);
            handleDataReport(bundle);
            break;

        default:
            Logger.d(TAG, "messageHandler - Unknown Message");
            break;
        }
    }

    /* adds the client to the callback list waiting for reply from meter of dataType
     * If there is no pending request, initiates an actual meter read.
     *
     * returns true if meter of dataType exists and registration is successful,
     * false otherwise
     */
    private boolean queueMeterRead(ArrayList<String> dataTypes, Callback_Client client) {

        if (dataTypes == null || client == null) {
            return false;
        }

        PendingMeters requestedMeters;

        ArrayList<String> metersToRead = new ArrayList<String>();
        metersToRead.ensureCapacity(dataTypes.size());

        PendingMeters newMeterRequests = new PendingMeters();
        newMeterRequests.pending = new ArrayList<String>();
        newMeterRequests.pending.ensureCapacity(dataTypes.size());

        boolean requestPending = false;

        int clientCallbackSize = clientCallbacks.beginBroadcast();

        for (String dataType : dataTypes) {
            Logger.d(TAG, "queueMeterRead(" + dataType + ")");
            /* make sure the meter is registered and it's not a duplicate */
            if (dataType != null && dataType.length() > 0 &&
                    meterExists(dataType) && !newMeterRequests.pending.contains(dataType)) {
                requestPending = false;
                newMeterRequests.pending.add(dataType);

                /* check if we already have a pending request, in which case we don't have to
                 * call readMeter() again
                 */
                clientLoop:
                for (int i = 0; i < clientCallbackSize; i++) {
                    requestedMeters = (PendingMeters) clientCallbacks.getBroadcastCookie(i);
                    if (requestedMeters != null &&
                            requestedMeters.pending != null &&
                            requestedMeters.pending.contains(dataType)) {
                        requestPending = true;
                        break clientLoop;
                    }
                }

                /* only request an actual read if there wasn't any pending read */
                if (!requestPending) {
                    metersToRead.add(dataType);
                }
            }
        }
        if (newMeterRequests.pending.size() > 0) {
            /* regardless of other clients, we still have to add the current client to the callback list
             * and associate it with this dataType
             */
            clientCallbacks.register(client, newMeterRequests);
        }
        clientCallbacks.finishBroadcast();

        if (metersToRead.size() > 0) {
            readMeter(metersToRead, newMeterRequests.pending);
        }

        return (newMeterRequests.pending.size() > 0);
    }

    /* would be nice to use SystemProperties.get() but that's an internal API,
     * and SystemProperty.get doesn't get us getprop values.
     * An alternative maybe is to use "exec getprop" style
     */
    private boolean logToFile() {
        File logOnFile = new File(LOG_CONFIG_FILE_NAME);
        return (logOnFile.exists());
    }

    private void handleDataReport(Bundle bundle) {
        String dataType = null;

        if (bundle != null) {
            dataType = CollectionServiceAPI.getDataType(bundle);
        }
        if (dataType == null) {
            Logger.e(TAG, "received null dataType");
            return;
        }

        if (FILE_DEBUG_MODE && logToFile()) {
            Logger.d(TAG,"Logging meter readings to: " + LOG_OUTPUT_FILE_NAME);

            String timestamp = String.valueOf(CollectionServiceAPI.getTimestamp(bundle));
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String formattedTime = simpleDateFormat.format(new Date(CollectionServiceAPI.getTimestamp(bundle)));
            String rawData = CollectionServiceAPI.getRawData(bundle);
            if (rawData == null) {
                /* we want to log this in the output file */
                rawData = "null";
            }

            BufferedWriter bwriter = null;
            boolean append = true;
            try {
                bwriter = new BufferedWriter(new FileWriter(LOG_OUTPUT_FILE_NAME, append));

                bwriter.write(dataType);
                bwriter.newLine();

                bwriter.write(timestamp);
                bwriter.newLine();

                bwriter.write(formattedTime);
                bwriter.newLine();

                bwriter.write(rawData);
                bwriter.newLine();
                bwriter.newLine();

                bwriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bwriter != null) {
                    try {
                        bwriter.close();
                    } catch (IOException e) {
                        Logger.e(TAG,"IOException in bwriter.close()");
                    }
                    bwriter = null;
                }
            }
        }

        int i = clientCallbacks.beginBroadcast();
        Callback_Client client = null;
        PendingMeters pendingMeters;
        String savedMeterType;

        /* go through all clients and report data to whoever requested it */
        while (i > 0) {
            i--;

            pendingMeters = (PendingMeters) clientCallbacks.getBroadcastCookie(i);

            if (pendingMeters != null &&
                    pendingMeters.pending != null &&
                    pendingMeters.pending.contains(dataType)) {
                Logger.d(TAG, "handleDataReport found match - " + dataType);

                /* Remove the timer expiration.
                 * As mentioned before, this is a very round about way checking the meterType, which is just a string.
                 * However, it has to be done this way because mHandler.removeMessage(int what, Object object)
                 * expects the exact same object that was used for obtainMessage. Even if the
                 * string values were the same, it wouldn't work unless it's exactly the same object.
                 * This is because mHandler.removeMessage compares object with == operator, not the .equals method.
                 *
                 * Since we already save the meterType string in clientCallback's pendingMeters.pending list,
                 * we are using that.
                 */
                savedMeterType = pendingMeters.pending.get(pendingMeters.pending.indexOf(dataType));
                if (savedMeterType != null) {
                    mHandler.removeMessages(MESSAGE_READ_METER_TIMER_EXPIRED, savedMeterType);
                }

                if (pendingMeters.bundles == null) {
                    pendingMeters.bundles = new ArrayList<Bundle>();
                    pendingMeters.bundles.ensureCapacity(pendingMeters.pending.size());
                }
                /* populate our response bundle array */
                pendingMeters.bundles.add(bundle);

                /* only send the bundle array if we received response from all the meters
                 * we were waiting for. Otherwise, just populate the bundle array and wait for
                 * other responses to arrive
                 */
                pendingMeters.pending.remove(dataType);
                if (pendingMeters.pending.size() == 0) {
                    client = clientCallbacks.getBroadcastItem(i);
                    if (client != null) {
                        try {
                            client.readingCallback(pendingMeters.bundles);
                        } catch (RemoteException e) {
                            Logger.d(TAG, "RemoteException on readingCallback.");
                        }
                        clientCallbacks.unregister(client);
                    } else {
                        //it should never be null
                        Logger.e(TAG, "Received null client.");
                    }
                }
            }
        }
        clientCallbacks.finishBroadcast();
    }

    private ArrayList<String> getRegisteredMeterTypes() {
        ArrayList<String> dataTypes = null;
        int i = meterCallbacks.beginBroadcast();
        if (i > 0) {
            dataTypes = new ArrayList<String>();
            dataTypes.ensureCapacity(i);
            while (i > 0) {
                i--;
                dataTypes.add(String.valueOf(meterCallbacks.getBroadcastCookie(i)));
            }
        }
        meterCallbacks.finishBroadcast();
        return dataTypes;
    }

    /**
     * Find the meter with the corresponding MeterReading type, and read that meter.
     *
     * Do not cache the callback returned by getBroadCastItem, since the data is not valid
     * after calling finishBroadcast()
     *
     * @param dataTypes The dataTypes to request the readings on. readMeter will be called on the meters listed here.
     * @param savedMeterTypes It is very important that the ArrayList passed in here is the one that's used to
     *                       register with clientCallbacks. This is used to save the object for READ_METER timer expiration.
     */
    private void readMeter(ArrayList<String> dataTypes, ArrayList<String> savedMeterTypes) {
        if (dataTypes != null && dataTypes.size() > 0) {
            Callback_Meter meterCallback = null;
            int i = meterCallbacks.beginBroadcast();
            int count = 0;
            int savedMeterIndex = -1;
            String meterType = null;
            Message expireMsg = null;
            while (i > 0) {
                i--;
                meterType = (String) meterCallbacks.getBroadcastCookie(i);
                if (dataTypes.contains(meterType)) {
                    meterCallback = meterCallbacks.getBroadcastItem(i);
                    if (meterCallback != null) {
                        try {
                            /* anything we do between beginBroadcast() and finishBroadcast() should finish ASAP,
                             * since it will prevent other threads from entering this critical section
                             */

                            /* start the READ_METER expiration timer.
                             * This timer is used to handle the case where the peripheral doesn't respond
                             */
                            if (savedMeterTypes != null) {
                                savedMeterIndex = savedMeterTypes.indexOf(meterType);
                                if (savedMeterIndex >= 0) {
                                    /* This is a very round about way of saving the meterType, which is just a string.
                                     * However, it has to be done this way because when we call mHandler.removeMessage,
                                     * it expects the exact same object that was used for obtainMessage. Even if the
                                     * string values were the same, it wouldn't work unless it's exactly the same object.
                                     * This is because mHandler.removeMessage compares object with == operator, not the .equals method.
                                     *
                                     * Since we already save the meterType string in clientCallback's pendingMeters.pending list,
                                     * we are using that.
                                     */
                                    expireMsg = mHandler.obtainMessage(MESSAGE_READ_METER_TIMER_EXPIRED, savedMeterTypes.get(savedMeterIndex));
                                    Logger.d(TAG, "expireMsg with dataType: " + meterType);
                                    mHandler.sendMessageDelayed(expireMsg, READ_METER_EXPIRE_TIME_MS);
                                }
                            }

                            meterCallback.readMeter();
                            count++;
                        } catch (RemoteException e) {
                            Logger.d(TAG, "RemoteException on readMeter");
                        }
                        if (count == dataTypes.size()) {
                            break;
                        }
                    } else {
                        //meterCallback should never be null
                        Logger.e(TAG, "meterCallback for readMeter is null.");
                    }
                }
            }
            meterCallbacks.finishBroadcast();
        }
    }

    /* check if the meter of dataType exists
     *
     * returns true if meter of dataType is found,
     * false otherwise
     */
    private boolean meterExists(String dataType) {
        boolean exists = false;
        if (dataType != null && dataType.length() > 0) {
            int i = meterCallbacks.beginBroadcast();

            while (i > 0) {
                i--;
                if (dataType.equals(meterCallbacks.getBroadcastCookie(i))) {
                    exists = true;
                    break;
                }
            }

            meterCallbacks.finishBroadcast();
        }

        Logger.d(TAG, "meterExists(" + dataType + ") - " + exists);

        return exists;
    }
}
