/*
 * @(#)SmartProfileLocUtils.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2010/09/11 NA				  Initial version
 *
 */
package com.motorola.contextual.smartprofile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.motorola.contextual.smartprofile.RemoteLocationDatabaseOps.LocDbColumns;
import com.motorola.contextual.smartprofile.locations.LocConstants;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

/**
 * This class is an utility class for smart profile location.
 *
 *<code><pre>
 * CLASS:
 * 	None.
 *
 * 	implements
 *      Constants - for the constants used
 *
 * RESPONSIBILITIES:
 *  None
 *
 * COLABORATORS:
 * 	None
 *
 * USAGE:
 *  None
 *</pre></code>
 **/
public class SmartProfileLocUtils implements Constants, LocConstants {

    private final static String TAG = SmartProfileLocUtils.class.getSimpleName();

    public Context mContext;

    public static final String ON  = "ON";
    public static final String OFF = "OFF";

    public SmartProfileLocUtils(Context ctx) {
        mContext = ctx;
    }

    public boolean getWifiState() {
        WifiManager wifiMgr = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        if (wifiMgr == null) {
            if(LOG_DEBUG) Log.d(TAG,"Null wifiMgr");
            return false;
        }
        boolean state = wifiMgr.isWifiEnabled();
        return state;
    }

    public boolean getBtState() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            if(LOG_DEBUG) Log.d(TAG,"Null btAdapter");
            return false;
        }
        boolean state = btAdapter.isEnabled();
        return state;
    }

    public boolean getGpsState() {
        LocationManager locManager = (LocationManager) mContext.getSystemService( Context.LOCATION_SERVICE );

        if (locManager == null) {
            if(LOG_DEBUG) Log.d(TAG,"Null locManager");
            return false;
        }
        boolean state = locManager.isProviderEnabled( LocationManager.GPS_PROVIDER );
        return state;
    }

    /**
     *  Utility Method which exports location data in a separate thread only if
     *  called from UI thread. Export happens on caller thread if isUithread
     *  is false
     *
     *  @param context - Application Context
     *  @param isUiThread - if the method is being called from UI thread
     */
    public static void exportLocationData(final Context context, boolean isUiThread) {

        // Is export ON?
        if (EXPORT_LOCATION_DATA) {

            if(isUiThread){
                Intent intent = new Intent();
                intent.setClass(context, ExportPrefService.class);
                context.startService(intent);
            } else {
                exportLocationData(context);
            }
        }
    }

    /**
     *  Utility Method which creates an array of JSON Objects of the
     *  Location DB and exports it
     *
     *  @param context - Application Context
     */
    public static void exportLocationData(final Context context){
    	// Is export ON?
        if (EXPORT_LOCATION_DATA) {
        	
        
	        final JSONArray locJSONObj = new JSONArray();
	
	        if(LOG_DEBUG) Log.d(TAG, "Exporting Location Data");
	
	        Cursor cursor = context.getContentResolver().query(Uri.parse(LOC_POI_URI), null, null, null, null);
	
	        if (cursor == null) {
	            Log.e(TAG, " Location DB Cursor is null");
	        } else {
	
	            if (LOG_DEBUG) Log.d(TAG, " No. of location entries : " + cursor.getCount());
	
	            try {
	                if(cursor.moveToFirst()) {
	                    do {
	                        JSONObject jsonObj = new JSONObject();

                            String data = cursor.getString(cursor.getColumnIndexOrThrow(CELLJSONS));
                            if (data != null && data.length() > 0) {
                                jsonObj.put(CELLJSONS, data);
                            }
	
	                        // Copy all the data into JSON Object
	                        jsonObj.put(ID, cursor.getLong(cursor.getColumnIndex(ID)));
	                        jsonObj.put(POI, cursor.getString(cursor.getColumnIndex(POI)));
	                        jsonObj.put(POI_LAT, cursor.getString(cursor.getColumnIndexOrThrow(POI_LAT)));
	                        jsonObj.put(POI_LNG, cursor.getString(cursor.getColumnIndexOrThrow(POI_LNG)));
	                        jsonObj.put(RADIUS, cursor.getString(cursor.getColumnIndexOrThrow(RADIUS)));
	                        jsonObj.put(NAME, cursor.getString(cursor.getColumnIndexOrThrow(NAME)));
	                        jsonObj.put(ADDRESS, cursor.getString(cursor.getColumnIndexOrThrow(ADDRESS)));
	                        jsonObj.put(POITYPE, cursor.getString(cursor.getColumnIndexOrThrow(POITYPE)));
	
	                        if(LOG_DEBUG) Log.d(TAG, " JSON data = { " + jsonObj.toString() + " } ");
	
	                        locJSONObj.put(jsonObj);
	
	                    } while (cursor.moveToNext());
	                }
	            } catch (Exception e) {
	                e.printStackTrace();
	            } finally {
	                cursor.close();
	            }
	        }
	
	        if(locJSONObj.length() > 0) {
	            if (LOG_DEBUG) Log.d(TAG," Sending Rules Exporter Intent ");

	            Intent intent = new Intent(LAUNCH_RULES_EXPORTER);

	            // ======== Phoenix Security Changes ========
	            // Writing the xml data into shared preference 
	            // to prevent other apps from snooping the data
	
	            SharedPreferences locPref = context.getSharedPreferences(LOCATION_SHARED_PREFERENCE, Context.MODE_PRIVATE);
	            SharedPreferences.Editor editor = locPref.edit();
	            editor.clear();
	            editor.putString(LOCATION_XML_CONTENT, locJSONObj.toString());
	            editor.commit();
	            
	            if(LOG_DEBUG)  Log.d(TAG, "Location Data updated !!!");
	            
	            context.sendBroadcast(intent);
	        } else {
	            if (LOG_DEBUG) Log.d(TAG," JSON array length :  " + locJSONObj.length());
	        }
        }
    }

    /** Populate column data from JSON Object and insert/update in the DB
     *
     * @param context - Context
     * @param locData - Location data string
     */
    public static void importLocationData(Context context, final String locData){

        try {
            JSONArray locArray = new JSONArray(locData);

            for (int i=0; i < locArray.length(); i++) {

                JSONObject locJSON = locArray.getJSONObject(i);
                LocDbColumns dbColumn = new LocDbColumns();

                dbColumn._id = locJSON.getLong(ID);
                dbColumn.lat = locJSON.getDouble(POI_LAT);
                dbColumn.lng = locJSON.getDouble(POI_LNG);
                dbColumn.radius = (float) locJSON.getDouble(RADIUS);
                dbColumn.name = locJSON.getString(NAME);
                dbColumn.address = locJSON.getString(ADDRESS);
                String cellJsons = locJSON.optString(CELLJSONS);
                if (cellJsons != null && !cellJsons.equals("NA") && !cellJsons.equals(""))
                    dbColumn.cellJsonValue = locJSON.getString(CELLJSONS);
                dbColumn.poiType = locJSON.getString(POITYPE);
                dbColumn.poiTag = locJSON.getString(POI);

                // Update DB
                RemoteLocationDatabaseOps.checkAndImportPoiData(context, dbColumn);
            }
        } catch (JSONException e) {
            Log.e(TAG, "importLocationData: Invalid locData="+ locData);
            e.printStackTrace();
        }
    }
}
