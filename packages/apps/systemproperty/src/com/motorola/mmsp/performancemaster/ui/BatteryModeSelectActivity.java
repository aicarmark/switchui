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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.motorola.mmsp.performancemaster.engine.BatteryModeData;
import com.motorola.mmsp.performancemaster.engine.BatteryModeMgr;
import com.motorola.mmsp.performancemaster.engine.Log;
import com.motorola.mmsp.performancemaster.R;

import java.util.ArrayList;
import java.util.List;

public class BatteryModeSelectActivity extends Activity implements DialogInterface.OnClickListener, View.OnClickListener, OnItemClickListener{
    private static final String LOG_TAG = "BatterySelect: ";
    
    public static final String EXTRA_BATT_PERCENT = "batt_percent";
    public static final int DIALOG_ID_REMOVE_BATTERY_MODE = 1;
    
    private static final int ADD_MODE_REQUEST = 1;
    private static final int EDIT_MODE_REQUEST = 2;
    public static final String ADD_MODE_ID = "add_mode_id";
    public static final String EDIT_MODE_ID = "edit_mode_id";

    private BatteryModeData mCurrMode;
    private BatteryModeData mSelectedMode;
    private List<BatteryModeData> mModeList;
    private BatteryModeMgr mBatteryModeMgr;
    private BatteryModeListAdapter mListAdapter;
    private ListView mModeListView;
    private long mRemoveModeId;
    
    private ImageView mBarWifi;
    private ImageView mBarMobileData;
    private ImageView mBarBrightness;
    private ImageView mBarBluetooth;
    private LinearLayout mBarAddMode;
    private Button mBtnSet;
    private Button mBtnCancel;
    
    private ProgressDialog mProgressDialog; // progress dialog for mode switching
    private boolean mModeSwitching;         // indicate whether mode switching is ongoing
    private boolean mUserSwitch;            // indicate whether user consent mode switching and exit
    
    // legacy code: battery percent
    private int mBattPercent;
    
    private ChangeCompleteReceiver mCompleteReceiver;
    private class ChangeCompleteReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            
            if (action.equals(BatteryModeMgr.ACTION_BATTERY_MODE_CHANGED)) {
                Log.i(LOG_TAG, "[SEL]: ACTION_BATTERY_MODE_CHANGED");
                mCurrMode = mBatteryModeMgr.getCurrMode();
                mModeList = mBatteryModeMgr.getAllMode();
                mSelectedMode = mCurrMode; // change list focus
                updateUI();
            } else if (action.equals(BatteryModeMgr.ACTION_BATTERY_MODE_CHANGE_COMPLETED)) {
                mModeSwitching = false;
                mProgressDialog.dismiss();
                
                mCurrMode = mBatteryModeMgr.getCurrMode();
                mModeList = mBatteryModeMgr.getAllMode();
                mSelectedMode = mCurrMode; // change list focus
                
                if (mUserSwitch) {
                    Log.e(LOG_TAG, "ChangeCompleted, BY_MYSELF_SELECT====");
                    finish();
                } else {
                    Log.e(LOG_TAG, "ChangeCompleted, BY_ANOTHER_SESSION====");
                    updateUI();
                }
            } 
        }
    }

    public void onEditMode(long id) {
        Log.i(LOG_TAG, "onEditMode id=" + id +
                ", mBattPercent=" + mBattPercent);

        Intent intent = new Intent(this, BatteryModeEditActivity.class);
        intent.setAction(BatteryModeEditActivity.ACTION_EDIT_MODE);
        intent.putExtra(BatteryModeEditActivity.EXTRA_ID, id);
        intent.putExtra(BatteryModeEditActivity.EXTRA_BATT_PERCENT, mBattPercent);
        startActivityForResult(intent, EDIT_MODE_REQUEST);
    }

    public void onAddMode() {
        Log.i(LOG_TAG, "onAddMode");

        Intent intent = new Intent(this, BatteryModeEditActivity.class);
        intent.setAction(BatteryModeEditActivity.ACTION_NEW_MODE);
        intent.putExtra(BatteryModeEditActivity.EXTRA_BATT_PERCENT, mBattPercent);
        startActivityForResult(intent, ADD_MODE_REQUEST);
    }
    
    /**
     * for single instance activity likes BatteryModeSelectActivity
     * onActivityResult can not work as normal.
     * since singleInstance activity is the only activity on its own stack
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(LOG_TAG, "onActivityResult");
        long selectId = -1;
        if (requestCode == ADD_MODE_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                selectId = data.getLongExtra(ADD_MODE_ID, -1);
            }
        } else if (requestCode == EDIT_MODE_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                selectId = data.getLongExtra(EDIT_MODE_ID, -1);
            }
        }
        
        if (selectId != -1) {
            mSelectedMode = mBatteryModeMgr.getModeFromId(selectId);
            if (mSelectedMode != null) {
                Log.e(LOG_TAG, "onActivityResult selMode=" + mSelectedMode.toString());
            }
        }
    }

    public void onRemoveMode(long id) {
        Log.i(LOG_TAG, "onRemoveMode id=" + id);

        BatteryModeData removeModeData = mBatteryModeMgr.getModeFromId(id);
        mRemoveModeId = id;
        String text = String.format(this.getString(R.string.bm_warning_hint),
                "\""+ removeModeData.getModeName() + "\"");
        showMyAlert(DIALOG_ID_REMOVE_BATTERY_MODE, text);
    }
    
    private void changeBrightness(int val) {
        Window w = this.getWindow();

        if (val < BatteryModeData.BRIGHTNESS_MIN_VALUE) {
            val = BatteryModeData.BRIGHTNESS_MIN_VALUE;
        }

        // preview brightness changes at this window
        // get the current window attributes
        WindowManager.LayoutParams layoutpars = w.getAttributes();
        // set the brightness of this window
        // 0.0 - 1.0
        layoutpars.screenBrightness = val / (float) 100;
        // apply attribute changes to this window
        w.setAttributes(layoutpars);
    }

    public void onSwitchMode(long id) {
        Log.i(LOG_TAG, "onSwitchMode new Mode id=" + id + "  switching=" + mModeSwitching);
        
        if (mModeSwitching) {
            Log.e(LOG_TAG, "OnSwitchMode, Pending return!");
            return;
        }
               
        BatteryModeData newMode = mBatteryModeMgr.getModeFromId(id);
        
        // change brightness effect immediately
        if (mCurrMode.getBrightness() != newMode.getBrightness()) {
            changeBrightness(newMode.getBrightness());
        }
        
        // switching battery mode: intensive work load
        int saveRet = mBatteryModeMgr.switchMode(newMode);
        if (saveRet == BatteryModeMgr.APPLY_CHANGE_RET_SUCCESS_PENDING
                || saveRet == BatteryModeMgr.APPLY_CHANGE_RET_SUCCESS) {
            mUserSwitch = true;
            
            // show switching... dialog
            mModeSwitching = true;
            mProgressDialog.show();
            mCurrMode = newMode;
        } else if (saveRet == BatteryModeMgr.APPLY_CHANGE_RET_FAIL) {
            Log.e(LOG_TAG, "onSwitch failed");
            
            // switch mode failed, recover icon bar
            setBarIcons(mCurrMode);
        }        

        BatteryModeListItem item = convertToListItem(mCurrMode);
        mListAdapter.setCurrMode(item);
    }
    
    public void onSetBarIcons(long id) {
        if (mBatteryModeMgr == null) {
            return;
        }
        
        mSelectedMode = mBatteryModeMgr.getModeFromId(id);
        setBarIcons(mSelectedMode);
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mListAdapter != null) {
            mListAdapter.setItemClicked(position);
        }
    }

    private BatteryModeListItem convertToListItem(BatteryModeData mode) {
        BatteryModeListItem item = new BatteryModeListItem();
        if (mode == null) {
            Log.e(LOG_TAG, "convertToListItem mode==null return a dummy");
            return item;
        }

        item.setId(mode.getId());

        if (mode.getPreset()) {
            int type = mBatteryModeMgr.getPresetModeType(mode.getId());
            int itemType = 0;
            int strId = 0;

            switch (type) {
                case BatteryModeData.PRESET_MODE_GENERAL:
                    itemType = BatteryModeListItem.BATTERY_MODE_GENERAL;
                    strId = R.string.bm_mode_general;
                    break;
                case BatteryModeData.PRESET_MODE_NIGHT:
                    itemType = BatteryModeListItem.BATTERY_MODE_NIGHT;
                    strId = R.string.bm_mode_night;
                    break;
                case BatteryModeData.PRESET_MODE_SAVER:
                    itemType = BatteryModeListItem.BATTERY_MODE_SAVER;
                    strId = R.string.bm_mode_saver;
                    break;
                case BatteryModeData.PRESET_MODE_PERFORMANCE:
                    itemType = BatteryModeListItem.BATTERY_MODE_PERFORMANCE;
                    strId = R.string.bm_mode_performance;
                    break;
                default: 
                    Log.e(LOG_TAG, "getPresetModeType failed");
                    itemType = BatteryModeListItem.BATTERY_MODE_GENERAL;
                    strId = R.string.bm_mode_performance;
                    break;
            }
            item.setModeType(itemType);
            item.setText(this.getString(strId));
        } else {
            item.setModeType(BatteryModeListItem.BATTERY_MODE_CUSTOMIZE);
            item.setText(mode.getModeName());
        }

        return item;
    }

    private ArrayList<BatteryModeListItem> getModeList() {
        ArrayList<BatteryModeListItem> list = new ArrayList<BatteryModeListItem>();
        if (mModeList == null) {
            Log.e(LOG_TAG, "getModeList mModeList==null return dummy list");
            return list;
        }

        for (int i = 0; i < mModeList.size(); i++) {
            BatteryModeListItem item = convertToListItem(mModeList.get(i));
            list.add(item);
        }

        return list;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.i(LOG_TAG, "<<onCreate>>");
        
        setContentView(R.layout.bm_mode_select);

        Intent intent = getIntent();
        
        if (intent != null && intent.getExtras() != null) {
            mBattPercent = intent.getExtras().getInt(EXTRA_BATT_PERCENT);
            Log.i(LOG_TAG, "BatteryModeSelectActivity percent=" + mBattPercent);
        }

        BatteryModeMgr.setContext(this.getApplicationContext());
        mBatteryModeMgr = BatteryModeMgr.getInstance();
        if (mBatteryModeMgr != null) {
            mBatteryModeMgr.init();
            mCurrMode = mBatteryModeMgr.getCurrModeFromPrefs();
            // when created, selected mode is the current mode
            mSelectedMode = new BatteryModeData();
            mSelectedMode = mCurrMode;
        }
        
        // create progress dialog
        mModeSwitching = false;
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(this.getString(R.string.bm_switching));
        
        mCompleteReceiver = new ChangeCompleteReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BatteryModeMgr.ACTION_BATTERY_MODE_CHANGE_COMPLETED);
        filter.addAction(BatteryModeMgr.ACTION_BATTERY_MODE_CHANGED);
        this.registerReceiver(mCompleteReceiver, filter);
        
        // icons bar
        mBarWifi = (ImageView) findViewById(R.id.bar_wifi);
        mBarMobileData = (ImageView) findViewById(R.id.bar_mobiledata);
        mBarBrightness = (ImageView) findViewById(R.id.bar_brightness);
        mBarBluetooth = (ImageView) findViewById(R.id.bar_bluetooth);
        
        // list view
        mModeListView = (ListView) this.findViewById(R.id.bm_list_mode_select);
        mModeListView.setOnItemClickListener(this);
        
        // add mode bar
        mBarAddMode = (LinearLayout) findViewById(R.id.bar_add_mode);
        
        // save/cancel button
        mBtnSet = (Button) findViewById(R.id.btn_mode_list_set);
        mBtnCancel = (Button) findViewById(R.id.btn_mode_list_cancel);
        mBtnSet.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        Log.e(LOG_TAG, "<<onNewIntent>>");
    }

    @Override
    protected void onStart() {
        super.onStart();
        
        Log.i(LOG_TAG, "<<onStart>>");
        
        // start BatteryWidgetService in background
        Intent i = new Intent(this, BatteryWidgetService.class);
        i.setAction(BatteryWidgetService.ACTION_SVC_START);
        startService(i);
        
        mUserSwitch = false;
        mModeSwitching = mBatteryModeMgr.isModeSwiching();
        if (mModeSwitching && mProgressDialog != null) {
            mProgressDialog.show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        
        Log.i(LOG_TAG, "<<OnStop>>");
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        Log.i(LOG_TAG, "<<onPause>>");
    }
    
    private void setBarIcons(BatteryModeData data) {
        if (data == null) {
            return;
        }
        
        // wifi
        if (data.getWiFiOn()) {
            mBarWifi.setImageResource(R.drawable.ic_bm_wifi_enable);
        } else {
            mBarWifi.setImageResource(R.drawable.ic_bm_wifi_disable);
        }
        
        // mobile data
        if (data.getMobileDataOn()) {
            mBarMobileData.setImageResource(R.drawable.ic_bm_mobiledata_enable);
        } else {
            mBarMobileData.setImageResource(R.drawable.ic_bm_mobiledata_disable);
        }
        
        // brightness
        int brightness = data.getBrightness();
        if (brightness == -1) {
            mBarBrightness.setImageResource(R.drawable.ic_bm_brightness_auto);
        } else if (brightness < BatteryModeData.BRIGHTNESS_LEVEL_2) {
            mBarBrightness.setImageResource(R.drawable.ic_bm_brightness_1);
        } else if (brightness < BatteryModeData.BRIGHTNESS_LEVEL_3) {
            mBarBrightness.setImageResource(R.drawable.ic_bm_brightness_2);
        } else if (brightness == BatteryModeData.BRIGHTNESS_LEVEL_3) {
            mBarBrightness.setImageResource(R.drawable.ic_bm_brightness_3);
        }
        
        // bluetooth
        if (data.getBluetoothOn()) {
            mBarBluetooth.setImageResource(R.drawable.ic_bm_bluetooth_enable);
        } else {
            mBarBluetooth.setImageResource(R.drawable.ic_bm_bluetooth_disable);
        }
    } 
    
    void checkSelectedMode() {
        boolean bFind = false;
        for (int i = 0; i < mModeList.size(); i++) {
            if (mModeList.get(i).getId() == mSelectedMode.getId()) {
                mSelectedMode = mModeList.get(i);
                bFind = true;
                break;
            }
        }
        
        if (!bFind) {
            Log.i(LOG_TAG, "checkSelectedMode, NOT FOUND****");
            mSelectedMode = mCurrMode;
        } else {
            Log.i(LOG_TAG, "checkSelectMode, FOUND");
            if (mSelectedMode.getId() == mCurrMode.getId()) {
                mSelectedMode = mBatteryModeMgr.getCurrModeFromPrefs();
            }
        }
    }
    
    private void updateUI() {
        // icons bar
        mBarWifi = (ImageView) findViewById(R.id.bar_wifi);
        mBarMobileData = (ImageView) findViewById(R.id.bar_mobiledata);
        mBarBrightness = (ImageView) findViewById(R.id.bar_brightness);
        mBarBluetooth = (ImageView) findViewById(R.id.bar_bluetooth);
        
        setBarIcons(mSelectedMode);
        
        // list view
        ArrayList<BatteryModeListItem> listForAdapter = getModeList();
        mListAdapter = new BatteryModeListAdapter(this, listForAdapter);
        
        if (mModeListView != null) {
            mModeListView.setAdapter(mListAdapter);
        }
        
        BatteryModeListItem currModeItem = convertToListItem(mSelectedMode);
        mListAdapter.setCurrMode(currModeItem);
        
        // add mode
        if (mModeList.size() >= BatteryModeMgr.MAX_BATTERY_MODE_NUM) {
            mBarAddMode.setVisibility(View.GONE);
        } else {
            mBarAddMode.setVisibility(View.VISIBLE);
            mBarAddMode.setOnClickListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        Log.i(LOG_TAG, "<<OnResume>>");
        
        mModeList = mBatteryModeMgr.getAllMode();
        mCurrMode = mBatteryModeMgr.getCurrModeFromPrefs();
        
        checkSelectedMode();
    
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        Log.i(LOG_TAG, "<<onDestroy>>");
        
        this.unregisterReceiver(mCompleteReceiver);
    }

    /**
     * show alert dialog: battery mode remove
     * @param id
     * @param args
     */
    public void showMyAlert(int id, String args) {
        AlertDialog.Builder myDlgBuilder = new AlertDialog.Builder(this);

        if (id == DIALOG_ID_REMOVE_BATTERY_MODE) {
            
            final TextView tv = new TextView(getApplicationContext());
            //myDlgBuilder.setMessage(args);
            tv.setText(args);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f);
            myDlgBuilder.setView(tv);
            
            myDlgBuilder.setPositiveButton(R.string.bm_delete, this);
            myDlgBuilder.setNegativeButton(R.string.bm_cancel, this);
            myDlgBuilder.setTitle(R.string.bm_warning);
        }

        AlertDialog alertDlg = myDlgBuilder.create();
        alertDlg.show();
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
                mBatteryModeMgr.removeMode(mRemoveModeId);    
            }
            
            // remove a mode, so enable "add mode bar"
            if (mBarAddMode != null) {
                mBarAddMode.setVisibility(View.VISIBLE);
                mBarAddMode.setOnClickListener(this);
            }
                        
            // remove on UI list
            if (mListAdapter != null) {
                mListAdapter.removeMode(mRemoveModeId);
            }

            if (mRemoveModeId == mCurrMode.getId()) {
                // current mode is removed
                // force switching to the first mode in the list
                onSwitchMode(mModeList.get(0).getId());
            } else {
                // the selected mode is removed
                // re-focus the selected mode
                if (mRemoveModeId == mListAdapter.getSelectedModeId()) {
                    mListAdapter.setCurrMode(convertToListItem(mCurrMode));
                    setBarIcons(mCurrMode);
                }
            }
        } else if (arg1 == DialogInterface.BUTTON_NEGATIVE) {
            // do nothing
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mBarAddMode) {
            onAddMode();
        } else if (v == mBtnSet) {
            // switch mode/or do nothing
            long selectId = mListAdapter.getSelectedModeId();
            if (mCurrMode.getId() != selectId) {
                onSwitchMode(selectId);
            } else {
                finish();
            }
        } else if (v == mBtnCancel) {
            // do nothing
            finish();
        }
    }
}
