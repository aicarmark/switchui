package com.motorola.mmsp.render;

import android.view.MotionEvent;

public class MotoInteractionEvent {
	public MotionEvent mEvent;
	public String mTag;
	public Object mData;
	
	/**
	 * The offset x compare to previous motion event's x;
	 */
	public float mOffserX;
	
	/**
	 * The offset y compare to previous motion event's y;
	 */
	public float mOffserY;
}
