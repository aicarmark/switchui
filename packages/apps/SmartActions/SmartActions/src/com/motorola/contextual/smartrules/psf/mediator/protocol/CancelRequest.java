/*
 * @(#)CancelRequest.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/05/22  NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.psf.mediator.protocol;

import java.util.ArrayList;
import java.util.List;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.psf.mediator.MediatorConstants;
import com.motorola.contextual.smartrules.psf.mediator.MediatorHelper;
import com.motorola.contextual.smartrules.psf.table.MediatorTable;
import com.motorola.contextual.smartrules.psf.table.MediatorTuple;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

/**
 * This class handles "cancel" command from consumers.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Implements IMediatorProtocol 
 *
 * RESPONSIBILITIES:
 * Send the "cancel" command intent to desired condition publisher
 *
 * COLABORATORS:
 *     Consumer - Uses the preconditions available across the system
 *     ConditionPublisher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public class CancelRequest implements IMediatorProtocol, MediatorConstants, DbSyntax {

    private static final String TAG = MEDIATOR_PREFIX + CancelRequest.class.getSimpleName();

    private String mConsumer;
    private String mConfig;
    private String mPublisher;
    private String mRequestId;

    public CancelRequest(Intent intent) {
        mConsumer = intent.getStringExtra(EXTRA_CONSUMER);
        mConfig = intent.getStringExtra(EXTRA_CONFIG);
        mPublisher = intent.getStringExtra(EXTRA_PUBLISHER_KEY);
        mRequestId = intent.getStringExtra(EXTRA_REQUEST_ID);
    }


    public boolean processRequest(Context context, Intent intent) {
        return (mConsumer != null && mConfig != null && mPublisher != null &&
                mRequestId != null && MediatorHelper.isMediatorInitialized(context)) ? true : false;
    }

    public List<Intent> execute(Context context, Intent intent) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();
        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = null;
        String where = MediatorTable.Columns.CONFIG + EQUALS + Q + mConfig + Q + AND +
                       MediatorTable.Columns.PUBLISHER + EQUALS + Q + mPublisher + Q;

        try {
            cursor = contentResolver.query(MediatorTable.CONTENT_URI, null, where, null, null);

            if (cursor != null) {
                int count = cursor.getCount();
                if (count > 1) {
                    //Multiple consumers have subscribed to the publisher and config
                    //Remove only that consumer for which cancel is requested
                    intentsToBroadcast = handleMultipleConsumersSubscribed(contentResolver, cursor);
                } else if (count == 1) {
                    //Only one consumer has subscribed for publisher and config
                    //Send the request to the publisher to cancel the subscription
                    intentsToBroadcast = handleSingleConsumerSubscribed(contentResolver, cursor);
                } else {
                    Log.w(TAG, "Cancel received for unsubscribed config");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return intentsToBroadcast;
    }

    /**
     * Method to be called when cancel is requested for a config which is subscribed by multiple consumers.
     * Mediator returns cancel response with forwarding the request to the publisher.
     *
     * @param contentResolver
     * @param cursor Mediator table cursor with info about all the consumers subscribed to a particular config
     */
    private List<Intent> handleMultipleConsumersSubscribed(ContentResolver contentResolver, Cursor cursor) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();
        cursor.moveToFirst();
        do {
            String consumerFromDb = cursor.getString(cursor.getColumnIndex(MediatorTable.Columns.CONSUMER));
            if (consumerFromDb != null && consumerFromDb.equals(mConsumer)) {
                //This consumer has requested cancel
                MediatorHelper.deleteFromMediatorDatabase(contentResolver, mConsumer,
                        mPublisher, mConfig);

                if (!mRequestId.contains(MEDIATOR_SEPARATOR)) {
                    intentsToBroadcast.add(MediatorHelper.createCommandResponseIntent(mConsumer, mConfig,
                                           mRequestId, CANCEL_RESPONSE_EVENT, mPublisher, SUCCESS, null, null));
                } else {
                    //Mediator itself requested cancel. No need to send cancel response.
                    if (Constants.LOG_DEBUG) Log.d(TAG, "Mediator requested cancel completed");
                }
                break;
            }
        } while (cursor.moveToNext());
        return intentsToBroadcast;
    }

    /**
     * Method to be called when cancel is requested for a config which is subscribed by a single consumer.
     * Mediator forwards the request to the publisher.
     *
     * @param contentResolver
     * @param cursor Mediator table cursor with info about the subscribed consumer
     * @return
     */
    private List<Intent> handleSingleConsumerSubscribed(ContentResolver contentResolver, Cursor cursor) {
        cursor.moveToFirst();
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();
        MediatorTuple mediatorTuple = new MediatorTuple(cursor);
        String consumerFromDb = mediatorTuple.getConsumer();

        if (consumerFromDb != null && consumerFromDb.equals(mConsumer)) {
            mediatorTuple.addCancelRequestId(mRequestId);
            MediatorHelper.updateTuple(contentResolver, mediatorTuple);
            intentsToBroadcast.add(MediatorHelper.createCommandIntent(mPublisher, mConfig,
                                   mRequestId, CANCEL_EVENT));
        } else {
            Log.w(TAG, "Cancel sent by consumer which has not subscribed");
        }
        return intentsToBroadcast;
    }

}
