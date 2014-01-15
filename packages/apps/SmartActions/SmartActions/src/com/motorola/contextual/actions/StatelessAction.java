/*
 * @(#)StatelessAction.java
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

import java.util.ArrayList;
import java.util.List;

import com.motorola.contextual.debug.DebugTable;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * The abstract class for a stateless action
 * <code><pre>
 * CLASS:
 *     This class should be extended by all Stateless actions
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

public abstract class StatelessAction extends Action implements Constants {

    public static final String TAG = TAG_PREFIX + StatelessAction.class.getSimpleName();

    @Override
    public int getType() {
        return Constants.TYPE_STATELESS_ACTION;
    }

    @Override
    public Intent handleFire(Context context, Intent intent) {
        String actionKey = intent.getAction();
        String config = intent.getStringExtra(EXTRA_CONFIG);
        String debugReqResp = Utils.generateReqRespKey();
        String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
        String status = FAILURE;
        String exceptionString = null;

        if (config != null) {
            Intent configIntent = ActionHelper.getConfigIntent(config);

            if (configIntent != null) {
                String configAction = configIntent.getAction();
                if ((configAction != null && configAction.equals(STATELESS_ACTION)) ||
                        isConfigValid(configIntent)) {
                    //Either GB/HSS6 config or config with version

                    Utils.writeToDebugViewer(context, requestId,
                            DebugTable.Direction.INTERNAL, MM_TO_QA, "", debugReqResp, "",
                            actionKey);

                    if (isResponseAsync()) {
                        //Action takes time to respond to fire
                        //It needs debug parameters which will be written to the Debug service when action executes
                        configIntent.setAction(actionKey);
                        configIntent.putExtra(EXTRA_RESPONSE_ID, requestId);
                        configIntent.putExtra(EXTRA_DEBUG_REQRESP, debugReqResp);
                    }

                    ReturnValues retValues = fireAction(context, configIntent);

                    status = (retValues.status) ? SUCCESS : FAILURE;
                    exceptionString = retValues.exceptionString;
                    
                    Utils.writeToDebugViewer(context, requestId,
                    		DebugTable.Direction.INTERNAL, retValues.toFrom,
                    		retValues.dbgString, debugReqResp,
                    		status, actionKey);
                    

                    if (isResponseAsync() && retValues.status) {
                        //Action is going to send a response at a later time after it completes execution
                        return null;
                    }

                } else {
                    Log.e(TAG, "Invalid config");
                }
            } else {
                Log.e(TAG, "Error in retrieving Intent from config.");
            }
        } else {
            Log.e(TAG, "Error. Null config.");
        }

        return ActionHelper.getResponseIntent(context, actionKey,
                status, requestId, exceptionString, EXTRA_FIRE_RESPONSE);
    }

    @Override
    public Intent handleRevert(Context context, Intent intent) {
        String config = intent.getStringExtra(EXTRA_CONFIG);
        if (config != null) {
            Intent configIntent = ActionHelper.getConfigIntent(config);
            if (configIntent != null) {
                String configAction = configIntent.getAction();
                if ((configAction != null && configAction.equals(STATELESS_ACTION)) ||
                        configIntent.hasExtra(EXTRA_CONFIG_VERSION)) {
                    revertAction(context, configIntent);
                } else {
                    Log.e(TAG, "Config version missing");
                }
            }
        }
        return null;
    }

    @Override
    public Intent handleRefresh(Context context, Intent intent) {
        String config = intent.getStringExtra(EXTRA_CONFIG);
        String actionKey = intent.getAction();
        String responseId = intent.getStringExtra(EXTRA_REQUEST_ID);
        String description = null;
        String status = FAILURE;
        Intent configIntent = ActionHelper.getConfigIntent(config);

        if (configIntent != null) {
            String configAction = configIntent.getAction();
            if (configAction != null && configAction.equals(STATELESS_ACTION)) {
                //Config intent contains old fire uri. Convert it to latest version.
                config = getUpdatedConfig(context, configIntent);
                configIntent = ActionHelper.getConfigIntent(config);
            }

            //Config must have version
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

    @Override
    public List<String> getConfigList(Context context) {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getDescriptionList(Context context) {
        return new ArrayList<String>();
    }

    /**
     * Method to fire a stateless action
     *
     * @param context Caller's context
     * @param configIntent Intent having details of the action to be fired
     * @return Status to the request and error message if any
     */
    public abstract ReturnValues fireAction(Context context, Intent configIntent);

    /**
     * Method to revert a stateless action.
     * This method should be overridden in special cases when a stateless action
     * needs to perform a task at both the beginning and end of the rule
     *
     * @param context Caller's context
     * @param intent Intent containing details about action to be reverted.
     * @return Status to the request and error message if any
     */
    public ReturnValues revertAction(Context context, Intent intent) {
        return null;
    }

    /** used to get status and error string (if applicable) from actions */
    public static class ReturnValues {
        boolean status;
        String dbgString;
        String toFrom;
        String exceptionString;
    };

}
