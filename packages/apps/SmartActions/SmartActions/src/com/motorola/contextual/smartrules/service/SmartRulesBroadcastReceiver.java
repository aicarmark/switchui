/*
 * @(#)SmartRulesBroadcastReceiver.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2010/12/09 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;

/** This is a receiver class for handling the pending intents that are fired by VSM.
 *
 *<code><pre>
 * CLASS:
 * 	 extends BroadcastReceiver.
 *
 *  implements
 *   Constants - for the constants used
 *
 * RESPONSIBILITIES:
 * 	 Starts the IntentService SmartRulesService to process the request from VSM
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 *</pre></code>
 */
public class SmartRulesBroadcastReceiver extends BroadcastReceiver implements Constants {

    private static final String TAG = SmartRulesBroadcastReceiver.class.getSimpleName();

    /** onReceive()
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        
        if(intent != null) {
        	String action = intent.getAction();
        	if(action != null) {
			if(LOG_DEBUG) Log.i(TAG, "In onReceive to handle "+intent.toUri(0));
			if(action.equals(ACTION_RULESTATE_CHANGED)) {

                    String ruleKey    = intent.getStringExtra(EXTRA_RULE_KEY);
                    String status  = intent.getStringExtra(EXTRA_STATE);
		            			         
		    		if(isValidParams(ruleKey, status)) {			        	
	        			if(LOG_DEBUG) Log.d(TAG, "starting the SmartRulesService");
			            
						Intent serviceIntent = new Intent(context, SmartRulesService.class);
						serviceIntent.putExtra(MM_RULE_KEY, ruleKey);
						serviceIntent.putExtra(MM_RULE_STATUS, status);
						context.startService(serviceIntent); 			        		
				}  else
		        		Log.e(TAG, "Input params invalid: ruleKey = "+(ruleKey == null?"null":ruleKey)+
		        					" eventArray = "+ status);
			} else if(action.equals(RULE_STATE_CHANGE)) {

                    if(LOG_DEBUG) Log.d(TAG, "starting the SmartRulesService");

                    Intent serviceIntent = new Intent(context, SmartRulesService.class);
                    serviceIntent.putExtra(MM_RULE_KEY, intent.getStringExtra(MM_CHANGED_RULE));
                    serviceIntent.putExtra(MM_RULE_STATUS, intent.getStringExtra(MM_RULE_STATUS));
                    serviceIntent.putExtra(MM_ENABLE_RULE, intent.getBooleanExtra(MM_ENABLE_RULE, false));
                    serviceIntent.putExtra(MM_DISABLE_RULE, intent.getBooleanExtra(MM_DISABLE_RULE, false));
                    context.startService(serviceIntent);

                } else if(action.equals(ACTION_SA_CORE_INIT_COMPLETE)) {
			Intent serviceIntent = new Intent(context, SmartRulesService.class);
                    serviceIntent.putExtra(EXTRA_SA_CORE_INIT_COMPLETE_SENT, true);
                    context.startService(serviceIntent);
		} else
	            	Log.e(TAG, "Receiver not invoked for handling = "+intent.getAction()+" which is not a subscribed action");
		}  else
        		Log.e(TAG, "Receiver invoked with a null action");
        } else
        	Log.e(TAG, "Receiver invoked with a null intent");
    }
    
    private boolean isValidParams(String ruleKey, String status) {
		
		boolean result = false;    
    	if(ruleKey != null && status != null) {   		
			if(status.equals(FALSE) || status.equals(TRUE))
				result = true;
			else
    			Log.e(TAG, "status is not valid (true or false) = "+ status);    		   		   
    	} else
    		Log.e(TAG, "Required input params are invalid: ruleKey = "+ruleKey
	        				+" or status = " +status );	    		
		return result;
	}

}