/*
 * @(#)LocalPublisherTable.java
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

import com.motorola.contextual.smartrules.psf.PsfProvider;

import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
* Defines columns for LocalPublisherTable
* CLASS:
*     LocalPublisherTable extends TableBase
*
* RESPONSIBILITIES:
*  Implements abstract functions from TableBase
*  Contains column names of LocalPublisherTable
*
* USAGE:
*     See each method.
*
*/
public class LocalPublisherTable extends TableBase {

    public static final String TABLE_NAME           = "local_publisher";
    public static final Uri    CONTENT_URI             = Uri.parse("content://" + PsfProvider.AUTHORITY + "/" + TABLE_NAME);

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
         * package in which the publisher key is contained
         */
        public static final String PACKAGE                      = "package";
        /**
         * description of the publisher - Example: GPS, Battery Level
         */
        public static final String DESCRIPTION                  = "desc";
        /**
         * activity intent that handles action = GET_CONFIG
         */
        public static final String ACTIVITY_INTENT              = "activity_intent";
        /**
         * tells whether the publisher has an UI for creating a new config
         * example: Timeframes, Location, Missed call etc...
         */
        public static final String NEW_CONFIG_SUPPORT           = "new_config";
        /**
         * Label to be shown by the UI to allow user to create new state
         * Example: New timeframe, Add a location etc..
         */
        public static final String NEW_CONFIG_LABEL             = "new_config_label";
        /**
         * Whether the publisher is interested in sharing its data outside the device
         */
        public static final String SHARE                        = "share";
        /**
         * Whether the publisher is an action or condition
         */
        public static final String TYPE                         = "type";
        /**
         * Market link if any from where the publisher can be downloaded
         */
        public static final String MARKET_LINK                  = "market";
        /**
         * whether the publisher is blacklisted or not
         */
        public static final String BLACKLIST                    = "blacklist";
        /**
         * whether the publisher is a stateful or a stateless publisher
         * note that all condition publishers are stateful
         */
        public static final String STATE_TYPE                   = "statetype";
        /**
         * battery drain value
         */
        public static final String BATTERY_DRAIN                = "battery";
        /**
         * data usage value
         */
        public static final String DATA_USAGE                   = "data";
        /**
         * response latency value
         */
        public static final String RESPONSE_LATENCY             = "response";
        /**
         * Settings to launch.  Applicable for Action publishers only
         */
        public static final String SETTINGS_ACTION              = "settings_action";
        /**
         * icon of the activity pointed by ACTIVITY_INTENT
         */
        public static final String ICON                         = "icon";
        /**
         * Interface version supported by the publisher
         */
        public static final String INTERFACE_VERSION            = "interface_version";
        /**
         * Publisher version supported by the publisher
         */
        public static final String PUBLISHER_VERSION            = "publisher_version";
    }

    public static final String CREATE_TABLE_SQL = CREATE_TABLE
            + TABLE_NAME
            + LP
            + Columns._ID                       + PKEY_TYPE      + CONT
            + Columns.PUBLISHER_KEY             + TEXT_TYPE      + UNIQUE + CONT
            + Columns.PACKAGE                   + TEXT_TYPE      + CONT
            + Columns.DESCRIPTION               + TEXT_TYPE      + CONT
            + Columns.ACTIVITY_INTENT           + TEXT_TYPE      + CONT
            + Columns.NEW_CONFIG_SUPPORT        + TEXT_TYPE      + CONT
            + Columns.NEW_CONFIG_LABEL          + TEXT_TYPE      + CONT
            + Columns.SHARE                     + INTEGER_TYPE   + CONT
            + Columns.TYPE                      + TEXT_TYPE      + CONT
            + Columns.MARKET_LINK               + TEXT_TYPE      + CONT
            + Columns.BLACKLIST                 + INTEGER_TYPE   + CONT
            + Columns.STATE_TYPE                + INTEGER_TYPE   + CONT
            + Columns.BATTERY_DRAIN             + TEXT_TYPE      + CONT
            + Columns.DATA_USAGE                + TEXT_TYPE      + CONT
            + Columns.RESPONSE_LATENCY          + TEXT_TYPE      + CONT
            + Columns.SETTINGS_ACTION           + TEXT_TYPE      + CONT
            + Columns.INTERFACE_VERSION         + REAL_TYPE      + CONT
            + Columns.PUBLISHER_VERSION         + REAL_TYPE      + CONT
            + Columns.ICON                      + BLOB_TYPE
            + RP;

    /**
     * Whether a publisher is an action or condition or rule publisher
     */
    public interface Type {
        String ACTION       = "action";
        String CONDITION    = "condition";
        String RULE         = "rule";
    }

    /**
     * Whether the publisher is stateful or stateless
     */
    public interface Modality {
        int STATEFUL        = 1;
        int STATELESS       = 0;
        int UNKNOWN         = -1;
    }

    /**
     * Whether the publisher has been blacklisted or not
     */
    public interface BlackList {
        int TRUE = 1;
        int FALSE = 0;
    }

    /**
     * Whether the publisher is interested in sharing its state
     * outside the device
     */
    public interface Share {
        int TRUE = 1;
        int FALSE = 0;
    }

    /**
     * Whether the publisher supports adding a new state for user via UI
     */
    public interface NewState {
        int TRUE = 1;
        int FALSE = 0;
        public interface Definition {
            String TRUE = "true";
            String FALSE = "false";
        }
    }


    /**
     * Whether the publisher is stateful or stateless
     */
    public interface PkgMgr {
        public interface Type {
            String STATEFUL = "stateful";
            String STATELESS = "stateless";
        }
    }

    /**
     * Constructor
     * @param context - Context to work with
     * @param tag     - tag for logging purposes
     */
    public LocalPublisherTable(Context context, String tag) {
        super(context, tag);
        if (LOG_DEBUG) Log.d(tag, "Creating LocalPublisher object");
    }

    /**
     * required by base class
     */
    public String getTableName() {
        return LocalPublisherTable.TABLE_NAME;
    }

    /**
     * required by base class
     */
    public Uri getContentUri() {
        return LocalPublisherTable.CONTENT_URI;
    }
}
