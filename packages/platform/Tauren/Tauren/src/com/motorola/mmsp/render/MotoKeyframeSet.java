package com.motorola.mmsp.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.util.Log;

import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;

public class MotoKeyframeSet {

	static final String TAG = MotoKeyframe.TAG;

	int mNumKeyframes;

	protected int mDuration;

	public int getDuration() {
		return mDuration;
	}

	public void transformFraction() {
		for (int i = 0; i < mNumKeyframes; i++) {
			MotoKeyframe obj = mKeyframes.get(i);
			if (-1 == obj.mTime && -1 != obj.mFraction) {
				obj.mTime = (int) (obj.mFraction * mDuration);
			}
		}
	}

	public int getNumKeyframes() {
		return mNumKeyframes;
	}

	public ArrayList<MotoKeyframe> getKeyframes() {
		return mKeyframes;
	}

	ArrayList<MotoKeyframe> mKeyframes = new ArrayList<MotoKeyframe>(5);

	public MotoKeyframeSet(MotoAttributeArray attr, ResOutputInfo resInfo) {
	}

	public MotoKeyframeSet(List<MotoKeyframe> keyframes) {
		setKeyframes(keyframes);
	}

	@Deprecated
	/**
	 * get value from the specified keyframe by the given fraction
	 * 
	 * @param fraction
	 * @return
	 */
	public float getValue(float fraction) {
		// Log.d(TAG, "---getValue(" + fraction + ")---");
		MotoKeyframe prevKeyframe = (MotoKeyframe) mKeyframes.get(0);

		for (int i = 1; i < mNumKeyframes; i++) {
			MotoKeyframe nextKeyframe = (MotoKeyframe) mKeyframes.get(i);
			if (fraction < nextKeyframe.mFraction) {
				float intervalFraction = (fraction - prevKeyframe.mFraction)
						/ (nextKeyframe.mFraction - prevKeyframe.mFraction);
				return nextKeyframe.getValue(intervalFraction,
						prevKeyframe.mValue, nextKeyframe.mValue);
			}
			prevKeyframe = nextKeyframe;
		}

		return mKeyframes.get(mNumKeyframes - 1).mValue;
	}

	/**
	 * get value from the specified keyframe by the given time
	 * 
	 * @param time
	 * @return
	 */
	public float getValue(int time) {
		// Log.d(TAG, "---getValue(" + time + ")---");
		MotoKeyframe prevKeyframe = mKeyframes.get(0);
		for (int i = 1; i < mNumKeyframes; i++) {
			MotoKeyframe nextKeyframe = mKeyframes.get(i);
			if (time < nextKeyframe.mTime) {
				final float intervalFraction = (float) (time - prevKeyframe.mTime)
						/ (nextKeyframe.mTime - prevKeyframe.mTime);
				// final float ret = nextKeyframe.getValue(intervalFraction,
				// prevKeyframe.mValue, nextKeyframe.mValue);
				// Log.d(TAG,"---intervalFraction="+intervalFraction+" ret="+ret);
				return nextKeyframe.getValue(intervalFraction,
						prevKeyframe.mValue, nextKeyframe.mValue);
			}
			prevKeyframe = nextKeyframe;
		}

		return mKeyframes.get(mNumKeyframes - 1).mValue;
	}

	public void addKeyframe(MotoKeyframe keyframe) {
		mKeyframes.add(keyframe);
		mNumKeyframes++;
	}

	public void removeKeyframe(int index) {
		mKeyframes.remove(index);
		mNumKeyframes = mKeyframes.size();
	}

	public void removeKeyframe(MotoKeyframe keyframe) {
		mKeyframes.remove(keyframe);
		mNumKeyframes = mKeyframes.size();
	}

	public void setKeyframes(List<MotoKeyframe> keyframes) {
		if (null == keyframes || keyframes.size() == 0) {
			Log.w(TAG,
					"----keyframes is null, so clear the current keyframeset--");
			mKeyframes.clear();
			mNumKeyframes = 0;
			return;
		}

		mKeyframes.clear();
		mKeyframes.addAll(keyframes);
		mNumKeyframes = keyframes.size();
	}

	/**
	 * Constructs and returns a MotoKeyframeSet object with the specified set of
	 * values.
	 * 
	 * @param keyframes
	 * @return
	 */
	public static MotoKeyframeSet ofKeyframe(MotoKeyframe... keyframes) {
		if (null != keyframes && keyframes.length != 0) {
			return new MotoKeyframeSet(Arrays.asList(keyframes));
		}
		return null;
	}

}
