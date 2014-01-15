/*
 * @(#) RulesImporterBroadcastReceiver
 *
 * (c) COPYRIGHT 2010-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18172        2012/08/08 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.rulesimporter;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;

/**
 * This Receiver is currently exposed
 * (1) To import the rule published by Rule Publisher into smartrules DB, the XML is send as an extra
 * (2) Any generic import, where in XML can be send  as an extra as well as XML will read from 
 * specific location such as /sdcard/rules.d/rulesimporter.rules during TEST_IMPORT
 * or from /data/data/com.motorola.contextual.smartrules/files/myrules_restored.xml after
 * restoring rules from Google Server.
 */

public class RulesImporterBroadcastReceiver extends BroadcastReceiver implements Constants {

    private static final String TAG = RulesImporterBroadcastReceiver.class.getSimpleName();

    /** onReceive()
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent == null) {
            Log.e(TAG, "intent is null");
            return;
        }

        String action = intent.getAction();
        if(action != null){
            if(action.equals(LAUNCH_CANNED_RULES)         ||
               action.equals(LAUNCH_IMPORTER_FROM_RP)) {
                if (LOG_DEBUG) Log.d(TAG, "In onReceive to handle "+intent.toUri(0));

                int importType = XmlConstants.ImportType.IGNORE;

                if (action.equals(LAUNCH_IMPORTER_FROM_RP)){ 
                    importType = XmlConstants.ImportType.RP_RULE;
                } else {
                    importType = intent.getIntExtra(IMPORT_TYPE, XmlConstants.ImportType.IGNORE);
                }

                Float rpCoreVersion = intent.getFloatExtra(EXTRA_VERSION, INVALID_VERSION);
                
                if(importType == XmlConstants.ImportType.RP_RULE  &&
                		((rpCoreVersion == INVALID_VERSION) ||
                		(rpCoreVersion > VERSION))){
                	Log.e(TAG,"Ignore Request :" +
                			"RP's Core Protocol Version does not match with Core => "+
                			" RP Core Version : "+rpCoreVersion+" Core Version : "+VERSION);
                }else{

                	Intent serviceIntent = new Intent(context, RulesImporterService.class);
	                serviceIntent.putExtra(IMPORT_TYPE, importType);

	                Bundle b = intent.getExtras();
	                if (b != null) serviceIntent.putExtras(b);

	                if(LOG_INFO) Log.d(TAG, "Starting RI Service with importType=" + importType);

	                ComponentName component = context.startService(serviceIntent);
	                if (component == null) {
	                    Log.e(TAG, "context.startService() failed for: " +
	                              RulesImporterService.class.getSimpleName());
	                }
                }
            } else
                Log.e(TAG, "Action type does not match");
        }
    }
}
