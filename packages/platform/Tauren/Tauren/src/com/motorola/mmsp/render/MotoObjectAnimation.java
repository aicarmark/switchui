package com.motorola.mmsp.render;

import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;
import com.motorola.mmsp.render.util.MotoMathUtil;



public abstract class MotoObjectAnimation extends MotoAnimation {
	public static final int ANIMATION_TRANSLATE_X = 1;
	public static final int ANIMATION_TRANSLATE_Y = 2;
	public static final int ANIMATION_TRANSLATE_Z = 3;
	public static final int ANIMATION_SCALE_X = 4;
	public static final int ANIMATION_SCALE_Y = 5;
	public static final int ANIMATION_ROTATE = 6;
	public static final int ANIMATION_ROTATE_PX = 7;
	public static final int ANIMATION_ROTATE_PY = 8;
	public static final int ANIMATION_ALPHA = 9;
	public static final int ANIMATION_VISIBLE = 10;

	// add at 04.09
	public static final int ANIMATION_ROTATE_X = 11;
	public static final int ANIMATION_ROTATE_Y = 12;
	public static final int ANIMATION_ROTATE_X_PY = 13;
	public static final int ANIMATION_ROTATE_Y_PX = 14;

	public static final int ANIMAION_MAX = 15;
	
	public static final int AFFECT_TYPE_NONE = 0;
	public static final int AFFECT_TYPE_DELTA = 1;
	public static final int AFFECT_TYPE_SET = 2;
	//public static final int AFFECT_TYPE_PHYSICAL = 3;
	
	
	protected int mPropertyType = ANIMATION_TRANSLATE_X;
	protected int mAffectType;
	protected MotoValueCalculator mValueCalculator;
	protected boolean mHasNext;
	protected int mCurrentTime;
	protected float mCurrentPropertyValue;
	
	
	/**
	 * 
	 * The default constructor called by resource loader.
	 * @param attr: the attributes defined in resource file.
	 * @param resInfo: the loaded res.
	 */
	public MotoObjectAnimation(MotoAttributeArray attr, ResOutputInfo resInfo) {
		super(attr, resInfo);
		String sDuration = (String)attr.getValue("duration");
		if (sDuration != null) {
			mDuration = MotoMathUtil.parseInt(sDuration);
		}
		String sPropertyType = (String)attr.getValue("type");
		if (sPropertyType == null) {
			mPropertyType = ANIMATION_TRANSLATE_X;
		} else if ("translate_y".equals(sPropertyType)) {
			mPropertyType = ANIMATION_TRANSLATE_Y;
		} else if ("translate_z".equals(sPropertyType)) {
			mPropertyType = ANIMATION_TRANSLATE_Z;
		} else if ("scale_x".equals(sPropertyType)) {
			mPropertyType = ANIMATION_SCALE_X;
		} else if ("scale_y".equals(sPropertyType)) {
			mPropertyType = ANIMATION_SCALE_Y;
		} else if ("rotate".equals(sPropertyType)) {
			mPropertyType = ANIMATION_ROTATE;
		} else if ("rotate_px".equals(sPropertyType)) {
			mPropertyType = ANIMATION_ROTATE_PX;
		} else if ("rotate_py".equals(sPropertyType)) {
			mPropertyType = ANIMATION_ROTATE_PY;
		} else if ("alpha".equals(sPropertyType)) {
			mPropertyType = ANIMATION_ALPHA;
		} else if ("visible".equals(sPropertyType)) {
			mPropertyType = ANIMATION_VISIBLE;
		} else if ("rotate_x".equals(sPropertyType)) {
			mPropertyType = ANIMATION_ROTATE_X;
		} else if ("rotate_y".equals(sPropertyType)) {
			mPropertyType = ANIMATION_ROTATE_Y;
		} else if ("rotate_x_py".equals(sPropertyType)) {
			mPropertyType = ANIMATION_ROTATE_X_PY;
		} else if ("rotate_y_px".equals(sPropertyType)) {
			mPropertyType = ANIMATION_ROTATE_Y_PX;
		} else {
			mPropertyType = ANIMATION_TRANSLATE_X;
		}

		String sAffectType = (String)attr.getValue("affect_type");
		if (sAffectType == null) {
			mAffectType = AFFECT_TYPE_NONE;
		} else if ("delta".equals(sAffectType)) {
			mAffectType = AFFECT_TYPE_DELTA;
		} else if ("set".equals(sAffectType)) {
			mAffectType = AFFECT_TYPE_SET;
		}
		
		mValueCalculator = onCreateMotoValueCalculator(attr, resInfo);
		init();
	}
	
	/**
	 * @param duration: Must be larger than 0.
	 * @param frameTime: 
	 * @param type: 
	 * @param affectType: 
	 * @param valueCalculator:  
	 */
	public MotoObjectAnimation(int duration, int type, int affectType, MotoValueCalculator valueCalculator) {
		mDuration = duration;
		mPropertyType = type;
		mAffectType = affectType;
		mValueCalculator = valueCalculator;
		init();
	}
	
	protected abstract MotoValueCalculator onCreateMotoValueCalculator(MotoAttributeArray attr, ResOutputInfo resInfo);
	
	private void init() {
		float totalDuration = getTotalDuration();
		mHasNext = ((totalDuration == DURATION_ENDLESS) || (totalDuration > 0));
	}
	
	public void setType(int type) {
		mPropertyType = type;
	}
	public int getType() {
		return mPropertyType;
	}
	public void setAffectType(int affectType) {
		mAffectType = affectType;
	}
	public int getAffectType() {
		return mAffectType;
	}
	
	@Override
	public void seekToProgress(float progress) {
		//if (mStatus == STATUS_NOT_STARTED || mStatus == STATUS_STOPED) {
		//	return;
		//}
		mCurrentTime = (int) (progress * mDuration);
		if (mListener != null) {
			mListener.onAnimationSeek(this, progress);
		}
	}
	
	@Override
	public void seekToTime(int time) {
		// modify at 2012.04.17
		seekToProgress((float) time / mDuration);
	}
	
	
	@Override
	public void reset() {
		super.reset();
		mCurrentTime = 0;
		float totalDuration = getTotalDuration();
		mHasNext = ((totalDuration == DURATION_ENDLESS) || (totalDuration > 0));
	}
	
	@Override
	public int reset(int time) {
		reset();
		return next(time, null);
	}
	
	@Override
	public void updateDuration() {
		//do nothing.
	}
	
	@Override
	public void updateParentDuration() {
		float totalDuration = getTotalDuration();
		mHasNext = ((totalDuration == DURATION_ENDLESS) || (totalDuration > 0));
		if (mParent != null) {
			mParent.updateDuration();
		}
	}
	
	@Override
	protected boolean hasNext() {
		return mHasNext;
	}
	
	@Override
	protected void seekToTime(int time, MotoAnimationProperties animationProperties) {
		// delete at 2012.04.17
		// if (mStatus == STATUS_NOT_STARTED) {
		// return;
		// }
		
		if (mRepeatCount == REPEAT_COUNT_ENDLESS) {
			mCurrentTime = (int) (time % mDuration);
			mCurrentPropertyValue = mValueCalculator.getValueAt(mCurrentTime);
			affectTo(animationProperties);
			if (mStatus != STATUS_RUNNING) {
				mStatus = STATUS_RUNNING;
			}
			if (mListener != null) {
				mListener.onAnimationSeek(this, ((float)mCurrentTime / mDuration));
			}
		} else {
			if (time >= mRepeatCount * mDuration) {
				//seek to the end;
				mCurrentTime = (int) mDuration;
				mRepeatIndex = mRepeatCount - 1;
				mCurrentPropertyValue = mValueCalculator.getValueAt(mCurrentTime);
				affectTo(animationProperties);
				mHasNext = false;
				//mStatus = STATUS_STOPED;
				//if (mListener != null) {
					//mListener.onAnimationEnd(this);
				//}
			} else {
				mCurrentTime = (int) (time % mDuration);
				mRepeatIndex = (int)(time / mDuration);
				mCurrentPropertyValue = mValueCalculator.getValueAt(mCurrentTime);
				affectTo(animationProperties);
				if (mListener != null) {
					mListener.onAnimationSeek(this, ((float)mCurrentTime / mDuration));
				}
			}
		}
	}
	
	@Override
	protected void seekToProgress(float progress,
			MotoAnimationProperties animationProperties) {
		int time = (int)(mDuration * progress);
		seekToTime(time, animationProperties);
	}
	
	@Override
	protected int getTotalDuration() {
		if (mRepeatCount == REPEAT_COUNT_ENDLESS) {
			return DURATION_ENDLESS;
		} else {
			return mDuration * mRepeatCount;
		}
	}
	@Override
	protected int next(int time, MotoAnimationProperties animationProperties) {
		if (animationProperties != null) {
			mCurrentPropertyValue = mValueCalculator.getValueAt(mCurrentTime);
			affectTo(animationProperties);
		}
		mCurrentTime += time;
		int leftTime = -1;
		if (mCurrentTime > mDuration) {
			if (mRepeatCount == REPEAT_COUNT_ENDLESS) {
				mCurrentTime %= mDuration;
				if (mListener != null) {
					mListener.onAnimationRepeat(this);
				}
			} else {
				int oldRepeatIndex = mRepeatIndex;
				mRepeatIndex += mCurrentTime / mDuration;
				if (mRepeatIndex < mRepeatCount) {
					mCurrentTime %= mDuration;
					if (mListener != null) {
						mListener.onAnimationRepeat(this);
					}
				} else {
				    //affect value of last frame
				    if (animationProperties != null) {
			            mCurrentPropertyValue = mValueCalculator.getValueAt(mDuration);
			            affectTo(animationProperties);
			        }
				    
					mHasNext = false;
					mStatus = STATUS_STOPED;
					
					if (mListener != null) {
						//modify at 2012.05.08 by owen
						mListener.onAnimationStop(this);
						//mListener.onAnimationEnd(this);
					}
					leftTime = mCurrentTime - (mRepeatCount - oldRepeatIndex) * mDuration;
				}
			}
		}
		return leftTime;
	}
	
	protected void affectTo(MotoAnimationProperties animationProperties) {
		animationProperties.setPropertyAffectType(mPropertyType, mAffectType);
		animationProperties.setPropertyValue(mPropertyType, mCurrentPropertyValue);
		animationProperties.mHasChanged = true;
	}
	
	
	public static interface MotoValueCalculator {
		float getValueAt(int time);
		int getDuration();
	}


	@Override
	public MotoAnimation findAnimationById(int id) {
		if (getId() == id) {
			return this;
		}
		return null;
	}
}
