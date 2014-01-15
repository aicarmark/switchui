/*
 * Copyright (C) 2009, Motorola, Inc,
 *
 * This file is derived in part from code issued under the following license.
 * Class used store and manipulate CityID information
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
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

public class CityIdInfo
{
    private static final String TAG = "CityIdInfo";
    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End
    private static final String PROVIDER_NAME = "com.cequint.cityid";
    private static final Uri CONTENT_URI = Uri.parse("content://"+ PROVIDER_NAME + "/lookup");

    public static final String CITYID_DATA_KEY = "com.cequint.cityidinfo";
        
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_COUNT = 1;
    private static final int COLUMN_CITY = 2;
    private static final int COLUMN_STATE = 3;
    private static final int COLUMN_STATE_ABRV = 4;
    private static final int COLUMN_COUNTRY = 5;
    private static final int COLUMN_ISNANP = 6;

    public String mCityName = null;
    public String mStateName = null;
    public String mStateAbrv = null;
    public String mCountryName = null;
    public boolean mIsNanpNum = false;
    private String mDisplayName = "";

    private int minCityidViewWidth = 560;//MOT Calling code - IKSHADOW-1590

    public static boolean isAvaialble(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(PROVIDER_NAME, PackageManager.GET_PROVIDERS);

            if ((null == info) || (null == info.providers) || (0 == info.providers.length)) {
                return false;
            }

            for (int i = 0; i < info.providers.length; i++) {
                String authority = info.providers[0].authority;
                if (PROVIDER_NAME.equals(authority))
                    return true;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public String computeDisplayName(TextView view)
    {
        int curCityidViewWidth = view.getWidth();
        if ((curCityidViewWidth > 0)&&(curCityidViewWidth < minCityidViewWidth)){
            minCityidViewWidth = curCityidViewWidth;
        }
        return computeDisplayName(view.getPaint(), minCityidViewWidth);
    }

    //Mot Calling CR IKMAIN-11582
    //
    // Use the information in the data returned by CityId and the font specified by the Paint object
    // to determine the proper formatting--including truncation, if any--to display in the space
    // available.
    public String computeDisplayName(Paint paint, int wAvailable)
    {
        mDisplayName = null;
        // For the NANP number, if City is NULL, show full StateName, else show full CityName+StateAbrv
        if (mIsNanpNum)
        {
            if (TextUtils.isEmpty(mCityName))
            {
                mDisplayName = mStateName;
            }
            else
            {
                /* If screen width is not enough to show full City/State info, CityName should be truncated
                 * the State abbreviation SHALL remain. (example: 'Morrisons Cro..., AL')
                 */
                if ((wAvailable != 0) && ((paint.measureText(mCityName) + paint.measureText(mStateAbrv) + paint.measureText(", ")) > wAvailable)) {
                    int curCityNameWidth = (int)paint.measureText(mCityName);
                    int otherStrWidth = (int)(paint.measureText(mStateAbrv) + paint.measureText("..., "));
                    int maxCityNameWidth = wAvailable - otherStrWidth;
                    int len = mCityName.length();
                    float widths[] = new float[len];
                    paint.getTextWidths(mCityName, widths);
                    while ( curCityNameWidth > maxCityNameWidth && len>0 ) {
                        curCityNameWidth = curCityNameWidth - (int)widths[len-1];
                        len--;
                    }
                    mDisplayName = mCityName.substring(0, len) + "..., " + mStateAbrv;
                } else {
                    mDisplayName = mCityName + ", " + mStateAbrv;
                }
            }
        }
        //For non-NANP number, just show CountryName.
        else
        {
            mDisplayName = mCountryName;
        }
        return mDisplayName;
    }
    //Mot Calling CR IKMAIN-11582 End

    private void doLookup(Context context, CharSequence strNumber, boolean isIncoming, boolean isNanpNetwork)
    {
        if (TextUtils.isEmpty(strNumber) || context == null)
            return;

        // The character sequence provided is formatted
        // We need to strip the formatting characters into a new char[] and then create a String
        // to send to the Query
        // We must not modify strNumber
        if(DBG) log("Lookup " + strNumber.toString());
        ContentResolver cr = context.getContentResolver();

        String[] flags = new String[3];

        if(isNanpNetwork)
        {
            Log.d(TAG, "Now is in NANP network");
            flags[0] = "NANP";
        } else {
            flags[0] = null;
        }
        if (isIncoming)
        {
            flags[1] = "system";
            flags[2] = "incoming";
        } else {
            flags[1] = "user";
            flags[2] = null;
        }
        //MOT Calling code - IKSHADOW-2754
        Cursor c = null;
        try {
            c = cr.query(CONTENT_URI, null, strNumber.toString(), flags , null);
        } catch (NullPointerException ex) {
            if(DBG) log("catch exception = " + ex);
            c = null;
        }
        //MOT Calling code - End
        if (c != null && c.moveToFirst())
        {
            mCityName = c.getString(COLUMN_CITY);
            mStateName = c.getString(COLUMN_STATE);
            mStateAbrv = c.getString(COLUMN_STATE_ABRV);
            mCountryName = c.getString(COLUMN_COUNTRY);
            if(VDBG) log("City   : " + mCityName);
            if(VDBG) log("State  : " + mStateName);
            if(VDBG) log("ST     : " + mStateAbrv);
            if(VDBG) log("Country: " + mCountryName);
            mIsNanpNum = c.getInt(COLUMN_ISNANP) != 0;
        }
        else
        {
            if(DBG) log("No CityID found");
        }
        if (c != null) c.close();//MOT Calling code - IKSHADOW-2462
    }

    /*
     * Place call to CityID lookup here
     * This lookup takes 10-30 ms
     * so can be done inline rather than asynchronously
     */
    public static CityIdInfo recentCallCityIdLookup(Context context, String phoneNumber, boolean isIncoming)
    {
        if (TextUtils.isEmpty(phoneNumber) || context == null)
            return null;

        boolean isNanp = isNanpNetwork(context);
        CityIdInfo cidInfo = new CityIdInfo();
        cidInfo.doLookup(context, phoneNumber, isIncoming, isNanp);

        return cidInfo;
    }

    // [ MOTO Calling Code CR - IKSHADOW-122, 33638 CityID
    public static boolean isNanpNetwork(Context context){
        boolean isNanpNetwork = true;
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        /* to-pass-build, Xinyu Liu/dcjf34 */ 
        // if (tm != null) isNanpNetwork = tm.isInNanpNetwork();
        return isNanpNetwork;
    }

    static void log(String msg) {
        Log.d(TAG, " -> " + msg);
    }
}
