package com.motorola.mmsp.render.util;

public class MotoScreenLayout {
	public static final int SCALE_MODE_CENTER_CUT_LONG_EDGE = 0;
	public static final int SCALE_MODE_UPPER_LEFT_CUT_LONG_EDGE = 1;
	public static final int SCALE_MODE_CENTER_INSIDE = 2;
	public static final int SCALE_MODE_FIT_XY = 3;
	public static final int SCALE_MODE_NOT_SCALE = 4;
	
	private float mX;
	private float mY;
	private float mScaleX;
	private float mScaleY;
	
	
	/**
	 * Only SCALE_MODE_CENTER_CUT_LONG_EDGE is done.
	 * @param srcWidth
	 * @param srcHeight
	 * @param dstWidth
	 * @param dstHeight
	 * @param scaleMode
	 */
	public void convert(int srcWidth, int srcHeight, 
			int dstWidth, int dstHeight, 
			int scaleMode) {
		switch (scaleMode) {
		case SCALE_MODE_CENTER_CUT_LONG_EDGE:{
			float wScale = (float)dstWidth / srcWidth;
			float hScale = (float)dstHeight / srcHeight;
			if (wScale < hScale) {
				//width has some pixels left, so scale to height.
				wScale = hScale;
				float targetWidth = srcWidth * wScale;
				float scaledWidthLeftPixel = targetWidth - dstWidth;
				float scaledCoordX = -scaledWidthLeftPixel / 2;
				float x = scaledCoordX / wScale;
				mX = x;
				mY = 0;
				mScaleX = wScale;
				mScaleY = hScale;
			} else {
				//height has some pixels left, so scale to width.
				hScale = wScale;
				float targetHeight = srcHeight * hScale;
				float scaledHeightLeftPixel = targetHeight - dstHeight;
				float scaleCoordY = -scaledHeightLeftPixel / 2;
				float y = scaleCoordY / hScale;
				mX = 0;
				mY = y;
				mScaleX = wScale;
				mScaleY = hScale;
			}
			break;
		}
		
		case SCALE_MODE_UPPER_LEFT_CUT_LONG_EDGE:{
			float wScale = (float)dstWidth / srcWidth;
			float hScale = (float)dstHeight / srcHeight;
			float scale = wScale > hScale ? wScale : hScale;
			mX = mY = 0;
			mScaleX = mScaleY = scale;
			break;
		}
		
		default:
			break;
		}
	}
	
	public float getX() {
		return mX;
	}
	
	public float getY() {
		return mY;
	}
	
	public float getScaleX() {
		return mScaleX;
	}
	
	public float getScaleY() {
		return mScaleY;
	}
}
