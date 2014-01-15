/*
 * @(#)SetVoiceAnnounce.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * rdq478       2011/09/28   IKMAIN-28588     Initial version
 * rdq478       2011/10/26   IKMAIN-31519     Moved ACTION_SMS_RECEIVED to Constants
 */

package com.motorola.contextual.actions;

import java.util.ArrayList;
import java.util.List;

import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android_const.provider.TelephonyConst;

/**
 * This class extends the StatefulAction class for SetVoiceAnnounce <code><pre>
 * CLASS:
 *     extends StatefulAction
 *
 * RESPONSIBILITIES:
 *     Perform voice announce during incoming call or text message.
 *
 * COLLABORATORS:
 *     None
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public final class SetVoiceAnnounce extends StatefulAction implements Constants {

    private static final String TAG = TAG_PREFIX + SetVoiceAnnounce.class.getSimpleName();
    private static final String VOICE_ANNOUNCE_ACTION_KEY = ACTION_KEY_PREFIX + SetVoiceAnnounce.class.getSimpleName();
    private static final String ACTION_PHONE_STATE = TelephonyManager.ACTION_PHONE_STATE_CHANGED;
    private static final String VOICE_ANNOUNCE_SETTINGS = "voice_announce_settings";
    public static final String VOICE_ANNOUNCE_REGISTER = "voice_announce_register";
    private static final String TEXT_AND_CALL = "1";
    private static final String TEXT_ONLY = "2";
    private static final String CALL_ONLY = "3";

    @Override
    public boolean setState(Context context, Intent intent) {
        if (LOG_DEBUG) Log.d(TAG, "setState.");

        boolean status = false;

        if (context == null || intent == null) {
            Log.e(TAG, "Error: could not setState either context or intent is null");
            return status;
        }

        if (intent.getBooleanExtra(EXTRA_RESTORE_DEFAULT, false)) {
            Persistence.removeBooleanValue(context, VOICE_ANNOUNCE_REGISTER);
        } else {
            setVARead(context,
                    intent.getBooleanExtra(EXTRA_VA_READ_TEXT, false),
                    intent.getBooleanExtra(EXTRA_VA_READ_CALL, false));
            if (intent.getBooleanExtra(EXTRA_SAVE_DEFAULT, false)) {
                Persistence.commitValue(context, VOICE_ANNOUNCE_REGISTER, true);
            }
        }
        status = true;

        return status;
    }

    @Override
    public String getDefaultSetting(Context context) {
        String defaultSetting = null;
        String vaSettings = Persistence.retrieveValue(context,
                VOICE_ANNOUNCE_SETTINGS);
        if (vaSettings != null) {
            boolean text = false;
            boolean call = false;
            if (vaSettings.equals(TEXT_AND_CALL)) {
                text = true;
                call = true;
            } else if (vaSettings.equals(CALL_ONLY)) {
                call = true;
            } else if (vaSettings.equals(TEXT_ONLY)) {
                text = true;
            }
            defaultSetting = getConfig(text, call);
        }
        return defaultSetting;
    }

    @Override
    public String getActionString(Context context) {
        return context.getString(R.string.voice_announce);
    }

    @Override
    public Status handleSettingChange(Context context, Object obj) {
        
        Status status = Status.FAILURE;

        if (context == null || obj == null) {
            Log.e(TAG, "Error unable to handle setting change, either context or obj is null");
            return status;
        }
        else if (!Persistence.retrieveBooleanValue(context, VOICE_ANNOUNCE_REGISTER)) {
            status = Status.SUCCESS;
            return status;
        }

        String vaSettings = Persistence.retrieveValue(context, VOICE_ANNOUNCE_SETTINGS);
        if (LOG_INFO) Log.i(TAG, "HandleSettingChange: setting = " + vaSettings);

        if(vaSettings == null) {
            Log.e(TAG, "Error unable to handle setting change, no vaSettings");
            return status;
        }

        Intent intent = (Intent) obj;
        String action = intent.getAction();

        if (action == null) {
            Log.e(TAG, "Error unable to handle setting change, action is null");
            return status;
        }
        else if (action.equals(ACTION_PHONE_STATE) && (!vaSettings.equals(TEXT_ONLY))) {
            startPhoneCallService(context, intent);
            status = Status.NO_CHANGE;
        }
        else if (action.equals(TelephonyConst.ACTION_SMS_RECEIVED) && (!vaSettings.equals(CALL_ONLY))) {
            startSmsService(context, intent);
            status = Status.NO_CHANGE;
        }
        else {
            if (LOG_DEBUG) Log.d(TAG, "Unknown or unconfigured action received, action = " + action);
        }

        return status;
    }

    @Override
    public String getActionKey() {
        return VOICE_ANNOUNCE_ACTION_KEY;
    }

    /**
     * Method for saving the voice announce settings to shared preferences
     *
     * @param context
     *            - application's context
     * @param text
     *            - true if announcement shall be done for sms, false otherwise
     * @param call
     *            - true if announcement shall be done for call, false otherwise
     */
    private static void setVARead(Context context, boolean text, boolean call) {

        String vaConfigValue = (((text == true) && (call == true)) ? TEXT_AND_CALL
                : ((text == false) ? CALL_ONLY : TEXT_ONLY));
        if (LOG_INFO) Log.i(TAG, "setVARead." + vaConfigValue);

        Persistence
                .commitValue(context, VOICE_ANNOUNCE_SETTINGS, vaConfigValue);
    }

    /**
     * Method for starting the voice announce service for announcing the current
     * phone call
     *
     * @param context
     *            - application's context
     * @param intent
     *            - intent containing the config EXTRA_STATE
     */
    private void startPhoneCallService(Context context, Intent intent) {
        String callState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (callState != null) {
            if (callState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                String number = intent
                        .getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                Intent serviceIntent = new Intent(context,
                        VoiceAnnounceService.class);
                serviceIntent.putExtra(EXTRA_VA_READ_CALL, true);
                serviceIntent.putExtra(EXTRA_NUMBER, number);
                context.startService(serviceIntent);
            } else if (callState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                Intent serviceIntent = new Intent(context,
                        VoiceAnnounceService.class);
                serviceIntent.putExtra(EXTRA_VA_CALL_ACTIVE, true);
                context.startService(serviceIntent);

            } else if (callState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                Intent serviceIntent = new Intent(context,
                        VoiceAnnounceService.class);
                serviceIntent.putExtra(EXTRA_VA_CALL_IDLE, true);
                context.startService(serviceIntent);
            }
        }
    }

    /**
     * Method for starting the voice announce service for sms recieved
     *
     * @param context
     *            - application's context
     * @param intent
     *            - intent containing the information like phone number and
     *            other config related details
     */
    private void startSmsService(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        Intent serviceIntent = new Intent(context, VoiceAnnounceService.class);
        serviceIntent.putExtra(EXTRA_VA_READ_TEXT, true);
        serviceIntent.putExtras(bundle);
        context.startService(serviceIntent);
    }

    @Override
    public String getDescription(Context context, Intent configIntent) {
        return getDescription(context,
                configIntent.getBooleanExtra(EXTRA_VA_READ_TEXT, false),
                configIntent.getBooleanExtra(EXTRA_VA_READ_CALL, false));
    }

    /**
     * This method returns the user readable description string for voice
     * announce
     *
     * @param context
     *            - application's context
     * @param text
     *            - true if announcement shall happen for sms
     * @param call
     *            - true if announcement shall happen for call
     * @return
     */
    private String getDescription(Context context, boolean text, boolean call) {
        String description = null;
        if (text && call) {
            description = context
                    .getString(R.string.caller_name_and_text_message_sender);
        } else {
            String[] descArray = context.getResources().getStringArray(
                    R.array.voice_announce_items);
            if (text) {
                description = descArray[0];
            } else if (call) {
                description = descArray[1];
            }
        }
        return description;
    }

    @Override
    public String getUpdatedConfig(Context context, Intent configIntent) {
        return getConfig(configIntent.getBooleanExtra(EXTRA_VA_READ_TEXT, false),
                configIntent.getBooleanExtra(EXTRA_VA_READ_CALL, false));
    }

    @Override
    public List<String> getConfigList(Context context) {
        ArrayList<String> configList = new ArrayList<String>();
        configList.add(getConfig(true, true));
        configList.add(getConfig(true, false));
        configList.add(getConfig(false, true));
        return configList;
    }

    @Override
    public List<String> getDescriptionList(Context context) {
        ArrayList<String> descList = new ArrayList<String>();
        descList.add(getDescription(context, true, true));
        descList.add(getDescription(context, true, false));
        descList.add(getDescription(context, false, true));
        return descList;
    }

    @Override
    String getDefaultSetting(Context context, Intent defaultIntent) {
        return getUpdatedConfig(context, defaultIntent);
    }

    @Override
    public boolean validateConfig(Intent configIntent) {
        return configIntent.hasExtra(EXTRA_VA_READ_TEXT) ||
                configIntent.hasExtra(EXTRA_VA_READ_CALL);
    }

    /**
     * This method returns the config string (intent.toUri(0)) for various
     * possible configs
     *
     * @param text
     *            - true if voice announce shall happen for sms
     * @param call
     *            - true if voice announce shall happen for call
     * @return - config string (intent.toUri(0))
     */
    public static String getConfig(boolean text, boolean call) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        intent.putExtra(EXTRA_VA_READ_TEXT, text);
        intent.putExtra(EXTRA_VA_READ_CALL, call);
        return intent.toUri(0);
    }

    @Override
    String getSettingString(Context context) {
        // Nothing to be done here
        return null;
    }
}
