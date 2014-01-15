package com.motorola.mmsp.render.resloader;

import java.util.ArrayList;

import com.motorola.mmsp.render.MotoAnimation;
import com.motorola.mmsp.render.MotoAnimationSet;
import com.motorola.mmsp.render.MotoInteraction;
import com.motorola.mmsp.render.MotoInteractionSet;
import com.motorola.mmsp.render.MotoKeyframe;
import com.motorola.mmsp.render.MotoKeyframeAnimation;
import com.motorola.mmsp.render.MotoKeyframeSet;
import com.motorola.mmsp.render.MotoPicture;
import com.motorola.mmsp.render.MotoPictureGroup;
import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;

public class MotoComponentParentBuilder extends MotoComponentBuilder{
	ArrayList<MotoComponentBuilder> mChildren;
	MotoComponentParentBuilder(int cap) {
		mChildren = new ArrayList<MotoComponentBuilder>(cap);
	}
	void add(MotoComponentBuilder child) {
		mChildren.add(child);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Object build(ResOutputInfo resInfo) {
		if (mType == TYPE_ANIMATION_SET) {
			MotoAnimationSet me = (MotoAnimationSet)super.build(resInfo);
			int size = mChildren.size();
			for (int i=0; i<size; i++) {
				MotoComponentBuilder childBuilder = mChildren.get(i);
				MotoAnimation child = (MotoAnimation)childBuilder.build(resInfo);
				me.addMotoAnimation(child, false);
			}
			me.updateDuration();
			return me;
		} else if (mType == TYPE_KEYFRAME_ANIMATION) {
			MotoKeyframeAnimation me = (MotoKeyframeAnimation) super
					.build(resInfo);
			MotoComponentBuilder childBuilder = mChildren.get(0);
			MotoKeyframeSet keyframeSet = (MotoKeyframeSet) childBuilder
					.build(resInfo);
			me.setKeyframeSet(keyframeSet);
			return me;
		} else if (mType == TYPE_KEYFRAME_SET) {
			MotoKeyframeSet me = (MotoKeyframeSet) super.build(resInfo);
			int size = mChildren.size();
			for (int i = 0; i < size; i++) {
				MotoComponentBuilder childBuilder = mChildren.get(i);
				MotoKeyframe keyframe = (MotoKeyframe) childBuilder
						.build(resInfo);
				me.addKeyframe(keyframe);
			}
			return me;
		}else if (mType == TYPE_INTERACTION_SET) {
			MotoInteractionSet me = (MotoInteractionSet)super.build(resInfo);
			int size = mChildren.size();
			for (int i=0; i<size; i++) {
				MotoComponentBuilder childBuilder = mChildren.get(i);
				MotoInteraction child = (MotoInteraction)childBuilder.build(resInfo);
				me.addInteraction(child);
			}
			return me;
		} else if (mType == TYPE_PICTURE_SET) {
			ArrayList<MotoPicture> meArray = (ArrayList<MotoPicture>)super.build(resInfo);
			if (meArray != null) {
				int size = meArray.size();
				for (int i=0; i<size; i++) {
					MotoPictureGroup meInstance = (MotoPictureGroup)meArray.get(i);
					int childCount = mChildren.size();
					for (int j=0; j<childCount; j++) {
						MotoComponentBuilder childBuilder = mChildren.get(j);
						if (childBuilder != null) {
							ArrayList<MotoPicture> childArray = (ArrayList<MotoPicture>)childBuilder.build(resInfo);
							if (childArray != null) {
								int childInstance = childArray.size();
								for (int k=0; k<childInstance; k++) {
									meInstance.addChild(childArray.get(k));
								}
							}
						}
					}
				}
				return meArray;
			}
			
		}
		return null;
	}
}
