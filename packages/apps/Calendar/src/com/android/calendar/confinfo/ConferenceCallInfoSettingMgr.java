package com.android.calendar.confinfo;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.provider.CalendarContract.Events;
import android.content.ContentValues;
import android.database.Cursor;

import com.android.calendar.confinfo.ConferenceCallInfo;
import com.android.calendar.confinfo.ConferenceCallInfoDataMgr;

public class ConferenceCallInfoSettingMgr {

    ConferenceCallInfoDataMgr mDataMgr = null;
    // map to the ConferenceCallInfo table
    private int mDataSourceFlag = ConferenceCallInfo.DATA_SOURCE_FLAG_INVALID;
    private int mContactDataId = -1;
    private int mContactPhoneType = -1;
    private String mManualConfNumber = null;
    private String mManualConfId = null;
    private long mAssociatedEventId = -1L;

    public ConferenceCallInfoSettingMgr(ConferenceCallInfoDataMgr dataMgr) {
        mDataMgr = dataMgr;
    }

    private void notifyConfCallInfoDataMgr() {
        if (mDataMgr != null) {
            mDataMgr.notifySettingChange(this);
        }
    }

    // only for database, it will not touch memory cache
    public static boolean hasConferenceCallSetting(Context ctx, String organizerEmail, long eventId) {
        if ((ctx == null) || (organizerEmail == null) || (eventId < 0))
            return false;

        boolean bFound = false;
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(ConferenceCallInfo.CONTENT_URI, null,
                            ConferenceCallInfo.ColumnNames.ORGANIZER_EMAIL + "='"+ organizerEmail + "' and " +
                            ConferenceCallInfo.ColumnNames.ASSOCIATED_EVENT_ID + "="+ eventId, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    bFound = true;
                }
            } finally {
                c.close();
            }
        }

        return bFound;
    }

    // only for database, it will not touch memory cache
    public static ContentValues getConferenceCallSetting(Context ctx, String organizerEmail, long eventId) {
        if ((ctx == null) || (organizerEmail == null) || (eventId < 0))
            return null;

        ContentValues content = null;
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(ConferenceCallInfo.CONTENT_URI, null,
                            ConferenceCallInfo.ColumnNames.ORGANIZER_EMAIL + "='"+ organizerEmail + "' and " +
                            ConferenceCallInfo.ColumnNames.ASSOCIATED_EVENT_ID + "="+ eventId, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    content = new ContentValues();

                    content.put(ConferenceCallInfo.ColumnNames.DATA_SOURCE_FLAG,
                                c.getInt(ConferenceCallInfo.ColumnIndex.DATA_SOURCE_FLAG));
                    content.put(ConferenceCallInfo.ColumnNames.CONTACT_DATA_ID,
                                c.getInt(ConferenceCallInfo.ColumnIndex.CONTACT_DATA_ID));
                    content.put(ConferenceCallInfo.ColumnNames.CONTACT_PHONE_TYPE,
                                c.getInt(ConferenceCallInfo.ColumnIndex.CONTACT_PHONE_TYPE));
                    content.put(ConferenceCallInfo.ColumnNames.MANUAL_CONFERENCE_NUMBER,
                                c.getString(ConferenceCallInfo.ColumnIndex.MANUAL_CONFERENCE_NUMBER));
                    content.put(ConferenceCallInfo.ColumnNames.MANUAL_CONFERENCE_ID,
                                c.getString(ConferenceCallInfo.ColumnIndex.MANUAL_CONFERENCE_ID));
                }
            } finally {
                c.close();
            }
        }

        return content;
    }

    public static boolean saveConfCallInfoWithBackRef(ArrayList<ContentProviderOperation> ops,
            int eventIdIndex, String organizerEmail, boolean isMyPrference) {

        // Delete all the existing conference call info for this event
        ContentProviderOperation.Builder b = ContentProviderOperation.newDelete(ConferenceCallInfo.CONTENT_URI);
        b.withSelection(ConferenceCallInfo.ColumnNames.ASSOCIATED_EVENT_ID + "=?", new String[1]);
        b.withSelectionBackReference(0, eventIdIndex);
        ops.add(b.build());

        ContentValues values = new ContentValues();

        values.clear();
        values.put(ConferenceCallInfo.ColumnNames.ORGANIZER_EMAIL, organizerEmail);
        if (isMyPrference) {
            values.put(ConferenceCallInfo.ColumnNames.DATA_SOURCE_FLAG,
                    ConferenceCallInfo.DATA_SOURCE_FLAG_MY_PREFERENCE);
        } else {
            values.put(ConferenceCallInfo.ColumnNames.DATA_SOURCE_FLAG,
                    ConferenceCallInfo.DATA_SOURCE_FLAG_AUTO_DETECT);
        }
        b = ContentProviderOperation.newInsert(ConferenceCallInfo.CONTENT_URI).withValues(values);
        b.withValueBackReference(ConferenceCallInfo.ColumnNames.ASSOCIATED_EVENT_ID, eventIdIndex);
        ops.add(b.build());

        return true;
    }

    public static boolean saveConfCallInfo(ArrayList<ContentProviderOperation> ops,
            long eventId, String organizerEmail) {
        // Delete all the existing conference call info for this event
        // Delete all the existing reminders for this event
        String where = ConferenceCallInfo.ColumnNames.ASSOCIATED_EVENT_ID + "=?";
        String[] args = new String[] { Long.toString(eventId) };
        ContentProviderOperation.Builder b = ContentProviderOperation.newDelete(ConferenceCallInfo.CONTENT_URI);
        b.withSelection(where, args);
        ops.add(b.build());

        ContentValues values = new ContentValues();

        values.clear();
        values.put(ConferenceCallInfo.ColumnNames.ORGANIZER_EMAIL, organizerEmail);
        values.put(ConferenceCallInfo.ColumnNames.ASSOCIATED_EVENT_ID, eventId);
        b = ContentProviderOperation.newInsert(ConferenceCallInfo.CONTENT_URI).withValues(values);
        ops.add(b.build());
        return true;
    }

    // only for database, it will not touch memory cache
    public static void createConferenceCallSetting(Context ctx, ContentValues content) {
        if ((ctx == null) || (content == null))
            return;

        String organizerEmail = content.getAsString(ConferenceCallInfo.ColumnNames.ORGANIZER_EMAIL);
        if ((organizerEmail == null) || (organizerEmail.length() == 0)){
            return;
        }

        if (!content.containsKey(ConferenceCallInfo.ColumnNames.ASSOCIATED_EVENT_ID))
            return;

        if (!content.containsKey(ConferenceCallInfo.ColumnNames.DATA_SOURCE_FLAG))
            return;

        ContentResolver cr = ctx.getContentResolver();
        cr.insert(ConferenceCallInfo.CONTENT_URI, content);
    }

    // cache will be refreshed
    public void loadConferenceCallSetting(Context ctx, String organizerEmail, long eventId, String eventLocation) {
        if ((organizerEmail == null) || (organizerEmail.length() == 0)){
            return;
        }

        if ((eventLocation != null) && (mDataMgr != null)) {
            mDataSourceFlag = ConferenceCallInfo.DATA_SOURCE_FLAG_MEETING_LOCATION;
            mDataMgr.decodeMeetingConfCallInfo(eventLocation);
        }

        // load settings from ConferenceCallInfo table
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(ConferenceCallInfo.CONTENT_URI, null,
                            ConferenceCallInfo.ColumnNames.ORGANIZER_EMAIL + "='"+ organizerEmail + "'", null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    do {
                        mAssociatedEventId = c.getLong(ConferenceCallInfo.ColumnIndex.ASSOCIATED_EVENT_ID);

                        if ((mAssociatedEventId == eventId) || (mAssociatedEventId == -1L)) {
                            mDataSourceFlag = c.getInt(ConferenceCallInfo.ColumnIndex.DATA_SOURCE_FLAG);
                            mContactDataId = c.getInt(ConferenceCallInfo.ColumnIndex.CONTACT_DATA_ID);
                            mContactPhoneType = c.getInt(ConferenceCallInfo.ColumnIndex.CONTACT_PHONE_TYPE);
                            mManualConfNumber = c.getString(ConferenceCallInfo.ColumnIndex.MANUAL_CONFERENCE_NUMBER);
                            mManualConfId = c.getString(ConferenceCallInfo.ColumnIndex.MANUAL_CONFERENCE_ID);
                        }
                    } while ((mAssociatedEventId != eventId) && c.moveToNext());
                }
            } finally {
                c.close();
            }
        }

        notifyConfCallInfoDataMgr();
    }

    // cache will be refreshed
    public void caceheConferenceCallSetting(Cursor eventCursor, Cursor confInfoCursor) {
        if (confInfoCursor == null) {
            return;
        }
        if ((eventCursor == null) || (eventCursor.getCount() == 0)) {
            confInfoCursor.close();
            return;
        }

        long eventId = -1L;
        int index = eventCursor.getColumnIndex(Events._ID);
        if (index != -1) {
            eventId = eventCursor.getLong(index);
        }

        // extract conference call info from the meeting location and set
        // default data source flag
        // to ConferenceCallInfo.DATA_SOURCE_FLAG_MEETING_LOCATION
        index = eventCursor.getColumnIndex(Events.EVENT_LOCATION);
        if ((index != -1) && (mDataMgr != null)) {
            mDataSourceFlag = ConferenceCallInfo.DATA_SOURCE_FLAG_MEETING_LOCATION;
            mDataMgr.decodeMeetingConfCallInfo(eventCursor.getString(index));
        }
        try {
            if (confInfoCursor.moveToFirst()) {
                do {
                    mAssociatedEventId = confInfoCursor.getLong(ConferenceCallInfo.ColumnIndex.ASSOCIATED_EVENT_ID);
                    if ((mAssociatedEventId == eventId) || (mAssociatedEventId == -1L)) {
                        mDataSourceFlag = confInfoCursor.getInt(ConferenceCallInfo.ColumnIndex.DATA_SOURCE_FLAG);
                        mContactDataId = confInfoCursor.getInt(ConferenceCallInfo.ColumnIndex.CONTACT_DATA_ID);
                        mContactPhoneType = confInfoCursor.getInt(ConferenceCallInfo.ColumnIndex.CONTACT_PHONE_TYPE);
                        mManualConfNumber = confInfoCursor.getString(ConferenceCallInfo.ColumnIndex.MANUAL_CONFERENCE_NUMBER);
                        mManualConfId = confInfoCursor.getString(ConferenceCallInfo.ColumnIndex.MANUAL_CONFERENCE_ID);
                    }
                } while ((mAssociatedEventId != eventId) && confInfoCursor.moveToNext());
            }
        } finally {
            eventCursor.close();
            confInfoCursor.close();
        }
        // notifyConfCallInfoDataMgr();
        mDataMgr.notifySettingChange(this);
    }

    // cache will be refreshed
    public void updateConferenceCallSetting(Context ctx, ContentValues content) {
        if (content == null)
            return;

        String organizerEmail = content.getAsString(ConferenceCallInfo.ColumnNames.ORGANIZER_EMAIL);
        if ((organizerEmail == null) || (organizerEmail.length() == 0)){
            return;
        }

        if (!content.containsKey(ConferenceCallInfo.ColumnNames.ASSOCIATED_EVENT_ID))
            return;

        if (!content.containsKey(ConferenceCallInfo.ColumnNames.DATA_SOURCE_FLAG))
            return;


        // update the cache
        Long longValue  = content.getAsLong(ConferenceCallInfo.ColumnNames.ASSOCIATED_EVENT_ID);
        if (longValue != null) {
            mAssociatedEventId = longValue;
        }
        // check the validity of associated event id, valid value is -1 for all meetings or > 0 for
        // specific event
        if ((mAssociatedEventId == 0) || (mAssociatedEventId < -1L))
            return;

        Integer temp = null; // for KW
        temp = content.getAsInteger(ConferenceCallInfo.ColumnNames.DATA_SOURCE_FLAG);
        if (temp != null) {
            mDataSourceFlag = temp;
        }

        mContactDataId = -1;
        if (content.containsKey(ConferenceCallInfo.ColumnNames.CONTACT_DATA_ID)) {
            temp = content.getAsInteger(ConferenceCallInfo.ColumnNames.CONTACT_DATA_ID);
            if (temp != null) {
                mContactDataId = temp;
            }
        }

        mContactPhoneType = -1;
        if (content.containsKey(ConferenceCallInfo.ColumnNames.CONTACT_PHONE_TYPE)) {
            temp = content.getAsInteger(ConferenceCallInfo.ColumnNames.CONTACT_PHONE_TYPE);
            if (temp != null) {
                mContactPhoneType = temp;
            }
        }

        mManualConfNumber = null;
        if (content.containsKey(ConferenceCallInfo.ColumnNames.MANUAL_CONFERENCE_NUMBER)) {
            mManualConfNumber = content.getAsString(ConferenceCallInfo.ColumnNames.MANUAL_CONFERENCE_NUMBER);
        }

        mManualConfId = null;
        if (content.containsKey(ConferenceCallInfo.ColumnNames.MANUAL_CONFERENCE_ID)) {
            mManualConfId = content.getAsString(ConferenceCallInfo.ColumnNames.MANUAL_CONFERENCE_ID);
        }

        // update the database
        ContentResolver cr = ctx.getContentResolver();
        if (mAssociatedEventId == -1L) {
            // Apply to all meetings: remove all settings for this organizer and insert a new one
            cr.delete(ConferenceCallInfo.CONTENT_URI,
                      ConferenceCallInfo.ColumnNames.ORGANIZER_EMAIL + "='"+ organizerEmail + "'", null);
            cr.insert(ConferenceCallInfo.CONTENT_URI, content);
        } else {
            // Apply to a specific meeting: if exist then update it, otherwise insert a new one
            int _id = -1;
            Cursor c = cr.query(ConferenceCallInfo.CONTENT_URI, new String[]{ConferenceCallInfo.ColumnNames._ID},
                                ConferenceCallInfo.ColumnNames.ORGANIZER_EMAIL + "='"+ organizerEmail + "' and " +
                                ConferenceCallInfo.ColumnNames.ASSOCIATED_EVENT_ID + "="+ mAssociatedEventId, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        _id = c.getInt(0);
                    }
                } finally {
                    c.close();
                }
            }

            if (_id > 0) {
                cr.update(ContentUris.withAppendedId(ConferenceCallInfo.CONTENT_URI, _id), content, null, null);
            } else {
                cr.insert(ConferenceCallInfo.CONTENT_URI, content);
            }
        }

        notifyConfCallInfoDataMgr();
    }

    public int getConfCallInfoDataSource() {
        return mDataSourceFlag;
    }

    public int getContactDataId() {
        return mContactDataId;
    }

    public int getContactPhoneType() {
        return mContactPhoneType;
    }

    public String getManualConfNumber() {
        return mManualConfNumber;
    }

    public String getManualConfId() {
        return mManualConfId;
    }
}
