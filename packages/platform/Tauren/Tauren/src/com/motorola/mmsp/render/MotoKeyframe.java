package com.motorola.mmsp.render;

import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;
import com.motorola.mmsp.render.util.MotoMathUtil;

public class MotoKeyframe {

	static final String TAG = "MotoKeyframe";

	float mFraction = -1f;

	int mTime = -1;

	KFType mType;

	float mValue;

	public static enum KFType {
		linear
	}

	public MotoKeyframe(KFType kfType, float fraction, float value) {
		mFraction = fraction;
		mValue = value;
		mType = kfType;
	}

	public MotoKeyframe(KFType kfType, int time, float value) {
		mTime = time;
		mValue = value;
		mType = kfType;
	}

	public MotoKeyframe(MotoAttributeArray attr, ResOutputInfo resInfo) {

		// scan duration first,if null, then scan fraction
		String sTime = (String) attr.getValue("time");
		if (sTime != null) {
			mTime = MotoMathUtil.parseInt(sTime);
		} else {
			String sFraction = (String) attr.getValue("fraction");
			if (sFraction != null) {
				mFraction = MotoMathUtil.parseFloat(sFraction);
			}
		}

		String sValue = (String) attr.getValue("value");
		if (sValue != null) {
			mValue = MotoMathUtil.parseFloatNormal(sValue,
					resInfo.mPlayConfigInfo.mDs2PxScale);
		}

		String sType = (String) attr.getValue("type");
		if (sType != null) {
			mType = KFType.valueOf(sType);
		}
	}

	/**
	 * get a value by the elapsed fraction of an keyframe, starValue of an
	 * keyframe and endValue of an keyframe.This allows keyframes to have
	 * non-linear motion, such as acceleration and deceleration.
	 * 
	 * @param fraction
	 * @param startValue
	 * @param endValue
	 * @return
	 */
	public float getValue(float fraction, float startValue, float endValue) {
		switch (mType) {
		case linear:
			return evaluateLinearValue(fraction, startValue, endValue);
		}
		return 0;
	}

	/**
	 * Constructs and returns a MotoKeyframe object with the specified
	 * properties
	 * 
	 * @param fraction
	 * @param value
	 * @return
	 */
	public static MotoKeyframe ofInstance(KFType kfType, float fraction,
			float value) {
		return new MotoKeyframe(kfType, fraction, value);
	}

	/**
	 * Constructs and returns a MotoKeyframe object with the specified
	 * properties
	 * 
	 * @param fraction
	 * @param value
	 * @return
	 */
	public static MotoKeyframe ofInstance(KFType kfType, int time, float value) {
		return new MotoKeyframe(kfType, time, value);
	}


	/**
	 * This function returns the result of linearly interpolating the start and
	 * end values, with <code>fraction</code> representing the proportion
	 * between the start and end values. The calculation is a simple parametric
	 * calculation: <code>result = x0 + t * (v1 - v0)</code>, where
	 * <code>x0</code> is <code>startValue</code>, <code>x1</code> is
	 * <code>endValue</code>, and <code>t</code> is <code>fraction</code>.
	 * 
	 * @param fraction
	 *            The fraction from the starting to the ending values
	 * @param startValue
	 *            The start value; should be of type <code>int</code> or
	 *            <code>Integer</code>
	 * @param endValue
	 *            The end value; should be of type <code>int</code> or
	 *            <code>Integer</code>
	 * @return A linear interpolation between the start and end values, given
	 *         the <code>fraction</code> parameter.
	 */
	public static final float evaluateLinearValue(float fraction,
			float startValue, float endValue) {
		return startValue + fraction * (endValue - startValue);
	}
}
