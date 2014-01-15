package com.motorola.mmsp.render.gl;

import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;

import com.motorola.mmsp.render.MotoConfig;
import com.motorola.mmsp.render.MotoRenderPlayer;

class GLThread extends Thread {
	private final static String TAG = MotoConfig.TAG;
	private final static boolean DEBUG = MotoConfig.DEBUG;
	public final static int DEBUG_CHECK_GL_ERROR = 1;
	public final static int DEBUG_LOG_GL_CALLS = 2;
	
	
	// Once the thread is started, all accesses to the following member
    // variables are protected by the sGLThreadManager monitor
    private boolean mShouldExit;
    boolean mExited;
    public boolean mStarted;
    private boolean mRequestPaused;
    public boolean mPaused;
    private boolean mHasSurface;
    private boolean mWaitingForSurface;
    private boolean mHaveEglContext;
    private boolean mHaveEglSurface;
    private boolean mShouldReleaseEglContext;
    private int mWidth;
    private int mHeight;
    private int mRenderMode;
    private boolean mSizeChanged = true;
    private boolean mRequestRender;
    private boolean mRenderComplete;
    private ArrayList<Runnable> mEventQueue = new ArrayList<Runnable>();
    private static final GLThreadManager sGLThreadManager = new GLThreadManager();
    private final static boolean DRAW_TWICE_AFTER_SIZE_CHANGED = true;
    private boolean mPreserveEGLContextOnPause;
    
	private EGLConfigChooser mEGLConfigChooser;
	private EGLContextFactory mEGLContextFactory;
	private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
	private GLWrapper mGLWrapper;
	public SurfaceHolder mHolder;
    private Renderer mRenderer;
    private EglHelper mEglHelper;
	
	
	private long mFrameTime;
	private int mFrame;
	private long mTotalTime;
	private long mSwapTotalTime;
	private long mNow;
	private long mTimeElipse;

	private static long sId;
	private long mId;

	GLThread(Renderer renderer, EGLConfigChooser chooser, EGLContextFactory contextFactory,
			EGLWindowSurfaceFactory surfaceFactory, GLWrapper wrapper) {
		super("GLThread-" + sId);
		mId = sId++;
		mExited = false;
		mWidth = 0;
		mHeight = 0;
		mFrameTime = MotoGLRenderPlayer.DEFAULT_FRAME_TIME;
		mRenderer = renderer;
		mRequestRender = true;
		this.mEGLConfigChooser = chooser;
		this.mEGLContextFactory = contextFactory;
		this.mEGLWindowSurfaceFactory = surfaceFactory;
		this.mGLWrapper = wrapper;
	}
	
	public void setFrameTime(long frameTime) {
		this.mFrameTime = frameTime;
	}
	
	public long getFrameTime() {
		return mFrameTime;
	}
	
	public int getStatus() {
		if (mExited) {
			return MotoRenderPlayer.PLAYER_STOP;
		}
		if (mPaused) {
			return MotoRenderPlayer.PLAYER_PAUSE;
		}
		if (mStarted) {
			return MotoRenderPlayer.PLAYER_RUNNING;
		}
		return MotoGLRenderPlayer.PLAYER_NOT_START;
	}
	
	@Override
	public synchronized void start() {
		if (mStarted) {
			return;
		}
		
		mStarted = true;
		if (MotoConfig.KEY_LOG) {
			Log.d(TAG, "Request GL thread start! " + mId);
		}
		super.start();
		if (MotoConfig.KEY_LOG) {
			Log.d(TAG, "Finish GL thread start! " + mId);
		}
	}

    @Override
    public void run() {
        //setName("GLThread " + getId());
        if (MotoConfig.KEY_LOG) {
            Log.i("GLThread", "starting tid=" + mId);
        }

        try {
            guardedRun();
        } catch (InterruptedException e) {
            // fall thru and exit normally
        } finally {
            sGLThreadManager.threadExiting(this);
        }
    }

    /*
     * This private method should only be called inside a
     * synchronized(sGLThreadManager) block.
     */
    private void stopEglSurfaceLocked() {
        if (mHaveEglSurface) {
            mHaveEglSurface = false;
            mRenderer.release();
            mEglHelper.destroySurface();
        }
    }

    /*
     * This private method should only be called inside a
     * synchronized(sGLThreadManager) block.
     */
    private void stopEglContextLocked() {
        if (mHaveEglContext) {
            mEglHelper.finish();
            mHaveEglContext = false;
            sGLThreadManager.releaseEglContextLocked(this);
        }
    }
    private void guardedRun() throws InterruptedException {
    	mEglHelper = new EglHelper(mEGLConfigChooser, 
				mEGLContextFactory, mEGLWindowSurfaceFactory, 
				mGLWrapper);
        mHaveEglContext = false;
        mHaveEglSurface = false;
        try {
            GL10 gl = null;
            boolean createEglContext = false;
            boolean createEglSurface = false;
            boolean lostEglContext = false;
            boolean sizeChanged = false;
            boolean wantRenderNotification = false;
            boolean doRenderNotification = false;
            boolean askedToReleaseEglContext = false;
            int w = 0;
            int h = 0;
            Runnable event = null;

            while (true) {
                synchronized (sGLThreadManager) {
                    while (true) {
                        if (mShouldExit) {
                            return;
                        }

                        if (! mEventQueue.isEmpty()) {
                            event = mEventQueue.remove(0);
                            break;
                        }

                        // Update the pause state.
                        if (mPaused != mRequestPaused) {
                            mPaused = mRequestPaused;
                            sGLThreadManager.notifyAll();
                            if (MotoConfig.KEY_LOG) {
                                Log.i("GLThread", "mPaused is now " + mPaused + " tid=" + getId());
                            }
                        }

                        // Do we need to give up the EGL context?
                        if (mShouldReleaseEglContext) {
                            if (MotoConfig.KEY_LOG) {
                                Log.i("GLThread", "releasing EGL context because asked to tid=" + getId());
                            }
                            stopEglSurfaceLocked();
                            stopEglContextLocked();
                            mShouldReleaseEglContext = false;
                            askedToReleaseEglContext = true;
                        }

                        // Have we lost the EGL context?
                        if (lostEglContext) {
                            stopEglSurfaceLocked();
                            stopEglContextLocked();
                            lostEglContext = false;
                        }

                        // Do we need to release the EGL surface?
                        if (mHaveEglSurface && mPaused) {
                            if (MotoConfig.KEY_LOG) {
                                Log.i("GLThread", "releasing EGL surface because paused tid=" + getId());
                            }
                            stopEglSurfaceLocked();
                            if (!mPreserveEGLContextOnPause || sGLThreadManager.shouldReleaseEGLContextWhenPausing()) {
                                stopEglContextLocked();
                                if (MotoConfig.KEY_LOG) {
                                    Log.i("GLThread", "releasing EGL context because paused tid=" + getId());
                                }
                            }
                            if (sGLThreadManager.shouldTerminateEGLWhenPausing()) {
                                mEglHelper.finish();
                                if (MotoConfig.KEY_LOG) {
                                    Log.i("GLThread", "terminating EGL because paused tid=" + getId());
                                }
                            }
                        }

                        // Have we lost the surface view surface?
                        if ((! mHasSurface) && (! mWaitingForSurface)) {
                            if (MotoConfig.KEY_LOG) {
                                Log.i("GLThread", "noticed surfaceView surface lost tid=" + getId());
                            }
                            if (mHaveEglSurface) {
                                stopEglSurfaceLocked();
                            }
                            mWaitingForSurface = true;
                            sGLThreadManager.notifyAll();
                        }

                        // Have we acquired the surface view surface?
                        if (mHasSurface && mWaitingForSurface) {
                            if (MotoConfig.KEY_LOG) {
                                Log.i("GLThread", "noticed surfaceView surface acquired tid=" + getId());
                            }
                            mWaitingForSurface = false;
                            sGLThreadManager.notifyAll();
                        }

                        if (doRenderNotification) {
                            if (MotoConfig.KEY_LOG) {
                                Log.i("GLThread", "sending render notification tid=" + getId());
                            }
                            wantRenderNotification = false;
                            doRenderNotification = false;
                            mRenderComplete = true;
                            sGLThreadManager.notifyAll();
                        }

                        // Ready to draw?
                        if (readyToDraw()) {

                            // If we don't have an EGL context, try to acquire one.
                            if (! mHaveEglContext) {
                                if (askedToReleaseEglContext) {
                                    askedToReleaseEglContext = false;
                                } else if (sGLThreadManager.tryAcquireEglContextLocked(this)) {
                                    try {
                                        mEglHelper.start();
                                    } catch (RuntimeException t) {
                                        sGLThreadManager.releaseEglContextLocked(this);
                                        throw t;
                                    }
                                    mHaveEglContext = true;
                                    createEglContext = true;

                                    sGLThreadManager.notifyAll();
                                }
                            }

                            if (mHaveEglContext && !mHaveEglSurface) {
                                mHaveEglSurface = true;
                                createEglSurface = true;
                                sizeChanged = true;
                            }

                            if (mHaveEglSurface) {
                                if (mSizeChanged) {
                                    sizeChanged = true;
                                    w = mWidth;
                                    h = mHeight;
                                    wantRenderNotification = true;
                                    if (MotoConfig.KEY_LOG) {
                                        Log.i(TAG, "noticing that we want render notification tid=" + getId());
                                    }

                                    if (DRAW_TWICE_AFTER_SIZE_CHANGED) {
                                        // We keep mRequestRender true so that we draw twice after the size changes.
                                        // (Once because of mSizeChanged, the second time because of mRequestRender.)
                                        // This forces the updated graphics onto the screen.
                                    } else {
                                        mRequestRender = false;
                                    }
                                    mSizeChanged = false;
                                } else {
                                    mRequestRender = false;
                                }
                                sGLThreadManager.notifyAll();
                                break;
                            }
                        }

                        // By design, this is the only place in a GLThread thread where we wait().
                        if (MotoConfig.DEBUG) {
                            Log.i("GLThread", "waiting tid=" + getId()
                                + " mHaveEglContext: " + mHaveEglContext
                                + " mHaveEglSurface: " + mHaveEglSurface
                                + " mPaused: " + mPaused
                                + " mHasSurface: " + mHasSurface
                                + " mWaitingForSurface: " + mWaitingForSurface
                                + " mWidth: " + mWidth
                                + " mHeight: " + mHeight
                                + " mRequestRender: " + mRequestRender
                                + " mRenderMode: " + mRenderMode);
                        }
                        //Log.w(TAG, "we wait!!");
                        sGLThreadManager.wait();
                    }
                } // end of synchronized(sGLThreadManager)

                if (event != null) {
                    event.run();
                    event = null;
                    continue;
                }

                try {
                	long time = SystemClock.uptimeMillis();
                    if (createEglSurface) {
                        if (MotoConfig.KEY_LOG) {
                            Log.w("GLThread", "egl createSurface");
                        }
                        gl = (GL10) mEglHelper.createSurface(mHolder);
                        if (gl == null) {
                            // Couldn't create a surface. Quit quietly.
                            break;
                        }
                        sGLThreadManager.checkGLDriver(gl);
                        createEglSurface = false;
                    }

                    if (createEglContext) {
                        if (MotoConfig.KEY_LOG) {
                            Log.w("GLThread", "onSurfaceCreated");
                        }
                        mRenderer.onSurfaceCreated(gl, mEglHelper.mEglConfig);
                        createEglContext = false;
                    }

                    if (sizeChanged) {
                        if (MotoConfig.KEY_LOG) {
                            Log.w("GLThread", "onSurfaceChanged(" + w + ", " + h + ")");
                        }
                        mEglHelper.purgeBuffers();
                        mRenderer.onSurfaceChanged(gl, w, h);
                        sizeChanged = false;
                    }

                    boolean bDraw = mRenderer.onDrawFrame(gl);
                    if (bDraw) {
                    	long swapT = SystemClock.uptimeMillis();
                    	if (!mEglHelper.swap()) {
                        	Log.i("GLThread", "egl context lost tid=" + getId());
                            lostEglContext = true;
                        }
                    	mSwapTotalTime += (SystemClock.uptimeMillis() - swapT);
                    }

                    if (wantRenderNotification) {
                        doRenderNotification = true;
                    }
                    
                    mFrame ++;
    				if (mNow == 0) {
    					mNow = SystemClock.uptimeMillis();
    				} else {
    					long now = SystemClock.uptimeMillis();
    					mTimeElipse += now - mNow;
    					mNow = now;
    				}
    				long timeCost = SystemClock.uptimeMillis() - time;
    				mTotalTime += timeCost;
    				long timeWait = mFrameTime - timeCost;
    				if (timeWait <= 0) {
    					timeWait = 5;
    				}
    				
    				if (DEBUG) {
    					if (mFrame >= 100) {
    						Log.d(TAG, "draw frame total time = " + mTotalTime / (float)mFrame 
    								+ ", fps is " + (1000f * mFrame / mTimeElipse)
    								+ ", swap time = " + (mSwapTotalTime / 100f));
    						mTotalTime = 0;
    						mFrame = 0;
    						mTimeElipse = 0;
    						mSwapTotalTime = 0;
    					}
    				}
    				
    				Thread.sleep(timeWait);
                } catch (EglBadSurfaceException e) {
					e.printStackTrace();
					lostEglContext = true;
				}
                
            }

        } finally {
            /*
             * clean-up everything...
             */
            synchronized (sGLThreadManager) {
                stopEglSurfaceLocked();
                stopEglContextLocked();
            }
        }
    }

    public boolean ableToDraw() {
        return mHaveEglContext && mHaveEglSurface && readyToDraw();
    }

    private boolean readyToDraw() {
        return (!mPaused) && mHasSurface
            && (mWidth > 0) && (mHeight > 0);
    }

    public void requestRender() {
        synchronized(sGLThreadManager) {
            mRequestRender = true;
            sGLThreadManager.notifyAll();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        synchronized(sGLThreadManager) {
            if (MotoConfig.KEY_LOG) {
                Log.i("GLThread", "surfaceCreated tid=" + getId());
            }
            mHasSurface = true;
            mHolder = holder;
            sGLThreadManager.notifyAll();
            while((mWaitingForSurface) && (!mExited) && (mStarted)) {
                try {
                    sGLThreadManager.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void surfaceDestroyed() {
        synchronized(sGLThreadManager) {
            if (MotoConfig.KEY_LOG) {
                Log.i("GLThread", "surfaceDestroyed tid=" + getId());
            }
            mHasSurface = false;
            sGLThreadManager.notifyAll();
            while((!mWaitingForSurface) && (!mExited) && (mStarted)) {
                try {
                    sGLThreadManager.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void onPause() {
        synchronized (sGLThreadManager) {
            if (MotoConfig.KEY_LOG) {
                Log.i("GLThread", "onPause tid=" + getId());
            }
            mRequestPaused = true;
            sGLThreadManager.notifyAll();
            while ((mStarted) && (! mExited) && (! mPaused)) {
                if (MotoConfig.KEY_LOG) {
                    Log.i("Main thread", "onPause waiting for mPaused.");
                }
                try {
                    sGLThreadManager.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void onResume() {
        synchronized (sGLThreadManager) {
            if (MotoConfig.KEY_LOG) {
                Log.i("GLThread", "onResume tid=" + getId());
            }
            mRequestPaused = false;
            mRequestRender = true;
            mRenderComplete = false;
            sGLThreadManager.notifyAll();
            while ((! mExited) && mPaused && (!mRenderComplete) && (mStarted)) {
                if (MotoConfig.KEY_LOG) {
                    Log.i("Main thread", "onResume waiting for !mPaused.");
                }
                try {
                    sGLThreadManager.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void onWindowResize(int w, int h) {
        synchronized (sGLThreadManager) {
            mWidth = w;
            mHeight = h;
            mSizeChanged = true;
            mRequestRender = true;
            mRenderComplete = false;
            sGLThreadManager.notifyAll();

            // Wait for thread to react to resize and render a frame
            while (! mExited && !mPaused && !mRenderComplete
                    && ableToDraw() && (mStarted)) {
                if (MotoConfig.KEY_LOG) {
                    Log.i("Main thread", "onWindowResize waiting for render complete from tid=" + getId());
                }
                try {
                    sGLThreadManager.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void requestExitAndWait() {
        // don't call this from GLThread thread or it is a guaranteed
        // deadlock!
        synchronized(sGLThreadManager) {
            mShouldExit = true;
            sGLThreadManager.notifyAll();
            while (! mExited  && (mStarted)) {
                try {
                    sGLThreadManager.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void requestReleaseEglContextLocked() {
        mShouldReleaseEglContext = true;
        sGLThreadManager.notifyAll();
    }

    /**
     * Queue an "event" to be run on the GL rendering thread.
     * @param r the runnable to be run on the GL rendering thread.
     */
    public void queueEvent(Runnable r) {
        if (r == null) {
            throw new IllegalArgumentException("r must not be null");
        }
        synchronized(sGLThreadManager) {
            mEventQueue.add(r);
            sGLThreadManager.notifyAll();
        }
    }
}
