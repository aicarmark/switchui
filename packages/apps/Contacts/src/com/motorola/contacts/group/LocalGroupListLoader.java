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

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract;

/**
 * Group loader for the group list that includes details such as the number of contacts per group
 * and number of groups per account. This list is sorted by account type, account name, where the
 * group names are in alphabetical order. Note that the list excludes default, favorite, and deleted
 * groups.
 */
public final class LocalGroupListLoader extends CursorLoader {

    private final static String[] COLUMNS = new String[] {
        Groups.TITLE,
        Groups.SUMMARY_COUNT,
    };

    public final static int TITLE = 0;
    public final static int MEMBER_COUNT = 1;

    private static final Uri LOCAL_GROUP_LIST_URI = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "agg_groups_summary");

    public LocalGroupListLoader(Context context) {
        this(context, null);
    }
//MOTO MOD BEGIN IKPIM-1044 support group list search
    public LocalGroupListLoader(Context context, String queryString) {
        super(context, LOCAL_GROUP_LIST_URI, COLUMNS,
                    queryString != null ? (Groups.DELETED + "=0 AND " + Groups.TITLE + " LIKE ?") : (Groups.DELETED + "=0"),
                    queryString != null ? (new String[] { "%" + queryString + "%" }) : null,
                    Groups.TITLE + " COLLATE LOCALIZED ASC");
   }
//MOTO MOD END IKPIM-1044 support group list search
}
