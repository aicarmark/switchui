package com.motorola.mmsp.performancemaster.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.motorola.mmsp.performancemaster.engine.BatteryModeMgr;
import com.motorola.mmsp.performancemaster.engine.Log;
import com.motorola.mmsp.performancemaster.R;

public class DisclaimerActivity extends Activity {
    private static final String LOG_TAG = "Disclaimer: ";
    private Button mDisclaimerButton;
    
    private void confirmDisclaimer() {
        // user consent to this disclaimer
        SharedPreferences sharePrefs = getSharedPreferences(BatteryModeMgr.BATTERY_DISCLAIMER_PREFS, 0);
        SharedPreferences.Editor editor = sharePrefs.edit();
        // disclaimer_shown = 1 for disclaimer has been shown
        editor.putInt(BatteryModeMgr.BATTERY_DISCLAIMER_SHOWN, 1);
        editor.commit();
        
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.i(LOG_TAG, "onCreate");
        
        setContentView(R.layout.disclaimer);
        
        mDisclaimerButton = (Button) findViewById(R.id.btn_disclaimer);
        
        mDisclaimerButton.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                confirmDisclaimer();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        Log.i(LOG_TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        Log.i(LOG_TAG, "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        Log.i(LOG_TAG, "onDestroy");
        confirmDisclaimer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        
        Log.i(LOG_TAG, "onStop");
    }
}
