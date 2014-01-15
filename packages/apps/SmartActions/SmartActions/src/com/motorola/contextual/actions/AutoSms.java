/*
 * @(#)AutoSms.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/03/28  NA                  Initial version
 *
 */
package com.motorola.contextual.actions;

import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.content.Intent;

/**
 * This class represents Auto reply text action
 *
 * <code><pre>
 * CLASS:
 *     Extends StatelessAction
 *
 * RESPONSIBILITIES:
 *    This class implements the methods needed to respond to fire, revert, refresh
 *    and list commands. It gets the necessary information in the form of an Intent
 *
 * COLABORATORS:
 *     Smart Actions Core
 *
 * USAGE:
 *      See individual methods.
 *
 * </pre></code>
 **/

public class AutoSms extends StatelessAction implements Constants {

    @Override
    public ReturnValues fireAction(Context context, Intent configIntent) {
        return enableAutoReplyText (context, configIntent, true);
    }

    @Override
    public String getActionString(Context context) {
        return context.getString(R.string.auto_reply_text);
    }

    @Override
    public String getDescription(Context context, Intent configIntent) {
        String description = null;
        if (configIntent != null) {
            description = DOUBLE_QUOTE + configIntent.getStringExtra(EXTRA_SMS_TEXT) + DOUBLE_QUOTE;
        }
        return description;
    }

    @Override
    public String getUpdatedConfig(Context context, Intent configIntent) {
        return getConfig(configIntent.getStringExtra(EXTRA_INTERNAL_NAME),
                configIntent.getStringExtra(EXTRA_NUMBERS), configIntent.getStringExtra(EXTRA_NAME),
                configIntent.getStringExtra(EXTRA_SMS_TEXT), configIntent.getStringExtra(EXTRA_KNOWN_FLAG),
                configIntent.getIntExtra(EXTRA_RESPOND_TO, RESPOND_TO_CALLS_AND_TEXTS));
    }

    @Override
    public ReturnValues revertAction(Context context, Intent configIntent) {
        return enableAutoReplyText (context, configIntent, false);
    }

    @Override
    public boolean validateConfig(Intent configIntent) {
        String internalName = configIntent.getStringExtra(EXTRA_INTERNAL_NAME);
        String numbers = configIntent.getStringExtra(EXTRA_NUMBERS);
        String smsText = configIntent.getStringExtra(EXTRA_SMS_TEXT);
        int respondToFlag = configIntent.getIntExtra(EXTRA_RESPOND_TO, -1);
        boolean isValid = internalName != null && numbers != null && smsText != null && respondToFlag != -1;

        if (isValid && !numbers.equals(ALL_CONTACTS) && !numbers.equals(KNOWN_CONTACTS)) {
            //If auto reply has to be sent to specific contacts then
            //config must have names and known flags
            String names = configIntent.getStringExtra(EXTRA_NAME);
            String knownFlags = configIntent.getStringExtra(EXTRA_KNOWN_FLAG);
            isValid = names != null && knownFlags != null;
        }

        return isValid;
    }

    /**
     * Method to enable/disable auto replies to missed calls/texts
     *
     * @param context Application context
     * @param intent Intent received from SA Core requesting Auto reply text to be enabled/disabled
     * @param enable true to enable auto replies, false to disable them
     * @return ReturnValues Info about the status of the request
     */
    private ReturnValues enableAutoReplyText (Context context, Intent intent, boolean enable) {
        ReturnValues retValues = new ReturnValues();
        retValues.status = true;
        retValues.toFrom = QA_TO_MM;
        String internalName = intent.getStringExtra(EXTRA_INTERNAL_NAME);
        retValues.dbgString = internalName;

        Intent svc = new Intent(context, DatabaseUtilityService.class);
        svc.putExtra(EXTRA_INTENT_ACTION, AUTO_SMS_ACTION_KEY);
        svc.putExtra(EXTRA_REGISTER_RECEIVER, enable);
        svc.putExtras(intent);
        context.startService(svc);

        return retValues;
    }

    /**
     * Method to return a config based on input parameters
     *
     * @param internalName Internal name of the entry
     * @param numbers Numbers for which auto reply is needed
     * @param names Names of the contacts
     * @param message Message to be sent
     * @param knownFlags Whether a contact is known or unknown at the time of rule creation
     * @param respondToFlag Respond to missed call or sms or both
     * @return Config
     */
    public static String getConfig (String internalName, String numbers, String names, String message,
            String knownFlags, int respondToFlag) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        intent.putExtra(EXTRA_INTERNAL_NAME, internalName);
        intent.putExtra(EXTRA_NUMBERS, numbers);
        intent.putExtra(EXTRA_NAME, names);
        intent.putExtra(EXTRA_RESPOND_TO, respondToFlag);
        intent.putExtra(EXTRA_SMS_TEXT, message);
        intent.putExtra(EXTRA_KNOWN_FLAG, knownFlags);
        return intent.toUri(0);
    }

}
