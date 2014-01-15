package com.motorola.firewall;

import com.android.phone.PhoneHub;
import com.android.mms.SmsHub;
// import com.qihoo.keywordservice.InterfaceService;

import com.motorola.firewall.FireWall.BlockLog;
import com.motorola.firewall.FireWall.Name;
import com.motorola.firewall.FireWall.Settings;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.CallerInfo;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class FireWallService extends Service {

    private static final String[] PHONES_PROJECTION = new String[] {
            PhoneLookup._ID, // 0
            PhoneLookup.DISPLAY_NAME, // 1
            PhoneLookup.NUMBER, // 2
            PhoneLookup.TYPE, // 3
            PhoneLookup.LABEL, //4
            PhoneLookup.LOOKUP_KEY //5
    };
    private static final String[] NAMEPROJECTION = new String[] {
            Name._ID, // 0
            Name.CACHED_NAME, // 1
            Name.PHONE_NUMBER_KEY //2
    };
    private static final String[] mlist = new String[] {
        Name.WHITELIST,
        Name.WHITEPATTERN,
        Name.BLACKLIST,
        Name.BLACKPATTERN,
        Name.WHITENAME,
        Name.BLACKNAME
    };
    private static final int COLUMN_INDEX_NAME = 1;
    private static final int COLUMN_INDEX_NUMBER = 2;
    private static final int COLUMN_INDEX_PHONE_LOOKUP = 5;

    private NotificationManager mNM;
    private Notification notification;
    private SharedPreferences mPre;
    private FirewallLogListActivity mLogListActivity = null;
    private static FireWallService sMe;
    private boolean mCallIn = false;
    // private boolean mCallOut = false;
    private boolean mSmsIn = false;
    private boolean mCallInCdma = false;
    private boolean mSmsInCdma = false;
    private boolean mCallInGsm = false;
    private boolean mSmsInGsm = false;
    // private boolean mSmsKeyword = false;
    private boolean mCallLog = true;
    private boolean mSmsLog = true;
    private String mCallInbtype = "";
    // private String mCallOutbtype = "";
    private String mSmsInbtype = "";
    private String mBlock = "";
    private String[] mincallblock;
    // private String[] moutcallblock;
    private String[] minsmsblock;
    private String mcachename;
    private static final String TAG = "FireWallService";
    private static final String ACTION_WIDGET_RECEIVER = "ActionReceiverWidget";
    PhoneHub mCallService = null;
    SmsHub mSmsService = null;
    // InterfaceService mKeywordService = null;
    
    private static String mCallInVal = "";
    private static String mSmsInVal = "";
    private static String mCallInbtypeVal = "";
    private static String mSmsInbtypeVal = "";

    public static int notifyid = 0;
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("FireWallService", ">>>>>>>>>>>>>>enter onCreate()");
        sMe = this;
        mincallblock = getResources().getStringArray(R.array.incoming_call_block_list_value);
        // moutcallblock = getResources().getStringArray(R.array.outgoing_call_block_list_value);
        minsmsblock = getResources().getStringArray(R.array.incoming_sms_block_list_value);
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showNotification();
	checkCallAndSmsFirewall();
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
	super.onDestroy();
	Log.d("FireWallService", ">>>>>>>>>>>>>>enter onDestroy()");
        mNM.cancelAll();
        mCallIn = false;
        // mCallOut = false;
        mSmsIn = false;
        if (mCallService != null) {
            try {
                mCallService.unregisterCallback(mCallback);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service
                // has crashed.
            }
            unbindService(mCallServiceConn);
            mCallService = null;
        }
        if (mSmsService != null) {
            try {
                mSmsService.unregisterCallback(mSmsback);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service
                // has crashed.
            }
            unbindService(mSmsServiceConn);
            mSmsService = null;
        }
//        if (mKeywordService != null) {
//            unbindService(mSmsKeywordServiceConn);
//            mKeywordService = null;
//        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStart(intent, startId);
        Log.d("FireWallService", ">>>>>>>>>>>>>>enter onStartCommand()"); 
        checkCallAndSmsFirewall();
	/*mPre = PreferenceManager.getDefaultSharedPreferences(this);
        ContentResolver mresolver = getContentResolver();

        mCallIn = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.INBLOCK_CALL_ON));
        mSmsIn = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.INBLOCK_SMS_ON));
        mCallInCdma = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_CALL_REJECT_CDMA));
        mCallInGsm = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_CALL_REJECT_GSM));
        mSmsInCdma = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_SMS_REJECT_CDMA));
        mSmsInGsm = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_SMS_REJECT_GSM));
        mCallInbtype = FireWallSetting.getDBValue(mresolver, Settings.IN_BLOCK_CALL_LIST);
        mSmsInbtype = FireWallSetting.getDBValue(mresolver, Settings.IN_BLOCK_SMS_LIST);
        mBlock = FireWallSetting.getDBValue(mresolver, Settings.CALL_BLOCK_TYPE);
        mCallLog = mPre.getBoolean("call_log_on", true);
        mSmsLog = mPre.getBoolean("sms_log_on", true);
        //update widget
        configurationChanged();
        
        if (mCallService == null) {
            // if ((mCallIn || mCallOut)) {
            if ( mCallIn || mCallInCdma || mCallInGsm) {
                bindService(new Intent(PhoneHub.class.getName()), mCallServiceConn,
                        Context.BIND_AUTO_CREATE);
            }
        } else {
            // if ((! mCallIn) && (! mCallOut)) {
            if ((! mCallIn)  && (! mCallInCdma) && (! mCallInGsm) ){
                try {
                    mCallService.unregisterCallback(mCallback);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
                unbindService(mCallServiceConn);
                mCallService = null;
            }
        }
        if (mSmsService == null) {
            // SMS firewall not started
            // if (mSmsIn || mSmsKeyword) {
            if (mSmsIn || mSmsInCdma || mSmsInGsm) {
                // Turn on SMS firewall
                bindService(new Intent(SmsHub.class.getName()), mSmsServiceConn,
                        Context.BIND_AUTO_CREATE);
            }
        } else {
            // SMS firewall already started
            // if (( ! mSmsIn) && (! mSmsKeyword)) {
            if (( ! mSmsIn) && ( ! mSmsInCdma) && ( ! mSmsInGsm)) {
                // Turn off SMS firewall
                try {
                    mSmsService.unregisterCallback(mSmsback);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
                unbindService(mSmsServiceConn);
                mSmsService = null;
            }
        }
        
//        if (mKeywordService == null) {
//            // SMS keyword filter not started
//            if (mSmsKeyword) {
//                // Turn on key word filter
//                bindService(new Intent(InterfaceService.class.getName()), mSmsKeywordServiceConn,
//                        Context.BIND_AUTO_CREATE);
//            }
//        } else {
//            if ( ! mSmsKeyword) {
//                unbindService(mSmsKeywordServiceConn);
//                mKeywordService = null;
//            }
//        }*/
        return START_STICKY;
        
    }

    private void checkCallAndSmsFirewall() {
        
	mPre = PreferenceManager.getDefaultSharedPreferences(this);
        ContentResolver mresolver = getContentResolver();

        mCallIn = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.INBLOCK_CALL_ON));
        mSmsIn = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.INBLOCK_SMS_ON));
        mCallInCdma = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_CALL_REJECT_CDMA));
        mCallInGsm = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_CALL_REJECT_GSM));
        mSmsInCdma = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_SMS_REJECT_CDMA));
        mSmsInGsm = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_SMS_REJECT_GSM));
        mCallInbtype = FireWallSetting.getDBValue(mresolver, Settings.IN_BLOCK_CALL_LIST);
        mSmsInbtype = FireWallSetting.getDBValue(mresolver, Settings.IN_BLOCK_SMS_LIST);
        mBlock = FireWallSetting.getDBValue(mresolver, Settings.CALL_BLOCK_TYPE);
        mCallLog = mPre.getBoolean("call_log_on", true);
        mSmsLog = mPre.getBoolean("sms_log_on", true);
        //update widget
        configurationChanged();
        
        if (mCallService == null) {
            // if ((mCallIn || mCallOut)) {
            if ( mCallIn || mCallInCdma || mCallInGsm) {
                bindService(new Intent(PhoneHub.class.getName()), mCallServiceConn,
                        Context.BIND_AUTO_CREATE);
            }
        } else {
            // if ((! mCallIn) && (! mCallOut)) {
            if ((! mCallIn)  && (! mCallInCdma) && (! mCallInGsm) ){
                try {
                    mCallService.unregisterCallback(mCallback);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
                unbindService(mCallServiceConn);
                mCallService = null;
            }
        }
        if (mSmsService == null) {
            // SMS firewall not started
            // if (mSmsIn || mSmsKeyword) {
            if (mSmsIn || mSmsInCdma || mSmsInGsm) {
                // Turn on SMS firewall
                bindService(new Intent(SmsHub.class.getName()), mSmsServiceConn,
                        Context.BIND_AUTO_CREATE);
            }
        } else {
            // SMS firewall already started
            // if (( ! mSmsIn) && (! mSmsKeyword)) {
            if (( ! mSmsIn) && ( ! mSmsInCdma) && ( ! mSmsInGsm)) {
                // Turn off SMS firewall
                try {
                    mSmsService.unregisterCallback(mSmsback);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
                unbindService(mSmsServiceConn);
                mSmsService = null;
            }
        }
    }

    private void showNotification() {
        // we'll use the same text for the ticker and the expanded notification
        CharSequence text;
        int picid;
        ContentResolver mresolver = getContentResolver();
        // Set the icon, scrolling text and timestamp
        if (FireWallSetting.isVipMode(mresolver)){
            text = getText(R.string.firewall_vip_mode_started);
            picid = R.drawable.vip_stat_bar_connected;
            notifyid = R.string.firewall_vip_mode_started;
        }
        else {
            text = getText(R.string.firewall_service_started);
            picid = R.drawable.firewall_stat_bar_connected;
            notifyid = R.string.firewall_service_started;
        }
     
        notification = new Notification(picid, text, System.currentTimeMillis());
        notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        // The PendingIntent to launch our activity if the user selects this
        // notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, FireWallTab.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, text , text, contentIntent);

        // Send the notification.
        // We use a string id because it is a unique number. We use it later to
        // cancel.
        mNM.notify(notifyid, notification);     
    };
  
    private void setFirewallCallNotification(String phonenumber) {
        CharSequence number = (CharSequence) phonenumber;
        Intent mIntent = new Intent(this, FireWallTab.class);
        mIntent.setType(BlockLog.CONTENT_TYPE);
        int id = 0;
        ContentResolver mresolver = getContentResolver();
        if(FireWallSetting.isVipMode(mresolver)) {
            notification.icon = R.drawable.vip_stat_bar_call_blocked_successful;
            id = R.string.firewall_vip_mode_started;
        }
        else {
            notification.icon = R.drawable.firewall_stat_bar_call_blocked_successful;
            id = R.string.firewall_service_started;
        }
        notification.when = System.currentTimeMillis();
        notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, mIntent, 0);

        notification.setLatestEventInfo(this, getText(R.string.firewall_call_intercepted), number,
                contentIntent);

        mNM.notify(id, notification);
    }

    private void setFirewallSMSNotification(String phonenumber) {
        CharSequence number = (CharSequence) phonenumber;
        Intent mIntent = new Intent(this, FireWallTab.class);
        mIntent.setType(BlockLog.CONTENT_TYPE);
        int id = 0;
        ContentResolver mresolver = getContentResolver();
        if(FireWallSetting.isVipMode(mresolver)) {
            notification.icon = R.drawable.vip_stat_bar_call_blocked_successful;
            id = R.string.firewall_vip_mode_started;
        }
        else {
            notification.icon = R.drawable.firewall_stat_bar_sms_blocked_successful;
            id = R.string.firewall_service_started;
        }
        notification.when = System.currentTimeMillis();
        notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, mIntent, 0);

        notification.setLatestEventInfo(this, getText(R.string.firewall_sms_intercepted), number,
                contentIntent);

        mNM.notify(id, notification);
    }

    /**
     * Returns the singleton instance of the FireWallService.
     */
    static FireWallService getInstance() {
        return sMe;
    }

    public void setFirewallLogListActivity(FirewallLogListActivity logActivity) {
        mLogListActivity = logActivity;
    }

    private CallFirewallCallback mCallback = new CallFirewallCallback.Stub() {
        /**
         * This is called by the remote service regularly to tell us about new
         * values. Note that IPC calls are dispatched through a thread pool
         * running in each process, so the code executing here will NOT be
         * running in our main thread like most other things -- so, to update
         * the UI, we need to use a Handler to hop over there.
         */
        public int CheckNumberBlock(String phonenumber, int calltype, boolean updatelog, int network) {
            boolean res = false;
            Log.d(TAG,"call CheckNumberBlock phonenumber=" + phonenumber
                      + " calltype=" + calltype + " updatelog=" + updatelog + " network=" +network);
            String mnetnumber = phonenumber;
            if ((null != phonenumber) && (! phonenumber.equals(CallerInfo.UNKNOWN_NUMBER)) && ( ! phonenumber.equals(CallerInfo.PRIVATE_NUMBER))) {
                mnetnumber = PhoneNumberUtils.extractNetworkPortion(phonenumber);
            }
            mcachename = "";

            if (PhoneNumberUtils.isEmergencyNumber(mnetnumber)) {
                return 0;
            }
            if (calltype == Calls.INCOMING_TYPE) {
                if (mCallInCdma) {
                    if (network == TelephonyManager.PHONE_TYPE_CDMA) {
                        res = true;
                    }
                }
                if (!res && mCallInGsm) {
                    if (network == TelephonyManager.PHONE_TYPE_GSM) {
                        res = true;
                    }
                }
                if (!res && mCallIn) {
                    if (mCallInbtype.equals(mincallblock[1])) {
                        /* in_call_rejectall */
                        res = true;
                    } else if (mCallInbtype.equals(mincallblock[0])) {
                        /* in_call_pimlist */
                        res = !(inPIM(mnetnumber));
                    } else if (mCallInbtype.equals(mincallblock[2])) {
                        /* in_call_whitelist */
                        res = !(inNameList(mnetnumber, mCallInbtype));
                    } else if (mCallInbtype.equals(mincallblock[3])) {
                        /* in_call_blacklist */
                        res = inNameList(mnetnumber, mCallInbtype);
                    } else {
                        res = false;
                    }
                }
            } else if (calltype == Calls.OUTGOING_TYPE) {
                // if (mCallOut) {
                    // if (mCallOutbtype.equals(moutcallblock[1])) {
                        // res = true;
                    // } else if (mCallOutbtype.equals(moutcallblock[0])) {
                        // res = !(inPIM(mnetnumber));
                    // } else if (mCallOutbtype.equals(moutcallblock[2])) {
                        // res = !(inNameList(mnetnumber, mCallOutbtype));
                    // } else if (mCallOutbtype.equals(moutcallblock[3])) {
                        // res = inNameList(mnetnumber, mCallOutbtype);
                    // } else {
                        // res = false;
                    // }
                // } else {
                    // res = false;
                // }
                res = false;
            }

            if (res)
                setFirewallCallNotification(phonenumber);

            if (res && updatelog && mCallLog) {
                ContentValues values = new ContentValues();

                // Write our text back into the provider.
                if (null == phonenumber) {
		    values.put(BlockLog.PHONE_NUMBER, CallerInfo.UNKNOWN_NUMBER);
		} else if (phonenumber.equals(CallerInfo.UNKNOWN_NUMBER) ||  phonenumber.equals(CallerInfo.PRIVATE_NUMBER)) {
                    values.put(BlockLog.PHONE_NUMBER, phonenumber);
                } else if (TextUtils.isEmpty(phonenumber)){
                    values.put(BlockLog.PHONE_NUMBER, CallerInfo.UNKNOWN_NUMBER);
                } else {
                    values.put(BlockLog.PHONE_NUMBER, PhoneNumberUtils.formatNumber(phonenumber));
                }
                values.put(BlockLog.CACHED_NAME, mcachename);
                values.put(BlockLog.LOG_TYPE, BlockLog.CALL_BLOCK);
                values.put(BlockLog.LOG_DATA, String.valueOf(calltype));
                if (network == TelephonyManager.PHONE_TYPE_CDMA || network == TelephonyManager.PHONE_TYPE_GSM || (network >= 0 && network <= 4)) {
                    values.put(BlockLog.NETWORK, network);
                } else {
                    values.put(BlockLog.NETWORK, TelephonyManager.PHONE_TYPE_NONE);
                }

                // Commit all of our changes to persistent storage. When the
                // update completes
                // the content provider will notify the cursor of the change,
                // which will
                // cause the UI to be updated.
                FireWallService.this.getContentResolver().insert(BlockLog.CONTENT_URI, values);
                mPre.edit().putInt("log_type", 0).commit();
                // remove expired entries
                FireWallService.this.getContentResolver().delete(
                        BlockLog.CONTENT_URI,
                        "_id IN " + "(SELECT _id FROM loglist ORDER BY " + BlockLog.DEFAULT_SORT_ORDER
                                + " LIMIT -1 OFFSET 500)", null);
                if (mLogListActivity != null) {
                    mLogListActivity.updateLogList();
                }
            }
            int mret = 0;
            if (res) {
                if (calltype == Calls.INCOMING_TYPE) {
                    if (mBlock.equals("ignore_block")) {
                        mret = 2;
                    } else {
                        mret = 3;
                    }
                } else {
                    mret = 1;
                }
            }
            return mret;
        }

    };

    private SmsFirewallCallback mSmsback = new SmsFirewallCallback.Stub() {
        /**
         * This is called by the remote service regularly to tell us about new
         * values. Note that IPC calls are dispatched through a thread pool
         * running in each process, so the code executing here will NOT be
         * running in our main thread like most other things -- so, to update
         * the UI, we need to use a Handler to hop over there.
         */
        public boolean CheckSmsBlock(String phonenumber, String smscontent, boolean updatelog, int network) {
            boolean res = false;
            boolean ret = false;
            Log.d(TAG,"call CheckSmsBlock phonenumber=" + phonenumber + " smscontent=" + smscontent
                      + " updatelog=" + updatelog + " network=" + network);
            String mnetnumber = phonenumber;
            if ((! phonenumber.equals(CallerInfo.UNKNOWN_NUMBER)) && ( ! phonenumber.equals(CallerInfo.PRIVATE_NUMBER))) {
                mnetnumber = PhoneNumberUtils.extractNetworkPortion(phonenumber);
            }
            mcachename = "";

            if (PhoneNumberUtils.isEmergencyNumber(mnetnumber)) {
                return false;
            }

            if (mSmsInCdma) {
                if (network == TelephonyManager.PHONE_TYPE_CDMA) {
                    res = true;
                }
            }
            if (!res && mSmsInGsm) {
                if (network == TelephonyManager.PHONE_TYPE_GSM) {
                    res = true;
                }
            }
            if (!res && mSmsIn) {
                if (mSmsInbtype.equals(minsmsblock[1])) {
                    /* in_sms_rejectall */
                    res = true;
                } else if (mSmsInbtype.equals(minsmsblock[0])) {
                    /* in_sms_pimlist */
                    res = !(inPIM(mnetnumber));
                } else if (mSmsInbtype.equals(minsmsblock[2])) {
                    /* in_sms_whitelist */
                    res = !(inNameList(mnetnumber, mSmsInbtype));
                } else if (mSmsInbtype.equals(minsmsblock[3])) {
                    /* in_sms_blacklist */
                    res = inNameList(mnetnumber, mSmsInbtype);
                } else {
                    res = false;
                }
            }

//            try {
//                if (mSmsKeyword && mKeywordService != null) {
//                    ret = mKeywordService.checkSMSKeyWordsBlock(smscontent);
//                }
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }

            res|=ret;

            if (res)
                setFirewallSMSNotification(phonenumber);

            if (res && updatelog && mSmsLog) {
                ContentValues values = new ContentValues();

                // Write our text back into the provider.
                if (phonenumber.equals(CallerInfo.UNKNOWN_NUMBER) ||  phonenumber.equals(CallerInfo.PRIVATE_NUMBER)) {
                    values.put(BlockLog.PHONE_NUMBER, phonenumber);
                } else if (TextUtils.isEmpty(phonenumber)){
                    values.put(BlockLog.PHONE_NUMBER, CallerInfo.UNKNOWN_NUMBER);
                } else {
                    values.put(BlockLog.PHONE_NUMBER, PhoneNumberUtils.formatNumber(phonenumber));
                }
                values.put(BlockLog.CACHED_NAME, mcachename);
                values.put(BlockLog.LOG_TYPE, BlockLog.SMS_BLOCK);
                values.put(BlockLog.LOG_DATA, smscontent);
                if (network == TelephonyManager.PHONE_TYPE_CDMA || network == TelephonyManager.PHONE_TYPE_GSM || (network >= 0 && network <= 4)) {
                    values.put(BlockLog.NETWORK, network);
                } else {
                    values.put(BlockLog.NETWORK, TelephonyManager.PHONE_TYPE_NONE);
                }

                // Commit all of our changes to persistent storage. When the
                // update completes
                // the content provider will notify the cursor of the change,
                // which will
                // cause the UI to be updated.
                FireWallService.this.getContentResolver().insert(BlockLog.CONTENT_URI, values);
                mPre.edit().putInt("log_type", 0).commit();
                // remove expired entries
                FireWallService.this.getContentResolver().delete(
                        BlockLog.CONTENT_URI,
                        "_id IN " + "(SELECT _id FROM loglist ORDER BY " + BlockLog.DEFAULT_SORT_ORDER
                                + " LIMIT -1 OFFSET 500)", null);
            }
            return res;
        }
    };

    private boolean inPIM(String phonenumber) {
        if (TextUtils.isEmpty(phonenumber) || phonenumber.equals(CallerInfo.UNKNOWN_NUMBER) || phonenumber.equals(CallerInfo.PRIVATE_NUMBER)) {
            return false;
        }
        Cursor phonesCursor = FireWallService.this.getContentResolver().query(
                Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phonenumber)),
                PHONES_PROJECTION, null, null, null);
        if (phonesCursor != null) {
            if (phonesCursor.moveToFirst()) {
                phonesCursor.close();
                return true;
            } else {
                phonesCursor.close();
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean inNameList(String phonenumber, String listtype) {
        if (TextUtils.isEmpty(phonenumber) || phonenumber.equals(CallerInfo.UNKNOWN_NUMBER) || phonenumber.equals(CallerInfo.PRIVATE_NUMBER)) {
            return false;
        }
        if (inPatten(phonenumber, listtype)) {
            return true;
        }
//        if (inContactName(phonenumber, listtype)) {
//            return true;
//        }
        String mwblist = null;
        String mflag = null;
        // if (listtype.equals(mincallblock[2]) || listtype.equals(moutcallblock[2])) {
        if (listtype.equals(mincallblock[2])) {
            mwblist = mlist[0];
            mflag = " and " + Name.FOR_CALL;
        // } else if (listtype.equals(mincallblock[3]) || listtype.equals(moutcallblock[3])) {
        } else if (listtype.equals(mincallblock[3])) {
            mwblist = mlist[2];
            mflag = " and " + Name.FOR_CALL;
        } else if (listtype.equals(minsmsblock[2])) {
            mwblist = mlist[0];
            mflag = " and " + Name.FOR_SMS;
        } else if (listtype.equals(minsmsblock[3])) {
            mwblist = mlist[2];
            mflag = " and " + Name.FOR_SMS;
        }
        String mobilenumber = null;
        if (phonenumber.length() >= 14) {
            mobilenumber = phonenumber.replaceFirst("^\\+86","");
        } else if (phonenumber.length() == 11) {
            mobilenumber = "+86" + phonenumber;
        }
        String selecting_string = null;
        if (TextUtils.isEmpty(mobilenumber)) {
            selecting_string = Name.BLOCK_TYPE + " = '" + mwblist + "' and "
                    + Name.PHONE_NUMBER_KEY + " = '" + phonenumber + "'" + mflag;
        } else {
            selecting_string = Name.BLOCK_TYPE + " = '" + mwblist + "' and ("
            + Name.PHONE_NUMBER_KEY + " = '" + phonenumber + "' or "
            + Name.PHONE_NUMBER_KEY + " = '" + mobilenumber + "')" + mflag;
        }
        Cursor phonesCursor = FireWallService.this.getContentResolver().query(Name.CONTENT_URI,
                NAMEPROJECTION, selecting_string, null, null);
        if (phonesCursor != null) {
            if (phonesCursor.moveToFirst()) {
                mcachename = phonesCursor.getString(COLUMN_INDEX_NAME);
                phonesCursor.close();
                return true;
            } else {
                phonesCursor.close();
                return false;
            }
        } else {
            return false;
        }
    }

//    private boolean inContactName(String phonenumber, String listtype) {
//        if (TextUtils.isEmpty(phonenumber) || phonenumber.equals(CallerInfo.UNKNOWN_NUMBER) || phonenumber.equals(CallerInfo.PRIVATE_NUMBER)) {
//            return false;
//        }
//        String mwblist = null;
//        if (listtype.equals(mincallblock[2]) || listtype.equals(moutcallblock[2]) ||
//                listtype.equals(minsmsblock[2])) {
//            mwblist = mlist[4];
//        } else if (listtype.equals(mincallblock[3]) || listtype.equals(moutcallblock[3]) ||
//                listtype.equals(minsmsblock[3])) {
//            mwblist = mlist[5];
//        }
//        Cursor phonesCursor = FireWallService.this.getContentResolver().query(
//                Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phonenumber)),
//                PHONES_PROJECTION, null, null, null);
//        if (phonesCursor != null) {
//            while (phonesCursor.moveToNext()) {
//                String mlookup = phonesCursor.getString(COLUMN_INDEX_PHONE_LOOKUP);
//                String selecting_string = Name.BLOCK_TYPE + " = '" + mwblist + "' and "
//                    + Name.PHONE_NUMBER_KEY + " = '" + mlookup + "'";
//                Cursor nameCursor = FireWallService.this.getContentResolver().query(Name.CONTENT_URI,
//                    NAMEPROJECTION, selecting_string, null, null);
//                if (nameCursor != null) {
//                    if (nameCursor.moveToFirst()) {
//                        nameCursor.close();
//                        return true;
//                    } else {
//                        nameCursor.close();
//                        break;
//                    }
//                } else {
//                    break;
//                }
//            }
//            phonesCursor.close();
//            return false;
//        } else {
//            return false;
//        }
//    }

    private boolean inPatten(String phonenumber, String listtype) {
        if (TextUtils.isEmpty(phonenumber) || phonenumber.equals(CallerInfo.UNKNOWN_NUMBER) || phonenumber.equals(CallerInfo.PRIVATE_NUMBER)) {
            return false;
        }
        Pattern p;
        Pattern s;
        boolean res = false;
        boolean isSms = false;
        String selecting_string = null;
        String mPatten;
        String sPatten;
        // if (listtype.equals(mincallblock[2]) || listtype.equals(moutcallblock[2])) {
        if (listtype.equals(mincallblock[2])) {
            selecting_string = Name.BLOCK_TYPE + " = " + "'" + mlist[1] + "'" + " and " + Name.FOR_CALL;
        } else if (listtype.equals(minsmsblock[2])) {
            selecting_string = Name.BLOCK_TYPE + " = " + "'" + mlist[1] + "'" + " and " + Name.FOR_SMS;
            isSms =  true;
        // } else if (listtype.equals(mincallblock[3]) || listtype.equals(moutcallblock[3])) {
        } else if (listtype.equals(mincallblock[3])) {
            selecting_string = Name.BLOCK_TYPE + " = " + "'" + mlist[3] + "'" + " and " + Name.FOR_CALL;
        } else if (listtype.equals(minsmsblock[3])) {
            selecting_string = Name.BLOCK_TYPE + " = " + "'" + mlist[3] + "'" + " and " + Name.FOR_SMS;
            isSms = true;
        }
        Cursor phonesCursor = FireWallService.this.getContentResolver().query(Name.CONTENT_URI,
                NAMEPROJECTION, selecting_string, null, null);
        if (phonesCursor != null) {
            if (phonesCursor.moveToFirst()) {
                int i = 0;
                do {
                    mPatten = phonesCursor.getString(COLUMN_INDEX_NUMBER);
                    sPatten = mPatten;
                    int len = mPatten.length();
                    if (mPatten != null && mPatten.length() > 0) {
                        if (mPatten.charAt(0) == '+') {
                            mPatten = "\\" + mPatten;
                        } else if (isSms) {
                            sPatten = "\\+86" + mPatten;
                        }
                        if(mPatten.charAt(len - 1) == '#'){
                            mPatten = mPatten.substring(0,len - 1);
                            p = Pattern.compile(mPatten + "$");
                        }else{
                            p = Pattern.compile("^" + mPatten);
                        }
                        s = Pattern.compile("^" + sPatten);
                        if (p.matcher(phonenumber).find() || s.matcher(phonenumber).find() ) {
                            res = true;
                            break;
                        }
                    }
                    i++;
                } while (phonesCursor.moveToNext() && i < Name.MAXBLACKPATTERNS);
            }
            phonesCursor.close();
        }

        return res;
    }

    private ServiceConnection mCallServiceConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mCallService = PhoneHub.Stub.asInterface(service);

            // We want to monitor the service for as long as we are
            // connected to it.
            if (mCallService != null) {
                try {
                    mCallService.registerCallback(mCallback);
                } catch (RemoteException e) {
                    // In this case the service has crashed before we could even
                    // do anything with it; we can count on soon being
                    // disconnected (and then reconnected if it can be restarted)
                    // so there is no need to do anything here.
                }
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mCallService = null;

        }
    };

    private ServiceConnection mSmsServiceConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mSmsService = SmsHub.Stub.asInterface(service);

            // We want to monitor the service for as long as we are
            // connected to it.
            if (mSmsService != null) {
                try {
                    mSmsService.registerCallback(mSmsback);
                } catch (RemoteException e) {
                    // In this case the service has crashed before we could even
                    // do anything with it; we can count on soon being
                    // disconnected (and then reconnected if it can be restarted)
                    // so there is no need to do anything here.
                }
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mSmsService = null;

        }
    };
    
//    private ServiceConnection mSmsKeywordServiceConn = new ServiceConnection() {
//        public void onServiceConnected(ComponentName className, IBinder service) {
//            mKeywordService = InterfaceService.Stub.asInterface(service);
//        }
//
//        public void onServiceDisconnected(ComponentName className) {
//            mKeywordService = null;
//        }
//    };

    private void configurationChanged() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName componentName = new ComponentName(this, FirewallAppWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.setComponent(componentName);
        Bundle extras = new Bundle();
        extras.putIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        intent.putExtras(extras);

        sendBroadcast(intent);
    }
}
