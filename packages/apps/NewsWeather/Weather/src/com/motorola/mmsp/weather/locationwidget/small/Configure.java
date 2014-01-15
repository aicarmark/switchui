/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.motorola.mmsp.weather.locationwidget.small;

import com.motorola.mmsp.weather.R;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.

/**
 * The configuration screen for the ExampleAppWidgetProvider widget sample.
 */
public class Configure extends Activity {

	private static final String LOG_TAG = "WeatherNewsProvider";

	int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	RadioButton mRadiobutton_big;
	RadioButton mRadiobutton_small;

	public Configure() {
		super();
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		// Set the result to CANCELED. This will cause the widget host to cancel
		// out of the widget placement if they press the back button.
		setResult(RESULT_CANCELED);

		setContentView(R.layout.configure);
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		// If they gave us an intent without the widget id, just bail.
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
		}
		mRadiobutton_big = (RadioButton) findViewById(R.id.radio_big);
		mRadiobutton_small = (RadioButton) findViewById(R.id.radio_small);
		mRadiobutton_big.setOnClickListener(mOnClickListener);
		mRadiobutton_small.setOnClickListener(mOnClickListener);
	}

	View.OnClickListener mOnClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			// Make sure we pass back the original appWidgetId
			saveWidgetType();
			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
			setResult(RESULT_OK, resultValue);
			finish();
		}
	};

	public void saveWidgetType() {
		SharedPreferences sp = getSharedPreferences(GlobalDef.CITYIDSHAREDPFE, 0);
		Editor e = sp.edit();
		if (mRadiobutton_small.isChecked()) {
			GlobalDef.MyLog.d(LOG_TAG, GlobalDef.WIDGET_TYPE + mAppWidgetId + "=" + GlobalDef.WIDGET_TYPE_SMALL);
			e.putString(GlobalDef.WIDGET_TYPE + mAppWidgetId, GlobalDef.WIDGET_TYPE_SMALL);
		} else {
			GlobalDef.MyLog.d(LOG_TAG, GlobalDef.WIDGET_TYPE + mAppWidgetId + "=" + GlobalDef.WIDGET_TYPE_BIG);
			e.putString(GlobalDef.WIDGET_TYPE + mAppWidgetId, GlobalDef.WIDGET_TYPE_BIG);
		}
		e.commit();
		AppWidgetManager am = AppWidgetManager.getInstance(this);
		WeatherNewsProvider.updateAppWidget(this, am, mAppWidgetId);
	}
}
