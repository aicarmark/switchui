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
import static com.android.calendar.CalendarController.EVENT_EDIT_ON_LAUNCH;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.QuickContact;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.text.util.Rfc822Token;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.calendar.CalendarController.EventInfo;
import com.android.calendar.CalendarController.EventType;
import com.android.calendar.CalendarEventModel.Attendee;
import com.android.calendar.CalendarEventModel.ReminderEntry;
import com.android.calendar.confinfo.ConferenceCallInfo;
import com.android.calendar.confinfo.ConferenceCallInfoDataMgr;
import com.android.calendar.confinfo.ConferenceCallInfoSettingMgr;
import com.android.calendar.confinfo.ConferenceCallOrganizerInfoActivity;
import com.android.calendar.confinfo.ConferenceCallPreferenceDialogBuilder;
import com.android.calendar.confinfo.PhoneProjection;
import com.android.calendar.event.AttendeesView;
import com.android.calendar.event.EditEventActivity;
import com.android.calendar.event.EditEventHelper;
import com.android.calendar.event.EventAttendeeActivity;
import com.android.calendar.event.EventViewUtils;
import com.android.calendar.event.EventAttendeeActivity.EventAttendee;
import com.android.calendarcommon.EventRecurrence;
import com.android.calendar.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
//Begin Motorola
import com.android.calendar.ics.IcsConstants;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.CalendarContract.ExtendedProperties;
import java.io.File;
import com.motorola.calendarcommon.vcal.common.VCalConstants;
import com.motorola.calendarcommon.vcal.VCalUtils;
//End Motorola

public class EventInfoFragment extends DialogFragment implements OnCheckedChangeListener,
        CalendarController.EventHandler, OnClickListener, DeleteEventHelper.DeleteNotifyListener,OnSharedPreferenceChangeListener {

    public static final boolean DEBUG = false;

    public static final String TAG = "EventInfoFragment";

    protected static final String BUNDLE_KEY_EVENT_ID = "key_event_id";
    protected static final String BUNDLE_KEY_START_MILLIS = "key_start_millis";
    protected static final String BUNDLE_KEY_END_MILLIS = "key_end_millis";
    protected static final String BUNDLE_KEY_IS_DIALOG = "key_fragment_is_dialog";
    protected static final String BUNDLE_KEY_DELETE_DIALOG_VISIBLE = "key_delete_dialog_visible";
    protected static final String BUNDLE_KEY_WINDOW_STYLE = "key_window_style";
    protected static final String BUNDLE_KEY_ATTENDEE_RESPONSE = "key_attendee_response";
    // Begin motorola
    protected static final String BUNDLE_KEY_CHANGE_RESPONE_DIALOG_VISIBLE = "key_change_response_dialog_visible";
    protected static final String BUNDLE_KEY_EVENT_LOCATION = "key_fragment_event_location";
    protected static final String BUNDLE_KEY_ORIGINAL_EMAIL = "key_fragment_original_email";
    protected static final String BUNDLE_KEY_SYNC_ACCOUNT_TYPE = "key_sync_account_type";
    protected static final String BUNDLE_KEY_DELETE_OPTION_INDEX = "key_event_option_index";
    protected static final String BUNDLE_KEY_CHANGE_RESPONE_OPTION_INDEX = "key_change_respone_index";
    protected static final String BUNDLE_KEY_CONFINFO_CONTENT = "key_confinfo_content";
    protected static final String BUNDLE_KEY_IS_ICS_IMPORT = "key_fragment_is_ics_import";
    private static final String MIME_TYPE_MSG_RFC822 = "message/rfc822";
    private static final String ANDROID_EMAIL_PKG = "com.android.email";
    private static final String GOOGLE_EMAIL_PKG = "com.google.android.email";
    private static final String GMAIL_PKG = "com.google.android.gm";
    private static final String GOOGLE_ACCOUNT_TYPE = "com.google";
    private static final String EXCHANGE_ACCOUNT_TYPE = "com.android.exchange";
    private static final String COMPOSE_ACCT_ID = "account_id";

    private static final int ACTION_REPLY = 1;
    private static final int ACTION_REPLYALL = 2;
    // End motorola

    private static final String PERIOD_SPACE = ". ";

    /**
     * These are the corresponding indices into the array of strings
     * "R.array.change_response_labels" in the resource file.
     */
    static final int UPDATE_SINGLE = 0;
    static final int UPDATE_ALL = 1;

    // Style of view
    public static final int FULL_WINDOW_STYLE = 0;
    public static final int DIALOG_WINDOW_STYLE = 1;
    /*2012-11-27, add by amt_jiayuanyuan for SWITCHUITWO-125 */
    private boolean mIsEditEvent;
    /*2012-11-27, add end*/
    private int mWindowStyle = DIALOG_WINDOW_STYLE;

    // Query tokens for QueryHandler
    private static final int TOKEN_QUERY_EVENT = 1 << 0;
    private static final int TOKEN_QUERY_CALENDARS = 1 << 1;
    private static final int TOKEN_QUERY_ATTENDEES = 1 << 2;
    private static final int TOKEN_QUERY_DUPLICATE_CALENDARS = 1 << 3;
    private static final int TOKEN_QUERY_REMINDERS = 1 << 4;
    // Begin Motorola
    private static final int TOKEN_QUERY_EXTENDED_PROPERTIES = 1 << 5;
    private static final int TOKEN_QUERY_CONFERENCEINFO = 1 << 6;
    //Begin Motorola - IKHSS6-8628 ANR caused by main query
    private static final int TOKEN_QUERY_CONTACTINFO = 1 << 7;
    //End Motorola - IKHSS6-8628
    private static final int TOKEN_QUERY_ALL = TOKEN_QUERY_DUPLICATE_CALENDARS
            | TOKEN_QUERY_ATTENDEES | TOKEN_QUERY_CALENDARS | TOKEN_QUERY_EVENT
            | TOKEN_QUERY_REMINDERS | TOKEN_QUERY_EXTENDED_PROPERTIES;
    // End Motorola
    private int mCurrentQuery = 0;

    private static final String[] EVENT_PROJECTION = new String[] {
        Events._ID,                  // 0  do not remove; used in DeleteEventHelper
        Events.TITLE,                // 1  do not remove; used in DeleteEventHelper
        Events.RRULE,                // 2  do not remove; used in DeleteEventHelper
        Events.ALL_DAY,              // 3  do not remove; used in DeleteEventHelper
        Events.CALENDAR_ID,          // 4  do not remove; used in DeleteEventHelper
        Events.DTSTART,              // 5  do not remove; used in DeleteEventHelper
        Events._SYNC_ID,             // 6  do not remove; used in DeleteEventHelper
        Events.EVENT_TIMEZONE,       // 7  do not remove; used in DeleteEventHelper
        Events.DESCRIPTION,          // 8
        Events.EVENT_LOCATION,       // 9
        Calendars.CALENDAR_ACCESS_LEVEL,      // 10
        Events.DISPLAY_COLOR,                 // 11
        Events.HAS_ATTENDEE_DATA,    // 12
        Events.ORGANIZER,            // 13
        Events.HAS_ALARM,            // 14
        Calendars.MAX_REMINDERS,     //15
        Calendars.ALLOWED_REMINDERS, // 16
        Events.CUSTOM_APP_PACKAGE,   // 17
        Events.CUSTOM_APP_URI,       // 18
        Events.ORIGINAL_SYNC_ID,     // 19 do not remove; used in DeleteEventHelper
        // Begin Motorola
        Events.DURATION,             // 20
        Events.AVAILABILITY,         // 21
        Events.DELETED,              // 22
        Events.DTEND,                // 23
        Events.STATUS,               // 24
        // End Motorola
    };
    private static final int EVENT_INDEX_ID = 0;
    private static final int EVENT_INDEX_TITLE = 1;
    private static final int EVENT_INDEX_RRULE = 2;
    private static final int EVENT_INDEX_ALL_DAY = 3;
    private static final int EVENT_INDEX_CALENDAR_ID = 4;
    private static final int EVENT_INDEX_DTSTART = 5;
    private static final int EVENT_INDEX_SYNC_ID = 6;
    private static final int EVENT_INDEX_EVENT_TIMEZONE = 7;
    private static final int EVENT_INDEX_DESCRIPTION = 8;
    private static final int EVENT_INDEX_EVENT_LOCATION = 9;
    private static final int EVENT_INDEX_ACCESS_LEVEL = 10;
    private static final int EVENT_INDEX_COLOR = 11;
    private static final int EVENT_INDEX_HAS_ATTENDEE_DATA = 12;
    private static final int EVENT_INDEX_ORGANIZER = 13;
    private static final int EVENT_INDEX_HAS_ALARM = 14;
    private static final int EVENT_INDEX_MAX_REMINDERS = 15;
    private static final int EVENT_INDEX_ALLOWED_REMINDERS = 16;
    private static final int EVENT_INDEX_CUSTOM_APP_PACKAGE = 17;
    private static final int EVENT_INDEX_CUSTOM_APP_URI = 18;
    // Begin Motorola
    private static final int EVENT_INDEX_ORIGINAL_SYNC_ID = 19;
    private static final int EVENT_INDEX_DURATION = 20;
    private static final int EVENT_INDEX_AVAILABILITY = 21;
    private static final int EVENT_INDEX_DELETED = 22;
    private static final int EVENT_INDEX_DTEND = 23;
    private static final int EVENT_INDEX_STATUS = 24;
    // End Motorola


    private static final String[] ATTENDEES_PROJECTION = new String[] {
        Attendees._ID,                      // 0
        Attendees.ATTENDEE_NAME,            // 1
        Attendees.ATTENDEE_EMAIL,           // 2
        Attendees.ATTENDEE_RELATIONSHIP,    // 3
        Attendees.ATTENDEE_STATUS,          // 4
        Attendees.ATTENDEE_IDENTITY,        // 5
        Attendees.ATTENDEE_ID_NAMESPACE,     // 6
        // Begin Motorola
        Attendees.ATTENDEE_TYPE,            // 7
        // End Motorola
    };
    private static final int ATTENDEES_INDEX_ID = 0;
    private static final int ATTENDEES_INDEX_NAME = 1;
    private static final int ATTENDEES_INDEX_EMAIL = 2;
    private static final int ATTENDEES_INDEX_RELATIONSHIP = 3;
    private static final int ATTENDEES_INDEX_STATUS = 4;
    private static final int ATTENDEES_INDEX_IDENTITY = 5;
    private static final int ATTENDEES_INDEX_ID_NAMESPACE = 6;
    // Begin Motorola
    private static final int ATTENDEES_INDEX_TYPE = 7;
    // End Motorola

    private static final String ATTENDEES_WHERE = Attendees.EVENT_ID + "=?";

    private static final String ATTENDEES_SORT_ORDER = Attendees.ATTENDEE_NAME + " ASC, "
            + Attendees.ATTENDEE_EMAIL + " ASC";
    // Begin Motorola
    private static final String CONFERENCE_WHERE = ConferenceCallInfo.ColumnNames.ORGANIZER_EMAIL + "=?";
    // End Motorola
    private static final String[] REMINDERS_PROJECTION = new String[] {
        Reminders._ID,                      // 0
        Reminders.MINUTES,            // 1
        Reminders.METHOD           // 2
    };
    private static final int REMINDERS_INDEX_ID = 0;
    private static final int REMINDERS_MINUTES_ID = 1;
    private static final int REMINDERS_METHOD_ID = 2;

    private static final String REMINDERS_WHERE = Reminders.EVENT_ID + "=?";

    static final String[] CALENDARS_PROJECTION = new String[] {
        Calendars._ID,           // 0
        Calendars.CALENDAR_DISPLAY_NAME,  // 1
        Calendars.OWNER_ACCOUNT, // 2
        Calendars.CAN_ORGANIZER_RESPOND, // 3
        Calendars.ACCOUNT_NAME, // 4
        // Begin Motorola
        Calendars.ACCOUNT_TYPE, //5
        // End Motorola
    };
    // Begin motorola
    static final int CALENDARS_INDEX_ID = 0;
    // End motorola
    static final int CALENDARS_INDEX_DISPLAY_NAME = 1;
    static final int CALENDARS_INDEX_OWNER_ACCOUNT = 2;
    static final int CALENDARS_INDEX_OWNER_CAN_RESPOND = 3;
    static final int CALENDARS_INDEX_ACCOUNT_NAME = 4;
    // Begin motorola
    static final int CALENDARS_INDEX_ACCOUNT_TYPE = 5;
    // End motorola
    static final String CALENDARS_WHERE = Calendars._ID + "=?";
    static final String CALENDARS_DUPLICATE_NAME_WHERE = Calendars.CALENDAR_DISPLAY_NAME + "=?";

    private static final String NANP_ALLOWED_SYMBOLS = "()+-*#.";
    private static final int NANP_MIN_DIGITS = 7;
    private static final int NANP_MAX_DIGITS = 11;
    // Begin Motorola
    private static final String[] EXTENDED_PROPERTIES_PROJECTION = new String[] {
        ExtendedProperties._ID,            // 0
        ExtendedProperties.NAME,           // 1
        ExtendedProperties.VALUE           // 2
    };
    private static final int EXTENDED_PROPERTIES_INDEX_ID = 0;
    private static final int EXTENDED_PROPERTIES_INDEX_NAME = 1;
    private static final int EXTENDED_PROPERTIES_INDEX_VALUE = 2;

    static final String EXTENDED_PROPERTIES_WHERE = ExtendedProperties.EVENT_ID + "=?";
    // End Motorola

    private View mView;

    private Uri mUri;
    private long mEventId;
    private Cursor mEventCursor;
    private Cursor mAttendeesCursor;
    private Cursor mCalendarsCursor;
    private Cursor mRemindersCursor;
    // Begin motorola
    private String mEventLocation;
    private Cursor mConfInfoCursor;
    Bundle mSavedInstanceState = null;
    private Cursor mExtendedPropertiesCursor;
    // End motorola

    private static float mScale = 0; // Used for supporting different screen densities

    private static int mCustomAppIconSize = 32;

    private long mStartMillis;
    private long mEndMillis;
    private boolean mAllDay;

    private boolean mHasAttendeeData;
    private String mEventOrganizerEmail;
    private String mEventOrganizerDisplayName = "";
    private boolean mIsOrganizer;
    private long mCalendarOwnerAttendeeId = EditEventHelper.ATTENDEE_ID_NONE;
    private boolean mOwnerCanRespond;
    private String mSyncAccountName;
    private String mCalendarOwnerAccount;
    private boolean mCanModifyCalendar;
    private boolean mCanModifyEvent;
    private boolean mIsBusyFreeCalendar;
    private int mNumOfAttendees;
    // Begin motorola
    private long mCalendarId;
    private String mSyncAccountType;
    private String mEventOrganizer;
    private int mAttendeesCount;
    private int mDeleteOptionIndex = -1;
    //For change respone
    private boolean mChangeResponseDialogVisible = false;
    private int mChangeResponseOptionIndex = -1;
    // End motorola
    private EditResponseHelper mEditResponseHelper;
    private boolean mDeleteDialogVisible = false;
    private DeleteEventHelper mDeleteHelper;

    private int mOriginalAttendeeResponse;
    private int mAttendeeResponseFromIntent = Attendees.ATTENDEE_STATUS_NONE;
    private int mUserSetResponse = Attendees.ATTENDEE_STATUS_NONE;
    private boolean mIsRepeating;
    private boolean mHasAlarm;
    private int mMaxReminders;
    private String mCalendarAllowedReminders;
    // Used to prevent saving changes in event if it is being deleted.
    private boolean mEventDeletionStarted = false;

    private TextView mTitle;
    private TextView mWhenDateTime;
    private TextView mLunarDate;
    private TextView mWhere;
    private ExpandableTextView mDesc;
    private AttendeesView mLongAttendees;
    // Begin motorola
    private LinearLayout mShowAttendeeDetails;
    private ImageButton mShowQuickTaskAction;
    private View mShowAttendeeContainer;
    // End motorola
    private Menu mMenu = null;
    private View mHeadlines;
    private ScrollView mScrollView;
    private View mLoadingMsgView;
    private ObjectAnimator mAnimateAlpha;
    private long mLoadingMsgStartTime;
    private static final int FADE_IN_TIME = 300;   // in milliseconds
    private static final int LOADING_MSG_DELAY = 600;   // in milliseconds
    private static final int LOADING_MSG_MIN_DISPLAY_TIME = 600;
    private boolean mNoCrossFade = false;  // Used to prevent repeated cross-fade
    
    //Lunar Calendar
    private boolean mShowHoliday = false;
    private boolean mShowLunar = false;


    private static final Pattern mWildcardPattern = Pattern.compile("^.*$");
    // Begin motorola
    ArrayList<EventAttendee> mRequiredAcceptedAttendees = new ArrayList<EventAttendee>();
    ArrayList<EventAttendee> mRequiredDeclinedAttendees = new ArrayList<EventAttendee>();
    ArrayList<EventAttendee> mRequiredTentativeAttendees = new ArrayList<EventAttendee>();
    ArrayList<EventAttendee> mRequiredNoResponseAttendees = new ArrayList<EventAttendee>();
    ArrayList<EventAttendee> mOptionalAcceptedAttendees = new ArrayList<EventAttendee>();
    ArrayList<EventAttendee> mOptionalDeclinedAttendees = new ArrayList<EventAttendee>();
    ArrayList<EventAttendee> mOptionalTentativeAttendees = new ArrayList<EventAttendee>();
    ArrayList<EventAttendee> mOptionalNoResponseAttendees = new ArrayList<EventAttendee>();
    EventAttendee mOrganizer;
    // End motorola
    ArrayList<Attendee> mAcceptedAttendees = new ArrayList<Attendee>();
    ArrayList<Attendee> mDeclinedAttendees = new ArrayList<Attendee>();
    ArrayList<Attendee> mTentativeAttendees = new ArrayList<Attendee>();
    ArrayList<Attendee> mNoResponseAttendees = new ArrayList<Attendee>();
    ArrayList<String> mToEmails = new ArrayList<String>();
    ArrayList<String> mCcEmails = new ArrayList<String>();
    private int mColor;


    private int mDefaultReminderMinutes;
    private final ArrayList<LinearLayout> mReminderViews = new ArrayList<LinearLayout>(0);
    public ArrayList<ReminderEntry> mReminders;
    public ArrayList<ReminderEntry> mOriginalReminders = new ArrayList<ReminderEntry>();
    public ArrayList<ReminderEntry> mUnsupportedReminders = new ArrayList<ReminderEntry>();
    private boolean mUserModifiedReminders = false;

    /**
     * Contents of the "minutes" spinner.  This has default values from the XML file, augmented
     * with any additional values that were already associated with the event.
     */
    private ArrayList<Integer> mReminderMinuteValues;
    private ArrayList<String> mReminderMinuteLabels;

    /**
     * Contents of the "methods" spinner.  The "values" list specifies the method constant
     * (e.g. {@link Reminders#METHOD_ALERT}) associated with the labels.  Any methods that
     * aren't allowed by the Calendar will be removed.
     */
    private ArrayList<Integer> mReminderMethodValues;
    private ArrayList<String> mReminderMethodLabels;

    private QueryHandler mHandler;
    // Begin motorola
    private String mConfOrganizer;

    // it will always use email format (mOrganizer may use name format)
    private String mOrganizerEmail;

    // used to store the value that's to be saved to the conference info table
    private final ContentValues mConfInfoContent = new ContentValues();

    // used to manage conference call info data
    private ConferenceCallInfoDataMgr mConfDataMgr = null;

    // used to manage conference call info setting, it will automatically notify ConferenceCallInfoDataMgr
    // to referesh itself when the setting info is changed
    private ConferenceCallInfoSettingMgr mConfSettingMgr = null;

    private static final String WINDOW_HIERARCHY_TAG = "android:viewHierarchyState";
    private static final String FRAGMENTS_TAG = "android:fragments";
    private static final String SAVED_DIALOG_IDS_KEY = "android:savedDialogIds";
    private static final String SAVED_DIALOGS_TAG = "android:savedDialogs";
    private static final String SAVED_DIALOG_KEY_PREFIX = "android:dialog_";
    private static final String SAVED_DIALOG_ARGS_KEY_PREFIX = "android:dialog_args_";

    public static final int REQUEST_AVAILABILITY = 51;
    public static final String START_TIME_MILLIS = "StartTimeMillis";
    public static final String END_TIME_MILLIS = "EndTimeMillis";
    public static final String EVENT_BEGIN_TIME = "beginTime";
    public static final String EVENT_END_TIME = "endTime";

    // following definitions are for dialog id
    private static final int DIALOG_ID_CONTEXT_DIALOG_FOR_EXISTING_CONF_INFO = 0;
    private static final int DIALOG_ID_CONTEXT_DIALOG_FOR_EMPTY_CONF_INFO = 1;
    private static final int DIALOG_ID_CHANGE_CONFERENCE_CALL_INFO = 2;
    private static final int DIALOG_ID_SET_INFO = 3;
    private static final int DIALOG_ID_CLEAR_CONFERENCE_INFO = 4;
    private static final int DIALOG_ID_COPY_CONFERENCE_CALL_INFO = 5;
    private static final int DIALOG_ID_SET_INFO_WITH_NO_VALUE = 6;
    private static final int DIALOG_ID_SET_INFO_BY_HAND_WITH_CURRENT = 7;
    private static final int DIALOG_ID_SET_INFO_BY_HAND_WITH_MINE = 8;


    // following definitions are for the dailog item
    private static final int DIALOG_ITEM_CALL_NUMBER = 0;
    private static final int DIALOG_ITEM_CHANGE_NUMBER = 1;
    private static final int DIALOG_ITEM_CLEAR_CONFERENCE_CALL_INFO = 2;
    private static final int DIALOG_ITEM_USE_MEETING_VALUE = 3;
    private static final int DIALOG_ITEM_MY_CONFERENCE_CALL_INFO = 4;
    private static final int DIALOG_ITEM_USE_ORGANIZER_INFO = 5;
    private static final int DIALOG_ITEM_SELECT_FROM_CONTACTS = 6;
    private static final int DIALOG_ITEM_ENTER_CONFERENCE_CALL_INFO = 7;
    private static final int DIALOG_ITEM_COPY_NUMBER = 8;
    private static final int DIALOG_ITEM_COPY_ENTIRE_NUMBER = 9;
    private static final int DIALOG_ITEM_COPY_CONFERENCE_NUMBER = 10;
    private static final int DIALOG_ITEM_COPY_CONFERENCE_ID = 11;

    /**
     * Creates a resizable {@code ArrayList} instance containing the given
     * elements.
     * @param elements the elements that the list should contain, in order
     * @return a newly-created {@code ArrayList} containing those elements
     */
    private <E> ArrayList<E> newArrayList(E... elements) {
        int capacity = (elements.length * 110) / 100 + 5;
        ArrayList<E> list = new ArrayList<E>(capacity);
        Collections.addAll(list, elements);
        return list;
    }

    private final ArrayList<ConfCallInfoDialogItem> mExistingConfInfoArrayList =
        newArrayList(
           new ConfCallInfoDialogItem(DIALOG_ITEM_CALL_NUMBER, R.string.call_number),
           new ConfCallInfoDialogItem(DIALOG_ITEM_CHANGE_NUMBER, R.string.change_number),
           new ConfCallInfoDialogItem(DIALOG_ITEM_COPY_NUMBER, R.string.conf_call_copy_number),
           new ConfCallInfoDialogItem(DIALOG_ITEM_CLEAR_CONFERENCE_CALL_INFO, R.string.clear_conference_call_info)
        );

    private final ArrayList<ConfCallInfoDialogItem> mEmptyConfInfoArrayList =
        newArrayList(
           new ConfCallInfoDialogItem(DIALOG_ITEM_USE_MEETING_VALUE, R.string.use_meeting_value),
           new ConfCallInfoDialogItem(DIALOG_ITEM_MY_CONFERENCE_CALL_INFO, R.string.my_conference_call_info),
           new ConfCallInfoDialogItem(DIALOG_ITEM_USE_ORGANIZER_INFO, R.string.use_organizer_info),
           new ConfCallInfoDialogItem(DIALOG_ITEM_SELECT_FROM_CONTACTS, R.string.select_from_contacts),
           new ConfCallInfoDialogItem(DIALOG_ITEM_ENTER_CONFERENCE_CALL_INFO, R.string.enter_conference_call_info)
        );

    private final ArrayList<ConfCallInfoDialogItem> mChangeConfInfoArrayList =
        newArrayList(
            new ConfCallInfoDialogItem(DIALOG_ITEM_USE_MEETING_VALUE, R.string.use_meeting_value),
            new ConfCallInfoDialogItem(DIALOG_ITEM_MY_CONFERENCE_CALL_INFO, R.string.my_conference_call_info),
            new ConfCallInfoDialogItem(DIALOG_ITEM_USE_ORGANIZER_INFO, R.string.use_organizer_info),
            new ConfCallInfoDialogItem(DIALOG_ITEM_SELECT_FROM_CONTACTS, R.string.select_from_contacts),
            new ConfCallInfoDialogItem(DIALOG_ITEM_ENTER_CONFERENCE_CALL_INFO, R.string.enter_conference_call_info)
        );

    private final ArrayList<ConfCallInfoDialogItem> mCopyConfInfoArrayList =
        newArrayList(
            new ConfCallInfoDialogItem(DIALOG_ITEM_COPY_ENTIRE_NUMBER, R.string.conf_call_copy_entire_number),
            new ConfCallInfoDialogItem(DIALOG_ITEM_COPY_CONFERENCE_NUMBER, R.string.conf_call_copy_part),
            new ConfCallInfoDialogItem(DIALOG_ITEM_COPY_CONFERENCE_ID, R.string.conf_call_copy_part)
        );

    // following definitions are the request code for startActivityForResult
    private static final int REQUEST_ID_FOR_CONTACT_PHONE_PICK = 0;
    private static final int REQUEST_ID_FOR_CONTACT_PHONE_PICK_BY_EMAIL = 1;
    private static final int REQUEST_ID_FOR_ADDING_EVENT_TO_TASKS = 2;

    private static class ManagedDialog {
        Dialog mDialog;
        Bundle mArgs;
    }
    private SparseArray<ManagedDialog> mManagedDialogs;
    // End motorola
    private final Runnable mTZUpdater = new Runnable() {
        @Override
        public void run() {
            updateEvent(mView);
        }
    };

    private final Runnable mLoadingMsgAlphaUpdater = new Runnable() {
        @Override
        public void run() {
            // Since this is run after a delay, make sure to only show the message
            // if the event's data is not shown yet.
            if (!mAnimateAlpha.isRunning() && mScrollView.getAlpha() == 0) {
                mLoadingMsgStartTime = System.currentTimeMillis();
                mLoadingMsgView.setAlpha(1);
            }
        }
    };

    private OnItemSelectedListener mReminderChangeListener;

    private static int mDialogWidth = 500;
    private static int mDialogHeight = 600;
    private static int DIALOG_TOP_MARGIN = 8;
    private boolean mIsDialog = false;
    private boolean mIsPaused = true;
    private boolean mDismissOnResume = false;
    private int mX = -1;
    private int mY = -1;
    private int mMinTop;         // Dialog cannot be above this location
    private boolean mIsTabletConfig;
    private Activity mActivity;
    private Context mContext;
    // BEGIN, Motorola, bck683, 38173,for Active sync
    private int mAttendeeSize;
    //END, Motorola
    // Begin Motorola
    private boolean mIsIcsImport = false;
    private boolean mTaskifyFlag = false;
    private String mEventUID = null;
    private AlertDialog mReplaceEventDialog = null;
    // End Motorola
    private class QueryHandler extends AsyncQueryService {
        public QueryHandler(Context context) {
            super(context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            // Begin Motorola
            if (cursor == null) {
                return;
            }
            // End Motorola
            // if the activity is finishing, then close the cursor and return
            final Activity activity = getActivity();
            if (activity == null || activity.isFinishing()) {
                cursor.close();
                return;
            }

            switch (token) {
            case TOKEN_QUERY_EVENT:
                mEventCursor = Utils.matrixCursorFromCursor(cursor);
                if (initEventCursor()) {
                    // The cursor is empty. This can happen if the event was
                    // deleted.
                    // FRAG_TODO we should no longer rely on Activity.finish()
                    // Begin Motorola
                    Toast.makeText(activity, R.string.event_has_been_deleted, Toast.LENGTH_SHORT).show();
                    // End Motorola
                    activity.finish();
                    return;
                }
                updateEvent(mView);
                prepareReminders();

                // start calendar query
                // Begin Motorola
//                Uri uri = Calendars.CONTENT_URI;
                Uri uri = decorateForScratch(Calendars.CONTENT_URI);
                // End Motorola
                String[] args = new String[] {
                        Long.toString(mEventCursor.getLong(EVENT_INDEX_CALENDAR_ID))};
                startQuery(TOKEN_QUERY_CALENDARS, null, uri, CALENDARS_PROJECTION,
                        CALENDARS_WHERE, args, null);
                break;
            case TOKEN_QUERY_CALENDARS:
                mCalendarsCursor = Utils.matrixCursorFromCursor(cursor);
                updateCalendar(mView);
                // FRAG_TODO fragments shouldn't set the title anymore
                updateTitle();

                if (!mIsBusyFreeCalendar) {
                    args = new String[] { Long.toString(mEventId) };

                    // start attendees query
                    // Begin Motorola
//                  uri = Attendees.CONTENT_URI;
                    uri = decorateForScratch(Attendees.CONTENT_URI);
                    // End Motorola
                    startQuery(TOKEN_QUERY_ATTENDEES, null, uri, ATTENDEES_PROJECTION,
                            ATTENDEES_WHERE, args, ATTENDEES_SORT_ORDER);
                } else {
                    sendAccessibilityEventIfQueryDone(TOKEN_QUERY_ATTENDEES);
                }
                if (mHasAlarm) {
                    // start reminders query
                    args = new String[] { Long.toString(mEventId) };
                    // Begin Motorola
//                  uri = Reminders.CONTENT_URI;
                    uri = decorateForScratch(Reminders.CONTENT_URI);
                    // End Motorola
                    startQuery(TOKEN_QUERY_REMINDERS, null, uri,
                            REMINDERS_PROJECTION, REMINDERS_WHERE, args, null);
                } else {
                    sendAccessibilityEventIfQueryDone(TOKEN_QUERY_REMINDERS);
                }
                // Begin Motorola
                // TOKEN_EXTENDED_PROPERTIES
                // Only need query ExtendedProperties when it's an ICS imported event
                if (mIsIcsImport) {
                    args = new String[] { Long.toString(mEventId) };
                    uri = ExtendedProperties.CONTENT_URI;
                    mHandler.startQuery(TOKEN_QUERY_EXTENDED_PROPERTIES, null, uri,
                            EXTENDED_PROPERTIES_PROJECTION,
                            EXTENDED_PROPERTIES_WHERE,
                            args, null);
                } else {
                    sendAccessibilityEventIfQueryDone(TOKEN_QUERY_EXTENDED_PROPERTIES);
                }
                // Load conference call info, do it before updateView() since we
                // need use them there.
                //boolean isConfEnable = setConferenceCallInfoVisible();
//                if (mConfDataMgr == null && isConfEnable && !mIsTabletConfig) {
//                    mConfDataMgr = new ConferenceCallInfoDataMgr(getActivity());
//                    mConfSettingMgr = new ConferenceCallInfoSettingMgr(mConfDataMgr);
//                    args = new String[] { mEventCursor.getString(EVENT_INDEX_ORGANIZER) };
//                    startQuery(TOKEN_QUERY_CONFERENCEINFO, null, ConferenceCallInfo.CONTENT_URI,
//                            null, CONFERENCE_WHERE, args, null);
//                } else {
//                    sendAccessibilityEventIfQueryDone(TOKEN_QUERY_CONFERENCEINFO);
//                }
                // End Motorola
                break;
            case TOKEN_QUERY_ATTENDEES:
                mAttendeesCursor = Utils.matrixCursorFromCursor(cursor);
                // BEGIN, 38173,for Active sync
                // modified by amt_sunli 2013-1-9 for feature 38173 begin
                if (Utils.isNeedRemoveActiveSync) {
                    if (mAttendeesCursor != null){
                        mAttendeeSize = mAttendeesCursor.getCount();
                        updateMenu();
                    }
                }
                // modified by amt_sunli 2013-1-9 for feature 38173 end
                // END
                initAttendeesCursor(mView);
                updateResponse(mView);
                break;
            case TOKEN_QUERY_REMINDERS:
                mRemindersCursor = Utils.matrixCursorFromCursor(cursor);
                initReminders(mView, mRemindersCursor);
                break;
            case TOKEN_QUERY_DUPLICATE_CALENDARS:
                Resources res = activity.getResources();
                SpannableStringBuilder sb = new SpannableStringBuilder();

                // Label
                String label = res.getString(R.string.view_event_calendar_label);
                sb.append(label).append(" ");
                sb.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                // Calendar display name
                String calendarName = mCalendarsCursor.getString(CALENDARS_INDEX_DISPLAY_NAME);
                sb.append(calendarName);

                // Show email account if display name is not unique and
                // display name != email
                String email = mCalendarsCursor.getString(CALENDARS_INDEX_OWNER_ACCOUNT);
                if (cursor.getCount() > 1 && !calendarName.equalsIgnoreCase(email)) {
                    sb.append(" (").append(email).append(")");
                }

                break;
                // Begin Motorola
                case TOKEN_QUERY_EXTENDED_PROPERTIES:
                    mExtendedPropertiesCursor = Utils.matrixCursorFromCursor(cursor);
                    initExtendedProperties(mView, mExtendedPropertiesCursor);
                    break;
                case TOKEN_QUERY_CONFERENCEINFO:
//                    mConfInfoCursor = Utils.matrixCursorFromCursor(cursor);
//                    new updateConfCallInfoInBackground().execute();
//                    //mConfSettingMgr.caceheConferenceCallSetting(mEventCursor, cursor);
//                    // set the title and onClick listener on the conference call info button
//                    //updateDynamicInfoOfConfCallButton();
//                    break;
                // End Motorola
                //Begin Motorola - IKHSS6-8628 ANR caused by main query
                case TOKEN_QUERY_CONTACTINFO:
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            int contactDataId = cursor.getInt(PhoneProjection.sPhoneIdIdx);
                            int phoneType = cursor.getInt(PhoneProjection.sPhoneTypeIdx);

                            mConfInfoContent.clear();
                            mConfInfoContent.put(ConferenceCallInfo.ColumnNames.ORGANIZER_EMAIL, mOrganizerEmail);
                            mConfInfoContent.put(ConferenceCallInfo.ColumnNames.DATA_SOURCE_FLAG, ConferenceCallInfo.DATA_SOURCE_FLAG_CONTACT_DATA);
                            mConfInfoContent.put(ConferenceCallInfo.ColumnNames.CONTACT_DATA_ID, contactDataId);
                            mConfInfoContent.put(ConferenceCallInfo.ColumnNames.CONTACT_PHONE_TYPE, phoneType);
                            showConfDialog(DIALOG_ID_SET_INFO);
                        }
                    }
                break;
              //End Motorola - IKHSS6-8628
            }
            cursor.close();
            sendAccessibilityEventIfQueryDone(token);
            // All queries are done, show the view
            if (mCurrentQuery == TOKEN_QUERY_ALL) {
                if (mLoadingMsgView.getAlpha() == 1) {
                    // Loading message is showing, let it stay a bit more (to prevent
                    // flashing) by adding a start delay to the event animation
                    long timeDiff = LOADING_MSG_MIN_DISPLAY_TIME - (System.currentTimeMillis() -
                            mLoadingMsgStartTime);
                    if (timeDiff > 0) {
                        mAnimateAlpha.setStartDelay(timeDiff);
                    }
                }
                if (!mAnimateAlpha.isRunning() &&!mAnimateAlpha.isStarted() && !mNoCrossFade) {
                    mAnimateAlpha.start();
                } else {
                    mScrollView.setAlpha(1);
                    mLoadingMsgView.setVisibility(View.GONE);
                }
            }
        }
    }

    private void sendAccessibilityEventIfQueryDone(int token) {
        mCurrentQuery |= token;
        if (mCurrentQuery == TOKEN_QUERY_ALL) {
            sendAccessibilityEvent();
        }
    }

    public EventInfoFragment(Context context, Uri uri, long startMillis, long endMillis,
            int attendeeResponse, boolean isDialog, int windowStyle) {

        Resources r = context.getResources();
        if (mScale == 0) {
            mScale = context.getResources().getDisplayMetrics().density;
            if (mScale != 1) {
                mCustomAppIconSize *= mScale;
                if (isDialog) {
                    DIALOG_TOP_MARGIN *= mScale;
                }
            }
        }
        if (isDialog) {
            setDialogSize(r);
        }
        mIsDialog = isDialog;

        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        mUri = uri;
        mStartMillis = startMillis;
        mEndMillis = endMillis;
        mAttendeeResponseFromIntent = attendeeResponse;
        mWindowStyle = windowStyle;
    }

    // This is currently required by the fragment manager.
    public EventInfoFragment() {
    }

    // Begin Motorola
    public EventInfoFragment(Context context, long eventId, long startMillis, long endMillis,
            int attendeeResponse, boolean isDialog, int windowStyle, Bundle extras) {
        this(context, eventId, startMillis, endMillis, attendeeResponse, isDialog, windowStyle);
        mIsIcsImport = extras.getBoolean(IcsConstants.EXTRA_IMPORT_ICS, false);
        mTaskifyFlag = extras.getBoolean("is_taskify", false);
    }
    // End Motorola

    public EventInfoFragment(Context context, long eventId, long startMillis, long endMillis,
            int attendeeResponse, boolean isDialog, int windowStyle) {
        this(context, ContentUris.withAppendedId(Events.CONTENT_URI, eventId), startMillis,
                endMillis, attendeeResponse, isDialog, windowStyle);
        mEventId = eventId;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mReminderChangeListener = new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Integer prevValue = (Integer) parent.getTag();
                if (prevValue == null || prevValue != position) {
                    parent.setTag(position);
                    mUserModifiedReminders = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }

        };

        if (savedInstanceState != null) {
            mIsDialog = savedInstanceState.getBoolean(BUNDLE_KEY_IS_DIALOG, false);
            mWindowStyle = savedInstanceState.getInt(BUNDLE_KEY_WINDOW_STYLE,
                    DIALOG_WINDOW_STYLE);
        }

        if (mIsDialog) {
            applyDialogParams();
        }
        mContext = getActivity();
    }

    private void applyDialogParams() {
        Dialog dialog = getDialog();
        dialog.setCanceledOnTouchOutside(true);

        Window window = dialog.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        WindowManager.LayoutParams a = window.getAttributes();
        a.dimAmount = .4f;

        a.width = mDialogWidth;
        a.height = mDialogHeight;


        // On tablets , do smart positioning of dialog
        // On phones , use the whole screen

        if (mX != -1 || mY != -1) {
            a.x = mX - mDialogWidth / 2;
            a.y = mY - mDialogHeight / 2;
            if (a.y < mMinTop) {
                a.y = mMinTop + DIALOG_TOP_MARGIN;
            }
            a.gravity = Gravity.LEFT | Gravity.TOP;
        }
        window.setAttributes(a);
    }

    public void setDialogParams(int x, int y, int minTop) {
        mX = x;
        mY = y;
        mMinTop = minTop;
    }

    // Implements OnCheckedChangeListener
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // If this is not a repeating event, then don't display the dialog
        // asking which events to change.
        mUserSetResponse = getResponseFromButtonId(checkedId);
        if (!mIsRepeating) {
            return;
        }

        // If the selection is the same as the original, then don't display the
        // dialog asking which events to change.
        if (checkedId == findButtonIdForResponse(mOriginalAttendeeResponse)) {
            return;
        }

        // This is a repeating event. We need to ask the user if they mean to
        // change just this one instance or all instances.
        // Begin motorola
//        mEditResponseHelper.showDialog(mEditResponseHelper.getWhichEvents());
        mEditResponseHelper.setOnDismissListener(createChangeResponseOnDismissListener());
        mChangeResponseOptionIndex =  mEditResponseHelper.getWhichEvents();
        mEditResponseHelper.showDialog(mChangeResponseOptionIndex);
        mChangeResponseDialogVisible = true;
        // End motorola
    }

    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        mEditResponseHelper = new EditResponseHelper(activity);

        if (mAttendeeResponseFromIntent != Attendees.ATTENDEE_STATUS_NONE) {
            mEditResponseHelper.setWhichEvents(UPDATE_ALL);
        }
        mHandler = new QueryHandler(activity);
        // Begin Motorola
        // Need initialize mIsTabletConfig here instead of in onCreateView, because
        // onCreateOptionsMenu is called once setHasOptionsMenu is called.
        mIsTabletConfig = Utils.getConfigBool(mActivity, R.bool.tablet_config);
        // End Motorola
        if (!mIsDialog) {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            mIsDialog = savedInstanceState.getBoolean(BUNDLE_KEY_IS_DIALOG, false);
            mWindowStyle = savedInstanceState.getInt(BUNDLE_KEY_WINDOW_STYLE,
                    DIALOG_WINDOW_STYLE);
            mDeleteDialogVisible =
                savedInstanceState.getBoolean(BUNDLE_KEY_DELETE_DIALOG_VISIBLE,false);
            // Begin motorola
            mDeleteOptionIndex = savedInstanceState.getInt(BUNDLE_KEY_DELETE_OPTION_INDEX,-1);
            //For change response
            mChangeResponseDialogVisible =
                savedInstanceState.getBoolean(BUNDLE_KEY_CHANGE_RESPONE_DIALOG_VISIBLE,false);
            mChangeResponseOptionIndex = savedInstanceState.getInt(BUNDLE_KEY_CHANGE_RESPONE_OPTION_INDEX,-1);
            mEditResponseHelper.setWhichEvents(mChangeResponseOptionIndex);
            // End motorola
        }

        if (mWindowStyle == DIALOG_WINDOW_STYLE) {
            mView = inflater.inflate(R.layout.event_info_dialog, container, false);
        } else {
            mView = inflater.inflate(R.layout.event_info, container, false);
        }
        mScrollView = (ScrollView) mView.findViewById(R.id.event_info_scroll_view);
        mLoadingMsgView = mView.findViewById(R.id.event_info_loading_msg);
        mTitle = (TextView) mView.findViewById(R.id.title);
        mWhenDateTime = (TextView) mView.findViewById(R.id.when_datetime);
        mLunarDate = (TextView) mView.findViewById(R.id.event_info_lunar_date);
        mWhere = (TextView) mView.findViewById(R.id.where);
        mDesc = (ExpandableTextView) mView.findViewById(R.id.description);
        mHeadlines = mView.findViewById(R.id.event_info_headline);
        // Begin motorola
        // BEGIN MOTOROLA - IKPIM-803: Support meeting attendee list screen
        mShowAttendeeDetails = (LinearLayout) mView.findViewById(R.id.attendee_details);
        //Begin Emergent Group
        mShowQuickTaskAction = (ImageButton)mView.findViewById(R.id.quick_task_button);
        mLongAttendees = (AttendeesView)mView.findViewById(R.id.long_attendee_list);
//        mIsTabletConfig = Utils.getConfigBool(mActivity, R.bool.tablet_config);
        View.OnClickListener quickTaskOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onQuickTaskClick(mShowQuickTaskAction);
            }
        };
        mShowQuickTaskAction.setOnClickListener(quickTaskOnClickListener);
        mShowAttendeeContainer = mView.findViewById(R.id.attendee_details_container);
        //End Emergent Group
        // END MOTOROLA - IKPIM-803
        // End morotola
        if (mUri == null) {
            // restore event ID from bundle
            mEventId = savedInstanceState.getLong(BUNDLE_KEY_EVENT_ID);
            mUri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
            mStartMillis = savedInstanceState.getLong(BUNDLE_KEY_START_MILLIS);
            mEndMillis = savedInstanceState.getLong(BUNDLE_KEY_END_MILLIS);
            // Begin Motorola
            mEventLocation = savedInstanceState.getString(BUNDLE_KEY_EVENT_LOCATION);
            mOrganizerEmail = savedInstanceState.getString(BUNDLE_KEY_ORIGINAL_EMAIL);
            mSavedInstanceState = savedInstanceState;
            mIsDialog = savedInstanceState.getBoolean(BUNDLE_KEY_IS_DIALOG);
            mAttendeeResponseFromIntent = savedInstanceState.getInt(BUNDLE_KEY_ATTENDEE_RESPONSE);
            mIsIcsImport = savedInstanceState.getBoolean(BUNDLE_KEY_IS_ICS_IMPORT);
            mSyncAccountType = savedInstanceState.getString(BUNDLE_KEY_SYNC_ACCOUNT_TYPE);
            ContentValues confInfoContent = savedInstanceState.getParcelable(BUNDLE_KEY_CONFINFO_CONTENT);
            if (confInfoContent != null) {
                mConfInfoContent.clear();
                mConfInfoContent.putAll(confInfoContent);
            }
            // End Motorola
        }

        mAnimateAlpha = ObjectAnimator.ofFloat(mScrollView, "Alpha", 0, 1);
        mAnimateAlpha.setDuration(FADE_IN_TIME);
        mAnimateAlpha.addListener(new AnimatorListenerAdapter() {
            int defLayerType;

            @Override
            public void onAnimationStart(Animator animation) {
                // Use hardware layer for better performance during animation
                defLayerType = mScrollView.getLayerType();
                mScrollView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                // Ensure that the loading message is gone before showing the
                // event info
                mLoadingMsgView.removeCallbacks(mLoadingMsgAlphaUpdater);
                mLoadingMsgView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mScrollView.setLayerType(defLayerType, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mScrollView.setLayerType(defLayerType, null);
                // Do not cross fade after the first time
                mNoCrossFade = true;
            }
        });

        mLoadingMsgView.setAlpha(0);
        mScrollView.setAlpha(0);
        mLoadingMsgView.postDelayed(mLoadingMsgAlphaUpdater, LOADING_MSG_DELAY);

        // start loading the data
        // Begin Motorola
        // TODO
        mUri = decorateForScratch(mUri);
        // End Motorola
        mHandler.startQuery(TOKEN_QUERY_EVENT, null, mUri, EVENT_PROJECTION,
                null, null, null);

        View b = mView.findViewById(R.id.delete);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mCanModifyCalendar) {
                    return;
                }
                mDeleteHelper =
                        new DeleteEventHelper(mContext, mActivity, !mIsDialog && !mIsTabletConfig /* exitWhenDone */);
                mDeleteHelper.setDeleteNotificationListener(EventInfoFragment.this);
                mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
                mDeleteDialogVisible = true;
                mDeleteHelper.delete(mStartMillis, mEndMillis, mEventId, -1, onDeleteRunnable);
            }
        });

        // Hide Edit/Delete buttons if in full screen mode on a phone
        if (!mIsDialog && !mIsTabletConfig || mWindowStyle == EventInfoFragment.FULL_WINDOW_STYLE) {
            mView.findViewById(R.id.event_info_buttons_container).setVisibility(View.GONE);
        // Begin Motorola
        } else if (mIsIcsImport) {
            mView.findViewById(R.id.event_info_buttons_container).setVisibility(View.GONE);
             mView.findViewById(R.id.ical_event_info_buttons_container).setVisibility(View.VISIBLE);
        }
        // End Motorola
        // BEGIN MOTOROLA - IKPIM-803: Support meeting attendee list screen
        mShowAttendeeDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mActivity, EventAttendeeActivity.class);
                intent.putExtra(Events.TITLE, mTitle.getText().toString());
                intent.putExtra(EventAttendeeActivity.INTENT_KEY_WHEN_DATE, mWhenDateTime.getText().toString());
//                intent.putExtra(EventAttendeeActivity.INTENT_KEY_WHEN_TIME, mWhenTime.getText().toString());
                intent.putExtra(Events.EVENT_COLOR, mColor);
                intent.putExtra(EventAttendeeActivity.INTENT_KEY_SHOW_ORGANIZER, !mIsOrganizer);
                intent.putExtra(Events.ORGANIZER, mOrganizer);
                intent.putExtra(EventAttendeeActivity.INTENT_KEY_ATTENDEE_REQUIRED_ACCEPT,
                        mRequiredAcceptedAttendees);
                intent.putExtra(EventAttendeeActivity.INTENT_KEY_ATTENDEE_REQUIRED_TENTATIVE,
                        mRequiredTentativeAttendees);
                intent.putExtra(EventAttendeeActivity.INTENT_KEY_ATTENDEE_REQUIRED_DECLINE,
                        mRequiredDeclinedAttendees);
                intent.putExtra(EventAttendeeActivity.INTENT_KEY_ATTENDEE_REQUIRED_NO_RESP,
                        mRequiredNoResponseAttendees);
                intent.putExtra(EventAttendeeActivity.INTENT_KEY_ATTENDEE_OPTIONAL_ACCEPT,
                        mOptionalAcceptedAttendees);
                intent.putExtra(EventAttendeeActivity.INTENT_KEY_ATTENDEE_OPTIONAL_TENTATIVE,
                        mOptionalTentativeAttendees);
                intent.putExtra(EventAttendeeActivity.INTENT_KEY_ATTENDEE_OPTIONAL_DECLINE,
                        mOptionalDeclinedAttendees);
                intent.putExtra(EventAttendeeActivity.INTENT_KEY_ATTENDEE_OPTIONAL_NO_RESP,
                        mOptionalNoResponseAttendees);
                startActivity(intent);
            }
        });
        // END MOTOROLA - IKPIM-803
        // Create a listener for the email guests button
        View emailAttendeesButton = mView.findViewById(R.id.email_attendees_button);
        if (emailAttendeesButton != null) {
            emailAttendeesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    emailAttendees();
                }
            });
        }

        // Create a listener for the add reminder button
        View reminderAddButton = mView.findViewById(R.id.reminder_add);
        // Begin Motorola
        if (mIsIcsImport) {
            // Hide reminderAddButton for iCal-imported event
            reminderAddButton.setVisibility(View.GONE);
        } else {
        // End Motorola
            View.OnClickListener addReminderOnClickListener = new View.OnClickListener() {
                @Override
               public void onClick(View v) {
                    addReminder();
                    mUserModifiedReminders = true;
                }
            };
            reminderAddButton.setOnClickListener(addReminderOnClickListener);
        }
        //Lunar Calendar
        //Begin Motorola
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(mActivity);
        prefs.registerOnSharedPreferenceChangeListener(this);
        mShowLunar = prefs.getBoolean(GeneralPreferences.KEY_SHOW_LUNAR, GeneralPreferences.DEFAULT_VALUE_OF_SHOW_LUNAR);
        mShowHoliday = prefs.getBoolean(GeneralPreferences.KEY_SHOW_HOLIDAY, GeneralPreferences.DEFAULT_VALUE_OF_SHOW_HOLIDAY);
        //End Motorola
        
        // Set reminders variables
        String defaultReminderString = prefs.getString(
                GeneralPreferences.KEY_DEFAULT_REMINDER, GeneralPreferences.NO_REMINDER_STRING);
        mDefaultReminderMinutes = Integer.parseInt(defaultReminderString);
        prepareReminders();

        return mView;
    }

    private final Runnable onDeleteRunnable = new Runnable() {
        @Override
        public void run() {
            if (EventInfoFragment.this.mIsPaused) {
                mDismissOnResume = true;
                return;
            }
            if (EventInfoFragment.this.isVisible()) {
                EventInfoFragment.this.dismiss();
            }
        }
    };

    private void updateTitle() {
        Resources res = getActivity().getResources();
        if (mCanModifyCalendar && !mIsOrganizer) {
            getActivity().setTitle(res.getString(R.string.event_info_title_invite));
        } else {
            getActivity().setTitle(res.getString(R.string.event_info_title));
        }
    }

    /**
     * Initializes the event cursor, which is expected to point to the first
     * (and only) result from a query.
     * @return true if the cursor is empty.
     */
    private boolean initEventCursor() {
        if ((mEventCursor == null) || (mEventCursor.getCount() == 0)) {
            return true;
        }
        mEventCursor.moveToFirst();
        mEventId = mEventCursor.getInt(EVENT_INDEX_ID);
        String rRule = mEventCursor.getString(EVENT_INDEX_RRULE);
        mIsRepeating = !TextUtils.isEmpty(rRule);
        mHasAlarm = (mEventCursor.getInt(EVENT_INDEX_HAS_ALARM) == 1)?true:false;
        mMaxReminders = mEventCursor.getInt(EVENT_INDEX_MAX_REMINDERS);
        mCalendarAllowedReminders =  mEventCursor.getString(EVENT_INDEX_ALLOWED_REMINDERS);
        // Begin Motorola
		mEventLocation = mEventCursor.getString(EVENT_INDEX_EVENT_LOCATION);
        mConfOrganizer = mEventCursor.getString(EVENT_INDEX_ORGANIZER);
        mOrganizerEmail = mConfOrganizer;

        boolean deleted = mEventCursor.getInt(EVENT_INDEX_DELETED) != 0;
        if (deleted) {
            return true;
        }

        if (mTaskifyFlag && !mIsRepeating) {
            mStartMillis = mEventCursor.getLong(EVENT_INDEX_DTSTART);
            mEndMillis = mEventCursor.getLong(EVENT_INDEX_DTEND);
        }
        // End Motorola
        return false;
    }

    @SuppressWarnings("fallthrough")
    private void initAttendeesCursor(View view) {
        mOriginalAttendeeResponse = Attendees.ATTENDEE_STATUS_NONE;
        mCalendarOwnerAttendeeId = EditEventHelper.ATTENDEE_ID_NONE;
        mNumOfAttendees = 0;
        if (mAttendeesCursor != null) {
            mNumOfAttendees = mAttendeesCursor.getCount();
            if (mAttendeesCursor.moveToFirst()) {
                mAcceptedAttendees.clear();
                mDeclinedAttendees.clear();
                mTentativeAttendees.clear();
                mNoResponseAttendees.clear();
               // BEGIN MOTOROLA - IKPIM-803: Support meeting attendee list screen
                mRequiredAcceptedAttendees.clear();
                mRequiredDeclinedAttendees.clear();
                mRequiredTentativeAttendees.clear();
                mRequiredNoResponseAttendees.clear();
                mOptionalAcceptedAttendees.clear();
                mOptionalDeclinedAttendees.clear();
                mOptionalTentativeAttendees.clear();
                mOptionalNoResponseAttendees.clear();
                int count = 0;
                EventAttendee[] attendeeItems = new EventAttendee[mNumOfAttendees];
                // End
                do {
                    int status = mAttendeesCursor.getInt(ATTENDEES_INDEX_STATUS);
                    String name = mAttendeesCursor.getString(ATTENDEES_INDEX_NAME);
                    String email = mAttendeesCursor.getString(ATTENDEES_INDEX_EMAIL);
                    // Begin motorola
                    int type = mAttendeesCursor.getInt(ATTENDEES_INDEX_TYPE);
                    int relationship = mAttendeesCursor.getInt(ATTENDEES_INDEX_RELATIONSHIP);
                    // End motorola
                    // BEGIN CONFERENCE CALL INFO
                    if (mAttendeesCursor.getInt(ATTENDEES_INDEX_RELATIONSHIP) ==
                            Attendees.RELATIONSHIP_ORGANIZER) {

                        // Overwrites the one from Event table if available
                        if (name != null && name.length() > 0) {
                            mConfOrganizer = name;
                            mEventOrganizerDisplayName = name;
                            if (!mIsOrganizer) {
                                setVisibilityCommon(view, R.id.organizer_container, View.VISIBLE);
                                setTextCommon(view, R.id.organizer, mEventOrganizerDisplayName);
                            }
                        } else if (email != null && email.length() > 0) {
                            mConfOrganizer = email;
                        }

                        if (email != null && email.length() > 0) {
                            // mOrganizerEmail always use email format
                            mOrganizerEmail = email;
                        }
                     }
                     // END CONFERENCE CALL INFO
//                    if (mAttendeesCursor.getInt(ATTENDEES_INDEX_RELATIONSHIP) ==
//                            Attendees.RELATIONSHIP_ORGANIZER) {
//
//                        // Overwrites the one from Event table if available
//                        if (!TextUtils.isEmpty(name)) {
//                            mEventOrganizerDisplayName = name;
//                            if (!mIsOrganizer) {
//                                setVisibilityCommon(view, R.id.organizer_container, View.VISIBLE);
//                                setTextCommon(view, R.id.organizer, mEventOrganizerDisplayName);
//                            }
//                        }
//                    }

                    if (mCalendarOwnerAttendeeId == EditEventHelper.ATTENDEE_ID_NONE &&
                            mCalendarOwnerAccount.equalsIgnoreCase(email)) {
                        mCalendarOwnerAttendeeId = mAttendeesCursor.getInt(ATTENDEES_INDEX_ID);
                        mOriginalAttendeeResponse = mAttendeesCursor.getInt(ATTENDEES_INDEX_STATUS);
                    } else {
                        String identity = mAttendeesCursor.getString(ATTENDEES_INDEX_IDENTITY);
                        String idNamespace = mAttendeesCursor.getString(
                                ATTENDEES_INDEX_ID_NAMESPACE);

                        // Don't show your own status in the list because:
                        //  1) it doesn't make sense for event without other guests.
                        //  2) there's a spinner for that for events with guests.
                        if (name == null) {
                            name = "";
                        }
                        if (email == null) {
                            email = "";
                        }
                        // Check whether same attendees were specified multiple times
                        if (email.length() > 0 || name.length() > 0) {
                            if (relationship == Attendees.RELATIONSHIP_ORGANIZER) {
                                mOrganizer = new EventAttendee(name, email);
                            }

                            boolean duplicate = false;
                            for (int i = 0; i < count; i++) {
                                if (TextUtils.equals(email, attendeeItems[i].mEmail)) {
                                    duplicate = true;
                                    if (DEBUG)
                                        Log.d(TAG, "Duplicate with previous items, will ignore!");
                                    break;
                                }
                            }
                            if (!duplicate) {
                                EventAttendee attendeeItem = new EventAttendee(name, email);
                                attendeeItems[count++] = attendeeItem;
                        switch(status) {
                            case Attendees.ATTENDEE_STATUS_ACCEPTED:
                                mAcceptedAttendees.add(new Attendee(name, email,
                                        Attendees.ATTENDEE_STATUS_ACCEPTED, identity,
                                        idNamespace));
                                if (Attendees.TYPE_OPTIONAL == type) {
                                    mOptionalAcceptedAttendees.add(attendeeItem);
                                } else {
                                    mRequiredAcceptedAttendees.add(attendeeItem);
                                }
                                break;
                            case Attendees.ATTENDEE_STATUS_DECLINED:
                                mDeclinedAttendees.add(new Attendee(name, email,
                                        Attendees.ATTENDEE_STATUS_DECLINED, identity,
                                        idNamespace));
                                if (Attendees.TYPE_OPTIONAL == type) {
                                    mOptionalDeclinedAttendees.add(attendeeItem);
                                } else {
                                    mRequiredDeclinedAttendees.add(attendeeItem);
                                }
                                break;
                            case Attendees.ATTENDEE_STATUS_TENTATIVE:
                                mTentativeAttendees.add(new Attendee(name, email,
                                        Attendees.ATTENDEE_STATUS_TENTATIVE, identity,
                                        idNamespace));
                                if (Attendees.TYPE_OPTIONAL == type) {
                                    mOptionalTentativeAttendees.add(attendeeItem);
                                } else {
                                    mRequiredTentativeAttendees.add(attendeeItem);
                                }
                                break;
                            default:
                                mNoResponseAttendees.add(new Attendee(name, email,
                                        Attendees.ATTENDEE_STATUS_NONE, identity,
                                        idNamespace));
                                if (Attendees.TYPE_OPTIONAL == type) {
                                    mOptionalNoResponseAttendees.add(attendeeItem);
                                } else {
                                    mRequiredNoResponseAttendees.add(attendeeItem);
                                }
                            }
                        }
                    }
                }
                } while (mAttendeesCursor.moveToNext());
                mAttendeesCursor.moveToFirst();

                updateAttendees(view);
                // Begin motorola
                mAttendeesCount = count;
                if (count > 0) {
                    mShowAttendeeContainer.setVisibility(View.VISIBLE);
                    // BEGIN MOTOROLA - IKHSS7-1357: Change style of show attendees bar
                    String format = mActivity.getResources().getQuantityString(
                            R.plurals.attendee_detail_num, count);
                    TextView attendeeDetailCount =
                            (TextView) mView.findViewById(R.id.attendee_detail_count);
                    attendeeDetailCount.setText(String.format(format, count));
                    // END MOTOROLA - IKHSS7-1357

                    // BEGIN MOTOROLA - IKPIM-936: Support reply/reply all on event detail
                    // BEGIN MOTOROLA - IKHSS7-364: Hide reply/reply all for local account event
                    if (!mIsIcsImport && mSyncAccountType != null
                            && !CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType)) {
                        setVisibilityCommon(view, R.id.reply_all, View.VISIBLE);
                        Button replyAllButton = (Button) mView.findViewById(R.id.reply_all);
                        replyAllButton.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                showReplyComposeScreen(ACTION_REPLYALL);
                            }
                        });
                    } else {
                        setVisibilityCommon(view, R.id.reply_all, View.GONE);
                    }

                    if (!mIsOrganizer && !mIsIcsImport && mSyncAccountType != null
                            && !CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType)) {
                        // END MOTOROLA - IKHSS7-364
                        setVisibilityCommon(view, R.id.reply, View.VISIBLE);
                        Button replyButton = (Button) mView.findViewById(R.id.reply);
                        replyButton.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                showReplyComposeScreen(ACTION_REPLY);
                            }
                        });
                    } else {
                        // Hide button "Reply" if is event organizer
                        setVisibilityCommon(view, R.id.reply, View.GONE);
                    }
                    // END MOTOROLA - IKPIM-936
                }
                // END MOTOROLA - IKPIM-803

                // BEGIN MOTOROLA - IKPIM-974: Support forward on event detail
                if (!mIsIcsImport
                        && (mSyncAccountType != null)
                        && (!CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType))) {
                    setVisibilityCommon(view, R.id.forward, View.VISIBLE);
                    Button forwardButton = (Button) mView.findViewById(R.id.forward);
                    forwardButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new ForwardIcsTask(getActivity()).execute(mEventId);
                        }
                    });
                } else {
                    setVisibilityCommon(view, R.id.forward, View.GONE);
                }

                updateOptionMenuIfAny();
                // END MOTOROLA - IKPIM-974
                // End motorola
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Begin motorola
        saveManagedDialogs(outState);
        // End motorola
        outState.putLong(BUNDLE_KEY_EVENT_ID, mEventId);
        outState.putLong(BUNDLE_KEY_START_MILLIS, mStartMillis);
        outState.putLong(BUNDLE_KEY_END_MILLIS, mEndMillis);
        outState.putBoolean(BUNDLE_KEY_IS_DIALOG, mIsDialog);
        outState.putInt(BUNDLE_KEY_WINDOW_STYLE, mWindowStyle);
        outState.putBoolean(BUNDLE_KEY_DELETE_DIALOG_VISIBLE, mDeleteDialogVisible);
        outState.putInt(BUNDLE_KEY_ATTENDEE_RESPONSE, mAttendeeResponseFromIntent);
        // Begin Motorola
        outState.putString(BUNDLE_KEY_EVENT_LOCATION, mEventLocation);
        outState.putString(BUNDLE_KEY_ORIGINAL_EMAIL, mOrganizerEmail);
        outState.putBoolean(BUNDLE_KEY_IS_ICS_IMPORT, mIsIcsImport);
        outState.putString(BUNDLE_KEY_SYNC_ACCOUNT_TYPE, mSyncAccountType);
        outState.putInt(BUNDLE_KEY_DELETE_OPTION_INDEX, mDeleteOptionIndex);
        //For Change response
        outState.putBoolean(BUNDLE_KEY_CHANGE_RESPONE_DIALOG_VISIBLE , mChangeResponseDialogVisible);
        outState.putInt(BUNDLE_KEY_CHANGE_RESPONE_OPTION_INDEX, mChangeResponseOptionIndex);
        //For Confinfo
        outState.putParcelable(BUNDLE_KEY_CONFINFO_CONTENT, mConfInfoContent);
        // End Motorola
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Show edit/delete buttons only in non-dialog configuration
        if (!mIsDialog && !mIsTabletConfig || mWindowStyle == EventInfoFragment.FULL_WINDOW_STYLE) {
//            inflater.inflate(R.menu.event_info_title_bar, menu);
//            mMenu = menu;
            // Begin Motorola
            if (mIsIcsImport) {
                inflater.inflate(R.menu.ical_event_info_title_bar, menu);
                mMenu = menu;
            } else {
                inflater.inflate(R.menu.event_info_title_bar, menu);
                mMenu = menu;
                // End Motorola
                // modified by amt_sunli 2012-12-18 SWITCHUITWO-334 begin
                // modified by amt_sunli 2013-1-9 for feature 38173
                if (!Utils.isNeedRemoveActiveSync) {
                    updateMenu();
                }
                // modified by amt_sunli 2013-1-9 for feature 38173 end
                // modified by amt_sunli 2012-12-18 SWITCHUITWO-334 end
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // If we're a dialog we don't want to handle menu buttons
        if (mIsDialog) {
            return false;
        }
        // Handles option menu selections:
        // Home button - close event info activity and start the main calendar
        // one
        // Edit button - start the event edit activity and close the info
        // activity
        // Delete button - start a delete query that calls a runnable that close
        // the info activity

        switch (item.getItemId()) {
            case android.R.id.home:
                Utils.returnToCalendarHome(mContext);
                mActivity.finish();
                return true;
            case R.id.info_action_edit:
                Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
                Intent intent = new Intent(Intent.ACTION_EDIT, uri);
                intent.putExtra(EXTRA_EVENT_BEGIN_TIME, mStartMillis);
                intent.putExtra(EXTRA_EVENT_END_TIME, mEndMillis);
                intent.putExtra(EXTRA_EVENT_ALL_DAY, mAllDay);
                intent.setClass(mActivity, EditEventActivity.class);
                intent.putExtra(EVENT_EDIT_ON_LAUNCH, true);
                /*2012-11-27, add by amt_jiayuanyuan for SWITCHUITWO-125 */
                if(saveReminders()){
                mIsEditEvent = true;
            	}
                /*2012-11-27, add end*/
                startActivity(intent);
                mActivity.finish();
                break;
            case R.id.info_action_delete:
                mDeleteHelper =
                        new DeleteEventHelper(mActivity, mActivity, true /* exitWhenDone */);
                mDeleteHelper.setDeleteNotificationListener(EventInfoFragment.this);
                mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
                mDeleteDialogVisible = true;
                mDeleteHelper.delete(mStartMillis, mEndMillis, mEventId, -1, onDeleteRunnable);
                break;
             // BEGIN MOTOROLA - IKPIM-936: Support reply/reply all on event detail
            case R.id.info_action_reply:
                    showReplyComposeScreen(ACTION_REPLY);
                    break;
            case R.id.info_action_reply_all:
                    showReplyComposeScreen(ACTION_REPLYALL);
                    break;
            // END MOTOROLA - IKPIM-936
            // BEGIN MOTOROLA - IKPIM-974: Support forward on event detail
            case R.id.info_action_forward:
                    new ForwardIcsTask(getActivity()).execute(mEventId);
                    break;
            // END MOTOROLA - IKPIM-974
            // Begin Motorola
            case R.id.info_action_cancel:
                    destroyICalView(false/*exit calendar app*/, true/*delete iCal event*/);
                    break;
            case R.id.info_action_save:
                    saveICalEventToLocal();
                    break;
            case R.id.info_action_print:
                    new ComposeIcsTask(getActivity()).execute(mEventId);
                    break;
            case R.id.info_action_share:
                    new ShareIcsTask(getActivity()).execute(mEventId);
                    break;
            case R.id.info_action_add_event_to_tasks:
                    Utils.addEventToTasks(this, mUri, null, mStartMillis, mEndMillis,
                            mTitle.getText().toString(),
                            mWhere.getText().toString(),
                            CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType) ? getString(R.string.phone_calendar) : mEventCursor.getString(EVENT_INDEX_ORGANIZER),
                            mEventCursor.getInt(EVENT_INDEX_ALL_DAY) != 0,
                            REQUEST_ID_FOR_ADDING_EVENT_TO_TASKS);
                    break;
            // End Motorola
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
    	
        if (!mEventDeletionStarted) {
			boolean responseSaved = saveResponse();
            /*2012-11-27, add by amt_jiayuanyuan for SWITCHUITWO-125 */
			if (mIsEditEvent) {
				Toast.makeText(getActivity(), R.string.saving_event,
						Toast.LENGTH_SHORT).show();
			} else {
                // Begin Motorola
                if (!mIsIcsImport && (saveResponse() || saveReminders())) {
                // End Motorola
					Toast.makeText(getActivity(), R.string.saving_event,
							Toast.LENGTH_SHORT).show();
				}
			}
			 /*2012-11-27, add end*/
		}
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (mEventCursor != null) {
            mEventCursor.close();
        }
        if (mCalendarsCursor != null) {
            mCalendarsCursor.close();
        }
        if (mAttendeesCursor != null) {
            mAttendeesCursor.close();
        }
        // Begin Motorola
        if (mRemindersCursor != null) {
            mRemindersCursor.close();
        }
        if (mExtendedPropertiesCursor != null) {
            mExtendedPropertiesCursor.close();
        }
        if (mReplaceEventDialog != null) {
            mReplaceEventDialog.dismiss();
            mReplaceEventDialog = null;
        }
        if (mConfInfoCursor != null) {
            mConfInfoCursor.close();
        }
        // dismiss any dialogs we are managing.
        if (mManagedDialogs != null) {
            final int numDialogs = mManagedDialogs.size();
            for (int i = 0; i < numDialogs; i++) {
                final ManagedDialog md = mManagedDialogs.valueAt(i);
                if (md.mDialog.isShowing()) {
                    md.mDialog.dismiss();
                }
            }
            mManagedDialogs = null;
        }
        // End Motorola
        super.onDestroy();
    }

    /**
     * Asynchronously saves the response to an invitation if the user changed
     * the response. Returns true if the database will be updated.
     *
     * @return true if the database will be changed
     */
    private boolean saveResponse() {
        if (mAttendeesCursor == null || mEventCursor == null) {
            return false;
        }

        RadioGroup radioGroup = (RadioGroup) getView().findViewById(R.id.response_value);
        // BEGIN, Motorola, bck683, 38173,for Active sync
        // Disable the attendees response
        // modified by amt_sunli 2013-1-9 for feature 38173 begin
        if (Utils.isNeedRemoveActiveSync) {
            for (int i = 0; i < radioGroup.getChildCount(); i++) {
                radioGroup.getChildAt(i).setEnabled(false);
            }
        }
        // modified by amt_sunli 2013-1-9 for feature 38173 end
        // END, Motorola
        int status = getResponseFromButtonId(radioGroup.getCheckedRadioButtonId());
        if (status == Attendees.ATTENDEE_STATUS_NONE) {
            return false;
        }

        // If the status has not changed, then don't update the database
        if (status == mOriginalAttendeeResponse) {
            return false;
        }

        // If we never got an owner attendee id we can't set the status
        if (mCalendarOwnerAttendeeId == EditEventHelper.ATTENDEE_ID_NONE) {
            return false;
        }

        if (!mIsRepeating) {
            // This is a non-repeating event
            updateResponse(mEventId, mCalendarOwnerAttendeeId, status);
            return true;
        }

        // This is a repeating event
        int whichEvents = mEditResponseHelper.getWhichEvents();
        switch (whichEvents) {
            case -1:
                return false;
            case UPDATE_SINGLE:
                createExceptionResponse(mEventId, status);
                return true;
            case UPDATE_ALL:
                updateResponse(mEventId, mCalendarOwnerAttendeeId, status);
                return true;
            default:
                Log.e(TAG, "Unexpected choice for updating invitation response");
                break;
        }
        return false;
    }

    private void updateResponse(long eventId, long attendeeId, int status) {
        // Update the attendee status in the attendees table.  the provider
        // takes care of updating the self attendance status.
        ContentValues values = new ContentValues();

        if (!TextUtils.isEmpty(mCalendarOwnerAccount)) {
            values.put(Attendees.ATTENDEE_EMAIL, mCalendarOwnerAccount);
        }
        values.put(Attendees.ATTENDEE_STATUS, status);
        values.put(Attendees.EVENT_ID, eventId);

        Uri uri = ContentUris.withAppendedId(Attendees.CONTENT_URI, attendeeId);

        mHandler.startUpdate(mHandler.getNextToken(), null, uri, values,
                null, null, Utils.UNDO_DELAY);
    }

    /**
     * Creates an exception to a recurring event.  The only change we're making is to the
     * "self attendee status" value.  The provider will take care of updating the corresponding
     * Attendees.attendeeStatus entry.
     *
     * @param eventId The recurring event.
     * @param status The new value for selfAttendeeStatus.
     */
    private void createExceptionResponse(long eventId, int status) {
        ContentValues values = new ContentValues();
        values.put(Events.ORIGINAL_INSTANCE_TIME, mStartMillis);
        values.put(Events.SELF_ATTENDEE_STATUS, status);
        values.put(Events.STATUS, Events.STATUS_CONFIRMED);

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        Uri exceptionUri = Uri.withAppendedPath(Events.CONTENT_EXCEPTION_URI,
                String.valueOf(eventId));
        ops.add(ContentProviderOperation.newInsert(exceptionUri).withValues(values).build());

        mHandler.startBatch(mHandler.getNextToken(), null, CalendarContract.AUTHORITY, ops,
                Utils.UNDO_DELAY);
   }

    public static int getResponseFromButtonId(int buttonId) {
        int response;
        switch (buttonId) {
            case R.id.response_yes:
                response = Attendees.ATTENDEE_STATUS_ACCEPTED;
                break;
            case R.id.response_maybe:
                response = Attendees.ATTENDEE_STATUS_TENTATIVE;
                break;
            case R.id.response_no:
                response = Attendees.ATTENDEE_STATUS_DECLINED;
                break;
            default:
                response = Attendees.ATTENDEE_STATUS_NONE;
        }
        return response;
    }

    public static int findButtonIdForResponse(int response) {
        int buttonId;
        switch (response) {
            case Attendees.ATTENDEE_STATUS_ACCEPTED:
                buttonId = R.id.response_yes;
                break;
            case Attendees.ATTENDEE_STATUS_TENTATIVE:
                buttonId = R.id.response_maybe;
                break;
            case Attendees.ATTENDEE_STATUS_DECLINED:
                buttonId = R.id.response_no;
                break;
                default:
                    buttonId = -1;
        }
        return buttonId;
    }

    private void doEdit() {
        Context c = getActivity();
        // This ensures that we aren't in the process of closing and have been
        // unattached already
        if (c != null) {
            CalendarController.getInstance(c).sendEventRelatedEvent(
                    this, EventType.EDIT_EVENT, mEventId, mStartMillis, mEndMillis, 0
                    , 0, -1);
        }
    }

    private void updateEvent(View view) {
        if (mEventCursor == null || view == null) {
            return;
        }

        Context context = view.getContext();
        if (context == null) {
            return;
        }

        String eventName = mEventCursor.getString(EVENT_INDEX_TITLE);
        if (eventName == null || eventName.length() == 0) {
            eventName = getActivity().getString(R.string.no_title_label);
        }

        mAllDay = mEventCursor.getInt(EVENT_INDEX_ALL_DAY) != 0;
        String location = mEventCursor.getString(EVENT_INDEX_EVENT_LOCATION);
        String description = mEventCursor.getString(EVENT_INDEX_DESCRIPTION);
        String rRule = mEventCursor.getString(EVENT_INDEX_RRULE);
        String eventTimezone = mEventCursor.getString(EVENT_INDEX_EVENT_TIMEZONE);

        mColor = Utils.getDisplayColorFromColor(mEventCursor.getInt(EVENT_INDEX_COLOR));
        mHeadlines.setBackgroundColor(mColor);

        // What
        if (eventName != null) {
            setTextCommon(view, R.id.title, eventName);
        }

        // When
        // Set the date and repeats (if any)
        String localTimezone = Utils.getTimeZone(mActivity, mTZUpdater);

        Resources resources = context.getResources();
        String displayedDatetime = Utils.getDisplayedDatetime(mStartMillis, mEndMillis,
                System.currentTimeMillis(), localTimezone, mAllDay, context);

        String displayedTimezone = null;
        if (!mAllDay) {
            displayedTimezone = Utils.getDisplayedTimezone(mStartMillis, localTimezone,
                    eventTimezone);
        }
        // Display the datetime.  Make the timezone (if any) transparent.
        if (displayedTimezone == null) {
            setTextCommon(view, R.id.when_datetime, displayedDatetime);
        } else {
            int timezoneIndex = displayedDatetime.length();
            displayedDatetime += "  " + displayedTimezone;
            SpannableStringBuilder sb = new SpannableStringBuilder(displayedDatetime);
            ForegroundColorSpan transparentColorSpan = new ForegroundColorSpan(
                    resources.getColor(R.color.event_info_headline_transparent_color));
            sb.setSpan(transparentColorSpan, timezoneIndex, displayedDatetime.length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            setTextCommon(view, R.id.when_datetime, sb);
        }

        // Display the repeat string (if any)
        String repeatString = null;
        if (!TextUtils.isEmpty(rRule)) {
            EventRecurrence eventRecurrence = new EventRecurrence();
            eventRecurrence.parse(rRule);
            Time date = new Time(localTimezone);
            if (mAllDay) {
                date.timezone = Time.TIMEZONE_UTC;
            }
            date.set(mStartMillis);
            eventRecurrence.setStartDate(date);
            repeatString = EventRecurrenceFormatter.getRepeatString(resources, eventRecurrence);
        }
        if (repeatString == null) {
            view.findViewById(R.id.when_repeat).setVisibility(View.GONE);
        } else {
            setTextCommon(view, R.id.when_repeat, repeatString);
        }
        
        // Begin Motorola
        if (Utils.isDefaultLocaleChinese() && mShowLunar) {
            Time start = new Time(Utils.getTimeZone(getActivity(), mTZUpdater));
            start.set(mStartMillis);
            Time end = new Time(Utils.getTimeZone(getActivity(), mTZUpdater));
            end.set(mEndMillis);
            String lunarTime = Utils
                    .lunarCoupleDate(mContext, start, end, mShowHoliday);
            setTextCommon(view, R.id.event_info_lunar_date, lunarTime);
            setVisibilityCommon(view, R.id.event_info_lunar_date, View.VISIBLE);
        } else {
            setVisibilityCommon(view, R.id.event_info_lunar_date, View.GONE);
        }
        // End Motorola
        
        // Organizer view is setup in the updateCalendar method


        // Where
        if (location == null || location.trim().length() == 0) {
            setVisibilityCommon(view, R.id.where, View.GONE);
        } else {
            final TextView textView = mWhere;
            if (textView != null) {
                textView.setAutoLinkMask(0);
                textView.setText(location.trim());
                try {
                    linkifyTextView(textView);
                } catch (Exception ex) {
                    // unexpected
                    Log.e(TAG, "Linkification failed", ex);
                }
                // Begin motorola
                /* set links to map url or phone number */
                // Token string (Conference call number:) is appended by Motorola for conference
                // call feature. Even if there happens to have this token string in the orginal
                // Where field, it doesn't have much impact to us since we search to the end of
                // the string.
                ConferenceCallInfoDataMgr confDataMgr = new ConferenceCallInfoDataMgr(getActivity());
                final String tokenStr = getString(R.string.conference_call_number_colon);
                final int tokenIdx = location.indexOf(tokenStr);
                confDataMgr.parseConferenceCallInfo(textView.getText().toString());
                final String telNumPatternStr = confDataMgr.getMeetingNumberPattern();
                int telNumPatternIdx = confDataMgr.getMeetingNumberIndex();
                Pattern telNumberPattern = null;

                if (telNumPatternStr != null) {
                    try {
                        telNumberPattern = Pattern.compile(telNumPatternStr);
                    } catch (PatternSyntaxException e) {
                        // treat as no conference number if there's syntax error
                        telNumPatternIdx = -1;
                    }
                } else {
                    telNumPatternIdx = -1;
                }

                if (tokenIdx == -1) {
                    if (telNumPatternIdx == -1) {
                        // If there's no token string (Conference call number:) and there's no conference
                         // number in the Where field, treat whole location string as a map link.
                         Linkify.addLinks(textView, mWildcardPattern, "geo:0,0?q=");
                     } else {
                         // If there's no token string (Conference call number:) but there's conference
                       // number in the Where field, add link to the conference number only.
                       Linkify.addLinks(textView, telNumberPattern, "tel:", null, Linkify.sPhoneNumberTransformFilter);
                   }
               } else if (tokenIdx > 0) {
                   Pattern mapLinkPattern = Pattern.compile("^.*(?=[\\s]*" + tokenStr + ")");

                   if (telNumPatternIdx == -1) {
                       // If there's token string (Conference call number:) but there's no conference
                        // number in the Where field, treat the string before the token string as
                        // map link.
                        Linkify.addLinks(textView, mapLinkPattern, "geo:0,0?q=");
                    } else if (telNumPatternIdx < tokenIdx) {
                        // If there's token string (Conference call number:) and there's conferencenumber
                        // in the Where field and the conference number exists before the token string,
                       // add link to the conference number only.
                       Linkify.addLinks(textView, telNumberPattern, "tel:", null, Linkify.sPhoneNumberTransformFilter);
                   } else {
                       // If there's token string (Conference call number:) and there's conferencenumber
                        // in the Where field and the conference number exists after the token string,
                        // add map link before the token string and add link to the conference number
                        // after the token string.
                        final int startPos = telNumPatternIdx;
                        Linkify.addLinks(textView, mapLinkPattern, "geo:0,0?q=");
                        Linkify.addLinks(textView, telNumberPattern, "tel:",
                                new Linkify.MatchFilter() {
                                    public boolean acceptMatch (CharSequence s, int start, int end)
                                    {
                                        return (start > startPos);
                                    }
                                }, Linkify.sPhoneNumberTransformFilter);
                    }
                }
                // End motorola
                textView.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        try {
                            return v.onTouchEvent(event);
                        } catch (ActivityNotFoundException e) {
                            // ignore
                            return true;
                        }
                    }
                });
            }
        }

        // Description
        if (description != null && description.length() != 0) {
            mDesc.setText(description);
        }

        // Launch Custom App
        updateCustomAppButton();
    }

    private void updateCustomAppButton() {
        buttonSetup: {
            final Button launchButton = (Button) mView.findViewById(R.id.launch_custom_app_button);
            if (launchButton == null)
                break buttonSetup;

            final String customAppPackage = mEventCursor.getString(EVENT_INDEX_CUSTOM_APP_PACKAGE);
            final String customAppUri = mEventCursor.getString(EVENT_INDEX_CUSTOM_APP_URI);

            if (TextUtils.isEmpty(customAppPackage) || TextUtils.isEmpty(customAppUri))
                break buttonSetup;

            PackageManager pm = mContext.getPackageManager();
            if (pm == null)
                break buttonSetup;

            ApplicationInfo info;
            try {
                info = pm.getApplicationInfo(customAppPackage, 0);
                if (info == null)
                    break buttonSetup;
            } catch (NameNotFoundException e) {
                break buttonSetup;
            }

            Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
            final Intent intent = new Intent(CalendarContract.ACTION_HANDLE_CUSTOM_EVENT, uri);
            intent.setPackage(customAppPackage);
            intent.putExtra(CalendarContract.EXTRA_CUSTOM_APP_URI, customAppUri);
            intent.putExtra(EXTRA_EVENT_BEGIN_TIME, mStartMillis);

            // See if we have a taker for our intent
            if (pm.resolveActivity(intent, 0) == null)
                break buttonSetup;

            Drawable icon = pm.getApplicationIcon(info);
            if (icon != null) {

                Drawable[] d = launchButton.getCompoundDrawables();
                icon.setBounds(0, 0, mCustomAppIconSize, mCustomAppIconSize);
                launchButton.setCompoundDrawables(icon, d[1], d[2], d[3]);
            }

            CharSequence label = pm.getApplicationLabel(info);
            if (label != null && label.length() != 0) {
                launchButton.setText(label);
            } else if (icon == null) {
                // No icon && no label. Hide button?
                break buttonSetup;
            }

            // Launch custom app
            launchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        startActivityForResult(intent, 0);
                    } catch (ActivityNotFoundException e) {
                        // Shouldn't happen as we checked it already
                        setVisibilityCommon(mView, R.id.launch_custom_app_container, View.GONE);
                    }
                }
            });

            setVisibilityCommon(mView, R.id.launch_custom_app_container, View.VISIBLE);
            return;

        }

        setVisibilityCommon(mView, R.id.launch_custom_app_container, View.GONE);
        return;
    }

    /**
     * Finds North American Numbering Plan (NANP) phone numbers in the input text.
     *
     * @param text The text to scan.
     * @return A list of [start, end) pairs indicating the positions of phone numbers in the input.
     */
    // @VisibleForTesting
    static int[] findNanpPhoneNumbers(CharSequence text) {
        ArrayList<Integer> list = new ArrayList<Integer>();

        int startPos = 0;
        int endPos = text.length() - NANP_MIN_DIGITS + 1;
        if (endPos < 0) {
            return new int[] {};
        }

        /*
         * We can't just strip the whitespace out and crunch it down, because the whitespace
         * is significant.  March through, trying to figure out where numbers start and end.
         */
        while (startPos < endPos) {
            // skip whitespace
            while (Character.isWhitespace(text.charAt(startPos)) && startPos < endPos) {
                startPos++;
            }
            if (startPos == endPos) {
                break;
            }

            // check for a match at this position
            int matchEnd = findNanpMatchEnd(text, startPos);
            if (matchEnd > startPos) {
                list.add(startPos);
                list.add(matchEnd);
                startPos = matchEnd;    // skip past match
            } else {
                // skip to next whitespace char
                while (!Character.isWhitespace(text.charAt(startPos)) && startPos < endPos) {
                    startPos++;
                }
            }
        }

        int[] result = new int[list.size()];
        for (int i = list.size() - 1; i >= 0; i--) {
            result[i] = list.get(i);
        }
        return result;
    }

    /**
     * Checks to see if there is a valid phone number in the input, starting at the specified
     * offset.  If so, the index of the last character + 1 is returned.  The input is assumed
     * to begin with a non-whitespace character.
     *
     * @return Exclusive end position, or -1 if not a match.
     */
    private static int findNanpMatchEnd(CharSequence text, int startPos) {
        /*
         * A few interesting cases:
         *   94043                              # too short, ignore
         *   123456789012                       # too long, ignore
         *   +1 (650) 555-1212                  # 11 digits, spaces
         *   (650) 555-1212, (650) 555-1213     # two numbers, return first
         *   1-650-555-1212                     # 11 digits with leading '1'
         *   *#650.555.1212#*!                  # 10 digits, include #*, ignore trailing '!'
         *   555.1212                           # 7 digits
         *
         * For the most part we want to break on whitespace, but it's common to leave a space
         * between the initial '1' and/or after the area code.
         */

        int endPos = text.length();
        int curPos = startPos;
        int foundDigits = 0;
        char firstDigit = 'x';

        while (curPos <= endPos) {
            char ch;
            if (curPos < endPos) {
                ch = text.charAt(curPos);
            } else {
                ch = 27;    // fake invalid symbol at end to trigger loop break
            }

            if (Character.isDigit(ch)) {
                if (foundDigits == 0) {
                    firstDigit = ch;
                }
                foundDigits++;
                if (foundDigits > NANP_MAX_DIGITS) {
                    // too many digits, stop early
                    return -1;
                }
            } else if (Character.isWhitespace(ch)) {
                if (!(  (firstDigit == '1' && (foundDigits == 1 || foundDigits == 4)) ||
                        (foundDigits == 3)) ) {
                    break;
                }
            } else if (NANP_ALLOWED_SYMBOLS.indexOf(ch) == -1) {
                break;
            }
            // else it's an allowed symbol

            curPos++;
        }

        if ((firstDigit != '1' && (foundDigits == 7 || foundDigits == 10)) ||
                (firstDigit == '1' && foundDigits == 11)) {
            // match
            return curPos;
        }

        return -1;
    }

    /**
     * Replaces stretches of text that look like addresses and phone numbers with clickable
     * links.
     * <p>
     * This is really just an enhanced version of Linkify.addLinks().
     */
    private static void linkifyTextView(TextView textView) {
        /*
         * If the text includes a street address like "1600 Amphitheater Parkway, 94043",
         * the current Linkify code will identify "94043" as a phone number and invite
         * you to dial it (and not provide a map link for the address).  We want to
         * have better recognition of phone numbers without losing any of the existing
         * annotations.
         *
         * Ideally this would be addressed by improving Linkify.  For now we manage it as
         * a second pass over the text.
         *
         * URIs and e-mail addresses are pretty easy to pick out of text.  Phone numbers
         * are a bit tricky because they have radically different formats in different
         * countries, in terms of both the digits and the way in which they are commonly
         * written or presented (e.g. the punctuation and spaces in "(650) 555-1212").
         * The expected format of a street address is defined in WebView.findAddress().  It's
         * pretty narrowly defined, so it won't often match.
         *
         * The RFC 3966 specification defines the format of a "tel:" URI.
         */

        /*
         * If we're in the US, handle this specially.  Otherwise, punt to Linkify.
         */
        String defaultPhoneRegion = System.getProperty("user.region", "US");
        if (!defaultPhoneRegion.equals("US")) {
            Linkify.addLinks(textView, Linkify.ALL);
            return;
        }

        /*
         * Start by letting Linkify find anything that isn't a phone number.  We have to let it
         * run first because every invocation removes all previous URLSpan annotations.
         *
         * Ideally we'd use the external/libphonenumber routines, but those aren't available
         * to unbundled applications.
         */
        boolean linkifyFoundLinks = Linkify.addLinks(textView,
                Linkify.ALL & ~(Linkify.PHONE_NUMBERS));

        /*
         * Search for phone numbers.
         *
         * Some URIs contain strings of digits that look like phone numbers.  If both the URI
         * scanner and the phone number scanner find them, we want the URI link to win.  Since
         * the URI scanner runs first, we just need to avoid creating overlapping spans.
         */
        CharSequence text = textView.getText();
        int[] phoneSequences = findNanpPhoneNumbers(text);

        /*
         * If the contents of the TextView are already Spannable (which will be the case if
         * Linkify found stuff, but might not be otherwise), we can just add annotations
         * to what's there.  If it's not, and we find phone numbers, we need to convert it to
         * a Spannable form.  (This mimics the behavior of Linkable.addLinks().)
         */
        Spannable spanText;
        if (text instanceof SpannableString) {
            spanText = (SpannableString) text;
        } else {
            spanText = SpannableString.valueOf(text);
        }

        /*
         * Get a list of any spans created by Linkify, for the overlapping span check.
         */
        URLSpan[] existingSpans = spanText.getSpans(0, spanText.length(), URLSpan.class);

        /*
         * Insert spans for the numbers we found.  We generate "tel:" URIs.
         */
        int phoneCount = 0;
        for (int match = 0; match < phoneSequences.length / 2; match++) {
            int start = phoneSequences[match*2];
            int end = phoneSequences[match*2 + 1];

            if (spanWillOverlap(spanText, existingSpans, start, end)) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    CharSequence seq = text.subSequence(start, end);
                    Log.v(TAG, "Not linkifying " + seq + " as phone number due to overlap");
                }
                continue;
            }

            /*
             * The Linkify code takes the matching span and strips out everything that isn't a
             * digit or '+' sign.  We do the same here.  Extension numbers will get appended
             * without a separator, but the dialer wasn't doing anything useful with ";ext="
             * anyway.
             */

            //String dialStr = phoneUtil.format(match.number(),
            //        PhoneNumberUtil.PhoneNumberFormat.RFC3966);
            StringBuilder dialBuilder = new StringBuilder();
            for (int i = start; i < end; i++) {
                char ch = spanText.charAt(i);
                if (ch == '+' || Character.isDigit(ch)) {
                    dialBuilder.append(ch);
                }
            }
            URLSpan span = new URLSpan("tel:" + dialBuilder.toString());

            spanText.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            phoneCount++;
        }

        if (phoneCount != 0) {
            // If we had to "upgrade" to Spannable, store the object into the TextView.
            if (spanText != text) {
                textView.setText(spanText);
            }

            // Linkify.addLinks() sets the TextView movement method if it finds any links.  We
            // want to do the same here.  (This is cloned from Linkify.addLinkMovementMethod().)
            MovementMethod mm = textView.getMovementMethod();

            if ((mm == null) || !(mm instanceof LinkMovementMethod)) {
                if (textView.getLinksClickable()) {
                    textView.setMovementMethod(LinkMovementMethod.getInstance());
                }
            }
        }

        if (!linkifyFoundLinks && phoneCount == 0) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "No linkification matches, using geo default");
            }
            Linkify.addLinks(textView, mWildcardPattern, "geo:0,0?q=");
        }
    }

    /**
     * Determines whether a new span at [start,end) will overlap with any existing span.
     */
    private static boolean spanWillOverlap(Spannable spanText, URLSpan[] spanList, int start,
            int end) {
        if (start == end) {
            // empty span, ignore
            return false;
        }
        for (URLSpan span : spanList) {
            int existingStart = spanText.getSpanStart(span);
            int existingEnd = spanText.getSpanEnd(span);
            if ((start >= existingStart && start < existingEnd) ||
                    end > existingStart && end <= existingEnd) {
                return true;
            }
        }

        return false;
    }

    private void sendAccessibilityEvent() {
        AccessibilityManager am =
            (AccessibilityManager) getActivity().getSystemService(Service.ACCESSIBILITY_SERVICE);
        if (!am.isEnabled()) {
            return;
        }

        AccessibilityEvent event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        event.setClassName(getClass().getName());
        event.setPackageName(getActivity().getPackageName());
        List<CharSequence> text = event.getText();

        addFieldToAccessibilityEvent(text, mTitle, null);
        addFieldToAccessibilityEvent(text, mWhenDateTime, null);
        addFieldToAccessibilityEvent(text, mWhere, null);
        addFieldToAccessibilityEvent(text, null, mDesc);

        RadioGroup response = (RadioGroup) getView().findViewById(R.id.response_value);
        // BEGIN, Motorola, bck683, 38173,for Active sync
        //Disable the attendees response
        // modified by amt_sunli 2013-1-9 for feature 38173 begin
        if (Utils.isNeedRemoveActiveSync) {
            for (int i = 0; i < response.getChildCount(); i++) {
                response.getChildAt(i).setEnabled(false);
            }
        }
        // modified by amt_sunli 2013-1-9 for feature 38173 end
        //END, Motorola
        if (response.getVisibility() == View.VISIBLE) {
            int id = response.getCheckedRadioButtonId();
            if (id != View.NO_ID) {
                text.add(((TextView) getView().findViewById(R.id.response_label)).getText());
                text.add((((RadioButton) (response.findViewById(id))).getText() + PERIOD_SPACE));
            }
        }

        am.sendAccessibilityEvent(event);
    }

    private void addFieldToAccessibilityEvent(List<CharSequence> text, TextView tv,
            ExpandableTextView etv) {
        CharSequence cs;
        if (tv != null) {
            cs = tv.getText();
        } else if (etv != null) {
            cs = etv.getText();
        } else {
            return;
        }

        if (!TextUtils.isEmpty(cs)) {
            cs = cs.toString().trim();
            if (cs.length() > 0) {
                text.add(cs);
                text.add(PERIOD_SPACE);
            }
        }
    }

    private void updateCalendar(View view) {
        mCalendarOwnerAccount = "";
        if (mCalendarsCursor != null && mEventCursor != null) {
            mCalendarsCursor.moveToFirst();
            String tempAccount = mCalendarsCursor.getString(CALENDARS_INDEX_OWNER_ACCOUNT);
            mCalendarOwnerAccount = (tempAccount == null) ? "" : tempAccount;
            mOwnerCanRespond = mCalendarsCursor.getInt(CALENDARS_INDEX_OWNER_CAN_RESPOND) != 0;
            mSyncAccountName = mCalendarsCursor.getString(CALENDARS_INDEX_ACCOUNT_NAME);

            String displayName = mCalendarsCursor.getString(CALENDARS_INDEX_DISPLAY_NAME);

            // start duplicate calendars query
           // Begin Motorola
//            mHandler.startQuery(TOKEN_QUERY_DUPLICATE_CALENDARS, null, Calendars.CONTENT_URI,
//                    CALENDARS_PROJECTION, CALENDARS_DUPLICATE_NAME_WHERE,
//                    new String[] {displayName}, null);
            mHandler.startQuery(TOKEN_QUERY_DUPLICATE_CALENDARS, null,
                    decorateForScratch(Calendars.CONTENT_URI),
                    CALENDARS_PROJECTION, CALENDARS_DUPLICATE_NAME_WHERE,
                    new String[] {displayName}, null);
            // End Motorola
            mEventOrganizerEmail = mEventCursor.getString(EVENT_INDEX_ORGANIZER);
            // BEGIN MOTOROLA - IKPIM-936: Support reply/reply all on event detail
            mEventOrganizer = mEventCursor.getString(EVENT_INDEX_ORGANIZER);
            mIsOrganizer = mCalendarOwnerAccount.equalsIgnoreCase(mEventOrganizer);
            setTextCommon(view, R.id.organizer, mEventOrganizer);

            mCalendarId = mCalendarsCursor.getLong(CALENDARS_INDEX_ID);
            mSyncAccountType = mCalendarsCursor.getString(CALENDARS_INDEX_ACCOUNT_TYPE);
            // END MOTOROLA - IKPIM-936
            if (!TextUtils.isEmpty(mEventOrganizerEmail) &&
                    !mEventOrganizerEmail.endsWith(Utils.MACHINE_GENERATED_ADDRESS)) {
                mEventOrganizerDisplayName = mEventOrganizerEmail;
            }

            if (!mIsOrganizer && !TextUtils.isEmpty(mEventOrganizerDisplayName)) {
                setTextCommon(view, R.id.organizer, mEventOrganizerDisplayName);
                setVisibilityCommon(view, R.id.organizer_container, View.VISIBLE);
            } else {
                setVisibilityCommon(view, R.id.organizer_container, View.GONE);
            }
            mHasAttendeeData = mEventCursor.getInt(EVENT_INDEX_HAS_ATTENDEE_DATA) != 0;
            mCanModifyCalendar = mEventCursor.getInt(EVENT_INDEX_ACCESS_LEVEL)
                    >= Calendars.CAL_ACCESS_CONTRIBUTOR;
            // TODO add "|| guestCanModify" after b/1299071 is fixed
            mCanModifyEvent = mCanModifyCalendar && mIsOrganizer;
            mIsBusyFreeCalendar =
                    mEventCursor.getInt(EVENT_INDEX_ACCESS_LEVEL) == Calendars.CAL_ACCESS_FREEBUSY;

            if (!mIsBusyFreeCalendar) {

                View b = mView.findViewById(R.id.edit);
                b.setEnabled(true);
                b.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        doEdit();
                        // For dialogs, just close the fragment
                        // For full screen, close activity on phone, leave it for tablet
                        if (mIsDialog) {
                            EventInfoFragment.this.dismiss();
                        }
                        else if (!mIsTabletConfig){
                            getActivity().finish();
                        }
                    }
                });
            }
            View button;
            if (mCanModifyCalendar) {
                button = mView.findViewById(R.id.delete);
                if (button != null) {
                    button.setEnabled(true);
                    button.setVisibility(View.VISIBLE);
                }
            }
            if (mCanModifyEvent) {
                button = mView.findViewById(R.id.edit);
                if (button != null) {
                    button.setEnabled(true);
                    button.setVisibility(View.VISIBLE);
                }
            }

            if ((!mIsDialog && !mIsTabletConfig ||
                    mWindowStyle == EventInfoFragment.FULL_WINDOW_STYLE) && mMenu != null) {
                mActivity.invalidateOptionsMenu();
            }
            // Begin Motorola
            updateOptionMenuIfAny();
            // End Motorola
        } else {
            setVisibilityCommon(view, R.id.calendar, View.GONE);
            sendAccessibilityEventIfQueryDone(TOKEN_QUERY_DUPLICATE_CALENDARS);
        }
    }
    // Begin Motorola
    private void updateOptionMenuIfAny() {
        if ((!mIsDialog && !mIsTabletConfig ||
                mWindowStyle == EventInfoFragment.FULL_WINDOW_STYLE) && mMenu != null) {
            mActivity.invalidateOptionsMenu();
        }
    }
    // End Motorola
    /**
     *
     */
    private void updateMenu() {
        if (mMenu == null) {
            return;
        }
        MenuItem delete = mMenu.findItem(R.id.info_action_delete);
        MenuItem edit = mMenu.findItem(R.id.info_action_edit);
        // BEGIN, Motorola, bck683, 38173,for Active sync
        //Disable the edit and delete options for the events with multiple reciepents
        // modified by amt_sunli 2013-1-9 for feature 38173 begin
        if (Utils.isNeedRemoveActiveSync) {
            if (delete != null || edit != null) {
                if((mAttendeeSize > 1) && (!mIsOrganizer)){
                    delete.setVisible(false);
                    delete.setEnabled(false);
                } else{
                    delete.setVisible(mCanModifyCalendar);
                    delete.setEnabled(mCanModifyCalendar);
                }
            }
        } else {
            if (delete != null) {
                delete.setVisible(mCanModifyCalendar);
                delete.setEnabled(mCanModifyCalendar);
            }
        }
        // modified by amt_sunli 2013-1-9 for feature 38173 end
        //END, Motorola
        if (edit != null) {
            edit.setVisible(mCanModifyEvent);
            edit.setEnabled(mCanModifyEvent);
        }
        // BEGIN MOTOROLA - IKPIM-936: Support reply/reply all on event detail
        MenuItem reply = mMenu.findItem(R.id.info_action_reply);
        MenuItem replyAll = mMenu.findItem(R.id.info_action_reply_all);
        // BEGIN MOTOROLA - IKHSS7-364: Hide reply/reply all for local account event
        if (reply != null) {
            reply.setVisible(mAttendeesCount > 0 && !mIsOrganizer && mSyncAccountType != null
                    && !CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType));
            reply.setEnabled(mAttendeesCount > 0 && !mIsOrganizer && mSyncAccountType != null
                    && !CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType));
        }
        if (replyAll != null) {
            replyAll.setVisible(mAttendeesCount > 0 && mSyncAccountType != null
                    && !CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType));
            replyAll.setEnabled(mAttendeesCount > 0 && mSyncAccountType != null
                    && !CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType));
        }
        // END MOTOROLA - IKHSS7-364
        // END MOTOROLA - IKPIM-936

        // BEGIN MOTOROLA - IKPIM-974: Support forward on event detail
        MenuItem forward = mMenu.findItem(R.id.info_action_forward);
        if (forward != null) {
            if (!mIsIcsImport
                    && (mSyncAccountType != null)
                    && (!CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType))) {
                forward.setVisible(true);
                forward.setEnabled(true);
            } else {
                forward.setVisible(false);
                forward.setEnabled(false);
            }

        }
        // END MOTOROLA - IKPIM-974

        // Begin Motorola
        MenuItem printEventItem = mMenu.findItem(R.id.info_action_print);
        MenuItem addEventToTasksItem = mMenu.findItem(R.id.info_action_add_event_to_tasks);
        int extCapabilites = Utils.getCalendarExtCapabilities(mContext);
        printEventItem.setVisible(false);
        if ((extCapabilites & Utils.EXT_CAPABILITY_PRINT_ICS) != 0) {
            printEventItem.setVisible(true);
        }
        addEventToTasksItem.setVisible(false);
        if ((extCapabilites & Utils.EXT_CAPABILITY_ADD_TO_TASKS) != 0) {
            addEventToTasksItem.setVisible(true);
        }
        // End Motorola
    }

    private void updateAttendees(View view) {
//        if (mAcceptedAttendees.size() + mDeclinedAttendees.size() +
//                mTentativeAttendees.size() + mNoResponseAttendees.size() > 0) {
//            mLongAttendees.clearAttendees();
//            (mLongAttendees).addAttendees(mAcceptedAttendees);
//            (mLongAttendees).addAttendees(mDeclinedAttendees);
//            (mLongAttendees).addAttendees(mTentativeAttendees);
//            (mLongAttendees).addAttendees(mNoResponseAttendees);
//            mLongAttendees.setEnabled(false);
//            mLongAttendees.setVisibility(View.VISIBLE);
//        } else {
//            mLongAttendees.setVisibility(View.GONE);
//        }

        updateEmailAttendees();
    }

    /**
     * Initializes the list of 'to' and 'cc' emails from the attendee list.
     */
    private void updateEmailAttendees() {
        // The declined attendees will go in the 'cc' line, all others will go in the 'to' line.
        mToEmails = new ArrayList<String>();
        for (Attendee attendee : mAcceptedAttendees) {
            addIfEmailable(mToEmails, attendee.mEmail);
        }
        for (Attendee attendee : mTentativeAttendees) {
            addIfEmailable(mToEmails, attendee.mEmail);
        }
        for (Attendee attendee : mNoResponseAttendees) {
            addIfEmailable(mToEmails, attendee.mEmail);
        }
        mCcEmails = new ArrayList<String>();
        for (Attendee attendee : this.mDeclinedAttendees) {
            addIfEmailable(mCcEmails, attendee.mEmail);
        }

        // The meeting organizer doesn't appear as an attendee sometimes (particularly
        // when viewing someone else's calendar), so add the organizer now.
        if (mEventOrganizerEmail != null && !mToEmails.contains(mEventOrganizerEmail) &&
                !mCcEmails.contains(mEventOrganizerEmail)) {
            addIfEmailable(mToEmails, mEventOrganizerEmail);
        }

        // The Email app behaves strangely when there is nothing in the 'mailto' part,
        // so move all the 'cc' emails to the 'to' list.  Gmail works fine though.
        if (mToEmails.size() <= 0 && mCcEmails.size() > 0) {
            mToEmails.addAll(mCcEmails);
            mCcEmails.clear();
        }

        if (mToEmails.size() <= 0) {
            setVisibilityCommon(mView, R.id.email_attendees_container, View.GONE);
        } else {
            setVisibilityCommon(mView, R.id.email_attendees_container, View.VISIBLE);
        }
    }

    public void initReminders(View view, Cursor cursor) {

        // Add reminders
        mOriginalReminders.clear();
        mUnsupportedReminders.clear();
        while (cursor.moveToNext()) {
            int minutes = cursor.getInt(EditEventHelper.REMINDERS_INDEX_MINUTES);
            int method = cursor.getInt(EditEventHelper.REMINDERS_INDEX_METHOD);

            if (method != Reminders.METHOD_DEFAULT && !mReminderMethodValues.contains(method)) {
                // Stash unsupported reminder types separately so we don't alter
                // them in the UI
                mUnsupportedReminders.add(ReminderEntry.valueOf(minutes, method));
            } else {
                mOriginalReminders.add(ReminderEntry.valueOf(minutes, method));
            }
        }
        // Sort appropriately for display (by time, then type)
        Collections.sort(mOriginalReminders);

        if (mUserModifiedReminders) {
            // If the user has changed the list of reminders don't change what's
            // shown.
            return;
        }

        LinearLayout parent = (LinearLayout) mScrollView
                .findViewById(R.id.reminder_items_container);
        if (parent != null) {
            parent.removeAllViews();
        }
        if (mReminderViews != null) {
            mReminderViews.clear();
        }

        if (mHasAlarm) {
            ArrayList<ReminderEntry> reminders = mOriginalReminders;
            // Insert any minute values that aren't represented in the minutes list.
            for (ReminderEntry re : reminders) {
                EventViewUtils.addMinutesToList(
                        mActivity, mReminderMinuteValues, mReminderMinuteLabels, re.getMinutes());
            }
            // Create a UI element for each reminder.  We display all of the reminders we get
            // from the provider, even if the count exceeds the calendar maximum.  (Also, for
            // a new event, we won't have a maxReminders value available.)
            for (ReminderEntry re : reminders) {
                EventViewUtils.addReminder(mActivity, mScrollView, this, mReminderViews,
                        mReminderMinuteValues, mReminderMinuteLabels, mReminderMethodValues,
                        mReminderMethodLabels, re, Integer.MAX_VALUE, mReminderChangeListener);
            }
            EventViewUtils.updateAddReminderButton(mView, mReminderViews, mMaxReminders);
            // TODO show unsupported reminder types in some fashion.
            // Begin Motorola
            // Prevent reminders info from being edited for iCal-imported event
            if (mIsIcsImport && mReminderViews != null) {
                for (LinearLayout rv : mReminderViews) {
                    View spinner = (View) rv.findViewById(R.id.reminder_minutes_value);
                    spinner.setEnabled(false);
                    spinner = (View) rv.findViewById(R.id.reminder_method_value);
                    spinner.setEnabled(false);
                    View reminderRemoveButton =
                        (View) rv.findViewById(R.id.reminder_remove);
                    reminderRemoveButton.setVisibility(View.GONE);
                }
            }
            // End Motorola
        }
        //Begin Motorola - Disable/Enable reminder button
        View reminderAddButton = mView.findViewById(R.id.reminder_add);
        //add by amt_xulei for SWITCHUITWO-554 at 2013-1-21
        if (mIsIcsImport) {
        	// Hide reminderAddButton for iCal-imported event
        	reminderAddButton.setVisibility(View.GONE);
        }
        //end add
        if (mReminderViews.size() > mMaxReminders-1) {
            reminderAddButton.setEnabled(false);
        } else {
            reminderAddButton.setEnabled(true);
        }
        //End Motorola
    }

    void updateResponse(View view) {
        // we only let the user accept/reject/etc. a meeting if:
        // a) you can edit the event's containing calendar AND
        // b) you're not the organizer and only attendee AND
        // c) organizerCanRespond is enabled for the calendar
        // (if the attendee data has been hidden, the visible number of attendees
        // will be 1 -- the calendar owner's).
        // (there are more cases involved to be 100% accurate, such as
        // paying attention to whether or not an attendee status was
        // included in the feed, but we're currently omitting those corner cases
        // for simplicity).

        // TODO Switch to EditEventHelper.canRespond when this class uses CalendarEventModel.
        if (!mCanModifyCalendar || (mHasAttendeeData && mIsOrganizer && mNumOfAttendees <= 1) ||
                (mIsOrganizer && !mOwnerCanRespond)||
                // Begin Motorola
                mIsIcsImport || CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType)) {
               // End Motorola
            setVisibilityCommon(view, R.id.response_container, View.GONE);
            return;
        }

        setVisibilityCommon(view, R.id.response_container, View.VISIBLE);


        int response;
        if (mUserSetResponse != Attendees.ATTENDEE_STATUS_NONE) {
            response = mUserSetResponse;
        } else if (mAttendeeResponseFromIntent != Attendees.ATTENDEE_STATUS_NONE) {
            response = mAttendeeResponseFromIntent;
        } else {
            response = mOriginalAttendeeResponse;
        }
        int buttonToCheck = findButtonIdForResponse(response);
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.response_value);
        // BEGIN, Motorola, bck683, 38173,for Active sync
        // Disable the attendees response
        // modified by amt_sunli 2013-1-9 for feature 38173 begin
        if (Utils.isNeedRemoveActiveSync) {
            for (int i = 0; i < radioGroup.getChildCount(); i++) {
                radioGroup.getChildAt(i).setEnabled(false);
           }
        }
        // modified by amt_sunli 2013-1-9 for feature 38173 end
        // END, Motorola
        radioGroup.check(buttonToCheck); // -1 clear all radio buttons
        radioGroup.setOnCheckedChangeListener(this);
    }

    private void setTextCommon(View view, int id, CharSequence text) {
        TextView textView = (TextView) view.findViewById(id);
        if (textView == null)
            return;
        textView.setText(text);
    }

    private void setVisibilityCommon(View view, int id, int visibility) {
        View v = view.findViewById(id);
        if (v != null) {
            v.setVisibility(visibility);
        }
        return;
    }

    /**
     * Taken from com.google.android.gm.HtmlConversationActivity
     *
     * Send the intent that shows the Contact info corresponding to the email address.
     */
    public void showContactInfo(Attendee attendee, Rect rect) {
        // First perform lookup query to find existing contact
        final ContentResolver resolver = getActivity().getContentResolver();
        final String address = attendee.mEmail;
        final Uri dataUri = Uri.withAppendedPath(CommonDataKinds.Email.CONTENT_FILTER_URI,
                Uri.encode(address));
        final Uri lookupUri = ContactsContract.Data.getContactLookupUri(resolver, dataUri);

        if (lookupUri != null) {
            // Found matching contact, trigger QuickContact
            QuickContact.showQuickContact(getActivity(), rect, lookupUri,
                    QuickContact.MODE_MEDIUM, null);
        } else {
            // No matching contact, ask user to create one
            final Uri mailUri = Uri.fromParts("mailto", address, null);
            final Intent intent = new Intent(Intents.SHOW_OR_CREATE_CONTACT, mailUri);

            // Pass along full E-mail string for possible create dialog
            Rfc822Token sender = new Rfc822Token(attendee.mName, attendee.mEmail, null);
            intent.putExtra(Intents.EXTRA_CREATE_DESCRIPTION, sender.toString());

            // Only provide personal name hint if we have one
            final String senderPersonal = attendee.mName;
            if (!TextUtils.isEmpty(senderPersonal)) {
                intent.putExtra(Intents.Insert.NAME, senderPersonal);
            }

            startActivity(intent);
        }
    }

    @Override
    public void onPause() {
        mIsPaused = true;
        mHandler.removeCallbacks(onDeleteRunnable);
        super.onPause();
        // Remove event deletion alert box since it is being rebuild in the OnResume
        // This is done to get the same behavior on OnResume since the AlertDialog is gone on
        // rotation but not if you press the HOME key
        if (mDeleteDialogVisible && mDeleteHelper != null) {
            // Begin motorola
            mDeleteOptionIndex = mDeleteHelper.getDeleteOptionIndex();
            // End motorola
            mDeleteHelper.dismissAlertDialog();
            mDeleteHelper = null;
        }
        // Begin motorola
        //Remove event change response dialog since it's being rebuild in the onResume
        if (mChangeResponseDialogVisible && mEditResponseHelper != null) {
            mChangeResponseOptionIndex = mEditResponseHelper.getWhichEvents();
            mEditResponseHelper.dismissAlertDialog();
        }
        // End motorola
    }

    @Override
    public void onResume() {
        super.onResume();
        // Begin Motorola
        if (mIsPaused) {
            // Check if the event has been deleted
            if (Utils.isEventDeleted(mContext, mUri)) {
                mActivity.finish();
                Log.v(TAG, "The event is deleted or with DELETED or CANCELED flag, so finish the activity.");
            }
        }
        // End Motorola
        if (mIsDialog) {
            setDialogSize(getActivity().getResources());
            applyDialogParams();
        }
        mIsPaused = false;
        if (mDismissOnResume) {
            mHandler.post(onDeleteRunnable);
        }
        // Begin motorola
//        if(mSavedInstanceState != null) {
//            if (mConfDataMgr == null) {
//                mConfDataMgr = new ConferenceCallInfoDataMgr(getActivity());
//                mConfSettingMgr = new ConferenceCallInfoSettingMgr(mConfDataMgr);
//                mConfSettingMgr.loadConferenceCallSetting(getActivity(), mOrganizerEmail, mEventId, mEventLocation);
//                restoreManagedDialogs(mSavedInstanceState);
//            }
            //updateDynamicInfoOfConfCallButton();
//        }
       // End motorola
        // Display the "delete confirmation" dialog if needed
        if (mDeleteDialogVisible) {
            mDeleteHelper = new DeleteEventHelper(
                    mContext, mActivity,
                    !mIsDialog && !mIsTabletConfig /* exitWhenDone */);
            mDeleteHelper.setOnDismissListener(createDeleteOnDismissListener());
            // Begin motorola
            mDeleteHelper.delete(mStartMillis, mEndMillis, mEventId, mDeleteOptionIndex, onDeleteRunnable);
            // End motorola
        }
        // Begin motorola
        //Display the "Change response" dialog if needed
        if(mChangeResponseDialogVisible) {
            mEditResponseHelper.setOnDismissListener(createChangeResponseOnDismissListener());
            mEditResponseHelper.showDialog(mChangeResponseOptionIndex);
        }
        // End motorola
    }

    @Override
    public void eventsChanged() {
    }

    @Override
    public long getSupportedEventTypes() {
        return EventType.EVENTS_CHANGED;
    }

    @Override
    public void handleEvent(EventInfo event) {
        if (event.eventType == EventType.EVENTS_CHANGED && mHandler != null) {
            // reload the data
            reloadEvents();
        }
    }

    public void reloadEvents() {
        mHandler.startQuery(TOKEN_QUERY_EVENT, null, mUri, EVENT_PROJECTION,
                null, null, null);
    }

    @Override
    public void onClick(View view) {

        // This must be a click on one of the "remove reminder" buttons
        LinearLayout reminderItem = (LinearLayout) view.getParent();
        LinearLayout parent = (LinearLayout) reminderItem.getParent();
        parent.removeView(reminderItem);
        mReminderViews.remove(reminderItem);
        mUserModifiedReminders = true;
        EventViewUtils.updateAddReminderButton(mView, mReminderViews, mMaxReminders);
    }


    /**
     * Add a new reminder when the user hits the "add reminder" button.  We use the default
     * reminder time and method.
     */
    private void addReminder() {
        // TODO: when adding a new reminder, make it different from the
        // last one in the list (if any).
        if (mDefaultReminderMinutes == GeneralPreferences.NO_REMINDER) {
            EventViewUtils.addReminder(mActivity, mScrollView, this, mReminderViews,
                    mReminderMinuteValues, mReminderMinuteLabels, mReminderMethodValues,
                    mReminderMethodLabels,
                    ReminderEntry.valueOf(GeneralPreferences.REMINDER_DEFAULT_TIME), mMaxReminders,
                    mReminderChangeListener);
        } else {
            EventViewUtils.addReminder(mActivity, mScrollView, this, mReminderViews,
                    mReminderMinuteValues, mReminderMinuteLabels, mReminderMethodValues,
                    mReminderMethodLabels, ReminderEntry.valueOf(mDefaultReminderMinutes),
                    mMaxReminders, mReminderChangeListener);
        }

        EventViewUtils.updateAddReminderButton(mView, mReminderViews, mMaxReminders);
    }

    synchronized private void prepareReminders() {
        // Nothing to do if we've already built these lists _and_ we aren't
        // removing not allowed methods
        if (mReminderMinuteValues != null && mReminderMinuteLabels != null
                && mReminderMethodValues != null && mReminderMethodLabels != null
                && mCalendarAllowedReminders == null) {
            return;
        }
        // Load the labels and corresponding numeric values for the minutes and methods lists
        // from the assets.  If we're switching calendars, we need to clear and re-populate the
        // lists (which may have elements added and removed based on calendar properties).  This
        // is mostly relevant for "methods", since we shouldn't have any "minutes" values in a
        // new event that aren't in the default set.
        Resources r = mActivity.getResources();
        mReminderMinuteValues = loadIntegerArray(r, R.array.reminder_minutes_values);
        mReminderMinuteLabels = loadStringArray(r, R.array.reminder_minutes_labels);
        mReminderMethodValues = loadIntegerArray(r, R.array.reminder_methods_values);
        mReminderMethodLabels = loadStringArray(r, R.array.reminder_methods_labels);

        // Remove any reminder methods that aren't allowed for this calendar.  If this is
        // a new event, mCalendarAllowedReminders may not be set the first time we're called.
        if (mCalendarAllowedReminders != null) {
            EventViewUtils.reduceMethodList(mReminderMethodValues, mReminderMethodLabels,
                    mCalendarAllowedReminders);
        }
        if (mView != null) {
            mView.invalidate();
        }
    }


    private boolean saveReminders() {
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>(3);

        // Read reminders from UI
        mReminders = EventViewUtils.reminderItemsToReminders(mReminderViews,
                mReminderMinuteValues, mReminderMethodValues);
        mOriginalReminders.addAll(mUnsupportedReminders);
        Collections.sort(mOriginalReminders);
        mReminders.addAll(mUnsupportedReminders);
        Collections.sort(mReminders);

        // added by amt_sunli 2012-11-26 SWITCHUITWO-59 begin
        // remove the same reminders
        if (mReminders != null && mReminders.size() > 1) {
            Map<ReminderEntry, ReminderEntry> map = new HashMap<ReminderEntry, ReminderEntry>();
            List<ReminderEntry> repeatEntry = new ArrayList<ReminderEntry>();
            for (int i = 0; i < mReminders.size(); i++){
                if (map.containsKey(mReminders.get(i))){
                    repeatEntry.add(mReminders.get(i));
                }else {
                    map.put(mReminders.get(i), mReminders.get(i));
                }
            }
            for (int i = 0; i < repeatEntry.size(); i++){
                mReminders.remove(repeatEntry.get(i));
            }
        }
        // added by amt_sunli 2012-11-26 SWITCHUITWO-59 end

        // Check if there are any changes in the reminder
        boolean changed = EditEventHelper.saveReminders(ops, mEventId, mReminders,
                mOriginalReminders, false /* no force save */);
        
        if (!changed) {
            return false;
        }

        // save new reminders
        AsyncQueryService service = new AsyncQueryService(getActivity());
        service.startBatch(0, null, Calendars.CONTENT_URI.getAuthority(), ops, 0);
        // Update the "hasAlarm" field for the event
        Uri uri = ContentUris.withAppendedId(Events.CONTENT_URI, mEventId);
        int len = mReminders.size();
        boolean hasAlarm = len > 0;
        if (hasAlarm != mHasAlarm) {
            ContentValues values = new ContentValues();
            values.put(Events.HAS_ALARM, hasAlarm ? 1 : 0);
            service.startUpdate(0, null, uri, values, null, null, 0);
        }
        return true;
    }

    /**
     * Adds the attendee's email to the list if:
     *   (1) the attendee is not a resource like a conference room or another calendar.
     *       Catch most of these by filtering out suffix calendar.google.com.
     *   (2) the attendee is not the viewer, to prevent mailing himself.
     */
    private void addIfEmailable(ArrayList<String> emailList, String email) {
        if (Utils.isEmailableFrom(email, mSyncAccountName)) {
            emailList.add(email);
        }
    }

    /**
     * Email all the attendees of the event, except for the viewer (so as to not email
     * himself) and resources like conference rooms.
     */
    private void emailAttendees() {
        String eventTitle = (mTitle == null || mTitle.getText() == null) ? null :
                mTitle.getText().toString();
        Intent emailIntent = Utils.createEmailAttendeesIntent(getActivity().getResources(),
                eventTitle, null /* body */, mToEmails, mCcEmails, mCalendarOwnerAccount);
        startActivity(emailIntent);
    }

    /**
     * Loads an integer array asset into a list.
     */
    private static ArrayList<Integer> loadIntegerArray(Resources r, int resNum) {
        int[] vals = r.getIntArray(resNum);
        int size = vals.length;
        ArrayList<Integer> list = new ArrayList<Integer>(size);

        for (int i = 0; i < size; i++) {
            list.add(vals[i]);
        }

        return list;
    }
    /**
     * Loads a String array asset into a list.
     */
    private static ArrayList<String> loadStringArray(Resources r, int resNum) {
        String[] labels = r.getStringArray(resNum);
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(labels));
        return list;
    }

    public void onDeleteStarted() {
        mEventDeletionStarted = true;
    }

    private Dialog.OnDismissListener createDeleteOnDismissListener() {
        return new Dialog.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        // Since OnPause will force the dialog to dismiss , do
                        // not change the dialog status
                        if (!mIsPaused) {
                            mDeleteDialogVisible = false;
                        }
                    }
                };
    }

    public long getEventId() {
        return mEventId;
    }

    public long getStartMillis() {
        return mStartMillis;
    }
    public long getEndMillis() {
        return mEndMillis;
    }
    private void setDialogSize(Resources r) {
        mDialogWidth = (int)r.getDimension(R.dimen.event_info_dialog_width);
        mDialogHeight = (int)r.getDimension(R.dimen.event_info_dialog_height);
    }

  //MOTOROLA Begin, Lunar Calendar
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(GeneralPreferences.KEY_SHOW_LUNAR)) {
            boolean newSetting = sharedPreferences.getBoolean(key, GeneralPreferences.DEFAULT_VALUE_OF_SHOW_LUNAR);
            if(mShowLunar != newSetting) {
                mShowLunar = newSetting;
                Log.d(TAG, "  ShowLunar setting changed!!!");
            }
        }else if(key.equals(GeneralPreferences.KEY_SHOW_HOLIDAY)){
             boolean newSetting = sharedPreferences.getBoolean(key, GeneralPreferences.DEFAULT_VALUE_OF_SHOW_HOLIDAY);
             if(mShowHoliday != newSetting) {
                 mShowHoliday = newSetting;
                 Log.d(TAG, "  ShowHoliday setting changed!!!");
             }
        }
    }
    //MOTOROLA End, Lunar Calendar
    // Begin Motorola
    private Dialog.OnDismissListener createChangeResponseOnDismissListener() {
        return new Dialog.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        // Since OnPause will force the dialog to dismiss , do
                        // not change the dialog status
                        if (!mIsPaused) {
                            mChangeResponseDialogVisible = false;
                        }
                    }
                };
    }

    // Begin Motorola
    /**
     * Decoreate given uri with moto_visibility parameter. If an iCal file is being
     * previewed, the moto_visibility parameter will be set to false. Otherwise the
     * orginal uri will be returned.
     *
     * @param uri
     *        The content uri to be decoreated
     *
     * @return The decoreated uri
     */
    private Uri decorateForScratch(Uri uri) {
        if (mIsIcsImport && (uri != null)) {
            Uri.Builder uriBuilder = uri.buildUpon();
            // TODO: consolidate the string constants
            uriBuilder.appendQueryParameter("use_hidden_calendar", String.valueOf(true));
            uri = uriBuilder.build();
        }

        return uri;
    }

    /**
     * initExtendedProperties will only be called when it's an iCal-imported event.
     */
    private void initExtendedProperties(View view, Cursor cursor) {
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(EXTENDED_PROPERTIES_INDEX_NAME);
                String value = cursor.getString(EXTENDED_PROPERTIES_INDEX_VALUE);
                if (VCalConstants.UID.equals(name)) {
                    mEventUID = value;
                    break;
                }
            } while(cursor.moveToNext());
        }

        if (!mIsDialog && !mIsTabletConfig) {
            if (mMenu != null) {
                MenuItem cancel = mMenu.findItem(R.id.info_action_cancel);
                MenuItem save = mMenu.findItem(R.id.info_action_save);
                if (cancel != null) {
                    cancel.setVisible(true);
                    cancel.setEnabled(true);
                }
                if (save != null) {
                    /*
                    // Always hide "Save" menu item now
                    save.setVisible(false);
                    */
                    if (TextUtils.isEmpty(mEventUID)) {
                        // Hide "Save" function
                        save.setVisible(false);
                    } else {
                        save.setVisible(true);
                        save.setEnabled(true);
                    }
                }
            }
        } else {
            Button b = (Button) mView.findViewById(R.id.cancel);
            if (b != null) {
                b.setVisibility(View.VISIBLE);
                b.setEnabled(true);
                b.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        destroyICalView(false/*exist calendar app*/, true/*delete iCal event*/);
                    }
                });
            }
            Button b1 = (Button) mView.findViewById(R.id.save);
            if (b1 != null) {
                /*
                // Always hide "Save" button now
                b.setVisibility(View.GONE);
                */
                if (TextUtils.isEmpty(mEventUID)) {
                    b1.setVisibility(View.GONE);
                } else {
                    b1.setVisibility(View.VISIBLE);
                    b1.setEnabled(true);
                    b1.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            saveICalEventToLocal();
                        }
                    });
                }
            }
        }
    }

    /**
     * Utility class used to store basic calendar account info.
     */
    static private class CalendarAccountInfo {
        // Account _id
        public final long mId;
        // Account name
        public final String mName;
        // Account type
        public final String mType;

        public CalendarAccountInfo(long id, String name, String type) {
            mId = id;
            mName = name;
            mType = type;
        }
    }

    /**
     * Get the local calendar account info.
     *
     * @return The found local account info or null if there's no local account.
     */
    private CalendarAccountInfo getLocalAccountInfo() {
        ContentResolver resolver = mContext.getContentResolver();
        String selection = Calendars.ACCOUNT_TYPE + "=?";
        String[] selectionArgs = new String[] {CalendarContract.ACCOUNT_TYPE_LOCAL};
        CalendarAccountInfo accountInfo = null;

        Cursor c = resolver.query(Calendars.CONTENT_URI,
                new String[]{Calendars._ID, Calendars.ACCOUNT_NAME, Calendars.ACCOUNT_TYPE},
                    selection, selectionArgs, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    accountInfo = new CalendarAccountInfo(c.getLong(0), c.getString(1),
                            c.getString(2));
                }
            } finally {
                c.close();
            }
        }

        return accountInfo;
    }

    /**
     * Get event ID by the given UID and calendar account ID.
     *
     * @param uid
     *        UID of the event to be queried
     *
     * @param calendarId
     *        Calendar account to be queried in
     *
     * @return The event _ID of the found record. or -1L if there's no such record.
     */
    private long getEventIdByUID(String uid, long calendarId) {
        ContentResolver resolver = mContext.getContentResolver();
        String selection = ExtendedProperties.NAME + "=? AND " +
            ExtendedProperties.VALUE + "=?";
        String selectionArgs[] = new String[] {VCalConstants.UID, uid};
        long eventId = -1L;

        Cursor c = resolver.query(ExtendedProperties.CONTENT_URI,
                new String[] {ExtendedProperties.EVENT_ID},
                selection, selectionArgs, null);
        if (c != null) {
            try {
                selection = Events._ID + "=? AND " +
                    Events.CALENDAR_ID + "=" + Long.toString(calendarId);
                while (c.moveToNext()) {
                    Cursor eventCursor = resolver.query(Events.CONTENT_URI,
                            new String[]{Events._ID}/* projection */,
                            selection /* selection */,
                            new String[]{c.getString(0)}/*selectionArgs*/,
                            null);
                    if (eventCursor != null) {
                        try {
                            if (eventCursor.moveToFirst()) {
                                eventId = eventCursor.getLong(0);
                            }
                        } finally {
                            eventCursor.close();
                        }
                    }
                    if (eventId != -1L) break;
                }
            } finally {
                c.close();
            }
        }

        return eventId;
    }

    private void addUpdateOperation(ArrayList<ContentProviderOperation> operations, Uri uri,
            ContentValues values, String selection, String[] selectionArgs) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(uri);
        if (!TextUtils.isEmpty(selection)) {
            builder.withSelection(selection, selectionArgs);
        }
        builder.withValues(values);
        operations.add(builder.build());
    }

    private void addDeleteOperation(ArrayList<ContentProviderOperation> operations, Uri uri,
            String selection, String[] selectionArgs) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newDelete(uri);
        if (!TextUtils.isEmpty(selection)) {
            builder.withSelection(selection, selectionArgs);
        }
        operations.add(builder.build());
    }

    // Adds an rRule and duration to a set of content values
    void addRecurrenceRule(ContentValues values) {
        if ((mEventCursor != null) && (mEventCursor.moveToFirst())) {
            String rrule = mEventCursor.getString(EVENT_INDEX_RRULE);
            boolean isAllday = mEventCursor.getInt(EVENT_INDEX_ALL_DAY) != 0;
            String duration = mEventCursor.getString(EVENT_INDEX_DURATION);

            values.put(Events.RRULE, rrule);
            long end = mEndMillis;
            long start = mStartMillis;

            if (end >= start) {
                if (isAllday) {
                    // if it's all day compute the duration in days
                    long days = (end - start + DateUtils.DAY_IN_MILLIS - 1)
                            / DateUtils.DAY_IN_MILLIS;
                    duration = "P" + days + "D";
                } else {
                    // otherwise compute the duration in seconds
                    long seconds = (end - start) / DateUtils.SECOND_IN_MILLIS;
                    duration = "P" + seconds + "S";
                }
            } else if (TextUtils.isEmpty(duration)) {

                // If no good duration info exists assume the default
                if (isAllday) {
                    duration = "P1D";
                } else {
                    duration = "P3600S";
                }
            }
            // recurring events should have a duration and dtend set to null
            values.put(Events.DURATION, duration);
            values.put(Events.DTEND, (Long) null);
        }
    }

    /**
     * Goes through the event cursor and fills in content values for saving. This
     * method will perform the initial collection of values from the cursor and
     * put them into a set of ContentValues. It performs some basic work such as
     * fixing the time on allDay events and choosing whether to use an rrule or
     * dtend.
     *
     * @return values
     */
    private ContentValues getContentValuesFromEvent() {
        if ((mEventCursor != null) && (mEventCursor.moveToFirst())) {
            String title = mEventCursor.getString(EVENT_INDEX_TITLE);
            int allDayFlag = mEventCursor.getInt(EVENT_INDEX_ALL_DAY);
            String location = mEventCursor.getString(EVENT_INDEX_EVENT_LOCATION);
            String description = mEventCursor.getString(EVENT_INDEX_DESCRIPTION);
            String rrule = mEventCursor.getString(EVENT_INDEX_RRULE);
            String timezone = mEventCursor.getString(EVENT_INDEX_EVENT_TIMEZONE);
            int hasAttendeeFlag = mEventCursor.getInt(EVENT_INDEX_HAS_ATTENDEE_DATA);
            int accessLevel = mEventCursor.getInt(EVENT_INDEX_ACCESS_LEVEL);
            long calendarId = mEventCursor.getLong(EVENT_INDEX_CALENDAR_ID);
            int available = mEventCursor.getInt(EVENT_INDEX_AVAILABILITY);

            if (timezone == null) {
                timezone = TimeZone.getDefault().getID();
            }
            Time startTime = new Time(timezone);
            Time endTime = new Time(timezone);

            startTime.set(mStartMillis);
            endTime.set(mEndMillis);

            ContentValues values = new ContentValues();

            long startMillis;
            long endMillis;
            if (allDayFlag != 0) {
                // Reset start and end time, ensure at least 1 day duration, and set
                // the timezone to UTC, as required for all-day events.
                timezone = Time.TIMEZONE_UTC;
                startTime.hour = 0;
                startTime.minute = 0;
                startTime.second = 0;
                startTime.timezone = timezone;
                startMillis = startTime.normalize(true);

                endTime.hour = 0;
                endTime.minute = 0;
                endTime.second = 0;
                endTime.timezone = timezone;
                endMillis = endTime.normalize(true);
                if (endMillis < startMillis + DateUtils.DAY_IN_MILLIS) {
                    endMillis = startMillis + DateUtils.DAY_IN_MILLIS;
                }
            } else {
                startMillis = startTime.toMillis(true);
                endMillis = endTime.toMillis(true);
            }

            values.put(Events.CALENDAR_ID, calendarId);
            values.put(Events.EVENT_TIMEZONE, timezone);
            values.put(Events.TITLE, title);
            values.put(Events.ALL_DAY, allDayFlag);
            values.put(Events.DTSTART, startMillis);
            values.put(Events.RRULE, rrule);
            if (!TextUtils.isEmpty(rrule)) {
                addRecurrenceRule(values);
            } else {
                values.put(Events.DURATION, (String) null);
                values.put(Events.DTEND, endMillis);
            }
            if (description != null) {
                values.put(Events.DESCRIPTION, description);
            } else {
                values.put(Events.DESCRIPTION, (String) null);
            }
            if (location != null) {
                values.put(Events.EVENT_LOCATION, location.trim());
            } else {
                values.put(Events.EVENT_LOCATION, (String) null);
            }
            values.put(Events.AVAILABILITY, available);
            values.put(Events.HAS_ATTENDEE_DATA, hasAttendeeFlag);
            values.put(Events.ACCESS_LEVEL, accessLevel);

            return values;
        }

        return null;
    }

    /**
     * Replace the given event by using the one which is being previewed.
     *
     * @param accountInfo
     *        The calendar account in which the event with _ID value
     *        eventId belongs to.
     *
     * @param eventId
     *        The _ID value of the event which is to be replaced.
     */
    private void replaceEventInDB(CalendarAccountInfo accountInfo, long eventId) {
        if (eventId == mEventId) {
            // Return direclty if they're the same event
            return;
        }

        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

        // Prepare the event ContentValues to update the existing event
        ContentValues values = getContentValuesFromEvent();
        if (values != null) {
            // Update the "calendar_id" field for the event
            values.put(Events.CALENDAR_ID, accountInfo.mId);

            // Update the "hasAlarm" field for the event
            if ((mRemindersCursor != null) && mRemindersCursor.moveToFirst()) {
                values.put(Events.HAS_ALARM, 1);
            } else {
                values.put(Events.HAS_ALARM, 0);
            }

            addUpdateOperation(operations,
                    ContentUris.withAppendedId(Events.CONTENT_URI, Long.valueOf(eventId)),
                    values, null, null);

            // Update Reminders on existing event. Don't touch Attendees and ExtendedProperties
            // since local claendar doesn't permit modifying them.
            // Delete existing Reminders first
            String selection = Reminders.EVENT_ID + " = " + Long.valueOf(eventId);
            addDeleteOperation(operations, Reminders.CONTENT_URI, selection, null);
            // Then add new Reminders
            ContentProviderOperation.Builder b;
            if ((mRemindersCursor != null) && mRemindersCursor.moveToFirst()) {
                do {
                    values.clear();
                    values.put(Reminders.MINUTES,
                            mRemindersCursor.getInt(EditEventHelper.REMINDERS_INDEX_MINUTES));
                    values.put(Reminders.METHOD,
                            mRemindersCursor.getInt(EditEventHelper.REMINDERS_INDEX_METHOD));
                    values.put(Reminders.EVENT_ID, eventId);
                    b = ContentProviderOperation.newInsert(Reminders.CONTENT_URI).withValues(values);
                    operations.add(b.build());
                } while(mRemindersCursor.moveToNext());
            }

            try {
                mHandler.startBatch(mHandler.getNextToken(), null, CalendarContract.AUTHORITY, operations, 0);
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
        }
    }

    /**
     * Move the event that's being previewed to the given calendar account.
     *
     * @param accountInfo
     *        The calendar account to which the event will be moved.
     */
    private void moveEventInDB(CalendarAccountInfo accountInfo) {
        if ((mEventCursor != null) && mEventCursor.moveToFirst()) {
            if (accountInfo.mId == mEventCursor.getInt(EVENT_INDEX_CALENDAR_ID)) {
                // Return directly if the event is already in the calendar account
                return;
            }
        } else {
            return;
        }

        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>(1);
        ContentValues values = new ContentValues(1);

        // Move the event to the new calendar account
        values.put(Events.CALENDAR_ID, Long.valueOf(accountInfo.mId));
        addUpdateOperation(operations,
                ContentUris.withAppendedId(Events.CONTENT_URI, Long.valueOf(mEventId)),
                values, null, null);

        try {
            mHandler.startBatch(mHandler.getNextToken(), null, CalendarContract.AUTHORITY, operations, 0);
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
    }

    /**
     * Save the event which is being previewed into the given calendar
     * account.  If there's already an event with the same uid in that
     * account, that event will be replaced.
     *
     * @param accountInfo
     *        The calendar account into which the event will be saved.
     */
    private void saveEventToDB(final CalendarAccountInfo accountInfo) {
        final long eventId = getEventIdByUID(mEventUID, accountInfo.mId);
        if (eventId == -1L) {
            moveEventInDB(accountInfo);
            destroyICalView(true/*return to calendar home*/, false/*keep iCal event*/);
        } else {
            // Popup dialog to ask whether to overwrite current event.
            DialogInterface.OnClickListener okButtonListener =
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int button) {
                        replaceEventInDB(accountInfo, eventId);
                        destroyICalView(true/*return to calendar home*/, true/*delete iCal event*/);
                    }
                };

            if (mReplaceEventDialog != null) {
                mReplaceEventDialog.dismiss();
                mReplaceEventDialog = null;
            }

            mReplaceEventDialog = new AlertDialog.Builder(mContext)
                .setTitle(R.string.replace_event_title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setMessage(R.string.replace_event_message)
                .setPositiveButton(android.R.string.ok, okButtonListener)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        }
    }

    /**
     * Save the iCal file which is being previewed into local account.
     * If there's no local account, the dialog will be dismissed directly.
     */
    private void saveICalEventToLocal() {
        CalendarAccountInfo accountInfo = getLocalAccountInfo();
        if (accountInfo != null) {
            saveEventToDB(accountInfo);
        } else {
            destroyICalView(false/*exit calendar app*/, true/*delete iCal event*/);
        }
    }

    /**
     * This method will destory the iCal preview window.
     *
     * @param returnToHome
     *        Indicates whether we need return to calendar home
     */
    private void destroyICalView(boolean returnToCalHome, boolean deleteIcs) {
        if (returnToCalHome) {
            long start = mStartMillis;
            long end = mEndMillis;
            boolean isAllDay = false;
            if ((mEventCursor != null) && mEventCursor.moveToFirst()) {
                isAllDay = mEventCursor.getInt(EVENT_INDEX_ALL_DAY) != 0;
            }
            if (isAllDay) {
                // For allday events we want to go to the day in the
                // user's current tz
                String tz = Utils.getTimeZone(mContext, null);
                Time t = new Time(Time.TIMEZONE_UTC);
                t.set(start);
                t.timezone = tz;
                start = t.toMillis(true);

                t.timezone = Time.TIMEZONE_UTC;
                t.set(end);
                t.timezone = tz;
                end = t.toMillis(true);
            }
            CalendarController.getInstance(mContext).launchViewEvent(-1, start, end);
        }

        if (mIsDialog && returnToCalHome) {
            EventInfoFragment.this.dismiss();
        } else {
            mActivity.finish();
        }

        if (deleteIcs) {
            mHandler.startDelete(mHandler.getNextToken(), null, mUri, null, null, 0);
        }
    }

    // BEGIN CONFERENCE CALL INFO //

    private void handleContactPhonePickResult(Intent intent) {
        Uri uri = intent.getData();
        if (DEBUG) Log.d(TAG, "URI received is : " + uri);
        //Begin Motorola - IKHSS6-8628 ANR caused by main query
        if (uri != null && mHandler != null) {
            mHandler.startQuery(TOKEN_QUERY_CONTACTINFO, null, uri, PhoneProjection.PHONES_PROJECTION, null, null, null);
        }
        //End Motorola - IKHSS6-8628
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_ID_FOR_CONTACT_PHONE_PICK) || (requestCode == REQUEST_ID_FOR_CONTACT_PHONE_PICK_BY_EMAIL)) {
            if((resultCode == Activity.RESULT_OK) && (data != null)) {
                handleContactPhonePickResult(data);
                if (DEBUG) Log.i(TAG, "onActivityResult() - Time Chosen: ");
            }
        } else if (requestCode == REQUEST_ID_FOR_ADDING_EVENT_TO_TASKS) {
            Utils.handleAddEventToTasksResult(this, resultCode, data, mTitle.getText().toString());
        }
    }

    private void handleOnClickDialogItem(int dlgId) {
        String myNumber = mConfDataMgr.getMyConfNumber();
        String myId = mConfDataMgr.getMyConfId();
        String currentNumber = mConfDataMgr.getCurrentConfNumber();
        String currentId = mConfDataMgr.getCurrentConfId();
        String meetingNumber =  mConfDataMgr.getMeetingConfNumber();
        String meetingId = mConfDataMgr.getMeetingConfId();
        Intent intent;
        ClipboardManager clip = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        switch (dlgId) {
            case DIALOG_ITEM_CALL_NUMBER:
                if ((currentNumber != null) && (currentNumber.length() > 0)) {
                    String hexCurrentId = null;
                    if ((currentId != null) && (currentId.length() > 0)) {
                        hexCurrentId = currentId.replaceAll("#", "%23");
                    }
                    intent = new Intent(Intent.ACTION_CALL,
                           Uri.parse("tel:" + getConcatenatedConfNumberAndId(currentNumber, hexCurrentId)));
                } else {
                    intent = new Intent(Intent.ACTION_DIAL);
                }
                startActivity(intent);
                break;

            case DIALOG_ITEM_CHANGE_NUMBER:
                showConfDialog(DIALOG_ID_CHANGE_CONFERENCE_CALL_INFO);
                break;

            case DIALOG_ITEM_COPY_NUMBER:
                showConfDialog(DIALOG_ID_COPY_CONFERENCE_CALL_INFO);
                break;

            case DIALOG_ITEM_COPY_ENTIRE_NUMBER:
                if ((clip != null) && (currentNumber != null) && (currentNumber.length() > 0)) {
                    clip.setText(getConcatenatedConfNumberAndId(currentNumber, currentId));
                }
                break;

            case DIALOG_ITEM_COPY_CONFERENCE_NUMBER:
                if ((clip != null) && (currentNumber != null) && (currentNumber.length() > 0)) {
                    clip.setText(currentNumber);
                }
                break;

            case DIALOG_ITEM_COPY_CONFERENCE_ID:
                if ((clip != null) && (currentId != null) && (currentId.length() > 0)) {
                    clip.setText(currentId);
                }
                break;

            case DIALOG_ITEM_CLEAR_CONFERENCE_CALL_INFO:
                mConfInfoContent.clear();
                mConfInfoContent.put(ConferenceCallInfo.ColumnNames.ORGANIZER_EMAIL, mOrganizerEmail);
                mConfInfoContent.put(ConferenceCallInfo.ColumnNames.DATA_SOURCE_FLAG, ConferenceCallInfo.DATA_SOURCE_FLAG_INVALID);
                showConfDialog(DIALOG_ID_CLEAR_CONFERENCE_INFO);
                break;

            case DIALOG_ITEM_USE_MEETING_VALUE:
                if ((meetingNumber != null) && (meetingNumber.length() > 0)) {
                    // will set it to DATA_SOURCE_FLAG_MANUAL_INPUT so all other events organized by the same person can refer to it
                    mConfInfoContent.clear();
                    mConfInfoContent.put(ConferenceCallInfo.ColumnNames.ORGANIZER_EMAIL, mOrganizerEmail);
                    mConfInfoContent.put(ConferenceCallInfo.ColumnNames.DATA_SOURCE_FLAG, ConferenceCallInfo.DATA_SOURCE_FLAG_MANUAL_INPUT);
                    mConfInfoContent.put(ConferenceCallInfo.ColumnNames.MANUAL_CONFERENCE_NUMBER, meetingNumber);
                    mConfInfoContent.put(ConferenceCallInfo.ColumnNames.MANUAL_CONFERENCE_ID, meetingId);
                    showConfDialog(DIALOG_ID_SET_INFO);
                }
                break;

            case DIALOG_ITEM_MY_CONFERENCE_CALL_INFO:
                mConfInfoContent.clear();
                mConfInfoContent.put(ConferenceCallInfo.ColumnNames.ORGANIZER_EMAIL, mOrganizerEmail);
                mConfInfoContent.put(ConferenceCallInfo.ColumnNames.DATA_SOURCE_FLAG, ConferenceCallInfo.DATA_SOURCE_FLAG_MY_PREFERENCE);

                if ((myNumber != null) && (myNumber.length() > 0)) {
                    showConfDialog(DIALOG_ID_SET_INFO);
                } else {
                    showConfDialog(DIALOG_ID_SET_INFO_WITH_NO_VALUE);
                }
                break;

            case DIALOG_ITEM_USE_ORGANIZER_INFO:
                intent = new Intent(getActivity(), ConferenceCallOrganizerInfoActivity.class);
                intent.putExtra(ConferenceCallOrganizerInfoActivity.EXTRA_ORGANIZER_EMAIL, mOrganizerEmail);
                startActivityForResult(intent, REQUEST_ID_FOR_CONTACT_PHONE_PICK_BY_EMAIL);
                break;

            case DIALOG_ITEM_SELECT_FROM_CONTACTS:
                intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(intent, REQUEST_ID_FOR_CONTACT_PHONE_PICK);
                break;

            case DIALOG_ITEM_ENTER_CONFERENCE_CALL_INFO:
                final AlertDialog dlg;
                if ((currentNumber != null) && (currentNumber.length() > 0)) {
                    // preload with existing conference call info
                    showConfDialog(DIALOG_ID_SET_INFO_BY_HAND_WITH_CURRENT);
                } else {
                    // preload with my conference call info
                    showConfDialog(DIALOG_ID_SET_INFO_BY_HAND_WITH_MINE);
                }
                break;

            default:
                break;
        }
    }



    private static class ConfCtxDlgViewHolder {
        final TextView title;
        final TextView summary;

        ConfCtxDlgViewHolder(View view) {
            title = (TextView) view.findViewById(R.id.confcall_ctx_dlg_item_title);
            summary = (TextView) view.findViewById(R.id.confcall_ctx_dlg_item_summary);
        }
    }

    private class ConfCallInfoDialogItem {
        int mDlgItemId;
        int mTitleResId;

        ConfCallInfoDialogItem(int dlgItemId, int titleResId) {
            mDlgItemId = dlgItemId;
            mTitleResId = titleResId;
        }

        public int getDlgItemId() {
            return mDlgItemId;
        }

        public boolean isEnabled(ConferenceCallInfoDataMgr confDataMgr) {
            boolean enabled = true;

            if (mDlgItemId == EventInfoFragment.DIALOG_ITEM_USE_MEETING_VALUE) {
                String meetingNumber = confDataMgr.getMeetingConfNumber();
                if ((meetingNumber == null) || (meetingNumber.length() == 0)) enabled = false;
            } else if (mDlgItemId == EventInfoFragment.DIALOG_ITEM_USE_ORGANIZER_INFO) {
                // always hide this menu item for local account
                if (CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType)) enabled = false;
            } else if ((mDlgItemId == EventInfoFragment.DIALOG_ITEM_COPY_NUMBER) ||
                       (mDlgItemId == EventInfoFragment.DIALOG_ITEM_COPY_ENTIRE_NUMBER)) {
                String currentNumber = confDataMgr.getCurrentConfNumber();
                if ((currentNumber == null) || (currentNumber.length() == 0)) enabled = false;
            } else if (mDlgItemId == EventInfoFragment.DIALOG_ITEM_COPY_CONFERENCE_NUMBER) {
                String currentNumber = confDataMgr.getCurrentConfNumber();
                String currentId = confDataMgr.getCurrentConfId();
                // only when both conference number and id are not empty, we'll show this menu item.
                // when the number is not empty and id is empty, we'll only show "Copy entire number".
                if ((currentNumber == null) || (currentNumber.length() == 0)
                    || (currentId == null) || (currentId.length() == 0)) {
                    enabled = false;
                }
            } else if (mDlgItemId == EventInfoFragment.DIALOG_ITEM_COPY_CONFERENCE_ID) {
                String currentId = confDataMgr.getCurrentConfId();
                if ((currentId == null) || (currentId.length() == 0)) enabled = false;
            }

            return enabled;
        }

        public View createView(Context ctx, View convertView, ConferenceCallInfoDataMgr confDataMgr) {
            if ((ctx == null) || (confDataMgr == null))
                return null;

            View view = convertView;
            if (view == null) {
                LayoutInflater factory = LayoutInflater.from(ctx);
                view = factory.inflate(R.layout.conference_call_context_dialog_layout, null);
            }

            ConfCtxDlgViewHolder holder = (ConfCtxDlgViewHolder)view.getTag();
            if (holder == null) {
                holder = new ConfCtxDlgViewHolder(view);
                view.setTag(holder);
            }
            holder.title.setText(mTitleResId);
            holder.summary.setVisibility(View.GONE);

            String currentNumber = confDataMgr.getCurrentConfNumber();
            String currentId = confDataMgr.getCurrentConfId();

            if (mDlgItemId == EventInfoFragment.DIALOG_ITEM_MY_CONFERENCE_CALL_INFO) {
                String myNumber = confDataMgr.getMyConfNumber();
                String myId = confDataMgr.getMyConfId();
                if ((myNumber != null) && (myNumber.length() > 0)) {
                    holder.summary.setText(getConcatenatedConfNumberAndId(myNumber, myId));
                    holder.summary.setVisibility(View.VISIBLE);
                }
            } else if (mDlgItemId == EventInfoFragment.DIALOG_ITEM_USE_MEETING_VALUE) {
                String meetingNumber = confDataMgr.getMeetingConfNumber();
                String meetingId = confDataMgr.getMeetingConfId();

                if ((meetingNumber != null) && (meetingNumber.length() > 0)) {
                    holder.summary.setText(getConcatenatedConfNumberAndId(meetingNumber, meetingId));
                    holder.summary.setVisibility(View.VISIBLE);
                }
            } else if ((mDlgItemId == EventInfoFragment.DIALOG_ITEM_CALL_NUMBER) ||
                       (mDlgItemId == EventInfoFragment.DIALOG_ITEM_COPY_ENTIRE_NUMBER)){
                if ((currentNumber != null) && (currentNumber.length() > 0)) {
                    holder.summary.setText(getConcatenatedConfNumberAndId(currentNumber, currentId));
                    holder.summary.setVisibility(View.VISIBLE);
                }
            } else if (mDlgItemId == EventInfoFragment.DIALOG_ITEM_COPY_CONFERENCE_NUMBER) {
                if ((currentNumber != null) && (currentNumber.length() > 0)) {
                    holder.summary.setText(currentNumber);
                    holder.summary.setVisibility(View.VISIBLE);
                }
            } else if (mDlgItemId == EventInfoFragment.DIALOG_ITEM_COPY_CONFERENCE_ID) {
                if ((currentId != null) && (currentId.length() > 0)) {
                    holder.summary.setText(currentId);
                    holder.summary.setVisibility(View.VISIBLE);
                }
            }

            return view;
        }
    }

    private static class ConfCallInfoDialogAdapter extends BaseAdapter {
        private final Context mCtx;
        private final ConferenceCallInfoDataMgr mConfDataMgr;
        private final ArrayList<ConfCallInfoDialogItem> mItems;

        ConfCallInfoDialogAdapter(Context ctx, ConferenceCallInfoDataMgr confDataMgr, ArrayList<ConfCallInfoDialogItem> items) {
            mCtx = ctx;
            mConfDataMgr = confDataMgr;
            mItems =items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getItem(position).createView(mCtx, convertView, mConfDataMgr);
        }

        public ConfCallInfoDialogItem getItem(int position) {
            if ((mItems == null) || (mItems.isEmpty())) {
                throw new IllegalArgumentException("Illegal conference dialog items");
            }

            int filteredPos = 0;
            for (int i = 0; i < mItems.size(); ++i) {
                if (!mItems.get(i).isEnabled(mConfDataMgr))
                    continue;

                if (filteredPos == position) {
                    return mItems.get(i);
                }

                ++filteredPos;
            }

            throw new IllegalArgumentException("Illegal conference dialog item position");
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getCount() {
            if ((mItems == null) || (mItems.isEmpty()))
                return 0;

            int count = 0;
            for (int i = 0; i < mItems.size(); ++i)
                if (mItems.get(i).isEnabled(mConfDataMgr))
                    ++count;

            return count;
        }
    }

    private Dialog createConfCallInfoCtxDialog(int id) {
        int dlgTitleId;
        final ArrayList<ConfCallInfoDialogItem> items;

        switch (id) {
            case DIALOG_ID_CONTEXT_DIALOG_FOR_EXISTING_CONF_INFO:
                items = mExistingConfInfoArrayList;
                dlgTitleId = R.string.select_title;
                break;

            case DIALOG_ID_CONTEXT_DIALOG_FOR_EMPTY_CONF_INFO:
                items = mEmptyConfInfoArrayList;
                dlgTitleId = R.string.select_title;
                break;

            case DIALOG_ID_CHANGE_CONFERENCE_CALL_INFO:
                items = mChangeConfInfoArrayList;
                dlgTitleId = R.string.select_conference_call_info_title;
                break;

            case DIALOG_ID_COPY_CONFERENCE_CALL_INFO:
                items = mCopyConfInfoArrayList;
                dlgTitleId = R.string.select_title;
                break;

            default:
                return null;
        }
        //ContextThemeWrapper wrapper = new ContextThemeWrapper(getActivity(),R.style.ConfDialogTheme);
        final ConfCallInfoDialogAdapter adapter = new ConfCallInfoDialogAdapter(getActivity(), mConfDataMgr, items);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(dlgTitleId);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick (DialogInterface dialog, int which) {
                handleOnClickDialogItem(adapter.getItem(which).getDlgItemId());
                dialog.dismiss();
            }
        });
        return builder.create();
    }

    private Dialog createSetConfCallInfoDialog() {
        String message = null;
        // refer to CalendarAttributes.isLocal() in EditEvent.java for how to check a local account
        if (CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType)) {
            message = getString(R.string.set_local_conference_call_info_message);
        } else {
            message = getString(R.string.set_conference_call_info_message);
        }

        // refer to CalendarAttributes.isLocal() in EditEvent.java for how to check a local account
        if (!CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType)) {
            if (mConfOrganizer != null) {
                message = String.format(message, mConfOrganizer);
            } else {
                message = String.format(message, "");
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.set_conference_call_info_titile);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setMessage(message);

        builder.setPositiveButton(R.string.all_meetings_label, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mConfInfoContent.put(ConferenceCallInfo.ColumnNames.ASSOCIATED_EVENT_ID, -1);
                onClickConfCallConfirmDialogButton(DIALOG_ID_SET_INFO);
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(R.string.this_one_only_label, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mConfInfoContent.put(ConferenceCallInfo.ColumnNames.ASSOCIATED_EVENT_ID, mEventId);
                onClickConfCallConfirmDialogButton(DIALOG_ID_SET_INFO);
                dialog.dismiss();
            }
        });

        return builder.create();
    }

    //Beigin Motorola - Rotate issue
    private Dialog createSetConfCallInfoDialogWithNoValue() {
        String myNumber = mConfDataMgr.getMyConfNumber();
        String myId = mConfDataMgr.getMyConfId();
        final AlertDialog dlg = ConferenceCallPreferenceDialogBuilder.createDialog(
                getActivity(), myNumber, myId);
        if (dlg != null) {
            // overwrite the positive button
            dlg.setButton(DialogInterface.BUTTON_POSITIVE,
                    getString(R.string.ok_label),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            EditText numberWidget = (EditText) dlg
                                    .findViewById(R.id.telephone_number);
                            EditText idWidget = (EditText) dlg
                                    .findViewById(R.id.conference_id);

                            // if it's an valid conference info, we'll prompt
                            // the user for futher setting actions
                            if (ConferenceCallPreferenceDialogBuilder
                                    .checkConferenceInfo(getActivity(),
                                            numberWidget, idWidget)) {
                                ConferenceCallPreferenceDialogBuilder
                                        .saveConferenceInfo(getActivity(),
                                                numberWidget, idWidget);
                                // mEditResponseHelper.showDialog(DIALOG_ID_SET_INFO);
                                showConfDialog(DIALOG_ID_SET_INFO);
                            } else {
                                Toast toast = Toast.makeText(getActivity(),
                                        R.string.empty_telephone_number,
                                        Toast.LENGTH_SHORT);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                            }

                            dialog.dismiss();
                        }
                    });

        }
        return dlg;
    }

    private Dialog createSetConfCallInfoDialogByHand(String number, String id) {
        final AlertDialog dlg = ConferenceCallPreferenceDialogBuilder
                .createDialog(getActivity(), number, id);
        if (dlg != null) {
            // overwrite the positive button
            dlg.setButton(DialogInterface.BUTTON_POSITIVE,
                    getString(R.string.ok_label),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String confNumber = null;
                            String confId = null;

                            EditText numberWidget = (EditText) dlg
                                    .findViewById(R.id.telephone_number);
                            if (numberWidget != null) {
                                confNumber = numberWidget.getText().toString();
                            }

                            EditText idWidget = (EditText) dlg
                                    .findViewById(R.id.conference_id);
                            if (idWidget != null) {
                                confId = idWidget.getText().toString();
                            }

                            if (ConferenceCallPreferenceDialogBuilder
                                    .checkConferenceInfo(getActivity(),
                                            confNumber, confId)) {
                                mConfInfoContent.clear();
                                mConfInfoContent
                                        .put(ConferenceCallInfo.ColumnNames.ORGANIZER_EMAIL,
                                                mOrganizerEmail);
                                mConfInfoContent
                                        .put(ConferenceCallInfo.ColumnNames.DATA_SOURCE_FLAG,
                                                ConferenceCallInfo.DATA_SOURCE_FLAG_MANUAL_INPUT);
                                mConfInfoContent
                                        .put(ConferenceCallInfo.ColumnNames.MANUAL_CONFERENCE_NUMBER,
                                                confNumber);
                                mConfInfoContent
                                        .put(ConferenceCallInfo.ColumnNames.MANUAL_CONFERENCE_ID,
                                                confId);
                                // mEditResponseHelper.showDialog(DIALOG_ID_SET_INFO);
                                showConfDialog(DIALOG_ID_SET_INFO);
                            } else {
                                Toast toast = Toast.makeText(getActivity(),
                                        R.string.empty_telephone_number,
                                        Toast.LENGTH_SHORT);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                            }
                            dialog.dismiss();
                        }
                    });
        }
        return dlg;
    }
  //End Motorola - Rotate issue

    private Dialog createDeleteConfCallInfoDialog() {
        String message = null;
        // refer to CalendarAttributes.isLocal() in EditEvent.java for how to check a local account
        if (CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType)) {
            message = getString(R.string.clear_local_conference_call_info_message);
        } else {
            message = getString(R.string.clear_conference_call_info_message);
        }
       // refer to CalendarAttributes.isLocal() in EditEvent.java for how to check a local account
        if (!CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType)) {
            if (mConfOrganizer != null) {
                message = String.format(message, mConfOrganizer);
            } else {
                message = String.format(message, "");
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.clear_conference_call_info);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setMessage(message);

        // if there's a particular conference call setting associated with this event, we'll only clear
        // this specific one.
        if (ConferenceCallInfoSettingMgr.hasConferenceCallSetting(getActivity(), mOrganizerEmail, mEventId)) {
            // reset the dialog content message
            message = getString(R.string.clear_one_particular_conference_call_info_message);
            builder.setMessage(message);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mConfInfoContent.put(ConferenceCallInfo.ColumnNames.ASSOCIATED_EVENT_ID, mEventId);
                    onClickConfCallConfirmDialogButton(DIALOG_ID_CLEAR_CONFERENCE_INFO);
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                }
            });
        } else {
            // let the user decide to clear conference call for all meetings or this one only
            builder.setPositiveButton(R.string.all_meetings_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mConfInfoContent.put(ConferenceCallInfo.ColumnNames.ASSOCIATED_EVENT_ID, -1);
                    onClickConfCallConfirmDialogButton(DIALOG_ID_CLEAR_CONFERENCE_INFO);
                    dialog.dismiss();
                }
            });

            builder.setNeutralButton(R.string.this_one_only_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mConfInfoContent.put(ConferenceCallInfo.ColumnNames.ASSOCIATED_EVENT_ID, mEventId);
                    onClickConfCallConfirmDialogButton(DIALOG_ID_CLEAR_CONFERENCE_INFO);
                    dialog.dismiss();
                }
            });

            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                }
            });
        }

        return builder.create();
    }

    private void onClickConfCallConfirmDialogButton(int dialogId) {
        if ((mConfOrganizer != null) && (mConfOrganizer.length() > 0)) {
            mConfSettingMgr.updateConferenceCallSetting(getActivity(), mConfInfoContent);
//            updateDynamicInfoOfConfCallButton();
            if (dialogId == DIALOG_ID_SET_INFO && mManagedDialogs != null) {
                // remove resource to make sure rebuilding dynamic menu
                mManagedDialogs.remove(DIALOG_ID_CONTEXT_DIALOG_FOR_EMPTY_CONF_INFO);
                mManagedDialogs.remove(DIALOG_ID_CONTEXT_DIALOG_FOR_EMPTY_CONF_INFO);
                mManagedDialogs.remove(DIALOG_ID_CONTEXT_DIALOG_FOR_EXISTING_CONF_INFO);
                mManagedDialogs.remove(DIALOG_ID_CHANGE_CONFERENCE_CALL_INFO);
                mManagedDialogs.remove(DIALOG_ID_COPY_CONFERENCE_CALL_INFO);
                mManagedDialogs.remove(DIALOG_ID_CLEAR_CONFERENCE_INFO);
            }
        } else {
            Toast toast = Toast.makeText(getActivity(), R.string.null_meeting_organizer, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    protected Dialog createConfDialog(int id) {
        String myNumber = mConfDataMgr.getMyConfNumber();
        String myId = mConfDataMgr.getMyConfId();
        String currentNumber = mConfDataMgr.getCurrentConfNumber();
        String currentId = mConfDataMgr.getCurrentConfId();
        switch (id) {
        case DIALOG_ID_CONTEXT_DIALOG_FOR_EXISTING_CONF_INFO:
        case DIALOG_ID_CONTEXT_DIALOG_FOR_EMPTY_CONF_INFO:
        case DIALOG_ID_CHANGE_CONFERENCE_CALL_INFO:
        case DIALOG_ID_COPY_CONFERENCE_CALL_INFO:
            return createConfCallInfoCtxDialog(id);
        case DIALOG_ID_SET_INFO:
            return createSetConfCallInfoDialog();
        case DIALOG_ID_CLEAR_CONFERENCE_INFO:
            return createDeleteConfCallInfoDialog();
        case DIALOG_ID_SET_INFO_WITH_NO_VALUE:
             return createSetConfCallInfoDialogWithNoValue();
        case DIALOG_ID_SET_INFO_BY_HAND_WITH_CURRENT:
             return createSetConfCallInfoDialogByHand(currentNumber, currentId);
        case DIALOG_ID_SET_INFO_BY_HAND_WITH_MINE:
            return createSetConfCallInfoDialogByHand(myNumber, myId);
        default:
            break;
        }
        return null;
    }

    private boolean showConfDialog(int id) {
        if (mManagedDialogs == null) {
            mManagedDialogs = new SparseArray<ManagedDialog>();
        }
        ManagedDialog md = mManagedDialogs.get(id);
        if (md == null) {
            md = new ManagedDialog();
            md.mDialog = createConfDialog(id);
            if (md.mDialog == null) {
                return false;
            }
            mManagedDialogs.put(id, md);
        }

        md.mArgs = null;
        md.mDialog.show();
        return true;
    }

    /**
     * Save the state of any managed dialogs.
     *
     * @param outState place to store the saved state.
     */
    private void saveManagedDialogs(Bundle outState) {
        if (mManagedDialogs == null) {
            return;
        }

        final int numDialogs = mManagedDialogs.size();
        if (numDialogs == 0) {
            return;
        }

        Bundle dialogState = new Bundle();

        int[] ids = new int[mManagedDialogs.size()];

        // save each dialog's bundle, gather the ids
        for (int i = 0; i < numDialogs; i++) {
            final int key = mManagedDialogs.keyAt(i);
            ids[i] = key;
            final ManagedDialog md = mManagedDialogs.valueAt(i);
            dialogState.putBundle(savedDialogKeyFor(key), md.mDialog.onSaveInstanceState());
            if (md.mArgs != null) {
                dialogState.putBundle(savedDialogArgsKeyFor(key), md.mArgs);
            }
        }

        dialogState.putIntArray(SAVED_DIALOG_IDS_KEY, ids);
        outState.putBundle(SAVED_DIALOGS_TAG, dialogState);
    }

    /**
     * Restore the state of any saved managed dialogs.
     *
     * @param savedInstanceState The bundle to restore from.
     */
    private void restoreManagedDialogs(Bundle savedInstanceState) {
        final Bundle b = savedInstanceState.getBundle(SAVED_DIALOGS_TAG);
        if (b == null) {
            return;
        }
        final int[] ids = b.getIntArray(SAVED_DIALOG_IDS_KEY);
        if (ids != null) {
            final int numDialogs = ids.length;
            mManagedDialogs = new SparseArray<ManagedDialog>(numDialogs);
            for (int i = 0; i < numDialogs; i++) {
                final Integer dialogId = ids[i];
                Bundle dialogState = b.getBundle(savedDialogKeyFor(dialogId));
                if (dialogState != null) {
                    // Calling onRestoreInstanceState() below will invoke dispatchOnCreate
                    // so tell createDialog() not to do it, otherwise we get an exception
                    final ManagedDialog md = new ManagedDialog();
                    md.mArgs = b.getBundle(savedDialogArgsKeyFor(dialogId));
                    md.mDialog = createConfDialog(dialogId);
                    if (md.mDialog != null) {
                        mManagedDialogs.put(dialogId, md);
                        md.mDialog.onRestoreInstanceState(dialogState);
                    }
                }
            }
        }
    }

    private static String savedDialogKeyFor(int key) {
        return SAVED_DIALOG_KEY_PREFIX + key;
    }
    private static String savedDialogArgsKeyFor(int key) {
        return SAVED_DIALOG_ARGS_KEY_PREFIX + key;
    }

    private String getConcatenatedConfNumberAndId(String number, String id) {
        StringBuilder strBuilder = new StringBuilder();

        if ((number != null) && (number.length() > 0)) {
            if ((id != null) && (id.length() > 0)) {
                strBuilder.append(number + ";" + id);
            } else {
                strBuilder.append(number);
            }
        }

        return strBuilder.toString();
    }

    /*private void updateDynamicInfoOfConfCallButton() {
        Button confInfoBtn = (Button) mView.findViewById(R.id.conference_call_info_button);
        ImageButton confDialBtn = (ImageButton) mView.findViewById(R.id.conference_call_dial_imagebutton);

        final String currentConfNumber = mConfDataMgr.getCurrentConfNumber();
        final String currentConfId = mConfDataMgr.getCurrentConfId();
        int dataSourceId = mConfSettingMgr.getConfCallInfoDataSource();
        final Activity activity = getActivity();
        // set button title and click listener
        if (DEBUG) {
            Log.d(TAG,"currentConfNumber:" + currentConfNumber);
        }
        if ((currentConfNumber != null) && (currentConfNumber.length() > 0)) {
            final StringBuilder strBuilder = new StringBuilder();

            if (dataSourceId == ConferenceCallInfo.DATA_SOURCE_FLAG_CONTACT_DATA) {
                String displayName = mConfDataMgr.getContactDisplayName();
                String phoneType = mConfDataMgr.getContactPhoneType();

                if ((displayName == null) || (displayName.length() == 0)) {
                    strBuilder.append(currentConfNumber);
                } else {
                    strBuilder.append(displayName + " (" + phoneType + ")" + "\r\n" + currentConfNumber);
                }
            } else if (dataSourceId == ConferenceCallInfo.DATA_SOURCE_FLAG_MY_PREFERENCE) {
                strBuilder.append(getString(R.string.my_conference_call_info) + "\r\n" +
                        getConcatenatedConfNumberAndId(currentConfNumber, currentConfId));
            } else {
                strBuilder.append(getConcatenatedConfNumberAndId(currentConfNumber, currentConfId));
            }

            // set Conference Info button
            confInfoBtn.setText(strBuilder.toString());
            confInfoBtn.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

            confInfoBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick (View v) {
                    showConfDialog(DIALOG_ID_CONTEXT_DIALOG_FOR_EXISTING_CONF_INFO);
                }
            });

            // set Dialer button
            confDialBtn.setVisibility(View.VISIBLE);
            confDialBtn.setOnClickListener( new View.OnClickListener() {
                public void onClick(View v) {
                    Intent dialIntent;
                    String hexCurrentId = null;
                    if ((currentConfId != null) && (currentConfId.length() > 0)) {
                        hexCurrentId = currentConfId.replaceAll("#", "%23");
                    }
                    dialIntent = new Intent(Intent.ACTION_CALL,
                        Uri.parse("tel:" + getConcatenatedConfNumberAndId(currentConfNumber, hexCurrentId)));
                    startActivity(dialIntent);
                }
            });
        } else {
            // set Conference Info button
            confInfoBtn.setText(R.string.set_conference_bridge);
            confInfoBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick (View v) {
                        showConfDialog(DIALOG_ID_CONTEXT_DIALOG_FOR_EMPTY_CONF_INFO);
                    }
            });

            // set Dialer button
            confDialBtn.setVisibility(View.GONE);
        }
    }*/

//    private boolean setConferenceCallInfoVisible() {
//        // set the visibility of the layout conference_call_info_container
//        LinearLayout confCallContainer = (LinearLayout) mView.findViewById(R.id.conference_call_container);
//        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(getActivity());
//        boolean showConfInfo = prefs.getBoolean(
//                GeneralPreferences.KEY_ENABLE_QUICK_CONFERENCE_DIALING, true/*default true*/);
//        if (!showConfInfo || mIsIcsImport) {
//            confCallContainer.setVisibility(View.GONE);
//            return false;
//        }
//        confCallContainer.setVisibility(View.VISIBLE);
//        //set the attributes conference_call_info_button and conference_call_dial_button
//        Button confButton = (Button) mView.findViewById(R.id.conference_call_info_button);
//        confButton.setFadingEdgeLength(0);
//        return true;
//    }

    private class updateConfCallInfoInBackground extends AsyncTask<Void, Void, Void> {
        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Void doInBackground(Void... params)  {
            mConfSettingMgr.caceheConferenceCallSetting(mEventCursor, mConfInfoCursor);
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            //updateDynamicInfoOfConfCallButton();
        }
    }

    private class ComposeIcsTask extends AsyncTask<Long, Void, Integer> {
        static private final String TAG = "ComposeIcsTask";
        Context mContext;
        Uri mIcsUri;

        public ComposeIcsTask(Context context) {
            mContext = context;
        }

        @Override
        protected Integer doInBackground(Long... params) {
            long eventId = params[0];
            VCalUtils vcalUtils = new VCalUtils();
            int result = vcalUtils.exportEventToSDCard(mContext, eventId, null,  "invite_" + eventId + ".vcs");
            if (result < 0) {
                Log.v(TAG, "Failed to save ics file, eventId: " + eventId + ", result: " + result);
                return result;
            }

            mIcsUri = vcalUtils.getSavedFileUri();
            if (mIcsUri != null) {
                Log.v(TAG, "The saved ics file path : " + mIcsUri.toString());
            }

            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            int error = result;
            if (error < 0) {
                switch (error) {
                case VCalUtils.ERROR_CODE_NO_SDCARD:
                    showToast(mContext, R.string.sd_card_not_found);
                    break;
                case VCalUtils.ERROR_CODE_SDCARD_ACCESS_FAILURE:
                    showToast(mContext, R.string.corrupted_sd_card);
                    break;
                case VCalUtils.ERROR_CODE_UNKNOWN:
                default:
                    showToast(mContext, R.string.unknown_error);
                    break;
                }
            } else {
                Utils.printIcs(mContext, mIcsUri);
            }
        }

        private void showToast(Context context, int resid) {
            Toast.makeText(context, context.getText(resid), Toast.LENGTH_SHORT).show();
        }
    }

    private class ShareIcsTask extends ComposeIcsTask {
        private static final String TAG = "ShareIcsTask";

        public ShareIcsTask(Context context) {
            super(context);
        }

        @Override
        protected Integer doInBackground(Long... params) {
            return super.doInBackground(params);
        }

        @Override
        protected void onPostExecute(Integer result) {
            String messageBody = prepareMessageBody();
            Utils.shareIcs(mContext, mIcsUri, mTitle.getText().toString(), messageBody);
        }
    }
    // End Motorola

    // BEGIN MOTOROLA - IKPIM-974: Support forward on event detail
    private class ForwardIcsTask extends ComposeIcsTask {
        private static final String TAG = "ForwardIcsTask";

        public ForwardIcsTask(Context context) {
            super(context);
        }

        @Override
        protected Integer doInBackground(Long... params) {
            return super.doInBackground(params);
        }

        @Override
        protected void onPostExecute(Integer result) {
            StringBuffer str = new StringBuffer();
            str.append(mContext.getString(R.string.forward_short));
            str.append(" ");
            str.append(mTitle.getText());

            String messageBody = prepareMessageBody();
            Utils.mailEventIcs(mContext, mIcsUri, str.toString(), messageBody, mSyncAccountType,
                    mCalendarId, null, null);
        }
    }
    // END MOTOROLA - IKPIM-974

    // BEGIN MOTOROLA - IKPIM-936: Support reply/reply all on event detail
    public String prepareMessageBody() {
        StringBuffer bodyText = new StringBuffer();
        StringBuffer organizerList = new StringBuffer();
        StringBuffer attendeeList = new StringBuffer();

        organizerList.append(mEventOrganizer);
        ArrayList<EventAttendee> attendeesList = new ArrayList<EventAttendee>();
        attendeesList.addAll(mRequiredAcceptedAttendees);
        attendeesList.addAll(mRequiredDeclinedAttendees);
        attendeesList.addAll(mRequiredTentativeAttendees);
        attendeesList.addAll(mRequiredNoResponseAttendees);
        attendeesList.addAll(mOptionalAcceptedAttendees);
        attendeesList.addAll(mOptionalDeclinedAttendees);
        attendeesList.addAll(mOptionalTentativeAttendees);
        attendeesList.addAll(mOptionalNoResponseAttendees);
        for (int i = 0; i < attendeesList.size(); i++) {
            EventAttendee attendee = attendeesList.get(i);
            if (!TextUtils.isEmpty(attendee.mEmail)) {
                if (!TextUtils.isEmpty(mEventOrganizer) &&
                        (attendee.mEmail.equalsIgnoreCase(mEventOrganizer))) continue;
                if (attendeeList.length() > 0)
                    attendeeList.append("; ");
                attendeeList.append(attendee.mEmail);
            }
        }
        if (!TextUtils.isEmpty(mCalendarOwnerAccount)) {
            if (attendeeList.length() > 0)
                attendeeList.append("; ");
            attendeeList.append(mCalendarOwnerAccount);
        }

        Resources res = mActivity.getResources();
        bodyText.append("\n\n\n");
        bodyText.append(res.getString(R.string.original_appoinment_label));
        bodyText.append("\n\n");
        bodyText.append(res.getString(R.string.subject_label));
        bodyText.append(" ");
        bodyText.append(mTitle.getText());
        bodyText.append("\n");
        bodyText.append(res.getString(R.string.sent_label));
        bodyText.append(" ");
        bodyText.append(mWhenDateTime.getText()/* + " " + mWhenTime.getText()*/);
        bodyText.append("\n");
        if (!TextUtils.isEmpty(organizerList) && !CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType)) {
            bodyText.append(res.getString(R.string.from_label));
            bodyText.append(" ");
            bodyText.append(organizerList);
            bodyText.append("\n");
        }
        if (!TextUtils.isEmpty(attendeeList) && !CalendarContract.ACCOUNT_TYPE_LOCAL.equals(mSyncAccountType)) {
            bodyText.append(res.getString(R.string.to_label));
            bodyText.append(" ");
            bodyText.append(attendeeList);
            bodyText.append("\n");
        }
        final CharSequence where = mWhere.getText();
        if (!TextUtils.isEmpty(where)) {
            bodyText.append(res.getString(R.string.mail_where_label));
            bodyText.append(" ");
            bodyText.append(where);
            bodyText.append("\n");
        }
        bodyText.append("\n");
        bodyText.append(mDesc.getText());
        return bodyText.toString();
    }

    /*
     * Show the compose screen with populated data from event
     * Show Gmail in case of google event, otherwise exchange compose screen
     */
    private void showReplyComposeScreen(int mode) {
        // BEGIN MOTOROLA - IKPIM-974: Support forward on event detail
        StringBuffer str = new StringBuffer();
        str.append(mActivity.getString(R.string.reply_short));
        str.append("  ");
        str.append(mTitle.getText());

        String[] orgEmail = new String[1];
        orgEmail[0] = mEventOrganizer;

        String[] ccList = null;
        if (mode == ACTION_REPLYALL) {
            ArrayList<String> addresses = new ArrayList<String>();
            ArrayList<EventAttendee> attendeesList = new ArrayList<EventAttendee>();
            attendeesList.addAll(mRequiredAcceptedAttendees);
            attendeesList.addAll(mRequiredDeclinedAttendees);
            attendeesList.addAll(mRequiredTentativeAttendees);
            attendeesList.addAll(mRequiredNoResponseAttendees);
            attendeesList.addAll(mOptionalAcceptedAttendees);
            attendeesList.addAll(mOptionalDeclinedAttendees);
            attendeesList.addAll(mOptionalTentativeAttendees);
            attendeesList.addAll(mOptionalNoResponseAttendees);
            for (int i = 0; i < attendeesList.size(); i++) {
                EventAttendee attendee = attendeesList.get(i);
                if (!TextUtils.isEmpty(attendee.mEmail)) {
                    if (!TextUtils.isEmpty(mEventOrganizer) &&
                            (attendee.mEmail.equalsIgnoreCase(mEventOrganizer))) continue;
                    addresses.add(attendee.mEmail);
                }
            }
            ccList = addresses.toArray(new String[addresses.size()]);
        }

        String messageBody = prepareMessageBody();
        Utils.mailEventIcs(mActivity, null, str.toString(), messageBody, mSyncAccountType,
                mCalendarId, orgEmail, ccList);
        // END MOTOROLA - IKPIM-974
    }
    // END MOTOROLA - IKPIM-936

    //MOTOROLA Begin, Emergent group
    /**
     * This called when quick action button is clicked. Set a listener that will be notified
     * when the user selects an item from the menu.
     * @param context
     * @param view
     */
    private void onQuickTaskClick(View view) {
        PopupMenu menu = new PopupMenu(mContext, view);
        menu.getMenuInflater().inflate(R.menu.quick_action_popup_menu, menu.getMenu());
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // TODO Auto-generated method stub
                // deleted by amt-sunli 2012-09-20 SWITCHUITWOV-209 begin
                /* if( item.getItemId() == R.id.action_save_group) {
                     saveGroup(getAttendeeEmails());
                 }
                 else*/
                // deleted by amt-sunli 2012-09-20 SWITCHUITWOV-209 end
                if(item.getItemId() == R.id.action_create_email) {
                     createCalendar(getAttendeeEmails());
                 }
                 else if(item.getItemId() == R.id.action_create_event) {
                    // modified by amt_sunli 2013-1-18 begin
                     // createEmail(getAttendeeEmails());
                    emailAttendees();
                    // modified by amt_sunli 2013-1-18 end
                 }
                return false;
            }
        });
        menu.show();
    }
    //Save all the attendees as a new group
    private void saveGroup(String attendeeEmails) {
        if(TextUtils.isEmpty(attendeeEmails))
            return;
        //Intent intent = new Intent(GroupAPI.GroupIntents.SavingGroup.ACTION_GROUPS_QUICKTASK_SAVE_GROUP);
        //intent.putExtra(GroupAPI.GroupIntents.SavingGroup.EXTRA_GROUP_QUICKTASK_GROUP_TITLE, mTitle.getText());
        //intent.putExtra(GroupAPI.GroupIntents.SavingGroup.EXTRA_GROUP_QUICKTASK_EMAIL_ADDRESSES, attendeeEmails);
        //startActivity(intent);
    }
    //Create new event
    private  void createCalendar(String attendeeEmails) {
        if(TextUtils.isEmpty(attendeeEmails))
            return;
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType("vnd.android.cursor.item/event");
        intent.putExtra(Events.TITLE, mTitle.getText());
        intent.putExtra(Intent.EXTRA_EMAIL, attendeeEmails);
        startActivity(intent);
    }
  //Create new email
    private void createEmail(String attendeeEmails){
        if(TextUtils.isEmpty(attendeeEmails))
            return;
        Intent intent = new Intent(Intent.ACTION_SENDTO)
        .setData(Uri.fromParts("mailto", attendeeEmails, null));
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, mTitle.getText().toString());
        startActivity(intent);
    }
    /*
     * Retrun all the attendees email of the Event.
     *
     */
    private String getAttendeeEmails(){
        //get attendee emails
        StringBuilder emailAddresses = new StringBuilder();
        ArrayList<EventAttendee> attendeesList = new ArrayList<EventAttendee>();
        attendeesList.addAll(mRequiredAcceptedAttendees);
        attendeesList.addAll(mRequiredDeclinedAttendees);
        attendeesList.addAll(mRequiredTentativeAttendees);
        attendeesList.addAll(mRequiredNoResponseAttendees);
        attendeesList.addAll(mOptionalAcceptedAttendees);
        attendeesList.addAll(mOptionalDeclinedAttendees);
        attendeesList.addAll(mOptionalTentativeAttendees);
        attendeesList.addAll(mOptionalNoResponseAttendees);
        if (!mIsOrganizer)
            attendeesList.add(mOrganizer);
        for (int i = 0; i < attendeesList.size(); i++) {
            EventAttendee attendee = attendeesList.get(i);
            if (!TextUtils.isEmpty(attendee.mEmail)) {
                emailAddresses.append(attendee.mEmail + ";");
            }
        }
        if (DEBUG) {
            Log.d(TAG, "emailAddresses: " + emailAddresses.toString());
        }
        return emailAddresses.toString();
    }
    //MOTOROLA End
    // End Motorola
}
