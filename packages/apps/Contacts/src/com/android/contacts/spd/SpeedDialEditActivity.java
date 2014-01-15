/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.android.contacts.spd;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.motorola.android.telephony.PhoneModeManager;
import com.motorola.internal.telephony.PhoneNumberUtilsExt;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 *
 * @author bluremployee
 */
public class SpeedDialEditActivity extends Activity
        implements SpeedDialDataManger.OnDataChangedListener, OnItemSelectedListener {

    static final String DIAL_FOR_SPEEDDIALASSIGNMENT = "save mDialForSpeedDialAssignment"; //MOT Calling code - IKSTABLEFIVE-12365
    private static final String TAG = "SpeedDialEditActivity";
    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End
    public static final int PICK_CONTACT_FOR_SPEED_DIAL_REQ = 2;

    private static final int DLG_DEFAULT_CARD = 1;
    private int mDialForSpeedDialAssignment;
    private String mNumberforSpeedDialAssignment;
    private SpeedDialDataManger mSpeedDialDataManger;
    private SpeedDialViewManager mSpeedDialViewManager;

    private static final int UPDATE_CALLBUTTON = 50;
    private static final long UPDATE_CALLBUTTON_DELAY = 4000;
    private boolean isDualMode;
    private ServiceState mServiceState = null;
    private ServiceState mSecondServiceState = null;
    private final BroadcastReceiver mReceiver = new CallButtonBroadcastReceiver();
    private GridView numGrid;
    private SpeedDialEditAdapter speedDialEditAdapter;
    private boolean mExcludeVoicemail;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_CALLBUTTON:
                    speedDialEditAdapter.updateDialCardTypes(numGrid);
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
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.speed_dial_edit_layout);
        isDualMode = PhoneModeManager.isDmds();
        mExcludeVoicemail = getResources().getBoolean(R.bool.ftr_36927_exclude_voicemail);
        numGrid = (GridView)findViewById(R.id.speed_dial_edit_gridview);
        //mSpeedDialViewManager = new SpeedDialViewManager();
        mSpeedDialDataManger = SpeedDialDataManger.getInstance(getApplicationContext());
        mSpeedDialDataManger.registerListenner(this);
        //MOT Calling code - IKSTABLEFIVE-12365
        if (state != null)
            mDialForSpeedDialAssignment = state.getInt(DIAL_FOR_SPEEDDIALASSIGNMENT, 0);
        //END - MOT Calling code - IKSTABLEFIVE-12365

        //setListAdapter(new SpeedDialEditListAdapter());
        speedDialEditAdapter = new SpeedDialEditAdapter(getApplicationContext(), isDualMode);
        numGrid.setAdapter(speedDialEditAdapter);
        numGrid.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            public void onItemClick(AdapterView<?> parent, View view, int position,
                    long id) {
                int temp = position + (position/9);
                if (!mExcludeVoicemail && temp == 0){
                    Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.speedDialVoiceDialRewriteMessage),
                            Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return;
                }
                String number = Utils.getSpeedDialNumberByPos(getApplicationContext(),(temp+1));
                if(!Utils.hasSpeedDialByNumber(getApplicationContext(), number)) {
                    AssignToSpeedDialMessage(temp+1);
                }
            }
        });

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return false;
        }
    }

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
            speedDialEditAdapter.updateDialCardTypes(numGrid);
            registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
        }
        speedDialEditAdapter.dataSetChangeRequest();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        speedDialEditAdapter.clearCache();
        speedDialEditAdapter = null;
        super.onDestroy();
    }

    //MOT Calling Code - IKSTABLEFIVE-12365
    @Override
    protected void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        icicle.putInt(DIAL_FOR_SPEEDDIALASSIGNMENT, mDialForSpeedDialAssignment);
    }
    //END - MOT Calling Code - IKSTABLEFIVE-12365

    private AsyncQueryHandler mNumberQueryHandler = null;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == PICK_CONTACT_FOR_SPEED_DIAL_REQ && resultCode == RESULT_OK && intent != null) {
            Uri uri = intent.getData();
            if(DBG) Log.d(TAG, "URI received is : " + uri);
            if (uri != null) {
                if(mNumberQueryHandler == null) {
                    mNumberQueryHandler = new AsyncQueryHandler(getContentResolver()) {
                        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    mNumberforSpeedDialAssignment = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
                                    //mSpeedDialDataManger.assignSpeedDial(mDialForSpeedDialAssignment, mNumberforSpeedDialAssignment);
                                    if (isDualMode) {
                                        int lastCallType = Utils.getLastCallCardType(getApplicationContext(), mNumberforSpeedDialAssignment);
                                        int contactLocation = Utils.getContactLocation(getApplicationContext(), mNumberforSpeedDialAssignment);
                                        if (ContactsUtils.isPhoneEnabled(TelephonyManager.PHONE_TYPE_CDMA)
                                                && ContactsUtils.isPhoneEnabled(TelephonyManager.PHONE_TYPE_GSM)) {
                                            if (Utils.hasSpeedDialByNumber(getApplicationContext(), mNumberforSpeedDialAssignment)) {
                                                String confirmationMsg =
                                                    getResources().getString(R.string.speedDial_numberAlreadyExists,
                                                    PhoneNumberUtilsExt.formatNumber(getApplicationContext(), mNumberforSpeedDialAssignment,
                                                            null, ContactsUtils.getCurrentCountryIso(getApplicationContext())));
                                                Toast toast = Toast.makeText(getApplicationContext(), confirmationMsg, Toast.LENGTH_SHORT);
                                                toast.setGravity(Gravity.CENTER, 0, 0);
                                                toast.show();
                                            } else {
                                                showDialog(DLG_DEFAULT_CARD);
                                            }
                                        } else {
                                            mSpeedDialDataManger.assignSpeedDial(mDialForSpeedDialAssignment, mNumberforSpeedDialAssignment);
                                            if (lastCallType == TelephonyManager.PHONE_TYPE_GSM
                                                    || lastCallType == TelephonyManager.PHONE_TYPE_CDMA){
                                                Utils.setupDefaultCallCardPrefence(getApplicationContext(), mDialForSpeedDialAssignment, lastCallType);
                                            } else if (contactLocation == TelephonyManager.PHONE_TYPE_GSM){
                                                Utils.setupDefaultCallCardPrefence(getApplicationContext(), mDialForSpeedDialAssignment, TelephonyManager.PHONE_TYPE_GSM);
                                            } else {
                                                Utils.setupDefaultCallCardPrefence(getApplicationContext(), mDialForSpeedDialAssignment, TelephonyManager.PHONE_TYPE_CDMA);
                                            }
                                        }
                                        speedDialEditAdapter.dataSetChangeRequest();
                                    } else {
                                        mSpeedDialDataManger.assignSpeedDial(mDialForSpeedDialAssignment, mNumberforSpeedDialAssignment);
                                    }
                                }
                                cursor.close();
                            }
                        }
                    };
                }
                // Query for the phone number
                final String[] PHONES_PROJECTION = { Phone.NUMBER };
                mNumberQueryHandler.startQuery(0, null, uri, PHONES_PROJECTION, null, null, null);
                speedDialEditAdapter.dataSetChangeRequest();
            }
        }
    }

    public void AssignToSpeedDialMessage(int dialerDigit) {
        mDialForSpeedDialAssignment = dialerDigit;
        Intent intent = new Intent(Intent.ACTION_PICK, Phone.CONTENT_URI);
        intent.putExtra("INTENTEXTRA_BOOLEAN_SELECTMULTIPLE", false);
        startActivityForResult(intent, PICK_CONTACT_FOR_SPEED_DIAL_REQ);
    }

    @Override
    public void onDataChanged(SpeedDialInfo data) {
        // tracer.t("onDataChanged " + data.mBindKey + "=" + data.mBaseNumber);
        /*speedDialEditAdapter.dataSetChangeRequest();
        View view = mSpeedDialViewManager.findViewByData(data);
        if(view != null) {
            setupViewBySpeedDialInfo(view, data);
        }*/
        speedDialEditAdapter.dataSetChangeRequest();
    }

    public void onItemSelected(AdapterView<?> arg0, View view, int position, long id) {
        int temp = position + (position/9);
        String number = Utils.getSpeedDialNumberByPos(getApplicationContext(),(temp+1));
        if(number != null && Utils.hasSpeedDialByNumber(getApplicationContext(), number)) {
            Utils.unassignSpeedDial(getApplicationContext(), number);
        }
    }

    public void onNothingSelected(AdapterView<?> arg0) {

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

    /*private void setupViewBySpeedDialInfo(View view, SpeedDialInfo data) {
        ImageView icSpdPosition = (ImageView) view.findViewById(R.id.pickerPosition);
        ImageView ivPhoto = (ImageView) view.findViewById(R.id.pickerImage);
        TextView tvName = (TextView) view.findViewById(R.id.pickerName);
        TextView tvNumber = (TextView) view.findViewById(R.id.pickerNumber);
        View areaAddSpd = view.findViewById(R.id.addSpeedDialClickArea);
        View areaRmvSpd = view.findViewById(R.id.removeSpeedDialClickArea);

        final int spdID = data.mBindKey;
        boolean bLocked = data.mLocked;
        String number = data.mBaseNumber;

        // tracer.t("setup view for " + spdID + " " + view + "number=" + number);

        icSpdPosition.setImageResource(getSpdDialPositionIcon(spdID));
        tvName.setText(data.mContactName);
        tvNumber.setText(data.mDisplayNumber);
        tvNumber.setVisibility(TextUtils.isEmpty(data.mDisplayNumber) ? View.GONE : View.VISIBLE); // Moto Dialer code - IKHSS6-5093
        ivPhoto.setImageBitmap(data.mContactPhoto);

        if (mSpeedDialDataManger.isVoicemailPos(spdID)) {
            tvName.setText(TelephonyManager.getDefault().getVoiceMailAlphaTag());
            ivPhoto.setImageResource(R.drawable.ic_launcher_voicemail);
            ivPhoto.setVisibility(View.VISIBLE);
            areaAddSpd.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Toast toast = Toast.makeText(getApplicationContext(), R.string.speedDialVoiceDialRewriteMessage, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            });
            areaRmvSpd.setVisibility(View.GONE);
        } else if (bLocked) {
            ivPhoto.setVisibility(View.VISIBLE);
            areaAddSpd.setClickable(false);
            areaRmvSpd.setVisibility(View.GONE);
        } else if (TextUtils.isEmpty(number)) {
            tvName.setText(R.string.speedDialAddAvailable);
            ivPhoto.setVisibility(View.GONE);
            areaAddSpd.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    AssignToSpeedDialMessage(spdID);
                }
            });
            areaRmvSpd.setVisibility(View.GONE);
        } else {
            ivPhoto.setVisibility(View.VISIBLE);

            areaAddSpd.setClickable(false);
            areaRmvSpd.setVisibility(View.VISIBLE);
            areaRmvSpd.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mSpeedDialDataManger.unassignSpeedDial(spdID);
                }
            });
        }
    }*/
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

    protected Dialog onCreateDialog(int id){
        switch(id){
        case DLG_DEFAULT_CARD:
            return new SpeedDialerChooseCardDialog(this){
                public void onItemClick(AdapterView<?> l, View v, int position, long id){
                    mSpeedDialDataManger.assignSpeedDial(mDialForSpeedDialAssignment, mNumberforSpeedDialAssignment);
                    Utils.setupDefaultCallCardPrefence(getContext(), mDialForSpeedDialAssignment, position + 1);
                    speedDialEditAdapter.dataSetChangeRequest();
                    dismiss();
                }
            };
        }
        return null;
    }

    protected void onPrepareDialog(int id, Dialog dialog){
        switch(id){
        case DLG_DEFAULT_CARD:
            SpeedDialerChooseCardDialog cdccDialog = (SpeedDialerChooseCardDialog) dialog;
            cdccDialog.setData(mNumberforSpeedDialAssignment);
            break;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*class SpeedDialEditListAdapter extends BaseAdapter {

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
                view.setFocusable(true); // this line can prevent list item be clicked while no click-able element
                // tracer.t("alloc view for key " + (pos + 1) + " " + view);
            } else {
                // tracer.t("reuse view for key " + (pos + 1) + " " + view);
            }
            if(mSpeedDialViewManager.bindView(view, data)) {
                setupViewBySpeedDialInfo(view, data);
            }
            return view;
        }
    }*/
}
