package com.test.silentcapture;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.CheckBoxPreference;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

/*import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.view.WindowManager;
import android.os.PowerManager;
*/

import com.test.silentcapture.Settings;
import com.test.silentcapture.R;

public class SettingActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.xml.main);
        addPreferencesFromResource(R.xml.preference);

        // Read value from shared preference
        SharedPreferences settings = getSharedPreferences(Settings.SHARED_PREFS, MODE_PRIVATE);
        boolean shared_auto_launch = settings.getBoolean(Settings.SHARED_AUTO_LAUNCH, true);
        //boolean shared_screen_on = settings.getBoolean(Settings.SHARED_SCREEN_ON, true);

        CheckBoxPreference auto_launch = (CheckBoxPreference)findPreference("auto_launch");
        //CheckBoxPreference screen_on_exit = (CheckBoxPreference)findPreference("screen_on_exit");
        auto_launch.setChecked(shared_auto_launch);
        //screen_on_exit.setChecked(shared_screen_on);
        
        auto_launch.setOnPreferenceChangeListener(this);
        //screen_on_exit.setOnPreferenceChangeListener(this);

        //if (Settings.LOGD) Log.d(Settings.TAG, "SettingActivity onCreate");
        // remote control
        //AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
        //manager.registerMediaButtonEventReceiver(new ComponentName(getPackageName(), TriggerReceiver.class.getName()));
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        SharedPreferences settings = getSharedPreferences("SilentCaptureSettings", MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        if (Settings.LOGD) Log.d(Settings.TAG, "SettingActivity onPreferenceChange newValue:" + (Boolean)newValue);

        if (preference.getKey().equals("auto_launch")) {
            editor.putBoolean(Settings.SHARED_AUTO_LAUNCH, (Boolean)newValue);
            editor.commit();

        } /*else if (preference.getKey().equals("screen_on_exit")) {
            editor.putBoolean(Settings.SHARED_SCREEN_ON, (Boolean)newValue);
            editor.commit();
        }*/
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //if (Settings.LOGD) Log.d(Settings.TAG, "SettingActivity onDestroy");
        // remote control
        //AudioManager manager = (AudioManager) getSystemService(AUDIO_SERVICE);
        //manager.unregisterMediaButtonEventReceiver(new ComponentName(getPackageName(), TriggerReceiver.class.getName()));
    }
}
