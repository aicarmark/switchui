package com.motorola.devicestatistics;

import java.util.HashMap;
import java.util.Iterator;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.EventLog;
import android.util.Log;
//BEGIN MMCP 
//import com.motorola.android.provider.MotorolaSettings;
//END MMCP 
import com.motorola.devicestatistics.eventlogs.EventConstants;
import com.motorola.devicestatistics.eventlogs.EventConstants.Events;
import com.motorola.devicestatistics.eventlogs.ILogger;


public class SettingsStat extends Thread {
	
    private final static String LOG_TAG = "SettingsStat";
	private final static int DEVSTAT_APPEND_TO_SETTING_DATA = 1;
	private final static int DEVSTAT_FIRST_POWER_UP_DONE = 2;
	private final static String FIRST_BOOT = "events.firsteverboot";
    private final static String DSBT_STATUS = "dsbt_status";

	private SharedPreferences mPrefSettings;
	private Editor mEditor;
	
	private Context mContext;
	private Handler mHandler;
	private final static Object sLock = new Object();
	private ContentResolver mContentResolver;
	private StringBuffer mInitialValueBuffer ;
	private boolean mIsFirstPowerup;
    
    public final static HashMap<String, String> sDefaultSettingMAP = new HashMap<String, String>();
    private static String sLastBluetoothState = "";
    
    static {
        sDefaultSettingMAP.put(Settings.System.WIFI_SLEEP_POLICY, "1");
        sDefaultSettingMAP.put(Settings.System.SCREEN_OFF_TIMEOUT, "2");
        sDefaultSettingMAP.put(Settings.System.AIRPLANE_MODE_ON, "3");
        sDefaultSettingMAP.put(Settings.System.AUTO_TIME, "4");
        sDefaultSettingMAP.put(Settings.System.SCREEN_BRIGHTNESS_MODE, "5");
        sDefaultSettingMAP.put(Settings.System.VIBRATE_IN_SILENT, "6");
        sDefaultSettingMAP.put(Settings.System.HAPTIC_FEEDBACK_ENABLED, "7");
        sDefaultSettingMAP.put(Settings.System.ACCELEROMETER_ROTATION, "8");
        sDefaultSettingMAP.put(Settings.System.DTMF_TONE_WHEN_DIALING, "22");
        sDefaultSettingMAP.put(Settings.System.VIBRATE_ON, "23");
        sDefaultSettingMAP.put(Settings.System.VOLUME_ALARM, "24");
        sDefaultSettingMAP.put("volume_alarm_speaker", "24");
        sDefaultSettingMAP.put(Settings.System.VOLUME_BLUETOOTH_SCO, "25");
        sDefaultSettingMAP.put(Settings.System.VOLUME_MUSIC, "26");
        sDefaultSettingMAP.put("volume_music_speaker", "26");
        sDefaultSettingMAP.put("volume_music_headset", "26");
        sDefaultSettingMAP.put(Settings.System.VOLUME_NOTIFICATION, "27");
        sDefaultSettingMAP.put(Settings.System.VOLUME_RING, "28");
        sDefaultSettingMAP.put("volume_ring_speaker", "28");
        sDefaultSettingMAP.put(Settings.System.VOLUME_SYSTEM, "29");
        sDefaultSettingMAP.put(Settings.System.VOLUME_VOICE, "30");
        sDefaultSettingMAP.put("volume_voice_earpiece", "30");
        sDefaultSettingMAP.put("volume_voice_headset", "30");
        sDefaultSettingMAP.put("volume_voice_speaker", "30");
        sDefaultSettingMAP.put(Settings.System.SCREEN_BRIGHTNESS, "32");
        //Following setting will indicate when user toggles
        // Settings->Language&Keyboard->Built-inKeyboard->AutoReplace
        sDefaultSettingMAP.put(Settings.System.TEXT_AUTO_REPLACE, "36");
        //Following settings will indicate when user toggles
        // Settings->Display->Animation->All or No animations
        sDefaultSettingMAP.put(Settings.System.WINDOW_ANIMATION_SCALE, "37");
    }
    
    public final static HashMap<String, String> sSecureSettingMAP = new HashMap<String, String>();
    static {
        sSecureSettingMAP.put(Settings.Secure.BACKUP_ENABLED, "9");
        sSecureSettingMAP.put(Settings.Secure.WIFI_ON, "10");
        sSecureSettingMAP.put(Settings.Secure.BACKGROUND_DATA, "11");
        sSecureSettingMAP.put(Settings.Secure.DATA_ROAMING, "12");
        sSecureSettingMAP.put(Settings.Secure.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, "13");
        sSecureSettingMAP.put(Settings.Secure.DEFAULT_INPUT_METHOD, "14");
        sSecureSettingMAP.put(Settings.Secure.ACCESSIBILITY_ENABLED, "19");
        sSecureSettingMAP.put(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "20");
        sSecureSettingMAP.put(Settings.Secure.MOBILE_DATA, "21");
        sSecureSettingMAP.put(Settings.Secure.BLUETOOTH_ON, "34");
        sSecureSettingMAP.put(Settings.Secure.LOCATION_PROVIDERS_ALLOWED, "35");
    }
//BEGIN MMCP
   /* 
	public final static HashMap<String, String> sMotorolaSettingMAP = new HashMap<String, String>();
	static {
        sMotorolaSettingMAP.put(MotorolaSettings.LOCK_TYPE, "15");
        //sMotorolaSettingMAP.put(MotorolaSettings.IN_POCKET_DETECTION, "16"); // Compilation error on ICS
        sMotorolaSettingMAP.put(MotorolaSettings.DOUBLE_TAP, "17");
        sMotorolaSettingMAP.put(MotorolaSettings.IS_TALKBACK_ON, "18");
        sMotorolaSettingMAP.put(MotorolaSettings.TTS_CALLER_ID_READOUT, "31");
        // Following Setting will indicate when user toggles
        // Settings->Wireless&Networks->Wi-Fi Settings->NotifyMe
        sMotorolaSettingMAP.put(MotorolaSettings.WIFI_OFFLOAD_FLAG, "38");
        // Following Setting will indicate when user toggles
        // Settings->Display->Brightness->DisplayPowerSaver
        sMotorolaSettingMAP.put(MotorolaSettings.POWER_SAVER_ENABLED, "39");
    } */
// END MMCP
    private final static int sBuildIdKey = 33;
	
	public SettingsStat (Context context, Handler handler) {
		mContext = context;
		mHandler = handler;
	    mPrefSettings = mContext.getSharedPreferences(EventConstants.OPTIONS,
                                                      Context.MODE_PRIVATE);
        mEditor = mPrefSettings.edit();
        mContentResolver = context.getContentResolver();
        mInitialValueBuffer  = new StringBuffer();
	}
	
    public void run() {
        mIsFirstPowerup = mPrefSettings.getBoolean(FIRST_BOOT, true);
        registerDefaultSettings();
        registerSecureSettings();
//BEGIN MMCP
//        registerMotorolaSettings();
// END MMCP
        logBuildId();
        if (mIsFirstPowerup) {
            String data = mInitialValueBuffer.toString();
            Message msg = mSettingObserverHandler.obtainMessage(DEVSTAT_APPEND_TO_SETTING_DATA, data);
            mSettingObserverHandler.sendMessage(msg);
            mSettingObserverHandler.sendEmptyMessage(DEVSTAT_FIRST_POWER_UP_DONE);
        }
           
    }

    private void logBuildId() {
        String storedId = mPrefSettings.getString(EventConstants.BUILD_ID, null);

        if(storedId == null || !storedId.equals(Build.ID)) {
            StringBuilder sb = new StringBuilder();
            String data = sb.append(sBuildIdKey)
                    .append(";").append(System.currentTimeMillis())
                    .append(";").append(Build.ID)
                    .toString();
            Message msg = mSettingObserverHandler.obtainMessage(DEVSTAT_APPEND_TO_SETTING_DATA, data);
            mSettingObserverHandler.sendMessage(msg);
        }
         mEditor.putString(EventConstants.BUILD_ID, Build.ID);
         Utils.saveSharedPreferences(mEditor);
    }
	
	public void registerDefaultSettings() {
	    Uri tempUri;
	    String val;
	    Iterator<String> iterator = sDefaultSettingMAP.keySet().iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            tempUri = Settings.System.getUriFor(key);
            if (tempUri != null) {
               // Log.i(LOG_TAG, "Regestring "+key);
                String checkinID = (String) sDefaultSettingMAP.get(key);
                SettingContentObsever sco= new SettingContentObsever(key, mHandler, checkinID, "System");
                mContentResolver.registerContentObserver(tempUri, false, sco);
                if (mIsFirstPowerup) {
                    val = getSystemSettingValue(key);
                    checkinAtFirstpowerUp(val, key, checkinID);
                }
                
            }
        }
    }

    private String getSystemSettingValue(String key) {
        String ret = null;
        if (Settings.System.DTMF_TONE_WHEN_DIALING.equals(key)) {
            int val = Settings.System.getInt(mContentResolver, key, 1);
            ret = String.valueOf(val);
        } else {
            ret = Settings.System.getString(mContentResolver, key);
        }
        return ret;
    }

    public void registerSecureSettings () { 
        Uri tempUri;
        String val;
        Iterator<String> iterator = sSecureSettingMAP.keySet().iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            tempUri = Settings.Secure.getUriFor(key);
            if (tempUri != null) {
                //Log.i(LOG_TAG, "Regestring "+key);
                String checkinID = (String) sSecureSettingMAP.get(key);
                SettingContentObsever sco= new SettingContentObsever(key, mHandler, checkinID, "Secure");
                mContentResolver.registerContentObserver(tempUri, false, sco);
                if (mIsFirstPowerup) {
                    val = getSecureSettingValue(key);
                    checkinAtFirstpowerUp(val, key, checkinID);
                }
                
            }
        }
    }   

    private String getSecureSettingValue(String key) {
        String ret = null;
        int val = -1;
        if (Settings.Secure.MOBILE_DATA.equals(key) || Settings.Secure.BACKGROUND_DATA.equals(key)) {
            val = Settings.Secure.getInt(mContentResolver, key, 1);
            ret = String.valueOf(val);
        } else if (Settings.Secure.ACCESSIBILITY_ENABLED.equals(key)) {
            val = Settings.Secure.getInt(mContentResolver, key, 0);
            ret = String.valueOf(val);
        } else {
            ret = Settings.Secure.getString(mContentResolver, key);
        }
        return ret;
    }

// BEGIN MMCP
/*    public void registerMotorolaSettings() {
        Uri tempUri;
        String val;
        Iterator<String> iterator = sMotorolaSettingMAP.keySet().iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            tempUri = MotorolaSettings.getUriFor(key);
            if (tempUri != null) {
                //Log.i(LOG_TAG, "Regestring "+key);
                String checkinID = (String) sMotorolaSettingMAP.get(key);
                SettingContentObsever sco= new SettingContentObsever(key, mHandler, checkinID,"Mot");
                mContentResolver.registerContentObserver(tempUri, false, sco);
                if (mIsFirstPowerup) {
                    val = getMotorolaSettingValue(key);
                    checkinAtFirstpowerUp(val, key, checkinID);
                }
            }
        }
    }

    private String getMotorolaSettingValue(String key) {
        String ret = null;
        if (MotorolaSettings.IS_TALKBACK_ON.equals(key)) {
            int val = MotorolaSettings.getInt(mContentResolver, key, 0);
            ret = String.valueOf(val);
        } else {
            ret = MotorolaSettings.getString(mContentResolver, key);
        }
        return ret;
    }
*/
//END MMCP

    private void checkinAtFirstpowerUp(String val, String key, String checkin_id) {
        //Following code is taken from onChange function define bellow.
        if (val != null) {
            val = processResult(val, key);
            if (mInitialValueBuffer.length() != 0) mInitialValueBuffer.append('[');
            mInitialValueBuffer .append(checkin_id+";"+System.currentTimeMillis()+";"+val);
        }
    }

	protected Handler mSettingObserverHandler = new Handler() {
		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
                case DEVSTAT_APPEND_TO_SETTING_DATA:
                    synchronized(sLock) {
                        String currentString;
                        currentString = mPrefSettings.getString(EventConstants.SETTING_DATA_KEY,
                                null);
                        currentString = (currentString == null) ? (String)msg.obj :
                            currentString + '[' + (String)msg.obj;
                        mEditor.putString(EventConstants.SETTING_DATA_KEY, currentString);
                        Utils.saveSharedPreferences(mEditor);
                    }
                    break;
                case DEVSTAT_FIRST_POWER_UP_DONE:
                    synchronized(sLock) {
                        mEditor.putBoolean(FIRST_BOOT, false);
                        Utils.saveSharedPreferences(mEditor);
                    }
                    break;
				default:
                    super.handleMessage(msg);
                    break;
			}
		}
	};
    
	
	public static void checkInSettingsData(SharedPreferences sp, ILogger logger) {
	    int source = EventConstants.Source.RECEIVER;
	    String settingData = null;
	    synchronized(sLock) {
	        settingData = sp.getString(EventConstants.SETTING_DATA_KEY, null);
            SharedPreferences.Editor ed = sp.edit();
	        ed.remove(EventConstants.SETTING_DATA_KEY);
	        Utils.saveSharedPreferences(ed);
        }
        if(settingData != null && settingData.length() != 0) {
            logger.log(source, Events.SETTINGSTAT, EventConstants.SETTING_CHK_ID, settingData);
        }
	}
	
    String processResult(String val, String key) {
// BEGIN MMCP
        //if (key.equals(MotorolaSettings.DOUBLE_TAP)) {
        if (false) { // END MMCP
            int lastIndexofDot = val.lastIndexOf(".");
            if (lastIndexofDot != -1) {
                val = val.substring(lastIndexofDot+1);
            }
        } else if (key.equals(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)) {
            StringBuilder result = new StringBuilder();
            if (val == null) val = "";

            for ( String service : val.split(":") ) {
                if (service.isEmpty()) continue;
                String[] parts = service.split("\\.");
                if (parts.length > 0) service = parts[parts.length - 1];
                if (result.length() != 0) result.append(':');
                result.append(service);
            }

            val = result.toString();
        }
        return val;
    }

	class SettingContentObsever extends ContentObserver {
	    String mKeyName;
	    Handler mHandler;
	    String mType;
        String mCheckinID;
	    
	    SettingContentObsever (String key, Handler handler, String checkinID, String type) {
	        super(handler);
	        mHandler = handler;
	        mKeyName = key;
	        mType = type;
            mCheckinID = checkinID;
	    }
	    
	    public boolean deliverSelfNotifications () {
	        return false;
	    }

	    public void onChange (boolean selfChange) {
	        if(!selfChange) {
                
                String val = null; // BEGIN END MMCP
                if (mType.equals("System")) {
			        val = Settings.System.getString(mContentResolver, mKeyName);
                } else if (mType.equals("Secure")) {
			        val = Settings.Secure.getString(mContentResolver, mKeyName);
                    if (Settings.Secure.BLUETOOTH_ON.equals(mKeyName) ) {
                        if (val == null || val.equals(sLastBluetoothState)) return;
                        sLastBluetoothState = val;
                        try {
                            int code = EventLog.getTagCode(DSBT_STATUS);
                            if (code != -1) EventLog.writeEvent(code, val);
                        } catch (Exception e) {
                            // Once a null pointer exception was reported to
                            // portal inside getTagCode
                            Log.e(LOG_TAG, "onChange DSBT_STATUS", e);
                        }
                    }
                } else {
                // BEGIN MMCP
		//	        val = MotorolaSettings.getString(mContentResolver, mKeyName);
                // END MMCP
                }
                if (val != null) {
                      val = processResult(val, mKeyName);
                    String data = mCheckinID+";"+System.currentTimeMillis()+";"+val;
                    //Log.i(LOG_TAG, data);
                    Message msg = mSettingObserverHandler.obtainMessage (DEVSTAT_APPEND_TO_SETTING_DATA, (Object)data);
                    mSettingObserverHandler.sendMessage(msg);
				}
	        }
	        
	    }
    }
}
