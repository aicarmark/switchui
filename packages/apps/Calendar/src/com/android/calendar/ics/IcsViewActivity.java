package com.android.calendar.ics;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Config;

public class IcsViewActivity extends Activity {
    private static final String TAG = "IcsViewActivity";
    private static final boolean DEVELOPMENT = Config.DEBUG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // forward .ics file to IcsService

        final Intent intent = getIntent();
        final String mimeType = intent.getType();
        if (DEVELOPMENT) {
            Log.v(TAG, "MimeType: " + mimeType);
        }

        try {
            intent.setClass(this, IcsService.class);
            startService(intent);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't start Calendar app service for intent " + intent);
        }

        finish();
    }
}
