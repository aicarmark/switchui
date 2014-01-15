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

package com.android.contacts.dialpad;

import static android.telephony.TelephonyManager.PHONE_TYPE_CDMA;
import static android.telephony.TelephonyManager.PHONE_TYPE_GSM;
import static android.telephony.TelephonyManager.PHONE_TYPE_NONE;

import com.android.contacts.LocationServiceManager;
import com.android.contacts.activities.DialpadActivity.ViewVisibilityListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.CallLog.Calls;
import android.provider.Contacts.Intents.Insert;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.provider.Contacts.PhonesColumns;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.Settings;
import android.preference.PreferenceManager;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.util.DisplayMetrics;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.BufferType;

import com.android.contacts.ContactsUtils;
import java.util.Locale;
import com.android.contacts.R;
import com.android.contacts.SpecialCharSequenceMgr;
import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.activities.DialtactsActivity.ViewPagerVisibilityListener;
import com.android.contacts.util.PhoneNumberFormatter;
import com.android.internal.telephony.ITelephony;
import com.android.phone.CallLogAsync;
//import com.android.phone.HapticFeedback;

import com.android.contacts.activities.DialpadActivity;
import com.android.contacts.spd.SpeedDialHelper;
import com.android.contacts.spd.Utils;
import com.android.contacts.util.Constants;
import com.motorola.contacts.util.MEDialer;
import com.motorola.android.telephony.PhoneModeManager;

//MOTO MOD BEGIN IKHSS7-2038
import com.motorola.internal.telephony.PhoneNumberUtilsExt;
//MOTO MOD END IKHSS7-2038
import com.android.contacts.RomUtility;

/**
 * Fragment that displays a twelve-key phone dialpad.
 */
public class DialpadFragment extends Fragment
        implements View.OnClickListener,
        View.OnLongClickListener, View.OnKeyListener,
        AdapterView.OnItemClickListener,
        TextWatcher,
        PopupMenu.OnMenuItemClickListener,
        ViewPagerVisibilityListener, ViewVisibilityListener {//MOT Dialer code IKHSS7-8639
    private static final String TAG = "DialpadFragment";
    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End

    private static final boolean DEBUG = true;

    private static final String EMPTY_NUMBER = "";

    /** The length of DTMF tones in milliseconds */
    private static final int TONE_LENGTH_MS = 150; //ChinaDev

    /** The DTMF tone volume relative to other sounds in the stream */
    private static final int TONE_RELATIVE_VOLUME = 50; //ChinaDev

    /** Stream type used to play the DTMF tones off call, and mapped to the volume control keys */
    private static final int DIAL_TONE_STREAM_TYPE = AudioManager.STREAM_SYSTEM; //MOT Dialer Code

    public interface Listener {
        public void onSearchButtonPressed();
    }

    /**
     * View (usually FrameLayout) containing mDigits field. This can be null, in which mDigits
     * isn't enclosed by the container.
     */
    private View mDigitsContainer;
    private EditText mDigits;

    private View mDelete;
    private ToneGenerator mToneGenerator;
    private Object mToneGeneratorLock = new Object();
    private View mDialpad;
    private View mAdditionalButtonsRow;

    //private View mSearchButton;//Google 4.0.4
    private View mMenuButton;
    private Listener mListener;

    //private View mDialButtonContainer;//Google 4.0.4
    private View mDialButton;
    private ListView mDialpadChooser;
    private DialpadChooserAdapter mDialpadChooserAdapter;

    /**
     * Regular expression prohibiting manual phone call. Can be empty, which means "no rule".
     */
    private String mProhibitedPhoneNumberRegexp;

    private boolean mShowOptionsMenu;

    // Last number dialed, retrieved asynchronously from the call DB
    // in onCreate. This number is displayed when the user hits the
    // send key and cleared in onPause.
    CallLogAsync mCallLog = new CallLogAsync();
    private String mLastNumberDialed = EMPTY_NUMBER;

    // determines if we want to playback local DTMF tones.
    private boolean mDTMFToneEnabled;

//MOT Dialer Start --Comment out haptic, use moto implemention
//    // Vibration (haptic feedback) for dialer key presses.
//    private HapticFeedback mHaptic = new HapticFeedback();
//MOT Dialer End

    /** Identifier for the "Add Call" intent extra. */
    static final String ADD_CALL_MODE_KEY = "add_call_mode";

    /**
     * Identifier for intent extra for sending an empty Flash message for
     * CDMA networks. This message is used by the network to simulate a
     * press/depress of the "hookswitch" of a landline phone. Aka "empty flash".
     *
     * TODO: Using an intent extra to tell the phone to send this flash is a
     * temporary measure. To be replaced with an ITelephony call in the future.
     * TODO: Keep in sync with the string defined in OutgoingCallBroadcaster.java
     * in Phone app until this is replaced with the ITelephony API.
     */
    static final String EXTRA_SEND_EMPTY_FLASH
            = "com.android.phone.extra.SEND_EMPTY_FLASH";

    private String mCurrentCountryIso;

    //MOT Dialer start
    //ChinaDev  private ImageView mLeftButton;
    private ImageView mRightButton;

    //ChinaDev private final String DIALER_BUTTON_LEFT = "dialer_button_left";
    private final String DIALER_BUTTON_RIGHT = "dialer_button_right";

    //ChinaDev private static final int ADDTOCONTACT = 0;
    private static final int SENDMESSAGE = 1;
    private static final int VOICECOMMAND =2;
    //ChinaDev private static final int SEARCH = 3;
    private static final int VOICEMAIL = 4;

    //ChinaDev int mButton_left_id;
    int mButton_right_id;

    Context mDialogContext = null;
	
    private boolean mMessageBtnFtrFlag = false; //MOT FID 35413-DialerLocalization IKCBS-2014
    private boolean mQwertySupported = false;
    private boolean mIsAdFtrOn = false; // Motorola IKOLYMP-5641 MO call KPI improvement
    private boolean mAdCDMAFeatureOn = false;
    private boolean mExcludeVoicemail = false;

    private SlidingDrawer mDrawer; //MOT Calling code IKPIM-660
    private boolean mNeedClosedDrawer = false; //Added for switchuitwo-350

    private SmartDialerAdapter mAdapter = null;

    // bkdp84 bug 15798 play DTMF tones when dialing with QWERTY Keypad
    private static final char[] DTMF_CHARS =
            new char [] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '#', '*'};
    // ] MOTO: End A

    static final String CALL_GSM_AD_ENABLE = "calling_gsm_ad_enabled"; // IKSTABLETWO-313:Add UI of AD in GSM mode
    static final String CALLED_BY = "called_by";
    static final String DIAL_BY_DIALER = "BY_DIALER";

    private boolean mIntentWithData; // MOTO A - used to finish this activity if not launched from home screen

    private boolean mIsCallBeenPlaced = false; // MOT Calling code - IKSTABLETWO-6716
    private boolean mIsNumberBeenDialed = false; //MOT Calling Code - IKSTABLETWOV-1777

    private String mVoiceMailNumber = null; // MOT Calling code - IKSTABLEFOURV-1933
    private String mVoiceMailTag = "";

    static final int VM_SELECTED = 0;
    static final int VVM_SELECTED = 1;
    static final int REMINDER = 2;
    final static String VM_VVM_SELECTION = "vm_vvm_selection";
    final static String VM_VVM_ROAMING_SELECTION = "vm_vvm_roaming_selection";
    final static String VVM_PACKAGE_NAME="com.motorola.vvm";
    final static String VVM_ACTIVITY_NAME="com.motorola.vvm.ui.VvmMainActivity";

    //MOTO MOD BEGIN IKPIM-1133
    final static String ADD_CONTACT_DIALOG="com.motorola.contacts.ACTION_ADD_CONTACT_DIALOG";
    //MOTO MOD END IKPIM-1133

    private PopupMenu mPopupMenu; //MOT Dialer Code - IKHSS7-7634
    private boolean fragmentVisibilty = false; //MOT Dialer code IKHSS7-8639
    //MOT Dialer End

    //ChinaDev Dialer
    private View callByCDMA, callByGSM;
    private View callBtnDivider;
    private View mEmergencyButton;
    private View mVoiceCallButton;
    private View mVideoCallButton;
    private boolean isGSMEnabled = false;
    private boolean isCDMAEnabled = false;
    private int mDefaultPhoneType = PHONE_TYPE_NONE;
    private int mSecondaryPhoneType = PHONE_TYPE_NONE;    
    private ListView mList;
    private View emptyText;
    private View spaceHolder;
    private LocationTextWatcher mLocTxtwatcher;
    private MyTextWatcher mTxtwatcher;
    private TextView mCallLocation;
    private TextView mSearchMatchesTv;
    private LinearLayout dialLocationLayout;
    private LocationServiceManager mLocationServiceManager = null;
    private boolean mIsAddCallMode;
    private Uri mSelectedContactUri;
    private String mVoiceMailNumberGSM = null;
    private String mVoiceMailNumberCDMA = null;
    private static final int MAX_VISIBLE_ITEMS_NORMAL = 3; //visible list items in normal dialer screen
    private static final int MAX_VISIBLE_ITEMS_FULL = 4; //visible list items in full dialer screen
    private boolean isDualMode;
    private static final String LIST_STATE_KEY = "liststate";
    private static final String FOCUS_KEY = "focused";
    private Parcelable mListState = null;
    private boolean mListHasFocus;
    private boolean mMultiPartyFeatureOn = false;
    private ServiceState mServiceState = null;
    private ServiceState mSecondServiceState = null;
    private final BroadcastReceiver mReceiver = new CallButtonBroadcastReceiver();
    private static final int UPDATE_CALLBUTTON=50;
    private static final long UPDATE_CALLBUTTON_DELAY=4000;
    private boolean isLastTypeUnknown = false;
    private ImageButton unhideDrawerBtn;
    private boolean isPortrait;
    private View dialerLayout;
    private String ip_cdma;
    private String ip_gsm;
    //add by txbv34 for IKCBSMMCPPRC-1358
    private boolean isIpCalling = false;
    
    private boolean isIpCall = false;
    private boolean isIntRoamCallBackCall = false;
    private boolean isIntRoamCall = false;
    private static boolean isDefaultRoaming = false;
    private static boolean isSecondaryRoaming = false;
    private static final int NO_CDMA_IP_SET_DIALOG = 100;
    private static final int NO_GSM_IP_SET_DIALOG = 200;

    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case UPDATE_CALLBUTTON:
                    updateCallButton();
                    break;
            }
        }
    };
    
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        /**
         * Listen for phone state changes so that we can take down the
         * "dialpad chooser" if the phone becomes idle while the
         * chooser UI is visible.
         */
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            // Log.i(TAG, "PhoneStateListener.onCallStateChanged: "
            //       + state + ", '" + incomingNumber + "'");
            if (!phoneIsInUse()){
                mDigits.setHint(null);
            }
            if ((state == TelephonyManager.CALL_STATE_IDLE) && dialpadChooserVisible()) {
                Log.i(TAG, "Call ended with dialpad chooser visible!  Taking it down...");
                // Note there's a race condition in the UI here: the
                // dialpad chooser could conceivably disappear (on its
                // own) at the exact moment the user was trying to select
                // one of the choices, which would be confusing.  (But at
                // least that's better than leaving the dialpad chooser
                // onscreen, but useless...)
                if (!phoneIsInUse()){//ChinaDev
                    showDialpadChooser(false);
                }
            }
        }

        /**
         * ChinaDev Listen for service state changes so that we can update call button accordingly
         * for example, when airplane mode switches
         */
        @Override
        public void onServiceStateChanged(ServiceState state) {
            if (mServiceState != null) {
                if (mServiceState.getState() != state.getState()) {
                    Log.d(TAG, "mPhoneStateListener, onServiceStateChanged, update call button, new state = "+state.getState());
                    mHandler.removeMessages(UPDATE_CALLBUTTON);
                    Message msg = Message.obtain(mHandler,UPDATE_CALLBUTTON);
                    mHandler.sendMessageDelayed(msg, ContactsUtils.UPDATE_VIEWS_DELAY);
                }
            }
            mServiceState = state;
        }

    };

    //ChinaDev
    PhoneStateListener mSecondPhoneStateListener = new PhoneStateListener() {
        /**
         * Listen for phone state changes so that we can take down the
         * "dialpad chooser" if the phone becomes idle while the
         * chooser UI is visible.
         */
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            // Log.i(TAG, "PhoneStateListener.onCallStateChanged: "
            //       + state + ", '" + incomingNumber + "'");
            if ((state == TelephonyManager.CALL_STATE_IDLE) && dialpadChooserVisible()) {
                Log.d(TAG, "SecCall ended with dialpad chooser visible!  Taking it down...");
                // Note there's a race condition in the UI here: the
                // dialpad chooser could conceivably disappear (on its
                // own) at the exact moment the user was trying to select
                // one of the choices, which would be confusing.  (But at
                // least that's better than leaving the dialpad chooser
                // onscreen, but useless...)
                if (!phoneIsInUse()){
                    showDialpadChooser(false);
                }
            }
            if (!phoneIsInUse()){
                mDigits.setHint(null);
            }
        }

        /**
         * Listen for service state changes so that we can update call button accordingly
         * for example, when airplane mode switches
         */
        @Override
        public void onServiceStateChanged(ServiceState state) {
            if (mSecondServiceState != null) {
                if (mSecondServiceState.getState() != state.getState()) {
                    Log.d(TAG, "mSecondPhoneStateListener, onServiceStateChanged, update call button, new state = "+state.getState());
                    mHandler.removeMessages(UPDATE_CALLBUTTON);
                    Message msg = Message.obtain(mHandler,UPDATE_CALLBUTTON);
                    mHandler.sendMessageDelayed(msg, ContactsUtils.UPDATE_VIEWS_DELAY);
                }
            }
            mSecondServiceState = state;
        }

    };

    private boolean mWasEmptyBeforeTextChange;

    /**
     * This field is set to true while processing an incoming DIAL intent, in order to make sure
     * that SpecialCharSequenceMgr actions can be triggered by user input but *not* by a
     * tel: URI passed by some other app. It will be cleared once the user manually interected
     * with the dialer.
     */
    private boolean mDigitsFilledByIntent;
    private static final String PREF_DIGITS_FILLED_BY_INTENT = "pref_digits_filled_by_intent";

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        mWasEmptyBeforeTextChange = TextUtils.isEmpty(s);
    }

    public void onTextChanged(CharSequence input, int start, int before, int changeCount) {
        if (mWasEmptyBeforeTextChange != TextUtils.isEmpty(input)) {
            final Activity activity = getActivity();
            if (activity != null) {
            	//add by txbv34 for IKCBSMMCPPRC-1358
            	if (!this.isIpCalling){
            		activity.invalidateOptionsMenu();
            	}else{
            		this.isIpCalling=false;
            	}
            }
        }
        // DTMF Tones do not need to be played here any longer -
        // the DTMF dialer handles that functionality now.
    }

    public void afterTextChanged(Editable input) {
        if (!mDigitsFilledByIntent &&
                SpecialCharSequenceMgr.handleChars(getActivity(), input.toString(), mDigits)) {
            // A special sequence was entered, clear the digits
            mDigits.getText().clear();
        }

        if (isDigitsEmpty()) {
            mDigits.setCursorVisible(false);
        }

        //MOT Dialer Start
        if (null != mAdapter) { // MOT Calling Code - IKMAIN-12267
            smartDialerInputChange(input);
        }
        updateAdditionalBtnState();
        mIsNumberBeenDialed = false; //MOT Calling Code - IKSTABLETWOV-1777
        //MOT Dialer End
    }

    @Override
    public void onCreate(Bundle state) {
        if (VDBG) {
            log("enter onCreate");
        }
        super.onCreate(state);
        isDualMode = PhoneModeManager.isDmds();
        mCurrentCountryIso = ContactsUtils.getCurrentCountryIso(getActivity());

//MOT Dialer Start -- Comment out mHaptic
//        try {
//            mHaptic.init(getActivity(),
//                         getResources().getBoolean(R.bool.config_enable_dialer_key_vibration));
//        } catch (Resources.NotFoundException nfe) {
//             Log.e(TAG, "Vibrate control bool missing.", nfe);
//        }
//MOT Dialer End

	 // Wrap our context to inflate list items using correct theme
	 mDialogContext = new ContextThemeWrapper(getActivity(),
			                          getResources().getBoolean(R.bool.contacts_dark_ui)
			                          ? com.android.internal.R.style.Theme_Holo_Dialog_Alert
			                          : com.android.internal.R.style.Theme_Holo_Light_Dialog_Alert
			                         );

        setHasOptionsMenu(true);

        mProhibitedPhoneNumberRegexp = getResources().getString(
                R.string.config_prohibited_phone_number_regexp);

        //MOT Dialer Start
        if (state != null) {
            mIsNumberBeenDialed = state.getBoolean("mIsNumberBeenDialed", false); //MOT Calling Code - IKSTABLETWOV-1777
            mSpeedDialAssignKey = state.getInt(SPEED_DIAL_ASSIGN_KEY, -1); // MOTO Dialer Code - IKHSS6-626
            mDigitsFilledByIntent = state.getBoolean(PREF_DIGITS_FILLED_BY_INTENT);
        }
        Resources r = getResources();
        mMessageBtnFtrFlag = r.getBoolean(R.bool.ftr_35413_use_message_button); //MOT FID 35413-DialerLocalization IKCBS-2014
        mQwertySupported = r.getBoolean(R.bool.smart_dialer_qwerty_support);
        mAdCDMAFeatureOn = r.getBoolean(R.bool.ftr_28651_assisted_dialing);
        mExcludeVoicemail = r.getBoolean(R.bool.ftr_36927_exclude_voicemail);
        //MOT Dialer End

        // ChinaDev
        mMultiPartyFeatureOn = getResources().getBoolean(R.bool.ftr_33270_multiparty_conference);
        Configuration config = getResources().getConfiguration();
        int orient = config.orientation ;
        if(orient == Configuration.ORIENTATION_LANDSCAPE){
            isPortrait = false;
        }else if(orient == Configuration.ORIENTATION_PORTRAIT){
            isPortrait = true;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        int layoutId = R.layout.dialpad_fragment;
        Activity activity = getActivity();
        if (activity != null && activity.getLocalClassName().equals("activities.DialpadActivity")) {
            boolean isChinaCarMode = ((DialpadActivity)activity).isChinaCarMode();
            if (isChinaCarMode && !isPortrait) {
                layoutId = R.layout.dialpad_fragment_car;
            }
        }
        View fragmentView = inflater.inflate(layoutId, container, false);

        //Retrieve list state. This will be applied after the QueryHandler has run
        if(null!=savedState){
            mListState = savedState.getParcelable(LIST_STATE_KEY);
            mListHasFocus = savedState.getBoolean(FOCUS_KEY);
        }

        // Load up the resources for the text field.
        Resources r = getResources();

        mDigitsContainer = fragmentView.findViewById(R.id.digits_container);
        mDigits = (EditText) fragmentView.findViewById(R.id.digits);
        //MOT Dialer Start
        if (mQwertySupported) {
            mDigits.setFilters(new InputFilter[] { new SmartDialerUtil.AlphaDialableInputFilter(), 
                                                   new InputFilter.LengthFilter(Constants.CONTACTS_FIELD_MAX_LENGTH) });
            mDigits.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        } else {
            mDigits.setKeyListener(DialerKeyListener.getInstance());
            mDigits.setFilters(new InputFilter[] {
                        new InputFilter.LengthFilter(Constants.CONTACTS_FIELD_MAX_LENGTH)});
        }
        //MOT Dialer End
        mDigits.setOnClickListener(this);
        mDigits.setOnKeyListener(this);
        mDigits.setOnLongClickListener(this);
        mDigits.addTextChangedListener(this);
        // refer to IKCNDEVICS-6229
        //maybeAddNumberFormatting();//ChinaDev

        mDelete = fragmentView.findViewById(R.id.deleteButton);
        mDelete.setOnClickListener(this);
        mDelete.setOnLongClickListener(this);

        /*
         * MOTO MOD BEGIN JRB768 IKCBS-1015 Check the Hyphenation feature flag,
         * if flag is enabled We don't call formatNumber, else we call
         * formatting editied number
         */
        boolean hyphenStatus = false;
        /* to-pass-build, Xinyu Liu/dcjf34 
        boolean hyphenStatus = MotorolaSettings.getInt(
                    getActivity().getContentResolver(),
                    "hyphenation_feature_enabled", 0) != 1 ? false : true;
        */
        //Begin Motorola vnxm46 02/07/2012 IKSPYLA-1849 Hyphenation not working correctly
        String language = Locale.getDefault().getDisplayLanguage();
        if (!hyphenStatus && !getActivity().getString(R.string.portuguese).equals(language)) {
        //End IKSPYLA-1849
            // refer to IKCNDEVICS-6229
            //PhoneNumberFormatter.setPhoneNumberFormattingTextWatcher(getActivity(), mDigits);
        }

//Google 4.0.4 Start
//        DisplayMetrics dm = getResources().getDisplayMetrics();
//        int minCellSize = (int) (56 * dm.density); // 56dip == minimum size of menu buttons
//        int cellCount = dm.widthPixels / minCellSize;
//        int fakeMenuItemWidth = dm.widthPixels / cellCount;
//          if (DEBUG) Log.d(TAG, "The size of fake menu buttons (in pixel): " + fakeMenuItemWidth);
//Google 4.0.4 End

        // Soft menu button should appear only when there's no hardware menu button.
        mMenuButton = fragmentView.findViewById(R.id.overflow_menu);
        final View divider = fragmentView.findViewById(R.id.menu_callButton_divider);
        //ChinaDev final View dividerView = fragmentView.findViewById(R.id.dialpad_divider);
        if (mMenuButton != null) {
            //mMenuButton.setMinimumWidth(fakeMenuItemWidth);//Google 4.0.4
            if (ViewConfiguration.get(getActivity()).hasPermanentMenuKey()) {
                // This is required for dialpad button's layout, so must use GONE here.
                mMenuButton.setVisibility(View.GONE);//MOT Dialer Code
                divider.setVisibility(View.GONE);
                //ChinaDev if (dividerView != null) dividerView.setVisibility(View.GONE);//MOT Dialer Code
            } else {
                mMenuButton.setOnClickListener(this);
                //MOT Dialer Code Start
                mMenuButton.setVisibility(View.VISIBLE);
                //ChinaDev if (dividerView != null) dividerView.setVisibility(View.VISIBLE);
                //MOT Dialer Code End
                divider.setVisibility(View.VISIBLE);
            }
        }
//Google 4.0.4 Start
//        mSearchButton = fragmentView.findViewById(R.id.searchButton);
//        if (mSearchButton != null) {
//            mSearchButton.setMinimumWidth(fakeMenuItemWidth);
//            mSearchButton.setOnClickListener(this);
//        }
//Google 4.0.4 End

        // Check for the presence of the keypad
        View oneButton = fragmentView.findViewById(R.id.one);
        if (oneButton != null) {
            setupKeypad(fragmentView);
        }
        mAdditionalButtonsRow = fragmentView.findViewById(R.id.dialpadAdditionalButtons);

        // Check whether we should show the onscreen "Dial" button.
        mDialButton = mAdditionalButtonsRow.findViewById(R.id.dialButton);

        //mDialButtonContainer = fragmentView.findViewById(R.id.dialButtonContainer);//Google 4.0.4
        if (r.getBoolean(R.bool.config_show_onscreen_dial_button)) {
            mDialButton.setOnClickListener(this);
        } else {
            mDialButton.setVisibility(View.GONE); // It's VISIBLE by default
            mDialButton = null;
        }

//MOTO Dialer Comment Start
//        mDialpad = fragmentView.findViewById(R.id.dialpad);  // This is null in landscape mode.
//
//        // In landscape we put the keyboard in phone mode.
//       if (null == mDialpad) {
//            mDigits.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
//        } else {
//            mDigits.setCursorVisible(false);
//        }
//MOTO Dialer End

        // Set up the "dialpad chooser" UI; see showDialpadChooser().
        mDialpadChooser = (ListView) fragmentView.findViewById(R.id.dialpadChooser);
        mDialpadChooser.setOnItemClickListener(this);
        mDialpadChooserAdapter = null;//ChinaDev
        initChinaViews(fragmentView);//ChinaDev
        initSmartDialer(fragmentView);
        //MOT Dialer start
        mDigits.setCursorVisible(false);

        /*ChinaDev mLeftButton = (ImageView)mAdditionalButtonsRow.findViewById(R.id.leftButton);
        if (mLeftButton != null) {
            mLeftButton.setOnClickListener(this);
        }*/
        // Check whether we should show the onscreen "Dial" button.
        mDialButton = (ImageView) mAdditionalButtonsRow.findViewById(R.id.dialButton);
        mRightButton = (ImageView) mAdditionalButtonsRow.findViewById(R.id.rightButton);
        if (mRightButton != null) {
            mRightButton.setOnClickListener(this);
        }
        setDialerButton();
        configureDialerButton();
        //MOT Dialer End

        // BEGIN, Motorola, w21071, 16-Apr-2012, IKHSS6UPGR-7284
        // Just moved from the above of MOT Dialer code to here since FC occurs this function is
        // called before the dialer buttons are not fully initialized.
        configureScreenFromIntent(getActivity().getIntent());
        // END IKHSS6UPGR-7284

        //updateFakeMenuButtonsVisibility(mShowOptionsMenu); //Google 4.0.4

        return fragmentView;
    }

    private boolean isLayoutReady() {
        return mDigits != null;
    }

    public EditText getDigitsWidget() {
        return mDigits;
    }

    /**
     * @return true when {@link #mDigits} is actually filled by the Intent.
     */
    private boolean fillDigitsIfNecessary(Intent intent) {
        mIntentWithData = false; // MOT Dialer Code
        final String action = intent.getAction();
        if (Intent.ACTION_DIAL.equals(action) || Intent.ACTION_VIEW.equals(action)) {
        	mIsAddCallMode = isAddCallMode(intent);
        	Uri uri = intent.getData();
            if (uri != null) {
                if ("tel".equals(uri.getScheme())) {
                    // Put the requested number into the input area
                    String data = uri.getSchemeSpecificPart();
                    // Remember it is filled via Intent.
                    mDigitsFilledByIntent = true;
                    setFormattedDigits(data, null);
                    mIntentWithData = true; // MOT Dialer Code
                    return true;
                } else {
                    String type = intent.getType();
                    if (People.CONTENT_ITEM_TYPE.equals(type)
                            || Phones.CONTENT_ITEM_TYPE.equals(type)) {
                        // Query the phone number
                        Cursor c = getActivity().getContentResolver().query(intent.getData(),
                                new String[] {PhonesColumns.NUMBER, PhonesColumns.NUMBER_KEY},
                                null, null, null);
                        if (c != null) {
                            try {
                                if (c.moveToFirst()) {
                                    // Remember it is filled via Intent.
                                    mDigitsFilledByIntent = true;
                                    // Put the number into the input area
                                    setFormattedDigits(c.getString(0), c.getString(1));
                                    mIntentWithData = true; // MOT Dialer Code
                                    return true;
                                }
                            } finally {
                                c.close();
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

//MOT Dialer Start-- Comment out : dialpad chooser
    /**
     * @see #showDialpadChooser(boolean)
     */
    private static boolean needToShowDialpadChooser(Intent intent, boolean isAddCallMode) {
        final String action = intent.getAction();

        boolean needToShowDialpadChooser = false;

        if (Intent.ACTION_DIAL.equals(action) || Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            if (uri == null) {
                // ACTION_DIAL or ACTION_VIEW with no data.
                // This behaves basically like ACTION_MAIN: If there's
                // already an active call, bring up an intermediate UI to
                // make the user confirm what they really want to do.
                // Be sure *not* to show the dialpad chooser if this is an
                // explicit "Add call" action, though.
                if (!isAddCallMode && phoneIsInUse()) {
                    needToShowDialpadChooser = true;
                }
            }
       } else if (Intent.ACTION_MAIN.equals(action)) {
            // The MAIN action means we're bringing up a blank dialer
            // (e.g. by selecting the Home shortcut, or tabbing over from
            // Contacts or Call log.)
            //
            // At this point, IF there's already an active call, there's a
            // good chance that the user got here accidentally (but really
            // wanted the in-call dialpad instead).  So we bring up an
            // intermediate UI to make the user confirm what they really
            // want to do.
            if (phoneIsInUse()) {
                // Log.i(TAG, "resolveIntent(): phone is in use; showing dialpad chooser!");
                needToShowDialpadChooser = true;
            }
        }

        return needToShowDialpadChooser;
    }
// MOT Dialer End

    public static boolean isAddCallMode(Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_DIAL.equals(action) || Intent.ACTION_VIEW.equals(action)) {
            // see if we are "adding a call" from the InCallScreen; false by default.
            return intent.getBooleanExtra(ADD_CALL_MODE_KEY, false);
        } else {
            return false;
        }
    }

    /**
     * Checks the given Intent and changes dialpad's UI state. For example, if the Intent requires
     * the screen to enter "Add Call" mode, this method will show correct UI for the mode.
     */
    public void configureScreenFromIntent(Intent intent) {
        if (!isLayoutReady()) {
            // This happens typically when parent's Activity#onNewIntent() is called while
            // Fragment#onCreateView() isn't called yet, and thus we cannot configure Views at
            // this point. onViewCreate() should call this method after preparing layouts, so
            // just ignore this call now.
            Log.i(TAG,
                    "Screen configuration is requested before onCreateView() is called. Ignored");
            return;
        }
        boolean needToShowDialpadChooser = false; // MOT Dialer Comment out

        //ChinaDev
        // by default we are not adding a call.
        mIsAddCallMode = isAddCallMode(intent);
        if (!mIsAddCallMode) {
            if (isInVTcall()) {
                needToShowDialpadChooser = true;
            }
            final boolean digitsFilled = fillDigitsIfNecessary(intent);
            if (!digitsFilled) {
                needToShowDialpadChooser = needToShowDialpadChooser(intent, mIsAddCallMode);
            }
        }
        showDialpadChooser(needToShowDialpadChooser);
    }

    /*ChinaDev private*/ void setFormattedDigits(String data, String normalizedNumber) {
        // strip the non-dialable numbers out of the data string.
        String dialString = PhoneNumberUtils.extractNetworkPortion(data);
        /* MOTO MOD BEGIN IKHSS7-2038
           * Changed calling PhoneNumberUtils.formatNumber API to New API
           * PhoneNumberUtilsExt.formatNumber for Hyphensation Feature 35615
        */

        dialString =
                PhoneNumberUtilsExt.formatNumber(getActivity(), dialString, normalizedNumber, mCurrentCountryIso);
      //MOTO END IKHSS7-2038

        if (!TextUtils.isEmpty(dialString)) {
            Editable digits = mDigits.getText();
            digits.replace(0, digits.length(), dialString);
            // for some reason this isn't getting called in the digits.replace call above..
            // but in any case, this will make sure the background drawable looks right
            afterTextChanged(digits);
        }
        highlightCallButton(dialString);//ChinaDev
    }

    //MOT Dialer Start - IKHSS7-4271
    private void setupKeypad(View fragmentView) {
        int[][] keydata =  {
             {R.id.one,      1,  R.drawable.dial_num_1_wht_skt},
             {R.id.two,      1,  R.drawable.dial_num_2_wht_skt},
             {R.id.three,    1,  R.drawable.dial_num_3_wht_skt},

             {R.id.four,     1,  R.drawable.dial_num_4_wht_skt},
             {R.id.five,     1,  R.drawable.dial_num_5_wht_skt},
             {R.id.six,      1,  R.drawable.dial_num_6_wht_skt},

             {R.id.seven,    1,  R.drawable.dial_num_7_wht_skt},
             {R.id.eight,    1,  R.drawable.dial_num_8_wht_skt},
             {R.id.nine,     1,  R.drawable.dial_num_9_wht_skt},

             {R.id.star,     1,  R.drawable.dial_num_star_wht_skt},
             {R.id.zero,     1,  R.drawable.dial_num_0_wht_skt},
             {R.id.pound,    1,  R.drawable.dial_num_pound_wht_skt},
        };

        for(int[] r: keydata) {
            View keyView = fragmentView.findViewById(r[0]);
            keyView.setOnClickListener(this);

            if(r[1] == 1) {
                keyView.setOnLongClickListener(this);
            }

            if(mMessageBtnFtrFlag) {
                ((ImageView) keyView).setImageResource(r[2]);
            }
        }
    }
    //MOT Dialer End - IKHSS7-4271

    @Override
    public void onResume() {
        if (VDBG) {
            log("enter onResume");
        }
        super.onResume();
        if (mLocationServiceManager != null) {
            mLocationServiceManager.checkService(true);
        }
        // Query the last dialed number. Do it first because hitting
        // the DB is 'slow'. This call is asynchronous.
        queryLastOutgoingCall();
        
        //ChinaDev default set them to invisible
        if (mCallLocation != null) {
            mCallLocation.setVisibility(View.GONE);
        }
        if (mSearchMatchesTv != null) {
            mSearchMatchesTv.setVisibility(View.GONE);
        }

        // retrieve the DTMF tone play back setting.
        mDTMFToneEnabled = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;

        // Retrieve the haptic feedback setting.
        //mHaptic.checkSystemSetting();//MOT Dialer comment out

        // if the mToneGenerator creation fails, just continue without it.  It is
        // a local audio signal, and is not as important as the dtmf tone itself.
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                try {
                    // we want the user to be able to control the volume of the dial tones
                    // outside of a call, so we use the stream type that is also mapped to the
                    // volume control keys for this activity
                    mToneGenerator = new ToneGenerator(DIAL_TONE_STREAM_TYPE, TONE_RELATIVE_VOLUME);
                    //getActivity().setVolumeControlStream(DIAL_TONE_STREAM_TYPE); //MOT Dialer Code - IKHSS7-838
                } catch (RuntimeException e) {
                    Log.w(TAG, "Exception caught while creating local tone generator: " + e);
                    mToneGenerator = null;
                }
            }
        }

        updateCallButton();//ChinaDev
        updatePhoneNumberFormat();//ChinaDev

        Activity parent = getActivity();
        if (parent instanceof DialtactsActivity) {
            // See if we were invoked with a DIAL intent. If we were, fill in the appropriate
            // digits in the dialer field.
            // Mot Calling Code - IKSTABLEFOURV-9312
            Intent intent = parent.getIntent();
            Uri dialUri = intent.getData();
            if ((dialUri != null) && (!intent.hasExtra("isDialUriUsed"))) {
                intent.putExtra("isDialUriUsed", true);
            //End Mot Calling Code - IKSTABLEFOURV-9312
                fillDigitsIfNecessary(parent.getIntent());
            }
        }
        
        // While we're in the foreground, listen for phone state changes,
        // purely so that we can take down the "dialpad chooser" if the
        // phone becomes idle while the chooser UI is visible.
        TelephonyManager telephonyManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE | PhoneStateListener.LISTEN_SERVICE_STATE);
        mDefaultPhoneType = telephonyManager.getPhoneType();
        isDefaultRoaming = telephonyManager.isNetworkRoaming();
        if (isDualMode) {
            /* to-pass-build, Xinyu Liu/dcjf34 */ 
            TelephonyManager secondMgr = null;
                   // (TelephonyManager) getActivity().getSystemService(Context.SECONDARY_TELEPHONY_SERVICE);
            if (secondMgr != null) {
                secondMgr.listen(mSecondPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE | PhoneStateListener.LISTEN_SERVICE_STATE);
                mSecondaryPhoneType = secondMgr.getPhoneType();
                isSecondaryRoaming = secondMgr.isNetworkRoaming();
            }
        }

        // Potentially show hint text in the mDigits field when the user
        // hasn't typed any digits yet.  (If there's already an active call,
        // this hint text will remind the user that he's about to add a new
        // call.)
        //
        // TODO: consider adding better UI for the case where *both* lines
        // are currently in use.  (Right now we let the user try to add
        // another call, but that call is guaranteed to fail.  Perhaps the
        // entire dialer UI should be disabled instead.)
        if (phoneIsInUse()) {
            mDigits.setHint(R.string.dialerDialpadHintText);
        } else {
            // Common case; no hint necessary.
            mDigits.setHint(null);
//MOT Dialer Start Comment out
            // Also, a sanity-check: the "dialpad chooser" UI should NEVER
            // be visible if the phone is idle!
            showDialpadChooser(false);
//MOT Dialer End
        }

        //MOT Dialer Start
        //MOT Calling Code - IKSTABLETWOV-1777
        if (mIsNumberBeenDialed) {
            mDigits.getText().clear();
            mIsNumberBeenDialed = false;
        }
        //END - MOT Calling Code - IKSTABLETWOV-1777
        //MOT calling code- IKMAIN-4172 IKHSS7-830 IKHSS7-8639
        if (fragmentVisibilty) {
            mAdapter.resumeQuery();
        }
        //END - MOT calling code- IKMAIN-4172 IKHSS7-830
        mIsAdFtrOn = isAdFtrOn(); // Motorola IKOLYMP-5641
        // Shouldn't be able to access regular dialer while in battery cooldown mode; only
        // emergency calls are allowed
        boolean isInCooldown = Settings.System.getInt(parent.getContentResolver(),
                "com.android.phone.PhoneAp.mIsInBatteryCooldown", 0) == 1;
        if (isInCooldown) {
            if(CDBG) log("In Battery Cooldown mode, finish...");
            getActivity().finish();
            return;
        }
        mIsCallBeenPlaced = false; // MOT Calling code - IKSTABLETWO-6716
        updateAdditionalBtnState();
        //MOT Dialer End
        /*Added for switchuitwo-350 begin*/
        if(mNeedClosedDrawer && (mDrawer != null)) {
        	mDrawer.close();
        	if (null != mDelete)
                mDelete.setVisibility(View.GONE);
            if (null != unhideDrawerBtn)
                unhideDrawerBtn.setVisibility(View.VISIBLE);
        }
        /*Added for switchuitwo-350 end*/
    }

    //MOT Calling Code - IKSTABLETWOV-1777
    @Override
    public void onSaveInstanceState(Bundle icicle) {
        // 2012.10.25 jrw647 modified to fix SWITCHUITWOV-286
        // super.onSaveInstanceState(icicle);
        icicle.putBoolean("mIsNumberBeenDialed", mIsNumberBeenDialed);
        icicle.putBoolean(PREF_DIGITS_FILLED_BY_INTENT, mDigitsFilledByIntent);
        /*Added for switchuitwo-529 begin*/
        icicle.putInt(SPEED_DIAL_ASSIGN_KEY, mSpeedDialAssignKey);
        /*Added for switchuitwo-529 end*/
        //ChinaDev Save list state in the bundle so we can restore it after the QueryHandler has run
        if (mList != null) {
            icicle.putParcelable(LIST_STATE_KEY, mList.onSaveInstanceState());
            icicle.putBoolean(FOCUS_KEY, mList.hasFocus());
        }
    }
    //END - MOT Calling Code - IKSTABLETWOV-1777

    @Override
    public void onPause() {
        if (VDBG) {
            log("enter onPause");
        }

        super.onPause();
        /*Added for switchuitwo-350 begin*/
        if(mDrawer != null) {
        	mNeedClosedDrawer = !mDrawer.isOpened();
        }
        /*Added for switchuitwo-350 end*/
        // Stop listening for phone state changes.
        TelephonyManager telephonyManager =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);

        if (isDualMode) {
            /* to-pass-build, Xinyu Liu/dcjf34 */
            TelephonyManager secondMgr = null;
                   // (TelephonyManager) getActivity().getSystemService(Context.SECONDARY_TELEPHONY_SERVICE);
            if (secondMgr != null) {
                secondMgr.listen(mSecondPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
        }

        synchronized (mToneGeneratorLock) {
            if (mToneGenerator != null) {
                mToneGenerator.release();
                mToneGenerator = null;
            }
        }
        // TODO: I wonder if we should not check if the AsyncTask that
        // lookup the last dialed number has completed.
        mLastNumberDialed = EMPTY_NUMBER;  // Since we are going to query again, free stale number.
        mVoiceMailNumber = null; // MOT Calling code - IKSTABLEFOURV-1933

        //MOT Dialer Code Start - IKHSS7-7634
        if (mPopupMenu != null) {
            mPopupMenu.dismiss();
        }
        //MOT Dialer Code End - IKHSS7-7634
        //MOT Dialer code Start IKHSS7-8639
        if(fragmentVisibilty) {
            mAdapter.pauseCleanUp();
        }
        //MOT Dialer code End IKHSS7-8639
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mLocationServiceManager != null) {
            mLocationServiceManager.unbind();
        }
    }

    @Override
    public void onDestroy() {
        if(VDBG) log("onDestroy");
        super.onDestroy();
        //ChinaDev
        if (mLocationServiceManager != null) {
            mLocationServiceManager = null;
        }
        synchronized(mToneGeneratorLock) {
            if (mToneGenerator != null) {
                mToneGenerator.release();
                mToneGenerator = null;
            }
        }
    }

    //MOT Dialer start
    @Override
    public void onDestroyView() {
        if (VDBG) {
            log("enter onDestroyView");
        }
        super.onDestroyView();
        mAdapter.destroyCleanUp();
        mAdapter = null; //MOT Calling Code - IKMAIN-11005
    }
    //MOT Dialer End

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (mShowOptionsMenu && ViewConfiguration.get(getActivity()).hasPermanentMenuKey() &&
                isLayoutReady()) {
            inflater.inflate(R.menu.dialpad_options, menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Hardware menu key should be available and Views should already be ready.
        if (mShowOptionsMenu && ViewConfiguration.get(getActivity()).hasPermanentMenuKey() &&
                isLayoutReady()) {
             setupMenuItems(menu);
        }
    }

    //Add by txbv34
    @Override
	public void onOptionsMenuClosed(Menu menu) {
		// TODO Auto-generated method stub
		super.onOptionsMenuClosed(menu);
		if(DEBUG)Log.d(TAG,"onOptionsMenuClosed");
	}//End add 

	private void setupMenuItems(Menu menu) {
    	if(DEBUG)Log.d(TAG,"setupMenuItems 111");
        final MenuItem callSettingsMenuItem = menu.findItem(R.id.menu_call_settings_dialpad);
        final MenuItem gsmIpCallMenuItem = menu.findItem(R.id.menu_gsm_ip_call);
        final MenuItem cdmaIpCallMenuItem = menu.findItem(R.id.menu_cdma_ip_call);
        final MenuItem gsmIntlRoamingCallMenuItem = menu.findItem(R.id.menu_gsm_intl_roaming_call);
        final MenuItem cdmaIntlRoamingCallMenuItem = menu.findItem(R.id.menu_cdma_intl_roaming_call);
        final MenuItem intlRoamingCallBackMenuItem = menu.findItem(R.id.menu_intl_roaming_call_back);
        final MenuItem twoSecPauseMenuItem = menu.findItem(R.id.menu_2s_pause);
        final MenuItem waitMenuItem = menu.findItem(R.id.menu_add_wait);
        final MenuItem addToBlackListMenuItem = menu.findItem(R.id.menu_add_to_blacklist);
        final MenuItem addToWhiteListMenuItem = menu.findItem(R.id.menu_add_to_whitelist);

        // Check if all the menu items are inflated correctly. As a shortcut, we assume all menu
        // items are ready if the first item is non-null.
        if (callSettingsMenuItem == null) {
            return;
        }
        // Mot Dialer start
        final MenuItem addContactsMenuItem = menu.findItem(R.id.menu_add_contacts);//China Dialer add
        final MenuItem sendSmsMenuItem = menu.findItem(R.id.menu_send_sms);
        final MenuItem searchMenuItem = menu.findItem(R.id.menu_search); //MOT Dialer Code - IKHSS6-4792
        // MOT Dialer end

        final Activity activity = getActivity();
        if (activity != null && ViewConfiguration.get(activity).hasPermanentMenuKey()) {
            // Call settings should be available via its parent Activity.
            callSettingsMenuItem.setVisible(false);
        } else {
            callSettingsMenuItem.setVisible(true);
            callSettingsMenuItem.setIntent(DialtactsActivity.getCallSettingsIntent());
        }

        SpeedDialHelper.setupOptionMenu_SpdSetup(this, menu);

        // We show "add to contacts", "2sec pause", and "add wait" menus only when the user is
        // seeing usual dialpads and has typed at least one digit.
        if (isDigitsEmpty()) {
            //addToContactMenuItem.setVisible(false);//MOT Dialer Code - IKHSS6-4792
            gsmIpCallMenuItem.setVisible(false);
            cdmaIpCallMenuItem.setVisible(false);
            gsmIntlRoamingCallMenuItem.setVisible(false);
            cdmaIntlRoamingCallMenuItem.setVisible(false);
            intlRoamingCallBackMenuItem.setVisible(false);
            twoSecPauseMenuItem.setVisible(false);
            waitMenuItem.setVisible(false);
            addContactsMenuItem.setVisible(false);//China Dialer add
            sendSmsMenuItem.setVisible(false);//Mot Dialer add
            addToBlackListMenuItem.setVisible(false);
            addToWhiteListMenuItem.setVisible(false);
        } else {
            final CharSequence digits = mDigits.getText();

            // Put the current digits string into an intent
            //MOT Dialer code start - IKHSS6-4792
            //addToContactMenuItem.setIntent(getAddToContactIntent(digits));
            TelephonyManager telephonyManager = TelephonyManager.getDefault();
            TelephonyManager defaultMgr = TelephonyManager.getDefault();
            TelephonyManager secondaryMgr = null;
            boolean isRoaming = telephonyManager.isNetworkRoaming();
            boolean isCDMARoaming = false;
            boolean isGSMRoaming = false;
            boolean isCDMAEnabled = ContactsUtils.isPhoneEnabled(PHONE_TYPE_CDMA);
            boolean isGSMEnabled = ContactsUtils.isPhoneEnabled(PHONE_TYPE_GSM);
            boolean isRoamingCallSupported = (!SystemProperties.get("ro.product.locale.region","CN").equalsIgnoreCase("TW")) && PhoneModeManager.IsRoamingCallSupported();
            boolean isRoamingCallBackSupported = PhoneModeManager.IsRoamingCallBackSupported();
            if (isDefaultRoaming) {
                if (mDefaultPhoneType == PHONE_TYPE_CDMA) {
                    isCDMARoaming = true;
                } else if (mDefaultPhoneType == PHONE_TYPE_GSM) {
                    isGSMRoaming = true;
                }
            }
            if (isSecondaryRoaming) {
                if (mSecondaryPhoneType == PHONE_TYPE_CDMA) {
                    isCDMARoaming = true;
                } else if (mSecondaryPhoneType == PHONE_TYPE_GSM) {
                    isGSMRoaming = true;
                }
            }
            if (cdmaIpCallMenuItem != null && isCDMAEnabled && !isCDMARoaming) {
                if (!isDualMode) {
                    cdmaIpCallMenuItem.setTitle(R.string.ip_call_single);
                }
                cdmaIpCallMenuItem.setVisible(true);
            } else if (cdmaIpCallMenuItem != null && (!isCDMAEnabled || isCDMARoaming)) {
                cdmaIpCallMenuItem.setVisible(false);
            }
            if (gsmIpCallMenuItem != null && isGSMEnabled && !isGSMRoaming) {
                if (!isDualMode) {
                    gsmIpCallMenuItem.setTitle(R.string.ip_call_single);
                }
                gsmIpCallMenuItem.setVisible(true);
            } else if (gsmIpCallMenuItem != null && (!isGSMEnabled || isGSMRoaming)) {
                gsmIpCallMenuItem.setVisible(false);
            }
            if (cdmaIntlRoamingCallMenuItem != null && isCDMAEnabled && isCDMARoaming) {
                if (!isDualMode) {
                    cdmaIntlRoamingCallMenuItem.setTitle(R.string.intl_roaming_call);
                }
                if (isRoamingCallSupported) {
                    cdmaIntlRoamingCallMenuItem.setVisible(true);
                } else {
                    cdmaIntlRoamingCallMenuItem.setVisible(false);
                }
            } else if (cdmaIntlRoamingCallMenuItem != null && (!isCDMAEnabled || !isCDMARoaming)) {
                cdmaIntlRoamingCallMenuItem.setVisible(false);
            }
            if (gsmIntlRoamingCallMenuItem != null && isGSMEnabled && isGSMRoaming) {
                if (!isDualMode) {
                    gsmIntlRoamingCallMenuItem.setTitle(R.string.intl_roaming_call);
                }
                if (isRoamingCallSupported) {
                    gsmIntlRoamingCallMenuItem.setVisible(true);
                } else {
                    gsmIntlRoamingCallMenuItem.setVisible(false);
                }
            } else if (gsmIntlRoamingCallMenuItem != null && (!isGSMEnabled || !isGSMRoaming)) {
                gsmIntlRoamingCallMenuItem.setVisible(false);
            }
            if (intlRoamingCallBackMenuItem != null && isGSMEnabled && isGSMRoaming && isRoamingCallBackSupported) {
                intlRoamingCallBackMenuItem.setVisible(true);
            } else if (intlRoamingCallBackMenuItem != null && (!isGSMEnabled || !isGSMRoaming || !isRoamingCallBackSupported)) {
                intlRoamingCallBackMenuItem.setVisible(false);
            }

            addToBlackListMenuItem.setVisible(true);
            addToWhiteListMenuItem.setVisible(true);

            // Check out whether to show Pause & Wait option menu items
            int selectionStart;
            int selectionEnd;
            String strDigits = digits.toString();

            selectionStart = mDigits.getSelectionStart();
            selectionEnd = mDigits.getSelectionEnd();

            if (selectionStart != -1) {
                if (selectionStart > selectionEnd) {
                    // swap it as we want start to be less then end
                    int tmp = selectionStart;
                    selectionStart = selectionEnd;
                    selectionEnd = tmp;
                }

                if (selectionStart != 0) {
                    // Pause can be visible if cursor is not in the begining
                    twoSecPauseMenuItem.setVisible(true);

                    // For Wait to be visible set of condition to meet
                    waitMenuItem.setVisible(showWait(selectionStart, selectionEnd, strDigits));
                } else {
                    // cursor in the beginning both pause and wait to be invisible
                    twoSecPauseMenuItem.setVisible(false);
                    waitMenuItem.setVisible(false);
                }
            } else {
                twoSecPauseMenuItem.setVisible(true);

                // cursor is not selected so assume new digit is added to the end
                int strLength = strDigits.length();
                waitMenuItem.setVisible(showWait(strLength, strLength, strDigits));
            }
            addContactsMenuItem.setVisible(true);//China Dialer Code
            sendSmsMenuItem.setVisible(true);//MOT Dialer Code
        }
    }

    // MOT Dialer Code Start - IKPIM-1133 IKHSS6-13862
    private Intent getAddToContactIntent() {
        String digits = PhoneNumberUtils.convertKeypadLettersToDigits(mDigits.getText().toString());
        final Intent intent;
        if(digits.isEmpty()) {
            intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
        } else {
            intent = new Intent(ADD_CONTACT_DIALOG);
            intent.putExtra(Insert.PHONE, digits);
        }
        return intent;
    }
    // MOT Dialer Code End - IKPIM-1133 IKHSS6-13862

    private Intent getAddToBlackListIntent() {
        String number = PhoneNumberUtils.convertKeypadLettersToDigits(mDigits.getText().toString());
        Intent blackintent = new Intent(Intent.ACTION_INSERT);
        blackintent.setType("vnd.android.cursor.item/vnd.motorola.firewall.name");
        // BEGIN Motorola, ODC_001639, 2013-01-05, SWITCHUITWO-439
        blackintent.putExtra("blocktype", "blacklist");
        blackintent.putExtra("blacklist", number);
        // END SWITCHUITWO-439
        Log.v(TAG, "getAddToBlackList number = "+number);
        return blackintent;
    }

    private Intent getAddToWhiteListIntent() {
        String number = PhoneNumberUtils.convertKeypadLettersToDigits(mDigits.getText().toString());
        Intent whiteintent = new Intent(Intent.ACTION_INSERT);
        whiteintent.setType("vnd.android.cursor.item/vnd.motorola.firewall.name");
        // BEGIN Motorola, ODC_001639, 2013-01-05, SWITCHUITWO-439
        whiteintent.putExtra("blocktype", "whitelist");
        whiteintent.putExtra("whitelist", number);
        // END SWITCHUITWO-439
        Log.v(TAG, "getAddToWhiteList number = "+number);
        return whiteintent;
    }

    private void keyPressed(int keyCode) {
        mDigitsFilledByIntent = false;
 //       mHaptic.vibrate(); //MOT Dialer Comment out
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        mDigits.onKeyDown(keyCode, event);

        // If the cursor is at the end of the text we hide it.
        final int length = mDigits.length();
        if (length == mDigits.getSelectionStart() && length == mDigits.getSelectionEnd()) {
            mDigits.setCursorVisible(false);
        }
        clearCallButtonHighlight();//ChinaDev

        // BEGIN Motorola, bjtx47, 09/15/2010, IKMAIN-4570
        /* to-pass-build, Xinyu Liu/dcjf34 
        if (RdcUtils.isRdcEnabled()) {
            RdcUtils.submitRdcMetric_UI01(getActivity(), KeyEvent.ACTION_DOWN, keyCode, 0);
        }*/
        // END IKMAIN-4570
    }

    public boolean onKey(View view, int keyCode, KeyEvent event) {
        // [ MOTO:Begin C bkdp84 bug 15798 play DTMF tones when dialing with QWERTY Keypad
        if ((view.getId() == R.id.digits) && (event.getAction() == KeyEvent.ACTION_DOWN)) {
            //ChinaDev Eat the long press event so the keyboard doesn't come up.
            if (keyCode == KeyEvent.KEYCODE_MENU && event.isLongPress()) {
                Log.v(TAG, "a23237");
                return true;
            }
            //ChinaDev
            if(keyCode ==  KeyEvent.KEYCODE_1) {
	            long timeDiff = SystemClock.uptimeMillis() - event.getDownTime();
	            if (timeDiff >= ViewConfiguration.getLongPressTimeout()) {
	                // Long press detected, call voice mail
	                callVoicemail();
	            }
	        	/**modify for SWITCHUITWOV-199 by bphx43 2012-09-18*/
//	        	return true;
	        	return false;
	        	/** end by bphx43*/
	        }

        	if(keyCode == KeyEvent.KEYCODE_ENTER) {
                // MOT Calling Code - IKMAIN-18641
                InputMethodManager inputMethodManager = (InputMethodManager)
                    getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(mDigits.getWindowToken(), 0);
                // END MOT Calling Code - IKMAIN-18641
                dialButtonPressed();
                return true;
            }
            if(keyCode == KeyEvent.KEYCODE_CALL) {
                long callPressDiff = SystemClock.uptimeMillis() - event.getDownTime();
                if (callPressDiff >= ViewConfiguration.getLongPressTimeout()) {
                    // Launch voice dialer
                    try {
                        startActivity(getVoiceDialIntent());
                    } catch (ActivityNotFoundException e) {
                        Log.w(TAG, "Failed to launch voice dialer: " + e);
                    }
                }
                return true;
            }
            if((keyCode!=KeyEvent.KEYCODE_MENU) && (keyCode!=KeyEvent.KEYCODE_SEARCH)
                && (keyCode!=KeyEvent.KEYCODE_HOME) && (keyCode!=KeyEvent.KEYCODE_BACK)){
                mAdapter.mAutoExpandList = true;
                MEDialer.setQwertyKeypad(true); // Motorola, FTR 36344, Apr-19-2011, IKPIM-384
            }
            // MOT Calling Code - IKMAIN-18641
            char charPressed;
            if (mQwertySupported) {
                charPressed = event.getMatch(SmartDialerUtil.CHARACTERS.toCharArray(), event.getMetaState());
            } else {
                charPressed = event.getMatch(DTMF_CHARS, event.getMetaState());
            }// END - MOT Calling Code - IKMAIN-18641

            switch(charPressed) {
                case '0':
                    playTone(ToneGenerator.TONE_DTMF_0);
                    break;
                case '1':
                    playTone(ToneGenerator.TONE_DTMF_1);
                    break;
                case '2':
                    playTone(ToneGenerator.TONE_DTMF_2);
                    break;
                case '3':
                    playTone(ToneGenerator.TONE_DTMF_3);
                    break;
                case '4':
                    playTone(ToneGenerator.TONE_DTMF_4);
                    break;
                case '5':
                    playTone(ToneGenerator.TONE_DTMF_5);
                    break;
                case '6':
                    playTone(ToneGenerator.TONE_DTMF_6);
                    break;
                case '7':
                    playTone(ToneGenerator.TONE_DTMF_7);
                    break;
                case '8':
                    playTone(ToneGenerator.TONE_DTMF_8);
                    break;
                case '9':
                    playTone(ToneGenerator.TONE_DTMF_9);
                    break;
                case '*':
                    playTone(ToneGenerator.TONE_DTMF_S);
                    break;
                case '#':
                    playTone(ToneGenerator.TONE_DTMF_P);
                    break;
            }
        }
        if ((view.getId() == R.id.digits) && (event.getAction() == KeyEvent.ACTION_UP)) {
            //As there is no onbackpressed()/onkeyUp() function in fragment, so move the back key
            //logic to onKey()
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK: {
                    //ChinaDev
                    if (mDrawer != null && !mDrawer.isOpened()) {
                    	mDrawer.animateOpen();//MOT Calling code IKPIM-660
                        return true;
                    }
                    mDigits.getText().clear();
                    break;
                }
                case KeyEvent.KEYCODE_CALL: {
                    dialButtonPressed();
                    return true;
                }
                //MOT Dialer Code Start - IKHSS6-7483
                case KeyEvent.KEYCODE_MENU: {
                    View overflowMenuButton = getActivity().findViewById(R.id.overflow_menu);
                    if (overflowMenuButton.getVisibility()== View.VISIBLE) {
                        PopupMenu popup = constructPopupMenu(overflowMenuButton);
                        if (popup != null) {
                            popup.show();
                        }
                    return true;
                    }
                    break;
                }
                //MOT Dialer Code End - IKHSS6-7483
            }
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        mDigitsFilledByIntent = false;
        mAdapter.mAutoExpandList = false; //MOT Calling code - IKPIM-161
        switch (view.getId()) {
            case R.id.one: {
                playTone(ToneGenerator.TONE_DTMF_1);
                keyPressed(KeyEvent.KEYCODE_1);
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                return;
            }
            case R.id.two: {
                playTone(ToneGenerator.TONE_DTMF_2);
                keyPressed(KeyEvent.KEYCODE_2);
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                return;
            }
            case R.id.three: {
                playTone(ToneGenerator.TONE_DTMF_3);
                keyPressed(KeyEvent.KEYCODE_3);
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                return;
            }
            case R.id.four: {
                playTone(ToneGenerator.TONE_DTMF_4);
                keyPressed(KeyEvent.KEYCODE_4);
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                return;
            }
            case R.id.five: {
                playTone(ToneGenerator.TONE_DTMF_5);
                keyPressed(KeyEvent.KEYCODE_5);
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                return;
            }
            case R.id.six: {
                playTone(ToneGenerator.TONE_DTMF_6);
                keyPressed(KeyEvent.KEYCODE_6);
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                return;
            }
            case R.id.seven: {
                playTone(ToneGenerator.TONE_DTMF_7);
                keyPressed(KeyEvent.KEYCODE_7);
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                return;
            }
            case R.id.eight: {
                playTone(ToneGenerator.TONE_DTMF_8);
                keyPressed(KeyEvent.KEYCODE_8);
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                return;
            }
            case R.id.nine: {
                playTone(ToneGenerator.TONE_DTMF_9);
                keyPressed(KeyEvent.KEYCODE_9);
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                return;
            }
            case R.id.zero: {
                playTone(ToneGenerator.TONE_DTMF_0);
                keyPressed(KeyEvent.KEYCODE_0);
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                return;
            }
            case R.id.pound: {
                playTone(ToneGenerator.TONE_DTMF_P);
                keyPressed(KeyEvent.KEYCODE_POUND);
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                return;
            }
            case R.id.star: {
                playTone(ToneGenerator.TONE_DTMF_S);
                keyPressed(KeyEvent.KEYCODE_STAR);
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                return;
            }
            case R.id.deleteButton: {
                keyPressed(KeyEvent.KEYCODE_DEL);
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                return;
            }
            case R.id.dialButton: {
                //mHaptic.vibrate();  // Vibrate here too, just like we do for the regular keys
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                dialButtonPressed();
                return;
            }
//            case R.id.searchButton: {
//                mHaptic.vibrate();
//                if (mListener != null) {
//                    mListener.onSearchButtonPressed();
//                }
//                return;
//            }
            case R.id.digits: {
                if (!isDigitsEmpty()) {
                    mDigits.setCursorVisible(true);
                }
                return;
            }
            case R.id.overflow_menu: {
                PopupMenu popup = constructPopupMenu(view);
                if (popup != null) {
                    popup.show();
                }
                return;
            }
            //MOT Dialer start
/*            case R.id.leftButton: {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                setDialerButtonFunc(mButton_left_id);
                return;
            }*/
            case R.id.rightButton: {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                setDialerButtonFunc(mButton_right_id);
                return;
            }
            //MOT Dialer End
        }
    }

    private PopupMenu constructPopupMenu(View anchorView) {
        final Context context = getActivity();
        if (context == null) {
            return null;
        }
        //MOT Dialer Code Start - IKHSS7-7634
        mPopupMenu = new PopupMenu(context, anchorView);
        final Menu menu = mPopupMenu.getMenu();
        mPopupMenu.inflate(R.menu.dialpad_options);
        mPopupMenu.setOnMenuItemClickListener(this);
        setupMenuItems(menu);
        return mPopupMenu;
        //MOT Dialer Code End - IKHSS7-7634
    }

    public boolean onLongClick(View view) {
        mDigitsFilledByIntent = false;
        final Editable digits = mDigits.getText();
        int id = view.getId();
        switch (id) {
            case R.id.deleteButton: {
                digits.clear();
                clearCallButtonHighlight();
                // TODO: The framework forgets to clear the pressed
                // status of disabled button. Until this is fixed,
                // clear manually the pressed status. b/2133127
                //mDelete.setPressed(false);//MOT Dialer Code
                return true;
            }
            case R.id.one: {
                if (!mExcludeVoicemail) { //MOT FID 36927-Speeddial#1 IKCBS-2013
                    if (isDigitsEmpty()) {
                        //MOTO Dialer Code IKHSS6UPGR-5380 - Start
                        //Comment dialer check for voicemail number. Calling already have overall check for voicemail number.
                        //Also, this check will cause the VVM option dialog has no chance to show up.
                        //if (isVoicemailAvailable()) {
                            callVoicemail();
                        /*} else if (getActivity() != null) {
                            DialogFragment dialogFragment = ErrorDialogFragment.newInstance(
                                R.string.dialog_voicemail_not_ready_title,
                                R.string.dialog_voicemail_not_ready_message);
                            dialogFragment.show(getFragmentManager(), "voicemail_not_ready");
                        }*/
                        //MOTO Dialer Code IKHSS6UPGR-5380 - End
                        return true;
                    }
                    return false;
                }
                if (isDigitsEmpty() && onSpeedDialLongClick(1)) return true;
                break;
            }

            case R.id.two: {
                if (isDigitsEmpty() && onSpeedDialLongClick(2)) return true;
                break;
            }

            case R.id.three: {
                if (isDigitsEmpty() && onSpeedDialLongClick(3)) return true;
                break;
            }
            case R.id.four: {
                if (isDigitsEmpty() && onSpeedDialLongClick(4)) return true;
                break;
            }
            case R.id.five: {
                if (isDigitsEmpty() && onSpeedDialLongClick(5)) return true;
                break;
            }
            case R.id.six: {
                if (isDigitsEmpty() && onSpeedDialLongClick(6)) return true;
                break;
            }
            case R.id.seven: {
                if (isDigitsEmpty() && onSpeedDialLongClick(7)) return true;
                break;
            }
            case R.id.eight: {
                if (isDigitsEmpty() && onSpeedDialLongClick(8)) return true;
                break;
            }
            case R.id.nine: {
                if (isDigitsEmpty() && onSpeedDialLongClick(9)) return true;
                break;
            }
            case R.id.zero: {
                keyPressed(KeyEvent.KEYCODE_PLUS);
                return true;
            }
            case R.id.star: {
                keyPressed(KeyEvent.KEYCODE_COMMA);
                return true;
            }
            case R.id.pound: {
                keyPressed(KeyEvent.KEYCODE_SEMICOLON);
                return true;
            }
            /*case R.id.pound: {
                keyPressed(KeyEvent.KEYCODE_SPACE);
                return true;
            }*/
            case R.id.digits: {
                // Right now EditText does not show the "paste" option when cursor is not visible.
                // To show that, make the cursor visible, and return false, letting the EditText
                // show the option by itself.
                mDigits.setCursorVisible(true);
                return false;
            }
        }
        return false;
    }

    public void callVoicemail() {
        if (Utils.isVvmAvailable(getActivity())){
            //MOT Calling code CR - IKSTABLETWO-7063
            /* to-pass-build, Xinyu Liu/dcjf34
            Activity activity = getActivity();
            int dataRoaming = Settings.Secure.getInt(activity.getContentResolver(), Settings.Secure.DATA_ROAMING, 0);
            TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            if (tm.isNetworkRoaming()) {
                if (MotorolaSettings.getInt(activity.getContentResolver(), VM_VVM_ROAMING_SELECTION, 0) == REMINDER) {
                    showVMDataRoamingDialog();
                } else if (MotorolaSettings.getInt(activity.getContentResolver(), VM_VVM_ROAMING_SELECTION, 0) == VM_SELECTED) {
                    startActivity(newVoicemailIntent());
                    MEDialer.onVM(getActivity()); //MOT Dialer Code - IKHSS6-9198
                    if (DBG) log ("callVoicemail roaming: directly start VM as call Setting");
                } else if (MotorolaSettings.getInt(activity.getContentResolver(), VM_VVM_ROAMING_SELECTION, 0) == VVM_SELECTED) {
                    startActivity(newVVMIntent());
                    MEDialer.onVVM(getActivity()); //MOT Dialer Code - IKHSS6-9198
                    if (DBG) log ("callVoicemail roaming: directly start VVM as call Setting");
                } else if (DBG) log ("callVoicemail roaming: bailing out for wrong path");
            } else {
                if (MotorolaSettings.getInt(activity.getContentResolver(), VM_VVM_SELECTION, 0) == REMINDER) {
                    showVMVVMSelectDialog();
                } else if (MotorolaSettings.getInt(activity.getContentResolver(), VM_VVM_SELECTION, 0) == VM_SELECTED) {
                    startActivity(newVoicemailIntent());
                    MEDialer.onVM(getActivity()); //MOT Dialer Code - IKHSS6-9198
                    if (DBG) log ("callVoicemail: directly start VM as call Setting");
                } else if (MotorolaSettings.getInt(activity.getContentResolver(), VM_VVM_SELECTION, 0) == VVM_SELECTED) {
                    startActivity(newVVMIntent());
                    MEDialer.onVVM(getActivity()); //MOT Dialer Code - IKHSS6-9198
                    if (DBG) log ("callVoicemail roaming: directly start VVM as call Setting");
                } else if (DBG) log ("callVoicemail: bailing out for wrong path");
            } // MOT Calling code CR - IKSTABLETWO-7063 END*/
            startActivity(newVoicemailIntent());
        } else {
            startActivity(newVoicemailIntent());
        }
        mDigits.getText().clear(); // TODO: Fix bug 1745781
        //getActivity().finish(); // MOTO D - on end of the call we want to come back to the dialer itself
    }

    public static class ErrorDialogFragment extends DialogFragment {
        private int mTitleResId;
        private Integer mMessageResId;  // can be null

        private static final String ARG_TITLE_RES_ID = "argTitleResId";
        private static final String ARG_MESSAGE_RES_ID = "argMessageResId";

        public static ErrorDialogFragment newInstance(int titleResId) {
            return newInstanceInter(titleResId, null);
        }

        public static ErrorDialogFragment newInstance(int titleResId, int messageResId) {
            return newInstanceInter(titleResId, messageResId);
        }

        private static ErrorDialogFragment newInstanceInter(
                int titleResId, Integer messageResId) {
            final ErrorDialogFragment fragment = new ErrorDialogFragment();
            final Bundle args = new Bundle();
            args.putInt(ARG_TITLE_RES_ID, titleResId);
            if (messageResId != null) {
                args.putInt(ARG_MESSAGE_RES_ID, messageResId);
            }
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mTitleResId = getArguments().getInt(ARG_TITLE_RES_ID);
            if (getArguments().containsKey(ARG_MESSAGE_RES_ID)) {
                mMessageResId = getArguments().getInt(ARG_MESSAGE_RES_ID);
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(mTitleResId)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dismiss();
                                }
                            });
            if (mMessageResId != null) {
                builder.setMessage(mMessageResId);
            }
            return builder.create();
        }
    }

    /**
     * In most cases, when the dial button is pressed, there is a
     * number in digits area. Pack it in the intent, start the
     * outgoing call broadcast as a separate task and finish this
     * activity.
     *
     * When there is no digit and the phone is CDMA and off hook,
     * we're sending a blank flash for CDMA. CDMA networks use Flash
     * messages when special processing needs to be done, mainly for
     * 3-way or call waiting scenarios. Presumably, here we're in a
     * special 3-way scenario where the network needs a blank flash
     * before being able to add the new participant.  (This is not the
     * case with all 3-way calls, just certain CDMA infrastructures.)
     *
     * Otherwise, there is no digit, display the last dialed
     * number. Don't finish since the user may want to edit it. The
     * user needs to press the dial button again, to dial it (general
     * case described above).
     */
    public void dialButtonPressed() {
        mDigitsFilledByIntent = false;
        // MOT Calling code - IKSTABLETWO-6716
        if (mIsCallBeenPlaced){
            return;
        }
        // MOT Calling code - IKSTABLETWO-6716 End
        if (isDigitsEmpty()) { // No number entered.
            if (phoneIsCdma() && phoneIsOffhook()) {
                // This is really CDMA specific. On GSM is it possible
                // to be off hook and wanted to add a 3rd party using
                // the redial feature.
                startActivity(newFlashIntent());
            } else {
                //ChinaDev String lastNumberDialed = getLastOutgoingNumber();
                if (!TextUtils.isEmpty(mLastNumberDialed)) {
                    // Recall the last number dialed.
                    mDigits.setText(mLastNumberDialed);

                    // ...and move the cursor to the end of the digits string,
                    // so you'll be able to delete digits using the Delete
                    // button (just as if you had typed the number manually.)
                    //
                    // Note we use mDigits.getText().length() here, not
                    // mLastNumberDialed.length(), since the EditText widget now
                    // contains a *formatted* version of mLastNumberDialed (due to
                    // mTextWatcher) and its length may have changed.
                    mDigits.setSelection(mDigits.getText().length());
                } else {
                    // There's no "last number dialed" or the
                    // background query is still running. There's
                    // nothing useful for the Dial button to do in
                    // this case.  Note: with a soft dial button, this
                    // can never happens since the dial button is
                    // disabled under these conditons.
                    playTone(ToneGenerator.TONE_PROP_NACK);
                }
            }
        } else {
            final String number = mDigits.getText().toString();
            // MOT Calling Code - IKMAIN-18641
            if (mQwertySupported && (!SmartDialerUtil.hasDigitsChar(number))) {
                return;
            }
            // END MOT Calling Code - IKMAIN-18641

            // "persist.radio.otaspdial" is a temporary hack needed for one carrier's automated
            // test equipment.
            // TODO: clean it up.
            if (number != null
                    && !TextUtils.isEmpty(mProhibitedPhoneNumberRegexp)
                    && number.matches(mProhibitedPhoneNumberRegexp)
                    && (SystemProperties.getInt("persist.radio.otaspdial", 0) != 1)) {
                Log.i(TAG, "The phone number is prohibited explicitly by a rule.");
                if (getActivity() != null) {
                    DialogFragment dialogFragment = ErrorDialogFragment.newInstance(
                                    R.string.dialog_phone_call_prohibited_title);
                    dialogFragment.show(getFragmentManager(), "phone_prohibited_dialog");
                }

                // Clear the digits just in case.
                mDigits.getText().clear();
            } else {
                mIsCallBeenPlaced = true;  // MOT Calling code - IKSTABLETWO-6716
                mIsNumberBeenDialed = true; //MOT Calling Code - IKSTABLETWOV-1777

                /*final Intent intent = newDialNumberIntent(number);
                if (getActivity() instanceof DialtactsActivity) {
                    intent.putExtra(DialtactsActivity.EXTRA_CALL_ORIGIN,
                                    DialtactsActivity.CALL_ORIGIN_DIALTACTS);
                }
                //MOT Caling code -CR IKHSS7-1479
                if (MEDialer.getSmartDialer()) {
                    if (MEDialer.getQwertyKeypad()) {
                        MEDialer.onDial(getActivity(), intent, MEDialer.SmartDialFrom.QWERTY);
                    } else {
                        MEDialer.onDial(getActivity(), intent, MEDialer.SmartDialFrom.SOFT);
                    }
                } else {
                    MEDialer.onDial(getActivity(), intent, MEDialer.DialFrom.DIALERKEY);
                }
                //MOT Caling code -CR IKHSS7-1479 -End
                startActivity(intent);*/
                placeChinaCall();//ChinaDev
                //mDigits.getText().clear();  // TODO: Fix bug 1745781 //MOT Dialer Code, part fix of IKSTABLETWOV-1777
                if(mIntentWithData) { // MOTO A
                    mIntentWithData = false; // resetting bkdp84 bug 13058
                    // xjr467: bugfix #9855, #9917
                    // We have to delay finish() call because Android drops activity start
                    // if the current task moved to back immediately.
                    // It takes 500+ ms to pick up the call intent and show the incall screen, so
                    // we will sleep 1.5 sec before dialer app will be moved to back and the
                    // call initiator will be the top task.
                    new Thread() { public void run() {
                        SystemClock.sleep(1500);
                        // MOT Calling Code - IKHSS6UPGR-5263 Begin
                        Activity activity = getActivity();
                        if(activity != null)
                            activity.finish();
                        }
                        // MOT Calling Code - IKHSS6UPGR-5263 End
                    }.start();
                    // - end xjr467: bugfix #9855, #9917
                }
            }
        }
    }

    /**
     * Plays the specified tone for TONE_LENGTH_MS milliseconds.
     *
     * The tone is played locally, using the audio stream for phone calls.
     * Tones are played only if the "Audible touch tones" user preference
     * is checked, and are NOT played if the device is in silent mode.
     *
     * @param tone a tone code from {@link ToneGenerator}
     */
    void playTone(int tone) {
        // if local tone playback is disabled, just return.
        if (!mDTMFToneEnabled) {
            return;
        }

        // Also do nothing if the phone is in silent mode.
        // We need to re-check the ringer mode for *every* playTone()
        // call, rather than keeping a local flag that's updated in
        // onResume(), since it's possible to toggle silent mode without
        // leaving the current activity (via the ENDCALL-longpress menu.)
        AudioManager audioManager =
                (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if ((ringerMode == AudioManager.RINGER_MODE_SILENT)
            || (ringerMode == AudioManager.RINGER_MODE_VIBRATE)) {
            return;
        }

        synchronized (mToneGeneratorLock) {
            if (mToneGenerator == null) {
                Log.w(TAG, "playTone: mToneGenerator == null, tone: " + tone);
                return;
            }

            // Start the new tone (will stop any playing tone)
            mToneGenerator.startTone(tone, TONE_LENGTH_MS);
        }
    }

//MOTO Dialer Start --Comment out, do not use dialpad chooser any more
    /**
     * Brings up the "dialpad chooser" UI in place of the usual Dialer
     * elements (the textfield/button and the dialpad underneath).
     *
     * We show this UI if the user brings up the Dialer while a call is
     * already in progress, since there's a good chance we got here
     * accidentally (and the user really wanted the in-call dialpad instead).
     * So in this situation we display an intermediate UI that lets the user
     * explicitly choose between the in-call dialpad ("Use touch tone
     * keypad") and the regular Dialer ("Add call").  (Or, the option "Return
     * to call in progress" just goes back to the in-call UI with no dialpad
     * at all.)
     *
     * @param enabled If true, show the "dialpad chooser" instead
     *                of the regular Dialer UI
     */
    private void showDialpadChooser(boolean enabled) {
        // Check if onCreateView() is already called by checking one of View objects.
        if (!isLayoutReady()) {
            return;
        }

        if (enabled) {
            // Log.i(TAG, "Showing dialpad chooser!");
            /*if (mDigitsContainer != null) {
                mDigitsContainer.setVisibility(View.GONE);
            } else {
                // mDigits is not enclosed by the container. Make the digits field itself gone.
                mDigits.setVisibility(View.GONE);
            }
            if (mDialpad != null) {
                mDialpad.setVisibility(View.GONE);
                if (mDrawer != null) mDrawer.setVisibility(View.GONE);
                if (mList != null) mList.setVisibility(View.GONE);
                if (emptyText != null) emptyText.setVisibility(View.GONE);
                if (spaceHolder != null) spaceHolder.setVisibility(View.GONE);
            }
            mAdditionalButtonsRow.setVisibility(View.GONE);*/
            if (dialerLayout != null) dialerLayout.setVisibility(View.GONE);
            if (mDialpadChooser != null) mDialpadChooser.setVisibility(View.VISIBLE);
            if (mSearchMatchesTv != null) mSearchMatchesTv.setVisibility(View.GONE);
            if (mCallLocation != null) mCallLocation.setVisibility(View.GONE);

            // Instantiate the DialpadChooserAdapter and hook it up to the
            // ListView.  We do this only once.
            if (mDialpadChooserAdapter == null) {
                mDialpadChooserAdapter = new DialpadChooserAdapter(getActivity());
            }
            if (mDialpadChooser != null) mDialpadChooser.setAdapter(mDialpadChooserAdapter);
        } else {
            // Log.i(TAG, "Displaying normal Dialer UI.");
            /*if (mDigitsContainer != null) {
                mDigitsContainer.setVisibility(View.VISIBLE);
            } else {
                mDigits.setVisibility(View.VISIBLE);
            }
            if (mDialpad != null)  {
                mDialpad.setVisibility(View.VISIBLE);
                if (mDrawer != null)  mDrawer.setVisibility(View.VISIBLE);
                if (emptyText != null) emptyText.setVisibility(View.VISIBLE);
                if (mList != null) mList.setVisibility(View.VISIBLE);
                if (spaceHolder != null) spaceHolder.setVisibility(View.VISIBLE);
            }
            mAdditionalButtonsRow.setVisibility(View.VISIBLE);*/
            if (dialerLayout != null) dialerLayout.setVisibility(View.VISIBLE);
            if (mDialpadChooser != null) mDialpadChooser.setVisibility(View.GONE);
            changeNumberLocation();
            if (mList != null) {
                changeSearchingMatches(mList.getCount());
            }
        }
    }

    /**
     * @return true if we're currently showing the "dialpad chooser" UI.
     */
    private boolean dialpadChooserVisible() {
        return mDialpadChooser != null && mDialpadChooser.getVisibility() == View.VISIBLE;
    }

    /**
     * Simple list adapter, binding to an icon + text label
     * for each item in the "dialpad chooser" list.
     */
    private static class DialpadChooserAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        // Simple struct for a single "choice" item.
        static class ChoiceItem {
            String text;
            Bitmap icon;
            int id;

            public ChoiceItem(String s, Bitmap b, int i) {
                text = s;
                icon = b;
                id = i;
            }
        }

        // IDs for the possible "choices":
        //static final int DIALPAD_CHOICE_USE_DTMF_DIALPAD = 101;
        static final int DIALPAD_CHOICE_RETURN_TO_CALL = 101;
        static final int DIALPAD_CHOICE_ADD_NEW_CALL = 102;

        private static final int NUM_ITEMS = 2;
        private ChoiceItem mChoiceItems[] = new ChoiceItem[NUM_ITEMS];

        public DialpadChooserAdapter(Context context) {
            // Cache the LayoutInflate to avoid asking for a new one each time.
            mInflater = LayoutInflater.from(context);

            // Initialize the possible choices.
            // TODO: could this be specified entirely in XML?

            // - "Use touch tone keypad"
            /*mChoiceItems[0] = new ChoiceItem(
                    context.getString(R.string.dialer_useDtmfDialpad),
                    BitmapFactory.decodeResource(context.getResources(),
                                                 R.drawable.ic_dialer_fork_tt_keypad),
                    DIALPAD_CHOICE_USE_DTMF_DIALPAD);*/

            // - "Return to call in progress"
            mChoiceItems[0] = new ChoiceItem(
                    context.getString(R.string.dialer_returnToInCallScreen),
                    BitmapFactory.decodeResource(context.getResources(),
                                                 R.drawable.ic_dialer_fork_current_call),
                    DIALPAD_CHOICE_RETURN_TO_CALL);

            // - "Add call"
            mChoiceItems[1] = new ChoiceItem(
                    context.getString(R.string.dialer_addAnotherCall),
                    BitmapFactory.decodeResource(context.getResources(),
                                                 R.drawable.ic_dialer_fork_add_call),
                    DIALPAD_CHOICE_ADD_NEW_CALL);
        }

        public int getCount() {
            return NUM_ITEMS;
        }

        /**
         * Return the ChoiceItem for a given position.
         */
        public Object getItem(int position) {
            return mChoiceItems[position];
        }

        /**
         * Return a unique ID for each possible choice.
         */
        public long getItemId(int position) {
            return position;
        }

        /**
         * Make a view for each row.
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            // When convertView is non-null, we can reuse it (there's no need
            // to reinflate it.)
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.dialpad_chooser_list_item, null);
            }

            TextView text = (TextView) convertView.findViewById(R.id.text);
            text.setText(mChoiceItems[position].text);

            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            icon.setImageBitmap(mChoiceItems[position].icon);

            return convertView;
        }
    }

    /**
     * Handle clicks from the dialpad chooser.
     */
    public void onItemClick(AdapterView parent, View v, int position, long id) {
        DialpadChooserAdapter.ChoiceItem item =
                (DialpadChooserAdapter.ChoiceItem) parent.getItemAtPosition(position);
        int itemId = item.id;
        switch (itemId) {
            /*case DialpadChooserAdapter.DIALPAD_CHOICE_USE_DTMF_DIALPAD:
                // Log.i(TAG, "DIALPAD_CHOICE_USE_DTMF_DIALPAD");
                // Fire off an intent to go back to the in-call UI
                // with the dialpad visible.
                returnToInCallScreen(true);
                break;*/

            case DialpadChooserAdapter.DIALPAD_CHOICE_RETURN_TO_CALL:
                // Log.i(TAG, "DIALPAD_CHOICE_RETURN_TO_CALL");
                // Fire off an intent to go back to the in-call UI
                // (with the dialpad hidden).
                if (isInVTcall()) {
                    try {
                        Log.i(TAG, "Return to VT call");
                        Intent VTIntent = new Intent("com.motorola.videocall.RESTORE_VTCALL");
                        getActivity().sendBroadcast(VTIntent);
                        return;

                    } catch (Exception e) {
                        Log.w(TAG, "Return to VT call intent fail", e);
                    }
                } else {
                    // Fire off an intent to go back to the in-call UI
                    // (with the dialpad hidden).
                    returnToInCallScreen(false);
                }
                break;

            case DialpadChooserAdapter.DIALPAD_CHOICE_ADD_NEW_CALL:
                // Log.i(TAG, "DIALPAD_CHOICE_ADD_NEW_CALL");
                // Ok, guess the user really did want to be here (in the
                // regular Dialer) after all.  Bring back the normal Dialer UI.
                if (isInVTcall()) {
                    Toast.makeText(getActivity(), R.string.no_new_call_in_vt, Toast.LENGTH_LONG).show();
                } else {
                    // Ok, guess the user really did want to be here (in the
                    // regular Dialer) after all.  Bring back the normal Dialer UI.
                    showDialpadChooser(false);
                }
                break;

            default:
                Log.w(TAG, "onItemClick: unexpected itemId: " + itemId);
                break;
        }
    }

    /**
     * Returns to the in-call UI (where there's presumably a call in
     * progress) in response to the user selecting "use touch tone keypad"
     * or "return to call" from the dialpad chooser.
     */
    private void returnToInCallScreen(boolean showDialpad) {
        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            ITelephony phone2 = ITelephony.Stub.asInterface(ServiceManager.checkService("phone2"));//ChinaDev
            if (phone != null) {
                if (!phone.isIdle()) {
                    phone.showCallScreenWithDialpad(showDialpad);
                    return;
                }
            }
            if (phone2 != null) {
                if (!phone2.isIdle()) {
                    phone2.showCallScreenWithDialpad(showDialpad);
                    return;
                }
            }
        } catch (RemoteException e) {
            Log.w(TAG, "phone.showCallScreenWithDialpad() failed", e);
        }

        // Finally, finish() ourselves so that we don't stay on the
        // activity stack.
        // Note that we do this whether or not the showCallScreenWithDialpad()
        // call above had any effect or not!  (That call is a no-op if the
        // phone is idle, which can happen if the current call ends while
        // the dialpad chooser is up.  In this case we can't show the
        // InCallScreen, and there's no point staying here in the Dialer,
        // so we just take the user back where he came from...)
        getActivity().finish();
    }
// MOTO Dialer End Comment out

    /**
     * @return true if the phone is "in use", meaning that at least one line
     *              is active (ie. off hook or ringing or dialing).
     */
    public static boolean phoneIsInUse() {
        boolean phoneInUse = false;
        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            ITelephony phone2 = ITelephony.Stub.asInterface(ServiceManager.checkService("phone2"));

            if (phone != null)  phoneInUse = !phone.isIdle();
            if (!phoneInUse){
                if (phone2 != null)  phoneInUse = !phone2.isIdle();
            }
        }catch (RemoteException e) {
            Log.w(TAG, "phone.isIdle() failed", e);
        }
        return phoneInUse;
    }

    /**
     * @return true if the phone is a CDMA phone type
     */
    private boolean phoneIsCdma() {
        boolean isCdma = false;
        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            if (phone != null) {
                isCdma = (phone.getActivePhoneType() == TelephonyManager.PHONE_TYPE_CDMA);
            }
        } catch (RemoteException e) {
            Log.w(TAG, "phone.getActivePhoneType() failed", e);
        }
        return isCdma;
    }

    /**
     * @return true if the phone state is OFFHOOK
     */
    private boolean phoneIsOffhook() {
        boolean phoneOffhook = false;
        try {
            ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            if (phone != null) phoneOffhook = phone.isOffhook();
        } catch (RemoteException e) {
            Log.w(TAG, "phone.isOffhook() failed", e);
        }
        return phoneOffhook;
    }

    /**
     * Returns true whenever any one of the options from the menu is selected.
     * Code changes to support dialpad options
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(RomUtility.isOutofMemory()){
            Toast.makeText(this.getActivity(), R.string.rom_full, Toast.LENGTH_LONG).show();
            return true;
            }

        SharedPreferences shareddata;
        switch (item.getItemId()) {
            case R.id.menu_2s_pause:
                updateDialString(PhoneNumberUtils.PAUSE); //MOTO C, BZ11958
                return true;
            case R.id.menu_add_wait:
                updateDialString(PhoneNumberUtils.WAIT);
                return true;
            case R.id.menu_add_contacts:
                startActivity(getAddToContactIntent());
                return true;
            case R.id.menu_add_to_blacklist:
                startActivity(getAddToBlackListIntent());
                return true;
            case R.id.menu_add_to_whitelist:
                startActivity(getAddToWhiteListIntent());
                return true;
            //MOT Dialer Code Start - IKHSS7-1370
            case R.id.menu_send_sms:
                startActivity(getSendSmsIntent());
                return true;
            //MOT Dialer Code End - IKHSS7-1370
            //MOT Dialer Code Start - IKHSS6-4792
            case R.id.menu_search:
                if (mListener != null) {
                    mListener.onSearchButtonPressed();
                }
                return true;
            //MOT Dialer Code End - IKHSS6-4792
            case R.id.menu_cdma_ip_call:
            	// add by txbv34 for IKCBSMMCPPRC-1358
            	isIpCalling = true;
            	if(DEBUG)Log.d(TAG,"menu_cdma_ip_call");
                shareddata = getActivity().getSharedPreferences("IP_PREFIX", Context.MODE_WORLD_READABLE);
                ip_cdma = shareddata.getString("ip_cdma", null);
                if (ip_cdma == null) {
                    // IP Prefix for CDMA never setup by the user, so use the default setting.
                    ip_cdma = getString(R.string.default_cdma_ip_prefix);
                    // launch CDMA IP call
                    isIpCall = true;
                    placeCall(PHONE_TYPE_CDMA);
                } else if (ip_cdma.length() == 0) {
                     // IP Prefix for CDMA is disabled
                    AlertDialog dialog = new AlertDialog.Builder(mDialogContext)
                        .setTitle(R.string.no_ip_dlg_title)  
                        .setMessage(R.string.no_cdma_ip_dlg_msg)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                isIpCall = true;
                                placeCall(PHONE_TYPE_CDMA);
                            }
                        })
                        .create();
                    dialog.show();
                    return false;
                } else {
                    // launch CDMA IP call
                    isIpCall = true;
                    placeCall(PHONE_TYPE_CDMA);
                }
                return true;
            case R.id.menu_gsm_ip_call:
            	// add by txbv34 for IKCBSMMCPPRC-1358
            	isIpCalling = true;
            	if(DEBUG)Log.d(TAG,"menu_gsm_ip_call");
                shareddata = getActivity().getSharedPreferences("IP_PREFIX", Context.MODE_WORLD_READABLE);
                ip_gsm = shareddata.getString("ip_gsm", null);
                if (ip_gsm == null) {
                    // IP Prefix for GSM never setup by the user, so use the default setting.
                    int res_id = ContactsUtils.getDefaultIPPrefixbyPhoneType(PHONE_TYPE_GSM);
                    if (res_id != -1) {
                        ip_gsm = getString(res_id);
                    } else { // For safty
                        ip_gsm = getString(R.string.default_gsm_ip_prefix);
                    }
                    // launch GSM IP call
                    isIpCall = true;
                    placeCall(PHONE_TYPE_GSM);
                } else if (ip_gsm.length() == 0) {
                     // IP Prefix for GSM is disabled
                    AlertDialog dialog = new AlertDialog.Builder(mDialogContext)
                        .setTitle(R.string.no_ip_dlg_title)  
                        .setMessage(R.string.no_gsm_ip_dlg_msg)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                isIpCall = true;
                                placeCall(PHONE_TYPE_GSM);
                            }
                        })
                        .create();
                    dialog.show();
                    return false;
                } else {
                    // launch GSM IP call
                    isIpCall = true;
                    placeCall(PHONE_TYPE_GSM);
                }
                return true;
            case R.id.menu_cdma_intl_roaming_call:
                isIntRoamCall = true;
                placeCall(PHONE_TYPE_CDMA);
                return true;
            case R.id.menu_gsm_intl_roaming_call:
                isIntRoamCall = true;
                placeCall(PHONE_TYPE_GSM);
                return true;
            case R.id.menu_intl_roaming_call_back:
                isIntRoamCallBackCall = true;
                placeCall(PHONE_TYPE_GSM);
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    /**
     * Updates the dial string (mDigits) after inserting a Pause character (,)
     * or Wait character (;).
     */
    // [MOTO: Begin A - BZ11958 vsood1 - Handle Pause/wait char input
    private void updateDialString(char type) {
        // insert pause at cursor
        StringBuilder sb = new StringBuilder(mDigits.getText());
        // MOT Calling code - IKPITTSBURGH-237
        int selectionStart = mDigits.getSelectionStart();
        int selectionEnd = mDigits.getSelectionEnd();

        // BEGIN Motorola, w21071, 2011-08-09, IKSPYDERSKT-441
        // In order to fix the wrong cursor position in korean language mode.
        //    (korean language mode has different number formatting rule)
        // - handling only in case of adding a pause/wait to the end of phone number.
        // - the "-" characters to be automatically added during formatNumber() causes wrong cursor position.
        // - to fix this, locate the cursor to the end of number calculating the length of digits again.
        boolean cursorStartsFromEnd = (mDigits.length() == selectionStart || mDigits.length() == selectionEnd);
        // END IKSPYDERSKT-441

        // MOT Calling LIBtt25116
        if (selectionStart > selectionEnd) {
            // swap it as we want start to be less then end
            int tmp = selectionStart;
            selectionStart = selectionEnd;
            selectionEnd = tmp;
        } // end - MOT Calling LIBtt25116

        if (selectionStart != selectionEnd) {
            // There is a selection. So delete the selection first before allowing the
            // pause/wait character to be inserted at selectionStart
            sb.delete(selectionStart, selectionEnd);
        }
        String newStr =
        sb.insert(selectionStart, type).toString();
        // End MOT Calling code - IKPITTSBURGH-237

        // MOT Calling Code - IKMAIN-18641
        if ((!mQwertySupported) || SmartDialerUtil.noAlphaChar(newStr)) { //pure dialable string
            String dialStr = PhoneNumberUtils.extractNetworkPortion(newStr);
            String postDialStr = PhoneNumberUtils.extractPostDialPortion(newStr);
            if((null != dialStr) && (null != postDialStr)) {
                /* MOTO MOD BEGIN IKHSS7-2038
                 * Changed calling PhoneNumberUtils.formatNumber API to New API
                 * PhoneNumberUtilsExt.formatNumber for Hyphensation Feature 35615
                */
                mDigits.setText(PhoneNumberUtilsExt.formatNumber(getActivity(), dialStr, null, mCurrentCountryIso) + postDialStr);
                //MOTO END IKHSS7-2038
            }
        } else {
            mDigits.setText(newStr);
        }
        // END MOT Calling Code - IKMAIN-18641

        // [ MOTO:Begin A rdq478 Bug 18302, handle a case where the user enter more than 16 digits
        //   and then adding a pause/wait which will cause the "-" characters to be removed out
        //   during formatNumber() above. This will cause cursorPos to be larger than length of text.
        if ( selectionStart >= mDigits.length() )
            selectionStart = mDigits.length() - 1;
        // ] MOTO:End A rdq478 Bug 18302
        // BEGIN Motorola, w21071, 2011-08-09, IKSPYDERSKT-441
        // In order to fix the wrong cursor position in korean language mode.
        //    (korean language mode has different number formatting rule)
        // - handling only in case of adding a pause/wait to the end of phone number.
        // - the "-" characters to be automatically added during formatNumber() causes wrong cursor position.
        // - to fix this, locate the cursor to the end of number calculating the length of digits again.
        else if (cursorStartsFromEnd && (selectionStart != (mDigits.length() - 1)))
            selectionStart = mDigits.length() - 1;
        // END IKSPYDERSKT-441

        mDigits.setSelection(selectionStart+1);
    }
    //MOT Dialer Code End

// MOT Dialer Start -- Comment out
//     Do not use it anymore, use updateAdditionalBtnState() instead
//    /**
//     * Update the enabledness of the "Dial" and "Backspace" buttons if applicable.
//     */
//    private void updateDialAndDeleteButtonEnabledState() {
//        final boolean digitsNotEmpty = !isDigitsEmpty();
//
//        if (mDialButton != null) {
//            // On CDMA phones, if we're already on a call, we *always*
//            // enable the Dial button (since you can press it without
//            // entering any digits to send an empty flash.)
//            if (phoneIsCdma() && phoneIsOffhook()) {
//                mDialButton.setEnabled(true);
//            } else {
//                // Common case: GSM, or CDMA but not on a call.
//                // Enable the Dial button if some digits have
//                // been entered, or if there is a last dialed number
//                // that could be redialed.
//                mDialButton.setEnabled(digitsNotEmpty ||
//                        !TextUtils.isEmpty(mLastNumberDialed));
//            }
//        }
//        mDelete.setEnabled(digitsNotEmpty);
//    }
// MOT Dialer End

    /**
     * Check if voicemail is enabled/accessible.
     *
     * @return true if voicemail is enabled and accessibly. Note that this can be false
     * "temporarily" after the app boot.
     * @see TelephonyManager#getVoiceMailNumber()
     */
    private boolean isVoicemailAvailable() {
        try {
            return (TelephonyManager.getDefault().getVoiceMailNumber() != null);
        } catch (SecurityException se) {
            // Possibly no READ_PHONE_STATE privilege.
            Log.w(TAG, "SecurityException is thrown. Maybe privilege isn't sufficient.");
        }
        return false;
    }

    /**
     * This function return true if Wait menu item can be shown
     * otherwise returns false. Assumes the passed string is non-empty
     * and the 0th index check is not required.
     */
    private static boolean showWait(int start, int end, String digits) {
        if (start == end) {
            // visible false in this case
            if (start > digits.length()) return false;

            // preceding char is ';', so visible should be false
            if (digits.charAt(start - 1) == ';') return false;

            // next char is ';', so visible should be false
            if ((digits.length() > start) && (digits.charAt(start) == ';')) return false;
        } else {
            // visible false in this case
            if (start > digits.length() || end > digits.length()) return false;

            // In this case we need to just check for ';' preceding to start
            // or next to end
            if (digits.charAt(start - 1) == ';') return false;
        }
        return true;
    }

    /**
     * @return true if the widget with the phone number digits is empty.
     */
    private boolean isDigitsEmpty() {
        return mDigits.length() == 0;
    }

    /**
     * Starts the asyn query to get the last dialed/outgoing
     * number. When the background query finishes, mLastNumberDialed
     * is set to the last dialed number or an empty string if none
     * exists yet.
     */
    private void queryLastOutgoingCall() {
        mLastNumberDialed = EMPTY_NUMBER;
        CallLogAsync.GetLastOutgoingCallArgs lastCallArgs =
                new CallLogAsync.GetLastOutgoingCallArgs(
                    getActivity(),
                    new CallLogAsync.OnLastOutgoingCallComplete() {
                        public void lastOutgoingCall(String number) {
                            // TODO: Filter out emergency numbers if
                            // the carrier does not want redial for
                            // these.
                            mLastNumberDialed = number;
                            //ChinaDev updateDialAndDeleteButtonEnabledState();
                        }
                    });
        mCallLog.getLastOutgoingCall(lastCallArgs);
    }

//MOT Dialer Start
//    private String getLastOutgoingNumber() {
//
//        String outgoingNum=EMPTY_NUMBER;
//
//        /** The projection to use when querying the call log table */
//        String[] CALL_LOG_PROJECTION = new String[] {
//            Calls.NUMBER
//        };
//
//        StringBuilder where = new StringBuilder(Calls.TYPE);
//        where.append("=");
//        where.append(Calls.OUTGOING_TYPE);
//        Log.w (TAG, "filter: " + where);
//        Cursor c = getActivity().getContentResolver().query(Calls.CONTENT_URI,CALL_LOG_PROJECTION,
//                    where.toString(), null,Calls.DEFAULT_SORT_ORDER + " LIMIT 1");
//        if(c != null) {
//            Log.w (TAG, "Cursor count=" + c.getCount());
//            if(c.moveToFirst()){
//                outgoingNum = c.getString(c.getColumnIndex(Calls.NUMBER));
//                Log.w (TAG, "Recent outgoing call = "+outgoingNum);
//            }
//            c.close();
//        }else{
//        Log.w (TAG, "No outgoing calls");
//    }
//        return outgoingNum;
//    }
// MOT Dialer End

    // Helpers for the call intents.
    private static Intent newVoicemailIntent() {
        final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                                         Uri.fromParts("voicemail", EMPTY_NUMBER, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private static Intent newVVMIntent() {
    // MOT Calling code - IKSHADOW-6090
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.setClassName(VVM_PACKAGE_NAME, VVM_ACTIVITY_NAME);
    // MOT Calling code - IKSHADOW-6090 - End
    return intent;
    }

    private Intent newFlashIntent() {
        final Intent intent = newDialNumberIntent(EMPTY_NUMBER);
        intent.putExtra(EXTRA_SEND_EMPTY_FLASH, true);
        return intent;
    }

    private Intent newDialNumberIntent(String number) {
        Intent intent;
        if (ContactsUtils.haveTwoCards(getActivity())) {
            intent = new Intent(ContactsUtils.TRANS_DIALPAD);
            intent.putExtra("phoneNumber", number);
        } else {
            intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                                             Uri.fromParts("tel", number, null));
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (mIsAdFtrOn) { // Motorola IKOLYMP-5641
            intent.putExtra(CALLED_BY, DIAL_BY_DIALER);
            if (DBG) log("Assisted Dialing: 12keypad dialer: placecall(): has put call_by intent extra");
        }
        return intent;
    }

    //MOT Dialer Start
    private Intent getSendSmsIntent() {
        //MOT Dialer Code - IKHSS6-13862
        String digits = PhoneNumberUtils.convertKeypadLettersToDigits(mDigits.getText().toString());
        final Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("sms",digits , null));
        return intent;
    }

    private static Intent getVoiceDialIntent() {
        final Intent intent = new Intent(Intent.ACTION_VOICE_COMMAND);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
    //MOT Dialer End

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        mShowOptionsMenu = visible;
        //MOT Dialer Code Start - IKHSS7-9457
        if (mAdapter != null) {
            if (visible && isResumed()) {//MOT Dialer code IKHSS7-8639
                mAdapter.resumeQuery();
            } else if (!visible){ //visibility is false
                if(mAdapter != null){
                    mAdapter.pauseCleanUp();
                }
            }
        }
        fragmentVisibilty = visible;//MOT Dialer code IKHSS7-8639
        //MOT Dialer Code End - IKHSS7-9457
    }

//Google 4.0.4 Start
//    /**
//     * Update visibility of the search button and menu button at the bottom of dialer screen, which
//     * should be invisible when bottom ActionBar's real items are available and be visible
//     * otherwise.
//     *
//     * @param visible True when visible.
//     */
//    public void updateFakeMenuButtonsVisibility(boolean visible) {
//        if (DEBUG) Log.d(TAG, "updateFakeMenuButtonVisibility(" + visible + ")");
//
//        if (mSearchButton != null) {
//            if (visible) {
//                mSearchButton.setVisibility(View.VISIBLE);
//            } else {
//                mSearchButton.setVisibility(View.INVISIBLE);
//            }
//        }
//        if (mMenuButton != null) {
//            if (visible && !ViewConfiguration.get(getActivity()).hasPermanentMenuKey()) {
//                mMenuButton.setVisibility(View.VISIBLE);
//            } else {
//                mMenuButton.setVisibility(View.INVISIBLE);
//            }
//        }
//    }
//Google 4.0.4 End

    // MOT Dialer Start
    private String lastQueryDigits = "";
    /**
     * This function will be called when user input digits into editBox
     */
    private void smartDialerInputChange (CharSequence input) {
        String inputDigits = SmartDialerUtil.stripSeparators(input.toString());
        if ((inputDigits == null) || (mAdapter == null)) {
            return;
        }
        if (DBG) log("inputDigits = " + inputDigits);
        if(inputDigits.length() >= SmartDialerAdapter.START_QUERY_DIGITS_NUMBER) {
            if (inputDigits.equals(lastQueryDigits)) {
                log("skip currquery =  "+ input + "lastQueryDigits = " + lastQueryDigits);
                return;
            }
            mAdapter.startQuery(SmartDialerAdapter.QUERY_STRING_MATCHED);//MOT Calling Code - IKPIM-447
        } else {
            //ChinaDev showList(false); //MOT Calling code IKPIM-660
            mAdapter.startQuery(SmartDialerAdapter.QUERY_MOST_RECENT);
        }
        lastQueryDigits = inputDigits;
    }
    //END - MOT calling code- IKMAIN-4172


    /**
     * init a smart dialer adapter
     */
    private void initSmartDialer(View view) {
        mAdapter = new SmartDialerAdapter(getActivity(), R.layout.smart_dialer_list, view, mCurrentCountryIso, this);
        final View toggleView = view;
        mList = (ListView) view.findViewById(R.id.sd_list);//ChinaDev
        mList.setAdapter(mAdapter);
        mList.setFocusable(false);
        mList.setFocusableInTouchMode(false);

        //MOT Calling code IKPIM-660
        mDrawer = (SlidingDrawer) view.findViewById(R.id.drawer);//ChinaDev

        /*ChinaDev
        view.findViewById(R.id.sd_title).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showList(mList.getVisibility() != View.VISIBLE);
            }
        });*/

        mDigits.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN && mDrawer != null
                		&& !mDrawer.isOpened() && !mDrawer.isMoving()) {
                    showList(false);
                }
                return false; //MOT Dialer Code -IKHSS6UPGR-9355
            }
        });

        initChinaSmartDialer(view);
        /*ChinaDev mDrawer.animateOpen();
        mDrawer.setOnDrawerOpenListener(new SlidingDrawer.OnDrawerOpenListener() {
            public void onDrawerOpened() {
                toggleList(toggleView, false);
            }
        });
        mDrawer.setOnDrawerCloseListener(new SlidingDrawer.OnDrawerCloseListener() {
            public void onDrawerClosed() {
                toggleList(toggleView, true);
            }
        });*/
        //MOT Calling code End IKPIM-660
    }
    //MOT Calling code IKPIM-660
    void showList(boolean showList) {
    	if (mDrawer == null) return;
        if (!showList) {
            if (!mDrawer.isOpened()) {
                mDrawer.animateOpen();
            }
    } else {
            if (mDrawer.isOpened()) {
                mDrawer.animateClose();
            }
    }
    }
    //MOT Calling code End IKPIM-660

    /**
     * expand/collapse smart dialer search results list
     */
    /*ChinaDev
     private void toggleList(View view, boolean showList) {
        if (mAdapter == null) return; // MOT Calling Code - IKMAIN-26904
        View single_item = null;
        ListView mList = (ListView)view.findViewById(R.id.sd_list);
        ImageView expander = (ImageView)view.findViewById(R.id.sd_expandIcon);
        if (mAdapter.isFullSingleItemNeeded()) { //MOT Calling Code - IKMAIN-21175
            single_item = view.findViewById(R.id.sd_singleItem);
        }

        if (DBG) log("toggleList = " + showList);
        if (showList) {
            if (single_item != null) {
                single_item.setVisibility(View.GONE);
            }
            view.findViewById(R.id.dialPad).setVisibility(View.GONE);
            //MOT Dialer Code - IKHSS6-5094
            view.findViewById(R.id.divider_while_expand_list).setVisibility(View.VISIBLE);
            mList.setVisibility(View.VISIBLE);
            mList.setSelection(0);
            // MOT Calling code - IKSTABLETWOV-2893
            expander.setImageResource(com.android.internal.
                R.drawable.expander_close_holo_dark); //MOT Dialer Code - IKHSS7-3963
        } else {
            if (single_item != null) {
                single_item.setVisibility(View.VISIBLE);
            }
            view.findViewById(R.id.dialPad).setVisibility(View.VISIBLE);
            //MOT Dialer Code - IKHSS6-5094
            view.findViewById(R.id.divider_while_expand_list).setVisibility(View.GONE);
            mList.setVisibility(View.GONE);
            // MOT Calling code - IKSTABLETWOV-2893
            expander.setImageResource(com.android.internal.
                R.drawable.expander_open_holo_dark); //MOT Dialer Code - IKHSS7-3963
        }
        mAdapter.updateSingleItemAndTitle();
    }*/

    //MOT Calling Code IKSTABLETWO-313: Add Assisted Dialing UI in GSM mode
    private boolean isAdFtrOn() {
        /* to-pass-build, Xinyu Liu/dcjf34 */ 
        int is33861On = 0;//MotorolaSettings.getInt(getActivity().getContentResolver(), CALL_GSM_AD_ENABLE, 0);
        if(phoneIsCdma()){
            return mAdCDMAFeatureOn;
        } else if ((!phoneIsCdma()) && (is33861On == 1)) {
            return mAdCDMAFeatureOn;
        } else
            return false;
    }
    //End MOT Calling Code IKSTABLETWO-313

    //[ MOTO:Begin A rdq478 Bug 3160, check if voice dial is supported or not for the device language
    private boolean isVoiceCommandSupported() {
        String langSupported = getResources().getString(R.string.voice_dialer_supported_locales); //MOT Calling Code - IKSTABLETWO-1038
        if ( langSupported != null ) {
            langSupported = langSupported.replaceAll(" ", "").toLowerCase();    // remove any unnecessary space
            String[] langList = langSupported.split(",");
            Locale locale = Locale.getDefault();
            String deviceLang = locale.getLanguage().toLowerCase() + "_" + locale.getCountry().toLowerCase();
            // search for supported language
            for ( int i = 0; i < langList.length; i++ )
                if ( deviceLang.compareTo(langList[i]) == 0 )
                    return true;
        }
        return false;
    }
    // ] MOTO:End A rdq478 Bug 3160, check if voice dial is supported or not for the device language

    private void updateAdditionalBtnState() {
        if (mQwertySupported) {
            if (isDigitsEmpty()) {
                // enable call button to populate last outgoing call number
                mDialButton.setEnabled(true);
                mDialButton.setAlpha(255);

                //MOT FID 35413-DialerLocalization IKCBS-2014
                if (mMessageBtnFtrFlag) {
                    mRightButton.setEnabled(false);
                    mRightButton.setAlpha(100);
                }
                //END MOT FID 35413-DialerLocalization IKCBS-2014
            } else {
                if (SmartDialerUtil.hasDigitsChar(mDigits.getText().toString())) {
                    // enable Call buttons
                    mDialButton.setEnabled(true);
                    mDialButton.setAlpha(255);
                    //MOT FID 35413-DialerLocalization IKCBS-2014
                    if (mMessageBtnFtrFlag) {
                        mRightButton.setEnabled(true);
                        mRightButton.setAlpha(255);
                    }
                    //END MOT FID 35413-DialerLocalization IKCBS-2014
                } else {
                    // disalbe Call buttons
                    mDialButton.setEnabled(false);
                    mDialButton.setAlpha(100);
                    //MOT FID 35413-DialerLocalization IKCBS-2014
                    if (mMessageBtnFtrFlag) {
                        mRightButton.setEnabled(false);
                        mRightButton.setAlpha(100);
                    }
                    //END MOT FID 35413-DialerLocalization IKCBS-2014
                }
            }
        } else {
            if (isDigitsEmpty()) {
                //MOT FID 35413-DialerLocalization IKCBS-2014
                if (mMessageBtnFtrFlag) {
                    mRightButton.setEnabled(false);
                    mRightButton.setAlpha(100);
                }
                //END MOT FID 35413-DialerLocalization IKCBS-2014
            } else {
                //MOT FID 35413-DialerLocalization IKCBS-2014
                if (mMessageBtnFtrFlag) {
                    mRightButton.setEnabled(true);
                    mRightButton.setAlpha(255);
                }
                //END MOT FID 35413-DialerLocalization IKCBS-2014
            }
        }
    }

    private void configureDialerButton() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //ChinaDev mButton_left_id = sp.getInt(DIALER_BUTTON_LEFT, ADDTOCONTACT);
        mButton_right_id = sp.getInt(DIALER_BUTTON_RIGHT, VOICECOMMAND);
        //ChinaDev setDialerButtonImg(mLeftButton, mButton_left_id);
        setDialerButtonImg(mRightButton, mButton_right_id);
    }

    private void setDialerButtonImg(ImageView view, int id) {
    	 int resId;
         CharSequence txt;//MOT Dialer Code - IKHSS7-15079
         switch (id) {
            /*ChinaDev case ADDTOCONTACT:
                resId = R.drawable.ic_add_contact;//MOT Dialer code - IKHSS6-4792, IKHSS7-6208
                txt = getString(R.string.addtocontact);//MOT Dialer Code - IKHSS7-15079
                break;*/
            case SENDMESSAGE:
                resId = R.drawable.ic_control_message;
                txt = getString(R.string.description_message_button);//MOT Dialer Code - IKHSS7-15079
                break;
            case VOICECOMMAND:
                resId = R.drawable.ic_voice_search_holo_dark; //MOT Dialer Code - IKHSS6-11871
                txt = getString(R.string.description_voiceCommands_button);//MOT Dialer Code - IKHSS7-15079
                break;
            /*ChinaDev case SEARCH:
                resId = R.drawable.ic_dial_action_search;
                txt = getString(R.string.description_search_button);//MOT Dialer Code - IKHSS7-15079
                break;*/
            case VOICEMAIL:
                resId = R.drawable.ic_control_voicemail;
                txt = getString(R.string.description_voicemail_button);//MOT Dialer Code - IKHSS7-15079
                break;
            default:
                Log.w(TAG, "invalid button action.");
                return;
        }
        view.setImageResource(resId);
        view.setContentDescription(txt);//MOT Dialer Code - IKHSS7-15079
    }

    private void setDialerButtonFunc(int id) {
        try{
            switch (id) {
                /*ChinaDev case ADDTOCONTACT:
                    getActivity().startActivity(getAddToContactIntent());//MOT Dialer Code - IKHSS6-13862
                    return;*/
                case SENDMESSAGE:
                    getActivity().startActivity(getSendSmsIntent());
                    return;
                case VOICECOMMAND:
                    getActivity().startActivity(getVoiceDialIntent());
                    return;
                /*ChinaDev case SEARCH:
                    if (mListener != null) {
                        mListener.onSearchButtonPressed();
                    }
                    return;*/
                case VOICEMAIL:
                    callVoicemail();
                    return;
                default:
                    Log.w(TAG, "invalid button action.");
                    return;
            }
        }catch (ActivityNotFoundException e) {
                Log.w(TAG, "Failed to launch voice dialer: " + e);
        }
    }

    //tmp function, set left/right dialer button to sharedPreference by feature flag
    private void setDialerButton() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sp.edit();

        //left button
        //left button is always Search button currently by existing feature.
        //ChinaDev editor.putInt(DIALER_BUTTON_LEFT, SEARCH);
        //right button
        //currently, right button maybe voicemail, msg, voice command
        int tmp;
        if (mMessageBtnFtrFlag) {
            tmp = SENDMESSAGE;
        } else if (isVoiceCommandSupported()) {
            tmp = VOICECOMMAND;
        } else {
            tmp = VOICEMAIL;
        }
        editor.putInt(DIALER_BUTTON_RIGHT, tmp);
        // commit and log the result.
        if (!editor.commit()) {
            log("failed to commit dialer button preference");
        }
    }

    void showVMDataRoamingDialog(){
        if(DBG) log("show vm data roaming dialog");
        VMDataRoamingDialogFragment.show(this);
    }

    void showVMVVMSelectDialog(){
        VMVVMSelectDialogFragment.show(this);
    }

    /**
     * The VMDataRoaming dialog.
     */
    public static class VMDataRoamingDialogFragment extends DialogFragment {
        public static void show(Fragment fragment) {
            if(DBG) log("VMDataRoamingDialogFragment");
            VMDataRoamingDialogFragment dialog = new VMDataRoamingDialogFragment();
            dialog.setTargetFragment(fragment, 0);
            dialog.show(fragment.getFragmentManager(), "VMDataRoaming");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.vmvvm_roaming_dialog, null);
            final CheckBox VMVVM_roaming_remembermychoice = (CheckBox) dialogView.findViewById(R.id.vmvvm_roaming_toggle);
            final TextView textMsg = (TextView) dialogView.findViewById(R.id.vmvvm_roaming_text);
            textMsg.setText(getActivity().getResources().getText(R.string.Data_roaming_msg));

            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.Data_roaming_title)
                .setPositiveButton(R.string.visual_voicemail, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Settings.Secure.putInt(getActivity().getContentResolver(), Settings.Secure.DATA_ROAMING, 1);
                            startActivity(newVVMIntent());
                            MEDialer.onVVM(getActivity()); //MOT Dialer Code - IKHSS6-9198
                            if (VMVVM_roaming_remembermychoice.isChecked())
                            /* to-pass-build, Xinyu Liu/dcjf34 */ 
                            ;//MotorolaSettings.putInt(getActivity().getContentResolver(), VM_VVM_ROAMING_SELECTION, VVM_SELECTED);
                            }
                    }
                )
                .setNegativeButton(R.string.voicemail,  new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (DBG) log ("showVMDataRoamingDialog VM in roaming selected");
                        startActivity(newVoicemailIntent());
                        MEDialer.onVM(getActivity()); //MOT Dialer Code - IKHSS6-9198
                        if (VMVVM_roaming_remembermychoice.isChecked())
                            /* to-pass-build, Xinyu Liu/dcjf34 */ 
                            ;//MotorolaSettings.putInt(getActivity().getContentResolver(), VM_VVM_ROAMING_SELECTION, VM_SELECTED);
                        }
                })
                .setView(dialogView)
                .create();
            return dialog;
        }
    }

    /**
     * The VMVVMSelect dialog.
     */
    public static class VMVVMSelectDialogFragment extends DialogFragment {
        public static void show(Fragment fragment) {
            VMVVMSelectDialogFragment dialog = new VMVVMSelectDialogFragment();
            dialog.setTargetFragment(fragment, 0);
            dialog.show(fragment.getFragmentManager(), "VMVVMSelect");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog mVMVVMSelectDialog;
            View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.vmvvm_select_dialog, null);
            final CheckBox VMVVM_remembermychoice = (CheckBox) dialogView.findViewById(R.id.vmvvm_select_toggle);
            //MOT Calling code -CR  IKHSS6-2320
            //When clickListner is not null, Audible selection feedback will be heared
            VMVVM_remembermychoice.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){}
                    });
            //MOT Calling code  IKHSS6-2320 -End
            final ListView vmvvmList = (ListView) dialogView.findViewById(R.id.vmvvm_list);
            vmvvmList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            vmvvmList.setAdapter(new ArrayAdapter<String>(
                (Context)getActivity(),
                //android.R.layout.simple_list_item_1,
                R.layout.vmvvm_select_text,
                new String[]{getString(R.string.voicemail), getString(R.string.visual_voicemail)}
            ));
            vmvvmList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
                public void onItemClick(AdapterView<?> listView, View listItem, int position, long id) {
                    boolean mIsVVMselected = false;
                    mIsVVMselected = (0 == position)?false:true;
                    if (VMVVM_remembermychoice.isChecked()) {
                        if (DBG) log ("showVMVVMSelectDialog: reminder is checked");
                        /* to-pass-build, Xinyu Liu/dcjf34 */ 
                        //MotorolaSettings.putInt(getActivity().getContentResolver(), VM_VVM_SELECTION, mIsVVMselected?VVM_SELECTED:VM_SELECTED);
                        if (!mIsVVMselected)
                            /* to-pass-build, Xinyu Liu/dcjf34 */ 
                            ;//MotorolaSettings.putInt(getActivity().getContentResolver(), VM_VVM_ROAMING_SELECTION, VM_SELECTED);
                    }
                    if (mIsVVMselected) {
                        startActivity(newVVMIntent());
                        MEDialer.onVVM(getActivity()); //MOT Dialer Code - IKHSS6-9198
                        if (DBG) log ("callVoicemail: VVM chosen in dialog");
                    } else {
                        startActivity(newVoicemailIntent());
                        MEDialer.onVM(getActivity()); //MOT Dialer Code - IKHSS6-9198
                        if (DBG) log ("callVoicemail: VM chosen in dialog");
                    }
                    dismiss();
                }
            });

            mVMVVMSelectDialog = new AlertDialog.Builder(getActivity()) //Mot calling code IKSTABLEFOURV-1552
                    .setTitle(R.string.voicemail_choose_type_title)
                    .setCancelable(true)
                    .setView(dialogView)
                    .create();
            return mVMVVMSelectDialog;
        }
    }

    // MOT Calling code - IKSTABLEFOURV-1933
    String getVoiceMailTag(String number) {
        if (mVoiceMailNumber == null) {
            if (DBG) log("start to query voicemain number");
            TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(
                    Context.TELEPHONY_SERVICE);
            mVoiceMailNumber = tm.getVoiceMailNumber();
            mVoiceMailTag = tm.getVoiceMailAlphaTag();
        }
        if (PhoneNumberUtils.compare(number, mVoiceMailNumber))
            return mVoiceMailTag;
        else
            return null;
    }
    // END- MOT Calling code - IKSTABLEFOURV-1933

    private static void log(String msg) {
        Log.d(TAG, msg);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // MOTO Dialer Code - IKHSS6-626 Begin
        Activity activity = getActivity();
        if (requestCode == PICK_CONTACT_FOR_SPEED_DIAL_REQ && resultCode == Activity.RESULT_OK
                && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                final String[] PHONES_PROJECTION = {
                    Phone.NUMBER
                };
                Cursor c = activity.getContentResolver().query(uri, PHONES_PROJECTION, null, null,
                        null);
                if (null != c) {
                    if (c.moveToFirst()) {
                        String number = c.getString(c.getColumnIndex(Phone.NUMBER));
                        Utils.tryAssignSpeedDial(activity, mSpeedDialAssignKey, PhoneNumberUtils.stripSeparators(number));
                    }
                    c.close();
                }
            }
        }
        // MOTO Dialer Code - IKHSS6-626 End
    }
    // MOTO Dialer Code - IKHSS6-626 Begin
    private static final int PICK_CONTACT_FOR_SPEED_DIAL_REQ = 0;
    private static final String SPEED_DIAL_ASSIGN_KEY = "SpeedDialAssignKey";
    private int mSpeedDialAssignKey = -1;

    private boolean onSpeedDialLongClick(int keyname) {
        log("onKeyLongClick " + keyname);
        Activity activity = getActivity();
        String number = Utils.getSpeedDialNumberByPos(activity, keyname); // Motorola, Aug-05-2011,
                                                                          // IKPIM-282
        if (null != number) {
            if (ContactsUtils.haveTwoCards(activity)) {
                if (mDigits != null) {
                    mDigits.setText(number, BufferType.NORMAL);
                }
                int callType = Utils.getDefaultCallCard(activity, keyname);
                if (callType != PHONE_TYPE_NONE) {
                    placeCall(callType);
                } else {
                    if (mDigits != null) {
                        mDigits.getText().clear();
                    }
                    Intent intent = new Intent(ContactsUtils.TRANS_DIALPAD);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("phoneNumber", number);
                    startActivity(intent);
                }
            } else {
                Uri numberUri = Uri.fromParts("tel", number, null);
                Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, numberUri); //MOTO Dialer Code - IKHSS7-2554
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                MEDialer.onDial(activity, intent, MEDialer.DialFrom.SPEEDDIAL); // Motorola, FTR 36344,
                                                                           // Apr-19-2011, IKPIM-384
                activity.startActivity(intent);
            }
        } else {
            mSpeedDialAssignKey = keyname;
            SpeedDialAssignDialogFragment dlgFragment = SpeedDialAssignDialogFragment.newInstanceInter(keyname);
            dlgFragment.show(getFragmentManager(), "speeddial_assign_dialog");
        }
        return true;
    }

    // class for SpeedDialAssignDialog
    public static class SpeedDialAssignDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {
        private static final String TAG = "SpeedDialAssignDialogFragment";

        public static SpeedDialAssignDialogFragment newInstanceInter(int keyname) {
            final SpeedDialAssignDialogFragment fragment = new SpeedDialAssignDialogFragment();
            final Bundle args = new Bundle();
            args.putInt(SPEED_DIAL_ASSIGN_KEY, keyname);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            int speedDialAssignKey = getArguments().getInt(SPEED_DIAL_ASSIGN_KEY, -1);
            AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.speedDialAssignSpeedDial)
                .setMessage(activity.getString(R.string.speedDialAssignmentFromDialer, speedDialAssignKey))
                .setPositiveButton(R.string.ok, this)
                .setNegativeButton(R.string.cancelButton, null)
                .create();
            return dialog;
        }
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Intent intent = new Intent(Intent.ACTION_PICK, Phone.CONTENT_URI);
            intent.putExtra("INTENTEXTRA_BOOLEAN_SELECTMULTIPLE", false);
            Activity activity = getActivity();
            Fragment fragment = null;
            if(activity instanceof DialtactsActivity) {
                fragment = ((DialtactsActivity)activity).getFragmentAt(DialtactsActivity.TAB_INDEX_DIALER);
            } else if (activity instanceof DialpadActivity) {
                fragment = activity.getFragmentManager().findFragmentById(R.id.dialpad_fragment);
            } else {
                Log.w(TAG, "Can not matched activity type.");
            }
            if(fragment != null) {
                fragment.startActivityForResult(intent, PICK_CONTACT_FOR_SPEED_DIAL_REQ);
            }
        }
    }
    // MOTO Dialer Code - IKHSS6-626 End

    /**
    * @param intent the intent which launch this activity
    * @return true  if it's VT offhook or in VT ring
    */
    private boolean isInVTcall(){
        /* this part code can not suport in Dinara
        try {
            ITelephony mTelephony = ITelephony.Stub.asInterface(ServiceManager
                                .getService(TELEPHONY_SERVICE));
            if (mTelephony == null) {
                Log.w(TAG, "isInVTcall, mTelephony is null");
                return false;
            }

            if (mTelephony.isOffhook()) { // MO/MT active
                if (mTelephony.isVTRunning()) {
                    return true;
                }
            } else if (mTelephony.isRinging()) // MT Ringing {
                if (mTelephony.isVTRunning()) {
                    return true;
                }

        }catch (RemoteException e) {
            Log.e(TAG, "Failed to check VT call status", e);
        }
        }*/
        return false;
    }

    private void initChinaViews(View fragmentView) {
    	dialLocationLayout = (LinearLayout) fragmentView.findViewById(R.id.dial_location);
    	mCallLocation = (TextView) fragmentView.findViewById(R.id.number_location);
        mSearchMatchesTv = (TextView) fragmentView.findViewById(R.id.matches_textview);
        if (mLocationServiceManager == null) {
            mLocationServiceManager = new LocationServiceManager(getActivity().getApplicationContext(),mCallLocation);
        } else {
            mLocationServiceManager.setLocationView(mCallLocation);
        }
        mDigits.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        mDialpad = (View) fragmentView.findViewById(R.id.dialpad);

        callByCDMA = (View) fragmentView.findViewById(R.id.callByCDMA);
        callBtnDivider = fragmentView.findViewById(R.id.dual_call_btn_divider);
        callByCDMA.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                placeCall(PHONE_TYPE_CDMA);
            }
        });

        callByGSM = (View) fragmentView.findViewById(R.id.callByGSM);
        callByGSM.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                placeCall(PHONE_TYPE_GSM);
            }
        });

        mVoiceCallButton = fragmentView.findViewById(R.id.voicecall);
        mVoiceCallButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                placeCall(PHONE_TYPE_GSM);
            }
        });

        mVideoCallButton = fragmentView.findViewById(R.id.videocall);
        if (true == ContactsUtils.is_vt_enabled) {
            mVideoCallButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String number = mDigits.getText().toString();
                    if (!TextUtils.isEmpty(number)) {
                        number = PhoneNumberUtils.convertKeypadLettersToDigits(number);
                        number = PhoneNumberUtils.stripSeparators(number);
                        String vtnumber = "vtTel://"+number;
                        Intent mIntent = new Intent("com.motorola.videocall.OutGoingActivity.NEW_OUTGOING_VTCALL",
                                                        Uri.parse(vtnumber));
                        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(mIntent);
                        Log.v(TAG, "Start VT Call vtnumber = " + vtnumber);
                        mDigits.getText().clear();
                    }
                }
            });
            mVideoCallButton.setVisibility(View.VISIBLE);
        } else {
            mVideoCallButton.setVisibility(View.INVISIBLE);
        }

        mEmergencyButton = fragmentView.findViewById(R.id.callByEmergency);
        mEmergencyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                placeEmergencyCall();
            }
        });

        unhideDrawerBtn = (ImageButton) fragmentView.findViewById(R.id.imgbtn_unhide_slidedrawer);

        mTxtwatcher = new MyTextWatcher();
        mDigits.addTextChangedListener(mTxtwatcher);

        mLocTxtwatcher = new LocationTextWatcher();
        mCallLocation.addTextChangedListener(mLocTxtwatcher);

    }

    private void initChinaSmartDialer(View view) {
        dialerLayout = view.findViewById(R.id.dialer_layout);
        // below are all null in landscape mode
        emptyText = view.findViewById(R.id.empty);
        spaceHolder = view.findViewById(R.id.spaceHolder);
        if (mDrawer != null) {
            // in portrait mode
            final SlidingDrawer drawer = mDrawer;
            final DrawerManager drawerManager = new DrawerManager();
            drawer.setOnDrawerOpenListener(drawerManager);
            drawer.setOnDrawerCloseListener(drawerManager);
            /*Added for switchuitwo-350 begin*/
            if(!mNeedClosedDrawer) {
            	drawer.animateOpen();
            }
            /*Added for switchuitwo-350 end*/
        }
        if (null != unhideDrawerBtn) {
            unhideDrawerBtn.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction() == MotionEvent.ACTION_DOWN && !mDrawer.isOpened() && !mDrawer.isMoving()) {
                    	v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        showList(false);
                    }
                    return true;
                }
            });
    	}

        // Set the contactlist
        mListHasFocus = false;
        mListState = null;
        mList.setFocusable(true);
        mList.setOnCreateContextMenuListener(this);
        // there is no need to enable the listView's textfilter, coz MyTextWatcher does this
        mList.setTextFilterEnabled(false);

        // We manually save/restore the listview state
        mList.setSaveEnabled(false);
    }

    protected void maybeAddNumberFormatting() {
        if(!(PhoneNumberUtils.FORMAT_UNKNOWN == PhoneNumberUtils.getFormatTypeForLocale(Locale.getDefault())))
            mDigits.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
    }

    private int getLastCallType(String number) {
        int type = TelephonyManager.PHONE_TYPE_NONE;
        final Uri uri = Uri.withAppendedPath(Calls.CONTENT_FILTER_URI, number);
        final Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, CallLog.Calls.DEFAULT_SORT_ORDER);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(ContactsUtils.CallLog_NETWORK);
                if (column != -1) type = cursor.getInt(column);
            }
            cursor.close();
        }

        return type;
    }

    private void clearCallButtonHighlight() {
        if (!isDualMode) return;
        if (callByCDMA.isEnabled())
            callByCDMA.setSelected(false);
        if (callByGSM.isEnabled())
            callByGSM.setSelected(false);
    }

    private void highlightCallButton(String number) {
        clearCallButtonHighlight();

        // empty input
        if (TextUtils.isEmpty(number))
            return;

        // only emergency button or only one card not need highlight
        if (isDualMode && callByCDMA.isEnabled() && callByGSM.isEnabled()) {
            number = PhoneNumberUtils.convertKeypadLettersToDigits(number);
            number = PhoneNumberUtils.stripSeparators(number);
            int callType = getLastCallType(number);
            switch (callType) {
            case TelephonyManager.PHONE_TYPE_CDMA:
                callByCDMA.setSelected(true);
                break;
            case TelephonyManager.PHONE_TYPE_GSM:
                callByGSM.setSelected(true);
                break;
            default:
                break;
            }
        }
    }

    private void configLayOut(TextView tv, String curText) {
        if (tv == null) {
            Log.e(TAG, "get number location layout instance error!");
            return;
        }

        ViewGroup.LayoutParams lpar = (ViewGroup.LayoutParams) tv.getLayoutParams();
        if (lpar == null) {
            Log.e(TAG, "get number location LayoutParams error!");
            return;
        }

        /*float widthPixels;
        if (isPortrait) {
            widthPixels = getResources().getDisplayMetrics().widthPixels;
        } else {
            widthPixels = getResources().getDisplayMetrics().heightPixels;
        }
        lpar.width = (int) ((1f/3) * widthPixels);
        tv.setLayoutParams(lpar);
        // Set text size by default for text length measure rule used below
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, 25);
        final int textLength = (int) tv.getPaint().measureText(curText);
        if (textLength >= 135 && textLength <= 175) {
            lpar.width = (int) ((4f/9) * widthPixels);
            tv.setLayoutParams(lpar);
        } else if (textLength > 175) {
            lpar.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            tv.setLayoutParams(lpar);
        }
        // Set text size according widthPixels
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, (int) ((30f/540) * widthPixels));*/
    }

    void changeNumberLocation() {
        if (mCallLocation == null) {
            return;
        }
        if (dialpadChooserVisible()) {
            mCallLocation.setVisibility(View.GONE);
            return;
        }

        String mNumber = null;
        if (mDigits != null && mDigits.length() > 0) {
            mNumber = mDigits.getText().toString();
        }

        if (mLocationServiceManager != null) {
            mLocationServiceManager.showLocation(mNumber);
        }
    }

    void changeSearchingMatches(int matches) {
        if (mSearchMatchesTv == null) {
            return;
        }
        String inputStr = null;
        if (mDigits != null && mDigits.length() > 0) {
            inputStr = mDigits.getText().toString();
        }
        if (matches >1 && inputStr != null && inputStr.length() >= SmartDialerAdapter.START_QUERY_DIGITS_NUMBER && !dialpadChooserVisible()) {
            String curText = getString(R.string.smart_results, matches);
            Spannable styledName = new SpannableString(curText);
            ForegroundColorSpan redSpan = new ForegroundColorSpan(android.graphics.Color.rgb(98, 193, 255));
            StyleSpan boldSpan = new StyleSpan (Typeface.BOLD);
            styledName.setSpan(boldSpan, 0, String.valueOf(matches).length(), 0);
            styledName.setSpan(redSpan, 0, String.valueOf(matches).length(), 0);
            mSearchMatchesTv.setText(styledName);
            configLayOut(mSearchMatchesTv, curText);
            mSearchMatchesTv.setVisibility(View.VISIBLE);
        } else {
            mSearchMatchesTv.setText("");
            mSearchMatchesTv.setVisibility(View.GONE);
        }
        showResultsOrLocation();
    }

    private void showResultsOrLocation(){
        if (null != mCallLocation && null != mSearchMatchesTv
                && null != dialLocationLayout) {
            dialLocationLayout.setGravity(Gravity.CENTER_HORIZONTAL);
            ViewGroup.MarginLayoutParams searchParams = (ViewGroup.MarginLayoutParams) mSearchMatchesTv.getLayoutParams();
            ViewGroup.MarginLayoutParams locationParams = (ViewGroup.MarginLayoutParams) mCallLocation.getLayoutParams();
            searchParams.leftMargin = 0;
            locationParams.leftMargin = 0;
            searchParams.rightMargin = 0;
            locationParams.rightMargin = 0;
            mSearchMatchesTv.setLayoutParams(searchParams);
            mCallLocation.setLayoutParams(locationParams);
            if (mCallLocation.getVisibility() == View.VISIBLE
                    || mSearchMatchesTv.getVisibility() == View.VISIBLE) {
                dialLocationLayout.setVisibility(View.VISIBLE);
                View separate = dialLocationLayout.findViewById(R.id.matches_location_separate);
                if (null != separate) separate.setVisibility(View.VISIBLE);
                if (mCallLocation.getVisibility() == View.VISIBLE
                        && mSearchMatchesTv.getVisibility() == View.VISIBLE/*
                        && searchParams.width != ViewGroup.LayoutParams.WRAP_CONTENT
                        && locationParams.width != ViewGroup.LayoutParams.WRAP_CONTENT*/) {
                    locationParams.leftMargin = 2;
                    mCallLocation.setLayoutParams(locationParams);
                    final int locationWidth = locationParams.width;
                    final int searchWidth = searchParams.width;
                    searchParams.rightMargin = 8;
                    locationParams.leftMargin = 8;
                    if (isPortrait &&  locationWidth > searchWidth) {
                        searchParams.leftMargin = locationWidth - searchWidth;
                    } else if (isPortrait && locationWidth < searchWidth) {
                        locationParams.rightMargin = searchWidth - locationWidth;
                    }
                    mSearchMatchesTv.setLayoutParams(searchParams);
                    mCallLocation.setLayoutParams(locationParams);
                } else {
                    if (null != separate) separate.setVisibility(View.INVISIBLE);
                }
            } else {
                dialLocationLayout.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Get phone disable or enable infor to set button state.
     */
    private void setPhoneModeInfo() {
        isCDMAEnabled = ContactsUtils.isPhoneEnabled(PHONE_TYPE_CDMA);
        isGSMEnabled = ContactsUtils.isPhoneEnabled(PHONE_TYPE_GSM);
        TelephonyManager defaultMgr = TelephonyManager.getDefault();
        TelephonyManager secondMgr = null;
        if (isDualMode) {
            /* to-pass-build, Xinyu Liu/dcjf34 */
            secondMgr = null;//TelephonyManager.getDefault(false);
        }

        int defaultPhoneType = defaultMgr.getPhoneType();
        if (defaultPhoneType == PHONE_TYPE_CDMA) {
            mVoiceMailNumberCDMA = defaultMgr.getVoiceMailNumber();
            if (secondMgr != null) {
                mVoiceMailNumberGSM = secondMgr.getVoiceMailNumber();
            }
        } else if (defaultPhoneType == PHONE_TYPE_GSM) {
            mVoiceMailNumberGSM = defaultMgr.getVoiceMailNumber();
            if (secondMgr != null) {
                mVoiceMailNumberCDMA = secondMgr.getVoiceMailNumber();
            }
        } else
            return;
    }

    private void updateCallButton(){
        setPhoneModeInfo();
        if (!isGSMEnabled&&!isCDMAEnabled){
            callByCDMA.setVisibility(View.GONE);
            callBtnDivider.setVisibility(View.GONE);
            callByGSM.setVisibility(View.GONE);
            mDialButton.setVisibility(View.GONE);
            mVoiceCallButton.setVisibility(View.GONE);
            mEmergencyButton.setVisibility(View.VISIBLE);
        }else if ((isGSMEnabled&&!isCDMAEnabled)||(!isGSMEnabled&&isCDMAEnabled)){
            callByCDMA.setVisibility(View.GONE);
            callByGSM.setVisibility(View.GONE);
            callBtnDivider.setVisibility(View.GONE);
            mEmergencyButton.setVisibility(View.GONE);
            mDialButton.setVisibility(View.VISIBLE);
            if (isDualMode) {
                mVoiceCallButton.setVisibility(View.GONE);
                if (isGSMEnabled&&!isCDMAEnabled){
                    ((ImageButton)mDialButton).setImageResource(R.drawable.dial_btn_smart_g_src);
                } else if (!isGSMEnabled&&isCDMAEnabled){
                    ((ImageButton)mDialButton).setImageResource(R.drawable.dial_btn_smart_c_src);
                }
            } else {
                ((ImageButton)mDialButton).setImageResource(R.drawable.dial_btn_smart_single_src);
                if (isGSMEnabled&&!isCDMAEnabled){
                    if (true == ContactsUtils.is_vt_enabled) {
                    	mDialButton.setVisibility(View.INVISIBLE);
                        mVoiceCallButton.setVisibility(View.VISIBLE);
                    } else {
                        mVoiceCallButton.setVisibility(View.INVISIBLE);
                        mDialButton.setVisibility(View.VISIBLE);
                    }
                }
            }
        } else {
            callByCDMA.setVisibility(View.VISIBLE);
            callBtnDivider.setVisibility(View.VISIBLE);
            callByGSM.setVisibility(View.VISIBLE);
            mDialButton.setVisibility(View.GONE);
            mEmergencyButton.setVisibility(View.GONE);
            mVoiceCallButton.setVisibility(View.GONE);
            if (isGSMEnabled){
                callByGSM.setEnabled(true);
            } else {
                callByGSM.setEnabled(false);
                //callByGSM.setTextColor(getResources().getColor(R.color.button_disable));
            }
            if (isCDMAEnabled){
                callByCDMA.setEnabled(true);
            } else {
                callByCDMA.setEnabled(false);
                //callByCDMA.setTextColor(getResources().getColor(R.color.button_disable));
            }
        }
    }

	private void updatePhoneNumberFormat(){
		if(PhoneNumberUtils.FORMAT_UNKNOWN == PhoneNumberUtils.getFormatTypeForLocale(Locale.getDefault())){
			if(isLastTypeUnknown){
				return;
			}
			if(mDigits != null){
				CharSequence num = mDigits.getText();
				if(num != null){
					int position = mDigits.getSelectionStart();
					String newNum = PhoneNumberUtils.stripSeparators(num.toString());
					mDigits.setText(newNum);
					if( position >= newNum.length()){position = newNum.length();}
					mDigits.setSelection(position);
					}
			}
			isLastTypeUnknown = true;
		}else{
				isLastTypeUnknown = false;
		}
	}

    private void placeChinaCall () {
        if(mDigits == null) return;
        if (mIsAddCallMode && (TextUtils.isEmpty(mDigits.getText().toString()))) {
            // if we are adding a call from the InCallScreen and the phone
            // number entered is empty, we just close the dialer to expose
            // the InCallScreen under it.
            getActivity().finish();
        } else {
            // otherwise, we place the call.
            // placeCall();
            // Changes for Webtop support
            String dialString = mDigits.getText().toString();
            if (!TextUtils.isEmpty(dialString)) {
                dialString = PhoneNumberUtils.convertKeypadLettersToDigits(dialString);
                dialString = PhoneNumberUtils.stripSeparators(dialString);
            }
            Log.d(TAG, "onKeyUp, call number = "+dialString+", isCDMAEnabled = "+isCDMAEnabled+", isGSMEnabled ="+isGSMEnabled);
            if (!TextUtils.isEmpty(dialString)) {
                if (isCDMAEnabled && isGSMEnabled) {
                    Intent intent = new Intent(ContactsUtils.TRANS_DIALPAD);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("phoneNumber", dialString);
                    if (getActivity() instanceof DialtactsActivity) {
                        intent.putExtra(DialtactsActivity.EXTRA_CALL_ORIGIN,
                                        DialtactsActivity.CALL_ORIGIN_DIALTACTS);
                    }
                    startActivity(intent);
                } else if (isCDMAEnabled) {
                    placeCall(PHONE_TYPE_CDMA);
                } else if (isGSMEnabled) {
                    placeCall(PHONE_TYPE_GSM);
                } else {
                    placeEmergencyCall();
                }
            }
        }
    }

    void placeCall(int phoneType) {
        if(mDigits == null) return;
        String number = mDigits.getText().toString();
        if (TextUtils.isEmpty(number) || !TextUtils.isGraphic(number)) {
            if (!TextUtils.isEmpty(mLastNumberDialed)) {
                // Otherwise, pressing the Dial button without entering
                // any digits means "recall the last number dialed".
                setFormattedDigits(mLastNumberDialed, null);
                return;
            } else {
                // There is no number entered.
                playTone(ToneGenerator.TONE_PROP_NACK);
                return;
            }
        }
        if (isIpCall) {
            if (isDualMode) {
                if (phoneType == PHONE_TYPE_CDMA) {
                    number = ip_cdma + number;
            } else if (phoneType == PHONE_TYPE_GSM) {
                    number = ip_gsm + number;
                } else {
                Log.e(TAG, "placeCall: wrong phoneType.");
                }
            } else {
                if (TelephonyManager.getDefault().getPhoneType() == PHONE_TYPE_CDMA) {
                    number = ip_cdma + number;
                } else {
                    number = ip_gsm + number;
                }
            }
        }

        Intent intent;
        if (isIntRoamCallBackCall) {
            intent = new Intent("com.android.phone.InternationalRoamingCallback");
            intent.putExtra("phoneNumber", number);
            intent.putExtra(ContactsUtils.IntRoamCallBackCall, true);
            isIntRoamCallBackCall = false;
        } else {
            intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
            Uri.fromParts("tel", number, null));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(ContactsUtils.CALLED_BY, "BY_DIALER");
            if (isIntRoamCall) {
                intent.putExtra(ContactsUtils.CALLED_BY, ContactsUtils.DIAL_BY_INTL_ROAMING_CALL);
                isIntRoamCall = false;
            }
        }
        intent.putExtra("phone", phoneType == PHONE_TYPE_GSM ? "GSM" : "CDMA" );
        if (isIpCall) {
            if (isDualMode) {
                if (phoneType == PHONE_TYPE_CDMA) {
                    intent.putExtra("ip_prefix", ip_cdma);
                } else if (phoneType == PHONE_TYPE_GSM) {
                    intent.putExtra("ip_prefix", ip_gsm);
                } else {
                    Log.e(TAG, "placeCall: wrong phoneType.");
                }
            } else {
                if (TelephonyManager.getDefault().getPhoneType() == PHONE_TYPE_CDMA) {
                    intent.putExtra("ip_prefix", ip_cdma);
                } else {
                    intent.putExtra("ip_prefix", ip_gsm);
                }
            }
            isIpCall = false; // reset isIpCall
        }
        try {
            if (getActivity() instanceof DialtactsActivity) {
                intent.putExtra(DialtactsActivity.EXTRA_CALL_ORIGIN,
                                DialtactsActivity.CALL_ORIGIN_DIALTACTS);
            }
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            // don't crash if ip call is not supported.
        }
        clearCallButtonHighlight();
        mDigits.getText().clear();
        // Don't finish, so after call, will back to dialer
        // Avoid tel:\\number show again after call
        final Intent subintent = new Intent(Intent.ACTION_MAIN);
        subintent.setClassName("com.android.contacts","com.android.contacts.ContactsLaunchActivity");
        if (getActivity().isChild()) {
            getActivity().getParent().setIntent(subintent);
        } else {
            getActivity().setIntent(subintent);
        }
        // finish();
    }

    void placeEmergencyCall(){
        final String number = mDigits.getText().toString();
        if (TextUtils.isEmpty(number) || !TextUtils.isGraphic(number)) {
            if (!TextUtils.isEmpty(mLastNumberDialed)) {
                setFormattedDigits(mLastNumberDialed, null);
            } else {
                playTone(ToneGenerator.TONE_PROP_NACK);
            }
            return;
        }
        if (PhoneNumberUtils.isEmergencyNumber(number)||isEmergencyNumber(number)) {
            // place the call if it is a valid number
        	/*Added for switchuitwo-465 begin*/
        	mDigits.getText().delete(0, mDigits.getText().length());
        	/*Added for switchuitwo-465 end*/
            Intent intent = new Intent(Intent.ACTION_CALL_EMERGENCY);
            intent.setData(Uri.fromParts("tel", number, null));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            // Don't finish, so after call, will back to dialer
            // finish();
        } else {
            // erase the number and throw up an alert dialog.
            mDigits.getText().delete(0, mDigits.getText().length());
            /*displayErrorBadNumber(number);*/
            final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED,
                      Uri.fromParts("tel", number, null));
            startActivity(intent);
        }
    }

    public boolean isEmergencyNumber(String number) {
        if (number != null){
            if (ContactsUtils.getCarrierName(getActivity()).equals(ContactsUtils.Carrier_CMCC)) {
                return (number.equals("112") || number.equals("911")
                    || number.equals("110") || number.equals("999")
                    || number.equals("000") || number.equals("08")
                    || number.equals("118") || number.equals("119"));
            } else if (ContactsUtils.getCarrierName(getActivity()).equals(ContactsUtils.Carrier_CT)) {
                return (number.equals("112") || number.equals("911")
                    || number.equals("110") || number.equals("999")
                    || number.equals("120") || number.equals("119"));
            } else // For CU
                return false;
        }else{
            return false;
        }
    }

    /**
     * display the alert dialog
     */
    /*private void displayErrorBadNumber (String number) {
        // construct error string
        CharSequence errMsg;
        if (!TextUtils.isEmpty(number)) {
            errMsg = getString(R.string.dial_emergency_error, number);
        } else {
            errMsg = getText(R.string.dial_emergency_empty_error);
        }

        // construct dialog
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setTitle(R.string.callByEmergency);
        b.setMessage(errMsg);
        b.setPositiveButton(android.R.string.ok, null);
        b.setCancelable(true);

        // show the dialog
        AlertDialog dialog = b.create();
        dialog.getWindow()
                .addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        dialog.show();
    }*/

    private class CallButtonBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                mHandler.removeMessages(UPDATE_CALLBUTTON);
                Message msg = Message.obtain(mHandler,UPDATE_CALLBUTTON);
                mHandler.sendMessageDelayed(msg,UPDATE_CALLBUTTON_DELAY);
            }
        }
    }

    private final class MyTextWatcher implements TextWatcher {

        public MyTextWatcher() {
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence input, int start, int before, int changeCount) {
            changeNumberLocation();
        }

        public void afterTextChanged(Editable input) {
            adjustDigitsTextSize(input);
        }
    }

    private final class LocationTextWatcher implements TextWatcher {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence input, int start, int before, int changeCount) {
            CharSequence loc = input;
            if (loc != null && loc.length() > 0 && mDigits != null && mDigits.length() > 0) {
                configLayOut(mCallLocation, loc.toString());
                mCallLocation.setVisibility(View.VISIBLE);
            } else {
                mCallLocation.setVisibility(View.GONE);
            }
            showResultsOrLocation();
        }

        public void afterTextChanged(Editable input) {
        }
    }

    private class DrawerManager implements SlidingDrawer.OnDrawerOpenListener,
            SlidingDrawer.OnDrawerCloseListener {
        private boolean mOpen;

        public void onDrawerOpened() {
            if (null != mDrawer && !mOpen) {
                mOpen = true;
                if (null != mDelete)
                    mDelete.setVisibility(View.VISIBLE);
                if (null != unhideDrawerBtn)
                    unhideDrawerBtn.setVisibility(View.GONE);
                if (null != getActivity() && null != mList) {
                    int paddingBottom = getResources().getDimensionPixelSize(R.dimen.dialer_digits_height)
                            + getResources().getDimensionPixelSize(R.dimen.dialer_drawer_body_height);
                    mList.setPadding(0, 0, 0, paddingBottom);
                }
            }
        }

        public void onDrawerClosed() {
            if (null != mDrawer && mOpen) {
                mOpen = false;
                if (null != mDelete)
                    mDelete.setVisibility(View.GONE);
                if (null != unhideDrawerBtn)
                    unhideDrawerBtn.setVisibility(View.VISIBLE);
                if (null != getActivity() && null != mList) {
                    int paddingBottom = getResources().getDimensionPixelSize(R.dimen.dialer_digits_height);
                    mList.setPadding(0, 0, 0, paddingBottom);
                }
            }
        }

    }

    public void restoreListState() {
        // Now that the cursor is populated again, it's possible to restore the list state
        if (mListState != null && mList != null) {
            mList.onRestoreInstanceState(mListState);
            if (mListHasFocus) {
                mList.requestFocus();
            }
            mListHasFocus = false;
            mListState = null;
        }
    }

    private void adjustDigitsTextSize (Editable input) {
        if (input == null) return;
        mDigits.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        if (!TextUtils.isEmpty(input)) {
            mDigits.setTextSize(getResources().getDimensionPixelSize(R.dimen.largeDialerDigitsSize));
            //final float editWidth = mDigits.getWidth() - mDigits.getPaddingLeft() - mDigits.getPaddingRight();
            final float editWidth = getResources().getDimensionPixelSize(R.dimen.dialer_digits_text_width);
            final float textLength = mDigits.getPaint().measureText(input.toString());
            if (textLength > editWidth) {
                mDigits.setTextSize(getResources().getDimensionPixelSize(R.dimen.smallDialerDigitsSize));
                mDigits.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
            }
        } else {
            mDigits.setTextSize(getResources().getDimensionPixelSize(R.dimen.hintDialerDigitsSize));
        }
    }
}
