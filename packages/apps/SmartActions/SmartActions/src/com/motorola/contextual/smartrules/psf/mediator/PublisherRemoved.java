/*
 * @(#)PublisherRemoved.java
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
import com.motorola.contextual.smartrules.psf.mediator.protocol.IMediatorProtocol;
import com.motorola.contextual.smartrules.psf.table.MediatorTable;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class handles the scenarios when publisher is removed or its data is removed
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Implements IMediatorProtocl 
 *
 * RESPONSIBILITIES:
 * Check if the data cleared package is one of the publishers
 * Take appropriate action when data clear on publisher happens
 *
 * COLABORATORS:
 *     ConditionPublisher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public class PublisherRemoved implements IMediatorProtocol, MediatorConstants, DbSyntax {

    private static final String TAG = MEDIATOR_PREFIX + PublisherRemoved.class.getSimpleName();

    public boolean processRequest(Context context, Intent intent) {
        return MediatorHelper.isMediatorInitialized(context);
    }

    public List<Intent> execute(Context context, Intent intent) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();
        ContentResolver contentResolver = context.getContentResolver();
        String action = intent.getAction();

        if (action != null) {
            List<String> removedPublishers = new ArrayList<String>();
            if (action.equals(RESET)) {
                removedPublishers = intent.getStringArrayListExtra(EXTRA_PUBLISHER_KEY_LIST);
            } else if (action.equals(UPDATED)) {
                removedPublishers = intent.getStringArrayListExtra(EXTRA_PUBLISHER_REMOVED_LIST);
            }

            if (removedPublishers != null) {

                for (String publisher : removedPublishers) {
                    String where = MediatorTable.Columns.PUBLISHER + EQUALS + Q + publisher + Q;
                    try {
                        //Cleanup mediator database. Remove all the entries for this publisher.
                        contentResolver.delete(MediatorTable.CONTENT_URI, where, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } else {
                Log.e(TAG, "Intent doesn't have publisher list");
            }
        }

        return intentsToBroadcast;
    }

}
