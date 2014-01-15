/*
 * @(#)TimeFrames.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a15776       2010/12/01   NA               Initial Version
 *
 */

package com.motorola.contextual.smartprofile.sensors.timesensor;

import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrame.EndIntent;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrame.EndIntents;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrame.StartIntent;
import com.motorola.contextual.smartprofile.sensors.timesensor.TimeFrame.StartIntents;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.util.Log;

import java.util.ArrayList;

/**
 * Container class for the list of TimeFrame Container. Whenever there is a need to register all the
 * time frames with the Alarm Manager service, a list of time frame containers are created. It is
 * then traversed and all the start intents and end intents are registered.
 *<code><pre>
 * CLASS:
 *  Extends array list of {@link TimeFrame}
 *  Implements {@link TimeFrameConstants} &  {@link Constants}
 *
 * RESPONSIBILITIES:
 *  1. Maintains the time frame container in a list
 *  2. Provide high level methods to add, remove time frames to the list.
 *  3. Provide high level methods to register & deregister the methods with the Alarm Manager
 *
 * COLABORATORS:
 *  None
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class TimeFrames extends ArrayList<TimeFrame>
    implements Constants, TimeFrameConstants, TimeFrameXmlSyntax {

    private static final long serialVersionUID = 4856968866467766892L;
    private static final String TAG = TimeFrames.class.getSimpleName();


    /**
     * Method to read the time frame db and construct the list of time frames
     * which can be used for registering and de registering with Alarm Manager
     *
     * @return TimeFramesContainer
     */
    public TimeFrames getData(Context context) {
        TimeFrames result;
        TimeFrameDBAdapter mDbAdapter = new TimeFrameDBAdapter(context);
        try {
            Cursor cursor = mDbAdapter.getAllTimeframes();

            // the cursor can be null if there are database issues
            if (cursor == null) {
                Log.e(TAG, "Null cursor returned by getModeRows");
                return null;
            }

            // if there are no time frames created just log and exit
            if (cursor.getCount() == 0) {
                if (LOG_INFO) Log.i(TAG, "No timeframes in DB. Timeframes need to be created first");
                cursor.close();
                mDbAdapter.close();
                return null;
            }

            // if time frames are found loop through each of them and create containers for it.
            // once all the time frames are created, return the list
            try {
                result = this;
                if (LOG_INFO) Log.i(TAG, "Number of timeframes returned:" + cursor.getCount());
                if (cursor.moveToFirst()) {
                    while (!cursor.isAfterLast()) {
                        // Add each time frame to the container list.
                        String name =
                            cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.NAME));
                        String intName =
                            cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.NAME_INT));
                        String from =
                            cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.START));
                        String to   =
                            cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.END));
                        String allDayEvent =
                            cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.ALL_DAY));

                        int daysOfWeek =
                            cursor.getInt(cursor.getColumnIndex(TimeFrameTableColumns.DAYS_OF_WEEK));
                        boolean isAllDayEvent;
                        if (allDayEvent.equalsIgnoreCase(ALL_DAY_FLAG_TRUE)) {
                            isAllDayEvent = true;
                        }
                        else {
                            isAllDayEvent = false;
                        }
                        if (LOG_DEBUG) DatabaseUtils.dumpCursorToString(cursor);

                        // convert the comma separated day string, to an array of days of week
                        //String[] when = freq.split(SHORT_DAY_SEPERATOR);
                        String[] when = new TimeFrameDaysOfWeek(daysOfWeek)
                        .toCommaSeparatedString(context, true)
                        .split(SHORT_DAY_SEPARATOR);

                        // In case this is a Single day time frame, the day will be in long format
                        // convert this to short format
                        TimeFrameDaysOfWeek tfDow = new TimeFrameDaysOfWeek();
                        when[0] = tfDow.getShortFormat(when[0]);
                        this.add(new TimeFrame(context, name, intName, when, from, to, isAllDayEvent));
                        if (LOG_DEBUG) Log.d(TAG," Time Frame : " + name + " added to list");
                        cursor.moveToNext();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // return null on any exception
                result = null;
            } finally {
                // finally close the cursor
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // return null on any exception
            result = null;
        } finally {
            //finally close the db
            mDbAdapter.close();
        }
        return result;
    }
    /**
     * This Method runs through each of the time frame and registers the intents associated with it,
     *  with the Alarm Manager, as pending intent
     *
     * @return None
     */
    public void registerAllTimeFrames(Context context) {
        /* loop through each of the container and register */
        if (LOG_DEBUG) Log.d(TAG, "Register all time frames");
        for (int i=0; i < this.size(); i++) {
            TimeFrame singleContainer = this.get(i);
            if (singleContainer == null) continue;
            if (TimeUtil.isTimeFrameRegistered(context,
                                               singleContainer.getName())) {
                if (LOG_DEBUG) logTimeFrame(singleContainer);
                TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(context);
                dbAdapter.setTimeFrameAsInactive(singleContainer.getInternalName());
                dbAdapter.close();
                singleContainer.regsiterAllIntents();
            }
        }
        if (LOG_DEBUG) Log.d(TAG, "Registered all time frames");
    }

    /**
     * This Method runs through each of the time frame and deregisters the intents associated with
     * it, from the Alarm Manager
     *
     * @return None
     */
    public void deRegisterAllTimeFrames(Context context) {
        if (LOG_DEBUG) Log.d(TAG, "De-register all time frames");
        /* loop through each of the container and de-register */
        TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(context);
        for (int i=0; i < this.size(); i++) {
            TimeFrame singleContainer = this.get(i);
            singleContainer.deRegsiterAllIntents();
            dbAdapter.setTimeFrameAsUnregistered(singleContainer.getInternalName());
        }
        dbAdapter.close();
        if (LOG_DEBUG) Log.d(TAG, "De-registered all time frames");
    }

    /**
     * Returns the TimeFrameContainer with the given time frame name
     *
     * @param name - Time frame name
     * @return     - TimeFrameContainer
     */
    public TimeFrame getTimeFramebyName(String name) {

        if (name == null) {
            if (LOG_INFO) Log.i(TAG, "Timeframe Name Null");
            return null;
        }

        // loop to locate the time frame
        for (int i=0; i < this.size(); i ++) {
            TimeFrame timeFrameContainer = this.get(i);
            if (timeFrameContainer.getName().equals(name)) {
                return timeFrameContainer;
            }
        }
        if (LOG_INFO) Log.i(TAG, "Time frame : " + name + " not found");
        return null;
    }

    /**
     * Returns the external/user provided name of the time frame with the given time frame
     * internal name
     *
     * @param name - Internal Time frame name
     * @return     - TimeFrameContainer
     */
    public String getFriendlyNameForTimeFrame(String name) {
        if (name == null) {
            if (LOG_INFO) Log.i(TAG, "Internal Timeframe Name Null");
            return null;
        }

        for (int i=0; i < this.size(); i ++) {
            TimeFrame timeFrameContainer = this.get(i);
            if (timeFrameContainer.getInternalName().equals(name)) {
                return timeFrameContainer.getName();
            }
        }
        if (LOG_INFO) Log.i(TAG, "Time frame with internal name : " + name + " not found");
        return null;
    }

    /**
     * Returns the TimeFrameContainer with the given time frame internal name
     *
     * @param name - Internal Time frame name
     * @return     - TimeFrameContainer
     */
    public TimeFrame getTimeFrameByInternalName(String name) {

        if (name == null) {
            if (LOG_INFO) Log.i(TAG, "Internal Timeframe Name Null");
            return null;
        }

        for (int i=0; i < this.size(); i ++) {
            TimeFrame timeFrameContainer = this.get(i);
            if (timeFrameContainer.getInternalName().equals(name)) {
                return timeFrameContainer;
            }
        }
        if (LOG_INFO) Log.i(TAG, "Time frame with internal name : " + name + " not found");
        return null;
    }
    /**
     * Utility method to loop through the intents of the time frame and log the details
     *
     * @param timeContainer - Time frame thats being logged
     */
    private void logTimeFrame(TimeFrame timeContainer) {

        StartIntents startIntents;
        EndIntents   endIntents;

        Log.d(TAG,"Time Frame Name "+ timeContainer.getName());
        startIntents = (StartIntents)timeContainer.getStartIntents();

        Log.d(TAG,
              " ------------- Dump of Start Intents - Start --------------");
        for (int j = 0; j < startIntents.size(); j++) {
            Log.d(TAG, " -----x----- " + j + " ----- x -----");
            StartIntent singleIntent;
            singleIntent = (StartIntent)startIntents.get(j);
            Log.d(TAG, "Intent Name : " + singleIntent.getIntent().toUri(0));
            Log.d(TAG, "Fires on Day : " + singleIntent.getDay());
            Log.d(TAG, "Fires at : " + singleIntent.getTime());
        }
        Log.d(TAG, " ------------- Dump of Start Intents - End --------------");

        endIntents = (EndIntents)timeContainer.getEndIntents();
        Log.d(TAG, " ------------- Dump of End Intents - Start --------------");
        for (int j = 0; j < endIntents.size(); j++) {
            Log.d(TAG, " -----x----- " + j + " ----- x -----");
            EndIntent singleIntent;
            singleIntent = (EndIntent)endIntents.get(j);
            Log.d(TAG, "Intent Name : " + singleIntent.getIntent().toUri(0));
            Log.d(TAG, "Fires on Day : " + singleIntent.getDay());
            Log.d(TAG, "Fires at : " + singleIntent.getTime());
        }
        Log.d(TAG, " ------------- Dump of End Intents - End --------------");
    }
}
