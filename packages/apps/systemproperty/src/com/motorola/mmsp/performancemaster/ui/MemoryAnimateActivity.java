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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.motorola.mmsp.performancemaster.R;

public class MemoryAnimateActivity extends Activity {
    private static final String LOG_TAG = "MemoryAnimateActivity: ";

    // send by DashboardWidgetService, stop animate when clean memory finished
    public static final String ACTION_STOP_FLOATING_ANIMATION = "dashboard.animation.stop";

    private Rect mDestRect;

    private AnimationDrawable animCleaning;

    private StopReceiver mStopReceiver;

    private View mMemView;

    private ImageView mSecCircleImage;

    private class StopReceiver extends BroadcastReceiver {
        private Context mContext;

        public StopReceiver(Context context) {
            mContext = context;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_STOP_FLOATING_ANIMATION)) {
                finish();
            }
        }

        public void start() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_STOP_FLOATING_ANIMATION);
            mContext.registerReceiver(this, filter);
        }

        public void stop() {
            mContext.unregisterReceiver(this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set layout
        setContentView(R.layout.memory_widget_update);

        mStopReceiver = new StopReceiver(this);
        mStopReceiver.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mStopReceiver.stop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!hasFocus)
            return;

        getStatusBarHeight3(this);

        // animate activity is started by PendingIntent of RemoteView
        Intent intent = getIntent();
        mDestRect = intent.getSourceBounds();

        // find the root of R.layout.memory_widget_update
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.memory_update_layout);

        // dynamic load memory view, ImageViews and ImageViews and LinearLayout
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMemView = inflater.inflate(R.layout.dashboard_widget_memory, null);

        // WIDTH and HEIGHT
        float width = (float) mDestRect.width();
        float height = (float) mDestRect.height();
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) width,
                (int) height);

        // LEFT and TOP
        int statusHeight = getStatusBarHeight3(this);
        Log.i(LOG_TAG, "statusHeight=" + statusHeight);

        float left = (float) mDestRect.left;
        float top = (float) (mDestRect.top - statusHeight);

        params.leftMargin = (int) left;
        params.topMargin = (int) top;

        Log.i(LOG_TAG, "left=" + left + " top=" + top + " width=" + width + " height=" + height);

        if (rl != null) {
            rl.addView(mMemView, params);
        } else {

            Log.e(LOG_TAG, "rl is not found");
            return;
        }

        mSecCircleImage = (ImageView) rl.findViewById(R.id.sencond_circle);
        if (mSecCircleImage != null) {
            mSecCircleImage.setImageResource(R.drawable.memory_anim_go);
        } else {
            Log.e(LOG_TAG, "can not find image view");
        }

        animCleaning = (AnimationDrawable) mSecCircleImage.getDrawable();

        animCleaning.start();

        mHandler.sendEmptyMessageDelayed(0, 500);
        Log.i(LOG_TAG, "startService for clean rom/ram...");

    }

    // start DashboardWidgetService for clean RAM/ROM
    private void startMemoryService() {
        Intent i = new Intent(this, MemoryCleanerService.class);
        i.setAction(MemoryCleanerService.START_SERVICE_CLEAN);
        startService(i);
    }

    float getRatio(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();

        ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(
                metrics);

        float density = metrics.density;

        int dpi = metrics.densityDpi;
        int heightPx = metrics.heightPixels;
        int widthPx = metrics.widthPixels;

        Log.i(LOG_TAG, "dpi=" + dpi + " h=" + heightPx + " w=" + widthPx);
        Log.i(LOG_TAG, "density=" + density);

        density = (float) dpi / (float) DisplayMetrics.DENSITY_DEFAULT;
        Log.i(LOG_TAG, "ratio=" + density);

        return density;
    }

    int getStatusBarHeight3(Context context) {
        Rect rect = new Rect();
        Window win = this.getWindow();
        win.getDecorView().getWindowVisibleDisplayFrame(rect);
        int statusBarHeight = rect.top;

        Log.i(LOG_TAG, "statusbar height3=" + statusBarHeight);

        return statusBarHeight;
    }

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            startMemoryService();
            finish();
        }

    };
}
