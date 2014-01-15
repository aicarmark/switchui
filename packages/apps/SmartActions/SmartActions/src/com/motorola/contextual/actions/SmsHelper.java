/*
 * @(#)SmsHelper.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984        2011/11/02  IKINTNETAPP-444   Initial version
 *
 */
package com.motorola.contextual.actions;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.SmsManager;
import android.util.Log;

import com.motorola.contextual.smartrules.R;
import com.motorola.contracts.messaging.IMessagingService;
import com.motorola.contracts.messaging.IMessagingServiceCallback;
import com.motorola.contracts.messaging.Message;

/** This class is responsible for sending SMS.
 * <code><pre>
 *
 * CLASS:
 *  implements Constants
 *
 * RESPONSIBILITIES:
 *  Interacts with Messaging Service
 *
 * COLABORATORS:
 *  Messaging Service
 *
 *
 * USAGE:
 *  See each method.
 *
 * </pre></code>
 */

public class SmsHelper implements Constants {

    private static final String TAG = TAG_PREFIX + SmsHelper.class.getSimpleName();

    private static final String SEND_SMS_ACTION     = "android.intent.action.SEND";
    private static final String EXTRA_RECIPIENT_ID  = "com.motorola.blur.util.messaging.QuickMessageUtil.extra.RECIPIENT_ID";
    private static final String SEND_SMS_CATEGORY   = "com.motorola.blur.util.messaging.QuickMessageUtil.SMS";
    private static final String SEND_SMS_MIME_TYPE  = "text/plain";
    private static final String ACTION_SMS_SENT     = "QUICK_ACTION_SMS_SENT";
    private static final int    TIMEOUT_MS          = 5000;


    private IMessagingService mMessagingService;
    private ServiceConnection mServiceConnection;
    private Context mContext;
    private final Lock mLock = new ReentrantLock();
    private final Condition mBound  = mLock.newCondition();

    SmsHelper(Context context) {
        mContext = context;
    }

    /** Closes the service connection if one exists
     *
     */
    void close() {
        mLock.lock();
        try {
            if (mServiceConnection != null) {
                mContext.unbindService(mServiceConnection);
                mServiceConnection = null;
            }
            mMessagingService = null;
        } finally {
            mLock.unlock();
        }
    }

    /** API to send an sms. Uses one of Messaging service or QuickMessageUtil broadcast
     * to send the sms. Falls back to Android SMS manager if both of
     * them don't exist on the phone.
     *
     * @param numbers
     * @param msg
     * @param statusIntent
     */
    void send(String[] numbers, String msg, Intent statusIntent) {
        boolean status = false;
        boolean sendStatus = true;
        if (isServicePresent(mContext)) {
            status = sendUsingService(numbers, msg);

        } else if (isBroadcastPresent(mContext)) {
            if (LOG_INFO) Log.i(TAG, "Sending broadcast for Numbers: " + numbers.length);
            for (String number: numbers) {
                sendUsingBroadcast(mContext, msg, number);
            }
            status = true;

        } else {
            sendStatus = false;
            if (LOG_INFO) Log.i(TAG, "Sending Message to Numbers: " + numbers.length);
            sendUsingSmsManager(numbers, msg, statusIntent);
        }

        if (statusIntent != null && sendStatus) {
            ActionHelper.sendActionStatus(mContext, statusIntent, status,
                                                   (status) ? null : mContext.getString(R.string.send_failure));
        }
    }

    /** Send sms using messaging service
     *
     * @param numbers
     * @param msg
     * @return true if the sms was sent, false otherwise
     */
    private boolean sendUsingService(String[] numbers, String msg) {

        boolean status = false;
        status = bindToMessagingService();

        if (status) {
            status = false;
            IMessagingServiceCallback.Stub callback = new IMessagingServiceCallback.Stub() {

                public void onSendRequestProcessed(Uri uri, int requestId, String recipient)
                throws RemoteException {
                    if (LOG_INFO) Log.i(TAG, "onSendRequestProcessed " + requestId + "," + recipient);
                }

                public void onSendRequestFailed(int requestId, String recipient, int errorNo)
                throws RemoteException {
                    Log.e(TAG, "onSendRequestFailed " + requestId + "," + recipient);
                }
            };
            Message message = new Message();
            message.setMessageBody(msg);
            for (String n: numbers) {
                message.addRecipient(n);
            }

            mLock.lock();

            try {
                if (mMessagingService != null) {

                    try {
                        int requestId = mMessagingService.sendMessage(message, callback, 0);
                        if (LOG_INFO) Log.i(TAG, "Request id is " + requestId);
                        status = true;
                    } catch (RemoteException e) {
                        Log.e(TAG, "remote exception when sending message");
                        e.printStackTrace();
                    }

                } else {
                    Log.e(TAG, "Bind to messaging service failed");
                }
            } finally {
                mLock.unlock();
            }
        }

        return status;
    }

    /** Binds to messaging service
     *
     * @return true if bind succeeded, false otherwise
     */
    private boolean bindToMessagingService() {
        boolean status = false;
        if (LOG_INFO) Log.i(TAG, "bindToMessagingService");

        ServiceConnection conn = new ServiceConnection() {
            public void onServiceDisconnected(ComponentName name) {
                mLock.lock();
                try {
                    mMessagingService = null;
                    if (mServiceConnection != null) {
                        mContext.unbindService(mServiceConnection);
                        mServiceConnection = null;
                    }
                    if (LOG_INFO) Log.i(TAG, "onServiceDisConnected");
                } finally {
                    mLock.unlock();
                }
            }

            public void onServiceConnected(ComponentName name, IBinder service) {
                mLock.lock();
                try {
                    mMessagingService = IMessagingService.Stub.asInterface(service);
                    mBound.signal();
                    if (LOG_INFO) Log.i(TAG, "onServiceConnected");
                } finally {
                    mLock.unlock();
                }
            }
        };

        Intent serviceIntent = new Intent(com.motorola.contracts.messaging.Intent.ACTION_SEND_MESSAGE);
        serviceIntent.addCategory(Intent.CATEGORY_DEFAULT);
        mLock.lock();
        try {
            if (mMessagingService == null) {

                boolean serviceConnected = mContext.bindService(serviceIntent,
                                           conn,
                                           Context.BIND_AUTO_CREATE);
                if (serviceConnected) {
                    int attempts = 0;
                    while (mMessagingService == null && attempts < 2) {
                        try {
                            mBound.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        ++attempts;
                    }
                    if (mMessagingService == null) {
                        mContext.unbindService(conn);
                    } else {
                        mServiceConnection = conn;
                        status = true;
                    }

                } else {
                    //Failure
                    Log.e(TAG, "Service Connection failed");
                }
            } else {
                //already connected
                status = true;
           }
        } finally {
            mLock.unlock();
        }

        return status;
    }

    /**
     * Method to send SMS using blur so that the Message is added to Sent items
     *
     * @param context Application context
     * @param message Message to be sent
     * @param recepientNumber Number of the recipient
     */
    private static void sendUsingBroadcast (Context context, String message, String recipientNumber) {
        Intent i = new Intent(SEND_SMS_ACTION);
        i.putExtra(EXTRA_RECIPIENT_ID, recipientNumber);
        i.putExtra(Intent.EXTRA_TEXT, message);
        i.addCategory(SEND_SMS_CATEGORY);
        i.setType(SEND_SMS_MIME_TYPE);
        context.sendBroadcast(i);
    }


    /** Method to send sms using Android SMS manager
     *
     * @param numbers
     * @param message
     * @param statusIntent
     */
    private void sendUsingSmsManager(String[] numbers, String message, Intent statusIntent) {

        Intent intent = new Intent(ACTION_SMS_SENT);
        if (statusIntent != null) {
            intent.putExtra(EXTRA_STATUS_INTENT, statusIntent);
        }
        PendingIntent sentPendingIntent = PendingIntent.getBroadcast(mContext, 0,
                                          intent, PendingIntent.FLAG_ONE_SHOT);

        mContext.getApplicationContext().registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent smsIntent) {
                handleSmsSendResponse(context, getResultCode(), smsIntent);
                context.getApplicationContext().unregisterReceiver(this);
            }
        }, new IntentFilter(ACTION_SMS_SENT));

        try {
            for (String number: numbers) {
                SmsManager.getDefault().sendTextMessage(number, null, message, sentPendingIntent, null);
            }

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            ActionHelper.sendActionStatus(mContext, statusIntent, false, mContext.getString(R.string.send_failure));
        }
    }


    /** Handle the response sent by Android SMS manager
     *
     * @param context
     * @param resultCode
     * @param intent
     */
    private static void handleSmsSendResponse(Context context, int resultCode, Intent intent) {

        Intent statusIntent = intent.getParcelableExtra(EXTRA_STATUS_INTENT);
        if (statusIntent != null) {
            boolean status = false;
            String exceptionString = null;
            switch (resultCode) {
            case Activity.RESULT_OK:
                if (LOG_INFO) Log.i(TAG, "SMS successfully sent");
                status = true;
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE: {
                exceptionString = context.getString(R.string.send_failure);
                break;
            }
            case SmsManager.RESULT_ERROR_NO_SERVICE: {
                exceptionString = context.getString(R.string.no_service);
                break;
            }
            case SmsManager.RESULT_ERROR_NULL_PDU: {
                exceptionString = context.getString(R.string.send_failure);
                break;
            }
            case SmsManager.RESULT_ERROR_RADIO_OFF: {
                exceptionString = context.getString(R.string.send_failure);
                break;
            }
            default: {
                exceptionString = context.getString(R.string.unknown_error);
                break;
            }
            }

            ActionHelper.sendActionStatus(context, statusIntent, status, exceptionString);
        }
    }

    /**
     * Method to check if the service which sends SMS is present
     *
     * @param context
     * @return true if the service is present, false otherwise
     */
    private static boolean isServicePresent(Context context) {
        Intent serviceIntent = new Intent(com.motorola.contracts.messaging.Intent.ACTION_SEND_MESSAGE);
        serviceIntent.addCategory(Intent.CATEGORY_DEFAULT);
        return (context.getPackageManager().queryIntentServices(serviceIntent,
                PackageManager.GET_RESOLVED_FILTER).size() > 0);
    }

    /** Method to check if the broadcast receiver which sends SMS is present
     *
     * @param context
     * @return true if the receiver is present, false otherwise
     */
    private static boolean isBroadcastPresent(Context context) {

        Intent bIntent = new Intent(SEND_SMS_ACTION);
        bIntent.addCategory(SEND_SMS_CATEGORY);
        bIntent.setType(SEND_SMS_MIME_TYPE);
        return (context.getPackageManager().queryBroadcastReceivers(bIntent,
                PackageManager.GET_RESOLVED_FILTER).size() > 0);

    }

}
