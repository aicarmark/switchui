/*
 * @(#)CancelResponse.java
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
 * This class handles "cancel response" from condition publishers.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Implements IMediatorProtocol 
 *
 * RESPONSIBILITIES:
 * Send the "cancel response" intent to desired consumer
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

public class CancelResponse implements IMediatorProtocol, MediatorConstants, DbSyntax {

    private static final String TAG = MEDIATOR_PREFIX + CancelResponse.class.getSimpleName();

    private String mConfig;
    private String mPublisher;
    private String mResponseId;
    private String mStatus;

    public CancelResponse(Intent intent) {
        mConfig = intent.getStringExtra(EXTRA_CONFIG);
        mPublisher = intent.getStringExtra(EXTRA_PUBLISHER_KEY);
        mResponseId = intent.getStringExtra(EXTRA_RESPONSE_ID);
        mStatus = intent.getStringExtra(EXTRA_STATUS);
    }


    public boolean processRequest(Context context, Intent intent) {
        return (mConfig != null && mPublisher != null && mResponseId != null &&
                mStatus != null && MediatorHelper.isMediatorInitialized(context)) ? true : false;
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
                intentsToBroadcast = handleCancelResponse(contentResolver, cursor);
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
     * Method to handle cancel response
     * @param contentResolver
     * @param cursor
     * @return
     */
    private List<Intent> handleCancelResponse(ContentResolver contentResolver, Cursor cursor) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();
        cursor.moveToFirst();

        do {
            MediatorTuple mediatorTuple = new MediatorTuple(cursor);
            String consumer = mediatorTuple.getConsumer();

            if (consumer != null && mediatorTuple.getCancelRequestIdsAsList().contains(mResponseId)) {
                if (mStatus.equals(FAILURE)) {
                    //Cancel failed. Remove cancel ID
                    mediatorTuple.removeCancelRequestId(mResponseId);
                    MediatorHelper.updateTuple(contentResolver, mediatorTuple);
                } else {
                    MediatorHelper.deleteFromMediatorDatabase(contentResolver, consumer, mPublisher, mConfig);
                }

                if (!mResponseId.contains(MEDIATOR_SEPARATOR)) {
                    intentsToBroadcast.add(MediatorHelper.createCommandResponseIntent(consumer, mConfig,
                                           mResponseId, CANCEL_RESPONSE_EVENT, mPublisher, mStatus, null, null));
                } else {
                    //Mediator itself requested cancel. No need to send cancel response.
                    if (Constants.LOG_DEBUG) Log.d(TAG, "Mediator requested cancel request completed");
                }
                break;
            }
        } while (cursor.moveToNext());

        return intentsToBroadcast;
    }

}
