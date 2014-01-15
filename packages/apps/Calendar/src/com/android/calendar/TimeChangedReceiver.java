/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.AsyncTask;
import android.provider.CalendarContract.CalendarCache;

/**
 * Receive the TIME_CHANGED action notification from system. Upon time changed,
 * we need to reload the cached <code>TimezoneRow</code> in
 * <code>TimezoneAdapter</code>
 */
public class TimeChangedReceiver extends BroadcastReceiver {
    private static String TAG = "TimeChangedReceiver";
    private static boolean DEBUG = false;
    /*2012-11-8, add by amt_sunli for SWITCHUITWOV-296 begin*/
    private static final String DISPLAY_BEIJINGTIME_ACTION = "android.intent.action.TimeDisplayChanged";
    /*2012-11-8, add by amt_sunli for SWITCHUITWOV-296 end*/

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction())) {
            // The system time has been changed, we needs to reload the
            // TimezoneAdapter's cache
            new UpdateTimezoneTask().execute();
        } else if (DISPLAY_BEIJINGTIME_ACTION.equals(intent.getAction())) {
         /*2012-11-8, add by amt_sunli for SWITCHUITWOV-296 begin*/
            if (Utils.isMOLCTModel()) {
                boolean isBeijing = intent.getBooleanExtra("IsDisplayBeijingTime", true);
                Log.d(TAG, "isBeijing = " + isBeijing);
                // if is beijing time set timezone to Asia/Shanghai
                Utils.setTimeZone(context, isBeijing?"Asia/Shanghai":CalendarCache.TIMEZONE_TYPE_AUTO);
             }
        }/*2012-11-8, add by amt_sunli for SWITCHUITWOV-296 end*/
    }

    public static class UpdateTimezoneTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            if (DEBUG) {
                Log.d(TAG, "Updating TimezoneRow's in TimezoneAdapter");
            }
            // Renew timezone caches in Adapter
            TimezoneAdapter.renewTimezones();
            return null;
        }
    }
}
