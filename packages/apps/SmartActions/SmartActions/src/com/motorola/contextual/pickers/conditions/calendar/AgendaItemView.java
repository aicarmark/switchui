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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RelativeLayout;
import android.graphics.Color;

import com.motorola.contextual.pickers.conditions.calendar.AgendaAdapter.ViewHolder;

/**
 * This class provides a custom layout for each item in list view.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Extends {@link RelativeLayout}
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
public class AgendaItemView extends RelativeLayout {

    /**
     * Reference to {@link Paint} object used for painting color codes
     */
    Paint mPaint = new Paint();

    /**
     * Reference to {@link Bundle} object for holding the color code associated
     * with an individual item
     */
    private Bundle mTempEventBundle;

    /**
     * Constructor
     *
     * @param context
     *            - Application's context
     */
    public AgendaItemView(Context context) {
        super(context);
    }

    /**
     * Constructor
     *
     * @param context
     *            - Application's context
     * @param attrs
     *            - contains layout parameters
     */
    public AgendaItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        ViewHolder holder = (ViewHolder) getTag();
        if (holder != null) {

            /* Draw vertical color stripe */

            //HSHIEH: Issue: Calendar color blocks are not showing.
            //The below code is a hack because alpha is returning from Calendar Provider as zero and colors are incorrect.
            mPaint.setColor(Color.rgb(Color.red(holder.calendarColor), Color.green(holder.calendarColor), Color.blue(holder.calendarColor)));
            canvas.drawRect(0, 0, 15, getHeight(), mPaint);
            /* Gray out item if the event was declined */
            if (holder.overLayColor != 0) {
                mPaint.setColor(holder.overLayColor);
                canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);
            }
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (mTempEventBundle == null) {
            mTempEventBundle = new Bundle();
        }
        ViewHolder holder = (ViewHolder) getTag();
        if (holder != null) {
            // this way to be consistent with CalendarView
            Bundle bundle = mTempEventBundle;
            bundle.putInt("color", holder.calendarColor);
            event.setParcelableData(bundle);
        }
        return super.dispatchPopulateAccessibilityEvent(event);
    }
}
