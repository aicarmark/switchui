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

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;
import android.provider.ContactsContract.Groups;

/**
 * Group meta-data loader. Loads all groups or just a single group from the
 * database (if given a {@link Uri}).
 */
public final class LocalGroupMetaDataLoader extends CursorLoader {

    private final static String[] COLUMNS = new String[] {
        Groups.ACCOUNT_NAME,    // 0
        Groups.ACCOUNT_TYPE,    // 1
        Groups.DATA_SET,        // 2
        Groups._ID,             // 3
        Groups.TITLE,           // 4
        Groups.AUTO_ADD,        // 5
        Groups.FAVORITES,       // 6
        Groups.GROUP_IS_READ_ONLY,  // 7
        Groups.DELETED,             // 8
    };

    public final static int ACCOUNT_NAME = 0;
    public final static int ACCOUNT_TYPE = 1;
    public final static int DATA_SET = 2;
    public final static int GROUP_ID = 3;
    public final static int TITLE = 4;
    public final static int AUTO_ADD = 5;
    public final static int FAVORITES = 6;
    public final static int IS_READ_ONLY = 7;
    public final static int DELETED = 8;

    public LocalGroupMetaDataLoader(Context context, String groupTitle) {
        super(context, Groups.CONTENT_URI, COLUMNS, Groups.DELETED + "=0 AND "
                + Groups.TITLE + "=?", new String[]{groupTitle}, null);
    }

}
