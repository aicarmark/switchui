package com.motorola.mmsp.weather.locationwidget.small;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.motorola.mmsp.plugin.base.PluginHost.PluginHostCallback;
import com.motorola.mmsp.plugin.widget.PluginWidgetDragListener;
import com.motorola.mmsp.plugin.widget.PluginWidgetHostView;
import com.motorola.mmsp.plugin.widget.PluginWidgetModel;
import com.motorola.mmsp.plugin.widget.PluginWidgetModelListener;
import com.motorola.mmsp.plugin.widget.PluginWidgetScrollListener;
import com.motorola.mmsp.plugin.widget.PluginWidgetStatusListener;
import com.motorola.mmsp.render.MotoAnimation;
import com.motorola.mmsp.render.MotoAnimationListener;
import com.motorola.mmsp.render.MotoAnimationSet;
import com.motorola.mmsp.render.MotoPicture;
import com.motorola.mmsp.render.MotoPictureGroup;
import com.motorola.mmsp.render.MotoRenderPlayer;
import com.motorola.mmsp.render.MotoRenderView;
import com.motorola.mmsp.weather.R;
import com.motorola.mmsp.weather.locationwidget.small.WeatherManager.WeatherInfo;
import com.motorola.mmsp.weather.sinaweather.app.DatabaseHelper;
import com.motorola.mmsp.weather.sinaweather.app.PersistenceData;
import com.motorola.mmsp.weather.sinaweather.service.WeatherServiceConstant;

public class WeatherWidgetLayout extends RelativeLayout implements PluginWidgetDragListener
, PluginWidgetScrollListener, PluginWidgetStatusListener, PluginWidgetModelListener, View.OnClickListener
{
	private Context mPackageContext;
	
	private Context mContext;
	
	public TextView mTemperatureTextView;
	public TextView mCityNameTextView;
	
	public TextView mDateTextView;
	public TextView mTimeTextView;
	private TextView mNoCityText;
	private TextView mNoWeatherText;
	
	private MotoRenderView mRenderView;
	private LinearLayout mRenderLinearLayout;
	private TextView mLoadingText;
	
	private RelativeLayout mWeatherInfoLayout;
	
	private SharedPreferences mPrefs;
	
	private PluginHostCallback hostCallback;
	private WeatherInfo mWeatherInfo;
	private boolean mIsNight;
	
	private static final int TAG_WEATHER_ADD_CITY = 0;
	
	private boolean isInterrupt = false;
	
	private String selections = "settingName = ?";
	private String[] selectionargs = new String[] { "clearData" };
	
	private String selection2 = "widget_id=?";
	
	public WeatherWidgetLayout(Context packageContext, Context viewContext)
	{
		super(viewContext);		
		mPackageContext = packageContext;
		mContext = viewContext;
		mPrefs = mPackageContext.getSharedPreferences(GlobalDef.CITYIDSHAREDPFE, Context.MODE_PRIVATE);
		
		Resources res = mPackageContext.getResources();
		
		inflate(packageContext, R.layout.widget_useplugin_4_1, this);	
		
		initWeatherConditionString(packageContext);
		
		mRenderView = new MotoRenderView(mPackageContext);	
		
		mRenderLinearLayout = (LinearLayout)findViewById(R.id.weather_animation_layout);
		mRenderLinearLayout.addView(mRenderView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		
		mCityNameTextView = (TextView) findViewById(R.id.city_name);
		mTemperatureTextView = (TextView) findViewById(R.id.temperature);
		mDateTextView = (TextView) findViewById(R.id.date);
		
		mTimeTextView = (TextView) findViewById(R.id.time);		
		mNoCityText = (TextView) findViewById(R.id.no_city);
		mNoWeatherText = (TextView) findViewById(R.id.no_weather_info);
				
		mWeatherInfoLayout = (RelativeLayout) findViewById(R.id.weather);
		mWeatherInfoLayout.setTag(TAG_WEATHER_ADD_CITY);
		mLoadingText = (TextView) findViewById(R.id.loading);
		
		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		mIsNight = isNight(hour);	
	}
	
	@Override
	public void finishLoading()
	{

	}

	@Override
	public void onModelChanged(PluginWidgetModel model)
	{
		WeatherWidgetModel weatherModel = ((WeatherWidgetModel)model);
		weatherModel.update(this);		
	}

	@Override
	public void startLoading()
	{
		
	}

	@Override
	public void onDestroy()
	{
		if(mRenderView != null)
		{
			mRenderView.getPlayer().release();
		}
	}

	@Override
	public void onHide()
	{
		isInterrupt = true;
		seekToEnd();
	}

	@Override
	public void onPause()
	{
		isInterrupt = true;
		seekToEnd();
		insertClearDataFlag();
	}

	@Override
	public void onResume()
	{
		isInterrupt = false;
		
		updateTime();
		readClearDataFlag();
	}

	@Override
	public void onShow()
	{
		isInterrupt = false;
	}

	@Override
	public void finishScrolling(boolean isDefault)
	{
		isInterrupt = false;
	}


	@Override
	public void startScrolling(boolean isDefault)
	{
		isInterrupt = true;
		seekToEnd();
	}
	
	@Override
	public void onDragStart()
	{
		isInterrupt = true;
		seekToEnd();
	}

	@Override
	public void onDragEnd()
	{
		isInterrupt = false;
	}

	@Override
	public void onDragMoving()
	{

	}

	public void release()
	{
		if(mRenderView != null)
		{
			mRenderView.release();
		}
		hostCallback = null;
	}
	
	private String[][] WEAHTER_CONDITION  = null;	
	private static int[] weatherAnimDay = new int[] { 
		R.xml.widget_weather_sunny,
		R.xml.widget_weather_cloudy,
		R.xml.widget_weather_rain,
		R.xml.widget_weather_snow,
		R.xml.widget_weather_fog,
		R.xml.widget_weather_sand_storm,
		R.xml.widget_weather_rain,
	};
	private static int[] weatherAnimNight = new int[]{
		R.xml.widget_weather_sunny_night,
		R.xml.widget_weather_cloudy_night,
	};
	
	public void initWeatherConditionString(Context context) {
		if (WEAHTER_CONDITION == null) {
			String[] sunny = context.getResources().getStringArray(R.array.sunny);
			String[] cloudy = context.getResources().getStringArray(R.array.cloudy);
			String[] rain = context.getResources().getStringArray(R.array.rain);
			String[] snow = context.getResources().getStringArray(R.array.snow);
			String[] fog = context.getResources().getStringArray(R.array.fog);
			String[] sand_storm = context.getResources().getStringArray(R.array.sand_storm);
			String[] thunder = context.getResources().getStringArray(R.array.thunder);
			WEAHTER_CONDITION = new String[][]{sunny, cloudy, rain, snow, fog, sand_storm, thunder};
		}
	}
	
	 private int getResourceId(String weatherCondition, boolean day_night)
     {
     	for (int i = 0; i < WEAHTER_CONDITION.length; i++)
			{
				for (int j = 0; j < WEAHTER_CONDITION[i].length; j++)
				{
					if(WEAHTER_CONDITION[i][j].equals(weatherCondition))
					{
						if(day_night && (i == 0 || i == 1))
						{
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
	
	public void showNoWeatherInfo(String cityName, final String cityId, int widgetId)
	{
		mWeatherInfo = null;
		
		mNoWeatherText.setVisibility(View.VISIBLE);
		mNoCityText.setVisibility(View.GONE);
		mLoadingText.setVisibility(View.INVISIBLE);
		
		mTemperatureTextView.setVisibility(View.GONE);
		mCityNameTextView.setVisibility(View.VISIBLE);
		
		mCityNameTextView.setText(cityName);
		mRenderView.setPlayerRes(mPackageContext.getResources(), R.xml.widget_weather_sunny);
		displayAnimation(widgetId);
		mWeatherInfoLayout.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(
						"motorola.weather.action.MoreWeatherActivity");
				int widgetId = ((PluginWidgetHostView) getParent())
						.getPluginWidgetHostId();
				intent.putExtra("widgetId", widgetId);
				intent.putExtra("cityId", cityId);

				mContext.startActivity(intent);
			}
		});
	}
	
	public void showWeatherInfo(WeatherInfo weatherInfo, final String cityId, int widgetId)
	{
		if(weatherInfo.mState != WeatherManager.WEATHER_STATE_NORMAL)
		{
			showNoWeatherInfo(weatherInfo.mCityName, cityId, widgetId);
			mTimeTextView.setVisibility(View.VISIBLE);
			mDateTextView.setVisibility(View.VISIBLE);
			updateTime();
			return;
		}
		
		this.mWeatherInfo = weatherInfo;
		mNoCityText.setVisibility(View.GONE);
		mNoWeatherText.setVisibility(View.GONE);
		mLoadingText.setVisibility(View.INVISIBLE);
		
		mDateTextView.setVisibility(View.VISIBLE);
		mTemperatureTextView.setVisibility(View.VISIBLE);
		mCityNameTextView.setVisibility(View.VISIBLE);		
		mTimeTextView.setVisibility(View.VISIBLE);

		mRenderLinearLayout.setVisibility(View.VISIBLE);
		
		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		boolean isNight = isNight(hour);
		int resId = getResourceId(weatherInfo.mWeather, isNight);
		mRenderView.setPlayerRes(mPackageContext.getResources(), resId);
		displayAnimation(widgetId);
		
		mCityNameTextView.setText(weatherInfo.mCityName);		
		updateTemperature(weatherInfo, cityId);		
		updateTime();

		mWeatherInfoLayout.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(
						"motorola.weather.action.MoreWeatherActivity");
				int widgetId = ((PluginWidgetHostView) getParent())
						.getPluginWidgetHostId();
				intent.putExtra("widgetId", widgetId);
				intent.putExtra("cityId", cityId);

				mContext.startActivity(intent);
			}
		});

	}
	
	public void showWeatherNoCity(int widgetId)
	{
		mWeatherInfo = null;
		
		mTemperatureTextView.setVisibility(View.GONE);
		mCityNameTextView.setVisibility(View.GONE);
		mLoadingText.setVisibility(View.GONE);
		
		mDateTextView.setVisibility(View.VISIBLE);
		mTimeTextView.setVisibility(View.VISIBLE);

		mNoCityText.setVisibility(View.VISIBLE);
		mNoWeatherText.setVisibility(View.GONE);
		mWeatherInfoLayout.setOnClickListener(this);
		
		updateTime();
		
		mRenderView.setPlayerRes(mPackageContext.getResources(), R.xml.widget_weather_sunny);
		displayAnimation(widgetId);
	}
	
	private void displayAnimation(int widgetId)
	{
		boolean isCurrentScreen = (hostCallback == null ? false : hostCallback.isCurrentPanel(widgetId));
		
		Log.i("animation", "hostCallback = " + (hostCallback == null));
		Log.i("animation", "isCurrentScreen = " + isCurrentScreen);
		Log.i("animation", "isInterrupt = " + isInterrupt);
		if(isCurrentScreen && !isInterrupt)
		{
			Log.i("animation", "displayAnimation play " + widgetId);
			mRenderView.play();
			
//			MotoPictureGroup rootPicture = (MotoPictureGroup)mRenderView.getPlayer().getRootPicture();			
//			int count = rootPicture.getChildCount();	
//			MotoPicture last = rootPicture.getChildAt(count - 1);
//			last.getAnimationSet().setListener(new MotoAnimationListener()
//			{
//				@Override
//				public void onAnimationStop(MotoAnimation arg0)
//				{
//					mRenderView.getPlayer().pause();
//					Log.d("animation", "animationSet stop");
//				}
//				
//				@Override
//				public void onAnimationStart(MotoAnimation arg0)
//				{
//					
//				}
//				
//				@Override
//				public void onAnimationSeek(MotoAnimation arg0, float arg1)
//				{
//					
//				}
//				
//				@Override
//				public void onAnimationResume(MotoAnimation arg0)
//				{
//					
//				}
//				
//				@Override
//				public void onAnimationRepeat(MotoAnimation arg0)
//				{
//					
//				}
//				
//				@Override
//				public void onAnimationPause(MotoAnimation arg0)
//				{
//					
//				}
//				
//				@Override
//				public void onAnimationEnd(MotoAnimation arg0)
//				{
//					
//				}
//				
//				@Override
//				public void onAnimationCancel(MotoAnimation arg0)
//				{
//					
//				}
//			});
		} else{
			Log.i("animation", "displayAnimation play seekToEnd" + widgetId);
			seekToEnd();
		}
	}
	
	public void showWeatherLoading()
	{
		mLoadingText.setVisibility(View.VISIBLE);
		
		mTemperatureTextView.setVisibility(View.INVISIBLE);
		mCityNameTextView.setVisibility(View.INVISIBLE);
		mDateTextView.setVisibility(View.INVISIBLE);
		mTimeTextView.setVisibility(View.INVISIBLE);
		
		mNoWeatherText.setVisibility(View.GONE);
		mNoCityText.setVisibility(View.INVISIBLE);
	}
	
	public void showWeatherAnimation(int widgetId)
	{
		boolean isCurrentScreen = hostCallback == null ? false : hostCallback.isCurrentPanel(widgetId);
		if(isCurrentScreen && !isInterrupt)
		{
			Log.i("animation", " unlock Animation play " + widgetId);
			if(mRenderLinearLayout.getChildCount() != 0)
			{
				MotoRenderPlayer player = mRenderView.getPlayer();
				MotoPictureGroup rootPicture = (MotoPictureGroup)player.getRootPicture();
				
				int count = rootPicture.getChildCount();			
				for (int i = 0; i < count; i++)
				{
					MotoPicture picture = rootPicture.getChildAt(i);
					MotoAnimationSet animation = picture.getAnimationSet();
					if(animation != null)
					{
						animation.seekToProgress(0f);
						animation.start();
					}				
				}
				player.play();			
			}
		} else
		{
			Log.i("animation", " unlock Animation not play " + widgetId);
		}
	}
	
	@Override
	public void scrolledY(float scrollProgress, boolean isDefault)
	{
		
	}

	public void updateTime()
	{

		Calendar calendar = Calendar.getInstance();
		Date now = calendar.getTime();

		ContentResolver cr = mPackageContext.getContentResolver();
		String timeFormat = Settings.System.getString(cr, Settings.System.TIME_12_24);
		
		SimpleDateFormat dateFormater = (SimpleDateFormat)DateFormat.getDateFormat(mPackageContext);
		String date = dateFormater.format(now);
		mDateTextView.setText(date);
		
		String time = null;
		SimpleDateFormat timeFormater = null;
		if ("24".equals(timeFormat))
		{
			timeFormater = new SimpleDateFormat("HH:mm");
			time = timeFormater.format(now);
		} else
		{
			timeFormater = new SimpleDateFormat("h:mma");
			time = timeFormater.format(now);
			
		}
		mTimeTextView.setText(time);
		
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		boolean isNight = isNight(hour);
		
		if(isNight != mIsNight)
		{
			mIsNight = isNight;
			if(mWeatherInfo != null)
			{
				int resId = getResourceId(mWeatherInfo.mWeather, mIsNight);
				mRenderView.setPlayerRes(mPackageContext.getResources(), resId);
				seekToEnd();
			}
		}
	
	}
	
	private boolean isNight(int hour)
	{
		return ((hour >= 6 && hour < 18)?false:true);
	}

	public void updateTemperature(WeatherInfo weatherInfo, final String cityId)
	{
		String temperature = weatherInfo.mTemperature;
		if(TextUtils.isEmpty(temperature))
		{
			mTemperatureTextView.setText("");
			return;
		}
		//default as centigrade
		//modify by 002777 for switchui-1910
//		int temperatureUnit = mPrefs.getInt(GlobalDef.TEMPUNIT, 0);
		String units = DatabaseHelper.readSetting(mContext,
				PersistenceData.SETTING_TEMP_UNIT);
		if (units.equals("")) {
			units = "0";
		}
		int temperatureUnit = Integer.parseInt(units);
		//end modify by 002777 for switchui-1910
		Log.d("Weather", "temperatureUnit=" + temperatureUnit + ",temperature=" + temperature);
		if (temperatureUnit == 0)
		{
			mTemperatureTextView.setText(temperature + mPackageContext.getString(
					R.string.centigrade_unit));
//			mTemperatureTextView.setText(mPackageContext.getString(
//					R.string.format_centigrade_unit, weatherInfo.mTemperature));			
		} else if (temperatureUnit == 1)
		{
			mTemperatureTextView.setText(temperature + mPackageContext.getString(
					R.string.fahrenheit_unit));
			
//			mTemperatureTextView.setText(mPackageContext.getString(
//					R.string.format_fahrenheit_unit, weatherInfo.mTemperature));
		}
	}

	@Override
	public void pluginAnimation(int arg0, int arg1)
	{
				
	}

	@Override
	public void onClick(View v)
	{
		int tag = (Integer) v.getTag();
		int widgetId = ((PluginWidgetHostView) getParent()).getPluginWidgetHostId();
		switch (tag)
		{
		case TAG_WEATHER_ADD_CITY:
			Intent intent = new Intent(
					"motorola.weather.action.CitiesActivity");
			intent.putExtra("widgetId",Integer.toString(widgetId));
			
			mContext.startActivity(intent);		
			break;
		}
	}
	
	@Override
	public void scrolled(float arg0, int arg1, boolean arg2)
	{
				
	}

	@Override
	public void createViewCompleted(PluginHostCallback hostCallback, int widgetId) {
		Log.d("animation", "createViewCompleted, widgetId is " + widgetId);
		this.hostCallback = hostCallback;
		
		WeatherManager weatherManager = WeatherManager.getInstace(mPackageContext);;
		weatherManager.setWeatherServiceEdition(mPackageContext);
		String cityId = Utils.getCityIdByWidgetId(mPackageContext, widgetId);
		Log.i("Weather","Utils.getCityIdByWidgetId = " + cityId);
		
		if (cityId == null)
		{
			Cursor allCitys = null;
			Cursor widgetCity = null;
			try {
				allCitys = GlobalDef.query(mPackageContext, GlobalDef.uri_citylist,
						null, null, null);
				
				if (allCitys != null && allCitys.getCount() != 0)
				{
					String lastCityId = Utils.getLastWidgetCityId(mPackageContext);
					Log.i("Weather","Utils.getLastWidgetCityId = " + lastCityId);
					
					cityId = Utils.getNextCityId(allCitys, lastCityId);				
					Log.i("Weather","Utils.getNextCityId = " + cityId);				
				}
				
				ContentResolver resolver = mPackageContext.getContentResolver();

				String selection = "widget_id=?";
				String[] selectionArgs = new String[]{String.valueOf(widgetId)};
				widgetCity = resolver.query(GlobalDef.uri_widgetCity, null, selection, selectionArgs, null);
				
				if(widgetCity != null && widgetCity.getCount() == 0){
					ContentValues values = new ContentValues();
					values.put("widget_id", widgetId);
					values.put("city_id", cityId);					
					resolver.insert(GlobalDef.uri_widgetCity, values);
				}
				
			}
			catch(Exception e){
				e.printStackTrace();
			}
			finally{
				if (allCitys != null){
					allCitys.close();
				}
				if(widgetCity != null){
					widgetCity.close();
				}
			}

		}
		Log.d("animation", "show weather info start");
		if(cityId != null)
		{
			WeatherInfo info = weatherManager.getInfoWithCityId(cityId, mPrefs);
			if (info != null){
				showWeatherInfo(info, cityId, widgetId);
			}
		} else{
			showWeatherNoCity(widgetId);
		}
		Log.d("animation", "show weather info end, createViewCompleted finished");
	}
	
	private void seekToEnd() {
		if(mRenderView != null
				&& mRenderView.getStatus() != MotoRenderPlayer.PLAYER_PAUSE)
		{
			mRenderView.pause();
			MotoPictureGroup root = (MotoPictureGroup)mRenderView.getPlayer().getRootPicture();
			if(root != null)
			{
				ArrayList<MotoPicture> children = root.getChildren();
				for (MotoPicture pic : children) {
					pic.seekAnimationTo(1f);
				}
				mRenderView.getPlayer().prepareNextFrame();
			}			
		}
	}
	
	/**
	 * modified for SwitchUI-2456, insert a flag when using the widget first.
	 */
	private void insertClearDataFlag(){
		Log.d("Weather", "insertClearDataFlag begin");

		Cursor cursor = null;
		try{
			ContentResolver resolver = mPackageContext.getContentResolver();
			cursor = resolver.query(GlobalDef.uri_setting,null,
					selections, selectionargs, null);
			if (cursor == null || cursor.getCount() == 0){
				Log.d("Weather", "insertClearDataFlag insert");
				ContentValues values = new ContentValues();
				values.put("settingName", "clearData");
				values.put("settingValue", "hasData");					
				resolver.insert(GlobalDef.uri_setting, values);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			if (cursor != null){
				cursor.close();
			}
		}
		
		Log.d("Weather", "insertClearDataFlag end");
	}
	
	/**
	 * modified for SwitchUI-2456
	 * read the flag from db.after clearing data this flag cann't be read.
	 */
	private void readClearDataFlag(){
		Log.d("Weather", "readClearDataFlag begin");
		Cursor cursor = null;
		Cursor cursor2 = null;
		try {
			ContentResolver resolver = mPackageContext.getContentResolver();
			cursor = resolver.query(GlobalDef.uri_setting,null,
					selections, selectionargs, null);
			if (cursor == null || cursor.getCount() == 0){
				int widgetId = ((PluginWidgetHostView) getParent()).getPluginWidgetHostId();				
				
				String[] selectionArgs = new String[]{String.valueOf(widgetId)};
				cursor2 = resolver.query(GlobalDef.uri_widgetCity, null,selection2, selectionArgs, null);
				if(cursor2 == null || cursor2.getCount() == 0){
					Log.d("Weather", "insertClearDataFlag update self");
					showWeatherNoCity(widgetId);
					
					ContentValues values = new ContentValues();
					String cityId = null;
					values.put("widget_id", widgetId);
					values.put("city_id", cityId);						
					resolver.insert(GlobalDef.uri_widgetCity, values);
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally {
			if (cursor != null){
				cursor.close();
			}
			if (cursor2 != null){
				cursor2.close();
			}
		}
		Log.d("Weather", "readClearDataFlag end");
	}

}
