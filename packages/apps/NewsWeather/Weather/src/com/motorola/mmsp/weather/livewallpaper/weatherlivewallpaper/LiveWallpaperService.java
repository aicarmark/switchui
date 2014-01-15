package com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.service.wallpaper.WallpaperService;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.motorola.mmsp.weather.R;
import com.motorola.mmsp.weather.livewallpaper.utility.Config;
import com.motorola.mmsp.weather.livewallpaper.utility.DevCarbonplay;
import com.motorola.mmsp.weather.livewallpaper.utility.DevCarbonsplash;
import com.motorola.mmsp.weather.livewallpaper.utility.DevIronmax;
import com.motorola.mmsp.weather.livewallpaper.utility.DevIronmaxct;
import com.motorola.mmsp.weather.livewallpaper.utility.DevIronmaxtd;
import com.motorola.mmsp.weather.livewallpaper.utility.DevTinQ;
import com.motorola.mmsp.weather.livewallpaper.utility.DevTinboost;
import com.motorola.mmsp.weather.livewallpaper.utility.Device;
import com.motorola.mmsp.weather.livewallpaper.utility.GlobalDef;
import com.motorola.mmsp.weather.livewallpaper.utility.GlobalDef.TYPE;
import com.motorola.mmsp.weather.livewallpaper.utility.WeatherInfoProvider;
import com.motorola.mmsp.weather.livewallpaper.utility.WeatherInfoProvider.WeatherInfo;



public class LiveWallpaperService extends WallpaperService {
    private static final String   TAG = "LiveWallpaperService";
    public  static final String   SHARED_PREFS_NAME = "livewallpapersettings";
    public  static CubeEngine     mCubeEngine;
    private SystemWeatherReceiver mWeatherReceiver = null;
    //text
    private TextBig    mBigTextDisplay = null;
    private TextSmall  mSmallTextDisplay = null;
    private TextBase   mCurTextDisplay = null;
    private TextView   mCurTextView = null;
    private WeatherInfoProvider mWnp = new WeatherInfoProvider();
    public static SurfaceHolder mSurfaceHolder;
    private TextView tv;
	private int BIGTEXT = 1;
	private int SMALLTEXT = 2;
	private String mLastWeatherCondition = "";
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if( mWeatherReceiver != null ){
            unregisterReceiver(mWeatherReceiver);
            mWeatherReceiver = null;
        }
    }

    @Override
    public Engine onCreateEngine() {
        Log.d(TAG,"==onCreateEngine==");
        mCubeEngine = new CubeEngine(this.getBaseContext());

        //register receiver
        if( mWeatherReceiver == null ){
            Context c = getBaseContext();
            mWeatherReceiver = new SystemWeatherReceiver( c );
            String  acts =  GlobalDef.BROADCAST_TIME_TICK;
            mWeatherReceiver.registerAction(acts);
        }

        return mCubeEngine;
    }

    public class SystemWeatherReceiver extends BroadcastReceiver {  

        private final static String TAG = "LiveWallpaperService";
        private Context mContext = null;

        public SystemWeatherReceiver( Context c ) {  
            Log.d(TAG, "SystemWeatherReceiver construction"); 
            mContext = c;
        }  

        public void registerAction(String  acts){
            IntentFilter filter = new IntentFilter();
                filter.addAction( acts ); 
            //filter.addDataScheme("file");
            mContext.registerReceiver(this, filter);
            Log.d(TAG, "SystemWeatherReceiver registerAction"); 
        }

        public void unregisterAction(){
            mContext.unregisterReceiver(this);
            Log.d(TAG, "SystemWeatherReceiver unregister ");
        }
        @Override
        public void onReceive(Context arg0, Intent intent) {
            String action = intent.getAction();
            if( action == null )
                return;

            Log.d(TAG, "SystemWeatherReceiver onReceive -->"+action );
           if( action.equals(GlobalDef.BROADCAST_TIME_TICK)){
                if(mCubeEngine != null){
                    mCubeEngine.updateCityList();
                    mCubeEngine.onUpdateCityWeather();
                }
            } 
        }
    }

    class CubeEngine extends Engine 
                implements CityAndWeather{

        //configure
        private Device        mDevice = null;
        public ScrDisplayBase mDisplayType = null;
        private Canvas mCanvas = null;
        private Context mContext;
        private SharedPreferences mPreferences;
        private ForeignPackage   mForeignPackage;
        private boolean mVisible;

        private void config(){
            mDevice = createDevice(Config.DEVICE_TYPE);
            //mDevice = createDevice(TYPE.IRONMAX);
            mDisplayType = mDevice.getDisplayType();
        } 

        private Device createDevice(TYPE type){
            //dynamic
            if( TYPE.TINQ == type ){
                Log.d(TAG,"current device is tinq");
                return new DevTinQ();
            }
            else if( TYPE.IRONMAX == type ){
                Log.d(TAG,"current device is ironmax");
                return new DevIronmax();
            }
            else if( TYPE.CARBONPLAY == type ){
                Log.d(TAG,"current device is carbonplay");
                return new DevCarbonplay();
            }
            else if( TYPE.CARBONSPALSH == type ){
                Log.d(TAG,"current device is carbonsplash");
                return new DevCarbonsplash();
            }
            //static
            else if( TYPE.IRONMAXCT == type ){
                Log.d(TAG,"current device is ironmaxct");
                return new DevIronmaxct();
            }
            else if( TYPE.IRONMAXTD == type ){
                Log.d(TAG,"current device is ironmaxtd");
                return new DevIronmaxtd();
            }
            else if( TYPE.TINBOOST == type ){
                Log.d(TAG,"current device is tinboost");
                return new DevTinboost();
            }
            //default
            else {
                Log.d(TAG,"current device is ironmax for default.");
                return new DevIronmax();
            }
        }

        CubeEngine(Context cont) {

            config();

            mContext = cont;	

            //load animation files package
//            try {
//                mForeignPackage = new  ForeignPackage(mContext, GlobalDef.WEATHER_ANIMATION_PACKAGE);
//            } catch (NameNotFoundException e) {
//                e.printStackTrace();
//                Log.e(TAG, "live wallpaper res is not existed.");
//            }

            //register update weather callback from city list layout
            LiveWallpaperSettings.registerCityAndWeather( this );
            LivewallpaperCityList.registerCityAndWeather( this );
        }

        public void onDesiredSizeChanged(int desiredWidth, int desiredHeight) {
            mDisplayType.desiredSizeChanged();
        }

        public void updateCityList(){
            //set the first item defaultly
            if( WeatherInfoProvider.getCurCityName(getBaseContext()).equals("") ){
                if(WeatherInfoProvider.getExitCities(getBaseContext()).isEmpty() == false ){
                    GlobalDef.CityInfo info = WeatherInfoProvider.getExitCities(getBaseContext()).get(0);
                    String str = info.name;
                    if( str.equals("") == false ){
                        WeatherInfoProvider.setCurCity(0, info.id, str, getBaseContext());
                        Log.d(TAG,"default city is "+str);
                    }
                }
            }

            String curCityId = WeatherInfoProvider.getCurCityId(mContext);
            boolean bExit = false;
            ArrayList<GlobalDef.CityInfo> cityInfoList = WeatherInfoProvider.getExitCities(mContext);	
            try{
                for( int i = 0; cityInfoList != null && i < cityInfoList.size(); ++i ){
                    if( cityInfoList.get(i).id.equals(curCityId) ){
                        bExit = true;
                        Log.d(TAG,"current city is "+cityInfoList.get(i).name);
                        break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                return;
            }

            if( bExit == false ){
                WeatherInfoProvider.setCurCity(-1, "", "", mContext);
            }

            //onUpdateCityWeather();
        }
       
     @Override
        public void onMySharedPreferenceChanged(SharedPreferences sp, String key) {
            if(mDisplayType != null && mDisplayType instanceof ScrDisplayAnimation){
                if( tv == null ){
                	tv = getBigTextView(tv);
                }
             //   tv = mDisplayType.getText();
                try{
                    String summary = sp.getString( key, "1" );
                    Log.d(TAG,"summary = "+summary);
                    int value = Integer.parseInt( summary );
                    //display
                    if(key.equals(getResources().getString(R.string.display_key))){
                        switch( value ){
                        case 0: //always
                        	Log.d("jiayy","always see text");
                        	tv.setVisibility(View.VISIBLE);
                            break;
                        case 1: //never
                        	Log.d("jiayy","never see text");
                        	tv.setVisibility(View.GONE);
                            break;
                        default://never	
                        	tv.setVisibility(View.GONE);
                        }
                    }
                    //size
                    else if(key.equals( getResources().getString(R.string.size_key))){
                        int b = tv.getVisibility();
                        Log.d("jiayy","visibilty = "+b);
                        switch( value ){
                        case 0: //big
                        	Log.d("jiayy","big text");
                        	tv = getBigTextView(tv);
                            break;
                        case 1: //small
                        	Log.d("jiayy","small text");
                        	tv = getSmallTextView(tv);
                            break;
                        default://big
                        	tv = getBigTextView(tv);
                            break;
                        }
                        tv.setVisibility(b);
                    }
                }catch( NullPointerException e ){
                    e.printStackTrace();
                }
            
			}else{
            Log.d(TAG,"onSharedPreferenceChanged");
            if( mCurTextDisplay == null ){
                Log.d(TAG,"mCurTextDisplay is null, return");
                return;
            }
				try {
					String summary = sp.getString(key, "0");
					Log.d(TAG, "summary = " + summary);
					int value = Integer.parseInt(summary);
					// display
					if (key.equals(getResources().getString(
							R.string.display_key))) {
						switch (value) {
						case 0: // always
							mCurTextDisplay.setVisiblity(true);
							break;
						case 1: // never
							mCurTextDisplay.setVisiblity(false);
							break;
						default:// never
							mCurTextDisplay.setVisiblity(false);
						}
					}
					// size
					else if (key.equals(getResources().getString(
							R.string.size_key))) {
						boolean b = mCurTextDisplay.getVisiblity();
						switch (value) {
						case 0: // big
							mCurTextDisplay = getBigTextDisplay();
							break;
						case 1: // small
							mCurTextDisplay = getSmallTextDisplay();
							break;
						default:// big
							mCurTextDisplay = getBigTextDisplay();
							break;
						}
						mCurTextDisplay.setVisiblity(b);
					}
				}catch( NullPointerException e ){
                e.printStackTrace();
            }
            }
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xStep, float yStep, int xPixels, int yPixels) {

            mDisplayType.offsetsChanged(xOffset, yOffset, xStep, yStep, xPixels, yPixels);
        }

        private void initPreferences(){
            mPreferences = getSharedPreferences(SHARED_PREFS_NAME, 0);
            onMySharedPreferenceChanged( mPreferences, getResources().getString(R.string.display_key) );
            onMySharedPreferenceChanged( mPreferences, getResources().getString(R.string.size_key) );
        }
        private TextBig getBigTextDisplay(){
            mCanvas = mDisplayType.getCanvas();
            Log.d("jiayy","mCanvas = "+mCanvas);
            if( mBigTextDisplay == null && mCanvas != null ) {
                mBigTextDisplay = new TextBig(mCanvas, mContext, mDevice);
                Log.d(TAG,"create big text seccessfully");
            }
            return mBigTextDisplay;
        }
        TextChooser textChooser = new TextChooser();
        private TextView getBigTextView(TextView tv){
        	android.util.Log.d("jiayy","initBigText begin");
    		tv.setTextColor(Color.WHITE);
    		tv.setTag(BIGTEXT);
    		Log.d("jiayy","tv set big = "+tv);
    		tv.setTypeface(Typeface.DEFAULT_BOLD);
    		return tv;
        }
        private TextView getSmallTextView(TextView tv){
    		tv.setTag(SMALLTEXT);
        	tv.setTextColor(Color.WHITE);
    		return tv;
        }

        private TextSmall getSmallTextDisplay(){
            mCanvas = mDisplayType.getCanvas();
            if( mSmallTextDisplay == null && mCanvas != null ){ 
                mSmallTextDisplay = new TextSmall(mCanvas, mContext, mDevice);
                Log.d(TAG,"create small text seccessfully");
            }

            return mSmallTextDisplay;
        }

        public void updateWeather(boolean isPreview){
            mWnp.updateWeatherInfoFromDB(getBaseContext());
            mDisplayType.updateVideo(isPreview);
        }

        public String getWeatherTypeStr( WeatherInfo info ){
            String strWeather = null;
            if( info.unit == GlobalDef.ACCU_SERVICE ){ // accu
                //set weather type
                int weatherType = info.weatherType;
                if( getAccuConditionsList(R.array.accu_condition_sunny).contains(weatherType) )
                    strWeather = "sunny";
                else if( getAccuConditionsList(R.array.accu_condition_cloudy).contains(weatherType) )
                    strWeather = "cloudy";
                else if( getAccuConditionsList(R.array.accu_condition_thunderstorm).contains(weatherType) )
                    strWeather = "thunderstorm";
                else if( getAccuConditionsList(R.array.accu_condition_rainy).contains(weatherType) )
                    strWeather = "rainy";
                else if( getAccuConditionsList(R.array.accu_condition_foggy).contains(weatherType) )
                    strWeather = "foggy";
                else if( getAccuConditionsList(R.array.accu_condition_snowy).contains(weatherType) )
                    strWeather = "snowy";
                else if( getAccuConditionsList(R.array.accu_condition_sandstorm).contains(weatherType) )
                    strWeather = "sandstorm";
                else
                    strWeather = "sunny";
            }
            else {  //sina
                String strCon = info.condition;
                if( strCon == null || strCon.length() == 0 )
                    strWeather = "sunny";
                else if( getSinaConditionsList(R.array.sina_condition_sunny).contains(strCon) )
                    strWeather = "sunny";
                else if( getSinaConditionsList(R.array.sina_condition_cloudy).contains(strCon) )
                    strWeather = "cloudy";
                else if( getSinaConditionsList(R.array.sina_condition_thunderstorm).contains(strCon) )
                    strWeather = "thunderstorm";
                else if( getSinaConditionsList(R.array.sina_condition_rainy).contains(strCon) )
                    strWeather = "rainy";
                else if( getSinaConditionsList(R.array.sina_condition_foggy).contains(strCon) )
                    strWeather = "foggy";
                else if( getSinaConditionsList(R.array.sina_condition_snowy).contains(strCon) )
                    strWeather = "snowy";
                else if( getSinaConditionsList(R.array.sina_condition_sandstorm).contains(strCon) )
                    strWeather = "sandstorm";
                else
                    strWeather = "sunny";
            }
            return strWeather;
        }

        public List<Integer> getAccuConditionsList( int id ){
            List<Integer> intList = new ArrayList<Integer>();
            int[] conditions = mContext.getResources().getIntArray(id);
            for( int i = 0; i < conditions.length; ++i )
                intList.add(conditions[i]);
            return intList;
        }

        public List<String> getSinaConditionsList( int id ){
            List<String> strList = new ArrayList<String>();
            String[] conditions = mContext.getResources().getStringArray(id);
            for( int i = 0; i < conditions.length; ++i )
                strList.add(conditions[i]);
            return strList;
        }
        public TextView getText(){
    		return tv;
    	}
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

    		tv = new TextView(mContext);
    		tv.setBackgroundColor(Color.TRANSPARENT);
    		tv.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
            // By default we don't get touch events, so enable them.
            setTouchEventsEnabled(true);
            surfaceHolder.setFormat(PixelFormat.RGBA_8888);
            Log.d("jyy", "mDisplayType = " + mDisplayType + ", type = " + (mDisplayType instanceof ScrDisplayAnimation));
			if(mDisplayType != null && mDisplayType instanceof ScrDisplayAnimation){
				((ScrDisplayAnimation)mDisplayType).setSurfaceHolder(surfaceHolder);
			}
            Log.d(TAG, "onCreate SurfaceHolder" );
            Log.d("jyy", "onCreate SurfaceHolder = "+mSurfaceHolder );
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            //mDisplayType.unInit();
         //   if( isPreview() == false )
            mDisplayType.unInit();

            Log.d(TAG, "CubeEngine onDestroy" );
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
        	 Log.d(TAG, "visible = "+visible);
        	 Log.d(TAG, "mVisible = "+mVisible);
        	 //modify by 002777 for switchui-1980 2012-7-11
//            if( mVisible == visible )
//                return;
        	//end modify by 002777 for switchui-1980 2012-7-11
            mDisplayType.setParams(mWnp, mContext, mDevice, mForeignPackage, this);
            mVisible = visible;
            mDisplayType.setVisible(mVisible);
            
            if(mDisplayType != null && mDisplayType instanceof ScrDisplayAnimation){
				((ScrDisplayAnimation)mDisplayType).setSurfaceVisible(mVisible);
			}
            
            if (mVisible) {
                Log.d(TAG, "==visible==" );
              //modify by 002777 for switchui-1980 2012-7-11
                LiveWallpaperService.mCubeEngine = this;
              //end modify by 002777 for switchui-1980 2012-7-11
//              mDisplayType.resetFilePath();
                updateCityList();
                Log.d("jyy","onVisibilityChanged.start() begin");
                mDisplayType.start(mCubeEngine.isPreview());
            } else {
                Log.d(TAG, "==unvisible==" );
                //mDisplayType.unRegisterTimer();
               mDisplayType.pause();
            }
            super.onVisibilityChanged(visible);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format,
                int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            Log.d(TAG, "onSurfaceChanged" );
            mDisplayType.setParams(mWnp, mContext, mDevice, mForeignPackage, this);
         //   mDisplayType.start();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            Log.d(TAG, "onSurfaceDestroyed" );
        }

        public void unint(){
            mVisible = false;
            mDisplayType.setVisible(mVisible);
            mDisplayType.unInit();
        }

		public void updateWeatherTextInfo(Canvas canvas) {
			Log.d(TAG, "updateWeatherTextInfo");
			if (mCurTextDisplay == null) {
				mCurTextDisplay = getBigTextDisplay();
				initPreferences();
			} else {
				mCurTextDisplay.updateCanvas(mDisplayType.getCanvas());
			}

			if (mForeignPackage == null) {
				mCurTextDisplay.drawTipText(getString(R.string.no_res),
						canvas.getWidth(), canvas.getHeight());
				Log.d(TAG, "mForeignPackage == null");
				return;
			}

			WeatherInfo info = mWnp.getWeatherInfo();

			// city
			String city = info.city;
			if (city == null || city.length() == 0) {
				// mCurTextDisplay.drawTipText(
				// getString(R.string.add_city_tip), canvas.getWidth(),
				// canvas.getHeight() );
				Log.d(TAG, "there is no city.");
				return;
			}
			mCurTextDisplay.drawText(0, city, TextBase.BIG);

			// condition and temperature
			String str = "";
			if (info.condition != null)
				str += info.condition;
			if (info.temperature != null) {
				str += " ";
				str += info.temperature;
			}
			// Log.d(TAG,str);
			if (str.equals("") || str.equals(" ") || str.equals(" -"))
				str = getString(R.string.no_weather_info);
			mCurTextDisplay.drawText(1, str, TextBase.SMALL);
		}

		public void updateWeatherTextInfo(TextView tv) {
			tv.setText("");
			initPreferences();
			if (tv == null) {
				Log.d("jiayy", "tv == null");
			}

			WeatherInfo info = mWnp.getWeatherInfo();
			// city
			String city = info.city;
			if (city == null || city.length() == 0) {
				// mCurTextDisplay.drawTipText(
				// getString(R.string.add_city_tip), canvas.getWidth(),
				// canvas.getHeight() );
				Log.d(TAG, "there is no city.");
				return;
			}
			// condition and temperature
			String str = "";
			if (info.condition != null)
				str += info.condition;
			if (info.temperature != null) {
				str += " ";
				str += info.temperature;
			}
			// Log.d(TAG,str);
			if (str.equals("") || str.equals(" ") || str.equals(" -"))
				str = getString(R.string.no_weather_info);
			int cityLen = city.length();
			int strLen = str.length();
			if (cityLen > 6) {
				city = city.substring(0,4) + "...";
				cityLen = city.length();
			}
			if(strLen > 10) {
				str = str.substring(0,9) + "...";
			}
			int len = city.length() + str.length();
			SpannableString weatherInfo = new SpannableString(city + "\n" + str);
			if ((Integer) tv.getTag() == BIGTEXT) {
				weatherInfo.setSpan(new AbsoluteSizeSpan(
						mDevice.BIG_BIG_TEXT_SIZE), 0, cityLen,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				weatherInfo.setSpan(new AbsoluteSizeSpan(
						mDevice.BIG_SMALL_TEXT_SIZE), cityLen, len+1,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else if ((Integer) tv.getTag() == SMALLTEXT) {
				weatherInfo.setSpan(new AbsoluteSizeSpan(
						mDevice.SMALL_BIG_TEXT_SIZE), 0, cityLen,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				weatherInfo.setSpan(new AbsoluteSizeSpan(
						mDevice.SMALL_SMALL_TEXT_SIZE), cityLen, len+1,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (tv != null) {
				tv.setText(weatherInfo);
			}
		}

        @Override
        public String onUpdateCityWeather() {
            if( mWnp != null ){
                Log.d(TAG,"onUpdateCityWeather");
                mWnp.updateWeatherInfoFromDB(getBaseContext());
                WeatherInfo info = mWnp.getWeatherInfo();
                mDisplayType.updateWeatherNewsProvider( mWnp );
                if( mCurTextDisplay != null && mCanvas != null){
                    mCurTextDisplay.updateCanvas(mCanvas);
                }
                Log.d(TAG, "info is " + info.condition);
                if(!mLastWeatherCondition.equals(info.condition)) {
                	Log.d(TAG, "condition changed, Weather animation need change");
                	mLastWeatherCondition = info.condition;
                	// mDisplayType.updateVideo();
                } else {
                	Log.d(TAG, "condition not change, no need change weather animation");
                }
                mDisplayType.updateVideo(mCubeEngine.isPreview());
                return info.city;
            }

            return null;
        }

        public void configurationChanged(){

            int x = mDevice.HD_OFFSET_X;
            int y = mDevice.HD_OFFSET_Y;
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                x = mDevice.LD_OFFSET_X;
                y = mDevice.LD_OFFSET_Y;
            }
            Log.d(TAG,"x = "+x+", y = "+y);

            TextBase.setOffset(x, y);
        }

        @Override
        public Device getCurDevice() {
            return mDevice;
        }

		@Override
		public String onUpdateCityWeatherData() {
			 if( mWnp != null ){
	                Log.d(TAG,"onUpdateCityWeather");
	                mWnp.updateWeatherInfoFromDB(getBaseContext());
	                WeatherInfo info = mWnp.getWeatherInfo();
	                mDisplayType.updateWeatherNewsProvider( mWnp );
	                if( mCurTextDisplay != null && mCanvas != null){
	                    mCurTextDisplay.updateCanvas(mCanvas);
	                }
	                Log.d(TAG, "info is " + info.condition);
	                if(!mLastWeatherCondition.equals(info.condition)) {
	                	Log.d(TAG, "condition changed, Weather animation need change");
	                	mLastWeatherCondition = info.condition;
	                	// mDisplayType.updateVideo();
	                } else {
	                	Log.d(TAG, "condition not change, no need change weather animation");
	                }
	                return info.city;
	            }

	            return null;
		}
    }

    @Override
    public void onConfigurationChanged(Configuration paramConfiguration){
        Log.d(TAG,"onConfigurationChanged");
    }
}
