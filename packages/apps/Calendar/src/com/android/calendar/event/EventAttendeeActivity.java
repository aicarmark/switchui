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

import com.android.calendar.R;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Events;
import android.util.Log;
import android.view.MenuItem;

public class EventAttendeeActivity extends Activity {
    private static final String TAG = "EventAttendeeActivity";

    private static final boolean DEBUG = true;

    public static final String INTENT_KEY_WHEN_DATE = "whenDate";
    public static final String INTENT_KEY_WHEN_TIME = "whenTime";
    public static final String INTENT_KEY_SHOW_ORGANIZER = "showOrganizer";
    public static final String INTENT_KEY_ATTENDEE_REQUIRED_ACCEPT = "requiredAcceptedAttendees";
    public static final String INTENT_KEY_ATTENDEE_REQUIRED_TENTATIVE = "requiredTentativeAttendees";
    public static final String INTENT_KEY_ATTENDEE_REQUIRED_DECLINE = "requiredDeclinedAttendees";
    public static final String INTENT_KEY_ATTENDEE_REQUIRED_NO_RESP = "requiredNoResponseAttendees";
    public static final String INTENT_KEY_ATTENDEE_OPTIONAL_ACCEPT = "optionalAcceptedAttendees";
    public static final String INTENT_KEY_ATTENDEE_OPTIONAL_TENTATIVE = "optionalTentativeAttendees";
    public static final String INTENT_KEY_ATTENDEE_OPTIONAL_DECLINE = "optionalDeclinedAttendees";
    public static final String INTENT_KEY_ATTENDEE_OPTIONAL_NO_RESP = "optionalNoResponseAttendees";

    private EventAttendeeFragment mAttendeeFragment;
    private EventBundle mEventBundle;

    public static class EventAttendee implements Parcelable {
        public String mName;
        public String mEmail;

        public EventAttendee(String name, String email) {
            mName = name;
            mEmail = email;
        }

        public EventAttendee(Parcel source) {
            mName = source.readString();
            mEmail = source.readString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mName);
            dest.writeString(mEmail);
        }

        public static final Parcelable.Creator<EventAttendee> CREATOR = new Parcelable.Creator<EventAttendee>() {
            public EventAttendee createFromParcel(Parcel source) {
                return new EventAttendee(source);
            }

            public EventAttendee[] newArray(int size) {
                return new EventAttendee[size];
            }
        };
    }

    static class EventBundle {
        public String mTitle;
        public String mWhenDate;
        public String mWhenTime;
        public int mEventColor;
        public boolean mShowOrganizer;

        public EventAttendee mOrganizer;
        public List<EventAttendee> mRequiredAcceptedAttendees;
        public List<EventAttendee> mRequiredTentativeAttendees;
        public List<EventAttendee> mRequiredDeclinedAttendees;
        public List<EventAttendee> mRequiredNoResponseAttendees;
        public List<EventAttendee> mOptionalAcceptedAttendees;
        public List<EventAttendee> mOptionalTentativeAttendees;
        public List<EventAttendee> mOptionalDeclinedAttendees;
        public List<EventAttendee> mOptionalNoResponseAttendees;

        public EventBundle() {
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.simple_frame_layout);

        mEventBundle = getEventInfoFromIntent(icicle);
        mAttendeeFragment = (EventAttendeeFragment) getFragmentManager().findFragmentById(
                R.id.main_frame);

        getActionBar().setDisplayOptions(
                ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME);

        if (mAttendeeFragment == null && mEventBundle != null) {
            mAttendeeFragment = new EventAttendeeFragment(mEventBundle);

            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.replace(R.id.main_frame, mAttendeeFragment);
            ft.show(mAttendeeFragment);
            ft.commit();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mAttendeeFragment != null) {
            mAttendeeFragment.hideOptionsMenu();
        }
        invalidateOptionsMenu();
    }

    private EventBundle getEventInfoFromIntent(Bundle icicle) {
        Intent intent = getIntent();
        if (intent == null) {
            return null;
        }

        EventBundle eventBundle = new EventBundle();
        eventBundle.mTitle = intent.getStringExtra(Events.TITLE);
        eventBundle.mWhenDate = intent.getStringExtra(INTENT_KEY_WHEN_DATE);
        eventBundle.mWhenTime = intent.getStringExtra(INTENT_KEY_WHEN_TIME);
        eventBundle.mEventColor = intent.getIntExtra(Events.EVENT_COLOR, Color.TRANSPARENT);
        eventBundle.mShowOrganizer = intent.getBooleanExtra(INTENT_KEY_SHOW_ORGANIZER, true);

        eventBundle.mOrganizer = intent.getParcelableExtra(Events.ORGANIZER);
        eventBundle.mRequiredAcceptedAttendees = intent
                .getParcelableArrayListExtra(INTENT_KEY_ATTENDEE_REQUIRED_ACCEPT);
        eventBundle.mRequiredTentativeAttendees = intent
                .getParcelableArrayListExtra(INTENT_KEY_ATTENDEE_REQUIRED_TENTATIVE);
        eventBundle.mRequiredDeclinedAttendees = intent
                .getParcelableArrayListExtra(INTENT_KEY_ATTENDEE_REQUIRED_DECLINE);
        eventBundle.mRequiredNoResponseAttendees = intent
                .getParcelableArrayListExtra(INTENT_KEY_ATTENDEE_REQUIRED_NO_RESP);
        eventBundle.mOptionalAcceptedAttendees = intent
                .getParcelableArrayListExtra(INTENT_KEY_ATTENDEE_OPTIONAL_ACCEPT);
        eventBundle.mOptionalTentativeAttendees = intent
                .getParcelableArrayListExtra(INTENT_KEY_ATTENDEE_OPTIONAL_TENTATIVE);
        eventBundle.mOptionalDeclinedAttendees = intent
                .getParcelableArrayListExtra(INTENT_KEY_ATTENDEE_OPTIONAL_DECLINE);
        eventBundle.mOptionalNoResponseAttendees = intent
                .getParcelableArrayListExtra(INTENT_KEY_ATTENDEE_OPTIONAL_NO_RESP);

        return eventBundle;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
