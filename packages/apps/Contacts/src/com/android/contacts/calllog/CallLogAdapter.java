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

package com.android.contacts.calllog;

import com.android.common.widget.GroupingListAdapter;
import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.ContactsUtils;
import com.android.contacts.LocationServiceManager;
import com.android.contacts.EcidContact;
import com.android.contacts.PhoneCallDetails;
import com.android.contacts.PhoneCallDetailsHelper;
import com.android.contacts.R;
import com.android.contacts.util.ExpirableCache;
import com.android.contacts.util.UriUtils;
import com.google.common.annotations.VisibleForTesting;
import com.motorola.contacts.util.MEDialer;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView.OnScrollListener;
import android.widget.TextView;

import java.util.LinkedList;

import libcore.util.Objects;


/**
 * Adapter class to fill in data for the Call Log.
 */
/*package*/ class CallLogAdapter extends GroupingListAdapter
        implements Runnable, ViewTreeObserver.OnPreDrawListener, CallLogGroupBuilder.GroupCreator {

    /** Interface used to initiate a refresh of the content. */
    public interface CallFetcher {
        public void fetchCalls();
    }

    /**
     * Stores a phone number of a call with the country code where it originally occurred.
     * <p>
     * Note the country does not necessarily specifies the country of the phone number itself, but
     * it is the country in which the user was in when the call was placed or received.
     */
    private static final class NumberWithCountryIso {
        public final String number;
        public final String countryIso;

        public NumberWithCountryIso(String number, String countryIso) {
            this.number = number;
            this.countryIso = countryIso;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (!(o instanceof NumberWithCountryIso)) return false;
            NumberWithCountryIso other = (NumberWithCountryIso) o;
            return TextUtils.equals(number, other.number)
                    && TextUtils.equals(countryIso, other.countryIso);
        }

        @Override
        public int hashCode() {
            return (number == null ? 0 : number.hashCode())
                    ^ (countryIso == null ? 0 : countryIso.hashCode());
        }
    }

    /** The time in millis to delay starting the thread processing requests. */
    private static final int START_PROCESSING_REQUESTS_DELAY_MILLIS = 1000;

    /** The size of the cache of contact info. */
    private static final int CONTACT_INFO_CACHE_SIZE = 100;
    /** The size of the cache of CityId info. */
    private final static int CITYID_INFO_CACHE_SIZE = 200;// MOT Dialer Code

    private final Context mContext;
    private final ContactInfoHelper mContactInfoHelper;
    private final CallFetcher mCallFetcher;

    // IKHSS6-8052 FID:34118 Enhanced CityID BEGIN
    private boolean mEcidEnabled;
    // IKHSS6-8052 FID:34118 Enhanced CityID END

    /**
     * A cache of the contact details for the phone numbers in the call log.
     * <p>
     * The content of the cache is expired (but not purged) whenever the application comes to
     * the foreground.
     * <p>
     * The key is number with the country in which the call was placed or received.
     */
    private ExpirableCache<NumberWithCountryIso, ContactInfo> mContactInfoCache;
    
    //MOTO Dialer Code Start
    /**
     * A cache of the cityID info for the phone numbers in the call log.
     */
    private ExpirableCache<String, String> mCityIdInfoCache;//ChinaDev

     private Bitmap mUnknownPhoto;  //MOTO Dialer Code

    public boolean mIsCallBeenPlaced = false; // MOT Calling code - IKSTABLETWO-6716

    private static final String TAG = "CallLogAdapter";
    
    private boolean isInActionMode;

    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End
    //MOTO Dialer Code End

    /**
     * A request for contact details for the given number.
     */
    private static final class ContactInfoRequest {
        /** The number to look-up. */
        public final String number;
        /** The country in which a call to or from this number was placed or received. */
        public final String countryIso;
        /** The cached contact information stored in the call log. */
        public final ContactInfo callLogInfo;
        /** The convert number stored in the call log. */
        public final String convertNumber; //MOTO Dialer Code

        public ContactInfoRequest(String number, String countryIso, ContactInfo callLogInfo, String convertNumber) {
            this.number = number;
            this.countryIso = countryIso;
            this.callLogInfo = callLogInfo;
            this.convertNumber = convertNumber; //MOTO Dialer Code
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (!(obj instanceof ContactInfoRequest)) return false;

            ContactInfoRequest other = (ContactInfoRequest) obj;

            if (!TextUtils.equals(number, other.number)) return false;
            if (!TextUtils.equals(countryIso, other.countryIso)) return false;
            if (!Objects.equal(callLogInfo, other.callLogInfo)) return false;
            if (!TextUtils.equals(convertNumber, other.convertNumber)) return false; //MOTO Dialer Code

            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((callLogInfo == null) ? 0 : callLogInfo.hashCode());
            result = prime * result + ((countryIso == null) ? 0 : countryIso.hashCode());
            result = prime * result + ((number == null) ? 0 : number.hashCode());
            result = prime * result + ((convertNumber == null) ? 0 : convertNumber.hashCode()); //MOTO Dialer Code TBD
            return result;
        }
    }

    /**
     * List of requests to update contact details.
     * <p>
     * Each request is made of a phone number to look up, and the contact info currently stored in
     * the call log for this number.
     * <p>
     * The requests are added when displaying the contacts and are processed by a background
     * thread.
     */
    private final LinkedList<ContactInfoRequest> mRequests;

    private volatile boolean mDone;
    private boolean mLoading = true;
    private ViewTreeObserver.OnPreDrawListener mPreDrawListener;
    private static final int REDRAW = 1;
    private static final int START_THREAD = 2;

    private boolean mFirst;
    private Thread mCallerIdThread;

    /** Instance of helper class for managing views. */
    private final CallLogListItemHelper mCallLogViewsHelper;

    /** Helper to set up contact photos. */
    private final ContactPhotoManager mContactPhotoManager;
    /** Helper to parse and process phone numbers. */
    private PhoneNumberHelper mPhoneNumberHelper;
    /** Helper to group call log entries. */
    private final CallLogGroupBuilder mCallLogGroupBuilder;

    /** Can be set to true by tests to disable processing of requests. */
    private volatile boolean mRequestProcessingDisabled = false;

    /** Listener for the primary action in the list, opens the call details. */
    /*
    private final View.OnClickListener mPrimaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            IntentProvider intentProvider = (IntentProvider) view.getTag();
            if (intentProvider != null) {
                mContext.startActivity(intentProvider.getIntent(mContext));
            }
        }
    };*/ //comment by MOTO
    /** Listener for the secondary action in the list, either call or play. */
    private final View.OnClickListener mSecondaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
              // MOT Calling code - IKSTABLETWO-6716
            if (mIsCallBeenPlaced){
                return;
            }

            int[] layoutLocation = new int[] { -1, -1 };
            view.getLocationInWindow(layoutLocation);
            if(CDBG) Log.d("Calling_Kpi_Debug","RecentCall Call Icon Clicked! layoutLocation[0]: " + layoutLocation[0]+"layoutLocation[1]:" +layoutLocation[1]); // Mot Calling CR IKPRODUCT5-1140

            IntentProvider intentProvider = (IntentProvider) view.getTag();
            if (intentProvider != null) {
                Intent intent = intentProvider.getIntent(mContext);
                MEDialer.onDial(mContext, intent, MEDialer.DialFrom.RECENT); //Motorola, FTR 36344, Apr-19-2011, IKPIM-384
                mContext.startActivity(intent);
                //TODO, need to consider any error case which will not cause onPause()/onResume() happen e.g. launch activity failure.
                mIsCallBeenPlaced = true;
            }
        }
    };

    @Override
    public boolean onPreDraw() {
        if (mFirst) {
            mHandler.sendEmptyMessageDelayed(START_THREAD,
                    START_PROCESSING_REQUESTS_DELAY_MILLIS);
            mFirst = false;
        }
        return true;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REDRAW:
                    notifyDataSetChanged();
                    break;
                case START_THREAD:
                    startRequestProcessing();
                    break;
            }
        }
    };

    CallLogAdapter(Context context, CallFetcher callFetcher,
            ContactInfoHelper contactInfoHelper) {
        super(context);

        mContext = context;
        mCallFetcher = callFetcher;
        mContactInfoHelper = contactInfoHelper;

        mContactInfoCache = ExpirableCache.create(CONTACT_INFO_CACHE_SIZE);

        //MOT Dialer Start
        /*ChinaDev 
         if (CityIdInfo.isAvaialble(mContext)) {
            mCityIdInfoCache = ExpirableCache.create(CITYID_INFO_CACHE_SIZE);
        }*/
        mCityIdInfoCache = ExpirableCache.create(CITYID_INFO_CACHE_SIZE);
        // IKHSS6-8052 FID:34118 Enhanced CityID BEGIN
        mEcidEnabled = EcidContact.isECIDAvailable(mContext);
        // IKHSS6-8052 FID:34118 Enhanced CityID END
        //MOT Dialer End
        mRequests = new LinkedList<ContactInfoRequest>();
        mPreDrawListener = null;

        Resources resources = mContext.getResources();
        CallTypeHelper callTypeHelper = new CallTypeHelper(resources);

        mContactPhotoManager = ContactPhotoManager.getInstance(mContext);
        mPhoneNumberHelper = new PhoneNumberHelper(resources);
        PhoneCallDetailsHelper phoneCallDetailsHelper = new PhoneCallDetailsHelper(
                resources, callTypeHelper, mPhoneNumberHelper, context); // Motorola, w21071, 2011-12-21, IKCBS-2736 context added.
        mCallLogViewsHelper =
                new CallLogListItemHelper(
                        phoneCallDetailsHelper, mPhoneNumberHelper, resources);
        mCallLogGroupBuilder = new CallLogGroupBuilder(this);
        // MOTO Dialer Code Start - IKMAIN-35664
        Options opts = new Options();
        opts.inPurgeable = true;
        mUnknownPhoto = BitmapFactory.decodeResource(
                    resources,
                    R.drawable.ic_thb_unknown_caller, opts);
        // MOTO Dialer Code End - IKMAIN-35664
    }

    /**
     * Requery on background thread when {@link Cursor} changes.
     */
    @Override
    protected void onContentChanged() {
        if(DBG) log("onContentChanged");
        mCallFetcher.fetchCalls();
        CallLogFragment mFragment = (CallLogFragment) mCallFetcher;
        mFragment.finishMode();
    }

    void setLoading(boolean loading) {
        mLoading = loading;
    }

    @Override
    public boolean isEmpty() {
        if (mLoading) {
            // We don't want the empty state to show when loading.
            return false;
        } else {
            return super.isEmpty();
        }
    }

    // Keep it as moto need use it
    public ContactInfo getContactInfo(NumberWithCountryIso numberCountryIso) {
        return mContactInfoCache.getPossiblyExpired(numberCountryIso);
    }
 // Mot add
    public void setActionModeStatus(boolean isActionMode) {
        isInActionMode = isActionMode;
    }
  // Mot add end
    private void startRequestProcessing() {
        if (mRequestProcessingDisabled) {
            return;
        }

        mDone = false;
        mCallerIdThread = new Thread(this, "CallLogContactLookup");
        if (DBG) log("start new thread CallLogContactLookup");
        mCallerIdThread.setPriority(Thread.MIN_PRIORITY);
        mCallerIdThread.start();
    }

    /**
     * Stops the background thread that processes updates and cancels any pending requests to
     * start it.
     * <p>
     * Should be called from the main thread to prevent a race condition between the request to
     * start the thread being processed and stopping the thread.
     */
    public void stopRequestProcessing() {
        // Remove any pending requests to start the processing thread.
        mHandler.removeMessages(START_THREAD);
        mDone = true;
        //ChinaDev if (mCallerIdThread != null) mCallerIdThread.interrupt();
        if (mCallerIdThread != null) {
            mCallerIdThread.interrupt();
            if (DBG) log("mCallerIdThread thread get interruped");
            try {
                mCallerIdThread.join();
            } catch (InterruptedException e) {
            	if (DBG) Log.e(TAG, "stopRequestProcessing: mCallerIdThread.join exception " + e);
            }
            mCallerIdThread = null;
        }
    }

    public void invalidateCache() {
        mContactInfoCache.expireAll();
    }

    public void invalidatePreDrawListener() {
          // Let it restart the thread after next draw
        mPreDrawListener = null;
    }

    //MOTO Dialer Code Start
    public void clearAllCache() {
        synchronized (mContactInfoCache) { //TBD, whether need synchronized
            mContactInfoCache.clearCache();
        }

        // IKHSS6-8052 FID:34118 Enhanced CityID BEGIN
        if (mEcidEnabled) {
            EcidContact.clearEcidContacts();
        } // IKHSS6-8052 FID:34118 Enhanced CityID END
            synchronized (mCityIdInfoCache) {
               mCityIdInfoCache.clearCache();
            }
        //ChinaDev }
    }
    //MOTO Dialer Code End

    /**
     * Enqueues a request to look up the contact details for the given phone number.
     * <p>
     * It also provides the current contact info stored in the call log for this number.
     * <p>
     * If the {@code immediate} parameter is true, it will start immediately the thread that looks
     * up the contact information (if it has not been already started). Otherwise, it will be
     * started with a delay. See {@link #START_PROCESSING_REQUESTS_DELAY_MILLIS}.
     */
    @VisibleForTesting
    void enqueueRequest(String number, String countryIso, ContactInfo callLogInfo,
            boolean immediate, String convertNumber) {
        ContactInfoRequest request = new ContactInfoRequest(number, countryIso, callLogInfo, convertNumber);
        synchronized (mRequests) {
            if (!mRequests.contains(request)) {
                mRequests.add(request);
                mRequests.notifyAll();
                if (VDBG) {
                   log("enqueueRequest " + mRequests.size());
                }
            }
        }
        if (mFirst && immediate) {
            startRequestProcessing();
            mFirst = false;
        }
    }

    /**
     * Queries the appropriate content provider for the contact associated with the number.
     * <p>
     * Upon completion it also updates the cache in the call log, if it is different from
     * {@code callLogInfo}.
     * <p>
     * The number might be either a SIP address or a phone number.
     * <p>
     * It returns true if it updated the content of the cache and we should therefore tell the
     * view to update its content.
     */
    private boolean queryContactInfo(String number, String countryIso, ContactInfo callLogInfo, String convertNumber) {
        if (DBG) log("queryContactInfo ");
        final ContactInfo info = mContactInfoHelper.lookupNumber(number, countryIso, convertNumber);

        if (info == null) {// MOT Dialer Code
            // The lookup failed, just return without requesting to update the view.
            return false;
        }

        // Check the existing entry in the cache: only if it has changed we should update the
        // view.
        NumberWithCountryIso numberCountryIso = new NumberWithCountryIso(number, countryIso);
        ContactInfo existingInfo = mContactInfoCache.getPossiblyExpired(numberCountryIso);
        boolean updated = !info.equals(existingInfo);
        // Store the data in the cache so that the UI thread can use to display it. Store it
        // even if it has not changed so that it is marked as not expired.
        mContactInfoCache.put(numberCountryIso, info);
        // Update the call log even if the cache it is up-to-date: it is possible that the cache
        // contains the value from a different call log entry.
        updateCallLogContactInfoCache(number, countryIso, info, callLogInfo);
        /* //MOT DIaler Start
        if (mEcidEnabled) { // IKHSS6-8052 FID:34118 Enhanced CityID BEGIN
            EcidContact newEcidInfo = null;
            EcidContact ecidInfo = EcidContact.doCacheLookup(mContext, info.number, Calls.INCOMING_TYPE);
            if (ecidInfo == null) {
                newEcidInfo = EcidContact.doLookup(mContext, info.number, Calls.INCOMING_TYPE);
            }
            updated = (updated || (newEcidInfo != null));
            // IKHSS6-8052 FID:34118 Enhanced CityID END
        } else if (CityIdInfo.isAvaialble(mContext)) {
            CityIdInfo cidInfo = null;// MOT Dialer Code
            CityIdInfo existingcidInfo = mCityIdInfoCache.getPossiblyExpired(number);
            if (existingcidInfo == null) {
                cidInfo = queryCityIDInfo(number);
            }
            updated = (updated || (cidInfo != null));
            if (cidInfo != null) mCityIdInfoCache.put(number, cidInfo);
        }
        //MOT Dialer End */
        String numLocation = null;
        final String noSeNumber = PhoneNumberUtils.stripSeparators(number);
        if (isLocationEnable()){
            String existingcidInfo = mCityIdInfoCache.getPossiblyExpired(noSeNumber);
            if (existingcidInfo == null) {
            	numLocation =  queryLocationInfo(noSeNumber);
            }
            updated = (updated || (numLocation != null));
            if (numLocation != null) mCityIdInfoCache.put(noSeNumber, numLocation);
        }
        return updated;
    }


    //MOT Dialer Start
    /**
     * Determines the cityid information for the given phone number.
     * <p>
     * It returns the cityid info if found.
     * <p>
     * If no cityid corresponds to the given phone number, returns null}.
     */
   /* private CityIdInfo queryCityIDInfo(String number) {
        CityIdInfo cidInfo = null;
        if (!PhoneNumberUtils.isUriNumber(number) && CityIdInfo.isAvaialble(mContext)
                && (mCityIdInfoCache.getPossiblyExpired(number) == null)) {
            if(DBG) log("start recentCallCityIdLookup");
            // TODO: Currently, we pass call type default as incoming
            cidInfo = CityIdInfo.recentCallCityIdLookup(mContext, number, true);
            if(DBG) log("end recentCallCityIdLookup");
        }
        return cidInfo;
    }*/
    // MOT Dialer End

    /*
     * Handles requests for contact name and number type
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        boolean needNotify = false;
        while (!mDone) {
            ContactInfoRequest request = null;
            synchronized (mRequests) {
                if (!mRequests.isEmpty()) {
                    request = mRequests.removeFirst();
                } else {
                    if (needNotify) {
                        needNotify = false;
                        mHandler.sendEmptyMessage(REDRAW);
                    }
                    try {
                        mRequests.wait(1000);
                    } catch (InterruptedException ie) {
                        // Ignore and continue processing requests
                        Thread.currentThread().interrupt();
                        mDone = true;
                        if (VDBG) log("contact db query thread get interruped");
                    }
                }
            }

            if (!mDone && request != null
                    && queryContactInfo(request.number, request.countryIso, request.callLogInfo, request.convertNumber)) {
                needNotify = true;
            }
        }
    }

    @Override
    protected void addGroups(Cursor cursor) {
        mCallLogGroupBuilder.addGroups(cursor);
    }

    @Override
    protected View newStandAloneView(Context context, ViewGroup parent) {
          if (VDBG) {
           log("newStandAloneView ");
        }
        LayoutInflater inflater =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.call_log_list_item, parent, false);
        findAndCacheViews(view);
        return view;
    }

    @Override
    protected void bindStandAloneView(View view, Context context, Cursor cursor) {
          if (VDBG) {
           log("bindStandAloneView ");
        }
        bindView(view, cursor, 1);
    }

    @Override
    protected View newChildView(Context context, ViewGroup parent) {
          if (VDBG) {
           log("newChildView ");
        }
        LayoutInflater inflater =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.call_log_list_item, parent, false);
        findAndCacheViews(view);
        return view;
    }

    @Override
    protected void bindChildView(View view, Context context, Cursor cursor) {
          if (VDBG) {
           log("bindChildView ");
        }
        bindView(view, cursor, 1);
    }

    @Override
    protected View newGroupView(Context context, ViewGroup parent) {
          if (VDBG) {
           log("newGroupView ");
        }
        LayoutInflater inflater =
                (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.call_log_list_item, parent, false);
        findAndCacheViews(view);
        return view;
    }

    @Override
    protected void bindGroupView(View view, Context context, Cursor cursor, int groupSize,
            boolean expanded) {
        if (VDBG) {
           log("bindGroupView ");
        }
        bindView(view, cursor, groupSize);
    }

    private void findAndCacheViews(View view) {
          if (VDBG) {
           log("findAndCacheViews ");
        }
        // Get the views to bind to.
        CallLogListItemViews views = CallLogListItemViews.fromView(view);
        //views.primaryActionView.setOnClickListener(mPrimaryActionListener);
        views.secondaryActionView.setOnClickListener(mSecondaryActionListener);
        view.setTag(views);
    }

    /**
     * Binds the views in the entry to the data in the call log.
     *
     * @param view the view corresponding to this entry
     * @param c the cursor pointing to the entry in the call log
     * @param count the number of entries in the current item, greater than 1 if it is a group
     */
    private void bindView(View view, Cursor c, int count) {
        final CallLogListItemViews views = (CallLogListItemViews) view.getTag();
        final int section = c.getInt(CallLogQuery.SECTION);

        // MOTO Calling - IKHSS7-6114 Begin
        int[] layoutLocation = new int[] { -1, -1 };
        view.getLocationInWindow(layoutLocation);
        if(CDBG) Log.d("Calling_dbg","BindView layoutLocation[0]:" + layoutLocation[0] + " layoutLocation[1]:" +layoutLocation[1] + "cursor Position: " + c.getPosition()); // Mot Calling CR IKPRODUCT5-1140
        // MOTO Calling - IKHSS7-6114 End

        // This might be a header: check the value of the section column in the cursor.
        if (section == CallLogQuery.SECTION_NEW_HEADER
                || section == CallLogQuery.SECTION_OLD_HEADER) {
            views.primaryActionView.setVisibility(View.GONE);
            views.bottomDivider.setVisibility(View.GONE);
            views.listHeaderTextView.setVisibility(View.VISIBLE);
            views.listHeaderTextView.setText(
                    section == CallLogQuery.SECTION_NEW_HEADER
                            ? R.string.call_log_new_header
                            : R.string.call_log_old_header);
            views.primaryActionView.setTag(null); // MOTO Calling - IKHSS7-6114
            views.secondaryActionView.setTag(null); // MOTO Calling - IKHSS7-6114
            // Nothing else to set up for a header.
            return;
        }
        // Default case: an item in the call log.
        views.primaryActionView.setVisibility(View.VISIBLE);
        views.bottomDivider.setVisibility(View.VISIBLE); // Mot  CR IKHSS7-8758
        views.listHeaderTextView.setVisibility(View.GONE);

        final long id = c.getLong(CallLogQuery.ID);
        final String number = c.getString(CallLogQuery.NUMBER);
        final long date = c.getLong(CallLogQuery.DATE);
        final long duration = c.getLong(CallLogQuery.DURATION);
        final int callType = c.getInt(CallLogQuery.CALL_TYPE);
        final String countryIso = c.getString(CallLogQuery.COUNTRY_ISO);
        

        String convertNumber = c.getString(CallLogQuery.CALLER_CONVERT_NUM);
        String cnapName = c.getString(CallLogQuery.CALLER_CNAPNAME); //IKMAIN-10554, a19591, 34425 CNAP feature

        final ContactInfo cachedContactInfo = getContactInfoFromCallLog(c);

        views.primaryActionView.setTag(
                IntentProvider.getCallDetailIntentProvider(
                        this, c.getPosition(), c.getLong(CallLogQuery.ID), count));
        // Store away the voicemail information so we can play it directly.
        if (callType == Calls.VOICEMAIL_TYPE) {
            String voicemailUri = c.getString(CallLogQuery.VOICEMAIL_URI);
            final long rowId = c.getLong(CallLogQuery.ID);
            views.secondaryActionView.setTag(
                    IntentProvider.getPlayVoicemailIntentProvider(rowId, voicemailUri));
        } else if (!TextUtils.isEmpty(number)) {
            // Store away the number so we can call it directly if you click on the call icon.
            views.secondaryActionView.setTag(
                    IntentProvider.getReturnCallIntentProvider(number));
        } else {
            // No action enabled.
            views.secondaryActionView.setTag(null);
        }

        // Lookup contacts with this number
        NumberWithCountryIso numberCountryIso = new NumberWithCountryIso(number, countryIso);
        ExpirableCache.CachedValue<ContactInfo> cachedInfo =
                mContactInfoCache.getCachedValue(numberCountryIso);
        ContactInfo info = cachedInfo == null ? null : cachedInfo.getValue();

        //Mot add
        if (isInActionMode){
          views.primaryActionView.setBackgroundResource(R.drawable.list_item_background);
        } else {
          views.primaryActionView.setBackgroundDrawable(null);
      }

        //Mot add
        if (VDBG) {
            log("bindView [");
            log("cursor id: " + id);
            log("cursor Count: " + c.getCount());
            log("cursor Position: " + c.getPosition());
            log(" call_log.number        " + number);
            log(" call_log.callType    " + callType);
            log(" call_log.callerNameType    " + cnapName);
            log(" call_log.countryIso    " + countryIso);
            log(" .......................");
            if (info != null) {
                log(" info.lookupUri          " + info.lookupUri);
                log(" info.name              " + info.name);
                log(" info.number            " + info.number);
                log(" info.formattedNumber  " + info.formattedNumber);
                log(" info.normalizedNumber  " + info.normalizedNumber);
            } else {
                log(" info is null");
            }

            if (cachedContactInfo != null) {
                log(" cachedContactInfo.lookupUri          " + cachedContactInfo.lookupUri);
                log(" cachedContactInfo.name              " + cachedContactInfo.name);
                log(" cachedContactInfo.number            " + cachedContactInfo.number);
                log(" cachedContactInfo.formattedNumber  " + cachedContactInfo.formattedNumber);
                log(" cachedContactInfo.normalizedNumber  " + cachedContactInfo.normalizedNumber);
            } else {
                log(" cachedContactInfo is null");
            }
            log("]");
        }
        //Mot add end

        if (!mPhoneNumberHelper.canPlaceCallsTo(number) //TBD??
                /*|| mPhoneNumberHelper.isVoicemailNumber(number)*/) { //comment by MOTO
            // If this is a number that cannot be dialed, there is no point in looking up a contact
            // for it.
            info = ContactInfo.EMPTY;
        } else if (cachedInfo == null) {
            mContactInfoCache.put(numberCountryIso, ContactInfo.EMPTY);
            // Use the cached contact info from the call log.
            info = cachedContactInfo;
            // The db request should happen on a non-UI thread.
            // Request the contact details immediately since they are currently missing.
            if(DBG) log("call log number not find in contact info cache, push request to query");
            enqueueRequest(number, countryIso, cachedContactInfo, true, convertNumber);
            // We will format the phone number when we make the background request.
        } else {
              if(DBG) log("call log number find in contact info cache.");
            if (cachedInfo.isExpired()) {
                // The contact info is no longer up to date, we should request it. However, we
                // do not need to request them immediately.
                if(DBG) log("call log number find in contact info cache, but expired, push request to query");
                enqueueRequest(number, countryIso, cachedContactInfo, false, convertNumber);
            } else  if (!callLogInfoMatches(cachedContactInfo, info)) { //can happen if call log db update failure while contact query finish
                // The call log information does not match the one we have, look it up again.
                // We could simply update the call log directly, but that needs to be done in a
                // background thread, so it is easier to simply request a new lookup, which will, as
                // a side-effect, update the call log.
                enqueueRequest(number, countryIso, cachedContactInfo, false, convertNumber);
            }

            if (info == ContactInfo.EMPTY) {  //TBD, suppose should not happen, will not have empty if do query.
                // Use the cached contact info from the call log.
                info = cachedContactInfo;
            }
        }

        final Uri lookupUri = info.lookupUri;
        final String name = info.name;
        final int ntype = info.type;
        final String label = info.label;
        final long photoId = info.photoId;
        CharSequence formattedNumber = info.formattedNumber;
        /* Another solution for convertNumber format */ //TBD
        //CharSequence formattedNumber = formatPhoneNumber(number, info.normalizedNumber, countryIso, convertNumber);
        final int[] callTypes = getCallTypes(c, count);
        final int[] networkTypes =  getNetworkTypes(c, count);
        //MOT Dialer Start
        // Mot will not use google location function, use CityID feature instead
        // Currently, just set geocode to empty
        // TODO: remove those geocode related logic in future
        //final String geocode = c.getString(CallLogQuery.GEOCODED_LOCATION);
        final String geocode;
        TextView cityIdView = (TextView) view.findViewById(R.id.cityid);
        final String noSeNumber = PhoneNumberUtils.stripSeparators(number);
        if (isLocationEnable()) {
            String numLoc = mCityIdInfoCache.getPossiblyExpired(noSeNumber);
            if (numLoc == null) numLoc = queryLocationInfo(noSeNumber);
            geocode = numLoc;
        }else {
            geocode = null;
        }
        cityIdView.setText(geocode);
        /*if (CityIdInfo.isAvaialble(mContext)) {
            TextView cityIdView = (TextView) view.findViewById(R.id.cityid);
            CityIdInfo cidinfo = mCityIdInfoCache.getPossiblyExpired(number);
            String displayCityId = null;
            if (cidinfo != null) {
               displayCityId = cidinfo.computeDisplayName(cityIdView);
            }
            geocode = displayCityId;
        } else {
            geocode = null;
        }*/
        //MOT Dialer End

        final PhoneCallDetails details;
        if (TextUtils.isEmpty(name)) {
            details = new PhoneCallDetails(number, formattedNumber, countryIso, geocode,
                    callTypes, networkTypes, date, duration, cnapName);
        } else {
            // We do not pass a photo id since we do not need the high-res picture.
            details = new PhoneCallDetails(number, formattedNumber, countryIso, geocode,
                    callTypes, networkTypes, date, duration, name, ntype, label, lookupUri, null, cnapName);
        }

        final boolean isNew = c.getInt(CallLogQuery.IS_READ) == 0;
        // New items also use the highlighted version of the text.
        final boolean isHighlighted = isNew;

        //MOTO Dialer Code Start
        boolean isVoicemail = false;
        boolean isEmergency = false;
        if (mPhoneNumberHelper.isVoicemailNumber(number)) {// MOT Calling code - IKSTABLEFOURV-1933
            //name = mContext.getString(R.string.voicemail);
            isVoicemail = true;
        }
        /*Added for switchuitwo-381 begin*/
        //if (PhoneNumberUtils.isLocalEmergencyNumber(number, mContext)) {
        if (ContactsUtils.isEmergencyNumber(number, mContext)) {
            //name = getString(
                    //com.android.internal.R.string.emergency_call_dialog_number_for_display);
            isEmergency = true;
        }
        /*Added for switchuitwo-381 end*/
        //MOTO Dialer Code End

        mCallLogViewsHelper.setPhoneCallDetails(views, details, isHighlighted, isVoicemail, isEmergency);
        setPhoto(views, photoId, lookupUri, isVoicemail, isEmergency, name, number);

        // Listen for the first draw
        if (mPreDrawListener == null) {
            mFirst = true;
            mPreDrawListener = this;
            view.getViewTreeObserver().addOnPreDrawListener(this);
        }

        if (VDBG) {
            log("exit bindView");
        }
    }

    /** Returns true if this is the last item of a section. */
    private boolean isLastOfSection(Cursor c) {
        if (c.isLast()) return true;
        final int section = c.getInt(CallLogQuery.SECTION);
        if (!c.moveToNext()) return true;
        final int nextSection = c.getInt(CallLogQuery.SECTION);
        c.moveToPrevious();
        return section != nextSection;
    }

    /** Checks whether the contact info from the call log matches the one from the contacts db. */
    private boolean callLogInfoMatches(ContactInfo callLogInfo, ContactInfo info) {
        // The call log only contains a subset of the fields in the contacts db.
        // Only check those.
        return TextUtils.equals(callLogInfo.name, info.name)
                && callLogInfo.type == info.type
                && TextUtils.equals(callLogInfo.label, info.label);
    }

    /** Stores the updated contact info in the call log if it is different from the current one. */
    private void updateCallLogContactInfoCache(String number, String countryIso,
            ContactInfo updatedInfo, ContactInfo callLogInfo) {
        final ContentValues values = new ContentValues();
        boolean needsUpdate = false;

        if (callLogInfo != null) {
            if (!TextUtils.equals(updatedInfo.name, callLogInfo.name)) {
                values.put(Calls.CACHED_NAME, updatedInfo.name);
                needsUpdate = true;
            }

            if (updatedInfo.type != callLogInfo.type) {
                values.put(Calls.CACHED_NUMBER_TYPE, updatedInfo.type);
                needsUpdate = true;
            }

            if (!TextUtils.equals(updatedInfo.label, callLogInfo.label)) {
                values.put(Calls.CACHED_NUMBER_LABEL, updatedInfo.label);
                needsUpdate = true;
            }
            if (!UriUtils.areEqual(updatedInfo.lookupUri, callLogInfo.lookupUri)) {
                values.put(Calls.CACHED_LOOKUP_URI, UriUtils.uriToString(updatedInfo.lookupUri));
                needsUpdate = true;
            }
            if (!TextUtils.equals(updatedInfo.normalizedNumber, callLogInfo.normalizedNumber)) {
                values.put(Calls.CACHED_NORMALIZED_NUMBER, updatedInfo.normalizedNumber);
                needsUpdate = true;
            }
            if (!TextUtils.equals(updatedInfo.number, callLogInfo.number)) {
                values.put(Calls.CACHED_MATCHED_NUMBER, updatedInfo.number);
                needsUpdate = true;
            }
            if (updatedInfo.photoId != callLogInfo.photoId) {
                values.put(Calls.CACHED_PHOTO_ID, updatedInfo.photoId);
                needsUpdate = true;
            }
            if (!TextUtils.equals(updatedInfo.formattedNumber, callLogInfo.formattedNumber)) {
                values.put(Calls.CACHED_FORMATTED_NUMBER, updatedInfo.formattedNumber);
                needsUpdate = true;
            }
        } else {
            // No previous values, store all of them.
            values.put(Calls.CACHED_NAME, updatedInfo.name);
            values.put(Calls.CACHED_NUMBER_TYPE, updatedInfo.type);
            values.put(Calls.CACHED_NUMBER_LABEL, updatedInfo.label);
            values.put(Calls.CACHED_LOOKUP_URI, UriUtils.uriToString(updatedInfo.lookupUri));
            values.put(Calls.CACHED_MATCHED_NUMBER, updatedInfo.number);
            values.put(Calls.CACHED_NORMALIZED_NUMBER, updatedInfo.normalizedNumber);
            values.put(Calls.CACHED_PHOTO_ID, updatedInfo.photoId);
            values.put(Calls.CACHED_FORMATTED_NUMBER, updatedInfo.formattedNumber);
            needsUpdate = true;
        }

        if (!needsUpdate) {
            return;
        }

        if (countryIso == null) {
            if (VDBG) {
                log("updateCallLogContactInfoCache: update call log record number = " + number);
            }
            mContext.getContentResolver().update(Calls.CONTENT_URI_WITH_VOICEMAIL, values,
                    Calls.NUMBER + " = ? AND " + Calls.COUNTRY_ISO + " IS NULL",
                    new String[]{ number });
        } else {
            if (VDBG) {
                log("updateCallLogContactInfoCache: update call log record number = " + number + " countryIso = " + countryIso);
            }
            mContext.getContentResolver().update(Calls.CONTENT_URI_WITH_VOICEMAIL, values,
                    Calls.NUMBER + " = ? AND " + Calls.COUNTRY_ISO + " = ?",
                    new String[]{ number, countryIso });
        }
    }

    /** Returns the contact information as stored in the call log. */
    private ContactInfo getContactInfoFromCallLog(Cursor c) {
        ContactInfo info = new ContactInfo();
        info.lookupUri = UriUtils.parseUriOrNull(c.getString(CallLogQuery.CACHED_LOOKUP_URI));
        info.name = c.getString(CallLogQuery.CACHED_NAME);
        info.type = c.getInt(CallLogQuery.CACHED_NUMBER_TYPE);
        info.label = c.getString(CallLogQuery.CACHED_NUMBER_LABEL);
        String matchedNumber = c.getString(CallLogQuery.CACHED_MATCHED_NUMBER);
        info.number = matchedNumber == null ? c.getString(CallLogQuery.NUMBER) : matchedNumber;
        info.normalizedNumber = c.getString(CallLogQuery.CACHED_NORMALIZED_NUMBER);
        info.photoId = c.getLong(CallLogQuery.CACHED_PHOTO_ID);
        info.photoUri = null;  // We do not cache the photo URI.
        info.formattedNumber = c.getString(CallLogQuery.CACHED_FORMATTED_NUMBER);
        return info;
    }

    /**
     * Returns the call types for the given number of items in the cursor.
     * <p>
     * It uses the next {@code count} rows in the cursor to extract the types.
     * <p>
     * It position in the cursor is unchanged by this function.
     */
    private int[] getCallTypes(Cursor cursor, int count) {
        int position = cursor.getPosition();
        int[] callTypes = new int[count];
        for (int index = 0; index < count; ++index) {
            callTypes[index] = cursor.getInt(CallLogQuery.CALL_TYPE);
            cursor.moveToNext();
        }
        cursor.moveToPosition(position);
        return callTypes;
    }

    private int[] getNetworkTypes(Cursor cursor, int count) {
        int position = cursor.getPosition();
        int[] networkTypes = new int[count];
        for (int index = 0; index < count; ++index) {
            networkTypes[index] = cursor.getInt(CallLogQuery.NETWORK_TYPE_COLUMN_INDEX);
            cursor.moveToNext();
        }
        cursor.moveToPosition(position);
        return networkTypes;
    }

    private void setPhoto(CallLogListItemViews views, long photoId, Uri contactUri,
                  boolean isVoicemail, boolean isEmergency, String contactName, String number) {
        if (contactUri != null) { //contact exists
            views.quickContactView.assignContactUri(contactUri);
        } else {  //To pop up add to contact window
            views.quickContactView.assignContactFromPhone(number, true);
        }
   
        // Set the caller photo delete
        if (isVoicemail) {
            views.quickContactView.setImageResource(
                	R.drawable.ic_launcher_voicemail);  //MOT Calling code - IKMAIN-20475
      	} else if (isEmergency) {
           	views.quickContactView.setImageResource(
                   	R.drawable.picture_emergency);
       	} else if (photoId != 0) {
           	//mContactPhotoManager.loadPhoto(views.quickContactView, photoId, false, true);
           	mContactPhotoManager.loadPhotoForRC(views.quickContactView, photoId, false, true, contactName, mUnknownPhoto);
        } else {
            mContactPhotoManager.loadDefaultPhoto(views.quickContactView, contactUri, false, true);
        }        
    }

    /**
     * Sets whether processing of requests for contact details should be enabled.
     * <p>
     * This method should be called in tests to disable such processing of requests when not
     * needed.
     */
    @VisibleForTesting
    void disableRequestProcessingForTest() {
        mRequestProcessingDisabled = true;
    }

    @VisibleForTesting
    void injectContactInfoForTest(String number, String countryIso, ContactInfo contactInfo) {
        NumberWithCountryIso numberCountryIso = new NumberWithCountryIso(number, countryIso);
        mContactInfoCache.put(numberCountryIso, contactInfo);
    }

    @Override
    public void addGroup(int cursorPosition, int size, boolean expanded) {
        super.addGroup(cursorPosition, size, expanded);
    }

    /*
     * Get the number from the Contacts, if available, since sometimes
     * the number provided by caller id may not be formatted properly
     * depending on the carrier (roaming) in use at the time of the
     * incoming call.
     * Logic : If the caller-id number starts with a "+", use it
     *         Else if the number in the contacts starts with a "+", use that one
     *         Else if the number in the contacts is longer, use that one
     */
    public String getBetterNumberFromContacts(String number, String countryIso) {
        String matchingNumber = null;
        // Look in the cache first. If it's not found then query the Phones db
        NumberWithCountryIso numberCountryIso = new NumberWithCountryIso(number, countryIso);
        ContactInfo ci = mContactInfoCache.getPossiblyExpired(numberCountryIso);
        if (ci != null && ci != ContactInfo.EMPTY) {
            matchingNumber = ci.number;
        } else {
            try {
                Cursor phonesCursor = mContext.getContentResolver().query(
                        Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)),
                        PhoneQuery._PROJECTION, null, null, null);
                if (phonesCursor != null) {
                    if (phonesCursor.moveToFirst()) {
                        matchingNumber = phonesCursor.getString(PhoneQuery.MATCHED_NUMBER);
                    }
                    phonesCursor.close();
                }
            } catch (Exception e) {
                // Use the number from the call log
            }
        }
        if (!TextUtils.isEmpty(matchingNumber) &&
                (matchingNumber.startsWith("+")
                        || matchingNumber.length() > number.length())) {
            number = matchingNumber;
        }
        return number;
    }

    public PhoneNumberHelper getPhoneNumberHelper() {
          return mPhoneNumberHelper;
    }

    //MOT Dialer Code Start - IKHSS6UPGR-4081
    public void onScrollStateChanged(int scrollState) {
        if (DBG) log("onScrollState changed, scrollState = " + scrollState);

        //the flag mIsScrolling is used for query contact info and cityid info,
        //when list is SCROLL_STATE_FLING or SCROLL_STATE_TOUCH_SCROLL,
        //we will not query contact & cityid info.
        boolean isScrolling = (scrollState != OnScrollListener.SCROLL_STATE_IDLE);
        if (isScrolling) {
            stopRequestProcessing();
        } else {
            startRequestProcessing();
        }

        //stop loading photos when scroll state is fling.
        if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
            mContactPhotoManager.pause();
        } else {
            mContactPhotoManager.resume();
        }
    }
    //MOT Dialer Code End - IKHSS6UPGR-4081

    static void log(String msg) {
        Log.d(TAG, " -> " + msg);
    }
    
    //ChinaDev
	private String queryLocationInfo(String number) {
		CallLogFragment mFragment = (CallLogFragment) mCallFetcher;
		LocationServiceManager mLocationServiceManager = mFragment.getmLocationServiceManager();
		if ((mLocationServiceManager != null) && (mLocationServiceManager.checkService(false))) {
			if (number != null) {
				String mDisplayName = mLocationServiceManager.getLocInfo(number);
				if (VDBG) {
		            log("PUT mCityInfo NoSepNumber=" + number + "  mDisplayName=" + mDisplayName);
		        }
				return mDisplayName;
			}
		}
		return null;
	}
	
	private boolean isLocationEnable() {
		CallLogFragment mFragment = (CallLogFragment) mCallFetcher;
		return mFragment.isLocationEnable();
	}
}
