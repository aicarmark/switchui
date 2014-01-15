package com.motorola.mmsp.activitygraph;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextWatcher;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.view.View;
import android.widget.Button;
import android.content.Context;
import android.content.Intent;
import com.motorola.mmsp.activitygraph.R;
import android.util.Log;
import android.content.res.Configuration;

public class WelcomeActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "WelcomeActivity";
    RadioGroup radiogroup;
    RadioButton radio_auto, radio_manual;
    int maxCard = 9;
    private static Activity mActivity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        mActivity = this;
        if ((ActivityGraphModel.getMaxCard() != 7)
                && (ActivityGraphModel.getMaxCard() != 9)) {
            Log.d(TAG, "first set card num");
            Intent intent = getIntent();
            maxCard = intent.getIntExtra("screen", 0);
            ActivityGraphModel.setMaxCard(maxCard);
        }
        //createViews();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        createViews();
        super.onResume();
        boolean autoEnabled = (ActivityGraphModel.getGraphyMode(this) == ActivityGraphModel.AUTO_MODE) ? true
                : false;
        if (autoEnabled) {
            radio_auto.setChecked(true);
        } else {
            radio_manual.setChecked(true);
        }

    }

    // @Override
    // public void onConfigurationChanged(Configuration newConfig) {
    // Log.d(TAG, "onConfigurationChanged()");
    // super.onConfigurationChanged(newConfig);
    // createViews();
    // }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        mActivity = null;
        super.onDestroy();
    }

    private void createViews() {
        setContentView(R.layout.welcome_screen);
        findViewById(R.id.ok).setOnClickListener(this);
        radiogroup = (RadioGroup) findViewById(R.id.radiogroup1);
        radio_auto = (RadioButton) findViewById(R.id.radiobutton_auto);
        radio_manual = (RadioButton) findViewById(R.id.radiobutton_manual);
        setTitle(R.string.welcome_title);
        radiogroup
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        // TODO Auto-generated method stub
                     // Added by amt_chenjing for SWITCHUI-2058 20120712 begin
                        if (checkedId == radio_auto.getId()) {
                            ActivityGraphModel
                                    .setGraphyModeOnly(ActivityGraphModel.AUTO_MODE);
                        } else {
                            ActivityGraphModel
                                    .setGraphyModeOnly(ActivityGraphModel.MANUAL_MODE);
                        }
                     // Added by amt_chenjing for SWITCHUI-2058 20120712 begin
                    }
                });
    }

    public void onClick(View v) {
        Log.d(TAG, "onClick");
        Intent intent = new Intent();
        intent.putExtra("welcome", 1);

        if (!radio_auto.isChecked()) {
            intent.putExtra("card num", maxCard);
            intent.setClass(WelcomeActivity.this, ManageShortcuts.class);
        } else {
            intent.setClass(WelcomeActivity.this, WelcomeAutoActivity.class);
        }
        startActivity(intent);

    }

    public static Activity getActivity() {
        return mActivity;
    }

}
