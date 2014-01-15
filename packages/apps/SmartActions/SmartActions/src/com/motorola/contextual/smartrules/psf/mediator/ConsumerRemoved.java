/*
 * @(#)ConsumerRemoved.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/06/21  NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.psf.mediator;

import java.util.ArrayList;
import java.util.List;

import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.psf.mediator.protocol.CancelRequest;
import com.motorola.contextual.smartrules.psf.mediator.protocol.IMediatorProtocol;
import com.motorola.contextual.smartrules.psf.table.MediatorTable;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

/**
 * This class handles the scenarios when consumer is removed or its data is removed
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Implements IMediatorProtocol 
 *
 * RESPONSIBILITIES:
 * Check if the data cleared/uninstalled package is one of the consumers
 * Take appropriate action
 *
 * COLABORATORS:
 *     Consumer - Uses the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public class ConsumerRemoved implements IMediatorProtocol, MediatorConstants, DbSyntax {

    private static final String TAG = MEDIATOR_PREFIX + ConsumerRemoved.class.getSimpleName();

    private String mPackageName;

    public ConsumerRemoved(Intent intent) {
        String data = intent.getDataString();
        if (data != null) {
            mPackageName = data.substring(PACKAGE_PREFIX.length());
        }
    }

    public boolean processRequest(Context context, Intent intent) {
        return (mPackageName != null && !mPackageName.equals(PSF_APP_PACKAGE) && MediatorHelper.isMediatorInitialized(context)) ? true : false;
    }

    public List<Intent> execute(Context context, Intent intent) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();
        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = null;
        String [] projection = {MediatorTable.Columns.CONSUMER,
                                MediatorTable.Columns.PUBLISHER, MediatorTable.Columns.CONFIG
                               };
        String where = MediatorTable.Columns.CONSUMER_PACKAGE + EQUALS + Q + mPackageName + Q;

        try {
            //Check if consumer data is cleared
            cursor = contentResolver.query(MediatorTable.CONTENT_URI, projection, where, null, null);

            if (cursor != null && cursor.getCount() > 0) {
                //Data cleared on consumer. Send cancel request to all publishers.
                intentsToBroadcast = handleConsumerRemoved(context, cursor);
            } else {
                if (LOG_DEBUG) Log.d(TAG, "Removed/data cleared package is not a consumer");
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
     * Method to handle the cases when consumer is either removed or its data is cleared
     * In this case cancel is sent to all the subscribed publishers
     * @param context
     * @param cursor
     * @return
     */
    private List<Intent> handleConsumerRemoved (Context context, Cursor cursor) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();

        if (cursor.moveToFirst()) {
            do {
                String publisher = cursor.getString(cursor.getColumnIndex(MediatorTable.Columns.PUBLISHER));
                String config = cursor.getString(cursor.getColumnIndex(MediatorTable.Columns.CONFIG));
                String consumer = cursor.getString(cursor.getColumnIndex(MediatorTable.Columns.CONSUMER));

                if (publisher != null && config != null && consumer != null) {
                    //Execute cancel command
                    Intent cancelIntent = MediatorHelper.createCommandRequestIntent(CANCEL_EVENT, config,
                                          publisher, consumer, null);
                    CancelRequest cancelRequest = new CancelRequest(cancelIntent);
                    if (cancelRequest.processRequest(context, cancelIntent)) {
                        intentsToBroadcast.addAll(cancelRequest.execute(context, cancelIntent));
                    }
                }

            } while (cursor.moveToNext());
        }

        return intentsToBroadcast;
    }

}
