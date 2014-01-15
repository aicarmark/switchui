package com.motorola.contextual.pickers.conditions.calendar;

import android.content.Context;
import android.database.ContentObserver;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.util.Log;

/**
 * This class extends {@link ContentObserver} and listens to changes happening
 * in {@link Event} table. This is a singleton class
 *
 * @author wkh346
 *
 */
public class CalendarEventObserver extends ContentObserver implements
        CalendarEventSensorConstants {

    /**
     * TAG for logging
     */
    private static final String TAG = CalendarEventObserver.class
            .getSimpleName();

    /**
     * static instance for implementing singleton pattern
     */
    private static CalendarEventObserver sObserver = null;

    /**
     * Reference to {@link Context} instance
     */
    private Context mContext;

    /**
     * The singleton method for returning unique instance
     *
     * @param context
     *            - application's context
     * @return - unique instance of {@link CalendarEventObserver}
     */
    public static CalendarEventObserver getInstance(Context context) {
        if (sObserver == null) {
            sObserver = new CalendarEventObserver(context);
            if (LOG_INFO) {
                Log.i(TAG,
                        "getInstance created new instance of CalendarEventObserver");
            }
        }
        return sObserver;
    }

    /**
     * Constructor
     *
     * @param context
     *            - application's context
     */
    private CalendarEventObserver(Context context) {
        super(null);
        mContext = context;
    }

    @Override
    public void onChange(boolean selfChange) {
        if (LOG_INFO) {
            Log.i(TAG,
                    "onChange notifying calendar event service of db change, selfChange "
                            + selfChange);
        }
        CalendarEventUtils.notifyServiceOfCalendarDbChange(mContext);
    }

}
