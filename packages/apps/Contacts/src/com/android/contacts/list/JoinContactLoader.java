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
 * limitations under the License.
 */
package com.android.contacts.list;

import com.android.contacts.ContactsUtils;
import com.android.contacts.model.HardCodedSources;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

/**
 * A specialized loader for the Join Contacts UI.  It executes two queries:
 * join suggestions and (optionally) the full contact list.
 */
public class JoinContactLoader extends CursorLoader {

    private String[] mProjection;
    private Uri mSuggestionUri;
    private MatrixCursor mSuggestionsCursor;

    public JoinContactLoader(Context context) {
        super(context, null, null, null, null, null);
    }

    public void setSuggestionUri(Uri uri) {
        this.mSuggestionUri = uri;
    }

    @Override
    public void setProjection(String[] projection) {
        super.setProjection(projection);
        this.mProjection = projection;
    }

    public Cursor getSuggestionsCursor() {
        return mSuggestionsCursor;
    }

    @Override
    public Cursor loadInBackground() {
        // First execute the suggestions query, then call super.loadInBackground
        // to load the entire list
        mSuggestionsCursor = loadSuggestions();
        return super.loadInBackground();
    }

    /**
     * Loads join suggestions into a MatrixCursor.
     */
    private MatrixCursor loadSuggestions() {
        Cursor cursor = getContext().getContentResolver().query(mSuggestionUri, mProjection,
                null, null, null);
        try {
            MatrixCursor matrix = new MatrixCursor(mProjection);
            Object[] row = new Object[mProjection.length];
            ContentResolver cr = getContext().getContentResolver();
            long nameRawContactId;
            Account account;
            while (cursor.moveToNext()) {
                // MOTOROLA: Hardcode here because we know the project is PROJECTION_CONTACT, 0 is Contacts._ID
                nameRawContactId = ContactsUtils.queryForNameRawContactId(cr, cursor.getLong(0));
                account = ContactsUtils.getAccountbyRawContactId(cr, nameRawContactId);
                if (account != null && HardCodedSources.ACCOUNT_TYPE_CARD.equals(account.type)) {
                    // Filter out SIM contacts from suggestions
                    continue;
                }
                // End MOTOROLA
                for (int i = 0; i < row.length; i++) {
                    row[i] = cursor.getString(i);
                }
                matrix.addRow(row);
            }
            return matrix;
        } finally {
            cursor.close();
        }
    }
}
