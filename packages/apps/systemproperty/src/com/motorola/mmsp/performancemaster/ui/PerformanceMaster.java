/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * pjw346                       05/10/2012                  Initial release 
 *         
 */
package com.motorola.mmsp.performancemaster.ui;

import com.motorola.mmsp.performancemaster.engine.CacheListInfo;
import com.motorola.mmsp.performancemaster.engine.Log;
import com.motorola.mmsp.performancemaster.engine.RunningStateInfo;
import com.motorola.mmsp.performancemaster.engine.SysPropEngine;
import com.motorola.mmsp.performancemaster.R;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;


@SuppressWarnings("deprecation")
public class PerformanceMaster extends TabActivity {
    private final static String TAG = "SystemProperty";

    public final static String ACTION_CPU = "action_cpu";

    public final static String ACTION_MEMORY = "action_memory";

    public final static String ACTION_BATTERY = "action_battery";

    private SysPropEngine engine = null;

    private CacheListInfo mCacheListInfo = null;

    private RunningStateInfo mRunningStateInfo = null;

    private MainTabHost mTabHost = null;

    private boolean mBSystemPropertyIsRunning = false;

    private static final String[] mStrTabName = {
            "CPUInfo", "MemoryInfo", "BatteryInfo"};

    private void initResources() {
        mTabHost = (MainTabHost) findViewById(android.R.id.tabhost);
    }

    private void initTab() {
        mTabHost.addTab(mTabHost.newTabSpec(mStrTabName[0].toString())
                .setIndicator(getResources().getText(R.string.CPUInfo))
                .setContent(new Intent(this, CpuInfoActivity.class)));

        mTabHost.addTab(mTabHost.newTabSpec(mStrTabName[1].toString())
                .setIndicator(getResources().getText(R.string.MemeryInfo))
                .setContent(new Intent(this, MemoryInfoActivity.class)));

        mTabHost.addTab(mTabHost.newTabSpec(mStrTabName[2].toString())
                .setIndicator(getResources().getText(R.string.BatteryInfo))
                .setContent(new Intent(this, BatteryModeActivity.class)));

    }

    private void initMemoryListener() {
        // add clean listener
        SysPropEngine.setContext(getApplicationContext());
        engine = SysPropEngine.getInstance();

        mRunningStateInfo = (RunningStateInfo) engine.getRunningStateInfo();

        mCacheListInfo = (CacheListInfo) engine.getCacheListInfo();

        mRunningStateInfo.startUpdate();
        mCacheListInfo.onCacheListUpdate();
    }

    private void initServices() {
        // battery remaining time update depend on DashboardSerivce
        Intent startD = new Intent(this, BatteryWidgetService.class);
        startService(startD);
    }

    private void switchTab(Intent intent) {
        String action = intent.getAction();

        Log.e(TAG, "switchTab() to: " + action);

        if (action != null) {
            if (action.equals(ACTION_CPU)) {
                mTabHost.setCurrentTab(0);
            } else if (action.equals(ACTION_BATTERY)) {
                mTabHost.setCurrentTab(2);
            } else if (action.equals(ACTION_MEMORY)) {
                mTabHost.setCurrentTab(1);
            } else if (action.equals(new String("android.intent.action.MAIN")))// should
                                                                               // discard
            {
                Log.e(TAG, "received intent====MAIN");
            }
        } else {
            Log.e(TAG, "switchTab() action is null");
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // create view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        initMemoryListener();
        
        // init resources
        initResources();
        
        initTab();

        initServices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        Log.e(TAG, " onResume(), running=" + mBSystemPropertyIsRunning);

        /*
         * we called switchTab() only once when user start our application,
         * because this function only received Intent.Action ==
         * android.intent.action.MAIN after launching SystemProperty.so, other
         * intent request should be processed in onNewIntent();
         */
        if (!mBSystemPropertyIsRunning) {
            switchTab(getIntent());
            mBSystemPropertyIsRunning = true;
        }

        super.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {

        Log.e(TAG, "onNewIntent(), intent action=" + intent.getAction());

        switchTab(intent);

        super.onNewIntent(intent);
    }
}
