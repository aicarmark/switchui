package com.motorola.providers.contacts;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.android.providers.contacts.ContactsDatabaseHelper.MimetypesColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;

public class GroupBota {
    private static final String TAG = "GroupBota";

    /**
     * legacy local group account type/name
     */
    public static final String sLegacyLocalGroupName = "LocalGroups";
    public static final String sLegacyLocalGroupType = "com.motorola.contacts.LocalGroups";

    /**
     * local group account name
     */
    public static final String sLocalGroupName = "Motorola";
    /**
     * local group account type
     */
    public static final String sLocalGroupType = "com.motorola.contacts.agg_group_account_type";
    /**
     * local group mime-type
     */
    public static final String sLocalGroupMimetype = "vnd.android.cursor.item/local_group_membership";


    private SQLiteStatement mGroupInsert = null;
    private SQLiteStatement mGroupMembershipInsert = null;
    private long           mGroupMimetypeId = 0;

    // join table of legacy group/raw contact
    // the collume would be group_name, raw_contact_id, raw_contact_account_type, raw_contact_account_name
    String legacy_group_join_table = " raw_contacts join "
    + "(select data.data_sync2 as contact_id, title from data "
    + " join mimetypes on (data.mimetype_id=mimetypes._id AND mimetypes.mimetype='vnd.android.cursor.item/group_membership' AND data_sync1 LIKE 'content://com.android.contacts/contacts%') "
    + "  join contacts on ( data.data_sync2=contacts._id) "
    + " join groups on (groups.account_type='com.motorola.contacts.LocalGroups' AND groups.account_name='LocalGroups' "
    + "          AND groups.deleted<> 1 AND group_visible=0 AND data.data1=groups._id) "
    + " ) as allcontacts "
    + "on allcontacts.contact_id=raw_contacts.contact_id ";

    private static GroupBota  sGroupBotaInstance = null;
    private GroupBota(){
    }

    public static GroupBota SingleInstance(){
        if(sGroupBotaInstance == null){

            sGroupBotaInstance = new GroupBota();

        }
        return sGroupBotaInstance;
    }

    // handle all contacts that belongs to accounts that does not support sync
    private boolean update(SQLiteDatabase db) {

        HashMap<String, Long> local_groupaccount2Id = new HashMap<String, Long>();

        // get all distinct groups title
        // account name
        Cursor cursor_all_local_group = db.query(true,
                Tables.GROUPS, new String[] { Groups.TITLE },
                Groups.ACCOUNT_NAME + "=? AND " + Groups.ACCOUNT_TYPE + "=?",
                new String[]{sLegacyLocalGroupName, sLegacyLocalGroupType},
                null, null, null, null);

        while (cursor_all_local_group.moveToNext()) {

            String title = cursor_all_local_group.getString(0);

            mGroupInsert.bindString(1, title);
            mGroupInsert.bindString(2, sLocalGroupType);
            mGroupInsert.bindString(3, sLocalGroupName);

            long groupId = mGroupInsert.executeInsert();

            local_groupaccount2Id.put(title, groupId);
        }
        cursor_all_local_group.close();

        ///////////// for test
        java.util.Iterator<String> it0 = local_groupaccount2Id.keySet().iterator();

        Log.i(TAG, "all local groups:");
        while (it0.hasNext()) {
            String ga = (String) it0.next();
            Log.i(TAG, ga);
        }
        Log.i(TAG, "all local groups -- end");
        /////////////////// for test

        //all contacts that support sync
        HashMap<Long, ArrayList<Long>> groupid2RawContactArray = new HashMap<Long, ArrayList<Long>>();

        // all raw contacts that belonging one group
        Cursor all_raw_contact = db.query(legacy_group_join_table,
                new String[] { Groups.TITLE, RawContacts._ID },
                null, null, null, null, null);

        while (all_raw_contact.moveToNext()) {
            String groupTitle = all_raw_contact.getString(0);
            long rawContactId = all_raw_contact.getLong(1);

            long groupid = local_groupaccount2Id.get(groupTitle);

            ArrayList<Long> rawcontacts = groupid2RawContactArray.get(groupid);
            if (rawcontacts == null) {
                rawcontacts = new ArrayList<Long>();
                groupid2RawContactArray.put(groupid, rawcontacts);
            }

            rawcontacts.add(rawContactId);
        }
        all_raw_contact.close();


        // now we need to query all rawcontacts
        java.util.Iterator<Long> it = groupid2RawContactArray.keySet().iterator();

        while (it.hasNext()) {
            Long key = (Long) it.next();

            long groupid = key.longValue();
            ArrayList<Long> rawcontacts = groupid2RawContactArray.get(groupid);
            if(rawcontacts != null){
                String allrawcontacts_ids = "";
                for(Long raw:rawcontacts){
                    allrawcontacts_ids = allrawcontacts_ids + String.valueOf(raw) + ",";

                    mGroupMembershipInsert.bindLong(1, mGroupMimetypeId);
                    mGroupMembershipInsert.bindLong(2, raw);
                    mGroupMembershipInsert.bindLong(3, groupid);
                    mGroupMembershipInsert.executeInsert();
                }
                Log.i(TAG, "Group id is:" + groupid + ", all raw id:");
                Log.i(TAG, allrawcontacts_ids);
            }else{
                Log.i(TAG, "Group id is:" + groupid + ", all raw id:");
                Log.i(TAG, "null");
            }
        }

        return true;
    }

    private void init(SQLiteDatabase db){
        // compile group insert statement
        mGroupInsert = db
        .compileStatement("INSERT OR IGNORE INTO "
                + Tables.GROUPS + "("
                + Groups.TITLE
                + ","
                + Groups.ACCOUNT_TYPE
                + "," + Groups.ACCOUNT_NAME
                + ") VALUES (?,?,?)");

        // compile local group member ship insert
        mGroupMembershipInsert = db
        .compileStatement("INSERT OR IGNORE INTO "
                + Tables.DATA + "("
                + " mimetype_id "
                + "," + Data.RAW_CONTACT_ID
                + "," + Data.DATA1
                + ") VALUES (?,?,?)");

        // query mimetype for group membership
        Cursor cmimetypes= db.query(Tables.MIMETYPES,
                new String[]{MimetypesColumns._ID}, MimetypesColumns.MIMETYPE + "=?",
                //new String[]{ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE},
                new String[]{sLocalGroupMimetype},
                null, null, null);
        try {
            if (cmimetypes != null && cmimetypes.moveToFirst()) {
                mGroupMimetypeId = cmimetypes.getInt(0);
            }else{
                ContentValues content = new ContentValues();
                content.put(MimetypesColumns.MIMETYPE, sLocalGroupMimetype);
                mGroupMimetypeId = db.insert(Tables.MIMETYPES, null, content);
            }
        } finally {
            if (cmimetypes != null) {
                cmimetypes.close();
            }
        }

        Log.i(TAG, "mimetype id of group is:" + mGroupMimetypeId);
    }
    private void cleanup(SQLiteDatabase db) {

        // cleanup group table
        db.delete(Tables.GROUPS,
                Groups.ACCOUNT_NAME + "=? AND " + Groups.ACCOUNT_TYPE + "=?",
                new String[]{sLegacyLocalGroupName, sLegacyLocalGroupType});

        // cleanup raw contacts table
        db.delete(Tables.RAW_CONTACTS,
                Groups.ACCOUNT_NAME + "=? AND " + Groups.ACCOUNT_TYPE + "=?",
                new String[]{sLegacyLocalGroupName, sLegacyLocalGroupType});

    }

    public void run(SQLiteDatabase db){
        init(db);
        update(db);
        cleanup(db);
    }
}
