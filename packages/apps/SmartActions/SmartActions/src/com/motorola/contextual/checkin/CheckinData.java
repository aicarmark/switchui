/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date         CR Number       Brief Description
 * -------------------------   ----------   -------------   ------------------------------
 * w04917 (Brian Lee)          2012/02/17   IKHSS7-8262     Initial version
 *
 */

package com.motorola.contextual.checkin;

import java.util.Collection;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class CheckinData {
    private static final String TAG = "CheckinData";
    private static final boolean LOCAL_D = false;

    /* the values here must match the values in SmartActionsFW */
    private static final String CHECKIN_TAG = "CHECKIN_TAG";
    private static final String CHECKIN_EVENT = "CHECKIN_EVENT";
    private static final String CHECKIN_VERSION = "CHECKIN_VERSION";
    private static final String CHECKIN_EVENT_DATA = "CHECKIN_EVENT_DATA";
    private static final String CHECKIN_FIELD_KEY = "KEY";
    private static final String CHECKIN_FIELD_VALUE = "VALUE";

    private static final String NULL_STR = "null";

    private final String mTag;
    private final String mEventName;
    private final String mVersion;
    private final Collection<JSONObject> mEventData = new LinkedList<JSONObject>();

    private static class CheckinField {
        final String mKey;
        final String mValue;

        CheckinField(String key, String value) {
            if (value == null || value.isEmpty()) {
                value = NULL_STR;
            }
            mKey = key;
            mValue = value;
        }

        JSONObject getJsonObject() {
            JSONObject jsonObject = null;

            if (mKey != null && !mKey.isEmpty()) {
                jsonObject = new JSONObject();
                try {
                    jsonObject.put(CHECKIN_FIELD_KEY, mKey);
                    jsonObject.put(CHECKIN_FIELD_VALUE, mValue);
                } catch (JSONException e) {
                    jsonObject = null;
                    Log.e(TAG, "Unable to create JSON Object.");
                }
            }
            return jsonObject;
        }
    }

    /**
     * Passing in null or empty string for any of the three required parameters
     * will result in a failed checkin.
     * @param tag the checkin tag to use for this CheckinData
     * @param eventName the checkin event name
     * @param version the version of this checkin event
     */
    public CheckinData(String tag, String eventName, String version) {
        mTag = tag;
        mEventName = eventName;
        mVersion = version;
    }

    /**
     * Sets the field (name-value pair) for the checkin event.
     * If value is null or empty, the value will be set to the word "null".
     * The order in which you call setValue() will be preserved when
     * checkin object is generated on the FW side.
     * It does not check whether the key already exists or not, it will just append at the end.
     * However, at the FW side, when this gets parsed, the latter value will overwrite the
     * previous value, so everything will work out in the end.
     * @param key checkin field key
     * @param value checkin field value
     * @return true on success, false otherwise
     */
    public boolean setValue(String key, String value) {
        boolean success = false;
        if (key != null && !key.isEmpty()) {
            CheckinField dataEntry = new CheckinField(key, value);
            JSONObject jsonObject = dataEntry.getJsonObject();
            if (jsonObject != null) {
                mEventData.add(jsonObject);
                success = true;
            }
        }
        return success;
    }

    /**
     * This should be called on a background thread.
     * @param uri Uri of SmartActionsFW to checkin to
     * @param context Context to use
     * @return true on success, false otherwise
     */
    public boolean checkin(Uri uri, Context context) {
        boolean success = false;
        if (uri != null && context != null && mTag != null && !mTag.isEmpty()
                && mEventName != null && !mEventName.isEmpty()
                && mVersion != null && !mVersion.isEmpty()
                && !mEventData.isEmpty()) {
            /* would have been nice to just use name-value pairs based on ContentValues only,
             * but the order of fields actually matter in check-in. This is because
             * it may be optimized to strip out the names from the name-value pair and make it
             * into a position based check-in String. Since it's important to remember the original
             * order of key-value pairs, we have to use a JSONArray. You can't put arrays into
             * ContentValues.
             */
            JSONArray eventDataJsonArray = new JSONArray(mEventData);
            String jsonEventData = eventDataJsonArray.toString();

            /* any changes to format here must be reflected in the SmartActionsFW */
            ContentValues cv = new ContentValues();
            cv.put(CHECKIN_TAG, mTag);
            cv.put(CHECKIN_VERSION, mVersion);
            cv.put(CHECKIN_EVENT, mEventName);
            cv.put(CHECKIN_EVENT_DATA, jsonEventData);

            if (LOCAL_D) {
                Log.d(TAG, jsonEventData);
            }

            try {
                context.getContentResolver().insert(uri, cv);
                success = true;
            } catch (Exception e) {
                Log.e(TAG, "Checkin failed.");
                if (LOCAL_D) {
                    e.printStackTrace();
                }
            }
        }

        return success;
    }
}
