/*
 * @(#)SendMessage.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/03/27  NA                  Initial version
 *
 */
package com.motorola.contextual.actions;

import com.motorola.contextual.commonutils.StringUtils;
import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class represents Send text message action
 *
 * <code><pre>
 * CLASS:
 *     Extends StatelessAction
 *
 * RESPONSIBILITIES:
 *    This class implements the methods needed to respond to fire, refresh
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

public class SendMessage extends StatelessAction implements Constants {

    public static final String TAG = TAG_PREFIX + SendMessage.class.getSimpleName();

    @Override
    public ReturnValues fireAction(Context context, Intent configIntent) {
        ReturnValues retValues = new ReturnValues();
        retValues.status = true;
        String numbers = configIntent.getStringExtra(EXTRA_NUMBER);
        retValues.dbgString = numbers;

        if (LOG_INFO) Log.i(TAG, "Sending Message to Number " + numbers);

        retValues.toFrom = QA_TO_MM;
        try {
            if(numbers != null) {
                Intent svc = new Intent(context, DatabaseUtilityService.class);
                svc.putExtra(EXTRA_INTENT_ACTION, configIntent.getAction());
                svc.putExtras(configIntent);

                // compose the status intent and add it to the service intent
                Intent statusIntent = new Intent(ACTION_PUBLISHER_EVENT);
                statusIntent.putExtra(EXTRA_EVENT_TYPE, EXTRA_FIRE_RESPONSE);
                statusIntent.putExtra(EXTRA_PUBLISHER_KEY, SMS_ACTION_KEY);
                statusIntent.putExtra(EXTRA_RESPONSE_ID, configIntent.getStringExtra(EXTRA_RESPONSE_ID));
                statusIntent.putExtra(EXTRA_DEBUG_REQRESP, configIntent.getStringExtra(EXTRA_DEBUG_REQRESP));
                svc.putExtra(EXTRA_STATUS_INTENT, statusIntent);

                context.startService(svc);
                retValues.toFrom = QA_TO_FW;

            } else {
                retValues.status = false;
                retValues.exceptionString = context.getString(R.string.address_not_configured);
            }
        } catch (Exception e) {
            e.printStackTrace();
            retValues.status = false;
            retValues.exceptionString = e.toString();
        }
        return retValues;
    }

    @Override
    public String getActionString(Context context) {
        return context.getString(R.string.send_a_text_message);
    }

    @Override
    public String getDescription(Context context, Intent configIntent) {
        String description = null;
        if (configIntent != null) {
            String message = configIntent.getStringExtra(EXTRA_MESSAGE);
            String names = configIntent.getStringExtra(EXTRA_NAME);
            description = getDescription(context, names, StringUtils.COMMA_STRING, message);
        }
        return description;
    }

    @Override
    public boolean isResponseAsync() {
        return true;
    }

    @Override
    public String getUpdatedConfig(Context context, Intent configIntent) {
        return getConfig(configIntent.getStringExtra(EXTRA_NUMBER),
                configIntent.getStringExtra(EXTRA_NAME), configIntent.getStringExtra(EXTRA_KNOWN_FLAG),
                configIntent.getStringExtra(EXTRA_MESSAGE), configIntent.getBooleanExtra(OLD_EXTRA_RULE_ENDS, false));
    }

    /**
     * Utility method to get user readable description of names and message
     *
     * @param context Caller's context
     * @param names Names of the contacts
     * @param delimiter Delimiter separating the names
     * @param message Message to be sent
     * @return User readable description
     */
    public static String getDescription(Context context, String names, String delimiter, String message) {
        String[] nameArr = names.split(StringUtils.COMMA_STRING);
        StringBuilder descriptionBuilder = new StringBuilder();
        if (nameArr.length > 1) {
            descriptionBuilder.append(nameArr[0]).append(SPACE)
            .append(PLUS).append(SPACE).append(nameArr.length - 1);
        } else {
            descriptionBuilder.append(nameArr[0]);
        }
        descriptionBuilder.append(NEW_LINE).append(DOUBLE_QUOTE)
        .append(message).append(DOUBLE_QUOTE);
        return context.getString(R.string.send_text) + descriptionBuilder.toString();
    }

    @Override
    public boolean validateConfig(Intent configIntent) {
        String numbers = configIntent.getStringExtra(EXTRA_NUMBER);
        String names = configIntent.getStringExtra(EXTRA_NAME);
        String knownFlags = configIntent.getStringExtra(EXTRA_KNOWN_FLAG);
        String message = configIntent.getStringExtra(EXTRA_MESSAGE);
        return numbers != null && names != null && knownFlags != null && message != null;
    }

    /**
     * Method to get config based on supplied parameters
     *
     * @param numbers Numbers to send message
     * @param names Names of contacts
     * @param knownFlags Whether a contact is known or unknown at the time of rule creation
     * @param message Text message
     * @param ruleEndFlag Whether action kicks in at rule end or not
     * @return
     */
    public static String getConfig(String numbers, String names, String knownFlags, String message, boolean ruleEndFlag) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        intent.putExtra(EXTRA_NUMBER, numbers);
        intent.putExtra(EXTRA_NAME, names);
        intent.putExtra(EXTRA_KNOWN_FLAG, knownFlags);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_RULE_ENDS, ruleEndFlag);
        return intent.toUri(0);
    }
}
