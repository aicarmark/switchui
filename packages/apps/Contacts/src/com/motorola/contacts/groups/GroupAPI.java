/*
 * Copyright (C) 2011, Motorola, Inc,
 * All Rights Reserved
 * Class name: Intent
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 01-DEC-2011    a21740       Initial creation.
 **********************************************************
 */

package com.motorola.contacts.groups;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.util.Log;
import android.provider.ContactsContract;

public class GroupAPI {

    /**
     * local group account name
     */
    public static final String LOCAL_GROUP_ACCOUNT_NAME = "local-contacts"; //MOT CHINA
    /**
     * local group account type
     */
    public static final String LOCAL_GROUP_ACCOUNT_TYPE = "com.local.contacts"; //MOT CHINA
    /**
     * local group mime-type
     */
    public static final String LOCAL_GROUP_MIMETYPE = "vnd.android.cursor.item/local_group_membership";
   
  //begin add by txbv34 for IKCBSMMCPPRC-1427
    public static final String MIME_EMAIL_ADDRESS = "vnd.android.cursor.item/email_v2";
    public static final String MIME_PHONE_ADDRESS = "vnd.android.cursor.item/phone_v2";
    /**
     * Matched data table in aggregated group
     */
    private static final String ExactMatchTable = "matcheddata";
    /**
     * Group title in matched data table in certain aggregated group
     */
    private static final String ExactGroupTitle = "matched_title";

    /**
     * Intent defines extension constants to {@link android.content.Intent}.
     */
    public static class GroupIntents {
        /**
         * A constant string binds to email/phone confirmative activity, used
         * with {@link android.content.Intent#action} to support to confirm
         * email/phone.
         */
        public static final String ACTION_CONFIRM_GROUP = "com.motorola.contacts.ACTION_CONFIRM_GROUP";

        public static final class ConfirmGroup {
            public static final String INTENTEXTRA_AGG_GROUP_NAME = "INTENTEXTRA_AGG_GROUP_NAME";
            public static final String INTENTEXTRA_AGG_GROUP_LOOKUP_KEY = "INTENTEXTRA_AGG_GROUP_LOOKUP_KEY";
            // type
            public static final String INTENTEXTRA_CONFIRM_TYPE_EVENT = ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE;
            public static final String INTENTEXTRA_CONFIRM_TYPE_EMAIL = ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE;
            public static final String INTENTEXTRA_CONFIRM_TYPE_SMS = "vnd.android.cursor.item/sms-address";

            // confirmed data list
            public static final String INTENTEXTRA_STRING_ARRAY_CONFIRMED_DATALIST = "EXTRA_STRING_ARRAY_CONFIRMED_DATALIST";
        }



        /**
         * A constant string binds to save number/email to a group activity,
         * used with {@link android.content.Intent#action} to support to save
         * numbers/emails to group.
         */
        public static final String ACTION_SAVING_GROUP = "com.motorola.contacts.ACTION_SAVING_GROUP";

        public static final class SavingGroup {
            public static final String EXTRA_GROUP_NAME = "EXTRA_GROUP_NAME";
            public static final String EXTRA_BUNDLE_PHONE2NAME = "EXTRA_BUNDLE_PHONE2NAME";
            public static final String EXTRA_BUNDLE_EMAIL2NAME = "EXTRA_BUNDLE_EMAIL2NAME";
            public static final String EXTRA_LONG_ARRAY_DATAIDS = "EXTRA_LONG_ARRAY_DATAIDS";

            public static final String ACTION_GROUPS_QUICKTASK_SAVE_GROUP = "com.motorola.contacts.QUICKTASK_SAVE_GROUPS";
            public static final String EXTRA_GROUP_QUICKTASK_EMAIL_ADDRESSES = "com.motorola.contacts.QUICKGROUP.EMAIL_ADDRESSES";
            public static final String EXTRA_GROUP_QUICKTASK_GROUP_TITLE = EXTRA_GROUP_NAME;
        }
    }

    /**
     * !!! WARNING, this is a VERY expensive operation, do not call it directly
     * in UI/Main thread !!! Get group cursor with exact matched email addresses
     * or phone numbers String array
     *
     * @param context
     * @param searchParameters
     *            - String array of combined email addresses and phone numbers
     * @return local group name array
     *
     * @see Test Sample code for queryMyGroupsByExactContactInfos: String[]
     *      contactInfo = {"#3282","a19284@motorola.com"}; String[] groupname =
     *      GroupAPI.queryMyGroupsByExactContactInfos(contactInfo,0);
     */
    public static String[] queryMyGroupsByExactContactInfos(Context context,
            String[] searchParameters) {

        StringBuffer sb_search_par = new StringBuffer();
        for (int i = 0; i < searchParameters.length; i++) {
            if (i != 0) {
                sb_search_par.append(",");
            }
            sb_search_par.append(DatabaseUtils
                    .sqlEscapeString(searchParameters[i].toLowerCase()));
        }

        StringBuffer sb = new StringBuffer();
        sb.append("LOWER(" + ExactMatchTable + "." + ExactGroupTitle + ")");
        sb.append(" IN (");
        sb.append(sb_search_par);
        sb.append(")");

        // group title <-> matched email or phone
        HashMap<String, ArrayList<String>> groupTitle2Members = new HashMap<String, ArrayList<String>>();
        String groupTitle = null;
        String phone_email = null;
        Cursor c = context.getContentResolver().query(
                Uri.withAppendedPath(ContactsContract.AUTHORITY_URI,
                        "agg_groups_search"),
                new String[] { Groups.TITLE, Data.MIMETYPE, "matched_title" },
                sb.toString(), null, Groups.TITLE);

        if (c != null) {
            try {
                while (c.moveToNext()) {
                    Log.e("a21740",
                            "Group name:" + c.getString(0) + "," + "mimetype="
                                    + c.getString(1) + "," + "data1="
                                    + c.getString(2));

                    groupTitle = c.getString(0);
                    phone_email = c.getString(2);

                    if (groupTitle2Members.containsKey(groupTitle)) {
                        ArrayList<String> list = groupTitle2Members
                                .get(groupTitle);
                        list.add(phone_email);
                    } else {
                        ArrayList<String> list = new ArrayList<String>();
                        list.add(phone_email);
                        groupTitle2Members.put(groupTitle, list);
                    }
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        // clean up the unmatched groups
        {
            int size = groupTitle2Members.keySet().size();
            if (size == 0) {
                return new String[0];
            } else {
                String[] groupTitleArray = new String[size];

                groupTitleArray = groupTitle2Members.keySet().toArray(
                        groupTitleArray);

                for (int j = 0; j < searchParameters.length; j++) {
                    for (int loop = 0; loop < groupTitleArray.length; loop++) {

                        if (groupTitle2Members.containsKey(groupTitleArray[loop])
                                && !groupTitle2Members.get(groupTitleArray[loop])
                                .contains(searchParameters[j])) {
                            groupTitle2Members.remove(groupTitleArray[loop]);
                        }
                    }
                }
            }
        }

        // after cleanup, all remaining group are the group that contain all the
        // strings
        {
            int size = groupTitle2Members.keySet().size();
            if (size == 0) {
                return new String[0];
            }else{

                String[] groupTitleArray = new String[size];
                groupTitleArray = groupTitle2Members.keySet().toArray(
                        groupTitleArray);
                return groupTitleArray;
            }
        }
    }

    /**
     * Group name is not consistent, it might be changed. If the
     * caller needs to persist the group, it shall retrieve the group lookup
     * key. And later, loopup key can be used to retrieve the original group
     * even its name is changed underline.
     *
     * @param context
     * @param titlearray
     *            - String array of group title
     * @return local group lookup key array
     *
     * @see Test Sample code for queryMyGroupsByExactContactInfos: String[]
     *      grouparray = {"group1","group2"}; String[] retCur =
     *      GroupAPI.queryLookupKeyByGroupName(context, grouparray);
     */
    public static String[] queryLookupKeyByGroupName(Context context,
            String titlearray[]) {

        if (titlearray == null || titlearray.length == 0) {
            return null;
        }

        // get ids string, it shall be like "1,2,3,4,5"
        StringBuffer sb_search_par = new StringBuffer();
        int j = -1;
        for (int i = 0; i < titlearray.length; i++) {

            if (titlearray[i] == null || titlearray[i].length() == 0) {
                continue;
            } else {
                j++;
            }
            if (j != 0) {
                sb_search_par.append(",");
            }

            sb_search_par.append(DatabaseUtils.sqlEscapeString(titlearray[i]));
        }

        // get all groups in the ids above.
        Cursor c = context.getContentResolver().query(Groups.CONTENT_URI,
                new String[] { Groups.TITLE, Groups._ID },
                Groups.DELETED + "=0" +  " AND " + Groups.TITLE + " IN (" + sb_search_par + ")", null, Groups.TITLE);
        // get title<-> all group id of the title
        HashMap<String, ArrayList<Integer>> groupTitle2Members = new HashMap<String, ArrayList<Integer>>();
        String groupTitle = null;
        int groupId = 0;

        try {
            while (c.moveToNext()) {

                groupTitle = c.getString(0);
                groupId = c.getInt(1);

                if (groupTitle2Members.containsKey(groupTitle)) {
                    ArrayList<Integer> list = groupTitle2Members.get(groupTitle);
                    list.add(groupId);
                } else {
                    ArrayList<Integer> list = new ArrayList<Integer>();
                    list.add(groupId);
                    groupTitle2Members.put(groupTitle, list);
                }
            }
        } finally {

            c.close();
        }

        String[] loopupArray = new String[titlearray.length];
        int loop = 0;
        for (String o : titlearray) {
            loopupArray[loop++] = generateGroupLoopupKey(groupTitle2Members
                    .get(o));
        }

        return loopupArray;
    }

    // private api to get group lookup key
    private static String generateGroupLoopupKey(ArrayList<Integer> groupIdArray) {
        StringBuffer keybd = new StringBuffer();

        if (groupIdArray == null || groupIdArray.size() < 1) {
            return "";
        } else {
            for (int i = 0; i < groupIdArray.size(); i++) {
                if (i != 0) {
                    keybd.append(".");
                }
                keybd.append(groupIdArray.get(i));
            }
        }

        return keybd.toString();
    }
}
