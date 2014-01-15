package com.motorola.mmsp.render;


/**
 * All affect type in mAffectTypes must initialized to MotoObjectAnimation.AFFECT_TYPE_NONE
 * */
public class MotoAnimationProperties {
	private int[] mAffectTypes = new int[MotoObjectAnimation.ANIMAION_MAX];
	private float[] mValues = new float[MotoObjectAnimation.ANIMAION_MAX];
	boolean mHasChanged;
	
	public int getPropertyAffectType(int property) {
		return mAffectTypes[property];
	}
	
	public float getPropertyValue(int property) {
		return mValues[property];
	}
	
	public void setPropertyAffectType(int property, int affectType) {
		mAffectTypes[property] = affectType;
	}
	
	public void setPropertyValue(int property, float value) {
		mValues[property] = value;
	}
	
	public static void copy(MotoAnimationProperties src, MotoAnimationProperties dest) {
		System.arraycopy(src.mAffectTypes, 0, dest.mAffectTypes, 0, src.mAffectTypes.length);
		System.arraycopy(src.mValues, 0, dest.mValues, 0, src.mValues.length);
	}
}
