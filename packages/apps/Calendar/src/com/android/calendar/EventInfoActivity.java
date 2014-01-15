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
package com.android.calendar;

import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;
import static android.provider.CalendarContract.Attendees.ATTENDEE_STATUS;
import static com.android.calendar.CalendarController.EVENT_ATTENDEE_RESPONSE;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract.Attendees;
import android.util.Log;
// Begin Motorola
import com.android.calendar.ics.IcsConstants;
// End Motorola
public class EventInfoActivity extends Activity {
//        implements CalendarController.EventHandler, SearchView.OnQueryTextListener,
//        SearchView.OnCloseListener {

    private static final String TAG = "EventInfoActivity";
    private EventInfoFragment mInfoFragment;
    private long mStartMillis, mEndMillis;
    private long mEventId;
    // Begin Motorola
    private boolean mIsIcsImport;
    private boolean mIsDialog;
    private int mAttendeeResponse;
    // End Motorola

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Get the info needed for the fragment
        Intent intent = getIntent();
        mEventId = 0;
        // Begin Motorola
        mAttendeeResponse = Attendees.ATTENDEE_STATUS_NONE;
        mIsDialog = false;
        // End Motorola

        if (icicle != null) {
            mEventId = icicle.getLong(EventInfoFragment.BUNDLE_KEY_EVENT_ID);
            mStartMillis = icicle.getLong(EventInfoFragment.BUNDLE_KEY_START_MILLIS);
            mEndMillis = icicle.getLong(EventInfoFragment.BUNDLE_KEY_END_MILLIS);
            // Begin Motorola
            mAttendeeResponse = icicle.getInt(EventInfoFragment.BUNDLE_KEY_ATTENDEE_RESPONSE);
            mIsDialog = icicle.getBoolean(EventInfoFragment.BUNDLE_KEY_IS_DIALOG);
            mIsIcsImport = icicle.getBoolean(EventInfoFragment.BUNDLE_KEY_IS_ICS_IMPORT);
            // End Motorola
        } else if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            mStartMillis = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, 0);
            mEndMillis = intent.getLongExtra(EXTRA_EVENT_END_TIME, 0);
            // Begin Motorola
            mAttendeeResponse = intent.getIntExtra(EVENT_ATTENDEE_RESPONSE,  Attendees.ATTENDEE_STATUS_NONE);
            mIsIcsImport = intent.getBooleanExtra(IcsConstants.EXTRA_IMPORT_ICS, false);
            // End Motorola
            Uri data = intent.getData();
            if (data != null) {
                try {
                    mEventId = Long.parseLong(data.getLastPathSegment());
                } catch (NumberFormatException e) {
                    Log.wtf(TAG,"No event id");
                }
            }
        }

        // If we do not support showing full screen event info in this configuration,
        // close the activity and show the event in AllInOne.
        Resources res = getResources();
        if (!res.getBoolean(R.bool.agenda_show_event_info_full_screen)
                && !res.getBoolean(R.bool.show_event_info_full_screen)) {
            CalendarController.getInstance(this)
                    .launchViewEvent(mEventId, mStartMillis, mEndMillis, mAttendeeResponse);
            finish();
            return;
        }

        setContentView(R.layout.simple_frame_layout);

        // Get the fragment if exists
        mInfoFragment = (EventInfoFragment)
                getFragmentManager().findFragmentById(R.id.main_frame);


        // Remove the application title
        ActionBar bar = getActionBar();
        if (bar != null) {
            bar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME);
        }

        // Create a new fragment if none exists
        if (mInfoFragment == null) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction ft = fragmentManager.beginTransaction();
            // Begin Motorola
            Bundle extras = new Bundle();
            extras.putBoolean(IcsConstants.EXTRA_IMPORT_ICS, mIsIcsImport);
            extras.putBoolean("is_taskify", intent.getBooleanExtra("is_taskify", false));
            mInfoFragment = new EventInfoFragment(this, mEventId, mStartMillis, mEndMillis,
                    mAttendeeResponse, mIsDialog, mIsDialog ?
                            EventInfoFragment.DIALOG_WINDOW_STYLE :
                                EventInfoFragment.FULL_WINDOW_STYLE, extras);
            // End Motorola
            ft.replace(R.id.main_frame, mInfoFragment);
            ft.commit();
        }
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//
//        // Handles option menu selections:
//        // Home button - close event info activity and start the main calendar one
//        // Edit button - start the event edit activity and close the info activity
//        // Delete button - start a delete query that calls a runnable that close the info activity
//
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                Intent launchIntent = new Intent();
//                launchIntent.setAction(Intent.ACTION_VIEW);
//                launchIntent.setData(Uri.parse(CalendarContract.CONTENT_URI + "/time"));
//                launchIntent.setFlags(
//                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(launchIntent);
//                finish();
//                return true;
//            case R.id.info_action_edit:
//                Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
//                Intent intent = new Intent(Intent.ACTION_EDIT, uri);
//                intent.putExtra(EXTRA_EVENT_BEGIN_TIME, mStartMillis);
//                intent.putExtra(EXTRA_EVENT_END_TIME, mEndMillis);
//                intent.setClass(this, EditEventActivity.class);
//                intent.putExtra(EVENT_EDIT_ON_LAUNCH, true);
//                startActivity(intent);
//                finish ();
//                break;
//            case R.id.info_action_delete:
//                DeleteEventHelper deleteHelper = new DeleteEventHelper(
//                        this, this, true /* exitWhenDone */);
//                deleteHelper.delete(mStartMillis, mEndMillis, mEventId, -1, onDeleteRunnable);
//                break;
//            default:
//                break;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    // runs at the end of a delete action and closes the activity
//    private Runnable onDeleteRunnable = new Runnable() {
//        @Override
//        public void run() {
//            finish ();
//        }
//    };

    @Override
    protected void onNewIntent(Intent intent) {
        // From the Android Dev Guide: "It's important to note that when
        // onNewIntent(Intent) is called, the Activity has not been restarted,
        // so the getIntent() method will still return the Intent that was first
        // received with onCreate(). This is why setIntent(Intent) is called
        // inside onNewIntent(Intent) (just in case you call getIntent() at a
        // later time)."
        setIntent(intent);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Begin Motorola
        outState.putLong(EventInfoFragment.BUNDLE_KEY_EVENT_ID, mEventId);
        outState.putLong(EventInfoFragment.BUNDLE_KEY_START_MILLIS, mStartMillis);
        outState.putLong(EventInfoFragment.BUNDLE_KEY_END_MILLIS, mEndMillis);
        outState.putBoolean(EventInfoFragment.BUNDLE_KEY_IS_DIALOG, mIsDialog);
        outState.putInt(EventInfoFragment.BUNDLE_KEY_ATTENDEE_RESPONSE, mAttendeeResponse);
        outState.putBoolean(EventInfoFragment.BUNDLE_KEY_IS_ICS_IMPORT, mIsIcsImport);
        // End Motorola
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Begin Motorola - IKHSS7-8425: Fix memory leak issue.
        try {
            // To avoid memory leak, remove the activity instance from the CalendarController no matter it is registered or not.
            CalendarController.removeInstance(this);
        } catch (Exception e) {
        }
        // End Motorola - IKHSS7-8425
    }
}
