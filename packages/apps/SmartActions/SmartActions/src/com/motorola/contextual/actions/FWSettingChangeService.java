/*
 * @(#)PowerManagerSettingChangeService.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * w30219       2012/06/04  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/**
 * This class handles the request to write to the Analytics Provider <code><pre>
 * CLASS:
 *     Extends IntentService.
 *
 * RESPONSIBILITIES:
 *     Writes to the Analytics Content Provider.
 *
 * COLLABORATORS:
 *     Actions
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

    public class FWSettingChangeService extends IntentService implements Constants {
        private static final String TAG =  TAG_PREFIX + FWSettingChangeService.class.getSimpleName();
        private static final String EXTRA_DISPLAY_CURVE_SUPPORTED = "display_curve_supported";
        private static final String EXTRA_CPU_POWERSAVE_SUPPORTED = "cpu_powersave_supported";
        
        public FWSettingChangeService () {
              super("DebugService");
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if( LOG_INFO) Log.i(TAG, " onDestroy");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            if (intent == null) return;
            
            if (LOG_INFO) Log.d(TAG, "onHandleIntent intent is " + intent.toUri(0));
            String action = intent.getAction();
            if (action == null) {
                Log.e(TAG, "Intent action is null");
                return;
            }

            if (action.equalsIgnoreCase(POWER_SETTING_INTENT)) {
                boolean isDisplayCurveSupported = intent.getBooleanExtra(EXTRA_DISPLAY_CURVE_SUPPORTED, false);
                boolean isCpuPowerSaveSupported = intent.getBooleanExtra(EXTRA_CPU_POWERSAVE_SUPPORTED, false);;
               
                if (LOG_DEBUG) Log.d(TAG, "isDisplayCurveSupported is " + isDisplayCurveSupported + 
                        " isCpuPowerSaveSupported is " + isCpuPowerSaveSupported);
                
                //Save this in shared Pref
                Persistence.commitValue(getApplicationContext(), DISPLAY_CURVE_SUPPORT_KEY, isDisplayCurveSupported);
                CpuPowerSaverSupport.setCpuPowerSaverSupportPref(getApplicationContext(), isCpuPowerSaveSupported);
            } else if (action.equalsIgnoreCase(ACTION_PUBLISHER_EVENT)) {
                // Sync setting changes need to be processed by Background data
                if (Sync.SYNC_ACTION_KEY.equalsIgnoreCase(intent.getStringExtra(EXTRA_PUBLISHER_KEY))) {
                    ActionHelper.getAction(this, BackgroundData.BD_ACTION_KEY).handleChildActionCommands(getApplicationContext(), intent);
                }
            }
        }
    }

