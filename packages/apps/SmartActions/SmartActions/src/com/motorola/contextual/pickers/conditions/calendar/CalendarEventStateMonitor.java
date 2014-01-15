package com.motorola.contextual.pickers.conditions.calendar;

import java.util.ArrayList;

import android.content.Context;
import android.database.ContentObserver;
import android.provider.CalendarContract;

import com.motorola.contextual.smartrules.monitorservice.CommonStateMonitor;

/**
 * This class extends {@link CommonStateMonitor} and overrides the necessary
 * methods
 *
 * @author wkh346
 *
 */
public class CalendarEventStateMonitor extends CommonStateMonitor implements
        CalendarEventSensorConstants {

    @Override
    public ContentObserver getObserver(Context context) {
        return CalendarEventObserver.getInstance(context);
    }

    @Override
    public String getType() {
        return OBSERVER;
    }

    @Override
    public ArrayList<String> getStateMonitorIdentifiers() {
        ArrayList<String> uriList = new ArrayList<String>();
        uriList.add(CalendarContract.Events.CONTENT_URI.toString());
        return uriList;
    }

}
