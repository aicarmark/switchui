package com.motorola.mmsp.render.resloader;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import android.util.Log;

import com.motorola.mmsp.render.MotoAttributeArray;
import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;

@SuppressWarnings("rawtypes")
public class MotoComponentBuilder{
	private static final String TAG = "MotoRenderPlayer";
	
	static final int TYPE_INTERACTION = 1;
	static final int TYPE_INTERACTION_SET = 2;
	static final int TYPE_INTERACTION_TAG = 3;
	
	static final int TYPE_ANIMATION = 4;
	static final int TYPE_ANIMATION_SET = 5;
	static final int TYPE_ANIMATION_TAG = 6;
	
	static final int TYPE_PICTURE = 7;
	static final int TYPE_PICTURE_SET = 8;
	static final int TYPE_PICTURE_TAG = 9;
	
	static final int TYPE_KEYFRAME = 10;
	static final int TYPE_KEYFRAME_ANIMATION = 11;
	static final int TYPE_KEYFRAME_SET = 12;
	
	
	int mId;
	int mType;
	Class mClass;
	MotoAttributeArray mAttr;
	int mInstanceCount = 1;
	
	@SuppressWarnings("unchecked")
	public Object build(ResOutputInfo resInfo) {
		try {
			if (mType == TYPE_PICTURE || mType == TYPE_PICTURE_SET) {
				Constructor constructor = mClass.getConstructor(mAttr.getClass(), resInfo.getClass());
				ArrayList<Object> array = new ArrayList<Object>();
				for (int i=0; i<mInstanceCount; i++) {
					Object object = constructor.newInstance(mAttr, resInfo);
					array.add(object);
				}
				return array;
			} else {
				Constructor constructor = mClass.getConstructor(mAttr.getClass(), resInfo.getClass());
				return constructor.newInstance(mAttr, resInfo);
			}
		}catch (Exception e) {
			Log.e(TAG, "Error when construct " + mClass, e);
		}
		return null;
	}
	
	public void setClass(Class clz)
	{
		mClass = clz;
	}
}
