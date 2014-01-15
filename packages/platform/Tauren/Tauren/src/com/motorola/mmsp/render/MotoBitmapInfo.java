package com.motorola.mmsp.render;

import android.graphics.Bitmap;

public class MotoBitmapInfo {
	public boolean mGLInit;
	public int mGLTextureID;
	
	public int mId;
	public Bitmap mBitmap;
	public int mBitmapWidth;
	public int mBitmapHeight;
	public boolean mHasAlpha;
	
	public void setBitmap(int id, Bitmap bmp) {
		mId = id;
		mBitmap = bmp;
		if (mBitmap != null) {
			mBitmapWidth = mBitmap.getWidth();
			mBitmapHeight = mBitmap.getHeight();
			mHasAlpha = mBitmap.hasAlpha();
			mGLInit = false;
			mGLTextureID = -1;
		} else {
			mBitmapWidth = 0;
			mBitmapHeight = 0;
			mHasAlpha = false;
			mGLInit = false;
			mGLTextureID = -1;
		}
	}
}
