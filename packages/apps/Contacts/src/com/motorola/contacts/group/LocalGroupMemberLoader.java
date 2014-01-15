/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;

import com.motorola.contacts.groups.GroupAPI;

/**
 * Group membership loader.
 * (if given a {@link Uri}).
 */

public final class LocalGroupMemberLoader extends CursorLoader {

    public static class LocalGroupDetailMemberQuery {
        private final static String[] PROJECTION = new String[] {
            Contacts._ID,                    // 0
            Contacts.DISPLAY_NAME_PRIMARY,   // 1
            Contacts.LOOKUP_KEY,             // 2
            Contacts.PHOTO_URI,              // 3
            Contacts.PHOTO_ID,               // 4

            Contacts.STARRED,                // 5
            Contacts.CONTACT_STATUS,         // 6
            Contacts.CONTACT_PRESENCE        // 7
        };

        public final static int CONTACT_ID_COLUMN_INDEX = 0;
        public final static int CONTACT_DISPLAY_NAME_PRIMARY_COLUMN_INDEX = 1;
        public static final int CONTACT_LOOKUP_KEY_COLUMN_INDEX = 2;
        public static final int CONTACT_PHOTO_URI_COLUMN_INDEX = 3;
        public static final int CONTACT_PHOTO_ID_COLUMN_INDEX = 4;
        public static final int CONTACT_STARRED_COLUMN_INDEX = 5;
        public static final int CONTACT_STATUS_COLUMN_INDEX = 6;
        public static final int CONTACT_PRESENCE_STATUS_COLUMN_INDEX = 7;
    }

    public static class LocalGroupEditorMemberQuery {
        private final static String[] PROJECTION = new String[] {
            Contacts._ID,                    // 0
            Contacts.DISPLAY_NAME_PRIMARY,   // 1
            Contacts.LOOKUP_KEY,             // 2
            Contacts.PHOTO_URI,              // 4
        };

        public final static int CONTACT_ID_COLUMN_INDEX = 0;
        public final static int CONTACT_DISPLAY_NAME_PRIMARY_COLUMN_INDEX = 1;
        public static final int CONTACT_LOOKUP_KEY_COLUMN_INDEX = 2;
        public static final int CONTACT_PHOTO_URI_COLUMN_INDEX = 3;
    }

    private final String mGroupTitle;

    public final static String SELECT_EXISTING_MEMBER_CONTACT_IDS =
        " _id IN ( SELECT " + RawContacts.CONTACT_ID
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
                + " AND " + Groups.DELETED + "<>1" + " ) ) )";


    public final static String SELECT_EDITOR_MEMBER_CONTACT_IDS = " OR _id IN (%s)) AND ( _id NOT IN (%s))";

    /**
     * @return LocalGroupMemberLoader object used for existing member list.
     */
    public static LocalGroupMemberLoader constructLoaderForGroupDetailMemberQuery(
            Context context, String groupTitle) {
        return new LocalGroupMemberLoader(context, groupTitle,
                LocalGroupDetailMemberQuery.PROJECTION, SELECT_EXISTING_MEMBER_CONTACT_IDS);
    }

    /**
     * @return LocalGroupMemberLoader object used for editing member list.
     */
    public static LocalGroupMemberLoader constructLoaderForGroupEditorMemberQuery(
            Context context, String groupTitle, ArrayList<Long> addList, ArrayList<Long> removeList) {

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(SELECT_EXISTING_MEMBER_CONTACT_IDS);
        sb.append(SELECT_EDITOR_MEMBER_CONTACT_IDS);
        String str = sb.toString();

        String addListArgs = LocalGroupUtils.buildSelectionIdList(addList);
        String removeListArgs = LocalGroupUtils.buildSelectionIdList(removeList);

        String selection = String.format(str, addListArgs, removeListArgs);

        return new LocalGroupMemberLoader(context, groupTitle, LocalGroupEditorMemberQuery.PROJECTION,
                selection);
    }

    public LocalGroupMemberLoader(Context context, String groupTitle, String[] projection, String selection) {
        super(context);
        if (TextUtils.isEmpty(groupTitle)) {
            mGroupTitle = "";
        } else {
            mGroupTitle = groupTitle;
        }
        setUri(createUri());
        setProjection(projection);
        setSelection(selection);
        setSelectionArgs(createSelectionArgs());
        setSortOrder(Contacts.SORT_KEY_PRIMARY);
    }

    private Uri createUri() {
        Uri uri = Contacts.CONTENT_URI;
        return uri;
    }

    private String[] createSelectionArgs() {
        List<String> selectionArgs = new ArrayList<String>();
        selectionArgs.add(mGroupTitle);
        return selectionArgs.toArray(new String[0]);
    }

}
