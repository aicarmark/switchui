package com.test.silentcapture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.bluetooth.BluetoothHeadset;
import android.media.AudioManager;
import android.os.PowerManager;
import android.view.KeyEvent;

import com.test.silentcapture.Settings;
import com.test.silentcapture.CaptureService;

public class TriggerReceiver extends BroadcastReceiver {
    private static boolean mBTConnectedPulse = false;
    private static boolean mSetVolumeBySelf  = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (Settings.LOGD) Log.d(Settings.TAG, "TriggerReceiver on received action:" + intent);
        if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
            abortBroadcast();

            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            Log.d(Settings.TAG, "Trigger on media button event:" + event);
            if (event != null  && event.getAction() == KeyEvent.ACTION_UP) {
                int code = event.getKeyCode();
                //Log.d(Settings.TAG, "TriggerReceiver on media button, key code:" + code);
                if (code == KeyEvent.KEYCODE_MEDIA_NEXT ||
                    code == KeyEvent.KEYCODE_MEDIA_PREVIOUS ||
                    code == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                        Intent i = new Intent(context, CaptureService.class);
                        i.putExtra("CAPTURE_BUTTON", "button");
                        context.startService(i);
                }
            }

        // By normal BT headset
        } else if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
            if (Settings.LOGD) Log.d(Settings.TAG, "TriggerReceiver on BT state changed, state:" + state);
            // BT Headset disconnected, stop the capture service
            if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                Intent i = new Intent(context, CaptureService.class);
                i.putExtra("BT_DISCONNECTED", "true");
                //context.stopService(new Intent(context, CaptureService.class));
                context.startService(i);

            // BT Headset connected, start the capture service
            } else if (state == BluetoothHeadset.STATE_CONNECTED) {
                if (Settings.isAutoLaunchEnabled(context)) {
                    mBTConnectedPulse = true;
                    //context.startService(new Intent(context, CaptureService.class));

                    // init the volume of sco stream to ensure volume up / down button valid at any
                    // time. Avoid volume up invalid at max volume and volume down invalid at min
                    // volume.
                    resetVolume(context);
                }
            }
        
        // Capture button triggered
        } else if (action.equals("android.media.VOLUME_CHANGED_ACTION")) {
            // If volume is set by itself, discard this intent notify
            if (mSetVolumeBySelf) {
                mSetVolumeBySelf = false;
                Log.d(Settings.TAG, "TriggerReceiver on volume changed, but it's self triggerred.");
                return;
            }

            if (Settings.isAutoLaunchEnabled(context)) {

                int nowType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                int nowValue = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, -1);
                int preValue = intent.getIntExtra(AudioManager.EXTRA_PREV_VOLUME_STREAM_VALUE, -1);
                Log.d(Settings.TAG, "TriggerReceiver on volume changed, nowType:" + nowType + 
                        ", nowValue" + nowValue + ", preValue:" + preValue);

                PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        
                // Only BT headset volume changed causes capture action
                if (nowType == AudioManager.STREAM_BLUETOOTH_SCO) {
                    if (mBTConnectedPulse == true) {
                        mBTConnectedPulse = false;
                        Intent i = new Intent(context, CaptureService.class);
                        i.putExtra("BT_CONNECTED", "true");
                        context.startService(new Intent(context, CaptureService.class));
                    } else {
                        Intent i = new Intent(context, CaptureService.class);
                        i.putExtra("CAPTURE_BUTTON", "button");
                        context.startService(i);
                    }
                // Screen Off volume change will trigger capture too
                } else if (!power.isScreenOn()) {
                    Intent i = new Intent(context, CaptureService.class);
                    i.putExtra("CAPTURE_BUTTON", "button");
                    context.startService(i);
                }

                // reset volume.
                resetVolume(context);
            }
        
        // BOOT COMPLETED
        } else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            if (Settings.isAutoLaunchEnabled(context)) {
                Intent i = new Intent(context, CaptureService.class);
                i.putExtra("BOOT_COMPLETED", "true");
                context.startService(i); 
            }
        }
    }

    private void resetVolume(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int curVolume = am.getStreamVolume(AudioManager.STREAM_BLUETOOTH_SCO);
        int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_BLUETOOTH_SCO);
        Log.d(Settings.TAG, "TriggerReceiver resetVolume, sco max:" + maxVolume + ", cur:" + curVolume);
        mSetVolumeBySelf = true;
        am.setStreamVolume(AudioManager.STREAM_BLUETOOTH_SCO, maxVolume/2+1, 0);
    }
}
