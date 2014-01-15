package com.motorola.mmsp.render;

import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;
import com.motorola.mmsp.render.util.MotoMathUtil;

public class MotoLinearAnimation extends MotoObjectAnimation {
	public MotoLinearAnimation(MotoAttributeArray attr, ResOutputInfo resInfo) {
		super(attr, resInfo);
	}
	
	public MotoLinearAnimation(int type, int affectType, int duration, float from,
			float to) {
		super(duration, type, affectType, 
				new MotoLinearValueCalculator(duration, from, to));
	}
	
	public MotoLinearAnimation(int type, int affectType, float from,
			float speed, int duration) {
		super(duration, type, affectType, 
				new MotoLinearValueCalculator(from, speed, duration));
	}

	@Override
	protected MotoValueCalculator onCreateMotoValueCalculator(
			MotoAttributeArray attr, ResOutputInfo resInfo) {
		String from_string = (String)attr.getValue("from");
		String to_string = (String)attr.getValue("to");
		final float ds2PxScale = resInfo.mPlayConfigInfo.mDs2PxScale;
		float from = MotoMathUtil.parseFloatNormal(from_string, ds2PxScale);
		float to = MotoMathUtil.parseFloatNormal(to_string, ds2PxScale);
		if (to_string != null) {
			return new MotoLinearValueCalculator(mDuration, from, to);
		} else {
			String speed_string = (String) attr.getValue("speed");
			float speed = MotoMathUtil.parseFloatVelocity(speed_string,
					ds2PxScale);
			return new MotoLinearValueCalculator(from, speed, mDuration);
		}

	}
	
	public static class MotoLinearValueCalculator implements MotoValueCalculator {
		private float mFrom;
		private float mStep;
		private int mDuration;

		/**
		 * Construction
		 * 
		 * @param duration
		 *            : the duration of line process
		 * @frameTime: the duration between two frames
		 */
		public MotoLinearValueCalculator(int duration, float from,
				float to) {
			float length = to - from;
			mStep = length / duration;
			mFrom = from;
			mDuration = duration;
		}
		
		public MotoLinearValueCalculator(float from,
				float speed, int duration) {
			mStep = speed / 1000;
			mFrom = from;
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
			return mFrom + mStep * time;
		}

		public int getDuration() {
			return mDuration;
		}
	}
}
