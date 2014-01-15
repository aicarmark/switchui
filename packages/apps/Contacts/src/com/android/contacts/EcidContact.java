/*
 * Copyright (C) 2009-2011 Cequint, Inc.
 *
 * Authors: John Seghers jseghers@cequint.com, Thien-An Nguyen, Arati Ogale, Jiyeon Lee
 * This is a class used to store and manipulate ECID information
 * obtained from the com.cequint.cityid.CityIdContentProvider
 *
 */
package com.android.contacts;

import java.util.HashMap;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

// Added by MOTOROLA
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Paint;

public class EcidContact
{
    private static final String TAG = "EcidContact";
    private static final String ECID_AOSP_VERSION = "4.0.2"; // make sure to update EcidClient.java
    private static final String PROVIDER_NAME = "com.cequint.ecid";
    public static final boolean DBG = false;

    private static final Uri CONTENT_URI = Uri.parse("content://"+ PROVIDER_NAME + "/lookup");

    private static final String                          _ID = "_ID";
    private static final String                       _COUNT = "_COUNT";
    private static final String                       NUMBER = "cid_pNumber";
    private static final String                         CITY = "cid_pCityName";
    private static final String                        STATE = "cid_pStateName";
    private static final String                   STATE_ABBR = "cid_pStateAbbr";
    private static final String                      COUNTRY = "cid_pCountryName";
    private static final String                      COMPANY = "cid_pCompany";
    private static final String                         NAME = "cid_pName";
    private static final String                   FIRST_NAME = "cid_pFirstName";
    private static final String                    LAST_NAME = "cid_pLastName";
//    private static final String                        IMAGE = "cid_pImage";
//    private static final String                 SAME_NETWORK = "cid_bSameNetwork";
//    private static final String                      FRIENDS = "cid_bFriends";
//    private static final String             PREFER_CID_IMAGE = "cid_bPreferCidImage";
    private static final String NO_OUTGOING_CALL_RESTRICTION = "cid_bNoOutgoingCallRestriction";

    static int COLUMN_ID;
    static int COLUMN_COUNT;
    static int COLUMN_NUMBER;
    static int COLUMN_CITY;
    static int COLUMN_STATE;
    static int COLUMN_STATE_ABBR;
    static int COLUMN_COUNTRY;
    static int COLUMN_COMPANY;
    static int COLUMN_NAME;
    static int COLUMN_FIRST_NAME;
    static int COLUMN_LAST_NAME;
//    static int COLUMN_IMAGE;
//    static int COLUMN_SAME_NETWORK;
//    static int COLUMN_FRIENDS;
//    static int COLUMN_PREFER_CID_IMAGE;
    static int COLUMN_NO_OUTGOING_CALL_RESTRICTION;

    public String m_number;
    public boolean m_isNanp = false;

    public String m_bizName;
    public String m_firstName;
    public String m_lastName;
    public String m_cname;
    public String m_callerId;
    public String m_displayName;
    public String m_cityName;
    public String m_stateName;
    public String m_stateAbbr;
    public String m_countryName;
//    public String m_urlPicture;
//    public boolean m_bSameNetwork;
//    public boolean m_bFriends;
//    public boolean m_bPreferCidImage;
    public boolean m_bNoOutgoingCallRestriction = false;
//    public Bitmap m_picture;

    static HashMap<String, EcidContact> m_mapEcidContacts = new HashMap<String, EcidContact>();

    public static EcidContact getEcidContact(String number)
    {
        String phoneNumber = PhoneNumberUtils.stripSeparators(number);
        return m_mapEcidContacts.get(PhoneNumberUtils.stripSeparators(phoneNumber));
    }

    public static void clearEcidContacts() {
        synchronized (m_mapEcidContacts) {
            m_mapEcidContacts.clear();
        }
        if (DBG) Log.d(TAG, "clearEcidContacts()");
    }

    /*
     * CEQUINT(JRS): lookup here
     * This lookup takes a while so it MUST BE looked up on back ground thread
     */
    // Modified by Motorola: use this function ONLY to do content provider lookup.
    // Added separate function doCacheLookup() for cache lookup.
    public static EcidContact doLookup(Context ctx, String number, int numberType)
    {
        Log.d(TAG, "ECID AOSP VERSION: " + ECID_AOSP_VERSION);
        if (ctx != null && !TextUtils.isEmpty(number)) {
//            EcidContact cidContact = getEcidContact(number);
//            if (cidContact != null) {
//                if (DBG) Log.d(TAG, "Skip look up, already done previously: ctx: " + ctx + " number: " + number);
//                return cidContact;
//            }
            EcidContact cidContact = new EcidContact();
            cidContact.m_number = PhoneNumberUtils.stripSeparators(number);
            cidContact.implLookup(ctx, number, numberType != Calls.OUTGOING_TYPE, false);
            m_mapEcidContacts.put(cidContact.m_number, cidContact);
            if (DBG) Log.d(TAG, "added cidContact " + cidContact + "for number: " + cidContact.m_number);

            if (DBG) Log.d(TAG, "look up completed: number: " + number);
            return cidContact;
        }
        else {
            Log.d(TAG, "doLookup Invalid Args: ctx: " + ctx + " number: " + number);
            return null;
        }
    }

    // Function added by Motorola
    public static EcidContact doCacheLookup(Context ctx, String number, int numberType)
    {
        Log.d(TAG, "ECID AOSP VERSION: " + ECID_AOSP_VERSION);
        if (ctx != null && !TextUtils.isEmpty(number)) {
            EcidContact cidContact = getEcidContact(number);
            return cidContact;
        }
        else {
            Log.d(TAG, "doLookup Invalid Args: ctx: " + ctx + " number: " + number);
            return null;
        }
    }

    //
    // Cequint truncation requirements would not be applied for android platform... we will let
    // system fade off text to take place here
    public static String getCityId(String number)
    {
        EcidContact info = getEcidContact(number);
        if (info == null) {
            String nEmpty = "";
            return nEmpty;
        }

//Commented by MOTOROLA.  Best to reset the display string in case the amount of space to show has changed
//        // If we've already computed the display name for this size, just return it.
//        if (!TextUtils.isEmpty(info.m_displayName))
//            return info.m_displayName;

        info.m_displayName = null;

        if (TextUtils.isEmpty(info.m_cityName) && ! TextUtils.isEmpty(info.m_stateName))
        {
            info.m_displayName = info.m_stateName;
        }
        else if (! TextUtils.isEmpty(info.m_cityName) && ! TextUtils.isEmpty(info.m_stateAbbr))
        {
            info.m_displayName = info.m_cityName + ", " + info.m_stateAbbr;
        }
        else if (! TextUtils.isEmpty(info.m_countryName)) {
            info.m_displayName = info.m_countryName;
        }

        if (info.m_displayName == null)
            info.m_displayName = "";

        if (DBG) Log.d(TAG, "m_displayName: " + info.m_displayName);

        return info.m_displayName;
    }

    boolean hasCityId()
    {
        return !TextUtils.isEmpty(m_cityName) ||
                !TextUtils.isEmpty(m_stateName) ||
                !TextUtils.isEmpty(m_stateAbbr) ||
                !TextUtils.isEmpty(m_countryName);
    }

    static void readColumnIds(Cursor c)
    {
        COLUMN_ID = c.getColumnIndex(_ID);
        COLUMN_COUNT = c.getColumnIndex(_COUNT);
        COLUMN_NUMBER = c.getColumnIndex(NUMBER);
        COLUMN_CITY = c.getColumnIndex(CITY);
        COLUMN_STATE = c.getColumnIndex(STATE);
        COLUMN_STATE_ABBR = c.getColumnIndex(STATE_ABBR);
        COLUMN_COUNTRY = c.getColumnIndex(COUNTRY);
        COLUMN_COMPANY = c.getColumnIndex(COMPANY);
        COLUMN_NAME = c.getColumnIndex(NAME);
        COLUMN_FIRST_NAME = c.getColumnIndex(FIRST_NAME);
        COLUMN_LAST_NAME = c.getColumnIndex(LAST_NAME);
//        COLUMN_IMAGE = c.getColumnIndex(IMAGE);
//        COLUMN_SAME_NETWORK = c.getColumnIndex(SAME_NETWORK);
//        COLUMN_FRIENDS = c.getColumnIndex(FRIENDS);
//        COLUMN_PREFER_CID_IMAGE = c.getColumnIndex(PREFER_CID_IMAGE);
        COLUMN_NO_OUTGOING_CALL_RESTRICTION = c.getColumnIndex(NO_OUTGOING_CALL_RESTRICTION);

    }

    public void implLookup(Context context, CharSequence strNumber, boolean isSystemProvided, boolean isNotNanp)
    {
        // The character sequence provided is formatted
        // We need to strip the formatting characters into a new char[] and then create a String
        // to send to the Query
        // We must not modify strNumber
        if (DBG) Log.d(TAG, "Lookup " + strNumber.toString());
        ContentResolver cr = context.getContentResolver();

        // TODO: OEM must set the String array appropriately
        // "NOT-NANP" should be included if the call was NOT received while on an NANP servicing network
        // "system" or "system provided" should be included if it was an incoming call, or returning a call from a system-provided number
        // "user" or "user provided" should be included if it cannot be determined to be a system-provided number
        //
        String[] flags;
        if (isNotNanp)
            flags = new String[] { isSystemProvided ? "system" : "user", "NOT-NANP" };
        else
            flags = new String[] { isSystemProvided ? "system" : "user" };

        m_isNanp = !isNotNanp;        // Save this for later

        Cursor c = cr.query(CONTENT_URI, null, strNumber.toString(), flags, null);
        if (c != null && c.moveToFirst())
        {
            readColumnIds(c);
            m_callerId                    = getString(c, COLUMN_NUMBER);
            m_cityName                      = getString(c, COLUMN_CITY);
            m_stateName                       = getString(c, COLUMN_STATE);
            m_stateAbbr                       = getString(c, COLUMN_STATE_ABBR);
            m_countryName                  = getString(c, COLUMN_COUNTRY);

            m_bizName                       = getString(c, COLUMN_COMPANY);
            m_cname                           = getString(c, COLUMN_NAME);
            m_firstName                       = getString(c, COLUMN_FIRST_NAME);
            m_lastName                       = getString(c, COLUMN_LAST_NAME);
//            m_urlPicture                   = getString(c, COLUMN_IMAGE);
//            m_bSameNetwork                   = c.getInt(COLUMN_SAME_NETWORK) != 0;
//            m_bFriends                       = c.getInt(COLUMN_FRIENDS) != 0;
//            m_bPreferCidImage              = c.getInt(COLUMN_PREFER_CID_IMAGE) != 0;
            if (COLUMN_NO_OUTGOING_CALL_RESTRICTION >= 0) {
                m_bNoOutgoingCallRestriction = c.getInt(COLUMN_NO_OUTGOING_CALL_RESTRICTION) != 0;
            }
        }
        else
        {
            Log.d(TAG, "No CityID found");
        }
        if ( c != null ) c.close(); // IKHSS6UPGR-11068
    }

    static String getString(Cursor c, int nColumn)
    {
        if (!c.isNull(nColumn))
        {
            String str = c.getString(nColumn);
            if (!TextUtils.isEmpty(str))
                return str;
        }
        return null;
    }

    public static String getContactName(String number, int numberType)
    {
        if (DBG) Log.d(TAG, "getContactName() for phone:" + number);
        String phoneNumber = PhoneNumberUtils.stripSeparators(number);

        EcidContact info = getEcidContact(phoneNumber);
        if (info == null) {
            if (DBG) Log.d(TAG, "getContactName skip: getEcidContact return null for phone:" + number);
            return null;
        }

        return info.getContactName(numberType);
    }

    public String getContactName(int numberType)
    {
        if (DBG) Log.d(TAG, "getContactName() for phone:" + m_number);

        if (numberType == Calls.OUTGOING_TYPE && m_bNoOutgoingCallRestriction == false) {
            return null;
        }

        String contactDisplayName = getContactDisplayName();
        if (DBG) Log.d(TAG, String.format("getContactName: num: %s type: %s name: %s", m_number, numberType, contactDisplayName));
        return contactDisplayName;
    }

    static boolean isNameThePhoneNumber(String name, String number)
    {
        // If either is empty, then no
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(number))
            return false;

        // Strip separators from both and return the comparison
        return TextUtils.equals(PhoneNumberUtils.stripSeparators(name), PhoneNumberUtils.stripSeparators(number));
    }

    public String getContactDisplayName()
    {
        // Set booleans for easy testing
        boolean bHasFName = !TextUtils.isEmpty(m_firstName);
        boolean bHasLName = !TextUtils.isEmpty(m_lastName);
        boolean bHasBizName = !TextUtils.isEmpty(m_bizName);
        boolean bHasName = !TextUtils.isEmpty(m_cname);

        StringBuilder sb = new StringBuilder();

        if (bHasFName || bHasLName)
        {
            if (bHasFName)
            {
                sb.append(m_firstName);
                if (bHasLName)
                    sb.append(" ");
            }
            if (bHasLName)
                sb.append(m_lastName);
        }
        else if (bHasBizName)
        {
            sb.append(m_bizName);
        }
        else if (bHasName)
        {
            sb.append(m_cname);
        }

        if (DBG) Log.d(TAG, "gDN len " + sb.length() + "sb string is " +sb.toString());
        if (sb.length() > 0)
            return sb.toString();
        if (DBG) Log.d(TAG, "null Contact Display Name");
        return null;
    }

    public String getBizName()
    {
        if (!TextUtils.isEmpty(m_bizName))
            return m_bizName;
        return null;
    }

    static String strOrNull(String s)
    {
        if (s != null)
            return s;
        return "(NULL)";
    }

    public void dumpToLog()
    {
        if (DBG) {
        Log.d(TAG, "m_callerId: " + strOrNull(m_callerId));
        Log.d(TAG, "m_isNanp: " + m_isNanp);
        Log.d(TAG, "m_bizName: " + strOrNull(m_bizName));
        Log.d(TAG, "m_firstName: " + strOrNull(m_firstName));
        Log.d(TAG, "m_lastName: " + strOrNull(m_lastName));
        Log.d(TAG, "m_cname: " + strOrNull(m_cname));
        Log.d(TAG, "m_displayName: " + strOrNull(m_displayName));
        Log.d(TAG, "m_cityName: " + strOrNull(m_cityName));
        Log.d(TAG, "m_stateName: " + strOrNull(m_stateName));
        Log.d(TAG, "m_stateAbbr: " + strOrNull(m_stateAbbr));
        Log.d(TAG, "m_countryName: " + strOrNull(m_countryName));
//        Log.d(TAG, "m_urlPicture: " + strOrNull(m_urlPicture));
//        Log.d(TAG, "m_bSameNetwork: " + m_bSameNetwork);
//        Log.d(TAG, "m_bFriends: " + m_bFriends);
//        Log.d(TAG, "m_bPreferCidImage: " + m_bPreferCidImage);
            }
    }

    // Function added by MOTOROLA
    // Use the information in the data returned by CityId and the font specified by the Paint object
    // to determine the proper formatting--including truncation, if any--to display in the space
    // available.
    public static String computeDisplayName(String number, TextView cityIDView)//, int wAvailable)
    {
        EcidContact info = getEcidContact(number);

        if (info == null || cityIDView == null) { return ""; }

        // Determine width available for showing the City/State. If the City is too long,
        // it will be truncated. Between 150 and 240 is reasonable default.  Sometimes the
        // view's width reports as smaller than it should.
        int wAvailable = cityIDView.getWidth() > 150 ? cityIDView.getWidth() : 240;
        if (DBG) Log.d(TAG, "wAvailable is " + wAvailable);

        Paint paint = cityIDView.getPaint();

        // Clear out the current display name, assuming
        // we may have to recalculate the string
        info.m_displayName = null;

        if (TextUtils.isEmpty(info.m_cityName) && !TextUtils.isEmpty(info.m_stateName)) {
            info.m_displayName = info.m_stateName;
        }
        else if (!TextUtils.isEmpty(info.m_cityName) && !TextUtils.isEmpty(info.m_stateAbbr)) {
            // If screen width is not enough to show full City/State info, CityName should be truncated
            // the State abbreviation SHALL remain. (example: 'Morrisons Cro..., AL')
            if ((wAvailable != 0) && ((paint.measureText(info.m_cityName) + paint.measureText(info.m_stateAbbr) + paint.measureText(", ")) > wAvailable)) {
                int curCityNameWidth = (int)paint.measureText(info.m_cityName);
                int otherStrWidth = (int)(paint.measureText(info.m_stateAbbr) + paint.measureText("..., "));
                int maxCityNameWidth = wAvailable - otherStrWidth;

                int len = info.m_cityName.length();
                float widths[] = new float[len];
                paint.getTextWidths(info.m_cityName, widths);
                while ( curCityNameWidth > maxCityNameWidth) {
                    curCityNameWidth = curCityNameWidth - (int)widths[len-1];
                    len--;
                }

                info.m_displayName = info.m_cityName.substring(0, len) + "..., " + info.m_stateAbbr;
            } else {
                info.m_displayName = info.m_cityName + ", " + info.m_stateAbbr;
            }
        }
        else {
            info.m_displayName = info.m_countryName;
        }

        return info.m_displayName;
    }

    // Function added by MOTOROLA
    public static boolean isECIDAvailable(Context context) {
        PackageManager pm = context.getPackageManager();

        if ( pm == null ) return false;

        try {
            pm.getPackageInfo(PROVIDER_NAME, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }
}
