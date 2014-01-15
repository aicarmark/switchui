/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.motorola.contextual.pickers.conditions.calendar;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.MatrixCursor.RowBuilder;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Instances;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.motorola.contextual.smartrules.R;

/**
 * This class implements a custom adapter for list view
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Extends {@link BaseAdapter}
 *      Implements {@link CalendarEventSensorConstants}
 *
 * RESPONSIBILITIES:
 * This class is responsible for providing data for each item in list view.
 * This class performs asynchronous queries to Instances table over a certain
 * interval of days and stores the cursor obtained in DayAdapterInfo
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class AgendaWindowAdapter extends BaseAdapter implements
    CalendarEventSensorConstants {

    /**
     * TAG used for printing logs
     */
    private static final String TAG = "AgendaWindowAdapter";

    /**
     * Tag for header view
     */
    private static final String HEADER_VIEW_TAG = "Header View";

    /**
     * Tag for footer view
     */

    private static final String FOOTER_VIEW_TAG = "Footer View";

    /**
     * Sorting order used while querying the Instances table
     */
    private static final String AGENDA_SORT_ORDER = "startDay ASC, begin ASC, title ASC";

    /**
     * Index for the column Instances.TITLE
     */
    public static final int INDEX_TITLE = 1;

    /**
     * Index for the column Instances.EVENT_LOCATION
     */
    public static final int INDEX_EVENT_LOCATION = 2;

    /**
     * Index for the column Instances.ALL_DAY
     */
    public static final int INDEX_ALL_DAY = 3;

    /**
     * Index for the column Instances.HAS_ALARM
     */
    public static final int INDEX_HAS_ALARM = 4;

    /**
     * Index for the column Instances.COLOR
     */
    public static final int INDEX_COLOR = 5;

    /**
     * Index for the column Instances.RRULE
     */
    public static final int INDEX_RRULE = 6;

    /**
     * Index for the column Instances.BEGIN
     */
    public static final int INDEX_BEGIN = 7;

    /**
     * Index for the column Instances.END
     */
    public static final int INDEX_END = 8;

    /**
     * Index for the column Instances.EVENT_ID
     */
    public static final int INDEX_EVENT_ID = 9;

    /**
     * Index for the column Instances.START_DAY
     */
    public static final int INDEX_START_DAY = 10;

    /**
     * Index for the column Instances.END_DAY
     */
    public static final int INDEX_END_DAY = 11;

    /**
     * Index for the column Instances.SELF_ATTENDEE_STATUS
     */
    public static final int INDEX_SELF_ATTENDEE_STATUS = 12;

    private static final String[] PROJECTION = new String[] { Instances._ID, // 0
            Instances.TITLE, // 1
            Instances.EVENT_LOCATION, // 2
            Instances.ALL_DAY, // 3
            Instances.HAS_ALARM, // 4
            Instances.CALENDAR_COLOR, // 5
            Instances.RRULE, // 6
            Instances.BEGIN, // 7
            Instances.END, // 8
            Instances.EVENT_ID, // 9
            Instances.START_DAY, // 10 Julian start day
            Instances.END_DAY, // 11 Julian end day
            Instances.SELF_ATTENDEE_STATUS, // 12
                                                            };

    /**
     * Used for debugging purpose and setting the selection
     */
    private static final int OFF_BY_ONE_BUG = 1;

    /**
     * Maximum number for {@link DayAdapterInfo} to be stored at a time
     */
    private static final int MAX_NUM_OF_ADAPTERS = 5;

    /**
     * Used for calculating query duration
     */
    private static final int IDEAL_NUM_OF_EVENTS = 50;

    /**
     * Minimum value for query duration
     */
    private static final int MIN_QUERY_DURATION = 7; // days

    /**
     * Maximum value for query duration
     */
    private static final int MAX_QUERY_DURATION = 60; // days

    /**
     * Used for identifying when shall the newer requests be processed
     */
    private static final int PREFETCH_BOUNDARY_NEW = 1;

    /**
     * Used for identifying when shall the older requests be processed
     */
    private static final int PREFETCH_BOUNDARY_OLD = 0;

    /**
     * Times to auto-expand/retry query after getting no dat
     */
    private static final int RETRIES_ON_NO_DATA = 0;

    /**
     * Application's context
     */
    private Context mContext;

    /**
     * {@link QueryHandler} used for asynchronous queries to Instances table
     */
    private QueryHandler mQueryHandler;

    /**
     * Reference to {@link AgendaListView}
     */
    private ListView mAgendaListView;

    /**
     * The sum of the rows in all the adapters
     */
    private int mRowCount;

    /**
     * Number or empty cursors
     */
    private int mEmptyCursorCount;

    /**
     * Cached value of the last used adapter
     */
    private DayAdapterInfo mLastUsedInfo;

    /**
     * {@link LinkedList} for storing {@link DayAdapterInfo} objects
     */
    private LinkedList<DayAdapterInfo> mAdapterInfos = new LinkedList<DayAdapterInfo>();

    /**
     * {@link ConcurrentLinkedQueue} for storing {@link QuerySpec} objects
     */
    private ConcurrentLinkedQueue<QuerySpec> mQueryQueue = new ConcurrentLinkedQueue<QuerySpec>();

    /**
     * List's header view
     */
    private TextView mHeaderView;

    /**
     * List's footer view
     */
    private TextView mFooterView;

    private boolean mDoneSettingUpHeaderFooter = false;

    /**
     * When the user scrolled to the top, a query will be made for older events
     * and this will be incremented. Don't make more requests if mOlderRequests
     * > mOlderRequestsProcessed.
     */
    private int mOlderRequests;

    /**
     * Number of "older" query that has been processed.
     */
    private int mOlderRequestsProcessed;

    /**
     * When the user scrolled to the bottom, a query will be made for newer
     * events and this will be incremented. Don't make more requests if
     * mNewerRequests > mNewerRequestsProcessed.
     */
    private int mNewerRequests;

    /**
     * Number of "newer" query that has been processed.
     */
    private int mNewerRequestsProcessed;

    /**
     * Boolean to identify when list view is getting closed
     */
    private boolean mShuttingDown;

    /**
     * Query for older events
     */
    private static final int QUERY_TYPE_OLDER = 0;

    /**
     * Query for newer events
     */
    private static final int QUERY_TYPE_NEWER = 1;

    /**
     * Delete everything and query around a date
     */
    private static final int QUERY_TYPE_CLEAN = 2;

    /**
     * The selection criteria or where clause used for querying Instances table
     */
    private String mQuerySelection = null;

    /**
     * Boolean to identify if events containing multiple participants should be
     * listed down only
     */
    private boolean mMultipleParticipants = false;

    /**
     * Reference to the currently selected view in the list
     */
    private View mSelectedView;

    /**
     * The type of the currently selected item
     */
    private CurrentItem mCurrentItem = CurrentItem.UNKNOWN_ITEM;

    /**
     * The selected item will have any of the following specified types
     *
     * @author wkh346
     *
     */
    public enum CurrentItem {
        UNKNOWN_ITEM, HEADER_ITEM, EVENT_ITEM, FOOTER_ITEM, POSITIVE_BUTTON_ITEM
    }

    /**
     * This class acts as data structure for holding the query related
     * information
     *
     * <CODE><PRE>
     *
     * RESPONSIBILITIES:
     * This class acts as data structure for holding the query related information
     *
     * COLABORATORS:
     *     SmartProfile - Implements the preconditions available across the system
     *
     * USAGE:
     *     See each method.
     *
     * </PRE></CODE>
     */
    private static class QuerySpec {

        /**
         * start time in milliseconds
         */
        long queryStartMillis;

        /**
         * query start time
         */
        Time goToTime;

        /**
         * start day
         */
        int start;

        /**
         * end day
         */
        int end;

        /**
         * query type
         */
        int queryType;

        /**
         * Constructor
         *
         * @param queryType
         *            - query type
         */
        public QuerySpec(int queryType) {
            this.queryType = queryType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + end;
            result = prime * result
                     + (int) (queryStartMillis ^ (queryStartMillis >>> 32));
            result = prime * result + queryType;
            result = prime * result + start;
            if (goToTime != null) {
                long goToTimeMillis = goToTime.toMillis(false);
                result = prime * result
                         + (int) (goToTimeMillis ^ (goToTimeMillis >>> 32));
            }
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            QuerySpec other = (QuerySpec) obj;
            if (end != other.end || queryStartMillis != other.queryStartMillis
                    || queryType != other.queryType || start != other.start) {
                return false;
            }
            if (goToTime != null) {
                if (goToTime.toMillis(false) != other.goToTime.toMillis(false)) {
                    return false;
                }
            } else {
                if (other.goToTime != null) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * This class acts as data structure for holding the query related
     * information
     *
     * <CODE><PRE>
     *
     * RESPONSIBILITIES:
     * This class acts as data structure for holding the query related information
     *
     * COLABORATORS:
     *     SmartProfile - Implements the preconditions available across the system
     *
     * USAGE:
     *     See each method.
     *
     * </PRE></CODE>
     */
    static class DayAdapterInfo {

        /**
         * Reference to {@link Cursor} obtained on querying Instances table
         */
        Cursor cursor;

        /**
         * Reference to {@link AgendaByDayAdapter}
         */
        AgendaByDayAdapter dayAdapter;

        /**
         * start day of the cursor's coverage
         */
        int start;

        /**
         * end day of the cursor's coverage
         */
        int end;

        /**
         * offset in position in the list view
         */
        int offset;

        /**
         * dayAdapter.getCount()
         */
        int size;

        /**
         * Constructor
         *
         * @param context
         *            - Application's context
         */
        public DayAdapterInfo(Context context) {
            dayAdapter = new AgendaByDayAdapter(context);
        }

        @Override
        public String toString() {
            Time time = new Time();
            StringBuilder sb = new StringBuilder();
            time.setJulianDay(start);
            time.normalize(false);
            sb.append("Start:").append(time.toString());
            time.setJulianDay(end);
            time.normalize(false);
            sb.append(" End:").append(time.toString());
            sb.append(" Offset:").append(offset);
            sb.append(" Size:").append(size);
            return sb.toString();
        }
    }

    /**
     * Constructor
     *
     * @param context
     *            - Application's context
     * @param agendaListView
     *            - Reference to {@link AgendaListView}
     */
    public AgendaWindowAdapter(Context context) {
        mContext = context;
        mQueryHandler = new QueryHandler(context.getContentResolver());

        LayoutInflater inflater = (LayoutInflater) context
                                  .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mHeaderView = (TextView) inflater.inflate(
                          R.layout.agenda_header_footer, null);
        mHeaderView.setTag(HEADER_VIEW_TAG);
        mFooterView = (TextView) inflater.inflate(
                          R.layout.agenda_header_footer, null);
        mFooterView.setTag(FOOTER_VIEW_TAG);
        mHeaderView.setText(R.string.loading);
    }

    /**
     * Method for initializing the adapter with the reference to list view
     *
     * @param listview
     *            - reference to the list view
     */
    public void initialize(ListView listview) {
        mAgendaListView = listview;
        mAgendaListView.addHeaderView(mHeaderView);
    }

    @Override
    public int getViewTypeCount() {
        return AgendaByDayAdapter.TYPE_LAST;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public int getItemViewType(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getItemViewType(position - info.offset);
        } else {
            return -1;
        }
    }

    @Override
    public boolean isEnabled(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.isEnabled(position - info.offset);
        } else {
            return false;
        }
    }


    public int getCount() {
        return mRowCount;
    }


    public Object getItem(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return info.dayAdapter.getItem(position - info.offset);
        } else {
            return null;
        }
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }


    public long getItemId(int position) {
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            return ((position - info.offset) << 20) + info.start;
        } else {
            return -1;
        }
    }


    public View getView(int position, View convertView, ViewGroup parent) {
        if (LOG_DEBUG) {
            Log.d(TAG, "getView position = " + position);
        }
        if (position >= (mRowCount - PREFETCH_BOUNDARY_NEW)
                && mNewerRequests <= mNewerRequestsProcessed) {
            if (LOG_DEBUG)
                Log.d(TAG, "getView queryForNewerEvents: ");
            mNewerRequests++;
            queueQuery(new QuerySpec(QUERY_TYPE_NEWER));
        }

        if (position < PREFETCH_BOUNDARY_OLD
                && mOlderRequests <= mOlderRequestsProcessed) {
            if (LOG_DEBUG)
                Log.d(TAG, "getView queryForOlderEvents: ");
            mOlderRequests++;
            queueQuery(new QuerySpec(QUERY_TYPE_OLDER));
        }

        View v;
        DayAdapterInfo info = getAdapterInfoByPosition(position);
        if (info != null) {
            if (LOG_DEBUG) {
                Log.d(TAG, "getView info = " + info);
            }
            v = info.dayAdapter.getView(position - info.offset, convertView,
                                        parent);
        } else {
            if (LOG_DEBUG) {
                Log.d(TAG, "getView bug at position = " + position);
            }
            TextView tv = new TextView(mContext);
            tv.setText("Bug! " + position);
            v = tv;
        }
        // set on click listener so that the dialog doesn't dismiss when the
        // item is clicked. Setting click able false might not work.
        v.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                // nothing to be done here
            }
        });

        //Explicitly disable sound effects
        v.setSoundEffectsEnabled(false);

        //To not conflict with the scrollbar, we'll add some padding to list elements on the right. Note that we don't want this for the left edge.
        //Also, note that units are in DP
        v.setPadding(0,0,(int)(mContext.getResources().getDimension(R.dimen.agenda_window_adapter_list_item_padding_right)),0);

        return v;
    }

    /**
     * Method for finding {@link DayAdapterInfo} to which the time belongs to
     *
     * @param time
     *            - {@link Time}
     * @return - index of {@link DayAdapterInfo} in mAdapterInfos if an object
     *         is found, -1 otherwise
     */
    private int findDayPositionNearestTime(Time time) {
        DayAdapterInfo info = getAdapterInfoByTime(time);
        if (info != null) {
            return info.offset
                   + info.dayAdapter.findDayPositionNearestTime(time);
        } else {
            return -1;
        }
    }

    /**
     * Method for finding {@link DayAdapterInfo} on the basis of position of an
     * item in list view
     *
     * @param position
     *            - position of an item in list view
     * @return - {@link DayAdapterInfo} in mAdapterInfos if an object is found,
     *         null otherwise
     */
    private DayAdapterInfo getAdapterInfoByPosition(int position) {
        synchronized (mAdapterInfos) {
            if (mLastUsedInfo != null && mLastUsedInfo.offset <= position
                    && position < (mLastUsedInfo.offset + mLastUsedInfo.size)) {
                return mLastUsedInfo;
            }
            for (DayAdapterInfo info : mAdapterInfos) {
                if (info.offset <= position
                        && position < (info.offset + info.size)) {
                    mLastUsedInfo = info;
                    return info;
                }
            }
        }
        return null;
    }

    /**
     * Method for finding {@link DayAdapterInfo} to which the time belongs to
     *
     * @param time
     *            - {@link Time}
     * @return - {@link DayAdapterInfo} in mAdapterInfos if an object is found,
     *         null otherwise
     */
    private DayAdapterInfo getAdapterInfoByTime(Time time) {
        Time tmpTime = new Time(time);
        long timeInMillis = tmpTime.normalize(true);
        int day = Time.getJulianDay(timeInMillis, tmpTime.gmtoff);
        synchronized (mAdapterInfos) {
            for (DayAdapterInfo info : mAdapterInfos) {
                if (info.start <= day && day < info.end) {
                    return info;
                }
            }
        }
        return null;
    }

    /**
     * Method for refreshing the data set in list view
     *
     * @param goToTime
     *            - time to which the selection shall go
     * @param forced
     *            - if true, new query is performed, if false, only set
     *            selection is called
     */
    public void refresh(Time goToTime, boolean forced) {
        int startDay = Time.getJulianDay(goToTime.toMillis(false),
                                         goToTime.gmtoff);

        if (!forced && isInRange(startDay, startDay)) {
            // No need to requery
            mAgendaListView.setSelection(findDayPositionNearestTime(goToTime)
                                         + OFF_BY_ONE_BUG);
            return;
        }

        // Query for a total of MIN_QUERY_DURATION days
        int endDay = startDay + MIN_QUERY_DURATION;

        queueQuery(startDay, endDay, goToTime, QUERY_TYPE_CLEAN);
    }

    /**
     * Method for canceling all query operations
     */
    public void close() {
        mShuttingDown = true;
        pruneAdapterInfo(QUERY_TYPE_CLEAN);
        if (mQueryHandler != null) {
            mQueryHandler.cancelOperation(0);
        }
    }

    /**
     * Method for pruning the {@link DayAdapterInfo} on basis of query type
     *
     * @param queryType
     *            - the query type
     * @return - the {@link DayAdapterInfo}
     */
    private DayAdapterInfo pruneAdapterInfo(int queryType) {
        synchronized (mAdapterInfos) {
            DayAdapterInfo recycleMe = null;
            if (!mAdapterInfos.isEmpty()) {
                if (mAdapterInfos.size() >= MAX_NUM_OF_ADAPTERS) {
                    if (queryType == QUERY_TYPE_NEWER) {
                        recycleMe = mAdapterInfos.removeFirst();
                    } else if (queryType == QUERY_TYPE_OLDER) {
                        recycleMe = mAdapterInfos.removeLast();
                        // Keep the size only if the oldest items are removed.
                        recycleMe.size = 0;
                    }
                    if (recycleMe != null) {
                        if (recycleMe.cursor != null) {
                            recycleMe.cursor.close();
                        }
                        return recycleMe;
                    }
                }

                if (mRowCount == 0 || queryType == QUERY_TYPE_CLEAN) {
                    mRowCount = 0;
                    int deletedRows = 0;
                    DayAdapterInfo info;
                    do {
                        info = mAdapterInfos.poll();
                        if (info != null) {
                            info.cursor.close();
                            deletedRows += info.size;
                            recycleMe = info;
                        }
                    } while (info != null);

                    if (recycleMe != null) {
                        recycleMe.cursor = null;
                        recycleMe.size = deletedRows;
                    }
                }
            }
            return recycleMe;
        }
    }

    /**
     * Method for obtaining selection or where clause for SQL query
     *
     * @return - string containing the where clause
     */
    private String getQuerySelection() {
        if (mQuerySelection == null) {
            mQuerySelection = Instances.VISIBLE + "='1'";
        }
        if (LOG_INFO) {
            Log.i(TAG, "getQuerySelection mQuerySelection = " + mQuerySelection);
        }
        return mQuerySelection;
    }

    /**
     * Method for building URI for querying the Instances table
     *
     * @param start
     *            - the start day
     * @param end
     *            - the end day
     * @return - {@link Uri} containing the URI for querying Instance table
     */
    private Uri buildQueryUri(int start, int end) {
        StringBuilder path = new StringBuilder();
        path.append(start);
        path.append('/');
        path.append(end);
        Uri uri = Uri.withAppendedPath(Instances.CONTENT_BY_DAY_URI,
                                       path.toString());
        if (LOG_DEBUG) {
            Log.d(TAG, "buildQueryUri uri = " + uri);
        }
        return uri;
    }

    /**
     * Method to find out if start and end lie inside the interval for which the
     * events have been queried
     *
     * @param start
     *            - the start day
     * @param end
     *            - the end day
     * @return - true if start and end lie inside the interval, false otherwise
     */
    private boolean isInRange(int start, int end) {
        synchronized (mAdapterInfos) {
            if (mAdapterInfos.isEmpty()) {
                return false;
            }
            return mAdapterInfos.getFirst().start <= start
                   && end <= mAdapterInfos.getLast().end;
        }
    }

    /**
     * Method for calculating the query duration
     *
     * @param start
     *            - the start day
     * @param end
     *            - the end day
     * @return - query duration
     */
    private int calculateQueryDuration(int start, int end) {
        int queryDuration = MAX_QUERY_DURATION;
        if (mRowCount != 0) {
            queryDuration = IDEAL_NUM_OF_EVENTS * (end - start + 1) / mRowCount;
        }

        if (queryDuration > MAX_QUERY_DURATION) {
            queryDuration = MAX_QUERY_DURATION;
        } else if (queryDuration < MIN_QUERY_DURATION) {
            queryDuration = MIN_QUERY_DURATION;
        }
        return queryDuration;
    }

    /**
     * Method for queuing the queries
     *
     * @param start
     *            - start day
     * @param end
     *            - end day
     * @param goToTime
     *            - go to time
     * @param queryType
     *            - query type
     * @return
     */
    private boolean queueQuery(int start, int end, Time goToTime, int queryType) {
        QuerySpec queryData = new QuerySpec(queryType);
        queryData.goToTime = goToTime;
        queryData.start = start;
        queryData.end = end;
        return queueQuery(queryData);
    }

    /**
     * Method for queuing the queries
     *
     * @param queryData
     *            - {@link QuerySpec} object containing query data
     * @return - true if query is queued
     */
    private boolean queueQuery(QuerySpec queryData) {
        Boolean queuedQuery;
        synchronized (mQueryQueue) {
            queuedQuery = false;
            Boolean doQueryNow = mQueryQueue.isEmpty();
            mQueryQueue.add(queryData);
            queuedQuery = true;
            if (doQueryNow) {
                doQuery(queryData);
            }
        }
        return queuedQuery;
    }

    /**
     * Method for starting the asynchronous query operation
     *
     * @param queryData
     *            - {@link QuerySpec} object containing query data
     */
    private void doQuery(QuerySpec queryData) {
        if (!mAdapterInfos.isEmpty()) {
            int start = mAdapterInfos.getFirst().start;
            int end = mAdapterInfos.getLast().end;
            int queryDuration = calculateQueryDuration(start, end);
            switch (queryData.queryType) {
            case QUERY_TYPE_OLDER:
                queryData.end = start - 1;
                queryData.start = queryData.end - queryDuration;
                break;
            case QUERY_TYPE_NEWER:
                queryData.start = end + 1;
                queryData.end = queryData.start + queryDuration;
                break;
            }
        }

        if (LOG_DEBUG) {
            Time time = new Time(Time.getCurrentTimezone());
            time.setJulianDay(queryData.start);
            Time time2 = new Time(Time.getCurrentTimezone());
            time2.setJulianDay(queryData.end);
            Log.d(TAG, "doQuery startQuery: " + time.toString() + " to "
                  + time2.toString() + " then go to " + queryData.goToTime);
        }

        mQueryHandler.cancelOperation(0);
        mQueryHandler.startQuery(0, queryData,
                                 buildQueryUri(queryData.start, queryData.end), PROJECTION,
                                 getQuerySelection(), null, AGENDA_SORT_ORDER);
    }

    /**
     * Method for converting the julianDay into a String containing date
     *
     * @param julianDay
     *            - the julianDay
     * @return - String containing the formatted date
     */
    private String formatDateString(int julianDay) {
        Time time = new Time(Time.getCurrentTimezone());
        time.setJulianDay(julianDay);
        long millis = time.toMillis(false);
        return DateUtils.formatDateRange(mContext, millis, millis,
                                         DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE
                                         | DateUtils.FORMAT_ABBREV_MONTH);
    }

    /**
     * Method for updating the header and footer of the list view
     *
     * @param start
     *            - the start day
     * @param end
     *            - the end day
     * @param queryType
     *            - one of QUERY_TYPE_OLDER, QUERY_TYPE_NEWER, or
     *            QUERY_TYPE_CLEAN
     */
    private void updateHeaderFooter(final int start, final int end,
            int queryType) {
        if (queryType == QUERY_TYPE_OLDER && !mHeaderView.isFocused()) {
            mAgendaListView.requestChildFocus(mHeaderView, mHeaderView);
        }
        mHeaderView.setText(mContext.getString(R.string.show_older_events,
                formatDateString(start)));
        if (queryType == QUERY_TYPE_NEWER && !mFooterView.isFocused()) {
            mAgendaListView.requestChildFocus(mFooterView, mFooterView);
        }
        mFooterView.setText(mContext.getString(R.string.show_newer_events,
                formatDateString(end)));
    }

    /**
     * A helper class to help make handling asynchronous ContentResolver queries
     * easier.
     *
     * <CODE><PRE>
     *
     * CLASS:
     *      Extends {@link AsyncQueryHandler}
     *
     * RESPONSIBILITIES:
     * A helper class to help make handling asynchronous ContentResolver queries easier.
     *
     * COLABORATORS:
     *     SmartProfile - Implements the preconditions available across the system
     *
     * USAGE:
     *     See each method.
     *
     * </PRE></CODE>
     */
    private class QueryHandler extends AsyncQueryHandler {

        /**
         * Constructor
         *
         * @param cr
         *            - {@link ContentResolver}
         */
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie,
                                       Cursor cursorOriginal) {
            if (cursorOriginal == null) {
                Log.e(TAG,
                      "QueryHandler.onQueryComplete cursor obtained is null");
                if (mHeaderView != null) {
                    mHeaderView.setText(R.string.calendar_no_events_found);
                }
                return;
            }
            QuerySpec data = (QuerySpec) cookie;

            if (mShuttingDown) {
                cursorOriginal.close();
                return;
            }

            // CALENDAR_EVENTS_CHANGES - start
            // Changes done for implementing the multiple participants use case.
            // Here we create a matrix cursor by reading data from Instances and
            // Attendees table
            Cursor cursor;
            if (mMultipleParticipants && cursorOriginal.getCount() > 0) {
                // create a cursor which contains events with with multiple
                // participants only
                if (LOG_DEBUG) {
                    Log.d(TAG,
                          "QueryHandler.onQueryComplete multiple participants case");
                }
                MatrixCursor matrixCursor = new MatrixCursor(PROJECTION);
                cursorOriginal.moveToFirst();
                while (!cursorOriginal.isAfterLast()) {
                    long eventId = cursorOriginal.getLong(INDEX_EVENT_ID);
                    String selection = Attendees.EVENT_ID + "='" + eventId
                                       + "'";
                    Cursor attendeesCursor = null;
                    try {
                        attendeesCursor = mContext.getContentResolver().query(
                                              Attendees.CONTENT_URI,
                                              new String[] { Attendees.EVENT_ID,
                                                             Attendees.ATTENDEE_RELATIONSHIP
                                                           },
                                              selection, null, null);
                        if (attendeesCursor != null
                                && attendeesCursor.getCount() > 1) {
                            int id = cursorOriginal.getInt(0);
                            String title = cursorOriginal
                                           .getString(INDEX_TITLE);
                            String location = cursorOriginal
                                              .getString(INDEX_EVENT_LOCATION);
                            int allDay = cursorOriginal.getInt(INDEX_ALL_DAY);
                            int hasAlarm = cursorOriginal
                                           .getInt(INDEX_HAS_ALARM);
                            int color = cursorOriginal.getInt(INDEX_COLOR);
                            String rrule = cursorOriginal
                                           .getString(INDEX_RRULE);
                            long begin = cursorOriginal.getLong(INDEX_BEGIN);
                            long end = cursorOriginal.getLong(INDEX_END);
                            int startDay = cursorOriginal
                                           .getInt(INDEX_START_DAY);
                            int endDay = cursorOriginal.getInt(INDEX_END_DAY);
                            int selfAttendeeStatus = cursorOriginal
                                                     .getInt(INDEX_SELF_ATTENDEE_STATUS);
                            RowBuilder builder = matrixCursor.newRow();
                            builder.add(id).add(title).add(location)
                            .add(allDay).add(hasAlarm).add(color)
                            .add(rrule).add(begin).add(end)
                            .add(eventId).add(startDay).add(endDay)
                            .add(selfAttendeeStatus);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG,
                              "QueryHandler.onQueryComplete failed to query "
                              + Attendees.CONTENT_URI
                              + " with selection " + selection);
                    } finally {
                        if (attendeesCursor != null) {
                            attendeesCursor.close();
                        }
                    }
                    cursorOriginal.moveToNext();
                }
                if (LOG_DEBUG) {
                    matrixCursor.moveToFirst();
                    Log.d(TAG,
                          "QueryHandler.onQueryComplete matrixCursor contents = "
                          + DatabaseUtils
                          .dumpCursorToString(matrixCursor));
                }
                matrixCursor.moveToPosition(-1);
                cursorOriginal.close();
                cursor = matrixCursor;
            } else {
                cursor = cursorOriginal;
            }

            // CALENDAR_EVENTS_CHANGES - end

            // Notify Listview of changes and update position
            int cursorSize = cursor.getCount();
            if (cursorSize > 0 || mAdapterInfos.isEmpty()
                    || data.queryType == QUERY_TYPE_CLEAN) {
                final int listPositionOffset = processNewCursor(data, cursor);
                if (data.goToTime == null) { // Typical Scrolling type query
                    notifyDataSetChanged();
                    if (listPositionOffset != 0) {
                        shiftSelection(listPositionOffset);
                    }
                } else { // refresh() called. Go to the designated position
                    final Time goToTime = data.goToTime;
                    notifyDataSetChanged();
                    int newPosition = findDayPositionNearestTime(goToTime);
                    if (newPosition >= 0) {
                        mAgendaListView.setSelection(newPosition
                                                     + OFF_BY_ONE_BUG);
                    }
                    if (LOG_DEBUG) {
                        Log.d(TAG, "Setting listview to "
                              + "findDayPositionNearestTime: "
                              + (newPosition + OFF_BY_ONE_BUG));
                    }
                }
            } else {
                cursor.close();
            }

            // Update header and footer
            if (!mDoneSettingUpHeaderFooter) {
                OnClickListener headerFooterOnClickListener = new OnClickListener() {
                    public void onClick(View v) {
                        if (v == mHeaderView) {
                            queueQuery(new QuerySpec(QUERY_TYPE_OLDER));
                        } else {
                            queueQuery(new QuerySpec(QUERY_TYPE_NEWER));
                        }
                    }
                };
                mHeaderView.setOnClickListener(headerFooterOnClickListener);
                mFooterView.setOnClickListener(headerFooterOnClickListener);
                mAgendaListView.addFooterView(mFooterView);
                mDoneSettingUpHeaderFooter = true;
            }
            synchronized (mQueryQueue) {
                int totalAgendaRangeStart = -1;
                int totalAgendaRangeEnd = -1;

                if (cursorSize != 0) {
                    // Remove the query that just completed
                    mQueryQueue.poll();
                    mEmptyCursorCount = 0;
                    if (data.queryType == QUERY_TYPE_NEWER) {
                        mNewerRequestsProcessed++;
                    } else if (data.queryType == QUERY_TYPE_OLDER) {
                        mOlderRequestsProcessed++;
                    }

                    totalAgendaRangeStart = mAdapterInfos.getFirst().start;
                    totalAgendaRangeEnd = mAdapterInfos.getLast().end;
                } else { // CursorSize == 0
                    QuerySpec querySpec = mQueryQueue.peek();

                    // Update Adapter Info with new start and end date range
                    if (!mAdapterInfos.isEmpty()) {
                        DayAdapterInfo first = mAdapterInfos.getFirst();
                        DayAdapterInfo last = mAdapterInfos.getLast();

                        if (first.start - 1 <= querySpec.end
                                && querySpec.start < first.start) {
                            first.start = querySpec.start;
                        }

                        if (querySpec.start <= last.end + 1
                                && last.end < querySpec.end) {
                            last.end = querySpec.end;
                        }

                        totalAgendaRangeStart = first.start;
                        totalAgendaRangeEnd = last.end;
                    } else {
                        totalAgendaRangeStart = querySpec.start;
                        totalAgendaRangeEnd = querySpec.end;
                    }

                    // Update query specification with expanded search range
                    // and maybe rerun query
                    switch (querySpec.queryType) {
                    case QUERY_TYPE_OLDER:
                        totalAgendaRangeStart = querySpec.start;
                        querySpec.start -= MAX_QUERY_DURATION;
                        break;
                    case QUERY_TYPE_NEWER:
                        totalAgendaRangeEnd = querySpec.end;
                        querySpec.end += MAX_QUERY_DURATION;
                        break;
                    case QUERY_TYPE_CLEAN:
                        totalAgendaRangeStart = querySpec.start;
                        totalAgendaRangeEnd = querySpec.end;
                        querySpec.start -= MAX_QUERY_DURATION / 2;
                        querySpec.end += MAX_QUERY_DURATION / 2;
                        break;
                    }

                    if (++mEmptyCursorCount > RETRIES_ON_NO_DATA) {
                        // Nothing in the cursor again. Dropping query
                        mQueryQueue.poll();
                    }
                }

                updateHeaderFooter(totalAgendaRangeStart, totalAgendaRangeEnd,
                        data.queryType);

                // Fire off the next query if any
                Iterator<QuerySpec> it = mQueryQueue.iterator();
                while (it.hasNext()) {
                    QuerySpec queryData = it.next();
                    if (!isInRange(queryData.start, queryData.end)) {
                        // Query accepted
                        if (LOG_DEBUG)
                            Log.d(TAG, "Query accepted. QueueSize:"
                                  + mQueryQueue.size());
                        doQuery(queryData);
                        break;
                    } else {
                        // Query rejected
                        it.remove();
                        if (LOG_DEBUG)
                            Log.d(TAG, "Query rejected. QueueSize:"
                                  + mQueryQueue.size());
                    }
                }
            }
            if (LOG_INFO) {
                for (DayAdapterInfo info3 : mAdapterInfos) {
                    Log.i(TAG, "> " + info3.toString());
                }
            }
        }

        /**
         * Update the adapter info array with a the new cursor. Close out old
         * cursors as needed.
         *
         * @return number of rows removed from the beginning
         */
        private int processNewCursor(QuerySpec data, Cursor cursor) {
            synchronized (mAdapterInfos) {
                // Remove adapter info's from adapterInfos as needed
                DayAdapterInfo info = pruneAdapterInfo(data.queryType);
                int listPositionOffset = 0;
                if (info == null) {
                    info = new DayAdapterInfo(mContext);
                } else {
                    if (LOG_DEBUG)
                        Log.d(TAG, "processNewCursor listPositionOffsetA="
                              + -info.size);
                    listPositionOffset = -info.size;
                }

                // Setup adapter info
                info.start = data.start;
                info.end = data.end;
                info.cursor = cursor;
                info.dayAdapter.changeCursor(info);
                info.size = info.dayAdapter.getCount();

                // Insert into adapterInfos
                if (mAdapterInfos.isEmpty()
                        || data.end <= mAdapterInfos.getFirst().start) {
                    mAdapterInfos.addFirst(info);
                    listPositionOffset += info.size;
                } else {
                    mAdapterInfos.addLast(info);
                }

                // Update offsets in adapterInfos
                mRowCount = 0;
                for (DayAdapterInfo info3 : mAdapterInfos) {
                    info3.offset = mRowCount;
                    mRowCount += info3.size;
                }
                mLastUsedInfo = null;
                return listPositionOffset;
            }
        }
    }

    // CALENDAR_EVENTS_CHANGES - start

    /**
     * Method for setting the query selection used while performing SQL based
     * query on Instances table
     *
     * @param querySelection
     *            - SQl query selection String build on the basis of user
     *            selections
     */
    public void setQuerySelection(String querySelection) {
        mQuerySelection = querySelection;
    }

    /**
     * Method for telling the adapter whether user has selected Multiple
     * Participants option
     *
     * @param multipleParticipants
     *            - true if user has selected multiple participants, false
     *            otherwise
     */
    public void setMultipleParticipants(boolean multipleParticipants) {
        mMultipleParticipants = multipleParticipants;
    }

    /**
     * Method for moving the currently selected or visible focus down by offset
     * amount
     *
     * @param offset
     *            - the offset amount
     */
    public void shiftSelection(int offset) {
        shiftPosition(offset);
        int position = mAgendaListView.getSelectedItemPosition();
        if (position != ListView.INVALID_POSITION) {
            mAgendaListView.setSelectionFromTop(position + offset, 0);
        }
    }

    /**
     * Method for setting selection of list view by offset amount
     *
     * @param offset
     *            - the offset amount
     */
    private void shiftPosition(int offset) {
        if (LOG_DEBUG) {
            Log.d(TAG, "shiftPosition " + offset);
        }

        View firstVisibleItem = getFirstVisibleView();

        if (firstVisibleItem != null) {
            Rect r = new Rect();
            firstVisibleItem.getLocalVisibleRect(r);
            // if r.top is < 0, getChildAt(0) and getFirstVisiblePosition() is
            // returning an item above the first visible item.
            int position = mAgendaListView.getPositionForView(firstVisibleItem);
            mAgendaListView.setSelectionFromTop(position + offset,
                                                r.top > 0 ? -r.top : r.top);
        } else if (mAgendaListView.getSelectedItemPosition() >= 0) {
            if (LOG_DEBUG) {
                Log.d(TAG, "shiftPosition Shifting selection from "
                      + mAgendaListView.getSelectedItemPosition() + " by "
                      + offset);
            }
            mAgendaListView.setSelection(mAgendaListView
                                         .getSelectedItemPosition() + offset);
        }
    }

    /**
     * Method for obtaining the first visible item in list view
     *
     * @return - the {@link View} representing the first visible item in the
     *         list
     */
    public View getFirstVisibleView() {
        Rect r = new Rect();
        int childCount = mAgendaListView.getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View listItem = mAgendaListView.getChildAt(i);
            if (listItem != null) {
                listItem.getLocalVisibleRect(r);
            }
            if (r.top >= 0) { // if visible
                return listItem;
            }
        }
        return null;
    }

    /**
     * This method performs the query based upon the key events received by the
     * dialog
     *
     * @param keyCode
     *            - the key code
     * @param event
     *            - the key event
     */
    public void onKeyPressed(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_ENTER:
        case KeyEvent.KEYCODE_DPAD_CENTER: {
            if (mSelectedView != null
                    && (mCurrentItem == CurrentItem.HEADER_ITEM || mCurrentItem == CurrentItem.FOOTER_ITEM)) {
                if (mSelectedView.findViewWithTag(HEADER_VIEW_TAG) != null) {
                    queueQuery(new QuerySpec(QUERY_TYPE_OLDER));
                } else if (mSelectedView.findViewWithTag(FOOTER_VIEW_TAG) != null) {
                    queueQuery(new QuerySpec(QUERY_TYPE_NEWER));
                } else {
                    mCurrentItem = CurrentItem.EVENT_ITEM;
                }
            }
            break;
        }
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_DPAD_DOWN: {
            if (mSelectedView != null) {
                if (mSelectedView.findViewWithTag(HEADER_VIEW_TAG) != null) {
                    mCurrentItem = CurrentItem.HEADER_ITEM;
                } else if (mSelectedView.findViewWithTag(FOOTER_VIEW_TAG) != null) {
                    if (mCurrentItem == CurrentItem.FOOTER_ITEM) {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                            mCurrentItem = CurrentItem.POSITIVE_BUTTON_ITEM;
                        }
                    } else if (mCurrentItem == CurrentItem.POSITIVE_BUTTON_ITEM) {
                        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                            mCurrentItem = CurrentItem.FOOTER_ITEM;
                        }
                    } else {
                        mCurrentItem = CurrentItem.FOOTER_ITEM;
                    }
                } else {
                    mCurrentItem = CurrentItem.EVENT_ITEM;
                }
            } else {
                mCurrentItem = CurrentItem.UNKNOWN_ITEM;
            }
            break;
        }
        }
        if (LOG_INFO) {
            Log.i(TAG, "onKeyPressed mCurrentItem = " + mCurrentItem);
        }
    }

    /**
     * This methods sets the currently selected view
     *
     * @param adapterView
     *            - the list view
     * @param view
     *            - the selected item view
     * @param position
     *            - position of the selected item view in the list
     * @param rowId
     *            - rowId of the selected item view in the list
     */
    public void onItemSelected(AdapterView<?> adapterView, View view,
                               int position, long rowId) {
        mSelectedView = view;
    }

    /**
     * This method returns the type of the currently selected item
     *
     * @return - the type of the currently selected item
     */
    public CurrentItem getCurrentSelectedItemType() {
        return mCurrentItem;
    }

    /**
     * This method initializes the references that are no longer valid
     */
    public void onDialogDismiss() {
        mSelectedView = null;
        mCurrentItem = CurrentItem.UNKNOWN_ITEM;
    }

    // CALENDAR_EVENTS_CHANGES - end

}
