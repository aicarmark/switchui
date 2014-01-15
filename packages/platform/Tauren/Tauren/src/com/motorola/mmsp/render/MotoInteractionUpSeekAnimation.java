package com.motorola.mmsp.render;

import com.motorola.mmsp.render.resloader.MotoComponentBuilder;
import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;
import com.motorola.mmsp.render.util.MotoMathUtil;

public class MotoInteractionUpSeekAnimation extends MotoInteraction{
	private MotoAnimationSet mAnimationSet;
	private float mProgress;

	public MotoInteractionUpSeekAnimation(MotoAttributeArray attr, ResOutputInfo resInfo) {
		super(attr, resInfo);
		String string_animation_set = (String)attr.getValue("animation_set");
		String string_seek_animation_to = (String)attr.getValue("seek_animation_to");
		if (string_animation_set != null) {
			string_animation_set = string_animation_set.replace("@", "");
			int id = MotoMathUtil.parseInt(string_animation_set);
			MotoComponentBuilder builder = resInfo.mAnimationSetsBuilders.get(id);
			if (builder != null) {
				try {
					mAnimationSet = (MotoAnimationSet)builder.build(resInfo);
					if (mAnimationSet != null) {
						mAnimationSet.start();
					}
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
		}
		if (string_seek_animation_to != null) {
			mProgress = MotoMathUtil.parseFloat(string_seek_animation_to);
		}
	}
	
	public MotoInteractionUpSeekAnimation(int interceptEventFlag,
			int handeEventFlag, boolean blockAnimation) {
		super(interceptEventFlag, handeEventFlag, blockAnimation);
	}
	
	public void setAnimationSet(MotoAnimationSet animationSet, float progress) {
		mAnimationSet = animationSet;
		mProgress = progress;
		if (mAnimationSet != null) {
			mAnimationSet.start();
		}
	}

	@Override
	public boolean handleEvent(MotoInteractionEvent event,
			MotoAnimationProperties propertiesOutput, MotoPicture bindPicture) {
		if (mAnimationSet != null) {
			mAnimationSet.seekToProgress(mProgress, propertiesOutput);
		}
		return mBlockAnimation;
	}
}
