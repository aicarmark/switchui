package com.motorola.mmsp.weather.sinaweather.app;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Gallery;

public class MyGallery extends Gallery {

	public MyGallery(Context context) {
		super(context);
	}

	public MyGallery(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MyGallery(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	private boolean isScrollingLeft(MotionEvent e1, MotionEvent e2) {
		return e2.getX() > e1.getX();
	}

	
//	public boolean onTouchEvent (MotionEvent event) {
//	    Log.d("WeatherWidget", "********+++++++********** MyGallery, onTouchEvent begin *******+++++++********");
//	   
//	    Log.d("WeatherWidget", "********+++++++********** MyGallery, onTouchEvent end *******+++++++********");
//	    return super.onTouchEvent(event);
//	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    Log.d("WeatherWidget", "MyGallery, onKeyDown begin");
	    super.onKeyDown(keyCode, event);
	    Log.d("WeatherWidget", "MyGallery, onKeyDown end");
	    return false;
	}
	
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
	    Log.d("WeatherWidget", "MyGallery, onFling begin");
		int kEvent;
		if (isScrollingLeft(e1, e2)) {
			// Check if scrolling left
			kEvent = KeyEvent.KEYCODE_DPAD_LEFT;
		} else {
			// Otherwise scrolling right
			kEvent = KeyEvent.KEYCODE_DPAD_RIGHT;
		}
		onKeyDown(kEvent, null);
		Log.d("WeatherWidget", "MyGallery, onFling end");
		return false;
	}

/*	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		float vx = velocityX;
		if (vx > 200) {
			vx = 200;
		}
		if (vx < -200) {
			vx = -200;
		}
		return super.onFling(e1, e2, vx, velocityY);
	}
	*/
}
