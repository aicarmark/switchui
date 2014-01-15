package com.motorola.mmsp.performancemaster.ui;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.motorola.mmsp.performancemaster.engine.BatteryModeMgr;
import com.motorola.mmsp.performancemaster.engine.Log;

public class BatteryWidgetConfigure extends Activity {
    private static final String LOG_TAG = "BatterWidgetConfig: ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.e(LOG_TAG, "onCreate WidgetConfigure----");
        
        int mAppWidgetId = 0;
        
        Intent intent = getIntent();
        
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, 
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        
        checkDisclaimer(this);
        
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        
        finish();
    }
    
    private void checkDisclaimer(Context context) {
        SharedPreferences sharePrefs = context.getSharedPreferences(BatteryModeMgr.BATTERY_DISCLAIMER_PREFS, 0);
        int shown = sharePrefs.getInt(BatteryModeMgr.BATTERY_DISCLAIMER_SHOWN, -1);
        if (shown != 1) {
            Intent i = new Intent(context, DisclaimerActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
