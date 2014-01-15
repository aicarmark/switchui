/*
 * @(#)FrameworkAction.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/04/23  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * The abstract class for an action present in Smart Actions FW
 * This class is required when Smart Actions FW is old.
 * It has the methods to handle features which were not supported by old architecture (refresh and list)
 *
 * <code><pre>
 * CLASS:
 *     This class should be extended by all Framework actions
 *
 *
 * RESPONSIBILITIES:
 *    The methods of this class should be called only if Smart Actions FW is old.
 *    See individual methods for further details
 *
 * COLABORATORS:
 *     None.
 *
 * USAGE:
 *      See individual methods.
 *
 * </pre></code>
 **/

public abstract class FrameworkAction extends Action {

    private static final String TAG = TAG_PREFIX + FrameworkAction.class.getSimpleName();

    @Override
    public Intent handleFire(Context context, Intent intent) {
        return null;
    }

    @Override
    public Intent handleRevert(Context context, Intent intent) {
        return null;
    }

    @Override
    public Intent handleRefresh(Context context, Intent intent) {
        String config = intent.getStringExtra(EXTRA_CONFIG);
        String actionKey = intent.getAction();
        String description = null;
        String status = FAILURE;
        Intent configIntent = ActionHelper.getConfigIntent(config);

        if (configIntent != null) {
            String configAction = configIntent.getAction();
            //Note: Currently only stateful actions are a part of Smart Actions FW.
            //In future if Stateless actions are added to it then
            //we may have to create two classes (for stateful and stateless FW actions)
            if (configAction != null && (configAction.equals(SETTING_CHANGE_ACTION))) {
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
                        Persistence.commitValue(context, actionKey + DEFAULT_FW_SUFFIX, defaultConfig);
                    }
                }
            } else {
                //Only get the updated config
                config = getUpdatedConfig(context, configIntent);
                configIntent = ActionHelper.getConfigIntent(config);
            }

            //Validate config
            if (configIntent != null && isConfigValid(configIntent)) {
                description = getDescription(context, configIntent);
                status = SUCCESS;
            } else {
                Log.e(TAG, "Invalid config");
            }
        }

        Intent responseIntent = ActionHelper.getRefreshResponse(context, actionKey,
                                intent.getStringExtra(EXTRA_REQUEST_ID), status, config, description);
        return responseIntent;
    }

    @Override
    public abstract String getActionString(Context context);

    @Override
    public abstract String getDescription(Context context, Intent configIntent);

    @Override
    public abstract String getUpdatedConfig(Context context, Intent configIntent);

    @Override
    public abstract List<String> getConfigList(Context context);

    @Override
    public abstract List<String> getDescriptionList(Context context);

    /** Converts the old version of default config Intent to new config
     *
     * @param context - caller's context
     * @param defaultIntent Intent containing old action's default info
     * @return the default config intent
     */
    abstract String getDefaultSetting(Context context, Intent defaultIntent);

}
