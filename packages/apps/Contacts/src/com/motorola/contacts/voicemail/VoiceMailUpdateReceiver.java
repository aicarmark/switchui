package com.motorola.contacts.voicemail;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receiver for voice mail number update.
 * It is currently used to handle ACTION_UPDATE_VOICEMAIL
 */
public class VoiceMailUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "VoiceMailUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
       if(intent.getAction().equals(VoiceMailUpdateService.ACTION_UPDATE_NOTIFICATIONS)){
           Log.v(TAG,VoiceMailUpdateService.ACTION_UPDATE_NOTIFICATIONS);
           updateVoicemailNotifications(context);
       }
    }
    private void updateVoicemailNotifications(Context context) {
        Intent serviceIntent = new Intent(context, VoiceMailUpdateService.class);
        serviceIntent.setAction(VoiceMailUpdateService.ACTION_UPDATE_VOICEMAIL);
        context.startService(serviceIntent);
    }
}
