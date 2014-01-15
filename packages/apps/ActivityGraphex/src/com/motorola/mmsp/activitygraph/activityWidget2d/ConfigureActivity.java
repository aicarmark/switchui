package com.motorola.mmsp.activitygraph.activityWidget2d;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public class ConfigureActivity extends Activity {
    private static final String TAG = "ConfigureActivity";
    private static final int MAXCARD = 9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        if (isShowWelcome()) {
            Log.d(TAG, "show welocme activity");
            Intent intent = new Intent();
            intent.putExtra("screen", MAXCARD);
            intent.setClassName("com.motorola.mmsp.activitygraph",
                    "com.motorola.mmsp.activitygraph.WelcomeActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            this.startActivity(intent);
        } else {
            Log.d(TAG, "show no welcome activity");
        }
        AppWidgetManager.getInstance(this);
        Intent resultValue = new Intent();
        resultValue.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID));
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private boolean isShowWelcome() {
        Context context = null;
        try {
            context = createPackageContext("com.motorola.mmsp.activitygraph", 0);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, " ----error----");
            return false;
        }

        SharedPreferences shared = context.getSharedPreferences(
                "graphyPreferences", Context.MODE_WORLD_READABLE
                        + Context.MODE_WORLD_WRITEABLE
                        + Context.MODE_MULTI_PROCESS);
        boolean showWelcome = shared.getBoolean("popupWelcome", true);
        if (showWelcome) {
            shared.edit().putBoolean("popupWelcome", false).commit();
            return true;
        } else {
            return false;
        }
    }

}
