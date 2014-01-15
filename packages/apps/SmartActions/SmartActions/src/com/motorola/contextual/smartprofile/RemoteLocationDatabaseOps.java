/*
 * @(#)RemoteLocationDatabaseOps.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/04/29 NA				  Initial version
 *
 */
package com.motorola.contextual.smartprofile;

import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartprofile.locations.LocConstants;
import com.motorola.contextual.smartprofile.locations.LocationUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.util.Log;

/**
 * This class performs the update, query, insert and delete operations on the
 * Location Sensor Database.
 *
 *<code><pre>
 * CLASS:
 * 	None.
 *
 * 	implements
 *      Constants - for the constants used
 *
 * RESPONSIBILITIES:
 * 	Perform Database updates.
 *
 * COLABORATORS:
 * 	Uses LocationSensor provider to update, query, insert and delete locations.
 *
 * USAGE:
 *  None
 *</pre></code>
 **/
public class RemoteLocationDatabaseOps implements Constants, LocConstants, DbSyntax {

	private static final String TAG = RemoteLocationDatabaseOps.class.getSimpleName();

    /** used to store the DB column values for processing
     */
    public static class LocDbColumns {
    	public long		_id;
    	public String 	name;
    	public String   accuName;
    	public String   address;
    	public String   poiTag;
    	public double 	lat;
    	public double 	lng;
    	public float    radius;
    	public long  	startTime;
    	public long   	endTime;
    	public int    	freqCount;
    	public int    	count;
    	public String	cellJsonValue;
    	public String   poiType;
    	public boolean  isChecked;
    	public String	wifiSSID;
    };

    /** Inserts a new record for the inferred location into the poi table of
     *  Location Sensor database.
     *
     * @param context - application context
     * @param lat - Latitude of the location from location sensor
     * @param lng - Latitude of the location from location sensor
     * @param poiTag - tag that is associated with this location
     * @string name - name that is associated with this location
     * @param radius - accuracy of the location
     */
    public static boolean insertInferredPoi(final Context context, final double lat, 
    											double lng, final String poiTag,
    											final String name, final long radius) {
    	boolean result = false;
    	
    	if (poiTag != null) {
        	ContentValues contentValues = new ContentValues();
            contentValues.put(POI, poiTag);
            contentValues.put(POI_LAT, lat);
            contentValues.put(POI_LNG, lng);
            contentValues.put(NAME, name);
            contentValues.put(RADIUS, radius);
            contentValues.put(POITYPE, SUGGESTED_POI);
            if(LOG_DEBUG)  Log.d(TAG, "newColVal = "+contentValues.toString());

            Uri newUri = context.getContentResolver().insert(Uri.parse(LOC_POI_URI), contentValues);
            if (newUri == null)
                Log.e(TAG, "NULL URI on insert");                    
            else {       
                    try {
                            long _id = Long.valueOf(newUri.getLastPathSegment());
                            result = (_id > 0);
                            if (result) {
                         	   SmartProfileLocUtils.exportLocationData(context, true);
                            }
                    } catch (NumberFormatException e){
                            Log.e(TAG, newUri.toString());
                    }
            }
            if(LOG_DEBUG)  Log.d(TAG, "insertedUri = "+newUri);
        }
        else {
            Log.e(TAG, "Invalid input params: poiTag is Null");
        }
    	return result;
    }

    /** checks the Location Sensor DB to see if there are any User tagged POI locations.
     *
     * @param context - application context
     * @return - true is there are User tagged POI locations, else false
     */
    public static boolean areThereAnyUserTaggedPOIs(final Context context) {
    	boolean result = false;
    	String whereClause = POITYPE + EQUALS + Q + USER_POI + Q;
    	Cursor locationCursor = context.getContentResolver().query(Uri.parse(LOC_POI_URI), new String[] {ID}, whereClause, null, null);
    	if(locationCursor != null) {
    		try {
    			if(locationCursor.moveToFirst())
    				if(locationCursor.getCount() > 0) {
    					if(LOG_DEBUG) Log.d(TAG, "User tagged locations so do not proceed");
    					result = true;
    				}
    		} catch (Exception e) {
    			e.printStackTrace();
    		} finally {
    			if(! locationCursor.isClosed())
    				locationCursor.close();
    		}
    	} else
			Log.e(TAG, "location cursor is null for whereClause "+whereClause);
    	if(LOG_DEBUG) Log.d(TAG, "Returning from areThereAnyUserTaggedPOIs "+result);
    	return result;
    }

    /** return the type of inference based on the count of the inferred location in the
     *  Location Sensor DB.
     *
     * @param context - application context
     * @return - the type of inference (refer enum InferenceType in LocConstants)
     */
    public static int getInferenceType(final Context context) {
    	int inferenceType = InferenceType.BOTH;
		String whereClause = POITYPE + EQUALS + Q + SUGGESTED_POI + Q
								+ OR + POITYPE + EQUALS + Q + ACCEPTED_POI + Q;
        String projection[] = new String[] {ID, POI};

		Cursor locationCursor = context.getContentResolver().query(Uri.parse(LOC_POI_URI),
										projection, whereClause, null, null);
    	if(locationCursor != null) {
    		try {
    			// if we cannot do a locationCursor.moveToFirst() then it means home and work
    			// have not been inferred.
    			if(locationCursor.moveToFirst()) {
    				int count = locationCursor.getCount();
    				if(count >= MAX_INFERRED_LOCATIONS)  {
    					// This means we have already inferred both home and work and need not
    					// infer any further locations.
    					inferenceType = InferenceType.NONE;
    				}
    				else {
    					String currentPoiTag = locationCursor.getString(locationCursor.getColumnIndex(POI));
    					if(LOG_DEBUG) Log.d(TAG, "poiTag = "+currentPoiTag);
						if(count == 1 && currentPoiTag.equals(context.getString(R.string.home_poitag))){
							// This means location home has already been inferred and we need to infer
							// for work only.
							inferenceType = InferenceType.WORK_ONLY;
						}
						else if(count == 1 && currentPoiTag.equals(context.getString(R.string.work_poitag))) {
							// This means location work has already been inferred and we need to infer
							// for home only.
							inferenceType = InferenceType.HOME_ONLY;
						}
    				}
    			}
    		} catch (Exception e) {
    			e.printStackTrace();
    		} finally {
    			if(! locationCursor.isClosed())
    				locationCursor.close();
    		}
    	} else
			Log.e(TAG, "location cursor is null for whereClause "+whereClause);

    	if(LOG_DEBUG) Log.d(TAG, "Returning inferenceType as "+inferenceType);
    	return inferenceType;
    }

    /** updates the current poi location being viewed into the POI table of location sensor DB
     *  when user selects the Save or Confirm buttons.
     *
     * @param context - application context
     * @param columnsData - LocDbColumns holding the columns data to add to the DB
     * @param poiType - type of POI (SUGGESTED_POI, ACCEPTED_POI, USER_POI)
     * @param name - name of the POI
     * @return - the number of rows updated
     */
    public static int updatePoiLocInDb(final Context context, final LocDbColumns columnValues,
            final String poiType, final String name) {

        if(LOG_DEBUG) Log.d(TAG, "updatePoiLocInDb for "+columnValues._id+" poiType is "+poiType);

        ContentValues contentValues = new ContentValues();
        contentValues.put(ID, columnValues._id);
        contentValues.put(POI, columnValues.poiTag);
        contentValues.put(POI_LAT, columnValues.lat);
        contentValues.put(POI_LNG, columnValues.lng);
        contentValues.put(RADIUS, columnValues.radius);
        contentValues.put(ADDRESS, columnValues.address);
        contentValues.put(NAME, name);
        contentValues.put(CELLJSONS, columnValues.cellJsonValue);
       	contentValues.put(POITYPE, poiType);
       	contentValues.put(WIFISSID, columnValues.wifiSSID);

        String whereClause = ID + EQUAL + Q + columnValues._id + Q;
        int rowsUpdated = context.getContentResolver().update(Uri.parse(LOC_POI_URI), contentValues, whereClause, null);

        if(rowsUpdated > 0){
            // take a backup of location data
            SmartProfileLocUtils.exportLocationData(context, true);
            LocationUtils.initiateRefreshRequest(context);
        }

        return rowsUpdated;
    }

    /** adds the entered poiTag into the poi table in Location Sensor database
    *
    * @param context - application context
    * @param poiTag - poiTag entered by the user
    * @param columnsData - LocDbColumns holding the columns data to add to the DB
    */
   public static Uri addPoiTagTableEntry(final Context context, final String poiTag,
       final LocDbColumns columnsData) {

       ContentValues contentValues = new ContentValues();
       contentValues.put(POI, poiTag);
       contentValues.put(POI_LAT, columnsData.lat);
       contentValues.put(POI_LNG, columnsData.lng);
       contentValues.put(RADIUS, columnsData.radius);
       contentValues.put(ADDRESS, columnsData.address);
       contentValues.put(NAME, columnsData.name);
       contentValues.put(CELLJSONS, columnsData.cellJsonValue);
       contentValues.put(POITYPE, USER_POI);
       if(LOG_DEBUG) Log.d(TAG, "newColVal = "+contentValues.toString());

       Uri newUri = context.getContentResolver().insert(Uri.parse(LOC_POI_URI), contentValues);

       if(newUri != null){
           SmartProfileLocUtils.exportLocationData(context, true);
       }

       if(LOG_DEBUG) Log.d(TAG, "insertedUri = "+newUri);

       return newUri;
   }

   /** delete the entry with the poiTag from the poi table in Location Sensor database.
    *
    * @param context - application context
    * @param poiTag - poiTag that needs to be deleted in the poi table
    */
   public static void deletePoiTag(final Context context, final String poiTag) {
       //Deleting from the POI Table.
       String whereClause = POI + EQUAL + Q + poiTag.replace(Q, QUOTE_REPLACE) + Q;
       int rowsDeleted = context.getContentResolver().delete(Uri.parse(LOC_POI_URI), whereClause, null);

       if(rowsDeleted > 0){
           SmartProfileLocUtils.exportLocationData(context, true);
       }
   }

   /** Receives data backed up by RulesImport.
    *   - Update the POI row, except the name if already exists else.
    *   - Just insert a new row
    *
    * @param ct - Context
    * @param dbColumns - LocDbColumns holding the columns data to add to the DB
    */
    public static void checkAndImportPoiData(Context ct, LocDbColumns dbColumns){

        String poiTag = dbColumns.poiTag;
        String whereClause = POI + EQUAL + Q + poiTag + Q;

        ContentValues contentValues = new ContentValues();
        contentValues.put(POI_LAT, dbColumns.lat);
        contentValues.put(POI_LNG, dbColumns.lng);
        contentValues.put(RADIUS, dbColumns.radius);
        contentValues.put(ADDRESS, dbColumns.address);
        contentValues.put(CELLJSONS, dbColumns.cellJsonValue);
        contentValues.put(POITYPE, dbColumns.poiType);

        if(LOG_DEBUG) Log.d(TAG, "checkAndImportPoiData values = "+contentValues.toString());

        // Update if the record exists
        Cursor cursor = ct.getContentResolver().query(Uri.parse(LOC_POI_URI), new String[] {NAME}, whereClause, null, null);
        if(cursor != null && cursor.moveToFirst() && cursor.getCount() > 0){

        	// Pass the ID also only if it is an update
            contentValues.put(ID, dbColumns._id);

            /*
             * We should never overwrite user defined location name as he/she might have changed it
             * between expert/import cycle. Overwrite only when Name in DB is null or empty
             */

            // Get the user defined name from the existing db entry
            String name = null;
            int nameCol = cursor.getColumnIndex(NAME);
            if(nameCol != -1)
                name = cursor.getString(nameCol);

            // Do this only when existing user defined location Name in DB was null/empty
            if(name == null || name.length() ==0)
                contentValues.put(NAME, dbColumns.name);

            int rows = ct.getContentResolver().update(Uri.parse(LOC_POI_URI), contentValues, whereClause, null);
            if(LOG_DEBUG) {
				if (rows > 0) {
				    Log.d(TAG, "checkAndImportPoiData rows updated = " + rows);
				} else if(rows == 0){
				    Log.w(TAG, "checkAndImportPoiData rows Updated Failed!");
				}
			}

            cursor.close();
        } else { // add a new location entry
            if(cursor != null) cursor.close();

            // add name and POI
            contentValues.put(NAME, dbColumns.name);
            contentValues.put(POI, dbColumns.poiTag);

            // add a new row
            Uri uri = ct.getContentResolver().insert(Uri.parse(LOC_POI_URI), contentValues);

            if(LOG_DEBUG) {
				if (uri != null)
					Log.d(TAG, "checkAndImportPoiData Row Inserted = "+ dbColumns.toString());
			}
       }

        // Update the backed up data
       SmartProfileLocUtils.exportLocationData(ct, false);
   }

    /**
     * We need to update SensorName in ConditionTable when user updates
     * the location name
     *
     * @param context - context
     * @param oldName - Name of the location before user changed it
     * @param newName - Name of the location after user changed it
     *
     */
    @SuppressWarnings("unused")
	private static void updateRuleConditionName(Context context, final String oldName, final String newName){

        final String LOCATION_PUBLISHER_KEY = "com.motorola.contextual.smartprofile.location";
        ContentResolver cr = context.getContentResolver();

        /*
         *  First, update the Rules where conditions have one location only
         *  - simple cases (Home, Work, etc)
         */
        ContentValues cv = new ContentValues();
        cv.put(CONDITON_DESCRIPTION, newName);
        String whereClause =    CONDITON_DESCRIPTION + EQUALS + Q + oldName + Q + AND +
                                CONDITON_PUBLISHER_KEY + EQUALS + Q + LOCATION_PUBLISHER_KEY + Q;

        int rows = context.getContentResolver().update(Uri.parse(Constants.CONDITION_TABLE_URI), cv, whereClause, null);

        /*
         * Now we traverse thru all rows whose condition use oldName location along with other locations
         * - Complex cases (ex: Home or Work or Gym)
         */

        // query all rules that may Contain oldName location sensor
        whereClause =    CONDITON_DESCRIPTION + LIKE + Q + LIKE_WILD + oldName + LIKE_WILD + Q + AND +
                                CONDITON_PUBLISHER_KEY + EQUALS + Q + LOCATION_PUBLISHER_KEY + Q;

        Cursor cursor = cr.query(Uri.parse(Constants.CONDITION_TABLE_URI), new String[] {CONDITON_DESCRIPTION},
                whereClause, null, null);

        if(cursor == null)
            Log.e(TAG, "NULL cursor in updateRuleConditionName");
        else{
            try{
                if(cursor.moveToFirst() && cursor.getCount() > 0){
                    int sensorNameCol = cursor.getColumnIndexOrThrow(CONDITON_DESCRIPTION);

                    do{ // Go thru each rule
                        String name = cursor.getString(sensorNameCol);

                        // split it into each location
                        String[] locations = name.split(OR.toLowerCase());

                        // check each location in the condition
                        StringBuilder str = new StringBuilder();
                        for(String loc : locations){

                            // replace oldName with newName
                            if(loc.equals(oldName))
                                loc = newName;

                            // rebuild condition string
                            if(str.length() > 0)
                                str.append(OR.toLowerCase());

                            str.append(loc);
                        }

                        cv.clear();
                        cv.put(CONDITON_DESCRIPTION, str.toString());

                        // Update condition at a time
                        String where = CONDITON_DESCRIPTION + EQUALS + Q + name + Q;
                        rows = cr.update(Uri.parse(Constants.CONDITION_TABLE_URI), cv, where, null);

                        if(LOG_DEBUG) {
							if (rows == 0)
								Log.w(TAG, "Locatoin name update failed! ="+name);
						}
                    } while (cursor.moveToNext());
                }
            } catch(Exception e){
                e.printStackTrace();
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Returns the location Name for the poiTag
     *
     * @param ct
     * @param poiTag
     * @return
     */
    @SuppressWarnings("unused")
	private static String getLocationName(Context ct, final String poiTag){

        String name = null;
        String whereClause = POI + EQUAL + Q + poiTag + Q;
        Cursor locationCursor = ct.getContentResolver().query(Uri.parse(LOC_POI_URI),
                new String[] {NAME}, whereClause, null, null);

        if(locationCursor == null)
            Log.e(TAG, "location cursor is null for whereClause "+whereClause);
        else{
            try {
                if(locationCursor.moveToFirst()){
                    int nameCol = locationCursor.getColumnIndexOrThrow(NAME);

                    name = locationCursor.getString(nameCol);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(! locationCursor.isClosed())
                   locationCursor.close();
            }
        }

        return name;
    }

    /** fetch the location details in DB and store it into LocDbColumns type and return it.
     *
     * @param context - application context
     * @param poiTag - poi tag associated with the location
     * @return - null or a variable of LocDbColumns type populated with the values
     */
    public static LocDbColumns fetchLocationDetailsFromDb(final Context context, final String poiTag) {

    	LocDbColumns locDbCols = null;
        if(poiTag != null) {
        	locDbCols = new LocDbColumns();
            String whereClause = POI + EQUALS + Q + poiTag.replace(Q, QUOTE_REPLACE) + Q;
            Cursor cursor = context.getContentResolver().query(Uri.parse(LOC_POI_URI), null, whereClause, null, null);
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    if(LOG_DEBUG) {
                        Log.e(TAG, "dumping cursor for whereClause = "+whereClause+" and count is "+cursor.getCount());
                        DatabaseUtils.dumpCursor(cursor);
                    }
                    locDbCols.poiTag = poiTag;
                    locDbCols._id = cursor.getLong(cursor.getColumnIndex(ID));
                    locDbCols.name = cursor.getString(cursor.getColumnIndex(NAME));
                    locDbCols.address = cursor.getString(cursor.getColumnIndex(ADDRESS));
                    locDbCols.cellJsonValue = cursor.getString(cursor.getColumnIndex(CELLJSONS));
                    locDbCols.lat = cursor.getDouble(cursor.getColumnIndex(POI_LAT));
                    locDbCols.lng = cursor.getDouble(cursor.getColumnIndex(POI_LNG));
                    locDbCols.radius = cursor.getFloat(cursor.getColumnIndex(RADIUS));
                    locDbCols.poiType = cursor.getString(cursor.getColumnIndex(POITYPE));
                    locDbCols.wifiSSID = cursor.getString(cursor.getColumnIndex(WIFISSID));

                }
                cursor.close();
            }
        }
        return locDbCols;
    }
}