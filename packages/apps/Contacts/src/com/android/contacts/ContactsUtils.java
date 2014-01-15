/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts;

import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.model.HardCodedSources;
import com.android.contacts.test.NeededForTesting;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.motorola.android.telephony.PhoneModeManager;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.graphics.Rect;
import android.database.Cursor;
import android.location.CountryDetector;
import android.location.Country;
import android.net.Uri;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.motorola.contacts.groups.GroupAPI;
import com.motorola.contacts.list.ContactMultiplePickerResultContentProvider.Resultable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ContactsUtils {
    private static final String TAG = "ContactsUtils";
    private static final String WAIT_SYMBOL_AS_STRING = String.valueOf(PhoneNumberUtils.WAIT);

    public final static String SELECT_EXISTING_MEMBER_DATA_IDS_RAW_CONTACT = 
                                                      Data.RAW_CONTACT_ID + " IN ( SELECT " + Data.RAW_CONTACT_ID 
                                                      + " FROM " + "data JOIN mimetypes ON (data.mimetype_id = mimetypes._id)"
                                                      + " WHERE (" + Data.MIMETYPE + "='" + GroupMembership.CONTENT_ITEM_TYPE + "'"
                                                      + " OR " + Data.MIMETYPE + "='" + GroupAPI.LOCAL_GROUP_MIMETYPE + "')" 
                                                      + " AND " + GroupMembership.GROUP_ROW_ID + " IN "
                                                          + "( SELECT groups." + Groups._ID
                                                          + " FROM groups"
                                                          + " WHERE " + Groups.TITLE + "=?"
                                                          + " AND " + Groups.DELETED + "<>1" + " ) ) ";          

    private static final String GROUP_SMS_SELECTION = Data.MIMETYPE + " = '" + Phone.CONTENT_ITEM_TYPE + "' AND " 
                                                      + SELECT_EXISTING_MEMBER_DATA_IDS_RAW_CONTACT;                                               
                                                      
//    private static final String GROUP_SMS_SELECTION = Data.MIMETYPE + " = '" + Phone.CONTENT_ITEM_TYPE + "' AND (" + Data.DATA2 + " = " + Phone.TYPE_MOBILE +
//                                                      " OR " + Data.DATA2 + " = " + Phone.TYPE_WORK_MOBILE +  " OR " + Data.DATA2 + " = " + Phone.TYPE_MMS + ") AND " +
//                                                      Data.RAW_CONTACT_ID + " IN ( SELECT " + Data.RAW_CONTACT_ID + " FROM view_data WHERE " +
//                                                      Data.MIMETYPE + " = '" + GroupMembership.CONTENT_ITEM_TYPE + "' AND " + Data.DATA1 + " = ? )";
    private static final String GROUP_EMAIL_SELECTION = Data.MIMETYPE + " = '" + Email.CONTENT_ITEM_TYPE + "' AND " 
                                                        + SELECT_EXISTING_MEMBER_DATA_IDS_RAW_CONTACT;                                               


    // Possible value of "carrier_name" in /res/values/config.xml: CMCC, CU, CT
    public static final String Carrier_CMCC = "CMCC";
    public static final String Carrier_CT = "CT";
    public static final String Carrier_CU = "CU";
    
    public static final String CallLog_NETWORK = "network";
    public static boolean is_vt_enabled = false;
    public static final String IntRoamCallBackCall = "isIntRoamCallBackCall";
    public static final String IntRoamCall = "isIntRoamCall";
    public static final String CALLED_BY = "called_by";
    public static final String DIAL_BY_INTL_ROAMING_CALL = "intl_roaming_call";
    public static final long UPDATE_VIEWS_DELAY = 2000;
    public static final String TRANS_DIALPAD = "com.android.contacts.activities.TransDialPad";

    public class Group {
        public static final int ADDR_PHONE = 0;
        public static final int ADDR_EMAIL = 1;
        public static final int ADDR_EMAIL_NAME = 2;
    }
    
    public static class GroupInfo
    {
        public long groupId;
        public String title;
        public String titleDisplay;
        public boolean isChecked ;  // if the contact bind to the group
        public int pos;   // use to indicate the show position
        public long dataId;  // the data Id through which the contact bind to
    }

    static final String[] GROUPS_PROJECTION = new String[] {
/*    	
            Groups._ID, // 0
            Groups.TITLE, // 1
            Groups.TITLE_RES, // 2
            Groups.RES_PACKAGE, // 3
*/          
            Groups.TITLE, // 0  
            Groups.SUMMARY_COUNT, //1
    };
/*
    static final int COL_ID = 0;
    static final int COL_TITLE = 1;
    static final int COL_TITLE_RES = 2;
    static final int COL_RES_PACKAGE = 3;
*/    
    static final int COL_TITLE = 0;
    static final int COL_SUMMARY_COUNT = 1;    

    // TODO find a proper place for the canonical version of these
    public interface ProviderNames {
        String YAHOO = "Yahoo";
        String GTALK = "GTalk";
        String MSN = "MSN";
        String ICQ = "ICQ";
        String AIM = "AIM";
        String XMPP = "XMPP";
        String JABBER = "JABBER";
        String SKYPE = "SKYPE";
        String QQ = "QQ";
    }

    /**
     * This looks up the provider name defined in
     * ProviderNames from the predefined IM protocol id.
     * This is used for interacting with the IM application.
     *
     * @param protocol the protocol ID
     * @return the provider name the IM app uses for the given protocol, or null if no
     * provider is defined for the given protocol
     * @hide
     */
    public static String lookupProviderNameFromId(int protocol) {
        switch (protocol) {
            case Im.PROTOCOL_GOOGLE_TALK:
                return ProviderNames.GTALK;
            case Im.PROTOCOL_AIM:
                return ProviderNames.AIM;
            case Im.PROTOCOL_MSN:
                return ProviderNames.MSN;
            case Im.PROTOCOL_YAHOO:
                return ProviderNames.YAHOO;
            case Im.PROTOCOL_ICQ:
                return ProviderNames.ICQ;
            case Im.PROTOCOL_JABBER:
                return ProviderNames.JABBER;
            case Im.PROTOCOL_SKYPE:
                return ProviderNames.SKYPE;
            case Im.PROTOCOL_QQ:
                return ProviderNames.QQ;
        }
        return null;
    }

    /**
     * Test if the given {@link CharSequence} contains any graphic characters,
     * first checking {@link TextUtils#isEmpty(CharSequence)} to handle null.
     */
    public static boolean isGraphic(CharSequence str) {
        return !TextUtils.isEmpty(str) && TextUtils.isGraphic(str);
    }

    /**
     * Returns true if two objects are considered equal.  Two null references are equal here.
     */
    @NeededForTesting
    public static boolean areObjectsEqual(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    /**
     * Returns true if two data with mimetypes which represent values in contact entries are
     * considered equal for collapsing in the GUI. For caller-id, use
     * {@link PhoneNumberUtils#compare(Context, String, String)} instead
     */
    public static final boolean shouldCollapse(CharSequence mimetype1, CharSequence data1,
            CharSequence mimetype2, CharSequence data2) {
        // different mimetypes? don't collapse
        if (!TextUtils.equals(mimetype1, mimetype2)) return false;

        // exact same string? good, bail out early
        if (TextUtils.equals(data1, data2)) return true;

        // so if either is null, these two must be different
        if (data1 == null || data2 == null) return false;

        // if this is not about phone numbers, we know this is not a match (of course, some
        // mimetypes could have more sophisticated matching is the future, e.g. addresses)
        if (!TextUtils.equals(Phone.CONTENT_ITEM_TYPE, mimetype1)) return false;

        // Now do the full phone number thing. split into parts, seperated by waiting symbol
        // and compare them individually
        final String[] dataParts1 = data1.toString().split(WAIT_SYMBOL_AS_STRING);
        final String[] dataParts2 = data2.toString().split(WAIT_SYMBOL_AS_STRING);
        if (dataParts1.length != dataParts2.length) return false;

        // MOTOROLA: Use PhoneNumberUtils.compareLoosely which requires MIN_MATCH (7) characters to match
        // because 10000 is collapsed with 18918910000 use PhoneNumberUtil in ICS
        for (int i = 0; i < dataParts1.length; i++) {
            if (!PhoneNumberUtils.compare(dataParts1[i], dataParts2[i])) {
                return false;
            }
        }
/*
        final PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        for (int i = 0; i < dataParts1.length; i++) {
            final String dataPart1 = dataParts1[i];
            final String dataPart2 = dataParts2[i];

            // substrings equal? shortcut, don't parse
            if (TextUtils.equals(dataPart1, dataPart2)) continue;

            // do a full parse of the numbers
            switch (util.isNumberMatch(dataPart1, dataPart2)) {
                case NOT_A_NUMBER:
                    // don't understand the numbers? let's play it safe
                    return false;
                case NO_MATCH:
                    return false;
                case EXACT_MATCH:
                case SHORT_NSN_MATCH:
                case NSN_MATCH:
                    break;
                default:
                    throw new IllegalStateException("Unknown result value from phone number " +
                            "library");
            }
        }
*/
        return true;
    }

    /**
     * Returns true if two {@link Intent}s are both null, or have the same action.
     */
    public static final boolean areIntentActionEqual(Intent a, Intent b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return TextUtils.equals(a.getAction(), b.getAction());
    }

    /**
     * @return The ISO 3166-1 two letters country code of the country the user
     *         is in.
     */
    public static final String getCurrentCountryIso(Context context) {
        CountryDetector detector =
                (CountryDetector) context.getSystemService(Context.COUNTRY_DETECTOR);
        // MOTO calling code IKHSS6-5858 Begin
        if(detector == null) {
            Log.w(TAG, "getCurrentCountryIso - context.getSystemService got null");
            return null;
        }
        Country ct = detector.detectCountry();
        if(ct == null) {
            Log.w(TAG, "getCurrentCountryIso - detector.detectCountry got null");
            return null;
        }
        return ct.getCountryIso();
        // MOTO calling code IKHSS6-5858 End
    }

    public static boolean areContactWritableAccountsAvailable(Context context) {
        final List<AccountWithDataSet> accounts =
                AccountTypeManager.getInstance(context).getAccountsWithCard(true /* writeable */);
        return !accounts.isEmpty();
    }

    public static boolean areGroupWritableAccountsAvailable(Context context) {
        final List<AccountWithDataSet> accounts =
                AccountTypeManager.getInstance(context).getGroupWritableAccounts();
        return !accounts.isEmpty();
    }

    /**
     * Returns the intent to launch for the given invitable account type and contact lookup URI.
     * This will return null if the account type is not invitable (i.e. there is no
     * {@link AccountType#getInviteContactActivityClassName()} or
     * {@link AccountType#resPackageName}).
     */
    public static Intent getInvitableIntent(AccountType accountType, Uri lookupUri) {
        String resPackageName = accountType.resPackageName;
        String className = accountType.getInviteContactActivityClassName();
        if (TextUtils.isEmpty(resPackageName) || TextUtils.isEmpty(className)) {
            return null;
        }
        Intent intent = new Intent();
        intent.setClassName(resPackageName, className);

        intent.setAction(ContactsContract.Intents.INVITE_CONTACT);

        // Data is the lookup URI.
        intent.setData(lookupUri);
        return intent;
    }

    /**
     * Returns a header view based on the R.layout.list_separator, where the
     * containing {@link TextView} is set using the given textResourceId.
     */
    public static View createHeaderView(Context context, int textResourceId) {
        View view = View.inflate(context, R.layout.list_separator, null);
        TextView textView = (TextView) view.findViewById(R.id.title);
        textView.setText(context.getString(textResourceId));
        return view;
    }

    /**
     * Returns the {@link Rect} with left, top, right, and bottom coordinates
     * that are equivalent to the given {@link View}'s bounds. This is equivalent to how the
     * target {@link Rect} is calculated in {@link QuickContact#showQuickContact}.
     */
    public static Rect getTargetRectFromView(Context context, View view) {
        final float appScale = context.getResources().getCompatibilityInfo().applicationScale;
        final int[] pos = new int[2];
        view.getLocationOnScreen(pos);

        final Rect rect = new Rect();
        rect.left = (int) (pos[0] * appScale + 0.5f);
        rect.top = (int) (pos[1] * appScale + 0.5f);
        rect.right = (int) ((pos[0] + view.getWidth()) * appScale + 0.5f);
        rect.bottom = (int) ((pos[1] + view.getHeight()) * appScale + 0.5f);
        return rect;
    }
    
    // get all members' raw_contact ids of the specified group in format (id1, id2 ...)
    public static String getGroupMemberIds(ContentResolver cr, long groupdId) {

        String rawidingroup = "( SELECT " + Data.RAW_CONTACT_ID + " FROM view_data WHERE " +
                              Data.MIMETYPE + " = '" + GroupMembership.CONTENT_ITEM_TYPE + "' AND " + Data.DATA1 + "=" +  groupdId + ")";
        return rawidingroup;
    }

    // get the data id of all members' default sms numbers or email of the specified group title in format (id1, id2 ...)
    public static int getAllDefDataIdOfGroupMembers(ContentResolver cr, ArrayList<Long> dataList, String title, String dataType) {
        int size = 0;
        if ("sms".equals(dataType)) {
            size = queryDefDataIdInGroup2(cr, dataList, title, Phone.CONTENT_ITEM_TYPE);
        } else if ("email".equals(dataType)) {
            size = queryDefDataIdInGroup2(cr, dataList, title, Email.CONTENT_ITEM_TYPE);
        }
        return size;
    }

/*    // get all members' default sms numbers or email of the specified group in format (number1, number2 ...)
    public static int getAllDefDataOfGroupMembers(ContentResolver cr, ArrayList<String> dataList, long groupId, int dataType) {
        int size = 0;
        if (Group.ADDR_PHONE == dataType) {
            size = queryDefDataInGroup2(cr, dataList, groupId, Phone.CONTENT_ITEM_TYPE);
        }
        if (Group.ADDR_EMAIL == dataType) {
            size = queryDefDataInGroup2(cr, dataList, groupId, Email.CONTENT_ITEM_TYPE);
        }
        if (Group.ADDR_EMAIL_NAME == dataType) {
            size = queryEmailsWithNameInGroup2(cr, dataList, groupId, Email.CONTENT_ITEM_TYPE);
        }
        return size;
    }*/

    // list all groups under a account, if account is null, return all group list
    public static int queryGroupInfoByAccount(Context context, String accountName, String accountType,
                      ArrayList<GroupInfo> groupList) {
        return queryGroupInfoByAccount(context, accountName, accountType, groupList, false);
    }

    // default show all the groups, if onlyVisible true, then only show visible groups
    public static int queryGroupInfoByAccount(Context context, String accountName, String accountType,
                      ArrayList<GroupInfo> groupList, boolean onlyVisible) {

        final ContentResolver resolver = context.getContentResolver();
        long groupId = -1;
        Cursor groupCursor = null;
        String sortOrder = Groups.TITLE;// + " COLLATE LOCALIZED_PINYIN ASC";

        try {
            if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountType)) {
                if (onlyVisible)
                    groupCursor = resolver.query(Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "agg_groups_summary"), GROUPS_PROJECTION,
                            Groups.GROUP_VISIBLE + "=1" + " AND " + Groups.DELETED + "=0", null, sortOrder);
                else
                    groupCursor = resolver.query(Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "agg_groups_summary"), GROUPS_PROJECTION,
                            Groups.DELETED + "=0", null, sortOrder);
            }
            else {
                if (onlyVisible)
                    groupCursor = resolver.query(Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "agg_groups_summary"), GROUPS_PROJECTION,
                            Groups.GROUP_VISIBLE + "=1" + " AND " + Groups.DELETED + "=0" + " AND " +
                            Groups.ACCOUNT_NAME + " =? AND " + Groups.ACCOUNT_TYPE + " =?",
                            new String[] {accountName, accountType}, sortOrder);
                else
                    groupCursor = resolver.query(Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "agg_groups_summary"), GROUPS_PROJECTION,
                            Groups.DELETED + "=0" + " AND " + Groups.ACCOUNT_NAME + " =? AND " + Groups.ACCOUNT_TYPE + " =?",
                            new String[] {accountName, accountType}, sortOrder);
            }
            if (groupCursor != null) {
                while(groupCursor.moveToNext()) {
                    insertOneGroupEntry(groupCursor, groupList, context);
                }
            }
        } finally {
            if (groupCursor != null) {
                groupCursor.close();
            }
        }

        return  groupList.size();
    }

    private static void insertOneGroupEntry(Cursor groupCursor, ArrayList<GroupInfo> groupList, Context context) {

        // get the group display name
        String title = groupCursor.getString(COL_TITLE);
        String titleDisplay = "";
        titleDisplay = title;

        GroupInfo pinfo = new GroupInfo();
        pinfo.groupId = -1;
        pinfo.title = title;
        pinfo.titleDisplay = titleDisplay;
        groupList.add(pinfo);
    }

    public static int queryEmailsWithNameInGroup2(ContentResolver resolver, ArrayList<String> dataList, long groupId, String mimeType) {
        String select;
        String sortorder = Data.RAW_CONTACT_ID;
        int result = 0;
        if (Email.CONTENT_ITEM_TYPE.equals(mimeType)){
                select = GROUP_EMAIL_SELECTION;
                sortorder = Data.RAW_CONTACT_ID + ", " + Data.IS_PRIMARY + " DESC";
        }else return result;
        Cursor count_cursor = resolver.query(Data.CONTENT_URI, new String [] {Data.RAW_CONTACT_ID, Data.DATA1,Data.DISPLAY_NAME},
                select, new String[] {String.valueOf(groupId)}, sortorder);
        if(count_cursor != null){
                        HashSet<Long> raw_id = new HashSet<Long> ();
                        while (count_cursor.moveToNext()){
                                Long id = count_cursor.getLong(0);
                                if(raw_id.add(id)){
                                        String email = count_cursor.getString(1);
                                String name = count_cursor.getString(2);
                                email = name + "<" + email + ">";
                                        dataList.add(email);
                                }
                        }
                        count_cursor.close();
                }
        return dataList.size();
    }

/*    public static int queryDefDataInGroup2(ContentResolver resolver, ArrayList<String> dataList, long groupId, String mimeType) {
        String select;
        String sortorder = Data.RAW_CONTACT_ID;
        int result = 0;
        if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                select = GROUP_SMS_SELECTION;
        } else if (Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
                select = GROUP_EMAIL_SELECTION;
                sortorder = Data.RAW_CONTACT_ID + ", " + Data.IS_PRIMARY + " DESC";
        } else {
                return result;
        }
        Cursor count_cursor = resolver.query(Data.CONTENT_URI, new String [] {Data.RAW_CONTACT_ID, Data.DATA1},
                                            select, new String[] {String.valueOf(groupId)}, sortorder);
        if (count_cursor != null) {
                HashSet<Long> raw_id = new HashSet<Long> ();
                while (count_cursor.moveToNext()) {
                        Long id = count_cursor.getLong(0);
                        if (raw_id.add(id)) {
                            dataList.add(count_cursor.getString(1));
                        }
                        }
                count_cursor.close();
        }
        return dataList.size();
    }*/

    public static int queryDefDataIdInGroup2(ContentResolver resolver, ArrayList<Long> dataList, String title, String mimeType) {
        String select;
        // String sortorder = Data.RAW_CONTACT_ID;
        String sortorder = Data.RAW_CONTACT_ID + ", " + Data.IS_SUPER_PRIMARY + " DESC, " + Data.IS_PRIMARY + " DESC";
        int result = 0;
        if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
            select = GROUP_SMS_SELECTION;
        } else if (Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
            select = GROUP_EMAIL_SELECTION;
        } else {
            return result;
        }
        
        Cursor count_cursor = resolver.query(Data.CONTENT_URI, new String [] {Data.RAW_CONTACT_ID, Data._ID},
                                            select, new String[] {title}, sortorder);
        if (count_cursor != null) {
            HashSet<Long> raw_id = new HashSet<Long> ();
            while (count_cursor.moveToNext()) {
                Long id = count_cursor.getLong(0);
                if (raw_id.add(id)) {
                    dataList.add(count_cursor.getLong(1));
                }
            }
            count_cursor.close();
        }
        return dataList.size();
    }

    public static long queryForContactId(ContentResolver cr, long rawContactId) {
        Cursor contactIdCursor = null;
        long contactId = -1;
        try {
            contactIdCursor = cr.query(RawContacts.CONTENT_URI,
                    new String[] {RawContacts.CONTACT_ID},
                    RawContacts._ID + "=" + rawContactId, null, null);
            if (contactIdCursor != null && contactIdCursor.moveToFirst()) {
                contactId = contactIdCursor.getLong(0);
            }
        } finally {
            if (contactIdCursor != null) {
                contactIdCursor.close();
            }
        }
        return contactId;
    }

    public static long queryForNameRawContactId(ContentResolver cr, long ContactId) {
        Cursor nameRawContactIdCursor = null;
        long nameRawContactId = -1;
        try {
            nameRawContactIdCursor = cr.query(Contacts.CONTENT_URI,
                    new String[] {Contacts.NAME_RAW_CONTACT_ID},
                    Contacts._ID + "=" + ContactId, null, null);
            if (nameRawContactIdCursor != null && nameRawContactIdCursor.moveToFirst()) {
                nameRawContactId = nameRawContactIdCursor.getLong(0);
            }
        } finally {
            if (nameRawContactIdCursor != null) {
                nameRawContactIdCursor.close();
            }
        }
        return nameRawContactId;
    }

    public static long queryForNameRawContactId(ContentResolver cr, Uri contactUri) {
        long nameRawContactId = -1;
        if (contactUri == null) return nameRawContactId;
        Cursor nameRawContactIdCursor = null;
        try {
            nameRawContactIdCursor = cr.query(contactUri,
                    new String[] {Contacts.NAME_RAW_CONTACT_ID},
                    null, null, null);
            if (nameRawContactIdCursor != null && nameRawContactIdCursor.moveToFirst()) {
                nameRawContactId = nameRawContactIdCursor.getLong(0);
            }
        } finally {
            if (nameRawContactIdCursor != null) {
                nameRawContactIdCursor.close();
            }
        }
        return nameRawContactId;
    }

    public static ArrayList<Long> queryForAllRawContactIds(ContentResolver cr, long contactId, boolean forExportOnly) {
        Cursor rawContactIdCursor = null;
        ArrayList<Long> rawContactIds = new ArrayList<Long>();
        Uri uri = RawContacts.CONTENT_URI;
        if (forExportOnly) {
            uri = uri.buildUpon().appendQueryParameter("for_export_only", "1").build();
        }
        try {
            rawContactIdCursor = cr.query(uri, new String[] {RawContacts._ID},
                    RawContacts.CONTACT_ID + "=" + contactId, null, null);
            if (rawContactIdCursor != null) {
                while (rawContactIdCursor.moveToNext()) {
                    rawContactIds.add(rawContactIdCursor.getLong(0));
                }
            }
        } finally {
            if (rawContactIdCursor != null) {
                rawContactIdCursor.close();
            }
        }
        return rawContactIds;
    }

    public static Account getAccountbyRawContactId(ContentResolver cr, long rawContactId) {
        Cursor accountTypeCursor = null;
        String accountType = null;
        String accountName = null;
        Account account = null;
        try {
            accountTypeCursor = cr.query(RawContacts.CONTENT_URI,
                    new String[] {RawContacts.ACCOUNT_TYPE, RawContacts.ACCOUNT_NAME},
                    RawContacts._ID + "=" + rawContactId, null, null);
            if (accountTypeCursor != null && accountTypeCursor.moveToFirst()) {
                accountType = accountTypeCursor.getString(0);
                accountName = accountTypeCursor.getString(1);
            }
        } finally {
            if (accountTypeCursor != null) {
                accountTypeCursor.close();
            }
        }
        if (!TextUtils.isEmpty(accountType) && !TextUtils.isEmpty(accountName)) {
            account = new Account(accountName, accountType);
        }
        return account;
    }

    public static boolean isCarDockMode(Intent intent) {
    	String intentSender = intent.getStringExtra("INTENT_SENDER");
    	boolean isCarDock = intentSender != null && "CARDOCK_SENT".equals(intentSender);
        return isCarDock;
    }

    public static String getCarrierName(Context context) {
        // Carrier name: CMCC, CU, CT
        String carrier_name = context.getResources().getText(R.string.carrier_name).toString();
        Log.d("ContactsUtils", "carrier_name = "+carrier_name);
        return carrier_name;
    }

    public static String getUIStyle(Context context) {
        // Look10 or Dark
        String ui_style = context.getResources().getText(R.string.ui_style).toString();
        Log.d("ContactsUtils", "ui_style = "+ui_style);
        return ui_style;
    }

    public static boolean isCT189EmailEnabled(Context context) {
        Log.d("ContactsUtils", "isCT189EmailEnabled = "+context.getResources().getBoolean(R.bool.ftr_34621_189email));
        return context.getResources().getBoolean(R.bool.ftr_34621_189email);
    }

    public static String getStringbyPhoneType(int phone_type) {
        if (phone_type == TelephonyManager.PHONE_TYPE_CDMA)
            return "CDMA";
        else if (phone_type == TelephonyManager.PHONE_TYPE_GSM)
            return "GSM";
        else
            return "";
    }

    public static String getGSMSimOperator() {
        String SimOperator = null;
        TelephonyManager defaultMgr = TelephonyManager.getDefault();
        TelephonyManager secondMgr = null;
        if (PhoneModeManager.isDmds()) {
            /* to-pass-build, Xinyu Liu/dcjf34 */ 
            secondMgr = null;//TelephonyManager.getDefault(false);
        }

        if (defaultMgr.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
            SimOperator = defaultMgr.getSimOperator();
        } else if (secondMgr != null) {
            if (secondMgr.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
                SimOperator = secondMgr.getSimOperator();
            }
        }
        Log.d("ContactsUtils", "getGSMSimOperator,  SimOperator = "+SimOperator);
        return SimOperator;
    }

    public static String getCarrierbySimOperator(String sim_operator) {
        if (sim_operator != null) {
            if (sim_operator.equals("46000") || sim_operator.equals("46002") || sim_operator.equals("46007"))
                return "CMCC";
            else if (sim_operator.equals("46001") || sim_operator.equals("46006"))
                return "CU";
            else
                return "";
        } else
            return "";
    }

    public static int getDefaultIPPrefixbyPhoneType(int phone_type) {
        Log.d("ContactsUtils", "getDefaultIPPrefixbyOperator,  phone_type = "+phone_type);
        int res_id = -1;

        if (phone_type == TelephonyManager.PHONE_TYPE_CDMA) {
            res_id = R.string.default_cdma_ip_prefix;
        } else if (phone_type == TelephonyManager.PHONE_TYPE_GSM) {
            String CarrierName = getCarrierbySimOperator(getGSMSimOperator());
            if (CarrierName != null) {
                if (CarrierName.equals("CMCC"))
                    res_id = R.string.default_gsm_ip_prefix;
                else if (CarrierName.equals("CU"))
                    res_id = R.string.default_cu_ip_prefix;
                else
                    res_id = R.string.default_gsm_ip_prefix; // Use CMCC as default for safty
            }
        }
        return res_id;
    }

    public static boolean isPhoneEnabled(int phoneType) {
        boolean isEnabled = false;

        TelephonyManager defaultMgr = TelephonyManager.getDefault();
        TelephonyManager secondMgr = null;
        if (PhoneModeManager.isDmds()) {
            /* to-pass-build, Xinyu Liu/dcjf34 */ 
            secondMgr = null;//TelephonyManager.getDefault(false);
        }

        int defaultPhoneType = defaultMgr.getPhoneType();
        if (defaultPhoneType == phoneType) {
            if (defaultMgr.getSimState() == TelephonyManager.SIM_STATE_READY) {
                isEnabled = true;
            }
        } else {
            if (secondMgr != null && secondMgr.getSimState() == TelephonyManager.SIM_STATE_READY) {
                isEnabled = true;
            }
        }
        return isEnabled;
    }

    public static boolean hasInternalSdcard(Context context) {
        boolean has_internal_sdcard = context.getResources().getBoolean(R.bool.has_internal_sdcard);
        Log.v(TAG, "hasInternalSdcard = "+has_internal_sdcard);
        return has_internal_sdcard;
    }

    public static boolean noSdcard(Context context) {
        boolean no_sdcard;
        if (ContactsUtils.hasInternalSdcard(context)) {
            no_sdcard = !isSdcardMounted(context, true) && !isSdcardMounted(context, false);
        } else {
            no_sdcard = !isSdcardMounted(context, false);
        }
        return no_sdcard;
    }

    public static boolean isSdcardMounted(Context context, boolean isAltSdcard) {
        String tag = "inner";
        String MOUNTED = "mounted";
        if (!ContactsUtils.hasInternalSdcard(context)) {
            isAltSdcard = false;
            tag = "outer";
        }
        if (isAltSdcard) {
            /* to-pass-build, Xinyu Liu/dcjf34 */ 
            String outer_storage_state = Environment.getExternalStorageState();
            if (outer_storage_state.equals(MOUNTED)) {
                Log.d(TAG, "outer sdcard is mounted.");
                return true;
            } else {
                Log.d(TAG, "outer sdcard is NOT mounted.");
                return false;
            }
        } else {
            String inner_storage_state = Environment.getExternalStorageState();
            if (inner_storage_state.equals(MOUNTED)) {
                Log.d(TAG, tag + " sdcard is mounted.");
                return true;
            } else {
                Log.d(TAG, tag + " sdcard is NOT mounted.");
                return false;
            }
        }
    }

    // suggest to run it with Async Task as it perhaps long running in session id case (>1000)    
    public static boolean getMultiplePickResult(Context context, Intent data, ArrayList<Uri> selected_contact_uris) {

        Boolean isResultIncluded = data.getBooleanExtra(Resultable.SELECTTION_RESULT_INCLUDED, false);
        if (!isResultIncluded) { //get uri via the multiple picker session id
            Log.v(TAG, "get uri via the multiple picker session id");
            final long sessionId = data.getLongExtra(Resultable.SELECTION_RESULT_SESSION_ID, -1);
            if (sessionId == -1) {
                Log.e(TAG, "error in the sessionId: " + sessionId);
                return false;
            } else {
                final String[] LOOKUP_URI_PROJECTION = new String[] {
                      "data"
                };
                Uri sessionUri = ContentUris.withAppendedId(Uri.withAppendedPath(
                        Uri.parse("content://com.motorola.contacts.list.ContactMultiplePickerResult/results"),"session_id"),
                        sessionId);

                final Cursor cursor = context.getContentResolver().query(sessionUri, LOOKUP_URI_PROJECTION, null, null, null);
                try{
                    if (cursor == null || !cursor.moveToFirst()) {
                        Log.e(TAG, "error in reading the multiplepicker uri: " + sessionUri);
                    }

                    //IKTABLETMAIN-518 original code will skip the 1st item directly
                    for(;!cursor.isAfterLast(); cursor.moveToNext()) {
                        Uri lookup_uri = Uri.parse(cursor.getString(0));
                        selected_contact_uris.add(lookup_uri);
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        } else { //retrieve the lookups from the result directly
            Log.v(TAG, "get uri from multiple picker directly");
            ArrayList<Uri> uris = data.getParcelableArrayListExtra(Resultable.SELECTED_CONTACTS);
            if (uris != null) {
                selected_contact_uris.addAll(uris);
            }
        }

        Log.v(TAG, "the return count = " + selected_contact_uris.size());
        return true;
    }

    private static class SimNotReadyClickListener implements DialogInterface.OnClickListener {
    	Context mContext = null;
    	boolean mToDismissParent = false;
    	
        public SimNotReadyClickListener(Context context, boolean dismissParent) {
        	mContext = context;
        	mToDismissParent = dismissParent;
        }
        
        public void onClick(DialogInterface dialog, int which) {
        	if (mContext != null && mToDismissParent) {
                ((Activity)mContext).finish();                     
            }
        }    	
    }

    private static Dialog createErrorDialog(Context context, int resIdTitle, int resIdMessage, boolean dismissParent) {
        final DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
            }
        };    	
    	
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(resIdTitle);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(resIdMessage);
        builder.setPositiveButton(android.R.string.ok, new SimNotReadyClickListener(context, dismissParent));
        builder.setCancelable(false);
        Dialog ret_dlg =  builder.create();
        ret_dlg.setOnDismissListener(dismissListener);        
        return ret_dlg;
    }

    public static boolean contactIsCardType(Context context, Uri contactUri) {
        if (contactUri != null) {
            final long rawContactId = queryForNameRawContactId(context.getContentResolver(), contactUri);
            Cursor accountCursor = null;
            String accountName = null;
            String accountType = null;
            try {
                accountCursor = context.getContentResolver().query(RawContacts.CONTENT_URI,
                        new String[] {RawContacts.ACCOUNT_NAME, RawContacts.ACCOUNT_TYPE},
                        RawContacts._ID + "=" + rawContactId, null, null);
                if (accountCursor != null && accountCursor.moveToFirst()) {
                    accountName = accountCursor.getString(0);
                    accountType = accountCursor.getString(1);
                }
            } finally {
                if (accountCursor != null) {
                    accountCursor.close();
                }
            }
            
            if (accountName != null && accountType != null && HardCodedSources.ACCOUNT_TYPE_CARD.equals(accountType)) {
            	return true;
            }
        }
        
        return false;                	    	
    }

    public static boolean checkSimAvailable(Context context, boolean dismissParent) {
    	boolean cardReady = false;    	
    	boolean cReady = false;
    	boolean gReady = false;
 
        if (PhoneModeManager.isDmds()) {
            cReady = SimUtility.isSimReady(TelephonyManager.PHONE_TYPE_CDMA);
            gReady = SimUtility.isSimReady(TelephonyManager.PHONE_TYPE_GSM);
            if (!cReady && !gReady) { // check C/G card available 
                createErrorDialog(context, R.string.no_card_title, R.string.no_card_text, dismissParent).show();
                return false;
            }
        } else {
            cardReady = SimUtility.isSimReady(TelephonyManager.getDefault().getPhoneType());
            if (!cardReady) { // check card available 
                createErrorDialog(context, R.string.no_card_title, R.string.no_card_text, dismissParent).show();
                return false;
            }            
        }

        if (!SimUtility.getSIMLoadStatus()) {
            createErrorDialog(context, R.string.loadsim_title, R.string.loadsim_text, dismissParent).show();
            return false;
        }
        
        return true;
    }

    public static boolean haveTwoCards(Context context) {
        if (!PhoneModeManager.isDmds()) return false;

        boolean isCDMAEnabled = isPhoneEnabled(TelephonyManager.PHONE_TYPE_CDMA);
        boolean isGSMEnabled = isPhoneEnabled(TelephonyManager.PHONE_TYPE_GSM);

        return isCDMAEnabled && isGSMEnabled;
    }
    
    /**
     * delete all groups under specified account which have been marked as "deleted" 
     *
     * @param resolver the content resolver
     */
    public static void deleteGroupsMarked(ContentResolver resolver, String accountType)
    {
    	//Log.v(TAG, "begin deleteGroupsMarked(), accountType = " + accountType);
        String selection = RawContacts.ACCOUNT_TYPE + "='" + accountType + "'" + 
                           " AND " + Groups.DELETED + "!=0";
        int deletedItems = resolver.delete(Groups.CONTENT_URI
                                                 .buildUpon()
                                                 .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                                                 .build(),
                                           selection, 
                                           null);
    	Log.v(TAG, "end deleteGroupsMarked(), deleted groups' items = "+deletedItems);
    }

	/* Added for switchuitwo-381 begin */
	/**
	 * @param number
	 *            The number used to make a call
	 * @param context
	 * @return
	 */
	public static boolean isEmergencyNumber(String number, Context context) {
		Log.d(TAG, "ContactsUtils isEmergencyNumber number=" + number
				+ " carrier=" + getCarrierName(context));
		if (number != null) {
			if (getCarrierName(context).equals(Carrier_CMCC)) {
				return (number.equals("112") || number.equals("911")
						|| number.equals("110") || number.equals("999")
						|| number.equals("000") || number.equals("08")
						|| number.equals("118") || number.equals("119"));
			} else if (getCarrierName(context).equals(Carrier_CT)) {
				return (number.equals("112") || number.equals("911")
						|| number.equals("110") || number.equals("999")
						|| number.equals("120") || number.equals("119"));
			} else
				return false;
		} else {
			return false;
		}
	}
	/* Added for switchuitwo-381 end */
    
}
