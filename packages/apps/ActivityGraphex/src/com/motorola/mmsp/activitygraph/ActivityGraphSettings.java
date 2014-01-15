package com.motorola.mmsp.activitygraph;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.content.Intent;
import android.util.Log;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class ActivityGraphSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    private CheckBoxPreference mAutoModePref;

    private Preference mHideApplicationsPref;
    private Preference mManageShortcutsPref;
    private Preference mSetSkinPref;
    private Preference mResetPref;
    private static final String TAG = "ActivityGraphSettings";
 // Added by amt_chenjing for SWITCHUI-2058 20120712 begin
    private int mMode;
 // Added by amt_chenjing for SWITCHUI-2058 20120712 begin

    boolean hasPaused = false;

    // private ActivityGraphApplication mApp = null;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.setupmode);

        if ((ActivityGraphModel.getMaxCard() != 7)
                && (ActivityGraphModel.getMaxCard() != 9)) {
            Log.d(TAG, "first set card num");
            Intent intent = getIntent();
            int cardNum = intent.getIntExtra("screen", 9);
            ActivityGraphModel.setMaxCard(cardNum);
        }

        mAutoModePref = (CheckBoxPreference) findPreference("auto setup mode");
        mHideApplicationsPref = findPreference("hide applications");
        mManageShortcutsPref = findPreference("manage shortcuts");
        mSetSkinPref = findPreference("set skin");
        mResetPref = findPreference("reset");
        Log.d(TAG, "onCreate");
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mAutoModePref) {
            boolean autoEnabled = ((CheckBoxPreference) mAutoModePref)
                    .isChecked();
            mHideApplicationsPref.setEnabled(autoEnabled);
            mManageShortcutsPref.setEnabled(!autoEnabled);
            int mode = autoEnabled ? ActivityGraphModel.AUTO_MODE
                    : ActivityGraphModel.MANUAL_MODE;
         // Added by amt_chenjing for SWITCHUI-2058 20120712 begin
            Log.d(TAG, "onClick()----hasPaused = " + hasPaused);
            if (hasPaused) {
                ActivityGraphModel.setGraphyMode(mode);
            } else {
                Log.d(TAG, "onResume()");
                ActivityGraphModel.setGraphyModeOnly(mode);
            }
         // Added by amt_chenjing for SWITCHUI-2058 20120712 end
        }

        if (preference == mResetPref) {
            showDialog(0);
        }
        return false;
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        hasPaused = false;
        
        boolean autoEnabled = (ActivityGraphModel.getGraphyMode(this) == ActivityGraphModel.AUTO_MODE) ? true
                : false;
        mAutoModePref.setChecked(autoEnabled);
        mHideApplicationsPref.setEnabled(autoEnabled);
        mManageShortcutsPref.setEnabled(!autoEnabled);
     // Added by amt_chenjing for SWITCHUI-2058 20120712 begin
        mMode = ActivityGraphModel.getGraphyMode(ActivityGraphSettings.this);
        Log.d(TAG, "onResume----------mode = " + mMode);
     // Added by amt_chenjing for SWITCHUI-2058 20120712 end

    }

    @Override
    protected void onPause() {
        super.onPause();
        
        hasPaused = true;
     // Added by amt_chenjing for SWITCHUI-2076 20120712 begin
        int mode = ActivityGraphModel.getGraphyMode(ActivityGraphSettings.this);
        Log.d(TAG, "onPause---------mode = " + mMode);
        if(mode != mMode) {
            ActivityGraphModel.sendModeChange(mode);
        }
     // Added by amt_chenjing for SWITCHUI-2076 20120712 end
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mAutoModePref) {
            boolean autoEnabled = ((CheckBoxPreference) mAutoModePref)
                    .isChecked();
            mHideApplicationsPref.setEnabled(autoEnabled);
            mManageShortcutsPref.setEnabled(!autoEnabled);
        }
        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        return new AlertDialog.Builder(this)
                .setTitle(R.string.resetConfirmation_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.resetConfirmationMessage)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.resetbutton,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                mHandler.sendEmptyMessage(0);
                                // ActivityGraphModel.resetWidget();
                                // mAutoModePref.setChecked(true);
                                // mHideApplicationsPref.setEnabled(true);
                                // mManageShortcutsPref.setEnabled(false);
                            }
                        }).setCancelable(true).create();
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);

            if (mAutoModePref != null) {
                mAutoModePref.setChecked(true);
            }
            if (mHideApplicationsPref != null) {
                mHideApplicationsPref.setEnabled(true);
            }
            if (mManageShortcutsPref != null) {
                mManageShortcutsPref.setEnabled(false);
            }
            ActivityGraphModel.resetWidget();
            mMode = 0;
        }

    };
}
