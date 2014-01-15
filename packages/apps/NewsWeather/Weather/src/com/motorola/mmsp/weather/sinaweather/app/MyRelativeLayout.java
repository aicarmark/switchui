package com.motorola.mmsp.weather.sinaweather.app;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class MyRelativeLayout extends RelativeLayout {
	public MyRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	public MyRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public MyRelativeLayout(Context context) {
		super(context);
	}
	
	/**
	 * 重写ViewGroup的dispatchSetPressed方法，在点击layout时不让子节点获得press事件
	 */
	@Override
	protected void dispatchSetPressed(boolean pressed) {
		;
	}
	
}
