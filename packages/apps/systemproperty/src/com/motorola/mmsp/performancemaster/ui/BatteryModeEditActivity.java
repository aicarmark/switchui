/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *                             Modification     Tracking
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * bntw34                      02/05/2012                   Initial release
 */

package com.motorola.mmsp.performancemaster.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.motorola.mmsp.performancemaster.engine.BatteryModeData;
import com.motorola.mmsp.performancemaster.engine.BatteryModeMgr;
import com.motorola.mmsp.performancemaster.engine.Log;
import com.motorola.mmsp.performancemaster.ui.BatteryWidgetService.LocalBinder;
import com.motorola.mmsp.performancemaster.R;

import java.util.ArrayList;
import java.util.List;

public class BatteryModeEditActivity extends Activity implements View.OnClickListener, DialogInterface.OnClickListener {
    private static final String LOG_TAG = "BatteryEdit: ";
    public static final String ACTION_EDIT_MODE = "battery_edit_mode";
    public static final String ACTION_NEW_MODE = "battery_new_mode";
    public static final String EXTRA_ID = "extra_id";
    public static final String EXTRA_BATT_PERCENT = "batt_percent";
    private static final int PRESET_MODE_NUM = 3;
    private static final int BATTERY_RED_PERCENT = 10;
    private static final int BATTERY_ORANGE_PERCENT = 30;
    private static final int DIALOG_ID_REMOVE_BATTERY_MODE = 1;
    
    private static final int PENDING_EXIT_REASON_DELETE_TO_GENERAL = 0;
    private static final int PENDING_EXIT_REASON_SAVE_CHANGE_APPLY = 1;
    private int mPendingExitCode;

    private BatteryModeMgr mBatteryModeMgr;
    private String mAction;
    private long mEditModeId;
    private BatteryModeData mEditModeData;
    private BatteryModeEditAdapter mEditListAdapter;

    private GridView mEditGridView;    
    private Button mCancelButton;
    private Button mDeleteButton;
    private Button mSaveButton;
    private EditText mNameEdit;
    private int mBatteryPercent;
    private boolean mCharging;
    private ImageView mBattPercentView;
    private ImageView mBattCharingView;
    private int mBatteryViewHeight;

    private BattRemainingUpdate mBatteryRemainingUpdate = null;
    private BatteryReceiver mBatteryReceiver;
    private BatteryWidgetService mService;
    private boolean mBound;
    
    private ProgressDialog mProgressDialog; // progress dialog for mode switching
    private boolean mModeSwitching;         // indicate whether mode switching is ongoing 
    private boolean mUserDone;              // indicate whether user click done

    private AlertDialog mDeleteDlg = null;
    private AlertDialog mAlertDlg = null;
    
    private ChangeCompleteReceiver mCompleteReceiver;
    private class ChangeCompleteReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mModeSwitching = false;
            mProgressDialog.dismiss();
            
            if (mUserDone) {
                Log.e(LOG_TAG, "ChangeCompleted, to finish EDIT****");
                Log.e(LOG_TAG, "ChangeCompleted pendingExitCode=" + mPendingExitCode);
                
                if (mPendingExitCode == PENDING_EXIT_REASON_DELETE_TO_GENERAL) {
                    // [EXIT: delete current mode, back to general mode]
                    ackCaller(RESULT_OK, mBatteryModeMgr.getCurrMode().getId());
                } else if (mPendingExitCode == PENDING_EXIT_REASON_SAVE_CHANGE_APPLY) {
                    // [EXIT: save current mode, change applied]
                    ackCaller(RESULT_OK, mEditModeData.getId());
                }
                
                finish();
            } else {
                Log.e(LOG_TAG, "ChangeCompleted, NOT BY THIS SESSION****");
            }
            
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            if (mService != null && mEditModeData != null) {
                Log.i(LOG_TAG, "ServiceConnected-->calcLeftTime");
                mService.calcLeftTime(mEditModeData);
                mBatteryPercent = mService.getBatteryPercent();
                mCharging = mService.getBatteryCharing();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    ArrayList<BatteryModeEditItem> getEditList() {
        ArrayList<BatteryModeEditItem> list = new ArrayList<BatteryModeEditItem>();

        if (mEditModeData != null) {
            BatteryModeEditItem bright = new BatteryModeEditItem();
            bright.setTag(BatteryModeEditItem.ITEM_TAG_BRIGHTNESS);
            bright.setInt(mEditModeData.getBrightness());

            BatteryModeEditItem timeout = new BatteryModeEditItem();
            timeout.setTag(BatteryModeEditItem.ITEM_TAG_TIMEOUT);
            timeout.setInt(mEditModeData.getTimeout());

            BatteryModeEditItem bt = new BatteryModeEditItem();
            bt.setTag(BatteryModeEditItem.ITEM_TAG_BLUETOOTH);
            bt.setBoolean(mEditModeData.getBluetoothOn());

            BatteryModeEditItem md = new BatteryModeEditItem();
            md.setTag(BatteryModeEditItem.ITEM_TAG_MOBILEDATA);
            md.setBoolean(mEditModeData.getMobileDataOn());

            BatteryModeEditItem sync = new BatteryModeEditItem();
            sync.setTag(BatteryModeEditItem.ITEM_TAG_SYNC);
            sync.setBoolean(mEditModeData.getSyncOn());

            BatteryModeEditItem haptic = new BatteryModeEditItem();
            haptic.setTag(BatteryModeEditItem.ITEM_TAG_HAPTIC);
            haptic.setBoolean(mEditModeData.getHapticOn());

            BatteryModeEditItem vibrate = new BatteryModeEditItem();
            vibrate.setTag(BatteryModeEditItem.ITEM_TAG_VIBRATION);
            vibrate.setBoolean(mEditModeData.getVibrationOn());

            BatteryModeEditItem rotate = new BatteryModeEditItem();
            rotate.setTag(BatteryModeEditItem.ITEM_TAG_ROTATION);
            rotate.setBoolean(mEditModeData.getRotationOn());

            BatteryModeEditItem wifi = new BatteryModeEditItem();
            wifi.setTag(BatteryModeEditItem.ITEM_TAG_WIFI);
            wifi.setBoolean(mEditModeData.getWiFiOn());

            list.add(bright);
            list.add(timeout);
            list.add(bt);
            list.add(wifi);          
            list.add(md);
            list.add(sync);
            list.add(rotate);
            list.add(vibrate);
            list.add(haptic);
        }

        return list;
    }

    private class BatteryReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                mBatteryPercent = (int) (((float) level / (float) scale) * 100);
                mCharging = (plugged != 0 ? true : false);
                Log.e(LOG_TAG, "onReceive batt percent=" + mBatteryPercent);
                updateBatteryDisplay();
            }
        }
    }

    private String getNewModeName() {
        String str = this.getString(R.string.bm_mode_prefix);
        if (mBatteryModeMgr != null) {
            List<BatteryModeData> list = mBatteryModeMgr.getAllMode();
            // custom mode name post fix with "-1" "-2" ...
            int numCustom = list.size() - PRESET_MODE_NUM + 1; 
            str += String.valueOf(numCustom);
        }
        return str;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.i(LOG_TAG, "onCreate");
        
        setContentView(R.layout.bm_mode_edit);
        
        // initialize action bar
        initActionBar();

        BatteryModeMgr.setContext(this.getApplicationContext());
        mBatteryModeMgr = BatteryModeMgr.getInstance();
        if (mBatteryModeMgr != null) {
            mBatteryModeMgr.init();
        }
        
        mCancelButton = (Button) findViewById(R.id.btn_edit_cancel);
        mDeleteButton = (Button) findViewById(R.id.btn_edit_delete);
        mSaveButton = (Button) findViewById(R.id.btn_edit_save);
        mCancelButton.setOnClickListener(this);
        mDeleteButton.setOnClickListener(this);
        mSaveButton.setOnClickListener(this);

        mBattPercentView = (ImageView) findViewById(R.id.bm_batt_percent);
        if (mBattPercentView != null) {
            mBatteryViewHeight = mBattPercentView.getLayoutParams().height;
        }
        
        // TODO: check with: original UI handling
        Intent intent = getIntent();
        if (!setMode(intent)) {
            finish();
        }
        
        // create progress dialog
        mModeSwitching = false;
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(this.getString(R.string.bm_switching));
        
        // register battery receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mBatteryReceiver = new BatteryReceiver();
        registerReceiver(mBatteryReceiver, filter);
        
        // register mode change complete receiver
        mCompleteReceiver = new ChangeCompleteReceiver();
        IntentFilter filterComplete = new IntentFilter();
        filterComplete.addAction(BatteryModeMgr.ACTION_BATTERY_MODE_CHANGE_COMPLETED);
        this.registerReceiver(mCompleteReceiver, filterComplete);
    }
    
    public void changeBrightness(int val) {
        Window w = this.getWindow();
        // preview brightness changes at this window
        // get the current window attributes
        WindowManager.LayoutParams layoutpars = w.getAttributes();
        
        if (val > 0) {
            if (val < BatteryModeData.BRIGHTNESS_MIN_VALUE) {
                val = BatteryModeData.BRIGHTNESS_MIN_VALUE;
            }

            // set the brightness of this window
            // 0.0 - 1.0
            layoutpars.screenBrightness = val / (float) 100;
            // apply attribute changes to this window
            w.setAttributes(layoutpars);
        } else {
            // set auto mode
            this.onSetAutoBrightness(true);
            
            layoutpars.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            w.setAttributes(layoutpars);
        }
    }
    
    /**
     * @param intent
     * @return false for failed, success for success
     */
    private boolean setMode(Intent intent) {
        Log.e(LOG_TAG, "setMode");
        if (intent != null && (mAction = intent.getAction()) != null) {
            if (mAction.equals(ACTION_EDIT_MODE)) {
                // edit mode
                mEditModeId = intent.getExtras().getLong(EXTRA_ID);

                Log.e(LOG_TAG, "edit id=" + mEditModeId +
                            ", battery percent=" + mBatteryPercent);

                mEditModeData = mBatteryModeMgr.getModeFromId(mEditModeId);
                if (mEditModeData != null) {
                    Log.e(LOG_TAG, "edit mode=" + mEditModeData.getModeName());
                } else {
                    Log.e(LOG_TAG, "no such mode in database");
                    return false;
                }
                
                // delete button for edit mode
                mDeleteButton.setVisibility(View.VISIBLE);
            } else {               
                // new mode
                mEditModeData = mBatteryModeMgr.getCurrModeFromPrefs();
                mEditModeData.setModeName(getNewModeName());
                
                // no delete button for new mode
                mDeleteButton.setVisibility(View.GONE);
            }
            
            mBatteryPercent = intent.getExtras().getInt(EXTRA_BATT_PERCENT, 60);
            if (mService != null) {
                mBatteryPercent = mService.getBatteryPercent();
            }

            ArrayList<BatteryModeEditItem> list = getEditList();
            mEditListAdapter = new BatteryModeEditAdapter(this, list);
            mEditGridView = (GridView) findViewById(R.id.bm_edit_list);
            if (mEditGridView != null){
                mEditGridView.setAdapter(mEditListAdapter);
                //mEditGridView.setCacheColorHint(0);
            }
            
            // when entry, change brightness
            changeBrightness(mEditModeData.getBrightness());

            mNameEdit = (EditText) findViewById(R.id.bm_edit_mode_name);
            mNameEdit.setText(mEditModeData.getModeName());

            mBattCharingView = (ImageView) findViewById(R.id.bm_batt_charing);

            // update battery info, we only update once when create this
            // activity
            // we will record mBatteryViewHeight, it is the original height
            // of the battery percent ImageView's height
            updateBatteryDisplay();

            // battery remaining time update
            mBatteryRemainingUpdate = new BattRemainingUpdate(this,
                        (TextView) findViewById(R.id.bm_remaining_time));
            
            return true;
        }
        
        return false;
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.e(LOG_TAG, "onNewIntent");

        if (null != mDeleteDlg) {
            mDeleteDlg.hide();
        }

        if (null != mAlertDlg) {
            mAlertDlg.hide();
        }

        if (!setMode(intent)) {
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        Log.i(LOG_TAG, "onStart");
        
        boolean ret = bindService(new Intent(this, BatteryWidgetService.class), mConnection,
                Context.BIND_AUTO_CREATE);
        
        Log.i(LOG_TAG, "onStart bindservice ret=" + ret);
        
        mUserDone = false;
        mModeSwitching = mBatteryModeMgr.isModeSwiching();
        if (mModeSwitching && mProgressDialog != null) {
            mProgressDialog.show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        
        Log.i(LOG_TAG, "onStop");
        
        // unbind BatteryWidgetService
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
            Log.i(LOG_TAG, "onStop unbindservice");
        }
        
        /*
        int brightness = mBatteryModeMgr.recoverBrightnessMode();
        if (brightness > 0) {
            Log.i(LOG_TAG, "onStop recover brightness=" + brightness);
            changeBrightness(brightness);
        } else {
            Log.i(LOG_TAG, "onStop recover brightness=auto");
        }
        */
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        Log.i(LOG_TAG, "onPause");
        
        int brightness = mBatteryModeMgr.recoverBrightnessMode();
        if (brightness > 0) {
            Log.i(LOG_TAG, "onPause recover brightness=" + brightness);
            changeBrightness(brightness);
        } else {
            Log.i(LOG_TAG, "onPause recover brightness=auto");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        Log.i(LOG_TAG, "onResume");
        
        updateBatteryDisplay();
        
        // when entry, change brightness
        changeBrightness(mEditModeData.getBrightness());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        Log.i(LOG_TAG, "onDestroy");
        
        if (null != mBatteryRemainingUpdate) {
            mBatteryRemainingUpdate.stop();
        }

        if (mBatteryReceiver != null) {
            unregisterReceiver(mBatteryReceiver);
        }

        if (mBatteryModeMgr != null) {
            mBatteryModeMgr.deinit();
        }
        
        this.unregisterReceiver(mCompleteReceiver);
    }
    
    private void ackCaller(int resultCode, long id) {
        Intent intent = new Intent();
        if (mAction != null) {
            if (mAction.equals(ACTION_EDIT_MODE)) {
                intent.putExtra(BatteryModeSelectActivity.EDIT_MODE_ID, id);
            } else if (mAction.equals(ACTION_NEW_MODE)) {
                intent.putExtra(BatteryModeSelectActivity.ADD_MODE_ID, id);
            }
            
            setResult(resultCode, intent);
        }
    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.btn_edit_cancel:
                // recover remaining time
                if (mService != null && mBound) {
                    mService.recoverLeftTime();
                }
                // add new: not add
                // edit exist: not edit
                ackCaller(RESULT_CANCELED, -1);
                // finish the activity
                finish();
                break;
            case R.id.btn_edit_save:   
                if (mAction != null) {
                    mUserDone = true;
                    if (mAction.equals(ACTION_EDIT_MODE)) {
                        saveBatteryMode(mEditListAdapter.getValues(), false);
                    } else if (mAction.equals(ACTION_NEW_MODE)) {
                        saveBatteryMode(mEditListAdapter.getValues(), true);
                    }
                }
                break;
            case R.id.btn_edit_delete:
                if (mAction != null && mAction.equals(ACTION_EDIT_MODE)) {
                    String modeName = mBatteryModeMgr.getModeUIName(mEditModeData);
                    String text = String.format(this.getString(R.string.bm_warning_hint),
                            "\""+ modeName + "\"");
                    showMyAlert(DIALOG_ID_REMOVE_BATTERY_MODE, text);
                }
                break;
            default:
                break;
        }
    }
    
    /**
     * show alert dialog: battery mode remove
     * @param id: dialog id
     * @param args: dialog text
     */
    public void showMyAlert(int id, String args) {
        AlertDialog.Builder myDlgBuilder = new AlertDialog.Builder(this);

        if (id == DIALOG_ID_REMOVE_BATTERY_MODE) {
            myDlgBuilder.setMessage(args);
            myDlgBuilder.setPositiveButton(R.string.bm_delete, this);
            myDlgBuilder.setNegativeButton(R.string.bm_cancel, this);
            myDlgBuilder.setTitle(R.string.bm_warning);
        }

        mDeleteDlg = myDlgBuilder.create();
        mDeleteDlg.show();
    }

    /**
     *  alert dialog button onClick
     */
    @Override
    public void onClick(DialogInterface arg0, int arg1) {
        if (arg1 == DialogInterface.BUTTON_POSITIVE) {
            // remove battery mode
            
            // remove on database
            if (mBatteryModeMgr != null) {
                mBatteryModeMgr.removeMode(mEditModeData.getId());    
            }
            
            BatteryModeData currMode = mBatteryModeMgr.getCurrMode();           
            
            if (mEditModeData.getId() == currMode.getId()) {
                // current mode is removed
                // force switching to the first mode in the list
                
                // show progress dialog
                mProgressDialog.setMessage(this.getString(R.string.bm_switching));
                mProgressDialog.show();
                mUserDone = true;
                
                mModeSwitching = true;
                
                BatteryModeData generalMode = mBatteryModeMgr.getAllMode().get(0);
                mBatteryModeMgr.switchMode(generalMode);
                
                // [pending EXIT: delete: current]
                mPendingExitCode = PENDING_EXIT_REASON_DELETE_TO_GENERAL;
            } else {
                Log.e(LOG_TAG, "delete: not current");
                // [EXIT: delete: not current]
                ackCaller(RESULT_OK, currMode.getId());
                
                finish();
            }
        } else if (arg1 == DialogInterface.BUTTON_NEGATIVE) {
            // do nothing
        }
    }

    public void onCalcLeftTime() {
        BatteryModeData data = getEditModeData(mEditListAdapter.getValues());

        if (mBound && mService != null) {
            mService.calcLeftTime(data);
            mBatteryPercent = mService.getBatteryPercent();
        }
    }
    
    public void onSetAutoBrightness(boolean bAuto) {
        Log.e(LOG_TAG, "onSetAutoBrightness bAuto=" + bAuto);
        mBatteryModeMgr.setAutoBrightness(bAuto);
    }

    public BatteryModeEditAdapter getBatteryModeEditAdapter() {
        return mEditListAdapter;
    }
    
    /**
     * Action Bar is only for display icon & text, no action!!!
     */
    private void initActionBar() {
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Inflate a custom action bar 
            LayoutInflater inflater = (LayoutInflater) getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            View customActionBarView = inflater.inflate(R.layout.bm_actionbar, null);
            
            // Show the custom action bar but hide the home icon and title
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME |
                    ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setCustomView(customActionBarView);
        }
    }

    private BatteryModeData getEditModeData(ArrayList<BatteryModeEditItem> list) {
        BatteryModeData mode = mEditModeData;

        for (int i = 0; i < list.size(); i++) {
            switch (list.get(i).getTag()) {
                case BatteryModeEditItem.ITEM_TAG_BRIGHTNESS:
                    mode.setBrightness(list.get(i).getInt());
                    break;
                case BatteryModeEditItem.ITEM_TAG_TIMEOUT:
                    mode.setTimeout(list.get(i).getInt());
                    break;
                case BatteryModeEditItem.ITEM_TAG_BLUETOOTH:
                    mode.setBluetoothOn(list.get(i).getBoolean());
                    break;
                case BatteryModeEditItem.ITEM_TAG_WIFI:
                    mode.setWiFiOn(list.get(i).getBoolean());
                    break;
                case BatteryModeEditItem.ITEM_TAG_MOBILEDATA:
                    mode.setMobileDataOn(list.get(i).getBoolean());
                    break;
                case BatteryModeEditItem.ITEM_TAG_SYNC:
                    mode.setSyncOn(list.get(i).getBoolean());
                    break;
                case BatteryModeEditItem.ITEM_TAG_HAPTIC:
                    mode.setHapticOn(list.get(i).getBoolean());
                    break;
                case BatteryModeEditItem.ITEM_TAG_VIBRATION:
                    mode.setVibrationOn(list.get(i).getBoolean());
                    break;
                case BatteryModeEditItem.ITEM_TAG_ROTATION:
                    mode.setRotationOn(list.get(i).getBoolean());
                    break;
                default:
                    break;
            }
        }

        return mode;
    }
    
    private void showAlertDialog(String str) {
        AlertDialog.Builder myDlgBuilder = new AlertDialog.Builder(this);

            myDlgBuilder.setMessage(str);
            myDlgBuilder.setNeutralButton(R.string.but_ok_text, new DialogInterface.OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // alert dialog OK: name exists, length == 0, etc.
                    
                    // alert dialog OK: exceed maximum battery mode number
                    if (mBatteryModeMgr.getModeNums() >= BatteryModeMgr.MAX_BATTERY_MODE_NUM) {
                        finish();
                    }
                }
            });
            myDlgBuilder.setTitle(R.string.bm_warning);

        mAlertDlg = myDlgBuilder.create();
        mAlertDlg.show();
    }

    /**
     * save battery mode
     * return -1: failure
     *        0: success
     *        1: success pending
     */
    private int saveBatteryMode(ArrayList<BatteryModeEditItem> list, boolean createNew) {
        BatteryModeData mode = getEditModeData(list);
        String strModeName = mNameEdit.getText().toString();
        int ret = 0;
        
        // exceed maximum battery mode number
        if (createNew 
                && mBatteryModeMgr.getModeNums() >= BatteryModeMgr.MAX_BATTERY_MODE_NUM) {
            showAlertDialog(this.getString(R.string.bm_mode_max));
            return -1;
        }
        
        // "" mode name or null mode name
        if (strModeName == null
                || strModeName.length() == 0 ) {
            showAlertDialog(this.getString(R.string.bm_name_length_zero));
            return -1;
        }
        
        boolean bNameUsed = mBatteryModeMgr.isModeNameUsed(strModeName);
        
        // new mode with the name used
        if (bNameUsed && createNew) {
            showAlertDialog(this.getString(R.string.bm_name_same));
            return -1;
        }
        
        // edit mode with the name used
        if (bNameUsed && !createNew && !strModeName.equals(mEditModeData.getModeName())) {
            showAlertDialog(this.getString(R.string.bm_name_same));
            return -1;
        }

        if (mode != null) {
            mode.setModeName(strModeName);
        }
        
        if (createNew) {
            mode = mBatteryModeMgr.createCustomizeMode(mode);
            
            // [EXIT: add new, add success]
            ackCaller(RESULT_OK, mode.getId());
            // save new battery mode, no pending
            ret = 0;
        } else {
            Log.i(LOG_TAG, "saveBatteryMode, saving the exist mode");
            
            // saving battery mode, apply change if any
            int saveRet = mBatteryModeMgr.saveCustomizeMode(mode);
            if (saveRet == BatteryModeMgr.APPLY_CHANGE_RET_SUCCESS_PENDING) {
                // show progress dialog
                mProgressDialog.setMessage(this.getString(R.string.bm_saving));
                mProgressDialog.show();
                
                mModeSwitching = true;
                
                // save existed battery mode, apply changes, pending
                ret = 1;
                
                // [pending EXIT: save current mode apply changes]
                mPendingExitCode = PENDING_EXIT_REASON_SAVE_CHANGE_APPLY;
            } else if (saveRet == BatteryModeMgr.APPLY_CHANGE_RET_SUCCESS) {
                // [EXIT: save exit, no pending]
                ackCaller(RESULT_OK, mEditModeData.getId());
                // save existed battery mode, no change apply, no pending
                ret = 0;
            }  else if (saveRet == BatteryModeMgr.APPLY_CHANGE_RET_FAIL) {
                ret = -1;
            }
        }
        
        // only ret == 0 result in finish()
        if (ret == 0) {
            finish();
        } else if (ret == -1) {
            Log.e(LOG_TAG, "edit error, need operate again");
        } else if (ret == 1) {
            Log.i(LOG_TAG, "edit apply changes process...");
        }
        
        return ret;
    }

    private void updateBatteryDisplay() {
        // battery percent image
        if (mBattPercentView == null) {
            mBattPercentView = (ImageView) findViewById(R.id.bm_batt_percent);
        }

        if (mBattPercentView != null) {
            if (mBatteryPercent < BATTERY_RED_PERCENT) {
                mBattPercentView.setImageResource(R.drawable.bm_power_red);
            } else if (mBatteryPercent < BATTERY_ORANGE_PERCENT) {
                mBattPercentView.setImageResource(R.drawable.bm_power_orange);
            } else {
                mBattPercentView.setImageResource(R.drawable.bm_power_green);
            }

            RelativeLayout.LayoutParams flp = (RelativeLayout.LayoutParams)
                    mBattPercentView.getLayoutParams();

            flp.height = mBatteryViewHeight * mBatteryPercent / 100;

            Log.i(LOG_TAG, "updateBatteryDisplay percent=" + mBatteryPercent + " totalheight="
                    + mBatteryViewHeight);

            mBattPercentView.setLayoutParams(flp);
        }
        
        if (mBattCharingView != null) {
            mBattCharingView.setVisibility(mCharging ? View.VISIBLE : View.INVISIBLE);
        }

        // battery percent text
        TextView tvBattPercent = (TextView) findViewById(R.id.bm_usage_percent);
        tvBattPercent.setText(mBatteryPercent + "%");
    }
}
