package com.motorola.mmsp.rss.app.activity;

import com.motorola.mmsp.rss.common.DatabaseHelper;
import com.motorola.mmsp.rss.common.RssConstant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public abstract class RssAppBaseActivity extends Activity {
	private static final String TAG = "RssAppBaseActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause() called");
		super.onPause();
		decreaseActivityCount();		
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume() called");
		super.onResume();
		increaseActivityCount();
	}
	
	
	@Override
	protected void onStop() {
		Log.d(TAG, "onStop() called");
		super.onStop();
		checkRssApplicationIsExit();
	}

	private void increaseActivityCount() {
		int count = DatabaseHelper.getInstance(this).getActivityCount();
		count = 1;
		DatabaseHelper.getInstance(this).updateActivityCount(count);
	}
	
	private void decreaseActivityCount() {
		int count = DatabaseHelper.getInstance(this).getActivityCount();
		count = 0;
		DatabaseHelper.getInstance(this).updateActivityCount(count);		
	}
	
	private void checkRssApplicationIsExit() {
		int count = DatabaseHelper.getInstance(this).getActivityCount();
		if(count <= 0) {
			startService(new Intent(RssConstant.Intent.INTENT_RSSAPP_EXITED));
		}
	}
}
