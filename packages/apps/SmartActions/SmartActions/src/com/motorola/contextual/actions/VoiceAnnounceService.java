/*
 * @(#)VoiceAnnounceService.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * rdq478        2011/09/28  IKMAIN-28588      Initial Version
 */

package com.motorola.contextual.actions;

import java.util.HashMap;
import java.util.Locale;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.ContactsContract.PhoneLookup;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.commonutils.*;

/**
 * This class handle the request to announce incoming caller's or sms sender's
 * name. <code><pre>
 *
 * CLASS:
 *     Extends Service.
 *
 * RESPONSIBILITIES:
 *     When received the request:
 *     (1). announce the caller's or sender's name/number, then stop
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class VoiceAnnounceService extends Service implements Constants,
    OnInitListener, OnUtteranceCompletedListener {

    private final static String TAG = TAG_PREFIX
                                      + VoiceAnnounceService.class.getSimpleName();
    private final static String UTTERANCE_ID = "VOICE_ANNOUNCE_";
    private final static String SMS_EXTRA_NAME = "pdus";
    private final static int UTTERANCE_CMPLT_MSG = 1;
    private final static int UTTERANCE_RPT_MSG = 2;
    private static final String KEY_CALL_ACTIVE = "VA_KEY_CALL_ACTIVE";
    private static final long CALLER_ID_READ_REPEAT_DELAY_TIMEOUT = 400;

    private Context mContext = null;
    private TextToSpeech mTextToSpeech = null;
    private AudioManager mAudioManager = null;

    private int mRingerVolume = 0;
    private int mAlarmVolume = 0;
    private int mMusicVolume = 0;
    private int mRingerMode;
    private int mVibrateSetting;

    private int mRequestCount = 0;
    private HashMap<String, RequestData> mRequestTable;
    private boolean mInitialized = false;
    private Handler mHandler;

    @Override
    public void onCreate() {
        if (LOG_INFO) {
            Log.i(TAG, "onCreate entry");
        }
        mContext = getApplicationContext();
        mHandler = new VoiceAnnounceHandler();
        mAudioManager = (AudioManager) mContext
                        .getSystemService(Context.AUDIO_SERVICE);
        mRequestTable = new HashMap<String, VoiceAnnounceService.RequestData>();
        // read volume values
        mRingerVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
        mAlarmVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        mMusicVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mRingerMode = mAudioManager.getRingerMode();
        mVibrateSetting = mAudioManager
                .getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
        mTextToSpeech = new TextToSpeech(this, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (LOG_INFO) {
            Log.i(TAG, "onStartCommand intent = " + intent.toUri(0));
        }
        if (intent.getBooleanExtra(EXTRA_VA_CALL_ACTIVE, false)) {
            Persistence.commitValue(this, KEY_CALL_ACTIVE, true);
        } else if (intent.getBooleanExtra(EXTRA_VA_CALL_IDLE, false)) {
            Persistence.removeBooleanValue(this, KEY_CALL_ACTIVE);
        }
        boolean activeCallState = Persistence.retrieveBooleanValue(this,
                KEY_CALL_ACTIVE);
        if (!activeCallState
                && (intent.getBooleanExtra(EXTRA_VA_READ_TEXT, false) || intent
                        .getBooleanExtra(EXTRA_VA_READ_CALL, false))) {
            mRequestCount++;
            String key = UTTERANCE_ID + mRequestCount;
            mRequestTable.put(key, new RequestData(intent));
            if (mInitialized) {
                processRequests();
            }
        } else {
            if (LOG_INFO) {
                Log.i(TAG,
                        "onStartCommand calling stopSelf, activeCallState = "
                                + activeCallState);
            }
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    /**
     * Method for processing the requests present in mRequestTable
     */
    private void processRequests() {
        if (LOG_DEBUG) {
            Log.d(TAG, "processRequests entry");
        }
        if (mTextToSpeech.isLanguageAvailable(Locale.getDefault()) >= 0) {
            mTextToSpeech.setLanguage(Locale.getDefault());
        } else {
            mTextToSpeech.setLanguage(Locale.US);
        }
        for (int count = 1; count <= mRequestCount; count++) {
            String key = UTTERANCE_ID + count;
            RequestData request = mRequestTable.get(key);
            if (request != null && !request.mProcessed) {
                request.mProcessed = true;
                if (request.mIntent.getBooleanExtra(EXTRA_VA_READ_TEXT, false)) {
                    incomingSmsVoiceAnnounceSetup(key);
                } else if (request.mIntent.getBooleanExtra(EXTRA_VA_READ_CALL,
                           false)) {
                    incomingCallVoiceAnnounceSetup(key);
                }
            }
        }
    }

    /**
     * Data structure for holding the intent and a boolean to identify if the
     * intent is processed or not
     *
     * @author wkh346
     *
     */
    private static class RequestData {
        public Intent mIntent;
        public boolean mProcessed = false;

        public RequestData(Intent intent) {
            mIntent = intent;
        }
    }

    /**
     * This class extends Handler class and implements handleMessage()
     *
     * @author wkh346
     *
     */
    private class VoiceAnnounceHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case UTTERANCE_CMPLT_MSG: {
                Bundle data = msg.getData();
                String utteranceId = data.getString(UTTERANCE_ID);

                if (utteranceId != null) {
                    handleUtteranceComplete(utteranceId);
                }
                return;
            }
            case UTTERANCE_RPT_MSG: {
                Bundle data = msg.getData();
                String utteranceId = data.getString(UTTERANCE_ID);

                if (utteranceId != null) {
                    incomingCallVoiceAnnounceSetup(utteranceId);
                }
                return;
            }
            default: {
                super.handleMessage(msg);
            }
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (LOG_INFO) {
            Log.i(TAG, "onDestroy entry");
        }
        if (mTextToSpeech != null) {
            mTextToSpeech.stop();
            mTextToSpeech.shutdown();
        }
        if (mAudioManager != null) {
            informRingerInterface(mRingerMode, mRingerVolume);
            mAudioManager.setRingerMode(mRingerMode);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                                          mMusicVolume, 0);
            mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM,
                                          mAlarmVolume, 0);
        }
        if (mRequestTable != null) {
            mRequestTable.clear();
        }
    }

    /**
     * This method implements the abstract method of OnInitListener.
     *
     * @param status
     *            - SUCCESS or ERROR
     */
    public void onInit(int status) {
        if (LOG_INFO) {
            Log.i(TAG, "onInit status = " + status);
        }
        if (status == TextToSpeech.SUCCESS) {
            mTextToSpeech.setOnUtteranceCompletedListener(this);
            processRequests();
            mInitialized = true;
        } else {
            stopSelf();
        }
    }

    /**
     * This method implements the abstract method of
     * OnUtteranceCompletedListener.
     *
     * @param utteranceId
     */
    public void onUtteranceCompleted(String utteranceId) {
        if (LOG_INFO) {
            Log.i(TAG, "onUtteranceCompleted entry");
        }
        Bundle data = new Bundle();
        data.putString(UTTERANCE_ID, utteranceId);
        Message msg = null;
        RequestData request = mRequestTable.get(utteranceId);
        if (request != null && 
                request.mIntent.getBooleanExtra(EXTRA_VA_READ_CALL, false) &&
                Persistence.retrieveBooleanValue(mContext, SetVoiceAnnounce.VOICE_ANNOUNCE_REGISTER)) {
            msg = mHandler.obtainMessage(UTTERANCE_RPT_MSG);
            msg.setData(data);
            mHandler.sendMessageDelayed(msg, CALLER_ID_READ_REPEAT_DELAY_TIMEOUT);
        } else {
            msg = mHandler.obtainMessage(UTTERANCE_CMPLT_MSG);
            msg.setData(data);
            mHandler.sendMessage(msg);
        }
    }

    /**
     * Call this method for handling the utteranceComplete callback
     *
     * @param utteranceId
     *            - the unique utteranceId for a request
     */
    private void handleUtteranceComplete(String utteranceId) {
        if (LOG_INFO) {
            Log.i(TAG, "handleUtteranceComplete for utterenceId = "
                  + utteranceId);
        }
        int currentKeyCount = Integer.parseInt(utteranceId.substring(
                UTTERANCE_ID.length(), utteranceId.length()));
        for (int count = 1; count <= currentKeyCount; count++) {
            String key = UTTERANCE_ID + count;
            mRequestTable.remove(key);
        }
        if (mRequestTable.isEmpty()) {
            if (LOG_INFO) {
                Log.i(TAG, "handleUtteranceComplete calling stopSelf");
            }
            stopSelf();
        }
    }

    /**
     * Method for informing the ringer interface that ringer settings are going to be changed
     * @param ringerMode New ringer mode to be set
     * @param ringerVolume New ringer volume to be set
     */
    private void informRingerInterface(int ringerMode, int ringerVolume) {
        Intent ringerIntent = new Intent();
        boolean vibeStatus = (mVibrateSetting == AudioManager.VIBRATE_SETTING_ON) ? true : false;
        ringerIntent.setAction(ACTION_EXT_RINGER_CHANGE);
        ringerIntent.putExtra(EXTRA_RINGER_MODE, ringerMode);
        ringerIntent.putExtra(EXTRA_VIBE_STATUS, vibeStatus);
        ringerIntent.putExtra(EXTRA_RINGER_VOLUME, ringerVolume);

        Volumes ringer = (Volumes)ActionHelper.getAction(this, Volumes.VOLUMES_ACTION_KEY);
        if (ringer != null) {
            ringer.setState(mContext, ringerIntent);
        }
    }
    
    /**
     * Format number in string by adding space before and after number.
     * This helper function is created to workaround TTS phone number readout issue.
     * TTS engine readouts phone number as large numbr which causes readouts overflowed number.
     * Please refer to IKJBREL1-7752 for detail.
     * @param text string to format
     * @return formated string which add space before and after number
     */
    private String numberToSingleDigit(String text) {
    	StringBuilder sb = new StringBuilder();
    	int len = text.length();
    	// determine to add space before number char
    	// If previous char is number or beginning of string,
    	// it does not add space.
    	boolean needFrontSpace = false;
    	for(int i = 0; i < len; i++) {
    		char c = text.charAt(i);
    	    String s1 = Character.toString(c);
    	    if(Character.isDigit(c)) {
    	    	if(needFrontSpace)
    	    		sb.append(" ");
    	    	sb.append(s1 + " ");
    	        needFrontSpace = false;
    	    } else {
    	    	sb.append(s1);
    	        needFrontSpace = true;
    	    }
    	 }
    	return sb.toString();
    }

    /**
     * Perform voice announce setup for incoming call.
     *
     * @param utteranceId
     *            - the unique utteranceId for a request
     */
    private void incomingCallVoiceAnnounceSetup(String utteranceId) {
        RequestData request = mRequestTable.get(utteranceId);
        if (request != null) {
            Intent intent = request.mIntent;
            if (LOG_DEBUG) {
                Log.d(TAG, "incomingCallVoiceAnnounceSetup utteranceId = "
                      + utteranceId + " intent = " + intent.toUri(0));
            }
            Resources res = mContext.getResources();
            String caller = res.getString(R.string.incoming_call)
                            + getContactName(intent.getStringExtra(EXTRA_NUMBER));
            if (mRingerMode == AudioManager.RINGER_MODE_NORMAL) {
            	// IKJBREL1-7752 phone number readouts issue.
            	// Format phone number in title by adding space before and after number
            	// to avoid TTS engine readouts issue
                // Inform Ringer that you are changing Ringer Volume to zero
                startVoiceAnnounce(numberToSingleDigit(caller), utteranceId,
                        TextToSpeech.QUEUE_FLUSH);
            } else {
                removeRequest(utteranceId);
            }
        }
        if (LOG_INFO) {
            Log.i(TAG, "incomingCallVoiceAnnounceSetup utteranceId = "
                    + utteranceId + " mRingerMode = " + mRingerMode
                    + " mAlarmVolume = " + mAlarmVolume + " mMusicVolume = "
                    + mMusicVolume);
        }
    }

    /**
     * Perform voice announce setup for incoming SMS.
     *
     * @param utteranceId
     *            - the unique utteranceId for a request
     */
    private void incomingSmsVoiceAnnounceSetup(String utteranceId) {
        RequestData request = mRequestTable.get(utteranceId);
        if (request != null) {
            TelephonyManager telephonyManager = (TelephonyManager) mContext
                                                .getSystemService(Context.TELEPHONY_SERVICE);
            // If call is running don't announce sms
            if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
                Intent intent = request.mIntent;
                if (LOG_DEBUG) {
                    Log.d(TAG, "incomingSmsVoiceAnnounceSetup utteranceId = "
                          + utteranceId + " intent = " + intent.toUri(0));
                }
                Resources res = mContext.getResources();
                String sender = res.getString(R.string.incoming_sms)
                                + getSender(intent);
                /*
                 * Since voice announce is on same Media Stream as music, so put
                 * the VA in another stream instead, so that we can mute the
                 * music and do the VA. This is helpful when music doesn't pause
                 * when receiving incoming SMS.
                 */
                // We announce only if the ringer mode is normal
                if (mRingerMode == AudioManager.RINGER_MODE_NORMAL) {
                	// IKJBREL1-7752 phone number readouts issue.
                	// Format phone number in title by adding space before and after number
                	// to avoid TTS engine readouts issue
                    // Inform Ringer that you are changing Ringer Volume to zero
                    startVoiceAnnounce(numberToSingleDigit(sender), utteranceId,
                            TextToSpeech.QUEUE_ADD);
                } else {
                    removeRequest(utteranceId);
                }
            } else {
                removeRequest(utteranceId);
            }
        }
        if (LOG_INFO) {
            Log.i(TAG, "incomingSmsVoiceAnnounceSetup utteranceId = "
                    + utteranceId + " mRingerMode = " + mRingerMode
                    + " mAlarmVolume = " + mAlarmVolume + " mMusicVolume = "
                    + mMusicVolume);
        }
    }

    /**
     * This method initiates the voice announce by setting the ringer mode,
     * music and alarm stream volumes to appropriate values
     *
     * @param text
     *            - the text to be announced
     * @param utteranceId
     *            - the unique utterance id
     * @param queueMode
     *            - to specify whether request shall be added to the queue or
     *            old requests shall be flushed
     */
    private void startVoiceAnnounce(String text, String utteranceId,
            int queueMode) {
        int ringerMode = (mVibrateSetting == AudioManager.VIBRATE_SETTING_ON) ? AudioManager.RINGER_MODE_VIBRATE :
            AudioManager.RINGER_MODE_SILENT;
        informRingerInterface(ringerMode, 0);
        mAudioManager.setRingerMode(ringerMode);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, mRingerVolume,
                0);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
        params.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
                String.valueOf(AudioManager.STREAM_ALARM));
        mTextToSpeech.speak(text, queueMode, params);
    }

    /**
     * Method for removing a request from mRequestTable. If mRequestTable
     * becomes empty, stopSelf() is called
     *
     * @param utteranceId
     *            - the unique utteranceId for a request
     */
    private void removeRequest(String utteranceId) {
        if (LOG_INFO) {
            Log.i(TAG, "removeRequest entry utteranceId = " + utteranceId);
        }
        if (mRequestTable != null) {
            mRequestTable.remove(utteranceId);
            if (mRequestTable.isEmpty()) {
                stopSelf();
            }
        }
    }

    /**
     * Search for contact name by phone number.
     *
     * @param number
     * @return name if found, otherwise return number
     */
    private String getContactName(String number) {
        String caller = number;

        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                                       Uri.encode(caller));
        String[] projection = new String[] { PhoneLookup.DISPLAY_NAME };
        Cursor cursor = mContext.getContentResolver().query(uri, projection,
                        null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String name = cursor.getString(0);

                if (name != null && !StringUtils.isEmpty(name)) {
                    caller = name;
                } else {
                    if (LOG_DEBUG)
                        Log.d(TAG, "Could not get contact name, name = " + name);
                }
            } else {
                if (LOG_INFO)
                    Log.i(TAG, "Contact name is not found.");
            }

            cursor.close();
        } else {
            if (LOG_DEBUG)
                Log.d(TAG, "Could not get contact name, cursor is null.");
        }

        return caller;
    }

    /**
     * Get SMS sender name or number if not exist in contact db.
     *
     * @param intent
     * @return sender - name or number
     */
    private String getSender(Intent intent) {
        String sender = "";

        Bundle extras = intent.getExtras();

        if (extras == null) {
            Log.e(TAG,
                  "Error could not get incoming message list, extras = null");
            return sender;
        }

        Object[] smsExtra = (Object[]) extras.get(SMS_EXTRA_NAME);

        if (smsExtra == null) {
            Log.e(TAG,
                  "Error could not get incoming message list, smsExtra = null");
            return sender;
        }

        int length = smsExtra.length - 1;
        SmsMessage message = SmsMessage
                             .createFromPdu((byte[]) smsExtra[length]);

        if (message == null) {
            Log.e(TAG, "Error could not get incoming message, message = null");
            return sender;
        }

        sender = getContactName(message.getOriginatingAddress());

        return sender;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
