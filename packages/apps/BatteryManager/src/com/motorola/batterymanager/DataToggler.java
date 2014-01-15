/**
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: DataToggler.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 11-06-09       A24178       Created file
 *                -Ashok
 * 01-07-10       A24178       IKMAP-4105:Port BZ40942/44216
 *                -Ashok
 * 01-18-10       A24178       IKMAP-4047: APN disable opt
 *                -Ashok
 * 04-06-10       A24178       IKMAP-8311: Multiple Apn types
 *                -Ashok                   fix
 * 07-13-10       A16462       IKSTABLETWO-2784: Dynamic Data Mode change
 *                -Selvi
 **********************************************************
 */

package com.motorola.batterymanager;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.content.ContentResolver;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.RemoteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.telephony.TelephonyManager; 

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;

import com.motorola.batterymanager.devicestatistics.DevStatPrefs;
import com.motorola.batterymanager.BatteryManagerDefs;
import com.motorola.batterymanager.BatteryManagerDefs.DataConnection;
import com.motorola.batterymanager.BatteryManagerDefs.Mode;

// Using Telephony interface is the straight forward way to disable/enable data.
// Due to the fact that, enabling a specific APN type is not entertained after
// Data connectivity is disabled, and to avoid platform changes to one specific
// Operator, we are falling back to old sytle of disabling data connectivity &
// APN based on Phone Type.
// TODO: Recheck future release of Google versions, for using Telephony Interface.

public class DataToggler {

    private static final Uri APN_CONTENT_URI = Uri.parse("content://telephony/carriers");
    private static final String APN_ID = "_id";
    private static final String APN_TYPE = "type";
    private static final String APN_NUMERIC = "numeric";
    private static final String APN_CURRENT_SELECTION = "(current = 1)";
    private static final String APN_DISABLE = "-@xyz";
    
    private static final String APN_ID_QUERY = "_id = ?";

    private static final String APN_TYPE_EMPTY = "empty";
    private static final String APN_TYPE_STAR = "star";

    private static final String LOG_TAG = "PowerProfileToggler";

    private Context mContext;
    private SharedPreferences mPref;
    private static Object sLock = new Object();
    private PowerManager mPowerManager;
    private ExecutorService mThreadManager;

    // Stats log
    private long mDisableTime = 0;
    private String mDisableLevel = null;

    private Cursor getActiveNwApn() {
        ContentResolver cr = mContext.getContentResolver();
        boolean bNumeric = false;

        // Combine all passes into 1 query
        StringBuilder sb = new StringBuilder(APN_CURRENT_SELECTION);
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        String netNumeric = (tm != null) ? tm.getNetworkOperator() : null;
        String simNumeric = (tm != null && tm.getSimState() == TelephonyManager.SIM_STATE_READY)
                ? tm.getSimOperator() : null;

        // Create the numeric query with both values
        if(netNumeric != null && netNumeric.length() > 0) {
            sb.append(" OR (" + APN_NUMERIC + "='" + netNumeric + "')");
        }
        if(simNumeric != null && simNumeric.length() > 0) {
            sb.append(" OR (" + APN_NUMERIC + "='" + simNumeric + "')");
            bNumeric = true;
        }
        if(tm != null)
            Utils.Log.d(LOG_TAG, "Combined pass: Q=" + sb.toString() + ", " + tm.getSimState());
        else
            Utils.Log.d(LOG_TAG, "Combined pass: Q=" + sb.toString());
        if(bNumeric)
            return cr.query(APN_CONTENT_URI, new String[]{APN_ID, APN_TYPE}, sb.toString(), null, null);
        else
            return null;
    }

    private void enableApnType(Cursor apnCursor, String type) {
        ContentResolver cr = mContext.getContentResolver();
        ContentValues cv = new ContentValues();
        int idIdx = apnCursor.getColumnIndex(APN_ID);
        int typeIdx = apnCursor.getColumnIndex(APN_TYPE);
        apnCursor.moveToFirst();
        int count = apnCursor.getCount();
        for(int i = 0; i < count; ++i) {
            cv.clear();
            String s_apnId = apnCursor.getString(idIdx);
            String s_type = apnCursor.getString(typeIdx);

            if ( s_type.equals(APN_DISABLE)) { // Null APN Type
                cv.put(APN_TYPE, APN_TYPE_EMPTY + "," + type);
                cr.update(APN_CONTENT_URI, cv, APN_ID_QUERY, new String[] {s_apnId});
            } else if (s_type.contains("*")) {
                cv.put(APN_TYPE, APN_TYPE_STAR + "," + type);
                cr.update(APN_CONTENT_URI, cv, APN_ID_QUERY, new String[] {s_apnId});
            } else if ((s_type.contains(APN_TYPE_EMPTY) || s_type.contains(APN_TYPE_STAR))
                    && !s_type.contains(type)) {
                cv.put(APN_TYPE, s_type + "," + type);
                cr.update(APN_CONTENT_URI, cv, APN_ID_QUERY, new String[] {s_apnId});
            }else if (s_type.contains(type)) {
                if (!s_type.contains(",")) { // single mms type, simple case
                    cv.put(APN_TYPE, type);
                    cr.update(APN_CONTENT_URI, cv, APN_ID_QUERY, new String[] {s_apnId});
                }else {
                    String[] types = s_type.split(",");
                    if (types != null) {
                        int len = types.length;
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j < len; ++j) {
                            if (types[j].contains(type)) {
                                sb.append(type);
                            } else {
                                sb.append(types[j]);
                            }
                            if (j != (len - 1)) {
                                // for everything but last, append the comma back
                                sb.append(",");
                            }
                        }
                        cv.put(APN_TYPE, sb.toString());
                        cr.update(APN_CONTENT_URI, cv, APN_ID_QUERY, new String[] {s_apnId});
                    }
                }
                Utils.Log.d(LOG_TAG, "Enabled: APN Type : " + s_type + s_apnId);
            }
            apnCursor.moveToNext();
        }
    }

    private void disableApns(Context ctx) {
        Cursor apnCursor = getActiveNwApn();
        if(apnCursor != null) {
            disableApns(ctx.getContentResolver(), apnCursor);
            apnCursor.close();
        }else {
            Utils.Log.e(LOG_TAG, "disableApns: apncursor is null, abort..");
        }
    }

   /*disable/enable data connection, start */
   // for Social, we following CDMA solution to enable / disable data connection
   // call PhoneService interface to enable /disable data connectivity instead of hack APN
   // porting  the solution from Clagary

    private void disableForCdma() {
        ITelephony ps = getPhoneService(false);
        if (ps == null) {
            Utils.Log.i(LOG_TAG, Utils.getCurrentTime() +
                    " Toggler:Disable: acquire PhoneService failed.");
            return;
        }

        for (int retry = 0; retry < 2; retry++) {
             try {
                 ps.disableDataConnectivity();
             } catch (RemoteException e) {
                Utils.Log.d(LOG_TAG, Utils.getCurrentTime() + " ===> PhoneService.disableDataConnectivity():failed" + e);
             }
        }
     
        Utils.Log.i(LOG_TAG, Utils.getCurrentTime() + 
                    " Toggler:Disable: call PhoneService.disableDataConnectivity().");
    }
    
    private void enableForCdma() {
        ITelephony ps = getPhoneService(false);
        if (ps == null) {
            Utils.Log.i(LOG_TAG, Utils.getCurrentTime() +
                     " Toggler:enable: acquire PhoneService failed.");
            return;
        }

     for (int retry = 0; retry < 2; retry++) {
         try {
              ps.enableDataConnectivity();
            } catch (RemoteException e) {
                    Utils.Log.d(LOG_TAG, Utils.getCurrentTime() + " ===> PhoneService.enableDataConnectivity():failed" + e);
            }
     }

     Utils.Log.i(LOG_TAG, Utils.getCurrentTime() + 
                " Toggler:enable: call PhoneService.enableDataConnectivity().");
    }
/*disable/enable data connection, end */

    private void disableApns(ContentResolver cr, Cursor apnCursor) {
        ContentValues cv = new ContentValues();
        int idIdx = apnCursor.getColumnIndex(APN_ID);
        int typeIdx = apnCursor.getColumnIndex(APN_TYPE);
        apnCursor.moveToFirst();
        int count = apnCursor.getCount();
        for(int i = 0; i < count; ++i) {
            cv.clear();
            String s_apnId = apnCursor.getString(idIdx);
            String s_type = apnCursor.getString(typeIdx);

            if ( s_type == null ) {
                cv.put(APN_TYPE, APN_DISABLE);
                cr.update(APN_CONTENT_URI, cv, APN_ID_QUERY, new String[] {s_apnId});
                //Utils.Log.d(LOG_TAG, "Disabled: APN Type null : " + s_apnId);
            }else if ( !s_type.contains(APN_DISABLE) && (!s_type.contains("mms") || s_type.contains("default"))) {
                if(!s_type.contains(",")) { // single type, simple case
                    cv.put(APN_TYPE, s_type + APN_DISABLE );
                    cr.update(APN_CONTENT_URI, cv, APN_ID_QUERY, new String[] {s_apnId});
                }else {
                    String[] types = s_type.split(",");
                    if(types == null) {
                        // should never happen
                        cv.put(APN_TYPE, s_type + APN_DISABLE );
                        cr.update(APN_CONTENT_URI, cv, APN_ID_QUERY, new String[] {s_apnId});
                    }else {
                        int len = types.length;
                        StringBuilder sb = new StringBuilder();
                        for(int j = 0; j < len; ++j) {
                            sb.append(types[j] + APN_DISABLE);
                            if(j != (len - 1)) {
                                // for everything but last, append the comma back
                                sb.append(",");
                            }
                        }
                        cv.put(APN_TYPE, sb.toString());
                        cr.update(APN_CONTENT_URI, cv, APN_ID_QUERY, new String[] {s_apnId});
                    } 
                }
                Utils.Log.d(LOG_TAG, "Disabled: APN Type : " + s_type + s_apnId);
            }
            apnCursor.moveToNext();
        }
    }

    private void restoreApns(Context ctx) {
        ContentResolver cr = ctx.getContentResolver();
        ContentValues cv = new ContentValues();
        Cursor apnCursor = cr.query(APN_CONTENT_URI, new String[]{APN_ID, APN_TYPE}, null, null, null);
        apnCursor.moveToFirst();
        int count = apnCursor.getCount();

        String preferapntype = null;

        int idIdx = apnCursor.getColumnIndex(APN_ID);
        int typeIdx = apnCursor.getColumnIndex(APN_TYPE);

        for(int i = 0; i < count; ++i) {
            cv.clear();
            String s_apnId = apnCursor.getString(idIdx);
            String s_type = apnCursor.getString(typeIdx);

            if ( s_type != null )	{
                if (s_type.contains(APN_TYPE_EMPTY)) {
                    cv.put(APN_TYPE, "");
                    cr.update(APN_CONTENT_URI, cv, APN_ID_QUERY, new String[] {s_apnId});
                } else if (s_type.contains(APN_TYPE_STAR)) {
                    cv.put(APN_TYPE, "*");
                    cr.update(APN_CONTENT_URI, cv, APN_ID_QUERY, new String[] {s_apnId});
                } else if(s_type.contains(APN_DISABLE)) {
                    String [] s_str = s_type.split(APN_DISABLE);
                    String actual_apn_type = "";

                    if((s_str != null) && (s_str.length > 0)) {
                        int len = s_str.length;
                        StringBuilder sb = new StringBuilder();
                        for(int j = 0; j < len; ++j) {
                            sb.append(s_str[j]);
                        } 
                        actual_apn_type = sb.toString();
                    }

                    cv.put(APN_TYPE, actual_apn_type);
                    cr.update(APN_CONTENT_URI, cv, APN_ID_QUERY, new String[] {s_apnId});
                    Utils.Log.d(LOG_TAG, "Enabled: " + actual_apn_type + s_apnId);
                }
            }
            apnCursor.moveToNext();
        }
        apnCursor.close();
    }

   private void updateDataConnection(int value) {
        SharedPreferences myPref = mContext.getSharedPreferences(BatteryProfile.OPTIONS_STORE,
                    Context.MODE_PRIVATE);
        SharedPreferences.Editor myPrefEditor = myPref.edit();
        myPrefEditor.putInt(BatteryProfile.KEY_DATA_CONNECTION_STATE, value);
        myPrefEditor.commit();
    }

    private void sendBMStateChangeBroadCast(int state) {
        Intent bcIntent = new Intent(BatteryManagerDefs.ACTION_BM_STATE_CHANGED);
        if (state == BatteryProfile.DATA_CONN_OFF) {
            bcIntent.putExtra(BatteryManagerDefs.KEY_DATA_CONNECTION, DataConnection.OFF);
        } else {
            bcIntent.putExtra(BatteryManagerDefs.KEY_DATA_CONNECTION, DataConnection.ON);
        }

        SharedPreferences myPref = mContext.getSharedPreferences(BatteryProfile.OPTIONS_STORE,
                    Context.MODE_PRIVATE);
        int prefMode = myPref.getInt(BatteryProfile.KEY_OPTION_PRESET_MODE,
                BatteryProfile.DEFAULT_PRESET_MODE);
        boolean isPreset = myPref.getBoolean(BatteryProfile.KEY_OPTION_IS_PRESET,
                BatteryProfile.DEFAULT_OPTION_SELECT);

        // Map here to avoid confusion
        int mode = 0;
        if (isPreset) {
            if (prefMode == BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE) {
                mode = Mode.PERFORMANCE;
            } else if (prefMode == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
                mode = Mode.NIGHT_SAVER;
            } else if (prefMode == BatteryProfile.OPTION_PRESET_MAXSAVER_MODE) {
                mode = Mode.BATTERY_SAVER;
            }
        } else {
            mode = Mode.CUSTOM;
        }
        bcIntent.putExtra(BatteryManagerDefs.KEY_BM_MODE, mode);
        mContext.sendBroadcast(bcIntent);
    }

    private void disableForNonCdma() {
        mThreadManager.submit(new Runnable() {
            public void run() {
                synchronized(sLock) {
                    PowerManager.WakeLock runWL = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
                    runWL.acquire();
                    disableApns(mContext);
                    updateDataConnection(BatteryProfile.DATA_CONN_OFF);
                    sendBMStateChangeBroadCast(BatteryProfile.DATA_CONN_OFF);
                    saveDisableTime();
                    Utils.Log.d(LOG_TAG, Utils.getCurrentTime() + " Toggler:Disable: Apns are now disabled");
                    runWL.release();
                }
            }
        });
    }

    private void enableForNonCdma() {
        mThreadManager.submit(new Runnable() {
            public void run() {
                synchronized(sLock) {
                    PowerManager.WakeLock runWL = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
                    runWL.acquire();
                    restoreApns(mContext);
                    updateDataConnection(BatteryProfile.DATA_CONN_ON);
                    sendBMStateChangeBroadCast(BatteryProfile.DATA_CONN_ON);
                    saveEnableTime();
                    Utils.Log.d(LOG_TAG, Utils.getCurrentTime() +
                            " Toggler:Enable: Apns are now enabled");
                    runWL.release();
                }
            }
        });
    }

    private ITelephony getPhoneService(boolean doSleep) {
        for (int retry = 0; retry < 2; retry++) {
            ITelephony phoneService =
                    ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            if (phoneService != null) {
                return phoneService;
            } else if (!doSleep) return null;
            // PhoneService is not active atpresent, sleep for a ms and retry.
            SystemClock.sleep(5000); //5sec 
        }
        return null;
    }

    public void enableApnType(String type) {
        class myThread extends Thread {
            private String mType;

            public myThread(String type) { mType = type; }

            public void run() {
                Utils.Log.d(LOG_TAG, Utils.getCurrentTime() + " ===> DataToggler:EnableApnType ()");
                if (TelephonyManager.getDefault().getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
                   // Not supported
                   return;
                } else {
                   Cursor apnCursor = getActiveNwApn();
                   if(apnCursor != null) {
                       enableApnType(apnCursor, mType);
                       apnCursor.close();
                   }else {
                       Utils.Log.e(LOG_TAG, "enableApnType: apncursor is null, abort..");
                       return;
                   }
               }
               Utils.Log.d(LOG_TAG, Utils.getCurrentTime() + " ===> DataToggler:EnableApnType succeed ()");
            }
        }
        mThreadManager.submit(new myThread(type));
    }

    public void disable() {
        Utils.Log.d(LOG_TAG, Utils.getCurrentTime() + " ===> DataToggler:disable ()");
        if (TelephonyManager.getDefault().getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            disableForCdma();
            updateDataConnection(BatteryProfile.DATA_CONN_OFF);
            sendBMStateChangeBroadCast(BatteryProfile.DATA_CONN_OFF);
            saveDisableTime();
        } else {
            disableForNonCdma();
        }

        Utils.Log.d(LOG_TAG, Utils.getCurrentTime() + " ===> DataToggler:disable succeed ()");
    }

    public void enable(boolean cdma) {
        if (TelephonyManager.getDefault().getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            if(cdma)
                enable();
        }
        else {
            if(!cdma)
                enable();
        }
    }

    public void enable() {
        Utils.Log.d(LOG_TAG, Utils.getCurrentTime() + " ===> DataToggler:Enable ()");
        if (TelephonyManager.getDefault().getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            enableForCdma();
            updateDataConnection(BatteryProfile.DATA_CONN_ON);
            sendBMStateChangeBroadCast(BatteryProfile.DATA_CONN_ON);
            saveEnableTime();
        } else {
            enableForNonCdma();
        }

        Utils.Log.d(LOG_TAG, Utils.getCurrentTime() + " ===> DataToggler:Enable succeed ()");
    }

    public DataToggler(Context ctx) {
        mContext = ctx;		
        mPref = mContext.getSharedPreferences(BatteryProfile.LOG_STORE, 
                                                Context.MODE_WORLD_READABLE);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        mThreadManager = Executors.newSingleThreadExecutor();
    }

    public boolean isDataConnected(){
        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
	if(tm!=null){
		int dataState = tm.getDataState();
		if(dataState==TelephonyManager.DATA_DISCONNECTED)
			return false;
	}
	return true;
    }

    private void saveDisableTime() {
        mDisableTime = System.currentTimeMillis() / 1000;
        mDisableLevel = Utils.getBattCapacity();

        SharedPreferences.Editor myEditor = mPref.edit();

        int val = mPref.getInt(BatteryProfile.KEY_LOG_INDEX, 0);

        myEditor.putString(BatteryProfile.LOG_KEYS_ARRAY[val], 
                            new String(Utils.getCurrentTime() +
                           ", Battery Level:" + mDisableLevel + "% --- "));
        myEditor.commit();
    }

    private void saveEnableTime() {
        long now = System.currentTimeMillis() / 1000;
        String nowLevel = Utils.getBattCapacity();

        SharedPreferences.Editor myEditor = mPref.edit();

        int val = mPref.getInt(BatteryProfile.KEY_LOG_INDEX, 0);
        String str = mPref.getString(BatteryProfile.LOG_KEYS_ARRAY[val], null);

        if((str == null) || !str.endsWith("--- ")) {
            return;
        }

        myEditor.putString(BatteryProfile.LOG_KEYS_ARRAY[val], 
                            new String(str + Utils.getCurrentTime() + 
                           ", Battery Level:" + nowLevel + "%,\n"));
        val++;
        if(val >= BatteryProfile.LOG_KEYS_ARRAY.length) {
            val = 0;
        }
        myEditor.putInt(BatteryProfile.KEY_LOG_INDEX, val);
        myEditor.commit();

        // Log the disable-enable pair too - only when its the disable/enable flow
        if(mDisableTime != 0) {
            Utils.Log.i(DevStatPrefs.CHECKIN_EVENT_ID, "[ID=BMStats;ver=" + DevStatPrefs.VERSION +
                    ";time=" + now + ";dis=" + mDisableTime + ";dblvl=" + mDisableLevel +
                    ";en=" + now + ";eblvl=" + nowLevel + ";]");
            mDisableTime = 0;
        }
    }
}

