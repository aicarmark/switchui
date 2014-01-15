package com.motorola.mmsp.socialGraph;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.util.Log;

import com.motorola.mmsp.socialGraph.R;
import com.motorola.mmsp.socialGraph.socialGraphWidget.Setting;
import com.motorola.mmsp.socialGraph.socialGraphWidget.define.Intents;
import com.motorola.mmsp.socialGraph.socialGraphWidget.model.RingLayoutModel;
import com.motorola.mmsp.socialGraph.socialwidget2D.SocialWidget2DModel;
import java.util.Locale;
public class SocialGraphApp extends Application {
	private Setting setting;
	private OnSharedPreferenceChangeListener settingListener;
	private static final String TAG = "SocialGraphApp";
	private SocialWidget2DModel mSocialWidget2DModel = null;
	private Locale mLocale;
	private int mOrientation;
	@Override
	public void onCreate() {
		super.onCreate();
		mSocialWidget2DModel = SocialWidget2DModel.getInstace();
		if(mSocialWidget2DModel != null){
			mSocialWidget2DModel.onCreate(this);
		}
		Constant.SHORTCUT_COUNT = getResources().getInteger(R.integer.shortcut_count);
		Constant.SHORTCUT_BIG_COUNT = getResources().getInteger(R.integer.shortcut_big_count);
		Constant.SHORTCUT_MEDIUM_COUNT = getResources().getInteger(R.integer.shortcut_medium_count);
		Constant.SHORTCUT_SMALL_COUNT = getResources().getInteger(R.integer.shortcut_small_count);
		
        startService(new Intent("com.motorola.mmsp.intent.action.SOCIAL_SERVICE_ACTION"));
		
		initSetting();
		Setting.getInstance(this).loadFactorySetting();
		
		new Thread(new Runnable() {
			public void run() {
				initRingLayoutModel();
			}
		}).start();	
		mLocale = Locale.getDefault();
		mOrientation = Configuration.ORIENTATION_UNDEFINED;
	}

    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d(TAG, "SocialWidget2DApp onConfigurationChanged()");
		if (newConfig.locale != mLocale || newConfig.orientation != mOrientation){
			mLocale = newConfig.locale;
			Log.d(TAG, "SocialWidget2DApp onConfigurationChanged mLocale:"+mLocale);
		}else{
			Intent intent = new Intent(Constant.BROADCAST_WIDGET_UPDATE);
			sendBroadcast(intent);
		}
	}	

	private void initRingLayoutModel() {
		RingLayoutModel.getDefaultRingLayoutModel(this);
	}
	
	private void initSetting() {
		setting = Setting.getInstance(this);
		settingListener = new OnSharedPreferenceChangeListener() {			
			//@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
					String key) {
				Intent intent = new Intent(Intents.BROADCAST_SETTING_CHANGE);
				intent.putExtra("key", key);
				sendBroadcast(intent);
			}
		};
		setting.registerOnSharedPreferenceChangeListener(settingListener);
	}
	
	@Override
	public void onTerminate() {
		setting.unregisterOnSharedPreferenceChangeListener(settingListener);
		if(mSocialWidget2DModel != null){
			mSocialWidget2DModel.onRelease(this);			
		}
		super.onTerminate();
	}
}
