package com.motorola.mmsp.render;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.View;


public class MotoPaintable {
	public float mLevel;
	public MotoPicture mPicture;
	public Rect mRect;
	public boolean mVisible = true;
	public View mView;
	public Matrix mMatrix = new Matrix();
	public int mAlpha = 255;
	public MotoTextureInfo mTextureInfo;
	
	
	public int getWidth() {
		if (mTextureInfo != null) {
			return mTextureInfo.mWidth;
		}
		
		if (mView != null) {
			return mView.getWidth();
		}
		
		return 0;
	}
	
	public int getHeight() {
		if (mTextureInfo != null) {
			return mTextureInfo.mHeight;
		}
		
		if (mView != null) {
			return mView.getHeight();
		}
		
		return 0;
	}
	
	public void glChanged() {
		if (mTextureInfo != null && mTextureInfo.mBitmapInfo != null) {
			mTextureInfo.mBitmapInfo.mGLInit = false;
			mTextureInfo.mBitmapInfo.mGLTextureID = -1;
		}
	}
	
	final void setView(View view) {
		mView = view;
		mTextureInfo = null;
	}
	
	final void setTexture(MotoTextureInfo textureInfo) {
		mTextureInfo = textureInfo;
		mView = null;
	}
	
	final View getView() {
		return mView;
	}
	
	final Bitmap getBitmap() {
		if (mTextureInfo != null && mTextureInfo.mBitmapInfo != null) {
			return mTextureInfo.mBitmapInfo.mBitmap;
		}
		
		return null;
	}
	
	final int getGLTextureId() {
		if (mTextureInfo != null && mTextureInfo.mBitmapInfo != null) {
			return mTextureInfo.mBitmapInfo.mGLTextureID;
		}
		
		return 0;
	}
	
	final boolean hasBitmapOrTexture() {
		if (mTextureInfo != null && mTextureInfo.mBitmapInfo != null) {
			if (mTextureInfo.mBitmapInfo.mBitmap != null
					|| mTextureInfo.mBitmapInfo.mGLTextureID > 0) {
				return true;
			}
		}
		
		return false;
	}
	
	final void setBitmap(Bitmap bitmap, 
			int left, int top, int width, int height) {
		MotoBitmapInfo bitmapInfo = new MotoBitmapInfo();
		bitmapInfo.setBitmap(0, bitmap);
		mTextureInfo = new MotoTextureInfo();
		mTextureInfo.set(0, left, top, 
				width, height, 
				bitmapInfo);
		if (mPicture != null) {
			MotoPicture root = mPicture.findRoot();
			if (root != null) {
				MotoRenderPlayer player = root.getPlayer();
				if (player != null) {
					player.mInnerBitmapChanged = true;
				}
			}
		}
	}
	
	final Rect getBoundInBitmap() {
		return mTextureInfo.getBound();
	}
}
