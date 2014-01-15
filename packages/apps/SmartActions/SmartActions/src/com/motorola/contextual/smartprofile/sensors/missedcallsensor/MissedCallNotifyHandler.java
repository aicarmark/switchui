/*
 * @(#)MissedCallNotifyHandler.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2010/03/06  NA                Initial version of MissedCallNotifyHandler
 *
 */

package com.motorola.contextual.smartprofile.sensors.missedcallsensor;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartprofile.Persistence;
import com.motorola.contextual.smartrules.db.DbSyntax;



/**
 * This class handles "notify" command for Missed Call
 * Condition Publisher
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommandHandler
 *     Implements MissedCallConstants, DbSyntax
 *
 * RESPONSIBILITIES:
 * This class handles "notify" command for Missed Call
 * Condition Publisher
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public final class MissedCallNotifyHandler extends  CommandHandler implements MissedCallConstants, DbSyntax  {

    private final static String LOG_TAG = MissedCallNotifyHandler.class.getSimpleName();
    private final static String CALL_END_EVENT = "com.motorola.smartactions.intent.action.CONDITION_PUBLISHER_CALL_END_EVENT";

    @Override
    protected String executeCommand(Context context, Intent intent) {

        if(intent == null) {
            Log.w(LOG_TAG, " Null intent received ");
        } else {
            if(checkIfCallAddedToCallDB(context)) {
                getMissedCallInfoAndNotify(context);
                Intent newIntent = new Intent(CALL_END_EVENT);
                context.sendBroadcast(newIntent);
                if(LOG_INFO) Log.i(LOG_TAG, "Posting call intent..");
            }
            getUserSeenInfoAndNotify(context);
        }
        return SUCCESS;
    }

    /**
      * Checks if a call is added to the DB
      * @param context
      * @return true/false
      */
    private boolean checkIfCallAddedToCallDB(Context context) {
        long maxId = getmaxId(context);
        boolean retVal = false;
        long persId = Persistence.retrieveIntValue(context, MISSED_CALLS_MAX_ID_PERSISTENCE);
        
        if(LOG_INFO) Log.i(LOG_TAG, " checkIfCallAddedToCallDB : " + maxId + " : " + persId);
        if(maxId == 0) {
            Persistence.commitValue(context, MISSED_CALLS_MAX_ID_PERSISTENCE, (int)maxId);
            retVal = true;
        } else {
            if(maxId > persId) {
                Persistence.commitValue(context, MISSED_CALLS_MAX_ID_PERSISTENCE, (int)maxId);
                retVal = true;
            }
        }
        return retVal;
    }

    /**
      * Get the id of last call log entry.
      * @param context
      * @return _ID of call table in CallLogProvider
      */
    private long getmaxId(Context context) {
        long max = 0;
        Cursor c = null;
        String[] projection = new String[] { Calls._ID };
        try {
            c = context.getContentResolver().query(
                    android.provider.CallLog.Calls.CONTENT_URI, projection, null,
                    null, Calls._ID + " DESC");
            if ((c != null) && c.moveToFirst()) {
                max =  c.getLong(c.getColumnIndex(Calls._ID));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(c != null)
                c.close();
        }
        return max;
    }

    /**
     * This method queries the call log DB to get the type
     * of the call ended and the corresponding number of the last call ended.
     * If the type is "Missed Call", this method broadcasts a Missed Call intent
     * which is used to trigger
     * the missed call rule constructed by MissedCall module.
     * @param context
     */
    private final void getMissedCallInfoAndNotify(Context context) {

        Cursor callCursor = null;
        final String[] EVENT_PROJECTION = new String[] {
            CallLog.Calls.TYPE,
            CallLog.Calls.NUMBER
        };
        try {
            String sortOrder = Calls._ID + DESC + LIMIT + "1";
            callCursor = context.getContentResolver().query(Calls.CONTENT_URI,
                         EVENT_PROJECTION,
                         null,
                         null,
                         sortOrder);
            if((callCursor != null) && (callCursor.moveToFirst())) {

        		int lastCallType = callCursor.getInt(callCursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));

                if(lastCallType == CallLog.Calls.MISSED_TYPE) {
                    String lastEventNumber = callCursor.getString(callCursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                    lastEventNumber = PhoneNumberUtils.extractNetworkPortion(lastEventNumber);

                    if(lastEventNumber != null &&
                            lastEventNumber.length() > MissedCallActivity.MAX_SUPPORTED_DIGITS_IN_NUMBER) {
                        lastEventNumber = lastEventNumber.substring(((lastEventNumber.length()-MissedCallActivity.MAX_SUPPORTED_DIGITS_IN_NUMBER)), (lastEventNumber.length()));
                    }

                    if (LOG_DEBUG) Log.d(LOG_TAG, "FormatNumber after strip : " + lastEventNumber);

                    MissedCallDBAdapter dbAdapter = new MissedCallDBAdapter(context);
                    int updatedCount = dbAdapter.updateRowToMissedCallTable(lastEventNumber);
                    if(updatedCount != -1) {
                        checkAndSendStatus(context, lastEventNumber, updatedCount);
                    }
                }
                
            } else {
                Log.e(LOG_TAG, "callCursor is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if ((callCursor!=null) && (!callCursor.isClosed()))
                callCursor.close();
        }
    }

    /**
     * Get config list by number
     * @param context
     * @param number
      * @return config list
     */
    static ArrayList<String> getConfigListByNumber(Context context, String number) {
        List<String> valueList = Persistence.retrieveValuesAsList(context, MISSED_CALLS_CONFIG_PERSISTENCE);
        ArrayList<String> returnConfigList = new ArrayList<String>();
        for(String config : valueList) {
            if(config.contains(number) || (config.contains(MISSED_CALL_ANY_NUMBER))) {
                returnConfigList.add(config);
            }
        }
        return returnConfigList;
    }

    /**
     * Get state for config
     * @param context
     * @param updatedCount
     * @param config
      * @return state
     */
    static String getStateForConfig(Context context, int updatedCount, String config) {
        String state = null;

        String frequency = config.substring(config.indexOf(CLOSE_B+OPEN_B)+(CLOSE_B+OPEN_B).length());
        frequency = frequency.substring(0, frequency.indexOf(CLOSE_B));
        int maxCount = Integer.valueOf(frequency);

        state = ((updatedCount >= maxCount) ? TRUE : MissedCallDetailComposer.getStateForConfig(context, config));

        return state;
    }



    /**
     * Resets status of all configs
     * @param context
     */
    static public void resetAllConfigStatus(Context context) {
        ArrayList<String> configs = (ArrayList<String>)Persistence.retrieveValuesAsList(context, MISSED_CALLS_CONFIG_PERSISTENCE);
        HashMap<String, String> configStateMap = new HashMap<String, String>();

        int size = configs.size();
        for(int i=0; i<size; i++) {
            configStateMap.put(configs.get(i), FALSE);
        }

        if(!configs.isEmpty()) {
            Intent newIntent = CommandHandler.constructNotification(configStateMap, MISSED_CALLS_PUB_KEY);
            context.sendBroadcast(newIntent, PERM_CONDITION_PUBLISHER_ADMIN);
            if(LOG_INFO) Log.i(LOG_TAG, "postNotify - resetAllConfigStatus : " + configStateMap.toString());
        }
    }

    /**
     * Checks and sends status
     * @param context
     * @param number
     * @param updatedCount
     */
    public void checkAndSendStatus(Context context, String number, int updatedCount) {

        HashMap<String, String> configStateMap = new HashMap<String, String>();
        ArrayList<String> configs = getConfigListByNumber(context, number);

        int size = configs.size();
        for(int index = 0; index < size; index++) {
            configStateMap.put(configs.get(index), getStateForConfig(context, updatedCount, configs.get(index)));
        }

        if(!configs.isEmpty()) {
            Intent newIntent = CommandHandler.constructNotification(configStateMap, MISSED_CALLS_PUB_KEY);
            context.sendBroadcast(newIntent, PERM_CONDITION_PUBLISHER_ADMIN);
            if(LOG_INFO) Log.i(LOG_TAG, "postNotify : " +  configStateMap.toString());
        }
    }

    /**
     * getUserSeenInfo - This function queries the call logs DB
     * and extracts the value for "NEW" calls column. If there are any
     * changes from 1 to 0, in the last value of this column, true is returned .
     * This means the user has launched "recent  calls" activity and "is aware" of the
     * missed calls and hence the "missed call mode" could be reset.
     *
     * @param context
     * @return true/false
     */
    static final synchronized boolean getUserSeenInfo(Context context) {

        final String[] EVENT_PROJECTION = new String[] {
            CallLog.Calls.NEW
        };
        Cursor callCursor = null;
        String whereClause = CallLog.Calls.TYPE
                             + EQUALS
                             + CallLog.Calls.MISSED_TYPE;

        String sortOrder = Calls._ID + DESC + LIMIT + "1";

        try {
            callCursor = context.getContentResolver().query(Calls.CONTENT_URI,
                         EVENT_PROJECTION,
                         whereClause,
                         null,
                         sortOrder);
            if((callCursor != null) && (callCursor.moveToFirst()))  {
                int newCalls = callCursor.getInt(callCursor.getColumnIndexOrThrow(CallLog.Calls.NEW));

                if(newCalls == 0) {
                    if (LOG_DEBUG) Log.d(LOG_TAG, "registerContentObserverForUserSeenMc - changed : " );
                    MissedCallDBAdapter dbAdapter = new MissedCallDBAdapter(context);
                    dbAdapter.resetMissedCallTable(context);
                    return true;
                }
            } else {
                Log.e(LOG_TAG, "callCursor is null");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if ((callCursor!=null) && (!callCursor.isClosed()))
                callCursor.close();
        }

        return false;
    }

    /**
     * getUserSeenInfoAndNotify - This function queries the call logs DB
     * and extracts the value for "NEW" calls column. If there are any
     * changes from 1 to 0, in the last value of this column, a "userseen" intent is posted.
     * This means the user has launched "recent  calls" activity and "is aware" of the
     * missed calls and hence the "missed call mode" could be reset.
     *
     * @param context
     * @return true/false
     */
    static final synchronized boolean getUserSeenInfoAndNotify(Context context) {

        if(getUserSeenInfo(context)) {
            resetAllConfigStatus(context);
            return true;
        }
        return false;
    }

}

