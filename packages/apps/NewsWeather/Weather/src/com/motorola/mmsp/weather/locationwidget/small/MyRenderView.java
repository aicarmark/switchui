package com.motorola.mmsp.weather.locationwidget.small;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.motorola.mmsp.render.MotoRenderView;

public class MyRenderView extends MotoRenderView{
	public MyRenderView(Context context) {
		super(context);
	}
	
	
	public MyRenderView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}



	public MyRenderView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}



	public MyRenderView(Context context, int resId)
	{
		super(context, resId);
	}


	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (getPlayer() != null) {
			getPlayer().pause();
		}
	}
	
	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		if (getPlayer() != null) {
			getPlayer().pause();
		}
	}
}
