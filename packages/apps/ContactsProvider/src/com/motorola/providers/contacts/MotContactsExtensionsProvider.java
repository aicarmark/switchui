package com.motorola.providers.contacts;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Groups;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.CallerInfo;
import com.android.providers.contacts.CallLogProvider;
import com.android.providers.contacts.ContactsDatabaseHelper; //IKHSS7-2898
import com.android.providers.contacts.ContactsDatabaseHelper.MimetypesColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.NameLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.ContactsDatabaseHelper.Views;
import com.android.providers.contacts.ContactsProvider2;
import com.android.providers.contacts.NameNormalizer;
import com.android.common.content.ProjectionMap;
import com.motorola.providers.contacts.smartdialer.SmartDialerTokenUtils;

public class MotContactsExtensionsProvider {

    private static final String TAG = "MotContactsExtensionsProvider";

    private final ContactsProvider2 mDelegate;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    /*final string definition*/
    private static final String PARTIAL_FIELD_PICK_VCARD = "partial_field_vcard";//smartdialer feature
    private static final String AGG_GROUPS_ACCOUNT_TYPE = "com.local.contacts";//aggregated Group feature
    //smartdialer feature
    private static final String MOST_RC_SELECTION  =
        "(" + Calls.NUMBER + "!=" + CallerInfo.PRIVATE_NUMBER + ") AND (" +
        Calls.NUMBER + "!=" + CallerInfo.UNKNOWN_NUMBER + ") AND (" +
        Calls.NUMBER + "!=" + CallerInfo.PAYPHONE_NUMBER + ")";

    // 29967 ICE
    public static final class InCaseOfEmergency implements BaseColumns {
        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of
         * people.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/ice";

        public static final String ACTION_VIEW_DETAIL_IN_LOCKMODE = "com.motorola.contacts.VIEW_PERSON_DETAILS_IN_LOCKMODE";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://"
                + ContactsContract.AUTHORITY + "/ice");

        public static final Uri ICE_CONTACTS_URI = Uri.parse("content://"
                + ContactsContract.AUTHORITY + "/ice/contact");

        public static final Uri ICE_NOTES_URI = Uri.parse("content://"
                + ContactsContract.AUTHORITY + "/ice/note");

        public static final Uri ICE_OWNER_URI = Uri.parse("content://"
                + ContactsContract.AUTHORITY + "/ice/owner");

        public static final Uri ICE_IDENTITY_URI = Uri.parse("content://"
                + ContactsContract.AUTHORITY + "/ice/contacts");

        //Column name definition
        public static final String ICE_SLOTNO = "no";

        public static final String ICE_DISPLAYNAME = "displayname";

        public static final String ICE_ITEMTYPE = "type";

        public static final String ICE_NOTE = "note";

        public static final String ICE_IDENTITY = "contacts";

        public static final int ICE_CONTACT_SLOT_COUNT = 3;

        public static final int ICE_NOTE_SLOT_COUNT = 3;

        //Item type
        public static final int ICE_ITEM_TYPE_NOTE = -1;

        public static final int ICE_ITEM_TYPE_OWNER = 0;

        public static final int ICE_ITEM_TYPE_CONTACT = 1;

        public static final String ICE_LOCKMODE_EXTRA = "isLockMode";

        public static final String ICE_FILTER_PEOPLES = "iceFilterPeoples";
    }

    private static final int CONTACTS_PARTIAL_FIELD_AS_VCARD = 1000;//mot partial field vcard feature
    private static final int PROFILE_PARTIAL_FIELD_AS_VCARD = 1001;//mot partial field vcard feature
    private static final int SMARTDIALER_FUZZY_MATCH_QUERY = 1005;//smartdialer feature
    private static final int SMARTDIALER_EXACT_MATCH_QUERY = 1006;//smartdialer feature
    private static final int AGG_GROUPS_SEARCH = 1011;    //aggregated Group search
    private static final int AGG_GROUPS_SUMMARY = 1012;  // aggregated Group summary
    private static final int AGG_GROUPS_RAWCONTACT_GROUPS_DETAILS = 1013;  // aggregated Group summary
    // 29967 ICE
    private static final int ICE = 1020;
    private static final int ICE_CONTACTS = 1021;
    private static final int ICE_NOTES = 1022;
    private static final int ICE_CONTACTS_ID = 1023;
    private static final int ICE_NOTES_ID = 1024;
    private static final int ICE_OWNER = 1025;
    private static final int ICE_IDENTITY_ID = 1026;

    /*class member variable*/
    private String mPartialFieldPickVcard;//used to save partial vcard string
    private HashMap<Long, Boolean> mLocalGroupIds = new HashMap<Long, Boolean>();//aggregated Group feature


    /** Contains columns from the view_data table */
    public static final HashMap<String, String> sDataViewProjectionMap;//smartdialer feature
    /** Contains columns from the calls table */
    public static final HashMap<String, String> sCallsProjectionMap;//smartdialer feature
    /** Contains {@link Groups} columns along with summary details */
    private static final ProjectionMap sPhoneEmailInAggGroup ;// aggregated Group feature


    static {
        // Contacts URI matching table
        final UriMatcher matcher = sUriMatcher;
        matcher.addURI(ContactsContract.AUTHORITY, "contacts/as_partial_field_vcard/*",
                CONTACTS_PARTIAL_FIELD_AS_VCARD);
        matcher.addURI(ContactsContract.AUTHORITY, "profile/as_partial_field_vcard",
                PROFILE_PARTIAL_FIELD_AS_VCARD);
        //smartdialer feature
        matcher.addURI(ContactsContract.AUTHORITY, "smartdialer/fuzzy_match_query", SMARTDIALER_FUZZY_MATCH_QUERY);
        matcher.addURI(ContactsContract.AUTHORITY, "smartdialer/exact_match_query", SMARTDIALER_EXACT_MATCH_QUERY);
        matcher.addURI(ContactsContract.AUTHORITY, "smartdialer/exact_match_query/*", SMARTDIALER_EXACT_MATCH_QUERY);
        //aggregated Group feature
        matcher.addURI(ContactsContract.AUTHORITY, "agg_groups_summary", AGG_GROUPS_SUMMARY);
        matcher.addURI(ContactsContract.AUTHORITY, "agg_groups_search", AGG_GROUPS_SEARCH);
        matcher.addURI(ContactsContract.AUTHORITY, "agg_groups_rawcontact_groups_detail", AGG_GROUPS_RAWCONTACT_GROUPS_DETAILS);

        //29967 ICE
        matcher.addURI(ContactsContract.AUTHORITY, "ice", ICE);
        matcher.addURI(ContactsContract.AUTHORITY, "ice/contact", ICE_CONTACTS);
        matcher.addURI(ContactsContract.AUTHORITY, "ice/note", ICE_NOTES);
        matcher.addURI(ContactsContract.AUTHORITY, "ice/owner", ICE_OWNER);
        matcher.addURI(ContactsContract.AUTHORITY, "ice/contact/#", ICE_CONTACTS_ID);
        matcher.addURI(ContactsContract.AUTHORITY, "ice/note/#", ICE_NOTES_ID);
        matcher.addURI(ContactsContract.AUTHORITY, "ice/contacts/#", ICE_IDENTITY_ID);

        //smartdialer feature
        sDataViewProjectionMap = new HashMap<String, String>();
        sDataViewProjectionMap.put(Data.DATA1, Data.DATA1); // Phone.NUMBER
        sDataViewProjectionMap.put(Data.DATA2, Data.DATA2); // Phone.TYPE
        sDataViewProjectionMap.put(Data.DATA3, Data.DATA3); // Phone.LABEL
        sDataViewProjectionMap.put(Data.DISPLAY_NAME, Data.DISPLAY_NAME);
        sDataViewProjectionMap.put(Data.CONTACT_ID, Data.CONTACT_ID); // CHINADEV
        sDataViewProjectionMap.put(Data.PHOTO_ID, Data.PHOTO_ID);
        sDataViewProjectionMap.put(Data.SORT_KEY_PRIMARY, Data.SORT_KEY_PRIMARY);
        sDataViewProjectionMap.put(Data.TIMES_CONTACTED, Data.TIMES_CONTACTED);
        sDataViewProjectionMap.put(Data.STARRED, Data.STARRED);
        //smartdialer feature
        sCallsProjectionMap = new HashMap<String, String>();
        sCallsProjectionMap.put(Calls.NUMBER, Calls.NUMBER);
        sCallsProjectionMap.put(Calls.DATE, Calls.DATE);
        sCallsProjectionMap.put(Calls.TYPE, Calls.TYPE);
        sCallsProjectionMap.put(Calls.CACHED_NAME, Calls.CACHED_NAME);
        sCallsProjectionMap.put(Calls.CACHED_NUMBER_TYPE, Calls.CACHED_NUMBER_TYPE);
        sCallsProjectionMap.put(Calls.CACHED_NUMBER_LABEL, Calls.CACHED_NUMBER_LABEL);
        sCallsProjectionMap.put(Calls.CACHED_LOOKUP_URI, Calls.CACHED_LOOKUP_URI); // CHINADEV
        sCallsProjectionMap.put(CallLogProvider.NETWORK, CallLogProvider.NETWORK); // CHINADEV DMDS
        // aggregated Group feature
        sPhoneEmailInAggGroup = ProjectionMap.builder()
            .add(Groups.TITLE)
            .add(MimetypesColumns.MIMETYPE, "matcheddata.mimetype")
            .add("matched_title", "matcheddata.matched_title")
            .add(Data.RAW_CONTACT_ID, Tables.DATA + "." + Data.RAW_CONTACT_ID)
            .build();
    }


    public MotContactsExtensionsProvider(ContactsProvider2 delegate) {
        mDelegate = delegate;
    }

    //Implement this to initialize Motorola contacts extension on startup.
    //call back from onCreate() of hosted provider
    //please do not invoke getWritableDatabase in function onCreate
    public void onCreate(ContactsDatabaseHelper dbHelper) { //IKHSS7-2898

    }

    public Cursor query(SQLiteDatabase db, Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String groupBy = null;
        String limit = null;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case CONTACTS_PARTIAL_FIELD_AS_VCARD: {//mot partial field vcard feature
                final String lookupKey = Uri.encode(uri.getPathSegments().get(2));
                long contactId = mDelegate.lookupContactIdByLookupKey(db, lookupKey);
                qb.setTables(mDelegate.getDelegateDatabaseHelper().getContactView(true));/* require restricted */
                qb.setProjectionMap(mDelegate.sContactsVCardProjectionMap);
                selectionArgs = mDelegate.insertSelectionArg(selectionArgs, String.valueOf(contactId));
                qb.appendWhere(Contacts._ID + "=?");
                break;
            }

            case PROFILE_PARTIAL_FIELD_AS_VCARD: {//mot partial field vcard feature
                //for profile, no need to append query parameter
                qb.setTables(mDelegate.getDelegateDatabaseHelper().getContactView(true));/* require restricted */
                qb.setProjectionMap(mDelegate.sContactsVCardProjectionMap);
                break;
            }

            case SMARTDIALER_EXACT_MATCH_QUERY: {//smartdialer feature
                mDelegate.setTablesAndProjectionMapForData(qb, uri, projection, false, null);
                qb.appendWhere(" AND " + Data.MIMETYPE + " = '" + Phone.CONTENT_ITEM_TYPE + "'");
                if (uri.getPathSegments().size() > 2) {
                    String filterParam = uri.getLastPathSegment();
                    StringBuilder sb = new StringBuilder();

                    sb.append(" AND (");
                    String normalizedName = NameNormalizer.normalize(filterParam);

                    // query contact name no matter input string contain digits or not
                    if (normalizedName.length() > 0) {
                        sb.append(Data.RAW_CONTACT_ID + " IN ");
                        sb.append("(" +
                            "SELECT " + NameLookupColumns.RAW_CONTACT_ID +
                            " FROM " + Tables.NAME_LOOKUP +
                            " WHERE " + NameLookupColumns.NORMALIZED_NAME +
                            " GLOB '");
                        sb.append(normalizedName);
                        sb.append("*')");
                    }

                    // if input string contain digits, replace the letters to digits to query contact number.
                    if (hasDigitsChar(filterParam)) {
                        Log.d(TAG, "The string contain digits, will convert string to pure digits for number search.");
                        sb.append(" OR ");
                        String number = PhoneNumberUtils.convertKeypadLettersToDigits(filterParam);
                        String dataSelect = Data._ID + " IN " + "(SELECT " + Data._ID + " FROM "
                                         + mDelegate.getDelegateDatabaseHelper().getDataView()
                                         + " WHERE " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE
                                         + "' AND " + Data.DATA1 + "<>''" + " AND " + Data.DATA1 + " IS NOT NULL "
                                         + " AND " + "(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE("
                                         + Phone.NUMBER
                                         + ", ' ', ''), '(', ''), ')', ''), '-', ''), '.', ''), ';', ''), '/', ''), ',', ''), '*', ''), '+', ''), '#', '') LIKE '%"
                                         + number + "%'))";

                        sb.append(dataSelect);
                    }
                    sb.append(")");
                    qb.appendWhere(sb);
                }
                qb.setDistinct(true);

                if (sortOrder == null) {
                    sortOrder = Contacts.TIMES_CONTACTED + " desc"+',' + Contacts.STARRED +" desc"+',' + Contacts.SORT_KEY_PRIMARY + " ASC";
                }
                limit = "25"; // MOTOROLA Contacts MOD IKSTABLE6-3473, limit contacts to 25 items for exact match
                break;
            }

            case SMARTDIALER_FUZZY_MATCH_QUERY: {//smartdialer feature
                Log.d(TAG, "query(): smartDialer query. selection=[" + selection + "]");

                String filterMatchStr = SmartDialerTokenUtils.getFilterDigitsForMatch(selection);

                Log.d(TAG, "query(): filterMatchDigits = [" + filterMatchStr + "]");

                if (TextUtils.isEmpty(filterMatchStr)) {
                    Log.d(TAG, "query(): query for most recent call.");
                    String mostRecentSelect = "(SELECT * FROM " + Tables.CALLS + " GROUP BY " + Calls.NUMBER +
                                                  " ORDER BY " + Calls.DEFAULT_SORT_ORDER + " LIMIT 1)";
                    mostRecentSelect = "(SELECT * FROM " + Tables.CALLS + " GROUP BY " + Calls.NUMBER +
                            " ORDER BY " + Calls.DATE + " DESC LIMIT 50)";//ChinaDev

                    SQLiteQueryBuilder qb_mostRc = new SQLiteQueryBuilder();
                    qb_mostRc.setTables(mostRecentSelect);
                    qb_mostRc.setProjectionMap(sCallsProjectionMap);

                    String[] callsProjection = new String[projection.length];
                    for (int i = 0; i < projection.length; i++) {
                        if (sCallsProjectionMap.containsKey(projection[i])) {
                            callsProjection[i] = projection[i];
                        } else {
                            callsProjection[i] = "NULL AS " + projection[i];
                        }
                    }

                    callsProjection[projection.length-1] = "0 AS group_id";

                    final String mostRecentQuery = qb_mostRc.buildQuery(callsProjection, MOST_RC_SELECTION,
                            null, null, null, null, null);
                    
                    //String orderBy = Calls.DATE + " DESC, " + Contacts.DISPLAY_NAME + " COLLATE LOCALIZED_PINYIN ASC";
                    String orderBy = Calls.DATE + " DESC";

                    // Put them together
                    String dialerLimit = ContactsProvider2.getLimit(uri);
                    final String query = qb.buildUnionQuery(new String[] {mostRecentQuery},
                            orderBy, dialerLimit);
                    return db.rawQuery(query, null);

                } else {
                    Log.d(TAG, "query(): query view_data & calls table.");

                    // construct the search filter string
                    String nameMatchStr = DatabaseUtils.sqlEscapeString(filterMatchStr + '%');
                    String numberMatchStr = DatabaseUtils.sqlEscapeString('%' + filterMatchStr + '%');

                    // MOTO MOD BEGIN, PIM-Contacts, gfj763, IKHSS7-12126
                    SQLiteQueryBuilder qb_viewData = new SQLiteQueryBuilder();

                    /*
                     * Build 1st query try to match name & number in view_data table
                     * Sort order: frequent -> starred -> contacts
                     * Limit: 500
                     */
                    qb_viewData.setTables(mDelegate.getDelegateDatabaseHelper().getDataView());
                    qb_viewData.setProjectionMap(sDataViewProjectionMap);

                    String where_data = Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
                                   + " AND (" + Data.DATA1 + "<>'')" + " AND (" + Data.DATA1 + " IS NOT NULL) "
                                   + "AND ((" + Phone.RAW_CONTACT_ID + " IN (SELECT " + Phone.RAW_CONTACT_ID + " FROM "
                                   + MotContactsDatabaseHelper.Tables.MOTO_EXTENSION_LOOKUP + " WHERE " + Data.DATA2 + " LIKE "
                                   + nameMatchStr + ")) OR "
                                   + "(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE("
                                   + Phone.NUMBER + ", ' ', ''), '(', ''), ')', ''), '-', ''), '.', ''), ';', ''), '/', ''), ',', ''), '*', ''), '+', ''), '#', '') LIKE "
                                   + numberMatchStr + "))";
                    qb_viewData.appendWhere(where_data);
                    qb_viewData.setDistinct(true); // MOTO MOD, IKHSS6UPGR-9215

                    // filter all invalid input for view_data projection
                    String[] dataProjection = new String[projection.length];
                    for (int i = 0; i < projection.length; i++) {
                        if (sDataViewProjectionMap.containsKey(projection[i])) {
                            dataProjection[i] = projection[i];
                        } else {
                            dataProjection[i] = "NULL AS " + projection[i];
                        }
                    }

                    // set last column "group_id" to 1 to indicate the cursor is from view_data table
                    dataProjection[projection.length-1] = "1 AS group_id";

                    final String firstInnerQuery = qb_viewData.buildQuery(dataProjection, null, null, null, null,
                            Contacts.TIMES_CONTACTED + " DESC, " + Contacts.STARRED + " DESC, " + Contacts.SORT_KEY_PRIMARY + " COLLATE LOCALIZED ASC",
                            "500");

                    /*
                     * Build 2nd query to match non-contact recent call number in calls table
                     * Sort order: call date
                     * Limit: 40
                     */
                    SQLiteQueryBuilder qb_calls = new SQLiteQueryBuilder();
                    qb_calls.setTables(Tables.CALLS);
                    qb_calls.setProjectionMap(sCallsProjectionMap);
                    final int BLOCKED_TYPE = 5;

                    String where_calls = "(REPLACE( "
                            + Calls.NUMBER + ", '-', '') LIKE " + numberMatchStr + " AND (" + Calls.CACHED_NAME
                            + " IS NULL)" + " AND (" +  Calls.TYPE + "!=" + Integer.toString(BLOCKED_TYPE)
                            + " ))";
                    qb_calls.appendWhere(where_calls);

                    // filter all invalid input for calls projection
                    String[] callsProjection = new String[projection.length];
                    for (int i=0; i<projection.length; i++) {
                        if (sCallsProjectionMap.containsKey(projection[i])) {
                            callsProjection[i] = projection[i];
                        } else {
                            callsProjection[i] = "NULL AS " + projection[i];
                        }
                    }

                    // set last column "group_id" to 2 to indicate the cursor is from calls table
                    callsProjection[projection.length-1] = "2 AS group_id";

                    final String secondInnerQuery = qb_calls.buildQuery(callsProjection, null, null, Calls.NUMBER,
                            null, Calls.DEFAULT_SORT_ORDER, "40");

                    final String firstQuery = "SELECT * FROM (" + firstInnerQuery + ")";
                    final String secondQuery = "SELECT * FROM (" + secondInnerQuery + ")";

                    final String unionQuery =
                       qb.buildUnionQuery(new String[] {firstQuery, secondQuery}, null, null);

                    return db.rawQuery(unionQuery, null);
                    // MOTO MOD END, PIM-Contacts, gfj763, IKHSS7-12126
                }
            }

            case AGG_GROUPS_SUMMARY: {//aggregated Group feature
                qb.setTables("(  "
                        + "select * from groups where " + Groups.AUTO_ADD + "=0 AND " + Groups.FAVORITES + "=0 " +"group by title "
                        + ") as a "
                        + " left join "
                        + "("
                        + "select title as btitle, count(*) as "
                        + Groups.SUMMARY_COUNT
                        + " from ( "
                        + " select distinct groups.title as title, contact_id "
                        + " from contacts "
                        + " join raw_contacts ON (raw_contacts.contact_id=contacts._id) "
                        + " join data ON (data.raw_contact_id=raw_contacts._id) "
                        + " join mimetypes on mimetypes._id=data.mimetype_id AND ( mimetypes.mimetype='vnd.android.cursor.item/local_group_membership' OR mimetypes.mimetype='vnd.android.cursor.item/group_membership')"
                        + " join groups on data.data1=groups._id AND " + Groups.AUTO_ADD + "=0 AND " + Groups.FAVORITES + "=0 )"
                        + "group by btitle " + ") as b " + "on "
                        + "a.title= b.btitle");

                groupBy = Groups.TITLE;
                break;
            }


            case AGG_GROUPS_SEARCH:{//aggregated Group feature
                qb.setTables(
                        Tables.GROUPS
                        + " join data on data.data1=groups._id " + " AND " + Groups.AUTO_ADD + "=0 AND " + Groups.FAVORITES + "=0"
                        + " join " + Tables.RAW_CONTACTS + " on " + " raw_contacts._id=data.raw_contact_id "
                        + " join " + Tables.CONTACTS + " on contacts._id=raw_contacts.contact_id "
                        + " join "
                        + " (select data._id, data.data1 as matched_title, mimetype, raw_contacts.contact_id as contact_id from data "
                        + " join mimetypes on data.mimetype_id=mimetypes._id AND "
                        + " (mimetypes.mimetype='vnd.android.cursor.item/email_v2' ) "
                        + " join " + Tables.RAW_CONTACTS + " on " + " raw_contacts._id=data.raw_contact_id "
                        + " join " + Tables.CONTACTS + " on contacts._id=raw_contacts.contact_id "
                        + " UNION "
                        + " select data._id, phone_lookup.normalized_number as matched_title, mimetype, raw_contacts.contact_id as contact_id from data "
                        + " join mimetypes on data.mimetype_id=mimetypes._id AND "
                        + " (mimetypes.mimetype='vnd.android.cursor.item/phone_v2') "
                        + " join phone_lookup on data._id=phone_lookup.data_id  "
                        + " join " + Tables.RAW_CONTACTS + " on " + " raw_contacts._id=data.raw_contact_id "
                        + " join " + Tables.CONTACTS + " on contacts._id=raw_contacts.contact_id "
                        + " ) as matcheddata on matcheddata.contact_id=contacts._id "
                        + " join mimetypes on mimetypes._id=data.mimetype_id AND (mimetypes.mimetype='vnd.android.cursor.item/group_membership' OR mimetypes.mimetype='vnd.android.cursor.item/local_group_membership') "

                );
                qb.setProjectionMap(sPhoneEmailInAggGroup);
                break;
            }

            case AGG_GROUPS_RAWCONTACT_GROUPS_DETAILS:{

                qb.setTables(
                " (SELECT title, groups._id as _id, data.raw_contact_id as raw_contact_id, raw_contacts.contact_id as contact_id, accounts.account_name as account_name, accounts.account_type as account_type, accounts.data_set as data_set"
                + " from data "
                + " join groups as groups on groups._id=data.data1"
                /* nwq834   SWITCHUITWOV-333  2012-11-16 */
                + " join accounts on accounts._id=groups.account_id "
                 /* nwq834  end*/
                + " join mimetypes on mimetypes._id=data.mimetype_id AND (mimetypes.mimetype='vnd.android.cursor.item/group_membership' OR mimetypes.mimetype='vnd.android.cursor.item/local_group_membership') "
                + " join raw_contacts on raw_contacts._id=data.raw_contact_id"
                + " join contacts on contacts._id=raw_contacts.contact_id) "
                );

                break;
            }
            case ICE:{// 29967 ICE
                final HashMap<String, String> map = new HashMap<String, String>();
                map.put("_id", "ice_contacts._id AS " + InCaseOfEmergency._ID);
                map.put(InCaseOfEmergency.ICE_SLOTNO, "ice_contacts.no AS " + InCaseOfEmergency.ICE_SLOTNO);
                map.put(InCaseOfEmergency.ICE_ITEMTYPE, "ice_contacts.type AS " + InCaseOfEmergency.ICE_ITEMTYPE);
                map.put(InCaseOfEmergency.ICE_DISPLAYNAME, "name_raw_contact.display_name AS " + InCaseOfEmergency.ICE_DISPLAYNAME);

                SQLiteQueryBuilder qb1 = new SQLiteQueryBuilder();
                qb1.setTables(" ice_contacts join contacts AS contacts on (ice_contacts.contacts=contacts._id) "+
                           "join raw_contacts AS name_raw_contact ON (contacts.name_raw_contact_id=name_raw_contact._id)" );
                qb1.setProjectionMap(map);
                String mSubSQL1= qb1.buildQuery(projection, null, null, null, null, null, null);

                map.clear();
                map.put("_id", "ice_notes._id AS " + InCaseOfEmergency._ID);
                map.put(InCaseOfEmergency.ICE_SLOTNO, "ice_notes.no AS " + InCaseOfEmergency.ICE_SLOTNO);
                map.put(InCaseOfEmergency.ICE_ITEMTYPE, "ice_notes.type AS " + InCaseOfEmergency.ICE_ITEMTYPE);
                map.put(InCaseOfEmergency.ICE_DISPLAYNAME, "ice_notes.note AS " + InCaseOfEmergency.ICE_DISPLAYNAME);

                SQLiteQueryBuilder qb2 = new SQLiteQueryBuilder();
                qb2.setTables(" ice_notes");
                qb2.setProjectionMap(map);
                String mSubSQL2= qb2.buildQuery(projection, null, null, null, null, null, null);

                String sSQL = qb.buildUnionQuery(new String[] {mSubSQL1, mSubSQL2}, null, null );

                return db.rawQuery(sSQL, null);
            }

            /** Query contacts information By contacts id */
            case ICE_IDENTITY_ID:{// 29967 ICE
                 String rowID= uri.getLastPathSegment();
                 qb.setTables("view_contacts");
                 selection = "_id=?";
                 selectionArgs = new String[]{rowID};
                 sortOrder = null;
                 break;
            }

            /** ICE Contact Query By RowID */
            case ICE_CONTACTS_ID:{// 29967 ICE
                 String rowID= uri.getLastPathSegment();
                 final HashMap<String, String> map = new HashMap<String, String>();
                 map.put("_id", "ice_contacts._id AS " + InCaseOfEmergency._ID);
                 map.put(InCaseOfEmergency.ICE_SLOTNO, "ice_contacts.no AS " + InCaseOfEmergency.ICE_SLOTNO);
                 map.put(InCaseOfEmergency.ICE_ITEMTYPE, "ice_contacts.type AS " + InCaseOfEmergency.ICE_ITEMTYPE);
                 map.put(InCaseOfEmergency.ICE_IDENTITY, "ice_contacts.contacts AS " + InCaseOfEmergency.ICE_IDENTITY);

                 qb.setTables(" ice_contacts ");
                 qb.setProjectionMap(map);
                 selection = "ice_contacts._id=?";
                 selectionArgs = new String[]{rowID};
                 sortOrder = null;
                 break;
            }

            /** ICE Notes Query By RowID */
            case ICE_NOTES_ID:{// 29967 ICE
                String rowID= uri.getLastPathSegment();
                qb.setTables(" ice_notes");
                selection = "_id=?";
                selectionArgs = new String[]{rowID};
                sortOrder = null;
                break;
            }
        }

        qb.setStrict(true);
        Cursor cursor = mDelegate.query(db, qb, projection, selection, selectionArgs, sortOrder,
                groupBy, null, limit, null);
        return cursor;
    }

    public Uri insertInTransaction(SQLiteDatabase db, Uri uri, ContentValues values) {
        long id = 0;
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case CONTACTS_PARTIAL_FIELD_AS_VCARD://mot partial field vcard feature: save the vcard string
            case PROFILE_PARTIAL_FIELD_AS_VCARD:{//mot partial field vcard feature: save the vcard string
                mPartialFieldPickVcard = values.getAsString(PARTIAL_FIELD_PICK_VCARD);
                return uri;
            }
            case ICE_CONTACTS:{// 29967 ICE
                id = insertIceContact(db, uri, values);
                break;
            }
            case ICE_NOTES:{// 29967 ICE
                id = insertIceNotes(db, uri, values, false);
                break;
            }
            case ICE_OWNER:{// 29967 ICE
                id = insertIceNotes(db, uri, values, true);
                break;
            }
        }

        if (id < 0) {
            return null;
        }

        return ContentUris.withAppendedId(uri, id);
    }

    public int updateInTransaction(SQLiteDatabase db, Uri uri, ContentValues values,
            String selection, String[] selectionArgs) {
        int count = 0;
        Uri notifyUri = null;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case ICE_CONTACTS_ID:{// 29967 ICE
                notifyUri  = InCaseOfEmergency.CONTENT_URI;
                String rowID = uri.getLastPathSegment();

                count = db.update( "ice_contacts", values, "_id=?", new String[] {rowID});

                if (( count > 0 ) && (notifyUri != null)){
                    mDelegate.getContext().getContentResolver().notifyChange(InCaseOfEmergency.CONTENT_URI, null, false);
                }
                break;
            }
            case ICE_NOTES_ID:{// 29967 ICE
                notifyUri = InCaseOfEmergency.CONTENT_URI;
                String rowID= uri.getLastPathSegment();
                count = db.update( "ice_notes", values, "_id=?", new String[] {rowID});

                if (( count > 0 ) && (notifyUri != null)){
                    mDelegate.getContext().getContentResolver().notifyChange(InCaseOfEmergency.CONTENT_URI, null, false);
                }
                break;
            }
        }

        return count;
    }

    public int deleteInTransaction(SQLiteDatabase db, Uri uri, String selection,
            String[] selectionArgs) {
        int count = 0;
        Uri notifyUri = null;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            //29967 ICE
            case ICE:{
                notifyUri = InCaseOfEmergency.CONTENT_URI;
                int numContactsDeleted = db.delete("ice_contacts", selection, selectionArgs);
                int numNotesDeleted = db.delete("ice_notes", selection, selectionArgs);
                if (notifyUri != null){
                    mDelegate.getContext().getContentResolver().notifyChange(notifyUri, null, false);
                }
                count = numContactsDeleted + numNotesDeleted;
                break;
            }
            case ICE_CONTACTS_ID:{
                notifyUri = InCaseOfEmergency.CONTENT_URI;
                String rowID = uri.getLastPathSegment();
                count = db.delete("ice_contacts", "_id=?", new String[] {rowID});
                if (( count > 0 ) && (notifyUri != null)){
                    mDelegate.getContext().getContentResolver().notifyChange(notifyUri, null, false);
                }
                break;
            }
            case ICE_NOTES_ID:{
                notifyUri = InCaseOfEmergency.CONTENT_URI;
                String rowID = uri.getLastPathSegment();
                count = db.delete("ice_notes", "_id=?", new String[] {rowID});
                if ((count > 0) && (notifyUri != null)){
                    mDelegate.getContext().getContentResolver().notifyChange(notifyUri, null, false);
                }
                break;
            }
        }

        return count;
    }

    public AssetFileDescriptor openAssetFile(SQLiteDatabase db, Uri uri, String mode)
            throws FileNotFoundException {
        final int match = sUriMatcher.match(uri);
        switch (match) {
        case CONTACTS_PARTIAL_FIELD_AS_VCARD://mot partial field vcard feature
        case PROFILE_PARTIAL_FIELD_AS_VCARD: {//mot partial field vcard feature
            // When opening a contact as file, we pass back contents as a
            // vCard-encoded stream. We build into a local buffer first,
            // then pipe into MemoryFile once the exact size is known.
            final ByteArrayOutputStream localStream = new ByteArrayOutputStream();
            outputPartialFieldPickVCardAsVCard(uri, localStream, mPartialFieldPickVcard);
            return mDelegate.buildAssetFileDescriptor(localStream);
        }
        }
        return null;
    }

    public boolean isUriMatched(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case CONTACTS_PARTIAL_FIELD_AS_VCARD:
            case PROFILE_PARTIAL_FIELD_AS_VCARD:
            case SMARTDIALER_EXACT_MATCH_QUERY:
            case SMARTDIALER_FUZZY_MATCH_QUERY:
            case AGG_GROUPS_SUMMARY:
            case AGG_GROUPS_SEARCH:
            case AGG_GROUPS_RAWCONTACT_GROUPS_DETAILS:
            case ICE:
            case ICE_CONTACTS:
            case ICE_NOTES:
            case ICE_OWNER:
            case ICE_CONTACTS_ID:
            case ICE_NOTES_ID:
            case ICE_IDENTITY_ID:{
                return true;
            }
        }
        return false;
    }

    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case CONTACTS_PARTIAL_FIELD_AS_VCARD:
            case PROFILE_PARTIAL_FIELD_AS_VCARD: {
                return Contacts.CONTENT_VCARD_TYPE;
            }
            case ICE:{// 29967 ICE
                return InCaseOfEmergency.CONTENT_TYPE;
            }
        }
        return null;
    }

    //mot partial field vcard feature
    private void outputPartialFieldPickVCardAsVCard(Uri uri, OutputStream stream,
            String partialFieldPickVcard) {
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(stream));
            writer.write(partialFieldPickVcard);
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Log.w(TAG, "IOException during closing output stream: " + e);
                }
            }
        }
    }

    //smartdialer feature
    public static boolean hasDigitsChar(CharSequence str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        final int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    // 29967 ICE
    /** the leftmost is zero, if no empty slot, return -1 */
    private int getEmptySlotNo(SQLiteDatabase db, String table, int itemtype, String slotFieldName, int upperBound){
        Cursor c = db.rawQuery("select "+ slotFieldName + " as no from "+ table
                + " where "+ slotFieldName + "<" + upperBound
                + " and type=" + itemtype
                + " order by " + slotFieldName + " asc" , null);

        try {
            if (c!=null) {
                int expectNo = 0;
                if (c.moveToFirst()) {
                    while(!c.isAfterLast()) {
                        if (c.getInt(0) > expectNo) return expectNo;
                        expectNo++;
                        c.moveToNext();
                        }
                    if (expectNo < upperBound) return expectNo;
                } else {
                    return expectNo; //is empty table
                }
            }
        }
        finally {
            if(c != null) {
                c.close();
            }
        }
        return -1;
    }
    // 29967 ICE
    private long insertIceNotes(SQLiteDatabase db, Uri url, ContentValues initialValues,
            boolean isOwnerInfo) {
        ContentValues values = new ContentValues(initialValues);

        int itemtype = (isOwnerInfo ? InCaseOfEmergency.ICE_ITEM_TYPE_OWNER
                : InCaseOfEmergency.ICE_ITEM_TYPE_NOTE);

        if (!initialValues.containsKey(InCaseOfEmergency.ICE_SLOTNO)) {
            int slotNo = getEmptySlotNo(db, "ice_notes", itemtype, InCaseOfEmergency.ICE_SLOTNO,
                    InCaseOfEmergency.ICE_NOTE_SLOT_COUNT);
            if (slotNo < 0) return -1;
            values.put(InCaseOfEmergency.ICE_SLOTNO, slotNo);
        }
        values.put(InCaseOfEmergency.ICE_ITEMTYPE, itemtype);

        long id = db.insert("ice_notes", InCaseOfEmergency.ICE_NOTE, values);

        mDelegate.getContext().getContentResolver().notifyChange(
                InCaseOfEmergency.CONTENT_URI, null);
        return id;
    }
    // 29967 ICE
    private long insertIceContact(SQLiteDatabase db, Uri url, ContentValues initialValues) {
        ContentValues values = new ContentValues(initialValues);

        if (!initialValues.containsKey(InCaseOfEmergency.ICE_SLOTNO)) {
            int slotNo = getEmptySlotNo(db, "ice_contacts",
                    InCaseOfEmergency.ICE_ITEM_TYPE_CONTACT, InCaseOfEmergency.ICE_SLOTNO,
                    InCaseOfEmergency.ICE_CONTACT_SLOT_COUNT);
            if (slotNo < 0)    return -1;
            values.put(InCaseOfEmergency.ICE_SLOTNO, slotNo);
        }

        values.put(InCaseOfEmergency.ICE_ITEMTYPE, InCaseOfEmergency.ICE_ITEM_TYPE_CONTACT);

        long id = db.insert("ice_contacts", InCaseOfEmergency.ICE_IDENTITY, values);

        mDelegate.getContext().getContentResolver().notifyChange(
                InCaseOfEmergency.CONTENT_URI, null);
        return id;
    }

}
