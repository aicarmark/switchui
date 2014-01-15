package com.motorola.mmsp.render;

import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;

public class MotoKeyframeAnimation extends MotoObjectAnimation {

	/**
	 * Constructs MotoKeyframeAnimation with the specified properties, if
	 * valueCalculator is null, constructs a MotoKeyframeCalculator object for
	 * the current MotoKeyframeAnimation
	 * 
	 * @param duration
	 * @param type
	 * @param affectType
	 * @param valueCalculator
	 */
	public MotoKeyframeAnimation(int duration, int type, int affectType,
			MotoValueCalculator valueCalculator) {
		// TODO Auto-generated constructor stub
		super(duration, type, affectType, valueCalculator);
		if (null == valueCalculator) {
			mValueCalculator = new MotoKeyframeCalculator(mDuration);
		}

	}

	public MotoKeyframeAnimation(MotoAttributeArray attr, ResOutputInfo resInfo) {
		super(attr, resInfo);
		// TODO Auto-generated constructor stub

	}

	/**
	 * Constructs and returns a MotoKeyframeAnimation object with the specified
	 * properties
	 * 
	 * @param duration
	 * @param type
	 * @param affectType
	 * @param valueCalculator
	 * @param keyframeSet
	 * @return
	 */
	public static MotoKeyframeAnimation ofKeyframeSet(int duration, int type,
			int affectType, MotoValueCalculator valueCalculator,
			MotoKeyframeSet keyframeSet) {
		MotoKeyframeAnimation animation = new MotoKeyframeAnimation(duration,
				type, affectType, valueCalculator);
		animation.setKeyframeSet(keyframeSet);
		return animation;
	}

	public void setKeyframeSet(MotoKeyframeSet keyframeSet) {
		if (null != keyframeSet) {
			keyframeSet.mDuration = mDuration;
			keyframeSet.transformFraction();
			((MotoKeyframeCalculator) mValueCalculator).mKeyframeSet = keyframeSet;
		}

	}

	@Override
	protected MotoValueCalculator onCreateMotoValueCalculator(
			MotoAttributeArray attr, ResOutputInfo resInfo) {
		// TODO Auto-generated method stub
		mValueCalculator = new MotoKeyframeCalculator(mDuration);
		return mValueCalculator;
	}

	public static class MotoKeyframeCalculator implements MotoValueCalculator {

		private int mDuration;

		MotoKeyframeSet mKeyframeSet;

		/**
		 * Construction
		 * 
		 * @param duration
		 *            : the duration of line process
		 * @frameTime: the duration between two frames
		 */
		public MotoKeyframeCalculator(int duration) {
			mDuration = duration;
		}

		/**
		 * get a value from the given frame
		 * 
		 * @param frame
		 *            : get the value by frame
		 * @return float
		 */
		public float getValueAt(int time) {
			return mKeyframeSet.getValue(time);
		}

		public int getDuration() {
			return mDuration;
		}
	}
}
