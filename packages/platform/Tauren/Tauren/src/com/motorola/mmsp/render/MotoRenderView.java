package com.motorola.mmsp.render;

import java.util.ArrayList;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;
import com.motorola.mmsp.render.util.MotoMathUtil;

public class MotoRenderView extends View {
	@SuppressWarnings("unused")
	private final static String TAG = MotoConfig.TAG;
	
	private MotoRenderPlayer mPlayer;
	private MotoRenderPlayerListener mPlayerListener;
	private String mResPackageName;
	private int mResId;


    /**
     * Extends from android view. Please reference android view constructor for more.
     */
	public MotoRenderView(Context context) {
		super(context);
		init(null);
	}
	
	/**
	 * New a MotoRenderView from code.
	 * @param context: context of activity.
	 * @param resId: the id of a XML file. 
	 */
	public MotoRenderView(Context context, int resId) {
		super(context);
		mResId = resId;
		init(null);
	}
	
	/**
     * Extends from android view. Please reference android view constructor for more.
     */
	public MotoRenderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}

	/**
     * Extends from android view. Please reference android view constructor for more.
     */
	public MotoRenderView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
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
		
		initPlayer();
	}
	
	private void initPlayer() {
		setPlayerRes(mResPackageName, mResId);
		if (mPlayer != null) {
			mPlayer.setRect(new Rect(0, 0, getWidth(), getHeight()));
		}
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
		if (mPlayerListener == null) {
			mPlayerListener = new MotoRenderPlayerListener() {
				public void onBeforePrepareFrame() {
				}
				public void onBeforeDraw(Canvas canvas, Rect dirty) {
				}
				public void onAfterPrepareFrame(Rect dirty) {
					if (dirty != null && !dirty.isEmpty()) {
						invalidate(dirty.left, dirty.top, dirty.right, dirty.bottom);
					}
					//Log.d(TAG, "postInvalidate time = " + (SystemClock.uptimeMillis() - start));
				}
				public void onAfterDraw(Canvas canvas, Rect dirty) {
				}
				//@Override
				public void onResourceLoaded(ResOutputInfo resInfo) {
					
				}
			};
		}
		
		if (mPlayer != null) {
			mPlayer.stop();
		}
		MotoRenderPlayer player = mPlayer;
		
		mPlayer = new MotoRenderPlayer();
		mPlayer.setListener(mPlayerListener);
		mPlayer.setRect(new Rect(0, 0, getWidth(), getHeight()));
		if (resId != 0) {
			mPlayer.setRes(res, resId);
			mPlayer.play();
		}
		
		if (player != null) {
			player.release();
		}
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (changed) {
			if (mPlayer != null) {
				mPlayer.setRect(new Rect(0, 0, getWidth(), getHeight()));
				invalidate();
			}
		}
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
	
	@Override
	protected void onDraw(Canvas canvas) {
		mPlayer.onDraw(canvas);
	}
	
	public void play() {
		mPlayer.play();
	}
	
	public void pause() {
		mPlayer.pause();
	}
	
 	public void stop() {
 		mPlayer.stop();
	}
	
	public void release() {
		mPlayer.release();
	}
	
	
	public int getStatus() {
		return mPlayer.getStatus();
	}
	

	/**
	 * order by z desc
	 * */
	public MotoPicture hitTest(int x, int y, int index) {
		return mPlayer.hitTest(x, y, index);
	}
	
	/**
	 * order by z desc
	 * */
	public ArrayList<MotoPicture> hitTest(int x, int y) {
		return mPlayer.hitTest(x, y);
	}
	
	public MotoPicture findPictureById(int id) {
		return mPlayer.findPictureById(id);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return mPlayer.handleEvnet(event);
	}
	
	public void handleEvent(MotoInteractionEvent event) {
		mPlayer.handleEvent(event);
	}
	
	public MotoRenderPlayer getPlayer() {
		return mPlayer;
	}

}
