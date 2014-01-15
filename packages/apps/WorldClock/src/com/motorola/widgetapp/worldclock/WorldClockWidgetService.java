package com.motorola.widgetapp.worldclock;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.motorola.widgetapp.worldclock.WorldClockDBAdapter.WidgetDataHolder;

public class WorldClockWidgetService extends Service implements
        WorldClockTimeChangeReceiver.TimeChangedCallBack {
    // BEGIN MOTOROLA - IKSTABLE6-8711: Fix FindBug issues in WorldClock app
    public static final String ACTION_WORLDCLOCK_REALTIMESERVICE = "com.motorola.widgetapp.worldclock.WorldClockWidgetService";
    public static final String ACTION_WORLDCLOCK_SHOWBUTTON = "com.motorola.widgetapp.worldclock.WorldClockWidgetService.ShowButton";
    public static final String ACTION_WORLDCLOCK_LEFTBUTTON = "com.motorola.widgetapp.worldclock.WorldClockWidgetService.LeftButton";
    public static final String ACTION_WORLDCLOCK_RIGHTUTTON = "com.motorola.widgetapp.worldclock.WorldClockWidgetService.RightButton";
    public static final int BUTTONACTIVESTATUSTIMEOUT = 5 * 1000;
    private Hashtable<Integer, Timer> mTimers_List = new Hashtable<Integer, Timer>();
    public static final int WORLDCLOCK_UPDATING_ID = 1;
    // END MOTOROLA - IKSTABLE6-8711

    private static boolean RIGHT = true;
    private static boolean LEFT = false;

    private WorldClockTimeChangeReceiver mReceiver;

    private BroadcastReceiver mWidgetActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("WorldClockWidgetService", action);
            Uri data = intent.getData();
            if (data != null) {
                int appWidgetId = Integer.valueOf(data.toString());
                Log
                        .d("WorldClockWidgetService", "appWidgetId = "
                                + appWidgetId);

                WorldClockDBAdapter dbHelper = new WorldClockDBAdapter(context);
                dbHelper.open();
                WidgetDataHolder widgetData = dbHelper
                        .queryWidgetData(appWidgetId);
                // For case that widget data are force clear by user but widget
                // is on screen.
                if (widgetData == null) {
                    dbHelper.deleteWidgetData(appWidgetId);
                    widgetData = WorldClockWidgetProvider.loadDefaultSettings(
                            appWidgetId, context, dbHelper);
                }
                widgetData=WorldClockWidgetProvider.hiddenCityCheck(widgetData);
                if (ACTION_WORLDCLOCK_SHOWBUTTON.equals(action)) {
                    startActiveButtonTimeOut(appWidgetId);
                }
                updateWidgetView(widgetData, appWidgetId);
                dbHelper.close();
            }
        }
    };

    private void registerActionFilter() {
        Log.d("WorldClockWidgetService", "registerActionFilter");
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(ACTION_WORLDCLOCK_SHOWBUTTON);
        commandFilter.addAction(ACTION_WORLDCLOCK_RIGHTUTTON);
        commandFilter.addAction(ACTION_WORLDCLOCK_LEFTBUTTON);
        registerReceiver(mWidgetActionReceiver, commandFilter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    public void onTimeChanged(boolean tzChange) {
        Log.d("WorldClockWidgetService", "Minute Tick, Send update Action");
        Intent updateIntent = new Intent(
                WorldClockWidgetProvider.ACTION_WORLDCLOCK_UPDATE_ALL);
        if (tzChange) {
            updateIntent = new Intent(WorldClockWidgetProvider.ACTION_WORLDCLOCK_TIMEZONE_CHANGE);
        }
        sendBroadcast(updateIntent);
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        Log.d("WorldClockWidgetService", "onCreate");
        Notification status = new Notification();
        // status.contentView = new RemoteViews(this.getPackageName(),
        // R.layout.widget_worldclock);
        status.flags |= Notification.FLAG_ONGOING_EVENT;
        // BEGIN IKQCOM-3464 - PT:IT:LED is flashing when screen is locked if World clock is added to idle screen
        //status.defaults |= Notification.DEFAULT_LIGHTS;
        // END IKQCOM-3464
        // status.defaults |= Notification.DEFAULT_VIBRATE;
        // status.icon = null;
        // status.contentIntent = PendingIntent.getActivity(this, 0,
        // new Intent("com.android.music.PLAYBACK_VIEWER"), 0);
        startForeground(WORLDCLOCK_UPDATING_ID, status);

        mReceiver = WorldClockTimeChangeReceiver.getInstance();
        mReceiver.registerTimerBroadcasts(this);
        mReceiver.registerTimeChangedCallBack(this);
        registerActionFilter();
        mTimers_List.clear();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        // TODO Auto-generated method stub
        super.onStart(intent, startId);
        Log.d("WorldClockWidgetService", "onStart");

        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        Uri data = intent.getData();
        if (data != null) {
            int appWidgetId = Integer.valueOf(data.toString());
            Log.d("WorldClockWidgetService", "appWidgetId = " + appWidgetId);

            WorldClockDBAdapter dbHelper = new WorldClockDBAdapter(this);
            dbHelper.open();
            WidgetDataHolder widgetData = dbHelper.queryWidgetData(appWidgetId);
            // For case that widget data are force clear by user but widget is
            // on screen.
            if (widgetData == null) {
                dbHelper.deleteWidgetData(appWidgetId);
                widgetData = WorldClockWidgetProvider.loadDefaultSettings(
                        appWidgetId, this, dbHelper);
            }
            widgetData=WorldClockWidgetProvider.hiddenCityCheck(widgetData);
            if (ACTION_WORLDCLOCK_SHOWBUTTON.equals(action)) {
                startActiveButtonTimeOut(appWidgetId);
            }

            updateWidgetView(widgetData, appWidgetId);
            dbHelper.close();
        }

    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        Log.e("WorldClockWidgetService", "Notice: Service are destroy");
        mReceiver.unregisterTimerBroadcasts(this);
        mReceiver.unregisterTimeChangedCallBack(this);
        WorldClockTimeChangeReceiver.freeTimeChangeReceiver();
        unregisterReceiver(mWidgetActionReceiver);
        cancelAllActiveButtonTimeOut();
        super.onDestroy();
    }

    private void updateWidgetView(WidgetDataHolder data, int appWidgetId) {
        Intent updateIntent = new Intent(
                WorldClockWidgetProvider.ACTION_WORLDCLOCK_UPDATE);

        updateIntent
                .putExtra(WorldClockWidgetProvider.APPWIDGETID, appWidgetId);
        sendBroadcast(updateIntent);
    }

    private void startActiveButtonTimeOut(int appWidgetId) {
        if (!mTimers_List.containsKey(appWidgetId)) {
            Timer timer = new Timer();
            mTimers_List.put(appWidgetId, timer);
        }

        mTimers_List.get(appWidgetId).schedule(
                new ActiveStatusTimeOutTask(appWidgetId, this),
                BUTTONACTIVESTATUSTIMEOUT);
    }

    private void cancelActiveButtonTimeOut(int appWidgetId) {
        if (mTimers_List.containsKey(appWidgetId)) {
            mTimers_List.get(appWidgetId).cancel();
            mTimers_List.remove(appWidgetId);
        }
    }

    private void cancelAllActiveButtonTimeOut() {
        /*
         * Iterator iter = mTimers_List.entrySet().iterator(); while
         * (iter.hasNext()) { Map.Entry entry = (Map.Entry) iter.next();
         * ((Timer)entry.getValue()).cancel(); }
         */
        mTimers_List.clear();
    }

    static class ActiveStatusTimeOutTask extends TimerTask {
        private int mAppWidgetId;
        private Context mContext;

        public ActiveStatusTimeOutTask(int appWidgetId, Context context) {
            super();
            mAppWidgetId = appWidgetId;
            mContext = context;
        }

        public void run() {
            Log.d("WorldClockWidgetService", "appWidgetId = " + mAppWidgetId);
            Intent updateIntent = new Intent(
                    WorldClockWidgetProvider.ACTION_WORLDCLOCK_UPDATE);
            updateIntent.putExtra(WorldClockWidgetProvider.APPWIDGETID,
                    mAppWidgetId);
            updateIntent.putExtra(WorldClockWidgetProvider.SHOWBUTTON,
                    WorldClockWidgetProvider.WIDGET_ALL_BUTTON_INACTIVE);
            mContext.sendBroadcast(updateIntent);
        }
    }

}
