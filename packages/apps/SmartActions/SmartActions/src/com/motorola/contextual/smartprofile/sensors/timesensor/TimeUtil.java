/*
 * @(#)TimeUtil.java
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.Log;

import com.motorola.contextual.pickers.conditions.timeframe.TimeFramesRefreshHandler;
import com.motorola.contextual.smartrules.db.DbSyntax;
/**
 * Utility class to do basic time frame sanity checks and formatting
 *
 * RESPONSIBILITIES:
 *   Provides the basic methods to format time and day, conversion from string to time and
 *   time to string
 *
 */
public  class TimeUtil implements TimeFrameConstants, DbSyntax {
    private static final String TAG =  TimeUtil.class.getSimpleName();
    private static final String HOUR_MINUTE_SPERATOR = ":";
    private static final String FILL_ZERO = "0";
    /**
     * Method to get the time String to be displayed in UI.
     * Display time format is hh:mm if system setting is 24 hour format
     * Display time format is hh:mm AM/PM if system setting is 12 hour format
     *
     * @param time - String representing time in 24 hour format (hh:mm)
     * @return  String representing time in 12 hour format (hh:mm AM/PM)
     */
    public static String getDisplayTime(Context c, String time) {
        int hourOfDay   = Integer.parseInt(time.split(HOUR_MINUTE_SPERATOR)[0]);
        int minute = Integer.parseInt(time.split(HOUR_MINUTE_SPERATOR)[1]);
        return getDisplayTime(c, hourOfDay, minute);
    }
    /**
     * Method to get the time String to be displayed in UI.
     *
     * @param hourOfDay - Hour of the day in 24 hour format
     * @param minute    - minutes
     *
     * @return  String representing time in 12/24 hour format
     */
    public static String getDisplayTime(Context c, int hourOfDay, int minute) {

        int flags = 0;
        flags |= DateUtils.FORMAT_SHOW_TIME;
        flags |= DateUtils.FORMAT_NO_NOON_MIDNIGHT;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minute);

        return(DateUtils.formatDateTime(c, cal.getTimeInMillis(), flags));
    }

    /**
     * Helper method to format time string to store in DB. Time should be
     * in hh:mm 24 hour format
     *
     * @param hourOfDay - Hour of the day
     * @param minute    - Minute
     * @return          - String in hh:mm format
     */
    static  String getDbTime(int hourOfDay, int minute) {
        String setDBTime;

        if (minute >= 10) {
            setDBTime = hourOfDay + HOUR_MINUTE_SPERATOR + minute;
        }
        else {
            setDBTime = hourOfDay + HOUR_MINUTE_SPERATOR + FILL_ZERO + minute;
        }
        return setDBTime;
    }

    /**
     * Method to generate a unique internal name for the time frame based on
     * system time in milliseconds
     *
     * @return Unique string for a time frame
     */
    public static String getUniqIdForTime() {
        return (new Date().getTime() + "").substring(7);
    }

    /**
     * Find the difference between start time and end time. If the end time is less than
     * start time, it is considered as the time on next day
     *
     * @param startTime - Start Time
     * @param endTime   - End Time
     *
     * @return - Difference in Milliseconds
     */
    static long getDiffBetweenStartAndEnd(String startTime, String endTime) {
        long timeDiffInSec;

        // get the start time in the unit of minutes
        int startHour   = Integer.parseInt(startTime.split(HOUR_MINUTE_SPERATOR)[0]);
        int startMinute = Integer.parseInt(startTime.split(HOUR_MINUTE_SPERATOR)[1]);
        long start = (startHour * 60) + startMinute;

        // get the end time in the unit of minutes
        int endHour   = Integer.parseInt(endTime.split(HOUR_MINUTE_SPERATOR)[0]);
        int endMinute = Integer.parseInt(endTime.split(HOUR_MINUTE_SPERATOR)[1]);
        long end = (endHour * 60) + endMinute;

        // if end time  is less than start time, increment it by 24 hours
        if (end < start) {
            end = (24 * 60) + end;
        }
        timeDiffInSec = (end - start) * 60;
        // convert to milliseconds
        return timeDiffInSec * 1000;
    }

    /**
     * Utility method to find if the end time falls on the next day (w.r.t start time)
     *
     * @param startHour - Hour of the day of the start time
     * @param startMin  - Minutes of the start time
     * @param endHour   - Hour of the day of the end time
     * @param endMin    - Minutes of the end time
     *
     * @return  - true, if end time is less than or equal to start time
     *          - false, other wise
     */
    static boolean isEndTimeOnNextDay(int startHour, int startMin, int endHour, int endMin) {
        long startInMillis = (long) ((startHour * 60) + startMin) * 60 * 1000;
        long endInMillis   = (long) ((endHour * 60) + endMin) * 60 * 1000;

        if (endInMillis <= startInMillis) {
            return true;
        }
        return false;
    }
    /**
     *  Utility Method to creates a new record or update an existing record of the time frame
     *  @param context - Application Context
     *  @param oldName - Name to be edited/replaced
     *  @param newName - Name to be added
     *  @param start   - Start Time for the Time Frame
     *  @param end     - End Time for the Time Frame
     *  @param alldayflag - If the Time Frame is an allday event
     *  @param daysOfWeek - Days of the week that the Time Frame is repeated
     */
    public static int writeTimeframeToDb(Context context, String oldName, String newName, String start, String end, Boolean alldayflag, int daysOfWeek) {
        TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(context);
        TimeFramesRefreshHandler asyncRefresh = new TimeFramesRefreshHandler();
        TimeFrame timeFrame;
        String internalName = null;
        int responseCode = FAILED;

        if (LOG_DEBUG) Log.d(TAG, "writeTimeframeToDb Entry");

        if (newName == null || ((start == null || end == null) && alldayflag == false)) {
            if (LOG_INFO) Log.e(TAG,"Error : Null Required parameter!!!");
            return responseCode;
        }

        TimeFrameTuple tfTuple = new TimeFrameTuple();

        tfTuple.setName(newName);
        tfTuple.setStartTime(start);
        tfTuple.setEndTime(end);
        tfTuple.setAllDayFlag(alldayflag);

        tfTuple.setDaysOfWeek(daysOfWeek);

        if (oldName != null) {
            //Try to get the existing time frame settings from the db
            Cursor cursor = dbAdapter.getTimeframe(oldName);
            if (cursor != null) {
                try {
                    if (LOG_DEBUG) Log.d(TAG, "Number of modes returned :" + cursor.getCount() + " for " + oldName);
                    if (cursor.getCount() != 0 && cursor.moveToFirst()) {

                        // deregister old timeframes associated with this name 
                        TimeFrame oldTf = new TimeFrame(context, cursor);
                        oldTf.deRegsiterAllIntents();

                        internalName = cursor.getString(
                                           cursor.getColumnIndex(TimeFrameTableColumns.NAME_INT));
                        tfTuple.setInternalName(internalName);

                        if (LOG_DEBUG) Log.d(TAG, " Updating Record: OldName: " + oldName +
                                                 " New Name: " + newName +
                                                 " Internal Name: " + internalName + "From Time: "
                                                 + start +
                                                 " End Time: " + end + "Repeat Days: " + daysOfWeek);

                        dbAdapter.updateRow(oldName, tfTuple);

                        //Update Smart Rule DB with new Name
                        asyncRefresh.constructAndPostAsyncRefresh(context, internalName);

                        responseCode = WRITE_SUCCESS;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    cursor.close();
                }
            }
        }

        if (responseCode != WRITE_SUCCESS) {
            // No existing time frame or Edit failed. Try adding new Time Frame
            //remove all special characters from the name for the internal name
            String trimmedName = newName.replaceAll(REG_EX_TO_IGNORE, EMPTY_STRING);
            String newline = System.getProperty("line.separator");
            trimmedName = trimmedName.replace(newline, EMPTY_STRING);
            internalName = TimeUtil.getUniqIdForTime();

            tfTuple.setInternalName(internalName);

            if (LOG_DEBUG) Log.d(TAG, "Inserting New Record: Name: " + newName +
                                     " Internal Name: " + internalName + "From Time: " +
                                     start +
                                     " End Time: " + end + "Repeat Days: " + daysOfWeek);

            //update the table
            dbAdapter.insertRow(tfTuple);

            responseCode = WRITE_SUCCESS;
        }
        // register the new time with the alarm manager
        // convert the comma separated day string, to an array of days of week
        String[] days = new TimeFrameDaysOfWeek(daysOfWeek)
        .toCommaSeparatedString(context, true)
        .split(SHORT_DAY_SEPARATOR);

        // In case this is a Single day time frame, the day will be in long format
        // convert this to short format
        TimeFrameDaysOfWeek tfDow = new TimeFrameDaysOfWeek();
        days[0] = tfDow.getShortFormat(days[0]);
        timeFrame = new TimeFrame(context, newName, internalName,
                                  days, start, end, alldayflag);
        timeFrame.deRegsiterAllIntents();
        if (isTimeFrameRegistered(context, newName)) {
            if (LOG_INFO) {
                Log.i(TAG, "writeTimeframeToDb registering timeframe = "
                      + newName);
            }
            timeFrame.regsiterAllIntents();
        }

        dbAdapter.close();
        return responseCode;
    }

    /**
     *  Utility Method which creates an array of JSON Objects of the
     *  TimeFrame DB and exports it
     *  @param context - Application Context
     */
    public static void exportTimeFrameData(Context context) {

        if (EXPORT_TIMEFRAMES) {

            TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(context);
            final JSONArray tfJSONObj = new JSONArray();

            Cursor cursor = dbAdapter.getAllTimeframes();

            if (cursor == null) {
                Log.e(TAG, " Time Frame DB Cursor is null");
            } else {

				if (LOG_DEBUG) Log.d(TAG, " No. of entries : " + cursor.getCount());

                try {
                    if(cursor.moveToFirst()) {
                        do {
                            JSONObject jsonObj = new JSONObject();

                            // Copy all the data into JSON Object

							String startTime = cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.START));
                            String endTime = cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.END));

                            // Need to check start and end time as incase of
                            // All-day option both of these are null/empty
                            // which might give an exception when converting back
                            if ((startTime == null) || (startTime.length() == 0) || (startTime.equals("0"))) {
                                startTime = DAY_START_TIME;
                            }

                            if ((endTime == null) || (endTime.length() == 0) || (endTime.equals("0"))) {
                                endTime = DAY_END_TIME;
                            }

                            jsonObj.put(TimeFrameTableColumns.NAME, cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.NAME)));
                            jsonObj.put(TimeFrameTableColumns.START, startTime);
                            jsonObj.put(TimeFrameTableColumns.END, endTime);
                            jsonObj.put(TimeFrameTableColumns.NAME_INT, cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.NAME_INT)));
                            jsonObj.put(TimeFrameTableColumns.ALL_DAY, cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.ALL_DAY)));
                            jsonObj.put(TimeFrameTableColumns.DAYS_OF_WEEK, cursor.getInt(cursor.getColumnIndex(TimeFrameTableColumns.DAYS_OF_WEEK)));
                            jsonObj.put(TimeFrameTableColumns.VISIBLE, cursor.getString(cursor.getColumnIndex(TimeFrameTableColumns.VISIBLE)));

                            if(LOG_INFO) Log.i(TAG, " JSON data = { " + jsonObj.toString() + " } ");

                            tfJSONObj.put(jsonObj);

                        } while (cursor.moveToNext());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    cursor.close();
                }
            }
            dbAdapter.close();

            if(tfJSONObj.length() > 0) {
                if (LOG_DEBUG) Log.d(TAG," Sending Rules Exporter Intent ");
                // ======== Phoenix Security Changes ========
                // Writing the xml data into shared preference
                // to prevent other apps from snooping the data

    	        SharedPreferences tfPref = context.getSharedPreferences(TIMEFRAME_SHARED_PREFERENCE, Context.MODE_PRIVATE);
    	        SharedPreferences.Editor editor = tfPref.edit();
    	        editor.clear();
    	        editor.putString(TIMEFRAME_XML_CONTENT, tfJSONObj.toString());
    	        editor.commit();

    	        if(LOG_INFO)  Log.i(TAG, "TimeFrame Data updated !!!");

                Intent intent = new Intent(NOTIFY_DATA_CHANGE);
                context.sendBroadcast(intent);

            } else {
                if (LOG_INFO) Log.i(TAG," JSON array length :  " + tfJSONObj.length());
            }
        }

    }


    /**
    * Utility method which updates Smart Rules DB when
    * a TimeFrame is created/modified in TimeFrame DB
    * @param context - Application Context
    * @param intName - Internal Name maintained in timeframes.db
    * @param newName - New Name of the timeframe associated with the intName
    *
    * @return - true if update is successful
    *         - false if updates fails
    */
   static boolean updateSmartRulesDB(Context context, String intName, String newName) {

	   if(LOG_DEBUG) Log.d(TAG, "Fn: updateSmartRulesDB >> TimeFrame Name : " + newName);

	   boolean result = false;
	   Cursor cursor  = null;
       Uri uri = Uri.parse(CONDITION_TABLE_URI);

       String whereClause = CONDITON_PUBLISHER_KEY + EQUALS + Q + TIMEFRAME_PUBLISHER_KEY + Q + AND +
       						CONDITON_TARGET_STATE + EQUALS + Q + intName + Q;
       ContentResolver cr = context.getContentResolver();
       try {
    	   cursor = cr.query(uri, null, whereClause, null, null);

    	   if((cursor != null) && (cursor.moveToFirst())) {
			   // Updating the Condition Description, as it is what is visible to
			   // the user in GraphicsActivity, we need not update anything else

    		   String oldDesc = cursor.getString(cursor.getColumnIndex(CONDITON_DESCRIPTION));

    		   if ((!newName.equals(oldDesc))) {
				   // Update the Condition Table
    			   ContentValues newValues = new ContentValues();
				   newValues.put(CONDITON_DESCRIPTION, newName);

    			   if(LOG_DEBUG) Log.d(TAG, "Updating Condition Table : " + newValues.toString());

				   cr.update(uri, newValues, whereClause, null);
    			   result = true;

    		   } else {
    			   if(LOG_DEBUG) Log.d(TAG, "Condition Table already contains : " + newName);
    		   }

           } else {
               Log.e(TAG, "updateSmartRulesDB : Cursor is null for " + whereClause);
           }
       } catch (Exception e) {
    	   e.printStackTrace();
       } finally {
           if (cursor != null) {
               cursor.close();
           }
       }
       return result;
   }

   /**
    * For a rule Key it updates the respective Condition description
    * for a time frame condition
    * @param context - Application Context
    * @param ruleKey - rule Key for a Rule
    */
   static void updateConditionTableForARule(Context context, final String ruleKey) {
	    if (LOG_INFO) Log.i(TAG,"Fn:updateConditionTableForARule");

	    Cursor ruleCursor = null;
	    Cursor cursor = null;

	    try{
	     	// Get the Cursor for a Rule Key
	       	String ruleWhereClause = RULE_KEY + EQUALS + Q + ruleKey  + Q;
	       	Uri ruleUri = Uri.parse(RULE_TABLE_URI);

	       	ContentResolver cr = context.getContentResolver();
	        ruleCursor = cr.query(ruleUri, null, ruleWhereClause, null, null);

	       	if (ruleCursor == null){
	       		Log.e(TAG,"ruleCursor is null");
	       	}else{

	       		 if (ruleCursor.getCount()>0){

	       			 ruleCursor.moveToFirst();
	       			 String _id = ruleCursor.getString(ruleCursor.getColumnIndexOrThrow(RULE_ID));

	       			 Log.i(TAG,"Rule id : "+ _id +"for rule Key :"+ruleKey);
	                 // Query the condition table for the specific rule id whose
	                 // publisher key is timeframe

	                 String whereClause = CONDITION_TABLE_RULE_ID + EQUALS + Q +  _id + Q + AND +
	                		CONDITON_PUBLISHER_KEY + EQUALS + Q + TIMEFRAME_PUBLISHER_KEY + Q;
	                 // Projection does not work for Condition Table
	                 // String[] projection = { CONDITON_SENSOR_NAME,CONDITON_TARGET_STATE,CONDITON_DESCRIPTION};
	                 Uri uri = Uri.parse(CONDITION_TABLE_URI);

	                 cursor = cr.query(uri, null, whereClause, null, null);

	                    if (cursor == null){
	                    	Log.e(TAG,"ConditionTable Cursor is null");
	                    }else{
	                    	if (cursor.getCount()>0){
	                	    	 cursor.moveToFirst();
	                        	 TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(context);

	                        	 if (dbAdapter!=null){
	     		                	 do {
	     		                		 String internalName = cursor.getString(cursor.getColumnIndexOrThrow(CONDITON_TARGET_STATE));
	     		                		 String conditionDesc = cursor.getString(cursor.getColumnIndexOrThrow(CONDITON_DESCRIPTION));
	     		                		 String sensorName = cursor.getString(cursor.getColumnIndexOrThrow(CONDITON_SENSOR_NAME));

										 if (LOG_INFO) Log.i(TAG,"internalName : "+ internalName+
	     		                				 				" condDesc : "+conditionDesc +
	     		                				 				" sensorName : "+ sensorName);
	      		                		 String friendlyNamefromTimeFrame = dbAdapter.getFriendlyNameForInternalName(internalName);

										 if ((friendlyNamefromTimeFrame != null) &&
	     	                            		 ((!friendlyNamefromTimeFrame.equals(conditionDesc)) ||
												  (!friendlyNamefromTimeFrame.equals(sensorName)))) {

	     	                            	 if (LOG_INFO) Log.i(TAG,"Updating DB Values");

	     	                            	 //Update DB
	     	                            	 ContentValues updateValues = new ContentValues();

	     	                                 updateValues.put(CONDITON_DESCRIPTION,friendlyNamefromTimeFrame);

	     	                                 // Update time frame corresponding to a specific rule id, publisher key
	     	                                 // and internal name
	     	                                 String filterClause = CONDITION_TABLE_RULE_ID + EQUALS + Q + _id  + Q + AND +
	     	                                 CONDITON_PUBLISHER_KEY + EQUALS + Q + TIMEFRAME_PUBLISHER_KEY + Q + AND +
	     	                                 CONDITON_TARGET_STATE + EQUALS + Q + internalName + Q ;
	     	                                 cr.update(uri, updateValues, filterClause, null);

	     	                             }else{
	     	                            	 Log.w(TAG,"Friendly Name for Target State"+internalName+"is up to date with TimeFrame DB whose" +
	     	                            	 		"friendly name is : "+friendlyNamefromTimeFrame);
	     	                             }

	     		                	 } while(cursor.moveToNext());

	     		                	 dbAdapter.close();

	     		                 }else{
	     		                	 Log.e(TAG,"dbAdapter is null");
	     		                 }
	                	    }else{
	                	    	 Log.w(TAG,"No Timeframe Conditions for Suggested Rule : "+ ruleKey);
	                	     }
	                    }
	       		 }else{
	       			 Log.w(TAG,"No Rule is matching for Rule Key : "+ruleKey);
	       		 }
	       	}

	   	} catch (Exception e) {
	   		e.printStackTrace();
	   	} finally {
         	 if (cursor != null) {
            	 cursor.close();
         	 }
         	 if (ruleCursor != null) {
       			 ruleCursor.close();
         	 }
	   	}

   }

    public static boolean isTimeFrameRegistered(Context context, String name) {
        Cursor cursor = null;
        int registered = TIMEFRAME_UNREGISTERED;
        TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(context);
        if (LOG_INFO) {
            Log.i(TAG, "isTimeFrameRegistered looking for timeframe = " + name);
        }
        try {
            cursor = dbAdapter.getTimeframe(name);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                registered = cursor
                             .getInt(cursor
                                     .getColumnIndexOrThrow(TimeFrameTableColumns.REGISTERED));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        dbAdapter.close();
        boolean result = (registered == TIMEFRAME_REGISTERED) ? true : false;
        if (LOG_INFO) {
            Log.i(TAG, "isTimeFrameRegistered timeframe = " + name
                  + " registered = " + result);
        }
        return result;
    }

    /**
     * A translated version of the passed in id
     *
     * @param context - Context to work with
     * @param id      - id to be translated
     * @return        - Translated text if able to, else the id is returned
     */
    public static String getTranslatedTextForId(Context context, String id) {
        String translatedText = null;

        // Until this arrayList is required elsewhere, this constant
        // could stay local to function
        ArrayList<String> canBeTranslatedList = new ArrayList<String> (5);
        canBeTranslatedList.add(MORNING_TIMEFRAME);
        canBeTranslatedList.add(EVENING_TIMEFRAME);
        canBeTranslatedList.add(NIGHT_TIMEFRAME);
        canBeTranslatedList.add(WORK_TIMEFRAME);
        canBeTranslatedList.add(WEEKEND_TIMEFRAME);

        if (!canBeTranslatedList.contains(id)) {
            translatedText = id;
        } else {
            try {
                //resource ID for "Morning" is morning, for "Evening" it is evening and so on.
                //So, id to be translated is converted to lower case to get the resource name.
                translatedText = context.getString(context.getResources().getIdentifier(id.toLowerCase(), "string", context.getPackageName()));
            } catch (Exception e) {
                if (LOG_DEBUG) Log.d(TAG, "Unable to get string for " + id);
            } finally {
                if (translatedText == null || translatedText.length() == 0) {
                    translatedText = id;
                }
            }
        }

        return translatedText;
    }

    public static boolean isTimeFrameActive(Context context, String name) {
        Cursor cursor = null;
        int registered = TIMEFRAME_INACTIVE;
        TimeFrameDBAdapter dbAdapter = new TimeFrameDBAdapter(context);
        if (LOG_INFO) {
            Log.i(TAG, "isTimeFrameActive looking for timeframe = " + name);
        }
        try {
            cursor = dbAdapter.getTimeframeFromInternalName(name);
            if (cursor != null && cursor.moveToFirst()) {
                registered = cursor
                             .getInt(cursor
                                     .getColumnIndexOrThrow(TimeFrameTableColumns.ACTIVE));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        dbAdapter.close();
        boolean result = (registered == TIMEFRAME_ACTIVE) ? true : false;
        if (LOG_INFO) {
            Log.i(TAG, "isTimeFrameActive timeframe = " + name
                  + " active = " + result);
        }
        return result;
    }

    public static String trimBraces(String stringToTrim) {
	if (stringToTrim.contains(OPEN_B) && stringToTrim.contains(CLOSE_B)) {
		stringToTrim = stringToTrim.substring(stringToTrim.indexOf(OPEN_B) + 1, stringToTrim.indexOf(CLOSE_B));
	}

	return stringToTrim;
    }

}
