/*
 * Copyright (C) 2010, Motorola, Inc,
 *
 * This file is derived in part from code issued under the following license.
 * Changed for MOTO UI requirements
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
 *
 * @author htf768
 */

//MOT calling code- IKMAIN-4172
package com.android.contacts.dialpad;

import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.util.Constants;
import com.android.contacts.util.ExpirableCache;
import com.android.contacts.util.UriUtils;
import com.motorola.contacts.util.MEDialer;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.ResourceCursorAdapter;
import android.widget.SlidingDrawer;
import android.widget.TextView;

//MOTO MOD BEGIN IKHSS7-2038
import com.motorola.internal.telephony.PhoneNumberUtilsExt;
//MOTO MOD END IKHSS7-2038
import com.motorola.android.telephony.PhoneModeManager;

import java.util.Hashtable;

public class SmartDialerAdapter extends ResourceCursorAdapter {

    private static final String TAG   = "SmartDialerAdapter";
    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End

    private static final String BACKGROUND_THREAD_NAME = "SmartDialer Query";
    private static final String CLOSECURSOR_THREAD_NAME = "SmartDialer CloseCursor";

    static final int START_QUERY_DIGITS_NUMBER  = 1; //MOT Calling code - IKPIM-161

    private Context mContext = null;
    private ContentResolver mResolver = null;
    private DialpadFragment mdialpadFragment = null;
    private View mAdapterView;
    private View mFragmentView;
    private Cursor mMostRecentCursor             = null;
    private boolean misVGA                       = false; //Mot Calling Code IKPIM-363
    private int mDensity; //MOT Calling Code - IKMAIN-21175
    private Cursor mMatchedCursor                = null; //MOT Calling Code IKPIM-447
    private MyAsyncQueryHandler mAsyncQueryHandler= null;
    private MySyncQueryHandler mSyncQueryHandler = null;
    private CharSequence[] mLabelArray           = null;

    private String mQueriedDigits     = "";

    private static final int DEFAULT_PHOTOID      = 0;
    private static final int ADDCOTNACT_PHOTOID   = -1;
    // MOT Calling code - IKSTABLETWOV-2393
    private static final int VM_PHOTO             = -5000;
    private static final int ECM_PHOTO            = -5001;
    // END - MOT Calling code - IKSTABLETWOV-2393

    /** Helper to set up contact photos. */
    private final ContactPhotoManager mContactPhotoManager;

    // MOT Calling Code - IKPIM-447
    private int mHighlightColor;
    private int mHighlightTextColor;
    // END MOT Calling Code - IKPIM-447

    boolean mAutoExpandList = false;

    private ExpirableCache<String, ContactInfo> mContactInfoCache;

    private final static int CACHE_SIZE = 100;

    private static final int RECENT_CURSOR  = 0;
    private static final int CONTACT_CURSOR = 1;
    //MOT Calling Code - IKPIM-447
    private static final String[] PHONES_PROJECTION = new String[] {
            Contacts.DISPLAY_NAME, //1 - display_name
            CommonDataKinds.Phone.NUMBER, //2 - data1
            CommonDataKinds.Phone.TYPE, //3 - data2
            CommonDataKinds.Phone.LABEL, //4 - data3
            Contacts.PHOTO_ID, // 5 - photo_id
            Contacts.STARRED, //6 - starred
            Contacts.TIMES_CONTACTED, //7 - times_contacted
            Contacts.SORT_KEY_PRIMARY, //8 - sort key
            Calls.NUMBER, //9 - number
            Calls.DATE, //10 - date
            Calls.TYPE, //11 - type
            Calls.CACHED_NAME, // 12 - name
            Calls.CACHED_NUMBER_TYPE, //13 - numbertype
            Calls.CACHED_NUMBER_LABEL, //14 - numberlabel
            Calls.CACHED_LOOKUP_URI, // 15
            ContactsUtils.CallLog_NETWORK, // 16
            Data.CONTACT_ID, // 17
            "group_id", //18
    };
    private static final int CONTACTS_DISPLAY_NAME_INDEX = 0;
    private static final int CONTACTS_NUMBER_INDEX = 1;
    private static final int CONTACTS_TYPE_INDEX = 2;
    private static final int CONTACTS_LABEL_INDEX = 3;
    private static final int CONTACTS_PHOTO_ID_INDEX = 4;
    private static final int CONTACTS_STARRED_INDEX = 5;
    private static final int CONTACTS_TIMES_INDEX = 6;
    private static final int CONTACTS_SORT_INDEX = 7;
    private static final int RC_NUMBER_COLUMN_INDEX = 8;
    private static final int RC_DATE_COLUMN_INDEX = 9;
    private static final int RC_CALL_TYPE_COLUMN_INDEX = 10;
    private static final int RC_CALLER_NAME_COLUMN_INDEX = 11;
    private static final int RC_CALLER_NUMBERTYPE_COLUMN_INDEX = 12;
    private static final int RC_CALLER_NUMBERLABEL_COLUMN_INDEX = 13;
    private static final int RC_CALLER_LOOKUP_URI_COLUMN_INDEX = 14;
    private static final int RC_CALL_NETWORK_TYPE_COLUMN_INDEX = 15;
    private static final int CONTACTS_ID = 16;
    private static final int GROUP_ID_INDEX = 17;
    //END MOT Calling Code - IKPIM-447
    private MyObserver mChangeObserver; // MOT Calling code - IKSTABLETWOV-2897
    // MOT Calling code - IKSTABLETWOV-2768
    /**
     * To filter same contacts which is from different source,
     * app remove the "Contacts._ID" from projection to make "distinct" working.
     */
    private static final String[] PEOPLE_PHONE_PROJECTION = new String[] {
            Contacts.DISPLAY_NAME,
            Phone.NUMBER,
            Phone.TYPE,
            Phone.LABEL,
            Contacts.PHOTO_ID,
            Data.CONTACT_ID
    };
    private static final int CONTACTS_COLUMN_NUMBER = CONTACTS_PHOTO_ID_INDEX + 2;
    private static final int EXACT_MATCHED_CONTACTS_ID = 5;
    // END - MOT Calling code - IKSTABLETWOV-2768

    static final int QUERY_MOST_RECENT     = 1000;
    //MOT Calling code - IKPIM-447
    static final int QUERY_FUZZY_MATCHED   = 1001;
    static final int QUERY_EXACT_MATCHED   = 1002;
    static final int QUERY_STRING_MATCHED  = 1003;
    //END MOT Calling code - IKPIM-447
    static final int QUERY_CONTACTS_DETAIL = 1004;

    //Mot Calling Code IKPIM-363
    enum ScreenSize {
        VGA,
        UNKNOWN
    }

    private String mCurrentCountryIso;

    private static final Hashtable<Integer, ScreenSize> SCREEN_SIZE_MAP =
            new Hashtable<Integer, ScreenSize>();

    static { SCREEN_SIZE_MAP.put(480 * 640, ScreenSize.VGA); } // VGA
    //Mot Calling Code IKPIM-363 -End
    private boolean mDarkTheme;

    private boolean mCallTimeDisplayByDateTimeFormat = false;

    /**
    * Construction function
    */
    public SmartDialerAdapter(Context context, int layout, View fragmentView, String countryIso, Object o) {
        super(context,layout,null);

        mdialpadFragment = (DialpadFragment) o;
        mContext = context; // Motorola, July-04-2011, IKMAIN-21909
        mResolver = mContext.getContentResolver();
        mCallTimeDisplayByDateTimeFormat = PhoneModeManager.IsCallTimeDisplayByDateTimeFormat();
        mAdapterView = LayoutInflater.from(mContext).inflate(layout, null);
        mFragmentView = fragmentView;
        mCurrentCountryIso = countryIso;
        //MOT Calling Code IKPIM-447
        mHighlightColor = mContext.getResources().getColor(R.color.highlight);
        mDarkTheme = mContext.getResources().getBoolean(R.bool.contacts_dark_ui);
        mHighlightTextColor = Color.WHITE;
        //END MOT Calling Code IKPIM-447

        mContactPhotoManager = ContactPhotoManager.getInstance(mContext);
        mContactInfoCache = ExpirableCache.create(CACHE_SIZE);

        //Mot Calling Code IKPIM-363
        //TO Find out if it's a VGA Screen or not
        //VGA used hdpi
        if (getScreenSize(context)== ScreenSize.VGA){
            misVGA = true;
            if(DBG) log("It is VGA Screen");
        }
        //Mot Calling Code IKPIM-363 -End

        mDensity = mContext.getResources().getDisplayMetrics().densityDpi; //MOT Calling Code - IKMAIN-21175

        // AsyncQueryHandler is used to query main list, it has the default priority
        mAsyncQueryHandler = new MyAsyncQueryHandler(context);

        // This thread is used to query detail contactInfo/photo, it has the lower priority.
        HandlerThread thread = new HandlerThread(BACKGROUND_THREAD_NAME,
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        //MOT Calling oode - IKMAIN-22581
        Looper threadlooper = thread.getLooper();
        if (threadlooper != null) {
            mSyncQueryHandler = new MySyncQueryHandler(threadlooper);
        }
        //End MOT Calling oode - IKMAIN-22581
        mChangeObserver = new MyObserver(); // MOT Calling code - IKSTABLETWOV-2897
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView nameView = (TextView) view.findViewById(R.id.sd_name);
        TextView numberView = (TextView) view.findViewById(R.id.sd_number);
        QuickContactBadge picView = (QuickContactBadge) view.findViewById(R.id.sd_caller);
        View callButton = (View)view.findViewById(R.id.sd_call_icon);
        View callerDetail = (View)view.findViewById(R.id.sd_callerDetail);
        ImageView callType = (ImageView) view.findViewById(R.id.sd_call_type);
        ImageView networkType = (ImageView) view.findViewById(R.id.sd_network_type);
        TextView dateView = (TextView) view.findViewById(R.id.sd_call_date);
        // MOT Calling code - IKMAIN-20311
        picView.setVisibility(View.VISIBLE); // Motorola, June-23-2011, IKPIM-20891
        // END - MOT Calling code - IKMAIN-20311
        view.setClickable(false);
        setItemView(cursor, nameView, numberView, callerDetail, callType, networkType, picView,
                dateView, callButton);
    }

    @Override
    protected void init(Context context, Cursor c, boolean autoRequery) {
        boolean cursorPresent = c != null;
        mAutoRequery = autoRequery;
        mCursor = c;
        mDataValid = cursorPresent;
        mContext = context;
        mRowIDColumn = 0;
    }

    @Override
    public void changeCursor(Cursor cursor) {
        synchronized(this){
        if (cursor == mCursor) {
            return;
        }
        updateSearchingResults(cursor);//ChinaDev
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = cursor;
        if (cursor != null) {
            mRowIDColumn = 0;
            mDataValid = true;
            notifyDataSetChanged();
        } else {
            mRowIDColumn = -1;
            mDataValid = false;
            notifyDataSetInvalidated();
        }
        }
    }

    @Override
    protected void onContentChanged() {
        if (DBG) log("onContentChanged activityPaused = " + activityPaused);
        if (!activityPaused) {
            String inputDigits = getInputDigits();
            if((inputDigits != null) && (inputDigits.length() >= START_QUERY_DIGITS_NUMBER)) {
                startQuery(QUERY_STRING_MATCHED);//MOT Calling code - IKPIM-447
            } else {
                startQuery(QUERY_MOST_RECENT);
            }
        }
    }

    // MOT Calling code - IKSTABLETWOV-2897
    @Override
    public long getItemId(int position) {
        return position;
    }
    // END - MOT Calling code - IKSTABLETWOV-2897
    /**ChinaDev
    * This function is used to update view of single item
    */
    /*void updateSingleItemAndTitle() {

        Cursor cursor;
        // title view include expander and searching result textView.
        View title           = mFragmentView.findViewById(R.id.sd_title);
        ImageView expander   = (ImageView) mFragmentView.findViewById(R.id.sd_expandIcon);
        TextView matchResult = (TextView) mFragmentView.findViewById(R.id.sd_title_header);
        ImageView divider    = (ImageView) mFragmentView.findViewById(R.id.sd_divider);
        QuickContactBadge picView = (QuickContactBadge) mFragmentView.findViewById(R.id.sd_caller);
        View callerDetail         = (View) mFragmentView.findViewById(R.id.sd_callerDetail);
        TextView nameView         = (TextView) mFragmentView.findViewById(R.id.sd_name);
        TextView numberView       = (TextView) mFragmentView.findViewById(R.id.sd_number);
        ImageView callType        = (ImageView) mFragmentView.findViewById(R.id.sd_call_type);
        TextView dateView         = (TextView) mFragmentView.findViewById(R.id.sd_call_date);

        String inputDigits = getInputDigits();
        title.setEnabled(false);
        title.setVisibility(View.VISIBLE);
        nameView.setVisibility(View.VISIBLE);
        numberView.setVisibility(View.VISIBLE);
        expander.setVisibility(View.GONE);
        // MOT Calling code - IKMAIN-20311
        if (picView != null) {
            picView.setVisibility(View.VISIBLE);
        }
        // END - MOT Calling code - IKMAIN-20311
        // display MostRecent cursor data
        if((inputDigits != null) && (inputDigits.length() < START_QUERY_DIGITS_NUMBER)) {
            cursor = getCursor();//MOT Calling code - IKPIM-447
            if((cursor != null) && !cursor.isClosed() && (cursor.moveToFirst())) {
                if (DBG) log("MostRecent data exists");
                if (isFullSingleItemNeeded()){ //MOT Calling Code - IKMAIN-21175
                    matchResult.setText(R.string.sd_most_recent);
                    setItemView(cursor, nameView, numberView, callerDetail, callType,
                            picView, dateView, null);
                } else {
                    title.setVisibility(View.GONE);
                    divider.setVisibility(View.GONE);
                    setCommonView(cursor, nameView, numberView, callerDetail, callType);
                    callType.setVisibility(View.VISIBLE);
                }

            } else { // There is no Most Recent call
                if (DBG) log("MostRecent data doesn't exist");
                matchResult.setText(R.string.sd_empty_recent);
                nameView.setVisibility(View.GONE);
                numberView.setVisibility(View.GONE);
                callType.setVisibility(View.GONE);
                callerDetail.setOnClickListener(null);

                if (isFullSingleItemNeeded()){ //MOT Calling Code - IKMAIN-21175
                    picView.setVisibility(View.GONE);
                    callType.setVisibility(View.GONE);
                    dateView.setVisibility(View.GONE);
                } else {
                    if (DBG) log("DisplayMetrics.DENSITY_MEDIUM");
                    divider.setVisibility(View.GONE);
                }
             }

        } else { // Display the first item data of searching results
            cursor = getCursor();
            expander.setVisibility(View.VISIBLE);
            // at least has one matched item
            if((cursor != null) && !cursor.isClosed() && (cursor.moveToFirst())) {
                title.setEnabled(true);
                int matchCount = cursor.getCount();
                if (matchCount == 1) {
                    matchResult.setText(matchCount + " " +
                            mContext.getResources().getString(R.string.sd_match));
                } else {
                    matchResult.setText(matchCount + " " +
                            mContext.getResources().getString(R.string.sd_matches));
                }
                ListView mList = (ListView) mFragmentView.findViewById(R.id.sd_list);
                int visibily = mList.getVisibility();
                // MOT Calling code - IKSTABLETWOV-2893
                if (visibily == View.VISIBLE) {
                    expander.setImageResource(com.android.internal.
                        R.drawable.expander_close_holo_dark); //MOT Dialer Code - IKHSS7-3963
                } else {
                    expander.setImageResource(com.android.internal.
                        R.drawable.expander_open_holo_dark); //MOT Dialer Code - IKHSS7-3963
                }
                // END - MOT Calling code - IKSTABLETWOV-2893
                if (isFullSingleItemNeeded()){ //MOT Calling Code - IKMAIN-21175
                    setItemView(cursor, nameView, numberView, callerDetail, callType,
                            picView, dateView, null);
                } else {
                    callType.setVisibility(View.GONE);
                    if (visibily != View.VISIBLE) {
                        setCommonView(cursor, nameView, numberView, callerDetail, callType);
                        divider.setVisibility(View.VISIBLE);
                        if (numberView.getVisibility() != View.VISIBLE) {
                            callType.setVisibility(View.VISIBLE);
                        }
                        matchResult.setText(matchCount + " ");
                    } else {
                        divider.setVisibility(View.GONE);
                        nameView.setVisibility(View.GONE);
                        numberView.setVisibility(View.GONE);
                        callerDetail.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                mdialpadFragment.showList(false);//MOT Calling code IKPIM-660
                            }
                        });
                        callType.setVisibility(View.GONE);
                    }
                }

            } else { //there is no matched item
                if (DBG) log("no matched item");
                matchResult.setText(mContext.getResources().getString(R.string.sd_no_result));
                expander.setVisibility(View.GONE);
                callType.setVisibility(View.GONE);
                nameView.setVisibility(View.GONE);
                numberView.setVisibility(View.GONE);
                callerDetail.setOnClickListener(null);

                if (isFullSingleItemNeeded()){ //MOT Calling Code - IKMAIN-21175
                    picView.setVisibility(View.GONE);
                    dateView.setVisibility(View.GONE);
                } else {
                    divider.setVisibility(View.GONE);
                }
            }
        }
    }*/

    /**
    * This function is used to create one item view for list or single item
    */
    ContactInfo setCommonView(Cursor cursor, TextView nameView, TextView numberView,
            View callerDetail, ImageView callType, ImageView networkType) {
        final  String number; //have to declare it as final because it will be used by inner class.
        String displayNumber;
        String displayLabel = null;
        ContactInfo ci;

        if (getCurCursorType(cursor) == RECENT_CURSOR) { //Current cursor is from recent query
            number = cursor.getString(RC_NUMBER_COLUMN_INDEX);
            int type = cursor.getInt(RC_CALL_TYPE_COLUMN_INDEX);
            int network = cursor.getInt(RC_CALL_NETWORK_TYPE_COLUMN_INDEX);
            // get contact info for this recentcall number, if no info found in cache,
            // will send message to background thread to query
            ci = getContactInfoToDisplay(number, cursor);
            // MOT Calling code - IKSTABLETWOV-2393
            if (PhoneNumberUtils.isLocalEmergencyNumber(number, mContext)) {
                displayLabel = "";
            // END - MOT Calling code - IKSTABLETWOV-2393
            } else if (!TextUtils.isEmpty(ci.name)) {
                displayLabel = getDisplayLabel(ci.type, ci.label);
            }
            switch (type) {
                case Calls.INCOMING_TYPE:
                    callType.setImageResource(R.drawable.ic_call_incoming_holo_dark);
                    break;
                case Calls.OUTGOING_TYPE:
                    callType.setImageResource(R.drawable.ic_call_outgoing_holo_dark);
                    break;
                case Calls.MISSED_TYPE:
                    callType.setImageResource(R.drawable.ic_call_missed_holo_dark);
                    break;
                case Calls.VOICEMAIL_TYPE:
                    callType.setImageResource(R.drawable.ic_call_voicemail_holo_dark);
                    break;
                default:
                    callType.setImageBitmap(null);
                    callType.setVisibility(View.INVISIBLE);
                    break;
            }
            switch (network) {
                case TelephonyManager.PHONE_TYPE_GSM:
                    networkType.setImageResource(R.drawable.ic_call_log_list_gsm_call);
                    break;
                case TelephonyManager.PHONE_TYPE_CDMA:
                    networkType.setImageResource(R.drawable.ic_call_log_list_cdma_call);
                    break;
                default:
                    networkType.setImageBitmap(null);
                    networkType.setVisibility(View.INVISIBLE);
                    break;
            }
        } else { //Current cursor is from contacts query, need to parse it with different way.
            number = cursor.getString(CONTACTS_NUMBER_INDEX);
            ci = new ContactInfo();
            ci.name   = cursor.getString(CONTACTS_DISPLAY_NAME_INDEX);
            ci.label  = cursor.getString(CONTACTS_LABEL_INDEX);
            ci.type   = cursor.getInt(CONTACTS_TYPE_INDEX);
            ci.photoId= 0; //need not to query photo
            displayLabel = getDisplayLabel(ci.type, ci.label);
        }
        // format number to add '-' when display
        if (!TextUtils.isEmpty(number)) {
            /* MOTO MOD BEGIN IKHSS7-2038
             * Changed calling PhoneNumberUtils.formatNumber API to New API
             * PhoneNumberUtilsExt.formatNumber for Hyphensation Feature 35615
            */
            displayNumber = PhoneNumberUtilsExt.formatNumber(mContext, number, null, mCurrentCountryIso);
            //MOTO END IKHSS7-2038
        } else {
            displayNumber = "";
        }
        // set listener to populate number into EditBox when clicked
        callerDetail.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                 if (!TextUtils.isEmpty(number)) {
                     EditText et = (EditText)mFragmentView.findViewById(R.id.digits);
                     //ChinaDev
                     mdialpadFragment.setFormattedDigits(PhoneNumberUtils.stripSeparators(number), null);
                     //et.setText(PhoneNumberUtils.stripSeparators(number)); // MOT Calling Code - IKMAIN-18641
                     et.setSelection(et.getText().length());
                     mdialpadFragment.getActivity().invalidateOptionsMenu(); //MOT Dialer Code - IKHSS7-1033
                     mAutoExpandList = false;
                     mdialpadFragment.showList(false);//MOT Calling code IKPIM-660
                     MEDialer.setSmartDialer(true); // Motorola, FTR 36344, Apr-19-2011, IKPIM-384
                 }
            }
        });
        String inputDigits = getInputDigits();
        if (TextUtils.isEmpty(ci.name)) {
            numberView.setVisibility(View.GONE);
            nameView.setText(displayNumber, TextView.BufferType.SPANNABLE);
            if((inputDigits != null) && (inputDigits.length() >= START_QUERY_DIGITS_NUMBER)) {
                Highlighter.highlight((Spannable)nameView.getText(), mQueriedDigits,//MOT Calling code - IKMAIN-17457
                                         true, 0, mHighlightColor, mHighlightTextColor);//MOT Calling code - IKSTABLETWOV-2767 IKPIM-447
            }
        } else {
            numberView.setVisibility(View.VISIBLE);
            nameView.setText(ci.name,TextView.BufferType.SPANNABLE);
            numberView.setText(displayLabel+" "+ displayNumber, TextView.BufferType.SPANNABLE);
            int matchOffset;
            if (displayLabel != null) {
                matchOffset = displayLabel.length() + 1;
            } else {
                matchOffset = 1;
            }
            if((inputDigits != null) && (inputDigits.length() >= START_QUERY_DIGITS_NUMBER)) {
                //MOT Calling code - IKSTABLETWOV-2767 IKMAIN-12107 IKPIM-447
                Highlighter.highlight((Spannable)numberView.getText(), mQueriedDigits,//MOT Calling code - IKMAIN-17457
                                        true, matchOffset ,mHighlightColor, mHighlightTextColor);
                Highlighter.highlight((Spannable)nameView.getText(),mQueriedDigits,//MOT Calling code- IKMAIN-17457
                                        false, 0, mHighlightColor, mHighlightTextColor);
                //End MOT Calling code - IKSTABLETWOV-2767 IKMAIN-12107 IKPIM-447
            }
        }
        return ci;
    }
    /**
    * This function is used to create one item view for list or single item
    */
    void setItemView(Cursor cursor, TextView nameView, TextView numberView, View callerDetail,
            ImageView callType, ImageView networkType, QuickContactBadge picView,
            TextView dateView, View callButton) {
        final  String number; //have to declare it as final because it will be used by inner class.
        long contactId = 0;
        String rcLookUpUri = null;
        ContactInfo ci;

        ci = setCommonView(cursor, nameView, numberView, callerDetail, callType, networkType);
        if (getCurCursorType(cursor) == RECENT_CURSOR) { //Current cursor is from recent query
            number = cursor.getString(RC_NUMBER_COLUMN_INDEX);
            rcLookUpUri = cursor.getString(RC_CALLER_LOOKUP_URI_COLUMN_INDEX);
            long date = cursor.getLong(RC_DATE_COLUMN_INDEX);
            int flags = DateUtils.FORMAT_ABBREV_RELATIVE;
            if(mCallTimeDisplayByDateTimeFormat == true && mContext != null){
                dateView.setText(DateUtils.formatDateTime(mContext, date,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_YEAR));
            }else{
                dateView.setText(DateUtils.getRelativeTimeSpanString(date,
                        System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, flags));
            }

            dateView.setVisibility(View.VISIBLE);
            callType.setVisibility(View.VISIBLE);
            networkType.setVisibility(View.VISIBLE);
        } else {
            int columnCount = cursor.getColumnCount();
            if (columnCount == CONTACTS_COLUMN_NUMBER) {
                contactId = cursor.getLong(EXACT_MATCHED_CONTACTS_ID);
            } else {
                contactId = cursor.getLong(CONTACTS_ID);
            }
            number = cursor.getString(CONTACTS_NUMBER_INDEX);
            ci.photoId = cursor.getLong(CONTACTS_PHOTO_ID_INDEX);
            callType.setVisibility(View.GONE);
            networkType.setVisibility(View.GONE);
            dateView.setVisibility(View.GONE);
        }
        if (callButton != null) {
            callButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                     if (!TextUtils.isEmpty(number)) {
                         // number of current item is combined with this listener
                        //StickyTabs.saveTab(mActivity, mActivity.getIntent());
                         // BEGIN Motorola, FTR 36344, Apr-19-2011, IKPIM-384
                         final Intent intent;
                         if (ContactsUtils.haveTwoCards(mContext)) {
                             intent = new Intent(ContactsUtils.TRANS_DIALPAD);
                             intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("phoneNumber", number);
                         } else {
                            intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                                   Uri.fromParts("tel", number, null));
                         }
                         if (MEDialer.getQwertyKeypad()) {
                             MEDialer.onDial(mContext, intent, MEDialer.SmartDialFrom.QWERTY);
                         } else {
                             MEDialer.onDial(mContext, intent, MEDialer.SmartDialFrom.SOFT);
                         }
                         mContext.startActivity(intent);
                         // END IKPIM-384
                         EditText et = (EditText)mFragmentView.findViewById(R.id.digits);
                         et.getText().clear();
                     }
                }
            });
        }

        if (TextUtils.isEmpty(ci.name)) {
            numberView.setVisibility(View.INVISIBLE);
        }

        setPhoto(picView, ci.photoId, number, contactId, rcLookUpUri);
    }

    /**
    * This function is used to get the cursor type by column count of cursor
    * group_id == 0 Most Recent Query
    * group_id == 1 Fuzzy Query with matched Contacts item
    * group_id == 2 Fuzzy Query with matched Recent item
    */
    private int getCurCursorType(Cursor cursor) {
        int columnCount = cursor.getColumnCount();
        //MOT Calling Code - IKPIM-447
        if (columnCount == CONTACTS_COLUMN_NUMBER) {// Exact Match Query
            return CONTACT_CURSOR;
        }else{
            String group_id = cursor.getString(GROUP_ID_INDEX);
        if (group_id.equals("1")) {
                return CONTACT_CURSOR;
            }else if (group_id.equals("0") ||group_id.equals("2")) {
                return RECENT_CURSOR;
            }else{
                    if (DBG) log("cursor type is wrong, group_id = " + group_id);
                    throw new IllegalArgumentException("the cursor type is wrong!");
            }
        } //END MOT Calling Code - IKPIM-447
    }

    /**
    * This function is used to compose the dispaly label of number
    */
    private String getDisplayLabel(int type, CharSequence label) {
        if (mLabelArray == null) {
            mLabelArray  = mContext.getResources().getTextArray(com.android.internal.R.array.phoneTypes);
        }
        return Phone.getDisplayLabel(mContext, type, label, mLabelArray).toString();
    }

    /**
    * This function is used to get current input digits
    */
    private String getInputDigits() {
        String str = SmartDialerUtil.stripSeparators((mdialpadFragment.getDigitsWidget()).getText().toString());
        if (DBG) log ("getInputDigits =" + str);
        return str;
    }

    public void invalidateCache() {
        mContactInfoCache.expireAll();
    }

    /**********************************************************************
     * This session implemented main list query handler
     *********************************************************************/
    private long mContactStartTime  = 0;
    private static final Uri SMARTDIALER_FUZZY_CONTENT_URI = Uri.parse("content://"
                + ContactsContract.AUTHORITY + "/smartdialer/fuzzy_match_query");
    public static final Uri SMARTDIALER_EXACT_CONTENT_URI = Uri.parse("content://"
                + ContactsContract.AUTHORITY + "/smartdialer/exact_match_query");
    void startQuery(int token) {
        switch (token) {
            case QUERY_MOST_RECENT: {
                if (DBG) log("start QUERY_MOST_RECENT");
                mQueriedDigits = "";
                if (mAsyncQueryHandler != null) { // MOT Calling code - IKSTABLEFOURV-988
                    mAsyncQueryHandler.cancelOperation(QUERY_MOST_RECENT);
                    mAsyncQueryHandler.cancelOperation(QUERY_FUZZY_MATCHED); //MOT Calling code - IKPIM-447
                    mAsyncQueryHandler.cancelOperation(QUERY_EXACT_MATCHED); //MOT Calling code - IKPIM-447
                    mMainThreadHandler.removeMessages(START_DELAYED_QUERY);
                    // For most recent query, don't select the blocked call type
                    // only 1 item need to be queried out.
                    // MOT Calling code - IKSTABLETWOV-70 IKPIM-447
                    mAsyncQueryHandler.startQuery(token, null, SMARTDIALER_FUZZY_CONTENT_URI,
                            PHONES_PROJECTION, null, null, null);
                }
                break;
            }
            case QUERY_STRING_MATCHED: {
                if (mMainThreadHandler.hasMessages(START_DELAYED_QUERY)){
                    //cancel previous query
                    if (DBG) log("previous query is canceled!");
                    mMainThreadHandler.removeMessages(START_DELAYED_QUERY);
                }
                //MOT Calling Code - IKSTABLE6-3459
                //hack for KPI test, do not delay when input only 1 digit
                String digits = getInputDigits();
                if (digits.length() == 1) {
                    mMainThreadHandler.sendEmptyMessage(START_DELAYED_QUERY);
                } else {
                    mMainThreadHandler.sendEmptyMessageDelayed(START_DELAYED_QUERY, DELAYED_QUERY_TIMER);
                }
                //End MOT Calling Code - IKSTABLE6-3459
                break;
            }
            default : {
                break;
            }
        }
    }

    private class MyAsyncQueryHandler extends AsyncQueryHandler {

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            long currTime = System.currentTimeMillis();
            switch(token) {
                case QUERY_MOST_RECENT: {
                    if (DBG) log("QUERY_MOST_RECENT finish");
                    if (!activityGone) {
                        mMostRecentCursor = cursor;
                        changeCursor(mMostRecentCursor);
                        mdialpadFragment.restoreListState();//ChinaDev
                        //ChinaDev updateSingleItemAndTitle();
                    } else {
                        if (cursor != null) {
                            cursor.close();
                            // dont set cursor to null here because the cursor is input param
                        }
                        //MOT Calling Code - IKPIM-447
                        if (mMostRecentCursor != null) {
                            mMostRecentCursor.close();
                            mMostRecentCursor = null;
                        } //END MOT Calling Code - IKPIM-447
                    }
                    break;
                }
                //MOT Calling Code - IKPIM-447 IKMAIN-18641
                // BEGIN Motorola, July-04-2011, IKMAIN-21909
                case QUERY_FUZZY_MATCHED:
                case QUERY_EXACT_MATCHED: {
                    if (token == QUERY_EXACT_MATCHED) {
                        if (DBG) log("QUERY_EXACT_MATCHED cost time =  " +
                            (currTime - mContactStartTime) +" ms");
                    } else if (token == QUERY_FUZZY_MATCHED) {
                        if (DBG) log("QUERY_FUZZY_MATCHED cost time =  " +
                                (currTime - mContactStartTime) +" ms");
                    }
                // END IKMAIN-21909
                // End MOT Calling Code - IKMAIN-18641
                    if (!activityGone) {
                        mMatchedCursor = cursor;
                        mQueriedDigits = ((String)cookie).toLowerCase(); //MOT Calling code - IKPIM-161
                        changeCursor(mMatchedCursor);
                        mdialpadFragment.restoreListState();//ChinaDev
                        // MOT Calling code - IKSTABLETWOV-7910
                        if (mMatchedCursor != null && mMatchedCursor.getCount() == 0) {
                            mdialpadFragment.showList(false);//MOT Calling code IKPIM-660
                        //MOT Calling code - IKPIM-161
                        } else if (mAutoExpandList) {
                            mdialpadFragment.showList(true);//MOT Calling code IKPIM-660
                        }
                        //End -MOT Calling code - IKPIM-161 IKSTABLETWOV-7910
                        //ChinaDev updateSingleItemAndTitle();
                    } else {
                        if (cursor != null) {
                            cursor.close();
                        }
                        if (mMatchedCursor != null) {
                            mMatchedCursor.close();
                            mMatchedCursor = null;
                        }
                    }
                    break;
                } //END MOT Calling Code - IKPIM-447
            }
        }

        public MyAsyncQueryHandler(Context context) {
            super(context.getContentResolver());
        }
    }
    /**********************************************************************
    *  End MyAsyncQueryHandler session
    *********************************************************************/

    /******************************************************************
     * This session implemented a background thread to query detail info
    ******************************************************************/
    private ContactInfo getContactInfoToDisplay(String number, Cursor cursor) {
        ContactInfo ci = null;
        // MOT Calling code - IKSTABLETWOV-2393
        // check voicemail/911 number
        if (mdialpadFragment.getVoiceMailTag(number) != null) { // MOT Calling code - IKSTABLEFOURV-1933
            ci = new ContactInfo();
            ci.name    = mdialpadFragment.getVoiceMailTag(number); // MOT Calling code - IKSTABLEFOURV-1933
            ci.photoId = VM_PHOTO;
            ci.label   = cursor.getString(RC_CALLER_NUMBERLABEL_COLUMN_INDEX);
            ci.type    = cursor.getInt(RC_CALLER_NUMBERTYPE_COLUMN_INDEX);
            return ci;
        } else if (PhoneNumberUtils.isEmergencyNumber(number)) {
            ci = new ContactInfo();
            ci.name    = mContext.getResources().getString(
                    R.string.emergency_call_dialog_number_for_display);
            ci.photoId = ECM_PHOTO;
            ci.label   = "";
            ci.type    = 0;
            return ci;
        }
        // END - MOT Calling code - IKSTABLETWOV-2393

        ci = mContactInfoCache.getPossiblyExpired(number);
        if (ci == null) {
            ci = new ContactInfo();
            ci.name    = cursor.getString(RC_CALLER_NAME_COLUMN_INDEX);;
            ci.label   = cursor.getString(RC_CALLER_NUMBERLABEL_COLUMN_INDEX);;
            ci.type    = cursor.getInt(RC_CALLER_NUMBERTYPE_COLUMN_INDEX);
            if (TextUtils.isEmpty(ci.name)) {
                ci.photoId = ADDCOTNACT_PHOTOID; //unknown photo
            } else {
                ci.photoId = 0; //default photo
            }
        }

        if (mContactInfoCache.get(number) == null) {//contact info expired or not cached
            Message msg = Message.obtain(mSyncQueryHandler, QUERY_CONTACTS_DETAIL, number);
            if (DBG) log("sendContactsMsg = "+ msg);
            msg.sendToTarget();
        }

        if (ci == null) {
            ci = new ContactInfo();
            ci.name    = null;
            ci.label   = null;
            ci.type    = 0;
            ci.photoId = ADDCOTNACT_PHOTOID;
        }
        return ci;
    }

    private void setPhoto(QuickContactBadge view, long photoId, String number, long contactId, String rcLookUpUri) {
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        if (photoId == VM_PHOTO) {
            view.setImageResource(R.drawable.ic_launcher_voicemail); //MOT Calling code - IKMAIN-20475
            view.assignContactFromPhone(null, true);
        } else if (photoId == ECM_PHOTO) {
            view.setImageResource(R.drawable.picture_emergency);
            view.assignContactFromPhone(null, true);
        } else if(photoId < DEFAULT_PHOTOID) {
            view.setImageResource(R.drawable.ic_thb_unknown_caller);
            if (contactId > 0) {
            	view.assignContactUri(contactUri);
            } else {
            	view.assignContactFromPhone(number, true);
            }
        } else {
            if (photoId == DEFAULT_PHOTOID && contactId > 0) {
                mContactPhotoManager.loadDefaultPhoto(view, contactId, false, mDarkTheme);
            } else if (photoId == DEFAULT_PHOTOID && !TextUtils.isEmpty(rcLookUpUri)) {
            	mContactPhotoManager.loadDefaultPhoto(view, UriUtils.parseUriOrNull(rcLookUpUri), false, mDarkTheme);
            } else {
                //mContactPhotoManager.loadPhotoForRC(view, photoId, contactName, mUnknownPhoto);
                mContactPhotoManager.loadPhoto(view, photoId, false, true);
            }
            if (contactId > 0) {
                view.assignContactUri(contactUri);
            } else {
                view.assignContactFromPhone(number, true);
            }
        }
        view.setVisibility(View.VISIBLE);
    }

    private class MySyncQueryHandler extends Handler {
        public MySyncQueryHandler(Looper looper)  {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case QUERY_CONTACTS_DETAIL:
                    if (DBG) log("receiveContactsMsg = "+msg);
                    queryContactDetail((String)msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
            if (!hasMessages(QUERY_CONTACTS_DETAIL)) {
                if (DBG) log("notify main UI thread");
                mMainThreadHandler.sendEmptyMessage(UPDATE_DETAIL_INFO);
            }
        }
    }

    private final class ContactInfo {
        public String name;
        public int type;
        public String label;
        public long photoId = ADDCOTNACT_PHOTOID; //MOT Dialer Code - IKHSS7-830
    }

    private final ContactInfo CONTACTINFO_EMPTY = new ContactInfo(); //MOT Dialer Code - IKHSS7-1018

    private void queryContactDetail(String number) {
        Cursor detailCursor = null;
        long photoId = ADDCOTNACT_PHOTOID;
        if ( TextUtils.isEmpty(number)) {
            return;
        }
        QueryParam queryParam = getContactQueryParam(number);
        detailCursor = mResolver.query(queryParam.uri,
                                       queryParam.projection,
                                       queryParam.selection,
                                       queryParam.selectionArgs,
                                       queryParam.orderBy);

        if ((detailCursor != null) && (detailCursor.moveToFirst())) {
            photoId = detailCursor.getLong(
                    detailCursor.getColumnIndexOrThrow(Contacts.PHOTO_ID));
            String name  = detailCursor.getString(
                    detailCursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
            String label = detailCursor.getString(
                    detailCursor.getColumnIndex(PhoneLookup.LABEL));
            int type = detailCursor.getInt(detailCursor.getColumnIndex(PhoneLookup.TYPE));
            ContactInfo ci = new ContactInfo();
            ci.name = name;
            ci.type = type;
            ci.label= label;
            ci.photoId = photoId;
            if (DBG) log("mContactInfo = "+name+ " "+type+" "+label+ " "+ photoId);
            mContactInfoCache.put(number, ci);
        // MOT Dialer Code Start - IKHSS7-1018
        } else {
            mContactInfoCache.put(number, CONTACTINFO_EMPTY);
        // MOT Dialer Code End - IKHSS7-1018
        }

        if ((detailCursor != null)) {
            detailCursor.close();
        }
    }

    private class QueryParam {
        Uri uri;
        String[] projection;
        String selection;
        String[] selectionArgs;
        String orderBy;
    }

    private QueryParam getContactQueryParam(String number) {
        QueryParam param = new QueryParam();

        param.uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        param.projection = new String[]{
            PhoneLookup._ID,
            PhoneLookup.DISPLAY_NAME,
            PhoneLookup.PHOTO_ID,
            PhoneLookup.TYPE,
            PhoneLookup.LABEL
        };
        param.selection = null;
        param.selectionArgs = null;
        param.orderBy = null; //MOT Calling Code - IKMAIN-16808
        return param;
    }
    /******************************************************************
     * End background thread session
    ******************************************************************/

    private static final int UPDATE_DETAIL_INFO = 1;
    private static final int START_DELAYED_QUERY = 2;
    private static final int DELAYED_QUERY_TIMER = 250; //250ms

    private Handler mMainThreadHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_DETAIL_INFO:
                    //MOT Dialer Code Start - IKHSS6-6569
                    if (!activityGone) {
                        notifyDataSetChanged();
                    //ChinaDev updateSingleItemAndTitle();
                    }
                    //MOT Dialer Code End - IKHSS6-6569
                    break;
                case START_DELAYED_QUERY:
                    if (DBG) log("start real query");
                    if (DBG) log("start QUERY_STRING_MATCHED");
                    String digits = getInputDigits();
                    // MOT Calling code - IKSTABLEFOURV-988 IKPIM-447 IKMAIN-18641
                    if ((digits != null) && (mAsyncQueryHandler != null)) {
                        mAsyncQueryHandler.cancelOperation(QUERY_MOST_RECENT);
                        mAsyncQueryHandler.cancelOperation(QUERY_FUZZY_MATCHED);
                        mAsyncQueryHandler.cancelOperation(QUERY_EXACT_MATCHED);
                        if (SmartDialerUtil.noAlphaChar(digits)) {
                            mAsyncQueryHandler.startQuery(QUERY_FUZZY_MATCHED, digits, SMARTDIALER_FUZZY_CONTENT_URI,
                                PHONES_PROJECTION, digits, null, null); //MOT Calling code - IKPIM-161 IKMAIN-17457
                        mContactStartTime = System.currentTimeMillis();
                        } else {
                        // Contacts provider implemented a new URI for smart dialer
                            mAsyncQueryHandler.startQuery(QUERY_EXACT_MATCHED, digits,
                                Uri.withAppendedPath(SMARTDIALER_EXACT_CONTENT_URI, Uri.encode(digits)),
                                PEOPLE_PHONE_PROJECTION, null, null, null);
                        mContactStartTime = System.currentTimeMillis();
                        // END - MOT Calling code - IKSTABLETWOV-2768 IKPIM-447
                        }
                    }
                    // END MOT Calling code - IKMAIN-18641
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    // MOT Calling code - IKSTABLETWOV-2897
    private class MyObserver extends BlurContentObserver {
        public MyObserver() {
            super(new Handler());
        }

        public void onChangeRealNeeded(boolean selfChange) {
            if (DBG) log("onChangeRealNeeded smartdialer activityPaused = " + activityPaused);
            invalidateCache();
            if (!activityPaused) {
                String inputDigits = getInputDigits();
                if((inputDigits != null) && (inputDigits.length() >= START_QUERY_DIGITS_NUMBER)) {
                    startQuery(QUERY_STRING_MATCHED); // MOT Calling code - IKPIM-447
                }else {
                    startQuery(QUERY_MOST_RECENT);
                }
            }
        }
    }
    // END - MOT Calling code - IKSTABLETWOV-2897

    /****************************************************************
    * Follow APIs are used in TwelveKeyDialer.java
    *****************************************************************/
    private boolean activityPaused = false;
    private boolean activityGone   = false;
    void resumeQuery() {
        if (DBG) log("resumeQuery");
        activityPaused = false;
        CharSequence input = ((EditText)mFragmentView.findViewById(R.id.digits)).getText();

        String inputDigits = SmartDialerUtil.stripSeparators(input.toString());
        if (inputDigits != null) {
            if(inputDigits.length() >= START_QUERY_DIGITS_NUMBER) {
                startQuery(QUERY_STRING_MATCHED);
            } else {
                startQuery(QUERY_MOST_RECENT);
            }
        }
        // MOT Calling code - IKSTABLETWOV-2897
        mResolver.registerContentObserver(Phone.CONTENT_URI, true, mChangeObserver);
    }

    void pauseCleanUp() {
        if (DBG) log("pauseCleanUp");
        activityPaused = true;
        invalidateCache();
        if (mAsyncQueryHandler != null) {
            mAsyncQueryHandler.cancelOperation(QUERY_MOST_RECENT);
            mAsyncQueryHandler.cancelOperation(QUERY_FUZZY_MATCHED); //MOT Calling code - IKPIM-447
            mAsyncQueryHandler.cancelOperation(QUERY_EXACT_MATCHED); //MOT Calling code - IKPIM-447
        }
        if (mSyncQueryHandler != null) {
            mSyncQueryHandler.removeMessages(QUERY_CONTACTS_DETAIL);
        }
        // MOT Calling code - IKSTABLETWOV-2897
        mResolver.unregisterContentObserver(mChangeObserver);
    }

    void destroyCleanUp() {
        if (DBG) log("destoryCleanUp");
        activityGone = true;
        new HandlerThread(CLOSECURSOR_THREAD_NAME) {
            public void run() {
                if (mMostRecentCursor != null) {
                    mMostRecentCursor.close();
                    mMostRecentCursor = null;
                }
                //MOT Calling Code - IKPIM-447
                if (mMatchedCursor != null) {
                    mMatchedCursor.close();
                    mMatchedCursor = null;
                } //END MOT Calling Code - IKPIM-447
            }
        }.start();

        if (mSyncQueryHandler != null) {
            mSyncQueryHandler.getLooper().quit();
        }
        mAsyncQueryHandler = null;
    }
    /********************************************************
    * End of this session
    *********************************************************/

    //Mot Calling Code IKPIM-363
    ScreenSize getScreenSize(Context context) {

        int x = context.getResources().getDisplayMetrics().widthPixels;
        int y = context.getResources().getDisplayMetrics().heightPixels;

        ScreenSize sz = SCREEN_SIZE_MAP.get(x * y);

        if (null == sz)
            return ScreenSize.UNKNOWN;

        return sz;
    }
    //Mot Calling Code IKPIM-363 -End

    //MOT Calling Code - IKMAIN-21175
    /*ChinaDev
     boolean isFullSingleItemNeeded() {
        if (((mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            && (mDensity == DisplayMetrics.DENSITY_MEDIUM)) //mdpi in portrait mode
            || (misVGA)) {
            // Currently, VGA & mdpi in portrait, we do not show full info of single item
            return false;
        }
        // for others (hdpi/xhdpi/mdpi in landscape), we need show full single item.
        // do not consider QVGA phone here.
        return true;
    }*/
    //END MOT Calling Code - IKMAIN-21175

    private void log(String msg) {
        Log.d(TAG, msg);
    }

    //ChinaDev
    void updateSearchingResults(Cursor cursor) {
        String inputDigits = getInputDigits();
        ListView mList = (ListView) mFragmentView.findViewById(R.id.sd_list);
        View spaceHolder = mFragmentView.findViewById(R.id.spaceHolder);
        TextView emptyText = (TextView) mFragmentView.findViewById(R.id.emptyText);

        if (null==mList || null==spaceHolder || null==emptyText) return;

        if (cursor != null && cursor.getCount () == 0 && inputDigits.length() == 0) {
            emptyText.setText(R.string.inputInformation);
        } else {
        	emptyText.setText("");
        }
        if ((cursor != null) && !cursor.isClosed() && (cursor.moveToFirst())) {
            mdialpadFragment.changeSearchingMatches(cursor.getCount());
        } else {
            mdialpadFragment.changeSearchingMatches(0);
        }
        if (!DialpadFragment.phoneIsInUse()) {
            if((cursor != null) && !cursor.isClosed() && (cursor.moveToFirst())) {
                mList.setVisibility(View.VISIBLE);
                spaceHolder.setVisibility(View.VISIBLE);
            } else { // There is no Most Recent call
                mList.setVisibility(View.GONE);
                spaceHolder.setVisibility(View.GONE);
            }
        }
        // let the listview move to top
        mList.setSelection(0);
    }
}
//END MOT calling code- IKMAIN-4172
