/*
 * @(#)RegisterListeners.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21693        2012/05/16  NA               Initial version
 *
 */
package com.motorola.contextual.rule.receivers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.rule.Constants;
import com.motorola.contextual.rule.CoreConstants;
import com.motorola.contextual.smartrules.monitorservice.CommonMonitorService;
/** 
 * This class contains the methods to register or
 * unregister listeners
*
*<code><pre>
*
* CLASS:
* extends None
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
* 
* @author a21693
*
*/
public abstract class RegisterListeners implements CoreConstants{

    /**
     * Method to register or unregister receiver to listen to generic broadcasts
     *
     * @param ct - context
     * @param tag - debug tag
     * @param classNameString - Class that contains the receiver
     * @param register - true or false
     */
    public static void register(Context ct, String tag, String classNameString, boolean register){

        if(Constants.LOG_INFO) Log.i(tag, (register?"":"Un") + "Register: "  + classNameString);

        // First start the common monitor service
        Intent serviceIntent = new Intent(ct, CommonMonitorService.class);

        serviceIntent.putExtra(CommonMonitorService.EXTRA_REGISTER, register);
        serviceIntent.putExtra(CommonMonitorService.EXTRA_STATE_MON_CLASS, classNameString);

        ComponentName component = ct.startService(serviceIntent);
        if (component == null) {
            Log.e(tag, "context.startService() failed for: "
                    + CommonMonitorService.class.getSimpleName());
        }
    }

    /**
     * Method to subscribe for CP state Notify events
     *
     * @param ct - context
     * @param tag - debug tag
     * @param pubKey - pub key
     * @param config - config to subscribe to
     * @param ruleKey - rule key
     * @param register - true to register
     */
    public static void subscribeCpNotify(Context ct, String tag,
            String pubKey, String config, String ruleKey, boolean register){

        String event = register?CoreConstants.SUBSCRIBE_REQUEST:CoreConstants.SUBSCRIBE_CANCEL;

        Intent intent = new Intent(com.motorola.contextual.smartrules.Constants.ACTION_CONDITION_PUBLISHER_REQUEST);
        intent.putExtra(CoreConstants.EXTRA_EVENT_TYPE, event);

        intent.putExtra(CoreConstants.REQUEST_ID_EXTRA, ruleKey);
        intent.putExtra(CoreConstants.CONFIG_EXTRA, config);
        intent.putExtra(CoreConstants.EXTRA_PUB_KEY, pubKey);
        intent.putExtra(CoreConstants.EXTRA_CONSUMER, Constants.PUBLISHER_KEY);
        intent.putExtra(CoreConstants.EXTRA_CONSUMER_PACKAGE, com.motorola.contextual.smartrules.Constants.PACKAGE);

        if(Constants.LOG_INFO)Log.i(tag, (register?"":"Un") + "Register: " + config);

        ct.sendBroadcast(intent);
    }
}