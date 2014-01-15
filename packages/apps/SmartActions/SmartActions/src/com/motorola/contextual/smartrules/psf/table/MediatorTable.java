/*
 * @(#)MediatorTable.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA MOBILITY INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21034        2012/05/15                   Initial version
 *
 */
package com.motorola.contextual.smartrules.psf.table;

import com.motorola.contextual.smartrules.psf.mediator.MediatorProvider;

import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
* Defines columns for MediatorTable
* CLASS:
*     MediatorTable extends TableBase
*
* RESPONSIBILITIES:
*  Implements abstract functions from TableBase
*  Contains column names of MediatorTable
*
* USAGE:
*     See each method.
*
*/
public class MediatorTable extends TableBase {

    public static final String TABLE_NAME              = "mediator";
    public static final Uri    CONTENT_URI             = Uri.parse("content://" + MediatorProvider.AUTHORITY + "/" + TABLE_NAME);

    public interface Columns {
        /**
         * _id
         */
        public static final String _ID                          = BaseColumns._ID;
        /**
         * Consumer of a subscribe/cancel transaction
         */
        public static final String CONSUMER                = "consumer";
        /**
         * Package of the consumer who subscribed for the config
         */
        public static final String CONSUMER_PACKAGE                = "consumer_package";
        /**
         * Publisher of a subscribe/cancel transaction
         */
        public static final String PUBLISHER                      = "publisher";
        /**
         * Package of the publisher to whom the config has been subscribed
         */
        public static final String PUBLISHER_PACKAGE                = "publisher_package";
        /**
         * Config of the transaction
         */
        public static final String CONFIG              = "config";
        /**
         * State of the config
         */
        public static final String STATE                  = "state";
        /**
         * subscribe - request id lists
         */
        public static final String SUBSCRIBE_ID           = "sub_req_id";
        /**
         * cancel - request id lists
         */
        public static final String CANCEL_ID             = "cancel_req_id";
    }

    public static final String CREATE_TABLE_SQL = CREATE_TABLE
            + TABLE_NAME
            + LP
            + Columns._ID                       + PKEY_TYPE      + CONT
            + Columns.CONSUMER             		+ TEXT_TYPE      + CONT
            + Columns.CONSUMER_PACKAGE     		+ TEXT_TYPE      + CONT
            + Columns.PUBLISHER                 + TEXT_TYPE      + CONT
            + Columns.PUBLISHER_PACKAGE         + TEXT_TYPE      + CONT
            + Columns.STATE               		+ TEXT_TYPE      + CONT
            + Columns.CONFIG           			+ TEXT_TYPE      + CONT
            + Columns.SUBSCRIBE_ID        		+ TEXT_TYPE      + CONT
            + Columns.CANCEL_ID          		+ TEXT_TYPE
            + RP;

    /**
     * Constructor
     * @param context - Context to work with
     * @param tag     - tag for logging purposes
     */
    public MediatorTable(Context context, String tag) {
        super(context, tag);
        if (LOG_DEBUG) Log.d(tag, "Creating MediatorTable object");
    }

    /**
     * required by base class
     */
    public String getTableName() {
        return MediatorTable.TABLE_NAME;
    }

    /**
     * required by base class
     */
    public Uri getContentUri() {
        return MediatorTable.CONTENT_URI;
    }
}
