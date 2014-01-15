/*
 * @(#)VipRinger.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/08/02  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.Log;
import android_const.provider.SettingsConst;

import com.motorola.contextual.commonutils.StringUtils;
import com.motorola.contextual.smartrules.R;

/**
 * This class extends the StatefulAction class for VIP Ringer <code><pre>
 * CLASS:
 *     extends StatefulAction
 *
 * RESPONSIBILITIES:
 *     Interacts with Audio Manager to set/get the Ringer state for specific contacts
 *
 * COLLABORATORS:
 *     None
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class VipRinger extends StatefulAction implements Constants {

    private static final String TAG = TAG_PREFIX + VipRinger.class.getSimpleName();
    public static final String VIP_RINGER_ACTION_KEY = ACTION_KEY_PREFIX + VipRinger.class.getSimpleName();
    private static final String RINGER_MODE_PREVIOUS_SET_STATE = "vip_ringer_mode_current_state";
    private static final String RINGER_VIBRATE_PREVIOUS_SET_STATE = "vip_ringer_vibrate_current_state";
    private static final String RINGER_VOLUME_PREVIOUS_SET_STATE = "vip_ringer_volume_current_state";

    public static final int VOLUME_LEVEL_MAX = 100;
    public static final int VOLUME_LEVEL_HIGH = 75;
    public static final int VOLUME_LEVEL_MEDIUM = 50;

    private static final int MAX_RINGER_VOLUME = 15;

    private static final double CONFIG_VERSION = 1.1;

    private String mInternalName;
    private int mRingerMode;

    
    @Override
    public boolean setState(Context context, Intent intent) {
        mInternalName = intent.getStringExtra(EXTRA_INTERNAL_NAME);
        if (LOG_INFO) Log.i(TAG, "setState called for internal name:"+mInternalName);

        intent.setClass(context, DatabaseUtilityService.class);
        intent.putExtra(EXTRA_INTENT_ACTION, VIP_RINGER_ACTION_KEY);
        if (intent.getBooleanExtra(EXTRA_RESTORE_DEFAULT, false)) {
            intent.putExtra(EXTRA_REGISTER_RECEIVER, false);
            context.startService(intent);
        } else {
            intent.putExtra(EXTRA_REGISTER_RECEIVER, true);
            context.startService(intent);
        }

        //The rule is triggered now, don't have to execute action.
        //Action is executed when incoming call comes
        return true;
    }

    @Override
    public String getState(Context context) {
        return convertRingerModeToString(mRingerMode);
    }

    @Override
    public String getSettingString(Context context) {
        return convertRingerModeToUserString(context, mRingerMode);
    }

    @Override
    public String getDefaultSetting(Context context) {

        Intent configIntent = new Intent();
        configIntent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        configIntent.putExtra(EXTRA_INTERNAL_NAME, mInternalName);
        return configIntent.toUri(0);

    }

    @Override
    String getDefaultSetting(Context context, Intent defaultIntent) {
        Intent configIntent = new Intent();
        configIntent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        configIntent.putExtra(EXTRA_INTERNAL_NAME, defaultIntent.getStringExtra(EXTRA_INTERNAL_NAME));
        return configIntent.toUri(0);
    }

    @Override
    public Status handleSettingChange(Context context, Object obj) {

        boolean extChange = false;
        if (Persistence.retrieveValue(context, NUMBER_TO_END) == null) {
            //NUMBER_TO_END is present in persistence whenever a call from VIP number is going on
            //Settings change intent to be sent only when a call with VIP is going on
            return Status.NO_CHANGE;
        }

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
                    if (action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {

                        mRingerMode = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE,
                                                         AudioManager.RINGER_MODE_NORMAL);
                        int oldRingerMode = Persistence.retrieveIntValue(context, RINGER_MODE_PREVIOUS_SET_STATE);
                        if (LOG_INFO)
                            Log.i(TAG, "handleSettingChange ringer mode new : "+mRingerMode+" old : "+oldRingerMode);
                        if (oldRingerMode != mRingerMode) {
                            extChange = true;
                        }
                    } else if (action.equals(AudioManager.VIBRATE_SETTING_CHANGED_ACTION) && isPreJellyBean()) {
                        int vibrateSetting = intent.getIntExtra(AudioManager.EXTRA_VIBRATE_SETTING,
                                                                AudioManager.VIBRATE_SETTING_ONLY_SILENT);
                        int oldVibrateSetting = Persistence.retrieveIntValue(context, RINGER_VIBRATE_PREVIOUS_SET_STATE);

                        if (LOG_INFO) {
                            Log.i(TAG, "handleSettingChange vibrate mode new value: " +vibrateSetting+" " +
                                                 "old value: "+oldVibrateSetting);
                        }
                        if (vibrateSetting != oldVibrateSetting) {
                            extChange = true;
                        }
                    }
                }
            }
        } else if (obj instanceof String) {
            // obj is is a String. So it is a notification from SettingsService
            String settingName = (String) obj;
            if (settingName.equals(System.VOLUME_RING)) {
                int oldVolume = Persistence.retrieveIntValue(context, RINGER_VOLUME_PREVIOUS_SET_STATE);
                int volume = System.getInt(context.getContentResolver(), System.VOLUME_RING, 0);
                if (LOG_INFO)
                    Log.i(TAG, "handleSettingChange volume new value: " + volume + " old value: " + oldVolume);
                if(oldVolume != volume) {
                    extChange = true;
                }

            } else if (settingName.equals(SettingsConst.System.VOLUME_RING_SPEAKER)) {
                int oldVolume = Persistence.retrieveIntValue(context, RINGER_VOLUME_PREVIOUS_SET_STATE);
                int volume = System.getInt(context.getContentResolver(), SettingsConst.System.VOLUME_RING_SPEAKER, 0);
                if (LOG_INFO)
                    Log.i(TAG, "settingChange volume new : " + volume + " old : " + oldVolume);
                if(oldVolume != volume) {
                    extChange = true;
                }
                
            } else if (settingName.equals(SettingsConst.System.VIBRATE_WHEN_RINGING)) {
                int vibrateSetting = getCurrentVibrateSetting(context);
                int oldVibrateSetting = Persistence.retrieveIntValue(context, RINGER_VIBRATE_PREVIOUS_SET_STATE);
                if (LOG_INFO)
                    Log.i(TAG, "settingChange vibrate new : " + vibrateSetting +" old : "+oldVibrateSetting);
                if (vibrateSetting != oldVibrateSetting) {
                    extChange = true;
                }
            }
        }
        if(extChange) {
            Persistence.commitValue(context, VIP_RINGER_NO_REVERT_FLAG, true);
        }
        return Status.NO_CHANGE;
    }

    @Override
    public String[] getSettingToObserve() {
        return isPreJellyBean() ?
             new String[] {System.VOLUME_RING} :
             new String[] {
                   SettingsConst.System.VOLUME_RING_SPEAKER,
                   SettingsConst.System.VIBRATE_WHEN_RINGING
               };
    }

    @Override
    public Uri getUriForSetting(String setting) {
        return System.getUriFor(setting);
    }

    @Override
    public String getActionString(Context context) {
        return context.getString(R.string.vip_ringer);
    }

    @Override
    public String getActionKey() {
        return VIP_RINGER_ACTION_KEY;
    }

    @Override
    public String getBroadcastAction() {
        return isPreJellyBean() ?
        AudioManager.RINGER_MODE_CHANGED_ACTION+BROADCAST_ACTION_DELIMITER+AudioManager.VIBRATE_SETTING_CHANGED_ACTION :
        AudioManager.RINGER_MODE_CHANGED_ACTION;
    }

    @Override
    public String getDescription(Context context, Intent configIntent) {
        String names = configIntent.getStringExtra(EXTRA_NAME);
        int volumeLevel = 0;
        double version = configIntent.getDoubleExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        if (version == INITIAL_VERSION) {
            int maxVolume = getMaxRingerVolume(context);
            int ringerVolume = configIntent.getIntExtra(EXTRA_RINGER_VOLUME, maxVolume);
            volumeLevel = convertRingerVolumeToVolumeLevel(ringerVolume, maxVolume);
        } else {
            volumeLevel = configIntent.getIntExtra(EXTRA_VOLUME_LEVEL, VOLUME_LEVEL_MAX); 
        }
        
        return getDescription(context, names, StringUtils.COMMA_STRING, volumeLevel);
    }

    @Override
    public boolean isConfigUpdated(Context context, Intent configIntent) {
        double version = configIntent.getDoubleExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        return (version <= INITIAL_VERSION);
    }

    @Override
    public String getUpdatedConfig(Context context, Intent configIntent) {
        int volumeLevel = convertRingerVolumeToVolumeLevel(configIntent.getIntExtra(EXTRA_RINGER_VOLUME, 0),
                                                 getMaxRingerVolume(context));
        return getConfig(configIntent.getStringExtra(EXTRA_INTERNAL_NAME),
                         configIntent.getStringExtra(EXTRA_NUMBER),
                         configIntent.getStringExtra(EXTRA_NAME),
                         volumeLevel,
                         configIntent.getBooleanExtra(EXTRA_VIBE_STATUS, false),
                         configIntent.getStringExtra(EXTRA_KNOWN_FLAG));
    }

    @Override
    public List<String> getConfigList(Context context) {
        return new ArrayList<String>();
        
    }

    @Override
    public List<String> getDescriptionList(Context context) {
        return new ArrayList<String>();
    }
    

    /** Returns the current config version
     * 
     * @return
     */
    public static double getConfigVersion() {
        return CONFIG_VERSION;
    }
    
    /** Returns the base version of the config
     * 
     * @return
     */
    public static double getInitialVersion() {
        return INITIAL_VERSION;
    }
    
    /**
     * Method to change the ringer settings
     * This is done at the time of incoming call from a VIP number
     * @param context Caller context
     * @param intent Intent containing information about the ringer settings to be set
     */
    public void changeRingerSettings (Context context, Intent intent) {
        if (LOG_DEBUG) Log.d(TAG, "changeRingerSettings");
        AudioManager audioManager = (AudioManager)context.getSystemService(Activity.AUDIO_SERVICE);

        mInternalName = intent.getStringExtra(EXTRA_INTERNAL_NAME);
        mRingerMode = intent.getIntExtra(EXTRA_RINGER_MODE, audioManager.getRingerMode());
        boolean vibCheckBoxStatus = intent.getBooleanExtra(EXTRA_VIBE_STATUS, false);

        audioManager.setRingerMode(mRingerMode);
        int ringerVolume = 0;
        if (mRingerMode == AudioManager.RINGER_MODE_NORMAL) {
            ringerVolume = intent.getIntExtra(EXTRA_RINGER_VOLUME,
                                              audioManager.getStreamVolume(AudioManager.STREAM_RING));
            audioManager.setStreamVolume(AudioManager.STREAM_RING, ringerVolume, 0);
        }

        int vibrateSetting = getVibrateSetting(vibCheckBoxStatus);
        if ( vibrateSetting != getCurrentVibrateSetting(context)) {
            setVibrateSetting(context, vibrateSetting);
        }
        
        if (LOG_INFO) Log.i(TAG, "Vibrate: "+ vibrateSetting + " Volume: "+ringerVolume);
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put(RINGER_MODE_PREVIOUS_SET_STATE, mRingerMode);
        map.put(RINGER_VIBRATE_PREVIOUS_SET_STATE, vibrateSetting);
        map.put(RINGER_VOLUME_PREVIOUS_SET_STATE, ringerVolume);
        Persistence.commitValues(context, map);
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
    public static String getDescription (Context context, String names, String delimiter, int volumeLevel) {
        StringBuilder description = new StringBuilder();
        if (names != null) {
            String[] namesArr = names.split(delimiter);
            if (namesArr.length > 1) {
                description.append(namesArr[0]).append(SPACE).append(PLUS)
                .append(SPACE).append(namesArr.length - 1);
            } else {
                description.append(namesArr[0]);
            }
        }
        description.append(NEW_LINE).append(getRingerVolumeString(context, volumeLevel))
        .append(SPACE).append(context.getString(R.string.volume));
        return description.toString();
    }
    
    /**
     * Method to return a config based on input parameters
     *
     * @param internalName Internal name of the entry
     * @param numbers VIP numbers
     * @param names Contact names
     * @param level volume level to set
     * @param vibeStatus Vibrate status to set
     * @param knownFlags Whether a contact is known or unknown at the time of rule creation
     * @return Config
     */
    public static String getConfig(String internalName, String numbers, String names, int volumeLevel,
            boolean vibeStatus, String knownFlags) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_VERSION, getConfigVersion());
        intent.putExtra(EXTRA_INTERNAL_NAME, internalName);
        intent.putExtra(EXTRA_NUMBER, numbers);
        intent.putExtra(EXTRA_NAME, names);
        intent.putExtra(EXTRA_VOLUME_LEVEL, volumeLevel);
        intent.putExtra(EXTRA_VIBE_STATUS, vibeStatus);
        intent.putExtra(EXTRA_KNOWN_FLAG, knownFlags);
        return intent.toUri(0);
    }
    
    /**
     * Method to register for Ringer setting changes when the call is active
     * We need to monitor changes only during call.
     * @param context
     */
    public static void registerForSettingChangesDuringCall(Context context) {
        StatefulActionHelper.registerForSettingChanges(context, VIP_RINGER_ACTION_KEY);
    }

    /**
     * Method to stop listening to Ringer setting change events
     * @param context
     */
    public static void deregisterFromSettingChangesDuringCall(Context context) {
        StatefulActionHelper.deregisterFromSettingChanges(context, VIP_RINGER_ACTION_KEY);
    }

    /** Returns true if the phone is on a Pre Jelly Bean software load
     * 
     * @return
     */
    private static final boolean isPreJellyBean() {
        return (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
    }
    
    /** Returns the audio manager vibrate setting corresponding to the vibrate check box status 
     * 
     * @param vibCheckBoxStatus
     * @return
     */
    private static int getVibrateSetting(boolean vibCheckBoxStatus) {
        int vibrateSetting = 0;
        if (isPreJellyBean()) {
            vibrateSetting = (vibCheckBoxStatus) ? AudioManager.VIBRATE_SETTING_ON :
                AudioManager.VIBRATE_SETTING_ONLY_SILENT;
        } else {
            vibrateSetting = (vibCheckBoxStatus) ? SettingsConst.System.VIBRATE_ON
                                                 : SettingsConst.System.VIBRATE_OFF;
        }

        return vibrateSetting;
    }

    /** Returns the current vibrate setting of the phone.
     * 
     * @param context
     * @return
     */
    public static int getCurrentVibrateSetting(Context context) {
        AudioManager audioManager = (AudioManager)context.getSystemService(Activity.AUDIO_SERVICE);
        return isPreJellyBean() ?
                audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER) :
                Settings.System.getInt(context.getContentResolver(),
                      SettingsConst.System.VIBRATE_WHEN_RINGING, SettingsConst.System.DEFAULT_VALUE);
    }

    /** Sets the passed in vibrate setting to Framework.
     * 
     * @param context
     * @param vibrateSetting
     */
    private static void setVibrateSetting(Context context, int vibrateSetting) {
        if (isPreJellyBean()) {
            //setVibrateSetting influences the vibrate setting in normal ringer mode.
            //Ringer mode silent/vibrate governs the vibrate setting in other modes
            AudioManager audioManager = (AudioManager)context.getSystemService(Activity.AUDIO_SERVICE);
            audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, vibrateSetting);

        } else {
            Settings.System.putInt(context.getContentResolver(),
                    SettingsConst.System.VIBRATE_WHEN_RINGING, vibrateSetting);
        }
    }

    /** Returns true if the passed in vibrate setting corresponds to "ON" state of the vibrator. 
     * 
     * @param vibrateSetting
     * @return
     */
    public static boolean isVibrateOn(int vibrateSetting) {
        return isPreJellyBean() ?
               (vibrateSetting == AudioManager.VIBRATE_SETTING_ON) :
               (vibrateSetting == SettingsConst.System.VIBRATE_ON);
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
    
    /** Scales the passed in ringer volume to audio manager supported volume levels. Only used
     *  for backward compatibility with old rules.
     * 
     * @param context
     * @param volume
     * @return
     */
    public static int convertRingerVolume(Context context, int volume) {
        AudioManager audioManager = (AudioManager)context.getSystemService(Activity.AUDIO_SERVICE);
        return 
        ( (volume * audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)) / VipRinger.getMaxRingerVolume(audioManager) );
    }
    
    /** Returns the volume level by extracting it from the passed in intent.
     * 
     * @param intent
     * @param audioManager
     * @return
     */
    public static int getVolumeLevel(Intent intent, AudioManager audioManager) {
        int volumeLevel = 0;
        double version = intent.getDoubleExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        if (version == INITIAL_VERSION) {
            int ringerVolume = intent.getIntExtra(EXTRA_RINGER_VOLUME,
                    audioManager.getStreamVolume(AudioManager.STREAM_RING));
            volumeLevel = convertRingerVolumeToVolumeLevel(ringerVolume, VipRinger.getMaxRingerVolume(audioManager));
        } else {
            volumeLevel = intent.getIntExtra(EXTRA_VOLUME_LEVEL, VOLUME_LEVEL_MAX);
        }
        return volumeLevel;
    }

    /** Returns a ringer volume that can be passed to audio manager.
     * 
     * @param context
     * @param volumeLevel
     * @return
     */
    public static int convertVolumeLevelToRingerVolume(Context context, int volumeLevel) {
        AudioManager audioManager = (AudioManager)context.getSystemService(Activity.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        return (volumeLevel * maxVolume) / 100;
    }

    @Override
    public boolean validateConfig(Intent configIntent) {
        String internalName = configIntent.getStringExtra(EXTRA_INTERNAL_NAME);
        String numbers = configIntent.getStringExtra(EXTRA_NUMBER);
        String names = configIntent.getStringExtra(EXTRA_NAME);
        String knownFlags = configIntent.getStringExtra(EXTRA_KNOWN_FLAG);
        return internalName != null && numbers != null && names != null && knownFlags != null;
    }

    /** Returns a user readable text (in the current locale) that corresponds to the
     *  passed in volume level.
     * 
     * @param context
     * @param volumeLevel
     * @return
     */
    public static String getRingerVolumeString(Context context, int volumeLevel) {

        if (volumeLevel == VOLUME_LEVEL_HIGH) {
            return context.getString(R.string.high);
        } else if (volumeLevel == VOLUME_LEVEL_MEDIUM) {
            return context.getString(R.string.medium);
        } else {
            return context.getString(R.string.max);
        }
    }

    /** Converts ringer volume to volume level
     * 
     * @param ringerVolume
     * @param maxVolume
     * @return
     */
    public static int convertRingerVolumeToVolumeLevel(int ringerVolume, int maxVolume) {
        int highVolume = 3*maxVolume/4;
        int mediumVolume = maxVolume/2;
        if (ringerVolume == highVolume) {
            return VOLUME_LEVEL_HIGH;
        } else if (ringerVolume == mediumVolume) {
            return VOLUME_LEVEL_MEDIUM;
        } else {
            return VOLUME_LEVEL_MAX;
        }
    }

    /** Returns the maximum ringer volume. Only used for backward compatibility - rules
     *  created in the old format. 
     * 
     * @param audioManager
     * @return
     */
    public static int getMaxRingerVolume(Context context) {
        if (isPreJellyBean()) {
            AudioManager audioManager = (AudioManager)context.getSystemService(Activity.AUDIO_SERVICE);
            return audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        } else {
            return VipRinger.MAX_RINGER_VOLUME;
        }
    }
    
    /** Returns the maximum ringer volume. Only used for backward compatibility - rules
     *  created in the old format.  
     * 
     * @param audioManager
     * @return
     */
    public static int getMaxRingerVolume(AudioManager audioManager) {
        if (isPreJellyBean()) {
            return audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        } else {
            return VipRinger.MAX_RINGER_VOLUME;
        }
    }
   
}
