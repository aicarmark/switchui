/*
 * @(#)TimeFrame.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a15776       2010/02/21  NA                Incorporated review comments
 * a15776       2010/12/01  NA                Initial Version
 *
 */

package com.motorola.contextual.smartprofile.sensors.timesensor;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartrules.R;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import java.lang.NumberFormatException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;


/**
 * Container class for the time frame. Each TimeFrame has a list of
 * start intents and end intents, which will be registered with the Alarm
 * manager. This Class has the helper methods to register and de-register intents,
 * compose dvs rules, get and set of member variables
 *
 *<code><pre>
 *
 * CLASS:
 *  Implements TimeFrameConstants &  Constants
 *
 * RESPONSIBILITIES:
 *  1. Creates the data structure that holds the time frame and provides access methods for
 *  registering the pending intents with the Alarm Manager Service at appropriate times
 *
 * COLABORATORS:
 *  None
 *
 * USAGE:
 *  See each method.
 *
 *</pre></code>
 */
public class TimeFrame implements TimeFrameConstants, Constants {
    /** Log constant */
    private static final String TAG = TimeFrame.class.getSimpleName();
    private static final long serialVersionUID = -5502362426494919210L;
    private static final String COLON = ":";
    /** Name visible to to the user */
    private String mTimeFrameName;
    /** Name internally generated. This wont be visible to the user */
    private String mInternalTimeFrameName;
    /** days on which this time frame should repeat */
    private String mDays[];
    /** Collection of start intents */
    private StartIntents mStartIntents = null;
    /** Collection of end intents */
    private EndIntents mEndIntents = null;
    private Context mContext;
    /** state to check if the time frame should be currently active - Needed to set the right state
     * if the phone is rebooted or time/date shifted
     */
    private boolean mIsTimeFrameActive;

    /**
     * constructor. create a container from the details available in the cursor
     *
     * @param context - context
     * @param cursor - cursor containing the details of the time frame
     */
    public TimeFrame(Context context, Cursor cursor) {
        mContext = context;
        mIsTimeFrameActive = false;


        String freq,from, to;
        try {
        	if (cursor.moveToFirst()) {
                // read the data from the cursor
                mTimeFrameName =
                    cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.NAME));
                mInternalTimeFrameName =
                    cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.NAME_INT));
                String allDayFlag =
                    cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.ALL_DAY));
                int daysOfWeek =
                    cursor.getInt(cursor.getColumnIndex(TimeFrameTableColumns.DAYS_OF_WEEK));

                //convert the bit mask (daysOfWeek) to comma separated list of days
                freq = new TimeFrameDaysOfWeek(daysOfWeek).toCommaSeparatedString(mContext,true);

                //set the time accordingly, if this is an all day event
                if (allDayFlag.equalsIgnoreCase(ALL_DAY_FLAG_TRUE)) {
                    from = DAY_START_TIME;
                    to   = DAY_END_TIME;
                }
                else {
                    from = cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.START));
                    to   = cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.END));
                }
                // split the comma separated string and store it in the string array mDays
                if (freq != null && freq.length() > 0) {
                    mDays = freq.split(SHORT_DAY_SEPARATOR);
                    // add the start and end intent list
                    mStartIntents = new StartIntents(this);
                    mEndIntents   = new EndIntents(this);
                    // This loop should add as many containers as number of week days on
                    // which this time frame is applicable
                    for (int i=0; i < mDays.length; i++) {
                        //populate the start intents list
                        mStartIntents.add(
                            new StartIntent(
                                this,mTimeFrameName, mInternalTimeFrameName, mDays[i], from));
                        //populate the start intents list
                        mEndIntents.add(
                            new EndIntent(
                                this,mTimeFrameName, mInternalTimeFrameName, mDays[i], to));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }         
    }

    /**
     * Constructor to create the time frame with the details provided
     *
     * @param ctx                   - Context
     * @param timeframeName         - Name of the time frame
     * @param internalTimeFrameName - Internal name of the time frame
     * @param days                  - List of days on which the time frame is to repeat
     * @param from                  - Start time of the time frame
     * @param to                    - End time of the time frame
     * @param isAllDayEvent         - Flag to indicate if its an all day event. If yes, from & to
     *                                are ignored
     */
    public TimeFrame(Context ctx, String timeframeName, String internalTimeFrameName,
                     String[] days, String from, String to, boolean isAllDayEvent) {
        mContext = ctx;
        mTimeFrameName = timeframeName;
        mInternalTimeFrameName = internalTimeFrameName;

        mDays = new String[days.length];
        for(int i=0 ; i<days.length; i++) {
            mDays[i] = days[i];
        }


        mIsTimeFrameActive = false;
        mStartIntents = new StartIntents(this);
        mEndIntents   = new EndIntents(this);

        if (isAllDayEvent == true) {
            from = DAY_START_TIME;
            to   = DAY_END_TIME;
        }
        else {
            // need not do anything. user entered start and end times are assigned by default
            if (from == null || from.length() == 0 || from.equals("0")) {
                from = DAY_START_TIME;
            }
            if (to == null || to.length() == 0 || to.equals("0")) {
                to = DAY_END_TIME;
            }
        }

        int numDays = mDays.length;
        // This loop should add as many containers as number of week days on which this time frame
        // is applicable
        for (int i=0; i < numDays; i++) {
            //populate the start intents list
            mStartIntents.add(
                new StartIntent(
                    this,mTimeFrameName, mInternalTimeFrameName, mDays[i], from));
            //populate the end intents list
            mEndIntents.add(
                new EndIntent(
                    this,mTimeFrameName, mInternalTimeFrameName, mDays[i], to));
        }
    }

    /**
     * Retrieves the Start Intents list for the time frame
     * @return - List of StartIntents - {@link StartIntents}
     */
    StartIntents getStartIntents() {
        return mStartIntents;
    }

    /**
     * Retrieves the End Intents list for the time frame
     * @return - List of EndIntents - {@link EndIntents}
     */
    EndIntents getEndIntents() {
        return mEndIntents;
    }

    /**
     * Retrieves the name of the time frame
     * @return - String. Name of the time frame
     */
    String getName() {
        return mTimeFrameName;
    }

    /**
     * Retrieves the internal name of the time frame
     * @return - String. Internal Name of the time frame
     */
    String getInternalName() {
        return mInternalTimeFrameName;
    }

    /**
     * Broadcast time frame intent, so that the state of the time frame will be properly set. This
     * is required to handle cases
     * 1. where the user randomly changes the time
     * 2. Phone is switched off when a time frame is active and then switched on when the time
     *    frame is not active
     */
    private void setTimeFrameStateAsBackup() {
        if (mIsTimeFrameActive == false) {
            if (LOG_INFO) Log.i(TAG,"Fire false state  for " + mTimeFrameName);
            TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(mContext);
            dbAdapter.setTimeFrameAsInactive(mInternalTimeFrameName);
            dbAdapter.close();
            setFalseStateForTimeframe();
        }
        else {
            if (LOG_INFO) Log.i(TAG,"State of " + mTimeFrameName + " should be true");
            // explicitly sending the intent in required, since Alarm Manager does not send the
            // intent more than once for the same time. So there may not be any broadcast from AM
            // if the user moves the time/date back and forth
            setTrueStateForTimeframe();
        }
    }

    /**
     * Composes the rule for the dvs for this time frame, for the true state
     * @param dvsName - DVS Name for this Time frame
     * @return        - Rule String
     */
    public String getRuleSyntaxWhenTrue(String dvsName) {

        StringBuilder trueRule = new StringBuilder();
        trueRule.append(INTENT_ACION_PREFIX)
        .append(TIME_FRAME_INTENT_PREFIX)
        .append(mInternalTimeFrameName)
        .append(INTENT_WILD_CHAR)
        .append(EXTRA_ACTIVE_FLAG)
        .append(EXTRA_EQUALS)
        .append(ACTIVE_TRUE)
        .append(INTENT_END_STRING)
        .append(TRIGGERS)
        .append(VSENSOR_CONSTANT)
        .append(dvsName)
        .append(TIME_END_TRUE_STRING);

        String intentString = trueRule.toString();

        if (LOG_DEBUG) Log.d(TAG, "True Rule for " + mTimeFrameName + " is " + intentString);
        return intentString;
    }

    /**
     * Composes the rule for the dvs for this time frame, for the false state
     * @param dvsName - DVS Name for this Time frame
     * @return        - Rule String
     */
    public String getRuleSyntaxWhenFalse(String dvsName) {

        StringBuilder falseRule = new StringBuilder();
        falseRule.append(INTENT_ACION_PREFIX)
        .append(TIME_FRAME_INTENT_PREFIX)
        .append(mInternalTimeFrameName)
        .append(INTENT_WILD_CHAR)
        .append(EXTRA_ACTIVE_FLAG)
        .append(EXTRA_EQUALS)
        .append(ACTIVE_FALSE)
        .append(INTENT_END_STRING)
        .append(TRIGGERS)
        .append(VSENSOR_CONSTANT)
        .append(dvsName)
        .append(TIME_END_FALSE_STRING);

        String intentString = falseRule.toString();
        if (LOG_DEBUG) Log.d(TAG, "False Rule for " + mTimeFrameName + " is " + intentString);
        return intentString;
    }

    /**
     * Helper method to compose the the rules for the DVS for the time frame
     *
     * @param dvsName - Internal name of the time frame
     * @return        - Rules for the Rule Based DVS
     */
    public String[] getDvsRules(String dvsName) {
        String[] result =
            new String[] { getRuleSyntaxWhenTrue(dvsName), getRuleSyntaxWhenFalse(dvsName) };
        if (LOG_DEBUG) Log.d (TAG, "Rules returned:" + Arrays.toString(result));
        return result;
    }

    /**
     * Wrapper method to register the all the intents for the time frame, with AM. To be done
     * every time during BOOT_COMPLETE, DATE_CHANGE, TIME_ZONE_CHANGE
     */
    public void regsiterAllIntents() {
        //register all the start intents and the end intents for this time frame
        if (this.mStartIntents != null)
            this.mStartIntents.registerIntentsWithAm();

        if (this.mEndIntents != null)
            this.mEndIntents.registerIntentsWithAm();

        // after registering all the start intents and the end intents, we now know, if any of the
        // would have fired by now and hence the right state.It is now possible to set the right
        // state, even if the phone has been rebooted or date was changed or time was changed when
        // the time frame was active.
        // This is required for the following scenario
        // A TimeFrame defined as 5PM - 10PM. At time 7PM, when the time frame is active, the user
        // powers off the phone and powers it up again at 11.30PM. We now need to set the state as
        // false
        setTimeFrameStateAsBackup();
    }

    /**
     * Wrapper method to de-register the all the intents for the time frame from AM.
     * This is required, when deleting the time frame
     */
    public void deRegsiterAllIntents() {
        if (this.mStartIntents != null)
            this.mStartIntents.deRegisterIntentsWithAm();

        if (this.mEndIntents != null)
            this.mEndIntents.deRegisterIntentsWithAm();
    }

    /**
     * broadcast that the time frame is not active, so context engine can change the dvs
     * state if needed
     */
    public void setFalseStateForTimeframe() {
        // broadcast the time frame end,so context engine can change the dvs state if needed
        ArrayList<String> configs = TimeFramesDetailComposer.getConfigListByInternalName(mContext, mInternalTimeFrameName);
        HashMap<String, String> configStateMap = new HashMap<String, String>();
        int size = configs.size();
        for(int i =0; i<size; i++) {
            configStateMap.put(configs.get(i), TimeFramesDetailComposer.getStateForConfig(mContext, configs.get(i)));
        }
        if(!configs.isEmpty()) {
            //make a new intent with the extracted details and broadcast
            Intent newIntent = CommandHandler.constructNotification(configStateMap, TIMEFRAME_PUBLISHER_KEY);
            if (LOG_INFO) Log.i(TAG, "Intent being broadcasted " + newIntent.toUri(0));
            mContext.sendBroadcast(newIntent, PERM_CONDITION_PUBLISHER_ADMIN);
        }

    }

    /**
     * broadcast that the time frame is active, so context engine can change the dvs state
     * if needed
     */
    public void setTrueStateForTimeframe() {
        // broadcast the time frame end,so context engine can change the dvs state if needed
        ArrayList<String> configs = TimeFramesDetailComposer.getConfigListByInternalName(mContext, mInternalTimeFrameName);
        HashMap<String, String> configStateMap = new HashMap<String, String>();
        int size = configs.size();
        for(int i =0; i<size; i++) {
            configStateMap.put(configs.get(i), TRUE);
        }

        if(!configs.isEmpty()) {
            //make a new intent with the extracted details and broadcast
            Intent newIntent = CommandHandler.constructNotification(configStateMap, TIMEFRAME_PUBLISHER_KEY);
            if (LOG_INFO) Log.i(TAG, "Intent being broadcasted " + newIntent.toUri(0));
            mContext.sendBroadcast(newIntent, PERM_CONDITION_PUBLISHER_ADMIN);
        }
    }

    /** Delete the time frame. Before deleting, this method would make the time frame state to
     * false, so that the rules dependent on this time frame state are in proper state. After a
     * time frame is deleted, the state of the virtual sensor for this time frame  should be false
     */
    public void deleteSelf() {
        if (LOG_INFO) Log.i(TAG, " Time Frame " + mTimeFrameName + " is being deleted ");
        // before deleting a time frame set/broadcast its state to false
        setFalseStateForTimeframe();
        deRegsiterAllIntents();
    }

    /**
     * Method to set the active flag
     * @param flag Flag to be set
     */
    public void setTimeFrameActiveFlag (boolean flag) {
        if (LOG_INFO) Log.i(TAG, "Setting timeframe active flag to "+flag+" for "+ mTimeFrameName);
        mIsTimeFrameActive = flag;
    }

    /**
     * Collection of StartIntents for a time frame
     *
     * CLASS:
     *  Extends {@link BaseIntents}
     *
     * RESPONSIBILITIES:
     *  Maintains the list of start intents of the time frame. Provides high level methods to
     *  register and de-register the start intents with the alarm manager
     */
    public static class StartIntents extends BaseIntents {
        transient private TimeFrame mParent;
        private static final long serialVersionUID = -3645994572915576160L;

        /**
         * Constructor that takes the handle to parent
         *
         * @param parent - Handle to the enclosing TimeFrame
         */
        public StartIntents(TimeFrame parent) {
            mParent = parent;
        }

        /**
         * Method to register each of the start intents with the Alarm Manager for the time frame
         */
        void registerIntentsWithAm() {
            int numIntents = this.size();
            // loop through all the intents and register with the alarm manager.
            for (int i=0; i < numIntents; i++) {
                this.get(i).registerStartIntentWithAm(this.mParent.mEndIntents.get(i));
            }
        }

        /**
         * Method to unregister each of the start intents with the Alarm Manager for the time frame
         */
        void deRegisterIntentsWithAm() {
            int numIntents = this.size();
            // loop through all the intents and unregister with the alarm manager.
            for (int i=0; i < numIntents; i++) {
                this.get(i).deRegisterIntentWithAm();
            }
        }
    }

    /**
     * Collection of EndIntents for a time frame
     *
     * CLASS:
     *  Extends {@link BaseIntents}
     *
     * RESPONSIBILITIES:
     *  Maintains the list of end intents of the time frame. Provides high level methods to
     *  register and de-register the end intents with the alarm manager

     */
    public static class EndIntents extends BaseIntents {
        transient private TimeFrame mParent;
        private static final long serialVersionUID = 441880348197026974L;

        /**
         * Constructor
         * @param parent - Time frame that contains this EndIntents
         */
        public EndIntents(TimeFrame parent) {
            mParent = parent;
        }

        /**
         * Method to register each of the end intents with the Alarm Manager for the time frame
         */
        void registerIntentsWithAm() {
            int numIntents = this.size();
            // loop through all the intents and register with the alarm manager.
            for (int i=0; i < numIntents; i++) {
                this.get(i).registerEndIntentWithAm(this.mParent.mStartIntents.get(i));
            }
        }

        /**
         * Method to unregister each of the end intents with the Alarm Manager for the time frame
         */
        void deRegisterIntentsWithAm() {
            int numIntents = this.size();
            // loop through all the intents and unregister with the alarm manager.
            for (int i=0; i < numIntents; i++) {
                this.get(i).deRegisterIntentWithAm();
            }
        }
    }
    /**
     * Abstract class to give {@link ArrayList} functionality to {@link StartIntents} and
     * {@link EndIntents}
     *
     * CLASS:
     *  Extends {@link ArrayList<BaseIntent>}
     *
     * RESPONSIBILITIES:
     *  To provide the {@link ArrayList} functionality to the sub classes that derive from this
     *  class
     */
    public static abstract class BaseIntents extends ArrayList<BaseIntent> {
        private static final long serialVersionUID = -2286947546018453557L;
    }

    /**
     * Container class for the start intent.
     *
     * CLASS:
     *  Extends {@link BaseIntent}
     *
     * RESPONSIBILITIES:
     *  Contains the details about the start time of the time frame. This also does the following
     *  1. Provides method to calculate the time at which the pending intent should be registered
     *     with the alarm manager
     *
     *  2. Provides method to register the pending intent with the alarm manager
     *
     */
    public static class StartIntent extends BaseIntent {

        private static final long serialVersionUID = 7138254645482484819L;
        private static final String TAG = StartIntent.class.getSimpleName();

        /**
         * Constructor for the StartIntent that takes all the details
         * @param parent - Time frame that contains this StartIntent
         * @param name   - Name of the time frame
         * @param intName - Internal name of the time frame
         * @param day     - Calendar Day on which the intent should fire
         * @param time    - Time (hh:mm) at which the intent should fire
         */
        public StartIntent(TimeFrame parent, String name,
                           String intName, String day, String time) {
            super(parent, name, intName, day, time, true);
        }

        /**
         * Computes when the start intent has to be fire and registers the intent
         * with the Alarm Manager with appropriate frequency
         *
         * @param endIntent - Corresponding end intent for this start intent.
         */
        void registerStartIntentWithAm(EndIntent endIntent) {
            if (LOG_DEBUG) Log.d(TAG, "registerStartIntentWithAmNew - Entry");

            Calendar currCal = Calendar.getInstance();
            long endTime     = endIntent.fireIntentAt();
            long startTime   = fireIntentAt();
            long currentTime = currCal.getTimeInMillis();

            // check if both start & end fire times are expired. in that case do not fire
            if (startTime < endTime) {
                startTime = handleTimeframeOnSameDay(startTime, endTime, currentTime);
            }
            else {
                if (LOG_DEBUG) Log.d(TAG, "Handle Timeframes across days");
                startTime = handleTimeframeAcrossDays(startTime, endTime,currentTime);
            }

            if (LOG_DEBUG) Log.d(TAG, "Clear any intent registered already for the same time");
            registerPendingIntentWithAlarmManager(startTime);

            if (mRegisteredTime <= currentTime) {
                // the state of the time frame should be active
                // the sticky intent would be sent by TimeFrames Module
                if (LOG_INFO) Log.i(TAG, "Mark active");
                this.mParent.mIsTimeFrameActive = true;
                TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(mContext);
                dbAdapter.setTimeFrameAsActive(this.mParent.mInternalTimeFrameName);
                dbAdapter.close();

            }

            if (LOG_INFO) Log.i(TAG, "registerStartIntentWithAm - Exit");
        }

        /**
         * This method handles the case where the time frame is on the same day
         * (end time > start time)
         *
         * @param startTime   - Start Time of the time frame
         * @param endTime     - End Time of the time frame
         * @param currentTime - Current system time
         *
         * @return Time at which the start intent has to be registered with the Alarm Manager
         */
        long handleTimeframeOnSameDay(long startTime, long endTime, long currentTime) {
            // Start time is less than end time, which is most common case
            if (endTime < currentTime) {
                // Both Expired. Example 2PM - 4PM. Current Time 6 PM
                if (mCalDay != TIME_FRAME_ON_ALL_DAYS) {
                    startTime = startTime + NUM_OF_MILLIS_PER_WEEK;
                }
                else {
                    startTime = startTime + NUM_OF_MILLIS_PER_DAY;
                }
            }
            else {
                // Start Expired. Example 2PM - 4PM. Current Time 3 PM. No special handling
                // need. startTime need not be altered
                if (LOG_DEBUG) {
                    Log.d(TAG, "TimeFrame Name " + this.mParent.mTimeFrameName +
                          " Register for current day !! ");
                }
            }
            return startTime;
        }
        /**
         * This method handles the case where the time frame is across time (end time < start time)
         *
         * @param startTime   - Start Time of the time frame
         * @param endTime     - End Time of the time frame
         * @param currentTime - Current system time
         *
         * @return Time at which the start intent has to be registered with the Alarm Manager
         */
        long  handleTimeframeAcrossDays(long startTime, long endTime, long currentTime) {
            long prevStart;
            long prevEnd;

            // since this is a special case where end time is less then start time, adjust the
            // end time by a day, before using it for any checks
            endTime = endTime + NUM_OF_MILLIS_PER_DAY;

            if (LOG_DEBUG) logTimes(startTime, endTime, currentTime);

            // get the previous start time and previous end time
            if (mCalDay == TIME_FRAME_ON_ALL_DAYS) {
                if (LOG_DEBUG) Log.d(TAG, "All Day Time frame");
                prevStart = startTime - NUM_OF_MILLIS_PER_DAY;
                prevEnd   = endTime   - NUM_OF_MILLIS_PER_DAY;
            }
            else {
                if (LOG_DEBUG) Log.d(TAG, "Specific day time frame");
                prevStart = startTime - NUM_OF_MILLIS_PER_WEEK;
                prevEnd   = endTime   - NUM_OF_MILLIS_PER_WEEK;
            }

            if (LOG_DEBUG) logTimes(prevStart, endTime, currentTime);

            if (prevEnd > currentTime) {
                if (LOG_DEBUG) Log.d(TAG, "Register for Previous Start");
                return prevStart;
            }
            else {
                //register for current start & end
                if (LOG_DEBUG) Log.d(TAG, "Register for Current Start");
                return startTime;
            }
        }

        @Override
        void registerEndIntentWithAm(BaseIntent startIntent) {
            // not needed for this class
        }

        @Override
        void registerStartIntentWithAm(BaseIntent endIntent) {
            this.registerStartIntentWithAm((EndIntent)endIntent);
        }
    }

    /**
     * Container class for a single "End Intent" for a time frame
     *
     * CLASS - Extends {@link BaseIntent}
     *
     * RESPONSIBILITIES:
     *  1. Creates the data structure that holds the end intent. Stores informations like
     *     Name of the intent and the day & time on which it should fire.
     *
     * COLABORATORS:
     *  None
     *
     * USAGE:
     *  See each method.
     *
     */
    public static class EndIntent extends BaseIntent {
        private static final long serialVersionUID = 1216082470365662339L;
        private static final String TAG = EndIntent.class.getSimpleName();

        /**
         * Constructor for the EndIntent that takes all the details
         * @param parent - Time frame that contains this EndIntent
         * @param name   - Name of the time frame
         * @param intName - Internal name of the time frame
         * @param day     - Calendar Day on which the intent should fire
         * @param time    - Time (hh:mm) at which the intent should fire
         */
        public EndIntent(TimeFrame parent,String name,
                         String intName, String day, String time) {
            super(parent, name, intName, day, time,false);
        }

        /**
         * Computes when the end intent has to be fired and registers the intent
         * with the Alarm Manager with appropriate repeat interval
         *
         * @param startIntent - Start Intent Container for this time frame. End Intent time is
         *                      always computed relative to the start intent's time.
         */
        void registerEndIntentWithAm(StartIntent startIntent) {

            Intent intentToRegister = mIntent;

            Calendar currCal = Calendar.getInstance();
            long     currentTime = currCal.getTimeInMillis();

            if (LOG_DEBUG) Log.d(TAG,"Intent registered : " +intentToRegister.toUri(0));

            //get the absolute difference between start & end intent
            long diffInMs =
                TimeUtil.getDiffBetweenStartAndEnd(startIntent.mFireAtTime, this.mFireAtTime);
            if (diffInMs == 0) {
                //this just means that start time and end time are same. so treat the end time
                // as the time on the next day
                diffInMs = diffInMs + NUM_OF_MILLIS_PER_DAY;
            }
            //adjust the end time relative to the start time.
            long endTime = startIntent.mRegisteredTime + diffInMs;
            //register with Alarm Manager
            registerPendingIntentWithAlarmManager(endTime);

            if (endTime <= currentTime) {
                // the state of the time frame should be active
                // the sticky intent would be sent by TimeFrames Module
                if (LOG_INFO) Log.d(TAG, "Mark false");
                this.mParent.mIsTimeFrameActive = false;
                TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(mContext);
                dbAdapter.setTimeFrameAsInactive(this.mParent.mInternalTimeFrameName);
                dbAdapter.close();

            }
            if (LOG_DEBUG) Log.d(TAG, "registerEndIntentWithAm - Exit");
        }

        @Override
        void registerEndIntentWithAm(BaseIntent startIntent) {
            this.registerEndIntentWithAm((StartIntent)startIntent);

        }

        @Override
        void registerStartIntentWithAm(BaseIntent endIntent) {
            // not needed for this class
        }
    }

    /**
     * Abstract Base class for the Intent
     *
     * RESPONSIBILITIES:
     *  1. Creates the data structure that holds the intent. Stores informations like
     *     Name of the intent and the day & time on which it should fire.
     *
     *  2. Provide Common methods for the start intent container and the end intent container
     *
     * COLABORATORS:
     *  None
     *
     *
     * USAGE:
     *  See each method.
     *
     */
    public static abstract class BaseIntent {
        private static final String TAG = BaseIntent.class.getSimpleName();
        private static final long serialVersionUID = -1166067068625498141L;

        protected Context mContext;
        /** back pointer to the Start Intent List */
        protected TimeFrame mParent;
        /** Intent for this particular instance of the time frame */
        Intent mIntent;
        /** string representation of the day in a week */
        String mDay;
        /** time to fire at in 24 hour hh:mm format */
        String mFireAtTime;
        /** Calendar day */
        int mCalDay;
        /** Time registered with Alarm Manager in milliseconds*/
        long mRegisteredTime;

        /**
         * Constructor for the BaeIntent
         * @param parent - Time frame that contains this BaseIntent
         * @param name   - Name of the time frame
         * @param intName - Internal name of the time frame
         * @param day     - Calendar Day on which the intent should fire
         * @param time    - Time (hh:mm) at which the intent should fire
         * @param type    - Flag to indicate if its a StartIntent or EndIntent
         */
        public BaseIntent(TimeFrame parent, String name, String intName,
                          String day, String time, boolean type) {
            mContext = parent.mContext;
            mParent = parent;
            String activeFlag;
            String mime;
            String startOrEnd;
            if (type == true) {
                startOrEnd = START;
                activeFlag = ACTIVE_TRUE;
            }
            else {
                startOrEnd = END;
                activeFlag = ACTIVE_FALSE;
            }
            mime = new StringBuilder().append(TIME_FRAME_INTENT_MIME).
            append(TIME_FRAME_INTENT_PREFIX).
            append(intName).
            append(PERIOD).
            append(day).
            append(startOrEnd).toString();

            // make the intent for this instance
            mIntent = new Intent(TIMEFRAME_INTENT_ACTION);
            mIntent.setType(mime);
            mIntent.putExtra(EXTRA_FRIENDLY_NAME, name);
            mIntent.putExtra(EXTRA_FRAME_NAME, TIME_FRAME_INTENT_PREFIX + intName);
            mIntent.putExtra(EXTRA_ACTIVE_FLAG, activeFlag);
            mDay = day;
            mCalDay = convertStrToCalDay(day);
            mFireAtTime = time;
        }

        /** Convenience method */
        Intent getIntent() {
            return mIntent;
        }

        /** Convenience method */
        String getDay() {
            return mDay;
        }

        /** Convenience method */
        String getTime() {
            return mFireAtTime;
        }

        /**
         * De-register the pending intent from the alarm manager
         */
        void deRegisterIntentWithAm() {
            if (LOG_INFO) Log.i(TAG, "deregister with Am " + mIntent.toUri(0));
            AlarmManager am =
                (AlarmManager)this.mContext.getSystemService(android.content.Context.ALARM_SERVICE);
            PendingIntent sender =
                PendingIntent.getBroadcast(mContext, 0, mIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            am.cancel(sender);
        }

        /**
         * compute the time at which the intent has to be fired
         */
        long fireIntentAt() {
            if (LOG_DEBUG) Log.d(TAG, "fireIntentAt");

            //convert the time in string to integer format
            int hour = 0;
            int minute = 0;

            try {
                hour = Integer.parseInt(mFireAtTime.split(COLON)[0]);;
                minute = Integer.parseInt(mFireAtTime.split(COLON)[1]);
            } catch (NumberFormatException e) {
                hour = 0;
                minute = 0;
                mFireAtTime = DAY_START_TIME; //just to ensure mFireAtTime is in hh:mm format
            }

            Calendar nowCal,setCal;
            nowCal =  Calendar.getInstance();
            setCal =  Calendar.getInstance();

            // if this is not the time frame for all the days, then set the day
            if (mCalDay != TIME_FRAME_ON_ALL_DAYS) {
                setCal.set(Calendar.DAY_OF_WEEK, mCalDay);
            }

            // set the time at which the intent should fire
            setCal.set(Calendar.HOUR_OF_DAY, hour);
            setCal.set(Calendar.MINUTE, minute);
            setCal.set(Calendar.SECOND, 0);

            // now convert to milliseconds for further comparisons
            long curTimeInMilli = nowCal.getTimeInMillis();
            long setTimeInMilli  = setCal.getTimeInMillis();

            if (LOG_DEBUG) {
                Log.d(TAG, " Current Time : " + nowCal.getTime().toString() +
                      " Intent Fire Time : " + setCal.getTime().toString());
            }

            if (curTimeInMilli <= setTimeInMilli) {
                // These intents to be fired for this week
                return setTimeInMilli;
            }

            if (getDaysBetween(nowCal, setCal) == 0) {
                // if the intent is to fire on this day, it should be fired even if the time of the
                // intent has passed. i.e current time is greater than the time at which the intent
                // should have fired
                return setTimeInMilli;
            }
            if (mCalDay != TIME_FRAME_ON_ALL_DAYS) {
                // Time for this is over. Makes sense only for next time so, offset the time by
                // number of remaining days for mCalDay
                setTimeInMilli = setTimeInMilli + NUM_OF_MILLIS_PER_WEEK;
                return setTimeInMilli;
            }
            //fire on all days
            return setTimeInMilli;
        }

        /**
         * Routine to convert the day of week in string format to the
         * standard format used by Calendar
         *
         * @param day - Day in String format
         */
        int convertStrToCalDay(String day) {
            if (day.equalsIgnoreCase(mContext.getString(R.string.everyday))) {
                return TIME_FRAME_ON_ALL_DAYS;
            }
            else {
                return (new TimeFrameDaysOfWeek().convertStrToCalendarDay(day));
            }
        }

        /**
         * Utility function to get the number of calendar days between
         * 2 calendar time in milliseconds
         *
         * @param d1 - Time
         * @param d2 - Time
         *
         * @return Number of days Calendar days between d1 & d2
         */
        int getDaysBetween (Calendar d1, Calendar d2) {

            if (LOG_DEBUG) Log.d(TAG, "getDaysBetween");
            if (d1.after(d2)) {  // swap dates so that d1 is start and d2 is end
                Calendar swap = d1;
                d1 = d2;
                d2 = swap;
            }
            int days = d2.get(Calendar.DAY_OF_YEAR) -
                       d1.get(Calendar.DAY_OF_YEAR);

            int y2 = d2.get(Calendar.YEAR);

            if (d1.get(Calendar.YEAR) != y2) {
                d1 = (Calendar) d1.clone();
                do {
                    days += d1.getActualMaximum(Calendar.DAY_OF_YEAR);
                    d1.add(Calendar.YEAR, 1);
                } while (d1.get(Calendar.YEAR) != y2);
            }
            if (LOG_DEBUG) Log.d(TAG, "Number of days returned: " + days);
            return days;
        }

        /**
         * Register the pending start/end intent for the time frame with the alarm manager
         *
         * @param registerAt - Time at which the pending intent should first fire
         */
        void registerPendingIntentWithAlarmManager(long registerAt) {

            AlarmManager am = (AlarmManager)this.mContext.getSystemService(
                                  android.content.Context.ALARM_SERVICE);

            long repeatFrequency;

            if (mCalDay != TIME_FRAME_ON_ALL_DAYS) {
                repeatFrequency = NUM_OF_MILLIS_PER_WEEK;
            }
            else {
                repeatFrequency = NUM_OF_MILLIS_PER_DAY;
            }

            PendingIntent sender = PendingIntent.getBroadcast(
                                       mContext, 0, mIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            // clear previous pending intents and set new
            am.cancel(sender);

            mRegisteredTime = registerAt;

            Calendar currCal = Calendar.getInstance();
            long currentTime = currCal.getTimeInMillis();
            if(registerAt < currentTime) {
                registerAt +=  repeatFrequency;
            }
            am.setRepeating(AlarmManager.RTC_WAKEUP, registerAt, repeatFrequency, sender);

            if (LOG_INFO) {
                currCal.setTimeInMillis(registerAt);
                Log.i(TAG, "Intent " + mIntent.toUri(0) + " should first fire at " +
                      currCal.getTime().toString());
            }
        }

        /**
         * Method to log the time in milliseconds in the readable format
         * @param startTime   - Start Time of the Intent
         * @param endTime     - End Time of the Intent
         * @param currentTime - Current Time
         */
        void logTimes(long startTime, long endTime, long currentTime) {
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();
            Calendar cal3 = Calendar.getInstance();

            cal1.setTimeInMillis(startTime);
            cal2.setTimeInMillis(endTime);
            cal3.setTimeInMillis(currentTime);

            // No need to preface this with if (LOG_DEBUG) since method itself is for
            // logging and is called only LOG_DEBUG is true
            Log.d(TAG, "Start Time : "  + cal1.getTime().toString() +
                  "End Time: "     + cal2.getTime().toString() +
                  "Current Time: " + cal3.getTime().toString());
        }

        /**
         * Computes when the end intent has to be fired and registers the intent
         * with the Alarm Manager with appropriate repeat interval
         *
         * @param startIntent - Start Intent Container for this time frame. End Intent time is
         *                      always computed relative to the start intent's time.
         */
        abstract void registerEndIntentWithAm(BaseIntent startIntent);

        /**
         * Computes when the start intent has to be fire and registers the intent
         * with the Alarm Manager with appropriate frequency
         *
         * @param endIntent - Corresponding end intent for this start intent.
         */
        abstract void registerStartIntentWithAm(BaseIntent endIntent);

    }
}
