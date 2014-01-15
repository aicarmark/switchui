/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.motorola.mmsp.socialGraph.socialGraphWidget.model;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.motorola.mmsp.socialGraph.R;
import com.motorola.mmsp.socialGraph.socialGraphWidget.common.ArrayListCursor;

/**
 * This adapter is used to filter contacts on both name and number.
 */
public class RecipientsSinglePickerAdapter extends ResourceCursorAdapter {

	public static final int ID_INDEX = 0;
    public static final int RAW_CONTACT_ID_INDEX = 1;
    public static final int NAME_INDEX       = 2;
    public static final int PHOTOID_INDEX      = 3;
    public static final int PHOTOURL_INDEX      = 4;
    private static final String[] PROJECTION_CONTACT_FILTER = {
        Phone._ID,                  // 0
        BaseColumns._ID, 	     	// 1
        Phone.DISPLAY_NAME,         // 2
        Contacts.PHOTO_ID,                      // 3
        Contacts.PHOTO_THUMBNAIL_URI,           // 4
    };    

    private static final String SORT_ORDER = Contacts.SORT_KEY_PRIMARY + " ASC";
    private final ContentResolver mContentResolver;

    private static final String TAG = "SocialGraphWidget";
    private ArrayList<Integer> mSelectedContactIDs;
    private Context mContext = null;
    private ArrayList<Integer> mContactIds = null;
    private StringBuffer mSelectedContactsString = null;
    private ContactPhotoManager mPhotoLoader;
	public RecipientsSinglePickerAdapter(Context context, ArrayList<Integer> selectedContactIDs) {
		// Note that the RecipientsAdapter doesn't support auto-requeries. If we
		// want to respond to changes in the contacts we're displaying in the
		// drop-down,
		// code using this adapter would have to add a line such as:
		// mRecipientsAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
		// See ComposeMessageActivity for an example.
		super(context, R.layout.single_recipient_filter_item, null, false /*
																	 * no
																	 * auto-requery
																	 */);
		mContext = context;
		mContentResolver = context.getContentResolver();
		mSelectedContactIDs = selectedContactIDs;
		mPhotoLoader =ContactPhotoManager.getInstance(mContext);

	}

    @Override
    public final void bindView(View view, Context context, Cursor cursor) {
    	String nameValue = cursor.getString(NAME_INDEX);
    	//String photoValue = cursor.getString(PHOTO_INDEX);
		if (nameValue == null) {
			nameValue = mContext.getString(android.R.string.unknownName);
		}
    	TextView name = (TextView) view.findViewById(R.id.name);
    	ImageView photo=(ImageView)view.findViewById(R.id.photo);
        name.setText(nameValue);
        bindPhoto(photo,cursor);
       // mPhotoLoader.loadPhoto(photo, Uri.parse(photoValue) , false, false) ;
    }
    
    protected void bindPhoto(final ImageView view,  Cursor cursor) {

        // Set the photo, if available
        long photoId = 0;
        if (!cursor.isNull(PHOTOID_INDEX)) {
            photoId = cursor.getLong(PHOTOID_INDEX);
        }

        if (photoId != 0) {
        	mPhotoLoader.loadPhoto(view, photoId, false, false);
        } else {
            final String photoUriString = cursor.getString(PHOTOURL_INDEX);
            final Uri photoUri = photoUriString == null ? null : Uri.parse(photoUriString);
            mPhotoLoader.loadPhoto(view, photoUri, false, false);
        }
    }
    
    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        String cons = null;
        Uri uri;
        Cursor resultCursor = null;

        //get black list from setting.
		if (mContactIds == null) {
			
			mContactIds = getContacts();

			for (int i = 0; mSelectedContactIDs != null
					&& i < mSelectedContactIDs.size(); i++) {
				Integer selectedId = mSelectedContactIDs.get(i);
				Log.d(TAG, "selected id =" + selectedId);
				if (mContactIds.contains(selectedId)) {
					Log.d(TAG, "remove selectedId id =" + selectedId);
					mContactIds.remove(selectedId);
				}
			}
			
			mSelectedContactsString = new StringBuffer();

			for (int i = 0; mSelectedContactIDs != null
					&& i < mSelectedContactIDs.size(); i++) {
				if (i != 0) {
					mSelectedContactsString.append(",");
				}
				mSelectedContactsString.append(mSelectedContactIDs.get(i));
			}

		}
        
        
        //get person informations from the database with these people.
        if (constraint != null) {
            cons = constraint.toString(); 
            uri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(cons));
        } else {
        	uri = Contacts.CONTENT_URI;
        }
        Cursor phoneCursor=null;
        try {
         phoneCursor =
            mContentResolver.query(uri,
            		PROJECTION_CONTACT_FILTER,
            		String.format("%s not in (%s)", BaseColumns._ID, mSelectedContactsString),
                    null,
                    SORT_ORDER);
		
        ArrayList<ArrayList> rows = new ArrayList<ArrayList>();
        ArrayList<Object> item = new ArrayList<Object>();
		item.add(-1);
		item.add(-1);
		item.add(mContext.getString(R.string.none_contact_pick));
		item.add(-1);
		item.add(null);
		rows.add(item);		
        Cursor noneCursor = new ArrayListCursor(PROJECTION_CONTACT_FILTER, rows);
        MergeCursor mergeCursor = new MergeCursor (new Cursor[] {noneCursor, phoneCursor});

        resultCursor = mergeCursor;
		} catch (Exception e) {
			e.printStackTrace();
		} 
        return resultCursor;
    }    
    
    private ArrayList<Integer> getContacts() {
		ArrayList<Integer> ContactsList = new ArrayList<Integer>();
		Uri uri = ContactsContract.Contacts.CONTENT_URI;
		Cursor cursor = null;
		try {
			cursor = mContentResolver.query(uri,
					new String[] { ContactsContract.Contacts._ID }, null, null,
					null);
			
			if ((cursor != null) && cursor.moveToFirst()) {
				for (; !cursor.isAfterLast(); cursor.moveToNext()) {
					int contactId = cursor.getInt(0);
					ContactsList.add(contactId);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ContactsList;
	}
}
