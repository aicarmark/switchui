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

import com.android.contacts.preference.ContactsPreferences;

import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.SearchSnippetColumns;
import com.android.contacts.util.PhoneCapabilityTester;


import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.accounts.Account;
import java.util.Iterator;


public class MulContactListAdapter extends DefaultContactListAdapter {

    private static final boolean DEBUG = true;
	private static final String TAG = "MulContactListAdapter";
	private ArrayList<Account> mIncludeAccount = null;   // MOT CHINA
    private ArrayList<Account> mExcludeAccount = null;   // MOT CHINA
    private ArrayList<String> mExcludeId = null;   //mtdr83 for IKCBSMMCPPRC-1310 

    public MulContactListAdapter(Context context) {
        super(context);
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {
        super.configureLoader(loader, directoryId);
        if (mIncludeAccount == null && mExcludeAccount == null) {
            return;
        }
        if (isSearchMode()) {
            String query = getQueryString();
            if (query == null) {
                query = "";
            }
            query = query.trim();
            if (TextUtils.isEmpty(query)) {
                // Regardless of the directory, we don't want anything returned,
                // so let's just send a "nothing" query to the local directory.
                loader.setUri(Contacts.CONTENT_URI);
                loader.setProjection(getProjection(false));
                loader.setSelection("0");
            } else {
                Builder builder = Contacts.CONTENT_FILTER_URI.buildUpon();
                builder.appendPath(query);      // Builder will encode the query
                builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                        String.valueOf(directoryId));
                if (directoryId != Directory.DEFAULT && directoryId != Directory.LOCAL_INVISIBLE) {
                    builder.appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY,
                            String.valueOf(getDirectoryResultLimit()));
                }
                builder.appendQueryParameter(SearchSnippetColumns.SNIPPET_ARGS_PARAM_KEY,
                        SNIPPET_ARGS);
                builder.appendQueryParameter(SearchSnippetColumns.DEFERRED_SNIPPETING_KEY,"1");
                loader.setUri(builder.build());
                loader.setProjection(getProjection(true));
                if(mExcludeAccount !=null && mExcludeAccount.size() >0){
                    int i = 0;
                    int size = mExcludeAccount.size();
                    StringBuilder selection = new StringBuilder();
                    List<String> selectionArgs = new ArrayList<String>();
                    if(!TextUtils.isEmpty(loader.getSelection())){
                        selection.append(loader.getSelection());
                        selection.append(" AND ");
                    }
					/**modify for 	SWITCHUITWO-45 by bphx43 2012-11-20*/
                    selection.append(
                        Contacts._ID + " IN ("
                        + "SELECT DISTINCT " + RawContacts.CONTACT_ID
                        + " FROM view_raw_contacts" + " WHERE " );
					/**end*/
                    Iterator<Account> it = mExcludeAccount.iterator();
                    while(it.hasNext())
                    {
                        Account account = (Account)it.next();
                        selection.append(
                            RawContacts.ACCOUNT_TYPE + "!=?"
                            + " AND " + RawContacts.ACCOUNT_NAME + "!=?");
                        if (i < size - 1) {
                            selection.append(" AND ");
                        }
                        selectionArgs.add(account.type);
                        selectionArgs.add(account.name);
                        i++;
                    }
                    selection.append(")");
                    loader.setSelection(selection.toString());
                    loader.setSelectionArgs(selectionArgs.toArray(new String[0]));
                }
            }
            return;
        }
        ContactListFilter filter = getFilter();
        /*2013-1-23, add by amt_sunzhao for SWITCHUITWO-542 */ 
        //if (filter != null && filter.filterType == ContactListFilter.FILTER_TYPE_CUSTOM) {
            StringBuilder selection = new StringBuilder();
            List<String> selectionArgs = new ArrayList<String>();
            selection.append(loader.getSelection());
            final String[] oldSelectionArgs = loader.getSelectionArgs();
            if(null != oldSelectionArgs) {
            	final int count = oldSelectionArgs.length;
            	for(int i = 0; i < count; i++) {
            		selectionArgs.add(oldSelectionArgs[i]);
            	}
            }
            if (selection.length() > 0) {
                selection.append(" AND ");
            }
			/**modify for 	SWITCHUITWO-45 by bphx43 2012-11-20*/
            selection.append(
                        Contacts._ID + " IN ("
                        + "SELECT DISTINCT " + RawContacts.CONTACT_ID
                        + " FROM view_raw_contacts" + " WHERE " );
            /**end*/            
            if (mIncludeAccount != null) {
            	  int i = 0;
            	  int size = mIncludeAccount.size();
                Iterator<Account> it = mIncludeAccount.iterator();
                while(it.hasNext())
                {
                    Account account = (Account)it.next();
                    selection.append(
                            RawContacts.ACCOUNT_TYPE + "=?"
                            + " AND " + RawContacts.ACCOUNT_NAME + "=?");
                    if (i < size - 1) {
                        selection.append(" OR ");
                    }
                    selectionArgs.add(account.type);
                    selectionArgs.add(account.name);
                    i++;
                }
            }            
            else if (mExcludeAccount != null) {
            	  int i = 0;
            	  int size = mExcludeAccount.size();
                Iterator<Account> it = mExcludeAccount.iterator();
                while(it.hasNext())
                {
                    Account account = (Account)it.next();
                    selection.append(
                            RawContacts.ACCOUNT_TYPE + "!=?"
                            + " AND " + RawContacts.ACCOUNT_NAME + "!=?");
                    if (i < size - 1) {
                        selection.append(" AND ");
                    }
                    selectionArgs.add(account.type);
                    selectionArgs.add(account.name);
                    i++;
                }
            }
            
            //begin mtdr83 for IKCBSMMCPPRC-1310 
            if(mExcludeId != null){
            	
            	int i = 0;
          	    int size = mExcludeId.size();
            	Iterator<String> ids = mExcludeId.iterator();
            	while(ids.hasNext()){
            		if(ids.hasNext() && i == 0){
            			selection.append(" AND ");
            		}
            		String id = ids.next();
            		selection.append(RawContacts.CONTACT_ID + "!=?");
            		selectionArgs.add(id);
                    if (i < size - 1) {
                        selection.append(" AND ");
                    }
                    i++;
            	}
            }
            //end mtdr83 for IKCBSMMCPPRC-1310 
            selection.append(")");
            if(DEBUG)Log.d(TAG, "selection=" + selection + ",selectionArgs=" + selectionArgs);
            loader.setSelection(selection.toString());
            loader.setSelectionArgs(selectionArgs.toArray(new String[0]));
        //}
            /*2013-1-23, add end*/ 
    }


    public void setIncludeAccount(ArrayList<Account> includeAccount) {
        if (mIncludeAccount == null) {
            mIncludeAccount = new ArrayList<Account>();
        }
        mIncludeAccount.clear();
        mIncludeAccount.addAll(includeAccount);
    }

    public void setExcludeAccount(ArrayList<Account> excludeAccount) {
        if (mExcludeAccount == null) {
            mExcludeAccount = new ArrayList<Account>();
        }
        mExcludeAccount.clear();
        mExcludeAccount.addAll(excludeAccount);
    }

    //Begin mtdr83 for IKCBSMMCPPRC-1310 
	public void setExcludeId(ArrayList<String> mExcludeId2) {
		// TODO Auto-generated method stub
		if (mExcludeId == null) {
			mExcludeId = new ArrayList<String>();
        }
		mExcludeId.clear();
		mExcludeId.addAll(mExcludeId2);
	}
	//End mtdr83 for IKCBSMMCPPRC-1310 

}
