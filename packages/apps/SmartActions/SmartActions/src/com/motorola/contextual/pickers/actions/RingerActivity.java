/*
 * @(#)RingerActivity.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * crgw47        2012/06/19 NA                Initial version
 *
 */

package com.motorola.contextual.pickers.actions;

import com.motorola.contextual.actions.ActionHelper;
import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.Utils;
import com.motorola.contextual.actions.Volumes;
import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SeekBar;

/**
 * This class allows the user to select the Ringer setting to be set as part of Rule activation.
 * <code><pre>
 * CLASS:
 *     Extends MultiScreenPickerActivity
 *
 * RESPONSIBILITIES:
 *     Shows a dialog allowing the user to select Ringer & Vibrate Settings.
 *     A preview tone for the ringer loudness is played when user moves the volume slider.
 *     Sends the intent containing the setting to Rules Builder.
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

// cjd - I'm not a fan of making the Activity Runnable, either it should have an internal class which is Runnable or use
//    an AsyncTask, actually, I cannot find a reference which starts this as a thread, perhaps it's changed.
public class RingerActivity extends MultiScreenPickerActivity implements Constants,
    SeekBar.OnSeekBarChangeListener, Runnable {

    private static final String TAG = TAG_PREFIX + RingerActivity.class.getSimpleName();

    private AudioManager mAudioManager;
    private int mMaxRingerVolume;
    // Alarm volume is used to preview the volume.
    private int mMaxAlarmVolume;
    private int mOriginalAlarmVolume;
    private int mCurrentAlarmVolume = 0;

    private int mLastProgress = -1;
    private Handler mHandler = new Handler();
    private View mContentView;
    private ListView mListView;

    // list item indexes && size
    private static int RINGER_VOLUME_IDX = 0;
    private static int VIBRATE_MODE_IDX = RINGER_VOLUME_IDX + 1;
    private static int SILENT_MODE_IDX = VIBRATE_MODE_IDX + 1;
    private static int TOTAL_LISTITEMS = SILENT_MODE_IDX + 1;

    private ListItem[] mListItems;
    private boolean[] mCheckedItems;

    /**
     * interface for defining constants for thresholds
     *
     * @author wkh346
     *
     */
    private static interface Thresholds {
        public static final int MIN = 0;
        public static final int LOW = 30;
        public static final int MEDIUM = 70;
        public static final int HIGH = 99;
//        public static final int MAX = 100;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.ringer_volume_title));
        // cjd - please put this on one line, thanks. Also, this may fall into the Log.d category
        if (LOG_INFO)
            Log.i(TAG, "onCreate called");
        mAudioManager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        mOriginalAlarmVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);

        mContentView = createPicker().getView();
        mListView = (ListView) mContentView.findViewById(R.id.list);

        setContentView(mContentView);
    }

    private Picker createPicker() {
        Picker alert = null;

        mMaxAlarmVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        mMaxRingerVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        mCurrentAlarmVolume = mMaxAlarmVolume / 2;

        // Setup item list to use with picker
        mListItems = new ListItem[TOTAL_LISTITEMS];
        mListItems[RINGER_VOLUME_IDX] = new ListItem(-1, getString(R.string.ringer_volume_title), ListItem.typeFIVE, Integer.valueOf(AudioManager.RINGER_MODE_NORMAL), mMaxAlarmVolume, this);
        mListItems[VIBRATE_MODE_IDX] =  new ListItem(null, getString(R.string.volumes_vibrate), null, ListItem.typeONE, Integer.valueOf(AudioManager.RINGER_MODE_VIBRATE), null);
        mListItems[SILENT_MODE_IDX] = new ListItem(null, getString(R.string.volumes_silent), null, ListItem.typeONE, Integer.valueOf(AudioManager.RINGER_MODE_SILENT), null);

        mCheckedItems = new boolean[mListItems.length];
        for (int i = 0; i <  mCheckedItems.length; i++) {
            mCheckedItems[i] = false;
        }
        mCheckedItems[RINGER_VOLUME_IDX] = true;

        String config = getIntent().getStringExtra(Constants.EXTRA_CONFIG);
        if (config != null) {
            // edit case
            Intent configIntent = ActionHelper.getConfigIntent(config);
            int ringerMode = configIntent.getIntExtra(EXTRA_RINGER_MODE, mAudioManager.getRingerMode());
            int vibSetting = configIntent.getIntExtra(EXTRA_VOL_VIBRATE_SETTING, -1);
            boolean vibrateOn;

            if (vibSetting != -1) {
                // v1.1 (volumes picker) and beyond
                vibrateOn = (vibSetting == 1);
            } else {
                vibrateOn = configIntent.getBooleanExtra(EXTRA_VIBE_STATUS, false);
            }

            setVibratetMode(vibrateOn);
            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                float maxRingerVolume = configIntent.getIntExtra(EXTRA_MAX_RINGER_VOLUME, mMaxRingerVolume);
                float ringerVolumePercent = configIntent.getIntExtra(EXTRA_VOL_RINGER_VOLUME, -1);
                float ringerVolume;

                if (ringerVolumePercent != -1) {
                    // v1.1 and beyond
                    ringerVolume = maxRingerVolume * ringerVolumePercent / 100f;
                } else {
                    ringerVolume = configIntent.getIntExtra(EXTRA_RINGER_VOLUME,
                            mAudioManager.getStreamVolume(AudioManager.STREAM_RING));
                }
                mCurrentAlarmVolume = Math.round((ringerVolume*(float)mMaxAlarmVolume)/maxRingerVolume);
                setVolume(mCurrentAlarmVolume);
                setSilentMode(ringerVolume == 0 && !getVibrateMode());//if volume > 0, not silent. if volume == 0 and is not set to vibrate, set silent mode.

            } else {
                setSilentMode(true);
                setVolume(0);
            }
        } else {
            setVolume(mMaxAlarmVolume / 2);
        }

        Picker.Builder builder = new Picker.Builder(this);
        builder.setTitle(Html.fromHtml(getString(R.string.volumes_prompt)))
        .setOnKeyListener(Utils.sDisableSearchKey)
        .setMultiChoiceItems(mListItems, mCheckedItems, new OnMultiChoiceClickListener() {
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                switch((Integer)mListItems[which].mMode) {
                case AudioManager.RINGER_MODE_NORMAL:
                    mCheckedItems[which] = true;
                    break;

                case AudioManager.RINGER_MODE_VIBRATE:
                    mCheckedItems[which] = isChecked;
                    //Turning on vibrate mode turns off silent mode. Turning off Vibrate mode only turns on silent mode IF ringer is 0
                    if (isChecked && getSilentMode()){
                        setSilentMode(!isChecked);
                    } else if (!isChecked && (getVolume() == 0)){
                        setSilentMode(true);//Note that this is not !isChecked, in other words, this use case does not follow the vibrate/silent toggle functionality
                    }
                    mListView.setItemChecked(VIBRATE_MODE_IDX, getVibrateMode());//Update UI when setting
                    mListView.setItemChecked(SILENT_MODE_IDX, getSilentMode());
                    break;

                case AudioManager.RINGER_MODE_SILENT:
                    mCheckedItems[which] = isChecked;
                    setSilentMode(isChecked);
                    //Turning on silent mode turns off vibrate. Turning off silent, does NOT turn on vibrate
                    if (isChecked && getVibrateMode()){
                        setVibratetMode(!isChecked);
                    }
                    setVolume(getSilentMode() ? 0 : mCurrentAlarmVolume);
                    mListView.setItemChecked(VIBRATE_MODE_IDX, getVibrateMode());//Update UI when setting
                    mListView.setItemChecked(SILENT_MODE_IDX, getSilentMode());
                    break;

                default:
                    mCheckedItems[which] = isChecked;
                    break;
                }
            }
        })
        .setPositiveButton(getString(R.string.iam_done),
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int pos) {
                setResult(RESULT_OK, prepareResultIntent());
                finish();
            }
        });
        alert = builder.create();

        return alert;
    }

    private boolean getSilentMode() {
        return mCheckedItems[SILENT_MODE_IDX];
    }

    private void setSilentMode(boolean value) {
        mCheckedItems[SILENT_MODE_IDX] = value;
    }

    private boolean getVibrateMode() {
        return mCheckedItems[VIBRATE_MODE_IDX];
    }

    private void setVibratetMode(boolean value) {
        mCheckedItems[VIBRATE_MODE_IDX] = value;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onPause() {
        super.onPause();
        // Restore the volume change done during preview
        revertVolume();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, getVolume(), 0);
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            return;
        }

        // cjd - mLastProgress isn't really progress, right, it's mSliderPosition, correct? Can we call it that?
        mLastProgress = progress;
        if (progress > 0) {
            mCurrentAlarmVolume = progress;
        }
    }

    // cjd - if this method is required for an interface, then state that in the comments.
    // cjd - all public methods need comments.
    public void onStartTrackingTouch(SeekBar seekBar) {
        // Not Implemented
    }

    // cjd - all public methods need comments.
    public void onStopTrackingTouch(SeekBar seekBar) {
        //HSHIEH: making this logic clearer..
        if (mLastProgress > 0){
            setSilentMode(false); //Turning silent mode on and off
        } else { //if volume is 0
            if (!getVibrateMode()){//if volume is zero and vibrate is on, silent should not turn on
                setSilentMode(true);
            }
        }
        mListView.setItemChecked(SILENT_MODE_IDX, getSilentMode());//Update UI when setting
        setVolume(getSilentMode() ? 0 : mLastProgress);
        postSetVolume(mLastProgress);
    }

    private void revertVolume() {
        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, mOriginalAlarmVolume, 0);
    }

    private int getVolume() {
        return mListItems[RINGER_VOLUME_IDX].mSeekBarParams.currentProgress;
    }

    private void setVolume(int value) {
        mListItems[RINGER_VOLUME_IDX].mSeekBarParams.currentProgress = value;
    }

    private void postSetVolume(int progress) {
        // Do the volume changing separately to give responsive UI
        mHandler.removeCallbacks(this);
        mHandler.post(this);
    }

    // cjd - I'm not a fan of making an entire activity Runnable. Can this be either
    //  narrowed down as an AsyncTask or to a thread or class which extends runnable?
    public void run() {
        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, mLastProgress, AudioManager.FLAG_PLAY_SOUND);
    }

    /**
     * Prepares the result intent to be returned to Rules Builder via activity result
     */
    private Intent prepareResultIntent() {
        Intent intent = new Intent();
        Intent configIntent = new Intent();
        int ringerMode;
        int ringerVolumePercent = getVolume()*100/mMaxAlarmVolume;
        // Set the notification volume to be the same as the ringer volume.
        int notificationVolumePercent = ringerVolumePercent;

        if (getSilentMode()) {
            if (getVibrateMode()) {
                ringerMode = AudioManager.RINGER_MODE_VIBRATE;
            } else {
                ringerMode = AudioManager.RINGER_MODE_SILENT;
            }
        } else {
            ringerMode = AudioManager.RINGER_MODE_NORMAL;
        }

        // The config intent is a simplified version of what the Volumes picker does.
        configIntent = Volumes.getConfigIntent(ringerVolumePercent,
                                        notificationVolumePercent, VOL_INVALID_VALUE, VOL_INVALID_VALUE,
                                        ringerMode, (getVibrateMode() ? 1 : 0));
        intent.putExtra(EXTRA_CONFIG, configIntent.toUri(0));
        intent.putExtra(EXTRA_DESCRIPTION, Volumes.getDescriptionString(this, configIntent));

        return intent;
    }

    /**
     * This method returns the appropriate threshold string on the basis of
     * current value
     *
     * @param currentValue
     *            - current value in percent
     * @return - appropriate threshold string on the basis of current value
     */
    public static String getThresholdString(Context context, int currentValue) {
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

}
