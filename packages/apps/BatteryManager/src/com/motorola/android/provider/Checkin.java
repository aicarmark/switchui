/*
 * Copyright (C) 2006 The Android Open Source Project
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
 *
 * Motorola 02/09 kamil@motorola.com added SYSTEM_PANIC
 * Motorola 06/09 kamil@motorola.com added this class as a clone of
 * Checkin.java to work around marketplace issues
 * Motorola 06/10 kamil@motorola.com brought over from endive.
 */

package com.motorola.android.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.SQLException;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Contract class for the checkin provider, used to store events and
 * statistics that will be uploaded to a checkin server eventually.
 * Describes the exposed database schema, and offers methods to add
 * events and statistics to be uploaded.
 *
 *
 * @hide
 */
public final class Checkin {
    public static final String AUTHORITY = "com.motorola.android.server.checkin";

    /**
     * The events table is a log of important timestamped occurrences.
     * Each event has a type tag and an optional string value.
     * If too many events are added before they can be reported, the
     * content provider will erase older events to limit the table size.
     */
    public interface Events extends BaseColumns {
        public static final String TABLE_NAME = "events";
        public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

        public static final String TAG = "tag";     // TEXT
        public static final String VALUE = "value"; // TEXT
        public static final String DATE = "date";   // INTEGER

        /** Valid tag values.  Extend as necessary for your needs. */
        public enum Tag {
            APANIC_CONSOLE,
            APANIC_THREADS,
            AUTOTEST_FAILURE,
            AUTOTEST_SEQUENCE_BEGIN,
            AUTOTEST_SUITE_BEGIN,
            AUTOTEST_TCPDUMP_BEGIN,
            AUTOTEST_TCPDUMP_DATA,
            AUTOTEST_TCPDUMP_END,
            AUTOTEST_TEST_BEGIN,
            AUTOTEST_TEST_FAILURE,
            AUTOTEST_TEST_SUCCESS,
            BROWSER_BUG_REPORT,
            CARRIER_BUG_REPORT,
            CHECKIN_FAILURE,
            CHECKIN_SUCCESS,
            FOTA_BEGIN,
            FOTA_FAILURE,
            FOTA_INSTALL,
            FOTA_PROMPT,
            FOTA_PROMPT_ACCEPT,
            FOTA_PROMPT_REJECT,
            FOTA_PROMPT_SKIPPED,
            GSERVICES_ERROR,
            GSERVICES_UPDATE,
            LOGIN_SERVICE_ACCOUNT_TRIED,
            LOGIN_SERVICE_ACCOUNT_SAVED,
            LOGIN_SERVICE_AUTHENTICATE,
            LOGIN_SERVICE_CAPTCHA_ANSWERED,
            LOGIN_SERVICE_CAPTCHA_SHOWN,
            LOGIN_SERVICE_PASSWORD_ENTERED,
            LOGIN_SERVICE_SWITCH_GOOGLE_MAIL,
            NETWORK_DOWN,
            NETWORK_UP,
            PHONE_UI,
            RADIO_BUG_REPORT,
            SETUP_COMPLETED,
            SETUP_INITIATED,
            SETUP_IO_ERROR,
            SETUP_NETWORK_ERROR,
            SETUP_REQUIRED_CAPTCHA,
            SETUP_RETRIES_EXHAUSTED,
            SETUP_SERVER_ERROR,
            SETUP_SERVER_TIMEOUT,
            SETUP_NO_DATA_NETWORK,
            SYSTEM_BOOT,
            SYSTEM_LAST_KMSG,
            SYSTEM_RECOVERY_LOG,
            SYSTEM_RESTART,
            SYSTEM_SERVICE_LOOPING,
            SYSTEM_TOMBSTONE,
	    SYSTEM_PANIC,
            TEST,
            BATTERY_DISCHARGE_INFO,
        }
    }

    /**
     * The stats table is a list of counter values indexed by a tag name.
     * Each statistic has a count and sum fields, so it can track averages.
     * When multiple statistics are inserted with the same tag, the count
     * and sum fields are added together into a single entry in the database.
     */
    public interface Stats extends BaseColumns {
        public static final String TABLE_NAME = "stats";
        public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

        public static final String TAG = "tag";      // TEXT UNIQUE
        public static final String COUNT = "count";  // INTEGER
        public static final String SUM = "sum";      // REAL

        /** Valid tag values.  Extend as necessary for your needs. */
        public enum Tag {
            BROWSER_SNAP_CENTER,
            BROWSER_TEXT_SIZE_CHANGE,
            BROWSER_ZOOM_OVERVIEW,

            CRASHES_REPORTED,
            CRASHES_TRUNCATED,
            ELAPSED_REALTIME_SEC,
            ELAPSED_UPTIME_SEC,
            HTTP_REQUEST,
            HTTP_REUSED,
            HTTP_STATUS,
            PHONE_GSM_REGISTERED,
            PHONE_GPRS_ATTEMPTED,
            PHONE_GPRS_CONNECTED,
            PHONE_RADIO_RESETS,
            TEST,
            NETWORK_RX_MOBILE,
            NETWORK_TX_MOBILE,
            PHONE_CDMA_REGISTERED,
            PHONE_CDMA_DATA_ATTEMPTED,
            PHONE_CDMA_DATA_CONNECTED,
            EVENTS_DROPPED,
        }
    }

    /**
	 * The properties table is a set of configurations of the checkin provider 
	 * proper.
     */
    public interface Properties extends BaseColumns {
        public static final String TABLE_NAME = "properties";
        public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

        public static final String TAG = "tag";      // TEXT UNIQUE
        public static final String VALUE = "value";  // TEXT

        /** Valid tag values, to be extended as necessary. */
        public enum Tag {
		  	DESIRED_BUILD,			//legacy android, not used
			EVENT_LIMIT,
			LOG_LEVEL,
        }
    }

    /**
     * The crashes table is a log of crash reports, kept separate from the
     * general event log because crashes are large, important, and bursty.
     * Like the events table, the crashes table is pruned on insert.
     */
    public interface Crashes extends BaseColumns {
        public static final String TABLE_NAME = "crashes";
        public static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

        // TODO: one or both of these should be a file attachment, not a column
        public static final String DATA = "data";    // TEXT
        public static final String LOGS = "logs";    // TEXT
    }

    /**
     * Intents with this action cause a checkin attempt.  Normally triggered by
     * a periodic alarm, these may be sent directly to force immediate checkin.
     */
    public interface TriggerIntent {
        public static final String ACTION = "com.motorola.android.server.checkin.CHECKIN";

        // The category is used for GTalk service messages
        public static final String CATEGORY = "com.motorola.android.server.checkin.CHECKIN";
        
        // If true indicates that the checkin should only transfer market related data
        public static final String EXTRA_MARKET_ONLY = "market_only";
    }

    private static final String TAG = "Checkin";

    /**
     * Helper function to log an event to the database.
     *
     * @param resolver from {@link android.content.Context#getContentResolver}
     * @param tag identifying the type of event being recorded
     * @param value associated with event, if any
     * @return URI of the event that was added
     */
    static public Uri logEvent(ContentResolver resolver,
            Events.Tag tag, String value) {
		return logEvent(resolver, tag.toString(), value);
    }

	static public Uri logEvent(ContentResolver resolver,
			String tag, String value) {
        try {
            // Don't specify the date column; the content provider will add that.
            ContentValues values = new ContentValues();
            values.put(Events.TAG, tag);
            if (value != null) values.put(Events.VALUE, value);
            return resolver.insert(Events.CONTENT_URI, values);
        } catch (IllegalArgumentException e) {  // thrown when provider is unavailable.
            Log.w(TAG, "Can't log event " + tag + ": " + e);
            return null;
        } catch (SQLException e) {
            Log.e(TAG, "Can't log event " + tag, e);  // Database errors are not fatal.
            return null;
        }
	}

    /**
     * Helper function to update statistics in the database.
     * Note that multiple updates to the same tag will be combined.
     *
     * @param tag identifying what is being observed
     * @param count of occurrences
     * @param sum of some value over these occurrences
     * @return URI of the statistic that was returned
     */
    static public Uri updateStats(ContentResolver resolver,
            Stats.Tag tag, int count, double sum) {
        try {
            ContentValues values = new ContentValues();
            values.put(Stats.TAG, tag.toString());
            if (count != 0) values.put(Stats.COUNT, count);
            if (sum != 0.0) values.put(Stats.SUM, sum);
            return resolver.insert(Stats.CONTENT_URI, values);
        } catch (IllegalArgumentException e) {  // thrown when provider is unavailable.
            Log.w(TAG, "Can't update stat " + tag + ": " + e);
            return null;
        } catch (SQLException e) {
            Log.e(TAG, "Can't update stat " + tag, e);  // Database errors are not fatal.
            return null;
        }
    }

	/**
	* A helper function to set properties on the checkin provider.
	*
    * @param resolver from {@link android.content.Context#getContentResolver}
    * @param tag identifying the type of property being set
    * @param value of the property being set
    * @return URI of the event that was added
	*/
	static public Uri setProperty(ContentResolver resolver,
			Properties.Tag tag, String value){
		try {
			ContentValues values = new ContentValues();
			values.put(Properties.TAG, tag.toString());
			values.put(Properties.VALUE, value);
			return resolver.insert(Properties.CONTENT_URI, values);
		} catch (SQLException e){
			Log.e(TAG, "Can't set property " + tag, e); 
			return null;
		}
	} //setProperty

	/** 
	* A helper function to read a property of the checkin provider.
	*
    * @param resolver from {@link android.content.Context#getContentResolver}
    * @param tag identifying the type of property being read
	*
	* @return the value of the propery, null if it does not exist
	*/
	static public String getProperty(ContentResolver resolver, 
			Properties.Tag tag)
	{
		Cursor cursor = null;
		try {
			cursor = resolver.query(Properties.CONTENT_URI, 
									new String[] { Properties.VALUE } , 
									 "tag = ?", 
									new String[] { tag.toString() }, 
									(String) null);
			if (cursor == null) {
				Log.e(TAG, "Can't get property, null tag " + tag);
				return null;
			}
			int value = cursor.getColumnIndex(Properties.VALUE.toString());
				
			while (cursor.moveToNext()){
				return cursor.getString(value);
			}
			return null;

		} catch (Exception e){
			Log.e(TAG, "Can't get property " + tag, e);
			return null;
		}finally {
			if (cursor != null) { cursor.close(); }
		}
	} //getProperty

} //Checkin
