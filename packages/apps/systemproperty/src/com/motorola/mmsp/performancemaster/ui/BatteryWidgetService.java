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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.widget.RemoteViews;

import com.motorola.mmsp.performancemaster.engine.BatteryLeftTime;
import com.motorola.mmsp.performancemaster.engine.BatteryModeData;
import com.motorola.mmsp.performancemaster.engine.BatteryModeMgr;
import com.motorola.mmsp.performancemaster.engine.Log;
import com.motorola.mmsp.performancemaster.R;

/**
 * battery widget update service
 */
public class BatteryWidgetService extends Service {
    private static final String LOG_TAG = "BatteryWidget: ";
    
    public static final String ACTION_BOOT_START = "com.motorola.batterymanager.bws.BOOT_START";
    public static final String ACTION_SVC_START = "com.motorola.batterymanager.bws.SVC_START";
    public static final String ACTION_APPWIDGET_UPDATE = "com.motorola.batterymanager.bws.ACTION_APPWIDGET_UPDATE";
    public static final String ACTION_TICK_UPDATE = "com.motorola.batterymanager.bws.ACTION_TICK_UPDATE";
    public static final String ACTION_MODE_UPDATE = "com.motorola.batterymanager.bws.ACTION_MODE_UPDATE";
    public static final String ACTION_SCR_ON_UPDATE = "com.motorola.batterymanager.bws.ACTION_SCR_ON_UPDATE";
    public static final String ACTION_SCR_OFF_UPDATE = "com.motorola.batterymanager.bws.ACTION_SCR_OFF_UPDATE";
    public static final String EXTRA_WIDGET_IDS = "widget_ids";
    
    // notification
    //private static final int BATTERY_LOW_NOTIFICATION_ID = 1;

    // battery percent
    private static int[] mBattLevelIndex = new int[] {
            R.drawable.bm_level_red_0,
            R.drawable.bm_level_orange_1,
            R.drawable.bm_level_orange_2,
            R.drawable.bm_level_green_3,
            R.drawable.bm_level_green_4,
            R.drawable.bm_level_green_5,
            R.drawable.bm_level_green_6,
            R.drawable.bm_level_green_7,
            R.drawable.bm_level_green_8,
            R.drawable.bm_level_green_9,
            R.drawable.bm_level_green_10,
            R.drawable.bm_level_green_11,
    };
    
    /*
    private boolean mNotified = false;
    private static final int BATTERY_LOW_THRESHOLD = 30;
    private static final int BATTERY_NORMAL_THRESHOLD = 80;
    */

    private int mPercent = 0;
    private boolean mIsCharging = false;
    private boolean mScreenOff = false;
    private int[] mWidgetIds;

    private BattStateReceiver mBattReceiver;
    private BatteryModeMgr mBatteryModeMgr;
    private BatteryModeData mCurrMode;

    private BatteryLeftTime mModel;

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        BatteryWidgetService getService() {
            // Return this instance of LocalService so clients can call public
            // methods
            return BatteryWidgetService.this;
        }
    }

    /**
     * not used now
     * @return current battery remaining time
     */
    public long getRemainTime() {
        if (mModel == null) {
            return 0;
        }
        
        return mModel.getRemainTime();
    }
    
    /**
     * used by remote client
     * @return battery percentage
     */
    public int getBatteryPercent() {
        return mPercent;
    }
    
    /**
     * used by remote client
     * @return battery charing status
     */
    public boolean getBatteryCharing() {
        return mIsCharging;
    }
    
    /**
     *  used by remote client
     * @param data
     */
    public void calcLeftTime(BatteryModeData data) {
        if (mModel == null || mBatteryModeMgr == null) {
            Log.e(LOG_TAG, "calcLeftTime parm==null");
            return;
        }
        
        data.setRadioOn(mBatteryModeMgr.getRadioOn());
        mModel.setBatteryData(data);

        broadcastRemainTime();
    }

    /**
     *  used by remote client
     */
    public void recoverLeftTime() {
        if (mModel == null || mCurrMode == null || mBatteryModeMgr == null) {
            Log.e(LOG_TAG, "recoverLeftTime parm==null");
            return; 
        }
        
        mCurrMode.setRadioOn(mBatteryModeMgr.getRadioOn());
        mModel.setBatteryData(mCurrMode);
        
        broadcastRemainTime();
        
    }
    
    private long broadcastRemainTime() {
        if (mModel == null) {
            Log.e(LOG_TAG, "broadcastRemainTime model==null");
            return 0;
        }
        
        // send broadcast
        Intent intent = new Intent();
        intent.setAction(BattRemainingUpdate.ACTION_BATTERY_REMAINING_TIMES);
        long leftMins = mModel.getRemainTime();
        intent.putExtra(BattRemainingUpdate.EXTRA_KEY_REMAINING_TIMES, leftMins);
        sendBroadcast(intent);
        //Log.i(LOG_TAG, "broadcast ==>remain time=" + leftMins);
        
        return leftMins;
    }
    
    /*
    private void showBattLowNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // notification ticker, when
        Notification notification = new Notification(R.drawable.ic_batterymanager, 
                this.getString(R.string.bm_notify_ticker),
                System.currentTimeMillis());
        
        Intent notificationIntent = new Intent(this, BatteryModeSelectActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        
        notification.setLatestEventInfo(this, 
                this.getString(R.string.bm_notify_title), 
                this.getString(R.string.bm_notify_message), 
                contentIntent);
        
        mNotificationManager.notify(BATTERY_LOW_NOTIFICATION_ID, notification);
        
        mNotified = true;
    }
    
    private void cancelBattLowNotification() {
        if (mNotified) {
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(BATTERY_LOW_NOTIFICATION_ID);
            mNotified = false;
        }
    }
    */
    
    private void handleModeChange() {
        if (mBatteryModeMgr != null) {
            mCurrMode = mBatteryModeMgr.getCurrModeFromPrefs();
            
            /*
            // cancel battery low notification if it is in battery saver mode.
            if (mBatteryModeMgr.getPresetModeType(mCurrMode.getId()) == BatteryModeData.PRESET_MODE_SAVER) {
                cancelBattLowNotification();
            }
            */
        }
        
        updateRemoteView(this.getApplicationContext(), true);
    }
    
    private void handleTick() {
        if (mBatteryModeMgr != null) {
            mCurrMode = mBatteryModeMgr.getCurrModeFromPrefs();
        }
        
        /*
        if (mPercent < BATTERY_LOW_THRESHOLD && !mIsCharging) {
            if (mBatteryModeMgr != null 
                    && mBatteryModeMgr.getPresetModeType(mCurrMode.getId()) != BatteryModeData.PRESET_MODE_SAVER) {
                showBattLowNotification();
            }
        }
        */
        
        updateRemoteView(this.getApplicationContext(), true);
    }
    
    private void handleScreenOn() {
        mScreenOff = false;
        
        updateRemoteView(this.getApplicationContext(), true);
    }
    
    private void handleScreenOff() {
        mScreenOff = true;
    }

    private class BattStateReceiver extends BroadcastReceiver {
        private Context mContext;

        public BattStateReceiver(Context ctx) {
            mContext = ctx;

            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(Intent.ACTION_TIME_TICK);
            //filter.addAction(Intent.ACTION_SCREEN_ON);
            //filter.addAction(Intent.ACTION_SCREEN_OFF);
            //filter.addAction(BatteryModeMgr.ACTION_BATTERY_MODE_CHANGED);

            // sticky intent, update battery level, remaining time immediately
            Intent initIntent = mContext.registerReceiver(this, filter);

            onReceive(mContext, initIntent);
        }

        public void stop() {
            mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Log.e(LOG_TAG, "onReceive, intent==null");
                return;
            }
            String action = intent.getAction();
            if (action == null) {
                Log.e(LOG_TAG, "onReceive, action==null");
                return;
            }
            
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                int percent = (int) ((level / (float) scale) * 100);
                boolean bIsCharging = (plugged != 0);
                boolean bShouldUpdate = true;

                Log.e(LOG_TAG, "Batt OnReceive: percent=" + percent + 
                		 ", level=" + level +
                		 ", scale=" + scale +
                		 ", bIsCharge=" + bIsCharging);             

                if (percent != mPercent || bIsCharging != mIsCharging) {
                    mPercent = percent;
                    mIsCharging = bIsCharging;
                    bShouldUpdate = true;
                }
                
                /*
                if (percent < BATTERY_LOW_THRESHOLD && !bIsCharging) {
                    if (mBatteryModeMgr != null 
                            && mBatteryModeMgr.getPresetModeType(mCurrMode.getId()) != BatteryModeData.PRESET_MODE_SAVER) {
                        showBattLowNotification();
                    }
                }
                
                if (percent > BATTERY_NORMAL_THRESHOLD) {
                    cancelBattLowNotification();
                }
                */

                if (mModel != null) {
                    mModel.setBattInfo(percent, plugged);
                }

                if (bShouldUpdate) {
                    updateRemoteView(context, false);
                }
            } else if (Intent.ACTION_TIME_TICK.equals(action)) {
                //Log.i(LOG_TAG, "onReceive: TIME_TICK");
                
                handleTick();
            } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                Log.i(LOG_TAG, "onReceive SCREEN_ON");
                
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                Log.i(LOG_TAG, "onReceive SCREEN_OFF");
                
            } else if (BatteryModeMgr.ACTION_BATTERY_MODE_CHANGED.equals(action)) {
                Log.i(LOG_TAG, "onReceive MODE_CHANGED");                            
            }
        }
    }

    @Override
    public void onCreate() {
        Log.e(LOG_TAG, "onCreate");

        BatteryModeMgr.setContext(getApplicationContext());
        mBatteryModeMgr = BatteryModeMgr.getInstance();
        if (mBatteryModeMgr != null) {
            mBatteryModeMgr.init();
            
            mCurrMode = mBatteryModeMgr.getCurrModeFromPrefs();
        }

        mModel = new BatteryLeftTime(this);
        mModel.setBatteryData(mCurrMode);

        mBattReceiver = new BattStateReceiver(this);
    }

    @Override
    public void onDestroy() {
        Log.e(LOG_TAG, "onDestroy");
        if (mBattReceiver != null) {
            mBattReceiver.stop();
        }

        if (mBatteryModeMgr != null) {
            mBatteryModeMgr.deinit();
        }
        
        // alarm for restart service
        Intent operator = new Intent(ACTION_SVC_START);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, operator, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.RTC, System.currentTimeMillis() + 60 * 1000, pendingIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = null;
        if (intent != null && (action = intent.getAction()) != null) {
            if (action.equals(BatteryWidgetService.ACTION_SVC_START)) {
                Log.e(LOG_TAG, "BatteryWidgetService start SVC_START [widget|select]");
                
                if (mBatteryModeMgr != null) {
                    mCurrMode = mBatteryModeMgr.getCurrModeFromPrefs();
                }
                
                updateRemoteView(this, true);
                
            } else if (action.equals(BatteryWidgetService.ACTION_BOOT_START)) {
              Log.e(LOG_TAG, "BatteryWidgetService start [boot_up]");
              
              if (mBatteryModeMgr != null) {
                  mCurrMode = mBatteryModeMgr.checkCurrModeOnStart();
              }
            } else if (action.equals(BatteryWidgetService.ACTION_MODE_UPDATE)) {
                Log.e(LOG_TAG, "BatteryWidgetService start ACTION_MODE_UPDATE");
                
                handleModeChange();
            } else if (action.equals(BatteryWidgetService.ACTION_TICK_UPDATE)) {
                Log.e(LOG_TAG, "BatteryWidgetService start ACTION_TICK_UPDATE");
                
                handleTick();
            } else if (action.equals(BatteryWidgetService.ACTION_APPWIDGET_UPDATE)) {
                Log.e(LOG_TAG, "BatteryWidgetService start ACTION_APPWIDGET_UPDATE");
                
                mScreenOff = false;
                if (mBatteryModeMgr != null) {
                    mCurrMode = mBatteryModeMgr.getCurrModeFromPrefs();
                }
                
                mWidgetIds = intent.getIntArrayExtra(BatteryWidgetService.EXTRA_WIDGET_IDS);
                
                updateRemoteView(this, true);
            } else if (action.equals(BatteryWidgetService.ACTION_SCR_ON_UPDATE)) {
                Log.e(LOG_TAG, "BatteryWidgetService start ACTION_SCR_ON_UPDATE");
                
                handleScreenOn();
            } else if (action.equals(BatteryWidgetService.ACTION_SCR_OFF_UPDATE)) {
                Log.e(LOG_TAG, "BatteryWidgetService start ACTION_SCR_OFF_UPDATE");
                
                handleScreenOff();
            } else {
                Log.e(LOG_TAG, "BatteryWidgetService start unknown action");
            }
        } else {
            Log.e(LOG_TAG, "BatteryWidgetService start no intent, no action");
        }

        // if we get killed, after returning from here, restart.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.e(LOG_TAG, "onBind");
        
        return mBinder;
    }
    
    @Override
    public void onRebind(Intent intent) {
        Log.e(LOG_TAG, "onRebind");
        
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(LOG_TAG, "onUnbind");
        
        return super.onUnbind(intent);
    }

    private void updateRemoteView(Context context, boolean bSetModeData) {
        if (mBatteryModeMgr == null) {
            Log.e(LOG_TAG, "updateRemoteView modeMgr == null");
            return;
        }
        
        if (mCurrMode == null) {
            Log.e(LOG_TAG, "updateRemoteView currMode==null");
            return;
        }
        
        if (mWidgetIds == null) {
            // sometimes, service restart
            // mWidgetIds is null, so we treat it as normal
            Log.e(LOG_TAG, "updateRemoteView mWidgetIds==null");
        }

        if (mScreenOff) {
            Log.e(LOG_TAG, "updateRemoteView mScreenOff");
            return;
        }

        // remaining time
        long mins = 0;
        if (mModel != null) {
            if (bSetModeData && mBatteryModeMgr != null) {
                //Log.i(LOG_TAG, "updateRemoteViews leftTime setData");
                mCurrMode.setRadioOn(mBatteryModeMgr.getRadioOn());
                mModel.setBatteryData(mCurrMode);
            }
            
            mins = broadcastRemainTime();
        }
        
        // construct remote view
        RemoteViews views = getRemoteView(this);

        views.setTextViewText(R.id.battery_hours, String.valueOf((mins / 60)) +
                    context.getResources().getString(R.string.bm_hour));
        views.setTextViewText(R.id.battery_minutes, String.valueOf(mins % 60) +
                    context.getResources().getString(R.string.bm_minute));

        // battery mode name
        //views.setTextViewText(R.id.battery_mode,
        //        mCurrMode.getModeName());

        // battery percent
        mPercent = (mPercent > 100) ? (100) : ((mPercent<=1) ? 1 : mPercent);
        views.setImageViewResource(R.id.battery_level, mBattLevelIndex[(mPercent - 1) / 9]);

        // battery mode icon
        int resId = R.drawable.ic_batt_custom;
        if (mCurrMode.getPreset()) {
            int type = mBatteryModeMgr.getPresetModeType(mCurrMode.getId());
            switch (type) {
                case BatteryModeData.PRESET_MODE_GENERAL:
                    resId = R.drawable.ic_batt_general;
                    break;
                case BatteryModeData.PRESET_MODE_SAVER:
                    resId = R.drawable.ic_batt_saver;
                    break;
                case BatteryModeData.PRESET_MODE_NIGHT:
                    resId = R.drawable.ic_batt_super;
                    break;
                case BatteryModeData.PRESET_MODE_PERFORMANCE:
                    resId = R.drawable.ic_batt_perform;
                    break;
                default:
                    resId = R.drawable.ic_batt_general;
                    break;
            }
        }    
        // charging status icon
        if (mIsCharging) {
            resId = R.drawable.ic_batt_charge;
        }

        views.setImageViewResource(R.id.battery_icon, resId);

        // update to all widgets
        // update widget using component name because sometimes mWidgetIds == null
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        ComponentName myComponentName = new ComponentName(this,
                BatteryWidgetProvider.class);
        gm.updateAppWidget(myComponentName, views);
        //Log.i(LOG_TAG, "updateRemoteView called ");
    }

    public RemoteViews getRemoteView(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.bm_widget_single);

        Intent intent = new Intent(context, BatteryModeSelectActivity.class);
        intent.putExtra(BatteryModeSelectActivity.EXTRA_BATT_PERCENT, mPercent);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        views.setOnClickPendingIntent(R.id.battery_center, pendingIntent);

        return views;
    }
}
