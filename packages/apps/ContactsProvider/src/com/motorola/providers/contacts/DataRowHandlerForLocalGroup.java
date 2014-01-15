package com.motorola.providers.contacts;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.text.TextUtils;

import com.android.internal.util.Objects;
import com.android.providers.contacts.AccountWithDataSet;
import com.android.providers.contacts.aggregation.ContactAggregator;
import com.android.providers.contacts.ContactsDatabaseHelper;
import com.android.providers.contacts.DataRowHandler;
import com.android.providers.contacts.TransactionContext;
import com.android.providers.contacts.ContactsDatabaseHelper.Clauses;
import com.android.providers.contacts.ContactsDatabaseHelper.DataColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.GroupsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.ContactsProvider2.GroupIdCacheEntry;
import com.android.providers.contacts.DataRowHandler.DataDeleteQuery;
import com.android.providers.contacts.DataRowHandler.DataUpdateQuery;

public class DataRowHandlerForLocalGroup extends DataRowHandler {


    private final HashMap<String, ArrayList<GroupIdCacheEntry>> mGroupIdCache;

    public DataRowHandlerForLocalGroup(Context context, ContactsDatabaseHelper dbHelper,
            ContactAggregator aggregator,
            HashMap<String, ArrayList<GroupIdCacheEntry>> groupIdCache) {
        super(context, dbHelper, aggregator, GroupMembership.CONTENT_ITEM_TYPE);
        mGroupIdCache = groupIdCache;
    }

    @Override
    public long insert(SQLiteDatabase db, TransactionContext txContext, long rawContactId,
            ContentValues values) {
        long dataId = super.insert(db, txContext, rawContactId, values);
        return dataId;
    }

    @Override
    public boolean update(SQLiteDatabase db, TransactionContext txContext, ContentValues values,
            Cursor c, boolean callerIsSyncAdapter) {
        long rawContactId = c.getLong(DataUpdateQuery.RAW_CONTACT_ID);

        if (!super.update(db, txContext, values, c, callerIsSyncAdapter)) {
            return false;
        }
        return true;
    }


    @Override
    public int delete(SQLiteDatabase db, TransactionContext txContext, Cursor c) {
        long rawContactId = c.getLong(DataDeleteQuery.RAW_CONTACT_ID);

        int count = super.delete(db, txContext, c);
        return count;
    }
}
