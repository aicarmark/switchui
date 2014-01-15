/*
 * @(#)WebsiteLaunch.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/04/02  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * This class represents Launch website action
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

public class WebsiteLaunch extends StatelessAction implements Constants {

    private static final String TAG = TAG_PREFIX + WebsiteLaunch.class.getSimpleName();
    private static final String HTTP_PREFIX = "http://";
    private static final String HTTPS_PREFIX = "https://";

    @Override
    public ReturnValues fireAction(Context context, Intent configIntent) {
        ReturnValues retValues = new ReturnValues();
        retValues.status = true;
        String url = configIntent.getStringExtra(EXTRA_URL);
        if (LOG_INFO) Log.i(TAG, "Website url is " + url);
        retValues.dbgString = url;
        retValues.toFrom = QA_TO_MM;

        // If the url does not start with http:// or https://, try
        // launching the url with appending http:// first.
        // If this also fails, try launching the url with appending https://
        if(url != null && !url.startsWith(HTTP_PREFIX) && !url.startsWith(HTTPS_PREFIX)) {
            StringBuilder str = new StringBuilder();

            str.append(HTTP_PREFIX)
            .append(url);
            url = str.toString();
        }

        launchBrowserIntent(context, url, retValues);
        return retValues;
    }

    @Override
    public String getActionString(Context context) {
        return context.getString(R.string.open_website);
    }

    @Override
    public String getDescription(Context context, Intent configIntent) {
        return (configIntent != null) ? configIntent.getStringExtra(EXTRA_URL) : null;
    }

    @Override
    public String getUpdatedConfig(Context context, Intent configIntent) {
        return getConfig(configIntent.getStringExtra(EXTRA_URL),
                configIntent.getBooleanExtra(OLD_EXTRA_RULE_ENDS, false));
    }

    @Override
    public boolean validateConfig(Intent configIntent) {
        return configIntent.getStringExtra(EXTRA_URL) != null;
    }

    /**
     * Launches the given url
     * @param context Caller context
     * @param url Url to be launched
     */
    private void launchBrowserIntent(Context context, String url, ReturnValues retValues) {

        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
            browserIntent.setData(Uri.parse(url));
            context.startActivity(browserIntent);
        } catch(Exception e) {
            retValues.status = false;
            Log.e(TAG, "Exception received while launching URI :" +e.toString());
        }

    }

    /**
     * Method to get config based on supplied parameters
     *
     * @param url Website url
     * @param ruleEndFlag Whether action kicks in at rule end or not
     * @return
     */
    public static String getConfig(String url, boolean ruleEndFlag){
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_RULE_ENDS, ruleEndFlag);
        return intent.toUri(0);
    }

}
