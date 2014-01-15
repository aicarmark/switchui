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

package com.android.contacts.model;

import com.android.contacts.R;
import android.content.Context;

import com.android.contacts.model.AccountType.DefinitionException;
import com.android.contacts.model.EntityDelta.ValuesDelta;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts.Data;
import android.util.Log;


public class LocalAccountType extends FallbackAccountType {

    public static final String ACCOUNT_TYPE = HardCodedSources.ACCOUNT_TYPE_LOCAL;
    public static final String LOCAL_CONTACTS_SYSTEM_GROUP = HardCodedSources.LOCAL_CONTACTS_SYSTEM_GROUP;
    public static final String TAG = "LocalAccountType";
    
    public LocalAccountType(Context context) {
        super(context);
        this.accountType = ACCOUNT_TYPE;
        this.titleRes = R.string.account_local;
        this.iconRes = R.drawable.ic_contact_picture_local;
                
        try {            
            addDataKindGroupMembership(context);
            
            mIsInitialized = true;
        } catch (DefinitionException e) {
            Log.e(TAG, "Problem building local account type", e);
        }        
    }
/*
    @Override
    protected void inflate(Context context, int inflateLevel) {
        super.inflate(context, inflateLevel);
    }
*/
    public static final void attemptMyContactsMembership(EntityDelta state, Context context) {
        final ValuesDelta stateValues = state.getValues();
	      stateValues.setFromTemplate(true);
        final String accountName = stateValues.getAsString(RawContacts.ACCOUNT_NAME);
        final String accountType = stateValues.getAsString(RawContacts.ACCOUNT_TYPE);
                    
        final ContentResolver resolver = context.getContentResolver();

        long groupId = -1;
        Cursor groupCursor = null;

        try {
            groupCursor = resolver.query(Groups.CONTENT_URI,
                    new String[] {Groups._ID},
                    Groups.TITLE + " =? AND " + Groups.ACCOUNT_NAME + " =? AND " + Groups.ACCOUNT_TYPE + " =?",
                    new String[] {LOCAL_CONTACTS_SYSTEM_GROUP, accountName, accountType}, null);
            if (groupCursor != null && groupCursor.moveToFirst()) {
                groupId = groupCursor.getLong(0);
            }
        } finally {
            if (groupCursor != null) {
                groupCursor.close();
            }
        }
        
        if (groupId < 0)   // not exist
            return;
        Log.v(TAG, "groupId = " + groupId);
                        
        final ContentValues values = new ContentValues();
        values.put(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
        values.put(GroupMembership.GROUP_ROW_ID, groupId);
            
        if (state != null) {
            state.addEntry(ValuesDelta.fromAfter(values));
        }
    }

    @Override
    public boolean isGroupMembershipEditable() {
        return true;
    }

    @Override
    public boolean areContactsWritable() {
        return true;
    }

}
