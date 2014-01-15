package com.motorola.mmsp.render;

import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;
import com.motorola.mmsp.render.util.MotoMathUtil;
//TODO
public class MotoRandomStepAnimation extends MotoObjectAnimation{

	public MotoRandomStepAnimation(MotoAttributeArray attr, ResOutputInfo resInfo) {
		super(attr, resInfo);
	}
	
	public MotoRandomStepAnimation(int type, int affectType, int duration, 
			float from, String speed_string) {
		super(duration, type, affectType, 
				new MotoRandomStepValueCalculator(duration, from, speed_string, 1f));
	}

	@Override
	protected MotoValueCalculator onCreateMotoValueCalculator(
			MotoAttributeArray attr, ResOutputInfo resInfo) {
		String from_string = (String)attr.getValue("from");
		String speed_string = (String)attr.getValue("speed");
		final float ds2PxScale = resInfo.mPlayConfigInfo.mDs2PxScale;
		float from = MotoMathUtil.parseFloatNormal(from_string, ds2PxScale);
		return new MotoRandomStepValueCalculator(mDuration, from, speed_string,
				ds2PxScale);	
	}
	
	public static class MotoRandomStepValueCalculator implements MotoValueCalculator {
		private float[] mValues;
		private int mDuration; 
		private float mIndexPerMili;
		private static final int FRAME_TIME = 80;
		public MotoRandomStepValueCalculator(int duration, float from, String speed_string, float ds2PxScale) {
			mDuration = duration;
			int count = 0;
			if (speed_string.startsWith("random_run_time")) {
				String first = speed_string.substring((speed_string.indexOf('(') + 1), speed_string.indexOf(',')).trim();
				String end = speed_string.substring(speed_string.indexOf(',') + 1, speed_string.indexOf(')')).trim();
				float firstFloat_speedPerStep = MotoMathUtil.parseFloatVelocity(first,ds2PxScale) * FRAME_TIME / 1000;
				float endFloat_speedPerStep = MotoMathUtil.parseFloatVelocity(end, ds2PxScale) * FRAME_TIME / 1000;
				count = (int) (duration / FRAME_TIME);
				mValues = new float[count + 2];
				float currentValue = from;
				for (int i=0; i<=count; i++) {
					mValues[i] = currentValue;
					float currentSpeedPerStep = MotoMathUtil.getRandomFloatBetween(firstFloat_speedPerStep, endFloat_speedPerStep);
					currentValue += currentSpeedPerStep;
				}
			} else {
				count = (int) (duration / FRAME_TIME);
				mValues = new float[count + 2];
				float speedPerStep = MotoMathUtil.parseFloatVelocity(speed_string, ds2PxScale);
				float currentValue = from;
				for (int i=0; i<=count; i++) {
					mValues[i] = currentValue;
					currentValue += speedPerStep;
				}
			}
			mIndexPerMili = (float)(count) / mDuration;
		}

		/*
		public MotoRandomStepValueCalculator(float from, float to, String speed_string) {
			if (speed_string.startsWith("random_run_time")) {
				String first = speed_string.substring((speed_string.indexOf('(') + 1), speed_string.indexOf(',')).trim();
				String end = speed_string.substring(speed_string.indexOf(',') + 1, speed_string.indexOf(')')).trim();
				float firstFloat_speedPerStep = Float.parseFloat(first) / 1000;
				float endFloat_speedPerStep = Float.parseFloat(end) / 1000;
				int guessCount = (int)((to - from) / ((endFloat_speedPerStep + firstFloat_speedPerStep) / 2));
				if (guessCount > 0) {
					int maxCount = guessCount << 1;
					mValues = new float[maxCount];
					float currentValue = from;
					boolean positive = (to - from) > 0;
					if (positive) {
						int i = 0;
						while (currentValue <= to && i < maxCount) {
							mValues[i++] = currentValue;
							currentValue += MotoMathUtil.getRandomFloatBetween(firstFloat_speedPerStep, endFloat_speedPerStep);
						}
					} else {
						int i = 0;
						while (currentValue >= to && i < maxCount) {
							mValues[i++] = currentValue;
							currentValue += MotoMathUtil.getRandomFloatBetween(firstFloat_speedPerStep, endFloat_speedPerStep);
						}
					}
				} else {
					mValues = new float[0];
					mDuration = 0;
				}
			} else {
				float speedPerStep = MotoMathUtil.parseFloat(speed_string);
				int count = (int)((to - from) / speedPerStep);
				if (count > 0) {
					float currentValue = from;
					mValues = new float[count + 1];
					for (int i=0; i<count; i++) {
						mValues[i] = currentValue;
						currentValue += speedPerStep;
					}
					mValues[count] = to;
				}
			}
		}
		*/
		
		//@Override
		public float getValueAt(int time) {
			float index = (time * mIndexPerMili);
			int index_int = (int)index;
			float value0 = mValues[index_int];
			float step = mValues[index_int+1] - value0;
			float value = value0 + (index - index_int) * step;
			return value;
		}
		
		public int getDuration() {
			return mDuration;
		}
	}
}
