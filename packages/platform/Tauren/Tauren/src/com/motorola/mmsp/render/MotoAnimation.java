package com.motorola.mmsp.render;

import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;
import com.motorola.mmsp.render.util.MotoMathUtil;

/**
 * 
 * @author mtd673
 * 
 */
public abstract class MotoAnimation {
	public static final int REPEAT_COUNT_ENDLESS = -1;
	public static final int DURATION_ENDLESS = -1;
	
	public static final int STATUS_NOT_STARTED = 0;
	public static final int STATUS_STOPED = 1;
	public static final int STATUS_PAUSED = 2;
	public static final int STATUS_RUNNING = 3;
	
	protected int mId;
	protected int mStatus = STATUS_NOT_STARTED;
	protected int mDuration;
	protected int mRepeatCount = 1;
	protected int mRepeatIndex;
	protected boolean mPlayback;
	protected MotoAnimationListener mListener;
	protected MotoAnimation mParent;

	public MotoAnimation(MotoAttributeArray attr, ResOutputInfo resInfo) {
		String sId = (String)attr.getValue("id");
		if (sId != null) {
			mId = MotoMathUtil.parseInt(sId);
		}
		String sRepeatCount = (String)attr.getValue("repeat_count");
		if (sRepeatCount != null) {
			if ("endless".equals(sRepeatCount) || "-1".equals(sRepeatCount)) {
				mRepeatCount = REPEAT_COUNT_ENDLESS;
			} else {
				mRepeatCount = MotoMathUtil.parseInt(sRepeatCount);
			}
		}
	}
	
	public MotoAnimation() {
		
	}
	
	public void setId(int id) {
		mId = id;
	}
	public int getId() {
		return mId;
	}
	
	/**
	 * Set the animation's duration. 
	 * The animation-set's duration is matched with its children.
	 * So there should be no effect to set the animation=-set's duration.
	 * @param duration : duration to be set.
	 */
	public void setDuration(int duration) {
		mDuration = duration;
		updateDuration();
	}
	
	public int getDuration() {
		return mDuration;
	}
	
	/**
	 * Start the animation from the beginning.
	 */
	public void start() {
		reset();
		mStatus = STATUS_RUNNING;
		if (mListener != null) {
			mListener.onAnimationStart(this);
		}
	}
	
	/**
	 * The stopped animation will not affect the properties of object.
	 */
	public void stop() {
		mStatus = STATUS_STOPED;
		if (mListener != null) {
			mListener.onAnimationStop(this);
		}
	}
	
	/**
	 * Pause the animation, keep the status at the time of paused.
	 * Only running animation can be paused.
	 */
	public void pause() {
		mStatus = STATUS_PAUSED;
		if (mListener != null) {
			mListener.onAnimationPause(this);
		}
	}
	
	/**
	 * Resume the paused animation.
	 * Nothing will happen when the animation is not paused.
	 */
	public void resume() {
		if (mStatus == STATUS_PAUSED) {
			mStatus = STATUS_RUNNING;
			if (mListener != null) {
				mListener.onAnimationResume(this);
			}
		}
	}
	
	/**
	 * The canceled animation will not affect the object's properties.
	 */
	public void cancel() {
		mStatus = STATUS_STOPED;
		if (mListener != null) {
			mListener.onAnimationCancel(this);
		}
	}
	
	/**
	 * Reset the animation to the beginning.
	 * If the animation is started, it will be set to the status of the beginning.
	 * Nothing will happen when it's not started.
	 */
	public void reset() {
		if (mStatus != STATUS_NOT_STARTED) {
			mStatus = STATUS_RUNNING;
		}
		mRepeatIndex = 0;
	}
	
	/**
	 * Reset the animation and seek to the given time.
	 * If the animation is started, it will be set to the status of the beginning.
	 * Nothing will happen when it's not started.
	 * @param time: the time seek to. Must be larger than 0.
	 * @return: the left time that haven't be used by this animation.
	 * -1 stands for that all time is used by this animation.
	 * 0 stands for that all time is used by this animation and it just reach to the end.
	 * Otherwise, the return value would be larger than 0.
	 */
	public int reset(int time) {
		reset();
		return next(time, null);
	}
	
	/**
	 * 
	 * @return The status of this animation.
	 */
	public int getStatus() {
		return mStatus;
	}
	
	/**
	 * @return Whether the animation is going or not.
	 * Running animation can be treated as going.
	 */
	public boolean isGoing() {
		return (mStatus == STATUS_RUNNING);
	}
	
	/**
	 * 
	 * @param repeatCount: How many times the animation will repeat. Could be REPEAT_COUNT_ENDLESS;
	 */
	public void setRepeatCount(int repeatCount) {
		mRepeatCount = repeatCount;
		updateParentDuration();
	}
	
	/**
	 * 
	 * @return repeat count.
	 */
	public int getRepeatCount() {
		return mRepeatCount;
	}
	
	/**
	 * TODO
	 * When the animation reaches to the end, it will play from end to beginning. 
	 * The repeat count should be treated as 2 when an animation plays and then backs.
	 * @param bPlayback
	 */
	public void setPlayback(boolean bPlayback) {
		mPlayback = bPlayback;
	}
	
	/**
	 * 
	 * @return Whether the animation will play back when reaches to the end.
	 */
	public boolean getPlayback() {
		return mPlayback;
	}
	
	/**
	 * 
	 * @param id: the id of animation to be found.
	 * @return The matched animation.
	 */
	public abstract MotoAnimation findAnimationById(int id);
	
	/**
	 * Animation which not started or in stopped can not be seek.
	 */
	public abstract void seekToProgress(float progress);
	
	/**
	 * Animation which not started or in stopped can not be seek.
	 */
	public abstract void seekToTime(int time);
	
	/**
	 * Update its own duration and its parent's duration.
	 * Happens when children's repeat count change, children count change or others change.
	 */
	public abstract void updateDuration();
	
	/**
	 * Update parent duration when my duration, repeat count, or others change.
	 */
	public abstract void updateParentDuration();

	protected abstract boolean hasNext();
	/**
	 * Make sure that the animation has next and is on going before call it.
	 * @param time Must be larger than 0.
	 * @return: the left time that haven't be used by this animation.
	 * -1 stands for that all time is used by this animation.
	 * 0 stands for that all time is used by this animation and it just reach to the end.
	 * Otherwise, the return value would be larger than 0.
	 */
	protected abstract int next(int time, MotoAnimationProperties animationProperties);
	protected abstract void seekToTime(int time, MotoAnimationProperties animationProperties);
	protected abstract void seekToProgress(float progress, MotoAnimationProperties animationProperties);
	protected abstract int getTotalDuration();
	
	public void setListener(MotoAnimationListener l) {
		mListener = l;
	}
	
	public MotoAnimationListener getListener() {
		return mListener;
	}
	
}
