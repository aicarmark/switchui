package com.motorola.mmsp.render.gl;

import com.motorola.mmsp.render.util.MotoMathUtil;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class MotoGLRenderView extends SurfaceView implements SurfaceHolder.Callback{
	protected MotoGLRenderPlayer mPlayer;
	private String mResPackageName;
	private int mResId;
	
	public MotoGLRenderView(Context context) {
		super(context);
		init(null);
	}
	
	public MotoGLRenderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
		
	}
	public MotoGLRenderView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}
	
	public MotoGLRenderView(Context context, int resId) {
		super(context);
		mResId = resId;
		init(null);
	}
	
	private void init(AttributeSet attrs) {
		if (attrs != null) {
			int N = attrs.getAttributeCount();
			for (int i=0; i<N; i++) {
				String name = attrs.getAttributeName(i);
				if ("res_id".equals(name)) {
					String value = attrs.getAttributeValue(i);
					if (value != null) {
						if ('@' == value .charAt(0)) {
							value = value.substring(1);
						}
						mResId = MotoMathUtil.parseIntFast(value);
					}
				} else if ("res_package_name".equals(name)) {
					mResPackageName = attrs.getAttributeValue(i);
				}
			}
		}
		
		setPlayerRes(mResPackageName, mResId);
		getHolder().addCallback(this);
	}
	
	private Context getPackageContext(String packageName) {
		Context context = null;
		if (packageName != null) {
			try {
				context = getContext().createPackageContext(packageName, 0);
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			context = getContext();
		}
		return context;
	}

	public void setPlayerRes(Resources res, int resId) {
		MotoGLRenderPlayer player = mPlayer;
		
		
		mPlayer = new MotoGLRenderPlayer();
		if (resId != 0) {
			mPlayer.setRes(res, resId);
		}
		mPlayer.setSurfaceHolder(getHolder());
		mPlayer.setRect(new Rect(0, 0, getWidth(), getHeight()));

		if (player != null) {
			player.stop();
			player.release();
		}
	}
	
	public void setPlayerRes(String packageName, int resId) {
		Context context = getPackageContext(packageName);
		if (context != null) {
			Resources res = context.getResources();
			setPlayerRes(res, resId);
		}
	}

	public void setPlayerRes(String packageName, String resName) {
		Context context = getPackageContext(packageName);
		Resources res = context.getResources();
		int resId = res.getIdentifier(packageName + ":xml/" + resName, null,
				null);
		setPlayerRes(res, resId);
	}
	
	//@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mPlayer.setSurfaceHolder(holder);
	}

	//@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		//mPlayer.setSurfaceHolder(holder);
		mPlayer.setRect(new Rect(0, 0, width, height));
	}

	//@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mPlayer.setSurfaceHolder(null);
	}
	
	public MotoGLRenderPlayer getRenderPlayer() {
		return mPlayer;
	}
	
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mPlayer != null) {
			mPlayer.pause();
		}
	}
	
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (mPlayer != null) {
			mPlayer.play();
		}
	}
	
	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		if (visibility == VISIBLE) {
			if (mPlayer != null) {
				mPlayer.play();
			}
		} else {
			if (mPlayer != null) {
				mPlayer.pause();
			}
		}
	}

}
