package com.motorola.contextual.actions;

import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.fragment.EditFragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * This class extends {@link Activity} class and implements {@link Constants},
 * {@link OnSeekBarChangeListener}. This class is responsible for showing the UI
 * and allowing the user to create a rule with Volumes action
 *
 * @author wkh346
 *
 */
public class VolumesActivity extends Activity implements Constants,
        OnSeekBarChangeListener {

    /**
     * Name of the thread in which the sound is played
     */
    private static final String HANDLER_THREAD_NAME = "VOLHandlerThread";

    /**
     * {@link Message#what} for the message that specifies the sound to be
     * played at certain volume level
     */
    private static final int MSG_SET_AND_PLAY = 1;

    /**
     * TAG for logging
     */
    private static final String TAG = TAG_PREFIX
            + VolumesActivity.class.getSimpleName();
    /**
     * Extra used in {@link Bundle} added to the message specified by
     * {@link #MSG_SET_AND_PLAY}
     */
    private static final String VOLUME_LEVEL = "volume_level";

    /**
     * Request code for starting {@link VibrateSettingActivity} for result
     */
    private static final int VIBRATE_SETTING_REQUEST_CODE = 1;

    /**
     * The default percentage
     */
    private static final int DEFAULT_PERCENT = 50;

    /**
     * The thread in which sound is played
     */
    private HandlerThread mHandlerThread;

    /**
     * The handler for posting messages
     */
    private Handler mHandler;

    /**
     * Reference to audio manager
     */
    private AudioManager mAudioManager;

    /**
     * Original alarm volume level
     */
    private int mOrigAlarmVolume;

    /**
     * Current media volume level for which the action is configured
     */
    private int mCurrentMediaVolume;

    /**
     * Current ringer volume level for which the action is configured
     */
    private int mCurrentRingerVolume;

    /**
     * Current notification volume level for which the action is configured
     */
    private int mCurrentNotificationVolume;

    /**
     * Current alarm volume level for which the action is configured
     */
    private int mCurrentAlarmVolume;

    /**
     * Current ringer mode for which the action is configured
     */
    private int mCurrentRingerMode;

    /**
     * Current vibrate setting for which the action is configured
     */
    private int mCurrentVibrateSetting;

    /**
     * Maximum ringer stream volume
     */
    private int mMaxRingerVolume;

    /**
     * Maximum notification stream volume
     */
    private int mMaxNotificationVolume;

    /**
     * Maximum alarm stream volume
     */
    private int mMaxAlarmVolume;

    /**
     * Maximum media stream volume
     */
    private int mMaxMediaVolume;

    /**
     * Boolean that restricts the action bar fragment from getting committed
     * when activity is not in resumed state
     */
    private boolean mDisableActionBar = false;

    /**
     * The {@link ImageView} for media volume icon
     */
    private ImageView mMediaVolumeView;

    /**
     * The {@link ImageView} for ringer volume icon
     */
    private ImageView mRingerVolumeView;

    /**
     * The {@link ImageView} for notification volume icon
     */
    private ImageView mNotificationVolumeView;

    /**
     * The {@link ImageView} for alarm volume icon
     */
    private ImageView mAlarmVolumeView;

    /**
     * The media volume {@link SeekBar}
     */
    private SeekBar mMediaVolumeBar;

    /**
     * The ringer volume {@link SeekBar}
     */
    private SeekBar mRingerVolumeBar;

    /**
     * The notification volume {@link SeekBar}
     */
    private SeekBar mNotificationVolumeBar;

    /**
     * The alarm volume {@link SeekBar}
     */
    private SeekBar mAlarmVolumeBar;

    /**
     * The media volume {@link CheckBox}
     */
    private CheckBox mMediaCheckBox;

    /**
     * The ringer volume {@link CheckBox}
     */
    private CheckBox mRingerCheckBox;

    /**
     * The notification volume {@link CheckBox}
     */
    private CheckBox mNotificationCheckBox;

    /**
     * The alarm volume {@link CheckBox}
     */
    private CheckBox mAlarmCheckBox;

    /**
     * The vibrate setting {@link TextView}
     */
    private TextView mVibrateView;

    /**
     * Layout for {@link #mVibrateView}
     */
    private LinearLayout mVibrateViewLayout;

    /**
     * This identifies the state of action bar
     */
    private ActionBarStatus mActionBarStatus = ActionBarStatus.ACTION_BAR_UNINITIALIZED;

    /**
     * Various possible states of action bar
     *
     * @author wkh346
     *
     */
    private enum ActionBarStatus {
        /**
         * This indicates that action bar is enabled
         */
        ACTION_BAR_ENABLED,

        /**
         * This indicates that action bar is disabled
         */
        ACTION_BAR_DISABLED,

        /**
         * This indicates that action bar is not initialized yet
         */
        ACTION_BAR_UNINITIALIZED
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.volumes_main);
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.volumes);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);
        mHandlerThread.start();
        Looper looper = mHandlerThread.getLooper();
        if (looper != null) {
            mHandler = new VolumeControlHandler(looper, mAudioManager);
        } else {
            mHandler = new VolumeControlHandler(mAudioManager);
        }
        initialize();
    }

    /**
     * This method sets the action bar visibility
     *
     * @param enableSaveButton
     *            - true if action bar has to ben enabled, false otherwise
     */
    protected void setupActionBarItemsVisibility(boolean enableSaveButton) {
        if (mDisableActionBar) {
            return;
        } else if ((enableSaveButton && mActionBarStatus == ActionBarStatus.ACTION_BAR_ENABLED)
                || (!enableSaveButton && mActionBarStatus == ActionBarStatus.ACTION_BAR_DISABLED)) {
            // The action bar is already in desired state
            return;
        }
        int editFragmentOption = EditFragment.EditFragmentOptions.DEFAULT;
        if (enableSaveButton) {
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_ENABLED;
        } else {
            editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_DISABLED;
        }
        // Add menu items from fragment
        Fragment fragment = EditFragment.newInstance(editFragmentOption, false);
        getFragmentManager().beginTransaction()
                .replace(R.id.edit_fragment_container, fragment, null).commit();
        mActionBarStatus = enableSaveButton ? ActionBarStatus.ACTION_BAR_ENABLED
                : ActionBarStatus.ACTION_BAR_DISABLED;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDisableActionBar = true;
        mHandler.removeMessages(MSG_SET_AND_PLAY);
        if (mAudioManager != null) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM,
                    mOrigAlarmVolume, 0);
            if (LOG_INFO) {
                Log.i(TAG, "onPause restored alarm volume to "
                        + mOrigAlarmVolume);
            }
        }
        Volumes.setDiscardSettingsChanges(this, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDisableActionBar = false;
        Volumes.setDiscardSettingsChanges(this, true);
    }

    /**
     * This method initializes the variables in {@link VolumesActivity} class
     */
    private void initialize() {
        boolean mediaSelected = false;
        boolean ringerSelected = false;
        boolean alarmSelected = false;
        boolean notificationSelected = false;
        mOrigAlarmVolume = mAudioManager
                .getStreamVolume(AudioManager.STREAM_ALARM);
        if (LOG_INFO) {
            Log.i(TAG, "initialize original alarm volume is "
                    + mOrigAlarmVolume);
        }
        mMaxRingerVolume = mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_RING);
        mMaxNotificationVolume = mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
        mMaxAlarmVolume = mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_ALARM);
        mMaxMediaVolume = mAudioManager
                .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // These have to either taken out from intent, in edit case, or they
        // shall be max / 2
        Intent configIntent = ActionHelper.getConfigIntent(getIntent()
                .getStringExtra(EXTRA_CONFIG));
        if (configIntent != null) {
            if (LOG_INFO) {
                Log.i(TAG, "initialize config = " + configIntent.toUri(0));
            }
            int currentRingerPercent = configIntent.getIntExtra(
                    EXTRA_VOL_RINGER_VOLUME, VOL_INVALID_VALUE);
            int currentNotificationPercent = configIntent.getIntExtra(
                    EXTRA_VOL_NOTIFICATION_VOLUME, VOL_INVALID_VALUE);
            int currentAlarmPercent = configIntent.getIntExtra(
                    EXTRA_VOL_ALARM_VOLUME, VOL_INVALID_VALUE);
            int currentMediaPercent = configIntent.getIntExtra(
                    EXTRA_VOL_MEDIA_VOLUME, VOL_INVALID_VALUE);
            // Convert the percentages to value
            if (currentRingerPercent != VOL_INVALID_VALUE) {
                ringerSelected = true;
                mCurrentRingerVolume = Volumes.getRoundedVolume(
                        currentRingerPercent, mMaxRingerVolume);
            } else {
                mCurrentRingerVolume = Volumes.getRoundedVolume(
                        DEFAULT_PERCENT, mMaxRingerVolume);
            }
            if (currentNotificationPercent != VOL_INVALID_VALUE) {
                notificationSelected = true;
                mCurrentNotificationVolume = Volumes.getRoundedVolume(
                        currentNotificationPercent, mMaxNotificationVolume);
            } else {
                mCurrentNotificationVolume = Volumes.getRoundedVolume(
                        DEFAULT_PERCENT, mMaxNotificationVolume);
            }
            if (currentAlarmPercent != VOL_INVALID_VALUE) {
                alarmSelected = true;
                mCurrentAlarmVolume = Volumes.getRoundedVolume(
                        currentAlarmPercent, mMaxAlarmVolume);
            } else {
                mCurrentAlarmVolume = Volumes.getRoundedVolume(DEFAULT_PERCENT,
                        mMaxAlarmVolume);
            }
            if (currentMediaPercent != VOL_INVALID_VALUE) {
                mediaSelected = true;
                mCurrentMediaVolume = Volumes.getRoundedVolume(
                        currentMediaPercent, mMaxMediaVolume);
            } else {
                mCurrentMediaVolume = Volumes.getRoundedVolume(DEFAULT_PERCENT,
                        mMaxMediaVolume);
            }
            mCurrentRingerMode = configIntent.getIntExtra(
                    EXTRA_VOL_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL);
            if (configIntent.hasExtra(EXTRA_VIBE_STATUS)) {
                // From Ringer to Volume control
                mCurrentVibrateSetting = configIntent.getBooleanExtra(
                        EXTRA_VIBE_STATUS, false) ? Volumes.VIBRATE_ON
                        : Volumes.VIBRATE_OFF;
            } else {
                mCurrentVibrateSetting = configIntent.getIntExtra(
                        EXTRA_VOL_VIBRATE_SETTING, Volumes.VIBRATE_DONT_ADJUST);
            }
        } else {
            mCurrentRingerVolume = Volumes.getRoundedVolume(DEFAULT_PERCENT,
                    mMaxRingerVolume);
            mCurrentNotificationVolume = Volumes.getRoundedVolume(
                    DEFAULT_PERCENT, mMaxNotificationVolume);
            mCurrentAlarmVolume = Volumes.getRoundedVolume(DEFAULT_PERCENT,
                    mMaxAlarmVolume);
            mCurrentMediaVolume = Volumes.getRoundedVolume(DEFAULT_PERCENT,
                    mMaxMediaVolume);
            mCurrentRingerMode = AudioManager.RINGER_MODE_NORMAL;
            mCurrentVibrateSetting = Volumes.VIBRATE_DONT_ADJUST;
        }
        initializeView(ringerSelected, notificationSelected, alarmSelected,
                mediaSelected);
    }

    /**
     * This method initialzes the views in {@link VolumesActivity}
     *
     * @param ringerSelected
     *            - if true {@link #mRingerCheckBox} will be checked, otherwise
     *            it will be unchecked
     * @param notificationSelected
     *            - if true {@link #mNotificationCheckBox} will be checked,
     *            otherwise unchecked
     * @param alarmSelected
     *            - if true {@link #mAlarmCheckBox} will be checked, otherwise
     *            unchecked
     * @param mediaSelected
     *            - if true {@link #mMediaCheckBox} will be checked, otherwise
     *            unchecked
     */
    private void initializeView(boolean ringerSelected,
            boolean notificationSelected, boolean alarmSelected,
            boolean mediaSelected) {
        // checkboxes
        mMediaCheckBox = (CheckBox) findViewById(R.id.media_volume_checkbox);
        mRingerCheckBox = (CheckBox) findViewById(R.id.ringer_volume_checkbox);
        mNotificationCheckBox = (CheckBox) findViewById(R.id.notification_volume_checkbox);
        mAlarmCheckBox = (CheckBox) findViewById(R.id.alarm_volume_checkbox);
        // seekbars
        mMediaVolumeBar = (SeekBar) findViewById(R.id.media_volume_seekbar);
        mRingerVolumeBar = (SeekBar) findViewById(R.id.ringer_volume_seekbar);
        mNotificationVolumeBar = (SeekBar) findViewById(R.id.notification_volume_seekbar);
        mAlarmVolumeBar = (SeekBar) findViewById(R.id.alarm_volume_seekbar);
        // imageviews
        mMediaVolumeView = (ImageView) findViewById(R.id.media_mute_button);
        mRingerVolumeView = (ImageView) findViewById(R.id.ringer_mute_button);
        mNotificationVolumeView = (ImageView) findViewById(R.id.notification_mute_button);
        mAlarmVolumeView = (ImageView) findViewById(R.id.alarm_mute_button);
        // vibrate view
        mVibrateView = (TextView) findViewById(R.id.vibrate_view);
        mVibrateViewLayout = (LinearLayout) findViewById(R.id.layout_vibrate_view);
        mVibrateViewLayout.setOnClickListener(mVibrateViewClickListener);
        // checkboxes settings
        mMediaCheckBox.setOnClickListener(mVolumeCheckBoxListener);
        mRingerCheckBox.setOnClickListener(mVolumeCheckBoxListener);
        mNotificationCheckBox.setOnClickListener(mVolumeCheckBoxListener);
        mAlarmCheckBox.setOnClickListener(mVolumeCheckBoxListener);
        mMediaCheckBox.setChecked(mediaSelected);
        mRingerCheckBox.setChecked(ringerSelected);
        mNotificationCheckBox.setChecked(notificationSelected);
        mAlarmCheckBox.setChecked(alarmSelected);
        refreshActionBar();
        // seekbars settings
        mMediaVolumeBar.setMax(mMaxMediaVolume);
        mMediaVolumeBar.setProgress(mCurrentMediaVolume);
        mRingerVolumeBar.setMax(mMaxRingerVolume);
        mRingerVolumeBar.setProgress(mCurrentRingerVolume);
        mNotificationVolumeBar.setMax(mMaxNotificationVolume);
        mNotificationVolumeBar.setProgress(mCurrentNotificationVolume);
        mAlarmVolumeBar.setMax(mMaxAlarmVolume);
        mAlarmVolumeBar.setProgress(mCurrentAlarmVolume);
        mMediaVolumeBar.setOnSeekBarChangeListener(this);
        mRingerVolumeBar.setOnSeekBarChangeListener(this);
        mNotificationVolumeBar.setOnSeekBarChangeListener(this);
        mAlarmVolumeBar.setOnSeekBarChangeListener(this);
        // imageviews settings
        mAlarmVolumeView.setImageResource(R.drawable.ic_audio_alarm);
        mMediaVolumeView.setImageResource(R.drawable.ic_audio_vol);
        refreshRingerAndNotificationIcons();
        // vibrate view
        refreshVibrateViewText();
        if (!Volumes.isPreJellyBean()) {
            findViewById(R.id.volume_notification_description_layout)
                    .setVisibility(View.GONE);
            findViewById(R.id.volume_notification_selection_layout)
                    .setVisibility(View.GONE);
            findViewById(R.id.volume_notification_separater).setVisibility(
                    View.GONE);
            ((TextView) findViewById(R.id.volume_ringer_textview))
                    .setText(getString(R.string.volume_ringer_and_notification_description));
        }
    }

    /**
     * This method sets the {@link #mRingerVolumeView} and
     * {@link #mNotificationVolumeView} with appropriate icons
     */
    private void refreshRingerAndNotificationIcons() {
        // These are dependent upon current ringer mode
        int ringerViewResource = R.drawable.ic_audio_ring_notif;
        int notificationViewResource = R.drawable.ic_audio_notification;
        if (mCurrentRingerMode == AudioManager.RINGER_MODE_VIBRATE) {
            ringerViewResource = R.drawable.ic_audio_ring_notif_vibrate;
            notificationViewResource = R.drawable.ic_audio_notification_mute;
        } else if (mCurrentRingerMode == AudioManager.RINGER_MODE_SILENT) {
            ringerViewResource = R.drawable.ic_audio_ring_notif_mute;
            notificationViewResource = R.drawable.ic_audio_notification_mute;
        }
        mRingerVolumeView.setImageResource(ringerViewResource);
        mNotificationVolumeView.setImageResource(notificationViewResource);
    }

    /**
     * This method set {@link #mVibrateView} with appropriate text
     */
    private void refreshVibrateViewText() {
        switch (mCurrentVibrateSetting) {
        case Volumes.VIBRATE_ON: {
            mVibrateView.setText(getString(R.string.vol_turn_vibrate_on));
            break;
        }
        case Volumes.VIBRATE_OFF: {
            mVibrateView.setText(getString(R.string.vol_turn_vibrate_off));
            break;
        }
        case Volumes.VIBRATE_DONT_ADJUST: {
            mVibrateView.setText(getString(R.string.vol_dont_adjut_vibrate));
            break;
        }
        }
    }

    /**
     * This method refreshes action bar and sets {@link #mActionBarStatus} to
     * appropriate state
     */
    private void refreshActionBar() {
        boolean value = true;
        if (!mMediaCheckBox.isChecked() && !mRingerCheckBox.isChecked()
                && !mNotificationCheckBox.isChecked()
                && !mAlarmCheckBox.isChecked()) {
            value = false;
        }
        setupActionBarItemsVisibility(value);
    }

    /**
     * This method updates {@link #mCurrentRingerMode} and invokes
     * {@link #refreshRingerAndNotificationIcons()}
     */
    private void updateCurrentRingerMode() {
        if (mRingerCheckBox.isChecked()) {
            if (mCurrentRingerVolume > 0) {
                mCurrentRingerMode = AudioManager.RINGER_MODE_NORMAL;
            } else {
                mCurrentRingerMode = (mCurrentVibrateSetting == Volumes.VIBRATE_ON) ? AudioManager.RINGER_MODE_VIBRATE
                        : AudioManager.RINGER_MODE_SILENT;
            }
        } else {
            mCurrentRingerMode = AudioManager.RINGER_MODE_NORMAL;
        }
        refreshRingerAndNotificationIcons();
    }

    /**
     * This handles click events on check boxes
     */
    private OnClickListener mVolumeCheckBoxListener = new OnClickListener() {

        public void onClick(View view) {
            switch (view.getId()) {
            case R.id.ringer_volume_checkbox: {
                updateCurrentRingerMode();
                break;
            }
            }
            refreshActionBar();
        }
    };

    /**
     * This handles click events on vibrate view and starts
     * {@link VibrateSettingActivity} for result
     */
    private OnClickListener mVibrateViewClickListener = new OnClickListener() {

        public void onClick(View view) {
            launchVibrateSettingScreen();
        }
    };

    /**
     * This method starts {@link VibrateSettingActivity} for result
     */
    private void launchVibrateSettingScreen() {
        Intent intent = new Intent(this, VibrateSettingActivity.class);
        intent.putExtra(EXTRA_VOL_VIBRATE_SETTING, mCurrentVibrateSetting);
        startActivityForResult(intent, VIBRATE_SETTING_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (LOG_INFO) {
            Log.i(TAG, "onActivityResult requestCode " + requestCode
                    + " resultCode " + resultCode);
        }
        if (requestCode == VIBRATE_SETTING_REQUEST_CODE
                && resultCode == RESULT_OK && data != null) {
            mCurrentVibrateSetting = data.getIntExtra(
                    EXTRA_VOL_VIBRATE_SETTING, mCurrentVibrateSetting);
            refreshVibrateViewText();
            updateCurrentRingerMode();
        }
    }

    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        if (fromUser) {
            int id = seekBar.getId();
            switch (id) {
            case R.id.media_volume_seekbar: {
                mCurrentMediaVolume = progress;
                mMediaCheckBox.setChecked(true);
                setVolumeAndPlay(progress, AudioManager.STREAM_MUSIC);
                break;
            }
            case R.id.alarm_volume_seekbar: {
                mCurrentAlarmVolume = progress;
                mAlarmCheckBox.setChecked(true);
                setVolumeAndPlay(progress, AudioManager.STREAM_ALARM);
                break;
            }
            case R.id.ringer_volume_seekbar: {
                mCurrentRingerVolume = progress;
                mRingerCheckBox.setChecked(true);
                updateCurrentRingerMode();
                setVolumeAndPlay(progress, AudioManager.STREAM_RING);
                break;
            }
            case R.id.notification_volume_seekbar: {
                mCurrentNotificationVolume = progress;
                mNotificationCheckBox.setChecked(true);
                if (!mRingerCheckBox.isChecked()
                        || mCurrentRingerMode == AudioManager.RINGER_MODE_NORMAL) {
                    setVolumeAndPlay(progress, AudioManager.STREAM_NOTIFICATION);
                }
                break;
            }
            }
            refreshActionBar();
        }
    }


    public void onStartTrackingTouch(SeekBar seekBar) {
        // Nothing to be done here

    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        // Nothing to be done here

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            result = true;
            break;
        case R.id.edit_save:
            sendResult(RESULT_OK);
            result = true;
            break;
        case R.id.edit_cancel:
            sendResult(RESULT_CANCELED);
            result = true;
            break;
        }
        return result;
    }

    /**
     * This method posts messages to {@link #mHandler} for playing sound
     *
     * @param volumeLevel
     *            - the volume level
     */
    private void setVolumeAndPlay(int volumeLevel, int stream) {
        mHandler.removeMessages(MSG_SET_AND_PLAY);
        Message msg = mHandler.obtainMessage(MSG_SET_AND_PLAY);
        Bundle data = new Bundle();
        int maxStreamVolume = mMaxAlarmVolume;
        switch (stream) {
        case AudioManager.STREAM_RING: {
            maxStreamVolume = mMaxRingerVolume;
            break;
        }
        case AudioManager.STREAM_NOTIFICATION: {
            maxStreamVolume = mMaxNotificationVolume;
            break;
        }
        case AudioManager.STREAM_MUSIC: {
            maxStreamVolume = mMaxMediaVolume;
            break;
        }
        }
        // Convert volume level for alarm stream
        volumeLevel = (volumeLevel * mMaxAlarmVolume) / maxStreamVolume;
        data.putInt(VOLUME_LEVEL, volumeLevel);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    /**
     * This method sends the result to calling activity
     *
     * @param resultCode
     *            - the result code
     */
    private void sendResult(int resultCode) {
        if (resultCode == RESULT_OK) {
            Intent intent = new Intent();
            int ringerVolumePercent = VOL_INVALID_VALUE;
            int notificationVolumePercent = VOL_INVALID_VALUE;
            int alarmVolumePercent = VOL_INVALID_VALUE;
            int mediaVolumePercent = VOL_INVALID_VALUE;
            int ringerMode = VOL_INVALID_VALUE;
            if (mRingerCheckBox.isChecked()) {
                ringerVolumePercent = (mCurrentRingerVolume * 100)
                        / mMaxRingerVolume;
                ringerMode = mCurrentRingerMode;
            }
            if (mNotificationCheckBox.isChecked()) {
                notificationVolumePercent = (mCurrentNotificationVolume * 100)
                        / mMaxNotificationVolume;
            }
            if (mAlarmCheckBox.isChecked()) {
                alarmVolumePercent = (mCurrentAlarmVolume * 100)
                        / mMaxAlarmVolume;
            }
            if (mMediaCheckBox.isChecked()) {
                mediaVolumePercent = (mCurrentMediaVolume * 100)
                        / mMaxMediaVolume;
            }
            Intent configIntent = Volumes.getConfigIntent(ringerVolumePercent,
                    notificationVolumePercent, alarmVolumePercent,
                    mediaVolumePercent, ringerMode, mCurrentVibrateSetting);
            intent.putExtra(EXTRA_CONFIG, configIntent.toUri(0));
            intent.putExtra(EXTRA_DESCRIPTION,
                    Volumes.getDescriptionString(this, configIntent));
            setResult(RESULT_OK, intent);
            if (LOG_INFO) {
                Log.i(TAG, "sendResult send config = " + configIntent.toUri(0));
            }
        } else {
            setResult(RESULT_CANCELED, null);
        }
        finish();
    }

    /**
     * This class extends {@link Handler} class and provides methods for playing
     * sound in secondary thread
     *
     * @author wkh346
     *
     */
    private static class VolumeControlHandler extends Handler {

        /**
         * Reference to {@link AudioManager} instance
         */
        private AudioManager mAudioManager;

        /**
         * Constructor
         *
         * @param looper
         *            - the secondary thread's looper
         * @param audioManager
         *            - reference to {@link AudioManager} instance
         */
        public VolumeControlHandler(Looper looper, AudioManager audioManager) {
            super(looper);
            mAudioManager = audioManager;
        }

        /**
         * Constructor
         *
         * @param audioManager
         *            - reference to {@link AudioManager} instance
         */
        public VolumeControlHandler(AudioManager audioManager) {
            super();
            mAudioManager = audioManager;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_SET_AND_PLAY: {
                Bundle msgData = msg.getData();
                int volumeLevel = msgData.getInt(VOLUME_LEVEL);
                int flag = (volumeLevel > 0) ? AudioManager.FLAG_PLAY_SOUND : 0;
                mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM,
                        volumeLevel, flag);
                break;
            }
            default: {
                super.handleMessage(msg);
            }
            }
        }

    }

}
