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
import java.util.HashMap;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.motorola.mmsp.socialGraph.R;
import com.motorola.mmsp.socialGraph.socialGraphWidget.Setting;
import com.motorola.mmsp.socialGraph.socialGraphWidget.common.WaitDialog;

/**
 * This adapter is used to filter contacts on both name and number.
 */
public class RecipientsAdapter extends ResourceCursorAdapter {

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

    private final Context mContext;
    private final ContentResolver mContentResolver;
    private HashMap<Integer, Boolean> checkStatus = null;
    
    private ArrayList<Integer> mUnCheckedContacts = null;
    
	private StringBuffer mHiddenContactsString = null;
	private ArrayList<Integer> mHiddenContacts = null;
    private ContactPhotoManager mPhotoLoader;
    
    private Handler mHandler = null;
    private WaitDialog mWaitingDialog = null;

    public RecipientsAdapter(Context context, WaitDialog waitingDialog) {
        // Note that the RecipientsAdapter doesn't support auto-requeries. If we
        // want to respond to changes in the contacts we're displaying in the drop-down,
        // code using this adapter would have to add a line such as:
        //   mRecipientsAdapter.setOnDataSetChangedListener(mDataSetChangedListener);
        // See ComposeMessageActivity for an example.
        super(context, R.layout.recipient_filter_item, null, false /* no auto-requery */);
        mContext = context;
        mContentResolver = context.getContentResolver();        
		mPhotoLoader =ContactPhotoManager.getInstance(mContext);
		
		mWaitingDialog = waitingDialog;
		mHandler = new Handler(mContext.getMainLooper());
    }
    
    public RecipientsAdapter(Context context, ArrayList<Integer> unCheckedContacts, WaitDialog waitingDialog) {        
        super(context, R.layout.recipient_filter_item, null, false /* no auto-requery */);
        mContext = context;
        mContentResolver = context.getContentResolver();
        mUnCheckedContacts = unCheckedContacts;
		mPhotoLoader =ContactPhotoManager.getInstance(mContext);
		
		mWaitingDialog = waitingDialog;
		mHandler = new Handler(mContext.getMainLooper());
    }
    
    public void setMarked(ArrayList<Integer> checked) {
		if (checkStatus == null) {
			return;
		}
    	for (int i : checkStatus.keySet()) {
    		checkStatus.put(i, false);
    	}
    	for (int i : checked) {
    		checkStatus.put(i, true);
    	}
    }
    
	public void setMarked(int person, Boolean checked) {
		if (checkStatus != null) {
			checkStatus.put(person, checked);
		}
	}

    @Override
    public final void bindView(View view, Context context, Cursor cursor) {
    	String nameValue = cursor.getString(NAME_INDEX);
		if (nameValue == null) {
			nameValue = mContext.getString(android.R.string.unknownName);
		}
    	TextView name = (TextView) view.findViewById(R.id.name);
    	ImageView photo=(ImageView)view.findViewById(R.id.photo);
        name.setText(nameValue);
        bindPhoto(photo,cursor);

        final int rawContactValue = cursor.getInt(RAW_CONTACT_ID_INDEX);
        CheckBox check = (CheckBox)view.findViewById(R.id.checkbox);
        
		if (checkStatus != null) {
			check.setChecked(checkStatus.get(rawContactValue) != null ? checkStatus.get(rawContactValue) : false);
		}
        
        check.setFocusable(false);
        check.setClickable(false);
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
        if(mHiddenContacts == null){
        	mHiddenContacts = Setting.getInstance(mContext).getHideContacts();
        	
        	mHiddenContactsString = new StringBuffer();
        	
        	for (int i = 0; i < mHiddenContacts.size(); i++) {
            	if (i != 0) {
            		mHiddenContactsString.append(",");
            	}
            	mHiddenContactsString.append(mHiddenContacts.get(i));
            }
        }        
        
		if (checkStatus == null) {
			checkStatus = new HashMap<Integer, Boolean>();
			for (int i = 0; mHiddenContacts != null && i < mHiddenContacts.size(); i++) {
				checkStatus.put(mHiddenContacts.get(i), true);
			}
		}
		
		if (mUnCheckedContacts != null) {
			if (checkStatus != null) {
				for (int i = 0; mUnCheckedContacts != null
						&& i < mUnCheckedContacts.size(); i++) {
					checkStatus.put(mUnCheckedContacts.get(i), false);
				}
			}
			mUnCheckedContacts = null;
		}        
        
        //get person informations from the database with these people.
        if (constraint != null) {
            cons = constraint.toString(); 
            uri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(cons));
        } else {
        	uri = Contacts.CONTENT_URI;
        }

		try {
		resultCursor = mContentResolver.query(uri, PROJECTION_CONTACT_FILTER,
				String.format("%s in (%s)", BaseColumns._ID,
						mHiddenContactsString), null, SORT_ORDER);

		} catch (Exception e) {
			e.printStackTrace();
		} 
		mHandler.post(new Runnable() {
            public void run() {
               try{
                   if (mWaitingDialog != null) {
                        mWaitingDialog.dismiss();
                   } 
                }catch(Exception e){
                }
             }
        });
		
        return resultCursor;
    }
    
    public ArrayList<Integer> getHiddenContacts() {    	
    	ArrayList<Integer> hiddenContacts = new ArrayList<Integer>();
    	
		if (checkStatus != null) {
			for (int contact : checkStatus.keySet()) {
				if (checkStatus.get(contact)) {
					hiddenContacts.add(contact);
				}
			}
		}	
    	
    	return hiddenContacts;
    }
    
    public ArrayList<Integer> getUnCheckedContacts() {
    	ArrayList<Integer> unCheckedContacts = new ArrayList<Integer>();
    	
		if (checkStatus != null) {
			for (int contact : checkStatus.keySet()) {
				if (!checkStatus.get(contact)) {
					unCheckedContacts.add(contact);
				}
			}
		}
		
    	return unCheckedContacts;
    }
}
