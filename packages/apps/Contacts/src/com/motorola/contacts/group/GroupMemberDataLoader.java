/*
 * Copyright (C) 2011 The Android Open Source Project
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
 * limitations under the License
 */
package com.motorola.contacts.group;

import com.android.contacts.list.ContactListAdapter;
import com.android.contacts.preference.ContactsPreferences;

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.Groups;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.motorola.contacts.groups.GroupAPI;

/**
 * Group Member data loader. Loads all group member data from the given contactIds
 */
public final class GroupMemberDataLoader extends CursorLoader {

    /**
     * Projection map is taken from {@link ContactListAdapter}
     */
    private final String[] PROJECTION_DATA = new String[] {
        // TODO: Pull Projection_data out into util class
        Data.RAW_CONTACT_ID,                    // 0
        Data.MIMETYPE,                          // 1
        Data.DATA1,                             // 2
        Data.DATA2,                             // 3
        Data.DATA3,                             // 4
        Data.CONTACT_ID,                        // 5
        Data.DISPLAY_NAME,                      // 6
        Data.LOOKUP_KEY,                        // 7
        Data._ID,                               // 8
        Data.PHOTO_URI,                         // 9
    };

    private String mContactIds = "";
    private final int mSelectionType;
    private final boolean mGroupBy;
    private String mGroupTitle = "";

    public static final int RAW_CONTACT_ID_COLUMN_INDEX = 0;
    public static final int MIMETYPE_ID_COLUMN_INDEX = 1;
    public static final int DATA1_COLUMN_INDEX = 2;
    public static final int DATA2_COLUMN_INDEX = 3;
    public static final int DATA3_COLUMN_INDEX = 4;
    public static final int CONTACT_ID_COLUMN_INDEX = 5;
    public static final int DISPLAY_NAME_COLUMN_INDEX = 6;
    public static final int LOOKUP_KEY_COLUMN_INDEX = 7;
    public static final int ID_COLUMN_INDEX = 8;
    public static final int PHOTO_URI_COLUMN_INDEX = 9;

    public static final int SELECTION_TYP_BOTH  = 1;
    public static final int SELECTION_TYP_EMAIL = 2;
    public static final int SELECTION_TYP_PHONE = 3;

    // this returns all the contact ids for the given group title
    public final static String SELECT_GROUP_MEMBER_CONTACT_IDS =
        " SELECT " + RawContacts.CONTACT_ID
        + " FROM raw_contacts"
        + " WHERE " + RawContacts.DELETED + "<> 1"
        + " AND " + RawContacts._ID + " IN "
            + "( SELECT " + Data.RAW_CONTACT_ID
            + " FROM " + "data JOIN mimetypes ON (data.mimetype_id = mimetypes._id)"
            + " WHERE (" + Data.MIMETYPE + "='" + GroupMembership.CONTENT_ITEM_TYPE + "'"
            + " OR " + Data.MIMETYPE + "='" + GroupAPI.LOCAL_GROUP_MIMETYPE + "')" // MOT MOD - IKHSS6-1470
            + " AND " + GroupMembership.GROUP_ROW_ID + " IN "
                + "( SELECT groups." + Groups._ID
                + " FROM groups"
                + " WHERE " + Groups.TITLE + "=?"
                + " AND " + Groups.DELETED + "<>1" + " ) )";

    // this returns all the contact ids with given type(email or number)
    public final static String SELECT_GROUP_MEMBER_WITH_DATA =
         " SELECT DISTINCT " + RawContacts.CONTACT_ID
         + " FROM raw_contacts, data, mimetypes WHERE (data.raw_contact_id = raw_contacts._id)"
         + " AND (data.mimetype_id = mimetypes._id) AND(" + Data.MIMETYPE + "=? OR " + Data.MIMETYPE + "=?)"
         + " AND " + RawContacts.CONTACT_ID + " IN (" + SELECT_GROUP_MEMBER_CONTACT_IDS
         + ") ";

    public final static String WHERE_GROUP_MEMBER_WITH_DATA =
         "(" + Data.MIMETYPE + "=? OR " + Data.MIMETYPE + "=?)"
         + " AND " + Data.CONTACT_ID + " IN (" + SELECT_GROUP_MEMBER_CONTACT_IDS + ") ";

    public final static String WHERE_GROUP_MEMBER_NO_DATA =
         Data.MIMETYPE + " = '" +StructuredName.CONTENT_ITEM_TYPE +"' AND "
         + Data.CONTACT_ID + " IN (" + SELECT_GROUP_MEMBER_CONTACT_IDS + ") "
         + " AND " + Data.CONTACT_ID + " NOT IN (" + SELECT_GROUP_MEMBER_WITH_DATA + ") ";

    public GroupMemberDataLoader(Context context, String groupTitle, int selectionType, boolean groupBy, boolean hasData) {
        super(context);

        if(TextUtils.isEmpty(groupTitle)) {
            mGroupTitle = "";
        } else {
            mGroupTitle = groupTitle;
        }
        mSelectionType = selectionType;
        mGroupBy = groupBy;
        setUri(createUri());
        setProjection(PROJECTION_DATA);
        setSelection(createSelection(hasData));
        setSelectionArgs(createSelectionArgs(hasData));
        setSortOrder(Contacts.SORT_KEY_PRIMARY);
    }


    public GroupMemberDataLoader(Context context, String contactIds, int selectionType, boolean groupBy) {
        super(context);

        if(TextUtils.isEmpty(contactIds)) {
            mContactIds = "()";
        } else {
            mContactIds = contactIds;
        }
        mSelectionType = selectionType;
        mGroupBy = groupBy;
        setUri(createUri());
        setProjection(PROJECTION_DATA);
        setSelection(createSelection());
        setSelectionArgs(createSelectionArgs());
        setSortOrder(Contacts.SORT_KEY_PRIMARY);
    }

    private Uri createUri() {
        Uri uri = Data.CONTENT_URI;
        uri = uri.buildUpon().appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                String.valueOf(Directory.DEFAULT)).build();
        if(mGroupBy) {
            uri = uri.buildUpon().appendQueryParameter("GROUP_BY_PRIMARY_KEY", "contact_id").build();
        }
        return uri;
    }

    private String createSelection(boolean hasData) {
        StringBuilder selection = new StringBuilder();
        String whereClause;
        if(hasData) {
            whereClause = WHERE_GROUP_MEMBER_WITH_DATA;
        } else {
            whereClause = WHERE_GROUP_MEMBER_NO_DATA;
        }
        selection.append(whereClause);
        return selection.toString();
    }

    private String createSelection() {
        StringBuilder selection = new StringBuilder();
        String whereClause = "(" + Data.MIMETYPE + "=? OR " + Data.MIMETYPE + "=?)"
                              + " AND " + Data.CONTACT_ID + " IN " + mContactIds;
        selection.append(whereClause);
        return selection.toString();
    }

    private String[] createSelectionArgs() {
        List<String> selectionArgs = new ArrayList<String>();
        switch(mSelectionType) {
          case SELECTION_TYP_BOTH: {// both email and phone
              selectionArgs.add(Phone.CONTENT_ITEM_TYPE);
              selectionArgs.add(Email.CONTENT_ITEM_TYPE);
              break;
              }
          case SELECTION_TYP_EMAIL: {// email only
              selectionArgs.add(Email.CONTENT_ITEM_TYPE);
              selectionArgs.add(Email.CONTENT_ITEM_TYPE);
              break;
              }
          case SELECTION_TYP_PHONE: {// phone only
              selectionArgs.add(Phone.CONTENT_ITEM_TYPE);
              selectionArgs.add(Phone.CONTENT_ITEM_TYPE);
              break;
              }
          default: break;
          }

        return selectionArgs.toArray(new String[0]);
    }

    private String[] createSelectionArgs(boolean hasData) {
        List<String> selectionArgs = new ArrayList<String>();
        if(!hasData) {
            selectionArgs.add(mGroupTitle);
        }
        switch(mSelectionType) {
          case SELECTION_TYP_BOTH: {// both email and phone
              selectionArgs.add(Phone.CONTENT_ITEM_TYPE);
              selectionArgs.add(Email.CONTENT_ITEM_TYPE);
              break;
              }
          case SELECTION_TYP_EMAIL: {// email only
              selectionArgs.add(Email.CONTENT_ITEM_TYPE);
              selectionArgs.add(Email.CONTENT_ITEM_TYPE);
              break;
              }
          case SELECTION_TYP_PHONE: {// phone only
              selectionArgs.add(Phone.CONTENT_ITEM_TYPE);
              selectionArgs.add(Phone.CONTENT_ITEM_TYPE);
              break;
              }
          default: break;
          }
        selectionArgs.add(mGroupTitle);

        return selectionArgs.toArray(new String[0]);
    }

}
