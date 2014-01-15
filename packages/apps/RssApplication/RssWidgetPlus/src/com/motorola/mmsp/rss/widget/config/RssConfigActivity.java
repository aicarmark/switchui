package com.motorola.mmsp.rss.widget.config;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.motorola.mmsp.rss.R;
import com.motorola.mmsp.rss.common.RssConstant;
import com.motorola.mmsp.rss.service.flex.RssFlexFactory;

public class RssConfigActivity extends Activity {
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private static final String TAG = "RssConfigActivity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate begin");
		super.onCreate(savedInstanceState);
		Log.d(TAG, "intent=" + getIntent());
		mAppWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		Intent intent = new Intent(RssConstant.Intent.INTENT_RSSWIDGET_CONFIG);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		startActivity(intent);
		ContentValues values = getSettingDefaultValues();
//		ContentValues values = new ContentValues();
//		values.put(RssConstant.Content.WIDGET_ID, mAppWidgetId);
//		values.put(RssConstant.Content.WIDGET_TITLE, getResources().getString(R.string.rss));
//		values.put(RssConstant.Content.WIDGET_UPDATE_FREQUENCY, DEFAULT_FREQUENCY_VALUE_INDEX);
//		values.put(RssConstant.Content.WIDGET_VALIDITY_TIME, DEFAULT_VALIDATY_VALUE_INDEX);
		getContentResolver().insert(RssConstant.Content.WIDGET_URI, values);
		setResult(RESULT_OK,intent);
		finish();
	}
	
	private ContentValues getSettingDefaultValues() {
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.WIDGET_ID, mAppWidgetId);
		values.put(RssConstant.Content.WIDGET_TITLE, getResources().getString(R.string.rss));
		int defaultFrequencyIndex = RssFlexFactory.createFlexSettings().getUpdateFrequency(this);
		int defaultValidityIndex = RssFlexFactory.createFlexSettings().getNewsValidity(this);
		values.put(RssConstant.Content.WIDGET_UPDATE_FREQUENCY, defaultFrequencyIndex);
		values.put(RssConstant.Content.WIDGET_VALIDITY_TIME, defaultValidityIndex);
		return values;
	}

}
