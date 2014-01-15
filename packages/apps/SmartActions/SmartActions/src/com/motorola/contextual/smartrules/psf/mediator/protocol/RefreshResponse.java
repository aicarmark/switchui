/*
 * @(#)RefreshResponse.java
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
import java.util.HashMap;
import java.util.List;

import com.motorola.contextual.commonutils.StringUtils;
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
 * This class handles "refresh response" from condition publishers.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Implements IMediatorProtocol 
 *
 * RESPONSIBILITIES:
 * Send the "refresh response" intent to desired consumer
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

public class RefreshResponse implements IMediatorProtocol, MediatorConstants, DbSyntax {

    private static final String TAG = MEDIATOR_PREFIX + RefreshResponse.class.getSimpleName();

    private String mConfig;
    private String mPublisher;
    private String mResponseId;
    private String mStatus;
    private String mState;
    private String mDescription;

    public RefreshResponse(Intent intent) {
        mConfig = intent.getStringExtra(EXTRA_CONFIG);
        mPublisher = intent.getStringExtra(EXTRA_PUBLISHER_KEY);
        mResponseId = intent.getStringExtra(EXTRA_RESPONSE_ID);
        mStatus = intent.getStringExtra(EXTRA_STATUS);
        mState = intent.getStringExtra(EXTRA_STATE);
        mDescription = intent.getStringExtra(EXTRA_DESCRIPTION);
    }


    public boolean processRequest(Context context, Intent intent) {
        return (mPublisher != null && mResponseId != null &&
                mResponseId.contains(MEDIATOR_SEPARATOR) && mStatus != null) ? true : false;
    }


    public List<Intent> execute(Context context, Intent intent) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();
        ContentResolver contentResolver = context.getContentResolver();

        int separatorIndex = mResponseId.indexOf(MEDIATOR_SEPARATOR);
        String consumer = mResponseId.substring(0, separatorIndex);
        mResponseId = mResponseId.substring(separatorIndex + MEDIATOR_SEPARATOR.length());
        if (!StringUtils.isEmpty(consumer) && !StringUtils.isEmpty(mResponseId)) {
            if (mConfig != null && mState != null && MediatorHelper.isMediatorInitialized(context)) {
                intentsToBroadcast.addAll(getNotifyIntentIfStateChanged(contentResolver));
            }

            intentsToBroadcast.add(MediatorHelper.createCommandResponseIntent(consumer, mConfig, mResponseId,
                                   REFRESH_RESPONSE_EVENT, mPublisher, mStatus, mState, mDescription));
        } else {
            Log.e(TAG, "Consumer or responseId empty. Aborting");
        }

        return intentsToBroadcast;
    }

    /**
     * Method to check if state has changed for a particular config
     * It sends notify to respective consumers in case state has changed
     * @param contentResolver
     * @return
     */
    private List<Intent> getNotifyIntentIfStateChanged(ContentResolver contentResolver) {
        Cursor cursor = null;
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();
        String where = MediatorTable.Columns.CONFIG + EQUALS + Q + mConfig + Q + AND +
                       MediatorTable.Columns.PUBLISHER + EQUALS + Q + mPublisher + Q + AND +
                       MediatorTable.Columns.STATE + NOT_EQUAL + Q + mState + Q;

        try {
            cursor = contentResolver.query(MediatorTable.CONTENT_URI, null, where, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    MediatorTuple mediatorTuple = new MediatorTuple(cursor);

                    //State has changed. Update the tuple and send notify.
                    mediatorTuple.setState(mState);
                    MediatorHelper.updateTuple(contentResolver, mediatorTuple);

                    String consumer = mediatorTuple.getConsumer();
                    HashMap<String, String> configStateMap = new HashMap<String, String>();
                    configStateMap.put(mConfig, mState);
                    intentsToBroadcast.add(MediatorHelper.createNotifyIntent(consumer, mPublisher, configStateMap));
                } while (cursor.moveToNext());
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

}
