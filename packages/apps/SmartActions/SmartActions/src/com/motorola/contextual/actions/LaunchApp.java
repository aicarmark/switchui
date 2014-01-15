/*
 * @(#)LaunchApp.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/03/21  NA                  Initial version
 *
 */
package com.motorola.contextual.actions;

import java.net.URISyntaxException;

import com.motorola.contextual.smartrules.R;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import java.util.List;

/**
 * This class represents Launch application action
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

public class LaunchApp extends StatelessAction implements Constants {

    private static final String TAG = TAG_PREFIX + LaunchApp.class.getSimpleName();

    @Override
    public ReturnValues fireAction(Context context, Intent configIntent) {
        ReturnValues retValues = new ReturnValues();
        retValues.toFrom = QA_TO_MM;
        retValues.status = false;

        String actionUri = configIntent.getStringExtra(EXTRA_APP_URI);

        if (actionUri != null && actionUri.length() != 0) {
            // This special case is for suggested rules to launch actions automatically
            // Suggested rules will have the intent translated in a URI directly instead of
            // the two step process for Launch App
            try {
                Intent launchIntent = Intent.parseUri(actionUri, 0);
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                retValues.dbgString = actionUri;
                if (LOG_INFO) Log.i(TAG, "Launch app " + retValues.dbgString);
                context.startActivity(launchIntent);
                retValues.status = true;
            } catch (URISyntaxException e) {
                Log.e(TAG, "actionUri passed is invalid! " + actionUri);
                e.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "actionUri passed cannot be started! " + actionUri);
                e.printStackTrace();
            }
        } else {
            String component = configIntent.getStringExtra(EXTRA_COMPONENT);
            if (component != null) {
                ComponentName componentName = ComponentName.unflattenFromString(component);
                Intent launchIntent = new Intent(Intent.ACTION_MAIN, null);
                launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                launchIntent.setComponent(componentName);

                // Check if the activity exists
                if (context.getPackageManager().resolveActivity(
                            launchIntent, 0) != null) {
                    if (LOG_INFO) Log.i(TAG, "Launching app: " + componentName);
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(launchIntent);
                    retValues.status = true;
                } else {
                    retValues.exceptionString = context
                                            .getString(R.string.error_item_missing);
                    Log.e(TAG, "App to be launched has been uninstalled");
                }
            } else {
                Log.e(TAG, "Component is null");
            }
        }

        return retValues;
    }

    @Override
    public String getActionString(Context context) {
        return context.getString(R.string.launch_an_application);
    }

    @Override
    public String getDescription(Context context, Intent configIntent) {
        String description = null;
        String actionUri = configIntent.getStringExtra(EXTRA_APP_URI);
        if (actionUri != null && actionUri.length() != 0) {

                try {
                    Intent launchIntent = Intent.parseUri(actionUri, 0);
                    List<ResolveInfo> rList = context.getPackageManager().queryIntentActivities(launchIntent, 0);
                    if(rList != null) {
                        for(ResolveInfo ri : rList) {
                            ActivityInfo info = ((ri !=null) ? ri.activityInfo : null);
                            if (info != null) {
                                String tmpLabel = info.loadLabel(context.getPackageManager()).toString();
                                description = (tmpLabel != null) ?  tmpLabel : description;
                                if(LOG_INFO) Log.i(TAG, "description " + description + " : " +  ri.isDefault);
                                if(ri.isDefault) { 
                                    break; 
                                } 
                            }
                        }
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    Log.e(TAG, "launchApplication failed to get activity info for ");
                }

        } else {
            String componentName = configIntent.getStringExtra(EXTRA_COMPONENT);
            if (componentName != null) {
                ComponentName component = ComponentName.unflattenFromString(componentName);
                try {
                    ActivityInfo info = context.getPackageManager()
                                        .getActivityInfo(component, 0);
                    if (info != null) {
                        description = info.loadLabel(
                                          context.getPackageManager()).toString();
                    }
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                    Log.e(TAG, "launchApplication failed to get activity info for " + component);
                }
            } else {
                Log.e(TAG, "Component name is null");
            }
        }
        return description;
    }

    @Override
    public String getUpdatedConfig(Context context, Intent configIntent) {
        return getConfig(configIntent.getStringExtra(EXTRA_APP_URI), configIntent.getStringExtra(EXTRA_COMPONENT));
    }

    @Override
    public boolean validateConfig(Intent configIntent) {
        String actionUri = configIntent.getStringExtra(EXTRA_APP_URI);
        String component = configIntent.getStringExtra(EXTRA_COMPONENT);
        return actionUri != null || component != null;
    }

    /**
     * Method to get the config based on input parameters
     *
     * @param actionUri This is valid only for sample rules
     * @param component Component name of the app to be launched
     * @return Config
     */
    public static String getConfig (String actionUri, String component) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        if (actionUri != null && actionUri.length() != 0) {
            intent.putExtra(EXTRA_APP_URI, actionUri);
        } else {
            intent.putExtra(EXTRA_COMPONENT, component);
        }
        return intent.toUri(0);
    }

}
