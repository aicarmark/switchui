package com.motorola.mmsp.render;

import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;
import com.motorola.mmsp.render.util.MotoMathUtil;


public class MotoAccelerationAnimation extends MotoObjectAnimation{

	public MotoAccelerationAnimation(MotoAttributeArray attr, ResOutputInfo resInfo) {
		super(attr, resInfo);
	}
	
	public MotoAccelerationAnimation(int type, int affectType, int duration, float acceleration, float from,
			float to) {
		super(duration, type, affectType, 
				new MotoAccelerationValueCalculator(duration, acceleration, from, to));
	}
	
	public MotoAccelerationAnimation(int type, int affectType, int duration, int frameTime, float from,
			float init_velocity, float acceleration) {
		super(duration, type, affectType, 
				new MotoAccelerationValueCalculator(duration, from, init_velocity, acceleration));
	}

	@Override
	protected MotoValueCalculator onCreateMotoValueCalculator(
			MotoAttributeArray attr, ResOutputInfo resInfo) {
		String string_acceleration = (String)attr.getValue("acceleration");
		String string_from = (String)attr.getValue("from");
		String string_to = (String)attr.getValue("to");
		final float ds2PxScale = resInfo.mPlayConfigInfo.mDs2PxScale;
		float acceleration = MotoMathUtil.parseFloatAcceleration(string_acceleration, ds2PxScale);
		float from = MotoMathUtil.parseFloatNormal(string_from, ds2PxScale);
		float to = MotoMathUtil.parseFloatNormal(string_to, ds2PxScale);
		if (string_to != null) {
			return new MotoAccelerationValueCalculator(mDuration, acceleration, from, to);
		} else {
			String string_init_velocity = (String)attr.getValue("init_velocity");
			float init_velocity = MotoMathUtil.parseFloatVelocity(string_init_velocity, ds2PxScale);
			return new MotoAccelerationValueCalculator(from, init_velocity, acceleration, mDuration);
		}
	}

	public static class MotoAccelerationValueCalculator implements MotoValueCalculator {
		private float mV0InMili;
		private float mAInMiliDev2000;
		private int mDuration;
		private float mFrom;
		public MotoAccelerationValueCalculator(int duration, float acceleration, float from,
				float to) {
			this(from, getV0((to - from), duration, acceleration), acceleration, duration);
		}
		
		public MotoAccelerationValueCalculator(float from,
			float init_velocity, float acceleration, int duration) {
			mFrom = from;
			mDuration = duration;
			mV0InMili = init_velocity / 1000;
			mAInMiliDev2000 = acceleration / (2000 * 1000);
		}
		
		private static float getV0(float s, long duration, float acceleration) {
			float v0 = s * 1000 / duration - acceleration * duration / 1000 / 2;
			return v0;
		}

		//@Override
		public float getValueAt(int time) {
			return mFrom + (mV0InMili + mAInMiliDev2000 * time) * time;
		}

		public int getDuration() {
			return mDuration;
		}
		
	}
}
