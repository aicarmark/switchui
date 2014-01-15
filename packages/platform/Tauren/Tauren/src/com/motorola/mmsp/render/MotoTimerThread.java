package com.motorola.mmsp.render;

import android.os.Handler;
import android.os.SystemClock;

public class MotoTimerThread {
	@SuppressWarnings("unused")
	private static final String TAG = MotoConfig.TAG;
	
	private Boolean mIsRunning = false;
	private boolean mIsExit = false;
	private Object mLock = new Object();
	private Runnable mRun = new Runnable() {
		public void run() {
			main();
		}
	};
	private Thread mThread = null;
	private Handler mUIHandler = null;
	private Runnable mUIRunner = null;
	private Runnable mThreadRunner = null;
	private long mTime = 0;
	private static long sId;
	private long mId;

	/**
	 * this is a constructor. uiRunner is running in UI thread, threadRunner is
	 * running in timer thread. Time is the time between frames on ms level.
	 * 
	 * @param uiHandler
	 * @param uiRunner
	 * @param threadRunner
	 * @param time
	 */
	MotoTimerThread(Handler uiHandler, Runnable uiRunner,
			Runnable threadRunner, long time) {
		mUIHandler = uiHandler;
		mUIRunner = uiRunner;
		mThreadRunner = threadRunner;
		mTime = time;
		mId = sId++;
	}

	/**
	 * set frame time of every frame
	 * 
	 * @param time
	 */
	public void setFrameTime(long time) {
		mTime = time;
	}

	/**
	 * get frame time of current player
	 * 
	 * @return frame time
	 */
	public long getFrameTime() {
		return mTime;
	}
	
	public int getStatus() {
		if (mIsRunning) {
			return MotoRenderPlayer.PLAYER_RUNNING;
		}
		if (mIsExit) {
			return MotoRenderPlayer.PLAYER_STOP;
		}
		if (!mIsRunning && !mIsExit) {
			return MotoRenderPlayer.PLAYER_PAUSE;
		}
		return MotoRenderPlayer.PLAYER_NOT_START;
	}

	/**
	 * make the time thread start
	 */
	public void start() {
		if (mIsExit) {
			return;
		}
		mIsRunning = true;
		if (mThread == null) {
			mThread = new Thread(mRun, "TimerThread-" + mId);
			mThread.start();
		}
		synchronized (mLock) {
			mLock.notifyAll();
		}
	}

	/**
	 * make the time thread pause
	 */
	public void pause() {
		mIsRunning = false;
	}

	/**
	 * make the time thread pause.Don't use thread.stop(),let the looper exit
	 * itself.
	 */
	public void stop() {
		mUIHandler.removeCallbacks(mUIRunner);
		mIsRunning = false;
		mIsExit = true;
		synchronized (mLock) {
			mLock.notifyAll();
		}
	}

	private final void main() {
		while (true) {
			if (mIsExit) {
				break;
			}
			if (mIsRunning) {
				long time = SystemClock.uptimeMillis();
				if (mThreadRunner != null) {
					mThreadRunner.run();
				}
				freeSelfForTime(mTime - (SystemClock.uptimeMillis() - time));
			} else {
				suspendSelf();
			}
		}
	}

	private final void freeSelfForTime(long time) {
		if (mUIHandler != null && mUIRunner != null && mIsRunning) {
			mUIHandler.postAtFrontOfQueue(mUIRunner);
		}
		
		try {
			if (time > 0) {
				Thread.sleep(time, 0);
				//this.wait(time, 1);
			} else {
				Thread.sleep(2);
				//this.wait(2);
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Suspend by itself, let the timer thread suspend and do not use CPU.
	 */
	private final void suspendSelf() {
		synchronized (mLock) {
			try {
				mLock.wait();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
