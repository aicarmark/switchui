/*
 * Copyright (C) 2010, Motorola, Inc,
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts.spd;

import com.android.contacts.ContactsUtils;
import com.android.contacts.R;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

//MOTO MOD BEGIN IKHSS7-2038
import com.motorola.internal.telephony.PhoneNumberUtilsExt;
import com.motorola.android.telephony.PhoneModeManager;
//MOTO MOD END IKHSS7-2038

/**
 * Helper class to initialize and run the InCallScreen's "Manage conference" UI.
 */
public class SpeedDialPicker extends Activity
        implements SpeedDialDataManger.OnDataChangedListener {

    private static final boolean VDBG = (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final String TAG = "SpeedDialPicker";
    // call types used in filtering [ MOTO:Begin A
    public static final String CALL_NUMBER_TYPE = "com.android.phone.SpeedDialPicker.CallNumberType";
    // call types used in filtering [ MOTO:Begin A
    public static final String CALL_NUMBER = "com.android.phone.SpeedDialPicker.CallNumber";

    private int mSpeedDialPos;
    private String mSpeedDialNumber;
    private DialogInterface.OnClickListener mBtnListener;
    private AlertDialog mOverwriteSpeedDialDialog;

    private SpeedDialDataManger mSpeedDialDataManger;
    private SpeedDialViewManager mSpeedDialViewManager;
    private String mCurrentCountryIso; //MOTO Dialer Code - IKHSS7-2091

    private GridView numGrid;
    private SpeedDialPickerAdapter speedDialPickerAdapter;
    private boolean isDualMode;
    private ServiceState mServiceState = null;
    private ServiceState mSecondServiceState = null;
    private final BroadcastReceiver mReceiver = new CallButtonBroadcastReceiver();
    private static final int DLG_DEFAULT_CARD = 1;
    private static final int UPDATE_CALLBUTTON=50;
    private static final long UPDATE_CALLBUTTON_DELAY=4000;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_CALLBUTTON:
                    speedDialPickerAdapter.updateDialCardTypes(numGrid);
                    break;
            }
        }
    };

    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        /**
         * Listen for service state changes so that we can update speed call button accordingly
         * for example, when airplane mode switches
         */
        @Override
        public void onServiceStateChanged(ServiceState state) {
            if (mServiceState != null) {
                if (mServiceState.getState() != state.getState()) {
                    mHandler.removeMessages(UPDATE_CALLBUTTON);
                    Message msg = Message.obtain(mHandler,UPDATE_CALLBUTTON);
                    mHandler.sendMessageDelayed(msg, ContactsUtils.UPDATE_VIEWS_DELAY);
                }
            }
            mServiceState = state;
        }
    };

    PhoneStateListener mSecondPhoneStateListener = new PhoneStateListener() {
        /**
         * Listen for service state changes so that we can update speed call button accordingly
         * for example, when airplane mode switches
         */
        @Override
        public void onServiceStateChanged(ServiceState state) {
            if (mSecondServiceState != null) {
                if (mSecondServiceState.getState() != state.getState()) {
                    mHandler.removeMessages(UPDATE_CALLBUTTON);
                    Message msg = Message.obtain(mHandler,UPDATE_CALLBUTTON);
                    mHandler.sendMessageDelayed(msg, ContactsUtils.UPDATE_VIEWS_DELAY);
                }
            }
            mSecondServiceState = state;
        }
    };

    @Override
    public void onCreate(Bundle icicle) {

        super.onCreate(icicle);
        isDualMode = PhoneModeManager.isDmds();
        setContentView(R.layout.speed_dial_picker_layout);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Show the custom action bar but hide the home icon and title
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        }

        //mSpeedDialViewManager = new SpeedDialViewManager();
        mSpeedDialDataManger = SpeedDialDataManger.getInstance(getApplicationContext());
        mSpeedDialDataManger.registerListenner(this);

        Intent intent = getIntent();
        //String numberType = intent.getStringExtra(CALL_NUMBER_TYPE);
        //TextView tvType = (TextView) findViewById(R.id.callerNumberType);
        //tvType.setText(numberType);

        mSpeedDialNumber = intent.getStringExtra(CALL_NUMBER);
        //TextView tvNumber = (TextView) findViewById(R.id.callerNumber);
        //MOTO Dialer Code Start - IKHSS7-2091
        mCurrentCountryIso = ContactsUtils.getCurrentCountryIso(this);

        /* MOTO MOD BEGIN IKHSS7-2038
         * Changed calling PhoneNumberUtils.formatNumber API to New API
         * PhoneNumberUtilsExt.formatNumber for Hyphensation Feature 35615
        */
        //tvNumber.setText(PhoneNumberUtilsExt.formatNumber(this, mSpeedDialNumber, null, mCurrentCountryIso));
        //MOTO MOD END - IKHSS7-2038
        //MOTO Dialer Code End - IKHSS7-2091

        /*Spinner spinner = (Spinner) findViewById(R.id.speed_dial_spinner);
        spinner.setAdapter(new SpeedDialSpinnerAdapter());

        int pos = Utils.adviseSpeedDialPosForAssign(this); //END MOT FID 36927-Speeddial#1 IKCBS-2013 // Motorola, Aug-05-2011, IKPIM-282
        // Sets the current position with the lowest Speed Dial position available
        if (pos > 0) {
            spinner.setSelection(pos - 1);
        } else {
            spinner.setSelection(0);
        }*/

        numGrid = (GridView)findViewById(R.id.speed_dial_picker_gridview);
        speedDialPickerAdapter = new SpeedDialPickerAdapter(getApplicationContext(), isDualMode);
        mBtnListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                switch (whichButton) {
                    case DialogInterface.BUTTON_POSITIVE:
                        if(isDualMode) {
                            assignToSpeedDial();
                        } else {
                            mSpeedDialDataManger.assignSpeedDial(mSpeedDialPos, mSpeedDialNumber); // Motorola, July-28-2011, IKMAIN-24377
                            speedDialPickerAdapter.dataSetChangeRequest();
                            dialog.cancel();
                            finish();
                        }
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                            dialog.cancel();
                        break;
                    }
                }
        };
        numGrid.setAdapter(speedDialPickerAdapter);
        numGrid.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {
                int temp = position + (position/9);
                mSpeedDialPos = temp + 1;
                String number = Utils.getSpeedDialNumberByPos(getApplicationContext(), mSpeedDialPos);
                if(!Utils.hasSpeedDialByNumber(getApplicationContext(), number)) {
                    doAddSpeedDial();
                } else {
                    showOverwriteSpeedDialMessage();
                }
            }
        });
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.speed_dial_picker_action_bar, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            case R.id.cancel_menu_item:
                finish();
                break;
            case R.id.save_menu_item:
                doAddSpeedDila();
                break;
            default:
               return false;
        }
        return true;
    }*/

    @Override
    protected void onResume() {
        super.onResume();
        TelephonyManager defaultMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        defaultMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        if (isDualMode) {
            /* to-pass-build, Xinyu Liu/dcjf34 */ 
            TelephonyManager secondMgr = null;//(TelephonyManager) getSystemService(SECONDARY_TELEPHONY_SERVICE);
            if (secondMgr != null) {
                secondMgr.listen(mSecondPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
            }
            speedDialPickerAdapter.updateDialCardTypes(numGrid);
            registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
        }
        mSpeedDialNumber = getIntent().getStringExtra(CALL_NUMBER);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop listening for phone state changes.
        TelephonyManager defaultMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        defaultMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        if (isDualMode) {
            /* to-pass-build, Xinyu Liu/dcjf34 */ 
            TelephonyManager secondMgr = null;//(TelephonyManager) getSystemService(SECONDARY_TELEPHONY_SERVICE);
            if (secondMgr != null) {
                secondMgr.listen(mSecondPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
            unregisterReceiver(mReceiver);
        }
    }

    @Override
    protected void onDestroy() {
        mSpeedDialDataManger.unRegisterListenner(this);
        if (mOverwriteSpeedDialDialog != null) {
            mOverwriteSpeedDialDialog.dismiss();
            mOverwriteSpeedDialDialog = null;
        }
        speedDialPickerAdapter.clearCache();
        speedDialPickerAdapter = null;
        super.onDestroy();
    }

    private class CallButtonBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                mHandler.removeMessages(UPDATE_CALLBUTTON);
                Message msg = Message.obtain(mHandler,UPDATE_CALLBUTTON);
                mHandler.sendMessageDelayed(msg,UPDATE_CALLBUTTON_DELAY);
            }
        }
    }

    // Dialog shown when the user wants to overwrite an existing Speed Dial number with another number
    private void showOverwriteSpeedDialMessage() {
        /* MOTO MOD BEGIN IKHSS7-2038
         * Changed calling PhoneNumberUtils.formatNumber API to New API
         * PhoneNumberUtilsExt.formatNumber for Hyphensation Feature 35615
        */
        mOverwriteSpeedDialDialog = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle(getString(R.string.speedDialOverwrite))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(getString(R.string.speedDialRewrite,
                        Integer.toString(mSpeedDialPos),
                        PhoneNumberUtilsExt.formatNumber(this, mSpeedDialNumber, null, mCurrentCountryIso)))
                .setPositiveButton(getString(R.string.ok), mBtnListener)
                .setNegativeButton(getString(R.string.cancelButton), mBtnListener)
                .setCancelable(false)
                .create();
        //MOTO END IKHSS7-2038
        mOverwriteSpeedDialDialog.show();
    }

    @Override
    public void onDataChanged(SpeedDialInfo data) {
        /*tracer.t("SpeedDialPicker.onDataChanged");
        View view = mSpeedDialViewManager.findViewByData(data);
        if (view != null) {
            setupViewBySpeedDialInfo(view, data);
        }*/
        speedDialPickerAdapter.dataSetChangeRequest();
    }

    /*private void setupViewBySpeedDialInfo(View view, SpeedDialInfo data) {
        ImageView icSpdPosition = (ImageView) view.findViewById(R.id.pickerPosition);
        ImageView ivPhoto = (ImageView) view.findViewById(R.id.pickerImage);
        TextView tvName = (TextView) view.findViewById(R.id.pickerName);
        TextView tvNumber = (TextView) view.findViewById(R.id.pickerNumber);

        int spdID = data.mBindKey;
        boolean bLocked = data.mLocked;
        String number = data.mBaseNumber;

        tracer.t("setup view for " + spdID + " " + view);

        icSpdPosition.setImageResource(getSpdDialPositionIcon(spdID));
        tvName.setText(data.mContactName);
        tvNumber.setText(data.mDisplayNumber);
        tvNumber.setVisibility(TextUtils.isEmpty(data.mDisplayNumber) ? View.GONE : View.VISIBLE); // Moto Dialer code - IKHSS6-5093
        ivPhoto.setImageBitmap(data.mContactPhoto);

        if (mSpeedDialDataManger.isVoicemailPos(spdID)) {
            tvName.setText(TelephonyManager.getDefault().getVoiceMailAlphaTag());
            ivPhoto.setImageResource(R.drawable.ic_launcher_voicemail);
            ivPhoto.setVisibility(View.VISIBLE);
        } else if (bLocked) {
            ivPhoto.setVisibility(View.VISIBLE);
        } else if (TextUtils.isEmpty(number)) {
            tvName.setText(R.string.speedDialAddAvailable);
            ivPhoto.setVisibility(View.GONE);
        } else {
            ivPhoto.setVisibility(View.VISIBLE);
        }
    }*/

    private void doAddSpeedDial() {
        //Spinner spinner = (Spinner) findViewById(R.id.speed_dial_spinner);
        //mSpeedDialPos = spinner.getSelectedItemPosition() + 1;

        if (!mSpeedDialDataManger.isVoicemailPos(mSpeedDialPos)) { //MOT FID 36927-Speeddial#1 IKCBS-2013
            Boolean numberInSpd = mSpeedDialDataManger.hasSpeedDialByNumber(mSpeedDialNumber); // Motorola, Aug-05-2011, IKPIM-282

            if (!numberInSpd) {
                String number = mSpeedDialDataManger.getSpeedDialNumberByPos(mSpeedDialPos); // Motorola, Aug-05-2011, IKPIM-282
                if (null != number) {
                    showOverwriteSpeedDialMessage();
                }
                else {
                    if (isDualMode) {
                        assignToSpeedDial();
                    } else {
                        mSpeedDialDataManger.assignSpeedDial(mSpeedDialPos, mSpeedDialNumber); // Motorola, July-28-2011, IKMAIN-24377
                        speedDialPickerAdapter.dataSetChangeRequest();
                        finish();
                    }
                }
            } else {
                // We should not be here
                /* MOTO MOD BEGIN IKHSS7-2038
                * Changed calling PhoneNumberUtils.formatNumber API to New API
                * PhoneNumberUtilsExt.formatNumber for Hyphensation Feature 35615
                */
                String confirmationMsg =
                        getApplicationContext().getResources().getString(R.string.speedDial_numberAlreadyExists,
                        PhoneNumberUtilsExt.formatNumber(this, mSpeedDialNumber, null, mCurrentCountryIso));
                //MOTO END IKHSS7-2038
                Toast toast = Toast.makeText(getApplicationContext(), confirmationMsg, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                finish();
            }
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.speedDialVoiceDialRewriteMessage),
                    Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    private void assignToSpeedDial(){
        int lastCallType = Utils.getLastCallCardType(getApplicationContext(), mSpeedDialNumber);
        int contactLocation = Utils.getContactLocation(getApplicationContext(), mSpeedDialNumber);
        if (ContactsUtils.isPhoneEnabled(TelephonyManager.PHONE_TYPE_CDMA)
                && ContactsUtils.isPhoneEnabled(TelephonyManager.PHONE_TYPE_GSM)) {
            showDialog(DLG_DEFAULT_CARD);
        } else {
            if (lastCallType == TelephonyManager.PHONE_TYPE_GSM
                    || lastCallType == TelephonyManager.PHONE_TYPE_CDMA) {
                Utils.setupDefaultCallCardPrefence(getApplicationContext(), mSpeedDialPos, lastCallType);
            } else if (contactLocation == TelephonyManager.PHONE_TYPE_GSM) {
                Utils.setupDefaultCallCardPrefence(getApplicationContext(), mSpeedDialPos, TelephonyManager.PHONE_TYPE_GSM);
            } else {
                Utils.setupDefaultCallCardPrefence(getApplicationContext(), mSpeedDialPos, TelephonyManager.PHONE_TYPE_CDMA);
            }
            mSpeedDialDataManger.assignSpeedDial(mSpeedDialPos, mSpeedDialNumber);
            speedDialPickerAdapter.dataSetChangeRequest();
            finish();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id){
        switch(id){
        case DLG_DEFAULT_CARD:
            return new SpeedDialerChooseCardDialog(this){
                public void onItemClick(AdapterView<?> l, View v, int position, long id){
                    Utils.setupDefaultCallCardPrefence(getContext(), mSpeedDialPos, position + 1);
                    mSpeedDialDataManger.assignSpeedDial(mSpeedDialPos, mSpeedDialNumber);
                    speedDialPickerAdapter.dataSetChangeRequest();
                    dismiss();
                    finish();
                }
            };
        }
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog){
        switch(id){
        case DLG_DEFAULT_CARD:
            SpeedDialerChooseCardDialog cdccDialog = (SpeedDialerChooseCardDialog) dialog;
            cdccDialog.setData(mSpeedDialNumber);
            break;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // map from index to icon resource
    /*static private int[] mIconResource = new int[] {
            R.drawable.sd01, R.drawable.sd02,
            R.drawable.sd03, R.drawable.sd04,
            R.drawable.sd05, R.drawable.sd06,
            R.drawable.sd07, R.drawable.sd08,
            R.drawable.sd09,
    };

    private static int getSpdDialPositionIcon(int pos) {
        int index = pos - 1;
        if (index >= 0 && index < mIconResource.length)
            return mIconResource[index];
        else
            return R.drawable.ic_launcher_contacts;
    }*/

    /*class SpeedDialSpinnerAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return SpeedDialDataManger.SPD_COUNT;
        }

        @Override
        public Object getItem(int arg0) {
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            return 0;
        }

        @Override
        public View getView(int pos, View view, ViewGroup vg) {
            SpeedDialInfo data = mSpeedDialDataManger.getData(pos + 1);
            if (view == null) {
                view = View.inflate(getApplicationContext(), R.layout.speed_dial_list_item, null);
                view.findViewById(R.id.removeSpeedDialClickArea).setVisibility(View.GONE);
                // tracer.t("alloc view for key " + (pos + 1) + " " + view);
            } else {
                // tracer.t("reuse view for key " + (pos + 1) + " " + view);
            }
            if (mSpeedDialViewManager.bindView(view, data)) {
                setupViewBySpeedDialInfo(view, data);
            }
            return view;
        }
    }*/
}
