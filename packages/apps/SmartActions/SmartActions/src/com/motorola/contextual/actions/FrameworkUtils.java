/*
 * @(#)FrameworkUtils.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/04/16  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import java.util.List;

import com.motorola.contextual.pickers.actions.BrightnessActivity;
import com.motorola.contextual.pickers.actions.CellularDataActivity;
import com.motorola.contextual.pickers.actions.BackgroundDataActivity;
import com.motorola.contextual.pickers.actions.ProcessorSpeedActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

/**
 * Class containing utility methods for interacting with Smart Actions FW
 *
 * <code><pre>
 * CLASS:
 *     Implements Constants
 *
 * RESPONSIBILITIES:
 *    Contains utility methods related to Smart Actions FW
 *
 * COLABORATORS:
 *     Smart Actions Framework
 *     Smart Actions Core
 *
 * USAGE:
 *      See individual methods.
 *
 * </pre></code>
 **/

public class FrameworkUtils implements Constants {

    private static final String TAG = TAG_PREFIX + FrameworkUtils.class.getSimpleName();

    public static final String BRIGHTNESS_ACTION_KEY = "com.motorola.contextual.actions.Brightness";
    public static final String CELLULAR_DATA_ACTION_KEY = "com.motorola.contextual.actions.CellularData";
    public static final String BACKGROUND_SYNC_ACTION_KEY = "com.motorola.contextual.actions.Sync";

    public static final int REQ_TYPE_FIRE = 0;
    public static final int REQ_TYPE_REVERT = 1;

    public static final String SMART_ACTIONS_FRAMEWORK = "com.motorola.contextual.fw";

    /**
     * Method to check if the Smart Actions framework apk is old (HSS6 or older).
     * Intent must be converted to old format. This happens if the action publisher
     * is present in Smart Actions FW but the FW apk hasn't been updated
     *
     * @param context Caller's context
     * @return true if incoming intents for this action need to be converted to old format, false otherwise
     */
    public static boolean isFrameworkOld (Context context) {
        PackageManager pm = context.getPackageManager();
        final Intent intent = new Intent(SETTING_CHANGE_ACTION);
        intent.setPackage(SMART_ACTIONS_FRAMEWORK);
        final List<ResolveInfo> receivers = pm.queryBroadcastReceivers(intent, 0);
        int count = (receivers == null) ? 0 : receivers.size();
        if (LOG_DEBUG) {
            for (int i=0; i<count; i++)
                Log.d(TAG, "Receiver name: " + receivers.get(i).activityInfo.name);
        }
        return (count > 0) ? true : false;
    }

    /**
     * Method to handle incoming intent. This method converts the incoming intent
     * to old format compatible with old Smart Actions FW and broadcasts it
     * In case old FW can't handle the request (like refresh/list) this method executes the command
     *
     * @param context Caller's context
     * @param intent Intent with details of the action to be executed
     */
    public static void handleIntent (Context context, Intent intent) {
        String actionKey = intent.getAction();
        String eventType = intent.getStringExtra(EXTRA_EVENT_TYPE);

        if (actionKey != null && eventType != null) {

            Intent responseIntent = null;
            Intent frameworkIntent = null;

            Action action = ActionHelper.getAction(context, actionKey);

            if (eventType.equalsIgnoreCase(COMMAND_FIRE)) {
                frameworkIntent = getFrameworkCompatibleIntent(intent.getStringExtra(EXTRA_CONFIG),
                                 intent.getBooleanExtra(EXTRA_SAVE_DEFAULT, false), false, REQ_TYPE_FIRE,
                                 actionKey, intent.getStringExtra(EXTRA_REQUEST_ID));

            } else if (eventType.equalsIgnoreCase(COMMAND_REVERT)) {
                frameworkIntent = getFrameworkCompatibleIntent(Persistence.retrieveValue(context, actionKey + DEFAULT_FW_SUFFIX),
                                 false, true, REQ_TYPE_REVERT,
                                 actionKey, intent.getStringExtra(EXTRA_REQUEST_ID));

            } else if (eventType.equalsIgnoreCase(COMMAND_REFRESH)) {
                if (action != null) {
                    responseIntent = action.handleRefresh(context, intent);
                }

            } else if (eventType.equalsIgnoreCase(COMMAND_LIST)) {
                if (action != null) {
                    responseIntent = action.handleList(context, intent);
                }
            }

            if (responseIntent != null) {
                if (LOG_INFO) {
                    Log.i(TAG, "Sending response: " + responseIntent.toUri(0));
                }
                context.sendBroadcast(responseIntent, PERM_ACTION_PUBLISHER_ADMIN);
            }

            if (frameworkIntent != null) {
                if (LOG_INFO) {
                    Log.i(TAG, "Sending intent to framework: " + frameworkIntent.toUri(0));
                }
                context.sendBroadcast(frameworkIntent);
            }
        } else {
            Log.w(TAG, "Action not recognized for command = " + eventType);
        }
    }

    /**
     * Method to get an intent compatible with old Smart Actions FW
     * This intent can be broadcasted to execute the action
     *
     * @param config New configuration of the action publisher
     * @param saveDefault true if the action becomes active for the first time
     * @param restoreDefault true if there is no active rule containing this action
     * @param reqType Fire or revert
     * @param actionKey Action key of the action to be executed
     * @param requestId Request ID sent by SA Core
     * @return Intent which will cause the action to be executed with the old framework
     */
    public static Intent getFrameworkCompatibleIntent (String config, boolean saveDefault, boolean restoreDefault,
            int reqType, String actionKey, String requestId) {
        Intent frameworkIntent = new Intent(SETTING_CHANGE_ACTION);

        frameworkIntent.putExtra(EXTRA_OLD_SAVE_DEFAULT, saveDefault);
        frameworkIntent.putExtra(EXTRA_RESTORE_DEFAULT, restoreDefault);

        Intent configIntent = ActionHelper.getConfigIntent(config);
        if (configIntent != null) {
            frameworkIntent.putExtras(configIntent);
        }

        frameworkIntent.putExtra(EXTRA_REQ_TYPE, reqType);
        frameworkIntent.putExtra(EXTRA_ACTION_KEY, actionKey);
        frameworkIntent.putExtra(RULEKEY, requestId);
        return frameworkIntent;
    }

    /** Updates the state of the passed in components.
     *
     * @param pm
     * @param components
     * @param state
     */
    private static void updateComponentStates(PackageManager pm, ComponentName [] components, int state) {
        for (ComponentName componentName : components) {
            pm.setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP);
        }
    }

    /**
     * Method to check if Smart Actions FW apk is present on the device or not
     * and disable FW activities in case FW is not found
     *
     * @param context Caller's context
     */
    public static void checkAndDisableFramework (Context context) {
        PackageManager pm = context.getPackageManager();
        ComponentName [] frameworkComponents = {new ComponentName(context, BrightnessActivity.class),
                new ComponentName(context, CellularDataActivity.class),
                new ComponentName(context, BackgroundDataActivity.class)};
        ComponentName processorSpeedComponent = new ComponentName(context, ProcessorSpeedActivity.class);

        try {
            pm.getPackageInfo(SMART_ACTIONS_FRAMEWORK, PackageManager.GET_ACTIVITIES);
            updateComponentStates(pm, frameworkComponents, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

            if (isFrameworkOld(context)) {
                //FW apk is old. Disable processor speed.
                pm.setComponentEnabledSetting(processorSpeedComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            } else {
                pm.setComponentEnabledSetting(processorSpeedComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            }

        } catch (NameNotFoundException e) {
            Log.w(TAG, "Smart Actions framework not present on the device");
            updateComponentStates(pm, frameworkComponents, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            pm.setComponentEnabledSetting(processorSpeedComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

}
