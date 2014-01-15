package com.motorola.mmsp.render;

import java.util.ArrayList;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Debug;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

import com.motorola.mmsp.render.resloader.MotoResLoader;
import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;
import com.motorola.mmsp.render.util.MotoIntEtypeHashMap;
import com.motorola.mmsp.render.util.MotoLinkedList;

/**
 * 
 * Must statics the performance in debug mode. Log the average performance every
 * 100 frames.
 * 
 * @author mtd673
 * 
 */
public class MotoRenderPlayer {
	private static final String TAG = MotoConfig.TAG;
	private static final boolean DEBUG = MotoConfig.DEBUG;
	private static final int LOG_FRAME = 100;
	public final static long DEFAULT_FRAME_TIME = 40;
	
	public static final int PLAYER_NOT_START = 0;
	public static final int PLAYER_RUNNING = 1;
	public static final int PLAYER_PAUSE = 2;
	public static final int PLAYER_STOP = 3;
	
	public boolean mInnerBitmapChanged;
	protected static int sPlayerId;
	protected int mPlayerId;
	
	
	
	protected MotoRenderPlayerListener mListener;
	protected MotoResLoader.ResOutputInfo mResLoaded = new MotoResLoader.ResOutputInfo();
	protected MotoLinkedList<MotoPaintable> mPaintableList = new MotoLinkedList<MotoPaintable>(100, 2f);
	protected final static MotoPaintable EMPTY_PAINTABLE = new MotoPaintable();
	protected MotoInteractionEvent mInteractionEvent = new MotoInteractionEvent();
	protected MotoPicture mTargetPicture;
	protected MotoPicture mUserEventTargetPicture;
	protected float mCurrentMotionX;
	protected float mCurrentMotionY;
	protected Paint mPaint = new Paint();
	
	private MotoTimerThread mTimerThread;
	private ArrayList<Rect> mDirtyRects;
	private Rect mFrameRect;
	private Rect mDirtyRectAll;
	protected static final ArrayList<Integer> sActivePlayers = new ArrayList<Integer>();

	
	private long mDebugPrepareFrameCount;
	private long mDebugNextTime;
	private long mDebugRenderTime;
	private long mDebugPrepareTime;
	
	private long mDebugCurrentFrame;
	private long mDebugTotalDrawTime;
	private long mDebugTotalPrepareTime;
	private long mDebugTotalDrawBitmapTime;
	private long mDebugTotalProcessEventTime;
	private long mDebugCurrentTime;
	

	private Handler mHandler = new Handler();
	private Runnable mUIRunnable = new Runnable() {
		//@Override
		public void run() {
			if (mTimerThread != null && mTimerThread.getStatus() != PLAYER_RUNNING) {
				return;
			}
			prepareNextFrame();
		}
	};

	/**
	 * this is a constructor
	 * 
	 */
	public MotoRenderPlayer() {
		this(DEFAULT_FRAME_TIME);
	}

	/**
	 * this is a constructor
	 * 
	 * @param frameTime
	 *            the time of every frame
	 */
	public MotoRenderPlayer(long frameTime) {
		mPlayerId = sPlayerId++;
		mTimerThread = new MotoTimerThread(mHandler, mUIRunnable,
				null, frameTime);
		mDirtyRects = new ArrayList<Rect>(200);
		mFrameRect = new Rect(0, 0, 0, 0);
		mDirtyRectAll = new Rect();
		sActivePlayers.add(mPlayerId);
		if (MotoConfig.KEY_LOG) {
			Log.d(TAG, "Player " + mPlayerId + ", create!"
					+ " active players = " + sActivePlayers);
		}
	}
	
	protected MotoRenderPlayer(boolean unused) {
		mPlayerId = sPlayerId++;
		mDirtyRects = new ArrayList<Rect>();
		mFrameRect = new Rect(0, 0, 0, 0);
		mDirtyRectAll = new Rect();
		sActivePlayers.add(mPlayerId);
		if (MotoConfig.KEY_LOG) {
			Log.d(TAG, "Player " + mPlayerId + ", create!"
					+ " active players = " + sActivePlayers);
		}
	}
	
	public int getId() {
		return mPlayerId;
	}

	/**
	 * make the player to play
	 */
	public void play() {
		if (MotoConfig.KEY_LOG) {
			Log.d(TAG, "Player " + mPlayerId + " play");
		}
		// mPrepareTime = SystemClock.uptimeMillis();
		mDebugPrepareTime = -1;
		if (mTimerThread != null) {
			mTimerThread.start();
		}
	}

	/**
	 * make the player to stop
	 */
	public void stop() {
		if (MotoConfig.KEY_LOG) {
			Log.d(TAG, "Player " + mPlayerId + " stop");
		}
		if (mTimerThread != null) {
			mTimerThread.stop();
		}
	}

	/**
	 * make player pause
	 * 
	 */
	public void pause() {
		if (MotoConfig.KEY_LOG) {
			Log.d(TAG, "Player " + mPlayerId + " pause");
		}
		if (mTimerThread != null) {
			mTimerThread.pause();
		}
	}

	/**
	 * get the player's status
	 * 
	 * @return a current status of a player
	 */
	public int getStatus() {
		return mTimerThread.getStatus();
	}

	/**
	 * release the player
	 */
	public void release() {
		mTimerThread.stop();
		releaseResource();
		sActivePlayers.remove(Integer.valueOf(mPlayerId));
		if (MotoConfig.KEY_LOG) {
			Log.d(TAG, "Player " + mPlayerId + ", release!"
					+ " active players = " + sActivePlayers);
		}
	}

	/**
	 * set a listener
	 * 
	 * @param listener
	 */
	public void setListener(MotoRenderPlayerListener listener) {
		mListener = listener;
	}

	/**
	 * 
	 * @return
	 */
	public MotoPicture getRootPicture() {
		return mResLoaded.mPicture;
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	public MotoPicture findPictureById(int id) {
		MotoPicture root = getRootPicture();
		if (root != null) {
			return root.findPictureById(id);
		}
		return null;
	}

	/**
	 * get a listener
	 * 
	 * @return a listener
	 */
	public MotoRenderPlayerListener getListener() {
		return mListener;
	}

	/**
	 * set frame time
	 * 
	 * @param frameTime
	 *            the time of every frame
	 */
	public void setFrameTime(long frameTime) {
		mTimerThread.setFrameTime(frameTime);
	}
	
	public long getFrameTime() {
		return mTimerThread.getFrameTime();
	}

	
	/**
	 * To enable dirty rectangle control or not. Default is enabled.
	 * @param enable
	 */
	public void setEnableDirtyRect(boolean enable) {
		mResLoaded.mPlayConfigInfo.mDiryRectEnable = enable;
	}
	
	public boolean getEnableDitryRect() {
		return mResLoaded.mPlayConfigInfo.mDiryRectEnable;
	}
	
	public void setEnableHitRectCalculate(boolean enable) {
		mResLoaded.mPlayConfigInfo.mHitRectEnable = enable;
	}
	
	public boolean getEnableHitRectCalculate() {
		return mResLoaded.mPlayConfigInfo.mHitRectEnable;
	}

	/**
	 * Please use application context as possible.
	 * @param context.
	 * @param configId
	 *            need parser xml file id
	 */
	public void setRes(Resources res, int configId) {
		//Debug.startMethodTracing("engineLoadRes");
		if (mResLoaded != null && mResLoaded.mPicture != null) {
			stop();
			releaseResource();
		}

		new MotoResLoader(res, mResLoaded).loadRes(configId);
		if (mResLoaded.mPicture != null) {
			mResLoaded.mPicture.setPlayer(this);
		}
		if (mListener != null) {
			mListener.onResourceLoaded(mResLoaded);
		}
		if (MotoConfig.KEY_LOG) {
			Log.d(TAG, "Player " + mPlayerId + ", " + MotoBitmapWrapper.getString());
		}
		//Debug.stopMethodTracing();
	}
	
	public ResOutputInfo getLoadedRes() {
		return mResLoaded;
	}

	/**
	 * set Rect
	 * 
	 * @param rect
	 */
	public void setRect(Rect rect) {
		if (rect == null) {
			mFrameRect.set(0, 0, 0, 0);
		} else {
			mFrameRect.set(rect.left, rect.top, rect.right, rect.bottom);
		}
	}

	/**
	 * release all resources, most is bitmap
	 */
	protected void releaseResource() {
		if (mResLoaded != null) {
			mResLoaded.mPicture = null;
			MotoIntEtypeHashMap<MotoBitmapInfo> bitmapMap = mResLoaded.mBitmapMap;
			int size = bitmapMap != null ? bitmapMap.size() : 0;
			for (int i = 0; i < size; i++) {
				int id = bitmapMap.keyAt(i);
				MotoBitmapWrapper.recycle(id);
			}
			bitmapMap.clear();
			mResLoaded.mTextureInfoMap.clear();
			if (MotoConfig.KEY_LOG) {
				Log.d(TAG, "Player " + mPlayerId + ", " + MotoBitmapWrapper.getString());
			}
		}
	}
	
	public MotoLinkedList<MotoPaintable> getPaintableList() {
		return mPaintableList;
	}
	
	public void prepareNextFrame(int gap) {
		long time = SystemClock.uptimeMillis();
		if (mResLoaded != null && mResLoaded.mPicture != null) {
			if (mListener != null) {
				mListener.onBeforePrepareFrame();
			}
			
			long timeA = SystemClock.uptimeMillis();
			mResLoaded.mPlayConfigInfo.mHasFlushed = false;
			mResLoaded.mPicture.nextFrame(gap,  mResLoaded.mPlayConfigInfo);
			mDebugNextTime += SystemClock.uptimeMillis() - timeA;
			
            if (mResLoaded.mPlayConfigInfo.mHasFlushed) {
				mPaintableList.clear();
				if (mDirtyRects != null) {
					mDirtyRects.clear();
				}
				
				timeA = SystemClock.uptimeMillis();
				mResLoaded.mPicture.renderFrame(mPaintableList, EMPTY_PAINTABLE,
							mDirtyRects, mResLoaded.mPlayConfigInfo);
				mDebugRenderTime += SystemClock.uptimeMillis() - timeA;
				
				if (mDirtyRectAll != null) {
					if (mResLoaded.mPlayConfigInfo.mDiryRectEnable) {
						unionDirtyRect();
					} else {
						mDirtyRectAll.set(mFrameRect);
					}
				}
            } else {
                //reset mDirtyRectAll, so will not refresh view
            	if (mResLoaded.mPlayConfigInfo.mDiryRectEnable) {
            		mDirtyRectAll.setEmpty();
            	}
            }
			
			long duration = SystemClock.uptimeMillis() - time;
			mDebugTotalPrepareTime += duration;
			
			if (mListener != null) {
				mListener.onAfterPrepareFrame(mDirtyRectAll);
			}
			
			if (DEBUG) {
				if (++ mDebugPrepareFrameCount % 100 == 0) {
					Log.d(TAG, "nextFrame time = " + mDebugNextTime / 100f + ", renderFrame time = " + mDebugRenderTime / 100f);
					mDebugNextTime = mDebugRenderTime = 0;
				}
			}
		}
	}

	/**
	 * Make sure it run on UI thread.
	 */
	public void prepareNextFrame() {
		int gap = 40;
		if (mDebugPrepareTime <= 0) {
			mDebugPrepareTime = SystemClock.uptimeMillis();
		} else {
			long now = SystemClock.uptimeMillis();
			gap = (int)(now - mDebugPrepareTime);
			mDebugPrepareTime = now;
		}
		prepareNextFrame(gap);
	}
	
	private MotoPicture findTargetUp(MotoPicture from, MotoInteractionEvent event, boolean isPreviousIntercepted) {
		if (from == null) {
			return null;
		}
		if (from.handleEvnet(event, isPreviousIntercepted)) {
			return from;
		}
		return findTargetUp(from.getParent(), event, isPreviousIntercepted);
	}
	
	private MotoPicture findTargetDown(MotoPicture from, MotoInteractionEvent event) {
		if (from == null) {
			return null;
		}
		if (from.handleEvnet(event, false)) {
			return from;
		}
		if (from instanceof MotoPictureGroup) {
			int size = ((MotoPictureGroup)from).getChildCount();
			for (int i=0; i<size; i++) {
				MotoPicture child = ((MotoPictureGroup)from).getChildAt(i);
				if (child != null) {
					MotoPicture picture = findTargetDown(child, event);
					if (picture != null) {
						return picture;
					}
				}
			}
		}
		return null;
	}

	/**
	 * union dirty rect
	 */
	private void unionDirtyRect() {
		if (mDirtyRectAll == null) {
			return;
		}
		mDirtyRectAll.set(0, 0, 0, 0);
		for (Rect dirtyRect : mDirtyRects) {
			if (dirtyRect != null && Rect.intersects(mFrameRect, dirtyRect)) {
				mDirtyRectAll.union(dirtyRect);
			}
		}
		mDirtyRectAll.intersect(mFrameRect);
	}

	/**
	 * draw bitmap on canvas
	 */
	public void onDraw(Canvas canvas) {
		long time = SystemClock.uptimeMillis();
		if (mListener != null) {
			mListener.onBeforeDraw(canvas, canvas != null ? canvas
					.getClipBounds() : null);
		}

		if (canvas != null) {
			MotoLinkedList.Node<MotoPaintable> cursor = mPaintableList.getFirst();
			while (cursor != null) {
				MotoPaintable paintable = cursor.mData;
				if (paintable != null && paintable.getBitmap() != null
						&& paintable.mVisible) {
					Paint paint = null;
					if (paintable.mAlpha != 255) {
						paint = mPaint;
						paint.setAlpha(paintable.mAlpha);
					}
					canvas.drawBitmap(paintable.getBitmap(), paintable.mMatrix,
							paint);
				}
				cursor = cursor.mNext;
			}
		}

		if (mListener != null) {
			mListener.onAfterDraw(canvas, canvas != null ? canvas
					.getClipBounds() : null);
		}
		mDebugTotalDrawBitmapTime += (SystemClock.uptimeMillis() - time);

		mDebugCurrentFrame++;
		if (DEBUG) {
			if (mDebugCurrentTime == 0) {
				mDebugCurrentTime = SystemClock.uptimeMillis();
			} else {
				long now = SystemClock.uptimeMillis();
				mDebugCurrentTime = now;
			}
			mDebugTotalDrawTime += SystemClock.uptimeMillis() - time;
			if (mDebugCurrentFrame % LOG_FRAME == 0) {
				Log.d(TAG, "mPlayerId = "
						+ mPlayerId
						+ ", draw frame total="
						+ ((float) (mDebugTotalDrawTime + mDebugTotalPrepareTime) / LOG_FRAME)
						+ ", prepare frame time="
						+ ((float) mDebugTotalPrepareTime / LOG_FRAME)
						+ ", draw bitmap time="
						+ ((float) mDebugTotalDrawBitmapTime / LOG_FRAME) + ", process event: "
						+ ((float) mDebugTotalProcessEventTime / LOG_FRAME)
						+ ", bounds=" + mDirtyRectAll);
				
				mDebugTotalDrawTime = 0;
				mDebugTotalPrepareTime = 0;
				mDebugTotalDrawBitmapTime = 0;
				mDebugTotalProcessEventTime = 0;
			}
		}
	}



	/**
	 * return a MotoPicture when user hit screen
	 * 
	 * @param x
	 * @param y
	 * @param index
	 * @return
	 */
	public MotoPicture hitTest(int x, int y, int index) {
		MotoLinkedList.Node<MotoPaintable> cursor = mPaintableList.getFirst();
		while (cursor != null) {
			MotoPaintable paintable = cursor.mData;
			if (paintable != null) {
				if (paintable.mRect.contains(x, y)) {
					if (index-- <= 0) {
						return paintable.mPicture;
					}
				}
			}
			cursor = cursor.mNext;
		}
		return null;
	}
	
	protected MotoLinkedList.Node<MotoPaintable> hitTestBackBegin(MotoLinkedList.Node<MotoPaintable> node, int x, int y) {
		MotoLinkedList.Node<MotoPaintable> cursor = node;
		while (cursor != null) {
			MotoPaintable paintable = cursor.mData;
			if (paintable != null) {
				if (paintable.mRect.contains(x, y)) {
					return cursor;
				}
			}
			cursor = cursor.mPre;
		}
		return null;
	}

	/**
	 * return a list of MotoPicture when user hit screen
	 * @param x
	 * @param y
	 * @return
	 */
	public ArrayList<MotoPicture> hitTest(int x, int y) {
		//TODO memory allocate here.
		ArrayList<MotoPicture> hitPictures = new ArrayList<MotoPicture>(10);
		MotoLinkedList.Node<MotoPaintable> cursor = mPaintableList.getFirst();
		while (cursor != null) {
			MotoPaintable paintable = cursor.mData;
			if (paintable != null) {
				if (paintable.mRect.contains(x, y)) {
					hitPictures.add(paintable.mPicture);
				}
			}
			cursor = cursor.mNext;
		}
		return hitPictures;
	}
	
	public boolean handleEvnet(MotionEvent event) {
		mInteractionEvent.mEvent = event;
		return handleEvent(mInteractionEvent);
	}
	public boolean handleEvent(MotoInteractionEvent interactionEvent) {
		boolean handled = false;
		MotionEvent e = interactionEvent.mEvent;
		if (DEBUG) {
			Log.d(TAG, "handle event e = " + e);
		}
		if (e != null) {
			int action = e.getAction();
			if (action == MotionEvent.ACTION_DOWN) {
				mTargetPicture = null;
				mCurrentMotionX = e.getX();
				mCurrentMotionY = e.getY();
				MotoLinkedList.Node<MotoPaintable> cursor = mPaintableList.getLast();
				while (cursor != null) {
					MotoLinkedList.Node<MotoPaintable> targetNode = hitTestBackBegin(cursor, (int)e.getX(), (int)e.getY());
					if (targetNode == null) {
						break;
					}
					if (targetNode.mData == null) {
						continue;
					}
					MotoPicture targetPicture = findTargetUp(targetNode.mData.mPicture, interactionEvent, true);
					if (targetPicture != null) {
						mTargetPicture = targetPicture;
						handled = true;
						break;
					}
					cursor = targetNode.mPre;
				}
				
				/*
				if (mTargetPicture == null) {
					mTargetPicture = findTargetDown(mResLoaded.mPicture, interactionEvent);
				}
				*/
				
				
			} else if (action == MotionEvent.ACTION_MOVE) {
				interactionEvent.mOffserX = e.getX() - mCurrentMotionX;
				interactionEvent.mOffserY = e.getY() - mCurrentMotionY;
				mCurrentMotionX = e.getX();
				mCurrentMotionY = e.getY();
				if (mTargetPicture != null) {
					if (mTargetPicture.handleEvnet(interactionEvent, true)) {
						handled = true;
					} else {
						mTargetPicture = null;
					}
				}
				
				/*if (!handled) {
					mTargetPicture = findTargetDown(mResLoaded.mPicture, interactionEvent);
					if (mTargetPicture != null) {
						handled = true;
					}
				}*/
				
				
			} else if (action == MotionEvent.ACTION_UP) {
				interactionEvent.mOffserX = e.getX() - mCurrentMotionX;
				interactionEvent.mOffserY = e.getY() - mCurrentMotionY;
				mCurrentMotionX = e.getX();
				mCurrentMotionY = e.getY();
				if (mTargetPicture != null) {
					if (mTargetPicture.handleEvnet(interactionEvent, true)) {
						handled = true;
					} else {
						mTargetPicture = null;
					}
				}
				/*if (!handled) {
					mTargetPicture = findTargetDown(mResLoaded.mPicture, interactionEvent);
					if (mTargetPicture != null) {
						handled = true;
					}
				}*/
				mTargetPicture = null;
				mCurrentMotionX = e.getX();
				mCurrentMotionY = e.getY();
			}
			
		// User define type
		} else {
			if (mUserEventTargetPicture != null) {
				if (mUserEventTargetPicture.handleEvnet(interactionEvent, true)) {
					handled = true;
				} else {
					mUserEventTargetPicture = null;
				}
			}
			if (!handled) {
				mUserEventTargetPicture = findTargetDown(mResLoaded.mPicture,
						interactionEvent);
				if (mUserEventTargetPicture != null) {
					handled = true;
				}
			}
		}
		
		return handled;
	}
}
