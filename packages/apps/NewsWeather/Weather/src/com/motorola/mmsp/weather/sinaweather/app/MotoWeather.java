package com.motorola.mmsp.weather.sinaweather.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnTouchListener;
import android.widget.ViewFlipper;

public class MotoWeather extends Activity implements OnGestureListener  ,OnTouchListener{
    /** Called when the activity is first created. */
	ViewFlipper mViewFlipper = null;
	AnimationDrawable mRocketAnimation;
	GestureDetector mGestureDetector;
    //@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //setContentView(R.layout.top_widget);
        //setContentView(R.layout.main);
        
        //Intent intent = new Intent(this, WeatherSettingActivity.class);
        //startActivity(intent);
        
        //setContentView(R.layout.weather_setting_item);
        
        //Intent intent = new Intent(UpdateService.ACTION_WEATHER_UPDATING);
		//intent.setClass(this, UpdateService.class);
		//startService(intent);
		
		/*
		ImageView rocketImage = (ImageView)findViewById(R.id.top_widget_bg);
		rocketImage.setBackgroundResource(R.anim.anima);
		rocketAnimation = (AnimationDrawable) rocketImage.getBackground(); 
		*/
		//CityLocation.locateCity(this);
		
		//WeatherUtils.searchCity("bj");
        
        //TextView t = new TextView(this);
        //t.setText("13123123123123213133");
        //((Gallery)findViewById(R.id.gallery)).addView(t);
        //((Gallery)findViewById(R.id.gallery)).addView(t);
        //((Gallery)findViewById(R.id.gallery)).addView(t);
        //((Gallery)findViewById(R.id.gallery)).addView(t);
        /*
        TextView t = new TextView(this);
        t.setText("13123123123123213133");
        setContentView(R.layout.view_flipper);
        vf = (ViewFlipper)findViewById(R.id.city_weather_flipper);
        vf.addView(t);
        t = new TextView(this);
        t.setText("asfasfasfafasdfaa");
        vf.addView(t);
        
        mGestureDetector = new GestureDetector(this); 
        vf.setOnTouchListener(this);
        vf.setLongClickable(true);
        */
        Intent intent = new Intent(this, MoreWeatherActivity.class);
        this.startActivity(intent);
        
        /*
        setContentView(R.layout.main);
        new CityLocation(this).locateCity(this, new OnCityLocateListener() {
			//@Override
			public void hanlerCity(String city) {
				((TextView)findViewById(R.id.moto_weather_text)).setText(city);
				Log.e("MotoWeahter.Activity", "Location city = " + city);
			}
		});
		*/
        //Log.e("MotoWeahter.Activity", "Location city = " + city);
    }
    
    public void next(View v) {
    	mViewFlipper.showNext();
    }
    
    //@Override
    protected void onStart() {
    	//rocketAnimation.start();
    	super.onStart();
    }   
    

	//@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	//@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if(e1.getX() > e2.getX()) {//move to left   
	           mViewFlipper.startFlipping();  
	       }else if(e1.getX() < e2.getX()) {
	    	   mViewFlipper.showPrevious();
	       }
		return false;
	}

	//@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	//@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	//@Override
	public boolean onTouch(View v, MotionEvent event) {
		return mGestureDetector.onTouchEvent(event); 
	}
    
}



