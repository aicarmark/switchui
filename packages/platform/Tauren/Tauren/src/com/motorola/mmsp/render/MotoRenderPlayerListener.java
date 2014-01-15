package com.motorola.mmsp.render;

import android.graphics.Canvas;
import android.graphics.Rect;

import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;

/**
 * listener
 */
public interface MotoRenderPlayerListener {
	
	void onBeforePrepareFrame();
	
	void onAfterPrepareFrame(Rect dirty);
	
	/**
	 * It's not available in GL render player
	 */
	void onBeforeDraw(Canvas canvas, Rect dirty);
	
	/**
	 * It's not available in GL render player
	 */
	void onAfterDraw(Canvas canvas, Rect dirty);
	
	void onResourceLoaded(ResOutputInfo resInfo);
	
}