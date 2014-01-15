package com.motorola.datacollection.accessory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;

import com.motorola.datacollection.Utilities;
import com.motorola.datacollection.Watchdog;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothClass.Device;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class AccessoryReceiver extends BroadcastReceiver {

    private static final String TAG = "DCE_AccessoryReceiver";
    private static final boolean LOGD = Utilities.LOGD;
    private static final String BT_HEADSET_STATE_CHANGED_INTENT =
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED;
    private static final String BT_HEADSET_EXTRA_STATE = BluetoothProfile.EXTRA_STATE;
    private static final String BT_HEADSET_EXTRA_PREVIOUS_STATE =
            BluetoothProfile.EXTRA_PREVIOUS_STATE;
    private static final int BT_HEADSET_STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    private static final int BT_HEADSET_STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;
    private static final int BT_HEADSET_STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;

    private static final int INVALID_VALUE = -1;

    private static final int PAIR_FAILURE = 0;
    private static final int PAIR_SUCCESS = 1;

    private static AccessoryReceiver sAccessoryReceiver;

    private static boolean sPairingInProgress;

    synchronized public static final void initialize() {
        if ( LOGD ) { Log.d( TAG, "initialize" ); }

        sAccessoryReceiver = new AccessoryReceiver();
        IntentFilter intentFilter = new IntentFilter();

        // ACTION_HEADSET_PLUG will not be received, when it is declared in
        //  AndroidManifest.xml. It can be received only by receivers that are registered
        //  dynamically. So we create this dynamic broadcast receiver,
        //  and add ACTION_HEADSET_PLUG to the intent filter
        intentFilter.addAction( Intent.ACTION_HEADSET_PLUG );

        intentFilter.addAction( BT_HEADSET_STATE_CHANGED_INTENT );
        intentFilter.addAction( BluetoothDevice.ACTION_ACL_CONNECTED );
        intentFilter.addAction( BluetoothDevice.ACTION_ACL_DISCONNECTED );
        intentFilter.addAction( BluetoothDevice.ACTION_BOND_STATE_CHANGED );
        Utilities.getContext().registerReceiver( sAccessoryReceiver, intentFilter );
    }

    private static final boolean isHeadsetDevice( BluetoothDeviceInfo deviceInfo ) {
        return deviceInfo.mDeviceClass == Device.AUDIO_VIDEO_WEARABLE_HEADSET ? true : false;
    }

    private static class BtServiceBuilder {
        private boolean mHasData;
        private final BluetoothClass mBtClass;
        private StringBuilder mStringBuilder;

        BtServiceBuilder( BluetoothClass aBtClass ) {
            mBtClass = aBtClass;
            mStringBuilder = new StringBuilder();
        }

        final void checkService( int serviceNum, String serviceName ) {
            if ( mBtClass.hasService( serviceNum ) ) {
                if ( mHasData == true ) {
                    mStringBuilder.append( ',' );
                } else {
                    mHasData = true;
                }
                mStringBuilder.append( serviceName );
            }
        }

        public final String toString() {
            return mStringBuilder.toString();
        }
    }

    @Override
    public final void onReceive(final Context context, final Intent intent) {
        final long timeMs = System.currentTimeMillis();
        new Utilities.BackgroundRunnable() {
            public void run() {
                onReceiveImpl(context, intent, timeMs);
            }
        };
    }

    private final void onReceiveImpl(Context context, Intent intent, long timeMs) {
        if (Watchdog.isDisabled()) return;

        if ( intent == null ) {
            if ( LOGD ) { Log.d( TAG , "intent is null!" ); }
            return;
        }

        if ( LOGD ) { Log.d( TAG , "Received intent " + intent.toUri(0) ); }

        String action = intent.getAction();
        if ( action == null ) {
            if ( LOGD ) { Log.d( TAG , "intent.action is null!" ); }
            return;
        }

        if ( action.equals( Intent.ACTION_HEADSET_PLUG ) ||
                action.equals( BT_HEADSET_STATE_CHANGED_INTENT ) ||
                action.equals( BluetoothDevice.ACTION_ACL_CONNECTED ) ||
                action.equals( BluetoothDevice.ACTION_ACL_DISCONNECTED ) ) {
            handleAccessoryIntent(intent, timeMs);
        } else if ( action.equals( BluetoothDevice.ACTION_BOND_STATE_CHANGED ) ) {
            handleBluetoothPairingIntent(intent, timeMs);
        } else {
            if ( LOGD ) { Log.d( TAG , "Ignoring unexpected intent: " + intent.toString()); }
            return;
        }
    }

    private final void handleAccessoryIntent( Intent intent, long timeMs ) {
        String accessory = null;
        String state = null;
        String name = null;
        String serviceClass = null;
        String mic = "0";
        String action = intent.getAction();

        if ( action == null ) return;

        if ( action.equals( Intent.ACTION_HEADSET_PLUG ) ) {
            int stateInteger = intent.getIntExtra( "state", INVALID_VALUE );
            accessory = intent.getStringExtra("name");
            int micStateInt = intent.getIntExtra( "microphone", INVALID_VALUE );
            mic = String.valueOf( micStateInt );

            if ( stateInteger == INVALID_VALUE || accessory == null ||
                    micStateInt == INVALID_VALUE ) {
                if ( LOGD ) { Log.d( TAG , "Invalid data in intent " + stateInteger + " " +
                        accessory + " " + micStateInt ); }
                return;
            }

            if ( "No Device".equals( accessory ) ) accessory = "No Wired Headset";

            state = stateInteger == 0 ? "off" : "on";
        } else {
            BluetoothDeviceInfo deviceInfo = new BluetoothDeviceInfo();
            if ( deviceInfo.parseIntent( intent ) == false ) return;
            accessory = deviceInfo.mAccessory;
            name = deviceInfo.mName;
            serviceClass = deviceInfo.mServiceClass;

            if ( isHeadsetDevice(deviceInfo) ) {
                if ( action.equals( BT_HEADSET_STATE_CHANGED_INTENT ) ) {
                    int headsetState = intent.getIntExtra( BT_HEADSET_EXTRA_STATE, INVALID_VALUE );
                    if ( headsetState == INVALID_VALUE ) {
                        if ( LOGD ) {
                            Log.d( TAG, "BT_HEADSET_STATE_CHANGED_INTENT doesnt have state" );
                        }
                        return;
                    }
                    switch ( headsetState ) {
                    case BT_HEADSET_STATE_CONNECTED:
                        state = "on";
                        break;
                    case BT_HEADSET_STATE_DISCONNECTED:
                        int previousState =
                            intent.getIntExtra( BT_HEADSET_EXTRA_PREVIOUS_STATE, INVALID_VALUE );
                        if ( previousState != BT_HEADSET_STATE_CONNECTED &&
                                previousState != BT_HEADSET_STATE_DISCONNECTING ) {
                            if ( LOGD ) {
                                Log.d( TAG, "Ignoring previous state " + previousState);
                            }
                            return;
                        }
                        state = "off";
                        break;
                    default:
                        if ( LOGD ) { Log.d( TAG, "Ignoring BT headset state " + headsetState); }
                        return;
                    }
                }
            } else {
                if ( action.equals( BluetoothDevice.ACTION_ACL_CONNECTED ) ) {
                    state = "on";
                } else if ( action.equals( BluetoothDevice.ACTION_ACL_DISCONNECTED ) ) {
                    state = "off";
                } else {
                    if ( LOGD ) {
                        Log.d( TAG, "Ignoring non-acl message for non-headset " );
                    }
                    return;
                }
            }
        }

        if ( accessory != null && state != null ) {
            if ( name == null ) name = "";

            if ( serviceClass == null ) serviceClass = "";

            try {
                 Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_2, "DC_ACCESSORY",
                         Utilities.EVENT_LOG_VERSION, timeMs, "accessory",
                         URLEncoder.encode(accessory, "US-ASCII"), "state", state, "name",
                         URLEncoder.encode (name, "US-ASCII"), "service", serviceClass, "mic", mic);
            } catch (UnsupportedEncodingException e) {
                if ( LOGD ) { Log.d( TAG, Log.getStackTraceString(e) ); }
            }
        }
    }

    private void handleBluetoothPairingIntent(Intent intent, long timeMs) {
        if ( LOGD ) { Log.d( TAG, "Got Intent " + intent.toUri(0) ); }

        int pairState = intent.getIntExtra( BluetoothDevice.EXTRA_BOND_STATE, INVALID_VALUE );
        if ( pairState == INVALID_VALUE ) {
            if ( LOGD ) { Log.d( TAG, "BOND_STATE_CHANGED does not have EXTRA_BOND_STATE"); }
            return;
        }

        boolean logPairing = false;
        int pairSuccess = PAIR_FAILURE;

        if ( pairState == BluetoothDevice.BOND_BONDING ) {
            sPairingInProgress = true;
        } else if ( pairState == BluetoothDevice.BOND_BONDED ) {
            sPairingInProgress = false;
            logPairing = true;
            pairSuccess = PAIR_SUCCESS;
        } else if ( pairState == BluetoothDevice.BOND_NONE ) {
            if ( sPairingInProgress == true ) {
                sPairingInProgress = false;
                logPairing = true;
                pairSuccess = PAIR_FAILURE;
            }
        } else {
            if ( LOGD ) { Log.d( TAG, "Unexpected pair state " + pairState ); }
            return;
        }

        if ( logPairing ) {
            BluetoothDeviceInfo deviceInfo = new BluetoothDeviceInfo();
            if ( deviceInfo.parseIntent( intent ) == false ) return;
            try {
                if (deviceInfo.mAccessory ==null) deviceInfo.mAccessory = "null";
                if (deviceInfo.mName == null) deviceInfo.mName = "null";

                Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_2, "DC_PAIRING",
                        Utilities.EVENT_LOG_VERSION, timeMs, "accessory",
                        URLEncoder.encode (deviceInfo.mAccessory, "US-ASCII"), "name",
                        URLEncoder.encode (deviceInfo.mName, "US-ASCII"),"service",
                        deviceInfo.mServiceClass, "su", Integer.toString(pairSuccess));
            } catch (UnsupportedEncodingException e) {
                if ( LOGD ) { Log.d( TAG, Log.getStackTraceString(e) ); }
            }
        }
    }

    static class BluetoothDeviceInfo {
        String mAccessory;
        String mName;
        String mServiceClass;
        int    mDeviceClass;

        final boolean parseIntent( Intent intent ) {
            BluetoothDevice btDevice = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
            if ( btDevice == null ) {
                if ( LOGD ) { Log.d( TAG , "BluetoothDevice is null" ); }
                return false;
            }

            mName = btDevice.getName();

            BluetoothClass btClass = btDevice.getBluetoothClass();
            if ( btClass == null ) {
                if ( LOGD ) { Log.d( TAG , "BluetoothClass is null" ); }
                return false;
            }

            BtServiceBuilder serviceBuilder = new BtServiceBuilder( btClass );
            serviceBuilder.checkService( BluetoothClass.Service.AUDIO, "AUDIO" );
            serviceBuilder.checkService( BluetoothClass.Service.CAPTURE, "CAPTURE" );
            serviceBuilder.checkService( BluetoothClass.Service.INFORMATION, "INFORMATION" );
            serviceBuilder.checkService( BluetoothClass.Service.LIMITED_DISCOVERABILITY,
                    "LIMITED_DISCOVERABILITY" );
            serviceBuilder.checkService( BluetoothClass.Service.NETWORKING, "NETWORKING" );
            serviceBuilder.checkService( BluetoothClass.Service.OBJECT_TRANSFER,
                    "OBJECT_TRANSFER" );
            serviceBuilder.checkService( BluetoothClass.Service.POSITIONING, "POSITIONING" );
            serviceBuilder.checkService( BluetoothClass.Service.RENDER, "RENDER" );
            serviceBuilder.checkService( BluetoothClass.Service.TELEPHONY, "TELEPHONY" );
            mServiceClass = serviceBuilder.toString();

            mDeviceClass = btClass.getDeviceClass();
            switch ( mDeviceClass ) {
            case Device.AUDIO_VIDEO_CAMCORDER : mAccessory = "AUDIO_VIDEO_CAMCORDER"; break;
            case Device.AUDIO_VIDEO_CAR_AUDIO : mAccessory = "AUDIO_VIDEO_CAR_AUDIO"; break;
            case Device.AUDIO_VIDEO_HANDSFREE : mAccessory = "AUDIO_VIDEO_HANDSFREE"; break;
            case Device.AUDIO_VIDEO_HEADPHONES : mAccessory = "AUDIO_VIDEO_HEADPHONES"; break;
            case Device.AUDIO_VIDEO_HIFI_AUDIO : mAccessory = "AUDIO_VIDEO_HIFI_AUDIO"; break;
            case Device.AUDIO_VIDEO_LOUDSPEAKER : mAccessory = "AUDIO_VIDEO_LOUDSPEAKER"; break;
            case Device.AUDIO_VIDEO_MICROPHONE : mAccessory = "AUDIO_VIDEO_MICROPHONE"; break;
            case Device.AUDIO_VIDEO_PORTABLE_AUDIO : mAccessory =
                "AUDIO_VIDEO_PORTABLE_AUDIO"; break;
            case Device.AUDIO_VIDEO_SET_TOP_BOX : mAccessory = "AUDIO_VIDEO_SET_TOP_BOX"; break;
            case Device.AUDIO_VIDEO_UNCATEGORIZED : mAccessory = "AUDIO_VIDEO_UNCATEGORIZED"; break;
            case Device.AUDIO_VIDEO_VCR : mAccessory = "AUDIO_VIDEO_VCR"; break;
            case Device.AUDIO_VIDEO_VIDEO_CAMERA : mAccessory = "AUDIO_VIDEO_VIDEO_CAMERA"; break;
            case Device.AUDIO_VIDEO_VIDEO_CONFERENCING :
                mAccessory = "AUDIO_VIDEO_VIDEO_CONFERENCING";
                break;
            case Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER :
                mAccessory = "AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER"; break;
            case Device.AUDIO_VIDEO_VIDEO_GAMING_TOY :
                mAccessory = "AUDIO_VIDEO_VIDEO_GAMING_TOY"; break;
            case Device.AUDIO_VIDEO_VIDEO_MONITOR : mAccessory = "AUDIO_VIDEO_VIDEO_MONITOR"; break;
            case Device.AUDIO_VIDEO_WEARABLE_HEADSET :
                mAccessory = "AUDIO_VIDEO_WEARABLE_HEADSET"; break;
            case Device.COMPUTER_DESKTOP : mAccessory = "COMPUTER_DESKTOP"; break;
            case Device.COMPUTER_HANDHELD_PC_PDA : mAccessory = "COMPUTER_HANDHELD_PC_PDA"; break;
            case Device.COMPUTER_LAPTOP : mAccessory = "COMPUTER_LAPTOP"; break;
            case Device.COMPUTER_PALM_SIZE_PC_PDA : mAccessory = "COMPUTER_PALM_SIZE_PC_PDA"; break;
            case Device.COMPUTER_SERVER : mAccessory = "COMPUTER_SERVER"; break;
            case Device.COMPUTER_UNCATEGORIZED : mAccessory = "COMPUTER_UNCATEGORIZED"; break;
            case Device.COMPUTER_WEARABLE : mAccessory = "COMPUTER_WEARABLE"; break;
            case Device.HEALTH_BLOOD_PRESSURE : mAccessory = "HEALTH_BLOOD_PRESSURE"; break;
            case Device.HEALTH_DATA_DISPLAY : mAccessory = "HEALTH_DATA_DISPLAY"; break;
            case Device.HEALTH_GLUCOSE : mAccessory = "HEALTH_GLUCOSE"; break;
            case Device.HEALTH_PULSE_OXIMETER : mAccessory = "HEALTH_PULSE_OXIMETER"; break;
            case Device.HEALTH_PULSE_RATE: mAccessory = "HEALTH_PULSE_RATE"; break;
            case Device.HEALTH_THERMOMETER: mAccessory = "HEALTH_THERMOMETER"; break;
            case Device.HEALTH_UNCATEGORIZED: mAccessory = "HEALTH_UNCATEGORIZED"; break;
            case Device.HEALTH_WEIGHING: mAccessory = "HEALTH_WEIGHING"; break;
            case Device.PHONE_CELLULAR: mAccessory = "PHONE_CELLULAR"; break;
            case Device.PHONE_CORDLESS: mAccessory = "PHONE_CORDLESS"; break;
            case Device.PHONE_ISDN: mAccessory = "PHONE_ISDN"; break;
            case Device.PHONE_MODEM_OR_GATEWAY: mAccessory = "PHONE_MODEM_OR_GATEWAY"; break;
            case Device.PHONE_SMART: mAccessory = "PHONE_SMART"; break;
            case Device.PHONE_UNCATEGORIZED: mAccessory = "PHONE_UNCATEGORIZED"; break;
            case Device.TOY_CONTROLLER: mAccessory = "TOY_CONTROLLER"; break;
            case Device.TOY_DOLL_ACTION_FIGURE: mAccessory = "TOY_DOLL_ACTION_FIGURE"; break;
            case Device.TOY_GAME: mAccessory = "TOY_GAME"; break;
            case Device.TOY_ROBOT: mAccessory = "TOY_ROBOT"; break;
            case Device.TOY_UNCATEGORIZED: mAccessory = "TOY_UNCATEGORIZED"; break;
            case Device.TOY_VEHICLE: mAccessory = "TOY_VEHICLE"; break;
            case Device.WEARABLE_GLASSES: mAccessory = "WEARABLE_GLASSES"; break;
            case Device.WEARABLE_HELMET: mAccessory = "WEARABLE_HELMET"; break;
            case Device.WEARABLE_JACKET: mAccessory = "WEARABLE_JACKET"; break;
            case Device.WEARABLE_PAGER: mAccessory = "WEARABLE_PAGER"; break;
            case Device.WEARABLE_UNCATEGORIZED: mAccessory = "WEARABLE_UNCATEGORIZED"; break;
            case Device.WEARABLE_WRIST_WATCH: mAccessory = "WEARABLE_WRIST_WATCH"; break;
            default:
                if ( LOGD ) { Log.d( TAG , "deviceClass is unknown " + mDeviceClass ); }
                mAccessory = "Unknown:" + mDeviceClass;
                for ( Field field :BluetoothClass.Device.class.getFields() ) {
                    if (field.getType().equals(int.class)) {
                        int modifiers = field.getModifiers();
                        try {
                            if (Modifier.isStatic(modifiers) &&
                                field.getInt(null) == mDeviceClass ) {
                                mAccessory = field.getName();
                                if (LOGD) { Log.d (TAG, " mAccessory is "+mAccessory); }
                                break;
                            }
                        } catch (Exception e) {
                            Log.e (TAG, "Accessory" +mDeviceClass,e);
                        }
                    }
                }
                break;
            }
            if ( mAccessory == null ) mAccessory = "";
            if ( mName == null ) mName = "";
            if ( mServiceClass == null ) mServiceClass = "";

            return true;
        }
    }
}
