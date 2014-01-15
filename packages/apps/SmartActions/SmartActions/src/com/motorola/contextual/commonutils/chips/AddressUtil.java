/*
 * @(#)AddressUtil.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/02/21  NA                  Initial version
 *
 */

package com.motorola.contextual.commonutils.chips;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;

import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.commonutils.*;

/**
 * This class implements utility functions used to perform address related operations <code><pre>
 *
 * CLASS:
 *
 * RESPONSIBILITIES:
 *     This class implements utility functions used to perform address related operations
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */


public class AddressUtil implements Constants {

    private static final String TAG = AddressUtil.class.getSimpleName();

    public static final int TYPE_NAME = 0;
    public static final int TYPE_NUMBER = 1;
    public static final int TYPE_KNOWN_FLAG = 2;

    public static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]*");
    public static final String SPECIAL_CHAR = "[\\)\\(+. -]";
    public static final String INVALID_CHARS_FOR_RAW_NUMBERS = "[\\)\\(. -]";

    /**
     * Method to check if input string is a valid phone number
     * @param str Input string
     * @return true if the string is a valid phone number, false otherwise
     */
    public static boolean isMessagableNumber(String str) {
        return isNumber(cleanUpPhoneNumber(str, SPECIAL_CHAR));
    }

    /**
     * Removes all valid non-number characters allowed in a phone number
     * @param str Input number
     * @param regularExpression Chars to replace
     * @return Cleaned up number
     */
    public static String cleanUpPhoneNumber(String str, String regularExpression)
    {
        if(StringUtils.isEmpty(str))
            return StringUtils.EMPTY_STRING;

        return str.replaceAll(regularExpression, StringUtils.EMPTY_STRING);
    }

    /**
     * Checks if a given string is number or not
     * @param str Input string
     * @return true if the string is a number, false otherwise
     */
    public static boolean isNumber(String str) {
        if (StringUtils.isEmpty(str))
            return false;

        return NUMBER_PATTERN.matcher(str).matches();
    }

    /**
     * Creates a list of ChipInfo objects from the input String and returns it.
     * @param text The text to be converted. This text must be in RFC 822 format
     * @return List of ChipInfo objects
     */
    public static List<ChipInfo> getChipInfoList(String text) {
        ArrayList<ChipInfo> chipsList = new ArrayList<ChipInfo>();

        Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(text);
        for (int i=0; i<tokens.length; i++) {
            String number = tokens[i].getAddress();
            String name = tokens[i].getName();
            if (LOG_DEBUG) Log.d(TAG, "Populating name:"+name+", number:"+number);

            if (isMessagableNumber(number) && !isDuplicate(tokens, i)) {
                chipsList.add(
                    new ChipInfo(
                        TextUtils.isEmpty(name) ? number : name,
                        number,
                        TextUtils.isEmpty(name) || name.equals(number) ? 0 : 1
                    )
                );
            }
        }
        return chipsList;
    }

    /**
     * Returns a string of contact names separated by delimiter
     * @param text Input text in RFC 822 format
     * @param delimiter Delimiter to separate names
     * @return String of contact names separated by delimiter
     */
    public static String getNamesAsString (String text, String delimiter) {
        List<ChipInfo> chipInfoList = getChipInfoList(text);
        StringBuilder namesBuilder = new StringBuilder();

        if (chipInfoList != null && chipInfoList.size() > 0) {
            for (ChipInfo chipInfo : chipInfoList) {
                namesBuilder.append(chipInfo.getName()).append(delimiter);
            }
        }
        if (namesBuilder.length() > delimiter.length()) {
            //Remove trailing delimiter
            namesBuilder.delete(namesBuilder.length()-delimiter.length(), namesBuilder.length());
        }
        return namesBuilder.toString();
    }

    /**
     * Returns a string of numbers separated by delimiter
     * @param text Input text in RFC 822 format
     * @param delimiter Delimiter to separate numbers
     * @return String of numbers separated by delimiter
     */
    public static String getNumbersAsString (String text, String delimiter) {
        List<ChipInfo> chipInfoList = getChipInfoList(text);
        StringBuilder numbersBuilder = new StringBuilder();

        if (chipInfoList != null && chipInfoList.size() > 0) {
            for (ChipInfo chipInfo : chipInfoList) {
                numbersBuilder.append(chipInfo.getNumber()).append(delimiter);
            }
        }
        if (numbersBuilder.length() > delimiter.length()) {
            //Remove trailing delimiter
            numbersBuilder.delete(numbersBuilder.length()-delimiter.length(), numbersBuilder.length());
        }
        return numbersBuilder.toString();
    }

    /**
     * Returns a string of known flags separated by delimiter
     * @param text Input text in RFC 822 format
     * @param delimiter Delimiter to separate flags
     * @return String of flags separated by delimiter
     */
    public static String getKnownFlagsAsString (String text, String delimiter) {
        List<ChipInfo> chipInfoList = getChipInfoList(text);
        StringBuilder knownFlagsBuilder = new StringBuilder();

        if (chipInfoList != null && chipInfoList.size() > 0) {
            for (ChipInfo chipInfo : chipInfoList) {
                knownFlagsBuilder.append(chipInfo.getKnownFlag()).append(delimiter);
            }
        }
        if (knownFlagsBuilder.length() > delimiter.length()) {
            //Remove trailing delimiter
            knownFlagsBuilder.delete(knownFlagsBuilder.length()-delimiter.length(), knownFlagsBuilder.length());
        }
        return knownFlagsBuilder.toString();
    }

    /**
     * Checks if a particular RFC 822 token is duplicated in an array of tokens
     * @param tokens Array of RFC 822 tokens
     * @param index Index of the token to be checked
     * @return true if the token is duplicated, false otherwise
     */
    private static boolean isDuplicate (Rfc822Token[] tokens, int index) {
        if (tokens != null && index > -1 && index < tokens.length) {
            String address = tokens[index].getAddress();

            for (int i=0; i<index; i++) {
                if (tokens[i].getAddress().equals(address))
                    return true;
            }
        }
        return false;
    }

    /**
     * Method to combine names and numbers and store it into a list
     * @param names Names separated by delimiter
     * @param numbers Numbers separated by delimiter
     * @param delimiter Delimiter string
     * @return List of combined names and numbers
     */
    public static List<String> getFormattedAddresses (String names, String numbers, String delimiter) {
        String[] namesArr = names.split(delimiter);
        String[] numbersArr = numbers.split(delimiter);
        ArrayList<String> addressList = new ArrayList<String>();
        if (namesArr != null && numbersArr != null && namesArr.length == numbersArr.length) {
            for (int i=0; i< numbersArr.length; i++) {
                String name = namesArr[i];
                String number = numbersArr[i];
                if (isMessagableNumber(number)) {
                    number = PhoneNumberUtils.stripSeparators(number);
                }
                addressList.add(isMessagableNumber(name) ? number : new Rfc822Token(name, number, null).toString());
            }
        }
        return addressList;
    }

    public static class ChipInfo {

        private String mName;
        private String mNumber;
        private int mKnownFlag;

        public ChipInfo (String name, String number, int knownFlag) {
            mName = name;;
            mNumber = number;
            mKnownFlag = knownFlag;
        }

        public String getName() {
            return mName;
        }

        public String getNumber() {
            return mNumber;
        }

        public int getKnownFlag() {
            return mKnownFlag;
        }
    }

    /**
     * Returns the display name for the contact with the given number if a contact
     * exists
     * @param number
     * @param context
     * @return
     */
    public static String getContactDisplayName(String number, Context context)
    {
        if(number != null) {
            String name = null;
            Cursor c = getPhoneCursorForContactNumber(number, context);
            if(c != null) {
                name = c.getString(c.getColumnIndexOrThrow(Phone.DISPLAY_NAME));
                c.close();
            }
            return name;
        } else
            return null;

    }

    /**
     * Returns cursor if there is a contact with the given number
     * This query uses PhoneLookup table to query the name associated with a phone number
     * @param contact
     * @return
     */
    public static Cursor getPhoneCursorForContactNumber(String number, Context context)
    {
        if(number != null) {
            String[] PHONE_PROJECTION = {PhoneLookup._ID, PhoneLookup.NUMBER, PhoneLookup.DISPLAY_NAME, PhoneLookup.PHOTO_ID};

            Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, number);
            Cursor c = null;

            try {

                c =  context.getContentResolver().query(
                         uri, PHONE_PROJECTION,
                         null, null, null);

                if (c == null) {
                    Log.e(TAG, "Cursor open failed");
                }
                else {
                    if (c.moveToFirst())
                        return c;
                    else
                        c.close();
                }
            } catch(IllegalArgumentException ex) {
                if(c != null)
                    c.close();
                Log.w(TAG, " Exception received " + ex.toString());
            }

            return null;
        } else
            return null;
    }

}
