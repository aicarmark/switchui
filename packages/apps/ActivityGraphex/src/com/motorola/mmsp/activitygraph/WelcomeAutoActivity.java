package com.motorola.mmsp.activitygraph;
import android.app.Activity;
import android.os.Bundle;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.content.Context;
import android.content.Intent;
import com.motorola.mmsp.activitygraph.R;
import android.util.Log;
import android.app.ActivityManager;
import android.view.KeyEvent;
import android.provider.Settings;
import android.content.ContentResolver;

public class WelcomeAutoActivity extends Activity
implements View.OnClickListener {
    private static final String TAG = "WelcomeAutoActivity";
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.welcome_auto_screen);
        
        findViewById(R.id.finish).setOnClickListener(this);
        findViewById(R.id.back).setOnClickListener(this);
    }
    
    public void onClick(View v) {
        Log.d(TAG, "onClick");	
        if(v.getId() == R.id.finish) {
            ContentResolver resolver = getBaseContext().getContentResolver();
            Settings.System.putInt(resolver,"box",1);
            Intent intent = new Intent("com.motorola.mmsp.activitygraph.OUT_OF_BOX");        
            sendBroadcast(intent);
         // Added by amt_chenjing for SWITCHUI-2076 20120712 begin
          // Added by amt_chenjing for SWITCHUI-2058 20120712 begin
            //int mode = ActivityGraphModel.getGraphyMode(WelcomeAutoActivity.this);
            //ActivityGraphModel.sendModeChange(mode);
         // Added by amt_chenjing for SWITCHUI-2058 20120712 end
         // Added by amt_chenjing for SWITCHUI-2076 20120712 end
            Activity activity = WelcomeActivity.getActivity();
            Log.d(TAG, "activity = " +activity );	
            if (activity != null) {
                activity.finish();
            }
            finish();
        }else if (v.getId() == R.id.back){
            finish();
        }
        
    }  
    
}
