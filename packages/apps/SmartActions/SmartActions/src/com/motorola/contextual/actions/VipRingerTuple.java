/*
 * @(#)VipRingerTuple.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2011/08/01  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

/**
 * This class represents each row in the VIP Caller table present in the Quick Actions database <code><pre>
 * CLASS:
 *   Extends ContactsTuple which is used to represent a row in any table in the Quick Actions database
 *
 * RESPONSIBILITIES:
 *   To create an object representing a row in the VIP Caller table
 *
 * COLABORATORS:
 *       Extends ContactsTuple.
 *
 * USAGE:
 *      See each method.
 *
 * </pre></code>
 **/

public class VipRingerTuple extends BaseTuple implements Constants, VipCallerTableColumns {

    private static final String TAG = TAG_PREFIX + VipRingerTuple.class.getSimpleName();

    private String mInternalName;
    private String mNumber;
    private String mName;
    private int mRingerMode;
    private boolean mVibStatus;
    private int mRingerVolume;
    private String mRingtoneUri;
    private String mRingtoneTitle;
    private int mDefRingerMode;
    private int mDefVibStatus;
    private int mDefVibSettings;
    private int mDefVibInSilent;
    private int mDefRingerVolume;
    private String mDefRingtoneUri;
    private String mDefRingtoneTitle;
    private int mIsKnown;
    private double mConfigVersion;

    public VipRingerTuple(String internalName, String number, String name, int ringerMode, boolean vibStatus,
            int ringerVolume, String ringtoneUri, String ringtoneTitle, int isKnown, double configVersion) {
        initDefaults();
        mInternalName = internalName;
        mNumber = number;
        mName = name;
        mRingerMode = ringerMode;
        mVibStatus = vibStatus;
        mRingerVolume = ringerVolume;
        mRingtoneUri = ringtoneUri;
        mRingtoneTitle = ringtoneTitle;
        mIsKnown = isKnown;
        mConfigVersion = configVersion;
    }

    public VipRingerTuple(String internalName, String number, String name, int ringerMode, boolean vibStatus,
            int ringerVolume, String ringtoneUri, String ringtoneTitle, int defRingerMode, int defVibSettings,
            int defVibInSilent, int defRingerVolume, String defRingtoneUri, String defRingtoneTitle, int isKnown,
            double configVersion) {
        initDefaults();
        mInternalName = internalName;
        mNumber = number;
        mName = name;
        mRingerMode = ringerMode;
        mVibStatus = vibStatus;
        mRingerVolume = ringerVolume;
        mRingtoneUri = ringtoneUri;
        mRingtoneTitle = ringtoneTitle;
        mDefRingerMode = defRingerMode;
        mDefVibSettings = defVibSettings;
        mDefVibInSilent = defVibInSilent;
        mDefRingerVolume = defRingerVolume;
        mDefRingtoneUri = defRingtoneUri;
        mDefRingtoneTitle = defRingtoneTitle;
        mIsKnown = isKnown;
        mConfigVersion = configVersion;
    }

    public String getInternalName() {
        return mInternalName;
    }

    public String getNumber() {
        return mNumber;
    }

    public String getName() {
        return mName;
    }

    public int getRingerMode() {
        return mRingerMode;
    }

    public boolean getVibStatus() {
        return mVibStatus;
    }

    public int getRingerVolume() {
        return mRingerVolume;
    }

    public String getRingtoneUri() {
        return mRingtoneUri;
    }
    public String getRingtoneTitle() {
        return mRingtoneTitle;
    }
    public int getDefRingerMode() {
        return mDefRingerMode;
    }
    public int getDefVibStatus() {
        return mDefVibStatus;
    }
    public int getDefVibSettings() {
        return mDefVibSettings;
    }
    public int getDefVibInSilent() {
        return mDefVibInSilent;
    }
    public int getDefRingerVolume() {
        return mDefRingerVolume;
    }
    public String getDefRingtoneUri() {
        return mDefRingtoneUri;
    }
    public String getDefRingtoneTitle() {
        return mDefRingtoneTitle;
    }
    public int getIsKnown() {
        return mIsKnown;
    }

    public double getConfigVersion() {
        return mConfigVersion;
    }

    @Override
    public String getKeyValue() {
        return mInternalName;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues initialValues = new ContentValues();
        initialValues.put(INTERNAL_NAME, mInternalName);
        initialValues.put(NUMBER, mNumber);
        initialValues.put(NAME, mName);
        initialValues.put(RINGER_MODE, mRingerMode);
        initialValues.put(VIBE_STATUS, mVibStatus);
        initialValues.put(RINGER_VOLUME, mRingerVolume);
        initialValues.put(RINGTONE_URI, mRingtoneUri);
        initialValues.put(RINGTONE_TITLE, mRingtoneTitle);
        initialValues.put(DEF_RINGER_MODE, mDefRingerMode);
        initialValues.put(DEF_VIBE_STATUS, mDefVibStatus);
        initialValues.put(DEF_VIBE_SETTINGS, mDefVibSettings);
        initialValues.put(DEF_VIBE_IN_SILENT, mDefVibInSilent);
        initialValues.put(DEF_RINGER_VOLUME, mDefRingerVolume);
        initialValues.put(DEF_RINGTONE_URI, mDefRingtoneUri);
        initialValues.put(DEF_RINGTONE_TITLE, mDefRingtoneTitle);
        initialValues.put(IS_KNOWN, mIsKnown);
        initialValues.put(CONFIG_VERSION, mConfigVersion);

        return initialValues;
    }

    /**
     * Method to extract tuple data from a cursor
     * @param cursor Cursor from which data is to be extracted
     * @return Tuple containing the data present in the cursor
     */
    public static BaseTuple extractDataFromCursor(Cursor cursor) {

        VipRingerTuple tuple = null;

        if (cursor != null && cursor.moveToFirst()) {
            tuple = new VipRingerTuple(cursor.getString(cursor.getColumnIndexOrThrow(INTERNAL_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(NUMBER)),
                    cursor.getString(cursor.getColumnIndex(NAME)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(RINGER_MODE)),
                    (cursor.getInt(cursor.getColumnIndexOrThrow(VIBE_STATUS))==1) ? true : false,
                    cursor.getInt(cursor.getColumnIndexOrThrow(RINGER_VOLUME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(RINGTONE_URI)),
                    cursor.getString(cursor.getColumnIndexOrThrow(RINGTONE_TITLE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DEF_RINGER_MODE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DEF_VIBE_SETTINGS)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DEF_VIBE_IN_SILENT)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(DEF_RINGER_VOLUME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DEF_RINGTONE_URI)),
                    cursor.getString(cursor.getColumnIndexOrThrow(DEF_RINGTONE_TITLE)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(IS_KNOWN)),
                    cursor.getDouble(cursor.getColumnIndexOrThrow(CONFIG_VERSION)));

            if (LOG_DEBUG) Log.d(TAG, "Data present");
        } else {
            Log.e(TAG, "Data cannot be extracted");
        }

        return tuple;
    }

    private void initDefaults() {
        mKey = INTERNAL_NAME;
        mTableName = VIP_CALLER_TABLE;
    }
}