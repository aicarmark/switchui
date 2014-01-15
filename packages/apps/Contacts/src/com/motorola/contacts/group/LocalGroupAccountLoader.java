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
import android.provider.ContactsContract.RawContacts;

/**
 * Group membership contact Id loader.
 * (if given a {@link Uri}).
 */

public final class LocalGroupAccountLoader extends CursorLoader {

    private final static String[] COLUMNS = new String[] {
        RawContacts.ACCOUNT_TYPE,
        RawContacts.ACCOUNT_NAME,
        RawContacts.DATA_SET
    };

    public final static int RAWCONTACT_ACCOUNT_TYPE_COLUMN_INDEX = 0;
    public final static int RAWCONTACT_ACCOUNT_NAME_COLUMN_INDEX = 1;
    public final static int RAWCONTACT_DATASET_COLUMN_INDEX = 2;
/**SWITCHUITWOV-308 JB update : " fix edit group issue" by bphx43 2012-11-12*/
    public final static String SELECT_ACCOUNT_INFO =
        " _id IN ( SELECT "
        + RawContacts._ID + " FROM view_raw_contacts"
        + " WHERE " + RawContacts.CONTACT_ID + " IN (%s)" + " GROUP BY "
        + RawContacts.ACCOUNT_TYPE + "," + RawContacts.ACCOUNT_NAME + ","
        + RawContacts.DATA_SET + ")";
/**end*/
    public LocalGroupAccountLoader(Context context, String args) {
        super(context, RawContacts.CONTENT_URI, COLUMNS,
                String.format(SELECT_ACCOUNT_INFO, args), null, null);

    }

}
