/*
 * @(#)RulesExporterBroadcastReceiver.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18172        2011/04/29 NA				  Initial version
 */

package com.motorola.contextual.smartrules.rulesimporter;



import java.util.ArrayList;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.table.RuleTable;

import android.app.backup.BackupManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This Receiver is currently exposed to  export the XML String 
 * for a specific set of Rules based on Source List to the server
 */
public class RulesExporterBroadcastReceiver extends BroadcastReceiver implements Constants {

    private static final String TAG = RulesExporterBroadcastReceiver.class.getSimpleName();

    /** onReceive()
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent == null) {
            Log.e(TAG, "intent is null");
        }
        else {
        		String action = intent.getAction();
	            if (LOG_DEBUG) Log.d(TAG, "In onReceive to handle "+action);
	            
	        	if(action != null){
	        		if (action.equals(LAUNCH_RULES_EXPORTER)) {

		            boolean isExportToSdCard = intent.getBooleanExtra(EXTRA_IS_EXPORT_TO_SDCARD, false);
		            
		            Intent serviceIntent = new Intent(context, RulesExporterService.class);
		            
		            if(!isExportToSdCard){
		            	
		            	if (LOG_DEBUG) Log.d(TAG, "Exporting to the Server");
			            ArrayList<String> sourceList = intent.getStringArrayListExtra(EXTRA_SOURCE_LIST);
	
			            if (sourceList == null) {
			            	sourceList = new ArrayList<String>();
			            	sourceList.add(Integer.toString(RuleTable.Source.USER));
			                sourceList.add(Integer.toString(RuleTable.Source.SUGGESTED));
	                        sourceList.add(Integer.toString(RuleTable.Source.FACTORY));
			            }
			            serviceIntent.putStringArrayListExtra(EXTRA_SOURCE_LIST, sourceList);
	
			            String tfData = intent.getStringExtra(EXTRA_TIMEFRAME_DATA);
			            if(tfData != null) {
			            	if (LOG_DEBUG) Log.d(TAG," Time Frame data :  " + tfData);
			            	serviceIntent.putExtra(EXTRA_TIMEFRAME_DATA, tfData);
			            }
	
			            String locData = intent.getStringExtra(EXTRA_LOCATION_DATA);
			            if(locData != null) {
			                if (LOG_DEBUG) Log.d(TAG," Location data :  " + locData);
			                serviceIntent.putExtra(EXTRA_LOCATION_DATA, locData);
			            }

			            String calendarEventsData = intent.getStringExtra(EXTRA_CALENDAR_EVENTS_DATA);
			            if(calendarEventsData != null) {
					if(LOG_DEBUG) {
						Log.d(TAG, " Calendar events data : " + calendarEventsData);
					}
					serviceIntent.putExtra(EXTRA_CALENDAR_EVENTS_DATA, calendarEventsData);
			            }
		            }else{
		            	if (LOG_DEBUG) Log.d(TAG,"Exporting to SD Card");
		            	serviceIntent.putExtra(EXTRA_IS_EXPORT_TO_SDCARD, isExportToSdCard);
		            }

		            ComponentName component = context.startService(serviceIntent);
		            if (component == null) {
		                Log.e(TAG, "context.startService() failed for: " +
		                      RulesExporterService.class.getSimpleName());
		            }
	        	} else if (action.equals(NOTIFY_DATA_CHANGE)){
	        		
	        		if (LOG_DEBUG) Log.d(TAG,"BackupManager Notified"); 
	        		(new BackupManager(context)).dataChanged();	  
	        		
	        	} else
	        		Log.e(TAG, "Action type does not match");
	      }
       }
    }
}
