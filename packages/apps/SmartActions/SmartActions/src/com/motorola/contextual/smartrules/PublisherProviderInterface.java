package com.motorola.contextual.smartrules;

import android.net.Uri;
import android.provider.BaseColumns;

public interface PublisherProviderInterface {
    Uri    CONTENT_URI = Uri.parse("content://com.motorola.contextual.smartrules.psf/local_publisher");
    interface Columns {
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
    }

    /**
     * Whether a publisher is an action or condition or rule publisher
     */
    interface Type {
        String ACTION       = "action";
        String CONDITION    = "condition";
        String RULE    		= "rule";
    }

    /**
     * Whether the publisher is stateful or stateless
     */
    interface Modality {
        int STATEFUL        = 1;
        int STATELESS       = 0;
        int UNKNOWN         = -1;
    }

    /**
     * Whether the publisher has been blacklisted or not
     */
    interface BlackList {
        int TRUE = 1;
        int FALSE = 0;
    }

    /**
     * Whether the publisher is interested in sharing its state
     * outside the device
     */
    interface Share {
        int TRUE = 1;
        int FALSE = 0;
    }

    /**
     * Whether the publisher supports adding a new state for user via UI
     */
    interface NewState {
        int TRUE = 1;
        int FALSE = 0;
        interface Definition {
            String TRUE = "true";
            String FALSE = "false";
        }
    }


    /**
     * Whether the publisher is stateful or stateless
     */
    interface PkgMgr {
        interface Type {
            String STATEFUL = "stateful";
            String STATELESS = "stateless";
        }
    }
}