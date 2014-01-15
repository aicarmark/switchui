/*
 * Copyright (C) 2010-2011, Motorola, Inc,
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

package com.android.contacts.spd;

// moved from SpeedDialUtils.java

// BEGIN Motorola, Aug-08-2011, IKPIM-282
import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.spd.SpeedDialFlexReader.ItemInfo;
import com.android.contacts.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Math;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONException;
// import com.motorola.dialer.SpeedDialFlexReader.ItemInfo; //MOT FID 35850-Flexing SDN's IKCBS-2075


public class SpeedDialStorage {
    private static final String TAG = "SpeedDialStorage";

    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End

    // constant
    protected static final int MAX_SPEED_DIAL_POSITIONS = 10;
    private static final String APP_SETTINGS_DB_LAST_MODIFIED = "com.android.phone.SpeedDialActivity.db_timestamp";
    private static final String APP_SETTINGS_NAME = "com.android.phone.SpeedDialActivity.data";
    private static final String NEW_SPEED_DIAL_DATA_DELIMITER = "[";
    private static final String NAME = "name";
    private static final String VALUE = "value";

    // variable
    private static int FIRST_SPEED_POS = 2;
    private static HashMap<Integer, String> mSpeedDialList = null;

    // Start MOT FID 35850-Flexing SDN's IKCBS-2075
    private static HashMap<Integer, String> mSpeedDialLock = null;
    private static String VALUE_NO_LOCK = "0";

    private static final String sSpeedDialFlexFileName = "etc/motorola/com.motorola.dialer/SpeedDialList.xml";
    private static final String APP_SETTINGS_NUM_LOCK = "com.android.phone.SpeedDialActivity.num_lock";
    // End MOT FID 35850
    private static boolean initCompleted = false;//MOT Calling Code - IKMAIN-32434
    protected static void initSetupComplete(Context context) {
        // FIRST_SPEED_POS = DialerApp.bExcludeVoicemail ? 1 : 2;
        SpeedDialHelper.bExcludeVoicemail = context.getResources().getBoolean(R.bool.ftr_36927_exclude_voicemail); //MOT FID 36927-Speeddial#1 IKCBS-2013
        FIRST_SPEED_POS = SpeedDialHelper.bExcludeVoicemail ? 1 : 2;
        writeDbAsync(context); //MOT Calling Code - IKMAIN-32434
    }

    private static boolean initSpeedDialNum(Context context) {
        mSpeedDialList = new HashMap<Integer, String>();
        boolean needWriteDb = false;

        String blob = readDb(context, APP_SETTINGS_NAME);
        if (checkBlobIsValid(blob)) {
            // FirstTime SpeedDial

            needWriteDb = true;
        } else {
            try {
                JSONArray jsonArray = new JSONArray(blob);
                final int maxPosition = Math.min(MAX_SPEED_DIAL_POSITIONS,
                        jsonArray.length());
                synchronized (mSpeedDialList) {
                    for (int position = 0; position < maxPosition; position++) {
                        String value = jsonArray.getString(position);
                        if (position % 10 == 0 || checkNull(value)) {
                            continue;
                        }
                        mSpeedDialList.put(position, value);
                    }
                }
            } catch (JSONException ex) {
            }
        }

        return needWriteDb;
    }

    private static String readDb(Context context, String key) {

        return (getAppSettings(context, key, null));
    }

    /*****************
     * IMPORTANT NOTE:
     * Only single instance of speeddialprovider is now access DB directly.
     *****************/

    private static void writeDbAsync(final Context context) {
        new Thread() {
            @Override
            public void run() {
                //Calling Code Begin -  IKMAIN-32434
                 if(!initCompleted) {
                      // Start MOT FID 35850-Flexing SDN's IKCBS-2075
                      boolean needWriteDbNum = initSpeedDialNum(context);
                      boolean needWriteDbLock = initSpeedDialLock(context);
                      initCompleted = true;
                      if(!needWriteDbNum && !needWriteDbLock) {
                          return;
                      }
                      // End MOT FID 35850
                 }
                 //Calling Code End -  IKMAIN-32434
                SpeedDialSetting.setAppSettings(APP_SETTINGS_NAME, convertListToString(context, mSpeedDialList));
                SpeedDialSetting.setAppSettings(APP_SETTINGS_NUM_LOCK, convertListToString(context, mSpeedDialLock));
                SpeedDialSetting.setAppSettings(APP_SETTINGS_DB_LAST_MODIFIED, Long.toString(System.currentTimeMillis()));
                SpeedDialSetting.writeAppSettings(context);
            }
        }.start();
    }

    protected static int getSpeedDialPosByNumber(Context context, String number) {
        //MOT Calling Code IKSTABLETWOV-4404
        for (int position = FIRST_SPEED_POS; position < MAX_SPEED_DIAL_POSITIONS; position++) {
            synchronized (mSpeedDialList) {
                String val = mSpeedDialList.get(position);
                if ((val != null) && (val.equals(number) || true == PhoneNumberUtils.compare(context, val, number))) {
                    return position;
                }
            }
        }
        return -1;
    }

    protected static String getSpeedDialNumberByPos(int position) {
        synchronized (mSpeedDialList) {
            return mSpeedDialList.get(position);
        }
    }

    protected static boolean deleteSpeedDialByPos(Context context, int position) {
        if (position >= MAX_SPEED_DIAL_POSITIONS ||
                position < FIRST_SPEED_POS ||
                (position % 10) == 0) {
            return false;
        }
        synchronized (mSpeedDialList) {
            if (false == mSpeedDialList.containsKey(position)) {
                return false;
            }
            mSpeedDialList.remove(position);
        }
        writeDbAsync(context);
        return true;
    }

    protected static int firstEditablePos() {
        return FIRST_SPEED_POS;
    }

    protected static int firstEmptySpeedDialPos() { //MOT FID 36927-Speeddial#1 IKCBS-2013
        for (int position = FIRST_SPEED_POS; position < MAX_SPEED_DIAL_POSITIONS; position++) {
            if ((position % 10) == 0) {
                continue;
            }
            synchronized (mSpeedDialList) {
                if (!mSpeedDialList.containsKey(position)) {
                    return position;
                }
            }
        }
        return -1;
    }

    protected static boolean setSpeedDial(Context context, int position, String number) {
        if (position >= MAX_SPEED_DIAL_POSITIONS ||
                position < FIRST_SPEED_POS ||
                (position % 10) == 0) {
            return false;
        }
        synchronized (mSpeedDialList) {
            mSpeedDialList.put(position, number);
        }
        writeDbAsync(context);
        return true;
    }

    private static final String getAppSettings(Context context, String key, String defaultValue) {
        if (key == null)
            throw new IllegalArgumentException("App settings key is null");

        String value = SpeedDialSetting.getAppSettings(context, key);
        if(!TextUtils.isEmpty(value)){
            return value;
        }

        return defaultValue;
    }


    // Start MOT FID 35850-Flexing SDN's IKCBS-2075
    private static boolean initSpeedDialLock(Context context) {
        mSpeedDialLock = new HashMap<Integer, String>();
        String lockBlob = readDb(context, APP_SETTINGS_NUM_LOCK);

        boolean needWriteDb = false;

        if (checkBlobIsValid(lockBlob)) {
            // FirstTime SpeedDial
            needWriteDb = true;
        } else {
            try {
                JSONArray jsonArray = new JSONArray(lockBlob);
                final int maxPosition = Math.min(MAX_SPEED_DIAL_POSITIONS,
                        jsonArray.length());
                synchronized (mSpeedDialLock) {
                    for (int position = 0; position < maxPosition; position++) {
                        if (position % 10 == 0) {
                            continue;
                        }

                        if (!jsonArray.isNull(position)) {
                            String value = jsonArray.getString(position);
                            mSpeedDialLock.put(position, value);
                        }
                    }
                }
            } catch (JSONException ex) {
            }
        }

        if (!SpeedDialFlexUtils.checkApFlexNotModified(context)) {
            setFlexSpeedDialNumbers(context);
            needWriteDb = true;
        }

        return needWriteDb;
    }

    public static String convertListToString(final Context context,
            final HashMap<Integer, String> map) {
        JSONArray jsonArray = new JSONArray();
        try {
            synchronized (map) {
                for (int position : map.keySet()) {
                    jsonArray.put(position, map.get(position));
                }
            }
        } catch (JSONException ex) {
        }

        return jsonArray.toString();
    }

    private static boolean checkBlobIsValid(String blob) {
        return ((null == blob) ||
             (blob.compareTo("_E_") == 0) || // empty setting
             (false == blob.contains(NEW_SPEED_DIAL_DATA_DELIMITER)) || // Not backward compatible for MAPS3 setting.
             (blob.isEmpty()));
    }

    private static boolean checkNull(String value) {
        if ((null == value) || (value.isEmpty())
                || (value.compareTo("null") == 0)) {
            return true;
        }

        return false;
    }

    public static String getSpeedDialLockByPos(int position) {
        synchronized (mSpeedDialLock) {
            return mSpeedDialLock.get(position);
        }
    }

    private static void setFlexSpeedDialNumbers(final Context context) {
        ArrayList<ItemInfo> sdList = SpeedDialFlexReader
                .getSpeedDialList(sSpeedDialFlexFileName);

        for (int position = FIRST_SPEED_POS; position < MAX_SPEED_DIAL_POSITIONS; position++) {
            if ((position % 10) == 0) {
                continue;
            }

            if (!mSpeedDialList.containsKey(position)) {
                mSpeedDialLock.put(position, VALUE_NO_LOCK);
            }
        }

        for (ItemInfo sdInfo : sdList) {
            String num = sdInfo.getContactNum();
            String lockOrUnlock = sdInfo.getLockOrUnLock();
            int digit = sdInfo.getDialDigit();


            if (!mSpeedDialList.containsKey(digit)) {
                mSpeedDialList.put(digit, num);

                mSpeedDialLock.put(digit, lockOrUnlock);
            }
        }

        SpeedDialFlexUtils.setApFlexVerPreference(context,
                SpeedDialFlexUtils.getApFlexVersion(context));
    }
    // End MOT FID 35850
}
