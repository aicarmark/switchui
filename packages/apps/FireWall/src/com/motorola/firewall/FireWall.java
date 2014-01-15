package com.motorola.firewall;

import android.net.Uri;
import android.provider.BaseColumns;

public final class FireWall {

    public static final String AUTHORITY = "com.motorola.firewall.provider";

    // This class cannot be instantiated
    private FireWall() {
    }

    public static final class Name implements BaseColumns {
        // This class cannot be instantiated
        private Name() {
        }

        public static final int MAXNAMEITEMS = 1000;

        public static final int MAXBLACKPATTERNS = 3;

        public static final String WHITELIST = "whitelist";

        public static final String WHITEPATTERN = "whitepattern";

        public static final String BLACKLIST = "blacklist";

        public static final String BLACKPATTERN = "blackpattern";

        public static final String WHITENAME = "whitename";

        public static final String BLACKNAME = "blackname";
        
        public static final String WHITE = "white";
        
        public static final String BLACK = "black";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/namelist");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.motorola.firewall.name";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.motorola.firewall.name";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "_ID DESC";
        
        /**
         * The sort order for number and pattern combined 
         */
        public static final String COMBINED_SORT_ORDER = "block_type DESC, _id DESC";

        /**
         * The type of call Type: TEXT
         */
        public static final String BLOCK_TYPE = "block_type";

        /**
         * The phone number key network portion for phone number Type: TEXT
         */
        public static final String PHONE_NUMBER_KEY = "phone_number_key";

        /**
         * The contact name Type: TEXT
         */
        public static final String CACHED_NAME = "cached_name";
        
        /**
         * The cached number type (Home, Work, etc) associated with the contact
         * Type: INTEGER
         */
        public static final String CACHED_NUMBER_TYPE = "cached_name_type";
        
        /**
         * The cached number label, for a custom number type,
         * associated with the contact Type: TEXT
         */
        public static final String CACHED_NUMBER_LABEL = "cached_number_label";
        
        /**
         * This number apply for call firewall Type: BOOLEAN
         */
        public static final String FOR_CALL = "for_call";
        
        /**
         * This number apply for SMS firewall Type: BOOLEAN
         */
        public static final String FOR_SMS = "for_sms";

    }
    
    public static final class BlockLog implements BaseColumns {
        // This class cannot be instantiated
        private BlockLog() {
        }

        public static final int CALL_BLOCK = 1;
        
        public static final int SMS_BLOCK = 2;
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/loglist");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.motorola.firewall.log";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.motorola.firewall.log";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "blocked DESC";


        /**
         * The phone number Type: TEXT
         */
        public static final String PHONE_NUMBER = "phone_number";

        /**
         * The contact name Type: TEXT
         */
        public static final String CACHED_NAME = "cached_name";
        
        /**
         * The cached number type (Home, Work, etc) associated with the contact
         * Type: INTEGER
         */
        public static final String CACHED_NUMBER_TYPE = "cached_name_type";
        
        /**
         * The cached number label, for a custom number type,
         * associated with the contact Type: TEXT
         */
        public static final String CACHED_NUMBER_LABEL = "cached_number_label";

        /**
         * The timestamp for when the number was last blocked Type: INTEGER
         * (long from System.curentTimeMillis())
         */
        public static final String BLOCKED_DATE = "blocked";

        /**
         * The network type of the SMS .
         * <P>Type: INTEGER (int)</P>
         */
        public static final String NETWORK = "network";
        
        /**
         * The log type of the Firewall .
         * <P>Type: INTEGER (int)</P>
         */
        public static final String LOG_TYPE = "log_type";
        
        /**
         * The type of call Type: TEXT
         */
        public static final String LOG_DATA = "log_data";

    }

    public static final class Settings implements BaseColumns {
        // This class cannot be instantiated
        private Settings() {
        }

        public static final String INBLOCK_CALL_ON = "inblock_call_on";
        
        public static final String INBLOCK_SMS_ON = "inblock_sms_on";
        
        public static final String IN_BLOCK_CALL_LIST = "in_block_call_list";
        
        public static final String IN_BLOCK_SMS_LIST = "in_block_sms_list";

        public static final String CALL_BLOCK_TYPE = "reject_type";
        
        public static final String VINBLOCK_CALL_ON = "vinblock_call_on";
        
        public static final String VINBLOCK_SMS_ON = "vinblock_sms_on";
        
        public static final String VIN_BLOCK_CALL_LIST = "vin_block_call_list";
        
        public static final String VIN_BLOCK_SMS_LIST = "vin_block_sms_list";
        
        public static final String IN_CALL_REJECT_CDMA = "in_call_reject_cdma";
        
        public static final String IN_SMS_REJECT_CDMA = "in_sms_reject_cdma";
        
        public static final String IN_CALL_REJECT_GSM = "in_call_reject_gsm";
        
        public static final String IN_SMS_REJECT_GSM = "in_sms_reject_gsm";
        
        public static final String VIN_CALL_REJECT_CDMA = "vin_call_reject_cdma";
        
        public static final String VIN_SMS_REJECT_CDMA = "vin_sms_reject_cdma";
        
        public static final String VIN_CALL_REJECT_GSM = "vin_call_reject_gsm";
        
        public static final String VIN_SMS_REJECT_GSM = "vin_sms_reject_gsm";
        
        public static final String[] IN_BLOCK_CALL_LIST_VALUES = {"in_call_pimlist","in_call_rejectall",
            "in_call_whitelist","in_call_blacklist" };

        public static final String[] IN_BLOCK_SMS_LIST_VALUES = {"in_sms_pimlist","in_sms_rejectall",
            "in_sms_whitelist","in_sms_blacklist" };
        
        public static final String[] CALL_BLOCK_TYPE_VALUES = {"ignore_block", "reject_block"};
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/settinglist");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.motorola.firewall.settings";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.motorola.firewall.settings";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "name ASC";


        /**
         * The phone number Type: TEXT
         */
        public static final String NAME = "name";

        /**
         * The contact name Type: TEXT
         */
        public static final String VALUE = "value";

    }
}
