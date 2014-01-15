/*
 * @(#)SubscribeResponse.java
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
 * This class handles "subscribe response" from condition publishers.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Implements IMediatorProtocol 
 *
 * RESPONSIBILITIES:
 * Send the "subscribe response" intent to desired consumer
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

public class SubscribeResponse implements IMediatorProtocol, MediatorConstants, DbSyntax {

    private static final String TAG = MEDIATOR_PREFIX + SubscribeResponse.class.getSimpleName();

    private String mConfig;
    private String mPublisher;
    private String mResponseId;
    private String mStatus;
    private String mState;

    public SubscribeResponse(Intent intent) {
        mConfig = intent.getStringExtra(EXTRA_CONFIG);
        mPublisher = intent.getStringExtra(EXTRA_PUBLISHER_KEY);
        mResponseId = intent.getStringExtra(EXTRA_RESPONSE_ID);
        mStatus = intent.getStringExtra(EXTRA_STATUS);
        mState = intent.getStringExtra(EXTRA_STATE);
    }

    public boolean processRequest(Context context, Intent intent) {
        return (mConfig != null && mPublisher != null &&
                mResponseId != null && mStatus != null && MediatorHelper.isMediatorInitialized(context)) ? true : false;
    }

    public List<Intent> execute(Context context, Intent intent) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();
        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = null;
        String where = MediatorTable.Columns.CONFIG + EQUALS + Q + mConfig + Q + AND +
                       MediatorTable.Columns.PUBLISHER + EQUALS + Q + mPublisher + Q;

        try {
            cursor = contentResolver.query(MediatorTable.CONTENT_URI, null, where, null, null);

            if (cursor != null && cursor.getCount() > 0) {
                intentsToBroadcast = handleSubscribeResponse(contentResolver, cursor);
            } else {
                Log.w(TAG, "Matching publisher and config not found");
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
     * Method to handle subscribe response
     * @param contentResolver
     * @param cursor
     * @return
     */
    private List<Intent> handleSubscribeResponse(ContentResolver contentResolver, Cursor cursor) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();

        if (cursor.moveToFirst()) {
            do {
                MediatorTuple mediatorTuple = new MediatorTuple(cursor);
                ArrayList<String> requestIds = mediatorTuple.getSubscribeRequestIdsAsList();

                if (requestIds.size() > 0) {
                    if (mStatus.equals(FAILURE)) {
                        intentsToBroadcast.addAll(handleSubscribeFailure(contentResolver, requestIds, mediatorTuple));
                    } else {
                        intentsToBroadcast.addAll(handleSubscribeSuccess(contentResolver, requestIds, mediatorTuple));
                    }
                }
            } while (cursor.moveToNext());
        }

        return intentsToBroadcast;
    }

    /**
     * Method to handle subscribe failure
     *
     * @param contentResolver
     * @param requestIds List of request IDs for this subscribe request
     * @param mediatorTuple Mediator tuple for which subscribe response is received
     * @return List of intents to broadcast
     */
    private List<Intent> handleSubscribeFailure (ContentResolver contentResolver, ArrayList<String> requestIds,
            MediatorTuple mediatorTuple) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();
        String consumer = mediatorTuple.getConsumer();

        if (requestIds.size() > 1) {
            //Other subscribe requests are pending. Only remove this entry.
            mediatorTuple.removeSubscribeRequestId(mResponseId);
            MediatorHelper.updateTuple(contentResolver, mediatorTuple);
        } else {
            //Subscribe failed. Remove entry from the database
            MediatorHelper.deleteFromMediatorDatabase(contentResolver, consumer, mPublisher, mConfig);
        }
        intentsToBroadcast.add(MediatorHelper.createCommandResponseIntent(consumer, mConfig,
                               mResponseId, SUBSCRIBE_RESPONSE_EVENT, mPublisher, mStatus, mState, null));
        return intentsToBroadcast;
    }

    /**
     * Method to handle subscribe success
     *
     * @param contentResolver
     * @param requestIds List of request IDs for this subscribe request
     * @param mediatorTuple Mediator tuple for which subscribe response is received
     * @return List of intents to broadcast
     */
    private List<Intent> handleSubscribeSuccess (ContentResolver contentResolver, ArrayList<String> requestIds,
            MediatorTuple mediatorTuple) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();
        String consumer = mediatorTuple.getConsumer();

        //Update state and request IDs
        mediatorTuple.setState(mState);
        mediatorTuple.setSubscribeRequestIds("");
        MediatorHelper.updateTuple(contentResolver, mediatorTuple);

        for (String requestId : requestIds) {
            //Send subscribe response for all request IDs
            intentsToBroadcast.add(MediatorHelper.createCommandResponseIntent(consumer, mConfig,
                                   requestId, SUBSCRIBE_RESPONSE_EVENT, mPublisher, mStatus, mState, null));
        }
        return intentsToBroadcast;
    }
}
