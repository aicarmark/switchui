package com.test.silentcapture;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.IBinder;
import android.view.KeyEvent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioManager;
import android.view.WindowManager;
import android.os.PowerManager;
import android.os.Handler;
import android.view.View;

import com.test.silentcapture.CaptureSurface;
import com.test.silentcapture.Settings;

import android.util.Log;


public class CaptureService extends Service implements OnSharedPreferenceChangeListener {

    private static CaptureSurface mCapture;
    private PowerManager.WakeLock mWakeLock;

    private void enterCapture() {
        /**
         * Enter capture preview if the screen already been off.
         * Otherwise, it would enter capture preview after screen turns off.
         */
        boolean addWhileScreenOn = !Settings.isScreenOnExitEnabled(CaptureService.this);
        boolean addWhileScreenOff = (Settings.isScreenOnExitEnabled(CaptureService.this) && isScreenOff());
        boolean doAdd = (addWhileScreenOn || addWhileScreenOff);

        if (mCapture == null && doAdd) {
            mCapture = new CaptureSurface(CaptureService.this);

            WindowManager wm = (WindowManager) getApplicationContext().getSystemService("window");
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.type = 2006;
            params.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN;
            params.x = 0;
            params.y = 0;
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.MATCH_PARENT;
            params.format = 1;
            wm.addView(mCapture, params);    
        }

        // WakeLock: screen off will lead to CPU slows down, use wake lock to keep CPU 100% active.
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "strong_cpu");
            mWakeLock.acquire();
        }       

        // remote control
        AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
        manager.registerMediaButtonEventReceiver(new ComponentName(getPackageName(), TriggerReceiver.class.getName()));
    }

    private void exitCapture() {
        if (mCapture != null) {
            WindowManager wm = (WindowManager) getApplicationContext().getSystemService("window");
            wm.removeView(mCapture);
            mCapture = null;    
        }

        // WakeLock: release the lock.
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    private void capture() {
        if (mCapture == null) {
            enterCapture();
        }
        if (mCapture != null) {
            mCapture.capture();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // not supported
        return null;
    }

    private boolean isScreenOff() {
        PowerManager power = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return !power.isScreenOn();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Settings.LOGD) Log.d(Settings.TAG, "SilentCaptureService onCreate");

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        //filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(mReceiver, filter);

        SharedPreferences settings = this.getApplicationContext().getSharedPreferences(Settings.SHARED_PREFS, Context.MODE_PRIVATE);
        settings.registerOnSharedPreferenceChangeListener(this); 
    }  

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Settings.LOGD) Log.d(Settings.TAG, "SilentCaptureService onStartCommand intent:" + intent);
        // BT disconnected
        if (intent != null && intent.hasExtra("BT_DISCONNECTED")) {
            exitCapture();
        }

        // BT connected
        if (intent != null && intent.hasExtra("BT_CONNECTED")) {
            enterCapture();
        }

        // BT headset button triggered
        if (intent != null && intent.hasExtra("CAPTURE_BUTTON")) {
            enterCapture();
            capture();
        }
        
        // Boot completed
        if (intent != null && intent.hasExtra("BOOT_COMPLETED")) {
            // Do nothing
        }

        //return super.onStartCommand(intent, flags, startId);
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Settings.LOGD) Log.d(Settings.TAG, "SilentCaptureService onDestroy");

        unregisterReceiver(mReceiver);

        SharedPreferences settings = this.getApplicationContext().getSharedPreferences(Settings.SHARED_PREFS, Context.MODE_PRIVATE);
        settings.unregisterOnSharedPreferenceChangeListener(this);

        exitCapture();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Settings.LOGD) Log.d(Settings.TAG, "SilentCaptureService received intent:"+action);

            // Screen On will trigger exiting capture preview state.
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                if (Settings.isScreenOnExitEnabled(CaptureService.this)) {
                    exitCapture();

                    // remote control
                    AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
                    manager.unregisterMediaButtonEventReceiver(new ComponentName(getPackageName(), TriggerReceiver.class.getName()));
                }

            // Screen Off will trigger entering capture preview state.
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                if (Settings.isScreenOnExitEnabled(CaptureService.this)) {
                    //enterCapture();

                    // remote control
                    AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
                    manager.registerMediaButtonEventReceiver(new ComponentName(getPackageName(), TriggerReceiver.class.getName()));
                }
                 
            // Media Button will trigger capture an image.
            /*} else if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
                abortBroadcast();

                KeyEvent key = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                int keyCode = key.getKeyCode();
                Log.d(Settings.TAG, "SilentCaptureService onReceive media button, key:" + keyCode);
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                    capture();
                }
            */
            // take capture
            } else if ("android.media.VOLUME_CHANGED_ACTION".equals(action)) {
                //int nowType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                //int nowValue = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, -1);
                //int preValue = intent.getIntExtra(AudioManager.EXTRA_PREV_VOLUME_STREAM_VALUE, -1);
                //Log.d(Settings.TAG, "CaptureService on volume changed, nowType:" + nowType + 
                //        ", nowValue" + nowValue + ", preValue:" + preValue);
                // Only BT headset volume changed causes capture action
               // if ((nowValue != preValue) && (nowType == AudioManager.STREAM_BLUETOOTH_SCO)) {
                //    capture();
                //}
            }
        }
    };   

    /**
     * To observer shared preference value.
     * shared_auto_launch: if it turns off, stop the service.
     * shared_screen_on: 
     */
    public void onSharedPreferenceChanged(SharedPreferences shared, String key) {
        boolean value = shared.getBoolean(key, true);
        Log.d(Settings.TAG, "CaptureService onSharedPreferenceChanged key:" + key + ", value:" + value);

        if (key.equals(Settings.SHARED_AUTO_LAUNCH)) {
            // diabled the capture service
            if (value == false) {
                exitCapture();
                stopSelf();
            
            // enabled the capture service
            } else {
                // nothing todo
            }
            
        } else if (key.equals(Settings.SHARED_SCREEN_ON)) {
            // Support screen on capture preview
            if (value == false) {
                enterCapture();
            // Only support screen off capture preview
            } else {
                // nothing todo
            }
        }
    }
}
