/*
 * Copyright (C) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 */

package com.motorola.acousticwarning.mmcp;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.TypedValue; 
import android.widget.Toast;
import android.os.SystemProperties; 
import android.content.res.Resources;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.xmlpull.v1.*;
import java.io.StringReader;
import com.android.internal.util.XmlUtils;
import org.xml.sax.InputSource;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List; 
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class AcousticWarningService extends Service {

    private static boolean sHeadsetPlug = false;           // Indicates Wireless headset connected
    private static boolean sVolThresholdReached = false;   // Indicates Media Volume Threshold reached
    private static boolean sBluetoothConnected = false;    // Indicates Bluetooth headset connected
    private static boolean sFMRadioRunning = false;        // Indicates FM Radio app is active
    private static boolean sFMVolThresholdReached = false; // Indicates FM Radio Volume Thresold reached
    private static boolean sCountDownStarted = false;      // Indicates Headset connection timer started
    private static boolean sReminderPeriodExpired = false; // Indicates Reminder Period is expired
    private static boolean sPhoneCallStateIdle = true;     // Indicates call state is Idle
    //private static boolean DEBUG = (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static boolean DEBUG =true;
    private Context mContext;

    TypedValue  value = new TypedValue(); // value for reading flex 
    //private static int  sWarningThreshold ; // Default warning threshold level on flex value
    private static int  sMediaWarningThreshold ;
    private static int  sFMWarningThreshold ;
    private static int  sStreamValueFM;
    private static String sBootCompleteStatus;

    private static long REMINDER_PERIOD = 0; //72000000; // 20 hours in milliseconds
    //private static long REMINDER_PERIOD = 600000; // For Testing in milliseconds
    private static long TICK_TIME = 600000;
    private static long sRemTime = 0;               // Remaining time for Reminder period to expire
    
    private static long sConnectTime = 0;           // Headset connect time - elapsed time considered
    private static long sDisconnectTime = 0;        // Headset disconnect time - elapsed time considered
    // sCurrentTime and sHeadsetConnectedTime are required to update the remaining time if the service crashes and restarts
    private static long sCurrentTime = 0;           // Current time - elapsed time considered    
    private static long sHeadsetConnectedTime = 0;         // Headset Connected time to update in shared preference
    private static int AUDIO_MANAGER_STREAM_FM = 10;  
	
    private AudioManager mAudioManager;
    private CountDownTimer mTimer;
    private static final String LOG_TAG = "AcousticWarning";
    
    private static final String ALARM_REFRESH_ACTION = "com.motorola.mmcp.ALARM_REFRESH_ACTION";
    private BroadcastReceiver alarmReceiver;
    private PendingIntent pendingIntent;
    private AlarmManager alarmManager;

    /* Fields stored in Shared Preference :
       IsAcousticEnabled - Boolean to indicate if 35096 feature is turned On / Off (This value is checked in AcousticBootReceiver.java)
       ReminderPeriod    - Duration of headset connection time after which warning shall be displayed
       RemainingTime     - Time left for the timer to expire
     */
    public static final String ACOUSTIC_PREF_NAME       = "acoustic_shared_pref";
    public static final String REMAINING_TIME           = "RemainingTime";
    public static final String HEADSET_CONNECTED_TIME   = "HeadsetConnectedTime";
    public static final String HEADSET_PLUG = "HeadsetPlug";
    public static final String VOLUME_THRESHOLD_REACHED = "VolumeThresholdReached";
    public static final String BLUETOOTH_CONNECTED = "BluetoothConnected";
    public static final String FM_RADIO_RUNNING = "FmRadioRunning";
    public static final String FM_VOLUME_THRESHOLD_REACHED = "FmVolumeThresholdReached";
    public static final String BOOT_COMPLETE_STATUS           = "BootCompleteStatus";
    public static final String REMINDER_EXPIRY_PERIOD   = "ReminderPeriod";
    public static String sFmRadioState = "com.motorola.fmradio.state";
    public static String sFmRadioStateExtra = "state";
	
    public static final String CDA_TAG_ACOUSTICSETTING = "@MOTOFLEX@getAcousticSetting";
	//public static final String CDA_TAG_ACOUSTICSETTING = "@MOTOFLEX@getAcousticLevel";
    public static final String TAG_ACOUSTICSETTING = "AcousticSetting";
    public static final String TAG_DATA_ACOUSTICLEVEL = "AcousticLevel";
	
    public static final String CDA_TAG_ACOUSTIC_LEVEL = "getAcousticLevel";
    public static final String CDA_TAG_FM_STREAM_SETTING = "getAcousticFmStreamTypeValue";
    public static final String CDA_TAG_FM_STATE_INTENT = "getAcousticFmStateIntent";
    public static final String CDA_TAG_FM_STATE_EXTRA = "getAcousticFmStateExtra";
    
    // Receiver to listen to events required for warning display
    private final BroadcastReceiver mReceiver = new AcousticWarningReceiver();

    @Override
    public void onCreate() {
        super.onCreate();

        if (DEBUG) Log.d(LOG_TAG, "35096 Service onCreate");

        sRemTime = getApplicationContext().getSharedPreferences(ACOUSTIC_PREF_NAME,0)
                                          .getLong(REMAINING_TIME,0);
        if (DEBUG) Log.d(LOG_TAG,"Remaining timer period = "+ sRemTime);

        sHeadsetConnectedTime = getApplicationContext().getSharedPreferences(ACOUSTIC_PREF_NAME,0)
                                          .getLong(HEADSET_CONNECTED_TIME,0);
        if (DEBUG) Log.d(LOG_TAG,"Connected timer period = "+ sHeadsetConnectedTime);

	// To read the states of FM,Volume before crash from the shared preference

	sHeadsetPlug = getApplicationContext().getSharedPreferences(ACOUSTIC_PREF_NAME,0)
                                          .getBoolean(HEADSET_PLUG,false);
        if (DEBUG) Log.d(LOG_TAG,"sHeadsetPlug = "+ sHeadsetPlug);

        sVolThresholdReached = getApplicationContext().getSharedPreferences(ACOUSTIC_PREF_NAME,0)
                                          .getBoolean(VOLUME_THRESHOLD_REACHED,false);
        if (DEBUG) Log.d(LOG_TAG,"sVolThresholdReached = "+ sVolThresholdReached);

        sBluetoothConnected = getApplicationContext().getSharedPreferences(ACOUSTIC_PREF_NAME,0)
                                          .getBoolean(BLUETOOTH_CONNECTED,false);
        if (DEBUG) Log.d(LOG_TAG,"sBluetoothConnected = "+ sBluetoothConnected);

        sFMRadioRunning = getApplicationContext().getSharedPreferences(ACOUSTIC_PREF_NAME,0)
                                          .getBoolean(FM_RADIO_RUNNING,false);
        if (DEBUG) Log.d(LOG_TAG,"sFMRadioRunning = "+ sFMRadioRunning);

        sFMVolThresholdReached = getApplicationContext().getSharedPreferences(ACOUSTIC_PREF_NAME,0)
                                          .getBoolean(FM_VOLUME_THRESHOLD_REACHED,false);
        if (DEBUG) Log.d(LOG_TAG,"sFMVolThresholdReached = "+ sFMVolThresholdReached);

		Resources res = getApplicationContext().getResources();
		try {
		    int test = res.getIdentifier("config_apkTest", "bool", "com.motorola.acousticwarning.mmcp");
		    if (test > 0) {
		        REMINDER_PERIOD = 600000; // 20 hours in milliseconds
		    } else {
                REMINDER_PERIOD = 72000000;
		    }
		} catch (Resources.NotFoundException e) {
		    REMINDER_PERIOD = 72000000;
		}
		if (DEBUG) Log.d(LOG_TAG, "REMINDER_PERIOD = " + REMINDER_PERIOD);
        /* The below if statement is required to update the remaining time if the service
	is crashed and restarted again.
	sHeadsetConnectedTime is the headset connected time,which is updated in shared preference*/

        if(sHeadsetConnectedTime > 0) {
	    sCurrentTime = SystemClock.elapsedRealtime();
            if (DEBUG) Log.d(LOG_TAG,"Current time = " + sCurrentTime);
	// the below if check is required for battery removal case
            if(sCurrentTime > sHeadsetConnectedTime){
		sRemTime = sRemTime - (sCurrentTime - sHeadsetConnectedTime);
		if (DEBUG) Log.d(LOG_TAG,"Service is crashed and restarted, new remaining time = " + sRemTime);
            }	
	    updateRemainingTime();
		
        }		
		
	mContext = getApplicationContext();
	//sFmRadioState = AcousticWarningUtil.getValueCDA(mContext, AcousticWarningUtil.TAG_ACOUSTIC_FM_STATE_INTENT);
	sFmRadioState = "NOTUSED";
        if (DEBUG) Log.d(LOG_TAG, "sFmRadioState =" + sFmRadioState);	
		
	//sFmRadioStateExtra = AcousticWarningUtil.getValueCDA(mContext, AcousticWarningUtil.TAG_ACOUSTIC_FM_STATE_EXTRA);
	sFmRadioStateExtra = "NOTUSED";
	if (DEBUG) Log.d(LOG_TAG, "sFmRadioStateExtra = "+ sFmRadioStateExtra);
	
        if (sRemTime == 0) {
            sReminderPeriodExpired = true;
        }
        
         // We get the AlarmManager
	alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
     	if (DEBUG) Log.d(LOG_TAG,"Alarm Manager initialised");	
	// We prepare the pendingIntent for the AlarmManager
	Intent intent = new Intent(ALARM_REFRESH_ACTION);
	pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
     				PendingIntent.FLAG_CANCEL_CURRENT);

        IntentFilter inf = new IntentFilter();
        inf.addAction("android.media.VOLUME_CHANGED_ACTION");
        inf.addAction("android.intent.action.HEADSET_PLUG");
        inf.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        inf.addAction("com.motorola.acousticwarning.START_TIMER");
        inf.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        inf.addAction(Intent.ACTION_SHUTDOWN);
        inf.addAction(sFmRadioState);
        inf.addAction("com.motorola.fmradio.volume_set_done");
        inf.addAction(ALARM_REFRESH_ACTION);
 
        registerReceiver(mReceiver, inf);

        mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        int mVolLevel = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        mContext = getBaseContext();
        
        // Read Media volume index corresponding to below 85db from flex
	if (DEBUG) Log.d(LOG_TAG,"Calling getWarningThresholdValue to get flex value");
        String sWarningThreshold_string= AcousticWarningUtil.getValueCDA(this, AcousticWarningUtil.TAG_ACOUSTIC_LEVEL);

        try {
       	    if(sWarningThreshold_string.indexOf(":") != -1) {
  	        String[] sWarningThresholdDataSplit = new String[2];
	        sWarningThresholdDataSplit = sWarningThreshold_string.split(":");
	        sMediaWarningThreshold = Integer.parseInt(sWarningThresholdDataSplit[0]);
	        sFMWarningThreshold = Integer.parseInt(sWarningThresholdDataSplit[1]);
            }
  	    else {
	        sMediaWarningThreshold = Integer.parseInt(sWarningThreshold_string);
  		sFMWarningThreshold = Integer.parseInt(sWarningThreshold_string);
   	    }
	    if (DEBUG)Log.d(LOG_TAG, " warningthreshold int value for media in getWarningThresholdValue()  "
               +sMediaWarningThreshold);

            if (DEBUG)Log.d(LOG_TAG, " warningthreshold int value for FM in getWarningThresholdValue()  "
               +sFMWarningThreshold);

         } catch (Exception e) {
             if (DEBUG) Log.d(LOG_TAG, " Acoustic exception : sWarningThreshold_string " );
             e.printStackTrace();
             //Set the default values as 11 
             sMediaWarningThreshold = 11;
	     sFMWarningThreshold = 11;
	     // added the toast notification, to notify if flex is not there 
	     Toast.makeText(mContext, "Flex not found", Toast.LENGTH_SHORT).show();
           } 
		
        if (DEBUG) Log.d(LOG_TAG,"Read Flex value for Media from Flex "+sMediaWarningThreshold);
	if (DEBUG) Log.d(LOG_TAG,"Read Flex value for FM from Flex "+sFMWarningThreshold);
        if (DEBUG) Log.d(LOG_TAG,"mVolLevel value before if check "+mVolLevel);

        String sStreamValueFM_string= AcousticWarningUtil.getValueCDA(this,
                                               AcousticWarningUtil.TAG_ACOUSTIC_FM_STREAM_TYPE_VALUE);
		   
        try {                    
       	    sStreamValueFM = Integer.parseInt(sStreamValueFM_string);
            if (DEBUG)Log.d(LOG_TAG, " sStreamValueFM =  "+sStreamValueFM);
        } catch (Exception e) {
            if (DEBUG) Log.d(LOG_TAG, " Acoustic exception : sStreamValueFM_string " );
            e.printStackTrace();
  
        } 
	if (DEBUG) Log.d(LOG_TAG,"Read Flex value from Flex "+sStreamValueFM);

        int mFMVolLevel = mAudioManager.getStreamVolume(sStreamValueFM);

        // Check if volume is greater than warning threshold level on power up
        if (mVolLevel > sMediaWarningThreshold) {
            sVolThresholdReached = true;
	    updateStates(VOLUME_THRESHOLD_REACHED, sVolThresholdReached);
        }

	if (mFMVolLevel > sFMWarningThreshold) {
	    sFMVolThresholdReached = true;
	    updateStates(FM_VOLUME_THRESHOLD_REACHED, sFMVolThresholdReached);
	}

	sBootCompleteStatus = getApplicationContext().getSharedPreferences(ACOUSTIC_PREF_NAME,0)
                                          .getString(BOOT_COMPLETE_STATUS,"BootCompletedUnchecked");
	if (DEBUG) Log.d(LOG_TAG,"BootCompleteStatus = "+ sBootCompleteStatus);
	if(sBootCompleteStatus == "BootCompletedUnchecked") {
	    if(sVolThresholdReached){
	        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, sMediaWarningThreshold, 0);
		sVolThresholdReached = false;
	        updateStates(VOLUME_THRESHOLD_REACHED, sVolThresholdReached);
	    }
	    if(sFMVolThresholdReached) {
	        mAudioManager.setStreamVolume(sStreamValueFM, sFMWarningThreshold, 0);
		sFMVolThresholdReached = false;
	        updateStates(FM_VOLUME_THRESHOLD_REACHED, sFMVolThresholdReached);
	    }
            sBootCompleteStatus = "BootCompletedChecked";
	    updateBootCompleteStatus(BOOT_COMPLETE_STATUS,sBootCompleteStatus);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (DEBUG) Log.d(LOG_TAG, "Service onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
		
    private class AcousticWarningReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (DEBUG) Log.d(LOG_TAG, "Acoustic Receiver: onReceive Enter");
            String action = intent.getAction();

            if (action == null) return;

            // If VOLUME CHANGE event for Media Volume (Music stream) is received
            if (action.equals("android.media.VOLUME_CHANGED_ACTION")) {
                if (DEBUG) Log.d(LOG_TAG, "Volume change event");

                int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
                if (DEBUG) Log.d(LOG_TAG, "Stream type" + streamType);
                if (streamType == AudioManager.STREAM_MUSIC) {
                    if (DEBUG) Log.d(LOG_TAG, "Stream type is Music");
                    int newVolLevel = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, 0);
                    int oldVolLevel = intent.getIntExtra(AudioManager.EXTRA_PREV_VOLUME_STREAM_VALUE, 0);

                    if (DEBUG) {
                        Log.d(LOG_TAG," New Volume Level = " + newVolLevel);
                        Log.d(LOG_TAG," Old Volume Level = " + oldVolLevel);
                    }  

                    if (newVolLevel > sMediaWarningThreshold) {
                        sVolThresholdReached = true;
                    } else {
                        sVolThresholdReached = false;
                    }
                    updateStates(VOLUME_THRESHOLD_REACHED, sVolThresholdReached);
                } 
				
                if (sStreamValueFM > 0 ){
                //if (streamType == AUDIO_MANAGER_STREAM_FM) {
                   if (DEBUG) Log.d(LOG_TAG, "Stream type is FM");
                   int newVolLevel = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, 0);
                   if (DEBUG) {
                       Log.d(LOG_TAG," New Volume Level = " + newVolLevel);
                   }

                   if (newVolLevel > sFMWarningThreshold) {
                       sFMVolThresholdReached = true;
                   } else {
                       sFMVolThresholdReached = false;
                   }
		   updateStates(FM_VOLUME_THRESHOLD_REACHED, sFMVolThresholdReached);
                }
            }

            // If Wired Headset event is received
            if(action.equals("android.intent.action.HEADSET_PLUG")) {
                if (DEBUG) {
                    Log.d(LOG_TAG, "Wired Headset plug event");
                    Log.d(LOG_TAG,"state: " + intent.getIntExtra("state", 0));
                    Log.d(LOG_TAG, "name: " + intent.getStringExtra("name"));
                    Log.d(LOG_TAG, "microphone: " + intent.getIntExtra("microphone", 0));
                }

                sHeadsetPlug = (intent.getIntExtra("state", 0) == 1);
		updateStates(HEADSET_PLUG, sHeadsetPlug);
                if (DEBUG) {
                    Log.d(LOG_TAG, "sHeadsetPlug value for Wire head set event rxd:"+sHeadsetPlug);
                 
                }

                // Start count down timer based on bluetooth headset connection as well
                if (sHeadsetPlug == true && sCountDownStarted == false) {
                    startCountDownTimer();
                }

                // Stop the count down timer if no headsets are connected - wired/ bluetooth
                if (sHeadsetPlug == false && sBluetoothConnected == false && sCountDownStarted == true) {
                    stopCountDownTimer();
                }
            }

            // If Bluetooth Headset connect event is received
            if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) { // IKCBS-2627, 05/12/2011, a20746
                int mBluetoothHeadsetState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0);
                if (DEBUG) {
                    Log.d(LOG_TAG, "Bluetooth HEADSET_STATE_CHANGED_ACTION");
                    Log.d(LOG_TAG, "Bluetooth New State: " + mBluetoothHeadsetState);
                }

                if (mBluetoothHeadsetState == BluetoothProfile.STATE_CONNECTED) {
                    sBluetoothConnected = true;
                    if (sCountDownStarted == false) {
                        startCountDownTimer();
                    }
                } else {
                    sBluetoothConnected = false;
                    // Stop the count down if Wired headset is not plugged
                    if (sCountDownStarted == true && sHeadsetPlug == false) {
                      stopCountDownTimer();
                    }
                }
		updateStates(BLUETOOTH_CONNECTED, sBluetoothConnected);
            }
            
            // If START TIMER EVENT is received from Acoustic Dialog
            if (action.equals("com.motorola.acousticwarning.START_TIMER")) {
                if (DEBUG) Log.d(LOG_TAG,"Event Start Timer");

                sRemTime = getApplicationContext().getSharedPreferences(ACOUSTIC_PREF_NAME,0)
                                                  .getLong(REMINDER_EXPIRY_PERIOD, REMINDER_PERIOD);

                if (DEBUG) Log.d(LOG_TAG,"Remaining time after START_TIMER event = " + sRemTime);

                updateRemainingTime();
                sReminderPeriodExpired = false;

                if (sHeadsetPlug == true || sBluetoothConnected == true) {
                    startCountDownTimer();
                }
            }

            // If Call state change event is received
            if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                if (DEBUG) Log.d(LOG_TAG, "Phone state change event received");

                String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                if ((phoneState != null) && (phoneState.equals("IDLE"))) {
                    if (DEBUG) Log.d(LOG_TAG,"CALL STATE IDLE");
                    sPhoneCallStateIdle = true;
                } else {
                    if (DEBUG) Log.d(LOG_TAG,"Phone is not in  Idle state");
                    sPhoneCallStateIdle = false;
                }
            }

            // If FM Radio state event is received
            if (action.equals(sFmRadioState)) {
			        sFMRadioRunning = getFmRadioState(intent);
			        if (DEBUG)  Log.d(LOG_TAG, "Received FM Radio state, mFMRadioRuning = " + sFMRadioRunning);
	            updateStates(FM_RADIO_RUNNING, sFMRadioRunning);
	    }

            if (action.equals("com.motorola.fmradio.volume_set_done")) {
                if (DEBUG) Log.d(LOG_TAG,"FM Radio volume changed");

                int fmVol = intent.getIntExtra("volume", 0);

                if (DEBUG) Log.d(LOG_TAG, "FM Vol = " + fmVol);

                // If FM Radio volume is greater than warning threshold
                if (fmVol > sFMWarningThreshold) {
                    sFMVolThresholdReached = true;
                } else {
                    sFMVolThresholdReached = false;
                }
		updateStates(FM_VOLUME_THRESHOLD_REACHED, sFMVolThresholdReached);
            }

            // If SHUT DOWN event is received
            if (action.equals(Intent.ACTION_SHUTDOWN)) {
                if (DEBUG)  Log.d(LOG_TAG, "SHUT DOWN event received");

                if (sCountDownStarted == true) {
                    sDisconnectTime = SystemClock.elapsedRealtime();
                    if (DEBUG) Log.d(LOG_TAG,"Disconnect Time = " + sDisconnectTime);

                    sRemTime = sRemTime - (sDisconnectTime - sConnectTime);
                    sCountDownStarted = false;
                }
                updateRemainingTime();
                unregisterReceiver(mReceiver);
            }
            
            if (action.equals(ALARM_REFRESH_ACTION)) {
		// We increment received alarms
            	sReminderPeriodExpired = true;
                sCountDownStarted = false;
                sRemTime = 0;
                updateRemainingTime();
                displayWarningDialog();
	    }

            // Display the warning dialog if the required conditions are met
            displayWarningDialog();
        }
    }

    public void startCountDownTimer() {
        // Start the timer if reminder period is not expired
    	if (DEBUG) Log.d(LOG_TAG,"Entering startCountDownTimer fun() ");
        if (sReminderPeriodExpired == false) {
            // Get elapsed time. If current time is obtained, we need to take care of time/timezone change
            sConnectTime = SystemClock.elapsedRealtime();
            if (DEBUG) Log.d(LOG_TAG,"Headset Connect time = " + sConnectTime);

            sRemTime = getApplicationContext().getSharedPreferences(ACOUSTIC_PREF_NAME, 0)
                                              .getLong(REMAINING_TIME, REMINDER_PERIOD);

            if (DEBUG) Log.d(LOG_TAG,"Remaining time in startCountDownTimer = " + sRemTime);
			
            updateConnectedTime(); // updates the headset connected time in the shared preference 
			
            int startTime = (int) sRemTime;
            // We have to register to AlarmManager
    	    Calendar calendar = Calendar.getInstance();
    	    calendar.setTimeInMillis(System.currentTimeMillis());
    	    calendar.add(Calendar.MILLISECOND, startTime);
    		
    	    // We set a one shot alarm
    	    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar
    				.getTimeInMillis(), pendingIntent);

            sCountDownStarted = true;
        }
        if (DEBUG) Log.d(LOG_TAG,"Leaving startCountDownTimer fun() ");
    }

    public void stopCountDownTimer() {
        
    	if (DEBUG) Log.d(LOG_TAG,"Entering stopCountDownTimer fun() ");
    	// If reminder period is not expired, check disonnect time
        if (sReminderPeriodExpired == false) {
            sDisconnectTime = SystemClock.elapsedRealtime();
            if (DEBUG) Log.d(LOG_TAG,"Disconnect Time = " + sDisconnectTime);

            sRemTime = sRemTime - (sDisconnectTime - sConnectTime);
            if (DEBUG) Log.d(LOG_TAG,"Remaining time = " + sRemTime);
            updateRemainingTime();

            // Cancel the alaram - timer 
            alarmManager.cancel(pendingIntent);
            sCountDownStarted = false;
        }
        if (DEBUG) Log.d(LOG_TAG, " Leaving stopCountDownTimer() ");
    }

    public void displayWarningDialog() {

        if (DEBUG) Log.d(LOG_TAG, "Enter displayWarningDialog");

        // Display the dialog if any of the headsets are connected, Volume threshold is reached
        // and Reminder period is expired. The dialog needs to be displayed if the user is not in
        // voice call. If voice call is active, the dialog shall be displayed once the call ends

        // Please Note : Media volume and FM Radio volume are different and hence separate condition
        // checks are required whether volume threshold levels are reached
        if (DEBUG) Log.d(LOG_TAG,"HeadsetPlug value------->"+sHeadsetPlug); 

	boolean sFmRadioStatus = getFmRadioStatus();
       		
        if (DEBUG) Log.d(LOG_TAG,"Bluetoothconnected  value------->"+sBluetoothConnected); 
        if (DEBUG) Log.d(LOG_TAG,"vol threshold reached value------->"+sVolThresholdReached); 
        if (DEBUG) Log.d(LOG_TAG,"fm radio running value------->"+sFMRadioRunning); 
        if (DEBUG) Log.d(LOG_TAG,"fmvol threshold reached value------->"+sFMVolThresholdReached); 
        if (DEBUG)  Log.d(LOG_TAG,"fm radio running value------->"+sFMRadioRunning); 
        if (DEBUG) Log.d(LOG_TAG,"remainder perid expired value------->"+sReminderPeriodExpired); 
        if (DEBUG) Log.d(LOG_TAG,"phone callstate idle  value------->"+sPhoneCallStateIdle); 
        
        if (((sHeadsetPlug == true) || (sBluetoothConnected == true)) &&

            (((sVolThresholdReached == true) && ((sFMRadioRunning == false) || (sFmRadioStatus == false))) ||
             ((sFMVolThresholdReached == true) && ((sFMRadioRunning == true) || (sFmRadioStatus == true))))

                                            && (sReminderPeriodExpired == true)

                                            && (sPhoneCallStateIdle == true)) {

            if (DEBUG) Log.d (LOG_TAG,"Warning Dialog Displayed!");
            // Reset Media volume or FM Radio volume to below warning threshold level
            if (sVolThresholdReached == true) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, sMediaWarningThreshold, 0);

                int vol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (DEBUG) Log.d(LOG_TAG, "Media vol level adjusted to " + vol);

                getApplicationContext().sendBroadcast(
                                            new Intent("com.motorola.acousticwarning.VOLUME_UPDATE"));
            }
	    if (sFMVolThresholdReached == true) {
                if (DEBUG) Log.d(LOG_TAG, "FM Radio is ON, Reset volume");
		mAudioManager.setStreamVolume(sStreamValueFM, sFMWarningThreshold, 0);
                int vol = mAudioManager.getStreamVolume(sStreamValueFM);
					
                if (DEBUG) Log.d(LOG_TAG, "FM Media vol level adjusted to " + vol);
            }
                
            if (DEBUG) Log.d (LOG_TAG,"Before Intent -------------->!");
            Intent warningIntent = new Intent();
            warningIntent.setClass(getApplicationContext(),AcousticWarningDialog.class);
            warningIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                     | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            getApplicationContext().startActivity(warningIntent);
        }
        if (DEBUG) Log.d(LOG_TAG, " Leaving displayWarningDialog() ");
    }

    public void  updateRemainingTime(){
    	if (DEBUG) Log.d(LOG_TAG,"Entering updateRemainingTime() ");
        SharedPreferences sharedPrefs;
        SharedPreferences.Editor sharedPrefEditor;
        sharedPrefs = getApplicationContext().getSharedPreferences(ACOUSTIC_PREF_NAME, 0);
        sharedPrefEditor = sharedPrefs.edit();
        sharedPrefEditor.putLong(REMAINING_TIME, sRemTime);
        /* once the remaining time is updated, there is no need of headset connected time. 
	    Hence resettting the value to 0. */
	sharedPrefEditor.putLong(HEADSET_CONNECTED_TIME, 0); 
        sharedPrefEditor.apply();
        if (DEBUG) Log.d(LOG_TAG, " Leaving updateRemainingTime() ");
    }
	
    // This function updates the headset connected time to the shared preference file.
    public void  updateConnectedTime(){
    	if (DEBUG) Log.d(LOG_TAG,"Entering updateConnectedTime() ");
	sHeadsetConnectedTime = SystemClock.elapsedRealtime();
        if (DEBUG) Log.d(LOG_TAG,"Headset Connected Time = " + sHeadsetConnectedTime);
        SharedPreferences sharedPrefs;
        SharedPreferences.Editor sharedPrefEditor;
        sharedPrefs = getApplicationContext().getSharedPreferences(ACOUSTIC_PREF_NAME, 0);
        sharedPrefEditor = sharedPrefs.edit();
	sharedPrefEditor.putLong(HEADSET_CONNECTED_TIME,sHeadsetConnectedTime); // updating the shared preference with headset connected time
        sharedPrefEditor.apply();
        if (DEBUG) Log.d(LOG_TAG, " Leaving updateConnectedTime() ");
    }
    /* this method stores the states of FM, FMvolume, MediaConnected, BluetoothConnected in shared preference */
    public void  updateStates(String key, Boolean value){
        if (DEBUG) Log.d(LOG_TAG,"Entering updateStates() ");
        SharedPreferences sharedPrefs;
        SharedPreferences.Editor sharedPrefEditor;
        sharedPrefs = getApplicationContext().getSharedPreferences(ACOUSTIC_PREF_NAME, 0);
        sharedPrefEditor = sharedPrefs.edit();
	sharedPrefEditor.putBoolean(key,value); // updating the shared preference with headset connected time
        sharedPrefEditor.apply();
	if (DEBUG) Log.d(LOG_TAG, "Key = " + key + "value=" + value);
        if (DEBUG) Log.d(LOG_TAG, " Leaving updateStates() ");
    }

    public void  updateBootCompleteStatus(String key, String value){
        if (DEBUG) Log.d(LOG_TAG,"Entering updateBootCompleteStatus() ");
        SharedPreferences sharedPrefs;
        SharedPreferences.Editor sharedPrefEditor;
        sharedPrefs = getApplicationContext().getSharedPreferences(ACOUSTIC_PREF_NAME, 0);
        sharedPrefEditor = sharedPrefs.edit();
	sharedPrefEditor.putString(key,value); // updating the shared preference with headset connected time
        sharedPrefEditor.apply();
        if (DEBUG) Log.d(LOG_TAG, "Key = " + key + "value=" + value);
        if (DEBUG) Log.d(LOG_TAG, " Leaving updateStates() ");
    }
    /* This method is required to provide a generic solution across ODM's for FM Solution.
	The FM intent extra value may be a boolean value(true/false) or an integer value(1/0)  */
    public boolean getFmRadioState(Intent intent) {
	    if (DEBUG)  Log.d(LOG_TAG, "Inside getFmRadioState()" );
	    boolean sIntegerFMRadioStateStatus = false;
	    boolean sBooleanFMRadioStateStatus = intent.getBooleanExtra(sFmRadioStateExtra, false);
		if (DEBUG)  Log.d(LOG_TAG, "FM Radio State Value, sBooleanFMRadioStateStatus = " + sBooleanFMRadioStateStatus);
		int sIntegerFMRadioStateValue = intent.getIntExtra(sFmRadioStateExtra, 0);

        if( sIntegerFMRadioStateValue == 1){
            sIntegerFMRadioStateStatus = true;
        }
        else{
            sIntegerFMRadioStateStatus = false;
        }
		if (DEBUG)  Log.d(LOG_TAG, "FM Radio State Value, sIntegerFMRadioStateStatus = " + sIntegerFMRadioStateStatus);
        return (sBooleanFMRadioStateStatus || sIntegerFMRadioStateStatus);
	}


    public void onDestroy() {
        super.onDestroy();
        if (DEBUG) Log.d(LOG_TAG,"onDestroy Enter");

        if (sCountDownStarted == true) {
            sDisconnectTime = SystemClock.elapsedRealtime();
            if (DEBUG) Log.d(LOG_TAG,"Disconnect Time = " + sDisconnectTime);

            sRemTime = sRemTime - (sDisconnectTime - sConnectTime);
            sCountDownStarted = false;
        }
        updateRemainingTime();
        unregisterReceiver(mReceiver);
        alarmManager.cancel(pendingIntent);
    }
	
    public boolean getFmRadioStatus() {
        Class mapClass = null;
        boolean isMethodAvailable = false;
        boolean sFMStatus = false;
	if(sFmRadioState.equalsIgnoreCase("NOTUSED") && sFmRadioStateExtra.equalsIgnoreCase("NOTUSED")) {
	    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
	    try {
		mapClass = Class.forName("android.media.AudioManager");
		// Get the methods
		Method[] methods = mapClass.getDeclaredMethods();
                // Loop through the methods and print out their names
	        for (Method method : methods) {
		    if (DEBUG) Log.d(LOG_TAG,method.getName());
		    if ("isFMActive".equalsIgnoreCase(method.getName()))
		    isMethodAvailable = true;
	        }
	    } catch (ClassNotFoundException e) {		    
		e.printStackTrace();
	    }
	    if (DEBUG) Log.d(LOG_TAG,"isMethodAvailable = " + isMethodAvailable);
	    if (!isMethodAvailable) {
	        if (DEBUG) Log.d(LOG_TAG,"isFMActive() not available");
	    } else {
	        if (DEBUG) Log.d(LOG_TAG,"isFMActive() available");
	        try {		
		    Method methodGetInstance  = mapClass.getMethod("isFMActive",null);
		    if (DEBUG) Log.d(LOG_TAG,"methodGetInstance = " + methodGetInstance);
		    Object fmActiveInvoke = methodGetInstance.invoke(audioManager, null);
		    if (DEBUG) Log.d(LOG_TAG,"fmActiveInvoke = " + fmActiveInvoke);
		    sFMStatus = (Boolean) fmActiveInvoke;
		    if (DEBUG) Log.d(LOG_TAG,"sFMStatus = " + sFMStatus);
                } catch (IllegalAccessException e) { 		
                    e.printStackTrace();
		} catch (InvocationTargetException e) { 
		    e.printStackTrace();
		} catch (NoSuchMethodException e) {
		    e.printStackTrace();   
		}catch (Throwable e){
		    e.printStackTrace();
		}

	    }
	}
	return sFMStatus;
    }	
}
