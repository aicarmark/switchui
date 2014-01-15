/**
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: PowerProfileNotifyUi.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 02-15-10       A24178       Created file
 *                -Ashok
 **********************************************************
 */

package com.motorola.batterymanager;

import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TimePicker;

import android.view.MenuInflater;

import com.motorola.batterymanager.R;

public class CustomSettingsUi extends Activity {
    
    // Note - 1
    // We will not remember option states over activity pause
    // - If user moves out of screen before selecting OK/Cancel
    //   we will fallback to previous persistent state on resume
    // - Simplifies the logic to not have a temp persistent store
    //   which is bad in any case for settings screens
    //   Well - the above design got thrown out the window
    
    // Note - 2 Dialogs
    // We will not let dialogs stay on top if we are "pause"d
    // When we resume, we start with what is the last saved persistent
    // state
    
    // Note - 3 Orientation Change
    // In contradiction to 1, we need to preserve states on orientation
    // change which given Android's way of handling the condition is a bit
    // difficult, so until we come up with a good way of handling this, HACK
    
    private final static String LOG_TAG = "CustomSettingsUi";
    private final static int N_VALUES = 4;
    private final static int DISPLAY_MAX = DisplayControl.MAX_BRIGHTNESS;
    // 2011.11.30 jrw647 modified to fix cr 4733
    private final static int DISPLAY_OFFSET = 0; //DisplayControl.OFFSET;
    private final static int BRIGHT_OFFSET = 30;
    // 2011.11.30 jrw647 modified end
    // 2011.12.05 jrw647 added to fix cr 4890
    private final static int DEFAULT_BRIGHT = 65;
    // 2011.12.05 jrw647 added end
    private SharedPreferences mPreferences;
    private SharedPreferences.Editor mPreferenceEditor;
    private CustomsScreenHandler mCustomUiHandler;
    private boolean mAutoBrightnessOn;
    
    // Temp Objects
    private int mTempSmartBattTimeout;
    private int mTempSmartBattOffpeakTimeout;
    private int mTempSmartOffpeakStart;
    private int mTempSmartOffpeakEnd;
    private int mTempDisplayBrightness;
    private int mTempBackupBrightness;
    
    // Dialogs
    private final static int START_TIME_DLG_ID = 0;
    private final static int END_TIME_DLG_ID = 1;
    private final static int OFFPEAK_TIMEOUT_DLG_ID = 2;
    private final static int PEAK_TIMEOUT_DLG_ID = 3;
    private int mCurrentDialog;
    
    // For faster access
    private MyListView mListView;
    private String[] mValues = new String[N_VALUES];
    private Integer[] mIntValues = new Integer[N_VALUES];
    int[] mDataTimeoutValues;
    
    // TODO Hack
    private boolean hisRestart;
    private Menu mMenu = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customs_child);
        
        initObjects(savedInstanceState);
    }
    
    /**
     * 
     */
    private void initObjects(Bundle parcel) {
        mDataTimeoutValues = this.getResources().getIntArray(R.array.batt_timeout_values);
        mPreferences = getSharedPreferences(BatteryProfile.TEMP_OPTIONS_STORE, MODE_PRIVATE);
        mPreferenceEditor = mPreferences.edit();
        
        // We create these here so that they are 
        // available without the need to create at every screen switch
        mCustomUiHandler = new CustomsScreenHandler();
        
        // TODO Hack - Orientation change
        if(parcel != null) {
            hisRestart = true;
	     Utils.Log.d(LOG_TAG, "initObjects  parcel != null" );
            mTempSmartBattTimeout = parcel.getInt(
                    BatteryProfile.KEY_OPTION_CUSTOM_PEAK_DATATIMEOUT, 
                    BatteryProfile.DEFAULT_PEAK_DATATIMEOUT); 
            mTempSmartBattOffpeakTimeout = parcel.getInt(
                    BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_DATATIMEOUT,
                    BatteryProfile.DEFAULT_OFFPEAK_DATATIMEOUT);
            mTempSmartOffpeakStart = parcel.getInt(
                    BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_START,
                    BatteryProfile.DEFAULT_OFFPEAK_START);
            mTempSmartOffpeakEnd = parcel.getInt(
                    BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_END,
                    BatteryProfile.DEFAULT_OFFPEAK_END);
            mTempDisplayBrightness = parcel.getInt(
                    BatteryProfile.KEY_OPTION_CUSTOM_DISPLAY_BRIGHTNESS,
                    BatteryProfile.DEFAULT_DISPLAY_BRIGHTNESS);
            mTempBackupBrightness = parcel.getInt(
                    BatteryProfile.KEY_BACKUP_DISPLAY_BRIGHTNESS,
                    BatteryProfile.DEFAULT_DISPLAY_BRIGHTNESS);
        }else {
            // 2011.12.05 jrw647 added to fix cr 4890
            mTempDisplayBrightness = DEFAULT_BRIGHT;
            // 2011.12.05 jrw647 added end
            mTempBackupBrightness = 
                    DisplayControl.getBackupBrightness(getApplicationContext());
			Utils.Log.d(LOG_TAG, "initObjects  parcel === null mTempBackupBrightness = " + mTempBackupBrightness);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Get what we need to show and start off views    
        int screenToShow = UiConstants.UI_MAIN_CUSTOM_SCREEN;

        setupOptions(screenToShow);
        
        // TODO Hack - Orientation change
        hisRestart = false;
        Utils.Log.v(LOG_TAG, "onResume ends");
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // No dialogs showing while we are not on top
        removeDialog(START_TIME_DLG_ID);
        removeDialog(END_TIME_DLG_ID);
        removeDialog(OFFPEAK_TIMEOUT_DLG_ID);
        removeDialog(PEAK_TIMEOUT_DLG_ID);
        if (isScreanOn()) { // 2011.12.13 jrw647 added to fix cr 5143
            if(!DisplayControl.smartStateApplied()) {
                notifyDisplay(false);
            }
        }
        Utils.Log.v(LOG_TAG, "txvd onPause ");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // No dialogs showing while we are not on top
	Utils.Log.v(LOG_TAG, "txvd onStop ");
    }

   @Override
    protected void onRestart() {
        super.onRestart();
        // No dialogs showing while we are not on top
	Utils.Log.v(LOG_TAG, "txvd onRestart ");
    }

     @Override
    protected void onDestroy() {
        super.onDestroy();
        // No dialogs showing while we are not on top
	Utils.Log.v(LOG_TAG, "txvd onDestroy ");
    }
		

    @Override
    protected void onSaveInstanceState(Bundle parcel) {
        parcel.putInt(BatteryProfile.KEY_OPTION_CUSTOM_PEAK_DATATIMEOUT, 
                mTempSmartBattTimeout); 
        parcel.putInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_DATATIMEOUT,
                mTempSmartBattOffpeakTimeout);
        parcel.putInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_START, 
                mTempSmartOffpeakStart);
        parcel.putInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_END, 
                mTempSmartOffpeakEnd);                
        parcel.putInt(BatteryProfile.KEY_OPTION_CUSTOM_DISPLAY_BRIGHTNESS, 
                mTempDisplayBrightness);
        parcel.putInt(BatteryProfile.KEY_BACKUP_DISPLAY_BRIGHTNESS, 
                mTempBackupBrightness);
    }

    /**
     * 
     */
    private void setupTemporaryObjects() {
        // Read options from persistent storage
        // The service semantics of user options and the way the user sees them are 
        // different, with temp objects we hold the values as understood by
        // the service

        mTempSmartBattTimeout = mPreferences.getInt(
                BatteryProfile.KEY_OPTION_CUSTOM_PEAK_DATATIMEOUT, 
                BatteryProfile.DEFAULT_PEAK_DATATIMEOUT); 
        mTempSmartBattOffpeakTimeout = mPreferences.getInt(
                BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_DATATIMEOUT,
                BatteryProfile.DEFAULT_OFFPEAK_DATATIMEOUT);
        mTempSmartOffpeakStart = mPreferences.getInt(
                BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_START,
                BatteryProfile.DEFAULT_OFFPEAK_START);
        mTempSmartOffpeakEnd = mPreferences.getInt(
                BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_END,
                BatteryProfile.DEFAULT_OFFPEAK_END);
        // 2011.12.05 jrw647 added to fix cr 4890
        if (mTempDisplayBrightness == DEFAULT_BRIGHT){
        // 2011.12.05 jrw647 added end
            mTempDisplayBrightness = mPreferences.getInt(
                    BatteryProfile.KEY_OPTION_CUSTOM_DISPLAY_BRIGHTNESS,
                    BatteryProfile.DEFAULT_DISPLAY_BRIGHTNESS);
        }
    }

    /**
     * 
     */
    private void setupOptions(int whichScreen) {
        // Re-initialize temp objects
        // TODO Hack - Orientation change
        if(!hisRestart) {
            setupTemporaryObjects();
        }
        
        if(whichScreen == UiConstants.UI_MAIN_CUSTOM_SCREEN) {
            View v2 = findViewById(R.id.ichild_custom);
            populateCustomScreen(v2);
          
            /* 
            Button b = (Button)v2.findViewById(R.id.custom_positive);
            b.setOnClickListener(mCustomUiHandler);
            b = (Button)v2.findViewById(R.id.custom_negative);
            b.setOnClickListener(mCustomUiHandler);
            */
        }        
    }

    /**
     * @param v2
     */
    private void populateCustomScreen(View v2) {
        // Auto-brightness setting handling
        mAutoBrightnessOn = getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);
        try {
            mAutoBrightnessOn = mAutoBrightnessOn && 
                    Settings.System.getInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        }catch(SettingNotFoundException snfEx) {
            mAutoBrightnessOn = false;
        }
        // Make sure base view is visible
        mCustomUiHandler.initViews(v2.findViewById(R.id.customs_list));
    }
   
    // Handles all UI for custom screen
    class CustomsScreenHandler extends ScreenResponseHandler implements OnTimeSetListener,
            DialogInterface.OnClickListener,
            DialogInterface.OnDismissListener {
        
        public void initViews(View parent) {
            mListView = (MyListView)parent;

            // For keypad access to views within rows
            mListView.setItemsCanFocus(true);
            
            // Off-peak values
            clearValues();
            mValues[0] = getTimeString(BatteryProfile.getDayHour(mTempSmartOffpeakStart),
                    BatteryProfile.getHourMins(mTempSmartOffpeakStart));
            mValues[1] = getTimeString(BatteryProfile.getDayHour(mTempSmartOffpeakEnd),
                    BatteryProfile.getHourMins(mTempSmartOffpeakEnd));
            mValues[2] = getTimeoutString(mTempSmartBattOffpeakTimeout);
            mListView.setOffPeakContent(mValues, false);
            
            // Peak values
            mListView.setPeakContent(getTimeoutString(mTempSmartBattTimeout), false);
            
            // Display content
            clearIntValues();
            mIntValues[0] = mTempDisplayBrightness;
            // 2011.11.30 jrw647 modified to fix cr 4733
            mIntValues[1] = (mTempBackupBrightness - BRIGHT_OFFSET) * DISPLAY_MAX / (DISPLAY_MAX - BRIGHT_OFFSET);
            // 2011.11.30 jrw647 modified end
            // mIntValues[1] = DisplayControl.getBackupBrightness(CustomSettingsUi.this.getApplicationContext());
            mIntValues[2] = DISPLAY_MAX - DISPLAY_OFFSET;
            mIntValues[3] = mAutoBrightnessOn ? 1 : 0;
            mListView.setProgressContent(mIntValues, true);

            if(!mAutoBrightnessOn) {
                // Also set brightness to what we have as the custom setting
                DisplayControl.changeBrightness(mTempDisplayBrightness, CustomSettingsUi.this.getApplicationContext());
            }
            
            mListView.setUpdateCallbacks(this);
        }

        /* (non-Javadoc)
         * @see com.test.spinner.ScreenResponseHandler#onClick(android.view.View)
         */
        public void onClick(View v) {
            int id = v.getId();
            if(id == R.id.custom_positive) {
                commitAndnotifyService(false);
                finish();
            }else if(id == R.id.custom_negative) {
                if(!DisplayControl.smartStateApplied()) {
                    notifyDisplay(false);
                }
                setupTemporaryObjects();
                finish();
            }else if(id == R.id.op_start_button) {
                showDialog(START_TIME_DLG_ID);
            }else if(id == R.id.op_end_button) {
                showDialog(END_TIME_DLG_ID);
            }else if(id == R.id.op_data_button) {
                showDialog(OFFPEAK_TIMEOUT_DLG_ID);
            }else if(id == R.id.p_data_button) {
                showDialog(PEAK_TIMEOUT_DLG_ID);
            }
        }

        /* (non-Javadoc)
         * @see com.test.spinner.ScreenResponseHandler#onProgressChanged(android.widget.SeekBar, int, boolean)
         */
        public void onProgressChanged(SeekBar seekBar, int progress,
                boolean fromUser) {
            if(fromUser) {
                mTempDisplayBrightness = progress + DISPLAY_OFFSET;
                updateProgress(progress);
                DisplayControl.changeBrightness(mTempDisplayBrightness, 
                        CustomSettingsUi.this.getApplicationContext());
            }
        }

        /* (non-Javadoc)
         * @see com.test.spinner.ScreenResponseHandler#onStartTrackingTouch(android.widget.SeekBar)
         */
        public void onStartTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
            
        }

        /* (non-Javadoc)
         * @see com.test.spinner.ScreenResponseHandler#onStopTrackingTouch(android.widget.SeekBar)
         */
        public void onStopTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
            
        }

        private void updateProgress(int progress) {
            clearIntValues();
            mIntValues[0] = progress;
            mListView.setProgressContent(mIntValues, false);
        }

        /* (non-Javadoc)
         * @see android.app.TimePickerDialog.OnTimeSetListener#onTimeSet(android.widget.TimePicker, int, int)
         */
        public void onTimeSet(TimePicker dialog, int hourOfDay, int minute) {
            switch(mCurrentDialog) {
            case START_TIME_DLG_ID:
                String text = getTimeString(hourOfDay, minute);
                mTempSmartOffpeakStart = (hourOfDay * 60) + minute;
                updateStartTime(text);
                Utils.Log.d(LOG_TAG, "TimePicker: StartTime: " + text);
                break;
            case END_TIME_DLG_ID:
                String etext = getTimeString(hourOfDay, minute);
                mTempSmartOffpeakEnd = (hourOfDay * 60) + minute;
                updateEndTime(etext);
                Utils.Log.d(LOG_TAG, "TimePicker: EndTime: " + etext);
                break;
            }
        }

        /**
         * @param etext
         */
        private void updateEndTime(String etext) {
            clearValues();
            mValues[1] = etext;
            mListView.setOffPeakContent(mValues, true);
        }

        /**
         * @param text
         */
        private void updateStartTime(String text) {
            clearValues();
            mValues[0] = text;
            mListView.setOffPeakContent(mValues, true);            
        }
        
        private void clearValues() {
            for(int i = 0; i < N_VALUES; ++i) {
                mValues[i] = null;
            }
        }
        
        private void clearIntValues() {
            for(int i = 0; i < N_VALUES; ++i) {
                mIntValues[i] = null;
            }
        }

        /* (non-Javadoc)
         * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
         */
        public void onClick(DialogInterface dI, int which) {
            switch(mCurrentDialog) {
            case OFFPEAK_TIMEOUT_DLG_ID:
                mTempSmartBattOffpeakTimeout = getTimeout(which);
                removeDialog(OFFPEAK_TIMEOUT_DLG_ID);
                updateOffpeakTimeout(mTempSmartBattOffpeakTimeout);
                break;
            case PEAK_TIMEOUT_DLG_ID:
                mTempSmartBattTimeout = getTimeout(which);
                removeDialog(PEAK_TIMEOUT_DLG_ID);
                updatePeakTimeout(mTempSmartBattTimeout);
                break;
            }
        }

        /**
         * @param timeout
         */
        private void updatePeakTimeout(int timeout) {
            mListView.setPeakContent(getTimeoutString(timeout), true);
        }

        /**
         * @param timeout
         */
        private void updateOffpeakTimeout(int timeout) {
            clearValues();
            mValues[2] = getTimeoutString(timeout);
            mListView.setOffPeakContent(mValues, true);
        }

        /* (non-Javadoc)
         * @see android.content.DialogInterface.OnDismissListener#onDismiss(android.content.DialogInterface)
         */
        public void onDismiss(DialogInterface dI) {
            // TODO Auto-generated method stub
            
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.xml.action_bar, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.btn_ok:
            commitAndnotifyService(false);
            finish();
            break;
        case R.id.btn_cancel:
            if(!DisplayControl.smartStateApplied()) {
                notifyDisplay(false);
            }
            setupTemporaryObjects();
            finish();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onConfigurationChanged (Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(mMenu != null) {
            try {
                mMenu.clear();
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.xml.action_bar, mMenu);
            } catch(Exception e) {
                Utils.Log.e(LOG_TAG, "Exception in configChanged: " + e);
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int Id) {
        switch(Id) {
        // Intentional fall-through
        case START_TIME_DLG_ID:
        case END_TIME_DLG_ID:
            TimePickerDialog timeDlg = new TimePickerDialog(this, mCustomUiHandler,
                    0, 0, DateFormat.is24HourFormat(this));
            return timeDlg;
        // Intentional fall-through
        case OFFPEAK_TIMEOUT_DLG_ID:
        case PEAK_TIMEOUT_DLG_ID:
            int resId;
            int listPosition;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.pref_user_max_timeout);
            if(Id == PEAK_TIMEOUT_DLG_ID) {
                resId = R.array.smart_batt_timeout_entries;
                listPosition = getListPosition(mTempSmartBattTimeout);
            }else {
                resId = R.array.smart_batt_offpeak_timeout_entries;
                listPosition = getListPosition(mTempSmartBattOffpeakTimeout);
            }
            builder.setSingleChoiceItems(resId, listPosition, mCustomUiHandler);
            builder.setNeutralButton(R.string.but_cancel_text, 
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            removeDialog(mCurrentDialog);
                        }
            });
            return builder.create();
        }
        return null;
    }
    
    /**
     * @param which
     * @return
     */
    public int getTimeout(int which) {
        return mDataTimeoutValues[which];
    }

    /**
     * @param timeout
     * @return
     */
    private int getListPosition(int timeout) {
        for(int i = 0; i < mDataTimeoutValues.length; ++i) {
            if(timeout == mDataTimeoutValues[i]) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param timeout
     * @return
     */
    public String getTimeoutString(int timeout) {
        if(timeout != -1) {
            //return (timeout < 60) ? 
            return (timeout + " " + getString(R.string.time_unit_minutes));
                    //timeout/60 + " " + getString(R.string.time_unit_hour);
        }else {
            return getString(R.string.never_timeout);
        }
    }

    @Override
    protected void onPrepareDialog(int Id, Dialog dlg) {
        mCurrentDialog = Id;
        switch(Id) {
        case START_TIME_DLG_ID:
            TimePickerDialog starttimeDlg = (TimePickerDialog)dlg;
            int startHour = BatteryProfile.getDayHour(mTempSmartOffpeakStart);
            int startMin = BatteryProfile.getHourMins(mTempSmartOffpeakStart);

            starttimeDlg.updateTime(startHour, startMin);
            break;
        case END_TIME_DLG_ID:
            TimePickerDialog endtimeDlg = (TimePickerDialog)dlg;
            int endHour = BatteryProfile.getDayHour(mTempSmartOffpeakEnd);
            int endMin = BatteryProfile.getHourMins(mTempSmartOffpeakEnd);

            endtimeDlg.updateTime(endHour, endMin);
            break;
        case OFFPEAK_TIMEOUT_DLG_ID:
        // Intentional fall-through
        case PEAK_TIMEOUT_DLG_ID:
            int listPosition;
            if(Id == PEAK_TIMEOUT_DLG_ID) {
                listPosition = getListPosition(mTempSmartBattTimeout);
            }else {
                listPosition = getListPosition(mTempSmartBattOffpeakTimeout);
            }
            AlertDialog alertDlg = (AlertDialog)dlg;
            ListView lv = alertDlg.getListView();
            Utils.Log.v(LOG_TAG, "list selection is: " + lv.getSelectedItemPosition() + 
                    listPosition);
            if(lv.getCheckedItemPosition() == ListView.INVALID_POSITION) {
                //lv.setItemChecked(listPosition, true);
                Utils.Log.v(LOG_TAG, "No list item checked");
            }
            break;
        }
    } 
    
    /**
     * 
     */
    public void commitAndnotifyService(boolean isPreset) {
        mPreferenceEditor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_START,
                mTempSmartOffpeakStart);
        mPreferenceEditor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_END,
                mTempSmartOffpeakEnd);
        mPreferenceEditor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_DATATIMEOUT,
                mTempSmartBattOffpeakTimeout);
        mPreferenceEditor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_PEAK_DATATIMEOUT,
                mTempSmartBattTimeout);
        mPreferenceEditor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_DISPLAY_BRIGHTNESS,
                mTempDisplayBrightness);

        mPreferenceEditor.commit();

        // Now check if we need to reflect the above in the actual store
        if(commitToActualStore()) {
            notifyDisplay(true);
            notifyService();
        }else {
            notifyDisplay(false);
        }
    }

    private boolean commitToActualStore() {
        SharedPreferences svcPref = getSharedPreferences(BatteryProfile.OPTIONS_STORE,
                MODE_PRIVATE);
        boolean shouldCommit = !svcPref.getBoolean(BatteryProfile.KEY_OPTION_IS_PRESET,
                BatteryProfile.DEFAULT_OPTION_SELECT);
        if(shouldCommit) {
            // We are in custom mode, move to actual store
            SharedPreferences.Editor svcEditor = svcPref.edit();
          
            svcEditor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_START,
                    mTempSmartOffpeakStart);
            svcEditor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_END,
                    mTempSmartOffpeakEnd);
            svcEditor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_OFFPEAK_DATATIMEOUT,
                    mTempSmartBattOffpeakTimeout);
            svcEditor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_PEAK_DATATIMEOUT,
                    mTempSmartBattTimeout);
            svcEditor.putInt(BatteryProfile.KEY_OPTION_CUSTOM_DISPLAY_BRIGHTNESS,
                    mTempDisplayBrightness);
            svcEditor.commit();
        }
        return shouldCommit;
    }

    private void notifyDisplay(boolean isCustom) {
		Utils.Log.v(LOG_TAG, "txvd74 notifyDisplay isCustom="+ isCustom + " mTempDisplayBrightness = " + mTempDisplayBrightness + " mTempBackupBrightness =" + mTempBackupBrightness);
        if(isCustom) {
            DisplayControl.noteBrightnessChange(mTempDisplayBrightness,
                    mTempBackupBrightness, 
                    getApplicationContext());
        }else {
            if(!mAutoBrightnessOn) {
                DisplayControl.setBrightness(mTempBackupBrightness,
                        getApplicationContext());
            }
        }
    }

    // 2011.12.13 jrw647 added to fix cr 5143
    public boolean isScreanOn() {
        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        return pm.isScreenOn();
    }
    // 2011.12.13 jrw647 added end
    /**
     * 
     */
    private void notifyService() {
        Intent svcIntent = new Intent(PowerProfileSvc.SVC_START_ACTION)
                                .setClass(this, PowerProfileSvc.class);
        startService(svcIntent);
    }
    
    private String getTimeString(int hour, int min) {
        Date dummyDate = new Date(2009, 1, 1, hour, min, 0);
        return DateFormat.getTimeFormat(this).format(dummyDate);
    }
}

