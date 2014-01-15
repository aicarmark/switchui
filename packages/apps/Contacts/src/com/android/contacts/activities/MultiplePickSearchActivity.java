package com.android.contacts.activities;

//import com.google.android.googlelogin.GoogleLoginServiceConstants;
import com.android.contacts.ContactsUtils;
import com.android.contacts.model.HardCodedSources;
import com.android.contacts.SimUtility;
import com.android.contacts.R;

import android.app.ActionBar;
import android.app.ListActivity;
import android.widget.CheckBox;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.content.AsyncQueryHandler;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
//import android.provider.Gmail;
import android.provider.Contacts.Settings;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.ContactMethodsColumns;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.provider.Contacts.PhonesColumns;
import android.provider.Contacts.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.Presence;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts.AggregationSuggestions;
import android.provider.ContactsContract.Intents.UI;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.CheckedTextView;
import android.widget.Checkable;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SimpleCursorAdapter;
import android.widget.FilterQueryProvider;
import android.provider.Contacts.People;
import android.net.Uri;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.lang.ref.WeakReference;
import java.lang.Long;

public final class MultiplePickSearchActivity extends SearchListActivity implements View.OnClickListener, FilterQueryProvider  {

    public interface Prefs {
        public static final String DISPLAY_ONLY_PHONES = "only_phones";
        public static final boolean DISPLAY_ONLY_PHONES_DEFAULT = false;
        public static final String PREF_DISPLAY_MOBILE_ONLY = "display mobile only";
    }
    
    /**
     * The MIME type of mms-mixed records (phone+email)
     */
    public static final String MMS_TYPE = "vnd.android.cursor.dir/phone_email";
    public static final String EMAIL_TYPE = "vnd.android.cursor.dir/email_v2";

    public static final long OPTION_PHONE_ONLY = 11;
    public static final long OPTION_EMAIL_ONLY = 12;
    public static final long COPY_DEVICE_TO_CARD = 13;
    public static final long COPY_CCARD_TO_DEVICE = 14;
    public static final long COPY_GCARD_TO_DEVICE = 15;

    private static final String TAG = "MultiplePickerSearch";
    private static final String NAME_FIELD = "name";
    private static final String TYPE_FIELD = "type";
    private static final String NUMBER_FIELD = "number";
    private static final String GROUP_FIELD = "_id";    // used for group display
    private static final String DATA_FIELD = "data";
    private static final String EMAIL_LABEL_STRING = "Email";
    private static final String PEOPLE_GROUPMEMBERSHIP_CONTENT_URI = "content://contacts/people/groupmembership";
    private static final String PEOPLE_GROUPMEMBERSHIP_CONTENT_FILTER_URI = "content://contacts/people/groupmembership/filter_name";
    private static final String PHONE_GROUPMEMBERSHIP_CONTENT_URI = "content://contacts/phones/groupmembership/people";
    private static final String PHONE_GROUPMEMBERSHIP_CONTENT_FILTER_URI = "content://contacts/phones/groupmembership/people/filter_name";
    private static final String PHONE_GROUPMEMBERSHIP_CONTACTMETHODS_CONTENT_URI = "content://contacts/phones/groupmembership/people/contactmethods";
    private static final String PHONE_GROUPMEMBERSHIP_CONTACTMETHODS_CONTENT_FILTER_URI = "content://contacts/phones/groupmembership/people/contactmethods/filter_name";
    private static final String PHONE_CONTENT_FILTER_NAME_URI = "content://contacts/phones/filter_name";
    private static final String MULTI_PICKER_INTENT_EXTRA_FIXED_GROUPID = "com.android.contacts.MultiPickerFixedGroupId";
    private static final String MULTI_PICKER_INTENT_EXTRA_COPY_DEVICE_TO_CARD = "com.android.contacts.MultiPickerCopyD2C";
    private static final String MULTI_PICKER_INTENT_EXTRA_FULL_LIST_URI = "com.android.contacts.MultiPickerFullListUri";
    private static final String MULTI_PICKER_INTENT_EXTRA_FULL_LIST_CHECKED_STATUS = "com.android.contacts.MultiPickerFullListCheckedStatus";
    private static final String MULTI_PICKER_INTENT_EXTRA_MMS_KIND = "com.android.contacts.MultiPickerMmsKind";
    private static final String MULTI_PICKER_INTENT_EXTRA_SINGLE_PICKER_MODE = "com.android.contacts.SinglePickerMode";
    private static final String MULTI_PICKER_INTENT_EXTRA_LINKAGE_INCLUDE = "com.android.contacts.MultiPickerIncludeLinkageId";
    private static final String MULTI_PICKER_INTENT_EXTRA_LINKAGE_EXCLUDE = "com.android.contacts.MultiPickerExcludeLinkageId";
    private static final String MULTI_PICKER_INTENT_EXTRA_WRITABLE_ONLY = "com.android.contacts.MultiPickerWritableOnly";
    private static final String MULTI_PICKER_INTENT_EXTRA_UNRESTRICTED_ONLY = "com.android.contacts.MultiPickerUnRestrictedOnly";
    private static final String MULTI_PICKER_INTENT_EXTRA_ACCOUNT_NAME = "com.android.contacts.MultiPickerAccountName";
    private static final String MULTI_PICKER_INTENT_EXTRA_ACCOUNT_TYPE = "com.android.contacts.MultiPickerAccountType";
    private static final String MULTI_PICKER_INTENT_EXTRA_SELECTION = "com.android.contacts.MultiPickerSelection";
    private static final String MULTI_PICKER_INTENT_EXTRA_SELECTION_ARG = "com.android.contacts.MultiPickerSelectionArg";        

    private static final int QUERY_TOKEN = 63;
    private static final int UPDATE_TOKEN = 64;
    private static final int RESULT_MULTIPICKER_SEARCH_ONSTOP = RESULT_FIRST_USER + 1;

    private static final int SHOW_EMPTY_TEXT_MESG = 1;
    private static final int HIDE_EMPTY_TEXT_MESG = 2;

    private static final String CLAUSE_ONLY_MOBILE = " AND (" + Data.DATA2 + "='" + Phone.TYPE_MOBILE + "' OR " + Data.DATA2 + "='" + Phone.TYPE_WORK_MOBILE + "' OR " + Data.DATA2 + "='" + Phone.TYPE_MMS + "')";
    private static final String CLAUSE_ONLY_VISIBLE = Contacts.IN_VISIBLE_GROUP + "=1";     
    
    private static final String CLAUSE_ONLY_PHONES = Contacts.HAS_PHONE_NUMBER + "=1";
    private static final String CLAUSE_ONLY_DEVICES = RawContacts.ACCOUNT_TYPE + "!=\"" +HardCodedSources.ACCOUNT_TYPE_CARD + "\"";
    private static final String CLAUSE_ONLY_CARDS = RawContacts.ACCOUNT_TYPE + "=\"" +HardCodedSources.ACCOUNT_TYPE_CARD + "\"";
    private static final String CLAUSE_NOT_CARDS = RawContacts.ACCOUNT_TYPE + "!=\"" +HardCodedSources.ACCOUNT_TYPE_CARD + "\"";
    private static final String CLAUSE_ONLY_WRITE = RawContacts.ACCOUNT_TYPE + "!=\"" +HardCodedSources.ACCOUNT_TYPE_BLUR + "\"";
    //???private static final String CLAUSE_ONLY_UNRESTRICTED = RawContacts.IS_RESTRICTED + "=0";    
    private static final String CLAUSE_ONLY_UNRESTRICTED = "1";   

    /** Unknown mode */
    static final int MODE_DEFAULT = 0;
    /** Mask for picker mode */
    static final int MODE_MASK_PICKER = 0x80000000;
    /** Mask for no presence mode */
    static final int MODE_MASK_NO_PRESENCE = 0x40000000;
    /** Mask for enabling list filtering */
    static final int MODE_MASK_NO_FILTER = 0x20000000;
    /** Mask for showing photos in the list */
    static final int MODE_MASK_SHOW_PHOTOS = 0x08000000;
    /** Show all contacts and pick them when clicking */
    static final int MODE_PICK_CONTACT = 40 | MODE_MASK_PICKER | MODE_MASK_SHOW_PHOTOS;
    /** Show all people through the legacy provider and pick them when clicking */
    static final int MODE_LEGACY_PICK_PERSON = 41 | MODE_MASK_PICKER;
    /** Show all phone numbers and pick them when clicking */
    static final int MODE_PICK_PHONE = 50 | MODE_MASK_PICKER | MODE_MASK_NO_PRESENCE;
    /** Show all phone numbers through the legacy provider and pick them when clicking */
    static final int MODE_LEGACY_PICK_PHONE =
            51 | MODE_MASK_PICKER | MODE_MASK_NO_PRESENCE | MODE_MASK_NO_FILTER;
    /** Show all email */
    static final int MODE_PICK_EMAIL = 60 | MODE_MASK_PICKER ;
    /** Show qualified mms types: phone+email */
    static final int MODE_PICK_MMS_MIXED = 65 | MODE_MASK_PICKER ;

    static final String NAME_SORT = Contacts.DISPLAY_NAME;
    static final String NAME_COLUMN = People.DISPLAY_NAME;
    static final String TYPE_COLUMN = People.TYPE;
    static final String NUMBER_COLUMN = People.NUMBER;
    static final String PERSON_COLUMN = Phones.PERSON_ID;
    static final String ID_COLUMN = Phones._ID;
    static final String TIMES_CONTACTED_COLUMN = People.TIMES_CONTACTED;

    static final String SORT_STRING = People.SORT_STRING;

    static final String[] CONTACT_METHODS_EMAIL_PROJECTION = new String[] {
        // only following 3 columns in email table
        ContactMethods._ID, // 0
        ContactMethods.DATA, // 1
        ContactMethods.NAME, // 2
    };

    static final String[] NORMAL_ITEM_PROJECTION = new String[] {
        // only need 4 columns for people/phones tables
        NAME_COLUMN, // 0
        TYPE_COLUMN, // 1
        NUMBER_COLUMN, // 2
        People._ID, // 3
    };

    static final String[] PHONE_ITEM_PROJECTION = new String[] {
        // phones table
        NAME_COLUMN, // 0
        TYPE_COLUMN, // 1
        NUMBER_COLUMN, // 2
        Phones._ID, // 3
        Phones.PERSON_ID, //4
    };

    static final String[] GROUPMEMBERSHIP_PEOPLE_PHONES_ITEM_PROJECTION = new String[] {
        // only need 5 columns for mixed groupmemebership+people+phones table
        NAME_COLUMN, // 0
        TYPE_COLUMN, // 1
        NUMBER_COLUMN, // 2
        People._ID, // 3
        PERSON_COLUMN, //4
    };

    static final String[] MMS_ITEM_PROJECTION = new String[] {
        // mixed phones column names
        NAME_COLUMN, // 0
        TYPE_COLUMN, // 1
        NUMBER_COLUMN, // 2
        PERSON_COLUMN,    // 3         = 'last_time_contacted' in contact_methods, used for identifying tables
        ID_COLUMN, // 4                = '_id' of phone or contact_methods
        TIMES_CONTACTED_COLUMN, // 5   = real 'person' of contact_methods
    };

    // normal & mms mixed item column index
    static final int COLUMN_INDEX_COM_NAME     = 0;
    static final int COLUMN_INDEX_COM_TYPE     = 1;
    static final int COLUMN_INDEX_COM_NUMBER   = 2;
    static final int COLUMN_INDEX_NORM_ID      = 3;

    static final int COLUMN_INDEX_GPP_PERSON   = 4;

    static final int COLUMN_INDEX_MMS_PERSON   = 3;
    static final int COLUMN_INDEX_MMS_ID       = 4;

    static final int COLUMN_INDEX_MMS_TIMES_CONTACTED = 5;    // used for return real 'person' of contact_methods

    /*
    static final String[] GROUPS_PROJECTION = new String[] {
        Groups._ID, // 0
        Groups.SYSTEM_ID, // 2
        Groups.NAME, // 2
    }; */
    static final int GROUPS_COLUMN_INDEX_ID = 0;
    static final int GROUPS_COLUMN_INDEX_SYSTEM_ID = 1;
    static final int GROUPS_COLUMN_INDEX_NAME = 2;

    static final String[] GROUPS_PROJECTION = new String[] {
        Groups._ID, // 0
        Groups.TITLE + " AS " + Contacts.DISPLAY_NAME, // 1
        Groups.SYSTEM_ID + " AS " + Phone.TYPE, // 2
        Groups.SUMMARY_COUNT,   // don't support ' AS xxx '
        Groups.TITLE_RES, // 4
        Groups.RES_PACKAGE + " AS " + Phone.NUMBER, // 5
    };

    static final int COL_ID = 0;
    static final int COL_TITLE = 1;
    static final int COL_SYSTEM_ID = 2;
    static final int COL_SUMMARY_COUNT = 3;
    static final int COL_TITLE_RES = 4;
    static final int COL_RES_PACKAGE = 5;

    static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] {
        Contacts._ID, // 0
        Contacts.DISPLAY_NAME, // 1
        Contacts.STARRED, //2
        Contacts.TIMES_CONTACTED, //3
        //Presence.PRESENCE_STATUS, //4
        Contacts.CONTACT_PRESENCE,  //4
        Contacts.PHOTO_ID, //5
        Contacts.HAS_PHONE_NUMBER, //6
        Contacts.LOOKUP_KEY, //7
    };

    static final int SUMMARY_ID_COLUMN_INDEX = 0;
    static final int SUMMARY_NAME_COLUMN_INDEX = 1;
    static final int SUMMARY_STARRED_COLUMN_INDEX = 2;
    static final int SUMMARY_TIMES_CONTACTED_COLUMN_INDEX = 3;
    static final int SUMMARY_PRESENCE_STATUS_COLUMN_INDEX = 4;
    static final int SUMMARY_PHOTO_ID_COLUMN_INDEX = 5;
    static final int SUMMARY_HAS_PHONE_COLUMN_INDEX = 6;
    static final int SUMMARY_LOOKUP_KEY = 7;

    static final String[] RAW_CONTACTS_SUMMARY_PROJECTION = new String[] {
        RawContacts._ID, // 0
        Contacts.DISPLAY_NAME, // 1
    };

    static final int RAW_SUMMARY_ID_COLUMN_INDEX = 0;
    static final int RAW_SUMMARY_NAME_COLUMN_INDEX = 1;

    static final String[] PHONES_PROJECTION = new String[] {
        Data._ID, //0
        CommonDataKinds.Phone.TYPE, //1
        CommonDataKinds.Phone.LABEL, //2
        CommonDataKinds.Phone.NUMBER, //3
        Contacts.DISPLAY_NAME, // 4
        RawContacts.CONTACT_ID, // 5
    };
    static final int PHONE_ID_COLUMN_INDEX = 0;
    static final int PHONE_TYPE_COLUMN_INDEX = 1;
    static final int PHONE_LABEL_COLUMN_INDEX = 2;
    static final int PHONE_NUMBER_COLUMN_INDEX = 3;
    static final int PHONE_DISPLAY_NAME_COLUMN_INDEX = 4;
    static final int PHONE_CONTACT_ID_COLUMN_INDEX = 5;

    static final String[] EMAIL_PROJECTION = new String[] {
        Data._ID, //0
        CommonDataKinds.Email.TYPE, //1
        CommonDataKinds.Email.LABEL, //2
        CommonDataKinds.Email.DATA, //3
        Contacts.DISPLAY_NAME, // 4
        RawContacts.CONTACT_ID, // 5
    };
    static final int EMAIL_ID_COLUMN_INDEX = 0;
    static final int EMAIL_TYPE_COLUMN_INDEX = 1;
    static final int EMAIL_LABEL_COLUMN_INDEX = 2;
    static final int EMAIL_DATA_COLUMN_INDEX = 3;
    static final int EMAIL_DISPLAY_NAME_COLUMN_INDEX = 4;
    static final int EMAIL_CONTACT_ID_COLUMN_INDEX = 5;

    static final String GROUP_WITH_PHONES = "android_smartgroup_phone";
    static final int DISPLAY_GROUP_INDEX_ALL_CONTACTS = 0;
    static final int DISPLAY_GROUP_INDEX_ALL_CONTACTS_WITH_PHONES = 1;
    static final int DISPLAY_GROUP_INDEX_MY_CONTACTS = 2;
    /** Unknown display type. */
    static final int DISPLAY_TYPE_UNKNOWN = -1;
    /** Display all contacts */
    static final int DISPLAY_TYPE_ALL = 0;
    /** Display all contacts that have phone numbers */
    static final int DISPLAY_TYPE_ALL_WITH_PHONES = 1;
    /** Display a system group */
    static final int DISPLAY_TYPE_SYSTEM_GROUP = 2;
    /** Display a user group */
    static final int DISPLAY_TYPE_USER_GROUP = 3;

    SimpleCursorAdapter mPickerAdapter;
    private QueryHandler mQueryHandler;
    // check mark status for current group
    boolean[] mChecked = null;
    // uris of current group
    Uri[] mCurrentGroupUri4Return = null;
    // common uri portion
    Uri mBaseUri4Return = null;
//    boolean mSetCheckMark = false;
//    boolean mSelectAll;
    // check mark status for the full list
//    boolean[] mFullListChecked = null;
    // uris of the checked list
//    Uri[] mFullListUri4Return = null;
    ArrayList<Uri> mFullListUri4Return = new ArrayList<Uri>();
    int mMode = MODE_DEFAULT;
    long mIntentGroup = 0;
//    long mCGroupId = 0;
//    long mGGroupId = 0;
    long mVirtualGroupId = 0;

//    private int mDisplayGroupOriginalSelection = 0;
    // The current display group
    private String mDisplayInfo=null;
    private int mDisplayType = DISPLAY_TYPE_ALL;
    private long mGroupId = 0;
    // The current list of display groups, during selection from menu
//    private CharSequence[] mDisplayGroups;
    // The groups' ids
//    private Long[] mGroupIds;
    // If true position 2 in mDisplayGroups is the MyContacts group
//    private boolean mDisplayGroupsIncludesMyContacts = false;

    // private boolean mDisplayAll;
    private boolean mDisplayOnlyMobiles;
    private boolean mDisplayOnlyPhones;
    private CharSequence mUnknownName;
    private long mCurrentMmsKind;
    private long mLinkageIdInc = -1;
    private long mLinkageIdExc = -1;
    private String mExcludedIds = null;
    private String mAccountName = null;
    private String mAccountType = null;
    private boolean mJustCreated;
    private ListView mListView;
    private String mPassedInSelection = null;
    private long mIntentCopyD2C = 0;
    private boolean mSingleMode = false;
    private boolean mWritableOnly = false;
    private boolean mUnRestrictedOnly = false;    
    String mSelection = null;
    String[] mSelectionArgs = null;    

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TextView empty = null;
            switch (msg.what) {
                case SHOW_EMPTY_TEXT_MESG:
                    empty = (TextView) findViewById(R.id.emptyText);
                    //Log.v(TAG, "handleMessage(SHOW_EMPTY_TEXT_MESG), empty ="+empty);
                    if (empty != null) {
                        empty.setVisibility(View.VISIBLE);
                    }
                    break;

                case HIDE_EMPTY_TEXT_MESG:
                    empty = (TextView) findViewById(R.id.emptyText);
                    //Log.v(TAG, "handleMessage(HIDE_EMPTY_TEXT_MESG), empty ="+empty);
                    if (empty != null) {
                        empty.setVisibility(View.INVISIBLE);
                    }
                    break;
            }
        }
    };


    private static final class QueryHandler extends AsyncQueryHandler {
        private final WeakReference<MultiplePickSearchActivity> mActivity;

        public QueryHandler(Context context) {
            super(context.getContentResolver());
            mActivity = new WeakReference<MultiplePickSearchActivity>(
                    (MultiplePickSearchActivity) context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            Log.v(TAG, "onQueryComplete()");
            final MultiplePickSearchActivity activity = mActivity.get();
            if (activity != null && !activity.isFinishing()) {
                final SimpleCursorAdapter pickerAdapter = activity.mPickerAdapter;

                TextView empty = (TextView) activity.findViewById(R.id.emptyText);
                if (cursor != null && cursor.getCount() > 0) {
                    //empty.setVisibility(View.GONE);
                    empty.setVisibility(View.INVISIBLE);
                }
                else
                    empty.setVisibility(View.VISIBLE);

                if (cursor != null) {
                    int count = cursor.getCount();
                    boolean changed = true;

                    // show 'empty' text if no list items
                    //TextView empty = (TextView) activity.findViewById(R.id.emptyText);
                    if (count <= 0) {
                        if (activity.mGroupId == 0) {
                            // all list is empty
                            //Log.v(TAG, "set all_emtpy visible");
                        } else {
                            //empty.setVisibility(View.VISIBLE);
                            //Log.v(TAG, "set emtpy visible");
                        }

                    } else {
                        Log.v(TAG, "count > 0 : "+count+", re-init/sync globals, cursor pos = "+cursor.getPosition());
                        // init full_list data only once at the very first beginning
                        /*
                        if (activity.mFullListChecked == null) {
                            activity.mFullListChecked = new boolean[count];
                            activity.mFullListUri4Return = new Uri[count];
                            Log.v(TAG, "INIT the FULL_LIST_URI & CHECK_STATUS");

                            // build full_list uri for returning
                            activity.buildData4Return(cursor, true);

                            // sync checked status if there are passed-in list
                            if (activity.mChecked != null || activity.mCurrentGroupUri4Return != null) {
                                Log.v(TAG, "sync PASSED-IN status");
                                activity.syncCheckedStatus(false);
                                // clear passed-in list status temporarily stored in current list
                                activity.mChecked = null;
                                activity.mCurrentGroupUri4Return = null;
                            }
                        } */

                        // init current list with new queried cursor
                        if (activity.mChecked == null || (activity.mChecked != null && count != activity.mChecked.length)) {
                            Log.v(TAG, "re-init current group globals: mChecked &  mCurrentGroupUri4Return with count = "+count);
                            activity.mChecked = new boolean[count];
                            activity.mCurrentGroupUri4Return = new Uri[count];
                            changed = true;

                            // build list data for current group
                            activity.buildData4Return(cursor, false);

                            // sync checked status from checked_list to current group
                            activity.syncCheckedStatus(true);

                            //empty.setVisibility(View.INVISIBLE);
                        } else {
                            changed = false;
                            Log.v(TAG, "no change of List Items, no refresh !");
                        }
                    }

                    if (changed) {
                        //Log.v(TAG, "before moving cursor position = "+cursor.getPosition());
                        cursor.moveToPosition(-1);
                        pickerAdapter.changeCursor(cursor);

                        // a trick way to reset the list, otherwise it moves to bottom
                        activity.setListAdapter(activity.mPickerAdapter);
                        //Log.v(TAG, "after moving of cursor position = "+cursor.getPosition());
                    }
                } else {
                    Log.v(TAG, "cursor = "+ cursor);
                }
            } else {
                cursor.close();
            }
        }
    }

    private void buildData4Return(Cursor cursor, boolean isFullList) {
        //Log.v(TAG, "buildData4Return(), cursor="+cursor+", isFullList="+isFullList);
        if (cursor != null) {
            //int count = cursor.getCount();
            int pos = 0;
            long id = 0;

            // init the cursor postion
            cursor.moveToPosition(-1);
            //Log.v(TAG, "count="+count+", cursor pos="+cursor.getPosition());
            while (cursor.moveToNext()) {
                if (mMode == MODE_LEGACY_PICK_PERSON) {
                    if (mDisplayType == DISPLAY_TYPE_USER_GROUP) {
                        // groupmembership + people + phone joint table.
                        // '_id' is groupmembership's id, should use 'person' instead
                        id = cursor.getLong(COLUMN_INDEX_GPP_PERSON);
                    } else {
                        // people table
                        id = cursor.getLong(COLUMN_INDEX_NORM_ID);
                    }
                } else if (mMode == MODE_PICK_CONTACT) {
                    // new contact provider
                    id = cursor.getLong(SUMMARY_ID_COLUMN_INDEX);
                } else if (mMode == MODE_LEGACY_PICK_PHONE) {
                    // phones table
                    id = cursor.getLong(COLUMN_INDEX_NORM_ID);
                } else if (mMode == MODE_PICK_PHONE) {
                    // new phone provider
                    id = cursor.getLong(PHONE_ID_COLUMN_INDEX);
                } else if (mMode == MODE_PICK_EMAIL) {
                    // new email provider
                    id = cursor.getLong(EMAIL_ID_COLUMN_INDEX);
                } else if (mMode == MODE_PICK_MMS_MIXED) {
                    /*
                    if (mDisplayType == DISPLAY_TYPE_ALL_WITH_PHONES) {
                        // phones table
                        mBaseUri4Return = Phones.CONTENT_URI;
                        id = cursor.getLong(COLUMN_INDEX_NORM_ID);
                    } else {
                        // mixed table
                        boolean isPhone = false;
                        isPhone = ! cursor.isNull(COLUMN_INDEX_MMS_PERSON);
                        if (isPhone) {
                            // phones table
                            mBaseUri4Return = Phones.CONTENT_URI;
                            id = cursor.getLong(COLUMN_INDEX_MMS_ID);
                        } else {
                            // contact_methods table _id=[8]
                            mBaseUri4Return = ContactMethods.CONTENT_URI;
                            id = cursor.getLong(COLUMN_INDEX_MMS_ID);
                        }
                    } */
                    if (OPTION_EMAIL_ONLY == mCurrentMmsKind) {
                        // use email's
                        id = cursor.getLong(EMAIL_ID_COLUMN_INDEX);
                    } else {
                        // use phone's
                        id = cursor.getLong(PHONE_ID_COLUMN_INDEX);
                    }
                } else {
                    id = cursor.getLong(cursor.getColumnIndex(ContactMethods._ID));
                }

                if (isFullList) {
                    // full list, only do once
                    Log.v(TAG, "do nothing !");
                    //mFullListUri4Return[pos] = Uri.withAppendedPath(mBaseUri4Return, java.lang.Long.toString(id));
                    //Log.v(TAG, "FullList: mFullListUri4Return["+pos+"]="+mFullListUri4Return[pos]);
                } else {
                    // current list, update whenever cursor changes
                    mCurrentGroupUri4Return[pos] = Uri.withAppendedPath(mBaseUri4Return, java.lang.Long.toString(id));
                    //Log.v(TAG, "CurList: mCurrentGroupUri4Return["+pos+"]="+mCurrentGroupUri4Return[pos]);
                }
                pos++;
            }

        } else {
            Log.v(TAG, "cursor = null");
        }
    }

    private void syncCheckedStatus(boolean fromFullToCurrent) {
        //Log.v(TAG, "syncCheckedStatus, direction: " + (fromFullToCurrent ? "Full->Current" : "Current->Full"));
        //long start = System.currentTimeMillis();
        if ((fromFullToCurrent && mFullListUri4Return != null && mCurrentGroupUri4Return != null)
            ||(!fromFullToCurrent && mCurrentGroupUri4Return != null)) {
            int fullLen = 0;
            if (mFullListUri4Return != null) {
                fullLen = mFullListUri4Return.size();
            }
            int curLen = mCurrentGroupUri4Return.length;
            int i=0, j=0;
            boolean found = false;

            if (fullLen > 0) {
                if (fromFullToCurrent) {
                    // update status from F to C
                    for (j = 0; j < curLen; j++) {
                        if (mFullListUri4Return.contains(mCurrentGroupUri4Return[j])) {
                            //Log.v(TAG, "F-C matched, curlen="+curLen+", fullLen="+fullLen+", curChecked="+mChecked[j]+", fullchecked="+mFullListChecked[i]+",i="+i+",j="+j);
                            // found
                            mChecked[j] = true;
                        }
                    }

                } else {
                    // update status from C to F
                    for (i = 0; (i < curLen && i<mChecked.length); i++) {
                        found = false;
                        if (mFullListUri4Return.contains(mCurrentGroupUri4Return[i])) {
                            if (!mChecked[i]) {
                                // un-checked in current list, need remove it from checked list
                                mFullListUri4Return.remove(mCurrentGroupUri4Return[i]);
                            }
                            found = true;
                        }

                        if (!found && mChecked[i]) {
                            // checked but not yet in full list, need append it
                            mFullListUri4Return.add(mCurrentGroupUri4Return[i]);
                        }
                    }
                }
            } else {
                // empty checked list, append C->F
                if (!fromFullToCurrent) {
                    for (i = 0; (i < curLen && i<mChecked.length); i++) {
                        if (mChecked[i]) {
                            mFullListUri4Return.add(mCurrentGroupUri4Return[i]);
                        }
                    }
                }
            }
        } else {
            Log.v(TAG, "do nothing !");
        }

        //long end = System.currentTimeMillis();
        //Log.v(TAG, "    Leaving sync(), elapse time: "+(end-start));
    }

    /**
     * Handles clicks on the list items
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Log.v(TAG, "onListItemClick(), l="+l+", v="+v+", pos="+position+", id="+id);

        // if single picker, just return shortly
        if (mSingleMode && mCurrentGroupUri4Return != null && position < mCurrentGroupUri4Return.length) {
            Intent intent = new Intent();
            Uri uri = mCurrentGroupUri4Return[position];
            Log.v(TAG, "return from SINGLE_PICKER with uri: "+uri);
            setResult(RESULT_OK, intent.setData(uri));
            finish();
        } else if (mChecked != null && l != null && position < mChecked.length && mCurrentGroupUri4Return != null && position < mCurrentGroupUri4Return.length) {
            //boolean isChecked = l.isItemChecked(position);
            // toggle
            boolean isChecked = !(mChecked[position]);
            // record checkmark for specified position
            mChecked[position] = isChecked;
            Log.v(TAG, "mChecked[pos]=" + mChecked[position]);

            // set the checkmark view
            if (v != null && v.findViewById(R.id.multi_picker_list_item_name) != null ) {
                ((CheckBox)v.findViewById(R.id.checkitembox)).setChecked(isChecked);
            }

            // count it into the whole selected items
            if (isChecked && mCurrentGroupUri4Return != null && !mFullListUri4Return.contains(mCurrentGroupUri4Return[position])) {
                mFullListUri4Return.add(mCurrentGroupUri4Return[position]);
                Log.v(TAG, "added into F");
            } else if (!isChecked && mCurrentGroupUri4Return != null && mFullListUri4Return.contains(mCurrentGroupUri4Return[position])) {
                mFullListUri4Return.remove(mCurrentGroupUri4Return[position]);
                Log.v(TAG, "removed from F");
            }
        }
    }

    /**
     * Handles clicks on the 'Done' buttons
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ok: {
                int count = 0;
                int selectedCount = 0;
                ArrayList<String> uris = new ArrayList<String>();

                Log.v(TAG, "Entering onClick_OK()");
                //long start = System.currentTimeMillis();
                // update the current list to checked list
                //syncCheckedStatus(false);

                if (mFullListUri4Return != null) {
                    count = mFullListUri4Return.size();
                }

                Log.v(TAG, "all items count=" +count);
                if (count > 0) {
                    int i = 0;
                    for (i=0; i < count; i++) {
                        uris.add(mFullListUri4Return.get(i).toString());
                        //Log.v(TAG, " resultUris[" + i + "]=" + mFullListUri4Return.get(i));
                    }
                    //long end = System.currentTimeMillis();
                    //Log.v(TAG, "Leaving onClick_OK(), elapse time: "+(end-start));

                    Intent intent = new Intent();
                    //intent.putExtra("com.android.contacts.MultiPickerResult", resultAppened);
                    intent.putStringArrayListExtra(Intent.EXTRA_STREAM, uris);
                    setResult(RESULT_OK, intent);
                } else {
                	Log.v(TAG, "none selected or all cleared ! , uris.size = "+uris.size());
                    //setResult(RESULT_CANCELED);
                    Intent intent = new Intent();
                    intent.putStringArrayListExtra(Intent.EXTRA_STREAM, uris);
                    setResult(RESULT_OK, intent);
                }

                finish();
                break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedState) {
        Log.v(TAG, " Entering MultiPickSearch onCreate()");
        super.onCreate(savedState);

        // Resolve the intent
        final Intent intent = getIntent();

        // Allow the title to be set to a custom String using an extra on the intent
        String title = intent.getStringExtra(UI.TITLE_EXTRA_KEY);
        
        final String action = intent.getAction();
        mMode = MODE_DEFAULT;

        mSelection = intent.getStringExtra(MULTI_PICKER_INTENT_EXTRA_SELECTION);
        mSelectionArgs = intent.getStringArrayExtra(MULTI_PICKER_INTENT_EXTRA_SELECTION_ARG);     
        Log.v(TAG, " passed in title: "+ title+", mSelection: "+mSelection);   
        
        if ("com.motorola.action.PICK_MULTIPLE_SEARCH".equals(action)) {
            // XXX These should be showing the data from the URI given in the Intent.
            final String type = intent.resolveType(this);
            Log.v(TAG, " action = PICK_MULTIPLE_SEARCH, type=" + type);
            //Log.v(TAG, " ContactMethods.CONTENT_EMAIL_TYPE = " + ContactMethods.CONTENT_EMAIL_TYPE);
            if (People.CONTENT_TYPE.equals(type)) {
                mMode = MODE_LEGACY_PICK_PERSON;
                title = getResources().getString(R.string.select_people);
                mIntentGroup = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_FIXED_GROUPID, 0);
                Log.v(TAG, "MODE_LEGACY_PICK_PERSON");
            } else if (Phones.CONTENT_TYPE.equals(type)) {
                mMode = MODE_LEGACY_PICK_PHONE;
                title = getResources().getString(R.string.select_phone);
                //get extra request
                mIntentGroup = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_FIXED_GROUPID, 0);
                Log.v(TAG, "MODE_LEGACY_PICK_PHONE: passed in requested group_id = " + mIntentGroup);
            } else if (MultiplePickSearchActivity.EMAIL_TYPE.equals(type)) {
                mMode = MODE_PICK_EMAIL;
                mLinkageIdInc = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_LINKAGE_INCLUDE, 0);
                mLinkageIdExc = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_LINKAGE_EXCLUDE, 0);
                // select email excluding specified linkage
                title = getResources().getString(R.string.select_email);
                Log.v(TAG, "MODE_PICK_EMAIL: passed in copyD2C="+mIntentCopyD2C+", selection: " + mPassedInSelection
                           +", group_specified= "+mIntentGroup+", exc_group: "+mLinkageIdExc+", inc_group: "+mLinkageIdInc);
            } else if (MultiplePickSearchActivity.MMS_TYPE.equals(type)) {
                mMode = MODE_PICK_MMS_MIXED;
                title = getResources().getString(R.string.contactMultiplePickerActivityTitle);
                //get extra request
                mIntentGroup = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_FIXED_GROUPID, 0);
                mCurrentMmsKind = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_MMS_KIND, OPTION_PHONE_ONLY);
                Log.v(TAG, "MMS: passed in requested group_id = " + mIntentGroup+", mCurrentMmsKind="+mCurrentMmsKind);
            } else if (Contacts.CONTENT_TYPE.equals(type)) {
                mMode = MODE_PICK_CONTACT;
                mLinkageIdInc = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_LINKAGE_INCLUDE, 0);
                mLinkageIdExc = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_LINKAGE_EXCLUDE, 0);
                title = getResources().getString(R.string.select_people);
                mIntentGroup = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_FIXED_GROUPID, 0);
                mIntentCopyD2C = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_COPY_DEVICE_TO_CARD, 0);
                mSingleMode = intent.getBooleanExtra(MULTI_PICKER_INTENT_EXTRA_SINGLE_PICKER_MODE, false);
                mWritableOnly = intent.getBooleanExtra(MULTI_PICKER_INTENT_EXTRA_WRITABLE_ONLY, false);
                mUnRestrictedOnly = intent.getBooleanExtra(MULTI_PICKER_INTENT_EXTRA_UNRESTRICTED_ONLY, false);                
                // extra value of specified account
                mAccountName = intent.getStringExtra(MULTI_PICKER_INTENT_EXTRA_ACCOUNT_NAME);
                mAccountType = intent.getStringExtra(MULTI_PICKER_INTENT_EXTRA_ACCOUNT_TYPE);

                if (mIntentCopyD2C == COPY_DEVICE_TO_CARD) {
                	// only list non-card entry
                	mPassedInSelection = CLAUSE_ONLY_DEVICES;
                } else if (mIntentCopyD2C == COPY_CCARD_TO_DEVICE) {
                	// only list card entry
                	mPassedInSelection = RawContacts.ACCOUNT_NAME + "=\""
                	        +SimUtility.getAccountNameByType(TelephonyManager.PHONE_TYPE_CDMA) + "\"";
                } else if (mIntentCopyD2C == COPY_GCARD_TO_DEVICE) {
                	// only list card entry
                	mPassedInSelection = RawContacts.ACCOUNT_NAME + "=\""
                	        +SimUtility.getAccountNameByType(TelephonyManager.PHONE_TYPE_GSM) + "\"";
                }

                if (mIntentGroup > 0) {
                    if (TextUtils.isEmpty(mPassedInSelection)) {
                    	mPassedInSelection = RawContacts._ID + " IN " + ContactsUtils.getGroupMemberIds(getContentResolver(), mIntentGroup);
                    } else {
                    	mPassedInSelection += " AND " + RawContacts._ID + " IN " + ContactsUtils.getGroupMemberIds(getContentResolver(), mIntentGroup);
                    }
                }

                // select contact excluding specified group
                if (mLinkageIdExc > 0 ) {
                    //mPassedInSelection = CLAUSE_EXCLUDE_SPECIFIED_GROUP + mLinkageIdExc;
                    mExcludedIds = ContactsUtils.getGroupMemberIds(getContentResolver(), mLinkageIdExc);
                } else {
                    mExcludedIds = intent.getStringExtra("com.android.contacts.MultiPickerExcludeMemberIds");
                }

                if (mExcludedIds != null) {
                	if (!TextUtils.isEmpty(mExcludedIds)) {
                		mPassedInSelection = RawContacts._ID + " NOT IN " + mExcludedIds;
                	} else {
                		mPassedInSelection  = "1";
                	}

                    // don't include C/G cards contacts
                    mPassedInSelection += " AND " + CLAUSE_NOT_CARDS;
                } else if (mLinkageIdExc < 0 ) {
                	// not determined group_id yet, e.g. new group
                	mPassedInSelection = CLAUSE_NOT_CARDS;
                }

                if (mLinkageIdInc > 0 ) {
                	mPassedInSelection = RawContacts._ID + " IN " + ContactsUtils.getGroupMemberIds(getContentResolver(), mLinkageIdInc);
                }
                
                if (mWritableOnly) {
                	  //???String excludeReadonly = SimUtility.excludeReadonly(MultiplePickSearchActivity.this);
                	  String excludeReadonly = "";
                	  if (!TextUtils.isEmpty(excludeReadonly)) {
                        if (TextUtils.isEmpty(mPassedInSelection)) {
                            mPassedInSelection = excludeReadonly;
                        } else {
                            mPassedInSelection += " AND " + excludeReadonly;
                        }
                    }
                }
                
                if (mUnRestrictedOnly) {
                    if (TextUtils.isEmpty(mPassedInSelection)) {
                        mPassedInSelection = CLAUSE_ONLY_UNRESTRICTED;
                    } else {
                        mPassedInSelection += " AND " + CLAUSE_ONLY_UNRESTRICTED;
                    }                	
                }                
                                
                // if specified account name/type
                if (TextUtils.isEmpty(mPassedInSelection)) {
                	if (mAccountName != null) {
                	    mPassedInSelection = RawContacts.ACCOUNT_NAME + "=\"" + mAccountName + "\"";
                    }

                	if (mAccountType != null && TextUtils.isEmpty(mPassedInSelection)) {
                	    mPassedInSelection = RawContacts.ACCOUNT_TYPE + "=\"" + mAccountType + "\"";
                    } else if (mAccountType != null && !TextUtils.isEmpty(mPassedInSelection)) {
                    	mPassedInSelection += " AND " + RawContacts.ACCOUNT_TYPE + "=\"" + mAccountType + "\"";
                    }
                } else {
                	if (mAccountName != null) {
                	    mPassedInSelection += " AND " + RawContacts.ACCOUNT_NAME + "=\"" + mAccountName + "\"";
                    }

                	if (mAccountType != null) {
                	    mPassedInSelection += " AND " + RawContacts.ACCOUNT_TYPE + "=\"" + mAccountType + "\"";
                    }
                }
                Log.v(TAG, "PICK_CONTACTS: passed in single_mode= "+mSingleMode +", copyD2C="+mIntentCopyD2C+", selection: " + mPassedInSelection
                           +", group_specified= "+mIntentGroup+", exc_group: "+mLinkageIdExc+", inc_group: "+mLinkageIdInc);
            } else if (Phone.CONTENT_TYPE.equals(type)) {
                mMode = MODE_PICK_PHONE;
                title = getResources().getString(R.string.select_phone);
                //get extra request
                mIntentGroup = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_FIXED_GROUPID, 0);
                mLinkageIdInc = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_LINKAGE_INCLUDE, 0);
                mLinkageIdExc = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_LINKAGE_EXCLUDE, 0);
                // select phone excluding specified linkage
                Log.v(TAG, "MODE_PICK_PHONE: passed in copyD2C="+mIntentCopyD2C+", selection: " + mPassedInSelection
                           +", group_specified= "+mIntentGroup+", exc_group: "+mLinkageIdExc+", inc_group: "+mLinkageIdInc);

            }

            // get list uris and status passed-in
            String [] s = intent.getStringArrayExtra(MULTI_PICKER_INTENT_EXTRA_FULL_LIST_URI);
            if (s != null) {
                int count = s.length;
                /*
                if (mChecked == null || mCurrentGroupUri4Return == null) {
                    mChecked = new boolean[count];
                    mCurrentGroupUri4Return = new Uri[count];
                }
                // keep it in current list
                for (int i = 0; i < count; i++) {
                    mCurrentGroupUri4Return[i] = Uri.parse(s[i]);
                } */

                // keep it in checked list
                mFullListUri4Return.clear();
                for (int i = 0; i < count; i++) {
                    mFullListUri4Return.add(Uri.parse(s[i]));
                }

                Log.v(TAG, "passed in checked list count = " + count +", size = "+ mFullListUri4Return.size());

                // get checked status of the list uris passed in
                //mChecked = intent.getBooleanArrayExtra(MULTI_PICKER_INTENT_EXTRA_FULL_LIST_CHECKED_STATUS);
            }

            // set title for multi-picker
            if (title != null) {
                setTitle(title);
            }
        }
/*
        try{
            mCGroupId = SimUtility.getGroupId(getContentResolver(), SimUtility.C_GROUP_NAME);
        }
            catch(IllegalStateException e) {
                  mCGroupId = -1;
            }
        try{
            mGGroupId = SimUtility.getGroupId(getContentResolver(), SimUtility.G_GROUP_NAME);
        }
            catch(IllegalStateException e) {
                  mGGroupId = -1;
            }
*/
        setContentView(R.layout.multi_picker_search_fixed);

        // Set the proper empty string
        setEmptyText();

        // only display required group
        if (mIntentGroup > 0) {
            mDisplayType = DISPLAY_TYPE_USER_GROUP;
            // mDisplayInfo = ;
            mGroupId = mIntentGroup;
        }

        String [] from;
        int [] to;
        switch (mMode) {
            case MODE_LEGACY_PICK_PERSON:
            case MODE_LEGACY_PICK_PHONE:
            // case MODE_PICK_MMS_MIXED:              legacy mms
                from = new String[] {NAME_COLUMN, TYPE_FIELD, NUMBER_FIELD, GROUP_FIELD};
                to = new int[] {R.id.multi_picker_list_item_name, R.id.label, R.id.number, R.id.group};
                break;
/*
            case MODE_PICK_EMAIL:
                from = new String[] {NAME_COLUMN, TYPE_FIELD, DATA_FIELD, GROUP_FIELD};
                to = new int[] {R.id.multi_picker_list_item_name, R.id.label, R.id.number, R.id.group};
                break;
*/
            case MODE_PICK_MMS_MIXED:
            case MODE_PICK_PHONE:
                from = new String[] {Contacts.DISPLAY_NAME, Data._ID, CommonDataKinds.Phone.TYPE, CommonDataKinds.Phone.NUMBER};
                to = new int[] {R.id.multi_picker_list_item_name, R.id.checkitembox, R.id.label, R.id.number};
                break;

            case MODE_PICK_EMAIL:
                from = new String[] {Contacts.DISPLAY_NAME, Data._ID, CommonDataKinds.Email.TYPE, CommonDataKinds.Email.DATA};
                to = new int[] {R.id.multi_picker_list_item_name, R.id.checkitembox, R.id.label, R.id.number};
                break;

            default:   // MODE_PICK_CONTACT
                // only display name
                from = new String[] {Contacts.DISPLAY_NAME, RawContacts._ID};
                to = new int[] {R.id.multi_picker_list_item_name, R.id.checkitembox};
                break;
        }

        mPickerAdapter = new SimpleCursorAdapter(this,
                                          R.layout.multi_picker_list_item,
                                          null,
                                          from,
                                          to);

        SimpleCursorAdapter.ViewBinder viewBinder = new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                int position = 0;
                if (cursor != null) {
                    position = cursor.getPosition();
                }

                /*
                boolean isPhone = false;
                if (mMode == MODE_PICK_MMS_MIXED) {
                    // !!! column name is determined by 1st table of UNION sql, eg. phone+email
                    // same column index have different meaning in 2 tables, need adjust here !!!
                    // distinguish 2 tables' columns, phone[3].person = contact_methods[3].last_time_contacted
                    isPhone = ! cursor.isNull(COLUMN_INDEX_MMS_PERSON);
                } */

                if (cursor != null && Contacts.DISPLAY_NAME.equals(cursor.getColumnName(columnIndex))) {
                    // bind the 'name' field
                    /*
                    if (mMode==MODE_PICK_MMS_MIXED && isPhone==false) {
                        // for contact_methods table, display_name=column[6]
                        ((CheckedTextView)view).setText(cursor.getString(COLUMN_INDEX_COM_NAME));
                    } else {
                        //Log.v(TAG, "---- NAME field----, count="+count+", mSetCheckMark ="+mSetCheckMark);
                        ((CheckedTextView)view).setText(cursor.getString(columnIndex));
                    } */
                    // Log.v(TAG, "mChecked = "+ mChecked);
                    // Log.v(TAG, "ViewBinder-setViewValue(), cursor = " + cursor);
                    // Log.v(TAG, "cursor count = " + cursor.getCount());
                    // Log.v(TAG, "setCheckMark for pos=" + position + ", mChecked.length=" + mChecked.length);
                    String name = cursor.getString(columnIndex);
                    if (!TextUtils.isEmpty(name)) {
                        ((TextView)view).setText(name);
                    } else {
                        ((TextView)view).setText(mUnknownName);
                    }

                    return true;
                //} else if (cursor != null && TYPE_FIELD.equals(cursor.getColumnName(columnIndex))) {
                }else if(view instanceof CheckBox && view != null){
		    if(mSingleMode) {
                    	// check mark IN-visible for Single_Picker
                    	((CheckBox)view).setButtonDrawable(android.R.color.transparent);
                    } else if (mChecked != null && mChecked.length>position && mListView != null) {
                        // pretect index over-boundary due to timing between finish() and refresh display
                        ((CheckBox)view).setChecked(mChecked[position]);

			((CheckBox)view).setOnClickListener(new View.OnClickListener(){
			    public void onClick(View view){
				checkBoxClicked(view);
			    }
			});
                    }
		    return true;
	        } else if (cursor != null && CommonDataKinds.Phone.TYPE.equals(cursor.getColumnName(columnIndex))) {
                    // bind the 'type' field for both PICK_PHONE and PICK_EMAIL modes
                    int type = cursor.getInt(columnIndex);
                    String label = cursor.getString(PHONE_LABEL_COLUMN_INDEX);

                    //Log.v(TAG, "type or data = " +type+", label = "+label);
                    if (OPTION_EMAIL_ONLY == mCurrentMmsKind) {
                    	// PICK_EMAIL
                        ((TextView)view).setText(Email.getTypeLabel(MultiplePickSearchActivity.this.getResources(), type, label));
                    } else {
                    	// PICK_PHONE
                        ((TextView)view).setText(Phone.getDisplayLabel(MultiplePickSearchActivity.this, type, label));
                    }
					((TextView)view).setVisibility(View.VISIBLE);
                    return true;
                //} else if (cursor != null && NUMBER_FIELD.equals(cursor.getColumnName(columnIndex)) &&
                //    mMode==MODE_PICK_MMS_MIXED && isPhone==false) {
                } else if (cursor != null && CommonDataKinds.Phone.NUMBER.equals(cursor.getColumnName(columnIndex))) {
                    // bind the 'number' field for phone
                    ((TextView)view).setText(cursor.getString(columnIndex));
                    // Log.v(TAG, "=========== number field: "+  cursor.getString(PHONE_NUMBER_COLUMN_INDEX));
					((TextView)view).setVisibility(View.VISIBLE);
                    return true;
                } else if (cursor != null && CommonDataKinds.Email.DATA.equals(cursor.getColumnName(columnIndex))) {
                    // bind the 'data' field for email
                    ((TextView)view).setText(cursor.getString(columnIndex));
                    // Log.v(TAG, "=========== data field: "+  cursor.getString(EMAIL_DATA_COLUMN_INDEX));
					((TextView)view).setVisibility(View.VISIBLE);
                    return true;
                } else if (cursor != null && GROUP_FIELD.equals(cursor.getColumnName(columnIndex))) {
                    /*
                    // bind the 'group' field,  _id column used to bind group
                    long personId = -1;
                    if (mMode==MODE_PICK_MMS_MIXED && DISPLAY_TYPE_ALL_WITH_PHONES != mDisplayType) {
                        //Log.v(TAG, "personId in col:COLUMN_INDEX_MMS_ID/GPP ="+cursor.getLong(COLUMN_INDEX_MMS_ID)+", COLUMN_INDEX_NORM_ID="+ cursor.getLong(COLUMN_INDEX_NORM_ID));
                        //Log.v(TAG, "personId in col:5 ="+cursor.getLong(COLUMN_INDEX_MMS_TIMES_CONTACTED));
                        if(isPhone) {
                            // phone table's colum
                            personId = cursor.getLong(COLUMN_INDEX_MMS_PERSON);
                        } else {
                            // contact_methods table's column
                            personId = cursor.getLong(COLUMN_INDEX_MMS_TIMES_CONTACTED);
                        }
                    } else if (mDisplayType == DISPLAY_TYPE_ALL_WITH_PHONES && mMode==MODE_PICK_MMS_MIXED
                               || mDisplayType == DISPLAY_TYPE_ALL_WITH_PHONES && mMode==MODE_LEGACY_PICK_PHONE
                               || mDisplayType == DISPLAY_TYPE_ALL && mMode==MODE_LEGACY_PICK_PHONE) {
                        // get person_id(4) from phone table
                        personId = cursor.getLong(COLUMN_INDEX_GPP_PERSON);
                    } else {
                        if (mDisplayType == DISPLAY_TYPE_USER_GROUP) {
                            // groupmembership + people + phone joint table.
                            // '_id' is groupmembership's id, should use 'person' instead
                            personId = cursor.getLong(COLUMN_INDEX_GPP_PERSON);
                        } else {
                            // 'person' column
                            personId = cursor.getLong(COLUMN_INDEX_NORM_ID);
                        }
                    }

                    // Log.v(TAG, "=========== personId=" + personId);
                    if(SimUtility.isGroupMember(getContentResolver(), mCGroupId, personId)){
                        ((TextView)view).setText(getString(R.string.type_c_sim));
                    } else if(SimUtility.isGroupMember(getContentResolver(), mGGroupId, personId)){
                        ((TextView)view).setText(getString(R.string.type_g_sim));
                    } else {
                        //Log.v(TAG, "None-SIM group");
                        ((TextView)view).setText(getString(R.string.type_device));
                    }  */
                    // hide the group indicator
                    view.setVisibility(View.GONE);
                    return true;
                }
                return false;
            }
        };
        mPickerAdapter.setViewBinder(viewBinder);
        mPickerAdapter.setFilterQueryProvider(this);
        setListAdapter(mPickerAdapter);

        mQueryHandler = new QueryHandler(this);

        findViewById(R.id.ok).setOnClickListener(this);

        mListView = getListView();

/*
        // get the excluded group to be counted into 'ALL' group
        Cursor cursor = managedQuery(Groups.CONTENT_URI, GROUPS_PROJECTION,
                                     null, null, Groups.DEFAULT_SORT_ORDER);
        while (cursor != null && cursor.moveToNext()) {
            String name = cursor.getString(GROUPS_COLUMN_INDEX_NAME);
            if (Groups.GROUP_ANDROID_STARRED.equals(name)) {
                // "Starred in Android" group
                mVirtualGroupId = cursor.getLong(GROUPS_COLUMN_INDEX_ID);
                Log.v(TAG, "mVirtualGroupId = " + mVirtualGroupId);
                cursor.close();
                break;
            }
        }
*/
        // init globals
        mUnknownName = getResources().getText(android.R.string.unknownName);

        // hide the go_button
        findViewById(R.id.search_go_btn).setVisibility(View.GONE);
        // hide some views for SinglePicker
        if (mSingleMode) {
        	mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        	// no 'done' button
            findViewById(R.id.doneBackground).setVisibility(View.GONE);
        } else {
        	// don't setChoiceMode to MULTIPLE in order to support fastscroll
        	//mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        }

        mJustCreated = true;

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }	
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mJustCreated) {
            // Load the preferences
            /*
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);


            mDisplayOnlyPhones = prefs.getBoolean(Prefs.DISPLAY_ONLY_PHONES,
                    Prefs.DISPLAY_ONLY_PHONES_DEFAULT);
            */
            updateList();
        }

        mJustCreated = false;
    }

    @Override
    protected void onStop() {
        super.onStop();

        setResult(RESULT_MULTIPICKER_SEARCH_ONSTOP);
        // should destroy completely since contacts db may be updating
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Cursor cursor = mPickerAdapter.getCursor();
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    private void startQuery() {
        Uri uri;
        String[] projection = null;
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = MultiplePickActivity.getSortOrder(mMode);
        Log.v(TAG, "Entering MultiPickSearch startQuery()");

        // Cancel any pending queries
        mQueryHandler.cancelOperation(QUERY_TOKEN);

        Log.v(TAG, "mMode="+mMode+", sortOrder: "+sortOrder+", mDisplayType="+mDisplayType+", info="+mDisplayInfo+", mGroupId="+mGroupId);
        // Kick off the new async query
        switch (mMode) {
            case MODE_PICK_EMAIL:
                Log.v(TAG, "PICK-EMAIL case");
                /*
                uri = ContactMethods.CONTENT_URI;  // or ContactMethods.CONTENT_EMAIL_URI
                projection = CONTACT_METHODS_EMAIL_PROJECTION;
                selection = ContactMethodsColumns.KIND+"="+Contacts.KIND_EMAIL;
                mBaseUri4Return = ContactMethods.CONTENT_URI; */
                uri = getUriToQuery();
                projection = getProjectionForQuery();
                selection = getContactSelection();
                // specify linkage
                if (mLinkageIdInc > 0) {
                    selectionArgs = new String[] { java.lang.Long.toString(mLinkageIdInc) };
                }
                if (mLinkageIdExc > 0) {
                	selectionArgs = new String[] { java.lang.Long.toString(mLinkageIdExc) };
                }
                // data table's uri
                mBaseUri4Return = Data.CONTENT_URI;
                break;

            case MODE_PICK_CONTACT:
                Log.v(TAG, "PICK CONTACT case");
                uri = getUriToQuery();
                projection = getProjectionForQuery();
                //selection = getContactSelection();
                selection = mSelection;
                // always use raw_contacts uri even if raw_contacts/group case
                //mBaseUri4Return = uri;
                // Filter out deleted contacts
                selection += " AND " + RawContacts.DELETED + "=0";
                selectionArgs = mSelectionArgs;
                mBaseUri4Return = RawContacts.CONTENT_URI;
                break;

            case MODE_PICK_PHONE:
                Log.v(TAG, "PICK NEW PHONE case");
                uri = getUriToQuery();
                projection = getProjectionForQuery();
                selection = getContactSelection();
                // specify linkage
                if (mLinkageIdInc > 0) {
                    selectionArgs = new String[] { java.lang.Long.toString(mLinkageIdInc) };
                }
                if (mLinkageIdExc > 0) {
                	selectionArgs = new String[] { java.lang.Long.toString(mLinkageIdExc) };
                }
                // data table's uri
                mBaseUri4Return = Data.CONTENT_URI;
                break;

            case MODE_LEGACY_PICK_PHONE:
                Log.v(TAG, "PICK LEGACY PHONE case");
                switch(mDisplayType) {
                    case DISPLAY_TYPE_ALL:
                        uri = Phones.CONTENT_URI;
                        projection = PHONE_ITEM_PROJECTION;
                        break;
                    case DISPLAY_TYPE_ALL_WITH_PHONES:
                        uri = Phones.CONTENT_URI;
                        projection = PHONE_ITEM_PROJECTION;
                        selection = PhonesColumns.ISPRIMARY + "=1";
                        break;
                    case DISPLAY_TYPE_SYSTEM_GROUP:
                    default:  // DISPLAY_TYPE_USER_GROUP
                        uri = Uri.parse(PHONE_GROUPMEMBERSHIP_CONTENT_URI);
                        projection = NORMAL_ITEM_PROJECTION;
                        if (mGroupId <= 0) {
                            selection = GroupMembership.GROUP_ID + "!=" + mVirtualGroupId;
                        } else {
                            selection = GroupMembership.GROUP_ID + "=" + mGroupId;
                        }
                        break;
                }
                mBaseUri4Return = Phones.CONTENT_URI;
                break;

            case MODE_PICK_MMS_MIXED:
                Log.v(TAG, "PICK-MMS_MIXED case, list phone or email");
                /*
                switch(mDisplayType) {
                    case DISPLAY_TYPE_ALL_WITH_PHONES:
                        uri = Phones.CONTENT_URI;
                        projection = PHONE_ITEM_PROJECTION;
                        selection = PhonesColumns.ISPRIMARY + "=1";
                        break;
                    case DISPLAY_TYPE_ALL:
                    case DISPLAY_TYPE_SYSTEM_GROUP:
                    default:  // DISPLAY_TYPE_USER_GROUP
                        uri = Uri.parse(PHONE_GROUPMEMBERSHIP_CONTACTMETHODS_CONTENT_URI);
                        projection = MMS_ITEM_PROJECTION;
                        // pick desired group
                        if (mGroupId <= 0) {
                            selection = GroupMembership.GROUP_ID + "!=" + mVirtualGroupId;
                        } else {
                            selection = GroupMembership.GROUP_ID + "=" + mGroupId;
                        }
                        break;
                }
                mBaseUri4Return = Phones.CONTENT_URI;      // can be ContactMethods.CONTENT_URI
                */
                uri = getUriToQuery();
                projection = getProjectionForQuery();
                //projection = null;
                // don't pick those with 'deleted' column  marked non-zero AND 'in_visible_group' set as zero ???
                //selection = null;
                if (OPTION_EMAIL_ONLY == mCurrentMmsKind) {
                    selection = mSelection;
                } else {
                    if(mDisplayOnlyMobiles){
                        selection = mSelection + CLAUSE_ONLY_MOBILE;
                    } else {
                        selection = mSelection;
                    }
                }
                selectionArgs = mSelectionArgs;
                Log.v(TAG, "selection: "+selection);
                // data table's uri
                mBaseUri4Return = Data.CONTENT_URI;
                break;

            default:
                Log.v(TAG, "PICK LEGACY PERSON case");
                switch(mDisplayType) {
                    case DISPLAY_TYPE_SYSTEM_GROUP:
                        if (mGroupId <= 0) {
                            selection = GroupMembership.GROUP_ID + "!=" + mVirtualGroupId;
                        } else {
                            selection = GroupMembership.GROUP_ID + "=" + mGroupId;
                        }
                        // fall through
                    case DISPLAY_TYPE_ALL:
                        uri = People.CONTENT_URI;
                        projection = NORMAL_ITEM_PROJECTION;
                        break;
                    case DISPLAY_TYPE_ALL_WITH_PHONES:
                        uri = People.CONTENT_URI;
                        projection = NORMAL_ITEM_PROJECTION;
                        selection = People.PRIMARY_PHONE_ID + " IS NOT NULL";
                        break;
                    default:  // DISPLAY_TYPE_USER_GROUP
                        uri = Uri.parse(PEOPLE_GROUPMEMBERSHIP_CONTENT_URI);
                        projection = GROUPMEMBERSHIP_PEOPLE_PHONES_ITEM_PROJECTION;
                        selection = GroupMembership.GROUP_ID + "=" + mGroupId;
                        break;
                }
                // Filter out deleted contacts
                selection += " AND " + RawContacts.DELETED + "=0";
                mBaseUri4Return = People.CONTENT_URI;
                break;
        }
        Log.v(TAG, "uri="+uri+", selection="+selection+", sortorder="+sortOrder+", mBaseUri4Return:"+mBaseUri4Return);
        mQueryHandler.startQuery(QUERY_TOKEN, null,
                                 uri,
                                 projection,
                                 selection,
                                 selectionArgs,
                                 sortOrder);
        Log.v(TAG, "Leaving MultiPickSearch startQuery()");
    }

    private void setEmptyText() {
        TextView empty = (TextView) findViewById(R.id.emptyText);
        // Center the text by default
        //int gravity = Gravity.CENTER;
        int gravity = Gravity.NO_GRAVITY;
        empty.setText(R.string.no_matched_contact);

        /*
        switch (mMode) {
            case MODE_PICK_EMAIL:
                empty.setText(getText(R.string.noContactsWithEmail));
                break;

            case MODE_LEGACY_PICK_PHONE:
                empty.setText(getText(R.string.noContactsWithPhoneNumbers));
                break;

            case MODE_PICK_MMS_MIXED:
                empty.setText(getText(R.string.noContactsWithPhoneEmails));
                break;

            default:
                empty.setText(getText(R.string.noContacts));
                break;
        } */

        empty.setGravity(gravity);
        empty.setVisibility(View.GONE);
        //Log.v(TAG, "setEmptyText(), text="+empty.getText()+", visible="+empty.getVisibility());
        // only show the empty after query and find query result is empty
        //empty.setVisibility(View.GONE);
    }


    private Uri getPeopleFilterUri(String filter) {
        if (!TextUtils.isEmpty(filter)) {
            return Uri.withAppendedPath(People.CONTENT_FILTER_URI, Uri.encode(filter));
        } else {
            return People.CONTENT_URI;
        }
    }

    private Uri getPeopleGroupMembershipFilterUri(String filter) {
        if (!TextUtils.isEmpty(filter)) {
            return Uri.withAppendedPath(Uri.parse(PEOPLE_GROUPMEMBERSHIP_CONTENT_FILTER_URI), Uri.encode(filter));
        } else {
            return Uri.parse(PEOPLE_GROUPMEMBERSHIP_CONTENT_URI);
        }
    }

    private Uri getPhoneFilterUri(String filter) {
        if (!TextUtils.isEmpty(filter)) {
            return Uri.withAppendedPath(Uri.parse(PHONE_CONTENT_FILTER_NAME_URI), Uri.encode(filter));
        } else {
            return Phones.CONTENT_URI;
        }
    }

    private Uri getPhoneGroupMembershipFilterUri(String filter) {
        if (!TextUtils.isEmpty(filter)) {
            return Uri.withAppendedPath(Uri.parse(PHONE_GROUPMEMBERSHIP_CONTENT_FILTER_URI), Uri.encode(filter));
        } else {
            return Uri.parse(PHONE_GROUPMEMBERSHIP_CONTENT_URI);
        }
    }

    private Uri getMmsFilterUri(String filter) {
        if (!TextUtils.isEmpty(filter)) {
            return Uri.withAppendedPath(Uri.parse(PHONE_GROUPMEMBERSHIP_CONTACTMETHODS_CONTENT_FILTER_URI), Uri.encode(filter));
        } else {
            return Uri.parse(PHONE_GROUPMEMBERSHIP_CONTACTMETHODS_CONTENT_URI);
        }
    }

    /*
     * Function from FilterQueryProvider,
     * Note: this function is called in another filter thread
     *       consider race condition with bindView in main thread !
     */
    public Cursor runQuery(CharSequence filter) {
        Uri uri = null;
        String[] projection = null;
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = MultiplePickActivity.getSortOrder(mMode);
        Cursor cursor;

        Log.v(TAG, "========== Entering runQuery() by filter=" + filter.toString());

        // save checked status & reset current curosr
        // sync checked status from current list to checked_list
        syncCheckedStatus(false);
        mChecked = null;
        mBaseUri4Return = null;
        mCurrentGroupUri4Return = null;

        switch (mMode) {
            case MODE_PICK_CONTACT:
                Log.v(TAG, "PICK CONTACT case");
                uri = getContactFilterUri(filter.toString());
                projection = getProjectionForQuery();
                //selection = getContactSelection();
                selection = mSelection;
                /*
                if (mIntentCopyD2C == COPY_CCARD_TO_DEVICE
                    || mIntentCopyD2C == COPY_GCARD_TO_DEVICE
                    || mIntentCopyD2C == COPY_DEVICE_TO_CARD) {
                    mBaseUri4Return = RawContacts.CONTENT_URI;
                } else {
                    //mBaseUri4Return = Contacts.CONTENT_URI;
                    // list by raw_contacts by querying one table only to enhance searching performance
                    mBaseUri4Return = RawContacts.CONTENT_URI;
                } */
                // Filter out deleted contacts
                selection += " AND " + RawContacts.DELETED + "=0";
                selectionArgs = mSelectionArgs;
                mBaseUri4Return = RawContacts.CONTENT_URI;
                Log.v(TAG, "uri: "+uri+", selection: "+selection);
                break;

            case MODE_PICK_PHONE:
                Log.v(TAG, "PICK NEW PHONE case");
                uri = getUriToQuery();
                if (!TextUtils.isEmpty(filter)) {
                    uri = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, Uri.encode(filter.toString()));
                    //uri = Uri.withAppendedPath(Uri.withAppendedPath(ContactsUtils.Linkages.CONTENT_SUMMARY_URI, "name_only/filter"), Uri.encode(filter.toString()));
                    //uri = Uri.withAppendedPath(Uri.withAppendedPath(Phone.CONTENT_URI, "name_only/filter"),
                     //                              Uri.encode(filter.toString()));
                }

                projection = getProjectionForQuery();
                selection = getContactSelection();
                //selection = null;
                // specify linkage
                /*
                if (mLinkageIdInc > 0) {
                    selectionArgs = new String[] { java.lang.Long.toString(mLinkageIdInc) };
                }
                if (mLinkageIdExc > 0) {
                	selectionArgs = new String[] { java.lang.Long.toString(mLinkageIdExc) };
                } */
                // data table's uri
                mBaseUri4Return = Data.CONTENT_URI;
                Log.v(TAG, "uri: "+uri+", selection: "+selection);
                break;

            case MODE_PICK_EMAIL:
                Log.v(TAG, "PICK-EMAIL case");
                uri = getUriToQuery();
                if (!TextUtils.isEmpty(filter)) {
                    //uri = Uri.withAppendedPath(Uri.withAppendedPath(ContactsUtils.Linkages.CONTENT_SUMMARY_URI, "name_only/filter"), Uri.encode(filter.toString()));
                    uri = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, Uri.encode(filter.toString()));
                    //uri = Uri.withAppendedPath(Uri.withAppendedPath(Email.CONTENT_URI, "name_only/filter"),
                    //                               Uri.encode(filter.toString()));
                }
                projection = getProjectionForQuery();
                selection = getContactSelection();
                //selection = null;
                // specify linkage
                /*
                if (mLinkageIdInc > 0) {
                    selectionArgs = new String[] { java.lang.Long.toString(mLinkageIdInc) };
                }
                if (mLinkageIdExc > 0) {
                	selectionArgs = new String[] { java.lang.Long.toString(mLinkageIdExc) };
                } */
                // data table's uri
                mBaseUri4Return = Data.CONTENT_URI;
                Log.v(TAG, "uri: "+uri+", selection: "+selection);
                break;

            case MODE_LEGACY_PICK_PHONE:
                Log.v(TAG, "PICK LEGACY PHONE case");
                switch(mDisplayType) {
                    case DISPLAY_TYPE_ALL:
                        uri = getPhoneFilterUri(filter.toString());
                        projection = NORMAL_ITEM_PROJECTION;
                        break;
                    case DISPLAY_TYPE_ALL_WITH_PHONES:
                        uri = getPhoneFilterUri(filter.toString());
                        projection = NORMAL_ITEM_PROJECTION;
                        selection = PhonesColumns.ISPRIMARY + "=1";
                        break;
                    case DISPLAY_TYPE_SYSTEM_GROUP:
                    default:  // DISPLAY_TYPE_USER_GROUP
                        uri = getPhoneGroupMembershipFilterUri(filter.toString());
                        projection = NORMAL_ITEM_PROJECTION;
                        if (mGroupId <= 0) {
                            selection = GroupMembership.GROUP_ID + "!=" + mVirtualGroupId;
                        } else {
                            selection = GroupMembership.GROUP_ID + "=" + mGroupId;
                        }
                        break;
                }
                mBaseUri4Return = Phones.CONTENT_URI;
                break;

            case MODE_PICK_MMS_MIXED:
                Log.v(TAG, "PICK-MMS_MIXED case");
                /*
                switch(mDisplayType) {
                    case DISPLAY_TYPE_ALL_WITH_PHONES:
                        uri = getPhoneFilterUri(filter.toString());
                        projection = NORMAL_ITEM_PROJECTION;
                        selection = PhonesColumns.ISPRIMARY + "=1";
                        break;
                    case DISPLAY_TYPE_ALL:
                    case DISPLAY_TYPE_SYSTEM_GROUP:
                    default:  // DISPLAY_TYPE_USER_GROUP
                        uri = getMmsFilterUri(filter.toString());
                        projection = MMS_ITEM_PROJECTION;
                        // pick desired group
                        if (mGroupId <= 0) {
                            selection = GroupMembership.GROUP_ID + "!=" + mVirtualGroupId;
                        } else {
                            selection = GroupMembership.GROUP_ID + "=" + mGroupId;
                        }
                        break;
                }
                mBaseUri4Return = Phones.CONTENT_URI;  */
                uri = getUriToQuery();
            	if (OPTION_EMAIL_ONLY == mCurrentMmsKind) {
                    if (!TextUtils.isEmpty(filter)) {
                        uri = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, Uri.encode(filter.toString()));
                        //uri = Uri.withAppendedPath(Uri.withAppendedPath(Email.CONTENT_URI, "name_only/filter"),
                          //                         Uri.encode(filter.toString()));
                    }
            	} else {
            		// use phone's
                    if (!TextUtils.isEmpty(filter)) {
                        uri = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, Uri.encode(filter.toString()));
                        //uri = Uri.withAppendedPath(Uri.withAppendedPath(Phone.CONTENT_URI, "name_only/filter"),
                          //                         Uri.encode(filter.toString()));
                    }
                }
                // data table's uri
                mBaseUri4Return = Data.CONTENT_URI;

                projection = getProjectionForQuery();
                //projection = null;
                // don't pick those with 'deleted' column  marked non-zero AND 'in_visible_group' set as zero ???
                //selection = null;
                if (OPTION_EMAIL_ONLY == mCurrentMmsKind) {
                    selection = mSelection;
                } else {
                    if(mDisplayOnlyMobiles){
                        selection = mSelection + CLAUSE_ONLY_MOBILE;
                    } else {
                        selection = mSelection;
                    }
                }
                selectionArgs = mSelectionArgs;
                Log.v(TAG, "uri: "+uri+", selection: "+selection+", mDisplayOnlyMobiles= "+mDisplayOnlyMobiles);
                break;

            case MODE_LEGACY_PICK_PERSON:
                Log.v(TAG, "PICK LEGACY PEOPLE case");
                switch(mDisplayType) {
                    case DISPLAY_TYPE_SYSTEM_GROUP:
                        if (mGroupId <= 0) {
                            selection = GroupMembership.GROUP_ID + "!=" + mVirtualGroupId;
                        } else {
                            selection = GroupMembership.GROUP_ID + "=" + mGroupId;
                        }
                        // fall through
                    case DISPLAY_TYPE_ALL:
                        uri = getPeopleFilterUri(filter.toString());
                        projection = NORMAL_ITEM_PROJECTION;
                        break;
                    case DISPLAY_TYPE_ALL_WITH_PHONES:
                        uri = getPeopleFilterUri(filter.toString());
                        projection = NORMAL_ITEM_PROJECTION;
                        selection = People.PRIMARY_PHONE_ID + " IS NOT NULL";
                        break;
                    default:  // DISPLAY_TYPE_USER_GROUP
                        uri = getPeopleGroupMembershipFilterUri(filter.toString());
                        projection = GROUPMEMBERSHIP_PEOPLE_PHONES_ITEM_PROJECTION;
                        selection = GroupMembership.GROUP_ID + "=" + mGroupId;
                        break;
                }
                mBaseUri4Return = People.CONTENT_URI;
                break;

            default:
                Log.v(TAG, "DEFAULT mMode case, not supported !");
                throw new UnsupportedOperationException("filtering not allowed in mode " + mMode);
        }

        cursor = managedQuery(uri, projection, selection, selectionArgs, sortOrder);

        if (cursor != null) {
            int count = cursor.getCount();
            // show 'empty' text if no list items, but Only the original thread that
            // created a view hierarchy can touch its views.
            //TextView empty = (TextView) findViewById(R.id.emptyText);
            //Log.v(TAG, "TextView-empty ="+empty+", mGroupId="+mGroupId);
            Log.v(TAG, "cursor count = "+count);
            if (count <= 0) {
                if (mGroupId == 0) {
                    // all list is empty
                    //Log.v(TAG, "set all_emtpy visible, text = "+empty.getText()+", visible="+empty.getVisibility());
                    //Log.v(TAG, "set all_emtpy visible, send mesg to main thread to show empty_text");
                } else {
                    // empty.setVisibility(View.VISIBLE);       can't touch this view !!!
                    //Log.v(TAG, "set emtpy visible");
                }
                mHandler.sendEmptyMessage(SHOW_EMPTY_TEXT_MESG);
            } else {
                Log.v(TAG, "cursor count=" + count +", uri ="+uri+", selection ="+selection+", sortOrder ="+sortOrder);
                //Log.v(TAG, "send mesg to main thread to hide empty_text");
                mHandler.sendEmptyMessage(HIDE_EMPTY_TEXT_MESG);

                // init & sync current cursor
                if (mChecked == null) {
                    mChecked = new boolean[count];
                    mCurrentGroupUri4Return = new Uri[count];
                }

                // build list data for current list
                buildData4Return(cursor, false);

                // sync checked status from checked_list to current list
                syncCheckedStatus(true);

                // empty.setVisibility(View.INVISIBLE);   can't touch this view !!!
                //Log.v(TAG, "before move_cursor, cursor="+cursor);
                //Log.v(TAG, "count="+count+", cursor pos="+cursor.getPosition());
                //cursor.moveToPosition(-1);
                //mPickerAdapter.changeCursor(cursor);

            }
        } else {
            Log.v(TAG, "cursor = null");
        }
        Log.v(TAG, "return from runQuery(), cursor = " + cursor);
        return cursor;
    }

    private void updateList() {
        // sync checked status from current group to checked_list
        //syncCheckedStatus(false);
        mChecked = null;
        mBaseUri4Return = null;
        mCurrentGroupUri4Return = null;
//        mFullListChecked = null;
//        mFullListUri4Return = null;

/*
        // reset 'All' checkbox status and view
        mSelectAll = false;
        ((CheckBox)findViewById(R.id.checkbox)).setChecked(false);
*/

        // close the previous cursor if there is.
        Cursor cursor = mPickerAdapter.getCursor();
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        // Load the preferences since the groups may be changed
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        mDisplayOnlyPhones = prefs.getBoolean(Prefs.DISPLAY_ONLY_PHONES,
                    Prefs.DISPLAY_ONLY_PHONES_DEFAULT);

        mDisplayOnlyMobiles = false; //prefs.getBoolean(Prefs.PREF_DISPLAY_MOBILE_ONLY,true);
        // update the empty text when group switches
        setEmptyText();

        // Calling requery here may cause an ANR, so always do the async query
        startQuery();
   }

   Uri getUriToQuery() {
        switch(mMode) {
            case MODE_DEFAULT:
            case MODE_PICK_CONTACT: {
                if (mLinkageIdExc != 0 ) {
                    return RawContacts.CONTENT_URI;
                } else if (mIntentGroup > 0 || mLinkageIdInc > 0) {
                    // specified group
                    return RawContacts.CONTENT_URI;
                } else {
                    if (mIntentCopyD2C == COPY_CCARD_TO_DEVICE
                        || mIntentCopyD2C == COPY_GCARD_TO_DEVICE
                        || mIntentCopyD2C == COPY_DEVICE_TO_CARD) {
                            return RawContacts.CONTENT_URI;
                    } else {
                        //return Contacts.CONTENT_URI;
                        // list by raw_contacts by querying one table only to enhance searching performance
                        return RawContacts.CONTENT_URI;
                    }
                }
            }
            case MODE_LEGACY_PICK_PERSON: {
                return People.CONTENT_URI;
            }
            case MODE_PICK_PHONE: {
                // normal all phone entries
                return Phone.CONTENT_URI;
            }
            case MODE_LEGACY_PICK_PHONE: {
                return Phones.CONTENT_URI;
            }
            case MODE_PICK_MMS_MIXED: {
                if (OPTION_EMAIL_ONLY == mCurrentMmsKind) {
                    return Email.CONTENT_URI;
                } else {
                    // use phone's
                    return Phone.CONTENT_URI;
                }
            }
            case MODE_PICK_EMAIL: {
                // normal all email entries
                return Email.CONTENT_URI;
            }
            default: {
                return null;
            }
        }
    }

   String[] getProjectionForQuery() {
        switch(mMode) {
            case MODE_DEFAULT:
            case MODE_PICK_CONTACT: {
                if (mIntentCopyD2C == COPY_CCARD_TO_DEVICE
                    || mIntentCopyD2C == COPY_GCARD_TO_DEVICE
                    || mIntentCopyD2C == COPY_DEVICE_TO_CARD) {
                        return RAW_CONTACTS_SUMMARY_PROJECTION;
                } else {
                    //return CONTACTS_SUMMARY_PROJECTION;
                    // list by raw_contacts by querying one table only to enhance searching performance
                    return RAW_CONTACTS_SUMMARY_PROJECTION;
                }
            }
            case MODE_LEGACY_PICK_PERSON: {
                return NORMAL_ITEM_PROJECTION;
            }
            case MODE_PICK_PHONE: {
                return PHONES_PROJECTION;
            }
            case MODE_LEGACY_PICK_PHONE: {
                return PHONE_ITEM_PROJECTION;
            }
            case MODE_PICK_MMS_MIXED: { // set as phone
                if (OPTION_EMAIL_ONLY == mCurrentMmsKind) {
                    return EMAIL_PROJECTION;
                } else {
                    // use phone's
                    return PHONES_PROJECTION;
                }
            }
            case MODE_PICK_EMAIL: {
                return EMAIL_PROJECTION;
            }
        }

        // Default to normal aggregate projection
        return CONTACTS_SUMMARY_PROJECTION;
    }

    /**
     * Return the selection arguments for a default query based on
     * {@link #mDisplayAll} and {@link #mDisplayOnlyPhones} flags.
     */
    private String getContactSelection() {
        String org = null;
        if (!TextUtils.isEmpty(mPassedInSelection)) {
            org = "(" + mPassedInSelection + ")";
        }

        if (mIntentCopyD2C != COPY_CCARD_TO_DEVICE
            && mIntentCopyD2C != COPY_GCARD_TO_DEVICE) {
            // group filter enabled
            if (TextUtils.isEmpty(org)) {
                org = "";
            } else {
                org += " AND ";
            }
            if (mDisplayOnlyPhones) {
                return org + CLAUSE_ONLY_VISIBLE + " AND " + CLAUSE_ONLY_PHONES;
            } else {
                return org + CLAUSE_ONLY_VISIBLE;
            }
        }
        return org;

    }

    private Uri getContactFilterUri(String filter) {
        if (mLinkageIdExc != 0 ) {
            if (!TextUtils.isEmpty(filter)) {
                return Uri.withAppendedPath(Uri.withAppendedPath(RawContacts.CONTENT_URI, "filter"), Uri.encode(filter));
            } else {
                return RawContacts.CONTENT_URI;
            }
        } else if (mIntentGroup > 0 || mLinkageIdInc > 0) {
            // specified group
            if (!TextUtils.isEmpty(filter)) {
                return Uri.withAppendedPath(Uri.withAppendedPath(RawContacts.CONTENT_URI, "filter"), Uri.encode(filter));
            } else {
                return RawContacts.CONTENT_URI;
            }
        } else {
            if (mIntentCopyD2C == COPY_CCARD_TO_DEVICE
                || mIntentCopyD2C == COPY_GCARD_TO_DEVICE
                || mIntentCopyD2C == COPY_DEVICE_TO_CARD) {
                if (!TextUtils.isEmpty(filter)) {
                    // moto's customized filter uri
                    return Uri.withAppendedPath(Uri.withAppendedPath(RawContacts.CONTENT_URI, "filter"), Uri.encode(filter));
                } else {
                    return RawContacts.CONTENT_URI;
                }
            } else {
                // list by raw_contacts by querying one table only to enhance searching performance
                if (!TextUtils.isEmpty(filter)) {
                    //return Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(filter));
                    return Uri.withAppendedPath(Uri.withAppendedPath(RawContacts.CONTENT_URI, "filter"), Uri.encode(filter));
                } else {
                    //return Contacts.CONTENT_URI;
                    return RawContacts.CONTENT_URI;
                }
            }
        }
    }

    public void checkBoxClicked(View view){
        //Log.v(TAG, "onListItemClick(), l="+l+", v="+v+", pos="+position+", id="+id);
        final ListView l = getListView();
        if (l != null && view != null) {
            int position = l.getPositionForView((CheckBox)view);
            // if single picker, just return shortly
            if (mSingleMode && mCurrentGroupUri4Return != null && position < mCurrentGroupUri4Return.length) {
                Intent intent = new Intent();
                Uri uri = mCurrentGroupUri4Return[position];
                Log.v(TAG, "return from SINGLE_PICKER with uri: "+uri);
                setResult(RESULT_OK, intent.setData(uri));
                finish();
            } else if (mChecked != null && position < mChecked.length && mCurrentGroupUri4Return != null && position < mCurrentGroupUri4Return.length) {
                //boolean isChecked = l.isItemChecked(position);
                // toggle
                boolean isChecked = !(mChecked[position]);
                // record checkmark for specified position
                mChecked[position] = isChecked;
                Log.v(TAG, "mChecked[pos]=" + mChecked[position]);

                // set the checkmark view
                //if (v != null && v.findViewById(R.id.multi_picker_list_item_name) != null ) {
                   // ((CheckBox)v.findViewById(R.id.checkitembox)).setChecked(isChecked);
                //}

                // count it into the whole selected items
                if (isChecked && mCurrentGroupUri4Return != null && !mFullListUri4Return.contains(mCurrentGroupUri4Return[position])) {
                    mFullListUri4Return.add(mCurrentGroupUri4Return[position]);
                    Log.v(TAG, "added into F");
                } else if (!isChecked && mCurrentGroupUri4Return != null && mFullListUri4Return.contains(mCurrentGroupUri4Return[position])) {
                    mFullListUri4Return.remove(mCurrentGroupUri4Return[position]);
                    Log.v(TAG, "removed from F");
                }
            }
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }    
}
