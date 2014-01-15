/*
 * @(#)CallSmsReceiver.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2011/08/01  NA                Initial version
 * rdq478       2011/10/26  IKMAIN-31519      Added Voice Announce
 */

package com.motorola.contextual.actions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;
import android_const.provider.TelephonyConst;

/**
 * This class receives phone state change intents and takes action according to the type of event.
 * <code><pre>
 * CLASS:
 *     Extends BroadcastReceiver.
 *
 * RESPONSIBILITIES:
 *     Receive incoming call event and launch VIP Ringer Action if it is active.
 *     Receive missed call/incoming SMS event and launch Auto text reply action if it is active.
 *
 * COLLABORATORS:
 *     DatabaseUtilityService
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class CallSmsReceiver extends BroadcastReceiver implements Constants {

    private static final String TAG = TAG_PREFIX + CallSmsReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (LOG_INFO) Log.i(TAG, "In onReceive to handle " + intent.toUri(0));
        String action = intent.getAction();

        if (action != null) {
            if (action.equals(TelephonyConst.ACTION_SMS_RECEIVED) ||
                    action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED) ||
                    action.equals("com.motorola.smartactions.intent.action.CONDITION_PUBLISHER_CALL_END_EVENT")) {

                /* Handle Voice Annouce Action if needed for SMS
                 * For incoming calls it will be handled by DatabaseUtilityService
                 * after VIP Ringer is handled*/
                if (action.equals(TelephonyConst.ACTION_SMS_RECEIVED)) {

                    SetVoiceAnnounce va = new SetVoiceAnnounce();
                    va.handleSettingChange(context, intent);
                }

                if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                    String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    if ((phoneState != null) && phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE) &&
                            Persistence.retrieveValue(context, BLUETOOTH_STATE_AFTER_CALL) != null) {

                        //There is a pending BT action

                        Bluetooth sai =
                            (Bluetooth) ActionHelper.getAction(context, BLUETOOTH_ACTION_KEY);

                        if (sai == null) {
                            Log.w(TAG, "Could not get BT interface");
                            return;
                        }

                        sai.handlePhoneState(context, phoneState);
                    }
                }

                intent.setClass(context, DatabaseUtilityService.class);
                intent.putExtra(EXTRA_INTENT_ACTION, intent.getAction());
                context.startService(intent);
            }
        }
    }
}
