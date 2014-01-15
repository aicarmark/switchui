package com.motorola.mmsp.render;

import com.motorola.mmsp.render.resloader.MotoComponentBuilder;
import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;
import com.motorola.mmsp.render.util.MotoMathUtil;

public class MotoInteractionMoveAnimationFollow extends MotoInteraction{
	private MotoAnimationSet mAnimationSet;
	
	public MotoInteractionMoveAnimationFollow(MotoAttributeArray attr, ResOutputInfo resInfo) {
		super(attr, resInfo);
		String string_animationSetId = (String)attr.getValue("animation_follow");
		if (string_animationSetId != null) {
			string_animationSetId = string_animationSetId.replace("@", "");
			int id = MotoMathUtil.parseInt(string_animationSetId);
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
	}
	
	public MotoInteractionMoveAnimationFollow(int interceptEventFlag,
			int handeEventFlag, boolean blockAnimation) {
		super(interceptEventFlag, handeEventFlag, blockAnimation);
	}
	
	public void setFollowdAnimationSet(MotoAnimationSet animationSet) {
		mAnimationSet = animationSet;
		if (mAnimationSet != null) {
			mAnimationSet.start();
		}
	}

	@Override
	public boolean handleEvent(MotoInteractionEvent event,
			MotoAnimationProperties propertiesOutput, MotoPicture bindPicture) {
		if (event.mData != null && mAnimationSet != null) {
			float progress = (Float)event.mData;
			mAnimationSet.seekToProgress(progress, propertiesOutput);
			return mBlockAnimation;
		}
		return false;
	}
	
}
