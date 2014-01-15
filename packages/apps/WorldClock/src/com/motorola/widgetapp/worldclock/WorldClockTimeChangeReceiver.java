package com.motorola.widgetapp.worldclock;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.util.Log;


public class WorldClockTimeChangeReceiver {

    ArrayList<TimeChangedCallBack> mObserver;
    private static final String TAG = "WorldClockWidgetTCReceiver";

    private final BroadcastReceiver mSystemClockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            PowerManager powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            boolean isScreenOn = powerManager.isScreenOn();

            if ( (Intent.ACTION_SCREEN_ON.equals(action)
                    || isScreenOn && (Intent.ACTION_TIME_TICK.equals(action)
                    || Intent.ACTION_TIME_CHANGED.equals(action)
                    || Intent.ACTION_DATE_CHANGED.equals(action)
                    || Intent.ACTION_CONFIGURATION_CHANGED.equals(action)
                    || Intent.ACTION_TIMEZONE_CHANGED.equals(action)))) {
                Log.i(TAG,"onReceive, action=" + action);
                if (Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                    handleTimeChanged(context, true);
                } else {
                    handleTimeChanged(context, false);
                }
            }
        }
    };

    private static WorldClockTimeChangeReceiver mInstance;

    public static WorldClockTimeChangeReceiver getInstance() {
        if (mInstance == null) {
            mInstance = new WorldClockTimeChangeReceiver();
        }
        return mInstance;
    }

    public static void freeTimeChangeReceiver() {
        mInstance = null;
    }

    private WorldClockTimeChangeReceiver() {
        super();
        mObserver = new ArrayList<TimeChangedCallBack>();
        // TODO Auto-generated constructor stub
    }

    public void registerTimerBroadcasts(Context context) {
        // setup receiver for infoCallback
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        context.registerReceiver(mSystemClockReceiver, filter);
    }

    public void unregisterTimerBroadcasts(Context context) {
        context.unregisterReceiver(mSystemClockReceiver);
    }

    public void registerTimeChangedCallBack(TimeChangedCallBack observer) {
        mObserver.add(observer);
    }

    public void unregisterTimeChangedCallBack(TimeChangedCallBack observer) {
        mObserver.remove(observer);
    }

    private void handleTimeChanged(Context context, boolean tzChange) {
        int count = mObserver.size();
        for (int i = 0; i < count; i++) {
            mObserver.get(i).onTimeChanged(tzChange);
        }
    }

    interface TimeChangedCallBack {
        void onTimeChanged(boolean tzChange);
    }
}
