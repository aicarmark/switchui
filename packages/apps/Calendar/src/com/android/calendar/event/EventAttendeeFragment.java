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

package com.android.calendar.event;

import com.android.calendar.event.EventAttendeeActivity.EventAttendee;
import com.android.calendar.event.EventAttendeeActivity.EventBundle;
import com.android.calendar.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.text.SpannableStringBuilder;

//import com.motorola.contacts.groups.GroupAPI;

public class EventAttendeeFragment extends Fragment {
    private static final String TAG = "EventAttendeeFragment";
    private static final String BUNDLE_KEY_EVENT = "key_event";

    private static final boolean DEBUG = false;

    private Activity mContext;
    private Menu mMenu;
    private View mView;

    private EventBundle mEventBundle;

    private TextView mTitle;
    private TextView mWhenDate;
    private TextView mWhenTime;
    private TextView mWhere;
    private ExpandableListView mExpandableList;
    private EventAttendeeAdapter mExpandableListAdapter;
    private boolean mAllExpanded = false;

    private boolean mShowOrganizer;
    
    public ContactListener myContactListener;

    private List<Map<String, Object>> mGroupList = new ArrayList<Map<String, Object>>();
    private List<List<Map<String, Object>>> mAllAttendeesList = new ArrayList<List<Map<String, Object>>>();

    public EventAttendeeFragment() {
        this(null);
    }

    public EventAttendeeFragment(EventBundle eventBundle) {
        mEventBundle = eventBundle;
        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;

        if (mEventBundle != null) {
            updateAttendees();
        }
    }

    //add by amt_xulei for SWITCHUITWO-500/SWITCHUITWO-559 at 2013-1-22
    //reason:update the attendee contact icon
	@Override
	public void onDestroy() {
		if (myContactListener != null){
			mContext.getContentResolver().unregisterContentObserver(myContactListener);
		}
		super.onDestroy();
	}
	
	class ContactListener extends ContentObserver{

		public ContactListener(Handler handler){
			super(handler);
		}

		public void onChange(boolean selfChange){
			super.onChange(selfChange);
			if(selfChange){			
				Log.e(TAG, "no need to listener contacts");
				return;
			}		

			Log.e(TAG, "ContactListener have received the notification from the  contact database");
			mExpandableListAdapter.startUpdateChildViewTask();
		}
	}	
	//end add
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.event_attendee, null);
        if (mEventBundle != null) {
            setTextCommon(mView, R.id.title, mEventBundle.mTitle);
            setTextCommon(mView, R.id.when_date, mEventBundle.mWhenDate);
            setTextCommon(mView, R.id.when_time, mEventBundle.mWhenTime);
            View headlines = mView.findViewById(R.id.event_info_headline);
            headlines.setBackgroundColor(mEventBundle.mEventColor);
        }

        mExpandableList = (ExpandableListView) mView.findViewById(R.id.expandableList);
        mExpandableListAdapter = new EventAttendeeAdapter(mContext, mGroupList, mAllAttendeesList);
        mExpandableList.setAdapter(mExpandableListAdapter);

        int groupAccount = mExpandableListAdapter.getGroupCount();
        if (mShowOrganizer && groupAccount > 1) {
            mExpandableList.expandGroup(0);// expand organizer list
            mExpandableList.expandGroup(1);// expand Attending/Attendees list
        } else if (groupAccount > 0) {
            mExpandableList.expandGroup(0);// expand Attending/Attendees list
        }
        mAllExpanded = true;
        for (int i = 0; i < groupAccount; i++) {
            if (!mExpandableList.isGroupExpanded(i)) {
                mAllExpanded = false;
                break;
            }
        }
        
        //add by amt_xulei for SWITCHUITWO-500 at 2013-1-18
    	//reason:update the attendee contact icon
        myContactListener = new ContactListener(new Handler());
		mContext.getContentResolver().registerContentObserver(
				ContactsContract.Contacts.CONTENT_URI, true, myContactListener);
		//end add

        return mView;
    }

    private void updateActionBar() {
        if (mMenu == null) {
            return;
        }

        MenuItem collapseAllItem = mMenu.findItem(R.id.action_collapse_all);
        MenuItem expandAllItem = mMenu.findItem(R.id.action_expand_all);
        MenuItem saveGroupItem = mMenu.findItem(R.id.action_save_group);

        if (isAllGroupExpanded()) {
            // If all sections have been expanded, show option menu "Collapse all"
            collapseAllItem.setVisible(true);
            expandAllItem.setVisible(false);
        } else {
            // If some of sections have been collapsed, show option menu "Expand all"
            collapseAllItem.setVisible(false);
            expandAllItem.setVisible(true);
        }
        // modified by amt-sunli 2012-09-20 SWITCHUITWOV-209 begin
        saveGroupItem.setVisible(/*true*/false);
        // modified by amt-sunli 2012-09-20 SWITCHUITWOV-209 end
    }

    public void hideOptionsMenu() {
        if (mMenu == null) {
            return;
        }

        MenuItem collapseAllItem = mMenu.findItem(R.id.action_collapse_all);
        MenuItem expandAllItem = mMenu.findItem(R.id.action_expand_all);
        MenuItem saveGroupItem = mMenu.findItem(R.id.action_save_group);
        collapseAllItem.setVisible(false);
        expandAllItem.setVisible(false);
        saveGroupItem.setVisible(false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.event_attendee_title_bar, menu);
        synchronized (this) {
            mMenu = menu;
            updateActionBar();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_collapse_all:
            collapseAllGroup();
            updateActionBar();
            break;
        case R.id.action_expand_all:
            expandAllGroup();
            updateActionBar();
            break;
        //Begin Motorola - Emergent group
        case R.id.action_save_group:
            saveGroup();
            break;
        }
       //End Motorola - Emergent group
        return true;
    }

    private void setTextCommon(View view, int id, CharSequence text) {
        TextView textView = (TextView) view.findViewById(id);
        if (textView == null)
            return;
        textView.setText(text);
    }

    private void updateAttendees() {
        Resources res = mContext.getResources();

        if (mEventBundle.mShowOrganizer && mEventBundle.mOrganizer != null) {
            // Display organizer info
            mShowOrganizer = true;
            Map<String, Object> organizerGroupData = new HashMap<String, Object>();
            organizerGroupData.put(EventAttendeeAdapter.GROUP_TITLE_KEY,
                    res.getString(R.string.event_attendee_organizer_label));
            mGroupList.add(organizerGroupData);

            // create attendee data
            List<Map<String, Object>> organizerList = new ArrayList<Map<String,Object>>();
            Map<String, Object> attendeeItem = new HashMap<String, Object>();
            attendeeItem.put(EventAttendeeAdapter.CHILD_NAME_KEY, mEventBundle.mOrganizer.mName);
            attendeeItem.put(EventAttendeeAdapter.CHILD_EMAIL_KEY, mEventBundle.mOrganizer.mEmail);
            organizerList.add(attendeeItem);
            mAllAttendeesList.add(organizerList);
        } else {
            mShowOrganizer = false;
        }

        List<Map<String, Object>> attendeesRequiredYesList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> attendeesRequiredMaybeList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> attendeesRequiredNoList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> attendeesOptionalYesList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> attendeesOptionalMaybeList = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> attendeesOptionalNoList = new ArrayList<Map<String, Object>>();

        if (mEventBundle.mRequiredAcceptedAttendees != null) {
            for (int i = 0; i < mEventBundle.mRequiredAcceptedAttendees.size(); i++) {
                EventAttendee attendee = mEventBundle.mRequiredAcceptedAttendees.get(i);
                // create attendee data
                Map<String, Object> attendeeItem = new HashMap<String, Object>();
                attendeeItem.put(EventAttendeeAdapter.CHILD_NAME_KEY, attendee.mName);
                attendeeItem.put(EventAttendeeAdapter.CHILD_EMAIL_KEY, attendee.mEmail);
                attendeesRequiredYesList.add(attendeeItem);
            }
        }

        if (mEventBundle.mRequiredTentativeAttendees != null) {
            for (int i = 0; i < mEventBundle.mRequiredTentativeAttendees.size(); i++) {
                EventAttendee attendee = mEventBundle.mRequiredTentativeAttendees.get(i);
                // create attendee data
                Map<String, Object> attendeeItem = new HashMap<String, Object>();
                attendeeItem.put(EventAttendeeAdapter.CHILD_NAME_KEY, attendee.mName);
                attendeeItem.put(EventAttendeeAdapter.CHILD_EMAIL_KEY, attendee.mEmail);
                attendeesRequiredMaybeList.add(attendeeItem);
            }
        }

        if (mEventBundle.mRequiredDeclinedAttendees != null) {
            for (int i = 0; i < mEventBundle.mRequiredDeclinedAttendees.size(); i++) {
                EventAttendee attendee = mEventBundle.mRequiredDeclinedAttendees.get(i);
                // create attendee data
                Map<String, Object> attendeeItem = new HashMap<String, Object>();
                attendeeItem.put(EventAttendeeAdapter.CHILD_NAME_KEY, attendee.mName);
                attendeeItem.put(EventAttendeeAdapter.CHILD_EMAIL_KEY, attendee.mEmail);
                attendeesRequiredNoList.add(attendeeItem);
            }
        }

        if (mEventBundle.mRequiredNoResponseAttendees != null) {
            for (int i = 0; i < mEventBundle.mRequiredNoResponseAttendees.size(); i++) {
                EventAttendee attendee = mEventBundle.mRequiredNoResponseAttendees.get(i);
                // create attendee data
                Map<String, Object> attendeeItem = new HashMap<String, Object>();
                attendeeItem.put(EventAttendeeAdapter.CHILD_NAME_KEY, attendee.mName);
                attendeeItem.put(EventAttendeeAdapter.CHILD_EMAIL_KEY, attendee.mEmail);
                attendeesRequiredMaybeList.add(attendeeItem);
            }
        }

        if (mEventBundle.mOptionalAcceptedAttendees != null) {
            for (int i = 0; i < mEventBundle.mOptionalAcceptedAttendees.size(); i++) {
                EventAttendee attendee = mEventBundle.mOptionalAcceptedAttendees.get(i);
                // create attendee data
                Map<String, Object> attendeeItem = new HashMap<String, Object>();
                attendeeItem.put(EventAttendeeAdapter.CHILD_NAME_KEY, attendee.mName);
                attendeeItem.put(EventAttendeeAdapter.CHILD_EMAIL_KEY, attendee.mEmail);
                attendeesOptionalYesList.add(attendeeItem);
            }
        }

        if (mEventBundle.mOptionalTentativeAttendees != null) {
            for (int i = 0; i < mEventBundle.mOptionalTentativeAttendees.size(); i++) {
                EventAttendee attendee = mEventBundle.mOptionalTentativeAttendees.get(i);
                // create attendee data
                Map<String, Object> attendeeItem = new HashMap<String, Object>();
                attendeeItem.put(EventAttendeeAdapter.CHILD_NAME_KEY, attendee.mName);
                attendeeItem.put(EventAttendeeAdapter.CHILD_EMAIL_KEY, attendee.mEmail);
                attendeesOptionalMaybeList.add(attendeeItem);
            }
        }

        if (mEventBundle.mOptionalDeclinedAttendees != null) {
            for (int i = 0; i < mEventBundle.mOptionalDeclinedAttendees.size(); i++) {
                EventAttendee attendee = mEventBundle.mOptionalDeclinedAttendees.get(i);
                // create attendee data
                Map<String, Object> attendeeItem = new HashMap<String, Object>();
                attendeeItem.put(EventAttendeeAdapter.CHILD_NAME_KEY, attendee.mName);
                attendeeItem.put(EventAttendeeAdapter.CHILD_EMAIL_KEY, attendee.mEmail);
                attendeesOptionalNoList.add(attendeeItem);
            }
        }

        if (mEventBundle.mOptionalNoResponseAttendees != null) {
            for (int i = 0; i < mEventBundle.mOptionalNoResponseAttendees.size(); i++) {
                EventAttendee attendee = mEventBundle.mOptionalNoResponseAttendees.get(i);
                // create attendee data
                Map<String, Object> attendeeItem = new HashMap<String, Object>();
                attendeeItem.put(EventAttendeeAdapter.CHILD_NAME_KEY, attendee.mName);
                attendeeItem.put(EventAttendeeAdapter.CHILD_EMAIL_KEY, attendee.mEmail);
                attendeesOptionalMaybeList.add(attendeeItem);
            }
        }

        int attendeesRequiredYesCount = attendeesRequiredYesList.size();
        int attendeesRequiredMaybeCount = attendeesRequiredMaybeList.size();
        int attendeesRequiredNoCount = attendeesRequiredNoList.size();
        int attendeesOptionalYesCount = attendeesOptionalYesList.size();
        int attendeesOptionalMaybeCount = attendeesOptionalMaybeList.size();
        int attendeesOptionalNoCount = attendeesOptionalNoList.size();

        int yesTotalCount = 0;
        int maybeTotalCount = 0;
        int noTotalCount = 0;

        boolean hasAttendeeStatus = (attendeesRequiredYesCount > 0 || attendeesOptionalYesCount > 0
                || attendeesRequiredNoCount > 0 || attendeesOptionalNoCount > 0);
        String format;

        // Divider 'Optional:'
        Map<String, Object> dividerItem = new HashMap<String, Object>();
        dividerItem.put(EventAttendeeAdapter.CHILD_NAME_KEY,
                res.getString(R.string.view_event_optional_label));
        dividerItem.put(EventAttendeeAdapter.CHILD_EMAIL_KEY, "");

        // Attendee status can be retrieved only for Exchange ActiveSync 12x or later and Google calendar.
        // For ActiveSync, the status is available only to the organizer and not to the attendees.
        if (false/*!mModel.mIsOrganizer && !hasAttendeeStatus && mIsActiveSync*/) {
            maybeTotalCount = attendeesRequiredMaybeCount + attendeesOptionalMaybeCount;
            StringBuilder maybeGroupLabel = new StringBuilder();
            // Attendees (x) label
            format = res.getString(R.string.event_attendee_details_all_attendees_label);
            maybeGroupLabel.append(String.format(format, maybeTotalCount));
            Map<String, Object> maybeGroupData = new HashMap<String, Object>();
            maybeGroupData.put(EventAttendeeAdapter.GROUP_TITLE_KEY, maybeGroupLabel);
            mGroupList.add(maybeGroupData);

            List<Map<String, Object>> attendeesList = attendeesRequiredMaybeList;
            if (attendeesOptionalMaybeList.size() > 0) {
                attendeesList.add(dividerItem);
                attendeesList.addAll(attendeesOptionalMaybeList);
            }
            mAllAttendeesList.add(attendeesList);
        } else {
            // Attendees - YES response
            yesTotalCount = attendeesRequiredYesCount + attendeesOptionalYesCount;
            StringBuilder attendingGroupLabel = new StringBuilder();
            // Attending (x) label
            format = res.getString(R.string.event_attendee_details_yes_label);
            attendingGroupLabel.append(String.format(format, yesTotalCount));
            Map<String, Object> attendingGroupData = new HashMap<String, Object>();
            attendingGroupData.put(EventAttendeeAdapter.GROUP_TITLE_KEY, attendingGroupLabel);
            mGroupList.add(attendingGroupData);

            List<Map<String, Object>> attendeesYesList = attendeesRequiredYesList;
            if (attendeesOptionalYesList.size() > 0) {
                // Append the Optional Yes list to the Attending list
                attendeesYesList.add(dividerItem);
                attendeesYesList.addAll(attendeesOptionalYesList);
            }
            mAllAttendeesList.add(attendeesYesList);

            // Attendees - MAYBE response
            maybeTotalCount = attendeesRequiredMaybeCount + attendeesOptionalMaybeCount;
            StringBuilder maybeGroupLabel = new StringBuilder();
            // Maybe (x) label
            format = res.getString(R.string.event_attendee_details_maybe_label);
            maybeGroupLabel.append(String.format(format, maybeTotalCount));
            Map<String, Object> maybeGroupData = new HashMap<String, Object>();
            maybeGroupData.put(EventAttendeeAdapter.GROUP_TITLE_KEY, maybeGroupLabel);
            mGroupList.add(maybeGroupData);

            List<Map<String, Object>> attendeesMaybeList = attendeesRequiredMaybeList;
            if (attendeesOptionalMaybeList.size() > 0) {
                // Append the Optional Maybe list to the Maybe list
                attendeesMaybeList.add(dividerItem);
                attendeesMaybeList.addAll(attendeesOptionalMaybeList);
            }
            mAllAttendeesList.add(attendeesMaybeList);

            // Attendees - NO response
            noTotalCount = attendeesRequiredNoCount + attendeesOptionalNoCount;
            StringBuilder noGroupLabel = new StringBuilder();
            // Not attending (x) label
            format = res.getString(R.string.event_attendee_details_no_label);
            noGroupLabel.append(String.format(format, noTotalCount));
            Map<String, Object> noGroupData = new HashMap<String, Object>();
            noGroupData.put(EventAttendeeAdapter.GROUP_TITLE_KEY, noGroupLabel);
            mGroupList.add(noGroupData);

            List<Map<String, Object>> attendeesNoList = attendeesRequiredNoList;
            if (attendeesOptionalNoList.size() > 0) {
                // Append the Optional Not attending list to the Not attending list
                attendeesNoList.add(dividerItem);
                attendeesNoList.addAll(attendeesOptionalNoList);
            }
            mAllAttendeesList.add(attendeesNoList);
        }
    }

    private void collapseAllGroup() {
        int groupAccount = mExpandableListAdapter.getGroupCount();
        for (int i = 0; i < groupAccount; i++) {
            mExpandableList.collapseGroup(i);
        }
        mAllExpanded = false;
    }

    private void expandAllGroup() {
        int groupAccount = mExpandableListAdapter.getGroupCount();
        for (int i = 0; i < groupAccount; i++) {
            mExpandableList.expandGroup(i);
        }
        mAllExpanded = true;
    }

    private boolean isAllGroupExpanded() {
        return mAllExpanded;
    }
    //Begin Motorola - Emergent Group
    private void saveGroup() {
        //get attendee emails
        StringBuilder attendeeEmails = new StringBuilder();
        for (List<Map<String, Object>> attendeeList : mAllAttendeesList) {
            for (Map<String, Object> attendee : attendeeList) {
                String email = attendee.get("EMAIL").toString();
                if (!TextUtils.isEmpty(email)) {
                    attendeeEmails.append(email + ";");
                }
            }
        }
        if (DEBUG) {
            Log.d(TAG, "emailAddresses: " + attendeeEmails.toString());
        }
        if(TextUtils.isEmpty(attendeeEmails.toString()))
            return;
        //Intent intent = new Intent(GroupAPI.GroupIntents.SavingGroup.ACTION_GROUPS_QUICKTASK_SAVE_GROUP);
        //intent.putExtra(GroupAPI.GroupIntents.SavingGroup.EXTRA_GROUP_QUICKTASK_GROUP_TITLE, mEventBundle.mTitle);
        //intent.putExtra(GroupAPI.GroupIntents.SavingGroup.EXTRA_GROUP_QUICKTASK_EMAIL_ADDRESSES, attendeeEmails.toString());
        //startActivity(intent);
    }
    //Begin Motorola - Emergent Group
}
