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

import static android.provider.CalendarContract.EXTRA_EVENT_ALL_DAY;
import static android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME;
import static android.provider.CalendarContract.EXTRA_EVENT_END_TIME;
import static android.provider.CalendarContract.Attendees.ATTENDEE_STATUS;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SearchView;
import android.widget.SearchView.OnSuggestionListener;
import android.widget.TextView;

import com.android.calendar.CalendarController.EventHandler;
import com.android.calendar.CalendarController.EventInfo;
import com.android.calendar.CalendarController.EventType;
import com.android.calendar.CalendarController.ViewType;
import com.android.calendar.agenda.AgendaFragment;
import com.android.calendar.ics.IcsConstants;
import com.android.calendar.month.MonthByWeekFragment;
import com.android.calendar.selectcalendars.SelectVisibleCalendarsFragment;
import com.android.calendarcommon.DateException;
import com.android.calendarcommon.EventRecurrence;
import com.android.calendarcommon.RecurrenceProcessor;
import com.android.calendarcommon.RecurrenceSet;
import com.android.calendar.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AllInOneActivity extends Activity implements EventHandler,
        OnSharedPreferenceChangeListener, SearchView.OnQueryTextListener, ActionBar.TabListener,
        ActionBar.OnNavigationListener, OnSuggestionListener {
    private static final String TAG = "AllInOneActivity";
    private static final boolean DEBUG = false;
    private static final String EVENT_INFO_FRAGMENT_TAG = "EventInfoFragment";
    private static final String BUNDLE_KEY_RESTORE_TIME = "key_restore_time";
    private static final String BUNDLE_KEY_EVENT_ID = "key_event_id";
    private static final String BUNDLE_KEY_RESTORE_VIEW = "key_restore_view";
    private static final String BUNDLE_KEY_CHECK_ACCOUNTS = "key_check_for_accounts";
    private static final int HANDLER_KEY = 0;
    private static float mScale = 0;

    // Indices of buttons for the drop down menu (tabs replacement)
    // Must match the strings in the array buttons_list in arrays.xml and the
    // OnNavigationListener
    private static final int BUTTON_DAY_INDEX = 0;
    private static final int BUTTON_WEEK_INDEX = 1;
    private static final int BUTTON_MONTH_INDEX = 2;
    private static final int BUTTON_AGENDA_INDEX = 3;

    private CalendarController mController;
    private static boolean mIsMultipane;
    private static boolean mIsTabletConfig;
    private static boolean mShowAgendaWithMonth;
    private static boolean mShowEventDetailsWithAgenda;
    private boolean mOnSaveInstanceStateCalled = false;
    private boolean mBackToPreviousView = false;
    private ContentResolver mContentResolver;
    private int mPreviousView;
    private int mCurrentView;
    private boolean mPaused = true;
    private boolean mUpdateOnResume = false;
    private boolean mHideControls = false;
    private boolean mShowSideViews = true;
    private boolean mShowWeekNum = false;
    private TextView mHomeTime;
    private TextView mDateRange;
    private TextView mWeekTextView;
    private View mMiniMonth;
    private View mCalendarsList;
    private View mMiniMonthContainer;
    private View mSecondaryPane;
    private String mTimeZone;
    private boolean mShowCalendarControls;
    private boolean mShowEventInfoFullScreenAgenda;
    private boolean mShowEventInfoFullScreen;
    private int mWeekNum;
    private int mCalendarControlsAnimationTime;
    private int mControlsAnimateWidth;
    private int mControlsAnimateHeight;

    private long mViewEventId = -1;
    private long mIntentEventStartMillis = -1;
    private long mIntentEventEndMillis = -1;
    private int mIntentAttendeeResponse = Attendees.ATTENDEE_STATUS_NONE;
    private boolean mIntentAllDay = false;
    // Begin Motorola
    private static final String CTRL_EVT_EXTRA_KEY_TASKIFY = "is_taskify";
    private boolean mIsIcsImport = false;
    // The flag indicates it is launched from Tasks app.
    private boolean mTaskifyFlag = false;
    private static final int DIALOG_GO_TO_DATE = 0;
    // End Morotola
    // Action bar and Navigation bar (left side of Action bar)
    private ActionBar mActionBar;
    private ActionBar.Tab mDayTab;
    private ActionBar.Tab mWeekTab;
    private ActionBar.Tab mMonthTab;
    private ActionBar.Tab mAgendaTab;
    private SearchView mSearchView;
    private MenuItem mSearchMenu;
    private MenuItem mControlsMenu;
    private Menu mOptionsMenu;
    private CalendarViewAdapter mActionBarMenuSpinnerAdapter;
    private QueryHandler mHandler;
    private boolean mCheckForAccounts = true;

    private String mHideString;
    private String mShowString;

    DayOfMonthDrawable mDayOfMonthIcon;

    int mOrientation;
    
    //Motorola Begin - Lunar Calendar
    public static boolean mShowLunar;
    public static boolean mShowHoliday;
    //Motorola End - Lunar Calendar

    // Params for animating the controls on the right
    private LayoutParams mControlsParams;
    private LinearLayout.LayoutParams mVerticalControlsParams;

    private final AnimatorListener mSlideAnimationDoneListener = new AnimatorListener() {

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationEnd(android.animation.Animator animation) {
            int visibility = mShowSideViews ? View.VISIBLE : View.GONE;
            mMiniMonth.setVisibility(visibility);
            mCalendarsList.setVisibility(visibility);
            mMiniMonthContainer.setVisibility(visibility);
        }

        @Override
        public void onAnimationRepeat(android.animation.Animator animation) {
        }

        @Override
        public void onAnimationStart(android.animation.Animator animation) {
        }
    };

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            mCheckForAccounts = false;
            try {
                // If the query didn't return a cursor for some reason return
                if (cursor == null || cursor.getCount() > 0 || isFinishing()) {
                    return;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            Bundle options = new Bundle();
            options.putCharSequence("introMessage",
                    getResources().getString(R.string.create_an_account_desc));
            options.putBoolean("allowSkip", true);

            AccountManager am = AccountManager.get(AllInOneActivity.this);
            am.addAccount("com.google", CalendarContract.AUTHORITY, null, options,
                    AllInOneActivity.this,
                    new AccountManagerCallback<Bundle>() {
                        @Override
                        public void run(AccountManagerFuture<Bundle> future) {
                            if (future.isCancelled()) {
                                return;
                            }
                            try {
                                Bundle result = future.getResult();
                                boolean setupSkipped = result.getBoolean("setupSkipped");

                                if (setupSkipped) {
                                    Utils.setSharedPreference(AllInOneActivity.this,
                                            GeneralPreferences.KEY_SKIP_SETUP, true);
                                }

                            } catch (OperationCanceledException ignore) {
                                // The account creation process was canceled
                            } catch (IOException ignore) {
                            } catch (AuthenticatorException ignore) {
                            }
                        }
                    }, null);
        }
    }

    private final Runnable mHomeTimeUpdater = new Runnable() {
        @Override
        public void run() {
            mTimeZone = Utils.getTimeZone(AllInOneActivity.this, mHomeTimeUpdater);
            updateSecondaryTitleFields(-1);
            AllInOneActivity.this.invalidateOptionsMenu();
            Utils.setMidnightUpdater(mHandler, mTimeChangesUpdater, mTimeZone);
        }
    };

    // runs every midnight/time changes and refreshes the today icon
    private final Runnable mTimeChangesUpdater = new Runnable() {
        @Override
        public void run() {
            mTimeZone = Utils.getTimeZone(AllInOneActivity.this, mHomeTimeUpdater);
            AllInOneActivity.this.invalidateOptionsMenu();
            Utils.setMidnightUpdater(mHandler, mTimeChangesUpdater, mTimeZone);
        }
    };


    // Create an observer so that we can update the views whenever a
    // Calendar event changes.
    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            eventsChanged();
        }
    };

    BroadcastReceiver mCalIntentReceiver;

    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (DEBUG)
            Log.d(TAG, "New intent received " + intent.toString());
        // Don't change the date if we're just returning to the app's home
        if (Intent.ACTION_VIEW.equals(action)
                && !intent.getBooleanExtra(Utils.INTENT_KEY_HOME, false)) {
            long millis = parseViewAction(intent);
            if (millis == -1) {
                // modified by amt_sunli 2012-12-17 SWITCHUITWO-241 begin
                long beginTime = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, -1);
                if (beginTime < -1) {
                    millis = beginTime;
                } else {
                    millis = Utils.timeFromIntentInMillis(intent);
                }
                // modified by amt_sunli 2012-12-17 SWITCHUITWO-241 end
            }
            if (millis != -1 && mViewEventId == -1 && mController != null) {
                Time time = new Time(mTimeZone);
                time.set(millis);
                time.normalize(true);
                mController.sendEvent(this, EventType.GO_TO, time, time, -1, ViewType.CURRENT);
            }
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        if (Utils.getSharedPreference(this, OtherPreferences.KEY_OTHER_1, false)) {
            setTheme(R.style.CalendarTheme_WithActionBarWallpaper);
        }
        super.onCreate(icicle);

        if (icicle != null && icicle.containsKey(BUNDLE_KEY_CHECK_ACCOUNTS)) {
            mCheckForAccounts = icicle.getBoolean(BUNDLE_KEY_CHECK_ACCOUNTS);
        }
        // Launch add google account if this is first time and there are no
        // accounts yet
        if (mCheckForAccounts
                && !Utils.getSharedPreference(this, GeneralPreferences.KEY_SKIP_SETUP, false)) {

            mHandler = new QueryHandler(this.getContentResolver());
            mHandler.startQuery(0, null, Calendars.CONTENT_URI, new String[] {
                Calendars._ID
            }, null, null /* selection args */, null /* sort order */);
        }

        // This needs to be created before setContentView
        mController = CalendarController.getInstance(this);


        // Get time from intent or icicle
        long timeMillis = -1;
        int viewType = -1;
        final Intent intent = getIntent();
        if (icicle != null) {
            timeMillis = icicle.getLong(BUNDLE_KEY_RESTORE_TIME);
            viewType = icicle.getInt(BUNDLE_KEY_RESTORE_VIEW, -1);
        } else {
            String action = intent.getAction();
            if (Intent.ACTION_VIEW.equals(action)) {
                // Open EventInfo later
                timeMillis = parseViewAction(intent);
            }

            if (timeMillis == -1) {
                timeMillis = Utils.timeFromIntentInMillis(intent);
            }
        }

        if (viewType == -1) {
            viewType = Utils.getViewTypeFromIntentAndSharedPref(this);
        }
        mTimeZone = Utils.getTimeZone(this, mHomeTimeUpdater);
        Time t = new Time(mTimeZone);
        t.set(timeMillis);

        if (DEBUG) {
            if (icicle != null && intent != null) {
                Log.d(TAG, "both, icicle:" + icicle.toString() + "  intent:" + intent.toString());
            } else {
                Log.d(TAG, "not both, icicle:" + icicle + " intent:" + intent);
            }
        }

        Resources res = getResources();
        mHideString = res.getString(R.string.hide_controls);
        mShowString = res.getString(R.string.show_controls);
        mOrientation = res.getConfiguration().orientation;
        if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            mControlsAnimateWidth = (int)res.getDimension(R.dimen.calendar_controls_width);
            if (mControlsParams == null) {
                mControlsParams = new LayoutParams(mControlsAnimateWidth, 0);
            }
            mControlsParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        } else {
            // Make sure width is in between allowed min and max width values
            mControlsAnimateWidth = Math.max(res.getDisplayMetrics().widthPixels * 45 / 100,
                    (int)res.getDimension(R.dimen.min_portrait_calendar_controls_width));
            mControlsAnimateWidth = Math.min(mControlsAnimateWidth,
                    (int)res.getDimension(R.dimen.max_portrait_calendar_controls_width));
        }

        mControlsAnimateHeight = (int)res.getDimension(R.dimen.calendar_controls_height);

        mHideControls = !Utils.getSharedPreference(
                this, GeneralPreferences.KEY_SHOW_CONTROLS, true);
        mIsMultipane = Utils.getConfigBool(this, R.bool.multiple_pane_config);
        mIsTabletConfig = Utils.getConfigBool(this, R.bool.tablet_config);
        mShowAgendaWithMonth = Utils.getConfigBool(this, R.bool.show_agenda_with_month);
        mShowCalendarControls =
                Utils.getConfigBool(this, R.bool.show_calendar_controls);
        mShowEventDetailsWithAgenda =
            Utils.getConfigBool(this, R.bool.show_event_details_with_agenda);
        mShowEventInfoFullScreenAgenda =
            Utils.getConfigBool(this, R.bool.agenda_show_event_info_full_screen);
        mShowEventInfoFullScreen =
            Utils.getConfigBool(this, R.bool.show_event_info_full_screen);
        mCalendarControlsAnimationTime = res.getInteger(R.integer.calendar_controls_animation_time);
        Utils.setAllowWeekForDetailView(mIsMultipane);

        // setContentView must be called before configureActionBar
        setContentView(R.layout.all_in_one);

        if (mIsTabletConfig) {
            mDateRange = (TextView) findViewById(R.id.date_bar);
            mWeekTextView = (TextView) findViewById(R.id.week_num);
        } else {
            mDateRange = (TextView) getLayoutInflater().inflate(R.layout.date_range_title, null);
        }

        // configureActionBar auto-selects the first tab you add, so we need to
        // call it before we set up our own fragments to make sure it doesn't
        // overwrite us
        configureActionBar(viewType);

        mHomeTime = (TextView) findViewById(R.id.home_time);
        mMiniMonth = findViewById(R.id.mini_month);
        if (mIsTabletConfig && mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            mMiniMonth.setLayoutParams(new LinearLayout.LayoutParams(mControlsAnimateWidth,
                    mControlsAnimateHeight));
        }
        mCalendarsList = findViewById(R.id.calendar_list);
        mMiniMonthContainer = findViewById(R.id.mini_month_container);
        mSecondaryPane = findViewById(R.id.secondary_pane);

        // Must register as the first activity because this activity can modify
        // the list of event handlers in it's handle method. This affects who
        // the rest of the handlers the controller dispatches to are.
        mController.registerFirstEventHandler(HANDLER_KEY, this);

        initFragments(timeMillis, viewType, icicle);

        // Listen for changes that would require this to be refreshed
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        
        //Lunar Calendar
        mShowLunar = prefs.getBoolean(GeneralPreferences.KEY_SHOW_LUNAR,
                GeneralPreferences.DEFAULT_VALUE_OF_SHOW_LUNAR);
        mShowHoliday = prefs.getBoolean(GeneralPreferences.KEY_SHOW_HOLIDAY,
                GeneralPreferences.DEFAULT_VALUE_OF_SHOW_HOLIDAY);
        
        mContentResolver = getContentResolver();
        // added by amt_sunli 2012-12-06 SWITCHUITWO-64 begin
        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        Utils.setScreenHeight(dm.heightPixels);
        Utils.setScreenWidth(dm.widthPixels);
        // added by amt_sunli 2012-12-06 SWITCHUITWO-64 end
    }

    private long parseViewAction(final Intent intent) {
        long timeMillis = -1;
        Uri data = intent.getData();
        if (data != null && data.isHierarchical()) {
            List<String> path = data.getPathSegments();
            if (path.size() == 2 && path.get(0).equals("events")) {
                try {
                    mViewEventId = Long.valueOf(data.getLastPathSegment());
                    if (mViewEventId != -1) {
                        mIntentEventStartMillis = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, 0);
                        mIntentEventEndMillis = intent.getLongExtra(EXTRA_EVENT_END_TIME, 0);
                        mIntentAttendeeResponse = intent.getIntExtra(
                                ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_NONE);
                        mIntentAllDay = intent.getBooleanExtra(EXTRA_EVENT_ALL_DAY, false);
                        timeMillis = mIntentEventStartMillis;
                        // Begin Motorola
                        mIsIcsImport = intent.getBooleanExtra(IcsConstants.EXTRA_IMPORT_ICS, false);

                        // When launching from Tasks app, the intent data is the event Uri with query parameters (TASKIFY_FLAG, startMillis and endMillis)
                        String strTaskifyFlag = data.getQueryParameter(Utils.TASKIFY_FLAG);
                        if (strTaskifyFlag != null) {
                            mTaskifyFlag = Boolean.valueOf(strTaskifyFlag);
                        }
                        if (mTaskifyFlag) {
                            // For launching from Tasks app, the Uri should always include startMillis/endMillis query parameters
                            String beginTimeParam = data.getQueryParameter(EXTRA_EVENT_BEGIN_TIME);
                            String endTimeParam = data.getQueryParameter(EXTRA_EVENT_END_TIME);
                            if (beginTimeParam != null) {
                                mIntentEventStartMillis = Long.valueOf(beginTimeParam);
                            }
                            if (endTimeParam != null) {
                                mIntentEventEndMillis = Long.valueOf(endTimeParam);
                            }
                            if (!checkTaskifyEventConsistency()) {
                                timeMillis = -1;
                            }
                        }
                        // End Motorola
                    }
                } catch (NumberFormatException e) {
                    // Ignore if mViewEventId can't be parsed
                }
            }
        }
        return timeMillis;
    }

    private void configureActionBar(int viewType) {
        createButtonsSpinner(viewType, mIsTabletConfig);
        if (mIsMultipane) {
            mActionBar.setDisplayOptions(
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME);
        } else {
            mActionBar.setDisplayOptions(0);
        }
    }

    private void createTabs() {
        mActionBar = getActionBar();
        if (mActionBar == null) {
            Log.w(TAG, "ActionBar is null.");
        } else {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            mDayTab = mActionBar.newTab();
            mDayTab.setText(getString(R.string.day_view));
            mDayTab.setTabListener(this);
            mActionBar.addTab(mDayTab);
            mWeekTab = mActionBar.newTab();
            mWeekTab.setText(getString(R.string.week_view));
            mWeekTab.setTabListener(this);
            mActionBar.addTab(mWeekTab);
            mMonthTab = mActionBar.newTab();
            mMonthTab.setText(getString(R.string.month_view));
            mMonthTab.setTabListener(this);
            mActionBar.addTab(mMonthTab);
            mAgendaTab = mActionBar.newTab();
            mAgendaTab.setText(getString(R.string.agenda_view));
            mAgendaTab.setTabListener(this);
            mActionBar.addTab(mAgendaTab);
        }
    }

    private void createButtonsSpinner(int viewType, boolean tabletConfig) {
        // If tablet configuration , show spinner with no dates
        mActionBarMenuSpinnerAdapter = new CalendarViewAdapter (this, viewType, !tabletConfig);
        mActionBar = getActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        mActionBar.setListNavigationCallbacks(mActionBarMenuSpinnerAdapter, this);
        switch (viewType) {
            case ViewType.AGENDA:
                mActionBar.setSelectedNavigationItem(BUTTON_AGENDA_INDEX);
                break;
            case ViewType.DAY:
                mActionBar.setSelectedNavigationItem(BUTTON_DAY_INDEX);
                break;
            case ViewType.WEEK:
                mActionBar.setSelectedNavigationItem(BUTTON_WEEK_INDEX);
                break;
            case ViewType.MONTH:
                mActionBar.setSelectedNavigationItem(BUTTON_MONTH_INDEX);
                break;
            default:
                mActionBar.setSelectedNavigationItem(BUTTON_DAY_INDEX);
                break;
       }
    }
    // Clear buttons used in the agenda view
    private void clearOptionsMenu() {
        if (mOptionsMenu == null) {
            return;
        }
        MenuItem cancelItem = mOptionsMenu.findItem(R.id.action_cancel);
        if (cancelItem != null) {
            cancelItem.setVisible(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Must register as the first activity because this activity can modify
        // the list of event handlers in it's handle method. This affects who
        // the rest of the handlers the controller dispatches to are.
        mController.registerFirstEventHandler(HANDLER_KEY, this);

        mOnSaveInstanceStateCalled = false;
        mContentResolver.registerContentObserver(CalendarContract.Events.CONTENT_URI,
                true, mObserver);
        if (mUpdateOnResume) {
            initFragments(mController.getTime(), mController.getViewType(), null);
            mUpdateOnResume = false;
        }
        Time t = new Time(mTimeZone);
        t.set(mController.getTime());
        mController.sendEvent(this, EventType.UPDATE_TITLE, t, t, -1, ViewType.CURRENT,
                mController.getDateFlags(), null, null);
        // Make sure the drop-down menu will get its date updated at midnight
        if (mActionBarMenuSpinnerAdapter != null) {
            mActionBarMenuSpinnerAdapter.refresh(this);
        }

        if (mControlsMenu != null) {
            mControlsMenu.setTitle(mHideControls ? mShowString : mHideString);
        }
        mPaused = false;

        if (mViewEventId != -1 && mIntentEventStartMillis != -1 && mIntentEventEndMillis != -1) {
            long currentMillis = System.currentTimeMillis();
            long selectedTime = -1;
            if (currentMillis > mIntentEventStartMillis && currentMillis < mIntentEventEndMillis) {
                selectedTime = currentMillis;
            }
            // Begin Motorola
//          mController.sendEventRelatedEventWithExtra(this, EventType.VIEW_EVENT, mViewEventId,
//          mIntentEventStartMillis, mIntentEventEndMillis, -1, -1,
//          EventInfo.buildViewExtraLong(mIntentAttendeeResponse,mIntentAllDay),
//          selectedTime);
            Bundle extras = new Bundle();
            extras.putBoolean(IcsConstants.EXTRA_IMPORT_ICS, mIsIcsImport);
            extras.putBoolean(CTRL_EVT_EXTRA_KEY_TASKIFY, mTaskifyFlag);
            mController.sendEventRelatedEventWithExtra(this, EventType.VIEW_EVENT, mViewEventId,
                    mIntentEventStartMillis, mIntentEventEndMillis, -1, -1,
                    mIntentAttendeeResponse, selectedTime, extras);
            mIsIcsImport = false;
            mTaskifyFlag = false;
            // End Motorola
            mViewEventId = -1;
            mIntentEventStartMillis = -1;
            mIntentEventEndMillis = -1;
            mIntentAllDay = false;
        }
        Utils.setMidnightUpdater(mHandler, mTimeChangesUpdater, mTimeZone);
        // Make sure the today icon is up to date
        invalidateOptionsMenu();

        mCalIntentReceiver = Utils.setTimeChangesReceiver(this, mTimeChangesUpdater);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mController.deregisterEventHandler(HANDLER_KEY);
        mPaused = true;
        mHomeTime.removeCallbacks(mHomeTimeUpdater);
        if (mActionBarMenuSpinnerAdapter != null) {
            mActionBarMenuSpinnerAdapter.onPause();
        }
        mContentResolver.unregisterContentObserver(mObserver);
        if (isFinishing()) {
            // Stop listening for changes that would require this to be refreshed
            SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this);
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        // FRAG_TODO save highlighted days of the week;
        if (mController.getViewType() != ViewType.EDIT) {
            Utils.setDefaultView(this, mController.getViewType());
        }
        Utils.resetMidnightUpdater(mHandler, mTimeChangesUpdater);
        Utils.clearTimeChangesReceiver(this, mCalIntentReceiver);
    }

    @Override
    protected void onUserLeaveHint() {
        mController.sendEvent(this, EventType.USER_HOME, null, null, -1, ViewType.CURRENT);
        super.onUserLeaveHint();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mOnSaveInstanceStateCalled = true;
        super.onSaveInstanceState(outState);

        outState.putLong(BUNDLE_KEY_RESTORE_TIME, mController.getTime());
        outState.putInt(BUNDLE_KEY_RESTORE_VIEW, mCurrentView);
        if (mCurrentView == ViewType.EDIT) {
            outState.putLong(BUNDLE_KEY_EVENT_ID, mController.getEventId());
        }
        outState.putBoolean(BUNDLE_KEY_CHECK_ACCOUNTS, mCheckForAccounts);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        mController.deregisterAllEventHandlers();

        CalendarController.removeInstance(this);
    }

    // Begin Motorola
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_GO_TO_DATE:
            // modified by amt_sunli 2013-1-21 SWITCHUITWO-546 begin
            Time t = new Time(mTimeZone);
            t.setToNow();
            DatePickerDialog dpd = new DatePickerDialog(
                    this, new DateListener(), t.year, t.month, t.monthDay);
            // modified by amt_sunli 2013-1-21 SWITCHUITWO-546 end
            dpd.setTitle(getString(R.string.go_to_date));
            dpd.setButton(DatePickerDialog.BUTTON_POSITIVE, getString(R.string.go_label), dpd);
            return dpd;
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        switch (id) {
        case DIALOG_GO_TO_DATE:
            DatePickerDialog dpd = (DatePickerDialog) dialog;
            Time t = new Time(mTimeZone);
            t.setToNow();
            DatePicker dp = dpd.getDatePicker();
            dp.updateDate(t.year, t.month, t.monthDay);
            CalendarView cv = dpd.getDatePicker().getCalendarView();
            cv.setShowWeekNumber(Utils.getShowWeekNumber(this));
            int startOfWeek = Utils.getFirstDayOfWeek(this);
            // Utils returns Time days while CalendarView wants Calendar days
            if (startOfWeek == Time.SATURDAY) {
                startOfWeek = java.util.Calendar.SATURDAY;
            } else if (startOfWeek == Time.SUNDAY) {
                startOfWeek = java.util.Calendar.SUNDAY;
            } else {
                startOfWeek = java.util.Calendar.MONDAY;
            }
            cv.setFirstDayOfWeek(startOfWeek);
            break;
        }
    }
    // End Motorola
    private void initFragments(long timeMillis, int viewType, Bundle icicle) {
        if (DEBUG) {
            Log.d(TAG, "Initializing to " + timeMillis + " for view " + viewType);
        }
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        if (mShowCalendarControls) {
            Fragment miniMonthFrag = new MonthByWeekFragment(timeMillis, true);
            ft.replace(R.id.mini_month, miniMonthFrag);
            mController.registerEventHandler(R.id.mini_month, (EventHandler) miniMonthFrag);

            Fragment selectCalendarsFrag = new SelectVisibleCalendarsFragment();
            ft.replace(R.id.calendar_list, selectCalendarsFrag);
            mController.registerEventHandler(
                    R.id.calendar_list, (EventHandler) selectCalendarsFrag);
        }
        if (!mShowCalendarControls || viewType == ViewType.EDIT) {
            mMiniMonth.setVisibility(View.GONE);
            mCalendarsList.setVisibility(View.GONE);
        }

        EventInfo info = null;
        if (viewType == ViewType.EDIT) {
            mPreviousView = GeneralPreferences.getSharedPreferences(this).getInt(
                    GeneralPreferences.KEY_START_VIEW, GeneralPreferences.DEFAULT_START_VIEW);

            long eventId = -1;
            Intent intent = getIntent();
            Uri data = intent.getData();
            if (data != null) {
                try {
                    eventId = Long.parseLong(data.getLastPathSegment());
                } catch (NumberFormatException e) {
                    if (DEBUG) {
                        Log.d(TAG, "Create new event");
                    }
                }
            } else if (icicle != null && icicle.containsKey(BUNDLE_KEY_EVENT_ID)) {
                eventId = icicle.getLong(BUNDLE_KEY_EVENT_ID);
            }

            long begin = intent.getLongExtra(EXTRA_EVENT_BEGIN_TIME, -1);
            long end = intent.getLongExtra(EXTRA_EVENT_END_TIME, -1);
            info = new EventInfo();
            if (end != -1) {
                info.endTime = new Time();
                info.endTime.set(end);
            }
            if (begin != -1) {
                info.startTime = new Time();
                info.startTime.set(begin);
            }
            info.id = eventId;
            // We set the viewtype so if the user presses back when they are
            // done editing the controller knows we were in the Edit Event
            // screen. Likewise for eventId
            mController.setViewType(viewType);
            mController.setEventId(eventId);
        } else {
            mPreviousView = viewType;
        }

        setMainPane(ft, R.id.main_pane, viewType, timeMillis, true);
        ft.commit(); // this needs to be after setMainPane()

        Time t = new Time(mTimeZone);
        t.set(timeMillis);
        if (viewType != ViewType.EDIT) {
            mController.sendEvent(this, EventType.GO_TO, t, null, -1, viewType);
        }
    }

    @Override
    public void onBackPressed() {
        if (mCurrentView == ViewType.EDIT || mBackToPreviousView) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, mPreviousView);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.all_in_one_title_bar, menu);

        mSearchMenu = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) mSearchMenu.getActionView();
        if (mSearchView != null) {
            Utils.setUpSearchView(mSearchView, this);
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setOnSuggestionListener(this);
        }
        //Motorola Begin
        // lunar calendar picker, it's enabled when the language set to Chin.
        MenuItem lunarPickMenu = menu.findItem(R.id.action_lunar_picker);
        if (lunarPickMenu != null) {
            if (Utils.isDefaultLocaleChinese()) {
                lunarPickMenu.setVisible(true);
                lunarPickMenu.setEnabled(true);
            } else {
                lunarPickMenu.setVisible(false);
                lunarPickMenu.setEnabled(false);
            }
        }
        //Motorola End
        
        // Hide the "show/hide controls" button if this is a phone
        // or the view type is "Month" or "Agenda".

        mControlsMenu = menu.findItem(R.id.action_hide_controls);
        if (!mShowCalendarControls) {
            if (mControlsMenu != null) {
                mControlsMenu.setVisible(false);
                mControlsMenu.setEnabled(false);
            }
        } else if (mControlsMenu != null && mController != null
                    && (mController.getViewType() == ViewType.MONTH ||
                        mController.getViewType() == ViewType.AGENDA)) {
            mControlsMenu.setVisible(false);
            mControlsMenu.setEnabled(false);
        } else if (mControlsMenu != null){
            mControlsMenu.setTitle(mHideControls ? mShowString : mHideString);
        }

        // replace the default top layer drawable of the today icon with a custom drawable
        // that shows the day of the month of today
        LayerDrawable icon = (LayerDrawable)menu.findItem(R.id.action_today).getIcon();
        Utils.setTodayIcon(icon, this, mTimeZone);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Time t = null;
        int viewType = ViewType.CURRENT;
        long extras = CalendarController.EXTRA_GOTO_TIME;
        switch (item.getItemId()) {
            case R.id.action_refresh:
                mController.refreshCalendars();
                return true;
            case R.id.action_today:
                viewType = ViewType.CURRENT;
                t = new Time(mTimeZone);
                t.setToNow();
                extras |= CalendarController.EXTRA_GOTO_TODAY;
                break;
            case R.id.action_create_event:
                t = new Time();
                t.set(mController.getTime());
                if (t.minute > 30) {
                    t.hour++;
                    t.minute = 0;
                } else if (t.minute > 0 && t.minute < 30) {
                    t.minute = 30;
                }
                mController.sendEventRelatedEvent(
                        this, EventType.CREATE_EVENT, -1, t.toMillis(true), 0, 0, 0, -1);
                return true;
            case R.id.action_select_visible_calendars:
                mController.sendEvent(this, EventType.LAUNCH_SELECT_VISIBLE_CALENDARS, null, null,
                        0, 0);
                return true;
            //Begin Motorola -lunar calendar
            case R.id.action_lunar_picker:
                viewType = ViewType.CURRENT;
                mController.sendEvent(this, EventType.LUNAR_PICKER, null, null, 0, 0);
                break;
            //End Motorola
            case R.id.action_settings:
                mController.sendEvent(this, EventType.LAUNCH_SETTINGS, null, null, 0, 0);
                return true;
            case R.id.action_hide_controls:
                mHideControls = !mHideControls;
                Utils.setSharedPreference(
                        this, GeneralPreferences.KEY_SHOW_CONTROLS, !mHideControls);
                item.setTitle(mHideControls ? mShowString : mHideString);
                if (!mHideControls) {
                    mMiniMonth.setVisibility(View.VISIBLE);
                    mCalendarsList.setVisibility(View.VISIBLE);
                    mMiniMonthContainer.setVisibility(View.VISIBLE);
                }
                final ObjectAnimator slideAnimation = ObjectAnimator.ofInt(this, "controlsOffset",
                        mHideControls ? 0 : mControlsAnimateWidth,
                        mHideControls ? mControlsAnimateWidth : 0);
                slideAnimation.setDuration(mCalendarControlsAnimationTime);
                ObjectAnimator.setFrameDelay(0);
                slideAnimation.start();
                return true;
            case R.id.action_search:
                return false;
            // Begin Motorola
            case R.id.action_go_to_date:
                viewType = ViewType.CURRENT;
                showDialog(DIALOG_GO_TO_DATE);
                return true;
            // End Motorola
            default:
                return false;
        }
        mController.sendEvent(this, EventType.GO_TO, t, null, t, -1, viewType, extras, null, null);
        return true;
    }

    /**
     * Sets the offset of the controls on the right for animating them off/on
     * screen. ProGuard strips this if it's not in proguard.flags
     *
     * @param controlsOffset The current offset in pixels
     */
    public void setControlsOffset(int controlsOffset) {
        if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            mMiniMonth.setTranslationX(controlsOffset);
            mCalendarsList.setTranslationX(controlsOffset);
            mControlsParams.width = Math.max(0, mControlsAnimateWidth - controlsOffset);
            mMiniMonthContainer.setLayoutParams(mControlsParams);
        } else {
            mMiniMonth.setTranslationY(controlsOffset);
            mCalendarsList.setTranslationY(controlsOffset);
            if (mVerticalControlsParams == null) {
                mVerticalControlsParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, mControlsAnimateHeight);
            }
            mVerticalControlsParams.height = Math.max(0, mControlsAnimateHeight - controlsOffset);
            mMiniMonthContainer.setLayoutParams(mVerticalControlsParams);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(GeneralPreferences.KEY_WEEK_START_DAY)) {
            if (mPaused) {
                mUpdateOnResume = true;
            } else {
                initFragments(mController.getTime(), mController.getViewType(), null);
            }
        }
        //Begin Motorola - Lunar Calendar
        if (key.equals(GeneralPreferences.KEY_SHOW_LUNAR)) {
            boolean newSetting = prefs.getBoolean(key,
                    GeneralPreferences.DEFAULT_VALUE_OF_SHOW_LUNAR);
            if (mShowLunar != newSetting) {
                mShowLunar = newSetting;
                Log.d(TAG, "  ShowLunar setting changed!!!");
            }
        }
        if (key.equals(GeneralPreferences.KEY_SHOW_HOLIDAY)) {// fwr687
                                                                // IKSF-3317
            boolean newSetting = prefs.getBoolean(key,
                    GeneralPreferences.DEFAULT_VALUE_OF_SHOW_HOLIDAY);
            if (mShowHoliday != newSetting) {
                mShowHoliday = newSetting;
                Log.d(TAG, "  ShowHoliday setting changed!!!");
            }
        }
        //End Motorola - Lunar Calendar
    }

    private void setMainPane(
            FragmentTransaction ft, int viewId, int viewType, long timeMillis, boolean force) {
        if (mOnSaveInstanceStateCalled) {
            return;
        }
        if (!force && mCurrentView == viewType) {
            return;
        }

        // Remove this when transition to and from month view looks fine.
        boolean doTransition = viewType != ViewType.MONTH && mCurrentView != ViewType.MONTH;
        FragmentManager fragmentManager = getFragmentManager();
        // Check if our previous view was an Agenda view
        // TODO remove this if framework ever supports nested fragments
        if (mCurrentView == ViewType.AGENDA) {
            // If it was, we need to do some cleanup on it to prevent the
            // edit/delete buttons from coming back on a rotation.
            Fragment oldFrag = fragmentManager.findFragmentById(viewId);
            if (oldFrag instanceof AgendaFragment) {
                ((AgendaFragment) oldFrag).removeFragments(fragmentManager);
            }
        }

        if (viewType != mCurrentView) {
            // The rules for this previous view are different than the
            // controller's and are used for intercepting the back button.
            if (mCurrentView != ViewType.EDIT && mCurrentView > 0) {
                mPreviousView = mCurrentView;
            }
            mCurrentView = viewType;
        }
        // Create new fragment
        Fragment frag = null;
        Fragment secFrag = null;
        switch (viewType) {
            case ViewType.AGENDA:
                if (mActionBar != null && (mActionBar.getSelectedTab() != mAgendaTab)) {
                    mActionBar.selectTab(mAgendaTab);
                }
                if (mActionBarMenuSpinnerAdapter != null) {
                    mActionBar.setSelectedNavigationItem(CalendarViewAdapter.AGENDA_BUTTON_INDEX);
                }
                frag = new AgendaFragment(timeMillis, false);
                break;
            case ViewType.DAY:
                if (mActionBar != null && (mActionBar.getSelectedTab() != mDayTab)) {
                    mActionBar.selectTab(mDayTab);
                }
                if (mActionBarMenuSpinnerAdapter != null) {
                    mActionBar.setSelectedNavigationItem(CalendarViewAdapter.DAY_BUTTON_INDEX);
                }
                frag = new DayFragment(timeMillis, 1);
                break;
            case ViewType.WEEK:
                if (mActionBar != null && (mActionBar.getSelectedTab() != mWeekTab)) {
                    mActionBar.selectTab(mWeekTab);
                }
                if (mActionBarMenuSpinnerAdapter != null) {
                    mActionBar.setSelectedNavigationItem(CalendarViewAdapter.WEEK_BUTTON_INDEX);
                }
                frag = new DayFragment(timeMillis, 7);
                break;
            case ViewType.MONTH:
                if (mActionBar != null && (mActionBar.getSelectedTab() != mMonthTab)) {
                    mActionBar.selectTab(mMonthTab);
                }
                if (mActionBarMenuSpinnerAdapter != null) {
                    mActionBar.setSelectedNavigationItem(CalendarViewAdapter.MONTH_BUTTON_INDEX);
                }
                frag = new MonthByWeekFragment(timeMillis, false);
                if (mShowAgendaWithMonth) {
                    secFrag = new AgendaFragment(timeMillis, false);
                }
                break;
            default:
                throw new IllegalArgumentException(
                        "Must be Agenda, Day, Week, or Month ViewType, not " + viewType);
        }

        // Update the current view so that the menu can update its look according to the
        // current view.
        if (mActionBarMenuSpinnerAdapter != null) {
            mActionBarMenuSpinnerAdapter.setMainView(viewType);
            if (!mIsTabletConfig) {
                mActionBarMenuSpinnerAdapter.setTime(timeMillis);
            }
        }


        // Show date only on tablet configurations in views different than Agenda
        if (!mIsTabletConfig) {
            mDateRange.setVisibility(View.GONE);
        } else if (viewType != ViewType.AGENDA) {
            mDateRange.setVisibility(View.VISIBLE);
        } else {
            mDateRange.setVisibility(View.GONE);
        }

        // Clear unnecessary buttons from the option menu when switching from the agenda view
        if (viewType != ViewType.AGENDA) {
            clearOptionsMenu();
        }

        boolean doCommit = false;
        if (ft == null) {
            doCommit = true;
            ft = fragmentManager.beginTransaction();
        }

        if (doTransition) {
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }

        ft.replace(viewId, frag);
        if (mShowAgendaWithMonth) {

            // Show/hide secondary fragment

            if (secFrag != null) {
                ft.replace(R.id.secondary_pane, secFrag);
                mSecondaryPane.setVisibility(View.VISIBLE);
            } else {
                mSecondaryPane.setVisibility(View.GONE);
                Fragment f = fragmentManager.findFragmentById(R.id.secondary_pane);
                if (f != null) {
                    ft.remove(f);
                }
                mController.deregisterEventHandler(R.id.secondary_pane);
            }
        }
        if (DEBUG) {
            Log.d(TAG, "Adding handler with viewId " + viewId + " and type " + viewType);
        }
        // If the key is already registered this will replace it
        mController.registerEventHandler(viewId, (EventHandler) frag);
        if (secFrag != null) {
            mController.registerEventHandler(viewId, (EventHandler) secFrag);
        }

        if (doCommit) {
            if (DEBUG) {
                Log.d(TAG, "setMainPane AllInOne=" + this + " finishing:" + this.isFinishing());
            }
            ft.commit();
        }
    }

    private void setTitleInActionBar(EventInfo event) {
        if (event.eventType != EventType.UPDATE_TITLE || mActionBar == null) {
            return;
        }

        final long start = event.startTime.toMillis(false /* use isDst */);
        final long end;
        if (event.endTime != null) {
            end = event.endTime.toMillis(false /* use isDst */);
        } else {
            end = start;
        }

        final String msg = Utils.formatDateRange(this, start, end, (int) event.extraLong);
        CharSequence oldDate = mDateRange.getText();
        //Begin Motorola - Luanr Calendar
        if (Utils.isDefaultLocaleChinese() && mShowLunar) {
            Time t = new Time(mTimeZone);
            t.set(event.selectedTime.toMillis(true));
            String lunarString = msg + Utils.lunarYear(this, t);
            mDateRange.setText(lunarString);
        }
        //End Motorola - Lunar Calendar
        updateSecondaryTitleFields(event.selectedTime != null ? event.selectedTime.toMillis(true)
                : start);
        if (!TextUtils.equals(oldDate, msg)) {
            mDateRange.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            if (mShowWeekNum && mWeekTextView != null) {
                mWeekTextView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            }
        }
    }

    private void updateSecondaryTitleFields(long visibleMillisSinceEpoch) {
        mShowWeekNum = Utils.getShowWeekNumber(this);
        mTimeZone = Utils.getTimeZone(this, mHomeTimeUpdater);
        if (visibleMillisSinceEpoch != -1) {
            int weekNum = Utils.getWeekNumberFromTime(visibleMillisSinceEpoch, this);
            mWeekNum = weekNum;
        }

        if (mShowWeekNum && (mCurrentView == ViewType.WEEK) && mIsTabletConfig
                && mWeekTextView != null) {
            String weekString = getResources().getQuantityString(R.plurals.weekN, mWeekNum,
                    mWeekNum);
            mWeekTextView.setText(weekString);
            mWeekTextView.setVisibility(View.VISIBLE);
        } else if (visibleMillisSinceEpoch != -1 && mWeekTextView != null
                && mCurrentView == ViewType.DAY && mIsTabletConfig) {
            Time time = new Time(mTimeZone);
            time.set(visibleMillisSinceEpoch);
            int julianDay = Time.getJulianDay(visibleMillisSinceEpoch, time.gmtoff);
            time.setToNow();
            int todayJulianDay = Time.getJulianDay(time.toMillis(false), time.gmtoff);
            String dayString = Utils.getDayOfWeekString(julianDay, todayJulianDay,
                    visibleMillisSinceEpoch, this);
            mWeekTextView.setText(dayString);
            mWeekTextView.setVisibility(View.VISIBLE);
        } else if (mWeekTextView != null && (!mIsTabletConfig || mCurrentView != ViewType.DAY)) {
            mWeekTextView.setVisibility(View.GONE);
        }

        if (mHomeTime != null
                && (mCurrentView == ViewType.DAY || mCurrentView == ViewType.WEEK
                        || mCurrentView == ViewType.AGENDA)
                && !TextUtils.equals(mTimeZone, Time.getCurrentTimezone())) {
            Time time = new Time(mTimeZone);
            time.setToNow();
            long millis = time.toMillis(true);
            boolean isDST = time.isDst != 0;
            int flags = DateUtils.FORMAT_SHOW_TIME;
            if (DateFormat.is24HourFormat(this)) {
                flags |= DateUtils.FORMAT_24HOUR;
            }
            // Formats the time as
            String timeString = (new StringBuilder(
                    Utils.formatDateRange(this, millis, millis, flags))).append(" ").append(
                    TimeZone.getTimeZone(mTimeZone).getDisplayName(
                            isDST, TimeZone.SHORT, Locale.getDefault())).toString();
            mHomeTime.setText(timeString);
            mHomeTime.setVisibility(View.VISIBLE);
            // Update when the minute changes
            mHomeTime.removeCallbacks(mHomeTimeUpdater);
            mHomeTime.postDelayed(
                    mHomeTimeUpdater,
                    DateUtils.MINUTE_IN_MILLIS - (millis % DateUtils.MINUTE_IN_MILLIS));
        } else if (mHomeTime != null) {
            mHomeTime.setVisibility(View.GONE);
        }
    }

    @Override
    public long getSupportedEventTypes() {
        return EventType.GO_TO | EventType.VIEW_EVENT | EventType.UPDATE_TITLE;
    }

    @Override
    public void handleEvent(EventInfo event) {
        long displayTime = -1;
        if (event.eventType == EventType.GO_TO) {
            if ((event.extraLong & CalendarController.EXTRA_GOTO_BACK_TO_PREVIOUS) != 0) {
                mBackToPreviousView = true;
            } else if (event.viewType != mController.getPreviousViewType()
                    && event.viewType != ViewType.EDIT) {
                // Clear the flag is change to a different view type
                mBackToPreviousView = false;
            }

            setMainPane(
                    null, R.id.main_pane, event.viewType, event.startTime.toMillis(false), false);
            if (mSearchView != null) {
                mSearchView.clearFocus();
            }
            if (mShowCalendarControls) {
                int animationSize = (mOrientation == Configuration.ORIENTATION_LANDSCAPE) ?
                        mControlsAnimateWidth : mControlsAnimateHeight;
                boolean noControlsView = event.viewType == ViewType.MONTH || event.viewType == ViewType.AGENDA;
                if (mControlsMenu != null) {
                    mControlsMenu.setVisible(!noControlsView);
                    mControlsMenu.setEnabled(!noControlsView);
                }
                if (noControlsView || mHideControls) {
                    // hide minimonth and calendar frag
                    mShowSideViews = false;
                    if (!mHideControls) {
                            final ObjectAnimator slideAnimation = ObjectAnimator.ofInt(this,
                                    "controlsOffset", 0, animationSize);
                            slideAnimation.addListener(mSlideAnimationDoneListener);
                            slideAnimation.setDuration(mCalendarControlsAnimationTime);
                            ObjectAnimator.setFrameDelay(0);
                            slideAnimation.start();
                    } else {
                        mMiniMonth.setVisibility(View.GONE);
                        mCalendarsList.setVisibility(View.GONE);
                        mMiniMonthContainer.setVisibility(View.GONE);
                    }
                } else {
                    // show minimonth and calendar frag
                    mShowSideViews = true;
                    mMiniMonth.setVisibility(View.VISIBLE);
                    mCalendarsList.setVisibility(View.VISIBLE);
                    mMiniMonthContainer.setVisibility(View.VISIBLE);
                    if (!mHideControls &&
                            (mController.getPreviousViewType() == ViewType.MONTH ||
                             mController.getPreviousViewType() == ViewType.AGENDA)) {
                        final ObjectAnimator slideAnimation = ObjectAnimator.ofInt(this,
                                "controlsOffset", animationSize, 0);
                        slideAnimation.setDuration(mCalendarControlsAnimationTime);
                        ObjectAnimator.setFrameDelay(0);
                        slideAnimation.start();
                    }
                }
            }
            displayTime = event.selectedTime != null ? event.selectedTime.toMillis(true)
                    : event.startTime.toMillis(true);
            if (!mIsTabletConfig) {
                mActionBarMenuSpinnerAdapter.setTime(displayTime);
            }
        } else if (event.eventType == EventType.VIEW_EVENT) {

            // If in Agenda view and "show_event_details_with_agenda" is "true",
            // do not create the event info fragment here, it will be created by the Agenda
            // fragment

            // Begin Motorola
//          if (mCurrentView == ViewType.AGENDA && mShowEventDetailsWithAgenda) {
            // For iCal-imported event, we always need show the preview screen.
            if (!event.extras.getBoolean(IcsConstants.EXTRA_IMPORT_ICS, false) && !event.extras.getBoolean(CTRL_EVT_EXTRA_KEY_TASKIFY, false)
                    && mCurrentView == ViewType.AGENDA && mShowEventDetailsWithAgenda) {
            // End Motorola
                if (event.startTime != null && event.endTime != null) {
                    // Event is all day , adjust the goto time to local time
                    if (event.isAllDay()) {
                        Utils.convertAlldayUtcToLocal(
                                event.startTime, event.startTime.toMillis(false), mTimeZone);
                        Utils.convertAlldayUtcToLocal(
                                event.endTime, event.endTime.toMillis(false), mTimeZone);
                    }
                    mController.sendEvent(this, EventType.GO_TO, event.startTime, event.endTime,
                            event.id, ViewType.AGENDA);
                } else if (event.selectedTime != null) {
                    mController.sendEvent(this, EventType.GO_TO, event.selectedTime,
                        event.selectedTime, event.id, ViewType.AGENDA);
                }
            } else {
                // TODO Fix the temp hack below: && mCurrentView !=
                // ViewType.AGENDA
                if (event.selectedTime != null && mCurrentView != ViewType.AGENDA) {
                    mController.sendEvent(this, EventType.GO_TO, event.selectedTime,
                            event.selectedTime, -1, ViewType.CURRENT);
                }
                int response = event.getResponse();
                if ((mCurrentView == ViewType.AGENDA && mShowEventInfoFullScreenAgenda) ||
                        ((mCurrentView == ViewType.DAY || (mCurrentView == ViewType.WEEK) ||
                                mCurrentView == ViewType.MONTH) && mShowEventInfoFullScreen)){
                    // start event info as activity
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, event.id);
                    intent.setData(eventUri);
                    intent.setClass(this, EventInfoActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra(EXTRA_EVENT_BEGIN_TIME, event.startTime.toMillis(false));
                    intent.putExtra(EXTRA_EVENT_END_TIME, event.endTime.toMillis(false));
                    intent.putExtra(ATTENDEE_STATUS, response);
                    // Begin Motorola
                    intent.putExtra(IcsConstants.EXTRA_IMPORT_ICS, event.extras.getBoolean(IcsConstants.EXTRA_IMPORT_ICS, false));
                    intent.putExtra(CTRL_EVT_EXTRA_KEY_TASKIFY, event.extras.getBoolean(CTRL_EVT_EXTRA_KEY_TASKIFY, false));
                    // End Motorola
                    startActivity(intent);
                    // Begin Motorola
                    // Finish itself if it's an iCal-imported event
                    if (event.extras.getBoolean(IcsConstants.EXTRA_IMPORT_ICS, false) || event.extras.getBoolean(CTRL_EVT_EXTRA_KEY_TASKIFY, false)) {
                        finish();
                    }
                    // End Motorola
                } else {
                    // start event info as a dialog
                    // Begin Motorola
                    EventInfoFragment fragment = new EventInfoFragment(this,
                            event.id, event.startTime.toMillis(false),
                            event.endTime.toMillis(false), (int) event.extraLong, true,
                            EventInfoFragment.DIALOG_WINDOW_STYLE, event.extras);
//                    EventInfoFragment fragment = new EventInfoFragment(this,
//                            event.id, event.startTime.toMillis(false),
//                            event.endTime.toMillis(false), response, true,
//                            EventInfoFragment.DIALOG_WINDOW_STYLE);
                    // End Motorola
                    fragment.setDialogParams(event.x, event.y, mActionBar.getHeight());
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    // if we have an old popup replace it
                    Fragment fOld = fm.findFragmentByTag(EVENT_INFO_FRAGMENT_TAG);
                    if (fOld != null && fOld.isAdded()) {
                        ft.remove(fOld);
                    }
                    ft.add(fragment, EVENT_INFO_FRAGMENT_TAG);
                    ft.commit();
                }
            }
            displayTime = event.startTime.toMillis(true);
        } else if (event.eventType == EventType.UPDATE_TITLE) {
            setTitleInActionBar(event);
            if (!mIsTabletConfig) {
                mActionBarMenuSpinnerAdapter.setTime(mController.getTime());
            }
        }
        updateSecondaryTitleFields(displayTime);
    }

    // Needs to be in proguard whitelist
    // Specified as listener via android:onClick in a layout xml
    public void handleSelectSyncedCalendarsClicked(View v) {
        mController.sendEvent(this, EventType.LAUNCH_SETTINGS, null, null, null, 0, 0,
                CalendarController.EXTRA_GOTO_TIME, null,
                null);
    }

    @Override
    public void eventsChanged() {
        mController.sendEvent(this, EventType.EVENTS_CHANGED, null, null, -1, ViewType.CURRENT);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if ("TARDIS".equalsIgnoreCase(query)) {
            Utils.tardis();
        }
        mSearchMenu.collapseActionView();
        mController.sendEvent(this, EventType.SEARCH, null, null, -1, ViewType.CURRENT, 0, query,
                getComponentName());
        return true;
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        Log.w(TAG, "TabSelected AllInOne=" + this + " finishing:" + this.isFinishing());
        if (tab == mDayTab && mCurrentView != ViewType.DAY) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.DAY);
        } else if (tab == mWeekTab && mCurrentView != ViewType.WEEK) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.WEEK);
        } else if (tab == mMonthTab && mCurrentView != ViewType.MONTH) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.MONTH);
        } else if (tab == mAgendaTab && mCurrentView != ViewType.AGENDA) {
            mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.AGENDA);
        } else {
            Log.w(TAG, "TabSelected event from unknown tab: "
                    + (tab == null ? "null" : tab.getText()));
            Log.w(TAG, "CurrentView:" + mCurrentView + " Tab:" + tab.toString() + " Day:" + mDayTab
                    + " Week:" + mWeekTab + " Month:" + mMonthTab + " Agenda:" + mAgendaTab);
        }
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }


    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        switch (itemPosition) {
            case CalendarViewAdapter.DAY_BUTTON_INDEX:
                if (mCurrentView != ViewType.DAY) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.DAY);
                }
                break;
            case CalendarViewAdapter.WEEK_BUTTON_INDEX:
                if (mCurrentView != ViewType.WEEK) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.WEEK);
                }
                break;
            case CalendarViewAdapter.MONTH_BUTTON_INDEX:
                if (mCurrentView != ViewType.MONTH) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.MONTH);
                }
                break;
            case CalendarViewAdapter.AGENDA_BUTTON_INDEX:
                if (mCurrentView != ViewType.AGENDA) {
                    mController.sendEvent(this, EventType.GO_TO, null, null, -1, ViewType.AGENDA);
                }
                break;
            default:
                Log.w(TAG, "ItemSelected event from unknown button: " + itemPosition);
                Log.w(TAG, "CurrentView:" + mCurrentView + " Button:" + itemPosition +
                        " Day:" + mDayTab + " Week:" + mWeekTab + " Month:" + mMonthTab +
                        " Agenda:" + mAgendaTab);
                break;
        }
        return false;
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        mSearchMenu.collapseActionView();
        return false;
    }

    @Override
    public boolean onSearchRequested() {
        if (mSearchMenu != null) {
            mSearchMenu.expandActionView();
        }
        return false;
    }
    // Begin Motorola
    private class DateListener implements OnDateSetListener {
        public void onDateSet(DatePicker view, int year, int month, int monthDay) {
            int viewType = ViewType.CURRENT;
            Time t = new Time(mTimeZone);
            t.year = year;
            t.month = month;
            t.monthDay = monthDay;
            t.normalize(true);
            mController.sendEvent(AllInOneActivity.this, EventType.GO_TO, t, null, -1, viewType);
        }
    }

    private boolean checkTaskifyEventConsistency() {
        if (mViewEventId < 0) {
            // Need not care more, just go ahead.
            return true;
        }

        boolean isException = false;
        Cursor c = initEvtCursorForTaskifyConsistency(mViewEventId);
        if ((c == null) || (c.getCount() <= 0)) {
            Log.v(TAG, "The event has been deleted from the db.");
            relEvtCursorForTaskifyConsistencyAndFinish(c);
            return false;
        }
        c.moveToFirst();

        long calendarId = c.getLong(TASKIFY_EVENT_INDEX_CALENDAR_ID);
        String syncId = c.getString(TASKIFY_EVENT_INDEX_SYNC_ID);
        String rRule = c.getString(TASKIFY_EVENT_INDEX_RRULE);
        boolean isRepeating = (rRule != null && rRule.length() > 0);

        // For non-repeating event, should use the real dtstart and dtend insteand of those from intent.
        if (!isRepeating) {
            mIntentEventStartMillis = c.getLong(TASKIFY_EVENT_INDEX_DTSTART);
            mIntentEventEndMillis = c.getLong(TASKIFY_EVENT_INDEX_DTEND);
        }

        // Check if there is corresponding exception event for recurrence event instance.
        // Note, the syncId should be non-null when checking.
        if (isRepeating && !TextUtils.isEmpty(syncId)) {
            Long exceptionId = Utils.getRecurrenceException(this, calendarId, syncId, mIntentEventStartMillis);
            if (exceptionId != null) {
                // Should use the exception event instead of the parent event.
                c.close();
                c = initEvtCursorForTaskifyConsistency(exceptionId);
                if ((c == null) || (c.getCount() <= 0)) {
                    Log.v(TAG, "The exception event has been deleted from the db.");
                    relEvtCursorForTaskifyConsistencyAndFinish(c);
                    return false;
                }
                c.moveToFirst();

                // Re-get the values since there is requery.
                calendarId = c.getLong(TASKIFY_EVENT_INDEX_CALENDAR_ID);
                syncId = c.getString(TASKIFY_EVENT_INDEX_SYNC_ID);
                rRule = c.getString(TASKIFY_EVENT_INDEX_RRULE);
                isRepeating = (rRule != null && rRule.length() > 0);

                // For non-repeating event, should use the real dtstart and dtend insteand of those from intent.
                if (!isRepeating) {
                    mIntentEventStartMillis = c.getLong(TASKIFY_EVENT_INDEX_DTSTART);
                    mIntentEventEndMillis = c.getLong(TASKIFY_EVENT_INDEX_DTEND);
                }

                mViewEventId = exceptionId;
                isException = true;
            }
        }

        // Check if the event is set to DELETED but still in db.
        boolean deleted = c.getInt(TASKIFY_EVENT_INDEX_DELETED) != 0;
        if (deleted) {
            Log.v(TAG, "The event is in db but with DELETED flag.");
            relEvtCursorForTaskifyConsistencyAndFinish(c);
            return false;
        }

        // For the deleted exception, the event status is CANCELED.
        boolean canceled = c.getInt(TASKIFY_EVENT_INDEX_STATUS) == Events.STATUS_CANCELED;
        if (isException && canceled) {
            Log.v(TAG, "The exception event is in db but with CANCELED flag.");
            relEvtCursorForTaskifyConsistencyAndFinish(c);
            return false;
        }

        // Make sure the instance is in the range of the repeating event.
        // Consider it when delete or modify this and all following instances of a repeating event.
        if (isRepeating) {
            boolean isInstanceInRange = isInstanceInRange(c, mIntentEventStartMillis);
            if (!isInstanceInRange) {
                Log.v(TAG, "The instance is not in the range of a repeating event.");
                relEvtCursorForTaskifyConsistencyAndFinish(c);
                return false;
            }
        }

        if (c != null) {
            c.close();
        }
        return true;
    }

    private Cursor initEvtCursorForTaskifyConsistency(long eventId) {
        Log.v(TAG, "initEvtCursorForTaskifyConsistency() for event " + eventId);
        Uri eventUri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId);
        ContentResolver cr = getContentResolver();
        Cursor c = cr.query(eventUri, TASKIFY_EVENT_PROJECTION, null, null, null);
        return c;
    }

    private void relEvtCursorForTaskifyConsistencyAndFinish(Cursor c) {
        Log.v(TAG, "relEvtCursorForTaskifyConsistencyAndFinish()");

        Toast.makeText(this, R.string.event_has_been_deleted, Toast.LENGTH_SHORT).show();
        if (c != null) {
            c.close();
        }

        finish();
    }

    /**
     * Check if the instance is in the range of the occurences of a repeating event.
     * @param eventCursor the event cursor.
     * @param instanceStartTime the instance start time.
     * @return whether the instance is in the range.
     */
    private static boolean isInstanceInRange(Cursor eventCursor, long instanceStartTime) {
        RecurrenceSet recur = null;
        try {
            recur = new RecurrenceSet(eventCursor);
        } catch (EventRecurrence.InvalidFormatException e) {
            Log.w(TAG, "Could not parse RRULE recurrence string " + e);
            return false;
        }

        if (null == recur || !recur.hasRecurrence()) {
            return false;
        } else {
            int dtStartMillisColumn = eventCursor.getColumnIndex(Events.DTSTART);
            int tzColumn = eventCursor.getColumnIndex(Events.EVENT_TIMEZONE);
            long dtStartMillis = eventCursor.getLong(dtStartMillisColumn);
            String tz = eventCursor.getString(tzColumn);

            if (instanceStartTime < dtStartMillis) {
                return false;
            }

            if (TextUtils.isEmpty(tz)) {
                tz = Time.TIMEZONE_UTC;
            }
            Time dtStart = new Time(tz);
            dtStart.set(dtStartMillis);

            RecurrenceProcessor rp = new RecurrenceProcessor();
            long lastMillis = 0;
            try {
                lastMillis = rp.getLastOccurence(dtStart, recur);
            } catch (DateException e) {
                Log.w(TAG, "RecurrenceProcessor error ", e);
                return false;
            }

            if (lastMillis == -1) {
                return true; // Repeat forever
            } else if (lastMillis == 0) {
                return false; // No occurences and don't repeat forever
            } else if (instanceStartTime <= lastMillis) {
                return true;
            } else {
                return false;
            }
        }
    }


    private static final String[] TASKIFY_EVENT_PROJECTION = new String[] {
        Events._ID, // 0
        Events.RRULE, // 1
        Events.RDATE, // 2
        Events.EXRULE, // 3
        Events.EXDATE, // 4
        Events.DELETED, // 5
        Events.DTSTART, // 6
        Events.DTEND, // 7
        Events.EVENT_TIMEZONE, // 8
        Events.STATUS, // 9
        Events.CALENDAR_ID, // 10
        Events._SYNC_ID, // 11
    };

    private static final int TASKIFY_EVENT_INDEX_ID = 0;
    private static final int TASKIFY_EVENT_INDEX_RRULE = 1;
    private static final int TASKIFY_EVENT_INDEX_RDATE = 2;
    private static final int TASKIFY_EVENT_INDEX_EXRULE = 3;
    private static final int TASKIFY_EVENT_INDEX_EXDATE = 4;
    private static final int TASKIFY_EVENT_INDEX_DELETED = 5;
    private static final int TASKIFY_EVENT_INDEX_DTSTART = 6;
    private static final int TASKIFY_EVENT_INDEX_DTEND = 7;
    private static final int TASKIFY_EVENT_INDEX_TIMEZONE = 8;
    private static final int TASKIFY_EVENT_INDEX_STATUS = 9;
    private static final int TASKIFY_EVENT_INDEX_CALENDAR_ID = 10;
    private static final int TASKIFY_EVENT_INDEX_SYNC_ID = 11;
    // End Motorola
}
