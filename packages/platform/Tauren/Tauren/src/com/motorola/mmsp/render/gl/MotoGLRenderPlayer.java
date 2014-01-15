package com.motorola.mmsp.render.gl;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;

import com.motorola.mmsp.render.MotoConfig;
import com.motorola.mmsp.render.MotoRenderPlayer;

public class MotoGLRenderPlayer extends MotoRenderPlayer{
	private static final String TAG = MotoConfig.TAG;
	public final static long DEFAULT_FRAME_TIME = 50;

	private GLThread mGLThread;
	private EGLConfigChooser mEGLConfigChooser;
	private EGLContextFactory mEGLContextFactory;
	private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
	private GLWrapper mGLWrapper;
	private Renderer mRenderer;
	

	/**
	 * this is a constructor
	 * 
	 */
	public MotoGLRenderPlayer() {
		this(DEFAULT_FRAME_TIME);
	}

	/**
	 * this is a constructor
	 * 
	 * @param frameTime
	 *            the time of every frame
	 */
	public MotoGLRenderPlayer(long frameTime) {
		super(true);
		mResLoaded.mPlayConfigInfo.mDiryRectEnable = false;
		mResLoaded.mPlayConfigInfo.mHitRectEnable = false;
		
		mRenderer = new GLRenderer20(this);
		setRenderer(mRenderer);
		mGLThread = new GLThread(mRenderer, mEGLConfigChooser, 
				mEGLContextFactory, mEGLWindowSurfaceFactory, mGLWrapper);
	}

	/**
	 * make the player to play
	 */
	public void play() {
		super.play();
		if (mGLThread == null) {
			mGLThread = new GLThread(mRenderer, mEGLConfigChooser, 
					mEGLContextFactory, mEGLWindowSurfaceFactory, mGLWrapper);
		} else {
			if (mGLThread.mExited) {
				Log.e(TAG, "Player already stop!! could not start again!");
			} else if (mGLThread.mStarted) {
				mGLThread.onResume();
			} else {
				mGLThread.start();
			}
		}
	}

	/**
	 * make the player to stop
	 */
	public void stop() {
		if (!mGLThread.mExited) {
			mGLThread.requestExitAndWait();
			super.stop();
		}
	}

	/**
	 * make player pause
	 * 
	 */
	public void pause() {
		if (!mGLThread.mPaused) {
			mGLThread.onPause();
			super.pause();
		}
	}

	/**
	 * get the player's status
	 * 
	 * @return a current status of a player
	 */
	public int getStatus() {
		return mGLThread.getStatus();
	}

	/**
	 * release the player
	 */
	public void release() {
		stop();
		if (MotoConfig.KEY_LOG) {
			Log.d(TAG, "MotoGLRenderPlayer " + mPlayerId + " release!!");
		}
		releaseResource();
		sActivePlayers.remove(Integer.valueOf(mPlayerId));
		if (MotoConfig.KEY_LOG) {
			Log.d(TAG, "Player " + mPlayerId + ", release!"
					+ " active players = " + sActivePlayers);
		}
	}

	/**
	 * set frame time
	 * 
	 * @param frameTime
	 *            the time of every frame
	 */
	public void setFrameTime(long frameTime) {
		mGLThread.setFrameTime(frameTime);
	}
	
	public long getFrameTime() {
		return mGLThread.getFrameTime();
	}

	/**
	 * set Rect
	 * 
	 * @param rect
	 */
	public void setRect(Rect rect) {
		if (rect == null) {
			mGLThread.onWindowResize(0, 0);
		} else {
			mGLThread.onWindowResize(rect.width(), rect.height());
		}
	}
	
	@Override
	public void setRes(Resources res, int configId) {
		super.setRes(res, configId);

		if (mResLoaded != null) {
			if (mResLoaded.mAnimationSetsBuilders != null) {
				mResLoaded.mAnimationSetsBuilders.clear();
			}
			if (mResLoaded.mInteractionSetsBuilders != null) {
				mResLoaded.mInteractionSetsBuilders.clear();
			}
			if (mResLoaded.mPlayConfigInfo != null) {
				setFrameTime(mResLoaded.mPlayConfigInfo.mFrameTime);
			}
		}
	}
	
	public void queueEvent(Runnable r) {
		if (mGLThread != null) {
			mGLThread.queueEvent(r);
		}
	}
	
	/**
	 * draw bitmap on canvas
	 */
	public void onDraw(Canvas canvas) {
		throw new RuntimeException("Don't call it when you use MotoGLRenderPlayer");
	}
	
	/**
	 * set render target, the target is a SurfaceHolder
	 * 
	 * @param surfaceHolder
	 *            the developer set it
	 */
	public void setSurfaceHolder(SurfaceHolder surfaceHolder) {
		if (surfaceHolder == mGLThread.mHolder) {
			return;
		}
		
		if (surfaceHolder != null) {
			mGLThread.surfaceCreated(surfaceHolder);
		} else {
			mGLThread.surfaceDestroyed();
			mGLThread.mHolder = null;
		}
	}
	
	public SurfaceHolder getSurfaceHolder() {
		return mGLThread.mHolder;
	}
	
	/**
	 * An EGL helper class.
	 */
	public void setGLWrapper(GLWrapper glWrapper) {
		mGLWrapper = glWrapper;
	}

	private void setRenderer(Renderer renderer) {
		//checkRenderThreadState();
		if (mEGLConfigChooser == null) {
			mEGLConfigChooser = new SimpleEGLConfigChooser(false);
		}
		if (mEGLContextFactory == null) {
			mEGLContextFactory = new DefaultContextFactory();
		}
		if (mEGLWindowSurfaceFactory == null) {
			mEGLWindowSurfaceFactory = new DefaultWindowSurfaceFactory();
		}
		mRenderer = renderer;
	}

	void setEGLContextFactory(EGLContextFactory factory) {
		checkRenderThreadState();
		mEGLContextFactory = factory;
	}

	void setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory factory) {
		checkRenderThreadState();
		mEGLWindowSurfaceFactory = factory;
	}

	public void setEGLConfigChooser(EGLConfigChooser configChooser) {
		checkRenderThreadState();
		mEGLConfigChooser = configChooser;
	}

	public void setEGLConfigChooser(boolean needDepth) {
		setEGLConfigChooser(new SimpleEGLConfigChooser(needDepth));
	}

	public void setEGLConfigChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize,
			int stencilSize) {
		setEGLConfigChooser(new ComponentSizeChooser(redSize, greenSize, blueSize, alphaSize, depthSize,
				stencilSize));
	}

	private void checkRenderThreadState() {
		if (mGLThread != null) {
			throw new IllegalStateException("setRenderer has already been called for this instance.");
		}
	}
}
