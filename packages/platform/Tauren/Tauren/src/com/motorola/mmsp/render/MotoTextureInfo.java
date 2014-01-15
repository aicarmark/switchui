package com.motorola.mmsp.render;

import android.graphics.Rect;

public final class MotoTextureInfo {
	public int mId;
	public MotoBitmapInfo mBitmapInfo;
	
	public int mLeftInBitmap;
	public int mTopInBitmap;
	public int mWidth;
	public int mHeight;

	public float mTextureCoordX;
	public float mTextureCoordY;
	public float mTextureCoordWidth;
	public float mTextureCoordHeight;
	
	public void set(int id, int x, int y, 
			int width, int height, MotoBitmapInfo bitmapInfo) {
		mId = id;
		mBitmapInfo = bitmapInfo;
		mLeftInBitmap = x;
		mTopInBitmap = y;
		mWidth = width;
		mHeight = height;
		if (bitmapInfo != null && bitmapInfo.mBitmap != null) {
			mTextureCoordX = (float)mLeftInBitmap / bitmapInfo.mBitmapWidth;
			mTextureCoordY = (float)mTopInBitmap / bitmapInfo.mBitmapHeight;
			mTextureCoordWidth = (float)mWidth / bitmapInfo.mBitmapWidth;
			mTextureCoordHeight = (float)mHeight / bitmapInfo.mBitmapHeight;
		} else {
			mTextureCoordX = 0;
			mTextureCoordY = 0;
			mTextureCoordWidth = 0;
			mTextureCoordWidth = 0;
		}
	}
	
	public void set(int id, MotoBitmapInfo bitmapInfo) {
		set(id, 0, 0, bitmapInfo.mBitmapWidth, 
				bitmapInfo.mBitmapHeight, bitmapInfo);
	}
	
	public Rect getBound() {
		return new Rect(mLeftInBitmap, mTopInBitmap, 
				mLeftInBitmap + mWidth,  mTopInBitmap + mHeight);
	}
}