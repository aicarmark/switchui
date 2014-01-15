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

package com.android.contacts;

import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.calllog.CallTypeHelper;
import com.android.contacts.calllog.PhoneNumberHelper;
import com.android.contacts.format.FormatUtils;
import com.android.contacts.EcidContact;
import com.motorola.android.telephony.PhoneModeManager;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.SystemProperties;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.provider.CallLog.Calls;

import android.content.Context; // Motorola, w21071, 2011-12-21, IKCBS-2736 context added.

/**
 * Helper class to fill in the views in {@link PhoneCallDetailsViews}.
 */
public class PhoneCallDetailsHelper {
    /** The maximum number of icons will be shown to represent the call types in a group. */
    private static final int MAX_CALL_TYPE_ICONS = 3;
    private static final int MAX_NETWORK_TYPE_ICONS = 1;

    private final Resources mResources;
    /** The injected current time in milliseconds since the epoch. Used only by tests. */
    private Long mCurrentTimeMillisForTest;
    // Helper classes.
    private final CallTypeHelper mCallTypeHelper;
    private final PhoneNumberHelper mPhoneNumberHelper;
    //MOTO Dialer Code Start
    //IKMAIN-10554, a19591, 34425 begin
    private int mCnapFlex;
    static final int CNAP_OFF = 0;
    static final int CNAP_PRIORITY = 1;
    static final int CNAP_CONTACT_PRIORITY = 2;
    //IKMAIN-10554 end
    private static final String TAG = "PhoneCallDetailsHelper";
    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End
    //MOTO Dialer Code End

    // BEGIN Motorola, w21071, 2011-12-21, IKCBS-2736
    private boolean mCallTimeDisplayByDateTimeFormat = false;
    private Context mContext = null;
    // END IKCBS-2736

    // IKHSS6-8052 FID:34118 Enhanced CityID BEGIN
    private boolean mEcidEnabled;
    // IKHSS6-8052 FID:34118 Enhanced CityID END

    /**
     * Creates a new instance of the helper.
     * <p>
     * Generally you should have a single instance of this helper in any context.
     *
     * @param resources used to look up strings
     */
    public PhoneCallDetailsHelper(Resources resources, CallTypeHelper callTypeHelper,
            PhoneNumberHelper phoneNumberHelper, Context context) { // Motorola, w21071, 2011-12-21, IKCBS-2736 context added.
        mResources = resources;
        mCallTypeHelper = callTypeHelper;
        mPhoneNumberHelper = phoneNumberHelper;
        //MOTO Dialer Code Start
        //IKMAIN-10554, a19591, 34425 CNAP feature
        //Read CNAP feature flex
        mCnapFlex = resources.getInteger(R.integer.ftr_cnap);
        if (VDBG) log("mCnapFlex = " + mCnapFlex);
        //MOTO Dialer Code End

        // BEGIN Motorola, w21071, 2011-12-21, IKCBS-2736
        // FID 36876 Change the format of call time on recent calls list
        mContext = context;
        mCallTimeDisplayByDateTimeFormat = PhoneModeManager.IsCallTimeDisplayByDateTimeFormat();
                //resources.getBoolean(R.bool.ftr_36876_call_time_display_by_date_time_format);
        // END IKCBS-2736

        // IKHSS6-8052 FID:34118 Enhanced CityID BEGIN
        mEcidEnabled = EcidContact.isECIDAvailable(mContext);
        // IKHSS6-8052 FID:34118 Enhanced CityID END
    }

    /** Fills the call details views with content. */
    public void setPhoneCallDetails(PhoneCallDetailsViews views, PhoneCallDetails details,
            boolean isHighlighted, boolean isVoicemail, boolean isEmergency) {
        // Display up to a given number of icons.
        views.callTypeIcons.clear();
        int count = details.callTypes.length;
        for (int index = 0; index < count && index < MAX_CALL_TYPE_ICONS; ++index) {
            views.callTypeIcons.add(details.callTypes[index]);
        }
        views.callTypeIcons.setVisibility(View.VISIBLE);

        // Display up to a given number of network type icons.
        views.networkTypeIcons.clear();
        int netCount = details.networkTypes.length;
        for (int index = 0; index < MAX_NETWORK_TYPE_ICONS && index < netCount; ++index) {
            views.networkTypeIcons.add(details.networkTypes[index]);
        }
        views.networkTypeIcons.setVisibility(View.VISIBLE);

        // Show the total call count only if there are more than the maximum number of icons.
        final Integer callCount;
        if (count > MAX_CALL_TYPE_ICONS) {
            callCount = count;
        } else {
            callCount = null;
        }
        // The color to highlight the count and date in, if any. This is based on the first call.
        Integer highlightColor =
                isHighlighted ? mCallTypeHelper.getHighlightedColor(details.callTypes[0]) : null;

        CharSequence dateText =
            // BEGIN Motorola, w21071, 2011-12-21, IKCBS-2736
            // FID 36876 Change the format of call time on recent calls list
            // from the relative time format to the date and time format.
            (mCallTimeDisplayByDateTimeFormat == true && mContext != null) ?
            DateUtils.formatDateTime(mContext, details.date,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME) :
            // END IKCBS-2736
            // The date of this call, relative to the current time.
            DateUtils.getRelativeTimeSpanString(details.date,
                    getCurrentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);

        // Set the call count and date.
        setCallCountAndDate(views, callCount, dateText, highlightColor);
        
        //MOTO Dialer Code Start
        CharSequence displayName = details.name;

        // IKHSS6-8052 FID:34118 Enhanced CityID BEGIN
        boolean isEcidDisplay = false;
        if ( mEcidEnabled && TextUtils.isEmpty(displayName))
        {
            // check if name is empty, if so, replace displayed name.
            // TODO: Currently, pass call type default as incoming. Because ECID
            // will not lookup outgoing types, if the last call for this number
            // was outgoing, we'll end up with no information.
            String ecidInfoName = EcidContact.getContactName( details.number.toString(), Calls.INCOMING_TYPE );

            if (!TextUtils.isEmpty(ecidInfoName)) {
                displayName = ecidInfoName;
                isEcidDisplay = true;
                // Note that ECID name is being checked before CNAP, to give CNAP priority to replace it
            }
        } // IKHSS6-8052 FID:34118 Enhanced CityID END

        if (isVoicemail) {// MOT Calling code - IKSTABLEFOURV-1933
            displayName = mResources.getString(R.string.voicemail);
        }

        if (isEmergency) {
            displayName = mResources.getString(
                    R.string.emergency_call_dialog_number_for_display);
        }

        //IKMAIN-10554, a19591, 34425 CNAP feature begin
        boolean isCnapDisplay = false;
        if (isCnapDisplay(details.cnapName, details.name.toString())) {
            displayName = details.cnapName;
            isCnapDisplay = true;
        }
        //IKMAIN-10554 end
        //MOTO Dialer Code End

        CharSequence numberFormattedLabel = null;
        // Only show a label if the number is shown and it is not a SIP address.
        if (!TextUtils.isEmpty(details.number)
                && !PhoneNumberUtils.isUriNumber(details.number.toString())
                && !isCnapDisplay && !isEmergency && !isVoicemail && !isEcidDisplay) {  //MOTO Dialer Code
            numberFormattedLabel = Phone.getTypeLabel(mResources, details.numberType,
                    details.numberLabel);
        }

        final CharSequence nameText;
        final CharSequence numberText;
        final CharSequence displayNumber =
            mPhoneNumberHelper.getDisplayNumber(details.number, details.formattedNumber);
        if (TextUtils.isEmpty(displayName)) { //MOTO Dialer Code
            nameText = displayNumber;
//MOT Dialer Comment: cityid has its own view, so do not reuse number view
//            if (TextUtils.isEmpty(details.geocode)
//                    || mPhoneNumberHelper.isVoicemailNumber(details.number)) {
//                numberText = mResources.getString(R.string.call_log_empty_gecode);
//            } else {
//                numberText = details.geocode;
//            }
            numberText = null; // MOT Dialer Code
        } else {
            nameText = displayName; //MOTO Dialer Code
            if (numberFormattedLabel != null) {
                numberText = FormatUtils.applyStyleToSpan(Typeface.BOLD,
                        numberFormattedLabel + " " + displayNumber, 0,
                        numberFormattedLabel.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                numberText = displayNumber;
            }
        }

        //MOT Dialer Start
        final CharSequence cityidText;
        if (TextUtils.isEmpty(details.geocode)) {
            cityidText = null;
        } else {
            cityidText = details.geocode;
        }
        //MOT Dialer End
        views.nameView.setText(nameText);
        views.numberView.setText(numberText);
        views.cityidView.setText(cityidText); //MOT Dialer Code
    }

    /** Sets the text of the header view for the details page of a phone call. */
    public void setCallDetailsHeader(TextView nameView, PhoneCallDetails details, boolean isVoicemail, boolean isEmergency) { //MOT Dialer Code
        final CharSequence nameText;
        final CharSequence displayNumber =
                mPhoneNumberHelper.getDisplayNumber(details.number,
                        mResources.getString(R.string.recentCalls_addToContact));
        if (TextUtils.isEmpty(details.name)) {
            //MOT Dialer Start
            if (isVoicemail) {
                nameText = mResources.getString(R.string.voicemail);
            } else if (isEmergency) {
                nameText = mResources.getString(
                    R.string.emergency_call_dialog_number_for_display);
            // IKHSS6-8052 FID:34118 Enhanced CityID BEGIN
            } else if (mEcidEnabled) {
                // TODO: Currently, pass call type default as incoming. Because ECID
                // will not lookup outgoing types, if the last call for this number
                // was outgoing, we'll get no information.
                String ecidName = EcidContact.getContactName( details.number.toString(), Calls.INCOMING_TYPE );

                if (!TextUtils.isEmpty(ecidName)) {
                    nameText = ecidName;
                } else {
                    nameText = displayNumber;
                } // IKHSS6-8052 FID:34118 Enhanced CityID END
            } else {
                nameText = displayNumber;
            }
            //MOT Dialer End
        } else {
            nameText = details.name;
        }

        nameView.setText(nameText);
    }

    public void setCurrentTimeForTest(long currentTimeMillis) {
        mCurrentTimeMillisForTest = currentTimeMillis;
    }

    /**
     * Returns the current time in milliseconds since the epoch.
     * <p>
     * It can be injected in tests using {@link #setCurrentTimeForTest(long)}.
     */
    private long getCurrentTimeMillis() {
        if (mCurrentTimeMillisForTest == null) {
            return System.currentTimeMillis();
        } else {
            return mCurrentTimeMillisForTest;
        }
    }

    /** Sets the call count and date. */
    private void setCallCountAndDate(PhoneCallDetailsViews views, Integer callCount,
            CharSequence dateText, Integer highlightColor) {
        // Set the count (if present) and the date.
        // MOT Dialer Start
        final CharSequence callDateText;
        CharSequence callCountText = null;
        if (callCount != null) {
            callDateText = dateText;
            callCountText = mResources.getString(
                    R.string.call_log_item_count, callCount.intValue());
        } else {
            callDateText = dateText;
        }

        // Apply the highlight color if present.
        final CharSequence formattedDateText;
        CharSequence formattedCountText = null;
        if (highlightColor != null) {
            formattedDateText = addBoldAndColor(callDateText, highlightColor);
            if(callCountText != null) formattedCountText= addBoldAndColor(callCountText, highlightColor);
        } else {
            formattedDateText = callDateText;
            formattedCountText= callCountText;
        }
        views.callCountView.setText(formattedCountText);
        views.callTypeAndDate.setText(formattedDateText);
        //MOT Dialer End
    }

    /** Creates a SpannableString for the given text which is bold and in the given color. */
    private CharSequence addBoldAndColor(CharSequence text, int color) {
        int flags = Spanned.SPAN_INCLUSIVE_INCLUSIVE;
        SpannableString result = new SpannableString(text);
        result.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), flags);
        result.setSpan(new ForegroundColorSpan(color), 0, text.length(), flags);
        return result;
    }

    //IKMAIN-10554, a19591, 34425 CNAP feature begin
    /*
     * To indicate if RC list should display the CNAP name.
     * This refer to the mCnapFlex , the CNAP name,
     * and the contact name.
     * incoming call.
     * case 1. CNAP name take priority, cnap name not null, display cnap name
     *         otherwise contact name , if both null ,display the number only
     * case 2. Contact name take priority, contact name not null, display contact name
     *          otherwise cnap name , if both null ,display the number only
     */
     private boolean isCnapDisplay( String cnapName , String contactName ) {
         boolean cnapDislpaly = false;
         if (((mCnapFlex == CNAP_PRIORITY) && (!TextUtils.isEmpty(cnapName)))
               ||((mCnapFlex == CNAP_CONTACT_PRIORITY) && (TextUtils.isEmpty(contactName)) && (!TextUtils.isEmpty(cnapName)))) {
             cnapDislpaly = true;
         }
         if(DBG) log("isCnapDisplay  cnapDislpaly:" + cnapDislpaly + " mCnapFlex:" + mCnapFlex +
                 "cnapName:" + cnapName + " contactName:" + contactName);
         return cnapDislpaly ;
    }
    //IKMAIN-10554, end

    static void log(String msg) {
        Log.d(TAG, " -> " + msg);
    }
}
