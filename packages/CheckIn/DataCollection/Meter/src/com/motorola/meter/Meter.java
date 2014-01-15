/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number         Brief Description
 * ------------------------- ---------- -------------------- ------------------------------
 * w04917 (Brian Lee)        2011/05/06   IKCTXTAW-272       Initial version
 * w04917 (Brian Lee)        2012/07/19   IKHSS6UPGR-14637   Handle Gson init error
 */

package com.motorola.meter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

import com.motorola.meter.reader.IMeterReader;
import com.motorola.meter.reading.MeterReading;

/** Base class that handles the communication with the MeterReader service.
 *
 * @author w04917 (Brian Lee)
 */
public abstract class Meter {

    private static final String TAG = "Meter";

    private static final int CALLBACK_HANDLER_THREAD_CREATION_WAIT_TIME_MS = 100;

    /* intent used by peripheral to bind to MeterReader service */
    public static final String INTENT_BIND_METER = "com.motorola.meter.intent.bind";

    /* Bundle Key Strings for MeterReader */
    public static final String METER_READING_JSON = "METER_READING_JSON";
    public static final String METER_DATA_TYPE = "METER_DATA_TYPE"; //this is the simple class name of the MeterReading subclass
    public static final String METER_TIMESTAMP = "METER_TIMESTAMP";

    private Context mContext;
    private boolean isBound = false;
    IMeterReader meterReader;
    private String simpleClassName; //for logging purposes to show subclass name
    private String dataType; //full class name for object returned by getMeterReading, used in unpacking

    private static final int MESSAGE_REPORT_DATA = 1;

    /* to switch process and do the calculation asynchronously */
    private Handler mHandler;
    /* Handlers are attached to the thread that created it.
     * Normally, this shouldn't matter.
     * However, when running Android Unit Test,
     * the Meter, MeterReader, and the Application will all be
     * created in the same thread. This means that the handler will
     * be blocked from processing messages while trying to handle
     * the simulated components in an asynchronous way.
     * The solution is to make sure the handler runs in its own thread.
     */
    private class CallbackHandlerThread extends Thread {
        CountDownLatch latch;

        public CallbackHandlerThread(CountDownLatch newLatch) {
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

    private void messageHandler(Message msg) {
        switch (msg.what) {
        case MESSAGE_REPORT_DATA:
            Bundle data = getMeterReadingBundle();
            if (meterReader != null) {
                try {
                    meterReader.reportData(data);
                } catch (RemoteException e) {
                    meterLog(e);
                }
            } else {
                Logger.e(TAG, simpleClassName + " - MeterReader is null on MESSAGE_REPORT_DATA.");
            }
            break;
        }
    }

    public Meter(Context context) {
        mContext = context;
        simpleClassName = this.getClass().getSimpleName();
        /* log should be after we get simpleClassName */
        meterLog("Constructor");
    }

    private void meterLog(String log) {
        Logger.d(TAG, simpleClassName + " - " + log);
    }

    private void meterLog(Exception e) {
        Logger.e(TAG, simpleClassName + " - " + e.toString());
    }

    /* connected to MeterReader service */
    private ServiceConnection meterReaderConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            meterLog("onServiceConnected");

            meterReader = IMeterReader.Stub.asInterface(service);
            if (meterReader != null) {
                try {
                    meterReader.registerMeter(meterCallback, dataType);
                } catch (RemoteException e) {
                    meterLog(e);
                }
                /* do not remove. required for unit tests and special cases */
                onMeterReaderConnected();
            } else {
                Logger.e(TAG, simpleClassName + " - MeterReader is null");
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            meterLog("onServiceDisconnected");
            meterReader = null;
        }
    };

    /* Meter.registerMeter returns immediately after making an asynchronous request
     * to bind to the MeterReader. Meter is not actually bound to MeterReader until
     * meterReaderConnection.onServiceConnected is called. Normally, this shouldn't matter because
     * the request to read the meters come far after meters are registered.
     *
     * However, in unit test, meters are created, registered, and requested to be read
     * immediately. In the case of the unit test, request to read may come before the meter is actually
     * bound.
     *
     * In this special case, we want to make sure MeterReader is actually connected before we request to read.
     * To do so, Meter subclasses used in the unit tests can override this method, which gets called
     * in meterReaderConnection.onServiceConnected.
     *
     * This can also be overridden by subclasses who would like to do something special when meterReader
     * is actually connected. Normally, this is not the case, and subclasses shouldn't override this method.
     */
    protected void onMeterReaderConnected() {}

    /* callback invoked from MeterReader service */
    private Callback_Meter meterCallback = new Callback_Meter.Stub() {
        public void readMeter() {
            meterLog("readMeter");
            /* return the data asynchronously, since this is a oneway interface
             * and since the MeterReader calls this function in a critical section
             */
            Message msg = mHandler.obtainMessage(MESSAGE_REPORT_DATA);
            mHandler.sendMessage(msg);
        }
    };

    /** register interface exposed to peripherals
     *
     * @param meterReadingSubclass The MeterReading subclass used. It is used to identify each Meter.
     * The assumption is that this is unique to each Meter.
     *
     * @return True if we're already bound to service or if we successfully requested to bind,
     * false otherwise
     */
    public final boolean registerMeter(Class<? extends MeterReading> meterReadingSubclass) {
        meterLog("registerMeter");
        if (!isBound && meterReadingSubclass != null) {
            /* make sure we only return from the function
             * after mHandler is created,
             * by setting a latch
             */
            final CountDownLatch latch = new CountDownLatch(1);
            new CallbackHandlerThread(latch).start();
            boolean success = false;
            try {
                success = latch.await(CALLBACK_HANDLER_THREAD_CREATION_WAIT_TIME_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                meterLog(new Exception("Interrupted while creating CallbackHandlerThread!"));
                e.printStackTrace();
            }

            if (success) {
                dataType = meterReadingSubclass.getCanonicalName();
                isBound = mContext.bindService(new Intent(INTENT_BIND_METER), meterReaderConnection, Context.BIND_AUTO_CREATE);
            } else {
                meterLog(new Exception("Unable to create CallbackHandlerThread!"));
            }
        }
        return isBound;
    }

    /** unregister interface exposed to peripherals
     */
    public final void unregisterMeter() {
        meterLog("unregisterMeter");
        if (isBound) {
            isBound = false;
            try {
                if (meterReader != null) {
                    meterReader.unregisterMeter(meterCallback);
                    meterReader = null;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mContext.unbindService(meterReaderConnection);
            if (mHandler != null) {
                mHandler.getLooper().quit();
            }
        }
    }

    /* read the MeterReading data, blocking call, may take a long time */
    private Bundle getMeterReadingBundle() {
        meterLog("getMeterReadingBundle");

        long timestamp = System.currentTimeMillis();
        MeterReading meterReading =  getMeterReading();
        String jsonString = meterReading.pack();

        Bundle bundle = new Bundle();
        bundle.putLong(METER_TIMESTAMP, timestamp);
        bundle.putString(METER_DATA_TYPE, dataType);
        if (jsonString != null) {
            bundle.putString(METER_READING_JSON, jsonString);
        } else {
            Logger.e(TAG, "Unable to pack " + dataType);
        }
        return bundle;
    }

    /* abstract method that returns calculated MeterReading data */
    protected abstract MeterReading getMeterReading();
}

