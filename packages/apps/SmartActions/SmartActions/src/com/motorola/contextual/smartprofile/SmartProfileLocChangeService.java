/*
 * @(#)SmartProfileLocChangeService.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/04/15 NA				  Initial version
 *
 */
package com.motorola.contextual.smartprofile;

import java.util.Calendar;

import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartprofile.locations.LocConstants;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.smartrules.R;

/**
 * This class processes the intent from location sensor.Then queries the location sensor 
 * DB for all the entries that match this location and computes the total time spent at 
 * this location. If this total time spent at this location is > MINUTES_IN_A_WEEK then it 
 * is inferred as a meaningful location and of these total minutes spent at this meaningful 
 * location if HOME_MINUTES_FOR_INFERENCE minutes is spent at this location then it is treated 
 * as a home location or if WORK_MINUTES_FOR_INFERENCE is spent at this meaningful location 
 * then it is treated as Work location else it is tagged as Location<#ofExistingPOI's+1>. 
 * Once a meaningful location is inferred the inference manager is invoked 
 * to suggest a location based rule to the user.
 *
 *<code><pre>
 * CLASS:
 * 	extends IntentService.
 *
 * 	implements
 *      Constants - for the constants used
 *      LocConstants - for the Location related constants used
 *      
 * RESPONSIBILITIES:
 * 	handles the location change broadcast intent from location sensor.
 *
 * COLABORATORS:
 * 	Uses LocationSensor provider to create an entry in the POI table of location sensor.
 *
 * USAGE:
 *  None
 *</pre></code>
 **/
public class SmartProfileLocChangeService extends IntentService implements Constants, LocConstants,
																			DbSyntax {

	private static final String TAG = SmartProfileLocChangeService.class.getSimpleName();
	
	/*
     * At Home hour is start with HOME_HOUR_START and end with HOME_HOUR_END.
     */
    private static final int HOME_HOUR_START = 1;
    private static final int HOME_HOUR_END = 5;

    /*
     * At Work hour is start with WORK_HOUR_START and end with WORK_HOUR_END.
     */
    private static final int WORK_HOUR_START = 13;
    private static final int WORK_HOUR_END = 16;
    
    // tip over percentage - 2 Days * 4 Hours
    private static final int HOME_MINUTES_FOR_INFERENCE = 480;

    // tip over percentage - 2 Days * 3 Hours
    private static final int WORK_MINUTES_FOR_INFERENCE = 360;
    
    // 2 * 24 * 60 = 10080
    private static final int MINUTES_IN_A_WEEK = 2880;
    
    // Minimum entries in the Location Sensor DB
    private static final int MIN_ENTRIES_FOR_LOC_IN_DB = 10;
    
    // Minimum accuracy in meters of the location in the Location Sensor DB
    private static final int MIN_ACCURACY_IN_METERS_FOR_INFERENCE = 100;
    
	private Context mContext = null;
	
	/** default constructor
	 */
	public SmartProfileLocChangeService() {
		super(TAG);
	}

	/** onCreate
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;
	}

	/** onDestroy()
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	/** handles the intent from Location Sensor app
	 * 
	 * @param intent - Intent that invokes this Service
	 */
	@Override
	protected void onHandleIntent(Intent intent) {

        if(LOG_INFO) Log.i(TAG, "In onHandleIntent to handle "+intent.toURI());

        String locData = intent.getStringExtra(EXTRA_LOCATION_DATA);
        if(locData != null && locData.length() > 0){
            // Restore location data
            SmartProfileLocUtils.importLocationData(mContext, locData);
        } else if (intent.getExtras() != null) {
        	// Proceed further only if :
        	//   - The current location accuracy is acceptable
        	//   - There are no user tagged POI's already.
        	if((intent.getLongExtra(ACCURACY, MIN_ACCURACY_IN_METERS_FOR_INFERENCE + 1) 
        			< MIN_ACCURACY_IN_METERS_FOR_INFERENCE) &&
        			!RemoteLocationDatabaseOps.areThereAnyUserTaggedPOIs(mContext)) {
        		
        		// get the type of inference i.e. home or work or both or no inference.
        		int inferenceType = RemoteLocationDatabaseOps.getInferenceType(mContext);
        		
        		// If we already have two inferred locations then do not process this request further
        		// the inferenceType returned from RemoteLocationDatabaseOps.getInferenceType() should
        		// be None.
        		if(inferenceType == InferenceType.NONE)
        			Log.e(TAG, "Cannot proceed as we have a hit the limit on the inferred locations");
        		else   						
        			checkIfPassedLocIsPoi(intent.getLongExtra(LOC_ID, 0), intent.getDoubleExtra(LAT, 0.0), 
        									intent.getDoubleExtra(LNG, 0.0), inferenceType,
        									intent.getLongExtra(ACCURACY, MIN_ACCURACY_IN_METERS_FOR_INFERENCE + 1));
        	} else
        		Log.e(TAG, "Cannot proceed as we already have user tagged locations or Accuracy is low or already in a POI");
        }
    }

    /** query the location sensor database and find POI that meet the minimum requirement
     *  initialize at the top.
     * 
     * @param accuName - locId of the location from location sensor.
     * 					 This corresponds to a row-id in locatime table used for querying location sensor
     * 					 when a location change notification is received.
     * @param lat - Latitude of the location from location sensor.
     * @param lng - Latitude of the location from location sensor.
     * @param inferenceType - the type of inference (refer enum InferenceType in LocConstants)
     * @param accuracy - accuracy of the location to be inferred.
     */
    private void checkIfPassedLocIsPoi(final long locId, double lat, double lng, 
    									final int inferenceType, final long accuracy) {

        long spentTime = 0;
        long homePoiTime = 0;
        long workPoiTime = 0;
        
        // Get all matching locations with the same id from location sensor database
        String whereClause = ID + EQUALS + Q + locId + Q;
        String sortOrder = ACCURACY;
        String projection[] = new String[] {STARTTIME, ENDTIME};
        if(LOG_DEBUG)  Log.d(TAG, "checkIfPassedLocIsPoi whereClause: " + whereClause);
        Cursor locationCursor = mContext.getContentResolver().query(Uri.parse(LOC_CONSOLIDATE_URI),
                projection, whereClause, null, sortOrder);

	    if (locationCursor != null) {
	    	try {
	            if(LOG_DEBUG)  Log.d(TAG, "whereClause: " + whereClause + ", cursor count: " + locationCursor.getCount());
                // There should be a minimum of 10 entries for this location before we even try to process the
            	// request from location sensor to see if this is a meaningful location or not.
                if (locationCursor.getCount() >= MIN_ENTRIES_FOR_LOC_IN_DB && locationCursor.moveToFirst()) {               	                          
                    // Calculate the total spent time during 
                	// night (between 1AM and 5 AM for home) or day (between 1PM and 4 PM for work) 
                	// at this location
                    do {
                        // Start and end time for each entry in the cursor.
                        long sTimeStamp = locationCursor.getLong(locationCursor.getColumnIndexOrThrow(STARTTIME));
                        long eTimeStamp = locationCursor.getLong(locationCursor.getColumnIndexOrThrow(ENDTIME));
                        if (LOG_DEBUG) Log.d(TAG, "sTimeStamp = "+sTimeStamp+"; eTimeStamp = "+eTimeStamp);
                                      
                        switch(inferenceType) {
                        
	                        case InferenceType.BOTH:
	                        	homePoiTime += calcTimeBetweenBrackets(sTimeStamp, eTimeStamp, HOME_HOUR_START, HOME_HOUR_END);
	                        	workPoiTime += calcTimeBetweenBrackets(sTimeStamp, eTimeStamp, WORK_HOUR_START, WORK_HOUR_END);
	                        	break;
                        	
	                        case InferenceType.HOME_ONLY:
	                        	homePoiTime += calcTimeBetweenBrackets(sTimeStamp, eTimeStamp, HOME_HOUR_START, HOME_HOUR_END);
	                        	break;
	                        	
	                        case InferenceType.WORK_ONLY:
	                        	workPoiTime += calcTimeBetweenBrackets(sTimeStamp, eTimeStamp, WORK_HOUR_START, WORK_HOUR_END);
	                        	break;
	                        	
	                        default:
		                        homePoiTime += calcTimeBetweenBrackets(sTimeStamp, eTimeStamp, HOME_HOUR_START, HOME_HOUR_END);

                        }
                        spentTime += (eTimeStamp - sTimeStamp);
                        if (LOG_DEBUG) Log.d(TAG, "homePoiTime = "+homePoiTime+"; workPoiTime = "+workPoiTime+"; spentTime = "+spentTime);
                    } while(locationCursor.moveToNext());
                    
                    String suggestedName = getSuggestedPoiName(spentTime, homePoiTime, workPoiTime, inferenceType);
                    if(suggestedName != null) {
                    	// The tag and the name should be the same for inferred locations.
                    	if (RemoteLocationDatabaseOps.insertInferredPoi(mContext, lat, lng, suggestedName, suggestedName, accuracy))
                    		broadcastInferedPoiTag(suggestedName, suggestedName);
                    	else
                    		Log.e(TAG, "Error inserting POI to db, POI tag is " + suggestedName);
                    }
                
                } else
                	Log.e(TAG, "Entries count in Location Sensor DB "+locationCursor.getCount()+" < MIN_ENTRIES_FOR_LOC_IN_DB");
	        } catch (Exception e) {
	            e.printStackTrace();
	        } finally {
	        	if(!locationCursor.isClosed())
	        		locationCursor.close();
	        }
	    } 
	    else
	    	Log.e(TAG, "Location Sensor Cursor is null for whereClause "+whereClause);
        return;
    }

    /** gets the poi name for the location inferred based on the total time spent at this location.
     * 
     * @param spentTime - total time spent at this location
     * @param homePoiTime - total time spent between 1 AM and 5 AM (home inference time)
     * @param workPoiTime - total time spent between 1 PM and 4 PM (work inference time)
     * @param inferenceType - the type of inference (refer enum InferenceType in LocConstants)
     * @return - null or the poi name (Home or Work)
     */
    private String getSuggestedPoiName(final long spentTime, final long homePoiTime, 
    									final long workPoiTime, final int inferenceType) {
    	
    	String suggestedPoiName = null;
    	
        long spentTimeinMinutes = (spentTime/(1000 * 60)) ;
        if (LOG_DEBUG) Log.d(TAG, "spentTimeInMinutes = "+ spentTimeinMinutes);

        if (spentTimeinMinutes > MINUTES_IN_A_WEEK) {
        	
        	switch (inferenceType) {
	    		case InferenceType.BOTH:
	    			if (homePoiTime > HOME_MINUTES_FOR_INFERENCE)
	                	suggestedPoiName = mContext.getString(R.string.home_poitag);        			
	    			else if(workPoiTime > WORK_MINUTES_FOR_INFERENCE)
	                    	suggestedPoiName = mContext.getString(R.string.work_poitag);   				
	    			break;
	    		
	    		case InferenceType.HOME_ONLY:
	    			if (homePoiTime > HOME_MINUTES_FOR_INFERENCE)
	                	suggestedPoiName = mContext.getString(R.string.home_poitag);
	    			break;
	    		
	    		case InferenceType.WORK_ONLY:
	    			if(workPoiTime > WORK_MINUTES_FOR_INFERENCE)
	                	suggestedPoiName = mContext.getString(R.string.work_poitag);
	    			break;
        	}
        } else 
        	Log.e(TAG, "Total time "+spentTimeinMinutes+" is not enough to infer this as a meaningful location");
       
        if(LOG_DEBUG) Log.d(TAG, "Returning suggestedPoiName as "+suggestedPoiName);        
        return suggestedPoiName;
    }
    
    /** Calculates the home or work poi time spent at this location. This is used to 
     *  find the time spent at a particular location between time brackets (for work 
     *  between 1 PM and 4 PM and for home between 1 AM and 5 AM). This will calculate 
     *  the time between a set of "brackets" bracketed by hour of the day. Those brackets
     *  act as boundaries as start and end under which the sTimeStamp and eTimeStamp are 
     *  calcuated.
     *  
     *  NOTE: the startHour and endHour have not been tested across midnight i.e. endHour < startHour).
     *  Testing has been only done such that startHour is always less than endHour and these takes values
     *  from 1 to 24.
     *
     * @param sTimeStamp - start timestamp
     * @param eTimeStamp - end timestamp (can be on a different day or month or year than the sTimeStamp)
     * @param startHour - start bracket hour for our POI time calculation (either HOME_HOUR_START or WORK_HOUR_START)
     * @param endHour - end bracket hour for our POI time calculation (either HOME_HOUR_END or WORK_HOUR_END)
     * @return - returns the time spent at this home or work poi
     */
    private static long calcTimeBetweenBrackets(long sTimeStamp, long eTimeStamp, int startHour, int endHour) {

        long poiTime = 0;

        Calendar startDateStartTime = Calendar.getInstance();
        startDateStartTime.setTimeInMillis(sTimeStamp);
        startDateStartTime.set(startDateStartTime.get(Calendar.YEAR), startDateStartTime.get(Calendar.MONTH), startDateStartTime.get(Calendar.DATE), startHour, 0);
        long startDateStartTimeStamp = Math.max(startDateStartTime.getTimeInMillis(), sTimeStamp);

        Calendar startDateEndTime = Calendar.getInstance();
        startDateEndTime.set(startDateStartTime.get(Calendar.YEAR), startDateStartTime.get(Calendar.MONTH), startDateStartTime.get(Calendar.DATE), endHour, 0);
        long startDateEndTimeStamp = Math.min(startDateEndTime.getTimeInMillis(), eTimeStamp);

        Calendar endDateStartTime = Calendar.getInstance();
        endDateStartTime.setTimeInMillis(eTimeStamp);
        endDateStartTime.set(endDateStartTime.get(Calendar.YEAR), endDateStartTime.get(Calendar.MONTH), endDateStartTime.get(Calendar.DATE), startHour, 0);
        long endDateStartTimeStamp = Math.max(endDateStartTime.getTimeInMillis(), sTimeStamp);

        Calendar endDateEndTime = Calendar.getInstance();
        endDateEndTime.set(endDateStartTime.get(Calendar.YEAR), endDateStartTime.get(Calendar.MONTH), endDateStartTime.get(Calendar.DATE), endHour, 0);
        long endDateEndTimeStamp = Math.min(endDateEndTime.getTimeInMillis(), eTimeStamp);

        if(LOG_DEBUG)  Log.d(TAG, "startDateStartTimeStamp = "+startDateStartTimeStamp+"; startDateEndTimeStamp = "+startDateEndTimeStamp);
        if(LOG_DEBUG)  Log.d(TAG, "endDateStartTimeStamp = "+endDateStartTimeStamp+"; endDateEndTimeStamp = "+endDateEndTimeStamp);

        if(startDateEndTimeStamp > startDateStartTimeStamp)
            poiTime += (startDateEndTimeStamp - startDateStartTimeStamp)/(60 * 1000);
        
        int numberOfDays = Math.round((float) (endDateEndTimeStamp - startDateStartTimeStamp)/(24 * 60 * 60 * 1000L));

        if(startDateStartTime.get(Calendar.DATE) != endDateEndTime.get(Calendar.DATE) || numberOfDays > 0) {
            if(LOG_DEBUG)  Log.d(TAG, "The start time and end time are on different dates");

            if(endDateEndTimeStamp > endDateStartTimeStamp)
                poiTime += 	(endDateEndTimeStamp - endDateStartTimeStamp)/(60 * 1000);

            if(numberOfDays > 0)
                poiTime +=	(numberOfDays - 1) * 4 * 60;
        }
        return poiTime;
    }

    /** broadcasts the inferred poi tag to the inference manager.
     *
     * @param poiTag - poi tag for the location.
     * @param poiName - poi name for the location.	
     */
    private void broadcastInferedPoiTag(final String poiTag, final String poiName) {

        final String LOCATION_TAG   = "LOCATION_TAG";
        final String WIFI_SETTING   = "WIFI_SETTING";
        final String BT_SETTING     = "BT_SETTING";
        final String GPS_SETTING    = "GPS_SETTING";

        String wifiState,btState,gpsState;

        SmartProfileLocUtils locUtils = new SmartProfileLocUtils(mContext);
        wifiState = locUtils.getWifiState() ? SmartProfileLocUtils.ON:SmartProfileLocUtils.OFF;
        btState   = locUtils.getBtState()   ? SmartProfileLocUtils.ON:SmartProfileLocUtils.OFF;
        gpsState  = locUtils.getGpsState()  ? SmartProfileLocUtils.ON:SmartProfileLocUtils.OFF;

        Intent bIntent = new Intent(LOCATION_INFERRED_ACTION);
        bIntent.putExtra(BT_SETTING,   btState);
        bIntent.putExtra(GPS_SETTING,  gpsState);
        bIntent.putExtra(LOCATION_TAG, poiTag);
        bIntent.putExtra(WIFI_SETTING, wifiState);
        
        if(LOG_DEBUG)  Log.d(TAG, "Broadcasted Location Inference Intent: " + bIntent.toUri(0));
        mContext.sendBroadcast(bIntent);
    }
}