/*
 * Copyright (C) 2010-2011 Motorola, Inc.
 * All Rights Reserved.
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */
/*
 * some Proxy management runs here
 *
 * Ray Essick, ray.essick@motorola.com
 */
package com.motorola.contextual.proxy;


import com.motorola.contextual.actions.Constants;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class implements the ApplicationProxy for CE <code><pre>
 * CLASS:
 *     Extends BroadcastReceiver, implements Constants
 *
 * RESPONSIBILITIES:
 *     This Broadcast Receiver Serves as a proxy between Context Engine
 *     and various components of CA Framework, mainly Quick Actions.
 *     Any Sender permissions enforced by CA Framework components are to
 *     be added here instead of the CE.
 *     This receiver receives broadcast Intents from CE and translates and
 *     rebroadcasts the intents to other components.
 *
 * COLLABORATORS:
 *     None
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class ApplicationProxy extends BroadcastReceiver implements Constants
{

    private static final String TAG = Constants.TAG_PREFIX + ApplicationProxy.class.getSimpleName();
    public static final String PROXY = "com.motorola.situation.PROXY";

    Context mContext;

    Context getApplicationContext() {
        return mContext;
    }

    /**
     *  Semantics of the proxy.
     *  this is where we do interesting things based on the
     * command represented by 'event'
     *
     * @param event - Command. string is "name;[key=value;]end" - Only name is currently used
     * @param intent - Incoming Intent with the command removed
     */

    public void incoming(String event, Intent intent) {

        if (LOG_DEBUG) Log.d(TAG, "incoming event: " + event);

        String[] pieces = event.split(";");
        if (pieces == null || pieces.length == 0 || pieces[0] == null)
            return;

        if (pieces[0].equals("qa_setting_change")) {
            // Send QA Settings change intent with extras from the original intent
            Intent qaIntent = new Intent("com.motorola.intent.action.QUICK_SETTING_CHANGE");
            qaIntent.putExtras(intent);
            if (LOG_DEBUG) Log.d(TAG, "qa_setting_change; Sending Intent " + qaIntent.toUri(0));
            getApplicationContext().sendBroadcast(qaIntent);
        } else {
            // proper proxy behavior is to ignore the ones that
            // are not for me; they are likely for someone else.
        }
    }

    /*
     * This Broadcast Receiver Serves as a proxy between Context Engine
     * and various components of CA Framework, mainly Quick Actions.
     * Any Sender permissions enforced by CA Framework components are to
     * be added here instead of the CE.
     * This receiver receives broadcast Intents from CE and translates and
     * rebroadcasts the intents to other components.
     */

    @Override
    public void onReceive(Context context, Intent intent) {

        mContext = context;

        if (intent != null) {
            String action = intent.getAction();
            if (action != null && action.equals(PROXY)) {
                if (LOG_DEBUG) Log.d(TAG, "Received Proxy Intent " + intent.toUri(0));
                String value = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (value != null) {
                    // Extra Text contains the command; Remove it before processing the command
                    intent.removeExtra(Intent.EXTRA_TEXT);
                    incoming(value, intent);
                }
            } else {
                if (LOG_DEBUG) Log.d(TAG, "awoken for unknown action: "+intent.getAction());
            }
        }
    }
}
