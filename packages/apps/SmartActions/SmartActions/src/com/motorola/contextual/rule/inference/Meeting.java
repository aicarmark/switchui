/*
 * @(#)MeetingMissedCall.java
 *
 * (c) COPYRIGHT 2010-2013 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2012/06/14 NA                Initial version
 *
 */
package com.motorola.contextual.rule.inference;

import java.util.Date;
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.motorola.contextual.rule.Constants;
import com.motorola.contextual.rule.CoreConstants;
import com.motorola.contextual.rule.Util;
import com.motorola.contextual.rule.receivers.MeetingRingerStateMonitor;
import com.motorola.contextual.rule.receivers.SmsObserverStateMonitor;
import com.motorola.contextual.smartrules.db.DbSyntax;

/** This class contains the logic to infer Meeting Rule
*
*<code><pre>
* Logic:
* 1. Receive a missed call within a meeting
* 2. Send SMS to the missed call number within 2 mins
*
* CLASS:
* extends Inferencing
*
*
* RESPONSIBILITIES:
*  see each method for details
*
* COLABORATORS:
*  None.
*
* USAGE:
*  see methods for usage instructions
*
*</pre></code>
*/
public class Meeting extends Inference {

    private final static int SMS_TIMEOUT_DELAY      = 120000;
    private final static int CLEANUP_SMS_HANDLER    = 0xDEAD;

    private static boolean sInMeeting           = false;
    private static Context sContext             = null;
    private static HashMap<String,Long> sMissedCallTimeMap = new HashMap<String, Long>();

    public Meeting() {
        super(Meeting.class.getSimpleName());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.motorola.contextual.rule.inferer.IInference#infer(android.content
     * .Context, android.content.Intent)
     */
    protected boolean infer(Context ct, Intent intent) {

        boolean result = false;
        if(sContext == null) sContext = ct;

        String action = intent.getAction();
        if(action == null) return result;

        if (action.equals(Constants.PUBLISHER_KEY)) {

            String pubKey = intent.getStringExtra(CoreConstants.EXTRA_PUB_KEY);
            if(pubKey == null) return result;

            if (pubKey.equals(PUBLISHER_KEY_CALENDAR)) {
                handleCalendarEvent(intent);
            } else if (pubKey.equals(PUBLISHER_KEY_MISSED_CALL)) {
                handleMissedCall(intent);
            }
        } else if (action.equals(OUTBOUND_SMS_ACTION)) {
            result = handleSentSmsEvent(intent);
        } else if (action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
            result = handleRingerChangedEvent(intent);
        }

        return result;
    }

    /**
     * Handle ringer changes event
     *
     * @param intent - incoming intent
     * @return - true if inferred
     */
    private boolean handleRingerChangedEvent(Intent intent){

        boolean result = false;

        int ringerMode = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE,-1);
        if (ringerMode == AudioManager.RINGER_MODE_SILENT
                || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {

            if (LOG_INFO) Log.i(TAG, "RingerMode=" + ringerMode);

            // we don't care about the sticky ones or the ones set by volumes action
            if(sContext.getSharedPreferences(ACTIONS_PACKAGE, 0).getBoolean(KEY_IGNORE_VOLUME_CHANGE, false)
                    || intent.getBooleanExtra(IS_STICKY, false))
                return result;

            if(! Util.getSharedPrefStateValue(sContext, INFERENCE_STATE_MEETING_RINGER)){
                Util.setSharedPrefStateValue(sContext, INFERENCE_STATE_MEETING_RINGER, true);

                registerListener(sContext, MeetingRingerStateMonitor.class.getName(), false);

                if (LOG_DEBUG) Log.d(TAG, "Meeting Ringer cycle-I complete");
            } else {
                // check if meeting is already inferred?
                if (! getInferredState(sContext)) result = true;
            }
        }

        return result;
    }

    /**
     * Handle outbound SMS event
     *
     * @param intent - incoming intent
     * @return - true if inferred
     */
    private boolean handleSentSmsEvent(Intent intent){

        boolean result = false;

        String no = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        if (LOG_DEBUG) Log.d(TAG, "number=" + no);

        Long cTime = sMissedCallTimeMap.remove(no);
        long callTime =  cTime == null? 0:cTime.longValue();
        if (callTime == 0) return result;

        long smsTime = new Date().getTime();
        long elapsedtime = (smsTime - callTime);
        if (LOG_INFO) Log.i(TAG, "Elapsed time=" + elapsedtime);
        if (elapsedtime < SMS_TIMEOUT_DELAY) {
            registerListener(sContext, SmsObserverStateMonitor.class.getName(), false);
            registerListener(sContext, CoreConstants.PUBLISHER_KEY_MISSED_CALL,
                    CoreConstants.CONFIG_MISSED_CALL, false);
            
            if(! Util.getSharedPrefStateValue(sContext, INFERENCE_STATE_MEETING_MISSEDCALL)){
                Util.setSharedPrefStateValue(sContext, INFERENCE_STATE_MEETING_MISSEDCALL, true);

                if (LOG_INFO) Log.i(TAG, "Meeting Missed Call cycle-I complete");

                // clear the map
                sMissedCallTimeMap.clear();
            } else {

                // check if meeting is already inferred?
                if (! getInferredState(sContext)) result = true;
                if (LOG_INFO) Log.i(TAG, "Meeting Missed Call cycle-II result is " + result);
            }
        } else {
            if (sMissedCallTimeMap.size() == 0)
                registerListener(sContext, SmsObserverStateMonitor.class.getName(), false);
        }

        return result;
    }

    /**
     * Handle missed call event
     *
     * @param intent - incoming intent
     */
    private void handleMissedCall(Intent intent){

        String stateString = getConfigStateFromIntent(intent, CONFIG_MISSED_CALL);
        if(Util.isNull(stateString)) return;

        boolean state = Boolean.parseBoolean(stateString);
        if (LOG_INFO) Log.i(TAG, "Missed call=" + state);
        if (state) {

            // remember the missed call time and number
            String missedCallNo = getMissedCallNumber(sContext);
            if(missedCallNo == null) return;

            registerListener(sContext, SmsObserverStateMonitor.class.getName(), true);
            sMissedCallTimeMap.put(missedCallNo, new Date().getTime());
        }
    }

    /**
     * Handle event from calendar: meeting = true, register for
     * - missed call and ringer change
     *
     * @param intent - incoming intent
     */
    private void handleCalendarEvent(Intent intent){

        String stateString = getConfigStateFromIntent(intent, CONFIG_CALENDAR);
        if(Util.isNull(stateString)) return;

        boolean status = Boolean.parseBoolean(stateString);
        if (LOG_INFO) Log.i(TAG, "Meeting " + status);

        if(sInMeeting != status){
            sInMeeting = status;
            registerListener(sContext, MeetingRingerStateMonitor.class.getName(), status);

            registerListener(sContext, CoreConstants.PUBLISHER_KEY_MISSED_CALL,
                    CoreConstants.CONFIG_MISSED_CALL, sInMeeting);

            if(!sInMeeting) {
         //       Util.setSharedPrefStateValue(sContext, INFERENCE_STATE_MEETING_MISSEDCALL, false);
                sMissedCallTimeMap.clear();
                registerListener(sContext, SmsObserverStateMonitor.class.getName(), false);
            }
        }
    }

    /**
     * Method to read missed call no# from CallLog DB
     *
     * @return - missed call no#
     */
    private String getMissedCallNumber(Context ct) {

        String number = null;
        final String[] EVENT_PROJECTION = new String[] { CallLog.Calls.TYPE,
                CallLog.Calls.NUMBER };
        String whereTypeIs = CallLog.Calls.TYPE + DbSyntax.EQUALS
                + CallLog.Calls.MISSED_TYPE;
        String sortOrder = Calls._ID + DbSyntax.DESC + DbSyntax.LIMIT + "1";
        Cursor callCursor = null;
        try {
            callCursor = ct.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI, EVENT_PROJECTION, whereTypeIs,
                    null, sortOrder);
            if (callCursor != null) {
                if (callCursor.moveToFirst()) {
                    number = callCursor.getString(callCursor
                            .getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                    number = PhoneNumberUtils.extractNetworkPortion(number);
                    number = Util.getLastXChars(number, 10);

                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Not able to find missed call number");
        } finally {
            if (callCursor != null)
                callCursor.close();
        }

        if (LOG_DEBUG) Log.d(TAG, "Missed call from: " + number);
        return number;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.motorola.contextual.rule.inferer.IInference#unregisterAllListeners
     * (android.content.Context)
     */
    public void cleanUp(Context ct) {

        sMissedCallTimeMap.clear();
        registerListener(ct, CoreConstants.PUBLISHER_KEY_MISSED_CALL,
                CoreConstants.CONFIG_MISSED_CALL, false);

        registerListener(ct, SmsObserverStateMonitor.class.getName(), false);
        registerListener(ct, MeetingRingerStateMonitor.class.getName(), false);

        initRegister(ct, false);

    }

    /*
     * (non-Javadoc)
     *
     * @see com.motorola.contextual.rule.inferer.Inferencing#setRuleKey()
     */
    @Override
    public void setRuleKey() {
        mRuleKey = RULE_KEY_MEETING;
    }

    protected void initRegister(Context ct, boolean state) {

        registerListener(ct,CoreConstants.PUBLISHER_KEY_CALENDAR,
                CoreConstants.CONFIG_CALENDAR, state);
    }

    @Override
    public String getUpdatedConfig(Context ct, String pubKey, String config) {
        return null;
    }
}