/*
 * @(#)Action.java
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

import java.util.List;

import android.content.Context;
import android.content.Intent;

/**
 * The abstract class for actions
 *
 * <code><pre>
 * CLASS:
 *     Implements Constants
 *
 * RESPONSIBILITIES:
 *    This class should be extended by all the actions
 *
 * COLABORATORS:
 *     None
 *
 * USAGE:
 *      See individual methods.
 *
 * </pre></code>
 **/

public abstract class Action implements Constants {

    /**
     * Method to handle fire command
     *
     * @param context Caller's context
     * @param intent Intent containing various parameters to fire the action
     * @return Fire response intent
     */
    public abstract Intent handleFire(Context context, Intent intent);

    /**
     * Method to handle revert command
     *
     * @param context Caller's context
     * @param intent Intent containing various parameters to revert the action
     * @return Revert response intent
     */
    public abstract Intent handleRevert(Context context, Intent intent);

    /**
     * Method to handle refresh command
     *
     * @param context Caller's context
     * @param intent Intent containing various parameters to refresh the action
     * @return Refresh response intent
     */
    public abstract Intent handleRefresh(Context context, Intent intent);

    /**
     * Method to handle list command
     *
     * @param context Caller's context
     * @param intent Intent containing various parameters to get the list of states
     * @return List response intent
     */
    public final Intent handleList(Context context, Intent intent) {
        List<String> configs = getConfigList(context);
        List<String> descriptions = getDescriptionList(context);

        return ActionHelper.getListResponse(context, configs, descriptions,
                                            intent.getAction(), getActionString(context),
                                            intent.getStringExtra(EXTRA_REQUEST_ID));
    }
    
    /**
     * Method to handle passthru commands
     *
     * @param context Caller's context
     * @param intent Intent containing various parameters to get the list of states
     * @return if the intent was handled or not
     */
    public Intent handleChildActionCommands(Context context, Intent intent) {
        
        return null;
    }

    /**
     * Whether the action is fired some time after receiving the command or
     * if there is a possibility to retry if fire fails
     *
     * @return true if the response is async, false otherwise
     */
    public boolean isResponseAsync() {
         return false;
    }

    /**
     * Returns the user readable Action String (e.g. Ringer or Display Brightness)
     *
     * @param context Caller's context
     * @return User readable action name
     */
    public abstract String getActionString(Context context);

    /**
     * Method to get the user readable description of the action
     *
     * @param context Caller's context
     * @param configIntent The configuration for which refresh was sent
     * @return User readable description of the action
     */
    public abstract String getDescription (Context context, Intent configIntent);

    /**
     * Update config to latest version
     *
     * @param configIntent Config intent with old extras
     * @return Updated config
     */
    public abstract String getUpdatedConfig (Context context, Intent configIntent);

    /**
     * Method to get a list of all configurations supported by an action
     *
     * @param context Caller's context
     * @return List of all configurations
     */
    public abstract List<String> getConfigList(Context context);

    /**
     * Method to get a list of descriptions of all configurations supported by an action
     *
     * @param context Caller's context
     * @return List of descriptions of all configurations
     */
    public abstract List<String> getDescriptionList(Context context);

    /**
     * Returns if the config is valid for an action or not.
     * Each action publisher should make sure that config contains all the necessary parameters
     *
     * @param configIntent Config intent
     * @return true if config is valid, false otherwise
     */
    public final boolean isConfigValid (Intent configIntent) {
        return validateConfigVersion(configIntent) && validateConfig(configIntent);
    }

    /**
     * Method to check if the config version is valid or not
     * @param configIntent Config intent
     * @return true if config version is valid, false otherwise
     */
    public boolean validateConfigVersion (Intent configIntent) {
        double configVersion = configIntent.getDoubleExtra(EXTRA_CONFIG_VERSION, -1);
        return configVersion != -1;
    }

    /**
     * Method to check if the config is valid or not
     * Config version need not be validated while implementing this method
     *
     * @param configIntent Config intent
     * @return true if config is valid, false otherwise
     */
    public abstract boolean validateConfig (Intent configIntent);

    /**
     * Method to get the type of action
     * @return Type of action
     */
    public int getType() {
        return TYPE_ACTION;
    }
}
