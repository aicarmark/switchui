/*
 * @(#)ActionHelper.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/03/20  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import com.motorola.contextual.debug.DebugTable;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.util.Xml;

/**
 * Helper class for actions
 *
 * <code><pre>
 * CLASS:
 *     Implements Constants
 *
 * RESPONSIBILITIES:
 *    See individual methods.
 *
 * COLABORATORS:
 *     All actions
 *
 * USAGE:
 *      See individual methods.
 *
 * </pre></code>
 **/

public class ActionHelper implements Constants {

    private static final String TAG = TAG_PREFIX + ActionHelper.class.getSimpleName();

    /**
     * Enum of rule trigger times.
     */
    public enum RuleTriggerTime {
        START, END, START_AND_END;
    }

    /**
     * Constants for Smart Actions
     */
    public interface Params {
        // Extras related to messaging
        public static final String EXTRA_NAME = "name";

        // Auto SMS-related
        String EXTRA_INTERNAL_NAME = "internal_name";
        String EXTRA_PHONE_NUMBERS = "numbers";
        String EXTRA_RESPOND_TO = "respond_to_flag";
        String EXTRA_SMS_TEXT = "sms_text";
        String ALL_CONTACTS = "*all*";
        String KNOWN_CONTACTS = "*known*";

        //VIP Caller related
        public static final String EXTRA_KNOWN_FLAG = "KNOWN_FLAG";
    }

    /**
     * Method to get the response intent for fire/revert command
     *
     * @param context Caller's context
     * @param actionKey Action key of the action
     * @param status Status of the command
     * @param responseId Response ID
     * @param failureDescription Error description if the command failed
     * @param responseEventType Type of response. Fire or revert
     *
     * @return Fire/revert response Intent
     */
    public static Intent getResponseIntent (Context context, String actionKey,
                                            String status, String responseId, String failureDescription, String responseEventType) {
        if (responseEventType == null) {
            return null;
        }
        Intent statusIntent = new Intent(ACTION_PUBLISHER_EVENT);
        statusIntent.putExtra(EXTRA_EVENT_TYPE, responseEventType);
        statusIntent.putExtra(EXTRA_PUBLISHER_KEY, actionKey);
        statusIntent.putExtra(EXTRA_STATUS, status);
        statusIntent.putExtra(EXTRA_RESPONSE_ID, responseId);
        if (status.equals(FAILURE) && failureDescription != null) {
            statusIntent.putExtra(EXTRA_FAILURE_DESCRIPTION, failureDescription);
        }
        return statusIntent;
    }

    /**
     * Method to get the response intent for list command
     *
     * @param context Caller's context
     * @param configs List of configurations supported by the actions
     * @param descriptions List of descriptions of all configurations
     * @param actionKey Action key of the action
     * @param newStateTitle Title for the button that launches the
     * configuration activity in new state mode
     *
     * @return List response intent
     */
    public static Intent getListResponse(Context context, List<String> configs,
                                         List<String> descriptions, String actionKey, String newStateTitle,
                                         String responseId) {
        if (configs != null && descriptions != null) {
            int size = configs.size();
            XmlSerializer serializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            try {
                serializer.setOutput(writer);
                serializer.startDocument("utf-8", null);
                serializer.startTag(null, TAG_CONFIG_ITEMS);
                for (int index = 0; index < size; index++) {
                    serializer.startTag(null, TAG_ITEM);
                    serializer.startTag(null, TAG_CONFIG);
                    serializer.text(configs.get(index));
                    serializer.endTag(null, TAG_CONFIG);
                    serializer.startTag(null, TAG_DESCRIPTION);
                    serializer.text(descriptions.get(index));
                    serializer.endTag(null, TAG_DESCRIPTION);
                    serializer.endTag(null, TAG_ITEM);
                }
                serializer.endTag(null, TAG_CONFIG_ITEMS);
                serializer.endDocument();

                Intent listIntent = new Intent(ACTION_PUBLISHER_EVENT);
                listIntent.putExtra(EXTRA_EVENT_TYPE, EXTRA_LIST_RESPONSE);
                listIntent.putExtra(EXTRA_RESPONSE_ID, responseId);
                listIntent.putExtra(EXTRA_PUBLISHER_KEY, actionKey);
                listIntent.putExtra(EXTRA_CONFIG_ITEMS, writer.toString());
                listIntent.putExtra(EXTRA_NEW_STATE_TITLE, newStateTitle);
                return listIntent;
            } catch (Exception exp) {
                exp.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * Method to get the response intent for refresh command
     *
     * @param context Caller's context
     * @param actionKey Action key of the action
     * @param responseId Response ID
     * @param status Status of the command
     * @param config Configuration to be refreshed
     * @param description Description of the configuration
     *
     * @return Refresh response intent
     */
    public static Intent getRefreshResponse(Context context, String actionKey,
                                            String responseId, String status, String config, String description) {
        Intent refreshIntent = new Intent(ACTION_PUBLISHER_EVENT);
        refreshIntent.putExtra(EXTRA_EVENT_TYPE, EXTRA_REFRESH_RESPONSE);
        refreshIntent.putExtra(EXTRA_PUBLISHER_KEY, actionKey);
        refreshIntent.putExtra(EXTRA_RESPONSE_ID, responseId);
        refreshIntent.putExtra(EXTRA_STATUS, status);
        refreshIntent.putExtra(EXTRA_CONFIG, config);
        refreshIntent.putExtra(EXTRA_DESCRIPTION, description);
        
        Intent configIntent = ActionHelper.getConfigIntent(config);
        if (configIntent != null) {
            refreshIntent.putExtra(EXTRA_RULE_ENDS, configIntent.getBooleanExtra(EXTRA_RULE_ENDS, false));
        }
        return refreshIntent;
    }

    /**
     * Returns the Action object corresponding to an action key
     *
     * @param pubKey Publisher key of the action to be returned
     * @return Action object corresponding to the action key
     */
    public static Action getAction (Context context, String pubKey) {
        Action action = null;
        if (pubKey != null && pubKey.startsWith(PACKAGE)) {
            action = instantiate(context, pubKey);
        } else {
        	Log.w(TAG, "Null or Invalid Action Key " + pubKey+ " prefix must be (PKG):"+PACKAGE);
        }
        return action;
    }

    /**
     * Instantiates the class. Expects the caller to perform
     * sanity checks on the validity of the action key.
     *
     * @param pubKey Key of the action to be instantiated
     * @return Instantiated action
     */
    public static Action instantiate(Context context, String pubKey) {
        Class<?> cls = null;
        Action action = null;
        String actionKey = getActionKeyForPubKey(context, pubKey);
        if(actionKey == null) {
            actionKey = pubKey;
        }
        try {
            // actionKey is infact the fully qualified class name
            cls = Class.forName(actionKey);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Class not found " + actionKey);
            return null;
        }
        try {
            action = (Action) cls.newInstance();
        } catch (IllegalAccessException e) {
            Log.e(TAG, "class instantiation failed: default ctor not visible "
                  + actionKey);

        } catch (InstantiationException e) {
            Log.e(TAG,
                  "class instantiation failed: instance cannot be created "
                  + actionKey);
        }
        return action;
    }

    /**
     * Returns the action key for the action specified by the publisher key
     *
     * @param context
     * @return
     */
    public static String getActionKeyForPubKey(Context context, String pubKey) {
        if (LOG_DEBUG) Log.d(TAG, "getActionKeyForPubKey for: " + pubKey + "APP_PACKAGE :" +APP_PACKAGE);

        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pInfo = pm.getPackageInfo(APP_PACKAGE, PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);

            for (ActivityInfo aInfo : pInfo.activities) {
                if (aInfo.metaData != null) {
                    String publisherKey = aInfo.metaData.getString(PUBLISHER_KEY);
                    String actionKey = aInfo.metaData.getString(ACTION_KEY);
                    if (LOG_DEBUG) Log.d(TAG, "getActionKeyForPubKey publisherKey " + publisherKey);
                    if (LOG_DEBUG) Log.d(TAG, "getActionKeyForPubKey actionKey " + actionKey);
                    if (publisherKey != null && publisherKey.equals(pubKey))
                        return actionKey;
                } else {
                    // this is not an error on single apk since in addition to action activities there are
                    // Smart profile and Smart Rules Activities
                }
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "package not found ? this code wouldn't execute if that was the case");
        }
        return null;
    }

    /**
     * Returns the intent present in a config string
     *
     * @param config Input configuration
     * @return Extracted intent from the config
     */
    public static Intent getConfigIntent (String config) {
        Intent configIntent = null;
        if (config != null) {
            try {
                // Convert the config to an intent to get at the fields
                configIntent = Intent.parseUri(config, 0);
            } catch (URISyntaxException e) {
                Log.w(TAG, "Exception when retrieving from config");
            }
        }
        return configIntent;
    }

    /**
     * Sends the Action status for an action.
     * This is needed for those actions which send the response of command after a delay
     *
     * @param context Caller's context
     * @param statusIntent Intent to be broadcasted
     * @param status Status of the request
     * @param exceptionString Error string if any
     */
    public static void sendActionStatus(Context context, Intent statusIntent, boolean status, String exceptionString) {
        if (statusIntent != null) {
            String strStatus = (status) ? SUCCESS : FAILURE;
            String debugReqResp = statusIntent.getStringExtra(EXTRA_DEBUG_REQRESP);

            statusIntent.putExtra(EXTRA_STATUS, strStatus);
            statusIntent.removeExtra(EXTRA_DEBUG_REQRESP);
            if (strStatus.equals(FAILURE) && exceptionString != null) {
                statusIntent.putExtra(EXTRA_FAILURE_DESCRIPTION, exceptionString);
            }
            if (LOG_INFO) {
                Log.i(TAG, "Sending response intent: " + statusIntent.toUri(0));
            }

		context.sendBroadcast(statusIntent, PERM_ACTION_PUBLISHER_ADMIN);


            Utils.writeToDebugViewer(context, statusIntent.getStringExtra(EXTRA_RESPONSE_ID), DebugTable.Direction.INTERNAL, QA_TO_MM,
                                      strStatus, debugReqResp, strStatus, statusIntent.getStringExtra(EXTRA_PUBLISHER_KEY));
        }

    }

}
