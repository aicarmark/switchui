/**
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: BatteryProfile.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date               Author      Comments
 * Feb 17, 2010       A24178      Created file
 * Apr 10, 2010       A24178      IKMAPFOUR-523
 * Jul 13, 2010       A16462      IKSTABLETWO-2784: Dynamic Data Mode change
 **********************************************************
 */

package com.motorola.batterymanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.DialogInterface;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.widget.RadioButton;

//import com.motorola.batterymanager.R;
import com.motorola.batterymanager.BatteryManagerDefs;
import com.motorola.batterymanager.BatteryManagerDefs.DataConnection;
import com.motorola.batterymanager.BatteryManagerDefs.Mode;

public class BatteryProfileUi extends PreferenceActivity implements 
        MyPreference.MyPreferenceClickListener, DialogInterface.OnClickListener,
        DialogInterface.OnDismissListener {
    
    // Constants - Local
    private final static String LOG_TAG = "BatteryProfileUi";
    private final static int REMOTE_ALERT_DLG_ID = 0;

    // Local objects - keep this to a minimum
    private PreferenceManager mPreferenceManager;
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mPreferenceEditor;
    private int mCurrMode;
    private boolean mCurrIsPreset;
    
    // Could keep these static - but thats too hacky
    private int mTempMode = -1;
    private boolean mTempIsPreset;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        getListView().setItemsCanFocus(true);
        mPreferenceManager = getPreferenceManager();
        mPreferences = getSharedPreferences(BatteryProfile.OPTIONS_STORE, Context.MODE_PRIVATE);
        mPreferenceEditor = mPreferences.edit();
        
        initLayouts(savedInstanceState);

        // Notification Change - Remove the status bar first time boot notification
        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
    }

    @Override
    public void onSaveInstanceState(Bundle saveStore) {
        if(mTempMode != -1) {
            saveStore.putBoolean(UiConstants.UI_BACKUP_PRESET, mTempIsPreset);
            saveStore.putInt(UiConstants.UI_BACKUP_MODE, mTempMode);
        }
        super.onSaveInstanceState(saveStore);
    }
    
    private void initLayouts(Bundle parcel) {
        MyPreference myPref = null;
	 if(parcel == null)
	 {
		Utils.Log.d(LOG_TAG, "BatteryProfileUi initObjects  parcel === null" );

	 }
        
        boolean isPreset = mPreferences.getBoolean(BatteryProfile.KEY_OPTION_IS_PRESET, 
                BatteryProfile.DEFAULT_OPTION_SELECT);
        int mode = mPreferences.getInt(BatteryProfile.KEY_OPTION_PRESET_MODE, 
                BatteryProfile.DEFAULT_PRESET_MODE);
        
        myPref = (MyPreference) 
                mPreferenceManager.findPreference(UiConstants.UI_PREF_KEY_MAXBATT);
        if(myPref != null) {
            if(isPreset && mode == BatteryProfile.OPTION_PRESET_MAXSAVER_MODE) {
                myPref.setRadioChecked(true);
            }
            myPref.setOnClickListener(this);
        }
        myPref = (MyPreference) 
                mPreferenceManager.findPreference(UiConstants.UI_PREF_KEY_NIGHTTIME);
        if(myPref != null) {
            if(isPreset && mode == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
                myPref.setRadioChecked(true);
            }
            myPref.setOnClickListener(this);
        }
        myPref = (MyPreference) 
                mPreferenceManager.findPreference(UiConstants.UI_PREF_KEY_PERFMODE);
        if(myPref != null) {
            if(isPreset && mode == BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE) {
                myPref.setRadioChecked(true);
            }
            myPref.setOnClickListener(this);
        }
        myPref = (MyPreference) 
                mPreferenceManager.findPreference(UiConstants.UI_PREF_KEY_CUSTOMS);
        if(myPref != null) {
            if(!isPreset) {
                myPref.setRadioChecked(true);
            }
            myPref.setOnClickListener(this);
        }
        mCurrMode = mode;
        mCurrIsPreset = isPreset;

        // In case of restart with dialog on top, restore temps
        if(parcel != null) {
            mTempMode = parcel.getInt(UiConstants.UI_BACKUP_MODE, -1);
            mTempIsPreset = parcel.getBoolean(UiConstants.UI_BACKUP_PRESET, false);
        }
    }

    /* (non-Javadoc)
     * @see com.test.apnstyle.MyPreference.MyPreferenceClickListener#onClick(java.lang.String, android.view.View)
     */
    public void onClick(String key, View view) {
        switch(view.getId()) {
        case R.id.radio:
        case R.id.ptext: // intentional fall-through
            int mode = -1;
            boolean isPreset = true;
            if(key.equals(UiConstants.UI_PREF_KEY_MAXBATT)) {
                mode = BatteryProfile.OPTION_PRESET_MAXSAVER_MODE;
            }else if(key.equals(UiConstants.UI_PREF_KEY_NIGHTTIME)) {
                mode = BatteryProfile.OPTION_PRESET_NTSAVER_MODE;
            }else if(key.equals(UiConstants.UI_PREF_KEY_PERFMODE)) {
                mode = BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE;
            }else if(key.equals(UiConstants.UI_PREF_KEY_CUSTOMS)) {
                mode = BatteryProfile.OPTION_PRESET_NTSAVER_MODE;
                isPreset = false;
            }else {
                Utils.Log.e(LOG_TAG, "onClick: key not recognized - " + key);
                return;
            }
            // If the same mode is clicked again - nothing to do
            if((mode == mCurrMode) && (isPreset == mCurrIsPreset)) {
                return;
            }

            if(mode != BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE) {
                // By-product of not changing directly on select
                // We need to clear the button check here so that its 
                // applied on confirmation
                MyPreference myPref = null;


                myPref = (MyPreference)
                        mPreferenceManager.findPreference(key);
                myPref.setRadioChecked(false);
                //RadioButton rb = (RadioButton)view;
                //rb.setChecked(false);

                mTempMode = mode;
                mTempIsPreset = isPreset;
                showDialog(REMOTE_ALERT_DLG_ID);
            }else {
                // Perf mode is always OK :)
                modUi(mode, isPreset);
                commitAndNotifyService(mode, isPreset);
                mTempMode = -1;
            }
            break;
        case R.id.infoimg:
            Intent intent = new Intent();
            if(key.equals(UiConstants.UI_PREF_KEY_MAXBATT)) {
                intent.putExtra(UiConstants.UI_MODE_KEY, 
                        BatteryProfile.OPTION_PRESET_MAXSAVER_MODE);
                intent.setClass(this, ModeInfoUi.class);
            }else if(key.equals(UiConstants.UI_PREF_KEY_NIGHTTIME)) {
                intent.putExtra(UiConstants.UI_MODE_KEY, 
                        BatteryProfile.OPTION_PRESET_NTSAVER_MODE);
                intent.setClass(this, ModeInfoUi.class);
            }else if(key.equals(UiConstants.UI_PREF_KEY_PERFMODE)) {
                intent.putExtra(UiConstants.UI_MODE_KEY, 
                        BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE);
                intent.setClass(this, ModeInfoUi.class);
            }else if(key.equals(UiConstants.UI_PREF_KEY_CUSTOMS)) {
                intent.setClass(this, CustomSettingsUi.class);
            }else {
                Utils.Log.e(LOG_TAG, "onClick: key not recognized - " + key);
                return;
            }
            startActivity(intent);
            break;
        default:
            Utils.Log.e(LOG_TAG, "onClick: view Id not recognized");
            break;
        }
    }

    /**
     * @param mode
     * @param isPreset
     */
    private void modUi(int mode, boolean isPreset) {
        if(mCurrMode != mode || mCurrIsPreset != isPreset) {
            // First clear the prev checked button
            String key = null;
            if(mCurrIsPreset) {
                if(mCurrMode == BatteryProfile.OPTION_PRESET_MAXSAVER_MODE) {
                    key = UiConstants.UI_PREF_KEY_MAXBATT;
                }else if(mCurrMode == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
                    key = UiConstants.UI_PREF_KEY_NIGHTTIME;
                }else if(mCurrMode == BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE) {
                    key = UiConstants.UI_PREF_KEY_PERFMODE;
                }
            }else {
                key = UiConstants.UI_PREF_KEY_CUSTOMS;
            }
            // 2011.11.22 jrw modified to fix cr 4523
//            MyPreference myPref = (MyPreference)mPreferenceManager.findPreference(key);
//            myPref.setRadioChecked(false);
            Preference prePref = mPreferenceManager.findPreference(key);
            if (prePref instanceof MyPreference){
                MyPreference myPref = (MyPreference)prePref;
                myPref.setRadioChecked(false);
            }
            // 2011.11.22 jrw modified end
            
            if(isPreset) {
                if(mode == BatteryProfile.OPTION_PRESET_MAXSAVER_MODE) {
                    key = UiConstants.UI_PREF_KEY_MAXBATT;
                }else if(mode == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
                    key = UiConstants.UI_PREF_KEY_NIGHTTIME;
                }else if(mode == BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE) {
                    key = UiConstants.UI_PREF_KEY_PERFMODE;
                }
            }else {
                key = UiConstants.UI_PREF_KEY_CUSTOMS;
            }
            // 2011.11.22 jrw modified to fix cr 4523
//            myPref = (MyPreference)mPreferenceManager.findPreference(key);
//            myPref.setRadioChecked(true);
            Preference nowPref = mPreferenceManager.findPreference(key);
            if (nowPref instanceof MyPreference){
                MyPreference myPref = (MyPreference)nowPref;
                myPref.setRadioChecked(true);
            }
            // 2011.11.22 jrw modified end
            mCurrMode = mode;
            mCurrIsPreset = isPreset;
        }else {
            return;
        }
    }

    /**
     * @param mode
     * @param isPreset
     */
    private void commitAndNotifyService(int mode, boolean isPreset) {
        mPreferenceEditor.putBoolean(BatteryProfile.KEY_OPTION_IS_PRESET, isPreset);
        mPreferenceEditor.putInt(BatteryProfile.KEY_OPTION_PRESET_MODE, mode);
        sendBMStateChangeBroadCast(mode, isPreset);
        if(mode == BatteryProfile.OPTION_PRESET_MAXSAVER_MODE) {
            mPreferenceEditor.putInt(
                    BatteryProfile.KEY_OPTION_PRESET_MAXSAVER_DISPLAY_BRIGHTNESS,
                    BatteryProfile.DEFAULT_PRESET_MAXSAVER_DISPLAY_BRIGHTNESS);
        }

        if(!isPreset) {
            commitCustoms(mPreferenceEditor);
        }else if(mode == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
            commitPresets(mPreferenceEditor);
        }

        mPreferenceEditor.commit();
        notifyService();
    }

    private void sendBMStateChangeBroadCast(int mode, boolean isPreset) {
        Intent bcIntent = new Intent(BatteryManagerDefs.ACTION_BM_STATE_CHANGED);
        int retMode=0;
        if (isPreset) {
            if (mode == BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE) {
                retMode = Mode.PERFORMANCE;
            } else if (mode == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
                retMode = Mode.NIGHT_SAVER;
            } else if (mode == BatteryProfile.OPTION_PRESET_MAXSAVER_MODE) {
                retMode = Mode.BATTERY_SAVER;
            }
        } else {
            retMode = Mode.CUSTOM;
        }
        bcIntent.putExtra(BatteryManagerDefs.KEY_BM_MODE, retMode);

        int state = mPreferences.getInt(BatteryProfile.KEY_DATA_CONNECTION_STATE,
                    BatteryProfile.DEF_DATA_CONN_STATE);
        bcIntent.putExtra(BatteryManagerDefs.KEY_DATA_CONNECTION,
                (state==BatteryProfile.DATA_CONN_OFF)?DataConnection.OFF:DataConnection.ON);
        sendBroadcast(bcIntent);
    }

    /**
     * 
     */
    private void notifyService() {
        Intent svcIntent = new Intent(PowerProfileSvc.SVC_START_ACTION)
                                .setClass(this, PowerProfileSvc.class);
        startService(svcIntent);
    }

    private void commitCustoms(SharedPreferences.Editor editor) {
        // This is a really bad way to do things - having two persistent
        // stores for the same values, and arises out of two things
        // 1. Code - A need to keep the service simple and not crowded with
        //           reading from a thousand stores
        // 2. UI   - User wants to be able to change customs and have it
        //           remembered without applying custom mode
        // TODO - (Unlikely) - find a better way of doing this

        // Always read from temp store for custom preferences
        SharedPreferences custPref = getSharedPreferences(BatteryProfile.TEMP_OPTIONS_STORE,
                MODE_PRIVATE);
        int tempSmartBattTimeout = mPreferences.getInt(
                BatteryProfile.KEY_OPTION_CUSTOM_PEAK_DATATIMEOUT,
                BatteryProfile.DEFAULT_PEAK_DATATIMEOUT);
        int tempSmartBattOffpeakTimeout = mPreferences.getInt(
                BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_DATATIMEOUT,
                BatteryProfile.DEFAULT_OFFPEAK_DATATIMEOUT);
        int tempSmartOffpeakStart = mPreferences.getInt(
                BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_START,
                BatteryProfile.DEFAULT_OFFPEAK_START);
        int tempSmartOffpeakEnd = mPreferences.getInt(
                BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_END,
                BatteryProfile.DEFAULT_OFFPEAK_END);
        int tempDisplayBrightness = mPreferences.getInt(
                BatteryProfile.KEY_OPTION_CUSTOM_DISPLAY_BRIGHTNESS,
                BatteryProfile.DEFAULT_DISPLAY_BRIGHTNESS); 

        // reflect the custom prefs -> actual store
        editor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_START,
                tempSmartOffpeakStart);
        editor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_END,
                tempSmartOffpeakEnd);
        editor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_DATATIMEOUT,
                tempSmartBattOffpeakTimeout);
        editor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_PEAK_DATATIMEOUT,
                tempSmartBattTimeout);
        editor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_DISPLAY_BRIGHTNESS,
                tempDisplayBrightness);
    }

    private void commitPresets(SharedPreferences.Editor editor) {
        editor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_START,
                BatteryProfile.DEFAULT_PRESET_ALLDAY_OFFPEAK_START);
        editor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_END,
                BatteryProfile.DEFAULT_PRESET_ALLDAY_OFFPEAK_END);
        editor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_DATATIMEOUT,
                BatteryProfile.DEFAULT_PRESET_ALLDAY_OFFPEAK_DATATIMEOUT);
        editor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_PEAK_DATATIMEOUT,
                BatteryProfile.DEFAULT_PRESET_ALLDAY_PEAK_DATATIMEOUT);
        editor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_DISPLAY_BRIGHTNESS,
                BatteryProfile.DEFAULT_PRESET_ALLDAY_DISPLAY_BRIGHTNESS);
    }

    @Override
    public Dialog onCreateDialog(int dlgId) {
        if(dlgId == REMOTE_ALERT_DLG_ID) {
            AlertDialog.Builder myDlgBuilder = new AlertDialog.Builder(this);
            myDlgBuilder.setMessage(R.string.remote_mgmt_text);
            myDlgBuilder.setPositiveButton(android.R.string.ok, this);
            myDlgBuilder.setNegativeButton(android.R.string.cancel, this);
            myDlgBuilder.setTitle(R.string.remote_mgmt_title);
            myDlgBuilder.setIcon(android.R.drawable.ic_dialog_alert);
            AlertDialog alertDlg = myDlgBuilder.create();
            alertDlg.setOnDismissListener(this);
            return alertDlg;
        }
        return null;
    }

    public void onClick(DialogInterface dlg, int which) {
        if(which == DialogInterface.BUTTON_POSITIVE) {
            // We are done in this case, user is OK with alert
            modUi(mTempMode, mTempIsPreset);
            commitAndNotifyService(mTempMode, mTempIsPreset);
            mTempMode = -1;
            dlg.dismiss();
        }else if(which == DialogInterface.BUTTON_NEGATIVE) {
            mTempMode = -1;
            dlg.dismiss();
        }
    }

    public void onDismiss(DialogInterface dialog) {
        mTempMode = -1;
    }
}
