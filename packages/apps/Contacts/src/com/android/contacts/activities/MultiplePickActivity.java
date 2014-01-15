package com.android.contacts.activities;

//import com.google.android.googlelogin.GoogleLoginServiceConstants;

//import com.android.contacts.ui.ContactsPreferencesActivity;
//import com.android.contacts.ui.ContactsPreferencesActivity.Prefs;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.ScrollingTabWidget;
import com.android.contacts.ScrollingTabWidget.OnTabSelectionChangedListener;
import com.android.contacts.list.AccountFilterActivity;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactsSectionIndexer;
import com.android.contacts.model.HardCodedSources;
import com.android.contacts.preference.ContactsPreferences;
import com.android.contacts.SimUtility;
import com.android.contacts.widget.InteractiveTitleBarView;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.content.AsyncQueryHandler;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
//import android.provider.Gmail;
import android.provider.Contacts.Settings;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.ContactMethodsColumns;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.provider.Contacts.PhonesColumns;
import android.provider.Contacts.GroupMembership;
import android.provider.ContactsContract.ContactCounts;
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
import android.view.View;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import android.widget.Button;
import android.provider.Contacts.People;
import android.net.Uri;
import android.util.SparseArray;
import android.util.Log;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import java.util.Iterator;
import android.app.AlertDialog;
import android.view.Window;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.SectionIndexer;
import android.view.ViewGroup;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.lang.ref.WeakReference;
import java.lang.Long;
import java.util.HashMap;

public final class MultiplePickActivity extends ListActivity implements View.OnClickListener,
    OnTabSelectionChangedListener {

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

    public static final int MENU_DISPLAY_GROUP = 1;
    public static final int MENU_PHONE_ONLY = 2;
    public static final int MENU_EMAIL_ONLY = 3;
    public static final int MENU_MOBILE_ONLY = 4;
    public static final int MENU_ALL_NUMBERS = 5;

    public static final long OPTION_PHONE_ONLY = 11;
    public static final long OPTION_EMAIL_ONLY = 12;
    public static final long COPY_DEVICE_TO_CARD = 13;
    public static final long COPY_CCARD_TO_DEVICE = 14;
    public static final long COPY_GCARD_TO_DEVICE = 15;

    private static final String TAG = "MultiplePicker";
    private static final String NAME_FIELD = "name";
    private static final String TYPE_FIELD = "type";
    private static final String NUMBER_FIELD = "number";
    private static final String GROUP_FIELD = "_id";    // used for group display
    private static final String DATA_FIELD = "data";
    private static final String PEOPLE_GROUPMEMBERSHIP_CONTENT_URI = "content://contacts/people/groupmembership";
    private static final String PHONE_GROUPMEMBERSHIP_CONTENT_URI = "content://contacts/phones/groupmembership/people";
    private static final String PHONE_GROUPMEMBERSHIP_CONTACTMETHODS_CONTENT_URI = "content://contacts/phones/groupmembership/people/contactmethods";
    private static final String MULTI_PICKER_INTENT_EXTRA_FIXED_GROUPID = "com.android.contacts.MultiPickerFixedGroupId";
    private static final String MULTI_PICKER_INTENT_EXTRA_COPY_DEVICE_TO_CARD = "com.android.contacts.MultiPickerCopyD2C";
    private static final String MULTI_PICKER_INTENT_EXTRA_FULL_LIST_URI = "com.android.contacts.MultiPickerFullListUri";
    private static final String MULTI_PICKER_INTENT_EXTRA_FULL_LIST_CHECKED_STATUS = "com.android.contacts.MultiPickerFullListCheckedStatus";
    private static final String MULTI_PICKER_INTENT_EXTRA_MMS_KIND = "com.android.contacts.MultiPickerMmsKind";
    private static final String MULTI_PICKER_INTENT_EXTRA_LINKAGE_INCLUDE = "com.android.contacts.MultiPickerIncludeLinkageId";
    private static final String MULTI_PICKER_INTENT_EXTRA_LINKAGE_EXCLUDE = "com.android.contacts.MultiPickerExcludeLinkageId";
    private static final String MULTI_PICKER_INTENT_EXTRA_WRITABLE_ONLY = "com.android.contacts.MultiPickerWritableOnly";
    private static final String MULTI_PICKER_INTENT_EXTRA_UNRESTRICTED_ONLY = "com.android.contacts.MultiPickerUnRestrictedOnly";
    private static final String MULTI_PICKER_INTENT_EXTRA_SELECTION = "com.android.contacts.MultiPickerSelection";
    private static final String MULTI_PICKER_INTENT_EXTRA_SELECTION_ARG = "com.android.contacts.MultiPickerSelectionArg";    
    
    private static final String MULTI_PICKER_INTENT_EXTRA_ACCOUNT_NAME = "com.android.contacts.MultiPickerAccountName";
    private static final String MULTI_PICKER_INTENT_EXTRA_ACCOUNT_TYPE = "com.android.contacts.MultiPickerAccountType";
    
    private static final int MULTI_PICK_PHONE_EMAIL_ONLY_REQUEST = 0;
    private static final int MULTI_PICK_MIXED_PHONEEMAIL_REQUEST = 5;


    private static final int QUERY_TOKEN = 63;
    private static final int UPDATE_TOKEN = 64;
    private static final int RESULT_MULTIPICKER_SEARCH_ONSTOP = RESULT_FIRST_USER + 1;

    private static final int SUBACTIVITY_LOAD_SIM = 1;
    private static final int SUBACTIVITY_SEARCH = 2;
    private static final int SUBACTIVITY_DISPLAY_GROUP = 3;
    private static final int SUBACTIVITY_ACCOUNT_FILTER = 4;

    private static final String CLAUSE_ONLY_VISIBLE = Contacts.IN_VISIBLE_GROUP + "=1";
        
    private static final String CLAUSE_ONLY_PHONES = Contacts.HAS_PHONE_NUMBER + "=1";
    private static final String CLAUSE_ONLY_MOBILE = "(" + Data.DATA2 + "='" + Phone.TYPE_MOBILE + "' OR " + Data.DATA2 + "='" + Phone.TYPE_WORK_MOBILE + "' OR " + Data.DATA2 + "='" + Phone.TYPE_MMS + "')";

    private static final String CLAUSE_ONLY_DEVICES = RawContacts.ACCOUNT_TYPE + "!=\"" +HardCodedSources.ACCOUNT_TYPE_CARD + "\"";
    private static final String CLAUSE_ONLY_CARDS = RawContacts.ACCOUNT_TYPE + "=\"" +HardCodedSources.ACCOUNT_TYPE_CARD + "\"";
//    private static final String CLAUSE_ONLY_UNRESTRICTED = RawContacts.IS_RESTRICTED + "=0";
    private static final String CLAUSE_ONLY_UNRESTRICTED = "1";

    //????check
	private static final String CLAUSE_NOT_CARDS = RawContacts.ACCOUNT_TYPE + "!=\"" +HardCodedSources.ACCOUNT_TYPE_CARD + "\"";
    private static final String CLAUSE_SPECIFIED_GROUP = Data.MIMETYPE + "='"
                                    + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'" + " AND "
                                    + ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + "=";

    // query raw_contact table not in specified group
    private static final String CLAUSE_EXCLUDE_SPECIFIED_GROUP = Data.MIMETYPE + "='"
                                    + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'" + " AND "
                                    + ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + "!=";


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
        //Presence.PRESENCE_STATUS, //4  old
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
        Phone.TYPE, //1
        Phone.LABEL, //2
        Phone.NUMBER, //3
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
        Email.TYPE, //1
        Email.LABEL, //2
        Email.DATA, //3
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

    MultiplePickAdapter mPickerAdapter;
    private QueryHandler mQueryHandler;
    // check mark status for current group
    boolean[] mChecked = null;
    // uris of current group
    Uri[] mCurrentGroupUri4Return = null;
    // common uri portion
    Uri mBaseUri4Return = null;
//    boolean mSetCheckMark = false;
    boolean mSelectAll;
    boolean mIsSubActivityLaunched = false;
    boolean mSearchViewExitOnStop = false;
    boolean mIsSimReady = true;
    // for group dialog
    private ArrayList<ContactsUtils.GroupInfo> mGroupList = new ArrayList<ContactsUtils.GroupInfo>();

    // uris of the checked list
    //Uri[] mFullListUri4Return = null;
    ArrayList<Uri> mFullListUri4Return = new ArrayList<Uri>();
    int mMode = MODE_DEFAULT;
    String mSelection = null;
    String[] mSelectionArgs = null;
    long mIntentGroup = 0;
//    long mCGroupId = 0;
//    long mGGroupId = 0;
    long mVirtualGroupId = 0;
    Context mDialogContext = null;

    private ContactListFilter mFilter;

    private int mDisplayGroupOriginalSelection = 0;
    // The current display group
    private String mDisplayInfo;
    private int mDisplayType;
    private long mGroupId = 0;
    // The current list of display groups, during selection from menu
    private CharSequence[] mDisplayGroups;
    // The groups' ids
    private Long[] mGroupIds;
    // If true position 2 in mDisplayGroups is the MyContacts group
    private boolean mDisplayGroupsIncludesMyContacts = false;

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
    private int mListItemClickedCount = 0;
    private String mPassedInSelection = null;
    private long mIntentCopyD2C = 0;
    private boolean mWritableOnly = false;
    private boolean mUnRestrictedOnly = false;
    private boolean mSynedSearchReturn = false;

    private SparseArray<Long> mTabRawContactIdMap;
    protected ScrollingTabWidget mTabWidget;
    protected LayoutInflater mInflater;

    public static final int MMS_LIMIT_SIZE = 200;
    private boolean mHasLimitSize = false;
    private int mLimitSize = MMS_LIMIT_SIZE;
			// Gesture support
    private static final int GESTURE_MIN_DISTANCE = 160;
    private static final int GESTURE_SLOPE_THRESHOLD = 2;
    private static final int INVALID = -1;
    private static final int PREV = 1;
    private static final int NEXT = 2;
	private GestureDetector mSlideshowGestureDetector = null;
	private SlideGestureListener mSlideGestureListener = null;
	private int mDirection = INVALID;
	
	/*2013-1-8, add by amt_sunzhao for SWITCHUITWO-345 */ 
	private Uri mObserverUri = null;
	/*2013-1-8, add end*/ 

    private static final class QueryHandler extends AsyncQueryHandler {
        private final WeakReference<MultiplePickActivity> mActivity;

        public QueryHandler(Context context) {
            super(context.getContentResolver());
            mActivity = new WeakReference<MultiplePickActivity>(
                    (MultiplePickActivity) context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            Log.v(TAG, "onQueryComplete()");
            final MultiplePickActivity activity = mActivity.get();
            if (activity != null && !activity.isFinishing()) {
                final MultiplePickAdapter pickerAdapter = activity.mPickerAdapter;

                TextView empty = (TextView) activity.findViewById(R.id.emptyText);
                if (cursor != null && cursor.getCount() > 0) {
                    empty.setVisibility(View.GONE);
                    /*2013-1-9, add by amt_sunzhao for SWITCHUITWO-463 */ 
                    activity.findViewById(R.id.checkboxContainer).setEnabled(true);
                    /*2013-1-9, add end*/ 
                    /*2013-1-8, add by amt_sunzhao for SWITCHUITWO-345 */ 
                    ContactListFilter filter = activity.mFilter;
                    String accountType = null;
                    if(null != filter) {
                    	accountType = filter.accountType;
                    }
                    activity.findViewById(R.id.list_header_groups).setEnabled(!HardCodedSources.ACCOUNT_TYPE_CARD.equals(accountType));
                    activity.findViewById(R.id.list_header_search).setEnabled(true);
                    /*2013-1-8, add end*/ 
                    activity.findViewById(R.id.ok).setEnabled(true);
                	activity.findViewById(R.id.preview).setEnabled(true);
                } else if (activity.mFullListUri4Return.size() > 0){
                	/*2013-1-9, add by amt_sunzhao for SWITCHUITWO-463 */ 
                    activity.findViewById(R.id.checkboxContainer).setEnabled(false);
                    /*2013-1-9, add end*/ 
                    activity.findViewById(R.id.list_header_groups).setEnabled(false);
                    /*2013-1-8, add by amt_sunzhao for SWITCHUITWO-345 */ 
                    activity.findViewById(R.id.list_header_search).setEnabled(false);
                    /*2013-1-8, add end*/ 
                    activity.findViewById(R.id.ok).setEnabled(true);
                	activity.findViewById(R.id.preview).setEnabled(true);
                } else {
                    empty.setVisibility(View.VISIBLE);
                    /*2013-1-9, add by amt_sunzhao for SWITCHUITWO-463 */ 
                    activity.findViewById(R.id.checkboxContainer).setEnabled(false);
                    /*2013-1-9, add end*/ 
                    activity.findViewById(R.id.list_header_groups).setEnabled(false);
                    /*2013-1-8, add by amt_sunzhao for SWITCHUITWO-345 */ 
                    activity.findViewById(R.id.list_header_search).setEnabled(false);
                    /*2013-1-8, add end*/ 
                    activity.findViewById(R.id.ok).setEnabled(false);
                	activity.findViewById(R.id.preview).setEnabled(false);
                }
                
                if (cursor != null) {
                    int count = cursor.getCount();
                    boolean changed = true;
                    /*
                    int colcnt = cursor.getColumnCount();

                    String [] colname = cursor.getColumnNames();
                    Log.v(TAG, "IAN, record count=" + count + ", columncount=" + colcnt +", mGroupId=" +activity.mGroupId);
                    for(int i=0; i<colcnt;i++) {
                        Log.v(TAG, "IAN, column[" + i + "] = " + colname[i] );
                    } */

                    // show 'empty' text if no list items
                    // TextView empty = (TextView) activity.findViewById(R.id.emptyText);
                    if (count <= 0) {
                        if (activity.mGroupId == 0) {
                        	// all list is empty
                        	Log.v(TAG, "set all_emtpy visible");
                        } else {
                            //empty.setVisibility(View.VISIBLE);
                            Log.v(TAG, "set emtpy visible, text = " + empty.getText());
                        }
                        /*2013-1-14, add by amt_sunzhao for SWITCHUITWO-479 */ 
                        ((CheckBox)activity.findViewById(R.id.checkbox)).setChecked(false);
                        /*2013-1-14, add end*/ 
                    } else {
                    	Log.v(TAG, "count > 0 : " + count+ ", re-init/sync globals");
                    	// init checked_list data
                    	/*if (activity.mFullListChecked == null) {
                    		Log.v(TAG, "re-init full_list group globals with count = "+count);
                    		activity.mFullListChecked = new boolean[count];
                    		activity.mFullListUri4Return = new Uri[count];

                    		// build full_list uri for returning
                            buildData4Return(cursor, true);
                    	}*/
                    	// init list data of current group
                    	if (activity.mChecked == null || (activity.mChecked != null && count != activity.mChecked.length)) {
                    		Log.v(TAG, "re-init current group globals: mChecked &  mCurrentGroupUri4Return with count = "+count);
                	        activity.mChecked = new boolean[count];
                	        activity.mCurrentGroupUri4Return = new Uri[count];
                	        changed = true;

                    	    // build list data for current group
                    	    buildData4Return(cursor, false);

                    	    // sync checked status from full_list to current group
                    	    activity.syncCheckedStatus(true, activity.mCurrentGroupUri4Return, activity.mChecked);

                    	    //empty.setVisibility(View.INVISIBLE);
                    	    //update the selected count info
                    	    Button done = (Button) activity.findViewById(R.id.ok);
                    	    if (done != null) {
				                //int memberCount = ContactsUtils.getNoLinkageMemberCount(activity.mFullListUri4Return);
                                int memberCount = activity.mFullListUri4Return.size() + activity.mListItemClickedCount;
                    	    	CharSequence newText = activity.getString(R.string.menu_done)  + " (" + memberCount + ")";
			                    Log.v(TAG, "done : "+ newText +", mListItemClickedCount=" + activity.mListItemClickedCount);

			                    done.setText(newText);
			                    if (memberCount > 0) {
			                    	done.setEnabled(true);
			                    	activity.findViewById(R.id.preview).setEnabled(true);
                                } else {
                                	done.setEnabled(false);
                                	activity.findViewById(R.id.preview).setEnabled(false);
                                }
                    	    }

                            // update "All" checkbox
                            int k = 0;
                            for (int i=0; i < count; i++) {
                            	if (i < activity.mChecked.length) {
                            	    if (activity.mChecked[i]) k++;
                                }
                            }
                            // check if all are checked
                            if (k == count) {
                            	activity.mSelectAll = true;
                            	((CheckBox)activity.findViewById(R.id.checkbox)).setChecked(true);
                            } else {
                            	activity.mSelectAll = false;
                            	((CheckBox)activity.findViewById(R.id.checkbox)).setChecked(false);
                            }
                        } else {
                        	changed = false;
                            Log.v(TAG, "no change of List Items, no refresh !");
                        }
                    }

                    if (changed) {
                        cursor.moveToPosition(-1);
                        pickerAdapter.changeCursor(cursor);
                    }
                } else {
                	Log.v(TAG, "cursor = "+ cursor);
                }
            } else {
                cursor.close();
            }
        }

        private void buildData4Return(Cursor cursor, boolean isFullList) {
            final MultiplePickActivity activity = mActivity.get();
            //Log.v(TAG, "buildData4Return(), cursor="+cursor+", activity="+activity+", isFullList="+isFullList);
            if (activity != null && !activity.isFinishing()) {
            	//int count = cursor.getCount();
            	int pos = 0;
            	long id = 0;

                // init the cursor postion
                cursor.moveToPosition(-1);
                //Log.v(TAG, "count="+count+", cursor pos="+cursor.getPosition()+", mCurrentGroupUri4Return="+activity.mCurrentGroupUri4Return);
                while (cursor.moveToNext()) {
                	if (activity.mMode == MODE_LEGACY_PICK_PERSON) {
                		if (activity.mDisplayType == DISPLAY_TYPE_USER_GROUP) {
                			// groupmembership + people + phone joint table.
                			// '_id' is groupmembership's id, should use 'person' instead
                			id = cursor.getLong(COLUMN_INDEX_GPP_PERSON);
                		} else {
                			// people table
                			id = cursor.getLong(COLUMN_INDEX_NORM_ID);
                		}
                	} else if (activity.mMode == MODE_PICK_CONTACT) {
                    	// new contact provider, same for RAW_SUMMARY_NAME_COLUMN_INDEX
                	    id = cursor.getLong(SUMMARY_ID_COLUMN_INDEX);
                    } else if (activity.mMode == MODE_LEGACY_PICK_PHONE) {
                    	// legacy phones table
                	    id = cursor.getLong(COLUMN_INDEX_NORM_ID);
                    } else if (activity.mMode == MODE_PICK_PHONE) {
                    	// new phone provider
                	    id = cursor.getLong(PHONE_ID_COLUMN_INDEX);
                    } else if (activity.mMode == MODE_PICK_EMAIL) {
                    	// new email provider
                	    id = cursor.getLong(EMAIL_ID_COLUMN_INDEX);
                    } else if (activity.mMode == MODE_PICK_MMS_MIXED) {
                    	/*
                    	if (activity.mDisplayType == DISPLAY_TYPE_ALL_WITH_PHONES) {
                    		// phones table
                    	    activity.mBaseUri4Return = Phones.CONTENT_URI;
                	        id = cursor.getLong(COLUMN_INDEX_NORM_ID);
                    	} else {
                    		// mixed table
                    	    boolean isPhone = false;
                    	    isPhone = ! cursor.isNull(COLUMN_INDEX_MMS_PERSON);
                    	    if (isPhone) {
                    	        // phones table
                    	    	activity.mBaseUri4Return = Phones.CONTENT_URI;
                	            id = cursor.getLong(COLUMN_INDEX_MMS_ID);
                	        } else {
                    	        // contact_methods table _id=[8]
                	        	activity.mBaseUri4Return = ContactMethods.CONTENT_URI;
                	            id = cursor.getLong(COLUMN_INDEX_MMS_ID);
                	        }
                	    } */
            	        if (OPTION_EMAIL_ONLY == activity.mCurrentMmsKind) {
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
                		// full list
                		Log.v(TAG, "do nothing for checked_list !");
                		//Log.v(TAG, "FullList: mFullListUri4Return["+pos+"]="+activity.mFullListUri4Return[pos]);
                	} else {
                		// current group, update whenever switch group
                	    activity.mCurrentGroupUri4Return[pos] = Uri.withAppendedPath(activity.mBaseUri4Return, java.lang.Long.toString(id));
                		//Log.v(TAG, "CurList: mCurrentGroupUri4Return["+pos+"]="+activity.mCurrentGroupUri4Return[pos]);
                    }
                	pos++;
                }

            } else {
                cursor.close();
            }
        }
    }

    private void syncCheckedStatus(boolean fromFullToCurrent, Uri [] currentList, boolean [] currentListCheckStatus) {
    	//long start = System.currentTimeMillis();
        Log.v(TAG, "syncCheckedStatus, direction: " + (fromFullToCurrent ? "Full->Current" : "Current->Full"));
        if ((fromFullToCurrent && mFullListUri4Return != null && currentList != null)
            ||(!fromFullToCurrent && currentList != null)) {
        	int fullLen = 0;
        	if (mFullListUri4Return != null) {
        		fullLen = mFullListUri4Return.size();
        	}
        	int curLen = currentList.length;
        	Log.v(TAG, "fullLen = " + fullLen + ", curLen = " + curLen);
        	int i=0, j=0;
        	boolean found = false;

        	if (fullLen > 0) {
        		if (fromFullToCurrent) {
        			// update status from F to C
        	        for (j = 0; j < curLen; j++) {
        	        	if (mFullListUri4Return.contains(currentList[j])) {
        	        		//Log.v(TAG, "F-C matched, curlen="+curLen+", fullLen="+fullLen+", curChecked="+mChecked[j]+", fullchecked="+mFullListChecked[i]+",i="+i+",j="+j);
        	        		// found
        	        	    currentListCheckStatus[j] = true;
        	        	}
        	        }
        	    } else {
        	        // update status from C to F
        	        for (i = 0; i < curLen; i++) {
        	        	found = false;

        	        	if (mFullListUri4Return.contains(currentList[i])) {
        	        		if (!currentListCheckStatus[i]) {
        	        			// un-checked in current list, need remove it from checked list
        	        		    mFullListUri4Return.remove(currentList[i]);
        	        		}
        	        		found = true;
        	        	}

        	        	if (!found && currentListCheckStatus[i]) {
        	        		// checked but not yet in full list, need append it
        	        		mFullListUri4Return.add(currentList[i]);
        	        	}
        	        }
        	    }
            } else {
            	// empty checked list, append C->F
            	if (!fromFullToCurrent) {
            		for (i = 0; i < curLen; i++) {
            			if (!mFullListUri4Return.contains(currentList[i])) {
	            			if (currentListCheckStatus[i]) {
	        			        mFullListUri4Return.add(currentList[i]);
	        			    }
            			}
            		}
            	}
            }
        } else {
        	Log.v(TAG, "do nothing !");
        }
        //long end = System.currentTimeMillis();
        //Log.v(TAG, "Leaving sync(), elapse time: "+(end-start));
        Log.v(TAG, "Leaving sync(), mFullListUri4Return size: "+ mFullListUri4Return.size());
    }

    /**
     * Handles clicks on the list items
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	if (mChecked != null && l != null) {
            boolean isChecked = mChecked[position];                        
    	    // update checked item count
            if (!isChecked) {
            	Log.v(TAG, "mListItemClickedCount ++");
            	mListItemClickedCount ++;
                mChecked[position] = true;
				((CheckBox)v.findViewById(R.id.checkitembox)).setChecked(true);
            } else {
            	Log.v(TAG, "mListItemClickedCount --");
            	mListItemClickedCount --;
				mChecked[position] = false;
				((CheckBox)v.findViewById(R.id.checkitembox)).setChecked(false);
            }

            //mChecked[position] = isChecked;
            Log.v(TAG, "onListItemClick(), checked["+position+"]="+isChecked+", clicked_cnt="+mListItemClickedCount+", F_size="+mFullListUri4Return.size());
            //update the selected count info
            Button done = (Button) findViewById(R.id.ok);
            if (done != null) {
		        //int memberCount = ContactsUtils.getNoLinkageMemberCount(mFullListUri4Return);// + mListItemClickedCount;
		        int memberCount = mFullListUri4Return.size() + mListItemClickedCount;
            	CharSequence newText = getString(R.string.menu_done) + " (" + memberCount + ")";
                //Log.v(TAG, "done button text: "+ newText);
	            done.setText(newText);
                if (memberCount > 0 ) {
                	  done.setEnabled(true);
                    findViewById(R.id.preview).setEnabled(true);
                } else {
                	  done.setEnabled(false);
                    findViewById(R.id.preview).setEnabled(false);
	              }
            }

            // update "All" checkbox
            int k = 0;
            int count = l.getCount();
            for (int i=0; i < count; i++) {
            	if (i < mChecked.length) {
            	    if (mChecked[i]) k++;
            	}
            }
            // check if all are checked
            if (k == count) {
            	//Log.v(TAG, "check 'ALL'");
            	mSelectAll = true;
            	((CheckBox)findViewById(R.id.checkbox)).setChecked(true);
            } else {
            	//Log.v(TAG, "UN-check 'ALL'");
            	mSelectAll = false;
            	((CheckBox)findViewById(R.id.checkbox)).setChecked(false);
            }

            if (!isChecked) {
                // only warn once when reach the upper limit
                if (canAddToList(mFullListUri4Return.size() + mListItemClickedCount) == 0) {
       	            createLimitDialog().show();
                }
            }
        }
    }

    /**
     * Handles clicks on the 'All' checkbox and 'Done' buttons
     */
    public void onClick(View view) {
        switch (view.getId()) {
        	/*
            case R.id.cancel: {
                setResult(RESULT_CANCELED);
                Log.v(TAG, "Leaving onClick_CANCEL()");
                finish();
                break;
            } */

        	case R.id.list_header_groups: {
        		createGroupSelectDialog().show();
        		break;
        	}

            case R.id.list_header_search: {
            	Intent mpickIntent;
            	String mimeType = "";
            	int count = 0;
            	int i = 0;
                Log.v(TAG, "SEARCH textview clicked");
                // launch multi-picker-search sub-activity if the current list is not empty !
                //if (mFullListUri4Return != null && mFullListUri4Return.length > 0) {
                if (mCurrentGroupUri4Return != null && mCurrentGroupUri4Return.length > 0) {
                	// save current checked status to checked list
                	syncCheckedStatus(false, mCurrentGroupUri4Return, mChecked);

                    if (MODE_LEGACY_PICK_PERSON == mMode) {
                        mimeType = "vnd.android.cursor.dir/person";
                    } else if (MODE_LEGACY_PICK_PHONE == mMode) {
                    	mimeType = "vnd.android.cursor.dir/phone";
                    } else if (MODE_PICK_EMAIL == mMode) {
                    	mimeType = MultiplePickActivity.EMAIL_TYPE;
                    } else if (MODE_PICK_MMS_MIXED == mMode) {
                    	mimeType = MultiplePickActivity.MMS_TYPE;
                    } else if (MODE_PICK_CONTACT == mMode) {
                    	mimeType = Contacts.CONTENT_TYPE;
                    } else if (MODE_PICK_PHONE == mMode) {
                    	mimeType = Phone.CONTENT_TYPE;
                    }

                    mpickIntent = new Intent("com.motorola.action.PICK_MULTIPLE_SEARCH");
                    //mpickIntent.putExtra(UI.TITLE_EXTRA_KEY, "Select multiple contacts");
                    mpickIntent.setType(mimeType);
                    // for valid fixed group, i.e. simple mult-picker layout
                    if (mIntentGroup > 0) {
                        mpickIntent.putExtra(MULTI_PICKER_INTENT_EXTRA_FIXED_GROUPID, mIntentGroup);
                    }

                    // send checked_list uris
                    count = mFullListUri4Return.size();
                    String [] s = new String [count];
                    for (i = 0; i < count; i++) {
                    	s[i] = mFullListUri4Return.get(i).toString();
                        mpickIntent.putExtra(MULTI_PICKER_INTENT_EXTRA_FULL_LIST_URI, s);
                    }

                    // send checked status of the full_list
                    //mpickIntent.putExtra(MULTI_PICKER_INTENT_EXTRA_FULL_LIST_CHECKED_STATUS, mFullListChecked);
                    // send copy_to_device/card parameter
                    mpickIntent.putExtra(MULTI_PICKER_INTENT_EXTRA_COPY_DEVICE_TO_CARD, mIntentCopyD2C);

                    if (MODE_PICK_MMS_MIXED == mMode) {
                    	// send mms kind (phone or email) info
                        mpickIntent.putExtra(MULTI_PICKER_INTENT_EXTRA_MMS_KIND, mCurrentMmsKind);
                    }
                    // pass linkage info
                    if (mLinkageIdExc != 0) {
                        mpickIntent.putExtra(MULTI_PICKER_INTENT_EXTRA_LINKAGE_EXCLUDE, mLinkageIdExc);
                    } else if (mExcludedIds != null) {
                        mpickIntent.putExtra("com.android.contacts.MultiPickerExcludeMemberIds", mExcludedIds);
                    }
                    if (mLinkageIdInc > 0) {
                    	mpickIntent.putExtra(MULTI_PICKER_INTENT_EXTRA_LINKAGE_INCLUDE, mLinkageIdInc);
                    }
                    if (mWritableOnly) {
                        mpickIntent.putExtra(MULTI_PICKER_INTENT_EXTRA_WRITABLE_ONLY, mWritableOnly);
                    }
                    if (mUnRestrictedOnly) {
                        mpickIntent.putExtra(MULTI_PICKER_INTENT_EXTRA_UNRESTRICTED_ONLY, mUnRestrictedOnly);                    	
                    }

                    if (mAccountName != null) {
                        mpickIntent.putExtra(MULTI_PICKER_INTENT_EXTRA_ACCOUNT_NAME, mAccountName);
                    }
                    if (mAccountType != null) {
                        mpickIntent.putExtra(MULTI_PICKER_INTENT_EXTRA_ACCOUNT_TYPE, mAccountType);
                    }

                    if (!TextUtils.isEmpty(mSelection)) {
                        mpickIntent.putExtra(MULTI_PICKER_INTENT_EXTRA_SELECTION, mSelection);
                        mpickIntent.putExtra(MULTI_PICKER_INTENT_EXTRA_SELECTION_ARG, mSelectionArgs);
                    }

                    mpickIntent.setClassName("com.android.contacts", "com.android.contacts.activities.MultiplePickSearchActivity");
                    startActivityForResult(mpickIntent, SUBACTIVITY_SEARCH);
                    mIsSubActivityLaunched = true;
                } else {
                	Log.v(TAG, "Current List is empty, no need to launch Search !");
                }
                break;
            }
            /*2013-1-9, add by amt_sunzhao for SWITCHUITWO-463 */ 
            case R.id.checkboxContainer: {
                Log.v(TAG, "SELECT_ALL clicked");
                final CheckBox cb = (CheckBox)findViewById(R.id.checkbox);
                cb.setChecked(!cb.isChecked());
                /*2013-1-9, add end*/ 
                if (mSelectAll) {
                	mSelectAll = false;
                } else {
                	mSelectAll = true;
                }
                adjustChecks();
                break;
            }

            case R.id.preview: {
            	Log.v(TAG, "SEARCH preview clicked");
                // update full list with current group
                syncCheckedStatus(false, mCurrentGroupUri4Return, mChecked);

        	    long[] dataIds = new long[mFullListUri4Return.size()];
                int i = 0;
                Iterator<Uri> it = mFullListUri4Return.iterator();
                while (it.hasNext()) {
                   Uri info = (Uri) it.next();
                   // a17894 temp
                   //Log.v(TAG, "mFullListUri4Return(" + i + ") = " + info);
                   dataIds[i++] = ContentUris.parseId(info);
                }

                Intent mpickIntent = new Intent(MultiplePickPreviewActivity.MULTIPLE_PREVIEW);
                if (mMode == MODE_PICK_EMAIL) {
                    mpickIntent.putExtra(MultiplePickPreviewActivity.EXTRA_PICK_TYPE, MultiplePickPreviewActivity.MODE_PICK_EMAIL);
                } else if (mMode == MODE_PICK_MMS_MIXED) {
                    mpickIntent.putExtra(MultiplePickPreviewActivity.EXTRA_PICK_TYPE, MultiplePickPreviewActivity.MODE_PICK_ALL);
                }
                mpickIntent.putExtra(MultiplePickPreviewActivity.EXTRA_PICK_DATA_ID, dataIds);
                startActivityForResult(mpickIntent, MULTI_PICK_PHONE_EMAIL_ONLY_REQUEST);

                /*
                 * TODO: add startActivityForResult for MULTI_PICK_MIXED_PHONEEMAIL_REQUEST
                 */
                break;
            }

            case R.id.ok: {
            	  int count = 0;
                ArrayList<String> uris = new ArrayList<String>();

                //Log.v(TAG, " Entering onClick_OK()");
                // update full list with current group
                syncCheckedStatus(false, mCurrentGroupUri4Return, mChecked);

                if (mFullListUri4Return != null) {
                    count = mFullListUri4Return.size();
                }
                if (canAddToList(count) < 0) {
                    count = mLimitSize;
                    for (int i = 0; i < count; i++) {
                        uris.add(mFullListUri4Return.get(i).toString());
                    }
                    createLimitReturnDialog(uris).show();
                }
                else if (count > 0) {
                    for (int i = 0; i < count; i++) {
                        uris.add(mFullListUri4Return.get(i).toString());
                    }
                    Intent intent = new Intent();
                    intent.putStringArrayListExtra(Intent.EXTRA_STREAM, uris);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                	  setResult(RESULT_CANCELED);
                    finish();
                }
                break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        // Resolve the intent
        final Intent intent = getIntent();

        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Allow the title to be set to a custom String using an extra on the intent
        String title = intent.getStringExtra(UI.TITLE_EXTRA_KEY);

        final String action = intent.getAction();
        // check contacts db flag
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Log.v(TAG, "Entering onCreate(), action = "+ action +" title passed in ="+ title);
        // Check if SIM loaded and DB ready, or wait
        //if (!SimUtility.getSIMLoadStatus() || !isDbReady) {

        if (mIsSimReady) {
            enterMultiPicker(savedState);
        }
		if (mMode == MODE_PICK_MMS_MIXED) {
		mSlideGestureListener = new SlideGestureListener();
		mSlideshowGestureDetector = new GestureDetector(this, mSlideGestureListener);
		}
		
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }		

        // Wrap our context to inflate list items using correct theme
        mDialogContext = new ContextThemeWrapper(this,
                                                 getResources().getBoolean(R.bool.contacts_dark_ui)
                                                 ? com.android.internal.R.style.Theme_Holo_Dialog_Alert
                                                 : com.android.internal.R.style.Theme_Holo_Light_Dialog_Alert
                                                 );
                        
        //Log.v(TAG, "Leaving onCreate()");
    }

    private void enterMultiPicker(Bundle savedState) {
        // Resolve the intent
        final Intent intent = getIntent();
        // Allow the title to be set to a custom String using an extra on the intent
        String title = intent.getStringExtra(UI.TITLE_EXTRA_KEY);
        final String action = intent.getAction();
        mMode = MODE_DEFAULT;
        mIsSubActivityLaunched = false;
        mSearchViewExitOnStop = false;

        if ("com.motorola.action.PICK_MULTIPLE".equals(action)) {
            // XXX These should be showing the data from the URI given in the Intent.
            final String type = intent.resolveType(this);
            Log.v(TAG, " action = PICK_MULTIPLE, type=" + type);
            //Log.v(TAG, " ContactMethods.CONTENT_EMAIL_TYPE = " + ContactMethods.CONTENT_EMAIL_TYPE);
            if (People.CONTENT_TYPE.equals(type)) {
                mMode = MODE_LEGACY_PICK_PERSON;
                title = getResources().getString(R.string.select_people);
                mIntentGroup = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_FIXED_GROUPID, 0);
            } else if (Phones.CONTENT_TYPE.equals(type)) {
                mMode = MODE_LEGACY_PICK_PHONE;
                title = getResources().getString(R.string.select_phone);
                //get extra request
                mIntentGroup = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_FIXED_GROUPID, 0);
                Log.v(TAG, "LEGACY PHONE: passed in requested group_id = " + mIntentGroup);
            } else if (MultiplePickActivity.EMAIL_TYPE.equals(type)) {
                mMode = MODE_PICK_EMAIL;
                mLinkageIdInc = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_LINKAGE_INCLUDE, 0);
                mLinkageIdExc = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_LINKAGE_EXCLUDE, 0);
                // select email excluding specified linkage

                title = getResources().getString(R.string.select_email);
            } else if (MultiplePickActivity.MMS_TYPE.equals(type)) {
            	mMode = MODE_PICK_MMS_MIXED;
            	title = getResources().getString(R.string.contactMultiplePickerActivityTitle);
                //get extra request
                mIntentGroup = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_FIXED_GROUPID, 0);
                mCurrentMmsKind = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_MMS_KIND, OPTION_PHONE_ONLY);
                if (mCurrentMmsKind != OPTION_PHONE_ONLY && mCurrentMmsKind != OPTION_EMAIL_ONLY) {
                	// default to PHONE
                    mCurrentMmsKind = OPTION_PHONE_ONLY;
                }
                Log.v(TAG, "MMS: passed in requested group_id = " + mIntentGroup+", mms_kind = "+ mCurrentMmsKind);
            } else if (Contacts.CONTENT_TYPE.equals(type)) {
                mMode = MODE_PICK_CONTACT;
                mLinkageIdInc = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_LINKAGE_INCLUDE, 0);
                mLinkageIdExc = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_LINKAGE_EXCLUDE, 0);
                title = getResources().getString(R.string.select_people);
                mIntentGroup = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_FIXED_GROUPID, 0);
                mIntentCopyD2C = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_COPY_DEVICE_TO_CARD, 0);
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
                    	// mPassedInSelection = CLAUSE_SPECIFIED_GROUP + mIntentGroup ;
                        mPassedInSelection = RawContacts._ID + " IN " + ContactsUtils.getGroupMemberIds(getContentResolver(), mIntentGroup);
                    } else {
                    	// mPassedInSelection += " AND " + CLAUSE_SPECIFIED_GROUP + mIntentGroup;
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
                	//mPassedInSelection = CLAUSE_SPECIFIED_GROUP + mLinkageIdInc;
                    mPassedInSelection = RawContacts._ID + " IN " + ContactsUtils.getGroupMemberIds(getContentResolver(), mLinkageIdInc);
                }
                
                if (mWritableOnly) {
                	  //???String excludeReadonly = SimUtility.excludeReadonly(MultiplePickActivity.this);
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
               
                Log.v(TAG, "PICK_CONTACTS: passed in copyD2C="+mIntentCopyD2C+", selection: " + mPassedInSelection
                           +", group_specified= "+mIntentGroup+", exc_group: "+mLinkageIdExc+", inc_group: "+mLinkageIdInc);
            } else if (Phone.CONTENT_TYPE.equals(type)) {
                mMode = MODE_PICK_PHONE;
                title = getResources().getString(R.string.select_phone);
                //get extra request
                mIntentGroup = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_FIXED_GROUPID, 0);
                mLinkageIdInc = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_LINKAGE_INCLUDE, 0);
                mLinkageIdExc = intent.getLongExtra(MULTI_PICKER_INTENT_EXTRA_LINKAGE_EXCLUDE, 0);
                // select phone excluding specified linkage

                Log.v(TAG, "NEW PHONE: passed in requested group_id = " + mIntentGroup);
            }
            // set title for multi-picker
            if (title != null &&(mMode != MODE_PICK_MMS_MIXED)) {
                setTitle(title);
            }
        }

        if (mMode == MODE_PICK_MMS_MIXED || mMode == MODE_PICK_EMAIL) {
			//requestWindowFeature(Window.FEATURE_NO_TITLE);
            mHasLimitSize = true;
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

        if (mIntentGroup > 0) {
            // fix-group
            setContentView(R.layout.multi_picker_fixed_simple);
        } else
*/
        {
        	// filter-able group
            setContentView(R.layout.multi_picker_fixed);
			findViewById(R.id.list_header_add_contact).setVisibility(View.GONE);
			/*2013-1-9, add by amt_sunzhao for SWITCHUITWO-463 */ 
			findViewById(R.id.checkboxContainer).setVisibility(View.VISIBLE);
			/*2013-1-9, add end*/ 
        }

        // Set the proper empty string
        setEmptyText();

        // setup filter spinner UI
        //buildFilter();


        String [] from;
        int [] to;
        switch (mMode) {
            case MODE_LEGACY_PICK_PERSON:
            case MODE_LEGACY_PICK_PHONE:
            //case MODE_PICK_MMS_MIXED:     legacy mms
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
                from = new String[] {Contacts.DISPLAY_NAME, Data._ID, Phone.TYPE, Phone.NUMBER};
                to = new int[] {R.id.multi_picker_list_item_name, R.id.checkitembox, R.id.label, R.id.number};
                break;

            case MODE_PICK_EMAIL:
                from = new String[] {Contacts.DISPLAY_NAME, Data._ID, Email.TYPE, Email.DATA};
                to = new int[] {R.id.multi_picker_list_item_name, R.id.checkitembox, R.id.label, R.id.number};
                break;

            default:   // MODE_PICK_CONTACT
                // only display name
                from = new String[] {Contacts.DISPLAY_NAME, RawContacts._ID};
                to = new int[] {R.id.multi_picker_list_item_name, R.id.checkitembox};
                break;
        }

        mPickerAdapter = new MultiplePickAdapter(this,
                                          R.layout.multi_picker_list_item,
                                          null,
                                          from,
                                          to);
        

        MultiplePickAdapter.ViewBinder viewBinder = new MultiplePickAdapter.ViewBinder() {
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
                //Log.v(TAG, "ViewBinder(), pos = "+position+", cursor="+cursor+", col_index="+columnIndex);

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
                    String name = cursor.getString(columnIndex);
                    //Log.v(TAG, "NAME = " + name);
                    /*
                    String name;
                    if (mMode==MODE_PICK_PHONE || mMode==MODE_PICK_EMAIL || mMode==MODE_PICK_MMS_MIXED) {
                    	// phone or email projection's column
                    	name = cursor.getString(PHONE_DISPLAY_NAME_COLUMN_INDEX);
                    } else {
                    	// summary_contact projection's column
                        name = cursor.getString(SUMMARY_NAME_COLUMN_INDEX);
                    } */

                    if (!TextUtils.isEmpty(name)) {
                        ((TextView)view).setText(name);
                    } else {
                        ((TextView)view).setText(mUnknownName);
                    }

                    // set the check mark
                    //if (mChecked != null && position < mChecked.length  && mListView != null) {
                    	// pretect index over-boundary due to timing between finish() and refresh display
                      //  ((CheckBox)view).setChecked(mChecked[position]);
                    	// set listItem checked status
                    	//mListView.setItemChecked(position, mChecked[position]);
                    //}
                    return true;
                //} else if (cursor != null && TYPE_FIELD.equals(cursor.getColumnName(columnIndex))) {
                }else if(view instanceof CheckBox){
					if (mChecked != null && position < mChecked.length  && mListView != null) {
                    	// pretect index over-boundary due to timing between finish() and refresh display
                        ((CheckBox)view).setChecked(mChecked[position]);
						((CheckBox)view).setOnClickListener(new View.OnClickListener(){
							public void onClick(View view){
								checkBoxClicked(view);
							}
						});
                    	// set listItem checked status
                    	//mListView.setItemChecked(position, mChecked[position]);
                    }
                    return true;
				}else if (cursor != null && Phone.TYPE.equals(cursor.getColumnName(columnIndex))) {
                	// bind the 'type' field for both PICK_PHONE and PICK_EMAIL modes
                    int type = cursor.getInt(columnIndex);
                    String label = cursor.getString(PHONE_LABEL_COLUMN_INDEX);

                    //Log.v(TAG, "type or data = " +type+", label = "+label);
                    /*Modifyed for T810T-JB_P000743 begin*/
                    if ((OPTION_EMAIL_ONLY == mCurrentMmsKind) || (mMode == MODE_PICK_EMAIL)) {
                    	/*Modifyed for T810T-JB_P000743 end*/
                    	// PICK_EMAIL
                        ((TextView)view).setText(Email.getTypeLabel(MultiplePickActivity.this.getResources(), type, label));
                    } else {
                    	// PICK_PHONE
                        ((TextView)view).setText(Phone.getDisplayLabel(MultiplePickActivity.this, type, label));
                    }
					((TextView)view).setVisibility(View.VISIBLE);
                	return true;
                //} else if (cursor != null && NUMBER_FIELD.equals(cursor.getColumnName(columnIndex)) &&
                //    mMode==MODE_PICK_MMS_MIXED && isPhone==false) {
                } else if (cursor != null && Phone.NUMBER.equals(cursor.getColumnName(columnIndex))) {
                	// bind the 'number' field for phone
                	((TextView)view).setText(cursor.getString(columnIndex));
					((TextView)view).setVisibility(View.VISIBLE);
                	// Log.v(TAG, "=========== number field: "+  cursor.getString(PHONE_NUMBER_COLUMN_INDEX));
                	return true;
                } else if (cursor != null && Email.DATA.equals(cursor.getColumnName(columnIndex))) {
                	// bind the 'data' field for email
                	((TextView)view).setText(cursor.getString(columnIndex));
					((TextView)view).setVisibility(View.VISIBLE);
                	// Log.v(TAG, "=========== data field: "+  cursor.getString(EMAIL_DATA_COLUMN_INDEX));
                	return true;
                } else if (cursor != null && GROUP_FIELD.equals(cursor.getColumnName(columnIndex))) {
                	/*
                	// bind the 'group' field,  _id column used to bind group
                    long personId = -1;
                    if (mMode==MODE_PICK_MMS_MIXED && DISPLAY_TYPE_ALL_WITH_PHONES != mDisplayType) {
                        //Log.v(TAG, "personId in col:COLUMN_INDEX_MMS_ID/GPP ="+cursor.getLong(COLUMN_INDEX_MMS_ID)+", COLUMN_INDEX_NORM_ID="+ cursor.getLong(COLUMN_INDEX_NORM_ID));
                        //Log.v(TAG, "personId in col:5 ="+cursor.getLong(COLUMN_INDEX_MMS_TIMES_CONTACTED));
                    	if(isPhone) {
                    	    // phone table's column
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

                    //Log.v(TAG, "=========== personId=" + personId);
                    if(SimUtility.isGroupMember(getContentResolver(), mCGroupId, personId)) {
                        ((TextView)view).setText(getString(R.string.type_c_sim));
                        //Log.v(TAG, "C-Card group");
                    } else if(SimUtility.isGroupMember(getContentResolver(), mGGroupId, personId)){
                        ((TextView)view).setText(getString(R.string.type_g_sim));
                        //Log.v(TAG, "G-Card group");
                    } else {
                    	//Log.v(TAG, "None-SIM group");
                        ((TextView)view).setText(getString(R.string.type_device));
                        //Log.v(TAG, "Device group");
                    } */
                    // hide the group indicator
                    view.setVisibility(View.GONE);
                    return true;
                }
                return false;
            }
        };
        mPickerAdapter.setViewBinder(viewBinder);
//        mPickerAdapter.setFilterQueryProvider(this);
        setListAdapter(mPickerAdapter);

        mQueryHandler = new QueryHandler(this);

        findViewById(R.id.ok).setOnClickListener(this);
	    findViewById(R.id.preview).setOnClickListener(this);
//        findViewById(R.id.cancel).setOnClickListener(this);
	    /*2013-1-9, add by amt_sunzhao for SWITCHUITWO-463 */ 
        findViewById(R.id.checkboxContainer).setOnClickListener(this);
        /*2013-1-9, add end*/ 
        //findViewById(R.id.search).setOnClickListener(this);
        findViewById(R.id.list_header_search).setOnClickListener(this);
  	    findViewById(R.id.list_header_groups).setOnClickListener(this);
		
/*
        // hide the search views
        findViewById(R.id.search).setVisibility(View.GONE);
        findViewById(R.id.list_header_search).setVisibility(View.GONE);
*/
        mListView = getListView();
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mListView.setOverScrollMode(View.OVER_SCROLL_ALWAYS);

        // init globals
        mUnknownName = getResources().getText(android.R.string.unknownName);

        // setup tab view for MMS mode
        mTabWidget = (ScrollingTabWidget) findViewById(R.id.tab_widget);
        mTabWidget.setTabSelectionListener(this);
        if (mMode == MODE_PICK_MMS_MIXED) {
       	    findViewById(R.id.list_header_groups).setVisibility(View.VISIBLE);
        	findViewById(R.id.preview).setVisibility(View.VISIBLE);

        	mTabWidget.setVisibility(View.GONE);
        	mTabRawContactIdMap = new SparseArray<Long>();

            // bind tabs
            clearCurrentTabs();
            bindTabs();
        } else if (mMode == MODE_PICK_EMAIL){
        	mTabWidget.setVisibility(View.GONE);
        	findViewById(R.id.list_header_groups).setVisibility(View.VISIBLE);
        	findViewById(R.id.preview).setVisibility(View.VISIBLE);
        } else {
        	mTabWidget.setVisibility(View.GONE);
        	findViewById(R.id.list_header_groups).setVisibility(View.GONE);
			findViewById(R.id.divider1).setVisibility(View.GONE);
        	findViewById(R.id.preview).setVisibility(View.GONE);

        }

        mJustCreated = true;
        Log.v(TAG, " Leaving onCreate()");
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	Log.v(TAG, "onResume(), mIsSimReady = " + mIsSimReady + ", mJustCreated" + mJustCreated);
        if (mFilter == null) {
            ContactListFilter filter = ContactListFilter.restoreDefaultPreferences(PreferenceManager.getDefaultSharedPreferences(this));
            if (!((mIntentCopyD2C == COPY_DEVICE_TO_CARD) && HardCodedSources.ACCOUNT_TYPE_CARD.equals(filter.accountType))
                && !((mIntentCopyD2C == COPY_CCARD_TO_DEVICE) || (mIntentCopyD2C == COPY_GCARD_TO_DEVICE))) {
                // Use existing filter unless 1) the mode is copy-card-to-device, 2) the filter is set to card only and the mode is copy-to-card
                mFilter = filter;
            }
        }

    	if (mIsSimReady || mJustCreated) {
            // Load the preferences
/* WARNING: This would cause preview/done button be incorrectly enabled when no data is selected
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);


            mDisplayOnlyPhones = prefs.getBoolean(Prefs.DISPLAY_ONLY_PHONES,
                    Prefs.DISPLAY_ONLY_PHONES_DEFAULT);
            
    	    setEmptyText();
    	    // Async query
            startQuery(); */
            updateList();
        }

        mJustCreated = false;
    }

    public void clearAll() {
	    mChecked = null;
	    mBaseUri4Return = null;
	    mCurrentGroupUri4Return = null;
	    mListItemClickedCount = 0;
    }

    @Override
    protected void onRestart() {
    	super.onRestart();
    	//Log.v(TAG, "onRestart()");
    }

    @Override
    protected void onStart() {
    	super.onStart();
    	//Log.v(TAG, "onStart()");
    	// should destroy completely due to SearchView finished
    	if (mSearchViewExitOnStop) {
    		Log.v(TAG, "calling finish() to exit");
    	    setResult(RESULT_CANCELED);
    	    finish();
        }
    }

    @Override
    protected void onPause() {
    	super.onPause();
    	//Log.v(TAG, "onPause()");
    }

    @Override
    protected void onStop() {
    	super.onStop();
    	//Log.v(TAG, "onStop()");
    	// should destroy completely since contacts db may be updating
    	/*
    	if (!mIsSubActivityLaunched && mIsSimReady) {
    		Log.v(TAG, "calling finish() to exit");
    	    setResult(RESULT_CANCELED);
    	    finish();
        } */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mPickerAdapter != null) {
            Cursor cursor = mPickerAdapter.getCursor();
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            /*2013-1-8, add by amt_sunzhao for SWITCHUITWO-345 */ 
            if(null != mObserverUri) {
            	getContentResolver().unregisterContentObserver(mCursorContentObserver);
            }
            /*2013-1-8, add end*/ 
        }

        //Log.v(TAG, "onDestroy()");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_SEARCH: {
            	//Log.v(TAG, "Search Hardkey pressed");
                this.onClick(findViewById(R.id.list_header_search));
                break;
            }
        }
        
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mMode == MODE_PICK_MMS_MIXED) {
        	MenuItem item;
        	// hide menu items according to current data kind
        	if (OPTION_PHONE_ONLY == mCurrentMmsKind) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                mDisplayOnlyMobiles = false; //prefs.getBoolean(Prefs.PREF_DISPLAY_MOBILE_ONLY,true);
                if(mDisplayOnlyMobiles){
                    if ((item = menu.findItem(MENU_MOBILE_ONLY)) != null) {
                        item.setVisible(false);
                    }
                    if((item = menu.findItem(MENU_ALL_NUMBERS)) != null){
                        item.setVisible(true);
                    }
                } else {
                    if ((item = menu.findItem(MENU_MOBILE_ONLY)) != null) {
                        item.setVisible(true);
                    }
                    if((item = menu.findItem(MENU_ALL_NUMBERS)) != null){
                        item.setVisible(false);
                    }
                }
        		// hide
        	    if ((item = menu.findItem(MENU_PHONE_ONLY)) != null) {
        	        item.setVisible(false);
                }
                // show
        	    if ((item = menu.findItem(MENU_EMAIL_ONLY)) != null && !(item.isVisible())) {
        	        item.setVisible(true);
                }
        	} else if (OPTION_EMAIL_ONLY == mCurrentMmsKind) {
                // hide menu_mobile_only and menu_all_numbers
                if ((item = menu.findItem(MENU_MOBILE_ONLY)) != null) {
                    item.setVisible(false);
                }
                if((item = menu.findItem(MENU_ALL_NUMBERS)) != null){
                    item.setVisible(false);
                }
        		// hide
        	    if ((item = menu.findItem(MENU_EMAIL_ONLY)) != null) {
        	        item.setVisible(false);
                }
                // show
        	    if ((item = menu.findItem(MENU_PHONE_ONLY)) != null && !(item.isVisible())) {
        	        item.setVisible(true);
                }
            }
        }                
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        if (mMode == MODE_PICK_MMS_MIXED) {
//            menu.add(0, MENU_MOBILE_ONLY, 0, R.string.menu_mobileOnly).setIcon(R.drawable.ic_message_menu_mobile_contacts);
//            menu.add(0, MENU_ALL_NUMBERS, 0, R.string.menu_allNumbers).setIcon(R.drawable.ic_message_menu_all_contacts);
//        }

        // groups
        if (mIntentCopyD2C != COPY_CCARD_TO_DEVICE
            && mIntentCopyD2C != COPY_GCARD_TO_DEVICE
            && mIntentGroup <= 0) {  // don't display group filter when copy from card to device or specified group
            menu.add(0, MENU_DISPLAY_GROUP, 0, R.string.menu_contacts_filter)
                    .setIcon(R.drawable.ic_menu_allfriends);
        }
        /*
        if (mMode == MODE_PICK_MMS_MIXED) {
       	    // show email only
            menu.add(0, MENU_EMAIL_ONLY, 0, R.string.menu_emailOnly)
                    .setIcon(com.android.internal.R.drawable.ic_menu_set_as);

            // show phones only
            menu.add(0, MENU_PHONE_ONLY, 0, R.string.menu_phoneOnly)
                    .setIcon(com.android.internal.R.drawable.ic_menu_set_as);
        } */

    	return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
                        	
        	case MENU_DISPLAY_GROUP:
        	    //Log.v(TAG, "MENU_DISPLAY_GROUP");
                //final Intent intent = new Intent(this, ContactsPreferencesActivity.class);  ???
                final Intent intent = new Intent(this, AccountFilterActivity.class);                
                if (mIntentCopyD2C == COPY_DEVICE_TO_CARD || mLinkageIdExc > 0 || mLinkageIdInc > 0
                    || mExcludedIds != null) {
                    intent.putExtra("com.android.contacts.MultiPickerHideCardGroup", true);
                }
                if (mAccountName != null) {
                    intent.putExtra(MULTI_PICKER_INTENT_EXTRA_ACCOUNT_NAME, mAccountName);
        	    Log.w(TAG, "WARNING! a18768 mAccountName = "+mAccountName);
                }
                if (mAccountType != null) {
                    intent.putExtra(MULTI_PICKER_INTENT_EXTRA_ACCOUNT_TYPE, mAccountType);                
        	    Log.w(TAG, "WARNING! a18768 mAccountType = "+mAccountType);
                }
                
                startActivityForResult(intent, SUBACTIVITY_ACCOUNT_FILTER);
                mIsSubActivityLaunched = true;
        	    return true;

        	case MENU_PHONE_ONLY:
        	    //Log.v(TAG, "MENU_PHONE_ONLY");
        	    mCurrentMmsKind = OPTION_PHONE_ONLY;
        	    updateList();
        	    return true;

        	case MENU_EMAIL_ONLY:
        	    //Log.v(TAG, "MENU_EMAIL_ONLY");
        	    mCurrentMmsKind = OPTION_EMAIL_ONLY;
        	    updateList();
        	    return true;
            case MENU_MOBILE_ONLY:
                mCurrentMmsKind = OPTION_PHONE_ONLY;
                prefs.edit().putBoolean(Prefs.PREF_DISPLAY_MOBILE_ONLY, true).commit();
                updateList();
                return true;
            case MENU_ALL_NUMBERS:
                mCurrentMmsKind = OPTION_PHONE_ONLY;
                prefs.edit().putBoolean(Prefs.PREF_DISPLAY_MOBILE_ONLY, false).commit();
                updateList();
                return true;
        }
        return false;
    }

    private void startQuery() {
        Uri uri;
        String[] projection = null;
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = getSortOrder(mMode);
        //Log.v(TAG, "Entering startQuery()");

        // Cancel any pending queries
        mQueryHandler.cancelOperation(QUERY_TOKEN);

        //Log.v(TAG, "mMode="+mMode+", sortOrder: "+sortOrder+", mDisplayType="+mDisplayType+", info="+mDisplayInfo+", mGroupId="+mGroupId);
        // Kick off the new async query
        switch (mMode) {
            case MODE_PICK_EMAIL:
                /*
                uri = ContactMethods.CONTENT_URI;  // or ContactMethods.CONTENT_EMAIL_URI
                projection = CONTACT_METHODS_EMAIL_PROJECTION;
                selection = ContactMethodsColumns.KIND+"="+Contacts.KIND_EMAIL;
                mBaseUri4Return = ContactMethods.CONTENT_URI; */
                uri = getUriToQuery();
                projection = getProjectionForQuery();
                prepareSelectionAndArgs();
                selection = mSelection;
                selectionArgs = mSelectionArgs;                
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
                // Used in Copy to Card/Device case
                uri = getUriToQuery();
                projection = getProjectionForQuery();
                prepareSelectionAndArgs();
                selection = mSelection+" AND "+RawContacts.DELETED + "=0";
                selectionArgs = mSelectionArgs;
                mBaseUri4Return = RawContacts.CONTENT_URI;
                break;

            case MODE_PICK_PHONE:
                Log.w(TAG, "WARNING! a18768 Do we use it? PICK NEW PHONE case");
                uri = getUriToQuery();
                projection = getProjectionForQuery();
                prepareSelectionAndArgs();
                selection = mSelection;
                selectionArgs = mSelectionArgs;                
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
                Log.w(TAG, "WARNING! a18768 Do we use it? PICK LEGACY PHONE case");
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
                //Log.v(TAG, "PICK-MMS_MIXED case, list phone or email");
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
                	    	// mGroupId = 1;
                	    	// group=1 is google/My_contacts group, not include card groups !
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
                prepareSelectionAndArgs();
                if (OPTION_EMAIL_ONLY == mCurrentMmsKind) {
                    selection = mSelection;
                } else {
                    if(mDisplayOnlyMobiles){
                        selection = mSelection + " AND " +CLAUSE_ONLY_MOBILE;
                    } else {
                        selection = mSelection;
                    }
                }
                selectionArgs = mSelectionArgs;
                //Log.v(TAG, "selection: "+selection);
                // data table's uri
                mBaseUri4Return = Data.CONTENT_URI;
                break;

            default:
                Log.w(TAG, "WARNING! a18768 Do we use it? PICK LEGACY PERSON case");
                switch(mDisplayType) {
                	case DISPLAY_TYPE_ALL:
                	    uri = People.CONTENT_URI;
                	    projection = NORMAL_ITEM_PROJECTION;
                	    break;
                	case DISPLAY_TYPE_ALL_WITH_PHONES:
                	    uri = People.CONTENT_URI;
                	    projection = NORMAL_ITEM_PROJECTION;
                	    selection = People.PRIMARY_PHONE_ID + " IS NOT NULL";
                	    break;
                	case DISPLAY_TYPE_SYSTEM_GROUP:
                        if (!TextUtils.isEmpty(mDisplayInfo)) {
                            // Display the selected system group
                            uri = Uri.parse("content://contacts/groups/system_id/" + mDisplayInfo + "/members");
                        } else {
                            // No valid group is present, display as 'ALL'
                            uri = People.CONTENT_URI;
                        }
                        projection = NORMAL_ITEM_PROJECTION;
                	    break;
                	default:  // DISPLAY_TYPE_USER_GROUP
                	    uri = Uri.parse(PEOPLE_GROUPMEMBERSHIP_CONTENT_URI);
                	    projection = GROUPMEMBERSHIP_PEOPLE_PHONES_ITEM_PROJECTION;
                	    if (mGroupId <= 0) {
                	    	selection = GroupMembership.GROUP_ID + "!=" + mVirtualGroupId;
                	    } else {
                	        selection = GroupMembership.GROUP_ID + "=" + mGroupId;
                	    }
                	    break;
                }
                // Filter out deleted contacts
                selection += " AND " + RawContacts.DELETED + "=0";
                mBaseUri4Return = People.CONTENT_URI;
                break;
        }
        Log.v(TAG, "uri="+uri+", selection="+selection+", sortorder="+sortOrder+", mBaseUri4Return:"+mBaseUri4Return);
        /*2013-1-8, add by amt_sunzhao for SWITCHUITWO-345 */ 
        if(null != uri
        		&& !uri.equals(mObserverUri)) {
        	getContentResolver().unregisterContentObserver(mCursorContentObserver);
        	mObserverUri = uri;
        	getContentResolver().registerContentObserver(uri, false, mCursorContentObserver);
        }
        /*2013-1-8, add end*/ 
        mQueryHandler.startQuery(QUERY_TOKEN, null,
                                 uri,
                                 projection,
                                 selection,
                                 selectionArgs,
                                 sortOrder);
        //Log.v(TAG, "Leaving startQuery()");
    }

    private void setEmptyText() {
        TextView empty = (TextView) findViewById(R.id.emptyText);
        // Center the text by default
        int gravity = Gravity.CENTER;

        empty.setText(getText(R.string.noContacts));
        /*
        switch (mMode) {
            case MODE_PICK_EMAIL:
                if (mDisplayInfo == null || Groups.GROUP_MY_CONTACTS.equals(mDisplayInfo)) {
                	// system groups
                	empty.setText(getText(R.string.noContactsWithEmail));
                } else {
                	// user groups
                	empty.setText(getString(R.string.groupEmpty, mDisplayInfo));
                }
                break;

            case MODE_LEGACY_PICK_PHONE:
            case MODE_PICK_PHONE:
                if (mDisplayInfo == null || Groups.GROUP_MY_CONTACTS.equals(mDisplayInfo)) {
                	// system groups
                	empty.setText(getText(R.string.noContactsWithPhoneNumbers));
                } else {
                	// user groups
                	empty.setText(getString(R.string.groupEmpty, mDisplayInfo));
                }
                break;

            case MODE_PICK_MMS_MIXED:
                if (mDisplayInfo == null || Groups.GROUP_MY_CONTACTS.equals(mDisplayInfo)) {
                	// system groups
                	empty.setText(getText(R.string.noContactsWithPhoneEmails));
                } else {
                	// user groups
                	empty.setText(getString(R.string.groupEmpty, mDisplayInfo));
                }
                break;

            default:
                if (mDisplayInfo == null || Groups.GROUP_MY_CONTACTS.equals(mDisplayInfo)) {
                	// system groups
                	empty.setText(getText(R.string.noContacts));
                } else {
                	// user groups
                	empty.setText(getString(R.string.groupEmpty, mDisplayInfo));
                }
                break;
        }
        if (mDisplayOnlyPhones) {
            empty.setText(getText(R.string.noContactsWithPhoneNumbers));
        } else {
            empty.setText(getText(R.string.noContactsHelpText));
            gravity = Gravity.NO_GRAVITY;
        } */

        empty.setGravity(gravity);
        // only show the empty after query and find query result is empty
        empty.setVisibility(View.GONE);
    }

    private void adjustChecks() {
        final ListView list = mListView;
        int count = list.getCount();
        Button done = (Button) findViewById(R.id.ok);
        int memberCount = 0;
    	CharSequence newText;

        //Log.v(TAG, "Entering adjustChecks(), mSelectAll=" + mSelectAll+", list_size="+count+", clicked_cnt="+mListItemClickedCount+", mChecked = "+mChecked);
        if (mSelectAll) {
        	// set check mark for all items
            Log.v(TAG, "under mSelectAll=T");
            for (int i = 0; i < count; i++) {
            	if (i < mChecked.length) {
                    if (!mChecked[i]) { // CAN NOT use:  isItemChecked(), since it's applicable only to visible list items
            	    	mListItemClickedCount ++;
            	    	//Log.v(TAG, "i["+i+"]= F, inc count =" +mListItemClickedCount);
            	    }
                    if (list != null) list.setItemChecked(i, true);
                    mChecked[i] = true;
                }
            }
        } else {
        	// clear all check mark for all items
            Log.v(TAG, "under mSelectAll=F");
            for (int i = 0; i < count; i++) {
            	if (i < mChecked.length) {
            	    if (mChecked[i]) { // CAN NOT use:  isItemChecked(), since it's applicable only to visible list items
            	    	mListItemClickedCount --;
            	    }
                    if (list != null) list.setItemChecked(i, false);
                    mChecked[i] = false;
                }
            }
        }

        //update the selected count info
        if (done != null) {
    	    //memberCount = ContactsUtils.getNoLinkageMemberCount(mFullListUri4Return); //+ mListItemClickedCount;
    	    memberCount = mFullListUri4Return.size() + mListItemClickedCount;
            newText = getString(R.string.menu_done) + " (" + memberCount + ")";
        	//Log.v(TAG, "after: mListItemClickedCount = "+mListItemClickedCount+"memberCount: "+ memberCount);
            done.setText(newText);
            if (memberCount > 0) {
            	done.setEnabled(true);
            	findViewById(R.id.preview).setEnabled(true);
            } else {
            	done.setEnabled(false);
            	findViewById(R.id.preview).setEnabled(false);
            }
        }
        //Log.v(TAG, "Leaving adjustChecks(), cal_clicked_cnt="+mListItemClickedCount+", F_size="+mFullListUri4Return.size());
        if (mSelectAll) {
            // check if it reach the limit size
            if (canAddToList(mFullListUri4Return.size() + mListItemClickedCount) <= 0) {
       	        createLimitDialog().show();
            }
        }
    }

    protected static String getSortOrder(int mode) {
        if (Locale.getDefault().equals(Locale.JAPAN) &&
                mode == MODE_LEGACY_PICK_PERSON) {   // people in Japanese
            return SORT_STRING + " ASC";
        } else if (mode == MODE_LEGACY_PICK_PERSON) {   // people in English
        	return NAME_COLUMN;// + " COLLATE LOCALIZED_PINYIN ASC";
        } else if (mode == MODE_LEGACY_PICK_PHONE) {   // phone
        	return NAME_COLUMN;// + " COLLATE LOCALIZED_PINYIN ASC";
        } else if (mode == MODE_PICK_EMAIL) {   // email
        	return Contacts.SORT_KEY_PRIMARY + " ASC";
        } else if (mode == MODE_PICK_MMS_MIXED) {   // mms
        	return Contacts.SORT_KEY_PRIMARY + " ASC";
        } else if (mode == MODE_PICK_CONTACT) {   // contact
        	return Contacts.SORT_KEY_PRIMARY + " ASC";
        } else if (mode == MODE_PICK_PHONE) {   // phone
        	return Contacts.SORT_KEY_PRIMARY + " ASC";
        } else {   // others = no order
            return Contacts.SORT_KEY_PRIMARY + " ASC";
        }
    }

/*    private Uri getQueryUri(int displayType, long group_id, String displayInfo) {
    	Uri uri = null;
    	String selection = null;
        switch (mMode) {
            case MODE_PICK_EMAIL:
                uri = ContactMethods.CONTENT_URI;
//                selection = ContactMethodsColumns.KIND+"="+Contacts.KIND_EMAIL;
                break;
            case MODE_LEGACY_PICK_PHONE:
                switch(displayType) {
                	case DISPLAY_TYPE_ALL:
                	    uri = Phones.CONTENT_URI;
                	    break;
                	case DISPLAY_TYPE_ALL_WITH_PHONES:
                	    uri = Phones.CONTENT_URI;
                	    selection = PhonesColumns.ISPRIMARY + "=1";
                	    break;
                	case DISPLAY_TYPE_SYSTEM_GROUP:
                	    //Log.v(TAG, "getQueryUri() on DISPLAY_TYPE_SYSTEM_GROUP, displayInfo = "+displayInfo+", groupId="+group_id);
                        if (!TextUtils.isEmpty(displayInfo)) {
                            // Display the selected system group
                	        uri = Uri.parse(PHONE_GROUPMEMBERSHIP_CONTENT_URI);
                	        selection = GroupMembership.GROUP_ID + "=" + group_id;
                        } else {
                            // No valid group is present, display as 'ALL'
                            uri = Phones.CONTENT_URI;
                        }
                	    break;
                	default:  // DISPLAY_TYPE_USER_GROUP
                	    uri = Uri.parse(PHONE_GROUPMEMBERSHIP_CONTENT_URI);
                	    selection = GroupMembership.GROUP_ID + "=" + group_id;
                	    break;
                }
                break;
            case MODE_PICK_MMS_MIXED:
                switch(displayType) {
                	case DISPLAY_TYPE_ALL_WITH_PHONES:
                	    uri = Phones.CONTENT_URI;
                	    selection = PhonesColumns.ISPRIMARY + "=1";
                	    break;
                	case DISPLAY_TYPE_ALL:
                	case DISPLAY_TYPE_SYSTEM_GROUP:
                	default:  // DISPLAY_TYPE_USER_GROUP
                	    uri = Uri.parse(PHONE_GROUPMEMBERSHIP_CONTACTMETHODS_CONTENT_URI);
                	    // pick desired group
                	    if (group_id <= 0) {
                	    	selection = GroupMembership.GROUP_ID + "!=" + mVirtualGroupId;
                	    } else {
                	        selection = GroupMembership.GROUP_ID + "=" + group_id;
                	    }
                	    break;
                }
                break;
            default:
                switch(displayType) {
                	case DISPLAY_TYPE_ALL:
                	    uri = People.CONTENT_URI;
                	    break;
                	case DISPLAY_TYPE_ALL_WITH_PHONES:
                	    uri = People.CONTENT_URI;
                	    selection = People.PRIMARY_PHONE_ID + " IS NOT NULL";
                	    break;
                	case DISPLAY_TYPE_SYSTEM_GROUP:
                	    //Log.v(TAG, "getQueryUri() on DISPLAY_TYPE_SYSTEM_GROUP, displayInfo = "+displayInfo+", groupId="+group_id);
                        if (!TextUtils.isEmpty(displayInfo)) {
                            // Display the selected system group
                            uri = Uri.parse("content://contacts/groups/system_id/" + displayInfo + "/members");
                        } else {
                            // No valid group is present, display as 'ALL'
                            uri = People.CONTENT_URI;
                        }
                	    break;
                	default:  // DISPLAY_TYPE_USER_GROUP
                	    uri = Uri.parse(PEOPLE_GROUPMEMBERSHIP_CONTENT_URI);
                	    selection = GroupMembership.GROUP_ID + "=" + group_id;
                	    break;
                }
                break;
        }
        mSelection = selection;
        return uri;
    }*/

    private void setGroupEntries() {
    	/*
        boolean syncEverything;
        Uri uri;

        // refer to that in listview
        String value = Contacts.Settings.getSetting(getContentResolver(), null,
                Contacts.Settings.SYNC_EVERYTHING)  null;

        if (value == null) {
            // If nothing is set yet we default to syncing everything
            syncEverything = true;
        } else {
            syncEverything = !TextUtils.isEmpty(value) && !"0".equals(value);
        }

        Cursor cursor;
        if (!syncEverything) {
            cursor = getContentResolver().query(Groups.CONTENT_URI, GROUPS_PROJECTION,
                    Groups.SHOULD_SYNC + " != 0", null, Groups.DEFAULT_SORT_ORDER);
        } else {
            cursor = getContentResolver().query(Groups.CONTENT_URI, GROUPS_PROJECTION,
                    null, null, Groups.DEFAULT_SORT_ORDER);
        }
        if (cursor == null) return;
        try {
        	Cursor entryCursor;
        	String groupText;
        	int count = 0;
            ArrayList<CharSequence> groups = new ArrayList<CharSequence>();
            ArrayList<CharSequence> prefStrings = new ArrayList<CharSequence>();
            ArrayList<Long> groupIds = new ArrayList<Long>();

            // Add All Contacts as a placeholder in the first item, the actual operation is the last one
/*
            groupText = getString(R.string.showAllGroups);
            // calculate query element according to mode and group
            uri = getQueryUri(DISPLAY_TYPE_ALL, 0, null);
            Log.v(TAG, "populated ALL uri: "+ uri +" ,  selection:" + mSelection);
            entryCursor = getContentResolver().query(uri, null, mSelection, null, null);
            if (entryCursor != null) {
            	count = entryCursor.getCount();
                entryCursor.close();
            } else {
            	count = 0;
            }
            groupText += " (" + count + ")";
*
            groups.add(DISPLAY_GROUP_INDEX_ALL_CONTACTS, "");
            groupIds.add(DISPLAY_GROUP_INDEX_ALL_CONTACTS, Long.valueOf(0));
            prefStrings.add(0, "");

            // Add Contacts with phones
            groupText = getString(R.string.groupNameWithPhones);
            uri = getQueryUri(DISPLAY_TYPE_ALL_WITH_PHONES, 1, null);
            Log.v(TAG, "populated ALLwPhones uri: "+ uri +" ,  selection:" + mSelection);
            entryCursor = getContentResolver().query(uri, null, mSelection, null, null);
            if (entryCursor != null) {
            	count = entryCursor.getCount();
                entryCursor.close();
            } else {
            	count = 0;
            }
            groupText += " (" + count + ")";
            groups.add(DISPLAY_GROUP_INDEX_ALL_CONTACTS_WITH_PHONES, groupText);
            groupIds.add(DISPLAY_GROUP_INDEX_ALL_CONTACTS_WITH_PHONES, Long.valueOf(1));
            prefStrings.add(GROUP_WITH_PHONES);

            int currentIndex = DISPLAY_GROUP_INDEX_ALL_CONTACTS;
            while (cursor.moveToNext()) {
                String systemId = cursor.getString(GROUPS_COLUMN_INDEX_SYSTEM_ID);
                String name = cursor.getString(GROUPS_COLUMN_INDEX_NAME);
                long groupId = cursor.getLong(GROUPS_COLUMN_INDEX_ID);
                if (cursor.isNull(GROUPS_COLUMN_INDEX_SYSTEM_ID)
                        && !Groups.GROUP_MY_CONTACTS.equals(systemId)) {
                    // All groups that aren't My Contacts, since that one is localized on the phone

                    // Localize the "Starred in Android" string which we get from the server side.
                    if (Groups.GROUP_ANDROID_STARRED.equals(name)) {
                    	mVirtualGroupId = groupId;
                        name = getString(R.string.starredInAndroid);
*                    } else if (SimUtility.C_GROUP_NAME.equals(name)) {
                    	name = getString(R.string.type_c_sim);
                    } else if (SimUtility.G_GROUP_NAME.equals(name)) {
                    	name = getString(R.string.type_g_sim);
*                  }

                    groupText = name;
                    Log.v(TAG, "groupId="+groupId+", name="+name+", mVirtualGroupId="+mVirtualGroupId);
                    uri = getQueryUri(DISPLAY_TYPE_USER_GROUP, groupId, null);
                    Log.v(TAG, "populated USR_GROUP uri="+ uri +" ,  selection:" + mSelection);
                    entryCursor = getContentResolver().query(uri, null, mSelection, null, null);
                    if (entryCursor != null) {
                    	count = entryCursor.getCount();
                        entryCursor.close();
                    } else {
                    	count = 0;
                    }
                    groupText += " (" + count + ")";
                    groups.add(groupText);
                    groupIds.add(groupId);
                    if (name.equals(mDisplayInfo)) {
                        currentIndex = groups.size() - 1;
                    }
                } else {
                    // The My Contacts group
                    groupText = getString(R.string.groupNameMyContacts);

                    uri = getQueryUri(DISPLAY_TYPE_SYSTEM_GROUP, groupId, systemId);
                    Log.v(TAG, "populated SYSTEM_GROUP uri="+ uri +" ,  selection:" + mSelection);
                    entryCursor = getContentResolver().query(uri, null, mSelection, null, null);
                    if (entryCursor != null) {
                    	count = entryCursor.getCount();
                        entryCursor.close();
                    } else {
                    	count = 0;
                    }
                    groupText += " (" + count + ")";
                    groups.add(DISPLAY_GROUP_INDEX_MY_CONTACTS, groupText);
                    groupIds.add(DISPLAY_GROUP_INDEX_MY_CONTACTS, Long.valueOf(1));
                    if (mDisplayType == DISPLAY_TYPE_SYSTEM_GROUP
                            && Groups.GROUP_MY_CONTACTS.equals(mDisplayInfo)) {
                        currentIndex = DISPLAY_GROUP_INDEX_MY_CONTACTS;
                    }
                    mDisplayGroupsIncludesMyContacts = true;
                }
            }

            // re-Add All Contacts due to mVirtualGroupId calculated just now.
            groupText = getString(R.string.showAllGroups);
            // calculate query element according to mode and group
            uri = getQueryUri(DISPLAY_TYPE_ALL, 0, null);
            Log.v(TAG, "populated ALL uri: "+ uri +" ,  selection:" + mSelection);
            entryCursor = getContentResolver().query(uri, null, mSelection, null, null);
            if (entryCursor != null) {
            	count = entryCursor.getCount();
                entryCursor.close();
            } else {
            	count = 0;
            }
            groupText += " (" + count + ")";
            // replace with the real values
            groups.remove(DISPLAY_GROUP_INDEX_ALL_CONTACTS);
            groups.add(DISPLAY_GROUP_INDEX_ALL_CONTACTS, groupText);

            groupIds.remove(DISPLAY_GROUP_INDEX_ALL_CONTACTS);
            groupIds.add(DISPLAY_GROUP_INDEX_ALL_CONTACTS, Long.valueOf(0));

            prefStrings.remove(0);
            prefStrings.add(0,"");

            currentIndex = DISPLAY_GROUP_INDEX_ALL_CONTACTS;
            mDisplayGroups = groups.toArray(new CharSequence[groups.size()]);
            mGroupIds = groupIds.toArray(new Long[groupIds.size()]);
            mDisplayGroupOriginalSelection = currentIndex;
        } finally {
            cursor.close();
        } */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SUBACTIVITY_ACCOUNT_FILTER: {
                if (resultCode == RESULT_OK) {
                    final ContactListFilter filter = (ContactListFilter)
                            data.getParcelableExtra(AccountFilterActivity.KEY_EXTRA_CONTACT_LIST_FILTER);
                    if (filter == null) {
                        return;
                    }
                    if (filter.filterType == ContactListFilter.FILTER_TYPE_CUSTOM) {
                        mFilter = ContactListFilter.createFilterWithType(ContactListFilter.FILTER_TYPE_CUSTOM);
                    } else {
                        mFilter = filter;
                    }
                    /*2013-1-8, add by amt_sunzhao for SWITCHUITWO-345 */ 
                    String accountType = null;
                    if(null != mFilter) {
                    	accountType = mFilter.accountType;
                    }
                    if(HardCodedSources.ACCOUNT_TYPE_CARD.equals(accountType)) {
                    	findViewById(R.id.list_header_groups).setEnabled(false);
                    }
                    /*2013-1-8, add end*/ 
                }
                break;
            }
        	case MULTI_PICK_MIXED_PHONEEMAIL_REQUEST:
        		//TODO: for mixed.
        		break;

        	case MULTI_PICK_PHONE_EMAIL_ONLY_REQUEST:
              if (resultCode == RESULT_CANCELED) {
                  // do nothing return;
                	clearAll();
              } else {
                  ArrayList<String> uris = new ArrayList<String>();
                	long[] dataIds = data.getLongArrayExtra(MultiplePickPreviewActivity.EXTRA_PICK_DATA_ID);
                	int count = dataIds.length;
                  if (canAddToList(count) < 0) {
                 	    count = mLimitSize;
	                    for (int i = 0; i < count; i++) {
	                		uris.add(Uri.withAppendedPath(Data.CONTENT_URI, java.lang.Long.toString(dataIds[i])).toString());
	                    }
                      createLimitReturnDialog(uris).show();
                	}
                	else if (count > 0) {
	                    for (int i = 0; i < count; i++) {
	                		uris.add(Uri.withAppendedPath(Data.CONTENT_URI, java.lang.Long.toString(dataIds[i])).toString());
	                    }
	                    Intent intent = new Intent();
	                    intent.putStringArrayListExtra(Intent.EXTRA_STREAM, uris);
	                    setResult(RESULT_OK, intent);
                      finish();
                	} else {
                		  setResult(RESULT_CANCELED);
                      finish();
                  }
              }
              break;

        	case SUBACTIVITY_DISPLAY_GROUP:
        	    mIsSubActivityLaunched = false;
        	    Log.v(TAG, "return from DISPLAY_GROUP activity, need refresh the list.");
        	    // Mark as just created so we re-run the view query
                mJustCreated = true;
        	    break;

            case SUBACTIVITY_LOAD_SIM:
                if (resultCode == RESULT_CANCELED) {
                	Log.v(TAG, "SUBACTIVITY_LOAD_SIM cancelled");
                    // not finish SIM loading, quit.
                    setResult(RESULT_CANCELED);
                    finish();
                } else {
                    Log.v(TAG, "SIM loaded and DB ready");
                    mIsSimReady = true;
                    enterMultiPicker(null);
                }
                break;

            case SUBACTIVITY_SEARCH:
                mIsSubActivityLaunched = false;
                Log.v(TAG, "Multi-Pick_Search returned with ret_code: " + resultCode);
                if ( resultCode == RESULT_OK) {
                	ArrayList<String> uris = new ArrayList<String>();
                    //Log.v(TAG, "RESULT_OK returned from Search, mdata: " + data.getStringExtra("com.android.contacts.MultiPickerResult"));
                    //long start = System.currentTimeMillis();
                    uris = data.getStringArrayListExtra(Intent.EXTRA_STREAM);
                    final HashMap<Uri, Boolean> passedInUris = new HashMap<Uri, Boolean>();
                    int selectedCnt = 0;
                    int n = uris.size();
                    Log.v(TAG, "Total uri number selected from Search: " + n);

                    Object [] a = uris.toArray();
                    Uri [] uriList = new Uri[n];
                    for(int i=0; i<n; i++) {
                    	//Log.v(TAG, "RESULT_OK returned from Search with URI data["+ i + "] = " + a[i]);
                    	uriList[i] = Uri.parse(a[i].toString());
                    	passedInUris.put(uriList[i], true);
                    }

                    // since only the checked entries returned, need clear all check flags for the current_list first !
                    if (mChecked != null) {
                        int count = mChecked.length;
                        //Log.v(TAG, "current list flags count = "+count+" ALL to be cleared");
                        for (int i = 0; i < count; i++) {
                        	// count selected number
                        	if (mChecked[i]) {
                        	    mChecked[i] = false;
                        	    selectedCnt ++;
                        	}
                        }

                        // clear 'All' checkbox
                        if (mSelectAll) {
                        	 //Log.v(TAG, "UN-check 'ALL'");
                        	 mSelectAll = false;
                        	((CheckBox)findViewById(R.id.checkbox)).toggle();
                        }
                    }
                    // calculate checked number in none-current listed group
                    int checkedCntInOther = mFullListUri4Return.size() - selectedCnt;
                    //int checkedCntInOther = ContactsUtils.getNoLinkageMemberCount(mFullListUri4Return) - selectedCnt;
                    //Log.v(TAG, "get returned uris, selectedCnt="+selectedCnt+", cnt_in_backgournd = "+checkedCntInOther+", elapse time: "+(System.currentTimeMillis()-start));

                    // refresh current list with the returned flags
                    int checkedCntInCurrent = 0;
                    if (mCurrentGroupUri4Return != null && uriList != null) {
                    	int fullLen = mCurrentGroupUri4Return.length;

                    	for (int i = 0; i < fullLen; i++) {
                    		if (passedInUris.containsKey(mCurrentGroupUri4Return[i]) && i<mChecked.length) {
                    		    // found
                    		    mChecked[i] = true;
                    		    checkedCntInCurrent ++;
                    		}
                    	}
                    }

                    // refresh the display elements only
                    if (mPickerAdapter != null) {
                        Cursor cursor = mPickerAdapter.getCursor();
                        if (cursor != null && !cursor.isClosed()) {
                        	//Log.v(TAG, "force to refresh by requery() instead of updateList()");
                        	cursor.requery();

                            //update the selected count info
                            Button done = (Button) findViewById(R.id.ok);
                            if (done != null) {
                                int memberCount = checkedCntInOther + checkedCntInCurrent;
                            	CharSequence newText = getString(R.string.menu_done) + " (" + memberCount + ")";
                                Log.v(TAG, "done.setText: " + newText);
 				                done.setText(newText);
                                if (memberCount > 0) {
                                	done.setEnabled(true);
                                	findViewById(R.id.preview).setEnabled(true);
                                } else {
                                	done.setEnabled(false);
                                	findViewById(R.id.preview).setEnabled(false);
                                }
                            }

                            // update "All" checkbox
                            if (checkedCntInCurrent == cursor.getCount()) {
                            	mSelectAll = true;
                            	((CheckBox)findViewById(R.id.checkbox)).setChecked(true);
                            } else {
                            	mSelectAll = false;
                            	((CheckBox)findViewById(R.id.checkbox)).setChecked(false);
                            }
                        }
                    }

            	    // sync checked mark to F
            	    syncCheckedStatus(false, mCurrentGroupUri4Return, mChecked);
            	    //clearAll();
            	    mListItemClickedCount = 0;
            	    mSynedSearchReturn = true;
                    //Log.v(TAG, "sync returned uris, elapse time: "+(System.currentTimeMillis()-start));
                } else if (resultCode == RESULT_MULTIPICKER_SEARCH_ONSTOP ) {
                	Log.v(TAG, "search_view by onStop() returned");
                	mSearchViewExitOnStop = true;
                } else {
                	Log.v(TAG, "NONE-RESULT_OK or RESULT_CANCEL returned from Search, no data");
                }
                break;
        }
    }

    private void updateList() {
        if (mSynedSearchReturn) {
            mSynedSearchReturn = false;
    	    Log.v(TAG, "updateList(), do nothing to speed up display refresh !");
            return;
        } else {
            Log.v(TAG, "updateList(), clear globals");
        }

        // sync checked status from current group to checked_list
        syncCheckedStatus(false, mCurrentGroupUri4Return, mChecked);
        mChecked = null;
        mBaseUri4Return = null;
        mCurrentGroupUri4Return = null;
        mListItemClickedCount = 0;
//        mFullListChecked = null;
//        mFullListUri4Return.clear();

        // reset 'All' checkbox status and view
        mSelectAll = false;
        ((CheckBox)findViewById(R.id.checkbox)).setChecked(false);

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
            		return buildSectionIndexerUri(RawContacts.CONTENT_URI);
                } else if (mIntentGroup > 0 || mLinkageIdInc > 0) {
            		// specified group
            		return buildSectionIndexerUri(RawContacts.CONTENT_URI);
            	} else {
            	    if (mIntentCopyD2C == COPY_CCARD_TO_DEVICE
            	        || mIntentCopyD2C == COPY_GCARD_TO_DEVICE
            	        || mIntentCopyD2C == COPY_DEVICE_TO_CARD) {
            	        return buildSectionIndexerUri(RawContacts.CONTENT_URI);
            	    } else {
                        //return Contacts.CONTENT_URI;
                        // list by raw_contacts by querying one table only to enhance searching performance
            	        return buildSectionIndexerUri(RawContacts.CONTENT_URI);
                    }
                }
            }
            case MODE_LEGACY_PICK_PERSON: {
                return People.CONTENT_URI;
            }
            case MODE_PICK_PHONE: {
                // normal all phone entries
                return buildSectionIndexerUri(Phone.CONTENT_URI);
            }
            case MODE_LEGACY_PICK_PHONE: {
                return Phones.CONTENT_URI;
            }
            case MODE_PICK_MMS_MIXED: {
            	if (OPTION_EMAIL_ONLY == mCurrentMmsKind) {
            		return buildSectionIndexerUri(Email.CONTENT_URI);
            	} else {
            		// use phone's
            		return buildSectionIndexerUri(Phone.CONTENT_URI);
            	}
            }
            case MODE_PICK_EMAIL: {
                // normal all email entries
                return buildSectionIndexerUri(Email.CONTENT_URI);
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
    		}
            if (mDisplayOnlyPhones) {
                return org +  " AND " + CLAUSE_ONLY_PHONES;
            } else {
                return org;
            }
        }
        return org;
    }

    private void prepareSelectionAndArgs() {
        mSelection = getContactSelection();
        mSelectionArgs = null;
        StringBuilder selectionBd = new StringBuilder();
        if (!TextUtils.isEmpty(mSelection)) {
            selectionBd.append(mSelection);
        } else {
            selectionBd.append("1");
        }
        if (mFilter !=null) {
            List<String> selectionArgsList = new ArrayList<String>();
            //Log.v(TAG, "prepareSelectionAndArgs mFilter.filterType = "+mFilter.filterType);
            switch (mFilter.filterType) {
                case ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS: {
                    // We have already added directory=0 to the URI, which takes care of this
                    // filter
                    break;
                }
                case ContactListFilter.FILTER_TYPE_SINGLE_CONTACT: {
                    // We have already added the lookup key to the URI, which takes care of this
                    // filter
                    break;
                }
                case ContactListFilter.FILTER_TYPE_STARRED: {
                    if ((mMode == MODE_PICK_CONTACT) || (mMode == MODE_DEFAULT)) {
                        selectionBd.append(" AND " + RawContacts.STARRED + "!=0");
                    }
                    break;
                }
                case ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY: {
                    // We don't support it because Rawcontacts and data table doesn't have HAS_PHONE_NUMBER field
                    //selectionBd.append(" AND " + Contacts.HAS_PHONE_NUMBER + "=1");
                    Log.w(TAG, "WARNING! a18768 FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY");
                    break;
                }
                case ContactListFilter.FILTER_TYPE_CUSTOM: {
                    if ((mMode == MODE_PICK_CONTACT) || (mMode == MODE_DEFAULT)) {
                        selectionBd.append(
                                " AND (" + RawContacts.CONTACT_ID + " IN ("
                                        + "SELECT " + Contacts._ID + " FROM visible_contacts" + "))");
                    } else {
                        selectionBd.append(" AND " + CLAUSE_ONLY_VISIBLE);
                    }
                    break;
                }
                case ContactListFilter.FILTER_TYPE_ACCOUNT: {
                    // TODO: avoid the use of private API
                    selectionBd.append(" AND " + RawContacts.ACCOUNT_TYPE + "=?"
                                    + " AND " + RawContacts.ACCOUNT_NAME + "=?");
                    selectionArgsList.add(mFilter.accountType);
                    selectionArgsList.add(mFilter.accountName);
                    if (mFilter.dataSet != null) {
                        selectionBd.append(" AND " + RawContacts.DATA_SET + "=?");
                        selectionArgsList.add(mFilter.dataSet);
                    } else {
                        selectionBd.append(" AND " + RawContacts.DATA_SET + " IS NULL");
                    }
                    mSelectionArgs = selectionArgsList.toArray(new String[0]);
                    break;
                }
            }
        }
        mSelection = selectionBd.toString();
        // Log.v(TAG, "prepareSelectionAndArgs mSelection="+mSelection+"; mSelectionArgs="+mSelectionArgs);
    }

    protected void bindTabs() {
        // add tab - phone
        View phoneTabIndicator = mInflater.inflate(R.layout.multi_picker_tab_phone_indicator,
                                                   mTabWidget.getTabParent(), false);
        phoneTabIndicator.getBackground().setDither(true);
        ((TextView) phoneTabIndicator.findViewById(R.id.multipicker_tab_label)).setText(R.string.menu_phoneOnly);
        //TextView v1 = new TextView(this);
        //v1.setText(R.string.menu_phoneOnly);
        mTabRawContactIdMap.put(mTabWidget.getTabCount(), OPTION_PHONE_ONLY);
        mTabWidget.addTab(phoneTabIndicator);

        // add tab - email
        View emailTabIndicator = mInflater.inflate(R.layout.multi_picker_tab_email_indicator,
                                                   mTabWidget.getTabParent(), false);
        emailTabIndicator.getBackground().setDither(true);
        ((TextView) emailTabIndicator.findViewById(R.id.multipicker_tab_label)).setText(R.string.menu_emailOnly);
        mTabRawContactIdMap.put(mTabWidget.getTabCount(), OPTION_EMAIL_ONLY);
        mTabWidget.addTab(emailTabIndicator);

        selectInitialTab();
        mTabWidget.setVisibility(View.VISIBLE);
        mTabWidget.postInvalidate();
    }

    protected void clearCurrentTabs() {
        mTabRawContactIdMap.clear();
        mTabWidget.removeAllTabs();
    }

    protected int getTabIndexForMmsKind(long mmsKind) {
        int numTabs = mTabRawContactIdMap.size();
        for (int i=0; i < numTabs; i++) {
            if (mTabRawContactIdMap.get(i) == mmsKind) {
                return i;
            }
        }
        return -1;
    }

    protected void selectInitialTab() {
        int selectedTabIndex = 0;

        selectedTabIndex = getTabIndexForMmsKind(mCurrentMmsKind);
        if (selectedTabIndex == -1) {
            // If there was no matching tab, just select the first;
            selectedTabIndex = 0;
        }

        mTabWidget.setCurrentTab(selectedTabIndex);
        onTabSelectionChanged(selectedTabIndex, false);
    }

    public void onTabSelectionChanged(int tabIndex, boolean clicked) {
        mCurrentMmsKind = mTabRawContactIdMap.get(tabIndex);
        // re-populate data for current tab
        //bindData();

        Log.v(TAG, "onTabSelectionChanged(), tabIndex="+tabIndex+", mCurrentMmsKind = "+mCurrentMmsKind+", clicked="+clicked);
        if (clicked) {
            updateList();
        }
    }

    private class GroupSelectedListener implements
    	DialogInterface.OnClickListener, DialogInterface.OnMultiChoiceClickListener {

    	private ArrayList<ContactsUtils.GroupInfo> mGroupList = null;
    	boolean[] mIsChecked = null;
    	int mNumItems = 0;

    	public GroupSelectedListener(ArrayList<ContactsUtils.GroupInfo> groupList) {
    		mGroupList = groupList;
    		mNumItems = mGroupList.size();
    		mIsChecked = new boolean[mNumItems];
    	}

    	public void onClick(DialogInterface dialog, int position, boolean isChecked) {
    		mIsChecked[position] = isChecked;
    	}

    	public void onClick(DialogInterface dialog, int which) {
    		// If the user cancelled the dialog, then do nothing.
    		if (which == DialogInterface.BUTTON_NEGATIVE) {
    			return;
    		}
    		if (which == DialogInterface.BUTTON_POSITIVE) {
    			ArrayList<String> selectedGroupList = new ArrayList<String>();
    			for (int i = 0; i < mNumItems; i++) {
    				if (mIsChecked[i]) {
    					ContactsUtils.GroupInfo pinfo = mGroupList.get(i);
    					selectedGroupList.add(pinfo.title);
    				}
    			}
    			// update the Multiple picker list accroding the group select.
    			updateMultiplePicker(selectedGroupList);
    			return;
    		}
    	}
    }

    private void updateMultiplePicker(ArrayList<String> selectedGroupList) {
		    // if it cost time action, you need run it in another thread
        int count = selectedGroupList.size();
        ArrayList<Uri> dataIdUris = new ArrayList<Uri>();

        for (int i = 0; i < count; i++) {

            String dataType = "";
            if (OPTION_PHONE_ONLY == mCurrentMmsKind)
                dataType = "sms";
            else
                dataType = "email";
            ArrayList<Long> dataList = new ArrayList<Long>();
            int dataListSize = ContactsUtils.getAllDefDataIdOfGroupMembers(getContentResolver(), dataList,
                                   selectedGroupList.get(i), dataType);
            if (dataListSize == 0)
                continue;
            for (Long dataId : dataList) {
                if ( dataId > 0 ) {
                    dataIdUris.add(Uri.withAppendedPath(Data.CONTENT_URI, String.valueOf(dataId)));
                }
            }
        }

        if (dataIdUris.size() <= 0)
            return;
        adjustGroupChecks(dataIdUris);
    }


    private void adjustGroupChecks(ArrayList<Uri> dataIdUris) {
        final ListView list = mListView;

        int curLen = mCurrentGroupUri4Return.length;
        for (int j = 0; j < curLen; j++) {
            if (dataIdUris.contains(mCurrentGroupUri4Return[j])) {
                if (!mChecked[j]) {
            		    mListItemClickedCount++;
                }
                list.setItemChecked(j, true);
                mChecked[j] = true;
            }
        }

        //update the selected count info
        Button done = (Button) findViewById(R.id.ok);
        if (done != null) {
		        int memberCount = mFullListUri4Return.size() + mListItemClickedCount;
            CharSequence newText = getString(R.string.menu_done) + " (" + memberCount + ")";
	          done.setText(newText);
            if (memberCount > 0 ) {
                done.setEnabled(true);
                findViewById(R.id.preview).setEnabled(true);
            }
        }
        // update "All" checkbox
        int i = 0;
        int count = mChecked.length;
        for (i = 0; i < count; i++) {
            if (!mChecked[i])
                break;
        }
        // check if all are checked
        if ((i == count) && (count > 0)) {
            mSelectAll = true;
            ((CheckBox)findViewById(R.id.checkbox)).setChecked(true);
        }

        if (canAddToList(mFullListUri4Return.size() + mListItemClickedCount) <= 0) {
       	    createLimitDialog().show();
        }
    }

    private Dialog createNoGroupDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mDialogContext);
        builder.setTitle(R.string.no_group_warning_title);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(R.string.no_group_visible_text);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ;
            }
        });
        builder.setCancelable(false);
        return builder.create();
    }

    private Dialog createGroupSelectDialog() {
        //Log.v(TAG, "enter createGroupSelectDialog");
        ArrayList<ContactsUtils.GroupInfo> groupList = new ArrayList<ContactsUtils.GroupInfo>();
        //list both visible and invisible groups
        ContactsUtils.queryGroupInfoByAccount(this, null, null, groupList);
        int numItems = groupList.size();

        if (numItems <= 0) {
          return createNoGroupDialog();
      }

        //Log.v(TAG, "numItems =" + numItems +", groupList size= " + groupList.size());
        GroupSelectedListener listener = new GroupSelectedListener(groupList);

        CharSequence[] titles = new CharSequence[numItems];
        int i = 0;
        Iterator<ContactsUtils.GroupInfo> it = groupList.iterator();
        while (it.hasNext()) {
            ContactsUtils.GroupInfo pinfo = (ContactsUtils.GroupInfo) it.next();
            titles[i++] = pinfo.titleDisplay;
        }

        //Log.v(TAG, "create createGroupSelectDialog");

        final AlertDialog.Builder builder = new AlertDialog.Builder(mDialogContext);
        builder.setTitle(R.string.select_group_title);
        builder.setMultiChoiceItems(titles, (boolean[])null, listener);
        builder.setPositiveButton(android.R.string.ok, listener);
        builder.setNegativeButton(android.R.string.cancel, listener);
        return builder.create();
    }


    private Dialog createLimitDialog() {

        String warningtext = getString(R.string.limit_warning_text, mLimitSize);

        final AlertDialog.Builder builder = new AlertDialog.Builder(mDialogContext);
        builder.setTitle(R.string.limit_warning_title);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(warningtext);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setCancelable(false);
        return builder.create();
    }

    private Dialog createLimitReturnDialog(ArrayList<String> uris) {

        final ArrayList<String> return_uris = uris;
        String warningtext = getString(R.string.limit_return_text, mLimitSize);

        final AlertDialog.Builder builder = new AlertDialog.Builder(mDialogContext);
        builder.setTitle(R.string.limit_warning_title);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setMessage(warningtext);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.putStringArrayListExtra(Intent.EXTRA_STREAM, return_uris);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        builder.setCancelable(false);
        return builder.create();
    }

    private int canAddToList(int count) {
        if (mHasLimitSize)
            return (mLimitSize - count);
        return mLimitSize;
    }
		@Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean superResult = super.dispatchTouchEvent(ev);
		if (mMode == MODE_PICK_MMS_MIXED) {
        superResult |= mSlideshowGestureDetector.onTouchEvent(ev);
		}
        return superResult;
    }
	// Inner class
    private class SlideGestureListener implements
            GestureDetector.OnGestureListener {
        public boolean onDown(MotionEvent e) {
            mDirection = INVALID;
            return false;
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float deltaX,
                float deltaY) {

            boolean ret = false;
            int distanceX = 0;
			int distanceY = 0;
			if(e1 != null && e2 != null){
				distanceX = (int)e1.getX() - (int) e2.getX();
				distanceY = (int) e1.getY() - (int) e2.getY();
			}
            int absDistanceX = Math.abs(distanceX);
            int absDistanceY = Math.abs(distanceY);

            int currentDirection = INVALID;
            // check the slope of gesture movement. if x distance is smaller
            // than y distance times GESTURE_SLOPE_THRESHOD, the slope is
            // too
            // steep, and the scroll may not mean to switch slide.
            if (absDistanceX > GESTURE_MIN_DISTANCE && absDistanceX > GESTURE_SLOPE_THRESHOLD * absDistanceY) {
                currentDirection = distanceX > 0 ? NEXT : PREV;
                // check the slope of gesture movement. if x distance is smaller
                // than y distance times GESTURE_SLOPE_THRESHOD, the slope is
                // too
                // steep, and the scroll may not mean to switch slide.

                if (currentDirection != mDirection) {
                    int selectedTabIndex = getTabIndexForMmsKind(mCurrentMmsKind);
                    switch (currentDirection) {
                        case NEXT:
                             if (selectedTabIndex < mTabWidget.getTabCount() - 1){
                                selectedTabIndex++;
								mTabWidget.setCurrentTab(selectedTabIndex);
								onTabSelectionChanged(selectedTabIndex, true);
							}
                            ret = true;
                            break;
                        case PREV:
                             if (selectedTabIndex > 0){
                                selectedTabIndex--;
								mTabWidget.setCurrentTab(selectedTabIndex);
								onTabSelectionChanged(selectedTabIndex, true);		
							}	
                            ret = true;
                            break;
                        default:
                            break;
                    }
                    mDirection = currentDirection;
                }
            }
            return ret;
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        public void onLongPress(MotionEvent e) {
            return;
        }

        public void onShowPress(MotionEvent e) {
            return;
        }
    }

    public void checkBoxClicked(View view){
        if (mListView != null && view != null) {
            final ListView l = mListView;
            int position = l.getPositionForView((CheckBox)view);
            if (mChecked != null && position < mChecked.length) {
                boolean isChecked = mChecked[position];
                // update checked item count
                if (!isChecked) {
                    Log.v(TAG, "mListItemClickedCount ++");
                    mListItemClickedCount ++;
                    mChecked[position] = true;
                } else {
                    Log.v(TAG, "mListItemClickedCount --");
                    mListItemClickedCount --;
                    mChecked[position] = false;
                }

                //mChecked[position] = isChecked;
                Log.v(TAG, "onListItemClick(), checked["+position+"]="+isChecked+", clicked_cnt="+mListItemClickedCount+", F_size="+mFullListUri4Return.size());
                //update the selected count info
                Button done = (Button) findViewById(R.id.ok);
                if (done != null) {
                    //int memberCount = ContactsUtils.getNoLinkageMemberCount(mFullListUri4Return);// + mListItemClickedCount;
                    int memberCount = mFullListUri4Return.size() + mListItemClickedCount;
                    CharSequence newText = getString(R.string.menu_done) + " (" + memberCount + ")";
                    //Log.v(TAG, "done button text: "+ newText);
                    done.setText(newText);
                    if (memberCount > 0 ) {
                        done.setEnabled(true);
                        findViewById(R.id.preview).setEnabled(true);
                    } else {
                        done.setEnabled(false);
                        findViewById(R.id.preview).setEnabled(false);
                    }
                }

                // update "All" checkbox
                int k = 0;
                int count = l.getCount();
                for (int i=0; i < count; i++) {
                    if (i < mChecked.length) {
                        if (mChecked[i]) k++;
                    }
                }
                // check if all are checked
                if (k == count) {
                    //Log.v(TAG, "check 'ALL'");
                    mSelectAll = true;
                    ((CheckBox)findViewById(R.id.checkbox)).setChecked(true);
                } else {
                    //Log.v(TAG, "UN-check 'ALL'");
                    mSelectAll = false;
                    ((CheckBox)findViewById(R.id.checkbox)).setChecked(false);
                }

                if (!isChecked) {
                    // only warn once when reach the upper limit
                    if (canAddToList(mFullListUri4Return.size() + mListItemClickedCount) == 0) {
                        createLimitDialog().show();
                    }
                }
	    }
	}		
    }
    
    /*2013-1-8, add by amt_sunzhao for SWITCHUITWO-345 */ 
    private Handler mUIHandler = new Handler();
    
    private ContentObserver mCursorContentObserver = new ContentObserver(mUIHandler) {
    	public void onChange(boolean selfchange) {
    		Log.d(TAG, "onChange E.");
    		startQuery();
    	};
	};
	/*2013-1-8, add end*/ 
	
	private class MultiplePickAdapter extends SimpleCursorAdapter implements SectionIndexer{
		private SectionIndexer mIndexer;
		
		public MultiplePickAdapter(Context context, int layout, Cursor c, String[] from, int[] to){
			super(context,layout,c,from,to);
		}
		
		@Override
        public void changeCursor(Cursor cursor) {			
			super.changeCursor(cursor);

			 updateIndexer(cursor);
		}
		
		private void updateIndexer(Cursor cursor) {
            if (cursor == null) {
                mIndexer = null;
                return;
            }
            Bundle bundle = cursor.getExtras();
            if (bundle.containsKey(ContactCounts.EXTRA_ADDRESS_BOOK_INDEX_TITLES)) {
                String sections[] =
                    bundle.getStringArray(ContactCounts.EXTRA_ADDRESS_BOOK_INDEX_TITLES);
                int counts[] = bundle.getIntArray(ContactCounts.EXTRA_ADDRESS_BOOK_INDEX_COUNTS);
                mIndexer = new ContactsSectionIndexer(sections, counts);
            } else {
                mIndexer = null;
            }
        }
		
		public Object [] getSections() {
            if (mIndexer == null) {
                return new String[] { " " };
            } else {
                return mIndexer.getSections();
            }
        }

        public int getPositionForSection(int sectionIndex) {
            if (mIndexer == null) {
                return -1;
            }

            return mIndexer.getPositionForSection(sectionIndex);
        }

        public int getSectionForPosition(int position) {
            if (mIndexer == null) {
                return -1;
            }

            return mIndexer.getSectionForPosition(position);
        }

	}
	
	private static Uri buildSectionIndexerUri(Uri uri) {
        return uri.buildUpon()
                .appendQueryParameter(ContactCounts.ADDRESS_BOOK_INDEX_EXTRAS, "true").build();
    }
}
