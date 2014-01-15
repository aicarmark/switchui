package com.motorola.firewall;

import com.motorola.firewall.FireWall.BlockLog;
import com.motorola.firewall.FireWall.Settings;
import com.motorola.firewall.SyncNumberAdapter.CallerInfoQuery;
import com.motorola.firewall.SyncNumberAdapter.ContactInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ListActivity;
import android.app.ListFragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.CursorLoader; // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Settings.System;
import android.provider.Telephony.Sms.Inbox;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import android.util.Log;

import com.android.internal.telephony.CallerInfo;
// import com.motorola.android.telephony.PhoneModeManager; // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS

import java.util.HashMap;
import java.util.LinkedList;
import java.lang.ref.WeakReference;
import android.os.SystemProperties;

/*2010-09-10, DJHV83 added for SWITCHUITWOV-97*/
import android.provider.Telephony.Sms;
import android.database.sqlite.SQLiteException;
/*SWITCHUITWOV-97 End*/

public class FirewallLogListActivity extends ListFragment {

    public static final int MENU_DELETE_ALL = Menu.FIRST;
    public static final int MENU_ITEM_DELETE = Menu.FIRST + 1;
    public static final int MENU_ITEM_RESTORE = Menu.FIRST + 2;

    private static final String TAG = "FirewallLogList";
    private boolean misDualmode = false;
    private boolean misWcdmaAddGsm = false;
    private Activity mParentActivity;
    private View logListLayout;

    static final String[] LOG_PROJECTION = new String[] {
        BlockLog._ID, // 0
        BlockLog.PHONE_NUMBER, // 1
        BlockLog.CACHED_NAME, // 2
        BlockLog.BLOCKED_DATE, // 3
        BlockLog.CACHED_NUMBER_TYPE, // 4
        BlockLog.CACHED_NUMBER_LABEL, // 5
        BlockLog.NETWORK, //6
        BlockLog.LOG_TYPE, //7
        BlockLog.LOG_DATA //8
    };

    private static final int COLUMN_INDEX_NUMBER = 1;
    private static final int COLUMN_INDEX_NAME = 2;
    private static final int COLUMN_INDEX_DATE = 3;
    private static final int COLUMN_INDEX_NUMBER_TYPE = 4;
    private static final int COLUMN_INDEX_NUMBER_LABEL = 5;
    private static final int COLUMN_INDEX_NETWORK = 6;
    private static final int COLUMN_INDEX_LOG_TYPE = 7;
    private static final int COLUMN_INDEX_LOG_DATA = 8;


    private static final int DELETE_ALL_ALERT = 1;

    static final String[] PHONES_PROJECTION = new String[] {
        PhoneLookup._ID,
        PhoneLookup.DISPLAY_NAME,
        PhoneLookup.TYPE,
        PhoneLookup.LABEL,
        PhoneLookup.NUMBER
    };

    static final int PERSON_ID_COLUMN_INDEX = 0;
    static final int NAME_COLUMN_INDEX = 1;
    static final int PHONE_TYPE_COLUMN_INDEX = 2;
    static final int LABEL_COLUMN_INDEX = 3;
    static final int MATCHED_NUMBER_COLUMN_INDEX = 4;

    static final int ALL_LOG_TYPE = 0;
    static final int CALL_LOG_TYPE = 1;
    static final int SMS_LOG_TYPE = 2;
    private static final int QUERY_TOKEN = 53;


    static final String PREF_LOG_TYPE = "log_type";
    static final String PREF_CALL_ON = "inblock_call_on";
    static final String PREF_SMS_ON = "inblock_sms_on";
    static final String PREF_KEYWORD_ON = "keywords_filter_on";
    static final String PREF_CALL_ON_CDMA = "in_call_reject_cdma";
    static final String PREF_SMS_ON_CDMA = "in_sms_reject_cdma";
    static final String PREF_CALL_ON_GSM = "in_call_reject_gsm";
    static final String PREF_SMS_ON_GSM = "in_sms_reject_gsm";

    private NotificationManager mNM;

    //Intent
    private static final String NUMBER_LOCATION_SERVICE_ID = "com.motorola.numberlocation.INumberLocationService";
    //Flag
    private static final String NUM_ADDR_SERVICE_FLAG = "location_service_on";
    //Service
    private  com.motorola.numberlocation.INumberLocationService mNumberLocationService = null;


    LogAdapter mLogAdapter;

    private QueryHandler mQueryHandler;
    private int logType = ALL_LOG_TYPE;
    private SharedPreferences mPre;

    public FirewallLogListActivity(){
        super();
    }

    public FirewallLogListActivity(String log_type){
        super();
    }

    private SimpleCursorAdapter.ViewBinder mlogviewbinder = new SimpleCursorAdapter.ViewBinder() {

        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            String name = cursor.getString(COLUMN_INDEX_NAME);
            switch (columnIndex) {
                case COLUMN_INDEX_LOG_DATA:
                    int blockType = cursor.getInt(COLUMN_INDEX_LOG_TYPE);
                    if ((blockType == BlockLog.SMS_BLOCK) && (view instanceof TextView)) {
                        String msms = cursor.getString(columnIndex);
                        ((TextView) view).setText(msms);
                        view.setVisibility(View.VISIBLE);
                    } else {
                        view.setVisibility(View.GONE);
                    }
                    break;
                case COLUMN_INDEX_LOG_TYPE:
                    if (view instanceof ImageView) {
                        int mtype = cursor.getInt(columnIndex);
                        if (mtype == BlockLog.CALL_BLOCK) {
                            ((ImageView) view).setImageResource(R.drawable.ic_list_call_on);
                            ((ImageView) view).setVisibility(View.VISIBLE);
                        } else if (mtype == BlockLog.SMS_BLOCK) {
                            ((ImageView) view).setImageResource(R.drawable.ic_list_sms_on);
                            ((ImageView) view).setVisibility(View.VISIBLE);
                        } else {
                            ((ImageView) view).setVisibility(View.INVISIBLE);
                        }
                    }
                    break;
                case COLUMN_INDEX_NUMBER:
                    if (view instanceof TextView) {
                        String pnumber = cursor.getString(columnIndex);
                        if (view.getId() == R.id.log_line1) {
                            String bnumber = null;
                            if (pnumber.equals(CallerInfo.UNKNOWN_NUMBER)) {
                                bnumber = getString(R.string.unknown);
                            } else if (pnumber.equals(CallerInfo.PRIVATE_NUMBER)) {
                                bnumber = getString(R.string.private_num);
                            } else if (pnumber.equals(CallerInfo.PAYPHONE_NUMBER)) {
                                bnumber = getString(R.string.payphone);
                            } else {
                                bnumber = PhoneNumberUtils.formatNumber(pnumber);
                            }
                            ((TextView) view).setText(bnumber);
                        } else if (view.getId() == R.id.log_loc) {
                            if ( pnumber.equals(CallerInfo.UNKNOWN_NUMBER) || pnumber.equals(CallerInfo.PRIVATE_NUMBER)
                                    || pnumber.equals(CallerInfo.PAYPHONE_NUMBER)) {
                                view.setVisibility(View.INVISIBLE);
                            } else {
                                /*2012-0904, DJHV83 add for SWITCHUITWOV-94*/
                                //Judge the state of the location display setting in system settings.
                                //For Ironprime TD&CT, there are different way to get the system setting value.
                                if ( mNumberLocationService != null 
                                    && System.getInt(getActivity().getContentResolver(), "show_location_enable", 1) == 1
                                    && SystemProperties.getBoolean("persist.radio.number_location", true)) {
                                /*SWITCHUITWOV-94 end*/
                                    try {
                                        pnumber = PhoneNumberUtils.stripSeparators(pnumber);
                                        String phoneNumber = removeGcCode(pnumber);
                                        String address = mNumberLocationService.getLocationByNumber(phoneNumber);
                                        if (! TextUtils.isEmpty(address)) {
                                            ((TextView) view).setText(address);
                                            view.setVisibility(View.VISIBLE);
                                        } else {
                                            view.setVisibility(View.INVISIBLE);
                                        }
                                    } catch (RemoteException e) {
                                        view.setVisibility(View.INVISIBLE);
                                    }
                                } else {
                                    view.setVisibility(View.INVISIBLE);
                                }
                            }
                        }
                    }
                    break;
                case COLUMN_INDEX_NAME:
                    if (view instanceof TextView) {
                        if (TextUtils.isEmpty(name)) {
                            ((TextView) view).setVisibility(View.INVISIBLE);
                        } else {
                            ((TextView) view).setText(name);
                            ((TextView) view).setVisibility(View.VISIBLE);
                        }
                    }
                    break;
                case COLUMN_INDEX_DATE:
                    if (view instanceof TextView) {
                        long date = cursor.getLong(columnIndex);
                        // Set the date/time field by mixing relative and absolute
                        // times.
                        int flags = DateUtils.FORMAT_ABBREV_RELATIVE | DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_ABBREV_MONTH;
                        TextView dateView = (TextView) view;
                        dateView.setText(DateUtils.getRelativeDateTimeString(mParentActivity, date,
                            DateUtils.MINUTE_IN_MILLIS, DateUtils.DAY_IN_MILLIS, flags));
                    }
                    break;
                case COLUMN_INDEX_NUMBER_TYPE:
                    // fall through
                case COLUMN_INDEX_NUMBER_LABEL:
                    if (view instanceof TextView) {
                        if (TextUtils.isEmpty(name)) {
                            ((TextView) view).setVisibility(View.INVISIBLE);
                        } else {
                            int type = cursor.getInt(COLUMN_INDEX_NUMBER_TYPE);
                            CharSequence label = cursor.getString(COLUMN_INDEX_NUMBER_LABEL);
                            CharSequence mlabel = Phone.getTypeLabel(getResources(), type, label);
                            ((TextView) view).setText(mlabel);
                            ((TextView) view).setVisibility(View.VISIBLE);
                        }
                    }
                    break;
                case COLUMN_INDEX_NETWORK :
                    if (view instanceof ImageView) {
                        if ( misDualmode ) {
                            int mnet = cursor.getInt(COLUMN_INDEX_NETWORK);
                            if (mnet == TelephonyManager.PHONE_TYPE_CDMA) {
                                ((ImageView) view).setImageResource(R.drawable.ic_cdma_network);
                                ((ImageView) view).setVisibility(View.VISIBLE);
                            } else if (mnet == TelephonyManager.PHONE_TYPE_GSM) {
                                ((ImageView) view).setImageResource(R.drawable.ic_gsm_network);
                                ((ImageView) view).setVisibility(View.VISIBLE);
                            } else {
                                ((ImageView) view).setVisibility(View.INVISIBLE);
                            }
                        } else if ( misWcdmaAddGsm){
                            int mnet = cursor.getInt(COLUMN_INDEX_NETWORK);
                            switch (mnet) {
                                case 1:
                                    ((ImageView) view).setImageResource(R.drawable.ic_sim2_list);
                                    ((ImageView) view).setVisibility(View.VISIBLE);
                                    break;
                                case 2:
                                    ((ImageView) view).setImageResource(R.drawable.ic_sim1_list);
                                    ((ImageView) view).setVisibility(View.VISIBLE);
                                    break;
                                default:
                                    ((ImageView) view).setVisibility(View.INVISIBLE);
                                    break;
                            }
                        } else {
                            ((ImageView) view).setVisibility(View.INVISIBLE);
                        }
                    }
                    break;
            }
            return true;
        }
    };

    final class LogAdapter extends SyncNumberAdapter {
        public LogAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to);
        };

        protected void updateList(CallerInfoQuery ciq, ContactInfo ci) {
            // Check if they are different. If not, don't update.
            if (TextUtils.equals(ciq.name, ci.name) && TextUtils.equals(ciq.numberLabel, ci.label)
                    && ciq.numberType == ci.type) {
                return;
            }
            ContentValues values = new ContentValues();
            values.put(BlockLog.CACHED_NAME, ci.name);
            values.put(BlockLog.CACHED_NUMBER_TYPE, ci.type);
            values.put(BlockLog.CACHED_NUMBER_LABEL, ci.label);
            mParentActivity.getContentResolver().update(BlockLog.CONTENT_URI, values,
                    BlockLog.PHONE_NUMBER + "='" + ciq.number + "'", null);
        }

        @Override
        protected void onContentChanged (){
            super.onContentChanged();
            if (0 == getCount()) {
                checkAndSetNotification();
            }
        }
    }

    private final class QueryHandler extends AsyncQueryHandler {
        private final WeakReference<FirewallLogListActivity> mActivity;

        public QueryHandler(Fragment fragment) {
            super(fragment.getActivity().getContentResolver());
            mActivity = new WeakReference<FirewallLogListActivity>((FirewallLogListActivity) fragment);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            final FirewallLogListActivity activity = mActivity.get();
            if (activity != null && !activity.isDetached()) {
                final FirewallLogListActivity.LogAdapter smsAdapter = activity.mLogAdapter;
                smsAdapter.setLoading(false);
                smsAdapter.changeCursor(cursor);
                activity.setHasOptionsMenu(true);
            } else {
                cursor.close();
            }
        }
    }

    @Override
    public void onAttach (Activity activity){
        super.onAttach(activity);
        mParentActivity = activity;
        onHiddenChanged(isHidden());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPre = PreferenceManager.getDefaultSharedPreferences(mParentActivity);
        Intent intent = new Intent();
        if (intent.getData() == null) {
            intent.setData(BlockLog.CONTENT_URI);
        }

        //setContentView(R.layout.firewall_log);

        // Inform the list we provide context menus for items
        //getListView().setOnCreateContextMenuListener(this);

    // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
        // Perform a managed query. The Activity will handle closing and
        // requerying the cursor
        // when needed.
        /* Cursor logcursor = managedQuery(BlockLog.CONTENT_URI, LOG_PROJECTION, null, null,
                BlockLog.DEFAULT_SORT_ORDER);*/

    CursorLoader cursorLoader = new CursorLoader(mParentActivity, BlockLog.CONTENT_URI, LOG_PROJECTION, 
                                    null, null,BlockLog.DEFAULT_SORT_ORDER);
    Cursor logcursor = cursorLoader.loadInBackground();
    // MOT Calling Code End

        // Used to map notes entries from the database to views

        mLogAdapter = new LogAdapter(mParentActivity, R.layout.log_list_item, logcursor, new String[] {
                BlockLog.LOG_TYPE, BlockLog.LOG_DATA, BlockLog.PHONE_NUMBER, BlockLog.CACHED_NAME,
                BlockLog.BLOCKED_DATE, BlockLog.CACHED_NUMBER_TYPE, BlockLog.CACHED_NUMBER_LABEL,
                BlockLog.PHONE_NUMBER, BlockLog.NETWORK
        }, new int[] {
                R.id.log_type_icon, R.id.block_content, R.id.log_line1, R.id.log_line2, R.id.block_date,
                R.id.log_contacttype, R.id.log_contacttype, R.id.log_loc, R.id.log_network_type_icon
        });

        mLogAdapter.setViewBinder(mlogviewbinder);

        mQueryHandler = new QueryHandler(this);
        setListAdapter(mLogAdapter);

        mParentActivity.setTitle(R.string.smslog_title);

        setHasOptionsMenu(false);
        // misDualmode = PhoneModeManager.isDmds(); // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
        misDualmode = FireWallSetting.isDualMode();
        misWcdmaAddGsm = isWcdmaAddGsm();
    }

    public static boolean isWcdmaAddGsm() {
        String dual = SystemProperties.get("persist.dsds.enabled");
        Log.d(TAG,"get persist.dsds.enabled = " + dual);
        if (dual != null && dual.equals("true")){
            return true;
        } else {
            return false;
        }
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Inform the list we provide context menus for items
        getListView().setOnCreateContextMenuListener(this);

    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        logListLayout = inflater.inflate(R.layout.firewall_log, container, false);
        buildFilter();
        return logListLayout;
    }
    public void updateLogList() {
        // The adapter caches looked up numbers, clear it so they will get
        // looked up again.
        if (mLogAdapter != null) {
            mLogAdapter.clearCache();
            mLogAdapter.setLoading(true);
            mLogAdapter.mPreDrawListener = null; // Let it restart the thread after
        }

        startQuery();
    }

    @Override
    public void onResume() {
        super.onResume(); // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
    if (null != FireWallService.getInstance()) {
            FireWallService.getInstance().setFirewallLogListActivity(this);
    }

        checkLocationService();
        // The adapter caches looked up numbers, clear it so they will get
        // looked up again.
        if (mLogAdapter != null) {
            mLogAdapter.clearCache();
            mLogAdapter.setLoading(true);
            setHasOptionsMenu(false);
            mLogAdapter.mPreDrawListener = null; // Let it restart the thread after
        }

        logType = mPre.getInt(PREF_LOG_TYPE, 0);

        Spinner log_type = (Spinner) logListLayout.findViewById(R.id.select_log_type);
        log_type.setSelection(logType);
        startQuery();

        checkAndSetNotification();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (null != FireWallService.getInstance()) {
            FireWallService.getInstance().setFirewallLogListActivity(null);
        }

        // Kill the requests thread
        mLogAdapter.stopRequestProcessing();

        if (mNumberLocationService != null)
        {
            mParentActivity.getApplicationContext().unbindService(mNumberLocationConnection);
            mNumberLocationService = null;
        }
        checkAndSetNotification();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Kill the requests thread
        mLogAdapter.stopRequestProcessing();

        Cursor cursor = mLogAdapter.getCursor();
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        Log.d(TAG, "onHiddenChanged hidden =" + hidden);

        // Clear notifications only when window gains focus.  This activity won't
        // immediately receive focus if the keyguard screen is above it.
        if (!hidden) {
            checkAndSetNotification();
        }
    }

    public void checkAndSetNotification()
    {
        mNM = (NotificationManager) mParentActivity.getSystemService(mParentActivity.NOTIFICATION_SERVICE);
        ContentResolver mresolver = mParentActivity.getContentResolver();
        boolean in_call_on = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.INBLOCK_CALL_ON));
        boolean in_sms_on = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.INBLOCK_SMS_ON));
        boolean in_call_on_cdma = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_CALL_REJECT_CDMA));
        boolean in_sms_on_cdma = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_SMS_REJECT_CDMA));
        boolean in_call_on_gsm = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_CALL_REJECT_GSM));
        boolean in_sms_on_gsm = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_SMS_REJECT_GSM));
        // boolean keyword_on = mPre.getBoolean(PREF_KEYWORD_ON, false);
        // if (in_call_on || in_sms_on || keyword_on) {
        Log.d(TAG,"in_call_on=" + in_call_on + " in_sms_on=" + in_sms_on + " in_call_on_cdma=" + in_call_on_cdma + " in_sms_on_cdma=" + in_sms_on_cdma + " in_call_on_gsm=" + in_call_on_gsm + " in_sms_on_gsm=" + in_sms_on_gsm);

        if ((in_call_on || in_sms_on || in_call_on_cdma
                || in_sms_on_cdma || in_call_on_gsm || in_sms_on_gsm)) {
            CharSequence text;
            int picid;
            int id;

            /*2012-09-04, DJHV83 modified for SWITCHUITWOV-91*/
            try{
                if (FireWallSetting.isVipMode(mresolver)){
                    text = getText(R.string.firewall_vip_mode_started);
                    picid = R.drawable.vip_stat_bar_connected;
                    id = R.string.firewall_vip_mode_started;
                }
                else {
                    text = getText(R.string.firewall_service_started);
                    picid = R.drawable.firewall_stat_bar_connected;
                    id = R.string.firewall_service_started;
                }
                Notification notification = new Notification(picid, text, java.lang.System.currentTimeMillis());
                notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
                // The PendingIntent to launch our activity if the user selects this
                // notification
                PendingIntent contentIntent = PendingIntent.getActivity(mParentActivity, 0, new Intent(mParentActivity, FireWallTab.class), 0);

                // Set the info for the views that show in the notification panel.
                notification.setLatestEventInfo(mParentActivity, text , text, contentIntent);

                // Send the notification.
                // We use a string id because it is a unique number. We use it later to
                // cancel.
                mNM.notify(id, notification);
            }catch(IllegalStateException e){
                Log.e(TAG, "Error...", e);
                mNM.cancel(R.string.firewall_service_started);
            }
            /*SWITCHUITWOV-91 end*/
        } else {
            mNM.cancel(R.string.firewall_service_started);
        }
     }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // This is our one standard application action -- inserting a
        // new note into the list.
        menu.add(0, MENU_DELETE_ALL, 0, R.string.menu_delete_all).setShortcut('3', 'd').setIcon(
                android.R.drawable.ic_menu_delete); // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {
        if (getListAdapter().isEmpty()) {
            menu.findItem(MENU_DELETE_ALL).setEnabled(false);
        } else {
            menu.findItem(MENU_DELETE_ALL).setEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_DELETE_ALL:
                // Delete Call Log table
deleteAllAlertDialogFragment.show(this, logType);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Setup the menu header
        String mname = cursor.getString(COLUMN_INDEX_NAME);
        String pnumber = cursor.getString(COLUMN_INDEX_NUMBER);
        if (mname == null) {
            if (pnumber.equals(CallerInfo.UNKNOWN_NUMBER)) {
                menu.setHeaderTitle(getString(R.string.unknown));
            } else if (pnumber.equals(CallerInfo.PRIVATE_NUMBER)) {
                menu.setHeaderTitle(getString(R.string.private_num));
            } else if (pnumber.equals(CallerInfo.PAYPHONE_NUMBER)) {
                menu.setHeaderTitle(getString(R.string.payphone));
            } else {
                menu.setHeaderTitle(pnumber);
            }
        } else {
            menu.setHeaderTitle(pnumber + " " + mname);
        }

        // Add a menu item to delete the note
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.menu_delete);
        // Add a menu item to restore back to SMS
        int blockType = cursor.getInt(COLUMN_INDEX_LOG_TYPE);
        if (blockType == BlockLog.SMS_BLOCK) {
            menu.add(0, MENU_ITEM_RESTORE, 0, R.string.menu_restore);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        switch (item.getItemId()) {
            case MENU_ITEM_DELETE: {
                // Delete the note that the context menu is for

                Uri logUri = ContentUris.withAppendedId(BlockLog.CONTENT_URI, info.id);
                mParentActivity.getContentResolver().delete(logUri, null, null);
                return true;
            }
            case MENU_ITEM_RESTORE: {
                // Strore current message back to SMS
                Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
                if (cursor == null) {
                    // For some reason the requested item isn't available, do nothing
                    return false;
                }
                String pnumber = cursor.getString(COLUMN_INDEX_NUMBER);
                String msms = cursor.getString(COLUMN_INDEX_LOG_DATA);
                long date = cursor.getLong(COLUMN_INDEX_DATE);
                int network = cursor.getInt(COLUMN_INDEX_NETWORK);
                Uri msmsuri = Inbox.addMessage(mParentActivity.getContentResolver(), pnumber, msms, null, date, true);
                //IKCBSMMCPPRC-398 mode was not an standard column
                //ContentValues values = new ContentValues();
                //values.put("mode" ,network);
                //mParentActivity.getContentResolver().update(msmsuri, values, null, null);
                /*2010-09-10, DJHV83 added for SWITCHUITWOV-97*/
                try{
                    if(haveSubIdCol()){
                        ContentValues value = new ContentValues();
                        value.put("sub_id" , network);
                        mParentActivity.getContentResolver().update(msmsuri, value, null, null);
                    }
                }
                catch(SQLiteException e){
                    Log.e(TAG, "DB operation error...");
                    return false;
                }
                /*SWITCHUITWOV-97 End*/
                Uri logUri = ContentUris.withAppendedId(BlockLog.CONTENT_URI, info.id);
                mParentActivity.getContentResolver().delete(logUri, null, null);
                return true;
            }
        }
        return false;
    }

    /*2010-09-10, DJHV83 added for SWITCHUITWOV-97*/
    private boolean haveSubIdCol() throws SQLiteException {
        Cursor cursor = null;
        cursor = mParentActivity.getContentResolver().query(
        Sms.CONTENT_URI, null, null, null, null);
        if (cursor == null) {
            Log.e(TAG, "Get all ID error!");
            throw new SQLiteException();
        } else {
            if (cursor.getColumnIndex("sub_id") != -1) {
                cursor.close();
                cursor = null;
                return true;
            } else {
                cursor.close();
                cursor = null;
                return false;
            }
        }
    }
    /*SWITCHUITWOV-97 End*/

    private void startQuery() {

        // Cancel any pending queries
        mQueryHandler.cancelOperation(QUERY_TOKEN);
        String selection = null;
        if (logType == CALL_LOG_TYPE) {
            selection = BlockLog.LOG_TYPE + "=" + BlockLog.CALL_BLOCK;

        } else if (logType == SMS_LOG_TYPE) {
            selection = BlockLog.LOG_TYPE + "=" + BlockLog.SMS_BLOCK;
        }
        mQueryHandler.startQuery(QUERY_TOKEN, null, BlockLog.CONTENT_URI,
                LOG_PROJECTION, selection, null, BlockLog.DEFAULT_SORT_ORDER);
    }

    private void checkLocationService() {
        if (System.getInt(mParentActivity.getApplicationContext().getContentResolver(), NUM_ADDR_SERVICE_FLAG, 1) != 0)
        {
            if (mNumberLocationService == null)
            {
                mParentActivity.getApplicationContext().bindService(new Intent(NUMBER_LOCATION_SERVICE_ID),
                        mNumberLocationConnection,
                        Context.BIND_AUTO_CREATE);
            }
        }else{
            if (mNumberLocationService != null)
            {
                mParentActivity.getApplicationContext().unbindService(mNumberLocationConnection);
                mNumberLocationService = null;
            }
        }
    }

    public static String removeGcCode(String number) {
    if (null == number) {
        return null;
    }

    String removed = number;
    if (removed.startsWith("+86")) {
        if (!(removed.startsWith("0", 3))) {
            if (removed.startsWith("1", 3) && !(removed.startsWith("0", 4))) {
                removed = removed.substring(3);
            } else {
                removed = "0" + removed.substring(3);
            }
        }
    }
    if (removed.startsWith("0086")) {
        if (!(removed.startsWith("0", 4))) {
            if (removed.startsWith("1", 4) && !(removed.startsWith("0", 5))) {
                removed = removed.substring(4);
            } else {
                removed = "0" + removed.substring(4);
            }
        }
    }
    return removed;
    }

    //Connection
    private ServiceConnection mNumberLocationConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mNumberLocationService = com.motorola.numberlocation.INumberLocationService.Stub.asInterface(service);
            startQuery();
        }

        public void onServiceDisconnected(ComponentName className) {
            mNumberLocationService = null;
        }
    };

    public static class deleteAllAlertDialogFragment extends DialogFragment {
        static int firewall_logType;
        public static void show(FirewallLogListActivity fragment, int logtype) {
            deleteAllAlertDialogFragment dialog = new deleteAllAlertDialogFragment();
            dialog.setTargetFragment(fragment, 0);
            dialog.show(fragment.getFragmentManager(), "cancelEditor");
            firewall_logType = logtype;
        }


        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String confirm;

            if (firewall_logType == SMS_LOG_TYPE) {
                confirm = getResources().getString(R.string.alert_dialog_smslog_delete_all);
            } else if (firewall_logType == CALL_LOG_TYPE) {
                confirm = getResources().getString(R.string.alert_dialog_calllog_delete_all);
            } else {
                confirm = getResources().getString(R.string.alert_dialog_alllog_delete_all);
            }

            AlertDialog mAlertDialog = new AlertDialog.Builder(getActivity()).setIcon(
                    android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.alert_dialog_alllog_delete_all).setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            /* User clicked OK so do some stuff */
                            String selection = null;
                            if (firewall_logType == CALL_LOG_TYPE) {
                                selection = BlockLog.LOG_TYPE + "=" + BlockLog.CALL_BLOCK;

                            } else if (firewall_logType == SMS_LOG_TYPE) {
                                selection = BlockLog.LOG_TYPE + "=" + BlockLog.SMS_BLOCK;
                            }
                            getActivity().getContentResolver().delete(BlockLog.CONTENT_URI, selection, null);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null).create();
            mAlertDialog.setMessage(confirm);
            return mAlertDialog;
        }
    }

    protected void buildFilter() {
        Spinner log_type = (Spinner) logListLayout.findViewById(R.id.select_log_type);
        log_type.setOnItemSelectedListener(
                new OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {

                        TextView empty_string = (TextView) logListLayout.findViewById(android.R.id.empty);
                        if (position == ALL_LOG_TYPE) {
                            logType = ALL_LOG_TYPE;
                            mPre.edit().putInt(PREF_LOG_TYPE, ALL_LOG_TYPE).commit();
                            empty_string.setText(R.string.all_log_empty);
                        } else if (position == CALL_LOG_TYPE) {
                            logType = CALL_LOG_TYPE;
                            mPre.edit().putInt(PREF_LOG_TYPE, CALL_LOG_TYPE).commit();
                            empty_string.setText(R.string.call_log_empty);
                        } else {
                            logType = SMS_LOG_TYPE;
                            mPre.edit().putInt(PREF_LOG_TYPE, SMS_LOG_TYPE).commit();
                            empty_string.setText(R.string.sms_log_empty);
                        }
                        // repopuldate list items
                        startQuery();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
            );
        log_type.setSelection(logType);
    }
}
