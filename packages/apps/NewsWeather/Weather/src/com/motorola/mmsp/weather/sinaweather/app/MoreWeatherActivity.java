package com.motorola.mmsp.weather.sinaweather.app;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import android.app.ActionBar;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.mmsp.render.MotoAnimationSet;
import com.motorola.mmsp.render.MotoLinearAnimation;
import com.motorola.mmsp.render.MotoObjectAnimation;
import com.motorola.mmsp.render.gl.MotoGLRenderView;
import com.motorola.mmsp.render.util.MotoScreenLayout;
import com.motorola.mmsp.sinaweather.app.WeatherScreenConfig;
import com.motorola.mmsp.weather.R;
import com.motorola.mmsp.weather.locationwidget.small.GlobalDef;
import com.motorola.mmsp.weather.locationwidget.small.Utils;
import com.motorola.mmsp.weather.locationwidget.small.WeatherWidgetModel;
import com.motorola.mmsp.weather.sinaweather.app.MoreWeatherActivity.CityWeatherViewBuilder;
import com.motorola.mmsp.weather.sinaweather.service.WeatherServiceConstant;

public class MoreWeatherActivity extends Activity {
	private static final String TAG = "WeatherWidget";
	private DisplayMetrics dm;
	RelativeLayout.LayoutParams param1;
	ViewGroup.LayoutParams lp;
	private static int TIMEINTERVAL = 100;
	private static int UPDATETIMEINTERVAL = 100;
	private static final int GELLERY_KEY_ACTION_INTERVAL = 100;
	private static final int MENU_LIST1 = Menu.FIRST;
	private static final int MENU_LIST2 = Menu.FIRST + 1;
	private static final int MENU_LIST3 = Menu.FIRST + 2;
	
	private MotoGLRenderView mRenderView;
	private LinearLayout mRenderLinearLayout;
	private MyRelativeLayout mContentView;
	private Animation mActivityAnimation;
	
	public static boolean mIsWeatherUpdating = false;

	public View mViewloadingfooter = null;       

	public static String mSelectCityId = null;

	private int mPosition = 0;
	private WeathersManager mWeathersMgr = null;
	private CityWeatherViewBuilder mCityWeatherViewBuilder = new CityWeatherViewBuilder();
	private TabWidget mTabWidget = null;
	private Gallery mGallery = null;
	private Toast mToast = null;
	private int mSrcWidth = 0;
	private int mSrcHeight = 0;
	private int mLastResourceId = -1;
	private boolean isAnimationBackground = true;

	private int mWidgetId = 0;
	private boolean isFinish = false;//this flag is using for unsetRes to RenderView 
	private int screenWidth = 540;

	private static final String RES_ID 	= "res_id";
	private Handler mSelectionHandler = new Handler();
	private Runnable mSelectionRunnable = new Runnable() {
		// @Override
		public void run() {
			startAnimation(mPosition);
		}
	};

	// For key action
	private int mEachStepOffset = 0;
	private int mStep = 0;
	private static final int STEP_MAX = 8;
	private Handler mKeyActionHandler = new Handler();
	private Runnable mGalleryKeyActionRunnable = new Runnable() {
		public void run() {
			if (mGallery != null) {
				if (mStep < STEP_MAX - 1) {
					Log.d(TAG, "@@@@@@@ i = " + mStep);
					mGallery.onScroll(null, null, mEachStepOffset, 0);
					mKeyActionHandler.postDelayed(this, GELLERY_KEY_ACTION_INTERVAL);
				} else if (mStep == (STEP_MAX - 1)) {
					Log.d(TAG, "@@@@@@@ i = (LOOP_MAX - 1), mPosition = " + mPosition);
					setPosition(mPosition);
				}
				mStep++;
			} else {
				Log.d(TAG, "mGallery is null, won't respond any action");
			}
		}
	};

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {

		mPosition = savedInstanceState.getInt("mPosition");
		Log.d(TAG, "<<<<<<<<<<<<<<<<<<<<<< onRestoreInstanceState, mPosition=" + mPosition);

		setPosition(mPosition);
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		Log.d(TAG,
				">>>>>>>>>>>>>>>>>>>>>>>> onSaveInstanceState, mPosition=" + mPosition);

		outState.putInt("mPosition", mPosition);
		super.onSaveInstanceState(outState);
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		// @Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null && WeatherWidget.ACTION_WEATHER_UPDATED.equals(intent.getAction())) {
				mViewloadingfooter.setVisibility(View.GONE);
				mIsWeatherUpdating = false;
				int result = intent.getIntExtra("result", 0);
				if (result == WeathersManager.UPDATE_RESULT_PART_FAILED) {
					/*mToast.cancel();
					mToast.setText(R.string.update_partly_failed);
					mToast.show();*/
					Toast.makeText(MoreWeatherActivity.this, R.string.update_partly_failed, Toast.LENGTH_LONG).show();
				} else if (result == WeathersManager.UPDATE_RESULT_ALL_FAILED) {
					/*mToast.cancel();
					mToast.setText(R.string.update_failed);
					mToast.show();*/
					Toast.makeText(MoreWeatherActivity.this, R.string.update_failed, Toast.LENGTH_LONG).show();
				} else if (result ==  WeathersManager.UPDATE_RESULT_SUCCESS) {
					Toast.makeText(MoreWeatherActivity.this, R.string.update_success, Toast.LENGTH_LONG).show();
				}
				mCityWeatherViewBuilder.setNeedUpdate(true);
				setPosition(mPosition);
				mGallery.invalidate();
				
				if(!isAnimationBackground) {//if activity in background, no need to play animation, Modified by 001010
					mSelectionHandler.removeCallbacks(mSelectionRunnable);
					mSelectionHandler.postDelayed(mSelectionRunnable, TIMEINTERVAL);
				}				
				// mSelectionHandler.post(mSelectionRunnable);

			} else if (intent != null && intent.getAction().equals(WeatherWidget.ACTION_CITIES_UPDATED)) {
				mViewloadingfooter.setVisibility(View.GONE);
				mIsWeatherUpdating = false;
				mCityWeatherViewBuilder.setNeedUpdate(true);
				String type = intent.getStringExtra("type");
				String id = intent.getStringExtra("id");
				if (type.equals("delete")){
					mCityWeatherViewBuilder.removeView(id);
					if (mSelectCityId != null && mSelectCityId.equals(id)){
						mSelectCityId = Utils.getCityIdByWidgetId(context, mWidgetId);
					}
				}
				mGallery.setAdapter(new WeatherAdapter(MoreWeatherActivity.this, mWeathersMgr, mCityWeatherViewBuilder));
				int count = (mWeathersMgr != null) ? mWeathersMgr.getCount(MoreWeatherActivity.this) : 0;
				Log.d(TAG, "&&&&&&&& DetailView receiver - city change handler count = " + count + " mPosition = " + mPosition + "&&&&&&&&&&");
				if (mPosition >= count && count > 0) {
					mPosition = count - 1;
				}
				setPosition(mPosition);
//	refreshManualIfNoWeatherInfo();
			} else if (intent != null && intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
				Log.d(TAG, "************ Intent.ACTION_USER_PRESENT, unlock, start the anmiation again!!! ************");
				//mSelectionHandler.removeCallbacks(mSelectionRunnable);
				//mSelectionHandler.postDelayed(mSelectionRunnable, TIMEINTERVAL);
				// mSelectionHandler.post(mSelectionRunnable);
			} else if (intent != null && mRenderView != null && intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				Log.d(TAG,
						"########### Screen is off, invoke stop() ###########");

//				mRenderView.getRenderPlayer().pause();
                                
			} else if (intent != null && intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
			    Log.d(TAG, "************ ACTION_SCREEN_ON, unlock, start the anmiation again!!! ************");
	            //mSelectionHandler.removeCallbacks(mSelectionRunnable);
	            //mSelectionHandler.postDelayed(mSelectionRunnable, TIMEINTERVAL);
	            //mSelectionHandler.post(mSelectionRunnable);
					mUpdateTimeHandler.removeCallbacks(mUpdateTimeRunnable);
				mUpdateTimeHandler.postDelayed(mUpdateTimeRunnable, UPDATETIMEINTERVAL);
			}
		}
	};

	private Handler mUpdateTimeHandler = new Handler();
	private Runnable mUpdateTimeRunnable = new Runnable() {
		// @Override
		public void run() {
			Log.d(TAG, "### mUpdateTimeRunnable, mPosition = " + mPosition);
			try {
				//modify  by davidwgx start for IPB-1811
				//modify by xulei for switchui-989
				ImageView selectImageView;
				if (mV != null){
					selectImageView = (ImageView) mV.findViewById(R.id.selectCity);
				}
				else{
					selectImageView = (ImageView) findViewById(R.id.selectCity);
				}
				//end modify by xulei for switchui-989
				//modify  by davidwgx end
				String city = PersistenceData.getCityIdByIndex(MoreWeatherActivity.this, mPosition);
				if (city.equals(mSelectCityId)) {
//					selectImageView.setBackgroundResource(R.drawable.setting_icon_h);
					selectImageView.setBackgroundResource(R.drawable.city_home_highlight);
				} else {
//					selectImageView.setBackgroundResource(R.drawable.setting_icon);
					selectImageView.setBackgroundResource(R.drawable.city_home);
				}
				if(mV != null) //add by davidwgx for IPB-1811
				{
					WeatherInfo weatherInfo = mWeathersMgr.getWeatherFromCacheByCityId(MoreWeatherActivity.this, city);
					mCityWeatherViewBuilder.updateDayAndDate(MoreWeatherActivity.this, weatherInfo , mV);
					mCityWeatherViewBuilder.updateMoreWeatherStatus(MoreWeatherActivity.this, weatherInfo, mPosition, mV);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	private View mV = null;
	
	// @Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "************ DetailView onCreate begin 2011-12-09 ************");
		super.onCreate(savedInstanceState);
		int screenMode = -100;
                TIMEINTERVAL = WeatherScreenConfig.BG_DEDLAY_TIME;
		screenMode = WeatherScreenConfig.getWeatherScreenConfig(WeatherScreenConfig.DETAIL_VIEW_INDEX);
		if (screenMode == WeatherScreenConfig.LANDSCAPE_ONLY) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else if (screenMode == WeatherScreenConfig.PORTRAIT_ONLY) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		ActionBar bar = getActionBar();		
		bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_HOME);
		bar.setTitle("Weather");		
		bar.setHomeButtonEnabled(false);
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		//modify by 002777 for switchui-2185
		bar.setBackgroundDrawable(getResources().getDrawable(R.drawable.silver_bg));
		//end modify by 002777 for switchui-2185
		mContentView = (MyRelativeLayout) LayoutInflater.from(this).inflate(R.layout.gallery2, null);
//		setContentView(R.layout.gallery2);
		setContentView(mContentView);
		dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;     // 屏幕宽度（像素）
		
		mViewloadingfooter = findViewById(R.id.LinearLayoutloadingfooter);
		mGallery = (Gallery) findViewById(R.id.city_weather_gallery);
		
		mRenderLinearLayout = (LinearLayout) findViewById(R.id.weather_animation_layout);
		initWeatherConditionString(this);
		initSrcWidthandHeight();
		mWeathersMgr = WeathersManager.getInstance(this);
		mActivityAnimation = AnimationUtils.loadAnimation(this, R.anim.activityscale);
        mGallery.setCallbackDuringFling(true);
		mGallery.setOnItemSelectedListener(new OnItemSelectedListener() {
			// @Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				Log.d(TAG, "switch city start, time is " + System.currentTimeMillis());
				Log.d(TAG, "============ onItemSelected begin =============");
				int count = (mWeathersMgr != null) ? mWeathersMgr.getCount(MoreWeatherActivity.this) : 0;
                                int prePosition = mPosition;
				if (MoreWeatherActivity.this != null && arg0 != null && mWeathersMgr != null) {
					if (arg2 > count) {
						mPosition = (arg2 - count * 100000) % count;
						if (mPosition < 0) {
							mPosition = mPosition + count;
						}
					} else if (arg2 < 0) {
						mPosition = 0;
					} else {
						mPosition = arg2;
					}
					setTabPosition(arg2, mWeathersMgr.getCount(MoreWeatherActivity.this));
					if (arg1 != null) {
						mV = arg1;
						mUpdateTimeHandler.removeCallbacks(mUpdateTimeRunnable);
						mUpdateTimeHandler.postDelayed(mUpdateTimeRunnable, UPDATETIMEINTERVAL);
					}
				}
				if (MoreWeatherActivity.this != null && mSelectionHandler != null && mSelectionRunnable != null & prePosition != mPosition) {
                                        Log.d(TAG, "============ onItemSelected update the bg =============");
					mSelectionHandler.removeCallbacks(mSelectionRunnable);
					mSelectionHandler.postDelayed(mSelectionRunnable,
							TIMEINTERVAL);
				} else {
					Log.d(TAG, "============ conidtion is not satisfied, will not update the bg =============");
				}
				Log.d(TAG, "============ onItemSelected end =============");
			}

			// @Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		mCityWeatherViewBuilder.setGallery(mGallery);

		mTabWidget = (TabWidget) findViewById(R.id.city_weather_tab);
		mTabWidget.setEnabled(false);
		mTabWidget.setClickable(false);
		mTabWidget.setFocusable(false);
		mTabWidget.setFocusableInTouchMode(false);
		mTabWidget.setLongClickable(false);

		mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
		registerReceiver(receiver, new IntentFilter(WeatherWidget.ACTION_CITIES_UPDATED));
		registerReceiver(receiver, new IntentFilter(WeatherWidget.ACTION_WEATHER_UPDATED));
		registerReceiver(receiver, new IntentFilter(Constant.ACTION_CITIES_UPDATED_RESPONSE));
		registerReceiver(receiver, new IntentFilter("com.motorola.mmsp.motoswitch.action.MODE_SWITCHED"));
		registerReceiver(receiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
		registerReceiver(receiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		registerReceiver(receiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
		updateDataFromLocationWidget();
	}
	
	private void setRenderWidthandHeight() {
		MotoScreenLayout layout = new MotoScreenLayout();
		layout.convert(mSrcWidth, mSrcHeight, getResources().getDisplayMetrics().widthPixels, 
				getResources().getDisplayMetrics().heightPixels, 
				MotoScreenLayout.SCALE_MODE_UPPER_LEFT_CUT_LONG_EDGE);
		if (mRenderView.getRenderPlayer().getRootPicture() != null) {
			mRenderView.getRenderPlayer().getRootPicture().setX(layout.getX());
			mRenderView.getRenderPlayer().getRootPicture().setY(layout.getY());
			mRenderView.getRenderPlayer().getRootPicture().setScaleX(layout.getScaleX());
			mRenderView.getRenderPlayer().getRootPicture().setScaleY(layout.getScaleY());			
		}
//		mRenderView.getRenderPlayer().play();
	}
	
	private void initSrcWidthandHeight() {
		try {
			Context resContext = createPackageContext(
					"com.motorola.mmsp.weather", 0);
			int srcWidthId = resContext.getResources().getIdentifier(
					"com.motorola.mmsp.weather:integer/width",
					null, null);
			mSrcWidth = resContext.getResources().getInteger(srcWidthId);
			
			int srcHeightId = resContext.getResources().getIdentifier(
					"com.motorola.mmsp.weather:integer/height",
					null, null);
			mSrcHeight = resContext.getResources().getInteger(srcHeightId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	protected void onStart() {
		Log.d(TAG, "************ DetailView onStart begin ************");
		super.onStart();
		if (mWeathersMgr == null || mWeathersMgr.getCount(this) <= 0) {
			finish();
			isFinish = true;
		} else {
			// int position = this.mPosition;
			mSelectCityId = Utils.getCityIdByWidgetId(this, mWidgetId);
			isAnimationBackground = false;
			mContentView.startAnimation(mActivityAnimation);
			mCityWeatherViewBuilder.setNeedUpdate(true);
			mGallery.setAdapter(new WeatherAdapter(this, mWeathersMgr, mCityWeatherViewBuilder));
			setPosition(mPosition);

		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(TAG, "@@@@@@@@@@@@@@@ DetailView onNewIntent begin @@@@@@@@@@@@@@@");
		super.onNewIntent(intent);
		setIntent(intent);
		updateDataFromLocationWidget();
		Log.d(TAG, "@@@@@@@@@@@@@@@ DetailView onNewIntent end @@@@@@@@@@@@@@@");
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "************ DetailView onResume begin ************");
		super.onResume();
		if (isFinish){
			Log.d(TAG, "is ready to finish.");
			return;
		}
		if (mIsWeatherUpdating == false) {
			mViewloadingfooter.setVisibility(View.GONE);
		} else {
			mViewloadingfooter.setVisibility(View.VISIBLE);
		}
		Log.d(TAG, "mRenderView is " + mRenderView);
		if(mRenderView != null && mRenderView.getRenderPlayer().getStatus() == mRenderView.getRenderPlayer().PLAYER_PAUSE)
		{
			Log.d(TAG, "PLAYER STATE IS " + mRenderView.getRenderPlayer().getStatus());
			setRenderWidthandHeight();
			mRenderView.getRenderPlayer().play();
		} 
//		else {
//			if (mSelectionRunnable != null) {
//			    mSelectionHandler.removeCallbacks(mSelectionRunnable);
//			    mSelectionHandler.post(mSelectionRunnable);
//			    }
//		}
		if (mSelectionRunnable != null) {
		    mSelectionHandler.removeCallbacks(mSelectionRunnable);
		    mSelectionHandler.post(mSelectionRunnable);
		    }
	}
    
	private void updateDataFromLocationWidget() {
		Intent intent = getIntent();
		mWidgetId = intent.getIntExtra("widgetId", 0);
		String cityId = intent.getStringExtra("cityId");		
		mSelectCityId = cityId;
		Log.d(TAG, "!!!!!!!!!!! mWidgetId = " + mWidgetId + " mCityId = " + cityId);
		if (cityId != null && !cityId.equals("")) {
			mPosition = mWeathersMgr.getIndexFromCacheByCityId(MoreWeatherActivity.this, cityId);
			Log.d(TAG, "$$$$$$$$$$$$$$ city pos is = " + mPosition);
			setPosition(mPosition);
		}
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "============ DetailView onPause begin ============");
		super.onPause();

		ArrayList<String> mCityIds = mWeathersMgr.getCityIdsFromCache(this);
		// If the selected city is removed
		if (mCityIds.size() > 0 && !mCityIds.contains(mSelectCityId)) {
			notifyLocationWidget();
		}

		if (mRenderView != null)
		{
			Log.d(TAG, "########### invoke stop() ###########");

			mRenderView.getRenderPlayer().pause();
			mSelectionHandler.removeCallbacks(mSelectionRunnable);
		}
		Log.d(TAG, "============ DetailView onPause end ============");
	}

	public void notifyLocationWidget() {
		Log.d(TAG, "============ DetailView notifyLocationWidget mPosition = " + mPosition + " ============");
		if (mWeathersMgr == null) {
			mWeathersMgr = WeathersManager.getInstance(this);
		}
		mSelectCityId = mWeathersMgr.getCityIdFromCacheByIndex(MoreWeatherActivity.this, mPosition);
		Intent intent = new Intent("com.motorola.mmsp.motoswitch.action.weatherdetailview.exit");
		intent.putExtra("widgetId", mWidgetId);
		intent.putExtra("cityId", mSelectCityId);
		
		ContentResolver resolver = getContentResolver();
		ContentValues values = new ContentValues();
		values.put(WeatherWidgetModel.WIDGET_ID, mWidgetId);
		values.put(WeatherWidgetModel.CITY_ID, mSelectCityId);
		resolver.update(GlobalDef.uri_widgetCity, values, 
				WeatherWidgetModel.WIDGET_ID + "=?", new String[]{String.valueOf(mWidgetId)});
		
		Log.d(TAG, "broadcast widgetId-" + mWidgetId + " cityId-" + mSelectCityId + " back to LocationWidget");
		sendBroadcast(intent);
		updateIntent(mSelectCityId);
	}
	
    private void updateIntent(String selectCityId) {
    	Intent intent = this.getIntent();
    	intent.putExtra("cityId", selectCityId);    	
    }
    
	// @Override
	protected void onStop()
	{
		Log.d(TAG, "============ DetailView onStop begin ============");
		super.onStop();

//		// for SWITCHUI-1122 wvkf68
		if (mRenderView != null)
		{
			Log.d(TAG, "########### invoke stop() ###########");
			mRenderView.getRenderPlayer().release();
			mSelectionHandler.removeCallbacks(mSelectionRunnable);
		}
		// end
		if (mToast != null)
		{
			mToast.cancel();
		}
		isAnimationBackground = true;
		mLastResourceId = -1;
		mCityWeatherViewBuilder.setNeedUpdate(true);
	}

	// @Override
	protected void onDestroy() {
		Log.d(TAG, "============ DetailView onDestroy begin ============");
		super.onDestroy();
		unregisterReceiver(receiver);
		// receiver = null;
		mSelectionHandler.removeCallbacks(mSelectionRunnable);
		// mSelectionHandler = null;
		// mSelectionRunnable = null;
		mCityWeatherViewBuilder.clear();
		// mCityWeatherViewBuilder = null;
		// mWeathersMgr = null;
		// mTabWidget = null;
		// mGallery = null;
		// mToast = null;
		// mViewloadingfooter = null;
		// mSurfaceView = null;

		mKeyActionHandler.removeCallbacks(mGalleryKeyActionRunnable);
		if (mRenderView != null) {
			Log.d(TAG, "########### invoke stop() ###########");
			mRenderView.getRenderPlayer().release();
			mRenderView = null;
		}
		// mKeyActionHandler = null;
		// mGalleryKeyActionRunnable = null;
		Log.d(TAG, "============ DetailView onDestroy end ============");
	}

//	private void findView() {
//		mSurfaceView = (SurfaceView) findViewById(R.id.current_icon);
//		 dm = new DisplayMetrics();
//	        getWindowManager().getDefaultDisplay().getMetrics(dm);
//		param1 = new RelativeLayout.LayoutParams(
//				dm.widthPixels, dm.heightPixels);
//		mSurfaceView.setLayoutParams(param1);
//		mViewloadingfooter = findViewById(R.id.LinearLayoutloadingfooter);
//		mGallery = (Gallery) findViewById(R.id.city_weather_gallery);
//	}

	public boolean onCreateOptionsMenu(Menu menu) {
		int idGroup1 = 0;
		int orderMenuItem1 = Menu.NONE;
		int orderMenuItem2 = Menu.NONE + 1;
		int orderMenuItem3 = Menu.NONE + 2;
		menu.add(idGroup1, MENU_LIST2, orderMenuItem1, R.string.details_menu_refresh).setIcon(R.drawable.refresh);
		menu.add(idGroup1, MENU_LIST1, orderMenuItem2, R.string.details_menu_setting).setIcon(R.drawable.setting);
		menu.add(idGroup1, MENU_LIST3, orderMenuItem3, R.string.details_menu_share).setIcon(R.drawable.share);
		return super.onCreateOptionsMenu(menu);
	}
	
	

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		if (menu == null){
			return false;
		}
		WeatherInfo weather = mWeathersMgr.getWeatherFromCacheByIndex(this, mPosition);
		if (weather == null) {
			menu.getItem(2).setEnabled(false);
		}
		else
		{
			menu.getItem(2).setEnabled(true);
		}
		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case (MENU_LIST1):
			Intent intent = new Intent(MoreWeatherActivity.this,
					WeatherSettingActivity.class);
			startActivity(intent);
			break;

		case (MENU_LIST2):
			mViewloadingfooter.setVisibility(View.VISIBLE);
			mIsWeatherUpdating = true;
			Intent intent_refresh = new Intent(Constant.ACTION_WEATHER_UPDATE_REQUEST);
			Log.d(TAG, "Refresh detail view, action is " + Constant.ACTION_WEATHER_UPDATE_REQUEST);
			intent_refresh.putExtra("Manual", "true");

			this.startService(intent_refresh);
			break;

		case (MENU_LIST3):
			mCityWeatherViewBuilder.shareInfo(MoreWeatherActivity.this, mWeathersMgr,mPosition);
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private String[][] WEAHTER_CONDITION  = null;	
	private static int[] weatherAnimDay = new int[] { 
		R.xml.livewallpaper_weather_sunny,
		R.xml.livewallpaper_weather_cloudy,//cloudy
		R.xml.livewallpaper_weather_rainy,
		R.xml.livewallpaper_weather_snowy,//snow
		R.xml.livewallpaper_weather_foggy,//fog
		R.xml.livewallpaper_weather_sandstorm,
		R.xml.livewallpaper_weather_thunderstorm
	};
	private static int[] weatherAnimNight = new int[]{
		R.xml.livewallpaper_weather_sunny_night,
		R.xml.livewallpaper_weather_cloudy_night,
	};
	
	public void initWeatherConditionString(Context context) {
		String[] sunny = context.getResources().getStringArray(R.array.sunny);
		String[] cloudy = context.getResources().getStringArray(R.array.cloudy);
		String[] rain = context.getResources().getStringArray(R.array.rain);
		String[] snow = context.getResources().getStringArray(R.array.snow);
		String[] fog = context.getResources().getStringArray(R.array.fog);
		String[] sand_storm = context.getResources().getStringArray(R.array.sand_storm);
		String[] thunder = context.getResources().getStringArray(R.array.thunder);
		WEAHTER_CONDITION = new String[][] { sunny, cloudy, rain, snow, fog, sand_storm, thunder };
	}
	
	 private int getResourceId(String weatherCondition, int day_night)
     {
		System.out.println("getResourceId weatherInfo = " + weatherCondition);
     	for (int i = 0; i < WEAHTER_CONDITION.length; i++)
			{
				for (int j = 0; j < WEAHTER_CONDITION[i].length; j++)
				{
					if(WEAHTER_CONDITION[i][j].equals(weatherCondition))
					{
						if(day_night == 2 && (i == 0 || i == 1))
						{
							//sunny or cloudy night
							return weatherAnimNight[i];
						}
						else 
						{
							return weatherAnimDay[i];
						}
						
					}
				}
			}
     	
     	//default return sunny day
     	return weatherAnimDay[0];	        	
     }
		
	private void refreshManual()
	{
		mViewloadingfooter.setVisibility(View.VISIBLE);
		mIsWeatherUpdating = true;
		Intent intent_refresh = new Intent(
				Constant.ACTION_WEATHER_UPDATE_REQUEST);
		Log.d(TAG, "Refresh detail view, action is "
				+ Constant.ACTION_WEATHER_UPDATE_REQUEST);
		intent_refresh.putExtra("Manual", "true");

		this.startService(intent_refresh);
	}
	
	private void refreshManualIfNoWeatherInfo(){
		int cityCount = mWeathersMgr.getCount(this);
		WeatherInfo weather = null;
		for (int i = 0;i<cityCount;i++){
			weather = mWeathersMgr.getWeatherFromCacheByIndex(this, i);
			if (weather == null){
				Log.d(TAG, "There is a city has no weather info,refresh all manual");
				refreshManual();
				break;
			}
		}
	}

	private static final int MSG_CHANGE_WEATHER_ANIMATION = 0;
	
	private Handler hander = new Handler()
	{
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case MSG_CHANGE_WEATHER_ANIMATION:
				if (mRenderView != null)
				{
					Log.i("Weather", "mRenderView != null");
					if(mLastResourceId != msg.getData().getInt(RES_ID)) {
						mLastResourceId = msg.getData().getInt(RES_ID);
						mRenderView.setPlayerRes("com.motorola.mmsp.weather", msg.getData().getInt(RES_ID));
						showSwitchAniamtion();
						setRenderWidthandHeight();
						mRenderView.getRenderPlayer().play();
					} 
					Log.d("Zhongyin", "switch city end, begin to play animation, time is " + System.currentTimeMillis());
				}
				else
				{
					Log.i("Weather", "mRenderView == null");
					mRenderView = new MotoGLRenderView(MoreWeatherActivity.this, msg.getData().getInt(RES_ID));
					mLastResourceId = msg.getData().getInt(RES_ID);
					showSwitchAniamtion();
					mRenderLinearLayout.addView(mRenderView);
					setRenderWidthandHeight();
					mRenderView.getRenderPlayer().play();
				}
				break;
			default:
				break;
			}
		};
	};
	private void startAnimation(final int position) {
		runOnUiThread(new Runnable() {
			// @Override
			public void run() {
				mCityWeatherViewBuilder.startAnimation(MoreWeatherActivity.this, mWeathersMgr, position);
			}
		});
	}

//	public void startMp4Animation(int Type)
//	{
//		if (mCtrl == null)
//		{
//			mSurfaceView = (SurfaceView) findViewById(R.id.current_icon);
//			dm = new DisplayMetrics();
//			getWindowManager().getDefaultDisplay().getMetrics(dm);
//			param1 = new RelativeLayout.LayoutParams(dm.widthPixels,
//					dm.heightPixels);
//			mSurfaceView.setLayoutParams(param1);
//			// mCtrl = new Controller(mSurfaceView);
//			mCtrl = new EngineController(this, mSurfaceView, -1);
//		}
//		mSurfaceView.setVisibility(View.VISIBLE);
//		Log.d(TAG, "########### invoke playWeather() ###########");
//		try
//		{
//			mCtrl.controllerPlay();
//		} catch (IllegalArgumentException e)
//		{
//			Log.d(TAG,
//					"IllegalArgumentException occurs when play the video");
//		} catch (IllegalStateException e)
//		{
//			Log.d(TAG,
//					"IllegalStateException occurs when play the video");
//		}
//	}

	private void setPosition(int pos) {
		Log.d(TAG, "============ DetailView setPosition begin ============");
		int count = (mWeathersMgr != null) ? mWeathersMgr.getCount(this) : 0;
		int nowpos = pos;

		if (nowpos >= count) {
			mPosition = count - 1;
			return;
		}
		if (nowpos < 0) {
			mPosition = 0;
			return;
		}
		int position = 0;
		if (nowpos >= 0 && nowpos < count) {
			if (count == 1) {
				position = 0;
			} else {
				position = (count * 100000)
						+ (pos % ((count != 0) ? count : 1));
			}
			mGallery.setSelection(position);
			setTabPosition(position, count);
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "onKeyDown, keycode = " + keyCode
				+ " mPosition = " + mPosition);
		int count = (mWeathersMgr != null) ? mWeathersMgr.getCount(this) : 0;
		mStep = 0;
            if ( count >1 ) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_DPAD_UP:
			/*mPosition -= 1;
			if (mPosition < 0) {
				mPosition = count - 1;
			}*/
			mEachStepOffset = 0 - (mGallery.getWidth() / STEP_MAX);
			Log.d(TAG,
					"onKeyDown, KEYCODE_DPAD_LEFT after caculate mPosition = "
							+ mPosition + " offset = " + mEachStepOffset);
			mSelectionHandler.removeCallbacks(mSelectionRunnable);
                        if (mRenderView != null) {
                            Log.d(TAG, "########### invoke pause() ###########");
                            mRenderView.getRenderPlayer().pause();
                        } 			
                        mKeyActionHandler.postAtFrontOfQueue(mGalleryKeyActionRunnable);

			break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
		case KeyEvent.KEYCODE_DPAD_DOWN:
			/*mPosition += 1;
			if (mPosition >= count) {
				mPosition = 0;
			}*/
			mEachStepOffset = mGallery.getWidth() / STEP_MAX;
			Log.d(TAG,
					"onKeyDown, KEYCODE_DPAD_RIGHT after caculate mPosition = "
							+ mPosition + " offset = " + mEachStepOffset);
			mSelectionHandler.removeCallbacks(mSelectionRunnable);
                        if (mRenderView != null) {
                            Log.d(TAG, "########### invoke pause() ###########");
                            mRenderView.getRenderPlayer().pause();
                        }
			mKeyActionHandler.postAtFrontOfQueue(mGalleryKeyActionRunnable);

			break;
		default:
			Log.d(TAG, "onKeyDown, default branch");
			break;
		}
            }
		return super.onKeyDown(keyCode, event);
	}
	
	/*
	 * public boolean onKeyUp(int keyCode, KeyEvent event) {
	 * Log.d(TAG, "onKeyUp, keycode = " + keyCode); switch (keyCode)
	 * { case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_DPAD_UP:
	 * Log.d(TAG, "onKeyUp, KEYCODE_DPAD_LEFT"); break; case
	 * KeyEvent.KEYCODE_DPAD_RIGHT: case KeyEvent.KEYCODE_DPAD_DOWN:
	 * Log.d(TAG, "onKeyUp, KEYCODE_DPAD_RIGHT");
	 * 
	 * break; default: Log.d(TAG, "onKeyUp, default branch"); break;
	 * }
	 * 
	 * return super.onKeyUp(keyCode, event); }
	 */
	private void setTabPosition(int position, int count) {
		int pos = (count == 0) ? 0 : (position % count);
		for (int i = 0; i < count; i++) {
			int icon = 0;
			if (i == pos) {
				icon = R.drawable.hotseat_scrubber_holo;
			} else {
				icon = R.drawable.hotseat_track_holo;
			}
			ImageView v = (ImageView) mTabWidget.getChildAt(i);
			int indicatorWidth = (int)(screenWidth - (40 * 1.5));//40dp is paddingLeft and paddingRight
			int newWidth = indicatorWidth/count;
			if (v == null) {
				v = new ImageView(this);
				// 设置想要的大小
				lp = new ViewGroup.LayoutParams(newWidth,8);
				v.setLayoutParams(lp);
				v.setEnabled(false);
				float sPadding = getResources().getDimension(
						R.dimen.detail_pointer_lr_padding);
				v.setPadding((int) sPadding, 0, (int) sPadding, 0);
				mTabWidget.addView(v, i);
			}else{
				lp = v.getLayoutParams();
				lp.width = newWidth;
				v.setLayoutParams(lp);
			}
			
			if (i == pos) {
				v.setImageResource(icon);
				v.setScaleType(ScaleType.FIT_XY);
			}else{
				v.setImageDrawable(null);
			}
			
		}

		for (int i = count; i < mTabWidget.getChildCount(); i++) {
			mTabWidget.removeViewAt(i);
		}
	}
	
	class CityWeatherViewBuilder {
		private static final float TEXTSIZE = 60;
		private String mSunRise = "6:00AM";
		private String mSunSet = "6:00PM";
		private HashMap<String, View> mViews = new HashMap<String, View>();
		private HashMap<String, String> mConditions = new HashMap<String, String>();
		private HashMap<String, Boolean> mUpdateStatus = new HashMap<String, Boolean>();

		private View mGallery = null;	

		public void setGallery(View gallery) {
			this.mGallery = gallery;
		}

		public View getView(Context context, WeathersManager wm, int pos) {
			int cityCount = wm.getCount(context);
			int position = (cityCount == 0) ? 0 : (pos % cityCount);

			String cityId = wm.getCityIdFromCacheByIndex(context, position);
			if (mViews == null) {
				mViews = new HashMap<String, View>();
			}
			View v = (mViews != null) ? mViews.get(cityId) : null;
			
			WeatherInfo weather = wm.getWeatherFromCacheByCityId(context, cityId);

			if (weather == null) {
	                        Log.d(TAG, "weather is empty, create a new object and set the city name");
				weather = new WeatherInfo();
				weather.mInfomation.mCity = cityId;
			} else if (weather.mCurrent != null && 
					   weather.mInfomation != null && 
					     (weather.mInfomation.mCity == null || 
					       "".equals(weather.mInfomation.mCity)
					     )
					   ) {
				Log.d(TAG, "weather condition is empty, set the city name");
				weather.mInfomation.mCity = cityId;
			} else {
				//TODO
			}

			if (v != null && weather != null && !mUpdateStatus.get(cityId)) {
				Log.d(TAG, "getView time_fomate v and weather is not null,only update time");

				//updateDayAndDate(context,weather,v);
				//updateMoreWeatherStatus(context,weather,pos,v);
			} else {
				if (v == null) {
					v = newCityWeatherView(context);
					mViews.put(cityId, v);
				}
				mUpdateStatus.put(cityId, false);
				setViewWeather(context, v, weather, position, cityCount);
			}

			return v;
		}

		public void startAnimation(MoreWeatherActivity context, WeathersManager wm,
				int pos) {
			int cityCount = wm.getCount(context);
			int position = (cityCount == 0) ? 0 : (pos % cityCount);
			WeatherInfo weather = wm.getWeatherFromCacheByIndex(context, position);
			if (weather == null) {
				String cityId = wm.getCityIdFromCacheByIndex(context, position);
				getActionBar().setTitle(cityId);
				
				//set default animation when no weather
				Message msg = new Message();
				msg.what = MSG_CHANGE_WEATHER_ANIMATION;
				Bundle data = new Bundle();
				data.putInt(RES_ID, R.xml.livewallpaper_weather_sunny);
				msg.setData(data);
				hander.sendMessage(msg);
				return;
			}
			String timeZone = weather.mCurrent.mTimezone;
			TimeZone tz = null;
			if (timeZone == null || timeZone.equals("")) {// sina
				tz = TimeZone.getDefault();
			} else {// accu
				if (timeZone.contains("-")) {
					tz = TimeZone.getTimeZone("GMT" + timeZone);
				} else {
					tz = TimeZone.getTimeZone("GMT+" + timeZone);
				}
			}

			Calendar calendar = Calendar.getInstance(tz);
			int hour = calendar.get(Calendar.HOUR_OF_DAY);
			int minute = calendar.get(calendar.MINUTE);
			String mF1 = "HH:mm";

			boolean isSuRise = WeatherUtils.isDayOrNight(mSunRise, hour, minute,
					mF1); // true
			boolean isSuSet = WeatherUtils.isDayOrNight(mSunSet, hour, minute, mF1);// false

			int dayNight = 0;
			if (isSuRise && !isSuSet) {
				// weather.mInfomation.mDayNight = WeatherInfo.DAY;
				dayNight = WeatherInfo.DAY;
			} else {
				dayNight = WeatherInfo.NIGHT;
			}
	             
			int resid = getResourceId(weather.mCurrent.mCondition, dayNight);
	                
			if (resid != -1)
			{
				getActionBar().setTitle(weather.mInfomation.mCity);
				Message msg = new Message();
				msg.what = MSG_CHANGE_WEATHER_ANIMATION;
				Bundle data = new Bundle();
				data.putInt(RES_ID, resid);
				msg.setData(data);
				hander.sendMessage(msg);
			}
		}

		public View newCityWeatherView(Context context) {
			LayoutInflater factory = LayoutInflater.from(context);
			View v = factory.inflate(R.layout.city_weather2, null);

			return v;
		}

		/*public void shareInfo(MoreWeatherActivity context) {
			String myShareInfo = mergerString(context.getString(R.string.mcity),
					mCityName)
					+ mergerString(context.getString(R.string.mDate), mDate)
					+ mergerString(context.getString(R.string.mWeatherCondition),
							mWeatherCondition)
					+ mergerString(context.getString(R.string.realfeel),
							mCurrenttemperature)
					+ mergerString(context.getString(R.string.mHighTemperature),
							mHighTemper)
					+ mergerString(context.getString(R.string.mLowTemperature),
							mLowpera);

			Intent send = new Intent(Intent.ACTION_SEND);
			send.setType("text/plain");
			send.putExtra(Intent.EXTRA_TEXT, myShareInfo);
			send.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(send);

		}*/
		
		
		public void shareInfo(MoreWeatherActivity context, WeathersManager wm, int pos) {
			String cityName = "";
			String dateName = "";
			String weatherCondition = "";
			String currenttemperature = "";
			String highTemper = "";
			String lowpera = "";
			
			int cityCount = wm.getCount(context);
			int position = (cityCount == 0) ? 0 : (pos % cityCount);
			WeatherInfo weather = wm.getWeatherFromCacheByIndex(context, position);
			if (weather != null) {
				if (weather.mInfomation != null && weather.mInfomation.mCity != null) {
				cityName = weather.mInfomation.mCity;
				}
				
				if (weather.mCurrent != null && weather.mCurrent.mCondition != null) {
				weatherCondition = weather.mCurrent.mCondition;
				}
				
				String temp ="";
				// SharedPreferences sp =
				// PersistenceData.getSharedPreferences(context);
				String units = DatabaseHelper.readSetting(context,
						PersistenceData.SETTING_TEMP_UNIT);
				if (units.equals("")) {
					units = "0";
				}
				int unit = 0;
				try {
					unit = Integer.parseInt(units);
				} catch (Exception e1) {
					unit = 0;
					e1.printStackTrace();
				}
				if (weather.mCurrent !=null && weather.mCurrent.mTemperatrue != null
						&& !"".equals(weather.mCurrent.mTemperatrue)) {
					temp = getTemperature(context,
							weather.mCurrent.mTemperatrue, unit, true);
					
				} else if (weather.mForecasts !=null && !weather.mForecasts.isEmpty()
						&& weather.mForecasts.get(0) != null) {
					String realFeel ="";
					if(weather.mCurrent !=null && weather.mCurrent.mRealFeel != null && !"".equals(weather.mCurrent.mRealFeel)){
					 realFeel = weather.mCurrent.mRealFeel;
					}
					if (null != realFeel && !"".equals(realFeel)) {	
						Float temperature = 0.0f;
						try {
							temperature = Float.parseFloat(realFeel);
							int feel = Math.round(temperature);
							realFeel = String.valueOf(feel);		
							temp = getTemperature(context, realFeel, unit, true);
						} catch (NumberFormatException e) {
							temp = "";
							e.printStackTrace();
						}
						
					
					} else if (null == realFeel || "".equals(realFeel)) {
						temp = "";
						
					} 
					
				
				} else {
					temp = context.getString(R.string.weather_get_error);
					
				}

				
				currenttemperature = temp;
				
				if (weather.mForecasts != null  && weather.mForecasts.get(0) !=null && !weather.mForecasts.isEmpty()  ) {
				highTemper = getTemperature(context, weather.mForecasts.get(0).mHigh, unit, true);
				lowpera = getTemperature(context, weather.mForecasts.get(0).mLow, unit, true);
				}
				String[] weekDays = new DateFormatSymbols().getShortWeekdays();
				SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");
				String day = "";

				Calendar calendar = Calendar.getInstance();
				long longmillis = calendar.getTimeInMillis();
				Date d = new Date(longmillis);
				int day_index = d.getDay();
				day = weekDays[day_index + 1];

				if (day == null || day.equals("")) {
					try {
						String date ="";
						if(weather.mForecasts != null && weather.mForecasts.get(0)!=null && weather.mForecasts.get(0).mDate!=null){
						 date = weather.mForecasts.get(0).mDate;
						}
						Date dt = ft.parse(date);
						int dayIndex = dt.getDay();
						day = weekDays[dayIndex + 1];

					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateFormat(context);
				String date = sdf.format(d);
				String time_fomate = day + ", " + date;
				dateName = time_fomate;
				StringBuffer myShareInfo = new StringBuffer("");
				myShareInfo.append(mergerString(context.getString(R.string.mcity), cityName));
				myShareInfo.append(mergerString(context.getString(R.string.mDate), dateName));
				myShareInfo.append(mergerString(context.getString(R.string.mWeatherCondition), weatherCondition));
				if (!currenttemperature.trim().equals("")){
					myShareInfo.append(mergerString(context.getString(R.string.realfeel), currenttemperature));
				}
				if (!highTemper.trim().equals("")){
					myShareInfo.append(mergerString(context.getString(R.string.mHighTemperature), highTemper));
				}
				if (!lowpera.trim().equals("")){
					myShareInfo.append(mergerString(context.getString(R.string.mLowTemperature), lowpera));
				}
				Intent send = new Intent(Intent.ACTION_SEND);
				send.setType("text/plain");
				send.putExtra(Intent.EXTRA_TEXT, myShareInfo.toString());
				send.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	                        send.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				context.startActivity(send);
			}

		}

		private String mergerString(String prefix, String suffix) {
			String mergerString = prefix + ":" + suffix + "\n";
			return mergerString;
		}

		public void setViewWeather(final Context context, View view,
				WeatherInfo weatherInfo, int pos, int count) {

			if (view == null || view.findViewById(R.id.current_city) == null) {
				return;
			}

			TextView tv = null;
			TextView cell_day_1 = null;
			TextView cell_day_2 = null;
			TextView cell_day_3 = null;
			TextView cell_day_4 = null;

			final ImageView selectCity = (ImageView) view
					.findViewById(R.id.selectCity);
			selectCity.setOnTouchListener(new OnTouchListener()
			{
				public boolean onTouch(View v, MotionEvent evenst)
				{
					 ((MoreWeatherActivity) context).notifyLocationWidget();
//					 selectCity.setBackgroundResource(R.drawable.setting_icon_h);
					 selectCity.setBackgroundResource(R.drawable.city_home_highlight);
					return false;
				}
			});
			LinearLayout linearLayout;

			ImageView cell_icon_1 = null;
			ImageView cell_icon_2 = null;
			ImageView cell_icon_3 = null;
			ImageView cell_icon_4 = null;

			TextView cell_range_1 = null;
			TextView cell_range_2 = null;
			TextView cell_range_3 = null;
			TextView cell_range_4 = null;

			cell_day_1 = (TextView) view.findViewById(R.id.cell_day_1);
			cell_icon_1 = (ImageView) view.findViewById(R.id.cell_icon_1);
			cell_range_1 = (TextView) view.findViewById(R.id.cell_range_1);

			cell_day_2 = (TextView) view.findViewById(R.id.cell_day_2);
			cell_icon_2 = (ImageView) view.findViewById(R.id.cell_icon_2);
			cell_range_2 = (TextView) view.findViewById(R.id.cell_range_2);

			cell_day_3 = (TextView) view.findViewById(R.id.cell_day_3);
			cell_icon_3 = (ImageView) view.findViewById(R.id.cell_icon_3);
			cell_range_3 = (TextView) view.findViewById(R.id.cell_range_3);

			cell_day_4 = (TextView) view.findViewById(R.id.cell_day_4);
			cell_icon_4 = (ImageView) view.findViewById(R.id.cell_icon_4);
			cell_range_4 = (TextView) view.findViewById(R.id.cell_range_4);

			if (weatherInfo != null && 
				weatherInfo.mInfomation.mCity != null && 
				 (weatherInfo.mCurrent.mCondition == null || 
				  "".equals(weatherInfo.mCurrent.mCondition)
				 )
				) {
				tv = (TextView) view.findViewById(R.id.more_weather_status);
				tv.setText(context.getString(R.string.weather_null));
				tv = (TextView) view.findViewById(R.id.current_city);
				tv.setText(weatherInfo.mInfomation.mCity);
				tv = (TextView) view.findViewById(R.id.realfeel);
				tv.setVisibility(View.INVISIBLE);
				view.findViewById(R.id.temperatureImg).setVisibility(View.INVISIBLE);
			} else if (weatherInfo != null && weatherInfo.mError != null) {
				tv = (TextView) view.findViewById(R.id.more_weather_status);
				tv.setText(context.getString(R.string.weather_get_error));
			} else {
				updateMoreWeatherStatus(context,weatherInfo,pos,view);

				tv = (TextView) view.findViewById(R.id.current_city);
				tv.setText(weatherInfo.mInfomation.mCity);
			

				tv = (TextView) view.findViewById(R.id.current_condition);
				tv.setText(weatherInfo.mCurrent.mCondition);

				String temp = null;
				// SharedPreferences sp =
				// PersistenceData.getSharedPreferences(context);
				String units = DatabaseHelper.readSetting(context,
						PersistenceData.SETTING_TEMP_UNIT);// String units =
															// sp.getString(PersistenceData.SETTING_TEMP_UNIT,
															// "0");
				if (units.equals("")) {
					units = "0";
				}
				int unit = Integer.parseInt(units);
				tv = (TextView) view.findViewById(R.id.realfeel);
				tv.setVisibility(View.VISIBLE);
				if (weatherInfo.mCurrent.mTemperatrue != null
						&& !"".equals(weatherInfo.mCurrent.mTemperatrue)) {
					temp = getTemperature(context,
							weatherInfo.mCurrent.mTemperatrue, unit, true);
					tv.setText(context.getString(R.string.realfeel));
					tv = (TextView) view.findViewById(R.id.current_temperature);
					tv.setText(temp);
					//tv.setTextSize(TEXTSIZE);
				} else if (!weatherInfo.mForecasts.isEmpty()
						&& weatherInfo.mForecasts.get(0) != null) {
					String realFeel = weatherInfo.mCurrent.mRealFeel;
					if (null != realFeel && !"".equals(realFeel)) {	
						Float temperature = 0.0f;
						try {
							temperature = Float.parseFloat(realFeel);
							int feel = Math.round(temperature);
							realFeel = String.valueOf(feel);		
							temp = getTemperature(context, realFeel, unit, true);
							tv.setText(context.getString(R.string.realfeel));
						} catch (Exception e) {
							temp = "";
							e.printStackTrace();
						}
						
						tv = (TextView) view.findViewById(R.id.current_temperature);
						tv.setText(temp);
						//tv.setTextSize(TEXTSIZE);
					} else if (null == realFeel || "".equals(realFeel)) {
						temp = "";
						tv = (TextView) view.findViewById(R.id.current_temperature);
						tv.setText(temp);
						tv = (TextView)view.findViewById(R.id.realfeel);
						tv.setVisibility(View.INVISIBLE);
					} 
				} else {
					temp = context.getString(R.string.weather_get_error);
					tv = (TextView) view.findViewById(R.id.current_temperature);
					tv.setText(temp);
					tv.setTextSize(12);
					tv = (TextView)view.findViewById(R.id.realfeel);
					tv.setVisibility(View.INVISIBLE);
				}

				
				// refresh
				tv = (TextView) view.findViewById(R.id.current_range);
				if (!weatherInfo.mForecasts.isEmpty()) {
					tv
							.setText(getString(R.string.high) + " " + getTemperature(context,
											weatherInfo.mForecasts.get(0).mHigh,
											unit, true));
					
				}
				tv = (TextView) view.findViewById(R.id.current_low_range);

				if (!weatherInfo.mForecasts.isEmpty()) {
					tv
							.setText(getString(R.string.low) + " " + getTemperature(context,
											weatherInfo.mForecasts.get(0).mLow,
											unit, true));
					
				}
				
				if (weatherInfo.mForecasts.isEmpty() || weatherInfo.mForecasts.get(0).mHigh == null 
						|| weatherInfo.mForecasts.get(0).mHigh.equals("")){
					view.findViewById(R.id.temperatureImg).setVisibility(View.INVISIBLE);
				}
				else
				{
					view.findViewById(R.id.temperatureImg).setVisibility(View.VISIBLE);
				}

				int size = weatherInfo.mForecasts.size();
				for (int i = size - 1; i > 0; i--) {
					if (weatherInfo.mForecasts.get(i) != null
							&& weatherInfo.mForecasts.get(i).mHigh != null) {
						size = i + 1;
						break;
					}
				}

				String[] weekDays = new DateFormatSymbols().getShortWeekdays();
				SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd");
				if (size > 0) {
					for (int i = 0; i < size; i++) {
						if (i > 5) {
							return;
						} else {
							Map<String, Object> item = new HashMap<String, Object>();
							String day = "";
							if (i == 0) {
								Calendar calendar = Calendar.getInstance();
								long longmillis = calendar.getTimeInMillis();
								Date d = new Date(longmillis);
								int day_index = d.getDay();
								day = weekDays[day_index + 1];
							} else {
							    day = weatherInfo.mForecasts.get(i).mDay;
							}

							if (day == null || day.equals("")) {
								try {
									String date = weatherInfo.mForecasts.get(i).mDate;
									Date d = ft.parse(date);
									int day_index = d.getDay();
									day = weekDays[day_index + 1];

								} catch (ParseException e) {
									e.printStackTrace();
								}
							}

							item.put("day", day);
							int icon = WeatherUtils.getWeatherDrawable(context,
									weatherInfo.mForecasts.get(i));
							item.put("icon", icon);
							String range = getTemperature(context,
									weatherInfo.mForecasts.get(i).mLow, unit, false)
									+ "~"
									+ getTemperature(context,
											weatherInfo.mForecasts.get(i).mHigh,
											unit, true);
							item.put("range", range);

							switch (i) {
							case 0:
								updateDayAndDate(context,weatherInfo,view);
							
								break;
							case 1:
								cell_day_1.setText(day);
								cell_icon_1.setImageResource(icon);
								cell_range_1.setText(range);
								break;
							case 2:
								cell_day_2.setText(day);
								cell_icon_2.setImageResource(icon);
								cell_range_2.setText(range);
								break;
							case 3:
								cell_day_3.setText(day);
								cell_icon_3.setImageResource(icon);
								cell_range_3.setText(range);
								break;
							case 4:
								cell_day_4.setText(day);
								cell_icon_4.setImageResource(icon);
								cell_range_4.setText(range);
								break;
							default:
								break;
							}
						}
					}
				}
			}

			tv = null;
		}

		public void setNeedUpdate(boolean bNeed) {
			for (String city : mUpdateStatus.keySet()) {
				mUpdateStatus.put(city, bNeed);
			}
		}

		public void clear() {
			mViews.clear();
			mConditions.clear();
			mUpdateStatus.clear();
		}
		
		public void removeView(String cityId){
			mViews.remove(cityId);
		}

		private String getTemperature(Context context, String c, int unit,
				boolean bUintString) {
			if (c == null || c.equals("")) {
				String u = null;
				if (bUintString) {
					u = (unit == 0) ? context.getString(R.string.centigrade_unit)
							: context.getString(R.string.fahrenheit_unit);
				} else {
					u = "";
				}
				return "--" + u;
				// return "";
			}
			if (unit == 0) {
				return c
						+ (bUintString ? context
								.getString(R.string.centigrade_unit) : "");
			}
			return WeatherUtils.getFahrenheit(c)
					+ (bUintString ? context.getString(R.string.fahrenheit_unit)
							: "");
		}

	    public void updateDayAndDate(Context context,WeatherInfo weatherInfo, View v) {
	    	
	    	if(weatherInfo == null || v == null){
	    		return;
	    	}
	    	
			String timeZone=weatherInfo.mCurrent.mTimezone;
			TimeZone tz = null;	
			
	        if (timeZone == null || timeZone.equals("")) {//sina
	            tz = TimeZone.getDefault();
	        } else {//accu
	            if (timeZone.contains("-")) {
	                tz = TimeZone.getTimeZone("GMT" + timeZone);
	            } else {
	                tz = TimeZone.getTimeZone("GMT+" + timeZone);
	            }
	        }
			Calendar calendar = Calendar.getInstance();
			long longmillis = calendar.getTimeInMillis();
			Date mUpdateDateTime = new Date(longmillis);
			SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateFormat(context);	
			sdf.setTimeZone(tz);
			String date = sdf.format(mUpdateDateTime);

			SimpleDateFormat formatter = new SimpleDateFormat("E");
			formatter.setTimeZone(tz);
			String day = formatter.format(mUpdateDateTime);			
			Log.d(TAG, "updateDayAndDate day="+day+"  date="+date);			
			String time_fomate = day + ", " + date;
			TextView tv = (TextView) v.findViewById(R.id.todaytime);
			tv.setText(time_fomate);	
	    }
	    
	    public void updateMoreWeatherStatus(Context context,WeatherInfo weatherInfo, int pos,View v) {			
	    	if(weatherInfo == null || v == null){
	    		return;
	    	}
	    	
	    	TextView tv = (TextView) v.findViewById(R.id.more_weather_status);
			String date_time = weatherInfo.mInfomation.mRequestTime;
		
			if (null != date_time && !"".equals(date_time)) {
				CharSequence updateTime = "";
				Date updateDateTime = new Date(Long.parseLong(date_time));
				String mF1 = "yyyy-MM-dd HH:mm:ss";				
				SimpleDateFormat myFormatter = new SimpleDateFormat(mF1);
				String mServerTime = myFormatter.format(updateDateTime);
				updateTime = WeatherUtils.getUpdateInterval(mServerTime, mF1);		
				String updateTimeAgo = context.getString(R.string.updatetimeago);
				String update = updateTimeAgo + "  " + updateTime;
				tv.setText(update);
			}
	    }
	}
	
	static MotoAnimationSet showAnimationSet = new MotoAnimationSet(MotoAnimationSet.ANIMATIONSET_ORDERING_TOGETHER);
	static {
		showAnimationSet.addMotoAnimation(new MotoLinearAnimation(MotoObjectAnimation.ANIMATION_ALPHA, 
				MotoObjectAnimation.AFFECT_TYPE_SET, 200, 0f, 255f), true);
	}
	
	private void showSwitchAniamtion() {
		if(showAnimationSet != null) {
			showAnimationSet.start();
			mRenderView.getRenderPlayer().getRootPicture().setAnimationSet(showAnimationSet);
		}
	}
	
}

class WeatherAdapter extends BaseAdapter {
	private Context mContext;
	private WeathersManager mWeathersManager;
	private CityWeatherViewBuilder mCityWeatherViewBuilder;

	public WeatherAdapter(Context c, WeathersManager wm,
			CityWeatherViewBuilder cwvb) {
		Log.d("WeatherWidget", "WeatherAdapter construction");
		mContext = c;
		mWeathersManager = wm;
		mCityWeatherViewBuilder = cwvb;
	}

	public int getCount() {
		int retCount = 0;
		int count = mWeathersManager.getCount(mContext);
		switch (count) {
		case 0:
		case 1:
			retCount = count;
			break;
		default:
			retCount = Integer.MAX_VALUE;
			break;
		}
		return retCount;
	}

	public Object getItem(int position) {
		int count = mWeathersManager.getCount(mContext);
		int realPos = (count == 0) ? 0 : (position % count);
		return mWeathersManager.getWeatherFromCacheByIndex(mContext, realPos);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		int count = mWeathersManager.getCount(mContext);
		if (position < 0) {
			position = position + count;
		}
		int realPos = (count == 0) ? 0 : (position % count);
		View v = mCityWeatherViewBuilder.getView(mContext, mWeathersManager,
				realPos);
		return v;
	}
	
	
}
