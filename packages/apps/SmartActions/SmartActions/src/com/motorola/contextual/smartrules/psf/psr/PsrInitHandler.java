/*
 * @(#)PsrInitHandler.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA MOBILITY INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21034        2012/03/21                   Initial version
 *
 */
package com.motorola.contextual.smartrules.psf.psr;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.motorola.contextual.smartrules.psf.IntentHandler;
import com.motorola.contextual.smartrules.psf.table.LocalPublisherTable;

/**
* Initializes Publisher States Receiver (PSR)
* CLASS:
*     PsrInitHandler
*
* RESPONSIBILITIES:
*  Initializes PSR
*  Sends command=list to all known publishers in PP
*  Sends out SA_CORE_INIT_COMPLETE that signals sa core initialization complete
*
* USAGE:
*     See each method.
*
*/
public class PsrInitHandler extends IntentHandler {

    /**
     * Constructor
     * @param context - context to work with
     * @param intent  - incoming intent
     */
    public PsrInitHandler(Context context, Intent intent) {
        super(context, intent);
    }

    @Override
    public boolean handleIntent() {

        Intent inComingintent = getIntent();
        if (LOG_DEBUG) Log.d(TAG, "Handling intent in PsrInitHandler " + inComingintent.toString());
        if(ENABLE_LIST_COMMAND) requestListOfStates();

        String launchCmd = inComingintent.getStringExtra(EXTRA_PSR_LAUNCH_COMMAND);
        if(launchCmd != null && (launchCmd.equals(ACTION_PSF_INIT) ||
                                 launchCmd.equals(Intent.ACTION_MY_PACKAGE_REPLACED))) {
            Intent intent = new Intent(ACTION_MEDIATOR_INIT);
            mContext.sendBroadcast(intent);
        }
        return true;
    }

    /**
     * Loops through all non-blacklisted publishers and sends out
     * command = list to all of them
     */
    private void requestListOfStates() {
        ContentResolver cResolver = mContext.getContentResolver();
        Cursor cursor = null;

        String whereClause = LocalPublisherTable.Columns.BLACKLIST
                             + EQUALS
                             + LocalPublisherTable.BlackList.FALSE;

        String columns[] = new String[] {LocalPublisherTable.Columns.PUBLISHER_KEY,
                                         LocalPublisherTable.Columns.BLACKLIST,
                                         LocalPublisherTable.Columns.TYPE
                                        };


        try {
            cursor = cResolver.query(LocalPublisherTable.CONTENT_URI,
                                     columns,
                                     whereClause, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++ ) {

                    String publisherKey = cursor.getString(cursor.getColumnIndex(LocalPublisherTable.Columns.PUBLISHER_KEY));
                    String type = cursor.getString(cursor.getColumnIndex(LocalPublisherTable.Columns.TYPE));
                    requestStateForPublisher(publisherKey, type);
                    cursor.moveToNext();

                }
            } else {
                Log.e(TAG, "Cursor null or empty in requestListOfStates ");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in requestListOfStates " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Given a publisher key and type (action or condition), sends
     * command = list to the publisher
     * @param publisherKey - publisher to whom the command has to be sent
     * @param type  - action or condition.  This parameter would be ignored when the
     * interface for AP and CP comes into sync
     */
    private void requestStateForPublisher(String publisherKey, String type) {
        //  This function has to be written when we turn on list requests from PSR

        Log.e(TAG, "Not sending list command");

    }

}
