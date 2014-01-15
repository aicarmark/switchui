package com.motorola.contextual.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.Log;
import android_const.provider.SettingsConst;

/**
 * This class extends {@link StatefulAction} class and provides methods
 * essential for functionality of Volumes action publisher
 *
 * @author wkh346
 *
 */
public class Volumes extends StatefulAction implements Constants {

    /**
     * This specifies Vibrate on setting
     */
    public static final int VIBRATE_ON = 1;

    /**
     * This specifies Vibrate off setting
     */
    public static final int VIBRATE_OFF = 0;

    /**
     * This specifies Don't adjust vibrate setting
     */
    public static final int VIBRATE_DONT_ADJUST = VOL_INVALID_VALUE;

    /**
     * The publisher key for Volumes action publisher
     */
    public static final String VOLUMES_ACTION_KEY = ACTION_KEY_PREFIX
            + "Ringer";

    /**
     * TAG for logging
     */
    private static final String TAG = TAG_PREFIX
            + Volumes.class.getSimpleName();

    /**
     * Key for storing current media volume in persistence
     */
    private static final String KEY_CURRENT_MEDIA_VOL = "key_current_media_vol";

    /**
     * Key for storing current ringer volume in persistence
     */
    private static final String KEY_CURRENT_RINGER_VOL = "key_current_ringer_vol";

    /**
     * Key for storing current notification volume in persistence
     */
    private static final String KEY_CURRENT_NOTIFICATION_VOL = "key_current_notification_vol";

    /**
     * Key for storing current alarm volume in persistence
     */
    private static final String KEY_CURRENT_ALARM_VOL = "key_current_alarm_vol";

    /**
     * Key for storing current ringer mode in persistence
     */
    private static final String KEY_CURRENT_RINGER_MODE = "key_current_ringer_mode";

    /**
     * Key for storing current vibrate setting in persistence
     */
    private static final String KEY_CURRENT_VIBRATE_SETTING = "key_current_vibrate_setting";

    /**
     * Key for storing discard settings change in persistence
     */
    private static final String KEY_DISCARD_SETTINGS_CHANGE = "key_discard_settings_change";

    /**
     * Current media volume
     */
    private int mMediaVolume;

    /**
     * Current ringer volume
     */
    private int mRingerVolume;

    /**
     * Current notification volume
     */
    private int mNotificationVolume;

    /**
     * Current alarm volume
     */
    private int mAlarmVolume;

    /**
     * Current ringer mode
     */
    private int mRingerMode;

    /**
     * Current vibrate setting
     */
    private int mVibrateSetting;

    /**
     * Default media volume
     */
    private int mDefaultMediaVolume;

    /**
     * Default ringer volume
     */
    private int mDefaultRingerVolume;

    /**
     * Default notification volume
     */
    private int mDefaultNotificationVolume;

    /**
     * Default alarm volume
     */
    private int mDefaultAlarmVolume;

    /**
     * Default ringer mode
     */
    private int mDefaultRingerMode;

    /**
     * Default vibrate setting
     */
    private int mDefaultVibrateSetting;

    /**
     * Max ringer volume
     */
    private int mMaxRingerVolume;

    /**
     * Max notification volume
     */
    private int mMaxNotificationVolume;

    /**
     * Max alarm volume
     */
    private int mMaxAlarmVolume;

    /**
     * Max media volume
     */
    private int mMaxMediaVolume;

    /**
     * Thresholds for volume level percentage
     *
     * @author wkh346
     *
     */
    private static interface Thresholds {
        public static final int MIN = 0;
        public static final int LOW = 30;
        public static final int MEDIUM = 70;
        public static final int HIGH = 99;
    }

    @Override
    boolean setState(Context context, Intent configIntent) {
        if (LOG_INFO) {
            Log.i(TAG, "setState configIntent = " + configIntent.toUri(0));
        }
        if (!handleConflictIfAny(context, configIntent)) {
            // Here all configs shall be of Volumes action publisher. If a
            // config of Ringer volume comes here then there is a issue with
            // core since refresh didn't happen for that config
            AudioManager audioManager = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            mMaxRingerVolume = audioManager
                    .getStreamMaxVolume(AudioManager.STREAM_RING);
            mMaxNotificationVolume = audioManager
                    .getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
            mMaxAlarmVolume = audioManager
                    .getStreamMaxVolume(AudioManager.STREAM_ALARM);
            mMaxMediaVolume = audioManager
                    .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            boolean restoreDefault = configIntent.getBooleanExtra(
                    EXTRA_RESTORE_DEFAULT, false);
            boolean saveDefault = configIntent.getBooleanExtra(
                    EXTRA_SAVE_DEFAULT, false);
            mRingerVolume = getRoundedVolume(configIntent.getIntExtra(
                    EXTRA_VOL_RINGER_VOLUME, VOL_INVALID_VALUE),
                    mMaxRingerVolume);
            mNotificationVolume = getRoundedVolume(configIntent.getIntExtra(
                    EXTRA_VOL_NOTIFICATION_VOLUME, VOL_INVALID_VALUE),
                    mMaxNotificationVolume);
            mAlarmVolume = getRoundedVolume(configIntent.getIntExtra(
                    EXTRA_VOL_ALARM_VOLUME, VOL_INVALID_VALUE), mMaxAlarmVolume);
            mMediaVolume = getRoundedVolume(configIntent.getIntExtra(
                    EXTRA_VOL_MEDIA_VOLUME, VOL_INVALID_VALUE), mMaxMediaVolume);
            mRingerMode = configIntent.getIntExtra(EXTRA_VOL_RINGER_MODE,
                    VOL_INVALID_VALUE);
            mVibrateSetting = configIntent.getIntExtra(
                    EXTRA_VOL_VIBRATE_SETTING, VOL_INVALID_VALUE);
            if (!restoreDefault) {
                fireAction(context, saveDefault, audioManager);
            } else {
                revertAction(context, audioManager);
            }
        }
        return true;
    }

    /**
     * This method handles the conflicts with other action publishers
     *
     * @param context
     *            - application's context
     * @param configIntent
     *            - the config intent
     * @return - true if conflict is handled, false otherwise
     */
    private boolean handleConflictIfAny(Context context, Intent configIntent) {
        boolean result = false;
        String intentAction = configIntent.getAction();
        if (ACTION_EXT_RINGER_CHANGE.equals(intentAction)) {
            // This config intent will be Ringer volume config intent so here
            // the values will be absolute values
            result = true;
            int ringerMode = configIntent.getIntExtra(EXTRA_RINGER_MODE,
                    VOL_INVALID_VALUE);
            int ringerVolume = 0;
            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                ringerVolume = configIntent.getIntExtra(EXTRA_RINGER_VOLUME,
                        VOL_INVALID_VALUE);
                // This means that external action, like Voice announce or VIP
                // caller, is going to change ringer volume to some value other
                // than zero so "ignore volume change" check shall be removed
                Persistence.removeBooleanValue(context,
                        KEY_IGNORE_VOLUME_CHANGE);
            } else if (ringerMode == AudioManager.RINGER_MODE_SILENT
                    || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                // This means that external action, like Voice announce, is
                // going to make ringer volume zero and this change shall be
                // ignored and shall not be used for inference
                Persistence
                        .commitValue(context, KEY_IGNORE_VOLUME_CHANGE, true);
            }
            boolean vibrateStatus = configIntent.getBooleanExtra(
                    EXTRA_VIBE_STATUS, false);
            int vibrateSetting = vibrateStatus ? VIBRATE_ON : VIBRATE_OFF;
            HashMap<String, Integer> volumeSettingsMap = new HashMap<String, Integer>();
            if (ringerVolume != VOL_INVALID_VALUE) {
                volumeSettingsMap.put(KEY_CURRENT_RINGER_VOL, ringerVolume);
            }
            if (ringerMode != VOL_INVALID_VALUE) {
                volumeSettingsMap.put(KEY_CURRENT_RINGER_MODE, ringerMode);
            }
            volumeSettingsMap.put(KEY_CURRENT_VIBRATE_SETTING, vibrateSetting);
            Persistence.commitValues(context, volumeSettingsMap);
            printPersistenceData(context, "handleConflictIfAny");
        }
        return result;
    }

    /**
     * This method triggers the action and changes the volume levels to desired
     * values
     *
     * @param context
     *            - application's context
     * @param saveDefault
     *            - if true the default values will be stored in persistence
     * @param audioManager
     *            - reference to {@link AudioManager} instance
     */
    private void fireAction(Context context, boolean saveDefault,
            AudioManager audioManager) {
        // Adjust ringer and notification volume, vibrate setting according to
        // the ringer mode
        adjustOnBasisOfRingerMode();
        if (saveDefault) {
            // If first time a rule with Volumes action is becoming active then
            // these default values need to be set in persistence
            saveDataInPersistence(context, VOL_INVALID_VALUE,
                    VOL_INVALID_VALUE, VOL_INVALID_VALUE, VOL_INVALID_VALUE,
                    VOL_INVALID_VALUE, VOL_INVALID_VALUE);
            mDefaultMediaVolume = audioManager
                    .getStreamVolume(AudioManager.STREAM_MUSIC);
            mDefaultRingerVolume = audioManager
                    .getStreamVolume(AudioManager.STREAM_RING);
            mDefaultNotificationVolume = audioManager
                    .getStreamVolume(AudioManager.STREAM_NOTIFICATION);
            mDefaultAlarmVolume = audioManager
                    .getStreamVolume(AudioManager.STREAM_ALARM);
            mDefaultRingerMode = audioManager.getRingerMode();
            // Convert actual vibrate setting to custom vibrate setting
            if (isPreJellyBean()) {
                mDefaultVibrateSetting = (audioManager
                        .getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER) == AudioManager.VIBRATE_SETTING_ON) ? VIBRATE_ON
                        : VIBRATE_OFF;
            } else {
                mDefaultVibrateSetting = Settings.System.getInt(
                        context.getContentResolver(),
                        SettingsConst.System.VIBRATE_WHEN_RINGING,
                        SettingsConst.System.DEFAULT_VALUE);
                ;
            }
           
            printValuesObtainedFromAudioManager(context, audioManager, "fireAction");
            adjustOnBasisOfDefaultSettings();
        }
        // Here VOL_INVALID_VALUE signifies that user did not select the
        // particular setting in volume control dialog while creating the rule
        // or default setting has same value so there is no need to change
        setVolumesAndSetting(context, audioManager, mRingerMode, mRingerVolume,
                mNotificationVolume, mAlarmVolume, mMediaVolume,
                mVibrateSetting);
        // Here just update shall happen. If a rule is becoming active on top of
        // the previous ones and this rule doesn't alter some of the volumes
        // altered by the previous rules then the values stored by previous
        // rules shall not be overwritten
        updateDataInPersistence(context, mRingerVolume, mNotificationVolume,
                mAlarmVolume, mMediaVolume, mRingerMode, mVibrateSetting);
        printPersistenceData(context, "fireAction");
    }

    /**
     * This method sets the volumes and setting to audio manager
     *
     * @param context
     *            - applicatoin's context
     * @param audioManager
     *            - reference to AudioManager
     * @param ringerMode
     *            - the ringer mode
     * @param ringerVolume
     *            - the ringer volume
     * @param notificationVolume
     *            - the notification volume
     * @param alarmVolume
     *            - the alarm volume
     * @param mediaVolume
     *            - the media volume
     * @param vibrateSetting
     *            - the vibrate setting
     */
    private static void setVolumesAndSetting(Context context,
            AudioManager audioManager, int ringerMode, int ringerVolume,
            int notificationVolume, int alarmVolume, int mediaVolume,
            int vibrateSetting) {
        if (mediaVolume != VOL_INVALID_VALUE) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    mediaVolume, 0);
        }
        if (alarmVolume != VOL_INVALID_VALUE) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
                    alarmVolume, 0);
        }
        // Always set ringer mode before ringer volume and notification volume
        if (ringerMode != VOL_INVALID_VALUE) {
            audioManager.setRingerMode(ringerMode);
            if (ringerMode == AudioManager.RINGER_MODE_SILENT
                    || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                // If ringer mode has been set to silent or vibrate then setting
                // ringer volume to 0 makes phone app to change the ringer mode.
                // Hence do not set ringer volume and notification volume in
                // this case
                ringerVolume = VOL_INVALID_VALUE;
                notificationVolume = VOL_INVALID_VALUE;
            }
        }
        if (ringerVolume != VOL_INVALID_VALUE) {
            audioManager.setStreamVolume(AudioManager.STREAM_RING,
                    ringerVolume, 0);
        }
        if (notificationVolume != VOL_INVALID_VALUE) {
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION,
                    notificationVolume, 0);
        }
        if (vibrateSetting != VOL_INVALID_VALUE) {
            if (isPreJellyBean()) {
                // Convert custom vibrate setting to actual vibrate setting
                int actualVibrateSetting = (vibrateSetting == VIBRATE_ON) ? AudioManager.VIBRATE_SETTING_ON
                        : AudioManager.VIBRATE_SETTING_ONLY_SILENT;
                audioManager.setVibrateSetting(
                        AudioManager.VIBRATE_TYPE_RINGER, actualVibrateSetting);
            } else {
                Settings.System.putInt(context.getContentResolver(),
                        SettingsConst.System.VIBRATE_WHEN_RINGING,
                        vibrateSetting);
            }
        }
        if (LOG_INFO) {
            Log.i(TAG, "setVolumesAndSetting altered values \n ringer mode = "
                    + ringerMode + "\n ringer volume = " + ringerVolume
                    + "\n notification volume = " + notificationVolume
                    + "\n vibrate setting = " + vibrateSetting
                    + "\n alarm volume = " + alarmVolume + "\n media volume = "
                    + mediaVolume);
        }
    }

    /**
     * This method adjusts the volumes on basis of default volume
     */
    private void adjustOnBasisOfDefaultSettings() {
        // Do not alter the settings if they are already in desired state. In
        // this case persistence should reflect that the setting is not altered
        if (mDefaultMediaVolume == mMediaVolume) {
            mMediaVolume = VOL_INVALID_VALUE;
        }
        if (mDefaultAlarmVolume == mAlarmVolume) {
            mAlarmVolume = VOL_INVALID_VALUE;
        }
        // Both of these will be custom setting so no need to convert
        if (mDefaultVibrateSetting == mVibrateSetting) {
            mVibrateSetting = VOL_INVALID_VALUE;
        }
        if (mDefaultRingerMode == mRingerMode) {
            mRingerMode = VOL_INVALID_VALUE;
        }
        /* w30219 : Additional check is needed here to handle
         * changing Ringer Mode to Normal since that will restore
         * previously set volume
         */
        if ((mDefaultRingerVolume == mRingerVolume) && mRingerMode != AudioManager.RINGER_MODE_NORMAL) {
            mRingerVolume = VOL_INVALID_VALUE;
        }
        if (mDefaultNotificationVolume == mNotificationVolume) {
            mNotificationVolume = VOL_INVALID_VALUE;
        }
    }

    /**
     * This method adjsusts ringer volume and notification volume on basis of
     * ringer mode
     */
    private void adjustOnBasisOfRingerMode() {
        // If ringer mode is either silent or vibrate then ringer volume and
        // notification volume will be 0. Hence store 0 in persistence so that
        // handle setting change doesn't think that user has changed ringer
        // volume or notification volume. In this case there is no need to
        // modify vibrate setting
        if (mRingerMode == AudioManager.RINGER_MODE_SILENT
                || mRingerMode == AudioManager.RINGER_MODE_VIBRATE) {
            mRingerVolume = 0;
            mNotificationVolume = 0;
            mVibrateSetting = VOL_INVALID_VALUE;
        }
    }

    /**
     * This method stores values in persistence
     *
     * @param context
     *            - application's context
     * @param ringerVolume
     *            - the ringe volume level
     * @param notificationVolume
     *            - the notification volume level
     * @param alarmVolume
     *            - the alarm volume level
     * @param mediaVolume
     *            - the media volume level
     * @param ringerMode
     *            - the ringer mode
     * @param vibrateSetting
     *            - the vibrate setting
     */
    private void saveDataInPersistence(Context context, int ringerVolume,
            int notificationVolume, int alarmVolume, int mediaVolume,
            int ringerMode, int vibrateSetting) {
        HashMap<String, Integer> volumeSettingsMap = new HashMap<String, Integer>();
        volumeSettingsMap.put(KEY_CURRENT_RINGER_MODE, ringerMode);
        volumeSettingsMap.put(KEY_CURRENT_RINGER_VOL, ringerVolume);
        volumeSettingsMap.put(KEY_CURRENT_NOTIFICATION_VOL, notificationVolume);
        volumeSettingsMap.put(KEY_CURRENT_ALARM_VOL, alarmVolume);
        volumeSettingsMap.put(KEY_CURRENT_MEDIA_VOL, mediaVolume);
        volumeSettingsMap.put(KEY_CURRENT_VIBRATE_SETTING, vibrateSetting);
        Persistence.commitValues(context, volumeSettingsMap);
    }

    /**
     * This method updates data in persistence, if a value is
     * {@link Constants#VOL_INVALID_VALUE} it is not written in persistence
     *
     * @param context
     *            - application's context
     * @param ringerVolume
     *            - ringer volume
     * @param notificationVolume
     *            - notification volume
     * @param alarmVolume
     *            - alarm volume
     * @param mediaVolume
     *            - media volume
     * @param ringerMode
     *            - ringer mode
     * @param vibrateSetting
     *            - vibrate setting
     */
    private void updateDataInPersistence(Context context, int ringerVolume,
            int notificationVolume, int alarmVolume, int mediaVolume,
            int ringerMode, int vibrateSetting) {
        HashMap<String, Integer> volumeSettingsMap = new HashMap<String, Integer>();
        if (ringerMode != VOL_INVALID_VALUE) {
            volumeSettingsMap.put(KEY_CURRENT_RINGER_MODE, ringerMode);
        }
        if (ringerVolume != VOL_INVALID_VALUE) {
            volumeSettingsMap.put(KEY_CURRENT_RINGER_VOL, ringerVolume);
        }
        if (notificationVolume != VOL_INVALID_VALUE) {
            volumeSettingsMap.put(KEY_CURRENT_NOTIFICATION_VOL,
                    notificationVolume);
        }
        if (alarmVolume != VOL_INVALID_VALUE) {
            volumeSettingsMap.put(KEY_CURRENT_ALARM_VOL, alarmVolume);
        }
        if (mediaVolume != VOL_INVALID_VALUE) {
            volumeSettingsMap.put(KEY_CURRENT_MEDIA_VOL, mediaVolume);
        }
        if (vibrateSetting != VOL_INVALID_VALUE) {
            volumeSettingsMap.put(KEY_CURRENT_VIBRATE_SETTING, vibrateSetting);
        }
        Persistence.commitValues(context, volumeSettingsMap);
    }

    /**
     * This method reverts the action by restoring the volume levels
     *
     * @param context
     *            - application's context
     * @param audioManager
     *            - reference to {@link AudioManager} instance
     */
    private void revertAction(Context context, AudioManager audioManager) {
        // Here VOL_INVALID_VALUE signifies that either user did not select this
        // setting in volume control dialog while creating the rule or this
        // setting was modified by user while the rule was active
        if (Persistence.retrieveIntValue(context, KEY_CURRENT_RINGER_MODE) == VOL_INVALID_VALUE) {
            mRingerMode = VOL_INVALID_VALUE;
        }
        if (Persistence.retrieveIntValue(context, KEY_CURRENT_RINGER_VOL) == VOL_INVALID_VALUE) {
            mRingerVolume = VOL_INVALID_VALUE;
        }
        if (Persistence.retrieveIntValue(context, KEY_CURRENT_NOTIFICATION_VOL) == VOL_INVALID_VALUE) {
            mNotificationVolume = VOL_INVALID_VALUE;
        }
        if (Persistence.retrieveIntValue(context, KEY_CURRENT_ALARM_VOL) == VOL_INVALID_VALUE) {
            mAlarmVolume = VOL_INVALID_VALUE;
        }
        if (Persistence.retrieveIntValue(context, KEY_CURRENT_MEDIA_VOL) == VOL_INVALID_VALUE) {
            mMediaVolume = VOL_INVALID_VALUE;
        }
        if (Persistence.retrieveIntValue(context, KEY_CURRENT_VIBRATE_SETTING) == VOL_INVALID_VALUE) {
            mVibrateSetting = VOL_INVALID_VALUE;
        }
        setVolumesAndSetting(context, audioManager, mRingerMode, mRingerVolume,
                mNotificationVolume, mAlarmVolume, mMediaVolume,
                mVibrateSetting);
        saveDataInPersistence(context, VOL_INVALID_VALUE, VOL_INVALID_VALUE,
                VOL_INVALID_VALUE, VOL_INVALID_VALUE, VOL_INVALID_VALUE,
                VOL_INVALID_VALUE);
    }

    /**
     * This method is used for logging and its temporary
     *
     * @param context
     *            - application's context
     * @param caller
     *            - the calling function name
     */
    private static final void printPersistenceData(Context context,
            String caller) {
        if (LOG_INFO) {
            Log.i(TAG,
                    caller
                            + " stored values \n Ringer mode = "
                            + Persistence.retrieveIntValue(context,
                                    KEY_CURRENT_RINGER_MODE)
                            + "\n Ringer volume = "
                            + Persistence.retrieveIntValue(context,
                                    KEY_CURRENT_RINGER_VOL)
                            + "\n Notification volume = "
                            + Persistence.retrieveIntValue(context,
                                    KEY_CURRENT_NOTIFICATION_VOL)
                            + "\n Vibrate setting = "
                            + Persistence.retrieveIntValue(context,
                                    KEY_CURRENT_VIBRATE_SETTING)
                            + "\n Alarm volume = "
                            + Persistence.retrieveIntValue(context,
                                    KEY_CURRENT_ALARM_VOL)
                            + "\n Media volume = "
                            + Persistence.retrieveIntValue(context,
                                    KEY_CURRENT_MEDIA_VOL)
                            + "\n Discard settings changes = "
                            + Persistence.retrieveBooleanValue(context,
                                    KEY_DISCARD_SETTINGS_CHANGE));
        }
    }

    /**
     * This method is used for logging and is temporary
     *
     * @param audioManager
     *            - the audio manager
     * @param caller
     *            - the caller of this method
     */
    private static final void printValuesObtainedFromAudioManager(Context context,
            AudioManager audioManager, String caller) {
        int mediaVolume = audioManager
                .getStreamVolume(AudioManager.STREAM_MUSIC);
        int ringerVolume = audioManager
                .getStreamVolume(AudioManager.STREAM_RING);
        int notificationVolume = audioManager
                .getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        int alarmVolume = audioManager
                .getStreamVolume(AudioManager.STREAM_ALARM);
        int ringerMode = audioManager.getRingerMode();
        // Convert actual vibrate setting to custom vibrate setting
        int vibrateSetting = VIBRATE_OFF;
        if (isPreJellyBean()) {
            vibrateSetting = (audioManager
                    .getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER) == AudioManager.VIBRATE_SETTING_ON) ? VIBRATE_ON
                    : VIBRATE_OFF;
        } else {
            vibrateSetting = Settings.System.getInt(
                    context.getContentResolver(),
                    SettingsConst.System.VIBRATE_WHEN_RINGING,
                    SettingsConst.System.DEFAULT_VALUE);
            ;
        }
        if (LOG_INFO) {
            Log.i(TAG, caller + " default values \n ringer mode = "
                    + ringerMode + "\n ringer volume = " + ringerVolume
                    + "\n notification volume = " + notificationVolume
                    + "\n vibrate setting = " + vibrateSetting
                    + "\n alarm volume = " + alarmVolume + "\n media volume = "
                    + mediaVolume);
        }
    }

    /**
     * This method sets value of {@link #KEY_DISCARD_SETTINGS_CHANGE} to desired
     * value
     *
     * @param context
     *            - application's context
     * @param discard
     *            - true if all setting changes shall be ignored
     */
    public static synchronized void setDiscardSettingsChanges(Context context,
            boolean discard) {
        Persistence.commitValue(context, KEY_DISCARD_SETTINGS_CHANGE, discard);
    }

    /**
     * This method retreives the value of {@link #KEY_DISCARD_SETTINGS_CHANGE}
     *
     * @param context
     *            - application's context
     * @return - true or false
     */
    public static synchronized boolean getDiscardSettingsChanges(Context context) {
        return Persistence.retrieveBooleanValue(context,
                KEY_DISCARD_SETTINGS_CHANGE);
    }

    @Override
    Status handleSettingChange(Context context, Object obj) {
        if (LOG_INFO) {
            Log.i(TAG, "handleSettingChange obj = " + obj);
        }
        if (!getDiscardSettingsChanges(context)) {
            AudioManager audioManager = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            printValuesObtainedFromAudioManager(context, audioManager,
                    "handleSettingChange");
            if (obj instanceof String) {
                HashMap<String, Integer> volumeSettingsMap = new HashMap<String, Integer>();

                // This has been called from settings observer
                String settingName = (String) obj;
                if (settingName.equals(System.VOLUME_MUSIC)
                        || settingName
                                .equals(SettingsConst.System.VOLUME_MUSIC_SPEAKER)
                        || settingName
                                .equals(SettingsConst.System.VOLUME_MUSIC_HEADSET)) {
                    int storedMediaVolume = Persistence.retrieveIntValue(
                            context, KEY_CURRENT_MEDIA_VOL);
                    if (storedMediaVolume != VOL_INVALID_VALUE) {
                        if (audioManager
                                .getStreamVolume(AudioManager.STREAM_MUSIC) != storedMediaVolume) {
                            volumeSettingsMap.put(KEY_CURRENT_MEDIA_VOL,
                                    VOL_INVALID_VALUE);
                        }
                    }
                } else if (settingName.equals(System.VOLUME_RING)
                        || settingName
                                .equals(SettingsConst.System.VOLUME_RING_SPEAKER)) {
                    // Ringer volume changed will be received either if ringer
                    // volume is changed or ringer mode is changed
                    int storedRingerVolume = Persistence.retrieveIntValue(
                            context, KEY_CURRENT_RINGER_VOL);
                    if (storedRingerVolume != VOL_INVALID_VALUE) {
                        if (audioManager
                                .getStreamVolume(AudioManager.STREAM_RING) != storedRingerVolume) {
                            // If ringer volume has changed then ringer mode,
                            // and vibrate setting should not be reverted and
                            // state change shall be send to core. This is done
                            // to mimic the behavior of old Ringer volume action
                            volumeSettingsMap.put(KEY_CURRENT_RINGER_VOL,
                                    VOL_INVALID_VALUE);
                            volumeSettingsMap.put(KEY_CURRENT_RINGER_MODE,
                                    VOL_INVALID_VALUE);
                            volumeSettingsMap.put(KEY_CURRENT_VIBRATE_SETTING,
                                    VOL_INVALID_VALUE);
                        }
                    }
                } else if (settingName.equals(System.VOLUME_NOTIFICATION)) {
                    // Notification volume changed will be received either if
                    // notification volume is changed or ringer mode is changed
                    int storedNotificationVolume = Persistence
                            .retrieveIntValue(context,
                                    KEY_CURRENT_NOTIFICATION_VOL);
                    if (storedNotificationVolume != VOL_INVALID_VALUE) {
                        if (audioManager
                                .getStreamVolume(AudioManager.STREAM_NOTIFICATION) != storedNotificationVolume) {
                            volumeSettingsMap.put(KEY_CURRENT_NOTIFICATION_VOL,
                                    VOL_INVALID_VALUE);
                        }
                    }
                } else if (settingName.equals(System.VOLUME_ALARM)
                        || settingName
                                .equals(SettingsConst.System.VOLUME_ALARM_SPEAKER)) {
                    int storedAlarmVolume = Persistence.retrieveIntValue(
                            context, KEY_CURRENT_ALARM_VOL);
                    if (storedAlarmVolume != VOL_INVALID_VALUE) {
                        if (audioManager
                                .getStreamVolume(AudioManager.STREAM_ALARM) != storedAlarmVolume) {
                            volumeSettingsMap.put(KEY_CURRENT_ALARM_VOL,
                                    VOL_INVALID_VALUE);
                        }
                    }
                } else if (settingName.equals(System.MODE_RINGER)) {
                    int storedRingerMode = Persistence.retrieveIntValue(
                            context, KEY_CURRENT_RINGER_MODE);
                    if (storedRingerMode != VOL_INVALID_VALUE) {
                        if (audioManager.getRingerMode() != storedRingerMode) {
                            // If ringer mode has changed then ringer volume,
                            // notification volume, and vibrate setting should
                            // not be reverted and state change shall be send to
                            // core. This is done to mimic the behavior of old
                            // Ringer volume action
                            volumeSettingsMap.put(KEY_CURRENT_RINGER_MODE,
                                    VOL_INVALID_VALUE);
                            volumeSettingsMap.put(KEY_CURRENT_RINGER_VOL,
                                    VOL_INVALID_VALUE);
                            volumeSettingsMap.put(KEY_CURRENT_NOTIFICATION_VOL,
                                    VOL_INVALID_VALUE);
                            volumeSettingsMap.put(KEY_CURRENT_VIBRATE_SETTING,
                                    VOL_INVALID_VALUE);
                        }
                    }
                } else if (settingName.equals(System.VIBRATE_ON)
                        || settingName
                                .equals(SettingsConst.System.VIBRATE_WHEN_RINGING)) {
                    int storedVibrateSetting = Persistence.retrieveIntValue(
                            context, KEY_CURRENT_VIBRATE_SETTING);
                    if (storedVibrateSetting != VOL_INVALID_VALUE) {
                        // Convert actual vibrate setting to custom vibrate
                        // setting
                        int currentVibrateSetting = VIBRATE_OFF;
                        if (isPreJellyBean()) {
                            currentVibrateSetting = (audioManager
                                    .getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER) == AudioManager.VIBRATE_SETTING_ON) ? VIBRATE_ON
                                    : VIBRATE_OFF;
                        } else {
                            currentVibrateSetting = Settings.System.getInt(
                                    context.getContentResolver(),
                                    SettingsConst.System.VIBRATE_WHEN_RINGING,
                                    SettingsConst.System.DEFAULT_VALUE);
                            ;
                        }
                        if (currentVibrateSetting != storedVibrateSetting) {
                            volumeSettingsMap.put(KEY_CURRENT_VIBRATE_SETTING,
                                    VOL_INVALID_VALUE);
                        }
                    }
                }
                Persistence.commitValues(context, volumeSettingsMap);
            }
        } else {
            if (LOG_INFO) {
                Log.i(TAG, "handleSettingChange discarding settings changes");
            }
        }
        printPersistenceData(context, "handleSettingChange");
        return getCurrentStatus(context);
    }

    /**
     * This method returns the status based upon the values in persistence
     *
     * @param context
     *            - application's context
     * @return - status based upon the values in persistence
     */
    private static final Status getCurrentStatus(Context context) {
        Status status = Status.NO_CHANGE;
        if ((Persistence.retrieveIntValue(context, KEY_CURRENT_RINGER_VOL) == VOL_INVALID_VALUE)
                && (Persistence.retrieveIntValue(context,
                        KEY_CURRENT_RINGER_MODE) == VOL_INVALID_VALUE)
                && (Persistence.retrieveIntValue(context,
                        KEY_CURRENT_VIBRATE_SETTING) == VOL_INVALID_VALUE)
                && (Persistence.retrieveIntValue(context,
                        KEY_CURRENT_NOTIFICATION_VOL) == VOL_INVALID_VALUE)
                && (Persistence
                        .retrieveIntValue(context, KEY_CURRENT_ALARM_VOL) == VOL_INVALID_VALUE)
                && (Persistence
                        .retrieveIntValue(context, KEY_CURRENT_MEDIA_VOL) == VOL_INVALID_VALUE)) {
            status = Status.SUCCESS;
        }
        if (LOG_INFO) {
            Log.i(TAG, "getCurrentStatus returning status " + status);
        }
        return status;
    }

    @Override
    String getSettingString(Context context) {
        // TODO To be done later
        return context.getString(R.string.volumes);
    }

    @Override
    String getDefaultSetting(Context context) {
        // Store all values since user can create more than one rule with Volume
        // control action. In one rule he can select only ringer volume and
        // notification where as in other rule he can select all of them or only
        // one of them. The rule will kick in one on top of another so stream
        // volumes will be changed accordingly
        int ringerVolumePercent = (mDefaultRingerVolume * 100)
                / mMaxRingerVolume;
        int notificationVolumePercent = VOL_INVALID_VALUE;
        if (isPreJellyBean()) {
            notificationVolumePercent = (mDefaultNotificationVolume * 100)
                    / mMaxNotificationVolume;
        }
        int alarmVolumePercent = (mDefaultAlarmVolume * 100) / mMaxAlarmVolume;
        int mediaVolumePercent = (mDefaultMediaVolume * 100) / mMaxMediaVolume;
        return getConfigIntent(ringerVolumePercent, notificationVolumePercent,
                alarmVolumePercent, mediaVolumePercent, mDefaultRingerMode,
                mDefaultVibrateSetting).toUri(0);
    }

    @Override
    String getDefaultSetting(Context context, Intent defaultIntent) {
        String newIntentUri = getUpdatedConfig(context, defaultIntent);
        if (defaultIntent.getDoubleExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION) < VOLUMES_ACTION_VERSION) {
            // This will be called when moving from Ringer volume to Volumes
            // action publisher. If a rule with Ringer volume is active before
            // the upgrade happens then this will be called to obtain the new
            // config for reverting the rule
            int storedRingerMode = Persistence.retrieveIntValue(context,
                    RINGER_MODE_PREVIOUS_SET_STATE);
            int storedRingerVolume = Persistence.retrieveIntValue(context,
                    RINGER_VOLUME_PREVIOUS_SET_STATE);
            if (storedRingerVolume > 0) {
                AudioManager audioManager = (AudioManager) context
                        .getSystemService(Context.AUDIO_SERVICE);
                int maxRingerVolume = audioManager
                        .getStreamMaxVolume(AudioManager.STREAM_RING);
                storedRingerVolume = (storedRingerVolume > maxRingerVolume) ? maxRingerVolume
                        : storedRingerVolume;
            }
            // Convert actual vibrate setting to custom vibrate setting
            int storedVibrateSetting = (Persistence.retrieveIntValue(context,
                    RINGER_VIBRATE_PREVIOUS_SET_STATE) == AudioManager.VIBRATE_SETTING_ON) ? VIBRATE_ON
                    : VIBRATE_OFF;
            saveDataInPersistence(context, storedRingerVolume,
                    VOL_INVALID_VALUE, VOL_INVALID_VALUE, VOL_INVALID_VALUE,
                    storedRingerMode, storedVibrateSetting);
        }
        if (LOG_INFO) {
            Log.i(TAG, "getDefaultSetting oldUri = " + defaultIntent.toUri(0));
            Log.i(TAG, "getDefaultSetting newUri = " + newIntentUri);
        }
        return newIntentUri;
    }

    @Override
    String getActionKey() {
        return VOLUMES_ACTION_KEY;
    }

    @Override
    public String getActionString(Context context) {
        if (LOG_INFO) {
            Log.i(TAG, "getActionString entry");
        }
        return context.getString(R.string.volumes);
    }

    @Override
    public String getDescription(Context context, Intent configIntent) {
        // By this time the config should be updated config
        return getDescriptionString(context, configIntent);
    }

    /**
     * Helper function for getting the description string
     *
     * @param context
     *            - application's context
     * @param configIntent
     *            - the config intent
     * @return - the description string
     */
    public static String getDescriptionString(Context context,
            Intent configIntent) {
        int ringerVolumePercent = configIntent.getIntExtra(
                EXTRA_VOL_RINGER_VOLUME, VOL_INVALID_VALUE);
        int notificationVolumePercent = configIntent.getIntExtra(
                EXTRA_VOL_NOTIFICATION_VOLUME, VOL_INVALID_VALUE);
        int alarmVolumePercent = configIntent.getIntExtra(
                EXTRA_VOL_ALARM_VOLUME, VOL_INVALID_VALUE);
        int mediaVolumePercent = configIntent.getIntExtra(
                EXTRA_VOL_MEDIA_VOLUME, VOL_INVALID_VALUE);
        int ringerMode = configIntent.getIntExtra(EXTRA_VOL_RINGER_MODE,
                VOL_INVALID_VALUE);
        int vibrateSetting = configIntent.getIntExtra(
                EXTRA_VOL_VIBRATE_SETTING, VOL_INVALID_VALUE);
        int count = 0;
        int percentage = VOL_INVALID_VALUE;
        StringBuilder descriptionBuilder = new StringBuilder();
        if (ringerVolumePercent != VOL_INVALID_VALUE
                && ringerMode != VOL_INVALID_VALUE) {
            count++;
            if (percentage == VOL_INVALID_VALUE) {
                percentage = ringerVolumePercent;
                String ringerVolumeText = isPreJellyBean() ? context
                        .getString(R.string.vol_ringer_volume) : context
                        .getString(R.string.vol_ringer_and_notification_volume);
                descriptionBuilder.append(ringerVolumeText).append(SPACE)
                        .append(DASH).append(SPACE);
                if (percentage == 0) {
                    descriptionBuilder.append(context
                            .getString(R.string.vol_silent));
                } else {
                    descriptionBuilder.append(getThresholdString(context,
                            percentage));
                }
                if (vibrateSetting == VIBRATE_ON) {
                    descriptionBuilder.append(SPACE).append(DASH).append(SPACE)
                            .append(context.getString(R.string.vol_vibrate));
                }
            }
        }
        if (notificationVolumePercent != VOL_INVALID_VALUE) {
            count++;
            if (percentage == VOL_INVALID_VALUE) {
                percentage = notificationVolumePercent;
                descriptionBuilder
                        .append(context
                                .getString(R.string.vol_notification_volume))
                        .append(SPACE).append(DASH).append(SPACE)
                        .append(getThresholdString(context, percentage));
            }
        }
        if (alarmVolumePercent != VOL_INVALID_VALUE) {
            count++;
            if (percentage == VOL_INVALID_VALUE) {
                percentage = alarmVolumePercent;
                descriptionBuilder
                        .append(context.getString(R.string.vol_alarm_volume))
                        .append(SPACE).append(DASH).append(SPACE)
                        .append(getThresholdString(context, percentage));
            }
        }
        if (mediaVolumePercent != VOL_INVALID_VALUE) {
            count++;
            if (percentage == VOL_INVALID_VALUE) {
                percentage = mediaVolumePercent;
                descriptionBuilder
                        .append(context.getString(R.string.vol_media_volume))
                        .append(SPACE).append(DASH).append(SPACE)
                        .append(getThresholdString(context, percentage));
            }
        }
        if (count > 1) {
            descriptionBuilder.append(SPACE).append(PLUS).append(SPACE)
                    .append(String.valueOf(count - 1)).append(SPACE)
                    .append(context.getString(R.string.more));
        }
        if (LOG_INFO) {
            Log.i(TAG, "getDescription configIntent = " + configIntent.toUri(0));
            Log.i(TAG,
                    "getDescription description = "
                            + descriptionBuilder.toString());
        }
        return descriptionBuilder.toString();
    }

    /**
     * Helper function for getting the threshold string
     *
     * @param context
     *            - application's context
     * @param currentValue
     *            - current volume level
     * @return - the threshold string
     */
    private static String getThresholdString(Context context, int currentValue) {
        String thresholdString = null;
        if (currentValue == Thresholds.MIN) {
            thresholdString = context.getString(R.string.silent);
        } else if (currentValue <= Thresholds.LOW) {
            thresholdString = context.getString(R.string.low);
        } else if (currentValue <= Thresholds.MEDIUM) {
            thresholdString = context.getString(R.string.medium);
        } else if (currentValue <= Thresholds.HIGH) {
            thresholdString = context.getString(R.string.high);
        } else {
            thresholdString = context.getString(R.string.max);
        }
        return thresholdString;
    }

    @Override
    public String getUpdatedConfig(Context context, Intent configIntent) {
        double version = configIntent.getDoubleExtra(EXTRA_CONFIG_VERSION,
                INITIAL_VERSION);
        String newIntentUri = "";
        if (version < VOLUMES_ACTION_VERSION) {
            // This is old Ringer volume config
            newIntentUri = converRingerVolumeConfigToVolumesConfig(context,
                    configIntent);
        } else {
            // This is ics config configured for notifications volume
            newIntentUri = convertIcsConfigToJbConfig(context, configIntent);
        }
        if (LOG_INFO) {
            Log.i(TAG, "getUpdatedConfig oldUri = " + configIntent.toUri(0));
            Log.i(TAG, "getUpdatedConfig newUri = " + newIntentUri);
        }
        return newIntentUri;
    }

    /**
     * This method converts the ringer volume config to Volumes action publisher
     * config
     *
     * @param context
     *            - application's context
     * @param configIntent
     *            - ringer volume config
     * @return - volumes config
     */
    private static final String converRingerVolumeConfigToVolumesConfig(
            Context context, Intent configIntent) {
        boolean vibCheckBoxStatus = false;
        // Determine the vibrate check box status
        if (configIntent.hasExtra(EXTRA_VIBE_SETTING)) {
            // For backward compatibility. This extra was a part of the default
            // intent. Use the vibrate setting value saved in the default intent
            int vibSetting = configIntent.getIntExtra(EXTRA_VIBE_SETTING,
                    AudioManager.VIBRATE_SETTING_ONLY_SILENT);
            vibCheckBoxStatus = (vibSetting == AudioManager.VIBRATE_SETTING_ON ? true
                    : false);
        } else {
            vibCheckBoxStatus = configIntent.getBooleanExtra(EXTRA_VIBE_STATUS,
                    false);
        }
        int ringerMode = configIntent.getIntExtra(EXTRA_RINGER_MODE,
                AudioManager.RINGER_MODE_NORMAL);
        int ringerVolume = 0;
        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            ringerVolume = configIntent.getIntExtra(EXTRA_RINGER_VOLUME, 0);
            if (ringerVolume == 0) {
                // In GB it is possible to set ringer mode "normal" with volume
                // 0 In ICS ringer volume 0 means "silent" or "vibrate" mode.
                // Convert accordingly
                if (vibCheckBoxStatus) {
                    ringerMode = AudioManager.RINGER_MODE_VIBRATE;
                } else {
                    ringerMode = AudioManager.RINGER_MODE_SILENT;
                }
            }
        } else if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
            if (vibCheckBoxStatus
                    && !configIntent.getBooleanExtra(EXTRA_RESTORE_DEFAULT,
                            false)) {
                // To fix Meeting rule issue It is not possible for the user to
                // configure Ringer action with RINGER_MODE_SILENT and vibe
                // status as true through UI. This can only happen if such a
                // config was saved by a sample rule
                ringerMode = AudioManager.RINGER_MODE_VIBRATE;
            }
        }
        int vibrateSetting = vibCheckBoxStatus ? VIBRATE_ON : VIBRATE_OFF;
        // If ringer volume is zero then ringer volume percent shall also be
        // zero
        int ringerVolumePercent = ringerVolume;
        if (ringerVolume > 0) {
            AudioManager audioManager = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            int maxRingerVolume = configIntent.getIntExtra(
                    EXTRA_MAX_RINGER_VOLUME,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_RING));
            ringerVolumePercent = (ringerVolume >= maxRingerVolume) ? 100
                    : ((ringerVolume * 100) / maxRingerVolume);
        }
        Intent newIntent = getConfigIntent(ringerVolumePercent,
                VOL_INVALID_VALUE, VOL_INVALID_VALUE, VOL_INVALID_VALUE,
                ringerMode, vibrateSetting);
        return newIntent.toUri(0);
    }

    /**
     * This method creates jelly bean config from ics config
     *
     * @param context
     *            - application's context
     * @param configIntent
     *            - ics config
     * @return - config for jelly bean
     */
    private static final String convertIcsConfigToJbConfig(Context context,
            Intent configIntent) {
        String newIntentUri = configIntent.toUri(0);
        if (!isPreJellyBean()
                && configIntent.hasExtra(EXTRA_VOL_NOTIFICATION_VOLUME)) {
            int ringerMode = configIntent.getIntExtra(EXTRA_VOL_RINGER_MODE,
                    VOL_INVALID_VALUE);
            int vibrateSetting = configIntent.getIntExtra(
                    EXTRA_VOL_VIBRATE_SETTING, VOL_INVALID_VALUE);
            int ringerVolumePercent = configIntent.getIntExtra(
                    EXTRA_VOL_RINGER_VOLUME, VOL_INVALID_VALUE);
            int notificationVolumePercent = configIntent.getIntExtra(
                    EXTRA_VOL_NOTIFICATION_VOLUME, VOL_INVALID_VALUE);
            if (ringerVolumePercent == VOL_INVALID_VALUE
                    && notificationVolumePercent != VOL_INVALID_VALUE) {
                // Ringer volume percent and ringer mode have to be interpreted
                // from notification volume and vibrate setting
                ringerVolumePercent = notificationVolumePercent;
                if (ringerVolumePercent > 0) {
                    ringerMode = AudioManager.RINGER_MODE_NORMAL;
                } else if (ringerVolumePercent == 0) {
                    ringerMode = (vibrateSetting == VIBRATE_ON) ? AudioManager.RINGER_MODE_VIBRATE
                            : AudioManager.RINGER_MODE_SILENT;
                }
            }
            newIntentUri = getConfigIntent(
                    ringerVolumePercent,
                    VOL_INVALID_VALUE,
                    configIntent.getIntExtra(EXTRA_VOL_ALARM_VOLUME,
                            VOL_INVALID_VALUE),
                    configIntent.getIntExtra(EXTRA_VOL_MEDIA_VOLUME,
                            VOL_INVALID_VALUE), ringerMode, vibrateSetting)
                    .toUri(0);

        }
        return newIntentUri;
    }

    @Override
    public List<String> getConfigList(Context context) {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getDescriptionList(Context context) {
        return new ArrayList<String>();
    }

    @Override
    void registerForSettingChanges(Context context) {
        StatefulActionHelper.registerForSettingChanges(context,
                VOLUMES_ACTION_KEY);
    }

    @Override
    void deregisterFromSettingChanges(Context context) {
        StatefulActionHelper.deregisterFromSettingChanges(context,
                VOLUMES_ACTION_KEY);
    }

    @Override
    String[] getSettingToObserve() {
        return isPreJellyBean() ? new String[] {
                System.VOLUME_SETTINGS[AudioManager.STREAM_MUSIC],
                System.VOLUME_SETTINGS[AudioManager.STREAM_RING],
                System.VOLUME_SETTINGS[AudioManager.STREAM_NOTIFICATION],
                System.VOLUME_SETTINGS[AudioManager.STREAM_ALARM],
                System.VIBRATE_ON, System.MODE_RINGER } : new String[] {
                SettingsConst.System.VOLUME_RING_SPEAKER,
                SettingsConst.System.VOLUME_ALARM_SPEAKER,
                SettingsConst.System.VOLUME_MUSIC_SPEAKER,
                SettingsConst.System.VOLUME_MUSIC_HEADSET,
                SettingsConst.System.VIBRATE_WHEN_RINGING, System.MODE_RINGER };
    }

    @Override
    Uri getUriForSetting(String setting) {
        return System.getUriFor(setting);
    }

    @Override
    public boolean validateConfig(Intent configIntent) {
        int ringerMode = configIntent.getIntExtra(EXTRA_VOL_RINGER_MODE, VOL_INVALID_VALUE);
        int vibrateMode = configIntent.getIntExtra(EXTRA_VOL_VIBRATE_SETTING, VOL_INVALID_VALUE);
        int ringerVolume = configIntent.getIntExtra(EXTRA_VOL_RINGER_VOLUME, VOL_INVALID_VALUE);
        int mediaVolume = configIntent.getIntExtra(EXTRA_VOL_MEDIA_VOLUME, VOL_INVALID_VALUE);
        int alarmVolume = configIntent.getIntExtra(EXTRA_VOL_ALARM_VOLUME, VOL_INVALID_VALUE);
        int notificationVolume = configIntent.getIntExtra(EXTRA_VOL_NOTIFICATION_VOLUME, VOL_INVALID_VALUE);
        return ringerMode != VOL_INVALID_VALUE || vibrateMode != VOL_INVALID_VALUE || ringerVolume != VOL_INVALID_VALUE ||
                mediaVolume != VOL_INVALID_VALUE || alarmVolume != VOL_INVALID_VALUE ||
                notificationVolume != VOL_INVALID_VALUE;
    }

    /**
     * Helper function for getting the config intent
     *
     * @param ringerVolumePercent
     *            - ringer volume percent
     * @param notificationVolumePercent
     *            - notification volume percent
     * @param alarmVolumePercent
     *            - alarm volume percent
     * @param mediaVolumePercent
     *            - media volume percent
     * @param ringerMode
     *            - ringer mode
     * @param vibrateSetting
     *            - vibrate setting
     * @return - the config intent
     */
    public static final Intent getConfigIntent(int ringerVolumePercent,
            int notificationVolumePercent, int alarmVolumePercent,
            int mediaVolumePercent, int ringerMode, int vibrateSetting) {
        Intent configIntent = new Intent();
        configIntent.putExtra(EXTRA_CONFIG_VERSION, VOLUMES_ACTION_VERSION);
        if (ringerVolumePercent != VOL_INVALID_VALUE) {
            configIntent.putExtra(EXTRA_VOL_RINGER_VOLUME, ringerVolumePercent);
        }
        if (notificationVolumePercent != VOL_INVALID_VALUE) {
            configIntent.putExtra(EXTRA_VOL_NOTIFICATION_VOLUME,
                    notificationVolumePercent);
        }
        if (alarmVolumePercent != VOL_INVALID_VALUE) {
            configIntent.putExtra(EXTRA_VOL_ALARM_VOLUME, alarmVolumePercent);
        }
        if (mediaVolumePercent != VOL_INVALID_VALUE) {
            configIntent.putExtra(EXTRA_VOL_MEDIA_VOLUME, mediaVolumePercent);
        }
        if (ringerMode != VOL_INVALID_VALUE) {
            configIntent.putExtra(EXTRA_VOL_RINGER_MODE, ringerMode);
        }
        if (vibrateSetting != VOL_INVALID_VALUE) {
            configIntent.putExtra(EXTRA_VOL_VIBRATE_SETTING, vibrateSetting);
        }
        return configIntent;
    }

    @Override
    public boolean isConfigUpdated(Context context, Intent configIntent) {
        boolean result = false;
        double configVersion = configIntent.getDoubleExtra(
                EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        if (configVersion < VOLUMES_ACTION_VERSION) {
            // This is old Ringer volume config
            result = true;
        }
        if (!isPreJellyBean()
                && configIntent.hasExtra(EXTRA_VOL_NOTIFICATION_VOLUME)) {
            // We are on jelly bean but config is of ics Volumes AP configured
            // for notification volume
            result = true;
        }
        return result;
    }

    /**
     * This method returns the rounded value for volume
     *
     * @param percentage
     *            - the percent level
     * @param maximumVolume
     *            - max volume level
     * @return - the rounded value for volume
     */
    public static final int getRoundedVolume(int percentage, int maximumVolume) {
        int roundedVolume = VOL_INVALID_VALUE;
        if (percentage != VOL_INVALID_VALUE) {
            float percent = (float) percentage;
            float maxVal = (float) maximumVolume;
            roundedVolume = Math.round((percent * maxVal) / 100);
        }
        return roundedVolume;
    }

    /**
     * This method returns true if build version is lesser than that for jelly
     * bean
     *
     * @return - true or false
     */
    public static final boolean isPreJellyBean() {
        return (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
    }

}
