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

import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.R;
import com.android.contacts.util.UriUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

//MOTO MOD BEGIN IKHSS7-2038
import com.motorola.internal.telephony.PhoneNumberUtilsExt;
//MOTO MOD END IKHSS7-2038

/**
 * Utility class to look up the contact information for a given number.
 */
public class ContactInfoHelper {
    private final Context mContext;
    private final String mCurrentCountryIso;
    
    private static final String TAG = "ContactInfoHelper";

    private final String[] ADDRESS_BOOK_COL_NAMES = new String[]{"name", "number"}; // MOT Calling code - FID 33664
    public static final int DEFAULT_PHOTOID = 0;

    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End

    public ContactInfoHelper(Context context, String currentCountryIso) {
        mContext = context;
        mCurrentCountryIso = currentCountryIso;
    }

    /**
     * Returns the contact information for the given number.
     * <p>
     * If the number does not match any contact, returns a contact info containing only the number
     * and the formatted number.
     * <p>
     * If an error occurs during the lookup, it returns null.
     *
     * @param number the number to look up
     * @param countryIso the country associated with this number
     */
    public ContactInfo lookupNumber(String number, String countryIso, String convertNumber) {
        final ContactInfo info;

        // Determine the contact info.
        if (PhoneNumberUtils.isUriNumber(number)) {
            // This "number" is really a SIP address.
            ContactInfo sipInfo = queryContactInfoForSipAddress(number);
            if (sipInfo == null || sipInfo == ContactInfo.EMPTY) {
                // Check whether the "username" part of the SIP address is
                // actually the phone number of a contact.
                String username = PhoneNumberUtils.getUsernameFromUriNumber(number);
                if (PhoneNumberUtils.isGlobalPhoneNumber(username)) {
                    sipInfo = queryContactInfoForPhoneNumber(username, countryIso, username);
                }
            }
            info = sipInfo;
        } else {
            // Look for a contact that has the given phone number.
            ContactInfo phoneInfo = queryContactInfoForPhoneNumber(number, countryIso, convertNumber);

            if (phoneInfo == null || phoneInfo == ContactInfo.EMPTY) {
                // Check whether the phone number has been saved as an "Internet call" number.
                phoneInfo = queryContactInfoForSipAddress(number);
            }
            info = phoneInfo;
        }

        final ContactInfo updatedInfo;
        if (info == null) {
            // The lookup failed.
            if (VDBG) log("lookupNumber failed");
            updatedInfo = null;
        } else {
            // If we did not find a matching contact, generate an empty contact info for the number.
            if (info == ContactInfo.EMPTY) {
                // Did not find a matching contact.
                updatedInfo = new ContactInfo();
                updatedInfo.number = number;
                updatedInfo.formattedNumber = formatPhoneNumber(number, null, countryIso, convertNumber);
            } else {
                updatedInfo = info;
            }
        }
        return updatedInfo;
    }

    /**
     * Looks up a contact using the given URI.
     * <p>
     * It returns null if an error occurs, {@link ContactInfo#EMPTY} if no matching contact is
     * found, or the {@link ContactInfo} for the given contact.
     * <p>
     * The {@link ContactInfo#formattedNumber} field is always set to {@code null} in the returned
     * value.
     */
    private ContactInfo lookupContactFromUri(Uri uri) {
        final ContactInfo info;
        Cursor phonesCursor =
                mContext.getContentResolver().query(
                        uri, PhoneQuery._PROJECTION, null, null, null);

        if (phonesCursor != null) {
            try {
                if (phonesCursor.moveToFirst()) {
                    info = new ContactInfo();
                    long contactId = phonesCursor.getLong(PhoneQuery.PERSON_ID);
                    String lookupKey = phonesCursor.getString(PhoneQuery.LOOKUP_KEY);
                    info.lookupUri = Contacts.getLookupUri(contactId, lookupKey);
                    info.name = phonesCursor.getString(PhoneQuery.NAME);
                    info.type = phonesCursor.getInt(PhoneQuery.PHONE_TYPE);
                    info.label = phonesCursor.getString(PhoneQuery.LABEL);
                    info.number = phonesCursor.getString(PhoneQuery.MATCHED_NUMBER);
                    info.normalizedNumber = phonesCursor.getString(PhoneQuery.NORMALIZED_NUMBER);
                    if (VDBG) log("PhoneQuery.MATCHED_NUMBER = " + info.number + " PhoneQuery.NORMALIZED_NUMBER = " + info.normalizedNumber);
                    info.photoId = phonesCursor.getLong(PhoneQuery.PHOTO_ID);
                    info.photoUri =
                            UriUtils.parseUriOrNull(phonesCursor.getString(PhoneQuery.PHOTO_URI));
                    info.formattedNumber = null;
                } else {
                    info = ContactInfo.EMPTY;
                }
            } finally {
                phonesCursor.close();
            }
        } else {
            // Failed to fetch the data, ignore this request.
            info = null;
        }
        return info;
    }

    /**
     * Determines the contact information for the given SIP address.
     * <p>
     * It returns the contact info if found.
     * <p>
     * If no contact corresponds to the given SIP address, returns {@link ContactInfo#EMPTY}.
     * <p>
     * If the lookup fails for some other reason, it returns null.
     */
    private ContactInfo queryContactInfoForSipAddress(String sipAddress) {
        final ContactInfo info;

        // "contactNumber" is a SIP address, so use the PhoneLookup table with the SIP parameter.
        Uri.Builder uriBuilder = PhoneLookup.CONTENT_FILTER_URI.buildUpon();
        uriBuilder.appendPath(Uri.encode(sipAddress));
        uriBuilder.appendQueryParameter(PhoneLookup.QUERY_PARAMETER_SIP_ADDRESS, "1");
        return lookupContactFromUri(uriBuilder.build());
    }

    /**
     * Determines the contact information for the given phone number.
     * <p>
     * It returns the contact info if found.
     * <p>
     * If no contact corresponds to the given phone number, returns {@link ContactInfo#EMPTY}.
     * <p>
     * If the lookup fails for some other reason, it returns null.
     */
    private ContactInfo queryContactInfoForPhoneNumber(String number, String countryIso, String convertNumber) {
        if (VDBG) log("queryContactInfoForPhoneNumber ");
        if (VDBG) log("number = " + number + " countryIso = " + countryIso);
        String contactNumber = number;
        if (!TextUtils.isEmpty(countryIso)) {
            // Normalize the number: this is needed because the PhoneLookup query below does not
            // accept a country code as an input.
            String numberE164 = PhoneNumberUtils.formatNumberToE164(number, countryIso);
            if (VDBG) log("numberE164 = " + numberE164);
            if (!TextUtils.isEmpty(numberE164)) {
                // Only use it if the number could be formatted to E164.
                contactNumber = numberE164;
            }
        }
        
        // The "contactNumber" is a regular phone number, so use the PhoneLookup table.
        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(contactNumber));
        ContactInfo info = lookupContactFromUri(uri);
    
        if (info == ContactInfo.EMPTY) { //contact DB not matched
        	  info = lookupFDNFromNumber(number);
        }
        if (info != null && info != ContactInfo.EMPTY) {
            info.formattedNumber = formatPhoneNumber(number, TextUtils.isEmpty(info.normalizedNumber)?null:info.normalizedNumber, //MOTO Dialer Code IKHSS7-2091
                                                      countryIso, convertNumber);
        }
        
        if (VDBG) {
            if (info != null) {
               log("queryContactInfoForPhoneNumber: " + number);
               log("--------------->info lookupUri " + info.lookupUri);
               log("--------------->info name " + info.name);
               log("--------------->info type " + info.type);
               log("--------------->info label " + info.label);
               log("--------------->info number " + info.number);
               log("--------------->info normalizedNumber " + info.normalizedNumber);
               log("--------------->info photoUri " + info.photoUri);
               log("--------------->info photoId " + info.photoId);
               log("--------------->info formattedNumber " + info.formattedNumber);
            }
        }
        
        return info;
    }
    
    //MOTO Dialer Code Start
    private ContactInfo lookupFDNFromNumber(String number) {
        boolean fdnSdnLookup = mContext.getResources().getBoolean(R.bool.fdn_sdn_lookup);
        ContactInfo info = ContactInfo.EMPTY;

        if (fdnSdnLookup) {
            if (VDBG) log("lookupFDNFromNumber ");
             /* Mot Calling Code - FID 33664 ADN/SDN/FDN enhancements to show in Call logs */
            ContentResolver contentResolver = mContext.getContentResolver();
            Cursor FdnCursor = contentResolver.query(
                    Uri.parse("content://icc/fdn"),
                    ADDRESS_BOOK_COL_NAMES, null, null, null);
            Cursor SdnCursor = contentResolver.query(
                    Uri.parse("content://icc/sdn"),
                    ADDRESS_BOOK_COL_NAMES, null, null, null);
            Cursor AdnCursor = contentResolver.query(
                    Uri.parse("content://icc/adn"),
                    ADDRESS_BOOK_COL_NAMES, null, null, null);
                
            Cursor totalCursor = new MergeCursor(new Cursor[]{FdnCursor,
                        SdnCursor, AdnCursor});
        
            if (totalCursor != null) {
                if (totalCursor.moveToFirst()) {
                    ContactInfo tempInfo = null;
                    while (!totalCursor.isAfterLast()) {
                          //FDN/SDN/ADN do not store E164-format number(Contact store it in NORMALIZED_NUMBER field), so can not use contactNumber to do match.
                        if (PhoneNumberUtils.compare(number,
                                totalCursor.getString(totalCursor.getColumnIndexOrThrow(ADDRESS_BOOK_COL_NAMES[1])))) {
                            tempInfo = new ContactInfo();
                            tempInfo.name = totalCursor.getString(totalCursor.getColumnIndexOrThrow(ADDRESS_BOOK_COL_NAMES[0]));
                            // Inform list to update this item, if in view
                            tempInfo.number = number; // MOTO A vsood1 bug 12803
                            tempInfo.photoId = DEFAULT_PHOTOID;
                            tempInfo.photoUri = null;//Mot add photo
                            tempInfo.formattedNumber = null;
                            break;
                        }
                        totalCursor.moveToNext();
                    }
                    if (totalCursor.isAfterLast()) {
                          // Not find matched contact in FDN/SDN/AND list.
                          tempInfo = ContactInfo.EMPTY;
                    }
                    info = tempInfo;
                } else {
                    info = ContactInfo.EMPTY;
                }
                totalCursor.close();
            } else {
                // Failed to fetch FDN/ADN/SDN data, ignore this request. //TBD, need test
                info = null;
            }
         }
         return info;
    }
    //MOTO Dialer Code Start End

    /**
     * Format the given phone number
     *
     * @param number the number to be formatted.
     * @param normalizedNumber the normalized number of the given number.
     * @param countryIso the ISO 3166-1 two letters country code, the country's
     *        convention will be used to format the number if the normalized
     *        phone is null.
     *
     * @return the formatted number, or the given number if it was formatted.
     */
    private String formatPhoneNumber(String number, String normalizedNumber,
            String countryIso, String convertNumber) {
        if (TextUtils.isEmpty(number)) {
            return "";
        }
        // If "number" is really a SIP address, don't try to do any formatting at all.
        if (PhoneNumberUtils.isUriNumber(number)) {
            return number;
        }
        if (TextUtils.isEmpty(countryIso)) {
            countryIso = mCurrentCountryIso;
        }
        if (!TextUtils.equals(number, convertNumber) && !TextUtils.isEmpty(convertNumber)) {
            //MOT MOD BEGIN IKHSS7-2038
            //Changed API to support New API PhoneNumberUtilsExt
            return PhoneNumberUtilsExt.formatNumber(mContext, convertNumber, normalizedNumber, countryIso);
        } else {
            return PhoneNumberUtilsExt.formatNumber(mContext, number, normalizedNumber, countryIso);
            //MOT MOD END IKHSS7-2038
        }
    }

    static void log(String msg) {
        Log.d(TAG, " -> " + msg);
    }
}
