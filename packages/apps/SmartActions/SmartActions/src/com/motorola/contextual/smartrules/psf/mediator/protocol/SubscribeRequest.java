/*
 * @(#)SubscribeRequest.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/05/21  NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.psf.mediator.protocol;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.psf.mediator.MediatorConstants;
import com.motorola.contextual.smartrules.psf.mediator.MediatorHelper;
import com.motorola.contextual.smartrules.psf.table.MediatorTable;
import com.motorola.contextual.smartrules.psf.table.MediatorTuple;

/**
 * This class handles "subscribe" command from consumers.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Implements IMediatorProtocol 
 *
 * RESPONSIBILITIES:
 * Send the "subscribe" command intent to desired condition publisher
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

public class SubscribeRequest implements IMediatorProtocol, MediatorConstants, DbSyntax {

    private static final String TAG = SubscribeRequest.class.getSimpleName();
    private String mConsumer;
    private String mConsumerPackage;
    private String mConfig;
    private String mPublisher;
    private String mRequestId;

    public SubscribeRequest(Intent intent) {
        mConsumer = intent.getStringExtra(EXTRA_CONSUMER);
        mConsumerPackage = intent.getStringExtra(EXTRA_CONSUMER_PACKAGE);
        mConfig = intent.getStringExtra(EXTRA_CONFIG);
        mPublisher = intent.getStringExtra(EXTRA_PUBLISHER_KEY);
        mRequestId = intent.getStringExtra(EXTRA_REQUEST_ID);
    }

    public boolean processRequest(Context context, Intent intent) {
        return (mConsumer != null && mConfig != null && mPublisher != null && mRequestId != null &&
                mConsumerPackage != null  && MediatorHelper.isMediatorInitialized(context)) ? true : false;
    }

    public List<Intent> execute(Context context, Intent intent) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();
        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = null;
        Cursor cursor1 = null;
        String where = MediatorTable.Columns.CONSUMER + EQUALS + Q + mConsumer + Q + AND +
                       MediatorTable.Columns.CONFIG + EQUALS + Q + mConfig + Q + AND +
                       MediatorTable.Columns.PUBLISHER + EQUALS + Q + mPublisher + Q;

        try {
            cursor = contentResolver.query(MediatorTable.CONTENT_URI, null, where, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                // Handling case where the consumer has already subscribed
                intentsToBroadcast = handleConsumerSubscribed(contentResolver, cursor);
            } else {
                // Handling case where some other consumer has subscribed for the config
                where = MediatorTable.Columns.CONFIG + EQUALS + Q + mConfig + Q + AND +
                        MediatorTable.Columns.PUBLISHER + EQUALS + Q + mPublisher + Q;
                cursor1 = contentResolver.query(MediatorTable.CONTENT_URI, null, where, null, null);

                if (cursor1 != null && cursor1.getCount() > 0) {
                    //A different consumer has already subscribed for the publisher and config
                    intentsToBroadcast = handleDifferentConsumerSubscribed(contentResolver, cursor1);
                } else {
                    //Entry not present in the database
                    insertToMediatorDatabase(contentResolver, mConsumer, mPublisher, mConfig, mRequestId, null, mConsumerPackage);
                    intentsToBroadcast.add(MediatorHelper.createCommandIntent(mPublisher, mConfig, mRequestId,
                                           SUBSCRIBE_EVENT));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (cursor1 != null) {
                cursor1.close();
            }
        }

        return intentsToBroadcast;
    }

    /**
     * Method to be called when a consumer-config-publisher combination
     * is already present in mediator database. This means that either the state is known
     * or a subscribe request is being processed.
     * @param cursor
     * @return
     */
    private List<Intent> handleConsumerSubscribed(ContentResolver contentResolver, Cursor cursor) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();

        if (cursor.moveToFirst()) {
            //Send subscribe request to the publisher
            MediatorTuple mediatorTuple = new MediatorTuple(cursor);
            mediatorTuple.addSubscribeRequestId(mRequestId);
            MediatorHelper.updateTuple(contentResolver, mediatorTuple);
            intentsToBroadcast.add(MediatorHelper.createCommandIntent(mPublisher, mConfig, mRequestId, SUBSCRIBE_EVENT));
        } else {
            Log.e(TAG, "Cursor problem in handleConsumerSubscribed");
        }

        return intentsToBroadcast;
    }

    /**
     * Method to be called when a different consumer has already subscribed to a publisher-config combination
     * @param contentResolver
     * @param cursor
     * @return
     */
    private List<Intent> handleDifferentConsumerSubscribed(ContentResolver contentResolver, Cursor cursor) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();

        if (cursor.moveToFirst()) {

            MediatorTuple mediatorTuple = new MediatorTuple(cursor);
            String state = mediatorTuple.getState();

            //Another consumer has requested subscribe which is being processed
            //Send request to publisher
            intentsToBroadcast.add(MediatorHelper.createCommandIntent(mPublisher, mConfig, mRequestId,
                                   SUBSCRIBE_EVENT));
            insertToMediatorDatabase(contentResolver, mConsumer, mPublisher, mConfig, mRequestId, state, mConsumerPackage);
        } else {
            Log.e(TAG, "Cursor problem in handleDifferentConsumerSubscribed");
        }
        return intentsToBroadcast;
    }

    /**
     * Method to insert an entry to the mediator database
     *
     * @param contentResolver
     * @param consumer
     * @param publisher
     * @param config
     * @param requestId
     * @param state
     * @param consumerPackage
     */
    private void insertToMediatorDatabase(ContentResolver contentResolver, String consumer, String publisher,
                                          String config, String requestId, String state, String consumerPackage) {

        MediatorTuple mediatorTuple = new MediatorTuple(consumer, consumerPackage, publisher,
                MediatorHelper.getPublisherPackage(contentResolver, publisher), config, state, requestId, "");

        contentResolver.insert(MediatorTable.CONTENT_URI, mediatorTuple.getAsContentValues());
    }

}
