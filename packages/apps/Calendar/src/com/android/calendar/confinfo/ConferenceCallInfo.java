package com.android.calendar.confinfo;

import android.provider.CalendarContract;
import android.net.Uri;

public interface ConferenceCallInfo {
    public static final int DATA_SOURCE_FLAG_INVALID = -1;
    public static final int DATA_SOURCE_FLAG_CONTACT_DATA = 0;
    public static final int DATA_SOURCE_FLAG_MY_PREFERENCE = 1;
    public static final int DATA_SOURCE_FLAG_MEETING_LOCATION = 2;
    public static final int DATA_SOURCE_FLAG_MANUAL_INPUT = 3;
    // DATA_SOURCE_FLAG_AUTO_DETECT has similar function as DATA_SOURCE_FLAG_MEETING_LOCATION
    // except that when the device locale is not US and China (more other locales might be
    // supported later) it will not return the conference info parsed from the meeting location.
    public static final int DATA_SOURCE_FLAG_AUTO_DETECT = 4;
    public static final String CONFERENCE_CALL_INFO_PATH = "conference_call_info";
    public static final String CONFERENCE_CALL_INFO_SETTINGS_PATH = "conference_call_info_settings";

    public static final Uri CONTENT_URI =  Uri.parse("content://" + CalendarContract.AUTHORITY + "/" + CONFERENCE_CALL_INFO_SETTINGS_PATH);

    public static final String TABLE_NAME = "ConferenceCallInfo";

    public static final String MY_CONFERENCE_NUM = "MyConferenceNum";
    public static final String MY_CONFERENCE_ID = "MyConferenceId";

    // data_source_flag == DATA_SOURCE_FLAG_CONTACT_DATA if the conference call info refers to contact db,
    // contact_data_id and contact_phone_type are used for this case.
    //
    // data_source_flag == DATA_SOURCE_FLAG_MANUAL_INPUT if the conference call info is manaully input by the user
    // manual_conference_number and manual_conference_id are used for this case.
    //
    //
    // associated_event_id == -1 for all events
    // associated_event_id > 0 for a specific event
    public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS ConferenceCallInfo (" +
            "_id INTEGER PRIMARY KEY," +
            "organizer_email TEXT NOT NULL," +
            "data_source_flag INTEGER NOT NULL DEFAULT -1," +
            "contact_data_id INTEGER DEFAULT -1," +
            "contact_phone_type INTEGER DEFAULT -1," +
            "manual_conference_number TEXT DEFAULT NULL," +
            "manual_conference_id TEXT DEFAULT NULL," +
            "associated_event_id INTEGER NOT NULL DEFAULT -1" +
            ");";

    public static final String CREATE_INDEX = "CREATE INDEX IF NOT EXISTS confInfoOrganzierEventIdIndex ON ConferenceCallInfo (" +
        "organizer_email," +
        "associated_event_id" +
        ");";

    public static final String DROP_TABLE = "DROP TABLE IF EXISTS ConferenceCallInfo;";

    public interface ColumnNames {
        public static final String _ID = "_id";
        public static final String ORGANIZER_EMAIL = "organizer_email";
        public static final String DATA_SOURCE_FLAG =  "data_source_flag";
        public static final String CONTACT_DATA_ID = "contact_data_id";
        public static final String CONTACT_PHONE_TYPE = "contact_phone_type";
        public static final String MANUAL_CONFERENCE_NUMBER = "manual_conference_number";
        public static final String MANUAL_CONFERENCE_ID = "manual_conference_id";
        public static final String ASSOCIATED_EVENT_ID = "associated_event_id";
    }

    public interface ColumnIndex {
        public static final int  _ID = 0;
        public static final int ORGANIZER_EMAIL = 1;
        public static final int DATA_SOURCE_FLAG =  2;
        public static final int CONTACT_DATA_ID = 3;
        public static final int CONTACT_PHONE_TYPE = 4;
        public static final int MANUAL_CONFERENCE_NUMBER = 5;
        public static final int MANUAL_CONFERENCE_ID = 6;
        public static final int ASSOCIATED_EVENT_ID = 7;
    }

    // extend to Calendars table
    public interface Calendars {
        public static final String CONFERENCE_CALL_ENABLED = "conference_call_enabled";
    }
}
