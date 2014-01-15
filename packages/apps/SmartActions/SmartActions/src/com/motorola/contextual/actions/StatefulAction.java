/*
 * @(#)StatefulAction.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
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

import com.motorola.contextual.debug.DebugTable;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * The abstract class for a stateful action
 * <code><pre>
 * CLASS:
 *     This class should be extended by all Stateful actions
 *
 *
 * RESPONSIBILITIES:
 *    See individual methods.
 *
 * COLABORATORS:
 *     None.
 *
 * USAGE:
 *      See individual methods.
 *
 * </pre></code>
 **/

public abstract class StatefulAction extends Action implements Constants {

    private static final String TAG = TAG_PREFIX + StatefulAction.class.getSimpleName();

    /**
     * This method sets the state for that setting given the configuration
     *
     * @param context - caller's context
     * @param configIntent - the config intent created during configuration
     * This intent also has saveDefault/restoreDefault info
     * @return whether the state setting succeeded or not
     */
    abstract boolean setState(Context context, Intent configIntent);

    /**
     * Returns the current state set as part of setState
     *
     * @param context - caller's context
     * @return the state as a String
     */
    String getState(Context context) {
        return null;
    }

    /** Handles setting changes received from the Framework
     *
     * @param context - caller's context
     * @param obj - an intent for settings which have a broadcast
     *            - setting name as a string for content observer based settings
     * @return
     */
    abstract Status handleSettingChange(Context context, Object obj);

    /**
     * Returns a user readable representation of the state that was currently set with the
     * information from the action Intent.
     *
     * @param context - caller's context
     * @return String to be displayed to the user
     */
    abstract String getSettingString(Context context);

    /** Returns the default setting as a string
    *
    * @param context - caller's context
    * @return the default config intent
    */
    abstract String getDefaultSetting(Context context);

    /** Converts the old version of default config Intent to new config
    *
    * @param context - caller's context
    * @param defaultIntent Intent containing old action's default info
    * @return the default config intent
    */
    abstract String getDefaultSetting(Context context, Intent defaultIntent);

    /** This function registers for setting changes
     * @param context - caller's context
     */
    void registerForSettingChanges(Context context) {
        return;
    }

    /** This function deregisters from setting changes
     * @param context - caller's context
     */
    void deregisterFromSettingChanges(Context context) {
        return;
    }


    /** This function returns the setting name(s) to be observed.
     * For settings which have intent broadcasts from framework, this
     * would return null.
     *
     * @return
     */
    String[] getSettingToObserve() {
        return null;
    }

    /** Returns the Uri to be passed to Content Observer
     *
     * @param setting - setting name
     * @return
     */
    Uri getUriForSetting(String setting) {
        return null;
    }

    /** Returns the actionKey
     *
     * @return
     */
    abstract String getActionKey();

    /** Returns the action Intent that is broadcast by framework when
     * this action state changes. If there is no broadcast then null is
     * returned.
     *
     * @return
     */
    String getBroadcastAction() {
        return null;
    }

    /**
     * This method shall be overridden by action publishers to indicate whether
     * a newer version of config is available or not
     *
     * @param context
     *            - application's context
     * @param configIntent
     *            - the config intent
     * @return - true if newer version is available, false otherwise
     */
    public boolean isConfigUpdated(Context context, Intent configIntent) {
        return false;
    }

    @Override
    public int getType() {
        return TYPE_STATEFUL_ACTION;
    }

    @Override
    public final Intent handleFire(Context context, Intent intent) {
        String actionKey = intent.getAction();
        boolean saveDefault = intent.getBooleanExtra(EXTRA_SAVE_DEFAULT, false);
        String config = intent.getStringExtra(EXTRA_CONFIG);
        String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
        Intent configIntent = ActionHelper.getConfigIntent(config);
        String status = FAILURE;

        if (configIntent != null) {
            String configAction = configIntent.getAction();
            if ((configAction != null && configAction.equals(SETTING_CHANGE_ACTION)) ||
                    isConfigValid(configIntent)) {
                //Either GB/HSS6 config or config with version

                // If the request was save default, then we need to start
                // monitoring
                // this setting for any changes made by external entities
                if (saveDefault) {
                    Persistence.commitValue(context,
                            actionKey + MONITOR_SUFFIX, "true");
                    registerForSettingChanges(context);
                }

                status = setState(context, intent, config) ? SUCCESS : FAILURE;

                if (isResponseAsync() && status.equals(SUCCESS)) {
                    //Action is going to send a response at a later time after it completes execution
                    return null;
                }

            } else {
                Log.e(TAG, "Config invalid");
            }
        } else {
            Log.e(TAG, "Config not in correct format");
        }

        return ActionHelper.getResponseIntent(context, actionKey, status,
                requestId, null, EXTRA_FIRE_RESPONSE);
    }

    @Override
    public final Intent handleRevert(Context context, Intent intent) {
        String actionKey = intent.getAction();
        String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);

        // Smart Rules is no longer interested in this setting, stop
        // monitoring
        Persistence
        	.removeValue(context, actionKey + MONITOR_SUFFIX);
        deregisterFromSettingChanges(context);

        String config = Persistence.retrieveValue(context, getActionKey() + DEFAULT_SUFFIX);
        if (LOG_INFO) Log.i(TAG, "Reverting " + actionKey + " state to " + config);
        boolean status = setState(context, intent, config);

        if (isResponseAsync() && status) {
            //Action is going to send a response at a later time after it completes execution
            return null;
        }

        return ActionHelper.getResponseIntent(context, actionKey,
                                              (status) ? SUCCESS : FAILURE, requestId, null, EXTRA_REVERT_RESPONSE);
    }

    @Override
    public final Intent handleRefresh(Context context, Intent intent) {
        String config = intent.getStringExtra(EXTRA_CONFIG);
        String actionKey = intent.getAction();
        String responseId = intent.getStringExtra(EXTRA_REQUEST_ID);
        String description = null;
        String status = FAILURE;
        Intent configIntent = ActionHelper.getConfigIntent(config);

        if (configIntent != null) {
            String configAction = configIntent.getAction();
            if ((configAction != null && configAction
                    .equals(SETTING_CHANGE_ACTION))
                    || isConfigUpdated(context, configIntent)) {
                //Config intent contains old fire uri. Convert it to latest version.
                config = getUpdatedConfig(context, configIntent);
                configIntent = ActionHelper.getConfigIntent(config);

                //The default uri for old rules needs to be converted to new config
                //It should be saved to the persistence for handling revert.
                String defaultUri = intent.getStringExtra(EXTRA_DEFAULT_URI);
                Intent defaultIntent = ActionHelper.getConfigIntent(defaultUri);
                if (defaultIntent != null) {
                    String defaultConfig = getDefaultSetting(context, defaultIntent);
                    if (defaultConfig != null) {
                        Persistence.commitValue(context, getActionKey() + DEFAULT_SUFFIX, defaultConfig);
                    }
                }
            }

            //Validate config
            if (configIntent != null && isConfigValid(configIntent)) {
                description = getDescription(context, configIntent);
                status = SUCCESS;
            } else {
                Log.e(TAG, "Invalid config");
            }
        }

        return ActionHelper.getRefreshResponse(context, actionKey,
                                               responseId, status, config, description);
    }

    /**
     * Method to perform common operations while setting a state for a stateful action
     *
     * @param context Caller's context
     * @param intent Intent containing details about the command
     * @param config Config to be set
     * @return Whether state change was successful or not
     */
    private boolean setState (Context context, Intent intent, String config) {
        String actionKey = intent.getAction();
        String eventType = intent.getStringExtra(EXTRA_EVENT_TYPE);
        String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
        String debugReqResp = Utils.generateReqRespKey();
        boolean saveDefault = intent.getBooleanExtra(EXTRA_SAVE_DEFAULT, false);
        boolean restoreDefault = (eventType != null) ? eventType.equals(COMMAND_REVERT) : false;

        Utils.writeToDebugViewer(context, requestId, DebugTable.Direction.INTERNAL,
                                 MM_TO_QA, eventType, debugReqResp, "", actionKey);

        Intent configIntent = ActionHelper.getConfigIntent(config);
        boolean status = false;
        if (configIntent != null) {
            configIntent.putExtra(EXTRA_SAVE_DEFAULT, saveDefault);
            configIntent.putExtra(EXTRA_RESTORE_DEFAULT, restoreDefault);

            if (isResponseAsync()) {
                //Action takes time to respond to set the state
                //It needs debug parameters which will be written to the Debug service when action executes
                configIntent.putExtra(EXTRA_RESPONSE_ID, requestId);
                configIntent.putExtra(EXTRA_DEBUG_REQRESP, debugReqResp);
            }

            status = setState(context, configIntent);
            if (saveDefault) {
                //Save default setting value
                Persistence.commitValue(context, getActionKey() + DEFAULT_SUFFIX, getDefaultSetting(context));
                if (LOG_DEBUG) Log.d(TAG, "Saved default setting");
            }
        }

        // We can send the action status right away
        Utils.writeToDebugViewer(context, requestId,
                                 DebugTable.Direction.INTERNAL, QA_TO_MM,
                                 getState(context), debugReqResp, (status) ? SUCCESS
                                 : FAILURE, actionKey);

        return status;
    }

    enum Status {
        SUCCESS,
        FAILURE,
        TRANSIENT,
        NO_CHANGE
    }

}
