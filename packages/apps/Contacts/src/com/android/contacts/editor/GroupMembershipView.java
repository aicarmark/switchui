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

package com.android.contacts.editor;

import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.interactions.GroupCreationDialogFragment;
import com.android.contacts.model.DataKind;
import com.android.contacts.model.EntityDelta;
import com.android.contacts.model.ExchangeAccountType;
import com.android.contacts.model.LocalAccountType;
import com.android.contacts.model.EntityDelta.ValuesDelta;
import com.android.contacts.model.EntityModifier;
import com.android.internal.util.Objects;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * An editor for group membership.  Displays the current group membership list and
 * brings up a dialog to change it.
 */
public class GroupMembershipView extends LinearLayout
        implements OnClickListener, OnItemClickListener {

    private static final int CREATE_NEW_GROUP_GROUP_ID = 133;
	private static final boolean DEBUG = true;
	private static final String TAG = "GroupMembershipView";

    public static final class GroupSelectionItem {
        private final long mGroupId;
        private final String mTitle;
        private boolean mChecked;

        public GroupSelectionItem(long groupId, String title, boolean checked) {
            this.mGroupId = groupId;
            this.mTitle = title;
            mChecked = checked;
        }

        public long getGroupId() {
            return mGroupId;
        }

        public boolean isChecked() {
            return mChecked;
        }

        public void setChecked(boolean checked) {
            mChecked = checked;
        }

        @Override
        public String toString() {
            return mTitle;
        }
    }

    private EntityDelta mState;
    private Cursor mGroupMetaData;
    private String mAccountName;
    private String mAccountType;
    private String mDataSet;
    private TextView mGroupList;
    private ArrayAdapter<GroupSelectionItem> mAdapter;
    private long mDefaultGroupId;
    private long mFavoritesGroupId;
    private ListPopupWindow mPopup;
    private DataKind mKind;
    private boolean mDefaultGroupVisibilityKnown;
    private boolean mDefaultGroupVisible;

    private String mNoGroupString;
    private int mPrimaryTextColor;
    private int mSecondaryTextColor;

    public GroupMembershipView(Context context) {
        super(context);
    }

    public GroupMembershipView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources resources = mContext.getResources();
        // Moto Mod Begin IKHSS6-4499, Config the dare theme by xml
        if (resources.getBoolean(R.bool.contacts_dark_ui)) {
            mPrimaryTextColor = resources.getColor(R.color.people_primary_text_color);
            mSecondaryTextColor = resources.getColor(R.color.dialtacts_secondary_text_color);
        } else {
            mPrimaryTextColor = resources.getColor(R.color.primary_text_color);
            mSecondaryTextColor = resources.getColor(R.color.secondary_text_color);
        }
        // Moto Mod End IKHSS6-4499
        mNoGroupString = mContext.getString(R.string.group_edit_field_hint_text);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (mGroupList != null) {
            mGroupList.setEnabled(enabled);
        }
    }

    public void setKind(DataKind kind) {
        mKind = kind;
        TextView kindTitle = (TextView) findViewById(R.id.kind_title);
        kindTitle.setText(getResources().getString(kind.titleRes).toUpperCase());
    }

    public void setGroupMetaData(Cursor groupMetaData) {
        this.mGroupMetaData = groupMetaData;
        updateView();
    }

    public void setState(EntityDelta state) {
        mState = state;
        ValuesDelta values = state.getValues();
        mAccountType = values.getAsString(RawContacts.ACCOUNT_TYPE);
        mAccountName = values.getAsString(RawContacts.ACCOUNT_NAME);
        mDataSet = values.getAsString(RawContacts.DATA_SET);
        mDefaultGroupVisibilityKnown = false;
        updateView();
    }

    private void updateView() {
        if (mGroupMetaData == null || mGroupMetaData.isClosed() || mAccountType == null
                || mAccountName == null) {
            setVisibility(GONE);
            return;
        }

        boolean accountHasGroups = false;
        mFavoritesGroupId = 0;
        mDefaultGroupId = 0;

        StringBuilder sb = new StringBuilder();
        mGroupMetaData.moveToPosition(-1);
        while (mGroupMetaData.moveToNext()) {
            String accountName = mGroupMetaData.getString(GroupMetaDataLoader.ACCOUNT_NAME);
            String accountType = mGroupMetaData.getString(GroupMetaDataLoader.ACCOUNT_TYPE);
            String dataSet = mGroupMetaData.getString(GroupMetaDataLoader.DATA_SET);
            if (accountName.equals(mAccountName) && accountType.equals(mAccountType)
                    && Objects.equal(dataSet, mDataSet)) {
                long groupId = mGroupMetaData.getLong(GroupMetaDataLoader.GROUP_ID);
                if (!mGroupMetaData.isNull(GroupMetaDataLoader.FAVORITES)
                        && mGroupMetaData.getInt(GroupMetaDataLoader.FAVORITES) != 0) {
                    mFavoritesGroupId = groupId;
                } else if (!mGroupMetaData.isNull(GroupMetaDataLoader.AUTO_ADD)
                            && mGroupMetaData.getInt(GroupMetaDataLoader.AUTO_ADD) != 0) {
                    mDefaultGroupId = groupId;
                } else {
                    accountHasGroups = true;
                }

                // Exclude favorites from the list - they are handled with special UI (star)
                // Also exclude the default group.
                if (groupId != mFavoritesGroupId && groupId != mDefaultGroupId
                        && hasMembership(groupId)) {
                    String title = mGroupMetaData.getString(GroupMetaDataLoader.TITLE);
                    if (!TextUtils.isEmpty(title)) {
                        if (sb.length() != 0) {
                            sb.append(", ");
                        }
                        sb.append(title);
                    }
                }
              /*2012-11-26, DJHV83 Deleted for Jellybean update.*/
              //Begin mtdr83 for IKCBSMMCPPRC-1277
            }/*else if(mAccountType.equalsIgnoreCase(ExchangeAccountType.ACCOUNT_TYPE)
            		|| mAccountType.equalsIgnoreCase(LocalAccountType.ACCOUNT_TYPE)){
            	
            	String label = mGroupMetaData.getString(GroupMetaDataLoader.TITLE);
            	/** modify for SWITCHUITWOV-88 by bphx43 2012-09-07
            	Cursor cursor  = null;
            	try{
            		cursor = mContext.getContentResolver().query(Groups.CONTENT_URI, 
            				new String[]{Groups._ID}, 
            				Groups.ACCOUNT_TYPE + "='" + mAccountType + "'" + " AND "
            						+ Groups.TITLE + "='" + label + "'" + " AND " + Groups.DELETED + "=0"
            						, null, null);
            		
            		if(cursor == null){
            			Log.d(TAG, "cursor == null");
            		}else{
            			boolean hasGroupExist = cursor.moveToFirst();
            			Log.d(TAG, "is this group exist: " + hasGroupExist);
            			if(!hasGroupExist){
            				ContentValues values = new ContentValues();
            				values.put(Groups.ACCOUNT_TYPE, mAccountType);
            				values.put(Groups.ACCOUNT_NAME, mAccountName);
            				values.put(Groups.DATA_SET, mDataSet);
            				
            				if (!TextUtils.isEmpty(label)){
            					values.put(Groups.TITLE, label);
            					values.put(Groups.GROUP_VISIBLE, 1);
            					
            					// Create the new group
            					final Uri groupUri = mContext.getContentResolver().insert(Groups.CONTENT_URI, values);
            					if(groupUri != null){
            						Log.d(TAG, "Create group with account succes: " + mAccountType);
            					}
            				}
            			}
            		}
            	}catch(SQLException e){
            		Log.e(TAG, e.getMessage());
            	}finally{
            		if(cursor != null){
            			cursor.close();//close cursor
            		}
            	}
            	/** modify end by bphx43 
                	

            }*/
          //End mtdr83 for IKCBSMMCPPRC-1277
          /*2012-11-26, DJHV83 deleted end*/
        }
        if(DEBUG)Log.d(TAG,"accountHasGroups=" + accountHasGroups);
        //begin delte by txbv34 for IKCBSMMCPPRC-1306
//        if (!accountHasGroups) {
//            setVisibility(GONE);
//            return;
//        }
        //end delte by txbv34 for IKCBSMMCPPRC-1306
        if (mGroupList == null) {
            mGroupList = (TextView) findViewById(R.id.group_list);
            mGroupList.setOnClickListener(this);
        }

        mGroupList.setEnabled(isEnabled());
        if (sb.length() == 0) {
            mGroupList.setText(mNoGroupString);
            mGroupList.setTextColor(mSecondaryTextColor);
        } else {
            mGroupList.setText(sb);
            mGroupList.setTextColor(mPrimaryTextColor);
        }
        setVisibility(VISIBLE);

        if (!mDefaultGroupVisibilityKnown) {
            // Only show the default group (My Contacts) if the contact is NOT in it
            mDefaultGroupVisible = mDefaultGroupId != 0 && !hasMembership(mDefaultGroupId);
            mDefaultGroupVisibilityKnown = true;
        }
    }

    @Override
    public void onClick(View v) {
        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
            return;
        }

        mAdapter = new ArrayAdapter<GroupSelectionItem>(
                getContext(), R.layout.group_membership_list_item);

        mGroupMetaData.moveToPosition(-1);
        while (mGroupMetaData.moveToNext()) {
            String accountName = mGroupMetaData.getString(GroupMetaDataLoader.ACCOUNT_NAME);
            String accountType = mGroupMetaData.getString(GroupMetaDataLoader.ACCOUNT_TYPE);
            String dataSet = mGroupMetaData.getString(GroupMetaDataLoader.DATA_SET);
            if (accountName.equals(mAccountName) && accountType.equals(mAccountType)
                    && Objects.equal(dataSet, mDataSet)) {
                long groupId = mGroupMetaData.getLong(GroupMetaDataLoader.GROUP_ID);
                if (groupId != mFavoritesGroupId
                        && (groupId != mDefaultGroupId || mDefaultGroupVisible)) {
                    String title = mGroupMetaData.getString(GroupMetaDataLoader.TITLE);
                    boolean checked = hasMembership(groupId);
                    mAdapter.add(new GroupSelectionItem(groupId, title, checked));
                }
            }
        }

        mAdapter.add(new GroupSelectionItem(CREATE_NEW_GROUP_GROUP_ID,
                getContext().getString(R.string.create_group_item_label), false));

        mPopup = new ListPopupWindow(getContext(), null);
        mPopup.setAnchorView(mGroupList);
        mPopup.setAdapter(mAdapter);
        mPopup.setModal(true);
        mPopup.show();

        ListView listView = mPopup.getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setOverScrollMode(OVER_SCROLL_ALWAYS);
        int count = mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            listView.setItemChecked(i, mAdapter.getItem(i).isChecked());
        }

        listView.setOnItemClickListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mPopup != null) {
            mPopup.dismiss();
            mPopup = null;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ListView list = (ListView) parent;
        int count = mAdapter.getCount();

        if (list.isItemChecked(count - 1)) {
            list.setItemChecked(count - 1, false);
            createNewGroup();
            return;
        }

        for (int i = 0; i < count; i++) {
            mAdapter.getItem(i).setChecked(list.isItemChecked(i));
        }

        // First remove the memberships that have been unchecked
        ArrayList<ValuesDelta> entries = mState.getMimeEntries(GroupMembership.CONTENT_ITEM_TYPE);
        if (entries != null) {
            for (ValuesDelta entry : entries) {
                if (!entry.isDelete()) {
                    Long groupId = entry.getAsLong(GroupMembership.GROUP_ROW_ID);
                    if (groupId != null && groupId != mFavoritesGroupId
                            && (groupId != mDefaultGroupId || mDefaultGroupVisible)
                            && !isGroupChecked(groupId)) {
                        entry.markDeleted();
                    }
                }
            }
        }

        // Now add the newly selected items
        for (int i = 0; i < count; i++) {
            GroupSelectionItem item = mAdapter.getItem(i);
            long groupId = item.getGroupId();
            if (item.isChecked() && !hasMembership(groupId)) {
                ValuesDelta entry = EntityModifier.insertChild(mState, mKind);
                entry.put(GroupMembership.GROUP_ROW_ID, groupId);
            }
        }

        updateView();
    }

    private boolean isGroupChecked(long groupId) {
        int count = mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            GroupSelectionItem item = mAdapter.getItem(i);
            if (groupId == item.getGroupId()) {
                return item.isChecked();
            }
        }
        return false;
    }

    private boolean hasMembership(long groupId) {
        if (groupId == mDefaultGroupId && mState.isContactInsert()) {
            return true;
        }

        ArrayList<ValuesDelta> entries = mState.getMimeEntries(GroupMembership.CONTENT_ITEM_TYPE);
        if (entries != null) {
            for (ValuesDelta values : entries) {
                if (!values.isDelete()) {
                    Long id = values.getAsLong(GroupMembership.GROUP_ROW_ID);
                    if (id != null && id == groupId) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void createNewGroup() {
        if (mPopup != null) {
            mPopup.dismiss();
            mPopup = null;
        }

        GroupCreationDialogFragment.show(
                ((Activity) getContext()).getFragmentManager(), mAccountType, mAccountName,
                mDataSet);
    }
}
