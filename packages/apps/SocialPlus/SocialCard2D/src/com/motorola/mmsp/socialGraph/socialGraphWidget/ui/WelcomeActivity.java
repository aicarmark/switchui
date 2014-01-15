package com.motorola.mmsp.socialGraph.socialGraphWidget.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.motorola.mmsp.socialGraph.R;
import com.motorola.mmsp.socialGraph.socialGraphWidget.Setting;

public class WelcomeActivity extends Activity implements View.OnClickListener {
	private static final String TAG = "WelcomeActivity";
	RadioGroup radiogroup;
	RadioButton radio_auto, radio_manual;

	private Setting setting;
	
	private boolean mMode = true;
	
	private static Activity mActivity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mActivity = this;
		
		setting = Setting.getInstance(this);
        // added by amt_sunli 2012-12-14 SWITCHUITWO-292 begin
        mMode = setting.getWidgetMode();
        // added by amt_sunli 2012-12-14 SWITCHUITWO-292 end
		createViews();		
	}
	
	@Override
	protected void onResume() {
		Log.d(TAG, "onResume()");
		super.onResume();
		setRadioChecked();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(TAG, "onConfigurationChanged()");
		super.onConfigurationChanged(newConfig);
		createViews();
		setRadioChecked();
	}
	
	private void setRadioChecked() {
		if (mMode) {
			radio_auto.setChecked(true);
		} else {
			radio_manual.setChecked(true);
		}
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
						// modified by amt_sunli 2012-12-21 SWITCHUITWO-377 begin
						if (checkedId == radio_auto.getId()) {
							mMode = true;
							setting.setAutoMode(mMode);
						} else {
							mMode = false;
							setting.setAutoMode(mMode);
						}
						// modified by amt_sunli 2012-12-21 SWITCHUITWO-377 end
					}
				});
	}

	public void onClick(View v) {
		Log.d(TAG, "onClick");
		Intent intent = new Intent();
		intent.putExtra("welcome", 1);
		// modified by amt_sunli 2012-12-21 SWITCHUITWO-377 begin
		if (!radio_auto.isChecked()) {
//			setting.setAutoMode(false);
			intent.setClass(WelcomeActivity.this, ContactsManageActivity.class);
		} else {
//			setting.setAutoMode(true);
			intent.setClass(WelcomeActivity.this, WelcomeAutoActivity.class);
		}
		// modified by amt_sunli 2012-12-21 SWITCHUITWO-377 end
		startActivity(intent);
		//finish();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		mActivity = null;
		
		super.onDestroy();
	}
	
	public static Activity getActivity() {
		return mActivity;
	}
}
