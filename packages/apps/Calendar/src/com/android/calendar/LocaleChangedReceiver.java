/*
 * Copyright (C) 2011 Motorola Mobility
 */

package com.android.calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Receive the LOCALE_CHANGED action notification from system. Upon locale changed,
 * we need to clear the cached <code>TimezoneRow</code> in
 * <code>TimezoneAdapter</code>
 */
public class LocaleChangedReceiver extends BroadcastReceiver {
    private static String TAG = "LocaleChangedReceiver";
    private static boolean DEBUG = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction())) {
            // The system locale has been changed, we needs to clear the
            // TimezoneAdapter's cache
            new UpdateTimezoneLocales().execute();
        }
    }

    public static class UpdateTimezoneLocales extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            if (DEBUG) {
                Log.i(TAG, "Updating TimezoneRow's in TimezoneAdapter");
            }
            // clear timezone caches in Adapter
            TimezoneAdapter.renewTimezonesLocales();
            return null;
        }
    }
}

