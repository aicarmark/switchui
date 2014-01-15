/*
 * Copyright (C) 2008 The Android Open Source Project
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

import com.motorola.contextual.pickers.conditions.calendar.AgendaWindowAdapter.DayAdapterInfo;
import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.database.Cursor;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This class is responsible for storing and retrieving information about each
 * item of list view
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Extends {@link BaseAdapter}
 *      Implements {@link CalendarEventSensorConstants}
 *
 * RESPONSIBILITIES:
 * This class is responsible for storing and retrieving information about each item of list view
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class AgendaByDayAdapter extends BaseAdapter implements
    CalendarEventSensorConstants {
    /**
     * The event type
     */
    private static final int TYPE_MEETING = 1;

    /**
     * Types of item in list
     */
    static final int TYPE_LAST = 1;

    /**
     * Reference to {@link AgendaAdapter}
     */
    private final AgendaAdapter mAgendaAdapter;

    /**
     * {@link ArrayList} for holding information about each item in list view
     */
    private ArrayList<RowInfo> mRowInfo;

    /**
     * TAG used for printing logs
     */
    private static final String TAG = "AgendaByDayAdapter";

    /**
     * Constructor
     *
     * @param context
     *            - Application's context
     */
    public AgendaByDayAdapter(Context context) {
        mAgendaAdapter = new AgendaAdapter(context, R.layout.agenda_item);
    }


    public int getCount() {
        if (mRowInfo != null) {
            return mRowInfo.size();
        }
        return mAgendaAdapter.getCount();
    }

    public Object getItem(int position) {
        if (mRowInfo != null) {
            RowInfo row = mRowInfo.get(position);
            return mAgendaAdapter.getItem(row.mData);
        }
        return mAgendaAdapter.getItem(position);
    }

 
    public long getItemId(int position) {
        if (mRowInfo != null) {
            RowInfo row = mRowInfo.get(position);
            return mAgendaAdapter.getItemId(row.mData);
        }
        return mAgendaAdapter.getItemId(position);
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_LAST;
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_MEETING;
    }


    public View getView(int position, View convertView, ViewGroup parent) {
        if (LOG_DEBUG) {
            Log.d(TAG, "getView position = " + position);
        }
        if ((mRowInfo == null) || (position > mRowInfo.size())) {
            // If we have no row info, mAgendaAdapter returns the view.
            if (LOG_DEBUG) {
                Log.d(TAG,
                      "getView getting view from mAgendaAdapter because no row is available");
            }
            return mAgendaAdapter.getView(position, convertView, parent);
        }

        RowInfo row = mRowInfo.get(position);
        if (row.mType == TYPE_MEETING) {
            View view = mAgendaAdapter.getView(row.mData, convertView, parent);
            if (LOG_DEBUG) {
                Log.d(TAG,
                      "getView getting view from mAgendaAdapter because row.mType is TYPE_MEETING");
            }
            return view;
        } else {
            // Error
            throw new IllegalStateException("Unknown event type:" + row.mType);
        }
    }

    /**
     * Method for changing the cursor associated with {@link AgendaAdapter}
     *
     * @param info
     *            - {@link DayAdapterInfo}
     */
    public void changeCursor(DayAdapterInfo info) {
        calculateDays(info);
        mAgendaAdapter.changeCursor(info.cursor);
    }

    /**
     * Method for storing information about each event obtained on reading
     * cursor
     *
     * @param dayAdapterInfo
     *            - {@link DayAdapterInfo}
     */
    public void calculateDays(DayAdapterInfo dayAdapterInfo) {
        Cursor cursor = dayAdapterInfo.cursor;
        ArrayList<RowInfo> rowInfo = new ArrayList<RowInfo>();
        int prevStartDay = -1;
        LinkedList<MultipleDayInfo> multipleDayList = new LinkedList<MultipleDayInfo>();
        for (int position = 0; cursor.moveToNext(); position++) {
            int startDay = cursor.getInt(AgendaWindowAdapter.INDEX_START_DAY);

            // Skip over the days outside of the adapter's range
            startDay = Math.max(startDay, dayAdapterInfo.start);

            if (startDay != prevStartDay) {
                // Check if we skipped over any empty days
                if (prevStartDay != -1) {
                    // If there are any multiple-day events that span the empty
                    // range of days, then create meeting events for
                    // those multiple-day events.
                    for (int currentDay = prevStartDay + 1; currentDay <= startDay; currentDay++) {
                        Iterator<MultipleDayInfo> iter = multipleDayList
                                                         .iterator();
                        while (iter.hasNext()) {
                            MultipleDayInfo info = iter.next();
                            // If this event has ended then remove it from the
                            // list.
                            if (info.mEndDay < currentDay) {
                                iter.remove();
                                continue;
                            }
                            rowInfo.add(new RowInfo(TYPE_MEETING,
                                                    info.mPosition));
                        }
                    }
                }
                prevStartDay = startDay;
            }

            // Add in the event for this cursor position
            rowInfo.add(new RowInfo(TYPE_MEETING, position));

            // If this event spans multiple days, then add it to the multipleDay
            // list.
            int endDay = cursor.getInt(AgendaWindowAdapter.INDEX_END_DAY);

            // Skip over the days outside of the adapter's range
            endDay = Math.min(endDay, dayAdapterInfo.end);
            if (endDay > startDay) {
                multipleDayList.add(new MultipleDayInfo(position, endDay));
            }
        }

        // There are no more cursor events but we might still have multiple-day
        // events left. So create meeting events for those.
        if (prevStartDay > 0) {
            for (int currentDay = prevStartDay + 1; currentDay <= dayAdapterInfo.end; currentDay++) {
                Iterator<MultipleDayInfo> iter = multipleDayList.iterator();
                while (iter.hasNext()) {
                    MultipleDayInfo info = iter.next();
                    // If this event has ended then remove it from the
                    // list.
                    if (info.mEndDay < currentDay) {
                        iter.remove();
                        continue;
                    }
                    rowInfo.add(new RowInfo(TYPE_MEETING, info.mPosition));
                }
            }
        }
        mRowInfo = rowInfo;
    }

    /**
     * This class represents the data structure for holding information about an
     * item in list view
     *
     * <CODE><PRE>
     *
     * RESPONSIBILITIES:
     * This class is responsible for storing information about an item in list view
     *
     * COLABORATORS:
     *     SmartProfile - Implements the preconditions available across the system
     *
     * USAGE:
     *     See each method.
     *
     * </PRE></CODE>
     */
    static class RowInfo {
        /**
         * The type of the event
         */
        // Shall be TYPE_MEETING always
        final int mType;

        /**
         * The cursor position of this event
         */
        final int mData;

        RowInfo(int type, int data) {
            mType = type;
            mData = data;
        }
    }

    /**
     * This class represents the data structure for holding information about a
     * multiple day event
     *
     * <CODE><PRE>
     *
     * RESPONSIBILITIES:
     * This class is responsible for storing information about about a multiple day event
     *
     * COLABORATORS:
     *     SmartProfile - Implements the preconditions available across the system
     *
     * USAGE:
     *     See each method.
     *
     * </PRE></CODE>
     */
    private static class MultipleDayInfo {
        /**
         * Cursor position for this event
         */
        final int mPosition;

        /**
         * End day of this event
         */
        final int mEndDay;

        /**
         * Constructor
         *
         * @param position
         *            - Cursor position for this event
         * @param endDay
         *            - End day of this event
         */
        MultipleDayInfo(int position, int endDay) {
            mPosition = position;
            mEndDay = endDay;
        }
    }

    /**
     * Searches for the day that matches the given Time object and returns the
     * list position of that day. If there are no events for that day, then it
     * finds the nearest day (before or after) that has events and returns the
     * list position for that day.
     *
     * @param time
     *            the date to search for
     * @return the cursor position of the first event for that date, or zero if
     *         no match was found
     */
    public int findDayPositionNearestTime(Time time) {
        // This function doesn't work any more since day header has been removed
        return 0;
    }

    /**
     * Converts a list position to a cursor position. The list contains day
     * headers as well as events. The cursor contains only events.
     *
     * @param listPos
     *            the list position of an event
     * @return the corresponding cursor position of that event
     */
    public int getCursorPosition(int listPos) {
        if (mRowInfo != null && listPos >= 0) {
            RowInfo row = mRowInfo.get(listPos);
            if (row.mType == TYPE_MEETING) {
                return row.mData;
            } else {
                int nextPos = listPos + 1;
                if (nextPos < mRowInfo.size()) {
                    nextPos = getCursorPosition(nextPos);
                    if (nextPos >= 0) {
                        return -nextPos;
                    }
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }
}
