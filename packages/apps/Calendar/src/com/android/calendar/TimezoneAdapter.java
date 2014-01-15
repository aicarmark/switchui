/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.android.calendar.TimezoneAdapter.TimezoneRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * {@link TimezoneAdapter} is a custom adapter implementation that allows you to
 * easily display a list of timezones for users to choose from. In addition, it
 * provides a two-stage behavior that initially only loads a small set of
 * timezones (one user-provided, the device timezone, and two recent timezones),
 * which can later be expanded into the full list with a call to
 * {@link #showAllTimezones()}.
 */
public class TimezoneAdapter extends ArrayAdapter<TimezoneRow> {
    private static final String TAG = "TimezoneAdapter";

    /**
     * {@link TimezoneRow} is an immutable class for representing a timezone. We
     * don't use {@link TimeZone} directly, in order to provide a reasonable
     * implementation of toString() and to control which display names we use.
     */
    public static class TimezoneRow implements Comparable<TimezoneRow> {

        /** The ID of this timezone, e.g. "America/Los_Angeles" */
        public final String mId;

        /** The display name of this timezone, e.g. "Pacific Time" */
        private final String mDisplayName;

        /** The actual offset of this timezone from GMT in milliseconds */
        private final int mOffset;

        /** Whether the TZ observes daylight saving time */
        private final boolean mUseDaylightTime;

        /**
         * A one-line representation of this timezone, including both GMT offset
         * and display name, e.g. "(GMT-7:00) Pacific Time"
         */
        private String mGmtDisplayName;

        public TimezoneRow(String id, String displayName) {
            mId = id;
            mDisplayName = displayName;
            TimeZone tz = TimeZone.getTimeZone(id);
            mUseDaylightTime = tz.useDaylightTime();
            if (mTime == -1 || mTime == 0) {
                mTime = System.currentTimeMillis();
            }
            mOffset = tz.getOffset(mTime);
            buildGmtDisplayName();
        }

        @Override
        public String toString() {
            if (mGmtDisplayName == null) {
                buildGmtDisplayName();
            }

            return mGmtDisplayName;
        }

        /**
         *
         */
        public void buildGmtDisplayName() {
            if (mGmtDisplayName != null) {
                return;
            }

            int p = Math.abs(mOffset);
            StringBuilder name = new StringBuilder();
            name.append("GMT");

            if (mOffset < 0) {
                name.append('-');
            } else {
                name.append('+');
            }

            name.append(p / (DateUtils.HOUR_IN_MILLIS));
            name.append(':');

            int min = p / 60000;
            min %= 60;

            if (min < 10) {
                name.append('0');
            }
            name.append(min);
            name.insert(0, "(");
            name.append(") ");
            name.append(mDisplayName);
            if (mUseDaylightTime) {
                name.append(" \u2600"); // Sun symbol
            }
            mGmtDisplayName = name.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((mDisplayName == null) ? 0 : mDisplayName.hashCode());
            result = prime * result + ((mId == null) ? 0 : mId.hashCode());
            result = prime * result + mOffset;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            TimezoneRow other = (TimezoneRow) obj;
            if (mDisplayName == null) {
                if (other.mDisplayName != null) {
                    return false;
                }
            } else if (!mDisplayName.equals(other.mDisplayName)) {
                return false;
            }
            if (mId == null) {
                if (other.mId != null) {
                    return false;
                }
            } else if (!mId.equals(other.mId)) {
                return false;
            }
            if (mOffset != other.mOffset) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(TimezoneRow another) {
            if (mOffset == another.mOffset) {
                return 0;
            } else {
                return mOffset < another.mOffset ? -1 : 1;
            }
        }

    }

    private static final String KEY_RECENT_TIMEZONES = "preferences_recent_timezones";

    /** The delimiter we use when serializing recent timezones to shared preferences */
    private static final String RECENT_TIMEZONES_DELIMITER = ",";

    /** The maximum number of recent timezones to save */
    private static final int MAX_RECENT_TIMEZONES = 3;

    /**
     * Static cache of all known timezones, mapped to their string IDs. This is
     * lazily-loaded on the first call to {@link #loadFromResources(Resources)}.
     * Loading is called in a synchronized block during initialization of this
     * class and is based off the resources available to the calling context.
     * This class should not be used outside of the initial context.
     * LinkedHashMap is used to preserve ordering.
     */
    private static LinkedHashMap<String, TimezoneRow> sTimezones = new LinkedHashMap<String, TimezoneRow>();
    private static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private static final Lock r = rwl.readLock();
    private static final Lock w = rwl.writeLock();

    private Context mContext;

    private String mCurrentTimezone;

    private boolean mShowingAll = false;

    private static long mTime;

    private static Date mDateTime;

    /**
     * Constructs a timezone adapter that contains an initial set of entries
     * including the current timezone, the device timezone, and two recently
     * used timezones.
     *
     * @param context
     * @param currentTimezone
     * @param time - needed to determine whether DLS is in effect
     */
    public TimezoneAdapter(Context context, String currentTimezone, long time) {
        super(context, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1);
        mContext = context;
        mCurrentTimezone = currentTimezone;
        mTime = time;
        mDateTime = new Date(mTime);
        mShowingAll = false;
        showInitialTimezones();
    }

    /**
     * Given the ID of a timezone, returns the position of the timezone in this
     * adapter, or -1 if not found.
     *
     * @param id the ID of the timezone to find
     * @return the row position of the timezone, or -1 if not found
     */
    public int getRowById(String id) {
//        TimezoneRow timezone = sTimezones.get(id);
//        if (timezone == null) {
//            return -1;
//        } else {
//            return getPosition(timezone);
//        }
        TimezoneRow timezoneRow = null;
        r.lock(); // read lock
        try {
            timezoneRow = sTimezones.get(id);
        } finally {
            r.unlock();
        }

        if (timezoneRow == null) {
            return -1;
        }
        return getPosition(timezoneRow);
    }
    //Begin Motorola
    /**
     * Populates the adapter with an initial list of timezones (one
     * user-provided, the device timezone, and two recent timezones), which can
     * later be expanded into the full list with a call to
     * {@link #showAllTimezones()}.
     *
     * @param currentTimezone
     */
    public void showInitialTimezones() {
        // Try to initialize the timezone list
        Set<String> ids = initTimezones(mContext, mCurrentTimezone);

        // Clear the adapter
        clear();

        // Add current and recent timezones into adapter.
        r.lock(); // read lock
        try {
            for (String id : ids) {
                if (sTimezones.get(id) != null) {
                    add(sTimezones.get(id));
                }
            }
        } finally {
            r.unlock();
        }
        mShowingAll = false;
    }

    /**
     * Initialize the cached timezones in <code>TimezoneAdaper</code>.
     * @param context The context
     * @param currentTimezone Current timezone id.
     * @return A set of timezone id's, including current and recent timezone id.
     */
    public static Set<String> initTimezones(Context mContext, String mCurrentTimezone) {
        // we use a linked hash set to guarantee only unique IDs are added, and
        // also to maintain the insertion order of the timezones
        LinkedHashSet<String> ids = new LinkedHashSet<String>();

        // add in the provided (event) timezone
        if (!TextUtils.isEmpty(mCurrentTimezone)) {
            ids.add(mCurrentTimezone);
        }

        // add in the device timezone if it is different
        ids.add(TimeZone.getDefault().getID());

        // add in recent timezone selections
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(mContext);
        String recentsString = prefs.getString(KEY_RECENT_TIMEZONES, null);
        if (recentsString != null) {
            String[] recents = recentsString.split(RECENT_TIMEZONES_DELIMITER);
            for (String recent : recents) {
                if (!TextUtils.isEmpty(recent)) {
                    ids.add(recent);
                }
            }
        }

        // After preparing the ids, now start to init the sTimezones
        w.lock(); // write lock
        try {
            // 1. load sTimezones from resources (id, label)
            loadFromResources(mContext.getResources());
            // 2. load recent timezones
            TimeZone gmt = TimeZone.getTimeZone("GMT");
            for (String id : ids) {
                if (!sTimezones.containsKey(id)) {
                    // a timezone we don't know about, so try to add it...
                    TimeZone newTz = TimeZone.getTimeZone(id);
                    // since TimeZone.getTimeZone actually returns a clone of GMT
                    // when it doesn't recognize the ID, this appears to be the only
                    // reliable way to check to see if the ID is a valid timezone
                    if (!newTz.equals(gmt)) {
                        if (mDateTime == null) {
                            sTimezones.put(id, new TimezoneRow(id, newTz.getDisplayName()));
                        }else {
                            final String tzDisplayName = newTz.getDisplayName(
                                    newTz.inDaylightTime(mDateTime), TimeZone.LONG,
                                    Locale.getDefault());
                            sTimezones.put(id, new TimezoneRow(id, tzDisplayName));
                        }
                    } else {
                        continue;
                    }
                }
            }
        } finally {
            w.unlock();
        }
        return ids;
    }

    /**
     * Populates this adapter with all known timezones.
     */
    public void showAllTimezones() {
//        List<TimezoneRow> timezones = new ArrayList<TimezoneRow>(sTimezones.values());
//        Collections.sort(timezones);
//        clear();
//        for (TimezoneRow timezone : timezones) {
//            timezone.buildGmtDisplayName();
//            add(timezone);
//        }
//        mShowingAll = true;
        List<TimezoneRow> timezones = null;
        r.lock(); // read lock
        try {
            timezones = new ArrayList<TimezoneRow>(sTimezones.values());
        } finally {
            r.unlock();
        }
        Collections.sort(timezones);
        clear();
        for (TimezoneRow timezone : timezones) {
            add(timezone);
        }
        mShowingAll = true;
    }

    /**
     * Sets the current timezone. If the adapter is currently displaying only a
     * subset of views, reload that view since it may have changed.
     *
     * @param currentTimezone the current timezone
     */
    public void setCurrentTimezone(String currentTimezone) {
        if (currentTimezone != null && !currentTimezone.equals(mCurrentTimezone)) {
            mCurrentTimezone = currentTimezone;
            if (!mShowingAll) {
                showInitialTimezones();
            }
        }
    }

    /**
     * Set the time for the adapter and update the display string appropriate
     * for the time of the year e.g. standard time vs daylight time
     *
     * @param time
     */
    public void setTime(long time) {
        if (time != mTime) {
            mTime = time;
            mDateTime.setTime(mTime);
            sTimezones = null;
            showInitialTimezones();
        }
    }

    /**
     * Saves the given timezone ID as a recent timezone under shared
     * preferences. If there are already the maximum number of recent timezones
     * saved, it will remove the oldest and append this one.
     *
     * @param id the ID of the timezone to save
     * @see {@link #MAX_RECENT_TIMEZONES}
     */
    public void saveRecentTimezone(String id) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(mContext);
        String recentsString = prefs.getString(KEY_RECENT_TIMEZONES, null);
        List<String> recents;
        if (recentsString == null) {
            recents = new ArrayList<String>(MAX_RECENT_TIMEZONES);
        } else {
            recents = new ArrayList<String>(
                Arrays.asList(recentsString.split(RECENT_TIMEZONES_DELIMITER)));
        }

        while (recents.size() >= MAX_RECENT_TIMEZONES) {
            recents.remove(0);
        }
        recents.add(id);
        recentsString = Utils.join(recents, RECENT_TIMEZONES_DELIMITER);
        Utils.setSharedPreference(mContext, KEY_RECENT_TIMEZONES, recentsString);
    }

    /**
     * Returns an array of ids/time zones. This returns a double indexed array
     * of ids and time zones for Calendar. It is an inefficient method and
     * shouldn't be called often, but can be used for one time generation of
     * this list.
     *
     * @return double array of tz ids and tz names
     */
    public static CharSequence[][] getAllTimezones() {
//        CharSequence[][] timeZones = new CharSequence[2][sTimezones.size()];
//        List<String> ids = new ArrayList<String>(sTimezones.keySet());
//        List<TimezoneRow> timezones = new ArrayList<TimezoneRow>(sTimezones.values());
//        int i = 0;
//        for (TimezoneRow row : timezones) {
//            timeZones[0][i] = ids.get(i);
//            timeZones[1][i++] = row.toString();
//        }
//        return timeZones;
        CharSequence[][] timeZones = null;
        r.lock(); // read lock
        try {
            timeZones = new CharSequence[2][sTimezones.size()];
            List<String> ids = new ArrayList<String>(sTimezones.keySet());
            List<TimezoneRow> timezoneRows = new ArrayList<TimezoneRow>(sTimezones.values());

            int i = 0;
            for (TimezoneRow row : timezoneRows) {
                timeZones[0][i] = ids.get(i);
                timeZones[1][i++] = row.toString();
            }
        } finally {
            r.unlock();
        }
        return timeZones;
    }

    private static void loadFromResources(Resources resources) {
        if (sTimezones == null || sTimezones.isEmpty()) {
            String[] ids = resources.getStringArray(R.array.timezone_values);
            String[] labels = resources.getStringArray(R.array.timezone_labels);

            int length = ids.length;
            sTimezones = new LinkedHashMap<String, TimezoneRow>(length);

            if (ids.length != labels.length) {
                Log.wtf(TAG, "ids length (" + ids.length + ") and labels length(" + labels.length +
                        ") should be equal but aren't.");
            }
            for (int i = 0; i < length; i++) {
                sTimezones.put(ids[i], new TimezoneRow(ids[i], labels[i]));
            }
        }
    }

    /**
     * @return true if the cached timezone is empty; false otherwise.
     */
    public static boolean timezonesEmpty() {
        r.lock(); // read lock
        try {
            return sTimezones.isEmpty();
        } finally {
            r.unlock();
        }
    }

    //Begin Motorola
    /**
     * Reload all the cached <code>TimezoneRow</code>s.
     */
    public static void renewTimezones() {
        w.lock(); // write lock
        try {
            Set<String> tzKeys = sTimezones.keySet();
            for (String id : tzKeys) {
                TimezoneRow tzRow = sTimezones.get(id);
                if (tzRow != null) {
                    // renew the timezone row
                    sTimezones.put(id, new TimezoneRow(tzRow.mId, tzRow.mDisplayName));
                }
            }
        } finally {
            w.unlock();
        }
    }
    /**
     * Clear all the cached <code>TimezoneRow</code>s and make it empty.
     */
    public static void renewTimezonesLocales() {
        w.lock(); // write lock
        try {
            sTimezones.clear();
        } finally {
            w.unlock();
        }
    }
    //End Motorola
}
