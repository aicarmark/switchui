/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.contacts;

import com.android.contacts.activities.ContactDetailActivity;
import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.BackScrollManager.ScrollableHeader;
import com.android.contacts.calllog.CallDetailHistoryAdapter;
import com.android.contacts.calllog.CallTypeHelper;
import com.android.contacts.calllog.ContactInfo;
import com.android.contacts.calllog.ContactInfoHelper;
import com.android.contacts.calllog.PhoneNumberHelper;
import com.android.contacts.EcidContact;
import com.android.contacts.util.AsyncTaskExecutor;
import com.android.contacts.util.AsyncTaskExecutors;
import com.android.contacts.voicemail.VoicemailPlaybackFragment;
import com.android.contacts.voicemail.VoicemailStatusHelper;
import com.android.contacts.voicemail.VoicemailStatusHelper.StatusMessage;
import com.android.contacts.voicemail.VoicemailStatusHelperImpl;
import com.motorola.contacts.util.MEDialer; //MOT CAlling code IKHSS7-1479
import com.motorola.android.telephony.PhoneModeManager;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.provider.Contacts.Intents.Insert;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.VoicemailContract.Voicemails;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Displays the details of a specific call log entry.
 * <p>
 * This activity can be either started with the URI of a single call log entry, or with the
 * {@link #EXTRA_CALL_LOG_IDS} extra to specify a group of call log entries.
 */
public class CallDetailActivity extends Activity implements ProximitySensorAware {
    private static final String TAG = "CallDetail";

    /** The time to wait before enabling the blank the screen due to the proximity sensor. */
    private static final long PROXIMITY_BLANK_DELAY_MILLIS = 100;
    /** The time to wait before disabling the blank the screen due to the proximity sensor. */
    private static final long PROXIMITY_UNBLANK_DELAY_MILLIS = 500;

    /** The enumeration of {@link AsyncTask} objects used in this class. */
    public enum Tasks {
        MARK_VOICEMAIL_READ,
        DELETE_VOICEMAIL_AND_FINISH,
        REMOVE_FROM_CALL_LOG_AND_FINISH,
        UPDATE_PHONE_CALL_DETAILS,
    }

    /** A long array extra containing ids of call log entries to display. */
    public static final String EXTRA_CALL_LOG_IDS = "EXTRA_CALL_LOG_IDS";
    /** If we are started with a voicemail, we'll find the uri to play with this extra. */
    public static final String EXTRA_VOICEMAIL_URI = "EXTRA_VOICEMAIL_URI";
    /** If we should immediately start playback of the voicemail, this extra will be set to true. */
    public static final String EXTRA_VOICEMAIL_START_PLAYBACK = "EXTRA_VOICEMAIL_START_PLAYBACK";

    private CallTypeHelper mCallTypeHelper;
    private PhoneNumberHelper mPhoneNumberHelper;
    private PhoneCallDetailsHelper mPhoneCallDetailsHelper;
    private TextView mHeaderTextView;
    private View mHeaderOverlayView;
    private ImageView mMainActionView;
    private ImageButton mMainActionPushLayerView;
    private ImageView mContactBackgroundView;
    private ImageView mSmallContactBackgroundView;//MOTO Dialer Code IKHSS6-1147
    private AsyncTaskExecutor mAsyncTaskExecutor;
    private ContactInfoHelper mContactInfoHelper;
    private ProgressDialog mProgressDialog ;
    private String mNumber = null;
    //add by txbv34 for IKCBSMMCPPRC-1551
    private String mName = null;
    
    private String mDefaultCountryIso;

    /* package */ LayoutInflater mInflater;
    /* package */ Resources mResources;
    /** Helper to load contact photos. */
    private ContactPhotoManager mContactPhotoManager;
    /** Helper to make async queries to content resolver. */
    private CallDetailActivityQueryHandler mAsyncQueryHandler;
    /** Helper to get voicemail status messages. */
    private VoicemailStatusHelper mVoicemailStatusHelper;
    // Views related to voicemail status message.
    private View mStatusMessageView;
    private TextView mStatusMessageText;
    private TextView mStatusMessageAction;

    /** Whether we should show "edit number before call" in the options menu. */
    private boolean mHasEditNumberBeforeCallOption;
    /** Whether we should show "trash" in the options menu. */
    private boolean mHasTrashOption;
    /** Whether we should show "remove from call log" in the options menu. */
    private boolean mHasRemoveFromCallLogOption;
    
    private boolean isIpCall;
    private boolean isIntRoamCall;
    private boolean isIntRoamCallBack;
    private boolean isVoiceMail;

    private ProximitySensorManager mProximitySensorManager;
    private final ProximitySensorListener mProximitySensorListener = new ProximitySensorListener();

    //MOTO Dialer Code Start
    private static final String CACHED_CONVERT_NUMBER = "cached_convert_number"; //33860

    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End

    // IKHSS6-8052 FID:34118 Enhanced CityID
    private boolean mIsEcidAvailable = false;

    //MOT Dialer Code - IKHSS6-13862
    final static String ADD_CONTACT_DIALOG="com.motorola.contacts.ACTION_ADD_CONTACT_DIALOG";
    //MOTO Dialer Code End
    private LocationServiceManager mLocationServiceManager = null;
    private TextView mCallLocation;
private Handler mHandler = new Handler();
    /** Listener to changes in the proximity sensor state. */
    private class ProximitySensorListener implements ProximitySensorManager.Listener {
        /** Used to show a blank view and hide the action bar. */
        private final Runnable mBlankRunnable = new Runnable() {
            @Override
            public void run() {
                View blankView = findViewById(R.id.blank);
                blankView.setVisibility(View.VISIBLE);
                getActionBar().hide();
            }
        };
        /** Used to remove the blank view and show the action bar. */
        private final Runnable mUnblankRunnable = new Runnable() {
            @Override
            public void run() {
                View blankView = findViewById(R.id.blank);
                blankView.setVisibility(View.GONE);
                getActionBar().show();
            }
        };

        @Override
        public synchronized void onNear() {
            clearPendingRequests();
            postDelayed(mBlankRunnable, PROXIMITY_BLANK_DELAY_MILLIS);
        }

        @Override
        public synchronized void onFar() {
            clearPendingRequests();
            postDelayed(mUnblankRunnable, PROXIMITY_UNBLANK_DELAY_MILLIS);
        }

        /** Removed any delayed requests that may be pending. */
        public synchronized void clearPendingRequests() {
            View blankView = findViewById(R.id.blank);
            blankView.removeCallbacks(mBlankRunnable);
            blankView.removeCallbacks(mUnblankRunnable);
        }

        /** Post a {@link Runnable} with a delay on the main thread. */
        private synchronized void postDelayed(Runnable runnable, long delayMillis) {
            // Post these instead of executing immediately so that:
            // - They are guaranteed to be executed on the main thread.
            // - If the sensor values changes rapidly for some time, the UI will not be
            //   updated immediately.
            View blankView = findViewById(R.id.blank);
            blankView.postDelayed(runnable, delayMillis);
        }
    }

    static final String[] CALL_LOG_PROJECTION = new String[] {
        CallLog.Calls.DATE,
        CallLog.Calls.DURATION,
        CallLog.Calls.NUMBER,
        CallLog.Calls.TYPE,
        CallLog.Calls.COUNTRY_ISO,
        CallLog.Calls.GEOCODED_LOCATION,
        CACHED_CONVERT_NUMBER, //MOT Calling code - IKDROIDPRO-283//Mot add weikai
        ContactsUtils.CallLog_NETWORK
    };

    static final int DATE_COLUMN_INDEX = 0;
    static final int DURATION_COLUMN_INDEX = 1;
    static final int NUMBER_COLUMN_INDEX = 2;
    static final int CALL_TYPE_COLUMN_INDEX = 3;
    static final int COUNTRY_ISO_COLUMN_INDEX = 4;
    static final int GEOCODED_LOCATION_COLUMN_INDEX = 5;
    static final int CACHED_CONVERT_NUMBER_COLUMN_INDEX = 6;
    static final int NETWORK_TYPE_COLUMN_INDEX = 7;

    private final View.OnClickListener mPrimaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            MEDialer.onDial(view.getContext(), ((ViewEntry)view.getTag()).primaryIntent, MEDialer.DialFrom.RECENT); //MOT Calling code IKHSS7-1479
            startActivity(((ViewEntry) view.getTag()).primaryIntent);
        }
    };

    private final View.OnClickListener mSecondaryActionListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            startActivity(((ViewEntry) view.getTag()).secondaryIntent);
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.call_detail);
        mAsyncTaskExecutor = AsyncTaskExecutors.createThreadPoolExecutor();
        mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mResources = getResources();

        mCallTypeHelper = new CallTypeHelper(getResources());
        mPhoneNumberHelper = new PhoneNumberHelper(mResources);
        mPhoneCallDetailsHelper = new PhoneCallDetailsHelper(mResources, mCallTypeHelper,
                mPhoneNumberHelper, this);  // Motorola, w21071, 2011-12-21, IKCBS-2736 context added.
        mVoicemailStatusHelper = new VoicemailStatusHelperImpl();
        mAsyncQueryHandler = new CallDetailActivityQueryHandler(this);
        mHeaderTextView = (TextView) findViewById(R.id.header_text);
        mHeaderOverlayView = findViewById(R.id.photo_text_bar);
        mStatusMessageView = findViewById(R.id.voicemail_status);
        mStatusMessageText = (TextView) findViewById(R.id.voicemail_status_message);
        mStatusMessageAction = (TextView) findViewById(R.id.voicemail_status_action);
        mMainActionView = (ImageView) findViewById(R.id.main_action);
        mMainActionPushLayerView = (ImageButton) findViewById(R.id.main_action_push_layer);
        mContactBackgroundView = (ImageView) findViewById(R.id.contact_background);
        mSmallContactBackgroundView = (ImageView) findViewById(R.id.small_contact_background);//MOTO Dialer Code IKHSS6-1147
        mDefaultCountryIso = ContactsUtils.getCurrentCountryIso(this);
        mContactPhotoManager = ContactPhotoManager.getInstance(this);
        mProximitySensorManager = new ProximitySensorManager(this, mProximitySensorListener);
        mContactInfoHelper = new ContactInfoHelper(this, ContactsUtils.getCurrentCountryIso(this));
        configureActionBar();
        optionallyHandleVoicemail();

        // IKHSS6-8052 FID:34118 Enhanced CityID
        mIsEcidAvailable = EcidContact.isECIDAvailable(this);
        mCallLocation = (TextView) findViewById(R.id.number_location);
        if (mLocationServiceManager == null) {
            mLocationServiceManager = new LocationServiceManager(getApplicationContext(),mCallLocation);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mLocationServiceManager != null) {
            mLocationServiceManager.checkService(true);
        }
        Log.d(TAG,"show wating dialog ...");
        showProgresDialog();
        updateData(getCallLogEntryUris());
    }

    /**
     * Handle voicemail playback or hide voicemail ui.
     * <p>
     * If the Intent used to start this Activity contains the suitable extras, then start voicemail
     * playback.  If it doesn't, then hide the voicemail ui.
     */
    private void optionallyHandleVoicemail() {
        View voicemailContainer = findViewById(R.id.voicemail_container);
        if (hasVoicemail()) {
            // Has voicemail: add the voicemail fragment.  Add suitable arguments to set the uri
            // to play and optionally start the playback.
            // Do a query to fetch the voicemail status messages.
            VoicemailPlaybackFragment playbackFragment = new VoicemailPlaybackFragment();
            Bundle fragmentArguments = new Bundle();
            fragmentArguments.putParcelable(EXTRA_VOICEMAIL_URI, getVoicemailUri());
            if (getIntent().getBooleanExtra(EXTRA_VOICEMAIL_START_PLAYBACK, false)) {
                fragmentArguments.putBoolean(EXTRA_VOICEMAIL_START_PLAYBACK, true);
            }
            playbackFragment.setArguments(fragmentArguments);
            voicemailContainer.setVisibility(View.VISIBLE);
            getFragmentManager().beginTransaction()
                    .add(R.id.voicemail_container, playbackFragment).commitAllowingStateLoss();
            mAsyncQueryHandler.startVoicemailStatusQuery(getVoicemailUri());
            markVoicemailAsRead(getVoicemailUri());
        } else {
            // No voicemail uri: hide the status view.
            mStatusMessageView.setVisibility(View.GONE);
            voicemailContainer.setVisibility(View.GONE);
        }
    }

    private boolean hasVoicemail() {
        return getVoicemailUri() != null;
    }

    private Uri getVoicemailUri() {
        return getIntent().getParcelableExtra(EXTRA_VOICEMAIL_URI);
    }

    private void markVoicemailAsRead(final Uri voicemailUri) {
        mAsyncTaskExecutor.submit(Tasks.MARK_VOICEMAIL_READ, new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {
                ContentValues values = new ContentValues();
                values.put(Voicemails.IS_READ, true);
                getContentResolver().update(voicemailUri, values,
                        Voicemails.IS_READ + " = 0", null);
                return null;
            }
        });
    }

    /**
     * Returns the list of URIs to show.
     * <p>
     * There are two ways the URIs can be provided to the activity: as the data on the intent, or as
     * a list of ids in the call log added as an extra on the URI.
     * <p>
     * If both are available, the data on the intent takes precedence.
     */
    private Uri[] getCallLogEntryUris() {
        Uri uri = getIntent().getData();
        if (uri != null) {
            // If there is a data on the intent, it takes precedence over the extra.
            return new Uri[]{ uri };
        }
        long[] ids = getIntent().getLongArrayExtra(EXTRA_CALL_LOG_IDS);
        Uri[] uris = new Uri[ids.length];
        for (int index = 0; index < ids.length; ++index) {
            uris[index] = ContentUris.withAppendedId(Calls.CONTENT_URI_WITH_VOICEMAIL, ids[index]);
        }
        return uris;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL: {
                // Make sure phone isn't already busy before starting direct call
                TelephonyManager tm = (TelephonyManager)
                        getSystemService(Context.TELEPHONY_SERVICE);
                if (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
                    Intent callIntent;
                    if (ContactsUtils.haveTwoCards(getApplicationContext())) {
                        callIntent = new Intent(ContactsUtils.TRANS_DIALPAD);
                        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        callIntent.putExtra("phoneNumber", mNumber);
                    } else {
                        callIntent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                                Uri.fromParts("tel", mNumber, null));
                    }
                    MEDialer.onDial(this, callIntent, MEDialer.DialFrom.RECENT); //MOT Calling code IKHSS7-1479
                    startActivity(callIntent);
                    return true;
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }
 private void hideProgressDialog(){
    	if(mProgressDialog!=null && mProgressDialog.isShowing() ){    		
    		mHandler.post(new Runnable(){
    			public void run() {
    				mProgressDialog.dismiss();    				
    			}});        	    		
    	}
    }
    private void showProgresDialog(){
    	if(mProgressDialog == null ){    		
    		 mProgressDialog = new ProgressDialog(this);                  
             mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
             mProgressDialog.setMessage(getResources().getString(R.string.wait_read_calllog));
             mProgressDialog.setCancelable(false);
             mProgressDialog.setCanceledOnTouchOutside(false);            
    	}
    	
    	mHandler.post(new Runnable(){
			public void run() {
				   mProgressDialog.show();
				
			}});
      

    }
    /**
     * Update user interface with details of given call.
     *
     * @param callUris URIs into {@link CallLog.Calls} of the calls to be displayed
     */
    private void updateData(final Uri... callUris) {
        class UpdateContactDetailsTask extends AsyncTask<Void, Void, PhoneCallDetails[]> {
            @Override
            public PhoneCallDetails[] doInBackground(Void... params) {

                // TODO: All phone calls correspond to the same person, so we can make a single
                // lookup.
                final int numCalls = callUris.length;
                PhoneCallDetails[] details = new PhoneCallDetails[numCalls];
                try {
                    for (int index = 0; index < numCalls; ++index) {
                        details[index] = getPhoneCallDetailsForUri(callUris[index]);
                    }
                    return details;
                } catch (IllegalArgumentException e) {
                    // Something went wrong reading in our primary data.
                    Log.w(TAG, "invalid URI starting call details", e);
                    return null;
                }
            }

            @Override
            public void onPostExecute(PhoneCallDetails[] details) {
                hideProgressDialog();
                if (details == null) {
                    // Somewhere went wrong: we're going to bail out and show error to users.
                    Toast.makeText(CallDetailActivity.this, R.string.toast_call_detail_error,
                            Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // We know that all calls are from the same number and the same contact, so pick the
                // first.
                PhoneCallDetails firstDetails = details[0];
                mNumber = firstDetails.number.toString();
                //add by txbv34 
                if (!TextUtils.isEmpty(firstDetails.name)) 
                mName = firstDetails.name.toString();
                final Uri contactUri = firstDetails.contactUri;
                final Uri photoUri = firstDetails.photoUri;

                //MOT Dialer Start
                final boolean isVoicemailNumber = mPhoneNumberHelper.isVoicemailNumber(mNumber);
				/* Modifyed for switchuitwo-381 begin */
				// final boolean isEmergency = PhoneNumberUtils.isLocalEmergencyNumber(mNumber, CallDetailActivity.this);
				final boolean isEmergency = ContactsUtils.isEmergencyNumber(
						mNumber, CallDetailActivity.this);
				/* Modifyed for switchuitwo-381 end */
                //MOT Dialer End
                // Set the details header, based on the first phone call.
                mPhoneCallDetailsHelper.setCallDetailsHeader(mHeaderTextView, firstDetails, isVoicemailNumber, isEmergency); //MOT Dialer Code

                // Cache the details about the phone number.
                final Uri numberCallUri = mPhoneNumberHelper.getCallUri(mNumber);
                final boolean canPlaceCallsTo = mPhoneNumberHelper.canPlaceCallsTo(mNumber);
                //final boolean isVoicemailNumber = mPhoneNumberHelper.isVoicemailNumber(mNumber);
                final boolean isSipNumber = mPhoneNumberHelper.isSipNumber(mNumber);

                // Let user view contact details if they exist, otherwise add option to create new
                // contact from this number.
                final Intent mainActionIntent;
                final int mainActionIcon;
                final String mainActionDescription;

                final CharSequence nameOrNumber;
                if (!TextUtils.isEmpty(firstDetails.name)) {
                    nameOrNumber = firstDetails.name;
                } else {
                    nameOrNumber = firstDetails.number;
                }

                // IKHSS6-8052 FID:34118 Enhanced CityID BEGIN
                if (mIsEcidAvailable) {
                    View convertView = findViewById(R.id.call_and_sms);
                    TextView cityIdView = (TextView) convertView.findViewById(R.id.call_and_sms_cityid);
                    if ( cityIdView != null ) {
                        String strCityID = EcidContact.computeDisplayName( firstDetails.number.toString(), cityIdView);
                        cityIdView.setText(strCityID);
                    }
                } // IKHSS6-8052 FID:34118 Enhanced CityID END

                if (contactUri != null) {
                    mainActionIntent = new Intent(Intent.ACTION_VIEW, contactUri);
                    mainActionIcon = R.drawable.ic_see_contacts; //MOTO Dialer Code IKHSS7-1980
                    mainActionDescription =
                            getString(R.string.description_view_contact, nameOrNumber);
                } else if (isVoicemailNumber || isEmergency) {  // MOT Dialer Code
                    mainActionIntent = null;
                    mainActionIcon = 0;
                    mainActionDescription = null;
                } else if (isSipNumber) {
                    // TODO: This item is currently disabled for SIP addresses, because
                    // the Insert.PHONE extra only works correctly for PSTN numbers.
                    //
                    // To fix this for SIP addresses, we need to:
                    // - define ContactsContract.Intents.Insert.SIP_ADDRESS, and use it here if
                    //   the current number is a SIP address
                    // - update the contacts UI code to handle Insert.SIP_ADDRESS by
                    //   updating the SipAddress field
                    // and then we can remove the "!isSipNumber" check above.
                    mainActionIntent = null;
                    mainActionIcon = 0;
                    mainActionDescription = null;
                } else if (canPlaceCallsTo) {
                    mainActionIntent = new Intent(ADD_CONTACT_DIALOG);//MOT Dialer Code - IKHSS6-13862
                    mainActionIntent.putExtra(Insert.PHONE, mNumber);

                    // IKHSS6-8052 FID:34118 Enhanced CityID BEGIN
                    // CEQUINT code begin
                    // CEQUINT Add name to contact
                    if (mIsEcidAvailable) {
                        // TODO: Currently, pass call type default as incoming. Because ECID
                        // will not lookup outgoing types, if the last call for this number
                        // was outgoing, we'll get no information.
                        EcidContact ecidInfo = EcidContact.doCacheLookup(CallDetailActivity.this, mNumber, Calls.INCOMING_TYPE);

                        if ( ecidInfo != null ) {
                            String contactName = ecidInfo.getContactDisplayName();
                            if (contactName != null && !TextUtils.equals(contactName, "Unknown Name"))
                                mainActionIntent.putExtra(Insert.NAME, contactName);
    
                            // CEQUINT Add Business Name to Contact
                            String bizName = ecidInfo.getBizName();
                            if (bizName != null)
                                //ContactsContract.Intents.Insert.COMPANY
                                mainActionIntent.putExtra(Insert.COMPANY, bizName);
                        }
                    }
                    // CEQUINT code end
                    // IKHSS6-8052 FID:34118 Enhanced CityID END

                    mainActionIcon = R.drawable.ic_add_contact_holo_dark; //MOTO Dialer Code IKHSS7-1980, IKHSS7-6208
                    mainActionDescription = getString(R.string.description_add_contact);
                } else {
                    // If we cannot call the number, when we probably cannot add it as a contact either.
                    // This is usually the case of private, unknown, or payphone numbers.
                    mainActionIntent = null;
                    mainActionIcon = 0;
                    mainActionDescription = null;
                }

                if (mainActionIntent == null) {
                    mMainActionView.setVisibility(View.INVISIBLE);
                    mMainActionPushLayerView.setVisibility(View.GONE);
                    mHeaderTextView.setVisibility(View.INVISIBLE);
                    mHeaderOverlayView.setVisibility(View.INVISIBLE);
                } else {
                    mMainActionView.setVisibility(View.VISIBLE);
                    mMainActionView.setImageResource(mainActionIcon);
                    mMainActionPushLayerView.setVisibility(View.VISIBLE);
                    mMainActionPushLayerView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                        	if(RomUtility.isOutofMemory()){
                            	Toast.makeText(CallDetailActivity.this, R.string.rom_full, Toast.LENGTH_LONG).show();
                            	return ;
                            }
                            startActivity(mainActionIntent);
                        }
                    });
                    mMainActionPushLayerView.setContentDescription(mainActionDescription);
                    mHeaderTextView.setVisibility(View.VISIBLE);
                    mHeaderOverlayView.setVisibility(View.VISIBLE);
                }

                // This action allows to call the number that places the call.
                if (canPlaceCallsTo) {
                    final CharSequence displayNumber =
                            mPhoneNumberHelper.getDisplayNumber(
                                    firstDetails.number, firstDetails.formattedNumber);

                    Intent intent;
                    if ((!mPhoneNumberHelper.isSipNumber(mNumber)
                            || !mPhoneNumberHelper.isVoicemailNumber(mNumber))
                            && ContactsUtils.haveTwoCards(getApplicationContext())) {
                        intent = new Intent(ContactsUtils.TRANS_DIALPAD);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("phoneNumber", mNumber);
                    } else {
                        intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, numberCallUri);
                    }
                    ViewEntry entry = new ViewEntry(
                            getString(R.string.menu_callNumber, displayNumber),
                            intent,
                            getString(R.string.description_call, nameOrNumber));

                    // Only show a label if the number is shown and it is not a SIP address.
                    if (!TextUtils.isEmpty(firstDetails.name)
                            && !TextUtils.isEmpty(firstDetails.number)
                            && !PhoneNumberUtils.isUriNumber(firstDetails.number.toString())) {
                        entry.label = Phone.getTypeLabel(mResources, firstDetails.numberType,
                                firstDetails.numberLabel);
                    }

                    // The secondary action allows to send an SMS to the number that placed the
                    // call.
                    if (mPhoneNumberHelper.canSendSmsTo(mNumber)) {
                        entry.setSecondaryAction(
                                R.drawable.ic_text_holo_light,  //MOTO Dialer Code IKHSS7-1980
                                new Intent(Intent.ACTION_SENDTO,
                                           Uri.fromParts("sms", mNumber, null)),
                                getString(R.string.description_send_text_message, nameOrNumber));
                    }

                    configureCallButton(entry);
                } else {
                    disableCallButton();
                }

                mHasEditNumberBeforeCallOption =
                        canPlaceCallsTo && !isSipNumber && !isVoicemailNumber;
                mHasTrashOption = hasVoicemail();
                mHasRemoveFromCallLogOption = !hasVoicemail();
                invalidateOptionsMenu();

                ListView historyList = (ListView) findViewById(R.id.history);
                historyList.setAdapter(
                        new CallDetailHistoryAdapter(CallDetailActivity.this, mInflater,
                                mCallTypeHelper, details, hasVoicemail(), canPlaceCallsTo,
                                findViewById(R.id.controls)));
                BackScrollManager.bind(
                        new ScrollableHeader() {
                            private View mControls = findViewById(R.id.controls);
                            private View mPhoto = findViewById(R.id.contact_background_sizer);
                            private View mHeader = findViewById(R.id.photo_text_bar);
                            private View mSeparator = findViewById(R.id.blue_separator);

                            @Override
                            public void setOffset(int offset) {
                                mControls.setY(-offset);
                            }

                            @Override
                            public int getMaximumScrollableHeaderOffset() {
                                // We can scroll the photo out, but we should keep the header if
                                // present.
                                if (mHeader.getVisibility() == View.VISIBLE) {
                                    return mPhoto.getHeight() - mHeader.getHeight();
                                } else {
                                    // If the header is not present, we should also scroll out the
                                    // separator line.
                                    return mPhoto.getHeight() + mSeparator.getHeight();
                                }
                            }
                        },
                        historyList);

                loadContactPhotos(photoUri, contactUri, isVoicemailNumber, isEmergency, TextUtils.isEmpty(firstDetails.name)); //MOT Dialer Code
                findViewById(R.id.call_detail).setVisibility(View.VISIBLE);
                if (mLocationServiceManager != null) {
                    mLocationServiceManager.showLocation(mNumber);
                }
            }
        }
        mAsyncTaskExecutor.submit(Tasks.UPDATE_PHONE_CALL_DETAILS, new UpdateContactDetailsTask());
    }

    /** Return the phone call details for a given call log URI. */
    private PhoneCallDetails getPhoneCallDetailsForUri(Uri callUri) {
        ContentResolver resolver = getContentResolver();
        Cursor callCursor = resolver.query(callUri, CALL_LOG_PROJECTION, null, null, null);
        try {
            if (callCursor == null || !callCursor.moveToFirst()) {
                throw new IllegalArgumentException("Cannot find content: " + callUri);
            }

            // Read call log specifics.
            String number = callCursor.getString(NUMBER_COLUMN_INDEX);
            long date = callCursor.getLong(DATE_COLUMN_INDEX);
            long duration = callCursor.getLong(DURATION_COLUMN_INDEX);
            int callType = callCursor.getInt(CALL_TYPE_COLUMN_INDEX);
            int networkType = callCursor.getInt(NETWORK_TYPE_COLUMN_INDEX);
            String countryIso = callCursor.getString(COUNTRY_ISO_COLUMN_INDEX);
            final String geocode = callCursor.getString(GEOCODED_LOCATION_COLUMN_INDEX);
            String convertNumber = callCursor.getString(CACHED_CONVERT_NUMBER_COLUMN_INDEX);

            if (TextUtils.isEmpty(countryIso)) {
                countryIso = mDefaultCountryIso;
            }

            // Formatted phone number.
            final CharSequence formattedNumber;
            // Read contact specifics.
            final CharSequence nameText;
            final int numberType;
            final CharSequence numberLabel;
            final Uri photoUri;
            final Uri lookupUri;
            // If this is not a regular number, there is no point in looking it up in the contacts.
            ContactInfo info =
                    mPhoneNumberHelper.canPlaceCallsTo(number)
                    && !mPhoneNumberHelper.isVoicemailNumber(number)
                            ? mContactInfoHelper.lookupNumber(number, countryIso, convertNumber)
                            : null;
            if (info == null) {
                formattedNumber = mPhoneNumberHelper.getDisplayNumber(number, null);
                nameText = "";
                numberType = 0;
                numberLabel = "";
                photoUri = null;
                lookupUri = null;
            } else {
                formattedNumber = info.formattedNumber;
                nameText = info.name;
                numberType = info.type;
                numberLabel = info.label;
                photoUri = info.photoUri;
                lookupUri = info.lookupUri;
            }
            return new PhoneCallDetails(number, formattedNumber, countryIso, geocode,
                    new int[]{ callType }, new int[]{ networkType }, date, duration,
                    nameText, numberType, numberLabel, lookupUri, photoUri);
        } finally {
            if (callCursor != null) {
                callCursor.close();
            }
        }
    }

    /** Load the contact photos and places them in the corresponding views. */
    private void loadContactPhotos(Uri photoUri, Uri contactUri, boolean isVoicemail, boolean isEmergency, boolean isNotSaved) {

	        // MOT Dialer Start
	      if (isVoicemail) {
            mSmallContactBackgroundView.setImageResource(
                        R.drawable.ic_launcher_voicemail);
            mSmallContactBackgroundView.setVisibility(View.VISIBLE);//MOTO Dialer Code IKHSS6-1147
            mContactBackgroundView.setVisibility(View.GONE);//MOTO Dialer Code IKHSS6-1147
        } else if (isEmergency) {
            mSmallContactBackgroundView.setImageResource(
                        R.drawable.picture_emergency);
            mSmallContactBackgroundView.setVisibility(View.VISIBLE);//MOTO Dialer Code IKHSS6-1147
            mContactBackgroundView.setVisibility(View.GONE);//MOTO Dialer Code IKHSS6-1147
        } else if (isNotSaved) {
            mContactBackgroundView.setImageResource(
                        R.drawable.picture_unknown); //MOTO Dialer Code IKHSS7-1980
            mSmallContactBackgroundView.setVisibility(View.GONE);//MOTO Dialer Code IKHSS6-1147
            mContactBackgroundView.setVisibility(View.VISIBLE);//MOTO Dialer Code IKHSS6-1147
        } else if (photoUri != null) {
            mContactPhotoManager.loadPhoto(mContactBackgroundView, photoUri, true, true);
            mSmallContactBackgroundView.setVisibility(View.GONE);//MOTO Dialer Code IKHSS6-1147
            mContactBackgroundView.setVisibility(View.VISIBLE);//MOTO Dialer Code IKHSS6-1147
	      }
        else {
            mContactPhotoManager.loadDefaultPhoto(mContactBackgroundView, contactUri, true, true);
            mSmallContactBackgroundView.setVisibility(View.GONE);//MOTO Dialer Code IKHSS6-1147
            mContactBackgroundView.setVisibility(View.VISIBLE);//MOTO Dialer Code IKHSS6-1147
	      }
	        // MOT Dialer End
    }

    static final class ViewEntry {
        public final String text;
        public final Intent primaryIntent;
        /** The description for accessibility of the primary action. */
        public final String primaryDescription;

        public CharSequence label = null;
        /** Icon for the secondary action. */
        public int secondaryIcon = 0;
        /** Intent for the secondary action. If not null, an icon must be defined. */
        public Intent secondaryIntent = null;
        /** The description for accessibility of the secondary action. */
        public String secondaryDescription = null;

        public ViewEntry(String text, Intent intent, String description) {
            this.text = text;
            primaryIntent = intent;
            primaryDescription = description;
        }

        public void setSecondaryAction(int icon, Intent intent, String description) {
            secondaryIcon = icon;
            secondaryIntent = intent;
            secondaryDescription = description;
        }
    }

    /** Disables the call button area, e.g., for private numbers. */
    private void disableCallButton() {
        findViewById(R.id.call_and_sms).setVisibility(View.GONE);
    }

    /** Configures the call button area using the given entry. */
    private void configureCallButton(ViewEntry entry) {
        View convertView = findViewById(R.id.call_and_sms);
        convertView.setVisibility(View.VISIBLE);

        ImageView icon = (ImageView) convertView.findViewById(R.id.call_and_sms_icon);
        View divider = convertView.findViewById(R.id.call_and_sms_divider);
        TextView text = (TextView) convertView.findViewById(R.id.call_and_sms_text);

        // IKHSS6-8052 FID:34118 Enhanced CityID BEGIN
        if (mIsEcidAvailable) {
            TextView cityIdView = (TextView) convertView.findViewById(R.id.call_and_sms_cityid);
            if ( cityIdView != null )
                if ( !TextUtils.isEmpty(cityIdView.getText()) )
                    cityIdView.setVisibility(View.VISIBLE);
                else
                    cityIdView.setVisibility(View.GONE);
        }
        // IKHSS6-8052 FID:34118 Enhanced CityID END

        View mainAction = convertView.findViewById(R.id.call_and_sms_main_action);
        mainAction.setOnClickListener(mPrimaryActionListener);
        mainAction.setTag(entry);
        mainAction.setContentDescription(entry.primaryDescription);

        if (entry.secondaryIntent != null) {
            icon.setOnClickListener(mSecondaryActionListener);
            icon.setImageResource(entry.secondaryIcon);
            icon.setVisibility(View.VISIBLE);
            icon.setTag(entry);
            icon.setContentDescription(entry.secondaryDescription);
            divider.setVisibility(View.VISIBLE);
        } else {
            icon.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
        }
        text.setText(entry.text);

        TextView label = (TextView) convertView.findViewById(R.id.call_and_sms_label);
        if (TextUtils.isEmpty(entry.label)) {
            label.setVisibility(View.GONE);
        } else {
            label.setText(entry.label);
            label.setVisibility(View.VISIBLE);
        }
    }

    protected void updateVoicemailStatusMessage(Cursor statusCursor) {
        if (statusCursor == null) {
            mStatusMessageView.setVisibility(View.GONE);
            return;
        }
        final StatusMessage message = getStatusMessage(statusCursor);
        if (message == null || !message.showInCallDetails()) {
            mStatusMessageView.setVisibility(View.GONE);
            return;
        }

        mStatusMessageView.setVisibility(View.VISIBLE);
        mStatusMessageText.setText(message.callDetailsMessageId);
        if (message.actionMessageId != -1) {
            mStatusMessageAction.setText(message.actionMessageId);
        }
        if (message.actionUri != null) {
            mStatusMessageAction.setClickable(true);
            mStatusMessageAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(Intent.ACTION_VIEW, message.actionUri));
                }
            });
        } else {
            mStatusMessageAction.setClickable(false);
        }
    }

    private StatusMessage getStatusMessage(Cursor statusCursor) {
        List<StatusMessage> messages = mVoicemailStatusHelper.getStatusMessages(statusCursor);
        if (messages.size() == 0) {
            return null;
        }
        // There can only be a single status message per source package, so num of messages can
        // at most be 1.
        if (messages.size() > 1) {
            Log.w(TAG, String.format("Expected 1, found (%d) num of status messages." +
                    " Will use the first one.", messages.size()));
        }
        return messages.get(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.call_details_options, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // This action deletes all elements in the group from the call log.
        // We don't have this action for voicemails, because you can just use the trash button.
        menu.findItem(R.id.menu_remove_from_call_log).setVisible(mHasRemoveFromCallLogOption);
        menu.findItem(R.id.menu_edit_number_before_call).setVisible(mHasEditNumberBeforeCallOption);
        menu.findItem(R.id.menu_trash).setVisible(mHasTrashOption);
        final boolean canPlaceCallsTo = mPhoneNumberHelper.canPlaceCallsTo(mNumber);
        menu.findItem(R.id.menu_send_via_sms).setVisible(canPlaceCallsTo);
        TelephonyManager defaultMgr = TelephonyManager.getDefault();
        TelephonyManager secondaryMgr = null;
        final boolean isDefaultRoaming = defaultMgr.isNetworkRoaming();
        final boolean isDefaultPhoneEnabled = (defaultMgr.getSimState() == TelephonyManager.SIM_STATE_READY);
        boolean isSecondaryRoaming = false;
        boolean isSecondaryPhoneEnabled = false;
        if (PhoneModeManager.isDmds()) {
            /* to-pass-build, Xinyu Liu/dcjf34 
            secondaryMgr = TelephonyManager.getDefault(false);
            isSecondaryRoaming = secondaryMgr.isNetworkRoaming();
            isSecondaryPhoneEnabled = secondaryMgr.getSimState() == TelephonyManager.SIM_STATE_READY;
            */
            menu.findItem(R.id.menu_ip_call).setVisible(false);
            menu.findItem(R.id.menu_int_roaming_call).setVisible(false);
            menu.findItem(R.id.menu_int_roaming_call_back).setVisible(false);
            if (isDefaultRoaming && isSecondaryRoaming) {
                menu.findItem(R.id.menu_int_roaming_call_back).setVisible(true);
            } else if (isDefaultRoaming) {
                if (isSecondaryPhoneEnabled) {
                    menu.findItem(R.id.menu_ip_call).setVisible(true);
                }
                if (isDefaultPhoneEnabled && defaultMgr.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
                    menu.findItem(R.id.menu_int_roaming_call_back).setVisible(true);
                }
            } else if (isSecondaryRoaming) {
                if (isDefaultPhoneEnabled) {
                    menu.findItem(R.id.menu_ip_call).setVisible(true);
                }
                if (isSecondaryPhoneEnabled && secondaryMgr.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
                    menu.findItem(R.id.menu_int_roaming_call_back).setVisible(true);
                }
            } else {
                menu.findItem(R.id.menu_ip_call).setVisible(true);
            }
        } else {
            menu.findItem(R.id.menu_ip_call).setVisible(canPlaceCallsTo && isDefaultPhoneEnabled && !isDefaultRoaming);
            boolean isRoamingCallSupported = (!SystemProperties.get("ro.product.locale.region","CN").equalsIgnoreCase("TW")) && PhoneModeManager.IsRoamingCallSupported();
            menu.findItem(R.id.menu_int_roaming_call).setVisible(canPlaceCallsTo && isDefaultPhoneEnabled && isDefaultRoaming && isRoamingCallSupported);
            boolean isRoamingCallbackSupported = (defaultMgr.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) && PhoneModeManager.IsRoamingCallBackSupported();
            menu.findItem(R.id.menu_int_roaming_call_back).setVisible(canPlaceCallsTo && isDefaultPhoneEnabled && isDefaultRoaming && isRoamingCallbackSupported);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onHomeSelected();
                return true;
            }

            // All the options menu items are handled by onMenu... methods.
            default:
                throw new IllegalArgumentException();
        }
    }

    public void onMenuRemoveFromCallLog(MenuItem menuItem) {
    	if(RomUtility.isOutofMemory()){
        	Toast.makeText(CallDetailActivity.this, R.string.rom_full, Toast.LENGTH_LONG).show();
        	return ;
        }
        final StringBuilder callIds = new StringBuilder();
        for (Uri callUri : getCallLogEntryUris()) {
            if (callIds.length() != 0) {
                callIds.append(",");
            }
            callIds.append(ContentUris.parseId(callUri));
        }
        mAsyncTaskExecutor.submit(Tasks.REMOVE_FROM_CALL_LOG_AND_FINISH,
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    public Void doInBackground(Void... params) {
                        getContentResolver().delete(Calls.CONTENT_URI_WITH_VOICEMAIL,
                                Calls._ID + " IN (" + callIds + ")", null);
                        return null;
                    }

                    @Override
                    public void onPostExecute(Void result) {
                        finish();
                    }
                });
    }

    public void onMenuEditNumberBeforeCall(MenuItem menuItem) {
        startActivity(new Intent(Intent.ACTION_DIAL, mPhoneNumberHelper.getCallUri(mNumber)));
    }

    public void onMenuTrashVoicemail(MenuItem menuItem) {
        final Uri voicemailUri = getVoicemailUri();
        mAsyncTaskExecutor.submit(Tasks.DELETE_VOICEMAIL_AND_FINISH,
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    public Void doInBackground(Void... params) {
                        getContentResolver().delete(voicemailUri, null, null);
                        return null;
                    }
                    @Override
                    public void onPostExecute(Void result) {
                        finish();
                    }
                });
    }

    public void onMenuAddToBlackList(MenuItem menuItem) {
    	if(RomUtility.isOutofMemory()){
        	Toast.makeText(CallDetailActivity.this, R.string.rom_full, Toast.LENGTH_LONG).show();
        	return ;
        }
        Intent blackintent = new Intent(Intent.ACTION_INSERT);
        blackintent.setType("vnd.android.cursor.item/vnd.motorola.firewall.name");
        //add by txbv34 for IKCBSMMCPPRC-1551
    	if(mName != null && mName.length() > 0){
            blackintent.putExtra("blocktype", "blackname");
            Log.d(TAG,"onMenuAddToBlackList,blackname");
            blackintent.putExtra("blackname", mNumber);

    	}else{
            blackintent.putExtra("blocktype", "blacklist");
            Log.d(TAG,"onMenuAddToBlackList,blacklist");
            blackintent.putExtra("blacklist", mNumber);
    	}   
       
        try {
            startActivity(blackintent);
        } catch (android.content.ActivityNotFoundException e) {
            // don't crash if not support.
        }
    }

    public void onMenuAddToWhiteList(MenuItem menuItem) {
    	if(RomUtility.isOutofMemory()){
        	Toast.makeText(CallDetailActivity.this, R.string.rom_full, Toast.LENGTH_LONG).show();
        	return ;
        }
        Intent whiteintent = new Intent(Intent.ACTION_INSERT);
        whiteintent.setType("vnd.android.cursor.item/vnd.motorola.firewall.name");
        //add by txbv34 for IKCBSMMCPPRC-1551
    	if(mName != null && mName.length() > 0){
            whiteintent.putExtra("blocktype", "whitename");
            whiteintent.putExtra("whitename", mNumber);

    	}else{
            whiteintent.putExtra("blocktype", "whitelist");
            whiteintent.putExtra("whitelist", mNumber);
    	}  
        try {
            startActivity(whiteintent);
        } catch (android.content.ActivityNotFoundException e) {
            // don't crash if not support.
        }
    }
    
    public void onMenuSendViaSms(MenuItem menuItem) {
    	Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("sms:"));
    	intent.putExtra("sms_body", mNumber);
    	intent.putExtra("exit_on_sent", true);
    	try {
    		startActivity(intent);
    	} catch (android.content.ActivityNotFoundException e) {
    		// don't crash if not support
    	}
    	
    }
    
    public void onMenuIpCall(MenuItem menuItem) {
        Intent intent = new Intent(ContactsUtils.TRANS_DIALPAD);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("phoneNumber", mNumber);
        intent.putExtra("isIpCall", true);
        startActivity(intent);
    }
    
    public void onMenuIntlRoamingCall(MenuItem menuItem) {
        Intent intent = new Intent(ContactsUtils.TRANS_DIALPAD);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("phoneNumber", mNumber);
        intent.putExtra(ContactsUtils.IntRoamCall, true);
        startActivity(intent);
    }
    
    public void onMenuIntlRoamingCallBack(MenuItem menuItem) {
        Intent intent = new Intent(ContactsUtils.TRANS_DIALPAD);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("phoneNumber", mNumber);
        intent.putExtra(ContactsUtils.IntRoamCallBackCall, true);
        startActivity(intent);
    }

    private void configureActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME|ActionBar.DISPLAY_SHOW_TITLE);
        }
    }

    /** Invoked when the user presses the home button in the action bar. */
    private void onHomeSelected() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Calls.CONTENT_URI);
        // This will open the call log even if the detail view has been opened directly.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        // Immediately stop the proximity sensor.
        disableProximitySensor(false);
        mProximitySensorListener.clearPendingRequests();
        super.onPause();
    }

    @Override
	protected void onStop() {
		super.onStop();
        if (mLocationServiceManager != null) {
            mLocationServiceManager.unbind();
        }
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
        if (mLocationServiceManager != null) {
            mLocationServiceManager = null;
        }
	}

	@Override
    public void enableProximitySensor() {
        mProximitySensorManager.enable();
    }

    @Override
    public void disableProximitySensor(boolean waitForFarState) {
        mProximitySensorManager.disable(waitForFarState);
    }
}
