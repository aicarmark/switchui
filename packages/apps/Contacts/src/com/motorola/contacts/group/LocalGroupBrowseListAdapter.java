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
 * limitations under the License.
 */

package com.motorola.contacts.group;

import com.android.contacts.ContactPhotoManager;
import com.android.contacts.GroupListLoader;
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;
import com.android.internal.util.Objects;
import com.android.contacts.R;

import com.motorola.contacts.group.LocalGroupListLoader;
import com.motorola.contacts.group.LocalGroupListItem;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Groups;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Adapter to populate the list of groups.
 */
public class LocalGroupBrowseListAdapter extends BaseAdapter {

    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final AccountTypeManager mAccountTypeManager;

    private Cursor mCursor;

    private boolean mSelectionVisible;
    private String mSelectedGroupTitle;

    public LocalGroupBrowseListAdapter(Context context) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        mAccountTypeManager = AccountTypeManager.getInstance(mContext);
    }

    public void setCursor(Cursor cursor) {
        mCursor = cursor;

        // If there's no selected group already and the cursor is valid, then by default, select the
        // first group
        if (TextUtils.isEmpty(mSelectedGroupTitle) && cursor != null && cursor.getCount() > 0) {
            LocalGroupListItem firstItem = getItem(0);
            mSelectedGroupTitle = (firstItem == null) ? null : firstItem.getTitle();
        }

        notifyDataSetChanged();
    }

    public int getSelectedGroupPosition() {
    	// Motorola, ODC_001639, 2013-01-09, SWITCHUITWO-475, add mCursor.isClosed()
        if (TextUtils.isEmpty(mSelectedGroupTitle) || mCursor == null || mCursor.isClosed() || mCursor.getCount() == 0) {
        	// END SWITCHUITWO-475	
            return -1;
        }

        int index = 0;
        mCursor.moveToPosition(-1);
        while (mCursor.moveToNext()) {
            String groupTitle = mCursor.getString(LocalGroupListLoader.TITLE);
            if (mSelectedGroupTitle.equals(groupTitle)) {
                  return index;
            }
            index++;
        }
        return -1;
    }

    public void setSelectionVisible(boolean flag) {
        mSelectionVisible = flag;
    }

    public void setSelectedGroup(String groupTitle) {
        mSelectedGroupTitle = groupTitle;
    }

    private boolean isSelectedGroup(String groupTitle) {
        return (!TextUtils.isEmpty(mSelectedGroupTitle)) && mSelectedGroupTitle.equals(groupTitle);
    }

    public String getSelectedGroup() {
        return mSelectedGroupTitle;
    }

    @Override
    public int getCount() {
    	// Motorola, ODC_001639, 2013-01-09, SWITCHUITWO-475, add mCursor.isClosed()
        return (mCursor == null || mCursor.isClosed()) ? 0 : mCursor.getCount();
        // END SWITCHUITWO-475
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public LocalGroupListItem getItem(int position) {
        if (mCursor == null || mCursor.isClosed() || !mCursor.moveToPosition(position)) {
            return null;
        }

        String title = mCursor.getString(LocalGroupListLoader.TITLE);
        int memberCount = mCursor.getInt(LocalGroupListLoader.MEMBER_COUNT);

        boolean isFirstGroupInAccount = false;
        if (position == 0) {
            isFirstGroupInAccount = true;
        } else {
            isFirstGroupInAccount = false;
        }
        return new LocalGroupListItem(title, memberCount, isFirstGroupInAccount);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LocalGroupListItem entry = getItem(position);
        View result;
        LocalGroupListItemViewCache viewCache;
        if (convertView != null) {
            result = convertView;
            viewCache = (LocalGroupListItemViewCache) result.getTag();
        } else {
            result = mLayoutInflater.inflate(R.layout.group_browse_list_item, parent, false);
            viewCache = new LocalGroupListItemViewCache(result);
            result.setTag(viewCache);
        }

        // Add a header if this is the first group in an account and hide the divider
        if (entry.isFirstGroupInAccount()) {
            viewCache.divider.setVisibility(View.GONE);
            if (position == 0) {
                // Have the list's top padding in the first header.
                //
                // This allows the ListView to show correct fading effect on top.
                // If we have topPadding in the ListView itself, an inappropriate padding is
                // inserted between fading items and the top edge.
                viewCache.accountHeaderExtraTopPadding.setVisibility(View.VISIBLE);
            } else {
                viewCache.accountHeaderExtraTopPadding.setVisibility(View.GONE);
            }
        } else {
            viewCache.divider.setVisibility(View.VISIBLE);
            viewCache.accountHeaderExtraTopPadding.setVisibility(View.GONE);
        }

        // Bind the group data
        String groupTitle = entry.getTitle();
        String memberCountString = mContext.getResources().getQuantityString(
                R.plurals.group_list_num_contacts_in_group, entry.getMemberCount(),
                entry.getMemberCount());
        viewCache.setGroupTitle(entry.getTitle());
        viewCache.groupTitle.setText(entry.getTitle());
        viewCache.groupMemberCount.setText(memberCountString);

        if (mSelectionVisible) {
            result.setActivated(isSelectedGroup(groupTitle));
        }
        return result;
    }
    private static Uri getGroupUriFromId(long groupId) {
        return ContentUris.withAppendedId(Groups.CONTENT_URI, groupId);
    }

    /**
     * Cache of the children views of a contact detail entry represented by a
     * {@link GroupListItem}
     */
    public static class LocalGroupListItemViewCache {
        public final TextView groupTitle;
        public final TextView groupMemberCount;
        public final View accountHeaderExtraTopPadding;
        public final View divider;
        private Uri mUri;
        private String mGroupTitle;

        public LocalGroupListItemViewCache(View view) {
            groupTitle = (TextView) view.findViewById(R.id.label);
            groupMemberCount = (TextView) view.findViewById(R.id.count);
            accountHeaderExtraTopPadding = view.findViewById(R.id.header_extra_top_padding);
            divider = view.findViewById(R.id.divider);
        }

        public void setUri(Uri uri) {
            mUri = uri;
        }

        public Uri getUri() {
            return mUri;
        }


        public void setGroupTitle(String title) {
            mGroupTitle = title;
        }

        public String getGroupTitle() {
            return mGroupTitle;
        }

    }
}
