/*
 * @(#)SetNotification.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/04/04  NA                  Initial version
 *
 */
package com.motorola.contextual.actions;

import java.security.SecureRandom;


import com.motorola.contextual.smartrules.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class represents Set Notofication action
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

public class SetNotification extends StatelessAction implements Constants {

    private static final String TAG = TAG_PREFIX + LaunchApp.class.getSimpleName();

    @Override
    public ReturnValues fireAction(Context context, Intent configIntent) {
	ReturnValues retValues = new ReturnValues();
        retValues.status = true;
        retValues.toFrom = QA_TO_MM;

        String message = configIntent.getStringExtra(EXTRA_MESSAGE);
        boolean vibrateFlag = configIntent.getBooleanExtra(EXTRA_VIBRATE, false);
        boolean soundFlag = configIntent.getBooleanExtra(EXTRA_SOUND, false);
        retValues.dbgString = message;

        if (LOG_INFO) Log.i(TAG, "vibrateFlag: " + vibrateFlag + ", soundFlag: " + soundFlag);

        int icon = R.drawable.stat_notify_sr_notification;
        CharSequence tickerText = message;
        long when = System.currentTimeMillis();
        CharSequence contentTitle = context.getString(R.string.notification_title);
        CharSequence contentText = message;
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(), 0);

        Notification notification = new Notification(icon, tickerText, when);
        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        notification.defaults = 0;
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        if (vibrateFlag) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        if (soundFlag) {
            notification.defaults |= Notification.DEFAULT_SOUND;
        }

        SecureRandom r = new SecureRandom();
        int id = r.nextInt();
        retValues.dbgString = Long.toString(id);
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
        retValues.exceptionString = null;
        return retValues;
    }

    @Override
    public String getActionString(Context context) {
        return context.getString(R.string.notif_label);
    }

    @Override
    public String getDescription(Context context, Intent configIntent) {
	String description = null;
	String message = configIntent.getStringExtra(EXTRA_MESSAGE);
	boolean vibStatus = configIntent.getBooleanExtra(EXTRA_VIBRATE, false);
	boolean soundStatus = configIntent.getBooleanExtra(EXTRA_SOUND, false);
        if((message == null) || ((message != null) && (message.length() == 0))) {
            if(vibStatus) {
                if(soundStatus) {
                    description = context.getString(R.string.vibrate_and_play);
                } else {
                    description = context.getString(R.string.vibrate);
                }
            } else {
                if(soundStatus) {
                    description = context.getString(R.string.play_sound);
                }
            }
        } else
            description = DOUBLE_QUOTE + message + DOUBLE_QUOTE;
        return description;
    }

    @Override
    public String getUpdatedConfig(Context context, Intent configIntent) {
        return getConfig(configIntent.getStringExtra(EXTRA_MESSAGE),
                configIntent.getBooleanExtra(EXTRA_VIBRATE, false),
                configIntent.getBooleanExtra(EXTRA_SOUND, false),
                configIntent.getBooleanExtra(OLD_EXTRA_RULE_ENDS, false));
    }

    @Override
    public boolean validateConfig(Intent configIntent) {
        String message = configIntent.getStringExtra(EXTRA_MESSAGE);
        return message != null || configIntent.hasExtra(EXTRA_VIBRATE) ||
                configIntent.hasExtra(EXTRA_SOUND);
    }

    /**
     * Method to get config based upon input parameters
     *
     * @param message Notification message
     * @param vibrate Vibrate flag
     * @param sound Notification sound flag
     * @param ruleEndFlag Activate the action at the end of the rule or not
     * @return
     */
    public static String getConfig (String message, boolean vibrate, boolean sound, boolean ruleEndFlag) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_VIBRATE, vibrate);
        intent.putExtra(EXTRA_SOUND, sound);
        intent.putExtra(EXTRA_RULE_ENDS, ruleEndFlag);
        return intent.toUri(0);
    }

}
