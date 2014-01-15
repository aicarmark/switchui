/*
 * Copyright (C) 2007 The Android Open Source Project
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

import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.provider.CalendarContract.Attendees;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

/**
 * This class is responsible for binding data to each of item of list view
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Extends {@link ResourceCursorAdapter}
 *
 * RESPONSIBILITIES:
 * This class is responsible for binding data to each of item of list view
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class AgendaAdapter extends ResourceCursorAdapter {
    /**
     * String for holding the "No Title" label
     */
    private String mNoTitleLabel;

    /**
     * Resources instance for application's package.
     */
    private Resources mResources;

    /**
     * Color value for shading the declined item
     */
    private int mDeclinedColor;

    /**
     * Used for indicating start time of an event
     */
    private static final String AT = " @ ";

    /**
     * This class holds the data associated with an individual item in list view
     *
     * <CODE><PRE>
     *
     * RESPONSIBILITIES:
     * This class is responsible for holding the data associated with an individual item in list view
     *
     * COLABORATORS:
     *     SmartProfile - Implements the preconditions available across the system
     *
     * USAGE:
     *     See each method.
     *
     * </PRE></CODE>
     */
    static class ViewHolder {
        int overLayColor; // Used by AgendaItemView to gray out the entire item
        // if so desired
        /* Event */
        TextView title;
        TextView when;
        int calendarColor; // Used by AgendaItemView to color the vertical
        // stripe
    }

    /**
     * Constructor
     *
     * @param context
     *            - The context where the ListView associated with this
     *            SimpleListItemFactory is running
     * @param resource
     *            - resource identifier of a layout file that defines the views
     *            for this list item. Unless you override them later, this will
     *            define both the item views and the drop down views.
     */
    public AgendaAdapter(Context context, int resource) {
        super(context, resource, null);
        mResources = context.getResources();
        mNoTitleLabel = mResources.getString(R.string.no_title_label);
        mDeclinedColor = mResources.getColor(R.color.agenda_item_declined);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = null;

        // Listview may get confused and pass in a different type of view since
        // we keep shifting data around. Not a big problem.
        Object tag = view.getTag();
        if (tag instanceof ViewHolder) {
            holder = (ViewHolder) view.getTag();
        }

        if (holder == null) {
            holder = new ViewHolder();
            view.setTag(holder);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.when = (TextView) view.findViewById(R.id.when);
        }

        // Fade text if event was declined.
        int selfAttendeeStatus = cursor
                                 .getInt(AgendaWindowAdapter.INDEX_SELF_ATTENDEE_STATUS);
        if (selfAttendeeStatus == Attendees.ATTENDEE_STATUS_DECLINED) {
            holder.overLayColor = mDeclinedColor;
        } else {
            holder.overLayColor = 0;
        }

        TextView title = holder.title;
        TextView when = holder.when;

        /* Calendar Color */
        int color = cursor.getInt(AgendaWindowAdapter.INDEX_COLOR);
        holder.calendarColor = color;

        // What
        String titleString = cursor.getString(AgendaWindowAdapter.INDEX_TITLE);
        if (titleString == null || titleString.length() == 0) {
            titleString = mNoTitleLabel;
        }
        title.setText(titleString);

        // When
        long begin = cursor.getLong(AgendaWindowAdapter.INDEX_BEGIN);
        boolean allDay = cursor.getInt(AgendaWindowAdapter.INDEX_ALL_DAY) != 0;
        int startDay = cursor.getInt(AgendaWindowAdapter.INDEX_START_DAY);
        Time date = new Time(Time.getCurrentTimezone());
        long millis = date.setJulianDay(startDay);
        int dayFlags = DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_DATE;
        String dayText = DateUtils.formatDateRange(context, millis, millis,
                         dayFlags);
        int flags;
        String whenString = "";
        flags = DateUtils.FORMAT_SHOW_TIME;
        if (DateFormat.is24HourFormat(context)) {
            flags |= DateUtils.FORMAT_24HOUR;
        }
        if (!allDay) {
            whenString = DateUtils
                         .formatDateRange(context, begin, begin, flags);
            when.setText(dayText + AT + whenString);
        } else {
            when.setText(dayText);
        }
        String rrule = cursor.getString(AgendaWindowAdapter.INDEX_RRULE);
        if (!TextUtils.isEmpty(rrule)) {
            when.setCompoundDrawablesWithIntrinsicBounds(null, null, context
                    .getResources().getDrawable(R.drawable.ic_repeat_dark),
                    null);
            // when.setCompoundDrawablePadding(5);
        } else {
            when.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        }
    }

}
