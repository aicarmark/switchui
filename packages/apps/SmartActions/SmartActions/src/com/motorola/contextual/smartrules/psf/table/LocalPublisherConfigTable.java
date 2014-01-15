/*
 * @(#)LocalPublisherConfigTable.java
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
package com.motorola.contextual.smartrules.psf.table;

import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.motorola.contextual.smartrules.psf.PsfProvider;

/**
* Defines columns for LocalPublisherConfigTable
* CLASS:
*     LocalPublisherConfigTable extends TableBase
*
* RESPONSIBILITIES:
*  Implements abstract functions from TableBase
*  Contains column names of LocalPublisherConfigTable
*
* USAGE:
*     See each method.
*
*/
public class LocalPublisherConfigTable extends TableBase {

    public static final String TABLE_NAME           = "local_publisher_config";
    public static final Uri    CONTENT_URI          = Uri.parse("content://" + PsfProvider.AUTHORITY + "/" + TABLE_NAME);
    private static final String SINGLE_SPACE        = " ";

    /**
     * Columns of LocalPublisherConfigTable
     */
    public interface Columns {
        /**
         * _id
         */
        public static final String _ID                          = BaseColumns._ID;
        /**
         * publisher key
         */
        public static final String PUBLISHER_KEY                = "pub_key";
        /**
         * one of the configs of the publisher key
         */
        public static final String CONFIG                       = "config";
        /**
         * state associated with the config
         */
        public static final String STATE                        = "state";
        /**
         * description associated with the config
         */
        public static final String DESCRIPTION                  = "description";
    }

    public static final String CREATE_TABLE_SQL = CREATE_TABLE
            + TABLE_NAME
            + LP
            + Columns._ID               + PKEY_TYPE      + CONT
            + Columns.PUBLISHER_KEY     + TEXT_TYPE      + CONT
            + Columns.CONFIG            + TEXT_TYPE      + CONT
            + Columns.STATE             + TEXT_TYPE      + CONT
            + Columns.DESCRIPTION       + TEXT_TYPE      + CONT
            + FOREIGN_KEY + SINGLE_SPACE + LP + Columns.PUBLISHER_KEY
            + RP + SINGLE_SPACE + REFERENCES + LocalPublisherTable.TABLE_NAME + SINGLE_SPACE + LP + LocalPublisherTable.Columns.PUBLISHER_KEY + RP
            + RP;

    /**
     * Constructor
     * @param context - Context to work with
     * @param tag     - tag for logging purposes
     */
    public LocalPublisherConfigTable(Context context, String tag) {
        super(context, tag);
        if (LOG_DEBUG) Log.d(tag, "Creating LocalPublisher object");
    }

    /**
     * required by base class
     */
    public String getTableName() {
        return LocalPublisherConfigTable.TABLE_NAME;
    }

    /**
     * required by base class
     */
    public Uri getContentUri() {
        return LocalPublisherConfigTable.CONTENT_URI;
    }

}
