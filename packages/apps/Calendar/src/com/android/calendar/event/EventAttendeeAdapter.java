/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.calendar.event;

import com.android.calendar.R;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Presence;
import android.provider.ContactsContract.QuickContact;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class EventAttendeeAdapter extends BaseExpandableListAdapter {
    private static final String TAG = "EventAttendeeAdapter";

    private static final boolean DEBUG = true;

    public static final String GROUP_TITLE_KEY = "TITLE";
    public static final String CHILD_NAME_KEY = "NAME";
    public static final String CHILD_EMAIL_KEY = "EMAIL";

    private static final String CHILD_EMAIL_TYPE_KEY = "EMAIL_TYPE";
    private static final String CHILD_CONTACT_ID_KEY = "CONTACT_ID";
    private static final String CHILD_PHOTO_ID_KEY = "PHOTO_ID";

    private static final Uri CONTACT_DATA_WITH_PRESENCE_URI = Data.CONTENT_URI;
    private static final int PRESENCE_PROJECTION_CONTACT_ID_INDEX = 0;
    private static final int PRESENCE_PROJECTION_PRESENCE_INDEX = 1;
    private static final int PRESENCE_PROJECTION_EMAIL_INDEX = 2;
    private static final int PRESENCE_PROJECTION_EMAIL_TYPE_INDEX = 3;
    private static final int PRESENCE_PROJECTION_PHOTO_ID_INDEX = 4;
    private static final int PRESENCE_PROJECTION_DISPLAY_NAME_INDEX = 5;

    private static final String[] PRESENCE_PROJECTION = new String[] {
            Email.CONTACT_ID, // 0
            Email.PRESENCE_STATUS, // 1
            Email.DATA, // 2
            Email.TYPE, // 3
            Email.PHOTO_ID, // 4
            Contacts.DISPLAY_NAME, // 5
    };

    private static final int TYPE_CHILD = 0;
    private static final int TYPE_DIVIDER = 1;
    private static final int TYPE_MAX_COUNT = TYPE_DIVIDER + 1;

    private Context mContext;
    private LayoutInflater mInflater;
    private List<Map<String, Object>> mGroupList;
    private List<List<Map<String, Object>>> mAllChildList;
    private Map<String, Object> mAttendeeItem;
    private UpdateChildViewTask mUpdateChildViewTask;

    static class GroupViewHolder {
        TextView text;
    }

    static class ChildViewHolder {
        TextView divider;
        TextView displayName;
        TextView emailType;
        TextView emailAddress;
        QuickContactBadge badge;
    }

    private class UpdateChildViewTask extends AsyncTask<Void, Integer, Void> {

        public UpdateChildViewTask() {
        }

        @Override
        protected void onPreExecute() {
            if (DEBUG) Log.d(TAG, "UpdateChildViewTask onPreExecute()");
        }

        @Override
        protected Void doInBackground(Void... v) {
            if (DEBUG) Log.d(TAG, "UpdateChildViewTask doInBackground()");
            for (int groupPosition = 0; groupPosition < mAllChildList.size(); groupPosition++) {
                for (int childPosition = 0; childPosition < mAllChildList.get(groupPosition).size(); childPosition++) {
                    Map<String, Object> attendeeItem = (Map<String, Object>)mAllChildList.get(groupPosition).get(childPosition);
                    String childEmail = attendeeItem.get(CHILD_EMAIL_KEY).toString();
                    if (childEmail != null && childEmail.length() == 0) {
                        // DIVIDER, do nothing;
                    } else {
                        // CHILD, search it in Contacts DB and update
                        ContentResolver cr = mContext.getContentResolver();
                        //IKMAIN-19763 SQLiteException
                        String childEmailEscapeString = DatabaseUtils.sqlEscapeString(childEmail);
                        Cursor contactCursor = cr.query(CONTACT_DATA_WITH_PRESENCE_URI, PRESENCE_PROJECTION,
                                               Email.DATA + " = " + childEmailEscapeString + " COLLATE NOCASE", null, null);
                        try {
                            if (contactCursor != null && contactCursor.moveToFirst()) {
                                // if found it in Contacts, update the display name and badge
                                int emailType = contactCursor.getInt(PRESENCE_PROJECTION_EMAIL_TYPE_INDEX);
                                int contactId = contactCursor.getInt(PRESENCE_PROJECTION_CONTACT_ID_INDEX);
                                int photoId = contactCursor.getInt(PRESENCE_PROJECTION_PHOTO_ID_INDEX);
                                String display_name = contactCursor.getString(PRESENCE_PROJECTION_DISPLAY_NAME_INDEX);

                                if (DEBUG) Log.d(TAG, "doInBackground contactId: " + contactId + " PhotoId: " +
                                            photoId + " Email: " + childEmailEscapeString + " Email type: " +
                                            emailType + " Display name: " + display_name);
                                attendeeItem.put(CHILD_NAME_KEY, display_name);
                                attendeeItem.put(CHILD_EMAIL_TYPE_KEY, Integer.valueOf(emailType));
                                attendeeItem.put(CHILD_CONTACT_ID_KEY, Integer.valueOf(contactId));
                                attendeeItem.put(CHILD_PHOTO_ID_KEY, Integer.valueOf(photoId));
                            } else {
                            	//modify by amt_xulei for SWITCHUITWO-579 at 2013-1-23
                            	//reason:update data to default when this contact email is changed.
                            	attendeeItem.put(CHILD_NAME_KEY, childEmail);
                            	attendeeItem.put(CHILD_EMAIL_TYPE_KEY, -1);
                            	attendeeItem.put(CHILD_CONTACT_ID_KEY, 0);
                            	attendeeItem.put(CHILD_PHOTO_ID_KEY, 0);
                            	//end modify for SWITCHUITWO-579
                            }
                        } finally {
                            if (contactCursor != null) {
                                contactCursor.close();
                            }
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            if (DEBUG) Log.d(TAG, "UpdateChildViewTask onPostExecute()");
            notifyDataSetChanged();// when update done, refresh the UI
        }
    }

    public void startUpdateChildViewTask() {
        if (mUpdateChildViewTask != null) {
            mUpdateChildViewTask.cancel(true);
        }
        mUpdateChildViewTask = new UpdateChildViewTask();
        mUpdateChildViewTask.execute();
    }

    public EventAttendeeAdapter(Context context, List<Map<String, Object>> groupList, List<List<Map<String, Object>>> allChildList) {
        // TODO Auto-generated constructor stub
        mContext = context;
        mGroupList = groupList;
        mAllChildList = allChildList;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        startUpdateChildViewTask();
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mAllChildList.get(groupPosition).get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public int getChildType(int groupPosition, int childPosition) {
        mAttendeeItem = (Map<String, Object>)mAllChildList.get(groupPosition).get(childPosition);
        String childEmail = mAttendeeItem.get(CHILD_EMAIL_KEY).toString();
        if (childEmail != null && childEmail.length() == 0) {
            return TYPE_DIVIDER;
        } else {
            return TYPE_CHILD;
        }
    }

    @Override
    public int getChildTypeCount() {
        return TYPE_MAX_COUNT;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View contactItemView, ViewGroup parent) {
        ChildViewHolder holder;
        int type = getChildType(groupPosition, childPosition);
        if (contactItemView == null) {
            holder = new ChildViewHolder();
            switch (type) {
                case TYPE_DIVIDER:
                    contactItemView = mInflater.inflate(R.layout.attendee_divider_item, null);
                    holder.divider = (TextView) contactItemView.findViewById(R.id.divideText);
                    break;
                case TYPE_CHILD:
                    contactItemView = mInflater.inflate(R.layout.contact_item_attendee, null);
                    holder.displayName = (TextView) contactItemView.findViewById(R.id.name);
                    holder.emailType = (TextView) contactItemView.findViewById(R.id.email_type);
                    holder.emailAddress = (TextView) contactItemView.findViewById(R.id.email_address);
                    holder.badge = (QuickContactBadge) contactItemView.findViewById(R.id.badge);
                    break;
            }
            contactItemView.setTag(holder);
        } else {
            holder = (ChildViewHolder)contactItemView.getTag();
        }

        switch (type) {
            case TYPE_DIVIDER:
                String dividerInfo = mAttendeeItem.get(CHILD_NAME_KEY).toString();
                if (dividerInfo != null && dividerInfo.length() > 0) {
                    holder.divider.setText(dividerInfo);
                } else {
                    holder.divider.setVisibility(View.GONE);
                }
                break;
            case TYPE_CHILD:
                // set the child item details
                setChildItemView(holder);
                break;
        }

        return contactItemView;
    }

    private void setImageFromContact(Uri personUri, ChildViewHolder holder, int defaultImageId) {
        InputStream inputStream = null;
        Object result;
        try {
            inputStream = Contacts.openContactPhotoInputStream(
                    mContext.getContentResolver(), personUri);
        } catch (Exception e) {
            Log.e(TAG, "Error opening photo input stream", e);
            result = null;
        }

        if (inputStream != null) {
            result = Drawable.createFromStream(inputStream, personUri.toString());
        } else {
            result = null;
        }

        if (result != null) {
            holder.badge.setImageDrawable((Drawable)result);
        } else {
            holder.badge.setImageResource(defaultImageId);
        }
    }

    private void setChildItemView(ChildViewHolder holder) {
        Resources res = mContext.getResources();
        int defaultImageId = R.drawable.ic_contact_picture;
        String displayName = mAttendeeItem.get(CHILD_NAME_KEY).toString();
        String email = mAttendeeItem.get(CHILD_EMAIL_KEY).toString();
        Integer emailType_Integer = (Integer)mAttendeeItem.get(CHILD_EMAIL_TYPE_KEY);
        Integer contactId_Integer = (Integer)mAttendeeItem.get(CHILD_CONTACT_ID_KEY);
        Integer photoId_Integer = (Integer)mAttendeeItem.get(CHILD_PHOTO_ID_KEY);
        int emailType = -1;
        int contactId = 0;
        int photoId = -1;
        if (emailType_Integer != null) {
            emailType = emailType_Integer.intValue();
        }
        if (contactId_Integer != null) {
            contactId = contactId_Integer.intValue();
        }
        if (photoId_Integer != null) {
            photoId = photoId_Integer.intValue();
        }
        //Update display name if it is recognized as unknown before
        if (holder.displayName != null) {
            if (displayName != null && displayName.length() > 0) {
                if (DEBUG) Log.d(TAG, "Update display name to " + displayName);
                holder.displayName.setText(displayName);
            } else {
                holder.displayName.setText(res.getString(R.string.attendee_name_unknown));
            }
        }
        //Update email type
        if (holder.emailType != null) {
            if (emailType != -1) {
                holder.emailType.setText(res.getString(Email.getTypeLabelResource(emailType)) + ": ");
            } else {
                holder.emailType.setText("");
            }
        }
        // Email address
        holder.emailAddress.setText(email);
        // Badge
        holder.badge.assignContactFromEmail(email, true);
        if (photoId > 0 ) {
            Uri personUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
            setImageFromContact(personUri, holder, defaultImageId);
        } else {
            holder.badge.setImageResource(defaultImageId);
        }
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mAllChildList.get(groupPosition).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mGroupList.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return mGroupList.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View groupItemView, ViewGroup parent) {
        GroupViewHolder holder;
        // group title
        String groupTitle = mGroupList.get(groupPosition).get(GROUP_TITLE_KEY).toString();

        if (groupItemView == null) {
            holder = new GroupViewHolder();
            groupItemView = mInflater.inflate(R.layout.attendee_group_item, null);
            holder.text = (TextView) groupItemView.findViewById(R.id.groupTitle);
            groupItemView.setTag(holder);
        } else {
            holder = (GroupViewHolder)groupItemView.getTag();
        }

        if (groupTitle.length() > 0) {
            holder.text.setText(groupTitle);
        } else {
            holder.text.setVisibility(View.GONE);
        }

        return groupItemView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

}
