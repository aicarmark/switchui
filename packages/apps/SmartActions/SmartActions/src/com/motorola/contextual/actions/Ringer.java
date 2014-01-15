/*
 * @(#)Ringer.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/17  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import com.motorola.contextual.pickers.actions.RingerActivity;
import com.motorola.contextual.smartrules.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.Settings.System;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;

/**
 * This class implements the StatefulActionInterface for Ringer <code><pre>
 * CLASS:
 *     implements StatefulActionInterface
 *
 * RESPONSIBILITIES:
 *     Interacts with Audio Manager to set/get the Ringer state
 *
 * COLLABORATORS:
 *     None
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class Ringer extends StatefulAction implements Constants {

    private static final String TAG = TAG_PREFIX + Ringer.class.getSimpleName();
    public static final String RINGER_ACTION_KEY = ACTION_KEY_PREFIX + Ringer.class.getSimpleName();
    private static final String RINGER_MODE_PREVIOUS_SET_STATE = "ringer_mode_current_state";
    private static final String RINGER_VIBRATE_PREVIOUS_SET_STATE = "ringer_vibrate_current_state";
    private static final String RINGER_VOLUME_PREVIOUS_SET_STATE = "ringer_volume_current_state";

    private int mOldRingerMode;
    private int mOldVibrateSetting;
    private int mOldRingerVolume;
    private static int mMaxRingerVolume = 15;
    private int mRingerMode;
    private int mVibrateSetting;
    private String mMode;

    public boolean setState(Context context, Intent intent) {

        if (intent != null) {
            AudioManager mAudioManager = (AudioManager)context.getSystemService(Activity.AUDIO_SERVICE);

            mRingerMode = intent.getIntExtra(EXTRA_RINGER_MODE, mAudioManager.getRingerMode());
            boolean vibCheckBoxStatus = intent.getBooleanExtra(EXTRA_VIBE_STATUS, false);
            mMode = intent.getStringExtra(EXTRA_MODE_STRING);

            String action = intent.getAction();
            if (action != null) {
                if (action.equals(ACTION_EXT_RINGER_CHANGE)) {
                    mVibrateSetting = (vibCheckBoxStatus) ? AudioManager.VIBRATE_SETTING_ON : AudioManager.VIBRATE_SETTING_ONLY_SILENT;
                    int ringerVolume = 0;
                    if (mRingerMode == AudioManager.RINGER_MODE_NORMAL) {
                        ringerVolume = intent.getIntExtra(EXTRA_RINGER_VOLUME,
                                                              mAudioManager.getStreamVolume(AudioManager.STREAM_RING));
                    }
                    if (LOG_INFO) {
                        Log.i(TAG, "Values committed for conflict resoulution. Mode: " + mRingerMode +
                                " Vibrate: " + mVibrateSetting + " Volume: " + ringerVolume);
                    }
                    //Expecting the conflicting actions to set the ringer settings to these values
                    //Ringer action will ignore the changes in handleSettingChanged
                    Persistence.commitValue(context, RINGER_MODE_PREVIOUS_SET_STATE, mRingerMode);
                    Persistence.commitValue(context, RINGER_VIBRATE_PREVIOUS_SET_STATE, mVibrateSetting);
                    Persistence.commitValue(context, RINGER_VOLUME_PREVIOUS_SET_STATE, ringerVolume);
                    return true;
                }
            }

            mOldRingerMode = mAudioManager.getRingerMode();
            mOldVibrateSetting = mAudioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
            mOldRingerVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);

            if (LOG_INFO)
                Log.i(TAG, "Current Mode/Vib setting " + mOldRingerMode + "," + mOldVibrateSetting);

            if (intent.hasExtra(EXTRA_VIBE_SETTING)) {
                //For backward compatibility. This extra was a part of the default intent.
                //Use the vibrate setting value saved in the default intent
                int vibSetting = intent.getIntExtra(EXTRA_VIBE_SETTING, AudioManager.VIBRATE_SETTING_ONLY_SILENT);
                vibCheckBoxStatus = (vibSetting == AudioManager.VIBRATE_SETTING_ON ? true : false);
            }

            int ringerVolume = 0;
            if (mRingerMode == AudioManager.RINGER_MODE_NORMAL) {
                ringerVolume = intent.getIntExtra(EXTRA_RINGER_VOLUME,
                                                      mAudioManager.getStreamVolume(AudioManager.STREAM_RING));
                mMaxRingerVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
                int maxOldRingerVolume = intent.getIntExtra(EXTRA_MAX_RINGER_VOLUME, mMaxRingerVolume);
                ringerVolume = (ringerVolume*mMaxRingerVolume)/maxOldRingerVolume;

                if (ringerVolume == 0) {
                    //In GB it is possible to set ringer mode "normal" with volume 0
                    //In ICS ringer volume 0 means "silent" or "vibrate" mode. Convert accordingly
                    if (vibCheckBoxStatus) {
                        mRingerMode = AudioManager.RINGER_MODE_VIBRATE;
                    } else {
                        mRingerMode = AudioManager.RINGER_MODE_SILENT;
                    }
                }

                mAudioManager.setStreamVolume(AudioManager.STREAM_RING, ringerVolume, 0);
            } else if (mRingerMode == AudioManager.RINGER_MODE_SILENT) {
                if (vibCheckBoxStatus && !intent.getBooleanExtra(EXTRA_RESTORE_DEFAULT, false)) {
                    //To fix Meeting rule issue
                    //It is not possible for the user to configure Ringer action with RINGER_MODE_SILENT and
                    //vibe status as true through UI. This can only happen if such a config was saved by a sample rule
                    mRingerMode = AudioManager.RINGER_MODE_VIBRATE;
                }
            }
            mAudioManager.setRingerMode(mRingerMode);

            //setVibrateSetting influences the vibrate setting in normal ringer mode.
            //Ringer mode silent/vibrate governs the vibrate setting in other modes
            mVibrateSetting = (vibCheckBoxStatus) ? AudioManager.VIBRATE_SETTING_ON : AudioManager.VIBRATE_SETTING_ONLY_SILENT;
            if (mVibrateSetting != mOldVibrateSetting) {
                mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, mVibrateSetting);
            }

            if (LOG_INFO)
                Log.i(TAG, "Values committed are Mode: "+mRingerMode+" Vibrate: "+mVibrateSetting+" Volume: "+ringerVolume);
            Persistence.commitValue(context, RINGER_MODE_PREVIOUS_SET_STATE, mRingerMode);
            Persistence.commitValue(context, RINGER_VIBRATE_PREVIOUS_SET_STATE, mVibrateSetting);
            Persistence.commitValue(context, RINGER_VOLUME_PREVIOUS_SET_STATE, ringerVolume);

            return true;
        }
        return false;
    }

    public String getDescription(Context context, Intent configIntent) {
        AudioManager mAudioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        int ringerVolume = configIntent.getIntExtra(EXTRA_RINGER_VOLUME, maxVolume);
        int maxOldRingerVolume = configIntent.getIntExtra(EXTRA_MAX_RINGER_VOLUME, maxVolume);
        ringerVolume = (ringerVolume*maxVolume)/maxOldRingerVolume;
        int ringerMode = configIntent.getIntExtra(EXTRA_RINGER_MODE, 0);
        boolean vibStatus = configIntent.getBooleanExtra(EXTRA_VIBE_STATUS, false);
        return getDescription(context, ringerVolume, ringerMode, vibStatus);
    }

    public String getUpdatedConfig(Intent configIntent) {
        return getConfig(configIntent.getBooleanExtra(EXTRA_VIBE_STATUS, false),
                configIntent.getIntExtra(EXTRA_RINGER_MODE, 0), configIntent.getIntExtra(EXTRA_RINGER_VOLUME, 0),
                configIntent.getIntExtra(EXTRA_MAX_RINGER_VOLUME, mMaxRingerVolume));
    }

    public List<String> getConfigList(Context context) {
        return new ArrayList<String>();
    }

    public List<String> getDescriptionList(Context context) {
        return new ArrayList<String>();
    }

     /**
       * Utility method to get user readable description of VIP Ringer action
       *
       * @param context Caller's context
       * @param names VIP callers
       * @param delimiter Delimiter separating the names
       * @param ringerVolume Volume level for VIP callers
       * @param maxVolume Max volume supported by the device
       * @return VIP caller mode action description
       */
    public static String getDescription (Context context, int ringerVolume, int ringerMode, boolean vibStatus) {
        String desc = "";

        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            AudioManager mAudioManager = (AudioManager)context.getSystemService(Activity.AUDIO_SERVICE);
            int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
            if (LOG_INFO)
                Log.i(TAG, "Stream set, max volume is " + ringerVolume + "," + maxVolume);
            desc = RingerActivity.getThresholdString(context, (ringerVolume * 100) / maxVolume);
            if(vibStatus)
                desc = desc + context.getString(R.string.vibe_and);
        } else {
            if (vibStatus) {
                desc = context.getString(R.string.silent_vib);
            } else {
                desc = context.getString(R.string.silent);
            }
        }
        return desc;
    }

    public String getState(Context context) {
        return mMode;
    }

    public String getSettingString(Context context) {
        return convertRingerModeToUserString(context, mRingerMode);
    }

    public String getDefaultSetting(Context context) {
        AudioManager audioManager = (AudioManager)context.getSystemService(Activity.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        boolean vibStatus = (mOldVibrateSetting == AudioManager.VIBRATE_SETTING_ON) ? true : false;
        return getConfig(vibStatus, mOldRingerMode, mOldRingerVolume, maxVolume);

    }

    String getDefaultSetting(Context context, Intent defaultIntent) {
        return getUpdatedConfig(defaultIntent);
    }

    public void registerForSettingChanges(Context context) {
        StatefulActionHelper.registerForSettingChanges(context, RINGER_ACTION_KEY);
    }

    public void deregisterFromSettingChanges(Context context) {
        StatefulActionHelper.deregisterFromSettingChanges(context, RINGER_ACTION_KEY);
    }

    public Status handleSettingChange(Context context, Object obj) {
        Status status = Status.NO_CHANGE;
        AudioManager audioManager = (AudioManager)context.getSystemService(Activity.AUDIO_SERVICE);
        mMaxRingerVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        mRingerMode = audioManager.getRingerMode();
        mMode = convertRingerModeToString(mRingerMode);
        // handleSettingChange is called with Intent as an argument from SettingsReceiver
        // It is called with String as an argument from SettingsService
        // So, we can differentiate between the two cases by using argument obj
        if (obj instanceof Intent) {
            // obj is is an Intent. So it can be a notification for either vibrate setting change
            // or ringer mode change as they are sent from SettingsReceiver
            Intent intent = (Intent)obj;
            if (intent != null) {
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(AudioManager.VIBRATE_SETTING_CHANGED_ACTION)) {
                        mVibrateSetting = intent.getIntExtra(AudioManager.EXTRA_VIBRATE_SETTING, AudioManager.VIBRATE_SETTING_ONLY_SILENT);
                        int oldVibrateSetting = Persistence.retrieveIntValue(context, RINGER_VIBRATE_PREVIOUS_SET_STATE);

                        if (LOG_INFO)
                            Log.i(TAG, "handleSettingChange vibrate mode new value: "+mVibrateSetting+" old value: "+oldVibrateSetting);
                        if (mVibrateSetting != oldVibrateSetting) {
                            status = Status.SUCCESS;
                        }
                    } else {
                        int oldRingerMode = Persistence.retrieveIntValue(context, RINGER_MODE_PREVIOUS_SET_STATE);
                        if (LOG_INFO)
                            Log.i(TAG, "handleSettingChange ringer mode new value: "+mRingerMode+" old value: "+oldRingerMode);
                        if (oldRingerMode != mRingerMode) {
                            status = Status.SUCCESS;
                        }
                    }
                }
            }
        } else if (obj instanceof String) {
            // obj is is a String. So it is a notification for volume change
            // as it is sent from SettingsService
            int oldVolume = Persistence.retrieveIntValue(context, RINGER_VOLUME_PREVIOUS_SET_STATE);
            int volume = System.getInt(context.getContentResolver(), System.VOLUME_SETTINGS[AudioManager.STREAM_RING], 0);
            if (LOG_INFO)
                Log.i(TAG, "handleSettingChange volume new value: " + volume + " old value: " + oldVolume);
            if(oldVolume != volume) {
                status = Status.SUCCESS;
            }
        }

        return status;
    }

    public String[] getSettingToObserve() {
        return new String[] {
                    System.VOLUME_SETTINGS[AudioManager.STREAM_RING]
                };
    }

    public Uri getUriForSetting(String setting) {
        return System.getUriFor(setting);
    }

    public String getActionString(Context context) {
        return context.getString(R.string.Ringer);
    }

    public String getActionKey() {
        return RINGER_ACTION_KEY;
    }

    public String getBroadcastAction() {
        return AudioManager.RINGER_MODE_CHANGED_ACTION+BROADCAST_ACTION_DELIMITER+AudioManager.VIBRATE_SETTING_CHANGED_ACTION;
    }

    /**
     * Converts ringerMode to String
     *
     * @param ringerMode
     * @return
     */
    public static String convertRingerModeToString(int ringerMode) {
        String ringer = NORMAL;
        if (ringerMode == AudioManager.RINGER_MODE_VIBRATE)
            ringer = VIBRATE;
        else if (ringerMode == AudioManager.RINGER_MODE_SILENT)
            ringer = SILENT;

        return ringer;

    }

    /**
     * Converts ringerMode to user readable String
     *
     * @param context
     * @param ringerMode
     * @return
     */
    public static String convertRingerModeToUserString(Context context, int ringerMode) {
        String ringer = context.getString(R.string.normal);
        if (ringerMode == AudioManager.RINGER_MODE_VIBRATE)
            ringer = context.getString(R.string.vibrate);
        else if (ringerMode == AudioManager.RINGER_MODE_SILENT)
            ringer = context.getString(R.string.silent);

        return ringer;

    }

    /**
     * Method to return config based upon supplied parameters
     *
     * @param vibrateStatus Vibrate on or off
     * @param ringerMode Normal/silent/vibrate
     * @param ringerVolume Ringer volume level
     * @param maxRingerVolume Max ringer volume supported
     * @return
     */
    public final static String getConfig(boolean vibrateStatus, int ringerMode,
            int ringerVolume, int maxRingerVolume) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        intent.putExtra(EXTRA_RINGER_VOLUME, ringerVolume);
        intent.putExtra(EXTRA_MAX_RINGER_VOLUME, maxRingerVolume);
        intent.putExtra(EXTRA_VIBE_STATUS, vibrateStatus);
        intent.putExtra(EXTRA_RINGER_MODE, ringerMode);
        intent.putExtra(EXTRA_MODE_STRING, Ringer.convertRingerModeToString(ringerMode));
        if (LOG_INFO) Log.i(TAG, "getConfig : " +  intent.toUri(0));
        return intent.toUri(0);
    }

	@Override
	public String getUpdatedConfig(Context context, Intent configIntent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean validateConfig(Intent configIntent) {
		// TODO Auto-generated method stub
		return false;
	}
}
